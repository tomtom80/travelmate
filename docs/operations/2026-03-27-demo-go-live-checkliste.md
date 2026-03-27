# Demo Go-Live Checkliste (Stand 2026-03-27)

## Ziel

Diese Checkliste fasst die letzten Schritte fuer den ersten externen Demo-Betrieb von Travelmate zusammen.

Die empfohlene Zielkombination ist:

- Hetzner Cloud VM
- Docker Compose Demo-Stack
- Caddy als Reverse Proxy
- GHCR fuer Container-Images
- GitHub Actions fuer CI/CD
- Brevo als SMTP-Provider

## Referenzartefakte im Repository

- Demo-Compose: [`../../docker-compose.demo.yml`](../../docker-compose.demo.yml)
- Demo-Env-Vorlage: [`../../.env.demo.hetzner-brevo.example`](../../.env.demo.hetzner-brevo.example)
- Caddy: [`../../Caddyfile.demo`](../../Caddyfile.demo)
- Server-Bootstrap: [`../../scripts/bootstrap-demo-server.sh`](../../scripts/bootstrap-demo-server.sh)
- Redeploy-Skript: [`../../scripts/deploy-demo.sh`](../../scripts/deploy-demo.sh)
- CI: [`../../.github/workflows/ci.yml`](../../.github/workflows/ci.yml)
- CD: [`../../.github/workflows/demo-deploy.yml`](../../.github/workflows/demo-deploy.yml)

## 1. Hetzner vorbereiten

1. Server anlegen:
   - Empfehlung: `CAX21`
   - Ubuntu LTS
2. Floating IP ist fuer die Demo nicht noetig.
3. SSH-Key beim Server anlegen.
4. DNS vorbereiten:
   - `demo.travelmate.example`
   - `auth.demo.travelmate.example`
5. Beide DNS-Eintraege auf die Server-IP zeigen lassen.

## 2. Server bootstrappen

Auf dem Server das Bootstrap-Skript ausfuehren:

```bash
sudo DEMO_DOMAIN=demo.travelmate.example \
  DEMO_AUTH_DOMAIN=auth.demo.travelmate.example \
  ACME_EMAIL=ops@travelmate.example \
  bash scripts/bootstrap-demo-server.sh
```

Das Skript erledigt:

- Docker und Compose
- optional Caddy
- Demo-Verzeichnis
- Docker-Berechtigung fuer den Demo-Benutzer
- Basis-Firewall

## 3. Brevo einrichten

### Konto und Domain

1. Brevo-Konto anlegen.
2. Absenderdomain oder Absenderadresse verifizieren.
3. Wenn moeglich, die komplette Domain verifizieren statt nur einer Einzeladresse.

### SMTP-Zugang

1. In Brevo den SMTP-Zugang oeffnen.
2. SMTP-Login notieren.
3. SMTP-Key erzeugen oder vorhandenen Key verwenden.
4. Host und Port festhalten:
   - Host: `smtp-relay.brevo.com`
   - Port: `587`
   - Auth: `true`
   - STARTTLS: `true`

### Absender

Empfohlene Demo-Werte:

- `SMTP_FROM=noreply@travelmate.example`
- `SMTP_FROM_DISPLAY_NAME=Travelmate-Demo`

Wichtig:

- `SMTP_FROM` muss zu einer in Brevo erlaubten Absenderadresse oder Domain passen.
- Falls Einladungs- oder Verifikationsmails nicht ankommen, zuerst Brevo-Absenderstatus und Versandlogs pruefen.

## 4. Demo-Env-Datei erstellen

Die Datei basiert auf:

- [`../../.env.demo.hetzner-brevo.example`](../../.env.demo.hetzner-brevo.example)

Kopierbare Vorlage:

```env
TRAVELMATE_PUBLIC_URL=https://demo.travelmate.example
KEYCLOAK_PUBLIC_URL=https://auth.demo.travelmate.example

IMAGE_REGISTRY=ghcr.io
IMAGE_NAMESPACE=your-github-user
IMAGE_TAG=demo

KEYCLOAK_ADMIN=travelmate-admin
KEYCLOAK_ADMIN_PASSWORD=replace-with-strong-password
KEYCLOAK_CLIENT_SECRET=replace-with-keycloak-client-secret
KEYCLOAK_DB_PASSWORD=replace-with-keycloak-db-password

DB_PASSWORD=replace-with-app-db-password

RABBITMQ_USERNAME=travelmate
RABBITMQ_PASSWORD=replace-with-rabbitmq-password

SMTP_HOST=smtp-relay.brevo.com
SMTP_PORT=587
SMTP_FROM=noreply@travelmate.example
SMTP_FROM_DISPLAY_NAME=Travelmate-Demo
MAIL_USERNAME=replace-with-brevo-smtp-login
MAIL_PASSWORD=replace-with-brevo-smtp-key
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS_ENABLE=true
SMTP_SSL=false
```

Diese Datei ist fuer GitHub gedacht:

- Inhalt als Secret `DEMO_ENV_FILE` speichern
- wird vom Deploy-Workflow nach `/opt/travelmate-demo/.env.demo` geschrieben

## 5. GitHub Environment und Secrets anlegen

Empfehlung:

- GitHub Environment `demo` anlegen

Benoetigte Secrets:

- `DEMO_SSH_HOST`
- `DEMO_SSH_USER`
- `DEMO_SSH_PORT`
- `DEMO_SSH_PRIVATE_KEY`
- `DEMO_APP_DIR`
- `DEMO_ENV_FILE`
- `DEMO_GHCR_READ_TOKEN`

Empfohlene Werte:

- `DEMO_APP_DIR=/opt/travelmate-demo`
- `DEMO_SSH_PORT=22`

Fuer `DEMO_GHCR_READ_TOKEN`:

- Fine-grained Token oder PAT mit mindestens `read:packages`

## 6. Keycloak und Gateway pruefen

Vor dem ersten oeffentlichen Test sicherstellen:

- `KEYCLOAK_CLIENT_SECRET` passt zum Client `travelmate-gateway`
- App-Domain und Auth-Domain sind oeffentlich erreichbar
- HTTPS ist fuer beide Domains aktiv

## 7. Ersten Deploy ausloesen

1. `main` pushen oder
2. GitHub Action `Demo Deploy` manuell starten

Der erwartete Ablauf:

1. Maven `verify`
2. Build und Push der vier App-Images nach GHCR
3. Upload der Demo-Artefakte auf den Server
4. automatischer SSH-basierter Redeploy

## 8. Nach dem Deploy pruefen

Smoke-Test:

- Startseite unter `https://demo.travelmate.example`
- Login-Redirect auf `https://auth.demo.travelmate.example`
- Registrierung oder Login funktioniert
- Mailversand funktioniert
- eine einfache Trips- oder Expense-Funktion funktioniert

Technische Checks:

- `docker compose ps`
- Container-Logs von `gateway`, `iam`, `trips`, `expense`, `keycloak`
- Brevo-Versandlog

## 9. Was fuer den Anfang bewusst offen bleiben darf

Nicht blockierend fuer den ersten Demo-Go-Live:

- zentrale Observability
- HA
- Kubernetes
- Rollback-Automatisierung jenseits alter Image-Tags

## Empfehlung in Kurzform

Fuer den ersten oeffentlichen Demo-Betrieb ist der einfachste vernuenftige Weg:

- Hetzner `CAX21`
- Brevo SMTP
- GitHub Actions `CI + Demo Deploy`
- `docker-compose.demo.yml`
- `.env.demo` aus der Brevo/Hetzner-Vorlage
