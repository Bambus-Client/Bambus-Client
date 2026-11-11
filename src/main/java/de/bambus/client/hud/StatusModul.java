package de.bambus.client.hud;

import de.bambus.client.BambusClient;
import de.bambus.client.Farben;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

/** Zeigt an, ob Sprinten oder Schleichen gerade umgeschaltet (dauerhaft an) ist. */
public class StatusModul extends TextModul {

	private final Option sprint = option("sprint", "Sprinten anzeigen", true);
	private final Option schleichen = option("schleichen", "Schleichen anzeigen", true);

	public StatusModul() {
		super("status", "Sprint-/Schleich-Status", 4, 172);
	}

	@Override
	protected List<Zeile> zeilen(Minecraft mc, boolean editor) {
		List<Zeile> z = new ArrayList<>();
		if (sprint.an() && BambusClient.config.toggleSprint && mc.options.keySprint.isDown()) {
			z.add(new Zeile("[Sprinten (umgeschaltet)]", Farben.GRUEN));
		}
		if (schleichen.an() && BambusClient.config.toggleSneak && mc.options.keyShift.isDown()) {
			z.add(new Zeile("[Schleichen (umgeschaltet)]", Farben.GRUEN));
		}
		if (z.isEmpty() && editor) {
			z.add(new Zeile("[Sprint-Status]", Farben.GRAU));
		}
		return z;
	}
}
