# Travelmate — Business Model und Strategy

**Author**: Thomas Klingler
**Status**: Living document — first edition 2026-04-30
**Geo-Fokus**: DACH (DE/AT/CH) first
**Zielgruppe**: B2C — Freundes- und Familiengruppen
**Zeitbudget des Founders**: 1–3h/Woche für Marketing/Sales/Community

---

## 1. Executive Summary

Travelmate ist eine SaaS-Plattform, die Freundes- und Familiengruppen bei der **gesamten Lifecycle-Koordination** ihrer Reisen unterstützt — von der ersten Einladung über Termin- und Unterkunfts-Polls bis zur fairen Endabrechnung. Im Gegensatz zu Splitwise (nur Expense), Doodle (nur Polling) oder Wanderlog (nur Itinerary) deckt Travelmate **den ganzen Workflow** integriert ab.

Dies ist ausdrücklich ein **Indie-Hacker-Lifestyle-Business**, kein VC-Startup. Erstes Ziel: **Ramen-Profitability** — die Operational-Costs (Hosting, Mail, Tools) von ca. €30/Monat aus zahlenden Kunden decken.

**Nordstern-Metrik**: Monthly Recurring Revenue (MRR) ≥ €30 — erreichbar mit 6 Pro-Subscribern à €5/Monat oder 7 Pay-per-Trip-Käufen pro Monat. Realistischer Zeithorizont bei 1–3h/Woche Marketing-Investment: **9–18 Monate**.

Sobald Op-Cost-Coverage erreicht ist, gilt das Produkt als „funktioniert". Weiteres Wachstum ist optional — keine Druck-Roadmap zu €1k MRR und höher.

---

## 2. Produkt-Status & Value Proposition

### 2.1 Reifegrad (Stand v0.19.0, 2026-04-30)

End-to-End funktional auf der Hetzner-Demo unter `https://travelmate-demo.de`:

- ✅ Self-Sign-up + Keycloak-OIDC-Login
- ✅ Reisepartei (Tenant) anlegen, Mitglieder einladen, Mitreisende anlegen
- ✅ Trip mit Datum-Range, Lifecycle (PLANNING → CONFIRMED → IN_PROGRESS → COMPLETED)
- ✅ Date-Polls (Doodle-Style), Accommodation-Polls mit Booking-Workflow
- ✅ Recipe-Library + Meal-Plan-Grid + Shopping-List mit HTMX-Polling
- ✅ Expense-Tracking, Receipt-Scan, gewichtete Settlement-PDF-Generierung
- ✅ External-Invitation (Mail-only-User per Trip-Einladung)
- ✅ DSGVO-konformes EU-Hosting (Hetzner Falkenstein)

In Arbeit / unfertig:
- ⏳ Payment-Integration (Stripe, Story `S20-PAYMENT-INTEGRATION`)
- ⏳ Free-Tier-Limits + Pricing-Page (Story `S21-PRICING-LIMITS`)
- ⏳ Public-Landing-Page mit Wait-List (Story `S19-LANDING-WAITLIST`)
- ⏳ Locale-aware Date/Time + User-Profile (Story `S23-USER-PROFILE-LOCALE-TZ-CURRENCY`)

### 2.2 Unique Value Proposition

> **„Schluss mit WhatsApp-Chaos und Excel-Trauma. Travelmate ist die einzige App, die deine ganze Gruppenreise begleitet — von der ersten Idee bis zur fairen Abrechnung."**

Drei Säulen:

1. **Workflow-Integration** — kein anderer Anbieter im Markt deckt Termin-Poll + Unterkunft-Poll + Mealplan + Abrechnung in einer App ab. Travelmate ist nicht „besser als Splitwise" oder „besser als Doodle"; es ist eine eigene Kategorie.

2. **Datensouveränität** — DSGVO-konformes Hosting in Deutschland (Hetzner Falkenstein), keine Datenweitergabe an US-Provider, kein Tracking-Pixel im Code. Rare in einer Branche, in der die meisten Tools bei Cloudflare-US-East-1 liegen.

3. **Domain-Sprache der Familie** — „Reisepartei" statt „Tenant", „Mitreisende(r)" statt „Dependent". Travelmate spricht Deutsch, nicht nur in der UI sondern im Konzept. Andere Tools sind übersetzte US-Software.

### 2.3 Customer-Pain-Statements

Diese drei sind aus echten Frustrationen extrahiert (eigene Erfahrung + Pre-Customer-Interviews):

- **„Wir hatten 3 WhatsApp-Gruppen für eine Reise — und niemand wusste mehr, wer welches Zimmer nimmt."** → Travelmate-Reisepartei zentralisiert.
- **„Am Ende der Reise musste ich 3 Stunden mit Excel sitzen, um zu rechnen, wer wem was schuldet."** → Auto-Settlement mit gewichteter Aufteilung.
- **„Wir konnten uns nicht auf einen Termin einigen — ich habe zigmal in der Gruppe abgefragt, wer wann kann."** → Doodle-Style-Date-Poll integriert.

