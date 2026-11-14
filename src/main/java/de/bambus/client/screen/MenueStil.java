package de.bambus.client.screen;

import de.bambus.client.BambusClient;
import de.bambus.client.Farben;

/**
 * Aussehen der Bambus-Menüs (Mod-Menü, Garderobe, Serverauswahl …) – einstellbar unter Mehr → DESIGN.
 * Das HUD im Spiel bleibt davon unberührt: Es setzt mit {@link Farben#themaAnwenden} immer die Bambus-Farben.
 */
public enum MenueStil {
	BAMBUS("bambus", "Bambus", "Neon mit Leuchten und Sci-Fi-Ecken"),
	CLEAN("clean", "Clean", "Schlicht, weich gerundet, ohne Leuchten"),
	KOMPAKT("kompakt", "Kompakt", "Tiefschwarz, kantig, kräftige Akzente");

	public final String id, name, beschreibung;

	MenueStil(String id, String name, String beschreibung) {
		this.id = id;
		this.name = name;
		this.beschreibung = beschreibung;
	}

	/** Stil, mit dem gerade gezeichnet wird (wird pro Bild von {@link #anwenden()} gesetzt). */
	private static MenueStil aktiv = BAMBUS;

	public static MenueStil aktiv() {
		return aktiv;
	}

	public static boolean bambus() {
		return aktiv == BAMBUS;
	}

	public static boolean clean() {
		return aktiv == CLEAN;
	}

	public static boolean kompakt() {
		return aktiv == KOMPAKT;
	}

	public static MenueStil von(String id) {
		for (MenueStil s : values()) if (s.id.equals(id)) return s;
		return BAMBUS;
	}

	public MenueStil naechster(int richtung) {
		MenueStil[] alle = values();
		return alle[Math.floorMod(ordinal() + richtung, alle.length)];
	}

	/** Am Anfang jedes Menü-Bildes: Farbthema und danach die Flächen und Akzente des Stils setzen. */
	public static void anwenden() {
		Farben.themaAnwenden(BambusClient.config.thema);   // setzt auch die Bambus-Flächen zurück
		aktiv = von(BambusClient.config.menueStil);
		switch (aktiv) {
			case CLEAN -> {
				int thema = Farben.NEON;
				Farben.NEON = 0xFFF1F4F8;                          // ruhiges Weiß als Hauptfarbe
				Farben.NEON_CYAN = Farben.mische(thema, 0xFFFFFFFF, 0.35f);   // Thema nur als sanfter Akzent
				Farben.NEON_PINK = Farben.mische(Farben.NEON_PINK, 0xFFFFFFFF, 0.35f);
				Farben.GRUEN = Farben.NEON;
				// durchsichtig wie Glas: das Spiel scheint durch
				Farben.HINTERGRUND = 0x4D000000;
				Farben.PANEL = 0xB3141619;
				Farben.KARTE = 0x99262A30;
				Farben.KARTE_HOVER = 0xB3343941;
				Farben.LINIE = 0x2EFFFFFF;
			}
			case KOMPAKT -> {
				Farben.HINTERGRUND = 0xD0000000;
				Farben.PANEL = 0xF7090A0C;
				Farben.KARTE = 0xFF131417;
				Farben.KARTE_HOVER = 0xFF1C1E22;
				Farben.LINIE = 0xFF26282D;
			}
			default -> { }
		}
	}
}
