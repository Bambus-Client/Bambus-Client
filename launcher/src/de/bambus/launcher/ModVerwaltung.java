package de.bambus.launcher;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Mods-Verwaltung: Profile, empfohlene Mods (FPS-Boost, Mod Menu), Mods von Modrinth und CurseForge
 * und eigene .jar-Dateien.
 *
 * Profile (wie in der Modrinth-App): Jedes Profil hat seine eigene Mod-Auswahl. Beim SPIELEN wird der
 * Mods-Ordner auf den Stand des aktiven Profils gebracht. Eigene .jar-Dateien gelten für alle Profile.
 *
 * Alles kommt direkt von Modrinth bzw. CurseForge (offizielle Download-Adressen der Mod-Autoren) – der
 * Launcher liefert keine fremden Mods mit. CurseForge verlangt einen eigenen API-Schlüssel
 * (kostenlos unter console.curseforge.com); ohne Schlüssel bleibt nur Modrinth.
 * Einstellungen und die Liste der von Bambus verwalteten Dateien liegen in config/bambus-mods.json
 * (eigene Datei, damit die Mod-Konfiguration bambus.json unberührt bleibt).
 */
final class ModVerwaltung {

	/** Modrinth-Projekt-ID der Fabric API – wird vom Installer selbst verwaltet. */
	private static final String FABRIC_API_ID = "P7dR8mSH";
	private static final String API = "https://api.modrinth.com/v2/";
	private static final String CF_API = "https://api.curseforge.com/v1/";
	/** CurseForge: Minecraft = Spiel 432, Mods = Klasse 6, Fabric = Loader 4, Fabric API = Projekt 306612. */
	private static final String CF_FABRIC_API = "306612";
	static final String MODRINTH = "modrinth", CURSEFORGE = "curseforge";
	/** Schlüssel-Vorsatz für CurseForge-Mods in den Listen (Modrinth-Mods stehen mit ihrem Slug drin). */
	private static final String CF = "cf:";

	/** Empfohlene Mods: {slug, Name, Beschreibung, Gruppe}. Gruppe "FPS" gehört zum FPS-Boost. */
	static final String[][] EMPFOHLEN = {
			{"sodium", "Sodium", "Neue Grafik-Engine – oft doppelt so viele FPS", "FPS"},
			{"lithium", "Lithium", "Schnellere Spiel-Logik: Physik, Tiere, Chunks", "FPS"},
			{"ferrite-core", "FerriteCore", "Braucht deutlich weniger Arbeitsspeicher", "FPS"},
			{"immediatelyfast", "ImmediatelyFast", "Zeichnet HUD, Text und Schilder schneller", "FPS"},
			{"entityculling", "Entity Culling", "Verdeckte Tiere und Truhen werden nicht gezeichnet", "FPS"},
			{"dynamic-fps", "Dynamic FPS", "Spart Leistung, wenn Minecraft im Hintergrund ist", "FPS"},
			{"modmenu", "Mod Menu", "„Mods“-Knopf im Spiel: alle Mods ansehen und einstellen", "MENÜ"},
	};

	/**
	 * Ein Suchergebnis oder eine zusätzlich gewählte Mod. slug ist der Schlüssel in den Listen:
	 * bei Modrinth der Projekt-Slug, bei CurseForge „cf:“ + Projektnummer.
	 */
	static final class Projekt {
		final String slug, titel, beschreibung, icon;
		final long downloads;

		Projekt(String slug, String titel, String beschreibung, long downloads, String icon) {
			this.slug = slug;
			this.titel = titel;
			this.beschreibung = beschreibung;
			this.downloads = downloads;
			this.icon = icon == null || icon.isEmpty() ? null : icon;
		}

		String quelle() {
			return slug.startsWith(CF) ? CURSEFORGE : MODRINTH;
		}
	}

	/** Ein Mod-Profil: eigener Name, eigene Minecraft-Version, eigene Mod-Auswahl. */
	static final class Profil {
		final String id, name, version;
		final int mods;
		final long erstellt, zuletzt;   // Millisekunden; zuletzt = zuletzt aktiviert oder gespielt

		Profil(String id, String name, String version, int mods, long erstellt, long zuletzt) {
			this.id = id;
			this.name = name;
			this.version = version;
			this.mods = mods;
			this.erstellt = erstellt;
			this.zuletzt = zuletzt;
		}
	}

	/** Sortierung der Profil-Liste. */
	static final String NACH_ZULETZT = "zuletzt", NACH_NAME = "name", NACH_MODS = "mods", NACH_NEU = "neu";
	/** Ansicht der Profil-Liste. */
	static final String ALS_LISTE = "liste", ALS_KACHELN = "kacheln";

