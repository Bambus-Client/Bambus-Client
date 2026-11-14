package de.bambus.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

/**
 * Was gerade an Musik läuft – über die Windows-Medienwiedergabe (Spotify, Browser und andere Player),
 * ohne Anmeldung und ohne Spotify Premium. Ein kleines PowerShell-Skript (assets/bambus/musik.ps1) läuft im
 * Hintergrund, solange das Musik-Modul an ist oder eine Musik-Taste gedrückt wurde.
 *
 * Nur unter Windows. Zum Testen auf anderen Systemen: -Dbambus.musik.befehl="python3 test.py".
 */
public final class Musik {

	private Musik() {}

	public static final Identifier COVER = Identifier.fromNamespaceAndPath(BambusClient.MOD_ID, "musik_cover");

	/** Aktueller Stand – wird vom Lese-Thread gesetzt, im Render-Thread gelesen. */
	public static volatile String titel = "", kuenstler = "", album = "", app = "";
	public static volatile double position, dauer;
	public static volatile boolean spielt, laeuftEtwas;
	/** Wann zuletzt Daten kamen (für eine flüssige Fortschrittsanzeige) und wann das Lied wechselte. */
	private static volatile long stand, liedWechsel;
	private static volatile boolean coverDa;
	private static String letztesLied = "";

	private static Process prozess;
	private static Writer eingabe;
	private static long letzterStart;
	private static boolean nurSpotify;
	private static String fehler = "";

	public static boolean moeglich() {
		return System.getProperty("bambus.musik.befehl") != null
				|| System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
	}

	public static boolean coverDa() {
		return coverDa && laeuftEtwas;
	}

	public static String fehler() {
		return fehler;
	}

	/** Position in Sekunden, zwischen zwei Meldungen weitergerechnet. */
	public static double positionJetzt() {
		double p = position;
		if (spielt && stand > 0) p += (System.currentTimeMillis() - stand) / 1000.0;
		return dauer > 0 ? Math.min(p, dauer) : p;
	}

	/** Millisekunden seit dem letzten Liedwechsel (für „nur beim Songwechsel einblenden“). */
	public static long seitWechsel() {
		return liedWechsel == 0 ? Long.MAX_VALUE : System.currentTimeMillis() - liedWechsel;
	}

	// ================================================================ Steuerung

	public static void pause() { befehl("pause"); }
	public static void weiter() { befehl("weiter"); }
	public static void zurueck() { befehl("zurueck"); }

	/** Nur Spotify anzeigen (sonst, was Windows gerade als aktive Wiedergabe meldet). */
	public static void nurSpotify(boolean an) {
		if (an != nurSpotify) {
			nurSpotify = an;
			if (laeuft()) befehl(an ? "modus spotify" : "modus alle");
		}
	}

	private static synchronized void befehl(String text) {
		if (!laeuft()) starte();
		if (eingabe == null) return;
		try {
			eingabe.write(text + "\n");
			eingabe.flush();
		} catch (Exception e) {
			stoppe();
		}
	}

	// ================================================================ Hintergrund-Skript

	private static boolean laeuft() {
		return prozess != null && prozess.isAlive();
	}

	/** Jeden Tick: Skript starten, solange es gebraucht wird – sonst beenden. */
	public static void tick(boolean gebraucht) {
		if (gebraucht && !laeuft() && moeglich() && System.currentTimeMillis() - letzterStart > 15_000) {
			starte();
		} else if (!gebraucht && laeuft() && System.currentTimeMillis() - letzterStart > 30_000) {
			stoppe();   // Modul aus: nach einer Weile beenden (Musik-Tasten starten es bei Bedarf neu)
		}
	}

