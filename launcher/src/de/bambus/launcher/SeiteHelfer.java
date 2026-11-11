package de.bambus.launcher;

import de.bambus.helfer.HelferWissen;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** HELFER: Offline-Hilfe-Chat zum Client und zu Minecraft (keine KI, kein Internet). */
final class SeiteHelfer extends JPanel {

	private static final int X = 40, B = 696;
	private static final int CHAT_Y = 168, CHAT_H = 338;
	private static final int BLASE_MAX = 470;

	private final BambusLauncher l;
	private final JPanel verlauf = new JPanel(null);
	private final JScrollPane scroll;
	private final List<Blase> blasen = new ArrayList<>();
	private final List<Chip> chips = new ArrayList<>();
	private boolean tippt;
	private String chipSprache = "";

	private final JTextField feld = new JTextField() {
		@Override
		protected void paintComponent(Graphics gr) {
			super.paintComponent(gr);
			if (getText().isEmpty()) {
				Graphics2D g = NeonUI.glatt(gr);
				g.setFont(NeonUI.normal(13));
				g.setColor(NeonUI.alpha(NeonUI.GRAU, 170));
				g.drawString("Schreib deine Frage, z. B. „Wie baue ich ein Netherportal?“", 13, 24);
				g.dispose();
			}
		}
	};

