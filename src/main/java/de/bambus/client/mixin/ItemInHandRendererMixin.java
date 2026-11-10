package de.bambus.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import de.bambus.client.Texturen;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Textur-Modul: Low Shield und Side Shield (der Aufrufer stellt die Pose danach wieder her). */
@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin {

	@Inject(method = "renderItem", at = @At("HEAD"))
	private void bambus$schild(LivingEntity wesen, ItemStack stack, ItemDisplayContext kontext, PoseStack pose,
							   SubmitNodeCollector sammler, int licht, CallbackInfo ci) {
		Texturen.schildVerschieben(stack, kontext, pose);
	}
}
