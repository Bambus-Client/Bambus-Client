package de.bambus.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import de.bambus.client.BambusClient;
import de.bambus.client.BambusConfig;
import de.bambus.client.Farben;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DirectJoinServerScreen;
import net.minecraft.client.gui.screens.FaviconTexture;
import net.minecraft.client.gui.screens.ManageServerScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.ServerStatusPinger;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.EventLoopGroupHolder;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Bambus-Serverauswahl: Server als Karten mit Icon, Beschreibung, Ping und Spielerzahl,
 * Favoriten (★, stehen immer oben, eigener Tab) und Suche. Ersetzt die Minecraft-Liste
 * (abschaltbar im Mod-Menü); über „Minecraft-Liste“ kommt man für LAN-Welten zur normalen.
 */
public class BambusServerScreen extends Screen {

	/** Die nächste Minecraft-Serverliste nicht ersetzen (Knopf „Minecraft-Liste“). */
	public static boolean vanillaEinmal = false;

	private static final ExecutorService PING = Executors.newFixedThreadPool(5, r -> {
		Thread t = new Thread(r, "Bambus-Server-Ping");
		t.setDaemon(true);
		return t;
	});

	private record Bereich(int x, int y, int b, int h, Runnable aktion, boolean inListe) {
		Bereich(int x, int y, int b, int h, Runnable aktion) {
			this(x, y, b, h, aktion, false);
		}
	}

	private final Screen eltern;
	private final ServerStatusPinger pinger = new ServerStatusPinger();
	private final Map<ServerData, FaviconTexture> icons = new HashMap<>();
	private final Map<ServerData, byte[]> hochgeladen = new HashMap<>();
	private final List<Bereich> bereiche = new ArrayList<>();
	private ServerList liste;
	private List<ServerData> sichtbar = new ArrayList<>();
	private ServerData ausgewaehlt;
	private ServerData bearbeitet;
	private EditBox suche;
	private boolean nurFavoriten;
	private double scroll;
	private int inhaltHoehe;
	private long letzterKlick;
	private ServerData letzterKlickAuf;

	// Listenbereich (wird in render berechnet)
	private int lx, ly, lb, lh;
	private static final int KARTE = 40;

	public BambusServerScreen(Screen eltern) {
		super(Component.literal("Server"));
		this.eltern = eltern;
	}

	private BambusConfig cfg() {
		return BambusClient.config;
	}

	// ================================================================ Aufbau

	@Override
	protected void init() {
		if (liste == null) {
			liste = new ServerList(minecraft);
			liste.load();
			pingeAlle();
		}
		String alt = suche == null ? "" : suche.getValue();
		suche = new EditBox(font, 0, 0, 140, 16, Component.literal("Suche"));
		suche.setHint(Component.literal("§8Suchen …"));
		suche.setBordered(false);
		suche.setTextColor(Farben.WEISS);
		suche.setValue(alt);
		suche.setResponder(s -> {
			scroll = 0;
			filtern();
		});
		addRenderableWidget(suche);
		filtern();
	}

	/** Sichtbare Server: Suche und Favoriten-Tab beachten; Favoriten stehen immer oben. */
	private void filtern() {
		String s = suche == null ? "" : suche.getValue().trim().toLowerCase(Locale.ROOT);
		List<ServerData> fav = new ArrayList<>(), rest = new ArrayList<>();
		for (int i = 0; i < liste.size(); i++) {
			ServerData d = liste.get(i);
			if (!s.isEmpty() && !d.name.toLowerCase(Locale.ROOT).contains(s) && !d.ip.toLowerCase(Locale.ROOT).contains(s)) continue;
			if (istFavorit(d)) fav.add(d);
			else if (!nurFavoriten) rest.add(d);
		}
		sichtbar = new ArrayList<>(fav);
		sichtbar.addAll(rest);
		if (ausgewaehlt != null && !sichtbar.contains(ausgewaehlt)) ausgewaehlt = null;
	}

	private boolean istFavorit(ServerData d) {
		return cfg().serverSterne.contains(d.ip.toLowerCase(Locale.ROOT));
	}

	private void favoritUmschalten(ServerData d) {
		String ip = d.ip.toLowerCase(Locale.ROOT);
		if (!cfg().serverSterne.remove(ip)) cfg().serverSterne.add(ip);
		cfg().speichern();
		filtern();
	}

