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

  # S15-B: Buchungs-Workflow (select → AWAITING_BOOKING)
  Szenario: Kandidat auswaehlen setzt Buchung-ausstehend-Status
    Angenommen eine Unterkunftsabstimmung wurde fuer die Reise erstellt
    Wenn ich den ersten Kandidaten auswaehle
    Dann sehe ich den Status "Buchung ausstehend"
    Und ich sehe die Buchungsaktionen

  # S15-C: Buchungsfehlschlag und Wiederherstellung
  Szenario: Buchungsfehlschlag kehrt zur offenen Abstimmung zurueck
    Angenommen eine Unterkunftsabstimmung wurde fuer die Reise erstellt
    Und ich den ersten Kandidaten auswaehle
    Wenn ich die Buchung als fehlgeschlagen mit Notiz "Ausgebucht" markiere
    Dann sehe ich die Unterkunftsabstimmung mit Status "Offen"
    Und ich sehe den Fehlschlag-Hinweis

  # S15-B: Buchungserfolg (AWAITING_BOOKING → BOOKED)
  Szenario: Buchungserfolg setzt Gebucht-Status und zeigt Gewinnerbanner
    Angenommen eine Unterkunftsabstimmung wurde fuer die Reise erstellt
    Und ich den ersten Kandidaten auswaehle
    Wenn ich die Buchung als erfolgreich markiere
    Dann sehe ich den Gewinnerbanner mit der ersten Unterkunft

  # S14-D: I18n-Aufloesung
  Szenario: I18n-Aufloesung auf der Unterkunftsabstimmungsseite
    Angenommen eine Unterkunftsabstimmung wurde fuer die Reise erstellt
    Dann enthaelt die Seite keine unaufgeloesten Message-Keys "??"
