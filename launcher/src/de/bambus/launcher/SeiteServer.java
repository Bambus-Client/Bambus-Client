package de.bambus.launcher;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** SERVER: Lieblingsserver mit Live-Status (Spieler, Ping, Beschreibung). Landen im Spiel in der Mehrspieler-Liste. */
final class SeiteServer extends JPanel {

	private static final int X = 40, B = 696, KARTE_H = 78;

	private final BambusLauncher l;
	private final JTextField feld = new JTextField() {
		@Override
		protected void paintComponent(Graphics gr) {
			super.paintComponent(gr);
			if (getText().isEmpty() && !hasFocus()) {   // Platzhalter
				Graphics2D g = NeonUI.glatt(gr);
				g.setFont(NeonUI.normal(13));
				g.setColor(NeonUI.alpha(NeonUI.GRAU, 170));
				g.drawString("Serveradresse, z. B. play.beispiel.de", 11, 23);
				g.dispose();
			}
		}
	};
	private final JPanel liste = new JPanel(null);
	private final List<ServerKarte> karten = new ArrayList<>();
	private final ExecutorService pool = Executors.newFixedThreadPool(4, r -> {
		Thread t = new Thread(r, "Server-Status");
		t.setDaemon(true);
		return t;
	});
	private long zuletzt;
	private String hinweis = "";

