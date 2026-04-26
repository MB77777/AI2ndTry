package eu.pp.mb.test.ai2ndtry.repository;

import eu.pp.mb.test.ai2ndtry.model.Account;
import eu.pp.mb.test.ai2ndtry.model.AccountCurrency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccountRepository extends JpaRepository<Account, Long> {

    boolean existsByNumber(String number);

    List<Account> findAllByCurrency(AccountCurrency currency);
}
