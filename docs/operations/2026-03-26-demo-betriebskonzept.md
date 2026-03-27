# Demo-Betriebskonzept fuer Travelmate (Stand 2026-03-26)

## Zielbild

Dieses Betriebskonzept beschreibt eine einfache, guenstige und ausreichend brauchbare Demo-Umgebung fuer Travelmate.

Empfohlene Zielplattform:

- 1 Hetzner Cloud VM
- bevorzugt `CAX21`
- Ubuntu LTS
- Docker Engine + Docker Compose
- oeffentlicher Zugriff nur ueber HTTPS auf das Gateway

Die Umgebung ist ausdruecklich fuer Demo, fruehes Feedback und kleine Nutzergruppen gedacht, nicht fuer belastbaren Produktivbetrieb.

## Warum dieses Zielbild

Travelmate besteht aktuell aus mehreren eng gekoppelten Laufzeitkomponenten:

- Gateway
- IAM
- Trips
- Expense
- Keycloak
- RabbitMQ
- mehreren PostgreSQL-Datenbanken

Fuer einen kleinen Freundeskreis ist die niedrigste brauchbare Komplexitaet deshalb:

- alle Komponenten auf einer VM
- Docker Compose als Betriebswerkzeug
- kein Kubernetes
- kein PaaS-Splitting ueber mehrere Plattformdienste

## Empfohlene Infrastruktur

### VM

Empfehlung:

- `Hetzner CAX21` als Startpunkt
- Root-Disk Standardgroesse ausreichend, solange keine grossen Upload-Mengen anfallen

Optional noch guenstiger:

- `CAX11`, nur bei sehr kleinem Testkreis und bewusst knappen Ressourcen

### DNS und HTTPS

Empfehlung:

- eine Demo-Domain oder Subdomain, z. B. `demo.travelmate.example`
- DNS per Hetzner DNS oder Cloudflare
- TLS via Let's Encrypt

Zugriff von aussen:

- `443/tcp` fuer HTTPS
- optional `80/tcp` nur fuer ACME-Challenge und Redirect

Nicht von aussen freigeben:

- PostgreSQL
- RabbitMQ
- Keycloak-Interna
- Mailpit

## Laufzeitarchitektur

### Oeffentlich erreichbarer Pfad

Nur der Gateway-Service soll oeffentlich erreichbar sein. Alle anderen Container bleiben intern im Docker-Netz.

Empfohlener Request-Fluss:

1. Browser greift auf `https://demo.travelmate.example` zu
2. Reverse Proxy auf dem Host terminiert TLS
3. Reverse Proxy leitet an den Gateway-Container weiter
4. Gateway routet intern an IAM, Trips und Expense
5. Gateway und Services sprechen intern mit Keycloak, RabbitMQ und PostgreSQL

### Reverse Proxy

Empfehlung:

- Caddy oder Nginx auf dem Host

Fuer die Demo ist Caddy meist am einfachsten:

- automatische Zertifikate
- einfache Konfiguration
- sauberer HTTPS-Default

## Compose-Prinzip fuer die Demo

Das vorhandene `docker-compose.yml` ist eine gute Basis, sollte fuer die Demo aber in einigen Punkten von der lokalen Entwicklung entkoppelt werden.

### Unbedingt anpassen

- `LLM_ENABLED=false` fuer `trips` und `expense`
- `ollama` und `ollama-init` nicht deployen
- `MAIL_HOST` und `MAIL_PORT` auf echten SMTP-Dienst umstellen oder Mailversand fuer Demo-Flows bewusst einschränken
- keine unnötigen Port-Freigaben fuer interne Container
- externe Konfiguration ueber `.env` oder Compose-Overrides statt harter lokaler Defaults

### Ports

Empfohlen oeffentlich:

- Host `443 -> reverse proxy`

Empfohlen nur lokal oder gar nicht gebunden:

- `8080` Gateway nur intern oder localhost-gebunden
- `7082` Keycloak nur intern oder localhost-gebunden
- `5432-5435`, `5672`, `15672`, `1025`, `8025` nicht oeffentlich

