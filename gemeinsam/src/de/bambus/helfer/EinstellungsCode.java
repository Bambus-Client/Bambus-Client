package de.bambus.helfer;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * Einstellungs-Code zum Teilen: „BAMBUS-“ + komprimiertes JSON in Base64 (URL-sicher).
 * Was im JSON steht, entscheiden Mod bzw. Launcher – hier nur Verpacken und Entpacken.
 * Gemeinsamer Code für Mod und Launcher: Java-8-kompatibel halten.
 */
public final class EinstellungsCode {

	public static final String VORSATZ = "BAMBUS-";
	/** Schutz vor riesigen Eingaben (entpackt höchstens 1 MB). */
	private static final int MAX = 1 << 20;

	private EinstellungsCode() {}

	public static String verpacken(String json) {
		byte[] roh = json.getBytes(StandardCharsets.UTF_8);
		Deflater d = new Deflater(Deflater.BEST_COMPRESSION);
		d.setInput(roh);
		d.finish();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] puffer = new byte[4096];
		while (!d.finished()) {
			out.write(puffer, 0, d.deflate(puffer));
		}
		d.end();
		return VORSATZ + Base64.getUrlEncoder().withoutPadding().encodeToString(out.toByteArray());
	}

	/**
	 * Entpackt einen Code zu JSON. Leerzeichen und Zeilenumbrüche (z. B. aus einem Chat kopiert) werden ignoriert.
	 * Reines JSON (alter Export) wird unverändert zurückgegeben. Ungültig: null.
	 */
	public static String entpacken(String eingabe) {
		if (eingabe == null) return null;
		String text = eingabe.trim();
		if (text.startsWith("{")) return text;
		text = text.replaceAll("\\s+", "");
		if (!text.toUpperCase(java.util.Locale.ROOT).startsWith(VORSATZ)) return null;
		try {
			byte[] daten = Base64.getUrlDecoder().decode(text.substring(VORSATZ.length()));
			Inflater i = new Inflater();
			i.setInput(daten);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			byte[] puffer = new byte[4096];
			while (!i.finished()) {
				int n = i.inflate(puffer);
				if (n == 0 && (i.needsInput() || i.needsDictionary())) break;
				out.write(puffer, 0, n);
				if (out.size() > MAX) {
					i.end();
					return null;
				}
			}
			boolean fertig = i.finished();
			i.end();
			return fertig ? new String(out.toByteArray(), StandardCharsets.UTF_8) : null;
		} catch (Exception e) {
			return null;
		}
	}
}
