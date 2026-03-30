# language: de
Funktionalität: Reiseplanung
  Als Teilnehmer einer Reise
  moechte ich eine Planungsuebersicht sehen,
  damit ich den Fortschritt aller Abstimmungen auf einen Blick verfolgen kann.

  Hintergrund:
    Angenommen ich bin als Mitglied einer Reisepartei eingeloggt
    Und es existiert eine Reise fuer die Planungsuebersicht

  # S14-F: Planungsuebersicht
  Szenario: Planungsuebersicht ohne Abstimmungen
    Wenn ich die Planungsseite der Reise oeffne
    Dann sehe ich den Status "Nicht gestartet" fuer beide Abstimmungen

  Szenario: Planungsuebersicht beschreibt beide demokratischen Entscheidungen
    Wenn ich die Planungsseite der Reise oeffne
    Dann sehe ich die neue Planungsueberschrift mit Ruecksprung zur Reise
    Und der Untertitel erwaehnt Reisezeitraum und Unterkunft

  Szenario: I18n-Aufloesung auf der Planungsseite
    Wenn ich die Planungsseite der Reise oeffne
    Dann enthaelt die Seite keine unaufgeloesten Message-Keys "??"
