package net.realityradio.eden.api;

import java.util.*;
import net.minecraft.resources.ResourceLocation;

public final class EdenPlatform {
    private static final Map<ResourceLocation, AppDefinition> APPS = new LinkedHashMap<>();
    private static final Map<ResourceLocation, EdenService> SERVICES = new LinkedHashMap<>();
    private static boolean frozen;
    private EdenPlatform() {}
    public static void registerApp(AppDefinition app) {
        if (frozen) throw new IllegalStateException("Register apps during RegisterEdenPlatformEvent");
        if (APPS.putIfAbsent(app.id(), app) != null) throw new IllegalArgumentException("Duplicate app " + app.id());
    }
    public static void registerService(ResourceLocation id, EdenService service) {
        if (frozen) throw new IllegalStateException("Register services during RegisterEdenPlatformEvent");
        Objects.requireNonNull(service);
        if (SERVICES.putIfAbsent(id, service) != null) throw new IllegalArgumentException("Duplicate service " + id);
    }
    public static Collection<AppDefinition> apps() { return List.copyOf(APPS.values()); }
    public static Optional<EdenService> service(ResourceLocation id) { return Optional.ofNullable(SERVICES.get(id)); }
    public static void freeze() { frozen = true; }
}
