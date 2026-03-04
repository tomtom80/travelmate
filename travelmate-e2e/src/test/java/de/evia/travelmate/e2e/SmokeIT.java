package de.evia.travelmate.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.microsoft.playwright.options.WaitUntilState;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SmokeIT {

    private static final String BASE_URL = System.getProperty("e2e.baseUrl", "http://localhost:8080");
    private static final String TEST_USER = "testuser";
    private static final String TEST_PASSWORD = "testpassword";
    private static final String RUN_ID = String.valueOf(System.currentTimeMillis());

    private static Playwright playwright;
    private static Browser browser;
    private static BrowserContext context;
    private static Page page;

    @BeforeAll
    static void setUp() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(
            new BrowserType.LaunchOptions().setHeadless(true));
        context = browser.newContext();
        page = context.newPage();
    }

    @AfterAll
    static void tearDown() {
        if (context != null) {
            context.close();
        }
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }

    private static void navigateAndWait(final String path) {
        page.navigate(BASE_URL + path, new Page.NavigateOptions().setWaitUntil(WaitUntilState.NETWORKIDLE));
    }

    private static void clickAndWaitForNavigation(final String selector) {
        page.waitForNavigation(() -> page.click(selector));
    }

    // --- Authentication ---

    @Test
    @Order(1)
    void gatewayRedirectsToKeycloakLogin() {
        page.navigate(BASE_URL);
        assertThat(page.title()).contains("Sign in to Travelmate");
    }

    @Test
    @Order(2)
    void loginViaKeycloak() {
        page.navigate(BASE_URL);
        page.fill("#username", TEST_USER);
        page.fill("#password", TEST_PASSWORD);
        page.click("#kc-login");
        page.waitForURL(url -> !url.contains("realms/travelmate"));
        assertThat(page.url()).startsWith(BASE_URL);
    }

    // --- Service Health (regression: Flyway autoconfiguration v0.2.1) ---

    @Test
    @Order(3)
    void iamStartPageLoads() {
        navigateAndWait("/iam/");
        assertThat(page.url()).contains("/iam/");
        assertThat(page.content()).doesNotContain("Whitelabel Error Page");
    }

    @Test
    @Order(4)
    void tripsStartPageLoads() {
        navigateAndWait("/trips/");
        assertThat(page.url()).contains("/trips/");
        assertThat(page.content()).doesNotContain("Whitelabel Error Page");
    }

    // --- Tenant CRUD (regression: CSRF on Gateway v0.2.4) ---

    @Test
    @Order(10)
    void createTenant() {
        final String tenantName = "Huettengaudi " + RUN_ID;

        navigateAndWait("/iam/tenants/new");
        assertThat(page.content()).contains("Neuen Tenant anlegen");

        page.fill("#name", tenantName);
        page.fill("#description", "E2E-Testlauf " + RUN_ID);
        clickAndWaitForNavigation("button[type=submit]");

        assertThat(page.content()).doesNotContain("Whitelabel Error Page");
        assertThat(page.content()).contains(tenantName);
    }

    @Test
    @Order(11)
    void tenantAppearsInList() {
        navigateAndWait("/iam/tenants");
        assertThat(page.content()).contains("Huettengaudi " + RUN_ID);
    }

    // --- Account Registration (regression: CSRF on SCS v0.2.3) ---

    @Test
    @Order(20)
    void registerAccount() {
        navigateAndWait("/iam/tenants");

        clickAndWaitForNavigation("a:has-text('Details')");
        clickAndWaitForNavigation("a:has-text('Accounts verwalten')");
        clickAndWaitForNavigation("a:has-text('Neuen Account')");
        assertThat(page.content()).contains("Neuen Account registrieren");

        page.fill("#keycloakUserId", "kc-e2e-" + RUN_ID);
        page.fill("#username", "e2euser" + RUN_ID);
        page.fill("#email", "e2e-" + RUN_ID + "@example.com");
        page.fill("#firstName", "Max");
        page.fill("#lastName", "Tester");
        clickAndWaitForNavigation("button[type=submit]");

        assertThat(page.content()).doesNotContain("Whitelabel Error Page");
        assertThat(page.content()).contains("Max");
        assertThat(page.content()).contains("Tester");
    }

    // --- Dependent (regression: CSRF on SCS for HTMX POST v0.2.3) ---

    @Test
    @Order(30)
    void addDependent() {
        navigateAndWait("/iam/tenants");

        clickAndWaitForNavigation("a:has-text('Details')");
        clickAndWaitForNavigation("a:has-text('Accounts verwalten')");
        clickAndWaitForNavigation("a:has-text('Details')");

        // Wait for HTMX to load the dependent form into #dependents
        page.waitForSelector("#depFirstName",
            new Page.WaitForSelectorOptions().setState(WaitForSelectorState.VISIBLE));

        page.fill("#depFirstName", "Lina");
        page.fill("#depLastName", "Tester");
        page.click("#dependents button[type=submit]");

        // HTMX replaces #dependents content — wait for the name to appear
        page.waitForSelector("td:has-text('Lina')");
        assertThat(page.content()).contains("Lina");
        assertThat(page.content()).contains("Tester");
    }
}
