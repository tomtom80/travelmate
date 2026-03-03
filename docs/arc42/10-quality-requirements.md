# 10. Qualitätsanforderungen

## Qualitätsbaum

```
Qualität
├── Benutzbarkeit (Usability)
│   ├── Mobile-First
│   ├── Einfache Bedienung
│   └── Offline-Fähigkeit
├── Zuverlässigkeit (Reliability)
│   ├── Datenkonsistenz
│   ├── Event-Idempotenz
│   └── Fehlertoleranz
├── Sicherheit (Security)
│   ├── Mandantentrennung
│   ├── Authentifizierung (OIDC)
│   └── Autorisierung (Rollen)
├── Wartbarkeit (Maintainability)
│   ├── Modulare SCS-Struktur
│   ├── Hexagonale Architektur
│   └── Testabdeckung (TDD)
└── Portabilität (Portability)
    ├── Container-basiertes Deployment
    └── PWA (plattformunabhängig)
```

## Qualitätsszenarien

| ID | Qualitätsziel | Szenario | Maßnahme |
|----|--------------|----------|----------|
| Q1 | **Mobile-First** | Ein Teilnehmer möchte unterwegs die Einkaufsliste auf dem Smartphone einsehen | PWA mit responsivem Design, Thymeleaf-Templates optimiert für mobile Viewports |
| Q2 | **Offline-Fähigkeit** | In der Hütte gibt es kein Internet; der Nutzer will trotzdem die Einkaufsliste sehen | Service Worker cached kritische Seiten und Daten |
| Q3 | **Einfache Bedienung** | Ein technisch wenig affines Familienmitglied soll einen Beleg erfassen können | Intuitive Formulare, Kamera-Integration für Belegfotos, minimale Klickpfade |
| Q4 | **Mandantentrennung** | Zwei Familien nutzen das System; keine Familie darf Daten der anderen sehen | TenantId-Scoping auf allen Aggregaten, TenantContext-Filter |
| Q5 | **Datenkonsistenz** | Eine Rollenzuweisung im IAM muss zuverlässig im Trips-SCS ankommen | Kafka garantiert At-Least-Once-Delivery, Consumer sind idempotent |
| Q6 | **Testbarkeit** | Ein Entwickler möchte die Domain-Logik ohne Spring-Kontext testen | Hexagonale Architektur: Domain-Schicht hat keine Framework-Abhängigkeiten |
| Q7 | **Unabhängiges Deployment** | Ein neues Feature im Trips-SCS soll ohne IAM-Deployment ausgerollt werden | SCS-Architektur mit eigenständigen Deployables |
| Q8 | **Sicherheit** | Nur Organisatoren dürfen Trips erstellen | OIDC-Token enthält Rollen, Spring Security prüft Autorisierung |

## Referenz

![Quality Scenarios](../../design/evia.team.orc.thomas-klingler%20-%20Quality%20Scenarios.jpg)
