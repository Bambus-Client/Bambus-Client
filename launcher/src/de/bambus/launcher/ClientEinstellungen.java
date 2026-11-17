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
			{"musik", "Musik (Spotify)", "Song, Songtext und 3D-Wörter wie ein Musikvideo", "20000", "3", "false"},
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
	/** Änderungszeit der Datei beim letzten Lesen oder Schreiben – ändert sie sich, hat die Mod gespeichert. */
	private long bekannteAenderung = Long.MIN_VALUE;
	private long letztePruefung;

	ClientEinstellungen(Path spielOrdner) {
		this.datei = spielOrdner.resolve("config").resolve("bambus.json");
		laden();
	}

	/**
	 * Liest bambus.json neu. Die Mod schreibt dieselbe Datei, während das Spiel läuft – darum wird vor jeder
	 * Änderung frisch gelesen (siehe {@link #frisch}). Ist die Datei gerade unlesbar, bleibt der bisherige
	 * Stand im Speicher; es wird nie ein leerer Stand gespeichert, der alles überschreibt.
	 */
	synchronized void laden() {
		if (!Files.isRegularFile(datei)) {
			bekannteAenderung = aenderungszeit();
			return;
		}
		for (int versuch = 0; versuch < 4; versuch++) {
			try {
				long zeit = aenderungszeit();
				Map<String, Object> m = Json.objekt(Json.parse(new String(Files.readAllBytes(datei), StandardCharsets.UTF_8)));
				if (m != null) {
					daten = m;
					bekannteAenderung = zeit;
					return;
				}
			} catch (Exception e) {
				// vielleicht schreibt die Mod gerade – kurz warten und noch einmal
			}
			try {
				Thread.sleep(40);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			}
		}
		System.err.println("[Bambus] bambus.json ist gerade nicht lesbar – der bisherige Stand bleibt erhalten.");
	}

	/** Neu lesen, wenn die Mod die Datei inzwischen geändert hat. sofort = false: höchstens zweimal pro Sekunde prüfen. */
	private synchronized void frisch(boolean sofort) {
		long jetzt = System.currentTimeMillis();
		if (!sofort && jetzt - letztePruefung < 500) return;
		letztePruefung = jetzt;
		if (aenderungszeit() != bekannteAenderung) laden();
	}

	private long aenderungszeit() {
		try {
			return Files.getLastModifiedTime(datei).toMillis() * 31 + Files.size(datei);
		} catch (IOException e) {
			return -1;
		}
	}

	synchronized void speichern() {
		try {
			Files.createDirectories(datei.getParent());
			Path tmp = datei.resolveSibling("bambus.json.tmp");
			Files.write(tmp, Json.write(daten).getBytes(StandardCharsets.UTF_8));
			try {
				Files.move(tmp, datei, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			} catch (IOException e) {
				Files.move(tmp, datei, StandardCopyOption.REPLACE_EXISTING);
			}
			bekannteAenderung = aenderungszeit();
		} catch (IOException e) {
			System.err.println("[Bambus] Einstellungen nicht gespeichert: " + e);
		}
	}

	/** Zahl erhöhen (oder mit negativem Wert verringern) – frisch gelesen, damit nichts von der Mod verloren geht. */
	synchronized int addiere(String schluessel, int menge) {
		frisch(true);
		Object o = daten.get(schluessel);
		int neu = (o instanceof Number ? ((Number) o).intValue() : 0) + menge;
		daten.put(schluessel, neu);
		speichern();
		return neu;
	}

	// ------------------------------------------------------------ Schalter

	boolean wert(String schluessel, boolean standard) {
		frisch(false);
		Object o = daten.get(schluessel);
		return o instanceof Boolean ? (Boolean) o : standard;
	}

	synchronized void setze(String schluessel, boolean wert) {
		frisch(true);
		daten.put(schluessel, wert);
		speichern();
	}

	String text(String schluessel, String standard) {
		frisch(false);
		Object o = daten.get(schluessel);
		return o instanceof String ? (String) o : standard;
	}

	synchronized void setze(String schluessel, String wert) {
		frisch(true);
		daten.put(schluessel, wert);
		speichern();
	}

	int zahl(String schluessel, int standard) {
		frisch(false);
		Object o = daten.get(schluessel);
		return o instanceof Number ? ((Number) o).intValue() : standard;
	}

	synchronized void setze(String schluessel, int wert) {
		frisch(true);
		daten.put(schluessel, wert);
		speichern();
	}

	// -------------------------------------------------------------- Server-Favoriten

	static final String[] STANDARD_SERVER = {"Hypixel|mc.hypixel.net", "GommeHD|gommehd.net", "GrieferGames|griefergames.net", "Minemen|minemen.club"};

	/** Favoriten als „Name|Adresse“. Die Mod trägt sie beim Start in die Mehrspieler-Liste ein. */
	java.util.List<String> serverFavoriten() {
		frisch(false);
		java.util.List<Object> l = Json.liste(daten.get("serverFavoriten"));
		java.util.List<String> aus = new java.util.ArrayList<>();
		if (l == null) {
			java.util.Collections.addAll(aus, STANDARD_SERVER);
		} else {
			for (Object o : l) if (o instanceof String) aus.add((String) o);
		}
		return aus;
	}

	/** Einstellungs-Profil (0 PvP, 1 Bauen, 2 Eigenes), das die Mod auf diesem Server lädt; -1 = keins. */
	int serverProfil(String adresse) {
		Map<String, Object> m = Json.objekt(daten.get("serverProfile"));
		Object v = m == null ? null : m.get(adresse.toLowerCase(java.util.Locale.ROOT));
		return v instanceof Number ? ((Number) v).intValue() : -1;
	}

	void setzeServerProfil(String adresse, int slot) {
		Map<String, Object> m = Json.objekt(daten.get("serverProfile"));
		if (m == null) {
			m = new java.util.LinkedHashMap<>();
			daten.put("serverProfile", m);
		}
		String k = adresse.toLowerCase(java.util.Locale.ROOT);
		if (slot < 0) m.remove(k);
		else m.put(k, slot);
		speichern();
	}

	/** Gibt es dieses Einstellungs-Profil schon (im Spiel unter Mod-Menü → MEHR → PROFILE gespeichert)? */
	boolean profilVorhanden(int slot) {
		return java.nio.file.Files.isRegularFile(datei.resolveSibling("bambus-profil-" + (slot + 1) + ".json"));
	}

	/** Mit ★ markierte Server (Adressen, klein geschrieben) – dieselbe Liste wie im Spiel. */
	java.util.List<String> serverSterne() {
		frisch(false);
		java.util.List<Object> l = Json.liste(daten.get("serverSterne"));
		java.util.List<String> aus = new java.util.ArrayList<>();
		if (l != null) for (Object o : l) if (o instanceof String) aus.add((String) o);
		return aus;
	}

	synchronized void setzeServerSterne(java.util.List<String> liste) {
		frisch(true);
		daten.put("serverSterne", new java.util.ArrayList<Object>(liste));
		speichern();
	}

	synchronized void setzeServerFavoriten(java.util.List<String> liste) {
		frisch(true);
		daten.put("serverFavoriten", new java.util.ArrayList<Object>(liste));
		speichern();
	}

	// -------------------------------------------------------------- Module

	private Map<String, Object> modul(String id) {
		frisch(false);
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

	synchronized void setzeModul(String id, boolean an) {
		frisch(true);
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

	synchronized void setzeCape(String id) {
		frisch(true);
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
