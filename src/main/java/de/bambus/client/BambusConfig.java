package de.bambus.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Alle Einstellungen des Bambus Clients. Gespeichert in .minecraft/config/bambus.json */
public class BambusConfig {

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public static class ModulPos {
		public boolean an = true;
		public int x;
		public int y;
		/** Größe im HUD (1 = 100 %), im HUD-Editor mit Ecke oder Mausrad änderbar. */
		public float groesse = 1f;
		/** Einstellungen des Moduls (⋮ im Mod-Menü), z. B. "cps" → false. Fehlt ein Eintrag, gilt der Standard. */
		public Map<String, Boolean> optionen = new LinkedHashMap<>();

		public ModulPos() {}

		public ModulPos(int x, int y) {
			this.x = x;
			this.y = y;
		}
	}

	// HUD
	public boolean hudAn = true;
	public Map<String, ModulPos> module = new LinkedHashMap<>();

	// Funktionen
	public boolean toggleSprint = false;
	public boolean toggleSneak = false;
	public boolean scrollZoom = true;
	public boolean freelook = true;
	public boolean fullbright = false;
	public boolean capeAn = true;
	public boolean bambusHauptmenue = true;

	// Anzeigen anpassen (Scoreboard, Bossbar, Tabliste, Chat): an/aus und Größe in Prozent
	public boolean scoreboardAn = true;
	public int scoreboardGroesse = 100;
	public boolean bossbarAn = true;
	public int bossbarGroesse = 100;
	public int tablisteGroesse = 100;
	public int chatGroesse = 100;

	// Client-Oberfläche
	public int thema = 0;                 // Farben.THEMEN
	public boolean verlauf = true;        // Farbverlauf an HUD-Kästen
	public boolean menueAnimation = true;
	public boolean hinweise = true;        // Neon-Benachrichtigungen
	public boolean ladebildschirm = true;  // eigener Ladebildschirm
	public String menueStil = "bambus";
	public int musikDurchsicht = 70;       // Musik-Insel: Durchsichtigkeit in Prozent
	public int musikSchriftInsel = 1;      // Schriften.NAMEN: 0 Minecraft, 1 Poppins, 2 Montserrat, 3 Pixel
	public int musikSchriftWoerter = 2;
	public String emoteRad = de.bambus.client.emote.Emotes.STANDARD_RAD;   // Belegung der 8 Plätze im Emote-Rad
	public boolean emoteMusik = true;      // eigener kleiner Beat bei Tanz-Emotes
	public int musikWoerterTempo = 2;      // SongtextWelt.TEMPO: wie schnell die Wörter verschwinden (2 = Normal)
	public int musikWoerterOrt = 0;        // SongtextWelt.ORTE: Seiten, Links, Rechts, Oben, Unten, Überall     // Aussehen der Menüs: bambus, clean, kompakt (screen.MenueStil)
	public String menueHintergrund = "synthwave";   // Bambus-Hauptmenü: synthwave, partikel, aurora, datenregen, gitter
	public String letzteVersion = "";      // für „Bambus wurde aktualisiert“
	/** Server-Favoriten aus dem Launcher, je „Name|Adresse“ – landen beim Start in der Mehrspieler-Liste. */
	public java.util.List<String> serverFavoriten = new java.util.ArrayList<>();
	public java.util.List<String> favoriten = new java.util.ArrayList<>();
	/** Bambus-Serverauswahl statt der Minecraft-Liste. */
	public boolean serverMenue = true;
	/** Mit ★ markierte Server (Adressen, klein geschrieben) – stehen in der Serverauswahl oben. */
	public java.util.List<String> serverSterne = new java.util.ArrayList<>();

	// Extras
	public java.util.List<String> freunde = new java.util.ArrayList<>();
	public boolean freundHinweis = true;   // Meldung, wenn ein Freund auf den Server kommt
	public boolean freundSchutz = false;   // Freunde lassen sich nicht schlagen
	public boolean autoScreenshot = false;
	public boolean discordAn = false;
	public boolean helferAn = true;        // Bambus-Helfer bei Rechts-Shift anbieten

