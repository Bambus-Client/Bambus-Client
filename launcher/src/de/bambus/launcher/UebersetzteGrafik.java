package de.bambus.launcher;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.text.AttributedCharacterIterator;

/**
 * Zeichenfläche des Launchers:
 * - übersetzt jeden Text beim Zeichnen in die gewählte Sprache (siehe {@link Sprache}); Breiten über
 *   getFontMetrics() nutzen ebenfalls den übersetzten Text, damit Zentrierungen stimmen;
 * - macht abgerundete Kästen eckig – für den Pixel-Look wie im Spiel.
 * Alles andere wird nur weitergereicht.
 */
final class UebersetzteGrafik extends Graphics2D {

	private final Graphics2D d;
	private final boolean uebersetzen;

	UebersetzteGrafik(Graphics2D d) {
		this(d, true);
	}

	UebersetzteGrafik(Graphics2D d, boolean uebersetzen) {
		this.d = d;
		this.uebersetzen = uebersetzen;
	}

	Graphics innen() { return d; }

	private String t(String s) { return uebersetzen && Sprache.englisch() ? Sprache.t(s) : s; }

	/** Abgerundete Rechtecke werden eckig (Pixel-Look). */
	private static java.awt.Shape eckig(java.awt.Shape s) {
		if (s instanceof java.awt.geom.RoundRectangle2D) {
			java.awt.geom.RoundRectangle2D r = (java.awt.geom.RoundRectangle2D) s;
			return new Rectangle2D.Double(Math.round(r.getX()), Math.round(r.getY()), Math.round(r.getWidth()), Math.round(r.getHeight()));
		}
		return s;
	}

	@Override public void drawString(String s, int x, int y) { zeichne(t(s), x, y); }
	@Override public void drawString(String s, float x, float y) { zeichne(t(s), x, y); }

	/** Zeichen, die die Pixelschrift nicht kennt (z. B. ✓ ⚠ ➤), kommen aus einer normalen Schrift. */
	private void zeichne(String s, float x, float y) {
		Font f = d.getFont();
		if (s == null || f == null || f.canDisplayUpTo(s) < 0) {
			d.drawString(s, x, y);
			return;
		}
		Font ersatz = ersatz(f);
		int i = 0;
		while (i < s.length()) {
			int cp = s.codePointAt(i);
			boolean kann = f.canDisplay(cp);
			int j = i;
			while (j < s.length() && f.canDisplay(s.codePointAt(j)) == kann) j += Character.charCount(s.codePointAt(j));
			String teil = s.substring(i, j);
			d.setFont(kann ? f : ersatz);
			d.drawString(teil, x, y);
			x += d.getFontMetrics().stringWidth(teil);
			i = j;
		}
		d.setFont(f);
	}

	static Font ersatz(Font f) {
		return new Font(Font.DIALOG, Font.BOLD, Math.round(f.getSize2D() * 1.1f));
	}

	/** Breite mit Ersatzschrift für unbekannte Zeichen. */
	static int breite(FontMetrics m, String s, java.util.function.Function<Font, FontMetrics> metrik) {
		Font f = m.getFont();
		if (s == null || f.canDisplayUpTo(s) < 0) return m.stringWidth(s);
		FontMetrics em = metrik.apply(ersatz(f));
		int w = 0;
		for (int i = 0; i < s.length(); ) {
			int cp = s.codePointAt(i);
			w += f.canDisplay(cp) ? m.charWidth(cp) : em.charWidth(cp);
			i += Character.charCount(cp);
		}
		return w;
	}
	@Override public void drawString(AttributedCharacterIterator it, int x, int y) { d.drawString(it, x, y); }
	@Override public void drawString(AttributedCharacterIterator it, float x, float y) { d.drawString(it, x, y); }
	@Override public Graphics create() { return new UebersetzteGrafik((Graphics2D) d.create(), uebersetzen); }
	@Override public FontMetrics getFontMetrics() { return new Metrik(d.getFontMetrics(), uebersetzen, d); }
	@Override public FontMetrics getFontMetrics(Font f) { return new Metrik(d.getFontMetrics(f), uebersetzen, d); }

