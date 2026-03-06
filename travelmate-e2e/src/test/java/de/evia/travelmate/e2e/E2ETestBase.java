package de.evia.travelmate.e2e;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.WaitUntilState;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

abstract class E2ETestBase {

    static final String BASE_URL = System.getProperty("e2e.baseUrl", "http://localhost:8080");
    static final String IAM_ADMIN_URL = System.getProperty("e2e.iamAdminUrl", "http://localhost:8081/iam");
    static final String MAILPIT_URL = System.getProperty("e2e.mailpitUrl", "http://localhost:8025");
    static final String RUN_ID = String.valueOf(System.currentTimeMillis());

    static final List<String> createdTenantIds = new ArrayList<>();

    static Playwright playwright;
    static Browser browser;
    static BrowserContext context;
    static Page page;

    @BeforeAll
    static void setUpBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(
            new BrowserType.LaunchOptions().setHeadless(true));
        context = browser.newContext();
        page = context.newPage();
    }

    @AfterAll
    static void tearDownBrowser() {
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
        page.waitForLoadState();

        if (page.url().contains("login-actions") || page.content().contains("Verify")) {
            final String verificationLink = getVerificationLinkFromMailpit(email);
            if (verificationLink != null) {
                page.navigate(verificationLink);
                page.waitForLoadState();
            }
        }

        if (page.url().contains("realms/travelmate") && !page.url().contains("login-actions")) {
            page.fill("#username", email);
            page.fill("#password", password);
            page.click("#kc-login");
            page.waitForURL(url -> !url.contains("realms/travelmate"));
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

    /**
     * Waits until the Trips module has received the TravelParty projection via RabbitMQ.
     * After IAM signup, events are propagated asynchronously.
     */
    static void waitForTripsReady() {
        for (int i = 0; i < 10; i++) {
            navigateAndWait("/trips/");
            if (!page.content().contains("Forbidden") && !page.content().contains("403")) {
                return;
            }
            page.waitForTimeout(500);
        }
    }

    /**
     * Clicks an element and waits for the HTMX response to complete.
     * Uses Playwright's waitForResponse to detect when the XHR finishes.
     */
    static void clickAndWaitForHtmx(final String selector) {
        page.waitForResponse(
            response -> response.url().contains("/iam/") || response.url().contains("/trips/"),
            () -> page.click(selector)
        );
        page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE);
    }

    /**
     * Submits an HTMX form and waits for the response.
     */
    static void submitHtmxForm(final String formSelector) {
        page.waitForResponse(
            response -> response.url().contains("/iam/") || response.url().contains("/trips/"),
            () -> page.click(formSelector + " button[type=submit]")
        );
        page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE);
    }

    private static String getVerificationLinkFromMailpit(final String email) {
        try (final HttpClient client = HttpClient.newHttpClient()) {
            final String encodedQuery = URLEncoder.encode("to:" + email, StandardCharsets.UTF_8);
            for (int attempt = 0; attempt < 10; attempt++) {
                final HttpRequest searchRequest = HttpRequest.newBuilder()
                    .uri(URI.create(MAILPIT_URL + "/api/v1/search?query=" + encodedQuery))
                    .GET()
                    .build();
                final String searchResponse = client.send(searchRequest, HttpResponse.BodyHandlers.ofString()).body();

                final String messageId = extractJsonField(searchResponse, "ID");
                if (messageId != null) {
                    final HttpRequest msgRequest = HttpRequest.newBuilder()
                        .uri(URI.create(MAILPIT_URL + "/api/v1/message/" + messageId))
                        .GET()
                        .build();
                    final String msgResponse = client.send(msgRequest, HttpResponse.BodyHandlers.ofString()).body();

                    final String link = extractVerificationLink(msgResponse);
                    if (link != null) {
                        return link;
                    }
                }
                Thread.sleep(500);
            }
        } catch (final Exception e) {
            System.err.println("Warning: Could not get verification link from Mailpit: " + e.getMessage());
        }
        return null;
    }

    private static String extractVerificationLink(final String messageJson) {
        final String unescaped = messageJson.replace("\\\"", "\"").replace("\\u0026", "&");
        final Pattern pattern = Pattern.compile("href=\"(http[^\"]*action-token[^\"]*?)\"");
        final Matcher matcher = pattern.matcher(unescaped);
        if (matcher.find()) {
            return matcher.group(1).replace("&amp;", "&");
        }
        return null;
    }

    private static String extractJsonField(final String json, final String field) {
        final String searchKey = "\"" + field + "\"";
        final int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) {
            return null;
        }
        final int colonIndex = json.indexOf(':', keyIndex + searchKey.length());
        if (colonIndex == -1) {
            return null;
        }
        final int valueStart = json.indexOf('"', colonIndex + 1);
        if (valueStart == -1) {
            return null;
        }
        final int valueEnd = json.indexOf('"', valueStart + 1);
        if (valueEnd == -1) {
            return null;
        }
        return json.substring(valueStart + 1, valueEnd);
    }
}
