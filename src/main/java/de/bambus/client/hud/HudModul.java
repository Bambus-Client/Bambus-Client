package de.bambus.client.hud;

import de.bambus.client.BambusClient;
import de.bambus.client.BambusConfig;
import de.bambus.client.Farben;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Ein verschiebbares Element des Bambus-HUDs.
 * Einstellungen: jedes Modul meldet seine Schalter mit {@link #option} an; sie erscheinen im Mod-Menü
 * hinter den drei Punkten (⋮) der Modul-Karte und werden pro Modul in der Config gespeichert.
 */
public abstract class HudModul {

	/** Ein Ein/Aus-Schalter eines Moduls. */
	public final class Option {
		public final String schluessel;
		public final String name;
		private final boolean standard;

		private Option(String schluessel, String name, boolean standard) {
			this.schluessel = schluessel;
			this.name = name;
			this.standard = standard;
		}

		public boolean an() {
			Boolean wert = optionenSpeicher().get(schluessel);
			return wert != null ? wert : standard;
		}

		public void umschalten() {
			optionenSpeicher().put(schluessel, !an());
		}
	}

	private final List<Option> optionen = new ArrayList<>();
	/** Gilt für jedes Modul: Kasten hinter dem Modul zeichnen. */
	public final Option hintergrund = option("hintergrund", "Hintergrund", true);

	public final String id;
	public final String name;
	private final int stdX;
	private final int stdY;
	private final boolean stdAn;
	/** Kurzbeschreibung für das Bambus-Menü. */
	public final String beschreibung;
	/** Kategorie im Menü: "PVP", "HUD" oder "INFO". */
	public final String kategorie;

	protected HudModul(String id, String name, int stdX, int stdY) {
		this(id, name, stdX, stdY, true, "", "HUD");
	}

	protected HudModul(String id, String name, int stdX, int stdY, boolean stdAn, String beschreibung, String kategorie) {
		this.id = id;
		this.name = name;
		this.stdX = stdX;
		this.stdY = stdY;
		this.stdAn = stdAn;
		this.beschreibung = beschreibung;
		this.kategorie = kategorie;
	}

	/** Meldet einen Schalter an – in den Feldern der Unterklasse aufrufen. */
	protected final Option option(String schluessel, String name, boolean standard) {
		Option o = new Option(schluessel, name, standard);
		optionen.add(o);
		return o;
	}

	/** Alle Schalter dieses Moduls, in der Reihenfolge der Anmeldung. */
	public List<Option> optionen() {
		return Collections.unmodifiableList(optionen);
	}

	/** Alle Schalter auf ihren Standard zurück. */
	public void optionenZuruecksetzen() {
		optionenSpeicher().clear();
	}

	private java.util.Map<String, Boolean> optionenSpeicher() {
		BambusConfig.ModulPos p = pos();
		if (p.optionen == null) {   // ältere Config ohne Einstellungen
			p.optionen = new java.util.LinkedHashMap<>();
		}
		return p.optionen;
	}

	public abstract int breite(Minecraft mc, boolean editor);

	public abstract int hoehe(Minecraft mc, boolean editor);

	public abstract void zeichne(GuiGraphics g, Minecraft mc, int x, int y, boolean editor);

	public BambusConfig.ModulPos pos() {
		return BambusClient.config.modul(id, stdX, stdY, stdAn);
	}

	public void zuruecksetzen() {
		BambusConfig.ModulPos p = pos();
		p.x = stdX;
		p.y = stdY;
		p.groesse = 1f;
	}

	/** Größe des Moduls (0,5 – 2,5), im HUD-Editor einstellbar. */
	public float skala() {
		float s = pos().groesse;
		return (s >= 0.5f && s <= 2.5f) ? s : 1f;
	}

	/** Breite/Höhe so, wie das Modul mit seiner Größe auf dem Bildschirm erscheint. */
	public int breiteS(Minecraft mc, boolean editor) {
		return Math.round(breite(mc, editor) * skala());
	}

	public int hoeheS(Minecraft mc, boolean editor) {
		return Math.round(hoehe(mc, editor) * skala());
	}

	/** Position, begrenzt auf den sichtbaren Bildschirm. */
	public int x(Minecraft mc, boolean editor) {
		int max = mc.getWindow().getGuiScaledWidth() - breiteS(mc, editor);
		return Math.max(0, Math.min(pos().x, max));
	}

	public int y(Minecraft mc, boolean editor) {
		int max = mc.getWindow().getGuiScaledHeight() - hoeheS(mc, editor);
		return Math.max(0, Math.min(pos().y, max));
	}

	public void zeichneAnPosition(GuiGraphics g, Minecraft mc, boolean editor) {
		if (breite(mc, editor) <= 0 || hoehe(mc, editor) <= 0) {
			return;
		}
		float s = skala();
		if (s == 1f) {
			zeichne(g, mc, x(mc, editor), y(mc, editor), editor);
			return;
		}
		g.pose().pushMatrix();
		g.pose().translate(x(mc, editor), y(mc, editor));
		g.pose().scale(s, s);
		zeichne(g, mc, 0, 0, editor);
		g.pose().popMatrix();
	}

	/** Kasten hinter dem Modul – entfällt, wenn „Hintergrund“ ausgeschaltet ist. */
	protected void panel(GuiGraphics g, int x, int y, int b, int h) {
		if (!hintergrund.an()) {
			return;
		}
		g.fill(x, y, x + b, y + h, Farben.HINTERGRUND);
		if (BambusClient.config.verlauf) {
			g.fillGradient(x, y, x + 2, y + h, Farben.NEON, Farben.NEON_CYAN);
		} else {
			g.fill(x, y, x + 2, y + h, Farben.GRUEN);
		}
	}
}