	/** Eine .jar im Mods-Ordner, die der Spieler selbst hineingelegt hat. */
	static final class EigeneMod {
		final Path datei;
		final boolean an;

		EigeneMod(Path datei, boolean an) {
			this.datei = datei;
			this.an = an;
		}

		String name() {
			String n = datei.getFileName().toString();
			return n.replaceAll("\\.disabled$", "").replaceAll("\\.jar$", "");
		}
	}

	private final Installer installer;
	private final Path datei;
	private Map<String, Object> daten = new LinkedHashMap<>();

	ModVerwaltung(Installer installer) {
		this.installer = installer;
		this.datei = installer.spielOrdner.resolve("config").resolve("bambus-mods.json");
		laden();
	}

	// ================================================================ Einstellungen

	private void laden() {
		try {
			if (Files.isRegularFile(datei)) {
				Map<String, Object> m = Json.objekt(Json.parse(new String(Files.readAllBytes(datei), StandardCharsets.UTF_8)));
				if (m != null) {
					daten = m;
				}
			}
		} catch (Exception e) {
			System.err.println("[Bambus] bambus-mods.json nicht lesbar, Standard wird benutzt: " + e);
		}
		profileVorbereiten();
	}

	// ================================================================ Profile

	/** Alte Dateien (vor den Profilen) hatten „an“ und „extra“ ganz oben – das wird das Profil „Standard“. */
	private void profileVorbereiten() {
		Map<String, Object> profile = Json.objekt(daten.get("profile"));
		if (profile == null || profile.isEmpty()) {
			profile = new LinkedHashMap<>();
			Map<String, Object> standard = new LinkedHashMap<>();
			standard.put("name", "Standard");
			standard.put("an", Json.objekt(daten.get("an")) != null ? daten.get("an") : new LinkedHashMap<String, Object>());
			standard.put("extra", Json.objekt(daten.get("extra")) != null ? daten.get("extra") : new LinkedHashMap<String, Object>());
			profile.put("standard", standard);
			daten.put("profile", profile);
			daten.put("aktiv", "standard");
		}
		daten.remove("an");
		daten.remove("extra");
		if (!(daten.get("aktiv") instanceof String) || !profile.containsKey(daten.get("aktiv"))) {
			daten.put("aktiv", profile.keySet().iterator().next());
		}
	}

	private Map<String, Object> alleProfile() {
		return Json.objekt(daten.get("profile"));
	}

	/** Mit diesem Profil wird gespielt. */
	String aktivesProfil() {
		return (String) daten.get("aktiv");
	}

	/** Profil, das gerade in der MODS-Seite geöffnet ist (nur im Speicher) – null = keins. */
	private volatile String offen;

	/** Das geöffnete Profil wird bearbeitet; ist keins offen, gelten Änderungen dem aktiven. */
	String offenesProfil() {
		String o = offen;
		return o != null && alleProfile().containsKey(o) ? o : aktivesProfil();
	}

	/** Öffnet ein Profil zum Bearbeiten (null = zurück zur Liste). */
	void oeffne(String id) {
		offen = id != null && alleProfile().containsKey(id) ? id : null;
	}

	List<Profil> profile() {
		List<Profil> l = new ArrayList<>();
		for (Map.Entry<String, Object> e : alleProfile().entrySet()) {
			Map<String, Object> p = Json.objekt(e.getValue());
			if (p != null) {
				l.add(new Profil(e.getKey(), str(p.get("name"), e.getKey()), str(p.get("version"), Installer.MC_VERSION),
						anzahlMods(p), zahl(p.get("erstellt")), zahl(p.get("zuletzt"))));
			}
		}
		return l;
	}

	Profil profil(String id) {
		for (Profil p : profile()) {
			if (p.id.equals(id)) return p;
		}
		return profile().get(0);
	}

	/** Profile in der gewählten Reihenfolge; das aktive steht bei „zuletzt“ immer oben. */
	List<Profil> profileSortiert() {
		List<Profil> l = profile();
		String nach = profilSortierung();
		java.util.Comparator<Profil> name = (a, b) -> a.name.compareToIgnoreCase(b.name);
		if (NACH_NAME.equals(nach)) {
			l.sort(name);
		} else if (NACH_MODS.equals(nach)) {
			l.sort((a, b) -> a.mods != b.mods ? Integer.compare(b.mods, a.mods) : name.compare(a, b));
		} else if (NACH_NEU.equals(nach)) {
			l.sort((a, b) -> Long.compare(b.erstellt, a.erstellt));
		} else {
			String aktiv = aktivesProfil();
			l.sort((a, b) -> a.id.equals(aktiv) ? -1 : b.id.equals(aktiv) ? 1 : Long.compare(b.zuletzt, a.zuletzt));
		}
		return l;
	}

