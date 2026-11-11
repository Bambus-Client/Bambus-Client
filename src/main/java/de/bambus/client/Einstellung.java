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
				 BooleanSupplier aktiv) implements Einstellung {

		public void aendern(int richtung) {
			setzen.accept(Math.max(min, Math.min(max, wert.getAsInt() + richtung * schritt)));
		}

		public String text() {
			return String.format(format, wert.getAsInt());
		}
	}
}
