package de.evia.travelmate.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LandingPageIT extends E2ETestBase {

    @Test
    @Order(1)
    void landingPageLoadsWithGermanContent() {
        navigateAndWait("/iam/");
        final String content = page.content();

        assertThat(page.title()).contains("Travelmate");
        assertThat(content).contains("Gemeinsam Reisen planen");
        assertThat(content).contains("Registrieren");
        assertThat(content).contains("Anmelden");
    }

    @Test
    @Order(2)
    void landingPageSwitchesToEnglish() {
        navigateAndWait("/iam/?lang=en");
        final String content = page.content();

        assertThat(content).contains("Plan trips together");
        assertThat(content).contains("Sign Up");
        assertThat(content).contains("Log In");
    }

    @Test
    @Order(3)
    void landingPageSwitchesBackToGerman() {
        navigateAndWait("/iam/?lang=de");
        final String content = page.content();

        assertThat(content).contains("Gemeinsam Reisen planen");
        assertThat(content).contains("Registrieren");
    }

    @Test
    @Order(4)
    void landingPageShowsFeatures() {
        navigateAndWait("/iam/");
        final String content = page.content();

        assertThat(content).contains("Reisepartei");
        assertThat(content).contains("Reisen organisieren");
        assertThat(content).contains("Abrechnung");
    }

    @Test
    @Order(5)
    void landingPageShowsHowItWorks() {
        final String content = page.content();

        assertThat(content).contains("So funktioniert");
        assertThat(content).contains("Registrieren");
        assertThat(content).contains("Reise planen");
    }

    @Test
    @Order(6)
    void signUpPageIsAccessibleFromLanding() {
        navigateAndWait("/iam/signup");
        final String content = page.content();

        assertThat(content).contains("Reisepartei erstellen");
        assertThat(content).doesNotContain("Whitelabel Error Page");
        assertThat(content).doesNotContain("Sign in to Travelmate");
    }
}
