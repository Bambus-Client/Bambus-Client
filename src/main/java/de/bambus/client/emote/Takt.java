package de.bambus.client.emote;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

/**
 * Ein kleiner, eigener Beat aus Notenblock-Klängen (nur für dich hörbar). Jede Spur hat ein Muster mit einem
 * Zeichen pro Achtel: „x“ = Schlag, „.“ = Pause; die Bass- und Melodie-Spur nutzt Buchstaben als Tonhöhe
 * (a = tiefster Ton, jeder weitere Buchstabe einen Halbton höher).
 *
 * @param ticksProAchtel  Tempo: 5 = 120 Schläge pro Minute, 6 = 100, 4 = 150
 */
public record Takt(int ticksProAchtel, String kick, String snare, String hat, String bass, String melodie) {

	/** Spielt, was im Schritt (Achtel) fällig ist. */
	void spiele(int schritt) {
		ton(SoundEvents.NOTE_BLOCK_BASEDRUM, kick, schritt, 1f, 0.55f);
		ton(SoundEvents.NOTE_BLOCK_SNARE, snare, schritt, 1f, 0.4f);
		ton(SoundEvents.NOTE_BLOCK_HAT, hat, schritt, 1.4f, 0.22f);
		ton(SoundEvents.NOTE_BLOCK_BASS, bass, schritt, -1f, 0.6f);
		ton(SoundEvents.NOTE_BLOCK_BIT, melodie, schritt, -1f, 0.25f);
	}

	private static void ton(Holder<SoundEvent> klang, String muster, int schritt, float hoehe, float lautstaerke) {
		if (muster == null || muster.isEmpty()) return;
		char c = muster.charAt(schritt % muster.length());
		if (c == '.') return;
		float tonhoehe = hoehe > 0 ? hoehe : (float) Math.pow(2, (Character.toLowerCase(c) - 'a' - 12) / 12.0);
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(klang.value(), tonhoehe, lautstaerke));
	}
}
