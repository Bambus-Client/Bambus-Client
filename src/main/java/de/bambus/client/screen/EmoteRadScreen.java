package de.bambus.client.screen;

import de.bambus.client.Sprache;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import de.bambus.client.BambusClient;
import de.bambus.client.Farben;
import de.bambus.client.emote.Emote;
import de.bambus.client.emote.Emotes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Emote-Rad: Taste (Standard B) gedrückt halten, Maus in eine Richtung, loslassen – schon tanzt du.
 * Kurz antippen lässt das Rad offen; dann per Klick oder mit den Zahlen 1–8 wählen.
 * In der Mitte tanzt dein eigenes Modell das Emote vor, auf das du gerade zeigst.
 */
public class EmoteRadScreen extends Screen {

	private static final int TEX = 256;
	/** Innen- und Außenradius des Rings in Textur-Pixeln. */
	private static final float INNEN = 60f, AUSSEN = 126f;

	private static final Identifier RING = Identifier.fromNamespaceAndPath(BambusClient.MOD_ID, "emote_rad/ring");
	private static final Identifier KEIL = Identifier.fromNamespaceAndPath(BambusClient.MOD_ID, "emote_rad/keil");
	private static final Identifier MITTE = Identifier.fromNamespaceAndPath(BambusClient.MOD_ID, "emote_rad/mitte");
	private static boolean texturenDa;

	private final long geoeffnet = System.currentTimeMillis();
	private final String[] rad = Emotes.rad();
	private int auswahl = -1;

