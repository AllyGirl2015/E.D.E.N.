package net.realityradio.eden.api;

import net.minecraft.resources.ResourceLocation;
import java.util.Objects;

/** Common-side descriptor. Client renderers are registered separately to keep servers safe. */
public record AppDefinition(ResourceLocation id, String title, String description, ResourceLocation service) {
    public AppDefinition {
        Objects.requireNonNull(id);
        if (title == null || title.isBlank() || title.length() > 48)
            throw new IllegalArgumentException("App title is required (maximum 48)");
        if (description == null || description.length() > 1024)
            throw new IllegalArgumentException("App description exceeds 1024 characters");
    }
}
