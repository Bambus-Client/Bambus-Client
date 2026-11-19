package de.bambus.launcher;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Mod-Scanner – das „Antivirus“ für den mods-Ordner.
 *
 * Prüft vor jedem Spielstart (und auf Knopfdruck unter EINSTELLUNGEN → SICHERHEIT) jede .jar-Datei:
 *  1. Inhalt: Sucht in allen Klassen und Dateien – auch in eingebetteten Jars – nach Spuren bekannter
 *     Minecraft-Schadsoftware: fractureiser, Versand von Daten über Discord-Webhooks oder Telegram-Bots,
 *     Zugriff auf Discord-Token, Browser-Passwörter, Krypto-Wallets und Anmeldedaten anderer Launcher.
 *     Auch Base64-versteckte Webhook-Adressen werden erkannt.
 *  2. Herkunft: Fragt Modrinth anhand der Prüfsumme, ob es genau diese Datei dort gibt.
 *
 * Ergebnis je Datei: SICHER (von Modrinth oder die Bambus-Mod selbst), UNBEKANNT (nicht bei Modrinth,
 * nichts Verdächtiges), VERDAECHTIG (unbekannte Herkunft und Code, der z. B. Programme startet oder auf den
 * Minecraft-Zugangsschlüssel zugreift) oder GEFAEHRLICH (Spuren von Schadsoftware). Gefährliche Dateien können in eine
 * Quarantäne verschoben werden (~/.bambus-launcher/quarantaene) – nichts wird gelöscht.
 *
 * Kein Ersatz für einen Virenscanner für den ganzen Computer (Windows Defender, macOS XProtect).
 * Ergebnisse werden je Prüfsumme zwischengespeichert, damit der Start schnell bleibt.
 */
final class ModScanner {

	/** Höher setzen, wenn sich die Erkennungsmerkmale ändern – dann wird alles neu geprüft. */
	static final int MERKMALE_VERSION = 1;

	enum Stufe { SICHER, UNBEKANNT, VERDAECHTIG, GEFAEHRLICH }

	static final class Ergebnis {
		final Path datei;
		Stufe stufe = Stufe.UNBEKANNT;
		boolean modrinth;
		boolean eigeneMod;
		final Set<String> gruende = new LinkedHashSet<>();

		Ergebnis(Path datei) { this.datei = datei; }

		String name() { return datei.getFileName().toString(); }
	}

	static final class Bericht {
		final List<Ergebnis> ergebnisse = new ArrayList<>();
		boolean herkunftGeprueft;

		List<Ergebnis> mit(Stufe s) {
			List<Ergebnis> l = new ArrayList<>();
			for (Ergebnis e : ergebnisse) if (e.stufe == s) l.add(e);
			return l;
		}

		boolean gefahr() { return !mit(Stufe.GEFAEHRLICH).isEmpty(); }
	}

	interface Fortschritt { void melde(String text); }

	static final Path ORDNER = Paths.get(System.getProperty("user.home"), ".bambus-launcher");
	static final Path QUARANTAENE = ORDNER.resolve("quarantaene");
	private static final Path ZWISCHENSPEICHER = ORDNER.resolve("mod-scan.properties");

	private static final long MAX_DATEI = 150L * 1024 * 1024;      // größere Mods werden nicht geöffnet
	private static final long MAX_ENTPACKT = 400L * 1024 * 1024;   // Schutz vor „Zip-Bomben“
	private static final int MAX_TIEFE = 3;                         // Jars in Jars in Jars
	private static final long EIN_TAG = 24L * 60 * 60 * 1000;

	private ModScanner() {}

	// ================================================================== Erkennungsmerkmale

	/** Merkmal → Grund. Alles klein geschrieben, Pfade mit „/“ (Backslashes werden vorher umgewandelt). */
	private static final Map<String, String> GEFAHR = new LinkedHashMap<>();
	/** Gefährlich nur, wenn die Datei nicht von Modrinth stammt (könnte sonst ein echter Konto-Wechsler sein). */
	private static final Map<String, String> GEFAHR_UNBEKANNT = new LinkedHashMap<>();
	/** Verdächtig, wenn die Herkunft unbekannt ist. */
	private static final Map<String, String> VERDACHT = new LinkedHashMap<>();

