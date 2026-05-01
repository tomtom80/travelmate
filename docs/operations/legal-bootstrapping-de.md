# Legal Bootstrapping — DE Solopreneur Checkliste

**Status**: Stub — vor erster zahlender Transaktion vollständig durchgehen
**Geltungsbereich**: Travelmate als nebenberuflich betriebenes SaaS-Side-Project mit Sitz in Deutschland
**Disclaimer**: Diese Checkliste ist keine Rechtsberatung. Bei Unsicherheit Steuerberater + Anwalt konsultieren. Insbesondere für AGB und DSGVO-Verarbeitungsverzeichnis lohnen die ~€200–300 für eine professionelle Erst-Prüfung.

---

## 1. Rechtsform-Entscheidung

Für €0–€22.000 Jahresumsatz ist **Einzelunternehmer mit Kleinunternehmer-Regelung** die richtige Wahl:

- **Keine Gründungskosten** (außer Gewerbeanmeldung ~€20)
- **Keine Mehrwertsteuer-Pflicht** (§19 UStG, solange < €22.000/Jahr)
- **Einfache Buchhaltung** (Einnahmen-Überschuss-Rechnung statt Bilanz)
- **Persönliche Haftung** — bei einem SaaS mit DB-Backups, Stripe-Trust und kleinem Risk-Footprint vertretbar

GmbH/UG erst sinnvoll bei MRR > €1.000 oder Kapitalbedarf für Hardware. Bis dahin: Einzelunternehmer.

---

## 2. Gewerbeanmeldung

**Wann?** Sobald die erste zahlende Transaktion absehbar ist (z. B. Stripe-Onboarding gestartet).

**Wie?**

1. Online-Formular der eigenen Stadt-Verwaltung (z. B. Bonn: `dienstleistungsportal.bonn.de`)
2. Tätigkeitsbeschreibung: „Entwicklung und Betrieb von Webanwendungen, insbesondere Software-as-a-Service zur Reise-Koordination"
3. Steuer-ID + Adresse + ggf. Personalausweis-Kopie
4. Gebühr: ca. €15–30 je nach Stadt

**Folgen automatisch** (innerhalb 4 Wochen):

- Steuernummer für Einzelunternehmer (Finanzamt schickt Fragebogen zur steuerlichen Erfassung)
- Mitgliedschaft IHK (Industrie- und Handelskammer) — Beitragsfrei bis ~€5.200 Jahresgewinn
- Berufsgenossenschaft (sofern keine Mitarbeiter, kann gewählt werden)

---

## 3. Kleinunternehmer-Status (§19 UStG)

**Definition**: Wer im vorangegangenen Jahr ≤ €22.000 und im laufenden Jahr ≤ €50.000 Umsatz erwartet, kann den Kleinunternehmer-Status wählen.

**Konsequenz**:

- **Keine Mehrwertsteuer auf Rechnungen** — Travelmate stellt €4.99 Brutto = €4.99 Netto in Rechnung, keine 19% MwSt
- **Kein Vorsteuerabzug** auf eigene Kosten (Hetzner-Rechnung €15 brutto, davon kann nichts vorgesteuert werden)
- **Hinweis auf Rechnung Pflicht**: „Gemäß §19 UStG wird keine Umsatzsteuer berechnet"

**Wann verlassen?** Sobald Jahresumsatz €22.000 überschreitet — dann auf Regelbesteuerung wechseln. In Travelmate's Lifestyle-Business-Pfad (MRR-Ziel €30/Monat = €360/Jahr) ist das fern.

---

## 4. Stripe-Onboarding

Erforderliche Daten bei Stripe-Account-Anlegung:

- Steuer-ID (vom Finanzamt nach Gewerbeanmeldung)
- Bankverbindung (Geschäftskonto empfohlen, siehe §6)
- Kopie Personalausweis (Stripe macht KYC)
- Geschäftsadresse
- Tätigkeitsbeschreibung (analog Gewerbeanmeldung)

**Stripe Tax** für DACH aktivieren — auch wenn als Kleinunternehmer keine MwSt anfällt, hilft das Setup automatisch beim späteren Wechsel zur Regelbesteuerung.

