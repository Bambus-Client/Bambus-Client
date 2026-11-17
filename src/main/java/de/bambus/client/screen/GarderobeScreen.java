package de.bambus.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import de.bambus.client.BambusClient;
import de.bambus.client.EigeneKosmetik;
import de.bambus.client.Farben;
import de.bambus.client.Garderobe;
import de.bambus.client.Kosmetik;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.List;

/**
 * Garderobe – aus dem Escape-Menü erreichbar.
 * Links eine schlanke Seitenleiste mit den Kosmetik-Arten, rechts Vorschau und Auswahl.
 * ESC führt zurück ins Escape-Menü.
 */
public class GarderobeScreen extends Screen {

	private static final int LEISTE = 132;      // Breite der Seitenleiste
	private static final int KOPF = 46;         // Höhe des Kopfbereichs rechts
	private static final int RAND = 12;
	private static final int KACHEL_B = 54;
	private static final int KACHEL_H = 70;
	private static final int ABSTAND = 6;
	private static final int VORSCHAU_B = 110;
	private static final int VORSCHAU_MAX_H = 190;

	private static Garderobe.Art letzteArt = Garderobe.Art.CAPES;

	private record Bereich(int x, int y, int b, int h, Runnable aktion, boolean imRaster) {}

	private final Screen eltern;
	private final List<Bereich> bereiche = new ArrayList<>();
	private final long offenSeit = System.currentTimeMillis();

	private Garderobe.Art art = letzteArt;
	private long artSeit = 0;
	private int scroll = 0;
	private String kaufBereit = null;   // Kosmetik, für die schon einmal geklickt wurde
	private int inhaltHoehe = 0;

	// Raster-Bereich (wird in render berechnet)
	private int rx, ry, rb, rh;

	public GarderobeScreen(Screen eltern) {
		super(Component.literal("Garderobe"));
		this.eltern = eltern;
	}

	private void wechsle(Garderobe.Art neu) {
		if (art != neu) {
			art = neu;
			letzteArt = neu;
			scroll = 0;
			kaufBereit = null;
			artSeit = System.currentTimeMillis();
		}
	}

	private static boolean weich() {
		return BambusClient.config.menueAnimation;
	}

	// =================================================================== Zeichnen

	@Override
	public void renderBackground(GuiGraphics g, int mx, int my, float delta) {
		g.fill(0, 0, width, height, 0x90020407);
		Neon.gitter(g, 0, 0, width, height, 24, Farben.alpha(Farben.NEON, 6));
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float delta) {
		bereiche.clear();
		MenueStil.anwenden();

		// Seitenleiste gleitet von links herein
		float k = weich() ? Anim.aus(Math.min(1f, (System.currentTimeMillis() - offenSeit) / Anim.OEFFNEN_MS)) : 1f;
		int versatz = Math.round((1 - k) * -LEISTE);

		seitenleiste(g, mx, my, versatz);
		inhalt(g, mx, my, versatz + LEISTE + RAND);
		super.render(g, mx, my, delta);
	}

	// ------------------------------------------------------------ Seitenleiste

