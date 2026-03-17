# language: de
Funktionalität: Rezeptverwaltung
  Als Mitglied einer Reisepartei
  moechte ich Rezepte erstellen und verwalten,
  damit ich sie spaeter meinem Essensplan zuweisen kann.

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
    Und es gibt einen Button "Zutat hinzufuegen"

  Szenario: Rezept mit Zutaten erstellen
    Wenn ich auf "Neues Rezept" klicke
    Und ich den Namen "Spaghetti Bolognese" eingebe
    Und ich die Portionen auf "4" setze
    Und ich die Zutat "Spaghetti" mit Menge "500" und Einheit "g" eingebe
    Und ich eine weitere Zutat "Hackfleisch" mit Menge "400" und Einheit "g" hinzufuege
    Und ich das Formular absende
    Dann werde ich zur Rezeptliste weitergeleitet
    Und ich sehe "Spaghetti Bolognese" in der Liste

  Szenario: Erstelles Rezept zeigt korrekte Details in der Liste
    Dann sehe ich in der Rezepttabelle den Namen "Spaghetti Bolognese"
    Und die Portionenzahl "4"
    Und die Zutatenzahl "2"
    Und Buttons zum Bearbeiten und Loeschen

  # S7-B: Rezepte bearbeiten (US-TRIPS-042)
  Szenario: Rezept bearbeiten
    Wenn ich beim Rezept "Spaghetti Bolognese" auf "Bearbeiten" klicke
    Dann sehe ich das Formular mit den vorausgefuellten Werten
    Wenn ich den Namen auf "Pasta Bolognese" aendere
    Und ich die Portionen auf "6" setze
    Und ich das Formular absende
    Dann sehe ich "Pasta Bolognese" in der Rezeptliste
    Und die Portionenzahl zeigt "6"

  # S7-C: Rezept loeschen (US-TRIPS-043)
  Szenario: Rezept loeschen
    Wenn ich beim Rezept "Pasta Bolognese" auf "Loeschen" klicke
    Und ich die Loeschbestaetigung akzeptiere
    Dann wird das Rezept aus der Liste entfernt

  # S7-B: Rezeptliste (US-TRIPS-044)
  Szenario: Navigation zur Rezeptseite ueber die Navigationsleiste
    Angenommen ich befinde mich auf einer beliebigen Seite
    Wenn ich in der Navigation auf "Rezepte" klicke
    Dann werde ich zur Rezeptliste weitergeleitet

  Szenario: Zutat dynamisch hinzufuegen
    Wenn ich auf "Neues Rezept" klicke
    Und ich auf "Zutat hinzufuegen" klicke
    Dann sehe ich eine zusaetzliche Zutatenzeile

  Szenario: I18n-Aufloesung auf der Rezeptseite
    Dann enthaelt die Seite keine unaufgeloesten Message-Keys "??"
