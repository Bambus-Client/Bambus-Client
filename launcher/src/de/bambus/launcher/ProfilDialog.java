package de.bambus.launcher;

import de.bambus.quests.Quests;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Window;
import java.awt.geom.RoundRectangle2D;
import java.io.IOException;

/**
 * Profil-Fenster (Klick oben rechts aufs Profil): Profilbild, Name, Rang, Anmeldung, Statistiken und PIN.
 */
final class ProfilDialog extends JDialog {

	private static final int B = 660, H = 640, RAND = 28;
	private static final Color GOLD = new Color(0xFFC83D);

	private final BambusLauncher l;
	private final Runnable geaendert;
	private final boolean en = Sprache.englisch();
	private final JPanel p;
	private Profil profil;
	private String meldung = "";
	private boolean meldungGut;

	private final JTextField nameFeld = new JTextField();
	private final NeonUI.UmrissKnopf speichern = new NeonUI.UmrissKnopf("", NeonUI.NEON);
	private final NeonUI.UmrissKnopf microsoft = new NeonUI.UmrissKnopf("", NeonUI.CYAN);
	private final NeonUI.UmrissKnopf abmelden = new NeonUI.UmrissKnopf("", NeonUI.PINK);
	private final NeonUI.UmrissKnopf pinSetzen = new NeonUI.UmrissKnopf("", NeonUI.NEON);
	private final NeonUI.UmrissKnopf pinWeg = new NeonUI.UmrissKnopf("", NeonUI.GRAU);

