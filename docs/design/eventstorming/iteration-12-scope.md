# Design-Level EventStorming: Iteration 12 -- Gemeinsames Reisekonto pro Reise mit Partei-Konten

**Datum**: 2026-03-19
**Scope**: Aktuelle Reise, Teilnehmerpflege, StayPeriods, Organizer-Rollen, laufende Abrechnung pro Reisepartei
**Methode**: Design-Level EventStorming (Alberto Brandolini)
**Bounded Contexts**: Trips (Core), Expense (Generic), IAM nur indirekt
**Ausgangslage**: v0.11.0; die bisherige Sicht "Settlement am Ende mit Ausgleichszahlungen zwischen Teilnehmern" trifft den gewuenschten Fachprozess nicht

---

## 1. Fachliche Leitidee

Die Abrechnung einer Reise ist ein **gemeinsames Reisekonto**.

Es gibt:

1. ein **Gesamtkonto der Reise**
2. darunter **Konten je Reisepartei/Familie**

Nicht gewuenscht ist:

- eine primaere Darstellung "Teilnehmer A zahlt an Teilnehmer B"
- eine reine Endabrechnung erst nach Reiseende

Gewuenscht ist:

- laufende Sicht auf den aktuellen Stand waehrend der Reise
- Reisepartei-basierte Belastungen und Guthaben
- ein Gesamtreisekonto, in das Kosten und Einkaufsbelege einfliessen

---

## 2. Neue fachliche Regeln

### 2.1 Teilnehmerpflege an der Reise

1. Jedes **Mitglied einer Reisepartei** darf jederzeit weitere Mitglieder oder Mitreisende/Kinder zu einer Reise hinzufuegen.
2. Jedes **Mitglied einer Reisepartei** darf Teilnehmer der eigenen Reisepartei aus der Reise entfernen.
3. Jedes **Mitglied einer Reisepartei** darf fuer die eigene Reisepartei die `StayPeriod` setzen oder aendern.
4. **Reise-Organizer** duerfen die `StayPeriod` fuer **alle** Teilnehmer setzen.
5. **Reise-Organizer** duerfen weitere Teilnehmer zu Organizern machen.

### 2.2 Review-Regel

Das Review ist nachrangig.

- Das 4-Augen-Review darf auch von normalen Teilnehmern uebernommen werden.
- Wichtig ist nur: `reviewer != submitter`.
- Eine harte Organizer-Pflicht fuer Reviews ist **nicht** Teil des Kernmodells dieser Iteration.

### 2.3 Abrechnungsmodell

1. Es gibt ein **gemeinsames Reisekonto** fuer die Reise.
2. Kosten werden fachlich auf **Reiseparteien** gerechnet, nicht primaer auf Einzelpersonen.
3. Der Preis einer Unterkunft fliesst als Kostenblock in das Gesamtkonto ein.
4. Der Unterkunftskostenblock wird auf **Gesamtteilnehmerbasis** verteilt.
5. Die Verteilung orientiert sich an:
   - `StayPeriod`
   - Gewichtung aller Teilnehmer
   - Personenzahl im Gesamtmodell
6. **Anzahlungen** werden **nicht** nach Gewichtung oder Aufenthaltsdauer verteilt, sondern zu **gleichen Anteilen auf alle Reiseparteien**.
7. Einkaufsbelege laufen wie Unterkunftskosten als **negativer Betrag in das Gesamtreisekonto** ein.
8. Gleichzeitig werden Einkaufsbelege bei der **leistenden Reisepartei als positiver Betrag/Guthaben** gewertet.

---

## 3. Warum das bisherige Modell nicht passt

Die bisherige Richtung war:

```text
Trip completed
-> Settlement berechnen
-> Transfers "A zahlt an B"
```

Das ist fachlich zu spaet und in der falschen Einheit.

Die gewuenschte Einheit ist:

```text
Reise -> gemeinsames Konto -> Reisepartei-Konten
```

Die zentrale Frage lautet nicht:

- "Wer zahlt wem?"

sondern:

- "Wie ist der aktuelle Stand meiner Reisepartei im gemeinsamen Reisekonto?"

---

## 4. Ubiquitous Language

