package de.bambus.client.emote;

import de.bambus.client.BambusClient;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Alle Emotes und das gerade laufende. Emotes sind – wie die Kosmetik – nur auf deinem Bildschirm zu sehen:
 * Während eines Emotes zeigt die Kamera dich von vorn; Bewegen, Springen, Schleichen oder Angreifen beendet es.
 *
 * Alle Bewegungen und Beats sind eigene Kreationen (lockerer Stil wie in Battle-Royale-Spielen).
 */
public final class Emotes {

	private Emotes() {}

	private static final float PI = (float) Math.PI;

	/** Alle Emotes in der Reihenfolge der Garderobe. */
	public static final List<Emote> ALLE = new ArrayList<>();
	private static final Map<String, Emote> NACH_ID = new LinkedHashMap<>();

	/** Belegung des Rads ab Werk. */
	/** Ab Werk liegen nur die Emotes aus dem Start-Set im Rad – die anderen werden mit Blättern freigeschaltet. */
	public static final String STANDARD_RAD = "winken,jubeln,klatschen,,,,,";
	public static final int PLAETZE = 8;

	private static float sin(float t, float hz) {
		return (float) Math.sin(t * hz * 2 * Math.PI);
	}

	private static float cos(float t, float hz) {
		return (float) Math.cos(t * hz * 2 * Math.PI);
	}

	/** 0 → 1 → 0 im Takt (für Hüpfer). */
	private static float hops(float t, float hz) {
		return Math.abs(sin(t, hz));
	}

	private static void neu(String id, String name, String text, int farbe, boolean schleife, float dauer,
							Emote.Bewegung b, Takt takt) {
		neu(id, name, text, farbe, schleife, dauer, b, takt, null);
	}

	private static void neu(String id, String name, String text, int farbe, boolean schleife, float dauer,
							Emote.Bewegung b, Takt takt, Emote.Effekt effekt) {
		Emote e = new Emote(id, name, text, farbe, schleife, dauer, b, takt, effekt);
		ALLE.add(e);
		NACH_ID.put(id, e);
	}

