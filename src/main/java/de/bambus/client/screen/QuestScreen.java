package de.bambus.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import de.bambus.client.BambusClient;
import de.bambus.client.Blaetter;
import de.bambus.client.Farben;
import de.bambus.client.Garderobe;
import de.bambus.client.Hinweise;
import de.bambus.quests.Quests;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.List;

/**
 * QUESTS im Spiel: Blätter, Rang, die Herausforderungen des Tages und der Shop.
 * Die Daten teilt sich der Bildschirm mit dem Launcher (bambus.json), es geht nie um echtes Geld.
 */
public class QuestScreen extends Screen {

	private record Bereich(int x, int y, int b, int h, Runnable aktion) {}

	private final Screen eltern;
	private final List<Bereich> bereiche = new ArrayList<>();
	private int px, py, pb, ph, cx, cy, cb, ch;
	private int scroll;
	private int maxScroll;

	public QuestScreen(Screen eltern) {
		super(Component.literal("Quests"));
		this.eltern = eltern;
	}

	@Override
	public void renderBackground(GuiGraphics g, int mx, int my, float delta) {
		g.fill(0, 0, width, height, 0xC0020407);
		Neon.gitter(g, 0, 0, width, height, 24, Farben.alpha(Farben.NEON, 7));
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float delta) {
		bereiche.clear();
		MenueStil.anwenden();
		pb = Math.min(width - 20, 460);
		ph = Math.min(height - 20, 320);
		px = (width - pb) / 2;
		py = (height - ph) / 2;

		g.fill(px, py, px + pb, py + ph, Farben.PANEL);
		Neon.gitter(g, px, py, pb, ph, 16, Farben.alpha(Farben.NEON, 6));
		Neon.leuchtRahmen(g, px, py, pb, ph, Farben.NEON, 0.55f + 0.45f * Farben.puls());
		Neon.ecken(g, px - 5, py - 5, pb + 10, ph + 10, 12, Farben.NEON_CYAN);

		kopf(g);

		cx = px + 10;
		cy = py + 58;
		cb = pb - 20;
		ch = py + ph - 14 - cy;
		inhalt(g, mx, my);

		super.render(g, mx, my, delta);
	}

	private void kopf(GuiGraphics g) {
		Neon.logo(g, px + 12, py + 8, 16);
		Neon.leuchtText(g, font, "QUESTS", px + 30, py + 9, 1.25f, Farben.NEON);
		Neon.text(g, font, "Blätter sammeln · kostet nie Geld", px + 30, py + 21, 0.6f, Farben.GRAU, false);

		// Guthaben rechts oben
		String bl = Blaetter.blaetter() + " Blätter";
		int bb = font.width(bl);
		Neon.text(g, font, bl, px + pb - 12 - bb, py + 11, 1f, Farben.NEON, true);

		// Rangleiste
		int rx = px + 10, ry = py + 34, rb = pb - 20, rh = 16;
		int gesammelt = Blaetter.gesammelt();
		int farbe = Blaetter.rangFarbe();
		g.fill(rx, ry, rx + rb, ry + rh, Farben.KARTE);
		g.fill(rx, ry, rx + (int) (rb * Quests.rangFortschritt(gesammelt)), ry + rh, Farben.alpha(farbe, 60));
		g.renderOutline(rx, ry, rb, rh, Farben.LINIE);
		Neon.text(g, font, "RANG: " + Blaetter.rangName().toUpperCase(), rx + 6, ry + 4, 0.75f, farbe, false);
		int rest = Quests.bisNaechsterRang(gesammelt);
		String text = rest > 0 ? "noch " + rest + " bis " + Quests.RAENGE[Quests.rangNummer(gesammelt) + 1][0]
				: "höchster Rang";
		int tb = (int) (font.width(text) * 0.75f);
		Neon.text(g, font, text, rx + rb - 6 - tb, ry + 4, 0.75f, Farben.GRAU, false);
	}

