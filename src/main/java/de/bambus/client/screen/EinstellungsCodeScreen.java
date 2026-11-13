package de.bambus.client.screen;

import de.bambus.client.BambusClient;
import de.bambus.client.EinstellungsTransfer;
import de.bambus.client.Farben;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.List;

/**
 * Einstellungs-Code: eigenen Code kopieren oder einen Code einfügen und übernehmen.
 * Vor dem Übernehmen zeigt das Fenster, was im Code steckt, und man wählt Bambus und/oder Minecraft.
 */
public class EinstellungsCodeScreen extends Screen {

	private record Bereich(int x, int y, int b, int h, Runnable aktion) {}

	private final Screen eltern;
	private final List<Bereich> bereiche = new ArrayList<>();
	private EditBox feld;
	private EinstellungsTransfer.Inhalt inhalt;
	private boolean bambusAuch = true, minecraftAuch = true;
	private String meldung = "";
	private int meldungFarbe = Farben.NEON_CYAN;
	private long meldungZeit;

	public EinstellungsCodeScreen(Screen eltern) {
		super(Component.literal("Einstellungs-Code"));
		this.eltern = eltern;
	}

	@Override
	protected void init() {
		String alt = feld == null ? "" : feld.getValue();
		feld = new EditBox(font, 0, 0, 200, 16, Component.literal("Code"));
		feld.setMaxLength(200_000);
		feld.setHint(Component.literal("§8BAMBUS-… hier einfügen (Strg+V)"));
		feld.setBordered(false);
		feld.setTextColor(Farben.WEISS);
		feld.setValue(alt);
		feld.setResponder(this::pruefe);
		addRenderableWidget(feld);
	}

	private void pruefe(String text) {
		inhalt = text.isBlank() ? null : EinstellungsTransfer.lies(text);
	}

	private void melde(String text, int farbe) {
		meldung = text;
		meldungFarbe = farbe;
		meldungZeit = System.currentTimeMillis();
	}

	private void kopieren() {
		String code = EinstellungsTransfer.code(minecraft);
		minecraft.keyboardHandler.setClipboard(code);
		melde("✓ Code kopiert (" + code.length() + " Zeichen) – schick ihn deinen Freunden", Farben.NEON);
	}

	private void ausZwischenablage() {
		String text = minecraft.keyboardHandler.getClipboard();
		feld.setValue(text == null ? "" : text.trim());
		if (inhalt == null) melde("In der Zwischenablage ist kein Bambus-Code", Farben.ROT);
	}

	private void uebernehmen() {
		if (inhalt == null) return;
		if (!bambusAuch && !minecraftAuch) {
			melde("Wähle aus, was übernommen werden soll", Farben.ROT);
			return;
		}
		String text = EinstellungsTransfer.anwenden(minecraft, inhalt, bambusAuch, minecraftAuch);
		melde("✓ " + text, Farben.NEON);
		feld.setValue("");
		rebuildWidgets();   // Größe der Oberfläche kann sich geändert haben
	}

	@Override
	public void onClose() {
		minecraft.setScreen(eltern);
	}

	// ================================================================ Zeichnen

