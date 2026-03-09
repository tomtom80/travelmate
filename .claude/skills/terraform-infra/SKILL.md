---
name: Terraform Infrastructure
description: "Design and implement Terraform modules for Travelmate cloud infrastructure — databases, messaging, identity, application containers"
user-invocable: false
---

# Terraform Infrastructure Skill

Design infrastructure as code for Travelmate deployment.

## Required Infrastructure

| Component | Local (Docker) | Cloud (Terraform) |
|-----------|---------------|-------------------|
| PostgreSQL IAM | postgres-iam:5432 | Managed PostgreSQL |
| PostgreSQL Trips | postgres-trips:5433 | Managed PostgreSQL |
| PostgreSQL Expense | postgres-expense:5434 | Managed PostgreSQL |
| PostgreSQL Keycloak | postgres-keycloak:5435 | Managed PostgreSQL |
| RabbitMQ | rabbitmq:5672 | Managed Message Broker |
| Keycloak | keycloak:7082 | Container/VM |
| Gateway | gateway:8080 | Container + LB |
| IAM | iam:8081 | Container |
| Trips | trips:8082 | Container |
| Expense | expense:8083 | Container |
| Mailpit (dev only) | mailpit:8025 | Real SMTP in production |

## Module Structure

```hcl
# infrastructure/terraform/
# ├── modules/
# │   ├── database/       # PostgreSQL instances
# │   ├── messaging/      # RabbitMQ
# │   ├── identity/       # Keycloak
# │   ├── application/    # SCS containers
# │   ├── networking/     # VPC, subnets, SGs
# │   └── monitoring/     # Logging, metrics
# ├── environments/
# │   ├── dev/
# │   ├── staging/
# │   └── production/
# └── variables.tf
```

## Environment Variables (per SCS)
```
DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD
SPRING_RABBITMQ_HOST, SPRING_RABBITMQ_PORT
SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI
```

## Output Location
- Terraform configs → `infrastructure/terraform/`
- Ansible playbooks → `infrastructure/ansible/`
