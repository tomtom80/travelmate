# ADR-0005: PWA statt nativer Mobile App

## Status

Accepted

## Context

Travelmate soll auch mobil nutzbar sein, z.B. um waehrend des Huettenurlaubs Ausgaben zu erfassen und Belege per Kamera zu fotografieren. Eine native App (iOS/Android) wuerde separate Codebases, App-Store-Prozesse und zusaetzliche Entwicklungs-Expertise erfordern. Das Projekt hat begrenzte Ressourcen.

## Decision

Wir liefern Travelmate als Progressive Web App (PWA) aus. Die SSR-basierte Webanwendung (siehe ADR-0004) wird um ein Web App Manifest und einen Service Worker ergaenzt. Die PWA ist responsiv, installierbar auf dem Homescreen und kann Geraete-APIs wie die Kamera (fuer Belegfotos) nutzen. Offline-Faehigkeit wird fuer kritische Funktionen (Ausgabenerfassung) angestrebt.

## Consequences

### Positive

- Eine Codebasis fuer Desktop und Mobile
- Kein App-Store-Review-Prozess, sofortige Updates
- Kamera-Zugriff fuer Belegfotos ueber Web APIs moeglich
- Installierbar auf dem Homescreen (App-aehnliche Erfahrung)
- Geringerer Entwicklungsaufwand als native Apps

### Negative

- Eingeschraenkter Zugriff auf native APIs im Vergleich zu nativen Apps (vor allem iOS)
- Push Notifications auf iOS erst seit iOS 16.4 verfuegbar
- Offline-Faehigkeit erfordert sorgfaeltige Service-Worker-Implementierung
- Kein Vertrieb ueber App Stores (geringere Sichtbarkeit)