	static {
		// fractureiser (2023) – bekannte Klassennamen und Server
		GEFAHR.put("dev/neko/nekoinjector", "fractureiser-Schadsoftware");
		GEFAHR.put("dev/neko/nekoclient", "fractureiser-Schadsoftware");
		GEFAHR.put("dev.neko.nekoclient", "fractureiser-Schadsoftware");
		GEFAHR.put("files.skyrage.de", "fractureiser-Schadsoftware");
		GEFAHR.put("files-8ie.pages.dev", "fractureiser-Schadsoftware");
		GEFAHR.put("85.217.144.130", "fractureiser-Schadsoftware");
		GEFAHR.put("107.189.3.101", "fractureiser-Schadsoftware");
		// Daten heimlich verschicken
		GEFAHR.put("discord.com/api/webhooks", "schickt Daten an einen Discord-Webhook");
		GEFAHR.put("discordapp.com/api/webhooks", "schickt Daten an einen Discord-Webhook");
		GEFAHR.put("canary.discord.com/api/webhooks", "schickt Daten an einen Discord-Webhook");
		GEFAHR.put("api.telegram.org/bot", "schickt Daten an einen Telegram-Bot");
		// Zugangsdaten stehlen
		GEFAHR.put("local storage/leveldb", "liest Discord- oder Browser-Anmeldedaten");
		GEFAHR.put("discord/local state", "liest Discord-Anmeldedaten");
		GEFAHR.put("user data/default/login data", "liest gespeicherte Browser-Passwörter");
		GEFAHR.put("user data/local state", "liest Browser-Schlüssel");
		GEFAHR.put("mozilla/firefox/profiles", "liest Browser-Daten");
		GEFAHR.put("wallet.dat", "sucht Krypto-Wallets");
		GEFAHR.put("exodus.wallet", "sucht Krypto-Wallets");
		GEFAHR.put("metamask", "sucht Krypto-Wallets");
		GEFAHR_UNBEKANNT.put("firefox/profiles/logins.json", "liest gespeicherte Browser-Passwörter");
		GEFAHR_UNBEKANNT.put("launcher_accounts", "liest Minecraft-Anmeldedaten des offiziellen Launchers");
		GEFAHR_UNBEKANNT.put("launcher_profiles_microsoft_store", "liest Minecraft-Anmeldedaten des offiziellen Launchers");
		GEFAHR_UNBEKANNT.put("microsoft_accounts.json", "liest Anmeldedaten anderer Clients");
		GEFAHR_UNBEKANNT.put(".lunarclient/settings/game/accounts", "liest Anmeldedaten von Lunar Client");
		GEFAHR_UNBEKANNT.put(".feather/accounts", "liest Anmeldedaten von Feather");
		GEFAHR_UNBEKANNT.put("tlauncher/accounts", "liest Anmeldedaten anderer Launcher");
		GEFAHR_UNBEKANNT.put("polymc/accounts.json", "liest Anmeldedaten anderer Launcher");
		GEFAHR_UNBEKANNT.put("prismlauncher/accounts.json", "liest Anmeldedaten anderer Launcher");
		// verdächtig bei unbekannter Herkunft
		VERDACHT.put("java/lang/processbuilder", "kann andere Programme starten");
		VERDACHT.put("pastebin.com/raw", "lädt Inhalte von Pastebin");
		VERDACHT.put("cdn.discordapp.com/attachments", "lädt Dateien von Discord herunter");
		VERDACHT.put("getaccesstoken", "greift auf den Minecraft-Zugangsschlüssel zu");
	}

	/** Webhook- und Telegram-Adressen auch als Base64 erkennen – in allen drei möglichen Verschiebungen. */
	private static final Map<String, String> BASE64 = new LinkedHashMap<>();

	static {
		for (String s : new String[] {"discord.com/api/webhooks", "discordapp.com/api/webhooks", "api.telegram.org/bot"}) {
			for (String vorsatz : new String[] {"", "x", "xx"}) {
				String b = Base64.getEncoder().encodeToString((vorsatz + s).getBytes(StandardCharsets.US_ASCII));
				// nur der sichere Mittelteil: Anfang (vom Vorsatz) und Ende (vom Padding) abschneiden
				int von = vorsatz.isEmpty() ? 0 : 4, bis = b.length() - 4;
				BASE64.put(b.substring(von, bis), "versteckte Adresse zum Datenversand (Base64)");
			}
		}
	}

	// ================================================================== Prüfen

