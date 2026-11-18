package de.bambus.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sprache des Clients: Deutsch oder Englisch.
 * Der Client ist auf Deutsch geschrieben; bei Englisch wird jeder Text beim Zeichnen über das Wörterbuch
 * {@code assets/bambus/sprache/en.txt} übersetzt (wie im Launcher). Neue Texte: dort eine Zeile „Deutsch => English“ (Kommentare mit //)
 * ergänzen; Zahlen als „#“, Textteile mit „~“ davor.
 *
 * Einstellung „sprache“: „auto“ (wie Minecraft), „de“ oder „en“.
 */
public final class Sprache {

	private Sprache() {}

	public static final String[] WAHL = {"auto", "de", "en"};

	/** Ganze Texte. Zahlen stehen als „#“ im Schlüssel. */
	private static final Map<String, String> GANZ = new HashMap<>();
	/** Teile von zusammengesetzten Texten (z. B. „ ist dein Freund“), längste zuerst. */
	private static final List<String[]> TEILE = new ArrayList<>();
	/** Alles, was schon Englisch ist – wird nicht noch einmal übersetzt. */
	private static final Set<String> FERTIG = new HashSet<>();
	private static final Map<String, String> CACHE = new LinkedHashMap<>(1024, 0.75f, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<String, String> e) {
			return size() > 4000;
		}
	};
	private static final Pattern ZAHL = Pattern.compile("[-−+]?\\d+(?:[.,]\\d+)*");
	private static final Pattern RAND = Pattern.compile("^((?:§.|[^\\p{L}\\p{N}§])*)(.*?)((?:§.|[^\\p{L}\\p{N}§)])*)$", Pattern.DOTALL);
	private static final Pattern STUECKE = Pattern.compile("\\s+·\\s+");
	private static volatile boolean geladen;

	/** Ist gerade Englisch eingestellt? */
	public static boolean englisch() {
		String s = BambusClient.config == null ? "auto" : BambusClient.config.sprache;
		if ("en".equals(s)) return true;
		if ("de".equals(s)) return false;
		Minecraft mc = Minecraft.getInstance();
		return mc != null && mc.options != null && !mc.options.languageCode.startsWith("de");
	}

	/** Name einer Wahl für das Menü. */
	public static String name(String wahl) {
		return switch (wahl == null ? "auto" : wahl) {
			case "de" -> "Deutsch";
			case "en" -> "English";
			default -> englisch() ? "Automatic (Minecraft)" : "Automatisch (Minecraft)";
		};
	}

	public static String naechste(String wahl, int richtung) {
		int i = 0;
		for (int k = 0; k < WAHL.length; k++) if (WAHL[k].equals(wahl)) i = k;
		return WAHL[Math.floorMod(i + richtung, WAHL.length)];
	}

	/** Nach einem Sprachwechsel: alles neu übersetzen. */
	public static void geaendert() {
		synchronized (CACHE) {
			CACHE.clear();
		}
	}

	/** Übersetzt einen deutschen Text in die eingestellte Sprache. */
	public static String t(String s) {
		if (s == null || s.isEmpty() || !englisch()) return s;
		laden();
		synchronized (CACHE) {
			String c = CACHE.get(s);
			if (c != null) return c;
		}
		String r = uebersetze(s);
		synchronized (CACHE) {
			CACHE.put(s, r);
		}
		return r;
	}

	/** Komponenten werden schon beim Erzeugen übersetzt (Component.literal) – hier nur durchreichen. */
	public static Component t(Component c) {
		return c;
	}

	public static net.minecraft.util.FormattedCharSequence t(net.minecraft.util.FormattedCharSequence c) {
		return c;
	}

	public static net.minecraft.network.chat.FormattedText t(net.minecraft.network.chat.FormattedText c) {
		return c;
	}

	// ------------------------------------------------------------------ Übersetzen

	private static String uebersetze(String s) {
		String r = ganz(s);
		if (r != null) return r;
		// Farbcodes (§x) und Zeichen wie „● “, „✓ “, „ …“ vorn und hinten abtrennen
		Matcher m = RAND.matcher(s);
		if (m.matches() && !m.group(2).isEmpty() && (m.group(1).length() + m.group(3).length() > 0)) {
			String kern = ganz(m.group(2));
			if (kern != null) return m.group(1) + kern + m.group(3);
		}
		if (FERTIG.contains(s)) return s;   // schon Englisch
		// mit „ · “ zusammengesetzt: jedes Stück für sich übersetzen
		if (s.contains(" · ") || s.contains("  ·  ")) {
			Matcher st = STUECKE.matcher(s);
			StringBuilder aus = new StringBuilder();
			int von = 0;
			while (st.find()) {
				aus.append(uebersetze(s.substring(von, st.start()))).append(st.group());
				von = st.end();
			}
			aus.append(uebersetze(s.substring(von)));
			return aus.toString();
		}
		// zusammengesetzt: bekannte Teile ersetzen
		String t = s;
		for (String[] p : TEILE) {
			if (t.contains(p[0])) t = t.replace(p[0], p[1]);
		}
		return t;
	}

	/** Ganzer Text – direkt, in Großbuchstaben oder mit Zahlen als „#“. */
	private static String ganz(String s) {
		String r = GANZ.get(s);
		if (r != null) return r;
		String trim = s.strip();
		if (!trim.equals(s) && !trim.isEmpty()) {
			r = GANZ.get(trim);
			if (r != null) return s.replace(trim, r);
		}
		Matcher z = ZAHL.matcher(s);
		List<String> zahlen = new ArrayList<>();
		StringBuilder schluessel = new StringBuilder();
		while (z.find()) {
			zahlen.add(z.group());
			z.appendReplacement(schluessel, "#");
		}
		if (zahlen.isEmpty()) return null;
		z.appendTail(schluessel);
		r = GANZ.get(schluessel.toString());
		if (r == null) return null;
		StringBuilder aus = new StringBuilder();
		int n = 0;
		for (int i = 0; i < r.length(); i++) {
			char c = r.charAt(i);
			if (c == '#' && n < zahlen.size()) aus.append(zahlen.get(n++));
			else aus.append(c);
		}
		return aus.toString();
	}

	// ------------------------------------------------------------------ Wörterbuch

	private static void laden() {
		if (geladen) return;
		synchronized (GANZ) {
			if (geladen) return;
			try (InputStream in = Sprache.class.getResourceAsStream("/assets/bambus/sprache/en.txt")) {
				if (in != null) {
					BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
					String zeile;
					while ((zeile = r.readLine()) != null) {
						if (zeile.isBlank() || zeile.startsWith("//")) continue;
						int i = zeile.indexOf(" => ");
						if (i < 0) continue;
						String de = zeile.substring(0, i).replace("\\n", "\n");
						String en = zeile.substring(i + 4).replace("\\n", "\n");
						if (de.startsWith("~")) {
							TEILE.add(new String[]{de.substring(1), en});
						} else {
							GANZ.putIfAbsent(de, en);
							GANZ.putIfAbsent(de.toUpperCase(Locale.GERMAN), en.toUpperCase(Locale.ENGLISH));
							FERTIG.add(en);
							FERTIG.add(en.toUpperCase(Locale.ENGLISH));
						}
					}
				}
			} catch (Exception e) {
				BambusClient.LOGGER.warn("Bambus: Wörterbuch nicht lesbar", e);
			}
			TEILE.sort((a, b) -> b[0].length() - a[0].length());
			geladen = true;
		}
	}
}
