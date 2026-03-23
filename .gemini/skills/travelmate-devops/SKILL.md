---
name: travelmate-devops
description: "CI/CD, infrastructure automation (Terraform, Ansible), Docker optimization, and SRE practices (SLOs, monitoring). Use when discussing pipelines, deployment, infrastructure, or reliability."
---

# Travelmate DevOps & SRE Skill

Expertise in CI/CD, infrastructure as code, container orchestration, and site reliability engineering for the Travelmate project.

## Core Mandates
- Follow Google's SRE practices.
- Use `e2e-test.sh` for local smoke testing.
- Target: GitHub Actions for CI/CD.

## Specialized Workflows

### 🚀 1. CI/CD Pipeline Design
- Build & Unit Test (Maven)
- Security Scan (dependency check, SAST)
- Docker Build (multi-stage per SCS)
- Integration Test (docker compose + E2E)
- Deploy (push images, target environments)
- **Reference**: See [ci-cd-sre.md](references/ci-cd-sre.md).

### 🏗️ 2. Infrastructure as Code (Terraform)
- Managed services: PostgreSQL, RabbitMQ, Keycloak.
- Environments: `dev`, `staging`, `production`.
- **Reference**: See [ci-cd-sre.md](references/ci-cd-sre.md).

### 🐳 3. Docker Optimization
- Multi-stage builds (JDK -> JRE).
- Health checks for all 10 services in `docker-compose.yml`.
- **Reference**: See [ci-cd-sre.md](references/ci-cd-sre.md).

### 📊 4. Monitoring & SRE
- Golden Signals: Latency, Traffic, Errors, Saturation.
- Metrics: Micrometer + Prometheus.
- SLOs/SLIs and Error Budgets.
- **Reference**: See [ci-cd-sre.md](references/ci-cd-sre.md).
