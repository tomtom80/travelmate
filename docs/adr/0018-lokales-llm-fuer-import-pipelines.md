# ADR-0018: Lokales LLM (Ollama) fuer Import-Pipelines statt regelbasierter Extraktion

## Status

Vorgeschlagen

## Kontext

Die in ADR-0016 (Import-Pipeline-Pattern) und ADR-0017 (OCR-Technologiewahl) beschlossenen Ansaetze basieren auf **regelbasierter Extraktion**: Jsoup + JSON-LD/OpenGraph-Heuristiken fuer den Accommodation URL Import, Tesseract + Regex-Heuristik fuer den Kassenzettel-Scan. Beide Ansaetze haben fundamentale Schwaechen:

### Problem 1: Accommodation URL Import (Trips SCS)

Die aktuelle Strategie (JSON-LD -> Open Graph -> HTML-Heuristik) scheitert an der Heterogenitaet der Ferienhausportale:

| Portal | JSON-LD | Open Graph | Zimmerstruktur im HTML |
|--------|---------|------------|----------------------|
| booking.com | Ja (LodgingBusiness) | Ja | Proprietaeres JS-Rendering |
| Airbnb | Nein (SPA) | Teilweise | Client-seitig gerendert |
| Ferienhausmiete.de | Nein | Teilweise | Individuelles HTML |
| huetten.com | Nein | Minimal | Freitext-Beschreibungen |
| FeWo-direkt | Teilweise | Ja | Tabellen + Freitext gemischt |

Fuer jedes neue Portal muessten site-spezifische Parser geschrieben und gewartet werden. Bei Aenderungen am HTML brechen die Extraktoren ohne Vorwarnung. Die Zimmerstruktur (Zimmertypen, Bettenanzahl, Ausstattung) ist das wertvollste Extraktionsziel, aber am schwersten per Regex zu extrahieren.

### Problem 2: Kassenzettel-Scan (Expense SCS)

Tesseract erreicht bei Thermopapier-Kassenzetteln erfahrungsgemaess ~70% Trefferquote fuer den Gesamtbetrag (ADR-0017). Die Regex-Heuristik (`GermanReceiptParser`) erkennt bekannte Muster (SUMME, TOTAL, GESAMT), scheitert aber an:

- Unueblichen Formaten (handschriftliche Quittungen, auslaendische Belege)
- Unscharfen Fotos (Bewegungsunschaerfe, schlechte Beleuchtung)
- Thermopapier-Verblassung (aeltere Bons)
- Mehrzeiligen Betraegen und Zwischensummen

### Gemeinsame Ursache

Beide Probleme haben dieselbe Wurzel: **Regelbasierte Extraktion ist fragil gegenueber Formatvarianz.** Ein Vision-faehiges LLM kann beliebige Layouts verstehen, ohne dass site-spezifische oder formatspezifische Regeln gepflegt werden muessen.

### Decision Drivers

1. **Ein Modell fuer beide Use Cases**: Accommodation (Text/HTML -> strukturierte Daten) und Receipt (Bild -> strukturierte Daten) sollen vom gleichen Modell bedient werden
2. **Datenschutz**: Kassenzetteldaten und Reisedaten duerfen nicht an Cloud-Dienste gesendet werden (DSGVO, Hobbyprojekt ohne DPA)
3. **Kosten**: Keine laufenden API-Gebuehren -- Travelmate ist ein Hobbyprojekt
4. **Bestehende Ports**: `AccommodationImportPort` und `ReceiptScanPort` sind bereits als hexagonale Ports definiert -- die Adapter-Implementierung ist austauschbar
5. **Docker-Compose-Kompatibilitaet**: Muss als Sidecar neben den bestehenden Services laufen
6. **Apple Silicon / Consumer-Hardware**: Muss auf typischer Entwickler-Hardware (16 GB RAM, M1/M2/M3) und auf VPS (8-16 GB RAM, keine GPU) laufen

## Entscheidung

### 1. Ollama als LLM-Runtime mit Qwen3-VL 8B als Modell

