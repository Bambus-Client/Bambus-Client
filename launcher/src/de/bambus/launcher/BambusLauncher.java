package de.bambus.launcher;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BasicStroke;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Point;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/** Der Bambus Launcher im Neon-Look. */
public final class BambusLauncher extends JFrame {

	/** Version aus dem Build (gradle.properties → mod_version), damit Launcher, Mod und Release-Tag immer gleich sind. */
	static final String VERSION = leseVersion();

	private static String leseVersion() {
		try (InputStream in = BambusLauncher.class.getResourceAsStream("/bambus-version.txt")) {
			String v = in == null ? "" : new String(Installer.liesAlles(in), StandardCharsets.UTF_8).trim();
			if (v.matches("[0-9]+(\\.[0-9]+)*.*")) return v;
		} catch (Exception ignoriert) {
			// dann die Notfall-Version
		}
		return "3.3.1";
	}

	static final int BREITE = 1000;
	static final int HOEHE = 640;
	static final int LEISTE = 224;
	static final int KOPF = 46;

	private static final String[][] SEITEN = {
			{"start", "START", "spielen"},          // {Seite, Name wie im Spiel, Pixel-Symbol}
			{"module", "MOD-MENÜ", "alle"},
			{"kosmetik", "GARDEROBE", "person"},
			{"mods", "MODS", "blitz"},
			{"server", "SERVER", "server"},
			{"neu", "NEUIGKEITEN", "update"},
			{"quests", "QUESTS", "stern"},
			{"helfer", "HELFER", "helfer"},
			{"einstellungen", "EINSTELLUNGEN", "zahnrad"},
	};

	final Installer installer = new Installer(Installer.findeMinecraftOrdner());
	final ClientEinstellungen client = new ClientEinstellungen(installer.spielOrdner);
	final ModVerwaltung mods = new ModVerwaltung(installer);
	final MicrosoftKonto konto = new MicrosoftKonto();
	private final Path launcherDatei = installer.spielOrdner.resolve("launcher.properties");

	int ramGb = 4;
	boolean autoUpdate = true;
	String hintergrund = "partikel";
	boolean eingerichtet = false;
	volatile Updater.Release update;
	volatile String updateStatus = "";
	private UpdateChip updateChip;
	private BlaetterChip blaetterChip;
	boolean taskleisteTippGesehen = false;
	private String seite = "start";

	private final CardLayout karten = new CardLayout();
	private final JPanel inhalt = new JPanel(karten);
	private final List<NavPunkt> nav = new ArrayList<>();

	final SeiteStart start;
	final SeiteModule module;
	final SeiteKosmetik kosmetik;
	final SeiteEinstellungen einstellungen;
	final SeiteMods modsSeite;
	final SeiteServer serverSeite;
	final SeiteHelfer helferSeite;
	SeiteQuests questsSeite;
	final Fortschritt fortschritt = new Fortschritt(client);

	public static void main(String[] args) {
		// Linux: Der XRender-Weg von Java zeichnet gedrehte, teils durchsichtige Bilder fehlerhaft
		// (weiße Flächen in der 3D-Vorschau) – daher den Software-Weg benutzen.
		if ("linux".equals(Installer.betriebssystem()) && System.getProperty("sun.java2d.xrender") == null) {
			System.setProperty("sun.java2d.xrender", "false");
		}
		System.setProperty("awt.useSystemAAFontSettings", "on");
		System.setProperty("swing.aatext", "true");
		// Mac: Name in der Menüleiste und im Dock
		System.setProperty("apple.awt.application.name", "Bambus Launcher");
		System.setProperty("apple.awt.application.appearance", "NSAppearanceNameDarkAqua");
		UIManager.put("ToolTip.background", NeonUI.KARTE);
		UIManager.put("ToolTip.foreground", NeonUI.TEXT);
		SwingUtilities.invokeLater(() -> {
			BambusLauncher l = new BambusLauncher();
			l.setVisible(true);
			if (args.length > 0) {
				l.zeige(args[0]); // für Screenshots: start, module, kosmetik, einstellungen
			}
			if (!l.eingerichtet && args.length == 0) {
				new Willkommen(l).setVisible(true);   // erster Start: Sprache, Name, Look, Leistung
			}
			l.fortschritt.tagesBonus();               // Blätter fürs Einloggen
			l.fortschritt.pruefeErfolge(l);
			if (!l.fortschritt.verlorenerRang.isEmpty()) {
				javax.swing.JOptionPane.showMessageDialog(l,
						Sprache.t("Du warst einen Tag nicht da – die Serie ist gerissen.") + "\n"
								+ Sprache.t("Dein Rang fällt von") + " " + Sprache.t(l.fortschritt.verlorenerRang) + " "
								+ Sprache.t("eine Stufe zurück.") + "\n"
								+ Sprache.t("Deine freigeschaltete Kosmetik behältst du."),
						Sprache.t("Serie gerissen"), javax.swing.JOptionPane.WARNING_MESSAGE);
			}
			if (l.fortschritt.begruessungBlaetter > 0) {
				javax.swing.Timer t = new javax.swing.Timer(900, e -> l.tagesBonusMeldung());
				t.setRepeats(false);
				t.start();
			}
		});
	}

