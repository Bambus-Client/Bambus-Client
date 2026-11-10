package de.bambus.client.screen;

import de.bambus.client.BambusClient;
import de.bambus.client.Farben;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;

/**
 * Karte im Escape-Menü: dein Spieler-Avatar im Neon-Rahmen, darunter „GARDEROBE“.
 * Ein Klick öffnet die Garderobe.
 */
public class GarderobeKnopf extends AbstractButton {

	public static final int BREITE = 96;
	private static final int MAX_HOEHE = 140;
	private static final int FUSS = 30;   // Platz für die Beschriftung unter dem Avatar

	private final Screen eltern;

	private GarderobeKnopf(Screen eltern, int x, int y, int hoehe) {
		super(x, y, BREITE, hoehe, Component.literal("Garderobe"));
		this.eltern = eltern;
	}

	/** Links im Escape-Menü, senkrecht mittig. */
	public static GarderobeKnopf fuer(Screen eltern, int bildHoehe) {
		int hoehe = Math.max(70, Math.min(MAX_HOEHE, bildHoehe - 40));
		return new GarderobeKnopf(eltern, 10, (bildHoehe - hoehe) / 2, hoehe);
	}

	@Override
	public void onPress(InputWithModifiers eingabe) {
		Minecraft.getInstance().setScreen(new GarderobeScreen(eltern));
	}

	@Override
	protected void renderContents(GuiGraphics g, int mx, int my, float delta) {
		Farben.themaAnwenden(BambusClient.config.thema);
		int x = getX(), y = getY(), b = getWidth(), h = getHeight();
		boolean hover = isHoveredOrFocused();
		float k = BambusClient.config.menueAnimation ? Anim.zu("garderobe-knopf", hover ? 1f : 0f, 14f) : (hover ? 1f : 0f);

		// Fläche im Client-Look
		g.fillGradient(x, y, x + b, y + h, 0xF00B1626, 0xF0050A10);
		Neon.gitter(g, x, y, b, h, 12, Farben.alpha(Farben.NEON_CYAN, 12 + (int) (10 * k)));
		if (k > 0.02f) {
			Neon.leuchtRahmen(g, x, y, b, h, Farben.alpha(Farben.NEON, (int) (255 * k)), k);
		}
		Neon.ecken(g, x, y, b, h, 8, Anim.mische(Farben.NEON_CYAN, Farben.NEON, k));

		// Avatar
		Minecraft mc = Minecraft.getInstance();
		int modellUnten = y + h - FUSS;
		if (mc.player != null) {
			InventoryScreen.renderEntityInInventoryFollowsMouse(g, x + 4, y + 4, x + b - 4, modellUnten,
					(int) ((h - FUSS) * 0.40f), 0.0625f, mx, my, mc.player);
		}

		// Beschriftung
		g.fill(x + 6, modellUnten, x + b - 6, modellUnten + 1, Farben.alpha(Farben.NEON, 90 + (int) (100 * k)));
		var font = mc.font;
		String titel = "GARDEROBE";
		int tb = font.width(titel);
		Neon.leuchtText(g, font, titel, x + (b - tb) / 2, modellUnten + 6, 1f, Anim.mische(Farben.NEON_CYAN, Farben.NEON, k));
		String unter = "KOSMETIK ANPASSEN";
		int ub = (int) (font.width(unter) * 0.6f);
		Neon.text(g, font, unter, x + (b - ub) / 2, modellUnten + 18, 0.6f, Farben.GRAU, false);
	}

	@Override
	protected void updateWidgetNarration(NarrationElementOutput ausgabe) {
		defaultButtonNarrationText(ausgabe);
	}
}