**Empfohlenes Modell: `qwen3-vl:8b`** (Qwen3-VL-7B-Instruct, Q4_K_M Quantisierung)

Dieses Modell deckt beide Use Cases mit einem einzigen Deployment ab:

| Eigenschaft | Wert |
|-------------|------|
| Parameter | 8.77B (quantisiert ~6.1 GB) |
| Vision | Ja (Bilder + Text) |
| Deutsch | Ja (multilinguale Trainigsdaten) |
| Kontext | 256K Tokens (erweiterbar auf 1M) |
| DocVQA Benchmark | >95% (Nachfolger von Qwen2.5-VL mit 95.7%) |
| OCRBench | >86% (32-Sprachen-OCR) |
| Strukturierte Ausgabe | JSON-Mode via Ollama `format: json` |
| Lizenz | Apache 2.0 |
| Ollama-Verfuegbarkeit | `ollama pull qwen3-vl:8b` |

**Warum Qwen3-VL 8B?**

- **Bestes Preis-Leistungs-Verhaeltnis**: Uebertrifft in Document-Understanding-Benchmarks groessere Modelle (Llama 3.2 11B Vision) bei geringerem Speicherbedarf
- **Vision + Text in einem Modell**: Kann sowohl Bilder analysieren (Receipt-Scan) als auch langen Text/HTML verstehen (Accommodation-Import)
- **Strukturierte JSON-Ausgabe**: Ollama unterstuetzt `format: json` mit JSON-Schema-Constraint -- ideal fuer typisierte Extraktion
- **Deutsche Sprachunterstuetzung**: Wichtig fuer Kassenzettel-Parsing (SUMME, MwSt, etc.)

**Upgrade-Pfad**: Da Ollama-Modelle per Konfiguration austauschbar sind (`travelmate.llm.model`), kann bei Erscheinen leistungsfaehigerer Nachfolger (z.B. Qwen4-VL) ohne Code-Aenderung gewechselt werden.

### 2. Deployment: Ollama als Docker-Compose-Sidecar

```yaml
# docker-compose.yml (Ergaenzung)
services:
  ollama:
    image: ollama/ollama:latest
    container_name: travelmate-ollama
    ports:
      - "11434:11434"
    volumes:
      - ollama-data:/root/.ollama
    deploy:
      resources:
        limits:
          memory: 8G
    healthcheck:
      test: ["CMD", "ollama", "list"]
      interval: 30s
      timeout: 10s
      retries: 3
    profiles:
      - llm

volumes:
  ollama-data:
```

Das `profiles: [llm]` stellt sicher, dass Ollama nur bei explizitem `docker compose --profile llm up` gestartet wird. Fuer Entwickler ohne LLM-Bedarf aendert sich nichts.

**Modell-Pull beim ersten Start:**

```bash
docker compose --profile llm up -d ollama
docker exec travelmate-ollama ollama pull qwen3-vl:8b
```

Alternativ kann ein Init-Container oder ein Startup-Script das Modell automatisch laden.

### 3. Architektur-Integration: Ein shared LlmPort, zwei spezialisierte Adapter

Die bestehenden Domain-Ports (`AccommodationImportPort`, `ReceiptScanPort`) bleiben **unveraendert**. Die Adapter-Implementierungen wechseln von regelbasiert auf LLM-gestuetzt:

```
travelmate-trips/
  adapters/integration/
    +-- OllamaAccommodationImportAdapter    (NEU, implementiert AccommodationImportPort)
    +-- WebScrapingAccommodationImportAdapter (BESTEHEND, Fallback)

travelmate-expense/
  adapters/integration/
    +-- OllamaReceiptScanAdapter            (NEU, implementiert ReceiptScanPort)
    +-- TesseractReceiptScanAdapter          (BESTEHEND, Fallback)
```

**Kein gemeinsamer LlmPort im Domain-Layer.** Die Domain kennt kein LLM -- sie kennt nur `AccommodationImportResult` und `ReceiptScanResult`. Die Ollama-HTTP-Kommunikation ist ein Infrastruktur-Detail der Adapter.

