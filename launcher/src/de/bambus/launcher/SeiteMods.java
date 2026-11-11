package de.bambus.launcher;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * MODS: oben die Profile (wie in der Modrinth-App), darunter zwei Hälften –
 * links die Mods des gewählten Profils, rechts die Suche bei Modrinth und CurseForge. Mit Mod-Icons.
 */
final class SeiteMods extends JPanel {

	private static final int X = 40, HB = 340;          // linke Hälfte: installiert
	private static final int RX = 396;                  // rechte Hälfte: entdecken (gleich breit)
	private static final int PROFIL_B = 150;            // Breite einer Profil-Karte
	private static final int ZEILE = 44;

	private final BambusLauncher l;
	private final ModVerwaltung mods;
	private final ModIcons icons;
	private final BoostKarte boost;
	private final JTextField suchFeld = new JTextField();
	private final JPanel profilListe = liste();
	private final JPanel installiert = liste();
	private final JPanel treffer = liste();
	private final NeonUI.UmrissKnopf quelleModrinth, quelleCurseforge, schluesselKnopf;
	private String quelle = ModVerwaltung.MODRINTH;
	private String suchStatus = "";
	private int suchLauf;                               // nur das Ergebnis der letzten Suche anzeigen
	private boolean geladen;
	/** Icon-Adressen für Mods ohne gespeichertes Icon (empfohlene, ältere Einträge). */
	private final Map<String, String> iconAdressen = new HashMap<>();
	private final Set<String> iconGefragt = new HashSet<>();

