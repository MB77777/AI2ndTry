package eu.pp.mb.test.ai2ndtry.service;

import eu.pp.mb.test.ai2ndtry.model.Account;
import eu.pp.mb.test.ai2ndtry.model.AccountCurrency;
import eu.pp.mb.test.ai2ndtry.model.BankUser;
import eu.pp.mb.test.ai2ndtry.repository.AccountRepository;
import eu.pp.mb.test.ai2ndtry.repository.BankUserRepository;
import eu.pp.mb.test.ai2ndtry.repository.OperationRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Component
public class DemoDataInitializer implements ApplicationRunner {

    private static final List<String> FIRST_NAMES = List.of(
            "Adam", "Bartosz", "Celina", "Damian", "Eliza",
            "Filip", "Grzegorz", "Hanna", "Igor", "Joanna",
            "Kacper", "Laura", "Marek", "Natalia", "Oskar",
            "Paulina", "Rafal", "Sandra", "Tomasz", "Wiktoria"
    );

    private static final List<String> LAST_NAMES = List.of(
            "Nowak", "Kowalski", "Wisniewski", "Wojcik", "Kowalczyk",
            "Kaminska", "Lewandowski", "Zielinska", "Szymanski", "Dabrowska",
            "Kozlowski", "Jankowski", "Mazur", "Krawczyk", "Piotrowska",
            "Grabowski", "Nowicka", "Pawlowski", "Michalska", "Zajac"
    );

    private final BankUserRepository bankUserRepository;
    private final AccountRepository accountRepository;
    private final OperationRepository operationRepository;
    private final Random random = new Random();

    public DemoDataInitializer(
            BankUserRepository bankUserRepository,
            AccountRepository accountRepository,
            OperationRepository operationRepository
    ) {
        this.bankUserRepository = bankUserRepository;
        this.accountRepository = accountRepository;
        this.operationRepository = operationRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (bankUserRepository.count() > 0 || accountRepository.count() > 0 || operationRepository.count() > 0) {
            return;
        }

        List<String> firstNames = new ArrayList<>(FIRST_NAMES);
        Collections.shuffle(firstNames, random);

        for (int i = 0; i < 20; i++) {
            String firstName = firstNames.get(i);
            String lastName = LAST_NAMES.get(random.nextInt(LAST_NAMES.size()));
            LocalDateTime createdAt = LocalDateTime.now()
                    .minusDays(random.nextInt(3650))
                    .withNano(0);

            BankUser user = BankUser.builder()
                    .firstName(firstName)
                    .lastName(lastName)
                    .login(buildUniqueLogin(firstName, lastName, i))
                    .password("password-" + (i + 1))
                    .birthDate(randomBirthDate())
                    .createdAt(createdAt)
                    .build();
            BankUser savedUser = bankUserRepository.save(user);

            int accountCount = random.nextInt(3) + 1;
            for (int j = 0; j < accountCount; j++) {
                accountRepository.save(Account.builder()
                        .number(generateAccountNumber())
                        .createdAt(createdAt.plusDays(j))
                        .currency(randomCurrency())
                        .balance(randomBalance())
                        .lastOperationAt(null)
                        .owner(savedUser)
                        .build());
            }
        }
    }

    private String buildUniqueLogin(String firstName, String lastName, int index) {
        String normalized = (firstName + "." + lastName + "." + (index + 1)).toLowerCase();
        return normalized.replace('ą', 'a')
                .replace('ć', 'c')
                .replace('ę', 'e')
                .replace('ł', 'l')
                .replace('ń', 'n')
                .replace('ó', 'o')
                .replace('ś', 's')
                .replace('ż', 'z')
                .replace('ź', 'z');
    }

    private LocalDate randomBirthDate() {
        return LocalDate.now()
                .minusYears(18 + random.nextInt(50))
                .minusDays(random.nextInt(365));
    }

    private AccountCurrency randomCurrency() {
        AccountCurrency[] currencies = AccountCurrency.values();
        return currencies[random.nextInt(currencies.length)];
    }

    private BigDecimal randomBalance() {
        return BigDecimal.valueOf(random.nextDouble() * 100000)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private String generateAccountNumber() {
        String number;
        do {
            number = String.format("%05d", random.nextInt(100000));
        } while (accountRepository.existsByNumber(number));
        return number;
    }
}
