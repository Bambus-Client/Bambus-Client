package de.bambus.client.hud;

import de.bambus.client.BambusClient;
import de.bambus.client.Einstellung;
import de.bambus.client.Farben;
import de.bambus.client.Musik;
import de.bambus.client.Schriften;
import de.bambus.client.Songtext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Musik-Insel oben in der Mitte: kleines Cover, „Titel · Künstler“ bzw. der Songtext im Karaoke-Stil,
 * animierter Equalizer und Fortschritt. Dazu auf Wunsch die Songtext-Wörter groß in der Welt ({@link SongtextWelt}).
 * Quelle: Windows-Medienwiedergabe (Spotify, Browser …) – siehe {@link Musik}.
 */
public class MusikModul extends HudModul {

	/** Lila wie in der Vorlage. */
	public static final int AKZENT = 0xFF9A7BFF;
	private static final int BREITE = 190, HOEHE = 18;
	private static final long EINBLENDEN_MS = 6000, AUSBLENDEN_MS = 400;

	private final Option cover = option("cover", "Album-Cover", true);
	private final Option songtext = option("songtext", "Songtext (Karaoke)", true);
	private final Option woerter = option("woerter", "3D-Wörter", true);
	private final Option fortschritt = option("fortschritt", "Fortschritt", true);
	private final Option equalizer = option("equalizer", "Equalizer", true);
	private final Option nurSpotify = option("spotify", "Nur Spotify", false);
	private final Option nurWechsel = option("wechsel", "Nur beim Songwechsel", false);

	public MusikModul() {
		super("musik", "Musik (Spotify)", MITTE, 3, false, "Song, Songtext und 3D-Wörter wie ein Musikvideo", "HUD");
	}

	/** Jeden Tick: Abfrage nur, solange das Modul an ist; Songtext zum aktuellen Lied laden. */
	public void tick() {
		// früher ein Schalter „nur an den Seiten“ – aus hieß: frei in der Welt
		if (pos().optionen != null && pos().optionen.containsKey("seiten")) {
			if (Boolean.FALSE.equals(pos().optionen.remove("seiten"))) BambusClient.config.musikWoerterOrt = SongtextWelt.UEBERALL;
		}
		Musik.nurSpotify(nurSpotify.an());
		Musik.tick(pos().an);
		if (pos().an && (songtext.an() || woerter.an()) && Musik.laeuftEtwas) {
			Songtext.fuerLied(Musik.titel, Musik.kuenstler, Musik.album, Musik.dauer);
		}
	}

	/** Für das HUD: 3D-Wörter zeichnen, wenn das Modul und die Option an sind. */
	public void welt(GuiGraphics g, Minecraft mc) {
		if (pos().an && woerter.an()) SongtextWelt.zeichne(g, mc, BambusClient.config.musikWoerterOrt);
		else SongtextWelt.leeren();
	}

	/** Einstellungsfenster: alle Schalter plus die Durchsichtigkeit der Insel. */
	@Override
	public List<Einstellung> einstellungen() {
		List<Einstellung> liste = new ArrayList<>(super.einstellungen());
		liste.add(new Einstellung.Stufe("Insel durchsichtig", () -> BambusClient.config.musikDurchsicht,
				w -> BambusClient.config.musikDurchsicht = w, 0, 90, 10, "%d %%", hintergrund::an));
		liste.add(Einstellung.Stufe.auswahl("Schrift Insel", Schriften.NAMEN, () -> BambusClient.config.musikSchriftInsel,
				w -> BambusClient.config.musikSchriftInsel = w, () -> true));
		liste.add(Einstellung.Stufe.auswahl("Wörter-Ort", SongtextWelt.ORTE, () -> BambusClient.config.musikWoerterOrt,
				w -> BambusClient.config.musikWoerterOrt = w, woerter::an));
		liste.add(Einstellung.Stufe.auswahl("Schrift Wörter", Schriften.NAMEN, () -> BambusClient.config.musikSchriftWoerter,
				w -> BambusClient.config.musikSchriftWoerter = w, woerter::an));
		return liste;
	}

	@Override
	public void standard() {
		super.standard();
		BambusClient.config.musikDurchsicht = 70;
		BambusClient.config.musikSchriftInsel = 1;
		BambusClient.config.musikSchriftWoerter = 2;
		BambusClient.config.musikWoerterOrt = 0;
	}

