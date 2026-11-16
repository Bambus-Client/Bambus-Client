package de.bambus.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import de.bambus.client.emote.Emotes;
import de.bambus.client.emote.Pose;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Emotes: den ganzen Körper drehen, hüpfen lassen und neigen (um die Hüfte). */
@Mixin(AvatarRenderer.class)
public class AvatarRendererMixin {
	@Inject(method = "setupRotations(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;FF)V", at = @At("TAIL"))
	private void bambus$emote(AvatarRenderState zustand, PoseStack pose, float koerperDrehung, float skalierung, CallbackInfo ci) {
		Pose p = Emotes.poseFuer(zustand.id);
		if (p == null) return;
		pose.translate(0, p.hoehe, 0);
		if (p.drehung != 0) pose.mulPose(Axis.YP.rotationDegrees(p.drehung));
		if (p.neigung != 0) {   // um die Hüfte (0,75 Blöcke über den Füßen) nach vorn neigen
			pose.translate(0, 0.75f, 0);
			pose.mulPose(Axis.XP.rotationDegrees(p.neigung));
			pose.translate(0, -0.75f, 0);
		}
	}
}
