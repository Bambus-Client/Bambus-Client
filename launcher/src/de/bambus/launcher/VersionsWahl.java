package de.bambus.launcher;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

/**
 * Fenster zum Wählen der Minecraft-Version eines Profils (und beim Anlegen auch des Namens):
 * eine Karte pro Version mit Datum und Hinweis, ob der Bambus Client darauf läuft. Kleine
 * Zwischenversionen (z. B. 26.1.1) stehen als Knöpfe auf der Karte ihrer Hauptversion.
 */
final class VersionsWahl extends JDialog {

	private static final int B = 760, KARTE_H = 196;
	/** Höhe des Fensters und Lage der Karten – ohne Namensfeld rücken die Karten nach oben. */
	private final int H, KARTE_Y;

	/** Erscheinungsdatum der Versionen (Mojang). */
	private static final Map<String, String> DATUM = new LinkedHashMap<>();

	static {
		DATUM.put("1.21.11", "Dez. 2025");
		DATUM.put("26.1", "März 2026");
		DATUM.put("26.1.1", "Apr. 2026");
		DATUM.put("26.1.2", "Apr. 2026");
		DATUM.put("26.2", "Juni 2026");
		DATUM.put("26.3", "Sep. 2026");
	}

	private final String titel, knopfText;
	private final JTextField name;
	private final List<Karte> karten = new ArrayList<>();
	private String gewaehlt;
	private boolean ok;

