package de.evia.travelmate.iam.adapters.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import de.evia.travelmate.iam.application.SignUpService;
import de.evia.travelmate.iam.application.command.SignUpCommand;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SignUpControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SignUpService signUpService;

    @Test
    void showSignUpFormReturnsSignUpView() throws Exception {
        mockMvc.perform(get("/signup"))
            .andExpect(status().isOk())
            .andExpect(view().name("layout/public"))
            .andExpect(model().attribute("view", "signup/form"));
    }

    @Test
    void signUpShowsSuccessPageOnSuccess() throws Exception {
        mockMvc.perform(post("/signup")
                .param("tenantName", "Hüttenurlaub 2026")
                .param("firstName", "Max")
                .param("lastName", "Mustermann")
                .param("email", "max@example.com")
                .param("password", "secureP4ss!")
                .param("passwordConfirm", "secureP4ss!"))
            .andExpect(status().isOk())
            .andExpect(view().name("layout/public"))
            .andExpect(model().attribute("view", "signup/success"));

        verify(signUpService).signUp(any(SignUpCommand.class));
    }

    @Test
    void signUpShowsErrorOnPasswordMismatch() throws Exception {
        mockMvc.perform(post("/signup")
                .param("tenantName", "Hüttenurlaub 2026")
                .param("firstName", "Max")
                .param("lastName", "Mustermann")
                .param("email", "max@example.com")
                .param("password", "secureP4ss!")
                .param("passwordConfirm", "differentPassword"))
            .andExpect(status().isOk())
            .andExpect(view().name("layout/public"))
            .andExpect(model().attribute("view", "signup/form"))
            .andExpect(model().attributeExists("error"));
    }

    @Test
    void signUpShowsErrorOnDuplicateTenantName() throws Exception {
        doThrow(new IllegalArgumentException("A travel group with the name 'Existing' already exists."))
            .when(signUpService).signUp(any(SignUpCommand.class));

        mockMvc.perform(post("/signup")
                .param("tenantName", "Existing")
                .param("firstName", "Max")
                .param("lastName", "Mustermann")
                .param("email", "max@example.com")
                .param("password", "secureP4ss!")
                .param("passwordConfirm", "secureP4ss!"))
            .andExpect(status().isOk())
            .andExpect(view().name("layout/public"))
            .andExpect(model().attribute("view", "signup/form"))
            .andExpect(model().attribute("error", "signup.error.tenantExists"));
    }
}
