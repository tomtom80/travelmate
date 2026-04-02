# language: de
Funktionalität: Unterkunft
  Als Organisator einer Reise
  möchte ich die demokratisch gewählte Unterkunft verwalten,
  damit alle Teilnehmer die Unterkunftsdetails und Zimmerbelegung sehen.

  Hintergrund:
    Angenommen ich bin als Mitglied einer Reisepartei eingeloggt
    Und es existiert eine Reise für die Unterkunftsverwaltung

  # S9-A: Unterkunft erfassen (US-TRIPS-060)
  Szenario: Unterkunftsseite verweist ohne Entscheidung auf die Planung
    Wenn ich die Unterkunftsseite der Reise öffne
    Dann sehe ich den Hinweis dass zuerst eine Unterkunftsabstimmung nötig ist

  Szenario: Gewinner der Unterkunftsabstimmung wird übernommen
    Wenn ich die Unterkunftsseite der Reise öffne
    Und ich die Unterkunftsabstimmung mit Gewinner "Berghütte" bestätige
    Dann sehe ich die Unterkunftsdetails mit Name "Berghütte"

  # S9-A: Zimmer hinzufügen
  Szenario: Zimmer hinzufügen
    Angenommen eine Unterkunft wurde für die Reise erfasst
    Wenn ich ein Zimmer mit Name "Familienzimmer" und Betten "4" hinzufüge
    Dann wird das Zimmer "Familienzimmer" in der Zimmerliste angezeigt

  # S9-A: I18n-Auflösung
  Szenario: I18n-Auflösung auf der Unterkunftsseite
    Angenommen eine Unterkunft wurde für die Reise erfasst
    Dann enthält die Seite keine unaufgelösten Message-Keys "??"
