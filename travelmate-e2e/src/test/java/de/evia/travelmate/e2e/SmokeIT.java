package de.evia.travelmate.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

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

    @Test
    @Order(3)
    void iamStartPageLoads() {
        page.navigate(BASE_URL + "/iam/");
        assertThat(page.url()).contains("/iam/");
        assertThat(page.content()).doesNotContain("Sign in to Travelmate");
    }

    @Test
    @Order(4)
    void tripsStartPageLoads() {
        page.navigate(BASE_URL + "/trips/");
        assertThat(page.url()).contains("/trips/");
        assertThat(page.content()).doesNotContain("Sign in to Travelmate");
    }
}
