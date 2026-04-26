# Security Expert Agent Memory

- [Security Findings Baseline (v0.17.0)](findings_v0_17_baseline.md) — concrete code-verified gaps as of v0.17.0; CSRF disabled, UrlSanitizer accepts HTTP, AdminController under-authorized, no SCA/audit/rate-limit
- Roadmap output: `reports/roadmap-security.md` (2026-04-26) — OWASP Top 10 coverage, STRIDE per BC, hardening backlog SEC-01..25 + GDPR-01..10, Iter 18-25 sequence

## Known Mitigations (verified in code as of v0.17.0)
- Keycloak OIDC at gateway, JWT validation on each SCS via `oauth2ResourceServer.jwt()`
- TenantId scoping in domain + JPA queries (no DB-level RLS)
- Thymeleaf auto-escaping (no `th:utext` anywhere)
- Spring Data JPA only — no native queries with user input
- `@Profile("!test")` / `@Profile("test")` clean separation
- HtmlFetcher (Trips Accommodation Import) has SSRF mitigations (size limit, timeout, redirect limit)
- ADR-0016 documents SSRF strategy; ADR-0023 documents Trip-delete cascade

## Known NOT-mitigations (v0.17.0)
- CSRF disabled in all four SecurityConfigs (contradicts security-rules.md)
- No security headers set anywhere
- No SCA pipeline (no Dependency-Check, no Dependabot)
- No audit logging
- No rate limiting at gateway
- AdminController only `.authenticated()`, no role check
- Default credentials in application.yml (work without ENV override)
