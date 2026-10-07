/* Adapted from SPhone Message.java, Apache-2.0. See third_party/sphone/NOTICE. */
package net.realityradio.eden.core;

import java.util.UUID;

/** Immutable message; the SIM account is its address, never a client-supplied player name. */
public record PhoneMessage(UUID id, String sender, String receiver, String body, long timestamp) {
  public PhoneMessage {
    if (id == null || sender == null || receiver == null || body == null)
      throw new IllegalArgumentException("Message fields are required");
    if (body.isBlank() || body.length() > 512)
      throw new IllegalArgumentException("Messages must contain 1–512 characters");
  }
}