	String profilSortierung() {
		return str(daten.get("sortierung"), NACH_ZULETZT);
	}

	void setzeProfilSortierung(String nach) {
		daten.put("sortierung", nach);
		speichern();
	}

	String profilAnsicht() {
		return str(daten.get("ansicht"), ALS_LISTE);
	}

	void setzeProfilAnsicht(String ansicht) {
		daten.put("ansicht", ansicht);
		speichern();
	}

	private static long zahl(Object o) {
		return o instanceof Number ? ((Number) o).longValue() : 0;
	}

	private void merkeZuletzt(String id) {
		Map<String, Object> p = Json.objekt(alleProfile().get(id));
		if (p != null) p.put("zuletzt", System.currentTimeMillis());
	}

	/** Eingeschaltete Mods eines Profils (empfohlene + zusätzlich gewählte). */
	private static int anzahlMods(Map<String, Object> profil) {
		Map<String, Object> an = Json.objekt(profil.get("an"));
		Map<String, Object> extra = Json.objekt(profil.get("extra"));
		int n = 0;
		for (String[] m : EMPFOHLEN) {
			Object v = an == null ? null : an.get(m[0]);
			if (!(v instanceof Boolean) || (Boolean) v) n++;
		}
		if (extra != null) {
			for (String k : extra.keySet()) {
				Object v = an == null ? null : an.get(k);
				if (!(v instanceof Boolean) || (Boolean) v) n++;
			}
		}
		return n;
	}

	void setzeAktivesProfil(String id) {
		if (alleProfile().containsKey(id)) {
			daten.put("aktiv", id);
			merkeZuletzt(id);
			speichern();
		}
	}

	// ================================================================ Minecraft-Version je Profil

	/** Minecraft-Version eines Profils (ältere Profile ohne Eintrag: die Bambus-Version). */
	String version(String profilId) {
		Map<String, Object> p = Json.objekt(alleProfile().get(profilId));
		return p == null ? Installer.MC_VERSION : str(p.get("version"), Installer.MC_VERSION);
	}

	/** Version des Profils, mit dem gespielt wird. */
	String spielVersion() {
		return version(aktivesProfil());
	}

	/** Version des geöffneten Profils – danach richten sich Suche und Mod-Auswahl. */
	String offeneVersion() {
		return version(offenesProfil());
	}

	void setzeVersion(String profilId, String version) {
		Map<String, Object> p = Json.objekt(alleProfile().get(profilId));
		if (p != null && version != null && !version.isEmpty()) {
			p.put("version", version);
			speichern();
		}
	}

	/** Eingebaute Liste, falls Fabric gerade nicht erreichbar ist. */
	private static final String[] VERSIONEN_OFFLINE = {
			"1.21.11", "1.21.10", "1.21.9", "1.21.8", "1.21.7", "1.21.6", "1.21.5", "1.21.4", "1.21.3", "1.21.1", "1.21",
			"1.20.6", "1.20.4", "1.20.2", "1.20.1", "1.19.4", "1.19.2", "1.18.2", "1.17.1", "1.16.5"};
	private static volatile List<String> versionen;

	/**
	 * Alle Minecraft-Versionen, für die es Fabric gibt (nur Vollversionen, neueste zuerst, ab 1.14).
	 * Fragt beim ersten Aufruf meta.fabricmc.net – darf nicht im Swing-Thread laufen.
	 */
	static List<String> mcVersionen() {
		List<String> l = versionen;
		if (l != null) return l;
		l = new ArrayList<>();
		try {
			List<Object> alle = Json.liste(Json.parse(new String(Installer.lade("https://meta.fabricmc.net/v2/versions/game"),
					StandardCharsets.UTF_8)));
			if (alle != null) {
				for (Object o : alle) {
					Map<String, Object> v = Json.objekt(o);
					if (v == null || !Boolean.TRUE.equals(v.get("stable")) || !(v.get("version") instanceof String)) continue;
					String id = (String) v.get("version");
					if (id.matches("1\\.(1[4-9]|[2-9]\\d)(\\.\\d+)?")) l.add(id);
				}
			}
		} catch (Exception e) {
			l.clear();
		}
		if (l.isEmpty()) {
			return Collections.unmodifiableList(java.util.Arrays.asList(VERSIONEN_OFFLINE));   // später nochmal fragen
		}
		versionen = Collections.unmodifiableList(l);
		return versionen;
	}

	/** Neues Profil; kopieVon = Id eines Profils, dessen Mods übernommen werden (oder null für ein frisches). */
	String neuesProfil(String name, String kopieVon) {
		return neuesProfil(name, kopieVon, kopieVon == null ? Installer.MC_VERSION : version(kopieVon));
	}

