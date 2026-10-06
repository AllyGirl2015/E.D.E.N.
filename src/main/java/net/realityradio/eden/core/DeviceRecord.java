package net.realityradio.eden.core;

import java.util.*;

public final class DeviceRecord {
    public final UUID id;
    public final String kind;
    public UUID sim;
    public String notes = "";
    public String theme = "magenta";
    public DeviceRecord(UUID id, String kind) {
        this.id = Objects.requireNonNull(id);
        this.kind = Objects.requireNonNull(kind);
    }
}
