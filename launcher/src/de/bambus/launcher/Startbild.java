package de.bambus.launcher;

import javax.swing.JComponent;
import javax.swing.JWindow;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Kleines Fenster beim Öffnen des Launchers: Logo, Name und ein Ladebalken.
 * Es erscheint sofort, damit man nicht vor einem leeren Bildschirm wartet, und geht
 * von selbst wieder zu, sobald der Launcher da ist.
 */
final class Startbild extends JWindow {

	private static final int B = 460, H = 240;

	private static Startbild offen;

	private final Inhalt inhalt = new Inhalt();
	private String text = "Bambus wird geladen …";
	private int prozent;

	private Startbild() {
		setContentPane(inhalt);
		setSize(B, H);
		setLocationRelativeTo(null);
		setBackground(new Color(0, 0, 0, 0));
	}

	/** Zeigt das Startbild. Klappt etwas nicht (z. B. keine Transparenz), passiert einfach nichts. */
	static void zeige() {
		try {
			offen = new Startbild();
			offen.setVisible(true);
			offen.zeichneJetzt();
		} catch (Exception ignoriert) {
			offen = null;
		}
	}

	/** Zwischenstand melden – wird sofort gezeichnet, auch während der Launcher noch baut. */
	static void melde(String was, int prozent) {
		if (offen == null) return;
		offen.text = was;
		offen.prozent = Math.max(0, Math.min(100, prozent));
		offen.zeichneJetzt();
	}

	static void schliesse() {
		if (offen == null) return;
		offen.dispose();
		offen = null;
	}

	/**
	 * Der Launcher baut sich im selben Zeichen-Faden auf. Darum malen wir hier von Hand,
	 * sonst käme das Bild erst, wenn schon alles fertig ist.
	 */
	private void zeichneJetzt() {
		inhalt.paintImmediately(0, 0, B, H);
		java.awt.Toolkit.getDefaultToolkit().sync();
	}

	private final class Inhalt extends JComponent {
		@Override
		protected void paintComponent(Graphics graphics) {
			Graphics2D g = NeonUI.glatt(graphics);
			RoundRectangle2D form = new RoundRectangle2D.Float(1, 1, B - 3, H - 3, 18, 18);
			g.setColor(new Color(0x0B1220));
			g.fill(form);
			g.setStroke(new BasicStroke(1.6f));
			g.setColor(NeonUI.alpha(NeonUI.NEON, 150));
			g.draw(form);

			NeonUI.logo(g, 44, 52, 108);

			g.setFont(NeonUI.titel(30));
			NeonUI.leuchtText(g, "BAMBUS", 136, 96, NeonUI.NEON, 1);
			g.setFont(NeonUI.mono(12));
			g.setColor(NeonUI.CYAN);
			g.drawString("C L I E N T", 140, 120);

			// Ladebalken
			int bx = 38, by = 176, bb = B - 76, bh = 8;
			g.setColor(new Color(0x18212F));
			g.fillRoundRect(bx, by, bb, bh, 4, 4);
			int voll = Math.max(6, bb * prozent / 100);
			g.setColor(NeonUI.NEON);
			g.fillRoundRect(bx, by, voll, bh, 4, 4);
			g.setColor(NeonUI.alpha(NeonUI.NEON, 60));
			g.fillRoundRect(bx, by - 2, voll, bh + 4, 6, 6);

			g.setFont(NeonUI.normal(11));
			g.setColor(NeonUI.GRAU);
			g.drawString(NeonUI.passend(g, text, bb), bx, by + 26);
			g.dispose();
		}
	}
}
