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
		public String[] rezept;          // 9 Felder (Gegenstand oder "") für ein 3×3-Rezept
		public String rezeptDe, rezeptEn;
		public final List<String[]> aktionen = new ArrayList<>();   // {Text de, Text en, Launcher-Seite}

		Eintrag(String stichworte, String de, String en) {
			this.stichworte = stichworte.split(",");
			for (int i = 0; i < this.stichworte.length; i++) this.stichworte[i] = norm(this.stichworte[i]);
			this.de = de;
			this.en = en;
		}

		Eintrag rezept(String nameDe, String nameEn, String... felder) {
			this.rezeptDe = nameDe;
			this.rezeptEn = nameEn;
			this.rezept = felder;
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

	/** Beste Antwort zur Frage oder null. */
	public static Eintrag finde(String frage) {
		String n = " " + norm(frage) + " ";
		String[] woerter = n.trim().split(" ");
		Eintrag beste = null;
		double besteWertung = 0;
		for (Eintrag e : WISSEN) {
			double w = 0;
			double[] proWort = new double[woerter.length];   // jedes Wort der Frage zählt nur einmal
			for (String k : e.stichworte) {
				if (k.isEmpty()) continue;
				if (k.contains(" ")) {
					if (n.contains(" " + k + " ") || n.contains(" " + k)) w += 2.5 * k.split(" ").length;
					continue;
				}
				for (int i = 0; i < woerter.length; i++) {
					String wort = woerter[i];
					double p = 0;
					if (wort.equals(k)) p = 2;
					else if (k.length() >= 4 && wort.length() >= 4 && (wort.startsWith(k) || k.startsWith(wort))) p = 1.5;
					else if (k.length() >= 5 && Math.abs(k.length() - wort.length()) <= 1 && abstand(k, wort) <= 1) p = 1.2;
					proWort[i] = Math.max(proWort[i], p);
				}
			}
			for (double p : proWort) w += p;
			if (w > besteWertung) {
				besteWertung = w;
				beste = e;
			}
		}
		return besteWertung >= 2 ? beste : null;
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
				"Module schaltest du auf der Seite MODULE an und aus – ein Klick auf die Karte genügt. Im Spiel geht das auch im Bambus-Menü (RECHTS-SHIFT). Mit H blendest du das ganze HUD aus und ein.",
				"You turn modules on and off on the MODULES page – just click the card. In game you can also do it in the Bambus menu (RIGHT SHIFT). Press H to hide or show the whole HUD.")
				.aktion("MODULE öffnen", "Open MODULES", "module");
		e("fps,ruckelt,ruckeln,lag,laggt,langsam,fps boost,performance,stutter,slow,more fps,mehr fps,fps erhoehen",
				"Mehr FPS: 1) Auf der Seite MODS den FPS-BOOST einschalten (Sodium, Lithium & Co. – oft doppelt so viele FPS). 2) Im Spiel die Sichtweite auf 8–12 Chunks stellen. 3) Bei sehr langsamen PCs den Low-End-Modus einschalten. 4) Andere Programme schließen.",
				"More FPS: 1) Turn on FPS BOOST on the MODS page (Sodium, Lithium & more – often double the FPS). 2) Set render distance to 8–12 chunks in game. 3) On very slow PCs turn on low-end mode. 4) Close other programs.")
				.aktion("MODS öffnen", "Open MODS", "mods");
		e("ram,arbeitsspeicher,speicher,memory,wie viel gb,how much ram",
				"Für die meisten Computer reichen 4–6 GB. Wähle nie mehr als die Hälfte deines echten Arbeitsspeichers, sonst wird der Computer langsamer. Einstellen kannst du das unter EINSTELLUNGEN.",
				"4–6 GB is enough for most computers. Never pick more than half of your real memory, or the computer gets slower. You can change it under SETTINGS.")
				.aktion("EINSTELLUNGEN öffnen", "Open SETTINGS", "einstellungen");
		e("cape,umhang,fluegel,wings,kosmetik,cosmetics,hut,hat,partikel,particles",
				"Kosmetik wählst du auf der Seite KOSMETIK: Umhänge, 3D-Flügel, Hüte, Partikel, Kill-Effekte und Pfeil-Spuren. Die Figur kannst du mit der Maus drehen. Im Spiel geht es in der Garderobe: RECHTS-SHIFT → GARDEROBE oder ESC → GARDEROBE. Sehen kannst du sie mit F5 (3. Person).",
				"You pick cosmetics on the COSMETICS page: capes, 3D wings, hats, particles, kill effects and arrow trails. Rotate the figure with the mouse. In game use the wardrobe: RIGHT SHIFT → WARDROBE or ESC → WARDROBE. Press F5 (third person) to see them.")
				.aktion("KOSMETIK öffnen", "Open COSMETICS", "kosmetik");
		e("garderobe,wardrobe,kosmetik im spiel,kosmetik ingame,cosmetics in game,umziehen",
				"Die Garderobe öffnest du im Spiel mit RECHTS-SHIFT → GARDEROBE oder im Escape-Menü über die Karte mit deinem Avatar. Links wählst du die Art (Capes, Flügel, Partikel, Hüte, Kill-Effekte, Pfeil-Spuren), rechts das Design – es wird sofort gespeichert.",
				"Open the wardrobe in game with RIGHT SHIFT → WARDROBE or in the pause menu via the card with your avatar. Pick the type on the left (capes, wings, particles, hats, kill effects, arrow trails) and the design on the right – it saves instantly.");
		e("low fire,lowfire,low shield,lowshield,side shield,short sword,kurzes schwert,schwert kleiner,feuer tiefer,schild tiefer,textur,texturen,texture,kuerbis overlay,pumpkin overlay,wasser sicht",
				"Das Textur-Modul ist im Spiel eine Karte unter RECHTS-SHIFT → MOD-MENÜ → MODULE → „Texturen“; die Einstellungen öffnest du mit ⋮ oder Rechtsklick: Low Fire (Feuer tiefer), Low Shield (Schild tiefer), Side Shield (Schild zur Seite), Short Sword (Schwert kürzer), kein Kürbis-Overlay und klare Sicht unter Wasser. Wie tief Feuer und Schild rutschen und wie groß das Schwert ist, stellst du mit ‹ › ein.",
				"The texture module is a card in game under RIGHT SHIFT → MOD MENU → MODULES → “Textures”; open its settings with ⋮ or right-click: Low Fire, Low Shield, Side Shield, Short Sword, no pumpkin overlay and clear underwater view. Use ‹ › to set how low fire and shield go and how big the sword is.");
		e("helfer ausschalten,helfer deaktivieren,helfer weg,disable helper,turn off helper,hide helper",
				"Im Spiel kannst du mich ausblenden: RECHTS-SHIFT → MOD-MENÜ → MEHR → EXTRAS → „Bambus-Helfer“ ausschalten. Dann zeigt RECHTS-SHIFT nur noch Garderobe und Mod-Menü. Im Launcher bleibe ich auf der Seite HELFER.",
				"You can hide me in game: RIGHT SHIFT → MOD MENU → MORE → EXTRAS → turn off “Bambus Helper”. Then RIGHT SHIFT only shows wardrobe and mod menu. In the launcher I stay on the HELPER page.");
		e("sehen andere,andere spieler,others see,can others see,sichtbar fuer andere",
				"Die Bambus-Kosmetik siehst nur du selbst. Andere Spieler sehen sie nicht – dafür bräuchte Bambus einen eigenen Server.",
				"Only you can see Bambus cosmetics. Other players can't see them – that would need a Bambus server.");
		e("eigenes cape,eigene fluegel,studio,selbst machen,eigenes bild,custom cape,own cape,make my own",
				"Im Kosmetik-Studio baust du eigene Capes und Flügel: auf der Seite KOSMETIK bei Capes oder Flügel auf „+ Erstellen“ klicken. Du kannst Vorlagen, Farben und Initialen wählen oder ein eigenes Bild nehmen.",
				"In the cosmetics studio you can build your own capes and wings: on the COSMETICS page click “+ Create” under capes or wings. Pick templates, colors and initials, or use your own image.")
				.aktion("KOSMETIK öffnen", "Open COSMETICS", "kosmetik");
		e("skin,skin laden,mein skin,load skin,change skin,skin aendern",
				"Auf der Seite KOSMETIK deinen Minecraft-Namen eingeben und LADEN klicken – dann siehst du deinen Skin in der 3D-Vorschau. Deinen echten Skin im Spiel änderst du auf minecraft.net in deinem Profil.",
				"On the COSMETICS page enter your Minecraft name and click LOAD – then you see your skin in the 3D preview. To change your real in-game skin, go to your profile on minecraft.net.")
				.aktion("KOSMETIK öffnen", "Open COSMETICS", "kosmetik");
		e("zoom,zoomen,ranzoomen,optifine zoom",
				"Zoom: C gedrückt halten, mit dem Mausrad zoomst du weiter rein oder raus. Die Taste kannst du unter Tastenbelegung ändern.",
				"Zoom: hold C, and use the mouse wheel to zoom further in or out. You can change the key under key binds.");
		e("freelook,umschauen,perspektive,look around,alt",
				"Freelook: Linke Alt-Taste (Mac: Option) halten und mit der Maus umschauen, ohne die Laufrichtung zu ändern. Achtung: Auf manchen Servern ist Freelook nicht erlaubt.",
				"Freelook: hold left Alt (Mac: Option) and look around with the mouse without changing your direction. Note: some servers don't allow freelook.");
		e("toggle sprint,sprinten,immer sprinten,toggle sneak,schleichen,sprint",
				"Toggle Sprint und Toggle Sneak schaltest du auf der Seite MODULE bei den Funktionen an. Dann reicht ein Druck auf die Sprint- bzw. Schleich-Taste, und es bleibt an.",
				"Turn on toggle sprint and toggle sneak on the MODULES page under features. Then one press of the sprint or sneak key keeps it on.")
				.aktion("MODULE öffnen", "Open MODULES", "module");
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

		// ====================================================== Minecraft – Rezepte
		e("werkbank,crafting table,werktisch",
				"Werkbank: 4 Holzbretter im 2×2-Feld deines Inventars.", "Crafting table: 4 wooden planks in the 2×2 grid of your inventory.")
				.rezept("Werkbank", "Crafting Table", "Brett", "Brett", "", "Brett", "Brett", "", "", "", "");
		e("fackel,torch,licht",
				"Fackel: Kohle (oder Holzkohle) über einen Stock legen – ergibt 4 Fackeln.", "Torch: coal (or charcoal) above a stick – makes 4 torches.")
				.rezept("Fackel", "Torch", "", "Kohle", "", "", "Stock", "", "", "", "");
		e("truhe,kiste,chest",
				"Truhe: 8 Holzbretter im Kreis, die Mitte bleibt frei.", "Chest: 8 wooden planks in a ring, the middle stays empty.")
				.rezept("Truhe", "Chest", "Brett", "Brett", "Brett", "Brett", "", "Brett", "Brett", "Brett", "Brett");
		e("ofen,furnace,schmelzen",
				"Ofen: 8 Bruchstein im Kreis, die Mitte bleibt frei. Damit schmilzt du Erz und brätst Essen.", "Furnace: 8 cobblestone in a ring, middle empty. Use it to smelt ore and cook food.")
				.rezept("Ofen", "Furnace", "Stein", "Stein", "Stein", "Stein", "", "Stein", "Stein", "Stein", "Stein");
		e("spitzhacke,pickaxe,picke",
				"Spitzhacke: oben 3 Material (Holz, Stein, Eisen oder Diamant), darunter 2 Stöcke in der Mitte.", "Pickaxe: 3 material on top (wood, stone, iron or diamond), 2 sticks below in the middle.")
				.rezept("Spitzhacke", "Pickaxe", "Eisen", "Eisen", "Eisen", "", "Stock", "", "", "Stock", "");
		e("schwert,sword",
				"Schwert: 2 Material übereinander, darunter 1 Stock – alles in der mittleren Spalte.", "Sword: 2 material stacked, 1 stick below – all in the middle column.")
				.rezept("Schwert", "Sword", "", "Diamant", "", "", "Diamant", "", "", "Stock", "");
		e("axt,axe",
				"Axt: 3 Material oben links in L-Form, 2 Stöcke in der Mitte.", "Axe: 3 material in an L shape at the top left, 2 sticks in the middle.")
				.rezept("Axt", "Axe", "Eisen", "Eisen", "", "Eisen", "Stock", "", "", "Stock", "");
		e("bett,bed,schlafen,schlafe,schlaf,sleep,nacht ueberspringen",
				"Bett: oben 3 Wolle (gleiche Farbe), darunter 3 Holzbretter. Schlafen überspringt die Nacht und setzt deinen Spawnpunkt.", "Bed: 3 wool on top (same color), 3 wooden planks below. Sleeping skips the night and sets your spawn point.")
				.rezept("Bett", "Bed", "Wolle", "Wolle", "Wolle", "Brett", "Brett", "Brett", "", "", "");
		e("zaubertisch,verzauberungstisch,enchanting table,enchantment table",
				"Zaubertisch: oben ein Buch, in der Mitte Diamant – Obsidian – Diamant, unten 3 Obsidian.", "Enchanting table: a book on top, diamond – obsidian – diamond in the middle, 3 obsidian at the bottom.")
				.rezept("Zaubertisch", "Enchanting Table", "", "Buch", "", "Diamant", "Obsidian", "Diamant", "Obsidian", "Obsidian", "Obsidian");
		e("amboss,anvil",
				"Amboss: oben 3 Eisenblöcke, in der Mitte 1 Eisenbarren, unten 3 Eisenbarren. Damit reparierst und benennst du Sachen.", "Anvil: 3 iron blocks on top, 1 iron ingot in the middle, 3 iron ingots at the bottom. Use it to repair and rename items.")
				.rezept("Amboss", "Anvil", "Eisenblock", "Eisenblock", "Eisenblock", "", "Eisen", "", "Eisen", "Eisen", "Eisen");
		e("bogen,bow",
				"Bogen: 3 Stöcke als Bogen links, 3 Fäden als Sehne rechts.", "Bow: 3 sticks as the bow on the left, 3 strings on the right.")
				.rezept("Bogen", "Bow", "", "Stock", "Faden", "Stock", "", "Faden", "", "Stock", "Faden");
		e("schild,shield",
				"Schild: Brett – Eisen – Brett, darunter 3 Bretter, unten 1 Brett in der Mitte.", "Shield: plank – iron – plank, then 3 planks, then 1 plank in the middle.")
				.rezept("Schild", "Shield", "Brett", "Eisen", "Brett", "Brett", "Brett", "Brett", "", "Brett", "");
		e("eimer,bucket,wassereimer",
				"Eimer: 3 Eisenbarren in V-Form.", "Bucket: 3 iron ingots in a V shape.")
				.rezept("Eimer", "Bucket", "Eisen", "", "Eisen", "", "Eisen", "", "", "", "");
		e("tnt,sprengstoff",
				"TNT: 5 Schwarzpulver und 4 Sand im Schachbrettmuster.", "TNT: 5 gunpowder and 4 sand in a checkerboard pattern.")
				.rezept("TNT", "TNT", "Pulver", "Sand", "Pulver", "Sand", "Pulver", "Sand", "Pulver", "Sand", "Pulver");
		e("buecherregal,bookshelf,regal",
				"Bücherregal: oben und unten je 3 Bretter, in der Mitte 3 Bücher. 15 Regale um den Zaubertisch geben die besten Verzauberungen.", "Bookshelf: 3 planks on top and bottom, 3 books in the middle. 15 shelves around the enchanting table give the best enchantments.")
				.rezept("Bücherregal", "Bookshelf", "Brett", "Brett", "Brett", "Buch", "Buch", "Buch", "Brett", "Brett", "Brett");
		e("leiter,ladder",
				"Leiter: 7 Stöcke in H-Form – ergibt 3 Leitern.", "Ladder: 7 sticks in an H shape – makes 3 ladders.")
				.rezept("Leiter", "Ladder", "Stock", "", "Stock", "Stock", "Stock", "Stock", "Stock", "", "Stock");
		e("rezept,rezepte,craften,crafting,recipe,recipes,how to craft,herstellen",
				"Ich kenne viele Rezepte: Werkbank, Fackel, Truhe, Ofen, Spitzhacke, Schwert, Axt, Bett, Zaubertisch, Amboss, Bogen, Schild, Eimer, TNT, Bücherregal und Leiter. Frag zum Beispiel: „Rezept Zaubertisch“. Im Spiel hilft dir auch das Rezeptbuch (grünes Buch im Inventar).",
				"I know lots of recipes: crafting table, torch, chest, furnace, pickaxe, sword, axe, bed, enchanting table, anvil, bow, shield, bucket, TNT, bookshelf and ladder. Ask for example: “enchanting table recipe”. In game the recipe book (green book in your inventory) helps too.");

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
