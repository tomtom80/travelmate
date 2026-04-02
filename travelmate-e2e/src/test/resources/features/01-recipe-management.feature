# language: de
Funktionalität: Rezeptverwaltung
  Als Mitglied einer Reisepartei
  möchte ich Rezepte erstellen und verwalten,
  damit ich sie später meinem Essensplan zuweisen kann.

  Hintergrund:
    Angenommen ich bin als Mitglied einer Reisepartei eingeloggt
    Und ich befinde mich auf der Rezeptseite

  # S7-A: Rezept manuell erstellen (US-TRIPS-040)
  Szenario: Rezeptliste ist initial leer
    Dann sehe ich die Meldung "Noch keine Rezepte vorhanden"

  Szenario: Rezeptformular zeigt alle Felder
    Wenn ich auf "Neues Rezept" klicke
    Dann sehe ich ein Formular mit den Feldern Name, Portionen und Zutaten
    Und es gibt eine leere Zutatenzeile
    Und es gibt einen Button "Zutat hinzufügen"

  Szenario: Rezept mit Zutaten erstellen
    Wenn ich auf "Neues Rezept" klicke
    Und ich den Namen "Spaghetti Bolognese" eingebe
    Und ich die Portionen auf "4" setze
    Und ich die Zutat "Spaghetti" mit Menge "500" und Einheit "g" eingebe
    Und ich eine weitere Zutat "Hackfleisch" mit Menge "400" und Einheit "g" hinzufüge
    Und ich das Formular absende
    Dann werde ich zur Rezeptliste weitergeleitet
    Und ich sehe "Spaghetti Bolognese" in der Liste

  Szenario: Erstelles Rezept zeigt korrekte Details in der Liste
    Dann sehe ich in der Rezepttabelle den Namen "Spaghetti Bolognese"
    Und die Portionenzahl "4"
    Und die Zutatenzahl "2"
    Und Buttons zum Bearbeiten und Löschen

  # S7-B: Rezepte bearbeiten (US-TRIPS-042)
  Szenario: Rezept bearbeiten
    Wenn ich beim Rezept "Spaghetti Bolognese" auf "Bearbeiten" klicke
    Dann sehe ich das Formular mit den vorausgefüllten Werten
    Wenn ich den Namen auf "Pasta Bolognese" ändere
    Und ich die Portionen auf "6" setze
    Und ich das Formular absende
    Dann sehe ich "Pasta Bolognese" in der Rezeptliste
    Und die Portionenzahl zeigt "6"

  # S7-C: Rezept löschen (US-TRIPS-043)
  Szenario: Rezept löschen
    Wenn ich beim Rezept "Pasta Bolognese" auf "Löschen" klicke
    Und ich die Löschbestätigung akzeptiere
    Dann wird das Rezept aus der Liste entfernt

  # S7-B: Rezeptliste (US-TRIPS-044)
  Szenario: Navigation zur Rezeptseite über die Navigationsleiste
    Angenommen ich befinde mich auf einer beliebigen Seite
    Wenn ich in der Navigation auf "Rezepte" klicke
    Dann werde ich zur Rezeptliste weitergeleitet

  Szenario: Zutat dynamisch hinzufügen
    Wenn ich auf "Neues Rezept" klicke
    Und ich auf "Zutat hinzufügen" klicke
    Dann sehe ich eine zusätzliche Zutatenzeile

  Szenario: I18n-Auflösung auf der Rezeptseite
    Dann enthält die Seite keine unaufgelösten Message-Keys "??"
