package de.bambus.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

/**
 * Inventar sortieren: ein Knopf neben Inventar, Truhe und Shulkerkiste.
 *
 * Es werden nur ganz normale Klicks geschickt – genau die, die man auch mit der Maus machen würde
 * (aufnehmen, ablegen). Deshalb geht nichts verloren, und Server sehen nichts Ungewöhnliches.
 * Die Hotbar und die Rüstung bleiben unberührt. Pro Tick gehen nur wenige Klicks raus.
 */
public final class Sortieren {

	public static final String[] REGELN = {"art", "name", "menge"};
	public static final String[] REGEL_NAMEN = {"nach Art", "nach Name", "nach Menge"};
	private static final int KLICKS_PRO_TICK = 6;

	private static final Deque<Integer> klicks = new ArrayDeque<>();
	private static int fensterId = -1;

	private Sortieren() {}

	// ------------------------------------------------------------ Knopf

	/** Beim Öffnen eines Bildschirms: passt er, kommt ein kleiner Sortier-Knopf an die Seite. */
	public static void knopfAnbringen(Minecraft mc, Screen screen, int breite, int hoehe) {
		if (!(screen instanceof AbstractContainerScreen<?> acs)) return;
		AbstractContainerMenu menu = acs.getMenu();
		int fensterHoehe;
		if (menu instanceof ChestMenu chest) fensterHoehe = 114 + chest.getRowCount() * 18;
		else if (menu instanceof ShulkerBoxMenu || menu instanceof InventoryMenu) fensterHoehe = 166;
		else return;
		int links = (breite - 176) / 2, oben = (hoehe - fensterHoehe) / 2;
		Button sortieren = Button.builder(Component.literal(Sprache.t("⇅")), b -> starte(mc))
				.bounds(links + 178, oben + 2, 16, 16)
				.tooltip(Tooltip.create(Component.literal(Sprache.t("Sortieren (" + regelName() + ")\n§7Regel ändern: Mod-Menü → MEHR"))))
				.build();
		net.fabricmc.fabric.api.client.screen.v1.Screens.getButtons(screen).add(sortieren);
	}

	public static String regelName() {
		for (int i = 0; i < REGELN.length; i++) if (REGELN[i].equals(BambusClient.config.sortierRegel)) return REGEL_NAMEN[i];
		return REGEL_NAMEN[0];
	}

	/** Nächste Sortierregel (für das Mod-Menü). */
	public static void naechsteRegel() {
		int i = 0;
		for (int k = 0; k < REGELN.length; k++) if (REGELN[k].equals(BambusClient.config.sortierRegel)) i = k;
		BambusClient.config.sortierRegel = REGELN[(i + 1) % REGELN.length];
		BambusClient.config.speichern();
	}

	// ------------------------------------------------------------ Planen

	/** Welche Slots gehören sortiert? Bei Truhen die Truhe, im Inventar nur die 27 Plätze über der Hotbar. */
	private static int[] bereich(AbstractContainerMenu menu) {
		if (menu instanceof ChestMenu chest) return reihe(0, chest.getRowCount() * 9);
		if (menu instanceof ShulkerBoxMenu) return reihe(0, 27);
		if (menu instanceof InventoryMenu) return reihe(9, 36);
		return null;
	}

	private static int[] reihe(int von, int bis) {
		int[] r = new int[bis - von];
		for (int i = 0; i < r.length; i++) r[i] = von + i;
		return r;
	}

	public static void starte(Minecraft mc) {
		if (mc.player == null || mc.gameMode == null || !klicks.isEmpty()) return;
		AbstractContainerMenu menu = mc.player.containerMenu;
		int[] slots = bereich(menu);
		if (slots == null) return;
		if (!menu.getCarried().isEmpty()) {
			Hinweise.zeige("alle", "Erst ablegen", "Du hältst noch etwas mit der Maus", Farben.NEON_CYAN);
			return;
		}
		ItemStack[] sim = new ItemStack[slots.length];
		for (int i = 0; i < slots.length; i++) sim[i] = menu.slots.get(slots[i]).getItem().copy();

		List<Integer> plan = new ArrayList<>();
		zusammenlegen(sim, plan);
		ordnen(sim, plan);
		if (plan.isEmpty()) {
			Hinweise.zeige("alle", "Schon sortiert", regelName(), Farben.NEON);
			return;
		}
		fensterId = menu.containerId;
		for (int k : plan) klicks.add(slots[k]);
	}

