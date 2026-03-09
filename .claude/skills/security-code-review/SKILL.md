---
name: Security Code Review
description: "Review code for OWASP Top 10 vulnerabilities, secure programming practices, and Travelmate-specific security requirements"
user-invocable: false
---

# Security Code Review Skill

Systematic security code review following OWASP secure coding practices.

## Review Checklist

### A01: Broken Access Control
- [ ] All endpoints enforce authentication (except public: /signup, /login)
- [ ] TenantId scoping on every database query
- [ ] Role-based access control (organizer/participant) enforced
- [ ] No IDOR (Insecure Direct Object Reference) — IDs are not guessable
- [ ] CORS configuration restricts origins
- [ ] HTTP method restrictions on endpoints

### A02: Cryptographic Failures
- [ ] No sensitive data in URLs or logs
- [ ] JWT tokens properly validated (signature, expiry, issuer)
- [ ] No hardcoded secrets or credentials
- [ ] TLS enforced for all external communication

### A03: Injection
- [ ] No string concatenation in JPA queries
- [ ] No native SQL queries without parameterization
- [ ] Thymeleaf uses `th:text` (escaped), NOT `th:utext` (unescaped)
- [ ] No shell command execution with user input
- [ ] HTMX endpoints validate all parameters

### A04: Insecure Design
- [ ] Rate limiting on authentication endpoints
- [ ] Account lockout after failed login attempts (Keycloak config)
- [ ] Business logic validated in domain layer (Assertion)
- [ ] Invite tokens are single-use and time-limited

### A05: Security Misconfiguration
- [ ] SecurityConfig @Profile("!test") for production
- [ ] Security headers: X-Frame-Options, X-Content-Type-Options, CSP
- [ ] Error pages don't expose stack traces
- [ ] Actuator endpoints secured or disabled in production
- [ ] Default credentials changed (Keycloak admin, RabbitMQ, PostgreSQL)

### A06: Vulnerable Components
- [ ] Spring Boot BOM manages dependency versions
- [ ] No known CVEs in current dependencies
- [ ] Review: `./mvnw dependency:tree` for unexpected transitive deps

### A07: Authentication Failures
- [ ] Keycloak handles password policies
- [ ] Session timeout configured
- [ ] JWT refresh token rotation
- [ ] Logout invalidates session (RP-Initiated Logout)

### A08: Data Integrity
- [ ] CSRF protection enabled (Spring Security default)
- [ ] Domain events integrity (trusted RabbitMQ internal network)
- [ ] Flyway migrations are immutable (checksums validated)

### A09: Logging & Monitoring
- [ ] Security events logged (login, logout, failed auth, access denied)
- [ ] No sensitive data in logs (passwords, tokens, PII)
- [ ] Audit trail for state changes

### A10: SSRF
- [ ] No user-controlled URLs in server-side HTTP calls
- [ ] Keycloak Admin API calls use configured base URL only

## Grep Patterns for Common Issues

```bash
# Unescaped HTML output
grep -r "th:utext" --include="*.html"

# Native SQL queries
grep -r "@Query.*nativeQuery.*true" --include="*.java"

# String concatenation in queries
grep -r "\"SELECT\|\"INSERT\|\"UPDATE\|\"DELETE" --include="*.java"

# Shell execution
grep -r "Runtime.getRuntime().exec\|ProcessBuilder" --include="*.java"

# Hardcoded credentials
grep -ri "password\s*=\s*\"" --include="*.java" --include="*.yml" --include="*.properties"

# Missing @Profile on SecurityConfig
grep -rn "SecurityConfig" --include="*.java" | grep -v "@Profile"
```

## Report Format

For each finding:
```markdown
### [SEVERITY] [Finding Title]
- **File**: `path/to/file.java:line`
- **Category**: OWASP A0X
- **Code**: [snippet showing the issue]
- **Risk**: [explanation of impact]
- **Fix**: [code showing the remediation]
```
