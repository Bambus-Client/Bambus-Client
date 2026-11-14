package de.bambus.launcher;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Liest und schreibt die Einstellungen des Bambus Clients (config/bambus.json im Spielordner),
 * damit man Funktionen und Kosmetik schon im Launcher wählen kann.
 * Die Namen müssen zu BambusConfig in der Mod passen.
 */
final class ClientEinstellungen {

	/** HUD-Module mit ihren Standardpositionen (wie in der Mod). */
	static final String[][] MODULE = {   // {id, Name, Beschreibung, x, y, Standard an}
			{"titel", "Bambus-Schriftzug", "„Bambus Client“ im HUD", "4", "4", "true"},
			{"fps", "FPS", "Bilder pro Sekunde", "4", "19", "true"},
			{"cps", "CPS", "Klicks pro Sekunde, links und rechts", "4", "34", "true"},
			{"tasten", "Keystrokes", "W A S D, Maus und Leertaste", "4", "54", "true"},
			{"server", "Server", "Adresse des Servers", "4", "158", "true"},
			{"ping", "Ping", "Dein Ping mit Signalbalken", "4", "172", "true"},
			{"ruestung", "Rüstungsanzeige", "Rüstung mit Haltbarkeit", "10000", "4", "true"},
			{"hand", "Item in der Hand", "Haltbarkeit bzw. Anzahl deines Items", "10000", "74", "true"},
			{"effekte", "Trank-Effekte", "Aktive Tränke mit Restzeit", "10000", "100", "true"},
			{"ziel", "Target-HUD", "Name, Leben und Abstand deines Ziels", "10000", "60", "true"},
			{"combo", "Combo-Zähler", "Treffer in Folge", "4", "120", "true"},
			{"reach", "Reach-Anzeige", "Abstand beim letzten Treffer", "4", "134", "true"},
			{"tap", "W-/S-Tap", "Sprint-Reset vor dem Treffer?", "4", "148", "false"},
			{"kills", "Kill-Zähler", "Kills, Tode, K/D der Sitzung", "4", "162", "false"},
			{"pfeile", "Pfeil-Zähler", "Pfeile im Inventar", "4", "176", "true"},
			{"cooldown", "Item-Cooldowns", "Enderperle, Schild & Co.", "10000", "200", "true"},
			{"richtung", "Richtung", "Himmelsrichtung und Winkel", "4", "60", "true"},
			{"biom", "Biom", "In welchem Biom du bist", "4", "74", "false"},
			{"tempo", "Tempo", "Blöcke pro Sekunde", "4", "88", "false"},
			{"uhr", "Uhr", "Echte Uhrzeit", "4", "102", "false"},
			{"speicher", "Arbeitsspeicher", "RAM von Minecraft", "4", "116", "false"},
			{"tnt", "TNT-Timer", "Restzeit von gezündetem TNT", "10000", "260", "true"},
			{"kompass", "Kompass-Balken", "Himmelsrichtungen gleiten vorbei", "10000", "2", "false"},
			{"koordinaten", "Koordinaten", "Mit Nether-Umrechnung und Kopier-Taste", "4", "190", "false"},
			{"spielzeit", "Spielzeit", "Zeit seit dem Betreten der Welt", "4", "245", "false"},
			{"ingametag", "Ingame-Tag", "Tag in der Welt, auf Wunsch mit Uhrzeit", "4", "350", "false"},
	};

