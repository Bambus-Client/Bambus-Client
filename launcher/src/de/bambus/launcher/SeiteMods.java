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
 * PROFILE: Zuerst die Liste aller Profile (sortierbar, als Liste oder Kacheln) – jedes lässt sich direkt
 * spielen. Ein Klick öffnet ein Profil mit seinen installierten Mods; „+ MODS HINZUFÜGEN“ führt zu einer
 * eigenen Seite mit Suche bei Modrinth und CurseForge, mit Mod-Icons. Gespielt wird mit dem aktiven Profil.
 */
final class SeiteMods extends JPanel {

	private static final int X = 40, HB = 340;          // linke Hälfte: installiert
	private static final int RX = 396;                  // rechte Hälfte: entdecken (gleich breit)
	private static final int VB = HB * 2 + 16;          // volle Breite
	private static final int ZEILE = 44;
	/** Suchtreffer: zwei Spalten, Kartenbreite und Zeilenhöhe. */
	private static final int TB = (VB - 10 - 10) / 2, TZ = 66;

	private final BambusLauncher l;
	private final ModVerwaltung mods;
	private final ModIcons icons;
	private final BoostKarte boost;
	/** Suchleiste mit Lupe und Platzhalter, solange nichts eingetippt ist. */
	private final JTextField suchFeld = new JTextField() {
		@Override
		protected void paintComponent(Graphics graphics) {
			super.paintComponent(graphics);
			Graphics2D g = NeonUI.glatt(graphics);
			if (getText().isEmpty()) {
				g.setFont(NeonUI.normal(14));
				g.setColor(NeonUI.alpha(NeonUI.GRAU, 150));
				g.drawString(Sprache.t("Mods suchen – z. B. Sodium, Iris, Zoom …"), getInsets().left, getHeight() / 2 + 5);
			}
			g.dispose();
		}
	};
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

	/** Bauteile der Profil-Liste bzw. des geöffneten Profils – je nach Ansicht sichtbar. */
	private final List<JComponent> listeTeile = new ArrayList<>();
	private final List<JComponent> profilTeile = new ArrayList<>();
	private final List<JComponent> entdeckenTeile = new ArrayList<>();
	private List<JComponent> sammeln;
	/** Ansicht: Profil-Liste, geöffnetes Profil oder „Mods hinzufügen“. */
	private static final int LISTE = 0, PROFIL = 1, ENTDECKEN = 2;
	private int ansicht = LISTE;
	private final Map<String, NeonUI.UmrissKnopf> sortKnoepfe = new HashMap<>();
	private NeonUI.UmrissKnopf ansichtKnopf, aktivierenKnopf;
	private boolean profilOffen;