	/** Deckkraft des Insel-Hintergrunds (0–1) aus der Einstellung „durchsichtig“. */
	private static float deckend() {
		int d = Math.max(0, Math.min(90, BambusClient.config.musikDurchsicht));
		return 1f - d / 100f;
	}

	private float sichtbar(boolean editor) {
		if (editor || !nurWechsel.an()) return 1f;
		long t = Musik.seitWechsel();
		if (t >= EINBLENDEN_MS) return 0f;
		if (t < AUSBLENDEN_MS) return t / (float) AUSBLENDEN_MS;
		if (t > EINBLENDEN_MS - AUSBLENDEN_MS) return (EINBLENDEN_MS - t) / (float) AUSBLENDEN_MS;
		return 1f;
	}

	private boolean zeigen(boolean editor) {
		return editor || (Musik.laeuftEtwas && sichtbar(false) > 0f);
	}

	@Override
	public int breite(Minecraft mc, boolean editor) {
		return zeigen(editor) ? BREITE : 0;
	}

	@Override
	public int hoehe(Minecraft mc, boolean editor) {
		return zeigen(editor) ? HOEHE : 0;
	}

	@Override
	public void zeichne(GuiGraphics g, Minecraft mc, int x, int y, boolean editor) {
		if (!zeigen(editor)) return;
		boolean beispiel = editor && !Musik.laeuftEtwas;
		float a = sichtbar(editor);
		y -= Math.round((1 - a) * 8);   // von oben hereingleiten
		int b = BREITE, h = HOEHE;
		Font font = mc.font;

		// Pille mit feinem Rand
		float deck = deckend();
		boolean schatten = deck < 0.6f;   // durchsichtig: Schrift mit Schatten, damit sie lesbar bleibt
		if (hintergrund.an()) {
			pille(g, x, y, b, h, Farben.alpha(0x60FFFFFF, (int) (0x60 * a * Math.max(0.5f, deck))));
			pille(g, x + 1, y + 1, b - 2, h - 2, Farben.alpha(0xFF0C0D12, (int) (255 * a * deck)));
		}

		// Cover links (mit abgerundeten Ecken)
		int tx = x + 8;
		if (cover.an()) {
			int cs = 12, cx = x + 4, cy = y + 3;
			if (!beispiel && Musik.coverDa()) {
				g.blit(RenderPipelines.GUI_TEXTURED, Musik.COVER, cx, cy, 0f, 0f, cs, cs, cs, cs, Farben.alpha(0xFFFFFFFF, (int) (255 * a)));
				int ecke = Farben.alpha(0xFF0C0D12, (int) (255 * a * deck));
				g.fill(cx, cy, cx + 1, cy + 1, ecke);
				g.fill(cx + cs - 1, cy, cx + cs, cy + 1, ecke);
				g.fill(cx, cy + cs - 1, cx + 1, cy + cs, ecke);
				g.fill(cx + cs - 1, cy + cs - 1, cx + cs, cy + cs, ecke);
			} else {
				g.fill(cx, cy, cx + cs, cy + cs, Farben.alpha(0xFF2A2440, (int) (255 * a)));
				g.drawString(font, "♪", cx + 3, cy + 2, Farben.alpha(AKZENT, (int) (255 * a)), false);
			}
			tx = cx + cs + 6;
		}
		int rechts = x + b - (equalizer.an() ? 20 : 8);
		int textB = rechts - tx;

		// Text: Songtext (Karaoke) oder „Titel · Künstler“
		double zeit = beispiel ? 0 : Musik.positionJetzt();
		Songtext.Zeile zeile = !beispiel && songtext.an() ? Songtext.aktuelleZeile(zeit) : null;
		if (zeile != null) {
			karaoke(g, font, zeile, zeit, tx, y + 5, textB, a, schatten);
		} else {
			String titel = beispiel ? "Song-Titel" : Musik.titel, kuenstler = beispiel ? "Künstler" : Musik.kuenstler;
			String text = titel + (kuenstler.isEmpty() ? "" : "  ·  " + kuenstler);
			int schrift = BambusClient.config.musikSchriftInsel;
			float s = Math.min(1f, textB / (float) Math.max(1, font.width(Schriften.text(text, schrift, false))));
			if (s < 0.75f) {   // zu lang: kürzen statt winzig
				s = 0.75f;
				while (text.length() > 1 && font.width(Schriften.text(text + "…", schrift, false)) > textB / s) {
					text = text.substring(0, text.length() - 1);
				}
				text = text.stripTrailing() + "…";
			}
			Component c = Schriften.text(text, schrift, false);
			int w = (int) (font.width(c) * s);
			klein(g, font, c, tx + (textB - w) / 2, y + 5 + (int) ((1 - s) * 4), s, Farben.alpha(Farben.WEISS, (int) (255 * a)), schatten);
		}

		// Equalizer rechts
		if (equalizer.an()) {
			boolean spielt = beispiel || Musik.spielt;
			long ms = System.currentTimeMillis();
			for (int i = 0; i < 4; i++) {
				int bh = spielt ? 2 + (int) Math.round(3.5 * (1 + Math.sin(ms / (110.0 + i * 37) + i * 1.7))) : 2;
				int ex = x + b - 17 + i * 3, ey = y + h / 2 + 4;
				g.fill(ex, ey - bh, ex + 2, ey, Farben.alpha(AKZENT, (int) (255 * a)));
			}
		}

		// Fortschritt als feine Linie unten
		if (fortschritt.an()) {
			double dauer = beispiel ? 200 : Musik.dauer, pos = beispiel ? 60 : zeit;
			int lx = x + 9, lb = b - 18, ly = y + h - 3;
			if (dauer > 0) {
				g.fill(lx, ly, lx + (int) Math.round(lb * Math.min(1, pos / dauer)), ly + 1, Farben.alpha(AKZENT, (int) (230 * a)));
			}
		}
	}

