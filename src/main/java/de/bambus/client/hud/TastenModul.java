package de.bambus.client.hud;

import de.bambus.client.Farben;
import de.bambus.client.Funktionen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** Keystrokes: W A S D, Maustasten mit CPS, Leertaste – jede Reihe einzeln abschaltbar (⋮ im Mod-Menü). */
public class TastenModul extends HudModul {

	private static final int K = 22;      // Größe einer Taste
	private static final int ABSTAND = 2;
	private static final int BREITE = 3 * K + 2 * ABSTAND;
	private static final int LEERTASTE_H = 12;

	private final Option wasd = option("wasd", "W A S D", true);
	private final Option links = option("links", "Linksklick (LMB)", true);
	private final Option rechts = option("rechts", "Rechtsklick (RMB)", true);
	private final Option cps = option("cps", "CPS anzeigen", true);
	private final Option leertaste = option("leertaste", "Leertaste", true);

	public TastenModul() {
		super("tasten", "Keystrokes", 4, 54);
	}

	private boolean maus() {
		return links.an() || rechts.an();
	}

	@Override
	public int breite(Minecraft mc, boolean editor) {
		return hoehe(mc, editor) > 0 ? BREITE : 0;
	}

	@Override
	public int hoehe(Minecraft mc, boolean editor) {
		int h = 0;
		if (wasd.an()) h += 2 * (K + ABSTAND);
		if (maus()) h += K + ABSTAND;
		if (leertaste.an()) h += LEERTASTE_H;
		return h;
	}

	@Override
	public void zeichne(GuiGraphics g, Minecraft mc, int x, int y, boolean editor) {
		Font font = mc.font;
		int ky = y;

		if (wasd.an()) {
			taste(g, font, mc.options.keyUp, x + K + ABSTAND, ky);
			ky += K + ABSTAND;
			taste(g, font, mc.options.keyLeft, x, ky);
			taste(g, font, mc.options.keyDown, x + K + ABSTAND, ky);
			taste(g, font, mc.options.keyRight, x + 2 * (K + ABSTAND), ky);
			ky += K + ABSTAND;
		}

		if (maus()) {
			boolean beide = links.an() && rechts.an();
			int mausBreite = beide ? (BREITE - ABSTAND) / 2 : BREITE;
			int mx = x;
			if (links.an()) {
				maus(g, font, "LMB", Funktionen.cpsLinks(), mc.options.keyAttack.isDown(), mx, ky, mausBreite);
				mx += mausBreite + ABSTAND;
			}
			if (rechts.an()) {
				maus(g, font, "RMB", Funktionen.cpsRechts(), mc.options.keyUse.isDown(), mx, ky, mausBreite);
			}
			ky += K + ABSTAND;
		}

		if (leertaste.an()) {
			boolean springen = mc.options.keyJump.isDown();
			flaeche(g, x, ky, BREITE, LEERTASTE_H, springen);
			g.fill(x + BREITE / 2 - 12, ky + 5, x + BREITE / 2 + 12, ky + 7, springen ? Farben.SCHWARZ : Farben.WEISS);
		}
	}

	/** Fläche einer Taste: gedrückt immer sichtbar, sonst nur mit Hintergrund. */
	private void flaeche(GuiGraphics g, int x, int y, int b, int h, boolean gedrueckt) {
		if (gedrueckt) {
			g.fill(x, y, x + b, y + h, Farben.GRUEN_GEDRUECKT);
		} else if (hintergrund.an()) {
			g.fill(x, y, x + b, y + h, Farben.HINTERGRUND);
		}
	}

	private void taste(GuiGraphics g, Font font, KeyMapping mapping, int x, int y) {
		boolean gedrueckt = mapping.isDown();
		flaeche(g, x, y, K, K, gedrueckt);
		String name = mapping.getTranslatedKeyMessage().getString().toUpperCase();
		if (font.width(name) > K - 2) {
			name = name.substring(0, 1);
		}
		g.drawString(font, name, x + (K - font.width(name)) / 2, y + (K - 8) / 2,
				gedrueckt ? Farben.SCHWARZ : Farben.WEISS, !gedrueckt);
	}

	private void maus(GuiGraphics g, Font font, String name, int klicks, boolean gedrueckt, int x, int y, int b) {
		flaeche(g, x, y, b, K, gedrueckt);
		if (!cps.an()) {   // ohne CPS: Name mittig
			g.drawString(font, name, x + (b - font.width(name)) / 2, y + (K - 8) / 2,
					gedrueckt ? Farben.SCHWARZ : Farben.WEISS, !gedrueckt);
			return;
		}
		g.drawString(font, name, x + (b - font.width(name)) / 2, y + 3,
				gedrueckt ? Farben.SCHWARZ : Farben.WEISS, !gedrueckt);
		String cpsText = klicks + " CPS";
		g.drawString(font, cpsText, x + (b - font.width(cpsText)) / 2, y + 12,
				gedrueckt ? Farben.SCHWARZ : Farben.GRAU, false);
	}
}
