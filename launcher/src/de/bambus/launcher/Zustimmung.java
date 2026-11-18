package de.bambus.launcher;

import de.bambus.recht.Rechtstext;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.WindowConstants;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.RoundRectangle2D;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Das Rechtliche lesen und zustimmen – beim ersten Start und immer dann, wenn sich der Text geändert hat.
 *
 * Zustimmen geht erst, wenn man bis ganz nach unten gescrollt und den Haken gesetzt hat.
 * Ablehnen (oder Fenster schließen) beendet den Launcher. Über EINSTELLUNGEN → RECHTLICHES lässt sich
 * der Text jederzeit noch einmal lesen – dann nur mit SCHLIESSEN.
 */
final class Zustimmung extends JDialog {

	private static final int B = 700, H = 560, RAND = 28;
	/** Langer Text in normaler Schrift – die Pixelschrift ist für Überschriften da. */
	private static final java.awt.Font LESBAR = new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.PLAIN, 14);

	private final BambusLauncher l;
	private final boolean nurLesen;
	private final boolean englisch = Sprache.englisch();
	private final Text text = new Text();
	private final JScrollPane scroll;
	private boolean gelesen, haken, zugestimmt;
	private boolean ueberHaken, ueberJa, ueberNein;

	/** Hat man dem aktuellen Text schon zugestimmt? */
	static boolean liegtVor(BambusLauncher l) {
		return l.client.zahl("rechtlichZugestimmt", 0) >= Rechtstext.VERSION;
	}

	/** Beim Start: fragt, falls nötig. Gibt false zurück, wenn abgelehnt wurde. */
	static boolean pruefe(BambusLauncher l) {
		if (liegtVor(l)) return true;
		Zustimmung z = new Zustimmung(l, false);
		z.setVisible(true);   // wartet, bis das Fenster zu ist
		if (z.zugestimmt) {
			l.client.setze("rechtlichZugestimmt", Rechtstext.VERSION);
			l.client.setze("rechtlichDatum", LocalDate.now().toString());
			return true;
		}
		JOptionPane.showMessageDialog(l, Rechtstext.abgelehnt(Sprache.englisch()), Sprache.englisch() ? "Bamboo" : "Bambus",
				JOptionPane.INFORMATION_MESSAGE);
		return false;
	}

	/** Zum Nachlesen (EINSTELLUNGEN → RECHTLICHES). */
	static void zeigen(BambusLauncher l) {
		new Zustimmung(l, true).setVisible(true);
	}

	private Zustimmung(BambusLauncher l, boolean nurLesen) {
		super(l, Sprache.englisch() ? "Bamboo" : "Bambus", true);
		this.l = l;
		this.nurLesen = nurLesen;
		setUndecorated(true);
		setSize(B, H);
		setLocationRelativeTo(l);
		try { setShape(new RoundRectangle2D.Double(0, 0, B, H, 22, 22)); } catch (Exception ignoriert) { /* eckig */ }
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override public void windowClosing(WindowEvent e) { dispose(); }   // zu = nicht zugestimmt
		});

		JPanel p = new JPanel(null) {
			@Override protected void paintComponent(Graphics gr) { zeichneRahmen(NeonUI.glatt(gr)); }
		};
		p.setBackground(NeonUI.SEITENLEISTE);
		setContentPane(p);

		scroll = new JScrollPane(text, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setBounds(RAND, 92, B - 2 * RAND, H - 92 - 112);
		scroll.setBorder(null);
		scroll.setOpaque(false);
		scroll.getViewport().setOpaque(false);
		scroll.getVerticalScrollBar().setUnitIncrement(18);
		// dünne Neon-Leiste wie auf den anderen Seiten – etwas breiter, damit man sie gut greifen kann
		scroll.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
		scroll.getVerticalScrollBar().setOpaque(false);
		scroll.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
			@Override protected void configureScrollBarColors() {
				thumbColor = NeonUI.alpha(NeonUI.NEON, 150);
				trackColor = NeonUI.alpha(NeonUI.LINIE, 160);
			}
			@Override protected javax.swing.JButton createDecreaseButton(int o) { return leer(); }
			@Override protected javax.swing.JButton createIncreaseButton(int o) { return leer(); }
			private javax.swing.JButton leer() {
				javax.swing.JButton k = new javax.swing.JButton();
				k.setPreferredSize(new Dimension(0, 0));
				return k;
			}
		});
		SanftScroll.an(scroll);
		scroll.getVerticalScrollBar().addAdjustmentListener(e -> pruefeUnten());
		p.add(scroll);
		if (nurLesen) gelesen = true;

		MouseAdapter maus = new MouseAdapter() {
			@Override public void mouseMoved(MouseEvent e) {
				ueberHaken = hakenFlaeche(e.getX(), e.getY());
				ueberJa = jaFlaeche(e.getX(), e.getY());
				ueberNein = !nurLesen && neinFlaeche(e.getX(), e.getY());
				boolean hand = (ueberHaken && gelesen && !nurLesen) || (ueberJa && (nurLesen || haken)) || ueberNein;
				p.setCursor(Cursor.getPredefinedCursor(hand ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
				p.repaint();
			}

			@Override public void mouseClicked(MouseEvent e) {
				if (!nurLesen && hakenFlaeche(e.getX(), e.getY()) && gelesen) {
					haken = !haken;
				} else if (jaFlaeche(e.getX(), e.getY())) {
					if (nurLesen) {
						dispose();
					} else if (haken) {
						zugestimmt = true;
						dispose();
					}
				} else if (!nurLesen && neinFlaeche(e.getX(), e.getY())) {
					zugestimmt = false;
					dispose();
				}
				p.repaint();
			}
		};
		p.addMouseListener(maus);
		p.addMouseMotionListener(maus);
		javax.swing.SwingUtilities.invokeLater(this::pruefeUnten);   // passt der Text ganz rein, ist er gleich „gelesen“
	}

	/** Ganz unten angekommen? Dann darf man den Haken setzen. */
	private void pruefeUnten() {
		javax.swing.JScrollBar sb = scroll.getVerticalScrollBar();
		if (sb.getValue() + sb.getVisibleAmount() >= sb.getMaximum() - 12) {
			if (!gelesen) {
				gelesen = true;
				getContentPane().repaint();
			}
		}
	}

	// ------------------------------------------------------------ Klickflächen unten

	private boolean hakenFlaeche(int x, int y) {
		return x >= RAND && x < RAND + 420 && y >= H - 104 && y < H - 76;
	}

	private boolean jaFlaeche(int x, int y) {
		return x >= B - RAND - 170 && x < B - RAND && y >= H - 62 && y < H - 22;
	}

	private boolean neinFlaeche(int x, int y) {
		return x >= B - RAND - 340 && x < B - RAND - 180 && y >= H - 62 && y < H - 22;
	}

	private void zeichneRahmen(Graphics2D g) {
		g.setColor(NeonUI.SEITENLEISTE);
		g.fillRect(0, 0, B, H);
		NeonUI.leuchten(g, new RoundRectangle2D.Float(1, 1, B - 3, H - 3, 22, 22), NeonUI.NEON, 2);

		NeonUI.logo(g, RAND, 22, 40);
		g.setFont(NeonUI.titel(22));
		g.setColor(NeonUI.TEXT);
		g.drawString(NeonUI.passend(g, Rechtstext.titel(englisch), B - 2 * RAND - 40), RAND + 40, 46);
		g.setFont(NeonUI.normal(12));
		g.setColor(gelesen ? NeonUI.NEON : NeonUI.CYAN);
		String unter = nurLesen ? (englisch ? "Last updated: " : "Stand: ") + Rechtstext.STAND
				+ datumZustimmung()
				: gelesen ? (englisch ? "✓ Read to the end." : "✓ Bis zum Ende gelesen.") : Rechtstext.hinweisLesen(englisch);
		g.drawString(unter, RAND + 40, 68);

		// Trennlinien um den Text
		g.setColor(NeonUI.LINIE);
		g.drawLine(RAND, 86, B - RAND, 86);
		g.drawLine(RAND, H - 116, B - RAND, H - 116);

		if (!nurLesen) {
			// Haken
			int hx = RAND, hy = H - 100;
			Color f = !gelesen ? NeonUI.alpha(NeonUI.GRAU, 110) : haken ? NeonUI.NEON : ueberHaken ? NeonUI.TEXT : NeonUI.GRAU;
			g.setStroke(new BasicStroke(1.6f));
			g.setColor(f);
			g.drawRoundRect(hx, hy, 20, 20, 5, 5);
			if (haken) {
				g.setStroke(new BasicStroke(2.6f));
				g.drawLine(hx + 4, hy + 10, hx + 8, hy + 15);
				g.drawLine(hx + 8, hy + 15, hx + 16, hy + 5);
			}
			g.setFont(NeonUI.normal(13));
			g.drawString(Rechtstext.haken(englisch), hx + 32, hy + 15);

			knopf(g, B - RAND - 340, H - 62, 160, Rechtstext.ablehnen(englisch), NeonUI.PINK, true, ueberNein);
		}
		boolean jaAn = nurLesen || haken;
		knopf(g, B - RAND - 170, H - 62, 170,
				nurLesen ? (englisch ? "CLOSE" : "SCHLIESSEN") : Rechtstext.zustimmen(englisch),
				NeonUI.NEON, jaAn, ueberJa && jaAn);
	}

	private String datumZustimmung() {
		String d = l.client.text("rechtlichDatum", "");
		if (d.isEmpty()) return "";
		return (englisch ? "   ·   You agreed on " : "   ·   Zugestimmt am ") + d;
	}

	private static void knopf(Graphics2D g, int x, int y, int b, String text, Color farbe, boolean an, boolean ueber) {
		RoundRectangle2D form = new RoundRectangle2D.Float(x, y, b, 40, 10, 10);
		Color f = an ? farbe : NeonUI.alpha(NeonUI.GRAU, 100);
		g.setColor(ueber ? NeonUI.alpha(f, 46) : NeonUI.alpha(f, 18));
		g.fill(form);
		g.setStroke(new BasicStroke(ueber ? 2f : 1.4f));
		g.setColor(f);
		g.draw(form);
		g.setFont(NeonUI.mono(13));
		NeonUI.zentriert(g, text, x, y, b, 40);
	}

	// ------------------------------------------------------------ Der Text selbst

	/** Zeichnet die Abschnitte mit Zeilenumbruch; die Höhe ergibt sich aus der Breite. */
	private final class Text extends JComponent {
		private int letzteBreite = -1, hoehe = 400;

		Text() {
			setOpaque(false);
		}

		@Override
		public Dimension getPreferredSize() {
			int b = scroll == null ? B - 2 * RAND - 20 : scroll.getViewport().getWidth();
			if (b <= 0) b = B - 2 * RAND - 20;
			if (b != letzteBreite) {
				letzteBreite = b;
				java.awt.image.BufferedImage mess = new java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_ARGB);
				Graphics2D g = roh(mess.getGraphics());
				hoehe = zeichne(g, b, false) + 12;
				g.dispose();
			}
			return new Dimension(b, hoehe);
		}

		@Override
		protected void paintComponent(Graphics gr) {
			Graphics2D g = roh(gr);
			zeichne(g, getWidth(), true);
			g.dispose();
		}

		/** Gibt die benötigte Höhe zurück; mit malen = false wird nur gemessen. */
		private int zeichne(Graphics2D g, int breite, boolean malen) {
			int y = 8, rechts = breite - 18;
			for (String[] a : Rechtstext.abschnitte(englisch)) {
				g.setFont(NeonUI.mono(12));
				y += 16;
				if (malen) {
					g.setColor(NeonUI.NEON);
					g.drawString(a[0].toUpperCase(englisch ? java.util.Locale.ENGLISH : java.util.Locale.GERMAN), 0, y);
				}
				y += 10;
				g.setFont(LESBAR);
				FontMetrics fm = g.getFontMetrics();
				for (String absatz : a[1].split("\n")) {
					boolean punkt = absatz.startsWith("• ");
					int einzug = punkt ? 16 : 0;
					String rest = punkt ? absatz.substring(2) : absatz;
					List<String> zeilen = umbrich(fm, rest, rechts - einzug);
					for (int i = 0; i < zeilen.size(); i++) {
						y += fm.getHeight();
						if (malen) {
							if (punkt && i == 0) {
								g.setColor(NeonUI.CYAN);
								g.drawString("•", 2, y);
							}
							g.setColor(new Color(0xD5DEEA));
							g.drawString(zeilen.get(i), einzug, y);
						}
					}
					y += 4;
				}
				y += 8;
			}
			return y;
		}
	}

	/** Wie NeonUI.glatt, aber ohne Übersetzung – der Rechtstext liegt schon in beiden Sprachen vor. */
	private static Graphics2D roh(Graphics graphics) {
		Graphics2D g = (Graphics2D) graphics.create();
		g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		return g;
	}

	/**
	 * Zeilenumbruch nach Wörtern. Der Text ist schon in der richtigen Sprache – er wird beim Zeichnen
	 * nicht noch einmal übersetzt, deshalb hier mit den echten Breiten der Schrift gemessen.
	 */
	private static List<String> umbrich(FontMetrics fm, String text, int breite) {
		List<String> aus = new ArrayList<>();
		StringBuilder zeile = new StringBuilder();
		for (String wort : text.split(" ")) {
			String probe = zeile.length() == 0 ? wort : zeile + " " + wort;
			if (fm.stringWidth(probe) > breite && zeile.length() > 0) {
				aus.add(zeile.toString());
				zeile = new StringBuilder(wort);
			} else {
				zeile = new StringBuilder(probe);
			}
		}
		if (zeile.length() > 0) aus.add(zeile.toString());
		return aus;
	}
}
