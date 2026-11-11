package de.bambus.launcher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Sprache des Launchers (Deutsch oder Englisch).
 * Der Launcher ist auf Deutsch geschrieben; bei Englisch übersetzt {@link UebersetzteGrafik}
 * jeden Text beim Zeichnen über diese Tabelle. Neue Texte: einfach hier ein Paar ergänzen.
 */
final class Sprache {

	static final String DE = "de", EN = "en";
	private static volatile String code = Locale.getDefault().getLanguage().equals("de") ? DE : EN;

	/** Ganze Texte (genau so). */
	private static final Map<String, String> GANZ = new HashMap<>();
	/** Teile von zusammengesetzten Texten (z. B. „Du hast Version “ + Nummer). Längste zuerst. */
	private static final List<String[]> TEILE = new ArrayList<>();
	private static final Map<String, String> GROSS = new HashMap<>();
	private static final Map<String, String> CACHE = new LinkedHashMap<String, String>(512, 0.75f, true) {
		@Override protected boolean removeEldestEntry(Map.Entry<String, String> e) { return size() > 3000; }
	};

	private Sprache() {}

	static String code() { return code; }

	static boolean englisch() { return EN.equals(code); }

	static void setze(String c) {
		code = EN.equals(c) ? EN : DE;
		synchronized (CACHE) { CACHE.clear(); }
		boolean en = englisch();   // Knöpfe der Standard-Dialoge
		javax.swing.UIManager.put("OptionPane.yesButtonText", en ? "Yes" : "Ja");
		javax.swing.UIManager.put("OptionPane.noButtonText", en ? "No" : "Nein");
		javax.swing.UIManager.put("OptionPane.cancelButtonText", en ? "Cancel" : "Abbrechen");
		javax.swing.UIManager.put("OptionPane.okButtonText", "OK");
	}

	/** Übersetzt einen deutschen Text in die gewählte Sprache. */
	static String t(String s) {
		if (s == null || s.isEmpty() || !englisch()) return s;
		synchronized (CACHE) {
			String c = CACHE.get(s);
			if (c != null) return c;
		}
		String r = GANZ.get(s);
		if (r == null) r = GROSS.get(s);
		if (r == null) {
			String trim = s.trim();
			String g = GANZ.get(trim);
			if (g == null) g = GROSS.get(trim);
			if (g != null) r = s.replace(trim, g);
		}
		if (r == null) {
			r = s;
			for (String[] p : TEILE) if (r.contains(p[0])) r = r.replace(p[0], p[1]);
		}
		synchronized (CACHE) { CACHE.put(s, r); }
		return r;
	}

	private static void g(String de, String en) {
		GANZ.put(de, en);
		GROSS.put(de.toUpperCase(Locale.GERMAN), en.toUpperCase(Locale.ENGLISH));
	}

	private static void teil(String de, String en) {
		TEILE.add(new String[]{de, en});
	}

