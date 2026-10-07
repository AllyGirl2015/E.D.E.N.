# Architecture

`EdenSavedData` owns UUID-keyed device records and SIM accounts in native world storage. An additive migration restores alpha.1 notes and defaults. GUI actions require a valid held-device or nearby desktop session and are rate limited. The client receives bounded compressed JSON snapshots; no client-supplied identity controls another player's account.

SPhone's portrait shell and assets are adapted in `DeviceScreen`. Physical SIMs and batteries have persistent native item components. Installed battery charge resides in the device record; `BatteryPower` bridges it to standard FE item capabilities.

`Connectivity` indexes loaded network nodes per dimension and follows loaded bounded cable graphs. `NetworkNodeEntity` owns FE, owner/configuration, nine-slot inventory and encrypted package storage. Its optional OC2 callback annotations are compile-only, with no API binaries bundled.

`CallBook` validates call lifecycle; `PhoneCalls` validates online ownership, voice session, power, SIM and coverage. The optional Simple Voice Chat plugin reroutes accepted microphone packets only to the peer of an active call, cancelling proximity forwarding first. Server shutdown/logout and liveness polling clean up sessions and calls.

The app/service SDK is documented in SDK.md and APP_STUDIO.md. Published apps are declarative UI/service descriptors; packages hold bounded text/data. This is an in-game network model, with no arbitrary remote code or external internet transport.
