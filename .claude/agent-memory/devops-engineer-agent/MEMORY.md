# DevOps Engineer Agent Memory

## Current Infrastructure
- Docker Compose: 10 services (4 PostgreSQL, Keycloak, Mailpit, RabbitMQ, 3 SCS)
- Dockerfile: Multi-stage build (Eclipse Temurin 21 JDK → JRE)
- e2e-test.sh: Smoke test script
- No CI/CD pipeline yet (GitHub Actions, GitLab CI)
- No IaC yet (Terraform, Ansible)

## Ports
- Gateway: 8080, IAM: 8081, Trips: 8082, Expense: 8083
- PostgreSQL: 5432-5435, Keycloak: 7082
- RabbitMQ: 5672/15672, Mailpit: 1025/8025
