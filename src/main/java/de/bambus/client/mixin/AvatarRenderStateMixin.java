package de.bambus.client.mixin;

import de.bambus.client.emote.EmoteZustand;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** Merkt sich am Zeichenzustand, welches Emote dieses Spielermodell gerade zeigt. */
@Mixin(AvatarRenderState.class)
public class AvatarRenderStateMixin implements EmoteZustand {
	@Unique
	private String bambus$emote;
	@Unique
	private float bambus$zeit;

	@Override
	public String bambus$emote() {
		return bambus$emote;
	}

	@Override
	public float bambus$zeit() {
		return bambus$zeit;
	}

	@Override
	public void bambus$setze(String emote, float zeit) {
		bambus$emote = emote;
		bambus$zeit = zeit;
	}
}