	private static synchronized void starte() {
		if (laeuft() || !moeglich()) return;
		letzterStart = System.currentTimeMillis();
		try {
			List<String> befehl = new ArrayList<>();
			String test = System.getProperty("bambus.musik.befehl");
			if (test != null) {
				for (String teil : test.split(" ")) befehl.add(teil);
			} else {
				String skript;
				try (InputStream in = Musik.class.getResourceAsStream("/assets/bambus/musik.ps1")) {
					skript = new String(in.readAllBytes(), StandardCharsets.UTF_8);
				}
				String kodiert = Base64.getEncoder().encodeToString(skript.getBytes(StandardCharsets.UTF_16LE));
				befehl.addAll(List.of("powershell.exe", "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass",
						"-WindowStyle", "Hidden", "-EncodedCommand", kodiert));
			}
			ProcessBuilder pb = new ProcessBuilder(befehl);
			pb.redirectErrorStream(true);
			prozess = pb.start();
			eingabe = new OutputStreamWriter(prozess.getOutputStream(), StandardCharsets.UTF_8);
			if (nurSpotify) {
				eingabe.write("modus spotify\n");
				eingabe.flush();
			}
			Process p = prozess;
			Thread leser = new Thread(() -> lies(p), "Bambus-Musik");
			leser.setDaemon(true);
			leser.start();
			fehler = "";
		} catch (Exception e) {
			fehler = "Musik-Anzeige konnte nicht starten";
			BambusClient.LOGGER.warn("Musik-Anzeige nicht gestartet: {}", e.toString());
			prozess = null;
			eingabe = null;
		}
	}

	public static synchronized void stoppe() {
		try {
			if (eingabe != null) eingabe.close();   // das Skript beendet sich, wenn die Eingabe zu ist
		} catch (Exception ignoriert) {
			// schon zu
		}
		if (prozess != null) prozess.destroy();
		prozess = null;
		eingabe = null;
		laeuftEtwas = false;
	}

	private static void lies(Process p) {
		try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
			String zeile;
			while ((zeile = r.readLine()) != null) {
				zeile = zeile.trim();
				if (!zeile.startsWith("{")) continue;   // Meldungen von PowerShell überspringen
				try {
					verarbeite(JsonParser.parseString(zeile).getAsJsonObject());
				} catch (Exception e) {
					// kaputte Zeile – nächste
				}
			}
		} catch (Exception ignoriert) {
			// Prozess beendet
		}
		laeuftEtwas = false;
	}

	private static void verarbeite(JsonObject o) {
		if (o.has("fehler")) {
			fehler = text(o, "fehler");
			return;
		}
		if (o.has("leer")) {
			laeuftEtwas = false;
			return;
		}
		titel = text(o, "titel");
		kuenstler = text(o, "kuenstler");
		album = text(o, "album");
		app = text(o, "app");
		position = o.has("position") ? o.get("position").getAsDouble() : 0;
		dauer = o.has("dauer") ? o.get("dauer").getAsDouble() : 0;
		spielt = o.has("spielt") && o.get("spielt").getAsBoolean();
		stand = System.currentTimeMillis();
		laeuftEtwas = !titel.isEmpty();
		fehler = "";
		String lied = titel + "|" + kuenstler;
		if (!lied.equals(letztesLied)) {
			letztesLied = lied;
			liedWechsel = System.currentTimeMillis();
		}
		if (o.has("cover")) {
			String b64 = text(o, "cover");
			if (b64.isEmpty()) {
				coverDa = false;
			} else {
				byte[] bild = Base64.getDecoder().decode(b64);
				Minecraft.getInstance().execute(() -> ladeCover(bild));
			}
		}
	}

	/** Im Render-Thread: Cover als Textur hochladen (die alte wird dabei ersetzt). */
	private static void ladeCover(byte[] png) {
		try {
			NativeImage bild = NativeImage.read(png);
			Minecraft.getInstance().getTextureManager().register(COVER, new DynamicTexture(() -> "bambus-musik-cover", bild));
			coverDa = true;
		} catch (Exception e) {
			coverDa = false;
		}
	}

	private static String text(JsonObject o, String name) {
		return o.has(name) && !o.get(name).isJsonNull() ? o.get(name).getAsString() : "";
	}

	/** Zeit als m:ss. */
	public static String zeit(double sekunden) {
		int s = (int) Math.max(0, sekunden);
		return (s / 60) + ":" + String.format("%02d", s % 60);
	}
}
