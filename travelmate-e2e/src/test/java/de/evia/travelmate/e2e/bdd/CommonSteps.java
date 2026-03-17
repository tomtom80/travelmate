package de.evia.travelmate.e2e.bdd;

import static org.assertj.core.api.Assertions.assertThat;
import static de.evia.travelmate.e2e.bdd.PlaywrightHooks.*;

import io.cucumber.java.de.Angenommen;
import io.cucumber.java.de.Dann;
import io.cucumber.java.de.Und;

public class CommonSteps {

    private static final String TENANT_NAME = "BDD-Test " + RUN_ID;
    private static final String EMAIL = "bdd-" + RUN_ID + "@e2e.test";
    private static final String PASSWORD = "Test1234!";

    private static boolean loggedIn = false;

    @Angenommen("ich bin als Mitglied einer Reisepartei eingeloggt")
    public void ichBinAlsMitgliedEingeloggt() {
        if (!loggedIn) {
            ensureLoggedOut();
            signUpAndLogin(TENANT_NAME, "Koch", "Tester", EMAIL, PASSWORD);
            waitForTripsReady();
            loggedIn = true;
        }
    }

    @Und("ich das Formular absende")
    public void ichDasFormularAbsende() {
        page.locator("main button[type=submit]").click();
        page.waitForLoadState();
    }

    @Dann("enthaelt die Seite keine unaufgeloesten Message-Keys {string}")
    public void enthaeltDieSeiteKeineUnaufgeloestenMessageKeys(final String marker) {
        assertThat(page.content()).doesNotContain(marker);
    }
}
