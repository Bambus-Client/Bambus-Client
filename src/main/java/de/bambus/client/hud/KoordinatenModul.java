package de.bambus.client.hud;

import de.bambus.client.Farben;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/** Koordinaten mit Umrechnung Oberwelt ↔ Nether. Mit einer eigenen Taste in die Zwischenablage. */
public class KoordinatenModul extends TextModul {

	public KoordinatenModul() {
		super("koordinaten", "Koordinaten", 4, 216, false, "Mit Nether-Umrechnung, Taste zum Kopieren", "INFO");
	}

	@Override
	protected List<Zeile> zeilen(Minecraft mc, boolean editor) {
		List<Zeile> z = new ArrayList<>();
		LocalPlayer p = mc.player;
		if (p == null) {
			z.add(new Zeile("X 128  Y 64  Z -340", Farben.WEISS));
			z.add(new Zeile("Nether: X 16  Z -42", Farben.ROT));
			return z;
		}
		int x = (int) Math.floor(p.getX()), y = (int) Math.floor(p.getY()), zz = (int) Math.floor(p.getZ());
		z.add(new Zeile("X " + x + "  Y " + y + "  Z " + zz, Farben.WEISS));
		boolean nether = mc.level != null && mc.level.dimension() == Level.NETHER;
		boolean ende = mc.level != null && mc.level.dimension() == Level.END;
		if (nether) {
			z.add(new Zeile("Oberwelt: X " + x * 8 + "  Z " + zz * 8, Farben.GRUEN));
		} else if (!ende) {
			z.add(new Zeile("Nether: X " + Math.floorDiv(x, 8) + "  Z " + Math.floorDiv(zz, 8), Farben.ROT));
		}
		return z;
	}

	/** Text für die Zwischenablage. */
	public static String alsText(Minecraft mc) {
		LocalPlayer p = mc.player;
		if (p == null) return "";
		return (int) Math.floor(p.getX()) + " " + (int) Math.floor(p.getY()) + " " + (int) Math.floor(p.getZ());
	}
}