	String neuesProfil(String name, String kopieVon, String version) {
		Map<String, Object> profile = alleProfile();
		String id;
		int nr = profile.size();
		do {
			id = "profil" + (++nr);
		} while (profile.containsKey(id));
		Map<String, Object> p = new LinkedHashMap<>();
		p.put("name", profilName(name));
		p.put("version", version == null || version.isEmpty() ? Installer.MC_VERSION : version);
		Map<String, Object> vorlage = kopieVon == null ? null : Json.objekt(profile.get(kopieVon));
		Map<String, Object> an = new LinkedHashMap<>(), extra = new LinkedHashMap<>();
		if (vorlage != null) {
			if (Json.objekt(vorlage.get("an")) != null) an.putAll(Json.objekt(vorlage.get("an")));
			Map<String, Object> alt = Json.objekt(vorlage.get("extra"));
			if (alt != null) {
				for (Map.Entry<String, Object> e : alt.entrySet()) {
					Map<String, Object> eintrag = Json.objekt(e.getValue());
					extra.put(e.getKey(), eintrag == null ? e.getValue() : new LinkedHashMap<>(eintrag));
				}
			}
		}
		p.put("an", an);
		p.put("extra", extra);
		p.put("erstellt", System.currentTimeMillis());
		profile.put(id, p);
		speichern();
		return id;
	}

	void benenneProfil(String id, String name) {
		Map<String, Object> p = Json.objekt(alleProfile().get(id));
		if (p != null) {
			p.put("name", profilName(name));
			speichern();
		}
	}

	/** Löscht ein Profil – das letzte bleibt immer bestehen. */
	boolean loescheProfil(String id) {
		Map<String, Object> profile = alleProfile();
		if (profile.size() <= 1 || !profile.containsKey(id)) {
			return false;
		}
		profile.remove(id);
		if (id.equals(offen)) {
			offen = null;
		}
		if (id.equals(daten.get("aktiv"))) {
			daten.put("aktiv", profile.keySet().iterator().next());
		}
		speichern();
		return true;
	}

	private static String profilName(String name) {
		String n = name == null ? "" : name.trim().replaceAll("\\s+", " ");
		if (n.isEmpty()) n = "Profil";
		return n.length() > 24 ? n.substring(0, 24) : n;
	}

	// ================================================================ CurseForge-Schlüssel

	String curseforgeSchluessel() {
		return str(daten.get("curseforgeSchluessel"), "").trim();
	}

	void setzeCurseforgeSchluessel(String schluessel) {
		daten.put("curseforgeSchluessel", schluessel == null ? "" : schluessel.trim());
		speichern();
	}

	boolean hatCurseforge() {
		return !curseforgeSchluessel().isEmpty();
	}

