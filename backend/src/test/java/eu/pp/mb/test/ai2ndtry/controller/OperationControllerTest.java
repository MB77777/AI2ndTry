package eu.pp.mb.test.ai2ndtry.controller;

import eu.pp.mb.test.ai2ndtry.model.Account;
import eu.pp.mb.test.ai2ndtry.model.AccountCurrency;
import eu.pp.mb.test.ai2ndtry.model.BankUser;
import eu.pp.mb.test.ai2ndtry.model.Operation;
import eu.pp.mb.test.ai2ndtry.model.OperationStatus;
import eu.pp.mb.test.ai2ndtry.model.OperationType;
import eu.pp.mb.test.ai2ndtry.repository.OperationRepository;
import eu.pp.mb.test.ai2ndtry.service.OperationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OperationController.class)
class OperationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OperationRepository operationRepository;

    @MockitoBean
    private OperationService operationService;

    @Test
    void findAllReturnsAccountNumbersAndOwnerNames() throws Exception {
        BankUser sourceOwner = bankUser(1L, "Anna", "Kowalska");
        BankUser targetOwner = bankUser(2L, "Jan", "Nowak");
        BankUser initiatedBy = bankUser(3L, "Ewa", "Zielinska");
        Account sourceAccount = account(10L, "12345", sourceOwner);
        Account targetAccount = account(20L, "54321", targetOwner);

        when(operationRepository.findAll(any(Sort.class)))
                .thenReturn(List.of(Operation.builder()
                        .id(100L)
                        .type(OperationType.TRANSAKCJA)
                        .sourceAccount(sourceAccount)
                        .targetAccount(targetAccount)
                        .amount(new BigDecimal("150.25"))
                        .operationAt(LocalDateTime.of(2024, 2, 3, 4, 5, 6))
                        .initiatedBy(initiatedBy)
                        .status(OperationStatus.ZREALIZOWANA)
                        .build()));

        mockMvc.perform(get("/api/operations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(100))
                .andExpect(jsonPath("$[0].type").value("TRANSAKCJA"))
                .andExpect(jsonPath("$[0].sourceAccountId").value(10))
                .andExpect(jsonPath("$[0].sourceAccountNumber").value("12345"))
                .andExpect(jsonPath("$[0].sourceOwnerId").value(1))
                .andExpect(jsonPath("$[0].sourceOwnerFirstName").value("Anna"))
                .andExpect(jsonPath("$[0].sourceOwnerLastName").value("Kowalska"))
                .andExpect(jsonPath("$[0].targetAccountId").value(20))
                .andExpect(jsonPath("$[0].targetAccountNumber").value("54321"))
                .andExpect(jsonPath("$[0].targetOwnerId").value(2))
                .andExpect(jsonPath("$[0].targetOwnerFirstName").value("Jan"))
                .andExpect(jsonPath("$[0].targetOwnerLastName").value("Nowak"))
                .andExpect(jsonPath("$[0].currency").value("PLN"))
                .andExpect(jsonPath("$[0].initiatedByUserId").value(3))
                .andExpect(jsonPath("$[0].initiatedByFirstName").value("Ewa"))
                .andExpect(jsonPath("$[0].initiatedByLastName").value("Zielinska"))
                .andExpect(jsonPath("$[0].status").value("ZREALIZOWANA"));
    }

    private static Account account(Long id, String number, BankUser owner) {
        return Account.builder()
                .id(id)
                .number(number)
                .createdAt(LocalDateTime.of(2024, 1, 2, 3, 4, 5))
                .currency(AccountCurrency.PLN)
                .balance(new BigDecimal("100.00"))
                .owner(owner)
                .build();
    }

    private static BankUser bankUser(Long id, String firstName, String lastName) {
        return BankUser.builder()
                .id(id)
                .firstName(firstName)
                .lastName(lastName)
                .login(firstName.toLowerCase() + "." + lastName.toLowerCase())
                .password("secret")
                .birthDate(LocalDate.of(1990, 1, 1))
                .createdAt(LocalDateTime.of(2024, 1, 2, 3, 4, 5))
                .build();
    }
}
