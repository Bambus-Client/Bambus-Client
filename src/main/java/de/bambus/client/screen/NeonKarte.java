package de.bambus.client.screen;

import de.bambus.client.BambusClient;
import de.bambus.client.Farben;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;

/**
 * Klickbare Karte im Bambus-Look: oben ein Motiv (z. B. der Spieler-Avatar), darunter Titel und Untertitel.
 * Benutzt im Escape-Menü (Garderobe) und im Auswahl-Bildschirm (Rechts-Shift).
 */
public class NeonKarte extends AbstractButton {

	/** Zeichnet das Motiv in den Bereich (x1, y1) – (x2, y2); hover = 0..1. */
	@FunctionalInterface
	public interface Motiv {
		void zeichne(GuiGraphics g, int x1, int y1, int x2, int y2, int mx, int my, float hover);
	}

	/** Dein Spieler-Avatar, der der Maus folgt. */
	public static final Motiv AVATAR = (g, x1, y1, x2, y2, mx, my, hover) -> {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null) {
			InventoryScreen.renderEntityInInventoryFollowsMouse(g, x1, y1, x2, y2,
					(int) ((y2 - y1) * 0.42f), 0.0625f, mx, my, mc.player);
		}
	};

	private static final int FUSS = 30;   // Platz für Titel und Untertitel

	private final String titel;
	private final String untertitel;
	private final Motiv motiv;
	private final Runnable aktion;

	public NeonKarte(int x, int y, int b, int h, String titel, String untertitel, Motiv motiv, Runnable aktion) {
		super(x, y, b, h, Component.literal(titel));
		this.titel = titel;
		this.untertitel = untertitel;
		this.motiv = motiv;
		this.aktion = aktion;
	}

	@Override
	public void onPress(InputWithModifiers eingabe) {
		aktion.run();
	}

	@Override
	protected void renderContents(GuiGraphics g, int mx, int my, float delta) {
		MenueStil.anwenden();
		int x = getX(), y = getY(), b = getWidth(), h = getHeight();
		boolean hover = isHoveredOrFocused();
		float k = BambusClient.config.menueAnimation
				? Anim.zu("neon-karte:" + titel, hover ? 1f : 0f, Anim.HOVER) : (hover ? 1f : 0f);

		// Fläche
		g.fillGradient(x, y, x + b, y + h, 0xF00B1626, 0xF0050A10);
		Neon.gitter(g, x, y, b, h, 12, Farben.alpha(Farben.NEON_CYAN, 12 + (int) (10 * k)));
		if (k > 0.02f) {
			Neon.leuchtRahmen(g, x, y, b, h, Farben.alpha(Farben.NEON, (int) (255 * k)), k);
		}
		Neon.ecken(g, x, y, b, h, 8, Anim.mische(Farben.NEON_CYAN, Farben.NEON, k));

		// Motiv
		int motivUnten = y + h - FUSS;
		motiv.zeichne(g, x + 4, y + 4, x + b - 4, motivUnten, mx, my, k);

		// Titel und Untertitel
		Font font = Minecraft.getInstance().font;
		g.fill(x + 6, motivUnten, x + b - 6, motivUnten + 1, Farben.alpha(Farben.NEON, 90 + (int) (100 * k)));
		float groesse = font.width(titel) > b - 8 ? 0.75f : 1f;   // schmale Karten: kleinerer Titel
		int tb = (int) (font.width(titel) * groesse);
		Neon.leuchtText(g, font, titel, x + (b - tb) / 2, motivUnten + (groesse < 1f ? 7 : 6), groesse, Anim.mische(Farben.NEON_CYAN, Farben.NEON, k));
		int ub = (int) (font.width(untertitel) * 0.6f);
		Neon.text(g, font, untertitel, x + (b - ub) / 2, motivUnten + 18, 0.6f, Farben.GRAU, false);
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput ausgabe) {
		defaultButtonNarrationText(ausgabe);
	}
}
