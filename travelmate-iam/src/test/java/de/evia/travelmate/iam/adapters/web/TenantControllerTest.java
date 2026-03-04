package de.evia.travelmate.iam.adapters.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
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

import de.evia.travelmate.common.domain.TenantId;
import de.evia.travelmate.iam.application.TenantService;
import de.evia.travelmate.iam.application.command.CreateTenantCommand;
import de.evia.travelmate.iam.application.representation.TenantRepresentation;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TenantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TenantService tenantService;

    @Test
    void listReturnsTenants() throws Exception {
        final TenantRepresentation tenant = new TenantRepresentation(UUID.randomUUID(), "Test", "Desc");
        when(tenantService.findAll()).thenReturn(List.of(tenant));

        mockMvc.perform(get("/tenants"))
            .andExpect(status().isOk())
            .andExpect(view().name("layout/default"))
            .andExpect(model().attributeExists("tenants"));
    }

    @Test
    void formReturnsFormView() throws Exception {
        mockMvc.perform(get("/tenants/new"))
            .andExpect(status().isOk())
            .andExpect(view().name("layout/default"));
    }

    @Test
    void createRedirectsToDetail() throws Exception {
        final UUID id = UUID.randomUUID();
        when(tenantService.createTenant(any(CreateTenantCommand.class)))
            .thenReturn(new TenantRepresentation(id, "Test", null));

        mockMvc.perform(post("/tenants")
                .param("name", "Test"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("/tenants/*"));
    }

    @Test
    void detailReturnsTenant() throws Exception {
        final UUID id = UUID.randomUUID();
        when(tenantService.findById(any(TenantId.class)))
            .thenReturn(new TenantRepresentation(id, "Test", "Desc"));

        mockMvc.perform(get("/tenants/" + id))
            .andExpect(status().isOk())
            .andExpect(view().name("layout/default"))
            .andExpect(model().attributeExists("tenant"));
    }
}
