package de.bambus.client.screen;

import de.bambus.client.BambusClient;
import de.bambus.client.Farben;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Erster Bildschirm nach Rechts-Shift: Garderobe, Mod-Menü, HUD bearbeiten oder – falls eingeschaltet –
 * Bambus-Helfer öffnen. Bewusst kompakt, damit das Spiel dahinter sichtbar bleibt.
 */
public class BambusAuswahlScreen extends Screen {

	private static final int KARTE_B = 84;
	private static final int KARTE_MAX_H = 100;
	private static final int ABSTAND = 8;

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
		java.util.List<NeonKarte> karten = new java.util.ArrayList<>();
		int h = Math.max(64, Math.min(KARTE_MAX_H, (int) (height * 0.42f)));
		karten.add(new NeonKarte(0, 0, KARTE_B, h, "GARDEROBE", "KOSMETIK", NeonKarte.AVATAR,
				() -> minecraft.setScreen(new GarderobeScreen(this))));
		karten.add(new NeonKarte(0, 0, KARTE_B, h, "MOD-MENÜ", "MODULE & MEHR", symbol("alle"),
				() -> minecraft.setScreen(new BambusMenuScreen(this))));
		if (minecraft.player != null) {   // der HUD-Editor braucht eine Welt
			karten.add(new NeonKarte(0, 0, KARTE_B, h, "HUD", "BEARBEITEN", symbol("hud"),
					() -> minecraft.setScreen(new HudEditorScreen(this))));
		}
		karten.add(new NeonKarte(0, 0, KARTE_B, h, "QUESTS", "BLÄTTER & SHOP", symbol("stern"),
				() -> minecraft.setScreen(new QuestScreen(this))));
		if (BambusClient.config.helferAn) {
			karten.add(new NeonKarte(0, 0, KARTE_B, h, "HELFER", "FRAGEN & TIPPS", symbol("helfer"),
					() -> minecraft.setScreen(new HelferScreen(this))));
		}
		int n = karten.size();
		int b = Math.min(KARTE_B, ((int) (width * 0.85f) - ABSTAND * (n - 1)) / n);   // höchstens 85 % der Breite
		int x = (width - n * b - (n - 1) * ABSTAND) / 2;
		int y = (height - h) / 2 + 10;
		for (NeonKarte k : karten) {
			k.setX(x);
			k.setY(y);
			k.setWidth(b);
			addRenderableWidget(k);
			x += b + ABSTAND;
		}
	}

	@Override
	public void renderBackground(GuiGraphics g, int mx, int my, float delta) {
		g.fill(0, 0, width, height, 0xC0020407);
		Neon.gitter(g, 0, 0, width, height, 24, Farben.alpha(Farben.NEON, 7));
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float delta) {
		MenueStil.anwenden();
		super.render(g, mx, my, delta);

		// Kopf: Logo + Schriftzug, mittig über den Karten
		int kartenOben = children().isEmpty() ? height / 2 : ((NeonKarte) children().get(0)).getY();
		String titel = "BAMBUS";
		int tb = font.width(titel);
		int kx = (width - tb - 16) / 2;
		Neon.logo(g, kx, kartenOben - 26, 12);
		Neon.leuchtText(g, font, titel, kx + 16, kartenOben - 24, 1f, Farben.NEON);

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
