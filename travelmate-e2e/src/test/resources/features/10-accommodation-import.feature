# language: de
Funktionalität: Unterkunft per URL importieren
  Als Organisator einer Reise
  möchte ich eine Ferienhaus-URL in die Unterkunftsabstimmung eingeben und die Vorschlagsdaten automatisch extrahieren lassen,
  damit ich Unterkünfte nicht manuell als Poll-Vorschlag anlegen muss.

  Hintergrund:
    Angenommen ich bin als Mitglied einer Reisepartei eingeloggt
    Und es existiert eine Reise für den Unterkunfts-Import-Test
    Und eine Unterkunftsabstimmung für den Import-Test wurde erstellt

  # S14-D: Unterkunftsvorschlag per URL importieren
  Szenario: Import-Bereich auf der Unterkunftsabstimmungsseite sichtbar
    Wenn ich die Unterkunftsabstimmungsseite für den Import-Test öffne
    Dann sehe ich einen Bereich zum URL-Import

  @manuell
  Szenario: Unterkunftsvorschlag per URL importieren und bearbeiten
    Angenommen ich befinde mich auf der Unterkunftsabstimmungsseite mit offener Abstimmung
    Wenn ich eine URL eingebe und auf Importieren klicke
    Dann sehe ich ein vorausgefülltes Formular mit den erkannten Daten
    Und ich kann die Daten korrigieren und speichern

  @manuell
  Szenario: Ungültige URL zeigt Fehlermeldung
    Angenommen ich befinde mich auf der Unterkunftsabstimmungsseite mit offener Abstimmung
    Wenn ich eine ungültige URL eingebe und auf Importieren klicke
    Dann sehe ich eine Fehlermeldung zum Import

  Szenario: I18n-Auflösung auf der Import-Seite
    Wenn ich die Unterkunftsabstimmungsseite für den Import-Test öffne
    Dann enthält die Seite keine unaufgelösten Message-Keys "??"
