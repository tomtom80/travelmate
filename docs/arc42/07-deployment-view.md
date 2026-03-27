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
├── Keycloak                Port 7082
├── RabbitMQ (AMQP)         Port 5672
└── RabbitMQ Management UI  Port 15672
```

### Starten der lokalen Umgebung

```bash
# Gesamte Infrastruktur (RabbitMQ + Keycloak + PostgreSQL)
docker compose up -d
```

Die Services selbst laufen lokal auf Port **8080** (jeweils einzeln):

```bash
cd travelmate-iam && ./mvnw spring-boot:run
cd travelmate-trips && ./mvnw spring-boot:run
```

## Produktionsumgebung

Aktueller Stand:

- Es existiert derzeit kein umgesetzter produktiver Kubernetes-Stack im Repository.
- Die aktuelle produktionsnahe Betriebsdiskussion basiert auf einer Migration vom Compose-orientierten Setup zu Kubernetes.
- Die aktuelle Marktrecherche und Zielbild-Empfehlung ist dokumentiert in
  [`../operations/2026-03-26-kubernetes-hosting-marktrecherche.md`](../operations/2026-03-26-kubernetes-hosting-marktrecherche.md).

### Zielbild fuer eine Kubernetes-basierte Produktionsumgebung

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
│  │            RabbitMQ (AMQP)               │  │
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

Das Diagramm beschreibt ein moegliches Zielbild, nicht den aktuell implementierten Plattformstand.

### Erwartete CI/CD-Bausteine fuer das Zielbild

- GitHub Actions fuer Build, Test und Deployment-Orchestrierung
- Container-Build pro SCS (`gateway`, `iam`, `trips`, `expense`)
- Kubernetes-Deployment mit Helm oder Kustomize
- gestufte Einfuehrung: zuerst stateless Services, danach Stateful Workloads

## Referenzen

![Network Design](../../design/evia.team.orc.thomas-klingler%20-%20Network%20Design.jpg)

![Network Diagramm](../../design/evia.team.orc.thomas-klingler%20-%20Network%20Diagramm.jpg)