	private void inhalt(GuiGraphics g, int mx, int my) {
		g.enableScissor(cx, cy, cx + cb, cy + ch);
		int y = cy - scroll;

		y = ueberschrift(g, "HERAUSFORDERUNGEN HEUTE", y);
		for (String[] q : Blaetter.heuteMitStand()) {
			y = questZeile(g, q, y);
		}
		y += 6;
		y = saisonKarte(g, mx, my, y);
		y = ueberschrift(g, "SHOP  –  NUR MIT BLÄTTERN", y);
		for (String[] s : Quests.SHOP) {
			y = shopZeile(g, mx, my, s, y);
		}
		y += 4;
		Neon.text(g, font, "Tages-Quests und Erfolge gibt es im Launcher auf der Seite QUESTS.",
				cx + 2, y, 0.6f, Farben.GRAU, false);
		y += 12;

		g.disableScissor();
		maxScroll = Math.max(0, y + scroll - cy - ch);
		scroll = Math.max(0, Math.min(scroll, maxScroll));
		if (maxScroll > 0) {   // Scrollbalken
			int bh = Math.max(16, ch * ch / (ch + maxScroll));
			int by = cy + (ch - bh) * scroll / maxScroll;
			g.fill(cx + cb - 2, by, cx + cb, by + bh, Farben.alpha(Farben.NEON, 160));
		}
	}

	private int ueberschrift(GuiGraphics g, String text, int y) {
		Neon.text(g, font, text, cx + 2, y, 0.75f, Farben.NEON, false);
		int tb = (int) (font.width(text) * 0.75f);
		g.fill(cx + tb + 8, y + 3, cx + cb - 4, y + 4, Farben.LINIE);
		return y + 14;
	}

	/** {id, Name, Hinweis, Blätter, Ziel, Stand, fertig} */
	private int questZeile(GuiGraphics g, String[] q, int y) {
		int h = 30;
		boolean fertig = "1".equals(q[6]);
		int ziel = Integer.parseInt(q[4]);
		int stand = Math.min(Integer.parseInt(q[5]), ziel);
		g.fill(cx, y, cx + cb - 4, y + h, Farben.KARTE);
		g.renderOutline(cx, y, cb - 4, h, fertig ? Farben.alpha(Farben.NEON, 170) : Farben.LINIE);
		if (!fertig && ziel > 1) {   // Fortschrittsbalken im Hintergrund
			g.fill(cx + 1, y + h - 3, cx + 1 + (cb - 6) * stand / ziel, y + h - 1, Farben.alpha(Farben.NEON_CYAN, 140));
		}
		g.fill(cx + 5, y + 10, cx + 15, y + 20, fertig ? Farben.NEON : 0xFF2A3647);
		if (fertig) Neon.text(g, font, "✔", cx + 7, y + 11, 0.75f, Farben.SCHWARZ, false);
		Neon.text(g, font, q[1], cx + 22, y + 6, 0.85f, fertig ? Farben.WEISS : 0xFFC8D2DE, false);
		Neon.text(g, font, q[2], cx + 22, y + 18, 0.6f, Farben.GRAU, false);
		String lohn = ziel > 1 && !fertig ? stand + "/" + ziel + "  +" + q[3] : "+" + q[3];
		int lb = (int) (font.width(lohn) * 0.75f);
		Neon.text(g, font, lohn, cx + cb - 10 - lb, y + 11, 0.75f, fertig ? Farben.NEON : Farben.NEON_CYAN, false);
		return y + h + 4;
	}

