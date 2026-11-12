package de.bambus.helfer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Alle Crafting-Rezepte von Minecraft Java Edition 1.21 für den Bambus-Helfer (Werkbank bzw. 2×2-Feld).
 * Gegenstände stehen als Minecraft-Id (z. B. "oak_planks") – so zeigt das Spiel echte Icons.
 * „#…“ steht für eine Gruppe (z. B. "#planks" = beliebige Holzbretter).
 * Holz-, Farb- und Steinvarianten werden aus Vorlagen erzeugt. Java-8-kompatibel (Launcher läuft ab Java 8).
 */
public final class Rezepte {

	public static final class Rezept {
		public final String id, de, en;
		public final int anzahl;
		public final String[] felder;      // 9 Felder (Id oder ""), Zeile für Zeile
		public final boolean formlos;      // Form egal (shapeless)
		public final String gruppe;        // z. B. "Holz", "Werkzeug"
		String hinweisDe = "", hinweisEn = "";
		final List<String> worte = new ArrayList<>();

		Rezept(String id, String de, String en, int anzahl, String[] felder, boolean formlos, String gruppe) {
			this.id = id;
			this.de = de;
			this.en = en;
			this.anzahl = anzahl;
			this.felder = felder;
			this.formlos = formlos;
			this.gruppe = gruppe;
		}

		Rezept hinweis(String de, String en) {
			hinweisDe = de;
			hinweisEn = en;
			return this;
		}

		Rezept worte(String... w) {
			for (String x : w) worte.add(x);
			return this;
		}

		/** Passt ins 2×2-Feld des Inventars? */
		public boolean klein() {
			if (formlos) {
				int n = 0;
				for (String f : felder) if (!f.isEmpty()) n++;
				return n <= 4;
			}
			for (int i = 0; i < 9; i++) {
				if (!felder[i].isEmpty() && (i % 3 == 2 || i / 3 == 2)) return false;
			}
			return true;
		}
	}

	public static final List<Rezept> ALLE = new ArrayList<>();
	/** Name, Kurzname und Farbe eines Gegenstands: {de, en, kurzDe, kurzEn, farbe} */
	private static final Map<String, Object[]> DINGE = new HashMap<>();
	/** Gruppen ("#planks") → Gegenstand, der als Bild gezeigt wird. */
	private static final Map<String, String> ANZEIGE = new HashMap<>();

	private static String gruppe = "";

	private Rezepte() {}

	// ================================================================ Abfragen

	/** Anzeigename (de/en) – auch für Gruppen wie "#planks". */
	public static String name(String id, boolean en) {
		Object[] d = DINGE.get(id);
		if (d != null) return (String) (en ? d[1] : d[0]);
		for (Rezept r : ALLE) if (r.id.equals(id)) return en ? r.en : r.de;
		return id.replace('#', ' ').replace('_', ' ').trim();
	}

	/** Kurzer Name für ein Rezept-Feld im Launcher (höchstens etwa 8 Zeichen). */
	public static String kurz(String id, boolean en) {
		Object[] d = DINGE.get(id);
		if (d != null && d[2] != null) return (String) (en ? d[3] : d[2]);
		String n = name(id, en);
		return n.length() <= 9 ? n : n.substring(0, 8) + ".";
	}

	/** Farbe des Felds im Launcher (RGB). */
	public static int farbe(String id) {
		Object[] d = DINGE.get(id);
		if (d != null && d[4] != null) return (Integer) d[4];
		return 0x8A9BB0;
	}

	/** Minecraft-Id, deren Bild gezeigt wird (Gruppen → ein Beispiel). */
	public static String anzeige(String id) {
		String a = ANZEIGE.get(id);
		return a != null ? a : id;
	}

	// ================================================================ Bausteine

	private static void ding(String id, String de, String en, String kurzDe, String kurzEn, int farbe) {
		DINGE.put(id, new Object[]{de, en, kurzDe, kurzEn, farbe});
	}

	private static void ding(String id, String de, String en, int farbe) {
		DINGE.put(id, new Object[]{de, en, null, null, farbe});
	}

	/** Rezept mit Form: Zeilen mit „|“ getrennt, Schlüssel z. B. "P=#planks,S=stick". */
	private static Rezept r(String id, String de, String en, int anzahl, String form, String schluessel) {
		Map<Character, String> k = new HashMap<>();
		for (String teil : schluessel.split(",")) {
			if (teil.isEmpty()) continue;
			k.put(teil.charAt(0), teil.substring(2));
		}
		String[] zeilen = form.split("\\|", -1);
		int breite = 0;
		for (String z : zeilen) breite = Math.max(breite, z.length());
		int links = breite == 1 ? 1 : 0;   // einspaltige Rezepte in die Mitte
		String[] felder = new String[9];
		for (int i = 0; i < 9; i++) felder[i] = "";
		for (int zi = 0; zi < zeilen.length && zi < 3; zi++) {
			for (int si = 0; si < zeilen[zi].length() && si + links < 3; si++) {
				char c = zeilen[zi].charAt(si);
				if (c != ' ') felder[zi * 3 + si + links] = k.get(c);
			}
		}
		Rezept x = new Rezept(id, de, en, anzahl, felder, false, gruppe);
		ALLE.add(x);
		return x;
	}

	/** Rezept ohne feste Form: Zutaten mit Leerzeichen getrennt, „3*paper“ = dreimal. */
	private static Rezept s(String id, String de, String en, int anzahl, String zutaten) {
		String[] felder = new String[9];
		for (int i = 0; i < 9; i++) felder[i] = "";
		int i = 0;
		for (String z : zutaten.trim().split("\\s+")) {
			int mal = 1;
			if (z.contains("*")) {
				mal = Integer.parseInt(z.substring(0, z.indexOf('*')));
				z = z.substring(z.indexOf('*') + 1);
			}
			for (int m = 0; m < mal && i < 9; m++) felder[i++] = z;
		}
		Rezept x = new Rezept(id, de, en, anzahl, felder, true, gruppe);
		ALLE.add(x);
		return x;
	}

	// ================================================================ Daten