---

## 3. Market Analysis (DACH-Fokus)

### 3.1 Marktgröße

| Kennzahl | Wert | Begründung |
|---|---|---|
| **TAM** (Total Addressable Market) | ~15M | DACH-Erwachsene 25–65 Jahre, die ≥ 1× pro Jahr in einer Gruppe ≥ 4 Personen reisen (Quelle: Statista DACH-Reiseverhalten 2024) |
| **SAM** (Serviceable Addressable Market) | ~1–2M | TAM × ~10% digital-affin und bereit für SaaS-Tools |
| **SOM** (Serviceable Obtainable Market) | ~5.000–10.000 | In 24 Monaten organisch realistisch erreichbar (Indie-Marketing-Zahlen) |

Konservativ gerechnet: bei 5% Conversion zu zahlenden Pro-Subscribern wären das 250–500 zahlende User → MRR €1.250–2.500. Das ist 40–80× über dem ersten Ziel und damit eine realistische 24-Monats-Obergrenze.

### 3.2 Trends, die Travelmate begünstigen

- **Post-COVID Group-Travel-Boom**: Mehrgenerationen-Reisen +25% vs 2019, längere Aufenthalte, höhere Bedeutung von „bewussten Reise-Gruppen"
- **Climate-aware Travel**: Weniger Flüge, dafür längere und intensivere Trips → mehr Koordinationsbedarf
- **Privacy-Awakening in DACH**: Wachsende Aversion gegen US-SaaS, DSGVO-Bewusstsein höher als im internationalen Schnitt
- **Remote-Work-Workations**: Tech-affine Zielgruppe organisiert vermehrt Co-Working-Reisen, die sich strukturell wie Familienreisen verhalten

### 3.3 Saisonalität

- **Q1 (Jan–Mar)**: Sommerreise-Planung beginnt → Peak-Akquisition-Fenster #1
- **Q2 (Apr–Jun)**: Konkrete Sommerbuchungen → hohe Conversion auf Pay-per-Trip
- **Q3 (Jul–Sep)**: Live-Reise-Phase → Retention/Referral, weniger Acquisition
- **Q4 (Oct–Dec)**: Skifahren/Weihnachten/Silvester-Reisen → Peak-Akquisition-Fenster #2

Marketing-Plan sollte gezielt vor Q1 und Q4 verstärkt aktiv sein. Q3 ist „Beobachtungs-Zeit" mit Customer-Interviews und Code-Pflege.

---

## 4. Konkurrenzanalyse

### 4.1 Kernspieler

| Tool | Kategorie | Stärke | Schwäche | Travelmate-Differenzierung |
|---|---|---|---|---|
| **Splitwise** | Expense-Splitting | Marktstandard, ~30M User | Nur Expense, kein Pre-Trip-Workflow, keine Polling-Funktion | Travelmate deckt vollen Lifecycle |
| **Wanderlog** | Trip-Itinerary | Schöne Mobile-UX, Free-Tier | Nur Itinerary, kein Group-Workflow, kein Settlement | Group-First |
| **TripIt** | Booking-Aggregator | Auto-Parse von Bestätigungs-Mails | Individual-Reisen, nicht Group, $49/Jahr | Group-First |
| **Doodle** | Polling | Excellente Polling-UX | Nur Polling, kein Trip-Kontext, $7/Monat | Polling als Modul integriert |
| **Heytrip** / **Cospaze** / **Joinmytrip** | Group-Travel | Junge Konkurrenten, wachsen | Mostly EN, kein DACH-Fokus, kein DSGVO-Claim | DE-First, EU-Hosting |
| **WhatsApp + Spreadsheets** | DIY | Frei, allgegenwärtig | Chaos, verlorene Infos, kein Datenmodell | Strukturierte Datenmodellierung |
| **Notion-Templates** | DIY-Tech | Flexibel | Hoher Setup-Overhead, kein Group-Konzept | Out-of-the-box |
| **Airbnb Trips** | Verticalize | In-App nahtlos für Airbnb-Buchungen | Nur Airbnb-Ökosystem, keine externen Unterkünfte | Anbieterneutral |

### 4.2 Strategische Einordnung

- **Splitwise + Doodle + Wanderlog zusammen** sind im Prinzip „Travelmate auf 3 Apps verteilt". User müssen Daten manuell zwischen ihnen syncen → Reibung, die Travelmate beseitigt.
- **WhatsApp + Excel** ist der größte „Konkurrent" — nicht weil's gut ist, sondern weil's da ist. Marketing-Botschaft muss Excel-Trauma adressieren, nicht Feature-Parity zu Splitwise.
- **Heytrip/Cospaze** sind die echten Konkurrenten in der gleichen Kategorie. Aber: keiner davon hat DACH-Niche-Fokus oder DSGVO-Hosting. Das ist der Burggraben.

