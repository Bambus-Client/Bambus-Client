package de.bambus.client.screen;

import de.bambus.client.BambusClient;
import de.bambus.client.Farben;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Das Bambus-Hauptmenü: ersetzt den Minecraft-Titelbildschirm.
 * Synthwave-Gitter, fliegende Bambus-Segmente, Pixel-Logo; Einzelspieler und Mehrspieler als zwei große Karten
 * mit echten Minecraft-Gegenständen, darunter kleinere Knöpfe, rechts oben dein Profil.
 * Über "Minecraft-Menü" kommt man jederzeit zum normalen Titelbildschirm.
 */
public class BambusTitelScreen extends Screen {

	/** Wenn true, ersetzt der Client den normalen Titelbildschirm in dieser Sitzung nicht mehr. */
	public static boolean vanillaGewuenscht = false;

	private record Knopf(String text, String unter, int akzent, ItemStack symbol, Runnable aktion) {}

	private record Segment(float x, float y, float tempo, float groesse, int farbe) {}

	private final List<Knopf> knoepfe = new ArrayList<>();
	private final List<int[]> flaechen = new ArrayList<>(); // x, y, b, h, index
	private final List<Segment> segmente = new ArrayList<>();
	private final long start = System.currentTimeMillis();
	private final float[] hoverAnim = new float[10];

	public BambusTitelScreen() {
		super(Component.literal("Bambus Client"));
		Random r = new Random(7);
		for (int i = 0; i < 26; i++) {
			int farbe = r.nextInt(5) == 0 ? Farben.NEON_CYAN : r.nextInt(7) == 0 ? Farben.NEON_PINK : Farben.NEON;
			segmente.add(new Segment(r.nextFloat(), r.nextFloat(), 0.012f + r.nextFloat() * 0.03f, 2 + r.nextFloat() * 4, farbe));
		}
	}

	@Override
	protected void init() {
		knoepfe.clear();
		// die zwei großen Karten
		knoepfe.add(new Knopf("EINZELSPIELER", "Deine Welten", Farben.NEON, new ItemStack(Items.GRASS_BLOCK),
				() -> minecraft.setScreen(new SelectWorldScreen(this))));
		knoepfe.add(new Knopf("MEHRSPIELER", "Server & LAN", Farben.NEON_CYAN, new ItemStack(Items.DIAMOND_SWORD),
				() -> minecraft.setScreen(new JoinMultiplayerScreen(this))));
		// kleinere Knöpfe darunter
		knoepfe.add(new Knopf("BAMBUS-MENÜ", "Module · Texturen", Farben.NEON_PINK, new ItemStack(Items.BAMBOO),
				() -> minecraft.setScreen(new BambusMenuScreen(this))));
		if (FabricLoader.getInstance().isModLoaded("modmenu")) {
			knoepfe.add(new Knopf("MODS", "Alle Mods", 0xFFFFC846, new ItemStack(Items.BOOK), this::oeffneModMenu));
		}
		knoepfe.add(new Knopf("OPTIONEN", "Grafik · Ton", 0xFFB0C4DE, new ItemStack(Items.COMPARATOR),
				() -> minecraft.setScreen(new OptionsScreen(this, minecraft.options))));
		knoepfe.add(new Knopf("BEENDEN", "Bis bald!", Farben.ROT, new ItemStack(Items.OAK_DOOR), () -> minecraft.stop()));
	}

	/** Öffnet die Mod-Liste von Mod Menu (über dessen öffentliche API, ohne feste Abhängigkeit). */
	private void oeffneModMenu() {
		try {
			Class<?> api = Class.forName("com.terraformersmc.modmenu.api.ModMenuApi");
			Object screen = api.getMethod("createModsScreen", Screen.class).invoke(null, this);
			minecraft.setScreen((Screen) screen);
		} catch (Throwable t) {
			BambusClient.LOGGER.warn("Mod Menu konnte nicht geöffnet werden: {}", t.toString());
		}
	}

