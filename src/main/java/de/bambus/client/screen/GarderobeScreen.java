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
		Farben.themaAnwenden(BambusClient.config.thema);

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
		rh = height - KOPF - RAND;
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
		String status = aktiv ? "AKTIV" : "WÄHLEN";
		int sb = (int) (font.width(status) * 0.6f);
		Neon.text(g, font, status, tx + (KACHEL_B - sb) / 2, ty + KACHEL_H - 11, 0.6f, aktiv ? Farben.NEON : Farben.GRAU, false);

		String id = e != null ? e.id() : Kosmetik.AUS;
		klick(tx, ty, KACHEL_B, KACHEL_H, () -> {
			art.waehle(id);
			BambusClient.config.speichern();
		}, true);
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
		if (e.button() == 0) {
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
		}
		return super.mouseClicked(e, doppelt);
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
	public void onClose() {
		BambusClient.config.speichern();
		minecraft.setScreen(eltern);
	}
}
