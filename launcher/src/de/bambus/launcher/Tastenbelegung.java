package de.bambus.launcher;

import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Liest und schreibt die Tastenbelegung aus Minecrafts <code>options.txt</code>.
 * Minecraft speichert dort Zeilen wie <code>key_key.bambus.menu:key.keyboard.right.shift</code>.
 * Wir ändern nur diese Zeilen und lassen alles andere in der Datei unberührt.
 */
final class Tastenbelegung {

	/** {Schlüssel in options.txt, Name, Standardtaste} – erst Bambus, dann die wichtigsten aus Minecraft. */
	static final String[][] AKTIONEN = {
			{"key.bambus.menu", "Bambus-Menü", "key.keyboard.right.shift"},
			{"key.bambus.zoom", "Zoom", "key.keyboard.c"},
			{"key.bambus.freelook", "Freelook (umschauen)", "key.keyboard.left.alt"},
			{"key.bambus.hud", "HUD an/aus", "key.keyboard.h"},
			{"key.bambus.koordinaten", "Koordinaten kopieren", "key.keyboard.unknown"},
			{"key.bambus.musik_pause", "Musik: Pause / Weiter", "key.keyboard.unknown"},
			{"key.bambus.musik_weiter", "Musik: nächstes Lied", "key.keyboard.unknown"},
			{"key.bambus.musik_zurueck", "Musik: vorheriges Lied", "key.keyboard.unknown"},
			{"key.forward", "Vorwärts", "key.keyboard.w"},
			{"key.left", "Nach links", "key.keyboard.a"},
			{"key.back", "Rückwärts", "key.keyboard.s"},
			{"key.right", "Nach rechts", "key.keyboard.d"},
			{"key.jump", "Springen", "key.keyboard.space"},
			{"key.sneak", "Schleichen", "key.keyboard.left.shift"},
			{"key.sprint", "Sprinten", "key.keyboard.left.control"},
			{"key.inventory", "Inventar", "key.keyboard.e"},
			{"key.chat", "Chat", "key.keyboard.t"},
			{"key.playerlist", "Spielerliste", "key.keyboard.tab"},
			{"key.togglePerspective", "Perspektive wechseln", "key.keyboard.f5"},
			{"key.screenshot", "Screenshot", "key.keyboard.f2"},
	};

	/** Ab hier sind es normale Minecraft-Tasten, davor die von Bambus. */
	static final int MINECRAFT_AB = 8;

	static final String FREI = "key.keyboard.unknown";

	private Tastenbelegung() {}

	static Path datei(Path spielOrdner) {
		return spielOrdner.resolve("options.txt");
	}

	/** Alle belegten Tasten aus der Datei; fehlt eine Zeile, gilt die Standardtaste. */
	static Map<String, String> lies(Path spielOrdner) {
		Map<String, String> aus = new LinkedHashMap<>();
		for (String[] a : AKTIONEN) aus.put(a[0], a[2]);
		try {
			Path d = datei(spielOrdner);
			if (!Files.isRegularFile(d)) return aus;
			for (String zeile : Files.readAllLines(d, StandardCharsets.UTF_8)) {
				int doppel = zeile.indexOf(':');
				if (doppel <= 0 || !zeile.startsWith("key_")) continue;
				String schluessel = zeile.substring(4, doppel);
				if (aus.containsKey(schluessel)) aus.put(schluessel, zeile.substring(doppel + 1).trim());
			}
		} catch (IOException ignoriert) {
			// dann eben die Standardtasten
		}
		return aus;
	}

	/**
	 * Schreibt eine Taste in die Datei. Gibt es die Zeile noch nicht, wird sie angehängt.
	 * Existiert die Datei noch gar nicht, wird sie angelegt – Minecraft ergänzt den Rest beim Start.
	 */
	static void schreibe(Path spielOrdner, String schluessel, String taste) throws IOException {
		Path d = datei(spielOrdner);
		Files.createDirectories(d.getParent());
		List<String> zeilen = Files.isRegularFile(d)
				? new ArrayList<>(Files.readAllLines(d, StandardCharsets.UTF_8))
				: new ArrayList<>();
		String neu = "key_" + schluessel + ":" + taste;
		boolean ersetzt = false;
		for (int i = 0; i < zeilen.size(); i++) {
			if (zeilen.get(i).startsWith("key_" + schluessel + ":")) {
				zeilen.set(i, neu);
				ersetzt = true;
				break;
			}
		}
		if (!ersetzt) zeilen.add(neu);
		// sicher schreiben: erst Zwischendatei, dann austauschen – options.txt darf nie halb geschrieben sein
		Installer.schreibeSicher(d, (String.join(System.lineSeparator(), zeilen) + System.lineSeparator()).getBytes(StandardCharsets.UTF_8));
	}

	// ------------------------------------------------------- Namen und Umrechnen

