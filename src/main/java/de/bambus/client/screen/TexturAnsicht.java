package de.bambus.client.screen;

import de.bambus.client.Farben;
import de.bambus.client.Texturen;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Ansicht aus deiner Sicht für das Textur-Modul: Himmel und Boden, deine Hände mit Schild und Schwert
 * (echte Item-Modelle) und die echten Spieltexturen für Feuer, Kürbis und Wasser.
 * Jede Einstellung wirkt sofort – mit denselben Werten wie im Spiel (siehe {@link Texturen}).
 */
public final class TexturAnsicht {

	/** Welche Situation die Ansicht zeigt (oben als Chips wählbar). */
	public enum Lage {
		FEUER("BRENNEN"), KUERBIS("KÜRBIS"), WASSER("WASSER");

		final String titel;

		Lage(String titel) {
			this.titel = titel;
		}
	}

	/** Klickbereich, den der Bildschirm anmelden soll. */
	@FunctionalInterface
	public interface Klick {
		void melde(int x, int y, int b, int h, Runnable aktion);
	}

	private static final Identifier FEUER = Identifier.withDefaultNamespace("textures/block/fire_0.png");
	private static final int FEUER_BILDER = 32;          // 16 × 512: 32 Bilder untereinander, je 1 Tick
	private static final Identifier KUERBIS = Identifier.withDefaultNamespace("textures/misc/pumpkinblur.png");
	private static final Identifier WASSER = Identifier.withDefaultNamespace("textures/misc/underwater.png");

	private static final ItemStack SCHILD = new ItemStack(Items.SHIELD);
	private static final ItemStack SCHWERT = new ItemStack(Items.DIAMOND_SWORD);

	private static Lage lage = Lage.FEUER;

	private TexturAnsicht() {}

	public static void zeichne(GuiGraphics g, Font font, int x, int y, int b, int h, int mx, int my, Klick klick) {
		g.fill(x, y, x + b, y + h, 0xFF05080C);

		// Situation wählen
		int cx = x + 4;
		for (Lage l : Lage.values()) {
			int cb = (int) (font.width(l.titel) * 0.6f) + 10;
			boolean aktiv = l == lage;
			boolean hover = Neon.ueber(mx, my, cx, y + 4, cb, 11);
			g.fill(cx, y + 4, cx + cb, y + 15, aktiv ? Farben.alpha(Farben.NEON, 60) : hover ? Farben.KARTE_HOVER : Farben.KARTE);
			g.renderOutline(cx, y + 4, cb, 11, aktiv ? Farben.NEON : Farben.LINIE);
			Neon.text(g, font, l.titel, cx + 5, y + 7, 0.6f, aktiv ? Farben.WEISS : Farben.GRAU, false);
			klick.melde(cx, y + 4, cb, 11, () -> lage = l);
			cx += cb + 3;
		}

		int sx = x + 2, sy = y + 18, sb = b - 4, sh = h - 20;
		g.enableScissor(sx, sy, sx + sb, sy + sh);
		szene(g, sx, sy, sb, sh);
		haende(g, sx, sy, sb, sh);
		g.nextStratum();   // Overlays liegen wie im Spiel über den Händen
		switch (lage) {
			case FEUER -> feuer(g, sx, sy, sb, sh);
			case KUERBIS -> {
				if (!Texturen.overlayAusblenden(KUERBIS)) {
					g.blit(RenderPipelines.GUI_TEXTURED, KUERBIS, sx, sy, 0f, 0f, sb, sh, 256, 256, 256, 256);
				}
			}
			case WASSER -> {
				g.fill(sx, sy, sx + sb, sy + sh, 0x60204A9A);   // Wasserfarbe bleibt immer
				if (!Texturen.wasserAusblenden()) {
					g.blit(RenderPipelines.GUI_TEXTURED, WASSER, sx, sy, 0f, 0f, sb, sh, 16, 16, 16, 16, 0x99FFFFFF);
				}
			}
		}
		g.disableScissor();
		Neon.ecken(g, x, y + 16, b, h - 16, 8, Farben.NEON_CYAN);
	}

	/** Himmel, Hügel, Boden und Fadenkreuz. */
	private static void szene(GuiGraphics g, int x, int y, int b, int h) {
		int horizont = y + h * 11 / 20;
		g.fillGradient(x, y, x + b, horizont, 0xFF6FA6F5, 0xFFBFD9FF);
		for (int i = 0; i < 6; i++) {   // ferne Hügel
			int hx = x + i * b / 5 - b / 12;
			int hh = 6 + (i * 37 % 9);
			g.fill(hx, horizont - hh, hx + b / 4, horizont, 0xFF4E7F3A);
		}
		g.fillGradient(x, horizont, x + b, y + h, 0xFF5E9A3C, 0xFF3A6326);
		int mx = x + b / 2, my = y + h / 2;
		g.fill(mx - 4, my, mx + 5, my + 1, 0xCCFFFFFF);
		g.fill(mx, my - 4, mx + 1, my + 5, 0xCCFFFFFF);
	}

	/** Schild links, Schwert rechts – leicht wippend wie beim Gehen. */
	private static void haende(GuiGraphics g, int x, int y, int b, int h) {
		double t = System.currentTimeMillis() / 1000.0;
		float wippen = (float) Math.sin(t * 3.2) * 1.5f;
		float s = h / 42f;   // Größe eines Gegenstands in der Hand

		float[] schild = Texturen.schildVersatz(true);   // linke Hand
		float schildX = x - b * 0.02f + schild[0] * b * 0.5f;
		float schildY = y + h * 0.42f - schild[1] * h * 0.5f + wippen;
		item(g, SCHILD, schildX, schildY, s * 2.1f);   // das Schild-Modell füllt sein Feld nicht ganz aus

		float f = Texturen.schwertGroesse();
		float ss = s * 1.5f * f;
		// Griff unten rechts bleibt an der Hand – kleiner wird das Schwert zur Spitze hin
		float griffX = x + b * 0.6f, griffY = y + h + 4 + (1f - f) * h * 0.12f + wippen;
		item(g, SCHWERT, griffX, griffY - 16 * ss, ss);
	}

	private static void item(GuiGraphics g, ItemStack stack, float x, float y, float groesse) {
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(groesse, groesse);
		g.renderItem(stack, 0, 0);
		g.pose().popMatrix();
	}

	/** Zwei Flammen in den unteren Ecken – mit Low Fire tiefer, genau wie im Spiel. */
	private static void feuer(GuiGraphics g, int x, int y, int b, int h) {
		int bild = (int) ((System.currentTimeMillis() / 50) % FEUER_BILDER);
		int fb = (int) (b * 0.46f), fh = (int) (h * 0.62f);
		int oben = y + (int) (h * 0.40f + Texturen.feuerVersatz() * h * 0.5f);
		for (int seite = 0; seite < 2; seite++) {
			int fx = seite == 0 ? x - fb / 10 : x + b - fb + fb / 10;
			g.blit(RenderPipelines.GUI_TEXTURED, FEUER, fx, oben, 0f, bild * 16f, fb, fh, 16, 16, 16, 16 * FEUER_BILDER, 0xE6FFFFFF);
		}
	}
}
