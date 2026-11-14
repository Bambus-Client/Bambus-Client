# Bambus Client – Musik-Anzeige: liest die Windows-Medienwiedergabe (Spotify, Browser, andere Player).
# Gibt jede halbe Sekunde eine JSON-Zeile aus; bei einem neuen Lied zusätzlich das Cover als 64x64-PNG (Base64).
# Befehle über die Eingabe (eine Zeile): pause, weiter, zurueck, modus spotify, modus alle.
# Endet von selbst, sobald Minecraft die Eingabe schließt.
$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = New-Object System.Text.UTF8Encoding $false
Add-Type -AssemblyName System.Runtime.WindowsRuntime
Add-Type -AssemblyName System.Drawing
$null = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager, Windows.Media.Control, ContentType = WindowsRuntime]
$null = [Windows.Storage.Streams.IRandomAccessStreamWithContentType, Windows.Storage.Streams, ContentType = WindowsRuntime]

$asTask = ([System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object {
	$_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1'
})[0]
function Warte($vorgang, [Type]$typ) {
	$aufgabe = $asTask.MakeGenericMethod($typ).Invoke($null, @($vorgang))
	$null = $aufgabe.Wait(-1)
	$aufgabe.Result
}

function Schreibe($objekt) {
	[Console]::Out.WriteLine(($objekt | ConvertTo-Json -Compress))
	[Console]::Out.Flush()
}

function Cover($eigenschaften) {
	try {
		if ($null -eq $eigenschaften.Thumbnail) { return '' }
		$strom = Warte ($eigenschaften.Thumbnail.OpenReadAsync()) ([Windows.Storage.Streams.IRandomAccessStreamWithContentType])
		$netStrom = [System.IO.WindowsRuntimeStreamExtensions]::AsStreamForRead($strom)
		$bild = [System.Drawing.Image]::FromStream($netStrom)
		$seite = [Math]::Min($bild.Width, $bild.Height)
		$klein = New-Object System.Drawing.Bitmap 64, 64
		$g = [System.Drawing.Graphics]::FromImage($klein)
		$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
		$ziel = New-Object System.Drawing.Rectangle 0, 0, 64, 64
		$g.DrawImage($bild, $ziel, [float](($bild.Width - $seite) / 2), [float](($bild.Height - $seite) / 2), [float]$seite, [float]$seite, [System.Drawing.GraphicsUnit]::Pixel)
		$speicher = New-Object System.IO.MemoryStream
		$klein.Save($speicher, [System.Drawing.Imaging.ImageFormat]::Png)
		$g.Dispose(); $klein.Dispose(); $bild.Dispose(); $netStrom.Dispose()
		return [Convert]::ToBase64String($speicher.ToArray())
	} catch {
		return ''
	}
}

$manager = Warte ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]::RequestAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager])
$nurSpotify = $false
$letztesLied = $null
$coverNochmal = $null   # Spotify meldet das neue Cover oft erst kurz nach dem neuen Titel
$eingabe = [Console]::In
$naechsteZeile = $eingabe.ReadLineAsync()

while ($true) {
	# Befehle von Minecraft
	while ($naechsteZeile.IsCompleted) {
		$befehl = $naechsteZeile.Result
		if ($null -eq $befehl) { exit }   # Minecraft wurde beendet
		$befehl = $befehl.Trim()
		$naechsteZeile = $eingabe.ReadLineAsync()
		if ($befehl -eq 'modus spotify') { $nurSpotify = $true; $letztesLied = $null; continue }
		if ($befehl -eq 'modus alle') { $nurSpotify = $false; $letztesLied = $null; continue }
		$s = $null
		try { $s = $manager.GetCurrentSession() } catch { }
		if ($nurSpotify -or $null -eq $s) {
			$s = $manager.GetSessions() | Where-Object { $_.SourceAppUserModelId -match 'Spotify' } | Select-Object -First 1
		}
		if ($null -ne $s) {
			try {
				switch ($befehl) {
					'pause' { $null = Warte ($s.TryTogglePlayPauseAsync()) ([bool]) }
					'weiter' { $null = Warte ($s.TrySkipNextAsync()) ([bool]) }
					'zurueck' { $null = Warte ($s.TrySkipPreviousAsync()) ([bool]) }
				}
			} catch { }
		}
	}

	# Was läuft gerade?
	try {
		$sitzung = $null
		if ($nurSpotify) {
			$sitzung = $manager.GetSessions() | Where-Object { $_.SourceAppUserModelId -match 'Spotify' } | Select-Object -First 1
		} else {
			$sitzung = $manager.GetCurrentSession()
		}
		if ($null -eq $sitzung) {
			if ($null -ne $letztesLied) { Schreibe @{ leer = $true }; $letztesLied = $null }
		} else {
			$info = Warte ($sitzung.TryGetMediaPropertiesAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties])
			$zeit = $sitzung.GetTimelineProperties()
			$spielt = $sitzung.GetPlaybackInfo().PlaybackStatus -eq [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionPlaybackStatus]::Playing
			$dauer = ($zeit.EndTime - $zeit.StartTime).TotalSeconds
			$position = $zeit.Position.TotalSeconds
			if ($spielt -and $zeit.LastUpdatedTime.Year -gt 2000) {
				$position += ([DateTimeOffset]::Now - $zeit.LastUpdatedTime).TotalSeconds
			}
			if ($dauer -gt 0 -and $position -gt $dauer) { $position = $dauer }
			$lied = "$($info.Title)|$($info.Artist)|$($sitzung.SourceAppUserModelId)"
			$daten = @{
				titel = [string]$info.Title
				kuenstler = [string]$info.Artist
				album = [string]$info.AlbumTitle
				app = [string]$sitzung.SourceAppUserModelId
				position = [Math]::Round($position, 1)
				dauer = [Math]::Round($dauer, 1)
				spielt = [bool]$spielt
			}
			if ($lied -ne $letztesLied) {
				$daten.cover = Cover $info
				$letztesLied = $lied
				$coverNochmal = [DateTime]::Now.AddSeconds(2)
			} elseif ($null -ne $coverNochmal -and [DateTime]::Now -ge $coverNochmal) {
				$daten.cover = Cover $info
				$coverNochmal = $null
			}
			Schreibe $daten
		}
	} catch {
		Schreibe @{ fehler = [string]$_.Exception.Message }
	}
	Start-Sleep -Milliseconds 500
}