	/** {Schlüssel, Name, Beschreibung, Standard, Warnung} */
	static final String[][] FUNKTIONEN = {
			{"toggleSprint", "Toggle Sprint", "Sprint-Taste 1× = dauerhaft", "false", ""},
			{"toggleSneak", "Toggle Sneak", "Schleich-Taste 1× = dauerhaft", "false", ""},
			{"scrollZoom", "Zoom mit Mausrad", "C halten · Mausrad zoomt", "true", ""},
			{"freelook", "Freelook", "Linke Alt halten · umschauen", "true", "Nicht auf jedem Server erlaubt"},
			{"fullbright", "Fullbright", "Alles hell, auch in Höhlen", "false", "Nicht auf jedem Server erlaubt"},
			{"fadenkreuzAn", "Fadenkreuz", "Eigene Form & Farbe", "false", ""},
			{"trefferPartikel", "Treffer-Partikel", "Extra Funken beim Treffen", "true", ""},
			{"lowEnd", "Low-End-Modus", "Sparsame Grafik für langsame PCs", "false", ""},
			{"gegnerLebenZahl", "Gegner-Leben als Zahl", "Genaue ❤ im Target-HUD", "false", "Nicht auf jedem Server erlaubt"},
			{"autoScreenshot", "Auto-Screenshot", "Screenshot bei jedem Spieler-Kill", "false", ""},
			{"verlauf", "Farbverlauf", "HUD-Kästen mit Farbverlauf", "true", ""},
			{"helferAn", "Bambus-Helfer", "Fragen & Tipps bei Rechts-Shift", "true", ""},
			{"bambusHauptmenue", "Bambus-Hauptmenü", "Eigener Startbildschirm im Spiel", "true", ""},
			{"ladebildschirm", "Bambus-Ladebildschirm", "Mit Tipps beim Betreten einer Welt", "true", ""},
			{"hinweise", "Benachrichtigungen", "Neon-Hinweise im Spiel", "true", ""},
			{"menueAnimation", "Weiche Animationen", "Menüs und Schalter gleiten", "true", ""},
			{"capeAn", "Umhang anzeigen", "Deinen Umhang im Spiel zeigen", "true", ""},
	};

	/** Textur-Modul: PvP-Anpassungen wie in Texturpaketen (nur für dich sichtbar). */
	static final String[][] TEXTUREN = {   // {id, Name, Beschreibung, Standard an}
			{"lowFire", "Low Fire", "Flammen tiefer – du siehst mehr", "false"},
			{"lowShield", "Low Shield", "Schild tiefer – freiere Sicht", "false"},
			{"sideShield", "Side Shield", "Schild seitlich gedreht", "false"},
			{"shortSword", "Short Sword", "Kürzeres Schwert in der Hand", "false"},
			{"keinKuerbisOverlay", "Kein Kürbis-Overlay", "Kürbis auf dem Kopf stört nicht", "false"},
			{"klaresWasser", "Klare Sicht im Wasser", "Unter Wasser klar sehen", "false"},
	};

	/** {id, Name, Farbe} – wie Kosmetik.CAPES in der Mod */
	static final String[][] CAPES = {
			{"bambus", "Bambus", "3DFF8A"},
			{"neon", "Neon-Platine", "3DFF8A"},
			{"cyber", "Cyber", "00E5FF"},
			{"galaxie", "Galaxie", "B06BFF"},
			{"sunset", "Synthwave", "FF2BD6"},
			{"matrix", "Matrix", "39FF14"},
			{"polarlicht", "Polarlicht", "5AFFC0"},
			{"glut", "Glut", "FF8A3D"},
			// Nur im Quest-Shop (siehe Fortschritt.SHOP)
			{"gold", "Goldhalm", "FFC846"},
			{"eis", "Eiskristall", "AEEBFF"},
			{"prisma", "Lichtkristall", "B06BFF"},
	};

	/** {id, Name, Farbe} – wie Kosmetik.FLUEGEL in der Mod */
	static final String[][] FLUEGEL = {
			{"engel", "Engel", "00E5FF"},
			{"drache", "Drache", "FF2BD6"},
			{"cyber", "Cyber", "3DFF8A"},
			{"schmetterling", "Schmetterling", "FFD54A"},
			{"phoenix", "Phönix", "FF8A3D"},
			{"seraph", "Seraph", "FFC846"},
			{"nachtschwinge", "Nachtschwinge", "B040FF"},
			{"kristall", "Kristall", "5AE6FF"},
			{"aurora", "Aurora", "3DFFC8"},
			{"sakura", "Sakura", "FF8CC0"},
			{"bambus", "Bambus", "7BD65C"},
			// Flügel-Paket 1 (Pixel-Look, quadratische Bilder aus werkzeuge/fluegel_aus_bbmodel.py)
			{"engel_classic", "Engel Classic", "E6ECF5"},
			{"drache_classic", "Drache Classic", "C0303C"},
			{"fee", "Fee", "8A6BFF"},
			{"phoenix_classic", "Phönix Classic", "FFB03D"},
	};