	private void seitenleiste(GuiGraphics g, int mx, int my, int x) {
		g.fill(x, 0, x + LEISTE, height, Farben.PANEL);
		g.fill(x + LEISTE - 1, 0, x + LEISTE, height, Farben.alpha(Farben.NEON, 110));
		g.fill(x + LEISTE, 0, x + LEISTE + 2, height, Farben.alpha(Farben.NEON, 24));

		// Kopf
		Neon.logo(g, x + 12, 12, 16);
		Neon.leuchtText(g, font, "GARDEROBE", x + 28, 12, 1f, Farben.NEON);
		Neon.text(g, font, "BAMBUS CLIENT", x + 28, 24, 0.6f, Farben.GRAU, false);
		g.fill(x + 8, 36, x + LEISTE - 8, 37, Farben.LINIE);
		Neon.text(g, font, "KOSMETIK", x + 12, 44, 0.6f, Farben.GRAU, false);

		// Einträge – Höhe passt sich dem Fenster an
		Garderobe.Art[] arten = Garderobe.Art.values();
		int y0 = 54;
		int platz = height - y0 - 34;
		int eh = Math.max(18, Math.min(28, platz / arten.length - 2));
		boolean mitUntertitel = eh >= 24;

		int aktivY = y0 + art.ordinal() * (eh + 2);
		int markY = weich() ? Math.round(Anim.zu("garderobe-markierung", aktivY, Anim.GLEITEN)) : aktivY;
		g.fill(x + 6, markY, x + LEISTE - 8, markY + eh, Farben.alpha(Farben.NEON, 26));
		g.fill(x + 6, markY, x + 8, markY + eh, Farben.NEON);

		for (int i = 0; i < arten.length; i++) {
			Garderobe.Art a = arten[i];
			int y = y0 + i * (eh + 2);
			boolean aktiv = a == art;
			boolean hover = Neon.ueber(mx, my, x + 6, y, LEISTE - 14, eh);
			float h = weich() ? Anim.zu("garderobe-hover:" + a, hover ? 1f : 0f, Anim.HOVER) : (hover ? 1f : 0f);
			if (!aktiv && h > 0.01f) {
				g.fill(x + 6, y, x + LEISTE - 8, y + eh, Farben.alpha(Farben.KARTE_HOVER, (int) (255 * h)));
			}

			int farbe = aktiv ? Farben.NEON : Anim.mische(Farben.GRAU, Farben.WEISS, h);
			int textY = mitUntertitel ? y + 5 : y + (eh - 8) / 2;
			KosmetikSymbole.artSymbol(g, a, x + 14, mitUntertitel ? y + 8 : y + (eh - 12) / 2, farbe);
			g.drawString(font, a.titel, x + 32, textY, aktiv ? Farben.WEISS : 0xFFC8D2DE, false);
			if (mitUntertitel) {
				Garderobe.Eintrag gewaehlt = a.gewaehlt();
				String unter = font.plainSubstrByWidth(gewaehlt != null ? gewaehlt.name() : "Keins", (int) ((LEISTE - 44) / 0.6f));
				Neon.text(g, font, unter, x + 32, y + 16, 0.6f, gewaehlt != null ? (aktiv ? Farben.NEON : Farben.GRAU) : 0xFF4A5A6E, false);
			}
			klick(x + 6, y, LEISTE - 14, eh, () -> wechsle(a), false);
		}

		knopf(g, mx, my, "ZURÜCK", x + 8, height - 26, LEISTE - 16, 18, this::onClose);
	}

	// ------------------------------------------------------------------ Inhalt

