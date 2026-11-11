package de.bambus.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.world.entity.player.Player;

import java.io.InputStream;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Kosmetik für alle: Deine Auswahl (Cape, Flügel, Hut, Partikel) wird online gespeichert, und du siehst die
 * Auswahl anderer Bambus-Spieler in deiner Nähe.
 *
 * <p>Speicher ist ein Supabase-Projekt (siehe supabase/README.md). Lesen darf jeder. Schreiben geht nur über die
 * Funktion „kosmetik“: Der Client meldet sich dafür wie beim Betreten eines Servers bei Mojang an
 * ({@code joinServer}), und die Funktion fragt Mojang ({@code hasJoined}), ob das wirklich du bist. So kann niemand
 * die Kosmetik eines anderen Spielers ändern. Dein Zugangs-Token verlässt dabei nie deinen Computer
 * (nur Richtung Mojang, wie beim normalen Spielen).
 *
 * <p>Geteilt werden nur eingebaute Designs – selbst erstellte Capes und Flügel bleiben nur bei dir sichtbar.
 */
public final class KosmetikOnline {

	/** Was ein Spieler trägt (Ids, null = nichts). */
	public record Auswahl(String cape, String fluegel, String hut, String partikel) {
		public static final Auswahl LEER = new Auswahl(null, null, null, null);
	}

	private record Eintrag(Auswahl auswahl, long zeit) {}

	private static final long FRISCH_MS = 5 * 60_000L;       // so lange gilt eine geladene Auswahl
	private static final long WARTEN_NACH_AENDERUNG = 3_000L; // erst hochladen, wenn du dich entschieden hast
	private static final int MAX_PRO_ANFRAGE = 60;

	private static final String URL;
	private static final String SCHLUESSEL;

	static {
		Properties p = new Properties();
		try (InputStream in = KosmetikOnline.class.getResourceAsStream("/assets/bambus/online.properties")) {
			if (in != null) {
				p.load(new java.io.InputStreamReader(in, StandardCharsets.UTF_8));
			}
		} catch (Exception e) {
			BambusClient.LOGGER.warn("Bambus online.properties nicht lesbar: {}", e.toString());
		}
		String url = p.getProperty("supabase.url", "").trim();
		URL = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
		SCHLUESSEL = p.getProperty("supabase.schluessel", "").trim();
	}

	private static final Map<UUID, Eintrag> CACHE = new ConcurrentHashMap<>();
	private static final Set<UUID> LAEDT = ConcurrentHashMap.newKeySet();
	private static final ExecutorService ARBEITER = Executors.newSingleThreadExecutor(r -> {
		Thread t = new Thread(r, "Bambus-Kosmetik-Online");
		t.setDaemon(true);
		return t;
	});
	private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
	private static final SecureRandom ZUFALL = new SecureRandom();

	private static int ticks;
	private static volatile Auswahl hochgeladen;
	private static Auswahl wunsch;
	private static long wunschSeit;
	private static volatile long naechsterVersuch;
	private static volatile int fehler;
	private static volatile boolean laedtHoch;

	private KosmetikOnline() {}

	/** Ist ein Supabase-Projekt eingetragen? */
	public static boolean eingerichtet() {
		return !URL.isEmpty() && !SCHLUESSEL.isEmpty();
	}

	/** Online-Auswahl eines anderen Spielers – oder null, solange nichts bekannt ist. */
	public static Auswahl von(UUID spieler) {
		Eintrag e = CACHE.get(spieler);
		return e == null ? null : e.auswahl();
	}

	/** Jeden Tick (Client-Thread). */
	public static void tick(Minecraft mc) {
		if (!eingerichtet() || mc.level == null || mc.player == null) {
			return;
		}
		ticks++;
		if (BambusClient.config.kosmetikAndere && ticks % 40 == 0) {
			ladeSpielerInDerNaehe(mc);
		}
		if (BambusClient.config.kosmetikTeilen) {
			hochladenWennNoetig(mc);
		}
	}

	// ---------------------------------------------------------------- Laden

	private static void ladeSpielerInDerNaehe(Minecraft mc) {
		long jetzt = System.currentTimeMillis();
		List<UUID> fehlend = new ArrayList<>();
		for (Player p : mc.level.players()) {
			UUID id = p.getUUID();
			if (p == mc.player || id.version() != 4 || LAEDT.contains(id)) {
				continue;   // nur echte Mojang-Konten (Version 4) – keine NPCs oder Offline-Spieler
			}
			Eintrag e = CACHE.get(id);
			if (e == null || jetzt - e.zeit() > FRISCH_MS) {
				fehlend.add(id);
				if (fehlend.size() >= MAX_PRO_ANFRAGE) {
					break;
				}
			}
		}
		if (fehlend.isEmpty()) {
			return;
		}
		LAEDT.addAll(fehlend);
		ARBEITER.execute(() -> {
			try {
				lade(fehlend);
			} catch (Exception ex) {
				BambusClient.LOGGER.debug("Bambus-Kosmetik laden: {}", ex.toString());
				// beim nächsten Mal wieder versuchen – aber nicht sofort
				long spaeter = System.currentTimeMillis() - FRISCH_MS + 60_000L;
				for (UUID id : fehlend) {
					CACHE.putIfAbsent(id, new Eintrag(Auswahl.LEER, spaeter));
				}
			} finally {
				fehlend.forEach(LAEDT::remove);
			}
		});
	}

