# Demo-Hosting-Empfehlung fuer Travelmate (Stand 2026-03-26)

## Ziel

Diese Notiz beschreibt die guenstigste und zugleich brauchbare Hosting-Variante fuer eine fruehe Demo-Umgebung von Travelmate, die mit einigen Freunden getestet werden kann.

## Annahmen fuer die Demo

Die Demo-Umgebung soll:

- moeglichst guenstig sein
- schnell aufsetzbar sein
- fuer wenige gleichzeitige Nutzer brauchbar laufen
- kein hohes Betriebsniveau oder SLA erfordern
- notfalls noch auf Docker Compose basieren duerfen

Fuer diese Zielsetzung ist ein kompletter Kubernetes-Stack in der Regel nicht die beste Anfangsloesung. Der Mehrwert steht am Anfang meist nicht im Verhaeltnis zu den Mehrkosten und zum Betriebsaufwand.

## Travelmate-spezifische Randbedingungen

Auch fuer eine Demo benoetigt Travelmate mehrere Laufzeitkomponenten:

- Gateway
- IAM
- Trips
- Expense
- Keycloak
- RabbitMQ
- PostgreSQL

Ollama ist optional und sollte fuer eine guenstige Demo deaktiviert bleiben.

## Geeignete Optionen

| Option | Betriebsmodell | Grobe Kosten | Eignung | Urteil |
|---|---|---:|---|---|
| Oracle Cloud Always Free + Docker Compose | Single VM | 0 USD/Monat | Sehr guenstig, aber Free-Tier-Risiko | Billigste Demo-Option |
| Hetzner Cloud CAX11/CAX21 + Docker Compose | Single VM | ab 4.49 EUR bzw. 7.99 EUR/Monat ab 2026-04-01, zzgl. VAT | Einfach, guenstig, gut kontrollierbar | Beste Demo-Empfehlung |
| DigitalOcean Basic Droplet + Docker Compose | Single VM | ab 4 USD/Monat | Einfach, aber fuer Travelmate weniger Preis-Leistung als Hetzner | Gute Alternative |
| Railway | PaaS | ab 1 USD/Monat plus Nutzung oder 5 USD Hobby-Minimum | Einfach fuer kleine Apps, aber unguenstig fuer Travelmate-Mehrservice-Stack | Nur Test-/Kurzzeitoption |
| Render Free | PaaS | 0 USD fuer Preview-artige Nutzung | Starke Free-Limits, Spin-Down, keine echte Passung fuer den Gesamtstack | Nicht empfohlen fuer brauchbare Demo |

## Bewertung der Optionen

### 1. Oracle Cloud Always Free + Docker Compose

Vorteile:

- nominell kostenlos
- genug ARM-Ressourcen fuer eine kleine Demo
- Compose passt gut zum existierenden Repository-Setup

Nachteile:

- Free-Tier-Kapazitaet und Verfuegbarkeit sind unberechenbarer
- ARM kann in Randfaellen Images oder Tooling beeinflussen
- weniger gut fuer eine Demo, auf die ihr euch an einem konkreten Abend verlassen wollt

Empfehlung:

Gut fuer interne Tests und kostengetriebene Experimente. Nicht die erste Wahl, wenn die Demo fuer andere Personen verlaesslich erreichbar sein soll.

### 2. Hetzner Cloud CAX11 oder CAX21 + Docker Compose

Vorteile:

- sehr guenstig
- einfacher Betriebsweg
- gute europaeische Latenz
- ihr koennt den existierenden Compose-Stack fast direkt uebernehmen

Nachteile:

- weiterhin alles auf einer VM
- kein echtes Plattform-Feeling wie bei Managed-PaaS oder Kubernetes

Empfehlung:

Das ist fuer Travelmate die beste fruehe Demo-Loesung. Fuer einen kleinen Freundeskreis ist eine einzelne VM mit Docker Compose die niedrigste brauchbare Komplexitaet.

### 3. DigitalOcean Basic Droplet + Docker Compose

Vorteile:

- sehr einfache Bedienung
- guter Marktstandard fuer kleine VMs

Nachteile:

- fuer Travelmate preislich meist schlechter als Hetzner
- kleine Plangroessen sind schnell knapp, wenn mehrere Java-Services plus Keycloak parallel laufen

Empfehlung:

Akzeptable Alternative, wenn ihr DigitalOcean bereits nutzt oder eine besonders einfache UI wollt.

### 4. Railway

Vorteile:

- sehr einfacher Einstieg
- git-zentriertes Deployment

Nachteile:

- Travelmate besteht nicht aus einem simplen 1-Service-Setup
- Nutzungskosten steigen mit mehreren dauerhaft laufenden Services schnell
- Volumes und mehrere interne Dienste machen die Plattform fuer diesen Fall unattraktiv

Empfehlung:

Fuer Travelmate eher nur als kurzfristige Produktdemo einzelner Services geeignet, nicht als beste Gesamtdemo-Umgebung.

### 5. Render Free

Vorteile:

- kostenlos fuer sehr kleine Vorschau-Setups

