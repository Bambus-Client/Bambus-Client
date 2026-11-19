package de.bambus.launcher;

import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Selbsttest fürs Profil: PIN, Namen, Admin-Regel, Profilbild und Statistik.
 * Aufruf: java -Duser.home=<leerer Ordner> -cp <klassen> de.bambus.launcher.ProfilTest
 */
public final class ProfilTest {

	private static int fehler;

	public static void main(String[] args) throws Exception {
		// PIN
		pruefe(!Profil.pinGesetzt(), "PIN am Anfang schon gesetzt");
		pruefe(!Profil.gueltigePin("12".toCharArray()), "zu kurze PIN erlaubt");
		pruefe(!Profil.gueltigePin("12ab".toCharArray()), "Buchstaben in der PIN erlaubt");
		pruefe(Profil.gueltigePin("4711".toCharArray()), "gültige PIN abgelehnt");
		Profil.setzePin("4711".toCharArray());
		pruefe(Profil.pinGesetzt(), "PIN nicht gespeichert");
		pruefe(Profil.pruefePin("4711".toCharArray()), "richtige PIN abgelehnt");
		pruefe(!Profil.pruefePin("4712".toCharArray()), "falsche PIN angenommen");
		pruefe(!Profil.pruefePin(new char[0]), "leere PIN angenommen");
		String datei = new String(Files.readAllBytes(Profil.ORDNER.resolve("profil.properties")), StandardCharsets.UTF_8);
		pruefe(!datei.contains("4711"), "PIN steht im Klartext in der Datei");
		Profil.entfernePin();
		pruefe(!Profil.pinGesetzt(), "PIN nicht entfernt");

		// Namen
		pruefe(Profil.gueltigerName("Steve_42"), "gültiger Name abgelehnt");
		pruefe(!Profil.gueltigerName("ab"), "zu kurzer Name erlaubt");
		pruefe(!Profil.gueltigerName("böse name"), "ungültiger Name erlaubt");
		Profil.setzeName("Steve_42");
		pruefe("Steve_42".equals(Profil.eingetragenerName()), "Name nicht gespeichert");
		Profil.setzeName("");
		pruefe(Profil.eingetragenerName().isEmpty(), "Name nicht entfernt");

		// Admin nur für bestätigte Konten aus der Team-Liste
		String team = de.bambus.quests.Quests.ENTWICKLER[0];
		pruefe(Profil.mit(Profil.Quelle.MICROSOFT, team).admin(), "Team-Konto mit Microsoft ist kein Admin");
		pruefe(Profil.mit(Profil.Quelle.LAUNCHER, team.toUpperCase()).admin(), "Team-Konto aus dem Launcher ist kein Admin");
		pruefe(!Profil.mit(Profil.Quelle.EINGETRAGEN, team).admin(), "eingetippter Team-Name wird Admin");
		pruefe(!Profil.mit(Profil.Quelle.MICROSOFT, "Steve").admin(), "fremdes Konto wird Admin");
		pruefe("MITGLIED".equals(Profil.mit(Profil.Quelle.MICROSOFT, "Steve").rolle(false)), "Rolle nicht MITGLIED");
		pruefe("".equals(Profil.mit(Profil.Quelle.KEINE, "").rolle(false)), "ohne Namen eine Rolle");

		// Profilbild: Gesicht + Hut-Ebene
		BufferedImage skin = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
		skin.setRGB(9, 9, 0xFF112233);    // Gesicht
		skin.setRGB(42, 10, 0xFFAA0000);  // Hut
		BufferedImage k = Profil.kopf(skin);
		pruefe(k.getWidth() == 8 && k.getRGB(1, 1) == 0xFF112233, "Gesicht falsch ausgeschnitten");
		pruefe(k.getRGB(2, 2) == 0xFFAA0000, "Hut-Ebene fehlt");

		// Statistik über mehrere Versionen
		Path spiel = Files.createTempDirectory("bambus-profil-test");
		Files.createDirectories(spiel.resolve("config"));
		Files.write(spiel.resolve("config/bambus-statistik.json"),
				"{\"kills\":10,\"killsSpieler\":4,\"tode\":5,\"besteCombo\":7,\"spielSekunden\":3600,\"sitzungen\":3}".getBytes(StandardCharsets.UTF_8));
		Files.createDirectories(spiel.resolve("versionen/26.1/config"));
		Files.write(spiel.resolve("versionen/26.1/config/bambus-statistik.json"),
				"{\"kills\":2,\"killsSpieler\":1,\"tode\":1,\"besteCombo\":9,\"spielSekunden\":60,\"sitzungen\":1}".getBytes(StandardCharsets.UTF_8));
		long[] s = Profil.spielStatistik(spiel);
		pruefe(s[0] == 12 && s[1] == 5 && s[2] == 6 && s[3] == 9 && s[5] == 4, "Statistik falsch zusammengezählt: " + java.util.Arrays.toString(s));
		pruefe("2.00".equals(Profil.kd(12, 6)) && "3".equals(Profil.kd(3, 0)), "K/D falsch");

		if (fehler > 0) {
			System.out.println("FEHLGESCHLAGEN: " + fehler);
			System.exit(1);
		}
		System.out.println("Profil-Test: alles OK");
	}

	private static void pruefe(boolean ok, String text) {
		if (!ok) {
			fehler++;
			System.out.println("✖ " + text);
		}
	}
}
