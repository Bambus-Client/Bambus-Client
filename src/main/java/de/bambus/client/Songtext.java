package de.bambus.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Zeitgenauer Songtext von LRCLIB (lrclib.net – frei, ohne Konto). LRC liefert Zeitstempel je Zeile;
 * die Wörter einer Zeile werden gleichmäßig über die gesungene Zeit verteilt (Karaoke-Effekt).
 * Zum Testen ohne Internet: -Dbambus.musik.lrc=/pfad/zur/datei.lrc
 */
public final class Songtext {

	private Songtext() {}

	/** Ein Wort mit Startzeit (Sekunden). */
	public record Wort(String text, double start) {}

	/** Eine Zeile: Startzeit, Ende und ihre Wörter. */
	public record Zeile(double start, double ende, String text, List<Wort> woerter) {}

	private static final Pattern ZEIT = Pattern.compile("\\[(\\d+):(\\d+(?:\\.\\d+)?)]");

	private static volatile List<Zeile> zeilen = Collections.emptyList();
	private static volatile String fuer = "";

	public static List<Zeile> zeilen() {
		return zeilen;
	}

	public static boolean vorhanden() {
		return !zeilen.isEmpty();
	}

	/** Neues Lied: Songtext im Hintergrund laden (einmal je Lied; übernommen wird nur, was noch zum Lied passt). */
	public static void fuerLied(String titel, String kuenstler, String album, double dauer) {
		String schluessel = titel + "|" + kuenstler;
		if (schluessel.equals(fuer) || titel.isEmpty()) return;
		fuer = schluessel;
		zeilen = Collections.emptyList();
		Thread t = new Thread(() -> {
			try {
				String lrc = lade(titel, kuenstler, album, dauer);
				if (schluessel.equals(fuer) && lrc != null) zeilen = lies(lrc);
			} catch (Exception e) {
				BambusClient.LOGGER.debug("Kein Songtext: {}", e.toString());
			}
		}, "Bambus-Songtext");
		t.setDaemon(true);
		t.start();
	}

	private static String lade(String titel, String kuenstler, String album, double dauer) throws Exception {
		String test = System.getProperty("bambus.musik.lrc");
		if (test != null) return Files.readString(Paths.get(test), StandardCharsets.UTF_8);
		// Spotify hängt bei Künstlern oft weitere mit Komma an – LRCLIB kennt meist nur den ersten
		String erster = kuenstler.split(",")[0].trim();
		String adresse = "https://lrclib.net/api/get?track_name=" + enc(titel) + "&artist_name=" + enc(erster)
				+ (album.isEmpty() ? "" : "&album_name=" + enc(album)) + (dauer > 0 ? "&duration=" + Math.round(dauer) : "");
		String json = holen(adresse);
		if (json == null) {   // ohne Album und Dauer noch einmal über die Suche
			json = holen("https://lrclib.net/api/search?track_name=" + enc(titel) + "&artist_name=" + enc(erster));
		}
		if (json == null) return null;
		JsonElement e = JsonParser.parseString(json);
		JsonObject treffer = e.isJsonArray() ? (e.getAsJsonArray().isEmpty() ? null : e.getAsJsonArray().get(0).getAsJsonObject())
				: e.getAsJsonObject();
		if (treffer == null || !treffer.has("syncedLyrics") || treffer.get("syncedLyrics").isJsonNull()) return null;
		return treffer.get("syncedLyrics").getAsString();
	}

	private static String holen(String adresse) throws Exception {
		HttpURLConnection c = (HttpURLConnection) URI.create(adresse).toURL().openConnection();
		c.setRequestProperty("User-Agent", "Bambus Client " + BambusClient.VERSION);
		c.setConnectTimeout(8000);
		c.setReadTimeout(8000);
		if (c.getResponseCode() != 200) return null;
		try (InputStream in = c.getInputStream()) {
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	private static String enc(String s) {
		return URLEncoder.encode(s, StandardCharsets.UTF_8);
	}

	/** LRC lesen: „[01:23.45] Text“ – mehrere Zeitstempel je Zeile sind erlaubt. */
	static List<Zeile> lies(String lrc) {
		List<double[]> zeiten = new ArrayList<>();
		List<String> texte = new ArrayList<>();
		for (String roh : lrc.split("\\r?\\n")) {
			Matcher m = ZEIT.matcher(roh);
			List<Double> starts = new ArrayList<>();
			int ende = 0;
			while (m.find()) {
				starts.add(Integer.parseInt(m.group(1)) * 60 + Double.parseDouble(m.group(2)));
				ende = m.end();
			}
			String text = roh.substring(ende).trim();
			for (double s : starts) {
				zeiten.add(new double[]{s, texte.size()});
				texte.add(text);
			}
		}
		zeiten.sort((a, b) -> Double.compare(a[0], b[0]));
		List<Zeile> ergebnis = new ArrayList<>();
		for (int i = 0; i < zeiten.size(); i++) {
			String text = texte.get((int) zeiten.get(i)[1]);
			if (text.isEmpty()) continue;   // Pause im Lied
			double start = zeiten.get(i)[0];
			double naechste = i + 1 < zeiten.size() ? zeiten.get(i + 1)[0] : start + 5;
			String[] teile = text.split("\\s+");
			// gesungen wird nicht bis zur nächsten Zeile – höchstens ~0,5 s je Wort
			double dauer = Math.min((naechste - start) * 0.9, teile.length * 0.5 + 0.4);
			List<Wort> woerter = new ArrayList<>();
			for (int k = 0; k < teile.length; k++) {
				woerter.add(new Wort(teile[k], start + dauer * k / teile.length));
			}
			ergebnis.add(new Zeile(start, naechste, text, woerter));
		}
		return ergebnis;
	}

	/** Die Zeile, die gerade gesungen wird (oder null in Pausen). */
	public static Zeile aktuelleZeile(double zeit) {
		Zeile treffer = null;
		for (Zeile z : zeilen) {
			if (z.start() <= zeit + 0.05) treffer = z;
			else break;
		}
		if (treffer != null && zeit > treffer.ende()) return null;
		return treffer;
	}
}
