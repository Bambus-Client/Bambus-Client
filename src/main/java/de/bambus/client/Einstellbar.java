package de.bambus.client;

import java.util.List;

/** Etwas mit eigenem Einstellungsfenster im Mod-Menü: jedes HUD-Modul und das Textur-Modul. */
public interface Einstellbar {

	/** Überschrift des Fensters. */
	String titel();

	/** Name des Symbols (siehe screen.Symbole). */
	String symbol();

	/** Alle Zeilen des Fensters, in Anzeige-Reihenfolge. */
	List<Einstellung> einstellungen();

	/** Alles auf Standard zurück (Knopf STANDARD). */
	void standard();
}
