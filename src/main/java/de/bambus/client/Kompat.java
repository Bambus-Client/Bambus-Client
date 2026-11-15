package de.bambus.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Kleine Helfer für Stellen, an denen sich Minecraft zwischen den Versionen unterscheidet.
 * Die Fassung für Minecraft 26.x liegt in versionen/neu/src/26/java und ersetzt diese beim Bauen.
 */
public final class Kompat {

	private Kompat() {}

	/** Meldung an den Spieler: über der Hotbar (true) oder im Chat (false). */
	public static void meldung(Player spieler, Component text, boolean ueberHotbar) {
		spieler.displayClientMessage(text, ueberHotbar);
	}

	/** Tageszeit der Welt in Ticks (0 = Morgen, 24000 = ein Tag). */
	public static long tageszeit(Level welt) {
		return welt.getDayTime();
	}

	/**
	 * Gegenstands-Symbol zeichnen (16 × 16). Ab Minecraft 26.x gibt es Gegenstände erst in einer Welt –
	 * im Hauptmenü zeichnet die 26er-Fassung dann die Textur des Gegenstands.
	 */
	public static void item(GuiGraphics g, Item item, int x, int y) {
		g.renderItem(new ItemStack(item), x, y);
	}

	/** Wie {@link #item(GuiGraphics, Item, int, int)}, mit Anzahl unten rechts. */
	public static void item(GuiGraphics g, Font font, Item item, int anzahl, int x, int y) {
		ItemStack stapel = new ItemStack(item, anzahl);
		g.renderItem(stapel, x, y);
		g.renderItemDecorations(font, stapel, x, y);
	}
}
