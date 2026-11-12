package de.bambus.client.screen;

import com.mojang.blaze3d.platform.NativeImage;
import de.bambus.client.BambusClient;
import de.bambus.client.Farben;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Die animierten Hintergründe aus dem Launcher auch im Bambus-Hauptmenü
 * (Partikel, Aurora, Datenregen, Gitter) sowie ein eigenes Bild aus dem config-Ordner.
 * „synthwave“ zeichnet BambusTitelScreen selbst.
 */
public final class MenueHintergrund {

	public static final String[] ARTEN = {"synthwave", "partikel", "aurora", "datenregen", "gitter", "bild"};
	public static final String[] NAMEN = {"Synthwave", "Partikel", "Aurora", "Datenregen", "Gitter", "Eigenes Bild"};

	/** Eigenes Hintergrundbild – dieselbe Datei, die der Launcher schreibt. */
	public static final String BILD_DATEI = "bambus-hintergrund.png";
	private static final Identifier BILD_ID = Identifier.fromNamespaceAndPath(BambusClient.MOD_ID, "hintergrund/eigen");
	private static boolean bildGeladen;
	private static boolean bildDa;
	private static long bildStand;
	private static int bildB, bildH;

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
		for (int i = 0; i < ARTEN.length; i++) {
			if (!ARTEN[i].equals(art)) continue;
			String n = ARTEN[(i + 1) % ARTEN.length];
			if (n.equals("bild") && !ladeBild()) n = ARTEN[(i + 2) % ARTEN.length];   // ohne Bilddatei überspringen
			return n;
		}
		return ARTEN[0];
	}

	public static Path bildDatei() {
		return FabricLoader.getInstance().getConfigDir().resolve(BILD_DATEI);
	}

	/** Lädt das eigene Bild beim ersten Mal (und neu, wenn der Launcher es ausgetauscht hat). */
	private static boolean ladeBild() {
		Path p = bildDatei();
		long stand;
		try {
			stand = Files.exists(p) ? Files.getLastModifiedTime(p).toMillis() : 0L;
		} catch (Exception e) {
			stand = 0L;
		}
		if (bildGeladen && stand == bildStand) return bildDa;
		bildGeladen = true;
		bildStand = stand;
		bildDa = false;
		if (stand == 0L) return false;
		try (InputStream in = Files.newInputStream(p)) {
			NativeImage bild = NativeImage.read(in);
			bildB = bild.getWidth();
			bildH = bild.getHeight();
			Minecraft.getInstance().getTextureManager().register(BILD_ID, new DynamicTexture(() -> "bambus hintergrund", bild));
			bildDa = bildB > 0 && bildH > 0;
		} catch (Exception e) {
			BambusClient.LOGGER.warn("Hintergrundbild {} konnte nicht gelesen werden", p, e);
		}
		return bildDa;
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
			case "bild" -> {
				if (!bild(g, b, h)) Neon.gitter(g, 0, 0, b, h, 20, Farben.alpha(Farben.NEON, 14));
			}
			default -> Neon.gitter(g, 0, 0, b, h, 20, Farben.alpha(Farben.NEON, 14));
		}
		return true;
	}

	/** Eigenes Bild bildschirmfüllend (Überstand wird abgeschnitten) und abgedunkelt. */
	private static boolean bild(GuiGraphics g, int b, int h) {
		if (!ladeBild()) return false;
		float s = Math.max(b / (float) bildB, h / (float) bildH);
		int zb = Mth.ceil(bildB * s), zh = Mth.ceil(bildH * s);
		g.blit(RenderPipelines.GUI_TEXTURED, BILD_ID, (b - zb) / 2, (h - zh) / 2, 0f, 0f, zb, zh, bildB, bildH, bildB, bildH);
		g.fill(0, 0, b, h, 0x69050711);                       // abdunkeln, damit Schrift lesbar bleibt
		g.fillGradient(0, 0, b, h, 0x28050711, 0x87050711);
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
