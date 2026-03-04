# ADR-0010: End-to-End Testing mit Playwright (Java)

## Status

Accepted

## Context

Travelmate nutzt Server-Side Rendering mit Thymeleaf und HTMX. Klassische Unit- und Integrationstests (MockMvc) testen einzelne Controller isoliert, koennen aber nicht das Zusammenspiel von Browser, HTMX-Interaktionen, Keycloak-Login und Gateway-Routing verifizieren. Fehlende E2E-Tests sind als technische Schuld dokumentiert (siehe 11-risks-and-technical-debt.md).

Optionen:

1. **Selenium (Java)** -- Verbreitet, aber langsam, instabil bei dynamischen Seiten, komplexes Setup
2. **Cypress (JavaScript)** -- Schnell, gute DX, aber erfordert Node.js im Build
3. **Playwright (Java)** -- Moderne Browser-Automatisierung, Java-API, schnell, stabil, HTMX-kompatibel

## Decision

Wir verwenden **Playwright mit der Java-API** (`com.microsoft.playwright:playwright`) fuer End-to-End-Tests.

- Separates Maven-Modul `travelmate-e2e` im Profil `e2e` (`-Pe2e`)
- Tests laufen gegen eine vollstaendige Docker-Compose-Umgebung
- Playwright-Browser werden beim ersten Lauf automatisch installiert

## Consequences

### Positive

- Reale Browser-Interaktion (Chromium, Firefox, WebKit) -- HTMX-Verhalten wird vollstaendig getestet
- Kein Node.js erforderlich -- bleibt im Java/Maven-Oekosystem
- Automatisches Warten auf Netzwerk- und DOM-Aenderungen -- stabiler als Selenium
- Isoliertes Modul -- E2E-Tests blockieren nicht den normalen Build
- Headless-Modus fuer CI/CD -- kein Display erforderlich

### Negative

- Playwright-Browser-Download (~200 MB) beim ersten Lauf
- Tests sind langsamer als Unit-Tests -- nur fuer kritische Flows einsetzen
- Abhaengigkeit von laufender Docker-Compose-Infrastruktur
- Debugging erfordert headed-Modus oder Trace-Viewer
