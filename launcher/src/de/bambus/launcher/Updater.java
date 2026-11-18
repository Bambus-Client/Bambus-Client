package de.bambus.launcher;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Selbst-Update über den Branch „auto-update“ des GitHub-Projekts.
 *
 * So funktioniert's:
 *  1. Bei jedem Push auf main baut GitHub Actions Launcher und Mod und legt sie mit einer version.json
 *     in den Branch auto-update (siehe .github/workflows/auto-update.yml).
 *  2. Der Launcher schaut beim Start und alle 30 Minuten in die version.json. Hat sie eine höhere
 *     Bau-Nummer als er selbst, lädt er die neue BambusLauncher.jar nach ~/.bambus-launcher/updates
 *     und prüft die SHA-256-Prüfsumme. Die version.json selbst ist unterschrieben (version.sig) –
 *     ohne passende Unterschrift wird nichts geladen ({@link Sicherheit}).
 *  3. Beim nächsten Öffnen startet der installierte Launcher direkt die neue Version ({@link Selbststart}) –
 *     die Mod steckt darin, kommt also automatisch mit. Nichts muss neu installiert werden.
 *
 * Es werden nur öffentliche Dateien von GitHub gelesen – kein Konto, keine persönlichen Daten.
 */
final class Updater {

	static final class Release {
		String version = "";
		int build;
		String commit = "";
		String text = "";
		String seite = "";
		String launcherUrl, launcherSha;
	}

	interface Fortschritt { void melde(String text); }

	static final String PROJEKT = "Bambus-Client/Bambus-Client";
	private static final String STANDARD_QUELLE = "https://raw.githubusercontent.com/" + PROJEKT + "/auto-update";
	static final Path ORDNER = Paths.get(System.getProperty("user.home"), ".bambus-launcher", "updates");
	private static final Pattern DATEI = Pattern.compile("BambusLauncher-(\\d+)\\.jar");

	private static Properties bauInfo;

	private Updater() {}

	/** Adresse des Update-Branches (zum Testen mit -Dbambus.updatequelle=… änderbar). */
	static String quelle() {
		String s = System.getProperty("bambus.updatequelle", STANDARD_QUELLE).trim();
		return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
	}

	/** Abschaltbar mit -Dbambus.selbstupdate=false (z. B. beim Entwickeln). */
	static boolean aktiv() { return !"false".equals(System.getProperty("bambus.selbstupdate")); }

	// ---------------------------------------------------------- eigene Version

	private static synchronized Properties bauInfo() {
		if (bauInfo == null) {
			bauInfo = new Properties();
			try (InputStream in = Updater.class.getResourceAsStream("/bambus-build.properties")) {
				if (in != null) bauInfo.load(in);
			} catch (IOException ignoriert) {
				// dann Bau 0
			}
		}
		return bauInfo;
	}

	/** Bau-Nummer dieses Launchers (Anzahl der Commits auf main) – 0 bei einem Entwickler-Bau ohne Git. */
	static int build() { return zahl(bauInfo().getProperty("build", "0")); }

	static String commit() { return bauInfo().getProperty("commit", ""); }

	/** „3.4.0 · Build 45“ */
	static String versionText() {
		return BambusLauncher.VERSION + (build() > 0 ? " · Build " + build() : "");
	}

	// ---------------------------------------------------------- nachfragen