	// ================================================================ Ping

	private void pingeAlle() {
		for (int i = 0; i < liste.size(); i++) pinge(liste.get(i));
	}

	private void pinge(ServerData d) {
		d.setState(ServerData.State.PINGING);
		d.motd = Component.literal("Wird abgefragt …");
		d.ping = -1;
		d.players = null;
		boolean nativ = minecraft.options.useNativeTransport();
		PING.submit(() -> {
			try {
				pinger.pingServer(d, () -> minecraft.execute(liste::save), () -> {
					d.setState(d.protocol == SharedConstants.getCurrentVersion().protocolVersion()
							? ServerData.State.SUCCESSFUL : ServerData.State.INCOMPATIBLE);
					minecraft.execute(this::filtern);
				}, EventLoopGroupHolder.remote(nativ));
			} catch (Exception e) {
				d.setState(ServerData.State.UNREACHABLE);
				d.ping = -1;
				d.motd = Component.literal("Nicht erreichbar").withStyle(ChatFormatting.RED);
			}
		});
	}

	@Override
	public void tick() {
		pinger.tick();
	}

	@Override
	public void removed() {
		pinger.removeAll();
		for (FaviconTexture f : icons.values()) f.close();
		icons.clear();
		hochgeladen.clear();
	}

	@Override
	public void onClose() {
		minecraft.setScreen(eltern);
	}

	// ================================================================ Aktionen

	private void beitreten(ServerData d) {
		if (d == null) return;
		ConnectScreen.startConnecting(this, minecraft, ServerAddress.parseString(d.ip), d, false, null);
	}

	private void direkt() {
		bearbeitet = new ServerData(Component.translatable("selectServer.defaultName").getString(), "", ServerData.Type.OTHER);
		minecraft.setScreen(new DirectJoinServerScreen(this, ok -> {
			if (ok) {
				ServerData schon = liste.get(bearbeitet.ip);
				if (schon != null) {
					schon.copyNameIconFrom(bearbeitet);
					beitreten(schon);
				} else {
					beitreten(bearbeitet);
				}
			} else {
				minecraft.setScreen(this);
			}
		}, bearbeitet));
	}

	private void hinzufuegen() {
		bearbeitet = new ServerData(Component.translatable("selectServer.defaultName").getString(), "", ServerData.Type.OTHER);
		minecraft.setScreen(new ManageServerScreen(this, Component.translatable("manageServer.add.title"), ok -> {
			if (ok) {
				ServerData versteckt = liste.unhide(bearbeitet.ip);
				if (versteckt != null) {
					versteckt.copyNameIconFrom(bearbeitet);
				} else {
					liste.add(bearbeitet, false);
				}
				liste.save();
				pinge(versteckt != null ? versteckt : bearbeitet);
			}
			minecraft.setScreen(this);
		}, bearbeitet));
	}

	private void bearbeiten() {
		ServerData d = ausgewaehlt;
		if (d == null) return;
		String alteIp = d.ip.toLowerCase(Locale.ROOT);
		bearbeitet = new ServerData(d.name, d.ip, ServerData.Type.OTHER);
		bearbeitet.copyFrom(d);
		minecraft.setScreen(new ManageServerScreen(this, Component.translatable("manageServer.edit.title"), ok -> {
			if (ok) {
				d.copyFrom(bearbeitet);
				liste.save();
				// Favorit mitnehmen, wenn sich die Adresse geändert hat
				if (cfg().serverSterne.remove(alteIp)) {
					cfg().serverSterne.add(d.ip.toLowerCase(Locale.ROOT));
					cfg().speichern();
				}
				pinge(d);
			}
			minecraft.setScreen(this);
		}, bearbeitet));
	}

	private void loeschen() {
		ServerData d = ausgewaehlt;
		if (d == null) return;
		minecraft.setScreen(new ConfirmScreen(ok -> {
			if (ok) {
				liste.remove(d);
				liste.save();
				cfg().serverSterne.remove(d.ip.toLowerCase(Locale.ROOT));
				cfg().speichern();
				ausgewaehlt = null;
				FaviconTexture f = icons.remove(d);
				if (f != null) f.close();
			}
			minecraft.setScreen(this);
		}, Component.translatable("selectServer.deleteQuestion"),
				Component.translatable("selectServer.deleteWarning", d.name),
				Component.translatable("selectServer.deleteButton"), Component.translatable("gui.cancel")));
	}

