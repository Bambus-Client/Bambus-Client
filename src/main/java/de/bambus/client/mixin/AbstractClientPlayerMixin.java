package de.bambus.client.mixin;

import de.bambus.client.Kosmetik;
import de.bambus.client.render.CapeLayer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.ClientAsset;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Bambus-Capes: Der Umhang selbst wird seit 3.3 vom CapeLayer gezeichnet (wehend, animiert).
 * Hier wird das normale Minecraft-Cape ausgeblendet und das Design auf die Elytra gelegt –
 * bei dir und bei anderen Bambus-Spielern, die ein Bambus-Cape tragen.
 */
@Mixin(AbstractClientPlayer.class)
public class AbstractClientPlayerMixin {
	@Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
	private void bambus$cape(CallbackInfoReturnable<PlayerSkin> cir) {
		ClientAsset.Texture cape = Kosmetik.capeTextur(CapeLayer.capeVon((AbstractClientPlayer) (Object) this));
		PlayerSkin skin = cir.getReturnValue();
		if (cape == null || skin == null) {
			return;
		}
		cir.setReturnValue(new PlayerSkin(skin.body(), null, cape, skin.model(), skin.secure()));
	}
}