	static {
		// ---------------------------------------------------------------- Gesten
		neu("winken", "Winken", "Freundlich Hallo sagen", 0xFF3DFF8A, false, 2.6f, (p, t) -> {
			p.rArmX = -2.75f;
			p.rArmZ = 0.35f + 0.4f * sin(t, 2.2f);
			p.kopfZ = 0.08f * sin(t, 2.2f);
			p.lArmZ = -0.12f;
		}, null);

		neu("jubeln", "Jubeln", "Beide Arme hoch und hüpfen", 0xFFFFC846, true, 0, (p, t) -> {
			p.arme(-2.9f, 0.35f + 0.15f * sin(t, 2.4f));
			p.hoehe = 0.22f * hops(t, 1.2f);
			p.kopfX = -0.25f;
			p.rBeinZ = 0.08f;
			p.lBeinZ = -0.08f;
		}, null);

		neu("klatschen", "Klatschen", "Applaus!", 0xFF00E5FF, true, 0, (p, t) -> {
			float zu = 0.5f + 0.5f * sin(t, 2.6f);
			p.rArmX = -1.25f;
			p.lArmX = -1.25f;
			p.rArmY = -0.35f * zu;
			p.lArmY = 0.35f * zu;
			p.rArmZ = 0.25f - 0.32f * zu;
			p.lArmZ = -(0.25f - 0.32f * zu);
			p.kopfX = 0.08f * sin(t, 2.6f);
		}, null);

		neu("muskeln", "Muskeln", "Zeig, was du hast", 0xFFFF8A3D, true, 0, (p, t) -> {
			float pump = 0.5f + 0.5f * sin(t, 1.4f);
			p.rArmX = -2.2f - 0.3f * pump;
			p.lArmX = -2.2f - 0.3f * (1 - pump);
			p.rArmZ = 0.75f;
			p.lArmZ = -0.75f;
			p.koerperY = 0.12f * sin(t, 0.7f);
			p.kopfY = 0.25f * sin(t, 0.7f);
			p.rBeinZ = 0.12f;
			p.lBeinZ = -0.12f;
		}, null);

		neu("lachen", "Lachen", "Bauch halten vor Lachen", 0xFFFF2BD6, true, 0, (p, t) -> {
			float wackel = sin(t, 7f);
			p.neigung = 14 + 3 * wackel;
			p.rArmX = -0.75f;
			p.lArmX = -0.75f;
			p.rArmZ = -0.35f;
			p.lArmZ = 0.35f;
			p.kopfX = -0.35f + 0.12f * wackel;
		}, null);

		neu("salut", "Salut", "Respekt!", 0xFFAEEBFF, false, 2.4f, (p, t) -> {
			float rein = Math.min(1f, t * 4);
			p.rArmX = -2.35f * rein;
			p.rArmZ = -0.95f * rein;
			p.kopfX = -0.1f;
		}, null);

		neu("facepalm", "Facepalm", "Ernsthaft …?", 0xFFB06BFF, false, 3f, (p, t) -> {
			float rein = Math.min(1f, t * 3);
			p.rArmX = -2.15f * rein;
			p.rArmZ = -0.62f * rein;
			p.kopfX = 0.45f * rein;
			p.kopfY = t > 1.2f ? 0.18f * sin(t, 1.8f) : 0;   // Kopfschütteln
			p.neigung = 6 * rein;
		}, null);

		neu("sieger", "Siegerpose", "Gewonnen!", 0xFFFFC846, false, 2.6f, (p, t) -> {
			float sprung = t < 0.6f ? (float) Math.sin(t / 0.6f * Math.PI) : 0;
			p.hoehe = 0.55f * sprung;
			p.rArmX = -3.05f;
			p.rArmZ = 0.12f;
			p.lArmZ = -0.35f;
			p.lArmX = 0.25f;
			p.rBeinX = -0.5f * sprung;
			p.kopfX = -0.3f;
		}, null);

		// ---------------------------------------------------------------- Tänze
		neu("wackler", "Bambus-Wackler", "Hüften im Takt wie Bambus im Wind", 0xFF3DFF8A, true, 0, (p, t) -> {
			float s = sin(t, 1f);
			p.koerperZ = 0.16f * s;
			p.kopfZ = -0.12f * s;
			p.rArmX = 0.55f * s;
			p.lArmX = -0.55f * s;
			p.rArmZ = 0.35f;
			p.lArmZ = -0.35f;
			p.rBeinZ = 0.1f + 0.08f * s;
			p.lBeinZ = -0.1f + 0.08f * s;
			p.hoehe = 0.06f * hops(t, 1f);
			p.drehung = 18 * s;
		}, new Takt(5, "x...x...", "..x...x.", "xxxxxxxx", "a.a.f.h.", null));

		neu("roboter", "Roboter", "Zack, zack – wie aus Metall", 0xFF00E5FF, true, 0, (p, t) -> {
			// acht feste Stellungen, eine pro Achtel – ohne Übergang, wie ein Roboter
			int s = (int) (t * 3.33f) % 8;
			float[][] arme = {{-1.57f, 0}, {-1.57f, 0}, {0, 0}, {-1.57f, 0}, {-3.1f, 0}, {-1.57f, 0}, {0, 1.57f}, {-1.57f, 0}};
			float[][] links = {{0, 0}, {-1.57f, 0}, {-1.57f, 0}, {-1.57f, 0}, {-1.57f, 0}, {-3.1f, 0}, {0, -1.57f}, {0, 0}};
			p.rArmX = arme[s][0];
			p.rArmZ = arme[s][1];
			p.lArmX = links[s][0];
			p.lArmZ = links[s][1];
			p.kopfY = new float[]{0, 0.6f, 0.6f, 0, -0.6f, -0.6f, 0, 0}[s];
			p.drehung = new float[]{0, 0, 20, 20, 0, -20, -20, 0}[s];
		}, new Takt(6, "x..x..x.", "....x...", "x.x.x.x.", null, "m.m.p.m."));

		neu("disco", "Disco-Fieber", "Finger hoch, Finger runter", 0xFFFF2BD6, true, 0, (p, t) -> {
			float s = sin(t, 1f);
			float oben = 0.5f + 0.5f * s;
			p.rArmX = -1.0f - 1.9f * oben;
			p.rArmZ = 0.35f + 0.4f * (1 - oben);
			p.lArmX = -0.3f;
			p.lArmZ = -0.75f;   // Hand an der Hüfte
			p.koerperZ = 0.1f * s;
			p.kopfZ = 0.1f * s;
			p.rBeinZ = 0.18f * oben;
			p.hoehe = 0.04f * hops(t, 2f);
		}, new Takt(5, "x.x.x.x.", "..x...x.", ".x.x.x.x", "a.mah..f", null));

		neu("welle", "Wellenarme", "Eine Welle geht durch die Arme", 0xFF2B6BFF, true, 0, (p, t) -> {
			p.rArmZ = 1.45f + 0.45f * sin(t, 0.9f);
			p.lArmZ = -(1.45f + 0.45f * sin(t - 0.28f, 0.9f));
			p.rArmX = 0.25f * sin(t - 0.1f, 0.9f);
			p.lArmX = 0.25f * sin(t - 0.38f, 0.9f);
			p.koerperZ = 0.08f * sin(t - 0.14f, 0.9f);
			p.kopfZ = 0.12f * sin(t - 0.14f, 0.9f);
			p.hoehe = 0.03f * hops(t, 0.9f);
		}, new Takt(6, "x.....x.", "....x...", "x.xxx.xx", "d...f...", null));

		neu("propeller", "Propeller", "Arme kreisen wie Rotorblätter", 0xFFFF8A3D, true, 0, (p, t) -> {
			float w = t * 2 * PI * 1.3f;
			p.rArmZ = 1.5f;
			p.lArmZ = -1.5f;
			p.rArmX = w;
			p.lArmX = w + PI;
			p.drehung = 40 * sin(t, 0.4f);
			p.hoehe = 0.04f * hops(t, 1.3f);
		}, new Takt(4, "x.x.x.x.", "...x...x", "xxxxxxxx", "a.a.a.c.", null));

		neu("hampelmann", "Hampelmann", "Fit bleiben im Takt", 0xFFFFC846, true, 0, (p, t) -> {
			float k = 0.5f + 0.5f * sin(t, 1.5f);
			p.rArmZ = 0.2f + 2.6f * k;
			p.lArmZ = -(0.2f + 2.6f * k);
			p.rBeinZ = 0.04f + 0.32f * k;
			p.lBeinZ = -(0.04f + 0.32f * k);
			p.hoehe = 0.16f * hops(t, 1.5f);
		}, null);

		neu("kreisel", "Kreisel", "Drehen, bis die Welt sich dreht", 0xFF7DF9FF, true, 0, (p, t) -> {
			p.drehung = 360 * t * 0.9f;
			p.arme(-0.2f, 1.35f + 0.1f * sin(t, 1.8f));
			p.kopfX = -0.2f;
			p.hoehe = 0.05f * hops(t, 0.9f);
		}, null);

		neu("chillen", "Chillen", "Hinsetzen und entspannen", 0xFF8DACD4, true, 0, (p, t) -> {
			p.hoehe = -0.62f;
			p.neigung = -12;
			p.rBeinX = -1.45f;
			p.lBeinX = -1.45f;
			p.rBeinY = 0.28f;
			p.lBeinY = -0.28f;
			p.arme(0.65f, 0.35f);   // auf die Hände nach hinten gestützt
			p.kopfX = -0.15f + 0.06f * sin(t, 0.5f);
			p.kopfY = 0.2f * sin(t, 0.18f);
		}, null);
	}

