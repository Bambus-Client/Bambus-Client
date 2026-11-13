package de.bambus.launcher;

import de.bambus.quests.Quests;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Punkte, Quests, Ränge und der Shop – alles kostenlos und nur auf diesem Computer.
 * Gespeichert wird in bambus.json, damit auch die Mod den Rang und die gekauften Sachen kennt.
 * Punkte heißen im Spiel „Blätter“.
 */
final class Fortschritt {

	static final String[][] RAENGE = Quests.RAENGE;
	static final String[][] SHOP = Quests.SHOP;

	private static final int LOGIN_GRUND = 10;      // Blätter fürs Einloggen
	private static final int LOGIN_PRO_TAG = 3;     // dazu pro Tag Serie
	private static final int LOGIN_MAX = 40;

	private final ClientEinstellungen c;
	/** Wird gesetzt, wenn beim Start Blätter dazugekommen sind (für die Begrüßung). */
	int begruessungBlaetter;
	int begruessungTag;

	Fortschritt(ClientEinstellungen c) {
		this.c = c;
	}

	// ============================================================ Punkte und Rang

	int blaetter() { return c.zahl("punkte", 0); }

	int gesammelt() { return c.zahl("punkteGesamt", 0); }

	private void gib(int wieviel) {
		if (wieviel <= 0) return;
		c.setze("punkte", blaetter() + wieviel);
		c.setze("punkteGesamt", gesammelt() + wieviel);
	}

	int rang() { return Quests.rangNummer(gesammelt()); }

	String rangName() { return Quests.rangName(gesammelt()); }

	java.awt.Color rangFarbe() { return new java.awt.Color(Quests.rangFarbe(gesammelt()), true); }

	/** 0..1 bis zum nächsten Rang; 1, wenn der höchste erreicht ist. */
	float rangFortschritt() { return Quests.rangFortschritt(gesammelt()); }

	/** Noch fehlende Blätter bis zum nächsten Rang, oder 0. */
	int bisNaechsterRang() { return Quests.bisNaechsterRang(gesammelt()); }

	// ============================================================ Tages-Bonus

	/** Beim Start aufrufen: zählt die Serie weiter und schenkt die Blätter des Tages. */
	void tagesBonus() {
		stelleUm();
		String heute = LocalDate.now().toString();
		if (heute.equals(c.text("letzterTag", ""))) return;
		boolean gestern = LocalDate.now().minusDays(1).toString().equals(c.text("letzterTag", ""));
		String vorher = c.text("letzterTag", "");
		if (!gestern && !vorher.isEmpty() && c.zahl("streak", 0) > 0) rangVerlust();
		int serie = gestern ? c.zahl("streak", 0) + 1 : 1;
		c.setze("streak", serie);
		c.setze("letzterTag", heute);
		int blaetter = Math.min(LOGIN_MAX, LOGIN_GRUND + (serie - 1) * LOGIN_PRO_TAG);
		gib(blaetter);
		c.setze("letzterBonus", blaetter);
		begruessungBlaetter = blaetter;
		begruessungTag = serie;
		if (serie >= 7) erfolg("woche");
		erfolg("erstesMal");
	}

	int serie() { return c.zahl("streak", 0); }

	/** Wird beim Start gesetzt, wenn die Serie gerissen ist – für die Meldung. */
	String verlorenerRang = "";

	/**
	 * Serie gerissen: ein Rang zurück. Gekaufte Kosmetik bleibt dir erhalten,
	 * nur die gesammelten Blätter fallen auf den Anfang des vorherigen Rangs.
	 */
	private void rangVerlust() {
		int r = rang();
		if (r <= 0) return;
		verlorenerRang = Quests.RAENGE[r][0];
		c.setze("punkteGesamt", Integer.parseInt(Quests.RAENGE[r - 1][1]));
	}

	// ============================================================ Quests

	/** Die drei Launcher-Quests von heute – jeden Tag dieselben drei, aber über die Tage wechselnd. */
	List<String[]> heutigeQuests() {
		return Quests.auswahl(Quests.TAGES, LocalDate.now().toEpochDay(), Quests.TAGES_ANZAHL);
	}

	/** Die drei Herausforderungen von heute (werden im Spiel erledigt). */
	List<String[]> heutigeHart() {
		return Quests.auswahl(Quests.HART, LocalDate.now().toEpochDay(), Quests.HART_ANZAHL);
	}