| Begriff | Bedeutung |
|--------|-----------|
| Gemeinsames Reisekonto | Gesamtkonto aller Kosten und Gutschriften einer Reise |
| Partei-Konto | Konto einer Reisepartei/Familie innerhalb des gemeinsamen Reisekontos |
| Belastung | Anteilige Kosten, die nach Verrechnung aus dem Gesamtreisekonto einer Reisepartei zugerechnet werden |
| Guthaben | Positiver Betrag zugunsten einer Reisepartei, z.B. durch geleistete Einkaeufe oder bezahlte Anzahlungen |
| Aktueller Stand | Saldo eines Partei-Kontos zum aktuellen Zeitpunkt |
| Unterkunftskostenblock | Kostenblock aus definierter Unterkunft mit Gesamtpreis |
| Anzahlung | gleicher fixer Vorab-Betrag je Reisepartei |
| Einkaufsbeleg | negativer Eintrag ins Gesamtreisekonto und zugleich positiver Eintrag fuer die leistende Reisepartei |
| StayPeriod | Aufenthaltsdauer eines Teilnehmers auf der Reise |
| Reise-Organizer | Teilnehmer mit erweiterten Rechten auf Reiseebene |

Wichtige Trennung:

```text
Partei-Konto != Teilnehmer-Transferliste
```

---

## 5. Revidierter Iterationsfokus

| ID | Story-Cluster | Size | Bounded Context | Begruendung |
|----|---------------|------|-----------------|-------------|
| S12-A | Teilnehmer der Reise flexibel pflegen | L | Trips | Kern fuer reale Familienreisen |
| S12-B | StayPeriods pro Partei selbst pflegen, Organizer global | M | Trips | direkter Input fuer faire Kosten |
| S12-C | Organizer-Rolle auf Reiseebene erweitern | M | Trips / IAM Rand | Governance fuer Reiseorganisation |
| S12-D | Gemeinsames Reisekonto frueh oeffnen | L | Expense | laufende Reise statt Post-Trip |
| S12-E | Partei-Konten statt Teilnehmer-Transfers | L | Expense | zentrale fachliche Umstellung |
| S12-F | Unterkunft, Anzahlungen, Einkaufsbelege in Partei-Salden integrieren | L | Expense | fachlicher Kernnutzen |

Nicht Fokus:

- Bring-Integration
- Recipe Import
- Polls
- komplexe Transferoptimierung zwischen Einzelpersonen

---

## 6. Design-Level EventStorming

### 6.1 Orange Stickies: Domain Events

```text
TRIPS
  [1] TripCreated
  [2] ParticipantAddedToTrip
  [3] ParticipantRemovedFromTrip
  [4] StayPeriodSetForParticipant
  [5] TripOrganizerGranted

EXPENSE
  [6] TripAccountOpened
  [7] PartyAccountRegistered
  [8] AccommodationCostRegistered
  [9] GlobalCostSharesRecomputed
 [10] EqualAdvanceRequested
 [11] EqualAdvanceConfirmedForAllParties
 [12] PartyAdvanceMarkedPaid
 [13] ReceiptSubmitted
 [14] ReceiptApproved
 [15] ReceiptRejected
 [16] ReceiptPostedToSharedTripAccount
 [17] PartyCreditRegisteredFromReceipt
 [18] PartyBalanceRecomputed
 [19] FinalTripAccountClosed
```

### 6.2 Vollstaendige Timeline

```text
                    TRIPS SCS                          EXPENSE SCS
                    ---------                          -----------
    [1] TripCreated
              |
              +-------------------------------------> OpenTripAccount
                                                        |
                                                        v
                                                   [6] TripAccountOpened

 Reisepartei fuegt Teilnehmer hinzu
              |
              v
    [2] ParticipantAddedToTrip
              |
              +-------------------------------------> Partei/Teilnehmer im Konto registrieren
                                                        |
                                                        v
                                                   [7] PartyAccountRegistered

 StayPeriod wird gesetzt/geaendert
              |
              v
    [4] StayPeriodSetForParticipant
              |
              +-------------------------------------> globale Kostenanteile neu berechnen
                                                        |
                                                        v
                                                   [9] GlobalCostSharesRecomputed

 Unterkunft wird definiert / Preis gesetzt
              |
              v
    AccommodationPriceSet
              |
              +-------------------------------------> [8] AccommodationCostRegistered
                                                        |
                                                        v
                                                   [9] GlobalCostSharesRecomputed

 Organizer bestaetigt gleiche Anzahlungen
              |
              v
                                              [10] EqualAdvanceRequested
                                                      |
                                                      v
                                              [11] EqualAdvanceConfirmedForAllParties

 Partei bezahlt Anzahlung
              |
              v
                                              [12] PartyAdvanceMarkedPaid
                                                      |
                                                      v
                                              [17] PartyBalanceRecomputed

 Teilnehmer reicht Einkaufsbeleg ein
              |
              v
                                              [13] ReceiptSubmitted
                                                      |
                                                      +--> optional [14] / [15] Review
                                                      |
                                                      v
                                              [16] ReceiptPostedToSharedTripAccount
                                                      |
                                                      v
                                              [17] PartyCreditRegisteredFromReceipt
                                                      |
                                                      v
                                              [18] PartyBalanceRecomputed

 Reise spaeter finalisieren
              |
              v
                                              [19] FinalTripAccountClosed
```

