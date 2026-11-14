package de.bambus.client;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.bambus.helfer.EinstellungsCode;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Einstellungen per Code teilen: alle Bambus-Module mit Anordnung, Größe und Einstellungen, die Bambus-Funktionen
 * und alle Minecraft-Einstellungen (inklusive Tastenbelegung).
 *
 * Bewusst NICHT im Code: Blätter, Quests, freigeschaltete Kosmetik, Freunde, Discord-ID – und Minecraft-Werte,
 * die nur auf einem PC passen (Vollbild-Auflösung, Soundgerät, Ressourcenpakete).
 */
public final class EinstellungsTransfer {

	private EinstellungsTransfer() {}

	private static final Gson GSON = new Gson();

	/** Bambus-Einstellungen, die geteilt werden dürfen (alles andere bleibt auf dem eigenen PC). */
	private static final Set<String> BAMBUS = Set.of(
			"hudAn", "module", "favoriten",
			"toggleSprint", "toggleSneak", "scrollZoom", "freelook", "fullbright", "capeAn", "bambusHauptmenue",
			"scoreboardAn", "scoreboardGroesse", "bossbarAn", "bossbarGroesse", "tablisteGroesse", "chatGroesse",
			"thema", "menueStil", "musikDurchsicht", "musikSchriftInsel", "musikSchriftWoerter", "verlauf", "menueAnimation", "hinweise", "ladebildschirm", "menueHintergrund", "serverMenue",
			"autoScreenshot", "helferAn",
			"texturenAn", "lowFire", "lowFireStaerke", "lowShield", "lowShieldStaerke", "sideShield", "sideShieldDrehung",
			"shortSword", "shortSwordGroesse", "keinKuerbisOverlay", "klaresWasser",
			"gegnerLebenZahl", "zeitModus", "wetterModus", "trefferPartikel",
			"fadenkreuzAn", "fadenkreuzForm", "fadenkreuzFarbe", "fadenkreuzGroesse");

	/** Minecraft-Optionen, die nicht übertragen werden: hängen am PC oder sind nur interne Merker. */
	private static final Set<String> MC_NICHT = Set.of(
			"version", "lastServer", "resourcePacks", "incompatibleResourcePacks", "fullscreen", "fullscreenResolution",
			"soundDevice", "joinedFirstServer", "tutorialStep", "onboardAccessibility", "skipMultiplayerWarning",
			"syncChunkWrites", "overrideWidth", "overrideHeight", "glDebugVerbosity", "hideBundleTutorial");

	/** Was ein Code enthält – für die Vorschau vor dem Übernehmen. */
	public record Inhalt(JsonObject bambus, Map<String, String> minecraft) {
		public int module() {
			JsonElement m = bambus == null ? null : bambus.get("module");
			return m != null && m.isJsonObject() ? m.getAsJsonObject().size() : 0;
		}

		public int bambusWerte() {
			return bambus == null ? 0 : bambus.size();
		}

		public int tasten() {
			int n = 0;
			for (String k : minecraft.keySet()) if (k.startsWith("key_")) n++;
			return n;
		}
	}

	// ================================================================ Code erzeugen

	public static String code(Minecraft mc) {
		JsonObject wurzel = new JsonObject();
		wurzel.addProperty("v", 1);
		wurzel.addProperty("von", "Bambus Client " + BambusClient.VERSION);

		JsonObject alles = JsonParser.parseString(BambusClient.config.alsJson()).getAsJsonObject();
		JsonObject bambus = new JsonObject();
		for (String k : BAMBUS) {
			if (alles.has(k)) bambus.add(k, alles.get(k));
		}
		wurzel.add("bambus", bambus);

		JsonObject minecraft = new JsonObject();
		mc.options.save();   // aktuellen Stand in die options.txt schreiben
		for (Map.Entry<String, String> e : leseOptionen(mc.options.getFile().toPath()).entrySet()) {
			if (!MC_NICHT.contains(e.getKey())) minecraft.addProperty(e.getKey(), e.getValue());
		}
		wurzel.add("minecraft", minecraft);
		return EinstellungsCode.verpacken(GSON.toJson(wurzel));
	}

	// ================================================================ Code lesen

