package de.bambus.client.hud;

import de.bambus.client.Musik;
import de.bambus.client.Songtext;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3x2f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Songtext-Wörter wie im Musikvideo: Jedes gesungene Wort taucht groß und schräg an einem festen Punkt in der Welt
 * vor dir auf, bleibt dort stehen (auch wenn du dich umdrehst), fliegt dann auf dich zu und verblasst.
 * Gezeichnet im HUD: Der Weltpunkt wird jedes Bild mit Blickrichtung und Sichtfeld auf den Bildschirm umgerechnet.
 */
public final class SongtextWelt {

	private SongtextWelt() {}

	private static final long AUFTAUCHEN = 180, STEHEN = 1500, WEGFLIEGEN = 650;
	private static final int MAX = 14;

	private record Wort(Component text, Vec3 ort, long geboren, float neigung, float groesse) {}

	private static final List<Wort> woerter = new ArrayList<>();
	private static final Set<String> schonGezeigt = new HashSet<>();
	private static final Random zufall = new Random();
	private static String lied = "";
	private static double letzteZeit;

	public static void leeren() {
		woerter.clear();
		schonGezeigt.clear();
	}

	public static void zeichne(GuiGraphics g, Minecraft mc) {
		if (mc.player == null || mc.level == null || mc.options.hideGui) {
			woerter.clear();
			return;
		}
		Camera kamera = mc.gameRenderer.getMainCamera();
		neueWoerter(kamera);
		if (woerter.isEmpty()) return;

		int bw = mc.getWindow().getGuiScaledWidth(), bh = mc.getWindow().getGuiScaledHeight();
		double fov = Math.toRadians(mc.options.fov().get());
		double brennweite = (bh / 2.0) / Math.tan(fov / 2);
		Vec3 cam = kamera.position();
		Vector3fc vor = kamera.forwardVector(), links = kamera.leftVector();
		Vector3f hoch = new Vector3f(vor).cross(links);
		long jetzt = System.currentTimeMillis();

		// weiter entfernte zuerst, damit nahe Wörter oben liegen
		woerter.removeIf(w -> jetzt - w.geboren() > AUFTAUCHEN + STEHEN + WEGFLIEGEN);
		woerter.sort((a, b) -> Double.compare(b.ort().distanceToSqr(cam), a.ort().distanceToSqr(cam)));
		for (Wort w : woerter) {
			long alter = jetzt - w.geboren();
			Vec3 ort = w.ort();
			float deckkraft = 1f, pop = 1f;
			if (alter < AUFTAUCHEN) {   // aufploppen mit kleinem Überschwingen
				float k = alter / (float) AUFTAUCHEN;
				pop = (float) (1 + 2.2 * Math.pow(k - 1, 3) + 1.2 * Math.pow(k - 1, 2));
				deckkraft = Math.min(1f, k * 2);
			} else if (alter > AUFTAUCHEN + STEHEN) {   // auf die Kamera zufliegen und verblassen
				float k = (alter - AUFTAUCHEN - STEHEN) / (float) WEGFLIEGEN;
				ort = ort.lerp(cam, Math.min(0.85, k * k * 0.9));
				deckkraft = 1 - k;
			}
			Vec3 v = ort.subtract(cam);
			double z = v.x * vor.x() + v.y * vor.y() + v.z * vor.z();
			if (z < 0.6) continue;   // hinter oder zu dicht vor der Kamera
			double rx = -(v.x * links.x() + v.y * links.y() + v.z * links.z());
			double ry = v.x * hoch.x() + v.y * hoch.y() + v.z * hoch.z();
			float sx = (float) (bw / 2.0 + rx / z * brennweite);
			float sy = (float) (bh / 2.0 - ry / z * brennweite);
			float pixelHoehe = (float) (w.groesse() * brennweite / z) * Math.max(0.05f, pop);
			float s = Math.min(14f, pixelHoehe / 8f);
			if (s < 0.2f || deckkraft <= 0.02f) continue;
			zeichneWort(g, mc, w, sx, sy, s, deckkraft);
		}
	}

