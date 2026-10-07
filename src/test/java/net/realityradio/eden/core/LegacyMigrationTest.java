package net.realityradio.eden.core;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.*;
import java.util.*;
import net.realityradio.eden.storage.NetworkJson;
import org.junit.jupiter.api.Test;

class LegacyMigrationTest {
  @Test
  void alphaOneNotesSurviveAdditiveUpgrade() {
    var n = new WorldNetwork();
    var d = new DeviceRecord(UUID.randomUUID(), "phone");
    d.notes = "My old note";
    n.devices.put(d.id, d);
    var json = JsonParser.parseString(NetworkJson.encode(n)).getAsJsonObject();
    var device =
        json.getAsJsonObject("network").getAsJsonObject("devices").getAsJsonObject(d.id.toString());
    for (var key : List.of("wallpaper", "wifiSsid", "wifiKey", "installedApps", "notebook"))
      device.remove(key);
    var restored = NetworkJson.decode(json.toString()).device(d.id);
    assertEquals("My old note", restored.notebook.get("legacy").text());
    assertEquals("b1", restored.wallpaper);
    assertTrue(restored.installedApps.contains("eden:messages"));
  }
}