	static {
		// ---------------------------------------------------------------- Navigation & Rahmen
		g("START", "HOME"); g("MODULE", "MODULES"); g("KOSMETIK", "COSMETICS"); g("MOD-MENÜ", "MOD MENU"); g("Launcher im Pixel-Look wie im Spiel – mit Farbthemen, die auch im Spiel gelten", "Launcher in pixel style like in game – with color themes that also apply in game"); g("Flügel in zwei Stilen: HD oder Blockbench (eckig) – in der Garderobe umschaltbar", "Wings in two styles: HD or Blockbench (blocky) – switch it in the wardrobe"); g("STIL: HD", "STYLE: HD"); g("STIL: BLOCKBENCH", "STYLE: BLOCKBENCH"); g("HD = weich und biegsam · Blockbench = eckig mit groben Pixeln", "HD = smooth and bendy · Blockbench = blocky with chunky pixels"); g("FARBE  –  GILT AUCH IM SPIEL", "COLOR  –  ALSO USED IN GAME"); g("OZEAN", "OCEAN"); g("LILA", "PURPLE"); g("EIS", "ICE"); g("GARDEROBE", "WARDROBE"); g("MODS", "MODS");
		g("SERVER", "SERVERS"); g("NEUIGKEITEN", "NEWS"); g("HELFER", "HELPER");
		g("SENDEN  ➤", "SEND  ➤"); g("Schreib deine Frage, z. B. „Wie baue ich ein Netherportal?“", "Type your question, e.g. “How do I build a Nether portal?”");
		g("Frag mich alles zum Bambus Client und zu Minecraft – ganz ohne Internet.", "Ask me anything about the Bambus Client and Minecraft – no internet needed.");
		g("Der Helfer ist keine echte KI: Er kennt feste Antworten und kann sich irren. Alles bleibt auf deinem Computer.", "The helper isn't a real AI: it knows fixed answers and can be wrong. Everything stays on your computer.");
		g("Bambus-Helfer: Hilfe-Chat zu Client und Minecraft – ohne Internet", "Bambus Helper: help chat about the client and Minecraft – no internet"); g("EINSTELLUNGEN", "SETTINGS");
		g("NAVIGATION", "NAVIGATION"); g("Kein offizielles Minecraft-Produkt.", "Not an official Minecraft product.");
		g("Nicht von Mojang oder Microsoft genehmigt", "Not approved by or associated with");
		g("oder mit ihnen verbunden.", "Mojang or Microsoft.");
		g("MICROSOFT · ANMELDEN", "MICROSOFT · SIGN IN");
		g("BITTE WARTEN", "PLEASE WAIT"); g("ÖFFNEN", "OPEN"); g("ÖFFNEN →", "OPEN →");

		// ---------------------------------------------------------------- Start
		g("SPIELEN", "PLAY"); g("Dein PvP-Client im Neon-Look. Module, Kosmetik und HUD –", "Your PvP client in neon style. Modules, cosmetics and HUD –");
		g("alles mit einem Klick an- und ausschaltbar.", "everything on and off with a single click.");
		g("MODULE AKTIV", "MODULES ACTIVE"); g("STATUS", "STATUS"); g("TIPP", "TIP");
		g("System bereit. Klicke auf SPIELEN.", "System ready. Click PLAY.");
		g("Erster Start: Alles wird automatisch eingerichtet.", "First start: everything is set up automatically.");
		g("Minecraft nicht gefunden – installiere zuerst Minecraft.", "Minecraft not found – please install Minecraft first.");
		g("◆  Anmeldung nur über dein Microsoft-Konto im offiziellen Minecraft Launcher – Bambus sieht nie dein Passwort.",
				"◆  Sign-in only with your Microsoft account in the official Minecraft Launcher – Bambus never sees your password.");
		g("Rechtsklick aufs Bambus-Symbol in der Taskleiste → „An Taskleiste anheften“", "Right-click the Bambus icon in the taskbar → “Pin to taskbar”");
		g("Starte …", "Starting …"); g("Fehler – siehe Meldung.", "Error – see message.");
		g("Bereit – öffne jetzt Minecraft.", "Ready – now open Minecraft.");
		g("Minecraft wird geöffnet – Profil „Bambus Client“ wählen.", "Opening Minecraft – choose the “Bambus Client” profile.");
		teil("Minecraft startet – viel Spaß, ", "Minecraft is starting – have fun, ");
		teil("  ·  FABRIC  ·  V", "  ·  FABRIC  ·  V");

		// ---------------------------------------------------------------- Dialoge beim Spielen
		g("Minecraft fehlt", "Minecraft missing"); g("Verstanden", "Got it"); g("Ich habe die Datei", "I have the file");
		g("Vorschau-Version", "Preview version"); g("Bambus-Installation", "Bambus installation");
		g("Bambus-Client-Mod auswählen", "Choose the Bambus Client mod");
		teil("Der Minecraft-Ordner wurde nicht gefunden:", "The Minecraft folder was not found:");
		teil("Installiere zuerst Minecraft (den offiziellen Minecraft Launcher von minecraft.net),", "Please install Minecraft first (the official Minecraft Launcher from minecraft.net),");
		teil("melde dich an und starte das Spiel einmal. Danach hier auf SPIELEN klicken.", "sign in and start the game once. Then click PLAY here.");
		teil("Das ist eine Vorschau des Bambus Launchers.", "This is a preview of the Bambus Launcher.");
		teil("Du kannst dir schon alles ansehen: Module, Kosmetik mit 3D-Figur, Mods, Server und Einstellungen.", "You can already look at everything: modules, cosmetics with a 3D figure, mods, servers and settings.");
		teil("Zum Spielen fehlt noch der Spiel-Teil (die Datei bambus-client-", "To play, the game part is still missing (the file bambus-client-");
		teil("Er kommt mit der fertigen Version (Setup für Windows bzw. .dmg für den Mac) automatisch mit.", "It comes automatically with the finished version (setup for Windows or .dmg for Mac).");
		teil("Im Minecraft Launcher das Profil „", "In the Minecraft Launcher choose the profile “");
		teil("“ wählen", "”");
		teil("(unten links neben dem grünen Spielen-Knopf) und auf „Spielen“ klicken.", "(bottom left next to the green Play button) and click “Play”.");
		teil("War der Minecraft Launcher schon offen? Dann schließe ihn einmal", "Was the Minecraft Launcher already open? Then close it once");
		teil("und öffne ihn neu, damit er das Bambus-Profil sieht.", "and open it again so it sees the Bambus profile.");
		teil("Hinweis zu den Mods:", "Note about the mods:"); teil("Alles bereit!", "All set!");
		teil("Alles bereit! Minecraft konnte ich aber nicht automatisch öffnen.", "All set! But I couldn't open Minecraft automatically.");
		teil("Öffne es bitte selbst.", "Please open it yourself."); teil("Da ist etwas schiefgegangen:", "Something went wrong:");
		teil("Prüfe deine Internetverbindung und versuche es noch einmal.", "Check your internet connection and try again.");
		teil("Der Ordner liegt hier:", "The folder is here:");

		// ---------------------------------------------------------------- Einrichten (Fortschritt)
		g("Minecraft-Version wird geprüft …", "Checking Minecraft version …"); g("Fabric-Profil fehlt.", "Fabric profile missing.");
		g("Minecraft wird geladen …", "Loading Minecraft …"); g("Bibliotheken werden geprüft …", "Checking libraries …");
		g("Spielgrafiken und Töne werden geprüft …", "Checking game graphics and sounds …"); g("Java für Minecraft wird geprüft …", "Checking Java for Minecraft …");
		g("Minecraft startet …", "Minecraft is starting …"); g("Fabric API wird geprüft …", "Checking Fabric API …");
		g("Bambus Client wird kopiert …", "Copying Bambus Client …"); g("Fertig! Viel Spaß mit dem Bambus Client.", "Done! Have fun with the Bambus Client.");
		g("Fabric API wird heruntergeladen …", "Downloading Fabric API …"); g("Java für Minecraft wurde nicht gefunden.", "Java for Minecraft was not found.");
		teil(" wird eingerichtet …", " is being set up …"); teil("Profil „", "Profile “"); teil("“ wird angelegt …", "” is being created …");
		teil("Bibliotheken: ", "Libraries: "); teil("Spielgrafiken und Töne: ", "Game graphics and sounds: "); teil("Java für Minecraft: ", "Java for Minecraft: ");
		teil(" nicht bei Mojang gefunden.", " not found at Mojang."); teil("Keine Fabric API für Minecraft ", "No Fabric API for Minecraft ");
		teil(" gefunden.", " found."); teil("Download fehlgeschlagen (", "Download failed (");
		teil("Die Bambus-Client-Mod (bambus-client-….jar) wurde nicht gefunden.", "The Bambus Client mod (bambus-client-….jar) was not found.");
		teil(" ist beschädigt: ", " is damaged: "); teil("Die Datei ", "The file ");

		// ---------------------------------------------------------------- Module
		g("Karte anklicken = an/aus. Gilt ab dem nächsten Spielstart · im Spiel: RECHTS-SHIFT.", "Click a card = on/off. Applies from the next game start · in game: RIGHT SHIFT.");
		g("HUD-ANZEIGEN", "HUD DISPLAYS"); g("FUNKTIONEN", "FEATURES"); g("HUD AN", "HUD ON"); g("HUD AUS", "HUD OFF");
		g("Info", "Info"); g("FPS, Koordinaten und CPS", "FPS, coordinates and CPS"); g("W A S D, Maus und Leertaste", "W A S D, mouse and space bar");
		g("Serveradresse und Ping", "Server address and ping"); g("Zeigt Toggle Sprint / Sneak", "Shows toggle sprint / sneak");
		g("Rüstungsanzeige", "Armor Display"); g("Rüstung mit Haltbarkeit", "Armor with durability"); g("Trank-Effekte", "Potion Effects");
		g("Aktive Tränke mit Restzeit", "Active potions with time left"); g("Name, Leben und Abstand deines Ziels", "Name, health and distance of your target");
		g("Combo-Zähler", "Combo Counter"); g("Treffer in Folge", "Hits in a row"); g("Reach-Anzeige", "Reach Display");
		g("Abstand beim letzten Treffer", "Distance of your last hit"); g("Sprint-Reset vor dem Treffer?", "Sprint reset before the hit?");
		g("Kill-Zähler", "Kill Counter"); g("Kills, Tode, K/D der Sitzung", "Kills, deaths, K/D of the session"); g("Pfeil-Zähler", "Arrow Counter");
		g("Pfeile im Inventar", "Arrows in your inventory"); g("Enderperle, Schild & Co.", "Ender pearl, shield & more");
		g("Richtung", "Direction"); g("Himmelsrichtung und Winkel", "Compass direction and angle"); g("Biom", "Biome");
		g("In welchem Biom du bist", "Which biome you're in"); g("Tempo", "Speed"); g("Blöcke pro Sekunde", "Blocks per second");
		g("Uhr", "Clock"); g("Echte Uhrzeit", "Real time"); g("Arbeitsspeicher", "Memory"); g("RAM von Minecraft", "Minecraft's RAM");
		g("Restzeit von gezündetem TNT", "Time left on lit TNT"); g("Kompass-Balken", "Compass Bar"); g("Himmelsrichtungen gleiten vorbei", "Directions glide past");
		g("Koordinaten", "Coordinates"); g("Mit Nether-Umrechnung und Kopier-Taste", "With Nether conversion and copy key"); g("Spielzeit", "Playtime");
		g("Zeit seit dem Betreten, Ingame-Tag", "Time since joining, in-game day"); g("Ping-Verlauf", "Ping History");
		g("Signalbalken und Ping der letzten Minute", "Signal bars and ping of the last minute");
		g("Sprint-Taste 1× = dauerhaft", "Sprint key once = stays on"); g("Schleich-Taste 1× = dauerhaft", "Sneak key once = stays on");
		g("Zoom mit Mausrad", "Zoom with Mouse Wheel"); g("C halten · Mausrad zoomt", "Hold C · mouse wheel zooms");
		g("Linke Alt halten · umschauen", "Hold left Alt · look around"); g("Nicht auf jedem Server erlaubt", "Not allowed on every server");
		g("Alles hell, auch in Höhlen", "Everything bright, even in caves"); g("Fadenkreuz", "Crosshair"); g("Eigene Form & Farbe", "Your own shape & color");
		g("Treffer-Partikel", "Hit Particles"); g("Extra Funken beim Treffen", "Extra sparks when hitting"); g("Low-End-Modus", "Low-End Mode");
		g("Sparsame Grafik für langsame PCs", "Lightweight graphics for slow PCs"); g("Gegner-Leben als Zahl", "Enemy Health as Number");
		g("Genaue ❤ im Target-HUD", "Exact ❤ in the Target HUD"); g("Screenshot bei jedem Spieler-Kill", "Screenshot on every player kill");
		g("Farbverlauf", "Gradient"); g("HUD-Kästen mit Farbverlauf", "HUD boxes with a color gradient");

		// ---------------------------------------------------------------- Kosmetik
		g("Deine Figur mit der Maus drehen. Cape, Flügel und Partikel sind nur für dich sichtbar.", "Rotate your figure with the mouse. Cape, wings and particles are only visible to you.");
		g("CAPES", "CAPES"); g("FLÜGEL", "WINGS"); g("PARTIKEL", "PARTICLES"); g("HÜTE", "HATS"); g("KILL", "KILL"); g("PFEIL", "ARROW");
		g("3D-VORSCHAU", "3D PREVIEW"); g("ZIEHEN = DREHEN", "DRAG = ROTATE"); g("VORNE", "FRONT"); g("HINTEN", "BACK"); g("⟳ DREHEN", "⟳ ROTATE");
		g("SKIN", "SKIN"); g("LADEN", "LOAD"); g("CAPE", "CAPE"); g("SPUR", "TRAIL"); g("HUT", "HAT"); g("EIGEN", "CUSTOM"); g("STUDIO", "STUDIO");
		g("● AKTIV", "● ACTIVE"); g("WÄHLEN", "CHOOSE"); g("RECHTSKLICK: ✕", "RIGHT-CLICK: ✕"); g("ORDNER ÖFFNEN", "OPEN FOLDER");
		g("Erstellen", "Create"); g("Kein Cape", "No Cape"); g("Kein Hut", "No Hat"); g("Keine", "None"); g("Keins", "None");
		g("Skin wird geladen …", "Loading skin …"); g("Minecraft-Name eingeben und LADEN klicken", "Enter a Minecraft name and click LOAD");
		g("Hier liegen deine eigenen Capes und Flügel als PNG", "Your own capes and wings are stored here as PNG");
		g("Zuletzt geladen", "Last loaded"); g("Standard-Skin", "Default skin"); g("Kein Konto gefunden – Namen eingeben", "No account found – enter a name");
		g("Aus deinem Minecraft-Konto", "From your Minecraft account"); g("Geladen", "Loaded"); g("Fehler beim Laden", "Error while loading");
		g("Dein Skin", "Your skin"); teil("Offline – ", "Offline – "); teil("Lade ", "Loading ");
		g("Eigene Kosmetik löschen", "Delete custom cosmetic"); teil("“ wirklich löschen?", "” – really delete?");
		// Namen der Kosmetik
		g("Galaxie", "Galaxy"); g("Polarlicht", "Northern Lights"); g("Glut", "Ember"); g("Engel", "Angel"); g("Drache", "Dragon");
		g("Schmetterling", "Butterfly"); g("Phönix", "Phoenix"); g("Nachtschwinge", "Nightwing"); g("Kristall", "Crystal");
		g("Neon-Funken", "Neon Sparks"); g("Sternenstaub", "Stardust"); g("Herzen", "Hearts"); g("Musik", "Music"); g("Seelenfeuer", "Soul Fire");
		g("Kirschblüten", "Cherry Blossoms"); g("Magie", "Magic"); g("Flammen", "Flames"); g("Krone", "Crown"); g("Zylinder", "Top Hat");
		g("Heiligenschein", "Halo"); g("Hörner", "Horns"); g("Katzenohren", "Cat Ears"); g("Partyhut", "Party Hat"); g("Neon-Visier", "Neon Visor");
		g("Neon-Blitz", "Neon Bolt"); g("Feuerwerk", "Fireworks"); g("Seelen", "Souls"); g("Neon-Platine", "Neon Circuit");
		// Kosmetik-Studio
		g("Kosmetik-Studio", "Cosmetics Studio"); g("KOSMETIK-STUDIO", "COSMETICS STUDIO"); g("ERSTELLE DEIN EIGENES ", "CREATE YOUR OWN ");
		g("FLÜGELPAAR", "PAIR OF WINGS"); g("ART", "TYPE"); g("VORLAGE", "TEMPLATE"); g("FARBEN  ·  KLICKEN ZUM ÄNDERN", "COLORS  ·  CLICK TO CHANGE");
		g("HAUPTFARBE", "MAIN COLOR"); g("AKZENT", "ACCENT"); g("LEUCHTEN", "GLOW"); g("INITIALEN", "INITIALS"); g("ODER", "OR"); g("NAME", "NAME");
		g("1–3 Buchstaben groß auf dem Cape, z. B. deine Initialen", "1–3 big letters on the cape, e.g. your initials");
		g("EIGENES BILD …", "OWN IMAGE …"); g("Eigenes Bild entfernen", "Remove own image"); g("🎲 ZUFALL", "🎲 RANDOM"); g("ABBRECHEN", "CANCEL");
		g("SPEICHERN & ANZIEHEN", "SAVE & WEAR"); g("Mein Cape", "My Cape"); g("Meine Flügel", "My Wings"); g("Kein Bild", "No image");
		g("Verlauf", "Gradient"); g("Streifen", "Stripes"); g("Sterne", "Stars"); g("Platine", "Circuit"); g("Welle", "Wave"); g("Federn", "Feathers");
		g("AUSSENSEITE", "OUTSIDE"); g("TEXTUR", "TEXTURE"); g("LIVE-VORSCHAU", "LIVE PREVIEW"); g("EIGENE FARBE …", "CUSTOM COLOR …");
		g("Gib deiner Kosmetik einen Namen.", "Give your cosmetic a name."); g("Das Bild ist zu groß (max. 4096 Pixel)", "The image is too big (max. 4096 pixels)");
		g("Bild für dein Cape wählen", "Choose an image for your cape"); g("Bild für deine Flügel wählen", "Choose an image for your wings");
		g("PNG/JPG: Bild wird aufs Cape gesetzt · 64×32-Cape-Dateien gehen direkt", "PNG/JPG: image is put on the cape · 64×32 cape files work directly");
		g("PNG mit Transparenz: Gelenk links oben, Flügel nach rechts", "PNG with transparency: joint top left, wing pointing right");
		g("Wird in config/bambus-kosmetik gespeichert · nur für dich sichtbar", "Saved in config/bambus-kosmetik · only visible to you");
		teil("Cape-Vorlage erkannt: ", "Cape template detected: "); teil("Bild: ", "Image: "); teil("  (ersetzt die Vorlage)", "  (replaces the template)");
		teil("Bild nicht lesbar: ", "Image not readable: "); teil("Speichern fehlgeschlagen:", "Saving failed:");

		// ---------------------------------------------------------------- Mods
		g("FPS-Boost, Mods-Menü und eigene Mods · wird beim nächsten SPIELEN eingerichtet.", "FPS boost, mod menu and your own mods · set up the next time you PLAY.");
		g("⚡ FPS-BOOST", "⚡ FPS BOOST"); g("An · bis zu doppelte FPS, weniger Ruckler", "On · up to double FPS, less stutter");
		g("Aus · Klicken für mehr FPS", "Off · click for more FPS"); g("EMPFOHLEN", "RECOMMENDED"); g("MODRINTH DURCHSUCHEN", "SEARCH MODRINTH");
		g("DEINE MODS", "YOUR MODS"); g("Noch keine. Suche oben oder füge eine .jar hinzu.", "None yet. Search above or add a .jar.");
		g("Tippe z. B. „minimap“, „zoom“ oder „shader“.", "Type e.g. “minimap”, “zoom” or “shader”."); g("SUCHEN", "SEARCH"); g("+ MOD-DATEI", "+ MOD FILE");
		g("Profile mit eigenen Mods · wird beim nächsten SPIELEN eingerichtet.", "Profiles with their own mods · set up the next time you PLAY.");
		g("PROFILE", "PROFILES"); g("KOPIE", "COPY"); g("LÖSCHEN", "DELETE");
		g("INSTALLIERT", "INSTALLED"); g("ENTDECKEN", "DISCOVER"); g("SCHLÜSSEL", "API KEY"); g("DATEI", "FILE");
		g("Profil umbenennen", "Rename profile"); g("Profil mit allen Mods kopieren", "Copy profile with all its mods"); g("Profil löschen", "Delete profile");
		g("Neues Profil", "New profile"); g("Profil kopieren", "Copy profile"); g("Name des Profils:", "Profile name:");
		g("Das letzte Profil kann nicht gelöscht werden.", "The last profile can't be deleted.");
		g("CurseForge-API-Schlüssel eintragen oder ändern", "Enter or change your CurseForge API key"); g("CurseForge-Schlüssel", "CurseForge API key");
		g("CurseForge verlangt einen eigenen API-Schlüssel (kostenlos):\n1. Auf console.curseforge.com anmelden\n2. Unter „API keys“ den Schlüssel kopieren\n3. Hier einfügen\n\nDer Schlüssel bleibt nur auf diesem Computer.",
				"CurseForge requires your own API key (free):\n1. Sign in at console.curseforge.com\n2. Copy the key under “API keys”\n3. Paste it here\n\nThe key stays on this computer only.");
		g("Suche auf CurseForge …", "Searching CurseForge …"); g("Keine Verbindung zu CurseForge. Internet prüfen.", "No connection to CurseForge. Check your internet.");
		g("CurseForge lehnt den Schlüssel ab – bitte unter SCHLÜSSEL prüfen.", "CurseForge rejects the key – please check it under API KEY.");
		teil("Fabric-Mods für ", "Fabric mods for "); teil(" · „+“ fügt die Mod zum aktiven Profil hinzu", " · “+” adds the mod to the active profile");
		teil("INSTALLIERT · ", "INSTALLED · "); g("+ NEU", "+ NEW"); teil(" · AKTIV", " · ACTIVE");
		teil("Profil „", "Profile “"); 
		teil(": nur direkt bei curseforge.com ladbar – dort laden und mit „+ MOD-DATEI“ hinzufügen", ": only downloadable on curseforge.com – get it there and add it with “+ MOD FILE”");
		g("Suche auf Modrinth …", "Searching Modrinth …"); g("Keine Verbindung zu Modrinth. Internet prüfen.", "No connection to Modrinth. Check your internet.");
		g("Neue Grafik-Engine – oft doppelt so viele FPS", "New graphics engine – often twice the FPS");
		g("Schnellere Spiel-Logik: Physik, Tiere, Chunks", "Faster game logic: physics, mobs, chunks");
		g("Braucht deutlich weniger Arbeitsspeicher", "Uses much less memory"); g("Zeichnet HUD, Text und Schilder schneller", "Draws HUD, text and signs faster");
		g("Verdeckte Tiere und Truhen werden nicht gezeichnet", "Hidden mobs and chests are not drawn");
		g("Spart Leistung, wenn Minecraft im Hintergrund ist", "Saves power when Minecraft is in the background");
		g("„Mods“-Knopf im Spiel: alle Mods ansehen und einstellen", "“Mods” button in game: view and configure all mods");
		g("MENÜ", "MENU"); g("eigene Mod", "own mod"); g("keine Datei", "no file"); g("Mod entfernen", "Remove mod"); g("DATEI", "FILE");
		teil("Alle Downloads direkt von modrinth.com · nur Fabric-Mods für ", "All downloads straight from modrinth.com · only Fabric mods for ");
		teil("Nichts gefunden für Minecraft ", "Nothing found for Minecraft "); teil("Mods-Ordner: ", "Mods folder: ");
		teil(" gibt es noch nicht für Minecraft ", " isn't available yet for Minecraft "); teil(" Mio.", " M"); teil(" Tsd.", " K");
		teil("“ löschen?", "” – delete?"); teil("Löschen fehlgeschlagen: ", "Deleting failed: ");

		// ---------------------------------------------------------------- Server
		g("Deine Lieblingsserver mit Live-Status. Im Spiel findest du sie unter Mehrspieler.", "Your favorite servers with live status. In game you'll find them under Multiplayer.");
		g("Serveradresse, z. B. play.beispiel.de", "Server address, e.g. play.example.com"); g("+ HINZUFÜGEN", "+ ADD"); g("⟳ AKTUALISIEREN", "⟳ REFRESH");
		g("Abfrage wie in der Mehrspieler-Liste: Es wird nur der öffentliche Status gelesen, ohne Anmeldung.", "Same check as the multiplayer list: only the public status is read, no sign-in.");
		g("Frage Status ab …", "Checking status …"); g("Online", "Online"); g("Adresse nicht gefunden", "Address not found");
		g("Keine Antwort", "No answer"); g("Nicht erreichbar", "Not reachable");
		g("Das sieht nicht wie eine Serveradresse aus (z. B. play.beispiel.de).", "That doesn't look like a server address (e.g. play.example.com).");
		teil(" ist schon in deiner Liste.", " is already in your list."); teil(" hinzugefügt – im Spiel unter Mehrspieler.", " added – in game under Multiplayer.");
		teil(" entfernt (im Spiel bleibt er in der Liste, bis du ihn dort löschst).", " removed (in game it stays in the list until you delete it there).");
		teil("Offline · ", "Offline · ");

		// ---------------------------------------------------------------- Neuigkeiten
		g("Was neu ist im Bambus Client.", "What's new in the Bambus Client."); g("NEU", "NEW");
		g("Kosmetik im HD-Look & neue Oberfläche", "HD cosmetics & new interface");
		g("3D-Flügel: biegen sich ohne Lücken, mit Knochen, Deckfedern und Glanz – neu: Bambus", "3D wings: bend without gaps, with bones, covert feathers and shine – new: Bambus");
		g("Umhänge wehen im Wind und leuchten – neu: Polarlicht und Glut", "Capes flow in the wind and glow – new: Northern Lights and Ember");
		g("Bambus-Ladebildschirm mit Tipps beim Betreten einer Welt", "Bambus loading screen with tips when joining a world");
		g("HUD-Editor: Anzeigen rasten ein, Größe mit Ecke oder Mausrad", "HUD editor: displays snap into place, resize with the corner or mouse wheel");
		g("Neue HUD-Module: Kompass-Balken, Koordinaten mit Nether-Umrechnung, Spielzeit, Ping-Verlauf", "New HUD modules: compass bar, coordinates with Nether conversion, playtime, ping history");
		g("Neon-Benachrichtigungen, weiche Animationen und einheitliche Symbole", "Neon notifications, smooth animations and matching icons");
		g("Launcher: animierte Hintergründe, Server-Favoriten mit Live-Ping, automatische Updates", "Launcher: animated backgrounds, favorite servers with live ping, automatic updates");
		g("Das große PvP-Update", "The big PvP update");
		g("Target-HUD, Combo, Reach, W-/S-Tap, Kills, Pfeile, Item-Cooldowns, TNT-Timer", "Target HUD, combo, reach, W/S tap, kills, arrows, item cooldowns, TNT timer");
		g("Eigene Zeit und eigenes Wetter, Treffer-Partikel, Low-End-Modus", "Your own time and weather, hit particles, low-end mode");
		g("Hüte, Kill-Effekte und Pfeil-Spuren", "Hats, kill effects and arrow trails");
		g("Modul-Suche, Favoriten, 7 Farbthemen, 3 Profile, Freunde, Screenshot-Galerie", "Module search, favorites, 7 color themes, 3 profiles, friends, screenshot gallery");
		g("Flügel, Studio & Mods", "Wings, studio & mods");
		g("HD-Flügel mit Gelenk und das Kosmetik-Studio für eigene Capes und Flügel", "HD wings with a joint and the cosmetics studio for your own capes and wings");
		g("MODS-Seite mit FPS-Boost (Sodium, Lithium & Co.) und Modrinth-Suche", "MODS page with FPS boost (Sodium, Lithium & more) and Modrinth search");
		g("Bambus-Hauptmenü im Spiel", "Bambus main menu in game");
		teil(" IST DA  –  oben auf „UPDATE“ klicken", " IS HERE  –  click “UPDATE” at the top");
		teil("●  Du hast Version ", "●  You have version "); teil("  –  neue Versionen kommen automatisch", "  –  new versions arrive automatically");

		// ---------------------------------------------------------------- Einstellungen
		g("ARBEITSSPEICHER", "MEMORY"); g("ORDNER", "FOLDERS"); g("HINTERGRUND", "BACKGROUND"); g("SPRACHE", "LANGUAGE");
		g("4–6 GB reichen für die meisten Computer. Nicht mehr als die Hälfte deines RAMs wählen.", "4–6 GB is enough for most computers. Don't choose more than half of your RAM.");
		g("Bambus-Spielordner", "Bambus game folder"); g("Mods", "Mods"); g("CLIENT-EINSTELLUNGEN ZURÜCKSETZEN", "RESET CLIENT SETTINGS");
		g("AUTO-UPDATE: AN", "AUTO-UPDATE: ON"); g("AUTO-UPDATE: AUS", "AUTO-UPDATE: OFF"); g("JETZT PRÜFEN", "CHECK NOW");
		g("AUF DEN DESKTOP", "TO DESKTOP"); g("INS STARTMENÜ", "TO START MENU"); g("GITTER", "GRID"); g("DATENREGEN", "DATA RAIN");
		g("Taskleiste: Rechtsklick aufs Symbol → „Anheften“", "Taskbar: right-click the icon → “Pin”");
		g("WILLKOMMENS-SEITE", "WELCOME PAGE");
		g("Alle Bambus-Einstellungen (Module, HUD-Positionen, Kosmetik) zurücksetzen?", "Reset all Bambus settings (modules, HUD positions, cosmetics)?");
		g("Zurücksetzen", "Reset"); g("Zurückgesetzt. Starte den Launcher neu, damit alle Schalter stimmen.", "Reset done. Restart the launcher so all switches are correct.");
		teil("  ·  Automatische Updates gibt es, sobald der Client über GitHub gebaut wird.", "  ·  Automatic updates arrive once the client is built on GitHub.");
		teil("Fertig! Der Bambus Launcher liegt jetzt ", "Done! The Bambus Launcher is now "); teil("auf dem Desktop", "on the desktop");
		teil("im Startmenü", "in the start menu"); teil("Das hat nicht geklappt:", "That didn't work:");
		teil("Verknüpfungen gibt es nur unter Windows.", "Shortcuts are only available on Windows.");
		teil("Version ", "Version ");

		// ---------------------------------------------------------------- Updates
		g("Suche nach Updates …", "Checking for updates …"); g("Updates", "Updates"); g("keine Verbindung", "no connection");
		g("LAUNCHER AKTUALISIEREN", "UPDATE LAUNCHER"); g("ALLE DETAILS", "ALL DETAILS"); g("Wird geladen …", "Loading …");
		g("● NEUE VERSION", "● NEW VERSION"); g("WAS IST NEU", "WHAT'S NEW"); g("… mehr unter „Alle Details“", "… more under “All details”");
		g("Neue Funktionen im Spiel kommen automatisch. Für den neuen Launcher einmal hier klicken:", "New in-game features arrive automatically. Click here once for the new launcher:");
		g("Neue Funktionen im Spiel kommen automatisch – du musst nichts weiter tun.", "New in-game features arrive automatically – you don't need to do anything.");
		g("Die neue Version ist geöffnet: Bambus Launcher in „Programme“ ziehen und ersetzen.", "The new version is open: drag Bambus Launcher into “Applications” and replace it.");
		g("Es gibt noch kein veröffentlichtes Update.", "No update has been published yet.");
		g("Für diesen Launcher ist keine Update-Quelle eingetragen.", "No update source is set for this launcher.");
		g("Die heruntergeladene Datei ist beschädigt (Prüfsumme stimmt nicht). Bitte später noch mal versuchen.", "The downloaded file is damaged (checksum mismatch). Please try again later.");
		g("Die heruntergeladene Datei ist keine gültige Mod.", "The downloaded file is not a valid mod.");
		teil("Du hast schon die neueste Version (", "You already have the newest version ("); teil("Du hast die neueste Version (", "You have the newest version (");
		teil("Client-Update ", "Client update "); teil(" ist geladen – kommt beim nächsten SPIELEN automatisch rein.", " is downloaded – it's installed automatically the next time you PLAY.");
		teil(" ist da.", " is here."); teil("Update-Suche: ", "Update check: "); teil("Du hast ", "You have "); teil(" wird geladen …", " is downloading …");
		teil("Das Update konnte nicht geladen werden:", "The update could not be downloaded:"); teil("GitHub antwortet gerade nicht (", "GitHub isn't responding right now (");

		// ---------------------------------------------------------------- Microsoft
		g("MICROSOFT-KONTO", "MICROSOFT ACCOUNT"); g("MIT MICROSOFT ANMELDEN", "SIGN IN WITH MICROSOFT"); g("SEITE ÖFFNEN", "OPEN PAGE");
		g("CODE KOPIEREN", "COPY CODE"); g("ABMELDEN", "SIGN OUT"); g("SPEICHERN", "SAVE"); g("Abgemeldet.", "Signed out.");
		g("App-ID gespeichert.", "App ID saved."); g("● ANGEMELDET", "● SIGNED IN"); g("○ NICHT ANGEMELDET", "○ NOT SIGNED IN");
		g("○ NOCH NICHT FREIGESCHALTET", "○ NOT ACTIVATED YET"); g("AZURE-APP-ID (CLIENT-ID)", "AZURE APP ID (CLIENT ID)");
		g("Anmeldung nur auf der offiziellen Microsoft-Seite – Bambus sieht nie dein Passwort.", "Sign-in only on the official Microsoft page – Bambus never sees your password.");
		g("Melde dich einmal an – danach startet SPIELEN Minecraft direkt.", "Sign in once – after that PLAY starts Minecraft directly.");
		g("Ohne Anmeldung geht alles wie bisher über den offiziellen Minecraft Launcher.", "Without signing in, everything works as before via the official Minecraft Launcher.");
		g("Bis dahin startet Bambus über den offiziellen Minecraft Launcher – das funktioniert immer.", "Until then Bambus starts via the official Minecraft Launcher – that always works.");
		g("Zugangsdaten liegen nur auf diesem Computer (Benutzerordner/.bambus-launcher).", "Sign-in data stays on this computer only (user folder/.bambus-launcher).");
		g("✓  DIREKT STARTEN (ohne Minecraft Launcher)", "✓  START DIRECTLY (without Minecraft Launcher)");
		g("DIREKT STARTEN: AUS (über Minecraft Launcher)", "START DIRECTLY: OFF (via Minecraft Launcher)");
		g("Verbinde mit Microsoft …", "Connecting to Microsoft …"); g("Warte auf deine Anmeldung bei Microsoft …", "Waiting for your Microsoft sign-in …");
		g("Anmeldung fehlgeschlagen.", "Sign-in failed."); g("SPIELEN startet Minecraft jetzt direkt aus Bambus – Spiel und Java kommen frisch von Mojang.", "PLAY now starts Minecraft directly from Bambus – game and Java come fresh from Mojang.");
		teil("Angemeldet als ", "Signed in as "); teil(". Viel Spaß!", ". Have fun!"); teil("Öffne ", "Open "); teil(" und gib diesen Code ein:", " and enter this code:");

		// ---------------------------------------------------------------- Willkommen
		g("WILLKOMMEN BEI", "WELCOME TO"); g("Lass uns deinen Client in vier kurzen Schritten einrichten.", "Let's set up your client in four quick steps.");
		g("WÄHLE DEINE SPRACHE", "CHOOSE YOUR LANGUAGE"); g("DEIN MINECRAFT-NAME", "YOUR MINECRAFT NAME");
		g("Für die 3D-Vorschau mit deinem Skin. Du kannst das auch überspringen.", "For the 3D preview with your skin. You can also skip this.");
		g("Minecraft-Name, z. B. Steve", "Minecraft name, e.g. Steve"); g("DEIN LOOK", "YOUR LOOK");
		g("Hintergrund im Launcher und im Bambus-Hauptmenü", "Background in the launcher and in the Bambus main menu");
		g("Farbe der Anzeigen im Spiel", "Color of the displays in game"); g("LEISTUNG", "PERFORMANCE");
		g("Wie viel Arbeitsspeicher Minecraft bekommt", "How much memory Minecraft gets");
		g("FPS-Boost: Sodium, Lithium & Co. für flüssigeres Spielen", "FPS boost: Sodium, Lithium & more for smoother gameplay");
		g("ALLES BEREIT!", "ALL SET!"); g("Du kannst alles später unter EINSTELLUNGEN ändern.", "You can change everything later under SETTINGS.");
		g("WEITER", "NEXT"); g("ZURÜCK", "BACK"); g("ÜBERSPRINGEN", "SKIP"); g("LOS GEHT'S", "LET'S GO");
		g("Gefunden", "Found"); g("Suche …", "Searching …"); g("Nicht gefunden", "Not found"); g("empfohlen", "recommended");
		teil("Dein Computer hat ", "Your computer has "); teil(" GB Arbeitsspeicher", " GB of memory");
		g("SCHRITT", "STEP"); teil("SCHRITT ", "STEP "); teil(" VON ", " OF ");
		g("NEON", "NEON"); g("OZEAN", "OCEAN"); g("SAKURA", "SAKURA"); g("GOLD", "GOLD"); g("LILA", "PURPLE"); g("EIS", "ICE"); g("RGB", "RGB");

		teil("▶  SPIELEN", "▶  PLAY"); teil("Zuletzt geladen", "Last loaded"); teil("Dein Skin", "Your skin");
		teil("Aus deinem Minecraft-Konto", "From your Minecraft account"); teil("Standard-Skin", "Default skin");
		teil("Adresse nicht gefunden", "Address not found"); teil("Nicht erreichbar", "Not reachable"); teil("Keine Antwort", "No answer");
		g("Willkommens-Seite beim ersten Start – und der Launcher spricht jetzt auch Englisch", "Welcome page on first start – and the launcher now speaks English too");
		TEILE.sort((a, b) -> b[0].length() - a[0].length());
	}
}