	/** Halbe Stapel desselben Gegenstands zusammenlegen (wie mit der Maus: aufnehmen, drauflegen, Rest zurück). */
	private static void zusammenlegen(ItemStack[] sim, List<Integer> plan) {
		for (int a = 0; a < sim.length; a++) {
			if (sim[a].isEmpty() || sim[a].getCount() >= sim[a].getMaxStackSize()) continue;
			for (int b = a + 1; b < sim.length && sim[a].getCount() < sim[a].getMaxStackSize(); b++) {
				if (sim[b].isEmpty() || !ItemStack.isSameItemSameComponents(sim[a], sim[b])) continue;
				int passt = sim[a].getMaxStackSize() - sim[a].getCount();
				int mehr = Math.min(passt, sim[b].getCount());
				plan.add(b);                       // aufnehmen
				plan.add(a);                       // drauflegen
				sim[a].grow(mehr);
				sim[b].shrink(mehr);
				if (!sim[b].isEmpty()) plan.add(b); // Rest zurück auf den (jetzt leeren) Platz
				if (sim[b].isEmpty()) sim[b] = ItemStack.EMPTY;
			}
		}
	}

	/** Sortieren durch Tauschen. Gleiche Gegenstände werden nie getauscht (sie würden sonst verschmelzen). */
	private static void ordnen(ItemStack[] sim, List<Integer> plan) {
		List<ItemStack> soll = new ArrayList<>();
		for (ItemStack s : sim) if (!s.isEmpty()) soll.add(s);
		soll.sort(vergleich());
		for (int i = 0; i < sim.length; i++) {
			ItemStack ziel = i < soll.size() ? soll.get(i) : ItemStack.EMPTY;
			if (gleich(sim[i], ziel)) continue;
			int j = -1;
			for (int k = i + 1; k < sim.length; k++) {
				if (gleich(sim[k], ziel)) {
					j = k;
					break;
				}
			}
			if (j < 0) continue;
			if (sim[i].isEmpty()) {               // j nach i schieben
				plan.add(j);
				plan.add(i);
			} else if (sim[j].isEmpty()) {
				plan.add(i);
				plan.add(j);
			} else {                              // tauschen: i aufnehmen, auf j (Tausch), zurück auf i
				plan.add(i);
				plan.add(j);
				plan.add(i);
			}
			ItemStack t = sim[i];
			sim[i] = sim[j];
			sim[j] = t;
		}
	}

	/**
	 * Gleich im Sinne der Sortierung: derselbe Gegenstand (Menge egal) oder beide leer.
	 * Wichtig: Zwei Stapel desselben Gegenstands dürfen nie „getauscht“ werden – Minecraft würde sie
	 * zusammenlegen statt tauschen. So tauschen wir immer nur verschiedene Dinge oder mit leeren Plätzen.
	 */
	private static boolean gleich(ItemStack a, ItemStack b) {
		if (a.isEmpty() || b.isEmpty()) return a.isEmpty() && b.isEmpty();
		return ItemStack.isSameItemSameComponents(a, b);
	}

	private static Comparator<ItemStack> vergleich() {
		Comparator<ItemStack> nachName = Comparator.comparing(s -> s.getHoverName().getString().toLowerCase(Locale.ROOT));
		Comparator<ItemStack> nachId = Comparator.comparing(s -> BuiltInRegistries.ITEM.getKey(s.getItem()).toString());
		Comparator<ItemStack> nachMenge = Comparator.comparingInt(ItemStack::getCount).reversed();
		switch (BambusClient.config.sortierRegel) {
			case "name":
				return nachName.thenComparing(nachMenge);
			case "menge":
				return nachMenge.thenComparing(nachName);
			default:
				return Comparator.comparingInt(Sortieren::art).thenComparing(nachId).thenComparing(nachMenge);
		}
	}

	/** Grobe Art: Blöcke, Werkzeuge und Waffen, Essen, alles andere. */
	private static int art(ItemStack s) {
		if (s.getItem() instanceof BlockItem) return 0;
		if (s.isDamageableItem()) return 1;
		if (s.has(DataComponents.FOOD)) return 2;
		return 3;
	}

	// ------------------------------------------------------------ Ausführen

	/** Jeden Tick ein paar Klicks. Wird das Fenster geschlossen, hört es sofort auf. */
	public static void tick(Minecraft mc) {
		if (klicks.isEmpty()) return;
		if (mc.player == null || mc.gameMode == null || mc.player.containerMenu.containerId != fensterId) {
			klicks.clear();
			return;
		}
		for (int i = 0; i < KLICKS_PRO_TICK && !klicks.isEmpty(); i++) {
			mc.gameMode.handleInventoryMouseClick(fensterId, klicks.poll(), 0, ClickType.PICKUP, mc.player);
		}
		if (klicks.isEmpty()) Hinweise.zeige("alle", "Sortiert", regelName(), Farben.NEON);
	}
}
