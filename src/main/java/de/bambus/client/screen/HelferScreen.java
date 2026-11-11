package de.bambus.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import de.bambus.client.BambusClient;
import de.bambus.client.Farben;
import de.bambus.helfer.HelferWissen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Bambus-Helfer im Spiel: derselbe Offline-Hilfe-Chat wie im Launcher (gemeinsames {@link HelferWissen}).
 * Erreichbar über Rechts-Shift → HELFER, abschaltbar unter Mod-Menü → MEHR → EXTRAS.
 */
public class HelferScreen extends Screen {

	private record Nachricht(String text, boolean vonHelfer, HelferWissen.Eintrag eintrag) {}

	private record Bereich(int x, int y, int b, int h, Runnable aktion) {}

	/** Zutaten der Helfer-Rezepte als echte Gegenstände. */
	private static final Map<String, Item> ZUTATEN = Map.ofEntries(
			Map.entry("Brett", Items.OAK_PLANKS), Map.entry("Stock", Items.STICK), Map.entry("Kohle", Items.COAL),
			Map.entry("Stein", Items.COBBLESTONE), Map.entry("Eisen", Items.IRON_INGOT), Map.entry("Diamant", Items.DIAMOND),
			Map.entry("Wolle", Items.WHITE_WOOL), Map.entry("Buch", Items.BOOK), Map.entry("Obsidian", Items.OBSIDIAN),
			Map.entry("Eisenblock", Items.IRON_BLOCK), Map.entry("Faden", Items.STRING), Map.entry("Pulver", Items.GUNPOWDER),
			Map.entry("Sand", Items.SAND));

	/** Ergebnisse der Helfer-Rezepte (englischer Name aus dem Wissen → Gegenstand). */
	private static final Map<String, Item> ERGEBNISSE = Map.ofEntries(
			Map.entry("Crafting Table", Items.CRAFTING_TABLE), Map.entry("Torch", Items.TORCH), Map.entry("Chest", Items.CHEST),
			Map.entry("Furnace", Items.FURNACE), Map.entry("Pickaxe", Items.IRON_PICKAXE), Map.entry("Sword", Items.DIAMOND_SWORD),
			Map.entry("Axe", Items.IRON_AXE), Map.entry("Bed", Items.WHITE_BED), Map.entry("Enchanting Table", Items.ENCHANTING_TABLE),
			Map.entry("Anvil", Items.ANVIL), Map.entry("Bow", Items.BOW), Map.entry("Shield", Items.SHIELD),
			Map.entry("Bucket", Items.BUCKET), Map.entry("TNT", Items.TNT), Map.entry("Bookshelf", Items.BOOKSHELF),
			Map.entry("Ladder", Items.LADDER));

	private static final int FELD = 18;          // Rezept-Feld
	private static final int PAD = 6;            // Innenabstand der Sprechblasen
	private static final int ZEILE = 10;

	/** Verlauf bleibt erhalten, solange das Spiel läuft. */
	private static final List<Nachricht> VERLAUF = new ArrayList<>();

	private final Screen eltern;
	private final List<Bereich> bereiche = new ArrayList<>();
	private EditBox feld;
	private String wartendeFrage;
	private long antwortAb;
	private int scroll;
	private int inhaltHoehe;
	private boolean nachUnten = true;
	private final Map<Nachricht, List<FormattedCharSequence>> umbruch = new IdentityHashMap<>();
	private int umbruchBreite = -1;

	// Layout (wird in render berechnet)
	private int px, py, pb, ph;
	private int cx, cy, cb, ch;

	public HelferScreen(Screen eltern) {
		super(Component.literal("Bambus-Helfer"));
		this.eltern = eltern;
	}

	private boolean englisch() {
		return !minecraft.options.languageCode.startsWith("de");
	}

	@Override
	protected void init() {
		feld = new EditBox(font, 0, 0, 200, 16, Component.literal("Frage"));
		feld.setHint(Component.literal(englisch() ? "Ask something …" : "Frag etwas …"));
		feld.setMaxLength(120);
		addRenderableWidget(feld);
		setInitialFocus(feld);
		if (VERLAUF.isEmpty()) {
			VERLAUF.add(new Nachricht(HelferWissen.begruessung(englisch()), true, null));
		}
		nachUnten = true;
	}