## Travelmate-spezifische Konfiguration

### Gateway

Der Gateway ist bereits der richtige Einstiegspunkt und routet zu `iam`, `trips` und `expense`.

Fuer die Demo sollten die OIDC-URLs auf die oeffentlich erreichbare Domain zeigen, damit Browser-Redirects sauber funktionieren.

Beispielhafte Zielwerte:

- `KEYCLOAK_AUTH_URI=https://demo.travelmate.example/auth/realms/travelmate/protocol/openid-connect/auth`
- `KEYCLOAK_TOKEN_URI=http://keycloak:8080/realms/travelmate/protocol/openid-connect/token`
- `KEYCLOAK_JWK_URI=http://keycloak:8080/realms/travelmate/protocol/openid-connect/certs`
- `KEYCLOAK_USERINFO_URI=http://keycloak:8080/realms/travelmate/protocol/openid-connect/userinfo`
- `KEYCLOAK_END_SESSION_URI=https://demo.travelmate.example/auth/realms/travelmate/protocol/openid-connect/logout`

Prinzip:

- browser-facing URLs extern
- server-to-server URLs intern

### Keycloak

Fuer die Demo sollte Keycloak nicht mit `localhost` als Hostname betrieben werden.

Stattdessen:

- `KC_HOSTNAME` auf die externe Demo-Domain beziehungsweise den Auth-Pfad setzen
- Realm-Import beibehalten
- Admin-Zugang mit starkem Passwort versehen

Falls der Reverse Proxy den Pfad `/auth` auf Keycloak mappt, muss die Aussenadresse konsistent in Gateway und Keycloak konfiguriert sein.

### Mail

Mailpit ist fuer lokale Entwicklung gut, fuer eine externe Demo aber nur begrenzt sinnvoll.

Empfehlung fuer die Demo:

- SMTP ueber Mailgun, Brevo oder einen vorhandenen Mailserver
- alternativ Demo ohne echte E-Mail-Flows, wenn diese nicht im Fokus stehen

### Datenbanken

Fuer die Demo ist die bestehende Datenbanktrennung pro SCS in Ordnung. Sie muss nicht fuer den Anfang konsolidiert werden.

Wichtig ist:

- Persistenz ueber Docker Volumes
- taegliches Backup
- keine externe DB-Portfreigabe

## Deployment-Ablauf

### Einfachster Betriebsweg

Empfehlung:

1. Quellcode per Git auf die VM holen
2. Build und Compose-Start direkt auf der VM
3. Deploy per `git pull` und `docker compose up -d --build`

Das ist fuer eine Demo ausreichend und operativ am einfachsten.

### Etwas sauberere Variante

Wenn ein minimaler CI/CD-Fluss gewuenscht ist:

1. GitHub Actions baut Images
2. Images werden in eine Registry gepusht
3. VM zieht neue Images
4. Deploy via `docker compose pull && docker compose up -d`

Fuer die erste Demo ist das optional. Der Zusatznutzen ist klein, solange nur wenige Releases stattfinden.

### Empfohlen fuer haeufige Demo-Redeployments

Wenn die Demo-Umgebung regelmaessig aktualisiert werden soll, ist der empfohlene Weg:

1. GitHub Actions baut und testet den Monorepo-Stand
2. GitHub Actions baut Container-Images fuer `gateway`, `iam`, `trips` und `expense`
3. Die Images werden nach GHCR gepusht
4. GitHub Actions synchronisiert Demo-Dateien auf die VM
5. GitHub Actions startet den Redeploy per SSH automatisch

Damit entfaellt manuelles Deployment auf dem Host fuer normale Updates.

### Benoetigte GitHub-Secrets fuer die Demo-CD

Fuer den automatischen Redeploy werden folgende Repository- oder Environment-Secrets benoetigt:

