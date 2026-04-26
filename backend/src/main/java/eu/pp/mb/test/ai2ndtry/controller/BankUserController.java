package eu.pp.mb.test.ai2ndtry.controller;

import eu.pp.mb.test.ai2ndtry.model.BankUser;
import eu.pp.mb.test.ai2ndtry.repository.BankUserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class BankUserController {

    private final BankUserRepository bankUserRepository;

    public BankUserController(BankUserRepository bankUserRepository) {
        this.bankUserRepository = bankUserRepository;
    }

    @GetMapping
    public BankUserPageResponse findAll(
            @PageableDefault(sort = "lastName") Pageable pageable,
            @RequestParam(defaultValue = "") String search
    ) {
        Page<BankUser> users = search == null || search.isBlank()
                ? bankUserRepository.findAll(pageable)
                : bankUserRepository.searchByFirstNameOrLastName(search.trim(), pageable);

        Page<BankUserResponse> responsePage = users.map(this::toResponse);
        return new BankUserPageResponse(
                responsePage.getContent(),
                users.getNumber(),
                users.getSize(),
                users.getTotalElements(),
                users.getTotalPages(),
                users.isFirst(),
                users.isLast(),
                users.getSort().stream()
                        .map(order -> new SortResponse(order.getProperty(), order.getDirection().name()))
                        .toList()
        );
    }

    @GetMapping("/{id}")
    public BankUserResponse findById(@PathVariable Long id) {
        return bankUserRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    @PostMapping
    public ResponseEntity<BankUserResponse> create(@RequestBody CreateBankUserRequest request) {
        BankUser bankUser = BankUser.builder()
                .firstName(requireText(request.firstName(), "firstName"))
                .lastName(requireText(request.lastName(), "lastName"))
                .login(requireText(request.login(), "login"))
                .password(requireText(request.password(), "password"))
                .birthDate(requireValue(request.birthDate(), "birthDate"))
                .createdAt(LocalDateTime.now())
                .build();

        BankUser saved = bankUserRepository.save(bankUser);
        return ResponseEntity.created(URI.create("/api/users/" + saved.getId()))
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

    private BankUserResponse toResponse(BankUser bankUser) {
        return new BankUserResponse(
                bankUser.getId(),
                bankUser.getFirstName(),
                bankUser.getLastName(),
                bankUser.getLogin(),
                bankUser.getBirthDate(),
                bankUser.getCreatedAt()
        );
    }

    public record CreateBankUserRequest(
            String firstName,
            String lastName,
            String login,
            String password,
            LocalDate birthDate
    ) {
    }

    public record BankUserResponse(
            Long id,
            String firstName,
            String lastName,
            String login,
            LocalDate birthDate,
            LocalDateTime createdAt
    ) {
    }

    public record BankUserPageResponse(
            List<BankUserResponse> content,
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean first,
            boolean last,
            List<SortResponse> sort
    ) {
    }

    public record SortResponse(
            String property,
            String direction
    ) {
    }
}
