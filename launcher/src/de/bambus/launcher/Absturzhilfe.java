package de.bambus.launcher;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Sucht den letzten Absturzbericht und übersetzt die häufigsten Ursachen in einen klaren Satz.
 * Minecraft legt bei einem Absturz eine Datei in <code>crash-reports</code> ab; außerdem steht
 * oft schon in <code>logs/latest.log</code>, woran es lag.
 */
final class Absturzhilfe {

	static final class Befund {
		Path datei;
		long wann;
		String ueberschrift = "";
		String erklaerung = "";
		String rat = "";
		boolean gefunden;
	}

	/** {Suchtext im Bericht, Überschrift, Erklärung, Was tun} – von oben nach unten geprüft. */
	private static final String[][] MUSTER = {
			{"outofmemoryerror", "Der Arbeitsspeicher war voll",
					"Minecraft hat mehr Speicher gebraucht, als ihm zugeteilt war.",
					"Unter EINSTELLUNGEN mehr Arbeitsspeicher wählen (aber nie mehr als die Hälfte vom RAM deines Computers). Helfen auch weniger Mods und eine kleinere Sichtweite."},
			{"no space left on device", "Die Festplatte ist voll",
					"Auf dem Computer war kein Platz mehr, um Dateien zu schreiben.",
					"Platz schaffen und noch einmal starten."},
			{"incompatible mod set", "Eine Mod passt nicht zur Minecraft-Version",
					"Mindestens eine Mod im Profil ist für eine andere Minecraft-Version gebaut.",
					"Unter PROFILE schauen, welche Version das Profil spielt, und die Mods passend dazu wählen. Der Bericht nennt die Mod meist beim Namen."},
			{"requires version", "Eine Mod braucht etwas anderes",
					"Eine Mod verlangt eine bestimmte Version von Fabric, der Fabric API oder einer anderen Mod.",
					"Die verlangte Mod ergänzen oder die Mod entfernen, die sie braucht."},
			{"unsupportedclassversionerror", "Java ist zu alt",
					"Minecraft braucht ein neueres Java, als auf dem Computer installiert ist.",
					"Minecraft einmal über den offiziellen Launcher starten – er bringt das passende Java mit."},
			{"mixin apply", "Zwei Mods vertragen sich nicht",
					"Zwei Mods wollten dieselbe Stelle im Spiel verändern.",
					"Die zuletzt hinzugefügte Mod entfernen und noch einmal starten. So findet man die Störenfried-Mod Stück für Stück."},
			{"pixel format not accelerated", "Die Grafikkarte macht Ärger",
					"Minecraft konnte kein Fenster zum Zeichnen bekommen.",
					"Grafiktreiber aktualisieren und den Computer neu starten."},
			{"failed to create window", "Es ging kein Fenster auf",
					"Das Spiel konnte sein Fenster nicht öffnen – meist liegt es am Grafiktreiber.",
					"Grafiktreiber aktualisieren und den Computer neu starten."},
			{"could not execute entrypoint", "Eine Mod ist beim Start gestolpert",
					"Eine Mod hat sich beim Hochfahren verabschiedet.",
					"Der Bericht nennt die Mod. Sie unter PROFILE abschalten und schauen, ob es dann läuft."},
			{"java.lang.nullpointerexception", "Im Spiel ist etwas schiefgelaufen",
					"Eine Stelle im Spiel oder in einer Mod hat etwas erwartet, das nicht da war.",
					"Meist hilft es, die zuletzt hinzugefügte Mod zu entfernen. Hilft das nicht, schick den Bericht weiter."},
	};

	private Absturzhilfe() {}

	/** Neuester Absturzbericht im Spielordner, oder ein Befund mit gefunden = false. */
	static Befund letzter(Path startOrdner) {
		Befund b = new Befund();
		Path ordner = startOrdner.resolve("crash-reports");
		if (!Files.isDirectory(ordner)) return b;
		try (Stream<Path> s = Files.list(ordner)) {
			Path neuester = s.filter(p -> p.getFileName().toString().endsWith(".txt"))
					.max(Comparator.comparingLong(p -> p.toFile().lastModified()))
					.orElse(null);
			if (neuester == null) return b;
			b.datei = neuester;
			b.wann = neuester.toFile().lastModified();
			b.gefunden = true;
			deute(b, lies(neuester));
			return b;
		} catch (IOException e) {
			return b;
		}
	}

	private static String lies(Path p) {
		try {
			byte[] alles = Files.readAllBytes(p);
			int wieviel = Math.min(alles.length, 200_000);
			return new String(alles, 0, wieviel, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
		} catch (IOException e) {
			return "";
		}
	}

	private static void deute(Befund b, String text) {
		for (String[] m : MUSTER) {
			if (text.contains(m[0])) {
				b.ueberschrift = m[1];
				b.erklaerung = m[2];
				b.rat = m[3];
				return;
			}
		}
		b.ueberschrift = "Unbekannter Fehler";
		b.erklaerung = "Diesen Fehler kenne ich noch nicht.";
		b.rat = "Öffne den Bericht und schick die ersten 20 Zeilen weiter – daraus lässt sich fast immer erkennen, woran es lag.";
	}

	/** Die ersten Zeilen des Berichts, für die Anzeige im Launcher. */
	static String anfang(Path datei, int zeilen) {
		try {
			StringBuilder sb = new StringBuilder();
			int i = 0;
			for (String z : Files.readAllLines(datei, StandardCharsets.UTF_8)) {
				if (i++ >= zeilen) break;
				sb.append(z).append('\n');
			}
			return sb.toString();
		} catch (IOException e) {
			return "";
		}
	}
}
