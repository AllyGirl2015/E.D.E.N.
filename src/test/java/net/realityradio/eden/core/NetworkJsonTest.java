package net.realityradio.eden.core;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import net.realityradio.eden.storage.NetworkJson;
import static org.junit.jupiter.api.Assertions.*;
class NetworkJsonTest {
    @Test void saveReloadPreservesSimBindingMessagesContactsNotesAppsAndNumberCounter() {
        var n = new WorldNetwork(); var d = new DeviceRecord(UUID.randomUUID(), "tablet"); n.devices.put(d.id, d);
        var sim = n.issueSim("Alyon"); n.insert(d.id, sim.id); n.send(d.id, sim.number, "Saved", 42);
        n.contact(d.id, "1000001", "Alissa"); d.notes = "A persistent note";
        var author = UUID.randomUUID(); n.publishApp(new StudioApp("news", author, "News", "News app", "studio:news")); n.publishBulletin("news", author, "Broadcast");
        var restored = NetworkJson.decode(NetworkJson.encode(n));
        assertEquals("Saved", restored.account(d.id).messages.getFirst().body());
        assertEquals("Alissa", restored.account(d.id).contacts.get("1000001"));
        assertEquals("A persistent note", restored.device(d.id).notes);
        assertEquals("studio:news", restored.apps.get("news").service());
        assertEquals(author, restored.bulletins.get("news").author());
        assertEquals("1000001", restored.issueSim("Alyon").number);
        restored.eject(d.id); assertNull(restored.sims.get(sim.id).installedDevice);
    }
    @Test void futureSchemaFailsRatherThanOverwritingWorldState() {
        assertThrows(IllegalStateException.class, () -> NetworkJson.decode("{\"schema\":99,\"network\":{}}"));
    }
    @Test void emptyWorldRoundTripCanCreateAndUseDevice() {
        var restored = NetworkJson.decode(NetworkJson.encode(new WorldNetwork()));
        var d = new DeviceRecord(UUID.randomUUID(), "phone"); restored.devices.put(d.id, d);
        var sim = restored.issueSim("Alyon"); restored.insert(d.id, sim.id);
        assertEquals(sim.number, restored.account(d.id).number);
    }
    @Test void incompleteCurrentSchemaDoesNotBecomeAnEmptyWorld() {
        assertThrows(IllegalStateException.class, () -> NetworkJson.decode("{\"schema\":1,\"network\":{}}"));
    }
    @Test void inconsistentSimBindingIsRejectedOnReload() {
        var n = new WorldNetwork(); var d = new DeviceRecord(UUID.randomUUID(), "phone"); n.devices.put(d.id, d);
        var sim = n.issueSim("Alyon"); n.insert(d.id, sim.id); sim.installedDevice = UUID.randomUUID();
        assertThrows(IllegalStateException.class, () -> NetworkJson.decode(NetworkJson.encode(n)));
    }

}
