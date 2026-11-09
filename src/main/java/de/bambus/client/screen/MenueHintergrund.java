package de.bambus.client.screen;

import de.bambus.client.Farben;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/**
 * Die animierten Hintergründe aus dem Launcher auch im Bambus-Hauptmenü
 * (Partikel, Aurora, Datenregen, Gitter). „synthwave“ zeichnet BambusTitelScreen selbst.
 */
public final class MenueHintergrund {

	public static final String[] ARTEN = {"synthwave", "partikel", "aurora", "datenregen", "gitter"};
	public static final String[] NAMEN = {"Synthwave", "Partikel", "Aurora", "Datenregen", "Gitter"};

	private static final int ANZAHL = 46;
	private static final float[][] PUNKTE = new float[ANZAHL][4];

	static {
		java.util.Random r = new java.util.Random(7);
		for (float[] p : PUNKTE) {
			p[0] = r.nextFloat();
			p[1] = r.nextFloat();
			p[2] = 0.012f + r.nextFloat() * 0.03f;
			p[3] = 1 + r.nextFloat() * 2;
		}
	}

	private MenueHintergrund() {}

	public static String naechste(String art) {
		for (int i = 0; i < ARTEN.length; i++) if (ARTEN[i].equals(art)) return ARTEN[(i + 1) % ARTEN.length];
		return ARTEN[0];
	}

	public static String name(String art) {
		for (int i = 0; i < ARTEN.length; i++) if (ARTEN[i].equals(art)) return NAMEN[i];
		return NAMEN[0];
	}

	/** true, wenn gezeichnet wurde (sonst bleibt Synthwave). */
	public static boolean zeichne(GuiGraphics g, Font font, String art, int b, int h, float t) {
		if (art == null || art.equals("synthwave")) return false;
		g.fillGradient(0, 0, b, h, 0xFF03060C, 0xFF060A12);
		switch (art) {
			case "partikel" -> partikel(g, b, h, t);
			case "aurora" -> aurora(g, b, h, t);
			case "datenregen" -> datenregen(g, font, b, h, t);
			default -> Neon.gitter(g, 0, 0, b, h, 20, Farben.alpha(Farben.NEON, 14));
		}
		return true;
	}

	private static void partikel(GuiGraphics g, int b, int h, float t) {
		Neon.gitter(g, 0, 0, b, h, 20, Farben.alpha(Farben.NEON, 8));
		for (int i = 0; i < ANZAHL; i++) {
			float[] p = PUNKTE[i];
			float py = (p[1] - t * p[2]) % 1f;
			if (py < 0) py += 1;
			int x = (int) ((p[0] + 0.02f * Mth.sin(t * 0.6f + i)) * b);
			int y = (int) (py * h);
			int s = (int) p[3];
			int farbe = i % 3 == 0 ? Farben.NEON_CYAN : i % 7 == 0 ? Farben.NEON_PINK : Farben.NEON;
			g.fill(x - s * 2, y - s * 2, x + s * 2 + 1, y + s * 2 + 1, Farben.alpha(farbe, 22));
			g.fill(x - s, y - s, x + s + 1, y + s + 1, Farben.alpha(farbe, 60));
			g.fill(x, y, x + Math.max(1, s), y + Math.max(1, s), Farben.alpha(farbe, 200));
		}
	}

	private static void aurora(GuiGraphics g, int b, int h, float t) {
		int[] farben = {Farben.NEON, Farben.NEON_CYAN, 0xFFB06BFF};
		for (int band = 0; band < 3; band++) {
			float basis = h * (0.28f + band * 0.14f);
			for (int x = 0; x < b; x += 3) {
				float w = Mth.sin(x / 85f + t * (0.35f + band * 0.12f) + band * 2) * 17 + Mth.sin(x / 30f - t * 0.7f + band) * 6;
				int y = (int) (basis + w);
				g.fillGradient(x, y - 30, x + 3, y + 4, Farben.alpha(farben[band], 0), Farben.alpha(farben[band], 60));
				g.fillGradient(x, y + 4, x + 3, y + 70, Farben.alpha(farben[band], 40), Farben.alpha(farben[band], 0));
			}
		}
		for (int i = 0; i < 40; i++) {
			int a = (int) (60 + 60 * Mth.sin(t * 2 + i));
			g.fill((int) (PUNKTE[i][0] * b), (int) (PUNKTE[i][1] * h * 0.5f), (int) (PUNKTE[i][0] * b) + 1, (int) (PUNKTE[i][1] * h * 0.5f) + 1,
					Farben.alpha(0xFFFFFFFF, Math.max(0, a)));
		}
	}

	private static void datenregen(GuiGraphics g, Font font, int b, int h, float t) {
		int spalte = 11;
		for (int s = 0; s < b / spalte; s++) {
			float[] p = PUNKTE[s % ANZAHL];
			float tempo = 30 + p[2] * 1500;
			int laenge = 6 + (int) (p[3] * 3);
			float kopf = (t * tempo + p[1] * h * 2) % (h + laenge * 9);
			for (int k = 0; k < laenge; k++) {
				int y = (int) (kopf - k * 9);
				if (y < 0 || y > h) continue;
				int a = k == 0 ? 170 : (int) (90 * (1 - k / (float) laenge));
				int farbe = k == 0 ? 0xFFFFFFFF : (s % 5 == 0 ? Farben.NEON_CYAN : Farben.NEON);
				String c = ((s * 7 + k * 13 + (int) (t * 4)) % 2) == 0 ? "0" : "1";
				g.drawString(font, c, s * spalte + 3, y, Farben.alpha(farbe, a / 2), false);
			}
		}
	}
}
