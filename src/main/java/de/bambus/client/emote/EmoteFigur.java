package de.bambus.client.emote;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Kleine Strichfigur, die ein Emote von vorn tanzt – für das Emote-Rad und die Garderobe.
 * Rechnet die Gelenkwinkel wie Minecraft (Drehung Z·Y·X) und zeigt die Glieder verkürzt, wenn sie nach vorn zeigen.
 */
public final class EmoteFigur {

	private EmoteFigur() {}

	private static final Pose POSE = new Pose();

	/**
	 * @param cx, cy  Mitte der Figur (Hüfte)
	 * @param s       Größe: Körperhöhe = 6·s
	 */
	public static void zeichne(GuiGraphics g, Emote e, float t, float cx, float cy, float s, int farbe) {
		Pose p = e.pose(t, POSE);
		float d = s * 0.6f;   // Strichstärke
		cy -= p.hoehe * 8 * s;

		g.pose().pushMatrix();
		g.pose().translate(cx, cy);
		g.pose().rotate((float) Math.toRadians(p.neigung) * 0.4f + p.koerperZ * 0.6f);   // leicht seitlich wiegen
		// Beine (von der Hüfte nach unten)
		glied(g, -s, 0, p.rBeinX, p.rBeinY, p.rBeinZ, 6 * s, d, farbe);
		glied(g, s, 0, p.lBeinX, p.lBeinY, p.lBeinZ, 6 * s, d, farbe);
		// Körper und Kopf
		g.fill(Math.round(-1.5f * s), Math.round(-6 * s), Math.round(1.5f * s), 0, farbe);
		g.pose().pushMatrix();
		g.pose().translate(0, -6 * s);
		g.pose().rotate(-p.kopfZ);
		g.fill(Math.round(-2 * s), Math.round(-4.4f * s), Math.round(2 * s), Math.round(-0.4f * s), farbe);
		g.pose().popMatrix();
		// Arme (von den Schultern)
		glied(g, -2.2f * s, -5.6f * s, p.rArmX, p.rArmY, p.rArmZ, 5.5f * s, d, farbe);
		glied(g, 2.2f * s, -5.6f * s, p.lArmX, p.lArmY, p.lArmZ, 5.5f * s, d, farbe);
		g.pose().popMatrix();
	}

	/** Ein Glied ab (x, y) mit den Winkeln eines ModelPart – von vorn gesehen. */
	private static void glied(GuiGraphics g, float x, float y, float rx, float ry, float rz, float laenge, float dicke, int farbe) {
		// Richtung (0, 1, 0) (nach unten) mit Rz · Ry · Rx drehen
		double vx = 0, vy = Math.cos(rx), vz = Math.sin(rx);
		double ux = vx * Math.cos(ry) + vz * Math.sin(ry);
		double wx = ux * Math.cos(rz) - vy * Math.sin(rz);
		double wy = ux * Math.sin(rz) + vy * Math.cos(rz);
		// Bildschirm: rechter Arm erscheint links, darum x spiegeln; z (Tiefe) fällt weg – das verkürzt
		float bx = (float) -wx, by = (float) wy;
		float l = (float) Math.sqrt(bx * bx + by * by) * laenge;
		float winkel = (float) Math.atan2(-bx, by);
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().rotate(winkel);
		g.fill(Math.round(-dicke), 0, Math.round(dicke), Math.max(1, Math.round(l)), farbe);
		g.pose().popMatrix();
	}
}
