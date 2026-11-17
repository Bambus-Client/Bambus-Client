package de.bambus.launcher;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Eine Datei mit allem, was Bambus auf diesem PC ausmacht – zum Mitnehmen auf einen anderen PC.
 * Drin: Einstellungen und HUD, Blätter und Quests, freigeschaltete und selbst gemachte Kosmetik,
 * Mod-Profile, Server, Einstellungs-Profile, Hintergrundbild und die Tastenbelegung.
 * Nicht drin: Welten, Screenshots, Mods (die lädt der Launcher wieder) und das Microsoft-Konto.
 */
final class Sicherung {

	static final String ENDUNG = ".bambus";
	private static final String KENNUNG = "bambus-sicherung/1";
	private static final long HOECHSTENS = 60L * 1024 * 1024;   // mehr ist sicher keine Sicherung

	/** Was in einer Sicherung steckt – zum Anzeigen vor dem Einspielen. */
	static final class Inhalt {
		String datum = "";
		String version = "";
		final List<String> dateien = new ArrayList<>();
		int blaetter = -1;
		int module = -1;
	}

	private Sicherung() {}

	/** Pfade relativ zum Bambus-Ordner, die in die Sicherung gehören. */
	private static List<String> dateien(Path spielOrdner) throws IOException {
		List<String> aus = new ArrayList<>();
		String[] einzeln = {
				"config/bambus.json", "config/bambus-mods.json",
				"config/bambus-profil-1.json", "config/bambus-profil-2.json", "config/bambus-profil-3.json",
				"config/bambus-hintergrund.png", "launcher.properties",
		};
		for (String e : einzeln) if (Files.isRegularFile(spielOrdner.resolve(e))) aus.add(e);
		Path kosmetik = spielOrdner.resolve("config").resolve("bambus-kosmetik");
		if (Files.isDirectory(kosmetik)) {
			try (Stream<Path> s = Files.walk(kosmetik)) {
				for (Path p : s.filter(Files::isRegularFile).collect(Collectors.toList())) {
					aus.add(spielOrdner.relativize(p).toString().replace('\\', '/'));
				}
			}
		}
		return aus;
	}

	/** Nur die Tastenbelegung aus options.txt – der Rest (Auflösung, Soundgerät …) passt nur auf diesen PC. */
	private static String tasten(Path spielOrdner) {
		Path o = spielOrdner.resolve("options.txt");
		if (!Files.isRegularFile(o)) return null;
		try {
			StringBuilder sb = new StringBuilder();
			for (String z : Files.readAllLines(o, StandardCharsets.UTF_8)) {
				if (z.startsWith("key_")) sb.append(z).append('\n');
			}
			return sb.length() == 0 ? null : sb.toString();
		} catch (IOException e) {
			return null;
		}
	}

