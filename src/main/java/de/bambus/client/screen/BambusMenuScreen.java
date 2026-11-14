package de.bambus.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import de.bambus.client.Anzeigen;
import de.bambus.client.DiscordStatus;
import de.bambus.client.Profile;
import de.bambus.client.Statistik;
import de.bambus.client.Einstellbar;
import de.bambus.client.Einstellung;
import de.bambus.client.Texturen;
import de.bambus.client.Visuals;
import de.bambus.client.BambusClient;
import de.bambus.client.BambusConfig;
import de.bambus.client.Fadenkreuz;
import de.bambus.client.Farben;
import de.bambus.client.Funktionen;
import de.bambus.client.hud.HudModul;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

/**
 * Das Bambus-Menü im Neon-Look (Rechts-Shift).
 * Tabs: Module · Funktionen · Fadenkreuz · Mehr – mit der TAB-Taste wechseln.
 * Das Textur-Modul ist eine Karte unter MODULE; Einstellungen jeder Modul-Karte über ⋮ oder Rechtsklick.
 * Kosmetik gibt es in der Garderobe (GarderobeScreen).
 */
public class BambusMenuScreen extends Screen {

	/** Bereiche des Menüs – links in der Seitenleiste. */
	private enum Tab {
		MODULE("Module", "HUD & Texturen", "alle"),
		FUNKTIONEN("Funktionen", "Zoom & Visuals", "blitz"),
		FADENKREUZ("Fadenkreuz", "Form & Farbe", "fadenkreuz"),
		MEHR("Mehr", "Design & Extras", "mehr");

		final String titel;
		final String untertitel;
		final String symbol;

		Tab(String titel, String untertitel, String symbol) {
			this.titel = titel;
			this.untertitel = untertitel;
			this.symbol = symbol;
		}
	}

	/** Eine Schalter-Karte; fav = Modul-Id (Stern), einstellungen = öffnet das ⋮-Fenster (oder null). */
	private record Karte(String name, String text, BooleanSupplier an, Runnable umschalten, int akzent, String fav,
						 Runnable einstellungen) {
		Karte(String name, String text, BooleanSupplier an, Runnable umschalten, int akzent) {
			this(name, text, an, umschalten, akzent, null, null);
		}
	}

	private static final String[] KATEGORIEN = {"ALLE", "PVP", "HUD", "INFO", "★"};
	private static String kategorie = "ALLE";
	private EditBox suchFeld;
	private EditBox freundFeld;
	private String meldung = "";
	private long meldungZeit;
	private final long offenSeit = System.currentTimeMillis();
	private long tabSeit = 0;        // für den weichen Inhaltswechsel
	private long schliessSeit = 0;   // für die Schließ-Animation

	/** Klickfläche; rechts = Aktion für Rechtsklick (z. B. Einstellungen einer Modul-Karte) oder null. */
	private record Bereich(int x, int y, int b, int h, Runnable aktion, boolean imInhalt, Runnable rechts) {}

	private static final Map<String, String> MODUL_TEXT = Map.of(
			"fps", "Bilder pro Sekunde",
			"tasten", "W A S D, Maus und Leertaste",
			"server", "Adresse des Servers",
			"ping", "Ping mit Signalbalken",
			"ruestung", "Rüstung mit Haltbarkeit",
			"effekte", "Aktive Tränke mit Restzeit"
	);

	private static Tab letzterTab = Tab.MODULE;

	private final Screen eltern;
	private final List<Bereich> bereiche = new ArrayList<>();

	private Tab tab = letzterTab;
	/** Modul, dessen Einstellungsfenster (⋮ / Rechtsklick) gerade offen ist – oder null. */
	private Einstellbar einstellungsZiel;
	private int einstellungsScroll;
	private int[] einstellungsListe = new int[4];   // Bereich der Schalterliste {x, y, b, h} fürs Mausrad
	private int scroll = 0;
	private int inhaltHoehe = 0;

	// Layout (wird in render berechnet)
	private int px, py, pb, ph;
	private int leiste;   // Breite der Seitenleiste
	private int ix, iy, ib, ih;

	public BambusMenuScreen(Screen eltern) {
		super(Component.literal("Bambus Client"));
		this.eltern = eltern;
	}

	@Override
	protected void init() {
		suchFeld = new EditBox(font, 0, 0, 120, 14, Component.literal("Suche"));
		suchFeld.setHint(Component.literal("Suchen …"));
		suchFeld.setMaxLength(32);
		suchFeld.setResponder(t -> scroll = 0);
		addRenderableWidget(suchFeld);
		freundFeld = new EditBox(font, 0, 0, 120, 14, Component.literal("Freund"));
		freundFeld.setHint(Component.literal("Spielername …"));
		freundFeld.setMaxLength(16);
		freundFeld.setVisible(false);
		addRenderableWidget(freundFeld);
	}

	private void melde(String text) {
		meldung = text;
		meldungZeit = System.currentTimeMillis();
		de.bambus.client.Hinweise.zeige("mehr", "Bambus", text, Farben.NEON_CYAN);
	}

	private static BambusConfig cfg() {
		return BambusClient.config;
	}

	private void speichern() {
		cfg().speichern();
	}

	private void oeffneEinstellungen(Einstellbar ziel) {
		einstellungsZiel = ziel;
		einstellungsScroll = 0;
	}

	/**
	 * Textur-Einstellungen in einer Welt: Menü und Abdunkeln verschwinden, das Fenster rückt an den Rand –
	 * so siehst du jede Änderung sofort an deiner echten Hand im Spiel.
	 */
	private boolean liveAnsicht() {
		return einstellungsZiel == Texturen.MODUL && minecraft.level != null && minecraft.player != null;
	}

	private void wechsle(Tab neu) {
		einstellungsZiel = null;
		if (tab != neu) {
			tab = neu;
			letzterTab = neu;
			scroll = 0;
			tabSeit = System.currentTimeMillis();
		}
	}

	// =================================================================== Zeichnen

