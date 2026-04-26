package eu.pp.mb.test.ai2ndtry.repository;

import eu.pp.mb.test.ai2ndtry.model.BankUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BankUserRepository extends JpaRepository<BankUser, Long> {

    @Query("""
            select user
            from BankUser user
            where lower(user.firstName) like lower(concat('%', :searchText, '%'))
               or lower(user.lastName) like lower(concat('%', :searchText, '%'))
            """)
    Page<BankUser> searchByFirstNameOrLastName(@Param("searchText") String searchText, Pageable pageable);
}
