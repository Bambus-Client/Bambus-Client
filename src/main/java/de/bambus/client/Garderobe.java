package de.bambus.client;

import java.util.ArrayList;
import java.util.List;

/**
 * Einheitlicher Zugriff auf alle Kosmetik-Arten: was es gibt, was gewählt ist und wie man wählt.
 * Wird von der Garderobe (Escape-Menü) und vom Kosmetik-Tab im Bambus-Menü benutzt.
 */
public final class Garderobe {

	private Garderobe() {}

	/** Anzeigename eines Kosmetik-Stücks, z. B. ("cape", "gold") -> "Goldhalm". */
	public static String name(String art, String id) {
		Art a = Art.zuSchluessel(art);
		if (a != null) {
			for (Eintrag e : a.eintraege()) if (e.id().equals(id)) return e.name();
		}
		return id;
	}

	/** Ein wählbarer Eintrag einer Kosmetik-Art. */
	public record Eintrag(String id, String name, int farbe, boolean eigen) {}

	/** Die Kosmetik-Arten – in derselben Reihenfolge wie die Unter-Tabs im Bambus-Menü. */
	public enum Art {
		CAPES("Capes", "Wehende Umhänge"),
		FLUEGEL("Flügel", "3D-Flügel am Rücken"),
		PARTIKEL("Partikel", "Kreisen um dich herum"),
		HUETE("Hüte", "Kopf-Kosmetik"),
		KILL("Kill-Effekte", "Wenn du jemanden besiegst"),
		PFEIL("Pfeil-Spuren", "Hinter deinen Geschossen");

		public final String titel;
		public final String beschreibung;

		Art(String titel, String beschreibung) {
			this.titel = titel;
			this.beschreibung = beschreibung;
		}

		/** Alle wählbaren Einträge (ohne „Keins“). */
		public List<Eintrag> eintraege() {
			List<Eintrag> liste = new ArrayList<>();
			switch (this) {
				case CAPES -> Kosmetik.alleCapes().forEach(k -> liste.add(new Eintrag(k.id(), k.name(), k.farbe(), Kosmetik.istEigen(k.id()))));
				case FLUEGEL -> Kosmetik.alleFluegel().forEach(f -> liste.add(new Eintrag(f.id(), f.name(), f.farbe(), Kosmetik.istEigen(f.id()))));
				case PARTIKEL -> Kosmetik.PARTIKEL.forEach(p -> liste.add(new Eintrag(p.id(), p.name(), p.farbe(), false)));
				case HUETE -> Kosmetik.HUETE.forEach(h -> liste.add(new Eintrag(h.id(), h.name(), h.farbe(), false)));
				case KILL -> Kosmetik.KILL_EFFEKTE.forEach(e -> liste.add(new Eintrag(e.id(), e.name(), e.farbe(), false)));
				case PFEIL -> Kosmetik.SPUREN.forEach(e -> liste.add(new Eintrag(e.id(), e.name(), e.farbe(), false)));
			}
			return liste;
		}

		/** Kurzname der Art, wie ihn Quests.java und der Launcher verwenden. */
		public String schluessel() {
			return switch (this) {
				case CAPES -> "cape";
				case FLUEGEL -> "fluegel";
				case PARTIKEL -> "partikel";
				case HUETE -> "hut";
				case KILL -> "killeffekt";
				case PFEIL -> "spur";
			};
		}

		/** Die Art zu einem Kurznamen, oder null. */
		public static Art zuSchluessel(String schluessel) {
			for (Art a : values()) if (a.schluessel().equals(schluessel)) return a;
			return null;
		}

		/** Id in der Einstellung (kann „aus“ oder veraltet sein). */
		private String gespeichert() {
			BambusConfig c = BambusClient.config;
			return switch (this) {
				case CAPES -> c.capeDesign;
				case FLUEGEL -> c.fluegel;
				case PARTIKEL -> c.partikel;
				case HUETE -> c.hut;
				case KILL -> c.killEffekt;
				case PFEIL -> c.spur;
			};
		}

		/** Der gewählte Eintrag – oder null, wenn „Keins“ gewählt ist. */
		public Eintrag gewaehlt() {
			String id = gespeichert();
			if (id == null || Kosmetik.AUS.equals(id)) {
				return null;
			}
			return eintraege().stream().filter(e -> e.id().equals(id)).findFirst().orElse(null);
		}

		/** Wählt einen Eintrag; {@link Kosmetik#AUS} = keins. Speichert nicht. */
		public void waehle(String id) {
			BambusConfig c = BambusClient.config;
			switch (this) {
				case CAPES -> {
					c.capeDesign = id;
					if (!Kosmetik.AUS.equals(id)) {
						c.capeAn = true;
					}
				}
				case FLUEGEL -> c.fluegel = id;
				case PARTIKEL -> c.partikel = id;
				case HUETE -> c.hut = id;
				case KILL -> c.killEffekt = id;
				case PFEIL -> c.spur = id;
			}
		}
	}
}
