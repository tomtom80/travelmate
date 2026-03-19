# ADR-0017: OCR-Technologiewahl fuer Kassenzettel-Scan

## Status

Proposed (Entscheidung vor Implementierung von S10-B)

## Context

Story S10-B (Kassenzettel-Scan) fuehrt OCR-basierte Texterkennung auf fotografierten Kassenzetteln ein. Waehrend einer Gruppenreise kaufen Teilnehmer staendig ein (Supermarkt, Baeckerei, Restaurant, Liftkarte). Aktuell muessen Beschreibung, Betrag, Datum und Kategorie manuell im Expense-Formular eingetippt werden. Ein Foto des Kassenzettels soll diesen Prozess beschleunigen.

**Extraktionsziele (nach Prioritaet)**:

1. **Gesamtbetrag** (kritischstes Feld -- "SUMME", "TOTAL", "GESAMT", letzte grosse Zahl)
2. **Datum** (Regex: DD.MM.YYYY, DD.MM.YY)
3. **Geschaeftsname** (erste Zeile oder Header -- fuettert `CategoryGuesser` fuer automatische Kategorievorschlaege)
4. **Einzelposten** (nice-to-have fuer v1)

**Decision Drivers**:

1. Datenschutz: Kassenzettel-Fotos enthalten potenziell personenbezogene Daten (Zahlungsmittel, Kundennummern)
2. Betriebskosten: Travelmate ist ein Hobbyprojekt ohne laufendes Cloud-Budget
3. OCR-Qualitaet: Thermopapier-Bons (Supermarkt) sind besonders schwer zu erkennen (verblasster Druck, enge Zeilen)
4. Hexagonal Architecture: OCR-Technologie muss als austauschbarer Adapter hinter einem Domain-Port stecken
5. Docker-Kompatibilitaet: Jede Loesung muss in der bestehenden Docker-Compose-Infrastruktur lauffaehig sein

## Decision

### Port-Abstraktion im Domain-Layer

Unabhaengig von der konkreten OCR-Technologie wird ein `ReceiptScanPort` als sekundaerer Port im Domain-Layer definiert:

```
domain/expense/
  +-- ReceiptScanPort (Interface)
       +-- scanReceipt(imageBytes: byte[], contentType: String) -> ReceiptScanResult

  +-- ReceiptScanResult (Record)
       +-- success: boolean
       +-- totalAmount: BigDecimal (nullable)
       +-- date: LocalDate (nullable)
       +-- storeName: String (nullable)
       +-- suggestedCategory: ExpenseCategory (nullable)
       +-- lineItems: List<ScannedLineItem> (kann leer sein)
       +-- confidence: double (0.0 - 1.0)
       +-- rawText: String (nullable, fuer Debug/Transparenz)
       +-- errorMessage: String (nullable)
```

Diese Abstraktion macht die OCR-Technologie austauschbar, ohne den Domain-Layer oder die Application Services zu aendern.

### Empfohlene Implementierung: Tesseract (self-hosted) als Default, Cloud Vision API als spaetere Option

**Fuer die Erstimplementierung in Iteration 10 wird Tesseract via tess4j empfohlen.** Die Port-Abstraktion ermoeglicht spaeteres Wechseln auf Cloud Vision API ohne Domain-Aenderung.

#### Tesseract-Adapter (v1)

```
adapters/ocr/
  +-- TesseractReceiptScanAdapter (implementiert ReceiptScanPort)
       +-- tess4j (net.sourceforge.tess4j:tess4j:5.13.0)
       +-- Sprachen: deu + eng
       +-- @Profile("!test")
```

#### Kategorie-Erkennung via CategoryGuesser (Domain Service)

```
domain/expense/
  +-- CategoryGuesser
       +-- guess(storeName: String) -> ExpenseCategory

  Mapping-Regeln:
    EDEKA, REWE, ALDI, LIDL, SPAR, Penny, Netto    -> GROCERIES
    Baeckerei, Brot, Backhaus                         -> GROCERIES
    Restaurant, Gasthaus, Pizzeria, Trattoria         -> RESTAURANT
    Shell, Aral, Total, OMV, Tankstelle               -> FUEL
    Apotheke, Drogerie, DM, Mueller                   -> HEALTH
    Bergbahn, Seilbahn, Skilift, Therme, Museum       -> ACTIVITY
    DB, Bahn, Bus, Taxi, Uber, Maut                   -> TRANSPORT
    Default                                           -> OTHER
```

#### Docker-Erweiterung

Das Expense-SCS-Dockerfile muss um Tesseract erweitert werden:

```dockerfile
RUN apt-get update && apt-get install -y tesseract-ocr tesseract-ocr-deu && rm -rf /var/lib/apt/lists/*
```

