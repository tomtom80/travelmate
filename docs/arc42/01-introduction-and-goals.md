# 1. Einführung und Ziele

## Aufgabenstellung

Travelmate ist eine Multi-Tenant-Plattform zur Planung von Hüttenurlaubs (Cabin Holidays). Das System ersetzt die bisherige Excel-basierte Planung, die über WhatsApp-Gruppen koordiniert wurde.

### Wesentliche Features

- **Trip-Planung:** Erstellen und Verwalten von Hüttenurlaubs inkl. Unterkunft, Zeitraum und Teilnehmer
- **Essensplanung:** Frühstück- und Abendessen-Planung für die gesamte Reise mit Zutatenverwaltung
- **Einkaufslisten:** Automatische Generierung aus den geplanten Mahlzeiten
- **Abrechnung:** Erfassung von Belegen (mit Foto), gewichtete Aufteilung und Saldo-Berechnung pro Familie
- **Standort-Abstimmung:** Gemeinsame Auswahl der Unterkunft
- **Anzahlungen:** Verwaltung von Vorauszahlungen

## Qualitätsziele

| Priorität | Qualitätsziel | Motivation |
|-----------|---------------|------------|
| 1 | **Mobile-First** | Die Hauptnutzung erfolgt unterwegs über Smartphones (PWA) |
| 2 | **Offline-Fähigkeit** | Im Hüttenurlaub ist die Internetverbindung oft eingeschränkt |
| 3 | **Einfache Bedienung** | Nutzer sind Familien mit unterschiedlicher technischer Affinität |
| 4 | **Multi-Tenancy** | Jede Familie/Freundesgruppe arbeitet in ihrem eigenen Mandanten |
| 5 | **Erweiterbarkeit** | Neue Bounded Contexts (z.B. Expense) sollen unabhängig entwickelt werden können |

## Stakeholder

| Rolle | Erwartung |
|-------|-----------|
| **Familien / Freundesgruppen** | Einfache, intuitive Planung des Hüttenurlaubs ohne technische Hürden |
| **Organisatoren** | Überblick über Teilnehmer, Mahlzeiten, Einkäufe und Kosten |
| **Teilnehmer** | Eintragen von Mahlzeiten, Einsehen der Einkaufsliste, Erfassen von Belegen |
| **Entwickler (Kleinteam)** | Wartbare, testbare Architektur mit klaren Bounded Contexts |

## Referenz

![Requirements](../../design/evia.team.orc.thomas-klingler%20-%20Requirements.jpg)
