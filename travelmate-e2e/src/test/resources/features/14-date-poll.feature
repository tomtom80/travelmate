# language: de
Funktionalität: Terminabstimmung
  Als Organisator einer Reise
  moechte ich eine Terminabstimmung erstellen,
  damit alle Teilnehmer gemeinsam ueber den Reisezeitraum abstimmen koennen.

  Hintergrund:
    Angenommen ich bin als Mitglied einer Reisepartei eingeloggt
    Und es existiert eine Reise fuer die Terminabstimmung

  # S14-A: Terminabstimmung erstellen
  Szenario: Leere Terminabstimmungsseite anzeigen
    Wenn ich die Terminabstimmungsseite der Reise oeffne
    Dann sehe ich den Hinweis dass noch keine Terminabstimmung vorhanden ist

  Szenario: Terminabstimmung erstellen
    Wenn ich die Terminabstimmungsseite der Reise oeffne
    Und ich eine Terminabstimmung mit zwei Zeitraeumen erstelle
    Dann sehe ich die Terminabstimmung mit Status "Offen"
    Und die Abstimmung hat 2 Zeitraumoptionen

  # S14-B: Abstimmen
  Szenario: Fuer einen Zeitraum abstimmen
    Angenommen eine Terminabstimmung wurde fuer die Reise erstellt
    Wenn ich fuer den ersten Zeitraum abstimme
    Dann hat der erste Zeitraum mindestens 1 Stimme

  # S14-A: I18n-Aufloesung
  Szenario: I18n-Aufloesung auf der Terminabstimmungsseite
    Angenommen eine Terminabstimmung wurde fuer die Reise erstellt
    Dann enthaelt die Seite keine unaufgeloesten Message-Keys "??"
