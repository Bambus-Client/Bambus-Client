package de.bambus.launcher;

import de.bambus.quests.Quests;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.Properties;

/**
 * Das Spieler-Profil oben rechts: Name, Profilbild (Skin-Kopf), Rang (ADMIN/MITGLIED + Spiel-Rang),
 * Statistiken und die freiwillige Launcher-PIN.
 *
 * Wer bist du? In dieser Reihenfolge:
 *  1. Eigene Microsoft-Anmeldung im Launcher – bestätigt.
 *  2. Das angemeldete Konto im offiziellen Minecraft Launcher – bestätigt (das Konto auf diesem Computer).
 *  3. Ein eingetragener Minecraft-Name (oder der Name aus der Willkommens-Seite) – nicht bestätigt.
 *
 * ADMIN gibt es nur für bestätigte Konten aus der Team-Liste ({@link Quests#istEntwickler}) – wer nur den
 * Namen eintippt, wird nie Admin. Bambus hat keinen eigenen Server und kein eigenes Konto: Alles bleibt auf
 * diesem Computer (~/.bambus-launcher/profil.properties).
 *
 * Die PIN ist eine einfache Sperre gegen neugierige Mitspieler (z. B. Geschwister) – kein Ersatz für das
 * Benutzerkonto des Computers. Gespeichert wird nur ein PBKDF2-Hash mit Salz, nie die PIN selbst.
 */
final class Profil {

	enum Quelle { MICROSOFT, LAUNCHER, EINGETRAGEN, KEINE }

	static final Path ORDNER = Paths.get(System.getProperty("user.home"), ".bambus-launcher");
	private static final Path DATEI = ORDNER.resolve("profil.properties");

	private static final int PIN_RUNDEN = 120_000;

	Quelle quelle = Quelle.KEINE;
	String name = "";
	/** UUID des Kontos, falls bekannt – damit lädt der echte Skin auch nach einer Namensänderung. */
	String uuid = "";
	BufferedImage kopf;

	private Profil() {}

	/** Für Tests: Profil mit fester Quelle und festem Namen. */
	static Profil mit(Quelle quelle, String name) {
		Profil p = new Profil();
		p.quelle = quelle;
		p.name = name == null ? "" : name;
		return p;
	}

	// ================================================================== Wer bist du?

	/** Ermittelt das aktuelle Profil (ohne Internet, schnell). */
	static Profil ermittle(BambusLauncher l) {
		Profil p = new Profil();
		if (l.konto.angemeldet() && !l.konto.name().isEmpty()) {
			p.quelle = Quelle.MICROSOFT;
			p.name = l.konto.name();
			p.uuid = l.konto.uuid();
		} else {
			String[] offiziell = new SkinLader(l.installer.mcOrdner, l.installer.spielOrdner).kontoAusLauncher();
			if (offiziell != null && offiziell[1] != null && !offiziell[1].isEmpty()) {
				p.quelle = Quelle.LAUNCHER;
				p.name = offiziell[1];
				p.uuid = offiziell[0] == null ? "" : offiziell[0];
			} else {
				String n = eingetragenerName();
				if (n.isEmpty()) {
					SkinLader.Skin s = new SkinLader(l.installer.mcOrdner, l.installer.spielOrdner).ausCache();
					if (s != null && s.name != null && s.name.matches("[A-Za-z0-9_]{1,16}")) n = s.name;
				}
				if (!n.isEmpty()) {
					p.quelle = Quelle.EINGETRAGEN;
					p.name = n;
				}
			}
		}
		p.kopf = kopfAusCache(p.name);
		return p;
	}

	boolean bestaetigt() { return quelle == Quelle.MICROSOFT || quelle == Quelle.LAUNCHER; }

	boolean hatName() { return quelle != Quelle.KEINE; }

	/** ADMIN nur für bestätigte Konten aus der Team-Liste. */
	boolean admin() { return bestaetigt() && Quests.istEntwickler(name); }

	String rolle(boolean en) {
		if (!hatName()) return "";
		return admin() ? "ADMIN" : en ? "MEMBER" : "MITGLIED";
	}

