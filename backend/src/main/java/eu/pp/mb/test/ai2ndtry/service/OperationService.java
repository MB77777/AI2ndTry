package eu.pp.mb.test.ai2ndtry.service;

import eu.pp.mb.test.ai2ndtry.model.Account;
import eu.pp.mb.test.ai2ndtry.model.BankUser;
import eu.pp.mb.test.ai2ndtry.model.Operation;
import eu.pp.mb.test.ai2ndtry.model.OperationStatus;
import eu.pp.mb.test.ai2ndtry.model.OperationType;
import eu.pp.mb.test.ai2ndtry.repository.AccountRepository;
import eu.pp.mb.test.ai2ndtry.repository.BankUserRepository;
import eu.pp.mb.test.ai2ndtry.repository.OperationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
public class OperationService {

    private static final Logger log = LoggerFactory.getLogger(OperationService.class);

    private final OperationRepository operationRepository;
    private final AccountRepository accountRepository;
    private final BankUserRepository bankUserRepository;

    public OperationService(
            OperationRepository operationRepository,
            AccountRepository accountRepository,
            BankUserRepository bankUserRepository
    ) {
        this.operationRepository = operationRepository;
        this.accountRepository = accountRepository;
        this.bankUserRepository = bankUserRepository;
    }

    @Transactional
    public Operation registerAndExecute(
            OperationType type,
            Long sourceAccountId,
            Long targetAccountId,
            BigDecimal amount,
            Long initiatedByUserId,
            LocalDateTime operationAt
    ) {
        if (type == null) {
            throw new BusinessRuleViolationException("type is required");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessRuleViolationException("amount must be greater than zero");
        }

        BankUser initiatedBy = bankUserRepository.findById(initiatedByUserId)
                .orElseThrow(() -> new BusinessRuleViolationException("Initiating user not found"));

        Account sourceAccount = findAccount(sourceAccountId);
        Account targetAccount = findAccount(targetAccountId);
        BigDecimal normalizedAmount = amount.setScale(2, RoundingMode.HALF_UP);
        LocalDateTime executedAt = operationAt != null ? operationAt : LocalDateTime.now().withNano(0);

        Operation operation = Operation.builder()
                .type(type)
                .sourceAccount(sourceAccount)
                .targetAccount(targetAccount)
                .amount(normalizedAmount)
                .operationAt(executedAt)
                .initiatedBy(initiatedBy)
                .status(OperationStatus.ZAREJESTROWANA)
                .build();
        operationRepository.save(operation);

        try {
            applyOperation(operation, sourceAccount, targetAccount, normalizedAmount, executedAt);
            operation.setStatus(OperationStatus.ZREALIZOWANA);
            return operationRepository.save(operation);
        } catch (RuntimeException ex) {
            operation.setStatus(OperationStatus.ANULOWANA);
            operationRepository.save(operation);
            throw ex;
        }
    }

    private void applyOperation(
            Operation operation,
            Account sourceAccount,
            Account targetAccount,
            BigDecimal amount,
            LocalDateTime executedAt
    ) {
        switch (operation.getType()) {
            case WPLATA -> applyDeposit(operation, targetAccount, amount, executedAt);
            case WYPLATA -> applyWithdrawal(operation, sourceAccount, amount, executedAt);
            case TRANSAKCJA -> applyTransfer(operation, sourceAccount, targetAccount, amount, executedAt);
        }
    }

    private void applyDeposit(Operation operation, Account targetAccount, BigDecimal amount, LocalDateTime executedAt) {
        if (targetAccount == null) {
            throw new BusinessRuleViolationException("Deposit requires target account");
        }
        BigDecimal targetBalanceBefore = targetAccount.getBalance();
        targetAccount.setBalance(targetAccount.getBalance().add(amount));
        targetAccount.setLastOperationAt(executedAt);
        accountRepository.save(targetAccount);

        log.info(
                "Deposit executed by user {} (id={}) to account {} owned by user {} (id={}). Amount: {} {}. "
                        + "Account balance: {} -> {}.",
                operation.getInitiatedBy().getLogin(),
                operation.getInitiatedBy().getId(),
                targetAccount.getNumber(),
                targetAccount.getOwner().getLogin(),
                targetAccount.getOwner().getId(),
                amount,
                targetAccount.getCurrency(),
                targetBalanceBefore,
                targetAccount.getBalance()
        );
    }

    private void applyWithdrawal(Operation operation, Account sourceAccount, BigDecimal amount, LocalDateTime executedAt) {
        if (sourceAccount == null) {
            throw new BusinessRuleViolationException("Withdrawal requires source account");
        }
        BigDecimal sourceBalanceBefore = sourceAccount.getBalance();
        ensureEnoughFunds(sourceAccount, amount);
        sourceAccount.setBalance(sourceAccount.getBalance().subtract(amount));
        sourceAccount.setLastOperationAt(executedAt);
        accountRepository.save(sourceAccount);

        log.info(
                "Withdrawal executed by user {} (id={}) from account {}. Amount: {} {}. "
                        + "Account balance: {} -> {}.",
                operation.getInitiatedBy().getLogin(),
                operation.getInitiatedBy().getId(),
                sourceAccount.getNumber(),
                amount,
                sourceAccount.getCurrency(),
                sourceBalanceBefore,
                sourceAccount.getBalance()
        );
    }

    private void applyTransfer(
            Operation operation,
            Account sourceAccount,
            Account targetAccount,
            BigDecimal amount,
            LocalDateTime executedAt
    ) {
        if (sourceAccount == null || targetAccount == null) {
            throw new BusinessRuleViolationException("Transfer requires source and target account");
        }
        if (sourceAccount.getId().equals(targetAccount.getId())) {
            throw new BusinessRuleViolationException("Transfer requires two different accounts");
        }
        if (sourceAccount.getCurrency() != targetAccount.getCurrency()) {
            throw new BusinessRuleViolationException("Transfer requires accounts in the same currency");
        }

        BigDecimal sourceBalanceBefore = sourceAccount.getBalance();
        BigDecimal targetBalanceBefore = targetAccount.getBalance();

        ensureEnoughFunds(sourceAccount, amount);
        sourceAccount.setBalance(sourceAccount.getBalance().subtract(amount));
        targetAccount.setBalance(targetAccount.getBalance().add(amount));
        sourceAccount.setLastOperationAt(executedAt);
        targetAccount.setLastOperationAt(executedAt);
        accountRepository.save(sourceAccount);
        accountRepository.save(targetAccount);

        log.info(
                "Transaction executed by user {} (id={}) from account {} to user {} (id={}) on account {}. Amount: {} {}. "
                        + "Source balance: {} -> {}. Target balance: {} -> {}.",
                operation.getInitiatedBy().getLogin(),
                operation.getInitiatedBy().getId(),
                sourceAccount.getNumber(),
                targetAccount.getOwner().getLogin(),
                targetAccount.getOwner().getId(),
                targetAccount.getNumber(),
                amount,
                sourceAccount.getCurrency(),
                sourceBalanceBefore,
                sourceAccount.getBalance(),
                targetBalanceBefore,
                targetAccount.getBalance()
        );
    }

    private void ensureEnoughFunds(Account sourceAccount, BigDecimal amount) {
        if (sourceAccount.getBalance().subtract(amount).signum() < 0) {
            throw new BusinessRuleViolationException("Operation would reduce account balance below zero");
        }
    }

    private Account findAccount(Long accountId) {
        if (accountId == null) {
            return null;
        }
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new BusinessRuleViolationException("Account not found: " + accountId));
    }
}
