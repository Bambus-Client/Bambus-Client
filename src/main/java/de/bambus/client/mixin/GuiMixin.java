package de.bambus.client.mixin;

import de.bambus.client.Texturen;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Textur-Modul: Kürbis-Overlay ausblenden. */
@Mixin(Gui.class)
public class GuiMixin {

	@Inject(method = "renderTextureOverlay", at = @At("HEAD"), cancellable = true)
	private void bambus$overlay(GuiGraphics g, Identifier textur, float deckkraft, CallbackInfo ci) {
		if (Texturen.overlayAusblenden(textur)) {
			ci.cancel();
		}
	}
}
