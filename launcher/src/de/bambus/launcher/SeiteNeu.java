package de.bambus.launcher;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

/** NEUIGKEITEN: Was in den letzten Versionen dazugekommen ist – und ob ein Update wartet. */
final class SeiteNeu extends JPanel {

	private static final int X = 40, B = 696;

	/** {Version, Untertitel, Farbe(hex), Symbol, Punkte …} – neueste zuerst. */
	private static final String[][] VERSIONEN = {
			{"3.4", "Serverauswahl, Einstellungs-Code & mehrere Versionen", "00E5FF", "server",
					"Einstellungs-Code: Module mit Anordnung und alle Minecraft-Einstellungen mit einem Code teilen",
					"Profil-Menü: jedes Profil direkt spielen, Mods hinzufügen auf eigener Seite mit Suche und Icons",
					"Neben SPIELEN: ▾-Pfeil zur Profilauswahl – das aktive Profil steht über dem Knopf",
					"Neue Serverauswahl im Spiel: Karten mit Icon, Ping und Spielern – Favoriten ★ stehen oben",
					"Neues Hauptmenü im Spiel: große Karten für Einzel- und Mehrspieler",
					"Ping und Toggle Sprint als eigene Module – Toggle Sneak und HUD-Anzeige einzeln schaltbar",
					"Neue Einzel-Module: FPS, CPS, Bambus-Schriftzug, Item in der Hand und Ingame-Tag",
					"Mehrere Minecraft-Versionen: jedes Mod-Profil spielt seine eigene (1.21.11 bis 26.3)",
					"Beim Anlegen eines Profils wählst du die Version gleich mit – später über VERSION änderbar",
					"Jede Version bekommt eigene Mods und eigene Welten, damit nichts durcheinandergerät",
					"Der Bambus Client selbst läuft auf 1.21.11 – andere Versionen starten mit Fabric und deinen Mods",
					"Mod-Icons von Modrinth werden jetzt im Launcher angezeigt",
					"Bambus-Helfer kennt alle Crafting-Rezepte, versteht Deutsch besser und erklärt PvP-Taktiken",
					"Side Shield: beim Blocken in die normale Position, danach wieder zur Seite",
					"Beim Server-Beitritt liegt „Abbrechen“ nicht mehr mitten im Bild"},
			{"3.3", "Kosmetik im HD-Look & neue Oberfläche", "3DFF9A", "kosmetik",
					"Quests, Blätter und Ränge (Bronze bis Legende): Punkte sammeln und Kosmetik freischalten – kostenlos",
					"Start-Set geschenkt: Umhang, Flügel, Hut, Partikel und Pfeil-Spur gehören dir von Anfang an",
					"Alle weitere Kosmetik kostet Blätter – Preis steht auf jeder Kachel, Kauf auch in der Garderobe",
					"Saison-Sets: vier passende Stücke je Jahreszeit, zusammen deutlich günstiger als einzeln",
					"Serie: jeden Tag mehr Bonus – aber wer einen Tag auslässt, rutscht einen Rang zurück",
					"Blätter und Rang immer oben rechts neben dem Konto, ein Klick führt zu den Quests",
					"Drei Herausforderungen am Tag im Spiel: Runde gewinnen, Leiter-MLG, Combo, Sprint und mehr",
					"Herausforderungen steigen in Stufen: aus der 5er-Combo wird eine 10er, 15er, 20er – mit mehr Blättern",
					"QUESTS auch im Spiel: Rechts-Shift → QUESTS",
					"Drei neue Umhänge und zwei neue Partikel im Quest-Shop",
					"Eigenes Hintergrundbild für Launcher und Bambus-Hauptmenü",
					"Launcher: Texturen, Helfer und Hauptmenü lassen sich jetzt auch hier einstellen",
					"Launcher im Pixel-Look wie im Spiel – mit Farbthemen, die auch im Spiel gelten",
					"Flügel in zwei Stilen: HD oder Blockbench (eckig) – in der Garderobe umschaltbar",
					"3D-Flügel: biegen sich ohne Lücken, mit Knochen, Deckfedern und Glanz – neu: Bambus",
					"Umhänge wehen im Wind und leuchten – neu: Polarlicht und Glut",
					"Bambus-Ladebildschirm mit Tipps beim Betreten einer Welt",
					"HUD-Editor: Anzeigen rasten ein, Größe mit Ecke oder Mausrad",
					"Neue HUD-Module: Kompass-Balken, Koordinaten mit Nether-Umrechnung, Spielzeit, Ping-Verlauf",
					"Neon-Benachrichtigungen, weiche Animationen und einheitliche Symbole",
					"Launcher: animierte Hintergründe, Server-Favoriten mit Live-Ping, automatische Updates",
					"Willkommens-Seite beim ersten Start – und der Launcher spricht jetzt auch Englisch",
					"Bambus-Helfer: Hilfe-Chat zu Client und Minecraft – ohne Internet"},
			{"3.2", "Das große PvP-Update", "00E5FF", "pvp",
					"Target-HUD, Combo, Reach, W-/S-Tap, Kills, Pfeile, Item-Cooldowns, TNT-Timer",
					"Eigene Zeit und eigenes Wetter, Treffer-Partikel, Low-End-Modus",
					"Hüte, Kill-Effekte und Pfeil-Spuren",
					"Modul-Suche, Favoriten, 7 Farbthemen, 3 Profile, Freunde, Screenshot-Galerie"},
			{"3.1", "Flügel, Studio & Mods", "FF2BD6", "stern",
					"HD-Flügel mit Gelenk und das Kosmetik-Studio für eigene Capes und Flügel",
					"MODS-Seite mit FPS-Boost (Sodium, Lithium & Co.) und Modrinth-Suche",
					"Bambus-Hauptmenü im Spiel"},
	};

