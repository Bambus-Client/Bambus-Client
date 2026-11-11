package de.bambus.launcher;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Mod-Icons von Modrinth und CurseForge: im Hintergrund geladen, auf Icon-Größe verkleinert und auf der
 * Festplatte zwischengespeichert (Spielordner/cache/mod-icons), damit sie beim nächsten Start sofort da sind.
 * Bilder, die Java nicht lesen kann (z. B. WebP), bekommen stattdessen den Anfangsbuchstaben.
 */
final class ModIcons {

	/** Gespeicherte Größe – gezeichnet wird 28 × 28, doppelt so groß für scharfe Ränder bei Windows-Skalierung. */
	private static final int GROESSE = 56;

	private final Path ordner;
	private final Runnable neuZeichnen;
	private final Map<String, BufferedImage> bilder = new ConcurrentHashMap<>();
	private final Set<String> laedt = ConcurrentHashMap.newKeySet();
	private final Set<String> fehlgeschlagen = ConcurrentHashMap.newKeySet();
	private final ExecutorService arbeiter = Executors.newFixedThreadPool(3, r -> {
		Thread t = new Thread(r, "Bambus-Mod-Icons");
		t.setDaemon(true);
		return t;
	});

	ModIcons(Path ordner, Runnable neuZeichnen) {
		this.ordner = ordner;
		this.neuZeichnen = neuZeichnen;
	}

	/** Das Icon – oder null, solange es lädt oder es keins gibt. Startet das Laden beim ersten Aufruf. */
	BufferedImage icon(String adresse) {
		if (adresse == null || adresse.isEmpty()) {
			return null;
		}
		BufferedImage b = bilder.get(adresse);
		if (b != null || fehlgeschlagen.contains(adresse) || !laedt.add(adresse)) {
			return b;
		}
		arbeiter.execute(() -> {
			try {
				BufferedImage geladen = lade(adresse);
				if (geladen != null) {
					bilder.put(adresse, geladen);
					SwingUtilities.invokeLater(neuZeichnen);
				} else {
					fehlgeschlagen.add(adresse);
				}
			} catch (Exception e) {
				fehlgeschlagen.add(adresse);
			} finally {
				laedt.remove(adresse);
			}
		});
		return null;
	}

	private BufferedImage lade(String adresse) throws Exception {
		Path datei = ordner.resolve(name(adresse) + ".png");
		if (Files.isRegularFile(datei)) {
			BufferedImage b = ImageIO.read(datei.toFile());
			if (b != null) return b;
		}
		if (!adresse.startsWith("https://")) return null;
		BufferedImage roh = ImageIO.read(new ByteArrayInputStream(Installer.lade(adresse)));
		if (roh == null) return null;   // Format, das Java nicht kennt
		BufferedImage klein = verkleinere(roh);
		try {
			Files.createDirectories(ordner);
			ImageIO.write(klein, "png", datei.toFile());
		} catch (Exception ignoriert) {
			// dann eben ohne Zwischenspeicher
		}
		return klein;
	}

	/** In mehreren Halbierungs-Schritten verkleinern – sieht bei großen Icons viel sauberer aus als ein Schritt. */
	private static BufferedImage verkleinere(BufferedImage roh) {
		Image bild = roh;
		int b = roh.getWidth(), h = roh.getHeight();
		int seite = Math.max(1, Math.min(b, h));
		// quadratisch zuschneiden (Mitte), wie bei Modrinth
		BufferedImage quadrat = new BufferedImage(seite, seite, BufferedImage.TYPE_INT_ARGB);
		Graphics2D q = quadrat.createGraphics();
		q.drawImage(bild, -(b - seite) / 2, -(h - seite) / 2, null);
		q.dispose();
		BufferedImage aktuell = quadrat;
		while (aktuell.getWidth() / 2 >= GROESSE) {
			aktuell = skaliere(aktuell, aktuell.getWidth() / 2);
		}
		return aktuell.getWidth() == GROESSE ? aktuell : skaliere(aktuell, GROESSE);
	}

	private static BufferedImage skaliere(BufferedImage b, int seite) {
		BufferedImage neu = new BufferedImage(seite, seite, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = neu.createGraphics();
		// Kleine Pixel-Icons (z. B. 16×16) blockig vergrößern, große weich verkleinern
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, b.getWidth() < seite
				? RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR : RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g.drawImage(b, 0, 0, seite, seite, null);
		g.dispose();
		return neu;
	}

	private static String name(String adresse) throws Exception {
		byte[] h = MessageDigest.getInstance("SHA-1").digest(adresse.getBytes(StandardCharsets.UTF_8));
		StringBuilder s = new StringBuilder();
		for (int i = 0; i < 12; i++) s.append(String.format("%02x", h[i]));
		return s.toString();
	}
}
