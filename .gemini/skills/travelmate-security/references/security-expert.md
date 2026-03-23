# Security Expert Reference

## STRIDE Threat Modeling
- **Spoofing Identity**: Authentication bypass, token theft.
- **Tampering**: Input manipulation, JWT tampering.
- **Repudiation**: Missing audit trails.
- **Information Disclosure**: Data leakage, verbose errors.
- **Denial of Service**: Resource exhaustion.
- **Elevation of Privilege**: Tenant isolation bypass, role escalation.

## OWASP Top 10 Review Checklist
1. **Broken Access Control**: Check tenant isolation and IDOR.
2. **Cryptographic Failures**: Check JWT handling and token lifecycle.
3. **Injection**: Check for string concatenation in queries and `th:utext`.
4. **Insecure Design**: Review business logic and missing rate limits.
5. **Security Misconfiguration**: Check Spring Security config and headers.
6. **Vulnerable Components**: Scan for known CVEs in dependencies.
7. **Auth Failures**: Check token refresh and session management.
8. **SSRF**: Review external request points for SSRF vectors.

## Security Report Structure
1. **Executive Summary**
2. **Findings** (Severity, Location, Description, Proof of Concept, Remediation)
3. **Risk Matrix**
4. **Recommendations**
