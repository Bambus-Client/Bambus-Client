package de.bambus.client.screen;

import de.bambus.client.Farben;
import de.bambus.client.Garderobe;
import de.bambus.client.Kosmetik;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;

import java.util.Map;

/**
 * Vorschau-Symbole für Kosmetik-Kacheln (Bambus-Menü und Garderobe).
 * Jede Art hat ihr eigenes Motiv: Capes und Flügel zeigen die Textur, Partikel einen Funkenschwarm,
 * Hüte die Hutform, Kill-Effekte ihr Motiv mit Ring, Pfeil-Spuren einen Pfeil mit passender Spur.
 *
 * Pixelbilder: '#' = Farbe, '+' = hell, '-' = dunkel, 'w' = weiß, 's' = Schaft (hellgrau), '.' = leer.
 */
public final class KosmetikSymbole {

	private KosmetikSymbole() {}

	private static final int SCHAFT = 0xFFC8D2DE;

	// ------------------------------------------------------------------ Hüte (12 × 12)

	private static final Map<String, String[]> HUETE = Map.of(
			"krone", new String[]{
					"............",
					"............",
					".+...++...+.",
					".#...##...#.",
					".##..##..##.",
					".###.##.###.",
					".##########.",
					".##+####+##.",
					".#+w+##+w+#.",
					".##+####+##.",
					".----------.",
					"............"},
			"zylinder", new String[]{
					"............",
					"...######...",
					"...#+####...",
					"...#+####...",
					"...#+####...",
					"...#+####...",
					"...++++++...",
					"...#+####...",
					".##########.",
					".----------.",
					"............",
					"............"},
			"heiligenschein", new String[]{
					"............",
					"............",
					"............",
					"...++++++...",
					".##......##.",
					"#+........+#",
					"#-........-#",
					".##......##.",
					"...------...",
					"............",
					"............",
					"............"},
			"hoerner", new String[]{
					"............",
					"+..........+",
					"#+........+#",
					"##........##",
					".##......##.",
					".###....###.",
					"..###..###..",
					".##########.",
					".----------.",
					"............",
					"............",
					"............"},
			"katzenohren", new String[]{
					"............",
					"............",
					".#........#.",
					".##......##.",
					".#+#....#+#.",
					".#++#..#++#.",
					".####..####.",
					".##########.",
					".----------.",
					"............",
					"............",
					"............"},
			"partyhut", new String[]{
					".....ww.....",
					".....ww.....",
					".....##.....",
					"....#+##....",
					"....##+#....",
					"...#+##+#...",
					"...##+##+...",
					"..#+##+##+..",
					"..##+##+##..",
					".#+##+##+##.",
					".----------.",
					"............"},
			"bandana", new String[]{
					"............",
					"............",
					"..########..",
					".##+##+##+#.",
					".##########.",
					".--------##.",
					".........#+#",
					"........##.#",
					"........#...",
					"............",
					"............",
					"............"},
			"visier", new String[]{
					"............",
					"............",
					"............",
					".##########.",
					"#++++++++++#",
					"#+wwww+++++#",
					"#++++++++++#",
					".##########.",
					".----------.",
					"............",
					"............",
					"............"}
	);

	// ------------------------------------------------------------ Kill-Effekte (12 × 12)

	private static final Map<String, String[]> KILL = Map.of(
			"blitz", new String[]{
					"......####..",
					".....#+##...",
					"....#+##....",
					"...#+##.....",
					"..#+######..",
					".....+##....",
					"....+##.....",
					"...+##......",
					"..+##.......",
					"..##........",
					".#..........",
					"............"},
			"feuerwerk", new String[]{
					".....+......",
					".+...#...+..",
					"..#..#..#...",
					"...#.#.#....",
					"....+++.....",
					"+####w####+.",
					"....+++.....",
					"...#.#.#....",
					"..#..#..#...",
					".+...#...+..",
					".....+......",
					"............"},
			"seelen", new String[]{
					"............",
					"....####....",
					"...#+####...",
					"..#+######..",
					"..#+-##-##..",
					"..##-##-##..",
					"..########..",
					"..########..",
					"..########..",
					"..##.##.##..",
					"............",
					"............"},
			"flammen", new String[]{
					".....#......",
					".....##.....",
					"....###.....",
					"....###.#...",
					"...####.##..",
					"..###+####..",
					"..##+++###..",
					".###+w+####.",
					".##++w++###.",
					".##+www+##..",
					"..##+++##...",
					"...#####...."},
			"herzen", new String[]{
					"............",
					"............",
					".###....###.",
					"#####..#####",
					"#+##########",
					"#++#########",
					"#+##########",
					".##########.",
					"..########..",
					"...######...",
					"....####....",
					".....##....."},
			"totem", new String[]{
					"...######...",
					"...#+##+#...",
					"...#-##-#...",
					"...######...",
					"....#--#....",
					".##########.",
					"#+########+#",
					"....####....",
					"....#++#....",
					"....####....",
					"...##..##...",
					"............"}
	);

	// ---------------------------------------------------------- Pfeil-Spuren