	/** Songtext-Zeile: gesungene Wörter weiß, das aktuelle lila hinterlegt, kommende grau. */
	private static void karaoke(GuiGraphics g, Font font, Songtext.Zeile zeile, double zeit, int x, int y, int breite, float a,
								boolean schatten) {
		List<Songtext.Wort> woerter = zeile.woerter();
		int schrift = BambusClient.config.musikSchriftInsel;
		int raum = font.width(Schriften.text(" ", schrift, false));
		// Breite genauso messen wie gezeichnet wird: Wort für Wort plus Leerzeichen
		int gesamt = raum * Math.max(0, woerter.size() - 1);
		for (Songtext.Wort w : woerter) gesamt += font.width(Schriften.text(w.text(), schrift, false));
		float s = Math.min(1f, breite / (float) Math.max(1, gesamt));
		s = Math.max(0.6f, s);
		float start = x + Math.max(0, (breite - gesamt * s) / 2);
		int ty = y + (int) ((1 - s) * 4);
		float cx = start;
		for (int i = 0; i < woerter.size(); i++) {
			Songtext.Wort w = woerter.get(i);
			double naechstes = i + 1 < woerter.size() ? woerter.get(i + 1).start() : w.start() + 0.6;
			boolean gesungen = zeit >= w.start();
			boolean aktuell = gesungen && zeit < naechstes;
			Component wort = Schriften.text(w.text(), schrift, false);
			float ww = font.width(wort) * s;
			if (cx + ww > x + breite + 4) break;   // passt auch verkleinert nicht mehr
			if (aktuell) {
				g.fill((int) cx - 2, ty - 2, (int) (cx + ww) + 2, ty + (int) (8 * s) + 1, Farben.alpha(AKZENT, (int) (90 * a)));
			}
			int farbe = gesungen ? Farben.WEISS : schatten ? 0xFFB4BAC6 : 0xFF7E8492;
			klein(g, font, wort, (int) cx, ty, s, Farben.alpha(farbe, (int) (255 * a)), schatten);
			cx += ww + raum * s;
		}
	}

	/** Pille: Rechteck mit halbrunden Enden. */
	private static void pille(GuiGraphics g, int x, int y, int b, int h, int farbe) {
		double r = h / 2.0;
		for (int zeile = 0; zeile < h; zeile++) {
			double dy = r - zeile - 0.5;
			int einzug = (int) Math.round(r - Math.sqrt(Math.max(0, r * r - dy * dy)));
			g.fill(x + einzug, y + zeile, x + b - einzug, y + zeile + 1, farbe);
		}
	}

	private static void klein(GuiGraphics g, Font font, Component text, int x, int y, float groesse, int farbe, boolean schatten) {
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(groesse, groesse);
		g.drawString(font, text, 0, 0, farbe, schatten);
		g.pose().popMatrix();
	}
}