Allerdings wird eine **gemeinsame Ollama-Client-Bibliothek** im `travelmate-common` oder als Utility-Klasse in jedem SCS bereitgestellt:

```
OllamaClient (Utility, kein Domain-Konzept)
  +-- generateJson(prompt: String, imageBase64: String?) -> String
  +-- Konfiguration: travelmate.llm.base-url, travelmate.llm.model
```

**Wichtig**: `OllamaClient` gehoert NICHT in den Domain-Layer und NICHT in `travelmate-common` (das waere eine Framework-Abhaengigkeit im Shared Kernel). Er lebt als Infrastruktur-Utility in den jeweiligen Adapter-Packages oder als separates Maven-Modul (falls Wiederverwendung gewuenscht).

### 4. Prompt-Strategie

#### Accommodation URL Import

```
Du bist ein Datenextraktions-Assistent. Extrahiere aus dem folgenden HTML-Text
einer Ferienhausseite diese Informationen als JSON:

{
  "name": "Name der Unterkunft",
  "address": "Vollstaendige Adresse",
  "checkIn": "YYYY-MM-DD oder null",
  "checkOut": "YYYY-MM-DD oder null",
  "totalPrice": 1234.56 oder null,
  "maxGuests": 8 oder null,
  "rooms": [
    {"name": "Schlafzimmer 1", "bedCount": 2, "bedType": "Doppelbett"}
  ],
  "notes": "Wichtige Zusatzinfos (Haustiere, Parkplatz, etc.)"
}

Antworte NUR mit dem JSON-Objekt, keine Erklaerungen.

--- HTML START ---
{bereinigter HTML-Text, max 4000 Tokens}
--- HTML END ---
```

Der HTML-Text wird vor dem Senden bereinigt: `<script>`, `<style>`, Navigation, Footer entfernen. Jsoup wird weiterhin fuer das Fetching und HTML-Cleaning genutzt (SSRF-Schutz aus ADR-0016 bleibt vollstaendig erhalten).

#### Kassenzettel-Scan

```
Du bist ein OCR-Assistent fuer deutsche Kassenzettel. Analysiere das Foto
und extrahiere diese Informationen als JSON:

{
  "totalAmount": 23.45,
  "date": "YYYY-MM-DD oder null",
  "storeName": "Name des Geschaefts",
  "lineItems": [
    {"name": "Artikelname", "price": 1.99}
  ]
}

Antworte NUR mit dem JSON-Objekt. Wenn ein Feld nicht erkennbar ist, setze null.
```

Das Bild wird als Base64 im Multimodal-Request an Ollama gesendet (`/api/generate` mit `images`-Array).

### 5. Fallback-Strategie (Graceful Degradation)

Die LLM-Adapter sind **nicht die einzige Implementierung**, sondern die bevorzugte. Bei Nichtverfuegbarkeit greift ein Fallback:

```
Adapter-Auswahllogik (im Application Service oder via @Primary/@ConditionalOnProperty):

1. Ist Ollama erreichbar? (Health-Check: GET /api/tags)
   JA -> OllamaAccommodationImportAdapter / OllamaReceiptScanAdapter
   NEIN -> Fallback

2. Fallback Accommodation: WebScrapingAccommodationImportAdapter (Jsoup + JSON-LD)
3. Fallback Receipt: TesseractReceiptScanAdapter (oder ReceiptScanResult.empty())
```

**Konfigurationssteuerung:**

```yaml
travelmate:
  llm:
    enabled: true                          # Master-Schalter
    base-url: http://localhost:11434       # Ollama REST API
    model: qwen3-vl:8b                   # Modellname
    timeout: 120s                          # Grosszuegig fuer CPU-Inferenz
    max-image-size: 10485760               # 10 MB (wie ADR-0017)
```

`@ConditionalOnProperty(name = "travelmate.llm.enabled", havingValue = "true")` auf den Ollama-Adaptern. Bei `false` werden automatisch die regelbasierten Adapter verwendet.

