package de.bambus.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Einstellungs-Profile (z. B. PvP, Bauen, Aufnehmen) und Import/Export über die Zwischenablage.
 *
 * Ein Profil merkt sich Module, HUD-Anordnung, Funktionen und die Ressourcenpakete. Beim Laden werden
 * nur diese Teile übernommen – Blätter, Quests und Kosmetik bleiben unangetastet.
 *
 * Server-Profile: Jedem Server kann ein Profil zugeordnet werden; die Mod lädt es beim Betreten.
 */
public final class Profile {

	private Profile() {}

	public static final String[] NAMEN = {"PvP", "Bauen", "Eigenes"};
	private static final String PAKETE = "bambusRessourcenpakete";

	private static Path datei(int slot) {
		return FabricLoader.getInstance().getConfigDir().resolve("bambus-profil-" + (slot + 1) + ".json");
	}

	public static boolean vorhanden(int slot) {
		return Files.isRegularFile(datei(slot));
	}

	public static boolean speichern(int slot) {
		try {
			JsonObject o = JsonParser.parseString(BambusClient.config.alsJson()).getAsJsonObject();
			Minecraft mc = Minecraft.getInstance();
			JsonArray pakete = new JsonArray();
			for (String p : mc.options.resourcePacks) pakete.add(p);
			o.add(PAKETE, pakete);
			Files.writeString(datei(slot), o.toString(), StandardCharsets.UTF_8);
			return true;
		} catch (Exception e) {
			BambusClient.LOGGER.warn("Profil nicht speicherbar: {}", e.toString());
			return false;
		}
	}

	/** Lädt die Einstellungen eines Profils (ohne Blätter, Quests, Kosmetik) und seine Ressourcenpakete. */
	public static boolean laden(int slot) {
		try {
			String json = Files.readString(datei(slot), StandardCharsets.UTF_8);
			Minecraft mc = Minecraft.getInstance();
			boolean ok = EinstellungsTransfer.nurEinstellungen(mc, json);
			if (ok) paketeAnwenden(mc, json);
			return ok;
		} catch (Exception e) {
			return false;
		}
	}

	/** Übernimmt Einstellungen aus JSON (Zwischenablage) – auch hier ohne Blätter und Kosmetik. */
	public static boolean uebernehmen(String json) {
		return EinstellungsTransfer.nurEinstellungen(Minecraft.getInstance(), json);
	}

	/**
	 * Stellt die Ressourcenpakete des Profils ein, wenn sie sich von den jetzigen unterscheiden.
	 * Minecraft lädt dann kurz neu.
	 */
	private static void paketeAnwenden(Minecraft mc, String json) {
		try {
			JsonObject o = JsonParser.parseString(json).getAsJsonObject();
			if (!o.has(PAKETE) || !o.get(PAKETE).isJsonArray()) return;
			List<String> soll = new ArrayList<>();
			for (JsonElement e : o.getAsJsonArray(PAKETE)) soll.add(e.getAsString());
			if (soll.equals(mc.options.resourcePacks)) return;
			mc.options.resourcePacks.clear();
			mc.options.resourcePacks.addAll(soll);
			mc.getResourcePackRepository().setSelected(soll);
			mc.options.save();
			mc.reloadResourcePacks();
		} catch (Exception e) {
			BambusClient.LOGGER.warn("Ressourcenpakete des Profils nicht übernommen: {}", e.toString());
		}
	}

	// ============================================================ Server-Profile

	/** Adresse des Servers, auf dem man gerade spielt (klein geschrieben), oder null. */
	public static String serverAdresse(Minecraft mc) {
		if (mc.hasSingleplayerServer()) return null;
		ServerData s = mc.getCurrentServer();
		return s == null ? null : s.ip.toLowerCase(Locale.ROOT);
	}

	/** Zugeordnetes Profil eines Servers, oder -1. */
	public static int fuerServer(String adresse) {
		if (adresse == null) return -1;
		Integer slot = BambusClient.config.serverProfile.get(adresse);
		return slot == null || slot < 0 || slot >= NAMEN.length ? -1 : slot;
	}

	/** Ordnet einem Server ein Profil zu (-1 = keins). */
	public static void zuordnen(String adresse, int slot) {
		if (adresse == null) return;
		if (slot < 0) BambusClient.config.serverProfile.remove(adresse);
		else BambusClient.config.serverProfile.put(adresse, slot);
		BambusClient.config.speichern();
	}

	/** Beim Betreten eines Servers: zugeordnetes Profil laden. */
	public static void serverBetreten(Minecraft mc) {
		String adresse = serverAdresse(mc);
		int slot = fuerServer(adresse);
		if (slot < 0 || !vorhanden(slot)) return;
		if (laden(slot)) {
			Hinweise.zeige("server", "Profil „" + NAMEN[slot] + "“ geladen", "für " + adresse, Farben.NEON);
		}
	}
}