	// ================================================================ Neue Tänze und Gesten (eigene Kreationen)

	/** Ein Partikel nahe beim Spieler (nur für dich sichtbar). dx/dz: seitlich versetzt, y: Höhe über den Füßen. */
	private static void partikel(Minecraft mc, Player s, net.minecraft.core.particles.ParticleOptions art,
								 double dx, double y, double dz, double vx, double vy, double vz) {
		if (mc.level != null) mc.level.addParticle(art, s.getX() + dx, s.getY() + y, s.getZ() + dz, vx, vy, vz);
	}

	private static final java.util.Random ZUFALL = new java.util.Random();

	private static float z(float max) {
		return (ZUFALL.nextFloat() * 2 - 1) * max;
	}

	static {
		neu("fersenflitzer", "Fersen-Flitzer", "Hacken tippen, Arme pumpen – im Laufschritt auf der Stelle", 0xFF3DFF8A, true, 0, (p, t) -> {
			float s = sin(t, 1.2f);
			p.neigung = 10;
			p.rBeinX = -0.55f * Math.max(0, s);
			p.lBeinX = -0.55f * Math.max(0, -s);
			p.rArmX = 0.9f * s;
			p.lArmX = -0.9f * s;
			p.rArmZ = 0.2f;
			p.lArmZ = -0.2f;
			p.hoehe = 0.07f * hops(t, 1.2f);
			p.kopfX = 0.12f * sin(t, 2.4f);
		}, new Takt(5, "x..x.x..", "..x...x.", "xxxxxxxx", "a..a.c..", null));

		neu("rundum", "Rundum-Feuerwerk", "Im Kreis tanzen, über dir knallt Feuerwerk", 0xFFFFC846, true, 0, (p, t) -> {
			p.drehung = 360 * t * 0.35f;
			float s = sin(t, 1f);
			p.rArmX = -2.6f + 0.5f * s;
			p.lArmX = -2.6f - 0.5f * s;
			p.rArmZ = 0.45f;
			p.lArmZ = -0.45f;
			p.hoehe = 0.1f * hops(t, 1f);
			p.rBeinZ = 0.1f;
			p.lBeinZ = -0.1f;
		}, new Takt(5, "x...x.x.", "..x...x.", "x.x.x.x.", "a.h.a.j.", null), (mc, s, t, nr) -> {
			if (nr % 24 == 0) {   // alle 1,2 Sekunden eine Rakete über dir
				double w = ZUFALL.nextDouble() * Math.PI * 2;
				double x = Math.cos(w) * 1.6, zz = Math.sin(w) * 1.6;
				for (int i = 0; i < 24; i++) partikel(mc, s, net.minecraft.core.particles.ParticleTypes.FIREWORK, x, 3.2, zz, z(0.18f), z(0.18f), z(0.18f));
			}
		});

		neu("zickzack", "Zickzack-Schwung", "Arme schwingen hin und her, Beine kicken zur Seite", 0xFFFF8A3D, true, 0, (p, t) -> {
			float s = sin(t, 1f);
			p.rArmZ = 0.9f + 0.7f * s;
			p.lArmZ = -0.9f + 0.7f * s;
			p.rArmX = -0.5f;
			p.lArmX = -0.5f;
			p.koerperZ = -0.12f * s;
			p.rBeinZ = 0.45f * Math.max(0, s);
			p.lBeinZ = -0.45f * Math.max(0, -s);
			p.hoehe = 0.08f * hops(t, 1f);
			p.kopfZ = 0.1f * s;
		}, new Takt(5, "x..x..x.", "....x...", "xxxxxxxx", "c..c..f.", null));

		neu("huepfshuffle", "Hüpf-Shuffle", "Federnd auf der Stelle – immer im Takt", 0xFF00E5FF, true, 0, (p, t) -> {
			float s = sin(t, 1.1f);
			p.hoehe = 0.12f * hops(t, 2.2f);
			p.rBeinX = -0.35f * s;
			p.lBeinX = 0.35f * s;
			p.rArmX = -0.7f - 0.4f * hops(t, 2.2f);
			p.lArmX = -0.7f - 0.4f * hops(t, 2.2f);
			p.rArmZ = 0.3f;
			p.lArmZ = -0.3f;
			p.kopfX = 0.1f * hops(t, 2.2f);
			p.drehung = 12 * s;
		}, new Takt(5, "x.x.x.x.", "..x...x.", ".xxx.xxx", "a.a.d.d.", null));

		neu("breakdance", "Breakdance", "Toprock, ab auf den Boden und drehen", 0xFFB06BFF, true, 0, (p, t) -> {
			float phase = t % 6f;
			if (phase < 2f) {   // Toprock: Schritte vor und zurück, Arme locker
				float s = sin(t, 1.2f);
				p.rBeinX = -0.5f * s;
				p.lBeinX = 0.5f * s;
				p.arme(0.5f * s, 0.6f);
				p.drehung = 15 * s;
				p.hoehe = 0.05f * hops(t, 1.2f);
			} else if (phase < 5.4f) {   // auf dem Rücken drehen, Beine in V-Form
				float k = Math.min(1f, (phase - 2f) * 4);
				p.neigung = -85 * k;
				p.hoehe = -0.62f * k;
				p.drehung = 540 * (phase - 2f);
				p.rBeinZ = 0.6f;
				p.lBeinZ = -0.6f;
				p.rBeinX = -0.3f;
				p.lBeinX = -0.3f;
				p.arme(-0.3f, 1.4f);
			} else {   // Freeze: eine Hand hoch
				p.hoehe = -0.62f;
				p.neigung = -40;
				p.rArmX = -3.0f;
				p.lArmZ = -0.9f;
				p.rBeinX = -1.2f;
				p.lBeinX = -0.6f;
			}
		}, new Takt(5, "x..x..x.", "..x..x.x", "xxxxxxxx", "a...d...", null));

		neu("trommelwirbel", "Trommelwirbel", "Wild auf unsichtbare Trommeln hauen", 0xFFFF2BD6, true, 0, (p, t) -> {
			float s = sin(t, 4f);
			p.neigung = 12;
			p.rArmX = -1.0f + 0.4f * Math.max(0, s);
			p.lArmX = -1.0f + 0.4f * Math.max(0, -s);
			p.rArmZ = -0.15f;
			p.lArmZ = 0.15f;
			p.kopfX = 0.2f + 0.1f * sin(t, 2f);
			p.rBeinX = -0.15f;
			p.lBeinX = 0.15f;
		}, new Takt(4, "x..x..x.", ".x..x..x", "x.x.x.x.", null, null));

		neu("umarmung", "Umarmung", "Eine große Umarmung für alle", 0xFFFF6BA8, false, 3.2f, (p, t) -> {
			float rein = Math.min(1f, t * 3);
			p.rArmX = -1.45f * rein;
			p.lArmX = -1.45f * rein;
			p.rArmZ = -0.55f * rein;
			p.lArmZ = 0.55f * rein;
			p.koerperZ = 0.08f * sin(t, 0.7f) * rein;
			p.kopfZ = 0.15f * rein;
		}, null, (mc, s, t, nr) -> {
			if (nr % 16 == 4) partikel(mc, s, net.minecraft.core.particles.ParticleTypes.HEART, z(0.4f), 2.2, z(0.4f), 0, 0.05, 0);
		});

		neu("tauchgang", "Tauchgang", "Abtauchen und mit Brustschwimmen davon", 0xFF2B6BFF, true, 0, (p, t) -> {
			float zug = 0.5f + 0.5f * sin(t, 0.8f);
			p.neigung = 80;
			p.hoehe = 0.15f;
			p.rArmX = -3.0f + 0.3f * zug;
			p.lArmX = -3.0f + 0.3f * zug;
			p.rArmZ = 1.3f * zug;
			p.lArmZ = -1.3f * zug;
			p.rBeinX = 0.35f * sin(t, 2f);
			p.lBeinX = -0.35f * sin(t, 2f);
			p.kopfX = -0.9f;
		}, null, (mc, s, t, nr) -> {
			if (nr % 3 == 0) partikel(mc, s, net.minecraft.core.particles.ParticleTypes.BUBBLE_POP, z(0.5f), 1.0 + z(0.3f), z(0.5f), 0, 0.06, 0);
		});

		neu("lhopser", "L-Hopser", "Das L an die Stirn und seitlich hüpfen", 0xFF7DF9FF, true, 0, (p, t) -> {
			float s = sin(t, 0.9f);
			p.rArmX = -2.6f;
			p.rArmZ = -0.5f;
			p.lArmX = 0.4f * s;
			p.lArmZ = -0.3f;
			p.hoehe = 0.15f * hops(t, 1.8f);
			p.rBeinZ = 0.35f * Math.max(0, s);
			p.lBeinZ = -0.35f * Math.max(0, -s);
			p.drehung = 10 * s;
		}, new Takt(5, "x...x...", "....x...", "x.x.x.x.", "a.a.a.f.", null));

		neu("tiefenentspannt", "Tiefenentspannt", "Hände hinter den Kopf und ganz entspannt", 0xFF8DACD4, true, 0, (p, t) -> {
			p.rArmX = -2.6f;
			p.lArmX = -2.6f;
			p.rArmZ = -0.95f;
			p.lArmZ = 0.95f;
			p.neigung = -6;
			p.koerperZ = 0.06f * sin(t, 0.35f);
			p.kopfY = 0.25f * sin(t, 0.18f);
			p.kopfX = -0.1f;
			p.rBeinZ = 0.08f;
			p.lBeinZ = -0.08f;
		}, null);

		neu("aura", "Aura-Pose", "Arme verschränkt, ein Leuchten wirbelt um dich", 0xFFFFE680, true, 0, (p, t) -> {
			p.rArmX = -0.95f;
			p.lArmX = -0.95f;
			p.rArmZ = -0.75f;
			p.lArmZ = 0.75f;
			p.kopfX = -0.15f + 0.05f * sin(t, 0.4f);
			p.kopfY = 0.15f * sin(t, 0.2f);
			p.rBeinZ = 0.12f;
			p.lBeinZ = -0.12f;
		}, null, (mc, s, t, nr) -> {
			double w = t * 4;   // zwei Leuchtspuren schrauben sich um dich nach oben
			for (int i = 0; i < 2; i++) {
				double a = w + i * Math.PI;
				partikel(mc, s, net.minecraft.core.particles.ParticleTypes.END_ROD, Math.cos(a) * 0.8, (t * 0.9) % 2.2, Math.sin(a) * 0.8, 0, 0.01, 0);
			}
		});

		neu("gruselgroove", "Grusel-Groove", "Schaurig und doch funky", 0xFF6BFFB0, true, 0, (p, t) -> {
			float s = sin(t, 0.9f);
			p.rArmX = -1.5f + 0.25f * sin(t, 1.8f);
			p.lArmX = -1.5f - 0.25f * sin(t, 1.8f);
			p.rArmZ = 0.1f * s;
			p.lArmZ = 0.1f * s;
			p.koerperZ = 0.14f * s;
			p.kopfZ = -0.3f * s;
			p.kopfX = 0.15f;
			p.hoehe = -0.08f + 0.05f * hops(t, 0.9f);
			p.rBeinX = -0.2f * Math.max(0, s);
			p.lBeinX = -0.2f * Math.max(0, -s);
		}, new Takt(6, "x..x..x.", "....x...", ".x.x.x.x", "a..a.c.d", "h...g.f."), (mc, s, t, nr) -> {
			if (nr % 10 == 0) partikel(mc, s, net.minecraft.core.particles.ParticleTypes.SOUL, z(0.6f), 0.2, z(0.6f), 0, 0.04, 0);
		});

		neu("feuersprung", "Feuersprung", "Hochspringen, unten zünden die Flammen", 0xFFFF4D2B, true, 0, (p, t) -> {
			float h = hops(t, 0.7f);
			p.hoehe = 0.5f * h;
			p.arme(-2.9f * h, 0.4f);
			p.rBeinX = -0.6f * h;
			p.lBeinX = -0.6f * h;
			p.kopfX = -0.25f * h;
		}, new Takt(4, "x.x.x.x.", "..x...x.", "xxxxxxxx", "a.a.c.c.", null), (mc, s, t, nr) -> {
			if (nr % 14 == 0) {   // bei jeder Landung ein Flammenring
				for (int i = 0; i < 16; i++) {
					double a = i * Math.PI / 8;
					partikel(mc, s, net.minecraft.core.particles.ParticleTypes.FLAME, Math.cos(a) * 0.6, 0.1, Math.sin(a) * 0.6,
							Math.cos(a) * 0.05, 0.02, Math.sin(a) * 0.05);
				}
			}
			if (nr % 3 == 0) partikel(mc, s, net.minecraft.core.particles.ParticleTypes.SMALL_FLAME, z(0.4f), 0.1, z(0.4f), 0, 0.03, 0);
		});

		neu("paddelkreisel", "Paddel-Kreisel", "Sitzen, paddeln und sich im Kreis drehen", 0xFF38BDF8, true, 0, (p, t) -> {
			float seite = sin(t, 0.5f) > 0 ? 1 : -1;   // abwechselnd links und rechts paddeln
			float zug = sin(t, 1f);
			p.hoehe = -0.62f;
			p.rBeinX = -1.45f;
			p.lBeinX = -1.45f;
			p.rArmX = -0.9f + 0.6f * zug;
			p.lArmX = -0.9f - 0.6f * zug;
			p.rArmZ = 0.5f * seite;
			p.lArmZ = 0.5f * seite;
			p.koerperZ = 0.12f * seite;
			p.drehung = 360 * t * 0.45f;
			p.kopfZ = -0.1f * seite;
		}, null, (mc, s, t, nr) -> {
			if (nr % 4 == 0) partikel(mc, s, net.minecraft.core.particles.ParticleTypes.SPLASH, z(0.9f), 0.2, z(0.9f), 0, 0.1, 0);
		});
	}

