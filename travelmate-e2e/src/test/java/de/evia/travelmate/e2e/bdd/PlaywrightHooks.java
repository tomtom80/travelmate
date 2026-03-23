package de.evia.travelmate.e2e.bdd;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

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

    static void waitForTripsDependentProjection(final String tenantName, final String firstName, final String lastName) {
        waitForTripsProjectionCount("""
            select count(*)
            from travel_party_dependent d
            join travel_party t on t.tenant_id = d.tenant_id
            where t.name = '%s' and d.first_name = '%s' and d.last_name = '%s'
            """.formatted(sqlLiteral(tenantName), sqlLiteral(firstName), sqlLiteral(lastName)));
    }

    static void waitForTripsMemberProjection(final String tenantName, final String firstName, final String lastName) {
        waitForTripsProjectionCount("""
            select count(*)
            from travel_party_member m
            join travel_party t on t.tenant_id = m.tenant_id
            where t.name = '%s' and m.first_name = '%s' and m.last_name = '%s'
            """.formatted(sqlLiteral(tenantName), sqlLiteral(firstName), sqlLiteral(lastName)));
    }

    static void waitForTripParticipant(final String tripId, final String firstName, final String lastName) {
        waitForTripsProjectionCount("""
            select count(*)
            from trip_participant p
            where p.trip_id = '%s'::uuid and p.first_name = '%s' and p.last_name = '%s'
            """.formatted(sqlLiteral(tripId), sqlLiteral(firstName), sqlLiteral(lastName)));
    }

    static boolean tripParticipantExists(final String tripId, final String firstName, final String lastName) {
        return queryTripsCount("""
            select count(*)
            from trip_participant p
            where p.trip_id = '%s'::uuid and p.first_name = '%s' and p.last_name = '%s'
            """.formatted(sqlLiteral(tripId), sqlLiteral(firstName), sqlLiteral(lastName))) > 0;
    }

    static void waitForExpenseAccommodationPrice(final String tripId) {
        waitForExpenseProjectionCount("""
            select count(*)
            from trip_projection
            where trip_id = '%s'::uuid and accommodation_total_price is not null and accommodation_total_price > 0
            """.formatted(sqlLiteral(tripId)));
    }

    private static void waitForTripsProjectionCount(final String sql) {
        for (int i = 0; i < 60; i++) {
            if (queryTripsCount(sql) > 0) {
                return;
            }
            page.waitForTimeout(500);
        }
        throw new AssertionError("Trips projection did not contain expected row for SQL: " + sql);
    }

    private static void waitForExpenseProjectionCount(final String sql) {
        for (int i = 0; i < 60; i++) {
            if (queryExpenseCount(sql) > 0) {
                return;
            }
            page.waitForTimeout(500);
        }
        throw new AssertionError("Expense projection did not contain expected row for SQL: " + sql);
    }

    private static long queryTripsCount(final String sql) {
        return queryComposePsqlCount("postgres-trips", "travelmate_trips", sql);
    }

    private static long queryExpenseCount(final String sql) {
        return queryComposePsqlCount("postgres-expense", "travelmate_expense", sql);
    }

    private static long queryComposePsqlCount(final String service, final String database, final String sql) {
        try {
            final Process process = new ProcessBuilder(
                "docker", "compose", "exec", "-T", service,
                "psql", "-U", "travelmate", "-d", database, "-tAc", sql
            ).start();
            if (!process.waitFor(10, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return 0L;
            }
            final String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            if (process.exitValue() != 0 || output.isBlank()) {
                return 0L;
            }
            return Long.parseLong(output);
        } catch (final Exception e) {
            return 0L;
        }
    }

    private static String sqlLiteral(final String value) {
        return value.replace("'", "''");
    }
}