	String quelleText(boolean en) {
		switch (quelle) {
			case MICROSOFT: return en ? "Confirmed – signed in with Microsoft" : "Bestätigt – mit Microsoft angemeldet";
			case LAUNCHER: return en ? "Confirmed – account of the official Minecraft Launcher"
					: "Bestätigt – Konto aus dem offiziellen Minecraft Launcher";
			case EINGETRAGEN: return en ? "Not confirmed – name entered by hand" : "Nicht bestätigt – Name von Hand eingetragen";
			default: return en ? "Not signed in" : "Nicht angemeldet";
		}
	}

	// ================================================================== eingetragener Name

	static String eingetragenerName() {
		return lade().getProperty("name", "").trim();
	}

	static void setzeName(String name) throws IOException {
		Properties p = lade();
		if (name == null || name.trim().isEmpty()) p.remove("name");
		else p.setProperty("name", name.trim());
		speichere(p);
	}

	static boolean gueltigerName(String n) {
		return n != null && n.trim().matches("[A-Za-z0-9_]{3,16}");
	}

	// ================================================================== Profilbild

	/** Ordner mit eigenem Skin-Zwischenspeicher – damit die Vorschau in der Garderobe unberührt bleibt. */
	private static Path skinOrdner() { return ORDNER.resolve("profil"); }

	private static BufferedImage kopfAusCache(String name) {
		if (name == null || name.isEmpty()) return kopf(SkinLader.standard().bild);
		SkinLader.Skin s = new SkinLader(skinOrdner(), skinOrdner()).ausCache();
		if (s != null && s.name != null && s.name.equalsIgnoreCase(name)) return kopf(s.bild);
		return null;   // noch nicht geladen
	}

	/** Lädt den Skin-Kopf aus dem Internet (im Hintergrund aufrufen). */
	void ladeKopf() throws IOException {
		if (!hatName()) {
			kopf = kopf(SkinLader.standard().bild);
			return;
		}
		Files.createDirectories(skinOrdner());
		// über die UUID (fest, auch nach Namensänderung), sonst über den Namen
		String id = uuid != null && uuid.replace("-", "").matches("[0-9a-fA-F]{32}") ? uuid : name;
		SkinLader.Skin s = new SkinLader(skinOrdner(), skinOrdner()).lade(id);
		kopf = kopf(s.bild);
	}

	/** Gesicht (8×8) mit Hut-Ebene aus einem 64×64-Skin. */
	static BufferedImage kopf(BufferedImage skin) {
		BufferedImage k = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
		if (skin == null || skin.getWidth() < 64) return k;
		Graphics2D g = k.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		g.drawImage(skin, 0, 0, 8, 8, 8, 8, 16, 16, null);
		g.drawImage(skin, 0, 0, 8, 8, 40, 8, 48, 16, null);
		g.dispose();
		return k;
	}

