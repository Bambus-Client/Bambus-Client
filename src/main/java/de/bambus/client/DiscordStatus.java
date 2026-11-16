package de.bambus.client;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Discord-Status („Spielt Bambus Client“) über die lokale Discord-Schnittstelle (IPC) – ohne Zusatzbibliothek.
 * Braucht eine Discord-Anwendungs-ID (kostenlos im Discord Developer Portal). Ohne ID bleibt alles aus.
 *
 * Der Austausch mit Discord läuft in einem eigenen Hintergrund-Thread: Hängt Discord, ruckelt das Spiel nicht.
 * Der Spiel-Thread stellt nur den Text zusammen.
 */
public final class DiscordStatus {

	private DiscordStatus() {}

	/** Fest eingebaute Discord-App-ID (leer = keine). Eine ID in der Konfiguration hat Vorrang. */
	public static final String STANDARD_APP_ID = "";

	/** Ein einziger Hintergrund-Thread für alles, was mit Discord spricht. */
	private static final ExecutorService ARBEITER = Executors.newSingleThreadExecutor(r -> {
		Thread t = new Thread(r, "Bambus-Discord");
		t.setDaemon(true);
		return t;
	});
	/** Läuft gerade eine Aufgabe im Hintergrund? Dann wird nichts Neues eingereiht. */
	private static final AtomicBoolean BESCHAEFTIGT = new AtomicBoolean();

	// nur im Hintergrund-Thread benutzt (trennen() schließt zusätzlich vom Spiel-Thread aus, um Hänger zu lösen)
	private static volatile SocketChannel unix;
	private static volatile RandomAccessFile pipe;

	private static final long START = System.currentTimeMillis() / 1000;
	private static long letzterVersuch, letztesUpdate;
	private static String letzterText = "";

	public static String appId() {
		String id = BambusClient.config.discordAppId;
		return id == null || id.isBlank() ? STANDARD_APP_ID : id.trim();
	}

	public static boolean moeglich() {
		return appId().matches("\\d{15,22}");
	}

	/** Jeden Tick; schickt höchstens alle 15 s ein Update. */
	public static void tick(net.minecraft.client.Minecraft mc) {
		if (!BambusClient.config.discordAn || !moeglich()) {
			if (verbunden()) trennen();
			return;
		}
		long jetzt = System.currentTimeMillis();
		String details = mc.level == null ? "Im Hauptmenü"
				: mc.isLocalServer() ? "Einzelspieler"
				: mc.getCurrentServer() != null ? "Auf " + mc.getCurrentServer().ip : "Mehrspieler";
		String text = details + "|" + Kampf.kills;
		boolean verbunden = verbunden();
		if (!verbunden && jetzt - letzterVersuch < 30_000) return;
		if (verbunden && text.equals(letzterText) && jetzt - letztesUpdate < 60_000) return;
		if (verbunden && jetzt - letztesUpdate < 15_000) return;
		if (!BESCHAEFTIGT.compareAndSet(false, true)) return;   // die letzte Aufgabe läuft noch

		if (!verbunden) letzterVersuch = jetzt;
		letzterText = text;
		letztesUpdate = jetzt;
		String appId = appId();
		String json = "{\"cmd\":\"SET_ACTIVITY\",\"nonce\":\"" + jetzt + "\",\"args\":{\"pid\":" + ProcessHandle.current().pid()
				+ ",\"activity\":{\"details\":\"" + esc(details) + "\",\"state\":\"Bambus Client " + BambusClient.VERSION
				+ (Kampf.kills > 0 ? " · " + Kampf.kills + " Kills" : "") + "\",\"timestamps\":{\"start\":" + START
				+ "},\"assets\":{\"large_image\":\"bambus\",\"large_text\":\"Bambus Client\"}}}}";
		ARBEITER.execute(() -> {
			try {
				if (!verbunden() && !verbinden(appId)) return;
				if (!senden(1, json)) schliessen();
			} finally {
				BESCHAEFTIGT.set(false);
			}
		});
	}

	private static String esc(String s) {
		return s.replace("\\", "\\\\").replace("\"", "\\\"");
	}

	private static boolean verbunden() {
		SocketChannel u = unix;
		return (u != null && u.isOpen()) || pipe != null;
	}

	/** Im Hintergrund-Thread: mit Discord verbinden und anmelden. */
	private static boolean verbinden(String appId) {
		for (int i = 0; i < 10; i++) {
			try {
				if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
					pipe = new RandomAccessFile("\\\\.\\pipe\\discord-ipc-" + i, "rw");
				} else {
					Path p = ipcOrdner().resolve("discord-ipc-" + i);
					if (!Files.exists(p)) {
						continue;
					}
					SocketChannel u = SocketChannel.open(StandardProtocolFamily.UNIX);
					unix = u;
					u.connect(UnixDomainSocketAddress.of(p));
					u.configureBlocking(false);
				}
				if (senden(0, "{\"v\":1,\"client_id\":\"" + appId + "\"}")) {
					BambusClient.LOGGER.info("Discord-Status verbunden");
					return true;
				}
				schliessen();   // Anmeldung fehlgeschlagen: diese Verbindung schließen, bevor die nächste versucht wird
			} catch (Exception e) {
				schliessen();
			}
		}
		return false;
	}

	private static Path ipcOrdner() {
		for (String var : new String[]{"XDG_RUNTIME_DIR", "TMPDIR", "TMP", "TEMP"}) {
			String v = System.getenv(var);
			if (v != null && !v.isEmpty()) {
				return Paths.get(v);
			}
		}
		return Paths.get("/tmp");
	}

	/** Im Hintergrund-Thread: ein Paket senden und Discords Antwort abholen, damit sich nichts anstaut. */
	private static boolean senden(int op, String json) {
		byte[] daten = json.getBytes(StandardCharsets.UTF_8);
		ByteBuffer buf = ByteBuffer.allocate(8 + daten.length).order(ByteOrder.LITTLE_ENDIAN);
		buf.putInt(op).putInt(daten.length).put(daten).flip();
		try {
			RandomAccessFile pp = pipe;
			SocketChannel u = unix;
			if (pp != null) {
				pp.write(buf.array());
				antwortLesen(pp);   // Discord antwortet auf jedes Paket genau einmal
			} else if (u != null) {
				ByteBuffer weg = ByteBuffer.allocate(8192);
				while (u.read(weg) > 0) {
					weg.clear(); // Antworten von Discord verwerfen
				}
				int versuche = 0;
				while (buf.hasRemaining() && versuche++ < 200) {
					u.write(buf);
				}
			}
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	/** Eine Antwort von der Windows-Pipe lesen und verwerfen (blockiert nur den Hintergrund-Thread). */
	private static void antwortLesen(RandomAccessFile pp) throws IOException {
		byte[] kopf = new byte[8];
		pp.readFully(kopf);
		int laenge = ByteBuffer.wrap(kopf).order(ByteOrder.LITTLE_ENDIAN).getInt(4);
		if (laenge < 0 || laenge > 1 << 20) throw new IOException("Ungültige Antwort von Discord");
		pp.readFully(new byte[laenge]);
	}

	private static void schliessen() {
		try {
			SocketChannel u = unix;
			RandomAccessFile pp = pipe;
			if (u != null) u.close();
			if (pp != null) pp.close();
		} catch (IOException ignoriert) {
			// schon zu
		}
		unix = null;
		pipe = null;
	}

	/** Verbindung beenden – auch vom Spiel-Thread aus; löst dabei einen Hintergrund-Thread, der auf Discord wartet. */
	public static void trennen() {
		schliessen();
	}
}