	@Override
	public void renderBackground(GuiGraphics g, int mausX, int mausY, float delta) {
		if (liveAnsicht()) {
			return;   // freie Sicht aufs Spiel
		}
		MenueStil.anwenden();
		g.fill(0, 0, width, height, MenueStil.bambus() ? 0xC0020407 : Farben.HINTERGRUND);
		Neon.gitter(g, 0, 0, width, height, 24, Farben.alpha(Farben.NEON, 7));
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float delta) {
		bereiche.clear();
		MenueStil.anwenden();
		if (liveAnsicht()) {
			suchFeld.setVisible(false);
			freundFeld.setVisible(false);
			liveFenster(g, mx, my, einstellungsZiel);
			return;
		}
		int mausX = mx, mausY = my;
		if (einstellungsZiel != null) {   // darunter liegt alles still, solange das Einstellungsfenster offen ist
			mx = -1;
			my = -1;
		}

		pb = Math.min(width - 20, 540);
		ph = Math.min(height - 20, 310);
		px = (width - pb) / 2;
		py = (height - ph) / 2;
		float sichtbar = 1f;
		if (cfg().menueAnimation) {
			float k = Math.min(1f, (System.currentTimeMillis() - offenSeit) / Anim.OEFFNEN_MS);
			py += (int) ((1 - Anim.aus(k)) * 14);   // weich von unten einschweben
			sichtbar = Anim.aus(k);
			if (schliessSeit > 0) {
				float z = (System.currentTimeMillis() - schliessSeit) / Anim.SCHLIESSEN_MS;
				if (z >= 1f) {
					speichern();
					minecraft.setScreen(eltern);
					return;
				}
				py += (int) (Anim.aus(z) * 10);
				sichtbar = Math.min(sichtbar, 1 - Anim.aus(z));
			}
		}
		leiste = pb < 460 ? 92 : 112;
		ix = px + leiste + 10;
		iy = py + 34;
		ib = pb - leiste - 20;
		ih = ph - 34 - 32;
		boolean suchleiste = tab == Tab.MODULE;
		suchFeld.setVisible(suchleiste);
		if (tab != Tab.MEHR) {
			freundFeld.setVisible(false);
		}
		if (suchleiste) {
			iy += 22;
			ih -= 22;
		}

		// Grundfläche je nach Menü-Stil
		if (MenueStil.clean()) {          // weich gerundet, feiner heller Rand
			Neon.flaeche(g, px, py, pb, ph, Farben.PANEL);
			Neon.rahmen(g, px, py, pb, ph, 0x2EFFFFFF);
		} else if (MenueStil.kompakt()) { // kantig, Akzentleiste oben
			g.fill(px, py, px + pb, py + ph, Farben.PANEL);
			g.renderOutline(px, py, pb, ph, Farben.LINIE);
			g.fill(px, py, px + pb, py + 2, Farben.NEON);
		} else {                          // Bambus: ruhiger Neon-Rahmen, Ecken als Schmuck
			g.fill(px, py, px + pb, py + ph, Farben.PANEL);
			g.renderOutline(px - 1, py - 1, pb + 2, ph + 2, Farben.alpha(Farben.NEON, 28));
			g.renderOutline(px, py, pb, ph, Farben.alpha(Farben.NEON, 110));
			Neon.ecken(g, px - 5, py - 5, pb + 10, ph + 10, 12, Farben.alpha(Farben.NEON_CYAN, 170));
		}

		seitenleiste(g, mx, my);
		kopfzeile(g, mx, my);
		if (suchleiste) {
			suchleiste(g, mx, my);
		}

		// Inhalt (scrollbar)
		g.enableScissor(ix, iy, ix + ib, iy + ih);
		float wechsel = cfg().menueAnimation ? Anim.aus(Math.min(1f, (System.currentTimeMillis() - tabSeit) / Anim.WECHSEL_MS)) : 1f;
		int y0 = iy - scroll + (int) ((1 - wechsel) * 10);
		inhaltHoehe = switch (tab) {
			case MODULE -> karten(g, mx, my, moduleKarten(), ix, y0, ib);
			case FUNKTIONEN -> funktionen(g, mx, my, ix, y0, ib);
			case FADENKREUZ -> fadenkreuz(g, mx, my, ix, y0, ib);
			case MEHR -> mehr(g, mx, my, ix, y0, ib);
		};
		if (wechsel < 1f) {
			g.fill(ix, iy, ix + ib, iy + ih, Farben.alpha(Farben.PANEL, (int) ((1 - wechsel) * 230)));
		}
		g.disableScissor();

		int maxScroll = Math.max(0, inhaltHoehe - ih);
		scroll = Math.max(0, Math.min(scroll, maxScroll));
		if (maxScroll > 0) {
			int balkenH = Math.max(12, ih * ih / inhaltHoehe);
			int balkenY = iy + (ih - balkenH) * scroll / maxScroll;
			g.fill(px + pb - 5, iy, px + pb - 3, iy + ih, Farben.LINIE);
			g.fill(px + pb - 5, balkenY, px + pb - 3, balkenY + balkenH, Farben.NEON);
		}

		fuss(g, mx, my);
		super.render(g, mx, my, delta);
		if (einstellungsZiel != null) {
			bereiche.clear();   // nur das Fenster ist klickbar
			einstellungsFenster(g, mausX, mausY, einstellungsZiel);
		}
		if (sichtbar < 1f) {   // ein- und ausblenden
			g.fill(px - 6, py - 6, px + pb + 6, py + ph + 6, Farben.alpha(0xFF020407, (int) ((1 - sichtbar) * 255)));
		}
	}

	/** Kopf rechts: Titel der Seite und Schließen. */
	private void kopfzeile(GuiGraphics g, int mx, int my) {
		int x = px + leiste + 10;
		g.drawString(font, tab.titel, x, py + 11, Farben.WEISS, false);
		Neon.text(g, font, tab.untertitel, x + font.width(tab.titel) + 6, py + 12, 0.75f, Farben.GRAU, false);

		int sx = px + pb - 20;
		int sy = py + 8;
		boolean hover = Neon.ueber(mx, my, sx, sy, 12, 12);
		g.drawString(font, "✕", sx + 2, sy + 2, hover ? Farben.ROT : Farben.GRAU, false);
		klick(sx, sy, 12, 12, this::onClose, false);

		g.fill(x, py + 28, px + pb - 8, py + 29, Farben.LINIE);
	}


	/** Schlanke Seitenleiste links: Logo, die Bereiche mit gleitender Markierung, unten Tab-Hinweis und Version. */
	private void seitenleiste(GuiGraphics g, int mx, int my) {
		int x = px, b = leiste;
		boolean weich = cfg().menueAnimation;
		if (MenueStil.clean()) {
			Neon.flaeche(g, x, py, b, ph, 0x0AFFFFFF);
			g.fill(x + b - 1, py + 8, x + b, py + ph - 8, Farben.LINIE);
		} else if (MenueStil.kompakt()) {
			g.fill(x, py + 2, x + b, py + ph, 0x66000000);
			g.fill(x + b - 1, py + 2, x + b, py + ph, Farben.LINIE);
		} else {
			g.fill(x, py, x + b, py + ph, 0x50000000);
			g.fill(x + b - 1, py + 6, x + b, py + ph - 6, Farben.alpha(Farben.NEON, 70));
		}

		Neon.logo(g, x + 10, py + 10, 16);
		Neon.leuchtText(g, font, "BAMBUS", x + 28, py + 10, 1f, Farben.NEON);
		Neon.text(g, font, "CLIENT", x + 28, py + 21, 0.6f, Farben.NEON_CYAN, false);
		g.fill(x + 8, py + 33, x + b - 8, py + 34, Farben.LINIE);

		Tab[] alle = Tab.values();
		int y0 = py + 40, eh = 28, abstand = 2;
		float ziel = tab.ordinal() * (eh + abstand);
		int markY = y0 + (weich ? Math.round(Anim.zu("menue-markierung", ziel, Anim.GLEITEN)) : (int) ziel);
		if (MenueStil.clean()) {           // ruhige helle Fläche statt Leiste
			Neon.flaeche(g, x + 4, markY, b - 9, eh, 0x1AFFFFFF);
		} else if (MenueStil.kompakt()) {
			g.fill(x + 4, markY, x + b - 5, markY + eh, Farben.alpha(Farben.NEON, 34));
			g.fill(x + 4, markY, x + 7, markY + eh, Farben.NEON);
		} else {
			g.fill(x + 4, markY, x + b - 5, markY + eh, Farben.alpha(Farben.NEON, 26));
			g.fill(x + 4, markY, x + 6, markY + eh, Farben.NEON);
		}

		for (int i = 0; i < alle.length; i++) {
			Tab t = alle[i];
			int y = y0 + i * (eh + abstand);
			boolean aktiv = t == tab;
			boolean hover = Neon.ueber(mx, my, x + 4, y, b - 9, eh);
			float h = weich ? Anim.zu("menue-hover:" + t, hover ? 1f : 0f, Anim.HOVER) : (hover ? 1f : 0f);
			if (!aktiv && h > 0.01f) {
				g.fill(x + 4, y, x + b - 5, y + eh, Farben.alpha(Farben.KARTE_HOVER, (int) (255 * h)));
			}
			int farbe = aktiv ? Farben.NEON : Anim.mische(Farben.GRAU, Farben.WEISS, h);
			Symbole.zeichne(g, t.symbol, x + 12, y + 6, 1, farbe);
			g.drawString(font, t.titel, x + 26, y + 5, aktiv ? Farben.WEISS : 0xFFC8D2DE, false);
			String unter = font.plainSubstrByWidth(t.untertitel, (int) ((b - 32) / 0.6f));
			Neon.text(g, font, unter, x + 26, y + 16, 0.6f, aktiv ? Farben.NEON : Farben.GRAU, false);
			klick(x + 4, y, b - 9, eh, () -> wechsle(t), false);
		}

		Neon.text(g, font, "[TAB] wechseln", x + 10, py + ph - 26, 0.6f, Farben.GRAU, false);
		Neon.text(g, font, "v" + BambusClient.VERSION + " · 1.21.11", x + 10, py + ph - 16, 0.6f, 0xFF4A5A6E, false);
	}

