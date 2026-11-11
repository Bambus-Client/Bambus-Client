package de.bambus.client.hud;

import de.bambus.client.Farben;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/** Kompass-Balken wie in großen Clients: Himmelsrichtungen gleiten beim Umsehen vorbei. */
public class KompassModul extends HudModul {

	private static final int BREITE = 180;
	private static final int HOEHE = 20;
	private static final float PX_PRO_GRAD = 1.0f;   // sichtbar sind ±90°
	private static final String[] NAMEN = {"N", "NO", "O", "SO", "S", "SW", "W", "NW"};

	public KompassModul() {
		super("kompass", "Kompass-Balken", 160, 2, false, "Himmelsrichtungen gleiten beim Umsehen vorbei", "INFO");
	}

	@Override
	public int breite(Minecraft mc, boolean editor) {
		return BREITE;
	}

	@Override
	public int hoehe(Minecraft mc, boolean editor) {
		return HOEHE;
	}

	/** Blickrichtung als Kompasswinkel: 0 = Norden, 90 = Osten. */
	private static float richtung(Minecraft mc) {
		if (mc.player == null) {
			return (System.currentTimeMillis() / 40f) % 360f;   // Vorschau im Editor
		}
		return ((Mth.wrapDegrees(mc.player.getYRot()) + 180f) % 360f + 360f) % 360f;
	}

	@Override
	public void zeichne(GuiGraphics g, Minecraft mc, int x, int y, boolean editor) {
		panel(g, x, y, BREITE, HOEHE);
		float r = richtung(mc);
		int mitte = x + BREITE / 2;
		g.enableScissor(x + 2, y, x + BREITE - 2, y + HOEHE);
		for (int grad = 0; grad < 360; grad += 15) {
			float abstand = Mth.wrapDegrees(grad - r);
			if (Math.abs(abstand) > 95) continue;
			int px = mitte + Math.round(abstand * PX_PRO_GRAD);
			// zum Rand hin ausblenden
			int alpha = (int) (255 * Math.max(0.15f, 1f - Math.abs(abstand) / 95f));
			if (grad % 45 == 0) {
				String n = NAMEN[grad / 45];
				boolean haupt = grad % 90 == 0;
				int farbe = Farben.alpha(n.equals("N") ? Farben.ROT : haupt ? Farben.WEISS : Farben.NEON_CYAN, alpha);
				g.drawString(mc.font, n, px - mc.font.width(n) / 2, y + 6, farbe, true);
			} else {
				g.fill(px, y + 12, px + 1, y + 16, Farben.alpha(Farben.GRAU, alpha));
			}
		}
		g.disableScissor();
		// Markierung in der Mitte + Gradzahl
		g.fill(mitte, y, mitte + 1, y + 4, Farben.NEON);
		g.fill(mitte - 2, y, mitte + 3, y + 1, Farben.NEON);
		String zahl = Math.round(r) % 360 + "°";
		int zb = mc.font.width(zahl);
		g.pose().pushMatrix();
		g.pose().translate(mitte - zb * 0.3f, y + HOEHE - 1);
		g.pose().scale(0.6f, 0.6f);
		g.drawString(mc.font, zahl, 0, 0, Farben.NEON, false);
		g.pose().popMatrix();
	}
}
