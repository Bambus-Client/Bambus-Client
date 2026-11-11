-- Bambus Client: Kosmetik für alle
-- Jeder darf lesen, welche eingebaute Kosmetik ein Spieler trägt. Schreiben geht nur über die
-- Edge Function "kosmetik" (prüft bei Mojang, dass der Spieler wirklich er selbst ist).

create table if not exists public.kosmetik (
	uuid uuid primary key,
	cape text check (cape is null or cape ~ '^[a-z_]{1,32}$'),
	fluegel text check (fluegel is null or fluegel ~ '^[a-z_]{1,32}$'),
	hut text check (hut is null or hut ~ '^[a-z_]{1,32}$'),
	partikel text check (partikel is null or partikel ~ '^[a-z_]{1,32}$'),
	aktualisiert timestamptz not null default now()
);

comment on table public.kosmetik is
	'Bambus Client: eingebaute Kosmetik je Minecraft-Spieler (UUID). Lesen öffentlich, Schreiben nur über die Edge Function kosmetik.';

alter table public.kosmetik enable row level security;

-- Lesen für alle (auch ohne Anmeldung); keine Richtlinie für insert/update/delete → nur die Edge Function
-- (mit dem Service-Schlüssel) darf schreiben.
drop policy if exists "kosmetik lesen" on public.kosmetik;
create policy "kosmetik lesen" on public.kosmetik
	for select to anon, authenticated using (true);

revoke insert, update, delete, truncate on public.kosmetik from anon, authenticated;
grant select on public.kosmetik to anon, authenticated;