	SeiteMods(BambusLauncher l) {
		this.l = l;
		this.mods = l.mods;
		this.icons = new ModIcons(l.installer.spielOrdner.resolve("cache").resolve("mod-icons"), this::repaint);
		setOpaque(false);
		setLayout(null);

		// ---- Profile (oben, nebeneinander)
		JScrollPane profilScroll = scroll(profilListe, X, 114, HB * 2 + 16 - 262, 50);
		profilScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
		profilScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		profilScroll.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 4));
		add(profilScroll);
		int kx = X + HB * 2 + 16 - 254;
		knopf("+ NEU", NeonUI.NEON, kx, 114, 62, 46, this::neuesProfil).setToolTipText("Neues Profil");
		knopf("NAME", NeonUI.GRAU, kx + 64, 114, 62, 46, this::profilUmbenennen).setToolTipText("Profil umbenennen");
		knopf("KOPIE", NeonUI.GRAU, kx + 128, 114, 62, 46, this::profilKopieren).setToolTipText("Profil mit allen Mods kopieren");
		knopf("LÖSCHEN", NeonUI.PINK, kx + 192, 114, 62, 46, this::profilLoeschen).setToolTipText("Profil löschen");

		// ---- linke Hälfte: installiert
		boost = new BoostKarte();
		boost.setBounds(X, 194, HB, 56);
		add(boost);
		add(scroll(installiert, X, 256, HB, 278));
		knopf("+ MOD-DATEI", NeonUI.PINK, X, 542, HB / 2 - 4, 30, this::dateiHinzufuegen);
		knopf("ORDNER ÖFFNEN", NeonUI.GRAU, X + HB / 2 + 4, 542, HB / 2 - 4, 30, () -> {
			try {
				java.nio.file.Files.createDirectories(l.installer.modsOrdner);
				java.awt.Desktop.getDesktop().open(l.installer.modsOrdner.toFile());
			} catch (Exception ex) {
				JOptionPane.showMessageDialog(l, Sprache.t("Mods-Ordner: " + l.installer.modsOrdner));
			}
		});

		// ---- rechte Hälfte: entdecken
		quelleModrinth = knopf("MODRINTH", NeonUI.NEON, RX, 194, 110, 28, () -> waehleQuelle(ModVerwaltung.MODRINTH));
		quelleCurseforge = knopf("CURSEFORGE", NeonUI.GRAU, RX + 114, 194, 110, 28, () -> waehleQuelle(ModVerwaltung.CURSEFORGE));
		schluesselKnopf = knopf("SCHLÜSSEL", NeonUI.GRAU, RX + HB - 108, 194, 108, 28, () -> {
			if (frageSchluessel()) suche();
		});
		schluesselKnopf.setToolTipText("CurseForge-API-Schlüssel eintragen oder ändern");
		schluesselKnopf.setVisible(false);
		suchFeld.setBounds(RX, 228, HB - 92, 32);
		suchFeld.setBackground(NeonUI.KARTE);
		suchFeld.setForeground(NeonUI.TEXT);
		suchFeld.setCaretColor(NeonUI.NEON);
		suchFeld.setFont(NeonUI.normal(13));
		suchFeld.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(NeonUI.LINIE, 1, true), BorderFactory.createEmptyBorder(4, 10, 4, 10)));
		suchFeld.addActionListener(e -> suche());
		add(suchFeld);
		knopf("SUCHEN", NeonUI.CYAN, RX + HB - 86, 228, 86, 32, this::suche);
		add(scroll(treffer, RX, 268, HB, 304));

		addComponentListener(new java.awt.event.ComponentAdapter() {
			@Override public void componentShown(java.awt.event.ComponentEvent e) {
				if (!geladen) suche();   // beim ersten Öffnen: die beliebtesten Mods
			}
		});

		baueProfile();
		baueInstalliert();
		zeigeTreffer(new ArrayList<>());
	}

	private NeonUI.UmrissKnopf knopf(String text, Color farbe, int x, int y, int b, int h, Runnable aktion) {
		NeonUI.UmrissKnopf k = new NeonUI.UmrissKnopf(text, farbe);
		k.setFont(NeonUI.mono(11));
		k.setBounds(x, y, b, h);
		k.addActionListener(e -> aktion.run());
		add(k);
		return k;
	}

	// ============================================================ Quelle

	private void waehleQuelle(String neu) {
		if (ModVerwaltung.CURSEFORGE.equals(neu) && !mods.hatCurseforge() && !frageSchluessel()) {
			return;
		}
		quelle = neu;
		boolean cf = ModVerwaltung.CURSEFORGE.equals(quelle);
		quelleModrinth.setzeFarbe(cf ? NeonUI.GRAU : NeonUI.NEON);
		quelleCurseforge.setzeFarbe(cf ? NeonUI.NEON : NeonUI.GRAU);
		schluesselKnopf.setVisible(cf);
		suche();
	}

	/** CurseForge gibt Mods nur mit einem eigenen (kostenlosen) API-Schlüssel heraus. */
	private boolean frageSchluessel() {
		Object eingabe = JOptionPane.showInputDialog(l, Sprache.t("CurseForge verlangt einen eigenen API-Schlüssel (kostenlos):\n"
						+ "1. Auf console.curseforge.com anmelden\n2. Unter „API keys“ den Schlüssel kopieren\n3. Hier einfügen\n\n"
						+ "Der Schlüssel bleibt nur auf diesem Computer."),
				Sprache.t("CurseForge-Schlüssel"), JOptionPane.PLAIN_MESSAGE, null, null, mods.curseforgeSchluessel());
		if (eingabe == null) {
			return false;
		}
		mods.setzeCurseforgeSchluessel(eingabe.toString());
		return mods.hatCurseforge();
	}

	// ============================================================ Profile

	private void baueProfile() {
		profilListe.removeAll();
		int x = 0;
		for (ModVerwaltung.Profil p : mods.profile()) {
			ProfilKarte k = new ProfilKarte(p);
			k.setBounds(x, 0, PROFIL_B, 46);
			profilListe.add(k);
			x += PROFIL_B + 6;
		}
		profilListe.setPreferredSize(new Dimension(Math.max(x - 6, 10), 46));
		profilListe.revalidate();
		profilListe.repaint();
	}

	private void waehleProfil(String id) {
		mods.setzeAktivesProfil(id);
		aktualisiere();
		zeigeTreffer(letzteTreffer);   // Häkchen passen zum neuen Profil
	}

	private String frageName(String titel, String vorschlag) {
		Object eingabe = JOptionPane.showInputDialog(l, Sprache.t("Name des Profils:"), Sprache.t(titel),
				JOptionPane.PLAIN_MESSAGE, null, null, vorschlag);
		String name = eingabe == null ? "" : eingabe.toString().trim();
		return name.isEmpty() ? null : name;
	}

	private ModVerwaltung.Profil aktiv() {
		for (ModVerwaltung.Profil p : mods.profile()) {
			if (p.id.equals(mods.aktivesProfil())) return p;
		}
		return mods.profile().get(0);
	}

	private void neuesProfil() {
		String name = frageName("Neues Profil", "Profil " + (mods.profile().size() + 1));
		if (name != null) {
			mods.neuesProfil(name, null);
			waehleProfil(mods.aktivesProfil());
		}
	}

	private void profilKopieren() {
		ModVerwaltung.Profil p = aktiv();
		String name = frageName("Profil kopieren", p.name + " 2");
		if (name != null) {
			mods.neuesProfil(name, p.id);
			waehleProfil(mods.aktivesProfil());
		}
	}

	private void profilUmbenennen() {
		ModVerwaltung.Profil p = aktiv();
		String name = frageName("Profil umbenennen", p.name);
		if (name != null) {
			mods.benenneProfil(p.id, name);
			baueProfile();
			repaint();
		}
	}

	private void profilLoeschen() {
		ModVerwaltung.Profil p = aktiv();
		if (mods.profile().size() <= 1) {
			JOptionPane.showMessageDialog(l, Sprache.t("Das letzte Profil kann nicht gelöscht werden."));
			return;
		}
		if (JOptionPane.showConfirmDialog(l, Sprache.t("Profil „" + p.name + "“ löschen?"), Sprache.t("Profil löschen"),
				JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
			mods.loescheProfil(p.id);
			waehleProfil(mods.aktivesProfil());
		}
	}

	// ============================================================ Suche

	private List<ModVerwaltung.Projekt> letzteTreffer = new ArrayList<>();

	private void suche() {
		geladen = true;
		final String text = suchFeld.getText().trim();
		final String wo = quelle;
		final String name = ModVerwaltung.CURSEFORGE.equals(wo) ? "CurseForge" : "Modrinth";
		final int lauf = ++suchLauf;
		suchStatus = "Suche auf " + name + " …";
		zeigeTreffer(new ArrayList<>());
		Thread t = new Thread(() -> {
			List<ModVerwaltung.Projekt> ergebnis = new ArrayList<>();
			String status;
			try {
				ergebnis = mods.suche(text, wo);
				status = ergebnis.isEmpty() ? "Nichts gefunden für Minecraft " + Installer.MC_VERSION + "." : "";
			} catch (Exception ex) {
				boolean schluessel = String.valueOf(ex.getMessage()).contains("(403)") || String.valueOf(ex.getMessage()).contains("(401)");
				status = schluessel ? "CurseForge lehnt den Schlüssel ab – bitte unter SCHLÜSSEL prüfen."
						: "Keine Verbindung zu " + name + ". Internet prüfen.";
			}
			final List<ModVerwaltung.Projekt> fertig = ergebnis;
			final String meldung = status;
			SwingUtilities.invokeLater(() -> {
				if (lauf == suchLauf) {
					suchStatus = meldung;
					zeigeTreffer(fertig);
				}
			});
		}, "Mod-Suche");
		t.setDaemon(true);
		t.start();
	}

	private void zeigeTreffer(List<ModVerwaltung.Projekt> ergebnis) {
		letzteTreffer = ergebnis;
		treffer.removeAll();
		int y = 0;
		for (ModVerwaltung.Projekt p : ergebnis) {
			boolean schon = mods.istExtra(p.slug) || ModVerwaltung.istEmpfohlen(p.slug);
			TrefferZeile z = new TrefferZeile(p, schon);
			z.setBounds(0, y, HB - 10, ZEILE - 4);
			treffer.add(z);
			y += ZEILE;
		}
		treffer.setPreferredSize(new Dimension(HB - 10, Math.max(y, 10)));
		treffer.revalidate();
		treffer.repaint();
		repaint();
	}

	// ============================================================ Installiert

	private void baueInstalliert() {
		installiert.removeAll();
		int y = 0;
		List<String> ohneIcon = new ArrayList<>();
		for (ModVerwaltung.Projekt p : mods.extras()) {
			boolean cf = ModVerwaltung.CURSEFORGE.equals(p.quelle());
			if (p.icon == null) ohneIcon.add(p.slug);
			y = zeile(y, new ModZeile(p.titel, p.beschreibung, cf ? "CURSEFORGE" : "MODRINTH", cf ? CF_FARBE : NeonUI.CYAN,
					mods.istAn(p.slug), p.slug, p.icon, an -> { mods.setzeAn(p.slug, an); baueProfile(); },
					() -> { mods.entferne(p.slug); baueInstalliert(); baueProfile(); }));
		}
		for (final String[] m : ModVerwaltung.EMPFOHLEN) {
			boolean fps = "FPS".equals(m[3]);
			ohneIcon.add(m[0]);
			y = zeile(y, new ModZeile(m[1], m[2], fps ? "FPS" : "MENÜ", fps ? NeonUI.NEON : NeonUI.CYAN, mods.istAn(m[0]),
					m[0], null, an -> { mods.setzeAn(m[0], an); boost.repaint(); baueProfile(); }, null));
		}
		for (ModVerwaltung.EigeneMod m : mods.eigeneMods()) {
			y = zeile(y, new ModZeile(m.name(), m.datei.getFileName().toString(), "DATEI", NeonUI.PINK, m.an, null, null,
					an -> {
						try {
							mods.schalteEigene(m, an);
						} catch (Exception ex) {
							JOptionPane.showMessageDialog(l, Sprache.t("Umschalten fehlgeschlagen: " + ex.getMessage()));
						}
						SwingUtilities.invokeLater(this::baueInstalliert);
					},
					() -> {
						if (JOptionPane.showConfirmDialog(l, Sprache.t("„" + m.name() + "“ löschen?"), Sprache.t("Mod entfernen"),
								JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
							try {
								mods.loescheEigene(m);
							} catch (Exception ex) {
								JOptionPane.showMessageDialog(l, Sprache.t("Löschen fehlgeschlagen: " + ex.getMessage()));
							}
							baueInstalliert();
						}
					}));
		}
		installiert.setPreferredSize(new Dimension(HB - 10, Math.max(y, 10)));
		installiert.revalidate();
		installiert.repaint();
		fehlendeIconsSuchen(ohneIcon);
		repaint();
	}

	/** Für Mods ohne gespeichertes Icon einmal nachfragen (im Hintergrund). */
	private void fehlendeIconsSuchen(List<String> schluessel) {
		List<String> neu = new ArrayList<>();
		for (String k : schluessel) {
			if (iconGefragt.add(k)) neu.add(k);
		}
		if (neu.isEmpty()) return;
		Thread t = new Thread(() -> {
			Map<String, String> gefunden = mods.sucheIcons(neu);
			SwingUtilities.invokeLater(() -> {
				iconAdressen.putAll(gefunden);
				mods.merkeIcons(gefunden);
				repaint();
			});
		}, "Mod-Icons suchen");
		t.setDaemon(true);
		t.start();
	}

	private int zeile(int y, ModZeile z) {
		z.setBounds(0, y, HB - 10, ZEILE - 4);
		installiert.add(z);
		return y + ZEILE;
	}

	private void dateiHinzufuegen() {
		JFileChooser wahl = new JFileChooser();
		wahl.setDialogTitle("Fabric-Mod (.jar) für Minecraft " + Installer.MC_VERSION + " auswählen");
		wahl.setFileFilter(new FileNameExtensionFilter("Minecraft-Mod (*.jar)", "jar"));
		wahl.setMultiSelectionEnabled(true);
		if (wahl.showOpenDialog(l) != JFileChooser.APPROVE_OPTION) {
			return;
		}
		for (File f : wahl.getSelectedFiles()) {
			try {
				mods.kopiereEigene(f.toPath());
			} catch (Exception ex) {
				JOptionPane.showMessageDialog(l, Sprache.t(f.getName() + ": " + ex.getMessage()));
			}
		}
		baueInstalliert();
	}

	void aktualisiere() {
		baueProfile();
		baueInstalliert();
		boost.repaint();
	}

	// ============================================================ Icons

	private static final Color CF_FARBE = new Color(0xFF8A3D);

	/** Mod-Icon zeichnen – solange es lädt (oder bei eigenen .jar-Dateien) den Anfangsbuchstaben. */
	private void icon(Graphics2D g, String schluessel, String adresse, String name, int x, int y, int groesse, Color farbe) {
		String url = adresse != null ? adresse : schluessel == null ? null : iconAdressen.get(schluessel);
		java.awt.image.BufferedImage bild = icons.icon(url);
		if (bild == null) {
			avatar(g, name, x, y, groesse, farbe);
			return;
		}
		Object alt = g.getRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION);
		g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.setColor(NeonUI.KARTE);
		g.fillRect(x, y, groesse, groesse);
		g.drawImage(bild, x, y, groesse, groesse, null);
		if (alt != null) g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, alt);
		g.setColor(NeonUI.alpha(farbe, 110));
		g.drawRect(x, y, groesse - 1, groesse - 1);
	}

	// ============================================================ Zeichnen

	@Override
	protected void paintComponent(Graphics graphics) {
		Graphics2D g = NeonUI.glatt(graphics);
		g.setFont(NeonUI.titel(30));
		NeonUI.leuchtText(g, "MODS", X, 62, NeonUI.TEXT, 1);
		g.setFont(NeonUI.normal(13));
		g.setColor(NeonUI.GRAU);
		g.drawString("Profile mit eigenen Mods · wird beim nächsten SPIELEN eingerichtet.", X, 84);

		ueberschrift(g, "PROFILE", X, 106);
		ueberschrift(g, "INSTALLIERT · " + aktiv().name.toUpperCase(java.util.Locale.ROOT), X, 184);
		ueberschrift(g, "ENTDECKEN", RX, 184);
		// senkrechte Trennlinie zwischen den Hälften
		g.setColor(NeonUI.LINIE);
		g.fillRect(X + HB + 7, 176, 1, 396);

		g.setFont(NeonUI.normal(12));
		g.setColor(NeonUI.GRAU);
		if (!suchStatus.isEmpty()) {
			g.setFont(NeonUI.normal(11));
			g.drawString(kuerze(g, suchStatus, HB - 8), RX + 4, 290);
		}
		g.setFont(NeonUI.mono(10));
		g.setColor(new Color(0x5C6B80));
		g.drawString("Fabric-Mods für " + Installer.MC_VERSION + " · „+“ fügt die Mod zum aktiven Profil hinzu", X, 588);
		g.dispose();
	}

	private static void ueberschrift(Graphics2D g, String text, int x, int y) {
		g.setFont(NeonUI.mono(12));
		g.setColor(NeonUI.NEON);
		g.drawString(text, x, y);
		int tb = g.getFontMetrics().stringWidth(text);
		g.setColor(NeonUI.LINIE);
		g.fillRect(x + tb + 12, y - 4, Math.max(0, Math.min(120, HB - tb - 12)), 1);
	}

	private static JPanel liste() {
		JPanel p = new JPanel(null);
		p.setOpaque(false);
		return p;
	}

	static JScrollPane scroll(JPanel inhalt, int x, int y, int b, int h) {
		JScrollPane sp = new JScrollPane(inhalt, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		sp.setBounds(x, y, b, h);
		sp.setOpaque(false);
		sp.getViewport().setOpaque(false);
		sp.setBorder(BorderFactory.createEmptyBorder());
		sp.getVerticalScrollBar().setUnitIncrement(16);
		sp.getVerticalScrollBar().setPreferredSize(new Dimension(6, 0));
		sp.getVerticalScrollBar().setOpaque(false);
		sp.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
			@Override protected void configureScrollBarColors() { thumbColor = NeonUI.alpha(NeonUI.NEON, 120); trackColor = new Color(0, 0, 0, 0); }
			@Override protected javax.swing.JButton createDecreaseButton(int o) { return leer(); }
			@Override protected javax.swing.JButton createIncreaseButton(int o) { return leer(); }
			private javax.swing.JButton leer() { javax.swing.JButton k = new javax.swing.JButton(); k.setPreferredSize(new Dimension(0, 0)); return k; }
		});
		SanftScroll.an(sp);
		return sp;
	}

	// ============================================================ Bauteile

	/** Ein Profil in der linken Spalte: Anfangsbuchstabe, Name, Anzahl Mods. */
	private final class ProfilKarte extends JComponent {
		private final ModVerwaltung.Profil p;
		private boolean ueber;

		ProfilKarte(ModVerwaltung.Profil p) {
			this.p = p;
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			addMouseListener(new MouseAdapter() {
				@Override public void mouseEntered(MouseEvent e) { ueber = true; repaint(); }
				@Override public void mouseExited(MouseEvent e) { ueber = false; repaint(); }
				@Override public void mouseClicked(MouseEvent e) { waehleProfil(ProfilKarte.this.p.id); }
			});
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			Graphics2D g = NeonUI.glatt(graphics);
			int b = getWidth(), h = getHeight();
			boolean aktiv = p.id.equals(mods.aktivesProfil());
			RoundRectangle2D form = new RoundRectangle2D.Float(1, 1, b - 3, h - 3, 10, 10);
			g.setColor(aktiv ? NeonUI.alpha(NeonUI.NEON, 26) : ueber ? NeonUI.KARTE_HOVER : NeonUI.KARTE);
			g.fill(form);
			g.setColor(aktiv ? NeonUI.NEON : NeonUI.LINIE);
			g.setStroke(new BasicStroke(1.2f));
			g.draw(form);
			if (aktiv) {
				g.fillRect(8, h - 4, b - 17, 2);
			}
			avatar(g, p.name, 10, (h - 26) / 2, 26, aktiv ? NeonUI.NEON : NeonUI.GRAU);
			g.setFont(NeonUI.titel(13));
			g.setColor(aktiv ? NeonUI.TEXT : new Color(0x9AA8BC));
			g.drawString(NeonUI.passend(g, p.name, b - 52), 44, 20);
			g.setFont(NeonUI.normal(11));
			g.setColor(aktiv ? NeonUI.NEON : NeonUI.GRAU);
			g.drawString(p.mods + (p.mods == 1 ? " Mod" : " Mods") + (aktiv ? " · AKTIV" : ""), 44, 35);
			g.dispose();
		}
	}

	/** Quadrat mit dem Anfangsbuchstaben – ersetzt ein Symbolbild. */
	private static void avatar(Graphics2D g, String name, int x, int y, int groesse, Color farbe) {
		g.setColor(NeonUI.alpha(farbe, 40));
		g.fillRect(x, y, groesse, groesse);
		g.setColor(NeonUI.alpha(farbe, 150));
		g.drawRect(x, y, groesse - 1, groesse - 1);
		g.setFont(NeonUI.titel(15));
		g.setColor(farbe);
		String zeichen = name == null || name.trim().isEmpty() ? "?" : name.trim().substring(0, 1).toUpperCase(java.util.Locale.ROOT);
		NeonUI.zentriert(g, zeichen, x, y, groesse, groesse);
	}

	/** FPS-Boost-Karte: schaltet alle Leistungs-Mods des Profils gemeinsam. */
	private final class BoostKarte extends JComponent {
		private boolean ueber;

		BoostKarte() {
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			addMouseListener(new MouseAdapter() {
				@Override public void mouseEntered(MouseEvent e) { ueber = true; repaint(); }
				@Override public void mouseExited(MouseEvent e) { ueber = false; repaint(); }
				@Override public void mouseClicked(MouseEvent e) {
					mods.setzeFpsBoost(!mods.fpsBoost());
					aktualisiere();
				}
			});
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			Graphics2D g = NeonUI.glatt(graphics);
			boolean an = mods.fpsBoost();
			int b = getWidth(), h = getHeight();
			RoundRectangle2D form = new RoundRectangle2D.Float(3, 3, b - 7, h - 7, 14, 14);
			g.setColor(ueber ? NeonUI.KARTE_HOVER : NeonUI.KARTE);
			g.fill(form);
			if (an) {
				double p = NeonUI.puls();
				NeonUI.leuchten(g, form, NeonUI.alpha(NeonUI.NEON, (int) (150 + 100 * p)), 3);
			} else {
				g.setColor(NeonUI.LINIE);
				g.draw(form);
			}
			g.setFont(NeonUI.titel(20));
			NeonUI.leuchtText(g, "⚡ FPS-BOOST", 16, 27, an ? NeonUI.NEON : NeonUI.GRAU, an ? 1 : 0);
			g.setFont(NeonUI.normal(10));
			g.setColor(NeonUI.GRAU);
			g.drawString(an ? "An · bis zu doppelte FPS, weniger Ruckler" : "Aus · Klicken für mehr FPS", 16, 44);
			int sb = 54, sh = 26, sx = b - sb - 20, sy = (h - sh) / 2;
			RoundRectangle2D bahn = new RoundRectangle2D.Float(sx, sy, sb, sh, sh, sh);
			g.setColor(an ? NeonUI.alpha(NeonUI.NEON, 70) : new Color(0x1E2A3C));
			g.fill(bahn);
			g.setColor(an ? NeonUI.NEON : NeonUI.GRAU);
			g.fillRect(an ? sx + sb - sh + 4 : sx + 4, sy + 4, sh - 8, sh - 8);
			g.dispose();
		}
	}

	/** Eine Mod-Zeile mit Schalter (und optional ✕ zum Entfernen). */
	private final class ModZeile extends JComponent {
		private final String titel, text, marke, schluessel, iconAdresse;
		private final Color farbe;
		private final NeonUI.SchalterKarte.Aenderung aenderung;
		private final Runnable entfernen;
		private boolean an, ueber;

		ModZeile(String titel, String text, String marke, Color farbe, boolean an, String schluessel, String iconAdresse,
				 NeonUI.SchalterKarte.Aenderung aenderung, Runnable entfernen) {
			this.schluessel = schluessel;
			this.iconAdresse = iconAdresse;
			this.titel = titel;
			this.text = text;
			this.marke = marke;
			this.farbe = farbe;
			this.an = an;
			this.aenderung = aenderung;
			this.entfernen = entfernen;
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			setToolTipText(text);
			addMouseListener(new MouseAdapter() {
				@Override public void mouseEntered(MouseEvent e) { ueber = true; repaint(); }
				@Override public void mouseExited(MouseEvent e) { ueber = false; repaint(); }
				@Override public void mouseClicked(MouseEvent e) {
					if (ModZeile.this.entfernen != null && e.getX() > getWidth() - 88 && e.getX() < getWidth() - 62) {
						ModZeile.this.entfernen.run();
						return;
					}
					ModZeile.this.an = !ModZeile.this.an;
					ModZeile.this.aenderung.neu(ModZeile.this.an);
					repaint();
				}
			});
		}

		void setAn(boolean an) {
			this.an = an;
			repaint();
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			Graphics2D g = NeonUI.glatt(graphics);
			int b = getWidth(), h = getHeight();
			RoundRectangle2D form = new RoundRectangle2D.Float(1, 1, b - 3, h - 3, 10, 10);
			g.setColor(ueber ? NeonUI.KARTE_HOVER : NeonUI.KARTE);
			g.fill(form);
			g.setColor(an ? NeonUI.alpha(farbe, 140) : NeonUI.LINIE);
			g.setStroke(new BasicStroke(1.2f));
			g.draw(form);
			if (an) {
				g.setColor(farbe);
				g.fillRoundRect(1, 8, 3, h - 18, 3, 3);
			}
			// Icon, darunter nichts – Titel oben, Herkunft + Beschreibung unten
			icon(g, schluessel, iconAdresse, titel, 10, (h - 28) / 2, 28, an ? farbe : NeonUI.GRAU);
			int tx = 46;
			int textB = b - tx - 64 - (entfernen != null ? 26 : 0);
			g.setFont(NeonUI.titel(13));
			g.setColor(an ? NeonUI.TEXT : new Color(0x9AA8BC));
			g.drawString(kuerze(g, titel, textB), tx, 17);
			g.setFont(NeonUI.mono(9));
			int mb = g.getFontMetrics().stringWidth(Sprache.t(marke)) + 8;
			g.setColor(NeonUI.alpha(farbe, 40));
			g.fillRect(tx, 23, mb, 12);
			g.setColor(farbe);
			g.drawString(marke, tx + 4, 32);
			g.setFont(NeonUI.normal(11));
			g.setColor(NeonUI.GRAU);
			g.drawString(kuerze(g, text, textB - mb - 6), tx + mb + 6, 32);
			if (entfernen != null) {
				g.setFont(NeonUI.mono(12));
				g.setColor(NeonUI.GRAU);
				g.drawString("✕", b - 80, h / 2 + 5);
			}
			// Schalter
			int sb = 40, sh = 20, sx = b - sb - 12, sy = (h - sh) / 2;
			RoundRectangle2D bahn = new RoundRectangle2D.Float(sx, sy, sb, sh, sh, sh);
			g.setColor(an ? NeonUI.alpha(farbe, 70) : new Color(0x1E2A3C));
			g.fill(bahn);
			g.setColor(an ? farbe : NeonUI.GRAU);
			g.fillRect(an ? sx + sb - sh + 3 : sx + 3, sy + 3, sh - 6, sh - 6);
			g.dispose();
		}
	}

	/** Suchtreffer mit Knopf zum Hinzufügen. */
	private final class TrefferZeile extends JComponent {
		private final ModVerwaltung.Projekt p;
		private boolean drin, ueber;

		TrefferZeile(ModVerwaltung.Projekt p, boolean drin) {
			this.p = p;
			this.drin = drin;
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			setToolTipText(p.beschreibung);
			addMouseListener(new MouseAdapter() {
				@Override public void mouseEntered(MouseEvent e) { ueber = true; repaint(); }
				@Override public void mouseExited(MouseEvent e) { ueber = false; repaint(); }
				@Override public void mouseClicked(MouseEvent e) {
					if (!TrefferZeile.this.drin) {
						mods.fuegeHinzu(TrefferZeile.this.p);
						TrefferZeile.this.drin = true;
						baueProfile();
						repaint();
					}
				}
			});
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			Graphics2D g = NeonUI.glatt(graphics);
			int b = getWidth(), h = getHeight();
			RoundRectangle2D form = new RoundRectangle2D.Float(1, 1, b - 3, h - 3, 10, 10);
			g.setColor(ueber ? NeonUI.KARTE_HOVER : NeonUI.KARTE);
			g.fill(form);
			g.setColor(ueber && !drin ? NeonUI.alpha(NeonUI.CYAN, 160) : NeonUI.LINIE);
			g.draw(form);
			int textB = b - 46 - 76;
			icon(g, p.slug, p.icon, p.titel, 10, (h - 28) / 2, 28, ModVerwaltung.CURSEFORGE.equals(p.quelle()) ? CF_FARBE : NeonUI.CYAN);
			g.setFont(NeonUI.titel(13));
			g.setColor(NeonUI.TEXT);
			g.drawString(kuerze(g, p.titel, textB), 46, 17);
			g.setFont(NeonUI.normal(11));
			g.setColor(NeonUI.GRAU);
			g.drawString(kuerze(g, p.beschreibung, textB), 46, 32);
			g.setFont(NeonUI.mono(9));
			g.drawString(downloads(p.downloads), b - 70, h / 2 + 3);
			g.setFont(NeonUI.mono(12));
			g.setColor(drin ? NeonUI.NEON : NeonUI.CYAN);
			g.drawString(drin ? "✓" : "+", b - 24, h / 2 + 5);
			g.dispose();
		}
	}

	private static String downloads(long n) {
		if (n >= 1_000_000) return String.format("%.1f", n / 1e6).replace('.', ',') + " Mio.";
		if (n >= 1_000) return (n / 1000) + " Tsd.";
		return n > 0 ? String.valueOf(n) : "";
	}

	private static String kuerze(Graphics2D g, String s, int max) {
		if (s == null) return "";
		s = Sprache.t(s);   // erst übersetzen, dann kürzen
		if (g.getFontMetrics().stringWidth(s) <= max) return s;
		while (s.length() > 1 && g.getFontMetrics().stringWidth(s + "…") > max) s = s.substring(0, s.length() - 1);
		return s + "…";
	}
}