	// Texturen (Mod-Menü → MODULE → Texturen)
	public boolean texturenAn = true;     // Hauptschalter der Karte „Texturen“
	public boolean lowFire = false;
	public int lowFireStaerke = 30;       // Prozent, siehe Texturen
	public boolean lowShield = false;
	public int lowShieldStaerke = 30;
	public boolean sideShield = false;
	public int sideShieldDrehung = 60;    // Grad: Schild seitlich gedreht wie im Texturpaket
	public boolean shortSword = false;
	public int shortSwordGroesse = 70;    // Prozent der normalen Größe
	public boolean keinKuerbisOverlay = false;
	public boolean klaresWasser = false;
	public String discordAppId = "";

	// PvP
	/** Genaue Lebenspunkte des Gegners im Target-HUD (auf manchen Servern verboten, daher aus). */
	public boolean gegnerLebenZahl = false;

	// Visuals
	public int zeitModus = 0;      // 0 normal, 1 Morgen, 2 Mittag, 3 Abend, 4 Nacht
	public int wetterModus = 0;    // 0 normal, 1 klar, 2 Regen, 3 Gewitter
	public boolean trefferPartikel = true;
	public boolean lowEnd = false;
	public int[] lowEndAlt = null; // gemerkte Grafikwerte vor dem Low-End-Modus

	// Kosmetik ("aus" = nichts ausgewählt)
	// ===== Quests und Blätter (der Launcher rechnet damit, siehe Fortschritt.java) =====
	public int punkte = 0;              // Guthaben
	public int punkteGesamt = 0;        // insgesamt gesammelt – daraus kommt der Rang
	public int streak = 0;              // Tage in Folge
	public String letzterTag = "";
	public int letzterBonus = 0;
	public String questTag = "";
	public String questFertig = "";
	public String erfolge = "";
	public String shop = "";            // freigeschaltete Shop-Kosmetik, mit Komma getrennt
	public String farbenGesehen = "";
	public int helferFragen = 0;
	public int spielminuten = 0;        // zählt die Mod hoch
	public String spielTag = "";
	public int spielBonusHeute = 0;     // schon vergebene Spiel-Blätter von heute
	public String spielVerlauf = "";    // Spielzeit je Tag, z. B. „2026-11-14:35,2026-11-13:120“ (30 Tage)
	// Wegpunkte: „Name|x|y|z|Farbe|Ort|Dimension|sichtbar“, Ort = Server-Adresse oder „sp:Weltname“
	public java.util.List<String> wegpunkte = new java.util.ArrayList<>();
	public boolean wegpunkteImKompass = true;
	// Server-Profile: Adresse → Einstellungs-Profil (0 = PvP, 1 = Bauen, 2 = Eigenes)
	public java.util.Map<String, Integer> serverProfile = new java.util.LinkedHashMap<>();
	// Inventar sortieren: „name“, „art“ oder „menge“
	public String sortierRegel = "art";
	public String hartTag = "";         // Tag der aktuellen Herausforderungen
	public String hartFertig = "";      // heute geschaffte Herausforderungen
	public String hartStand = "";       // Zwischenstände, z. B. "kills5:2,sprint1000:640"
	public String hartStufe = "";       // erreichte Stufe je Herausforderung, z. B. "combo5:3"
	public boolean kosmetikUmgestellt = false; // Start-Set und Getragenes einmalig freigeschaltet

	public String capeDesign = "bambus";
	public String partikel = "aus";
	public String fluegel = "aus";
	public boolean fluegelBlock = false;  // Flügel-Stil: false = HD (weich, 24 Streifen), true = Blockbench (eckig, grobe Pixel)
	public String hut = "aus";
	public String killEffekt = "aus";
	public String spur = "aus";

	// Fadenkreuz
	public boolean fadenkreuzAn = false;
	public int fadenkreuzForm = 0;
	public int fadenkreuzFarbe = 0;
	public int fadenkreuzGroesse = 4;

	public ModulPos modul(String id, int stdX, int stdY) {
		return modul(id, stdX, stdY, true);
	}

	/** Neue Module können standardmäßig aus sein, damit das HUD nicht überladen wirkt. */
	public ModulPos modul(String id, int stdX, int stdY, boolean stdAn) {
		return module.computeIfAbsent(id, k -> {
			ModulPos p = new ModulPos(stdX, stdY);
			p.an = stdAn;
			return p;
		});
	}

