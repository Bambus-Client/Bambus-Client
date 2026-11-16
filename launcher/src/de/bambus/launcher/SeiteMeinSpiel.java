package de.bambus.launcher;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BasicStroke;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * MEIN SPIEL: vier Bereiche rund ums Spielen – die Bilder aus dem Spiel, die Spielzeit,
 * die Tastenbelegung und Hilfe, wenn Minecraft abgestürzt ist.
 */
final class SeiteMeinSpiel extends JPanel {

	private static final int X = 40, B = 696;
	private static final String[][] BEREICHE = {
			{"bilder", "BILDER"}, {"zeit", "SPIELZEIT"}, {"tasten", "TASTEN"}, {"hilfe", "PROBLEME"},
	};

	private final BambusLauncher l;
	private final CardLayout karten = new CardLayout();
	private final JPanel inhalt = new JPanel(karten);
	private final List<NeonUI.UmrissKnopf> reiter = new ArrayList<>();
	private String offen = "bilder";

	private final Bilder bilder;
	private final Spielzeit zeit;
	private final Tasten tasten;
	private final Hilfe hilfe;

	SeiteMeinSpiel(BambusLauncher l) {
		this.l = l;
		setOpaque(false);
		setLayout(null);

		for (int i = 0; i < BEREICHE.length; i++) {
			final String id = BEREICHE[i][0];
			NeonUI.UmrissKnopf k = new NeonUI.UmrissKnopf(BEREICHE[i][1], id.equals(offen) ? NeonUI.NEON : NeonUI.GRAU);
			k.setBounds(X + i * 150, 100, 142, 34);
			k.addActionListener(e -> zeigeBereich(id));
			reiter.add(k);
			add(k);
		}

		inhalt.setOpaque(false);
		inhalt.setBounds(X - 4, 150, B + 14, 436);
		bilder = new Bilder();
		zeit = new Spielzeit();
		tasten = new Tasten();
		hilfe = new Hilfe();
		inhalt.add(bilder, "bilder");
		inhalt.add(zeit, "zeit");
		inhalt.add(tasten, "tasten");
		inhalt.add(hilfe, "hilfe");
		add(inhalt);
	}

	private void zeigeBereich(String id) {
		offen = id;
		for (int i = 0; i < reiter.size(); i++) {
			reiter.get(i).setzeFarbe(BEREICHE[i][0].equals(id) ? NeonUI.NEON : NeonUI.GRAU);
		}
		karten.show(inhalt, id);
		aktualisiere();
		repaint();
	}

	/** Beim Öffnen der Seite: alles frisch einlesen. */
	void aktualisiere() {
		if (offen.equals("bilder")) bilder.sucheBilder();
		if (offen.equals("tasten")) tasten.neuLesen();
		if (offen.equals("hilfe")) hilfe.neuLesen();
		repaint();
	}

	@Override
	protected void paintComponent(Graphics graphics) {
		Graphics2D g = NeonUI.glatt(graphics);
		g.setFont(NeonUI.titel(30));
		NeonUI.leuchtText(g, "MEIN SPIEL", X, 62, NeonUI.TEXT, 1);
		g.setFont(NeonUI.normal(13));
		g.setColor(NeonUI.GRAU);
		String unter = offen.equals("bilder") ? "Deine Screenshots aus dem Spiel – anklicken zum Vergrößern."
				: offen.equals("zeit") ? "Wie lange du gespielt hast. Zählt nur, während Bambus läuft."
				: offen.equals("tasten") ? "Tastenbelegung ändern, ohne erst ins Spiel zu gehen."
				: "Wenn Minecraft abgestürzt ist, steht hier im Klartext, woran es lag.";
		g.drawString(NeonUI.passend(g, unter, B), X, 86);
		g.dispose();
	}

	// ============================================================ 1) Bilder

	private final class Bilder extends JPanel {
		private static final int BB = 164, BH = 104, LUECKE = 12;

		private final JPanel gitter = new JPanel(null);
		private final ExecutorService pool = Executors.newFixedThreadPool(2, r -> {
			Thread t = new Thread(r, "Bambus-Vorschau");
			t.setDaemon(true);
			return t;
		});
		private final Map<Path, BufferedImage> vorschau = new LinkedHashMap<>();
		private List<Path> dateien = new ArrayList<>();
		private boolean gesucht;

