package de.bambus.recht;

import java.util.regex.Pattern;

/**
 * Der Name im Englischen: aus „Bambus“ wird „Bamboo“.
 *
 * Wird am Ende der Übersetzung in Launcher und Client angewendet – so stimmt der Name in jedem Text,
 * auch in denen, die noch nicht im Wörterbuch stehen. Nicht angefasst werden Dateinamen, Pfade, Adressen
 * und Kennungen (die sind klein geschrieben, z. B. „bambus.json“, „key.bambus.menu“) und Links wie
 * „github.com/Bambus-Client“.
 */
public final class Marke {

	private Marke() {}

	/**
	 * „Bambus“ als eigenes Wort – nicht direkt nach „/“, „.“ oder Buchstaben, nicht vor Buchstaben/Ziffern.
	 * Direkt nach einem Minecraft-Farbcode wie „§b“ zählt es trotzdem als eigenes Wort.
	 */
	private static final String DAVOR = "(?:(?<=§.)|(?<![/.\\p{L}\\p{N}_]))";
	private static final String DANACH = "(?![\\p{L}\\p{N}_])";
	private static final Pattern NAME = Pattern.compile(DAVOR + "Bambus" + DANACH);
	private static final Pattern GROSS = Pattern.compile(DAVOR + "BAMBUS" + DANACH);

	/** Ersetzt den Namen in einem schon übersetzten englischen Text. */
	public static String englisch(String text) {
		if (text == null || text.indexOf('B') < 0) return text;
		if (!text.contains("Bambus") && !text.contains("BAMBUS")) return text;
		String r = NAME.matcher(text).replaceAll("Bamboo");
		return GROSS.matcher(r).replaceAll("BAMBOO");
	}
}