	/** Gesungene Wörter der aktuellen Zeile in die Welt setzen (jedes nur einmal). */
	private static void neueWoerter(Camera kamera) {
		if (!Musik.laeuftEtwas || !Musik.spielt || !Songtext.vorhanden()) return;
		String jetztLied = Musik.titel + "|" + Musik.kuenstler;
		double zeit = Musik.positionJetzt();
		if (!jetztLied.equals(lied) || zeit < letzteZeit - 2) {   // neues Lied oder zurückgespult
			lied = jetztLied;
			leeren();
		}
		letzteZeit = zeit;
		Songtext.Zeile zeile = Songtext.aktuelleZeile(zeit);
		if (zeile == null) return;
		for (int i = 0; i < zeile.woerter().size(); i++) {
			Songtext.Wort wort = zeile.woerter().get(i);
			String schluessel = zeile.start() + ":" + i;
			if (wort.start() > zeit || schonGezeigt.contains(schluessel)) continue;
			schonGezeigt.add(schluessel);
			if (zeit - wort.start() > 0.5) continue;   // nach dem Vorspulen nicht alles auf einmal
			setze(kamera, wort.text());
		}
		while (woerter.size() > MAX) woerter.remove(0);
	}

	private static void setze(Camera kamera, String text) {
		String sauber = text.replaceAll("^[\\p{Punct}„“”‚‘’»«]+|[\\p{Punct}„“”‚‘’»«]+$", "");
		if (sauber.isEmpty()) return;
		Vector3fc vor = kamera.forwardVector(), links = kamera.leftVector();
		Vector3f hoch = new Vector3f(vor).cross(links);
		double abstand = 7 + zufall.nextDouble() * 4;
		double seite = (zufall.nextDouble() - 0.5) * 7;
		double oben = -0.8 + zufall.nextDouble() * 3.2;
		Vec3 ort = kamera.position()
				.add(vor.x() * abstand, vor.y() * abstand, vor.z() * abstand)
				.add(-links.x() * seite, -links.y() * seite, -links.z() * seite)
				.add(hoch.x() * oben, hoch.y() * oben, hoch.z() * oben);
		float neigung = (float) Math.toRadians((zufall.nextDouble() - 0.5) * 24);
		float groesse = 0.45f + zufall.nextFloat() * 0.45f + (sauber.length() <= 3 ? -0.1f : 0f);
		Component c = Component.literal(sauber.toUpperCase(java.util.Locale.ROOT)).withStyle(ChatFormatting.BOLD);
		woerter.add(new Wort(c, ort, System.currentTimeMillis(), neigung, groesse));
	}

	/** Weißes Wort mit bläulichem 3D-Rand, leicht schräg und kursiv. */
	private static void zeichneWort(GuiGraphics g, Minecraft mc, Wort w, float x, float y, float s, float deckkraft) {
		int breite = mc.font.width(w.text());
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().rotate(w.neigung());
		g.pose().scale(s, s);
		g.pose().mul(new Matrix3x2f(1, 0, -0.18f, 1, 0, 0));   // kursiv
		g.pose().translate(-breite / 2f, -4f);
		int a = (int) (255 * deckkraft);
		int[] rand = {0x5B7FB0, 0x6A8DBC, 0x7A9BC8, 0x8DACD4, 0xA3BEE0};
		for (int i = rand.length; i >= 1; i--) {   // Ebenen nach hinten versetzt = Tiefe
			g.drawString(mc.font, w.text(), (int) (i * 0.6f), (int) (i * 0.6f), (a << 24) | rand[rand.length - i], false);
		}
		g.drawString(mc.font, w.text(), 0, 0, (a << 24) | 0xFFFFFF, false);
		g.pose().popMatrix();
	}
}
