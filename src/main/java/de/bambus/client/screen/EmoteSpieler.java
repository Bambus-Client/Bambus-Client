package de.bambus.client.screen;

import de.bambus.client.emote.Emote;
import de.bambus.client.emote.Emotes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

/** Der eigene Spieler (mit Skin, Cape und Kosmetik), der ein bestimmtes Emote vorführt – für Rad und Garderobe. */
public final class EmoteSpieler {

	private EmoteSpieler() {}

	/**
	 * Zeichnet den Spieler in den Bereich (x1, y1)–(x2, y2), von vorn, mit dem Emote zum Zeitpunkt t.
	 * @param groesse  Maßstab wie im Inventar: Spielerhöhe ≈ 1,8 · groesse
	 */
	public static void zeichne(GuiGraphics g, Emote e, float t, int x1, int y1, int x2, int y2, int groesse) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return;
		Emotes.erzwinge(e, t);   // gilt nur für dieses eine Modell
		try {
			InventoryScreen.renderEntityInInventoryFollowsMouse(g, x1, y1, x2, y2, groesse, 0.0625f,
					(x1 + x2) / 2, y1 + (y2 - y1) / 3, mc.player);
		} finally {
			Emotes.erzwinge(null, 0);
		}
	}
}