	/** Fuß rechts unter dem Inhalt: HUD BEARBEITEN und FERTIG. */
	private void fuss(GuiGraphics g, int mx, int my) {
		int y = py + ph - 26;
		g.fill(ix, y - 5, px + pb - 8, y - 4, Farben.LINIE);

		boolean imSpiel = minecraft.player != null;
		knopf(g, mx, my, "HUD BEARBEITEN", ix, y, 100, 18, false, imSpiel,
				() -> minecraft.setScreen(new HudEditorScreen(this)));
		knopf(g, mx, my, "FERTIG", px + pb - 80, y, 70, 18, true, true, this::onClose);
	}

	// ------------------------------------------------------------- Karten

	private List<Karte> moduleKarten() {
		BambusConfig c = cfg();
		List<Karte> k = new ArrayList<>();
		k.add(new Karte("HUD (alles)", "Alle Anzeigen ein/aus · Taste H",
				() -> c.hudAn, () -> c.hudAn = !c.hudAn, Farben.NEON_CYAN));
		String suche = suchFeld == null ? "" : suchFeld.getValue().trim().toLowerCase(java.util.Locale.ROOT);
		List<HudModul> liste = new ArrayList<>(BambusClient.MODULE);
		// Favoriten zuerst
		liste.sort((a, b2) -> Boolean.compare(!c.favoriten.contains(a.id), !c.favoriten.contains(b2.id)));
		for (HudModul m : liste) {
			String text = m.beschreibung.isEmpty() ? MODUL_TEXT.getOrDefault(m.id, "") : m.beschreibung;
			boolean passt = suche.isEmpty() || m.name.toLowerCase(java.util.Locale.ROOT).contains(suche)
					|| text.toLowerCase(java.util.Locale.ROOT).contains(suche);
			boolean kat = kategorie.equals("ALLE") || kategorie.equals(m.kategorie)
					|| (kategorie.equals("★") && c.favoriten.contains(m.id));
			if (passt && kat) {
				k.add(new Karte(m.name, text, () -> m.pos().an, () -> m.pos().an = !m.pos().an,
						m.kategorie.equals("PVP") ? Farben.NEON_PINK : m.kategorie.equals("INFO") ? Farben.NEON_CYAN : Farben.NEON, m.id,
						() -> oeffneEinstellungen(m)));
			}
		}
		texturKarte(k, suche, c);
		if (!suche.isEmpty() || !kategorie.equals("ALLE")) {
			k.remove(0); // "HUD (alles)" nur in der vollen Liste
		}
		return k;
	}

	/** Das Textur-Modul als Karte (Kategorie PVP) – als Favorit oben, sonst am Ende der Liste. */
	private void texturKarte(List<Karte> k, String suche, BambusConfig c) {
		String id = "texturen", name = "Texturen", text = "Low Fire, Low Shield, Short Sword …";
		boolean passt = suche.isEmpty() || name.toLowerCase(java.util.Locale.ROOT).contains(suche)
				|| text.toLowerCase(java.util.Locale.ROOT).contains(suche)
				|| "low fire shield side short sword kürbis wasser".contains(suche);
		boolean fav = c.favoriten.contains(id);
		boolean kat = kategorie.equals("ALLE") || kategorie.equals("PVP") || (kategorie.equals("★") && fav);
		if (!passt || !kat) {
			return;
		}
		Karte karte = new Karte(name, text, () -> c.texturenAn, () -> c.texturenAn = !c.texturenAn, Farben.NEON_PINK, id,
				() -> oeffneEinstellungen(Texturen.MODUL));
		int favoriten = 0;
		for (Karte x : k) {
			if (x.fav() != null && c.favoriten.contains(x.fav())) favoriten++;
		}
		boolean hudAlles = !k.isEmpty() && k.get(0).fav() == null;
		k.add(fav ? favoriten + (hudAlles ? 1 : 0) : k.size(), karte);
	}

	private List<Karte> funktionsKarten() {
		BambusConfig c = cfg();
		List<Karte> k = new ArrayList<>();
		k.add(new Karte("Zoom mit Mausrad", "C halten · Mausrad = näher/weiter",
				() -> c.scrollZoom, () -> c.scrollZoom = !c.scrollZoom, Farben.NEON));
		k.add(new Karte("Freelook", "Linke Alt halten · ⚠ nicht überall erlaubt",
				() -> c.freelook, () -> c.freelook = !c.freelook, Farben.NEON_PINK));
		k.add(new Karte("Fullbright", "Alles hell · ⚠ nicht überall erlaubt",
				() -> c.fullbright, () -> c.fullbright = !c.fullbright, Farben.NEON_PINK));
		k.add(new Karte("Gegner-Leben als Zahl", "Target-HUD zeigt genaue ❤ · ⚠ nicht überall erlaubt",
				() -> c.gegnerLebenZahl, () -> c.gegnerLebenZahl = !c.gegnerLebenZahl, Farben.NEON_PINK));
		k.add(new Karte("Bambus-Hauptmenü", "Neon-Startbildschirm statt Minecraft-Menü",
				() -> c.bambusHauptmenue, () -> c.bambusHauptmenue = !c.bambusHauptmenue, Farben.NEON_CYAN));
		return k;
	}

	/** Funktionen + Anzeigen anpassen (Scoreboard, Bossbar, Tabliste, Chat). */
	private int funktionen(GuiGraphics g, int mx, int my, int x, int y, int b) {
		BambusConfig c = cfg();
		int h = karten(g, mx, my, funktionsKarten(), x, y, b);
		int zy = y + h + 6;
		Neon.text(g, font, "VISUALS & LEISTUNG", x, zy, 0.75f, Farben.NEON, false);
		zy += 12;
		List<Karte> vis = List.of(
				new Karte("Treffer-Partikel", "Extra Funken, wenn du triffst", () -> c.trefferPartikel, () -> c.trefferPartikel = !c.trefferPartikel, Farben.NEON_CYAN),
				new Karte("Low-End-Modus", "Sparsame Grafik für langsame PCs", () -> c.lowEnd, () -> Visuals.lowEnd(minecraft, !c.lowEnd), Farben.NEON));
		zy += karten(g, mx, my, vis, x, zy, b, 2);
		int hb = (b - 6) / 2;
		auswahl(g, mx, my, "ZEIT", Visuals.ZEITEN[Math.floorMod(c.zeitModus, Visuals.ZEITEN.length)], x, zy, hb,
				() -> c.zeitModus = Math.floorMod(c.zeitModus - 1, Visuals.ZEITEN.length),
				() -> c.zeitModus = Math.floorMod(c.zeitModus + 1, Visuals.ZEITEN.length));
		auswahl(g, mx, my, "WETTER", Visuals.WETTER[Math.floorMod(c.wetterModus, Visuals.WETTER.length)], x + hb + 6, zy, hb,
				() -> c.wetterModus = Math.floorMod(c.wetterModus - 1, Visuals.WETTER.length),
				() -> c.wetterModus = Math.floorMod(c.wetterModus + 1, Visuals.WETTER.length));
		zy += 28;
		Neon.text(g, font, "ANZEIGEN ANPASSEN", x, zy, 0.75f, Farben.NEON, false);
		zy += 12;
		List<Karte> an = List.of(
				new Karte("Scoreboard", "Rechte Seitenleiste ein/aus", () -> c.scoreboardAn, () -> c.scoreboardAn = !c.scoreboardAn, Farben.NEON_CYAN),
				new Karte("Bossbar", "Bossleiste oben ein/aus", () -> c.bossbarAn, () -> c.bossbarAn = !c.bossbarAn, Farben.NEON_CYAN));
		zy += karten(g, mx, my, an, x, zy, b, 2);
		int sb = (b - 6) / 2;
		auswahl(g, mx, my, "SCORE", c.scoreboardGroesse + " %", x, zy, sb,
				() -> c.scoreboardGroesse = Anzeigen.naechste(c.scoreboardGroesse, -1),
				() -> c.scoreboardGroesse = Anzeigen.naechste(c.scoreboardGroesse, 1));
		auswahl(g, mx, my, "BOSS", c.bossbarGroesse + " %", x + sb + 6, zy, sb,
				() -> c.bossbarGroesse = Anzeigen.naechste(c.bossbarGroesse, -1),
				() -> c.bossbarGroesse = Anzeigen.naechste(c.bossbarGroesse, 1));
		zy += 22;
		auswahl(g, mx, my, "TAB", c.tablisteGroesse + " %", x, zy, sb,
				() -> c.tablisteGroesse = Anzeigen.naechste(c.tablisteGroesse, -1),
				() -> c.tablisteGroesse = Anzeigen.naechste(c.tablisteGroesse, 1));
		auswahl(g, mx, my, "CHAT", c.chatGroesse + " %", x + sb + 6, zy, sb,
				() -> setzeChat(c, -1), () -> setzeChat(c, 1));
		zy += 22;
		return zy - y;
	}

