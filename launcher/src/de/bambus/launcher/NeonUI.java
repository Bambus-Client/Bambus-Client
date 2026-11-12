package de.bambus.launcher;

import javax.swing.JButton;
import javax.swing.JComponent;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/** Farben, Schriften und Bauteile im Neon-Look. */
final class NeonUI {

	private NeonUI() {}

	static final Color HG = new Color(0x05070D);
	static final Color SEITENLEISTE = new Color(0x080C14);
	static final Color KARTE = new Color(0x0B1322);
	static final Color KARTE_HOVER = new Color(0x112038);
	static final Color LINIE = new Color(0x1A2A40);
	static final Color TEXT = new Color(0xE6F1FF);
	static final Color GRAU = new Color(0x7D8DA6);
	static final Color DUNKEL = new Color(0x04110A);

	// Akzentfarben – nicht final, weil das Farbthema sie ändert (gleiche Themen wie im Spiel)
	static Color NEON = new Color(0x3DFF8A);
	static Color CYAN = new Color(0x00E5FF);
	static Color PINK = new Color(0xFF2BD6);

	/** Farbthemen wie im Spiel (Farben.THEMEN der Mod): {Name, Haupt, Akzent, Zweitakzent}. */
	static final String[] THEMEN = {"NEON", "OZEAN", "SAKURA", "GOLD", "LILA", "EIS", "RGB"};
	private static final int[][] THEMA_FARBEN = {
			{0x3DFF8A, 0x00E5FF, 0xFF2BD6},
			{0x00E5FF, 0x2B6BFF, 0x7DF9FF},
			{0xFF8CC0, 0xFFC2DD, 0xFF2BD6},
			{0xFFC846, 0xFF8A3D, 0xFF4D6D},
			{0xB06BFF, 0xFF2BD6, 0x00E5FF},
			{0xAEEBFF, 0xFFFFFF, 0x5AE6FF},
			{0x3DFF8A, 0x00E5FF, 0xFF2BD6},
	};
	private static int thema;

	static int thema() { return thema; }

	/** Hauptfarbe eines Themas (für Farbfelder in den Einstellungen). */
	static Color themaFarbe(int t) {
		return new Color(THEMA_FARBEN[Math.floorMod(t, THEMEN.length)][0]);
	}

	/** Setzt die Farben des Themas. Bei RGB jedes Bild aufrufen, dann dreht sich die Farbe langsam weiter. */
	static void themaAnwenden(int t) {
		thema = Math.floorMod(t, THEMEN.length);
		if (THEMEN[thema].equals("RGB")) {
			float h = (System.currentTimeMillis() % 8000L) / 8000f;
			NEON = Color.getHSBColor(h, 0.75f, 1f);
			CYAN = Color.getHSBColor((h + 0.33f) % 1f, 0.8f, 1f);
			PINK = Color.getHSBColor((h + 0.66f) % 1f, 0.8f, 1f);
		} else {
			NEON = new Color(THEMA_FARBEN[thema][0]);
			CYAN = new Color(THEMA_FARBEN[thema][1]);
			PINK = new Color(THEMA_FARBEN[thema][2]);
		}
	}

	static boolean rgb() { return THEMEN[thema].equals("RGB"); }

	// =================================================================== Pixelschrift

	/**
	 * Pixelschrift im Minecraft-Stil: „Monocraft“ von Idrees Hassan (SIL Open Font License 1.1,
	 * siehe resources/OFL-Monocraft.txt). Ein Schrift-Pixel ist 1/9 der Schriftgröße – darum werden
	 * die Größen auf passende Stufen gerundet, damit die Pixel scharf bleiben.
	 */
	private static Font pixel;
	private static boolean pixelGeladen;

	private static Font pixelSchrift() {
		if (!pixelGeladen) {
			pixelGeladen = true;
			try (java.io.InputStream in = NeonUI.class.getResourceAsStream("/Monocraft.ttf")) {
				if (in != null) pixel = Font.createFont(Font.TRUETYPE_FONT, in);
			} catch (Exception e) {
				pixel = null;
			}
		}
		return pixel;
	}

	/** Gewünschte Größe (wie früher bei der normalen Schrift) → Pixelschrift-Größe. */
	static float pixelGroesse(int groesse) {
		if (groesse <= 11) return 9f;
		if (groesse <= 14) return 12f;
		if (groesse <= 17) return 13.5f;
		if (groesse <= 22) return 18f;
		if (groesse <= 28) return 27f;
		if (groesse <= 40) return 36f;
		return 45f;
	}

