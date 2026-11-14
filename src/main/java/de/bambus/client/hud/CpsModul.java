package de.bambus.client.hud;

import de.bambus.client.Farben;
import de.bambus.client.Funktionen;
import net.minecraft.client.Minecraft;

import java.util.List;

/** Klicks pro Sekunde (links und auf Wunsch rechts). */
public class CpsModul extends TextModul {

	private final Option rechts = option("rechts", "Rechtsklick-CPS", true);
	private final Option titel = option("titel", "Text „CPS:“ davor", true);

	public CpsModul() {
		super("cps", "CPS", 4, 34, true, "Klicks pro Sekunde, links und rechts", "PVP");
	}

	@Override
	protected List<Zeile> zeilen(Minecraft mc, boolean editor) {
		String wert = Funktionen.cpsLinks() + (rechts.an() ? " | " + Funktionen.cpsRechts() : "");
		return List.of(new Zeile((titel.an() ? "CPS: " : "") + wert, Farben.WEISS));
	}
}
