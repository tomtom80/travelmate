# Release v0.12.3

**Release Date**: 2026-03-27
**Release Type**: Patch Release
**Based On**: `v0.12.2`

## Scope

`v0.12.3` konsolidiert die Betriebs- und Demo-Vorbereitung nach `v0.12.2` und fuegt einen durchgaengigen Low-Cost-Hosting- und Redeploy-Pfad fuer Travelmate hinzu.

- Marktrecherche fuer guenstige Hosting- und Kubernetes-Optionen dokumentiert
- Demo-Betriebskonzept fuer Hetzner, Docker Compose und Caddy dokumentiert
- Go-Live-Checkliste fuer den ersten externen Demo-Betrieb erstellt
- Demo-Compose-Stack und Demo-Umgebungsdateien fuer Hetzner/Brevo angelegt
- GitHub Actions fuer CI sowie automatisierten Demo-Deploy nach GHCR/SSH eingefuehrt
- lokale Konfiguration fuer SMTP- und RabbitMQ-Credentials per Umgebungsvariablen erweitert

## Verification

- Quellstand auf Release-Version `0.12.3` gesetzt
- Demo-Compose-Konfiguration mit `docker compose --env-file .env.demo.example -f docker-compose.demo.yml config` validiert
- Shell-Skripte fuer SMTP-Setup, Bootstrap und Deploy per `bash -n` geprueft

## Included Work Since v0.12.2

- `docs: add hosting market research and demo operations documentation`
- `feat: add demo compose, env templates and caddy setup`
- `feat: add GitHub Actions CI and demo deployment workflow`
- `chore: parameterize SMTP and RabbitMQ credentials for demo hosting`

## Notes

Diese Freigabe fokussiert nicht auf neue Endnutzerfunktionen, sondern auf die betriebliche Faehigkeit, Travelmate schnell als Demo-Umgebung mit echten E-Mails und haeufigen Redeployments bereitzustellen.
