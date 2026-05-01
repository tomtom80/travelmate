# Story Detail — S19-UI-POLISH-DEMO-FEEDBACK

**Iteration**: 19 (Demo-Hardening + Phase-0-GTM)
**Estimated size**: S (2–4h)
**Status**: PLANNED
**Authored**: Cross-functional team-planning session 2026-04-30

---

## User Story (RE)

**As a** demo viewer or first-time user
**I want** the UI to render consistently — correct colors, consistent
button sizing, and fully translated labels
**so that** the application feels production-ready and first impressions
are not undermined by visible cosmetic defects.

## Background

Three cosmetic UI bugs surfaced during the 2026-04-30 demo smoke on
`https://travelmate-demo.de`:

1. **Login logo darker square** — Keycloak login page rendered a faintly
   tinted square around the circular logo. Already hotfixed
   (`docker/keycloak/themes/travelmate/login/resources/css/travelmate.css:141`
   set `.kc-card { background: #ffffff }`). This story documents the fix
   for traceability and ensures regression safeguards.
2. **Accommodation Poll create page mixed locale** — title in EN
   (proper i18n key) but several sub-labels and helper texts still in DE.
3. **Edit Trip vs Cancel button size mismatch** — Trip Detail page
   shows visually unbalanced buttons (`secondary outline` vs `contrast`).

## Acceptance Criteria (RE)

**Scenario 1: Keycloak login logo background (retrospective)**
Given the Keycloak login page is loaded against the page-gradient backdrop
When DevTools inspects `.kc-card`
Then `background` computes to `#ffffff` (fully opaque)
And no darker square is visible around the circular logo at any viewport
   width between 375 px and 1440 px
And the hotfix commit is referenced in the story for traceability

**Scenario 2: Accommodation Poll create page — full English locale**
Given a browser with `Accept-Language: en` (or user has selected EN)
And the user navigates to `/trips/{tripId}/accommodationpoll/create`
When the page renders
Then every visible label, placeholder, helper text, and button caption is in English
And no German-language string appears in the rendered DOM
And `messages_en.properties` contains a translation for every
   `accommodationpoll.*` key referenced in the template (verified by `MessageBundleParityTest`)

**Scenario 3: Trip Detail — Edit and Cancel buttons visually consistent**
Given the user is on a Trip Detail page (`/trips/{tripId}`)
When both the "Edit Trip" and "Cancel" action elements are visible
Then both share the same PicoCSS class combination
And browser rendering at 375 px, 768 px, and 1280 px shows no perceptible
   height or padding difference between the two buttons
And the fix is limited to `templates/trip/detail.html` (other detail
   pages out of scope for this story)

## Edge Cases / Error Scenarios (RE)

- **Missing i18n key causes Thymeleaf to throw at render time** rather
  than silently fall back: confirm the Thymeleaf message-resolver strategy.
  Add any newly required EN keys to both `messages.properties` (DE default)
  and `messages_en.properties` to keep them in sync.
- **Button-class change ripples to other trip-detail fragments**: if
  `detail.html` includes HTMX fragments that independently apply PicoCSS
  classes to Cancel/Edit elements, those fragments must be updated alongside
  the main template, otherwise the fix regresses on partial page swaps.
- **Hotfix commit not on main**: verify the `.kc-card` CSS change is
  merged to `main` before the story is closed.

---

## DDD / Architecture Notes (Architect)

- **Aggregate impact**: None. All three findings are presentation-layer
  only — Keycloak theme CSS, Thymeleaf templates, and i18n property files.
  No domain code touched.
- **New events**: none.
- **New ports / adapters**: none.
- **Files affected**:
  - `docker/keycloak/themes/travelmate/login/resources/css/travelmate.css:141`
    (already hotfixed pre-iteration)
  - `travelmate-trips/src/main/resources/messages_en.properties` (add
    missing `accommodationpoll.*` keys — see UX section for full audit table)
  - `travelmate-trips/src/main/resources/templates/accommodationpoll/create.html`
    (audit only — German fallback comes from missing EN keys, not
    hardcoded text)
  - `travelmate-trips/src/main/resources/templates/trip/detail.html:35,50`
    (button class alignment)
- **Hexagonal conformity check**: N/A — no architectural surface touched.
- **Reuse opportunities**: existing `messages_de.properties` is the
  canonical key set; diff against `messages_en.properties` to find missing
  keys. Use whichever PicoCSS class pair is already used on Recipe-Detail
  and Accommodation-Detail to keep visual system coherent.