	private void inhalt(GuiGraphics g, int mx, int my, int x) {
		int b = width - x - RAND;
		List<Garderobe.Eintrag> eintraege = art.eintraege();
		Garderobe.Eintrag gewaehlt = art.gewaehlt();

		// Kopf
		Neon.leuchtText(g, font, art.titel.toUpperCase(java.util.Locale.ROOT), x, 13, 1.25f, Farben.NEON);
		String unter = art.beschreibung + " · " + eintraege.size() + " Designs";
		Neon.text(g, font, unter, x, 28, 0.75f, Farben.GRAU, false);
		g.fill(x, KOPF - 8, x + b, KOPF - 7, Farben.LINIE);

		// Eigene Capes/Flügel: Ordner öffnen + neu einlesen
		if (art == Garderobe.Art.CAPES || art == Garderobe.Art.FLUEGEL) {
			kleinKnopf(g, mx, my, "⟳ LADEN", x + b - 50, 14, 50, EigeneKosmetik::neuLaden);
			kleinKnopf(g, mx, my, "ORDNER", x + b - 104, 14, 50, () -> Util.getPlatform().openPath(EigeneKosmetik.ordner()));
		}
		// Flügel-Stil: HD (weich) oder Blockbench (eckig wie ein Minecraft-Modell)
		if (art == Garderobe.Art.FLUEGEL) {
			kleinKnopf(g, mx, my, BambusClient.config.fluegelBlock ? "STIL: BLOCKBENCH" : "STIL: HD", x + b - 198, 14, 90, () -> {
				BambusClient.config.fluegelBlock = !BambusClient.config.fluegelBlock;
				BambusClient.config.speichern();
			});
		}

		// Emotes: Rad in der Mitte, Emotes links und rechts – ziehen oder klicken (eigene Ansicht statt Raster)
		if (art == Garderobe.Art.EMOTES) {
			kleinKnopf(g, mx, my, BambusClient.config.emoteMusik ? "MUSIK: AN" : "MUSIK: AUS", x + b - 70, 14, 70, () -> {
				BambusClient.config.emoteMusik = !BambusClient.config.emoteMusik;
				BambusClient.config.speichern();
			});
			String blaetter = de.bambus.client.Blaetter.blaetter() + " Blätter";
			Neon.text(g, font, blaetter, x + b - 78 - (int) (font.width(blaetter) * 0.75f), 18, 0.75f,
					de.bambus.client.Blaetter.blattFarbe(), false);
			emoteEditor(g, mx, my, x, b);
			return;
		}
		de.bambus.client.emote.Emotes.setzeVorschau(null);

		// Vorschau mit Spielerfigur – kompakte Box, nur wenn genug Platz ist
		int vb = b >= 260 ? VORSCHAU_B : 0;
		if (vb > 0) {
			int vh = Math.min(height - KOPF - RAND, VORSCHAU_MAX_H);
			vorschau(g, mx, my, x, KOPF, vb, vh, gewaehlt);
		}

		// Kacheln
		rx = x + (vb > 0 ? vb + RAND : 0);
		ry = KOPF;
		rb = x + b - rx;
		rh = height - ry - RAND;
		g.enableScissor(rx, ry, rx + rb, ry + rh);
		float wechsel = weich() ? Anim.aus(Math.min(1f, (System.currentTimeMillis() - artSeit) / Anim.WECHSEL_MS)) : 1f;
		int y0 = ry - scroll + Math.round((1 - wechsel) * 10);
		inhaltHoehe = raster(g, mx, my, eintraege, gewaehlt, rx, y0, rb);
		if (wechsel < 1f) {
			g.fill(rx, ry, rx + rb, ry + rh, Farben.alpha(0xFF05080C, (int) ((1 - wechsel) * 200)));
		}
		g.disableScissor();

		int maxScroll = Math.max(0, inhaltHoehe - rh);
		scroll = Math.max(0, Math.min(scroll, maxScroll));
		if (maxScroll > 0) {
			int balkenH = Math.max(12, rh * rh / inhaltHoehe);
			int balkenY = ry + (rh - balkenH) * scroll / maxScroll;
			g.fill(rx + rb - 2, ry, rx + rb, ry + rh, Farben.LINIE);
			g.fill(rx + rb - 2, balkenY, rx + rb, balkenY + balkenH, Farben.NEON);
		}
	}

	// ------------------------------------------------------------ Emote-Editor

	/** Ein Emote in den Listen links und rechts. */
	private record EmoteZeile(int x, int y, int b, int h, String id) {}

	private final List<EmoteZeile> emoteZeilen = new ArrayList<>();
	private String gezogen;              // Emote, das gerade mit der Maus gezogen wird
	private double zugX, zugY;
	private boolean zugBewegt;
	private float radX, radY;            // Lage des Rads (für Klicks und Ablegen)
	private int radD;