	public EmoteRadScreen() {
		super(Component.literal(Sprache.t("Emotes")));
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	// ================================================================ Texturen (einmal erzeugt)

	static void texturen(Minecraft mc) {
		if (texturenDa) return;
		texturenDa = true;
		mc.getTextureManager().register(RING, new DynamicTexture(() -> "bambus emote-ring", bild(0)));
		mc.getTextureManager().register(KEIL, new DynamicTexture(() -> "bambus emote-keil", bild(1)));
		mc.getTextureManager().register(MITTE, new DynamicTexture(() -> "bambus emote-mitte", bild(2)));
	}

	/** art: 0 = Ring mit 8 Feldern, 1 = helles Feld oben (wird gedreht und eingefärbt), 2 = Mittelscheibe. */
	private static NativeImage bild(int art) {
		NativeImage b = new NativeImage(TEX, TEX, true);
		float m = TEX / 2f;
		for (int y = 0; y < TEX; y++) {
			for (int x = 0; x < TEX; x++) {
				float dx = x + 0.5f - m, dy = y + 0.5f - m;
				float r = (float) Math.sqrt(dx * dx + dy * dy);
				int farbe = 0;
				if (art == 2) {
					float a = kante(r, INNEN - 6);
					if (a > 0) farbe = argb((int) (225 * a), 0x05, 0x0A, 0x10);
					if (Math.abs(r - (INNEN - 6)) < 1.2f) farbe = argb(110, 255, 255, 255);
				} else {
					float a = Math.min(kante(r, AUSSEN), 1 - kante(r, INNEN));
					// Lücke zwischen den 8 Feldern: Abstand zu den Trennlinien bei 22,5° + k·45°
					double winkel = Math.atan2(dx, -dy);
					double inFeld = ((winkel + Math.PI / 8) % (Math.PI / 4) + Math.PI / 4) % (Math.PI / 4);
					double abstandLinie = Math.min(inFeld, Math.PI / 4 - inFeld) * r;
					a *= (float) Math.max(0, Math.min(1, abstandLinie - 1.5));
					if (art == 1) {
						boolean oben = Math.abs(winkel) < Math.PI / 8;
						a *= oben ? 1 : 0;
						float verlauf = 0.45f + 0.55f * (r - INNEN) / (AUSSEN - INNEN);
						if (a > 0) farbe = argb((int) (230 * a * verlauf), 255, 255, 255);
					} else if (a > 0) {
						boolean rand = r > AUSSEN - 2.2f || r < INNEN + 2.2f;
						farbe = rand ? argb((int) (120 * a), 255, 255, 255) : argb((int) (210 * a), 0x0B, 0x13, 0x22);
					}
				}
				b.setPixel(x, y, farbe);
			}
		}
		return b;
	}

	/** Weiche Kante: 1 innerhalb von radius, 0 außerhalb. */
	private static float kante(float r, float radius) {
		return Math.max(0, Math.min(1, radius - r + 0.5f));
	}

	private static int argb(int a, int r, int g, int b) {
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	// ================================================================ Zeichnen

	@Override
	public void renderBackground(GuiGraphics g, int mx, int my, float delta) {
		g.fill(0, 0, width, height, 0x70000000);
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float delta) {
		super.render(g, mx, my, delta);
		int d = (int) Math.min(height * 0.78f, 236);
		float cx = width / 2f, cy = height / 2f;

		// Richtung der Maus → Feld (0 = oben, im Uhrzeigersinn)
		int feld = feld(cx, cy, d, mx, my, false);
		if (feld >= 0) auswahl = feld;
		Emote gezeigt = auswahl >= 0 ? Emotes.nachId(rad[auswahl]) : null;
		Emotes.setzeVorschau(gezeigt);
		int y0 = Math.round(cy - d / 2f);
		zeichneRad(g, font, minecraft, cx, cy, d, rad, auswahl, (System.currentTimeMillis() - geoeffnet) / 1000f);

		// Kopf und Hinweis
		String titel = gezeigt != null ? gezeigt.name().toUpperCase(java.util.Locale.ROOT) : "EMOTES";
		int tb = (int) (font.width(Sprache.t(titel)) * 1.4f);
		Neon.leuchtText(g, font, titel, (int) cx - tb / 2, y0 - 26, 1.4f, gezeigt != null ? gezeigt.farbe() : Farben.NEON);
		String unter = gezeigt != null ? gezeigt.beschreibung() : "Maus in eine Richtung – loslassen oder klicken";
		int ub = (int) (font.width(Sprache.t(unter)) * 0.7f);
		Neon.text(g, font, unter, (int) cx - ub / 2, y0 - 10, 0.7f, Farben.GRAU, false);
		String fuss = "Bewegen beendet das Emote  ·  Musik: " + (BambusClient.config.emoteMusik ? "AN" : "AUS") + " (M)  ·  Belegen in der Garderobe";
		int fb = (int) (font.width(Sprache.t(fuss)) * 0.6f);
		Neon.text(g, font, fuss, (int) cx - fb / 2, y0 + d + 8, 0.6f, 0xFF6B7A90, false);
	}

	// ================================================================ Rad (auch für die Garderobe)

	/**
	 * Feld unter der Maus (0 = oben, im Uhrzeigersinn) oder −1. nurImRing: nur wenn die Maus auf dem Ring ist
	 * (Garderobe); sonst zählt nur die Richtung (Rad mit Taste B).
	 */
	public static int feld(float cx, float cy, int d, double mx, double my, boolean nurImRing) {
		float massstab = d / (float) TEX;
		double dx = mx - cx, dy = my - cy;
		double abstand = Math.sqrt(dx * dx + dy * dy);
		if (nurImRing ? abstand < INNEN * massstab || abstand > AUSSEN * massstab : abstand < INNEN * massstab * 0.5f) return -1;
		return Math.floorMod((int) Math.round(Math.atan2(dx, -dy) / (Math.PI / 4)), Emotes.PLAETZE);
	}

	/** Zeichnet das Rad mit den Emotes der Plätze; in der Mitte tanzt das eigene Modell (Emotes.setzeVorschau). */
	public static void zeichneRad(GuiGraphics g, net.minecraft.client.gui.Font font, Minecraft mc, float cx, float cy, int d,
								  String[] rad, int hervor, float t) {
		texturen(mc);
		float massstab = d / (float) TEX;
		Emote gezeigt = hervor >= 0 ? Emotes.nachId(rad[hervor]) : null;
		int x0 = Math.round(cx - d / 2f), y0 = Math.round(cy - d / 2f);
		g.blit(RenderPipelines.GUI_TEXTURED, RING, x0, y0, 0f, 0f, d, d, TEX, TEX, TEX, TEX, 0xFFFFFFFF);
		if (hervor >= 0) {
			g.pose().pushMatrix();
			g.pose().translate(cx, cy);
			g.pose().rotate((float) (hervor * Math.PI / 4));
			g.blit(RenderPipelines.GUI_TEXTURED, KEIL, -d / 2, -d / 2, 0f, 0f, d, d, TEX, TEX, TEX, TEX,
					Farben.alpha(gezeigt != null ? gezeigt.farbe() : Farben.NEON, 150));
			g.pose().popMatrix();
		}
		g.blit(RenderPipelines.GUI_TEXTURED, MITTE, x0, y0, 0f, 0f, d, d, TEX, TEX, TEX, TEX, 0xFFFFFFFF);

		// Felder: tanzende Figur + Name
		float rm = (INNEN + AUSSEN) / 2f * massstab;
		for (int i = 0; i < Emotes.PLAETZE; i++) {
			double w = i * Math.PI / 4;
			float fx = cx + (float) Math.sin(w) * rm, fy = cy - (float) Math.cos(w) * rm;
			Emote e = Emotes.nachId(rad[i]);
			boolean an = i == hervor;
			Neon.text(g, font, String.valueOf(i + 1), (int) (fx + Math.sin(w) * 18 * massstab) - 2,
					(int) (fy - Math.cos(w) * 18 * massstab) - 3, 0.55f, 0xFF4A5A6E, false);
			if (e == null) {
				Neon.text(g, font, "frei", (int) fx - 7, (int) fy - 3, 0.7f, 0xFF4A5A6E, false);
				continue;
			}
			boolean zu = Emotes.gesperrt(e);
			// der eigene Spieler führt das Emote vor (das Feld unter der Maus bewegt sich, die anderen zeigen eine Pose)
			int halb = (int) (15 * massstab), groesse = (int) ((an ? 17.5f : 15f) * massstab);
			EmoteSpieler.zeichne(g, e, an && !zu ? t : 0.6f, (int) fx - halb, (int) (fy - 22 * massstab), (int) fx + halb,
					(int) (fy + 14 * massstab), groesse);
			if (zu) {
				g.fill((int) fx - halb, (int) (fy - 22 * massstab), (int) fx + halb, (int) (fy + 14 * massstab), 0x880B1322);
				schloss(g, (int) fx, (int) (fy - 6 * massstab), Farben.NEON_CYAN);
			}
			String n = font.plainSubstrByWidth(Sprache.t(e.name()), (int) ((AUSSEN - INNEN) * massstab * 1.2f / 0.6f));
			int nb = (int) (font.width(Sprache.t(n)) * 0.6f);
			Neon.text(g, font, n, (int) fx - nb / 2, (int) (fy + 17 * massstab), 0.6f, an ? Farben.WEISS : 0xFFB5C2D4, false);
		}

		// Mitte: das eigene Modell tanzt die Vorschau groß vor
		int r = (int) ((INNEN - 8) * massstab);
		EmoteSpieler.zeichne(g, Emotes.vorschau(), Emotes.vorschauZeit(), (int) cx - r, (int) cy - r, (int) cx + r, (int) cy + r,
				(int) (r * 0.62f));
	}

	/** Kleines Schloss (10 × 12) – für noch gesperrte Emotes. */
	public static void schloss(GuiGraphics g, int x, int y, int farbe) {
		g.fill(x - 5, y, x + 5, y + 8, farbe);
		g.fill(x - 3, y - 4, x + 3, y, farbe);
		g.fill(x - 2, y - 3, x + 2, y, 0xFF0B1322);
		g.fill(x - 1, y + 3, x + 1, y + 6, 0xFF0B1322);
	}

	// ================================================================ Eingabe

	private void spiele(int platz) {
		Emote e = platz >= 0 && platz < rad.length ? Emotes.nachId(rad[platz]) : null;
		onClose();
		if (e != null) Emotes.starte(e);
	}

	@Override
	public boolean keyReleased(KeyEvent e) {
		// Taste gehalten und losgelassen: das gezeigte Emote abspielen; nur kurz angetippt: Rad bleibt offen
		if (BambusClient.emoteTaste().matches(e) && System.currentTimeMillis() - geoeffnet > 250) {
			spiele(auswahl);
			return true;
		}
		return super.keyReleased(e);
	}

	@Override
	public boolean keyPressed(KeyEvent e) {
		int k = e.key();
		if (k >= InputConstants.KEY_1 && k <= InputConstants.KEY_8) {
			spiele(k - InputConstants.KEY_1);
			return true;
		}
		if (k == InputConstants.KEY_M) {
			BambusClient.config.emoteMusik = !BambusClient.config.emoteMusik;
			BambusClient.config.speichern();
			return true;
		}
		return super.keyPressed(e);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent e, boolean doppelt) {
		if (e.button() == InputConstants.MOUSE_BUTTON_LEFT) {
			spiele(auswahl);
			return true;
		}
		if (e.button() == InputConstants.MOUSE_BUTTON_RIGHT) {
			onClose();
			return true;
		}
		return super.mouseClicked(e, doppelt);
	}

	@Override
	public void removed() {
		Emotes.setzeVorschau(null);
	}
}
