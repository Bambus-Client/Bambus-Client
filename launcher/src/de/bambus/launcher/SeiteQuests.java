package de.bambus.launcher;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import de.bambus.quests.Quests;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/** QUESTS: Blätter sammeln, Rang aufsteigen und im Shop Kosmetik freischalten – alles kostenlos. */
final class SeiteQuests extends JPanel {

	private static final int X = 40, B = 696;

	private final BambusLauncher l;
	private final Inhalt inhalt;

	SeiteQuests(BambusLauncher l) {
		this.l = l;
		setOpaque(false);
		setLayout(null);
		inhalt = new Inhalt();
		add(SeiteMods.scroll(inhalt, X - 4, 150, B + 14, 428));
	}

	/** Beim Öffnen: Erfolge nachrechnen und alles neu zeichnen. */
	void aktualisiere() {
		l.fortschritt.pruefeErfolge(l);
		inhalt.neuMessen();
		repaint();
	}

	@Override
	protected void paintComponent(Graphics graphics) {
		Graphics2D g = NeonUI.glatt(graphics);
		Fortschritt f = l.fortschritt;
		g.setFont(NeonUI.titel(30));
		NeonUI.leuchtText(g, "QUESTS", X, 62, NeonUI.TEXT, 1);
		g.setFont(NeonUI.normal(13));
		g.setColor(NeonUI.GRAU);
		g.drawString("Sammle Blätter und schalte Kosmetik frei. Kostet nie Geld.", X, 86);

		// Guthaben oben rechts
		int kb = 210, kx = X + B - kb;
		g.setColor(NeonUI.KARTE);
		g.fillRect(kx, 36, kb, 48);
		g.setStroke(new BasicStroke(1.4f));
		g.setColor(NeonUI.alpha(NeonUI.NEON, 160));
		g.drawRect(kx, 36, kb, 48);
		blatt(g, kx + 16, 50, 18, NeonUI.NEON);
		g.setFont(NeonUI.titel(22));
		NeonUI.schattenText(g, String.valueOf(f.blaetter()), kx + 44, 68, NeonUI.TEXT);
		g.setFont(NeonUI.mono(10));
		g.setColor(NeonUI.GRAU);
		g.drawString("BLÄTTER", kx + 44 + g.getFontMetrics(NeonUI.titel(22)).stringWidth(String.valueOf(f.blaetter())) + 10, 68);

		// Rang-Leiste
		int ry = 104;
		g.setColor(NeonUI.KARTE);
		g.fillRect(X, ry, B, 36);
		g.setColor(NeonUI.LINIE);
		g.setStroke(new BasicStroke(1.2f));
		g.drawRect(X, ry, B, 36);
		Color rf = f.rangFarbe();
		g.setColor(NeonUI.alpha(rf, 40));
		g.fillRect(X + 1, ry + 1, (int) ((B - 1) * f.rangFortschritt()), 34);
		Symbole.zeichne(g, "stern", X + 10, ry + 10, 2, rf);
		g.setFont(NeonUI.mono(12));
		NeonUI.schattenText(g, "RANG: " + f.rangName().toUpperCase(), X + 38, ry + 24, rf);
		g.setFont(NeonUI.normal(11));
		g.setColor(NeonUI.GRAU);
		String rest = f.bisNaechsterRang() > 0
				? "noch " + f.bisNaechsterRang() + " Blätter bis " + Quests.RAENGE[f.rang() + 1][0]
				: "höchster Rang erreicht";
		g.drawString(rest, X + B - 12 - g.getFontMetrics().stringWidth(Sprache.t(rest)), ry + 23);
		g.dispose();
	}

	/** Kleines Bambusblatt als Punkte-Symbol. */
	static void blatt(Graphics2D g, int x, int y, int gr, Color farbe) {
		g.setColor(farbe);
		for (int i = 0; i < gr; i++) {
			int w = (int) (Math.sin(Math.PI * i / (double) gr) * gr * 0.42);
			g.fillRect(x + i - gr / 2, y + gr / 2 - i / 2 - w / 2, 1, Math.max(1, w));
		}
		g.setColor(NeonUI.alpha(farbe, 120));
		g.drawLine(x - gr / 2, y + gr / 2, x + gr / 2, y);
	}

	// ============================================================ Inhalt zum Scrollen