		Bilder() {
			setOpaque(false);
			setLayout(null);
			gitter.setOpaque(false);
			add(SeiteMods.scroll(gitter, 0, 44, B + 10, 392));

			NeonUI.UmrissKnopf ordner = new NeonUI.UmrissKnopf("ORDNER ÖFFNEN", NeonUI.CYAN);
			ordner.setBounds(4, 4, 164, 32);
			ordner.addActionListener(e -> l.oeffne(l.installer.startOrdner.resolve("screenshots")));
			add(ordner);
			NeonUI.UmrissKnopf neu = new NeonUI.UmrissKnopf("⟳ NEU LADEN", NeonUI.GRAU);
			neu.setBounds(176, 4, 150, 32);
			neu.addActionListener(e -> { gesucht = false; sucheBilder(); });
			add(neu);
		}

		void sucheBilder() {
			if (gesucht) return;
			gesucht = true;
			Path ordner = l.installer.startOrdner.resolve("screenshots");
			dateien = new ArrayList<>();
			if (Files.isDirectory(ordner)) {
				try (Stream<Path> s = Files.list(ordner)) {
					dateien = s.filter(p -> {
								String n = p.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
								return n.endsWith(".png") || n.endsWith(".jpg");
							})
							.sorted(Comparator.comparingLong((Path p) -> p.toFile().lastModified()).reversed())
							.limit(120)
							.collect(Collectors.toList());
				} catch (Exception ignoriert) {
					// dann bleibt die Liste leer
				}
			}
			baueGitter();
		}

		private void baueGitter() {
			gitter.removeAll();
			int proZeile = Math.max(1, (B + 10) / (BB + LUECKE));
			for (int i = 0; i < dateien.size(); i++) {
				Kachel k = new Kachel(dateien.get(i));
				k.setBounds((i % proZeile) * (BB + LUECKE), (i / proZeile) * (BH + LUECKE), BB, BH);
				gitter.add(k);
			}
			int zeilen = (dateien.size() + proZeile - 1) / proZeile;
			gitter.setPreferredSize(new Dimension(B, Math.max(1, zeilen * (BH + LUECKE))));
			gitter.revalidate();
			gitter.repaint();
			repaint();
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			if (!dateien.isEmpty()) return;
			Graphics2D g = NeonUI.glatt(graphics);
			g.setFont(NeonUI.normal(13));
			g.setColor(NeonUI.GRAU);
			g.drawString(Sprache.t("Noch keine Bilder. Im Spiel macht F2 einen Screenshot."), 6, 110);
			g.dispose();
		}

		private final class Kachel extends JComponent {
			private final Path datei;
			private boolean ueber;

			Kachel(Path datei) {
				this.datei = datei;
				setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
				addMouseListener(new MouseAdapter() {
					@Override public void mouseEntered(MouseEvent e) { ueber = true; repaint(); }
					@Override public void mouseExited(MouseEvent e) { ueber = false; repaint(); }
					@Override public void mouseClicked(MouseEvent e) { oeffneBild(datei); }
				});
				if (!vorschau.containsKey(datei)) pool.submit(() -> {   // schon geladen: nicht noch einmal entpacken
					BufferedImage klein = lade(datei);
					if (klein != null) {
						SwingUtilities.invokeLater(() -> {
							vorschau.put(datei, klein);
							repaint();
						});
					}
				});
			}

			@Override
			protected void paintComponent(Graphics graphics) {
				Graphics2D g = NeonUI.glatt(graphics);
				int b = getWidth(), h = getHeight();
				BufferedImage v = vorschau.get(datei);
				g.setColor(NeonUI.KARTE);
				g.fillRoundRect(0, 0, b, h, 10, 10);
				if (v != null) {
					g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
					int vb = v.getWidth(), vh = v.getHeight();
					double s = Math.min(b / (double) vb, (h - 18) / (double) vh);
					int zb = (int) (vb * s), zh = (int) (vh * s);
					g.drawImage(v, (b - zb) / 2, (h - 18 - zh) / 2, zb, zh, null);
				}
				g.setFont(NeonUI.mono(9));
				g.setColor(NeonUI.alpha(NeonUI.GRAU, 220));
				String name = datei.getFileName().toString();
				g.drawString(NeonUI.passend(g, name, b - 8), 4, h - 5);
				g.setStroke(new BasicStroke(ueber ? 2f : 1f));
				g.setColor(ueber ? NeonUI.NEON : NeonUI.LINIE);
				g.drawRoundRect(0, 0, b - 1, h - 1, 10, 10);
				g.dispose();
			}
		}