	/** Liest einen Code (oder einen alten JSON-Export). Ungültig: null. */
	public static Inhalt lies(String eingabe) {
		String json = EinstellungsCode.entpacken(eingabe);
		if (json == null) return null;
		try {
			JsonObject o = JsonParser.parseString(json).getAsJsonObject();
			JsonObject quelle = o.has("bambus") && o.get("bambus").isJsonObject() ? o.getAsJsonObject("bambus")
					: o.has("module") ? o : null;   // alter Export: die ganze bambus.json
			JsonObject bambus = null;
			if (quelle != null) {
				bambus = new JsonObject();
				for (String k : BAMBUS) {
					if (quelle.has(k)) bambus.add(k, quelle.get(k));
				}
			}
			Map<String, String> minecraft = new LinkedHashMap<>();
			if (o.has("minecraft") && o.get("minecraft").isJsonObject()) {
				for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("minecraft").entrySet()) {
					if (MC_NICHT.contains(e.getKey()) || !e.getValue().isJsonPrimitive()) continue;
					String wert = e.getValue().getAsString();
					if (e.getKey().matches("[A-Za-z0-9_.]{1,64}") && wert.length() < 4000 && !wert.contains("\n")) {
						minecraft.put(e.getKey(), wert);
					}
				}
			}
			if ((bambus == null || bambus.size() == 0) && minecraft.isEmpty()) return null;
			return new Inhalt(bambus, minecraft);
		} catch (Exception e) {
			return null;
		}
	}

	// ================================================================ Übernehmen

	/** Übernimmt die gewählten Teile. Gibt eine kurze Meldung zurück. */
	public static String anwenden(Minecraft mc, Inhalt inhalt, boolean bambusAuch, boolean minecraftAuch) {
		List<String> teile = new ArrayList<>();
		if (bambusAuch && inhalt.bambus() != null && inhalt.bambus().size() > 0) {
			JsonObject alles = JsonParser.parseString(BambusClient.config.alsJson()).getAsJsonObject();
			for (Map.Entry<String, JsonElement> e : inhalt.bambus().entrySet()) {
				alles.add(e.getKey(), e.getValue());
			}
			BambusConfig neu = BambusConfig.ausJson(GSON.toJson(alles));
			if (neu == null) return "Der Code enthält ungültige Bambus-Einstellungen";
			BambusClient.config = neu;
			neu.speichern();
			BambusClient.toggleSprint().ausConfigUebernehmen(neu);
			Funktionen.sprintUndSchleichenAnwenden(mc);
			teile.add(inhalt.module() + " Module");
		}
		if (minecraftAuch && !inhalt.minecraft().isEmpty()) {
			try {
				mc.options.save();
				Path datei = mc.options.getFile().toPath();
				Map<String, String> optionen = leseOptionen(datei);
				optionen.putAll(inhalt.minecraft());
				StringBuilder text = new StringBuilder();
				for (Map.Entry<String, String> e : optionen.entrySet()) {
					text.append(e.getKey()).append(':').append(e.getValue()).append('\n');
				}
				Files.writeString(datei, text.toString(), StandardCharsets.UTF_8);
				mc.options.load();
				KeyMapping.resetMapping();
				for (SoundSource s : SoundSource.values()) {
					mc.getSoundManager().refreshCategoryVolume(s);
				}
				mc.resizeDisplay();
				if (mc.level != null) mc.levelRenderer.allChanged();
				mc.options.broadcastOptions();
				mc.options.save();
				teile.add(inhalt.minecraft().size() + " Minecraft-Einstellungen");
			} catch (Exception e) {
				BambusClient.LOGGER.warn("Minecraft-Einstellungen nicht übernommen", e);
				return "Minecraft-Einstellungen konnten nicht übernommen werden";
			}
		}
		return teile.isEmpty() ? "Nichts übernommen" : String.join(" und ", teile) + " übernommen";
	}

	/** options.txt als geordnete Liste „Name → Wert“ (Wert = alles nach dem ersten Doppelpunkt). */
	private static Map<String, String> leseOptionen(Path datei) {
		Map<String, String> m = new LinkedHashMap<>();
		try {
			for (String zeile : Files.readAllLines(datei, StandardCharsets.UTF_8)) {
				int i = zeile.indexOf(':');
				if (i > 0) m.put(zeile.substring(0, i), zeile.substring(i + 1));
			}
		} catch (Exception e) {
			BambusClient.LOGGER.warn("options.txt nicht lesbar: {}", e.toString());
		}
		return m;
	}
}
