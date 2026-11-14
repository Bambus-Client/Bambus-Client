package de.bambus.client.hud;

import de.bambus.client.Farben;
import net.minecraft.client.Minecraft;

import java.util.List;

/** Bilder pro Sekunde – auf Wunsch farbig (grün flüssig, gelb, rot ruckelig). */
public class FpsModul extends TextModul {

	private final Option titel = option("titel", "Text „FPS:“ davor", true);
	private final Option farbig = option("farbig", "Farbig (grün / gelb / rot)", false);

	public FpsModul() {
		super("fps", "FPS", 4, 19, true, "Bilder pro Sekunde", "INFO");
	}

	@Override
	protected List<Zeile> zeilen(Minecraft mc, boolean editor) {
		int fps = mc.getFps();
		int farbe = !farbig.an() ? Farben.WEISS : fps >= 60 ? Farben.GRUEN : fps >= 30 ? Farben.GELB : Farben.ROT;
		return List.of(new Zeile((titel.an() ? "FPS: " : "") + fps, farbe));
	}
}
