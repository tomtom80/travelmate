# language: de
Funktionalität: Unterkunft
  Als Organisator einer Reise
  moechte ich eine Unterkunft mit Zimmern erfassen,
  damit alle Teilnehmer die Unterkunftsdetails und Zimmerbelegung sehen.

  Hintergrund:
    Angenommen ich bin als Mitglied einer Reisepartei eingeloggt
    Und es existiert eine Reise fuer die Unterkunftsverwaltung

  # S9-A: Unterkunft erfassen (US-TRIPS-060)
  Szenario: Leere Unterkunftsseite anzeigen
    Wenn ich die Unterkunftsseite der Reise oeffne
    Dann sehe ich den Hinweis dass noch keine Unterkunft hinterlegt ist

  Szenario: Unterkunft erfassen
    Wenn ich die Unterkunftsseite der Reise oeffne
    Und ich eine Unterkunft mit Name "Berghuette" und Adresse "Alpweg 7" und Gesamtpreis "1800" erfasse
    Dann sehe ich die Unterkunftsdetails mit Name "Berghuette"

  # S9-A: Zimmer hinzufuegen
  Szenario: Zimmer hinzufuegen
    Angenommen eine Unterkunft wurde fuer die Reise erfasst
    Wenn ich ein Zimmer mit Name "Familienzimmer" und Typ "QUAD" und Betten "4" hinzufuege
    Dann wird das Zimmer "Familienzimmer" in der Zimmerliste angezeigt

  # S9-A: I18n-Aufloesung
  Szenario: I18n-Aufloesung auf der Unterkunftsseite
    Angenommen eine Unterkunft wurde fuer die Reise erfasst
    Dann enthaelt die Seite keine unaufgeloesten Message-Keys "??"