	public static Emote nachId(String id) {
		return NACH_ID.get(id);
	}

	// ================================================================ Rad-Belegung

	/** Die 8 Plätze des Rads (Ids, leer = frei). */
	public static String[] rad() {
		String gespeichert = BambusClient.config.emoteRad;
		String[] teile = (gespeichert == null || gespeichert.isBlank() ? STANDARD_RAD : gespeichert).split(",", -1);
		String[] rad = new String[PLAETZE];
		Arrays.fill(rad, "");
		for (int i = 0; i < Math.min(PLAETZE, teile.length); i++) rad[i] = NACH_ID.containsKey(teile[i].trim()) ? teile[i].trim() : "";
		return rad;
	}

	public static void setzePlatz(int platz, String id) {
		String[] rad = rad();
		rad[Math.floorMod(platz, PLAETZE)] = id == null || !NACH_ID.containsKey(id) ? "" : id;
		BambusClient.config.emoteRad = String.join(",", rad);
		BambusClient.config.speichern();
	}

	/** In der Garderobe gewählter Platz des Rads (0–7). */
	public static int gewaehlterPlatz = 0;

	// ================================================================ Abspielen

	private static Emote aktiv;
	private static long seit;
	private static CameraType alteKamera;
	private static int letzterSchritt = -1;
	private static int effektTick;

