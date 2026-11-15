package de.bambus.launcher;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

/**
 * Macht Klicks im ganzen Launcher zuverlässig.
 *
 * Java meldet einen Klick (mouseClicked) nur, wenn sich die Maus zwischen Drücken und Loslassen nicht bewegt –
 * unter Windows reicht schon ein Pixel, und der Klick ist weg. Dann musste man zwei- oder dreimal klicken.
 * Dieser Helfer schaut allen Mausereignissen zu: Wurde auf einem Element gedrückt und über demselben Element
 * losgelassen, aber Java hat keinen Klick gemeldet, schickt er den Klick nach. Echte Klicks bleiben unverändert.
 */
final class KlickHilfe implements AWTEventListener {

	/** Vom Helfer nachgereichter Klick – wird selbst nicht noch einmal beobachtet. */
	private static final class Nachgereicht extends MouseEvent {
		Nachgereicht(Component quelle, MouseEvent los) {
			super(quelle, MOUSE_CLICKED, los.getWhen(), los.getModifiers(), los.getX(), los.getY(),
					los.getXOnScreen(), los.getYOnScreen(), Math.max(1, los.getClickCount()), false, los.getButton());
		}
	}

	private static boolean installiert;

	private Component gedrueckt;
	private int knopf;
	/** Zählt mit, ob seit dem Loslassen ein echter Klick kam. */
	private long echteKlicks;

	private KlickHilfe() {}

	static synchronized void installieren() {
		if (installiert) return;
		installiert = true;
		Toolkit.getDefaultToolkit().addAWTEventListener(new KlickHilfe(), AWTEvent.MOUSE_EVENT_MASK);
	}

	@Override
	public void eventDispatched(AWTEvent ereignis) {
		if (!(ereignis instanceof MouseEvent) || ereignis instanceof Nachgereicht) return;
		MouseEvent e = (MouseEvent) ereignis;
		Component c = e.getComponent();
		if (!(c instanceof JComponent)) return;   // nur die eigentlichen Bauteile, nicht das Fenster darum
		if (c instanceof javax.swing.text.JTextComponent) return;   // Textfelder: Markieren mit der Maus ist kein Klick
		switch (e.getID()) {
			case MouseEvent.MOUSE_PRESSED:
				gedrueckt = c;
				knopf = e.getButton();
				break;
			case MouseEvent.MOUSE_CLICKED:
				echteKlicks++;
				break;
			case MouseEvent.MOUSE_RELEASED:
				Component war = gedrueckt;
				gedrueckt = null;
				if (war != c || e.getButton() != knopf || !c.contains(e.getPoint()) || c.getMouseListeners().length == 0) return;
				long vorher = echteKlicks;
				MouseEvent nachgereicht = new Nachgereicht(c, e);
				// erst nach dem Loslassen nachsehen: Kam Javas eigener Klick, passiert nichts weiter
				SwingUtilities.invokeLater(() -> {
					if (echteKlicks == vorher && c.isShowing()) {
						c.dispatchEvent(nachgereicht);
					}
				});
				break;
			default:
				break;
		}
	}
}