		private BufferedImage lade(Path p) {
			try {
				BufferedImage voll = ImageIO.read(p.toFile());
				if (voll == null) return null;
				int zb = 320, zh = Math.max(1, voll.getHeight() * zb / Math.max(1, voll.getWidth()));
				BufferedImage klein = new BufferedImage(zb, zh, BufferedImage.TYPE_INT_RGB);
				Graphics2D g = klein.createGraphics();
				g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
				g.drawImage(voll, 0, 0, zb, zh, null);
				g.dispose();
				return klein;
			} catch (Exception e) {
				return null;
			}
		}
	}

	/** Bild im Bildbetrachter des Computers öffnen. */
	private void oeffneBild(Path datei) {
		try {
			Desktop.getDesktop().open(datei.toFile());
		} catch (Exception e) {
			javax.swing.JOptionPane.showMessageDialog(l, Sprache.t("Das Bild liegt hier:") + "\n" + datei,
					Sprache.t("Bild"), javax.swing.JOptionPane.INFORMATION_MESSAGE);
		}
	}

	// ============================================================ 2) Spielzeit

	private final class Spielzeit extends JPanel {
		Spielzeit() {
			setOpaque(false);
			setLayout(null);
		}

		/** Spielzeit je Tag aus der bambus.json: „2026-11-14:35,2026-11-13:120“. */
		private Map<String, Integer> tage() {
			Map<String, Integer> aus = new LinkedHashMap<>();
			for (String t : l.client.text("spielVerlauf", "").split(",")) {
				int d = t.lastIndexOf(':');
				if (d <= 0) continue;
				try {
					aus.put(t.substring(0, d).trim(), Integer.parseInt(t.substring(d + 1).trim()));
				} catch (NumberFormatException ignoriert) {
					// kaputter Eintrag
				}
			}
			return aus;
		}

		private String dauer(int minuten) {
			if (minuten < 60) return minuten + " min";
			return (minuten / 60) + " h " + String.format("%02d", minuten % 60) + " min";
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			Graphics2D g = NeonUI.glatt(graphics);
			Map<String, Integer> tage = tage();
			LocalDate heute = LocalDate.now();
			int heuteMin = tage.getOrDefault(heute.toString(), 0);
			int woche = 0;
			for (int i = 0; i < 7; i++) woche += tage.getOrDefault(heute.minusDays(i).toString(), 0);
			int gesamt = l.client.zahl("spielminuten", 0);

			String[][] kacheln = {
					{"HEUTE", dauer(heuteMin)},
					{"LETZTE 7 TAGE", dauer(woche)},
					{"INSGESAMT", dauer(gesamt)},
			};
			for (int i = 0; i < kacheln.length; i++) {
				int x = i * 234, b = 222, h = 78;
				RoundRectangle2D form = new RoundRectangle2D.Float(x, 0, b, h, 12, 12);
				g.setColor(NeonUI.KARTE);
				g.fill(form);
				g.setStroke(new BasicStroke(1.2f));
				g.setColor(NeonUI.LINIE);
				g.draw(form);
				g.setFont(NeonUI.mono(10));
				g.setColor(NeonUI.NEON);
				g.drawString(Sprache.t(kacheln[i][0]), x + 14, 24);
				g.setFont(NeonUI.titel(24));
				g.setColor(NeonUI.TEXT);
				g.drawString(kacheln[i][1], x + 14, 58);
			}

			// Balken der letzten 14 Tage
			g.setFont(NeonUI.mono(10));
			g.setColor(NeonUI.NEON);
			g.drawString(Sprache.t("LETZTE 14 TAGE"), 0, 118);
			int hoechst = 1;
			for (int i = 0; i < 14; i++) hoechst = Math.max(hoechst, tage.getOrDefault(heute.minusDays(i).toString(), 0));
			int bx = 4, by = 132, bh = 150, bb = 44;
			for (int i = 13; i >= 0; i--) {
				LocalDate tag = heute.minusDays(i);
				int min = tage.getOrDefault(tag.toString(), 0);
				int x = bx + (13 - i) * (bb + 6);
				int hoehe = Math.max(min > 0 ? 3 : 1, bh * min / hoechst);
				g.setColor(new Color(0x18212F));
				g.fillRoundRect(x, by, bb - 8, bh, 4, 4);
				g.setColor(i == 0 ? NeonUI.CYAN : NeonUI.alpha(NeonUI.NEON, 200));
				g.fillRoundRect(x, by + bh - hoehe, bb - 8, hoehe, 4, 4);
				g.setFont(NeonUI.mono(9));
				g.setColor(NeonUI.alpha(NeonUI.GRAU, 200));
				g.drawString(String.format("%02d.%02d", tag.getDayOfMonth(), tag.getMonthValue()), x, by + bh + 16);
				if (min > 0) {
					g.setColor(NeonUI.alpha(NeonUI.TEXT, 200));
					g.drawString(String.valueOf(min), x + 2, by + bh - hoehe - 4);
				}
			}

			g.setFont(NeonUI.normal(11));
			g.setColor(NeonUI.alpha(NeonUI.GRAU, 200));
			g.drawString(NeonUI.passend(g, "Gezählt wird nur die Zeit mit Bambus im Spiel. Alle 10 Minuten gibt es dafür Blätter.", B), 0, by + bh + 46);
			g.dispose();
		}
	}

