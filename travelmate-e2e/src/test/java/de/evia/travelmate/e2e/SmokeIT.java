package de.evia.travelmate.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
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
    private static final String IAM_ADMIN_URL = System.getProperty("e2e.iamAdminUrl", "http://localhost:8081/iam");
    private static final String TEST_USER = "testuser";
    private static final String TEST_PASSWORD = "testpassword";
    private static final String RUN_ID = String.valueOf(System.currentTimeMillis());

    private static final List<String> createdTenantIds = new ArrayList<>();

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
        cleanupTestData();
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

    private static void cleanupTestData() {
        try (final HttpClient client = HttpClient.newHttpClient()) {
            for (final String tenantId : createdTenantIds) {
                try {
                    final HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(IAM_ADMIN_URL + "/admin/tenants/" + tenantId))
                        .DELETE()
                        .build();
                    client.send(request, HttpResponse.BodyHandlers.ofString());
                } catch (final Exception ignored) {
                }
            }
        }
    }

    private static void navigateAndWait(final String path) {
        page.navigate(BASE_URL + path, new Page.NavigateOptions().setWaitUntil(WaitUntilState.NETWORKIDLE));
    }

    private static void clickAndWaitForNavigation(final String selector) {
        page.waitForNavigation(() -> page.click(selector));
    }

    // --- Landing Page ---

    @Test
    @Order(1)
    void landingPageLoads() {
        navigateAndWait("/");
        assertThat(page.title()).contains("Travelmate");
        assertThat(page.content()).contains("Gemeinsam Reisen planen");
        assertThat(page.content()).contains("Registrieren");
        assertThat(page.content()).contains("Anmelden");
    }

    @Test
    @Order(2)
    void landingPageLanguageSwitchWorks() {
        navigateAndWait("/iam/?lang=en");
        assertThat(page.content()).contains("Plan trips together");
        assertThat(page.content()).contains("Sign Up");
        assertThat(page.content()).contains("Log In");

        navigateAndWait("/iam/?lang=de");
        assertThat(page.content()).contains("Gemeinsam Reisen planen");
        assertThat(page.content()).contains("Registrieren");
        assertThat(page.content()).contains("Anmelden");
    }

    // --- Authentication ---

    @Test
    @Order(3)
    void loginViaKeycloak() {
        navigateAndWait("/iam/");
        page.click("a[href='/oauth2/authorization/keycloak']");
        page.waitForURL(url -> url.contains("realms/travelmate"));
        assertThat(page.title()).contains("Sign in to Travelmate");

        page.fill("#username", TEST_USER);
        page.fill("#password", TEST_PASSWORD);
        page.click("#kc-login");
        page.waitForURL(url -> !url.contains("realms/travelmate"));
        assertThat(page.url()).startsWith(BASE_URL);
    }

    // --- Service Health ---

    @Test
    @Order(4)
    void iamDashboardRedirectsToSignupForNewUser() {
        navigateAndWait("/iam/dashboard");
        if (page.url().contains("realms/travelmate")) {
            page.fill("#username", TEST_USER);
            page.fill("#password", TEST_PASSWORD);
            page.click("#kc-login");
            page.waitForURL(url -> !url.contains("realms/travelmate"));
        }
        assertThat(page.content()).doesNotContain("Whitelabel Error Page");
        assertThat(page.content()).doesNotContain("Internal Server Error");
        assertThat(page.url()).contains("/signup");
    }

    @Test
    @Order(5)
    void tripsStartPageLoads() {
        navigateAndWait("/trips/");
        assertThat(page.url()).contains("/trips/");
        assertThat(page.content()).doesNotContain("Whitelabel Error Page");
    }

    // --- Sign-up Flow ---

    @Test
    @Order(40)
    void signUpPageIsPubliclyAccessible() {
        navigateAndWait("/iam/signup");
        assertThat(page.content()).doesNotContain("Whitelabel Error Page");
        assertThat(page.content()).doesNotContain("Sign in to Travelmate");
    }

    @Test
    @Order(41)
    void signUpCreatesNewTravelParty() {
        navigateAndWait("/iam/signup");

        page.fill("#tenantName", "E2E-Partei " + RUN_ID);
        page.fill("#firstName", "Anna");
        page.fill("#lastName", "E2E");
        page.fill("#email", "anna-" + RUN_ID + "@e2e.test");
        page.fill("#password", "Test1234!");
        page.fill("#passwordConfirm", "Test1234!");
        clickAndWaitForNavigation("button[type=submit]");

        assertThat(page.content()).doesNotContain("Whitelabel Error Page");
    }

    // --- Navigation between SCS contexts ---

    @Test
    @Order(55)
    void navigateToTripsContextDoesNotError() {
        navigateAndWait("/trips/");
        assertThat(page.content()).doesNotContain("Whitelabel Error Page");
        assertThat(page.content()).doesNotContain("Internal Server Error");
        assertThat(page.content()).contains("Reiseplanung");
    }

    @Test
    @Order(56)
    void tripsPageResolvesI18nMessages() {
        navigateAndWait("/trips/");
        final String content = page.content();
        assertThat(content).doesNotContain("??");
        assertThat(content).contains("Travelmate");
        assertThat(content).contains("Reisepartei");
        assertThat(content).contains("Reisen");
    }

    @Test
    @Order(57)
    void iamPageResolvesI18nMessages() {
        navigateAndWait("/iam/dashboard");
        final String content = page.content();
        assertThat(content).doesNotContain("??");
        assertThat(content).contains("Travelmate");
    }

    // --- Logout ---

    @Test
    @Order(90)
    void logoutButtonIsPresent() {
        navigateAndWait("/iam/dashboard");
        assertThat(page.content()).contains("Abmelden");
    }
}
