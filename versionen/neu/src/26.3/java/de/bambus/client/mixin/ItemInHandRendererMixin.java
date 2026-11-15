package de.bambus.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import de.bambus.client.Texturen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Textur-Modul: Low Shield, Side Shield und Short Sword. Seit Minecraft 26.3 zeichnet
 * FirstPersonHandsAndItemsRenderer die Hand – angepasst wird direkt bevor das Item gezeichnet wird.
 */
@Mixin(FirstPersonHandsAndItemsRenderer.class)
public class ItemInHandRendererMixin {

	@Inject(method = "submitArmWithItem", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V"))
	private void bambus$hand(PlayerRenderState spielerZustand, FirstPersonHandsAndItemsRenderState zustand, float a, float b,
							   InteractionHand hand, float c, ItemStack stack, float d, PoseStack pose,
							   SubmitNodeCollector sammler, int licht, CallbackInfo ci) {
		Player spieler = Minecraft.getInstance().player;
		if (spieler == null) return;
		HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? spieler.getMainArm() : spieler.getMainArm().getOpposite();
		ItemDisplayContext kontext = arm == HumanoidArm.LEFT ? ItemDisplayContext.FIRST_PERSON_LEFT_HAND : ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
		Texturen.handAnpassen(spieler, stack, kontext, pose);
	}
}
