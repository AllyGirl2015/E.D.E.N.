# E.D.E.N. — Evolution Design Exploration Nexus

A NeoForge **Minecraft 1.21.1 / Java 21** device and service platform for Alissa's E.D.E.N./Pokemonsters project.

**Development alpha, not a completed SPhone port.** This repository preserves the uploaded original SPhone ZIP as a historical reference and develops a new implementation at the repository root. Do not install that ZIP or the old 1.12.2 libraries into a modern server.

## Available in this alpha

- Phone, tablet and laptop items; placeable desktop terminal.
- Shared native E.D.E.N. OS screen and app catalog on every device.
- Physical SIM insertion/ejection, server-issued unique numbers and Alyon Wireless accounts.
- SMS, including offline delivery; SIM-owned contacts and message history.
- Device notes, calculator, current world weather and three themes.
- App Studio: create/update text apps and shared bulletin services inside the game, then connect an app to a service.
- Java addon APIs for app descriptors, server services and optional client screens.
- Native ItemStack data components and overworld SavedData. No SQL/database installation.

Cellular coverage currently spans the whole world. **Voice calls, tower/carrier simulation, Create/energy adapters, Wi-Fi/Ethernet, banking, transport services, camera/gallery and an advanced visual app editor are not implemented.** No compatibility with those external mods is claimed yet.

## Install a built JAR

Use NeoForge **21.1.252** for Minecraft **1.21.1**, with Java **21**. Put `eden-0.1.0-alpha.1.jar` into `mods` on both client and server. Use a test world first; world storage and APIs are experimental.

A compiled alpha snapshot is also included in `downloads/`. The `-sources.jar` is for developers, not installation. The source ZIP is a project, not a playable mod.

## First play

1. Find the **E.D.E.N. Devices** creative tab, or craft a device and SIM with the included recipes.
2. Right-click a handheld device, or right-click a placed desktop with an empty hand.
3. Open **Settings**, keep a SIM in your inventory and click **Insert SIM**.
4. Your assigned number appears in the header. Give it to another player.
5. Open **Messages**, enter their number and message, then click **Send**.
6. **Eject SIM** returns the card to your inventory (or drops it if full). Insert it into a replacement device to retain your number, contacts and SMS.
7. Open **App Studio** for in-game apps and services. See [the Studio walkthrough](docs/APP_STUDIO.md).

SMS is limited to 512 characters; each SIM retains its most recent 100 messages and sends its latest 50 to the screen. Contacts are capped at 64. Scroll the text areas to view history. Refresh retrieves current server data.

The desktop ejects its SIM when broken. Its survival drop carries the device ID so replacing it retains notes/settings. An explosion that destroys the drop can lose the physical device; the SIM is ejected separately.

## Build

Install JDK 21, then:

```powershell
.\gradlew.bat build
```

On Linux/macOS:

```sh
./gradlew build
```

JARs appear in `build/libs`. The first build downloads Minecraft/NeoForge and can take several minutes. Development commands: `gradlew.bat runClient` and `gradlew.bat runServer`. Tests: `gradlew.bat test`.

GitHub Actions builds on the development branch and pull requests, and uploads the runtime and sources JARs plus test reports. It does not publish a release or deploy a server.

## Developers and project status

- [Java SDK](docs/SDK.md)
- [Architecture and data ownership](docs/ARCHITECTURE.md)
- [Features and remaining work](docs/ROADMAP.md)
- [Validation](docs/VALIDATION.md)

This is a new architecture inspired by SPhone and the shared-device concept of MrCrayfish's Device Mod. It is not binary-compatible with their apps or saves. The adapted SPhone message model retains Apache-2.0 attribution under `third_party/sphone`. No MrCrayfish source/assets, ACsGuis, DynamX or JDBC binaries are bundled. Original E.D.E.N. code has not yet been assigned an open-source redistribution license; see `LICENSE`.
