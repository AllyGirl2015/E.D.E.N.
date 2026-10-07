package net.realityradio.eden.infrastructure;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.HexFormat;

public final class NetworkPolicy {
  public static String wifiKey(String ssid, String password) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256")
                  .digest((ssid + "\0" + password).getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  public static int radius(int target, int power, int baseRange, int baseFE, int max) {
    return Math.max(
        1,
        Math.min(
            Math.min(target, max),
            (int) (baseRange * Math.sqrt(Math.max(0, power) / (double) Math.max(1, baseFE)))));
  }

  private NetworkPolicy() {}
}
