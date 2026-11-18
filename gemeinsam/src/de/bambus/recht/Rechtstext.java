package de.bambus.recht;

/**
 * Das Rechtliche, das man vor der ersten Nutzung lesen und bestätigen muss – derselbe Text im Launcher
 * und im Spiel, auf Deutsch und Englisch.
 *
 * Wird der Text inhaltlich geändert, {@link #VERSION} erhöhen: Dann fragen Launcher und Client beim
 * nächsten Start noch einmal nach. Die Zustimmung steht in {@code config/bambus.json}
 * (Felder {@code rechtlichZugestimmt} = Version, {@code rechtlichDatum}).
 *
 * Keine Rechtsberatung – ein verständlicher Rahmen für ein kostenloses Hobbyprojekt.
 */
public final class Rechtstext {

	/** Stand des Textes. Höher = alle müssen neu zustimmen. */
	public static final int VERSION = 2;

	public static final String STAND = "18.11.2026";

	private Rechtstext() {}

	/** Abschnitte als {Überschrift, Text}. Absätze mit „\n“, Aufzählungen mit „• “. */
	public static String[][] abschnitte(boolean englisch) {
		return englisch ? EN : DE;
	}

	public static String titel(boolean englisch) {
		return englisch ? "Before you start: the legal stuff" : "Bevor es losgeht: das Rechtliche";
	}

	public static String hinweisLesen(boolean englisch) {
		return englisch ? "Please read everything – scroll all the way down."
				: "Bitte alles lesen – ganz nach unten scrollen.";
	}

	public static String haken(boolean englisch) {
		return englisch ? "I have read everything and I agree." : "Ich habe alles gelesen und stimme zu.";
	}

	public static String zustimmen(boolean englisch) {
		return englisch ? "AGREE" : "ZUSTIMMEN";
	}

	public static String ablehnen(boolean englisch) {
		return englisch ? "DECLINE" : "ABLEHNEN";
	}

	public static String abgelehnt(boolean englisch) {
		return englisch ? "Without your agreement, Bamboo can't be used. It will close now."
				: "Ohne deine Zustimmung kann Bambus nicht genutzt werden. Es wird jetzt geschlossen.";
	}

