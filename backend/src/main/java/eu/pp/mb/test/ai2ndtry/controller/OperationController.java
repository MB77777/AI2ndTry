package eu.pp.mb.test.ai2ndtry.controller;

import eu.pp.mb.test.ai2ndtry.model.Account;
import eu.pp.mb.test.ai2ndtry.model.Operation;
import eu.pp.mb.test.ai2ndtry.model.OperationStatus;
import eu.pp.mb.test.ai2ndtry.model.OperationType;
import eu.pp.mb.test.ai2ndtry.repository.AccountRepository;
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
    private final AccountRepository accountRepository;
    private final OperationService operationService;

    public OperationController(
            OperationRepository operationRepository,
            AccountRepository accountRepository,
            OperationService operationService
    ) {
        this.operationRepository = operationRepository;
        this.accountRepository = accountRepository;
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
        return new OperationResponse(
                operation.getId(),
                operation.getType(),
                operation.getSourceAccount() != null ? operation.getSourceAccount().getId() : null,
                operation.getTargetAccount() != null ? operation.getTargetAccount().getId() : null,
                operation.getAmount(),
                operation.getOperationAt(),
                operation.getInitiatedBy().getId(),
                operation.getStatus()
        );
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
            Long targetAccountId,
            BigDecimal amount,
            LocalDateTime operationAt,
            Long initiatedByUserId,
            OperationStatus status
    ) {
    }
}