	/** Im Spiel erledigte Herausforderungen von heute (die Mod schreibt sie). */
	boolean hartFertig(String id) {
		if (!LocalDate.now().toString().equals(c.text("hartTag", ""))) return false;
		return menge("hartFertig").contains(id);
	}

	/** Zwischenstand einer Herausforderung, z. B. 340 von 1000 Blöcken. */
	int hartStand(String id) {
		if (!LocalDate.now().toString().equals(c.text("hartTag", ""))) return 0;
		for (String t : c.text("hartStand", "").split(",")) {
			String[] teil = t.split(":");
			if (teil.length == 2 && teil[0].trim().equals(id)) {
				try {
					return Integer.parseInt(teil[1].trim());
				} catch (NumberFormatException e) {
					return 0;
				}
			}
		}
		return 0;
	}

	private Set<String> menge(String schluessel) {
		Set<String> s = new LinkedHashSet<>();
		for (String t : c.text(schluessel, "").split(",")) if (!t.trim().isEmpty()) s.add(t.trim());
		return s;
	}

	private void merke(String schluessel, Set<String> s) {
		c.setze(schluessel, String.join(",", s));
	}

	/** Erledigte Quests von heute (werden um Mitternacht wieder frei). */
	private Set<String> heuteFertig() {
		if (!LocalDate.now().toString().equals(c.text("questTag", ""))) {
			c.setze("questTag", LocalDate.now().toString());
			c.setze("questFertig", "");
			return new LinkedHashSet<>();
		}
		return menge("questFertig");
	}

	boolean questFertig(String id) { return heuteFertig().contains(id); }

	/**
	 * Meldet eine Tat aus dem Launcher (z. B. „cape“). Erledigt die Quest, falls sie heute dran ist.
	 * Gibt die verdienten Blätter zurück (0, wenn nichts passiert ist).
	 */
	int tat(String id) {
		Set<String> fertig = heuteFertig();
		if (fertig.contains(id)) return 0;
		for (String[] q : heutigeQuests()) {
			if (!q[0].equals(id)) continue;
			fertig.add(id);
			merke("questFertig", fertig);
			int p = Integer.parseInt(q[3]);
			gib(p);
			return p;
		}
		return 0;
	}

	// ============================================================ Erfolge

	boolean erfolgFertig(String id) { return menge("erfolge").contains(id); }

	/** Schaltet einen Erfolg frei; gibt die Blätter zurück (0, wenn er schon da war). */
	int erfolg(String id) {
		Set<String> e = menge("erfolge");
		if (e.contains(id)) return 0;
		for (String[] a : Quests.ERFOLGE) {
			if (!a[0].equals(id)) continue;
			e.add(id);
			merke("erfolge", e);
			int p = Integer.parseInt(a[3]);
			gib(p);
			return p;
		}
		return 0;
	}

	/** Erfolge, die sich am Zustand ablesen lassen – beim Öffnen der Quest-Seite prüfen. */
	int pruefeErfolge(BambusLauncher l) {
		int summe = 0;
		if (c.serverFavoriten().size() >= 3) summe += erfolg("server3");
		if (c.anzahlAktiv() >= 10) summe += erfolg("module10");
		if (menge("farbenGesehen").size() >= NeonUI.THEMEN.length) summe += erfolg("alleFarben");
		if (c.zahl("helferFragen", 0) >= 10) summe += erfolg("fragen10");
		if (c.zahl("spielminuten", 0) >= 60) summe += erfolg("spiel60");
		if (serie() >= 7) summe += erfolg("woche");
		try {
			java.nio.file.Path k = l.installer.spielOrdner.resolve("config").resolve("bambus-kosmetik");
			if (java.nio.file.Files.isDirectory(k)) {
				try (java.util.stream.Stream<java.nio.file.Path> st = java.nio.file.Files.walk(k, 2)) {
					if (st.anyMatch(p -> p.toString().endsWith(".png"))) summe += erfolg("studio");
				}
			}
		} catch (Exception ignoriert) {
			// kein Kosmetik-Ordner, kein Erfolg
		}
		return summe;
	}

	/** Merkt sich eine ausprobierte Farbe (für den Erfolg „Regenbogen“). */
	void farbeGesehen(int nr) {
		Set<String> s = menge("farbenGesehen");
		if (s.add(String.valueOf(nr))) merke("farbenGesehen", s);
	}