- **Risk + mitigation**: **Drift risk — i18n missing-key fallback is
  silent.** No test today fails when an `#{}` key is missing in EN.
  Mitigation: add `MessageBundleParityTest` (see QA section below) that
  loads both bundles and asserts `keys(de) == keys(en)`. Converts entire
  class of "mixed locale" bugs into build failures. Cheap, deterministic,
  prevents recurrence.

---

## UX Wireframes / Journey (UX Designer)

### Issue 1 — Login logo (already hotfixed)

No further UX action. AC satisfied retrospectively by commit setting
`.kc-card { background: #ffffff }`. Document-only.

### Issue 2 — Accommodation Poll i18n audit

The following keys exist in DE but are absent from EN. All must be added
to `travelmate-trips/src/main/resources/messages_en.properties`:

| Key | DE value (reference) | EN translation needed |
|---|---|---|
| `accommodationpoll.createHint` | Erfasse mindestens zwei Unterkünfte mit Zimmern, damit die Gruppe sinnvoll vergleichen kann. | Add at least two accommodations with rooms so the group can compare meaningfully. |
| `accommodationpoll.candidateHint` | Lege Name, Link, Beschreibung und Zimmer an. | Enter the name, link, description, and rooms. |
| `accommodationpoll.candidateAddress` | Adresse | Address |
| `accommodationpoll.selectTitle` | Unterkunft auswählen | Select accommodation |
| `accommodationpoll.selectAction` | Auswählen | Select |
| `accommodationpoll.kpi.candidates` | Kandidaten | Candidates |
| `accommodationpoll.kpi.votes` | Stimmen | Votes |
| `accommodationpoll.kpi.leading` | Führender Kandidat | Leading candidate |
| `accommodationpoll.kpi.status` | Status | Status |
| `accommodationpoll.kpi.open` | Noch offen | Still open |
| `accommodationpoll.showOnMap` | Auf Karte anzeigen | Show on map |
| `accommodationpoll.viewListing` | Angebot öffnen | Open listing |
| `accommodationpoll.currentWinner` | Aktuelles Ergebnis | Current result |
| `accommodationpoll.currentWinnerHint` | Diese Unterkunft ist aktuell als Ergebnis der Abstimmung markiert. | This accommodation is currently marked as the poll result. |
| `accommodationpoll.readonlyResult` | Nur lesbar | Read-only |
| `accommodationpoll.readonlyHint` | Die Abstimmung ist abgeschlossen und wird nur noch lesbar angezeigt. | The poll is closed and is now displayed as read-only. |
| `accommodationpoll.awaitingBooking` | Buchung ausstehend | Awaiting booking |
| `accommodationpoll.awaitingBookingHint` | Die Unterkunft wurde ausgewählt. Die eigentliche Buchung steht noch aus. | The accommodation has been selected. The actual booking is still pending. |
| `accommodationpoll.manageHint` | Vorschläge pflegen, per URL importieren und die finale Unterkunft festlegen. | Manage candidates, import via URL, and select the final accommodation. |
| `accommodationpoll.room.name` | Zimmername | Room name |
| `accommodationpoll.room.bedDescription` | Bettenbeschreibung | Bed description |
| `accommodationpoll.roomBeds` | Betten | Beds |
| `accommodationpoll.chart.noVotes` | Noch keine Stimmen abgegeben. | No votes yet. |
| `accommodationpoll.booking.title` | Unterkunft buchen | Book accommodation |
| `accommodationpoll.booking.hint` | wurde ausgewählt. Bitte buche die Unterkunft und markiere das Ergebnis hier. | was selected. Please book the accommodation and confirm the result here. |
| `accommodationpoll.booking.openListing` | Inserat öffnen | Open listing |
| `accommodationpoll.booking.success` | Buchung erfolgreich bestätigen | Confirm successful booking |
| `accommodationpoll.booking.fail` | Buchung fehlgeschlagen | Booking failed |
| `accommodationpoll.booking.failedHint` | konnte nicht gebucht werden. | could not be booked. |
| `accommodationpoll.booking.failedAction` | Wähle eine andere Unterkunft oder füge neue Vorschläge hinzu. | Choose a different accommodation or add new candidates. |
| `accommodationpoll.booking.failureNote` | Notiz (optional) | Note (optional) |
| `accommodationpoll.amenities` | Ausstattung | Amenities |
| `accommodationpoll.status.AWAITING_BOOKING` | Buchung ausstehend | Awaiting booking |
| `accommodationpoll.status.BOOKED` | Gebucht | Booked |
| `planning.accommodationpoll.awaitingBooking` | Buchung ausstehend | Awaiting booking |

