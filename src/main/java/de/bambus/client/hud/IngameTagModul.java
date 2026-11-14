package de.bambus.client.hud;

import de.bambus.client.Farben;
import net.minecraft.client.Minecraft;

import java.util.List;

/** Welcher Tag in der Welt gerade ist – auf Wunsch mit Ingame-Uhrzeit (früher Teil der Spielzeit). */
public class IngameTagModul extends TextModul {

	private final Option uhrzeit = option("uhrzeit", "Ingame-Uhrzeit", false);

	public IngameTagModul() {
		super("ingametag", "Ingame-Tag", 4, 350, false, "Tag in der Welt, auf Wunsch mit Uhrzeit", "INFO");
	}

	@Override
	protected List<Zeile> zeilen(Minecraft mc, boolean editor) {
		long zeit = mc.level != null ? mc.level.getDayTime() : 11 * 24000L + 6000;
		String text = "Tag " + (zeit / 24000L + 1);
		if (uhrzeit.an()) {
			long t = (zeit + 6000) % 24000;   // Tick 0 = 6:00 Uhr
			text += String.format(" · %02d:%02d", t / 1000, (t % 1000) * 60 / 1000);
		}
		return List.of(new Zeile(text, Farben.WEISS));
	}
}
