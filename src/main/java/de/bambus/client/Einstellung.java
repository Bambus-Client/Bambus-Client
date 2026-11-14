package de.bambus.client;

import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * Eine Zeile im Einstellungsfenster (⋮ oder Rechtsklick im Mod-Menü):
 * ein Schalter (an/aus) oder eine Stufe (‹ Wert ›).
 */
public sealed interface Einstellung permits Einstellung.Schalter, Einstellung.Stufe {

	String name();

	/** Ein/Aus-Schalter. */
	record Schalter(String name, BooleanSupplier an, Runnable umschalten) implements Einstellung {}

	/**
	 * Zahl mit ‹ › in festen Schritten, z. B. „Feuer tiefer −30 %“.
	 * aktiv: nur bedienbar, wenn der zugehörige Schalter an ist (sonst ausgegraut).
	 */
	record Stufe(String name, IntSupplier wert, IntConsumer setzen, int min, int max, int schritt, String format,
				 BooleanSupplier aktiv, java.util.function.IntFunction<String> namen) implements Einstellung {

		/** Zahl als Text über das Format (z. B. „%d %%“). */
		public Stufe(String name, IntSupplier wert, IntConsumer setzen, int min, int max, int schritt, String format,
					 BooleanSupplier aktiv) {
			this(name, wert, setzen, min, max, schritt, format, aktiv, null);
		}

		/** Auswahl mit Namen statt Zahlen (z. B. Schriftarten); ‹ › läuft im Kreis. */
		public static Stufe auswahl(String name, String[] namen, IntSupplier wert, IntConsumer setzen, BooleanSupplier aktiv) {
			return new Stufe(name, wert, setzen, 0, namen.length - 1, 1, "%d", aktiv,
					i -> namen[Math.floorMod(i, namen.length)]);
		}

		public void aendern(int richtung) {
			int neu = wert.getAsInt() + richtung * schritt;
			if (namen != null) {
				neu = Math.floorMod(neu - min, max - min + 1) + min;   // Auswahl: im Kreis
			}
			setzen.accept(Math.max(min, Math.min(max, neu)));
		}

		public String text() {
			return namen != null ? namen.apply(wert.getAsInt()) : String.format(format, wert.getAsInt());
		}
	}
}