	void helferGefragt() {
		c.setze("helferFragen", c.zahl("helferFragen", 0) + 1);
	}

	// ============================================================ Shop

	/** Entwickler-Konto: alle Kosmetik frei (siehe Quests.ENTWICKLER). */
	volatile boolean entwickler;

	boolean gekauft(String id) { return entwickler || menge("shop").contains(id); }

	/**
	 * Einmalige Umstellung: Wer vorher schon Kosmetik getragen hat, behält sie.
	 * Das Start-Set gehört sowieso allen.
	 */
	void stelleUm() {
		if (c.wert("kosmetikUmgestellt", false)) return;
		Set<String> m = menge("shop");
		for (String[] e : Quests.STARTSET) m.add(e[1]);
		m.add(c.cape());
		m.add(c.fluegel());
		m.add(c.hut());
		m.add(c.partikel());
		m.add(c.spur());
		m.add(c.killEffekt());
		m.remove(ClientEinstellungen.AUS);
		m.remove("");
		merke("shop", m);
		c.setze("kosmetikUmgestellt", true);
	}

	/** Ist dieses Kosmetik-Stück noch gesperrt? „Aus“, Start-Set und eigene Sachen nie. */
	boolean gesperrtKosmetik(String art, String id) {
		if (id == null || id.isEmpty() || ClientEinstellungen.AUS.equals(id)) return false;
		if (id.startsWith("+")) return false;   // "+neu" ist der Knopf fürs Studio, keine Kosmetik
		if (Quests.imStartset(id) || EigeneKosmetik.istEigen(id)) return false;
		return !gekauft(id);
	}

	int preisKosmetik(String art, String id) { return Quests.preis(art, id); }

	/** Kauft ein Kosmetik-Stück, wenn genug Blätter da sind. */
	boolean kaufeKosmetik(String art, String id) {
		if (!gesperrtKosmetik(art, id)) return false;
		int p = Quests.preis(art, id);
		if (blaetter() < p) return false;
		c.setze("punkte", blaetter() - p);
		Set<String> m = menge("shop");
		m.add(id);
		merke("shop", m);
		return true;
	}

	// ============================================================ Saison

	String[] saison() { return Quests.saison(LocalDate.now().getMonthValue()); }

	/** Fehlen noch Stücke aus dem Saison-Set? */
	boolean saisonOffen() {
		for (String[] st : Quests.saisonStuecke(saison())) if (gesperrtKosmetik(st[0], st[1])) return true;
		return false;
	}

	/** Kauft das ganze Saison-Set auf einmal (günstiger als einzeln). */
	boolean kaufeSaison() {
		String[] s = saison();
		int p = Integer.parseInt(s[3]);
		if (!saisonOffen() || blaetter() < p) return false;
		c.setze("punkte", blaetter() - p);
		Set<String> m = menge("shop");
		for (String[] st : Quests.saisonStuecke(s)) m.add(st[1]);
		merke("shop", m);
		return true;
	}

	// ============================================================ Stufen der Herausforderungen

	/** Aktuelle Stufe einer Herausforderung (1 = noch nie geschafft). */
	int stufe(String id) {
		for (String t : c.text("hartStufe", "").split(",")) {
			String[] teil = t.split(":");
			if (teil.length == 2 && teil[0].trim().equals(id)) {
				try {
					return Math.max(1, Math.min(Quests.STUFE_MAX, Integer.parseInt(teil[1].trim())));
				} catch (NumberFormatException e) {
					return 1;
				}
			}
		}
		return 1;
	}

	static String[] shopEintrag(String id) { return Quests.finde(Quests.SHOP, id); }

	/** true, wenn der Gegenstand im Shop verkauft wird und noch nicht gekauft ist. */
	static boolean imShop(String id) { return shopEintrag(id) != null; }

	boolean gesperrt(String id) { return imShop(id) && !gekauft(id); }

	int preis(String id) {
		String[] s = shopEintrag(id);
		return s == null ? 0 : Integer.parseInt(s[3]);
	}

	/** Kauft den Gegenstand, wenn genug Blätter da sind. */
	boolean kaufe(String id) {
		String[] s = shopEintrag(id);
		if (s == null || gekauft(id)) return false;
		int p = Integer.parseInt(s[3]);
		if (blaetter() < p) return false;
		c.setze("punkte", blaetter() - p);
		Set<String> m = menge("shop");
		m.add(id);
		merke("shop", m);
		return true;
	}
}