	private final class Inhalt extends JPanel {

		private static final int ZEILE = 54, KACHEL_H = 74, SPALTEN = 3, ABSTAND = 10;

		private final List<Rectangle> kaufFlaechen = new ArrayList<>();
		private final List<String> kaufIds = new ArrayList<>();
		private int ueber = -1;
		private int yQuests, yHart, yErfolge, yShop, hoehe;

		Inhalt() {
			setOpaque(false);
			setLayout(null);
			neuMessen();
			MouseAdapter m = new MouseAdapter() {
				@Override public void mouseMoved(MouseEvent e) {
					int i = bei(e.getX(), e.getY());
					if (i != ueber) {
						ueber = i;
						setCursor(Cursor.getPredefinedCursor(i >= 0 ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
						repaint();
					}
				}
				@Override public void mouseExited(MouseEvent e) { ueber = -1; repaint(); }
				@Override public void mouseClicked(MouseEvent e) {
					int i = bei(e.getX(), e.getY());
					if (i >= 0) kaufen(kaufIds.get(i));
				}
			};
			addMouseListener(m);
			addMouseMotionListener(m);
		}

		private int bei(int x, int y) {
			for (int i = 0; i < kaufFlaechen.size(); i++) if (kaufFlaechen.get(i).contains(x, y)) return i;
			return -1;
		}

		void neuMessen() {
			yQuests = 126;
			hoehe = yQuests + 3 * (ZEILE + 8) + 36;
			yHart = hoehe;
			hoehe = yHart + Quests.HART_ANZAHL * (ZEILE + 8) + 36;
			yErfolge = hoehe;
			int ez = (Quests.ERFOLGE.length + SPALTEN - 1) / SPALTEN;
			hoehe = yErfolge + ez * (KACHEL_H + ABSTAND) + 36;
			yShop = hoehe;
			int sz = (Quests.SHOP.length + SPALTEN - 1) / SPALTEN;
			hoehe = yShop + sz * (KACHEL_H + 28 + ABSTAND) + 20;
			setPreferredSize(new Dimension(B, hoehe));
			revalidate();
		}

		private void kaufen(String id) {
			Fortschritt f = l.fortschritt;
			String[] s = Fortschritt.shopEintrag(id);
			if (s == null || f.gekauft(id)) return;
			int preis = Integer.parseInt(s[3]);
			if (f.blaetter() < preis) {
				JOptionPane.showMessageDialog(this,
						Sprache.t("Dafür fehlen dir noch") + " " + (preis - f.blaetter()) + " " + Sprache.t("Blätter."),
						Sprache.t("Shop"), JOptionPane.INFORMATION_MESSAGE);
				return;
			}
			int antwort = JOptionPane.showConfirmDialog(this,
					Sprache.t(s[2]) + " " + Sprache.t("für") + " " + preis + " " + Sprache.t("Blätter freischalten?"),
					Sprache.t("Shop"), JOptionPane.YES_NO_OPTION);
			if (antwort != JOptionPane.YES_OPTION || !f.kaufe(id)) return;
			JOptionPane.showMessageDialog(this,
					Sprache.t(s[2]) + " " + Sprache.t("ist freigeschaltet – du findest es in der GARDEROBE."),
					Sprache.t("Shop"), JOptionPane.INFORMATION_MESSAGE);
			l.kosmetik.ladeKacheln();
			SeiteQuests.this.repaint();
			repaint();
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			Graphics2D g = NeonUI.glatt(graphics);
			Fortschritt f = l.fortschritt;
			kaufFlaechen.clear();
			kaufIds.clear();

			tagesBonus(g, f);
			ueberschrift(g, "HEUTE", yQuests - 16);
			List<String[]> heute = f.heutigeQuests();
			for (int i = 0; i < heute.size(); i++) {
				questZeile(g, heute.get(i), f.questFertig(heute.get(i)[0]), 0, yQuests + i * (ZEILE + 8), null);
			}

			ueberschrift(g, "HERAUSFORDERUNGEN  –  IM SPIEL", yHart - 16);
			List<String[]> hart = f.heutigeHart();
			for (int i = 0; i < hart.size(); i++) {
				String[] q = hart.get(i);
				int ziel = Integer.parseInt(q[4]);
				questZeile(g, q, f.hartFertig(q[0]), 0, yHart + i * (ZEILE + 8),
						ziel > 1 ? Math.min(f.hartStand(q[0]), ziel) + " / " + ziel : null);
			}

			ueberschrift(g, "ERFOLGE", yErfolge - 16);
			int kb = (B - (SPALTEN - 1) * ABSTAND) / SPALTEN;
			for (int i = 0; i < Quests.ERFOLGE.length; i++) {
				String[] e = Quests.ERFOLGE[i];
				kachel(g, (i % SPALTEN) * (kb + ABSTAND), yErfolge + (i / SPALTEN) * (KACHEL_H + ABSTAND), kb, KACHEL_H,
						e[1], e[2], Integer.parseInt(e[3]), f.erfolgFertig(e[0]), null);
			}

			ueberschrift(g, "SHOP  –  NUR MIT BLÄTTERN", yShop - 16);
			for (int i = 0; i < Quests.SHOP.length; i++) {
				String[] s = Quests.SHOP[i];
				int kx = (i % SPALTEN) * (kb + ABSTAND), ky = yShop + (i / SPALTEN) * (KACHEL_H + 28 + ABSTAND);
				boolean hat = f.gekauft(s[0]);
				String art = s[1].equals("cape") ? "Umhang" : "Partikel";
				kachel(g, kx, ky, kb, KACHEL_H + 28, s[2], art, Integer.parseInt(s[3]), hat, s[0]);
			}
			g.dispose();
		}

		/** Tages-Bonus mit Serie als sieben Punkten. */
		private void tagesBonus(Graphics2D g, Fortschritt f) {
			int h = 86;
			g.setColor(NeonUI.KARTE);
			g.fillRect(0, 4, B, h);
			g.setStroke(new BasicStroke(1.4f));
			g.setColor(NeonUI.alpha(NeonUI.CYAN, 150));
			g.drawRect(0, 4, B, h);
			NeonUI.ecken(g, -2, 2, B + 4, h + 4, 8, NeonUI.CYAN);
			g.setFont(NeonUI.titel(18));
			NeonUI.schattenText(g, "TÄGLICHER BONUS", 18, 32, NeonUI.TEXT);
			g.setFont(NeonUI.normal(12));
			g.setColor(NeonUI.GRAU);
			int serie = f.serie();
			g.drawString("Tag " + serie + " in Folge · jeden Tag gibt es mehr", 18, 52);
			g.setFont(NeonUI.mono(11));
			g.setColor(NeonUI.CYAN);
			int heute = l.client.zahl("letzterBonus", 0);
			g.drawString("✓ " + Sprache.t("Heute abgeholt:") + " +" + heute + " " + Sprache.t("Blätter"), 18, 76);
			for (int i = 0; i < 7; i++) {
				int px = B - 24 - (6 - i) * 34, py = 34;
				boolean voll = (serie - 1) % 7 >= i || serie % 7 == 0 && serie > 0;
				g.setColor(voll ? NeonUI.alpha(NeonUI.CYAN, 60) : new Color(0x16202E));
				g.fillRect(px - 12, py - 12, 26, 26);
				g.setColor(voll ? NeonUI.CYAN : NeonUI.LINIE);
				g.drawRect(px - 12, py - 12, 26, 26);
				if (voll) blatt(g, px, py - 4, 13, NeonUI.CYAN);
				g.setFont(NeonUI.mono(9));
				g.setColor(NeonUI.GRAU);
				g.drawString(String.valueOf(i + 1), px - 3, py + 26);
			}
		}

		private void questZeile(Graphics2D g, String[] q, boolean fertig, int x, int y, String stand) {
			g.setColor(NeonUI.KARTE);
			g.fillRect(x, y, B, ZEILE);
			g.setStroke(new BasicStroke(1.2f));
			g.setColor(fertig ? NeonUI.alpha(NeonUI.NEON, 170) : NeonUI.LINIE);
			g.drawRect(x, y, B, ZEILE);
			// Haken
			g.setColor(fertig ? NeonUI.NEON : new Color(0x2A3647));
			g.fillRect(x + 14, y + 17, 20, 20);
			if (fertig) {
				g.setColor(NeonUI.HG);
				g.setStroke(new BasicStroke(2.4f));
				g.drawLine(x + 18, y + 27, x + 22, y + 32);
				g.drawLine(x + 22, y + 32, x + 30, y + 21);
			}
			g.setFont(NeonUI.titel(14));
			g.setColor(fertig ? NeonUI.TEXT : new Color(0xC8D2DE));
			g.drawString(q[1], x + 46, y + 24);
			g.setFont(NeonUI.normal(11));
			g.setColor(NeonUI.GRAU);
			g.drawString(q[2], x + 46, y + 42);
			if (stand != null && !fertig) {
				g.setFont(NeonUI.mono(10));
				g.setColor(NeonUI.CYAN);
				g.drawString(stand, x + B - 170, y + ZEILE / 2 + 4);
			}
			lohn(g, x + B - 90, y + ZEILE / 2, Integer.parseInt(q[3]), fertig ? NeonUI.NEON : NeonUI.GRAU, true);
		}

		/** Karte für Erfolg oder Shop. shopId != null macht sie anklickbar. */
		private void kachel(Graphics2D g, int x, int y, int b, int h, String titel, String text, int wert,
							boolean fertig, String shopId) {
			boolean kaufbar = shopId != null && !fertig;
			boolean hover = kaufbar && ueber >= 0 && ueber < kaufIds.size() && kaufIds.get(ueber).equals(shopId);
			Color rand = fertig ? NeonUI.alpha(NeonUI.NEON, 170) : kaufbar ? NeonUI.alpha(NeonUI.CYAN, 120) : NeonUI.LINIE;
			g.setColor(hover ? NeonUI.KARTE_HOVER : NeonUI.KARTE);
			g.fillRect(x, y, b, h);
			g.setStroke(new BasicStroke(fertig ? 1.4f : 1.2f));
			g.setColor(hover ? NeonUI.CYAN : rand);
			g.drawRect(x, y, b, h);
			g.setFont(NeonUI.titel(14));
			g.setColor(fertig ? NeonUI.TEXT : new Color(0xC8D2DE));
			g.drawString(NeonUI.passend(g, titel, b - 40), x + 12, y + 24);
			g.setFont(NeonUI.normal(11));
			g.setColor(NeonUI.GRAU);
			for (String zeile : zweiZeilen(g, text, b - 24)) {
				g.drawString(zeile, x + 12, y + 42);
				break;
			}
			if (fertig) {
				g.setFont(NeonUI.mono(10));
				g.setColor(NeonUI.NEON);
				g.drawString(shopId != null ? "✓ FREIGESCHALTET" : "✓ GESCHAFFT", x + 12, y + h - 12);
			} else {
				lohn(g, x + 12 + 14, y + h - 18, wert, kaufbar ? NeonUI.CYAN : NeonUI.GRAU, shopId == null);
			}
			if (kaufbar) {
				Rectangle r = new Rectangle(x, y, b, h);
				kaufFlaechen.add(r);
				kaufIds.add(shopId);
				g.setFont(NeonUI.mono(10));
				g.setColor(hover ? NeonUI.CYAN : NeonUI.alpha(NeonUI.CYAN, 150));
				String kt = "HOLEN →";
				g.drawString(kt, x + b - 12 - g.getFontMetrics().stringWidth(Sprache.t(kt)), y + h - 12);
			}
		}

		private List<String> zweiZeilen(Graphics2D g, String text, int max) {
			List<String> out = new ArrayList<>();
			out.add(NeonUI.passend(g, text, max));
			return out;
		}

		/** Blatt-Symbol mit Zahl. plus = Belohnung („+15“), sonst Preis („400“). */
		private void lohn(Graphics2D g, int x, int y, int wert, Color farbe, boolean plus) {
			blatt(g, x, y - 4, 13, farbe);
			g.setFont(NeonUI.mono(11));
			g.setColor(farbe);
			g.drawString((plus ? "+" : "") + wert, x + 12, y + 1);
		}

		private void ueberschrift(Graphics2D g, String text, int y) {
			g.setFont(NeonUI.mono(12));
			g.setColor(NeonUI.NEON);
			g.drawString(text, 0, y);
			int tb = g.getFontMetrics().stringWidth(Sprache.t(text));
			g.setColor(NeonUI.LINIE);
			g.fillRect(tb + 12, y - 4, B - tb - 12, 1);
		}
	}
}
