package de.bambus.helfer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Wissen des Bambus-Helfers: Fragen zum Client und zu Minecraft (Java Edition 1.21).
 * Läuft komplett offline – keine KI, kein Internet, keine Kosten.
 * Gemeinsam genutzt vom Launcher (Seite HELFER) und vom Spiel (Rechts-Shift → Helfer),
 * darum ohne Abhängigkeiten und Java-8-kompatibel; die Sprache wird jeweils übergeben.
 * Neue Antworten: einfach unten einen Eintrag mit Stichworten ergänzen.
 */
public final class HelferWissen {

	public static final class Eintrag {
		final String[] stichworte;
		public final String de, en;
		public String[] rezept;          // 9 Felder (Minecraft-Id oder "") für ein 3×3-Rezept, siehe Rezepte
		public String rezeptDe, rezeptEn;
		public String ergebnis;          // Minecraft-Id des Ergebnisses
		public int anzahl = 1;           // so viele Stück entstehen
		public Rezepte.Rezept quelle;
		public final List<String[]> aktionen = new ArrayList<>();   // {Text de, Text en, Launcher-Seite}

		Eintrag(String stichworte, String de, String en) {
			this.stichworte = stichworte.split(",");
			for (int i = 0; i < this.stichworte.length; i++) this.stichworte[i] = norm(this.stichworte[i]);
			this.de = de;
			this.en = en;
		}

		Eintrag rezept(Rezepte.Rezept r) {
			this.quelle = r;
			this.rezeptDe = r.de;
			this.rezeptEn = r.en;
			this.rezept = r.felder;
			this.ergebnis = r.id;
			this.anzahl = r.anzahl;
			return this;
		}

		Eintrag aktion(String de, String en, String seite) {
			aktionen.add(new String[]{de, en, seite});
			return this;
		}

		public String text(boolean englisch) { return englisch ? en : de; }
	}

	static final List<Eintrag> WISSEN = new ArrayList<>();

	public static final String[] VORSCHLAEGE_DE = {"Wie bekomme ich mehr FPS?", "HUD verschieben", "Rezept Zaubertisch", "Was ist ein W-Tap?", "Diamanten finden", "Ist Bambus erlaubt?"};
	public static final String[] VORSCHLAEGE_EN = {"How do I get more FPS?", "Move the HUD", "Enchanting table recipe", "What is a W-tap?", "Find diamonds", "Is Bambus allowed?"};

	private HelferWissen() {}

	public static String norm(String s) {
		return s.toLowerCase(Locale.ROOT).replace("ä", "ae").replace("ö", "oe").replace("ü", "ue").replace("ß", "ss")
				.replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
	}

	// ================================================================ Deutsch verstehen

	/** Füllwörter, die nichts über das Thema sagen („wie baue ich eine …“). */
	private static final java.util.Set<String> FUELLWOERTER = new java.util.HashSet<>(java.util.Arrays.asList(
			"wie", "was", "wo", "wann", "warum", "wieso", "welche", "welcher", "welches", "wer", "ich", "du", "man", "mir", "mich",
			"mein", "meine", "meinen", "dir", "dich", "ein", "eine", "einen", "einem", "einer", "eines", "der", "die", "das", "den",
			"dem", "des", "und", "oder", "mit", "ohne", "von", "vom", "aus", "zu", "zum", "zur", "im", "in", "an", "am", "auf",
			"fur", "fuer", "ist", "sind", "bin", "bist", "kann", "kannst", "koennen", "konnen", "soll", "sollte", "muss", "musst",
			"brauche", "brauch", "brauchst", "braucht", "gibt", "gibts", "es", "bitte", "mal", "so", "denn", "auch", "noch",
			"nur", "mach", "mache", "machen", "macht", "bau", "baue", "bauen", "baut", "craft", "crafte", "craften", "crafted",
			"herstellen", "stelle", "stellt", "her", "rezept", "rezepte", "anleitung", "erklaer", "erklaere", "erklaeren",
			"sag", "sage", "zeig", "zeige", "zeigen", "kriege", "krieg", "bekomme", "bekommt", "bekommen", "hab", "habe", "hat",
			"eigentlich", "genau", "gut", "richtig", "einfach", "am", "besten", "minecraft", "mc", "bitte", "danke",
			"how", "do", "does", "i", "you", "a", "an", "the", "to", "of", "for", "make", "can", "what", "is", "are", "my",
			"recipe", "recipes", "get", "please", "in", "with"));

	/** Wörter, die nach einem Rezept fragen. */
	private static final java.util.Set<String> REZEPT_WORTE = new java.util.HashSet<>(java.util.Arrays.asList(
			"rezept", "rezepte", "craften", "crafte", "craft", "crafting", "bauen", "baue", "bau", "herstellen", "stelle",
			"machen", "mache", "recipe", "make", "anleitung"));

	/**
	 * Wortstamm für die Suche: einfache deutsche (und englische) Endungen weg –
	 * „Fackeln“ → „fackel“, „Truhen“ → „truh“, „Bretter“ → „brett“, „Spitzhacken“ → „spitzhack“, „torches“ → „torch“.
	 */
	static String stamm(String w) {
		if (w.length() <= 4) return w;
		String[] endungen = {"ern", "en", "er", "es", "e", "n", "s"};
		for (String e : endungen) {
			if (w.endsWith(e) && w.length() - e.length() >= 4) return w.substring(0, w.length() - e.length());
		}
		return w;
	}

	/** Beste Antwort zur Frage oder null. */
	public static Eintrag finde(String frage) {
		String n = " " + norm(frage) + " ";
		String[] alle = n.trim().split(" ");
		boolean rezeptFrage = false;
		List<String> liste = new ArrayList<>();
		for (String w : alle) {
			if (REZEPT_WORTE.contains(w)) rezeptFrage = true;
			if (!w.isEmpty() && !FUELLWOERTER.contains(w)) liste.add(w);
		}
		if (liste.isEmpty()) {   // nur Füllwörter („wie geht das?“) – dann doch alles werten
			for (String w : alle) if (!w.isEmpty()) liste.add(w);
		}
		String[] woerter = liste.toArray(new String[0]);
		String[] staemme = new String[woerter.length];
		for (int i = 0; i < woerter.length; i++) staemme[i] = stamm(woerter[i]);

		Eintrag beste = null;
		double besteWertung = 0;
		for (Eintrag e : WISSEN) {
			double w = 0;
			double[] proWort = new double[woerter.length];   // jedes Wort der Frage zählt nur einmal
			for (String k : e.stichworte) {
				if (k.isEmpty()) continue;
				if (k.contains(" ")) {
					// Mehrwort-Stichwort: alle Inhaltswörter (auch in anderer Form) müssen in der Frage stehen,
					// Stichwörter nur aus Füllwörtern („wer bist du“) müssen wörtlich vorkommen
					String[] teile = k.split(" ");
					int inhalt = 0;
					boolean alleDa = true;
					for (String t : teile) {
						if (FUELLWOERTER.contains(t)) continue;
						inhalt++;
						boolean da = false;
						for (int i = 0; i < woerter.length && !da; i++) {
							da = woerter[i].equals(t) || staemme[i].equals(stamm(t));
						}
						if (!da) { alleDa = false; break; }
					}
					if (inhalt == 0) {
						if (n.contains(" " + k + " ")) w += 2.5 * teile.length;
					} else if (alleDa) {
						w += 2.5 * inhalt + (n.contains(" " + k + " ") ? 0.5 : 0);
					}
					continue;
				}
				String ks = stamm(k);
				for (int i = 0; i < woerter.length; i++) {
					String wort = woerter[i];
					double p = 0;
					if (wort.equals(k)) p = 2;
					else if (staemme[i].equals(ks)) p = 1.9;                                   // Fackeln ↔ Fackel
					else {
						if (k.length() >= 5 && wort.length() >= 4 && Math.abs(k.length() - wort.length()) <= 1 && abstand(k, wort) <= 1) p = 1.8;   // Tippfehler
						if (k.length() >= 4 && wort.length() >= 4 && (wort.startsWith(k) || k.startsWith(wort))) p = Math.max(p, 1.5);
						if (k.length() >= 5 && wort.length() >= 8 && wort.endsWith(ks)) p = Math.max(p, 1.1);   // Holztreppe → Treppe
					}
					proWort[i] = Math.max(proWort[i], p);
				}
			}
			for (double p : proWort) w += p;
			if (w > 0 && e.rezept != null && rezeptFrage) w += 0.6;   // „Rezept …“ / „wie baue ich …“
			if (w > besteWertung) {
				besteWertung = w;
				beste = e;
			}
		}
		return besteWertung >= 1.8 ? beste : null;
	}

