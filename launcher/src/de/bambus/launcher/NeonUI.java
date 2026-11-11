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
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

/** Farben, Schriften und Bauteile im Neon-Look. */
final class NeonUI {

	private NeonUI() {}

	static final Color HG = new Color(0x05070D);
	static final Color SEITENLEISTE = new Color(0x080C14);
	static final Color KARTE = new Color(0x0D1420);
	static final Color KARTE_HOVER = new Color(0x132036);
	static final Color LINIE = new Color(0x1A2638);
	static final Color NEON = new Color(0x3DFF8A);
	static final Color CYAN = new Color(0x00E5FF);
	static final Color PINK = new Color(0xFF2BD6);
	static final Color TEXT = new Color(0xE6F1FF);
	static final Color GRAU = new Color(0x7D8DA6);
	static final Color DUNKEL = new Color(0x04110A);

	// ------------------------------------------------------------ Pixelschrift
	// Eigene Pixelschrift im Stil der Spielschrift (werkzeuge/pixelschrift.py). Die Größen werden
	// auf ganze Pixel-Stufen gerundet (8 = 1 Bildschirmpixel pro Schriftpixel, 16 = 2 …),
	// damit jeder Buchstabe scharf und blockig bleibt.
	private static final Font PIXEL = ladeSchrift("/schrift/bambus-pixel.ttf");
	private static final Font PIXEL_FETT = ladeSchrift("/schrift/bambus-pixel-fett.ttf");

	private static Font ladeSchrift(String pfad) {
		try (java.io.InputStream in = NeonUI.class.getResourceAsStream(pfad)) {
			return in == null ? null : Font.createFont(Font.TRUETYPE_FONT, in);
		} catch (Exception e) {
			return null;
		}
	}

	/** Pixel-Stufe (1, 2, 3 …) für eine gewünschte Schriftgröße – ungefähr gleich breiter Text. */
	static int stufe(int groesse) {
		return Math.max(1, Math.round(groesse / 9.5f));
	}

	private static Font pixel(Font basis, int groesse, int ersatzStil, String ersatz) {
		if (basis == null) return new Font(ersatz, ersatzStil, groesse);
		return basis.deriveFont((float) (stufe(groesse) * 8));
	}

	static Font titel(int groesse) {
		return pixel(PIXEL_FETT, groesse, Font.BOLD, Font.SANS_SERIF);
	}

	static Font normal(int groesse) {
		return pixel(PIXEL, groesse, Font.PLAIN, Font.SANS_SERIF);
	}

	static Font mono(int groesse) {
		return pixel(PIXEL_FETT, groesse, Font.BOLD, Font.MONOSPACED);
	}

	/** Größe eines Schriftpixels der Schrift auf dem Bildschirm. */
	static int schriftPixel(Font f) {
		return Math.max(1, f.getSize() / 8);
	}