	@Override
	public void render(GuiGraphics g, int mx, int my, float delta) {
		bereiche.clear();
		Farben.themaAnwenden(BambusClient.config.thema);
		int b = Math.min(340, width - 20), h = Math.min(250, height - 16);
		int x = (width - b) / 2, y = (height - h) / 2;
		g.fill(0, 0, width, height, 0xB0020407);
		g.fill(x, y, x + b, y + h, 0xF00A0F17);
		Neon.leuchtRahmen(g, x, y, b, h, Farben.NEON, 0.6f);
		Neon.logo(g, x + 10, y + 9, 18);
		Neon.leuchtText(g, font, "EINSTELLUNGS-CODE", x + 26, y + 10, 1.1f, Farben.NEON);
		Neon.text(g, font, "Bambus-Module mit Anordnung + Minecraft-Einstellungen teilen", x + 26, y + 22, 0.6f, Farben.GRAU, false);
		int zy = y + 38;

		// ---- Teilen
		Neon.text(g, font, "DEINE EINSTELLUNGEN TEILEN", x + 10, zy, 0.7f, Farben.NEON, false);
		zy += 10;
		knopf(g, mx, my, "CODE KOPIEREN", x + 10, zy, 100, 18, Farben.NEON, true, this::kopieren);
		Neon.text(g, font, "Alle Module, Positionen, Funktionen,", x + 118, zy + 1, 0.6f, Farben.GRAU, false);
		Neon.text(g, font, "Grafik, Ton und Tastenbelegung", x + 118, zy + 9, 0.6f, Farben.GRAU, false);
		zy += 26;
		g.fill(x + 10, zy, x + b - 10, zy + 1, Farben.LINIE);
		zy += 8;

		// ---- Einfügen
		Neon.text(g, font, "CODE EINFÜGEN", x + 10, zy, 0.7f, Farben.NEON, false);
		zy += 10;
		int fb = b - 20 - 94;
		g.fill(x + 10, zy, x + 10 + fb, zy + 18, Farben.KARTE);
		g.renderOutline(x + 10, zy, fb, 18, feld.isFocused() ? Farben.NEON : inhalt != null ? Farben.NEON_CYAN : Farben.LINIE);
		feld.setX(x + 15);
		feld.setY(zy + 5);
		feld.setWidth(fb - 10);
		knopf(g, mx, my, "EINFÜGEN", x + b - 10 - 88, zy, 88, 18, Farben.NEON_CYAN, true, this::ausZwischenablage);
		zy += 24;

		if (inhalt != null) {
			int bm = inhalt.bambusWerte(), mcw = inhalt.minecraft().size();
			zy = auswahl(g, mx, my, x + 10, zy, b - 20, bm > 0, bambusAuch, "Bambus Client",
					bm > 0 ? inhalt.module() + " Module mit Anordnung · Funktionen, Texturen, Fadenkreuz" : "nicht im Code",
					() -> bambusAuch = !bambusAuch);
			zy = auswahl(g, mx, my, x + 10, zy, b - 20, mcw > 0, minecraftAuch, "Minecraft",
					mcw > 0 ? mcw + " Einstellungen · davon " + inhalt.tasten() + " Tasten" : "nicht im Code",
					() -> minecraftAuch = !minecraftAuch);
			Neon.text(g, font, "Blätter, Quests und Kosmetik bleiben unverändert.", x + 10, zy + 1, 0.6f, Farben.GRAU, false);
		} else if (!feld.getValue().isBlank()) {
			Neon.text(g, font, "Das ist kein gültiger Bambus-Code.", x + 10, zy + 2, 0.75f, Farben.ROT, false);
		} else {
			Neon.text(g, font, "Füge einen Code ein, den dir jemand geschickt hat.", x + 10, zy + 2, 0.75f, Farben.GRAU, false);
		}

		// ---- Meldung und Knöpfe unten
		int fy = y + h - 26;
		if (!meldung.isEmpty() && System.currentTimeMillis() - meldungZeit < 6000) {
			Neon.text(g, font, meldung, x + 10, fy - 12, 0.7f, meldungFarbe, false);
		}
		knopf(g, mx, my, "ZURÜCK", x + 10, fy, 70, 18, Farben.GRAU, true, this::onClose);
		knopf(g, mx, my, "ÜBERNEHMEN", x + b - 10 - 100, fy, 100, 18, Farben.NEON, inhalt != null, this::uebernehmen);
		super.render(g, mx, my, delta);
	}

	private int auswahl(GuiGraphics g, int mx, int my, int x, int y, int b, boolean vorhanden, boolean an,
						String titel, String text, Runnable umschalten) {
		boolean aktiv = vorhanden && an;
		boolean ueber = vorhanden && Neon.ueber(mx, my, x, y, b, 22);
		g.fill(x, y, x + b, y + 22, ueber ? Farben.KARTE_HOVER : Farben.KARTE);
		g.fill(x, y, x + 2, y + 22, aktiv ? Farben.NEON : Farben.LINIE);
		g.drawString(font, titel, x + 8, y + 3, vorhanden ? Farben.WEISS : Farben.GRAU, false);
		Neon.text(g, font, font.plainSubstrByWidth(text, (int) ((b - 40) / 0.6f)), x + 8, y + 13, 0.6f, Farben.GRAU, false);
		if (vorhanden) {
			Neon.schalter(g, x + b - 26, y + 7, an, "code:" + titel);
			bereiche.add(new Bereich(x, y, b, 22, umschalten));
		}
		return y + 26;
	}

	private void knopf(GuiGraphics g, int mx, int my, String text, int x, int y, int b, int h, int farbe, boolean aktiv, Runnable aktion) {
		boolean ueber = aktiv && Neon.ueber(mx, my, x, y, b, h);
		g.fill(x, y, x + b, y + h, ueber ? Farben.alpha(farbe, 45) : Farben.KARTE);
		g.renderOutline(x, y, b, h, aktiv ? (ueber ? farbe : Farben.alpha(farbe, 140)) : 0xFF161F2B);
		int w = (int) (font.width(text) * 0.75f);
		Neon.text(g, font, text, x + (b - w) / 2, y + (h - 6) / 2, 0.75f, aktiv ? (ueber ? Farben.WEISS : farbe) : 0xFF3A4656, false);
		if (aktiv) bereiche.add(new Bereich(x, y, b, h, aktion));
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent e, boolean doppelt) {
		if (e.button() == 0) {
			for (Bereich r : bereiche) {
				if (Neon.ueber(e.x(), e.y(), r.x(), r.y(), r.b(), r.h())) {
					minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
					r.aktion().run();
					return true;
				}
			}
		}
		return super.mouseClicked(e, doppelt);
	}
}
