package net.realityradio.eden.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;

class BatteryPackTest {
  @Test
  void lifetimesAndInstalledDrain() {
    String[] types = {"coal", "copper", "iron", "gold", "diamond", "netherite"};
    int[] days = {8, 15, 30, 40, 60, 80};
    for (int i = 0; i < types.length; i++) {
      var b = new BatteryPack(UUID.randomUUID(), types[i], Integer.MAX_VALUE, 100);
      assertEquals(days[i] * 24000, b.charge);
      b.advance(100 + days[i] * 24000 - 1);
      assertTrue(b.powered());
      b.advance(100 + days[i] * 24000);
      assertFalse(b.powered());
    }
  }

  @Test
  void pausedTicksAndCreativeDoNotDrain() {
    var b = new BatteryPack(UUID.randomUUID(), "coal", 50, 100);
    assertFalse(b.advance(100));
    assertEquals(50, b.charge);
    var c = new BatteryPack(UUID.randomUUID(), "creative", 0, 0);
    c.advance(Long.MAX_VALUE);
    assertTrue(c.powered());
  }

  @Test
  void installedStateRoundTripContinuesDrain() {
    var n = new WorldNetwork();
    var d = new DeviceRecord(UUID.randomUUID(), "phone");
    d.battery = new BatteryPack(UUID.randomUUID(), "iron", 500, 10);
    n.devices.put(d.id, d);
    var copy =
        net.realityradio.eden.storage.NetworkJson.decode(
                net.realityradio.eden.storage.NetworkJson.encode(n))
            .device(d.id);
    assertEquals(d.battery.id, copy.battery.id);
    copy.battery.advance(20);
    assertEquals(490, copy.battery.charge);
  }
}
