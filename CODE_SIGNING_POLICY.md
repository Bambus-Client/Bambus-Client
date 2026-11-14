# Code signing policy

Free code signing provided by [SignPath.io](https://about.signpath.io), certificate by [SignPath Foundation](https://signpath.org).

Signed files: the Windows program `Bambus Launcher.exe` and the installer `Bambus-Launcher-Setup.exe`.
They are built from this public repository by GitHub Actions (`.github/workflows/build.yml`) and signed
only through that build – never from a developer's computer.

## Team roles

| Role | Members |
|---|---|
| Committers and reviewers | [bamboocraft7](https://github.com/bamboocraft7), [Bambus-CPU](https://github.com/Bambus-CPU) |
| Approvers (release signing) | [bamboocraft7](https://github.com/bamboocraft7) |

All team members use multi-factor authentication for GitHub and SignPath.

## Privacy policy

This program will not transfer any information to other networked systems unless specifically requested
by the user or the person installing or operating it.

What the launcher and the Minecraft mod connect to, and why:

| Service | When | What is sent |
|---|---|---|
| Mojang / Microsoft (minecraft.net, xboxlive.com, microsoftonline.com) | Installing and starting Minecraft, signing in with a Microsoft account | Game file requests; sign-in tokens only to Microsoft and Mojang |
| FabricMC (meta.fabricmc.net) | Setting up Fabric for the chosen Minecraft version | Minecraft version |
| Modrinth, CurseForge | Searching for and installing mods | Search text, mod names, Minecraft version |
| LRCLIB (lrclib.net) | Only if the music module with lyrics is switched on | Title, artist, album and length of the song that is playing |
| GitHub (api.github.com) | Checking for updates | – |

No passwords are ever stored or sent to the Bambus team. Settings stay on the user's computer.
