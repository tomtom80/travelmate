package de.evia.travelmate.iam.adapters.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.iam.application.AccountService;
import de.evia.travelmate.iam.application.TenantService;
import de.evia.travelmate.iam.application.command.RegisterAccountCommand;
import de.evia.travelmate.iam.application.representation.AccountRepresentation;
import de.evia.travelmate.iam.application.representation.TenantRepresentation;
import de.evia.travelmate.iam.domain.account.AccountId;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountService accountService;

    @MockitoBean
    private TenantService tenantService;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID accountId = UUID.randomUUID();

    @Test
    void listReturnsAccounts() throws Exception {
        when(tenantService.findById(any(TenantId.class)))
            .thenReturn(new TenantRepresentation(tenantId, "Test", null));
        when(accountService.findAllByTenantId(any(TenantId.class)))
            .thenReturn(List.of());

        mockMvc.perform(get("/tenants/" + tenantId + "/accounts"))
            .andExpect(status().isOk())
            .andExpect(view().name("layout/default"))
            .andExpect(model().attributeExists("accounts"));
    }

    @Test
    void formReturnsFormView() throws Exception {
        mockMvc.perform(get("/tenants/" + tenantId + "/accounts/new"))
            .andExpect(status().isOk())
            .andExpect(view().name("layout/default"));
    }

    @Test
    void registerRedirectsToDetail() throws Exception {
        when(accountService.registerAccount(any(RegisterAccountCommand.class)))
            .thenReturn(new AccountRepresentation(accountId, tenantId, "testuser",
                "test@example.com", "Max", "Mustermann", LocalDate.of(1990, 5, 15)));

        mockMvc.perform(post("/tenants/" + tenantId + "/accounts")
                .param("keycloakUserId", "kc-123")
                .param("username", "testuser")
                .param("email", "test@example.com")
                .param("firstName", "Max")
                .param("lastName", "Mustermann")
                .param("dateOfBirth", "1990-05-15"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("/tenants/*/accounts/*"));
    }

    @Test
    void detailReturnsAccount() throws Exception {
        when(tenantService.findById(any(TenantId.class)))
            .thenReturn(new TenantRepresentation(tenantId, "Test", null));
        when(accountService.findById(any(AccountId.class)))
            .thenReturn(new AccountRepresentation(accountId, tenantId, "testuser",
                "test@example.com", "Max", "Mustermann", LocalDate.of(1990, 5, 15)));
        when(accountService.findDependentsByGuardian(any(AccountId.class)))
            .thenReturn(List.of());

        mockMvc.perform(get("/tenants/" + tenantId + "/accounts/" + accountId))
            .andExpect(status().isOk())
            .andExpect(view().name("layout/default"))
            .andExpect(model().attributeExists("account"));
    }
}
