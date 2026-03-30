# language: de
Funktionalität: Unterkunftsabstimmung
  Als Organisator einer Reise
  moechte ich eine Unterkunftsabstimmung erstellen,
  damit alle Teilnehmer gemeinsam ueber die Unterkunft abstimmen koennen.

  Hintergrund:
    Angenommen ich bin als Mitglied einer Reisepartei eingeloggt
    Und es existiert eine Reise fuer die Unterkunftsabstimmung

  # S14-D: Unterkunftsabstimmung erstellen
  Szenario: Leere Unterkunftsabstimmungsseite anzeigen
    Wenn ich die Unterkunftsabstimmungsseite der Reise oeffne
    Dann sehe ich den Hinweis dass noch keine Unterkunftsabstimmung vorhanden ist

  Szenario: Unterkunftsabstimmung erstellen
    Wenn ich die Unterkunftsabstimmungsseite der Reise oeffne
    Und ich eine Unterkunftsabstimmung mit zwei Vorschlaegen erstelle
    Dann sehe ich die Unterkunftsabstimmung mit Status "Offen"
    Und die Abstimmung hat 2 Unterkunftsvorschlaege

  # S14-E: Abstimmen
  Szenario: Fuer eine Unterkunft abstimmen
    Angenommen eine Unterkunftsabstimmung wurde fuer die Reise erstellt
    Wenn ich fuer die erste Unterkunft abstimme
    Dann hat die erste Unterkunft mindestens 1 Stimme

  # S14-D: I18n-Aufloesung
  Szenario: I18n-Aufloesung auf der Unterkunftsabstimmungsseite
    Angenommen eine Unterkunftsabstimmung wurde fuer die Reise erstellt
    Dann enthaelt die Seite keine unaufgeloesten Message-Keys "??"
