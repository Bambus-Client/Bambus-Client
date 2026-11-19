# Sicherheit – unterschriebene Updates

Der Launcher nimmt Updates nur an, wenn die `version.json` im Branch `auto-update` mit dem **geheimen
Bambus-Schlüssel** unterschrieben ist (Datei `version.sig`). In der `version.json` steht die Prüfsumme der neuen
`BambusLauncher.jar` – so ist auch das Programm selbst abgesichert. Wer nur an den Branch `auto-update` oder an die
Datei-Verbindung herankommt, kann so kein falsches Update unterschieben.

Außerdem prüft der Launcher bei Mods die Prüfsummen von Modrinth und CurseForge, nimmt nur saubere Dateinamen an
und lädt nur über https.

## Mod-Scanner (das „Antivirus“ für Mods)

`launcher/src/de/bambus/launcher/ModScanner.java` prüft vor jedem Spielstart alle `.jar`-Dateien im mods-Ordner
des gewählten Profils – auch eingebettete Jars:

- **Gefährlich** (Start wird angehalten, Quarantäne empfohlen): Spuren von fractureiser, Datenversand über
  Discord-Webhooks oder Telegram-Bots (auch Base64-versteckt), Zugriff auf Discord-Token, Browser-Passwörter,
  Krypto-Wallets. Bei Mods, die es nicht bei Modrinth gibt, zusätzlich: Zugriff auf Anmeldedaten anderer Launcher.
- **Verdächtig** (nur bei unbekannter Herkunft): startet Programme, lädt Code aus dem Internet nach usw.
- **Unbekannte Herkunft**: nicht bei Modrinth gefunden, sonst unauffällig.
- **Sicher**: genau diese Datei gibt es bei Modrinth, oder es ist die Bambus-Mod selbst.

Die Herkunft wird über die Prüfsumme bei Modrinth abgefragt (`/v2/version_files`) – es werden keine Dateien
hochgeladen. Ergebnisse stehen je Prüfsumme in `~/.bambus-launcher/mod-scan.properties`.
Quarantäne: `~/.bambus-launcher/quarantaene/<Datum>/` mit `LIES-MICH.txt` – nichts wird gelöscht.
Neue Erkennungsmerkmale: in `ModScanner` eintragen und `MERKMALE_VERSION` erhöhen.
Prüfung von Hand: Launcher → EINSTELLUNGEN → SICHERHEIT.

Der Scanner erkennt bekannte Muster – kein Ersatz für einen Virenscanner für den ganzen Computer.

## Einmal einrichten (Schlüssel erzeugen)

Der geheime Schlüssel wird **von euch** erzeugt und bleibt bei euch. Am Mac im Programm **Terminal**:

```sh
cd ~/Desktop
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:3072 -out bambus-schluessel.pem
openssl pkey -in bambus-schluessel.pem -pubout -outform DER | base64 > bambus-oeffentlich.txt
```

1. **Geheimen Schlüssel bei GitHub hinterlegen:** `pbcopy < bambus-schluessel.pem` (kopiert ihn),
   dann auf GitHub im Projekt → *Settings* → *Secrets and variables* → *Actions* → *New repository secret*.
   Name: `BAMBUS_SIGNIER_SCHLUESSEL`, Inhalt einfügen, speichern.
2. **Öffentlichen Schlüssel eintragen:** Den Inhalt von `bambus-oeffentlich.txt` in
   `launcher/src/de/bambus/launcher/Sicherheit.java` bei `OEFFENTLICH` als Text in Anführungszeichen eintragen
   (oder einfach an Claude schicken – der öffentliche Schlüssel darf öffentlich sein).
3. **Den geheimen Schlüssel sicher aufbewahren** (z. B. USB-Stick oder Passwort-Manager) und vom Schreibtisch
   löschen. Niemals weitergeben, nicht in den Chat, nicht ins Projekt.

Bis beides eingetragen ist, verteilt der Workflow „Auto-Update verteilen“ absichtlich **nichts** (er bricht mit
einer Fehlermeldung ab). So kann nie ein Launcher rausgehen, der keine Updates mehr annehmen würde.

## Schlüssel wechseln

1. Neuen Schlüssel erzeugen, seinen öffentlichen Teil **zusätzlich** in `OEFFENTLICH` eintragen, verteilen.
2. Ein paar Wochen warten, bis die meisten Launcher das Update haben.
3. Dann das Secret `BAMBUS_SIGNIER_SCHLUESSEL` durch den neuen geheimen Schlüssel ersetzen und den alten
   öffentlichen Schlüssel entfernen.

## Was das nicht abdeckt

Wer Schreibrechte auf `main` hat, kann den Workflow ändern und damit unterschreiben lassen. Darum:
**Zwei-Faktor-Anmeldung (2FA) für alle GitHub-Konten mit Schreibrechten** und für `main` am besten
„Require a pull request before merging“ einschalten.
