package de.bambus.launcher;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Cursor;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.nio.file.Files;
import java.nio.file.Path;

/** Arbeitsspeicher, Ordner und Hilfe. */
final class SeiteEinstellungen extends JPanel {

	private final java.util.List<NeonUI.UmrissKnopf> hgKnoepfe = new java.util.ArrayList<>();

	private static final int X = 40;
	private static final int[] RAM = {2, 3, 4, 6, 8, 12, 16};

	private final BambusLauncher l;

	SeiteEinstellungen(BambusLauncher l) {
		this.l = l;
		setOpaque(false);
		setLayout(null);

		RamWahl ram = new RamWahl();
		ram.setBounds(X, 128, 540, 44);
		add(ram);

		Path[] ordner = {l.installer.mcOrdner, l.installer.spielOrdner, l.installer.modsOrdner};
		for (int i = 0; i < ordner.length; i++) {
			final Path p = ordner[i];
			NeonUI.UmrissKnopf k = new NeonUI.UmrissKnopf("ÖFFNEN", NeonUI.CYAN);
			k.setBounds(X + 560, 230 + i * 44, 120, 38);
			k.addActionListener(e -> l.oeffne(p));
			add(k);
		}

		if (BambusLauncher.istWindows()) {
			NeonUI.UmrissKnopf desktop = new NeonUI.UmrissKnopf("AUF DEN DESKTOP", NeonUI.NEON);
			desktop.setFont(NeonUI.mono(10));
			desktop.setBounds(X, 494, 170, 30);
			desktop.addActionListener(e -> verknuepfung("Desktop", "auf dem Desktop"));
			add(desktop);
			NeonUI.UmrissKnopf startmenue = new NeonUI.UmrissKnopf("INS STARTMENÜ", NeonUI.NEON);
			startmenue.setFont(NeonUI.mono(10));
			startmenue.setBounds(X + 180, 494, 170, 30);
			startmenue.addActionListener(e -> verknuepfung("Programs", "im Startmenü"));
			add(startmenue);
		}

		NeonUI.UmrissKnopf zuruecksetzen = new NeonUI.UmrissKnopf("CLIENT-EINSTELLUNGEN ZURÜCKSETZEN", NeonUI.PINK);
		// Sprache + Willkommens-Seite (oben rechts)
		NeonUI.UmrissKnopf de = new NeonUI.UmrissKnopf("DEUTSCH", Sprache.englisch() ? NeonUI.GRAU : NeonUI.NEON);
		NeonUI.UmrissKnopf en = new NeonUI.UmrissKnopf("ENGLISH", Sprache.englisch() ? NeonUI.NEON : NeonUI.GRAU);
		de.setBounds(X + 340, 36, 104, 32);
		en.setBounds(X + 452, 36, 104, 32);
		de.addActionListener(e -> { Sprache.setze(Sprache.DE); de.setzeFarbe(NeonUI.NEON); en.setzeFarbe(NeonUI.GRAU); l.speichereLauncherEinstellungen(); l.repaint(); });
		en.addActionListener(e -> { Sprache.setze(Sprache.EN); en.setzeFarbe(NeonUI.NEON); de.setzeFarbe(NeonUI.GRAU); l.speichereLauncherEinstellungen(); l.repaint(); });
		add(de);
		add(en);
		NeonUI.UmrissKnopf willkommen = new NeonUI.UmrissKnopf("WILLKOMMENS-SEITE", NeonUI.CYAN);
		willkommen.setFont(NeonUI.mono(10));
		willkommen.setBounds(X + 564, 36, 132, 32);
		willkommen.addActionListener(e -> new Willkommen(l).setVisible(true));
		add(willkommen);

		// Hintergrund-Auswahl
		for (int i = 0; i < Hintergruende.ARTEN.length; i++) {
			final String id = Hintergruende.ARTEN[i][0];
			NeonUI.UmrissKnopf k = new NeonUI.UmrissKnopf(Hintergruende.ARTEN[i][1], id.equals(l.hintergrund) ? NeonUI.NEON : NeonUI.GRAU);
			k.setBounds(X + i * 137, 388, 129, 34);
			k.addActionListener(e -> {
				l.hintergrund = id;
				l.client.setze("menueHintergrund", id);   // gleicher Hintergrund im Bambus-Hauptmenü
				l.speichereLauncherEinstellungen();
				for (NeonUI.UmrissKnopf o : hgKnoepfe) o.setzeFarbe(NeonUI.GRAU);
				((NeonUI.UmrissKnopf) e.getSource()).setzeFarbe(NeonUI.NEON);
				l.repaint();
			});
			hgKnoepfe.add(k);
			add(k);
		}
		// Farbthema – gleiche Themen und gleiche Einstellung wie im Spiel
		Farbwahl farben = new Farbwahl();
		farben.setBounds(X, 452, 696, 36);
		add(farben);
		zuruecksetzen.setBounds(X, 532, 330, 40);
		zuruecksetzen.addActionListener(e -> zuruecksetzen());
		add(zuruecksetzen);

		// Updates
		NeonUI.UmrissKnopf auto = new NeonUI.UmrissKnopf("", NeonUI.NEON);
		auto.setText(l.autoUpdate ? "AUTO-UPDATE: AN" : "AUTO-UPDATE: AUS");
		auto.setBounds(X + 344, 532, 180, 40);
		auto.addActionListener(e -> {
			l.autoUpdate = !l.autoUpdate;
			l.speichereLauncherEinstellungen();
			auto.setText(l.autoUpdate ? "AUTO-UPDATE: AN" : "AUTO-UPDATE: AUS");
		});
		auto.setVisible(Updater.aktiv());
		add(auto);
		NeonUI.UmrissKnopf jetzt = new NeonUI.UmrissKnopf("JETZT PRÜFEN", NeonUI.CYAN);
		jetzt.setBounds(X + 536, 532, 150, 40);
		jetzt.addActionListener(e -> l.pruefeUpdates(true));
		jetzt.setVisible(Updater.aktiv());
		add(jetzt);
	}

