# language: de
Funktionalität: Essensplan
  Als Organisator einer Reise
  möchte ich einen Essensplan für meine Reise erstellen und verwalten,
  damit die Mahlzeiten für jeden Tag geplant sind.

  Hintergrund:
    Angenommen ich bin als Mitglied einer Reisepartei eingeloggt
    Und es existiert eine Reise mit Startdatum und Enddatum

  # S7-D: Essensplan erstellen (US-TRIPS-030)
  Szenario: Essensplan-Button auf der Reisedetailseite
    Wenn ich die Reisedetailseite öffne
    Dann sehe ich einen Button "Essensplan erstellen"

  Szenario: Essensplan generieren
    Wenn ich auf der Reisedetailseite "Essensplan erstellen" klicke
    Dann werde ich zur Essensplan-Übersicht weitergeleitet
    Und ich sehe ein Tagesraster mit den Spalten Frühstück, Mittagessen und Abendessen
    Und für jeden Reisetag existiert eine Zeile

  Szenario: Essensplan-Button wechselt nach Erstellung zu "Anzeigen"
    Angenommen ein Essensplan wurde für die Reise erstellt
    Wenn ich die Reisedetailseite öffne
    Dann sehe ich den Link "Essensplan anzeigen" statt "Essensplan erstellen"

  # S7-E: MealSlot als SKIP markieren (US-TRIPS-031)
  Szenario: Mahlzeit als SKIP markieren
    Angenommen ich befinde mich auf der Essensplan-Übersicht
    Wenn ich den Status eines Mahlzeit-Slots auf "Auslassen" ändere
    Dann wird der Slot als "Auslassen" angezeigt

  # S7-E: MealSlot als EATING_OUT markieren (US-TRIPS-032)
  Szenario: Mahlzeit als EATING_OUT markieren
    Angenommen ich befinde mich auf der Essensplan-Übersicht
    Wenn ich den Status eines Mahlzeit-Slots auf "Auswärts essen" ändere
    Dann wird der Slot als "Auswärts essen" angezeigt

  Szenario: Status zurück auf PLANNED setzen
    Angenommen ein Mahlzeit-Slot hat den Status "Auslassen"
    Wenn ich den Status auf "Geplant" zurücksetze
    Dann wird der Slot als "Geplant" angezeigt
    Und ich sehe eine Rezeptauswahl für diesen Slot

  # S7-F: Rezept einem MealSlot zuweisen (US-TRIPS-033)
  Szenario: Rezept einem Mahlzeit-Slot zuweisen
    Angenommen ich befinde mich auf der Essensplan-Übersicht
    Und es existiert ein Rezept in meiner Rezeptsammlung
    Wenn ich für einen geplanten Slot ein Rezept auswähle
    Dann wird der Rezeptname im Slot angezeigt

  # S7-G: Essensplan-Übersicht (US-TRIPS-034)
  Szenario: Essensplan-Übersicht zeigt korrekte Struktur
    Angenommen ich befinde mich auf der Essensplan-Übersicht
    Dann sehe ich eine Tabelle mit den Spalten Datum, Frühstück, Mittagessen, Abendessen
    Und jeder Slot hat ein Status-Dropdown
    Und geplante Slots haben eine Rezeptauswahl

  Szenario: Navigation zurück zur Reisedetailseite
    Angenommen ich befinde mich auf der Essensplan-Übersicht
    Wenn ich auf "Zurück" klicke
    Dann werde ich zur Reisedetailseite weitergeleitet

  Szenario: I18n-Auflösung auf der Essensplan-Seite
    Angenommen ich befinde mich auf der Essensplan-Übersicht
    Dann enthält die Seite keine unaufgelösten Message-Keys "??"