	/** Größere Flügel (wie Kosmetik.fluegelGroesse in der Mod); alle anderen 1. */
	static double fluegelGroesse(String id) {
		switch (id == null ? "" : id) {
			case "engel_classic": case "drache_classic": case "phoenix_classic": return 1.45;
			case "fee": return 1.35;
			default: return 1;
		}
	}

	/** {id, Name, Farbe} – wie Kosmetik.PARTIKEL in der Mod */
	static final String[][] PARTIKEL = {
			{"funken", "Neon-Funken", "00E5FF"},
			{"glitzer", "Sternenstaub", "FFFFFF"},
			{"herzen", "Herzen", "FF4D6D"},
			{"noten", "Musik", "3DFF8A"},
			{"seelen", "Seelenfeuer", "00E5FF"},
			{"kirsche", "Kirschblüten", "FF9BD6"},
			{"magie", "Magie", "B06BFF"},
			{"flammen", "Flammen", "FF8A3D"},
			// Nur im Quest-Shop
			{"komet", "Kometen", "FFFFFF"},
			{"zauber", "Drachenatem", "B06BFF"},
	};

	static final String AUS = "aus";

	/** {id, Name, Farbe} – wie Kosmetik.HUETE / KILL_EFFEKTE / SPUREN in der Mod */
	static final String[][] HUETE = {
			{"krone", "Krone", "FFD54A"}, {"zylinder", "Zylinder", "3DFF8A"}, {"heiligenschein", "Heiligenschein", "FFF3A0"},
			{"hoerner", "Hörner", "FF4D6D"}, {"katzenohren", "Katzenohren", "FF9BD6"}, {"partyhut", "Partyhut", "B06BFF"},
			{"bandana", "Bandana", "FF2BD6"}, {"visier", "Neon-Visier", "00E5FF"},
	};
	static final String[][] KILL_EFFEKTE = {
			{"blitz", "Neon-Blitz", "00E5FF"}, {"feuerwerk", "Feuerwerk", "FFD54A"}, {"seelen", "Seelen", "00E5FF"},
			{"flammen", "Flammen", "FF8A3D"}, {"herzen", "Herzen", "FF4D6D"}, {"totem", "Totem", "3DFF8A"},
	};
	static final String[][] SPUREN = {
			{"funken", "Neon-Funken", "00E5FF"}, {"glitzer", "Sternenstaub", "FFFFFF"}, {"flammen", "Flammen", "FF8A3D"},
			{"seelen", "Seelenfeuer", "00E5FF"}, {"noten", "Musik", "3DFF8A"}, {"magie", "Magie", "B06BFF"},
			{"herzen", "Herzen", "FF4D6D"},
	};

	String hut() { return text("hut", AUS); }
	void setzeHut(String id) { setze("hut", id); }
	String killEffekt() { return text("killEffekt", AUS); }
	void setzeKillEffekt(String id) { setze("killEffekt", id); }
	String spur() { return text("spur", AUS); }
	void setzeSpur(String id) { setze("spur", id); }

	private final Path datei;
	private Map<String, Object> daten = new LinkedHashMap<>();

	ClientEinstellungen(Path spielOrdner) {
		this.datei = spielOrdner.resolve("config").resolve("bambus.json");
		laden();
	}

	void laden() {
		daten = new LinkedHashMap<>();
		try {
			if (Files.isRegularFile(datei)) {
				Map<String, Object> m = Json.objekt(Json.parse(
						new String(Files.readAllBytes(datei), StandardCharsets.UTF_8)));
				if (m != null) {
					daten = m;
				}
			}
		} catch (Exception e) {
			daten = new LinkedHashMap<>();
		}
	}

