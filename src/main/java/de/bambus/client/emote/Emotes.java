package de.bambus.client.emote;

import de.bambus.client.BambusClient;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

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
	public static final String STANDARD_RAD = "winken,jubeln,wackler,roboter,disco,hampelmann,kreisel,chillen";
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
		Emote e = new Emote(id, name, text, farbe, schleife, dauer, b, takt);
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

	public static void starte(Emote e) {
		Minecraft mc = Minecraft.getInstance();
		if (e == null || mc.player == null) return;
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

	/** Haltung für den eigenen Spieler – null, wenn gerade kein Emote (und keine Vorschau) läuft. */
	public static Pose poseFuer(int entityId) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || entityId != mc.player.getId()) return null;
		Emote e = aktiv;
		long start = seit;
		if (vorschau != null && mc.screen != null) {   // in Garderobe und Rad tanzt das Modell im Menü
			e = vorschau;
			start = vorschauSeit;
		}
		if (e == null) return null;
		float t = (System.currentTimeMillis() - start) / 1000f;
		if (!e.schleife() && e == vorschau) t = t % (e.dauer() + 0.8f);   // Vorschau einmaliger Emotes wiederholen
		return e.pose(t, POSE);
	}

	/** Aus dem Mixin in PlayerModel.setupAnim: Gelenke überschreiben. */
	public static void anwenden(PlayerModel m, AvatarRenderState s) {
		Pose p = poseFuer(s.id);
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
