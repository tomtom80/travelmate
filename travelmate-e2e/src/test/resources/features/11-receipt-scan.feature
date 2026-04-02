# language: de
Funktionalität: Kassenzettel scannen
  Als Teilnehmer einer Reise
  möchte ich einen Kassenzettel fotografieren und den Beleg automatisch erkennen lassen,
  damit ich Belege schnell und einfach einreichen kann.

  Hintergrund:
    Angenommen ich bin als Mitglied einer Reisepartei eingeloggt

  @manuell
  Szenario: Beleg-Scan-Button auf der Abrechnungsseite sichtbar
    Angenommen ich befinde mich auf der Abrechnungsseite einer abgeschlossenen Reise
    Dann sehe ich einen Button zum Beleg scannen

  @manuell
  Szenario: Kassenzettel fotografieren und Daten erkennen
    Angenommen ich befinde mich auf der Abrechnungsseite
    Wenn ich ein Foto eines Kassenzettels hochlade
    Dann sehe ich ein vorausgefülltes Beleg-Formular
    Und der erkannte Betrag ist vorausgefüllt
    Und ich kann den Betrag korrigieren und den Beleg einreichen

  @manuell
  Szenario: Nicht erkennbares Foto zeigt leeres Formular
    Angenommen ich befinde mich auf der Abrechnungsseite
    Wenn ich ein unlesbares Foto hochlade
    Dann sehe ich ein leeres Beleg-Formular zur manuellen Eingabe
