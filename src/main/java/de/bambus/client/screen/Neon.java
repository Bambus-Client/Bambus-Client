package de.bambus.client.screen;

import de.bambus.client.Farben;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** Zeichenhilfen für den Neon-Look. */
public final class Neon {

	private Neon() {}

	/** Rahmen mit weichem Leuchten nach außen. */
	public static void leuchtRahmen(GuiGraphics g, int x, int y, int b, int h, int farbe, float staerke) {
		int a1 = (int) (70 * staerke);
		int a2 = (int) (30 * staerke);
		int a3 = (int) (12 * staerke);
		g.renderOutline(x - 3, y - 3, b + 6, h + 6, Farben.alpha(farbe, a3));
		g.renderOutline(x - 2, y - 2, b + 4, h + 4, Farben.alpha(farbe, a2));
		g.renderOutline(x - 1, y - 1, b + 2, h + 2, Farben.alpha(farbe, a1));
		g.renderOutline(x, y, b, h, farbe);
	}

	/** Futuristische Ecken statt eines vollen Rahmens. */
	public static void ecken(GuiGraphics g, int x, int y, int b, int h, int laenge, int farbe) {
		g.fill(x, y, x + laenge, y + 1, farbe);
		g.fill(x, y, x + 1, y + laenge, farbe);
		g.fill(x + b - laenge, y, x + b, y + 1, farbe);
		g.fill(x + b - 1, y, x + b, y + laenge, farbe);
		g.fill(x, y + h - 1, x + laenge, y + h, farbe);
		g.fill(x, y + h - laenge, x + 1, y + h, farbe);
		g.fill(x + b - laenge, y + h - 1, x + b, y + h, farbe);
		g.fill(x + b - 1, y + h - laenge, x + b, y + h, farbe);
	}

	/** Gitter-Hintergrund wie in einem Sci-Fi-Interface. */
	public static void gitter(GuiGraphics g, int x, int y, int b, int h, int abstand, int farbe) {
		for (int gx = x; gx < x + b; gx += abstand) {
			g.fill(gx, y, gx + 1, y + h, farbe);
		}
		for (int gy = y; gy < y + h; gy += abstand) {
			g.fill(x, gy, x + b, gy + 1, farbe);
		}
	}

	/** Ein-/Aus-Schalter, dessen Knopf weich hinübergleitet (schluessel = welcher Schalter). */
	public static void schalter(GuiGraphics g, int x, int y, boolean an, String schluessel) {
		if (!de.bambus.client.BambusClient.config.menueAnimation) {
			schalter(g, x, y, an);
			return;
		}
		float k = Anim.zu("schalter:" + schluessel, an ? 1f : 0f, Anim.HOVER);
		int b = 18;
		int h = 9;
		int spur = Anim.mische(0xFF2A3445, Farben.alpha(Farben.NEON, 90), k);
		g.fill(x, y + 1, x + b, y + h - 1, spur);
		g.fill(x + 1, y, x + b - 1, y + h, spur);
		int kx = x + Math.round((b - h) * k);
		g.fill(kx + 1, y + 1, kx + h - 1, y + h - 1, Anim.mische(Farben.GRAU, Farben.NEON, k));
		if (k > 0.05f) {
			g.renderOutline(x - 1, y - 1, b + 2, h + 2, Farben.alpha(Farben.NEON, (int) (50 * k)));
		}
	}

	/** Ein-/Aus-Schalter in Pillenform. */
	public static void schalter(GuiGraphics g, int x, int y, boolean an) {
		int b = 18;
		int h = 9;
		g.fill(x, y + 1, x + b, y + h - 1, an ? Farben.alpha(Farben.NEON, 90) : 0xFF2A3445);
		g.fill(x + 1, y, x + b - 1, y + h, an ? Farben.alpha(Farben.NEON, 90) : 0xFF2A3445);
		int kx = an ? x + b - h : x;
		g.fill(kx + 1, y + 1, kx + h - 1, y + h - 1, an ? Farben.NEON : Farben.GRAU);
		if (an) {
			g.renderOutline(x - 1, y - 1, b + 2, h + 2, Farben.alpha(Farben.NEON, 50));
		}
	}

