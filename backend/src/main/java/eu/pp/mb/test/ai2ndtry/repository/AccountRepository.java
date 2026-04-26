package eu.pp.mb.test.ai2ndtry.repository;

import eu.pp.mb.test.ai2ndtry.model.Account;
import eu.pp.mb.test.ai2ndtry.model.AccountCurrency;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AccountRepository extends JpaRepository<Account, Long> {

    boolean existsByNumber(String number);

    List<Account> findAllByCurrency(AccountCurrency currency);

    List<Account> findAllByOwnerId(Long ownerId, Sort sort);

    @Query("""
            select account from Account account
            join fetch account.owner owner
            where (:ownerId is null or owner.id = :ownerId)
              and (:number is null or lower(account.number) like lower(concat('%', :number, '%')))
              and (:ownerFirstName is null or lower(owner.firstName) like lower(concat('%', :ownerFirstName, '%')))
              and (:ownerLastName is null or lower(owner.lastName) like lower(concat('%', :ownerLastName, '%')))
            """)
    List<Account> findAllMatching(
            @Param("ownerId") Long ownerId,
            @Param("number") String number,
            @Param("ownerFirstName") String ownerFirstName,
            @Param("ownerLastName") String ownerLastName,
            Sort sort
    );
}