	/** Prüft alle .jar-Dateien im Ordner. Die Herkunftsprüfung braucht Internet – ohne geht der Rest trotzdem. */
	static Bericht pruefe(Path modsOrdner, Fortschritt f) {
		Bericht b = new Bericht();
		List<Path> dateien = new ArrayList<>();
		if (Files.isDirectory(modsOrdner)) {
			try (DirectoryStream<Path> d = Files.newDirectoryStream(modsOrdner, "*.jar")) {
				for (Path p : d) if (Files.isRegularFile(p)) dateien.add(p);
			} catch (IOException ignoriert) {
				// leer
			}
		}
		if (dateien.isEmpty()) {
			b.herkunftGeprueft = true;
			return b;
		}
		Properties speicher = ladeSpeicher();
		Map<String, Ergebnis> offen = new LinkedHashMap<>();   // sha512 → Ergebnis (Herkunft noch fragen)
		Map<Ergebnis, String> schluessel = new LinkedHashMap<>();
		Set<String> eigene = eigeneModPruefsummen();
		long jetzt = System.currentTimeMillis();
		int nr = 0;
		for (Path p : dateien) {
			nr++;
			if (f != null) f.melde("Sicherheitsprüfung " + nr + "/" + dateien.size() + ": " + p.getFileName());
			Ergebnis e = new Ergebnis(p);
			b.ergebnisse.add(e);
			byte[] daten;
			try {
				if (Files.size(p) > MAX_DATEI) {
					e.gruende.add("sehr groß – Inhalt nicht geprüft");
					continue;
				}
				daten = Files.readAllBytes(p);
			} catch (IOException ex) {
				e.gruende.add("konnte nicht gelesen werden");
				continue;
			}
			String sha256 = Sicherheit.hex("SHA-256", daten);
			if (eigene.contains(sha256)) {
				e.eigeneMod = true;
				e.stufe = Stufe.SICHER;
				continue;
			}
			String key = "v" + MERKMALE_VERSION + "." + sha256;
			schluessel.put(e, key);
			String alt = speicher.getProperty(key);
			if (alt != null && ausSpeicher(e, alt, jetzt)) continue;

			Set<String> treffer = new LinkedHashSet<>();
			try {
				durchsuche(daten, 0, new long[] {0}, treffer);
			} catch (IOException ex) {
				e.gruende.add("Datei ist beschädigt oder keine echte Mod");
			}
			e.gruende.addAll(treffer);
			offen.put(Sicherheit.hex("SHA-512", daten), e);
		}

		// Herkunft bei Modrinth fragen (eine Anfrage für alle)
		if (!offen.isEmpty()) {
			try {
				Set<String> bekannt = modrinthKennt(offen.keySet());
				for (Map.Entry<String, Ergebnis> m : offen.entrySet()) m.getValue().modrinth = bekannt.contains(m.getKey());
				b.herkunftGeprueft = true;
			} catch (IOException ex) {
				b.herkunftGeprueft = false;
			}
		} else {
			b.herkunftGeprueft = true;
		}

		for (Ergebnis e : offen.values()) {
			bewerte(e, b.herkunftGeprueft);
			if (b.herkunftGeprueft || e.stufe == Stufe.GEFAEHRLICH) {
				speicher.setProperty(schluessel.get(e), inSpeicher(e, jetzt));
			}
		}
		speichere(speicher, schluessel.values());
		return b;
	}

	/** Treffer in Stufen umrechnen. */
	private static void bewerte(Ergebnis e, boolean herkunftBekannt) {
		Set<String> gruende = new LinkedHashSet<>();
		boolean gefahr = false, verdacht = false;
		for (String t : e.gruende) {
			if (t.startsWith("G:")) {
				gefahr = true;
				gruende.add(t.substring(2));
			} else if (t.startsWith("U:")) {
				if (e.modrinth) continue;   // echte Mods von Modrinth (z. B. Konto-Wechsler) dürfen das
				if (herkunftBekannt) gefahr = true; else verdacht = true;   // ohne Internet nicht gleich Alarm
				gruende.add(t.substring(2));
			} else if (t.startsWith("V:")) {
				if (e.modrinth) continue;
				verdacht = true;
				gruende.add(t.substring(2));
			} else {
				gruende.add(t);
			}
		}
		e.gruende.clear();
		e.gruende.addAll(gruende);
		e.stufe = gefahr ? Stufe.GEFAEHRLICH : verdacht ? Stufe.VERDAECHTIG : e.modrinth ? Stufe.SICHER : Stufe.UNBEKANNT;
	}