	SeiteHelfer(BambusLauncher l) {
		this.l = l;
		setOpaque(false);
		setLayout(null);

		verlauf.setOpaque(false);
		scroll = SeiteMods.scroll(verlauf, X - 4, CHAT_Y, B + 14, CHAT_H);
		add(scroll);

		feld.setBounds(X, 522, B - 136, 38);
		feld.setBackground(NeonUI.KARTE);
		feld.setForeground(NeonUI.TEXT);
		feld.setCaretColor(NeonUI.NEON);
		feld.setFont(NeonUI.normal(13));
		feld.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(NeonUI.alpha(NeonUI.NEON, 110), 1, true), BorderFactory.createEmptyBorder(4, 12, 4, 12)));
		feld.addActionListener(e -> senden(feld.getText()));
		add(feld);
		NeonUI.UmrissKnopf los = new NeonUI.UmrissKnopf("SENDEN  ➤", NeonUI.NEON);
		los.setBounds(X + B - 124, 522, 124, 38);
		los.addActionListener(e -> senden(feld.getText()));
		add(los);

		baueChips();
		helfer(HelferWissen.begruessung(Sprache.englisch()), null);
	}

	void fokus() {
		SwingUtilities.invokeLater(feld::requestFocusInWindow);
	}

	/** Vorschläge passend zur Sprache (wird beim Zeichnen geprüft, falls die Sprache wechselt). */
	private void baueChips() {
		for (Chip c : chips) remove(c);
		chips.clear();
		chipSprache = Sprache.code();
		if (blasen.size() == 1 && blasen.get(0).eintrag == null) {   // Begrüßung in der neuen Sprache
			blasen.clear();
			helfer(HelferWissen.begruessung(Sprache.englisch()), null);
		}
		String[] v = Sprache.englisch() ? HelferWissen.VORSCHLAEGE_EN : HelferWissen.VORSCHLAEGE_DE;
		Font f = NeonUI.normal(12);
		FontMetrics fm = getFontMetrics(f);
		int x = X, y = 104;
		for (String s : v) {
			int b = fm.stringWidth(s) + 26;
			if (x + b > X + B) { x = X; y += 32; }
			if (y > 136) break;
			Chip c = new Chip(s);
			c.setBounds(x, y, b, 26);
			chips.add(c);
			add(c);
			x += b + 8;
		}
		revalidate();
		repaint();
	}

	private void senden(String text) {
		text = text.trim();
		if (text.isEmpty() || tippt) return;
		feld.setText("");
		blasen.add(new Blase(text, false, null));
		Blase punkte = new Blase("", true, null);
		punkte.tippen = true;
		blasen.add(punkte);
		tippt = true;
		neuAnordnen();
		final String frage = text;
		Timer t = new Timer(380 + Math.min(500, frage.length() * 8), e -> {
			blasen.remove(punkte);
			tippt = false;
			antworte(frage);
		});
		t.setRepeats(false);
		t.start();
	}

	private void antworte(String frage) {
		String rechnung = netherRechner(frage);
		if (rechnung != null) {
			helfer(rechnung, null);
			return;
		}
		HelferWissen.Eintrag e = HelferWissen.finde(frage);
		if (e == null) helfer(HelferWissen.unbekannt(Sprache.englisch()), null);
		else helfer(e.text(Sprache.englisch()), e);
	}

	private void helfer(String text, HelferWissen.Eintrag e) {
		blasen.add(new Blase(text, true, e));
		neuAnordnen();
	}

	private void neuAnordnen() {
		verlauf.removeAll();
		int y = 6;
		for (Blase b : blasen) {
			Dimension d = b.groesse();
			int x = b.helfer ? 4 : B - d.width;
			b.setBounds(x, y, d.width, d.height);
			verlauf.add(b);
			y += d.height + 12;
		}
		verlauf.setPreferredSize(new Dimension(B, y));
		verlauf.revalidate();
		verlauf.repaint();
		final int ende = y;
		SwingUtilities.invokeLater(() -> {
			JScrollBar sb = scroll.getVerticalScrollBar();
			sb.setValue(sb.getMaximum());
			verlauf.scrollRectToVisible(new Rectangle(0, ende, 1, 1));
		});
	}

	// ============================================================ Nether-Rechner

	static String netherRechner(String frage) {
		return HelferWissen.netherRechner(frage, Sprache.englisch());
	}

	// ============================================================ Zeichnen

	@Override
	protected void paintComponent(Graphics graphics) {
		if (!Sprache.code().equals(chipSprache)) SwingUtilities.invokeLater(this::baueChips);
		Graphics2D g = NeonUI.glatt(graphics);
		g.setFont(NeonUI.titel(30));
		NeonUI.leuchtText(g, "HELFER", X, 62, NeonUI.TEXT, 1);
		int tb = g.getFontMetrics().stringWidth(Sprache.t("HELFER"));
		g.setFont(NeonUI.mono(10));
		g.setColor(NeonUI.alpha(NeonUI.NEON, 34));
		g.fillRoundRect(X + tb + 14, 40, 70, 22, 11, 11);
		g.setColor(NeonUI.NEON);
		g.drawString("OFFLINE", X + tb + 24, 55);
		g.setFont(NeonUI.normal(13));
		g.setColor(NeonUI.GRAU);
		g.drawString("Frag mich alles zum Bambus Client und zu Minecraft – ganz ohne Internet.", X, 86);

		// Chat-Fläche
		RoundRectangle2D flaeche = new RoundRectangle2D.Float(X - 6, CHAT_Y - 6, B + 12, CHAT_H + 12, 16, 16);
		g.setColor(NeonUI.alpha(NeonUI.KARTE, 200));
		g.fill(flaeche);
		g.setStroke(new BasicStroke(1.2f));
		g.setColor(NeonUI.LINIE);
		g.draw(flaeche);

		g.setFont(NeonUI.normal(10));
		g.setColor(NeonUI.alpha(NeonUI.GRAU, 170));
		g.drawString("Der Helfer ist keine echte KI: Er kennt feste Antworten und kann sich irren. Alles bleibt auf deinem Computer.", X, 580);
		g.dispose();
	}

	/** Zeichnen ohne Übersetzung (die Texte sind schon in der richtigen Sprache). */
	private static Graphics2D roh(Graphics graphics) {
		return NeonUI.pixel(graphics);
	}

	private static List<String> umbrechen(FontMetrics fm, String text, int max) {
		List<String> zeilen = new ArrayList<>();
		for (String absatz : text.split("\n")) {
			StringBuilder z = new StringBuilder();
			for (String wort : absatz.split(" ")) {
				String probe = z.length() == 0 ? wort : z + " " + wort;
				if (fm.stringWidth(probe) > max && z.length() > 0) {
					zeilen.add(z.toString());
					z = new StringBuilder(wort);
				} else {
					z = new StringBuilder(probe);
				}
			}
			zeilen.add(z.toString());
		}
		return zeilen;
	}

	// ============================================================ Sprechblase

	private static final Map<String, Color> FARBEN = new HashMap<>();
	private static final Map<String, String> ENGLISCH = new HashMap<>();

	static {
		Object[] d = {
				"Brett", 0xB8894F, "Plank", "Stock", 0x8A6A3E, "Stick", "Kohle", 0x3A3F47, "Coal",
				"Stein", 0x8C939C, "Cobble", "Eisen", 0xD8DEE6, "Iron", "Diamant", 0x4FE3E0, "Diamond",
				"Wolle", 0xF2F2F2, "Wool", "Buch", 0xA0522D, "Book", "Obsidian", 0x3B2A5C, "Obsidian",
				"Eisenblock", 0xC4CBD4, "Iron blk", "Faden", 0xE8E8E8, "String", "Pulver", 0x5A5A5A, "Powder",
				"Sand", 0xE3D49A, "Sand",
		};
		for (int i = 0; i < d.length; i += 3) {
			FARBEN.put((String) d[i], new Color((Integer) d[i + 1]));
			ENGLISCH.put((String) d[i], (String) d[i + 2]);
		}
	}

	private final class Blase extends JComponent {
		final String text;
		final boolean helfer;
		final HelferWissen.Eintrag eintrag;
		boolean tippen;
		private final List<Rectangle> knopfFlaechen = new ArrayList<>();
		private int ueberKnopf = -1;

		private static final int PAD = 14, AVATAR = 38, ZEILE = 19, FELD = 44;

		Blase(String text, boolean helfer, HelferWissen.Eintrag eintrag) {
			this.text = text;
			this.helfer = helfer;
			this.eintrag = eintrag;
			if (eintrag != null && !eintrag.aktionen.isEmpty()) {
				MouseAdapter m = new MouseAdapter() {
					@Override public void mouseMoved(MouseEvent e) {
						int k = knopfBei(e.getX(), e.getY());
						if (k != ueberKnopf) {
							ueberKnopf = k;
							setCursor(Cursor.getPredefinedCursor(k >= 0 ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
							repaint();
						}
					}
					@Override public void mouseExited(MouseEvent e) { ueberKnopf = -1; repaint(); }
					@Override public void mouseClicked(MouseEvent e) {
						int k = knopfBei(e.getX(), e.getY());
						if (k >= 0) l.zeige(eintrag.aktionen.get(k)[2]);
					}
				};
				addMouseListener(m);
				addMouseMotionListener(m);
			}
		}

		private int knopfBei(int x, int y) {
			for (int i = 0; i < knopfFlaechen.size(); i++) if (knopfFlaechen.get(i).contains(x, y)) return i;
			return -1;
		}

		private int links() { return helfer ? AVATAR + 8 : 0; }

		Dimension groesse() {
			if (tippen) return new Dimension(links() + 70, 40);
			FontMetrics fm = getFontMetrics(NeonUI.normal(13));
			List<String> zeilen = umbrechen(fm, text, BLASE_MAX - 2 * PAD);
			int breite = 0;
			for (String z : zeilen) breite = Math.max(breite, fm.stringWidth(z));
			int h = PAD * 2 + zeilen.size() * ZEILE - 4;
			if (eintrag != null && eintrag.rezept != null) {
				h += 3 * FELD + 2 * 4 + 32;
				breite = Math.max(breite, 3 * FELD + 8 + 30 + 2 * FELD);
			}
			if (eintrag != null && !eintrag.aktionen.isEmpty()) {
				h += 38;
				FontMetrics km = getFontMetrics(NeonUI.mono(11));
				int kb = 0;
				for (String[] a : eintrag.aktionen) kb += km.stringWidth(Sprache.englisch() ? a[1] : a[0]) + 34 + 8;
				breite = Math.max(breite, kb);
			}
			return new Dimension(links() + breite + 2 * PAD + 2, h);
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			Graphics2D g = roh(graphics);
			int b = getWidth(), h = getHeight();
			int lx = links();
			Color farbe = helfer ? NeonUI.NEON : NeonUI.CYAN;

			if (helfer) {   // Bambus-Avatar
				g.setColor(NeonUI.alpha(NeonUI.NEON, 30));
				g.fillRoundRect(0, 0, AVATAR, AVATAR, 12, 12);
				g.setColor(NeonUI.alpha(NeonUI.NEON, 120));
				g.drawRoundRect(0, 0, AVATAR - 1, AVATAR - 1, 12, 12);
				NeonUI.logo(g, 7, 6, 24);
			}
			RoundRectangle2D form = new RoundRectangle2D.Float(lx + 0.5f, 0.5f, b - lx - 2, h - 2, 16, 16);
			g.setColor(helfer ? new Color(0x101A24) : NeonUI.alpha(NeonUI.CYAN, 34));
			g.fill(form);
			g.setStroke(new BasicStroke(1.2f));
			g.setColor(NeonUI.alpha(farbe, helfer ? 90 : 140));
			g.draw(form);

			if (tippen) {
				double t = System.currentTimeMillis() / 160.0;
				for (int i = 0; i < 3; i++) {
					double a = 0.35 + 0.65 * Math.max(0, Math.sin(t - i * 0.9));
					g.setColor(NeonUI.alpha(NeonUI.NEON, (int) (255 * a)));
					g.fillOval(lx + 18 + i * 14, 16, 8, 8);
				}
				g.dispose();
				return;
			}

			g.setFont(NeonUI.normal(13));
			FontMetrics fm = g.getFontMetrics();
			List<String> zeilen = umbrechen(fm, text, BLASE_MAX - 2 * PAD);
			g.setColor(helfer ? new Color(0xDDE6F0) : NeonUI.TEXT);
			int y = PAD + fm.getAscent() - 2;
			for (String z : zeilen) {
				g.drawString(z, lx + PAD, y);
				y += ZEILE;
			}
			y += -fm.getAscent() + 8;

			if (eintrag != null && eintrag.rezept != null) y = zeichneRezept(g, lx + PAD, y);

			knopfFlaechen.clear();
			if (eintrag != null && !eintrag.aktionen.isEmpty()) {
				g.setFont(NeonUI.mono(11));
				FontMetrics km = g.getFontMetrics();
				int kx = lx + PAD;
				int ky = h - PAD - 26;
				for (int i = 0; i < eintrag.aktionen.size(); i++) {
					String[] a = eintrag.aktionen.get(i);
					String txt = (Sprache.englisch() ? a[1] : a[0]) + "  →";
					int kb = km.stringWidth(txt) + 24;
					Rectangle r = new Rectangle(kx, ky, kb, 26);
					knopfFlaechen.add(r);
					boolean ue = i == ueberKnopf;
					g.setColor(NeonUI.alpha(NeonUI.NEON, ue ? 60 : 22));
					g.fillRoundRect(r.x, r.y, r.width, r.height, 13, 13);
					g.setColor(NeonUI.alpha(NeonUI.NEON, ue ? 230 : 150));
					g.drawRoundRect(r.x, r.y, r.width - 1, r.height - 1, 13, 13);
					g.drawString(txt, r.x + 12, r.y + 17);
					kx += kb + 8;
				}
			}
			g.dispose();
		}

		private int zeichneRezept(Graphics2D g, int x, int y) {
			boolean en = Sprache.englisch();
			y += 4;
			Font klein = NeonUI.normal(9);
			for (int i = 0; i < 9; i++) {
				int fx = x + (i % 3) * (FELD + 4), fy = y + (i / 3) * (FELD + 4);
				g.setColor(new Color(0x1C2633));
				g.fillRoundRect(fx, fy, FELD, FELD, 8, 8);
				g.setColor(new Color(0x2C3A4C));
				g.drawRoundRect(fx, fy, FELD - 1, FELD - 1, 8, 8);
				String ding = eintrag.rezept[i];
				if (ding.isEmpty()) continue;
				Color c = FARBEN.getOrDefault(ding, NeonUI.GRAU);
				g.setColor(c);
				g.fillRoundRect(fx + 11, fy + 6, FELD - 22, FELD - 26, 5, 5);
				g.setColor(NeonUI.alpha(Color.WHITE, 60));
				g.fillRect(fx + 13, fy + 8, FELD - 26, 3);
				g.setFont(klein);
				FontMetrics fm = g.getFontMetrics();
				String name = en ? ENGLISCH.getOrDefault(ding, ding) : ding;
				g.setColor(new Color(0xC8D2DE));
				g.drawString(name, fx + (FELD - fm.stringWidth(name)) / 2, fy + FELD - 7);
			}
			// Pfeil und Ergebnis
			int gx = x + 3 * FELD + 8;
			int mitte = y + (3 * FELD + 8) / 2;
			g.setColor(NeonUI.NEON);
			g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.drawLine(gx + 6, mitte, gx + 22, mitte);
			g.drawLine(gx + 16, mitte - 6, gx + 22, mitte);
			g.drawLine(gx + 16, mitte + 6, gx + 22, mitte);
			int ex = gx + 30, eb = 2 * FELD;
			g.setColor(NeonUI.alpha(NeonUI.NEON, 26));
			g.fillRoundRect(ex, mitte - FELD, eb, 2 * FELD, 12, 12);
			g.setColor(NeonUI.alpha(NeonUI.NEON, 160));
			g.setStroke(new BasicStroke(1.4f));
			g.drawRoundRect(ex, mitte - FELD, eb - 1, 2 * FELD - 1, 12, 12);
			// Ergebnis: Würfel in der Farbe der häufigsten Zutat
			Map<String, Integer> anzahl = new HashMap<>();
			String haupt = "";
			for (String z : eintrag.rezept) {
				if (z.isEmpty()) continue;
				int a = anzahl.merge(z, 1, Integer::sum);
				if (haupt.isEmpty() || a > anzahl.get(haupt)) haupt = z;
			}
			Color hc = FARBEN.getOrDefault(haupt, NeonUI.NEON);
			int wx = ex + eb / 2 - 18, wy = mitte - FELD + 12;
			g.setColor(hc);
			g.fillRoundRect(wx, wy, 36, 36, 8, 8);
			g.setColor(NeonUI.alpha(Color.WHITE, 70));
			g.fillRoundRect(wx + 4, wy + 4, 28, 6, 4, 4);
			g.setColor(NeonUI.alpha(Color.BLACK, 60));
			g.fillRoundRect(wx + 4, wy + 26, 28, 6, 4, 4);
			g.setColor(NeonUI.NEON);
			g.drawRoundRect(wx - 1, wy - 1, 37, 37, 9, 9);
			g.setFont(NeonUI.normal(11));
			FontMetrics fm = g.getFontMetrics();
			String name = en ? eintrag.rezeptEn : eintrag.rezeptDe;
			List<String> teile = umbrechen(fm, name, eb - 8);
			int ty = teile.size() > 1 ? mitte + 24 : mitte + 30;
			g.setColor(NeonUI.TEXT);
			for (int i = 0; i < Math.min(2, teile.size()); i++) {
				g.drawString(teile.get(i), ex + (eb - fm.stringWidth(teile.get(i))) / 2, ty + i * 13);
			}
			return y + 3 * FELD + 8 + 28;
		}
	}

	// ============================================================ Vorschlag

	private final class Chip extends JComponent {
		final String text;
		private boolean ueber;

		Chip(String text) {
			this.text = text;
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			addMouseListener(new MouseAdapter() {
				@Override public void mouseEntered(MouseEvent e) { ueber = true; repaint(); }
				@Override public void mouseExited(MouseEvent e) { ueber = false; repaint(); }
				@Override public void mouseClicked(MouseEvent e) { senden(text); }
			});
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			Graphics2D g = roh(graphics);
			int b = getWidth(), h = getHeight();
			g.setColor(ueber ? NeonUI.alpha(NeonUI.CYAN, 50) : NeonUI.alpha(NeonUI.KARTE, 230));
			g.fillRoundRect(0, 0, b - 1, h - 1, h, h);
			g.setColor(NeonUI.alpha(NeonUI.CYAN, ueber ? 220 : 110));
			g.drawRoundRect(0, 0, b - 1, h - 1, h, h);
			g.setFont(NeonUI.normal(12));
			g.setColor(ueber ? NeonUI.TEXT : new Color(0xC8D2DE));
			g.drawString(text, 13, 17);
			g.dispose();
		}
	}
}
