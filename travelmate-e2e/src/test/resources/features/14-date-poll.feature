# language: de
Funktionalität: Terminabstimmung
  Als Organisator einer Reise
  möchte ich eine Terminabstimmung erstellen,
  damit alle Teilnehmer gemeinsam über den Reisezeitraum abstimmen können.

  Hintergrund:
    Angenommen ich bin als Mitglied einer Reisepartei eingeloggt
    Und es existiert eine Reise für die Terminabstimmung

  # S14-A: Terminabstimmung erstellen
  Szenario: Leere Terminabstimmungsseite anzeigen
    Wenn ich die Terminabstimmungsseite der Reise öffne
    Dann sehe ich den Hinweis dass noch keine Terminabstimmung vorhanden ist

  Szenario: Terminabstimmung erstellen
    Wenn ich die Terminabstimmungsseite der Reise öffne
    Und ich eine Terminabstimmung mit zwei Zeiträumen erstelle
    Dann sehe ich die Terminabstimmung mit Status "Offen"
    Und die Abstimmung hat 2 Zeitraumoptionen

  # S14-B: Abstimmen
  Szenario: Für einen Zeitraum abstimmen
    Angenommen eine Terminabstimmung wurde für die Reise erstellt
    Wenn ich für den ersten Zeitraum abstimme
    Dann hat der erste Zeitraum mindestens 1 Stimme

  # S14-A: I18n-Auflösung
  Szenario: I18n-Auflösung auf der Terminabstimmungsseite
    Angenommen eine Terminabstimmung wurde für die Reise erstellt
    Dann enthält die Seite keine unaufgelösten Message-Keys "??"
