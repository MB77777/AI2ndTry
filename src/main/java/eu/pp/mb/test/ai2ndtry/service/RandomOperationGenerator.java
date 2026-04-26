package eu.pp.mb.test.ai2ndtry.service;

import eu.pp.mb.test.ai2ndtry.model.Account;
import eu.pp.mb.test.ai2ndtry.model.OperationType;
import eu.pp.mb.test.ai2ndtry.repository.AccountRepository;
import eu.pp.mb.test.ai2ndtry.repository.BankUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Random;

@Component
@ConditionalOnProperty(name = "bank.random-generator.enabled", havingValue = "true", matchIfMissing = true)
public class RandomOperationGenerator {

    private static final Logger log = LoggerFactory.getLogger(RandomOperationGenerator.class);

    private final TaskScheduler taskScheduler;
    private final OperationService operationService;
    private final BankUserRepository bankUserRepository;
    private final AccountRepository accountRepository;
    private final Random random = new Random();

    public RandomOperationGenerator(
            TaskScheduler taskScheduler,
            OperationService operationService,
            BankUserRepository bankUserRepository,
            AccountRepository accountRepository
    ) {
        this.taskScheduler = taskScheduler;
        this.operationService = operationService;
        this.bankUserRepository = bankUserRepository;
        this.accountRepository = accountRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        scheduleNext();
    }

    private void scheduleNext() {
        long delaySeconds = 2 + random.nextInt(19);
        taskScheduler.schedule(this::generateAndReschedule, Instant.now().plusSeconds(delaySeconds));
    }

    private void generateAndReschedule() {
        try {
            generateRandomOperation();
        } catch (Exception ex) {
            log.warn("Random operation generation failed: {}", ex.getMessage());
        } finally {
            scheduleNext();
        }
    }

    private void generateRandomOperation() {
        List<Account> accounts = accountRepository.findAll();
        List<Long> userIds = bankUserRepository.findAll().stream()
                .map(user -> user.getId())
                .toList();

        if (accounts.isEmpty() || userIds.isEmpty()) {
            return;
        }

        OperationType type = pickType(accounts);
        Long initiatedByUserId = userIds.get(random.nextInt(userIds.size()));

        switch (type) {
            case WPLATA -> createDeposit(accounts, initiatedByUserId);
            case WYPLATA -> createWithdrawal(accounts, initiatedByUserId);
            case TRANSAKCJA -> createTransfer(accounts, initiatedByUserId);
        }
    }

    private OperationType pickType(List<Account> accounts) {
        boolean hasPositiveBalance = accounts.stream().anyMatch(account -> account.getBalance().signum() > 0);
        boolean hasTransferPair = accounts.stream().anyMatch(source ->
                accounts.stream().anyMatch(target ->
                        !source.getId().equals(target.getId()) && source.getCurrency() == target.getCurrency()));

        if (!hasPositiveBalance) {
            return OperationType.WPLATA;
        }
        if (!hasTransferPair) {
            return random.nextBoolean() ? OperationType.WPLATA : OperationType.WYPLATA;
        }

        OperationType[] types = OperationType.values();
        return types[random.nextInt(types.length)];
    }

    private void createDeposit(List<Account> accounts, Long initiatedByUserId) {
        Account target = accounts.get(random.nextInt(accounts.size()));
        operationService.registerAndExecute(
                OperationType.WPLATA,
                null,
                target.getId(),
                randomAmount(BigDecimal.valueOf(5000)),
                initiatedByUserId,
                null
        );
    }

    private void createWithdrawal(List<Account> accounts, Long initiatedByUserId) {
        List<Account> candidates = accounts.stream()
                .filter(account -> account.getBalance().signum() > 0)
                .toList();
        if (candidates.isEmpty()) {
            createDeposit(accounts, initiatedByUserId);
            return;
        }

        Account source = candidates.get(random.nextInt(candidates.size()));
        operationService.registerAndExecute(
                OperationType.WYPLATA,
                source.getId(),
                null,
                randomAmount(source.getBalance()),
                initiatedByUserId,
                null
        );
    }

    private void createTransfer(List<Account> accounts, Long initiatedByUserId) {
        List<Account> sourceCandidates = accounts.stream()
                .filter(account -> account.getBalance().signum() > 0)
                .toList();
        if (sourceCandidates.isEmpty()) {
            createDeposit(accounts, initiatedByUserId);
            return;
        }

        for (int i = 0; i < 20; i++) {
            Account source = sourceCandidates.get(random.nextInt(sourceCandidates.size()));
            List<Account> targetCandidates = accounts.stream()
                    .filter(account -> !account.getId().equals(source.getId()))
                    .filter(account -> account.getCurrency() == source.getCurrency())
                    .toList();
            if (targetCandidates.isEmpty()) {
                continue;
            }

            Account target = targetCandidates.get(random.nextInt(targetCandidates.size()));
            operationService.registerAndExecute(
                    OperationType.TRANSAKCJA,
                    source.getId(),
                    target.getId(),
                    randomAmount(source.getBalance()),
                    initiatedByUserId,
                    null
            );
            return;
        }

        createWithdrawal(accounts, initiatedByUserId);
    }

    private BigDecimal randomAmount(BigDecimal upperBoundInclusive) {
        BigDecimal effectiveUpperBound = upperBoundInclusive.min(BigDecimal.valueOf(5000));
        if (effectiveUpperBound.signum() <= 0) {
            return BigDecimal.ONE.setScale(2, RoundingMode.HALF_UP);
        }

        double value = 0.01 + (random.nextDouble() * effectiveUpperBound.doubleValue());
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }
}
