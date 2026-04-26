package eu.pp.mb.test.ai2ndtry.controller;

import eu.pp.mb.test.ai2ndtry.model.Account;
import eu.pp.mb.test.ai2ndtry.model.AccountCurrency;
import eu.pp.mb.test.ai2ndtry.model.BankUser;
import eu.pp.mb.test.ai2ndtry.repository.AccountRepository;
import eu.pp.mb.test.ai2ndtry.repository.BankUserRepository;
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
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountRepository accountRepository;
    private final BankUserRepository bankUserRepository;

    public AccountController(AccountRepository accountRepository, BankUserRepository bankUserRepository) {
        this.accountRepository = accountRepository;
        this.bankUserRepository = bankUserRepository;
    }

    @GetMapping
    public List<AccountResponse> findAll() {
        return accountRepository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/{id}")
    public AccountResponse findById(@PathVariable Long id) {
        return accountRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
    }

    @PostMapping
    public ResponseEntity<AccountResponse> create(@RequestBody CreateAccountRequest request) {
        BankUser owner = bankUserRepository.findById(requireValue(request.ownerId(), "ownerId"))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Owner not found"));

        BigDecimal balance = requireValue(request.balance(), "balance");
        if (balance.signum() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "balance cannot be negative");
        }

        String number = requireText(request.number(), "number");
        if (!number.matches("\\d{5}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "number must contain exactly 5 digits");
        }

        Account account = Account.builder()
                .number(number)
                .createdAt(LocalDateTime.now())
                .currency(requireValue(request.currency(), "currency"))
                .balance(balance)
                .lastOperationAt(null)
                .owner(owner)
                .build();

        Account saved = accountRepository.save(account);
        return ResponseEntity.created(URI.create("/api/accounts/" + saved.getId()))
                .body(toResponse(saved));
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " is required");
        }
        return value;
    }

    private <T> T requireValue(T value, String fieldName) {
        if (value == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " is required");
        }
        return value;
    }

    private AccountResponse toResponse(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getNumber(),
                account.getCreatedAt(),
                account.getCurrency(),
                account.getBalance(),
                account.getLastOperationAt(),
                account.getOwner().getId()
        );
    }

    public record CreateAccountRequest(
            String number,
            AccountCurrency currency,
            BigDecimal balance,
            Long ownerId
    ) {
    }

    public record AccountResponse(
            Long id,
            String number,
            LocalDateTime createdAt,
            AccountCurrency currency,
            BigDecimal balance,
            LocalDateTime lastOperationAt,
            Long ownerId
    ) {
    }
}