	BambusLauncher() {
		super("Bambus Launcher");
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setUndecorated(true);
		setResizable(false);
		setIconImages(symbole());
		dockSymbol();
		ladeLauncherEinstellungen();
		Hintergruende.ladeBild(installer.spielOrdner);   // eigenes Hintergrundbild, falls vorhanden
		NeonUI.themaAnwenden(client.zahl("thema", 0));   // gleiche Farben wie im Spiel

		Hintergrund wurzel = new Hintergrund();
		wurzel.setLayout(null);
		wurzel.setPreferredSize(new Dimension(BREITE, HOEHE));
		setContentPane(wurzel);

		// Seitenleiste
		for (int i = 0; i < SEITEN.length; i++) {
			NavPunkt p = new NavPunkt(SEITEN[i][0], SEITEN[i][1], SEITEN[i][2]);
			p.setBounds(14, 140 + i * 40, LEISTE - 28, 36);
			nav.add(p);
			wurzel.add(p);
		}

		// Fensterknöpfe
		KontoChip chip = new KontoChip();
		chip.setBounds(BREITE - 330, 8, 226, 30);
		wurzel.add(chip);
		blaetterChip = new BlaetterChip();
		blaetterChip.setBounds(BREITE - 452, 8, 112, 30);
		wurzel.add(blaetterChip);
		updateChip = new UpdateChip();
		updateChip.setBounds(BREITE - 608, 8, 146, 30);
		updateChip.setVisible(false);
		wurzel.add(updateChip);

		FensterKnopf min = new FensterKnopf("—", () -> setState(ICONIFIED));
		min.setBounds(BREITE - 92, 8, 40, 30);
		wurzel.add(min);
		FensterKnopf zu = new FensterKnopf("✕", () -> System.exit(0));
		zu.setBounds(BREITE - 48, 8, 40, 30);
		wurzel.add(zu);

		// Seiten
		inhalt.setOpaque(false);
		inhalt.setBounds(LEISTE, KOPF, BREITE - LEISTE, HOEHE - KOPF);
		start = new SeiteStart(this);
		module = new SeiteModule(this);
		kosmetik = new SeiteKosmetik(this);
		einstellungen = new SeiteEinstellungen(this);
		installer.mods = mods;
		modsSeite = new SeiteMods(this);
		inhalt.add(modsSeite, "mods");
		serverSeite = new SeiteServer(this);
		inhalt.add(serverSeite, "server");
		inhalt.add(new SeiteNeu(this), "neu");
		questsSeite = new SeiteQuests(this);
		inhalt.add(questsSeite, "quests");
		helferSeite = new SeiteHelfer(this);
		inhalt.add(helferSeite, "helfer");
		inhalt.add(start, "start");
		inhalt.add(module, "module");
		inhalt.add(kosmetik, "kosmetik");
		inhalt.add(einstellungen, "einstellungen");
		wurzel.add(inhalt);

		// Fenster verschieben (oben oder in der Seitenleiste ziehen)
		MouseAdapter ziehen = new MouseAdapter() {
			private Point start;

			@Override
			public void mousePressed(MouseEvent e) {
				start = e.getPoint();
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				Point p = getLocation();
				setLocation(p.x + e.getX() - start.x, p.y + e.getY() - start.y);
			}
		};
		wurzel.addMouseListener(ziehen);
		wurzel.addMouseMotionListener(ziehen);

		pack();
		setLocationRelativeTo(null);
		runderRahmen();

		// Animationen (Leuchten, Partikel) – Bildrate passt sich an, siehe animationsSchritt()
		animation = new Timer(BILD_MS, e -> animationsSchritt());
		animation.start();

		zeige("start");
		if (autoUpdate && Updater.aktiv()) {
			pruefeUpdates(false);
		}
		// Solange der Launcher offen ist, alle 30 Minuten wieder nachsehen
		Timer updateTimer = new Timer(30 * 60 * 1000, e -> {
			if (autoUpdate && Updater.aktiv() && !installiertUpdate) pruefeUpdates(false);
		});
		updateTimer.start();
		// Ein wartendes Launcher-Update installieren, sobald du den Launcher nicht mehr benutzt
		addWindowListener(new java.awt.event.WindowAdapter() {
			@Override public void windowDeactivated(java.awt.event.WindowEvent e) {
				if (updateWartet) launcherUpdateWennMoeglich(false);
			}
			@Override public void windowIconified(java.awt.event.WindowEvent e) {
				if (updateWartet) launcherUpdateWennMoeglich(false);
			}
		});
	}

	// ---------------------------------------------------------------- Animation

	private static final int BILD_MS = 33;           // 30 Bilder pro Sekunde, wenn genug Luft ist
	private static final int BILD_MS_LANGSAM = 50;   // 20 Bilder pro Sekunde
	private static final int BILD_MS_SPARSAM = 66;   // 15 Bilder pro Sekunde auf sehr langsamen PCs
	private static final int BILD_MS_HINTERGRUND = 250;   // Fenster nicht im Vordergrund (z. B. während Minecraft läuft)