### 4.3 Kategorien-Positionierung

> Travelmate ist nicht in einer Kategorie, sondern an deren Schnittstelle.

Stärke: Niche-Position, die niemand komplett besetzt. Schwäche: Marketing-Botschaft schwerer zu fassen — User suchen nach „Splitwise-Alternative" oder „Doodle für Reisen", nicht nach „Group-Travel-Workflow-Tool". Daher: **Long-Tail-SEO-Strategie ist Pflicht**, breite Kategorie-Werbung hat keinen Sinn.

---

## 5. Business Model Canvas

### 5.1 Customer Segments

Primary: **DACH B2C Group-Travel-Organizer**, Alter 25–65, mittlere bis höhere digitale Affinität.

Sub-Segmente (Personas):

- **Sandra, 38, Familienurlaubsorganizerin**: Mama von 2 Kindern, plant seit 5 Jahren die jährliche Familienurlaube mit ihrer Schwester-Familie (8 Personen total). Pain: Excel-Tabelle, in der ständig Spalten verrutschen.
- **Markus, 52, Skitouren-Captain**: Plant seit 10 Jahren die jährliche Männer-Skitour mit 8 Kumpels. Pain: WhatsApp-Chaos, jedes Jahr die gleichen Diskussionen über Hütten und Termine.
- **Lisa, 29, Workation-Organizer**: Organisiert mit ihrem Remote-Team 2× jährlich Co-Working-Reisen. Pain: zeitintensive Koordination, Abrechnung mit gemischten Zahlungsmodellen (Firma vs Privat).
- **Klaus, 65, Großeltern-Generation**: Plant Mehrgenerationen-Reisen mit Kindern und Enkeln (12 Personen). Pain: Komplexe Aufteilungen (Senioren-Rabatte, Kinder-Sonderkosten).

### 5.2 Value Propositions

Drei Kernversprechen, je auf eine Customer-Pain mappend:

1. **„Ende des WhatsApp-Chaos"** — Strukturierte Reisepartei mit Rollen, Mitreisenden und Trip-Container-Modell.
2. **„Faire Abrechnung in 5 Minuten"** — Auto-Settlement mit gewichteter Kosten-Aufteilung (Erwachsene 1.0, Kinder <3 Jahre 0.0, alles dazwischen konfigurierbar).
3. **„Datensouveränität durch EU-Hosting"** — DSGVO-konform, keine US-Cloud-Provider, kein Tracking. Premium-Botschaft im DACH-Markt.

### 5.3 Channels

- **Organic Search** (SEO Long-Tail) — Hauptchannel, weil 1–3h/Woche-kompatibel
- **Build-in-Public** (LinkedIn + Bluesky) — 1×/Woche je Plattform, niedriger Aufwand pro Post
- **Niche-Communities** — passive Beobachtung, gezielte Antworten:
  - r/de, r/Familie, r/Skitour, r/Wandern, r/Selbststaendig
  - kicktipp.de-Forum (Sport-Reise-Gruppen)
  - alpenvereinaktiv-Forum
  - hike.bike-Foren
- **Content-Marketing** — Blog 1–2×/Monat, immer Long-Tail-SEO-orientiert
- **Word-of-Mouth** — incentiviert über Referral-Bonus (Phase 3)
- **Konferenz-Talks** — 1×/Jahr (BarCamp Bonn, FrOSCon, GAFAM-Alternativen-Treffen) — niedriger Aufwand, hohe Reichweite

### 5.4 Customer Relationships

- **Self-serve** — User onboarded sich selber, kein Sales-Touch
- **Async-Support per Mail** — Antwortzeit max. 48h, ein Mail-Ticket-System reicht (Buttondown oder einfach Inbox)
- **Public-Roadmap als Engagement** — `docs/backlog/` ist öffentlich, User können Issues kommentieren, mitvoten
- **Build-in-Public-Stream** als Community-Bonding — User folgen LinkedIn/Bluesky und sehen den Fortschritt

### 5.5 Revenue Streams

Drei Modelle parallel:

| Modell | Preis | Zielsegment | Erwartete Verteilung |
|---|---|---|---|
| **Free Forever** | €0 | First-time-User, gelegentliche Nutzer | 80% der Sign-ups |
| **Pay-per-Trip** | €4.99 einmalig | Sporadische Reise-Organizer (1–3 Trips/Jahr) | 15% der Sign-ups, ~40% des Revenues |
| **Pro Subscription** | €4.99/Monat oder €39/Jahr | Heavy-User, Multi-Trip-Organizer | 5% der Sign-ups, ~60% des Revenues |
| **Lifetime Deal** (Phase 1) | €49 einmalig | Early Adopters, Beta-Phase | Limited (~10–30 Verkäufe), zusätzliches Cash-Flow |

Details siehe §7.

### 5.6 Key Resources

