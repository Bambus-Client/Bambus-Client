package de.bambus.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

/** Fassung für Minecraft 26.x (siehe src/main/java/de/bambus/client/Kompat.java). */
public final class Kompat {

	private Kompat() {}

	public static void meldung(Player spieler, Component text, boolean ueberHotbar) {
		if (ueberHotbar) {
			spieler.sendOverlayMessage(text);
		} else {
			spieler.sendSystemMessage(text);
		}
	}

	/** Seit 26.1 läuft die Zeit über Weltuhren – die Tageszeit ist die der Oberwelt. */
	public static long tageszeit(Level welt) {
		return welt.getOverworldClockTime();
	}

	// ------------------------------------------------------------- Gegenstände

	/** Seit 26.x bekommen Gegenstände ihre Eigenschaften erst mit einer Welt – vorher gibt es keinen ItemStack. */
	public static boolean itemsBereit() {
		return Items.STONE.builtInRegistryHolder().areComponentsBound();
	}

	public static void item(GuiGraphicsExtractor g, Item item, int x, int y) {
		if (itemsBereit()) {
			g.item(new ItemStack(item), x, y);
		} else {
			bild(g, item, x, y);
		}
	}

	public static void item(GuiGraphicsExtractor g, Font font, Item item, int anzahl, int x, int y) {
		if (itemsBereit()) {
			ItemStack stapel = new ItemStack(item, anzahl);
			g.item(stapel, x, y);
			g.itemDecorations(font, stapel, x, y);
			return;
		}
		bild(g, item, x, y);
		if (anzahl > 1) {
			String t = String.valueOf(anzahl);
			g.text(font, t, x + 17 - font.width(t), y + 9, 0xFFFFFFFF, true);
		}
	}

	/** Gefundene Texturen je Gegenstand (null = keine passende). */
	private static final Map<Item, Identifier> BILDER = new HashMap<>();

	/** Flaches Bild aus der Textur des Gegenstands (im Hauptmenü, bevor es eine Welt gibt). */
	private static void bild(GuiGraphicsExtractor g, Item item, int x, int y) {
		Identifier bild = BILDER.computeIfAbsent(item, Kompat::sucheBild);
		if (bild != null) {
			g.blit(RenderPipelines.GUI_TEXTURED, bild, x, y, 0f, 0f, 16, 16, 16, 16);
		}
	}

	private static Identifier sucheBild(Item item) {
		Identifier id = BuiltInRegistries.ITEM.getKey(item);
		String[] kandidaten = {"textures/item/" + id.getPath() + ".png", "textures/block/" + id.getPath() + "_side.png",
				"textures/block/" + id.getPath() + "_front.png", "textures/block/" + id.getPath() + ".png"};
		for (String k : kandidaten) {
			Identifier t = Identifier.fromNamespaceAndPath(id.getNamespace(), k);
			if (Minecraft.getInstance().getResourceManager().getResource(t).isPresent()) {
				return t;
			}
		}
		return null;
	}
}