	// ============================================================ 3) Tasten

	private final class Tasten extends JPanel {
		private static final int ZEILE = 34;

		private final JPanel liste = new JPanel(null);
		private Map<String, String> belegung = new LinkedHashMap<>();
		private String wartetAuf;        // Schlüssel, für den gerade eine Taste erwartet wird
		private String meldung = "";

		Tasten() {
			setOpaque(false);
			setLayout(null);
			liste.setOpaque(false);
			add(SeiteMods.scroll(liste, 0, 30, B + 10, 374));
			neuLesen();
		}

		void neuLesen() {
			belegung = Tastenbelegung.lies(l.installer.startOrdner);
			wartetAuf = null;
			baue();
		}

		private void baue() {
			liste.removeAll();
			for (int i = 0; i < Tastenbelegung.AKTIONEN.length; i++) {
				Zeile z = new Zeile(Tastenbelegung.AKTIONEN[i]);
				z.setBounds(0, i * (ZEILE + 4), B, ZEILE);
				liste.add(z);
			}
			liste.setPreferredSize(new Dimension(B, Tastenbelegung.AKTIONEN.length * (ZEILE + 4)));
			liste.revalidate();
			liste.repaint();
			repaint();
		}

		/** Wie oft ist diese Taste vergeben? Doppelte werden rot angezeigt. */
		private boolean doppelt(String taste) {
			if (Tastenbelegung.FREI.equals(taste)) return false;
			int n = 0;
			for (String t : belegung.values()) if (t.equals(taste)) n++;
			return n > 1;
		}

		private void setze(String schluessel, String taste) {
			try {
				Tastenbelegung.schreibe(l.installer.startOrdner, schluessel, taste);
				belegung.put(schluessel, taste);
				meldung = Tastenbelegung.FREI.equals(taste)
						? "Taste gelöscht – die Aktion hat jetzt keine Taste."
						: "Gespeichert. Minecraft übernimmt es beim nächsten Start.";
			} catch (Exception e) {
				meldung = "Konnte options.txt nicht schreiben. Ist Minecraft noch offen?";
			}
			wartetAuf = null;
			baue();
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			Graphics2D g = NeonUI.glatt(graphics);
			g.setFont(NeonUI.normal(11));
			g.setColor(meldung.startsWith("Konnte") ? new Color(0xFFB0C4) : NeonUI.NEON);
			g.drawString(NeonUI.passend(g, meldung.isEmpty()
					? "Minecraft sollte dabei geschlossen sein, sonst überschreibt es die Datei beim Beenden."
					: meldung, B), 2, 18);
			g.dispose();
		}

		private final class Zeile extends JComponent {
			private final String[] aktion;
			private boolean ueberTaste, ueberWeg;