	@Override public void scale(double a0, double a1) { d.scale(a0, a1); }
	@Override public void fill(java.awt.Shape a0) { d.fill(eckig(a0)); }
	@Override public void transform(java.awt.geom.AffineTransform a0) { d.transform(a0); }
	@Override public void rotate(double a0) { d.rotate(a0); }
	@Override public void rotate(double a0, double a1, double a2) { d.rotate(a0, a1, a2); }
	@Override public boolean hit(java.awt.Rectangle a0, java.awt.Shape a1, boolean a2) { return d.hit(a0, a1, a2); }
	@Override public java.awt.Paint getPaint() { return d.getPaint(); }
	@Override public void setPaint(java.awt.Paint a0) { d.setPaint(a0); }
	@Override public void draw(java.awt.Shape a0) { d.draw(eckig(a0)); }
	@Override public boolean drawImage(java.awt.Image a0, java.awt.geom.AffineTransform a1, java.awt.image.ImageObserver a2) { return d.drawImage(a0, a1, a2); }
	@Override public void drawImage(java.awt.image.BufferedImage a0, java.awt.image.BufferedImageOp a1, int a2, int a3) { d.drawImage(a0, a1, a2, a3); }
	@Override public void drawRenderedImage(java.awt.image.RenderedImage a0, java.awt.geom.AffineTransform a1) { d.drawRenderedImage(a0, a1); }
	@Override public void drawRenderableImage(java.awt.image.renderable.RenderableImage a0, java.awt.geom.AffineTransform a1) { d.drawRenderableImage(a0, a1); }
	@Override public void drawGlyphVector(java.awt.font.GlyphVector a0, float a1, float a2) { d.drawGlyphVector(a0, a1, a2); }
	@Override public java.awt.GraphicsConfiguration getDeviceConfiguration() { return d.getDeviceConfiguration(); }
	@Override public void setComposite(java.awt.Composite a0) { d.setComposite(a0); }
	@Override public void setStroke(java.awt.Stroke a0) { d.setStroke(a0); }
	@Override public void setRenderingHint(java.awt.RenderingHints.Key a0, java.lang.Object a1) { d.setRenderingHint(a0, a1); }
	@Override public java.lang.Object getRenderingHint(java.awt.RenderingHints.Key a0) { return d.getRenderingHint(a0); }
	@Override public void setRenderingHints(java.util.Map a0) { d.setRenderingHints(a0); }
	@Override public void addRenderingHints(java.util.Map a0) { d.addRenderingHints(a0); }
	@Override public java.awt.RenderingHints getRenderingHints() { return d.getRenderingHints(); }
	@Override public void translate(int a0, int a1) { d.translate(a0, a1); }
	@Override public void translate(double a0, double a1) { d.translate(a0, a1); }
	@Override public void shear(double a0, double a1) { d.shear(a0, a1); }
	@Override public void setTransform(java.awt.geom.AffineTransform a0) { d.setTransform(a0); }
	@Override public java.awt.geom.AffineTransform getTransform() { return d.getTransform(); }
	@Override public java.awt.Composite getComposite() { return d.getComposite(); }
	@Override public void setBackground(java.awt.Color a0) { d.setBackground(a0); }
	@Override public java.awt.Color getBackground() { return d.getBackground(); }
	@Override public java.awt.Stroke getStroke() { return d.getStroke(); }
	@Override public void clip(java.awt.Shape a0) { d.clip(a0); }
	@Override public java.awt.font.FontRenderContext getFontRenderContext() { return d.getFontRenderContext(); }
	@Override public void setColor(java.awt.Color a0) { d.setColor(a0); }
	@Override public void dispose() { d.dispose(); }
	@Override public java.awt.Color getColor() { return d.getColor(); }
	@Override public void fillRect(int a0, int a1, int a2, int a3) { d.fillRect(a0, a1, a2, a3); }
	@Override public boolean drawImage(java.awt.Image a0, int a1, int a2, java.awt.image.ImageObserver a3) { return d.drawImage(a0, a1, a2, a3); }
	@Override public boolean drawImage(java.awt.Image a0, int a1, int a2, int a3, int a4, java.awt.Color a5, java.awt.image.ImageObserver a6) { return d.drawImage(a0, a1, a2, a3, a4, a5, a6); }
	@Override public boolean drawImage(java.awt.Image a0, int a1, int a2, int a3, int a4, java.awt.image.ImageObserver a5) { return d.drawImage(a0, a1, a2, a3, a4, a5); }
	@Override public boolean drawImage(java.awt.Image a0, int a1, int a2, java.awt.Color a3, java.awt.image.ImageObserver a4) { return d.drawImage(a0, a1, a2, a3, a4); }
	@Override public boolean drawImage(java.awt.Image a0, int a1, int a2, int a3, int a4, int a5, int a6, int a7, int a8, java.awt.Color a9, java.awt.image.ImageObserver a10) { return d.drawImage(a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10); }
	@Override public boolean drawImage(java.awt.Image a0, int a1, int a2, int a3, int a4, int a5, int a6, int a7, int a8, java.awt.image.ImageObserver a9) { return d.drawImage(a0, a1, a2, a3, a4, a5, a6, a7, a8, a9); }
	@Override public void clipRect(int a0, int a1, int a2, int a3) { d.clipRect(a0, a1, a2, a3); }
	@Override public java.awt.Font getFont() { return d.getFont(); }
	@Override public void drawLine(int a0, int a1, int a2, int a3) { d.drawLine(a0, a1, a2, a3); }
	@Override public void setPaintMode() { d.setPaintMode(); }
	@Override public void setXORMode(java.awt.Color a0) { d.setXORMode(a0); }
	@Override public void setFont(java.awt.Font a0) { d.setFont(a0); }
	@Override public void setClip(int a0, int a1, int a2, int a3) { d.setClip(a0, a1, a2, a3); }
	@Override public void setClip(java.awt.Shape a0) { d.setClip(a0); }
	@Override public java.awt.Shape getClip() { return d.getClip(); }
	@Override public void copyArea(int a0, int a1, int a2, int a3, int a4, int a5) { d.copyArea(a0, a1, a2, a3, a4, a5); }
	@Override public void clearRect(int a0, int a1, int a2, int a3) { d.clearRect(a0, a1, a2, a3); }
	@Override public void drawRoundRect(int a0, int a1, int a2, int a3, int a4, int a5) { d.drawRect(a0, a1, a2, a3); }
	@Override public void fillRoundRect(int a0, int a1, int a2, int a3, int a4, int a5) { d.fillRect(a0, a1, a2, a3); }
	@Override public void drawOval(int a0, int a1, int a2, int a3) { d.drawOval(a0, a1, a2, a3); }
	@Override public void fillOval(int a0, int a1, int a2, int a3) { d.fillOval(a0, a1, a2, a3); }
	@Override public void drawArc(int a0, int a1, int a2, int a3, int a4, int a5) { d.drawArc(a0, a1, a2, a3, a4, a5); }
	@Override public void fillArc(int a0, int a1, int a2, int a3, int a4, int a5) { d.fillArc(a0, a1, a2, a3, a4, a5); }
	@Override public void drawPolyline(int[] a0, int[] a1, int a2) { d.drawPolyline(a0, a1, a2); }
	@Override public java.awt.Rectangle getClipBounds() { return d.getClipBounds(); }
	@Override public void drawPolygon(int[] a0, int[] a1, int a2) { d.drawPolygon(a0, a1, a2); }
	@Override public void fillPolygon(int[] a0, int[] a1, int a2) { d.fillPolygon(a0, a1, a2); }