	private final BambusLauncher l;

	SeiteNeu(BambusLauncher l) {
		this.l = l;
		setOpaque(false);
		setLayout(null);
		JPanel inhalt = new JPanel(null);
		inhalt.setOpaque(false);
		int y = 0;
		for (int i = 0; i < VERSIONEN.length; i++) {
			VersionsKarte k = new VersionsKarte(VERSIONEN[i], i == 0);
			int h = k.hoehe();
			k.setBounds(4, y, B, h);
			inhalt.add(k);
			y += h + 14;
		}
		inhalt.setPreferredSize(new Dimension(B, y));
		JScrollPane sp = SeiteMods.scroll(inhalt, X - 4, 150, B + 14, 440);
		add(sp);
	}

	@Override
	protected void paintComponent(Graphics graphics) {
		Graphics2D g = NeonUI.glatt(graphics);
		g.setFont(NeonUI.titel(30));
		NeonUI.leuchtText(g, "NEUIGKEITEN", X, 62, NeonUI.TEXT, 1);
		g.setFont(NeonUI.normal(13));
		g.setColor(NeonUI.GRAU);
		g.drawString("Was neu ist im Bambus Client.", X, 86);

		// Update-Hinweis
		RoundRectangle2D form = new RoundRectangle2D.Float(X, 100, B, 38, 12, 12);
		Updater.Release r = l.update;
		g.setColor(NeonUI.KARTE);
		g.fill(form);
		g.setStroke(new BasicStroke(1.2f));
		g.setColor(r != null ? NeonUI.NEON : NeonUI.LINIE);
		g.draw(form);
		g.setFont(NeonUI.mono(11));
		if (r != null) {
			g.setColor(NeonUI.NEON);
			g.drawString("▲  VERSION " + r.version + " IST DA  –  oben auf „UPDATE“ klicken", X + 16, 124);
		} else {
			g.setColor(NeonUI.GRAU);
			g.drawString("●  Du hast Version " + BambusLauncher.VERSION
					+ (Updater.aktiv() ? "  –  neue Versionen kommen automatisch" : ""), X + 16, 124);
		}
		g.dispose();
	}

	private static final class VersionsKarte extends JComponent {
		private final String[] v;
		private final boolean neu;
		private final Color farbe;

		VersionsKarte(String[] v, boolean neu) {
			this.v = v;
			this.neu = neu;
			this.farbe = new Color(Integer.parseInt(v[2], 16));
		}

		int hoehe() {
			return 70 + (v.length - 4) * 24 + 10;
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			Graphics2D g = NeonUI.glatt(graphics);
			int b = getWidth(), h = getHeight();
			RoundRectangle2D form = new RoundRectangle2D.Float(1, 1, b - 3, h - 3, 16, 16);
			g.setColor(NeonUI.KARTE);
			g.fill(form);
			g.setPaint(new GradientPaint(0, 0, NeonUI.alpha(farbe, neu ? 26 : 14), 0, 70, NeonUI.alpha(farbe, 0)));
			g.fill(form);
			if (neu) NeonUI.leuchten(g, form, farbe, 2);
			else {
				g.setStroke(new BasicStroke(1.2f));
				g.setColor(NeonUI.LINIE);
				g.draw(form);
			}
			// Kopf: Symbol, Version, Untertitel
			g.setColor(NeonUI.alpha(farbe, 30));
			g.fillRoundRect(18, 16, 40, 40, 10, 10);
			Symbole.zeichne(g, v[3], 18 + 8, 16 + 8, 3, farbe);
			g.setFont(NeonUI.titel(22));
			NeonUI.leuchtText(g, "VERSION " + v[0], 72, 40, NeonUI.TEXT, 1);
			int vb = g.getFontMetrics().stringWidth("VERSION " + v[0]);
			if (neu) {
				g.setFont(NeonUI.mono(10));
				g.setColor(farbe);
				g.fillRoundRect(72 + vb + 12, 24, 52, 20, 10, 10);
				g.setColor(NeonUI.HG);
				g.drawString("NEU", 72 + vb + 12 + 15, 38);
			}
			g.setFont(NeonUI.normal(12));
			g.setColor(NeonUI.GRAU);
			g.drawString(v[1], 72, 58);
			// Punkte
			g.setFont(NeonUI.normal(13));
			FontMetrics fm = g.getFontMetrics();
			for (int i = 4; i < v.length; i++) {
				int y = 70 + (i - 4) * 24 + 16;
				g.setColor(farbe);
				g.fillOval(76, y - 7, 6, 6);
				g.setColor(new Color(0xD6DEE8));
				g.drawString(kuerzen(fm, v[i], b - 120), 94, y);
			}
			g.dispose();
		}

		private static String kuerzen(FontMetrics fm, String t, int max) {
			t = Sprache.t(t);
			if (fm.stringWidth(t) <= max) return t;
			while (t.length() > 2 && fm.stringWidth(t + "…") > max) t = t.substring(0, t.length() - 1);
			return t + "…";
		}
	}
}
