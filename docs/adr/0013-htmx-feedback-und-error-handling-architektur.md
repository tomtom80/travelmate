# ADR-0013: HTMX Feedback und Error-Handling Architektur

## Status
Vorgeschlagen

## Kontext

Die Travelmate-Anwendung verwendet HTMX fuer partielle Seitenaktualisierungen (ADR-0004), hat aber kein konsistentes Muster fuer:

1. **Erfolgs-Feedback**: Benutzer klicken Buttons, die Aktion wird ausgefuehrt, aber es gibt keine sichtbare Bestaetigung. Der Benutzer weiss nicht, ob die Aktion erfolgreich war.

2. **Fehler-Feedback**: Der `GlobalExceptionHandler` gibt Error-Toast-Fragmente zurueck, aber diese ersetzen den HTMX-Zielbereich (z.B. die Einladungsliste) statt in einem dedizierten Benachrichtigungsbereich zu erscheinen. Rohe Exception-Messages werden ohne i18n an den Benutzer weitergegeben.

3. **Ladezustaende**: Keine visuellen Indikatoren waehrend HTMX-Requests. Buttons bleiben unveraendert, kein Spinner, kein Deaktivieren.

4. **Event-Listener-Fehler**: `@TransactionalEventListener(AFTER_COMMIT)` in den `DomainEventPublisher`-Klassen haben kein Exception-Handling. Wenn RabbitMQ nicht erreichbar ist, erhaelt der Benutzer einen 500-Fehler, obwohl die Datenbank-Transaktion erfolgreich war. Die Aktion hat funktioniert, aber der Benutzer sieht einen Fehler.

5. **Code-Duplikation**: Die `GlobalExceptionHandler` in IAM und Trips sind identisch (77 Zeilen), was auf ein fehlendes Cross-Cutting-Concern-Pattern hindeutet.

Diese Probleme fuehren zu der Benutzerbeschwerde: "Benutzer klicken Buttons, aber nichts passiert, kein Feedback -- das ist sehr schlecht."

## Entscheidung

### 1. Toast-Benachrichtigungssystem

Jedes SCS-Layout (`layout/default.html`) erhaelt einen dedizierten Toast-Container:

```html
<div id="toast-container" aria-live="polite" class="toast-container"></div>
```

#### Erfolgs-Feedback via HX-Trigger

Nach erfolgreichen HTMX-Mutations-Requests setzt der Controller einen `HX-Trigger` Response-Header:

```java
response.setHeader("HX-Trigger", "{\"showToast\": {\"message\": \"Einladung gesendet\", \"type\": \"success\"}}");
```

Ein globaler HTMX-Event-Listener im Layout reagiert auf `showToast`-Events und rendert eine temporaere Benachrichtigung im Toast-Container.

#### Fehler-Feedback via hx-swap-oob

Der `GlobalExceptionHandler` gibt fuer HTMX-Requests ein Out-of-Band-Fragment zurueck, das den Toast-Container aktualisiert, ohne den urspruenglichen Zielbereich zu ueberschreiben:

```html
<div id="toast-container" hx-swap-oob="innerHTML:#toast-container">
    <div class="toast toast-error" role="alert">
        <p th:text="#{${errorMessageKey}}">Fehler</p>
    </div>
</div>
```

### 2. Ladezustaende

HTMX-Buttons erhalten die `htmx-indicator`-Klasse und einen Lade-Text. CSS-Regeln in `style.css` blenden den Indikator waehrend laufender Requests ein:

```css
.htmx-request .htmx-indicator { display: inline; }
.htmx-request button[type=submit] { opacity: 0.5; pointer-events: none; }
```

### 3. Resiliente Event-Listener

Alle `@TransactionalEventListener(AFTER_COMMIT)`-Methoden in `DomainEventPublisher`-Klassen werden mit Try-Catch umschlossen:

