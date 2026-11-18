package de.bambus.client.emote;

/**
 * Ein Emote: Bewegung über die Zeit, optional mit eigenem kleinen Beat und Effekten (Partikel).
 *
 * @param schleife  läuft weiter, bis man sich bewegt (Tänze) – sonst endet es nach {@code dauer} Sekunden
 * @param takt      eigener Beat aus Notenblock-Klängen oder null
 * @param effekt    Partikel o. Ä. jeden Tick (nur in der Welt, nur für dich sichtbar) oder null
 */
public record Emote(String id, String name, String beschreibung, int farbe, boolean schleife, float dauer,
					Bewegung bewegung, Takt takt, Effekt effekt) {

	/** Jeden Tick, solange das Emote in der Welt läuft. */
	@FunctionalInterface
	public interface Effekt {
		void tick(net.minecraft.client.Minecraft mc, net.minecraft.world.entity.player.Player spieler, float t, int nr);
	}

	/** Setzt die Haltung für den Zeitpunkt t (Sekunden seit dem Start). */
	@FunctionalInterface
	public interface Bewegung {
		void pose(Pose p, float t);
	}

	/** Haltung zum Zeitpunkt t – auf die Grundhaltung aufgesetzt. */
	public Pose pose(float t, Pose ziel) {
		ziel.zuruecksetzen();
		bewegung.pose(ziel, t);
		return ziel;
	}
}