	/** Durchsucht eine Jar (auch eingebettete Jars) nach Merkmalen. Treffer mit Vorsilbe G:/U:/V:. */
	static void durchsuche(byte[] jar, int tiefe, long[] entpackt, Set<String> treffer) throws IOException {
		boolean irgendwas = false;
		try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(jar))) {
			ZipEntry eintrag;
			byte[] puffer = new byte[16384];
			while ((eintrag = zip.getNextEntry()) != null) {
				irgendwas = true;
				if (eintrag.isDirectory()) continue;
				String name = eintrag.getName().toLowerCase(Locale.ROOT);
				pruefeText(name, treffer);   // auch Dateinamen/Pfade, z. B. dev/neko/nekoclient/...
				if (name.endsWith(".png") || name.endsWith(".ogg") || name.endsWith(".nbt")) continue;
				java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
				int n;
				while ((n = zip.read(puffer)) > 0) {
					entpackt[0] += n;
					if (entpackt[0] > MAX_ENTPACKT) throw new IOException("zu groß");
					out.write(puffer, 0, n);
				}
				byte[] inhalt = out.toByteArray();
				if (name.endsWith(".jar") && tiefe < MAX_TIEFE) {
					try {
						durchsuche(inhalt, tiefe + 1, entpackt, treffer);
					} catch (IOException ignoriert) {
						// eingebettete Datei kaputt – der Rest zählt trotzdem
					}
				} else {
					pruefeText(new String(inhalt, StandardCharsets.ISO_8859_1), treffer);
				}
			}
		}
		if (!irgendwas) throw new IOException("keine Jar-Datei");
	}

	private static void pruefeText(String roh, Set<String> treffer) {
		String s = roh.toLowerCase(Locale.ROOT).replace('\\', '/');
		for (Map.Entry<String, String> m : GEFAHR.entrySet()) if (s.contains(m.getKey())) treffer.add("G:" + m.getValue());
		for (Map.Entry<String, String> m : GEFAHR_UNBEKANNT.entrySet()) if (s.contains(m.getKey())) treffer.add("U:" + m.getValue());
		for (Map.Entry<String, String> m : VERDACHT.entrySet()) if (s.contains(m.getKey())) treffer.add("V:" + m.getValue());
		// Base64 ist groß-/kleinschreibungsempfindlich – im Original suchen
		for (Map.Entry<String, String> m : BASE64.entrySet()) if (roh.contains(m.getKey())) treffer.add("G:" + m.getValue());
	}

	// ================================================================== Herkunft (Modrinth)

	/** Welche dieser SHA-512-Prüfsummen kennt Modrinth? */
	static Set<String> modrinthKennt(Set<String> sha512) throws IOException {
		StringBuilder body = new StringBuilder("{\"algorithm\":\"sha512\",\"hashes\":[");
		boolean erst = true;
		for (String h : sha512) {
			if (!erst) body.append(',');
			body.append('"').append(h).append('"');
			erst = false;
		}
		body.append("]}");
		String adresse = "https://api.modrinth.com/v2/version_files";
		Sicherheit.pruefeAdresse(adresse);
		HttpURLConnection c = (HttpURLConnection) new URL(adresse).openConnection();
		c.setRequestMethod("POST");
		c.setRequestProperty("User-Agent", Installer.USER_AGENT);
		c.setRequestProperty("Content-Type", "application/json");
		c.setConnectTimeout(10000);
		c.setReadTimeout(20000);
		c.setDoOutput(true);
		try (OutputStream o = c.getOutputStream()) {
			o.write(body.toString().getBytes(StandardCharsets.UTF_8));
		}
		if (c.getResponseCode() != 200) throw new IOException("Modrinth antwortet nicht (" + c.getResponseCode() + ")");
		byte[] antwort;
		try (InputStream in = c.getInputStream()) {
			antwort = Installer.liesAlles(in);
		}
		Map<String, Object> o = Json.objekt(Json.parse(new String(antwort, StandardCharsets.UTF_8)));
		Set<String> bekannt = new LinkedHashSet<>();
		if (o != null) for (String k : o.keySet()) bekannt.add(k.toLowerCase(Locale.ROOT));
		return bekannt;
	}

	/** Prüfsummen der Bambus-Mods, die im Launcher stecken – die sind unsere eigenen. */
	private static Set<String> eigeneModPruefsummen() {
		Set<String> s = new LinkedHashSet<>();
		List<String> pfade = new ArrayList<>();
		pfade.add("/" + Installer.MOD_DATEI);
		for (String v : Installer.VERSIONEN) pfade.add(Installer.modResource(v));
		for (String r : pfade) {
			try (InputStream in = ModScanner.class.getResourceAsStream(r)) {
				if (in != null) s.add(Sicherheit.hex("SHA-256", Installer.liesAlles(in)));
			} catch (IOException ignoriert) {
				// fehlt
			}
		}
		return s;
	}

	// ================================================================== Quarantäne

	/** Verschiebt die Dateien in einen neuen Quarantäne-Ordner (mit Notiz, woher sie kamen). Gibt den Ordner zurück. */
	static Path inQuarantaene(List<Ergebnis> liste) throws IOException {
		Path ziel = QUARANTAENE.resolve(new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date()));
		Files.createDirectories(ziel);
		StringBuilder notiz = new StringBuilder("Von Bambus in Quarantäne verschoben. Zum Zurückholen die Datei wieder in den\n"
				+ "angegebenen Ordner legen – nur, wenn du sicher bist, dass sie ungefährlich ist.\n\n");
		for (Ergebnis e : liste) {
			Path neu = ziel.resolve(e.name() + ".gesperrt");
			Files.move(e.datei, neu, StandardCopyOption.REPLACE_EXISTING);
			notiz.append(e.name()).append("\n  kam aus: ").append(e.datei.getParent()).append("\n  Grund: ")
					.append(String.join(", ", e.gruende)).append("\n\n");
		}
		Files.write(ziel.resolve("LIES-MICH.txt"), notiz.toString().getBytes(StandardCharsets.UTF_8));
		return ziel;
	}

	// ================================================================== Zwischenspeicher

	private static boolean ausSpeicher(Ergebnis e, String wert, long jetzt) {
		String[] t = wert.split("\\|", 4);
		if (t.length < 4) return false;
		try {
			Stufe s = Stufe.valueOf(t[0]);
			long wann = Long.parseLong(t[2]);
			// Unbekannte Herkunft einmal am Tag neu bei Modrinth fragen – vielleicht wurde die Mod inzwischen hochgeladen
			if ((s == Stufe.UNBEKANNT || s == Stufe.VERDAECHTIG) && jetzt - wann > EIN_TAG) return false;
			e.stufe = s;
			e.modrinth = "1".equals(t[1]);
			for (String g : t[3].split(";")) if (!g.isEmpty()) e.gruende.add(g);
			return true;
		} catch (IllegalArgumentException ex) {
			return false;
		}
	}

	private static String inSpeicher(Ergebnis e, long jetzt) {
		StringBuilder g = new StringBuilder();
		for (String s : e.gruende) g.append(s.replace(';', ',').replace('|', '/')).append(';');
		return e.stufe + "|" + (e.modrinth ? "1" : "0") + "|" + jetzt + "|" + g;
	}

	private static Properties ladeSpeicher() {
		Properties p = new Properties();
		try (InputStream in = Files.newInputStream(ZWISCHENSPEICHER)) {
			p.load(new java.io.InputStreamReader(in, StandardCharsets.UTF_8));
		} catch (IOException ignoriert) {
			// noch keiner
		}
		return p;
	}

	/** Speichert – alte Einträge (andere Merkmal-Version) fliegen raus, damit die Datei klein bleibt. */
	private static void speichere(Properties p, java.util.Collection<String> aktuell) {
		Properties neu = new Properties();
		String vorsilbe = "v" + MERKMALE_VERSION + ".";
		for (String k : p.stringPropertyNames()) if (k.startsWith(vorsilbe)) neu.setProperty(k, p.getProperty(k));
		if (neu.size() > 2000) {   // nur die Dateien behalten, die gerade da sind
			Properties klein = new Properties();
			for (String k : aktuell) if (neu.getProperty(k) != null) klein.setProperty(k, neu.getProperty(k));
			neu = klein;
		}
		try {
			Files.createDirectories(ORDNER);
			try (java.io.Writer w = new java.io.OutputStreamWriter(Files.newOutputStream(ZWISCHENSPEICHER), StandardCharsets.UTF_8)) {
				neu.store(w, "Bambus Mod-Scanner – Ergebnisse je Prüfsumme");
			}
		} catch (IOException ignoriert) {
			// dann eben beim nächsten Mal neu prüfen
		}
	}

	// ================================================================== Texte

	static String stufeText(Stufe s, boolean en) {
		switch (s) {
			case SICHER: return en ? "safe" : "sicher";
			case UNBEKANNT: return en ? "unknown source" : "unbekannte Herkunft";
			case VERDAECHTIG: return en ? "suspicious" : "verdächtig";
			default: return en ? "DANGEROUS" : "GEFÄHRLICH";
		}
	}

	/** Bericht als lesbarer Text für Dialoge. */
	static String text(Bericht b, boolean en) {
		StringBuilder s = new StringBuilder();
		int gef = b.mit(Stufe.GEFAEHRLICH).size(), ver = b.mit(Stufe.VERDAECHTIG).size(),
				unb = b.mit(Stufe.UNBEKANNT).size(), ok = b.mit(Stufe.SICHER).size();
		if (b.ergebnisse.isEmpty()) {
			return en ? "No mods in this profile – nothing to check." : "In diesem Profil sind keine Mods – nichts zu prüfen.";
		}
		s.append(en ? "Checked mods: " : "Geprüfte Mods: ").append(b.ergebnisse.size()).append("\n");
		s.append("✔ ").append(ok).append(en ? " safe   " : " sicher   ");
		s.append("? ").append(unb).append(en ? " unknown source   " : " unbekannte Herkunft   ");
		s.append("! ").append(ver).append(en ? " suspicious   " : " verdächtig   ");
		s.append("✖ ").append(gef).append(en ? " dangerous\n" : " gefährlich\n");
		if (!b.herkunftGeprueft) {
			s.append(en ? "(No internet: the source could not be checked with Modrinth.)\n"
					: "(Kein Internet: Die Herkunft konnte nicht bei Modrinth geprüft werden.)\n");
		}
		for (Stufe st : new Stufe[] {Stufe.GEFAEHRLICH, Stufe.VERDAECHTIG, Stufe.UNBEKANNT}) {
			for (Ergebnis e : b.mit(st)) {
				s.append("\n").append(st == Stufe.GEFAEHRLICH ? "✖ " : st == Stufe.VERDAECHTIG ? "! " : "? ")
						.append(e.name()).append(" – ").append(stufeText(st, en));
				if (!e.gruende.isEmpty()) {
					s.append("\n    ").append(en ? uebersetze(String.join(", ", e.gruende)) : String.join(", ", e.gruende));
				}
			}
		}
		return s.toString();
	}

	private static String uebersetze(String de) {
		String[][] w = {
				{"fractureiser-Schadsoftware", "fractureiser malware"},
				{"schickt Daten an einen Discord-Webhook", "sends data to a Discord webhook"},
				{"schickt Daten an einen Telegram-Bot", "sends data to a Telegram bot"},
				{"liest Discord- oder Browser-Anmeldedaten", "reads Discord or browser login data"},
				{"liest Discord-Anmeldedaten", "reads Discord login data"},
				{"liest gespeicherte Browser-Passwörter", "reads saved browser passwords"},
				{"liest Browser-Schlüssel", "reads browser keys"},
				{"liest Browser-Daten", "reads browser data"},
				{"sucht Krypto-Wallets", "looks for crypto wallets"},
				{"liest Minecraft-Anmeldedaten des offiziellen Launchers", "reads login data of the official Minecraft Launcher"},
				{"liest Anmeldedaten anderer Clients", "reads login data of other clients"},
				{"liest Anmeldedaten von Lunar Client", "reads Lunar Client login data"},
				{"liest Anmeldedaten von Feather", "reads Feather login data"},
				{"liest Anmeldedaten anderer Launcher", "reads login data of other launchers"},
				{"kann andere Programme starten", "can start other programs"},
				{"kann Code aus dem Internet nachladen", "can load code from the internet"},
				{"lädt Inhalte von Pastebin", "loads content from Pastebin"},
				{"verbindet sich mit einem Tunnel-Server", "connects to a tunnel server"},
				{"lädt Dateien von Discord herunter", "downloads files from Discord"},
				{"greift auf den Minecraft-Zugangsschlüssel zu", "accesses the Minecraft access token"},
				{"versteckte Adresse zum Datenversand (Base64)", "hidden address for sending data (Base64)"},
				{"sehr groß – Inhalt nicht geprüft", "very large – contents not checked"},
				{"konnte nicht gelesen werden", "could not be read"},
				{"Datei ist beschädigt oder keine echte Mod", "file is damaged or not a real mod"},
		};
		String r = de;
		for (String[] p : w) r = r.replace(p[0], p[1]);
		return r;
	}
}
