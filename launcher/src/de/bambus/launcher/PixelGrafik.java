package de.bambus.launcher;

import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.text.AttributedCharacterIterator;

/**
 * Zeichenfläche im Pixel-Look des Clients: abgerundete Rechtecke werden zu Rechtecken mit
 * Treppen-Ecken (wie die Knöpfe und Schalter im Spiel), kleine Kreise werden aus Pixel-Blöcken
 * gebaut. Alles andere wird nur weitergereicht.
 */
final class PixelGrafik extends Graphics2D {

	/** Größe eines „Spiel-Pixels“ auf dem Bildschirm (wie GUI-Größe 2 im Spiel). */
	static final int P = 2;

	private final Graphics2D d;

	PixelGrafik(Graphics2D d) {
		this.d = d;
	}

	// ------------------------------------------------------------ Pixel-Formen

	/** Wie viele Treppenstufen eine Ecke mit dieser Rundung bekommt (0 bis 3). */
	static int stufen(double rundung, int b, int h) {
		int stufen = (int) Math.max(0, Math.min(3, Math.round(rundung / 4.0)));
		if (rundung > 0 && stufen == 0) stufen = 1;
		while (stufen > 0 && stufen * P * 2 > Math.min(b, h)) stufen--;
		return stufen;
	}

	/** Rechteck mit Treppen-Ecken; die Stufenzahl richtet sich nach der Rundung. */
	static Polygon treppe(double fx, double fy, double fb, double fh, double rundung) {
		int x = (int) Math.round(fx);
		int y = (int) Math.round(fy);
		int b = (int) Math.round(fb);
		int h = (int) Math.round(fh);
		int stufen = stufen(rundung, b, h);
		Polygon p = new Polygon();
		if (stufen == 0) {
			p.addPoint(x, y);
			p.addPoint(x + b, y);
			p.addPoint(x + b, y + h);
			p.addPoint(x, y + h);
			return p;
		}
		int c = stufen * P;
		// oben links → oben rechts
		p.addPoint(x + c, y);
		p.addPoint(x + b - c, y);
		for (int i = 1; i <= stufen; i++) {          // Ecke oben rechts
			p.addPoint(x + b - c + (i - 1) * P, y + i * P);
			p.addPoint(x + b - c + i * P, y + i * P);
		}
		p.addPoint(x + b, y + h - c);
		for (int i = 1; i <= stufen; i++) {          // Ecke unten rechts
			p.addPoint(x + b - (i - 1) * P, y + h - c + i * P);
			p.addPoint(x + b - i * P, y + h - c + i * P);
		}
		p.addPoint(x + c, y + h);
		for (int i = 1; i <= stufen; i++) {          // Ecke unten links
			p.addPoint(x + c - (i - 1) * P, y + h - i * P);
			p.addPoint(x + c - i * P, y + h - i * P);
		}
		p.addPoint(x, y + c);
		for (int i = 1; i <= stufen; i++) {          // Ecke oben links
			p.addPoint(x + (i - 1) * P, y + c - i * P);
			p.addPoint(x + i * P, y + c - i * P);
		}
		return p;
	}

	/** Kreis/Ellipse aus Pixel-Blöcken (nur für kleine Formen wie Schalterknöpfe und Punkte). */
	private void pixelOval(double fx, double fy, double fb, double fh) {
		int x = (int) Math.round(fx);
		int y = (int) Math.round(fy);
		int b = (int) Math.round(fb);
		int h = (int) Math.round(fh);
		if (b <= P * 2 || h <= P * 2) {
			d.fillRect(x, y, Math.max(1, b), Math.max(1, h));
			return;
		}
		double rx = b / 2.0;
		double ry = h / 2.0;
		for (int zy = 0; zy < h; zy += P) {
			int zh = Math.min(P, h - zy);
			double my = (zy + zh / 2.0) - ry;
			double k = 1 - (my * my) / (ry * ry);
			if (k <= 0) continue;
			int halb = (int) Math.round(rx * Math.sqrt(k) / P) * P;
			if (halb <= 0) continue;
			int mitte = x + b / 2;
			d.fillRect(mitte - halb, y + zy, halb * 2, zh);
		}
	}

	private static boolean kleinesOval(Shape s) {
		Rectangle2D r = s.getBounds2D();
		return r.getWidth() <= 28 && r.getHeight() <= 28;
	}

	@Override
	public void fill(Shape s) {
		if (s instanceof RoundRectangle2D) {
			RoundRectangle2D r = (RoundRectangle2D) s;
			d.fill(treppe(r.getX(), r.getY(), r.getWidth(), r.getHeight(), Math.min(r.getArcWidth(), r.getArcHeight()) / 2));
		} else if (s instanceof Ellipse2D && kleinesOval(s)) {
			Ellipse2D e = (Ellipse2D) s;
			pixelOval(e.getX(), e.getY(), e.getWidth(), e.getHeight());
		} else {
			d.fill(s);
		}
	}

