# Power and networking

Both loose batteries and whole handheld devices expose `Capabilities.EnergyStorage.ITEM`. Whole-phone charging resolves the server-owned installed battery by the device UUID. Receiving FE is capped by remaining capacity; simulation does not add charge. Batteries cannot export FE. Creative batteries provide infinite device power and do not accept energy.

Charging Stations have nine automation-accessible slots, a shared configurable charging budget and a 2 FE/t idle cost. Transfers debit only accepted FE from the block buffer. They never create charge when the buffer is empty. Removing a phone battery returns its physical item, UUID and current charge. Devices without installed batteries accept no charging energy.

Towers, routers, gateways, racks and chargers accept standard NeoForge block FE from every side. Flux/FE cables and Create Power Grid's FE bridge can use this boundary. Kinetic-to-FE conversion is supplied by the external modpack.

Loaded powered nodes are indexed per dimension. Bounded breadth-first traversal follows adjacent Ethernet/WAN blocks and operating nodes, without forcing chunks. Cable cuts, unavailable chunks and unpowered nodes break routing. Tower coverage uses matching SIM carriers, power-limited radius and gateway backhaul. WiFi uses SSID, key and radius. Local racks remain accessible over a LAN even without WAN. Gateway backends are abstractions, not real-world internet connections.

Package contents are capped at 2,048 characters and each rack at 32 files. AES-256-GCM uses random salt/nonce and PBKDF2-HMAC-SHA256 passphrase derivation. Owner/operator authorization gates writes; decrypting needs the package passphrase. Cartridges store ciphertext. Item automation supports AE2 Storage Bus inventory access. Optional OC2R callbacks expose status, package listing/reading and writes authenticated by the rack's separately configured control key. No packet-level Ethernet bridge or ME network service is implemented.