---

## 7. Commands (Blue Stickies)

| # | Command | Actor | Aggregate / Model | Vorbedingung |
|---|---------|-------|-------------------|-------------|
| C1 | AddParticipantToTrip(party, participant) | Mitglied der Reisepartei | Trip | Teilnehmer gehoert zur eigenen Partei |
| C2 | RemoveParticipantFromTrip(participantId) | Mitglied der Reisepartei | Trip | Teilnehmer gehoert zur eigenen Partei |
| C3 | SetOwnPartyStayPeriod(participantId, arrival, departure) | Mitglied der Reisepartei | Trip | Teilnehmer gehoert zur eigenen Partei |
| C4 | SetParticipantStayPeriod(participantId, arrival, departure) | Organizer | Trip | Teilnehmer ist Teil der Reise |
| C5 | GrantTripOrganizer(participantId) | Organizer | Trip / OrganizerPolicy | Teilnehmer ist Teil der Reise |
| C6 | OpenTripAccount(tripId) | System | Expense | Reise wurde erstellt |
| C7 | RegisterAccommodationCost(tripId, totalPrice) | Organizer/System | Expense | Unterkunftspreis ist bekannt |
| C8 | ConfirmEqualAdvanceForAllParties(tripId, amountPerParty) | Organizer | Expense | Parteien existieren |
| C9 | MarkPartyAdvancePaid(tripId, partyTenantId) | Organizer | Expense | Advance existiert |
| C10 | SubmitReceipt(tripId, paidByParticipantId, submittedBy, amount, category, date) | Participant | Expense | Konto ist offen |
| C11 | ApproveReceipt(tripId, receiptId, reviewerId) | Participant | Expense | reviewer != submitter |
| C12 | RejectReceipt(tripId, receiptId, reviewerId, reason) | Participant | Expense | reviewer != submitter |
| C13 | ViewPartyAccount(tripId, partyTenantId) | Party-Mitglied | Read Model | Partei nimmt an Reise teil |
| C14 | CloseTripAccount(tripId) | Organizer | Expense | Reise soll final fixiert werden |

---

## 8. Aggregate und Read Models

### 8.1 Trips

Die Reise braucht eine explizitere Governance auf Reiseebene.

Moegliche fachliche Elemente:

```text
Trip
  +-- participants
  +-- stayPeriods
  +-- organizerIds
```

Wichtige Regel:

- Organizer ist nicht mehr nur der Ersteller der Reise.
- Organizer ist eine erweiterbare Menge von Teilnehmern der Reise.

### 8.2 Expense

Das `Expense`-Aggregat sollte fachlich als `TripAccount` gelesen werden.

```text
TripAccount (heute: Expense)
  +-- tripId
  +-- tenantId
  +-- status
  +-- partyAccounts: List<PartyAccount>
  +-- receipts
  +-- advances
  +-- accommodationTotalPrice
```

#### PartyAccount

```text
PartyAccount
  +-- partyTenantId
  +-- partyName
  +-- weightedParticipantShare
  +-- allocatedSharedCost
  +-- equalAdvanceCharge
  +-- advancePaidCredit
  +-- receiptCredits
  +-- currentBalance
```

Interpretation:

- `weightedParticipantShare`: Anteil der Reisepartei aus gewichteten Personen-/Aufenthaltsdaten
- `allocatedSharedCost`: der Reisepartei zugerechneter Anteil aus dem Gesamtreisekonto
- `equalAdvanceCharge`: gleicher Soll-Betrag fuer Anzahlungen
- `advancePaidCredit`: tatsaechlich geleistete Anzahlung
- `receiptCredits`: positive Gutschriften aus von dieser Partei geleisteten Einkaeufen
- `currentBalance`: Gesamtsaldo der Partei

### 8.3 Read Models

#### PartyAccountView

```text
PartyAccountView
  +-- tripId
  +-- partyTenantId
  +-- partyName
  +-- memberSummaries
  +-- allocatedSharedCost
  +-- costShareExplanation
  +-- advanceTarget
  +-- advancePaid
  +-- receiptCredits
  +-- currentBalance
  +-- status
```

#### MyTripCostView

```text
MyTripCostView
  +-- tripId
  +-- participantId
  +-- ownPartyTenantId
  +-- ownPartyName
  +-- ownPartyBalance
  +-- ownPartyReceiptCredits
  +-- ownPartyAllocatedSharedCost
  +-- ownPartyAdvanceStatus
```

