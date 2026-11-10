package de.bambus.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Textur-Modul: PvP-Anpassungen wie in Texturpaketen – Low Fire, Low Shield, Side Shield,
 * kein Kürbis-Overlay und klare Sicht unter Wasser. Nur bei dir sichtbar, ändert nichts am Spiel.
 * Die Mixins (ScreenEffectRendererMixin, ItemInHandRendererMixin, GuiMixin) fragen hier nach.
 */
public final class Texturen {

	/** Stufen für „tiefer“ in Prozent (Mod-Menü → TEXTUREN). */
	public static final int MIN_STAERKE = 10;
	public static final int MAX_STAERKE = 80;
	public static final int SCHRITT = 10;

	private static final float FEUER_WEG = 0.5f;    // so weit rutscht das Feuer bei 100 %
	private static final float SCHILD_WEG = 0.5f;
	private static final float SCHILD_SEITE = 0.18f;

	private Texturen() {}

	private static BambusConfig cfg() {
		return BambusClient.config;
	}

	public static int naechsteStaerke(int wert, int richtung) {
		return Math.max(MIN_STAERKE, Math.min(MAX_STAERKE, wert + richtung * SCHRITT));
	}

	/** Low Fire: verschiebt das Feuer am Bildschirmrand nach unten. */
	public static void feuerVerschieben(PoseStack pose) {
		if (cfg().lowFire) {
			pose.translate(0f, -FEUER_WEG * cfg().lowFireStaerke / 100f, 0f);
		}
	}

	/** Low Shield / Side Shield: verschiebt den Schild in der eigenen Hand (nur 1. Person). */
	public static void schildVerschieben(ItemStack stack, ItemDisplayContext kontext, PoseStack pose) {
		BambusConfig c = cfg();
		if ((!c.lowShield && !c.sideShield) || !kontext.firstPerson() || !stack.is(Items.SHIELD)) {
			return;
		}
		float x = c.sideShield ? (kontext.leftHand() ? -SCHILD_SEITE : SCHILD_SEITE) : 0f;
		float y = c.lowShield ? -SCHILD_WEG * c.lowShieldStaerke / 100f : 0f;
		pose.translate(x, y, 0f);
	}

	/** Kein Kürbis-Overlay: true, wenn dieses Bildschirm-Overlay ausgeblendet werden soll. */
	public static boolean overlayAusblenden(Identifier textur) {
		return cfg().keinKuerbisOverlay && textur.getPath().contains("pumpkinblur");
	}

	/** Klare Sicht unter Wasser: blendet die Wasser-Textur vor dem Auge aus. */
	public static boolean wasserAusblenden() {
		return cfg().klaresWasser;
	}
}
