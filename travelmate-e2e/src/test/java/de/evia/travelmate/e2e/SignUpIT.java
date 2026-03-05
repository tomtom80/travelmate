package de.evia.travelmate.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SignUpIT extends E2ETestBase {

    private static final String SIGNUP_EMAIL = "signup-" + RUN_ID + "@e2e.test";
    private static final String SIGNUP_PASSWORD = "Test1234!";

    @Test
    @Order(1)
    void signUpFormDisplaysAllFields() {
        navigateAndWait("/iam/signup");

        assertThat(page.locator("#tenantName").isVisible()).isTrue();
        assertThat(page.locator("#firstName").isVisible()).isTrue();
        assertThat(page.locator("#lastName").isVisible()).isTrue();
        assertThat(page.locator("#email").isVisible()).isTrue();
        assertThat(page.locator("#password").isVisible()).isTrue();
        assertThat(page.locator("#passwordConfirm").isVisible()).isTrue();
        assertThat(page.content()).contains("Reisepartei erstellen");
    }

    @Test
    @Order(2)
    void signUpSubmitsAndShowsSuccessPage() {
        navigateAndWait("/iam/signup");

        page.fill("#tenantName", "E2E-SignUp " + RUN_ID);
        page.fill("#firstName", "Anna");
        page.fill("#lastName", "SignUp");
        page.fill("#email", SIGNUP_EMAIL);
        page.fill("#password", SIGNUP_PASSWORD);
        page.fill("#passwordConfirm", SIGNUP_PASSWORD);
        page.click("button[type=submit]");

        page.waitForLoadState();
        assertThat(page.content()).contains("Registrierung erfolgreich");
        assertThat(page.content()).contains("E-Mail");
        assertThat(page.locator("main a[href='/oauth2/authorization/keycloak']").isVisible()).isTrue();
    }

    @Test
    @Order(3)
    void afterSignUpFullLoginFlowWorks() {
        signUpAndLogin("E2E-SignUpLogin " + RUN_ID, "Ben", "Login", "signuplogin-" + RUN_ID + "@e2e.test",
            SIGNUP_PASSWORD);

        navigateAndWait("/iam/dashboard");
        final String content = page.content();
        assertThat(content).contains("E2E-SignUpLogin " + RUN_ID);
        assertThat(content).contains("Ben");
    }

    @Test
    @Order(4)
    void signUpLinkToLoginExists() {
        ensureLoggedOut();
        navigateAndWait("/iam/signup");

        assertThat(page.content()).contains("Bereits registriert");
        assertThat(page.locator("main a[href='/oauth2/authorization/keycloak']").isVisible()).isTrue();
    }
}
