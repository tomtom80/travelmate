package de.evia.travelmate.iam.adapters.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import de.evia.travelmate.iam.application.AccountService;
import de.evia.travelmate.iam.application.command.AddDependentCommand;
import de.evia.travelmate.iam.application.representation.DependentRepresentation;
import de.evia.travelmate.iam.domain.account.AccountId;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DependentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountService accountService;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID accountId = UUID.randomUUID();

    @Test
    void listReturnsDependents() throws Exception {
        when(accountService.findDependentsByGuardian(any(AccountId.class)))
            .thenReturn(List.of());

        mockMvc.perform(get("/tenants/" + tenantId + "/accounts/" + accountId + "/dependents"))
            .andExpect(status().isOk())
            .andExpect(view().name("dependent/list"));
    }

    @Test
    void addReturnsDependentsList() throws Exception {
        final DependentRepresentation dependent = new DependentRepresentation(
            UUID.randomUUID(), tenantId, accountId, "Lena", "Mustermann");
        when(accountService.addDependent(any(AddDependentCommand.class))).thenReturn(dependent);
        when(accountService.findDependentsByGuardian(any(AccountId.class)))
            .thenReturn(List.of(dependent));

        mockMvc.perform(post("/tenants/" + tenantId + "/accounts/" + accountId + "/dependents")
                .param("firstName", "Lena")
                .param("lastName", "Mustermann"))
            .andExpect(status().isOk())
            .andExpect(view().name("dependent/list"));
    }
}
