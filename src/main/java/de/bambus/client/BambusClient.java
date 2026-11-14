package de.bambus.client;

import com.mojang.blaze3d.platform.InputConstants;
import de.bambus.client.hud.*;
import de.bambus.client.screen.BambusMenuScreen;
import de.bambus.client.screen.BambusTitelScreen;
import de.bambus.client.screen.BambusAuswahlScreen;
import de.bambus.client.screen.GarderobeScreen;
import de.bambus.client.screen.NeonKarte;
import de.bambus.client.screen.HudEditorScreen;
import de.bambus.client.screen.Neon;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.world.InteractionResult;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import de.bambus.client.render.CapeLayer;
import de.bambus.client.render.FluegelLayer;
import de.bambus.client.render.HutLayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * Bambus Client – ein leichter PvP-/HUD-Client für Minecraft 1.21.11 (Fabric).
 *
 * Tasten (änderbar unter Optionen → Steuerung):
 *  Rechts-Shift  Auswahl: Garderobe oder Mod-Menü
 *  C             Zoom (mit Mausrad näher/weiter)
 *  Linke Alt     Freelook
 *  H             HUD an/aus
 */
public class BambusClient implements ClientModInitializer {

	public static final String MOD_ID = "bambus";
	public static final String VERSION = "3.4.0";
	private static final String FENSTERTITEL = "Bambus Client " + VERSION + " – Minecraft 1.21.11";

	public static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("Bambus");

	public static BambusConfig config = new BambusConfig();
	private static int screenshotIn = 0;

	public static final List<HudModul> MODULE = List.of(
			new TitelModul(),
			new FpsModul(),
			new CpsModul(),
			new TastenModul(),
			new ServerModul(),
			new PingModul(),
			new ToggleSprintModul(),
			new RuestungModul(),
			new HandModul(),
			new EffekteModul(),
			new ZielModul(),
			new ComboModul(),
			new ReachModul(),
			new TapModul(),
			new KillModul(),
			new PfeileModul(),
			new CooldownModul(),
			new WeltModul(WeltModul.Art.RICHTUNG),
			new WeltModul(WeltModul.Art.BIOM),
			new WeltModul(WeltModul.Art.TEMPO),
			new WeltModul(WeltModul.Art.UHR),
			new WeltModul(WeltModul.Art.SPEICHER),
			new TntModul(),
			new KompassModul(),
			new KoordinatenModul(),
			new SpielzeitModul(),
			new IngameTagModul(),
			new MusikModul()
	);

	private static KeyMapping menuTaste;
	private static KeyMapping zoomTaste;
	private static KeyMapping freelookTaste;
	private static KeyMapping hudTaste;
	private static KeyMapping koordinatenTaste;
	private static KeyMapping musikPause, musikWeiter, musikZurueck;

	private static boolean ersterTick = true;

