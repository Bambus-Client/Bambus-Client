package de.bambus.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * Textur-Modul: PvP-Anpassungen wie in Texturpaketen – Low Fire, Low Shield, Side Shield, Short Sword,
 * kein Kürbis-Overlay und klare Sicht unter Wasser. Nur bei dir sichtbar, ändert nichts am Spiel.
 * Im Mod-Menü eine Karte unter MODULE (Hauptschalter), Einstellungen über ⋮ oder Rechtsklick.
 * Die Mixins (ScreenEffectRendererMixin, ItemInHandRendererMixin, GuiMixin) fragen hier nach.
 */
public final class Texturen {

	/** Stufen für „tiefer“ in Prozent. */
	public static final int MIN_STAERKE = 10;
	public static final int MAX_STAERKE = 80;
	public static final int SCHRITT = 10;

	/** Größen für Short Sword in Prozent. */
	public static final int MIN_SCHWERT = 50;
	public static final int MAX_SCHWERT = 90;

	public static final float FEUER_WEG = 0.5f;    // so weit rutscht das Feuer bei 100 %
	public static final float SCHILD_WEG = 0.5f;
	public static final float SCHILD_SEITE = 0.12f;

	/** Drehung für Side Shield in Grad. */
	public static final int MIN_DREHUNG = 30;
	public static final int MAX_DREHUNG = 90;
	public static final int DREHUNG_SCHRITT = 15;

	private Texturen() {}

	private static BambusConfig cfg() {
		return BambusClient.config;
	}

	/** Hauptschalter der Karte „Texturen“. */
	public static boolean aktiv() {
		return cfg().texturenAn;
	}

	// ------------------------------------------------------------- Einstellungsfenster

	/** Das Textur-Modul im Einstellungsfenster des Mod-Menüs. */
	public static final Einstellbar MODUL = new Einstellbar() {
		@Override
		public String titel() {
			return "Texturen";
		}

		@Override
		public String symbol() {
			return "textur";
		}

		@Override
		public List<Einstellung> einstellungen() {
			BambusConfig c = cfg();
			return List.of(
					new Einstellung.Schalter("Low Fire", () -> c.lowFire, () -> c.lowFire = !c.lowFire),
					new Einstellung.Stufe("Feuer tiefer", () -> c.lowFireStaerke, w -> c.lowFireStaerke = w,
							MIN_STAERKE, MAX_STAERKE, SCHRITT, "−%d %%", () -> c.lowFire),
					new Einstellung.Schalter("Low Shield", () -> c.lowShield, () -> c.lowShield = !c.lowShield),
					new Einstellung.Stufe("Schild tiefer", () -> c.lowShieldStaerke, w -> c.lowShieldStaerke = w,
							MIN_STAERKE, MAX_STAERKE, SCHRITT, "−%d %%", () -> c.lowShield),
					new Einstellung.Schalter("Side Shield", () -> c.sideShield, () -> c.sideShield = !c.sideShield),
					new Einstellung.Stufe("Drehung", () -> c.sideShieldDrehung, w -> c.sideShieldDrehung = w,
							MIN_DREHUNG, MAX_DREHUNG, DREHUNG_SCHRITT, "%d°", () -> c.sideShield),
					new Einstellung.Schalter("Short Sword", () -> c.shortSword, () -> c.shortSword = !c.shortSword),
					new Einstellung.Stufe("Schwert-Größe", () -> c.shortSwordGroesse, w -> c.shortSwordGroesse = w,
							MIN_SCHWERT, MAX_SCHWERT, SCHRITT, "%d %%", () -> c.shortSword),
					new Einstellung.Schalter("Kein Kürbis-Overlay", () -> c.keinKuerbisOverlay, () -> c.keinKuerbisOverlay = !c.keinKuerbisOverlay),
					new Einstellung.Schalter("Klare Sicht im Wasser", () -> c.klaresWasser, () -> c.klaresWasser = !c.klaresWasser));
		}

		@Override
		public void standard() {
			BambusConfig c = cfg();
			BambusConfig neu = new BambusConfig();
			c.lowFire = neu.lowFire;
			c.lowFireStaerke = neu.lowFireStaerke;
			c.lowShield = neu.lowShield;
			c.lowShieldStaerke = neu.lowShieldStaerke;
			c.sideShield = neu.sideShield;
			c.sideShieldDrehung = neu.sideShieldDrehung;
			c.shortSword = neu.shortSword;
			c.shortSwordGroesse = neu.shortSwordGroesse;
			c.keinKuerbisOverlay = neu.keinKuerbisOverlay;
			c.klaresWasser = neu.klaresWasser;
		}
	};

	// ------------------------------------------------------------- Werte für Spiel und Vorschau

	/** Wie weit das Feuer nach unten rutscht (0 = gar nicht, in Bildschirm-Einheiten des Overlays). */
	public static float feuerVersatz() {
		return aktiv() && cfg().lowFire ? FEUER_WEG * cfg().lowFireStaerke / 100f : 0f;
	}

