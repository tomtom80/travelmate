package de.evia.travelmate.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

import org.junit.jupiter.api.Test;

class SmokeIT {

    private static final String BASE_URL = System.getProperty("e2e.baseUrl", "http://localhost:8080");

    @Test
    void gatewayIsReachable() {
        try (final Playwright playwright = Playwright.create()) {
            final Browser browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions().setHeadless(true));
            final Page page = browser.newPage();
            page.navigate(BASE_URL);
            assertThat(page.title()).contains("Travelmate");
            browser.close();
        }
    }

    @Test
    void iamStartPageLoads() {
        try (final Playwright playwright = Playwright.create()) {
            final Browser browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions().setHeadless(true));
            final Page page = browser.newPage();
            page.navigate(BASE_URL + "/iam/");
            assertThat(page.content()).contains("IAM");
            browser.close();
        }
    }
}