**Auszahlungs-Zyklus**: Standard 7 Tage rolling. Für DACH-EUR Einzahlungen bestens.

---

## 5. Impressum + DSGVO

### 5.1 Impressum (TMG § 5)

Erforderlich auf der Travelmate-Website (`travelmate-demo.de` und ggf. `travelmate.de` falls erworben). Inhalte:

- Vor- und Zuname (Thomas Klingler)
- Postanschrift (Geschäftssitz, kein Postfach)
- Kontakt: Email + Telefon (Telefon nicht zwingend, aber gängig)
- Steuernummer (oder Hinweis „beantragt", falls noch nicht zugeteilt)
- USt-IdNr. (nicht erforderlich für Kleinunternehmer)
- Berufsbezeichnung („Selbständiger Softwareentwickler") + zuständige Kammer (IHK)

**Generator**: e-recht24.de hat einen kostenfreien Impressum-Generator, der TMG-konform ist.

### 5.2 Datenschutzerklärung (DSGVO Art. 13)

Erforderlich. Inhalte:

- Verantwortlicher (Thomas Klingler, Adresse, Kontakt)
- Verarbeitungszwecke (Trip-Verwaltung, Mail-Versand, Analytics)
- Rechtsgrundlagen (Art. 6 Abs. 1 lit. b DSGVO — Vertragserfüllung; Art. 6 Abs. 1 lit. a — Einwilligung für Newsletter)
- Empfänger / Auftragsverarbeiter:
  - **Hetzner Online GmbH** (Hosting, AVV erforderlich, kostenlos via Hetzner Robot)
  - **Strato AG** (Mail, AVV erforderlich)
  - **Stripe Inc.** (Payment, US-Empfänger! → Standardvertragsklauseln und Bewertung erforderlich)
  - **Plausible Analytics** (EU-Hosted, kein AVV nötig wenn EU-Plan)
  - **Mailerlite** (US-Hosted! → Standardvertragsklauseln; Alternative: Buttondown EU oder eigene Mail-Liste)
- Speicherdauer
- Betroffenenrechte (Auskunft, Löschung, Datenübertragbarkeit, Widerspruch)
- Cookies + Tracking

**Generator**: e-recht24.de hat ebenfalls einen DSGVO-Generator. Output prüfen + ggf. anwaltlich kontrollieren.

### 5.3 Auftragsverarbeitungsverträge (AVV) abschließen

Pflicht für jeden externen Dienstleister, der personenbezogene Daten verarbeitet:

- ✅ Hetzner: AVV automatisch verfügbar via Hetzner Console
- ✅ Strato: AVV-Anforderung via Support-Ticket
- ✅ Stripe: AVV im Standard-Service-Agreement
- ✅ Plausible: AVV via Self-Service auf plausible.io
- ⚠️ Mailerlite: AVV verfügbar, US-Sub erfordert SCC

---

## 6. Geschäftskonto

**Pflicht?** Nein, technisch nicht — aber dringend empfohlen für saubere Buchhaltung.

**Optionen für Solopreneure**:

| Anbieter | Kosten | Vor-/Nachteile |
|---|---|---|
| **Holvi** | €0 (Lite) bis €11/Monat | Schnelle Eröffnung, integrierte Rechnungsstellung, Lite reicht für Phase 1 |
| **Kontist** | €0 bis €10/Monat | Mit Steuerberater-Integration, gut für Steueranfänger |
| **N26 Business Standard** | €0 | Einfaches Setup, keine Buchhaltungs-Tools |
| **Comdirect Geschäftskonto** | €5/Monat | Etablierte Bank, eher konservativ |

**Empfehlung**: Holvi Lite für die erste Phase. Bei MRR €100+ ggf. Wechsel zu Holvi Standard für Rechnungsstellung-Automatisierung.

---

## 7. AGB (Allgemeine Geschäftsbedingungen)

**Erforderlich?** Bei B2C technisch nicht zwingend, da das BGB die meisten Reibungspunkte regelt. Aber: hilft bei Streitigkeiten + signalisiert Professionalität.

**Inhalte**:

- Vertragsgegenstand (SaaS-Nutzung)
- Vertragsschluss (Online-Registrierung)
- Preise + Zahlung (Stripe, Pay-per-Trip vs. Subscription)
- Widerrufsrecht (für Subscriptions: 14 Tage; für digitale Inhalte verzichtbar wenn explizit angegeben)
- Verfügbarkeit (Best-Effort, kein 99.9% SLA)
- Haftungsausschluss
- Änderungen der AGB
- Anwendbares Recht (deutsches Recht)
- Gerichtsstand

**Generator**: e-recht24.de + AGB-Hosting kostenpflichtig (~€60/Jahr) ODER einmalige juristische Prüfung (~€200).

---

## 8. Markenrecherche „Travelmate"

**Wichtig vor öffentlichem Marketing-Push**:

1. **DPMA-Recherche** (Deutsches Patent- und Markenamt): https://register.dpma.de/DPMAregister/marke/einsteiger
   - Suche nach „Travelmate" in Klassen 9 (Software), 38 (Telekommunikation), 39 (Reisedienste), 42 (IT-Dienste)
   - Kosten: gratis
2. **EUIPO-Recherche** (EU-weit): https://euipo.europa.eu/eSearch
   - Klassen-Recherche für EU-Schutz prüfen
3. **Domain-Check**:
   - travelmate.de: aktuell?
   - travelmate.com: vermutlich vergeben
   - travelmate-demo.de: deine aktuelle Domain (bleibt, aber nicht final)

**Wenn Konflikt**: alternative Markennamen brainstormen (z. B. „Reisemate", „Tripcrew", „Pakora" — DDD-orthodoxer Begriff aus dem Domain-Glossar).

**Markenanmeldung selbst**: erst sinnvoll bei MRR > €100 (Anmeldegebühren ~€300 DE, ~€800 EU). Bis dahin Markenrecherche ausreichend.

---

## 9. Buchhaltung & Steuer

**Frequenz**:

- **Monatlich**: Kontoauszug + Stripe-Bericht + Belege sammeln (Hetzner-Rechnung, Strato-Rechnung, etc.)
- **Quartalsweise**: Einnahmen-Überschuss-Rechnung-Update (Excel oder Holvi-Tool)
- **Jährlich**: Steuererklärung (Anlage EÜR + Anlage S oder G) — DIY mit ELSTER oder Steuerberater (~€300/Jahr)

**Tooling**:

- **DIY**: Excel-Vorlage + ELSTER (kostenlos)
- **Tool-gestützt**: Holvi (integriert) oder sevdesk (~€8/Monat)
- **Steuerberater**: ~€300/Jahr für Kleinunternehmer-EÜR — empfohlen ab Jahr 2

**Aufbewahrungspflicht**: 10 Jahre für Belege, Rechnungen, Kontoauszüge.

---

## 10. Checkliste — Reihenfolge

Vor erster zahlender Transaktion abarbeiten, in dieser Reihenfolge:

1. ✅ DPMA-Markenrecherche „Travelmate" durchgeführt (~30 Min)
2. ✅ Geschäftskonto eröffnet (Holvi Lite empfohlen)
3. ✅ Gewerbeanmeldung online gestellt
4. ✅ Steuernummer erhalten (vom Finanzamt nach Gewerbeanmeldung)
5. ✅ Kleinunternehmer-Status gewählt (Fragebogen vom Finanzamt ankreuzen)
6. ✅ Stripe-Account angelegt + KYC durchlaufen
7. ✅ Impressum + DSGVO-Erklärung auf travelmate-demo.de live
8. ✅ AVVs mit Hetzner, Strato, Stripe, Plausible (ggf. Mailerlite) abgeschlossen
9. ✅ AGB live oder zumindest in Kurzform
10. ✅ Cookie-Banner falls Tracking aktiv (Plausible braucht keinen, weil cookieless)

---

## 11. Periodisches Re-Check (1× pro Jahr)

- Markenrecherche aktualisieren (neue Konflikte?)
- Datenschutzerklärung mit aktueller Tool-Liste abgleichen
- Umsatzgrenzen Kleinunternehmer-Regel prüfen (€22k/€50k)
- Belege geordnet ablegen für Steuererklärung

---

**Last updated**: 2026-04-30
**Next review trigger**: Nach Stripe-Account-Anlegung (Phase 2 der Business-Strategy)