	/** Aus „key.keyboard.right.shift“ wird „Shift rechts“. */
	static String anzeige(String taste) {
		if (taste == null || taste.isEmpty() || FREI.equals(taste)) return "—";
		if (taste.startsWith("key.mouse.")) {
			String m = taste.substring("key.mouse.".length());
			if (m.equals("left")) return "Maus links";
			if (m.equals("right")) return "Maus rechts";
			if (m.equals("middle")) return "Maus Mitte";
			return "Maus " + m;
		}
		String k = taste.startsWith("key.keyboard.") ? taste.substring("key.keyboard.".length()) : taste;
		switch (k) {
			case "space": return "Leertaste";
			case "left.shift": return "Shift links";
			case "right.shift": return "Shift rechts";
			case "left.control": return "Strg links";
			case "right.control": return "Strg rechts";
			case "left.alt": return "Alt links";
			case "right.alt": return "Alt rechts";
			case "tab": return "Tab";
			case "enter": return "Enter";
			case "backspace": return "Rücktaste";
			case "escape": return "Esc";
			case "delete": return "Entf";
			case "insert": return "Einfg";
			case "home": return "Pos1";
			case "end": return "Ende";
			case "page.up": return "Bild hoch";
			case "page.down": return "Bild runter";
			case "up": return "Pfeil hoch";
			case "down": return "Pfeil runter";
			case "left": return "Pfeil links";
			case "right": return "Pfeil rechts";
			case "caps.lock": return "Feststell";
			default:
				if (k.startsWith("keypad.")) return "Ziffernblock " + k.substring(7);
				return k.length() == 1 ? k.toUpperCase(Locale.ROOT) : k.toUpperCase(Locale.ROOT).replace('.', ' ');
		}
	}

	/** Eine gedrückte Taste in Minecrafts Schreibweise. Gibt null, wenn wir sie nicht kennen. */
	static String ausTastendruck(KeyEvent e) {
		int c = e.getKeyCode();
		boolean rechts = e.getKeyLocation() == KeyEvent.KEY_LOCATION_RIGHT;
		boolean block = e.getKeyLocation() == KeyEvent.KEY_LOCATION_NUMPAD;
		if (c >= KeyEvent.VK_A && c <= KeyEvent.VK_Z) return "key.keyboard." + (char) ('a' + c - KeyEvent.VK_A);
		if (c >= KeyEvent.VK_0 && c <= KeyEvent.VK_9) {
			return "key.keyboard." + (block ? "keypad." : "") + (char) ('0' + c - KeyEvent.VK_0);
		}
		if (c >= KeyEvent.VK_NUMPAD0 && c <= KeyEvent.VK_NUMPAD9) {
			return "key.keyboard.keypad." + (char) ('0' + c - KeyEvent.VK_NUMPAD0);
		}
		if (c >= KeyEvent.VK_F1 && c <= KeyEvent.VK_F12) return "key.keyboard.f" + (c - KeyEvent.VK_F1 + 1);
		switch (c) {
			case KeyEvent.VK_SPACE: return "key.keyboard.space";
			case KeyEvent.VK_SHIFT: return rechts ? "key.keyboard.right.shift" : "key.keyboard.left.shift";
			case KeyEvent.VK_CONTROL: return rechts ? "key.keyboard.right.control" : "key.keyboard.left.control";
			case KeyEvent.VK_ALT: case KeyEvent.VK_ALT_GRAPH:
				return rechts || c == KeyEvent.VK_ALT_GRAPH ? "key.keyboard.right.alt" : "key.keyboard.left.alt";
			case KeyEvent.VK_TAB: return "key.keyboard.tab";
			case KeyEvent.VK_ENTER: return "key.keyboard.enter";
			case KeyEvent.VK_BACK_SPACE: return "key.keyboard.backspace";
			case KeyEvent.VK_DELETE: return "key.keyboard.delete";
			case KeyEvent.VK_INSERT: return "key.keyboard.insert";
			case KeyEvent.VK_HOME: return "key.keyboard.home";
			case KeyEvent.VK_END: return "key.keyboard.end";
			case KeyEvent.VK_PAGE_UP: return "key.keyboard.page.up";
			case KeyEvent.VK_PAGE_DOWN: return "key.keyboard.page.down";
			case KeyEvent.VK_UP: return "key.keyboard.up";
			case KeyEvent.VK_DOWN: return "key.keyboard.down";
			case KeyEvent.VK_LEFT: return "key.keyboard.left";
			case KeyEvent.VK_RIGHT: return "key.keyboard.right";
			case KeyEvent.VK_CAPS_LOCK: return "key.keyboard.caps.lock";
			case KeyEvent.VK_PERIOD: return "key.keyboard.period";
			case KeyEvent.VK_COMMA: return "key.keyboard.comma";
			case KeyEvent.VK_MINUS: return "key.keyboard.minus";
			case KeyEvent.VK_PLUS: return "key.keyboard.equal";
			default: return null;
		}
	}
}