	/** Schreibt die Sicherung. Gibt die Anzahl der Dateien zurück. */
	static int exportiere(Path spielOrdner, Path ziel) throws IOException {
		List<String> liste = dateien(spielOrdner);
		String tasten = tasten(spielOrdner);
		try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(ziel))) {
			String kopf = KENNUNG + "\n"
					+ "datum=" + new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()) + "\n"
					+ "launcher=" + BambusLauncher.VERSION + "\n";
			schreibe(zip, "inhalt.txt", kopf.getBytes(StandardCharsets.UTF_8));
			for (String d : liste) schreibe(zip, "dateien/" + d, Files.readAllBytes(spielOrdner.resolve(d)));
			if (tasten != null) schreibe(zip, "tasten.txt", tasten.getBytes(StandardCharsets.UTF_8));
		}
		return liste.size() + (tasten != null ? 1 : 0);
	}

	private static void schreibe(ZipOutputStream zip, String name, byte[] daten) throws IOException {
		zip.putNextEntry(new ZipEntry(name));
		zip.write(daten);
		zip.closeEntry();
	}

	/** Liest eine Sicherung ein, ohne etwas zu verändern. null = keine Bambus-Sicherung. */
	static Map<String, byte[]> lies(Path datei) throws IOException {
		if (Files.size(datei) > HOECHSTENS) return null;
		Map<String, byte[]> aus = new LinkedHashMap<>();
		long summe = 0;
		try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(datei))) {
			ZipEntry e;
			while ((e = zip.getNextEntry()) != null) {
				if (e.isDirectory()) continue;
				byte[] d = alles(zip);
				summe += d.length;
				if (summe > HOECHSTENS) return null;
				aus.put(e.getName(), d);
			}
		} catch (IOException e) {
			return null;
		}
		byte[] kopf = aus.get("inhalt.txt");
		if (kopf == null || !new String(kopf, StandardCharsets.UTF_8).startsWith(KENNUNG)) return null;
		return aus;
	}

	private static byte[] alles(InputStream in) throws IOException {
		ByteArrayOutputStream b = new ByteArrayOutputStream();
		byte[] puffer = new byte[16384];
		int n;
		while ((n = in.read(puffer)) > 0) b.write(puffer, 0, n);
		return b.toByteArray();
	}

	/** Kurzer Überblick für die Nachfrage vor dem Einspielen. */
	static Inhalt ueberblick(Map<String, byte[]> daten) {
		Inhalt i = new Inhalt();
		for (String z : new String(daten.get("inhalt.txt"), StandardCharsets.UTF_8).split("\n")) {
			if (z.startsWith("datum=")) i.datum = z.substring(6).trim();
			if (z.startsWith("launcher=")) i.version = z.substring(9).trim();
		}
		for (String k : daten.keySet()) if (k.startsWith("dateien/")) i.dateien.add(k.substring(8));
		byte[] json = daten.get("dateien/config/bambus.json");
		if (json != null) {
			try {
				Map<String, Object> o = Json.objekt(Json.parse(new String(json, StandardCharsets.UTF_8)));
				if (o != null && o.get("punkte") instanceof Number) i.blaetter = ((Number) o.get("punkte")).intValue();
				if (o != null) {
					Map<String, Object> m = Json.objekt(o.get("module"));
					if (m != null) i.module = m.size();
				}
			} catch (Exception ignoriert) {
				// Überblick ist nur zur Info
			}
		}
		return i;
	}

	/**
	 * Spielt eine Sicherung ein. Vorher wird der jetzige Stand als eigene Sicherung im
	 * config-Ordner abgelegt – so kann man es rückgängig machen.
	 *
	 * @return Pfad der automatisch angelegten Vorher-Sicherung
	 */
	static Path spieleEin(Path spielOrdner, Map<String, byte[]> daten) throws IOException {
		Path vorher = spielOrdner.resolve("config").resolve("bambus-vor-import-"
				+ new SimpleDateFormat("yyyy-MM-dd_HH-mm").format(new Date()) + ENDUNG);
		Files.createDirectories(vorher.getParent());
		exportiere(spielOrdner, vorher);

		for (Map.Entry<String, byte[]> e : daten.entrySet()) {
			if (!e.getKey().startsWith("dateien/")) continue;
			String rel = e.getKey().substring(8);
			Path ziel = spielOrdner.resolve(rel).normalize();
			if (!ziel.startsWith(spielOrdner.normalize()) || !erlaubt(rel)) continue;   // nichts außerhalb, nichts Fremdes
			Files.createDirectories(ziel.getParent());
			Path tmp = ziel.resolveSibling(ziel.getFileName() + ".neu");
			Files.write(tmp, e.getValue());
			Files.move(tmp, ziel, StandardCopyOption.REPLACE_EXISTING);
		}
		byte[] tasten = daten.get("tasten.txt");
		if (tasten != null) tastenEinspielen(spielOrdner, new String(tasten, StandardCharsets.UTF_8));
		return vorher;
	}

	/** Nur Dateien, die eine Sicherung auch schreiben würde. */
	private static boolean erlaubt(String rel) {
		return rel.equals("config/bambus.json") || rel.equals("config/bambus-mods.json")
				|| rel.matches("config/bambus-profil-[123]\\.json")
				|| rel.equals("config/bambus-hintergrund.png") || rel.equals("launcher.properties")
				|| (rel.startsWith("config/bambus-kosmetik/") && !rel.contains(".."));
	}

	/** Tastenzeilen in options.txt übernehmen, alles andere dort bleibt, wie es ist. */
	private static void tastenEinspielen(Path spielOrdner, String tasten) throws IOException {
		Path o = spielOrdner.resolve("options.txt");
		Map<String, String> zeilen = new LinkedHashMap<>();
		if (Files.isRegularFile(o)) {
			for (String z : Files.readAllLines(o, StandardCharsets.UTF_8)) {
				int d = z.indexOf(':');
				zeilen.put(d > 0 ? z.substring(0, d) : z, z);
			}
		}
		for (String z : tasten.split("\n")) {
			int d = z.indexOf(':');
			if (d > 0 && z.startsWith("key_") && z.length() < 200) zeilen.put(z.substring(0, d), z.trim());
		}
		try (OutputStream out = Files.newOutputStream(o)) {
			for (String z : zeilen.values()) out.write((z + "\n").getBytes(StandardCharsets.UTF_8));
		}
	}
}
