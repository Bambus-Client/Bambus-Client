package de.bambus.client;

import de.bambus.quests.Quests;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Blätter (die Punkte des Quest-Systems) im Spiel: zählt die Spielzeit, erkennt die
 * Herausforderungen des Tages und kennt Rang und freigeschaltete Shop-Kosmetik.
 * Es geht nie um echtes Geld – alles bleibt auf diesem Computer.
 */
public final class Blaetter {

	private static final int MINUTEN_PRO_BONUS = 10;
	private static final int BONUS = 5;
	private static final int BONUS_MAX_PRO_TAG = 30;

	private static int ticks;
	private static double letzteX, letzteZ;
	private static boolean letzteGesetzt;
	private static float hoechsterFall;
	private static boolean leiterDabei, eimerDabei;
	private static int killsBeiWeltstart;

	private Blaetter() {}

	// ============================================================ Rang und Guthaben

	public static int blaetter() { return BambusClient.config.punkte; }

	public static int gesammelt() { return BambusClient.config.punkteGesamt; }

	public static String rangName() { return Quests.rangName(gesammelt()); }

	public static int rangFarbe() { return Quests.rangFarbe(gesammelt()); }

	private static void gib(int wieviel) {
		BambusClient.config.punkte += wieviel;
		BambusClient.config.punkteGesamt += wieviel;
	}

	// ============================================================ Shop

	/** true, wenn die Kosmetik im Shop verkauft wird und noch nicht freigeschaltet ist. */
	public static boolean gesperrt(String id) {
		return id != null && Quests.imShop(id) && !menge("shop").contains(id);
	}

	/** Kauft die Kosmetik, wenn genug Blätter da sind. */
	public static boolean kaufe(String id) {
		if (!gesperrt(id)) return false;
		int preis = Quests.preis(id);
		if (BambusClient.config.punkte < preis) return false;
		BambusClient.config.punkte -= preis;
		Set<String> m = menge("shop");
		m.add(id);
		BambusClient.config.shop = String.join(",", m);
		BambusClient.config.speichern();
		return true;
	}

	// ============================================================ Herausforderungen

	public static List<String[]> heute() {
		return Quests.auswahl(Quests.HART, LocalDate.now().toEpochDay(), Quests.HART_ANZAHL);
	}

	public static boolean fertig(String id) {
		tagPruefen();
		return menge("hartFertig").contains(id);
	}

	/** Zwischenstand, z. B. 340 von 1000 gesprinteten Blöcken. */
	public static int stand(String id) {
		tagPruefen();
		Integer w = staende().get(id);
		return w == null ? 0 : w;
	}