	private float zeit() {
		return (System.currentTimeMillis() - start) / 1000f;
	}

	// ============================================================ Zeichnen

	@Override
	public void renderBackground(GuiGraphics g, int mx, int my, float delta) {
		Farben.themaAnwenden(BambusClient.config.thema);
		float t = zeit();
		if (MenueHintergrund.zeichne(g, font, BambusClient.config.menueHintergrund, width, height, t)) {
			g.fillGradient(0, 0, width, height / 5, 0x90000000, 0x00000000);
			return;   // anderer Hintergrund gewählt (Launcher-Einstellungen oder Bambus-Menü)
		}
		// Himmel: dunkler Verlauf
		g.fillGradient(0, 0, width, height, 0xFF03060C, 0xFF0A0418);
		int horizont = (int) (height * 0.62f);

		// Neon-Sonne hinter dem Horizont
		int sx = width * 3 / 4;
		int sr = Math.min(width, height) / 5;
		for (int y = -sr; y < 0; y += 2) {
			float k = (float) Math.sqrt(Math.max(0, sr * sr - y * y));
			float p = (y + sr) / (float) sr;            // 0 oben .. 1 Horizont
			if (p > 0.45f && ((int) ((y + t * 8) / 3) & 3) == 0) {
				continue;                                 // Synthwave-Streifen
			}
			int farbe = mischen(Farben.NEON_PINK, 0xFFFFD54A, 1 - p);
			g.fill((int) (sx - k), horizont + y, (int) (sx + k), horizont + y + 2, Farben.alpha(farbe, 150));
		}
		g.fill(0, horizont - 1, width, horizont + 1, Farben.alpha(Farben.NEON_PINK, 200));

		// Boden: perspektivisches Gitter, das auf dich zufährt
		g.fill(0, horizont + 1, width, height, 0xFF05030C);
		float lauf = (t * 0.6f) % 1f;
		for (int i = 0; i < 18; i++) {
			float z = (i + 1 - lauf);                      // 0.x .. 18
			int y = horizont + (int) ((height - horizont) * 1.6f / (z + 0.6f));
			if (y > height || y <= horizont) {
				continue;
			}
			int al = (int) Mth.clamp(220 - z * 11, 30, 220);
			g.fill(0, y, width, y + 1, Farben.alpha(Farben.NEON, al));
		}
		int mitte = width / 2;
		for (int i = -14; i <= 14; i++) {
			for (int y = horizont + 1; y < height; y += 2) {
				float p = (y - horizont) / (float) (height - horizont);
				int x = mitte + (int) (i * 30 * (0.08f + p * 2.4f));
				if (x < -2 || x > width + 2) {
					continue;
				}
				g.fill(x, y, x + 1, y + 2, Farben.alpha(Farben.NEON_CYAN, (int) (40 + 120 * p)));
			}
		}

		// Fliegende Bambus-Segmente
		for (Segment s : segmente) {
			float y = (s.y() - t * s.tempo()) % 1f;
			if (y < 0) {
				y += 1;
			}
			int x = (int) (s.x() * width + Mth.sin(t * 0.7f + s.x() * 20) * 6);
			int py = (int) (y * horizont);
			int b = (int) s.groesse();
			int h = b * 4;
			g.fill(x - 1, py - 1, x + b + 1, py + h + 1, Farben.alpha(s.farbe(), 30));
			g.fill(x, py, x + b, py + h, Farben.alpha(s.farbe(), 120));
			g.fill(x, py + h / 2, x + b, py + h / 2 + 1, Farben.alpha(0xFF000000, 120));
		}
		// leichte Vignette
		g.fillGradient(0, 0, width, height / 5, 0x90000000, 0x00000000);
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float delta) {
		super.render(g, mx, my, delta);
		flaechen.clear();
		float t = zeit();
		float einblenden = Mth.clamp(t / 0.8f, 0, 1);
		boolean eng = height < 280;

		// dunkler Verlauf links, damit Logo und Karten gut lesbar sind
		int schatten = Math.min(width, 420);
		for (int x = 0; x < schatten; x += 4) {
			int a = (int) (150 * (1 - x / (float) schatten));
			g.fill(x, 0, x + 4, height, Farben.alpha(0xFF02040A, a));
		}

		// ---- Logo und Titel
		int lx = Math.max(16, width / 14);
		int ly = eng ? 12 : Math.max(16, height / 10);
		int lh = eng ? 36 : 54;
		Neon.logo(g, lx, ly, lh);
		int tx = lx + Neon.logoBreite(lh) + 8;
		float gross = eng ? 2.6f : 3.6f;
		int glanz = Farben.alpha(Farben.NEON, (int) (60 + 50 * Farben.puls()));
		Neon.text(g, font, "BAMBUS", tx + 2, ly + 3, gross, glanz, false);
		Neon.text(g, font, "BAMBUS", tx, ly + 1, gross, Farben.NEON, false);
		int unterY = ly + 1 + (int) (gross * 9) + 2;
		Neon.text(g, font, "C L I E N T", tx + 2, unterY, eng ? 0.85f : 1f, Farben.NEON_CYAN, false);
		Neon.text(g, font, "v" + BambusClient.VERSION + "  ·  Minecraft 1.21.11", tx + 2, unterY + (eng ? 10 : 12), 0.6f, Farben.GRAU, false);

		// ---- zwei große Karten: Einzelspieler und Mehrspieler
		int ky = ly + lh + (eng ? 10 : 18);
		int abstand = 8;
		int kb = Math.min(170, (Math.min(width - 2 * lx, 440) - abstand) / 2);
		int kh = eng ? 52 : 72;
		for (int i = 0; i < 2 && i < knoepfe.size(); i++) {
			int x = lx + i * (kb + abstand);
			karte(g, knoepfe.get(i), i, x, ky, kb, kh, mx, my, t, einblenden);
		}

		// ---- kleinere Knöpfe darunter
		int ry = ky + kh + 8;
		int rest = knoepfe.size() - 2;
		int gesamt = 2 * kb + abstand;
		int rb = (gesamt - (rest - 1) * 6) / Math.max(1, rest);
		int rh = eng ? 20 : 24;
		for (int i = 2; i < knoepfe.size(); i++) {
			int x = lx + (i - 2) * (rb + 6);
			kleinerKnopf(g, knoepfe.get(i), i, x, ry, rb, rh, mx, my, t, einblenden);
		}

		// ---- Profil rechts oben
		int pb = 128, ph = 40;
		int px = width - pb - 12, py = 12;
		int titelEnde = tx + (int) (font.width("BAMBUS") * gross) + 16;
		if (px > titelEnde && py + ph < ky - 4) {   // neben dem Titel, über den Karten
			profil(g, px, py, pb, ph);
		}

		// ---- unten: Tasten, Minecraft-Menü, Pflichthinweis
		String tasten = "RECHTS-SHIFT  Menü    C  Zoom    ALT  Freelook    H  HUD";
		Neon.text(g, font, tasten, lx, ry + rh + 8, 0.6f, Farben.alpha(Farben.GRAU, (int) (200 * einblenden)), false);
		String vanilla = "Minecraft-Menü";
		int vb = (int) (font.width(vanilla) * 0.75f);
		int vx = width - vb - 10;
		int vy = height - 14;
		boolean vh = Neon.ueber(mx, my, vx - 4, vy - 3, vb + 8, 13);
		g.fill(vx - 4, vy - 3, vx + vb + 4, vy + 10, vh ? 0xC0152233 : 0x80080D16);
		g.renderOutline(vx - 4, vy - 3, vb + 8, 13, vh ? Farben.NEON : Farben.LINIE);
		Neon.text(g, font, vanilla, vx, vy, 0.75f, vh ? Farben.WEISS : Farben.GRAU, false);
		flaechen.add(new int[]{vx - 4, vy - 3, vb + 8, 13, 101});
		// Pflichthinweis nach den Minecraft-Markenrichtlinien von Mojang
		Neon.text(g, font, "Kein offizielles Minecraft-Produkt.", 8, height - 18, 0.6f, 0xFF4A5A6E, false);
		Neon.text(g, font, "Nicht von Mojang oder Microsoft genehmigt oder mit ihnen verbunden.", 8, height - 11, 0.6f, 0xFF4A5A6E, false);
	}

