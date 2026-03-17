# language: de
Funktionalität: Essensplan
  Als Organisator einer Reise
  moechte ich einen Essensplan fuer meine Reise erstellen und verwalten,
  damit die Mahlzeiten fuer jeden Tag geplant sind.

  Hintergrund:
    Angenommen ich bin als Mitglied einer Reisepartei eingeloggt
    Und es existiert eine Reise mit Startdatum und Enddatum

  # S7-D: Essensplan erstellen (US-TRIPS-030)
  Szenario: Essensplan-Button auf der Reisedetailseite
    Wenn ich die Reisedetailseite oeffne
    Dann sehe ich einen Button "Essensplan erstellen"

  Szenario: Essensplan generieren
    Wenn ich auf der Reisedetailseite "Essensplan erstellen" klicke
    Dann werde ich zur Essensplan-Uebersicht weitergeleitet
    Und ich sehe ein Tagesraster mit den Spalten Fruehstueck, Mittagessen und Abendessen
    Und fuer jeden Reisetag existiert eine Zeile

  Szenario: Essensplan-Button wechselt nach Erstellung zu "Anzeigen"
    Angenommen ein Essensplan wurde fuer die Reise erstellt
    Wenn ich die Reisedetailseite oeffne
    Dann sehe ich den Link "Essensplan anzeigen" statt "Essensplan erstellen"

  # S7-E: MealSlot als SKIP markieren (US-TRIPS-031)
  Szenario: Mahlzeit als SKIP markieren
    Angenommen ich befinde mich auf der Essensplan-Uebersicht
    Wenn ich den Status eines Mahlzeit-Slots auf "Auslassen" aendere
    Dann wird der Slot als "Auslassen" angezeigt

  # S7-E: MealSlot als EATING_OUT markieren (US-TRIPS-032)
  Szenario: Mahlzeit als EATING_OUT markieren
    Angenommen ich befinde mich auf der Essensplan-Uebersicht
    Wenn ich den Status eines Mahlzeit-Slots auf "Auswaerts essen" aendere
    Dann wird der Slot als "Auswaerts essen" angezeigt

  Szenario: Status zurueck auf PLANNED setzen
    Angenommen ein Mahlzeit-Slot hat den Status "Auslassen"
    Wenn ich den Status auf "Geplant" zuruecksetze
    Dann wird der Slot als "Geplant" angezeigt
    Und ich sehe eine Rezeptauswahl fuer diesen Slot

  # S7-F: Rezept einem MealSlot zuweisen (US-TRIPS-033)
  Szenario: Rezept einem Mahlzeit-Slot zuweisen
    Angenommen ich befinde mich auf der Essensplan-Uebersicht
    Und es existiert ein Rezept in meiner Rezeptsammlung
    Wenn ich fuer einen geplanten Slot ein Rezept auswaehle
    Dann wird der Rezeptname im Slot angezeigt

  # S7-G: Essensplan-Uebersicht (US-TRIPS-034)
  Szenario: Essensplan-Uebersicht zeigt korrekte Struktur
    Angenommen ich befinde mich auf der Essensplan-Uebersicht
    Dann sehe ich eine Tabelle mit den Spalten Datum, Fruehstueck, Mittagessen, Abendessen
    Und jeder Slot hat ein Status-Dropdown
    Und geplante Slots haben eine Rezeptauswahl

  Szenario: Navigation zurueck zur Reisedetailseite
    Angenommen ich befinde mich auf der Essensplan-Uebersicht
    Wenn ich auf "Zurueck" klicke
    Dann werde ich zur Reisedetailseite weitergeleitet

  Szenario: I18n-Aufloesung auf der Essensplan-Seite
    Angenommen ich befinde mich auf der Essensplan-Uebersicht
    Dann enthaelt die Seite keine unaufgeloesten Message-Keys "??"
