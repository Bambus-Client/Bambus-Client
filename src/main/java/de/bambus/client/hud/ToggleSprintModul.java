package de.bambus.client.hud;

import de.bambus.client.BambusClient;
import de.bambus.client.BambusConfig;
import de.bambus.client.Einstellung;
import de.bambus.client.Farben;
import de.bambus.client.Funktionen;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

/**
 * Toggle Sprint als eigenes Modul: Der Schalter der Karte macht die Funktion an oder aus.
 * Hinter ⋮ lässt sich einstellen, ob Sprinten und/oder Schleichen umgeschaltet werden
 * und ob der Status im HUD erscheint.
 *
 * Die Launcher-Einstellungen toggleSprint/toggleSneak in bambus.json bleiben gültig:
 * Beim Start werden sie ins Modul übernommen, danach schreibt das Modul sie zurück.
 */
public class ToggleSprintModul extends TextModul {

	public static final String ID = "togglesprint";

	private final Option sprint = option("sprint", "Toggle Sprint", true);
	private final Option schleichen = option("schleichen", "Toggle Sneak", false);
	private final Option hud = option("hud", "Im HUD anzeigen", true);

	public ToggleSprintModul() {
		super(ID, "Toggle Sprint", 4, 192, false, "Dauersprint, auch Schleichen", "PVP");
	}

	/** Sprinten soll umgeschaltet werden (Modul an und Schalter „Toggle Sprint“). */
	public boolean sprintAn() {
		return pos().an && sprint.an();
	}

	public boolean schleichenAn() {
		return pos().an && schleichen.an();
	}

	/** Beim Start: Werte aus bambus.json (auch vom Launcher gesetzt) ins Modul übernehmen. */
	public void ausConfigUebernehmen(BambusConfig c) {
		BambusConfig.ModulPos p = pos();
		if (c.toggleSprint || c.toggleSneak) {
			p.an = true;
			if (sprint.an() != c.toggleSprint) sprint.umschalten();
			if (schleichen.an() != c.toggleSneak) schleichen.umschalten();
		} else {
			p.an = false;
		}
	}

	/** Jeden Tick: Stand des Moduls an Minecraft weitergeben, wenn er sich geändert hat. */
	public void anwenden(Minecraft mc) {
		BambusConfig c = BambusClient.config;
		boolean s = sprintAn(), k = schleichenAn();
		if (c.toggleSprint != s || c.toggleSneak != k) {
			c.toggleSprint = s;
			c.toggleSneak = k;
			Funktionen.sprintUndSchleichenAnwenden(mc);
		}
	}

	@Override
	public List<Einstellung> einstellungen() {
		List<Einstellung> liste = new ArrayList<>();
		liste.add(new Einstellung.Schalter("Toggle Sprint", sprint::an, sprint::umschalten));
		liste.add(new Einstellung.Schalter("Toggle Sneak", schleichen::an, schleichen::umschalten));
		liste.add(new Einstellung.Schalter("Im HUD anzeigen", hud::an, hud::umschalten));
		liste.add(new Einstellung.Schalter(hintergrund.name, hintergrund::an, hintergrund::umschalten));
		return liste;
	}

	@Override
	protected List<Zeile> zeilen(Minecraft mc, boolean editor) {
		List<Zeile> z = new ArrayList<>();
		if (!hud.an()) {
			if (editor) z.add(new Zeile("[Toggle Sprint – im HUD aus]", Farben.GRAU));
			return z;
		}
		if (sprintAn() && mc.options.keySprint.isDown()) {
			z.add(new Zeile("[Sprinten (umgeschaltet)]", Farben.GRUEN));
		}
		if (schleichenAn() && mc.options.keyShift.isDown()) {
			z.add(new Zeile("[Schleichen (umgeschaltet)]", Farben.GRUEN));
		}
		if (z.isEmpty() && editor) {
			z.add(new Zeile("[Sprinten (umgeschaltet)]", Farben.GRAU));
		}
		return z;
	}
}
