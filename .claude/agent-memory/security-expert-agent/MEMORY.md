# Security Expert Agent Memory

## Threat Models
- (to be populated after first threat model analysis)

## Security Review Findings
- (to be populated after first security review)

## Pentest Results
- (to be populated after first penetration test)

## Known Mitigations
- Keycloak OIDC for authentication
- JWT validation on all SCS
- TenantId scoping on all aggregates
- Thymeleaf auto-escaping for XSS prevention
- Spring Security CSRF protection
- Spring Data JPA for SQL injection prevention
- @Profile("!test") on SecurityConfig