	/**
	 * In welcher Sprache wurde gefragt? Eine deutsche Frage bekommt eine deutsche Antwort, auch wenn der
	 * Launcher auf Englisch steht – und umgekehrt. Bei Unklarheit gilt die eingestellte Sprache.
	 */
	public static boolean englischGefragt(String frage, boolean standard) {
		String roh = frage.toLowerCase(Locale.ROOT);
		int de = 0, en = 0;
		if (roh.matches(".*[äöüß].*")) de += 2;
		String[] deutsch = {"ich", "wie", "was", "wo", "der", "die", "das", "und", "ist", "nicht", "ein", "eine", "einen", "mit",
				"baue", "bauen", "mache", "machen", "rezept", "bekomme", "finde", "gibt", "fuer", "für", "man", "kann", "welche", "warum",
				"brauche", "geht", "craften", "herstellen", "zeig", "erklär", "erklaer", "mein", "meine", "gegen", "beste", "besten"};
		String[] englisch = {"how", "what", "where", "the", "is", "do", "does", "i", "make", "get", "find", "can", "recipe", "you",
				"my", "why", "which", "best", "against", "craft", "show", "explain", "with"};
		for (String w : norm(roh).split(" ")) {
			for (String d : deutsch) if (w.equals(norm(d))) de++;
			for (String e : englisch) if (w.equals(e)) en++;
		}
		if (de > en) return false;
		if (en > de) return true;
		return standard;
	}

	private static int abstand(String a, String b) {
		int[] v = new int[b.length() + 1];
		for (int j = 0; j <= b.length(); j++) v[j] = j;
		for (int i = 1; i <= a.length(); i++) {
			int vorher = v[0];
			v[0] = i;
			for (int j = 1; j <= b.length(); j++) {
				int tmp = v[j];
				v[j] = Math.min(Math.min(v[j] + 1, v[j - 1] + 1), vorher + (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1));
				vorher = tmp;
			}
		}
		return v[b.length()];
	}

	public static String unbekannt(boolean englisch) {
		return englisch
				? "Hmm, I don't know that one yet. I know a lot about the Bambus Client (HUD, FPS, cosmetics, servers, updates) and about Minecraft (recipes, diamonds, Nether, enchanting, PvP). Try one of the suggestions above!"
				: "Hmm, das weiß ich leider noch nicht. Ich kenne mich gut aus mit dem Bambus Client (HUD, FPS, Kosmetik, Server, Updates) und mit Minecraft (Rezepte, Diamanten, Nether, Zaubern, PvP). Probier einen der Vorschläge oben!";
	}

	public static String begruessung(boolean englisch) {
		return englisch
				? "Hi! I'm the Bambus Helper. Ask me anything about the client or Minecraft – for example how to get more FPS, how to craft an enchanting table, or what a W-tap is. I work completely offline."
				: "Hi! Ich bin der Bambus-Helfer. Frag mich alles zum Client oder zu Minecraft – zum Beispiel, wie du mehr FPS bekommst, wie man einen Zaubertisch baut oder was ein W-Tap ist. Ich funktioniere komplett ohne Internet.";
	}

	private static final Pattern ZAHL = Pattern.compile("(-?\\d+)");

	/** Nether-Rechner: „nether 800 -240“ → Koordinaten umrechnen. null, wenn die Frage keine Rechnung ist. */
	public static String netherRechner(String frage, boolean en) {
		String n = frage.toLowerCase(Locale.ROOT);
		if (!n.contains("nether")) return null;
		List<Integer> z = new ArrayList<>();
		Matcher m = ZAHL.matcher(n.replace("−", "-"));
		while (m.find()) {
			try { z.add(Integer.parseInt(m.group(1))); } catch (NumberFormatException ignoriert) { return null; }
		}
		if (z.size() < 2 || z.size() > 3) return null;
		int x = z.get(0), zz = z.get(z.size() - 1);
		boolean zurueck = n.contains("oberwelt") || n.contains("overworld") || n.contains("aus dem nether") || n.contains("from the nether") || n.contains("from nether");
		if (zurueck) {
			return en ? "Nether X " + x + " / Z " + zz + " is in the overworld: X " + (x * 8) + " / Z " + (zz * 8) + ". (Nether × 8)"
					: "Nether X " + x + " / Z " + zz + " ist in der Oberwelt: X " + (x * 8) + " / Z " + (zz * 8) + ". (Nether × 8)";
		}
		int nx = Math.floorDiv(x, 8), nz = Math.floorDiv(zz, 8);
		return en ? "Overworld X " + x + " / Z " + zz + " is in the Nether: X " + nx + " / Z " + nz + ". Build your Nether portal there and it links to your portal in the overworld. (Overworld ÷ 8)"
				: "Oberwelt X " + x + " / Z " + zz + " ist im Nether: X " + nx + " / Z " + nz + ". Baue dein Netherportal dort, dann verbindet es sich mit deinem Portal in der Oberwelt. (Oberwelt ÷ 8)";
	}

	private static Eintrag e(String stichworte, String de, String en) {
		Eintrag x = new Eintrag(stichworte, de, en);
		WISSEN.add(x);
		return x;
	}

