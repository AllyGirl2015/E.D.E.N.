package net.realityradio.eden.core;

import java.util.UUID;

/** Declarative app authored in-game. No scripts, command execution or filesystem access. */
public record StudioApp(String id, UUID author, String title, String text, String service) {
    public StudioApp {
        if (id == null || !id.matches("[a-z0-9_]{1,32}"))
            throw new IllegalArgumentException("App IDs use 1–32 lowercase letters, digits or underscores");
        if (author == null || title == null || title.isBlank() || title.length() > 32)
            throw new IllegalArgumentException("App titles use 1–32 characters");
        if (text == null || text.length() > 1024 || service == null || service.length() > 64)
            throw new IllegalArgumentException("App text/service exceeds its size limit");
    }
}