	/** Chat-Größe über die Minecraft-Option, damit Klicks auf Chat-Links weiter passen. */
	private void setzeChat(BambusConfig c, int richtung) {
		c.chatGroesse = Math.min(100, Anzeigen.naechste(c.chatGroesse, richtung));
		minecraft.options.chatScale().set(c.chatGroesse / 100.0);
		minecraft.options.save();
	}

	/** Zeichnet Karten im Raster und gibt die benötigte Höhe zurück. */
	private int karten(GuiGraphics g, int mx, int my, List<Karte> karten, int x, int y, int b) {
		return karten(g, mx, my, karten, x, y, b, b >= 420 ? 3 : 2);
	}

	private int karten(GuiGraphics g, int mx, int my, List<Karte> karten, int x, int y, int b, int spalten) {
		int abstand = 6;
		int kb = (b - 6 - abstand * (spalten - 1)) / spalten;
		int kh = 38;

		for (int i = 0; i < karten.size(); i++) {
			Karte k = karten.get(i);
			int kx = x + (i % spalten) * (kb + abstand);
			int ky = y + (i / spalten) * (kh + abstand);
			boolean an = k.an().getAsBoolean();
			boolean hover = imInhalt(mx, my) && Neon.ueber(mx, my, kx, ky, kb, kh);

			float hk = cfg().menueAnimation ? Anim.zu("karte:" + k.name(), hover ? 1f : 0f, Anim.HOVER) : (hover ? 1f : 0f);
			Neon.flaeche(g, kx, ky, kb, kh, Anim.mische(Farben.KARTE, Farben.KARTE_HOVER, hk));
			int randHover = MenueStil.clean() ? 0x55FFFFFF : MenueStil.kompakt() ? Farben.alpha(Farben.NEON, 150) : 0xFF2E4058;
			Neon.rahmen(g, kx, ky, kb, kh, Anim.mische(Farben.LINIE, randHover, hk));
			if (an && !MenueStil.clean()) {   // ein Balken zeigt „an“ – der Schalter sagt den Rest (Clean: nur der Schalter)
				g.fill(kx, ky + 1, kx + (MenueStil.kompakt() ? 3 : 2), ky + kh - 1, k.akzent());
			}

			Symbole.zeichne(g, Symbole.fuer(k.fav() != null ? k.fav() : k.name()), kx + 8, ky + 7, 1, an ? k.akzent() : 0xFF4A5A6E);
			String name = kuerzen(k.name(), kb - (k.fav() != null ? 66 : 52), 1f);
			g.drawString(font, name, kx + 20, ky + 7, an ? Farben.WEISS : 0xFFAAB6C4, false);
			String text = kuerzen(k.text(), kb - (k.einstellungen() != null ? 28 : 14), 0.75f);
			Neon.text(g, font, text, kx + 8, ky + 22, 0.75f, an ? Farben.GRAU : 0xFF5E6E82, false);
			Neon.schalter(g, kx + kb - 26, ky + 7, an, k.name());

			bereiche.add(new Bereich(kx, ky, kb, kh, () -> {
				k.umschalten().run();
				speichern();
			}, true, k.einstellungen()));   // Rechtsklick öffnet die Einstellungen
			if (k.einstellungen() != null) {
				punkte(g, mx, my, kx + kb - 14, ky + kh - 16, k.einstellungen());
			}
			if (k.fav() != null) {
				boolean fav = cfg().favoriten.contains(k.fav());
				int sx = kx + kb - 40;
				boolean sh = imInhalt(mx, my) && Neon.ueber(mx, my, sx - 2, ky + 4, 12, 12);
				if (fav || hover) {   // leerer Stern nur, wenn man über der Karte ist
					g.drawString(font, fav ? "★" : "☆", sx, ky + 6, fav ? Farben.GELB : sh ? Farben.WEISS : 0xFF4A5A6E, false);
				}
				klick(sx - 2, ky + 4, 12, 12, () -> {
					if (!cfg().favoriten.remove(k.fav())) {
						cfg().favoriten.add(k.fav());
					}
					speichern();
				}, true);
			}
		}
		int zeilen = (karten.size() + spalten - 1) / spalten;
		return zeilen * (kh + abstand);
	}

	/** Text auf eine Breite kürzen – mit „…“ statt mitten im Wort abzuschneiden. */
	private String kuerzen(String text, int breite, float groesse) {
		int max = (int) (breite / groesse);
		if (font.width(text) <= max) {
			return text;
		}
		return font.plainSubstrByWidth(text, max - font.width("…")).stripTrailing() + "…";
	}

	/** ⋮ – drei Punkte unten rechts auf der Karte, öffnen die Modul-Einstellungen. */
	private void punkte(GuiGraphics g, int mx, int my, int x, int y, Runnable aktion) {
		boolean hover = imInhalt(mx, my) && Neon.ueber(mx, my, x, y, 12, 14);
		if (hover) {
			g.fill(x, y, x + 12, y + 14, Farben.alpha(Farben.NEON, 40));
			g.renderOutline(x, y, 12, 14, Farben.alpha(Farben.NEON, 160));
		}
		int farbe = hover ? Farben.NEON : Farben.GRAU;
		for (int i = 0; i < 3; i++) {
			g.fill(x + 5, y + 3 + i * 4, x + 7, y + 5 + i * 4, farbe);
		}
		klick(x, y, 12, 14, aktion, true);
	}

	// ------------------------------------------------------ Modul-Einstellungen (⋮)

	/**
	 * Live-Modus (Textur-Modul in einer Welt): kompakte Leiste oben in der Mitte, Zeilen in zwei Spalten –
	 * beide Hände unten bleiben frei, jede Änderung wirkt sofort im Spiel.
	 */
	private void liveFenster(GuiGraphics g, int mx, int my, Einstellbar ziel) {
		List<Einstellung> zeilen = ziel.einstellungen();
		int zeile = 18;
		// in zwei Spalten teilen – eine Stufe bleibt bei ihrem Schalter
		int teilung = (zeilen.size() + 1) / 2;
		while (teilung < zeilen.size() && zeilen.get(teilung) instanceof Einstellung.Stufe) {
			teilung++;
		}
		int proSpalte = Math.max(teilung, zeilen.size() - teilung);
		int fb = Math.min(width - 20, 440);
		int fh = 28 + proSpalte * zeile + 4;
		int fx = (width - fb) / 2;
		int fy = 36;   // unter den Minecraft-Einblendungen oben rechts (Tutorial, Erfolge)

		Neon.flaeche(g, fx, fy, fb, fh, MenueStil.bambus() ? 0xE60A0F17 : Farben.PANEL);
		Neon.leuchtRahmen(g, fx, fy, fb, fh, Farben.NEON, 0.8f);
		Symbole.zeichne(g, Symbole.fuer(ziel.symbol()), fx + 8, fy + 8, 1, Farben.NEON);
		Neon.leuchtText(g, font, ziel.titel(), fx + 20, fy + 7, 1f, Farben.NEON);
		Neon.text(g, font, "LIVE · SIEH DIR DEINE HÄNDE AN", fx + 24 + font.width(ziel.titel()), fy + 9, 0.6f, Farben.NEON_CYAN, false);
		knopf(g, mx, my, "FERTIG", fx + fb - 58, fy + 5, 50, 14, true, true, () -> einstellungsZiel = null);
		knopf(g, mx, my, "STANDARD", fx + fb - 124, fy + 5, 62, 14, false, true, () -> {
			ziel.standard();
			speichern();
		});

		int sb = (fb - 16 - 6) / 2;
		for (int i = 0; i < zeilen.size(); i++) {
			int spalte = i < teilung ? 0 : 1;
			int reihe = spalte == 0 ? i : i - teilung;
			int zx = fx + 8 + spalte * (sb + 6), zy = fy + 26 + reihe * zeile;
			Einstellung e = zeilen.get(i);
			if (e instanceof Einstellung.Schalter sch) {
				schalterZeile(g, mx, my, ziel, sch, zx, zy, sb, zeile - 2, true);
			} else if (e instanceof Einstellung.Stufe st) {
				stufenZeile(g, mx, my, st, zx, zy, sb, zeile - 2, true);
			}
		}
	}