	static {
		// ====================================================== Plaudern
		e("hallo,hi,hey,moin,servus,hello,guten tag",
				"Hallo! Wobei kann ich dir helfen? Frag mich etwas zum Bambus Client oder zu Minecraft.",
				"Hello! How can I help? Ask me something about the Bambus Client or Minecraft.");
		e("danke,dankeschoen,thanks,thank you,super,cool",
				"Gern geschehen! Wenn du noch etwas wissen willst, frag einfach.",
				"You're welcome! If you want to know anything else, just ask.");
		e("wer bist du,was bist du,bist du eine ki,who are you,are you an ai,was kannst du,what can you do",
				"Ich bin der Bambus-Helfer, ein Hilfe-Chat im Launcher und im Spiel (RECHTS-SHIFT → HELFER). Ich bin keine echte KI, sondern kenne viele Antworten zum Client und zu Minecraft – dafür funktioniere ich ohne Internet und kostenlos.",
				"I'm the Bambus Helper, a help chat in the launcher and in game (RIGHT SHIFT → HELPER). I'm not a real AI, but I know lots of answers about the client and Minecraft – and I work offline and for free.");

		// ====================================================== Client
		e("installieren,installation,einrichten,starten,spielen,install,setup,start,play,how to play",
				"So geht's: Im Launcher auf SPIELEN klicken – beim ersten Mal wird alles automatisch eingerichtet. Dann öffnet sich der offizielle Minecraft Launcher: dort das Profil „Bambus Client“ wählen und auf Spielen klicken. Du brauchst Minecraft Java Edition.",
				"Here's how: click PLAY in the launcher – the first time, everything is set up automatically. Then the official Minecraft Launcher opens: choose the “Bambus Client” profile there and click Play. You need Minecraft Java Edition.")
				.aktion("Zur Startseite", "Go to home", "start");
		e("minecraft kaufen,brauche ich minecraft,java edition,bedrock,need minecraft,buy minecraft",
				"Zum Spielen brauchst du Minecraft: Java Edition (die PC-Version von minecraft.net). Mit der Bedrock-Version (Handy, Konsole, Windows-Store-„Minecraft für Windows“) funktioniert Bambus nicht.",
				"To play you need Minecraft: Java Edition (the PC version from minecraft.net). Bambus doesn't work with the Bedrock version (phone, console, “Minecraft for Windows”).");
		e("menue,menu,rechts shift,right shift,bambus menue,einstellungen im spiel,oeffnen im spiel,open menu",
				"Im Spiel drückst du RECHTS-SHIFT und wählst dann GARDEROBE (Kosmetik), MOD-MENÜ (Module, Funktionen), HUD (Anzeigen verschieben, vergrößern, mit ✕ ausblenden) oder HELFER. Im Mod-Menü wechselst du mit Tab die Reiter. Hat deine Tastatur kein rechtes Shift (oft beim Mac), kannst du die Taste unter Optionen → Steuerung → Tastenbelegung → „Bambus Client“ ändern.",
				"In game press RIGHT SHIFT and pick WARDROBE (cosmetics), MOD MENU (modules, features), HUD (move, resize, hide with ✕) or HELPER. In the mod menu Tab switches the tabs. If your keyboard has no right Shift (common on Macs), change the key under Options → Controls → Key Binds → “Bambus Client”.");
		e("hud,verschieben,position,anzeige verschieben,groesse,groesser,kleiner,move hud,resize,hud editor",
				"Öffne im Spiel das Bambus-Menü (RECHTS-SHIFT) und klick auf HUD BEARBEITEN. Dann kannst du jede Anzeige mit der Maus ziehen – sie rastet an Kanten ein (mit Shift frei schieben). Größe ändern: Ecke unten rechts ziehen oder Mausrad, Rechtsklick setzt auf 100 %.",
				"Open the Bambus menu in game (RIGHT SHIFT) and click EDIT HUD. Then drag any display with the mouse – it snaps to edges (hold Shift to move freely). Resize: drag the bottom-right corner or use the mouse wheel; right-click resets to 100%.");
		e("modul,module,anschalten,ausschalten,aktivieren,modules,turn on,enable",
				"Module schaltest du im Launcher auf der Seite MOD-MENÜ an und aus – ein Klick auf die Karte genügt. Im Spiel geht das auch im Bambus-Menü (RECHTS-SHIFT). Mit H blendest du das ganze HUD aus und ein.",
				"You turn modules on and off in the launcher on the MOD MENU page – just click the card. In game you can also do it in the Bambus menu (RIGHT SHIFT). Press H to hide or show the whole HUD.")
				.aktion("MOD-MENÜ öffnen", "Open MOD MENU", "module");
		e("fps,ruckelt,ruckeln,lag,laggt,langsam,fps boost,performance,stutter,slow,more fps,mehr fps,fps erhoehen",
				"Mehr FPS: 1) Auf der Seite MODS den FPS-BOOST einschalten (Sodium, Lithium & Co. – oft doppelt so viele FPS). 2) Im Spiel die Sichtweite auf 8–12 Chunks stellen. 3) Bei sehr langsamen PCs den Low-End-Modus einschalten. 4) Andere Programme schließen.",
				"More FPS: 1) Turn on FPS BOOST on the MODS page (Sodium, Lithium & more – often double the FPS). 2) Set render distance to 8–12 chunks in game. 3) On very slow PCs turn on low-end mode. 4) Close other programs.")
				.aktion("MODS öffnen", "Open MODS", "mods");
		e("ram,arbeitsspeicher,speicher,memory,wie viel gb,how much ram",
				"Für die meisten Computer reichen 4–6 GB. Wähle nie mehr als die Hälfte deines echten Arbeitsspeichers, sonst wird der Computer langsamer. Einstellen kannst du das unter EINSTELLUNGEN.",
				"4–6 GB is enough for most computers. Never pick more than half of your real memory, or the computer gets slower. You can change it under SETTINGS.")
				.aktion("EINSTELLUNGEN öffnen", "Open SETTINGS", "einstellungen");
		e("cape,umhang,fluegel,wings,kosmetik,cosmetics,hut,hat,partikel,particles",
				"Kosmetik wählst du im Launcher auf der Seite GARDEROBE: Umhänge, 3D-Flügel, Hüte, Partikel, Kill-Effekte und Pfeil-Spuren. Die Figur kannst du mit der Maus drehen. Im Spiel geht es in der Garderobe: RECHTS-SHIFT → GARDEROBE oder ESC → GARDEROBE. Sehen kannst du sie mit F5 (3. Person).",
				"You pick cosmetics in the launcher on the WARDROBE page: capes, 3D wings, hats, particles, kill effects and arrow trails. Rotate the figure with the mouse. In game use the wardrobe: RIGHT SHIFT → WARDROBE or ESC → WARDROBE. Press F5 (third person) to see them.")
				.aktion("GARDEROBE öffnen", "Open WARDROBE", "kosmetik");
		e("garderobe,wardrobe,kosmetik im spiel,kosmetik ingame,cosmetics in game,umziehen",
				"Die Garderobe öffnest du im Spiel mit RECHTS-SHIFT → GARDEROBE oder im Escape-Menü über die Karte mit deinem Avatar. Links wählst du die Art (Capes, Flügel, Partikel, Hüte, Kill-Effekte, Pfeil-Spuren), rechts das Design – es wird sofort gespeichert.",
				"Open the wardrobe in game with RIGHT SHIFT → WARDROBE or in the pause menu via the card with your avatar. Pick the type on the left (capes, wings, particles, hats, kill effects, arrow trails) and the design on the right – it saves instantly.");
		e("low fire,lowfire,low shield,lowshield,side shield,short sword,kurzes schwert,schwert kleiner,feuer tiefer,schild tiefer,textur,texturen,texture,texturpaket,texture pack,kuerbis overlay,pumpkin overlay,wasser sicht",
				"Das Textur-Modul ist im Spiel eine Karte unter RECHTS-SHIFT → MOD-MENÜ → MODULE → „Texturen“; die Einstellungen öffnest du mit ⋮ oder Rechtsklick: Low Fire (Feuer tiefer), Low Shield (Schild tiefer), Side Shield (Schild zur Seite), Short Sword (Schwert kürzer), kein Kürbis-Overlay und klare Sicht unter Wasser. Wie tief Feuer und Schild rutschen und wie groß das Schwert ist, stellst du mit ‹ › ein. An- und ausschalten kannst du sie auch im Launcher unten auf der Seite MOD-MENÜ.",
				"The texture module is a card in game under RIGHT SHIFT → MOD MENU → MODULES → “Textures”; open its settings with ⋮ or right-click: Low Fire, Low Shield, Side Shield, Short Sword, no pumpkin overlay and clear underwater view. Use ‹ › to set how low fire and shield go and how big the sword is. You can also turn them on and off in the launcher at the bottom of the MOD MENU page.");
		e("helfer ausschalten,helfer deaktivieren,helfer weg,disable helper,turn off helper,hide helper",
				"Im Spiel kannst du mich ausblenden: RECHTS-SHIFT → MOD-MENÜ → MEHR → EXTRAS → „Bambus-Helfer“ ausschalten. Dann zeigt RECHTS-SHIFT nur noch Garderobe und Mod-Menü. Im Launcher bleibe ich auf der Seite HELFER.",
				"You can hide me in game: RIGHT SHIFT → MOD MENU → MORE → EXTRAS → turn off “Bambus Helper”. Then RIGHT SHIFT only shows wardrobe and mod menu. In the launcher I stay on the HELPER page.");
		e("sehen andere,andere spieler,others see,can others see,sichtbar fuer andere",
				"Die Bambus-Kosmetik siehst nur du selbst. Andere Spieler sehen sie nicht – dafür bräuchte Bambus einen eigenen Server.",
				"Only you can see Bambus cosmetics. Other players can't see them – that would need a Bambus server.");
		e("blockbench,fluegel stil,hd fluegel,eckige fluegel,wing style,blocky wings",
				"Flügel gibt es in zwei Stilen: HD (weich, biegt sich) und Blockbench (eckig mit groben Pixeln, wie ein Minecraft-Modell). Umschalten: im Launcher auf der Seite GARDEROBE bei FLÜGEL mit dem Knopf „STIL“, im Spiel in der Garderobe oben rechts bei den Flügeln.",
				"Wings come in two styles: HD (smooth, bends) and Blockbench (blocky with chunky pixels, like a Minecraft model). Switch it in the launcher on the WARDROBE page under WINGS with the “STYLE” button, or in game in the wardrobe at the top right of the wings.")
				.aktion("GARDEROBE öffnen", "Open WARDROBE", "kosmetik");
		e("eigenes cape,eigene fluegel,studio,selbst machen,eigenes bild,custom cape,own cape,make my own",
				"Im Kosmetik-Studio baust du eigene Capes und Flügel: im Launcher auf der Seite GARDEROBE bei Capes oder Flügel auf „+ Erstellen“ klicken. Du kannst Vorlagen, Farben und Initialen wählen oder ein eigenes Bild nehmen.",
				"In the cosmetics studio you can build your own capes and wings: in the launcher on the WARDROBE page click “+ Create” under capes or wings. Pick templates, colors and initials, or use your own image.")
				.aktion("GARDEROBE öffnen", "Open WARDROBE", "kosmetik");
		e("skin,skin laden,mein skin,load skin,change skin,skin aendern",
				"Auf der Seite KOSMETIK deinen Minecraft-Namen eingeben und LADEN klicken – dann siehst du deinen Skin in der 3D-Vorschau. Deinen echten Skin im Spiel änderst du auf minecraft.net in deinem Profil.",
				"On the COSMETICS page enter your Minecraft name and click LOAD – then you see your skin in the 3D preview. To change your real in-game skin, go to your profile on minecraft.net.")
				.aktion("GARDEROBE öffnen", "Open WARDROBE", "kosmetik");
		e("zoom,zoomen,ranzoomen,optifine zoom",
				"Zoom: C gedrückt halten, mit dem Mausrad zoomst du weiter rein oder raus. Die Taste kannst du unter Tastenbelegung ändern.",
				"Zoom: hold C, and use the mouse wheel to zoom further in or out. You can change the key under key binds.");
		e("freelook,umschauen,perspektive,look around,alt",
				"Freelook: Linke Alt-Taste (Mac: Option) halten und mit der Maus umschauen, ohne die Laufrichtung zu ändern. Achtung: Auf manchen Servern ist Freelook nicht erlaubt.",
				"Freelook: hold left Alt (Mac: Option) and look around with the mouse without changing your direction. Note: some servers don't allow freelook.");
		e("toggle sprint,sprinten,immer sprinten,toggle sneak,schleichen,sprint",
				"Toggle Sprint und Toggle Sneak schaltest du im Launcher auf der Seite MOD-MENÜ bei den Funktionen an. Dann reicht ein Druck auf die Sprint- bzw. Schleich-Taste, und es bleibt an.",
				"Turn on toggle sprint and toggle sneak in the launcher on the MOD MENU page under features. Then one press of the sprint or sneak key keeps it on.")
				.aktion("MOD-MENÜ öffnen", "Open MOD MENU", "module");
		e("koordinaten,koordinaten kopieren,coords,coordinates,copy coordinates,nether koordinaten",
				"Schalte das Modul „Koordinaten“ an – es zeigt auch die passenden Nether-Koordinaten (Oberwelt ÷ 8). Zum Kopieren legst du im Spiel unter Tastenbelegung → „Bambus Client“ eine Taste für „Koordinaten kopieren“ fest.",
				"Turn on the “Coordinates” module – it also shows the matching Nether coordinates (overworld ÷ 8). To copy them, set a key for “Copy coordinates” under Key Binds → “Bambus Client” in game.");
		e("server,server hinzufuegen,lieblingsserver,multiplayer,mehrspieler,add server",
				"Auf der Seite SERVER trägst du deine Lieblingsserver ein und siehst live, wie viele Spieler online sind und wie gut der Ping ist. Im Spiel stehen sie dann automatisch unter Mehrspieler.",
				"On the SERVERS page add your favorite servers and see live how many players are online and how good the ping is. In game they automatically appear under Multiplayer.")
				.aktion("SERVER öffnen", "Open SERVERS", "server");
		e("update,updates,aktualisieren,neue version,new version",
				"Neue Versionen kommen automatisch: Der Launcher sucht beim Start nach Updates. Neue Funktionen im Spiel werden still geladen, für einen neuen Launcher erscheint oben „▲ UPDATE“. Unter EINSTELLUNGEN kannst du auch selbst prüfen.",
				"New versions arrive automatically: the launcher checks for updates at start. New in-game features are downloaded quietly; for a new launcher, “▲ UPDATE” appears at the top. You can also check under SETTINGS.")
				.aktion("NEUIGKEITEN öffnen", "Open NEWS", "neu");
		e("sprache,englisch,deutsch,language,english,german",
				"Die Sprache stellst du unter EINSTELLUNGEN oben rechts um: DEUTSCH oder ENGLISH.",
				"Change the language under SETTINGS at the top right: DEUTSCH or ENGLISH.")
				.aktion("EINSTELLUNGEN öffnen", "Open SETTINGS", "einstellungen");
		e("quest,quests,punkte,blaetter,blatt,rang,raenge,shop,belohnung,daily,taeglich,sammeln,freischalten,points,rank,reward",
				"Blätter sind die Punkte von Bambus. Du bekommst sie fürs tägliche Einloggen (je länger die Serie, desto mehr), für die drei Tages-Quests, für Erfolge und fürs Spielen (alle 10 Minuten). Mit Blättern schaltest du im Shop auf der Seite QUESTS besondere Umhänge und Partikel frei. Dazu gibt es jeden Tag drei Herausforderungen im Spiel, zum Beispiel eine Runde gewinnen, einen Leiter-MLG schaffen oder 1000 Blöcke sprinten – die erkennt der Client selbst. Je mehr du insgesamt gesammelt hast, desto höher dein Rang: Bronze, Silber, Gold, Platin, Diamant, Meister, Legende. Im Spiel findest du alles unter RECHTS-SHIFT → QUESTS. Das kostet nie Geld.",
				"Leaves are the points in Bambus. You get them for logging in daily (the longer your streak, the more), for the three daily quests, for achievements and for playing (every 10 minutes). Spend leaves in the shop on the QUESTS page to unlock special capes and particles. There are also three in-game challenges every day – win a round, pull off a ladder MLG, sprint 1000 blocks – and the client spots them by itself. The more you collect in total, the higher your rank: Bronze, Silver, Gold, Platinum, Diamond, Master, Legend. In game you find everything under RIGHT SHIFT → QUESTS. It never costs money.")
				.aktion("QUESTS öffnen", "Open QUESTS", "quests");
		e("version,versionen,minecraft version,1.21,26.1,26.2,26.3,andere version,neue version,alte version,welche version,profil version,snapshot",
				"Jedes Mod-Profil hat seine eigene Minecraft-Version. Beim Anlegen eines Profils wählst du sie aus; später kannst du sie im offenen Profil mit dem Knopf VERSION umstellen. Zur Wahl stehen 1.21.11, 26.1, 26.1.1, 26.1.2, 26.2 und 26.3. Wichtig: Jede Version bekommt einen eigenen Spielordner mit eigenen Mods und eigenen Welten – so baut eine neuere Version deine alten Welten nicht um. Der Bambus Client selbst ist für 1.21.11 gebaut; wählst du eine andere Version, startet das Profil Minecraft mit Fabric und deinen Mods, aber ohne die Bambus-Funktionen. Das Schild auf der Profilkarte zeigt dir die Version: grün heißt mit Bambus, blau heißt ohne.",
				"Every mod profile has its own Minecraft version. You pick it when you create the profile, and you can change it later inside the open profile with the VERSION button. You can choose 1.21.11, 26.1, 26.1.1, 26.1.2, 26.2 and 26.3. Important: every version gets its own game folder with its own mods and its own worlds – that way a newer version will not convert your old worlds. The Bambus Client itself is built for 1.21.11; pick another version and the profile starts Minecraft with Fabric and your mods, but without the Bambus features. The badge on the profile card shows the version: green means with Bambus, blue means without.")
				.aktion("MODS öffnen", "Open MODS", "mods");
		e("startset,start set,geschenk,gratis,kostenlos umsonst,was gehoert mir,preis,preise,kosten,kaufen,gesperrt,schloss,starter",
				"Zum Start gehören dir fünf Stücke geschenkt: der Bambus-Umhang, die Engelsflügel, das Bandana, die Funken-Partikel und die Funken-Spur. Alles andere kostet Blätter – der Preis steht auf jeder Kachel unter KOSMETIK. Klickst du auf ein gesperrtes Stück, fragt der Launcher nach und zieht die Blätter ab. Im Spiel geht das genauso in der Garderobe: einmal klicken fragt nach, zweimal klicken kauft. Was du schon trägst, bleibt natürlich deins.",
				"Five pieces are yours for free from the start: the bamboo cape, the angel wings, the bandana, the spark particles and the spark trail. Everything else costs leaves – the price is on every tile under COSMETICS. Click a locked piece and the launcher asks before spending your leaves. It works the same in game in the wardrobe: one click asks, a second click buys. Anything you already wear stays yours.")
				.aktion("KOSMETIK öffnen", "Open COSMETICS", "kosmetik");
		e("saison,season,set,jahreszeit,fruehling,sommer,herbst,winter,serie,streak,strek,stufe,stufen,schwerer,level",
				"Drei Dinge halten die Quests in Schwung: Das Saison-Set – vier passende Stücke zur Jahreszeit (Umhang, Flügel, Hut, Partikel), zusammen viel günstiger als einzeln, auf der Seite QUESTS. Die Serie – für jeden Tag hintereinander gibt es mehr Bonus-Blätter, aber wer einen ganzen Tag auslässt, verliert die Serie und rutscht einen Rang zurück. Und die Stufen – jede Herausforderung, die du schaffst, wird beim nächsten Mal größer: aus der 5er-Combo wird eine 10er, dann eine 15er, bis Stufe 5. Dafür gibt es auch mehr Blätter.",
				"Three things keep the quests going: The season set – four matching pieces for the season (cape, wings, hat, particles), much cheaper together than one by one, on the QUESTS page. The streak – every day in a row gives more bonus leaves, but miss a whole day and you lose the streak and drop one rank. And the levels – every challenge you finish gets bigger next time: a 5-combo becomes a 10, then a 15, up to level 5. They pay more leaves too.")
				.aktion("QUESTS öffnen", "Open QUESTS", "quests");
		e("hintergrundbild,eigenes bild,wallpaper,background image,own image,eigener hintergrund,foto",
				"Eigenes Hintergrundbild: im Launcher auf EINSTELLUNGEN → HINTERGRUND → EIGENES BILD klicken und ein PNG oder JPG aussuchen. Das Bild gilt auch im Bambus-Hauptmenü im Spiel. Noch einmal auf EIGENES BILD klicken wechselt das Bild.",
				"Your own background image: in the launcher click SETTINGS → BACKGROUND → OWN IMAGE and pick a PNG or JPG. The image is also used in the Bambus main menu in game. Click OWN IMAGE again to pick a different one.")
				.aktion("EINSTELLUNGEN öffnen", "Open SETTINGS", "einstellungen");
		e("hintergrund,background,design,aussehen,theme,farbe,color",
				"Den animierten Hintergrund (Partikel, Synthwave, Aurora, Datenregen, Gitter) wählst du unter EINSTELLUNGEN → HINTERGRUND. Er gilt auch im Bambus-Hauptmenü im Spiel. Die Farbe der Anzeigen im Spiel änderst du im Bambus-Menü unter MEHR.",
				"Choose the animated background (particles, synthwave, aurora, data rain, grid) under SETTINGS → BACKGROUND. It's also used in the Bambus main menu in game. Change the color of the in-game displays in the Bambus menu under MORE.")
				.aktion("EINSTELLUNGEN öffnen", "Open SETTINGS", "einstellungen");
		e("mod hinzufuegen,mods,modrinth,shader,minimap,add mod,install mod,eigene mods",
				"Auf der Seite MODS kannst du bei Modrinth suchen (z. B. „minimap“ oder „shader“) und Mods mit einem Klick hinzufügen. Eigene .jar-Dateien gehen über „+ MOD-DATEI“. Es passen nur Fabric-Mods für Minecraft 1.21.11.",
				"On the MODS page search Modrinth (e.g. “minimap” or “shader”) and add mods with one click. Your own .jar files work via “+ MOD FILE”. Only Fabric mods for Minecraft 1.21.11 fit.")
				.aktion("MODS öffnen", "Open MODS", "mods");
		e("fadenkreuz,crosshair",
				"Das Fadenkreuz änderst du im Spiel im Bambus-Menü (RECHTS-SHIFT) im Reiter FADENKREUZ: Form, Farbe, Größe und Abstand.",
				"Change the crosshair in game in the Bambus menu (RIGHT SHIFT) on the CROSSHAIR tab: shape, color, size and gap.");
		e("profil,profile,speichern,einstellungen teilen,export,import",
				"Im Bambus-Menü unter MEHR kannst du 3 Profile speichern (z. B. PvP, Bedwars, Bauen) und mit einem Klick wechseln. Mit Export/Import teilst du deine Einstellungen mit Freunden.",
				"In the Bambus menu under MORE you can save 3 profiles (e.g. PvP, Bedwars, building) and switch with one click. Export/import lets you share your settings with friends.");
		e("erlaubt,verboten,darf ich,bann,gebannt,banned,allowed,cheat,hack,hacks,is it allowed",
				"Bambus ist ein HUD- und Kosmetik-Client ohne Cheats: keine Wallhacks, kein Autoklicker, keine Reach-Hacks. Freelook ist auf manchen Servern verboten – lies im Zweifel die Regeln des Servers. Kosmetik siehst nur du.",
				"Bambus is a HUD and cosmetics client without cheats: no wallhacks, no autoclicker, no reach hacks. Freelook is forbidden on some servers – check the server rules if unsure. Only you see the cosmetics.");
		e("passwort,sicher,sicherheit,password,safe,account,konto gehackt",
				"Bambus fragt nie nach deinem Passwort. Angemeldet wird nur im offiziellen Minecraft Launcher bzw. auf der Microsoft-Seite. Gib dein Passwort niemals in andere Programme oder Webseiten ein.",
				"Bambus never asks for your password. You only sign in in the official Minecraft Launcher or on the Microsoft page. Never type your password into other programs or websites.");
		e("kostet,geld,kostenlos,gratis,free,cost,money,preis",
				"Der Bambus Client ist kostenlos – es gibt keinen Shop und nichts zu bezahlen. Du brauchst nur Minecraft Java Edition.",
				"The Bambus Client is free – there's no shop and nothing to pay. You only need Minecraft Java Edition.");
		e("screenshot,screenshots,bild gespeichert,where are screenshots",
				"Screenshots machst du im Spiel mit F2. Sie liegen im Bambus-Spielordner im Unterordner „screenshots“. Den Ordner öffnest du unter EINSTELLUNGEN → ORDNER, im Spiel gibt es im Bambus-Menü eine Galerie.",
				"Take screenshots in game with F2. They're in the Bambus game folder in the “screenshots” subfolder. Open the folder under SETTINGS → FOLDERS; in game there's a gallery in the Bambus menu.")
				.aktion("EINSTELLUNGEN öffnen", "Open SETTINGS", "einstellungen");
		e("minecraft nicht gefunden,minecraft fehlt,minecraft not found,ordner nicht gefunden",
				"Installiere zuerst den offiziellen Minecraft Launcher von minecraft.net, melde dich an und starte das Spiel einmal. Danach findet Bambus den Minecraft-Ordner und SPIELEN funktioniert.",
				"First install the official Minecraft Launcher from minecraft.net, sign in and start the game once. After that Bambus finds the Minecraft folder and PLAY works.");
		e("startet nicht,stuerzt ab,absturz,crash,crashed,funktioniert nicht,geht nicht,doesnt work,not working,fehler,error,profil fehlt",
				"Probier das: 1) Minecraft Launcher ganz schließen und in Bambus noch mal SPIELEN klicken. 2) Im Minecraft Launcher das Profil „Bambus Client“ wählen. 3) Unter MODS testweise eigene Mods ausschalten. 4) Arbeitsspeicher auf 4 GB stellen. Hilft nichts, mach ein Foto der Fehlermeldung und schick es dem Ersteller von Bambus.",
				"Try this: 1) Fully close the Minecraft Launcher and click PLAY in Bambus again. 2) Choose the “Bambus Client” profile in the Minecraft Launcher. 3) Temporarily turn off your own mods on the MODS page. 4) Set memory to 4 GB. If nothing helps, take a photo of the error and send it to the Bambus creator.");
		e("discord,discord status,rich presence",
				"Der Discord-Status zeigt deinen Freunden, dass du Bambus spielst. Er braucht eine Discord-App-ID, die der Ersteller von Bambus einträgt – bis dahin bleibt er aus.",
				"The Discord status shows your friends that you're playing Bambus. It needs a Discord app ID entered by the Bambus creator – until then it stays off.");

		// ====================================================== PvP
		e("w tap,wtap,w-tap,s tap,stap,sprint reset,what is a w tap",
				"Ein W-Tap ist ein Sprint-Reset: Kurz vor dem Treffer lässt du W ganz kurz los und drückst es wieder. Dadurch zählt der nächste Schlag wieder als Sprint-Treffer und der Gegner fliegt weiter zurück. Das Modul „W-/S-Tap“ zeigt dir, ob du es richtig machst.",
				"A W-tap is a sprint reset: just before the hit you release W for a moment and press it again. That way your next hit counts as a sprint hit again and knocks your opponent back further. The “W/S tap” module shows whether you're doing it right.");
		e("combo,reach,abstand,trefferserie",
				"Combo = wie viele Treffer du hintereinander landest, ohne selbst getroffen zu werden. Reach = der Abstand beim Treffer (in Minecraft höchstens 3 Blöcke). Beides zeigen dir die Module „Combo-Zähler“ und „Reach-Anzeige“.",
				"Combo = how many hits in a row you land without being hit. Reach = the distance of your hit (max. 3 blocks in Minecraft). The “Combo counter” and “Reach display” modules show both.");
		e("cps,klicken,klickgeschwindigkeit,clicks per second,click faster,jitter,butterfly",
				"CPS heißt Klicks pro Sekunde. In Minecraft 1.9+ zählt beim Schwert vor allem das Timing (Abklingbalken), nicht viele Klicks. Warte, bis der Balken voll ist, dann triffst du am stärksten. Autoklicker sind auf Servern verboten.",
				"CPS means clicks per second. In Minecraft 1.9+ timing (the cooldown bar) matters more than many clicks with a sword. Wait until the bar is full for the strongest hit. Autoclickers are banned on servers.");
		e("bedwars,bed wars,bedwars tipps,bedwars tips",
				"Bedwars-Tipps: 1) Bett sofort schützen (Wolle, dann Endstein oder Holz). 2) Erst Ressourcen sammeln, dann Upgrades kaufen. 3) Immer Blöcke dabeihaben, um nicht runterzufallen. 4) Mit Freunden absprechen, wer verteidigt.",
				"Bedwars tips: 1) Protect your bed right away (wool, then end stone or wood). 2) Collect resources first, then buy upgrades. 3) Always carry blocks so you don't fall. 4) Agree with friends who defends.");

		// ------------------------------------------------------ PvP-Taktiken
		e("pvp,pvp tipps,pvp tips,besser in pvp,besser kaempfen,kaempfen,kampf,fight,fighting,get better at pvp,pvp lernen",
				"Die wichtigsten PvP-Grundlagen: 1) Treffe erst, wenn der Abklingbalken voll ist. 2) Sprint-Reset (W-Tap) für mehr Rückstoß. 3) Kritische Treffer aus dem Fallen. "
						+ "4) Seitlich laufen (Strafen), damit der Gegner dich schwer trifft. 5) Bleib knapp außerhalb seiner Reichweite und geh rein, wenn sein Schlag vorbei ist. "
						+ "Frag mich zu jedem Punkt genauer, z. B. „kritischer Treffer“, „Strafen“, „Schild“, „Angel“, „Jump Reset“.",
				"The most important PvP basics: 1) Only hit when the cooldown bar is full. 2) Sprint reset (W-tap) for more knockback. 3) Critical hits while falling. "
						+ "4) Move sideways (strafing) so you're hard to hit. 5) Stay just outside their reach and go in when their swing is over. "
						+ "Ask me about each point, e.g. “critical hit”, “strafing”, “shield”, “fishing rod”, “jump reset”.");
		e("kritischer treffer,kritisch,krit,crit,critical hit,crits,critical",
				"Kritischer Treffer: Spring und triff den Gegner, während du wieder nach unten fällst – das macht 50 % mehr Schaden (Sterne-Partikel). "
						+ "Achtung: Beim Sprinten ist ein Krit nicht möglich, und der Abklingbalken muss fast voll sein. Trick: kurz Sprint loslassen, springen, im Fallen treffen.",
				"Critical hit: jump and hit while you're falling back down – 50% more damage (star particles). "
						+ "Note: you can't crit while sprinting, and the cooldown bar must be almost full. Trick: let go of sprint, jump, hit while falling.");
		e("abklingzeit,cooldown,abklingbalken,angriffsabklingzeit,attack cooldown,1 9 pvp,1 9,timing,schlagtiming",
				"Seit 1.9 hat jede Waffe eine Abklingzeit: Schwert ca. 0,6 s, Axt ca. 1 s. Schlägst du früher, macht der Treffer viel weniger Schaden. "
						+ "Also nicht spammen, sondern im Rhythmus treffen – der Balken unter dem Fadenkreuz zeigt, wann du wieder voll zuschlagen kannst.",
				"Since 1.9 every weapon has a cooldown: sword about 0.6 s, axe about 1 s. Hitting earlier does much less damage. "
						+ "So don't spam – hit in rhythm. The bar under the crosshair shows when you can hit at full strength again.");
		e("strafen,strafe,strafing,seitlich laufen,kreisen,im kreis laufen,ausweichen,dodge",
				"Strafen heißt: Im Kampf mit A oder D seitlich laufen, während du den Gegner anschaust. So schwenkt sein Fadenkreuz ständig hinterher und er verfehlt öfter. "
						+ "Wechsle die Richtung unregelmäßig und kombiniere es mit W-Taps.",
				"Strafing means moving sideways with A or D while looking at your opponent. Their crosshair has to keep following you, so they miss more. "
						+ "Change direction irregularly and combine it with W-taps.");
		e("jump reset,rueckstoss verringern,weniger rueckstoss,knockback reduzieren,reduce knockback,springen beim treffer",
				"Jump Reset: Spring genau in dem Moment, in dem du getroffen wirst. Dann fliegst du weniger weit zurück und kannst direkt zurückschlagen. "
						+ "Braucht Übung beim Timing – achte auf den Schlag des Gegners.",
				"Jump reset: jump exactly when you get hit. You get knocked back less and can hit back right away. "
						+ "Needs practice – watch your opponent's swing.");
		e("schild blocken,mit schild blocken,schild pvp,shield pvp,blocken,block,shield block,schild benutzen",
				"Schild: Rechtsklick halten blockt alle Treffer von vorne, auch Pfeile. Aber: Eine Axt deaktiviert den Schild für 5 Sekunden. "
						+ "Tipp: Blocke, wenn der Gegner zuschlägt, und lass los, um selbst zu treffen. Mit Side Shield im Textur-Modul siehst du dabei mehr.",
				"Shield: holding right click blocks all hits from the front, even arrows. But an axe disables the shield for 5 seconds. "
						+ "Tip: block when your opponent swings and release to hit back. The Side Shield texture option gives you a better view.");
		e("axt oder schwert,axt vs schwert,axe vs sword,welche waffe,beste waffe,best weapon,axt pvp",
				"Schwert: schneller (öfter treffen, Schwungangriff gegen mehrere). Axt: mehr Schaden pro Schlag, aber langsamer – und sie schaltet Schilde 5 Sekunden aus. "
						+ "Profi-Trick: Gegner blockt → kurz zur Axt wechseln, Schild ausschalten, dann zurück zum Schwert.",
				"Sword: faster (more hits, sweeping attack against groups). Axe: more damage per hit but slower – and it disables shields for 5 seconds. "
						+ "Pro trick: opponent blocks → switch to the axe, disable the shield, then switch back to the sword.");
		e("angel pvp,rod,rod pvp,angelrute pvp,fishing rod pvp,mit angel",
				"Angel im PvP: Triff den Gegner mit dem Haken – er bekommt kurz Rückstoß und du unterbrichst seinen Sprint. Gut, um eine Combo zu starten oder Flüchtende zu stoppen. "
						+ "Danach sofort zum Schwert wechseln.",
				"Fishing rod in PvP: hit your opponent with the hook – a little knockback and their sprint is interrupted. Great for starting a combo or stopping runners. "
						+ "Then switch to your sword right away.");
		e("bogen pvp,bow pvp,mit bogen kaempfen,pfeile pvp,bogen tipps,bow tips,armbrust pvp",
				"Bogen: Voll aufziehen (ca. 1 Sekunde) für den stärksten Schuss, auf Entfernung etwas über den Gegner zielen. Mit „Schlag“ (Punch) wirfst du ihn zurück. "
						+ "Gegen Bogenschützen: im Zickzack laufen oder mit Schild blocken. Die Armbrust lädt langsam, trifft aber stark und kann Feuerwerk schießen.",
				"Bow: draw fully (about 1 second) for the strongest shot, aim a bit above your target at range. Punch knocks them back. "
						+ "Against archers: zigzag or block with a shield. The crossbow loads slowly but hits hard and can shoot fireworks.");
		e("combo halten,combo machen,combos,lange combo,combo lock",
				"Combo halten: Nach jedem Treffer W-Tap, damit der Gegner weit zurückfliegt und nicht zurückschlagen kann. Immer leicht nach oben zielen, "
						+ "nicht zu nah ranlaufen und im Rhythmus des Abklingbalkens schlagen. Der Combo-Zähler im HUD zeigt dir, wie gut es klappt.",
				"Keep a combo: W-tap after every hit so your opponent flies back and can't hit you. Aim slightly up, "
						+ "don't run too close and hit in rhythm with the cooldown bar. The combo counter in the HUD shows how well it works.");
		e("goldener apfel pvp,gapple,heilen pvp,leben auffuellen,heal in pvp,essen im kampf,regeneration",
				"Heilen im Kampf: Goldener Apfel gibt Regeneration und 4 Extra-Herzen (Absorption) – iss ihn, wenn du kurz Abstand hast. "
						+ "Schneller: Wurftrank der Heilung. Zum Sättigen zwischen Kämpfen sind goldene Karotten am besten.",
				"Healing in a fight: a golden apple gives regeneration and 4 extra hearts (absorption) – eat it when you have some distance. "
						+ "Faster: splash potion of healing. Golden carrots are best for filling hunger between fights.");
		e("totem,totem der unsterblichkeit,totem of undying,totem pvp,nebenhand",
				"Totem der Unsterblichkeit: In der Nebenhand (F-Taste) rettet es dich einmal vor dem Tod. Im PvP immer ein neues Totem bereit haben – "
						+ "nach dem Auslösen sofort das nächste in die Nebenhand legen.",
				"Totem of undying: in your offhand (F key) it saves you from death once. In PvP always keep a spare – "
						+ "put the next one in your offhand right after it pops.");
		e("enderperle pvp,perle,pearl,ender pearl pvp,abhauen,fliehen,flucht,escape",
				"Enderperle: Werfen und dich dorthin teleportieren (kostet etwas Leben). Gut zum Fliehen, um auf eine Insel zu kommen oder um hinter den Gegner zu springen. "
						+ "In Bedwars und Skywars rettet eine Perle dich auch nach einem Sturz ins Void – sofort nach unten-vorne werfen.",
				"Ender pearl: throw it to teleport there (costs a little health). Great for escaping, reaching an island or getting behind your opponent. "
						+ "In Bedwars and Skywars a pearl can even save you after falling into the void – throw it forward and down right away.");
		e("clutch,mlg,wassereimer mlg,wasser mlg,fallschaden,sturz,fall damage,water bucket",
				"MLG/Clutch: Kurz vor dem Aufprall einen Wassereimer unter dich setzen (Rechtsklick auf den Boden) – kein Fallschaden. Danach das Wasser wieder aufnehmen. "
						+ "Gegen Rückstoß über dem Void: schnell Blöcke unter dich setzen (nach unten schauen, rechtsklicken, rückwärts laufen).",
				"MLG/clutch: just before landing, place a water bucket under you (right click the ground) – no fall damage. Pick the water back up afterwards. "
						+ "Knocked back over the void: quickly place blocks under you (look down, right click, walk backwards).");
		e("bridgen,bruecke bauen,speedbridge,speed bridge,bridging,godbridge,ninja bridge",
				"Bridgen (Brücke bauen): Schleichen (Shift), rückwärts zur Kante laufen und unter dich Blöcke setzen. Schneller: ohne Shift, nur kurz vor der Kante antippen. "
						+ "Übe zuerst langsam – ein Sturz kostet mehr Zeit als langsames Bauen.",
				"Bridging: sneak (shift), walk backwards to the edge and place blocks under you. Faster: no shift, just tap it right before the edge. "
						+ "Practice slowly first – falling costs more time than building slowly.");
		e("skywars,sky wars,skywars tipps,skywars tips",
				"Skywars-Tipps: 1) Sofort die Truhen deiner Insel plündern und Rüstung anziehen. 2) Zur Mitte bauen – dort ist die beste Ausrüstung. "
						+ "3) Schneebälle/Eier und Angel werfen Gegner ins Void. 4) Immer Blöcke und eine Enderperle dabeihaben.",
				"Skywars tips: 1) Loot your island's chests right away and put on armor. 2) Bridge to the middle – that's where the best gear is. "
						+ "3) Snowballs/eggs and the fishing rod knock opponents into the void. 4) Always carry blocks and an ender pearl.");
		e("kristall pvp,crystal pvp,endkristall pvp,crystal,anker pvp,anchor pvp,seelenanker pvp",
				"Crystal-PvP: Endkristalle auf Obsidian setzen und sofort zerschlagen – die Explosion macht riesigen Schaden. Seelenanker (geladen mit Glowstone) explodieren in der Oberwelt genauso. "
						+ "Schutz: Rüstung mit Explosionsschutz, Totem in der Nebenhand. Auf vielen Servern ist das nur in eigenen Modi erlaubt.",
				"Crystal PvP: place end crystals on obsidian and break them right away – the explosion deals huge damage. Respawn anchors (charged with glowstone) explode in the overworld too. "
						+ "Protection: blast protection armor, totem in your offhand. Many servers only allow it in special modes.");
		e("streitkolben pvp,mace pvp,windkugel pvp,wind charge pvp,mace,smash",
				"Streitkolben (Mace): Je tiefer du vor dem Treffer fällst, desto mehr Schaden – und dein Fallschaden fällt weg. Mit Windkugeln (vor die Füße werfen) "
						+ "schießt du dich hoch und landest von oben einen Riesentreffer. Verzauberungen: Dichte (mehr Fall-Schaden), Bresche (durch Rüstung), Windstoß.",
				"Mace: the further you fall before hitting, the more damage – and your fall damage is cancelled. Wind charges (thrown at your feet) "
						+ "launch you up for a huge hit from above. Enchantments: Density (more fall damage), Breach (through armor), Wind Burst.");
		e("pvp verzauberungen,beste verzauberungen pvp,pvp enchantments,best enchantments,schaerfe,schutz,sharpness,protection",
				"Gute PvP-Verzauberungen: Schwert – Schärfe V, Verbrennung II, Plünderung ist egal. Rüstung – Schutz IV, Haltbarkeit III. Bogen – Stärke V, Schlag II, Flamme. "
						+ "Axt – Schärfe V. Für alles: Reparatur, damit die Ausrüstung mit Erfahrung heil bleibt.",
				"Good PvP enchantments: sword – Sharpness V, Fire Aspect II. Armor – Protection IV, Unbreaking III. Bow – Power V, Punch II, Flame. "
						+ "Axe – Sharpness V. For everything: Mending, so XP keeps your gear repaired.");
		e("pvp traenke,staerketrank,strength potion,speed potion,schnelligkeit,traenke im kampf,splash potion,wurftrank",
				"Tränke im PvP: Stärke (mehr Schaden), Schnelligkeit (schneller), Feuerresistenz (gegen Lava und Verbrennung), Wurftrank der Heilung (sofort Leben). "
						+ "Wurftränke wirfst du am besten direkt vor deine Füße.",
				"Potions in PvP: Strength (more damage), Speed, Fire Resistance (against lava and fire aspect), splash potion of healing (instant health). "
						+ "Throw splash potions right at your feet.");
		e("zielen,aim,besser zielen,aim verbessern,improve aim,sensitivitaet,maus empfindlichkeit,sensitivity",
				"Besser zielen: Fadenkreuz immer auf Kopfhöhe halten, nicht nach unten schauen. Nimm eine Mausempfindlichkeit, mit der du dich mit einer Armbewegung einmal umdrehen kannst. "
						+ "Ein gut sichtbares Fadenkreuz hilft – stell es im Mod-Menü unter FADENKREUZ ein.",
				"Better aim: keep your crosshair at head height, don't look down. Use a mouse sensitivity where one arm movement turns you around once. "
						+ "A clearly visible crosshair helps – set it in the mod menu under CROSSHAIR.");
		e("hit select,hitselect,erster treffer,first hit,gegner zuerst treffen",
				"Hit Select: Nicht blind schlagen, sondern warten, bis der Gegner in deine Reichweite läuft, und genau dann treffen – so bekommst du den ersten Treffer und Rückstoß gegen ihn. "
						+ "Wer zuerst trifft, gewinnt meist den Schlagabtausch.",
				"Hit select: don't swing blindly – wait until your opponent walks into your reach and hit exactly then, so you get the first hit and the knockback. "
						+ "Whoever hits first usually wins the trade.");
		e("rueckzug,zurueckziehen,wenig leben,low health,retreat,wann fliehen",
				"Wenig Leben? Nicht weiterkämpfen: Rückwärts-seitlich weglaufen, Blöcke oder Spinnweben hinter dich setzen, Perle werfen und erst heilen. "
						+ "Komm zurück, wenn der Gegner selbst geschwächt ist oder seine Abklingzeit läuft.",
				"Low health? Don't keep fighting: back off sideways, place blocks or cobwebs behind you, throw a pearl and heal first. "
						+ "Come back when your opponent is weakened or their cooldown is running.");
		e("spinnweben,cobweb,spinnennetz,lava eimer pvp,lava pvp",
				"Spinnweben bremsen Gegner fast komplett – setz sie unter einen Gegner und triff ihn, wenn er festhängt. Ein Lava-Eimer verbrennt ihn, "
						+ "aber pass auf, dass du nicht selbst hineinläufst, und hab Wasser zum Löschen dabei.",
				"Cobwebs almost stop opponents – place one under them and hit while they're stuck. A lava bucket sets them on fire, "
						+ "but don't walk into it yourself and carry water to put it out.");

		// ====================================================== Minecraft – Rezepte (alle, aus Rezepte.java)
		e("rezept,rezepte,craften,crafting,recipe,recipes,how to craft,herstellen,alle rezepte,welche rezepte,rezeptbuch,wie viele rezepte,rezepte kennst,how many recipes",
				"Ich kenne alle " + Rezepte.anzahl() + " Crafting-Rezepte von Minecraft 1.21 – von Werkbank und Fackel über Rüstung, Redstone und Essen "
						+ "bis zu jeder Holz-, Stein- und Farbvariante. Frag einfach: „Rezept Kolben“, „wie baue ich eine Eichentreppe“, "
						+ "„rotes Bett“ oder „Diamanthelm“. Ich zeige dir dann das Bild mit allen Zutaten.",
				"I know all " + Rezepte.anzahl() + " crafting recipes of Minecraft 1.21 – from crafting table and torch to armor, redstone, food "
						+ "and every wood, stone and color variant. Just ask: “piston recipe”, “how do I make oak stairs”, “red bed” "
						+ "or “diamond helmet”. I'll show you a picture with all ingredients.");
		e("gruener farbstoff,gruene farbe,green dye,farbstoff gruen,kaktus farbstoff",
				"Grünen Farbstoff kann man nicht craften: Lege Kaktus in den Ofen und schmelze ihn. Grün + Weiß ergibt dann Hellgrün, Grün + Blau ergibt Türkis.",
				"Green dye can't be crafted: put cactus in a furnace and smelt it. Green + white gives lime, green + blue gives cyan.");
		e("schmelzen,ofen rezepte,was kann man schmelzen,erz schmelzen,smelting,smelt,glas machen,glas herstellen,holzkohle",
				"Im Ofen (oder schneller im Schmelzofen/Räucherofen) wird geschmolzen: Roheisen → Eisenbarren, Rohgold → Goldbarren, Rohkupfer → Kupferbarren, "
						+ "Sand → Glas, Bruchstein → Stein, Stein → Glatter Stein, Ton → Ziegel, Netherrack → Netherziegel, Stamm → Holzkohle, Kaktus → grüner Farbstoff, "
						+ "rohes Fleisch → gebratenes Fleisch. Als Brennstoff gehen Kohle, Holzkohle, Holz, Lohenruten und Lava-Eimer.",
				"A furnace (or the faster blast furnace/smoker) smelts: raw iron → iron ingot, raw gold → gold ingot, raw copper → copper ingot, sand → glass, "
						+ "cobblestone → stone, stone → smooth stone, clay → brick, netherrack → nether brick, log → charcoal, cactus → green dye, raw meat → cooked meat. "
						+ "Fuel: coal, charcoal, wood, blaze rods and lava buckets.");
		e("netherit,netherite,netheritschwert,netheritruestung,netherite sword,netherite armor,netherit aufwertung,smithing template",
				"Netherit-Sachen craftet man nicht direkt: Am Schmiedetisch legst du die Netherit-Aufwertung (Schmiedevorlage aus Bastionen), "
						+ "das Diamant-Teil und einen Netheritbarren zusammen. Netheritbarren = 4 Netheritplatten + 4 Goldbarren; die Platten schmilzt du aus Antikem Schutt "
						+ "(tief im Nether, Y 8–22, am besten mit Betten oder TNT sprengen).",
				"Netherite gear isn't crafted directly: at the smithing table you combine the netherite upgrade (smithing template from bastions), the diamond item "
						+ "and a netherite ingot. Netherite ingot = 4 netherite scrap + 4 gold ingots; smelt the scrap from ancient debris (deep in the Nether, Y 8–22, best blown up with beds or TNT).");
		for (Rezepte.Rezept r : Rezepte.ALLE) {
			e(Rezepte.stichworte(r), Rezepte.text(r, false), Rezepte.text(r, true)).rezept(r);
		}

		// ====================================================== Minecraft – Wissen
		e("diamant,diamanten,diamond,diamonds,wo finde ich diamanten,find diamonds",
				"Diamanten findest du am häufigsten ganz tief unten, um Höhe Y −58 bis −59 (unter dem Tiefenschiefer-Übergang). Du brauchst mindestens eine Eisenspitzhacke. Tipp: Gänge im Abstand von 2 Blöcken graben und auf Lava aufpassen!",
				"Diamonds are most common very deep, around Y −58 to −59. You need at least an iron pickaxe. Tip: dig tunnels 2 blocks apart and watch out for lava!");
		e("netherite,antiker schrott,ancient debris,netherit",
				"Netherite: Antiken Schrott im Nether suchen (am häufigsten um Y 15), im Ofen zu Netheritplatten schmelzen. 4 Platten + 4 Gold = 1 Netheritbarren. Mit einer Netherit-Schmiedevorlage (aus Bastionen) wertest du Diamant-Ausrüstung am Schmiedetisch auf.",
				"Netherite: find ancient debris in the Nether (most common around Y 15) and smelt it into netherite scrap. 4 scrap + 4 gold = 1 netherite ingot. With a netherite upgrade smithing template (from bastions) you upgrade diamond gear at the smithing table.");
		e("nether,netherportal,nether portal,portal bauen,build portal",
				"Netherportal: Ein Rahmen aus Obsidian, mindestens 4 breit und 5 hoch (die Ecken darfst du weglassen, dann reichen 10 Obsidian). Dann innen mit Feuerzeug anzünden. Nimm Gold-Rüstung mit, damit Piglins dich in Ruhe lassen.",
				"Nether portal: an obsidian frame at least 4 wide and 5 tall (you can skip the corners, then 10 obsidian is enough). Light the inside with flint and steel. Wear gold armor so piglins leave you alone.");
		e("ende,enderdrache,end,ender dragon,festung,stronghold,enderauge,eye of ender",
				"So kommst du ins Ende: Enderperlen + Lohenstaub = Enderaugen. Wirf ein Auge – es fliegt Richtung Festung. Dort ist das Endportal, das mit 12 Augen aktiviert wird. Gegen den Enderdrachen zuerst die Kristalle auf den Säulen zerstören (Bogen!).",
				"How to get to the End: ender pearls + blaze powder = eyes of ender. Throw one – it flies towards the stronghold. The End portal there needs 12 eyes. Against the Ender Dragon, first destroy the crystals on the pillars (bow!).");
		e("verzaubern,zaubern,enchant,enchanting,verzauberung,lapis",
				"Verzaubern: Zaubertisch bauen und 15 Bücherregale mit einem Block Abstand drumherum stellen. Dann Gegenstand + Lapislazuli hineinlegen und eine Verzauberung wählen – stärkere kosten mehr Erfahrungslevel (bis 30).",
				"Enchanting: build an enchanting table and place 15 bookshelves around it with a one-block gap. Then put in the item + lapis lazuli and pick an enchantment – stronger ones cost more XP levels (up to 30).");
		e("dorfbewohner,handeln,villager,trading,smaragd,emerald",
				"Dorfbewohner handeln mit Smaragden. Ihren Beruf bekommen sie durch einen Arbeitsblock (z. B. Lesepult = Bibliothekar mit Zauberbüchern). Rechtsklick öffnet das Handelsmenü.",
				"Villagers trade with emeralds. They get their job from a workstation (e.g. lectern = librarian with enchanted books). Right-click opens the trading menu.");
		e("elytra,fliegen,fly,flugel elytra",
				"Elytra findest du im Ende: auf den Endschiffen bei den Endstädten (hinter dem Drachen-Ausgangsportal). Mit Feuerwerksraketen kannst du beim Fliegen beschleunigen.",
				"You find elytra in the End: on End ships near End cities (through the gateway after the dragon). Use firework rockets to boost while flying.");
		e("erste nacht,ueberleben,anfang,anfaenger,first night,survive,beginner,tipps survival",
				"Erste Nacht: 1) Holz sammeln, Werkbank und Holzwerkzeuge bauen. 2) Stein abbauen für bessere Werkzeuge. 3) Vor der Nacht eine kleine Hütte bauen oder in einen Berg graben. 4) Fackeln aufstellen, damit keine Monster spawnen. 5) Schafe für ein Bett finden.",
				"First night: 1) Collect wood, build a crafting table and wooden tools. 2) Mine stone for better tools. 3) Before night, build a small hut or dig into a hill. 4) Place torches so no monsters spawn. 5) Find sheep for a bed.");
		e("trank,traenke,brauen,potion,potions,brewing,braustand",
				"Tränke: Braustand mit Lohenstaub befeuern, Wasserflaschen hineinstellen, Netherwarze dazu = seltsamer Trank. Dann die Zutat: z. B. Zucker = Schnelligkeit, glitzernde Melone = Heilung, Magmacreme = Feuerresistenz.",
				"Potions: fuel the brewing stand with blaze powder, add water bottles, then nether wart = awkward potion. Then the ingredient: e.g. sugar = speed, glistering melon = healing, magma cream = fire resistance.");
		e("redstone,schaltung,kolben,piston",
				"Redstone ist wie Strom in Minecraft: Redstone-Staub leitet Signale, Hebel und Knöpfe schalten sie, Kolben bewegen Blöcke. Ein Signal reicht 15 Blöcke weit – mit einem Verstärker geht es weiter.",
				"Redstone is like electricity in Minecraft: redstone dust carries signals, levers and buttons switch them, pistons move blocks. A signal reaches 15 blocks – use a repeater to go further.");
		e("creeper,zombie,skelett,enderman,monster,mobs",
				"Tipps gegen Monster: Creeper explodieren – weglaufen oder mit Schild blocken. Endermen nicht ansehen (oder Kürbis aufsetzen). Skelette hinter Deckung angreifen. Fackeln und Licht verhindern, dass Monster spawnen.",
				"Tips against monsters: creepers explode – run or block with a shield. Don't look at endermen (or wear a pumpkin). Attack skeletons from behind cover. Torches and light stop monsters from spawning.");
	}
}