The `create.html` template also renders amenity names via dynamic key
`#{${'amenity.' + amenity.name()}}`. Those keys must be audited separately
against the `amenity.*` namespace in both message files — outside this
story's scope but flagged as follow-up.

### Issue 3 — Trip Detail button alignment

Current state in `templates/trip/detail.html`:
- Line 35: Edit button — `<a role="button" class="secondary outline">`
- Line 50: Cancel button — `<button type="submit" class="contrast">`

PicoCSS renders `.contrast` with a filled background (same padding as
primary), while `.secondary.outline` renders border-only with slightly
different visual weight. On some viewport widths, line-height differs
between `<a role="button">` and `<button>` causing a 1–2px height mismatch.

**Recommendation**: change Cancel button to `class="secondary outline"`
to match Edit button exactly.

```html
<!-- Before -->
<button type="submit" class="contrast" th:text="#{trip.cancel}">Absagen</button>

<!-- After -->
<button type="submit" class="secondary outline" th:text="#{trip.cancel}">Absagen</button>
```

**Rationale**: Cancel is reversible-ish in PLANNING state — it triggers
an `hx-confirm` dialog before submitting (or should, per feedback-system
spec). Using `.secondary.outline` keeps it visually secondary without the
danger connotation of `.contrast`. If a future story adds `hx-confirm`,
the danger signal belongs on the confirm-dialog's confirm button, not on
the triggering button in the hero. Check Recipe Detail and Accommodation
Detail for the same pattern as a follow-up (out of this story's scope).

---

## BDD Scenarios / Test Plan (QA)

### Test pyramid distribution

- **Unit tests (1)**: `MessageBundleParityTest` (plain JUnit 5, no Spring
  context) asserts DE key-set equals EN key-set for `messages*.properties`
  files. Parameterized to run against `travelmate-trips`, `travelmate-iam`,
  `travelmate-expense` bundles.
- **Integration / Slice tests (0)**: cosmetic fixes are not business logic.
- **E2E (0 automated)**: button alignment is visual; manual checklist
  is the right artifact.
- **Manual visual-regression checklist**: 3 items (see below).

### `MessageBundleParityTest` design

```java
// travelmate-trips/src/test/java/de/evia/travelmate/trips/MessageBundleParityTest.java
class MessageBundleParityTest {

    @ParameterizedTest(name = "bundle parity: {0}")
    @MethodSource("bundlePaths")
    void de_and_en_key_sets_are_identical(final String module,
                                          final String deResource,
                                          final String enResource) throws Exception {
        final var deKeys = loadKeys(deResource);
        final var enKeys = loadKeys(enResource);

        final var missingInEn = new TreeSet<>(deKeys);
        missingInEn.removeAll(enKeys);
        final var missingInDe = new TreeSet<>(enKeys);
        missingInDe.removeAll(deKeys);

        assertThat(missingInEn)
            .as("Keys in messages_de.properties but missing from messages_en.properties in %s", module)
            .isEmpty();
        assertThat(missingInDe)
            .as("Keys in messages_en.properties but missing from messages_de.properties in %s", module)
            .isEmpty();
    }

    static Stream<Arguments> bundlePaths() {
        return Stream.of(
            Arguments.of("trips", "/messages_de.properties", "/messages_en.properties"),
            Arguments.of("trips-fallback", "/messages.properties", "/messages_en.properties")
        );
    }

    private static Set<String> loadKeys(final String resourcePath) throws Exception {
        final var props = new Properties();
        try (final var stream = MessageBundleParityTest.class.getResourceAsStream(resourcePath)) {
            assertThat(stream).as("resource not found: " + resourcePath).isNotNull();
            props.load(stream);
        }
        return props.stringPropertyNames();
    }
}
```

Runs on every `./mvnw -pl travelmate-trips test` without Spring context
overhead. When a future developer adds a German key and forgets the
English equivalent, the build breaks immediately.

### Manual visual-regression checklist

Recorded in sprint review, not automated:

```
[ ] accommodation-poll create page: verify all labels render in English
    when locale=en — specifically check candidate card headers, voting
    status labels, booking-workflow buttons.

[ ] trip detail page: "Bearbeiten" / "Absagen" buttons share class
    pair "secondary outline" — verify on desktop (≥1024px) and mobile
    (375px viewport).

[ ] login page logo: verify white logo background visible on Keycloak
    login screen (regression check) — load
    /oauth2/authorization/keycloak in incognito and inspect logo
    element background.
```

### Files to create

- `travelmate-trips/src/test/java/de/evia/travelmate/trips/MessageBundleParityTest.java`

### Test-doubles / mocking strategy

`MessageBundleParityTest`: no mocking — pure classpath resource loading
via `getResourceAsStream`.

---

## Threat Model (Security — STRIDE)

| STRIDE | Threat | Mitigation |
|---|---|---|
| **Spoofing** | New login logo asset loaded from external CDN that gets compromised → attacker replaces logo with phishing overlay | Logo served from `src/main/resources/static/img/` only. No new CDN entries. Existing CDN refs (HTMX 2.0.8, PicoCSS 2) already use SRI — verify SRI hash unchanged in this PR. |
| **Tampering** | i18n key change accidentally swaps a server-rendered label for a user-controlled value (e.g., template uses `${userField}` to look up key) | Confirm all new keys are referenced via static literals: `th:text="#{login.signup.button}"`, never `th:text="#{${dynamic}}"`. Grep PR diff for `#{${`. |
| **Repudiation** | n/a — purely cosmetic, no state change | n/a |
| **Information Disclosure** | New i18n message accidentally embeds technical detail (stack trace, internal id) in error string surfaced to anonymous users | Review every new value in `messages_*.properties` for leakage; reject any string containing class names, DB column names, stack-trace fragments. |
| **Denial of Service** | n/a | n/a |
| **Elevation of Privilege** | Button-class change ("contrast" → "secondary outline") moves a destructive action visually closer to a non-destructive one, increasing accidental high-impact clicks → no privilege gain, but UX-driven data loss | Ensure changed button (Cancel) is non-destructive in current state (PLANNING). If destructive, keep dangerous-action class plus existing `hx-confirm` HTMX attribute. Reviewer checklist item. |

### DSGVO / Privacy Notes

- No new personal-data processing.
- No 3rd-party additions.
- If logo changed at all (it didn't in this story; documenting for completeness):
  verify license + record provider in `docs/legal/asset-credits.md`.

### Concrete Action Items for the Sprint

1. Grep PR diff for new `th:utext`, `#{${`, and any new `<script src="http`
   or `<link href="http` — each must be empty or pre-approved.
2. Verify `messages_*.properties` diffs do not contain `Exception`,
   `org.springframework`, `java.`, or stack-trace patterns.
3. Confirm changed Cancel button does not control destructive flows
   (Trip/Tenant/Account deletion). PR description must annotate the
   affected route + risk class.

---

## Deployment Notes (DevOps)

### New Environment Variables

None.

### Docker / Infrastructure Changes

None. Template files compiled into the JAR at build time. No volume
mounts, no config changes.

### Database Migrations

None.

### CI/CD Pipeline Changes

None to the workflow file. The story may touch all four SCS image builds
(if i18n bundles in iam/expense are also audited as follow-up) — the
existing `publish-images` matrix job already builds all four in parallel.

### Container Restart Strategy

`docker compose up -d` (existing pipeline) is sufficient. Containers with
new image digest replaced in place. No state migration involved.

### External-Service Setup Steps

None.

### Monitoring / Metrics

No new application metrics needed. Monitor existing signals in the
30 minutes post-deploy:

- `http_server_requests_seconds_count{status="5xx", uri=~".*"}` — any new
  5xx responses from Thymeleaf rendering errors (e.g., broken `th:each`
  over null)
- Caddy access log error rate (status >= 500)
- Spring Boot `/actuator/health` — confirm all SCS remain UP

### Rollback

1. Fastest path: SSH to Hetzner VM, pull previous image tag, restart
   the affected container:

```bash
ssh travelmate-demo
docker pull ghcr.io/tomtom80/travelmate-trips:<previous-sha>
IMAGE_TAG=<previous-sha> docker compose -f docker-compose.demo.yml \
  --env-file .env.demo up -d trips
```

2. Alternatively, revert commit and let pipeline redeploy automatically
   (~10 min CI cycle).
3. No DB or Keycloak state affected — pure image replacement.

---

## Out of Scope

- Repository-wide audit of all PicoCSS-class uses for size consistency
  (this story is reactive, not proactive design-system work).
- Theme refresh or color-token changes.
- Recipe-Detail / Accommodation-Detail button-class consistency check
  (follow-up story if visual probe finds mismatches).
- `amenity.*` i18n key audit (separate namespace, separate story).
- Adding visual-regression screenshot-compare automation (would be
  iter-23 quality-gate territory).
