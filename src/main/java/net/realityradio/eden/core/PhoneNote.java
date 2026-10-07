package net.realityradio.eden.core;

/** SPhone Note model adapted to bounded, native world persistence. */
public record PhoneNote(String id, String title, String text, long date) {
  public PhoneNote {
    if (id == null
        || id.isBlank()
        || id.length() > 40
        || title == null
        || title.length() > 48
        || text == null
        || text.length() > 2048)
      throw new IllegalArgumentException("Invalid note (title 48, text 2048 characters)");
  }
}
