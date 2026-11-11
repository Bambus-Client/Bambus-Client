package de.bambus.launcher;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.nio.file.Files;

/** Startseite: großes Logo, Übersicht und SPIELEN. */
final class SeiteStart extends JPanel {

	private final BambusLauncher l;
	private final NeonUI.SpielKnopf spielen = new NeonUI.SpielKnopf("SPIELEN");

	private String status = "";
	private int prozent = 0;
	private boolean beschaeftigt = false;

	private final Rectangle[] infoKarten = {
			new Rectangle(40, 268, 220, 92),
			new Rectangle(276, 268, 220, 92),
			new Rectangle(512, 268, 220, 92),
	};
	private final String[] infoZiel = {"module", "kosmetik", "einstellungen"};
	private int hoverKarte = -1;
	private final Rectangle tipp = new Rectangle(40, 212, 692, 44);
	private final Rectangle tippZu = new Rectangle(40 + 692 - 34, 212 + 8, 28, 28);

	SeiteStart(BambusLauncher l) {
		this.l = l;
		setOpaque(false);
		setLayout(null);

		spielen.setBounds(480, 412, 262, 92);
		spielen.addActionListener(e -> l.spielen());
		add(spielen);

		MouseAdapter maus = new MouseAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				int neu = -1;
				for (int i = 0; i < infoKarten.length; i++) {
					if (infoKarten[i].contains(e.getPoint())) {
						neu = i;
					}
				}
				if (tippSichtbar() && tippZu.contains(e.getPoint())) {
					neu = 99;
				}
				if (neu != hoverKarte) {
					hoverKarte = neu;
					setCursor(Cursor.getPredefinedCursor(neu >= 0 ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
					repaint();
				}
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				if (tippSichtbar() && tippZu.contains(e.getPoint())) {
					l.tippAusblenden();
					repaint();
					return;
				}
				for (int i = 0; i < infoKarten.length; i++) {
					if (infoKarten[i].contains(e.getPoint())) {
						l.zeige(infoZiel[i]);
					}
				}
			}
		};
		addMouseListener(maus);
		addMouseMotionListener(maus);
		aktualisiere();
	}

	void aktualisiere() {
		if (beschaeftigt) {
			return;
		}
		if (!Files.isDirectory(l.installer.mcOrdner)) {
			status = "Minecraft nicht gefunden – installiere zuerst Minecraft.";
		} else if (l.installer.modSchonInstalliert()) {
			status = "System bereit. Klicke auf SPIELEN.";
		} else {
			status = "Erster Start: Alles wird automatisch eingerichtet.";
		}
		repaint();
	}

	void beschaeftigt(boolean ja) {
		beschaeftigt = ja;
		spielen.setEnabled(!ja);
		repaint();
	}

	void fortschritt(String text, int p) {
		status = text;
		prozent = p;
		repaint();
	}