	@Override
	public void onInitializeClient() {
		config = BambusConfig.laden();
		ModulAufteilung.uebernehmen(config);

		KeyMapping.Category kategorie = KeyMapping.Category.register(
				Identifier.fromNamespaceAndPath(MOD_ID, "bambus"));
		menuTaste = taste("menu", InputConstants.KEY_RSHIFT, kategorie);
		zoomTaste = taste("zoom", InputConstants.KEY_C, kategorie);
		freelookTaste = taste("freelook", InputConstants.KEY_LALT, kategorie);
		hudTaste = taste("hud", InputConstants.KEY_H, kategorie);
		koordinatenTaste = taste("koordinaten", InputConstants.UNKNOWN.getValue(), kategorie);   // frei belegbar
		musikPause = taste("musik_pause", InputConstants.UNKNOWN.getValue(), kategorie);
		musikWeiter = taste("musik_weiter", InputConstants.UNKNOWN.getValue(), kategorie);
		musikZurueck = taste("musik_zurueck", InputConstants.UNKNOWN.getValue(), kategorie);

		ClientTickEvents.END_CLIENT_TICK.register(BambusClient::tick);

		// Quests: Welt betreten und Nachrichten mitlesen (Siege erkennen)
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> Blaetter.weltBetreten());
		ClientReceiveMessageEvents.GAME.register((nachricht, ueberlagerung) -> Blaetter.nachricht(nachricht.getString()));
		ClientReceiveMessageEvents.CHAT.register((nachricht, unterschrieben, absender, art, zeit) ->
				Blaetter.nachricht(nachricht.getString()));
		Statistik.laden();
		Kampf.killHoerer = (opfer, spieler) -> {
			Kosmetik.killEffekt(opfer);
			if (spieler && config.autoScreenshot) {
				screenshotIn = 6; // ein paar Ticks warten, damit der Kill-Effekt mit drauf ist
			}
		};
		Anzeigen.registrieren();
		Ladebildschirm.registrieren();
		// Eigene Treffer auswerten (Combo, Reach, Target-HUD, Kills) – nur lesen, nichts verändern
		AttackEntityCallback.EVENT.register((spieler, welt, hand, entity, treffer) -> {
			if (welt.isClientSide()) {
				Kampf.angriff(spieler, entity);
			}
			return InteractionResult.PASS;
		});
		// Eigene Capes/Flügel aus config/bambus-kosmetik/ laden, sobald die Texturen bereit sind
		ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> Musik.stoppe());
		ClientLifecycleEvents.CLIENT_STARTED.register(mc -> {
			EigeneKosmetik.neuLaden();
			Hinweise.versionPruefen();
			serverFavoritenEintragen(mc);
		});

		// Flügel als zusätzliche Ebene am Spieler-Modell
		LivingEntityFeatureRendererRegistrationCallback.EVENT.register(BambusClient::registriereFluegel);

		HudElementRegistry.attachElementBefore(
				VanillaHudElements.CHAT,
				Identifier.fromNamespaceAndPath(MOD_ID, "hud"),
				BambusClient::zeichneHud);

		HudElementRegistry.replaceElement(VanillaHudElements.CROSSHAIR, vanilla -> (g, delta) -> {
			if (config.fadenkreuzAn) {
				Fadenkreuz.zeichne(g, Minecraft.getInstance());
			} else {
				vanilla.render(g, delta);
			}
		});

		ScreenEvents.AFTER_INIT.register((client, screen, breite, hoehe) ->
				ScreenEvents.afterRender(screen).register((s, grafik, mausX, mausY, d) -> Hinweise.zeichne(grafik, client)));
		ScreenEvents.AFTER_INIT.register((client, screen, breite, hoehe) -> {
			if (screen instanceof TitleScreen) {
				Screens.getButtons(screen).add(Button.builder(Component.literal("§bBAMBUS"),
								b -> client.setScreen(new BambusMenuScreen(screen)))
						.bounds(breite - 84, 6, 78, 20).build());
				ScreenEvents.afterRender(screen).register(
						(s, grafik, mausX, mausY, d) -> zeichneTitelLogo(grafik));
			}
			// Escape-Menü: Avatar-Karte „Garderobe“ an der linken Seite
			if (screen instanceof PauseScreen pause && pause.showsPauseMenu()) {
				int h = Math.max(70, Math.min(140, hoehe - 40));
				Screens.getButtons(screen).add(new NeonKarte(10, (hoehe - h) / 2, 96, h, "GARDEROBE", "KOSMETIK ANPASSEN",
						NeonKarte.AVATAR, () -> client.setScreen(new GarderobeScreen(screen))));
			}
		});
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static void registriereFluegel(EntityType<? extends LivingEntity> typ, LivingEntityRenderer<?, ?, ?> renderer,
										   LivingEntityFeatureRendererRegistrationCallback.RegistrationHelper helfer,
										   EntityRendererProvider.Context kontext) {
		if (renderer instanceof AvatarRenderer) {
			helfer.register(new CapeLayer((AvatarRenderer) renderer));
			helfer.register(new FluegelLayer((AvatarRenderer) renderer));
			helfer.register(new HutLayer((AvatarRenderer) renderer));
		}
	}

	/** Favoriten aus dem Launcher in die Mehrspieler-Liste übernehmen (nur fehlende, nichts wird gelöscht). */
	private static void serverFavoritenEintragen(Minecraft mc) {
		if (config.serverFavoriten == null || config.serverFavoriten.isEmpty()) return;
		try {
			net.minecraft.client.multiplayer.ServerList liste = new net.minecraft.client.multiplayer.ServerList(mc);
			liste.load();
			boolean neu = false;
			for (String eintrag : config.serverFavoriten) {
				String[] t = eintrag.split("\\|", 2);
				String name = t[0].trim(), adresse = (t.length > 1 ? t[1] : t[0]).trim();
				if (adresse.isEmpty() || liste.get(adresse) != null) continue;
				liste.add(new net.minecraft.client.multiplayer.ServerData(name.isEmpty() ? adresse : name, adresse,
						net.minecraft.client.multiplayer.ServerData.Type.OTHER), false);
				// Launcher-Favoriten bekommen in der Bambus-Serverauswahl gleich einen Stern
				String stern = adresse.toLowerCase(java.util.Locale.ROOT);
				if (!config.serverSterne.contains(stern)) config.serverSterne.add(stern);
				neu = true;
			}
			if (neu) {
				liste.save();
				config.speichern();
			}
		} catch (Exception e) {
			LOGGER.warn("Server-Favoriten konnten nicht eingetragen werden", e);
		}
	}

	public static ToggleSprintModul toggleSprint() {
		for (HudModul m : MODULE) if (m instanceof ToggleSprintModul t) return t;
		throw new IllegalStateException("Toggle-Sprint-Modul fehlt");
	}

	/** Das frühere Modul „Ping-Verlauf“ ist jetzt das Modul „Ping“ (mit Verlauf) – Position und Größe mitnehmen. */
	private static void pingVerlaufUebernehmen() {
		BambusConfig.ModulPos alt = config.module.remove(PingModul.ALTE_ID);
		if (alt == null || !alt.an || config.module.containsKey(PingModul.ID)) return;
		BambusConfig.ModulPos neu = new BambusConfig.ModulPos(alt.x, alt.y);
		neu.an = true;
		neu.groesse = alt.groesse;
		neu.optionen.putAll(alt.optionen == null ? java.util.Map.of() : alt.optionen);
		neu.optionen.put("verlauf", true);
		config.module.put(PingModul.ID, neu);
		config.speichern();
	}

	private static KeyMapping taste(String name, int taste, KeyMapping.Category kategorie) {
		return KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.bambus." + name, InputConstants.Type.KEYSYM, taste, kategorie));
	}

	// ------------------------------------------------------------------ Tick

	private static void tick(Minecraft mc) {
		mc.getWindow().setTitle(FENSTERTITEL);
		if (mc.level != null && mc.player != null) {
			Blaetter.tick(mc);   // Spielzeit, Blätter und die Herausforderungen des Tages
		}

		// Eigenes Bambus-Hauptmenü statt des Minecraft-Titelbildschirms
		if (mc.screen instanceof TitleScreen && config.bambusHauptmenue && !BambusTitelScreen.vanillaGewuenscht) {
			mc.setScreen(new BambusTitelScreen());
		}

		if (ersterTick) {
			ersterTick = false;
			if (config.toggleSprint || config.toggleSneak) {
				Funktionen.sprintUndSchleichenAnwenden(mc);
			}
			toggleSprint().ausConfigUebernehmen(config);
			pingVerlaufUebernehmen();
			// Low-End-Modus kann auch im Launcher geschaltet werden
			if (config.lowEnd != (config.lowEndAlt != null)) {
				Visuals.lowEnd(mc, config.lowEnd);
			}
		}

		toggleSprint().anwenden(mc);

		while (menuTaste.consumeClick()) {
			if (mc.screen == null) {
				mc.setScreen(new BambusAuswahlScreen());   // erst wählen: Garderobe oder Mod-Menü
			}
		}

		while (hudTaste.consumeClick()) {
			config.hudAn = !config.hudAn;
			config.speichern();
			Hinweise.zeige("hud", "Bambus-HUD", config.hudAn ? "eingeschaltet" : "ausgeschaltet", Farben.NEON);
			if (mc.player != null) {
				mc.player.displayClientMessage(
						Component.literal("§aBambus-HUD: " + (config.hudAn ? "an" : "aus")), true);
			}
		}

		Funktionen.zoomTick(mc, zoomTaste.isDown());
		Funktionen.freelookTick(mc, freelookTaste.isDown());
		Funktionen.fullbrightTick(mc);
		Kosmetik.tick(mc);
		Kampf.tick(mc);
		Visuals.tick(mc);
		WeltModul.tick(mc);
		SpielzeitModul.tick(mc);
		PingModul.tick(mc);
		for (HudModul m : MODULE) {
			if (m instanceof MusikModul musik) musik.tick();
		}
		while (musikPause.consumeClick()) Musik.pause();
		while (musikWeiter.consumeClick()) Musik.weiter();
		while (musikZurueck.consumeClick()) Musik.zurueck();
		while (koordinatenTaste.consumeClick()) {
			String k = KoordinatenModul.alsText(mc);
			if (!k.isEmpty()) {
				mc.keyboardHandler.setClipboard(k);
				Hinweise.zeige("richtung", "Koordinaten kopiert", k, Farben.NEON);
			}
		}
		Statistik.tick(mc.level != null);
		DiscordStatus.tick(mc);
		Hinweise.tick(mc);
		if (screenshotIn > 0 && --screenshotIn == 0 && mc.player != null) {
			net.minecraft.client.Screenshot.grab(mc.gameDirectory, mc.getMainRenderTarget(), nachricht -> {
				mc.player.displayClientMessage(nachricht, false);
				Hinweise.zeige("kamera", "Screenshot gespeichert", "Kill-Screenshot im Ordner screenshots", Farben.NEON_CYAN);
			});
		}
	}

	// ------------------------------------------------------------------- HUD

	private static void zeichneHud(GuiGraphics g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		Farben.themaAnwenden(config.thema);
		Funktionen.zaehleKlicks(mc);

		if (mc.screen == null && !mc.options.hideGui) {
			Hinweise.zeichne(g, mc);   // über Menüs zeichnet sie ScreenEvents.afterRender
		}
		if (!config.hudAn || mc.player == null || mc.options.hideGui || mc.screen instanceof HudEditorScreen) {
			return;
		}
		for (HudModul m : MODULE) {   // Songtext-Wörter liegen hinter allen Anzeigen
			if (m instanceof MusikModul musik) musik.welt(g, mc);
		}
		for (HudModul m : MODULE) {
			if (m.pos().an) {
				m.zeichneAnPosition(g, mc, false);
			}
		}
	}

	// ---------------------------------------------------------- Titelbildschirm

	private static void zeichneTitelLogo(GuiGraphics g) {
		Font font = Minecraft.getInstance().font;
		g.fill(4, 4, 150, 42, 0xA0050A10);
		Neon.ecken(g, 4, 4, 146, 38, 8, Farben.NEON_CYAN);
		Neon.logo(g, 12, 11, 24);
		Neon.leuchtText(g, font, "BAMBUS", 30, 11, 2.0f, Farben.NEON);
		Neon.text(g, font, "CLIENT  v" + VERSION, 31, 30, 0.75f, Farben.NEON_CYAN, false);
	}
}
