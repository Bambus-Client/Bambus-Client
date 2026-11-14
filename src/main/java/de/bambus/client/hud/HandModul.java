package de.bambus.client.hud;

import de.bambus.client.Farben;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

/** Das Item in der Hand mit Haltbarkeit bzw. Anzahl (früher Teil der Rüstungsanzeige). */
public class HandModul extends HudModul {

	private final Option haltbarkeit = option("haltbarkeit", "Haltbarkeit als Zahl", true);
	private final Option zweiteHand = option("zweitehand", "Auch Zweithand (Schild, Totem …)", false);

	public HandModul() {
		// x = 10000: ganz rechts, direkt unter der Rüstung
		super("hand", "Item in der Hand", 10000, 74, true, "Haltbarkeit bzw. Anzahl deines Items", "PVP");
	}

	private ItemStack[] items(Minecraft mc) {
		if (mc.player == null) return new ItemStack[0];
		ItemStack haupt = mc.player.getMainHandItem(), neben = mc.player.getOffhandItem();
		if (zweiteHand.an() && !neben.isEmpty()) {
			return haupt.isEmpty() ? new ItemStack[]{neben} : new ItemStack[]{haupt, neben};
		}
		return haupt.isEmpty() ? new ItemStack[0] : new ItemStack[]{haupt};
	}

	private String text(ItemStack s) {
		if (s.isDamageableItem()) {
			return haltbarkeit.an() ? String.valueOf(s.getMaxDamage() - s.getDamageValue()) : "";
		}
		return s.getCount() > 1 ? String.valueOf(s.getCount()) : "";
	}

	@Override
	public int breite(Minecraft mc, boolean editor) {
		ItemStack[] liste = items(mc);
		if (liste.length == 0) return editor ? mc.font.width("Hand") + 10 : 0;
		int b = 0;
		for (ItemStack s : liste) b = Math.max(b, mc.font.width(text(s)));
		return 20 + b;
	}

	@Override
	public int hoehe(Minecraft mc, boolean editor) {
		int n = items(mc).length;
		return n == 0 ? (editor ? 15 : 0) : n * 17;
	}

	@Override
	public void zeichne(GuiGraphics g, Minecraft mc, int x, int y, boolean editor) {
		ItemStack[] liste = items(mc);
		if (liste.length == 0) {
			panel(g, x, y, breite(mc, true), 15);
			g.drawString(mc.font, "Hand", x + 6, y + 4, Farben.GRAU, true);
			return;
		}
		for (int i = 0; i < liste.length; i++) {
			ItemStack s = liste[i];
			int zy = y + i * 17;
			g.renderItem(s, x, zy);
			g.renderItemDecorations(mc.font, s, x, zy);
			g.drawString(mc.font, text(s), x + 19, zy + 4, RuestungModul.farbe(s), true);
		}
	}
}
