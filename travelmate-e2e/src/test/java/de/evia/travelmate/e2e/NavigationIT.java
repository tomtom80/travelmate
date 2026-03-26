package de.evia.travelmate.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class NavigationIT extends E2ETestBase {

    private static final String TENANT_NAME = "E2E-Nav " + RUN_ID;
    private static final String EMAIL = "nav-" + RUN_ID + "@e2e.test";
    private static final String PASSWORD = "Test1234!";

    @Test
    @Order(1)
    void setUpAndLogin() {
        signUpAndLogin(TENANT_NAME, "Nora", "Navigation", EMAIL, PASSWORD);

        navigateAndWait("/iam/dashboard");
        assertThat(page.content()).contains(TENANT_NAME);
    }

    @Test
    @Order(10)
    void navigateFromDashboardToTrips() {
        navigateAndWait("/iam/dashboard");
        page.locator("nav a[href='/trips/']").click();
        page.waitForURL(url -> url.contains("/trips/"));

        assertThat(page.content()).doesNotContain("Whitelabel Error Page");
        assertThat(page.content()).doesNotContain("Internal Server Error");
    }

    @Test
    @Order(11)
    void navigateFromTripsBackToDashboard() {
        navigateAndWait("/trips/");
        page.locator("nav a:has-text('Reisepartei')").click();
        page.waitForURL(url -> url.contains("/iam/dashboard"));

        assertThat(page.content()).contains(TENANT_NAME);
    }

    @Test
    @Order(20)
    void navigationBarShowsAllLinks() {
        navigateAndWait("/iam/dashboard");
        final String navContent = page.locator("nav").innerHTML();

        assertThat(navContent).contains("Travelmate");
        assertThat(navContent).contains("Reisepartei");
        assertThat(navContent).contains("Reisen");
        assertThat(navContent).contains("Abmelden");
    }

    @Test
    @Order(30)
    void logoutButtonIsPresent() {
        navigateAndWait("/iam/dashboard");

        assertThat(page.locator("a.nav-logout-btn").isVisible()).isTrue();
    }

    @Test
    @Order(31)
    void logoutRedirectsAwayFromDashboard() {
        navigateAndWait("/iam/dashboard");
        page.locator("a.nav-logout-btn").click();
        page.waitForLoadState();

        final String url = page.url();
        assertThat(url).doesNotContain("/dashboard");
    }
}
