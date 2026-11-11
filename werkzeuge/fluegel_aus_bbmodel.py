"""Blockbench-Flügel (Box-UV, flache Platten) in das Bambus-Flügelbild umrechnen.

Liest aus jeder .bbmodel-Datei die Platten des rechten Flügels (Elemente "right_panel*"),
nimmt jeweils die Südseite (von hinten gesehen) und setzt sie zu einem quadratischen PNG zusammen:
Gelenk/Schulter oben links, Flügel nach rechts – das Format, das Launcher und Mod für
quadratische Flügelbilder erwarten (der linke Flügel wird gespiegelt).

Aufruf: python3 fluegel_aus_bbmodel.py <zielordner> <datei.bbmodel>=<id> [...]
Beispiel: python3 fluegel_aus_bbmodel.py src/main/resources/assets/bambus/textures/fluegel \\
          angel_wings.bbmodel=engel_classic
"""
import base64, io, json, sys
from PIL import Image

GROESSE = 256  # Ausgabe in Pixeln (Pixelkunst wird ohne Glätten hochskaliert)


def textur(modell):
    quelle = modell["textures"][0]["source"]
    return Image.open(io.BytesIO(base64.b64decode(quelle.split(",", 1)[1]))).convert("RGBA")


def umrechnen(pfad):
    modell = json.load(open(pfad, encoding="utf-8"))
    tex = textur(modell)
    platten = [e for e in modell["elements"] if e["name"].startswith("right_panel")]
    x_min = min(p["from"][0] for p in platten)
    x_max = max(p["to"][0] for p in platten)
    y_min = min(p["from"][1] for p in platten)
    y_max = max(p["to"][1] for p in platten)
    seite = int(round(max(x_max - x_min, y_max - y_min)))
    bild = Image.new("RGBA", (seite, seite), (0, 0, 0, 0))
    for p in platten:
        u0, v0, u1, v1 = (int(round(z)) for z in p["faces"]["south"]["uv"])
        stueck = tex.crop((min(u0, u1), min(v0, v1), max(u0, u1), max(v0, v1)))
        if u1 < u0:
            stueck = stueck.transpose(Image.FLIP_LEFT_RIGHT)
        # Südseite: Spalte 0 = kleinstes x (Schulterseite), Zeile 0 = oben
        ziel = (int(round(p["from"][0] - x_min)), int(round(y_max - p["to"][1])))
        bild.alpha_composite(stueck, ziel)
    return bild.resize((GROESSE, GROESSE), Image.NEAREST)


if __name__ == "__main__":
    ziel = sys.argv[1]
    for arg in sys.argv[2:]:
        datei, kennung = arg.split("=", 1)
        umrechnen(datei).save(f"{ziel}/{kennung}.png")
        print(kennung, "<-", datei)