	/** Text in anderer Größe. */
	public static void text(GuiGraphics g, Font font, String text, int x, int y, float groesse, int farbe, boolean schatten) {
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(groesse, groesse);
		g.drawString(font, text, 0, 0, farbe, schatten);
		g.pose().popMatrix();
	}

	/** Text mit Neon-Schimmer (leicht versetzte, durchsichtige Kopie). */
	public static void leuchtText(GuiGraphics g, Font font, String text, int x, int y, float groesse, int farbe) {
		text(g, font, text, x + 1, y + 1, groesse, Farben.alpha(farbe, 60), false);
		text(g, font, text, x, y, groesse, farbe, false);
	}

	/**
	 * Das Bambus-Zeichen als Pixel-Art (wie im Launcher): Halm mit Knoten, ein kräftiges Blatt seitlich
	 * und ein kleines unten. Raster 12 × 18, hoehe = Höhe in GUI-Pixeln (wird auf ganze Stufen gerundet).
	 */
	public static void logo(GuiGraphics g, int x, int y, int hoehe) {
		int p = Math.max(1, Math.round(hoehe / 18f));
		int hell = mische(Farben.NEON, 0xFFFFFFFF, 0.6f), dunkel = mische(Farben.NEON, 0xFF000000, 0.55f),
				knoten = mische(Farben.NEON, 0xFF000000, 0.3f), rippe = mische(Farben.NEON_CYAN, 0xFFFFFFFF, 0.65f),
				schatten = mische(Farben.NEON_CYAN, 0xFF000000, 0.45f);
		for (int r = 0; r < LOGO.length; r++) {
			for (int c = 0; c < LOGO[r].length(); c++) {
				int farbe;
				switch (LOGO[r].charAt(c)) {
					case 'H' -> farbe = hell;
					case 'N' -> farbe = Farben.NEON;
					case 'D' -> farbe = dunkel;
					case 'K' -> farbe = knoten;
					case 'L' -> farbe = Farben.NEON_CYAN;
					case 'M' -> farbe = rippe;
					case 'l' -> farbe = schatten;
					default -> { continue; }
				}
				// leichtes Leuchten rundherum, dann der Pixel
				g.fill(x + c * p - 1, y + r * p - 1, x + c * p + p + 1, y + r * p + p + 1, Farben.alpha(farbe, 18));
				g.fill(x + c * p, y + r * p, x + c * p + p, y + r * p + p, farbe);
			}
		}
	}

	/** Breite des Logos bei dieser Höhe. */
	public static int logoBreite(int hoehe) {
		return LOGO[0].length() * Math.max(1, Math.round(hoehe / 18f));
	}

	/** H hell, N Neon, D dunkle Seite, K Knoten · L Blatt, M Blattrippe, l Blattschatten (gleich wie im Launcher) */
	private static final String[] LOGO = {
			".HNND.....LL", ".HNND...LLLL", ".HNND..LLLMl", ".HNND.LLMMl.", ".HNNDLLMll..", "KKKKKKMll...",
			".HNND.l.....", ".HNND.......", ".HNND.......", ".HNND.......", ".HNND...LLl.", "KKKKKKLLMl..",
			".HNND.lll...", ".HNND.......", ".HNND.......", ".HNND.......", ".HNND.......", ".DDDD.......",
	};

	private static int mische(int a, int b, float t) {
		int r = Math.round(((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * t);
		int gr = Math.round(((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * t);
		int bl = Math.round((a & 255) + ((b & 255) - (a & 255)) * t);
		return 0xFF000000 | (r << 16) | (gr << 8) | bl;
	}

	public static boolean ueber(double mx, double my, int x, int y, int b, int h) {
		return mx >= x && my >= y && mx < x + b && my < y + h;
	}
}
