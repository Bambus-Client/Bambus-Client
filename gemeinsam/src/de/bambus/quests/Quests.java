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

	public static int preis(String id) {
		String[] s = finde(SHOP, id);
		return s == null ? 0 : Integer.parseInt(s[3]);
	}
}