	/** Rad in der Mitte, Emotes links und rechts – wie ein Kleiderschrank für Tänze. */
	private void emoteEditor(GuiGraphics g, int mx, int my, int x, int b) {
		emoteZeilen.clear();
		List<de.bambus.client.emote.Emote> alle = de.bambus.client.emote.Emotes.ALLE;
		String[] rad = de.bambus.client.emote.Emotes.rad();
		int oben = KOPF, unten = height - RAND - 14;
		int platz = unten - oben;
		// das Rad lässt den Listen links und rechts möglichst je 110 Pixel, wird aber nie kleiner als 110
		radD = Math.max(110, Math.min(Math.min((int) (b * 0.46f), platz), b - 2 * 110));
		radX = x + b / 2f;
		radY = oben + platz / 2f;
		int spalte = Math.max(60, (int) ((b - radD) / 2f) - 10);
		int haelfte = (alle.size() + 1) / 2;
		int zh = Math.max(16, Math.min(30, (platz + 4) / haelfte - 4));
		float t = (System.currentTimeMillis() % 100_000L) / 1000f;

		// Feld des Rads unter der Maus; beim Ziehen zeigt es, wo das Emote landet
		int radFeld = EmoteRadScreen.feld(radX, radY, radD, mx, my, true);
		String hoverId = null;

		for (int i = 0; i < alle.size(); i++) {
			de.bambus.client.emote.Emote e = alle.get(i);
			int zx = i < haelfte ? x : x + b - spalte;
			int zy = oben + (i % haelfte) * (zh + 4);
			boolean hover = gezogen == null && Neon.ueber(mx, my, zx, zy, spalte, zh);
			if (hover) hoverId = e.id();
			int imRad = java.util.Arrays.asList(rad).indexOf(e.id());
			boolean zu = de.bambus.client.emote.Emotes.gesperrt(e);
			g.fill(zx, zy, zx + spalte, zy + zh, hover ? Farben.KARTE_HOVER : Farben.KARTE);
			g.fill(zx, zy, zx + 2, zy + zh, e.farbe());
			g.renderOutline(zx, zy, spalte, zh, hover ? Farben.alpha(e.farbe(), 200) : Farben.LINIE);
			// der eigene Spieler mit diesem Emote (bewegt sich, wenn die Maus darauf ist)
			EmoteSpieler.zeichne(g, e, hover ? t : 0.6f, zx + 3, zy + 1, zx + 22, zy + zh - 1, (int) (zh * 0.48f));
			if (zu) g.fill(zx + 3, zy + 1, zx + 22, zy + zh - 1, 0x880B1322);
			int tx = zx + 24;
			float ns = spalte < 100 ? 0.65f : 0.8f;   // in schmalen Spalten kleiner, damit der Name passt
			String n = font.plainSubstrByWidth(e.name(), (int) ((spalte - 27 - (imRad >= 0 || zu ? 13 : 0)) / ns));
			Neon.text(g, font, n, tx, zy + (zh >= 24 ? 5 : zh / 2 - 3), ns, hover ? Farben.WEISS : 0xFFC8D2DE, false);
			if (zu) {   // noch gesperrt: Schloss und Preis (zweiter Klick kauft)
				de.bambus.client.screen.EmoteRadScreen.schloss(g, zx + spalte - 8, zy + zh / 2 - 3, Farben.NEON_CYAN);
				int p = de.bambus.client.emote.Emotes.preis(e);
				String preis = e.id().equals(kaufBereit) ? "KAUFEN?" : spalte < 100 ? p + " Bl." : p + " BLÄTTER";
				Neon.text(g, font, preis, tx, zy + zh - (zh >= 24 ? 10 : 7), 0.55f, Farben.NEON_CYAN, false);
			} else if (zh >= 24) {
				String bes = font.plainSubstrByWidth(e.beschreibung(), (int) ((spalte - 32) / 0.55f));
				Neon.text(g, font, bes, tx, zy + zh - 10, 0.55f, Farben.GRAU, false);
			}
			if (imRad >= 0) {   // liegt schon im Rad: Platznummer
				g.fill(zx + spalte - 12, zy + 3, zx + spalte - 3, zy + 12, Farben.alpha(Farben.NEON, 60));
				Neon.text(g, font, String.valueOf(imRad + 1), zx + spalte - 10, zy + 5, 0.6f, Farben.NEON, false);
			}
			emoteZeilen.add(new EmoteZeile(zx, zy, spalte, zh, e.id()));
		}

		// Vorschau in der Mitte: das Emote unter der Maus, sonst das gezogene, sonst das des gewählten Platzes
		int gewaehlt = de.bambus.client.emote.Emotes.gewaehlterPlatz;
		String zeigen = hoverId != null ? hoverId : gezogen != null ? gezogen
				: radFeld >= 0 && !rad[radFeld].isEmpty() ? rad[radFeld] : rad[gewaehlt];
		de.bambus.client.emote.Emotes.setzeVorschau(de.bambus.client.emote.Emotes.nachId(zeigen));
		int hervor = gezogen != null && radFeld >= 0 ? radFeld : radFeld >= 0 ? radFeld : gewaehlt;
		EmoteRadScreen.zeichneRad(g, font, minecraft, radX, radY, radD, rad, hervor, t);
		if (gezogen == null && radFeld < 0) {   // gewählter Platz bleibt markiert
			double w = gewaehlt * Math.PI / 4;
			float r = radD / 2f * 0.98f;
			int px = (int) (radX + Math.sin(w) * r), py = (int) (radY - Math.cos(w) * r);
			g.fill(px - 3, py - 3, px + 3, py + 3, Farben.NEON);
		}

		String hinweis = gezogen != null ? "Auf einen Platz im Rad ziehen und loslassen"
				: "Emote ins Rad ziehen – oder Platz anklicken, dann Emote anklicken · Rechtsklick leert einen Platz";
		int hb = (int) (font.width(hinweis) * 0.6f);
		Neon.text(g, font, hinweis, (int) radX - hb / 2, unten + 4, 0.6f, Farben.GRAU, false);

		// das gezogene Emote hängt an der Maus
		if (gezogen != null && zugBewegt) {
			de.bambus.client.emote.Emote e = de.bambus.client.emote.Emotes.nachId(gezogen);
			if (e != null) {
				g.fill(mx - 16, my - 34, mx + 16, my + 8, Farben.alpha(0xFF0B1322, 220));
				g.renderOutline(mx - 16, my - 34, 32, 42, e.farbe());
				EmoteSpieler.zeichne(g, e, t, mx - 15, my - 33, mx + 15, my + 7, 20);
			}
		}
	}

