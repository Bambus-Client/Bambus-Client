package de.bambus.launcher;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.geom.Path2D;

/** Animierte Hintergründe für den Launcher (in den Einstellungen wählbar). */
final class Hintergruende {

	static final String[][] ARTEN = {   // {id, Name}
			{"gitter", "GITTER"},
			{"partikel", "PARTIKEL"},
			{"synthwave", "SYNTHWAVE"},
			{"aurora", "AURORA"},
			{"datenregen", "DATENREGEN"},
	};

	private static final int ANZAHL = 46;
	private static final float[][] PUNKTE = new float[ANZAHL][4];   // x, y (0..1), Tempo, Größe

	static {
		java.util.Random r = new java.util.Random(7);
		for (float[] p : PUNKTE) {
			p[0] = r.nextFloat();
			p[1] = r.nextFloat();
			p[2] = 0.012f + r.nextFloat() * 0.03f;
			p[3] = 1.5f + r.nextFloat() * 2.5f;
		}
	}

	private Hintergruende() {}

	static void zeichne(Graphics2D g, String art, int x, int y, int b, int h) {
		double t = System.currentTimeMillis() / 1000.0;
		switch (art == null ? "gitter" : art) {
			case "partikel": partikel(g, x, y, b, h, t); break;
			case "synthwave": synthwave(g, x, y, b, h, t); break;
			case "aurora": aurora(g, x, y, b, h, t); break;
			case "datenregen": datenregen(g, x, y, b, h, t); break;
			default: NeonUI.gitter(g, x, y, b, h, 28, new Color(255, 255, 255, 6));
		}
	}

	/** Leuchtende Punkte steigen langsam auf; nahe Punkte verbinden sich mit Linien. */
	private static void partikel(Graphics2D g, int x, int y, int b, int h, double t) {
		NeonUI.gitter(g, x, y, b, h, 28, new Color(255, 255, 255, 4));
		float[][] pos = new float[ANZAHL][2];
		for (int i = 0; i < ANZAHL; i++) {
			float[] p = PUNKTE[i];
			float py = (float) ((p[1] - t * p[2]) % 1.0);
			if (py < 0) py += 1;
			float px = (float) (p[0] + 0.02 * Math.sin(t * 0.6 + i));
			pos[i][0] = x + px * b;
			pos[i][1] = y + py * h;
		}
		g.setStroke(new BasicStroke(1f));
		for (int i = 0; i < ANZAHL; i++) {
			for (int j = i + 1; j < ANZAHL; j++) {
				float dx = pos[i][0] - pos[j][0], dy = pos[i][1] - pos[j][1];
				float d = (float) Math.sqrt(dx * dx + dy * dy);
				if (d < 120) {
					g.setColor(NeonUI.alpha(i % 3 == 0 ? NeonUI.CYAN : NeonUI.NEON, (int) (38 * (1 - d / 120))));
					g.drawLine((int) pos[i][0], (int) pos[i][1], (int) pos[j][0], (int) pos[j][1]);
				}
			}
		}
		for (int i = 0; i < ANZAHL; i++) {
			float s = PUNKTE[i][3];
			Color c = i % 3 == 0 ? NeonUI.CYAN : i % 7 == 0 ? NeonUI.PINK : NeonUI.NEON;
			g.setColor(NeonUI.alpha(c, 40));
			g.fillOval((int) (pos[i][0] - s * 2.5f), (int) (pos[i][1] - s * 2.5f), (int) (s * 5), (int) (s * 5));
			g.setColor(NeonUI.alpha(c, 170));
			g.fillOval((int) (pos[i][0] - s / 2), (int) (pos[i][1] - s / 2), (int) Math.max(2, s), (int) Math.max(2, s));
		}
	}