	private void aktualisieren() {
		pinger.removeAll();
		liste.load();
		ausgewaehlt = null;
		pingeAlle();
		filtern();
	}

	private void minecraftListe() {
		vanillaEinmal = true;
		minecraft.setScreen(new JoinMultiplayerScreen(eltern));
	}

	// ================================================================ Zeichnen

	@Override
	public void render(GuiGraphics g, int mx, int my, float delta) {
		bereiche.clear();
		Farben.themaAnwenden(BambusClient.config.thema);
		int b = width, h = height;
		boolean eng = h < 280;

		// Kopf
		int rand = Math.max(10, b / 20);
		int kopf = eng ? 34 : 44;
		g.fill(0, 0, b, h, 0xB0020407);
		Neon.logo(g, rand, 8, 18);
		Neon.leuchtText(g, font, "SERVER", rand + 18, 9, 1.25f, Farben.NEON);
		int fav = 0;
		for (int i = 0; i < liste.size(); i++) if (istFavorit(liste.get(i))) fav++;
		Neon.text(g, font, liste.size() + " Server  ·  " + fav + " Favoriten", rand + 18, 22, 0.6f, Farben.GRAU, false);

		// Tabs ALLE / ★ FAVORITEN und Suche
		int tx = rand + 120;
		tab(g, mx, my, "ALLE", !nurFavoriten, tx, 10, 40, () -> { nurFavoriten = false; scroll = 0; filtern(); });
		tab(g, mx, my, "★ FAVORITEN", nurFavoriten, tx + 44, 10, 66, () -> { nurFavoriten = true; scroll = 0; filtern(); });
		int sb = Math.min(150, b - (tx + 116) - rand);
		suche.setVisible(sb >= 70);
		if (suche.isVisible()) {
			int fx = b - rand - sb;
			suche.setX(fx + 16);
			suche.setY(13);
			suche.setWidth(sb - 20);
			g.fill(fx, 9, fx + sb, 25, Farben.KARTE);
			g.renderOutline(fx, 9, sb, 16, suche.isFocused() ? Farben.NEON : Farben.LINIE);
			Neon.text(g, font, "⌕", fx + 5, 13, 1f, suche.isFocused() ? Farben.NEON : Farben.GRAU, false);
		}
		g.fill(0, kopf - 1, b, kopf, Farben.LINIE);

		// Fuß mit Knöpfen
		int fussH = eng ? 26 : 32;
		int fy = h - fussH;
		g.fill(0, fy, b, fy + 1, Farben.LINIE);
		boolean gewaehlt = ausgewaehlt != null;
		String[][] knoepfe = {{"BEITRETEN", "1"}, {"DIREKT", "2"}, {"HINZUFÜGEN", "3"}, {"BEARBEITEN", "4"}, {"LÖSCHEN", "5"}, {"PING", "6"}, {"ZURÜCK", "7"}};
		Runnable[] aktionen = {() -> beitreten(ausgewaehlt), this::direkt, this::hinzufuegen, this::bearbeiten, this::loeschen, this::aktualisieren, this::onClose};
		int[] farben = {Farben.NEON, Farben.NEON_CYAN, Farben.NEON_CYAN, Farben.GRAU, Farben.ROT, Farben.GRAU, Farben.GRAU};
		boolean[] aktiv = {gewaehlt, true, true, gewaehlt, gewaehlt, true, true};
		int[] breiten = new int[knoepfe.length];
		int gesamt = 0;
		for (int i = 0; i < knoepfe.length; i++) {
			breiten[i] = Math.max(40, (int) (font.width(knoepfe[i][0]) * 0.75f) + 16);
			gesamt += breiten[i] + 4;
		}
		float skala = Math.min(1f, (b - 2 * rand) / (float) gesamt);
		int kx = b / 2 - (int) (gesamt * skala) / 2;
		int kh = eng ? 16 : 20;
		for (int i = 0; i < knoepfe.length; i++) {
			int kb = (int) (breiten[i] * skala);
			knopf(g, mx, my, knoepfe[i][0], kx, fy + (fussH - kh) / 2, kb, kh, farben[i], aktiv[i], aktionen[i]);
			kx += kb + (int) (4 * skala);
		}
		// Minecraft-Liste (LAN) unten rechts klein
		String mc = "Minecraft-Liste (LAN)";
		int mw = (int) (font.width(mc) * 0.6f);
		boolean mh = Neon.ueber(mx, my, b - mw - 8, kopf + 3, mw + 4, 9);
		Neon.text(g, font, mc, b - mw - 6, kopf + 4, 0.6f, mh ? Farben.WEISS : Farben.GRAU, false);
		bereiche.add(new Bereich(b - mw - 8, kopf + 3, mw + 4, 9, this::minecraftListe));

		// Liste
		lx = Math.max(rand, b / 2 - 210);
		lb = Math.min(420, b - 2 * rand);
		ly = kopf + 14;
		lh = fy - ly - 6;
		inhaltHoehe = sichtbar.size() * (KARTE + 4);
		scroll = Mth.clamp(scroll, 0, Math.max(0, inhaltHoehe - lh));
		g.enableScissor(lx - 4, ly, lx + lb + 4, ly + lh);
		int y = ly - (int) scroll;
		for (ServerData d : sichtbar) {
			if (y + KARTE >= ly && y <= ly + lh) karte(g, d, lx, y, lb, mx, my);
			y += KARTE + 4;
		}
		g.disableScissor();
		if (sichtbar.isEmpty()) {
			String leer = nurFavoriten ? "Noch keine Favoriten – klick bei einem Server auf ☆." : liste.size() == 0
					? "Noch keine Server. Klick auf HINZUFÜGEN." : "Kein Server passt zur Suche.";
			int lw = (int) (font.width(leer) * 0.85f);
			Neon.text(g, font, leer, b / 2 - lw / 2, ly + 20, 0.85f, Farben.GRAU, false);
		}
		// Scrollbalken
		if (inhaltHoehe > lh) {
			int bh = Math.max(16, lh * lh / inhaltHoehe);
			int by = ly + (int) ((lh - bh) * (scroll / (inhaltHoehe - lh)));
			g.fill(lx + lb + 3, ly, lx + lb + 5, ly + lh, Farben.LINIE);
			g.fill(lx + lb + 3, by, lx + lb + 5, by + bh, Farben.NEON);
		}
		super.render(g, mx, my, delta);   // Suchfeld zuletzt, damit es über dem Hintergrund liegt
	}

