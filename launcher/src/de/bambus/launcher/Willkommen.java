package de.bambus.launcher;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

/**
 * Willkommens-Seite beim ersten Start: Sprache, Minecraft-Name (Skin), Look und Leistung.
 * Lässt sich später unter EINSTELLUNGEN → WILLKOMMENS-SEITE erneut öffnen.
 */
final class Willkommen extends JDialog {

	private static final int SCHRITTE = 5;   // 0 Sprache, 1 Name, 2 Look, 3 Leistung, 4 Fertig
	private static final String[][] THEMEN = {
			{"NEON", "3DFF9A"}, {"OZEAN", "2F8CFF"}, {"SAKURA", "FF8CC0"}, {"GOLD", "FFC846"},
			{"LILA", "B06BFF"}, {"EIS", "9FF3FF"}, {"RGB", ""}};
	private static final int[] RAM = {2, 3, 4, 6, 8};

	private final BambusLauncher l;
	private final int breite, hoehe;
	private int schritt = 0;
	private long schrittSeit = System.currentTimeMillis();
	private final List<Feld> felder = new ArrayList<>();   // klickbare Flächen des aktuellen Schritts

	private final JTextField nameFeld = new JTextField() {
		@Override
		protected void paintComponent(Graphics gr) {
			super.paintComponent(gr);
			if (getText().isEmpty()) {   // Platzhalter
				Graphics2D g = NeonUI.glatt(gr);
				g.setFont(NeonUI.normal(16));
				g.setColor(NeonUI.alpha(NeonUI.GRAU, 150));
				g.drawString("Minecraft-Name, z. B. Steve", 15, 29);
				g.dispose();
			}
		}
	};
	private volatile BufferedImage kopf;
	private volatile String nameStatus = "";
	private volatile String gefundenerName = "";
	private Timer suchVerzoegerung;
	private int thema;
	private final int ramEmpfohlen;
	private final long ramGesamtGb;

	private static final class Feld {
		final Rectangle r;
		final Runnable aktion;

		Feld(int x, int y, int b, int h, Runnable aktion) {
			this.r = new Rectangle(x, y, b, h);
			this.aktion = aktion;
		}
	}