	static {
		// ---------------------------------------------------------- Zutaten (Namen, Kurznamen, Farben)
		ding("#planks", "Holzbretter (beliebig)", "Planks (any)", "Bretter", "Planks", 0xB8894F);
		ding("#logs", "Holzstamm (beliebig)", "Log (any)", "Stamm", "Log", 0x6E5434);
		ding("#wool", "Wolle (beliebig)", "Wool (any)", "Wolle", "Wool", 0xF2F2F2);
		ding("#wooden_slabs", "Holzstufe (beliebig)", "Wooden slab (any)", "Holzst.", "W. slab", 0xB8894F);
		ding("#stone_crafting", "Bruchstein, Schwarzstein oder Tiefenschiefer-Bruchstein", "Cobblestone, blackstone or cobbled deepslate",
				"Bruchst.", "Cobble", 0x8C939C);
		ding("#coals", "Kohle oder Holzkohle", "Coal or charcoal", "Kohle", "Coal", 0x3A3F47);
		ding("#soul", "Seelensand oder Seelenerde", "Soul sand or soul soil", "Seelens.", "Soul s.", 0x5B4636);
		ding("#stripped_logs", "Entrindeter Stamm", "Stripped log", "Entr. St.", "Strip. log", 0xC9A36B);
		ANZEIGE.put("#planks", "oak_planks");
		ANZEIGE.put("#logs", "oak_log");
		ANZEIGE.put("#wool", "white_wool");
		ANZEIGE.put("#wooden_slabs", "oak_slab");
		ANZEIGE.put("#stone_crafting", "cobblestone");
		ANZEIGE.put("#coals", "coal");
		ANZEIGE.put("#soul", "soul_sand");
		ANZEIGE.put("#stripped_logs", "stripped_oak_log");

		Object[][] grund = {
				{"stick", "Stock", "Stick", "Stock", "Stick", 0x8A6A3E},
				{"coal", "Kohle", "Coal", "Kohle", "Coal", 0x3A3F47},
				{"charcoal", "Holzkohle", "Charcoal", "Holzkohle", "Charcoal", 0x4A4038},
				{"cobblestone", "Bruchstein", "Cobblestone", "Bruchst.", "Cobble", 0x8C939C},
				{"stone", "Stein", "Stone", "Stein", "Stone", 0x9A9A9A},
				{"smooth_stone", "Glatter Stein", "Smooth Stone", "Gl. Stein", "Sm. stone", 0xA8A8A8},
				{"iron_ingot", "Eisenbarren", "Iron Ingot", "Eisen", "Iron", 0xD8DEE6},
				{"gold_ingot", "Goldbarren", "Gold Ingot", "Gold", "Gold", 0xF5D33F},
				{"copper_ingot", "Kupferbarren", "Copper Ingot", "Kupfer", "Copper", 0xE07A4F},
				{"diamond", "Diamant", "Diamond", "Diamant", "Diamond", 0x4FE3E0},
				{"emerald", "Smaragd", "Emerald", "Smaragd", "Emerald", 0x3DDB6A},
				{"lapis_lazuli", "Lapislazuli", "Lapis Lazuli", "Lapis", "Lapis", 0x2752C9},
				{"redstone", "Redstone-Staub", "Redstone Dust", "Redstone", "Redstone", 0xD0201B},
				{"quartz", "Netherquarz", "Nether Quartz", "Quarz", "Quartz", 0xEAE4DC},
				{"netherite_scrap", "Netheritplatten", "Netherite Scrap", "Neth.Pl.", "Scrap", 0x5A4A46},
				{"string", "Faden", "String", "Faden", "String", 0xE8E8E8},
				{"feather", "Feder", "Feather", "Feder", "Feather", 0xF0F0F0},
				{"flint", "Feuerstein", "Flint", "Feuerst.", "Flint", 0x4A4A4A},
				{"leather", "Leder", "Leather", "Leder", "Leather", 0x9C5A32},
				{"sugar_cane", "Zuckerrohr", "Sugar Cane", "Zuckerr.", "Cane", 0x8FCB5A},
				{"wheat", "Weizen", "Wheat", "Weizen", "Wheat", 0xD6B656},
				{"egg", "Ei", "Egg", "Ei", "Egg", 0xF2E2C4},
				{"milk_bucket", "Milcheimer", "Milk Bucket", "Milch", "Milk", 0xF4F4F4},
				{"apple", "Apfel", "Apple", "Apfel", "Apple", 0xD8262A},
				{"carrot", "Karotte", "Carrot", "Karotte", "Carrot", 0xF08A1C},
				{"melon_slice", "Melonenscheibe", "Melon Slice", "Melone", "Melon", 0xE04A3A},
				{"pumpkin", "Kürbis", "Pumpkin", "Kürbis", "Pumpkin", 0xE38A1D},
				{"carved_pumpkin", "Geschnitzter Kürbis", "Carved Pumpkin", "Kürbis", "Pumpkin", 0xE38A1D},
				{"cocoa_beans", "Kakaobohnen", "Cocoa Beans", "Kakao", "Cocoa", 0x7A4A28},
				{"bone", "Knochen", "Bone", "Knochen", "Bone", 0xEDEAD8},
				{"ink_sac", "Tintenbeutel", "Ink Sac", "Tinte", "Ink", 0x2A2A35},
				{"glow_ink_sac", "Leuchttintenbeutel", "Glow Ink Sac", "Leuchtt.", "Glow ink", 0x4FD6C8},
				{"slime_ball", "Schleimball", "Slimeball", "Schleim", "Slime", 0x79C25A},
				{"gunpowder", "Schwarzpulver", "Gunpowder", "Pulver", "Powder", 0x5A5A5A},
				{"sand", "Sand", "Sand", "Sand", "Sand", 0xE3D49A},
				{"red_sand", "Roter Sand", "Red Sand", "R. Sand", "Red sand", 0xC2672A},
				{"gravel", "Kies", "Gravel", "Kies", "Gravel", 0x87807E},
				{"glass", "Glas", "Glass", "Glas", "Glass", 0xCFEFF5},
				{"obsidian", "Obsidian", "Obsidian", "Obsidian", "Obsidian", 0x3B2A5C},
				{"crying_obsidian", "Weinender Obsidian", "Crying Obsidian", "Wein.Obs.", "Cry. obs.", 0x5B2A8C},
				{"glowstone_dust", "Glowstonestaub", "Glowstone Dust", "Glowst.", "Glowst.", 0xF1C75A},
				{"blaze_rod", "Lohenrute", "Blaze Rod", "Lohenr.", "Blaze rod", 0xF0A21E},
				{"ender_pearl", "Enderperle", "Ender Pearl", "Perle", "Pearl", 0x1E6E5A},
				{"ghast_tear", "Ghastträne", "Ghast Tear", "Träne", "Tear", 0xDDF3F3},
				{"nether_star", "Netherstern", "Nether Star", "Stern", "Star", 0xF6F6CE},
				{"nautilus_shell", "Nautilusschale", "Nautilus Shell", "Nautilus", "Nautilus", 0xD8C8B8},
				{"heart_of_the_sea", "Herz des Meeres", "Heart of the Sea", "Herz", "Heart", 0x2A7AD8},
				{"honeycomb", "Honigwabe", "Honeycomb", "Wabe", "Comb", 0xE8A62A},
				{"honey_bottle", "Honigflasche", "Honey Bottle", "Honig", "Honey", 0xF0B030},
				{"echo_shard", "Echoscherbe", "Echo Shard", "Echo", "Echo", 0x1D3A4A},
				{"amethyst_shard", "Amethystscherbe", "Amethyst Shard", "Amethyst", "Amethyst", 0xA77BDB},
				{"breeze_rod", "Böenrute", "Breeze Rod", "Böenrute", "Breeze r.", 0x9EB3E8},
				{"heavy_core", "Schwerer Kern", "Heavy Core", "Kern", "Core", 0x4A4E57},
				{"popped_chorus_fruit", "Geplatzte Chorusfrucht", "Popped Chorus Fruit", "Chorus", "Chorus", 0xA67BA6},
				{"prismarine_shard", "Prismarinscherbe", "Prismarine Shard", "Prism.S.", "Shard", 0x63A89A},
				{"prismarine_crystals", "Prismarinkristalle", "Prismarine Crystals", "Prism.K.", "Crystals", 0xBDE3D6},
				{"nether_brick", "Netherziegel", "Nether Brick", "Netherz.", "N. brick", 0x4A2328},
				{"brick", "Ziegel", "Brick", "Ziegel", "Brick", 0xA0533A},
				{"clay_ball", "Ton", "Clay Ball", "Ton", "Clay", 0xA4A8B8},
				{"snowball", "Schneeball", "Snowball", "Schnee", "Snow", 0xF6FBFF},
				{"nether_wart", "Netherwarze", "Nether Wart", "Warze", "Wart", 0x8A1E24},
				{"vine", "Ranken", "Vines", "Ranken", "Vines", 0x3E7A2A},
				{"moss_block", "Moosblock", "Moss Block", "Moos", "Moss", 0x5E7A2E},
				{"mud", "Schlamm", "Mud", "Schlamm", "Mud", 0x3C3437},
				{"bamboo", "Bambus", "Bamboo", "Bambus", "Bamboo", 0x7BA83A},
				{"beetroot", "Rote Bete", "Beetroot", "R. Bete", "Beetroot", 0x9A2A3A},
				{"red_mushroom", "Roter Pilz", "Red Mushroom", "R. Pilz", "Red mush.", 0xD03A2A},
				{"brown_mushroom", "Brauner Pilz", "Brown Mushroom", "Br. Pilz", "Br. mush.", 0x9A7050},
				{"poppy", "Mohn", "Poppy", "Mohn", "Poppy", 0xD82A2A},
				{"dandelion", "Löwenzahn", "Dandelion", "Löwenz.", "Dandel.", 0xF5E02A},
				{"cornflower", "Kornblume", "Cornflower", "Kornbl.", "Cornfl.", 0x4A6AE8},
				{"lily_of_the_valley", "Maiglöckchen", "Lily of the Valley", "Maiglö.", "Lily", 0xF2F2F2},
				{"wither_rose", "Wither-Rose", "Wither Rose", "Witherr.", "W. rose", 0x2A2A2A},
				{"rabbit_hide", "Kaninchenfell", "Rabbit Hide", "Fell", "Hide", 0xC6A27A},
				{"turtle_scute", "Schildkrötenschuppe", "Turtle Scute", "Schuppe", "Scute", 0x4FA84A},
				{"dried_kelp", "Getrockneter Seetang", "Dried Kelp", "Seetang", "Kelp", 0x3A4A2A},
				{"sculk_sensor", "Sculk-Sensor", "Sculk Sensor", "Sensor", "Sensor", 0x0E5A6A},
				{"copper_nugget", "Kupferklumpen", "Copper Nugget", "Kupferkl.", "Cu nugget", 0xE07A4F},
				{"pointed_dripstone", "Spitzer Tropfstein", "Pointed Dripstone", "Tropfst.", "Dripst.", 0x8A6E5A},
				{"resin_brick", "Harzziegel", "Resin Brick", "Harzz.", "Resin br.", 0xD9762A},
				{"milk", "Milch", "Milk", "Milch", "Milk", 0xF4F4F4},
				{"poisonous_potato", "Giftige Kartoffel", "Poisonous Potato", "G. Kart.", "P. potato", 0xB8C24A},
		};
		for (Object[] g : grund) ding((String) g[0], (String) g[1], (String) g[2], (String) g[3], (String) g[4], (Integer) g[5]);

		// ---------------------------------------------------------- Grundlagen
		gruppe = "Grundlagen";
		r("stick", "Stock", "Stick", 4, "P|P", "P=#planks").worte("stoecke", "stab", "staebe");
		r("crafting_table", "Werkbank", "Crafting Table", 1, "PP|PP", "P=#planks").worte("werktisch", "crafting table", "craftingtable");
		r("chest", "Truhe", "Chest", 1, "PPP|P P|PPP", "P=#planks").worte("kiste");
		r("furnace", "Ofen", "Furnace", 1, "CCC|C C|CCC", "C=#stone_crafting").worte("steinofen");
		r("torch", "Fackel", "Torch", 4, "C|S", "C=#coals,S=stick").worte("licht");
		r("soul_torch", "Seelenfackel", "Soul Torch", 4, "C|S|O", "C=#coals,S=stick,O=#soul");
		r("ladder", "Leiter", "Ladder", 3, "S S|SSS|S S", "S=stick");
		r("bowl", "Schüssel", "Bowl", 4, "P P| P ", "P=#planks").worte("schale");
		r("barrel", "Fass", "Barrel", 1, "PSP|P P|PSP", "P=#planks,S=#wooden_slabs");
		r("trapped_chest", "Redstone-Truhe", "Trapped Chest", 1, "CH", "C=chest,H=tripwire_hook").worte("fallentruhe");
		r("smoker", "Räucherofen", "Smoker", 1, " L |LFL| L ", "L=#logs,F=furnace");
		r("blast_furnace", "Schmelzofen", "Blast Furnace", 1, "III|IFI|SSS", "I=iron_ingot,F=furnace,S=smooth_stone").worte("hochofen");
		r("campfire", "Lagerfeuer", "Campfire", 1, " S |SCS|LLL", "S=stick,C=#coals,L=#logs");
		r("soul_campfire", "Seelenlagerfeuer", "Soul Campfire", 1, " S |SOS|LLL", "S=stick,O=#soul,L=#logs");
		r("lantern", "Laterne", "Lantern", 1, "NNN|NTN|NNN", "N=iron_nugget,T=torch");
		r("soul_lantern", "Seelenlaterne", "Soul Lantern", 1, "NNN|NTN|NNN", "N=iron_nugget,T=soul_torch");
		r("scaffolding", "Gerüst", "Scaffolding", 6, "BSB|B B|B B", "B=bamboo,S=string");
		r("glass_pane", "Glasscheibe", "Glass Pane", 16, "GGG|GGG", "G=glass").worte("fenster");
		r("iron_bars", "Eisengitter", "Iron Bars", 16, "III|III", "I=iron_ingot").worte("gitter");
		r("iron_chain", "Eisenkette", "Iron Chain", 1, "N|I|N", "N=iron_nugget,I=iron_ingot").worte("kette", "chain");
		r("paper", "Papier", "Paper", 3, "SSS", "S=sugar_cane");
		s("book", "Buch", "Book", 1, "3*paper leather").worte("buecher");
		r("bookshelf", "Bücherregal", "Bookshelf", 1, "PPP|BBB|PPP", "P=#planks,B=book").worte("regal")
				.hinweis("15 Regale um den Zaubertisch geben die stärksten Verzauberungen.", "15 bookshelves around the enchanting table give the best enchantments.");
		r("chiseled_bookshelf", "Gearbeitetes Bücherregal", "Chiseled Bookshelf", 1, "PPP|SSS|PPP", "P=#planks,S=#wooden_slabs");
		s("writable_book", "Buch und Feder", "Book and Quill", 1, "book ink_sac feather").worte("schreibbuch", "buch und feder");
		r("item_frame", "Rahmen", "Item Frame", 1, "SSS|SLS|SSS", "S=stick,L=leather").worte("bilderrahmen", "itemrahmen");
		s("glow_item_frame", "Leuchtrahmen", "Glow Item Frame", 1, "item_frame glow_ink_sac");
		r("painting", "Gemälde", "Painting", 1, "SSS|SWS|SSS", "S=stick,W=#wool").worte("bild");
		r("armor_stand", "Rüstungsständer", "Armor Stand", 1, "SSS| S |SPS", "S=stick,P=smooth_stone_slab");
		r("flower_pot", "Blumentopf", "Flower Pot", 1, "B B| B ", "B=brick");
		r("decorated_pot", "Verzierter Krug", "Decorated Pot", 1, " B |B B| B ", "B=brick").worte("krug", "topf")
				.hinweis("Statt Ziegeln gehen auch Töpferscherben – dann bekommt der Krug Bilder.", "Pottery sherds work instead of bricks – then the pot gets pictures.");
		r("lead", "Leine", "Lead", 2, "SS |SB |  S", "S=string,B=slime_ball").worte("seil");
		r("candle", "Kerze", "Candle", 1, "S|H", "S=string,H=honeycomb");
		r("bundle", "Bündel", "Bundle", 1, "S|L", "S=string,L=leather").worte("beutel");
		r("glowstone", "Glowstone", "Glowstone", 1, "GG|GG", "G=glowstone_dust").worte("leuchtstein");
		r("jack_o_lantern", "Kürbislaterne", "Jack o'Lantern", 1, "P|T", "P=carved_pumpkin,T=torch");
		r("snow_block", "Schneeblock", "Snow Block", 1, "SS|SS", "S=snowball");
		r("snow", "Schnee", "Snow", 6, "SSS", "S=snow_block").worte("schneeschicht");
		r("clay", "Tonblock", "Clay", 1, "CC|CC", "C=clay_ball");
		r("bricks", "Ziegelsteine", "Bricks", 1, "BB|BB", "B=brick").worte("ziegelblock");
		r("white_wool", "Weiße Wolle", "White Wool", 1, "SS|SS", "S=string").worte("wolle", "wool");
		r("leather", "Leder", "Leather", 1, "HH|HH", "H=rabbit_hide");
		s("bone_meal", "Knochenmehl", "Bone Meal", 3, "bone").worte("duenger");
		r("bone_block", "Knochenblock", "Bone Block", 1, "MMM|MMM|MMM", "M=bone_meal");
		s("sugar", "Zucker", "Sugar", 1, "sugar_cane");
		r("tinted_glass", "Getöntes Glas", "Tinted Glass", 2, " A |AGA| A ", "A=amethyst_shard,G=glass");
		r("amethyst_block", "Amethystblock", "Block of Amethyst", 1, "AA|AA", "A=amethyst_shard");
		r("dripstone_block", "Tropfsteinblock", "Dripstone Block", 1, "PP|PP", "P=pointed_dripstone");
		r("moss_carpet", "Moosteppich", "Moss Carpet", 3, "MM", "M=moss_block");
		s("packed_mud", "Fester Schlamm", "Packed Mud", 1, "mud wheat");

		// ---------------------------------------------------------- Werkzeuge, Waffen, Rüstung
		gruppe = "Werkzeug";
		Object[][] stoffe = {   // {Material, Wortanfang de, en, Id-Anfang}
				{"#planks", "Holz", "Wooden", "wooden"},
				{"#stone_crafting", "Stein", "Stone", "stone"},
				{"copper_ingot", "Kupfer", "Copper", "copper"},
				{"iron_ingot", "Eisen", "Iron", "iron"},
				{"gold_ingot", "Gold", "Golden", "golden"},
				{"diamond", "Diamant", "Diamond", "diamond"},
		};
		for (Object[] m : stoffe) {
			String k = "M=" + m[0] + ",S=stick", de = (String) m[1], en = (String) m[2], id = (String) m[3];
			r(id + "_pickaxe", de + "spitzhacke", en + " Pickaxe", 1, "MMM| S | S ", k).worte("spitzhacke", "picke", "pickaxe");
			r(id + "_axe", de + "axt", en + " Axe", 1, "MM|MS| S", k).worte("axt", "axe");
			r(id + "_shovel", de + "schaufel", en + " Shovel", 1, "M|S|S", k).worte("schaufel", "shovel");
			r(id + "_hoe", de + "hacke", en + " Hoe", 1, "MM| S| S", k).worte("feldhacke", "hoe");
			r(id + "_sword", de + "schwert", en + " Sword", 1, "M|M|S", k).worte("schwert", "sword");
		}
		Object[][] ruestung = {
				{"leather", "Leder", "Leather", "leather", "kappe", "jacke", "hose", "stiefel", "Cap", "Tunic", "Pants", "Boots"},
				{"copper_ingot", "Kupfer", "Copper", "copper", "helm", "harnisch", "beinschutz", "stiefel", "Helmet", "Chestplate", "Leggings", "Boots"},
				{"iron_ingot", "Eisen", "Iron", "iron", "helm", "harnisch", "beinschutz", "stiefel", "Helmet", "Chestplate", "Leggings", "Boots"},
				{"gold_ingot", "Gold", "Golden", "golden", "helm", "harnisch", "beinschutz", "stiefel", "Helmet", "Chestplate", "Leggings", "Boots"},
				{"diamond", "Diamant", "Diamond", "diamond", "helm", "harnisch", "beinschutz", "stiefel", "Helmet", "Chestplate", "Leggings", "Boots"},
		};
		for (Object[] m : ruestung) {
			String k = "M=" + m[0], de = (String) m[1], en = (String) m[2], id = (String) m[3];
			r(id + "_helmet", de + m[4], en + " " + m[8], 1, "MMM|M M", k).worte("helm", "kappe", "helmet");
			r(id + "_chestplate", de + m[5], en + " " + m[9], 1, "M M|MMM|MMM", k).worte("brustpanzer", "brustplatte", "harnisch", "chestplate", "ruestung");
			r(id + "_leggings", de + m[6], en + " " + m[10], 1, "MMM|M M|M M", k).worte("hose", "beinschutz", "leggings");
			r(id + "_boots", de + m[7], en + " " + m[11], 1, "M M|M M", k).worte("stiefel", "schuhe", "boots");
		}
		r("turtle_helmet", "Schildkrötenpanzer", "Turtle Shell", 1, "SSS|S S", "S=turtle_scute").worte("schildkroetenhelm");
		r("bow", "Bogen", "Bow", 1, " ST|S T| ST", "S=stick,T=string");
		r("crossbow", "Armbrust", "Crossbow", 1, "SIS|THT| S ", "S=stick,I=iron_ingot,T=string,H=tripwire_hook");
		r("arrow", "Pfeil", "Arrow", 4, "F|S|E", "F=flint,S=stick,E=feather").worte("pfeile");
		r("spectral_arrow", "Spektralpfeil", "Spectral Arrow", 2, " G |GAG| G ", "G=glowstone_dust,A=arrow").worte("leuchtpfeil");
		r("shield", "Schild", "Shield", 1, "PIP|PPP| P ", "P=#planks,I=iron_ingot");
		r("mace", "Streitkolben", "Mace", 1, "H|B", "H=heavy_core,B=breeze_rod").worte("mace", "keule")
				.hinweis("Je tiefer du fällst, bevor du triffst, desto stärker der Schlag – und dein Sturz wird abgefangen.",
						"The further you fall before you hit, the stronger the smash – and your fall is cancelled.");
		s("wind_charge", "Windkugel", "Wind Charge", 4, "breeze_rod").worte("windladung");
		r("fishing_rod", "Angel", "Fishing Rod", 1, "  S| ST|S T", "S=stick,T=string").worte("angelrute", "rod");
		r("carrot_on_a_stick", "Karottenrute", "Carrot on a Stick", 1, "R | C", "R=fishing_rod,C=carrot");
		r("shears", "Schere", "Shears", 1, " I|I ", "I=iron_ingot");
		s("flint_and_steel", "Feuerzeug", "Flint and Steel", 1, "iron_ingot flint").worte("feuer", "anzuender");
		r("bucket", "Eimer", "Bucket", 1, "I I| I ", "I=iron_ingot").worte("wassereimer", "lavaeimer");
		r("compass", "Kompass", "Compass", 1, " I |IRI| I ", "I=iron_ingot,R=redstone");
		r("recovery_compass", "Bergungskompass", "Recovery Compass", 1, "EEE|ECE|EEE", "E=echo_shard,C=compass");
		r("clock", "Uhr", "Clock", 1, " G |GRG| G ", "G=gold_ingot,R=redstone");
		r("map", "Leere Karte", "Empty Map", 1, "PPP|PCP|PPP", "P=paper,C=compass").worte("karte", "landkarte");
		r("spyglass", "Fernrohr", "Spyglass", 1, "A|C|C", "A=amethyst_shard,C=copper_ingot").worte("fernglas");
		r("brush", "Pinsel", "Brush", 1, "F|C|S", "F=feather,C=copper_ingot,S=stick");
		r("copper_torch", "Kupferfackel", "Copper Torch", 4, "N|C|S", "N=copper_nugget,C=#coals,S=stick");

		// ---------------------------------------------------------- Arbeitsblöcke
		gruppe = "Arbeitsblock";
		r("enchanting_table", "Zaubertisch", "Enchanting Table", 1, " B |DOD|OOO", "B=book,D=diamond,O=obsidian")
				.worte("verzauberungstisch", "enchantment table");
		r("anvil", "Amboss", "Anvil", 1, "BBB| I |III", "B=iron_block,I=iron_ingot").hinweis("Damit reparierst und benennst du Sachen.", "Use it to repair and rename items.");
		r("grindstone", "Schleifstein", "Grindstone", 1, "STS|P P", "S=stick,T=stone_slab,P=#planks");
		r("smithing_table", "Schmiedetisch", "Smithing Table", 1, "II|PP|PP", "I=iron_ingot,P=#planks")
				.hinweis("Hier machst du aus Diamant-Sachen Netherit (Diamant-Ding + Netheritbarren + Netherit-Aufwertung).",
						"Here you turn diamond gear into netherite (diamond item + netherite ingot + netherite upgrade).");
		r("fletching_table", "Bognertisch", "Fletching Table", 1, "FF|PP|PP", "F=flint,P=#planks");
		r("cartography_table", "Kartentisch", "Cartography Table", 1, "PP|LL|LL", "P=paper,L=#planks");
		r("loom", "Webstuhl", "Loom", 1, "SS|PP", "S=string,P=#planks");
		r("stonecutter", "Steinsäge", "Stonecutter", 1, " I |SSS", "I=iron_ingot,S=stone");
		r("brewing_stand", "Braustand", "Brewing Stand", 1, " B |CCC", "B=blaze_rod,C=#stone_crafting");
		r("cauldron", "Kessel", "Cauldron", 1, "I I|I I|III", "I=iron_ingot");
		r("composter", "Komposter", "Composter", 1, "S S|S S|SSS", "S=#wooden_slabs");
		r("beehive", "Bienenstock", "Beehive", 1, "PPP|HHH|PPP", "P=#planks,H=honeycomb");
		r("lectern", "Lesepult", "Lectern", 1, "SSS| B | S ", "S=#wooden_slabs,B=bookshelf");
		r("jukebox", "Plattenspieler", "Jukebox", 1, "PPP|PDP|PPP", "P=#planks,D=diamond").worte("musik");
		r("note_block", "Notenblock", "Note Block", 1, "PPP|PRP|PPP", "P=#planks,R=redstone");
		r("beacon", "Leuchtfeuer", "Beacon", 1, "GGG|GNG|OOO", "G=glass,N=nether_star,O=obsidian");
		r("conduit", "Aquisator", "Conduit", 1, "NNN|NHN|NNN", "N=nautilus_shell,H=heart_of_the_sea");
		r("end_crystal", "Endkristall", "End Crystal", 1, "GGG|GEG|GTG", "G=glass,E=ender_eye,T=ghast_tear").worte("kristall");
		s("ender_eye", "Enderauge", "Eye of Ender", 1, "ender_pearl blaze_powder").worte("eye of ender");
		s("blaze_powder", "Lohenstaub", "Blaze Powder", 2, "blaze_rod");
		r("ender_chest", "Endertruhe", "Ender Chest", 1, "OOO|OEO|OOO", "O=obsidian,E=ender_eye");
		r("respawn_anchor", "Seelenanker", "Respawn Anchor", 1, "CCC|GGG|CCC", "C=crying_obsidian,G=glowstone");
		r("lodestone", "Leitstein", "Lodestone", 1, "SSS|SIS|SSS", "S=chiseled_stone_bricks,I=iron_ingot");
		r("crafter", "Werkmeister", "Crafter", 1, "III|ICI|RDR", "I=iron_ingot,C=crafting_table,R=redstone,D=dropper").worte("auto crafter");
		r("calibrated_sculk_sensor", "Kalibrierter Sculk-Sensor", "Calibrated Sculk Sensor", 1, " A |ASA", "A=amethyst_shard,S=sculk_sensor");
		s("netherite_ingot", "Netheritbarren", "Netherite Ingot", 1, "4*netherite_scrap 4*gold_ingot").worte("netherit", "netherite");

		// ---------------------------------------------------------- Redstone und Schienen
		gruppe = "Redstone";
		r("redstone_torch", "Redstone-Fackel", "Redstone Torch", 1, "R|S", "R=redstone,S=stick");
		r("repeater", "Redstone-Verstärker", "Redstone Repeater", 1, "TRT|SSS", "T=redstone_torch,R=redstone,S=stone").worte("verstaerker", "repeater");
		r("comparator", "Redstone-Komparator", "Redstone Comparator", 1, " T |TQT|SSS", "T=redstone_torch,Q=quartz,S=stone").worte("komparator", "vergleicher");
		r("piston", "Kolben", "Piston", 1, "PPP|CIC|CRC", "P=#planks,C=#stone_crafting,I=iron_ingot,R=redstone");
		r("sticky_piston", "Klebriger Kolben", "Sticky Piston", 1, "S|P", "S=slime_ball,P=piston");
		r("observer", "Beobachter", "Observer", 1, "CCC|RRQ|CCC", "C=cobblestone,R=redstone,Q=quartz");
		r("dispenser", "Werfer", "Dispenser", 1, "CCC|CBC|CRC", "C=cobblestone,B=bow,R=redstone");
		r("dropper", "Spender", "Dropper", 1, "CCC|C C|CRC", "C=cobblestone,R=redstone");
		r("hopper", "Trichter", "Hopper", 1, "I I|ICI| I ", "I=iron_ingot,C=chest");
		r("lever", "Hebel", "Lever", 1, "S|C", "S=stick,C=cobblestone");
		r("tripwire_hook", "Haken", "Tripwire Hook", 2, "I|S|P", "I=iron_ingot,S=stick,P=#planks").worte("stolperdraht");
		r("daylight_detector", "Tageslichtsensor", "Daylight Detector", 1, "GGG|QQQ|SSS", "G=glass,Q=quartz,S=#wooden_slabs");
		r("target", "Zielscheibe", "Target", 1, " R |RHR| R ", "R=redstone,H=hay_block");
		r("redstone_lamp", "Redstone-Lampe", "Redstone Lamp", 1, " R |RGR| R ", "R=redstone,G=glowstone").worte("lampe");
		r("lightning_rod", "Blitzableiter", "Lightning Rod", 1, "C|C|C", "C=copper_ingot");
		r("copper_bulb", "Kupferleuchte", "Copper Bulb", 4, " C |CBC| R ", "C=copper_block,B=blaze_rod,R=redstone");
		r("stone_pressure_plate", "Steindruckplatte", "Stone Pressure Plate", 1, "SS", "S=stone");
		r("light_weighted_pressure_plate", "Wägeplatte (leicht)", "Light Weighted Pressure Plate", 1, "GG", "G=gold_ingot").worte("goldene druckplatte");
		r("heavy_weighted_pressure_plate", "Wägeplatte (schwer)", "Heavy Weighted Pressure Plate", 1, "II", "I=iron_ingot").worte("eiserne druckplatte");
		s("stone_button", "Steinknopf", "Stone Button", 1, "stone").worte("knopf");
		r("iron_door", "Eisentür", "Iron Door", 3, "II|II|II", "I=iron_ingot");
		r("iron_trapdoor", "Eisenfalltür", "Iron Trapdoor", 1, "II|II", "I=iron_ingot");
		r("copper_door", "Kupfertür", "Copper Door", 3, "CC|CC|CC", "C=copper_ingot");
		r("copper_trapdoor", "Kupferfalltür", "Copper Trapdoor", 1, "CC|CC", "C=copper_ingot");
		r("tnt", "TNT", "TNT", 1, "GSG|SGS|GSG", "G=gunpowder,S=sand").worte("sprengstoff", "dynamit");
		r("rail", "Schiene", "Rail", 16, "I I|ISI|I I", "I=iron_ingot,S=stick").worte("schienen", "gleis");
		r("powered_rail", "Antriebsschiene", "Powered Rail", 6, "G G|GSG|GRG", "G=gold_ingot,S=stick,R=redstone").worte("boosterschiene");
		r("detector_rail", "Sensorschiene", "Detector Rail", 6, "I I|IPI|IRI", "I=iron_ingot,P=stone_pressure_plate,R=redstone");
		r("activator_rail", "Aktivierungsschiene", "Activator Rail", 6, "ISI|ITI|ISI", "I=iron_ingot,S=stick,T=redstone_torch");
		r("minecart", "Lore", "Minecart", 1, "I I|III", "I=iron_ingot").worte("minecart", "wagen");
		s("chest_minecart", "Güterlore", "Minecart with Chest", 1, "chest minecart");
		s("furnace_minecart", "Antriebslore", "Minecart with Furnace", 1, "furnace minecart");
		s("hopper_minecart", "Trichterlore", "Minecart with Hopper", 1, "hopper minecart");
		s("tnt_minecart", "TNT-Lore", "Minecart with TNT", 1, "tnt minecart");

		// ---------------------------------------------------------- Essen
		gruppe = "Essen";
		r("bread", "Brot", "Bread", 1, "WWW", "W=wheat");
		r("cake", "Kuchen", "Cake", 1, "MMM|SES|WWW", "M=milk_bucket,S=sugar,E=egg,W=wheat").worte("torte");
		r("cookie", "Keks", "Cookie", 8, "WCW", "W=wheat,C=cocoa_beans").worte("kekse");
		s("pumpkin_pie", "Kürbiskuchen", "Pumpkin Pie", 1, "pumpkin sugar egg");
		r("golden_apple", "Goldener Apfel", "Golden Apple", 1, "GGG|GAG|GGG", "G=gold_ingot,A=apple").worte("goldapfel", "gapple");
		r("golden_carrot", "Goldene Karotte", "Golden Carrot", 1, "NNN|NCN|NNN", "N=gold_nugget,C=carrot").worte("goldkarotte");
		r("glistering_melon_slice", "Glitzernde Melonenscheibe", "Glistering Melon Slice", 1, "NNN|NMN|NNN", "N=gold_nugget,M=melon_slice");
		s("mushroom_stew", "Pilzsuppe", "Mushroom Stew", 1, "red_mushroom brown_mushroom bowl");
		s("beetroot_soup", "Rote-Bete-Suppe", "Beetroot Soup", 1, "6*beetroot bowl");
		r("hay_block", "Strohballen", "Hay Bale", 1, "WWW|WWW|WWW", "W=wheat").worte("heuballen", "stroh");
		r("melon", "Melone", "Melon", 1, "MMM|MMM|MMM", "M=melon_slice");
		r("honey_block", "Honigblock", "Honey Block", 1, "HH|HH", "H=honey_bottle");
		r("honeycomb_block", "Honigwabenblock", "Honeycomb Block", 1, "HH|HH", "H=honeycomb");
		r("slime_block", "Schleimblock", "Slime Block", 1, "SSS|SSS|SSS", "S=slime_ball");
		r("dried_kelp_block", "Getrockneter Seetangblock", "Dried Kelp Block", 1, "KKK|KKK|KKK", "K=dried_kelp");
		s("fire_charge", "Feuerkugel", "Fire Charge", 3, "gunpowder blaze_powder #coals");
		s("firework_rocket", "Feuerwerksrakete", "Firework Rocket", 3, "paper gunpowder").worte("rakete", "feuerwerk")
				.hinweis("Mit 2 oder 3 Schwarzpulver fliegt sie weiter – gut für Elytra.", "With 2 or 3 gunpowder it flies further – good for elytra.");

		// ---------------------------------------------------------- Rohstoff-Blöcke (9 → 1 und zurück)
		gruppe = "Rohstoffblock";
		Object[][] bloecke = {   // {Block, Name de, en, Teil, Teil de, en, Anzahl}
				{"iron_block", "Eisenblock", "Block of Iron", "iron_ingot", 9},
				{"gold_block", "Goldblock", "Block of Gold", "gold_ingot", 9},
				{"diamond_block", "Diamantblock", "Block of Diamond", "diamond", 9},
				{"emerald_block", "Smaragdblock", "Block of Emerald", "emerald", 9},
				{"lapis_block", "Lapislazuliblock", "Block of Lapis Lazuli", "lapis_lazuli", 9},
				{"redstone_block", "Redstone-Block", "Block of Redstone", "redstone", 9},
				{"coal_block", "Kohleblock", "Block of Coal", "coal", 9},
				{"copper_block", "Kupferblock", "Block of Copper", "copper_ingot", 9},
				{"netherite_block", "Netheritblock", "Block of Netherite", "netherite_ingot", 9},
				{"raw_iron_block", "Roheisenblock", "Block of Raw Iron", "raw_iron", 9},
				{"raw_gold_block", "Rohgoldblock", "Block of Raw Gold", "raw_gold", 9},
				{"raw_copper_block", "Rohkupferblock", "Block of Raw Copper", "raw_copper", 9},
		};
		ding("raw_iron", "Roheisen", "Raw Iron", "Roheisen", "Raw iron", 0xD8AF93);
		ding("raw_gold", "Rohgold", "Raw Gold", "Rohgold", "Raw gold", 0xE8B93A);
		ding("raw_copper", "Rohkupfer", "Raw Copper", "Rohkupf.", "Raw cu", 0xC86A45);
		for (Object[] b : bloecke) {
			String teil = (String) b[3];
			r((String) b[0], (String) b[1], (String) b[2], 1, "XXX|XXX|XXX", "X=" + teil)
					.hinweis("Zurück: den Block allein ins Feld legen – ergibt 9.", "Back: put the block in the grid on its own – gives 9.");
		}
		r("iron_ingot", "Eisenbarren", "Iron Ingot", 1, "NNN|NNN|NNN", "N=iron_nugget")
				.hinweis("Eisenbarren bekommst du meist durch Schmelzen von Roheisen im Ofen.", "You usually get iron ingots by smelting raw iron in a furnace.");
		r("gold_ingot", "Goldbarren", "Gold Ingot", 1, "NNN|NNN|NNN", "N=gold_nugget");
		r("copper_ingot", "Kupferbarren", "Copper Ingot", 1, "NNN|NNN|NNN", "N=copper_nugget");
		s("iron_nugget", "Eisenklumpen", "Iron Nugget", 9, "iron_ingot").worte("eisennugget");
		s("gold_nugget", "Goldklumpen", "Gold Nugget", 9, "gold_ingot").worte("goldnugget");
		s("copper_nugget", "Kupferklumpen", "Copper Nugget", 9, "copper_ingot").worte("kupfernugget");

		// ---------------------------------------------------------- Holz (12 Holzarten)
		gruppe = "Holz";
		Object[][] holz = {   // {Id, Wortanfang de, Wortstamm für Suche, en, Kurz, Farbe, Stamm-Id, entrindet, Boot?, Holz-Block de}
				{"oak", "Eichenholz", "eichen", "Oak", "Eiche", 0xB8894F, "oak_log", "stripped_oak_log", true, "Eichenholz"},
				{"spruce", "Fichtenholz", "fichten", "Spruce", "Fichte", 0x7A5A34, "spruce_log", "stripped_spruce_log", true, "Fichtenholz"},
				{"birch", "Birkenholz", "birken", "Birch", "Birke", 0xD7C185, "birch_log", "stripped_birch_log", true, "Birkenholz"},
				{"jungle", "Tropenholz", "tropen", "Jungle", "Tropen", 0xB88764, "jungle_log", "stripped_jungle_log", true, "Tropenholz"},
				{"acacia", "Akazienholz", "akazien", "Acacia", "Akazie", 0xBA6337, "acacia_log", "stripped_acacia_log", true, "Akazienholz"},
				{"dark_oak", "Schwarzeichenholz", "schwarzeichen", "Dark Oak", "Schw.Eiche", 0x4F3218, "dark_oak_log", "stripped_dark_oak_log", true, "Schwarzeichenholz"},
				{"mangrove", "Mangrovenholz", "mangroven", "Mangrove", "Mangrove", 0x773634, "mangrove_log", "stripped_mangrove_log", true, "Mangrovenholz"},
				{"cherry", "Kirschholz", "kirsch", "Cherry", "Kirsche", 0xE3B3AD, "cherry_log", "stripped_cherry_log", true, "Kirschholz"},
				{"pale_oak", "Blasseichenholz", "blasseichen", "Pale Oak", "Blasseiche", 0xE8DCD5, "pale_oak_log", "stripped_pale_oak_log", true, "Blasseichenholz"},
				{"bamboo", "Bambus", "bambus", "Bamboo", "Bambus", 0xC9B256, "bamboo_block", "stripped_bamboo_block", true, null},
				{"crimson", "Karmesin", "karmesin", "Crimson", "Karmesin", 0x7E3A56, "crimson_stem", "stripped_crimson_stem", false, "Karmesinhyphen"},
				{"warped", "Wirr", "wirr", "Warped", "Wirr", 0x398382, "warped_stem", "stripped_warped_stem", false, "Wirrhyphen"},
		};
		for (Object[] h : holz) {
			String id = (String) h[0], de = (String) h[1], stamm = (String) h[2], en = (String) h[3], kurz = (String) h[4];
			int farbe = (Integer) h[5];
			String log = (String) h[6], ent = (String) h[7];
			boolean eiche = id.equals("oak");
			String planks = id + "_planks";
			boolean nether = id.equals("crimson") || id.equals("warped");
			String stammName = id.equals("bamboo") ? "Bambusblock" : nether ? de + "stiel" : de.replace("holz", "stamm");
			String stammEn = id.equals("bamboo") ? "Block of Bamboo" : en + (nether ? " Stem" : " Log");
			ding(log, stammName, stammEn, kurz + " St.", en + " log", darker(farbe));
			ding(ent, "Entrindeter " + stammName, "Stripped " + stammEn, kurz + " Ent.", "Strip " + en, farbe);
			ding(planks, de + "bretter", en + " Planks", kurz, en, farbe);
			s(planks, de + "bretter", en + " Planks", id.equals("bamboo") ? 2 : 4, log)
					.worte(stamm + "bretter", stamm + " bretter", stamm + "holzbretter", eiche ? "bretter" : "", eiche ? "holzbretter" : "", eiche ? "planks" : "")
					.hinweis("Jeder Stamm geht – die Bretter haben dann seine Farbe.", "Any log works – the planks get its color.");
			String P = "P=" + planks;
			r(id + "_stairs", de + "treppe", en + " Stairs", 4, "P  |PP |PPP", P).worte(stamm + "treppe", eiche ? "holztreppe" : "", eiche ? "treppe" : "", eiche ? "stairs" : "");
			r(id + "_slab", de + "stufe", en + " Slab", 6, "PPP", P).worte(stamm + "stufe", eiche ? "holzstufe" : "", eiche ? "stufe" : "", eiche ? "halbe stufe" : "", eiche ? "slab" : "");
			r(id + "_fence", de + "zaun", en + " Fence", 3, "PSP|PSP", P + ",S=stick").worte(stamm + "zaun", eiche ? "holzzaun" : "", eiche ? "zaun" : "", eiche ? "fence" : "");
			r(id + "_fence_gate", de + "zauntor", en + " Fence Gate", 1, "SPS|SPS", P + ",S=stick").worte(stamm + "zauntor", eiche ? "zauntor" : "", eiche ? "gartentor" : "", eiche ? "tor" : "");
			r(id + "_door", de + "tür", en + " Door", 3, "PP|PP|PP", P).worte(stamm + "tuer", eiche ? "holztuer" : "", eiche ? "tuer" : "", eiche ? "door" : "");
			r(id + "_trapdoor", de + "falltür", en + " Trapdoor", 2, "PPP|PPP", P).worte(stamm + "falltuer", eiche ? "falltuer" : "", eiche ? "trapdoor" : "");
			s(id + "_button", de + "knopf", en + " Button", 1, planks).worte(stamm + "knopf", eiche ? "holzknopf" : "");
			r(id + "_pressure_plate", de + "druckplatte", en + " Pressure Plate", 1, "PP", P).worte(stamm + "druckplatte", eiche ? "druckplatte" : "", eiche ? "holzdruckplatte" : "");
			r(id + "_sign", de + "schild", en + " Sign", 3, "PPP|PPP| S ", P + ",S=stick").worte(stamm + "schild", eiche ? "holzschild" : "", eiche ? "sign" : "", eiche ? "schild zum schreiben" : "")
					.hinweis("Nicht verwechseln mit dem Schild zum Blocken (Kampfschild).", "Not the shield for blocking – that's the shield recipe.");
			r(id + "_hanging_sign", de + "hängeschild", en + " Hanging Sign", 6, "C C|LLL|LLL", "C=iron_chain,L=" + ent).worte(stamm + "haengeschild", eiche ? "haengeschild" : "");
			if (h[9] != null) {
				r(id.equals("crimson") ? "crimson_hyphae" : id.equals("warped") ? "warped_hyphae" : id + "_wood", (String) h[9],
						id.equals("crimson") || id.equals("warped") ? en + " Hyphae" : en + " Wood", 3, "LL|LL", "L=" + log)
						.worte(eiche ? "holzblock" : "", stamm + "holzblock");
			}
			if ((Boolean) h[8]) {
				boolean floss = id.equals("bamboo");
				String boot = floss ? "bamboo_raft" : id + "_boat";
				r(boot, floss ? "Bambusfloß" : de + "boot", floss ? "Bamboo Raft" : en + " Boat", 1, "P P|PPP", P)
						.worte(stamm + "boot", floss ? "floss" : "", eiche ? "boot" : "", eiche ? "holzboot" : "", eiche ? "boat" : "");
				s(floss ? "bamboo_chest_raft" : id + "_chest_boat", floss ? "Bambusfloß mit Truhe" : de + "boot mit Truhe",
						floss ? "Bamboo Raft with Chest" : en + " Boat with Chest", 1, boot + " chest")
						.worte(stamm + "truhenboot", eiche ? "truhenboot" : "", eiche ? "boot mit truhe" : "");
			}
		}

		// ---------------------------------------------------------- Farben (16)
		gruppe = "Farbe";
		Object[][] farben = {   // {Id, Adjektiv de, en, Farbe}
				{"white", "Weiß", "White", 0xF2F2F2}, {"orange", "Orange", "Orange", 0xF07A1A},
				{"magenta", "Magenta", "Magenta", 0xC24AC0}, {"light_blue", "Hellblau", "Light Blue", 0x3AAFDA},
				{"yellow", "Gelb", "Yellow", 0xF8C628}, {"lime", "Hellgrün", "Lime", 0x70B91A},
				{"pink", "Rosa", "Pink", 0xEE8DAC}, {"gray", "Grau", "Gray", 0x3E4447},
				{"light_gray", "Hellgrau", "Light Gray", 0x8E8E87}, {"cyan", "Türkis", "Cyan", 0x158991},
				{"purple", "Violett", "Purple", 0x7A2AAD}, {"blue", "Blau", "Blue", 0x35399D},
				{"brown", "Braun", "Brown", 0x724728}, {"green", "Grün", "Green", 0x546D1B},
				{"red", "Rot", "Red", 0xA12722}, {"black", "Schwarz", "Black", 0x141519},
		};
		// Farbstoffe
		String[][] farbstoff = {   // {Farbe, Zutaten (formlos), Hinweis de, en}
				{"white", "bone_meal", "Auch aus Maiglöckchen.", "Also from lily of the valley."},
				{"orange", "red_dye yellow_dye", "Auch aus orangefarbener Tulpe oder Fackellilie.", "Also from an orange tulip or torchflower."},
				{"magenta", "purple_dye pink_dye", "Auch aus Zierlauch oder Flieder.", "Also from allium or lilac."},
				{"light_blue", "blue_dye white_dye", "Auch aus blauer Orchidee.", "Also from a blue orchid."},
				{"yellow", "dandelion", "Auch aus Sonnenblume.", "Also from a sunflower."},
				{"lime", "green_dye white_dye", "Auch Seegurke im Ofen schmelzen.", "Also by smelting a sea pickle."},
				{"pink", "red_dye white_dye", "Auch aus rosa Tulpe, Pfingstrose oder Rosa Blütenblättern.", "Also from a pink tulip, peony or pink petals."},
				{"gray", "black_dye white_dye", "", ""},
				{"light_gray", "gray_dye white_dye", "Auch aus Porzellansternchen, Margerite oder weißer Tulpe.", "Also from azure bluet, oxeye daisy or white tulip."},
				{"cyan", "blue_dye green_dye", "Auch aus Kannenpflanze.", "Also from a pitcher plant."},
				{"purple", "red_dye blue_dye", "", ""},
				{"blue", "lapis_lazuli", "Auch aus Kornblume.", "Also from a cornflower."},
				{"brown", "cocoa_beans", "", ""},
				{"green", "", "Grünen Farbstoff bekommst du nur durch Schmelzen von Kaktus im Ofen.", "Green dye only comes from smelting cactus in a furnace."},
				{"red", "poppy", "Auch aus Rosenstrauch, roter Tulpe oder Roter Bete.", "Also from a rose bush, red tulip or beetroot."},
				{"black", "ink_sac", "Auch aus Wither-Rose.", "Also from a wither rose."},
		};
		for (Object[] f : farben) {
			String id = (String) f[0], adj = (String) f[1], en = (String) f[2];
			int c = (Integer) f[3];
			String a = norm(adj);
			ding(id + "_dye", "Farbstoff (" + adj + ")", en + " Dye", adj.length() <= 9 ? adj : adj.substring(0, 8) + ".", en, c);
			ding(id + "_wool", "Wolle (" + adj + ")", en + " Wool", "Wolle", "Wool", c);
			ding(id + "_stained_glass", "Glas (" + adj + ")", en + " Stained Glass", "Glas", "Glass", c);
			ding(id + "_terracotta", "Keramik (" + adj + ")", en + " Terracotta", "Keramik", "Terrac.", c);
			ding(id + "_concrete_powder", "Trockenbeton (" + adj + ")", en + " Concrete Powder", "Trockenb.", "Concr. p.", c);
		}
		ding("terracotta", "Keramik", "Terracotta", "Keramik", "Terrac.", 0x985E43);
		ding("shulker_box", "Shulker-Kiste", "Shulker Box", "Shulker", "Shulker", 0x8A5E8A);
		for (String[] fs : farbstoff) {
			Object[] f = null;
			for (Object[] x : farben) if (x[0].equals(fs[0])) f = x;
			String adj = (String) f[1], en = (String) f[2], a = norm(adj);
			if (fs[1].isEmpty()) continue;   // nur durch Schmelzen (Grün) – das erklärt HelferWissen
			s(fs[0] + "_dye", "Farbstoff (" + adj + ")", en + " Dye", fs[1].contains(" ") ? 2 : (fs[0].equals("white") ? 1 : 1), fs[1])
					.worte(a + " farbstoff", a + "er farbstoff", a + "e farbe", "farbstoff " + a, a + " farbe", en.toLowerCase() + " dye")
					.hinweis(fs[2], fs[3]);
		}
		for (Object[] f : farben) {
			String id = (String) f[0], adj = (String) f[1], en = (String) f[2], a = norm(adj);
			String dye = id + "_dye";
			boolean weiss = id.equals("white");
			if (!weiss) {
				s(id + "_wool", "Wolle (" + adj + ")", en + " Wool", 1, "white_wool " + dye).worte(a + "e wolle", "wolle " + a, a + " wolle", en.toLowerCase() + " wool", "wolle faerben")
						.hinweis("Seit 1.20 kannst du jede Wolle umfärben.", "Since 1.20 you can re-dye any wool.");
			}
			r(id + "_bed", "Bett (" + adj + ")", en + " Bed", 1, "WWW|PPP", "W=" + id + "_wool,P=#planks")
					.worte(a + "es bett", "bett " + a, a + " bett", en.toLowerCase() + " bed", weiss ? "bett" : "", weiss ? "bed" : "", weiss ? "schlafen" : "")
					.hinweis(weiss ? "Gleiche Wollfarbe nehmen. Schlafen überspringt die Nacht und setzt deinen Spawnpunkt." : "Gleiche Wollfarbe nehmen.",
							weiss ? "Use one wool color. Sleeping skips the night and sets your spawn point." : "Use one wool color.");
			r(id + "_carpet", "Teppich (" + adj + ")", en + " Carpet", 3, "WW", "W=" + id + "_wool").worte(a + "er teppich", "teppich " + a, a + " teppich", weiss ? "teppich" : "", weiss ? "carpet" : "");
			r(id + "_stained_glass", "Glas (" + adj + ")", en + " Stained Glass", 8, "GGG|GDG|GGG", "G=glass,D=" + dye)
					.worte(a + "es glas", "glas " + a, a + " glas", "gefaerbtes glas", weiss ? "buntglas" : "", "stained glass");
			r(id + "_stained_glass_pane", "Glasscheibe (" + adj + ")", en + " Stained Glass Pane", 16, "GGG|GGG", "G=" + id + "_stained_glass")
					.worte(a + "e glasscheibe", "glasscheibe " + a, "bunte glasscheibe");
			r(id + "_terracotta", "Keramik (" + adj + ")", en + " Terracotta", 8, "TTT|TDT|TTT", "T=terracotta,D=" + dye)
					.worte(a + "e keramik", "keramik " + a, "terrakotta " + a, "gefaerbte keramik");
			s(id + "_concrete_powder", "Trockenbeton (" + adj + ")", en + " Concrete Powder", 8, dye + " 4*sand 4*gravel")
					.worte(a + "er beton", "beton " + a, a + " beton", "trockenbeton " + a, weiss ? "beton" : "", weiss ? "concrete" : "")
					.hinweis("Mit Wasser in Berührung wird daraus fester Beton.", "Touching water turns it into solid concrete.");
			s(id + "_candle", "Kerze (" + adj + ")", en + " Candle", 1, "candle " + dye).worte(a + "e kerze", "kerze " + a);
			r(id + "_banner", "Banner (" + adj + ")", en + " Banner", 1, "WWW|WWW| S ", "W=" + id + "_wool,S=stick")
					.worte(a + "es banner", "banner " + a, weiss ? "banner" : "", weiss ? "flagge" : "", weiss ? "fahne" : "");
			s(id + "_shulker_box", "Shulker-Kiste (" + adj + ")", en + " Shulker Box", 1, "shulker_box " + dye).worte(a + "e shulker kiste", "shulker " + a);
		}

		// ---------------------------------------------------------- Stein und Ziegel
		gruppe = "Stein";
		Object[][] sorten = {   // {Basis-Id, Name de, en, Treppe/Stufe/Mauer-Id-Anfang, Treppe?, Stufe?, Mauer?}
				{"cobblestone", "Bruchstein", "Cobblestone", "cobblestone", true, true, true},
				{"stone", "Stein", "Stone", "stone", true, true, false},
				{"smooth_stone", "Glatter Stein", "Smooth Stone", "smooth_stone", false, true, false},
				{"stone_bricks", "Steinziegel", "Stone Bricks", "stone_brick", true, true, true},
				{"mossy_cobblestone", "Bemooster Bruchstein", "Mossy Cobblestone", "mossy_cobblestone", true, true, true},
				{"mossy_stone_bricks", "Bemooste Steinziegel", "Mossy Stone Bricks", "mossy_stone_brick", true, true, true},
				{"granite", "Granit", "Granite", "granite", true, true, true},
				{"polished_granite", "Polierter Granit", "Polished Granite", "polished_granite", true, true, false},
				{"diorite", "Diorit", "Diorite", "diorite", true, true, true},
				{"polished_diorite", "Polierter Diorit", "Polished Diorite", "polished_diorite", true, true, false},
				{"andesite", "Andesit", "Andesite", "andesite", true, true, true},
				{"polished_andesite", "Polierter Andesit", "Polished Andesite", "polished_andesite", true, true, false},
				{"cobbled_deepslate", "Tiefenschiefer-Bruchstein", "Cobbled Deepslate", "cobbled_deepslate", true, true, true},
				{"polished_deepslate", "Polierter Tiefenschiefer", "Polished Deepslate", "polished_deepslate", true, true, true},
				{"deepslate_bricks", "Tiefenschieferziegel", "Deepslate Bricks", "deepslate_brick", true, true, true},
				{"deepslate_tiles", "Tiefenschieferfliesen", "Deepslate Tiles", "deepslate_tile", true, true, true},
				{"bricks", "Ziegelsteine", "Bricks", "brick", true, true, true},
				{"mud_bricks", "Schlammziegel", "Mud Bricks", "mud_brick", true, true, true},
				{"sandstone", "Sandstein", "Sandstone", "sandstone", true, true, true},
				{"smooth_sandstone", "Glatter Sandstein", "Smooth Sandstone", "smooth_sandstone", true, true, false},
				{"cut_sandstone", "Geschnittener Sandstein", "Cut Sandstone", "cut_sandstone", false, true, false},
				{"red_sandstone", "Roter Sandstein", "Red Sandstone", "red_sandstone", true, true, true},
				{"nether_bricks", "Netherziegel", "Nether Bricks", "nether_brick", true, true, true},
				{"red_nether_bricks", "Rote Netherziegel", "Red Nether Bricks", "red_nether_brick", true, true, true},
				{"blackstone", "Schwarzstein", "Blackstone", "blackstone", true, true, true},
				{"polished_blackstone", "Polierter Schwarzstein", "Polished Blackstone", "polished_blackstone", true, true, true},
				{"polished_blackstone_bricks", "Polierte Schwarzsteinziegel", "Polished Blackstone Bricks", "polished_blackstone_brick", true, true, true},
				{"end_stone_bricks", "Endsteinziegel", "End Stone Bricks", "end_stone_brick", true, true, true},
				{"purpur_block", "Purpurblock", "Purpur Block", "purpur", true, true, false},
				{"quartz_block", "Quarzblock", "Block of Quartz", "quartz", true, true, false},
				{"prismarine", "Prismarin", "Prismarine", "prismarine", true, true, true},
				{"prismarine_bricks", "Prismarinziegel", "Prismarine Bricks", "prismarine_brick", true, true, false},
				{"dark_prismarine", "Dunkler Prismarin", "Dark Prismarine", "dark_prismarine", true, true, false},
				{"tuff", "Tuffstein", "Tuff", "tuff", true, true, true},
				{"polished_tuff", "Polierter Tuffstein", "Polished Tuff", "polished_tuff", true, true, true},
				{"tuff_bricks", "Tuffsteinziegel", "Tuff Bricks", "tuff_brick", true, true, true},
				{"resin_bricks", "Harzziegel", "Resin Bricks", "resin_brick", true, true, true},
				{"cut_copper", "Geschnittenes Kupfer", "Cut Copper", "cut_copper", true, true, false},
		};
		ding("cobblestone", "Bruchstein", "Cobblestone", "Bruchst.", "Cobble", 0x8C939C);
		ding("end_stone", "Endstein", "End Stone", "Endstein", "End st.", 0xDBDE9E);
		ding("blackstone", "Schwarzstein", "Blackstone", "Schwarzst.", "Blackst.", 0x2E2A31);
		ding("cobbled_deepslate", "Tiefenschiefer-Bruchstein", "Cobbled Deepslate", "Tiefensch.", "Deepslate", 0x4D4D50);
		ding("granite", "Granit", "Granite", "Granit", "Granite", 0x9A6A57);
		ding("diorite", "Diorit", "Diorite", "Diorit", "Diorite", 0xBDBDBD);
		ding("andesite", "Andesit", "Andesite", "Andesit", "Andesite", 0x888888);
		ding("tuff", "Tuffstein", "Tuff", "Tuff", "Tuff", 0x6C6D66);
		// Grundblöcke dieser Sorten
		r("stone_bricks", "Steinziegel", "Stone Bricks", 4, "SS|SS", "S=stone").worte("steinziegel", "stonebricks");
		s("mossy_cobblestone", "Bemooster Bruchstein", "Mossy Cobblestone", 1, "cobblestone vine").worte("moosiger bruchstein")
				.hinweis("Statt Ranken geht auch ein Moosblock.", "A moss block works instead of vines.");
		s("mossy_stone_bricks", "Bemooste Steinziegel", "Mossy Stone Bricks", 1, "stone_bricks vine").hinweis("Statt Ranken geht auch ein Moosblock.", "A moss block works instead of vines.");
		r("chiseled_stone_bricks", "Gemeißelte Steinziegel", "Chiseled Stone Bricks", 1, "S|S", "S=stone_brick_slab");
		r("diorite", "Diorit", "Diorite", 2, "CQ|QC", "C=cobblestone,Q=quartz");
		s("granite", "Granit", "Granite", 1, "diorite quartz");
		s("andesite", "Andesit", "Andesite", 2, "diorite cobblestone");
		r("polished_granite", "Polierter Granit", "Polished Granite", 4, "GG|GG", "G=granite");
		r("polished_diorite", "Polierter Diorit", "Polished Diorite", 4, "GG|GG", "G=diorite");
		r("polished_andesite", "Polierter Andesit", "Polished Andesite", 4, "GG|GG", "G=andesite");
		r("polished_deepslate", "Polierter Tiefenschiefer", "Polished Deepslate", 4, "DD|DD", "D=cobbled_deepslate");
		r("deepslate_bricks", "Tiefenschieferziegel", "Deepslate Bricks", 4, "DD|DD", "D=polished_deepslate");
		r("deepslate_tiles", "Tiefenschieferfliesen", "Deepslate Tiles", 4, "DD|DD", "D=deepslate_bricks");
		r("chiseled_deepslate", "Gemeißelter Tiefenschiefer", "Chiseled Deepslate", 1, "S|S", "S=cobbled_deepslate_slab");
		r("mud_bricks", "Schlammziegel", "Mud Bricks", 4, "MM|MM", "M=packed_mud");
		r("sandstone", "Sandstein", "Sandstone", 1, "SS|SS", "S=sand");
		r("red_sandstone", "Roter Sandstein", "Red Sandstone", 1, "SS|SS", "S=red_sand");
		r("cut_sandstone", "Geschnittener Sandstein", "Cut Sandstone", 4, "SS|SS", "S=sandstone");
		r("cut_red_sandstone", "Geschnittener roter Sandstein", "Cut Red Sandstone", 4, "SS|SS", "S=red_sandstone");
		r("chiseled_sandstone", "Gemeißelter Sandstein", "Chiseled Sandstone", 1, "S|S", "S=sandstone_slab");
		r("nether_bricks", "Netherziegel", "Nether Bricks", 1, "NN|NN", "N=nether_brick").worte("netherziegelblock");
		r("red_nether_bricks", "Rote Netherziegel", "Red Nether Bricks", 1, "NW|WN", "N=nether_brick,W=nether_wart");
		r("polished_blackstone", "Polierter Schwarzstein", "Polished Blackstone", 4, "BB|BB", "B=blackstone");
		r("polished_blackstone_bricks", "Polierte Schwarzsteinziegel", "Polished Blackstone Bricks", 4, "BB|BB", "B=polished_blackstone");
		r("chiseled_polished_blackstone", "Gemeißelter polierter Schwarzstein", "Chiseled Polished Blackstone", 1, "S|S", "S=polished_blackstone_slab");
		r("end_stone_bricks", "Endsteinziegel", "End Stone Bricks", 4, "EE|EE", "E=end_stone");
		r("purpur_block", "Purpurblock", "Purpur Block", 4, "PP|PP", "P=popped_chorus_fruit");
		r("purpur_pillar", "Purpursäule", "Purpur Pillar", 1, "S|S", "S=purpur_slab");
		r("quartz_block", "Quarzblock", "Block of Quartz", 1, "QQ|QQ", "Q=quartz");
		r("quartz_pillar", "Quarzsäule", "Quartz Pillar", 2, "Q|Q", "Q=quartz_block");
		r("chiseled_quartz_block", "Gemeißelter Quarzblock", "Chiseled Quartz Block", 1, "S|S", "S=quartz_slab");
		r("quartz_bricks", "Quarzziegel", "Quartz Bricks", 4, "QQ|QQ", "Q=quartz_block");
		r("prismarine", "Prismarin", "Prismarine", 1, "SS|SS", "S=prismarine_shard");
		r("prismarine_bricks", "Prismarinziegel", "Prismarine Bricks", 1, "SSS|SSS|SSS", "S=prismarine_shard");
		r("dark_prismarine", "Dunkler Prismarin", "Dark Prismarine", 1, "SSS|SIS|SSS", "S=prismarine_shard,I=ink_sac");
		r("sea_lantern", "Seelaterne", "Sea Lantern", 1, "SCS|CCC|SCS", "S=prismarine_shard,C=prismarine_crystals");
		r("polished_tuff", "Polierter Tuffstein", "Polished Tuff", 4, "TT|TT", "T=tuff");
		r("tuff_bricks", "Tuffsteinziegel", "Tuff Bricks", 4, "TT|TT", "T=polished_tuff");
		r("chiseled_tuff", "Gemeißelter Tuffstein", "Chiseled Tuff", 1, "S|S", "S=tuff_slab");
		r("chiseled_tuff_bricks", "Gemeißelte Tuffsteinziegel", "Chiseled Tuff Bricks", 1, "S|S", "S=tuff_brick_slab");
		r("resin_bricks", "Harzziegel", "Resin Bricks", 1, "BB|BB", "B=resin_brick");
		r("chiseled_resin_bricks", "Gemeißelte Harzziegel", "Chiseled Resin Bricks", 1, "S|S", "S=resin_brick_slab");
		r("cut_copper", "Geschnittenes Kupfer", "Cut Copper", 4, "CC|CC", "C=copper_block");
		r("chiseled_copper", "Gemeißeltes Kupfer", "Chiseled Copper", 1, "S|S", "S=cut_copper_slab");
		r("copper_grate", "Kupferrost", "Copper Grate", 4, " C |C C| C ", "C=copper_block");
		for (Object[] st : sorten) {
			String basis = (String) st[0], de = (String) st[1], en = (String) st[2], id = (String) st[3];
			String nd = norm(de);
			String stamm = norm(wortstamm(de)).replace(" ", "");
			if ((Boolean) st[4]) {
				r(id + "_stairs", weiblich(de, "treppe"), en + " Stairs", 4, "S  |SS |SSS", "S=" + basis)
						.worte(nd + " treppe", stamm + "treppe", basis.equals("cobblestone") ? "steintreppe" : "", basis.equals("cobblestone") ? "treppe aus stein" : "");
			}
			if ((Boolean) st[5]) {
				r(id + "_slab", weiblich(de, "stufe"), en + " Slab", 6, "SSS", "S=" + basis)
						.worte(nd + " stufe", stamm + "stufe", basis.equals("stone") ? "steinstufe" : "");
			}
			if ((Boolean) st[6]) {
				r(id + "_wall", weiblich(de, "mauer"), en + " Wall", 6, "SSS|SSS", "S=" + basis)
						.worte(nd + " mauer", stamm + "mauer", basis.equals("cobblestone") ? "mauer" : "", basis.equals("cobblestone") ? "steinmauer" : "");
			}
		}
		// Treppen/Stufen, die oben noch nicht als Zutat benannt sind (für Gemeißeltes)
		for (Rezept x : new ArrayList<>(ALLE)) {
			if (!DINGE.containsKey(x.id)) ding(x.id, x.de, x.en, kurzVon(x.de), kurzVon(x.en), farbeVon(x));
		}
	}