	/**
	 * Pixel-Rahmen entlang von {@link #treppe}: vier Balken und an den Ecken einzelne Blöcke
	 * (wie ein gezeichneter Pixel-Kunst-Rahmen). Nur Rechtecke – schnell genug für jedes Bild.
	 */
	static void rahmen(Graphics2D g, double fx, double fy, double fb, double fh, double rundung, int dicke) {
		int x = (int) Math.round(fx);
		int y = (int) Math.round(fy);
		int b = (int) Math.round(fb);
		int h = (int) Math.round(fh);
		if (b <= 0 || h <= 0) return;
		int n = stufen(rundung, b, h);
		int c = n * P;
		g.fillRect(x + c, y, b - 2 * c, dicke);
		g.fillRect(x + c, y + h - dicke, b - 2 * c, dicke);
		g.fillRect(x, y + c, dicke, h - 2 * c);
		g.fillRect(x + b - dicke, y + c, dicke, h - 2 * c);
		for (int i = 1; i < n; i++) {
			int rechts = x + b - c + (i - 1) * P;
			int links = x + c - i * P;
			int oben = y + i * P;
			int unten = y + h - i * P - P;
			g.fillRect(rechts, oben, P, P);
			g.fillRect(links, oben, P, P);
			g.fillRect(rechts, unten, P, P);
			g.fillRect(links, unten, P, P);
		}
	}

	private void rahmen(double x, double y, double b, double h, double rundung) {
		float breite = d.getStroke() instanceof BasicStroke ? ((BasicStroke) d.getStroke()).getLineWidth() : 1f;
		rahmen(d, x, y, b + 1, h + 1, rundung, Math.max(P, Math.round(breite)));
	}

	@Override
	public void draw(Shape s) {
		if (s instanceof RoundRectangle2D) {
			RoundRectangle2D r = (RoundRectangle2D) s;
			rahmen(r.getX(), r.getY(), r.getWidth(), r.getHeight(), Math.min(r.getArcWidth(), r.getArcHeight()) / 2);
		} else {
			d.draw(s);
		}
	}

	@Override public void fillRoundRect(int x, int y, int b, int h, int ab, int ah) { d.fill(treppe(x, y, b, h, Math.min(ab, ah) / 2.0)); }
	@Override public void drawRoundRect(int x, int y, int b, int h, int ab, int ah) { rahmen(x, y, b, h, Math.min(ab, ah) / 2.0); }
	@Override public void fillOval(int x, int y, int b, int h) {
		if (b <= 28 && h <= 28) pixelOval(x, y, b, h); else d.fillOval(x, y, b, h);
	}
	@Override public void drawOval(int x, int y, int b, int h) { d.drawOval(x, y, b, h); }

	// ------------------------------------------------------------ weitergereicht

	@Override public void drawString(String s, int x, int y) { text(s, x, y); }
	@Override public void drawString(String s, float x, float y) { text(s, Math.round(x), Math.round(y)); }

	/**
	 * Text zeichnen; Zeichen, die die Pixelschrift nicht kennt (z. B. Emojis aus Update-Texten),
	 * kommen aus der Systemschrift statt als leeres Kästchen.
	 */
	private void text(String s, int x, int y) {
		if (s == null) return;
		Graphics2D ziel = d instanceof UebersetzteGrafik ? ((UebersetzteGrafik) d).innen() : d;
		String t = d instanceof UebersetzteGrafik ? Sprache.t(s) : s;
		Font f = ziel.getFont();
		if (f.canDisplayUpTo(t) < 0) {
			ziel.drawString(t, x, y);
			return;
		}
		Font ersatz = new Font(Font.DIALOG, f.isBold() ? Font.BOLD : Font.PLAIN, Math.max(10, Math.round(f.getSize2D() * 1.4f)));
		int i = 0;
		while (i < t.length()) {
			boolean kann = f.canDisplay(t.codePointAt(i));
			int j = i;
			while (j < t.length() && f.canDisplay(t.codePointAt(j)) == kann) j += Character.charCount(t.codePointAt(j));
			String stueck = t.substring(i, j);
			Font schrift = kann ? f : ersatz;
			ziel.setFont(schrift);
			ziel.drawString(stueck, x, y);
			x += ziel.getFontMetrics(schrift).stringWidth(stueck);
			i = j;
		}
		ziel.setFont(f);
	}
	@Override public void drawString(AttributedCharacterIterator it, int x, int y) { d.drawString(it, x, y); }
	@Override public void drawString(AttributedCharacterIterator it, float x, float y) { d.drawString(it, x, y); }
	@Override public Graphics create() { return new PixelGrafik((Graphics2D) d.create()); }
	@Override public FontMetrics getFontMetrics() { return d.getFontMetrics(); }
	@Override public FontMetrics getFontMetrics(Font f) { return d.getFontMetrics(f); }
	@Override public void scale(double a0, double a1) { d.scale(a0, a1); }
	@Override public void transform(java.awt.geom.AffineTransform a0) { d.transform(a0); }
	@Override public void rotate(double a0) { d.rotate(a0); }
	@Override public void rotate(double a0, double a1, double a2) { d.rotate(a0, a1, a2); }
	@Override public boolean hit(java.awt.Rectangle a0, java.awt.Shape a1, boolean a2) { return d.hit(a0, a1, a2); }
	@Override public java.awt.Paint getPaint() { return d.getPaint(); }
	@Override public void setPaint(java.awt.Paint a0) { d.setPaint(a0); }
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
	@Override public void drawArc(int a0, int a1, int a2, int a3, int a4, int a5) { d.drawArc(a0, a1, a2, a3, a4, a5); }
	@Override public void fillArc(int a0, int a1, int a2, int a3, int a4, int a5) { d.fillArc(a0, a1, a2, a3, a4, a5); }
	@Override public void drawPolyline(int[] a0, int[] a1, int a2) { d.drawPolyline(a0, a1, a2); }
	@Override public java.awt.Rectangle getClipBounds() { return d.getClipBounds(); }
	@Override public void drawPolygon(int[] a0, int[] a1, int a2) { d.drawPolygon(a0, a1, a2); }
	@Override public void fillPolygon(int[] a0, int[] a1, int a2) { d.fillPolygon(a0, a1, a2); }
}
