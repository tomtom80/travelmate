# 7. Verteilungssicht

## Lokale Entwicklungsumgebung (Docker Compose)

Die lokale Infrastruktur wird über Docker Compose bereitgestellt:

```
docker-compose.yml
│
├── PostgreSQL (IAM)        Port 5432
├── PostgreSQL (Trips)      Port 5433
├── PostgreSQL (Expense)    Port 5434
├── PostgreSQL (Keycloak)   Port 5435
├── Keycloak                Port 8180
├── Kafka (KRaft)           Port 9093 (extern) / 9092 (intern)
└── Kafka-UI                Port 8090
```

### Starten der lokalen Umgebung

```bash
# IAM-Infrastruktur (Kafka + Keycloak + PostgreSQL)
cd travelmate-iam/docker && docker compose up -d

# Trips-Infrastruktur (Kafka + PostgreSQL)
cd travelmate-trips/docker && docker compose up -d
```

Die Services selbst laufen lokal auf Port **8080** (jeweils einzeln):

```bash
cd travelmate-iam && ./mvnw spring-boot:run
cd travelmate-trips && ./mvnw spring-boot:run
```

## Produktionsumgebung (Kubernetes)

```
┌─────────────────────────────────────────────────┐
│                  Kubernetes Cluster              │
│                                                  │
│  ┌──────────┐  ┌──────────┐  ┌──────────────┐  │
│  │ Gateway  │  │ IAM Pod  │  │  Trips Pod   │  │
│  │   Pod    │  │          │  │              │  │
│  └────┬─────┘  └────┬─────┘  └──────┬───────┘  │
│       │              │               │          │
│  ┌────▼──────────────▼───────────────▼───────┐  │
│  │              Kafka (KRaft)                │  │
│  └───────────────────────────────────────────┘  │
│                                                  │
│  ┌──────────┐  ┌──────────┐  ┌──────────────┐  │
│  │Postgres  │  │Postgres  │  │  Postgres    │  │
│  │ (IAM)    │  │ (Trips)  │  │  (Expense)   │  │
│  └──────────┘  └──────────┘  └──────────────┘  │
│                                                  │
│  ┌──────────────┐                               │
│  │   Keycloak   │                               │
│  └──────────────┘                               │
└─────────────────────────────────────────────────┘
```

### CI/CD Pipeline

- **Azure Pipelines** (`azure-pipelines.yml` je Service)
- **Stufe 1:** Tests ausführen (`./mvnw clean test`)
- **Stufe 2:** Docker-Image bauen und in Google Artifact Registry pushen
- **Registry:** `europe-west3-docker.pkg.dev/travelmate-dev-422115/travelmate-cr/`
- **Deployment-Manifeste:** `travelmate-iam/manifest/`

## Referenzen

![Network Design](../../design/evia.team.orc.thomas-klingler%20-%20Network%20Design.jpg)

![Network Diagramm](../../design/evia.team.orc.thomas-klingler%20-%20Network%20Diagramm.jpg)