	private static final String[][] DE = {
			{"Kurz gesagt",
					"• Bambus ist kostenlos und kein offizielles Minecraft-Produkt.\n"
							+ "• Du brauchst ein eigenes, gekauftes Minecraft: Java Edition. Bambus fragt nie nach deinem Passwort.\n"
							+ "• Die Regeln von Mojang und von jedem Server gelten weiter – auch mit Bambus.\n"
							+ "• Bambus hat keinen eigenen Server. Deine Einstellungen, Blätter und Kosmetik bleiben auf deinem Computer.\n"
							+ "• Alle Rechte an Bambus gehören den Entwicklern. Kopieren, Verändern oder Weitergeben ist ohne ihre Erlaubnis verboten.\n"
							+ "• Bambus wird so angeboten, wie es ist – ohne Garantie."},
			{"1. Kein offizielles Minecraft-Produkt",
					"Bambus ist ein Hobbyprojekt. Es ist kein offizielles Minecraft-Produkt und nicht von Mojang oder Microsoft "
							+ "genehmigt oder mit ihnen verbunden. „Minecraft“ ist eine Marke von Mojang AB bzw. Microsoft.\n"
							+ "Bambus enthält keine Spieldateien von Minecraft. Das Spiel selbst kommt immer direkt von Mojang."},
			{"2. Dein Minecraft-Konto",
					"• Du spielst nur mit einem eigenen, gekauften Minecraft: Java Edition. Einen Modus ohne Kauf gibt es nicht.\n"
							+ "• Angemeldet wird ausschließlich bei Microsoft – im offiziellen Minecraft Launcher oder auf der Anmeldeseite von Microsoft. "
							+ "Bambus sieht dein Passwort nie.\n"
							+ "• Gib deine Zugangsdaten niemals weiter, auch nicht an Freunde."},
			{"3. Regeln von Mojang und den Servern",
					"• Für Minecraft gelten der Endbenutzer-Lizenzvertrag (EULA) und die Nutzungsrichtlinien von Mojang – mit und ohne Bambus.\n"
							+ "• Jeder Server hat eigene Regeln. Manche Funktionen (zum Beispiel Freelook oder Fullbright) "
							+ "sind auf einigen Servern verboten. Schalte sie dort aus.\n"
							+ "• Für das, was du auf Servern tust, bist du selbst verantwortlich. Wird dein Konto auf einem Server gesperrt, "
							+ "kann Bambus das nicht rückgängig machen.\n"
							+ "• Sei fair: Bambus bringt keine Cheats, Wallhacks oder sonstigen Vorteile gegenüber anderen Spielern mit."},
			{"4. Kostenlos – kein echtes Geld",
					"• Bambus kostet nichts und verkauft nichts.\n"
							+ "• Blätter, Ränge, Quests und Kosmetik sind nur Spielspaß. Sie haben keinen Geldwert, können nicht gekauft, "
							+ "verkauft oder getauscht werden und es gibt keinen Anspruch darauf.\n"
							+ "• Wer dir Blätter, Kosmetik oder „Premium“ gegen Geld anbietet, betrügt dich."},
			{"5. Deine Daten",
					"Bambus hat keinen eigenen Server, kein Benutzerkonto, keine Werbung und kein Tracking. "
							+ "Einstellungen, Blätter, Kosmetik, Wegpunkte und Freundesliste werden nur auf deinem Computer gespeichert.\n"
							+ "Damit alles funktioniert, verbindet sich Bambus direkt mit diesen Diensten – dabei sehen sie, wie jede Webseite, "
							+ "deine IP-Adresse:\n"
							+ "• Microsoft und Mojang: Anmeldung, Spieldateien und Skins\n"
							+ "• Fabric, Modrinth und CurseForge: Mods herunterladen und suchen\n"
							+ "• GitHub: Updates für Launcher und Client\n"
							+ "• lrclib.net: Liedtexte für den Musik-Player (dafür werden Titel und Interpret des Lieds gesendet)\n"
							+ "• Minecraft-Server, die du einträgst: Abfrage von Spielerzahl und Ping\n"
							+ "• Discord: nur wenn du die Discord-Anzeige einschaltest, und nur über das Discord-Programm auf deinem Computer\n"
							+ "Für diese Dienste gelten deren eigene Datenschutzbestimmungen."},
			{"6. Alle Rechte bei den Entwicklern",
					"Bambus – Launcher, Client, Name, Logo, Grafiken, Kosmetik, Texte und Musik – gehört den Entwicklern von Bambus. "
							+ "Alle Rechte sind vorbehalten.\n"
							+ "• Erlaubt: die offiziellen Versionen von GitHub oder aus dem Update kostenlos herunterladen und privat damit spielen. "
							+ "Den Quellcode darfst du auf GitHub ansehen.\n"
							+ "• Verboten ohne schriftliche Erlaubnis der Entwickler: Bambus kopieren, verändern, nachbauen, woanders hochladen, "
							+ "weitergeben oder verkaufen, Name, Logo, Grafiken oder Musik benutzen oder Bambus als eigenes Werk ausgeben.\n"
							+ "• Wer das trotzdem tut, verletzt die Rechte der Entwickler. Dann können rechtliche Schritte eingeleitet werden "
							+ "(zum Beispiel Abmahnung, Unterlassung oder Schadensersatz), und die Erlaubnis zum Spielen endet.\n"
							+ "Die genauen Bedingungen stehen in der Datei LICENSE."},
			{"7. Ohne Gewähr",
					"Bambus wird kostenlos und so angeboten, wie es ist – ohne Garantie, dass es fehlerfrei läuft. "
							+ "Mach Sicherungen deiner wichtigen Welten, bevor du Mods oder neue Minecraft-Versionen ausprobierst."},
			{"8. Für Kinder und Jugendliche",
					"Wenn du jünger als 16 bist, lies das hier bitte zusammen mit deinen Eltern oder einer anderen erwachsenen Person, "
							+ "der du vertraust. Frag nach, wenn etwas unklar ist."},
			{"9. Änderungen",
					"Wenn sich an diesen Regeln etwas Wichtiges ändert, zeigen Launcher und Client sie beim nächsten Start noch einmal "
							+ "an, und du stimmst neu zu. Nachlesen kannst du sie jederzeit – im Launcher unter EINSTELLUNGEN → RECHTLICHES, im Spiel unter Mod-Menü → MEHR → Rechtliches.\n"
							+ "Stand: " + STAND},
			{"Zum Nachlesen",
					"• Minecraft-EULA: minecraft.net/eula\n"
							+ "• Nutzungsrichtlinien: minecraft.net/usage-guidelines\n"
							+ "• Lizenz und Quellcode: github.com/Bambus-Client/Bambus-Client (Datei LICENSE)"},
	};

