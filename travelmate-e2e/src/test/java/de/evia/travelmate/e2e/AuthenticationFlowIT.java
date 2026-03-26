package de.evia.travelmate.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthenticationFlowIT extends E2ETestBase {

    private static final String TENANT_NAME = "E2E-Auth " + RUN_ID;
    private static final String EMAIL = "auth-" + RUN_ID + "@e2e.test";
    private static final String PASSWORD = "Test1234!";

    // --- Journey 1: Landing → Keycloak Login page ---

    @Test
    @Order(1)
    void clickLoginOnLandingNavigatesToKeycloakLogin() {
        navigateAndWait("/iam/");
        page.locator("a[href='/oauth2/authorization/keycloak']").first().click();
        page.waitForURL(url -> url.contains("realms/travelmate"));

        assertThat(page.url()).contains("realms/travelmate");
    }

    @Test
    @Order(2)
    void keycloakLoginPageShowsTravelmatebranding() {
        final String content = page.content();

        assertThat(content).contains("Travelmate");
        assertThat(page.locator("nav .nav-brand img").isVisible()).isTrue();
        assertThat(page.locator("footer").textContent()).contains("Travelmate");
    }

    @Test
    @Order(3)
    void keycloakLoginPageHasLocaleSwitcher() {
        assertThat(page.locator("nav summary:has-text('Sprache')").isVisible()
            || page.locator("nav summary:has-text('Language')").isVisible()).isTrue();
    }

    @Test
    @Order(4)
    void keycloakLoginPageHasFormFields() {
        assertThat(page.locator("#username").isVisible()).isTrue();
        assertThat(page.locator("#password").isVisible()).isTrue();
        assertThat(page.locator("#kc-login").isVisible()).isTrue();
    }

    @Test
    @Order(5)
    void keycloakLoginPageHasRememberMe() {
        assertThat(page.locator("#rememberMe").isVisible()).isTrue();
    }

    // --- Journey 2: Login page → Registration link → Signup page ---

    @Test
    @Order(10)
    void registrationLinkOnLoginPointsToTravelmate() {
        final var registrationLink = page.locator("a:has-text('Registrieren')").last();

        assertThat(registrationLink.isVisible()).isTrue();
        assertThat(registrationLink.getAttribute("href")).contains("/iam/signup");
    }

    @Test
    @Order(11)
    void clickRegistrationLinkNavigatesToSignUp() {
        page.locator("a:has-text('Registrieren')").last().click();
        page.waitForURL(url -> url.contains("/iam/signup"));

        assertThat(page.content()).contains("Reisepartei erstellen");
    }

    // --- Journey 3: Login page → Forgot Password → Back to Login ---

    @Test
    @Order(20)
    void navigateToLoginAndClickForgotPassword() {
        navigateAndWait("/iam/");
        page.locator("a[href='/oauth2/authorization/keycloak']").first().click();
        page.waitForURL(url -> url.contains("realms/travelmate"));

        page.locator("a:has-text('Passwort vergessen')").click();
        page.waitForLoadState();

        assertThat(page.content()).contains("Passwort vergessen");
    }

    @Test
    @Order(21)
    void forgotPasswordPageShowsFormAndBackLink() {
        assertThat(page.locator("#username").isVisible()).isTrue();
        assertThat(page.locator("button[type=submit]").isVisible()).isTrue();
        assertThat(page.locator("a:has-text('Anmeldung')").isVisible()).isTrue();
    }

    @Test
    @Order(22)
    void forgotPasswordPageShowsTravelmateBranding() {
        assertThat(page.locator("nav .nav-brand img").isVisible()).isTrue();
        assertThat(page.locator("footer").textContent()).contains("Travelmate");
    }

    @Test
    @Order(23)
    void backToLoginLinkNavigatesToLogin() {
        page.locator("a:has-text('Anmeldung')").click();
        page.waitForLoadState();

        assertThat(page.locator("#username").isVisible()).isTrue();
        assertThat(page.locator("#password").isVisible()).isTrue();
        assertThat(page.locator("#kc-login").isVisible()).isTrue();
    }

    // --- Journey 4: Full Login → Dashboard → Logout → Landing ---

    @Test
    @Order(30)
    void signUpTestUser() {
        signUpAndLogin(TENANT_NAME, "Auth", "Tester", EMAIL, PASSWORD);

        navigateAndWait("/iam/dashboard");
        assertThat(page.content()).contains(TENANT_NAME);
    }

    @Test
    @Order(31)
    void logoutRedirectsToLandingPage() {
        navigateAndWait("/iam/dashboard");
        page.locator("a.nav-logout-btn").click();
        page.waitForURL(url -> url.contains(BASE_URL) && !url.contains("/dashboard"), new com.microsoft.playwright.Page.WaitForURLOptions().setTimeout(10000));
        page.waitForLoadState();

        final String url = page.url();
        assertThat(url).doesNotContain("/dashboard");
        assertThat(url).doesNotContain("logout-confirm");
    }

    @Test
    @Order(32)
    void afterLogoutLandingPageIsShown() {
        final String content = page.content();

        assertThat(content).contains("Travelmate");
        assertThat(content).contains("Registrieren");
        assertThat(content).contains("Anmelden");
    }

    @Test
    @Order(33)
    void afterLogoutDashboardRequiresLogin() {
        navigateAndWait("/iam/dashboard");

        assertThat(page.url()).contains("realms/travelmate");
    }

    // --- Journey 5: Re-login after logout ---

    @Test
    @Order(40)
    void reLoginAfterLogoutWorks() {
        page.fill("#username", EMAIL);
        page.fill("#password", PASSWORD);
        page.click("#kc-login");
        page.waitForURL(url -> url.contains("/iam/dashboard"));

        assertThat(page.content()).contains(TENANT_NAME);
    }

    @Test
    @Order(99)
    void cleanup() {
        ensureLoggedOut();
    }
}