	// =================================================================== Logik

	private void senden(String text) {
		text = text.trim();
		if (text.isEmpty() || wartendeFrage != null) {
			return;
		}
		feld.setValue("");
		VERLAUF.add(new Nachricht(text, false, null));
		wartendeFrage = text;
		antwortAb = System.currentTimeMillis() + 380 + Math.min(500, text.length() * 8L);   // kurz „tippen“
		nachUnten = true;
	}

	@Override
	public void tick() {
		super.tick();
		if (wartendeFrage != null && System.currentTimeMillis() >= antwortAb) {
			VERLAUF.add(antwort(wartendeFrage));
			wartendeFrage = null;
			nachUnten = true;
		}
	}

	private Nachricht antwort(String frage) {
		boolean en = englisch();
		String rechnung = HelferWissen.netherRechner(frage, en);
		if (rechnung != null) {
			return new Nachricht(rechnung, true, null);
		}
		HelferWissen.Eintrag e = HelferWissen.finde(frage);
		return e == null ? new Nachricht(HelferWissen.unbekannt(en), true, null) : new Nachricht(e.text(en), true, e);
	}

	// =================================================================== Zeichnen

	@Override
	public void renderBackground(GuiGraphics g, int mx, int my, float delta) {
		g.fill(0, 0, width, height, 0xC0020407);
		Neon.gitter(g, 0, 0, width, height, 24, Farben.alpha(Farben.NEON, 7));
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float delta) {
		bereiche.clear();
		Farben.themaAnwenden(BambusClient.config.thema);
		pb = Math.min(width - 20, 440);
		ph = Math.min(height - 20, 300);
		px = (width - pb) / 2;
		py = (height - ph) / 2;

		g.fill(px, py, px + pb, py + ph, Farben.PANEL);
		Neon.gitter(g, px, py, pb, ph, 16, Farben.alpha(Farben.NEON, 6));
		Neon.leuchtRahmen(g, px, py, pb, ph, Farben.NEON, 0.55f + 0.45f * Farben.puls());
		Neon.ecken(g, px - 5, py - 5, pb + 10, ph + 10, 12, Farben.NEON_CYAN);

		kopf(g, mx, my);
		int chipsUnten = vorschlaege(g, mx, my, py + 36);

		cx = px + 10;
		cy = chipsUnten + 6;
		cb = pb - 20;
		ch = py + ph - 32 - cy;
		verlauf(g);

		// Eingabe
		int sb = 64;
		feld.setX(px + 10);
		feld.setY(py + ph - 24);
		feld.setWidth(pb - 20 - sb - 6);
		knopf(g, mx, my, englisch() ? "SEND" : "SENDEN", px + pb - 10 - sb, py + ph - 25, sb, 18, () -> senden(feld.getValue()));

		super.render(g, mx, my, delta);
	}

	private void kopf(GuiGraphics g, int mx, int my) {
		Neon.logo(g, px + 12, py + 8, 16);
		Neon.leuchtText(g, font, englisch() ? "BAMBUS HELPER" : "BAMBUS-HELFER", px + 30, py + 9, 1.25f, Farben.NEON);
		String unter = englisch() ? "offline · no AI · free" : "offline · keine KI · kostenlos";
		Neon.text(g, font, unter, px + 30, py + 21, 0.6f, Farben.GRAU, false);

		int sx = px + pb - 20, sy = py + 8;
		boolean hover = Neon.ueber(mx, my, sx, sy, 12, 12);
		g.drawString(font, "✕", sx + 2, sy + 2, hover ? Farben.ROT : Farben.GRAU, false);
		klick(sx, sy, 12, 12, this::onClose);
		g.fill(px + 8, py + 30, px + pb - 8, py + 31, Farben.LINIE);
	}