Nachteile:

- Free Web Services spinnen nach 15 Minuten Idle herunter
- Free Postgres laeuft nur als eingeschraenkte Preview-/Hobby-Option
- mehrere Travelmate-Komponenten passen schlecht auf die Free-Modellgrenzen

Empfehlung:

Fuer Travelmate keine brauchbare Gesamtloesung, wenn mehrere Freunde die Anwendung halbwegs normal ausprobieren sollen.

## Konkrete Empfehlung

### Empfehlung A: billigste Demo-Umgebung ueberhaupt

**Oracle Cloud Always Free + eine VM + Docker Compose**

Nur empfehlen, wenn:

- Kosten absolut wichtiger sind als Zuverlaessigkeit
- gelegentliche Free-Tier-Probleme akzeptabel sind
- die Demo eher intern oder locker geplant ist

### Empfehlung B: beste fruehe Demo-Umgebung

**Hetzner Cloud CAX21 + Docker Compose**

Warum gerade diese Variante:

- sehr guenstige laufende Kosten
- einfachster Weg von eurem jetzigen Setup zur oeffentlich erreichbaren Demo
- ausreichend brauchbar fuer einen kleinen Freundeskreis
- keine fruehe Kubernetes-Komplexitaet

### Empfehlung C: noch guenstigerer Hetzner-Einstieg

**Hetzner Cloud CAX11 + Docker Compose**

Nur sinnvoll, wenn:

- wirklich nur sehr wenig parallele Nutzung erwartet wird
- `LLM_ENABLED=false` gesetzt ist
- JVM-Memory bewusst begrenzt wird
- ihr Performance-Risiken akzeptiert

Pragmatisch ist fuer Travelmate eher `CAX21` die bessere Startgroesse.

## Empfohlene Demo-Architektur

Fuer die ersten externen Tests reicht:

- 1 VM
- Docker Compose
- Reverse Proxy oder direkter Gateway-Zugriff
- Keycloak auf derselben VM
- RabbitMQ auf derselben VM
- PostgreSQL auf derselben VM
- taegliches Datenbank-Backup
- HTTPS via Lets Encrypt oder Cloudflare

Nicht noetig fuer die erste Demo:

- Kubernetes
- Service Mesh
- separates Monitoring-Cluster
- GitOps-Plattform

## Demo-Betriebsmodus fuer Travelmate

Fuer eine einfache Demo sollte Folgendes gelten:

- `LLM_ENABLED=false`
- Mailpit nicht als oeffentlicher Mailweg, sondern durch echtes SMTP ersetzt
- kleine Datenmengen, keine grossen Datei-Uploads
- Restart und Wartung duerfen tolerierbar sein

## SMTP-Empfehlung

Fuer die Demo ist **Brevo SMTP** die pragmatischste Standardempfehlung:

- Free-Plan mit 300 E-Mails pro Tag
- SMTP-Relay direkt verfuegbar
- guter Fit fuer Demo- und Einladungsmails
- spaeter ohne Architekturwechsel auf bezahlten Plan erweiterbar

Resend ist ebenfalls solide, hat im Free-Plan fuer den Demo-Fall aber die engere Tagesgrenze.

## Entscheidung in Kurzform

| Ziel | Empfehlung |
|---|---|
| Billigste Demo-Option | Oracle Cloud Always Free + Docker Compose |
| Beste fruehe Demo-Option | Hetzner Cloud CAX21 + Docker Compose |
| Alternative mit einfacher Plattform-UI | DigitalOcean Basic Droplet + Docker Compose |

## Weiterfuehrung

Das konkrete Setup fuer die empfohlene Demo-Variante ist beschrieben in:

- [`2026-03-26-demo-betriebskonzept.md`](./2026-03-26-demo-betriebskonzept.md)

## Quellen

- Oracle Cloud Always Free Resources: <https://docs.oracle.com/iaas/Content/FreeTier/freetier_topic-Always_Free_Resources.htm>
- Hetzner Price Adjustment ab 2026-04-01: <https://docs.hetzner.com/general/infrastructure-and-availability/price-adjustment/>
- Brevo Pricing / Free Plan: <https://help.brevo.com/hc/en-us/articles/208589409>
- Brevo Free Plan Limits: <https://help.brevo.com/hc/en-us/articles/208580669-What-are-the-limits-of-the-Free-plans>
- Brevo SMTP Setup: <https://help.brevo.com/hc/en-us/articles/7924908994450-Send-transactional-emails-using-Brevo-SMTP>
- DigitalOcean Droplet Pricing: <https://www.digitalocean.com/pricing/droplets>
- DigitalOcean Droplet Pricing Docs: <https://docs.digitalocean.com/products/droplets/details/pricing/>
- Railway Pricing: <https://railway.com/pricing>
- Render Deploy for Free: <https://render.com/docs/free>
- Render Service Types: <https://render.com/docs/service-types>
- Resend Pricing: <https://resend.com/pricing>
- Resend Sending Limits: <https://resend.com/docs/knowledge-base/resend-sending-limits>
