package de.bambus.launcher;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

/**
 * Fragt einen Minecraft-Server nach seinem Status – genau wie die Mehrspieler-Liste im Spiel
 * (Server List Ping): Spieler online, Beschreibung, Version, Symbol und Ping.
 * Es wird nichts eingeloggt und nichts verändert.
 */
final class ServerStatus {

	static final class Ergebnis {
		boolean online;
		String fehler = "";
		String motd = "";
		String version = "";
		int spieler = -1, maxSpieler = -1;
		long ping = -1;
		BufferedImage symbol;
	}

	private ServerStatus() {}

	static Ergebnis frage(String adresse) {
		Ergebnis e = new Ergebnis();
		String host = adresse.trim();
		int port = 25565;
		int doppel = host.lastIndexOf(':');
		if (doppel > 0 && host.indexOf(']') < doppel) {
			try {
				port = Integer.parseInt(host.substring(doppel + 1));
				host = host.substring(0, doppel);
			} catch (NumberFormatException ignoriert) {
				// dann Standardport
			}
		} else {
			String[] srv = srvEintrag(host);   // z. B. _minecraft._tcp.gommehd.net
			if (srv != null) {
				host = srv[0];
				port = Integer.parseInt(srv[1]);
			}
		}
		try (Socket s = new Socket()) {
			s.connect(new InetSocketAddress(host, port), 4000);
			s.setSoTimeout(5000);
			DataOutputStream aus = new DataOutputStream(s.getOutputStream());
			DataInputStream ein = new DataInputStream(s.getInputStream());

			// Handshake (Zustand 1 = Status) und Status-Anfrage
			ByteArrayOutputStream hs = new ByteArrayOutputStream();
			DataOutputStream h = new DataOutputStream(hs);
			schreibeVarInt(h, 0x00);
			schreibeVarInt(h, 774);   // Protokollnummer; für den Status egal
			schreibeText(h, host);
			h.writeShort(port);
			schreibeVarInt(h, 1);
			paket(aus, hs.toByteArray());
			paket(aus, new byte[]{0x00});

			liesVarInt(ein);                      // Länge
			if (liesVarInt(ein) != 0x00) throw new IOException("Unerwartete Antwort");
			byte[] json = new byte[liesVarInt(ein)];
			ein.readFully(json);
			auswerten(e, new String(json, StandardCharsets.UTF_8));

			// Ping messen
			long start = System.nanoTime();
			ByteArrayOutputStream ps = new ByteArrayOutputStream();
			DataOutputStream p = new DataOutputStream(ps);
			schreibeVarInt(p, 0x01);
			p.writeLong(start);
			paket(aus, ps.toByteArray());
			liesVarInt(ein);
			liesVarInt(ein);
			ein.readLong();
			e.ping = (System.nanoTime() - start) / 1_000_000;
			e.online = true;
		} catch (java.net.UnknownHostException ex) {
			e.fehler = "Adresse nicht gefunden";
		} catch (java.net.SocketTimeoutException ex) {
			e.fehler = "Keine Antwort";
		} catch (Exception ex) {
			e.fehler = e.motd.isEmpty() ? "Nicht erreichbar" : e.fehler;
			e.online = !e.motd.isEmpty();   // Status kam an, nur der Ping nicht
		}
		return e;
	}

	@SuppressWarnings("unchecked")
	private static void auswerten(Ergebnis e, String json) {
		Map<String, Object> o = Json.objekt(Json.parse(json));
		if (o == null) return;
		e.motd = ohneFarbcodes(text(o.get("description"))).replaceAll("\\s+", " ").trim();
		Map<String, Object> sp = Json.objekt(o.get("players"));
		if (sp != null) {
			e.spieler = zahl(sp.get("online"));
			e.maxSpieler = zahl(sp.get("max"));
		}
		Map<String, Object> v = Json.objekt(o.get("version"));
		if (v != null) e.version = ohneFarbcodes(String.valueOf(v.get("name")));
		Object fav = o.get("favicon");
		if (fav instanceof String && ((String) fav).startsWith("data:image/png;base64,")) {
			try {
				byte[] png = Base64.getMimeDecoder().decode(((String) fav).substring(22));
				e.symbol = ImageIO.read(new ByteArrayInputStream(png));
			} catch (Exception ignoriert) {
				// ohne Symbol
			}
		}
	}

	/** Beschreibung kann Text oder ein Text-Bauteil mit "text" und "extra" sein. */
	@SuppressWarnings("unchecked")
	private static String text(Object o) {
		if (o == null) return "";
		if (o instanceof String) return (String) o;
		if (o instanceof List) {
			StringBuilder b = new StringBuilder();
			for (Object x : (List<Object>) o) b.append(text(x));
			return b.toString();
		}
		Map<String, Object> m = Json.objekt(o);
		if (m == null) return String.valueOf(o);
		StringBuilder b = new StringBuilder(text(m.get("text")));
		if (m.get("extra") != null) b.append(text(m.get("extra")));
		return b.toString();
	}

	static String ohneFarbcodes(String s) {
		return s == null ? "" : s.replaceAll("§.", "");
	}

	private static int zahl(Object o) {
		return o instanceof Number ? ((Number) o).intValue() : -1;
	}

	private static String[] srvEintrag(String host) {
		try {
			Hashtable<String, String> env = new Hashtable<>();
			env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
			javax.naming.directory.DirContext ctx = new javax.naming.directory.InitialDirContext(env);
			javax.naming.directory.Attribute a = ctx.getAttributes("_minecraft._tcp." + host, new String[]{"SRV"}).get("SRV");
			if (a == null) return null;
			String[] t = a.get().toString().split(" ");   // Priorität Gewicht Port Ziel
			String ziel = t[3].endsWith(".") ? t[3].substring(0, t[3].length() - 1) : t[3];
			return new String[]{ziel, t[2]};
		} catch (Throwable ex) {
			return null;   // kein SRV-Eintrag (oder DNS-Modul fehlt) → normaler Port
		}
	}

	private static void paket(OutputStream aus, byte[] daten) throws IOException {
		DataOutputStream d = new DataOutputStream(aus);
		schreibeVarInt(d, daten.length);
		d.write(daten);
		d.flush();
	}

	private static void schreibeText(DataOutputStream d, String s) throws IOException {
		byte[] b = s.getBytes(StandardCharsets.UTF_8);
		schreibeVarInt(d, b.length);
		d.write(b);
	}

	private static void schreibeVarInt(DataOutputStream d, int wert) throws IOException {
		while ((wert & ~0x7F) != 0) {
			d.writeByte((wert & 0x7F) | 0x80);
			wert >>>= 7;
		}
		d.writeByte(wert);
	}

	private static int liesVarInt(DataInputStream d) throws IOException {
		int wert = 0, stelle = 0;
		while (true) {
			byte b = d.readByte();
			wert |= (b & 0x7F) << stelle;
			if ((b & 0x80) == 0) return wert;
			stelle += 7;
			if (stelle > 35) throw new IOException("VarInt zu lang");
		}
	}
}
