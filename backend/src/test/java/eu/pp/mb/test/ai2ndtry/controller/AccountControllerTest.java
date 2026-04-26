package eu.pp.mb.test.ai2ndtry.controller;

import eu.pp.mb.test.ai2ndtry.model.Account;
import eu.pp.mb.test.ai2ndtry.model.AccountCurrency;
import eu.pp.mb.test.ai2ndtry.model.BankUser;
import eu.pp.mb.test.ai2ndtry.repository.AccountRepository;
import eu.pp.mb.test.ai2ndtry.repository.BankUserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountRepository accountRepository;

    @MockitoBean
    private BankUserRepository bankUserRepository;

    @Test
    void findAllFiltersAccountsByOwnerId() throws Exception {
        BankUser owner = bankUser(7L);
        when(accountRepository.findAllMatching(eq(7L), isNull(), isNull(), isNull(), any(Sort.class)))
                .thenReturn(List.of(account(11L, "12345", AccountCurrency.PLN, owner)));

        mockMvc.perform(get("/api/accounts").param("ownerId", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(11))
                .andExpect(jsonPath("$[0].number").value("12345"))
                .andExpect(jsonPath("$[0].currency").value("PLN"))
                .andExpect(jsonPath("$[0].balance").value(100.25))
                .andExpect(jsonPath("$[0].ownerId").value(7))
                .andExpect(jsonPath("$[0].ownerFirstName").value("Anna"))
                .andExpect(jsonPath("$[0].ownerLastName").value("Kowalska"));

        ArgumentCaptor<Sort> sortCaptor = ArgumentCaptor.forClass(Sort.class);
        verify(accountRepository).findAllMatching(eq(7L), isNull(), isNull(), isNull(), sortCaptor.capture());
        Sort.Order order = sortCaptor.getValue().iterator().next();
        assertEquals("id", order.getProperty());
        assertEquals(Sort.Direction.ASC, order.getDirection());
    }

    @Test
    void findAllReturnsAllAccountsWhenOwnerIdIsMissing() throws Exception {
        BankUser owner = bankUser(7L);
        when(accountRepository.findAllMatching(isNull(), isNull(), isNull(), isNull(), any(Sort.class)))
                .thenReturn(List.of(account(11L, "12345", AccountCurrency.PLN, owner)));

        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(11))
                .andExpect(jsonPath("$[0].ownerId").value(7))
                .andExpect(jsonPath("$[0].ownerFirstName").value("Anna"))
                .andExpect(jsonPath("$[0].ownerLastName").value("Kowalska"));

        ArgumentCaptor<Sort> sortCaptor = ArgumentCaptor.forClass(Sort.class);
        verify(accountRepository).findAllMatching(isNull(), isNull(), isNull(), isNull(), sortCaptor.capture());
        Sort.Order order = sortCaptor.getValue().iterator().next();
        assertEquals("id", order.getProperty());
        assertEquals(Sort.Direction.ASC, order.getDirection());
    }

    @Test
    void findAllPassesAccountAndOwnerSearchFilters() throws Exception {
        BankUser owner = bankUser(7L);
        when(accountRepository.findAllMatching(isNull(), eq("123"), eq("Anna"), eq("Kow"), any(Sort.class)))
                .thenReturn(List.of(account(11L, "12345", AccountCurrency.PLN, owner)));

        mockMvc.perform(get("/api/accounts")
                        .param("number", " 123 ")
                        .param("ownerFirstName", "Anna")
                        .param("ownerLastName", "Kow"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].number").value("12345"))
                .andExpect(jsonPath("$[0].ownerFirstName").value("Anna"))
                .andExpect(jsonPath("$[0].ownerLastName").value("Kowalska"));

        verify(accountRepository).findAllMatching(isNull(), eq("123"), eq("Anna"), eq("Kow"), any(Sort.class));
    }

    @Test
    void findCurrenciesReturnsAllAccountCurrencies() throws Exception {
        mockMvc.perform(get("/api/accounts/currencies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("PLN"))
                .andExpect(jsonPath("$[1]").value("EUR"))
                .andExpect(jsonPath("$[2]").value("USD"))
                .andExpect(jsonPath("$[3]").value("CHF"))
                .andExpect(jsonPath("$[4]").value("NOK"));
    }

    @Test
    void createAccountReturnsCreatedAccount() throws Exception {
        BankUser owner = bankUser(7L);
        when(bankUserRepository.findById(7L)).thenReturn(Optional.of(owner));
        when(accountRepository.existsByNumber("54321")).thenReturn(false);
        when(accountRepository.save(any(Account.class)))
                .thenReturn(account(12L, "54321", AccountCurrency.EUR, owner));

        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "number": "54321",
                                  "currency": "EUR",
                                  "balance": 250.50,
                                  "ownerId": 7
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/accounts/12"))
                .andExpect(jsonPath("$.id").value(12))
                .andExpect(jsonPath("$.number").value("54321"))
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.ownerId").value(7))
                .andExpect(jsonPath("$.ownerFirstName").value("Anna"))
                .andExpect(jsonPath("$.ownerLastName").value("Kowalska"));
    }

    @Test
    void createAccountRejectsDuplicateNumber() throws Exception {
        BankUser owner = bankUser(7L);
        when(bankUserRepository.findById(7L)).thenReturn(Optional.of(owner));
        when(accountRepository.existsByNumber("54321")).thenReturn(true);

        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "number": "54321",
                                  "currency": "EUR",
                                  "balance": 250.50,
                                  "ownerId": 7
                                }
                                """))
                .andExpect(status().isConflict());
    }

    private static Account account(Long id, String number, AccountCurrency currency, BankUser owner) {
        return Account.builder()
                .id(id)
                .number(number)
                .createdAt(LocalDateTime.of(2024, 1, 2, 3, 4, 5))
                .currency(currency)
                .balance(new BigDecimal("100.25"))
                .lastOperationAt(null)
                .owner(owner)
                .build();
    }

    private static BankUser bankUser(Long id) {
        return BankUser.builder()
                .id(id)
                .firstName("Anna")
                .lastName("Kowalska")
                .login("anna.kowalska")
                .password("secret")
                .birthDate(LocalDate.of(1990, 1, 1))
                .createdAt(LocalDateTime.of(2024, 1, 2, 3, 4, 5))
                .build();
    }
}
