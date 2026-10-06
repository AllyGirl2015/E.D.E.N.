package net.realityradio.eden.client;

import com.google.gson.JsonObject;
import java.util.*;
import java.util.function.BiFunction;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;

/** Optional Java addon renderers. Access only from physical-client code. */
public final class ClientApps {
    private static final Map<ResourceLocation, BiFunction<JsonObject, Screen, Screen>> SCREENS = new HashMap<>();
    private ClientApps() {}
    public static void register(ResourceLocation id, BiFunction<JsonObject, Screen, Screen> factory) {
        if (SCREENS.putIfAbsent(id, Objects.requireNonNull(factory)) != null)
            throw new IllegalArgumentException("Duplicate client app renderer " + id);
    }
    static Screen create(ResourceLocation id, JsonObject snapshot, Screen parent) {
        var factory = SCREENS.get(id);
        return factory == null ? null : factory.apply(snapshot.deepCopy(), parent);
    }
}