	/** Große Karte mit Gegenstand, Titel und Untertitel; hebt sich beim Drüberfahren und leuchtet. */
	private void karte(GuiGraphics g, Knopf k, int i, int x, int y0, int b, int h, int mx, int my, float t, float einblenden) {
		float rein = Mth.clamp((t - 0.15f - i * 0.1f) / 0.45f, 0, 1);
		int y = y0 + (int) ((1 - rein) * 14);
		int al = (int) (255 * rein * einblenden);
		if (al < 8) return;
		boolean hover = Neon.ueber(mx, my, x, y0, b, h);
		hoverAnim[i] += ((hover ? 1f : 0f) - hoverAnim[i]) * 0.25f;
		float hv = hoverAnim[i];
		y -= (int) (hv * 2);   // leicht anheben

		g.fillGradient(x, y, x + b, y + h, Farben.alpha(0xFF0B1626, (int) (al * 0.92f)), Farben.alpha(0xFF050A10, (int) (al * 0.92f)));
		Neon.gitter(g, x, y, b, h, 12, Farben.alpha(k.akzent(), (int) ((10 + 14 * hv) * rein)));
		g.fillGradient(x, y + h - h / 2, x + b, y + h, Farben.alpha(k.akzent(), 0), Farben.alpha(k.akzent(), (int) ((18 + 30 * hv) * rein)));
		if (hv > 0.05f) {
			Neon.leuchtRahmen(g, x, y, b, h, Farben.alpha(k.akzent(), (int) (255 * hv * rein)), hv);
		} else {
			g.renderOutline(x, y, b, h, Farben.alpha(Farben.LINIE, al));
		}
		Neon.ecken(g, x, y, b, h, 8, Farben.alpha(k.akzent(), al));
		g.fill(x, y + 6, x + 2, y + h - 6, Farben.alpha(k.akzent(), al));

		// Gegenstand groß (pixelig wie im Inventar), wippt leicht beim Drüberfahren
		float s = h >= 64 ? 2.5f : 2f;
		int is = (int) (16 * s);
		int ix = x + 12, iy = y + (h - is) / 2 - (int) (Mth.sin(t * 6) * 1.5f * hv);
		g.fill(ix - 3, iy - 3, ix + is + 3, iy + is + 3, Farben.alpha(k.akzent(), (int) ((20 + 25 * hv) * rein)));
		g.pose().pushMatrix();
		g.pose().translate(ix, iy);
		g.pose().scale(s, s);
		g.renderItem(k.symbol(), 0, 0);
		g.pose().popMatrix();

		int textX = ix + is + 10;
		float ts = b >= 160 ? 1.25f : 1f;
		Neon.text(g, font, k.text(), textX + 1, y + h / 2 - 9 + 1, ts, Farben.alpha(0xFF000000, al / 2), false);
		Neon.text(g, font, k.text(), textX, y + h / 2 - 9, ts, Farben.alpha(hover ? Farben.WEISS : k.akzent(), al), false);
		Neon.text(g, font, k.unter(), textX, y + h / 2 + 5, 0.75f, Farben.alpha(Farben.GRAU, al), false);
		Neon.text(g, font, "›", x + b - 12 + (int) (hv * 3), y + h / 2 - 4, 1f, Farben.alpha(k.akzent(), (int) (al * (0.35f + 0.65f * hv))), false);
		flaechen.add(new int[]{x, y0, b, h, i});
	}

