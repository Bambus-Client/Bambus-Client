package de.bambus.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Wegpunkte: Orte, die man sich merkt – mit Namen, Farbe und Entfernung im Kompass-Balken.
 * Jeder Wegpunkt gehört zu einem Server (oder einer Einzelspieler-Welt) und einer Dimension,
 * damit Hypixel-Wegpunkte nicht in der eigenen Welt auftauchen.
 */
public final class Wegpunkte {

	/** Farben zum Durchklicken. */
	public static final int[] FARBEN = {0x3DFF8A, 0x00E5FF, 0xFF2BD6, 0xFFC846, 0xFF4D4D, 0xFFFFFF};

	public static final class Punkt {
		public String name;
		public int x, y, z;
		public int farbe;
		public String ort, dimension;
		public boolean sichtbar = true;

		String alsText() {
			return name.replace("|", "/") + "|" + x + "|" + y + "|" + z + "|" + String.format("%06X", farbe & 0xFFFFFF)
					+ "|" + ort + "|" + dimension + "|" + (sichtbar ? "1" : "0");
		}

		static Punkt aus(String text) {
			String[] t = text.split("\\|", -1);
			if (t.length < 7) return null;
			try {
				Punkt p = new Punkt();
				p.name = t[0];
				p.x = Integer.parseInt(t[1]);
				p.y = Integer.parseInt(t[2]);
				p.z = Integer.parseInt(t[3]);
				p.farbe = Integer.parseInt(t[4], 16);
				p.ort = t[5];
				p.dimension = t[6];
				p.sichtbar = t.length < 8 || !"0".equals(t[7]);
				return p;
			} catch (NumberFormatException e) {
				return null;
			}
		}
	}

	private Wegpunkte() {}

	/** Wo man gerade spielt: Server-Adresse oder „sp:Weltname“. null = in keiner Welt. */
	public static String ort(Minecraft mc) {
		if (mc.level == null) return null;
		if (mc.hasSingleplayerServer() && mc.getSingleplayerServer() != null) {
			return "sp:" + mc.getSingleplayerServer().getWorldData().getLevelName();
		}
		ServerData s = mc.getCurrentServer();
		return s != null ? s.ip.toLowerCase(Locale.ROOT) : "unbekannt";
	}

	public static String dimension(Minecraft mc) {
		if (mc.level == null) return "";
		if (mc.level.dimension() == Level.NETHER) return "nether";
		if (mc.level.dimension() == Level.END) return "ende";
		return "oberwelt";
	}

	/** Alle Wegpunkte (in gespeicherter Reihenfolge). */
	public static List<Punkt> alle() {
		List<Punkt> aus = new ArrayList<>();
		for (String t : BambusClient.config.wegpunkte) {
			Punkt p = Punkt.aus(t);
			if (p != null) aus.add(p);
		}
		return aus;
	}

	/** Nur die, die zu diesem Server und dieser Dimension gehören. */
	public static List<Punkt> hier(Minecraft mc) {
		List<Punkt> aus = new ArrayList<>();
		String ort = ort(mc), dim = dimension(mc);
		if (ort == null) return aus;
		for (Punkt p : alle()) if (p.ort.equals(ort) && p.dimension.equals(dim)) aus.add(p);
		return aus;
	}

	private static void speichern(List<Punkt> liste) {
		List<String> t = new ArrayList<>();
		for (Punkt p : liste) t.add(p.alsText());
		BambusClient.config.wegpunkte = t;
		BambusClient.config.speichern();
	}

	/** Setzt einen Wegpunkt an der eigenen Position. */
	public static Punkt hierSetzen(Minecraft mc, String name) {
		if (mc.player == null) return null;
		List<Punkt> liste = alle();
		Punkt p = new Punkt();
		p.x = Mth.floor(mc.player.getX());
		p.y = Mth.floor(mc.player.getY());
		p.z = Mth.floor(mc.player.getZ());
		p.ort = ort(mc);
		p.dimension = dimension(mc);
		int nr = hier(mc).size() + 1;
		p.name = name == null || name.isBlank() ? "Wegpunkt " + nr : name.trim();
		if (p.name.length() > 24) p.name = p.name.substring(0, 24);
		p.farbe = FARBEN[(nr - 1) % FARBEN.length];
		liste.add(p);
		speichern(liste);
		return p;
	}

	/** Ändert einen Wegpunkt (gefunden über seinen alten Text). */
	private static void aendere(Punkt alt, java.util.function.Consumer<Punkt> aenderung) {
		List<Punkt> liste = alle();
		String schluessel = alt.alsText();
		for (Punkt p : liste) {
			if (p.alsText().equals(schluessel)) {
				aenderung.accept(p);
				break;
			}
		}
		speichern(liste);
	}

	public static void entfernen(Punkt p) {
		List<Punkt> liste = alle();
		String schluessel = p.alsText();
		liste.removeIf(q -> q.alsText().equals(schluessel));
		speichern(liste);
	}

	public static void umschalten(Punkt p) {
		aendere(p, q -> q.sichtbar = !q.sichtbar);
	}

	public static void naechsteFarbe(Punkt p) {
		aendere(p, q -> {
			int i = 0;
			for (int k = 0; k < FARBEN.length; k++) if (FARBEN[k] == (q.farbe & 0xFFFFFF)) i = k;
			q.farbe = FARBEN[(i + 1) % FARBEN.length];
		});
	}

	public static void umbenennen(Punkt p, String name) {
		if (name == null || name.isBlank()) return;
		String n = name.trim().length() > 24 ? name.trim().substring(0, 24) : name.trim();
		aendere(p, q -> q.name = n);
	}

	/** Entfernung in Blöcken (waagerecht, wie man sie läuft). */
	public static int abstand(Minecraft mc, Punkt p) {
		if (mc.player == null) return 0;
		double dx = p.x + 0.5 - mc.player.getX(), dz = p.z + 0.5 - mc.player.getZ();
		return (int) Math.round(Math.sqrt(dx * dx + dz * dz));
	}

	/** Richtung zum Wegpunkt als Kompasswinkel: 0 = Norden, 90 = Osten (wie der Kompass-Balken). */
	public static float peilung(Minecraft mc, Punkt p) {
		if (mc.player == null) return 0f;
		double dx = p.x + 0.5 - mc.player.getX(), dz = p.z + 0.5 - mc.player.getZ();
		float grad = (float) Math.toDegrees(Math.atan2(dx, -dz));
		return (grad % 360f + 360f) % 360f;
	}

	/** „1,2 km“ oder „85 m“. */
	public static String abstandText(int bloecke) {
		return bloecke >= 1000 ? String.format(Locale.GERMANY, "%.1f km", bloecke / 1000f) : bloecke + " m";
	}
}