	synchronized void speichern() {
		try {
			Files.createDirectories(datei.getParent());
			Path tmp = datei.resolveSibling("bambus-mods.json.tmp");
			Files.write(tmp, Json.write(daten).getBytes(StandardCharsets.UTF_8));
			Files.move(tmp, datei, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			System.err.println("[Bambus] bambus-mods.json nicht speicherbar: " + e);
		}
	}

	/** „an“ und „extra“ gehören zum geöffneten Profil, alles andere (z. B. „installiert“) gilt für alle. */
	private Map<String, Object> map(String schluessel) {
		return map(offenesProfil(), schluessel);
	}

	private Map<String, Object> map(String profilId, String schluessel) {
		Map<String, Object> ort = daten;
		if ("an".equals(schluessel) || "extra".equals(schluessel)) {
			ort = Json.objekt(alleProfile().get(profilId));
		}
		Map<String, Object> m = Json.objekt(ort.get(schluessel));
		if (m == null) {
			m = new LinkedHashMap<>();
			ort.put(schluessel, m);
		}
		return m;
	}

	boolean istAn(String slug) {
		return istAn(offenesProfil(), slug);
	}

	private boolean istAn(String profilId, String slug) {
		Object v = map(profilId, "an").get(slug);
		if (v instanceof Boolean) {
			return (Boolean) v;
		}
		return istEmpfohlen(slug); // empfohlene Mods sind standardmäßig an
	}

	void setzeAn(String slug, boolean an) {
		map("an").put(slug, an);
		speichern();
	}

	/** FPS-Boost = alle Mods der Gruppe "FPS" gemeinsam. */
	boolean fpsBoost() {
		for (String[] m : EMPFOHLEN) {
			if ("FPS".equals(m[3]) && istAn(m[0])) {
				return true;
			}
		}
		return false;
	}

	void setzeFpsBoost(boolean an) {
		for (String[] m : EMPFOHLEN) {
			if ("FPS".equals(m[3])) {
				map("an").put(m[0], an);
			}
		}
		speichern();
	}

	static boolean istEmpfohlen(String slug) {
		for (String[] m : EMPFOHLEN) {
			if (m[0].equals(slug)) {
				return true;
			}
		}
		return false;
	}

	/** Zusätzlich gewählte Mods des aktiven Profils von Modrinth und CurseForge (nicht die empfohlenen). */
	List<Projekt> extras() {
		return extras(offenesProfil());
	}

	private List<Projekt> extras(String profilId) {
		List<Projekt> l = new ArrayList<>();
		for (Map.Entry<String, Object> e : map(profilId, "extra").entrySet()) {
			Map<String, Object> p = Json.objekt(e.getValue());
			if (p != null) {
				l.add(new Projekt(e.getKey(), str(p.get("titel"), e.getKey()), str(p.get("beschreibung"), ""), 0,
						str(p.get("icon"), null)));
			}
		}
		return l;
	}

	void fuegeHinzu(Projekt p) {
		Map<String, Object> eintrag = new LinkedHashMap<>();
		eintrag.put("titel", p.titel);
		eintrag.put("beschreibung", p.beschreibung);
		if (p.icon != null) eintrag.put("icon", p.icon);
		map("extra").put(p.slug, eintrag);
		map("an").put(p.slug, true);
		speichern();
	}

	void entferne(String slug) {
		map("extra").remove(slug);
		map("an").remove(slug);
		speichern();
	}

	boolean istExtra(String slug) {
		return map("extra").containsKey(slug);
	}

	// ================================================================ Eigene .jar-Dateien

	/** Dateinamen, die Bambus selbst verwaltet (nicht als "eigene Mod" anzeigen). */
	private Set<String> verwalteteDateien() {
		Set<String> s = new LinkedHashSet<>();
		for (Object v : map("installiert").values()) {
			if (v instanceof String) {
				s.add((String) v);
			}
		}
		return s;
	}

	List<EigeneMod> eigeneMods() {
		List<EigeneMod> l = new ArrayList<>();
		if (!Files.isDirectory(installer.modsOrdner)) {
			return l;
		}
		Set<String> verwaltet = verwalteteDateien();
		try (DirectoryStream<Path> ds = Files.newDirectoryStream(installer.modsOrdner)) {
			for (Path p : ds) {
				String n = p.getFileName().toString();
				boolean jar = n.endsWith(".jar"), aus = n.endsWith(".jar.disabled");
				if (!jar && !aus) continue;
				if (n.equals(Installer.MOD_DATEI) || n.startsWith("fabric-api-") || verwaltet.contains(n)) continue;
				l.add(new EigeneMod(p, jar));
			}
		} catch (IOException e) {
			return l;
		}
		l.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));
		return l;
	}

	void schalteEigene(EigeneMod m, boolean an) throws IOException {
		String n = m.datei.getFileName().toString();
		String neu = an ? n.replaceAll("\\.disabled$", "") : (n.endsWith(".disabled") ? n : n + ".disabled");
		if (!neu.equals(n)) {
			Files.move(m.datei, m.datei.resolveSibling(neu), StandardCopyOption.REPLACE_EXISTING);
		}
	}

	void loescheEigene(EigeneMod m) throws IOException {
		Files.deleteIfExists(m.datei);
	}

	void kopiereEigene(Path jar) throws IOException {
		Files.createDirectories(installer.modsOrdner);
		Files.copy(jar, installer.modsOrdner.resolve(jar.getFileName()), StandardCopyOption.REPLACE_EXISTING);
	}

	// ================================================================ Modrinth

	/** Sucht Fabric-Mods für die Minecraft-Version des Clients – bei Modrinth oder CurseForge. */
	List<Projekt> suche(String text, String quelle) throws IOException {
		return CURSEFORGE.equals(quelle) ? sucheCurseforge(text) : suche(text);
	}

	/** Modrinth-Suche; ohne Suchtext die beliebtesten Mods. */
	List<Projekt> suche(String text) throws IOException {
		String facets = "[[\"categories:fabric\"],[\"versions:" + offeneVersion() + "\"],[\"project_type:mod\"]]";
		String url = API + "search?limit=20&index=" + (text.isEmpty() ? "downloads" : "relevance")
				+ "&query=" + enc(text) + "&facets=" + enc(facets);
		Map<String, Object> antwort = Json.objekt(Json.parse(new String(Installer.lade(url), StandardCharsets.UTF_8)));
		List<Projekt> l = new ArrayList<>();
		if (antwort == null) return l;
		List<Object> treffer = Json.liste(antwort.get("hits"));
		if (treffer == null) return l;
		for (Object o : treffer) {
			Map<String, Object> h = Json.objekt(o);
			if (h == null) continue;
			Object dl = h.get("downloads");
			l.add(new Projekt(str(h.get("slug"), ""), str(h.get("title"), "?"), str(h.get("description"), ""),
					dl instanceof Number ? ((Number) dl).longValue() : 0, str(h.get("icon_url"), null)));
		}
		return l;
	}

	/**
	 * Icon-Adressen für Mods, deren Icon noch nicht bekannt ist (empfohlene Mods, ältere Einträge).
	 * Modrinth: eine Anfrage für alle; CurseForge: je Mod eine (nur mit Schlüssel). Fehler werden übersprungen.
	 * Ändert nichts – darf im Hintergrund laufen.
	 */
	Map<String, String> sucheIcons(List<String> schluessel) {
		Map<String, String> icons = new LinkedHashMap<>();
		List<String> modrinth = new ArrayList<>();
		for (String k : schluessel) {
			if (!k.startsWith(CF)) {
				modrinth.add(k);
				continue;
			}
			if (!hatCurseforge()) continue;
			try {
				Map<String, Object> mod = Json.objekt(Json.objekt(cf("mods/" + enc(k.substring(CF.length())))).get("data"));
				Map<String, Object> logo = mod == null ? null : Json.objekt(mod.get("logo"));
				String url = logo == null ? null : str(logo.get("thumbnailUrl"), null);
				if (url != null) icons.put(k, url);
			} catch (Exception ignoriert) {
				// ohne Icon weiter
			}
		}
		if (!modrinth.isEmpty()) {
			try {
				StringBuilder ids = new StringBuilder("[");
				for (String k : modrinth) ids.append(ids.length() > 1 ? "," : "").append('"').append(k).append('"');
				ids.append(']');
				List<Object> projekte = Json.liste(Json.parse(new String(Installer.lade(API + "projects?ids=" + enc(ids.toString())),
						StandardCharsets.UTF_8)));
				if (projekte != null) {
					for (Object o : projekte) {
						Map<String, Object> p = Json.objekt(o);
						if (p == null || !(p.get("icon_url") instanceof String)) continue;
						String url = (String) p.get("icon_url");
						// Modrinth antwortet mit Slug und Id – beide Schreibweisen zuordnen
						for (String k : modrinth) {
							if (k.equals(p.get("slug")) || k.equals(p.get("id"))) icons.put(k, url);
						}
					}
				}
			} catch (Exception ignoriert) {
				// ohne Icons weiter
			}
		}
		return icons;
	}

	/** Gefundene Icons zu den gewählten Mods merken, damit sie beim nächsten Start sofort da sind (Swing-Thread). */
	void merkeIcons(Map<String, String> icons) {
		boolean geaendert = false;
		for (Map.Entry<String, String> e : icons.entrySet()) {
			Map<String, Object> eintrag = Json.objekt(map("extra").get(e.getKey()));
			if (eintrag != null && !e.getValue().equals(eintrag.get("icon"))) {
				eintrag.put("icon", e.getValue());
				geaendert = true;
			}
		}
		if (geaendert) speichern();
	}

	// ================================================================ CurseForge

	private Object cf(String pfad) throws IOException {
		String schluessel = curseforgeSchluessel();
		if (schluessel.isEmpty()) throw new IOException("CurseForge-Schlüssel fehlt");
		return Json.parse(new String(Installer.lade(CF_API + pfad, "x-api-key", schluessel, "Accept", "application/json"),
				StandardCharsets.UTF_8));
	}

	/** CurseForge-Suche (Fabric-Mods für die Minecraft-Version); ohne Suchtext die beliebtesten. */
	private List<Projekt> sucheCurseforge(String text) throws IOException {
		Map<String, Object> antwort = Json.objekt(cf("mods/search?gameId=432&classId=6&modLoaderType=4&pageSize=20"
				+ "&sortField=" + (text.isEmpty() ? "6" : "1") + "&sortOrder=desc"
				+ "&gameVersion=" + enc(offeneVersion()) + "&searchFilter=" + enc(text)));
		List<Projekt> l = new ArrayList<>();
		List<Object> treffer = antwort == null ? null : Json.liste(antwort.get("data"));
		if (treffer == null) return l;
		for (Object o : treffer) {
			Map<String, Object> h = Json.objekt(o);
			if (h == null || !(h.get("id") instanceof Number)) continue;
			Object dl = h.get("downloadCount");
			Map<String, Object> logo = Json.objekt(h.get("logo"));
			l.add(new Projekt(CF + ((Number) h.get("id")).longValue(), str(h.get("name"), "?"), str(h.get("summary"), ""),
					dl instanceof Number ? ((Number) dl).longValue() : 0, logo == null ? null : str(logo.get("thumbnailUrl"), null)));
		}
		return l;
	}

	/** Neueste stabile Datei (sonst neueste überhaupt) eines CurseForge-Projekts für Fabric + Minecraft-Version. */
	private Map<String, Object> passendeCurseforgeDatei(String nummer, String mc) throws IOException {
		Map<String, Object> antwort = Json.objekt(cf("mods/" + enc(nummer) + "/files?modLoaderType=4&pageSize=20&gameVersion="
				+ enc(mc)));
		List<Object> dateien = antwort == null ? null : Json.liste(antwort.get("data"));
		if (dateien == null || dateien.isEmpty()) return null;
		for (Object o : dateien) {
			Map<String, Object> d = Json.objekt(o);
			if (d != null && d.get("releaseType") instanceof Number && ((Number) d.get("releaseType")).intValue() == 1) return d;
		}
		return Json.objekt(dateien.get(0));
	}

	/** Lädt eine CurseForge-Mod; gibt den Dateinamen zurück und hängt benötigte Abhängigkeiten an die Warteschlange. */
	private String installiereCurseforge(String schluessel, String mc, List<String> warteschlange, List<String> hinweise)
			throws IOException {
		String nummer = schluessel.substring(CF.length());
		String titel = titel(schluessel);
		Map<String, Object> datei = passendeCurseforgeDatei(nummer, mc);
		if (datei == null) {
			hinweise.add(titel + " gibt es nicht für Minecraft " + mc);
			return null;
		}
		String name = str(datei.get("fileName"), "curseforge-" + nummer + ".jar");
		String adresse = str(datei.get("downloadUrl"), "");
		Path ziel = installer.modsOrdner.resolve(name);
		if (!Files.isRegularFile(ziel)) {
			if (adresse.isEmpty()) {
				// Der Autor erlaubt keine Downloads durch andere Launcher
				hinweise.add(titel + ": nur direkt bei curseforge.com ladbar – dort laden und mit „+ MOD-DATEI“ hinzufügen");
				return null;
			}
			Path tmp = installer.modsOrdner.resolve(name + ".download");
			Files.write(tmp, Installer.lade(adresse));
			Files.move(tmp, ziel, StandardCopyOption.REPLACE_EXISTING);
		}
		List<Object> abh = Json.liste(datei.get("dependencies"));
		if (abh != null) {
			for (Object o : abh) {
				Map<String, Object> a = Json.objekt(o);
				if (a == null || !(a.get("relationType") instanceof Number) || ((Number) a.get("relationType")).intValue() != 3) continue;
				Object id = a.get("modId");
				if (id instanceof Number && !CF_FABRIC_API.equals(String.valueOf(((Number) id).longValue()))) {
					warteschlange.add(CF + ((Number) id).longValue());
				}
			}
		}
		return name;
	}

	/** Anzeigename einer gewählten Mod (für Hinweise). */
	private String titel(String schluessel) {
		Map<String, Object> p = Json.objekt(map(aktivesProfil(), "extra").get(schluessel));
		return p == null ? schluessel : str(p.get("titel"), schluessel);
	}

	// ================================================================ Einrichten beim SPIELEN

	/**
	 * Bringt den Mods-Ordner auf den gewünschten Stand: lädt eingeschaltete Mods (samt benötigter
	 * Abhängigkeiten) passend zur Minecraft-Version des aktiven Profils, entfernt abgeschaltete.
	 * Einzelne Fehler stoppen den Start nicht – sie werden als Hinweise zurückgegeben.
	 */
	List<String> installiere(Installer.Fortschritt f, int von, int bis) {
		List<String> hinweise = new ArrayList<>();
		List<String> gewuenscht = new ArrayList<>();
		String aktiv = aktivesProfil();   // gespielt wird immer das aktive Profil, egal welches gerade offen ist
		String mc = version(aktiv);
		for (String[] m : EMPFOHLEN) if (istAn(aktiv, m[0])) gewuenscht.add(m[0]);
		for (Projekt p : extras(aktiv)) if (istAn(aktiv, p.slug)) gewuenscht.add(p.slug);
		merkeZuletzt(aktiv);

		Map<String, Object> installiert = map("installiert");
		if (!mc.equals(str(daten.get("installiertFuer"), Installer.MC_VERSION))) {
			// andere Minecraft-Version als beim letzten Mal: alle von Bambus geladenen Mods neu holen
			for (Object alt : installiert.values()) {
				if (alt instanceof String) {
					try {
						Files.deleteIfExists(installer.modsOrdner.resolve((String) alt));
					} catch (IOException ignoriert) {
						// wird beim Laden überschrieben
					}
				}
			}
			installiert.clear();
		}
		daten.put("installiertFuer", mc);
		Map<String, String> neu = new LinkedHashMap<>();
		Set<String> gesehen = new LinkedHashSet<>();
		List<String> warteschlange = new ArrayList<>(gewuenscht);
		int nr = 0;
		while (!warteschlange.isEmpty()) {
			String projekt = warteschlange.remove(0);
			if (!gesehen.add(projekt)) continue;
			nr++;
			f.melde("Mods: " + titel(projekt) + " …", von + (bis - von) * Math.min(nr, 10) / 10);
			try {
				if (projekt.startsWith(CF)) {
					String name = installiereCurseforge(projekt, mc, warteschlange, hinweise);
					if (name != null) neu.put(projekt, name);
					continue;
				}
				Map<String, Object> version = passendeVersion(projekt, mc);
				if (version == null) {
					hinweise.add(titel(projekt) + " gibt es nicht für Minecraft " + mc);
					continue;
				}
				String slug = projekt;
				Map<String, Object> datei = primaerDatei(version);
				String name = str(datei.get("filename"), projekt + ".jar");
				Path ziel = installer.modsOrdner.resolve(name);
				if (!Files.isRegularFile(ziel)) {
					Path tmp = installer.modsOrdner.resolve(name + ".download");
					Files.write(tmp, Installer.lade(str(datei.get("url"), "")));
					Files.move(tmp, ziel, StandardCopyOption.REPLACE_EXISTING);
				}
				neu.put(slug, name);
				// benötigte Abhängigkeiten mitnehmen (Fabric API verwaltet der Installer selbst)
				List<Object> abh = Json.liste(version.get("dependencies"));
				if (abh != null) {
					for (Object o : abh) {
						Map<String, Object> a = Json.objekt(o);
						if (a == null || !"required".equals(a.get("dependency_type"))) continue;
						Object id = a.get("project_id");
						if (id instanceof String && !FABRIC_API_ID.equals(id)) warteschlange.add((String) id);
					}
				}
			} catch (Exception e) {
				hinweise.add(titel(projekt) + ": " + e.getMessage());
				// alte Datei behalten, wenn der Download gerade nicht klappt
				Object alt = installiert.get(projekt);
				if (alt instanceof String) neu.put(projekt, (String) alt);
			}
		}
		// Was Bambus früher installiert hat und jetzt nicht mehr gebraucht wird: löschen
		for (Map.Entry<String, Object> e : installiert.entrySet()) {
			Object alt = e.getValue();
			if (alt instanceof String && !neu.containsValue(alt)) {
				try {
					Files.deleteIfExists(installer.modsOrdner.resolve((String) alt));
				} catch (IOException ignoriert) {
					// nächster Start versucht es erneut
				}
			}
		}
		installiert.clear();
		installiert.putAll(neu);
		speichern();
		return hinweise;
	}

	/** Neueste stabile Version (sonst neueste Beta) für Fabric + Minecraft-Version. */
	private Map<String, Object> passendeVersion(String projekt, String mc) throws IOException {
		String url = API + "project/" + enc(projekt) + "/version?loaders=" + enc("[\"fabric\"]")
				+ "&game_versions=" + enc("[\"" + mc + "\"]");
		List<Object> versionen = Json.liste(Json.parse(new String(Installer.lade(url), StandardCharsets.UTF_8)));
		if (versionen == null || versionen.isEmpty()) return null;
		for (Object o : versionen) {
			Map<String, Object> v = Json.objekt(o);
			if (v != null && "release".equals(v.get("version_type"))) return v;
		}
		return Json.objekt(versionen.get(0));
	}

	private static Map<String, Object> primaerDatei(Map<String, Object> version) throws IOException {
		List<Object> dateien = Json.liste(version.get("files"));
		if (dateien == null || dateien.isEmpty()) throw new IOException("keine Datei");
		for (Object o : dateien) {
			Map<String, Object> d = Json.objekt(o);
			if (d != null && Boolean.TRUE.equals(d.get("primary"))) return d;
		}
		return Json.objekt(dateien.get(0));
	}

	private static String enc(String s) {
		try {
			return URLEncoder.encode(s, "UTF-8").replace("+", "%20");
		} catch (Exception e) {
			return s;
		}
	}

	private static String str(Object o, String standard) {
		return o instanceof String ? (String) o : standard;
	}

	static List<String[]> fpsMods() {
		List<String[]> l = new ArrayList<>();
		for (String[] m : EMPFOHLEN) if ("FPS".equals(m[3])) l.add(m);
		return Collections.unmodifiableList(l);
	}
}