	/** Liest die version.json aus dem Branch auto-update. */
	static Release pruefe() throws IOException {
		// kleiner Zusatz an der Adresse, damit kein veralteter Zwischenspeicher antwortet
		byte[] daten;
		try {
			daten = Installer.lade(quelle() + "/version.json?t=" + System.currentTimeMillis() / 60000);
		} catch (IOException ex) {
			throw new IOException("Keine Verbindung zum Update-Server.");
		}
		// Unterschrift prüfen: Nur Updates, die mit dem Bambus-Schlüssel unterschrieben sind, werden angenommen.
		String signatur;
		try {
			signatur = new String(Installer.lade(quelle() + "/version.sig?t=" + System.currentTimeMillis() / 60000),
					StandardCharsets.US_ASCII);
		} catch (IOException ex) {
			throw new IOException("Das Update ist nicht unterschrieben – es wird aus Sicherheitsgründen nicht geladen.");
		}
		if (!Sicherheit.echt(daten, signatur)) {
			throw new IOException("Die Unterschrift des Updates stimmt nicht – es wird aus Sicherheitsgründen nicht geladen.");
		}
		Map<String, Object> o = Json.objekt(Json.parse(new String(daten, StandardCharsets.UTF_8)));
		if (o == null) throw new IOException("Die Update-Daten sind unlesbar.");
		Release r = new Release();
		r.version = text(o.get("version"));
		r.build = zahl(text(o.get("build")));
		r.commit = text(o.get("commit"));
		r.seite = "https://github.com/" + PROJEKT + "/commits/main";
		List<Object> neu = Json.liste(o.get("aenderungen"));
		if (neu != null) {
			StringBuilder b = new StringBuilder();
			for (Object z : neu) b.append("- ").append(text(z)).append('\n');
			r.text = b.toString();
		}
		Map<String, Object> launcher = Json.objekt(o.get("launcher"));
		if (launcher != null) {
			String datei = text(launcher.get("datei"));
			if (datei.matches("[A-Za-z0-9_.-]+\\.jar")) {
				r.launcherUrl = quelle() + "/" + datei + "?b=" + r.build;
				r.launcherSha = text(launcher.get("sha256"));
			}
		}
		if (r.build <= 0 || r.launcherUrl == null || !r.launcherSha.matches("[0-9a-fA-F]{64}")) {
			throw new IOException("Die Update-Daten sind unvollständig.");
		}
		return r;
	}

	/** Neuer als der laufende Launcher? (Entwickler-Bauten ohne Nummer aktualisieren sich nie.) */
	static boolean neuer(Release r) { return r != null && build() > 0 && r.build > build(); }

	// ---------------------------------------------------------- laden

	/** Lädt die neue Launcher-Version nach updates/ (mit Prüfsumme). Gibt die Datei zurück. */
	static Path ladeLauncher(Release r, Fortschritt f) throws IOException {
		Path ziel = ORDNER.resolve("BambusLauncher-" + r.build + ".jar");
		if (Files.isRegularFile(ziel) && r.launcherSha.equalsIgnoreCase(sha256(Files.readAllBytes(ziel)))) {
			return ziel;   // schon da
		}
		if (f != null) f.melde("Update " + r.version + " (Build " + r.build + ") wird geladen …");
		byte[] daten = Installer.lade(r.launcherUrl);
		if (!r.launcherSha.equalsIgnoreCase(sha256(daten))) {
			throw new IOException("Das Update ist noch nicht vollständig auf dem Server – es wird später noch mal versucht.");
		}
		if (daten.length < 4 || daten[0] != 'P' || daten[1] != 'K') {
			throw new IOException("Die heruntergeladene Datei ist kein gültiger Launcher.");
		}
		Files.createDirectories(ORDNER);
		Path tmp = ORDNER.resolve(ziel.getFileName() + ".download");
		Files.write(tmp, daten);
		Files.move(tmp, ziel, StandardCopyOption.REPLACE_EXISTING);
		aufraeumen(r.build);
		return ziel;
	}

	/** Bau-Nummer der neuesten geladenen Version, die neuer als dieser Launcher ist – sonst 0. */
	static int bereitBuild() {
		Path p = bereit();
		return p == null ? 0 : nummer(p);
	}

	/** Die neueste geladene Launcher-Version, falls sie neuer ist als die laufende – sonst null. */
	static Path bereit() {
		Path beste = null;
		for (Path p : geladene()) {
			if (nummer(p) > build() && (beste == null || nummer(p) > nummer(beste))) beste = p;
		}
		return beste;
	}