	/** Breiten mit dem übersetzten Text. */
	private static final class Metrik extends FontMetrics {
		private final FontMetrics m;
		private final boolean uebersetzen;
		private final Graphics2D d;

		Metrik(FontMetrics m, boolean uebersetzen, Graphics2D d) {
			super(m.getFont());
			this.m = m;
			this.uebersetzen = uebersetzen;
			this.d = d;
		}

		@Override public int stringWidth(String s) { return breite(m, uebersetzen && Sprache.englisch() ? Sprache.t(s) : s, d::getFontMetrics); }
		@Override public Rectangle2D getStringBounds(String s, Graphics g) { return m.getStringBounds(uebersetzen && Sprache.englisch() ? Sprache.t(s) : s, g); }
		@Override public int getAscent() { return m.getAscent(); }
		@Override public int getDescent() { return m.getDescent(); }
		@Override public int getLeading() { return m.getLeading(); }
		@Override public int getHeight() { return m.getHeight(); }
		@Override public int getMaxAscent() { return m.getMaxAscent(); }
		@Override public int getMaxDescent() { return m.getMaxDescent(); }
		@Override public int getMaxAdvance() { return m.getMaxAdvance(); }
		@Override public int charWidth(char c) { return m.charWidth(c); }
		@Override public int charWidth(int c) { return m.charWidth(c); }
		@Override public int charsWidth(char[] c, int o, int l) { return m.charsWidth(c, o, l); }
		@Override public int[] getWidths() { return m.getWidths(); }
	}
}
