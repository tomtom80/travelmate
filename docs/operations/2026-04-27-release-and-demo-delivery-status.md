# Release and Demo Delivery Status

**Date**: 2026-04-27

## Release Status

- latest stable tag: `v0.18.0`
- current repository version: `0.19.0-SNAPSHOT`
- current `main` branch includes CI hardening for Maven reactor test execution and a timestamp-precision fix in IAM invitation token persistence

## GitHub Actions Status

### CI workflow

File: [`../../.github/workflows/ci.yml`](../../.github/workflows/ci.yml)

- runs on pull requests and pushes to `main`
- executes `./mvnw -B -ntp verify`
- is the baseline quality gate for repository changes

### Demo delivery workflow

File: [`../../.github/workflows/demo-deploy.yml`](../../.github/workflows/demo-deploy.yml)

Stages:

1. `build-test`
2. `publish-images`
3. `deploy-demo`

The workflow builds service images, pushes them to GHCR, uploads demo deployment files via SSH, and redeploys the demo stack with the current image tag.

## GHCR Push Prerequisites

The image-push workflow uses `GITHUB_TOKEN`. No extra write secret is required if GitHub package ownership is configured correctly.

Required conditions:

- repository setting `Settings -> Actions -> General -> Workflow permissions` must be `Read and write permissions`
- workflow permission `packages: write` must stay enabled in the workflow file
- existing GHCR packages such as `ghcr.io/tomtom80/travelmate-iam` must be linked to the repository `tomtom80/travelmate`
- if a package was created outside this repository, `Manage Actions access` on the package must explicitly allow this repository

Typical failure symptom:

- `denied: permission_denied: write_package`

This error means authentication worked, but the package write authorization for the repository is missing.

## Demo Secrets

The current demo deployment expects these GitHub secrets:

- `DEMO_SSH_PRIVATE_KEY`
- `DEMO_SSH_HOST`
- `DEMO_SSH_USER`
- `DEMO_SSH_PORT`
- `DEMO_APP_DIR`
- `DEMO_ENV_FILE`
- `DEMO_GHCR_READ_TOKEN`

## Current Delivery Risks

- GHCR package ownership and repository access are an operational prerequisite, not just a YAML concern
- production-grade secrets handling, backups, and observability are still follow-up items beyond the current demo automation
- the demo workflow is suitable for a controlled demo environment, not yet a full production release pipeline

## Go-Live Planning Reference

The canonical planning path from the current demo-oriented delivery model to a major release now lives in:

- [`../backlog/roadmap-v1.0.0.md`](../backlog/roadmap-v1.0.0.md)
- [`../backlog/iteration-19-plan.md`](../backlog/iteration-19-plan.md)
- [`../backlog/iteration-20-plan.md`](../backlog/iteration-20-plan.md)
- [`../backlog/iteration-21-plan.md`](../backlog/iteration-21-plan.md)
- [`../backlog/iteration-22-plan.md`](../backlog/iteration-22-plan.md)
- [`../backlog/iteration-23-plan.md`](../backlog/iteration-23-plan.md)
- [`../backlog/iteration-24-plan.md`](../backlog/iteration-24-plan.md)
- [`../backlog/iteration-25-plan.md`](../backlog/iteration-25-plan.md)
