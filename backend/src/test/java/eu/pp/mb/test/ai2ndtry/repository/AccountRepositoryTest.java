package eu.pp.mb.test.ai2ndtry.repository;

import eu.pp.mb.test.ai2ndtry.model.Account;
import eu.pp.mb.test.ai2ndtry.model.AccountCurrency;
import eu.pp.mb.test.ai2ndtry.model.BankUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class AccountRepositoryTest {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private BankUserRepository bankUserRepository;

    @Test
    void existsByNumberReturnsTrueWhenAccountNumberExists() {
        BankUser owner = bankUserRepository.save(bankUser("jane.doe"));
        accountRepository.save(account("12345", AccountCurrency.PLN, owner));

        assertTrue(accountRepository.existsByNumber("12345"));
    }

    @Test
    void existsByNumberReturnsFalseWhenAccountNumberDoesNotExist() {
        BankUser owner = bankUserRepository.save(bankUser("john.doe"));
        accountRepository.save(account("54321", AccountCurrency.EUR, owner));

        assertFalse(accountRepository.existsByNumber("99999"));
    }

    @Test
    void findAllByCurrencyReturnsOnlyAccountsWithGivenCurrency() {
        BankUser firstOwner = bankUserRepository.save(bankUser("alice.smith"));
        BankUser secondOwner = bankUserRepository.save(bankUser("bob.smith"));
        Account plnAccount = accountRepository.save(account("11111", AccountCurrency.PLN, firstOwner));
        Account usdAccount = accountRepository.save(account("22222", AccountCurrency.USD, firstOwner));
        Account anotherPlnAccount = accountRepository.save(account("33333", AccountCurrency.PLN, secondOwner));

        List<Account> result = accountRepository.findAllByCurrency(AccountCurrency.PLN);

        Set<Long> resultIds = result.stream().map(Account::getId).collect(java.util.stream.Collectors.toSet());

        assertEquals(Set.of(plnAccount.getId(), anotherPlnAccount.getId()), resultIds);
        assertFalse(resultIds.contains(usdAccount.getId()));
    }

    @Test
    void findAllByCurrencyReturnsEmptyListWhenNoAccountsMatch() {
        BankUser owner = bankUserRepository.save(bankUser("charlie.brown"));
        accountRepository.save(account("44444", AccountCurrency.CHF, owner));

        assertTrue(accountRepository.findAllByCurrency(AccountCurrency.NOK).isEmpty());
    }

    private static Account account(String number, AccountCurrency currency, BankUser owner) {
        return Account.builder()
                .number(number)
                .createdAt(LocalDateTime.now())
                .currency(currency)
                .balance(BigDecimal.valueOf(1000))
                .owner(owner)
                .build();
    }

    private static BankUser bankUser(String login) {
        return BankUser.builder()
                .firstName("Test")
                .lastName("User")
                .login(login)
                .password("secret")
                .birthDate(LocalDate.of(1990, 1, 1))
                .createdAt(LocalDateTime.now())
                .build();
    }
}
