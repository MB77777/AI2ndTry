package eu.pp.mb.test.ai2ndtry.controller;

import eu.pp.mb.test.ai2ndtry.model.BankUser;
import eu.pp.mb.test.ai2ndtry.repository.BankUserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BankUserController.class)
class BankUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BankUserRepository bankUserRepository;

    @Test
    void findAllReturnsPagedUsersWithDefaultLastNameSort() throws Exception {
        when(bankUserRepository.findAll(any(Pageable.class)))
                .thenAnswer(invocation -> new PageImpl<>(
                        List.of(
                                bankUser(1L, "Anna", "Kowalska", "anna.kowalska"),
                                bankUser(2L, "Jan", "Nowak", "jan.nowak"),
                                bankUser(3L, "Ewa", "Zielinska", "ewa.zielinska")
                        ),
                        invocation.getArgument(0),
                        3
                ));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].firstName").value("Anna"))
                .andExpect(jsonPath("$.content[0].lastName").value("Kowalska"))
                .andExpect(jsonPath("$.content[0].login").value("anna.kowalska"))
                .andExpect(jsonPath("$.content[0].birthDate").value("1990-01-01"))
                .andExpect(jsonPath("$.content[0].createdAt").value("2024-01-02T03:04:05"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true))
                .andExpect(jsonPath("$.sort[0].property").value("lastName"))
                .andExpect(jsonPath("$.sort[0].direction").value("ASC"));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(bankUserRepository).findAll(pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();
        assertEquals(0, pageable.getPageNumber());
        assertEquals(10, pageable.getPageSize());
        assertEquals("lastName", pageable.getSort().iterator().next().getProperty());
    }

    @Test
    void findAllPassesRequestedPageSortAndSearchToRepository() throws Exception {
        when(bankUserRepository.searchByFirstNameOrLastName(any(String.class), any(Pageable.class)))
                .thenAnswer(invocation -> new PageImpl<>(
                        List.of(bankUser(2L, "Jan", "Nowak", "jan.nowak")),
                        invocation.getArgument(1),
                        4
                ));

        mockMvc.perform(get("/api/users")
                        .param("page", "1")
                        .param("size", "2")
                        .param("sort", "firstName,desc")
                        .param("search", " jan "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].firstName").value("Jan"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.sort[0].property").value("firstName"))
                .andExpect(jsonPath("$.sort[0].direction").value("DESC"));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(bankUserRepository).searchByFirstNameOrLastName(org.mockito.ArgumentMatchers.eq("jan"), pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();
        assertEquals(1, pageable.getPageNumber());
        assertEquals(2, pageable.getPageSize());
        assertEquals("firstName", pageable.getSort().iterator().next().getProperty());
        assertEquals("DESC", pageable.getSort().iterator().next().getDirection().name());
    }

    private static BankUser bankUser(Long id, String firstName, String lastName, String login) {
        return BankUser.builder()
                .id(id)
                .firstName(firstName)
                .lastName(lastName)
                .login(login)
                .password("secret")
                .birthDate(LocalDate.of(1990, 1, 1))
                .createdAt(LocalDateTime.of(2024, 1, 2, 3, 4, 5))
                .build();
    }
}