	private void karte(GuiGraphics g, ServerData d, int x, int y, int b, int mx, int my) {
		boolean ueber = Neon.ueber(mx, my, x, y, b, KARTE) && my >= ly && my < ly + lh;
		boolean gewaehlt = d == ausgewaehlt;
		boolean favorit = istFavorit(d);
		bereiche.add(new Bereich(x, y, b, KARTE, () -> waehle(d), true));   // zuerst: Stern und Pfeil liegen darüber
		int akzent = favorit ? Farben.GELB : Farben.NEON;
		g.fillGradient(x, y, x + b, y + KARTE, ueber || gewaehlt ? 0xF0122236 : 0xE00B1626, 0xE0050A10);
		if (gewaehlt) {
			Neon.leuchtRahmen(g, x, y, b, KARTE, akzent, 0.8f);
		} else {
			g.renderOutline(x, y, b, KARTE, ueber ? Farben.alpha(akzent, 160) : Farben.LINIE);
		}
		if (favorit) g.fill(x, y + 4, x + 2, y + KARTE - 4, Farben.GELB);

		// Server-Icon (oder Bambus-Platzhalter)
		int ix = x + 4, iy = y + 4, is = 32;
		FaviconTexture icon = icon(d);
		if (icon != null) {
			g.blit(RenderPipelines.GUI_TEXTURED, icon.textureLocation(), ix, iy, 0f, 0f, is, is, is, is);
		} else {
			g.fill(ix, iy, ix + is, iy + is, 0xFF101824);
			Neon.logo(g, ix + 10, iy + 7, 18);
		}
		if (ueber) {   // Beitreten-Pfeil über dem Icon
			g.fill(ix, iy, ix + is, iy + is, 0xA0000000);
			Neon.text(g, font, "▶", ix + 12, iy + 10, 1.25f, Farben.NEON, false);
			bereiche.add(new Bereich(ix, iy, is, is, () -> beitreten(d), true));
		}

		// Name, Adresse, Beschreibung
		int tx = ix + is + 6;
		int rechts = 74;
		String name = font.plainSubstrByWidth(d.name, b - (tx - x) - rechts - 14);
		g.drawString(font, name, tx, y + 4, Farben.WEISS, true);
		int nb = font.width(name);
		Neon.text(g, font, font.plainSubstrByWidth(d.ip, (int) ((b - (tx - x) - rechts - nb - 22) / 0.6f)), tx + nb + 6, y + 6, 0.6f, Farben.GRAU, false);
		List<FormattedCharSequence> zeilen = font.split(d.motd == null ? Component.empty() : d.motd, (int) ((b - (tx - x) - rechts) / 0.75f));
		for (int i = 0; i < Math.min(2, zeilen.size()); i++) {
			g.pose().pushMatrix();
			g.pose().translate(tx, y + 16 + i * 8);
			g.pose().scale(0.75f, 0.75f);
			g.drawString(font, zeilen.get(i), 0, 0, 0xFFA8B4C4, false);
			g.pose().popMatrix();
		}

		// rechts: Favorit-Stern, Ping, Spieler
		int sx = x + b - 14, sy = y + 3;
		boolean sternUeber = Neon.ueber(mx, my, sx - 2, sy - 1, 12, 11);
		Neon.text(g, font, favorit ? "★" : "☆", sx, sy, 1f, favorit ? Farben.GELB : sternUeber ? Farben.WEISS : Farben.GRAU, false);
		bereiche.add(new Bereich(sx - 2, sy - 1, 12, 11, () -> favoritUmschalten(d), true));
		pingBalken(g, d, x + b - 34, y + 5);
		String spieler = d.players == null ? "" : d.players.online() + "/" + d.players.max();
		if (!spieler.isEmpty()) {
			int sw = (int) (font.width(spieler) * 0.75f);
			Neon.text(g, font, spieler, x + b - 8 - sw, y + 18, 0.75f, Farben.GRAU, false);
		}
		if (d.state() == ServerData.State.INCOMPATIBLE && d.version != null) {
			String v = font.plainSubstrByWidth(d.version.getString(), 90);
			int vw = (int) (font.width(v) * 0.6f);
			Neon.text(g, font, v, x + b - 8 - vw, y + 29, 0.6f, Farben.ROT, false);
		} else if (d.ping >= 0 && d.state() == ServerData.State.SUCCESSFUL) {
			String ms = d.ping + " ms";
			int pw = (int) (font.width(ms) * 0.6f);
			Neon.text(g, font, ms, x + b - 8 - pw, y + 29, 0.6f, pingFarbe(d.ping), false);
		}
	}