- `DEMO_SSH_HOST`
- `DEMO_SSH_USER`
- `DEMO_SSH_PORT` optional, Standard ist `22`
- `DEMO_SSH_PRIVATE_KEY`
- `DEMO_APP_DIR` z. B. `/opt/travelmate-demo`
- `DEMO_ENV_FILE` als kompletter Inhalt der produktionsnahen `.env.demo`
- `DEMO_GHCR_READ_TOKEN` mit mindestens `read:packages`

Empfehlung:

- die Secrets in einem GitHub-Environment `demo` ablegen
- den Deploy-Workflow nur fuer `main` und `workflow_dispatch` erlauben
- das Environment mit Required Review absichern, falls spaeter noetig

## Backups

Minimalanforderung fuer die Demo:

- taeglicher PostgreSQL-Dump
- Aufbewahrung fuer 7 Tage
- Ablage nicht nur lokal auf derselben VM

Einfache Umsetzung:

- Cronjob oder systemd timer
- `pg_dump` pro Datenbank
- Upload nach S3-kompatiblem Storage oder Hetzner Storage Box

## Monitoring und Betrieb

Fuer die Demo reicht ein sehr kleines Betriebsset:

- Docker Container Logs
- einfacher Healthcheck via `docker compose ps`
- Verfuegbarkeitscheck von aussen auf `/actuator/health` nur ueber Gateway oder per interner Admin-Route
- Uptime-Monitor von extern, z. B. UptimeRobot oder Better Stack Free

Nicht noetig fuer die erste Demo:

- Prometheus/Grafana
- zentrales Tracing
- ELK/Loki Stack

## Minimal-Hardening

Auch fuer eine Demo sollten folgende Massnahmen umgesetzt werden:

- SSH nur per Key
- Root-Login deaktivieren
- Firewall aktivieren
- nur `80` und `443` von aussen offen
- starke Passwoerter fuer Keycloak Admin und Datenbanken
- Container-Neustart mit `restart: unless-stopped`
- keine Default-Credentials in oeffentlichen Dokumenten oder produktionsnahen `.env`-Dateien

## Empfohlene Umsetzungsreihenfolge

1. VM bereitstellen
2. Docker und Compose installieren
3. Demo-Domain und DNS anlegen
4. Reverse Proxy mit HTTPS einrichten
5. Compose-Datei fuer Demo ableiten
6. `.env` fuer Demo-Geheimnisse und Hostnamen anlegen
7. Ollama deaktivieren
8. Mail-Konzept festlegen
9. Backup-Job einrichten
10. ersten End-to-End-Durchlauf mit externer URL testen

## Nicht-Ziele der Demo-Umgebung

Nicht Teil dieses Betriebskonzepts:

- Hochverfuegbarkeit
- Zero-Downtime-Deployments
- horizontale Skalierung
- Kubernetes
- GitOps
- vollstaendige Observability-Plattform

## Konkrete Empfehlung

Fuer den Anfang sollte Travelmate als Demo so betrieben werden:

- `Hetzner CAX21`
- `Docker Compose`
- `Caddy oder Nginx`
- `LLM_ENABLED=false`
- `echtes HTTPS`
- `taegliche DB-Backups`
- `nur Gateway oeffentlich`

Das ist der beste Punkt auf der Kurve zwischen Kosten, Einfachheit und Brauchbarkeit.

## Konkrete Artefakte im Repository

Das Betriebskonzept ist mit folgenden Startartefakten hinterlegt:

- [`/docker-compose.demo.yml`](../../docker-compose.demo.yml)
- [`/.env.demo.example`](../../.env.demo.example)
- [`/.env.demo.hetzner-brevo.example`](../../.env.demo.hetzner-brevo.example)
- [`/Caddyfile.demo`](../../Caddyfile.demo)
- [`/scripts/deploy-demo.sh`](../../scripts/deploy-demo.sh)
- [`/scripts/bootstrap-demo-server.sh`](../../scripts/bootstrap-demo-server.sh)
- [`/.github/workflows/ci.yml`](../../.github/workflows/ci.yml)
- [`/.github/workflows/demo-deploy.yml`](../../.github/workflows/demo-deploy.yml)