	/** Vorschlags-Chips; gibt die Unterkante zurück. */
	private int vorschlaege(GuiGraphics g, int mx, int my, int y) {
		String[] liste = englisch() ? HelferWissen.VORSCHLAEGE_EN : HelferWissen.VORSCHLAEGE_DE;
		int x = px + 10;
		for (String v : liste) {
			int b = (int) (font.width(v) * 0.75f) + 12;
			if (x + b > px + pb - 10) {
				break;   // eine Zeile reicht – der Rest passt nicht
			}
			boolean hover = Neon.ueber(mx, my, x, y, b, 13);
			g.fill(x, y, x + b, y + 13, hover ? Farben.alpha(Farben.NEON, 40) : Farben.KARTE);
			g.renderOutline(x, y, b, 13, hover ? Farben.NEON : Farben.LINIE);
			Neon.text(g, font, v, x + 6, y + 3, 0.75f, hover ? Farben.WEISS : Farben.GRAU, false);
			klick(x, y, b, 13, () -> senden(v));
			x += b + 4;
		}
		return y + 13;
	}

	private void verlauf(GuiGraphics g) {
		int maxB = (int) (cb * 0.78f);
		// erst messen, damit neue Nachrichten unten stehen bleiben
		inhaltHoehe = 0;
		for (Nachricht n : VERLAUF) {
			inhaltHoehe += hoehe(n, maxB) + 6;
		}
		if (wartendeFrage != null) {
			inhaltHoehe += 22;
		}
		int maxScroll = Math.max(0, inhaltHoehe - ch);
		if (nachUnten) {
			scroll = maxScroll;
			nachUnten = false;
		}
		scroll = Math.max(0, Math.min(scroll, maxScroll));

		g.enableScissor(cx, cy, cx + cb, cy + ch);
		int y = cy - scroll;
		for (Nachricht n : VERLAUF) {
			y += blase(g, n, maxB, y) + 6;
		}
		if (wartendeFrage != null) {
			tippen(g, y);
		}
		g.disableScissor();

		if (maxScroll > 0) {
			int balkenH = Math.max(12, ch * ch / inhaltHoehe);
			int balkenY = cy + (ch - balkenH) * scroll / maxScroll;
			g.fill(px + pb - 5, cy, px + pb - 3, cy + ch, Farben.LINIE);
			g.fill(px + pb - 5, balkenY, px + pb - 3, balkenY + balkenH, Farben.NEON);
		}
	}

	/** Umgebrochene Zeilen je Nachricht – nur neu berechnen, wenn sich die Breite ändert (nicht jedes Bild). */
	private List<FormattedCharSequence> zeilen(Nachricht n, int maxB) {
		if (maxB != umbruchBreite) {
			umbruch.clear();
			umbruchBreite = maxB;
		}
		return umbruch.computeIfAbsent(n, m -> font.split(Component.literal(m.text()), maxB - 2 * PAD));
	}

	private boolean hatRezept(Nachricht n) {
		return n.eintrag() != null && n.eintrag().rezept != null;
	}

	private int hoehe(Nachricht n, int maxB) {
		int h = zeilen(n, maxB).size() * ZEILE + 2 * PAD - 2;
		return hatRezept(n) ? h + 3 * FELD + 8 : h;
	}

	/** Zeichnet eine Sprechblase und gibt ihre Höhe zurück. */
	private int blase(GuiGraphics g, Nachricht n, int maxB, int y) {
		List<FormattedCharSequence> z = zeilen(n, maxB);
		int textB = 0;
		for (FormattedCharSequence s : z) {
			textB = Math.max(textB, font.width(s));
		}
		int b = textB + 2 * PAD;
		if (hatRezept(n)) {
			b = Math.max(b, 3 * FELD + 2 * PAD + 44);
		}
		int h = hoehe(n, maxB);
		int x = n.vonHelfer() ? cx : cx + cb - 8 - b;

		if (n.vonHelfer()) {
			g.fill(x, y, x + b, y + h, Farben.KARTE);
			g.fill(x, y, x + 2, y + h, Farben.NEON);
			g.renderOutline(x, y, b, h, Farben.LINIE);
		} else {
			g.fill(x, y, x + b, y + h, Farben.alpha(Farben.NEON_CYAN, 40));
			g.renderOutline(x, y, b, h, Farben.alpha(Farben.NEON_CYAN, 140));
		}
		int ty = y + PAD;
		for (FormattedCharSequence s : z) {
			g.drawString(font, s, x + PAD, ty, n.vonHelfer() ? 0xFFDCE6F0 : Farben.WEISS, false);
			ty += ZEILE;
		}
		if (hatRezept(n)) {
			rezept(g, n.eintrag(), x + PAD, ty + 4);
		}
		return h;
	}

