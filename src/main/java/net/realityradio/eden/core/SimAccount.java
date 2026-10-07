package net.realityradio.eden.core;

import java.util.*;

public final class SimAccount {
  public final UUID id;
  public final String number;
  public String carrier;
  public UUID installedDevice;
  public final Map<String, String> contacts = new LinkedHashMap<>();
  public final List<PhoneMessage> messages = new ArrayList<>();

  public SimAccount(UUID id, String number, String carrier) {
    this.id = Objects.requireNonNull(id);
    this.number = Objects.requireNonNull(number);
    this.carrier = Objects.requireNonNull(carrier);
  }

  public void receive(PhoneMessage message) {
    messages.add(message);
    if (messages.size() > 100) messages.removeFirst();
  }
}