	SeiteServer(BambusLauncher l) {
		this.l = l;
		setOpaque(false);
		setLayout(null);

		feld.setBounds(X, 104, 380, 36);
		feld.setBackground(NeonUI.KARTE);
		feld.setForeground(NeonUI.TEXT);
		feld.setCaretColor(NeonUI.NEON);
		feld.setFont(NeonUI.normal(13));
		feld.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(NeonUI.LINIE, 1, true), BorderFactory.createEmptyBorder(4, 10, 4, 10)));
		feld.addActionListener(e -> hinzufuegen());
		add(feld);
		NeonUI.UmrissKnopf neu = new NeonUI.UmrissKnopf("+ HINZUFÜGEN", NeonUI.NEON);
		neu.setBounds(X + 392, 104, 146, 36);
		neu.addActionListener(e -> hinzufuegen());
		add(neu);
		NeonUI.UmrissKnopf auf = new NeonUI.UmrissKnopf("⟳ AKTUALISIEREN", NeonUI.CYAN);
		auf.setBounds(X + 550, 104, 146, 36);
		auf.addActionListener(e -> abfragen());
		add(auf);

		liste.setOpaque(false);
		JScrollPane sp = SeiteMods.scroll(liste, X - 4, 160, B + 14, 400);
		add(sp);
		neuAufbauen();

		// alle 30 Sekunden neu fragen, solange die Seite offen ist
		new Timer(30_000, e -> { if (isShowing()) abfragen(); }).start();
	}

	/** Beim Öffnen der Seite. */
	void aktualisiere() {
		if (System.currentTimeMillis() - zuletzt > 10_000) abfragen();
	}

	private void hinzufuegen() {
		String a = feld.getText().trim().replace(" ", "");
		if (a.isEmpty()) return;
		if (!a.matches("[A-Za-z0-9._:\\-\\[\\]]+")) {
			hinweis = "Das sieht nicht wie eine Serveradresse aus (z. B. play.beispiel.de).";
			repaint();
			return;
		}
		List<String> fav = l.client.serverFavoriten();
		for (String f : fav) {
			if (adresse(f).equalsIgnoreCase(a)) {
				hinweis = a + " ist schon in deiner Liste.";
				repaint();
				return;
			}
		}
		fav.add(name(a) + "|" + a);
		l.client.setzeServerFavoriten(fav);
		feld.setText("");
		hinweis = a + " hinzugefügt – im Spiel unter Mehrspieler.";
		neuAufbauen();
		abfragen();
	}

	private void entfernen(String eintrag) {
		List<String> fav = l.client.serverFavoriten();
		fav.remove(eintrag);
		l.client.setzeServerFavoriten(fav);
		hinweis = adresse(eintrag) + " entfernt (im Spiel bleibt er in der Liste, bis du ihn dort löschst).";
		neuAufbauen();
	}

	private static String adresse(String eintrag) {
		int i = eintrag.indexOf('|');
		return i < 0 ? eintrag : eintrag.substring(i + 1);
	}

	/** Aus „play.gommehd.net“ wird „Gommehd“. */
	private static String name(String adresse) {
		String[] t = adresse.split(":")[0].split("\\.");
		String n = t.length >= 2 ? t[t.length - 2] : t[0];
		return n.isEmpty() ? adresse : Character.toUpperCase(n.charAt(0)) + n.substring(1);
	}

	private void neuAufbauen() {
		liste.removeAll();
		karten.clear();
		List<String> fav = l.client.serverFavoriten();
		for (int i = 0; i < fav.size(); i++) {
			ServerKarte k = new ServerKarte(fav.get(i));
			k.setBounds(4, i * (KARTE_H + 10), B, KARTE_H);
			karten.add(k);
			liste.add(k);
		}
		liste.setPreferredSize(new Dimension(B, Math.max(1, fav.size() * (KARTE_H + 10))));
		liste.revalidate();
		repaint();
	}

	private void abfragen() {
		zuletzt = System.currentTimeMillis();
		for (ServerKarte k : karten) {
			k.laedt = true;
			k.repaint();
			pool.submit(() -> {
				ServerStatus.Ergebnis e = ServerStatus.frage(adresse(k.eintrag));
				SwingUtilities.invokeLater(() -> { k.status = e; k.laedt = false; k.repaint(); });
			});
		}
	}

	@Override
	protected void paintComponent(Graphics graphics) {
		Graphics2D g = NeonUI.glatt(graphics);
		g.setFont(NeonUI.titel(30));
		NeonUI.leuchtText(g, "SERVER", X, 62, NeonUI.TEXT, 1);
		g.setFont(NeonUI.normal(13));
		g.setColor(NeonUI.GRAU);
		g.drawString("Deine Lieblingsserver mit Live-Status. Im Spiel findest du sie unter Mehrspieler.", X, 86);
		g.setFont(NeonUI.normal(11));
		g.setColor(hinweis.startsWith("Das sieht") ? new Color(0xFFB0C4) : NeonUI.NEON);
		g.drawString(hinweis, X, 154);
		g.setColor(NeonUI.alpha(NeonUI.GRAU, 180));
		g.drawString("Abfrage wie in der Mehrspieler-Liste: Es wird nur der öffentliche Status gelesen, ohne Anmeldung.", X, 586);
		g.dispose();
	}

	// ============================================================ Karte

	private final class ServerKarte extends JComponent {
		final String eintrag;
		volatile ServerStatus.Ergebnis status;
		volatile boolean laedt;
		private boolean ueber, ueberX;

		ServerKarte(String eintrag) {
			this.eintrag = eintrag;
			addMouseListener(new MouseAdapter() {
				@Override public void mouseEntered(MouseEvent e) { ueber = true; repaint(); }
				@Override public void mouseExited(MouseEvent e) { ueber = false; ueberX = false; repaint(); }
				@Override public void mouseClicked(MouseEvent e) { if (aufX(e.getX(), e.getY())) entfernen(eintrag); }
			});
			addMouseMotionListener(new MouseAdapter() {
				@Override public void mouseMoved(MouseEvent e) {
					boolean x = aufX(e.getX(), e.getY());
					if (x != ueberX) {
						ueberX = x;
						setCursor(Cursor.getPredefinedCursor(x ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
						repaint();
					}
				}
			});
		}

		private boolean aufX(int x, int y) {
			return x > getWidth() - 34 && y < 30;
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			Graphics2D g = NeonUI.glatt(graphics);
			int b = getWidth(), h = getHeight();
			ServerStatus.Ergebnis s = status;
			boolean online = s != null && s.online;
			RoundRectangle2D form = new RoundRectangle2D.Float(1, 1, b - 3, h - 3, 14, 14);
			g.setColor(ueber ? NeonUI.KARTE_HOVER : NeonUI.KARTE);
			g.fill(form);
			g.setStroke(new BasicStroke(1.2f));
			g.setColor(online ? NeonUI.alpha(NeonUI.NEON, ueber ? 160 : 90) : NeonUI.LINIE);
			g.draw(form);

			// Symbol des Servers (oder Platzhalter)
			int sy = 13, sg = 52;
			if (s != null && s.symbol != null) {
				g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
				g.drawImage(s.symbol, 14, sy, sg, sg, null);
			} else {
				g.setColor(NeonUI.alpha(NeonUI.NEON, 24));
				g.fillRoundRect(14, sy, sg, sg, 10, 10);
				Symbole.zeichne(g, "server", 14 + 10, sy + 10, 4, NeonUI.alpha(NeonUI.NEON, 160));
			}

			String name = eintrag.contains("|") ? eintrag.substring(0, eintrag.indexOf('|')) : adresse(eintrag);
			g.setFont(NeonUI.titel(16));
			g.setColor(NeonUI.TEXT);
			g.drawString(name, 82, 30);
			int nb = g.getFontMetrics().stringWidth(name);
			g.setFont(NeonUI.mono(11));
			g.setColor(NeonUI.GRAU);
			g.drawString(adresse(eintrag), 82 + nb + 12, 29);

			g.setFont(NeonUI.normal(12));
			String zeile;
			if (laedt && s == null) {
				zeile = "Frage Status ab …";
				g.setColor(NeonUI.GRAU);
			} else if (s == null) {
				zeile = "";
			} else if (!s.online) {
				zeile = "Offline · " + s.fehler;
				g.setColor(new Color(0xFFB0C4));
			} else {
				zeile = s.motd.isEmpty() ? "Online" : s.motd;
				g.setColor(new Color(0xC8D2DE));
			}
			g.drawString(kuerzen(g.getFontMetrics(), zeile, b - 82 - 220), 82, 52);
			if (s != null && s.online && !s.version.isEmpty()) {
				g.setFont(NeonUI.mono(10));
				g.setColor(NeonUI.alpha(NeonUI.GRAU, 200));
				g.drawString(kuerzen(g.getFontMetrics(), s.version, 300), 82, 68);
			}

			// rechts: Spieler und Ping
			int rx = b - 200;
			if (s != null && s.online) {
				g.setFont(NeonUI.titel(15));
				g.setColor(NeonUI.NEON);
				String sp = s.spieler >= 0 ? String.format("%,d", s.spieler).replace(',', '.') : "?";
				g.drawString(sp, rx, 46);
				int sb = g.getFontMetrics().stringWidth(sp);
				g.setFont(NeonUI.normal(11));
				g.setColor(NeonUI.GRAU);
				g.drawString(s.maxSpieler > 0 ? "/ " + String.format("%,d", s.maxSpieler).replace(',', '.') + " online" : "online", rx + sb + 6, 46);
			}
			long ping = s != null && s.online ? s.ping : -1;
			Color pf = ping < 0 ? NeonUI.GRAU : ping < 80 ? NeonUI.NEON : ping < 180 ? new Color(0xFFD54A) : new Color(0xFF4D6D);
			int stufe = ping < 0 ? 0 : ping < 50 ? 4 : ping < 100 ? 3 : ping < 180 ? 2 : 1;
			for (int i = 0; i < 4; i++) {
				int bh = 5 + i * 4;
				g.setColor(i < stufe ? pf : new Color(0x2A3445));
				g.fillRect(b - 64 + i * 6, 52 - bh, 4, bh);
			}
			g.setFont(NeonUI.mono(10));
			g.setColor(pf);
			String ps = laedt ? "…" : ping >= 0 ? ping + " ms" : "–";
			g.drawString(ps, b - 64, 66);

			// Entfernen
			g.setFont(NeonUI.normal(13));
			g.setColor(ueberX ? new Color(0xFF4D6D) : NeonUI.alpha(NeonUI.GRAU, ueber ? 220 : 90));
			g.drawString("✕", b - 24, 22);
			g.dispose();
		}
	}

	private static String kuerzen(FontMetrics fm, String text, int max) {
		text = Sprache.t(text);
		if (fm.stringWidth(text) <= max) return text;
		while (text.length() > 2 && fm.stringWidth(text + "…") > max) text = text.substring(0, text.length() - 1);
		return text + "…";
	}
}
