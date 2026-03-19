# 3. Kontextabgrenzung

## Fachlicher Kontext

Travelmate ermöglicht Familien und Freundesgruppen die gemeinsame Planung von Hüttenurlaubs. Jede Gruppe (Tenant) arbeitet in einem isolierten Mandanten.

### Externe Akteure

| Akteur | Beschreibung |
|--------|-------------|
| **Organisator** | Erstellt Trips, verwaltet Teilnehmer, plant Mahlzeiten, verwaltet Unterkunft und Einkaufsliste |
| **Teilnehmer** | Nimmt am Trip teil, traegt Mahlzeiten ein, erfasst Belege, kauft Einkaufslistenartikel ein |
| **Keycloak** | Externes Identity Management (OIDC) |

## Technischer Kontext

```
                         ┌─────────────┐
                         │   Browser   │
                         │    (PWA)    │
                         └──────┬──────┘
                                │ HTTPS
                         ┌──────▼──────┐
                         │   Spring    │
                         │   Cloud     │
                         │   Gateway   │
                         └──────┬──────┘
                    ┌───────────┼───────────┐
                    │           │           │
             ┌──────▼──┐ ┌─────▼────┐ ┌────▼─────┐
             │   IAM   │ │  Trips   │ │ Expense  │
             │  (SCS)  │ │  (SCS)   │ │  (SCS)   │
             └────┬────┘ └────┬─────┘ └────┬─────┘
                  │           │            │
            ┌─────▼──┐  ┌────▼───┐  ┌─────▼──┐
            │Postgres│  │Postgres│  │Postgres│
            └────────┘  └────────┘  └────────┘
                  │           │            │
                  └─────┬─────┘────────────┘
                        │
                  ┌─────▼─────┐
                  │ RabbitMQ  │
                  │  (AMQP)   │
                  └───────────┘
                        │
                  ┌─────▼─────┐
                  │ Keycloak  │
                  │  (OIDC)   │
                  └───────────┘
```

### Schnittstellen

| Kanal | Von / Nach | Protokoll | Beschreibung |
|-------|-----------|-----------|-------------|
| HTTP/S | Browser → Gateway | HTTPS | Alle Benutzerinteraktionen |
| HTTP | Gateway → SCS | HTTP | Internes Routing zu den Services |
| AMQP | IAM → Trips | Async | Domain Events via Topic Exchange (z.B. `AccountRegistered`) |
| AMQP | IAM → Expense | Async | Domain Events via Topic Exchange |
| AMQP | Trips → Expense | Async | Domain Events via Topic Exchange (`TripCreated`, `ParticipantJoinedTrip`, `TripCompleted`, `AccommodationPriceSet`) |
| OIDC | SCS → Keycloak | HTTPS | Authentifizierung und Token-Validierung |
| JDBC | SCS → PostgreSQL | TCP | Datenpersistierung (je Service eigene DB) |

## Referenz

![System Context](../../design/evia.team.orc.thomas-klingler%20-%20System%20Context.jpg)
