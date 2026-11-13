package de.bambus.client.hud;

import de.bambus.client.Farben;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

import java.util.ArrayList;
import java.util.List;

/** Server-Adresse (der Ping ist ein eigenes Modul, siehe {@link PingModul}). */
public class ServerModul extends TextModul {

	public ServerModul() {
		super("server", "Server", 4, 158, true, "Serveradresse", "INFO");
	}

	@Override
	protected List<Zeile> zeilen(Minecraft mc, boolean editor) {
		List<Zeile> z = new ArrayList<>();
		if (mc.hasSingleplayerServer()) {
			z.add(new Zeile("Einzelspieler", Farben.WEISS));
			return z;
		}
		ServerData server = mc.getCurrentServer();
		z.add(new Zeile("Server: " + (server != null ? server.ip : editor ? "bambus.example" : "?"), Farben.WEISS));
		return z;
	}
}