	@Override
	protected void paintComponent(Graphics graphics) {
		Graphics2D g = NeonUI.glatt(graphics);
		int b = getWidth();

		// --- Titel
		g.setFont(NeonUI.mono(12));
		NeonUI.schattenText(g, "MINECRAFT " + Installer.MC_VERSION + "  ·  FABRIC  ·  V" + BambusLauncher.VERSION, 42, 72, NeonUI.CYAN);
		g.setFont(NeonUI.titel(48));
		NeonUI.leuchtText(g, "BAMBUS", 40, 130, NeonUI.NEON, 3);
		int tb = g.getFontMetrics().stringWidth("BAMBUS ");
		NeonUI.leuchtText(g, "CLIENT", 40 + tb, 130, NeonUI.TEXT, 1);
		g.setFont(NeonUI.normal(15));
		NeonUI.schattenText(g, "Dein PvP-Client im Neon-Look.", 42, 168, NeonUI.GRAU);
		NeonUI.schattenText(g, "Module, Kosmetik und HUD –", 42, 190, NeonUI.GRAU);
		NeonUI.schattenText(g, "alles mit einem Klick an- und ausschaltbar.", 42, 212, NeonUI.GRAU);

		emblem(g, b - 120, 150);

		if (tippSichtbar()) {
			taskleistenTipp(g);
		}

		// --- Info-Karten
		ClientEinstellungen c = l.client;
		String cape = c.cape();
		String[][] werte = {
				{"MODULE AKTIV", c.anzahlAktiv() + " / " + ClientEinstellungen.anzahlGesamt(), "alle"},
				{"KOSMETIK", ClientEinstellungen.AUS.equals(cape) ? "Kein Cape" : ClientEinstellungen.name(ClientEinstellungen.CAPES, cape), "kosmetik"},
				{"ARBEITSSPEICHER", l.ramGb + " GB", "speicher"},
		};
		Color[] farben = {NeonUI.NEON, NeonUI.PINK, NeonUI.CYAN};
		for (int i = 0; i < infoKarten.length; i++) {
			Rectangle r = infoKarten[i];
			boolean hover = i == hoverKarte;
			RoundRectangle2D form = new RoundRectangle2D.Float(r.x, r.y, r.width, r.height, 14, 14);
			g.setPaint(new GradientPaint(0, r.y, hover ? NeonUI.KARTE_HOVER : new Color(0x0B1626), 0, r.y + r.height, new Color(0x050A10)));
			g.fill(form);
			NeonUI.gitter(g, r.x + 2, r.y + 2, r.width - 4, r.height - 4, 12, NeonUI.alpha(NeonUI.CYAN, hover ? 20 : 10));
			NeonUI.ecken(g, r.x - 3, r.y - 3, r.width + 6, r.height + 6, 10, NeonUI.alpha(farben[i], hover ? 255 : 110));
			if (hover) {
				NeonUI.leuchten(g, form, farben[i], 3);
			} else {
				g.setColor(NeonUI.LINIE);
				g.setStroke(new BasicStroke(1.2f));
				g.draw(form);
			}
			g.setPaint(new GradientPaint(r.x + 16, 0, farben[i], r.x + r.width - 16, 0, NeonUI.alpha(farben[i], 0)));
			g.fillRect(r.x + 16, r.y, r.width - 32, 2);
			g.setFont(NeonUI.mono(10));
			NeonUI.schattenText(g, werte[i][0], r.x + 18, r.y + 28, NeonUI.GRAU);
			g.setFont(NeonUI.titel(22));
			if (g.getFontMetrics().stringWidth(werte[i][1]) > r.width - 36) g.setFont(NeonUI.titel(13));
			NeonUI.schattenText(g, werte[i][1], r.x + 18, r.y + 62, NeonUI.TEXT);
			Symbole.zeichne(g, werte[i][2], r.x + r.width - 36, r.y + 16, 2, NeonUI.alpha(farben[i], hover ? 255 : 150));
			if (hover) {
				g.setFont(NeonUI.mono(9));
				NeonUI.schattenText(g, "ÖFFNEN →", r.x + r.width - 70, r.y + r.height - 12, farben[i]);
			}
		}

		// --- Status und Fortschritt
		int sy = 420;
		g.setFont(NeonUI.mono(10));
		NeonUI.schattenText(g, "STATUS", 42, sy, NeonUI.GRAU);
		g.setFont(NeonUI.normal(15));
		if (g.getFontMetrics().stringWidth(status) > 420) g.setFont(NeonUI.normal(12));   // lange Meldungen kleiner
		NeonUI.schattenText(g, status, 42, sy + 26, beschaeftigt ? NeonUI.CYAN : NeonUI.TEXT);

		int bx = 42;
		int by = sy + 44;
		int bb = 400;
		// Fortschritt aus Pixel-Blöcken
		g.setColor(new Color(255, 255, 255, 14));
		g.fillRect(bx - 2, by - 2, bb + 4, 12);
		for (int x = 0; x < bb; x += 8) {
			g.setColor(new Color(255, 255, 255, 10));
			g.fillRect(bx + x, by, 6, 8);
		}
		if (prozent > 0) {
			int w = bb * prozent / 100;
			g.setPaint(new GradientPaint(bx, 0, NeonUI.NEON, bx + bb, 0, NeonUI.CYAN));
			for (int x = 0; x + 6 <= w; x += 8) {
				g.fillRect(bx + x, by, 6, 8);
			}
			g.setColor(NeonUI.alpha(NeonUI.NEON, 40));
			g.fillRect(bx - 2, by - 4, w + 2, 16);
			g.setFont(NeonUI.mono(10));
			NeonUI.schattenText(g, prozent + "%", bx + bb + 10, by + 8, NeonUI.NEON);
		}

		g.setFont(NeonUI.normal(12));
		g.setColor(NeonUI.GRAU);
		g.drawString("◆  Anmeldung nur über dein Microsoft-Konto im offiziellen Minecraft Launcher – Bambus sieht nie dein Passwort.", 42, 540);
		g.dispose();
	}

	private boolean tippSichtbar() {
		return BambusLauncher.istWindows() && !l.taskleisteTippGesehen;
	}