	/** Einstellungsfenster (⋮ oder Rechtsklick): links eine Vorschau, rechts Schalter und Stufen (scrollbar). */
	private void einstellungsFenster(GuiGraphics g, int mx, int my, Einstellbar ziel) {
		List<Einstellung> zeilen = ziel.einstellungen();
		int zeile = 20;
		int fb = Math.min(ziel == Texturen.MODUL ? 400 : 360, pb - 30);
		int fh = Math.min(ph - 16, 34 + Math.max(zeilen.size() * zeile, ziel == Texturen.MODUL ? 150 : 96) + 30);
		int fx = px + (pb - fb) / 2;
		int fy = py + (ph - fh) / 2;
		Neon.flaeche(g, px, py, pb, ph, MenueStil.bambus() ? 0xB0020407 : 0xA0000000);
		Neon.flaeche(g, fx, fy, fb, fh, MenueStil.bambus() ? 0xF20A0F17 : Farben.alpha(Farben.PANEL, MenueStil.clean() ? 225 : 250));
		Neon.leuchtRahmen(g, fx, fy, fb, fh, Farben.NEON, 0.8f);
		Neon.ecken(g, fx - 3, fy - 3, fb + 6, fh + 6, 8, Farben.NEON_CYAN);

		// Kopf
		Symbole.zeichne(g, Symbole.fuer(ziel.symbol()), fx + 10, fy + 10, 1, Farben.NEON);
		Neon.leuchtText(g, font, ziel.titel(), fx + 24, fy + 9, 1f, Farben.NEON);
		Neon.text(g, font, "EINSTELLUNGEN", fx + 24, fy + 20, 0.6f, Farben.GRAU, false);
		int sx = fx + fb - 18, sy = fy + 7;
		boolean zuHover = Neon.ueber(mx, my, sx, sy, 12, 12);
		g.drawString(font, "✕", sx + 2, sy + 2, zuHover ? Farben.ROT : Farben.GRAU, false);
		klick(sx, sy, 12, 12, () -> einstellungsZiel = null, false);

		int inhaltY = fy + 32;
		int inhaltH = fh - 32 - 30;

		// Vorschau links
		int vb = 0;
		if (ziel instanceof HudModul m) {
			vb = Math.max(90, Math.min(130, fb * 2 / 5));
			einstellungsVorschau(g, m, fx + 8, inhaltY, vb, inhaltH);
		} else if (ziel == Texturen.MODUL) {
			vb = Math.max(110, Math.min(190, fb / 2));
			TexturAnsicht.zeichne(g, font, fx + 8, inhaltY, vb, inhaltH, mx, my,
					(kx, ky, kb, kh, aktion) -> klick(kx, ky, kb, kh, aktion, false));
		}

		// Zeilen rechts
		int ox = vb > 0 ? fx + 8 + vb + 8 : fx + 8, ob = fb - (ox - fx) - 16;
		einstellungsListe = new int[]{ox, inhaltY, ob, inhaltH};
		int maxScroll = Math.max(0, zeilen.size() * zeile - inhaltH);
		einstellungsScroll = Math.max(0, Math.min(einstellungsScroll, maxScroll));
		g.enableScissor(ox, inhaltY, ox + ob, inhaltY + inhaltH);
		for (int i = 0; i < zeilen.size(); i++) {
			int zy = inhaltY + i * zeile - einstellungsScroll;
			if (zy + zeile < inhaltY || zy > inhaltY + inhaltH) {
				continue;
			}
			boolean ganz = zy >= inhaltY && zy + zeile - 2 <= inhaltY + inhaltH;   // nur ganz sichtbare Zeilen sind klickbar
			Einstellung e = zeilen.get(i);
			if (e instanceof Einstellung.Schalter sch) {
				schalterZeile(g, mx, my, ziel, sch, ox, zy, ob, zeile - 2, ganz);
			} else if (e instanceof Einstellung.Stufe st) {
				stufenZeile(g, mx, my, st, ox, zy, ob, zeile - 2, ganz);
			}
		}
		g.disableScissor();
		if (maxScroll > 0) {   // Scrollbalken
			int balkenH = Math.max(10, inhaltH * inhaltH / (zeilen.size() * zeile));
			int balkenY = inhaltY + (inhaltH - balkenH) * einstellungsScroll / maxScroll;
			g.fill(fx + fb - 5, inhaltY, fx + fb - 3, inhaltY + inhaltH, Farben.LINIE);
			g.fill(fx + fb - 5, balkenY, fx + fb - 3, balkenY + balkenH, Farben.NEON);
		}

		// Fuß
		int knopfY = fy + fh - 24;
		knopf(g, mx, my, "STANDARD", fx + 8, knopfY, 80, 16, false, true, () -> {
			ziel.standard();
			speichern();
		});
		knopf(g, mx, my, "FERTIG", fx + fb - 78, knopfY, 70, 16, true, true, () -> einstellungsZiel = null);
	}

	private void schalterZeile(GuiGraphics g, int mx, int my, Einstellbar ziel, Einstellung.Schalter sch,
							   int x, int y, int b, int h, boolean klickbar) {
		boolean hover = klickbar && Neon.ueber(mx, my, x, y, b, h);
		boolean an = sch.an().getAsBoolean();
		g.fill(x, y, x + b, y + h, hover ? Farben.KARTE_HOVER : Farben.KARTE);
		if (an) {
			g.fill(x, y, x + 2, y + h, Farben.NEON);
		}
		g.drawString(font, font.plainSubstrByWidth(sch.name(), b - 36), x + 8, y + 5, an ? Farben.WEISS : 0xFFC8D2DE, false);
		Neon.schalter(g, x + b - 26, y + 4, an, ziel.symbol() + ":" + sch.name());
		if (klickbar) {
			klick(x, y, b, h, () -> {
				sch.umschalten().run();
				speichern();
			}, false);
		}
	}

	/** Zeile „Name   ‹ Wert ›“ – ausgegraut, solange der zugehörige Schalter aus ist. */
	private void stufenZeile(GuiGraphics g, int mx, int my, Einstellung.Stufe st, int x, int y, int b, int h, boolean klickbar) {
		boolean aktiv = st.aktiv().getAsBoolean();
		g.fill(x + 8, y, x + b, y + h, Farben.alpha(Farben.KARTE, 200));
		g.fill(x + 8, y, x + 9, y + h, Farben.LINIE);   // eingerückt: gehört zum Schalter darüber
		Neon.text(g, font, st.name(), x + 14, y + 6, 0.75f, aktiv ? 0xFFC8D2DE : 0xFF4A5A6E, false);
		int wb = 70, wx = x + b - wb - 4;
		g.renderOutline(wx, y + 2, wb, h - 4, aktiv ? Farben.LINIE : 0xFF161F2B);
		boolean hl = aktiv && klickbar && Neon.ueber(mx, my, wx, y + 2, 14, h - 4);
		boolean hr = aktiv && klickbar && Neon.ueber(mx, my, wx + wb - 14, y + 2, 14, h - 4);
		int grau = aktiv ? Farben.GRAU : 0xFF33404F;
		g.drawString(font, "‹", wx + 4, y + 5, hl ? Farben.NEON : grau, false);
		g.drawString(font, "›", wx + wb - 9, y + 5, hr ? Farben.NEON : grau, false);
		String wert = st.text();
		g.drawString(font, wert, wx + (wb - font.width(wert)) / 2, y + 5, aktiv ? Farben.WEISS : 0xFF4A5A6E, false);
		if (aktiv && klickbar) {
			klick(wx, y + 2, 14, h - 4, () -> {
				st.aendern(-1);
				speichern();
			}, false);
			klick(wx + wb - 14, y + 2, 14, h - 4, () -> {
				st.aendern(1);
				speichern();
			}, false);
		}
	}