Geschaetzter Image-Groessenzuwachs: ~50 MB (Tesseract + deu-Sprachdaten).

## Consequences

### Positiv

- **Datenschutz (DSGVO)**: Alle Bilddaten bleiben lokal auf dem Server -- keine Uebermittlung an Cloud-Dienste
- **Keine laufenden Kosten**: Tesseract ist Open Source, keine API-Gebuehren
- **Austauschbarkeit**: `ReceiptScanPort`-Abstraktion ermoeglicht spaeteres Upgrade auf Cloud Vision API als Premium-Option
- **Offline-faehig**: OCR funktioniert ohne Internetzugang des Servers (nur Bild-Upload durch Nutzer erfordert Netz)
- **Framework-freier Domain-Layer**: Port-Interface und Result-Records haben keine Tesseract- oder Spring-Abhaengigkeiten

### Negativ

- **Geringere OCR-Qualitaet**: Tesseract erreicht bei Thermopapier-Bons erfahrungsgemaess ~70% Trefferquote fuer den Gesamtbetrag (Cloud Vision API: ~95%)
- **Native Abhaengigkeit**: Tesseract-Bibliothek + Sprachdaten muessen im Docker-Image installiert sein; lokale Entwicklung erfordert Tesseract-Installation
- **Docker-Image waechst**: ~50 MB Groessenzuwachs durch Tesseract + tessdata
- **Kein Machine-Learning-Vorteil**: Tesseract hat keinen "Document AI"-Modus fuer strukturierte Extraktion (Belege, Rechnungen); Betrag-Erkennung basiert auf Regex-Heuristik

## Alternatives

### Option 1: Google Cloud Vision API

- **Vorteile**: Deutlich bessere OCR-Qualitaet (~95% fuer Gesamtbetrag), Document AI fuer strukturierte Belegerkennung, einfache REST-API-Integration, Free Tier (1.000 Requests/Monat)
- **Nachteile**: Cloud-Abhaengigkeit, DSGVO-Bedenken (Kassenzetteldaten an Google), laufende Kosten bei Nutzung ueber Free Tier, externe API-Verfuegbarkeit beeinflusst Feature-Verfuegbarkeit

### Option 2: Nur Tesseract (ohne Port-Abstraktion)

- **Vorteile**: Einfachere Implementierung, weniger Indirektion
- **Nachteile**: Spaeterer Wechsel auf Cloud-OCR erfordert Refactoring in Application Services; verletzt das Hexagonale Architektur-Prinzip (ADR-0008)

### Option 3: Hybrid (Tesseract Default + Cloud Vision als Konfigurationsoption)

- **Vorteile**: Nutzer kann zwischen self-hosted und Cloud wechseln; best-of-both-worlds
- **Nachteile**: Erhoehter Implementierungs- und Testaufwand (zwei Adapter von Anfang an); ueberdimensioniert fuer aktuelle Nutzerzahl
- **Bewertung**: Sinnvolle langfristige Option, aber fuer Iteration 10 genuegt Tesseract allein -- der zweite Adapter kann spaeter hinzugefuegt werden, da das Port-Interface stabil bleibt

## Invarianten

```
INV-OCR1: Bild darf max 10 MB gross sein
INV-OCR2: Nur JPEG, PNG, HEIC als Formate akzeptiert (Magic-Byte-Pruefung)
INV-OCR3: OCR-Ergebnis ist immer ein VORSCHLAG -- Nutzer muss bestaetigen koennen
INV-OCR4: Gesamtbetrag ist das primaere Extraktionsziel
INV-OCR5: Wenn OCR fehlschlaegt, faellt der Nutzer auf das manuelle Formular zurueck (kein Blocker)
INV-OCR6: Foto wird NICHT persistiert -- nur transient verarbeitet (Datenschutz)
INV-OCR7: ReceiptScanPort ist framework-frei (kein Spring, kein tess4j im Interface)
```

## Related

- ADR-0008: DDD + Hexagonale Architektur (Port/Adapter-Muster)
- ADR-0014: Expense Domain Design (Receipt als Entity innerhalb Expense Aggregate)
- ADR-0016: Import-Pipeline-Pattern und SSRF-Schutz (gemeinsames Import-UX-Muster)
- EventStorming Iteration 10, Abschnitt 3.3: `docs/design/eventstorming/iteration-10-scope.md`
- Tesseract OCR: https://github.com/tesseract-ocr/tesseract
- tess4j (Java-Wrapper): https://github.com/nguyenq/tess4j
- Google Cloud Vision API: https://cloud.google.com/vision/docs/ocr