	/**
	 * Name für Treppe/Stufe/Mauer (alle weiblich) mit richtiger Endung:
	 * „Polierter Granit“ → „Polierte Granittreppe“, „Quarzblock“ → „Quarztreppe“, „Ziegelsteine“ → „Ziegeltreppe“.
	 */
	private static String weiblich(String de, String nomen) {
		String[] w = de.split(" ", 2);
		if (w.length == 2) {
			String adj = w[0];
			if (adj.endsWith("er") || adj.endsWith("es")) adj = adj.substring(0, adj.length() - 1);
			if (!adj.endsWith("e")) adj = adj + "e";
			adj = adj.replace("ee", "e");
			return adj + " " + wortstamm(w[1]) + nomen;
		}
		return wortstamm(de) + nomen;
	}

	/** „Quarzblock“ → „Quarz“, „Purpurblock“ → „Purpur“, „Ziegelsteine“ → „Ziegel“, „Geschnittenes Kupfer“ → „Kupfer“. */
	private static String wortstamm(String de) {
		String[] w = de.split(" ");
		String n = w[w.length - 1];
		if (n.endsWith("block")) n = n.substring(0, n.length() - 5);
		if (n.equals("Ziegelsteine")) n = "Ziegel";
		return n;
	}

	private static String kurzVon(String n) {
		return n.length() <= 9 ? n : n.substring(0, 8) + ".";
	}