	private static Font schrift(int stil, int groesse) {
		Font p = pixelSchrift();
		if (p == null) return new Font(Font.SANS_SERIF, stil, groesse);
		return p.deriveFont(Font.PLAIN, pixelGroesse(groesse));
	}

	static Font titel(int groesse) {
		return schrift(Font.BOLD, groesse);
	}

	static Font normal(int groesse) {
		return schrift(Font.PLAIN, groesse);
	}

	static Font mono(int groesse) {
		return schrift(Font.BOLD, groesse);
	}

	/** Text (schon übersetzt) so kürzen, dass er in die Breite passt – mit „…“ am Ende. */
	static String passend(Graphics2D g, String text, int max) {
		String t = Sprache.englisch() ? Sprache.t(text) : text;
		FontMetrics fm = g.getFontMetrics();
		if (fm.stringWidth(t) <= max) return t;
		while (t.length() > 1 && fm.stringWidth(t + "…") > max) t = t.substring(0, t.length() - 1);
		return t.trim() + "…";
	}

	/** Schatten unter Text – wie bei Minecraft eine dunkle Kopie der Farbe, um ein Schrift-Pixel versetzt. */
	static void schattenText(Graphics2D g, String text, int x, int y, Color farbe) {
		int d = Math.max(1, Math.round(g.getFont().getSize2D() / 9f));
		g.setColor(new Color(farbe.getRed() / 4, farbe.getGreen() / 4, farbe.getBlue() / 4, farbe.getAlpha()));
		g.drawString(text, x + d, y + d);
		g.setColor(farbe);
		g.drawString(text, x, y);
	}