	ProfilDialog(BambusLauncher l, Runnable geaendert) {
		super(l, "Profil", true);
		this.l = l;
		this.geaendert = geaendert;
		setUndecorated(true);
		setSize(B, H);
		setLocationRelativeTo(l);
		try { setShape(new RoundRectangle2D.Double(0, 0, B, H, 22, 22)); } catch (Exception ignoriert) { /* eckig */ }
		p = new JPanel(null) {
			@Override protected void paintComponent(Graphics gr) { zeichne(NeonUI.glatt(gr)); }
		};
		setContentPane(p);

		NeonUI.UmrissKnopf zu = new NeonUI.UmrissKnopf("✕", NeonUI.GRAU);
		zu.setBounds(B - 62, 18, 38, 32);
		zu.addActionListener(e -> dispose());
		p.add(zu);

		nameFeld.setBounds(RAND, 214, 250, 36);
		nameFeld.setBackground(NeonUI.KARTE);
		nameFeld.setForeground(NeonUI.TEXT);
		nameFeld.setCaretColor(NeonUI.NEON);
		nameFeld.setFont(NeonUI.normal(14));
		nameFeld.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(NeonUI.LINIE), BorderFactory.createEmptyBorder(0, 10, 0, 10)));
		nameFeld.setToolTipText(t("Dein Minecraft-Name (3–16 Zeichen)", "Your Minecraft name (3–16 characters)"));
		nameFeld.addActionListener(e -> nameSpeichern());
		p.add(nameFeld);
		speichern.setText(t("SPEICHERN", "SAVE"));
		speichern.setBounds(RAND + 258, 214, 110, 36);
		speichern.addActionListener(e -> nameSpeichern());
		p.add(speichern);
		microsoft.setText(t("MIT MICROSOFT ANMELDEN", "SIGN IN WITH MICROSOFT"));
		microsoft.setBounds(RAND + 378, 214, B - 2 * RAND - 378, 36);
		microsoft.addActionListener(e -> {
			new MicrosoftDialog(l, l.konto, () -> { }).setVisible(true);
			neuLaden();
		});
		p.add(microsoft);
		abmelden.setText(t("ABMELDEN", "SIGN OUT"));
		abmelden.setBounds(RAND, 214, 160, 36);
		abmelden.addActionListener(e -> {
			l.konto.abmelden();
			meldung(t("Abgemeldet.", "Signed out."), true);
			neuLaden();
		});
		p.add(abmelden);

		pinSetzen.setBounds(B - RAND - 304, H - 62, 150, 36);
		pinSetzen.addActionListener(e -> pinFestlegen());
		p.add(pinSetzen);
		pinWeg.setText(t("PIN ENTFERNEN", "REMOVE PIN"));
		pinWeg.setBounds(B - RAND - 146, H - 62, 146, 36);
		pinWeg.addActionListener(e -> pinEntfernen());
		p.add(pinWeg);

		neuLaden();
	}

	private String t(String de, String en) { return this.en ? en : de; }

	private void meldung(String text, boolean gut) {
		meldung = text;
		meldungGut = gut;
		p.repaint();
	}

	/** Profil neu ermitteln, Knöpfe anpassen und das Profilbild nachladen. */
	private void neuLaden() {
		profil = Profil.ermittle(l);
		boolean bestaetigt = profil.bestaetigt();
		boolean ms = profil.quelle == Profil.Quelle.MICROSOFT;
		nameFeld.setVisible(!bestaetigt);
		speichern.setVisible(!bestaetigt);
		abmelden.setVisible(ms);
		microsoft.setVisible(!ms);
		if (!bestaetigt) {
			nameFeld.setText(profil.quelle == Profil.Quelle.EINGETRAGEN ? profil.name : "");
			microsoft.setBounds(RAND + 378, 214, B - 2 * RAND - 378, 36);
		} else {
			microsoft.setBounds(RAND, 214, 260, 36);
		}
		boolean pin = Profil.pinGesetzt();
		pinSetzen.setText(pin ? t("PIN ÄNDERN", "CHANGE PIN") : t("PIN FESTLEGEN", "SET PIN"));
		pinWeg.setVisible(pin);
		l.pruefeEntwickler();
		if (geaendert != null) geaendert.run();
		p.repaint();
		if (profil.kopf == null) {
			final Profil fuer = profil;
			Thread t = new Thread(() -> {
				try {
					fuer.ladeKopf();
				} catch (IOException ex) {
					fuer.kopf = Profil.kopf(SkinLader.standard().bild);
				}
				SwingUtilities.invokeLater(() -> {
					p.repaint();
					if (geaendert != null) geaendert.run();
				});
			}, "Bambus-Profilbild");
			t.setDaemon(true);
			t.start();
		}
	}

	private void nameSpeichern() {
		String n = nameFeld.getText().trim();
		if (!n.isEmpty() && !Profil.gueltigerName(n)) {
			meldung(t("Ein Minecraft-Name hat 3 bis 16 Zeichen: Buchstaben, Ziffern und _.",
					"A Minecraft name has 3 to 16 characters: letters, digits and _."), false);
			return;
		}
		try {
			Profil.setzeName(n);
			meldung(n.isEmpty() ? t("Name entfernt.", "Name removed.") : t("Gespeichert.", "Saved."), true);
		} catch (IOException ex) {
			meldung(t("Speichern ging nicht: ", "Could not save: ") + ex.getMessage(), false);
		}
		neuLaden();
	}

	private void pinFestlegen() {
		if (Profil.pinGesetzt() && !PinAbfrage.pruefe(this, t("Aktuelle PIN eingeben", "Enter your current PIN"), false)) return;
		char[] neu = PinAbfrage.neu(this);
		if (neu == null) return;
		try {
			Profil.setzePin(neu);
			meldung(t("PIN gespeichert. Ab dem nächsten Start fragt der Launcher danach.",
					"PIN saved. The launcher will ask for it from the next start."), true);
		} catch (IOException ex) {
			meldung(ex.getMessage(), false);
		} finally {
			Profil.loesche(neu);
		}
		neuLaden();
	}

	private void pinEntfernen() {
		if (!PinAbfrage.pruefe(this, t("PIN eingeben, um sie zu entfernen", "Enter the PIN to remove it"), false)) return;
		try {
			Profil.entfernePin();
			meldung(t("PIN entfernt.", "PIN removed."), true);
		} catch (IOException ex) {
			meldung(ex.getMessage(), false);
		}
		neuLaden();
	}

	// ================================================================== Zeichnen

	private void zeichne(Graphics2D g) {
		g.setColor(NeonUI.SEITENLEISTE);
		g.fillRect(0, 0, B, H);
		NeonUI.leuchten(g, new RoundRectangle2D.Float(1, 1, B - 3, H - 3, 22, 22), NeonUI.NEON, 2);

		g.setFont(NeonUI.mono(11));
		g.setColor(NeonUI.GRAU);
		g.drawString(t("SPIELERPROFIL", "PLAYER PROFILE"), RAND, 38);

		// Profilbild mit Leuchtrahmen
		int kx = RAND, ky = 60, kg = 104;
		NeonUI.leuchten(g, new RoundRectangle2D.Float(kx - 4, ky - 4, kg + 8, kg + 8, 14, 14),
				profil.admin() ? GOLD : NeonUI.NEON, 2);
		Profil.zeichneKopf(g, profil.kopf, kx, ky, kg);

		int tx = kx + kg + 24;
		g.setFont(NeonUI.titel(28));
		g.setColor(NeonUI.TEXT);
		String name = profil.hatName() ? profil.name : t("Noch kein Name", "No name yet");
		g.drawString(NeonUI.passend(g, name, B - tx - RAND - 50), tx, ky + 34);

		// Abzeichen: Rolle und Spiel-Rang
		int bx = tx;
		if (profil.hatName()) {
			bx = abzeichen(g, profil.rolle(en), bx, ky + 50, profil.admin() ? GOLD : NeonUI.CYAN) + 8;
		}
		abzeichen(g, Sprache.t(l.fortschritt.rangName().toUpperCase()), bx, ky + 50, NeonUI.NEON);

		g.setFont(NeonUI.normal(12));
		g.setColor(profil.bestaetigt() ? NeonUI.NEON : NeonUI.GRAU);
		g.drawString((profil.bestaetigt() ? "✓ " : "? ") + profil.quelleText(en), tx, ky + 98);

		// Anmeldung
		g.setFont(NeonUI.mono(10));
		g.setColor(NeonUI.NEON);
		g.drawString(t("ANMELDUNG", "SIGN-IN"), RAND, 200);
		g.setColor(NeonUI.LINIE);
		g.drawLine(RAND + 80, 196, B - RAND, 196);
		g.setFont(NeonUI.normal(12));
		g.setColor(NeonUI.GRAU);
		String hinweis;
		switch (profil.quelle) {
			case MICROSOFT:
				hinweis = t("Abmelden löscht die Anmeldung nur auf diesem Computer.", "Signing out only removes the sign-in on this computer.");
				break;
			case LAUNCHER:
				hinweis = t("Abmelden geht im offiziellen Minecraft Launcher.", "Sign out in the official Minecraft Launcher.");
				break;
			default:
				hinweis = t("Nur der Name, kein Passwort – bestätigt erst mit Microsoft-Anmeldung.",
						"Just your name, no password – confirmed only with Microsoft sign-in.");
		}
		g.drawString(NeonUI.passend(g, hinweis, B - 2 * RAND), RAND, 268);
		if (!meldung.isEmpty()) {
			g.setColor(meldungGut ? NeonUI.NEON : new Color(0xFF6B8A));
			g.drawString(NeonUI.passend(g, meldung, B - 2 * RAND), RAND, 286);
		}

		// Statistiken
		g.setFont(NeonUI.mono(10));
		g.setColor(NeonUI.NEON);
		g.drawString(t("STATISTIK", "STATISTICS"), RAND, 314);
		g.setColor(NeonUI.LINIE);
		g.drawLine(RAND + 86, 310, B - RAND, 310);
		String[][] kacheln = statistik();
		int spalten = 3, kb = (B - 2 * RAND - (spalten - 1) * 10) / spalten, kh = 50;
		for (int i = 0; i < kacheln.length; i++) {
			int x = RAND + (i % spalten) * (kb + 10), y = 324 + (i / spalten) * (kh + 8);
			RoundRectangle2D form = new RoundRectangle2D.Float(x, y, kb, kh, 10, 10);
			g.setColor(NeonUI.KARTE);
			g.fill(form);
			g.setStroke(new BasicStroke(1f));
			g.setColor(NeonUI.LINIE);
			g.draw(form);
			g.setFont(NeonUI.mono(9));
			g.setColor(NeonUI.GRAU);
			g.drawString(kacheln[i][0], x + 12, y + 18);
			g.setFont(NeonUI.titel(17));
			g.setColor(NeonUI.TEXT);
			g.drawString(NeonUI.passend(g, kacheln[i][1], kb - 24), x + 12, y + 40);
		}

		// PIN
		g.setFont(NeonUI.mono(10));
		g.setColor(NeonUI.NEON);
		g.drawString(t("LAUNCHER-PIN", "LAUNCHER PIN"), RAND, H - 46);
		g.setFont(NeonUI.normal(11));
		g.setColor(NeonUI.GRAU);
		g.drawString(NeonUI.passend(g, Profil.pinGesetzt() ? t("An – wird beim Start abgefragt.", "On – asked at start.")
				: t("Aus – freiwillig, z. B. gegen Geschwister.", "Off – optional, e.g. against siblings."), B - 2 * RAND - 316), RAND, H - 30);
		g.drawString(t("Nicht dein Minecraft-Passwort!", "Not your Minecraft password!"), RAND, H - 15);
	}

	/** Kleines Abzeichen, gibt das rechte Ende zurück. */
	private int abzeichen(Graphics2D g, String text, int x, int y, Color farbe) {
		g.setFont(NeonUI.mono(11));
		int w = g.getFontMetrics().stringWidth(text) + 20;
		RoundRectangle2D form = new RoundRectangle2D.Float(x, y, w, 24, 12, 12);
		g.setColor(NeonUI.alpha(farbe, 40));
		g.fill(form);
		g.setColor(farbe);
		g.setStroke(new BasicStroke(1.2f));
		g.draw(form);
		g.drawString(text, x + 10, y + 16);
		return x + w;
	}

	private String[][] statistik() {
		long[] s = Profil.spielStatistik(l.installer.spielOrdner);
		int minuten = l.client.zahl("spielminuten", 0);
		int erfolge = 0;
		for (String e : l.client.text("erfolge", "").split(",")) if (!e.trim().isEmpty()) erfolge++;
		int kosmetik = 0;
		for (String e : l.client.text("shop", "").split(",")) if (!e.trim().isEmpty()) kosmetik++;
		String zeit = minuten < 60 ? minuten + " min" : (minuten / 60) + " h " + String.format("%02d", minuten % 60) + " min";
		return new String[][] {
				{t("SPIELZEIT", "PLAY TIME"), zeit},
				{t("SITZUNGEN", "SESSIONS"), String.valueOf(s[5])},
				{t("TAGE IN FOLGE", "DAY STREAK"), String.valueOf(l.fortschritt.serie())},
				{"KILLS", String.valueOf(s[0])},
				{t("SPIELER-KILLS", "PLAYER KILLS"), String.valueOf(s[1])},
				{t("TODE", "DEATHS"), String.valueOf(s[2])},
				{"K/D", Profil.kd(s[0], s[2])},
				{t("BESTE COMBO", "BEST COMBO"), String.valueOf(s[3])},
				{t("BLÄTTER", "LEAVES"), l.fortschritt.blaetter() + "  /  " + l.fortschritt.gesammelt() + t(" ges.", " total")},
				{t("ERFOLGE", "ACHIEVEMENTS"), erfolge + " / " + Quests.ERFOLGE.length},
				{t("KOSMETIK GEKAUFT", "COSMETICS BOUGHT"), String.valueOf(kosmetik)},
				{t("NÄCHSTER RANG", "NEXT RANK"), l.fortschritt.bisNaechsterRang() > 0
						? t("noch ", "") + l.fortschritt.bisNaechsterRang() + t(" Blätter", " leaves to go") : t("höchster Rang", "top rank")},
		};
	}

	// ================================================================== PIN-Abfrage

	/** Kleines Fenster für die PIN – beim Start und zum Ändern. */
	static final class PinAbfrage extends JDialog {
		private final JPasswordField feld1 = new JPasswordField();
		private final JPasswordField feld2 = new JPasswordField();
		private final boolean zweimal;
		private final boolean beimStart;
		private final String titel;
		private String fehler = "";
		private boolean ok;
		private int falsch;

		private PinAbfrage(Window eltern, String titel, boolean zweimal, boolean beimStart) {
			super(eltern, "PIN", ModalityType.APPLICATION_MODAL);
			this.titel = titel;
			this.zweimal = zweimal;
			this.beimStart = beimStart;
			boolean en = Sprache.englisch();
			int b = 480, h = zweimal ? 300 : 250;
			setUndecorated(true);
			setSize(b, h);
			setLocationRelativeTo(eltern);
			try { setShape(new RoundRectangle2D.Double(0, 0, b, h, 20, 20)); } catch (Exception ignoriert) { /* eckig */ }
			JPanel q = new JPanel(null) {
				@Override protected void paintComponent(Graphics gr) {
					Graphics2D g = NeonUI.glatt(gr);
					g.setColor(NeonUI.SEITENLEISTE);
					g.fillRect(0, 0, b, h);
					NeonUI.leuchten(g, new RoundRectangle2D.Float(1, 1, b - 3, h - 3, 20, 20), NeonUI.NEON, 2);
					NeonUI.logo(g, 26, 22, 30);
					g.setFont(NeonUI.titel(18));
					g.setColor(NeonUI.TEXT);
					g.drawString(NeonUI.passend(g, PinAbfrage.this.titel, b - 90), 66, 44);
					g.setFont(NeonUI.normal(11));
					g.setColor(NeonUI.GRAU);
					g.drawString(en ? "The launcher PIN – not your Minecraft password." : "Die Launcher-PIN – nicht dein Minecraft-Passwort.", 26, 76);
					if (zweimal) g.drawString(en ? "Repeat:" : "Wiederholen:", 26, 150);
					if (!fehler.isEmpty()) {
						g.setColor(new Color(0xFF6B8A));
						g.drawString(NeonUI.passend(g, fehler, b - 52), 26, h - 72);
					}
					if (PinAbfrage.this.beimStart) {
						g.setColor(NeonUI.alpha(NeonUI.GRAU, 180));
						g.setFont(NeonUI.normal(10));
						g.drawString(NeonUI.passend(g, en ? "Forgot it? Delete the file .bambus-launcher/profil.properties."
								: "Vergessen? Datei .bambus-launcher/profil.properties löschen.", b - 52), 26, h - 56);
					}
				}
			};
			setContentPane(q);
			for (JPasswordField f : new JPasswordField[] {feld1, feld2}) {
				f.setBackground(NeonUI.KARTE);
				f.setForeground(NeonUI.TEXT);
				f.setCaretColor(NeonUI.NEON);
				f.setFont(NeonUI.titel(20));
				f.setBorder(BorderFactory.createCompoundBorder(
						BorderFactory.createLineBorder(NeonUI.LINIE), BorderFactory.createEmptyBorder(0, 12, 0, 12)));
				f.addActionListener(e -> bestaetigen());
			}
			feld1.setBounds(26, 90, b - 52, 40);
			q.add(feld1);
			if (zweimal) {
				feld2.setBounds(26, 158, b - 52, 40);
				q.add(feld2);
			}
			NeonUI.UmrissKnopf okKnopf = new NeonUI.UmrissKnopf("OK", NeonUI.NEON);
			okKnopf.setBounds(b - 26 - 120, h - 48, 120, 34);
			okKnopf.addActionListener(e -> bestaetigen());
			q.add(okKnopf);
			NeonUI.UmrissKnopf nein = new NeonUI.UmrissKnopf(beimStart ? (en ? "QUIT" : "BEENDEN") : (en ? "CANCEL" : "ABBRECHEN"), NeonUI.GRAU);
			nein.setBounds(b - 26 - 250, h - 48, 120, 34);
			nein.addActionListener(e -> dispose());
			q.add(nein);
			getRootPane().setDefaultButton(null);
		}

		private void bestaetigen() {
			boolean en = Sprache.englisch();
			char[] a = feld1.getPassword();
			try {
				if (zweimal) {
					char[] b2 = feld2.getPassword();
					try {
						if (!Profil.gueltigePin(a)) {
							fehler = en ? "Use 4 to 12 digits." : "Bitte 4 bis 12 Ziffern.";
						} else if (!java.util.Arrays.equals(a, b2)) {
							fehler = en ? "The two PINs are different." : "Die beiden PINs sind verschieden.";
						} else {
							ok = true;
							dispose();
							return;
						}
					} finally {
						Profil.loesche(b2);
					}
				} else if (Profil.pruefePin(a)) {
					ok = true;
					dispose();
					return;
				} else {
					falsch++;
					fehler = en ? "Wrong PIN." : "Falsche PIN.";
					feld1.setText("");
					// nach mehreren Fehlversuchen kurz warten lassen
					if (falsch >= 3) {
						feld1.setEnabled(false);
						javax.swing.Timer warte = new javax.swing.Timer(Math.min(30, falsch * 2) * 1000, e -> {
							feld1.setEnabled(true);
							feld1.requestFocusInWindow();
						});
						warte.setRepeats(false);
						warte.start();
						fehler += en ? " Please wait a moment." : " Bitte kurz warten.";
					}
				}
				getContentPane().repaint();
			} finally {
				if (!zweimal || !ok) Profil.loesche(a);
			}
		}

		/** PIN abfragen. true = richtig. */
		static boolean pruefe(Window eltern, String titel, boolean beimStart) {
			PinAbfrage d = new PinAbfrage(eltern, titel, false, beimStart);
			d.setVisible(true);
			return d.ok;
		}

		/** Neue PIN zweimal eingeben lassen. null = abgebrochen. */
		static char[] neu(Window eltern) {
			PinAbfrage d = new PinAbfrage(eltern, Sprache.englisch() ? "Choose a new PIN" : "Neue PIN festlegen", true, false);
			d.setVisible(true);
			return d.ok ? d.feld1.getPassword() : null;
		}
	}
}
