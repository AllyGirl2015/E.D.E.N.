package net.realityradio.eden.core;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WorldNetworkTest {
    private static DeviceRecord device(WorldNetwork network) {
        var d = new DeviceRecord(UUID.randomUUID(), "phone"); network.devices.put(d.id, d); return d;
    }
    @Test void simTransferKeepsNumberMessagesAndContacts() {
        var network = new WorldNetwork(); var a = device(network); var b = device(network); var replacement = device(network);
        var simA = network.issueSim("Alyon"); var simB = network.issueSim("Alyon");
        network.insert(a.id, simA.id); network.insert(b.id, simB.id);
        network.contact(a.id, simB.number, "Friend"); network.send(b.id, simA.number, "Hello", 1);
        network.insert(replacement.id, network.eject(a.id));
        assertEquals(simA.number, network.account(replacement.id).number);
        assertEquals("Friend", network.account(replacement.id).contacts.get(simB.number));
        assertEquals("Hello", network.account(replacement.id).messages.getFirst().body());
        assertThrows(IllegalArgumentException.class, () -> network.account(a.id));
    }
    @Test void cannotInsertSameSimInTwoDevices() {
        var network = new WorldNetwork(); var a = device(network); var b = device(network); var sim = network.issueSim("Alyon");
        network.insert(a.id, sim.id);
        assertThrows(IllegalArgumentException.class, () -> network.insert(b.id, sim.id));
        assertNull(b.sim); assertEquals(a.id, sim.installedDevice);
    }
    @Test void failedInsertDoesNotReplaceExistingSim() {
        var network = new WorldNetwork(); var a = device(network); var sim = network.issueSim("Alyon");
        network.insert(a.id, sim.id);
        assertThrows(IllegalArgumentException.class, () -> network.insert(a.id, UUID.randomUUID()));
        assertEquals(sim.id, a.sim);
    }
    @Test void offlineRecipientGetsMessageAfterInsertion() {
        var network = new WorldNetwork(); var a = device(network); var b = device(network);
        var simA = network.issueSim("Alyon"); var simB = network.issueSim("Alyon"); network.insert(a.id, simA.id);
        network.send(a.id, simB.number, "Offline mail", 2); network.insert(b.id, simB.id);
        assertEquals("Offline mail", network.account(b.id).messages.getFirst().body());
    }
    @Test void messageRetentionAndSelfMessagingAreBounded() {
        var network = new WorldNetwork(); var d = device(network); var sim = network.issueSim("Alyon"); network.insert(d.id, sim.id);
        for (int i = 0; i < 150; i++) network.send(d.id, sim.number, "Message " + i, i);
        assertEquals(100, sim.messages.size()); assertEquals("Message 50", sim.messages.getFirst().body());
    }
    @Test void invalidMessageDoesNotMutateEitherInbox() {
        var network = new WorldNetwork(); var a = device(network); var b = device(network);
        var simA = network.issueSim("Alyon"); var simB = network.issueSim("Alyon"); network.insert(a.id, simA.id); network.insert(b.id, simB.id);
        assertThrows(IllegalArgumentException.class, () -> network.send(a.id, simB.number, "x".repeat(513), 1));
        assertThrows(IllegalArgumentException.class, () -> network.send(a.id, simB.number, "   ", 1));
        assertTrue(simA.messages.isEmpty()); assertTrue(simB.messages.isEmpty());
    }
    @Test void unknownNumbersDoNotCreateAccounts() {
        var network = new WorldNetwork(); var d = device(network); var sim = network.issueSim("Alyon"); network.insert(d.id, sim.id);
        assertThrows(IllegalArgumentException.class, () -> network.send(d.id, "9999999", "Hello", 1));
        assertEquals(1, network.sims.size()); assertTrue(sim.messages.isEmpty());
    }
    @Test void appAndServiceOwnershipAreEnforced() {
        var network = new WorldNetwork(); var alice = UUID.randomUUID(); var bob = UUID.randomUUID();
        network.publishApp(new StudioApp("news", alice, "News", "Hello", ""));
        assertThrows(IllegalArgumentException.class, () -> network.publishApp(new StudioApp("news", bob, "Stolen", "", "")));
        network.publishBulletin("news", alice, "World news");
        assertThrows(IllegalArgumentException.class, () -> network.publishBulletin("news", bob, "Hijack"));
        assertEquals("Hello", network.apps.get("news").text()); assertEquals("World news", network.bulletins.get("news").text());
    }
    @Test void issuedNumbersAreUniqueAndIncreasing() {
        var network = new WorldNetwork();
        assertEquals("1000000", network.issueSim("Alyon").number);
        assertEquals("1000001", network.issueSim("Alyon").number);
    }
    @Test void publishingRejectsOversizedAndInvalidApps() {
        assertThrows(IllegalArgumentException.class, () -> new StudioApp("../file", UUID.randomUUID(), "Title", "", ""));
        assertThrows(IllegalArgumentException.class, () -> new StudioApp("app", UUID.randomUUID(), "", "", ""));
        assertThrows(IllegalArgumentException.class, () -> new StudioApp("app", UUID.randomUUID(), "Title", "x".repeat(1025), ""));
    }
    @Test void catalogCapAllowsUpdatingExistingApps() {
        var n = new WorldNetwork(); var author = UUID.randomUUID();
        for (int i = 0; i < 128; i++) n.publishApp(new StudioApp("app" + i, author, "App", "", ""));
        assertThrows(IllegalArgumentException.class, () -> n.publishApp(new StudioApp("extra", author, "App", "", "")));
        n.publishApp(new StudioApp("app0", author, "Updated", "", ""));
        assertEquals("Updated", n.apps.get("app0").title());
    }
}