	/** Perspektiv-Gitter, das auf dich zufährt, mit leuchtendem Horizont. */
	private static void synthwave(Graphics2D g, int x, int y, int b, int h, double t) {
		int horizont = y + (int) (h * 0.58);
		int mitte = x + b / 2;
		// Sonne
		int r = (int) (h * 0.2);
		g.setPaint(new GradientPaint(0, horizont - 2 * r, NeonUI.alpha(new Color(0xFFC846), 60), 0, horizont, NeonUI.alpha(NeonUI.PINK, 50)));
		g.fillOval(mitte - r, horizont - 2 * r + r / 3, 2 * r, 2 * r);
		g.setColor(NeonUI.HG);
		for (int i = 0; i < 6; i++) {
			int sy = horizont - r / 2 - i * (r / 6) + (int) ((t * 6) % (r / 6));
			g.fillRect(mitte - r, sy, 2 * r, 2 + i / 2);
		}
		g.fillRect(x, horizont, b, h);
		// Horizont-Leuchten
		g.setPaint(new GradientPaint(0, horizont - 40, NeonUI.alpha(NeonUI.PINK, 0), 0, horizont, NeonUI.alpha(NeonUI.PINK, 70)));
		g.fillRect(x, horizont - 40, b, 40);
		// Längslinien
		g.setStroke(new BasicStroke(1.2f));
		for (int i = -14; i <= 14; i++) {
			int unten = mitte + i * b / 9;
			g.setColor(NeonUI.alpha(NeonUI.PINK, 70 - Math.abs(i) * 3));
			g.drawLine(mitte + i * 12, horizont, unten, y + h);
		}
		// Querlinien, die näherkommen
		double phase = (t * 0.6) % 1.0;
		for (int i = 0; i < 14; i++) {
			double k = (i + phase) / 14.0;
			int ly = horizont + (int) ((y + h - horizont) * k * k);
			g.setColor(NeonUI.alpha(NeonUI.CYAN, (int) (20 + 80 * k)));
			g.drawLine(x, ly, x + b, ly);
		}
	}

	/** Weiche Nordlicht-Bänder, die langsam wogen. */
	private static void aurora(Graphics2D g, int x, int y, int b, int h, double t) {
		NeonUI.gitter(g, x, y, b, h, 28, new Color(255, 255, 255, 3));
		Color[] farben = {NeonUI.NEON, NeonUI.CYAN, new Color(0xB06BFF)};
		for (int band = 0; band < 3; band++) {
			Path2D.Float p = new Path2D.Float();
			float basis = y + h * (0.28f + band * 0.14f);
			p.moveTo(x, y + h);
			for (int px = 0; px <= b; px += 16) {
				double w = Math.sin(px / 170.0 + t * (0.35 + band * 0.12) + band * 2) * 34
						+ Math.sin(px / 61.0 - t * 0.7 + band) * 12;
				p.lineTo(x + px, basis + (float) w);
			}
			p.lineTo(x + b, y + h);
			p.closePath();
			g.setPaint(new GradientPaint(0, basis - 60, NeonUI.alpha(farben[band], 0), 0, basis + 10, NeonUI.alpha(farben[band], 34)));
			g.fill(p);
			g.setPaint(new GradientPaint(0, basis, NeonUI.alpha(farben[band], 28), 0, y + h, NeonUI.alpha(farben[band], 0)));
			g.fill(p);
		}
		// funkelnde Sterne
		for (int i = 0; i < 40; i++) {
			float sx = PUNKTE[i][0] * b, sy = PUNKTE[i][1] * h * 0.5f;
			int a = (int) (60 + 60 * Math.sin(t * 2 + i));
			g.setColor(new Color(255, 255, 255, Math.max(0, a)));
			g.fillRect(x + (int) sx, y + (int) sy, 2, 2);
		}
	}

	/** Neonfarbene Zeichen-Spuren regnen herunter. */
	private static void datenregen(Graphics2D g, int x, int y, int b, int h, double t) {
		int spalte = 22;
		g.setFont(NeonUI.mono(12));
		for (int s = 0; s < b / spalte; s++) {
			float[] p = PUNKTE[s % ANZAHL];
			double tempo = 60 + p[2] * 3000;
			int laenge = 6 + (int) (p[3] * 3);
			double kopf = (t * tempo + p[1] * h * 2) % (h + laenge * 16);
			for (int k = 0; k < laenge; k++) {
				int zy = (int) (kopf - k * 16);
				if (zy < 0 || zy > h) continue;
				int a = k == 0 ? 170 : (int) (90 * (1 - k / (float) laenge));
				g.setColor(NeonUI.alpha(k == 0 ? Color.WHITE : (s % 5 == 0 ? NeonUI.CYAN : NeonUI.NEON), a / 2));
				char c = (char) ('0' + (int) ((s * 7 + k * 13 + (int) (t * 4)) % 2));
				g.drawString(String.valueOf(c), x + s * spalte + 6, y + zy);
			}
		}
	}

	/** Für das Licht in den Ecken bei allen Arten. */
	static void eckenLicht(Graphics2D g, int x, int b, int h) {
		g.setPaint(new RadialGradientPaint(x + b - 120f, 90f, 420f, new float[]{0f, 1f},
				new Color[]{NeonUI.alpha(NeonUI.CYAN, 38), NeonUI.alpha(NeonUI.CYAN, 0)}));
		g.fillRect(x, 0, b, h);
		g.setPaint(new RadialGradientPaint(x + 160f, h - 40f, 460f, new float[]{0f, 1f},
				new Color[]{NeonUI.alpha(NeonUI.NEON, 30), NeonUI.alpha(NeonUI.NEON, 0)}));
		g.fillRect(x, 0, b, h);
	}
}
