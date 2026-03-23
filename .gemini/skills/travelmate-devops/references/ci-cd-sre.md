# CI/CD & SRE Reference

## CI/CD Pipeline Stages
1. **Build**: `./mvnw clean verify` (common modules first, then SCS).
2. **Security**: Dependency check, SAST scan.
3. **Package**: Build Docker images per SCS (Eclipse Temurin 21 JRE base).
4. **Test**: `docker compose up` + Playwright E2E (`./mvnw -Pe2e verify`).
5. **Deploy**: Push images, deploy to AWS/Hetzner/DigitalOcean.

## Terraform Infrastructure
- **Modules**: `database` (PostgreSQL), `messaging` (RabbitMQ), `identity` (Keycloak), `application` (SCS), `networking` (VPC), `monitoring`.
- **Environments**: `dev`, `staging`, `production`.

## SRE Monitoring Stack
- **Metrics**: Micrometer + Prometheus (Spring Boot Actuator).
- **Logging**: Structured JSON (Logback).
- **Tracing**: OpenTelemetry.
- **Alerting**: Prometheus alerting rules.

## The Four Golden Signals
- **Latency**: P50/P95/P99 response time.
- **Traffic**: Requests/sec per SCS endpoint.
- **Errors**: 5xx rate, failed event deliveries.
- **Saturation**: CPU, Memory, DB connection pool, RabbitMQ queue depth.