	/** Zeichnet ein Profilbild pixelgenau (ohne Verwischen). */
	static void zeichneKopf(Graphics2D g, BufferedImage kopf, int x, int y, int groesse) {
		BufferedImage k = kopf != null ? kopf : kopf(SkinLader.standard().bild);
		Object alt = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		g.drawImage(k, x, y, groesse, groesse, null);
		if (alt != null) g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, alt);
	}

	// ================================================================== Statistiken

	/** Statistik des Clients (config/bambus-statistik.json) – über alle Spielordner der Versionen zusammengezählt. */
	static long[] spielStatistik(Path spielOrdner) {
		long[] s = new long[6];   // kills, killsSpieler, tode, besteCombo, spielSekunden, sitzungen
		addiere(spielOrdner.resolve("config").resolve("bambus-statistik.json"), s);
		Path versionen = spielOrdner.resolve("versionen");
		if (Files.isDirectory(versionen)) {
			try (DirectoryStream<Path> d = Files.newDirectoryStream(versionen)) {
				for (Path v : d) addiere(v.resolve("config").resolve("bambus-statistik.json"), s);
			} catch (IOException ignoriert) {
				// dann nur die Hauptversion
			}
		}
		return s;
	}

	private static void addiere(Path datei, long[] s) {
		if (!Files.isRegularFile(datei)) return;
		try {
			Map<String, Object> o = Json.objekt(Json.parse(new String(Files.readAllBytes(datei), StandardCharsets.UTF_8)));
			if (o == null) return;
			s[0] += zahl(o.get("kills"));
			s[1] += zahl(o.get("killsSpieler"));
			s[2] += zahl(o.get("tode"));
			s[3] = Math.max(s[3], zahl(o.get("besteCombo")));
			s[4] += zahl(o.get("spielSekunden"));
			s[5] += zahl(o.get("sitzungen"));
		} catch (Exception ignoriert) {
			// kaputte Datei – überspringen
		}
	}

	private static long zahl(Object o) {
		return o instanceof Number ? ((Number) o).longValue() : 0;
	}

	static String kd(long kills, long tode) {
		return tode == 0 ? String.valueOf(kills) : String.format(java.util.Locale.ROOT, "%.2f", kills / (double) tode);
	}

	// ================================================================== PIN

	static boolean pinGesetzt() {
		Properties p = lade();
		return p.getProperty("pinHash") != null && p.getProperty("pinSalz") != null;
	}

	static boolean gueltigePin(char[] pin) {
		if (pin == null || pin.length < 4 || pin.length > 12) return false;
		for (char c : pin) if (c < '0' || c > '9') return false;
		return true;
	}

	static void setzePin(char[] pin) throws IOException {
		if (!gueltigePin(pin)) throw new IOException("Die PIN muss aus 4 bis 12 Ziffern bestehen.");
		byte[] salz = new byte[16];
		new SecureRandom().nextBytes(salz);
		Properties p = lade();
		p.setProperty("pinSalz", Base64.getEncoder().encodeToString(salz));
		p.setProperty("pinRunden", String.valueOf(PIN_RUNDEN));
		p.setProperty("pinHash", Base64.getEncoder().encodeToString(hash(pin, salz, PIN_RUNDEN)));
		speichere(p);
	}

	static boolean pruefePin(char[] pin) {
		Properties p = lade();
		try {
			byte[] salz = Base64.getDecoder().decode(p.getProperty("pinSalz", ""));
			byte[] soll = Base64.getDecoder().decode(p.getProperty("pinHash", ""));
			int runden = Integer.parseInt(p.getProperty("pinRunden", String.valueOf(PIN_RUNDEN)));
			if (salz.length == 0 || soll.length == 0) return false;
			return MessageDigest.isEqual(soll, hash(pin, salz, runden));
		} catch (Exception e) {
			return false;
		}
	}

	static void entfernePin() throws IOException {
		Properties p = lade();
		p.remove("pinSalz");
		p.remove("pinHash");
		p.remove("pinRunden");
		speichere(p);
	}

	private static byte[] hash(char[] pin, byte[] salz, int runden) throws IOException {
		try {
			PBEKeySpec spec = new PBEKeySpec(pin, salz, runden, 256);
			try {
				return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
			} finally {
				spec.clearPassword();
			}
		} catch (Exception e) {
			throw new IOException("PIN konnte nicht verarbeitet werden.");
		}
	}

	static void loesche(char[] c) {
		if (c != null) Arrays.fill(c, '\0');
	}

	// ================================================================== Datei

	private static Properties lade() {
		Properties p = new Properties();
		if (Files.isRegularFile(DATEI)) {
			try (InputStream in = Files.newInputStream(DATEI); Reader r = new java.io.InputStreamReader(in, StandardCharsets.UTF_8)) {
				p.load(r);
			} catch (IOException ignoriert) {
				// leer
			}
		}
		return p;
	}

	private static void speichere(Properties p) throws IOException {
		Files.createDirectories(ORDNER);
		StringWriter w = new StringWriter();
		p.store(w, "Bambus-Profil (nur auf diesem Computer)");
		Installer.schreibeSicher(DATEI, w.toString().getBytes(StandardCharsets.UTF_8));
	}
}
