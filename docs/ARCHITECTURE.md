# Architecture

| Layer | Responsibility | Persistence |
|---|---|---|
| Item components | Physical device UUID and SIM UUID | ItemStack native components |
| Desktop block entity | Placed device UUID | Block entity NBT |
| Domain network | SIM/number bindings, SMS, contacts, device notes/settings, Studio catalog | Overworld SavedData |
| Server transport | Authenticate opened device, check current access, validate/rate-limit actions | Ephemeral session map; cleared at server stop/logout |
| Client UI | Shared launcher, native screens, drafts and rendering | Drafts only while the screen exists |
| Java SDK | App descriptors, trusted service handlers, optional client screen factories | Addon-owned behavior; E.D.E.N. state marked dirty after successful calls |

The server issues numbers and owns all account records. A device item carries no inbox. Removing a SIM disconnects its device without deleting the account, and offline SMS remains in the account. Provider is currently a label; there is no simulated tower requirement.

All dimensions resolve the overworld's `eden_network` data file so traveling between dimensions does not split accounts. Save with the world; back up the **whole world**, including inventories, block entities and `data/eden_network.dat`.

The world schema is versioned. This alpha does not import old SPhone SQL databases and makes no compatibility promise with SPhone save data. New devices initialize on first use; fresh SIMs register on first insertion.

Clients receive the opened device's recent SMS/contacts/settings and the public app catalog. They do not receive other accounts' histories. Request string lengths are bounded on the wire, and accepted requests are limited to once every four game ticks per opened session. One SIM cannot be inserted into two different registered devices through normal gameplay. Administrative copies of UUID-bearing items can share the original identity; physical uniqueness is not cryptographically guaranteed.

The initial module layout keeps pure domain logic independent of Minecraft. GUI dependencies remain client-only. This permits meaningful unit tests for transfer, messaging, ownership and persistence without starting the game, while registry/GUI/server integration still needs Minecraft runtime checks.
