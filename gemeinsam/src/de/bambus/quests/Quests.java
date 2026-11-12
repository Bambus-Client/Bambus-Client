package de.bambus.quests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Quests, Ränge und Shop – die gemeinsame Grundlage für Launcher und Mod.
 * Hier stehen nur Daten und Rechnerei, kein Speichern: der Launcher nutzt Fortschritt,
 * die Mod nutzt Blaetter. Punkte heißen „Blätter“ und kosten nie Geld.
 */
public final class Quests {

	/** {Name, Blätter ab, Farbe}. Der Rang kommt vom insgesamt Gesammelten. */
	public static final String[][] RAENGE = {
			{"Bronze", "0", "CD7F32"},
			{"Silber", "150", "C0C8D4"},
			{"Gold", "400", "FFC846"},
			{"Platin", "900", "7DF9FF"},
			{"Diamant", "1800", "4FE3E0"},
			{"Meister", "3000", "B06BFF"},
			{"Legende", "5000", "FF2BD6"},
	};

	/** Tages-Quests im Launcher: {id, Name, Hinweis, Blätter}. Drei davon gibt es jeden Tag. */
	public static final String[][] TAGES = {
			{"garderobe", "Garderobe besuchen", "Öffne die Seite GARDEROBE", "15"},
			{"cape", "Neuer Umhang", "Zieh einen anderen Umhang an", "20"},
			{"fluegel", "Abheben", "Zieh Flügel an oder wechsle den Stil", "20"},
			{"helfer", "Frag den Helfer", "Stell dem Helfer eine Frage", "15"},
			{"hintergrund", "Tapetenwechsel", "Wähle einen anderen Hintergrund", "15"},
			{"thema", "Farbe bekennen", "Wähle eine andere Farbe", "15"},
			{"modul", "Am HUD schrauben", "Schalte ein Modul an oder aus", "15"},
			{"server", "Server prüfen", "Aktualisiere die Server-Liste", "15"},
			{"mods", "Mod-Regal", "Öffne die Seite MODS", "10"},
			{"neu", "Was ist neu?", "Lies die NEUIGKEITEN", "10"},
	};

	/**
	 * Herausforderungen im Spiel: {id, Name, Hinweis, Blätter, Ziel}.
	 * Drei davon sind jeden Tag dran; die Mod erkennt sie selbst und zählt mit.
	 */
	public static final String[][] HART = {
			{"sieg", "Sieger", "Gewinne eine Runde, z. B. Bedwars", "100", "1"},
			{"mlg_leiter", "Leiter-MLG", "Fang einen Sturz ab 8 Blöcken mit einer Leiter ab", "70", "1"},
			{"mlg_wasser", "Wasser-MLG", "Fang einen Sturz ab 8 Blöcken mit Wasser ab", "55", "1"},
			{"kills5", "Jäger", "Besiege 5 Spieler", "70", "5"},
			{"combo5", "Combo-Meister", "Lande eine 5er-Combo", "45", "5"},
			{"fall20", "Harter Aufprall", "Überlebe einen Sturz aus 20 Blöcken", "35", "1"},
			{"tief", "Tiefgräber", "Grab hinunter bis Höhe Y −58", "30", "1"},
			{"nether", "Höllentor", "Betritt den Nether", "30", "1"},
			{"sprint1000", "Dauerläufer", "Sprinte 1000 Blöcke weit", "40", "1000"},
			{"elytra500", "Himmelsstürmer", "Flieg 500 Blöcke mit der Elytra", "60", "500"},
	};

	/** Erfolge: {id, Name, Hinweis, Blätter}. Gibt es nur einmal. */
	public static final String[][] ERFOLGE = {
			{"erstesMal", "Willkommen", "Bambus zum ersten Mal gestartet", "25"},
			{"woche", "Stammgast", "7 Tage am Stück eingeloggt", "100"},
			{"bild", "Eigener Look", "Eigenes Hintergrundbild gesetzt", "40"},
			{"studio", "Selbst gemacht", "Eigenes Cape oder eigene Flügel erstellt", "60"},
			{"server3", "Gut vernetzt", "3 Lieblingsserver eingetragen", "30"},
			{"module10", "Voll ausgestattet", "10 Module gleichzeitig an", "30"},
			{"alleFarben", "Regenbogen", "Alle 7 Farben ausprobiert", "50"},
			{"fragen10", "Neugierig", "10 Fragen an den Helfer gestellt", "40"},
			{"spiel60", "Eine Stunde Bambus", "60 Minuten im Spiel", "50"},
			{"drache", "Drachentöter", "Den Enderdrachen besiegt", "150"},
	};

	/** Shop: {id, Art (cape/partikel), Name, Blätter}. Nur mit Blättern, nie mit Geld. */
	public static final String[][] SHOP = {
			{"gold", "cape", "Goldhalm", "250"},
			{"eis", "cape", "Eiskristall", "400"},
			{"prisma", "cape", "Lichtkristall", "700"},
			{"komet", "partikel", "Kometen", "200"},
			{"zauber", "partikel", "Drachenatem", "350"},
	};

	/** Das Start-Set gehört dir von Anfang an und kostet nie etwas: {Art, id}. */
	public static final String[][] STARTSET = {
			{"cape", "bambus"},
			{"fluegel", "engel_classic"},
			{"hut", "bandana"},
			{"partikel", "funken"},
			{"spur", "funken"},
	};