	private void pingBalken(GuiGraphics g, ServerData d, int x, int y) {
		int stufen;
		int farbe;
		if (d.state() == ServerData.State.PINGING || d.state() == ServerData.State.INITIAL) {
			stufen = (int) (System.currentTimeMillis() / 150 % 6);
			farbe = Farben.NEON_CYAN;
		} else if (d.state() != ServerData.State.SUCCESSFUL || d.ping < 0) {
			stufen = 0;
			farbe = Farben.ROT;
		} else {
			stufen = d.ping < 80 ? 5 : d.ping < 150 ? 4 : d.ping < 300 ? 3 : d.ping < 600 ? 2 : 1;
			farbe = pingFarbe(d.ping);
		}
		for (int i = 0; i < 5; i++) {
			int bh = 2 + i * 2;
			g.fill(x + i * 3, y + 10 - bh, x + i * 3 + 2, y + 10, i < stufen ? farbe : 0xFF2A3445);
		}
		if (d.state() == ServerData.State.UNREACHABLE || d.state() == ServerData.State.INCOMPATIBLE) {
			Neon.text(g, font, "✕", x + 4, y + 1, 0.75f, Farben.ROT, false);
		}
	}

	private static int pingFarbe(long ping) {
		return ping < 80 ? Farben.NEON : ping < 150 ? 0xFFB6E33D : ping < 300 ? Farben.GELB : Farben.ROT;
	}

	/** Server-Icon hochladen, sobald der Ping es geliefert hat. */
	private FaviconTexture icon(ServerData d) {
		byte[] bytes = d.getIconBytes();
		if (bytes == null) return null;
		FaviconTexture f = icons.get(d);
		if (f == null) {
			f = FaviconTexture.forServer(minecraft.getTextureManager(), d.ip);
			icons.put(d, f);
		}
		if (!Arrays.equals(bytes, hochgeladen.get(d))) {
			try {
				f.upload(NativeImage.read(bytes));
				hochgeladen.put(d, bytes);
			} catch (Exception e) {
				d.setIconBytes(null);
				f.clear();
				return null;
			}
		}
		return f;
	}