	/** Klick im Emote-Editor: Platz wählen bzw. leeren oder ein Emote aufnehmen. */
	private boolean emoteKlick(MouseButtonEvent e) {
		int feld = EmoteRadScreen.feld(radX, radY, radD, e.x(), e.y(), true);
		if (feld >= 0) {
			if (e.button() == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT) {
				de.bambus.client.emote.Emotes.setzePlatz(feld, "");
			} else {
				de.bambus.client.emote.Emotes.gewaehlterPlatz = feld;
			}
			klickGeraeusch();
			return true;
		}
		if (e.button() != com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT) return false;
		for (EmoteZeile z : emoteZeilen) {
			if (Neon.ueber(e.x(), e.y(), z.x(), z.y(), z.b(), z.h())) {
				de.bambus.client.emote.Emote em = de.bambus.client.emote.Emotes.nachId(z.id());
				if (de.bambus.client.emote.Emotes.gesperrt(em)) {   // erst freischalten (zweiter Klick kauft)
					klickGeraeusch();
					kaufe(z.id(), de.bambus.client.emote.Emotes.preis(em));
					return true;
				}
				gezogen = z.id();
				zugX = e.x();
				zugY = e.y();
				zugBewegt = false;
				return true;
			}
		}
		return false;
	}

	/** Emote losgelassen: auf dem Rad → dieser Platz; nur geklickt → gewählter Platz (dann weiter zum nächsten). */
	private void emoteAblegen(double mx, double my) {
		if (gezogen == null) return;
		int feld = EmoteRadScreen.feld(radX, radY, radD, mx, my, false);
		double dx = mx - radX, dy = my - radY;
		boolean aufRad = Math.sqrt(dx * dx + dy * dy) <= radD / 2f;
		if (zugBewegt && aufRad && feld >= 0) {
			de.bambus.client.emote.Emotes.setzePlatz(feld, gezogen);
			de.bambus.client.emote.Emotes.gewaehlterPlatz = feld;
			klickGeraeusch();
		} else if (!zugBewegt) {
			int platz = de.bambus.client.emote.Emotes.gewaehlterPlatz;
			de.bambus.client.emote.Emotes.setzePlatz(platz, gezogen);
			de.bambus.client.emote.Emotes.gewaehlterPlatz = (platz + 1) % de.bambus.client.emote.Emotes.PLAETZE;
			klickGeraeusch();
		}
		gezogen = null;
	}