- **Codebase** (DDD/Hexagonal/Multi-Tenant) — Travelmate-Core, technisch durchdacht
- **Demo-Server + Domain** (`travelmate-demo.de` auf Hetzner) — laufende Beta
- **Reputation als Tech-Founder** — bei DDD/Java-Community (potentielle Build-in-Public-Audience)
- **Eigenes Netzwerk** — Familie, Freunde, Ex-Kollegen (= erste 5–10 Beta-User-Kandidaten)
- **Inhaltliche Domain-Expertise** — selbst Reise-Organizer, kennt die Pains aus erster Hand

### 5.7 Key Activities

- **Async-Content** — Blog 1–2×/Monat, optimiert für SEO Long-Tail
- **Build-in-Public** — 1×/Woche LinkedIn-Post + 1×/Woche Bluesky
- **Customer-Interviews** — 1×/Monat Call mit Beta-User, Lessons → Feature-Backlog
- **Codebase-Maintenance** — Iterations-Plan-Stories implementieren, Bugs fixen
- **Community-Engagement** — 1× wertvoller Beitrag/Woche in Niche-Foren

### 5.8 Key Partnerships

- **Hetzner** — Infrastruktur-Provider, ggf. später Sponsoring-Programm „Hetzner-hosted Apps"
- **Strato** — Mail-Provider (vorläufig, ggf. Wechsel zu Brevo für Skalierung)
- **OpenStreetMap** — Maps-Anbieter (kostenlos, kein Google-Maps-Lock-In)
- **Booking.com / Airbnb** — Affiliate-Programm in Phase 3 (10€/Buchung über Affiliate-Link)
- **DSGVO-aware-Communities** — Allianzen für Cross-Promotion (z. B. Cryptpad, Nextcloud-Community)

### 5.9 Cost Structure

Monatlich:

| Position | Kosten |
|---|---|
| Hetzner CAX21 VM | €15–20 |
| Strato Mail (1 Postfach) | €3–5 |
| Plausible Analytics | €9 |
| Mailerlite Free (bis 1000 Subs) | €0 |
| Stripe Transaction Fees (auf €30 MRR ~3%) | €1 |
| Kontist / Holvi Geschäftskonto | €0–9 |
| Domain (auf Monat umgerechnet) | €1 |
| **Total Op-Costs** | **~€30–45/Monat** |

Zeit (Founder): 1–3h/Woche — opportunity cost ignoriert (Side-Project-Mentalität).

Einmalige Kosten:
- Gewerbeanmeldung: €20
- Anwalts-Check für AGB/Impressum (optional, ~€200–300)
- Markenrecherche „Travelmate" (DPMA, ~€30 für Suche)

---

## 6. Value Proposition Canvas

### 6.1 Customer Jobs

Funktional:
- **Reise organisieren** ohne Streit und Reibung
- **Termine finden**, an denen alle können
- **Unterkunft buchen**, mit der alle einverstanden sind
- **Kosten gerecht aufteilen**, auch wenn Personen unterschiedlich profitieren
- **Nachträglich abrechnen**, ohne dass Konflikte über offene Beträge entstehen

Emotional:
- **Als guter Organisator dastehen** — Kompetenz nach außen demonstrieren
- **Stress vermeiden** — Reise selbst genießen, nicht nur planen
- **Konflikte vermeiden** — gerade bei Familienreisen heikel

### 6.2 Pains

- WhatsApp-Chats verlieren wichtige Infos
- Excel-Tabellen werden gegenseitig kaputt-editiert
- Niemand fühlt sich für Aufgaben zuständig
- Streitigkeiten am Reise-Ende über offene Beträge
- Updates via Mail/Chat doppelt-und-dreifach kommuniziert
- Privacy-Angst gegenüber US-SaaS bei sensiblen Daten (Geburtsdaten der Kinder, Wohnadressen)
- Unklare Verteilung bei gemischten Gruppen (Erwachsene+Kinder, Senior+Junior)

### 6.3 Gains

- Klare Übersicht, alle informiert
- Automatische Abrechnung
- Ein-Klick-Einladbarkeit
- Konsistente Erinnerungen
- Mobile-zugänglich (PWA)
- DSGVO-Sicherheit
- Wiederverwendbarkeit (Recipe-Library, Vorlagen)

### 6.4 Pain Relievers (Travelmate-Features)

| Travelmate-Feature | Adressierter Pain |
|---|---|
| Reisepartei-Modell | Beendet WhatsApp-Chaos |
| Auto-Settlement mit Weighting | Beendet Excel-Trauma |
| Doodle-Style-Date-Poll | Beendet „wann passt's?"-Threads |
| Accommodation-Poll mit Booking-Workflow | Strukturiert „wo schlafen wir?" |
| External-Invitation-Flow | Reduziert Onboarding-Reibung |
| EU-Hosting + DSGVO | Reduziert Privacy-Angst |
| Rollen (Organizer, Participant) | Klare Verantwortungs-Zuordnung |

