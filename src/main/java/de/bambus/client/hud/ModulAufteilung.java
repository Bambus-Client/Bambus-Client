package de.bambus.client.hud;

import de.bambus.client.BambusConfig;

import java.util.Map;

/**
 * Früher bündelten einige Module mehrere Funktionen. Jetzt sind sie aufgeteilt:
 * <ul>
 *   <li>Info (Titel, FPS, XYZ, CPS) → Bambus-Schriftzug, FPS, CPS und Koordinaten</li>
 *   <li>Rüstungsanzeige (Rüstung + Item in der Hand) → Rüstungsanzeige und Item in der Hand</li>
 *   <li>Spielzeit (Spielzeit + Ingame-Tag) → Spielzeit und Ingame-Tag</li>
 * </ul>
 * Beim ersten Start mit der neuen Version werden die alten Einstellungen übernommen: Was an war, bleibt an –
 * an derselben Stelle untereinander, in derselben Größe. Was aus war, bleibt aus.
 */
public final class ModulAufteilung {

	private ModulAufteilung() {}

	/** Höhe einer Textzeile im HUD (Kasten mit einer Zeile + 1 Pixel Abstand). */
	private static final int ZEILE = 16;

	public static void uebernehmen(BambusConfig c) {
		boolean geaendert = info(c);
		geaendert |= ruestung(c);
		geaendert |= spielzeit(c);
		if (geaendert) c.speichern();
	}

	private static boolean an(BambusConfig.ModulPos p, String option, boolean standard) {
		Boolean wert = p.optionen == null ? null : p.optionen.get(option);
		return wert != null ? wert : standard;
	}

	/** Neues Modul nur anlegen, wenn es noch keinen Eintrag hat (z. B. schon im Launcher geschaltet). */
	private static void neu(BambusConfig c, String id, BambusConfig.ModulPos alt, int y, boolean an) {
		if (c.module.containsKey(id)) return;
		BambusConfig.ModulPos p = new BambusConfig.ModulPos(alt.x, y);
		p.an = an;
		p.groesse = alt.groesse;
		if (alt.optionen != null && alt.optionen.containsKey("hintergrund")) {
			p.optionen.put("hintergrund", alt.optionen.get("hintergrund"));
		}
		c.module.put(id, p);
	}

	private static int schritt(BambusConfig.ModulPos p, int hoehe) {
		float g = p.groesse >= 0.5f && p.groesse <= 2.5f ? p.groesse : 1f;
		return Math.round(hoehe * g);
	}

	private static boolean info(BambusConfig c) {
		BambusConfig.ModulPos info = c.module.remove("info");
		if (info == null) return false;
		int y = info.y;
		// Reihenfolge wie im alten Kasten: Titel, FPS, (XYZ), CPS – nur eingeschaltete Zeilen belegen Platz
		boolean titel = info.an && an(info, "titel", true);
		boolean fps = info.an && an(info, "fps", true);
		boolean xyz = info.an && an(info, "xyz", true);
		boolean cps = info.an && an(info, "cps", true);
		neu(c, "titel", info, y, titel);
		if (titel) y += schritt(info, ZEILE);
		neu(c, "fps", info, y, fps);
		if (fps) y += schritt(info, ZEILE);
		neu(c, "cps", info, y, cps);
		if (cps) y += schritt(info, ZEILE);
		// XYZ gibt es schon als eigenes Modul „Koordinaten“ (mit Nether-Umrechnung) – an seinem eigenen Platz,
		// denn es ist mehrzeilig und würde sonst die Anzeigen unter dem alten Info-Kasten überdecken
		BambusConfig.ModulPos koord = c.module.get("koordinaten");
		if (xyz) {
			if (koord == null) {
				BambusConfig.ModulPos k = new BambusConfig.ModulPos(4, 216);
				k.an = true;
				c.module.put("koordinaten", k);
			} else {
				koord.an = true;
			}
		}
		return true;
	}

	private static boolean ruestung(BambusConfig c) {
		BambusConfig.ModulPos r = c.module.get("ruestung");
		if (r == null || c.module.containsKey("hand")) return false;
		Map<String, Boolean> o = r.optionen;
		boolean ruestungZeigen = an(r, "ruestung", true);
		boolean hand = r.an && an(r, "hand", true);
		int y = ruestungZeigen ? r.y + schritt(r, 4 * 17) + 2 : r.y;
		neu(c, "hand", r, y, hand);
		if (o != null && o.containsKey("haltbarkeit")) c.module.get("hand").optionen.put("haltbarkeit", o.get("haltbarkeit"));
		if (!ruestungZeigen) r.an = false;   // früher nur das Hand-Item gezeigt
		if (o != null) {
			o.remove("ruestung");
			o.remove("hand");
		}
		return true;
	}

	private static boolean spielzeit(BambusConfig c) {
		BambusConfig.ModulPos s = c.module.get("spielzeit");
		if (s == null || c.module.containsKey("ingametag")) return false;
		neu(c, "ingametag", s, s.y + schritt(s, ZEILE), s.an && an(s, "tag", true));
		if (s.optionen != null) s.optionen.remove("tag");
		return true;
	}
}