	private void vorschau(GuiGraphics g, int mx, int my, int x, int y, int b, int h, Garderobe.Eintrag gewaehlt) {
		g.fillGradient(x, y, x + b, y + h, 0xFF0B1626, 0xFF050A10);
		Neon.gitter(g, x, y, b, h, 12, Farben.alpha(Farben.NEON_CYAN, 14));
		Neon.ecken(g, x, y, b, h, 8, Farben.NEON_CYAN);
		int modellH = h - 26;
		if (minecraft.player != null) {
			InventoryScreen.renderEntityInInventoryFollowsMouse(g, x + 4, y + 4, x + b - 4, y + modellH,
					(int) (modellH * 0.42f), 0.0625f, mx, my, minecraft.player);
		}
		g.fill(x + 6, y + h - 24, x + b - 6, y + h - 23, Farben.LINIE);
		Neon.text(g, font, "AKTIV", x + 8, y + h - 18, 0.6f, Farben.GRAU, false);
		String name = font.plainSubstrByWidth(gewaehlt != null ? gewaehlt.name() : "Keins", b - 16);
		g.drawString(font, name, x + 8, y + h - 11, gewaehlt != null ? Farben.NEON : Farben.GRAU, false);
	}

	/** Zeichnet „Keins“ + alle Einträge als Kacheln und gibt die benötigte Höhe zurück. */
	private int raster(GuiGraphics g, int mx, int my, List<Garderobe.Eintrag> eintraege, Garderobe.Eintrag gewaehlt,
					   int x, int y, int b) {
		int spalten = Math.max(1, (b - 4 + ABSTAND) / (KACHEL_B + ABSTAND));
		int anzahl = eintraege.size() + 1;
		for (int i = 0; i < anzahl; i++) {
			int tx = x + (i % spalten) * (KACHEL_B + ABSTAND);
			int ty = y + (i / spalten) * (KACHEL_H + ABSTAND);
			Garderobe.Eintrag e = i == 0 ? null : eintraege.get(i - 1);
			boolean aktiv = e == null ? gewaehlt == null : gewaehlt != null && gewaehlt.id().equals(e.id());
			kachel(g, mx, my, e, aktiv, tx, ty, i);
		}
		int zeilen = (anzahl + spalten - 1) / spalten;
		return zeilen * (KACHEL_H + ABSTAND);
	}

