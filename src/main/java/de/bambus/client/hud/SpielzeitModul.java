package de.bambus.client.hud;

import de.bambus.client.Farben;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

/** Wie lange du schon spielst – und welcher Tag in der Welt gerade ist. */
public class SpielzeitModul extends TextModul {

	private static long sitzungSeit = 0;

	private final Option tagZeile = option("tag", "Ingame-Tag", true);
	private final Option pause = option("pause", "Gelb nach 2 Stunden (Pause!)", true);

	public SpielzeitModul() {
		super("spielzeit", "Spielzeit", 4, 214, false, "Zeit seit dem Betreten und Ingame-Tag", "INFO");
	}

	/** Jeden Tick: Start der Sitzung merken. */
	public static void tick(Minecraft mc) {
		if (mc.player == null) {
			sitzungSeit = 0;
		} else if (sitzungSeit == 0) {
			sitzungSeit = System.currentTimeMillis();
		}
	}

	private static String dauer(long ms) {
		long s = ms / 1000;
		long h = s / 3600, m = (s / 60) % 60, sek = s % 60;
		return h > 0 ? String.format("%d:%02d:%02d", h, m, sek) : String.format("%d:%02d", m, sek);
	}

	@Override
	protected List<Zeile> zeilen(Minecraft mc, boolean editor) {
		List<Zeile> z = new ArrayList<>();
		long ms = sitzungSeit == 0 ? 42 * 60_000L + 7_000L : System.currentTimeMillis() - sitzungSeit;
		int farbe = pause.an() && ms > 2 * 3_600_000L ? Farben.GELB : Farben.WEISS;   // nach 2 Stunden: kleine Pausen-Erinnerung per Farbe
		z.add(new Zeile("Spielzeit: " + dauer(ms), farbe));
		if (tagZeile.an()) {
			long tag = mc.level != null ? mc.level.getDayTime() / 24000L + 1 : 12;
			z.add(new Zeile("Tag " + tag + " in dieser Welt", Farben.GRAU));
		}
		return z;
	}
}