	private void verknuepfung(String ordner, String wo) {
		try {
			Installer.erstelleVerknuepfung(ordner);
			JOptionPane.showMessageDialog(this, Sprache.t("Fertig! Der Bambus Launcher liegt jetzt " + wo + "."), Sprache.t("Verknüpfung"), JOptionPane.INFORMATION_MESSAGE);
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(this, Sprache.t("Das hat nicht geklappt:\n" + ex.getMessage()), Sprache.t("Verknüpfung"), JOptionPane.ERROR_MESSAGE);
		}
	}

	private void zuruecksetzen() {
		int antwort = JOptionPane.showConfirmDialog(this, Sprache.t("Alle Bambus-Einstellungen (Module, HUD-Positionen, Kosmetik) zurücksetzen?"), Sprache.t("Zurücksetzen"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
		if (antwort == JOptionPane.YES_OPTION) {
			try {
				Files.deleteIfExists(l.installer.spielOrdner.resolve("config").resolve("bambus.json"));
			} catch (Exception ignoriert) {
				// egal
			}
			l.client.laden();
			JOptionPane.showMessageDialog(this, Sprache.t("Zurückgesetzt. Starte den Launcher neu, damit alle Schalter stimmen."), Sprache.t("Zurücksetzen"), JOptionPane.INFORMATION_MESSAGE);
		}
	}

	@Override
	protected void paintComponent(Graphics graphics) {
		Graphics2D g = NeonUI.glatt(graphics);
		g.setFont(NeonUI.titel(30));
		NeonUI.leuchtText(g, "EINSTELLUNGEN", X, 62, NeonUI.TEXT, 1);

		// Update-Zeile ganz unten
		g.setFont(NeonUI.normal(11));
		g.setColor(NeonUI.GRAU);
		String st = Updater.aktiv()
				? "Version " + BambusLauncher.VERSION + (l.updateStatus.isEmpty() ? "" : "  ·  " + l.updateStatus)
				: "Version " + BambusLauncher.VERSION + "  ·  Automatische Updates gibt es, sobald der Client über GitHub gebaut wird.";
		g.drawString(st, X, 592);

		ueberschrift(g, "ARBEITSSPEICHER", 112);
		g.setFont(NeonUI.normal(12));
		g.setColor(NeonUI.GRAU);
		g.drawString("4–6 GB reichen für die meisten Computer. Nicht mehr als die Hälfte deines RAMs wählen.", X, 190);

		ueberschrift(g, "ORDNER", 216);
		String[] namen = {"Minecraft", "Bambus-Spielordner", "Mods"};
		Path[] pfade = {l.installer.mcOrdner, l.installer.spielOrdner, l.installer.modsOrdner};
		for (int i = 0; i < namen.length; i++) {
			int y = 230 + i * 44;
			RoundRectangle2D form = new RoundRectangle2D.Float(X, y, 548, 38, 10, 10);
			g.setColor(NeonUI.KARTE);
			g.fill(form);
			g.setColor(NeonUI.LINIE);
			g.setStroke(new BasicStroke(1.2f));
			g.draw(form);
			g.setFont(NeonUI.mono(10));
			g.setColor(NeonUI.NEON);
			g.drawString(namen[i].toUpperCase(), X + 14, y + 23);
			g.setFont(NeonUI.normal(12));
			g.setColor(NeonUI.TEXT);
			g.drawString(kuerzen(g.getFontMetrics(), pfade[i].toString(), 370), X + 168, y + 24);
		}

		ueberschrift(g, "HINTERGRUND", 376);
		ueberschrift(g, "FARBE  –  GILT AUCH IM SPIEL", 442);
		if (BambusLauncher.istWindows()) {
			g.setFont(NeonUI.normal(11));
			g.setColor(NeonUI.GRAU);
			g.drawString("Taskleiste: Rechtsklick aufs Symbol → „Anheften“", X + 362, 513);
		}
		g.dispose();
	}

	/** Sieben Farbfelder: Neon, Ozean, Sakura, Gold, Lila, Eis, RGB. */
	private final class Farbwahl extends javax.swing.JComponent {
		private static final int FB = 92, ABSTAND = 8;
		private int ueber = -1;

		Farbwahl() {
			setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
			java.awt.event.MouseAdapter m = new java.awt.event.MouseAdapter() {
				@Override public void mouseMoved(java.awt.event.MouseEvent e) { int u = bei(e.getX()); if (u != ueber) { ueber = u; repaint(); } }
				@Override public void mouseExited(java.awt.event.MouseEvent e) { ueber = -1; repaint(); }
				@Override public void mouseClicked(java.awt.event.MouseEvent e) {
					int i = bei(e.getX());
					if (i < 0) return;
					l.client.setze("thema", i);
					NeonUI.themaAnwenden(i);
					l.repaint();
				}
			};
			addMouseListener(m);
			addMouseMotionListener(m);
		}

		private int bei(int x) {
			int i = x / (FB + ABSTAND);
			return i >= 0 && i < NeonUI.THEMEN.length && x - i * (FB + ABSTAND) < FB ? i : -1;
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			Graphics2D g = NeonUI.glatt(graphics);
			int h = getHeight();
			for (int i = 0; i < NeonUI.THEMEN.length; i++) {
				int x = i * (FB + ABSTAND);
				boolean aktiv = i == NeonUI.thema(), rgb = NeonUI.THEMEN[i].equals("RGB");
				java.awt.Color c = rgb ? java.awt.Color.getHSBColor((System.currentTimeMillis() % 4000L) / 4000f, 0.75f, 1f) : NeonUI.themaFarbe(i);
				g.setColor(i == ueber ? NeonUI.KARTE_HOVER : NeonUI.KARTE);
				g.fillRect(x, 1, FB, h - 3);
				g.setStroke(new BasicStroke(aktiv ? 2f : 1.2f));
				g.setColor(aktiv ? c : NeonUI.LINIE);
				g.drawRect(x, 1, FB, h - 3);
				if (aktiv) NeonUI.ecken(g, x - 2, -1, FB + 4, h + 1, 6, c);
				g.setColor(c);
				g.fillRect(x + 9, 10, 14, h - 21);
				g.setFont(NeonUI.mono(10));
				g.setColor(aktiv ? NeonUI.TEXT : NeonUI.GRAU);
				g.drawString(NeonUI.THEMEN[i], x + 31, h / 2 + 4);
			}
			g.dispose();
		}
	}

	private static String kuerzen(FontMetrics fm, String text, int max) {
		if (fm.stringWidth(text) <= max) {
			return text;
		}
		while (text.length() > 4 && fm.stringWidth("…" + text) > max) {
			text = text.substring(1);
		}
		return "…" + text;
	}

	private static void ueberschrift(Graphics2D g, String text, int y) {
		g.setFont(NeonUI.mono(12));
		g.setColor(NeonUI.NEON);
		g.drawString(text, X, y);
		int tb = g.getFontMetrics().stringWidth(text);
		g.setColor(NeonUI.LINIE);
		g.fillRect(X + tb + 12, y - 4, 180, 1);
	}

	/** Pillen zur Auswahl des Arbeitsspeichers. */
	private final class RamWahl extends JComponent {
		private int ueber = -1;

		RamWahl() {
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			MouseAdapter m = new MouseAdapter() {
				@Override public void mouseMoved(MouseEvent e) {
					int neu = index(e.getX());
					if (neu != ueber) {
						ueber = neu;
						repaint();
					}
				}
				@Override public void mouseExited(MouseEvent e) { ueber = -1; repaint(); }
				@Override public void mouseClicked(MouseEvent e) {
					int i = index(e.getX());
					if (i >= 0) {
						l.ramGb = RAM[i];
						l.speichereLauncherEinstellungen();
						repaint();
						l.start.repaint();
					}
				}
			};
			addMouseListener(m);
			addMouseMotionListener(m);
		}

		private int index(int x) {
			int i = x / 76;
			return i >= 0 && i < RAM.length && x % 76 < 68 ? i : -1;
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			Graphics2D g = NeonUI.glatt(graphics);
			for (int i = 0; i < RAM.length; i++) {
				int x = i * 76;
				boolean aktiv = RAM[i] == l.ramGb;
				RoundRectangle2D form = new RoundRectangle2D.Float(x + 2, 4, 64, 34, 34, 34);
				if (aktiv) {
					g.setColor(NeonUI.alpha(NeonUI.NEON, 60));
					g.fill(form);
					NeonUI.leuchten(g, form, NeonUI.NEON, 3);
				} else {
					g.setColor(i == ueber ? NeonUI.KARTE_HOVER : NeonUI.KARTE);
					g.fill(form);
					g.setColor(i == ueber ? NeonUI.alpha(NeonUI.NEON, 140) : NeonUI.LINIE);
					g.setStroke(new BasicStroke(1.2f));
					g.draw(form);
				}
				g.setFont(NeonUI.mono(13));
				g.setColor(aktiv ? NeonUI.TEXT : NeonUI.GRAU);
				NeonUI.zentriert(g, RAM[i] + " GB", x + 2, 4, 64, 34);
			}
			g.dispose();
		}
	}
}