### 6. Latenz und UX

| Szenario | Erwartete Latenz | Bewertung |
|----------|-----------------|-----------|
| Ollama auf Apple M2/M3 (16 GB, unified memory) | 5-15s (7B Q4) | Gut -- Loading-State genuegt |
| Ollama auf VPS mit GPU (z.B. T4, 16 GB VRAM) | 2-5s | Sehr gut |
| Ollama auf VPS ohne GPU (CPU-only, 8 GB RAM) | 30-90s | Grenzwertig -- aber Import ist einmalig pro Trip |
| Cloud API (GPT-4V, Claude Vision) | 1-3s | Beste Latenz, aber Kosten + Datenschutz |

Die Import-Pipeline hat bereits einen HTMX-Loading-State (ADR-0016). Fuer CPU-Inferenz kann ein Progress-Indikator mit `hx-indicator` und einer informativen Meldung ("KI analysiert die Seite...") die Wartezeit ueberbruecken. Da der Import ein einmaliger Vorgang pro Accommodation/Receipt ist (nicht wiederkehrend), sind selbst 30-60s akzeptabel.

## Konsequenzen

### Positiv

- **Ein Modell, zwei Use Cases**: Qwen3-VL 8B deckt sowohl Text-Extraktion (Accommodation HTML) als auch Bild-Verstehen (Kassenzettel-Fotos) ab -- kein Betrieb von zwei unterschiedlichen Systemen (Jsoup-Parser + Tesseract)
- **Format-agnostisch**: Das LLM versteht beliebige HTML-Strukturen und Bon-Layouts, ohne site-spezifische oder formatspezifische Regeln. Neue Portale oder Bonformate erfordern keine Code-Aenderungen
- **Datenschutz (DSGVO)**: Alle Daten bleiben lokal -- keine Uebermittlung an Cloud-Dienste (Google, OpenAI, Anthropic). Kein Auftragsverarbeitungsvertrag (AV) erforderlich
- **Keine laufenden Kosten**: Ollama + Qwen3-VL sind vollstaendig Open Source (Apache 2.0). Keine API-Gebuehren, kein Free-Tier-Limit
- **Hexagonal-konform**: Bestehende Ports (`AccommodationImportPort`, `ReceiptScanPort`) bleiben unveraendert. Nur die Adapter-Implementierungen wechseln. Domain-Layer bleibt framework- und LLM-frei
- **Graceful Degradation**: Bei Nichtverfuegbarkeit von Ollama fallen beide Features automatisch auf die bisherigen regelbasierten Adapter zurueck
- **Zukunftssicher**: Modell ist per Konfiguration austauschbar (`travelmate.llm.model`). Upgrade auf Qwen3-VL oder andere Modelle ohne Code-Aenderung
- **Wartungsarmer Betrieb**: Eliminiert den Wartungsaufwand fuer site-spezifische HTML-Parser und Regex-Heuristiken

### Negativ

- **Hoehere Hardwareanforderungen**: 7B-Modell benoetigt ~5 GB RAM (quantisiert). Auf Systemen mit <8 GB RAM nicht einsetzbar. Entwickler ohne ausreichend RAM nutzen den Fallback
- **Nicht-deterministische Ausgabe**: LLM-Antworten koennen bei identischem Input variieren. JSON-Parsing muss robust sein (Try-Catch, Validierung). Temperature=0 reduziert Varianz, eliminiert sie aber nicht
- **Neue Infrastruktur-Abhaengigkeit**: Ollama als zusaetzlicher Docker-Service. Erhoehte Komplexitaet im Docker-Compose-Setup (aber via `profiles: [llm]` optional)
- **CPU-Latenz**: Auf Servern ohne GPU kann die Inferenz 30-90s dauern. Fuer die Import-Pipeline akzeptabel, aber nicht fuer Echtzeit-Interaktionen
- **Kein Offline-Support fuer Accommodation**: Der URL-Fetch benoetigt weiterhin Internetzugang (unveraenderlich). Nur der Receipt-Scan profitiert von Offline-Faehigkeit
- **Tesseract-Abhaengigkeit entfaellt nicht sofort**: Tesseract bleibt als Fallback im Docker-Image, bis die LLM-Loesung ausreichend validiert ist. Docker-Image waechst kurzzeitig (Tesseract + Ollama-Sidecar)