	/** Vorschau in Menüs (Garderobe, Rad): tanzt nur das Modell im Menü. */
	private static Emote vorschau;
	private static long vorschauSeit;

	public static Emote aktiv() {
		return aktiv;
	}

	public static void setzeVorschau(Emote e) {
		if (e != vorschau) {
			vorschau = e;
			vorschauSeit = System.currentTimeMillis();
		}
	}

	/** Noch nicht mit Blättern freigeschaltet? */
	public static boolean gesperrt(Emote e) {
		return e != null && de.bambus.client.Blaetter.gesperrt("emote", e.id());
	}

	public static int preis(Emote e) {
		return de.bambus.client.Blaetter.preis("emote", e.id());
	}

	public static void starte(Emote e) {
		Minecraft mc = Minecraft.getInstance();
		if (e == null || mc.player == null) return;
		if (gesperrt(e)) {
			de.bambus.client.Hinweise.zeige("stern", e.name() + " ist noch gesperrt",
					"In der Garderobe für " + preis(e) + " Blätter freischalten", de.bambus.client.Farben.NEON_CYAN);
			return;
		}
		if (aktiv == null) {
			alteKamera = mc.options.getCameraType();
			if (alteKamera.isFirstPerson()) mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
		}
		aktiv = e;
		seit = System.currentTimeMillis();
		letzterSchritt = -1;
	}

