package eu.pp.mb.test.ai2ndtry.controller;

import eu.pp.mb.test.ai2ndtry.model.Account;
import eu.pp.mb.test.ai2ndtry.model.AccountCurrency;
import eu.pp.mb.test.ai2ndtry.model.Operation;
import eu.pp.mb.test.ai2ndtry.model.OperationStatus;
import eu.pp.mb.test.ai2ndtry.model.OperationType;
import eu.pp.mb.test.ai2ndtry.repository.OperationRepository;
import eu.pp.mb.test.ai2ndtry.service.OperationService;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/operations")
public class OperationController {

    private final OperationRepository operationRepository;
    private final OperationService operationService;

    public OperationController(
            OperationRepository operationRepository,
            OperationService operationService
    ) {
        this.operationRepository = operationRepository;
        this.operationService = operationService;
    }

    @GetMapping
    public List<OperationResponse> findAll() {
        return operationRepository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/{id}")
    public OperationResponse findById(@PathVariable Long id) {
        return operationRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Operation not found"));
    }

    @PostMapping
    public ResponseEntity<OperationResponse> create(@RequestBody CreateOperationRequest request) {
        Operation saved = operationService.registerAndExecute(
                requireValue(request.type(), "type"),
                request.sourceAccountId(),
                request.targetAccountId(),
                requireValue(request.amount(), "amount"),
                requireValue(request.initiatedByUserId(), "initiatedByUserId"),
                request.operationAt()
        );
        return ResponseEntity.created(URI.create("/api/operations/" + saved.getId()))
                .body(toResponse(saved));
    }

    private <T> T requireValue(T value, String fieldName) {
        if (value == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " is required");
        }
        return value;
    }

    private OperationResponse toResponse(Operation operation) {
        Account sourceAccount = operation.getSourceAccount();
        Account targetAccount = operation.getTargetAccount();

        return new OperationResponse(
                operation.getId(),
                operation.getType(),
                sourceAccount != null ? sourceAccount.getId() : null,
                sourceAccount != null ? sourceAccount.getNumber() : null,
                sourceAccount != null ? sourceAccount.getOwner().getId() : null,
                sourceAccount != null ? sourceAccount.getOwner().getFirstName() : null,
                sourceAccount != null ? sourceAccount.getOwner().getLastName() : null,
                targetAccount != null ? targetAccount.getId() : null,
                targetAccount != null ? targetAccount.getNumber() : null,
                targetAccount != null ? targetAccount.getOwner().getId() : null,
                targetAccount != null ? targetAccount.getOwner().getFirstName() : null,
                targetAccount != null ? targetAccount.getOwner().getLastName() : null,
                operation.getAmount(),
                operationCurrency(sourceAccount, targetAccount),
                operation.getOperationAt(),
                operation.getInitiatedBy().getId(),
                operation.getInitiatedBy().getFirstName(),
                operation.getInitiatedBy().getLastName(),
                operation.getStatus()
        );
    }

    private AccountCurrency operationCurrency(Account sourceAccount, Account targetAccount) {
        if (sourceAccount != null) {
            return sourceAccount.getCurrency();
        }
        if (targetAccount != null) {
            return targetAccount.getCurrency();
        }
        return null;
    }

    public record CreateOperationRequest(
            OperationType type,
            Long sourceAccountId,
            Long targetAccountId,
            BigDecimal amount,
            LocalDateTime operationAt,
            Long initiatedByUserId
    ) {
    }

    public record OperationResponse(
            Long id,
            OperationType type,
            Long sourceAccountId,
            String sourceAccountNumber,
            Long sourceOwnerId,
            String sourceOwnerFirstName,
            String sourceOwnerLastName,
            Long targetAccountId,
            String targetAccountNumber,
            Long targetOwnerId,
            String targetOwnerFirstName,
            String targetOwnerLastName,
            BigDecimal amount,
            AccountCurrency currency,
            LocalDateTime operationAt,
            Long initiatedByUserId,
            String initiatedByFirstName,
            String initiatedByLastName,
            OperationStatus status
    ) {
    }
}