	static Color alpha(Color c, int a) {
		return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, a)));
	}

	static Color farbe(String hex) {
		return new Color(Integer.parseInt(hex, 16));
	}

	/** Langsames Pulsieren zwischen 0 und 1. */
	static float puls() {
		return (float) (0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 500.0));
	}

	static Graphics2D glatt(Graphics graphics) {
		Graphics2D g = (Graphics2D) graphics.create();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		// Pixelschrift ohne Kantenglättung, damit die Pixel scharf bleiben
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
				pixelSchrift() != null ? RenderingHints.VALUE_TEXT_ANTIALIAS_OFF : RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		return new UebersetzteGrafik(g);   // Sprache + eckige Kästen (Pixel-Look)
	}

	/** Neon-Leuchten um eine Form. */
	static void leuchten(Graphics2D g, Shape form, Color farbe, int staerke) {
		for (int i = staerke; i >= 1; i--) {
			g.setColor(alpha(farbe, (int) (60.0 / (i + 1))));
			g.setStroke(new BasicStroke(i * 2f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
			g.draw(form);
		}
		g.setStroke(new BasicStroke(2f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
		g.setColor(farbe);
		g.draw(form);
	}

	/** Leuchtender Text. */
	static void leuchtText(Graphics2D g, String text, int x, int y, Color farbe, int staerke) {
		if (staerke > 1 && farbe != TEXT) {   // leichtes Leuchten nur bei farbigen Überschriften
			g.setColor(alpha(farbe, 26));
			for (int dx = -2; dx <= 2; dx += 2) {
				for (int dy = -2; dy <= 2; dy += 2) {
					g.drawString(text, x + dx, y + dy);
				}
			}
		}
		schattenText(g, text, x, y, farbe);
	}

	static void gitter(Graphics2D g, int x, int y, int b, int h, int abstand, Color farbe) {
		g.setColor(farbe);
		g.setStroke(new BasicStroke(1f));
		for (int gx = x; gx <= x + b; gx += abstand) {
			g.drawLine(gx, y, gx, y + h);
		}
		for (int gy = y; gy <= y + h; gy += abstand) {
			g.drawLine(x, gy, x + b, gy);
		}
	}

	/** Futuristische Eckwinkel. */
	static void ecken(Graphics2D g, int x, int y, int b, int h, int l, Color farbe) {
		g.setColor(farbe);
		g.setStroke(new BasicStroke(2f));
		g.drawLine(x, y, x + l, y);
		g.drawLine(x, y, x, y + l);
		g.drawLine(x + b, y, x + b - l, y);
		g.drawLine(x + b, y, x + b, y + l);
		g.drawLine(x, y + h, x + l, y + h);
		g.drawLine(x, y + h, x, y + h - l);
		g.drawLine(x + b, y + h, x + b - l, y + h);
		g.drawLine(x + b, y + h, x + b, y + h - l);
	}

	static void zentriert(Graphics2D g, String text, int x, int y, int b, int h) {
		FontMetrics fm = g.getFontMetrics();
		g.drawString(text, x + (b - fm.stringWidth(text)) / 2, y + (h - fm.getHeight()) / 2 + fm.getAscent());
	}

	/**
	 * Das Bambus-Zeichen als Pixel-Art: ein Halm mit drei Segmenten und Knoten, oben ein kräftiges Blatt,
	 * das seitlich herauswächst, unten ein kleines. Raster 12 × 18 Pixel, vergrößert in ganzen Stufen
	 * (groesse = Höhe in Bildschirmpixeln). Farben folgen dem Farbthema; dazu ein leichtes Pixel-Leuchten.
	 */
	static void logo(Graphics2D g, int x, int y, int groesse) {
		int p = Math.max(1, Math.round(groesse / 18f));
		Color hell = mische(NEON, Color.WHITE, 0.6f), dunkel = mische(NEON, Color.BLACK, 0.55f),
				knoten = mische(NEON, Color.BLACK, 0.3f), rippe = mische(CYAN, Color.WHITE, 0.65f),
				schatten = mische(CYAN, Color.BLACK, 0.45f);
		// Leuchten: zwei Ringe um die Form, nach außen schwächer
		boolean[][] form = new boolean[LOGO.length + 4][LOGO[0].length() + 4];
		for (int r = 0; r < LOGO.length; r++) {
			for (int c = 0; c < LOGO[r].length(); c++) {
				form[r + 2][c + 2] = LOGO[r].charAt(c) != '.';
			}
		}
		for (int ring = 2; ring >= 1; ring--) {
			g.setColor(alpha(NEON, ring == 1 ? 30 : 10));
			for (int r = 0; r < form.length; r++) {
				for (int c = 0; c < form[r].length; c++) {
					if (!form[r][c] && nahe(form, r, c, ring) && (ring == 2 ? !nahe(form, r, c, 1) : true)) {
						g.fillRect(x + (c - 2) * p, y + (r - 2) * p, p, p);
					}
				}
			}
		}
		for (int r = 0; r < LOGO.length; r++) {
			for (int c = 0; c < LOGO[r].length(); c++) {
				Color farbe;
				switch (LOGO[r].charAt(c)) {
					case 'H': farbe = hell; break;
					case 'N': farbe = NEON; break;
					case 'D': farbe = dunkel; break;
					case 'K': farbe = knoten; break;
					case 'L': farbe = CYAN; break;
					case 'M': farbe = rippe; break;
					case 'l': farbe = schatten; break;
					default: continue;
				}
				g.setColor(farbe);
				g.fillRect(x + c * p, y + r * p, p, p);
			}
		}
	}

	/** H hell, N Neon, D dunkle Seite, K Knoten · L Blatt, M Blattrippe, l Blattschatten */
	private static final String[] LOGO = {
			".HNND.....LL",
			".HNND...LLLL",
			".HNND..LLLMl",
			".HNND.LLMMl.",
			".HNNDLLMll..",
			"KKKKKKMll...",
			".HNND.l.....",
			".HNND.......",
			".HNND.......",
			".HNND.......",
			".HNND...LLl.",
			"KKKKKKLLMl..",
			".HNND.lll...",
			".HNND.......",
			".HNND.......",
			".HNND.......",
			".HNND.......",
			".DDDD.......",
	};

	/** Breite des Logos in Bildschirmpixeln bei dieser Höhe. */
	static int logoBreite(int groesse) {
		return LOGO[0].length() * Math.max(1, Math.round(groesse / 18f));
	}

	private static boolean nahe(boolean[][] form, int r, int c, int abstand) {
		for (int dr = -abstand; dr <= abstand; dr++) {
			for (int dc = -abstand; dc <= abstand; dc++) {
				int rr = r + dr, cc = c + dc;
				if (rr >= 0 && cc >= 0 && rr < form.length && cc < form[rr].length && form[rr][cc]) return true;
			}
		}
		return false;
	}

	private static Color mische(Color a, Color b, float t) {
		return new Color(Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
				Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
				Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
	}

	// =================================================================== Bauteile

	/** Basis: Knopf mit Hover-Erkennung und Handcursor. */
	abstract static class Knopf extends JButton {
		boolean ueber;

		Knopf(String text) {
			super(text);
			setContentAreaFilled(false);
			setBorderPainted(false);
			setFocusPainted(false);
			setOpaque(false);
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			addMouseListener(new MouseAdapter() {
				@Override public void mouseEntered(MouseEvent e) { ueber = true; repaint(); }
				@Override public void mouseExited(MouseEvent e) { ueber = false; repaint(); }
			});
		}
	}

	/** Großer, leuchtender Spielen-Knopf. */
	static final class SpielKnopf extends Knopf {
		String wartenText = "BITTE WARTEN";

		SpielKnopf(String text) {
			super(text);
			setFont(titel(24));
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			Graphics2D g = glatt(graphics);
			int b = getWidth();
			int h = getHeight();
			RoundRectangle2D form = new RoundRectangle2D.Float(8, 8, b - 17, h - 17, 14, 14);
			if (isEnabled()) {
				float p = puls();
				leuchten(g, form, ueber ? CYAN : NEON, ueber ? 6 : 3 + (int) (2 * p));
				g.setPaint(new GradientPaint(0, 0, ueber ? CYAN : NEON, b, h, ueber ? NEON : alpha(CYAN, 255)));
				g.fill(form);
				g.setColor(DUNKEL);
				g.setFont(getFont());
				zentriert(g, "▶  " + getText(), 0, 0, b, h);
			} else {
				g.setColor(KARTE);
				g.fill(form);
				leuchten(g, form, alpha(NEON, 120), 1);
				g.setColor(GRAU);
				g.setFont(mono(15));
				zentriert(g, wartenText, 0, 0, b, h);
			}
			g.dispose();
		}
	}

	/** Umrandeter Knopf. */
	static final class UmrissKnopf extends Knopf {
		private Color fest;
		private int rolle;   // 0 = feste Farbe, 1 = NEON, 2 = CYAN, 3 = PINK (folgt dem Farbthema)

		void setzeFarbe(Color f) {
			rolle = f == NEON ? 1 : f == CYAN ? 2 : f == PINK ? 3 : 0;
			this.fest = f;
			repaint();
		}

		UmrissKnopf(String text, Color farbe) {
			super(text);
			setzeFarbe(farbe);
			setFont(mono(12));
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			Graphics2D g = glatt(graphics);
			Color farbe = rolle == 1 ? NEON : rolle == 2 ? CYAN : rolle == 3 ? PINK : fest;
			RoundRectangle2D form = new RoundRectangle2D.Float(2, 2, getWidth() - 5, getHeight() - 5, 10, 10);
			g.setColor(ueber ? alpha(farbe, 40) : KARTE);
			g.fill(form);
			if (ueber) {
				leuchten(g, form, farbe, 2);
			} else {
				g.setColor(alpha(farbe, 150));
				g.setStroke(new BasicStroke(1.2f));
				g.draw(form);
			}
			g.setColor(ueber ? TEXT : farbe);
			g.setFont(getFont());
			zentriert(g, getText(), 0, 0, getWidth(), getHeight());
			g.dispose();
		}
	}

	/** Karte mit Titel, Beschreibung und Ein-/Aus-Schalter. */
	static final class SchalterKarte extends JComponent {
		interface Aenderung {
			void neu(boolean an);
		}

		private final String titel;
		private final String text;
		private final String warnung;
		private final Aenderung aenderung;
		private boolean an;
		private boolean ueber;
		private float animation;
		private String symbol;

		/** Symbol aus dem gemeinsamen Satz (wie im Spiel). */
		SchalterKarte mitSymbol(String name) {
			this.symbol = name;
			return this;
		}

		SchalterKarte(String titel, String text, String warnung, boolean an, Aenderung aenderung) {
			this.titel = titel;
			this.text = text;
			this.warnung = warnung;
			this.an = an;
			this.animation = an ? 1f : 0f;
			this.aenderung = aenderung;
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			setToolTipText(warnung != null && !warnung.isEmpty() ? "⚠ " + warnung : null);
			addMouseListener(new MouseAdapter() {
				@Override public void mouseEntered(MouseEvent e) { ueber = true; repaint(); }
				@Override public void mouseExited(MouseEvent e) { ueber = false; repaint(); }
				@Override public void mouseClicked(MouseEvent e) {
					SchalterKarte.this.an = !SchalterKarte.this.an;
					aenderung.neu(SchalterKarte.this.an);
					repaint();
				}
			});
		}

		void setAn(boolean an) {
			this.an = an;
			repaint();
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			Graphics2D g = glatt(graphics);
			int b = getWidth();
			int h = getHeight();
			animation += ((an ? 1f : 0f) - animation) * 0.35f;
			if (Math.abs((an ? 1f : 0f) - animation) > 0.01f) {
				repaint(16);
			}

			boolean warn = warnung != null && !warnung.isEmpty();
			Color akzent = warn ? PINK : NEON;
			RoundRectangle2D form = new RoundRectangle2D.Float(3, 3, b - 7, h - 7, 12, 12);
			g.setColor(ueber ? KARTE_HOVER : KARTE);
			g.fill(form);
			if (an) {
				leuchten(g, form, alpha(akzent, ueber ? 255 : 170), ueber ? 3 : 2);
				g.setColor(akzent);
				g.fillRoundRect(3, 14, 3, h - 30, 3, 3);
			} else {
				g.setColor(ueber ? new Color(0x2B3D57) : LINIE);
				g.setStroke(new BasicStroke(1.2f));
				g.draw(form);
			}

			int tx = 18;
			if (symbol != null) {
				Symbole.zeichne(g, symbol, 18, 17, 2, an ? akzent : new Color(0x4A5A6E));
				tx = 42;
			}
			g.setFont(titel(15));
			if (g.getFontMetrics().stringWidth(titel) > b - 62 - 8 - tx) g.setFont(titel(13));   // Platz für den Schalter lassen
			g.setColor(an ? TEXT : new Color(0xB5C2D4));
			g.drawString(passend(g, titel, b - 62 - 8 - tx), tx, 30);
			g.setFont(normal(12));
			g.setColor(GRAU);
			g.drawString(passend(g, text, b - 30), 18, 50);
			if (warn) {
				g.setFont(mono(10));
				g.setColor(alpha(PINK, 220));
				g.drawString("⚠ " + warnung.toUpperCase(), 18, 68);
			}

			// Schalter
			int sb = 44;
			int sh = 22;
			int sx = b - sb - 18;
			int sy = 16;
			RoundRectangle2D bahn = new RoundRectangle2D.Float(sx, sy, sb, sh, sh, sh);
			g.setColor(an ? alpha(akzent, 70) : new Color(0x1E2A3C));
			g.fill(bahn);
			if (an) {
				leuchten(g, bahn, alpha(akzent, 200), 2);
			}
			int kx = (int) (sx + 3 + animation * (sb - sh));
			g.setColor(an ? akzent : GRAU);
			g.fillRect(kx, sy + 3, sh - 6, sh - 6);
			g.dispose();
		}
	}

	/** Kleiner Schalter ohne Karte (z. B. neben einer Überschrift). */
	static final class MiniSchalter extends JComponent {
		private boolean an;
		private final String beschriftung;

		MiniSchalter(boolean an, SchalterKarte.Aenderung aenderung) {
			this(an, "HUD", aenderung);
		}

		/** beschriftung: Wort vor dem Schalter, z. B. „HUD“ → „HUD AN“ / „HUD AUS“. */
		MiniSchalter(boolean an, String beschriftung, SchalterKarte.Aenderung aenderung) {
			this.an = an;
			this.beschriftung = beschriftung;
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			addMouseListener(new MouseAdapter() {
				@Override public void mouseClicked(MouseEvent e) {
					MiniSchalter.this.an = !MiniSchalter.this.an;
					aenderung.neu(MiniSchalter.this.an);
					repaint();
				}
			});
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			Graphics2D g = glatt(graphics);
			int h = 20;
			int b = 40;
			int x = getWidth() - b - 4;
			int y = (getHeight() - h) / 2;
			g.setFont(mono(10));
			g.setColor(an ? NEON : GRAU);
			String t = beschriftung + (an ? " AN" : " AUS");
			g.drawString(t, x - g.getFontMetrics().stringWidth(t) - 10, y + 14);
			RoundRectangle2D bahn = new RoundRectangle2D.Float(x, y, b, h, h, h);
			g.setColor(an ? alpha(NEON, 70) : new Color(0x1E2A3C));
			g.fill(bahn);
			if (an) {
				leuchten(g, bahn, alpha(NEON, 200), 2);
			}
			g.setColor(an ? NEON : GRAU);
			g.fillRect(an ? x + b - h + 3 : x + 3, y + 3, h - 6, h - 6);
			g.dispose();
		}
	}

	/** Sanft einblendendes Panel (für Seitenwechsel). */
	static void blende(Graphics2D g, float deckkraft) {
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, Math.min(1f, deckkraft))));
	}
}
