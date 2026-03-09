# Security Rules (Always Active)

These security rules are ALWAYS enforced when writing or reviewing code.

## Authentication & Authorization
- Every SCS is an OAuth2 Resource Server — JWT validation is mandatory
- Gateway is the SOLE OIDC entry point (TokenRelay)
- Use Keycloak roles (`organizer`, `participant`) for method-level security
- SecurityConfig MUST use `@Profile("!test")` for production, `@Profile("test")` for test

## Tenant Isolation (CRITICAL)
- EVERY database query MUST be scoped by TenantId
- NEVER expose TenantId in URLs where users can manipulate it
- Resolve TenantId from JWT email claim server-side
- Cross-tenant access = SECURITY VULNERABILITY

## Input Validation
- All Value Objects self-validate via `Assertion` (null checks, format, bounds)
- Validate at system boundaries (controllers, message consumers)
- Use Thymeleaf `th:text` (auto-escaped) — NEVER `th:utext` without explicit approval

## Injection Prevention
- Spring Data JPA only — no string concatenation in queries
- No `@Query(nativeQuery=true)` with user input
- No `Runtime.exec()` or `ProcessBuilder` with user input
- CSRF protection active (Spring Security default) — do not disable

## Secrets Management
- No hardcoded credentials in source code
- Environment variables for all secrets (DB_PASSWORD, etc.)
- Never log sensitive data (passwords, tokens, PII)
