# language: de
Funktionalität: Reiseplanung
  Als Teilnehmer einer Reise
  möchte ich eine Planungsübersicht sehen,
  damit ich den Fortschritt aller Abstimmungen auf einen Blick verfolgen kann.

  Hintergrund:
    Angenommen ich bin als Mitglied einer Reisepartei eingeloggt
    Und es existiert eine Reise für die Planungsübersicht

  # S14-F: Planungsübersicht
  Szenario: Planungsübersicht ohne Abstimmungen
    Wenn ich die Planungsseite der Reise öffne
    Dann sehe ich den Status "Nicht gestartet" für beide Abstimmungen

  Szenario: Planungsübersicht beschreibt beide demokratischen Entscheidungen
    Wenn ich die Planungsseite der Reise öffne
    Dann sehe ich die neue Planungsüberschrift mit Rücksprung zur Reise
    Und der Untertitel erwähnt Reisezeitraum und Unterkunft

  Szenario: I18n-Auflösung auf der Planungsseite
    Wenn ich die Planungsseite der Reise öffne
    Dann enthält die Seite keine unaufgelösten Message-Keys "??"