	Willkommen(BambusLauncher l) {
		super(l, "Bambus", true);
		this.l = l;
		this.breite = l.getWidth();
		this.hoehe = l.getHeight();
		setUndecorated(true);
		setSize(breite, hoehe);
		setLocationRelativeTo(l);
		try { setShape(new RoundRectangle2D.Double(0, 0, breite, hoehe, 22, 22)); } catch (Exception ignoriert) { /* eckig */ }
		thema = l.client.zahl("thema", 0);
		ramGesamtGb = gesamtRamGb();
		ramEmpfohlen = ramGesamtGb >= 16 ? 6 : ramGesamtGb >= 8 ? 4 : 3;

		JPanel p = new JPanel(null) {
			@Override protected void paintComponent(Graphics gr) { zeichne(NeonUI.glatt(gr)); }
		};
		setContentPane(p);
		MouseAdapter mausHoerer = new MouseAdapter() {
			@Override public void mouseClicked(MouseEvent e) {
				for (Feld f : new ArrayList<>(felder)) if (f.r.contains(e.getPoint())) { f.aktion.run(); repaint(); return; }
			}
			@Override public void mouseMoved(MouseEvent e) {
				boolean hand = false;
				for (Feld f : felder) if (f.r.contains(e.getPoint())) hand = true;
				p.setCursor(Cursor.getPredefinedCursor(hand ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
				Willkommen.this.maus = e.getPoint();
				p.repaint();
			}
		};
		p.addMouseListener(mausHoerer);
		p.addMouseMotionListener(mausHoerer);

		nameFeld.setBackground(NeonUI.KARTE);
		nameFeld.setForeground(NeonUI.TEXT);
		nameFeld.setCaretColor(NeonUI.NEON);
		nameFeld.setFont(NeonUI.normal(16));
		nameFeld.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(NeonUI.LINIE, 1, true), BorderFactory.createEmptyBorder(6, 14, 6, 14)));
		nameFeld.setBounds(breite / 2 - 200, 300, 400, 46);
		nameFeld.setVisible(false);
		nameFeld.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
			@Override public void insertUpdate(javax.swing.event.DocumentEvent e) { nameGeaendert(); }
			@Override public void removeUpdate(javax.swing.event.DocumentEvent e) { nameGeaendert(); }
			@Override public void changedUpdate(javax.swing.event.DocumentEvent e) { nameGeaendert(); }
		});
		nameFeld.addActionListener(e -> weiter());
		p.add(nameFeld);

		new Timer(33, e -> p.repaint()).start();   // Hintergrund und Übergänge animieren
	}

	private java.awt.Point maus = new java.awt.Point(-1, -1);

	private static long gesamtRamGb() {
		try {
			Object os = ManagementFactory.getOperatingSystemMXBean();
			Object w = os.getClass().getMethod("getTotalPhysicalMemorySize").invoke(os);
			return Math.round(((Number) w).doubleValue() / (1024.0 * 1024 * 1024));
		} catch (Throwable e) {
			return 8;
		}
	}

	// ============================================================ Ablauf

	private void geheZu(int s) {
		schritt = Math.max(0, Math.min(SCHRITTE - 1, s));
		schrittSeit = System.currentTimeMillis();
		nameFeld.setVisible(schritt == 1);
		if (schritt == 1) SwingUtilities.invokeLater(nameFeld::requestFocusInWindow);
		repaint();
	}

	private void weiter() {
		if (schritt == 1 && !gefundenerName.isEmpty()) l.kosmetik.ladeSkin(gefundenerName);
		if (schritt == SCHRITTE - 1) { fertig(); return; }
		geheZu(schritt + 1);
	}

	private void fertig() {
		l.eingerichtet = true;
		l.speichereLauncherEinstellungen();
		l.repaint();
		dispose();
	}

	private void nameGeaendert() {
		gefundenerName = "";
		kopf = null;
		String n = nameFeld.getText().trim();
		nameStatus = n.isEmpty() ? "" : "Suche …";
		if (suchVerzoegerung != null) suchVerzoegerung.stop();
		if (n.isEmpty()) return;
		suchVerzoegerung = new Timer(600, e -> sucheSkin(n));   // erst suchen, wenn man kurz aufhört zu tippen
		suchVerzoegerung.setRepeats(false);
		suchVerzoegerung.start();
	}

	private void sucheSkin(String name) {
		Thread t = new Thread(() -> {
			try {
				SkinLader.Skin s = new SkinLader(l.installer.mcOrdner, l.installer.spielOrdner).lade(name);
				if (!name.equals(nameFeld.getText().trim())) return;
				kopf = s.bild;
				gefundenerName = s.name;
				nameStatus = "Gefunden";
			} catch (Exception e) {
				if (name.equals(nameFeld.getText().trim())) nameStatus = "Nicht gefunden";
			}
		}, "Skin suchen");
		t.setDaemon(true);
		t.start();
	}

	// ============================================================ Zeichnen

	private void zeichne(Graphics2D g) {
		felder.clear();
		int b = breite, h = hoehe;
		g.setColor(NeonUI.HG);
		g.fillRect(0, 0, b, h);
		Hintergruende.zeichne(g, l.hintergrund, 0, 0, b, h);
		Hintergruende.eckenLicht(g, 0, b, h);
		g.setColor(new Color(4, 7, 11, 150));
		g.fillRect(0, 0, b, h);
		NeonUI.leuchten(g, new RoundRectangle2D.Float(1, 1, b - 3, h - 3, 22, 22), NeonUI.NEON, 2);

		// Kopfzeile: Logo + Schritt
		g.setFont(NeonUI.titel(18));
		NeonUI.leuchtText(g, "BAMBUS", 34, 46, NeonUI.NEON, 1);
		g.setFont(NeonUI.mono(11));
		g.setColor(NeonUI.GRAU);
		if (schritt < SCHRITTE - 1) g.drawString("SCHRITT " + (schritt + 1) + " VON " + (SCHRITTE - 1), b - 160, 44);

		// Inhalt gleitet beim Schrittwechsel herein
		float k = Math.min(1f, (System.currentTimeMillis() - schrittSeit) / 260f);
		float weich = 1 - (1 - k) * (1 - k) * (1 - k);
		Graphics2D c = (Graphics2D) g.create();
		c.translate((1 - weich) * 40, 0);
		c.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, Math.max(0.01f, weich)));
		switch (schritt) {
			case 0: sprache(c, b); break;
			case 1: name(c, b); break;
			case 2: look(c, b); break;
			case 3: leistung(c, b); break;
			default: fertigSeite(c, b); break;
		}
		c.dispose();
		nameFeld.setLocation(breite / 2 - 200 + Math.round((1 - weich) * 40), 300);

		// Fortschritts-Punkte
		for (int i = 0; i < SCHRITTE; i++) {
			int px = b / 2 - SCHRITTE * 14 + i * 28;
			g.setColor(i == schritt ? NeonUI.NEON : i < schritt ? NeonUI.alpha(NeonUI.NEON, 120) : NeonUI.LINIE);
			if (i == schritt) g.fillRoundRect(px - 6, h - 46, 24, 8, 8, 8);
			else g.fillOval(px, h - 46, 8, 8);
		}
		// Knöpfe unten
		if (schritt > 0) knopf(g, "ZURÜCK", 34, h - 72, 140, 44, false, () -> geheZu(schritt - 1));
		if (schritt < SCHRITTE - 1) {
			g.setFont(NeonUI.mono(11));
			String ueb = "ÜBERSPRINGEN";
			int ub = g.getFontMetrics().stringWidth(ueb);
			boolean hov = new Rectangle(b - 34 - 180 - 24 - ub, h - 62, ub + 8, 24).contains(maus);
			g.setColor(hov ? NeonUI.TEXT : NeonUI.GRAU);
			g.drawString(ueb, b - 34 - 180 - 20 - ub, h - 45);
			felder.add(new Feld(b - 34 - 180 - 24 - ub, h - 62, ub + 8, 24, this::fertig));
		}
		knopf(g, schritt == SCHRITTE - 1 ? "LOS GEHT'S" : "WEITER", b - 34 - 180, h - 72, 180, 44, true, this::weiter);
	}

	private void ueberschrift(Graphics2D g, int b, String titel, String unter, int y) {
		g.setFont(NeonUI.titel(34));
		int tb = g.getFontMetrics().stringWidth(Sprache.t(titel));
		NeonUI.leuchtText(g, titel, b / 2 - tb / 2, y, NeonUI.TEXT, 1);
		if (unter != null) {
			g.setFont(NeonUI.normal(14));
			g.setColor(NeonUI.GRAU);
			int ub = g.getFontMetrics().stringWidth(unter);
			g.drawString(unter, b / 2 - ub / 2, y + 32);
		}
	}

	private void sprache(Graphics2D g, int b) {
		g.setFont(NeonUI.mono(13));
		g.setColor(NeonUI.CYAN);
		String w = "WILLKOMMEN BEI";
		g.drawString(w, b / 2 - g.getFontMetrics().stringWidth(w) / 2, 128);
		g.setFont(NeonUI.titel(58));
		int t1 = g.getFontMetrics().stringWidth("BAMBUS ");
		int t2 = g.getFontMetrics().stringWidth("CLIENT");
		int tx = b / 2 - (t1 + t2) / 2;
		NeonUI.leuchtText(g, "BAMBUS ", tx, 186, NeonUI.NEON, 2);
		NeonUI.leuchtText(g, "CLIENT", tx + t1, 186, NeonUI.TEXT, 1);
		g.setFont(NeonUI.normal(14));
		g.setColor(NeonUI.GRAU);
		String u = "Lass uns deinen Client in vier kurzen Schritten einrichten.";
		g.drawString(u, b / 2 - g.getFontMetrics().stringWidth(u) / 2, 218);

		g.setFont(NeonUI.mono(12));
		g.setColor(NeonUI.TEXT);
		String s = "WÄHLE DEINE SPRACHE";
		g.drawString(s, b / 2 - g.getFontMetrics().stringWidth(Sprache.t(s)) / 2, 282);
		int kb = 240, kh = 150, y = 302;
		sprachKarte(g, b / 2 - kb - 14, y, kb, kh, Sprache.DE, "DEUTSCH");
		sprachKarte(g, b / 2 + 14, y, kb, kh, Sprache.EN, "ENGLISH");
	}

	private void sprachKarte(Graphics2D g, int x, int y, int kb, int kh, String code, String name) {
		boolean aktiv = Sprache.code().equals(code);
		boolean hov = new Rectangle(x, y, kb, kh).contains(maus);
		karte(g, x, y, kb, kh, aktiv, hov);
		// Flagge
		int fx = x + kb / 2 - 45, fy = y + 24, fb = 90, fh = 56;
		Graphics2D f = (Graphics2D) g.create();
		f.setClip(new RoundRectangle2D.Float(fx, fy, fb, fh, 8, 8));
		if (Sprache.DE.equals(code)) {
			f.setColor(new Color(0x111111)); f.fillRect(fx, fy, fb, fh / 3 + 1);
			f.setColor(new Color(0xDD0000)); f.fillRect(fx, fy + fh / 3, fb, fh / 3 + 1);
			f.setColor(new Color(0xFFCE00)); f.fillRect(fx, fy + 2 * fh / 3, fb, fh - 2 * fh / 3);
		} else {   // vereinfachter Union Jack
			f.setColor(new Color(0x012169)); f.fillRect(fx, fy, fb, fh);
			f.setStroke(new BasicStroke(11)); f.setColor(Color.WHITE);
			f.drawLine(fx, fy, fx + fb, fy + fh); f.drawLine(fx + fb, fy, fx, fy + fh);
			f.setStroke(new BasicStroke(4)); f.setColor(new Color(0xC8102E));
			f.drawLine(fx, fy, fx + fb, fy + fh); f.drawLine(fx + fb, fy, fx, fy + fh);
			f.setColor(Color.WHITE); f.fillRect(fx + fb / 2 - 9, fy, 18, fh); f.fillRect(fx, fy + fh / 2 - 9, fb, 18);
			f.setColor(new Color(0xC8102E)); f.fillRect(fx + fb / 2 - 5, fy, 10, fh); f.fillRect(fx, fy + fh / 2 - 5, fb, 10);
		}
		f.dispose();
		g.setFont(NeonUI.titel(20));
		g.setColor(aktiv ? NeonUI.NEON : NeonUI.TEXT);
		// Sprachnamen nicht übersetzen: direkt mit der Original-Zeichenfläche messen
		int nb = g.getFontMetrics().stringWidth(name);
		g.drawString(name, x + kb / 2 - nb / 2, y + kh - 30);
		felder.add(new Feld(x, y, kb, kh, () -> {
			Sprache.setze(code);
			l.speichereLauncherEinstellungen();
			l.repaint();
		}));
	}

	private void name(Graphics2D g, int b) {
		ueberschrift(g, b, "DEIN MINECRAFT-NAME", "Für die 3D-Vorschau mit deinem Skin. Du kannst das auch überspringen.", 150);
		// Kopf des Skins
		int kg = 84, kx = b / 2 - kg / 2, ky = 200;
		g.setColor(NeonUI.KARTE);
		g.fillRoundRect(kx - 6, ky - 6, kg + 12, kg + 12, 16, 16);
		BufferedImage s = kopf;
		if (s != null) {
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
			g.drawImage(s, kx, ky, kx + kg, ky + kg, 8, 8, 16, 16, null);
			g.drawImage(s, kx - 4, ky - 4, kx + kg + 4, ky + kg + 4, 40, 8, 48, 16, null);
			NeonUI.leuchten(g, new RoundRectangle2D.Float(kx - 6, ky - 6, kg + 12, kg + 12, 16, 16), NeonUI.NEON, 2);
		} else {
			Symbole.zeichne(g, "person", kx + 18, ky + 18, 6, NeonUI.alpha(NeonUI.GRAU, 120));
		}
		g.setFont(NeonUI.mono(12));
		boolean ok = "Gefunden".equals(nameStatus);
		g.setColor(ok ? NeonUI.NEON : "Nicht gefunden".equals(nameStatus) ? new Color(0xFFB0C4) : NeonUI.GRAU);
		String st = ok ? "✓  " + Sprache.t("Gefunden") + ": " + gefundenerName : nameStatus;
		g.drawString(st, b / 2 - g.getFontMetrics().stringWidth(st) / 2, 372);
	}

	private void look(Graphics2D g, int b) {
		ueberschrift(g, b, "DEIN LOOK", "Hintergrund im Launcher und im Bambus-Hauptmenü", 130);
		int n = Hintergruende.ARTEN.length, kb = 158, kh = 104, gap = 14;
		int x0 = b / 2 - (n * kb + (n - 1) * gap) / 2, y = 184;
		for (int i = 0; i < n; i++) {
			final String id = Hintergruende.ARTEN[i][0];
			int x = x0 + i * (kb + gap);
			boolean aktiv = id.equals(l.hintergrund);
			boolean hov = new Rectangle(x, y, kb, kh).contains(maus);
			Graphics2D v = (Graphics2D) g.create();
			v.setClip(new RoundRectangle2D.Float(x, y, kb, kh, 14, 14));
			v.setColor(NeonUI.HG);
			v.fillRect(x, y, kb, kh);
			Hintergruende.zeichne(v, id, x, y, kb, kh);
			v.dispose();
			NeonUI.leuchten(g, new RoundRectangle2D.Float(x, y, kb, kh, 14, 14), aktiv ? NeonUI.NEON : hov ? NeonUI.CYAN : NeonUI.LINIE, aktiv ? 2 : 1);
			g.setFont(NeonUI.mono(11));
			g.setColor(aktiv ? NeonUI.NEON : NeonUI.TEXT);
			String name = Hintergruende.ARTEN[i][1];
			g.drawString(name, x + kb / 2 - g.getFontMetrics().stringWidth(name) / 2, y + kh + 20);
			felder.add(new Feld(x, y, kb, kh + 24, () -> {
				l.hintergrund = id;
				l.client.setze("menueHintergrund", id);
				l.speichereLauncherEinstellungen();
				l.repaint();
			}));
		}
		g.setFont(NeonUI.normal(14));
		g.setColor(NeonUI.GRAU);
		String u = "Farbe der Anzeigen im Spiel";
		g.drawString(u, b / 2 - g.getFontMetrics().stringWidth(u) / 2, 356);
		int sg = 52, sgap = 16, sx0 = b / 2 - (THEMEN.length * sg + (THEMEN.length - 1) * sgap) / 2, sy = 376;
		double t = System.currentTimeMillis() / 1000.0;
		for (int i = 0; i < THEMEN.length; i++) {
			final int nr = i;
			int x = sx0 + i * (sg + sgap);
			Color c = THEMEN[i][1].isEmpty() ? Color.getHSBColor((float) ((t * 0.3) % 1), 0.9f, 1f) : new Color(Integer.parseInt(THEMEN[i][1], 16));
			g.setPaint(new GradientPaint(x, sy, c, x + sg, sy + sg, c.darker()));
			g.fillRoundRect(x, sy, sg, sg, 14, 14);
			if (i == thema) {
				g.setStroke(new BasicStroke(3));
				g.setColor(Color.WHITE);
				g.drawRoundRect(x - 5, sy - 5, sg + 10, sg + 10, 18, 18);
			}
			g.setFont(NeonUI.mono(10));
			g.setColor(i == thema ? NeonUI.TEXT : NeonUI.GRAU);
			String n2 = THEMEN[i][0];
			g.drawString(n2, x + sg / 2 - g.getFontMetrics().stringWidth(n2) / 2, sy + sg + 18);
			felder.add(new Feld(x - 4, sy - 4, sg + 8, sg + 26, () -> {
				thema = nr;
				l.client.setze("thema", nr);
			}));
		}
	}

	private void leistung(Graphics2D g, int b) {
		ueberschrift(g, b, "LEISTUNG", "Wie viel Arbeitsspeicher Minecraft bekommt", 130);
		int kb = 112, kh = 70, gap = 14, x0 = b / 2 - (RAM.length * kb + (RAM.length - 1) * gap) / 2, y = 190;
		for (int i = 0; i < RAM.length; i++) {
			final int gb = RAM[i];
			int x = x0 + i * (kb + gap);
			boolean aktiv = l.ramGb == gb;
			boolean zuViel = gb > Math.max(2, ramGesamtGb / 2);
			karte(g, x, y, kb, kh, aktiv, new Rectangle(x, y, kb, kh).contains(maus));
			g.setFont(NeonUI.titel(22));
			g.setColor(aktiv ? NeonUI.NEON : zuViel ? NeonUI.alpha(NeonUI.GRAU, 120) : NeonUI.TEXT);
			String s = gb + " GB";
			g.drawString(s, x + kb / 2 - g.getFontMetrics().stringWidth(s) / 2, y + 40);
			if (gb == ramEmpfohlen) {
				g.setFont(NeonUI.mono(9));
				g.setColor(NeonUI.CYAN);
				String e = "empfohlen";
				g.drawString(e, x + kb / 2 - g.getFontMetrics().stringWidth(e) / 2, y + 58);
			}
			felder.add(new Feld(x, y, kb, kh, () -> { l.ramGb = gb; l.speichereLauncherEinstellungen(); }));
		}
		g.setFont(NeonUI.normal(12));
		g.setColor(NeonUI.GRAU);
		String info = "Dein Computer hat " + ramGesamtGb + " GB Arbeitsspeicher";
		g.drawString(info, b / 2 - g.getFontMetrics().stringWidth(info) / 2, y + kh + 26);

		// FPS-Boost
		boolean an = l.mods.fpsBoost();
		int fx = b / 2 - 260, fy = 330, fb = 520, fh = 84;
		karte(g, fx, fy, fb, fh, an, new Rectangle(fx, fy, fb, fh).contains(maus));
		g.setFont(NeonUI.titel(22));
		NeonUI.leuchtText(g, "⚡ FPS-BOOST", fx + 24, fy + 38, an ? NeonUI.NEON : NeonUI.TEXT, an ? 1 : 0);
		g.setFont(NeonUI.normal(12));
		g.setColor(NeonUI.GRAU);
		g.drawString("FPS-Boost: Sodium, Lithium & Co. für flüssigeres Spielen", fx + 24, fy + 62);
		// Schalter
		int sx = fx + fb - 80, sy = fy + fh / 2 - 14;
		g.setColor(an ? NeonUI.alpha(NeonUI.NEON, 90) : new Color(0x2A3445));
		g.fillRoundRect(sx, sy, 56, 28, 28, 28);
		g.setColor(an ? NeonUI.NEON : NeonUI.GRAU);
		g.fillOval(an ? sx + 31 : sx + 3, sy + 3, 22, 22);
		felder.add(new Feld(fx, fy, fb, fh, () -> l.mods.setzeFpsBoost(!l.mods.fpsBoost())));
	}

	private void fertigSeite(Graphics2D g, int b) {
		ueberschrift(g, b, "ALLES BEREIT!", "Du kannst alles später unter EINSTELLUNGEN ändern.", 150);
		String[][] zeilen = {
				{"SPRACHE", Sprache.englisch() ? "English" : "Deutsch"},
				{"SKIN", gefundenerName.isEmpty() ? "–" : gefundenerName},
				{"HINTERGRUND", name(l.hintergrund)},
				{"ARBEITSSPEICHER", l.ramGb + " GB"},
				{"⚡ FPS-BOOST", l.mods.fpsBoost() ? "✓" : "–"},
		};
		int x = b / 2 - 200, y = 222;
		for (String[] z : zeilen) {
			g.setColor(NeonUI.KARTE);
			g.fillRoundRect(x, y, 400, 38, 12, 12);
			g.setFont(NeonUI.mono(11));
			g.setColor(NeonUI.GRAU);
			g.drawString(z[0], x + 16, y + 24);
			g.setFont(NeonUI.titel(15));
			g.setColor(NeonUI.NEON);
			int wb = g.getFontMetrics().stringWidth(z[1]);
			g.drawString(z[1], x + 400 - 16 - wb, y + 25);
			y += 46;
		}
	}

	private static String name(String id) {
		for (String[] a : Hintergruende.ARTEN) if (a[0].equals(id)) return a[1];
		return id;
	}

	// ============================================================ Bauteile

	private static void karte(Graphics2D g, int x, int y, int b, int h, boolean aktiv, boolean hover) {
		RoundRectangle2D form = new RoundRectangle2D.Float(x, y, b, h, 16, 16);
		g.setColor(hover ? NeonUI.KARTE_HOVER : NeonUI.KARTE);
		g.fill(form);
		if (aktiv) {
			g.setPaint(new GradientPaint(0, y, NeonUI.alpha(NeonUI.NEON, 30), 0, y + h, NeonUI.alpha(NeonUI.NEON, 0)));
			g.fill(form);
			NeonUI.leuchten(g, form, NeonUI.NEON, 2);
		} else {
			g.setStroke(new BasicStroke(1.2f));
			g.setColor(hover ? NeonUI.alpha(NeonUI.CYAN, 160) : NeonUI.LINIE);
			g.draw(form);
		}
	}

	private void knopf(Graphics2D g, String text, int x, int y, int b, int h, boolean haupt, Runnable aktion) {
		boolean hov = new Rectangle(x, y, b, h).contains(maus);
		RoundRectangle2D form = new RoundRectangle2D.Float(x, y, b, h, 14, 14);
		if (haupt) {
			g.setPaint(new GradientPaint(x, y, NeonUI.NEON, x + b, y + h, NeonUI.CYAN));
			g.fill(form);
			if (hov) NeonUI.leuchten(g, form, NeonUI.NEON, 2);
			g.setColor(NeonUI.HG);
		} else {
			g.setColor(hov ? NeonUI.KARTE_HOVER : NeonUI.KARTE);
			g.fill(form);
			g.setStroke(new BasicStroke(1.2f));
			g.setColor(NeonUI.LINIE);
			g.draw(form);
			g.setColor(NeonUI.TEXT);
		}
		g.setFont(NeonUI.titel(15));
		int tb = g.getFontMetrics().stringWidth(text);
		g.drawString(text, x + b / 2 - tb / 2, y + h / 2 + 6);
		felder.add(new Feld(x, y, b, h, aktion));
	}

}