	/** Setzt einen Zwischenstand und schließt die Herausforderung ab, wenn das Ziel erreicht ist. */
	private static void setzeStand(String id, int wert) {
		String[] q = Quests.finde(Quests.HART, id);
		if (q == null || fertig(id)) return;
		Map<String, Integer> alle = staende();
		Integer vorher = alle.get(id);
		if (vorher != null && vorher >= wert) return;
		alle.put(id, wert);
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, Integer> e : alle.entrySet()) {
			if (sb.length() > 0) sb.append(',');
			sb.append(e.getKey()).append(':').append(e.getValue());
		}
		BambusClient.config.hartStand = sb.toString();
		if (wert >= Integer.parseInt(q[4])) schaffe(id);
	}

	/** Meldet eine erledigte Herausforderung (nur, wenn sie heute dran ist). */
	public static void schaffe(String id) {
		tagPruefen();
		if (fertig(id)) return;
		boolean heuteDran = false;
		for (String[] q : heute()) if (q[0].equals(id)) heuteDran = true;
		if (!heuteDran) return;
		String[] q = Quests.finde(Quests.HART, id);
		Set<String> f = menge("hartFertig");
		f.add(id);
		BambusClient.config.hartFertig = String.join(",", f);
		int p = Integer.parseInt(q[3]);
		gib(p);
		BambusClient.config.speichern();
		Hinweise.zeige("stern", "Quest geschafft: " + q[1], "+" + p + " Blätter", rangFarbe());
	}

	/** Zählt bei einer Herausforderung mit Ziel mit (z. B. Kills oder Blöcke). */
	private static void zaehle(String id, int neuerStand) {
		for (String[] q : heute()) {
			if (q[0].equals(id)) {
				setzeStand(id, neuerStand);
				return;
			}
		}
	}

	// ============================================================ Erkennung

	/** Jeden Tick aufrufen (20 Ticks = 1 Sekunde). */
	public static void tick(Minecraft mc) {
		LocalPlayer p = mc.player;
		if (p == null) return;
		tagPruefen();
		herausforderungen(mc, p);

		if (++ticks < 20 * 60) return;
		ticks = 0;
		BambusConfig c = BambusClient.config;
		c.spielminuten++;
		if (c.spielminuten % MINUTEN_PRO_BONUS == 0 && c.spielBonusHeute < BONUS_MAX_PRO_TAG) {
			c.spielBonusHeute += BONUS;
			gib(BONUS);
			Hinweise.zeige("stern", "+" + BONUS + " Blätter", c.spielminuten + " Minuten gespielt", rangFarbe());
		}
		c.speichern();
	}

	private static void herausforderungen(Minecraft mc, LocalPlayer p) {
		// Strecke: Sprinten und Elytra
		if (letzteGesetzt) {
			double dx = p.getX() - letzteX, dz = p.getZ() - letzteZ;
			double weg = Math.sqrt(dx * dx + dz * dz);
			if (weg > 0.01 && weg < 20) {   // Sprünge beim Teleportieren nicht mitzählen
				if (p.isSprinting()) zaehle("sprint1000", stand("sprint1000") + (int) Math.round(weg));
				if (p.isFallFlying()) zaehle("elytra500", stand("elytra500") + (int) Math.round(weg));
			}
		}
		letzteX = p.getX();
		letzteZ = p.getZ();
		letzteGesetzt = true;

		// Sturz merken, solange er dauert; Leiter oder Wassereimer in der Hand zählt als Vorbereitung
		float fall = p.fallDistance;
		if (fall > hoechsterFall) hoechsterFall = fall;
		if (fall > 1f) {
			if (hatItem(p, Items.LADDER)) leiterDabei = true;
			if (hatItem(p, Items.WATER_BUCKET)) eimerDabei = true;
		}
		if (hoechsterFall >= 8f) {
			if (p.onClimbable() && leiterDabei) {
				schaffe("mlg_leiter");
				sturzEnde();
			} else if (p.isInWater() && eimerDabei) {
				schaffe("mlg_wasser");
				sturzEnde();
			}
		}
		if (p.onGround() || p.isInWater() || p.onClimbable() || p.isFallFlying()) {
			if (p.onGround() && hoechsterFall >= 20f && p.isAlive() && p.getHealth() > 0) schaffe("fall20");
			sturzEnde();
		}

		// Tiefe, Nether, Kills, Combo
		if (p.getY() <= -58) schaffe("tief");
		if (mc.level != null && mc.level.dimension().location().getPath().contains("nether")) schaffe("nether");
		zaehle("kills5", Math.max(0, Kampf.killsSpieler - killsBeiWeltstart));
		if (Kampf.combo >= 5) schaffe("combo5");
	}

	private static void sturzEnde() {
		hoechsterFall = 0f;
		leiterDabei = false;
		eimerDabei = false;
	}

	private static boolean hatItem(LocalPlayer p, net.minecraft.world.item.Item item) {
		ItemStack haupt = p.getMainHandItem(), neben = p.getOffhandItem();
		return haupt.is(item) || neben.is(item);
	}

	/** Beim Betreten einer Welt: Kill-Zähler als Startpunkt merken. */
	public static void weltBetreten() {
		killsBeiWeltstart = Kampf.killsSpieler;
		sturzEnde();
		letzteGesetzt = false;
	}

	/** Nachricht aus dem Chat prüfen – viele Server schreiben beim Sieg „Victory“ oder „gewonnen“. */
	public static void nachricht(String text) {
		if (text == null) return;
		String t = text.toLowerCase(Locale.ROOT);
		boolean sieg = t.contains("victory") || t.contains("you won") || t.contains("winner")
				|| t.contains("gewonnen") || t.contains("sieg!") || t.contains("1. platz")
				|| t.contains("gewinner") || t.contains("du hast gewonnen");
		if (sieg) schaffe("sieg");
		// Minecraft meldet den besiegten Drachen als Fortschritt „Das Ende befreien“ / „Free the End“
		if (t.contains("free the end") || t.contains("das ende befreien")) erfolgDrache();
	}

	/** Erfolg „Drachentöter“ (einmalig, kein Tages-Quest). */
	public static void erfolgDrache() {
		Set<String> e = menge("erfolge");
		if (!e.add("drache")) return;
		BambusClient.config.erfolge = String.join(",", e);
		String[] a = Quests.finde(Quests.ERFOLGE, "drache");
		gib(Integer.parseInt(a[3]));
		BambusClient.config.speichern();
		Hinweise.zeige("stern", "Erfolg: " + a[1], "+" + a[3] + " Blätter", rangFarbe());
	}

	// ============================================================ Speicher-Kleinkram

	private static void tagPruefen() {
		String heute = LocalDate.now().toString();
		BambusConfig c = BambusClient.config;
		if (!heute.equals(c.hartTag)) {
			c.hartTag = heute;
			c.hartFertig = "";
			c.hartStand = "";
		}
		if (!heute.equals(c.spielTag)) {
			c.spielTag = heute;
			c.spielBonusHeute = 0;
		}
	}

	private static Set<String> menge(String welche) {
		String roh = "shop".equals(welche) ? BambusClient.config.shop
				: "erfolge".equals(welche) ? BambusClient.config.erfolge : BambusClient.config.hartFertig;
		Set<String> s = new LinkedHashSet<>();
		for (String t : roh.split(",")) if (!t.trim().isEmpty()) s.add(t.trim());
		return s;
	}

	private static Map<String, Integer> staende() {
		Map<String, Integer> m = new LinkedHashMap<>();
		for (String t : BambusClient.config.hartStand.split(",")) {
			String[] teil = t.split(":");
			if (teil.length != 2) continue;
			try {
				m.put(teil[0].trim(), Integer.parseInt(teil[1].trim()));
			} catch (NumberFormatException ignoriert) {
				// kaputter Eintrag – überspringen
			}
		}
		return m;
	}

	/** Für die Anzeige: alle Herausforderungen von heute mit Stand und Ziel. */
	public static List<String[]> heuteMitStand() {
		List<String[]> aus = new ArrayList<>();
		for (String[] q : heute()) {
			aus.add(new String[]{q[0], q[1], q[2], q[3], q[4], String.valueOf(stand(q[0])), fertig(q[0]) ? "1" : "0"});
		}
		return aus;
	}
}