	/** Pfeil von links unten nach rechts oben (10 × 10). */
	private static final String[] PFEIL = {
			"......wwww",
			".......www",
			"......s.ww",
			".....s...w",
			"....s.....",
			"...s......",
			".#s.......",
			"#s#.......",
			".#........",
			".........."};

	/** Kleine Motive der Spur (5 × 5). */
	private static final Map<String, String[]> SPUR_MOTIVE = Map.of(
			"funken", new String[]{"..#..", "..#..", "##w##", "..#..", "..#.."},
			"glitzer", new String[]{"..#..", ".#w#.", "#w+w#", ".#w#.", "..#.."},
			"flammen", new String[]{"..#..", ".##..", ".###.", "##+##", ".#w#."},
			"seelen", new String[]{".#...", "..#..", ".#+#.", "#+w+#", ".###."},
			"noten", new String[]{"..###", "..#.#", "..#..", "###..", "##..."},
			"magie", new String[]{"#...#", ".#.#.", "..w..", ".#.#.", "#...#"},
			"herzen", new String[]{".#.#.", "#####", "#####", ".###.", "..#.."}
	);

	// ------------------------------------------------- kleine Symbole für die Seitenleiste (12 × 12)

	private static final Map<Garderobe.Art, String[]> ART_SYMBOLE = Map.of(
			Garderobe.Art.CAPES, new String[]{
					"............",
					"..########..",
					"..########..",
					"..#......#..",
					"..#......#..",
					"..#......#..",
					".#........#.",
					".#........#.",
					".#........#.",
					"#..........#",
					"############",
					"............"},
			Garderobe.Art.FLUEGEL, new String[]{
					"............",
					"##........##",
					"###......###",
					"####....####",
					".####..####.",
					".##########.",
					"..###..###..",
					"..##....##..",
					"...#....#...",
					"............",
					"............",
					"............"},
			Garderobe.Art.PARTIKEL, new String[]{
					".....#......",
					".....#......",
					"...#####....",
					".....#....#.",
					".....#...###",
					"..........#.",
					".#..........",
					"###.....#...",
					".#.....###..",
					"........#...",
					"............",
					"............"},
			Garderobe.Art.HUETE, HUETE.get("krone"),
			Garderobe.Art.KILL, new String[]{
					"............",
					"...######...",
					"..########..",
					".##########.",
					".##..##..##.",
					".##..##..##.",
					".##########.",
					"..###..###..",
					"...######...",
					"...#.##.#...",
					"............",
					"............"},
			Garderobe.Art.PFEIL, new String[]{
					"........####",
					".........###",
					"........##.#",
					".......##..#",
					"......##....",
					".....##.....",
					"....##......",
					"#..##.......",
					"####........",
					".##.........",
					"###.........",
					"............"}
	);

	// =================================================================== Zeichnen

	/** Großes Symbol einer Kachel, zentriert um (cx, cy). saat = Versatz für die Animation. */
	public static void zeichne(GuiGraphics g, Garderobe.Art art, String id, int farbe, int cx, int cy, int saat) {
		switch (art) {
			case CAPES -> cape(g, id, farbe, cx, cy);
			case FLUEGEL -> fluegel(g, id, farbe, cx, cy);
			case PARTIKEL -> partikel(g, cx, cy, farbe, saat);
			case HUETE -> hut(g, id, farbe, cx, cy, saat);
			case KILL -> killEffekt(g, id, farbe, cx, cy, saat);
			case PFEIL -> pfeilSpur(g, id, farbe, cx, cy, saat);
		}
	}

	/** Kleines einfarbiges Symbol (12 × 12) einer Kosmetik-Art, z. B. für die Seitenleiste. */
	public static void artSymbol(GuiGraphics g, Garderobe.Art art, int x, int y, int farbe) {
		bild(g, ART_SYMBOLE.get(art), x, y, 1, farbe, true);
	}

	/** Vorderseite des Capes (10 × 16 Pixel im 64 × 32-Bild), doppelt groß. */
	private static void cape(GuiGraphics g, String id, int farbe, int cx, int cy) {
		g.fill(cx - 11, cy - 23, cx + 11, cy + 11, Farben.alpha(farbe, 40));
		g.blit(RenderPipelines.GUI_TEXTURED, Kosmetik.capeBild(id), cx - 10, cy - 22, 1f, 1f, 20, 32, 10, 16, 64, 32);
	}

	private static void fluegel(GuiGraphics g, String id, int farbe, int cx, int cy) {
		g.fill(cx - 23, cy - 24, cx + 23, cy + 16, Farben.alpha(farbe, 30));
		if (Kosmetik.fluegelForm(id) != null) {   // 3D-Flügel: Bild 2:1
			g.blit(RenderPipelines.GUI_TEXTURED, Kosmetik.fluegelBild(id), cx - 22, cy - 15, 0f, 0f, 44, 22, 64, 32, 64, 32);
		} else {
			g.blit(RenderPipelines.GUI_TEXTURED, Kosmetik.fluegelBild(id), cx - 18, cy - 22, 0f, 0f, 36, 36, 64, 64, 64, 64);
		}
	}