	/** Kleiner Knopf mit Gegenstand und Text. */
	private void kleinerKnopf(GuiGraphics g, Knopf k, int i, int x, int y0, int b, int h, int mx, int my, float t, float einblenden) {
		float rein = Mth.clamp((t - 0.35f - i * 0.06f) / 0.4f, 0, 1);
		int y = y0 + (int) ((1 - rein) * 10);
		int al = (int) (255 * rein * einblenden);
		if (al < 8) return;
		boolean hover = Neon.ueber(mx, my, x, y0, b, h);
		hoverAnim[i] += ((hover ? 1f : 0f) - hoverAnim[i]) * 0.25f;
		float hv = hoverAnim[i];
		g.fill(x, y, x + b, y + h, Farben.alpha(0xFF0A1220, (int) (al * 0.88f)));
		g.fill(x, y, x + (int) (b * hv), y + h, Farben.alpha(k.akzent(), (int) (34 * hv)));
		if (hover) {
			Neon.leuchtRahmen(g, x, y, b, h, k.akzent(), 0.5f + 0.4f * Farben.puls());
		} else {
			g.renderOutline(x, y, b, h, Farben.alpha(Farben.LINIE, al));
		}
		g.fill(x, y + h - 2, x + b, y + h, Farben.alpha(k.akzent(), (int) (al * (0.45f + 0.55f * hv))));
		int iy = y + (h - 16) / 2 - 1;
		g.renderItem(k.symbol(), x + 4, iy);
		float ts = font.width(k.text()) > b - 26 ? 0.6f : 0.75f;
		Neon.text(g, font, k.text(), x + 23, y + (h - (int) (8 * ts)) / 2 - 1, ts, Farben.alpha(hover ? Farben.WEISS : k.akzent(), al), false);
		flaechen.add(new int[]{x, y0, b, h, i});
	}

