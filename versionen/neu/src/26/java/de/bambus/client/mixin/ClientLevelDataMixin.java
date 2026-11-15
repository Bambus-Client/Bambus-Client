package de.bambus.client.mixin;

import de.bambus.client.Visuals;
import net.minecraft.client.ClientClockManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Eigene Tageszeit – nur auf deinem Bildschirm, der Server merkt davon nichts.
 * Seit Minecraft 26.1 kommt die Zeit aus den Weltuhren des Clients (ClientClockManager).
 */
@Mixin(ClientClockManager.class)
public class ClientLevelDataMixin {
	@Inject(method = "getTotalTicks", at = @At("HEAD"), cancellable = true)
	private void bambus$eigeneZeit(CallbackInfoReturnable<Long> cir) {
		long zeit = Visuals.eigeneZeit();
		if (zeit >= 0) {
			cir.setReturnValue(zeit);
		}
	}
}