	/** Das Set der Jahreszeit: vier Stücke zusammen, günstiger als einzeln. */
	private int saisonKarte(GuiGraphics g, int mx, int my, int y) {
		String[] s = Blaetter.saison();
		boolean offen = Blaetter.saisonOffen();
		int h = 38;
		boolean hover = offen && Neon.ueber(mx, my, cx, y, cb - 4, h) && Neon.ueber(mx, my, cx, cy, cb, ch);
		g.fill(cx, y, cx + cb - 4, y + h, hover ? Farben.KARTE_HOVER : Farben.KARTE);
		g.renderOutline(cx, y, cb - 4, h, offen ? (hover ? Farben.NEON_CYAN : Farben.LINIE) : Farben.alpha(Farben.NEON, 170));
		Neon.text(g, font, "SAISON: " + s[1].toUpperCase(), cx + 8, y + 5, 0.85f, Farben.WEISS, false);
		StringBuilder teile = new StringBuilder();
		for (String[] st : Quests.saisonStuecke(s)) {
			if (teile.length() > 0) teile.append("  ·  ");
			teile.append(Garderobe.name(st[0], st[1]));
		}
		Neon.text(g, font, teile.toString(), cx + 8, y + 17, 0.6f, Farben.GRAU, false);
		int einzeln = Quests.saisonEinzeln(s);
		int preis = Integer.parseInt(s[3]);
		if (offen) {
			Neon.text(g, font, "Einzeln: " + einzeln + "  ·  du sparst " + (einzeln - preis),
					cx + 8, y + 27, 0.6f, Farben.NEON_CYAN, false);
		}
		String rechts = offen ? preis + " BLÄTTER" : "✔ KOMPLETT";
		int rb = (int) (font.width(rechts) * 0.75f);
		Neon.text(g, font, rechts, cx + cb - 10 - rb, y + 14, 0.75f, offen ? Farben.NEON_CYAN : Farben.NEON, false);
		if (offen) {
			final int p = preis;
			final String name = s[1];
			bereiche.add(new Bereich(cx, y, cb - 4, h, () -> {
				if (Blaetter.kaufeSaison()) {
					Hinweise.zeige("stern", "Set " + name + " freigeschaltet", "In der Garderobe anziehen", Blaetter.rangFarbe());
				} else {
					Hinweise.zeige("stern", "Noch nicht genug Blätter",
							"Es fehlen " + (p - Blaetter.blaetter()), Farben.NEON_CYAN);
				}
			}));
		}
		return y + h + 8;
	}

	private int shopZeile(GuiGraphics g, int mx, int my, String[] s, int y) {
		int h = 24;
		boolean hat = !Blaetter.gesperrt(s[1], s[0]);
		boolean hover = !hat && Neon.ueber(mx, my, cx, y, cb - 4, h) && Neon.ueber(mx, my, cx, cy, cb, ch);
		g.fill(cx, y, cx + cb - 4, y + h, hover ? Farben.KARTE_HOVER : Farben.KARTE);
		g.renderOutline(cx, y, cb - 4, h, hat ? Farben.alpha(Farben.NEON, 170) : hover ? Farben.NEON_CYAN : Farben.LINIE);
		Neon.text(g, font, s[2], cx + 8, y + 4, 0.85f, hat ? Farben.WEISS : 0xFFC8D2DE, false);
		Neon.text(g, font, s[1].equals("cape") ? "Umhang" : "Partikel", cx + 8, y + 15, 0.6f, Farben.GRAU, false);
		String rechts = hat ? "✔ FREIGESCHALTET" : s[3] + " BLÄTTER";
		int rb = (int) (font.width(rechts) * 0.75f);
		Neon.text(g, font, rechts, cx + cb - 10 - rb, y + 8, 0.75f, hat ? Farben.NEON : Farben.NEON_CYAN, false);
		if (!hat) {
			final String id = s[0], art = s[1], name = s[2];
			final int preis = Integer.parseInt(s[3]);
			bereiche.add(new Bereich(cx, y, cb - 4, h, () -> {
				if (Blaetter.kaufe(art, id)) {
					Hinweise.zeige("stern", name + " freigeschaltet", "In der Garderobe anziehen", Blaetter.rangFarbe());
				} else {
					Hinweise.zeige("stern", "Noch nicht genug Blätter",
							"Es fehlen " + (preis - Blaetter.blaetter()), Farben.NEON_CYAN);
				}
			}));
		}
		return y + h + 4;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent e, boolean doppelt) {
		if (e.button() == 0) {
			for (int i = bereiche.size() - 1; i >= 0; i--) {
				Bereich b = bereiche.get(i);
				if (Neon.ueber(e.x(), e.y(), b.x(), b.y(), b.b(), b.h()) && Neon.ueber(e.x(), e.y(), cx, cy, cb, ch)) {
					minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
					b.aktion().run();
					return true;
				}
			}
		}
		return super.mouseClicked(e, doppelt);
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double horizontal, double vertikal) {
		if (Neon.ueber(mx, my, cx, cy, cb, ch)) {
			scroll -= (int) (vertikal * 18);
			return true;
		}
		return super.mouseScrolled(mx, my, horizontal, vertikal);
	}

	@Override
	public boolean keyPressed(KeyEvent e) {
		if (e.key() == InputConstants.KEY_ESCAPE) {
			onClose();
			return true;
		}
		return super.keyPressed(e);
	}

	@Override
	public void onClose() {
		minecraft.setScreen(eltern);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
