# Kosmetik für alle – Supabase

Damit andere Bambus-Spieler deine Kosmetik sehen, speichert der Client deine Auswahl (Cape, Flügel, Hut,
Partikel) in einem Supabase-Projekt. Nur **eingebaute** Designs werden geteilt – selbst erstellte Capes und
Flügel bleiben nur auf deinem Bildschirm.

## So funktioniert es

- **Lesen:** Der Client fragt alle 2 Sekunden die Spieler in deiner Nähe ab
  (`GET /rest/v1/kosmetik?uuid=in.(…)`, nur mit dem öffentlichen Schlüssel). Jeder darf lesen.
- **Schreiben:** Nur über die Edge Function `kosmetik`. Der Client meldet sich vorher bei Mojang an, als würde er
  einem Server beitreten (`joinServer` mit einer Zufallskennung). Die Funktion fragt Mojang (`hasJoined`), ob das
  stimmt, und bekommt die echte UUID. So kann niemand die Kosmetik eines anderen ändern, und der Zugangs-Token
  verlässt den Computer nur Richtung Mojang.
- Gespeichert wird nur: UUID, die vier Kosmetik-Ids und die Zeit der letzten Änderung. Kein Name, keine IP.

## Einrichten

1. Supabase-Projekt anlegen (oder ein vorhandenes nehmen).
2. **Tabelle:** Inhalt von `migrations/20261111120000_kosmetik.sql` im SQL-Editor ausführen
   (oder `supabase db push`).
3. **Funktion:** `functions/kosmetik/index.ts` als Edge Function `kosmetik` veröffentlichen –
   **ohne JWT-Prüfung** (die Funktion prüft selbst bei Mojang):
   `supabase functions deploy kosmetik --no-verify-jwt --project-ref <projekt-ref>`
4. In `src/main/resources/assets/bambus/online.properties` eintragen:
   - `supabase.url` = Projekt-Adresse, z. B. `https://abcd1234.supabase.co`
   - `supabase.schluessel` = öffentlicher Schlüssel (`sb_publishable_…` oder der alte „anon“-Schlüssel)
5. Client neu bauen. Fertig.

Neue eingebaute Kosmetik? Dann die Listen oben in `functions/kosmetik/index.ts` ergänzen und die Funktion neu
veröffentlichen – sonst speichert sie das neue Design nicht.

## Im Spiel

Bambus-Menü → Kosmetik: „Kosmetik teilen“ (deine Auswahl hochladen) und „Kosmetik anderer zeigen“ lassen sich
einzeln ausschalten (in `config/bambus.json`: `kosmetikTeilen`, `kosmetikAndere`).
