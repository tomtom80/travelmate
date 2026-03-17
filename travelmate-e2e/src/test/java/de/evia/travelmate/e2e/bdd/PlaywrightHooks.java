package de.evia.travelmate.e2e.bdd;

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
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitUntilState;

import io.cucumber.java.AfterAll;
import io.cucumber.java.BeforeAll;

/**
 * Manages Playwright browser lifecycle for Cucumber BDD tests.
 * Provides the same helper methods as E2ETestBase for step definitions.
 */
public class PlaywrightHooks {

    static final String BASE_URL = System.getProperty("e2e.baseUrl", "http://localhost:8080");
    static final String IAM_ADMIN_URL = System.getProperty("e2e.iamAdminUrl", "http://localhost:8081/iam");
    static final String RUN_ID = "bdd" + System.currentTimeMillis();

    static final List<String> createdTenantIds = new ArrayList<>();

    static Playwright playwright;
    static Browser browser;
    static BrowserContext context;
    static Page page;

    @BeforeAll
    public static void setUpBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(
            new BrowserType.LaunchOptions().setHeadless(true));
        context = browser.newContext();
        page = context.newPage();
    }

    @AfterAll
    public static void tearDownBrowser() {
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

    static void cleanupTestData() {
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

    static void navigateAndWait(final String path) {
        page.navigate(BASE_URL + path, new Page.NavigateOptions().setWaitUntil(WaitUntilState.NETWORKIDLE));
    }

    static void signUpAndLogin(final String tenantName, final String firstName, final String lastName,
                                final String email, final String password) {
        navigateAndWait("/iam/signup");
        page.fill("#tenantName", tenantName);
        page.fill("#firstName", firstName);
        page.fill("#lastName", lastName);
        page.fill("#dateOfBirth", "1990-01-15");
        page.fill("#email", email);
        page.fill("#password", password);
        page.fill("#passwordConfirm", password);
        page.click("button[type=submit]");
        page.waitForLoadState();

        page.click("main a[href='/oauth2/authorization/keycloak']");
        page.waitForURL(url -> url.contains("realms/travelmate"));

        page.fill("#username", email);
        page.fill("#password", password);
        page.click("#kc-login");
        page.waitForURL(url -> !url.contains("realms/travelmate"));
    }

    static void waitForTripsReady() {
        for (int i = 0; i < 10; i++) {
            navigateAndWait("/trips/");
            if (!page.content().contains("Forbidden") && !page.content().contains("403")) {
                return;
            }
            page.waitForTimeout(500);
        }
    }

    static void ensureLoggedOut() {
        navigateAndWait("/iam/dashboard");
        final var logoutButton = page.locator("form[action='/logout'] button[type=submit]");
        if (logoutButton.isVisible()) {
            logoutButton.click();
            page.waitForLoadState();
        }
    }

    static void clickAndWaitForHtmx(final String selector) {
        page.waitForResponse(
            response -> response.url().contains("/iam/") || response.url().contains("/trips/") || response.url().contains("/expense/"),
            () -> page.click(selector)
        );
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }

    static void submitHtmxForm(final String formSelector) {
        page.waitForResponse(
            response -> response.url().contains("/iam/") || response.url().contains("/trips/") || response.url().contains("/expense/"),
            () -> page.click(formSelector + " button[type=submit]")
        );
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }
}