	/** Profil-Karte: Spielername, Rang und Blätter. */
	private void profil(GuiGraphics g, int x, int y, int b, int h) {
		g.fill(x, y, x + b, y + h, 0xC0080D16);
		Neon.ecken(g, x - 2, y - 2, b + 4, h + 4, 6, Farben.alpha(de.bambus.client.Blaetter.rangFarbe(), 200));
		g.renderItem(new ItemStack(Items.PLAYER_HEAD), x + 6, y + (h - 16) / 2);
		String name = minecraft.getUser() == null ? "Spieler" : minecraft.getUser().getName();
		boolean dev = de.bambus.client.Blaetter.istEntwickler();
		Neon.text(g, font, font.plainSubstrByWidth(name, (int) ((b - 34) / 0.85f)), x + 27, y + 7, 0.85f, Farben.WEISS, false);
		Neon.text(g, font, dev ? "ENTWICKLER" : de.bambus.client.Blaetter.rangName().toUpperCase(), x + 27, y + 18, 0.6f,
				dev ? Farben.NEON_PINK : de.bambus.client.Blaetter.rangFarbe(), false);
		Neon.text(g, font, de.bambus.client.Blaetter.blaetter() + " Blätter", x + 27, y + 27, 0.6f, Farben.GRAU, false);
	}

	private static int mischen(int a, int b, float t) {
		t = Mth.clamp(t, 0, 1);
		int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
		int gr = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
		int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
		return 0xFF000000 | (r << 16) | (gr << 8) | bl;
	}

	// ============================================================ Eingabe

	@Override
	public boolean mouseClicked(MouseButtonEvent e, boolean doppelt) {
		if (e.button() == 0) {
			for (int[] f : flaechen) {
				if (Neon.ueber(e.x(), e.y(), f[0], f[1], f[2], f[3])) {
					minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
					if (f[4] == 101) {
						vanillaGewuenscht = true;
						minecraft.setScreen(new TitleScreen());
					} else {
						knoepfe.get(f[4]).aktion().run();
					}
					return true;
				}
			}
		}
		return super.mouseClicked(e, doppelt);
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
