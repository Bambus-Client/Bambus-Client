package de.bambus.client.hud;

import de.bambus.client.Musik;
import de.bambus.client.Songtext;
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
 * auf, bleibt dort stehen (auch wenn du dich umdrehst), fliegt dann auf dich zu und verblasst.
 * Gezeichnet im HUD: Der Weltpunkt wird jedes Bild mit Blickrichtung und Sichtfeld auf den Bildschirm umgerechnet.
 *
 * Wo die Wörter auftauchen, ist einstellbar ({@link #ORTE}) – gemeint ist die Stelle im Blickfeld beim Erscheinen,
 * danach bleiben sie in der Welt stehen. Wörter erscheinen nur dort, wo du gerade hinsiehst (ganz im Bild), und
 * verschwinden, sobald du wegschaust. Neue Wörter suchen sich einen freien Platz (keine Überlappung); kommt
 * dasselbe Wort direkt noch mal, steht es auf derselben Seite gleich dahinter.
 */
public final class SongtextWelt {

	private SongtextWelt() {}

	public static final String[] ORTE = {"Seiten", "Links", "Rechts", "Oben", "Unten", "Überall"};
	public static final int SEITEN = 0, LINKS = 1, RECHTS = 2, OBEN = 3, UNTEN = 4, UEBERALL = 5;

	private static final long AUFTAUCHEN = 160, STEHEN = 850, WEGFLIEGEN = 380;
	private static final int MAX = 14, VERSUCHE = 16;

	/** ort = fester Weltpunkt; rand = in einem Randbereich (dann nicht riesig und nicht übers Fadenkreuz). */
	private record Wort(Component text, String roh, Vec3 ort, long geboren, float neigung, float groesse, boolean rand) {}

	/** Bildschirm-Lage eines Wortes: Mitte, halbe Breite/Höhe (in GUI-Pixeln). */
	private record Kasten(float x, float y, float hb, float hh) {
		boolean ueberlappt(Kasten k) {
			return Math.abs(x - k.x) < hb + k.hb && Math.abs(y - k.y) < hh + k.hh;
		}

		float ueberlappung(Kasten k) {
			float dx = hb + k.hb - Math.abs(x - k.x), dy = hh + k.hh - Math.abs(y - k.y);
			return dx > 0 && dy > 0 ? dx * dy : 0;
		}
	}

	/** Kamera für ein Bild: Ort, Richtungen und Bildschirmmaße. */
	private record Sicht(Vec3 cam, Vector3fc vor, Vector3fc links, Vector3f hoch, int bw, int bh, double brennweite) {
		static Sicht von(Minecraft mc) {
			Camera k = mc.gameRenderer.getMainCamera();
			int bw = mc.getWindow().getGuiScaledWidth(), bh = mc.getWindow().getGuiScaledHeight();
			double fov = Math.toRadians(mc.options.fov().get());
			return new Sicht(k.position(), k.forwardVector(), k.leftVector(), new Vector3f(k.forwardVector()).cross(k.leftVector()),
					bw, bh, (bh / 2.0) / Math.tan(fov / 2));
		}

		/** Weltpunkt → {x, y, Tiefe} auf dem Bildschirm, oder null hinter/zu nah an der Kamera. */
		double[] projiziere(Vec3 ort) {
			Vec3 v = ort.subtract(cam);
			double z = v.x * vor.x() + v.y * vor.y() + v.z * vor.z();
			if (z < 0.6) return null;
			double rx = -(v.x * links.x() + v.y * links.y() + v.z * links.z());
			double ry = v.x * hoch.x() + v.y * hoch.y() + v.z * hoch.z();
			return new double[] {bw / 2.0 + rx / z * brennweite, bh / 2.0 - ry / z * brennweite, z};
		}

		/** Bildschirm-Richtung (dx nach rechts, dy nach unten) als Welt-Richtung. */
		Vec3 welt(double dx, double dy) {
			return new Vec3(-links.x() * dx - hoch.x() * dy, -links.y() * dx - hoch.y() * dy, -links.z() * dx - hoch.z() * dy);
		}
	}

	private static final List<Wort> woerter = new ArrayList<>();
	private static final Set<String> schonGezeigt = new HashSet<>();
	private static final Random zufall = new Random();
	private static Wort letztes;
	private static String lied = "";
	private static double letzteZeit;

	public static void leeren() {
		woerter.clear();
		schonGezeigt.clear();
		letztes = null;
	}

	public static void zeichne(GuiGraphics g, Minecraft mc, int bereich) {
		if (mc.player == null || mc.level == null || mc.options.hideGui) {
			woerter.clear();
			return;
		}
		Sicht sicht = Sicht.von(mc);
		long jetzt = System.currentTimeMillis();
		woerter.removeIf(w -> jetzt - w.geboren() > AUFTAUCHEN + STEHEN + WEGFLIEGEN);
		neueWoerter(mc, sicht, Math.floorMod(bereich, ORTE.length));
		if (woerter.isEmpty()) return;

		// weiter entfernte zuerst, damit nahe Wörter oben liegen
		List<Wort> reihe = new ArrayList<>(woerter);
		reihe.sort((a, b) -> Double.compare(b.ort().distanceToSqr(sicht.cam()), a.ort().distanceToSqr(sicht.cam())));
		List<Wort> weggeschaut = new ArrayList<>();
		for (Wort w : reihe) {
			if (!sichtbar(mc, sicht, w)) {   // aus dem Blick gedreht: nicht später hinter dir wieder auftauchen
				weggeschaut.add(w);
				continue;
			}
			long alter = jetzt - w.geboren();
			Vec3 ort = w.ort();
			float deckkraft = 1f, pop = 1f;
			if (alter < AUFTAUCHEN) {   // aufploppen mit kleinem Überschwingen
				float k = alter / (float) AUFTAUCHEN;
				pop = (float) (1 + 2.2 * Math.pow(k - 1, 3) + 1.2 * Math.pow(k - 1, 2));
				deckkraft = Math.min(1f, k * 2);
			} else if (alter > AUFTAUCHEN + STEHEN) {   // auf die Kamera zufliegen und verblassen
				float k = Math.min(1f, (alter - AUFTAUCHEN - STEHEN) / (float) WEGFLIEGEN);
				ort = ort.lerp(sicht.cam(), Math.min(0.85, k * k * 0.9));
				deckkraft = 1 - k;
			}
			double[] p = sicht.projiziere(ort);
			if (p == null) continue;
			float s = groesse(w, sicht, p[2]) * Math.max(0.05f, pop);
			if (s < 0.2f || deckkraft <= 0.02f) continue;
			zeichneWort(g, mc, w, (float) p[0], (float) p[1], s, deckkraft);
		}
		woerter.removeAll(weggeschaut);
	}

	/** Ist das Wort (an seinem festen Platz) noch irgendwo im Bild? */
	private static boolean sichtbar(Minecraft mc, Sicht sicht, Wort w) {
		Kasten k = kasten(mc, w.text(), w.groesse(), w.rand(), w.ort(), sicht);
		return k != null && k.x() + k.hb() > 0 && k.x() - k.hb() < sicht.bw() && k.y() + k.hh() > 0 && k.y() - k.hh() < sicht.bh();
	}

	/** Ganz im Bild (mit etwas Rand) – und Randwörter nicht über dem Fadenkreuz? */
	private static boolean imBlick(Kasten k, Sicht sicht, boolean rand) {
		boolean imBild = k.x() - k.hb() > 4 && k.x() + k.hb() < sicht.bw() - 4 && k.y() - k.hh() > 4 && k.y() + k.hh() < sicht.bh() - 4;
		boolean fadenkreuzFrei = !rand || Math.abs(k.x() - sicht.bw() / 2f) > k.hb() + sicht.bw() * 0.08f
				|| Math.abs(k.y() - sicht.bh() / 2f) > k.hh() + sicht.bh() * 0.08f;
		return imBild && fadenkreuzFrei;
	}

	/** Schrift-Skalierung eines Wortes in der Tiefe z (am Rand gedeckelt, damit nichts riesig wird). */
	private static float groesse(Wort w, Sicht sicht, double z) {
		float pixelHoehe = (float) (w.groesse() * sicht.brennweite() / z);
		return Math.min(w.rand() ? 6f : 14f, pixelHoehe / 8f);
	}

	private static Kasten kasten(Minecraft mc, Component text, float groesse, boolean rand, Vec3 ort, Sicht sicht) {
		double[] p = sicht.projiziere(ort);
		if (p == null) return null;
		float s = groesse(new Wort(text, "", ort, 0, 0, groesse, rand), sicht, p[2]);
		return new Kasten((float) p[0], (float) p[1], mc.font.width(text) * s / 2 + 3, 5.5f * s + 3);
	}

	/** Gesungene Wörter der aktuellen Zeile in die Welt setzen (jedes nur einmal). */
	private static void neueWoerter(Minecraft mc, Sicht sicht, int bereich) {
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
			setze(mc, sicht, wort.text(), bereich);
		}
		while (woerter.size() > MAX) woerter.remove(0);
	}

	private static void setze(Minecraft mc, Sicht sicht, String text, int bereich) {
		String sauber = text.replaceAll("^[\\p{Punct}„“”‚‘’»«]+|[\\p{Punct}„“”‚‘’»«]+$", "");
		if (sauber.isEmpty()) return;
		Component c = de.bambus.client.Schriften.text(sauber.toUpperCase(java.util.Locale.ROOT),
				de.bambus.client.BambusClient.config.musikSchriftWoerter, true);
		long jetzt = System.currentTimeMillis();

		// dasselbe Wort direkt noch mal: auf derselben Seite gleich dahinter
		if (letztes != null && letztes.roh().equalsIgnoreCase(sauber) && woerter.contains(letztes)
				&& jetzt - letztes.geboren() < AUFTAUCHEN + STEHEN) {
			Vec3 ort = dahinter(mc, sicht, letztes, c);
			if (ort != null) {
				merke(new Wort(c, sauber, ort, jetzt, letztes.neigung(), letztes.groesse(), letztes.rand()));
				return;
			}
		}

		boolean frei = bereich == UEBERALL;
		float neigung = (float) Math.toRadians((zufall.nextDouble() - 0.5) * 24);
		float groesse = 0.45f + zufall.nextFloat() * 0.45f + (sauber.length() <= 3 ? -0.1f : 0f);
		if (!frei) groesse *= 0.8f;   // am Rand etwas kleiner
		double tanV = (sicht.bh() / 2.0) / sicht.brennweite(), tanH = tanV * sicht.bw() / (double) sicht.bh();

		// mehrere Stellen probieren und die erste freie nehmen (sonst die mit der kleinsten Überlappung)
		Vec3 bester = null;
		float besteUeberlappung = Float.MAX_VALUE;
		for (int versuch = 0; versuch < VERSUCHE; versuch++) {
			Vec3 ort = stelle(sicht, bereich, tanH, tanV);
			Kasten k = kasten(mc, c, groesse, !frei, ort, sicht);
			if (k == null || !imBlick(k, sicht, !frei)) continue;   // nur dort, wo du hinsiehst
			float ueberlappung = ueberlappung(mc, sicht, k);
			if (ueberlappung < besteUeberlappung) {
				besteUeberlappung = ueberlappung;
				bester = ort;
			}
			if (ueberlappung == 0) break;
		}
		if (bester != null) merke(new Wort(c, sauber, bester, jetzt, neigung, groesse, !frei));
	}

	private static void merke(Wort w) {
		woerter.add(w);
		letztes = w;
	}

	/** Zufällige Stelle im gewählten Bereich des Blickfelds – als fester Weltpunkt. */
	private static Vec3 stelle(Sicht sicht, int bereich, double tanH, double tanV) {
		double abstand = 7 + zufall.nextDouble() * 4;
		// Anteile der halben Bildbreite (waagerecht) bzw. halben Bildhöhe (senkrecht); 0 = Bildmitte
		double waag, senk;
		switch (bereich) {
			case LINKS -> { waag = -(0.5 + zufall.nextDouble() * 0.4); senk = -0.35 + zufall.nextDouble() * 0.85; }
			case RECHTS -> { waag = 0.5 + zufall.nextDouble() * 0.4; senk = -0.35 + zufall.nextDouble() * 0.85; }
			case OBEN -> { waag = (zufall.nextDouble() - 0.5) * 1.5; senk = 0.4 + zufall.nextDouble() * 0.35; }
			case UNTEN -> { waag = (zufall.nextDouble() - 0.5) * 1.3; senk = -(0.3 + zufall.nextDouble() * 0.3); }
			case SEITEN -> { waag = (zufall.nextBoolean() ? 1 : -1) * (0.55 + zufall.nextDouble() * 0.35); senk = -0.25 + zufall.nextDouble() * 0.75; }
			default -> { waag = (zufall.nextDouble() - 0.5) * 1.6; senk = -0.3 + zufall.nextDouble() * 0.9; }
		}
		double seite = waag * tanH * abstand, oben = senk * tanV * abstand;
		Vector3fc vor = sicht.vor();
		return sicht.cam()
				.add(vor.x() * abstand, vor.y() * abstand, vor.z() * abstand)
				.add(sicht.welt(seite, -oben));
	}

	/** Gesamte Überlappung eines Kastens mit den schon stehenden Wörtern. */
	private static float ueberlappung(Minecraft mc, Sicht sicht, Kasten neu) {
		float summe = 0;
		for (Wort w : woerter) {
			Kasten k = kasten(mc, w.text(), w.groesse(), w.rand(), w.ort(), sicht);
			if (k != null) summe += neu.ueberlappung(k);
		}
		return summe;
	}

	/** Platz direkt hinter dem vorigen Wort (in seiner Schräge), sonst eine Zeile darunter – oder null, wenn beides nicht im Blick liegt. */
	private static Vec3 dahinter(Minecraft mc, Sicht sicht, Wort vorher, Component neu) {
		double einheit = vorher.groesse() / 8.0;   // Weltgröße eines Schrift-Pixels
		double cos = Math.cos(vorher.neigung()), sin = Math.sin(vorher.neigung());
		double weiter = (mc.font.width(vorher.text()) + mc.font.width(neu)) / 2.0 * einheit + 5 * einheit;
		Vec3 rechts = vorher.ort().add(sicht.welt(cos * weiter, sin * weiter));
		Vec3 zeile = vorher.ort().add(sicht.welt(-sin * 13 * einheit, cos * 13 * einheit));
		for (Vec3 ort : new Vec3[] {rechts, zeile}) {
			Kasten k = kasten(mc, neu, vorher.groesse(), vorher.rand(), ort, sicht);
			if (k != null && imBlick(k, sicht, vorher.rand()) && ueberlappung(mc, sicht, k) == 0) return ort;
		}
		return null;   // dahinter ist kein Platz im Blick – dann wie ein neues Wort platzieren
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