```java
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void onTripCreated(final TripCreated event) {
    try {
        rabbitTemplate.convertAndSend(RoutingKeys.EXCHANGE, RoutingKeys.TRIP_CREATED, event);
    } catch (final Exception e) {
        LOG.error("Failed to publish TripCreated event: {}", event, e);
    }
}
```

Event-Verlust wird akzeptiert (siehe Risk Register in Arc42 Abschnitt 11). Die langfristige Loesung ist das Transactional-Outbox-Pattern (geplant fuer spaetere Iterationen).

### 4. Konsolidierter GlobalExceptionHandler

Die identischen `GlobalExceptionHandler` werden ueberarbeitet:

- **Logging**: Alle Exceptions werden geloggt (ERROR fuer 5xx, WARN fuer 4xx).
- **i18n**: Exception-Messages werden auf i18n-Keys gemappt (`error.notFound`, `error.conflict`, `error.businessRule`, `error.internal`). Rohe Exception-Messages erscheinen nur im Log.
- **ResponseStatusException**: Wird explizit behandelt.
- **HTMX-Erkennung**: Bleibt via `HX-Request`-Header.
- **Out-of-Band-Swap**: HTMX-Fehler-Responses verwenden `hx-swap-oob` statt das Ziel-Element zu ersetzen.

Die Handler bleiben in jedem SCS (keine gemeinsame Bibliothek), aber folgen exakt demselben Pattern. Eine gemeinsame Basisklasse in `travelmate-common` ist nicht moeglich, da `travelmate-common` keine Spring-Web-Abhaengigkeit hat.

## Konsequenzen

### Positiv
- Benutzer erhalten sichtbares Feedback fuer jede Aktion (Erfolg und Fehler)
- Fehler in Event-Listenern verursachen keine falschen 500-Fehler mehr
- Ladezustaende verhindern Doppel-Klicks und signalisieren laufende Aktionen
- Exception-Messages werden nicht mehr an den Benutzer weitergegeben
- Konsistentes Verhalten ueber alle SCS hinweg

### Negativ
- Zusaetzliche Komplexitaet im Frontend (Toast-JavaScript, CSS-Indikatoren)
- Try-Catch in Event-Listenern akzeptiert Event-Verlust (bewusstes Trade-off)
- Jeder SCS muss das Toast-Pattern separat implementieren (kein Shared UI)
- HX-Trigger erfordert minimales JavaScript im Layout (ca. 15 Zeilen)

## Alternativen

### Alternative 1: Server-Side Redirect mit Flash-Attributes
- Vorteile: Kein JavaScript noetig, Spring-Standard-Pattern
- Nachteile: Funktioniert nicht mit HTMX-Partial-Updates, erfordert volle Seiten-Neuladung, verliert den Vorteil von HTMX

### Alternative 2: HTMX-Extensions (response-targets)
- Vorteile: htmx-ext `response-targets` kann Fehler-Responses an andere Ziele weiterleiten
- Nachteile: Zusaetzliche Abhaengigkeit, weniger flexibel als HX-Trigger/OOB-Swap, erfordert trotzdem Toast-Container

### Alternative 3: Event-Listener Retry mit Spring Retry
- Vorteile: Events werden nicht verloren, automatische Wiederholung
- Nachteile: Verzoegert die HTTP-Response an den Benutzer (blockiert den Thread), kann zu Timeout fuehren, loest das UX-Problem nicht

## Referenzen
- [HTMX Documentation: HX-Trigger](https://htmx.org/headers/hx-trigger/)
- [HTMX Documentation: OOB Swaps](https://htmx.org/docs/#oob_swaps)
- [HTMX Documentation: Indicators](https://htmx.org/docs/#indicators)
- [PicoCSS Documentation](https://picocss.com/docs)
- [Spring TransactionalEventListener](https://docs.spring.io/spring-framework/reference/data-access/transaction/event.html)
- ADR-0004: Thymeleaf + HTMX
- Arc42 Abschnitt 11: Risiken (Event Loss on Publish)