#### TripAccountOverviewView

```text
TripAccountOverviewView
  +-- tripId
  +-- totalCostPool
  +-- accommodationTotalPrice
  +-- totalReceiptCredits
  +-- totalAdvancePaid
  +-- partyCount
  +-- status
```

---

## 9. Policies (Purple Stickies)

| Policy | Trigger | Reaktion |
|--------|---------|----------|
| P1 | `TripCreated` | gemeinsames Reisekonto anlegen |
| P2 | `ParticipantAddedToTrip` | Partei-Konto anlegen, falls fuer Partei noch keines existiert |
| P3 | `ParticipantRemovedFromTrip` | Partei-Anteile neu berechnen |
| P4 | `StayPeriodSetForParticipant` | gewichtete Teilnehmeranteile und globale Kostenverteilung neu berechnen |
| P5 | `AccommodationPriceSet` | Unterkunftskostenblock als negativen Betrag ins Gesamtreisekonto uebernehmen |
| P6 | `EqualAdvanceConfirmedForAllParties` | gleicher Anzahlungssollwert fuer alle Parteien setzen |
| P7 | `PartyAdvanceMarkedPaid` | Partei-Guthaben aktualisieren |
| P8 | `ReceiptSubmitted` und ggf. `ReceiptApproved` | negativen Betrag ins Gesamtreisekonto buchen und positive Gutschrift der leistenden Partei zuordnen |
| P9 | jede relevante Aenderung | `PartyBalanceRecomputed` |

---

## 10. Invarianten

### 10.1 Trips

| ID | Invariante | Durchgesetzt in |
|----|-----------|-----------------|
| T-INV-1 | Mitglieder einer Reisepartei duerfen nur Teilnehmer ihrer eigenen Partei hinzufuegen/entfernen | Trip Service / Authorization |
| T-INV-2 | Mitglieder einer Reisepartei duerfen nur StayPeriods der eigenen Partei setzen | Trip Service / Authorization |
| T-INV-3 | Organizer duerfen StayPeriods fuer alle setzen | Trip Service |
| T-INV-4 | Nur Organizer duerfen weitere Organizer ernennen | Trip Service |
| T-INV-5 | Ein Organizer muss selbst Teilnehmer der Reise sein | Trip Domain/Service |

### 10.2 Expense

| ID | Invariante | Durchgesetzt in |
|----|-----------|-----------------|
| E-INV-1 | Pro Reise existiert genau ein gemeinsames Reisekonto | Repository / Service |
| E-INV-2 | Pro Reisepartei existiert genau ein Partei-Konto pro Reise | Aggregate |
| E-INV-3 | Anzahlungen sind fuer alle Parteien gleich hoch | Expense Domain |
| E-INV-4 | Unterkunftskosten werden erst im Gesamtreisekonto gebucht und dann ueber gewichtete Teilnehmeranteile den Partei-Konten zugerechnet | Expense Domain Service |
| E-INV-5 | Einkaufsbelege werden als negativer Betrag im Gesamtreisekonto und zugleich als positive Gutschrift fuer die leistende Reisepartei verbucht | Expense Domain |
| E-INV-6 | Transferlisten zwischen Einzelpersonen sind kein primaeres Ergebnis der Berechnung | Representation / UX |
| E-INV-7 | Review nur 4-Augen, nicht zwingend Organizer-basiert | Receipt Domain |

---

## 11. Hot Spots (Red Stickies)

### H1: Partei-Zugriffsrechte in der Reiseverwaltung

Die neue Regel ist deutlich offener als klassische Organizer-only-Verwaltung.

Konsequenz:

- Die Autorisierung muss sauber zwischen
  - eigene Partei
  - alle Parteien
  unterscheiden.

### H2: Was ist "fair" bei Unterkunftskosten?

Die Anforderung nennt:

- `StayPeriod`
- Gewichtung der Teilnehmer
- Partei-Ebene

Das ist die fachlich fuehrende Regel fuer den Unterkunftsanteil:

- Unterkunft geht als Kostenblock ins Gesamtreisekonto
- Verrechnung erfolgt ueber gewichtete Teilnehmeranteile und StayPeriods
- erst danach entsteht der angerechnete Partei-Anteil

Nur die Anzahlung ist der explizite Fall, der **wirklich gleich** pro Partei verteilt wird.

### H3: Receipt-Credit Zeitpunkte

Wenn Review optional ist:

- Wird das Guthaben sofort beim Einreichen gebucht?
- Oder erst nach Approval?

MVP-Empfehlung:

