package de.bambus.client.screen;

import de.bambus.client.BambusClient;
import de.bambus.client.Farben;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Erster Bildschirm nach Rechts-Shift: Garderobe, Mod-Menü oder – falls eingeschaltet – Bambus-Helfer öffnen.
 */
public class BambusAuswahlScreen extends Screen {

	private static final int KARTE_B = 140;
	private static final int KARTE_MAX_H = 180;
	private static final int ABSTAND = 16;

	/** Motiv: ein Symbol groß und pulsierend in der Kartenmitte. */
	private static NeonKarte.Motiv symbol(String name) {
		return (g, x1, y1, x2, y2, mx, my, hover) -> {
			int px = Math.max(3, Math.min(6, (y2 - y1) / 22));
			int groesse = 8 * px;
			int cx = (x1 + x2) / 2, cy = (y1 + y2) / 2;
			int schein = (int) (16 + 18 * Farben.puls() + 30 * hover);
			g.fill(cx - groesse / 2 - 8, cy - groesse / 2 - 8, cx + groesse / 2 + 8, cy + groesse / 2 + 8, Farben.alpha(Farben.NEON, schein));
			Symbole.zeichne(g, name, cx - groesse / 2, cy - groesse / 2, px, Anim.mische(Farben.NEON_CYAN, Farben.NEON, hover));
		};
	}

	private final long offenSeit = System.currentTimeMillis();

	public BambusAuswahlScreen() {
		super(Component.literal("Bambus"));
	}

	@Override
	protected void init() {
		boolean helfer = BambusClient.config.helferAn;
		int anzahl = helfer ? 3 : 2;
		int b = Math.min(KARTE_B, (width - 20 - ABSTAND * (anzahl - 1)) / anzahl);
		int h = Math.max(90, Math.min(KARTE_MAX_H, height - 90));
		int x = (width - anzahl * b - (anzahl - 1) * ABSTAND) / 2;
		int y = (height - h) / 2 + 10;
		addRenderableWidget(new NeonKarte(x, y, b, h, "GARDEROBE", "KOSMETIK ANPASSEN", NeonKarte.AVATAR,
				() -> minecraft.setScreen(new GarderobeScreen(this))));
		addRenderableWidget(new NeonKarte(x + b + ABSTAND, y, b, h, "MOD-MENÜ", "MODULE, FUNKTIONEN, HUD", symbol("alle"),
				() -> minecraft.setScreen(new BambusMenuScreen(this))));
		if (helfer) {
			addRenderableWidget(new NeonKarte(x + 2 * (b + ABSTAND), y, b, h, "HELFER", "FRAGEN & TIPPS", symbol("helfer"),
					() -> minecraft.setScreen(new HelferScreen(this))));
		}
	}

	@Override
	public void renderBackground(GuiGraphics g, int mx, int my, float delta) {
		g.fill(0, 0, width, height, 0xC0020407);
		Neon.gitter(g, 0, 0, width, height, 24, Farben.alpha(Farben.NEON, 7));
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float delta) {
		Farben.themaAnwenden(BambusClient.config.thema);
		super.render(g, mx, my, delta);

		// Kopf: Logo + Schriftzug, mittig über den Karten
		int kartenOben = children().isEmpty() ? height / 2 : ((NeonKarte) children().get(0)).getY();
		String titel = "BAMBUS";
		int tb = (int) (font.width(titel) * 1.5f);
		int kx = (width - tb - 18) / 2;
		Neon.logo(g, kx, kartenOben - 40, 16);
		Neon.leuchtText(g, font, titel, kx + 18, kartenOben - 39, 1.5f, Farben.NEON);
		String unter = "Was möchtest du öffnen?";
		int ub = (int) (font.width(unter) * 0.75f);
		Neon.text(g, font, unter, (width - ub) / 2, kartenOben - 20, 0.75f, Farben.GRAU, false);

		String hinweis = BambusClient.config.helferAn ? "ESC schließen · Helfer abschalten: Mod-Menü → MEHR → EXTRAS" : "ESC schließen";
		int hb = (int) (font.width(hinweis) * 0.6f);
		Neon.text(g, font, hinweis, (width - hb) / 2, height - 14, 0.6f, Farben.GRAU, false);

		// weiches Einblenden
		if (BambusClient.config.menueAnimation) {
			float k = Anim.aus(Math.min(1f, (System.currentTimeMillis() - offenSeit) / Anim.OEFFNEN_MS));
			if (k < 1f) {
				g.fill(0, 0, width, height, Farben.alpha(0xFF020407, (int) ((1 - k) * 255)));
			}
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
