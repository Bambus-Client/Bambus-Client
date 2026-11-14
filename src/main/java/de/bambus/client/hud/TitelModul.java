package de.bambus.client.hud;

import de.bambus.client.Farben;
import net.minecraft.client.Minecraft;

import java.util.List;

/** Schriftzug „Bambus Client“ (früher die erste Zeile des Info-Kastens). */
public class TitelModul extends TextModul {

	private final Option version = option("version", "Versionsnummer", false);

	public TitelModul() {
		super("titel", "Bambus-Schriftzug", 4, 4, true, "„Bambus Client“ im HUD", "HUD");
	}

	@Override
	protected List<Zeile> zeilen(Minecraft mc, boolean editor) {
		return List.of(new Zeile("Bambus Client" + (version.an() ? " " + de.bambus.client.BambusClient.VERSION : ""), Farben.GRUEN));
	}
}