	SeiteMods(BambusLauncher l) {
		this.l = l;
		this.mods = l.mods;
		this.icons = new ModIcons(l.installer.spielOrdner.resolve("cache").resolve("mod-icons"), this::repaint);
		setOpaque(false);
		setLayout(null);

		// ---- Ansicht 1: Liste aller Profile
		sammeln = listeTeile;
		String[][] sortierungen = {
				{ModVerwaltung.NACH_ZULETZT, "ZULETZT"}, {ModVerwaltung.NACH_NAME, "NAME"},
				{ModVerwaltung.NACH_MODS, "MODS"}, {ModVerwaltung.NACH_NEU, "NEUESTE"},
		};
		for (int i = 0; i < sortierungen.length; i++) {
			final String nach = sortierungen[i][0];
			NeonUI.UmrissKnopf k = knopf(sortierungen[i][1], NeonUI.GRAU, X + 70 + i * 84, 104, 80, 28, () -> {
				mods.setzeProfilSortierung(nach);
				baueProfile();
			});
			sortKnoepfe.put(nach, k);
		}
		ansichtKnopf = knopf("KACHELN", NeonUI.CYAN, X + 70 + 4 * 84 + 10, 104, 96, 28, () -> {
			mods.setzeProfilAnsicht(ModVerwaltung.ALS_LISTE.equals(mods.profilAnsicht()) ? ModVerwaltung.ALS_KACHELN : ModVerwaltung.ALS_LISTE);
			baueProfile();
		});
		ansichtKnopf.setToolTipText("Zwischen Liste und Kacheln wechseln");
		knopf("+ NEUES PROFIL", NeonUI.NEON, X + VB - 150, 104, 150, 28, this::neuesProfil);
		teil(scroll(profilListe, X, 144, VB, 430));

		// ---- Ansicht 2: ein geöffnetes Profil
		sammeln = profilTeile;
		knopf("← PROFILE", NeonUI.CYAN, X, 104, 104, 30, this::zurListe).setToolTipText("Zurück zur Liste aller Profile");
		int kx = X + VB - 5 * 80 - 4 * 4;
		aktivierenKnopf = knopf("AKTIVIEREN", NeonUI.NEON, kx, 104, 80, 30, () -> {
			mods.setzeAktivesProfil(mods.offenesProfil());
			aktualisiere();
		});
		aktivierenKnopf.setToolTipText("Mit diesem Profil spielen");
		knopf("NAME", NeonUI.GRAU, kx + 84, 104, 80, 30, this::profilUmbenennen).setToolTipText("Profil umbenennen");
		knopf("VERSION", NeonUI.CYAN, kx + 168, 104, 80, 30, this::profilVersion)
				.setToolTipText("Minecraft-Version dieses Profils ändern");
		knopf("KOPIE", NeonUI.GRAU, kx + 252, 104, 80, 30, this::profilKopieren).setToolTipText("Profil mit allen Mods kopieren");
		knopf("LÖSCHEN", NeonUI.PINK, kx + 336, 104, 80, 30, this::profilLoeschen).setToolTipText("Profil löschen");

		// ---- installierte Mods (volle Breite), darüber FPS-Boost und MODS HINZUFÜGEN
		boost = new BoostKarte();
		boost.setBounds(X, 194, HB, 56);
		teil(boost);
		NeonUI.UmrissKnopf hinzu = knopf("+  MODS HINZUFÜGEN", NeonUI.NEON, RX, 198, HB, 48, this::zeigeEntdecken);
		hinzu.setFont(NeonUI.titel(16));
		hinzu.setToolTipText("Mods bei Modrinth oder CurseForge suchen – mit Icons");
		teil(scroll(installiert, X, 256, VB, 270));
		knopf("+ MOD-DATEI", NeonUI.PINK, X, 540, 150, 32, this::dateiHinzufuegen);
		knopf("ORDNER ÖFFNEN", NeonUI.GRAU, X + 158, 540, 150, 32, () -> {
			try {
				java.nio.file.Files.createDirectories(l.installer.modsOrdner);
				java.awt.Desktop.getDesktop().open(l.installer.modsOrdner.toFile());
			} catch (Exception ex) {
				JOptionPane.showMessageDialog(l, Sprache.t("Mods-Ordner: " + l.installer.modsOrdner));
			}
		});

		NeonUI.SpielKnopf profilSpielen = new NeonUI.SpielKnopf("SPIELEN");
		profilSpielen.setFont(NeonUI.titel(18));
		profilSpielen.setBounds(X + VB - 214, 528, 222, 56);
		profilSpielen.setToolTipText("Dieses Profil aktivieren und Minecraft starten");
		profilSpielen.addActionListener(e -> spielenMit(mods.offenesProfil()));
		teil(profilSpielen);

		// ---- Ansicht 3: Mods hinzufügen (Modrinth / CurseForge mit Icons und Suche)
		sammeln = entdeckenTeile;
		knopf("← ZUM PROFIL", NeonUI.CYAN, X, 104, 128, 30, () -> zeigeAnsicht(PROFIL)).setToolTipText("Zurück zum Profil");
		quelleModrinth = knopf("MODRINTH", NeonUI.NEON, X + 140, 104, 110, 30, () -> waehleQuelle(ModVerwaltung.MODRINTH));
		quelleCurseforge = knopf("CURSEFORGE", NeonUI.GRAU, X + 254, 104, 110, 30, () -> waehleQuelle(ModVerwaltung.CURSEFORGE));
		schluesselKnopf = knopf("SCHLÜSSEL", NeonUI.GRAU, X + 368, 104, 108, 30, () -> {
			if (frageSchluessel()) suche();
		});
		schluesselKnopf.setToolTipText("CurseForge-API-Schlüssel eintragen oder ändern");
		schluesselKnopf.setVisible(false);
		suchFeld.setBounds(X, 144, VB - 100, 36);
		suchFeld.setBackground(NeonUI.KARTE);
		suchFeld.setForeground(NeonUI.TEXT);
		suchFeld.setCaretColor(NeonUI.NEON);
		suchFeld.setFont(NeonUI.normal(14));
		suchFeld.setToolTipText("Mod-Name eingeben und Enter drücken");
		suchFeld.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(NeonUI.LINIE, 1, true), BorderFactory.createEmptyBorder(4, 14, 4, 10)));
		suchFeld.addActionListener(e -> suche());
		teil(suchFeld);
		knopf("SUCHEN", NeonUI.CYAN, X + VB - 92, 144, 92, 36, this::suche);
		teil(scroll(treffer, X, 210, VB, 368));
		sammeln = null;

		baueProfile();
		zeigeTreffer(new ArrayList<>());
		zeigeAnsicht(false);
	}

	private <T extends JComponent> T teil(T c) {
		add(c);
		if (sammeln != null) sammeln.add(c);
		return c;
	}

	/** Profil-Liste (false) oder geöffnetes Profil (true) zeigen. */
	private void zeigeAnsicht(boolean profil) {
		zeigeAnsicht(profil ? PROFIL : LISTE);
	}

	private void zeigeAnsicht(int neu) {
		ansicht = neu;
		profilOffen = neu != LISTE;
		for (JComponent c : listeTeile) c.setVisible(neu == LISTE);
		for (JComponent c : profilTeile) c.setVisible(neu == PROFIL);
		for (JComponent c : entdeckenTeile) c.setVisible(neu == ENTDECKEN);
		schluesselKnopf.setVisible(neu == ENTDECKEN && ModVerwaltung.CURSEFORGE.equals(quelle));
		aktivierenKnopf.setVisible(neu == PROFIL && !mods.offenesProfil().equals(mods.aktivesProfil()));
		repaint();
	}

	/** Seite „Mods hinzufügen“ des geöffneten Profils. */
	private void zeigeEntdecken() {
		zeigeTreffer(letzteTreffer);   // Häkchen passen zum geöffneten Profil
		zeigeAnsicht(ENTDECKEN);
		if (!geladen) suche();         // beim ersten Öffnen: die beliebtesten Mods
		SwingUtilities.invokeLater(suchFeld::requestFocusInWindow);
	}

	/** Profil aktivieren und Minecraft starten (der Fortschritt erscheint auf der Startseite). */
	private void spielenMit(String id) {
		mods.setzeAktivesProfil(id);
		baueProfile();
		l.zeige("start");
		l.spielen();
	}

	/** Von der Startseite: anderes Profil zum Spielen wählen (ein geöffnetes Profil wird dabei geschlossen). */
	void waehleAktiv(String id) {
		mods.oeffne(null);
		mods.setzeAktivesProfil(id);
		baueProfile();
		zeigeAnsicht(LISTE);
	}

	/** Öffnet ein Profil: erst jetzt lassen sich seine Mods einstellen. */
	void oeffneProfil(String id) {
		mods.oeffne(id);
		baueInstalliert();
		boost.repaint();
		zeigeAnsicht(PROFIL);
	}

	private void zurListe() {
		mods.oeffne(null);
		baueProfile();
		zeigeAnsicht(false);
	}

	private NeonUI.UmrissKnopf knopf(String text, Color farbe, int x, int y, int b, int h, Runnable aktion) {
		NeonUI.UmrissKnopf k = new NeonUI.UmrissKnopf(text, farbe);
		k.setFont(NeonUI.mono(11));
		k.setBounds(x, y, b, h);
		k.addActionListener(e -> aktion.run());
		return teil(k);
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
		String nach = mods.profilSortierung();
		for (Map.Entry<String, NeonUI.UmrissKnopf> e : sortKnoepfe.entrySet()) {
			e.getValue().setzeFarbe(e.getKey().equals(nach) ? NeonUI.NEON : NeonUI.GRAU);
		}
		boolean kacheln = ModVerwaltung.ALS_KACHELN.equals(mods.profilAnsicht());
		ansichtKnopf.setText(kacheln ? "LISTE" : "KACHELN");
		int b = VB - 10, hoehe;
		List<ModVerwaltung.Profil> liste = mods.profileSortiert();
		if (kacheln) {
			int spalten = 3, kb = (b - (spalten - 1) * 10) / spalten, kh = 118;
			for (int i = 0; i < liste.size(); i++) {
				ProfilEintrag k = new ProfilEintrag(liste.get(i), true);
				k.setBounds((i % spalten) * (kb + 10), (i / spalten) * (kh + 10), kb, kh);
				profilListe.add(k);
			}
			hoehe = ((liste.size() + spalten - 1) / spalten) * (kh + 10);
		} else {
			for (int i = 0; i < liste.size(); i++) {
				ProfilEintrag k = new ProfilEintrag(liste.get(i), false);
				k.setBounds(0, i * 66, b, 60);
				profilListe.add(k);
			}
			hoehe = liste.size() * 66;
		}
		profilListe.setPreferredSize(new Dimension(b, Math.max(hoehe, 10)));
		profilListe.revalidate();
		profilListe.repaint();
		repaint();
	}

	/**
	 * Fragt Name und Minecraft-Version ab. Gibt {name, version} zurück oder null bei Abbruch.
	 */
	private String[] frageProfil(String titel, String vorschlagName, String vorschlagVersion) {
		javax.swing.JTextField feld = new javax.swing.JTextField(vorschlagName, 18);
		javax.swing.JComboBox<String> wahl = new javax.swing.JComboBox<>();
		for (int i = Installer.VERSIONEN.length - 1; i >= 0; i--) {   // neueste oben
			String v = Installer.VERSIONEN[i];
			wahl.addItem(v + (Installer.MC_VERSION.equals(v) ? "   " + Sprache.t("(mit Bambus Client)") : ""));
		}
		for (int i = 0; i < wahl.getItemCount(); i++) {
			if (wahl.getItemAt(i).startsWith(vorschlagVersion + " ") || wahl.getItemAt(i).equals(vorschlagVersion)) {
				wahl.setSelectedIndex(i);
			}
		}
		javax.swing.JPanel feldTeil = new javax.swing.JPanel(new java.awt.GridLayout(0, 1, 0, 4));
		feldTeil.add(new javax.swing.JLabel(Sprache.t("Name des Profils:")));
		feldTeil.add(feld);
		feldTeil.add(new javax.swing.JLabel(" "));
		feldTeil.add(new javax.swing.JLabel(Sprache.t("Minecraft-Version:")));
		feldTeil.add(wahl);
		javax.swing.JLabel hinweis = new javax.swing.JLabel();
		feldTeil.add(hinweis);
		Runnable zeigeHinweis = () -> hinweis.setText(gewaehlteVersion(wahl).equals(Installer.MC_VERSION) ? " "
				: Sprache.t("Nur Minecraft + Fabric + deine Mods – ohne Bambus Client."));
		wahl.addActionListener(e -> zeigeHinweis.run());
		zeigeHinweis.run();
		if (JOptionPane.showConfirmDialog(l, feldTeil, Sprache.t(titel),
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
			return null;
		}
		String name = feld.getText() == null ? "" : feld.getText().trim();
		if (name.isEmpty()) return null;
		return new String[]{name, gewaehlteVersion(wahl)};
	}

	private static String gewaehlteVersion(javax.swing.JComboBox<String> wahl) {
		Object o = wahl.getSelectedItem();
		String t = o == null ? Installer.MC_VERSION : o.toString().trim();
		int leer = t.indexOf(' ');
		return leer > 0 ? t.substring(0, leer) : t;
	}

	private String frageName(String titel, String vorschlag) {
		Object eingabe = JOptionPane.showInputDialog(l, Sprache.t("Name des Profils:"), Sprache.t(titel),
				JOptionPane.PLAIN_MESSAGE, null, null, vorschlag);
		String name = eingabe == null ? "" : eingabe.toString().trim();
		return name.isEmpty() ? null : name;
	}

	/** Das geöffnete Profil. */
	private ModVerwaltung.Profil offen() {
		return mods.profil(mods.offenesProfil());
	}

	private void neuesProfil() {
		String[] a = frageProfil("Neues Profil", "Profil " + (mods.profile().size() + 1), Installer.MC_VERSION);
		if (a != null) {
			oeffneProfil(mods.neuesProfil(a[0], null, a[1]));
		}
	}

	private void profilKopieren() {
		ModVerwaltung.Profil p = offen();
		String[] a = frageProfil("Profil kopieren", p.name + " 2", p.version);
		if (a != null) {
			oeffneProfil(mods.neuesProfil(a[0], p.id, a[1]));
		}
	}

	/** Minecraft-Version eines bestehenden Profils ändern. */
	private void profilVersion() {
		ModVerwaltung.Profil p = offen();
		javax.swing.JComboBox<String> wahl = new javax.swing.JComboBox<>();
		for (int i = Installer.VERSIONEN.length - 1; i >= 0; i--) {
			String v = Installer.VERSIONEN[i];
			wahl.addItem(v + (Installer.MC_VERSION.equals(v) ? "   " + Sprache.t("(mit Bambus Client)") : ""));
		}
		for (int i = 0; i < wahl.getItemCount(); i++) {
			if (wahl.getItemAt(i).startsWith(p.version)) wahl.setSelectedIndex(i);
		}
		javax.swing.JPanel teil = new javax.swing.JPanel(new java.awt.GridLayout(0, 1, 0, 4));
		teil.add(new javax.swing.JLabel(Sprache.t("Welche Minecraft-Version soll dieses Profil spielen?")));
		teil.add(wahl);
		teil.add(new javax.swing.JLabel(" "));
		teil.add(new javax.swing.JLabel(Sprache.t("Jede Version hat eigene Mods und eigene Welten.")));
		if (JOptionPane.showConfirmDialog(l, teil, Sprache.t("Minecraft-Version"),
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
			return;
		}
		String neu = gewaehlteVersion(wahl);
		if (neu.equals(p.version)) return;
		mods.setzeVersion(p.id, neu);
		suchStatus = "";
		aktualisiere();
		suche();
	}

	private void profilUmbenennen() {
		ModVerwaltung.Profil p = offen();
		String name = frageName("Profil umbenennen", p.name);
		if (name != null) {
			mods.benenneProfil(p.id, name);
			repaint();
		}
	}

	private void profilLoeschen() {
		ModVerwaltung.Profil p = offen();
		if (mods.profile().size() <= 1) {
			JOptionPane.showMessageDialog(l, Sprache.t("Das letzte Profil kann nicht gelöscht werden."));
			return;
		}
		if (JOptionPane.showConfirmDialog(l, Sprache.t("Profil „" + p.name + "“ löschen?"), Sprache.t("Profil löschen"),
				JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
			mods.loescheProfil(p.id);
			zurListe();
		}
	}

	// ============================================================ Suche

	private List<ModVerwaltung.Projekt> letzteTreffer = new ArrayList<>();

	private void suche() {
		geladen = true;
		icons.nochmalVersuchen();
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
				status = ergebnis.isEmpty() ? "Nichts gefunden für Minecraft " + mods.versionOffen() + "." : "";
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
		int y = 0, spalte = 0;
		for (ModVerwaltung.Projekt p : ergebnis) {
			boolean schon = mods.istExtra(p.slug) || ModVerwaltung.istEmpfohlen(p.slug);
			TrefferZeile z = new TrefferZeile(p, schon);
			z.setBounds(spalte * (TB + 10), y, TB, TZ - 6);
			treffer.add(z);
			if (++spalte == 2) {
				spalte = 0;
				y += TZ;
			}
		}
		if (spalte == 1) y += TZ;
		treffer.setPreferredSize(new Dimension(VB - 10, Math.max(y, 10)));
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
		installiert.setPreferredSize(new Dimension(VB - 10, Math.max(y, 10)));
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
		z.setBounds(0, y, VB - 10, ZEILE - 4);
		installiert.add(z);
		return y + ZEILE;
	}

	private void dateiHinzufuegen() {
		JFileChooser wahl = new JFileChooser();
		wahl.setDialogTitle("Fabric-Mod (.jar) für Minecraft " + mods.versionOffen() + " auswählen");
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
		zeigeAnsicht(ansicht);
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
		if (ansicht == ENTDECKEN) {
			zeichneEntdecken(g);
			g.dispose();
			return;
		}
		if (!profilOffen) {
			g.setFont(NeonUI.titel(30));
			NeonUI.leuchtText(g, "PROFILE", X, 62, NeonUI.TEXT, 1);
			g.setFont(NeonUI.normal(13));
			g.setColor(NeonUI.GRAU);
			g.drawString("Jedes Profil hat eigene Mods und eine eigene Minecraft-Version.", X, 84);
			g.setFont(NeonUI.mono(10));
			g.setColor(NeonUI.GRAU);
			g.drawString("SORTIEREN", X, 122);
			g.dispose();
			return;
		}
		ModVerwaltung.Profil p = offen();
		boolean aktiv = p.id.equals(mods.aktivesProfil());
		g.setFont(NeonUI.titel(30));
		String titel = NeonUI.passend(g, p.name.toUpperCase(java.util.Locale.ROOT), VB - 40);
		NeonUI.leuchtText(g, titel, X, 62, NeonUI.TEXT, 1);
		g.setFont(NeonUI.normal(13));
		g.setColor(aktiv ? NeonUI.NEON : NeonUI.GRAU);
		g.drawString(NeonUI.passend(g, aktiv ? "● Aktives Profil – wird beim nächsten SPIELEN eingerichtet."
				: "○ Nicht aktiv – mit AKTIVIEREN spielst du mit diesem Profil.", VB - 250), X, 84);
		versionsSchild(g, p, X + VB - 230, 50);
		if (aktiv) {
			g.setFont(NeonUI.mono(11));
			int ax = X + VB - 5 * 80 - 4 * 4;
			g.setColor(NeonUI.alpha(NeonUI.NEON, 40));
			g.fillRect(ax, 104, 80, 30);
			g.setColor(NeonUI.NEON);
			NeonUI.zentriert(g, "✓ AKTIV", ax, 104, 80, 30);
		}

		ueberschrift(g, "INSTALLIERTE MODS", X, 184);
		g.dispose();
	}

	/** Kopf der Seite „Mods hinzufügen“. */
	private void zeichneEntdecken(Graphics2D g) {
		ModVerwaltung.Profil p = offen();
		g.setFont(NeonUI.titel(30));
		NeonUI.leuchtText(g, "MODS HINZUFÜGEN", X, 62, NeonUI.TEXT, 1);
		g.setFont(NeonUI.normal(13));
		g.setColor(NeonUI.GRAU);
		g.drawString(kuerze(g, "Für „" + p.name + "“ · Fabric-Mods für Minecraft " + p.version + " · „+“ fügt die Mod hinzu",
				VB - 250), X, 84);
		versionsSchild(g, p, X + VB - 230, 50);
		g.setFont(NeonUI.normal(12));
		g.setColor(NeonUI.GRAU);
		String status = suchStatus.isEmpty() ? (letzteTreffer.isEmpty() ? "" : letzteTreffer.size() + " Mods") : suchStatus;
		g.drawString(kuerze(g, status, VB), X + 2, 200);
	}

	/** Schild mit der Minecraft-Version eines Profils. */
	private static void versionsSchild(Graphics2D g, ModVerwaltung.Profil p, int x, int y) {
		String text = "MINECRAFT " + p.version;
		Color farbe = p.mitBambus() ? NeonUI.NEON : NeonUI.CYAN;
		g.setFont(NeonUI.mono(12));
		int b = g.getFontMetrics().stringWidth(text) + 20;
		g.setColor(NeonUI.alpha(farbe, 30));
		g.fillRect(x, y, b, 24);
		g.setColor(NeonUI.alpha(farbe, 150));
		g.drawRect(x, y, b - 1, 23);
		g.setColor(farbe);
		NeonUI.zentriert(g, text, x, y, b, 24);
		g.setFont(NeonUI.normal(11));
		g.setColor(NeonUI.GRAU);
		String unten = p.mitBambus() ? "mit Bambus Client" : "ohne Bambus Client";
		g.drawString(unten, x + b - g.getFontMetrics().stringWidth(unten), y + 40);
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

	/**
	 * Ein Profil in der Liste (Zeile) oder als Kachel: Anfangsbuchstabe, Name, Mods, zuletzt benutzt.
	 * Klick öffnet das Profil; „AKTIVIEREN“ macht es zum Profil fürs Spielen.
	 */
	private final class ProfilEintrag extends JComponent {
		private final ModVerwaltung.Profil p;
		private final boolean kachel;
		private boolean ueber, ueberKnopf, ueberSpielen;

		ProfilEintrag(ModVerwaltung.Profil p, boolean kachel) {
			this.p = p;
			this.kachel = kachel;
			setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			setToolTipText("Öffnen und Mods einstellen");
			MouseAdapter maus = new MouseAdapter() {
				@Override public void mouseEntered(MouseEvent e) { ueber = true; repaint(); }
				@Override public void mouseExited(MouseEvent e) { ueber = false; ueberKnopf = false; ueberSpielen = false; repaint(); }
				@Override public void mouseMoved(MouseEvent e) {
					boolean k = knopf().contains(e.getPoint()), sp = spielenKnopf().contains(e.getPoint());
					if (k != ueberKnopf || sp != ueberSpielen) { ueberKnopf = k; ueberSpielen = sp; repaint(); }
				}
				@Override public void mouseClicked(MouseEvent e) {
					if (spielenKnopf().contains(e.getPoint())) {
						spielenMit(ProfilEintrag.this.p.id);
					} else if (!istAktiv() && knopf().contains(e.getPoint())) {
						mods.setzeAktivesProfil(ProfilEintrag.this.p.id);
						baueProfile();
					} else {
						oeffneProfil(ProfilEintrag.this.p.id);
					}
				}
			};
			addMouseListener(maus);
			addMouseMotionListener(maus);
		}

		private boolean istAktiv() {
			return p.id.equals(mods.aktivesProfil());
		}

		/** Fläche des AKTIVIEREN-Knopfs bzw. des AKTIV-Abzeichens. */
		private java.awt.Rectangle knopf() {
			int halb = (getWidth() - 24 - 6) / 2;
			return kachel ? new java.awt.Rectangle(12, getHeight() - 36, halb, 26)
					: new java.awt.Rectangle(getWidth() - 236, (getHeight() - 28) / 2, 104, 28);
		}

		/** Fläche des SPIELEN-Knopfs. */
		private java.awt.Rectangle spielenKnopf() {
			int halb = (getWidth() - 24 - 6) / 2;
			return kachel ? new java.awt.Rectangle(12 + halb + 6, getHeight() - 36, halb, 26)
					: new java.awt.Rectangle(getWidth() - 122, (getHeight() - 28) / 2, 108, 28);
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			Graphics2D g = NeonUI.glatt(graphics);
			int b = getWidth(), h = getHeight();
			boolean aktiv = istAktiv();
			RoundRectangle2D form = new RoundRectangle2D.Float(1, 1, b - 3, h - 3, 10, 10);
			g.setColor(ueber ? NeonUI.KARTE_HOVER : NeonUI.KARTE);
			g.fill(form);
			g.setColor(aktiv ? NeonUI.NEON : ueber ? NeonUI.alpha(NeonUI.CYAN, 150) : NeonUI.LINIE);
			g.setStroke(new BasicStroke(1.2f));
			g.draw(form);
			Color farbe = aktiv ? NeonUI.NEON : NeonUI.CYAN;
			String info = p.mods + (p.mods == 1 ? " Mod" : " Mods");
			boolean en = Sprache.englisch();
			String wann = p.zuletzt > 0 ? (en ? "last used " : "zuletzt ") + vor(p.zuletzt) : en ? "not used yet" : "noch nicht benutzt";
			java.awt.Rectangle k = knopf();

			if (kachel) {
				avatar(g, p.name, 12, 12, 36, farbe);
				g.setFont(NeonUI.titel(15));
				g.setColor(NeonUI.TEXT);
				g.drawString(NeonUI.passend(g, p.name, b - 70), 58, 28);
				g.setFont(NeonUI.normal(11));
				g.setColor(NeonUI.GRAU);
				g.drawString(info, 58, 44);
				g.drawString(kuerze(g, wann, b - 70), 12, 66);
				schild(g, p, b - 12, 54);
			} else {
				if (aktiv) {
					g.setColor(NeonUI.NEON);
					g.fillRect(1, 10, 3, h - 21);
				}
				avatar(g, p.name, 14, (h - 36) / 2, 36, farbe);
				g.setFont(NeonUI.titel(15));
				g.setColor(NeonUI.TEXT);
				g.drawString(NeonUI.passend(g, p.name, b - 400), 62, 26);
				g.setFont(NeonUI.normal(11));
				g.setColor(NeonUI.GRAU);
				g.drawString(info + "  ·  " + wann, 62, 44);
				schild(g, p, b - 246, h / 2 - 9);
			}
			// SPIELEN: aktiviert das Profil und startet Minecraft
			java.awt.Rectangle sp = spielenKnopf();
			RoundRectangle2D sf = new RoundRectangle2D.Float(sp.x, sp.y, sp.width, sp.height, 8, 8);
			if (ueberSpielen) {
				g.setPaint(new java.awt.GradientPaint(sp.x, 0, NeonUI.NEON, sp.x + sp.width, 0, NeonUI.CYAN));
				g.fill(sf);
				g.setColor(NeonUI.DUNKEL);
			} else {
				g.setColor(NeonUI.alpha(NeonUI.NEON, 28));
				g.fill(sf);
				g.setColor(NeonUI.alpha(NeonUI.NEON, 170));
				g.draw(sf);
				g.setColor(NeonUI.NEON);
			}
			g.setFont(NeonUI.mono(11));
			NeonUI.zentriert(g, "▶ SPIELEN", sp.x, sp.y, sp.width, sp.height);
			// Aktiv-Abzeichen oder AKTIVIEREN-Knopf
			g.setFont(NeonUI.mono(11));
			if (aktiv) {
				g.setColor(NeonUI.alpha(NeonUI.NEON, 40));
				g.fillRect(k.x, k.y, k.width, k.height);
				g.setColor(NeonUI.NEON);
				NeonUI.zentriert(g, "✓ AKTIV", k.x, k.y, k.width, k.height);
			} else {
				g.setColor(ueberKnopf ? NeonUI.alpha(NeonUI.NEON, 50) : NeonUI.alpha(NeonUI.NEON, 14));
				g.fillRect(k.x, k.y, k.width, k.height);
				g.setColor(NeonUI.alpha(NeonUI.NEON, ueberKnopf ? 255 : 140));
				g.drawRect(k.x, k.y, k.width - 1, k.height - 1);
				NeonUI.zentriert(g, "AKTIVIEREN", k.x, k.y, k.width, k.height);
			}
			g.dispose();
		}
	}

	/** Kleines Schild mit der Minecraft-Version, rechtsbündig ab x. */
	private static void schild(Graphics2D g, ModVerwaltung.Profil p, int x, int y) {
		Color farbe = p.mitBambus() ? NeonUI.NEON : NeonUI.CYAN;
		g.setFont(NeonUI.mono(10));
		int b = g.getFontMetrics().stringWidth(p.version) + 14;
		g.setColor(NeonUI.alpha(farbe, 28));
		g.fillRect(x - b, y, b, 18);
		g.setColor(farbe);
		NeonUI.zentriert(g, p.version, x - b, y, b, 18);
	}

	/** „vor 5 Min.“, „vor 3 Std.“, „vor 2 Tagen“ … */
	private static String vor(long zeit) {
		long s = Math.max(0, (System.currentTimeMillis() - zeit) / 1000);
		boolean en = Sprache.englisch();
		if (s < 60) return en ? "just now" : "gerade eben";
		if (s < 3600) return en ? (s / 60) + " min ago" : "vor " + (s / 60) + " Min.";
		if (s < 86400) return en ? (s / 3600) + " h ago" : "vor " + (s / 3600) + " Std.";
		long tage = s / 86400;
		if (tage == 1) return en ? "yesterday" : "gestern";
		return en ? tage + " days ago" : "vor " + tage + " Tagen";
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
			boolean cf = ModVerwaltung.CURSEFORGE.equals(p.quelle());
			RoundRectangle2D form = new RoundRectangle2D.Float(1, 1, b - 3, h - 3, 12, 12);
			g.setColor(ueber ? NeonUI.KARTE_HOVER : NeonUI.KARTE);
			g.fill(form);
			if (ueber && !drin) {
				NeonUI.leuchten(g, form, NeonUI.alpha(NeonUI.CYAN, 200), 2);
			} else {
				g.setColor(drin ? NeonUI.alpha(NeonUI.NEON, 120) : NeonUI.LINIE);
				g.draw(form);
			}
			int is = 44, ix = 10, iy = (h - is) / 2;
			icon(g, p.slug, p.icon, p.titel, ix, iy, is, cf ? CF_FARBE : NeonUI.CYAN);
			int tx = ix + is + 12, textB = b - tx - 80;
			g.setFont(NeonUI.titel(14));
			g.setColor(NeonUI.TEXT);
			g.drawString(kuerze(g, p.titel, textB), tx, 22);
			g.setFont(NeonUI.normal(11));
			g.setColor(NeonUI.GRAU);
			String[] zeilen = zweiZeilen(g, Sprache.t(p.beschreibung), textB);
			g.drawString(zeilen[0], tx, 38);
			g.drawString(zeilen[1], tx, 52);
			// rechts: Downloads und Knopf
			g.setFont(NeonUI.mono(9));
			String dl = downloads(p.downloads);
			if (!dl.isEmpty()) {
				g.setColor(NeonUI.alpha(NeonUI.GRAU, 200));
				String t = "↓ " + dl;
				g.drawString(t, b - 12 - g.getFontMetrics().stringWidth(t), 20);
			}
			int kb = 54, kh = 24, kx = b - kb - 10, ky = h - kh - 9;
			RoundRectangle2D k = new RoundRectangle2D.Float(kx, ky, kb, kh, 8, 8);
			if (drin) {
				g.setColor(NeonUI.alpha(NeonUI.NEON, 30));
				g.fill(k);
				g.setColor(NeonUI.NEON);
				g.setFont(NeonUI.mono(10));
				NeonUI.zentriert(g, "✓ DRIN", kx, ky, kb, kh);
			} else {
				g.setColor(ueber ? NeonUI.CYAN : NeonUI.alpha(NeonUI.CYAN, 30));
				g.fill(k);
				g.setColor(ueber ? NeonUI.DUNKEL : NeonUI.CYAN);
				g.setFont(NeonUI.mono(11));
				NeonUI.zentriert(g, "+ DAZU", kx, ky, kb, kh);
			}
			g.dispose();
		}
	}

	/** Text auf höchstens zwei Zeilen umbrechen (zweite Zeile mit „…“ gekürzt). */
	private static String[] zweiZeilen(Graphics2D g, String text, int max) {
		java.awt.FontMetrics fm = g.getFontMetrics();
		if (text == null) return new String[]{"", ""};
		if (fm.stringWidth(text) <= max) return new String[]{text, ""};
		int bruch = text.length();
		while (bruch > 0 && fm.stringWidth(text.substring(0, bruch)) > max) bruch--;
		int leer = text.lastIndexOf(' ', bruch);
		if (leer > bruch / 2) bruch = leer;
		String rest = text.substring(bruch).trim();
		while (rest.length() > 1 && fm.stringWidth(rest + "…") > max) rest = rest.substring(0, rest.length() - 1);
		return new String[]{text.substring(0, bruch), rest.equals(text.substring(bruch).trim()) ? rest : rest + "…"};
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
