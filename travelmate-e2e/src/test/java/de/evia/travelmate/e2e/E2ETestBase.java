package de.evia.travelmate.e2e;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    static final String IAM_PUBLIC_URL = System.getProperty("e2e.iamPublicUrl", BASE_URL + "/iam");
    static final String RUN_ID = String.valueOf(System.currentTimeMillis());

    static final List<String> createdTenantIds = new ArrayList<>();

    static Playwright playwright;
    static Browser browser;
    static BrowserContext context;
    static Page page;

    @BeforeAll
    static void setUpBrowser() {
        playwright = Playwright.create(new Playwright.CreateOptions().setEnv(playwrightEnv()));
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
        navigateAndWait(page, path);
    }

    static void navigateAndWait(final Page targetPage, final String path) {
        final String url = path.startsWith("http://") || path.startsWith("https://") ? path : BASE_URL + path;
        targetPage.navigate(url, new Page.NavigateOptions().setWaitUntil(WaitUntilState.NETWORKIDLE));
    }

    static void createTripWithoutDates(final String tripName, final String description) {
        createTripWithoutDates(page, tripName, description);
    }

    static void createTripWithoutDates(final Page targetPage, final String tripName, final String description) {
        navigateAndWait(targetPage, "/trips/new");
        targetPage.fill("input[name=name]", tripName);
        if (description != null) {
            targetPage.fill("textarea[name=description], #description", description);
        }
        targetPage.locator("main button[type=submit]").click();
        targetPage.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE);
    }

    static String openTripFromList(final String tripName) {
        return openTripFromList(page, tripName);
    }

    static String openTripFromList(final Page targetPage, final String tripName) {
        navigateAndWait(targetPage, "/trips/");
        targetPage.locator("a", new Page.LocatorOptions().setHasText(tripName)).click();
        targetPage.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE);
        return extractTripId(targetPage);
    }

    static void createAndConfirmDatePoll(final String tripId,
                                         final String option1Start,
                                         final String option1End,
                                         final String option2Start,
                                         final String option2End) {
        navigateAndWait("/trips/" + tripId + "/datepoll/create");
        page.locator("input[name=startDate]").nth(0).fill(option1Start);
        page.locator("input[name=endDate]").nth(0).fill(option1End);
        page.locator("input[name=startDate]").nth(1).fill(option2Start);
        page.locator("input[name=endDate]").nth(1).fill(option2End);
        page.locator("button[type=submit]:not(.outline)").first().click();
        page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE);

        page.locator("select[name=confirmedOptionId]").selectOption(
            page.locator("select[name=confirmedOptionId] option:not([value=''])").first().getAttribute("value")
        );
        page.locator("button[type=submit]:has-text('Bestaetigen'), button[type=submit]:has-text('Confirm')").click();
        page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE);
    }

    static void createAndConfirmAccommodationPoll(final String tripId,
                                                  final String candidate1Name,
                                                  final String candidate1Url,
                                                  final String candidate1Description,
                                                  final String candidate2Name,
                                                  final String candidate2Description) {
        navigateAndWait("/trips/" + tripId + "/accommodationpoll/create");
        page.locator("input[name=candidateName]").nth(0).fill(candidate1Name);
        if (candidate1Url != null) {
            page.locator("input[name=candidateUrl]").nth(0).fill(candidate1Url);
        }
        if (candidate1Description != null) {
            page.locator("input[name=candidateDescription]").nth(0).fill(candidate1Description);
        }
        page.locator(".candidate-entry").nth(0).locator("input[name=roomName]").first().fill("Familienzimmer");
        page.locator(".candidate-entry").nth(0).locator("input[name=roomBedCount]").first().fill("4");
        page.locator(".candidate-entry").nth(0).locator("input[name=roomFeatures]").first().fill("Seeblick, Balkon");
        page.locator("input[name=candidateName]").nth(1).fill(candidate2Name);
        if (candidate2Description != null) {
            page.locator("input[name=candidateDescription]").nth(1).fill(candidate2Description);
        }
        page.locator(".candidate-entry").nth(1).locator("input[name=roomName]").first().fill("Doppelzimmer");
        page.locator(".candidate-entry").nth(1).locator("input[name=roomBedCount]").first().fill("2");
        page.locator(".candidate-entry").nth(1).locator("input[name=roomFeatures]").first().fill("Bergblick");
        page.evaluate("""
            ([first, second]) => {
                const inputs = document.querySelectorAll('input.candidate-rooms-data');
                inputs[0].value = first;
                inputs[1].value = second;
            }
            """, List.of(
            "[{\"name\":\"Familienzimmer\",\"bedCount\":4,\"features\":\"Seeblick, Balkon\"}]",
            "[{\"name\":\"Doppelzimmer\",\"bedCount\":2,\"features\":\"Bergblick\"}]"
        ));
        page.locator("button[type=submit]:not(.outline)").first().click();
        page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE);

        page.locator("select[name=confirmedCandidateId]").selectOption(
            page.locator("select[name=confirmedCandidateId] option:not([value=''])").first().getAttribute("value")
        );
        page.locator("button[type=submit]:has-text('Bestaetigen'), button[type=submit]:has-text('Confirm')").click();
        page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE);
    }

    static void createAccommodationAfterPollDecision(final String tripId,
                                                     final String name,
                                                     final String address,
                                                     final String checkIn,
                                                     final String checkOut,
                                                     final String totalPrice,
                                                     final String roomName,
                                                     final String roomBedCount) {
        navigateAndWait("/trips/" + tripId + "/accommodation");
        page.locator("button:has-text('Unterkunft hinzufuegen'), button:has-text('Accommodation hinzufügen')").first().click();
        page.waitForSelector("dialog[open]");
        page.locator("dialog input[name=name]").fill(name);
        if (address != null) {
            page.locator("dialog input[name=address]").fill(address);
        }
        page.locator("dialog input[name=checkIn]").fill(checkIn);
        page.locator("dialog input[name=checkOut]").fill(checkOut);
        if (totalPrice != null) {
            page.locator("dialog input[name=totalPrice]").fill(totalPrice);
        }
        page.locator("dialog input[name=roomName]").fill(roomName);
        page.locator("dialog input[name=roomBedCount]").fill(roomBedCount);
        page.locator("dialog button[type=submit]").click();
        page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE);
    }

    static void signUpAndLogin(final String tenantName, final String firstName, final String lastName,
                                final String email, final String password) {
        signUpAndLogin(page, tenantName, firstName, lastName, email, password);
    }

    static void signUpAndLogin(final Page targetPage, final String tenantName, final String firstName, final String lastName,
                               final String email, final String password) {
        navigateAndWait(targetPage, "/iam/signup");
        targetPage.fill("#tenantName", tenantName);
        targetPage.fill("#firstName", firstName);
        targetPage.fill("#lastName", lastName);
        targetPage.fill("#dateOfBirth", "1990-01-15");
        targetPage.fill("#email", email);
        targetPage.fill("#password", password);
        targetPage.fill("#passwordConfirm", password);
        targetPage.click("button[type=submit]");
        targetPage.waitForLoadState();

        loginViaKeycloak(targetPage, email, password, "/oauth2/authorization/keycloak");
    }

    static void ensureLoggedOut() {
        ensureLoggedOut(page);
    }

    static void ensureLoggedOut(final Page targetPage) {
        navigateAndWait(targetPage, "/iam/dashboard");
        final var logoutButton = targetPage.locator("a.nav-logout-btn");
        if (logoutButton.isVisible()) {
            logoutButton.click();
            targetPage.waitForLoadState();
        }
    }

    /**
     * Waits until the Trips module has received the TravelParty projection via RabbitMQ.
     * After IAM signup, events are propagated asynchronously.
     */
    static void waitForTripsReady() {
        waitForTripsReady(page);
    }

    static void waitForTripsReady(final Page targetPage) {
        for (int i = 0; i < 10; i++) {
            navigateAndWait(targetPage, "/trips/");
            if (!targetPage.content().contains("Forbidden") && !targetPage.content().contains("403")) {
                return;
            }
            targetPage.waitForTimeout(500);
        }
    }

    /**
     * Clicks an element and waits for the HTMX response to complete.
     * Uses Playwright's waitForResponse to detect when the XHR finishes.
     */
    static void clickAndWaitForHtmx(final String selector) {
        clickAndWaitForHtmx(page, selector);
    }

    static void clickAndWaitForHtmx(final Page targetPage, final String selector) {
        targetPage.waitForResponse(
            response -> response.url().contains("/iam/") || response.url().contains("/trips/") || response.url().contains("/expense/"),
            () -> targetPage.click(selector)
        );
        targetPage.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE);
    }

    /**
     * Submits an HTMX form and waits for the response.
     */
    static void submitHtmxForm(final String formSelector) {
        submitHtmxForm(page, formSelector);
    }

    static void submitHtmxForm(final Page targetPage, final String formSelector) {
        targetPage.waitForResponse(
            response -> response.url().contains("/iam/") || response.url().contains("/trips/") || response.url().contains("/expense/"),
            () -> targetPage.click(formSelector + " button[type=submit]")
        );
        targetPage.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE);
    }

    static BrowserContext newContext() {
        return browser.newContext();
    }

    static Page newPage(final BrowserContext browserContext) {
        return browserContext.newPage();
    }

    static void loginViaKeycloak(final Page targetPage, final String email, final String password) {
        loginViaKeycloak(targetPage, email, password, "/oauth2/authorization/keycloak");
    }

    static void loginViaKeycloak(final Page targetPage, final String email, final String password, final String startPath) {
        navigateAndWait(targetPage, startPath);
        targetPage.waitForURL(url -> url.contains("realms/travelmate"));
        targetPage.fill("#username", email);
        targetPage.fill("#password", password);
        targetPage.click("#kc-login");
        targetPage.waitForURL(url -> !url.contains("realms/travelmate"));
        targetPage.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE);
    }

    static String waitForMailpitLink(final String email, final String requiredFragment) {
        return waitForMailpitLink(email, requiredFragment, 20);
    }

    static String waitForMailpitLink(final String email, final String requiredFragment, final int attempts) {
        try (final HttpClient client = HttpClient.newHttpClient()) {
            final String encodedQuery = URLEncoder.encode("to:" + email, StandardCharsets.UTF_8);
            for (int attempt = 0; attempt < attempts; attempt++) {
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

                    final String link = extractLinkContaining(msgResponse, requiredFragment);
                    if (link != null) {
                        return normalizeMailpitLink(link);
                    }
                }
                Thread.sleep(500);
            }
        } catch (final Exception e) {
            System.err.println("Warning: Could not get link from Mailpit: " + e.getMessage());
        }
        return null;
    }

    private static String getVerificationLinkFromMailpit(final String email) {
        return waitForMailpitLink(email, "action-token");
    }

    private static String extractVerificationLink(final String messageJson) {
        return extractLinkContaining(messageJson, "action-token");
    }

    private static String extractLinkContaining(final String messageJson, final String requiredFragment) {
        final String unescaped = messageJson.replace("\\\"", "\"").replace("\\u0026", "&");
        final Pattern pattern = Pattern.compile("href=\"(http[^\"]*?" + Pattern.quote(requiredFragment) + "[^\"]*?)\"");
        final Matcher matcher = pattern.matcher(unescaped);
        if (matcher.find()) {
            return matcher.group(1).replace("&amp;", "&");
        }
        final Pattern fallbackPattern = Pattern.compile("(https?://\\S*" + Pattern.quote(requiredFragment) + "\\S*)");
        final Matcher fallbackMatcher = fallbackPattern.matcher(unescaped);
        if (fallbackMatcher.find()) {
            return fallbackMatcher.group(1).replace("&amp;", "&").replaceAll("[\"'<>]$", "");
        }
        return null;
    }

    private static String normalizeMailpitLink(final String link) {
        return link
            .replace("http://iam:8081/iam", IAM_PUBLIC_URL)
            .replace("http://gateway:8080", BASE_URL)
            .replace("http://trips:8082", BASE_URL + "/trips");
    }

    static String extractTripId(final Page targetPage) {
        final String url = targetPage.url();
        final String path = url.replaceFirst(".*/(trips|expense)/", "");
        return path.replaceAll("[/?#].*", "");
    }

    private static Map<String, String> playwrightEnv() {
        final Map<String, String> env = new HashMap<>(System.getenv());
        final String browsersPath = preparePlaywrightBrowsersPath();
        env.putIfAbsent("PLAYWRIGHT_BROWSERS_PATH", browsersPath);
        return env;
    }

    private static String preparePlaywrightBrowsersPath() {
        final String configuredPath = System.getProperty("e2e.playwrightBrowsersPath");
        if (configuredPath != null && !configuredPath.isBlank()) {
            return configuredPath;
        }

        final Path targetPath = Path.of(System.getProperty("java.io.tmpdir"), "ms-playwright");
        final Path sourcePath = Path.of(System.getProperty("user.home"), "Library", "Caches", "ms-playwright");
        try {
            if (Files.notExists(targetPath)) {
                if (Files.exists(sourcePath)) {
                    copyDirectory(sourcePath, targetPath);
                } else {
                    Files.createDirectories(targetPath);
                }
            }
        } catch (final Exception ignored) {
        }
        return targetPath.toString();
    }

    private static void copyDirectory(final Path sourcePath, final Path targetPath) throws java.io.IOException {
        try (var paths = Files.walk(sourcePath)) {
            paths.forEach(source -> {
                try {
                    final Path target = targetPath.resolve(sourcePath.relativize(source).toString());
                    if (Files.isDirectory(source)) {
                        Files.createDirectories(target);
                    } else {
                        Files.createDirectories(target.getParent());
                        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (final java.io.IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (final RuntimeException e) {
            if (e.getCause() instanceof java.io.IOException ioException) {
                throw ioException;
            }
            throw e;
        }
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
