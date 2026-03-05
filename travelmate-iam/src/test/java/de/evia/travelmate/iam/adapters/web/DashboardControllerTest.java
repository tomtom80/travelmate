package de.evia.travelmate.iam.adapters.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.iam.application.AccountService;
import de.evia.travelmate.iam.application.TenantService;
import de.evia.travelmate.iam.application.command.AddDependentCommand;
import de.evia.travelmate.iam.application.representation.AccountRepresentation;
import de.evia.travelmate.iam.application.representation.DependentRepresentation;
import de.evia.travelmate.iam.application.representation.TenantRepresentation;
import de.evia.travelmate.iam.domain.account.Account;
import de.evia.travelmate.iam.domain.account.AccountId;
import de.evia.travelmate.iam.domain.account.AccountRepository;
import de.evia.travelmate.iam.domain.account.Email;
import de.evia.travelmate.iam.domain.account.FullName;
import de.evia.travelmate.iam.domain.account.KeycloakUserId;
import de.evia.travelmate.iam.domain.account.Username;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DashboardControllerTest {

    private static final UUID TENANT_UUID = UUID.randomUUID();
    private static final UUID ACCOUNT_UUID = UUID.randomUUID();
    private static final String KEYCLOAK_USER_ID = "kc-user-123";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountService accountService;

    @MockitoBean
    private TenantService tenantService;

    @MockitoBean
    private AccountRepository accountRepository;

    @Test
    void dashboardShowsTenantAndMembers() throws Exception {
        final Account account = createAccount();
        when(accountRepository.findByKeycloakUserId(new KeycloakUserId(KEYCLOAK_USER_ID)))
            .thenReturn(Optional.of(account));
        when(tenantService.findById(new TenantId(TENANT_UUID)))
            .thenReturn(new TenantRepresentation(TENANT_UUID, "Hüttenurlaub 2026", ""));
        when(accountService.findAllByTenantId(new TenantId(TENANT_UUID)))
            .thenReturn(List.of(new AccountRepresentation(ACCOUNT_UUID, TENANT_UUID,
                "max@example.com", "max@example.com", "Max", "Mustermann", null)));
        when(accountService.findDependentsByTenantId(any(TenantId.class)))
            .thenReturn(List.of());

        mockMvc.perform(get("/dashboard")
                .with(jwt().jwt(j -> j.subject(KEYCLOAK_USER_ID))))
            .andExpect(status().isOk())
            .andExpect(view().name("layout/default"))
            .andExpect(model().attribute("view", "dashboard/index"))
            .andExpect(model().attributeExists("tenant"))
            .andExpect(model().attributeExists("members"))
            .andExpect(model().attributeExists("dependents"));
    }

    @Test
    void dashboardRedirectsToSignupWhenAccountNotFound() throws Exception {
        when(accountRepository.findByKeycloakUserId(any(KeycloakUserId.class)))
            .thenReturn(Optional.empty());

        mockMvc.perform(get("/dashboard")
                .with(jwt().jwt(j -> j.subject("unknown-user"))))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/signup"));
    }

    @Test
    void addCompanionCreatesAndReturnsFragment() throws Exception {
        final Account account = createAccount();
        when(accountRepository.findByKeycloakUserId(new KeycloakUserId(KEYCLOAK_USER_ID)))
            .thenReturn(Optional.of(account));
        when(accountService.addDependent(any(AddDependentCommand.class)))
            .thenReturn(new DependentRepresentation(UUID.randomUUID(), TENANT_UUID, ACCOUNT_UUID, "Lina", "Mustermann", null));
        when(accountService.findDependentsByTenantId(new TenantId(TENANT_UUID)))
            .thenReturn(List.of(new DependentRepresentation(UUID.randomUUID(), TENANT_UUID, ACCOUNT_UUID, "Lina", "Mustermann", null)));

        mockMvc.perform(post("/dashboard/companions")
                .with(jwt().jwt(j -> j.subject(KEYCLOAK_USER_ID)))
                .param("firstName", "Lina")
                .param("lastName", "Mustermann"))
            .andExpect(status().isOk())
            .andExpect(view().name("dashboard/companions :: companionList"))
            .andExpect(model().attributeExists("dependents"));

        verify(accountService).addDependent(
            new AddDependentCommand(TENANT_UUID, ACCOUNT_UUID, "Lina", "Mustermann", null));
    }

    private Account createAccount() {
        return new Account(
            new AccountId(ACCOUNT_UUID),
            new TenantId(TENANT_UUID),
            new KeycloakUserId(KEYCLOAK_USER_ID),
            new Username("max@example.com"),
            new Email("max@example.com"),
            new FullName("Max", "Mustermann"),
            null
        );
    }
}
