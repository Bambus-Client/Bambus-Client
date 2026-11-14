package de.bambus.launcher;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Date;

/**
 * Startet beim Öffnen die neueste geladene Launcher-Version (aus ~/.bambus-launcher/updates) –
 * im selben Programm, ohne Dateien im Installationsordner zu ersetzen. Dadurch klappt das Update
 * auch ohne Administrator-Rechte und mit Smart App Control (es startet kein neues Programm).
 *
 * Sicherheitsnetz: Startet eine geladene Version einmal nicht bis zum Fenster durch, wird sie verworfen
 * und der installierte Launcher läuft wie gewohnt.
 */
final class Selbststart {

	/** Systemeigenschaft: Pfad der ursprünglich gestarteten .jar (für Verknüpfungen und Neustart). */
	static final String START = "bambus.launcher.start";
	private static final String AKTIV = "bambus.selbststart.aktiv";

	private Selbststart() {}

	/** true = eine neuere Version läuft jetzt, der Aufrufer soll nichts mehr tun. */
	static boolean uebergeben(String[] args) {
		if (System.getProperty(AKTIV) != null || !Updater.aktiv()) return false;
		Path neu = Updater.bereit();
		if (neu == null) {
			Updater.aufraeumen(Updater.build());   // ältere oder gleich alte Downloads braucht keiner mehr
			return false;
		}
		int nummer = Updater.nummer(neu);
		Path versuch = Updater.ORDNER.resolve("versuch-" + nummer);
		Path ok = Updater.ORDNER.resolve("ok-" + nummer);
		if (Files.exists(versuch) && !Files.exists(ok)) {
			protokoll("Build " + nummer + " ist beim letzten Mal nicht gestartet – wird verworfen.");
			verwerfen(neu, versuch);
			return !Files.exists(neu) && uebergeben(args);   // vielleicht ist noch eine ältere geladene Version gut
		}
		ClassLoader vorher = Thread.currentThread().getContextClassLoader();
		try {
			Path start = Updater.startProgramm();
			if (start != null && System.getProperty("jpackage.app-path") == null) System.setProperty(START, start.toString());
			System.setProperty(AKTIV, neu.toString());
			if (!Files.exists(ok)) Files.write(versuch, new byte[0]);
			// eigener Klassenlader ohne den alten Launcher dahinter: alles kommt aus der neuen Datei
			URLClassLoader lader = new URLClassLoader(new URL[] {neu.toUri().toURL()}, ClassLoader.getSystemClassLoader().getParent());
			Thread.currentThread().setContextClassLoader(lader);
			Class<?> haupt = Class.forName("de.bambus.launcher.BambusLauncher", true, lader);
			haupt.getMethod("main", String[].class).invoke(null, (Object) args);
			protokoll("Build " + nummer + " gestartet (installiert ist Build " + Updater.build() + ").");
			return true;
		} catch (Throwable t) {
			Thread.currentThread().setContextClassLoader(vorher);
			System.clearProperty(AKTIV);
			System.clearProperty(START);
			StringWriter s = new StringWriter();
			t.printStackTrace(new PrintWriter(s));
			protokoll("Build " + nummer + " ließ sich nicht starten:\n" + s);
			verwerfen(neu, versuch);
			return !Files.exists(neu) && uebergeben(args);
		}
	}

	/** Von der neuen Version aufgerufen, sobald ihr Fenster offen ist. */
	static void gestartet() {
		String aktiv = System.getProperty(AKTIV);
		if (aktiv == null) return;
		try {
			int nummer = Updater.nummer(java.nio.file.Paths.get(aktiv));
			Files.write(Updater.ORDNER.resolve("ok-" + nummer), new byte[0]);
			Files.deleteIfExists(Updater.ORDNER.resolve("versuch-" + nummer));
		} catch (Exception ignoriert) {
			// nicht schlimm
		}
	}

	/** Läuft gerade eine geladene Version statt der installierten? */
	static boolean laeuftGeladen() { return System.getProperty(AKTIV) != null; }

	private static void verwerfen(Path jar, Path versuch) {
		try {
			Files.move(jar, jar.resolveSibling(jar.getFileName() + ".kaputt"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
			Files.deleteIfExists(versuch);
		} catch (Exception ignoriert) {
			// dann beim nächsten Mal
		}
	}

	private static void protokoll(String text) {
		try {
			Files.createDirectories(Updater.ORDNER);
			Files.write(Updater.ORDNER.resolve("update.log"), (new Date() + "  " + text + "\n").getBytes(StandardCharsets.UTF_8),
					StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		} catch (Exception ignoriert) {
			// kein Protokoll
		}
	}
}