	private Timer animation;
	/** Gleitender Mittelwert: wie lange ein Bild zum Zeichnen braucht (ms). */
	private double zeichenMs;

	/**
	 * Ein Animationsschritt. Damit Klicks und Hover sofort ankommen, bekommt das Zeichnen nie mehr als etwa
	 * ein Drittel der Zeit: braucht ein Bild länger, sinkt die Bildrate. Liegt das Fenster im Hintergrund,
	 * läuft die Animation nur noch ganz langsam weiter und lässt Minecraft die Rechenzeit.
	 */
	private void animationsSchritt() {
		if (!isVisible() || getState() == ICONIFIED) return;
		if (NeonUI.rgb()) NeonUI.themaAnwenden(NeonUI.thema());   // RGB-Thema dreht sich weiter
		int ziel = !isActive() ? BILD_MS_HINTERGRUND
				: zeichenMs > 22 ? BILD_MS_SPARSAM
				: zeichenMs > 12 ? BILD_MS_LANGSAM
				: BILD_MS;
		if (animation.getDelay() != ziel) animation.setDelay(ziel);
		inhalt.repaint();
		if (updateChip != null && updateChip.isVisible()) updateChip.repaint();
		for (NavPunkt p : nav) {
			if (p.id.equals(seite)) {
				p.repaint();
			}
		}
	}

