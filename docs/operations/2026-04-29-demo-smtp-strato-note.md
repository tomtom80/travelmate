# Demo SMTP Hinweis fuer STRATO (Stand 2026-04-29)

## Ausgangslage

Fuer die Demo-Umgebung wurde STRATO als Domain- und Mail-Anbieter verwendet.

Die theoretisch naheliegende Konfiguration ist:

- `SMTP_HOST=smtp.strato.de`
- `SMTP_PORT=465`
- `SMTP_SSL=true`
- `MAIL_SMTP_STARTTLS_ENABLE=false`

Im realen Demo-Betrieb auf der Hetzner-VM trat dabei jedoch ein Verbindungs-Timeout aus den App-Containern auf.

## Beobachtung

Der Fehler zeigte sich als TCP-Timeout beim Verbindungsaufbau von `trips` zu `smtp.strato.de:465`.

Das spricht nicht fuer einen Authentifizierungsfehler, sondern fuer ein praktisches Konnektivitaetsproblem auf diesem Port im Demo-Betrieb.

## Empfohlene Demo-Konfiguration

Fuer den Travelmate-Demo-Host sollte STRATO daher ueber Submission mit STARTTLS konfiguriert werden:

- `SMTP_HOST=smtp.strato.de`
- `SMTP_PORT=587`
- `MAIL_SMTP_AUTH=true`
- `MAIL_SMTP_STARTTLS_ENABLE=true`
- `SMTP_SSL=false`

## Konsequenz

- Die Datei [`.env.demo.travelmate-demo.example`](/Users/t.klingler/repos/privat/travelmate/.env.demo.travelmate-demo.example) verwendet fuer das STRATO-Demo-Setup jetzt `587` statt `465`.
- Bereits laufende Demo-Container muessen nach der Aenderung mit aktualisierter `.env.demo` neu erstellt werden.

## Operative Schritte auf dem Demo-Host

```bash
cd /opt/travelmate-demo
sudo -u travelmate docker compose --env-file .env.demo -f docker-compose.demo.yml up -d --force-recreate iam trips keycloak
```

## Quellen

- STRATO SMTP-Server und Ports: https://www.strato.de/faq/mail/so-lauten-die-strato-e-mail-server/
- STRATO Hinweis zu STARTTLS ueber Port 587 fuer Smarthost/Relay: https://www.strato.de/faq/mail/wie-kann-ich-meine-e-mails-ueber-eine-gesicherte-verbindung-ssl-oder-tls-versenden-und-empfangen/
