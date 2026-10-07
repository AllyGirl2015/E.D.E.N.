package net.realityradio.eden.network;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.zip.*;
import org.junit.jupiter.api.Test;

class SnapshotCompressionTest {
  @Test
  void fullInboxSurvivesWireEncoding() {
    String s = "🌱 long message ".repeat(30000);
    var wire = SnapshotCompression.encode(s);
    assertTrue(wire.length < SnapshotCompression.MAX_WIRE);
    assertEquals(s, SnapshotCompression.decode(wire));
  }

  @Test
  void truncatedAndExpansionBombsAreRejected() throws Exception {
    assertThrows(
        IllegalArgumentException.class, () -> SnapshotCompression.decode(new byte[] {1, 2}));
    var out = new ByteArrayOutputStream();
    try (var z = new GZIPOutputStream(out)) {
      z.write(new byte[SnapshotCompression.MAX_JSON + 1]);
    }
    assertThrows(
        IllegalArgumentException.class, () -> SnapshotCompression.decode(out.toByteArray()));
  }
}