	/** Das HUD-Modul selbst, eingepasst in die Box (wie im HUD, mit Beispielwerten). */
	private void einstellungsVorschau(GuiGraphics g, HudModul m, int x, int y, int b, int h) {
		vorschauBox(g, x, y, b, h);
		int mb = m.breite(minecraft, true), mh = m.hoehe(minecraft, true);
		if (mb <= 0 || mh <= 0) {
			String[] leer = {"Alles", "ausgeschaltet"};
			for (int i = 0; i < leer.length; i++) {
				int tb = (int) (font.width(leer[i]) * 0.75f);
				Neon.text(g, font, leer[i], x + (b - tb) / 2, y + h / 2 - 8 + i * 9, 0.75f, Farben.GRAU, false);
			}
			return;
		}
		float s = Math.min(1.5f, Math.min((b - 12) / (float) mb, (h - 12) / (float) mh));
		g.enableScissor(x + 1, y + 1, x + b - 1, y + h - 1);
		g.pose().pushMatrix();
		g.pose().translate(x + (b - mb * s) / 2f, y + (h - mh * s) / 2f);
		g.pose().scale(s, s);
		m.zeichne(g, minecraft, 0, 0, true);
		g.pose().popMatrix();
		g.disableScissor();
	}

	private int katBreite(String kat) {
		return kat.equals("★") ? 18 : font.width(kat) + 22;
	}

	private static String katSymbol(String kat) {
		return switch (kat) {
			case "PVP" -> "pvp";
			case "HUD" -> "hud";
			case "INFO" -> "info";
			case "★" -> "stern";
			default -> "alle";
		};
	}

	/** Suche + Kategorien über der Modul-Liste. */
	private void suchleiste(GuiGraphics g, int mx, int my) {
		int y = py + 36;
		// Suchfeld so breit, dass alle Kategorien daneben passen
		int katGesamt = 0;
		for (String kat : KATEGORIEN) {
			katGesamt += katBreite(kat) + 4;
		}
		int suchB = Math.max(60, Math.min(120, ib - katGesamt - 8));
		suchFeld.setX(ix);
		suchFeld.setY(y + 1);
		suchFeld.setWidth(suchB);
		int start = ix + suchB + 8;
		int x = start;
		int aktivX = x, aktivB = 0;
		for (String kat : KATEGORIEN) {
			int b = katBreite(kat);
			if (kategorie.equals(kat)) { aktivX = x; aktivB = b; }
			x += b + 4;
		}
		x = start;
		for (String kat : KATEGORIEN) {
			int b = katBreite(kat);
			boolean hover = Neon.ueber(mx, my, x, y, b, 16);
			g.fill(x, y, x + b, y + 16, hover ? Farben.KARTE_HOVER : Farben.KARTE);
			g.renderOutline(x, y, b, 16, Farben.LINIE);
			x += b + 4;
		}
		boolean weich = cfg().menueAnimation;
		int mxk = weich ? Math.round(Anim.zu("kat-x", aktivX, Anim.GLEITEN)) : aktivX;
		int mbk = weich ? Math.round(Anim.zu("kat-b", aktivB, Anim.GLEITEN)) : aktivB;
		g.fill(mxk, y, mxk + mbk, y + 16, Farben.alpha(Farben.NEON, 40));
		g.renderOutline(mxk, y, mbk, 16, Farben.NEON);
		x = start;
		for (String kat : KATEGORIEN) {
			int b = katBreite(kat);
			boolean aktiv = kategorie.equals(kat);
			int farbe = aktiv ? Farben.NEON : kat.equals("★") ? Farben.GELB : Farben.GRAU;
			Symbole.zeichne(g, katSymbol(kat), x + 5, y + 4, 1, farbe);
			if (!kat.equals("★")) g.drawString(font, kat, x + 16, y + 4, farbe, false);
			final String ziel = kat;
			klick(x, y, b, 16, () -> {
				if (!kategorie.equals(ziel)) tabSeit = System.currentTimeMillis();
				kategorie = ziel;
				scroll = 0;
			}, false);
			x += b + 4;
		}
	}

	/** MEHR: Design, Profile, Teilen, Tasten, Statistik und Extras. */
	private int mehr(GuiGraphics g, int mx, int my, int x, int y, int b) {
		BambusConfig c = cfg();
		int zy = y;
		int hb = (b - 6) / 2;

		Neon.text(g, font, "DESIGN", x, zy, 0.75f, Farben.NEON, false);
		zy += 12;
		auswahl(g, mx, my, "THEMA", Farben.THEMEN[Math.floorMod(c.thema, Farben.THEMEN.length)], x, zy, hb,
				() -> c.thema = Math.floorMod(c.thema - 1, Farben.THEMEN.length),
				() -> c.thema = Math.floorMod(c.thema + 1, Farben.THEMEN.length));
		zy += 22;
		MenueStil stil = MenueStil.von(c.menueStil);
		auswahl(g, mx, my, "MENÜ-STIL", stil.name, x, zy, hb,
				() -> c.menueStil = stil.naechster(-1).id,
				() -> c.menueStil = stil.naechster(1).id);
		Neon.text(g, font, stil.beschreibung, x + hb + 8, zy + 6, 0.6f, Farben.GRAU, false);
		zy += 22;
		zy += karten(g, mx, my, List.of(
				new Karte("Farbverlauf", "HUD-Kästen mit Verlauf statt einer Farbe", () -> c.verlauf, () -> c.verlauf = !c.verlauf, Farben.NEON_CYAN),
				new Karte("Weiche Animationen", "Menü, Schalter und Tabs gleiten weich", () -> c.menueAnimation, () -> c.menueAnimation = !c.menueAnimation, Farben.NEON),
				new Karte("Benachrichtigungen", "Neon-Karten für Updates, Freunde, Screenshots", () -> c.hinweise, () -> c.hinweise = !c.hinweise, Farben.NEON_CYAN),
				new Karte("Menü-Hintergrund: " + MenueHintergrund.name(c.menueHintergrund), "Klicken zum Wechseln (wie im Launcher)", () -> true,
						() -> c.menueHintergrund = MenueHintergrund.naechste(c.menueHintergrund), Farben.NEON_CYAN),
				new Karte("Bambus-Ladebildschirm", "Eigener Ladebildschirm beim Betreten einer Welt", () -> c.ladebildschirm, () -> c.ladebildschirm = !c.ladebildschirm, Farben.NEON_PINK),
				new Karte("Bambus-Serverauswahl", "Serverliste mit Favoriten ★, Suche und Ping", () -> c.serverMenue, () -> c.serverMenue = !c.serverMenue, Farben.GELB)),
				x, zy, b, 2);

		Neon.text(g, font, "PROFILE", x, zy + 2, 0.75f, Farben.NEON, false);
		zy += 14;
		for (int i = 0; i < Profile.NAMEN.length; i++) {
			final int slot = i;
			int px0 = x + i * (b / 3);
			Neon.text(g, font, Profile.NAMEN[i] + (Profile.vorhanden(i) ? "" : " (leer)"), px0, zy + 4, 0.75f, Farben.WEISS, false);
			kleinKnopf(g, mx, my, "SPEICHERN", px0, zy + 13, 52, () -> melde(Profile.speichern(slot) ? "Profil „" + Profile.NAMEN[slot] + "“ gespeichert" : "Speichern fehlgeschlagen"));
			if (Profile.vorhanden(i)) {
				kleinKnopf(g, mx, my, "LADEN", px0 + 56, zy + 13, 40, () -> melde(Profile.laden(slot) ? "Profil „" + Profile.NAMEN[slot] + "“ geladen" : "Laden fehlgeschlagen"));
			}
		}
		zy += 36;

		Neon.text(g, font, "TEILEN & STEUERUNG", x, zy, 0.75f, Farben.NEON, false);
		zy += 12;
		kleinKnopf(g, mx, my, "EINSTELLUNGS-CODE", x, zy, 124, () -> minecraft.setScreen(new EinstellungsCodeScreen(this)));
		kleinKnopf(g, mx, my, "TASTENBELEGUNG", x + 128, zy, 84, () ->
				minecraft.setScreen(new KeyBindsScreen(this, minecraft.options)));
		zy += 22;
		if (!meldung.isEmpty() && System.currentTimeMillis() - meldungZeit < 4000) {
			Neon.text(g, font, meldung, x, zy, 0.75f, Farben.NEON_CYAN, false);
		}
		zy += 12;

		Neon.text(g, font, "STATISTIK", x, zy, 0.75f, Farben.NEON, false);
		zy += 12;
		Statistik.Daten d = Statistik.daten;
		String[][] werte = {
				{"Kills", String.valueOf(d.kills)}, {"davon Spieler", String.valueOf(d.killsSpieler)},
				{"Tode", String.valueOf(d.tode)}, {"K/D", Statistik.kd()},
				{"Beste Combo", String.valueOf(d.besteCombo)}, {"Spielzeit", Statistik.spielzeit()}};
		for (int i = 0; i < werte.length; i++) {
			int wx = x + (i % 3) * (b / 3), wy = zy + (i / 3) * 22;
			g.fill(wx, wy, wx + b / 3 - 6, wy + 19, Farben.KARTE);
			Neon.text(g, font, werte[i][0].toUpperCase(), wx + 5, wy + 3, 0.6f, Farben.GRAU, false);
			g.drawString(font, werte[i][1], wx + 5, wy + 10, Farben.WEISS, false);
		}
		zy += 48;
		zy = extras(g, mx, my, x, zy, b);
		return zy - y;
	}

