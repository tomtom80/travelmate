package de.evia.travelmate.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PasswordResetIT extends E2ETestBase {

    private static final String TENANT_NAME = "E2E-Reset " + RUN_ID;
    private static final String EMAIL = "reset-" + RUN_ID + "@e2e.test";
    private static final String INITIAL_PASSWORD = "Test1234!";
    private static final String NEW_PASSWORD = "NewPass5678!";

    private static String resetLink;

    @Test
    @Order(1)
    void setUpRegisteredUser() {
        signUpAndLogin(TENANT_NAME, "Reset", "Tester", EMAIL, INITIAL_PASSWORD);
        ensureLoggedOut();
    }

    // --- Journey 1: Forgot-Password link is reachable and shows form ---

    @Test
    @Order(10)
    void forgotPasswordLinkIsVisibleOnLoginPage() {
        navigateAndWait("/oauth2/authorization/keycloak");
        page.waitForURL(url -> url.contains("realms/travelmate"));

        assertThat(page.locator("a:has-text('Passwort vergessen')").isVisible()).isTrue();
    }

    @Test
    @Order(11)
    void clickingForgotPasswordShowsResetForm() {
        page.locator("a:has-text('Passwort vergessen')").click();
        page.waitForLoadState();

        assertThat(page.locator("#username").isVisible()).isTrue();
        assertThat(page.locator("button[type=submit]").isVisible()).isTrue();
    }

    // --- Journey 2: Submit email — generic response, no enumeration ---

    @Test
    @Order(20)
    void submittingKnownEmailShowsGenericConfirmation() {
        page.fill("#username", EMAIL);
        page.locator("button[type=submit]").click();
        page.waitForLoadState();

        assertThat(page.content()).doesNotContain("404");
        assertThat(page.content()).doesNotContain("Kein Benutzer");
    }

    @Test
    @Order(21)
    void unknownEmailShowsSameGenericResponse() {
        navigateAndWait("/oauth2/authorization/keycloak");
        page.waitForURL(url -> url.contains("realms/travelmate"));
        page.locator("a:has-text('Passwort vergessen')").click();
        page.waitForLoadState();

        page.fill("#username", "nobody-" + RUN_ID + "@nowhere.invalid");
        page.locator("button[type=submit]").click();
        page.waitForLoadState();

        assertThat(page.content()).doesNotContain("404");
        assertThat(page.content()).doesNotContain("not found");
        assertThat(page.content()).doesNotContain("Kein Benutzer");
    }

    @Test
    @Order(22)
    void resetEmailArrivesInMailpit() {
        resetLink = waitForMailpitLink(EMAIL, "action-token", 30);

        assertThat(resetLink)
            .as("Password reset email must arrive in Mailpit for %s", EMAIL)
            .isNotNull();
    }

    // --- Journey 3: Follow reset link, set new password ---

    @Test
    @Order(30)
    void resetLinkLeadsToPasswordUpdateForm() {
        navigateAndWait(resetLink);
        page.waitForLoadState();

        assertThat(
            page.locator("#password-new").isVisible()
            || page.locator("input[name='password-new']").isVisible()
        ).as("Password update form must be shown after following the reset link").isTrue();
    }

    @Test
    @Order(31)
    void settingNewPasswordCompletesReset() {
        page.locator("#password-new, input[name='password-new']").first().fill(NEW_PASSWORD);
        page.locator("#password-confirm, input[name='password-confirm']").first().fill(NEW_PASSWORD);
        page.locator("button[type=submit], input[type=submit]").first().click();
        page.waitForLoadState(LoadState.NETWORKIDLE);

        assertThat(page.url()).doesNotContain("reset-credentials");
    }

    // --- Journey 4: Old password rejected, new password accepted ---

    @Test
    @Order(40)
    void oldPasswordNoLongerGrantsAccess() {
        context.clearCookies();
        navigateAndWait("/oauth2/authorization/keycloak");
        page.waitForURL(url -> url.contains("realms/travelmate"));
        page.fill("#username", EMAIL);
        page.fill("#password", INITIAL_PASSWORD);
        page.click("#kc-login");
        page.waitForLoadState();

        assertThat(page.url()).contains("realms/travelmate");
    }

    @Test
    @Order(41)
    void newPasswordGrantsAccessToDashboard() {
        page.fill("#username", EMAIL);
        page.fill("#password", NEW_PASSWORD);
        page.click("#kc-login");
        page.waitForURL(
            url -> !url.contains("realms/travelmate"),
            new Page.WaitForURLOptions().setTimeout(10000)
        );

        assertThat(page.url()).contains("/iam/");
    }

    @Test
    @Order(99)
    void cleanup() {
        ensureLoggedOut();
    }
}
