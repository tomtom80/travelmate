# ADR-0003: Spring Cloud Gateway als zentraler Entry Point

## Status

Accepted

## Context

Die SCS-Architektur (siehe ADR-0001) erfordert einen zentralen Entry Point, der Requests an die einzelnen SCS routet. Zusaetzlich muss die OIDC-Token-Validierung zentral erfolgen, damit nicht jedes SCS die komplette OAuth2-Login-Logik implementieren muss. Der Gateway soll auch als Reverse Proxy fuer die PWA (siehe ADR-0005) dienen.

## Decision

Wir verwenden Spring Cloud Gateway als zentralen Entry Point. Der Gateway uebernimmt:

- OIDC-Authentifizierung gegenueber Keycloak (Authorization Code Flow mit PKCE)
- Token Relay: Weiterleitung des Access Tokens an die SCS
- Path-basiertes Routing zu den SCS (`/iam/**`, `/trips/**`, `/expense/**`)
- CORS-Konfiguration fuer die PWA

Die SCS selbst agieren als OAuth2 Resource Server und validieren nur noch das JWT.

## Consequences

### Positive

- Zentrale Authentifizierung an einer Stelle, SCS bleiben schlank
- Einheitlicher Entry Point fuer alle Clients (Browser, PWA)
- Path-basiertes Routing ermoeglicht transparente SCS-Integration
- Token Relay vereinfacht die Security-Konfiguration in den SCS

### Negative

- Gateway ist Single Point of Entry (muss hochverfuegbar sein)
- Zusaetzliche Latenz durch den Proxy-Hop
- Spring Cloud Gateway erfordert WebFlux (reaktiver Stack), waehrend SCS Servlet-basiert sind
