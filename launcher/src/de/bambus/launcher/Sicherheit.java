package de.bambus.launcher;

import java.io.IOException;
import java.net.URI;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Schutz für alles, was der Launcher aus dem Internet holt.
 *
 *  - Updates: Die version.json im Branch auto-update ist mit dem geheimen Bambus-Schlüssel unterschrieben
 *    (GitHub-Secret BAMBUS_SIGNIER_SCHLUESSEL, nur der Workflow „Auto-Update verteilen“ benutzt ihn).
 *    Der Launcher nimmt ein Update nur an, wenn die Unterschrift zu einem der hier eingebauten öffentlichen
 *    Schlüssel passt. Die version.json enthält die SHA-256-Prüfsumme der neuen BambusLauncher.jar –
 *    damit ist auch das Programm selbst abgesichert.
 *  - Mods: Prüfsummen von Modrinth und CurseForge werden kontrolliert, Dateinamen bereinigt.
 *  - Downloads nur über https (außer zum Testen auf dem eigenen Rechner).
 *
 * Mehrere öffentliche Schlüssel sind möglich: Soll der Schlüssel einmal gewechselt werden, zuerst den neuen
 * hier zusätzlich eintragen und verteilen, erst danach im Secret austauschen (siehe SICHERHEIT.md).
 */
final class Sicherheit {

	private Sicherheit() {}

	/** Öffentliche Schlüssel (RSA 3072, X.509, Base64) – dürfen öffentlich sein. */
	private static final String[] OEFFENTLICH = {
			// HIER_OEFFENTLICHER_SCHLUESSEL – wird eingetragen, sobald der Schlüssel erzeugt ist (siehe SICHERHEIT.md)
	};

	// ---------------------------------------------------------- Unterschrift

	/** Stimmt die Unterschrift (Base64, SHA256withRSA) über genau diese Bytes? */
	static boolean echt(byte[] daten, String signaturBase64) {
		if (daten == null || signaturBase64 == null) return false;
		byte[] sig;
		try {
			sig = Base64.getMimeDecoder().decode(signaturBase64.trim());
		} catch (IllegalArgumentException e) {
			return false;
		}
		for (String k : OEFFENTLICH) {
			try {
				PublicKey pub = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(k)));
				Signature s = Signature.getInstance("SHA256withRSA");
				s.initVerify(pub);
				s.update(daten);
				if (s.verify(sig)) return true;
			} catch (Exception ignoriert) {
				// nächster Schlüssel
			}
		}
		return false;
	}

	// ---------------------------------------------------------- Adressen

	/** Nur https – http nur für Tests auf dem eigenen Rechner. */
	static void pruefeAdresse(String adresse) throws IOException {
		URI u;
		try {
			u = URI.create(adresse);
		} catch (IllegalArgumentException e) {
			throw new IOException("Ungültige Adresse: " + adresse);
		}
		String schema = u.getScheme() == null ? "" : u.getScheme().toLowerCase(Locale.ROOT);
		String host = u.getHost() == null ? "" : u.getHost().toLowerCase(Locale.ROOT);
		if ("https".equals(schema)) return;
		if ("http".equals(schema) && ("localhost".equals(host) || "127.0.0.1".equals(host))) return;
		throw new IOException("Unsichere Verbindung abgelehnt (nur https): " + adresse);
	}

	// ---------------------------------------------------------- Dateien

	/** Dateiname aus dem Internet ohne Ordner-Tricks: nur ein Name, endet auf .jar, sonst Ersatz. */
	static String dateiname(String name, String ersatz) {
		if (name == null) return ersatz;
		String n = name.replace('\\', '/');
		n = n.substring(n.lastIndexOf('/') + 1).trim();
		if (n.isEmpty() || n.startsWith(".") || n.length() > 150 || !n.toLowerCase(Locale.ROOT).endsWith(".jar")
				|| !n.matches("[\\p{L}\\p{N} ._+()\\[\\]-]+")) {
			return ersatz;
		}
		return n;
	}

	/** Modrinth: {"sha512": "...", "sha1": "..."} – passt die Datei? */
	static void pruefeModrinth(byte[] daten, Object hashes, String name) throws IOException {
		Map<String, Object> h = Json.objekt(hashes);
		if (h == null) return;
		if (h.get("sha512") instanceof String) vergleiche(daten, "SHA-512", (String) h.get("sha512"), name);
		else if (h.get("sha1") instanceof String) vergleiche(daten, "SHA-1", (String) h.get("sha1"), name);
	}

	/** CurseForge: [{"value": "...", "algo": 1}] – 1 = SHA-1. */
	static void pruefeCurseforge(byte[] daten, Object hashes, String name) throws IOException {
		List<Object> l = Json.liste(hashes);
		if (l == null) return;
		for (Object o : l) {
			Map<String, Object> h = Json.objekt(o);
			if (h != null && h.get("algo") instanceof Number && ((Number) h.get("algo")).intValue() == 1
					&& h.get("value") instanceof String) {
				vergleiche(daten, "SHA-1", (String) h.get("value"), name);
				return;
			}
		}
	}

	private static void vergleiche(byte[] daten, String algo, String erwartet, String name) throws IOException {
		if (!erwartet.equalsIgnoreCase(hex(algo, daten))) {
			throw new IOException(name + " ist beschädigt oder wurde verändert – nicht installiert.");
		}
	}

	static String hex(String algo, byte[] d) {
		try {
			StringBuilder b = new StringBuilder();
			for (byte x : MessageDigest.getInstance(algo).digest(d)) b.append(String.format("%02x", x));
			return b.toString();
		} catch (Exception e) {
			return "";
		}
	}
}
