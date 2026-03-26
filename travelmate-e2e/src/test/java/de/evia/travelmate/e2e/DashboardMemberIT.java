package de.evia.travelmate.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DashboardMemberIT extends E2ETestBase {

    private static final String TENANT_NAME = "E2E-Dashboard " + RUN_ID;
    private static final String EMAIL = "dashboard-" + RUN_ID + "@e2e.test";
    private static final String PASSWORD = "Test1234!";

    @Test
    @Order(1)
    void setUpTravelParty() {
        signUpAndLogin(TENANT_NAME, "Max", "Dashboard", EMAIL, PASSWORD);

        navigateAndWait("/iam/dashboard");
        final String content = page.content();
        assertThat(content).contains(TENANT_NAME);
        assertThat(content).contains("Max");
    }

    @Test
    @Order(2)
    void dashboardShowsTravelPartyName() {
        navigateAndWait("/iam/dashboard");
        final String content = page.content();

        assertThat(content).contains(TENANT_NAME);
        assertThat(content).doesNotContain("Whitelabel Error Page");
    }

    @Test
    @Order(3)
    void dashboardShowsMembersList() {
        navigateAndWait("/iam/dashboard");
        final String content = page.content();

        assertThat(content).contains("Mitglieder");
        assertThat(content).contains("Max");
        assertThat(content).contains("Dashboard");
        assertThat(content).contains(EMAIL);
    }

    @Test
    @Order(4)
    void dashboardShowsCompanionSection() {
        navigateAndWait("/iam/dashboard");

        assertThat(page.content()).contains("Mitreisende");
    }

    @Test
    @Order(10)
    void inviteMemberViaHtmx() {
        navigateAndWait("/iam/dashboard");

        final String memberEmail = "member-" + RUN_ID + "@e2e.test";
        page.fill("form[hx-post$='/dashboard/members'] input[name=firstName]", "Lisa");
        page.fill("form[hx-post$='/dashboard/members'] input[name=lastName]", "Eingeladen");
        page.fill("form[hx-post$='/dashboard/members'] input[name=email]", memberEmail);
        page.fill("form[hx-post$='/dashboard/members'] input[name=dateOfBirth]", "1985-03-20");
        submitHtmxForm("form[hx-post$='/dashboard/members']");

        final String members = page.locator("#members").innerHTML();
        assertThat(members).contains("Lisa");
        assertThat(members).contains("Eingeladen");
        assertThat(members).contains(memberEmail);
    }

    @Test
    @Order(11)
    void inviteMemberWithDateOfBirth() {
        navigateAndWait("/iam/dashboard");

        final String memberEmail = "member-dob-" + RUN_ID + "@e2e.test";
        page.fill("form[hx-post$='/dashboard/members'] input[name=firstName]", "Tom");
        page.fill("form[hx-post$='/dashboard/members'] input[name=lastName]", "MitDatum");
        page.fill("form[hx-post$='/dashboard/members'] input[name=email]", memberEmail);
        page.fill("form[hx-post$='/dashboard/members'] input[name=dateOfBirth]", "1990-05-15");
        submitHtmxForm("form[hx-post$='/dashboard/members']");

        final String members = page.locator("#members").innerHTML();
        assertThat(members).contains("Tom");
        assertThat(members).contains("MitDatum");
        assertThat(members).contains("1990-05-15");
    }

    @Test
    @Order(20)
    void addCompanionViaHtmx() {
        navigateAndWait("/iam/dashboard");

        page.fill("form[hx-post$='/dashboard/companions'] input[name=firstName]", "Lina");
        page.fill("form[hx-post$='/dashboard/companions'] input[name=lastName]", "Kind");
        page.fill("form[hx-post$='/dashboard/companions'] input[name=dateOfBirth]", "2018-06-15");
        submitHtmxForm("form[hx-post$='/dashboard/companions']");

        final String companions = page.locator("#companions").innerHTML();
        assertThat(companions).contains("Lina");
        assertThat(companions).contains("Kind");
    }

    @Test
    @Order(21)
    void addCompanionWithDateOfBirth() {
        navigateAndWait("/iam/dashboard");

        page.fill("form[hx-post$='/dashboard/companions'] input[name=firstName]", "Mia");
        page.fill("form[hx-post$='/dashboard/companions'] input[name=lastName]", "Baby");
        page.fill("form[hx-post$='/dashboard/companions'] input[name=dateOfBirth]", "2023-01-01");
        submitHtmxForm("form[hx-post$='/dashboard/companions']");

        final String companions = page.locator("#companions").innerHTML();
        assertThat(companions).contains("Mia");
        assertThat(companions).contains("Baby");
        assertThat(companions).contains("2023-01-01");
    }

    @Test
    @Order(30)
    void deleteCompanionViaHtmx() {
        navigateAndWait("/iam/dashboard");

        assertThat(page.locator("#companions").innerHTML()).contains("Lina");

        page.onceDialog(dialog -> dialog.accept());
        clickAndWaitForHtmx("#companions tr:has-text('Lina') button.btn-icon--danger");

        assertThat(page.locator("#companions").innerHTML()).doesNotContain("Lina");
    }

    @Test
    @Order(40)
    void deleteMemberViaHtmx() {
        navigateAndWait("/iam/dashboard");

        assertThat(page.locator("#members").innerHTML()).contains("Lisa");

        page.onceDialog(dialog -> dialog.accept());
        page.waitForResponse(
            response -> response.url().contains("/iam/dashboard/members/"),
            () -> page.locator("#members tr:has-text('Lisa') button.btn-icon--danger").click()
        );
        page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE);

        assertThat(page.locator("#members").innerHTML()).doesNotContain("Lisa");
    }

    @Test
    @Order(50)
    void dashboardShowsNavigationLinks() {
        navigateAndWait("/iam/dashboard");
        final String content = page.content();

        assertThat(content).contains("Reisepartei");
        assertThat(content).contains("Reisen");
        assertThat(content).contains("Abmelden");
    }

    @Test
    @Order(51)
    void dashboardResolvesI18nMessages() {
        navigateAndWait("/iam/dashboard");

        assertThat(page.content()).doesNotContain("??");
        assertThat(page.content()).contains("Travelmate");
    }

    @Test
    @Order(90)
    void dangerZoneShowsDeleteButton() {
        navigateAndWait("/iam/dashboard");

        assertThat(page.content()).contains("Gefahrenzone");
        assertThat(page.locator("button[hx-delete$='/dashboard/tenant']").isVisible()).isTrue();
    }
}