	void speichern() {
		try {
			Files.createDirectories(datei.getParent());
			Path tmp = datei.resolveSibling("bambus.json.tmp");
			Files.write(tmp, Json.write(daten).getBytes(StandardCharsets.UTF_8));
			Files.move(tmp, datei, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			System.err.println("[Bambus] Einstellungen nicht gespeichert: " + e);
		}
	}

	// ------------------------------------------------------------ Schalter

	boolean wert(String schluessel, boolean standard) {
		Object o = daten.get(schluessel);
		return o instanceof Boolean ? (Boolean) o : standard;
	}

	void setze(String schluessel, boolean wert) {
		daten.put(schluessel, wert);
		speichern();
	}

	String text(String schluessel, String standard) {
		Object o = daten.get(schluessel);
		return o instanceof String ? (String) o : standard;
	}

	void setze(String schluessel, String wert) {
		daten.put(schluessel, wert);
		speichern();
	}

	int zahl(String schluessel, int standard) {
		Object o = daten.get(schluessel);
		return o instanceof Number ? ((Number) o).intValue() : standard;
	}

	void setze(String schluessel, int wert) {
		daten.put(schluessel, wert);
		speichern();
	}

	// -------------------------------------------------------------- Server-Favoriten

	static final String[] STANDARD_SERVER = {"Hypixel|mc.hypixel.net", "GommeHD|gommehd.net", "GrieferGames|griefergames.net", "Minemen|minemen.club"};

	/** Favoriten als „Name|Adresse“. Die Mod trägt sie beim Start in die Mehrspieler-Liste ein. */
	java.util.List<String> serverFavoriten() {
		java.util.List<Object> l = Json.liste(daten.get("serverFavoriten"));
		java.util.List<String> aus = new java.util.ArrayList<>();
		if (l == null) {
			java.util.Collections.addAll(aus, STANDARD_SERVER);
		} else {
			for (Object o : l) if (o instanceof String) aus.add((String) o);
		}
		return aus;
	}

	void setzeServerFavoriten(java.util.List<String> liste) {
		daten.put("serverFavoriten", new java.util.ArrayList<Object>(liste));
		speichern();
	}

	// -------------------------------------------------------------- Module

	private Map<String, Object> modul(String id) {
		Map<String, Object> module = Json.objekt(daten.get("module"));
		if (module == null) {
			module = new LinkedHashMap<>();
			daten.put("module", module);
		}
		Map<String, Object> m = Json.objekt(module.get(id));
		if (m == null) {
			m = new LinkedHashMap<>();
			m.put("an", true);
			for (String[] eintrag : MODULE) {
				if (eintrag[0].equals(id)) {
					m.put("an", Boolean.parseBoolean(eintrag[5]));
					m.put("x", Long.parseLong(eintrag[3]));
					m.put("y", Long.parseLong(eintrag[4]));
				}
			}
			module.put(id, m);
		}
		return m;
	}

	boolean modulAn(String id) {
		Object o = modul(id).get("an");
		if (o instanceof Boolean) return (Boolean) o;
		for (String[] m : MODULE) if (m[0].equals(id)) return Boolean.parseBoolean(m[5]);
		return true;
	}

	void setzeModul(String id, boolean an) {
		modul(id).put("an", an);
		speichern();
	}

	// ------------------------------------------------------------ Kosmetik

	String cape() {
		if (!wert("capeAn", true)) {
			return AUS;
		}
		return text("capeDesign", "bambus");
	}

	void setzeCape(String id) {
		daten.put("capeAn", true);
		daten.put("capeDesign", id);
		speichern();
	}

	String fluegel() {
		return text("fluegel", AUS);
	}

	void setzeFluegel(String id) {
		setze("fluegel", id);
	}

	String partikel() {
		return text("partikel", AUS);
	}

	void setzePartikel(String id) {
		setze("partikel", id);
	}

	static String name(String[][] liste, String id) {
		for (String[] e : liste) {
			if (e[0].equals(id)) {
				return e[1];
			}
		}
		return "Keins";
	}

	int anzahlAktiv() {
		int n = 0;
		for (String[] m : MODULE) {
			if (modulAn(m[0])) n++;
		}
		for (String[] f : FUNKTIONEN) {
			if (wert(f[0], Boolean.parseBoolean(f[3]))) n++;
		}
		return n;
	}

	static int anzahlGesamt() {
		return MODULE.length + FUNKTIONEN.length;
	}
}