### 6.5 Gain Creators

- **PDF-Export der Settlement** für Steuer-Doku oder Beleg
- **Mealplan-Vorschläge** (geplant in Iter-19) für die Feiertags-Reisen
- **Recipe-Library** wiederverwendbar über Trips hinweg
- **Mobile-PWA** — Travelmate auf'm Handy auch ohne App-Store

---

## 7. Pricing Strategy

### 7.1 3-Stufen-Modell

#### Free Forever
- **Preis**: €0
- **Limits**: 1 aktiver Trip gleichzeitig, max 5 Teilnehmer pro Trip
- **Features**: Reisepartei, Trip, Date-Poll, Settlement (Basic)
- **Zweck**: Hook für viralen Mund-zu-Mund. Gratis-Alternative zu WhatsApp+Excel.
- **Limit-Push**: Bei Limit-Erreichen Soft-Paywall „Upgrade auf Pro für unbegrenzt"

#### Pay-per-Trip
- **Preis**: €4.99 einmalig pro Trip (Trip-Pass)
- **Limits**: keine — beliebig viele Teilnehmer, alle Features
- **Features**: alles inkl. Accommodation-Poll, Mealplan, Shopping-List, PDF-Settlement
- **Zweck**: Niedrigste psychologische Hürde. Passt zur sporadischen Reise-Frequenz (1–3 Trips/Jahr).
- **Viraler Hook**: Bezahlt durch den Organizer, beneficiates die ganze Gruppe → ein Zahlung erzeugt 5–10 zufriedene User

#### Pro Subscription
- **Preis**: €4.99/Monat oder €39/Jahr (= €3.25/Monat, 35% Yearly-Discount)
- **Limits**: keine
- **Features**: alles + Premium-Support + Multi-Trip-Parallelität + (zukünftig) Recipe-Sharing-Marketplace
- **Zweck**: Recurring Revenue für Heavy-Users (Multi-Trip-Organisatoren)
- **Yearly-Lock-In**: Yearly-Discount incentiviert 12-Monats-Commitment, reduziert Churn

#### Lifetime Deal (Phase-1-only, max. 50 Verkäufe)
- **Preis**: €49 einmalig — „Pro for Life"
- **Limits**: keine, lifetime
- **Zweck**: Schnelles Cash für Op-Cost-Coverage + Power-User-Community
- **Math**: 10 Verkäufe = €490 = ~16 Monate Op-Costs. 50 Verkäufe = €2.450 = ~7 Jahre Op-Costs.
- **Indie-Hacker-Standard**: Signalisiert „committed Founder", baut frühe Vertrauensbasis

### 7.2 Math zum Op-Cost-Ziel

€30/Monat MRR-Coverage erreichbar durch:

- **Pfad A**: 6 Pro-Monthly-Subscriber → €30/Monat
- **Pfad B**: 4 Pro-Yearly-Subscriber + 2 Pay-per-Trip/Monat → €13 + €10 + €10 = €33/Monat (jährlich umgelegt)
- **Pfad C**: 7 Pay-per-Trip/Monat → €35/Monat-äquivalent

**Wahrscheinlichster First-Conversion-Pfad: Pay-per-Trip.** Die niedrigere Hürde + die sporadische Reise-Frequenz machen es zur natürlichen Eintritts-Option. Pro-Subscription kommt erst nach Erfahrung mit Pay-per-Trip.

### 7.3 Preisrechtfertigung — warum €4.99?

- **Splitwise Pro** kostet $3/Monat (~€2.79). Travelmate bietet ~3× den Funktionsumfang.
- **Wanderlog Pro** kostet $39/Jahr (~€36/Jahr). Travelmate-Pro-Yearly bei €39/Jahr ist konkurrenzfähig.
- **Doodle Pro** kostet $6.95/Monat. Travelmate kostet weniger und bietet Polling als ein Feature unter mehreren.
- **Psychologisch**: €4.99 ist „unter €5" — wichtige psychologische Schwelle bei B2C-DE.

### 7.4 Was NICHT funktioniert (bewusst nicht eingesetzt)

- **Freemium ohne Limits** — würde keinen Kaufanreiz schaffen
- **Free Trial mit Auto-Subscription** — schlechte Reputation in DACH, viele Ad-Block-User vermeiden
- **Per-User-Pricing** — würde Group-Adoption hemmen, Gruppen reisen genau dann, wenn alle dabei sind
- **Variable-Pricing nach Trip-Größe** — zu komplex, Conversion-Killer

---

## 8. Go-to-Market Roadmap

### Phase 0 — Validation (Monat 0, jetzt + nächste 4 Wochen)

Ziel: Customer-Pain bestätigen, Wait-List aufbauen, Plausible-Analytics live.