	private void runderRahmen() {
		try {
			GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
			if (gd.isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSPARENT)) {
				setShape(new RoundRectangle2D.Double(0, 0, BREITE, HOEHE, 22, 22));
			}
		} catch (Exception ignoriert) {
			// eckig ist auch in Ordnung
		}
	}

	/**
	 * Meldet eine Tat an das Quest-System und zeigt eine kurze Meldung, wenn es Blätter gab.
	 * Darf von überall aufgerufen werden – wenn heute keine passende Quest dran ist, passiert nichts.
	 */
	void belohne(String was) {
		if (fortschritt == null) return;
		int p = fortschritt.tat(was);
		if (p > 0) blaetterMeldung(p);
		if (questsSeite != null) questsSeite.repaint();
	}

	/** Begrüßung beim ersten Start am Tag: Serie und Blätter. */
	void tagesBonusMeldung() {
		blaetterMeldung(fortschritt.begruessungBlaetter);
		if (questsSeite != null) questsSeite.repaint();
		for (NavPunkt p : nav) p.repaint();
	}

	/** Kurzer Hinweis unten rechts: „+15 Blätter“ – bei negativer Zahl eine Ausgabe. */
	void blaetterMeldung(int blaetter) {
		if (blaetterChip != null) blaetterChip.repaint();
		javax.swing.JWindow w = new javax.swing.JWindow(this);
		javax.swing.JComponent c = new javax.swing.JComponent() {
			@Override protected void paintComponent(java.awt.Graphics graphics) {
				Graphics2D g = NeonUI.glatt(graphics);
				g.setColor(NeonUI.KARTE);
				g.fillRect(0, 0, getWidth(), getHeight());
				g.setStroke(new java.awt.BasicStroke(1.6f));
				g.setColor(blaetter < 0 ? NeonUI.CYAN : NeonUI.NEON);
				g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
				java.awt.Color f = blaetter < 0 ? NeonUI.CYAN : NeonUI.NEON;
				SeiteQuests.blatt(g, 24, getHeight() / 2 - 3, 16, f);
				g.setFont(NeonUI.titel(16));
				NeonUI.schattenText(g, (blaetter < 0 ? "−" + (-blaetter) : "+" + blaetter) + " " + Sprache.t("Blätter"),
						44, getHeight() / 2 + 6, NeonUI.TEXT);
				g.dispose();
			}
		};
		w.setContentPane(c);
		w.setSize(200, 44);
		w.setLocation(getX() + getWidth() - 232, getY() + getHeight() - 84);
		w.setVisible(true);
		javax.swing.Timer t = new javax.swing.Timer(2200, e -> w.dispose());
		t.setRepeats(false);
		t.start();
	}

	void zeige(String id) {
		seite = id;
		karten.show(inhalt, id);
		if ("start".equals(id)) {
			start.aktualisiere();
		}
		if ("mods".equals(id)) {
			modsSeite.aktualisiere();
		}
		if ("server".equals(id)) {
			serverSeite.aktualisiere();
		}
		if ("helfer".equals(id)) {
			helferSeite.fokus();
		}
		if ("quests".equals(id)) {
			questsSeite.aktualisiere();
		}
		belohne(id.equals("kosmetik") ? "garderobe" : id);
		for (NavPunkt p : nav) {
			p.repaint();
		}
		getContentPane().repaint();
	}

	// ================================================================== Spielen

	void spielen() {
		if (!Files.isDirectory(installer.mcOrdner)) {
			JOptionPane.showMessageDialog(this, Sprache.t("Der Minecraft-Ordner wurde nicht gefunden:\n" + installer.mcOrdner
							+ "\n\nInstalliere zuerst Minecraft (den offiziellen Minecraft Launcher von minecraft.net),\n"
							+ "melde dich an und starte das Spiel einmal. Danach hier auf SPIELEN klicken."), Sprache.t("Minecraft fehlt"), JOptionPane.WARNING_MESSAGE);
			return;
		}

		Path modDatei = null;
		if (installer.brauchtModDatei()) {
			// Vorschau-Version: der Spiel-Teil ist noch nicht eingebaut
			Object[] wahlKnoepfe = {Sprache.t("Verstanden"), Sprache.t("Ich habe die Datei")};
			int antwort = JOptionPane.showOptionDialog(this, Sprache.t("Das ist eine Vorschau des Bambus Launchers.\n\n"
							+ "Du kannst dir schon alles ansehen: Module, Kosmetik mit 3D-Figur, Mods, Server und Einstellungen.\n"
							+ "Zum Spielen fehlt noch der Spiel-Teil (die Datei bambus-client-" + VERSION + ".jar).\n"
							+ "Er kommt mit der fertigen Version (Setup für Windows bzw. .dmg für den Mac) automatisch mit."), Sprache.t("Vorschau-Version"), JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, wahlKnoepfe, wahlKnoepfe[0]);
			if (antwort != 1) {
				return;
			}
			JFileChooser wahl = new JFileChooser();
			wahl.setDialogTitle("Bambus-Client-Mod auswählen");
			wahl.setFileFilter(new FileNameExtensionFilter("Minecraft-Mod (*.jar)", "jar"));
			if (wahl.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
				return;
			}
			modDatei = wahl.getSelectedFile().toPath();
		}

		start.beschaeftigt(true);
		start.fortschritt("Starte …", 2);
		speichereLauncherEinstellungen();

		final Path mod = modDatei;
		final int ram = ramGb;
		final boolean direkt = konto.angemeldet() && konto.direktStart();
		Thread arbeit = new Thread(() -> {
			try {
				Installer.Fortschritt f = (text, prozent) -> SwingUtilities.invokeLater(() -> start.fortschritt(text, prozent));
				installer.installiere(ram, mod, f);
				if (direkt) {
					// Mit Microsoft angemeldet: Minecraft direkt aus Bambus starten
					MicrosoftKonto.Sitzung s = konto.sitzung();
					new DirektStart(installer).starte(s, ram, f);
					SwingUtilities.invokeLater(this::fertigDirekt);
					return;
				}
				boolean gestartet = Installer.starteMinecraftLauncher();
				SwingUtilities.invokeLater(() -> fertig(gestartet));
			} catch (Exception ex) {
				SwingUtilities.invokeLater(() -> fehler(ex));
			}
		}, "Bambus-Installation");
		arbeit.setDaemon(true);
		arbeit.start();
	}

	private void fertigDirekt() {
		start.beschaeftigt(false);
		start.aktualisiere();
		start.fortschritt("Minecraft startet – viel Spaß, " + konto.name() + "!", 100);
		if (!installer.letzteHinweise.isEmpty()) {
			JOptionPane.showMessageDialog(this, Sprache.t("Hinweis zu den Mods:\n• " + String.join("\n• ", installer.letzteHinweise)), Sprache.t("Bambus Client"), JOptionPane.INFORMATION_MESSAGE);
		}
		setState(ICONIFIED);
	}

	private void fertig(boolean launcherGestartet) {
		start.beschaeftigt(false);
		start.aktualisiere();
		String hinweis = "Im Minecraft Launcher das Profil „" + Installer.PROFIL_NAME + "“ wählen\n"
				+ "(unten links neben dem grünen Spielen-Knopf) und auf „Spielen“ klicken.\n\n"
				+ "War der Minecraft Launcher schon offen? Dann schließe ihn einmal\n"
				+ "und öffne ihn neu, damit er das Bambus-Profil sieht.";
		if (!installer.letzteHinweise.isEmpty()) {
			hinweis += "\n\nHinweis zu den Mods:\n• " + String.join("\n• ", installer.letzteHinweise);
		}
		if (launcherGestartet) {
			start.fortschritt("Minecraft wird geöffnet – Profil „Bambus Client“ wählen.", 100);
			JOptionPane.showMessageDialog(this, Sprache.t("Alles bereit!\n\n" + hinweis), Sprache.t("Bambus Client"), JOptionPane.INFORMATION_MESSAGE);
		} else {
			start.fortschritt("Bereit – öffne jetzt Minecraft.", 100);
			JOptionPane.showMessageDialog(this, Sprache.t("Alles bereit! Minecraft konnte ich aber nicht automatisch öffnen.\n"
							+ "Öffne es bitte selbst.\n\n" + hinweis), Sprache.t("Bambus Client"), JOptionPane.INFORMATION_MESSAGE);
		}
	}

	private void fehler(Exception ex) {
		start.beschaeftigt(false);
		start.fortschritt("Fehler – siehe Meldung.", 0);
		String text = ex.getMessage() != null ? ex.getMessage() : ex.toString();
		JOptionPane.showMessageDialog(this, Sprache.t("Da ist etwas schiefgegangen:\n\n" + text
						+ "\n\nPrüfe deine Internetverbindung und versuche es noch einmal."), Sprache.t("Bambus Launcher"), JOptionPane.ERROR_MESSAGE);
	}

	void oeffne(Path ordner) {
		try {
			Files.createDirectories(ordner);
			Desktop.getDesktop().open(ordner.toFile());
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(this, Sprache.t("Der Ordner liegt hier:\n" + ordner), Sprache.t("Ordner"), JOptionPane.INFORMATION_MESSAGE);
		}
	}

	// ======================================================= Launcher-Einstellungen

	private void ladeLauncherEinstellungen() {
		if (!Files.isRegularFile(launcherDatei)) {
			return;
		}
		try (Reader r = Files.newBufferedReader(launcherDatei, StandardCharsets.UTF_8)) {
			Properties p = new Properties();
			p.load(r);
			ramGb = Math.max(2, Math.min(16, Integer.parseInt(p.getProperty("ram", "4"))));
			taskleisteTippGesehen = Boolean.parseBoolean(p.getProperty("taskleisteTipp", "false"));
			autoUpdate = Boolean.parseBoolean(p.getProperty("autoUpdate", "true"));
			hintergrund = p.getProperty("hintergrund", "partikel");
			eingerichtet = Boolean.parseBoolean(p.getProperty("eingerichtet", "false"));
			if (p.getProperty("sprache") != null) Sprache.setze(p.getProperty("sprache"));
		} catch (Exception ignoriert) {
			ramGb = 4;
		}
	}

	void speichereLauncherEinstellungen() {
		try {
			Files.createDirectories(launcherDatei.getParent());
			try (Writer w = Files.newBufferedWriter(launcherDatei, StandardCharsets.UTF_8)) {
				Properties p = new Properties();
				p.setProperty("ram", String.valueOf(ramGb));
				p.setProperty("taskleisteTipp", String.valueOf(taskleisteTippGesehen));
				p.setProperty("autoUpdate", String.valueOf(autoUpdate));
				p.setProperty("hintergrund", hintergrund);
				p.setProperty("eingerichtet", String.valueOf(eingerichtet));
				p.setProperty("sprache", Sprache.code());
				p.store(w, "Bambus Launcher");
			}
		} catch (Exception ignoriert) {
			// nicht schlimm
		}
	}

	/** Symbol in mehreren Größen – damit es in der Windows-Taskleiste scharf aussieht. */
	private static List<Image> symbole() {
		List<Image> liste = new ArrayList<>();
		Image gross = bild("/bambus-icon-256.png");
		if (gross == null) {
			gross = bild("/bambus-icon.png");
		}
		if (gross == null) {
			return liste;
		}
		for (int groesse : new int[]{16, 20, 24, 32, 40, 48, 64, 128, 256}) {
			BufferedImage b = new BufferedImage(groesse, groesse, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = b.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g.drawImage(gross, 0, 0, groesse, groesse, null);
			g.dispose();
			liste.add(b);
		}
		return liste;
	}

	/** Mac: Symbol im Dock setzen (java.awt.Taskbar gibt es erst ab Java 9, daher per Reflexion). */
	private void dockSymbol() {
		List<Image> bilder = symbole();
		if (bilder.isEmpty()) {
			return;
		}
		try {
			Class<?> taskbar = Class.forName("java.awt.Taskbar");
			Object leiste = taskbar.getMethod("getTaskbar").invoke(null);
			taskbar.getMethod("setIconImage", Image.class).invoke(leiste, bilder.get(bilder.size() - 1));
		} catch (Throwable ignoriert) {
			// Windows/Linux oder Java 8: nicht nötig
		}
	}

	static boolean istWindows() {
		return "windows".equals(Installer.betriebssystem());
	}

	void tippAusblenden() {
		taskleisteTippGesehen = true;
		speichereLauncherEinstellungen();
	}

	static Image bild(String pfad) {
		try (InputStream in = BambusLauncher.class.getResourceAsStream(pfad)) {
			return in == null ? null : ImageIO.read(in);
		} catch (Exception e) {
			return null;
		}
	}

	// ================================================================== Updates

	/** Erste Suche nach dem Start: ein Launcher-Update wird dann sofort installiert. */
	private boolean ersteSuche = true;
	/** Ein Launcher-Update ist geladen und wartet, bis du den Launcher nicht mehr benutzt. */
	private volatile boolean updateWartet;
	private volatile boolean installiertUpdate;

	/**
	 * Neuen Launcher automatisch installieren (Windows): beim Start sofort, später erst, wenn das Fenster
	 * nicht mehr benutzt wird – nie, während SPIELEN gerade etwas einrichtet. Das Installationsprogramm läuft
	 * still durch und öffnet den Launcher danach wieder. Ein laufendes Minecraft bleibt dabei offen.
	 */
	private void launcherUpdateWennMoeglich(boolean sofort) {
		Updater.Release r = update;
		if (r == null || installiertUpdate || !autoUpdate || !Updater.automatischInstallierbar(r)) {
			return;
		}
		if (start.istBeschaeftigt() || (!sofort && isActive())) {
			updateWartet = true;   // später: sobald das Fenster in den Hintergrund geht
			return;
		}
		updateWartet = false;
		installiertUpdate = true;
		updateStatus = "Launcher-Update " + r.version + " wird installiert – der Launcher startet gleich neu …";
		start.fortschritt(updateStatus, 0);
		Thread t = new Thread(() -> {
			try {
				if (Updater.starteLauncherUpdate(r, text -> { updateStatus = text; start.fortschritt(text, 0); })) {
					System.exit(0);   // das Installationsprogramm ersetzt die Dateien und startet den Launcher neu
				}
			} catch (Exception ex) {
				installiertUpdate = false;
				updateStatus = "Launcher-Update fehlgeschlagen: " + ex.getMessage() + " – oben auf UPDATE klicken.";
				SwingUtilities.invokeLater(() -> start.fortschritt(updateStatus, 0));
			}
		}, "Bambus-Launcher-Update");
		t.setDaemon(true);
		t.start();
	}

	/** Sucht im Hintergrund nach einem neuen Release und lädt das Client-Update gleich mit. */
	void pruefeUpdates(boolean manuell) {
		final boolean beimStart = ersteSuche;
		ersteSuche = false;
		updateStatus = "Suche nach Updates …";
		if (einstellungen != null) einstellungen.repaint();
		Thread t = new Thread(() -> {
			try {
				Updater.Release r = Updater.pruefe();
				if (!Updater.neuer(r)) {
					updateStatus = "Du hast die neueste Version (" + VERSION + ").";
					if (manuell) SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, Sprache.t("Du hast schon die neueste Version (" + VERSION + ")."), Sprache.t("Updates"), JOptionPane.INFORMATION_MESSAGE));
				} else {
					update = r;
					SwingUtilities.invokeLater(() -> { updateChip.setVisible(true); updateChip.repaint(); });
					boolean neu = Updater.ladeMod(r, text -> updateStatus = text);
					updateStatus = (neu || Updater.neuereMod() != null)
							? "Client-Update " + r.version + " ist geladen – kommt beim nächsten SPIELEN automatisch rein."
							: "Version " + r.version + " ist da.";
					if (manuell) {
						SwingUtilities.invokeLater(() -> new UpdateDialog(this).setVisible(true));
					} else if (autoUpdate && Updater.automatischInstallierbar(r)) {
						SwingUtilities.invokeLater(() -> launcherUpdateWennMoeglich(beimStart));
					}
				}
			} catch (Exception ex) {
				updateStatus = "Update-Suche: " + (ex.getMessage() == null ? "keine Verbindung" : ex.getMessage());
				if (manuell) SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, Sprache.t(updateStatus), Sprache.t("Updates"), JOptionPane.WARNING_MESSAGE));
			}
			SwingUtilities.invokeLater(() -> { if (einstellungen != null) einstellungen.repaint(); updateChip.repaint(); });
		}, "Bambus-Updates");
		t.setDaemon(true);
		t.start();
	}

	// ================================================================ Bauteile

	/** Leuchtender Hinweis oben, sobald ein Update da ist. */
	private final class UpdateChip extends javax.swing.JComponent {
		private boolean ueber;

		UpdateChip() {
			setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
			addMouseListener(new MouseAdapter() {
				@Override public void mouseEntered(MouseEvent e) { ueber = true; repaint(); }
				@Override public void mouseExited(MouseEvent e) { ueber = false; repaint(); }
				@Override public void mouseClicked(MouseEvent e) { new UpdateDialog(BambusLauncher.this).setVisible(true); }
			});
		}

		@Override
		protected void paintComponent(java.awt.Graphics graphics) {
			java.awt.Graphics2D g = NeonUI.glatt(graphics);
			float puls = (float) (0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 300.0));
			g.setColor(ueber ? NeonUI.KARTE_HOVER : NeonUI.KARTE);
			g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
			g.setColor(NeonUI.alpha(NeonUI.NEON, (int) (120 + 135 * (ueber ? 1 : puls))));
			g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
			g.setFont(NeonUI.mono(11));
			g.setColor(NeonUI.NEON);
			Updater.Release r = update;
			g.drawString("▲  UPDATE " + (r == null ? "" : r.version), 12, getHeight() / 2 + 4);
			g.dispose();
		}
	}


	/** Hintergrund mit Seitenleiste, Gitter und Neon-Licht. */
	private final class Hintergrund extends JPanel {
		/** Misst jedes Bild (inklusive aller Seiten darauf) für die anpassbare Bildrate. */
		@Override
		public void paint(Graphics graphics) {
			long start = System.nanoTime();
			super.paint(graphics);
			double ms = (System.nanoTime() - start) / 1e6;
			zeichenMs = zeichenMs == 0 ? ms : zeichenMs * 0.9 + ms * 0.1;
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			Graphics2D g = NeonUI.glatt(graphics);
			int b = getWidth();
			int h = getHeight();

			g.setColor(NeonUI.HG);
			g.fillRect(0, 0, b, h);
			// animierter Hintergrund (Einstellungen → HINTERGRUND) und farbiges Licht in den Ecken
			java.awt.Shape clip = g.getClip();
			g.clipRect(LEISTE, 0, b - LEISTE, h);
			Hintergruende.zeichne(g, hintergrund, LEISTE, 0, b - LEISTE, h);
			Hintergruende.eckenLicht(g, LEISTE, b - LEISTE, h);
			g.setClip(clip);

			// Seitenleiste
			g.setColor(NeonUI.SEITENLEISTE);
			g.fillRect(0, 0, LEISTE, h);
			g.setPaint(new GradientPaint(LEISTE - 1, 0, NeonUI.alpha(NeonUI.NEON, 120), LEISTE - 1, h,
					NeonUI.alpha(NeonUI.CYAN, 60)));
			g.fillRect(LEISTE - 1, 0, 1, h);

			// Logo
			NeonUI.logo(g, 22, 34, 54);
			g.setFont(NeonUI.titel(30));
			NeonUI.leuchtText(g, "BAMBUS", 64, 66, NeonUI.NEON, 2);
			g.setFont(NeonUI.mono(12));
			g.setColor(NeonUI.CYAN);
			g.drawString("C L I E N T", 66, 86);

			g.setColor(NeonUI.LINIE);
			g.fillRect(20, 122, LEISTE - 40, 1);
			g.setFont(NeonUI.mono(10));
			g.setColor(NeonUI.GRAU);
			g.drawString("NAVIGATION", 24, 140);

			// unten: Versionsinfo
			g.setColor(NeonUI.LINIE);
			g.fillRect(20, h - 128, LEISTE - 40, 1);
			g.setFont(NeonUI.mono(10));
			g.setColor(NeonUI.GRAU);
			g.drawString("VERSION", 24, h - 106);
			g.drawString("MINECRAFT", 24, h - 88);
			g.drawString("LOADER", 24, h - 70);
			g.setColor(NeonUI.TEXT);
			g.drawString(VERSION, 110, h - 106);
			g.drawString(Installer.MC_VERSION, 110, h - 88);
			g.drawString("Fabric " + Installer.LOADER_VERSION, 110, h - 70);
			// Pflichthinweis nach den Minecraft-Markenrichtlinien von Mojang
			g.setFont(new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.PLAIN, 9));   // Pflichthinweis gut lesbar in normaler Schrift
			g.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g.setColor(new Color(0x5C6B80));
			g.drawString("Kein offizielles Minecraft-Produkt.", 24, h - 44);
			g.drawString("Nicht von Mojang oder Microsoft genehmigt", 24, h - 32);
			g.drawString("oder mit ihnen verbunden.", 24, h - 20);

			// Kopfzeile
			g.setFont(NeonUI.mono(11));
			g.setColor(NeonUI.GRAU);
			String pfad = "BAMBUS  //  ";
			g.drawString(pfad, LEISTE + 28, 29);
			int px = LEISTE + 28 + g.getFontMetrics().stringWidth(pfad);
			g.setColor(NeonUI.NEON);
			for (String[] s : SEITEN) {
				if (s[0].equals(seite)) {
					g.drawString(s[1], px, 29);
				}
			}
			g.setColor(NeonUI.LINIE);
			g.fillRect(LEISTE, KOPF - 1, b - LEISTE, 1);

			// Rahmen
			g.setColor(NeonUI.alpha(NeonUI.NEON, 70));
			g.setStroke(new BasicStroke(1.5f));
			g.draw(new RoundRectangle2D.Float(0.75f, 0.75f, b - 1.5f, h - 1.5f, 22, 22));
			g.dispose();
		}
	}

	/** Eintrag in der Seitenleiste. */
	/** Oben rechts: Microsoft-Konto (Anmeldung ist optional). */
	/** Blätter-Guthaben oben rechts neben dem Konto – ein Klick führt zu den Quests. */
	private final class BlaetterChip extends javax.swing.JComponent {
		private boolean ueber;

		BlaetterChip() {
			setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
			setToolTipText("Deine Blätter – auf der Seite QUESTS verdienst du mehr");
			addMouseListener(new MouseAdapter() {
				@Override public void mouseEntered(MouseEvent e) { ueber = true; repaint(); }
				@Override public void mouseExited(MouseEvent e) { ueber = false; repaint(); }
				@Override public void mouseClicked(MouseEvent e) { zeige("quests"); }
			});
		}

		@Override
		protected void paintComponent(java.awt.Graphics graphics) {
			Graphics2D g = NeonUI.glatt(graphics);
			int h = getHeight();
			g.setColor(ueber ? NeonUI.KARTE_HOVER : NeonUI.KARTE);
			g.fillRoundRect(0, 0, getWidth() - 1, h - 1, 14, 14);
			g.setColor(ueber ? NeonUI.NEON : NeonUI.alpha(NeonUI.NEON, 110));
			g.drawRoundRect(0, 0, getWidth() - 1, h - 1, 14, 14);
			SeiteQuests.blatt(g, 16, h / 2 - 3, 14, NeonUI.NEON);
			g.setFont(NeonUI.mono(12));
			g.setColor(NeonUI.TEXT);
			g.drawString(String.valueOf(fortschritt.blaetter()), 30, h / 2 + 4);
			g.setFont(NeonUI.mono(9));
			g.setColor(NeonUI.GRAU);
			String rang = fortschritt.rangName().toUpperCase();
			g.drawString(rang, getWidth() - 10 - g.getFontMetrics().stringWidth(Sprache.t(rang)), h / 2 + 4);
			g.dispose();
		}
	}

	private final class KontoChip extends javax.swing.JComponent {
		private boolean ueber;

		KontoChip() {
			setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
			addMouseListener(new MouseAdapter() {
				@Override public void mouseEntered(MouseEvent e) { ueber = true; repaint(); }
				@Override public void mouseExited(MouseEvent e) { ueber = false; repaint(); }
				@Override public void mouseClicked(MouseEvent e) {
					new MicrosoftDialog(BambusLauncher.this, konto, KontoChip.this::repaint).setVisible(true);
					repaint();
				}
			});
		}

		@Override
		protected void paintComponent(java.awt.Graphics graphics) {
			java.awt.Graphics2D g = NeonUI.glatt(graphics);
			boolean drin = konto.angemeldet();
			java.awt.Color c = drin ? NeonUI.NEON : konto.freigeschaltet() ? NeonUI.CYAN : NeonUI.GRAU;
			g.setColor(ueber ? NeonUI.KARTE_HOVER : NeonUI.KARTE);
			g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
			g.setColor(ueber ? c : NeonUI.LINIE);
			g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
			g.setColor(c);
			g.fillOval(12, getHeight() / 2 - 4, 8, 8);
			g.setFont(NeonUI.mono(11));
			String t = drin ? konto.name() : "MICROSOFT · ANMELDEN";
			g.setColor(drin ? NeonUI.TEXT : NeonUI.GRAU);
			g.drawString(t, 28, getHeight() / 2 + 4);
			g.dispose();
		}
	}

	private final class NavPunkt extends JComponent {
		final String id;
		private final String titel;
		private final String symbol;
		private boolean ueber;

		NavPunkt(String id, String titel, String symbol) {
			this.id = id;
			this.titel = titel;
			this.symbol = symbol;
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			addMouseListener(new MouseAdapter() {
				@Override public void mouseEntered(MouseEvent e) { ueber = true; repaint(); }
				@Override public void mouseExited(MouseEvent e) { ueber = false; repaint(); }
				@Override public void mouseClicked(MouseEvent e) { zeige(NavPunkt.this.id); }
			});
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			Graphics2D g = NeonUI.glatt(graphics);
			int b = getWidth();
			int h = getHeight();
			boolean aktiv = id.equals(seite);
			RoundRectangle2D form = new RoundRectangle2D.Float(1, 1, b - 3, h - 3, 12, 12);
			if (aktiv) {
				g.setPaint(new GradientPaint(0, 0, NeonUI.alpha(NeonUI.NEON, 50), b, 0, NeonUI.alpha(NeonUI.NEON, 0)));
				g.fill(form);
				g.setColor(NeonUI.NEON);
				g.fillRoundRect(0, 9, 4, h - 18, 4, 4);
				g.setColor(NeonUI.alpha(NeonUI.NEON, (int) (40 + 50 * NeonUI.puls())));
				g.fillRoundRect(0, 6, 8, h - 12, 8, 8);
			} else if (ueber) {
				g.setColor(NeonUI.alpha(Color.WHITE, 12));
				g.fill(form);
			}
			Color farbe = aktiv ? NeonUI.NEON : ueber ? NeonUI.TEXT : NeonUI.GRAU;
			Symbole.zeichne(g, symbol, 18, h / 2 - 8, 2, farbe);
			g.setFont(NeonUI.mono(13));
			if (aktiv) NeonUI.schattenText(g, titel, 46, h / 2 + 5, farbe);
			else {
				g.setColor(farbe);
				g.drawString(titel, 46, h / 2 + 5);
			}
			g.dispose();
		}
	}

	/** Minimieren / Schließen. */
	private static final class FensterKnopf extends NeonUI.Knopf {
		private final Runnable aktion;

		FensterKnopf(String text, Runnable aktion) {
			super(text);
			this.aktion = aktion;
			addActionListener(e -> this.aktion.run());
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			Graphics2D g = NeonUI.glatt(graphics);
			boolean schliessen = "✕".equals(getText());
			if (ueber) {
				g.setColor(schliessen ? NeonUI.alpha(NeonUI.PINK, 70) : NeonUI.alpha(Color.WHITE, 20));
				g.fillRoundRect(2, 2, getWidth() - 4, getHeight() - 4, 8, 8);
			}
			g.setFont(NeonUI.titel(13));
			g.setColor(ueber ? NeonUI.TEXT : NeonUI.GRAU);
			NeonUI.zentriert(g, getText(), 0, 0, getWidth(), getHeight());
			g.dispose();
		}
	}
}