	private void tab(GuiGraphics g, int mx, int my, String text, boolean an, int x, int y, int b, Runnable aktion) {
		boolean ueber = Neon.ueber(mx, my, x, y, b, 14);
		g.fill(x, y, x + b, y + 14, an ? Farben.alpha(Farben.NEON, 40) : ueber ? Farben.KARTE_HOVER : Farben.KARTE);
		g.renderOutline(x, y, b, 14, an ? Farben.NEON : Farben.LINIE);
		int w = (int) (font.width(text) * 0.75f);
		Neon.text(g, font, text, x + (b - w) / 2, y + 4, 0.75f, an ? Farben.WEISS : Farben.GRAU, false);
		bereiche.add(new Bereich(x, y, b, 14, aktion));
	}

	private void knopf(GuiGraphics g, int mx, int my, String text, int x, int y, int b, int h, int farbe, boolean aktiv, Runnable aktion) {
		boolean ueber = aktiv && Neon.ueber(mx, my, x, y, b, h);
		g.fill(x, y, x + b, y + h, ueber ? Farben.alpha(farbe, 40) : Farben.KARTE);
		g.renderOutline(x, y, b, h, aktiv ? (ueber ? farbe : Farben.alpha(farbe, 120)) : 0xFF161F2B);
		float s = Math.min(0.75f, (b - 6) / (float) font.width(text));
		int w = (int) (font.width(text) * s);
		Neon.text(g, font, text, x + (b - w) / 2, y + (h - (int) (8 * s)) / 2, s, aktiv ? (ueber ? Farben.WEISS : farbe) : 0xFF3A4656, false);
		if (aktiv) bereiche.add(new Bereich(x, y, b, h, aktion));
	}

	private void waehle(ServerData d) {
		long jetzt = System.currentTimeMillis();
		if (d == letzterKlickAuf && jetzt - letzterKlick < 350) {
			beitreten(d);   // Doppelklick
			return;
		}
		letzterKlick = jetzt;
		letzterKlickAuf = d;
		ausgewaehlt = d;
	}

	// ================================================================ Eingabe

	@Override
	public boolean mouseClicked(MouseButtonEvent e, boolean doppelt) {
		if (e.button() == 0) {
			// von hinten nach vorne: Stern und Pfeil liegen über der Karte
			boolean inListe = e.y() >= ly && e.y() < ly + lh;
			for (int i = bereiche.size() - 1; i >= 0; i--) {
				Bereich r = bereiche.get(i);
				if (r.inListe() && !inListe) continue;   // abgeschnittene Karte unter Kopf oder Fuß
				if (Neon.ueber(e.x(), e.y(), r.x(), r.y(), r.b(), r.h())) {
					minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
					r.aktion().run();
					return true;
				}
			}
		}
		return super.mouseClicked(e, doppelt);
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double horizontal, double vertikal) {
		scroll = Mth.clamp(scroll - vertikal * 24, 0, Math.max(0, inhaltHoehe - lh));
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent e) {
		int k = e.key();
		if (k == InputConstants.KEY_RETURN || k == InputConstants.KEY_NUMPADENTER) {
			if (ausgewaehlt != null) {
				beitreten(ausgewaehlt);
				return true;
			}
		} else if (k == InputConstants.KEY_F5) {
			aktualisieren();
			return true;
		} else if ((k == InputConstants.KEY_UP || k == InputConstants.KEY_DOWN) && !sichtbar.isEmpty() && !suche.isFocused()) {
			int i = ausgewaehlt == null ? -1 : sichtbar.indexOf(ausgewaehlt);
			i = Mth.clamp(i + (k == InputConstants.KEY_UP ? -1 : 1), 0, sichtbar.size() - 1);
			ausgewaehlt = sichtbar.get(i);
			int oben = i * (KARTE + 4);
			if (oben < scroll) scroll = oben;
			if (oben + KARTE > scroll + lh) scroll = oben + KARTE - lh;
			return true;
		} else if (k == InputConstants.KEY_DELETE && ausgewaehlt != null && !suche.isFocused()) {
			loeschen();
			return true;
		}
		return super.keyPressed(e);
	}
}
