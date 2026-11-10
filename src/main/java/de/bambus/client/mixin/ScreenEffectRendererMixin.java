package de.bambus.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import de.bambus.client.Texturen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Textur-Modul: Low Fire und klare Sicht unter Wasser. */
@Mixin(ScreenEffectRenderer.class)
public class ScreenEffectRendererMixin {

	@Inject(method = "renderFire", at = @At("HEAD"))
	private static void bambus$feuerTiefer(PoseStack pose, MultiBufferSource quelle, TextureAtlasSprite bild, CallbackInfo ci) {
		pose.pushPose();
		Texturen.feuerVerschieben(pose);
	}

	@Inject(method = "renderFire", at = @At("RETURN"))
	private static void bambus$feuerZurueck(PoseStack pose, MultiBufferSource quelle, TextureAtlasSprite bild, CallbackInfo ci) {
		pose.popPose();
	}

	@Inject(method = "renderWater", at = @At("HEAD"), cancellable = true)
	private static void bambus$klaresWasser(Minecraft mc, PoseStack pose, MultiBufferSource quelle, CallbackInfo ci) {
		if (Texturen.wasserAusblenden()) {
			ci.cancel();
		}
	}
}
