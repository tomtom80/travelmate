# language: de
Funktionalität: Unterkunft per URL importieren
  Als Organisator einer Reise
  moechte ich eine Ferienhaus-URL eingeben und die Unterkunftsdaten automatisch extrahieren lassen,
  damit ich die Unterkunft nicht manuell eingeben muss.

  Hintergrund:
    Angenommen ich bin als Mitglied einer Reisepartei eingeloggt
    Und es existiert eine Reise fuer den Unterkunfts-Import-Test

  # S10-A: Unterkunft per URL importieren
  Szenario: Import-Bereich auf der Unterkunftsseite sichtbar
    Wenn ich die Unterkunftsseite fuer den Import-Test oeffne
    Dann sehe ich einen Bereich zum URL-Import

  Szenario: Unterkunft per URL importieren und bearbeiten
    Angenommen ich befinde mich auf der Unterkunftsseite ohne Unterkunft
    Wenn ich eine URL eingebe und auf Importieren klicke
    Dann sehe ich ein vorausgefuelltes Formular mit den erkannten Daten
    Und ich kann die Daten korrigieren und speichern

  @manuell
  Szenario: Ungueltige URL zeigt Fehlermeldung
    Angenommen ich befinde mich auf der Unterkunftsseite ohne Unterkunft
    Wenn ich eine ungueltige URL eingebe und auf Importieren klicke
    Dann sehe ich eine Fehlermeldung zum Import

  Szenario: I18n-Aufloesung auf der Import-Seite
    Wenn ich die Unterkunftsseite fuer den Import-Test oeffne
    Dann enthaelt die Seite keine unaufgeloesten Message-Keys "??"
