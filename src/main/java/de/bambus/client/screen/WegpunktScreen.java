package de.bambus.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import de.bambus.client.BambusClient;
import de.bambus.client.BambusConfig;
import de.bambus.client.Farben;
import de.bambus.client.Hinweise;
import de.bambus.client.Wegpunkte;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.List;

/**
 * WEGPUNKTE: Orte merken, benennen, färben, ausblenden und löschen.
 * Angezeigt werden sie im Kompass-Balken – mit Namen und Entfernung, wenn man in ihre Richtung schaut.
 */
public class WegpunktScreen extends Screen {

	private record Bereich(int x, int y, int b, int h, Runnable aktion) {}

	private static final int ZEILE = 22;

	private final Screen eltern;
	private final List<Bereich> bereiche = new ArrayList<>();
	private EditBox name;
	private int px, py, pb, ph, ly, lh;
	private int scroll;

	public WegpunktScreen(Screen eltern) {
		super(Component.literal("Wegpunkte"));
		this.eltern = eltern;
	}

	@Override
	protected void init() {
		pb = Math.min(width - 20, 420);
		ph = Math.min(height - 20, 300);
		px = (width - pb) / 2;
		py = (height - ph) / 2;
		String alt = name == null ? "" : name.getValue();
		name = new EditBox(font, px + 14, py + 44, pb - 150, 14, Component.literal("Name"));
		name.setHint(Component.literal("§8Name, z. B. Basis"));
		name.setMaxLength(24);
		name.setBordered(false);
		name.setTextColor(Farben.WEISS);
		name.setValue(alt);
		addRenderableWidget(name);
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
		g.fill(px, py, px + pb, py + ph, Farben.PANEL);
		Neon.gitter(g, px, py, pb, ph, 16, Farben.alpha(Farben.NEON, 6));
		Neon.leuchtRahmen(g, px, py, pb, ph, Farben.NEON, 0.55f + 0.45f * Farben.puls());
		Neon.ecken(g, px - 5, py - 5, pb + 10, ph + 10, 12, Farben.NEON);

		Symbole.zeichne(g, "karte", px + 12, py + 10, 2, Farben.NEON);
		Neon.leuchtText(g, font, "WEGPUNKTE", px + 34, py + 10, 1f, Farben.NEON);
		String ort = Wegpunkte.ort(minecraft);
		String wo = ort == null ? "keine Welt" : (ort.startsWith("sp:") ? "Welt „" + ort.substring(3) + "“" : ort)
				+ "  ·  " + Wegpunkte.dimension(minecraft);
		Neon.text(g, font, wo, px + 34, py + 22, 0.6f, Farben.GRAU, false);

		// Eingabe + Setzen
		g.fill(px + 10, py + 40, px + pb - 132, py + 58, 0xFF0B1220);
		g.renderOutline(px + 10, py + 40, pb - 142, 18, name.isFocused() ? Farben.NEON : Farben.LINIE);
		knopf(g, mx, my, "+ HIER SETZEN", px + pb - 126, py + 40, 116, 18, Farben.NEON, this::setzen);

		// Liste
		ly = py + 68;
		lh = ph - 68 - 34;
		List<Wegpunkte.Punkt> liste = Wegpunkte.hier(minecraft);
		int maxScroll = Math.max(0, liste.size() * ZEILE - lh);
		scroll = Math.max(0, Math.min(scroll, maxScroll));
		g.enableScissor(px + 6, ly, px + pb - 6, ly + lh);
		if (liste.isEmpty()) {
			Neon.text(g, font, "Noch keine Wegpunkte hier.", px + 14, ly + 10, 0.75f, Farben.GRAU, false);
			Neon.text(g, font, "Name eintippen (oder leer lassen) und HIER SETZEN drücken.", px + 14, ly + 24, 0.6f,
					Farben.GRAU, false);
			Neon.text(g, font, "Schneller: Taste N setzt sofort einen Wegpunkt.", px + 14, ly + 36, 0.6f, Farben.GRAU, false);
		}
		int y = ly - scroll;
		for (Wegpunkte.Punkt p : liste) {
			zeile(g, mx, my, p, y);
			y += ZEILE;
		}
		g.disableScissor();

		// Fuß: Kompass-Hinweis
		int fy = py + ph - 28;
		boolean kompassAn = BambusClient.config.modul("kompass", 160, 2, false).an;
		boolean imKompass = BambusClient.config.wegpunkteImKompass;
		knopf(g, mx, my, imKompass ? "IM KOMPASS: AN" : "IM KOMPASS: AUS", px + 10, fy, 120, 18,
				imKompass ? Farben.NEON : Farben.GRAU, () -> {
					BambusClient.config.wegpunkteImKompass = !BambusClient.config.wegpunkteImKompass;
					BambusClient.config.speichern();
				});
		if (!kompassAn) {
			knopf(g, mx, my, "KOMPASS-BALKEN EINSCHALTEN", px + 136, fy, 170, 18, Farben.NEON, () -> {
				BambusConfig.ModulPos k = BambusClient.config.modul("kompass", 160, 2, false);
				k.an = true;
				BambusClient.config.speichern();
				Hinweise.zeige("richtung", "Kompass-Balken an", "Wegpunkte erscheinen oben im Bild", Farben.NEON);
			});
		} else {
			Neon.text(g, font, "Schau in Richtung eines Wegpunkts – oben steht dann die Entfernung.", px + 138, fy + 6,
					0.55f, Farben.GRAU, false);
		}
		super.render(g, mx, my, delta);
	}