	/** Eine Kachel; e == null ist „Keins“. */
	private void kachel(GuiGraphics g, int mx, int my, Garderobe.Eintrag e, boolean aktiv, int tx, int ty, int saat) {
		boolean hover = imRaster(mx, my) && Neon.ueber(mx, my, tx, ty, KACHEL_B, KACHEL_H);
		g.fill(tx, ty, tx + KACHEL_B, ty + KACHEL_H, hover ? Farben.KARTE_HOVER : Farben.KARTE);

		int farbe = e != null ? e.farbe() : Farben.GRAU;
		String name = e != null ? e.name() : "Keins";
		if (e != null) {
			KosmetikSymbole.zeichne(g, art, e.id(), e.farbe(), tx + KACHEL_B / 2, ty + 28, saat);
			if (e.eigen()) {
				g.fill(tx + 2, ty + 2, tx + 26, ty + 9, Farben.alpha(Farben.NEON_PINK, 200));
				Neon.text(g, font, "EIGEN", tx + 4, ty + 3, 0.5f, Farben.SCHWARZ, false);
			}
		} else {
			g.drawString(font, "∅", tx + KACHEL_B / 2 - 3, ty + 24, Farben.GRAU, false);
		}

		if (aktiv) {
			Neon.leuchtRahmen(g, tx, ty, KACHEL_B, KACHEL_H, e == null ? Farben.NEON : farbe, 0.6f + 0.4f * Farben.puls());
			g.fill(tx + KACHEL_B - 11, ty + 2, tx + KACHEL_B - 2, ty + 11, Farben.NEON);
			g.drawString(font, "✔", tx + KACHEL_B - 10, ty + 2, Farben.SCHWARZ, false);
		} else {
			g.renderOutline(tx, ty, KACHEL_B, KACHEL_H, hover ? 0xFF2E4058 : Farben.LINIE);
		}

		String n = font.plainSubstrByWidth(name, (int) ((KACHEL_B - 4) / 0.75f));
		int nb = (int) (font.width(n) * 0.75f);
		Neon.text(g, font, n, tx + (KACHEL_B - nb) / 2, ty + KACHEL_H - 22, 0.75f, aktiv ? Farben.WEISS : 0xFFC8D2DE, false);
		String id = e != null ? e.id() : Kosmetik.AUS;
		boolean gesperrt = de.bambus.client.Blaetter.gesperrt(art.schluessel(), id);
		int preis = gesperrt ? de.bambus.client.Blaetter.preis(art.schluessel(), id) : 0;
		if (gesperrt) {   // noch nicht freigeschaltet: abgedunkelt mit Schloss
			g.fill(tx + 1, ty + 1, tx + KACHEL_B - 1, ty + KACHEL_H - 1, 0xC00B1322);
			int sx = tx + KACHEL_B / 2, sy = ty + 24;
			g.fill(sx - 5, sy, sx + 5, sy + 8, Farben.NEON_CYAN);
			g.fill(sx - 3, sy - 4, sx + 3, sy, Farben.NEON_CYAN);
			g.fill(sx - 2, sy - 3, sx + 2, sy, 0xFF0B1322);
		}
		String status = gesperrt ? (id.equals(kaufBereit) ? "KAUFEN?" : preis + " BLÄTTER") : aktiv ? "AKTIV" : "WÄHLEN";
		int sb = (int) (font.width(status) * 0.6f);
		Neon.text(g, font, status, tx + (KACHEL_B - sb) / 2, ty + KACHEL_H - 11, 0.6f,
				gesperrt ? Farben.NEON_CYAN : aktiv ? Farben.NEON : Farben.GRAU, false);

		klick(tx, ty, KACHEL_B, KACHEL_H, () -> {
			if (gesperrt) {
				kaufe(id, preis);
				return;
			}
			kaufBereit = null;
			art.waehle(id);
			BambusClient.config.speichern();
		}, true);
	}

	/** Erster Klick fragt nach, zweiter Klick kauft – damit nichts aus Versehen weg ist. */
	private void kaufe(String id, int preis) {
		int da = de.bambus.client.Blaetter.blaetter();
		if (da < preis) {
			kaufBereit = null;
			de.bambus.client.Hinweise.zeige("stern", "Noch nicht genug Blätter",
					"Es fehlen " + (preis - da) + " – hol dir Quests", Farben.NEON_CYAN);
			return;
		}
		if (!id.equals(kaufBereit)) {
			kaufBereit = id;
			de.bambus.client.Hinweise.zeige("stern", "Für " + preis + " Blätter freischalten?",
					"Noch einmal klicken zum Kaufen", Farben.NEON_CYAN);
			return;
		}
		kaufBereit = null;
		if (de.bambus.client.Blaetter.kaufe(art.schluessel(), id)) {
			art.waehle(id);
			BambusClient.config.speichern();
			String danach = art == Garderobe.Art.EMOTES
					? "liegt jetzt auf Platz " + (de.bambus.client.emote.Emotes.gewaehlterPlatz + 1) + " im Rad"
					: "schon angezogen";
			de.bambus.client.Hinweise.zeige("stern", Garderobe.name(art.schluessel(), id) + " freigeschaltet",
					"−" + preis + " Blätter · " + danach, de.bambus.client.Blaetter.rangFarbe());
		}
	}

