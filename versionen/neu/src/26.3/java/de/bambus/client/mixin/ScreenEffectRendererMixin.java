package de.bambus.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import de.bambus.client.Texturen;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Textur-Modul: Low Fire und klare Sicht unter Wasser (ab Minecraft 26.3: submitWater mit WaterOverlay). */
@Mixin(ScreenEffectRenderer.class)
public class ScreenEffectRendererMixin {

	@Inject(method = "submitFire", at = @At("HEAD"))
	private static void bambus$feuerTiefer(PoseStack pose, SubmitNodeCollector sammler, TextureAtlasSprite bild, CallbackInfo ci) {
		pose.pushPose();
		Texturen.feuerVerschieben(pose);
	}

	@Inject(method = "submitFire", at = @At("RETURN"))
	private static void bambus$feuerZurueck(PoseStack pose, SubmitNodeCollector sammler, TextureAtlasSprite bild, CallbackInfo ci) {
		pose.popPose();
	}

	@Inject(method = "submitWater", at = @At("HEAD"), cancellable = true)
	private static void bambus$klaresWasser(PlayerRenderState.WaterOverlay wasser, PoseStack pose, SubmitNodeCollector sammler, CallbackInfo ci) {
		if (Texturen.wasserAusblenden()) {
			ci.cancel();
		}
	}
}