	/** Farbe eines erzeugten Gegenstands: die der häufigsten Zutat. */
	private static int farbeVon(Rezept x) {
		Map<String, Integer> anzahl = new LinkedHashMap<>();
		String haupt = null;
		for (String z : x.felder) {
			if (z.isEmpty()) continue;
			int a = anzahl.containsKey(z) ? anzahl.get(z) + 1 : 1;
			anzahl.put(z, a);
			if (haupt == null || a > anzahl.get(haupt)) haupt = z;
		}
		Object[] d = haupt == null ? null : DINGE.get(haupt);
		return d != null && d[4] != null ? (Integer) d[4] : 0x8A9BB0;
	}

	private static int darker(int c) {
		int r = (c >> 16) & 255, g = (c >> 8) & 255, b = c & 255;
		return ((r * 3 / 4) << 16) | ((g * 3 / 4) << 8) | (b * 3 / 4);
	}

	private static String norm(String s) {
		return HelferWissen.norm(s);
	}

	// ================================================================ Texte für den Helfer

	/** Erklärung in Worten: „Du brauchst: 6× Eichenholzbretter …“ */
	public static String text(Rezept r, boolean en) {
		Map<String, Integer> zutaten = new LinkedHashMap<>();
		for (String z : r.felder) {
			if (z.isEmpty()) continue;
			zutaten.put(z, zutaten.containsKey(z) ? zutaten.get(z) + 1 : 1);
		}
		StringBuilder b = new StringBuilder();
		b.append(en ? r.en : r.de);
		if (r.anzahl > 1) b.append(en ? " (makes " : " (ergibt ").append(r.anzahl).append(")");
		b.append(en ? ": you need " : ": Du brauchst ");
		int i = 0;
		for (Map.Entry<String, Integer> e : zutaten.entrySet()) {
			if (i++ > 0) b.append(i == zutaten.size() ? (en ? " and " : " und ") : ", ");
			b.append(e.getValue()).append("× ").append(name(e.getKey(), en));
		}
		b.append(". ");
		if (r.formlos) {
			b.append(en ? "The shape doesn't matter – just put everything in the grid." : "Die Form ist egal – einfach alles ins Feld legen.");
		} else {
			b.append(en ? "Place it like in the picture." : "Leg es so hin wie im Bild.");
		}
		if (r.klein()) b.append(en ? " Fits in the 2×2 grid of your inventory." : " Passt auch ins 2×2-Feld im Inventar.");
		String h = en ? r.hinweisEn : r.hinweisDe;
		if (!h.isEmpty()) b.append(' ').append(h);
		return b.toString();
	}

	/** Suchwörter eines Rezepts (für den Helfer). */
	static String stichworte(Rezept r) {
		StringBuilder b = new StringBuilder();
		b.append(norm(r.de)).append(',').append(norm(r.en)).append(',').append(r.id.replace('_', ' '));
		String de = norm(r.de);
		if (de.contains(" ")) b.append(',').append(de.replace(" ", ""));
		for (String w : r.worte) if (!w.isEmpty()) b.append(',').append(norm(w));
		return b.toString();
	}

	public static int anzahl() {
		return ALLE.size();
	}
}