	private void kleinKnopf(GuiGraphics g, int mx, int my, String text, int x, int y, int b, Runnable aktion) {
		boolean hover = Neon.ueber(mx, my, x, y, b, 15);
		g.fill(x, y, x + b, y + 15, hover ? Farben.KARTE_HOVER : Farben.KARTE);
		g.renderOutline(x, y, b, 15, hover ? Farben.NEON_PINK : Farben.LINIE);
		int tb = (int) (font.width(text) * 0.6f);
		Neon.text(g, font, text, x + (b - tb) / 2, y + 5, 0.6f, hover ? Farben.WEISS : Farben.GRAU, false);
		klick(x, y, b, 15, aktion, false);
	}

	private void knopf(GuiGraphics g, int mx, int my, String text, int x, int y, int b, int h, Runnable aktion) {
		boolean hover = Neon.ueber(mx, my, x, y, b, h);
		g.fill(x, y, x + b, y + h, hover ? Farben.alpha(Farben.NEON, 30) : Farben.KARTE);
		g.renderOutline(x, y, b, h, hover ? Farben.NEON : Farben.alpha(Farben.NEON, 120));
		g.drawString(font, text, x + (b - font.width(text)) / 2, y + (h - 8) / 2, Farben.NEON, false);
		klick(x, y, b, h, aktion, false);
	}

	// ============================================================ Eingabe

	private void klick(int x, int y, int b, int h, Runnable aktion, boolean imRaster) {
		bereiche.add(new Bereich(x, y, b, h, aktion, imRaster));
	}

	private boolean imRaster(double mx, double my) {
		return Neon.ueber(mx, my, rx, ry, rb, rh);
	}

	private void klickGeraeusch() {
		minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent e, boolean doppelt) {
		if (art == Garderobe.Art.EMOTES && e.button() == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT && emoteKlick(e)) {
			return true;
		}
		if (e.button() == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT) {
			for (int i = bereiche.size() - 1; i >= 0; i--) {
				Bereich b = bereiche.get(i);
				if (b.imRaster() && !imRaster(e.x(), e.y())) {
					continue;
				}
				if (Neon.ueber(e.x(), e.y(), b.x(), b.y(), b.b(), b.h())) {
					klickGeraeusch();
					b.aktion().run();
					return true;
				}
			}
			if (art == Garderobe.Art.EMOTES && emoteKlick(e)) return true;
		}
		return super.mouseClicked(e, doppelt);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
		if (gezogen != null) {
			if (Math.abs(e.x() - zugX) + Math.abs(e.y() - zugY) > 4) zugBewegt = true;
			return true;
		}
		return super.mouseDragged(e, dx, dy);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent e) {
		if (gezogen != null) {
			emoteAblegen(e.x(), e.y());
			return true;
		}
		return super.mouseReleased(e);
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double horizontal, double vertikal) {
		if (imRaster(mx, my)) {
			scroll -= (int) (vertikal * 18);
			scroll = Math.max(0, Math.min(scroll, Math.max(0, inhaltHoehe - rh)));
			return true;
		}
		return super.mouseScrolled(mx, my, horizontal, vertikal);
	}

	@Override
	public boolean keyPressed(KeyEvent e) {
		int richtung = switch (e.key()) {
			case InputConstants.KEY_UP -> -1;
			case InputConstants.KEY_DOWN -> 1;
			case InputConstants.KEY_TAB -> (e.modifiers() & 1) != 0 ? -1 : 1;   // Shift+Tab = zurück
			default -> 0;
		};
		if (richtung != 0) {
			Garderobe.Art[] arten = Garderobe.Art.values();
			wechsle(arten[Math.floorMod(art.ordinal() + richtung, arten.length)]);
			klickGeraeusch();
			return true;
		}
		return super.keyPressed(e);
	}

	@Override
	public void removed() {
		de.bambus.client.emote.Emotes.setzeVorschau(null);
	}

	@Override
	public void onClose() {
		BambusClient.config.speichern();
		minecraft.setScreen(eltern);
	}
}
