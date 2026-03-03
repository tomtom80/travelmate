# ADR-0002: Keycloak + OIDC statt eigener Auth-Implementierung

## Status

Accepted

## Context

Travelmate benoetigt eine sichere Authentifizierung und Autorisierung ueber alle SCS hinweg. Eine eigene Auth-Implementierung waere aufwendig und fehleranfaellig. Ausserdem soll spaeter Social Login (Google, Apple) einfach integrierbar sein. Die Loesung muss Multi-Tenancy unterstuetzen, da jeder Huettenurlaub-Veranstalter einen eigenen Tenant darstellt.

## Decision

Wir setzen Keycloak als Identity Provider mit OpenID Connect (OIDC) ein. Jeder Tenant wird als Keycloak Realm abgebildet. Die SCS validieren Access Tokens (JWT) ueber Spring Security OAuth2 Resource Server. Die Authentifizierung laeuft ueber den Authorization Code Flow mit PKCE, initiiert durch den Spring Cloud Gateway (siehe ADR-0003).

## Consequences

### Positive

- Bewaehrte, produktionsreife Auth-Loesung ohne eigene Implementierung
- Social Login (Google, Apple, GitHub) spaeter trivial ueber Keycloak Identity Providers erweiterbar
- Standardisiertes OIDC-Protokoll, keine proprietaere Kopplung
- Multi-Tenancy ueber Keycloak Realms nativ unterstuetzt
- Zentrales User-Management mit Admin-UI out of the box

### Negative

- Zusaetzliche Infrastruktur-Komponente (Keycloak muss betrieben werden)
- Keycloak-Konfiguration (Realms, Clients, Roles) muss automatisiert werden
- Abhaengigkeit von Keycloak Release-Zyklen und Breaking Changes
