package de.bambus.client.hud;

import de.bambus.client.Farben;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;

/** Ping als farbige Signalbalken mit kleinem Verlauf der letzten Minute. */
public class PingModul extends HudModul {

	private static final int BREITE = 112;
	private static final int HOEHE = 30;
	private static final int[] VERLAUF = new int[60];
	private static int pos = 0;
	private static int takt = 0;

	public PingModul() {
		super("pingverlauf", "Ping-Verlauf", 10000, 243, false, "Signalbalken und Ping der letzten Minute", "INFO");
	}

	private static int ping(Minecraft mc) {
		if (mc.getConnection() == null || mc.player == null || mc.hasSingleplayerServer()) return -1;
		PlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
		return info == null ? -1 : info.getLatency();
	}

	/** Einmal pro Sekunde einen Messwert merken. */
	public static void tick(Minecraft mc) {
		if (++takt % 20 != 0) return;
		VERLAUF[pos] = ping(mc);
		pos = (pos + 1) % VERLAUF.length;
	}

	private static int farbe(int ms) {
		return ms < 0 ? Farben.GRAU : ms < 80 ? Farben.GRUEN : ms < 180 ? Farben.GELB : Farben.ROT;
	}

	@Override
	public int breite(Minecraft mc, boolean editor) {
		return BREITE;
	}

	@Override
	public int hoehe(Minecraft mc, boolean editor) {
		return HOEHE;
	}

	@Override
	public void zeichne(GuiGraphics g, Minecraft mc, int x, int y, boolean editor) {
		panel(g, x, y, BREITE, HOEHE);
		int ms = editor && mc.player == null ? 46 : ping(mc);
		int f = farbe(ms);
		// 4 Signalbalken: je besser der Ping, desto mehr leuchten
		int stufe = ms < 0 ? 0 : ms < 50 ? 4 : ms < 100 ? 3 : ms < 180 ? 2 : 1;
		for (int i = 0; i < 4; i++) {
			int h = 3 + i * 3;
			g.fill(x + 6 + i * 4, y + 17 - h, x + 9 + i * 4, y + 17, i < stufe ? f : 0xFF2A3445);
		}
		String text = ms < 0 ? (mc.hasSingleplayerServer() ? "Einzelspieler" : "–") : ms + " ms";
		g.drawString(mc.font, text, x + 26, y + 5, f, true);
		// Verlauf (60 Sekunden) als Säulen
		int gx = x + 6, gy = y + HOEHE - 4, gh = 8;
		int max = 50;
		for (int v : VERLAUF) max = Math.max(max, v);
		for (int i = 0; i < VERLAUF.length; i++) {
			int v = VERLAUF[(pos + i) % VERLAUF.length];
			if (editor && mc.player == null) v = 40 + (int) (20 * Math.sin(i * 0.4));
			if (v < 0) continue;
			int h = Math.max(1, Math.round(gh * v / (float) max));
			g.fill(gx + i * 5 / 3, gy - h, gx + i * 5 / 3 + 1, gy, Farben.alpha(farbe(v), 200));
		}
	}
}