	/** Versatz des Schilds {x, y}; x wird für die linke Hand gespiegelt. */
	public static float[] schildVersatz(boolean linkeHand) {
		BambusConfig c = cfg();
		if (!aktiv()) {
			return new float[]{0f, 0f};
		}
		float x = c.sideShield ? (linkeHand ? -SCHILD_SEITE : SCHILD_SEITE) : 0f;
		float y = c.lowShield ? -SCHILD_WEG * c.lowShieldStaerke / 100f : 0f;
		return new float[]{x, y};
	}

	/** Side Shield: Drehung des Schilds zur Seite in Grad (0 = aus). */
	public static float schildDrehung() {
		return aktiv() && cfg().sideShield ? cfg().sideShieldDrehung : 0f;
	}

	/** Größe des Schwerts in der Hand (1 = normal). */
	public static float schwertGroesse() {
		return aktiv() && cfg().shortSword ? cfg().shortSwordGroesse / 100f : 1f;
	}

	// ------------------------------------------------------------- Aufrufe aus den Mixins

	/** Low Fire: verschiebt das Feuer am Bildschirmrand nach unten. */
	public static void feuerVerschieben(PoseStack pose) {
		float weg = feuerVersatz();
		if (weg > 0f) {
			pose.translate(0f, -weg, 0f);
		}
	}

	/** Side Shield je Hand (0 = rechts, 1 = links): 1 = ganz zur Seite, 0 = normale Block-Position. */
	private static final float[] SEITE_ANTEIL = {1f, 1f};
	private static final long[] SEITE_ZEIT = {0L, 0L};
	/** So schnell gleitet der Schild beim Blocken in die Mitte und danach zurück (Anteil pro Sekunde). */
	private static final float SEITE_TEMPO = 9f;

	/** Gegenstand in der eigenen Hand anpassen (nur 1. Person): Low/Side Shield und Short Sword. */
	public static void handAnpassen(LivingEntity wesen, ItemStack stack, ItemDisplayContext kontext, PoseStack pose) {
		if (!aktiv() || !kontext.firstPerson()) {
			return;
		}
		if (stack.is(Items.SHIELD)) {
			boolean links = kontext.leftHand();
			float[] v = schildVersatz(links);
			// Side Shield: beim Blocken wie ein normaler Schild (gerade vor dir), danach wieder zur Seite
			float seite = seitenAnteil(links, blocktMit(wesen, links));
			pose.translate(v[0] * seite, v[1], 0f);
			float drehung = schildDrehung() * seite;
			if (drehung > 0f) {   // wie im Texturpaket: Schild zur Seite gedreht, man sieht nur noch die Kante
				pose.mulPose(Axis.YP.rotationDegrees(links ? -drehung : drehung));   // Fläche nach außen
			}
		} else if (stack.is(ItemTags.SWORDS)) {
			float f = schwertGroesse();
			if (f < 1f) {   // kleiner und etwas nach unten, damit der Griff in der Hand bleibt
				pose.translate(0f, -(1f - f) * 0.15f, 0f);
				pose.scale(f, f, f);
			}
		}
	}

	/** Blockt der Spieler gerade mit dem Schild in der Hand auf dieser Bildschirmseite? */
	private static boolean blocktMit(LivingEntity wesen, boolean linkeSeite) {
		if (wesen == null || !wesen.isUsingItem() || !wesen.getUseItem().is(Items.SHIELD)) {
			return false;
		}
		boolean hauptHandLinks = wesen.getMainArm() == HumanoidArm.LEFT;
		InteractionHand hand = linkeSeite == hauptHandLinks ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
		return wesen.getUsedItemHand() == hand;
	}

	/** Weicher Übergang zwischen Seite (1) und Block-Position (0), damit der Schild nicht springt. */
	private static float seitenAnteil(boolean links, boolean blockt) {
		int i = links ? 1 : 0;
		long jetzt = System.nanoTime();
		float dt = SEITE_ZEIT[i] == 0L ? 1f : Math.min(0.1f, (jetzt - SEITE_ZEIT[i]) / 1e9f);
		SEITE_ZEIT[i] = jetzt;
		float ziel = blockt ? 0f : 1f;
		float schritt = SEITE_TEMPO * dt;
		float a = SEITE_ANTEIL[i];
		a = a < ziel ? Math.min(ziel, a + schritt) : Math.max(ziel, a - schritt);
		SEITE_ANTEIL[i] = a;
		return a * a * (3f - 2f * a);   // weich ein- und ausblenden
	}

	/** Kein Kürbis-Overlay: true, wenn dieses Bildschirm-Overlay ausgeblendet werden soll. */
	public static boolean overlayAusblenden(Identifier textur) {
		return aktiv() && cfg().keinKuerbisOverlay && textur.getPath().contains("pumpkinblur");
	}

	/** Klare Sicht unter Wasser: blendet die Wasser-Textur vor dem Auge aus. */
	public static boolean wasserAusblenden() {
		return aktiv() && cfg().klaresWasser;
	}
}
