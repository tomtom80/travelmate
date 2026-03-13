package de.evia.travelmate.iam.adapters.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import de.evia.travelmate.common.domain.EntityNotFoundException;
import de.evia.travelmate.iam.application.RegistrationService;
import de.evia.travelmate.iam.application.command.CompleteRegistrationCommand;
import de.evia.travelmate.iam.domain.IamTestFixtures;
import de.evia.travelmate.iam.domain.account.Account;
import de.evia.travelmate.iam.domain.account.AccountRepository;
import de.evia.travelmate.iam.domain.registration.InvitationToken;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RegisterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegistrationService registrationService;

    @MockitoBean
    private AccountRepository accountRepository;

    @Test
    void showsFormWithPrefilledData() throws Exception {
        final InvitationToken token = InvitationToken.generate(IamTestFixtures.ACCOUNT_ID);
        final Account account = IamTestFixtures.account();

        when(registrationService.findByTokenValue("valid-token")).thenReturn(token);
        when(accountRepository.findById(IamTestFixtures.ACCOUNT_ID)).thenReturn(Optional.of(account));

        mockMvc.perform(get("/register").param("token", "valid-token"))
            .andExpect(status().isOk())
            .andExpect(view().name("register/form"))
            .andExpect(model().attribute("token", "valid-token"))
            .andExpect(model().attribute("firstName", "Max"))
            .andExpect(model().attribute("lastName", "Mustermann"))
            .andExpect(model().attribute("email", "test@example.com"));
    }

    @Test
    void completesRegistrationAndRedirects() throws Exception {
        mockMvc.perform(post("/register")
                .param("token", "valid-token")
                .param("password", "SecurePass1!"))
            .andExpect(status().isOk())
            .andExpect(view().name("register/success"));

        verify(registrationService).completeRegistration(
            new CompleteRegistrationCommand("valid-token", "SecurePass1!"));
    }

    @Test
    void rejectsExpiredToken() throws Exception {
        doThrow(new IllegalStateException("Token has expired."))
            .when(registrationService).completeRegistration(any(CompleteRegistrationCommand.class));

        mockMvc.perform(post("/register")
                .param("token", "expired-token")
                .param("password", "SecurePass1!"))
            .andExpect(status().isOk())
            .andExpect(view().name("register/error"))
            .andExpect(model().attribute("error", "register.error.expired"));
    }

    @Test
    void rejectsInvalidPassword() throws Exception {
        doThrow(new IllegalArgumentException("Password too short"))
            .when(registrationService).completeRegistration(any(CompleteRegistrationCommand.class));

        mockMvc.perform(post("/register")
                .param("token", "valid-token")
                .param("password", "short"))
            .andExpect(status().isOk())
            .andExpect(view().name("register/form"))
            .andExpect(model().attribute("error", "register.error.invalidPassword"))
            .andExpect(model().attribute("token", "valid-token"));
    }

    @Test
    void showsErrorForInvalidToken() throws Exception {
        when(registrationService.findByTokenValue("unknown-token"))
            .thenThrow(new EntityNotFoundException("InvitationToken", "unknown-token"));

        mockMvc.perform(get("/register").param("token", "unknown-token"))
            .andExpect(status().isOk())
            .andExpect(view().name("register/error"))
            .andExpect(model().attribute("error", "register.error.invalidToken"));
    }

    @Test
    void showsErrorForExpiredTokenOnGet() throws Exception {
        final InvitationToken token = InvitationToken.generate(IamTestFixtures.ACCOUNT_ID);
        // Use reflection to set expiresAt to past — simulating an expired token
        final java.lang.reflect.Field expiresAtField = InvitationToken.class.getDeclaredField("expiresAt");
        expiresAtField.setAccessible(true);
        expiresAtField.set(token, java.time.LocalDateTime.now().minusHours(1));

        when(registrationService.findByTokenValue("expired-token")).thenReturn(token);

        mockMvc.perform(get("/register").param("token", "expired-token"))
            .andExpect(status().isOk())
            .andExpect(view().name("register/error"))
            .andExpect(model().attribute("error", "register.error.expired"));
    }
}