	private void zeile(GuiGraphics g, int mx, int my, Wegpunkte.Punkt p, int y) {
		int x = px + 10, b = pb - 20, h = ZEILE - 3;
		boolean ueber = Neon.ueber(mx, my, x, y, b, h) && Neon.ueber(mx, my, px, ly, pb, lh);
		g.fill(x, y, x + b, y + h, ueber ? Farben.KARTE_HOVER : Farben.KARTE);
		int farbe = 0xFF000000 | p.farbe;
		// Farbfeld – Klick wechselt die Farbe
		g.fill(x + 5, y + 4, x + 16, y + 15, farbe);
		bereich(x + 3, y + 2, 15, 15, () -> Wegpunkte.naechsteFarbe(p));
		Neon.text(g, font, p.name, x + 24, y + 3, 0.8f, p.sichtbar ? Farben.WEISS : Farben.GRAU, false);
		Neon.text(g, font, p.x + " / " + p.y + " / " + p.z, x + 24, y + 12, 0.55f, Farben.GRAU, false);
		String ab = Wegpunkte.abstandText(Wegpunkte.abstand(minecraft, p));
		int abb = (int) (font.width(ab) * 0.7f);
		Neon.text(g, font, ab, x + b - 74 - abb, y + 6, 0.7f, farbe, false);

		// Umbenennen (Text aus dem Feld oben)
		String neu = name.getValue().trim();
		if (!neu.isEmpty() && ueber) {
			knopfKlein(g, mx, my, "✎", x + b - 64, y + 3, () -> {
				Wegpunkte.umbenennen(p, neu);
				name.setValue("");
			});
		}
		knopfKlein(g, mx, my, p.sichtbar ? "◉" : "○", x + b - 42, y + 3, () -> Wegpunkte.umschalten(p));
		knopfKlein(g, mx, my, "✕", x + b - 20, y + 3, () -> Wegpunkte.entfernen(p));
	}

	private void setzen() {
		if (minecraft.player == null) return;
		Wegpunkte.Punkt p = Wegpunkte.hierSetzen(minecraft, name.getValue());
		if (p != null) {
			name.setValue("");
			Hinweise.zeige("karte", "Wegpunkt gesetzt", p.name + " · " + p.x + " / " + p.y + " / " + p.z,
					0xFF000000 | p.farbe);
		}
	}

	private void knopf(GuiGraphics g, int mx, int my, String text, int x, int y, int b, int h, int farbe, Runnable r) {
		boolean ueber = Neon.ueber(mx, my, x, y, b, h);
		g.fill(x, y, x + b, y + h, ueber ? Farben.KARTE_HOVER : Farben.KARTE);
		g.renderOutline(x, y, b, h, ueber ? farbe : Farben.alpha(farbe, 150));
		int tb = (int) (font.width(text) * 0.7f);
		Neon.text(g, font, text, x + (b - tb) / 2, y + (h - 6) / 2, 0.7f, farbe, false);
		bereiche.add(new Bereich(x, y, b, h, r));
	}

	private void knopfKlein(GuiGraphics g, int mx, int my, String zeichen, int x, int y, Runnable r) {
		boolean ueber = Neon.ueber(mx, my, x, y, 16, 13) && Neon.ueber(mx, my, px, ly, pb, lh);
		g.drawString(font, zeichen, x + 4, y + 2, ueber ? Farben.NEON : Farben.GRAU, false);
		bereich(x, y, 16, 13, r);
	}

	/** Klickfläche, die nur innerhalb der sichtbaren Liste zählt. */
	private void bereich(int x, int y, int b, int h, Runnable r) {
		int oben = Math.max(y, ly), unten = Math.min(y + h, ly + lh);
		if (unten > oben) bereiche.add(new Bereich(x, oben, b, unten - oben, r));
	}

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
		if (Neon.ueber(mx, my, px, ly, pb, lh)) {
			scroll -= (int) (vertikal * 16);
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
		if ((e.key() == InputConstants.KEY_RETURN || e.key() == InputConstants.KEY_NUMPADENTER) && name.isFocused()) {
			setzen();
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
