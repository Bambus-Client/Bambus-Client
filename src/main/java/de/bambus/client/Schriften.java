package de.bambus.client;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

/**
 * Wählbare Schriftarten (assets/bambus/font): die Minecraft-Schrift und drei freie Schriften
 * unter der SIL Open Font License (Lizenztexte liegen daneben).
 */
public final class Schriften {

	private Schriften() {}

	public static final String[] NAMEN = {"Minecraft", "Poppins", "Montserrat", "Pixel"};
	private static final String[] DATEI = {null, "poppins", "montserrat", "monocraft"};
	private static final FontDescription[] SCHRIFT = new FontDescription[DATEI.length];

	static {
		for (int i = 0; i < DATEI.length; i++) {
			SCHRIFT[i] = DATEI[i] == null ? null
					: new FontDescription.Resource(Identifier.fromNamespaceAndPath(BambusClient.MOD_ID, DATEI[i]));
		}
	}

	/** Text in der gewählten Schrift; „fett“ gilt nur für die Minecraft-Schrift (die anderen sind schon kräftig). */
	public static Component text(String text, int schrift, boolean fett) {
		int i = Math.floorMod(schrift, NAMEN.length);
		MutableComponent c = Component.literal(text);
		Style stil = SCHRIFT[i] != null ? Style.EMPTY.withFont(SCHRIFT[i]) : Style.EMPTY.withBold(fett);
		return c.setStyle(stil);
	}
}
