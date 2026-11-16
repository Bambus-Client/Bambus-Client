package de.bambus.client.emote;

/**
 * Haltung des Spielermodells in einem Moment eines Emotes. Winkel in Bogenmaß wie bei Minecrafts ModelPart:
 * Arme: xRot negativ = nach vorn/oben (−π = gerade nach oben), zRot = zur Seite (rechter Arm positiv = nach außen,
 * linker Arm negativ = nach außen). Beine genauso. Kopf: xRot positiv = nach unten schauen, yRot = drehen.
 * Dazu das ganze Modell: drehung (Grad um die Hochachse), hoehe (Blöcke nach oben), neigung (Grad nach vorn).
 */
public final class Pose {

	public float kopfX, kopfY, kopfZ;
	public float koerperX, koerperY, koerperZ;
	public float rArmX, rArmY, rArmZ;
	public float lArmX, lArmY, lArmZ;
	public float rBeinX, rBeinY, rBeinZ;
	public float lBeinX, lBeinY, lBeinZ;
	public float drehung, hoehe, neigung;

	void zuruecksetzen() {
		kopfX = kopfY = kopfZ = 0;
		koerperX = koerperY = koerperZ = 0;
		rArmX = rArmY = 0;
		rArmZ = 0.06f;
		lArmX = lArmY = 0;
		lArmZ = -0.06f;
		rBeinX = rBeinY = rBeinZ = 0;
		lBeinX = lBeinY = lBeinZ = 0;
		drehung = hoehe = neigung = 0;
	}

	/** Beide Arme spiegelbildlich setzen (rechter Arm wie angegeben, linker gespiegelt). */
	void arme(float x, float z) {
		rArmX = x;
		rArmZ = z;
		lArmX = x;
		lArmZ = -z;
	}
}
