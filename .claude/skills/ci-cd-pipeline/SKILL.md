---
name: CI/CD Pipeline
description: "Design and implement CI/CD pipelines for the Travelmate Maven multi-module project with Docker-based E2E testing"
user-invocable: false
---

# CI/CD Pipeline Skill

Design CI/CD pipelines for the Travelmate project.

## GitHub Actions Pipeline

### Build & Test Workflow
```yaml
name: Build & Test
on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 21
          cache: maven
      - name: Build & Test
        run: ./mvnw clean verify

  e2e:
    needs: build
    runs-on: ubuntu-latest
    services:
      # Docker Compose services for E2E
    steps:
      - name: Start infrastructure
        run: docker compose up -d
      - name: Wait for health
        run: ./e2e-test.sh
      - name: Run E2E tests
        run: ./mvnw -Pe2e verify
```

### Module-Specific Build
```yaml
# Only build/test affected modules
# common changes → rebuild all
# iam changes → rebuild common + iam
# trips changes → rebuild common + trips
```

### Release Workflow
```yaml
# 1. Set release version in pom.xml
# 2. ./mvnw clean verify
# 3. git tag vX.Y.Z
# 4. Build Docker images
# 5. Push to registry
# 6. Set next SNAPSHOT version
```

## Key Considerations
- Maven CI-Friendly Versions: `${revision}` property
- flatten-maven-plugin resolves at build time
- E2E requires: 4x PostgreSQL, RabbitMQ, Keycloak, Mailpit
- Docker multi-stage build (existing Dockerfile)
- Module dependency: common → gateway, iam, trips, expense

## Output Location
- GitHub Actions → `.github/workflows/`
- GitLab CI → `.gitlab-ci.yml`