	private static final String[][] SERVER = {
			{"Hypixel", "mc.hypixel.net", "Minispiele, SkyBlock, Bedwars"},
			{"GommeHD.net", "gommehd.net", "Deutsch · Bedwars, SkyWars"},
			{"GrieferGames", "griefergames.net", "Deutsch · Citybuild"},
			{"Minemen Club", "eu.minemen.club", "PvP-Training · Practice"},
	};

	/** Freunde, Extras (Screenshots, Auto-Screenshot, Discord) und empfohlene Server. */
	private int extras(GuiGraphics g, int mx, int my, int x, int zy, int b) {
		BambusConfig c = cfg();
		Neon.text(g, font, "FREUNDE", x, zy, 0.75f, Farben.NEON, false);
		zy += 12;
		// Eingabefeld scrollt mit – nur zeigen, wenn es ganz im sichtbaren Bereich ist
		freundFeld.setX(x);
		freundFeld.setY(zy);
		freundFeld.setVisible(zy >= iy && zy + 14 <= iy + ih);
		kleinKnopf(g, mx, my, "+ FREUND", x + 124, zy, 56, () -> {
			String n = freundFeld.getValue().trim();
			if (n.matches("[A-Za-z0-9_]{2,16}") && c.freunde.stream().noneMatch(f -> f.equalsIgnoreCase(n))) {
				c.freunde.add(n);
				freundFeld.setValue("");
				speichern();
			}
		});
		zy += 20;
		int fx = x;
		for (String f : new ArrayList<>(c.freunde)) {
			int fb = font.width(f) + 22;
			if (fx + fb > x + b) {
				fx = x;
				zy += 16;
			}
			g.fill(fx, zy, fx + fb, zy + 13, Farben.alpha(Farben.NEON_PINK, 40));
			g.drawString(font, f, fx + 4, zy + 3, Farben.WEISS, false);
			g.drawString(font, "✕", fx + fb - 10, zy + 3, Farben.GRAU, false);
			klick(fx + fb - 12, zy, 12, 13, () -> {
				c.freunde.remove(f);
				speichern();
			}, true);
			fx += fb + 4;
		}
		if (c.freunde.isEmpty()) {
			Neon.text(g, font, "Noch keine Freunde – Name eintippen und + FREUND.", x, zy + 3, 0.75f, Farben.GRAU, false);
		}
		zy += 20;

		Neon.text(g, font, "EXTRAS", x, zy, 0.75f, Farben.NEON, false);
		zy += 12;
		String discordText = DiscordStatus.moeglich() ? "Zeigt in Discord „Spielt Bambus Client“" : "Braucht eine Discord-App-ID (siehe Anleitung)";
		zy += karten(g, mx, my, List.of(
				new Karte("Auto-Screenshot", "Screenshot bei jedem Spieler-Kill", () -> c.autoScreenshot, () -> c.autoScreenshot = !c.autoScreenshot, Farben.NEON_CYAN),
				new Karte("Discord-Status", discordText, () -> c.discordAn, () -> c.discordAn = !c.discordAn, Farben.NEON_PINK),
				new Karte("Bambus-Helfer", "Hilfe-Chat bei Rechts-Shift anbieten", () -> c.helferAn, () -> c.helferAn = !c.helferAn, Farben.NEON)),
				x, zy, b, 2);
		kleinKnopf(g, mx, my, "SCREENSHOTS ANSEHEN", x, zy, 110, () -> minecraft.setScreen(new ScreenshotScreen(this)));
		kleinKnopf(g, mx, my, "RESSOURCENPAKETE", x + 114, zy, 100, () ->
				minecraft.setScreen(new net.minecraft.client.gui.screens.options.OptionsScreen(this, minecraft.options)));
		zy += 24;

		Neon.text(g, font, "EMPFOHLENE SERVER", x, zy, 0.75f, Farben.NEON, false);
		zy += 12;
		for (String[] sv : SERVER) {
			g.fill(x, zy, x + b - 6, zy + 18, Farben.KARTE);
			g.drawString(font, sv[0], x + 6, zy + 5, Farben.WEISS, false);
			Neon.text(g, font, sv[1] + " · " + sv[2], x + 10 + font.width(sv[0]), zy + 7, 0.6f, Farben.GRAU, false);
			kleinKnopf(g, mx, my, "+ LISTE", x + b - 62, zy + 2, 54, () -> {
				net.minecraft.client.multiplayer.ServerList liste = new net.minecraft.client.multiplayer.ServerList(minecraft);
				liste.load();
				if (liste.get(sv[1]) == null) {
					liste.add(new net.minecraft.client.multiplayer.ServerData(sv[0], sv[1],
							net.minecraft.client.multiplayer.ServerData.Type.OTHER), false);
					liste.save();
					melde(sv[0] + " steht jetzt in deiner Serverliste");
				} else {
					melde(sv[0] + " ist schon in deiner Serverliste");
				}
			});
			zy += 21;
		}
		return zy + 4;
	}

	// ----------------------------------------------------------- Fadenkreuz

	private int fadenkreuz(GuiGraphics g, int mx, int my, int x, int y, int b) {
		BambusConfig c = cfg();

		// Vorschau
		int vb = 96;
		vorschauBox(g, x, y, vb, vb);
		Fadenkreuz.zeichneBei(g, x + vb / 2, y + vb / 2);
		Neon.text(g, font, "VORSCHAU", x + 4, y + vb + 4, 0.75f, Farben.GRAU, false);

		int rx = x + vb + 12;
		int rb = b - vb - 18;
		karten(g, mx, my, List.of(new Karte("Eigenes Fadenkreuz", "Ersetzt das normale Fadenkreuz",
				() -> c.fadenkreuzAn, () -> c.fadenkreuzAn = !c.fadenkreuzAn, Farben.NEON)), rx, y, rb, 1);

		int zy = y + 46;
		auswahl(g, mx, my, "FORM", Fadenkreuz.FORMEN[Math.floorMod(c.fadenkreuzForm, Fadenkreuz.FORMEN.length)],
				rx, zy, rb,
				() -> c.fadenkreuzForm = Math.floorMod(c.fadenkreuzForm - 1, Fadenkreuz.FORMEN.length),
				() -> c.fadenkreuzForm = Math.floorMod(c.fadenkreuzForm + 1, Fadenkreuz.FORMEN.length));

		zy += 26;
		Neon.text(g, font, "FARBE", rx, zy + 5, 0.75f, Farben.GRAU, false);
		int fx = rx + 44;
		for (int i = 0; i < Fadenkreuz.FARBEN.length; i++) {
			int sx = fx + i * 18;
			boolean gewaehlt = Math.floorMod(c.fadenkreuzFarbe, Fadenkreuz.FARBEN.length) == i;
			if (gewaehlt) {
				Neon.leuchtRahmen(g, sx - 1, zy - 1, 16, 16, Fadenkreuz.FARBEN[i], 1f);
			}
			g.fill(sx + 1, zy + 1, sx + 13, zy + 13, Fadenkreuz.FARBEN[i]);
			final int index = i;
			klick(sx, zy, 14, 14, () -> {
				c.fadenkreuzFarbe = index;
				speichern();
			}, true);
		}

		zy += 24;
		auswahl(g, mx, my, "GRÖSSE", String.valueOf(c.fadenkreuzGroesse), rx, zy, rb,
				() -> c.fadenkreuzGroesse = Math.max(Fadenkreuz.MIN_GROESSE, c.fadenkreuzGroesse - 1),
				() -> c.fadenkreuzGroesse = Math.min(Fadenkreuz.MAX_GROESSE, c.fadenkreuzGroesse + 1));

		return Math.max(vb + 16, zy + 22 - y);
	}

