package de.bambus.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import de.bambus.client.BambusClient;
import de.bambus.client.Farben;
import de.bambus.client.Sprache;
import de.bambus.recht.Rechtstext;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Das Rechtliche im Spiel – für alle, die den Client ohne den Bambus Launcher starten.
 *
 * Erscheint vor dem Hauptmenü, solange dem aktuellen Text ({@link Rechtstext#VERSION}) nicht zugestimmt wurde.
 * Zustimmen geht erst nach dem Lesen bis ganz unten und mit Haken. Ablehnen beendet Minecraft.
 * Im Mod-Menü unter MEHR lässt sich der Text jederzeit nur zum Lesen öffnen.
 */
public class RechtlichesScreen extends Screen {

	private record Bereich(int x, int y, int b, int h, Runnable aktion) {}

	private final Screen eltern;
	private final boolean nurLesen;
	private final List<Bereich> bereiche = new ArrayList<>();
	private int px, py, pb, ph, tx, ty, tb, th;
	private int scroll, inhaltHoehe = 1;
	private boolean gelesen, haken, abgelehnt;

	public RechtlichesScreen(Screen eltern, boolean nurLesen) {
		super(Component.literal("Bambus"));
		this.eltern = eltern;
		this.nurLesen = nurLesen;
		this.gelesen = nurLesen;
	}

	/** Liegt die Zustimmung zum aktuellen Text vor? */
	public static boolean liegtVor() {
		return BambusClient.config.rechtlichZugestimmt >= Rechtstext.VERSION;
	}

	@Override
	protected void init() {
		pb = Math.min(width - 20, 460);
		ph = Math.min(height - 16, 330);
		px = (width - pb) / 2;
		py = (height - ph) / 2;
		tx = px + 14;
		ty = py + 42;
		tb = pb - 34;
		th = ph - 42 - 62;
	}

	@Override
	public void renderBackground(GuiGraphics g, int mx, int my, float delta) {
		g.fill(0, 0, width, height, 0xE0020407);
		Neon.gitter(g, 0, 0, width, height, 24, Farben.alpha(Farben.NEON, 7));
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float delta) {
		bereiche.clear();
		boolean en = Sprache.englisch();
		g.fill(px, py, px + pb, py + ph, Farben.PANEL);
		Neon.leuchtRahmen(g, px, py, pb, ph, Farben.NEON, 0.55f + 0.45f * Farben.puls());
		Neon.ecken(g, px - 5, py - 5, pb + 10, ph + 10, 12, Farben.NEON_CYAN);

		g.drawString(font, Rechtstext.titel(en), px + 14, py + 10, Farben.WEISS, true);
		String unter = nurLesen
				? (en ? "Last updated: " : "Stand: ") + Rechtstext.STAND + datum(en)
				: gelesen ? (en ? "✔ Read to the end." : "✔ Bis zum Ende gelesen.") : Rechtstext.hinweisLesen(en);
		klein(g, unter, px + 14, py + 24, gelesen ? Farben.NEON : Farben.NEON_CYAN);

		// Text mit Rollbalken
		g.fill(tx - 4, ty - 2, tx + tb + 10, ty + th + 2, 0x80000000);
		List<Zeile> zeilen = umbrechen(en, tb - 6);
		inhaltHoehe = 0;
		for (Zeile z : zeilen) inhaltHoehe += z.hoehe;
		int maxScroll = Math.max(0, inhaltHoehe - th);
		scroll = Math.max(0, Math.min(scroll, maxScroll));
		if (scroll >= maxScroll - 2) gelesen = true;
		g.enableScissor(tx - 4, ty, tx + tb + 10, ty + th);
		int y = ty - scroll + 2;
		for (Zeile z : zeilen) {
			if (y + z.hoehe >= ty && y <= ty + th) {
				if (z.punkt) g.drawString(font, "•", z.x - 7, y, Farben.NEON_CYAN, false);
				g.drawString(font, z.text, z.x, y, z.farbe, false);
			}
			y += z.hoehe;
		}
		g.disableScissor();
		if (maxScroll > 0) {   // Rollbalken
			int bx = tx + tb + 4, balken = Math.max(16, th * th / Math.max(1, inhaltHoehe));
			int bpos = ty + (int) ((th - balken) * (scroll / (float) maxScroll));
			g.fill(bx, ty, bx + 3, ty + th, Farben.alpha(Farben.LINIE, 160));
			g.fill(bx, bpos, bx + 3, bpos + balken, Farben.alpha(Farben.NEON, 190));
		}

		// unten: Haken und Knöpfe
		int uy = py + ph - 54;
		if (!nurLesen) {
			int f = !gelesen ? Farben.alpha(Farben.GRAU, 120) : haken ? Farben.NEON : Farben.WEISS;
			g.renderOutline(px + 14, uy + 2, 11, 11, f);
			if (haken) g.fill(px + 17, uy + 5, px + 22, uy + 10, Farben.NEON);
			klein(g, Rechtstext.haken(en), px + 31, uy + 4, f);
			if (gelesen) bereiche.add(new Bereich(px + 12, uy, 260, 15, () -> haken = !haken));
		}
		int ky = py + ph - 30;
		if (nurLesen) {
			knopf(g, mx, my, en ? "CLOSE" : "SCHLIESSEN", px + pb - 124, ky, 110, Farben.NEON, true, this::onClose);
		} else if (abgelehnt) {
			klein(g, Rechtstext.abgelehnt(en), px + 14, ky - 10, 0xFFFF6B8A);
			knopf(g, mx, my, en ? "QUIT MINECRAFT" : "MINECRAFT BEENDEN", px + pb - 270, ky, 140, 0xFFFF6B8A, true,
					() -> minecraft.stop());
			knopf(g, mx, my, en ? "BACK" : "ZURÜCK", px + pb - 124, ky, 110, Farben.GRAU, true, () -> abgelehnt = false);
		} else {
			knopf(g, mx, my, Rechtstext.ablehnen(en), px + pb - 250, ky, 110, 0xFFFF2BD6, true, () -> abgelehnt = true);
			knopf(g, mx, my, Rechtstext.zustimmen(en), px + pb - 124, ky, 110, Farben.NEON, haken, this::zustimmen);
		}
		super.render(g, mx, my, delta);
	}

	private String datum(boolean en) {
		String d = BambusClient.config.rechtlichDatum;
		if (d == null || d.isEmpty()) return "";
		return (en ? "  ·  agreed on " : "  ·  zugestimmt am ") + d;
	}

	private void zustimmen() {
		if (!haken) return;
		BambusClient.config.rechtlichZugestimmt = Rechtstext.VERSION;
		BambusClient.config.rechtlichDatum = LocalDate.now().toString();
		BambusClient.config.speichern();
		minecraft.setScreen(eltern);
	}

	private void klein(GuiGraphics g, String text, int x, int y, int farbe) {
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(0.75f, 0.75f);
		g.drawString(font, text, 0, 0, farbe, false);
		g.pose().popMatrix();
	}

	private void knopf(GuiGraphics g, int mx, int my, String text, int x, int y, int b, int farbe, boolean an, Runnable r) {
		boolean ueber = an && Neon.ueber(mx, my, x, y, b, 20);
		int f = an ? farbe : Farben.alpha(Farben.GRAU, 110);
		g.fill(x, y, x + b, y + 20, ueber ? Farben.KARTE_HOVER : Farben.KARTE);
		g.renderOutline(x, y, b, 20, ueber ? f : Farben.alpha(f, 170));
		int w = font.width(text);
		g.drawString(font, text, x + (b - w) / 2, y + 6, f, false);
		if (an) bereiche.add(new Bereich(x, y, b, 20, r));
	}

	// ------------------------------------------------------------ Zeilenumbruch

	private record Zeile(String text, int x, int farbe, int hoehe, boolean punkt) {}

	/** Text in Zeilen, die in die Breite passen – Überschriften in Grün, Aufzählungen eingerückt. */
	private List<Zeile> umbrechen(boolean en, int breite) {
		List<Zeile> aus = new ArrayList<>();
		for (String[] a : Rechtstext.abschnitte(en)) {
			aus.add(new Zeile(a[0].toUpperCase(), tx, Farben.NEON, 13, false));
			for (String absatz : a[1].split("\n")) {
				boolean punkt = absatz.startsWith("• ");
				String rest = punkt ? absatz.substring(2) : absatz;
				int x = punkt ? tx + 9 : tx;
				boolean erste = true;
				for (String zeile : woerter(rest, breite - (x - tx))) {
					aus.add(new Zeile(zeile, x, 0xFFD5DEEA, 10, punkt && erste));
					erste = false;
				}
				aus.add(new Zeile("", tx, 0, 2, false));
			}
			aus.add(new Zeile("", tx, 0, 6, false));
		}
		return aus;
	}

	private List<String> woerter(String text, int breite) {
		List<String> aus = new ArrayList<>();
		StringBuilder zeile = new StringBuilder();
		for (String wort : text.split(" ")) {
			String probe = zeile.length() == 0 ? wort : zeile + " " + wort;
			if (font.width(probe) > breite && zeile.length() > 0) {
				aus.add(zeile.toString());
				zeile = new StringBuilder(wort);
			} else {
				zeile = new StringBuilder(probe);
			}
		}
		if (zeile.length() > 0) aus.add(zeile.toString());
		return aus;
	}

	// ------------------------------------------------------------ Eingaben

	@Override
	public boolean mouseClicked(MouseButtonEvent e, boolean doppelt) {
		if (e.button() == InputConstants.MOUSE_BUTTON_LEFT) {
			for (int i = bereiche.size() - 1; i >= 0; i--) {
				Bereich b = bereiche.get(i);
				if (Neon.ueber(e.x(), e.y(), b.x(), b.y(), b.b(), b.h())) {
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
		scroll -= (int) (vertikal * 20);
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent e) {
		if (e.key() == InputConstants.KEY_DOWN || e.key() == InputConstants.KEY_PAGEDOWN) {
			scroll += e.key() == InputConstants.KEY_DOWN ? 20 : th - 20;
			return true;
		}
		if (e.key() == InputConstants.KEY_UP || e.key() == InputConstants.KEY_PAGEUP) {
			scroll -= e.key() == InputConstants.KEY_UP ? 20 : th - 20;
			return true;
		}
		if (e.key() == InputConstants.KEY_ESCAPE) {
			if (nurLesen) onClose();
			return true;   // ohne Zustimmung lässt sich das Fenster nicht wegdrücken
		}
		return super.keyPressed(e);
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return nurLesen;
	}

	@Override
	public void onClose() {
		if (nurLesen) minecraft.setScreen(eltern);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