- Customer-Interview-Liste: 10 Personen aus eigenem Reise-Umfeld auflisten (Eltern, Geschwister, beste Freunde, Kollegen)
- 5 Customer-Interviews à 30 Min führen — Aufnahme + Quotes für Marketing sammeln
- Plausible-Analytics-Setup auf travelmate-demo.de
- Story `S19-LANDING-WAITLIST` planen (in Iter-19)

**Erfolgs-Indikator**: 5 echte Customer-Interviews mit dokumentierten Pain-Quotes.

### Phase 1 — Soft Launch (Monat 1–3)

Ziel: 100 Wait-List-Sign-ups, 3 Lifetime-Deal-Käufe (€147 Cash).

- Public-Beta öffnen, Lifetime-Deal-Angebot scharf schalten (€49, max. 50)
- Build-in-Public-Account auf LinkedIn + Bluesky starten — 1 Post/Woche je
- Erste 4 Blog-Posts:
  - „Wie wir 8-Personen-Skitouren ohne Excel organisieren"
  - „DSGVO und Reise-Apps — warum Strato besser ist als Splitwise"
  - „Warum Splitwise nicht reicht für eine ganze Reise"
  - „Travelmate-Architektur — warum DDD bei einer Reise-App"
- Submission auf:
  - Product Hunt (vorbereitet, einmalig — bringt 100–500 Visits in 24h)
  - IndieHackers
  - r/SideProject
  - r/de
  - r/Selbststaendig
  - Hacker News (Show HN)
- Newsletter-Setup (Mailerlite Free)

**Erfolgs-Indikator**: 100 Wait-List-Sign-ups + 3 Lifetime-Deal-Käufe.

### Phase 2 — First Paying Users (Monat 4–6)

Ziel: MRR €15–25, fast Op-Cost-Coverage.

- Lifetime-Deal beenden, regulärer Pricing-Mode an
- Newsletter 1×/Monat mit Build-Updates an Wait-List
- Blog-Frequenz: 2×/Monat mit SEO-Long-Tail-Keywords:
  - „Reisekostenabrechnung Vorlage" (250 Suchen/Monat DE)
  - „Gruppenreise organisieren Tool" (100/Monat)
  - „Skitour Abrechnung Tool" (50/Monat)
  - „Familienurlaub planen App" (200/Monat)
- Niche-Community-Engagement: 1× wertvolle Antwort/Woche in passenden Threads (NICHT spammig)
- Iter-20 + Iter-21 Stories implementieren (Stripe + Pricing-Limits)

**Erfolgs-Indikator**: 5 zahlende Pro-Subscriber + 2 Pay-per-Trip = MRR €25 — fast Op-Cost-Coverage.

### Phase 3 — Op-Cost-Coverage + Slow Growth (Monat 7–12)

Ziel: MRR €60–100, ggf. erstes Quartal mit Profit über Op-Costs.

