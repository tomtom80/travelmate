package de.evia.travelmate.iam.adapters.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import de.evia.travelmate.iam.adapters.mailerlite.MailerliteException;
import de.evia.travelmate.iam.application.marketing.WaitlistSubscriber;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LandingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WaitlistSubscriber waitlistSubscriber;

    @Test
    void getLandingReturns200WithForm() throws Exception {
        final String content = mockMvc.perform(get("/landing"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        assertThat(content).contains("waitlist-form");
        assertThat(content).contains("hx-post");
        assertThat(content).contains("consentGiven");
    }

    @Test
    void validSubmissionCallsSubscriberAndReturnsSuccessFragment() throws Exception {
        final String content = mockMvc.perform(post("/landing/waitlist")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("email", "test@example.com")
                .param("consentGiven", "true"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        verify(waitlistSubscriber).subscribe("test@example.com");
        assertThat(content).contains("Danke!");
    }

    @Test
    void missingConsentRejectsWithoutCallingSubscriber() throws Exception {
        mockMvc.perform(post("/landing/waitlist")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("email", "test@example.com"))
            .andExpect(status().isOk());

        verify(waitlistSubscriber, never()).subscribe("test@example.com");
    }

    @Test
    void invalidEmailRejectsWithoutCallingSubscriber() throws Exception {
        mockMvc.perform(post("/landing/waitlist")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("email", "not-an-email")
                .param("consentGiven", "true"))
            .andExpect(status().isOk());

        verify(waitlistSubscriber, never()).subscribe("not-an-email");
    }

    @Test
    void honeypotFilledSkipsSubscriberAndReturnsSuccess() throws Exception {
        final String content = mockMvc.perform(post("/landing/waitlist")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("email", "bot@example.com")
                .param("consentGiven", "true")
                .param("website", "http://spam.example"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        verify(waitlistSubscriber, never()).subscribe("bot@example.com");
        assertThat(content).contains("Danke!");
    }

    @Test
    void mailerliteErrorReturnsErrorFragment() throws Exception {
        doThrow(new MailerliteException("API down", new RuntimeException()))
            .when(waitlistSubscriber).subscribe("fail@example.com");

        final String content = mockMvc.perform(post("/landing/waitlist")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("email", "fail@example.com")
                .param("consentGiven", "true"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        assertThat(content).contains("schiefgelaufen");
    }

    @Test
    void landingPageIsPublicNoAuthRequired() throws Exception {
        mockMvc.perform(get("/landing"))
            .andExpect(status().isOk());
    }
}