	/** Grundpreis je Art in Blättern; einzelne Stücke im SHOP haben eigene Preise. */
	private static final String[][] PREISE = {
			{"cape", "150"}, {"fluegel", "220"}, {"hut", "90"},
			{"partikel", "110"}, {"spur", "110"}, {"killeffekt", "110"},
	};

	/**
	 * Saison-Sets: {id, Name, Monate, Preis, Cape, Flügel, Hut, Partikel}.
	 * Während der Saison gibt es das ganze Set günstiger; einzeln kann man die Stücke immer kaufen.
	 */
	public static final String[][] SAISONS = {
			{"fruehling", "Frühling", "3,4,5", "400", "polarlicht", "sakura", "katzenohren", "kirsche"},
			{"sommer", "Sommer", "6,7,8", "400", "sunset", "schmetterling", "partyhut", "herzen"},
			{"herbst", "Herbst", "9,10,11", "400", "glut", "phoenix", "hoerner", "flammen"},
			{"winter", "Winter", "12,1,2", "580", "eis", "kristall", "krone", "glitzer"},
	};

	public static final int STUFE_MAX = 5;

	public static final int TAGES_ANZAHL = 3;
	public static final int HART_ANZAHL = 3;

	private Quests() {}

	/** Die Auswahl des Tages – überall gleich, weil nur das Datum entscheidet. */
	public static List<String[]> auswahl(String[][] pool, long tag, int anzahl) {
		List<String[]> alle = new ArrayList<>(Arrays.asList(pool));
		Collections.shuffle(alle, new Random(tag * 2654435761L + pool.length));
		return alle.subList(0, Math.min(anzahl, alle.size()));
	}

	public static String[] finde(String[][] pool, String id) {
		for (String[] e : pool) if (e[0].equals(id)) return e;
		return null;
	}

	public static int rangNummer(int gesammelt) {
		int r = 0;
		for (int i = 0; i < RAENGE.length; i++) if (gesammelt >= Integer.parseInt(RAENGE[i][1])) r = i;
		return r;
	}

	public static String rangName(int gesammelt) { return RAENGE[rangNummer(gesammelt)][0]; }

	public static int rangFarbe(int gesammelt) {
		return 0xFF000000 | Integer.parseInt(RAENGE[rangNummer(gesammelt)][2], 16);
	}

	/** 0..1 bis zum nächsten Rang; 1, wenn der höchste erreicht ist. */
	public static float rangFortschritt(int gesammelt) {
		int r = rangNummer(gesammelt);
		if (r >= RAENGE.length - 1) return 1f;
		int von = Integer.parseInt(RAENGE[r][1]), bis = Integer.parseInt(RAENGE[r + 1][1]);
		return Math.max(0f, Math.min(1f, (gesammelt - von) / (float) (bis - von)));
	}

	/** Noch fehlende Blätter bis zum nächsten Rang, oder 0. */
	public static int bisNaechsterRang(int gesammelt) {
		int r = rangNummer(gesammelt);
		return r >= RAENGE.length - 1 ? 0 : Integer.parseInt(RAENGE[r + 1][1]) - gesammelt;
	}

	public static boolean imShop(String id) { return finde(SHOP, id) != null; }

	/** Gehört das Stück zum Start-Set? Dann ist es immer frei. */
	public static boolean imStartset(String id) {
		for (String[] e : STARTSET) if (e[1].equals(id)) return true;
		return false;
	}

	/** Preis eines Kosmetik-Stücks: eigener Preis aus dem SHOP, sonst der Grundpreis seiner Art. */
	public static int preis(String art, String id) {
		String[] s = finde(SHOP, id);
        if (s != null) return Integer.parseInt(s[3]);
		for (String[] p : PREISE) if (p[0].equals(art)) return Integer.parseInt(p[1]);
		return 150;
	}

	public static int preis(String id) { return preis("", id); }

	// ================================================================ Saisons

	/** Die Saison zum Monat (1–12). */
	public static String[] saison(int monat) {
		for (String[] s : SAISONS) {
			for (String m : s[2].split(",")) if (Integer.parseInt(m.trim()) == monat) return s;
		}
		return SAISONS[0];
	}

	/** Die vier Stücke eines Saison-Sets, je {Art, id}. */
	public static String[][] saisonStuecke(String[] s) {
		return new String[][]{{"cape", s[4]}, {"fluegel", s[5]}, {"hut", s[6]}, {"partikel", s[7]}};
	}

	/** Was die Stücke einzeln kosten würden. */
	public static int saisonEinzeln(String[] s) {
		int summe = 0;
		for (String[] st : saisonStuecke(s)) summe += preis(st[0], st[1]);
		return summe;
	}

	// ================================================================ Stufen

	/** Ziel einer Herausforderung auf der gewünschten Stufe (Stufe 1 = Grundziel). */
	public static int ziel(String[] q, int stufe) {
		return Integer.parseInt(q[4]) * Math.max(1, stufe);
	}

	/** Blätter für eine Herausforderung auf der gewünschten Stufe. */
	public static int lohn(String[] q, int stufe) {
		int grund = Integer.parseInt(q[3]);
		return grund + (Math.max(1, stufe) - 1) * Math.round(grund * 0.6f);
	}
}