- zwei Werte ausweisen:
  - `gebuchte Gutschriften`
  - `offene Belege`

So bleibt das Konto nachvollziehbar.

### H4: Reise-Organizer in welchem Context?

Die Regel "Organizer koennen weitere Organizer machen" gehoert fachlich eher zur Reise als zur allgemeinen Tenant-Rolle.

Empfehlung:

- Reise-Organizer zunaechst im Trips-Kontext modellieren
- nicht die gesamte IAM-Rollenlogik als Voraussetzung erzwingen

### H5: Keine Teilnehmer-Transferliste mehr

Das ist ein echter Modellwechsel.

Konsequenz:

- bestehende Views fuer `partyTransfers()` / `transfers()` sind nicht mehr die fachliche Hauptsicht
- die Hauptsicht wird `PartyAccountView`

---

## 12. Empfohlene Story-Scheiben fuer die Iteration

### S12-A: Teilnehmer der eigenen Reisepartei auf Reise verwalten

**Outcome**:
- Mitglieder koennen weitere eigene Mitglieder/Kinder zur Reise hinzufuegen
- Mitglieder koennen eigene Teilnehmer entfernen

### S12-B: StayPeriods flexibel pflegen

**Outcome**:
- Mitglieder pflegen StayPeriods der eigenen Partei
- Organizer pflegen StayPeriods aller Teilnehmer

### S12-C: Reise-Organizer erweitern

**Outcome**:
- Organizer koennen weitere Reise-Organizer ernennen

### S12-D: Gemeinsames Reisekonto ab aktueller Reise

**Outcome**:
- Abrechnung ist waehrend der Reise sichtbar
- Konto existiert vor Reiseende

### S12-E: Partei-Konto als Hauptsicht

**Outcome**:
- jede Reisepartei sieht Belastungen, Anzahlungen, Beleg-Gutschriften und aktuellen Stand
- keine primaere Transferdarstellung zwischen Einzelpersonen

### S12-F: Unterkunft + Anzahlungen + Einkaufsbelege in einem Modell

**Outcome**:
- Unterkunftskosten zuerst ins Gesamtreisekonto, dann gewichtete Verrechnung auf Partei-Konten
- Anzahlung gleich pro Partei
- Einkaufsbelege als negativer Gesamtkonto-Eintrag plus positives Partei-Guthaben

---

## 13. Offene Designfragen fuer das Team

1. Soll `GrantTripOrganizer` rein im Trips-SCS modelliert werden oder an IAM gespiegelt werden?
2. Sollen eingereichte Belege sofort als vorlaeufiges Guthaben auftauchen oder erst nach Review?
3. Duerfen Mitglieder der Reisepartei auch erwachsene Mitglieder anderer Parteien zur Reise einladen oder strikt nur die eigene Partei verwalten?
4. Wie exakt lautet die Formel fuer die globale Kostenverrechnung:
   - Summe gewichteter Personen-Naechte pro Partei / Summe gewichteter Personen-Naechte gesamt?
   - gilt diese Formel nur fuer Unterkunft oder fuer weitere gemeinsame Kostenblocke?
5. Sollen Einzelpersonen weiterhin einen Detailblick erhalten, obwohl die Hauptabrechnung parteibasiert ist?

---

## 14. Empfohlene Iterationsreihenfolge

| Order | Story | Rationale |
|-------|-------|-----------|
| 1 | S12-A | reale Teilnehmerpflege ist Grundlage |
| 2 | S12-B | StayPeriods sind Input fuer Kosten |
| 3 | S12-D | laufendes Konto muss frueh existieren |
| 4 | S12-E | Hauptsicht fachlich umstellen |
| 5 | S12-F | Kostenregeln konsolidieren |
| 6 | S12-C | Organizer-Erweiterung danach oder parallel |

---

## 15. Empfehlung

Die naechste Iteration sollte als Umbau auf dieses Modell geplant werden:

1. **Reiseverwaltung auf Partei-Ebene flexibilisieren**
2. **StayPeriods als laufend pflegbare Kostengrundlage behandeln**
3. **Expense als gemeinsames Reisekonto mit Partei-Konten modellieren**
4. **Unterkunft und Einkaufsbelege erst im Gesamtreisekonto buchen und danach sauber auf Partei-Salden verrechnen**
5. **Anzahlungen bewusst als vereinfachte Gleichverteilung pro Reisepartei behandeln**
6. **Transferlisten zwischen Einzelpersonen aus der Hauptsicht entfernen**

Das entspricht der beschriebenen Domaene deutlich besser als eine klassische Endabrechnung mit Schuldner-Glaeubiger-Transfers.
