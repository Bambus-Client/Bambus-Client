// Bambus Client: Kosmetik eines Spielers speichern.
//
// Der Client ruft vorher bei Mojang "joinServer" mit einer Zufallskennung (serverId) auf – genau wie beim
// Betreten eines Servers. Diese Funktion fragt Mojang mit "hasJoined", ob der Spieler mit diesem Namen das
// wirklich getan hat, und bekommt dafür die echte UUID zurück. So kann niemand fremde Kosmetik ändern,
// und der Zugangs-Token des Spielers kommt nie hier an.
//
// Gespeichert werden nur eingebaute Designs (die Listen unten müssen zu Kosmetik.java passen).
import { createClient } from "jsr:@supabase/supabase-js@2";

const CAPES = ["bambus", "neon", "cyber", "galaxie", "sunset", "matrix", "polarlicht", "glut"];
const FLUEGEL = [
  "engel", "drache", "cyber", "schmetterling", "phoenix", "seraph", "nachtschwinge", "kristall", "aurora",
  "sakura", "bambus", "engel_classic", "drache_classic", "fee", "phoenix_classic",
];
const HUETE = ["krone", "zylinder", "heiligenschein", "hoerner", "katzenohren", "partyhut", "bandana", "visier"];
const PARTIKEL = ["funken", "glitzer", "herzen", "noten", "seelen", "kirsche", "magie", "flammen"];

const NAME = /^[A-Za-z0-9_]{1,16}$/;
const SERVER_ID = /^-?[0-9a-f]{1,64}$/;
const MIN_ABSTAND_MS = 5_000;   // höchstens alle 5 Sekunden pro Spieler

function antwort(status: number, daten: unknown): Response {
  return new Response(JSON.stringify(daten), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

function wahl(wert: unknown, erlaubt: string[]): string | null {
  return typeof wert === "string" && erlaubt.includes(wert) ? wert : null;
}

function mitStrichen(id: string): string {
  return `${id.slice(0, 8)}-${id.slice(8, 12)}-${id.slice(12, 16)}-${id.slice(16, 20)}-${id.slice(20)}`;
}

Deno.serve(async (req: Request) => {
  if (req.method !== "POST") return antwort(405, { fehler: "nur POST" });

  let daten: Record<string, unknown>;
  try {
    daten = await req.json();
  } catch {
    return antwort(400, { fehler: "kein JSON" });
  }
  const name = daten.name, serverId = daten.serverId;
  if (typeof name !== "string" || !NAME.test(name) || typeof serverId !== "string" || !SERVER_ID.test(serverId)) {
    return antwort(400, { fehler: "name oder serverId ungültig" });
  }

  // Bei Mojang nachfragen, wer das ist
  const mojang = await fetch(
    `https://sessionserver.mojang.com/session/minecraft/hasJoined?username=${encodeURIComponent(name)}` +
      `&serverId=${encodeURIComponent(serverId)}`,
  );
  if (mojang.status !== 200) return antwort(403, { fehler: "nicht bei Mojang bestätigt" });
  const profil = await mojang.json();
  if (typeof profil?.id !== "string" || !/^[0-9a-f]{32}$/.test(profil.id)) {
    return antwort(403, { fehler: "nicht bei Mojang bestätigt" });
  }
  const uuid = mitStrichen(profil.id);

  const db = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!, {
    auth: { persistSession: false },
  });

  const { data: alt } = await db.from("kosmetik").select("aktualisiert").eq("uuid", uuid).maybeSingle();
  if (alt && Date.now() - new Date(alt.aktualisiert).getTime() < MIN_ABSTAND_MS) {
    return antwort(429, { fehler: "zu schnell – bitte kurz warten" });
  }

  const zeile = {
    uuid,
    cape: wahl(daten.cape, CAPES),
    fluegel: wahl(daten.fluegel, FLUEGEL),
    hut: wahl(daten.hut, HUETE),
    partikel: wahl(daten.partikel, PARTIKEL),
    aktualisiert: new Date().toISOString(),
  };
  const { error } = await db.from("kosmetik").upsert(zeile);
  if (error) return antwort(500, { fehler: "speichern fehlgeschlagen" });
  return antwort(200, { ok: true, uuid });
});
