package de.bambus.client.mixin;

import de.bambus.client.BambusClient;
import de.bambus.client.screen.BambusServerScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Die Minecraft-Serverliste durch die Bambus-Serverauswahl ersetzen (abschaltbar im Mod-Menü). */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

	@ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
	private Screen bambus$serverMenue(Screen screen) {
		if (screen != null && screen.getClass() == JoinMultiplayerScreen.class) {
			if (BambusServerScreen.vanillaEinmal) {
				BambusServerScreen.vanillaEinmal = false;
				return screen;
			}
			if (BambusClient.config != null && BambusClient.config.serverMenue) {
				return new BambusServerScreen(((JoinMultiplayerScreenAccessor) screen).bambus$lastScreen());
			}
		}
		return screen;
	}
}