	static Color alpha(Color c, int a) {
		return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, a)));
	}

	static Color farbe(String hex) {
		return new Color(Integer.parseInt(hex, 16));
	}

	/** Dunkler Schatten wie bei der Spielschrift (ein Viertel der Helligkeit). */
	static Color schatten(Color c) {
		return new Color(c.getRed() / 4, c.getGreen() / 4, c.getBlue() / 4, c.getAlpha());
	}

	/** Langsames Pulsieren zwischen 0 und 1. */
	static float puls() {
		return (float) (0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 500.0));
	}

	/**
	 * Zeichenfläche im Pixel-Look: Formen ohne Kantenglättung (harte Pixelkanten), Treppen-Ecken statt
	 * Rundungen, Bilder ohne Weichzeichnen – und Texte in der gewählten Sprache.
	 */
	static Graphics2D glatt(Graphics graphics) {
		Graphics2D g = vorbereiten(graphics);
		return new PixelGrafik(Sprache.englisch() ? new UebersetzteGrafik(g) : g);
	}

	/** Wie {@link #glatt}, aber ohne Übersetzung. */
	static Graphics2D pixel(Graphics graphics) {
		return new PixelGrafik(vorbereiten(graphics));
	}

	private static Graphics2D vorbereiten(Graphics graphics) {
		Graphics2D g = (Graphics2D) graphics.create();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
		g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
		g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
		return g;
	}

	/** Pixel-Rahmen (innen hohl) mit Treppen-Ecken, um „aussen“ Pixel nach außen versetzt. */
	static void ring(Graphics2D g, double x, double y, double b, double h, double rundung, int aussen, int dicke) {
		PixelGrafik.rahmen(g, x - aussen, y - aussen, b + aussen * 2, h + aussen * 2, rundung + aussen, dicke);
	}

	/**
	 * Neon-Leuchten um eine Form – wie im Spiel als Stufen aus Pixel-Rahmen, die nach außen
	 * durchsichtiger werden.
	 */
	static void leuchten(Graphics2D g, Shape form, Color farbe, int staerke) {
		Rectangle2D r = form.getBounds2D();
		double rund = 0;
		if (form instanceof RoundRectangle2D) {
			RoundRectangle2D rr = (RoundRectangle2D) form;
			rund = Math.min(rr.getArcWidth(), rr.getArcHeight()) / 2;
		} else if (form instanceof Ellipse2D) {
			rund = Math.min(r.getWidth(), r.getHeight()) / 2;
		}
		int p = PixelGrafik.P;
		int stufen = Math.max(1, Math.min(4, staerke));
		for (int i = stufen; i >= 1; i--) {
			g.setColor(alpha(farbe, (int) (farbe.getAlpha() * 0.42f / i)));
			ring(g, r.getX(), r.getY(), r.getWidth(), r.getHeight(), rund, i * p, p);
		}
		g.setColor(farbe);
		ring(g, r.getX(), r.getY(), r.getWidth(), r.getHeight(), rund, 0, p);
	}

	/** Leuchtender Text: Pixelschatten wie im Spiel und ein leichter Schein drumherum. */
	static void leuchtText(Graphics2D g, String text, int x, int y, Color farbe, int staerke) {
		int s = schriftPixel(g.getFont());
		if (staerke > 0) {
			g.setColor(alpha(farbe, Math.min(60, 14 * staerke) / (s > 3 ? 2 : 1)));
			g.drawString(text, x - s, y);
			g.drawString(text, x + s, y);
			g.drawString(text, x, y - s);
			g.drawString(text, x, y + s);
		}
		g.setColor(schatten(farbe));
		g.drawString(text, x + s, y + s);
		g.setColor(farbe);
		g.drawString(text, x, y);
	}

	/** Text mit dem dunklen Pixelschatten der Spielschrift. */
	static void schattenText(Graphics2D g, String text, int x, int y, Color farbe) {
		int s = schriftPixel(g.getFont());
		g.setColor(schatten(farbe));
		g.drawString(text, x + s, y + s);
		g.setColor(farbe);
		g.drawString(text, x, y);
	}

	static void gitter(Graphics2D g, int x, int y, int b, int h, int abstand, Color farbe) {
		g.setColor(farbe);
		for (int gx = x; gx <= x + b; gx += abstand) {
			g.fillRect(gx, y, 1, h);
		}
		for (int gy = y; gy <= y + h; gy += abstand) {
			g.fillRect(x, gy, b, 1);
		}
	}

	/** Futuristische Eckwinkel (2 Pixel dick wie im Spiel). */
	static void ecken(Graphics2D g, int x, int y, int b, int h, int l, Color farbe) {
		int p = PixelGrafik.P;
		g.setColor(farbe);
		g.fillRect(x, y, l, p);
		g.fillRect(x, y, p, l);
		g.fillRect(x + b - l, y, l, p);
		g.fillRect(x + b - p, y, p, l);
		g.fillRect(x, y + h - p, l, p);
		g.fillRect(x, y + h - l, p, l);
		g.fillRect(x + b - l, y + h - p, l, p);
		g.fillRect(x + b - p, y + h - l, p, l);
	}

	/** Zu breiter fetter Text: auf die normale (schmalere) Pixelschrift ausweichen. */
	static void schmalWennNoetig(Graphics2D g, String text, int max) {
		Font f = g.getFont();
		if (PIXEL != null && PIXEL_FETT != null && f.getFontName().equals(PIXEL_FETT.getFontName())
				&& g.getFontMetrics().stringWidth(text) > max) {
			g.setFont(PIXEL.deriveFont(f.getSize2D()));
		}
	}

	static void zentriert(Graphics2D g, String text, int x, int y, int b, int h) {
		schmalWennNoetig(g, text, b - 4);
		FontMetrics fm = g.getFontMetrics();
		g.drawString(text, x + (b - fm.stringWidth(text)) / 2, y + (h - fm.getHeight()) / 2 + fm.getAscent());
	}

	/** Zentrierter Text mit Pixelschatten. */
	static void zentriertSchatten(Graphics2D g, String text, int x, int y, int b, int h, Color farbe) {
		schmalWennNoetig(g, text, b - 4);
		FontMetrics fm = g.getFontMetrics();
		schattenText(g, text, x + (b - fm.stringWidth(text)) / 2, y + (h - fm.getHeight()) / 2 + fm.getAscent(), farbe);
	}

	/** Das Bambus-Zeichen aus Pixel-Blöcken: drei leuchtende Segmente und ein Blatt (wie im Spiel). */
	static void logo(Graphics2D g, int x, int y, int groesse) {
		int p = Math.max(2, Math.round(groesse / 16f));
		int seg = Math.max(p * 3, (groesse / 3) / p * p);
		int b = p * 5;
		for (int i = 0; i < 3; i++) {
			int sy = y + i * seg;
			g.setColor(alpha(NEON, 45));
			g.fillRect(x - p, sy - p, b + p * 2, seg + p);
			g.setColor(NEON);
			g.fillRect(x, sy, b, seg - p);
			g.setColor(new Color(0x1C7A45));
			g.fillRect(x + p * 3, sy, p * 2, seg - p);
			g.setColor(new Color(0xB8FFD4));
			g.fillRect(x, sy, p, seg - p);
		}
		g.setColor(CYAN);
		g.fillRect(x + b + p, y + seg / 2, p * 5, p * 2);
		g.fillRect(x + b + p * 4, y + seg / 2 - p * 2, p * 4, p * 2);
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
				kante(g, form, 8, 8, b - 17, h - 17);
				g.setFont(getFont());
				int s = schriftPixel(getFont());
				g.setColor(alpha(Color.WHITE, 90));
				zentriert(g, "▶  " + getText(), s, s, b, h);
				g.setColor(DUNKEL);
				zentriert(g, "▶  " + getText(), 0, 0, b, h);
			} else {
				g.setColor(KARTE);
				g.fill(form);
				leuchten(g, form, alpha(NEON, 120), 1);
				g.setFont(mono(15));
				zentriertSchatten(g, wartenText, 0, 0, b, h, GRAU);
			}
			g.dispose();
		}
	}

	/** Umrandeter Knopf. */
	static final class UmrissKnopf extends Knopf {
		private Color farbe;

		void setzeFarbe(Color f) {
			this.farbe = f;
			repaint();
		}

		UmrissKnopf(String text, Color farbe) {
			super(text);
			this.farbe = farbe;
			setFont(mono(12));
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			Graphics2D g = glatt(graphics);
			RoundRectangle2D form = new RoundRectangle2D.Float(2, 2, getWidth() - 5, getHeight() - 5, 10, 10);
			g.setColor(ueber ? alpha(farbe, 40) : KARTE);
			g.fill(form);
			kante(g, form, 2, 2, getWidth() - 5, getHeight() - 5);
			if (ueber) {
				leuchten(g, form, farbe, 2);
			} else {
				g.setColor(alpha(farbe, 150));
				g.setStroke(new BasicStroke(1.2f));
				g.draw(form);
			}
			g.setFont(getFont());
			zentriertSchatten(g, getText(), 0, 0, getWidth(), getHeight(), ueber ? TEXT : farbe);
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
			gitter(g, 4, 4, b - 9, h - 9, 12, alpha(CYAN, ueber ? 14 : 8));
			if (an) {
				leuchten(g, form, alpha(akzent, ueber ? 255 : 170), ueber ? 3 : 2);
				g.setColor(akzent);
				g.fillRect(3, 9, PixelGrafik.P, h - 19);
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
			g.setFont(titel(13));
			schattenText(g, titel, tx, 29, an ? TEXT : new Color(0xB5C2D4));
			g.setFont(normal(12));
			schattenText(g, text, 18, 50, GRAU);
			if (warn) {
				g.setFont(mono(10));
				schattenText(g, "⚠ " + warnung.toUpperCase(), 18, 68, alpha(PINK, 220));
			}

			// Schalter
			int sb = 44;
			int sh = 22;
			int sx = b - sb - 18;
			int sy = 16;
			schalter(g, sx, sy, sb, sh, animation, an, akzent);
			g.dispose();
		}
	}

	/** Kleiner Schalter ohne Karte (z. B. neben einer Überschrift). */
	static final class MiniSchalter extends JComponent {
		private boolean an;

		MiniSchalter(boolean an, SchalterKarte.Aenderung aenderung) {
			this.an = an;
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
			String t = an ? "HUD AN" : "HUD AUS";
			schattenText(g, t, x - g.getFontMetrics().stringWidth(t) - 10, y + 14, an ? NEON : GRAU);
			schalter(g, x, y, b, h, an ? 1f : 0f, an, NEON);
			g.dispose();
		}
	}

	/**
	 * Ein-/Aus-Schalter wie im Spiel: Bahn mit Pixel-Ecken und eckiger Knopf, der hinübergleitet
	 * (k = 0 aus … 1 an).
	 */
	static void schalter(Graphics2D g, int x, int y, int b, int h, float k, boolean an, Color akzent) {
		Shape bahn = PixelGrafik.treppe(x, y, b, h, 2);
		g.setColor(an ? alpha(akzent, 70) : new Color(0x1E2A3C));
		g.fill(bahn);
		if (an) {
			leuchten(g, new RoundRectangle2D.Float(x, y, b, h, 4, 4), alpha(akzent, 200), 1);
		} else {
			g.setColor(new Color(0x2A3850));
			ring(g, x, y, b, h, 2, 0, PixelGrafik.P);
		}
		int kg = h - 8;
		int kx = Math.round(x + 4 + k * (b - kg - 8));
		g.setColor(schatten(an ? akzent : GRAU));
		g.fillRect(kx, y + 4, kg, kg);
		g.setColor(an ? akzent : GRAU);
		g.fillRect(kx, y + 4, kg - 2, kg - 2);
		g.setColor(alpha(Color.WHITE, an ? 120 : 60));
		g.fillRect(kx, y + 4, kg - 2, 2);
	}

	/** Pixel-Kante wie bei Minecraft-Knöpfen: oben heller, unten dunkler. */
	static void kante(Graphics2D g, Shape form, int x, int y, int b, int h) {
		if (form instanceof RoundRectangle2D) {
			RoundRectangle2D r = (RoundRectangle2D) form;
			form = PixelGrafik.treppe(r.getX(), r.getY(), r.getWidth(), r.getHeight(), Math.min(r.getArcWidth(), r.getArcHeight()) / 2);
		}
		Shape alt = g.getClip();
		g.clip(form);
		int p = PixelGrafik.P;
		g.setColor(alpha(Color.WHITE, 45));
		g.fillRect(x, y, b + 1, p);
		g.setColor(alpha(Color.BLACK, 70));
		g.fillRect(x, y + h + 1 - p, b + 1, p);
		g.setClip(alt);
	}

	/** Sanft einblendendes Panel (für Seitenwechsel). */
	static void blende(Graphics2D g, float deckkraft) {
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, Math.min(1f, deckkraft))));
	}
}
