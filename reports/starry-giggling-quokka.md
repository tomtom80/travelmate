# Plan: Travelmate Business Model & Strategy Documentation

## Context

Du hast Travelmate als technisch durchdachtes SaaS-Produkt gebaut (DDD/Hexagonal/Multi-Tenant, EU-gehostet auf Hetzner, derzeit v0.19.0 mit laufender Demo unter travelmate-demo.de) und willst es jetzt als Geschäft betreiben. **Kein Business-Hintergrund**, **erstes Ziel ist Op-Costs decken** (~€30/Monat = 6 zahlende Kunden à €5/Monat).

Drei strategische Weichen sind gestellt:

| Dimension | Entscheidung | Implikation |
|---|---|---|
| **Zielgruppe** | B2C Freundes-/Familiengruppen | Domain-Sprache „Reisepartei" passt natürlich; Pricing bleibt im Consumer-Bereich (€3-10/Monat); kein Sales-Cycle |
| **Geo** | DACH-First (DE/AT/CH) | Deutsch als Hauptsprache, EU-Hosting (Hetzner) als Privacy-/DSGVO-Differenzierung gegen US-SaaS |
| **Zeitbudget** | 1–3h/Woche | Asynchrones Marketing zwingend; Build-in-Public + Content-Marketing; keine aktiven Sales-Aktivitäten |

**Diese drei Weichen prägen das gesamte folgende Dokument.** Ein anderer Constraint-Mix (z. B. 15h/Woche + B2B + International) würde eine völlig andere Strategie erzeugen — Pitch-Deck statt Lifetime-Deal, Cold-Outreach statt Twitter-Build-in-Public.

**Zielartefakt**: Ein umfassendes Business-Strategie-Dokument unter `docs/business/business-model-and-strategy.md` — kommittiert ins Repo, sodass du es iterativ verfeinern und mit der Codebase-History synchron halten kannst. Kein Word-Dokument, kein PDF, kein externer Notion-Workspace — Markdown im Repo, weil das deine bevorzugte Arbeitsumgebung ist und das Business-Dokument neben Architecture (`arc42/`), Backlog (`backlog/`), Operations (`operations/`) den vierten Pfeiler des Repos darstellt.

---

## Ziel-Dokument: Struktur + Inhalt

Datei: `docs/business/business-model-and-strategy.md` (neu, ~600–900 Zeilen)

### 1. Executive Summary (½ Seite)