	private static final String[][] EN = {
			{"In short",
					"• Bamboo is free and not an official Minecraft product.\n"
							+ "• You need your own purchased copy of Minecraft: Java Edition. Bamboo never asks for your password.\n"
							+ "• Mojang's rules and every server's rules still apply – with Bamboo too.\n"
							+ "• Bamboo has no server of its own. Your settings, leaves and cosmetics stay on your computer.\n"
							+ "• All rights in Bamboo belong to its developers. Copying, changing or sharing it without their permission is not allowed.\n"
							+ "• Bamboo is provided as it is – without any warranty."},
			{"1. Not an official Minecraft product",
					"Bamboo is a hobby project. It is not an official Minecraft product and is not approved by or associated with "
							+ "Mojang or Microsoft. “Minecraft” is a trademark of Mojang AB / Microsoft.\n"
							+ "Bamboo contains no Minecraft game files. The game itself always comes straight from Mojang."},
			{"2. Your Minecraft account",
					"• You only play with your own purchased Minecraft: Java Edition. There is no mode without buying the game.\n"
							+ "• You sign in only with Microsoft – in the official Minecraft Launcher or on Microsoft's sign-in page. "
							+ "Bamboo never sees your password.\n"
							+ "• Never share your login details, not even with friends."},
			{"3. Mojang's rules and server rules",
					"• Minecraft's End User License Agreement (EULA) and Mojang's usage guidelines apply – with or without Bamboo.\n"
							+ "• Every server has its own rules. Some features (for example freelook or fullbright) "
							+ "are not allowed on some servers. Turn them off there.\n"
							+ "• You are responsible for what you do on servers. If a server bans your account, Bamboo cannot undo it.\n"
							+ "• Play fair: Bamboo comes without cheats, wallhacks or any other advantage over other players."},
			{"4. Free – no real money",
					"• Bamboo costs nothing and sells nothing.\n"
							+ "• Leaves, ranks, quests and cosmetics are just for fun. They have no money value, cannot be bought, "
							+ "sold or traded, and you have no claim to them.\n"
							+ "• Anyone offering you leaves, cosmetics or “premium” for money is trying to scam you."},
			{"5. Your data",
					"Bamboo has no server of its own, no user account, no ads and no tracking. "
							+ "Settings, leaves, cosmetics, waypoints and your friends list are stored only on your computer.\n"
							+ "To work, Bamboo connects directly to these services – like any website, they can see your IP address:\n"
							+ "• Microsoft and Mojang: sign-in, game files and skins\n"
							+ "• Fabric, Modrinth and CurseForge: downloading and searching mods\n"
							+ "• GitHub: updates for the launcher and the client\n"
							+ "• lrclib.net: song lyrics for the music player (the song's title and artist are sent)\n"
							+ "• Minecraft servers you add: checking player count and ping\n"
							+ "• Discord: only if you turn on the Discord status, and only through the Discord app on your computer\n"
							+ "Their own privacy policies apply to these services."},
			{"6. All rights belong to the developers",
					"Bamboo – launcher, client, name, logo, graphics, cosmetics, texts and music – belongs to Bamboo's developers. "
							+ "All rights are reserved.\n"
							+ "• Allowed: downloading the official versions from GitHub or the updater for free and playing with them privately. "
							+ "You may view the source code on GitHub.\n"
							+ "• Not allowed without the developers' written permission: copying, changing or recreating Bamboo, uploading it "
							+ "elsewhere, sharing or selling it, using its name, logo, graphics or music, or passing it off as your own work.\n"
							+ "• Anyone who does this anyway infringes the developers' rights. Legal action can then be taken "
							+ "(for example a cease-and-desist letter, an injunction or damages), and the permission to play ends.\n"
							+ "The exact terms are in the LICENSE file."},
			{"7. No warranty",
					"Bamboo is provided free of charge and as it is – without any guarantee that it works without errors. "
							+ "Back up your important worlds before you try mods or new Minecraft versions."},
			{"8. For children and teenagers",
					"If you are younger than 16, please read this together with your parents or another adult you trust. "
							+ "Ask if anything is unclear."},
			{"9. Changes",
					"If something important changes in these rules, the launcher and the client will show them again the next time "
							+ "they start, and you agree again. You can read them any time – in the launcher under SETTINGS → LEGAL, in the game under mod menu → MORE → Legal.\n"
							+ "Last updated: " + STAND},
			{"To read more",
					"• Minecraft EULA: minecraft.net/eula\n"
							+ "• Usage guidelines: minecraft.net/usage-guidelines\n"
							+ "• Licence and source code: github.com/Bambus-Client/Bambus-Client (LICENSE file)"},
	};
}
