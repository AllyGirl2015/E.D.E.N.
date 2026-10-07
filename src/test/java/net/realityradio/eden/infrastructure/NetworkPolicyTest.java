package net.realityradio.eden.infrastructure;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class NetworkPolicyTest {
  @Test
  void powerAndServerRangeCapCoverage() {
    assertEquals(128, NetworkPolicy.radius(1024, 32, 128, 32, 1024));
    assertEquals(256, NetworkPolicy.radius(1024, 128, 128, 32, 1024));
    assertEquals(100, NetworkPolicy.radius(1024, 128, 128, 32, 100));
  }

  @Test
  void credentialIsBoundToSsid() {
    assertNotEquals(
        NetworkPolicy.wifiKey("home", "secret"), NetworkPolicy.wifiKey("office", "secret"));
    assertNotEquals(NetworkPolicy.wifiKey("home", "secret"), NetworkPolicy.wifiKey("home", ""));
  }
}
