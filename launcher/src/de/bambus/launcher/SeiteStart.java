package de.bambus.launcher;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.BorderFactory;
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
	/** ▾ neben SPIELEN: Profil zum Spielen auswählen. */
	private final ProfilPfeil pfeil = new ProfilPfeil();
	private JPopupMenu auswahl;

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

		spielen.setBounds(480, 412, 200, 92);
		spielen.addActionListener(e -> l.spielen());
		add(spielen);
		pfeil.setBounds(678, 420, 58, 76);
		add(pfeil);

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
		} else if (!l.installer.bambusPasst()) {
			status = "Profil für Minecraft " + l.installer.version() + " – startet ohne Bambus-Mod.";
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
		pfeil.setEnabled(!ja);
		repaint();
	}

	// ============================================================ Profil-Auswahl

	private boolean auswahlOffen() {
		return auswahl != null && auswahl.isVisible();
	}

	/** Liste aller Profile über dem SPIELEN-Knopf; ein Klick macht das Profil zum aktiven. */
	private void zeigeAuswahl() {
		if (auswahlOffen()) {
			auswahl.setVisible(false);
			return;
		}
		auswahl = new JPopupMenu();
		auswahl.setBackground(NeonUI.KARTE);
		auswahl.setBorder(BorderFactory.createLineBorder(NeonUI.NEON, 1));
		auswahl.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
			@Override public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) { repaint(); }
			@Override public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) { spaeterNeuZeichnen(); }
			@Override public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) { spaeterNeuZeichnen(); }
		});
		int breite = 256;
		for (ModVerwaltung.Profil p : l.mods.profileSortiert()) {
			auswahl.add(new AuswahlZeile(p, breite));
		}
		auswahl.add(new AuswahlZeile(null, breite));
		java.awt.Dimension d = auswahl.getPreferredSize();
		auswahl.show(this, 736 - d.width, 412 - d.height);
	}

	private void spaeterNeuZeichnen() {
		javax.swing.SwingUtilities.invokeLater(this::repaint);
	}

	/** Eine Zeile der Profil-Auswahl (p == null: „Profile verwalten“). */
	private final class AuswahlZeile extends JComponent {
		private final ModVerwaltung.Profil p;
		private boolean ueber;

		AuswahlZeile(ModVerwaltung.Profil p, int breite) {
			this.p = p;
			setPreferredSize(new java.awt.Dimension(breite, p == null ? 34 : 42));
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			addMouseListener(new MouseAdapter() {
				@Override public void mouseEntered(MouseEvent e) { ueber = true; repaint(); }
				@Override public void mouseExited(MouseEvent e) { ueber = false; repaint(); }
				@Override public void mouseReleased(MouseEvent e) {
					auswahl.setVisible(false);
					if (AuswahlZeile.this.p == null) {
						l.zeige("mods");
					} else {
						l.modsSeite.waehleAktiv(AuswahlZeile.this.p.id);
						aktualisiere();
					}
				}
			});
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			Graphics2D g = NeonUI.glatt(graphics);
			int b = getWidth(), h = getHeight();
			g.setColor(ueber ? NeonUI.KARTE_HOVER : NeonUI.KARTE);
			g.fillRect(0, 0, b, h);
			if (p == null) {
				g.setColor(NeonUI.LINIE);
				g.fillRect(0, 0, b, 1);
				g.setFont(NeonUI.mono(11));
				g.setColor(ueber ? NeonUI.CYAN : NeonUI.GRAU);
				g.drawString(Sprache.t("PROFILE VERWALTEN →"), 14, h / 2 + 4);
				g.dispose();
				return;
			}
			boolean aktiv = p.id.equals(l.mods.aktivesProfil());
			if (aktiv) {
				g.setColor(NeonUI.NEON);
				g.fillRect(0, 6, 3, h - 12);
			}
			g.setFont(NeonUI.titel(14));
			g.setColor(aktiv ? NeonUI.NEON : NeonUI.TEXT);
			g.drawString(NeonUI.passend(g, p.name, b - 110), 14, 19);
			g.setFont(NeonUI.normal(10));
			g.setColor(NeonUI.GRAU);
			g.drawString(Sprache.t(p.mods + (p.mods == 1 ? " Mod" : " Mods") + (p.mitBambus() ? " · mit Bambus" : " · ohne Bambus")), 14, 34);
			Color farbe = p.mitBambus() ? NeonUI.NEON : NeonUI.CYAN;
			g.setFont(NeonUI.mono(10));
			int vb = g.getFontMetrics().stringWidth(p.version) + 12;
			g.setColor(NeonUI.alpha(farbe, 30));
			g.fillRect(b - vb - 34, (h - 18) / 2, vb, 18);
			g.setColor(farbe);
			NeonUI.zentriert(g, p.version, b - vb - 34, (h - 18) / 2, vb, 18);
			if (aktiv) {
				g.setFont(NeonUI.titel(14));
				g.drawString("✓", b - 22, h / 2 + 5);
			}
			g.dispose();
		}
	}

	/** Umrandeter Pfeil-Teil des SPIELEN-Knopfs. */
	private final class ProfilPfeil extends JComponent {
		private boolean ueber;

		ProfilPfeil() {
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			setToolTipText("Profil zum Spielen auswählen");
			addMouseListener(new MouseAdapter() {
				@Override public void mouseEntered(MouseEvent e) { ueber = true; SeiteStart.this.repaint(); }
				@Override public void mouseExited(MouseEvent e) { ueber = false; SeiteStart.this.repaint(); }
				@Override public void mousePressed(MouseEvent e) {
					if (isEnabled()) zeigeAuswahl();
				}
			});
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			Graphics2D g = NeonUI.glatt(graphics);
			int b = getWidth(), h = getHeight();
			boolean offen = auswahlOffen();
			Color farbe = !isEnabled() ? NeonUI.GRAU : ueber || offen ? NeonUI.CYAN : NeonUI.NEON;
			RoundRectangle2D form = new RoundRectangle2D.Float(2, 2, b - 5, h - 5, 14, 14);
			g.setColor(ueber || offen ? NeonUI.alpha(farbe, 45) : NeonUI.alpha(NeonUI.KARTE, 230));
			g.fill(form);
			if (ueber || offen) {
				NeonUI.leuchten(g, form, farbe, 3);
			} else {
				g.setColor(NeonUI.alpha(farbe, 200));
				g.setStroke(new BasicStroke(1.6f));
				g.draw(form);
			}
			// Pfeil als Pixel-Dreieck
			g.setColor(farbe);
			int px = b / 2, py = h / 2 + (offen ? 4 : -4), s = 3;
			for (int r = 0; r < 4; r++) {
				int y = offen ? py - r * s : py + r * s;
				int w = (4 - r) * 2 - 1;
				g.fillRect(px - (w * s) / 2, y, w * s, s);
			}
			g.dispose();
		}
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
		g.setColor(NeonUI.CYAN);
		g.drawString("MINECRAFT " + l.installer.version() + "  ·  FABRIC  ·  V" + BambusLauncher.VERSION, 42, 72);
		g.setFont(NeonUI.titel(56));
		NeonUI.leuchtText(g, "BAMBUS", 38, 136, NeonUI.NEON, 3);
		int tb = g.getFontMetrics().stringWidth("BAMBUS ");
		g.setFont(NeonUI.titel(56));
		NeonUI.leuchtText(g, "CLIENT", 38 + tb, 136, NeonUI.TEXT, 1);
		g.setFont(NeonUI.normal(12));
		g.setColor(NeonUI.GRAU);
		g.drawString("Dein PvP-Client im Neon-Look. Module, Kosmetik und HUD –", 42, 172);
		g.drawString("alles mit einem Klick an- und ausschaltbar.", 42, 194);

		emblem(g, b - 120, 150);

		if (tippSichtbar()) {
			taskleistenTipp(g);
		}

		// --- Info-Karten
		ClientEinstellungen c = l.client;
		String cape = c.cape();
		String[][] werte = {
				{"MODULE AKTIV", c.anzahlAktiv() + " / " + ClientEinstellungen.anzahlGesamt(), "alle"},
				{"GARDEROBE", ClientEinstellungen.AUS.equals(cape) ? "Kein Cape" : ClientEinstellungen.name(ClientEinstellungen.CAPES, cape), "person"},
				{"ARBEITSSPEICHER", l.ramGb + " GB", "speicher"},
		};
		Color[] farben = {NeonUI.NEON, NeonUI.PINK, NeonUI.CYAN};
		for (int i = 0; i < infoKarten.length; i++) {
			Rectangle r = infoKarten[i];
			boolean hover = i == hoverKarte;
			RoundRectangle2D form = new RoundRectangle2D.Float(r.x, r.y, r.width, r.height, 14, 14);
			g.setColor(hover ? NeonUI.KARTE_HOVER : NeonUI.alpha(NeonUI.KARTE, 230));
			g.fill(form);
			if (hover) {
				NeonUI.leuchten(g, form, farben[i], 3);
			} else {
				g.setColor(NeonUI.LINIE);
				g.setStroke(new BasicStroke(1.2f));
				g.draw(form);
			}
			NeonUI.ecken(g, r.x, r.y, r.width, r.height, 8, NeonUI.alpha(farben[i], hover ? 255 : 150));   // Eckwinkel wie die Karten im Spiel
			g.setPaint(new GradientPaint(r.x + 16, 0, farben[i], r.x + r.width - 16, 0, NeonUI.alpha(farben[i], 0)));
			g.fillRect(r.x + 16, r.y, r.width - 32, 2);
			g.setFont(NeonUI.mono(10));
			g.setColor(NeonUI.GRAU);
			g.drawString(werte[i][0], r.x + 18, r.y + 28);
			g.setFont(NeonUI.titel(22));
			g.setColor(NeonUI.TEXT);
			g.drawString(werte[i][1], r.x + 18, r.y + 62);
			Symbole.zeichne(g, werte[i][2], r.x + r.width - 38, r.y + 16, 2, NeonUI.alpha(farben[i], hover ? 255 : 150));
			g.setFont(NeonUI.mono(9));
			g.setColor(NeonUI.alpha(farben[i], hover ? 255 : 0));
			g.drawString("ÖFFNEN →", r.x + r.width - 70, r.y + r.height - 12);
		}

		// --- Status und Fortschritt
		int sy = 420;
		g.setFont(NeonUI.mono(10));
		g.setColor(NeonUI.GRAU);
		g.drawString("STATUS", 42, sy);
		g.setFont(NeonUI.normal(15));
		g.setColor(beschaeftigt ? NeonUI.CYAN : NeonUI.TEXT);
		g.drawString(NeonUI.passend(g, Sprache.t(status), 420), 42, sy + 26);

		int bx = 42;
		int by = sy + 44;
		int bb = 400;
		g.setColor(new Color(255, 255, 255, 18));
		g.fillRoundRect(bx, by, bb, 8, 8, 8);
		if (prozent > 0) {
			int w = bb * prozent / 100;
			g.setPaint(new GradientPaint(bx, 0, NeonUI.NEON, bx + bb, 0, NeonUI.CYAN));
			g.fillRoundRect(bx, by, w, 8, 8, 8);
			g.setColor(NeonUI.alpha(NeonUI.NEON, 60));
			g.fillRoundRect(bx, by - 3, w, 14, 14, 14);
			g.setFont(NeonUI.mono(10));
			g.setColor(NeonUI.NEON);
			g.drawString(prozent + "%", bx + bb + 10, by + 8);
		}

		// Aktives Profil über SPIELEN, Umriss um Knopf und Pfeil
		ModVerwaltung.Profil aktiv = l.mods.profil(l.mods.aktivesProfil());
		g.setFont(NeonUI.mono(10));
		g.setColor(NeonUI.GRAU);
		g.drawString("PROFIL", 490, 404);
		int lb = g.getFontMetrics().stringWidth("PROFIL") + 10;
		g.setFont(NeonUI.titel(13));
		g.setColor(NeonUI.TEXT);
		String pn = NeonUI.passend(g, aktiv.name, 150);
		g.drawString(pn, 490 + lb, 405);
		g.setFont(NeonUI.mono(10));
		g.setColor(aktiv.mitBambus() ? NeonUI.NEON : NeonUI.CYAN);
		g.drawString(aktiv.version, 490 + lb + g.getFontMetrics(NeonUI.titel(13)).stringWidth(pn) + 8, 404);
		boolean hervor = auswahlOffen() || pfeil.getMousePosition() != null;
		RoundRectangle2D umriss = new RoundRectangle2D.Float(484, 416, 256, 84, 18, 18);
		if (hervor && !beschaeftigt) {
			NeonUI.leuchten(g, umriss, NeonUI.alpha(NeonUI.CYAN, 160), 2);
		} else {
			g.setColor(NeonUI.alpha(NeonUI.NEON, beschaeftigt ? 40 : 90));
			g.setStroke(new BasicStroke(1.2f));
			g.draw(umriss);
		}

		g.setFont(NeonUI.normal(10));
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

	/** Rotierende Neon-Ringe um das Bambus-Zeichen. */
	private static void emblem(Graphics2D g, int cx, int cy) {
		double t = System.currentTimeMillis() / 1000.0;
		float p = NeonUI.puls();

		g.setColor(NeonUI.alpha(NeonUI.NEON, (int) (14 + 16 * p)));
		g.fill(new Ellipse2D.Double(cx - 70, cy - 70, 140, 140));

		Object[][] ringe = {
				{92, NeonUI.NEON, 40.0, 110.0, 2.2f},
				{78, NeonUI.CYAN, -65.0, 80.0, 1.6f},
				{104, NeonUI.PINK, 25.0, 50.0, 1.2f},
		};
		for (Object[] r : ringe) {
			int rad = (Integer) r[0];
			Color farbe = (Color) r[1];
			double winkel = ((Double) r[2]) * t;
			double laenge = (Double) r[3];
			float dicke = (Float) r[4];
			for (int k = 0; k < 2; k++) {
				Arc2D bogen = new Arc2D.Double(cx - rad, cy - rad, rad * 2, rad * 2, winkel + k * 180, laenge, Arc2D.OPEN);
				for (int i = 4; i >= 1; i--) {
					g.setColor(NeonUI.alpha(farbe, 50 / i));
					g.setStroke(new BasicStroke(dicke + i * 2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
					g.draw(bogen);
				}
				g.setColor(farbe);
				g.setStroke(new BasicStroke(dicke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
				g.draw(bogen);
			}
		}
		// Punkte auf der äußeren Bahn
		for (int i = 0; i < 6; i++) {
			double w = t * 0.8 + i * Math.PI / 3;
			int x = (int) (cx + Math.cos(w) * 118);
			int y = (int) (cy + Math.sin(w) * 118);
			g.setColor(NeonUI.alpha(NeonUI.CYAN, 120));
			g.fillOval(x - 2, y - 2, 4, 4);
		}
		NeonUI.logo(g, cx - NeonUI.logoBreite(72) / 2, cy - 36, 72);
	}
}