	/** Hinweis: So kommt der Launcher in die Windows-Taskleiste. */
	private void taskleistenTipp(Graphics2D g) {
		RoundRectangle2D form = new RoundRectangle2D.Float(tipp.x, tipp.y, tipp.width, tipp.height, 12, 12);
		g.setColor(NeonUI.KARTE);
		g.fill(form);
		g.setColor(NeonUI.alpha(NeonUI.CYAN, 22));
		g.fill(form);
		NeonUI.leuchten(g, form, NeonUI.alpha(NeonUI.CYAN, 200), 2);

		// kleine Taskleiste mit angeheftetem Bambus-Symbol
		int tx = tipp.x + 14;
		int ty = tipp.y + 12;
		g.setColor(new Color(0x1A2433));
		g.fillRoundRect(tx, ty, 62, 20, 6, 6);
		g.setColor(NeonUI.GRAU);
		g.fillRoundRect(tx + 6, ty + 5, 10, 10, 3, 3);
		g.fillRoundRect(tx + 20, ty + 5, 10, 10, 3, 3);
		g.setColor(NeonUI.NEON);
		g.fillRoundRect(tx + 36, ty + 4, 12, 12, 3, 3);
		g.fillRect(tx + 38, ty + 17, 8, 2);
		g.setColor(NeonUI.PINK);
		g.fillOval(tx + 46, ty - 3, 7, 7);

		g.setFont(NeonUI.mono(11));
		g.setColor(NeonUI.CYAN);
		g.drawString("TIPP", tipp.x + 90, tipp.y + 27);
		g.setFont(NeonUI.normal(13));
		g.setColor(NeonUI.TEXT);
		g.drawString("Rechtsklick aufs Bambus-Symbol in der Taskleiste → „An Taskleiste anheften“",
				tipp.x + 128, tipp.y + 27);

		boolean hover = hoverKarte == 99;
		g.setColor(hover ? NeonUI.TEXT : NeonUI.GRAU);
		g.setFont(NeonUI.titel(13));
		NeonUI.zentriert(g, "✕", tippZu.x, tippZu.y, tippZu.width, tippZu.height);
	}

	/** Rotierende Neon-Ringe um das Bambus-Zeichen – aus Pixel-Blöcken wie im Spiel. */
	private static void emblem(Graphics2D g, int cx, int cy) {
		double t = System.currentTimeMillis() / 1000.0;
		float p = NeonUI.puls();

		pixelScheibe(g, cx, cy, 70, 4, NeonUI.alpha(NeonUI.NEON, (int) (14 + 16 * p)));

		Object[][] ringe = {
				{92, NeonUI.NEON, 40.0, 110.0, 4},
				{78, NeonUI.CYAN, -65.0, 80.0, 4},
				{104, NeonUI.PINK, 25.0, 50.0, 2},
		};
		for (Object[] r : ringe) {
			int rad = (Integer) r[0];
			Color farbe = (Color) r[1];
			double winkel = ((Double) r[2]) * t;
			double laenge = (Double) r[3];
			int block = (Integer) r[4];
			for (int k = 0; k < 2; k++) {
				double von = Math.toRadians(winkel + k * 180);
				double bis = von + Math.toRadians(laenge);
				pixelBogen(g, cx, cy, rad, von, bis, block + 4, NeonUI.alpha(farbe, 40));
				pixelBogen(g, cx, cy, rad, von, bis, block, farbe);
			}
		}
		// Punkte auf der äußeren Bahn
		for (int i = 0; i < 6; i++) {
			double w = t * 0.8 + i * Math.PI / 3;
			int x = (int) (cx + Math.cos(w) * 118) / 2 * 2;
			int y = (int) (cy + Math.sin(w) * 118) / 2 * 2;
			g.setColor(NeonUI.alpha(NeonUI.CYAN, 50));
			g.fillRect(x - 4, y - 4, 8, 8);
			g.setColor(NeonUI.alpha(NeonUI.CYAN, 160));
			g.fillRect(x - 2, y - 2, 4, 4);
		}
		NeonUI.logo(g, cx - 16, cy - 30, 60);
	}

	/** Bogen aus quadratischen Blöcken auf einem festen Raster (wie gezeichnete Pixel-Kunst). */
	private static void pixelBogen(Graphics2D g, int cx, int cy, int rad, double von, double bis, int block, Color farbe) {
		g.setColor(farbe);
		java.util.Set<Long> fertig = new java.util.HashSet<>();
		double schritt = 1.0 / rad;
		for (double w = von; w <= bis; w += schritt) {
			int x = Math.floorDiv((int) Math.round(cx + Math.cos(w) * rad - block / 2.0), 2) * 2;
			int y = Math.floorDiv((int) Math.round(cy - Math.sin(w) * rad - block / 2.0), 2) * 2;
			if (fertig.add(((long) x << 32) ^ (y & 0xffffffffL))) {
				g.fillRect(x, y, block, block);
			}
		}
	}

	/** Gefüllter Kreis aus Pixel-Zeilen. */
	private static void pixelScheibe(Graphics2D g, int cx, int cy, int rad, int p, Color farbe) {
		g.setColor(farbe);
		for (int y = -rad; y < rad; y += p) {
			double my = y + p / 2.0;
			int halb = (int) Math.round(Math.sqrt(Math.max(0, rad * rad - my * my)) / p) * p;
			if (halb > 0) g.fillRect(cx - halb, cy + y, halb * 2, p);
		}
	}
}