## Alternativen

### Option 1: Status quo (Jsoup + Tesseract, regelbasiert)

- **Vorteile**: Keine neue Infrastruktur, deterministisch, minimale Latenz, geringste Komplexitaet
- **Nachteile**: Fragile site-spezifische Parser, ~70% OCR-Genauigkeit auf Thermopapier, hoher Wartungsaufwand bei neuen Portalen, keine Generalisierung auf unbekannte Formate
- **Bewertung**: Fuer Prototyp ausreichend, skaliert aber nicht mit der Anzahl unterstuetzter Portale

### Option 2: Cloud Vision API (Google Cloud Vision / GPT-4 Vision / Claude Vision)

- **Vorteile**: Beste Genauigkeit (~95% OCR), schnellste Latenz (1-3s), kein lokaler Ressourcenverbrauch, staendige Modellverbesserungen durch Anbieter
- **Nachteile**: DSGVO-Bedenken (Kassenzetteldaten an US-Cloud), laufende Kosten ($0.01-0.03 pro Request, Cloud Vision API $1.50/1000 nach Free Tier), externe Abhaengigkeit fuer Feature-Verfuegbarkeit, Auftragsverarbeitungsvertrag erforderlich
- **Bewertung**: Beste technische Loesung, aber unvereinbar mit Datenschutz-Anspruch und Kostenfreiheit eines Hobbyprojekts

### Option 3: Kleineres Modell (Phi-3 Vision 4.2B / Moondream 2B)

- **Vorteile**: Geringerer Speicherbedarf (~2-3 GB), schnellere Inferenz auf CPU
- **Nachteile**: Deutlich schlechtere Extraktionsqualitaet, schwaecher bei Deutsch, weniger robust bei komplexem HTML. Moondream 2B hat keine zuverlaessige JSON-Strukturierung
- **Bewertung**: Fuer Ultra-Low-Resource-Szenarien denkbar, aber die Qualitaetseinbussen ueberwiegen den Speichervorteil. Qwen3-VL 8B laeuft bereits komfortabel auf 8 GB-Systemen (Q4-Quantisierung)

### Option 4: Separates Text-Modell + Vision-Modell (z.B. Llama 3.1 8B fuer HTML + LLaVA fuer Bilder)

- **Vorteile**: Jeweils spezialisiertes Modell, potenziell bessere Ergebnisse pro Task
- **Nachteile**: Zwei Modelle im Speicher (~10 GB total), doppelte Konfiguration und Wartung, keine Synergieeffekte. Qwen3-VL 8B erreicht bereits State-of-the-Art in beiden Benchmarks (DocVQA 95.7%)
- **Bewertung**: Unnoetige Komplexitaet, da ein einzelnes Vision-Modell beide Tasks abdeckt

### Option 5: Spring AI Integration statt direkter Ollama REST API

- **Vorteile**: Abstrahiert LLM-Provider (Ollama, OpenAI, Claude) hinter einheitlichem API. Spaeterer Wechsel auf Cloud-Provider ohne Adapter-Aenderung
- **Nachteile**: Spring AI befindet sich in Milestone-Phase (2.0.0-M2, Stand Maerz 2026). Zusaetzliche Maven-Abhaengigkeit (`spring-ai-starter-model-ollama`). Unklar, ob Vision-Support fuer Ollama stabil ist. Die Port-Abstraktion im Domain-Layer bietet bereits Provider-Austauschbarkeit auf DDD-Ebene
- **Bewertung**: Langfristig attraktiv, aber fuer die Erstimplementierung ist ein schlanker REST-Client (`java.net.http.HttpClient`) ausreichend. Spring AI kann spaeter als Adapter-Implementierung hinzugefuegt werden, ohne den Domain-Layer zu beruehren

