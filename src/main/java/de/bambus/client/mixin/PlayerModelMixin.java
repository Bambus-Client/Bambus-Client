package de.bambus.client.mixin;

import de.bambus.client.emote.Emotes;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Emotes: nach Minecrafts eigener Animation die Gelenke des eigenen Spielers in die Emote-Haltung bringen. */
@Mixin(PlayerModel.class)
public class PlayerModelMixin {
	@Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("TAIL"))
	private void bambus$emote(AvatarRenderState zustand, CallbackInfo ci) {
		Emotes.anwenden((PlayerModel) (Object) this, zustand);
	}
}