- 3-Sätze-Pitch („Was ist Travelmate, für wen, was ist anders")
- Einordnung als **Indie-Hacker-Lifestyle-Business**, nicht VC-Startup
- Klares First-Goal: **Ramen-Profitability** (Op-Costs decken in 12 Monaten)
- Nordstern-Metrik: **Monthly Recurring Revenue (MRR) = €30** als „funktioniert"-Signal

### 2. Produkt-Status & Value Proposition

- Aktueller Reifegrad (v0.19.0, was funktioniert E2E, was noch nicht)
- **Unique Value Proposition**: Der einzige integrierte Group-Travel-Workflow — Einladung → Termin-Poll → Unterkunft-Poll → Zimmer-Aufteilung → Mealplan → Einkaufsliste → Abrechnung. Keine andere App deckt diesen kompletten Lifecycle ab.
- 3 Customer-Pain-Statements aus echten Frustrationen (WhatsApp-Chaos, Excel-Tabellen-Albträume, Geld-Driveby beim Reise-Ende)

### 3. Market Analysis (DACH-Fokus)

- **TAM** (Total Addressable Market): ca. 15M DACH-Erwachsene, die jährlich min. 1× in einer Gruppe reisen
- **SAM** (Serviceable Addressable Market): ca. 1–2M, die digital affin sind und Coordination-Tools nutzen würden
- **SOM** (Serviceable Obtainable Market): ca. 5.000–10.000 in 24 Monaten realistisch organisch erreichbar
- Trends: Post-COVID-Group-Travel-Boom, Mehrgenerationen-Reisen, Climate-aware Travel (= weniger, dafür länger gemeinsam)
- Saisonalität: Q1 (Sommerreise-Planung) und Q4 (Skifahren/Weihnachten) sind Peak-Acquisition-Fenster

### 4. Konkurrenzanalyse

Tabelle der 8–10 wichtigsten Player mit Bewertung:

| Tool | Stärke | Schwäche | Travelmate-Differenzierung |
|---|---|---|---|
| Splitwise | Marktstandard für Expense-Splitting, ~30M User | Nur Expense, nichts vor/nach der Reise | Travelmate deckt den ganzen Workflow |
| Wanderlog | Schöne Itinerary-UX, Mobile-first | Nur Itinerary, kein Polling, keine Abrechnung | Workflow-Integration |
| TripIt | Flugbuchungs-Aggregator | Kein Group-Workflow, nur Individual-Reisen | Group-First |
| Doodle | Excellent Polling | Nur Polling, sonst nix | Polling als Modul, nicht als Produkt |
| Heytrip / Cospaze / Joinmytrip | Group-Travel-Apps emerging | Mostly EN, kein DACH-Fokus, kein Privacy-Claim | DE-First, DSGVO, EU-Hosting |
| WhatsApp + Spreadsheets | Free, allgegenwärtig | Chaos, no structure, lost messages | Strukturierte Datenmodellierung |
| Notion-Templates | Flexibel, Tech-savvy User | Setup-Overhead, kein Group-Sharing-Konzept | Out-of-the-box, geteilte Reisepartei |
| Airbnb Trips | Built-in für Airbnb-Buchungen | Nur Airbnb-Ökosystem | Anbieterneutral |

Kernergebnis: **Travelmate ist nicht in einer einzelnen Kategorie, sondern an deren Schnittstelle**. Das ist Stärke (USP) und Schwäche (Marketing-Botschaft schwerer zu fassen).

### 5. Business Model Canvas (DACH-B2C-Edition)

Die 9 Felder ausgefüllt, je 4–6 Bullet-Points:

- **Customer Segments**: Reise-organisierende Personen in DACH zwischen 25–65, in Freundes-/Familiengruppen von 4–12. Persona-Beispiele: „Sandra, 38, Mama von 2 Kindern, organisiert seit 5 Jahren die Familienurlaube mit ihrer Schwester-Familie"; „Markus, 52, organisiert seit 10 Jahren die Männer-Skitour mit 8 Kumpels".
- **Value Propositions**: 3 Kernversprechen (Ende des WhatsApp-Chaos / Faire Abrechnung in 5 Min / Datensouveränität durch EU-Hosting)
- **Channels**: Organic Search / Build-in-Public / Niche-Communities (r/de, kicktipp-Community, Reise-Foren) / Word-of-Mouth / Konferenz-Talks (1×/Jahr)
- **Customer Relationships**: Self-serve, Async-Support per Mail, Public-Roadmap als Engagement
- **Revenue Streams**: 3 Modelle (Free Forever / Pay-per-Trip €5–10 / Pro-Subscription €4.99/Monat) — siehe §7
- **Key Resources**: Codebase (DDD/Hexagonal), Demo-Server, Domain, Reputation als Tech-Founder, eigenes Netzwerk
- **Key Activities**: Async-Content (Blog 2×/Monat) / Build-in-Public (LinkedIn/Bluesky 1×/Woche) / Customer-Interviews (1×/Monat) / Codebase-Maintenance
- **Key Partnerships**: Hetzner (Hosting, ggf. Sponsoring) / Strato (Mail) / OSM (Maps) / ggf. Buchungspartner für Affiliate (Late-Stage)
- **Cost Structure**: Hosting €15-25/Monat / Mail €5/Monat / Domain €10/Jahr / Tools (Plausible Analytics, Stripe) €10–20/Monat / **Zeit (1–3h/Woche, opportunity cost ignorieren bei Side-Project)**

### 6. Value Proposition Canvas

- **Customer Jobs** (4–6 functional + emotional): Reise organisieren ohne Streit, Abrechnung ohne Excel-Trauma, Mitstreiter mobilisieren, sichtbar als guter Organizer dastehen
- **Pains** (5–7): WhatsApp-Chat verliert wichtige Infos, Excel-Tabelle wird kaputt-editiert, Streitigkeiten am Reise-Ende über offene Beträge, Niemand fühlt sich für Aufgaben zuständig, Updates via Mail/Chat doppelt-und-dreifach
- **Gains** (5–7): Klare Übersicht, alle informiert, automatische Abrechnung, einladbar mit einem Klick, konsistente Erinnerungen
- **Pain Relievers** (Travelmate-Features → Pains): Reisepartei-Modell beendet WhatsApp-Chaos / Auto-Settlement beendet Excel-Trauma / Doodle-Style-Polls beenden „wann passt's?"-Threads
- **Gain Creators**: PDF-Export für Steuer / Mealplan-Vorschläge / Recipe-Library wiederverwendbar

### 7. Pricing Strategy

**Empfehlung: 3-Stufen-Modell mit Pay-per-Trip-Option**

- **Free Forever** — 1 aktiver Trip, max 5 Teilnehmer, Basis-Features
  - Hook für viralen Mund-zu-Mund
  - Limit pusht zu Upgrade, ohne offensichtliches Paywall-Aggro
- **Pay-per-Trip** — €4.99 einmalig pro Trip, beliebig viele Teilnehmer, alle Features
  - Psychologisch niedrige Hürde (kein Subscription-Commit)
  - Passt zu sporadischer Reise-Frequenz (1–3 Trips/Jahr)
  - Bezahlt durch Organizer, nicht durch Teilnehmer (= 1 Zahlung, viele Beneficiaries → viraler Effekt)
- **Pro Subscription** — €4.99/Monat oder €39/Jahr (=€3.25/Monat)
  - Unlimited Trips, alle Features, Premium-Support
  - Für Heavy-Users (Reise-Organisatoren, die mehrere Trips parallel managen)
  - Yearly-Discount = 35% incentiviert Lock-In

**Math zum Op-Cost-Ziel (€30/Monat MRR)**:
- 6 Pro-Monthly-Subscriber, ODER
- 4 Pro-Yearly-Subscriber + 2 Pay-per-Trip/Monat, ODER
- 7 Pay-per-Trip/Monat (€35 MRR-Equivalent)

→ Alle drei Pfade machbar. **Pay-per-Trip ist der wahrscheinlichste First-Conversion-Pfad** wegen der niedrigeren Hürde.

**Empfehlung „Lifetime Deal" für Early Adopters**:
- €49 einmalig für „Pro for Life" während der ersten 3 Monate Public-Beta
- Bringt Cash sofort (z. B. 10× €49 = €490 Up-Front = 16 Monate Op-Costs gedeckt)
- Schafft erste Power-User-Community
- Indie-Hacker-Standard, signalisiert Vertrauen ins Long-Term-Commitment

### 8. Go-to-Market: 12-Monats-Roadmap

Strikt am 1–3h/Woche-Constraint orientiert. Vier Phasen, je quartalsweise. Asynchrone Aktivitäten zuerst, weil sie skalieren.

**Phase 0 — Validation (Monat 0, „jetzt + nächste 4 Wochen")**
- Landing-Page mit Wait-List unter `travelmate-demo.de` oder eigener Domain
- Eigenes Netzwerk: 10 Freund/Familie-Reisepartys identifizieren, mit denen du bisher real Reisen organisiert hast — die werden deine ersten 5 Free-Beta-User
- 30-Min-Customer-Interviews mit 5 davon: „Wie planst du Reisen heute? Was nervt am meisten?" (Aufnehmen, Quotes sammeln, Pain-Statements für Marketing)
- Setze Plausible Analytics auf (privacy-friendly, EU-konform), kein Google-Analytics

**Phase 1 — Soft Launch (Monat 1–3)**
- Public-Beta öffnen (Lifetime-Deal-Angebot scharf schalten)
- Build-in-Public-Account auf Bluesky/LinkedIn starten: 1 Post/Woche („Diese Woche habe ich X gebaut")
- Erstes 4-Teile-Blog-Content: „Wie wir 8-Personen-Skitouren ohne Excel organisieren", „DSGVO und Reise-Apps", „Warum Splitwise nicht reicht für eine ganze Reise", „Travelmate-Architektur — warum DDD"
- Submission auf Product Hunt (sehr vorbereitet, einmalig — bringt 100–500 Visits in 24h)
- Submission auf IndieHackers + r/SideProject + r/de + r/Selbststaendig + Hacker News (Show HN)
- **Ziel**: 100 Wait-List-Sign-ups, 3 Lifetime-Deal-Käufe = €147 Cash

**Phase 2 — First Paying Users (Monat 4–6)**
- Wechsel von Lifetime-Deal zu regulärem Pricing
- 1×/Monat-Newsletter mit Build-Updates an Wait-List
- 2 Blog-Posts/Monat mit SEO-Fokus auf Long-Tail-Keywords:
  - „Reisekostenabrechnung Vorlage" (250 monatliche Suchen DE)
  - „Gruppenreise organisieren Tool" (100 Suchen)
  - „Skitour Abrechnung Tool" (50 Suchen)
  - „Familienurlaub planen App" (200 Suchen)
- Niche-Community-Engagement: 1× Antwort/Woche in passenden Reddit/Forum-Threads (NICHT spammig, sondern wertvoll)
- **Ziel**: 5 zahlende Pro-Subscriber + 2 Pay-per-Trip = MRR €25 — fast Op-Cost-Coverage

**Phase 3 — Op-Cost-Coverage + Slow Growth (Monat 7–12)**
- Op-Cost-Coverage erreicht → kein Druck mehr, Strategie kann ruhen oder beschleunigen
- Optional: Affiliate-Programm mit Booking.com/Airbnb (10€/Buchung) als zusätzliche Revenue-Stream
- 1× Konferenz-Talk-Submission (z. B. „Selbst-Hosted SaaS Lessons Learned" auf BarCamp Bonn / FrOSCon)
- Continue Content (1 Post/Monat reicht für SEO-Pflege)
- **Ziel**: MRR €60–100, ggf. erstes Quartal mit echten Profit über Op-Costs

### 9. Marketing-Channels (Detail)

#### Build-in-Public (Hauptchannel)

- 1×/Woche LinkedIn-Post mit Mini-Story
- 1×/Woche Bluesky-Thread (kürzer, technischer)
- Inhalt: Was diese Woche gebaut, was funktioniert, was nicht
- Zielgruppe sieht: ehrliche, technisch kompetente Person → Vertrauen

#### Content/SEO

- 2 Blog-Posts/Monat in Phase 1, 1/Monat danach
- Keyword-Recherche: kostenlose Tools (Ubersuggest free, Google Keyword Planner)
- Long-Tail über Short-Tail (kein Wettbewerb auf „Reise-App")
- Beispiel-Pipeline:
  - „Familienurlaub Kostenabrechnung Vorlage" → leitet auf Travelmate-Demo
  - „Skitour Hütte buchen wie organisieren" → leitet auf Accommodation-Poll-Feature
  - „Reisekosten gerecht aufteilen mit Kindern" → leitet auf Weighting-Feature

#### Niche-Communities

- r/de, r/Familie, r/Skitour, r/Wandern (passive Beobachtung, gezielte Antwort wenn relevant)
- kicktipp.de-Forum (Fußball-Saisons, oft mit Reisen verbunden)
- alpenvereinaktiv-Forum, hike.bike-Foren
- VHS-Skitour-Gruppen (offline! Flyer + QR auf eigene Initiative)

#### Word-of-Mouth (PROS)

- Empfehlungs-Mail-Vorlage für Beta-User: „Lade deine nächste Reisepartei ein"
- Referral-Bonus: Wer 3 zahlende Freunde wirbt, bekommt 1 Jahr Pro umsonst (in Phase 3)

### 10. Key Metrics (was zu tracken)

Wenige, klare KPIs (Plausible Analytics + manuelle Spreadsheet, kein BI-Stack):

- **Top-of-Funnel**: Unique Visitors auf travelmate-demo.de / Wait-List-Sign-ups
- **Activation**: % der Sign-ups, die in 7 Tagen Reise erstellen
- **Retention**: % der Sign-ups, die Monat 2 noch aktiv
- **Conversion**: % der Sign-ups, die zahlen (Pay-per-Trip oder Pro)
- **MRR**: Hauptzahl, monatlich tracken
- **Churn**: Wie viele Pro-Subscriber kündigen pro Monat (Ziel: <5%)

### 11. Tech-Setup für Sales/Billing

Da du Solo bist und Time-Budget knapp ist — minimal-invasive Tools:

- **Stripe Checkout** für Pay-per-Trip + Subscriptions (eingebunden in IAM-SCS, ~1 Sprint Aufwand)
- **Stripe Tax** für DACH-Mehrwertsteuer (automatisch)
- **Plausible Analytics** für Web-Tracking (€10/Monat)
- **Mailerlite Free** oder **Buttondown** für Newsletter (bis 1.000 Subscribers free)
- Keine CRM, kein Salesforce, kein HubSpot — eine Spreadsheet reicht

→ Story `S20-PAYMENT-INTEGRATION` einplanen — dazu mehr Detail im Implementation-Plan unten.

### 12. Legal/Tax-Mini-Checklist (DE)

Wichtig für Op-Cost-Coverage-Schritt:

- **Kleinunternehmer-Regelung** (§19 UStG): Bis €22.000 Jahresumsatz keine Mehrwertsteuer-Pflicht — passt für Phase 1+2
- **Gewerbeanmeldung** (~€20 einmalig) — sobald erste Einnahme generiert
- **Impressum + DSGVO-Datenschutzerklärung** rechtssicher (Generator-Vorlagen z. B. von e-recht24.de)
- **Steuer-ID** für Stripe-Auszahlungen
- **AGB** für SaaS — sehr kurz haltbar, da B2C ohnehin viele Reibungspunkte abdeckt durch §312i BGB
- **Trennung Privatkonto / Geschäftskonto** (z. B. Holvi, Kontist)

→ Aufnehmen als Operations-Doc `docs/operations/legal-bootstrapping-de.md` (Stub, später ausarbeiten)

### 13. Risiken + Mitigation

| Risiko | Wahrscheinlich | Impact | Mitigation |
|---|---|---|---|
| Niemand will zahlen | Hoch | Hoch | Customer-Interviews vor Build-Phase; Lifetime-Deal als Validation |
| Solo-Burnout | Mittel | Hoch | 1–3h/Woche-Cap einhalten; Code-Pause akzeptieren; Lifestyle, nicht Hustle |
| Konkurrenz baut Feature nach | Mittel | Mittel | DSGVO-Hosting + DACH-Localization als Burggraben |
| DSGVO-Verfahren wegen Verstoß | Niedrig | Hoch | Frühe Rechtsprüfung, Strato-Auftragsverarbeitungsvertrag |
| Hetzner-Outage | Niedrig | Mittel | Backup-Strategie (S21 Backup-Plan), Status-Page |
| Demo-Stack-Hack via SQL-Injection o. ä. | Mittel | Hoch | Phase 7 Hardening (S21-Iter), Security-Code-Review |

### 14. Konkrete 30-Tage-Action-Liste

Direkt umsetzbar nach Doc-Approval:

| Woche | Tag | Aktivität (max. 1h) |
|---|---|---|
| 1 | Mo | Customer-Interview-Liste: 10 Personen aus deinem Reise-Umfeld auflisten |
| 1 | Mi | Plausible-Analytics-Setup auf travelmate-demo.de |
| 1 | Sa | Erstes Customer-Interview führen + Notizen aufnehmen |
| 2 | Mo | Landing-Page mit Wait-List bauen (HTMX-Form auf travelmate-demo.de) |
| 2 | Mi | Mailerlite-Account anlegen, Wait-List anbinden |
| 2 | Sa | 2 weitere Customer-Interviews |
| 3 | Mo | Stripe-Account anlegen (Test-Mode) |
| 3 | Mi | Erstes Build-in-Public-Post auf LinkedIn („Travelmate v0.19 ist live") |
| 3 | Sa | Erster Blog-Post-Entwurf: „Wie wir 8-Personen-Skitouren organisieren" |
| 4 | Mo | Blog-Post veröffentlichen + auf Hacker News submitten |
| 4 | Mi | Stripe-Integration in IAM-SCS planen (Story S20-PAYMENT) |
| 4 | Sa | Review: Wie liefen die ersten 4 Wochen? Anpassen? |

---

## Implementation Steps des Plans

### Phase 1: Verzeichnis + Doc anlegen

```
docs/business/
├── README.md                              (kurze Erklärung was hier liegt)
├── business-model-and-strategy.md         (Hauptdokument, ~700 Zeilen)
├── pricing.md                             (Detail, ausgegliedert wenn nötig — Phase 2)
└── personas.md                            (Detail, ausgegliedert wenn nötig — Phase 2)
```

Phase-1-Scope: nur die Hauptdatei `business-model-and-strategy.md` mit den 14 Sektionen oben. Kein `pricing.md` / `personas.md` separat — gehört initial in die Hauptdatei, kann später extrahiert werden.

### Phase 2: Operations-Stub für Legal/Tax

`docs/operations/legal-bootstrapping-de.md` — kurzes Skelett (1 Seite) mit Checkliste:
- Gewerbeanmeldung (Stadt-Link, Kosten, Zeitfenster)
- Kleinunternehmer-Status §19 UStG
- Impressum + DSGVO (Generator-Hinweis)
- Stripe-Onboarding (Steuer-ID nötig)
- Trennung Privatkonto/Geschäftskonto (Holvi/Kontist)

### Phase 3: Iter-Plan-Ergänzungen

Story-Stub in den Iteration-Plans, damit das Business-Side den Code-Side leicht koppelt:

- **iteration-19-plan.md** oder **iteration-20-plan.md**: Story `S20-PAYMENT-INTEGRATION` (Stripe Checkout + Webhook + neue Subscription/Trip-Pass-Aggregate-Felder im IAM-Modul, ~2 Sprints)
- **iteration-19-plan.md**: Story `S19-LANDING-WAITLIST` (öffentliche Landing-Page für Wait-List + Plausible-Tracking + Mailerlite-Form, ~3 Tage)
- **iteration-20-plan.md** oder **iteration-21-plan.md**: Story `S21-PRICING-LIMITS` (Free-Tier-Enforcement: max 1 Trip + 5 Teilnehmer für Free-User; Soft-Paywall mit Upgrade-Prompt)

Diese Stories sind reine Stubs — Acceptance Criteria später beim Sprint-Planning ausarbeiten.

### Phase 4: Commit-Strategie

Drei separate Commits, weil sie inhaltlich getrennte Dimensionen abdecken:

1. `docs(business): add comprehensive business model and strategy documentation`
   — die Haupt-Datei

2. `docs(operations): add legal bootstrapping checklist for DE solopreneur`
   — Legal-Stub

3. `docs(backlog): add S19-LANDING-WAITLIST, S20-PAYMENT, S21-PRICING-LIMITS stubs`
   — Iter-Plan-Stories für die Implementation-Phase

Push als ein Block (alle drei Commits zusammen). Die Workflow-Pipeline triggert, ist aber Doku-only — keine VM-seitigen Auswirkungen.

---

## Kritische Dateien (Übersicht)

| Datei | Zustand | Rolle |
|---|---|---|
| `docs/business/README.md` | NEU | Verzeichnis-Erklärung |
| `docs/business/business-model-and-strategy.md` | NEU | Hauptdokument |
| `docs/operations/legal-bootstrapping-de.md` | NEU | Legal-Stub |
| `docs/backlog/iteration-19-plan.md` | EDIT | +S19-LANDING-WAITLIST |
| `docs/backlog/iteration-20-plan.md` | EDIT | +S20-PAYMENT-INTEGRATION |
| `docs/backlog/iteration-21-plan.md` | EDIT | +S21-PRICING-LIMITS |

---

## Verifikation

Das Plan-Outcome ist erfolgreich, wenn:

1. **Lesbarkeit**: Du kannst das `business-model-and-strategy.md`-Dokument in 20 Min einmal vollständig durchlesen und dabei zustimmen oder direkt Veto-Stellen markieren
2. **Aktionierbarkeit**: Die 30-Tage-Action-Liste ist konkret genug, dass du **morgen** mit Tag 1 (Customer-Interview-Liste) starten kannst — keine weitere Planung nötig
3. **Konsistenz**: Pricing-Math im Pricing-Abschnitt stimmt mit Op-Cost-Coverage-Ziel im Executive Summary überein (€30 MRR = 6 Pro à €5)
4. **Backlog-Anbindung**: Drei neue Story-Stubs (S19-LANDING-WAITLIST, S20-PAYMENT-INTEGRATION, S21-PRICING-LIMITS) sind in den passenden Iter-Plans angelegt → das ist der Bridge vom Business-Doc zum Code
5. **Wachstums-Ehrlichkeit**: Doc enthält keine Lifestyle-Business-Schönfärberei — explizit benannt, dass 1–3h/Woche eine Op-Cost-Coverage in 9–18 Monaten bedeutet, nicht 3 Monate

---

## Was bewusst _nicht_ im Doc steht

Damit der Doc fokussiert bleibt:

- **Kein Series-A-Pitch-Deck-Material** — VC-Slide-Format wäre an dieser Stelle Theater
- **Keine 5-Year-Financial-Projections** — bei dieser Unsicherheit reine Phantasie
- **Keine Hiring-Roadmap** — irrelevant in den ersten 12 Monaten
- **Keine Internationalisierungs-Strategie** über DACH hinaus — explizit nach DACH-First-Entscheidung verschoben
- **Kein detailliertes Personality-/Branding-Kapitel** — wird durch Build-in-Public organisch geprägt
- **Keine Investor-Outreach-Strategie** — Bootstrapping ist die Entscheidung, kein „Vielleicht später VC"-Hedging
