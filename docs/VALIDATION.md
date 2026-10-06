# Alpha validation

Validated locally against Minecraft 1.21.1, NeoForge 21.1.252, Gradle 9.2.1 and Java 21.

| Check | Result |
|---|---|
| Compile and package runtime/sources JARs | Passed |
| JUnit core/storage/calculator tests | 19 passed, 0 failed |
| Headless Minecraft GameTests | 3 passed, 0 failed |
| Documented Java addon example compilation | Passed |
| Resource JSON parsing and Git whitespace check | Passed |
| JAR metadata/notices and absence of legacy JDBC/ACS binaries | Checked |
| Real client GUI and multiplayer playtesting | Not performed |
| External modpack, voice, Create/energy compatibility | Not performed; adapters not implemented |

GameTests exercise registered device items/components and a desktop block entity, the broadcast SDK registration event, native SavedData serialization, and survival desktop breaking/replacement with SIM ejection and notes retention.

Unit tests exercise SIM transfers, single-device binding, failed-operation behavior, offline/self SMS, message retention, contact persistence, unique numbering, app/service ownership, catalog limits, calculator parsing, native-format JSON round trips and rejection of future/incomplete/inconsistent save data.

The sandbox does not expose Java's own process path through ProcessHandle. For local Minecraft artifact generation only, two lookups in NeoForm Runtime 2.0.31 were given a fallback to `java.home/bin/java` through a temporary local build-tool JAR and init script. No global tool cache, project dependency version, Minecraft code or mod source was altered for that workaround. Proxy and standard trust-store settings were local to execution. The checked-in project and GitHub workflow use unmodified standard build tools. GitHub CI results are tracked separately in the repository Actions tab; the validation results above are local.

Build command: `./gradlew build`. Minecraft integration command: `./gradlew runGameTestServer`.

The test world and intermediate build tools are excluded from source control. The historical SPhone upload remains unchanged: SHA-256 `f3adb8e961c823b11ed50e1c8d8ff7b574ea3a570a242afb83702c3b594e8365`.

This validates an initial platform alpha, not full SPhone feature parity or the completed E.D.E.N. project.