	private static void lade(List<UUID> spieler) throws Exception {
		StringBuilder liste = new StringBuilder();
		for (UUID id : spieler) {
			if (liste.length() > 0) {
				liste.append(',');
			}
			liste.append(id);
		}
		HttpRequest anfrage = HttpRequest.newBuilder(URI.create(URL
						+ "/rest/v1/kosmetik?select=uuid,cape,fluegel,hut,partikel&uuid=in.(" + liste + ")"))
				.timeout(Duration.ofSeconds(10))
				.header("apikey", SCHLUESSEL)
				.header("Accept", "application/json")
				.GET().build();
		HttpResponse<String> antwort = HTTP.send(anfrage, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
		if (antwort.statusCode() != 200) {
			throw new IllegalStateException("HTTP " + antwort.statusCode());
		}
		long jetzt = System.currentTimeMillis();
		for (UUID id : spieler) {
			CACHE.put(id, new Eintrag(Auswahl.LEER, jetzt));   // nicht gefunden = trägt nichts
		}
		JsonArray zeilen = JsonParser.parseString(antwort.body()).getAsJsonArray();
		for (JsonElement el : zeilen) {
			JsonObject z = el.getAsJsonObject();
			UUID id = UUID.fromString(text(z, "uuid"));
			Auswahl a = new Auswahl(
					Kosmetik.eingebautesCape(text(z, "cape")),
					Kosmetik.eingebauteFluegel(text(z, "fluegel")),
					Kosmetik.eingebauterHut(text(z, "hut")),
					Kosmetik.eingebautePartikel(text(z, "partikel")));
			CACHE.put(id, new Eintrag(a, jetzt));
		}
	}

	private static String text(JsonObject o, String feld) {
		JsonElement e = o.get(feld);
		return e == null || e.isJsonNull() ? null : e.getAsString();
	}

	// ---------------------------------------------------------------- Hochladen

	private static void hochladenWennNoetig(Minecraft mc) {
		Auswahl aktuell = Kosmetik.eigeneAuswahlFuerAlle();
		if (aktuell.equals(hochgeladen) || laedtHoch) {
			return;
		}
		long jetzt = System.currentTimeMillis();
		if (!aktuell.equals(wunsch)) {
			wunsch = aktuell;
			wunschSeit = jetzt;
		}
		if (jetzt - wunschSeit < WARTEN_NACH_AENDERUNG || jetzt < naechsterVersuch) {
			return;
		}
		User nutzer = mc.getUser();
		var sitzung = mc.services().sessionService();
		Auswahl senden = aktuell;
		laedtHoch = true;
		ARBEITER.execute(() -> {
			try {
				hochladen(nutzer, sitzung, senden);
				hochgeladen = senden;
				fehler = 0;
			} catch (Exception ex) {
				fehler++;
				naechsterVersuch = System.currentTimeMillis() + Math.min(10 * 60_000L, 30_000L << Math.min(fehler, 5));
				BambusClient.LOGGER.info("Bambus-Kosmetik konnte nicht geteilt werden: {}", ex.toString());
			} finally {
				laedtHoch = false;
			}
		});
	}

	private static void hochladen(User nutzer, com.mojang.authlib.minecraft.MinecraftSessionService sitzung, Auswahl a)
			throws Exception {
		// 1. Bei Mojang „einem Server beitreten“ – mit einer Zufallskennung statt einer echten Server-Adresse
		String serverId = new BigInteger(130, ZUFALL).toString(16);
		sitzung.joinServer(nutzer.getProfileId(), nutzer.getAccessToken(), serverId);

		// 2. Die Bambus-Funktion prüft das bei Mojang und speichert deine Auswahl
		JsonObject daten = new JsonObject();
		daten.addProperty("name", nutzer.getName());
		daten.addProperty("serverId", serverId);
		daten.addProperty("cape", a.cape());
		daten.addProperty("fluegel", a.fluegel());
		daten.addProperty("hut", a.hut());
		daten.addProperty("partikel", a.partikel());
		HttpRequest anfrage = HttpRequest.newBuilder(URI.create(URL + "/functions/v1/kosmetik"))
				.timeout(Duration.ofSeconds(15))
				.header("apikey", SCHLUESSEL)
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(daten.toString(), StandardCharsets.UTF_8))
				.build();
		HttpResponse<String> antwort = HTTP.send(anfrage, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
		if (antwort.statusCode() != 200) {
			throw new IllegalStateException("HTTP " + antwort.statusCode() + " " + antwort.body());
		}
		CACHE.put(nutzer.getProfileId(), new Eintrag(a, System.currentTimeMillis()));
	}
}
