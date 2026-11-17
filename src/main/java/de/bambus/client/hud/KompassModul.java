package de.bambus.client.hud;

import de.bambus.client.BambusClient;
import de.bambus.client.Farben;
import de.bambus.client.Wegpunkte;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/** Kompass-Balken wie in großen Clients: Himmelsrichtungen gleiten beim Umsehen vorbei. */
public class KompassModul extends HudModul {

	private static final int BREITE = 180;
	private static final int HOEHE = 20;
	private static final int HOEHE_MIT_PUNKTEN = 30;   // Platz für Name und Entfernung darunter
	private static final float PX_PRO_GRAD = 1.0f;   // sichtbar sind ±90°
	private static final String[] NAMEN = {"N", "NO", "O", "SO", "S", "SW", "W", "NW"};

	private final Option gradzahl = option("grad", "Gradzahl", true);
	private final Option zwischen = option("zwischen", "Zwischenrichtungen (NO, SW …)", true);

	public KompassModul() {
		super("kompass", "Kompass-Balken", 160, 2, false, "Himmelsrichtungen gleiten beim Umsehen vorbei", "INFO");
	}

	@Override
	public int breite(Minecraft mc, boolean editor) {
		return BREITE;
	}

	@Override
	public int hoehe(Minecraft mc, boolean editor) {
		return zeigtPunkte(mc, editor) ? HOEHE_MIT_PUNKTEN : HOEHE;
	}

	private static boolean zeigtPunkte(Minecraft mc, boolean editor) {
		if (!BambusClient.config.wegpunkteImKompass) return false;
		if (editor) return true;
		for (Wegpunkte.Punkt p : Wegpunkte.hier(mc)) if (p.sichtbar) return true;
		return false;
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
			boolean haupt = grad % 90 == 0;
			if (grad % 45 == 0 && (haupt || zwischen.an())) {
				String n = NAMEN[grad / 45];
				int farbe = Farben.alpha(n.equals("N") ? Farben.ROT : haupt ? Farben.WEISS : Farben.NEON_CYAN, alpha);
				g.drawString(mc.font, n, px - mc.font.width(n) / 2, y + 6, farbe, true);
			} else {
				g.fill(px, y + 12, px + 1, y + 16, Farben.alpha(Farben.GRAU, alpha));
			}
		}
		// Wegpunkte: farbiges Dreieck an der richtigen Stelle, der nächste zur Mitte mit Namen
		if (zeigtPunkte(mc, editor) && mc.player != null) {
			Wegpunkte.Punkt naechster = null;
			float naechsterAbstand = 999f;
			for (Wegpunkte.Punkt p : Wegpunkte.hier(mc)) {
				if (!p.sichtbar) continue;
				float abstand = Mth.wrapDegrees(Wegpunkte.peilung(mc, p) - r);
				boolean amRand = Math.abs(abstand) > 88;
				int px = mitte + Math.round(Mth.clamp(abstand, -88f, 88f) * PX_PRO_GRAD);
				int farbe = 0xFF000000 | p.farbe;
				int a = amRand ? 140 : 255;
				g.fill(px - 2, y + 1, px + 3, y + 2, Farben.alpha(farbe, a));
				g.fill(px - 1, y + 2, px + 2, y + 3, Farben.alpha(farbe, a));
				g.fill(px, y + 3, px + 1, y + 4, Farben.alpha(farbe, a));
				if (!amRand && Math.abs(abstand) < naechsterAbstand) {
					naechsterAbstand = Math.abs(abstand);
					naechster = p;
				}
			}
			if (naechster != null && naechsterAbstand < 30f) {
				String t = naechster.name + "  " + Wegpunkte.abstandText(Wegpunkte.abstand(mc, naechster));
				g.disableScissor();
				g.pose().pushMatrix();
				float s = 0.65f;
				int tb = Math.round(mc.font.width(t) * s);
				g.pose().translate(mitte - tb / 2f, y + HOEHE + 1);
				g.pose().scale(s, s);
				g.drawString(mc.font, t, 0, 0, 0xFF000000 | naechster.farbe, true);
				g.pose().popMatrix();
				g.enableScissor(x + 2, y, x + BREITE - 2, y + HOEHE);
			}
		} else if (zeigtPunkte(mc, editor)) {
			// Vorschau im HUD-Editor
			g.fill(mitte + 30, y + 1, mitte + 35, y + 2, 0xFF3DFF8A);
			g.fill(mitte + 31, y + 2, mitte + 34, y + 3, 0xFF3DFF8A);
		}
		g.disableScissor();
		// Markierung in der Mitte + Gradzahl
		g.fill(mitte, y, mitte + 1, y + 4, Farben.NEON);
		g.fill(mitte - 2, y, mitte + 3, y + 1, Farben.NEON);
		if (!gradzahl.an()) {
			return;
		}
		String zahl = Math.round(r) % 360 + "°";
		int zb = mc.font.width(zahl);
		g.pose().pushMatrix();
		g.pose().translate(mitte - zb * 0.3f, y + HOEHE - 1);
		g.pose().scale(0.6f, 0.6f);
		g.drawString(mc.font, zahl, 0, 0, Farben.NEON, false);
		g.pose().popMatrix();
	}
}