	public static void stoppe() {
		if (aktiv == null) return;
		aktiv = null;
		Minecraft mc = Minecraft.getInstance();
		if (alteKamera != null && mc.options.getCameraType() == CameraType.THIRD_PERSON_FRONT) {
			mc.options.setCameraType(alteKamera);
		}
		alteKamera = null;
	}

	/** Jeden Tick: beenden, wenn man sich bewegt, und den Beat spielen. */
	public static void tick(Minecraft mc) {
		if (aktiv == null) return;
		if (mc.player == null || mc.player.hurtTime > 0) {
			stoppe();
			return;
		}
		var o = mc.options;
		if (o.keyUp.isDown() || o.keyDown.isDown() || o.keyLeft.isDown() || o.keyRight.isDown() || o.keyJump.isDown()
				|| o.keyShift.isDown() || o.keyAttack.isDown() || o.keyUse.isDown()) {
			stoppe();
			return;
		}
		float t = (System.currentTimeMillis() - seit) / 1000f;
		if (!aktiv.schleife() && t > aktiv.dauer()) {
			stoppe();
			return;
		}
		if (aktiv.effekt() != null) aktiv.effekt().tick(mc, mc.player, t, effektTick++);
		Takt takt = aktiv.takt();
		if (takt != null && BambusClient.config.emoteMusik) {
			int schritt = (int) (t * 20 / takt.ticksProAchtel());
			if (schritt != letzterSchritt) {
				letzterSchritt = schritt;
				takt.spiele(schritt);
			}
		}
	}

