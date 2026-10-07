package net.realityradio.eden.core;

import java.util.*;

/** Server-owned domain state, independent of Minecraft and storage implementation. */
public final class WorldNetwork {
  public static final int SCHEMA = 1;
  public final Map<UUID, SimAccount> sims = new LinkedHashMap<>();
  public final Map<UUID, DeviceRecord> devices = new LinkedHashMap<>();
  public final Map<String, StudioApp> apps = new LinkedHashMap<>();
  public final Map<String, Bulletin> bulletins = new LinkedHashMap<>();
  private long nextNumber = 1000000;

  public long nextNumber() {
    return nextNumber;
  }

  public void restoreNextNumber(long value) {
    if (value < 1000000) throw new IllegalArgumentException("Invalid next number");
    nextNumber = value;
  }

  public SimAccount issueSim(String carrier) {
    String number;
    do {
      number = Long.toString(nextNumber++);
    } while (findNumber(number).isPresent());
    var sim = new SimAccount(UUID.randomUUID(), number, carrier);
    sims.put(sim.id, sim);
    return sim;
  }

  public Optional<SimAccount> findNumber(String number) {
    return sims.values().stream().filter(s -> s.number.equals(number)).findFirst();
  }

  public DeviceRecord device(UUID id) {
    var device = devices.get(id);
    if (device == null) throw new IllegalArgumentException("Unknown device");
    return device;
  }

  public SimAccount account(UUID deviceId) {
    var device = device(deviceId);
    var sim = sims.get(device.sim);
    if (sim == null || !deviceId.equals(sim.installedDevice))
      throw new IllegalArgumentException("Insert an active SIM first");
    return sim;
  }

  public void insert(UUID deviceId, UUID simId) {
    var device = device(deviceId);
    var sim = sims.get(simId);
    if (device.sim != null) throw new IllegalArgumentException("Eject the current SIM first");
    if (sim == null) throw new IllegalArgumentException("Unknown SIM");
    if (sim.installedDevice != null)
      throw new IllegalArgumentException("This SIM is already installed");
    device.sim = simId;
    sim.installedDevice = deviceId;
  }

  public UUID eject(UUID deviceId) {
    var device = device(deviceId);
    var sim = account(deviceId);
    device.sim = null;
    sim.installedDevice = null;
    return sim.id;
  }

  public PhoneMessage send(UUID deviceId, String number, String body, long now) {
    var from = account(deviceId);
    var to = findNumber(number).orElseThrow(() -> new IllegalArgumentException("Number not found"));
    var message = new PhoneMessage(UUID.randomUUID(), from.number, to.number, body, now);
    from.receive(message);
    if (to != from) to.receive(message);
    return message;
  }

  public void contact(UUID deviceId, String number, String name) {
    if (!number.matches("[0-9]{1,20}") || name.isBlank() || name.length() > 48)
      throw new IllegalArgumentException(
          "Provide a valid number and a name of up to 48 characters");
    var account = account(deviceId);
    if (!account.contacts.containsKey(number) && account.contacts.size() >= 64)
      throw new IllegalArgumentException("Contact list is full (64)");
    account.contacts.put(number, name);
  }

  public void publishApp(StudioApp app) {
    var old = apps.get(app.id());
    if (old != null && !old.author().equals(app.author()))
      throw new IllegalArgumentException("That app belongs to another developer");
    if (old == null && apps.size() >= 128)
      throw new IllegalArgumentException("World app catalog is full (128)");
    apps.put(app.id(), app);
  }

  public void publishBulletin(String id, UUID author, String text) {
    if (!id.matches("[a-z0-9_]{1,32}") || text.length() > 1024)
      throw new IllegalArgumentException("Invalid service ID or text (maximum 1024)");
    var old = bulletins.get(id);
    if (old != null && !old.author().equals(author))
      throw new IllegalArgumentException("That service belongs to another developer");
    if (old == null && bulletins.size() >= 128)
      throw new IllegalArgumentException("Service catalog is full (128)");
    bulletins.put(id, new Bulletin(author, text));
  }

  public record Bulletin(UUID author, String text) {}
}