			Zeile(String[] aktion) {
				this.aktion = aktion;
				setFocusable(true);
				addMouseListener(new MouseAdapter() {
					@Override public void mouseExited(MouseEvent e) { ueberTaste = false; ueberWeg = false; repaint(); }
					@Override public void mouseClicked(MouseEvent e) {
						if (aufWeg(e.getX())) {
							setze(aktion[0], Tastenbelegung.FREI);
						} else if (aufTaste(e.getX())) {
							wartetAuf = aktion[0];
							meldung = "Drück jetzt die neue Taste (Esc bricht ab).";
							requestFocusInWindow();
							liste.repaint();
							SeiteMeinSpiel.this.repaint();
						}
					}
				});
				addMouseMotionListener(new MouseAdapter() {
					@Override public void mouseMoved(MouseEvent e) {
						boolean t = aufTaste(e.getX()), w = aufWeg(e.getX());
						if (t != ueberTaste || w != ueberWeg) {
							ueberTaste = t;
							ueberWeg = w;
							setCursor(Cursor.getPredefinedCursor(t || w ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
							repaint();
						}
					}
				});
				addKeyListener(new KeyAdapter() {
					@Override public void keyPressed(KeyEvent e) {
						if (!aktion[0].equals(wartetAuf)) return;
						if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
							wartetAuf = null;
							meldung = "Abgebrochen.";
							baue();
							return;
						}
						String neu = Tastenbelegung.ausTastendruck(e);
						if (neu == null) {
							meldung = "Diese Taste kann ich nicht übernehmen – probier eine andere.";
							repaint();
							return;
						}
						setze(aktion[0], neu);
					}
				});
			}

			private boolean aufTaste(int x) {
				return x >= B - 230 && x < B - 60;
			}

			private boolean aufWeg(int x) {
				return x >= B - 52 && x < B - 20;
			}

			@Override
			protected void paintComponent(Graphics graphics) {
				Graphics2D g = NeonUI.glatt(graphics);
				int b = getWidth(), h = getHeight();
				boolean bambus = indexVon(aktion) < Tastenbelegung.MINECRAFT_AB;
				g.setColor(NeonUI.KARTE);
				g.fillRoundRect(0, 0, b, h, 8, 8);
				g.setStroke(new BasicStroke(1f));
				g.setColor(NeonUI.LINIE);
				g.drawRoundRect(0, 0, b - 1, h - 1, 8, 8);

				g.setFont(NeonUI.mono(9));
				g.setColor(bambus ? NeonUI.NEON : NeonUI.alpha(NeonUI.GRAU, 180));
				g.drawString(bambus ? "BAMBUS" : "MINECRAFT", 12, h / 2 + 4);
				g.setFont(NeonUI.normal(12));
				g.setColor(NeonUI.TEXT);
				g.drawString(NeonUI.passend(g, aktion[1], B - 330), 100, h / 2 + 5);

				// Tastenfeld
				String taste = belegung.getOrDefault(aktion[0], aktion[2]);
				boolean wartet = aktion[0].equals(wartetAuf);
				Color rahmen = wartet ? NeonUI.CYAN : doppelt(taste) ? new Color(0xFF4D6D)
						: ueberTaste ? NeonUI.NEON : NeonUI.LINIE;
				g.setColor(ueberTaste || wartet ? NeonUI.KARTE_HOVER : NeonUI.alpha(NeonUI.LINIE, 40));
				g.fillRoundRect(b - 230, 4, 170, h - 8, 6, 6);
				g.setStroke(new BasicStroke(wartet ? 2f : 1.2f));
				g.setColor(rahmen);
				g.drawRoundRect(b - 230, 4, 170, h - 8, 6, 6);
				g.setFont(NeonUI.mono(11));
				g.setColor(wartet ? NeonUI.CYAN : NeonUI.TEXT);
				NeonUI.zentriert(g, wartet ? "…" : Tastenbelegung.anzeige(taste), b - 230, 4, 170, h - 8);

				// Löschen
				g.setFont(NeonUI.normal(12));
				g.setColor(ueberWeg ? new Color(0xFF4D6D) : NeonUI.alpha(NeonUI.GRAU, 120));
				g.drawString("✕", b - 44, h / 2 + 5);
				g.dispose();
			}
		}

		private int indexVon(String[] aktion) {
			for (int i = 0; i < Tastenbelegung.AKTIONEN.length; i++) {
				if (Tastenbelegung.AKTIONEN[i] == aktion) return i;
			}
			return 99;
		}
	}

	// ============================================================ 4) Probleme

