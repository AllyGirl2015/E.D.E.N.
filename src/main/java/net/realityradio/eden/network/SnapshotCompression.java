package net.realityradio.eden.network;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.*;

public final class SnapshotCompression {
  public static final int MAX_WIRE = 900000, MAX_JSON = 2 * 1024 * 1024;

  public static byte[] encode(String text) {
    byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
    if (bytes.length > MAX_JSON) throw new IllegalArgumentException("Snapshot too large");
    try (var out = new ByteArrayOutputStream()) {
      try (var zip = new GZIPOutputStream(out)) {
        zip.write(bytes);
      }
      byte[] result = out.toByteArray();
      if (result.length > MAX_WIRE)
        throw new IllegalArgumentException("Compressed snapshot too large");
      return result;
    } catch (IOException e) {
      throw new IllegalArgumentException("Cannot encode snapshot", e);
    }
  }

  public static String decode(byte[] data) {
    if (data.length > MAX_WIRE) throw new IllegalArgumentException("Snapshot too large");
    try (var zip = new GZIPInputStream(new ByteArrayInputStream(data));
        var out = new ByteArrayOutputStream()) {
      byte[] b = new byte[4096];
      int read;
      while ((read = zip.read(b)) != -1) {
        if (out.size() + read > MAX_JSON) throw new IllegalArgumentException("Snapshot too large");
        out.write(b, 0, read);
      }
      return out.toString(StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalArgumentException("Invalid compressed snapshot", e);
    }
  }

  private SnapshotCompression() {}
}
