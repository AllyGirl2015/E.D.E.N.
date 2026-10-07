# E.D.E.N. — 0.1.0-alpha.2

Minecraft **1.21.1 / NeoForge 21.1.252 / Java 21**. Install the runtime JAR from `downloads/` into `mods/`. The original uploaded SPhone archive remains unchanged in this repository.

This alpha adapts SPhone's portrait phone shell, original icons, wallpapers, phone/SIM models, message model and note workflows to modern NeoForge. It adds contacts, threaded messages, multiple notes, calculator, weather, wallpaper selection, App Store, calls, batteries and powered networking. Phone, tablet, laptop and desktop share the app platform. It does not reproduce every historical SPhone feature.

## Quick setup

1. Supply FE to a Charging Station. Shift-right-click to open its nine slots. Charge a loose battery, then install it from the phone's Battery app, or put the entire phone in a slot to charge its installed battery. Compatible external FE item chargers can also charge phones and loose batteries.
2. Keep a SIM in your inventory and insert it from Settings. Set its carrier in Network.
3. Power an Internet Gateway, then connect a matching Cell Tower or WiFi Router with Ethernet/WAN cable blocks. Configure hardware with right-click; the placing player or an operator can edit settings. Shift-right-click Server Racks opens their cartridge inventory.
4. Join the router's SSID/password in Network, or use a matching carrier within tower coverage. Desktops can use adjacent Ethernet cables.
5. For calls, install Simple Voice Chat on both clients and the server. Both players need connected voice sessions, powered devices and coverage. Dial, answer and use your voice-chat microphone key.
6. Connect a Server Rack to the LAN. Its owner can upload passphrase-encrypted text/data packages, export cartridges or import them. Other users who know the passphrase can download packages.

## Batteries

Normal batteries are crafted empty. Installed batteries use one FE per game tick; removed batteries retain their charge. Sleeping changes daylight without consuming skipped game ticks. Pausing/stopping the server pauses drain. Creative batteries have no recipe.

| Battery | Minecraft days | Normal-speed real time | Capacity |
|---|---:|---:|---:|
| Coal | 8 | 2h 40m | 192,000 FE |
| Copper | 15 | 5h | 360,000 FE |
| Iron | 30 | 10h | 720,000 FE |
| Gold | 40 | 13h 20m | 960,000 FE |
| Diamond | 60 | 20h | 1,440,000 FE |
| Netherite | 80 | 26h 40m | 1,920,000 FE |
| Creative | Infinite | Infinite | Infinite |

## Scope and compatibility

All infrastructure accepts NeoForge FE. Flux Networks and Create Power Grid can feed that capability when their installed versions support it. Create kinetic power needs an FE generator/converter. The charger defaults to a shared 1,024 FE/t slot budget plus 2 FE/t idle draw. Server config controls required power, batteries, network/backhaul, range, draw, buffer capacity and traversal limits; hardware settings control SSID, password, carrier, enabled state, range and power budget.

AE2 compatibility exposes the rack's physical cartridge inventory via the standard item capability. It is not an ME terminal or autocrafting bridge. OC2R 0.1.0 compatibility adds adjacent device-bus callbacks for status and controlled package access. It is not an OC2 Linux Ethernet/TCP/IP bridge. These optional integrations need live modpack testing.

Internet Gateways connect to an abstract in-game backend. Ethernet and WAN cable blocks currently use the same routing rules. No real internet access, IP addressing, DHCP, VLANs or firewall simulation is implemented. Package encryption is AES-GCM encrypted storage with passphrase-derived keys; it does not make the LAN transport end-to-end encrypted. Packages are bounded text/data, not executable arbitrary code.

Camera uses Minecraft's F2 screenshot workflow; Gallery displays this computer's screenshots. Weather reports current conditions, without forecasting. Live two-player voice/audio and full external modpack compatibility have not yet been verified.

Build: `./gradlew build`; integration tests: `./gradlew runGameTestServer`. Optional rendered fixture: `./gradlew runClient -PedenUiTest`. See [validation](docs/VALIDATION.md), [power/network details](docs/POWER_AND_NETWORKING.md) and the retained SPhone license/notice under `third_party/sphone/`.