	/** Liest Einstellungen aus einem JSON-Text (Import / Profile). Gibt null zurück, wenn ungültig. */
	public static BambusConfig ausJson(String json) {
		try {
			BambusConfig c = GSON.fromJson(json, BambusConfig.class);
			if (c != null && c.module == null) {
				c.module = new LinkedHashMap<>();
			}
			if (c != null && c.favoriten == null) c.favoriten = new java.util.ArrayList<>();
			if (c != null && c.freunde == null) c.freunde = new java.util.ArrayList<>();
			return c;
		} catch (Exception e) {
			return null;
		}
	}

	public String alsJson() {
		return GSON.toJson(this);
	}

	private static Path datei() {
		return FabricLoader.getInstance().getConfigDir().resolve("bambus.json");
	}

	/**
	 * Änderungszeit der Datei nach dem letzten eigenen Laden oder Speichern. Ändert sie sich danach,
	 * hat der Launcher gespeichert (z. B. Kosmetik gekauft) – dann lädt die Mod neu, statt es zu überschreiben.
	 */
	private static volatile long bekannteAenderung = Long.MIN_VALUE;
	private static final Object SCHLOSS = new Object();

	public static BambusConfig laden() {
		Path p = datei();
		if (!Files.exists(p)) {
			return new BambusConfig();
		}
		for (int versuch = 0; versuch < 4; versuch++) {
			BambusConfig c = lesen(p);
			if (c != null) return c;
			try {
				Thread.sleep(40);   // vielleicht schreibt der Launcher gerade
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}
		// kaputte Datei aufheben, bevor mit Standardwerten weitergemacht wird – nichts geht unbemerkt verloren
		try {
			Path sicherung = p.resolveSibling("bambus.json.kaputt-" + java.time.LocalDateTime.now()
					.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")));
			Files.copy(p, sicherung, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
			System.err.println("[Bambus] bambus.json war unlesbar – gesichert als " + sicherung.getFileName());
		} catch (Exception e) {
			System.err.println("[Bambus] Einstellungen konnten nicht gelesen werden: " + e);
		}
		return new BambusConfig();
	}

	/** Liest die Datei; null, wenn sie fehlt oder unlesbar ist. */
	private static BambusConfig lesen(Path p) {
		synchronized (SCHLOSS) {
			try {
				long zeit = aenderungszeit();
				BambusConfig c;
				try (Reader r = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
					c = GSON.fromJson(r, BambusConfig.class);
				}
				if (c == null) return null;
				if (c.module == null) c.module = new LinkedHashMap<>();
				if (c.favoriten == null) c.favoriten = new java.util.ArrayList<>();
				if (c.freunde == null) c.freunde = new java.util.ArrayList<>();
				bekannteAenderung = zeit;
				return c;
			} catch (Exception e) {
				return null;
			}
		}
	}

	/** Hat jemand anderes (der Launcher) die Datei seit dem letzten Laden/Speichern geändert? Dann der neue Stand, sonst null. */
	public static BambusConfig vonAussenGeaendert() {
		long t = aenderungszeit();
		if (t == -1 || t == bekannteAenderung) return null;
		BambusConfig neu = lesen(datei());
		if (neu == null) bekannteAenderung = t;   // unlesbar: nicht jede Sekunde neu versuchen, eigener Stand bleibt
		return neu;
	}

	private static long aenderungszeit() {
		try {
			return Files.getLastModifiedTime(datei()).toMillis() * 31 + Files.size(datei());
		} catch (Exception e) {
			return -1;
		}
	}

	/** Speichert sicher: erst in eine Zwischendatei, dann in einem Schritt austauschen – nie eine halbe Datei. */
	public void speichern() {
		synchronized (SCHLOSS) {
			try {
				Path ziel = datei();
				Files.createDirectories(ziel.getParent());
				Path tmp = ziel.resolveSibling("bambus.json.speichern");
				try (Writer w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
					GSON.toJson(this, w);
				}
				try {
					Files.move(tmp, ziel, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
				} catch (java.io.IOException e) {
					Files.move(tmp, ziel, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
				}
				bekannteAenderung = aenderungszeit();
			} catch (Exception e) {
				System.err.println("[Bambus] Einstellungen konnten nicht gespeichert werden: " + e);
			}
		}
	}
}