	/** Zeile „NAME   ‹ Wert ›“ mit zwei Pfeilen. */
	private void auswahl(GuiGraphics g, int mx, int my, String name, String wert, int x, int y, int b,
						 Runnable links, Runnable rechts) {
		Neon.text(g, font, name, x, y + 5, 0.75f, Farben.GRAU, false);
		int wx = x + 44;
		int wb = Math.min(b - 44, 150);
		g.fill(wx, y, wx + wb, y + 16, Farben.KARTE);
		g.renderOutline(wx, y, wb, 16, Farben.LINIE);

		boolean hl = imInhalt(mx, my) && Neon.ueber(mx, my, wx, y, 16, 16);
		boolean hr = imInhalt(mx, my) && Neon.ueber(mx, my, wx + wb - 16, y, 16, 16);
		g.drawString(font, "‹", wx + 6, y + 4, hl ? Farben.NEON : Farben.GRAU, false);
		g.drawString(font, "›", wx + wb - 10, y + 4, hr ? Farben.NEON : Farben.GRAU, false);
		String w = font.plainSubstrByWidth(wert, wb - 36);
		g.drawString(font, w, wx + (wb - font.width(w)) / 2, y + 4, Farben.WEISS, false);

		klick(wx, y, 16, 16, () -> {
			links.run();
			speichern();
		}, true);
		klick(wx + wb - 16, y, 16, 16, () -> {
			rechts.run();
			speichern();
		}, true);
	}

	private void kleinKnopf(GuiGraphics g, int mx, int my, String text, int x, int y, int b, Runnable aktion) {
		boolean hover = imInhalt(mx, my) && Neon.ueber(mx, my, x, y, b, 15);
		Neon.flaeche(g, x, y, b, 15, hover ? Farben.KARTE_HOVER : Farben.KARTE);
		Neon.rahmen(g, x, y, b, 15, hover ? Farben.NEON_PINK : Farben.LINIE);
		int tb = (int) (font.width(text) * 0.6f);
		Neon.text(g, font, text, x + (b - tb) / 2, y + 5, 0.6f, hover ? Farben.WEISS : Farben.GRAU, false);
		klick(x, y, b, 15, aktion, true);
	}

	private void vorschauBox(GuiGraphics g, int x, int y, int b, int h) {
		if (MenueStil.bambus()) {
			g.fillGradient(x, y, x + b, y + h, 0xFF0B1626, 0xFF050A10);
		} else {
			Neon.flaeche(g, x, y, b, h, Farben.KARTE);
		}
		Neon.gitter(g, x, y, b, h, 12, Farben.alpha(Farben.NEON_CYAN, 14));
		Neon.ecken(g, x, y, b, h, 8, Farben.NEON_CYAN);
	}

	// ------------------------------------------------------------- Knöpfe

	private void knopf(GuiGraphics g, int mx, int my, String text, int x, int y, int b, int h,
					   boolean voll, boolean aktiv, Runnable aktion) {
		boolean hover = aktiv && Neon.ueber(mx, my, x, y, b, h);
		if (!aktiv) {
			Neon.flaeche(g, x, y, b, h, Farben.KARTE);
			Neon.rahmen(g, x, y, b, h, Farben.LINIE);
		} else if (voll) {
			Neon.flaeche(g, x, y, b, h, hover ? Farben.mische(Farben.NEON, 0xFFFFFFFF, 0.35f) : Farben.NEON);
			if (hover && MenueStil.bambus()) {
				Neon.leuchtRahmen(g, x, y, b, h, Farben.NEON, 1f);
			}
		} else {
			Neon.flaeche(g, x, y, b, h, hover ? Farben.alpha(Farben.NEON, 30) : Farben.KARTE);
			Neon.rahmen(g, x, y, b, h, hover ? Farben.NEON : Farben.alpha(Farben.NEON, 120));
		}
		int farbe = !aktiv ? Farben.GRAU : voll ? Farben.SCHWARZ : Farben.NEON;
		g.drawString(font, text, x + (b - font.width(text)) / 2, y + (h - 8) / 2, farbe, false);
		if (aktiv) {
			klick(x, y, b, h, aktion, false);
		}
	}

	// ============================================================ Eingabe

	private void klick(int x, int y, int b, int h, Runnable aktion, boolean imInhalt) {
		bereiche.add(new Bereich(x, y, b, h, aktion, imInhalt, null));
	}

	private boolean imInhalt(double mx, double my) {
		return Neon.ueber(mx, my, ix, iy, ib, ih);
	}

	private void klickGeraeusch() {
		minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent e, boolean doppelt) {
		boolean rechts = e.button() == 1;
		if (e.button() == 0 || rechts) {
			// rückwärts, damit oben liegende Bereiche zuerst drankommen
			for (int i = bereiche.size() - 1; i >= 0; i--) {
				Bereich b = bereiche.get(i);
				Runnable aktion = rechts ? b.rechts() : b.aktion();
				if (aktion == null || (b.imInhalt() && !imInhalt(e.x(), e.y()))) {
					continue;
				}
				if (Neon.ueber(e.x(), e.y(), b.x(), b.y(), b.b(), b.h())) {
					klickGeraeusch();
					aktion.run();
					return true;
				}
			}
		}
		if (einstellungsZiel != null) {   // Klick neben das Fenster schließt es
			einstellungsZiel = null;
			return true;
		}
		return super.mouseClicked(e, doppelt);
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double horizontal, double vertikal) {
		if (einstellungsZiel != null) {
			int[] l = einstellungsListe;
			if (Neon.ueber(mx, my, l[0], l[1], l[2], l[3])) {
				einstellungsScroll -= (int) (vertikal * 20);   // begrenzt wird beim Zeichnen
			}
			return true;
		}
		if (imInhalt(mx, my)) {
			scroll -= (int) (vertikal * 18);
			scroll = Math.max(0, Math.min(scroll, Math.max(0, inhaltHoehe - ih)));
			return true;
		}
		return super.mouseScrolled(mx, my, horizontal, vertikal);
	}

	@Override
	public boolean keyPressed(KeyEvent e) {
		if (einstellungsZiel != null && (e.key() == InputConstants.KEY_ESCAPE || e.key() == InputConstants.KEY_TAB)) {
			einstellungsZiel = null;   // ESC/Tab schließt erst das Einstellungsfenster
			return true;
		}
		if (e.key() == InputConstants.KEY_TAB) {
			Tab[] alle = Tab.values();
			int richtung = (e.modifiers() & 1) != 0 ? -1 : 1; // Shift+Tab = zurück
			wechsle(alle[Math.floorMod(tab.ordinal() + richtung, alle.length)]);
			klickGeraeusch();
			return true;
		}
		return super.keyPressed(e);
	}

	@Override
	public void onClose() {
		if (cfg().menueAnimation && schliessSeit == 0) {
			schliessSeit = System.currentTimeMillis();   // erst ausblenden, dann schließen (siehe render)
			return;
		}
		if (schliessSeit == 0 || !cfg().menueAnimation) {
			speichern();
			minecraft.setScreen(eltern);
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