## Kostenvergleich

| Loesung | Setup-Kosten | Laufende Kosten (100 Imports/Monat) | OCR-Genauigkeit | Latenz |
|---------|-------------|-------------------------------------|-----------------|--------|
| Tesseract (lokal) | 0 EUR | 0 EUR | ~70% | <1s |
| Ollama + Qwen3-VL 8B (lokal) | 0 EUR (Open Source) | 0 EUR (Strom) | ~90-95% (geschaetzt) | 5-60s |
| Google Cloud Vision API | 0 EUR | 0 EUR (Free Tier) / ~0.15 EUR | ~95% | 1-3s |
| GPT-4 Vision (OpenAI) | 0 EUR | ~1-3 EUR | ~95% | 1-3s |
| Claude Vision (Anthropic) | 0 EUR | ~1-3 EUR | ~95% | 1-3s |

Bei 100 Imports/Monat sind Cloud-APIs guenstig. Der entscheidende Faktor ist nicht der Preis, sondern **Datenschutz** (keine DSGVO-Problematik) und **Unabhaengigkeit** (kein API-Key, kein Rate-Limit, kein Vendor-Lock-in).

## Invarianten

```
INV-LLM1: Domain-Ports (AccommodationImportPort, ReceiptScanPort) haben KEINE LLM-Abhaengigkeiten
INV-LLM2: Ollama ist OPTIONAL -- bei travelmate.llm.enabled=false funktionieren beide Features via Fallback
INV-LLM3: JSON-Antworten des LLM werden IMMER validiert und defensiv geparst (Try-Catch + Defaults)
INV-LLM4: SSRF-Schutz (ADR-0016) bleibt vollstaendig erhalten -- das LLM ersetzt nur die Extraktion, nicht das Fetching
INV-LLM5: Bilder werden NICHT an externe Dienste gesendet -- nur an den lokalen Ollama-Container
INV-LLM6: LLM-Ergebnisse sind IMMER Vorschlaege -- der Nutzer kann im editierbaren Formular korrigieren
INV-LLM7: OllamaClient lebt NICHT im Domain-Layer und NICHT in travelmate-common
INV-LLM8: Docker-Profil "llm" -- Ollama startet nur bei expliziter Aktivierung
```

## Implementierungsreihenfolge

1. **Phase 1**: OllamaClient-Utility + OllamaReceiptScanAdapter (Expense SCS) -- Kassenzettel-Scan profitiert am staerksten vom LLM-Upgrade (70% -> ~95% Genauigkeit)
2. **Phase 2**: OllamaAccommodationImportAdapter (Trips SCS) -- Ersetzt fragile HTML-Heuristiken durch LLM-Extraktion
3. **Phase 3**: Tesseract-Abhaengigkeit aus Docker-Image entfernen, wenn LLM-Adapter stabil validiert ist
4. **Phase 4 (optional)**: Spring AI als Adapter-Implementierung evaluieren, wenn Spring AI 2.0 GA erreicht

## Related

- ADR-0008: DDD + Hexagonale Architektur (Port/Adapter-Muster)
- ADR-0016: Import-Pipeline-Pattern und SSRF-Schutz (gemeinsames Import-UX-Muster, SSRF-Schutz bleibt)
- ADR-0017: OCR-Technologiewahl fuer Kassenzettel-Scan (wird durch diesen ADR ergaenzt, nicht ersetzt)
- Qwen3-VL: https://huggingface.co/Qwen/Qwen3-VL-7B-Instruct
- Qwen3-VL (Nachfolger): https://github.com/QwenLM/Qwen3-VL
- Ollama: https://ollama.com/
- Ollama Structured Outputs: https://ollama.com/blog/structured-outputs
- Ollama Vision Models: https://ollama.com/search?c=vision
- Spring AI Ollama: https://docs.spring.io/spring-ai/reference/api/chat/ollama-chat.html