	private final class Hilfe extends JPanel {
		private Absturzhilfe.Befund befund = new Absturzhilfe.Befund();
		private final NeonUI.UmrissKnopf zeigen = new NeonUI.UmrissKnopf("BERICHT ANSEHEN", NeonUI.CYAN);

		Hilfe() {
			setOpaque(false);
			setLayout(null);
			NeonUI.UmrissKnopf ordner = new NeonUI.UmrissKnopf("ORDNER ÖFFNEN", NeonUI.GRAU);
			ordner.setBounds(4, 4, 164, 32);
			ordner.addActionListener(e -> l.oeffne(l.installer.startOrdner.resolve("crash-reports")));
			add(ordner);
			zeigen.setBounds(176, 4, 170, 32);
			zeigen.addActionListener(e -> zeigeBericht());
			add(zeigen);
			neuLesen();
		}

		void neuLesen() {
			befund = Absturzhilfe.letzter(l.installer.startOrdner);
			zeigen.setVisible(befund.gefunden);
			repaint();
		}

		private void zeigeBericht() {
			if (!befund.gefunden) return;
			javax.swing.JTextArea t = new javax.swing.JTextArea(Absturzhilfe.anfang(befund.datei, 40), 22, 78);
			t.setEditable(false);
			t.setCaretPosition(0);
			javax.swing.JOptionPane.showMessageDialog(l, new javax.swing.JScrollPane(t),
					befund.datei.getFileName().toString(), javax.swing.JOptionPane.PLAIN_MESSAGE);
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			Graphics2D g = NeonUI.glatt(graphics);
			if (!befund.gefunden) {
				g.setFont(NeonUI.titel(18));
				g.setColor(NeonUI.NEON);
				g.drawString(Sprache.t("Alles gut – kein Absturz gefunden."), 4, 96);
				g.setFont(NeonUI.normal(12));
				g.setColor(NeonUI.GRAU);
				g.drawString(NeonUI.passend(g, "Sollte Minecraft einmal abstürzen, steht hier hinterher, woran es lag.", B), 4, 122);
				g.dispose();
				return;
			}

			RoundRectangle2D form = new RoundRectangle2D.Float(2, 54, B, 190, 14, 14);
			g.setColor(NeonUI.KARTE);
			g.fill(form);
			g.setStroke(new BasicStroke(1.4f));
			g.setColor(NeonUI.alpha(new Color(0xFF4D6D), 160));
			g.draw(form);

			g.setFont(NeonUI.mono(10));
			g.setColor(NeonUI.alpha(NeonUI.GRAU, 220));
			g.drawString(new java.text.SimpleDateFormat("dd.MM.yyyy  HH:mm").format(new java.util.Date(befund.wann)), 20, 80);
			g.setFont(NeonUI.titel(18));
			g.setColor(NeonUI.TEXT);
			g.drawString(NeonUI.passend(g, befund.ueberschrift, B - 40), 20, 108);

			g.setFont(NeonUI.normal(13));
			g.setColor(new Color(0xC8D2DE));
			int y = 136;
			for (String zeile : umbrich(g, befund.erklaerung, B - 44)) {
				g.drawString(zeile, 20, y);
				y += 19;
			}
			y += 8;
			g.setFont(NeonUI.mono(10));
			g.setColor(NeonUI.NEON);
			g.drawString(Sprache.t("WAS HILFT"), 20, y);
			y += 18;
			g.setFont(NeonUI.normal(13));
			g.setColor(NeonUI.TEXT);
			for (String zeile : umbrich(g, befund.rat, B - 44)) {
				g.drawString(zeile, 20, y);
				y += 19;
			}
			g.dispose();
		}
	}

	/** Text auf mehrere Zeilen verteilen, ohne Wörter zu zerschneiden. */
	private static List<String> umbrich(Graphics2D g, String text, int breite) {
		List<String> aus = new ArrayList<>();
		StringBuilder zeile = new StringBuilder();
		for (String wort : Sprache.t(text).split(" ")) {
			String probe = zeile.length() == 0 ? wort : zeile + " " + wort;
			if (g.getFontMetrics().stringWidth(probe) > breite && zeile.length() > 0) {
				aus.add(zeile.toString());
				zeile = new StringBuilder(wort);
			} else {
				zeile = new StringBuilder(probe);
			}
		}
		if (zeile.length() > 0) aus.add(zeile.toString());
		return aus;
	}
}
