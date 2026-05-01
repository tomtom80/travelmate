# Iteration 21 — Production Hardening I

**Target Version**: `v0.21.0`  
**Status**: PLANNED

## Goal

Iteration 21 moves Travelmate from demo-grade delivery toward first production-grade operating discipline.

## Planned Scope

### Functional stories

- receipt editing and related expense-flow polish where still required for v1 readiness

### Non-functional stories

- CSRF strategy corrected for browser-based state changes
- security headers defined and enforced
- production secrets strategy introduced
- **S21-KEYCLOAK-CLIENT-SECRET-ROTATION** — make the `travelmate-gateway`
  Keycloak client secret env-driven so it can be rotated without rebuilding
  the realm template. See story detail below.
- TLS termination and deployment topology documented and implemented for the chosen target
- backup and restore path documented and practically verifiable
- CI/CD extended beyond basic verify and demo deploy toward environment promotion

## Planned Deliverables

- first real production hardening bundle across application and operations
- reduced exposure from browser and deployment misconfiguration risks
- explicit path for recovery from failed deployments or lost data

## Acceptance

- state-changing browser requests are protected consistently
- no dangerous secret defaults remain acceptable for production operation
- rollback and restore procedures exist and are testable
- CI/CD architecture is documented beyond the current single demo path
- S21-KEYCLOAK-CLIENT-SECRET-ROTATION acceptance criteria below are met end to end

---

## Story Detail: S21-KEYCLOAK-CLIENT-SECRET-ROTATION

### User Story

**As an** ops engineer,
**I want** the Keycloak `travelmate-gateway` client secret to be sourced from
the `KEYCLOAK_CLIENT_SECRET` env var instead of being hardcoded in the realm
template,
**so that** I can rotate the secret without rebuilding the realm-export
template or manually editing it through the Keycloak Admin UI.

### Background

`docker/keycloak/realm-export.template.json:50` contains
`"secret": "travelmate-gateway-secret"` — a **static value** that's
identical across every realm import. The init script
`docker/keycloak/init-smtp-from-env.sh:50-65` updates `redirectUris`,
`baseUrl`, and `webOrigins`, but **does not patch the client secret**.
Two operational consequences:

1. The secret in `.env.demo` (`KEYCLOAK_CLIENT_SECRET=...`) must **exactly
   match** the hardcoded value, or OIDC login fails. Discovered in the
   2026-04-30 demo when the team noted: *"client_secret=travelmate-gateway-secret
   is the hardcoded value from the realm template — adjust if you changed
   it manually in Keycloak Admin"*.

2. **Rotating** the secret requires updates in both the template and
   `.env.demo`. After redeploy, Keycloak re-imports the realm, which can
   overwrite other realm config that was changed via `kcadm.sh` since
   the last deploy.

### Acceptance Criteria

1. **Template substitution**: `docker/keycloak/realm-export.template.json`
   uses a `${CLIENT_SECRET}` placeholder for the `travelmate-gateway`
   client's `secret` field, analogous to the existing `${APP_BASE_URL}`
   placeholder for redirect URIs.

2. **Init script**: `docker/keycloak/init-realm-from-env.sh` substitutes
   `${CLIENT_SECRET}` from the env var `KEYCLOAK_CLIENT_SECRET` (sed
   substitution, like the existing `APP_BASE_URL` flow).

3. **Hot rotation path**: `docker/keycloak/init-smtp-from-env.sh` (or a new
   sibling script) calls `kcadm.sh update clients/{uuid} -s
   secret=${KEYCLOAK_CLIENT_SECRET}` after Keycloak is up. This handles
   the case where the running Keycloak still has the old secret from a
   previous deploy and the user wants to rotate without a full re-import.

4. **Documentation**: `.env.demo.travelmate-demo.example` (and any other
   `.env.demo*.example`) explicitly note that `KEYCLOAK_CLIENT_SECRET`
   is a secret value and must never be committed.

5. **Manual rotation drill**: a documented procedure in
   `docs/operations/secrets-rotation.md` (or extend the existing
   `2026-03-26-demo-betriebskonzept.md`) showing the exact sequence to
   rotate the client secret on the demo VM. Drill executed and proven
   to work — OIDC login still functional after rotation.

6. **Tests**: a new ArchUnit/integration test that fails if
   `realm-export.template.json` contains a literal hardcoded secret
   string in the `travelmate-gateway` client config.

### Technical Notes

- Keycloak accepts a plaintext `secret` field at realm-import time;
  internally it's hashed at runtime. Rotation post-import must use
  `kcadm.sh` (which writes through to the proper internal storage).
- This story is closely related to the broader Iter-21 **production
  secrets strategy** non-functional story. Could be implemented as
  the first concrete instance of that strategy. If a `Vault` or
  `SOPS` solution is chosen for the strategy, this story should
  fit into that flow rather than reinvent its own.

### Risks

- If both the template and the running realm hold different secrets
  on the same Keycloak instance after upgrade, the realm-import will
  silently overwrite the running value. Mitigation: the realm import
  is idempotent only on first run (`--import-realm` skips if realm
  already exists, by default). Verify this assumption before
  implementation.

### Out of Scope

- External secret manager integration (HashiCorp Vault, AWS Secrets
  Manager). Separate feature for Iter-22 or later.
- Automatic periodic rotation. A documented manual drill is sufficient
  for the v1.0 milestone.
- Rotation of other secrets (DB passwords, `KEYCLOAK_ADMIN_PASSWORD`,
  RabbitMQ passwords). Each follows its own rotation pattern; this
  story focuses on the OIDC-client secret because it's the one with
  the surfaced demo-test pain point.
