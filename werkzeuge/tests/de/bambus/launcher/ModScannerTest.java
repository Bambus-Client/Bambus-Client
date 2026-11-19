package de.bambus.launcher;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Selbsttest für den Mod-Scanner – ohne Internet (Herkunft bleibt dann „unbekannt“).
 * Läuft im Workflow „Änderung prüfen“. Aufruf: java -Duser.home=<leerer Ordner> -cp <klassen> de.bambus.launcher.ModScannerTest
 */
public final class ModScannerTest {

	private static int fehler;

	public static void main(String[] args) throws Exception {
		Path mods = Files.createTempDirectory("bambus-scan-test");
		jar(mods, "sauber.jar", "net/beispiel/Mod.class", "Hallo Welt fabric.mod.json render");
		jar(mods, "webhook.jar", "a/A.class", "xx https://discord.com/api/webhooks/123/abc xx");
		jar(mods, "neko.jar", "dev/neko/nekoclient/Main.class", "nichts");
		jar(mods, "base64.jar", "b/B.class", "q" + Base64.getEncoder().encodeToString(
				"https://discordapp.com/api/webhooks/42/geheim".getBytes(StandardCharsets.US_ASCII)) + "q");
		jar(mods, "konto.jar", "c/C.class", "C:\\\\Users\\\\x\\\\AppData\\\\Roaming\\\\.minecraft\\\\launcher_accounts.json");
		jar(mods, "programm.jar", "d/D.class", "java/lang/ProcessBuilder start");
		byte[] innen = jarBytes("e/E.class", "%APPDATA%\\discord\\Local Storage\\leveldb");
		jarRoh(mods, "verschachtelt.jar", "META-INF/jars/innen.jar", innen);
		Files.write(mods.resolve("kaputt.jar"), "keine jar".getBytes(StandardCharsets.UTF_8));

		ModScanner.Bericht b = ModScanner.pruefe(mods, null);
		erwarte(b, "sauber.jar", ModScanner.Stufe.UNBEKANNT);
		erwarte(b, "webhook.jar", ModScanner.Stufe.GEFAEHRLICH);
		erwarte(b, "neko.jar", ModScanner.Stufe.GEFAEHRLICH);
		erwarte(b, "base64.jar", ModScanner.Stufe.GEFAEHRLICH);
		erwarte(b, "konto.jar", ModScanner.Stufe.VERDAECHTIG);   // ohne Internet: Herkunft unklar → nur verdächtig
		erwarte(b, "programm.jar", ModScanner.Stufe.VERDAECHTIG);
		erwarte(b, "verschachtelt.jar", ModScanner.Stufe.GEFAEHRLICH);
		erwarte(b, "kaputt.jar", ModScanner.Stufe.UNBEKANNT);
		System.out.println(ModScanner.text(b, false));

		// zweiter Durchlauf aus dem Zwischenspeicher – gleiches Ergebnis
		ModScanner.Bericht b2 = ModScanner.pruefe(mods, null);
		erwarte(b2, "webhook.jar", ModScanner.Stufe.GEFAEHRLICH);
		erwarte(b2, "programm.jar", ModScanner.Stufe.VERDAECHTIG);

		// Quarantäne: Dateien verschwinden aus mods, landen mit Notiz im Quarantäne-Ordner
		Path q = ModScanner.inQuarantaene(b2.mit(ModScanner.Stufe.GEFAEHRLICH));
		pruefe(!Files.exists(mods.resolve("webhook.jar")), "webhook.jar noch im mods-Ordner");
		pruefe(Files.exists(q.resolve("webhook.jar.gesperrt")), "webhook.jar nicht in Quarantäne");
		pruefe(Files.exists(q.resolve("LIES-MICH.txt")), "LIES-MICH.txt fehlt");
		pruefe(!ModScanner.pruefe(mods, null).gefahr(), "nach der Quarantäne noch Gefahr");

		if (fehler > 0) {
			System.out.println("FEHLGESCHLAGEN: " + fehler);
			System.exit(1);
		}
		System.out.println("Mod-Scanner-Test: alles OK");
	}

	private static void erwarte(ModScanner.Bericht b, String name, ModScanner.Stufe stufe) {
		for (ModScanner.Ergebnis e : b.ergebnisse) {
			if (e.name().equals(name)) {
				pruefe(e.stufe == stufe, name + ": erwartet " + stufe + ", bekommen " + e.stufe + " " + e.gruende);
				return;
			}
		}
		pruefe(false, name + " fehlt im Bericht");
	}

	private static void pruefe(boolean ok, String text) {
		if (!ok) {
			fehler++;
			System.out.println("✖ " + text);
		}
	}

	private static void jar(Path ordner, String name, String eintrag, String inhalt) throws Exception {
		Files.write(ordner.resolve(name), jarBytes(eintrag, inhalt));
	}

	private static void jarRoh(Path ordner, String name, String eintrag, byte[] inhalt) throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (ZipOutputStream z = new ZipOutputStream(out)) {
			z.putNextEntry(new ZipEntry(eintrag));
			z.write(inhalt);
		}
		Files.write(ordner.resolve(name), out.toByteArray());
	}

	private static byte[] jarBytes(String eintrag, String inhalt) throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (ZipOutputStream z = new ZipOutputStream(out)) {
			z.putNextEntry(new ZipEntry("fabric.mod.json"));
			z.write("{\"id\":\"test\"}".getBytes(StandardCharsets.UTF_8));
			z.putNextEntry(new ZipEntry(eintrag));
			z.write(inhalt.getBytes(StandardCharsets.UTF_8));
		}
		return out.toByteArray();
	}
}
