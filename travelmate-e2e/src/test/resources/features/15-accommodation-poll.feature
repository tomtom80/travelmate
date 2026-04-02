# language: de
Funktionalität: Unterkunftsabstimmung
  Als Organisator einer Reise
  möchte ich eine Unterkunftsabstimmung erstellen,
  damit alle Teilnehmer gemeinsam über die Unterkunft abstimmen können.

  Hintergrund:
    Angenommen ich bin als Mitglied einer Reisepartei eingeloggt
    Und es existiert eine Reise für die Unterkunftsabstimmung

  # S14-D: Unterkunftsabstimmung erstellen
  Szenario: Leere Unterkunftsabstimmungsseite anzeigen
    Wenn ich die Unterkunftsabstimmungsseite der Reise öffne
    Dann sehe ich den Hinweis dass noch keine Unterkunftsabstimmung vorhanden ist

  Szenario: Unterkunftsabstimmung erstellen
    Wenn ich die Unterkunftsabstimmungsseite der Reise öffne
    Und ich eine Unterkunftsabstimmung mit zwei Vorschlägen erstelle
    Dann sehe ich die Unterkunftsabstimmung mit Status "Offen"
    Und die Abstimmung hat 2 Unterkunftsvorschläge

  # S14-E: Abstimmen
  Szenario: Für eine Unterkunft abstimmen
    Angenommen eine Unterkunftsabstimmung wurde für die Reise erstellt
    Wenn ich für die erste Unterkunft abstimme
    Dann hat die erste Unterkunft mindestens 1 Stimme

  # S15-B: Buchungs-Workflow (select → AWAITING_BOOKING)
  Szenario: Kandidat auswählen setzt Buchung-ausstehend-Status
    Angenommen eine Unterkunftsabstimmung wurde für die Reise erstellt
    Wenn ich den ersten Kandidaten auswähle
    Dann sehe ich den Status "Buchung ausstehend"
    Und ich sehe die Buchungsaktionen

  # S15-C: Buchungsfehlschlag und Wiederherstellung
  Szenario: Buchungsfehlschlag kehrt zur offenen Abstimmung zurück
    Angenommen eine Unterkunftsabstimmung wurde für die Reise erstellt
    Und ich den ersten Kandidaten auswähle
    Wenn ich die Buchung als fehlgeschlagen mit Notiz "Ausgebucht" markiere
    Dann sehe ich die Unterkunftsabstimmung mit Status "Offen"
    Und ich sehe den Fehlschlag-Hinweis

  # S15-B: Buchungserfolg (AWAITING_BOOKING → BOOKED)
  Szenario: Buchungserfolg setzt Gebucht-Status und zeigt Gewinnerbanner
    Angenommen eine Unterkunftsabstimmung wurde für die Reise erstellt
    Und ich den ersten Kandidaten auswähle
    Wenn ich die Buchung als erfolgreich markiere
    Dann sehe ich den Gewinnerbanner mit der ersten Unterkunft

  # S14-D: I18n-Auflösung
  Szenario: I18n-Auflösung auf der Unterkunftsabstimmungsseite
    Angenommen eine Unterkunftsabstimmung wurde für die Reise erstellt
    Dann enthält die Seite keine unaufgelösten Message-Keys "??"