	/** 3×3-Raster mit echten Gegenständen, Pfeil und Ergebnis. */
	private void rezept(GuiGraphics g, HelferWissen.Eintrag e, int x, int y) {
		for (int i = 0; i < 9; i++) {
			int fx = x + (i % 3) * FELD, fy = y + (i / 3) * FELD;
			g.fill(fx, fy, fx + FELD - 1, fy + FELD - 1, 0xFF1C2633);
			g.renderOutline(fx, fy, FELD - 1, FELD - 1, 0xFF2C3A4C);
			Item ding = ZUTATEN.get(e.rezept[i]);
			if (ding != null) {
				g.renderItem(new ItemStack(ding), fx, fy);
			}
		}
		int mitte = y + 3 * FELD / 2;
		int gx = x + 3 * FELD + 6;
		g.drawString(font, "→", gx, mitte - 4, Farben.NEON, false);
		int ex = gx + 14, ey = mitte - FELD / 2 - 1;
		g.fill(ex, ey, ex + FELD + 2, ey + FELD + 2, Farben.alpha(Farben.NEON, 30));
		g.renderOutline(ex, ey, FELD + 2, FELD + 2, Farben.NEON);
		Item ergebnis = ERGEBNISSE.get(e.rezeptEn);
		if (ergebnis != null) {
			g.renderItem(new ItemStack(ergebnis), ex + 2, ey + 2);
		}
	}

	/** „Helfer tippt …“ – drei hüpfende Punkte. */
	private void tippen(GuiGraphics g, int y) {
		g.fill(cx, y, cx + 34, y + 16, Farben.KARTE);
		g.fill(cx, y, cx + 2, y + 16, Farben.NEON);
		long t = System.currentTimeMillis() / 160;
		for (int i = 0; i < 3; i++) {
			int dy = (t + i) % 3 == 0 ? -2 : 0;
			g.fill(cx + 9 + i * 7, y + 7 + dy, cx + 12 + i * 7, y + 10 + dy, Farben.NEON);
		}
	}

	private void knopf(GuiGraphics g, int mx, int my, String text, int x, int y, int b, int h, Runnable aktion) {
		boolean hover = Neon.ueber(mx, my, x, y, b, h);
		g.fill(x, y, x + b, y + h, hover ? 0xFF7DFFB0 : Farben.NEON);
		if (hover) {
			Neon.leuchtRahmen(g, x, y, b, h, Farben.NEON, 1f);
		}
		g.drawString(font, text, x + (b - font.width(text)) / 2, y + (h - 8) / 2, Farben.SCHWARZ, false);
		klick(x, y, b, h, aktion);
	}

	// ============================================================ Eingabe

	private void klick(int x, int y, int b, int h, Runnable aktion) {
		bereiche.add(new Bereich(x, y, b, h, aktion));
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent e, boolean doppelt) {
		if (e.button() == 0) {
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
		if (Neon.ueber(mx, my, cx, cy, cb, ch)) {
			scroll -= (int) (vertikal * 18);
			return true;
		}
		return super.mouseScrolled(mx, my, horizontal, vertikal);
	}

	@Override
	public boolean keyPressed(KeyEvent e) {
		if (e.key() == InputConstants.KEY_RETURN || e.key() == InputConstants.KEY_NUMPADENTER) {
			senden(feld.getValue());
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