	/** Alte geladene Versionen löschen (die laufende ist unter Windows gesperrt – dann eben beim nächsten Mal). */
	static void aufraeumen(int behalten) {
		for (Path p : geladene()) {
			if (nummer(p) != behalten) {
				try {
					Files.deleteIfExists(p);
					Files.deleteIfExists(ORDNER.resolve("versuch-" + nummer(p)));
					Files.deleteIfExists(ORDNER.resolve("ok-" + nummer(p)));
				} catch (IOException ignoriert) {
					// wird gerade benutzt
				}
			}
		}
		if (Files.isDirectory(ORDNER)) {
			try (DirectoryStream<Path> d = Files.newDirectoryStream(ORDNER, "*.kaputt")) {
				for (Path p : d) Files.deleteIfExists(p);
			} catch (IOException ignoriert) {
				// egal
			}
		}
		try {
			Files.deleteIfExists(ORDNER.resolve("bambus-client.jar"));   // aus dem alten Update-Weg
			Files.deleteIfExists(ORDNER.resolve("stand.json"));
		} catch (IOException ignoriert) {
			// egal
		}
	}

	static List<Path> geladene() {
		List<Path> l = new ArrayList<>();
		if (!Files.isDirectory(ORDNER)) return l;
		try (DirectoryStream<Path> d = Files.newDirectoryStream(ORDNER, "BambusLauncher-*.jar")) {
			for (Path p : d) if (nummer(p) > 0) l.add(p);
		} catch (IOException ignoriert) {
			// leer
		}
		return l;
	}

	static int nummer(Path p) {
		Matcher m = DATEI.matcher(p.getFileName().toString());
		return m.matches() ? zahl(m.group(1)) : 0;
	}

	// ---------------------------------------------------------- neu starten

	/** Datei, mit der der Launcher gestartet wurde (.exe oder .jar) – auch wenn gerade eine geladene Version läuft. */
	static Path startProgramm() {
		String exe = System.getProperty("jpackage.app-path");
		if (exe != null) return Paths.get(exe);
		String start = System.getProperty(Selbststart.START);
		try {
			Path p = start != null ? Paths.get(start)
					: Paths.get(Updater.class.getProtectionDomain().getCodeSource().getLocation().toURI());
			return Files.exists(p) ? p : null;
		} catch (Exception e) {
			return null;
		}
	}

	/** Startet den Launcher neu (er nimmt dabei die neueste Version). false = geht hier nicht. */
	static boolean neuStarten() {
		List<String> befehl = new ArrayList<>();
		String exe = System.getProperty("jpackage.app-path");
		if (exe != null) {
			befehl.add(exe);
		} else {
			Path p = startProgramm();
			if (p == null || !Files.isRegularFile(p) || !p.toString().toLowerCase(Locale.ROOT).endsWith(".jar")) return false;
			boolean win = "windows".equals(Installer.betriebssystem());
			Path java = Paths.get(System.getProperty("java.home"), "bin", win ? "javaw.exe" : "java");
			if (!Files.isRegularFile(java)) java = Paths.get(System.getProperty("java.home"), "bin", win ? "java.exe" : "java");
			befehl.add(java.toString());
			befehl.add("-jar");
			befehl.add(p.toString());
		}
		try {
			new ProcessBuilder(befehl).start();
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	// ---------------------------------------------------------- Helfer

	static String sha256(byte[] d) {
		try {
			StringBuilder b = new StringBuilder();
			for (byte x : MessageDigest.getInstance("SHA-256").digest(d)) b.append(String.format("%02x", x));
			return b.toString();
		} catch (Exception ex) {
			return "";
		}
	}

	private static int zahl(String s) {
		try { return Integer.parseInt(s.trim().replaceAll("\\.0$", "")); } catch (Exception ex) { return 0; }
	}

	private static String text(Object o) { return o == null ? "" : o.toString(); }
}
