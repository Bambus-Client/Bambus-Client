package de.bambus.client.emote;

/**
 * Zusatz am Zeichenzustand eines Spielers (AvatarRenderState, per Mixin): welches Emote dieses Modell gerade zeigt
 * und seit wie vielen Sekunden. So kann jedes Vorschau-Modell im Menü ein anderes Emote tanzen.
 */
public interface EmoteZustand {
	String bambus$emote();

	float bambus$zeit();

	void bambus$setze(String emote, float zeit);
}
