package de.bambus.client.screen;

import java.util.HashMap;
import java.util.Map;

/**
 * Weiche Übergänge für die Oberfläche: Werte gleiten bildratenunabhängig zu ihrem Ziel
 * (Schalter, Tab-Unterstrich, Hover-Leuchten, Fenster).
 */
public final class Anim {

	private static final Map<String, double[]> WERTE = new HashMap<>();

	// Einheitliche Zeiten für alle Bambus-Menüs – kurz gehalten, damit die Oberfläche sofort reagiert
	/** Fenster einblenden (ms). */
	public static final float OEFFNEN_MS = 150f;
	/** Fenster ausblenden, bevor es wirklich schließt (ms). */
	public static final float SCHLIESSEN_MS = 90f;
	/** Inhalt beim Tab- oder Kategorie-Wechsel einblenden (ms). */
	public static final float WECHSEL_MS = 120f;
	/** Tempo für Hover-Leuchten und Schalter (höher = schneller, siehe zu()). */
	public static final float HOVER = 24f;
	/** Tempo für gleitende Markierungen (Tab-Unterstrich, Auswahlbalken). */
	public static final float GLEITEN = 24f;

	private Anim() {}

	/** Gleitet zum Ziel. tempo ≈ wie schnell (12 = flott, 20 = sehr schnell). */
	public static float zu(String schluessel, float ziel, float tempo) {
		long jetzt = System.nanoTime();
		double[] w = WERTE.get(schluessel);
		if (w == null) {
			WERTE.put(schluessel, new double[]{ziel, jetzt});
			return ziel;
		}
		double dt = Math.min(0.1, Math.max(0, (jetzt - w[1]) / 1e9));
		w[0] += (ziel - w[0]) * (1 - Math.exp(-tempo * dt));
		if (Math.abs(ziel - w[0]) < 0.001) w[0] = ziel;
		w[1] = jetzt;
		return (float) w[0];
	}

	public static void setze(String schluessel, float wert) {
		WERTE.put(schluessel, new double[]{wert, System.nanoTime()});
	}

	/** Weiches Abbremsen (0..1 → 0..1). */
	public static float aus(float k) {
		k = Math.max(0, Math.min(1, k));
		return 1 - (1 - k) * (1 - k) * (1 - k);
	}

	/** Mit kleinem Überschwingen am Ende (für einfahrende Karten). */
	public static float zurueck(float k) {
		k = Math.max(0, Math.min(1, k)) - 1;
		float s = 1.6f;
		return 1 + k * k * ((s + 1) * k + s);
	}

	/** Mischt zwei ARGB-Farben. */
	public static int mische(int a, int b, float t) {
		t = Math.max(0, Math.min(1, t));
		int aa = a >>> 24, ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
		int ba = b >>> 24, br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
		return (Math.round(aa + (ba - aa) * t) << 24) | (Math.round(ar + (br - ar) * t) << 16)
				| (Math.round(ag + (bg - ag) * t) << 8) | Math.round(ab + (bb - ab) * t);
	}
}
