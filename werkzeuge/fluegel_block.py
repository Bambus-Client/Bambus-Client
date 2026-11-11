#!/usr/bin/env python3
"""Erzeugt die Blockbench-Stil-Texturen der Flügel: <id>_block.png (32×16, grobe Pixel wie ein Minecraft-Modell).
Quelle ist das fertige HD-Bild <id>.png (512×256). Aufruf: python3 werkzeuge/fluegel_block.py"""
import glob, os
from PIL import Image

ORDNER = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "bambus", "textures", "fluegel")
B, H = 32, 16

for pfad in sorted(glob.glob(os.path.join(ORDNER, "*.png"))):
    name = os.path.basename(pfad)[:-4]
    if name.endswith(("_hinten", "_vorne", "_block")):
        continue
    bild = Image.open(pfad).convert("RGBA")
    zb, zh = bild.width // B, bild.height // H
    px = bild.load()
    aus = Image.new("RGBA", (B, H), (0, 0, 0, 0))
    for y in range(H):
        for x in range(B):
            r = g = b = a = 0
            for yy in range(y * zh, (y + 1) * zh):
                for xx in range(x * zb, (x + 1) * zb):
                    pr, pg, pb, pa = px[xx, yy]
                    r += pr * pa; g += pg * pa; b += pb * pa; a += pa
            if a / (zb * zh) >= 255 * 0.4:          # Zelle überwiegend gefüllt → ein deckender Pixel
                # leicht auf Stufen runden, damit es nach Pixel-Art aussieht
                farbe = tuple(min(255, int(round(c / a / 17.0)) * 17) for c in (r, g, b))
                aus.putpixel((x, y), farbe + (255,))
    aus.save(os.path.join(ORDNER, name + "_block.png"))
    print(name + "_block.png")