	/** Schwebender Funkenschwarm. */
	public static void partikel(GuiGraphics g, int cx, int cy, int farbe, int saat) {
		float p = Farben.puls();
		g.fill(cx - 10, cy - 10, cx + 10, cy + 10, Farben.alpha(farbe, (int) (18 + 22 * p)));
		g.fill(cx - 6, cy - 6, cx + 6, cy + 6, Farben.alpha(farbe, 50));
		int[][] punkte = {{-8, -6}, {6, -9}, {9, 4}, {-5, 8}, {1, -1}, {-10, 2}};
		long zeit = System.currentTimeMillis() / 120;
		for (int i = 0; i < punkte.length; i++) {
			int dy = (int) ((zeit + i * 3 + saat) % 6) - 3;
			int x = cx + punkte[i][0];
			int y = cy + punkte[i][1] + dy;
			g.fill(x, y, x + 2, y + 2, farbe);
		}
	}

	/** Hutform, wippt sanft auf und ab. */
	private static void hut(GuiGraphics g, String id, int farbe, int cx, int cy, int saat) {
		schein(g, cx, cy, farbe);
		int wippen = (int) Math.round(Math.sin(System.currentTimeMillis() / 380.0 + saat) * 1.2);
		bild(g, HUETE.get(id), cx - 12, cy - 12 + wippen, 2, farbe, false);
	}

	/** Motiv des Kill-Effekts mit einem Ring, der sich ausbreitet. */
	private static void killEffekt(GuiGraphics g, String id, int farbe, int cx, int cy, int saat) {
		schein(g, cx, cy, farbe);
		float k = ((System.currentTimeMillis() + saat * 170L) % 1400L) / 1400f;
		int r = 8 + Math.round(k * 9);
		int deckkraft = (int) ((1 - k) * 200);
		for (int i = 0; i < 10; i++) {
			double w = i * Math.PI * 2 / 10;
			int x = cx + (int) Math.round(Math.cos(w) * r);
			int y = cy + (int) Math.round(Math.sin(w) * r * 0.8);
			g.fill(x - 1, y - 1, x + 1, y + 1, Farben.alpha(farbe, deckkraft));
		}
		bild(g, KILL.get(id), cx - 12, cy - 12, 2, farbe, false);
	}

	/** Pfeil mit Spur aus den passenden Partikeln, die nach hinten wegtreiben. */
	private static void pfeilSpur(GuiGraphics g, String id, int farbe, int cx, int cy, int saat) {
		g.fill(cx - 13, cy - 13, cx + 13, cy + 13, Farben.alpha(farbe, 14));
		String[] motiv = SPUR_MOTIVE.get(id);
		float lauf = ((System.currentTimeMillis() + saat * 90L) % 900L) / 900f;
		for (int i = 0; i < 3; i++) {
			float t = (i + lauf) / 3f;               // 0 = am Pfeil, 1 = weit hinten
			int mx = cx - 6 - Math.round(t * 9);
			int my = cy + 4 + Math.round(t * 9);
			int deckkraft = (int) ((1 - t) * 255);
			if (motiv != null) {
				bild(g, motiv, mx - 2, my - 2, 1, Farben.alpha(farbe, Math.max(30, deckkraft)), false);
			}
		}
		bild(g, PFEIL, cx - 6, cy - 14, 2, farbe, false);
	}

	/** Weicher, pulsierender Schein hinter einem Motiv. */
	private static void schein(GuiGraphics g, int cx, int cy, int farbe) {
		int a = (int) (14 + 16 * Farben.puls());
		g.fill(cx - 13, cy - 9, cx + 13, cy + 9, Farben.alpha(farbe, a));
		g.fill(cx - 9, cy - 13, cx + 9, cy + 13, Farben.alpha(farbe, a));
		g.fill(cx - 7, cy - 7, cx + 7, cy + 7, Farben.alpha(farbe, a));
	}

	/** Zeichnet ein Pixelbild; gleiche Pixel nebeneinander werden auf einmal gefüllt. */
	private static void bild(GuiGraphics g, String[] zeilen, int x, int y, int px, int farbe, boolean einfarbig) {
		if (zeilen == null) {
			return;
		}
		int deckkraft = farbe >>> 24;
		for (int r = 0; r < zeilen.length; r++) {
			String z = zeilen[r];
			int start = 0;
			while (start < z.length()) {
				char zeichen = z.charAt(start);
				int ende = start + 1;
				while (ende < z.length() && z.charAt(ende) == zeichen) {
					ende++;
				}
				if (zeichen != '.') {
					int f = einfarbig ? farbe : Farben.alpha(farbton(zeichen, farbe), deckkraft);
					g.fill(x + start * px, y + r * px, x + ende * px, y + (r + 1) * px, f);
				}
				start = ende;
			}
		}
	}

	private static int farbton(char zeichen, int farbe) {
		int voll = farbe | 0xFF000000;
		return switch (zeichen) {
			case '+' -> Anim.mische(voll, Farben.WEISS, 0.5f);
			case '-' -> Anim.mische(voll, 0xFF000000, 0.5f);
			case 'w' -> Farben.WEISS;
			case 's' -> SCHAFT;
			default -> voll;
		};
	}
}