	// ================================================================ Auf das Modell anwenden

	private static final Pose POSE = new Pose();

	/** Für das nächste gezeichnete Spielermodell im Menü: dieses Emote zeigen (danach wieder null setzen). */
	private static Emote erzwungen;
	private static float erzwungenT;

	public static void erzwinge(Emote e, float t) {
		erzwungen = e;
		erzwungenT = t;
	}

	/** Sekunden seit dem Wechsel der Vorschau (für das große Modell in Garderobe und Rad). */
	public static float vorschauZeit() {
		return (System.currentTimeMillis() - vorschauSeit) / 1000f;
	}

	public static Emote vorschau() {
		return vorschau;
	}

	/** Beim Auslesen des Zustands: Welches Emote zeigt dieses Modell? Nur der eigene Spieler tanzt. */
	public static void stempeln(AvatarRenderState s, int entityId) {
		EmoteZustand z = (EmoteZustand) s;
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || entityId != mc.player.getId()) {
			z.bambus$setze(null, 0);
		} else if (erzwungen != null) {
			z.bambus$setze(erzwungen.id(), erzwungenT);
		} else if (aktiv != null && mc.screen == null) {
			z.bambus$setze(aktiv.id(), (System.currentTimeMillis() - seit) / 1000f);
		} else {
			z.bambus$setze(null, 0);
		}
	}

	/** Haltung für ein Modell – null, wenn es kein Emote zeigt. */
	public static Pose poseFuer(AvatarRenderState s) {
		EmoteZustand z = (EmoteZustand) s;
		Emote e = z.bambus$emote() == null ? null : NACH_ID.get(z.bambus$emote());
		if (e == null) return null;
		float t = z.bambus$zeit();
		if (!e.schleife()) t = t % (e.dauer() + 0.8f);   // einmalige Emotes in Menüs wiederholen
		return e.pose(t, POSE);
	}

	/** Aus dem Mixin in PlayerModel.setupAnim: Gelenke überschreiben. */
	public static void anwenden(PlayerModel m, AvatarRenderState s) {
		Pose p = poseFuer(s);
		if (p == null) return;
		m.head.xRot = p.kopfX;
		m.head.yRot = p.kopfY;
		m.head.zRot = p.kopfZ;
		m.body.xRot = p.koerperX;
		m.body.yRot = p.koerperY;
		m.body.zRot = p.koerperZ;
		m.rightArm.xRot = p.rArmX;
		m.rightArm.yRot = p.rArmY;
		m.rightArm.zRot = p.rArmZ;
		m.leftArm.xRot = p.lArmX;
		m.leftArm.yRot = p.lArmY;
		m.leftArm.zRot = p.lArmZ;
		m.rightLeg.xRot = p.rBeinX;
		m.rightLeg.yRot = p.rBeinY;
		m.rightLeg.zRot = p.rBeinZ;
		m.leftLeg.xRot = p.lBeinX;
		m.leftLeg.yRot = p.lBeinY;
		m.leftLeg.zRot = p.lBeinZ;
	}
}
