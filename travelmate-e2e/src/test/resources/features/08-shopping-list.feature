# language: de
Funktionalität: Einkaufsliste
  Als Organisator einer Reise
  moechte ich eine Einkaufsliste aus dem Essensplan generieren und verwalten,
  damit alle Zutaten und eigene Eintraege an einem Ort sind.

  Hintergrund:
    Angenommen ich bin als Mitglied einer Reisepartei eingeloggt
    Und es existiert eine Reise mit Essensplan und zugewiesenen Rezepten

  # S8-A: Einkaufsliste generieren (US-TRIPS-050)
  Szenario: Einkaufsliste-Button auf der Reisedetailseite
    Wenn ich die Reisedetailseite oeffne
    Dann sehe ich einen Link zur Einkaufsliste

  Szenario: Einkaufsliste generieren
    Wenn ich die Einkaufsliste-Seite oeffne
    Und ich die Einkaufsliste generiere
    Dann sehe ich die generierte Einkaufsliste
    Und die Rezept-Zutaten werden als Eintraege angezeigt

  Szenario: Einkaufsliste zeigt skalierte Zutaten
    Angenommen die Einkaufsliste wurde generiert
    Dann sehe ich die Zutaten aus dem Essensplan

  # S8-B: Manuellen Eintrag hinzufuegen (US-TRIPS-051)
  Szenario: Manuellen Eintrag hinzufuegen
    Angenommen die Einkaufsliste wurde generiert
    Wenn ich einen manuellen Eintrag "Bier" mit Menge "6" und Einheit "Flaschen" hinzufuege
    Dann wird der Eintrag "Bier" in der manuellen Liste angezeigt

  # S8-C: Eintrag zuweisen (US-TRIPS-052)
  @manuell
  Szenario: Eintrag mir selbst zuweisen
    Angenommen die Einkaufsliste hat Eintraege
    Wenn ich auf "Ich uebernehme" bei einem Eintrag klicke
    Dann wird der Eintrag als "Uebernommen" angezeigt
    Und mein Name steht beim Eintrag

  # S8-D: Eintrag als gekauft markieren (US-TRIPS-053)
  @manuell
  Szenario: Eintrag als gekauft markieren
    Angenommen ein Eintrag ist mir zugewiesen
    Wenn ich auf "Erledigt" bei diesem Eintrag klicke
    Dann wird der Eintrag als "Erledigt" angezeigt

  # S8-B: Regeneration (US-TRIPS-050)
  Szenario: Einkaufsliste aktualisieren
    Angenommen die Einkaufsliste wurde generiert
    Wenn ich die Einkaufsliste aktualisiere
    Dann wird die Einkaufsliste neu generiert
    Und manuelle Eintraege bleiben erhalten

  Szenario: I18n-Aufloesung auf der Einkaufslisten-Seite
    Angenommen die Einkaufsliste wurde generiert
    Dann enthaelt die Seite keine unaufgeloesten Message-Keys "??"