	private VersionsWahl(BambusLauncher l, String titel, String knopfText, String vorschlagName, String vorschlagVersion) {
		super(l, Sprache.t(titel), true);
		this.titel = titel;
		this.knopfText = knopfText;
		gewaehlt = Installer.kenntVersion(vorschlagVersion) ? vorschlagVersion : Installer.MC_VERSION;
		KARTE_Y = vorschlagName != null ? 196 : 130;
		H = KARTE_Y + 370;
		setUndecorated(true);
		setSize(B, H);
		setLocationRelativeTo(l);
		JPanel p = new JPanel(null) {
			@Override protected void paintComponent(Graphics gr) { zeichne(NeonUI.glatt(gr)); }
		};
		setContentPane(p);

		NeonUI.UmrissKnopf zu = new NeonUI.UmrissKnopf("✕", NeonUI.GRAU);
		zu.setBounds(B - 54, 16, 38, 32);
		zu.addActionListener(e -> dispose());
		p.add(zu);

		if (vorschlagName != null) {
			name = new JTextField(vorschlagName);
			name.setFont(NeonUI.normal(15));
			name.setForeground(NeonUI.TEXT);
			name.setCaretColor(NeonUI.NEON);
			name.setBackground(NeonUI.KARTE);
			name.setSelectionColor(NeonUI.alpha(NeonUI.NEON, 90));
			name.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(NeonUI.LINIE),
					BorderFactory.createEmptyBorder(0, 12, 0, 12)));
			name.setBounds(40, 120, 340, 38);
			name.selectAll();
			name.addActionListener(e -> fertig());
			p.add(name);
		} else {
			name = null;
		}

		// eine Karte pro Hauptversion, neueste links
		List<List<String>> linien = linien();
		int abstand = 14, kb = (B - 80 - abstand * (linien.size() - 1)) / Math.max(1, linien.size());
		for (int i = 0; i < linien.size(); i++) {
			Karte k = new Karte(linien.get(i), i == 0);
			k.setBounds(40 + i * (kb + abstand), KARTE_Y, kb, KARTE_H);
			karten.add(k);
			p.add(k);
		}

		NeonUI.SpielKnopf weiter = new NeonUI.SpielKnopf(knopfText);
		weiter.setFont(NeonUI.titel(15));
		weiter.setBounds(40, H - 80, 300, 52);
		weiter.addActionListener(e -> fertig());
		p.add(weiter);
		NeonUI.UmrissKnopf abbrechen = new NeonUI.UmrissKnopf("ABBRECHEN", NeonUI.GRAU);
		abbrechen.setBounds(356, H - 73, 150, 38);
		abbrechen.addActionListener(e -> dispose());
		p.add(abbrechen);

		getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
		getRootPane().registerKeyboardAction(e -> fertig(), KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
	}

	/**
	 * Fragt Name und Version für ein neues Profil. Gibt {name, version} zurück, oder null bei Abbruch.
	 */
	static String[] frageProfil(BambusLauncher l, String titel, String vorschlagName, String vorschlagVersion) {
		VersionsWahl w = new VersionsWahl(l, titel, "ERSTELLEN", vorschlagName, vorschlagVersion);
		w.setVisible(true);
		if (!w.ok) return null;
		String n = w.name.getText() == null ? "" : w.name.getText().trim();
		return n.isEmpty() ? null : new String[]{n, w.gewaehlt};
	}

	/** Fragt nur die Version (Profil umstellen). null = abgebrochen. */
	static String frageVersion(BambusLauncher l, String aktuell) {
		VersionsWahl w = new VersionsWahl(l, "Minecraft-Version", "ÜBERNEHMEN", null, aktuell);
		w.setVisible(true);
		return w.ok ? w.gewaehlt : null;
	}

	private void fertig() {
		if (name != null && name.getText().trim().isEmpty()) {
			name.requestFocusInWindow();
			return;
		}
		ok = true;
		dispose();
	}

	/** Versionen nach Hauptversion gruppiert (26.1, 26.1.1, 26.1.2 → eine Karte), neueste zuerst. */
	static List<List<String>> linien() {
		Map<String, List<String>> m = new LinkedHashMap<>();
		for (int i = Installer.VERSIONEN.length - 1; i >= 0; i--) {
			String v = Installer.VERSIONEN[i];
			List<String> l = m.get(Installer.modLinie(v));
			if (l == null) m.put(Installer.modLinie(v), l = new ArrayList<>());
			l.add(0, v);   // innerhalb der Karte aufsteigend: 26.1, 26.1.1, 26.1.2
		}
		return new ArrayList<>(m.values());
	}

	private void zeichne(Graphics2D g) {
		g.setColor(NeonUI.HG);
		g.fillRect(0, 0, B, H);
		NeonUI.gitter(g, 0, 0, B, H, 24, NeonUI.alpha(NeonUI.NEON, 8));
		NeonUI.leuchten(g, new RoundRectangle2D.Float(1, 1, B - 3, H - 3, 18, 18), NeonUI.NEON, 2);
		g.setFont(NeonUI.mono(12));
		g.setColor(NeonUI.NEON);
		g.drawString("● " + Sprache.t(titel).toUpperCase(), 40, 44);
		g.setFont(NeonUI.titel(26));
		NeonUI.leuchtText(g, Sprache.t(name != null ? "NAME & VERSION" : "VERSION WÄHLEN"), 40, 80, NeonUI.TEXT, 1);
		g.setFont(NeonUI.mono(11));
		g.setColor(NeonUI.GRAU);
		if (name != null) g.drawString(Sprache.t("NAME DES PROFILS"), 40, 112);
		g.drawString(Sprache.t("MINECRAFT-VERSION"), 40, KARTE_Y - 12);

		boolean bambus = Installer.bambusFuer(gewaehlt);
		g.setFont(NeonUI.normal(13));
		g.setColor(bambus ? NeonUI.NEON : NeonUI.CYAN);
		g.drawString(Sprache.t(bambus ? "Minecraft " + gewaehlt + " mit Bambus Client, Fabric und deinen Mods."
				: "Minecraft " + gewaehlt + " mit Fabric und deinen Mods – ohne Bambus Client."), 40, KARTE_Y + KARTE_H + 32);
		g.setColor(NeonUI.GRAU);
		g.drawString(Sprache.t("Jede Version hat eigene Mods und eigene Welten – alte Welten bleiben sicher."), 40, KARTE_Y + KARTE_H + 56);
		g.dispose();
	}

	/** Karte einer Hauptversion; Zwischenversionen als kleine Knöpfe unten. */
	private final class Karte extends JComponent {
		private final List<String> versionen;
		private final boolean neueste;
		private final List<Rectangle> chips = new ArrayList<>();
		private boolean ueber;

		Karte(List<String> versionen, boolean neueste) {
			this.versionen = versionen;
			this.neueste = neueste;
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			MouseAdapter m = new MouseAdapter() {
				@Override public void mouseEntered(MouseEvent e) { ueber = true; repaint(); }
				@Override public void mouseExited(MouseEvent e) { ueber = false; repaint(); }
				@Override public void mousePressed(MouseEvent e) {
					String v = versionen.get(versionen.size() - 1);   // Standard: neueste Zwischenversion
					if (versionen.contains(gewaehlt)) v = gewaehlt;
					for (int i = 0; i < chips.size(); i++) if (chips.get(i).contains(e.getPoint())) v = versionen.get(i);
					gewaehlt = v;
					VersionsWahl.this.repaint();
				}
				@Override public void mouseClicked(MouseEvent e) { if (e.getClickCount() == 2) fertig(); }
			};
			addMouseListener(m);
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			Graphics2D g = NeonUI.glatt(graphics);
			int b = getWidth(), h = getHeight();
			boolean aktiv = versionen.contains(gewaehlt);
			boolean bambus = Installer.bambusFuer(versionen.get(0));
			Color farbe = bambus ? NeonUI.NEON : NeonUI.CYAN;
			g.setColor(aktiv ? NeonUI.alpha(farbe, 28) : ueber ? NeonUI.KARTE_HOVER : NeonUI.KARTE);
			g.fillRoundRect(0, 0, b - 1, h - 1, 14, 14);
			if (aktiv) {
				g.setStroke(new BasicStroke(2f));
				g.setColor(farbe);
				g.drawRoundRect(1, 1, b - 3, h - 3, 14, 14);
				NeonUI.ecken(g, 0, 0, b, h, 12, farbe);
			} else {
				g.setColor(ueber ? NeonUI.alpha(farbe, 120) : NeonUI.LINIE);
				g.drawRoundRect(0, 0, b - 1, h - 1, 14, 14);
			}

			// Kopf: Abzeichen „NEUESTE“
			if (neueste) {
				g.setFont(NeonUI.mono(9));
				String t = Sprache.t("NEUESTE");
				int tb = g.getFontMetrics().stringWidth(t) + 12;
				g.setColor(NeonUI.alpha(NeonUI.PINK, 50));
				g.fillRoundRect(b - tb - 10, 12, tb, 18, 9, 9);
				g.setColor(NeonUI.PINK);
				g.drawString(t, b - tb - 4, 25);
			}

			String haupt = Installer.modLinie(versionen.get(0));
			g.setFont(NeonUI.passendeSchrift(g, NeonUI.titel(30), haupt, b - 28));
			NeonUI.leuchtText(g, haupt, 14, 62, aktiv ? NeonUI.TEXT : NeonUI.alpha(NeonUI.TEXT, 210), aktiv ? 1 : 0);
			g.setFont(NeonUI.normal(12));
			g.setColor(NeonUI.GRAU);
			String datum = DATUM.get(versionen.get(0));
			g.drawString(datum == null ? "" : datum, 14, 84);

			// Bambus Client läuft darauf?
			g.setFont(NeonUI.mono(10));
			String marke = bambus ? "✓ BAMBUS CLIENT" : "NUR FABRIC";
			marke = Sprache.t(marke);
			int mb = g.getFontMetrics().stringWidth(marke) + 14;
			g.setColor(NeonUI.alpha(farbe, 40));
			g.fillRoundRect(14, 98, mb, 22, 11, 11);
			g.setColor(farbe);
			g.drawString(marke, 21, 113);

			// Zwischenversionen als Knöpfe
			chips.clear();
			if (versionen.size() > 1) {
				g.setFont(NeonUI.mono(10));
				int x = 14, y = 132;
				for (String v : versionen) {
					int cb = g.getFontMetrics().stringWidth(v) + 14;
					if (x + cb > b - 10) { x = 14; y += 30; }
					Rectangle r = new Rectangle(x, y, cb, 24);
					chips.add(r);
					boolean an = v.equals(gewaehlt);
					g.setColor(an ? farbe : NeonUI.alpha(NeonUI.TEXT, 18));
					g.fillRoundRect(r.x, r.y, r.width, r.height, 10, 10);
					g.setColor(an ? NeonUI.DUNKEL : NeonUI.TEXT);
					g.drawString(v, r.x + 7, r.y + 16);
					x += cb + 6;
				}
			} else {
				g.setFont(NeonUI.normal(11));
				g.setColor(NeonUI.alpha(NeonUI.GRAU, 200));
				String text = Installer.MC_VERSION.equals(versionen.get(0)) ? "Bewährt & stabil" : neueste ? "Alle neuen Features" : "";
				g.drawString(Sprache.t(text), 14, h - 22);
			}
			g.dispose();
		}
	}
}
