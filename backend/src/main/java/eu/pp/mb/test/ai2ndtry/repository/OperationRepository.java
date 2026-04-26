package eu.pp.mb.test.ai2ndtry.repository;

import eu.pp.mb.test.ai2ndtry.model.Operation;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OperationRepository extends JpaRepository<Operation, Long> {

    @Override
    @EntityGraph(attributePaths = {
            "sourceAccount.owner",
            "targetAccount.owner",
            "initiatedBy"
    })
    List<Operation> findAll(Sort sort);

    @Override
    @EntityGraph(attributePaths = {
            "sourceAccount.owner",
            "targetAccount.owner",
            "initiatedBy"
    })
    Optional<Operation> findById(Long id);
}