- Op-Cost-Coverage erreicht → kein Druck mehr, Strategie kann ruhen oder beschleunigen
- Affiliate-Programm mit Booking.com + Airbnb (10€/Buchung) als zusätzliche Revenue-Stream
- 1× Konferenz-Talk-Submission (BarCamp Bonn, FrOSCon, c't-Webdev-Konferenz):
  - „Selbst-Hosted SaaS — Lessons from Travelmate"
  - „DDD bei einem Reise-Group-Tool"
- Content-Pflege: 1 Post/Monat reicht für SEO-Pflege
- Referral-Programm scharf schalten: 3 zahlende Freunde geworben → 1 Jahr Pro umsonst

**Erfolgs-Indikator**: MRR ≥ €60 — Coverage + 50% Puffer für unvorhergesehene Kosten.

---

## 9. Marketing Channels — Detail

### 9.1 Build-in-Public (Hauptchannel)

- **LinkedIn**: 1×/Woche, je 100–300 Wörter. Inhalt: „Diese Woche habe ich X gebaut, Y gelernt, Z war frustrierend." Persönlich, technisch, ehrlich.
- **Bluesky**: 1×/Woche, je 200–280 Zeichen Thread. Kürzer, technischer, mehr Niche.
- **Frequenz halten ist wichtiger als Glanz**. Ein durchschnittlicher wöchentlicher Post > ein perfekter monatlicher.

**Tooling**: Buffer Free (3 Channels, 10 Posts queued) oder direkt nativ.

### 9.2 Content-Marketing / SEO

- 2 Blog-Posts/Monat in Phase 1, 1/Monat danach
- Long-Tail über Short-Tail (kein Wettbewerb auf „Reise-App")
- Keyword-Recherche: kostenlose Tools (Ubersuggest free, Google Keyword Planner)
- Beispiel-Pipeline:
  - „Familienurlaub Kostenabrechnung Vorlage" → Travelmate-Demo + Settlement-Feature-Walkthrough
  - „Skitour Hütte buchen wie organisieren" → Accommodation-Poll-Feature
  - „Reisekosten gerecht aufteilen mit Kindern" → Weighting-Feature
  - „Gruppenreise Excel Vorlage Probleme" → Travelmate als Lösung

Jeder Blog-Post enthält:
- Echte User-Story (anonymisiert) als Hook
- Lösungsweg
- Dezenter Travelmate-Demo-Link am Ende (kein Hard-Sell)
- 1–2 Screenshots, falls relevant

### 9.3 Niche-Communities

Reddit:
- r/de (allgemein)
- r/Familie
- r/Skitour
- r/Wandern
- r/Selbststaendig (Ops-Themen)
- r/SideProject (Build-in-Public)
- r/IndieHackers

Foren:
- kicktipp.de-Forum (Sportverein-Reisen)
- alpenvereinaktiv-Forum
- hike.bike-Foren
- VHS-Skitour-Gruppen (offline! Flyer + QR-Code, eigene Initiative)

Strategie:
- 1× wertvolle Antwort/Woche
- NICHT „kommt mal Travelmate testen" — das ist Spam und tötet die Community-Reputation
- Stattdessen: ehrlich Antworten geben, am Ende „PS: Falls Excel-Tabellen das Problem sind, schau Travelmate an" als Soft-Mention

### 9.4 Word-of-Mouth

- Empfehlungs-Mail-Vorlage für Beta-User: „Lade deine nächste Reisepartei ein"
- Referral-Bonus (Phase 3): Wer 3 zahlende Freunde wirbt, bekommt 1 Jahr Pro umsonst

### 9.5 Konferenz-Talks (Phase 3)

Konferenzen, an denen Travelmate als Speaker passt:
- **BarCamp Bonn** (1× Jahr)
- **FrOSCon** (Aug. jährlich, St. Augustin)
- **c't-Webdev-Konferenz**
- **DSGVO-Awareness-Konferenzen** (verschiedene)
- **JUG (Java User Groups)** lokale Treffen

Talk-Themen:
- „Selbst-Hosted SaaS — Lessons from a Solo Founder"
- „DDD in einer Group-Travel-App"
- „Cost-aware Cloud — Travelmate auf Hetzner für €30/Monat"

---

## 10. Key Metrics

Wenige, klare KPIs — keine BI-Stack, eine Spreadsheet reicht:

| Metric | Tooling | Messung-Frequenz | Ziel Phase 2 | Ziel Phase 3 |
|---|---|---|---|---|
| **Unique Visitors** auf travelmate-demo.de | Plausible | wöchentlich | 200/Woche | 500/Woche |
| **Wait-List-Sign-ups** | Mailerlite | wöchentlich | 100 total | 300 total |
| **Activation %** (Sign-up → Trip in 7 Tagen) | Manuelle Spreadsheet | monatlich | 30% | 40% |
| **Retention %** (Sign-up → Monat 2 aktiv) | Manuelle Spreadsheet | monatlich | 20% | 30% |
| **Conversion %** (Sign-up → zahlt) | Stripe Dashboard | monatlich | 5% | 7% |
| **MRR** | Stripe Dashboard | monatlich | €25 | €60+ |
| **Churn %** Pro-Sub | Stripe Dashboard | monatlich | <10% | <5% |

---

## 11. Tech-Setup für Sales/Billing

Solo-Solo-Setup, minimal-invasiv:

- **Stripe Checkout** für Pay-per-Trip + Subscription
  - Eingebunden in IAM-SCS (Story `S20-PAYMENT-INTEGRATION`)
  - Stripe Webhook für Subscription-Lifecycle-Events
  - Stripe Tax für DACH-MwSt automatisch
- **Plausible Analytics** für Web-Tracking — privacy-friendly, EU-konform, €9/Monat
- **Mailerlite Free** für Newsletter — bis 1.000 Subs gratis
- **Buttondown** als Alternative für Mail (mehr Solo-Indie-tauglich, $9/Monat ab 1k)

Kein:
- CRM (Salesforce, HubSpot) — Spreadsheet reicht
- Helpdesk (Zendesk) — eine Inbox reicht für die ersten 100 User
- Marketing-Automation — manuelle Mails sind authentischer
- Customer-Success-Tool — 1×/Monat manuell durchgehen

---

## 12. Legal/Tax-Mini-Checklist (DE)

Ausführlicher Stub in `docs/operations/legal-bootstrapping-de.md`. Hier nur Headlines:

- **Kleinunternehmer-Regelung** (§19 UStG): Bis €22.000 Jahresumsatz keine Mehrwertsteuer-Pflicht
- **Gewerbeanmeldung** (~€20 einmalig) — sobald erste Einnahme
- **Impressum + DSGVO-Erklärung** (Generator-Vorlagen z. B. e-recht24.de)
- **Steuer-ID** für Stripe-Auszahlungen
- **AGB für SaaS** (kurz halbar, B2C ist großteils durch BGB abgedeckt)
- **Trennung Privatkonto / Geschäftskonto** (Holvi, Kontist — beide kostenlos für Kleinunternehmer)

---

## 13. Risiken + Mitigation

| Risiko | Wahrscheinlich | Impact | Mitigation |
|---|---|---|---|
| **Niemand will zahlen** | Hoch | Hoch | Customer-Interviews vor Build-Phase; Lifetime-Deal als Validation |
| **Solo-Burnout** | Mittel | Hoch | 1–3h/Woche-Cap einhalten; Code-Pause akzeptieren; Lifestyle, nicht Hustle |
| **Konkurrenz baut Feature nach** | Mittel | Mittel | DSGVO-Hosting + DACH-Localization als Burggraben |
| **DSGVO-Verfahren wegen Verstoß** | Niedrig | Hoch | Frühe Rechtsprüfung, Strato-Auftragsverarbeitungsvertrag |
| **Hetzner-Outage** | Niedrig | Mittel | Backup-Strategie (Iter-21), Status-Page |
| **Demo-Stack-Hack via SQL-Injection o. ä.** | Mittel | Hoch | Iter-21 Hardening, Security-Code-Review |
| **Markennamen-Konflikt „Travelmate"** | Mittel | Mittel | DPMA-Recherche vor Marketing-Push; Markenanmeldung evtl. später |
| **Strato-Mail-Limits / Sender-Reputation** | Mittel | Mittel | Bei Skalierung Wechsel zu Brevo/Mailgun |
| **Wait-List konvertiert nicht** | Mittel | Mittel | A/B-Test Pricing, Feedback-Loop mit Wait-List-Subs |

---

## 14. 30-Tage-Action-Liste

Direkt umsetzbar nach Doc-Approval. Max 1h pro Slot, drei Slots/Woche (Mo-Mi-Sa).

| Woche | Tag | Aktivität |
|---|---|---|
| **1** | Mo | Customer-Interview-Liste: 10 Personen aus eigenem Reise-Umfeld auflisten |
| 1 | Mi | Plausible-Analytics-Setup auf travelmate-demo.de |
| 1 | Sa | 1. Customer-Interview führen + Notizen aufnehmen |
| **2** | Mo | Landing-Page mit Wait-List bauen (HTMX-Form auf travelmate-demo.de) |
| 2 | Mi | Mailerlite-Account anlegen, Wait-List anbinden |
| 2 | Sa | 2 weitere Customer-Interviews |
| **3** | Mo | Stripe-Account anlegen (Test-Mode) |
| 3 | Mi | Erstes Build-in-Public-Post auf LinkedIn („Travelmate v0.19 ist live") |
| 3 | Sa | Erster Blog-Post-Entwurf: „Wie wir 8-Personen-Skitouren organisieren" |
| **4** | Mo | Blog-Post veröffentlichen + auf Hacker News (Show HN) submitten |
| 4 | Mi | Stripe-Integration in IAM-SCS planen (Story `S20-PAYMENT-INTEGRATION`) |
| 4 | Sa | Review: Wie liefen die ersten 4 Wochen? Was anpassen? |

---

## 15. Was bewusst _nicht_ in dieser Strategie steht

- **Series-A-Pitch-Deck-Material** — VC-Slide-Format wäre an dieser Stelle Theater
- **5-Year-Financial-Projections** — bei dieser Unsicherheit reine Phantasie
- **Hiring-Roadmap** — irrelevant in den ersten 12 Monaten
- **Internationalisierungs-Strategie** über DACH hinaus — explizit nach DACH-First-Entscheidung verschoben
- **Detailliertes Personality-/Branding-Kapitel** — wird durch Build-in-Public organisch geprägt
- **Investor-Outreach-Strategie** — Bootstrapping ist die Entscheidung, kein „Vielleicht später VC"-Hedging
- **Mobile-App-Planung** — PWA reicht erstmal, native App ist Iter-25+ Thema
- **Community-Forum / Discord** — bei <100 User irrelevant, würde nur Aufwand erzeugen ohne Engagement

---

## Iteration: Wann dieses Dokument aktualisiert wird

Update-Trigger:

1. **Nach 5 Customer-Interviews**: Persona-Sektion (§5.1) verfeinern mit echten Quotes
2. **Nach Phase 1 (Monat 3)**: Pricing-Math (§7) mit echten Conversion-Daten validieren
3. **Bei MRR €30 erreicht**: Strategy-Review — Op-Cost-Coverage erreicht, was kommt jetzt?
4. **Bei Churn > 10%**: Customer-Interviews mit Cancellern, Doc-Section für Retention-Improvements
5. **Bei neuem Konkurrenten**: §4 Konkurrenzanalyse aktualisieren

Updates erfolgen als git commits mit Prefix `docs(business):`.

---

**Document last updated**: 2026-04-30
**Next review trigger**: Nach 5 Customer-Interviews (geplant Phase 0, ~Monat 1)
