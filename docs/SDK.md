# E.D.E.N. Java SDK (experimental v0)

All common APIs live under `net.realityradio.eden.api`. Addons target Minecraft 1.21.1, NeoForge and Java 21, declare an `eden` mod dependency and compile against E.D.E.N.'s built JAR. Use a project dependency during joint development, or a local JAR:

```groovy
dependencies {
    implementation files('libs/eden-0.1.0-alpha.1.jar')
}
```

Install both mods on the server and clients. Common descriptors contain no client-only classes. IDs are namespaced; use your addon mod ID, not `eden` or `studio`.

## Register a service and an app

The following handler goes into an addon's common **mod-bus** subscriber. The event is broadcast to every mod bus during common setup, after all addon constructors have subscribed. The registry then freezes. Duplicate IDs fail visibly.

```java
package com.example.town;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.realityradio.eden.api.AppDefinition;
import net.realityradio.eden.api.RegisterEdenPlatformEvent;

@EventBusSubscriber(modid = "town", bus = EventBusSubscriber.Bus.MOD)
public final class TownApps {
    @SubscribeEvent
    public static void register(RegisterEdenPlatformEvent event) {
        var service = ResourceLocation.fromNamespaceAndPath("town", "position");
        event.service(service, (context, arguments) -> {
            var pos = context.player().blockPosition();
            return "Position: " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
        });
        event.app(new AppDefinition(
            ResourceLocation.fromNamespaceAndPath("town", "navigator"),
            "Navigator", "Read your current world position.", service));
    }
}
```

The default app screen renders description text, a JSON argument field and a **Run service** button. No custom client code is necessary for that interface.

## Service authority

`ServiceContext` contains the authenticated `ServerPlayer`, the server's `DeviceRecord` and the `WorldNetwork`. Requests run on the server game thread. E.D.E.N. first verifies that the player is still holding the opened device, or remains within eight blocks of the opened terminal in the same dimension. The submitted device UUID must match that server session.

A service must additionally enforce **its own** permissions, balances, resources, ownership and argument limits. A modified client can invoke any registered service directly; a hidden UI button is not authorization. Do not treat the argument JSON as trusted or run it as commands/scripts. Do not access state from background threads. E.D.E.N. marks its world data dirty after a successful service call; independent addon storage remains the addon's responsibility.

The SIM is a physical bearer token, not tied to a player UUID. World data binds one SIM to one device. Regular clients cannot assign a SIM number or substitute a server inventory stack through these packets. Creative/operator copying preserves an existing device UUID and can share that device's account; there is no claim of anti-cloning protection against administrative item duplication.

## Optional client screens

From your addon's **physical-client** setup handler:

```java
import net.minecraft.resources.ResourceLocation;
import net.realityradio.eden.client.ClientApps;

ClientApps.register(
    ResourceLocation.fromNamespaceAndPath("town", "navigator"),
    (snapshot, parent) -> new YourNavigatorScreen(snapshot, parent));
```

`YourNavigatorScreen` is your own Minecraft `Screen` implementation. Keep all screen imports in client-only classes; loading them on a dedicated server causes crashes. The provided snapshot is a deep copy, and `parent` is the shared launcher screen. A custom screen can return to its parent with `Minecraft.getInstance().setScreen(parent)`.

`DeviceAction` requests use the device UUID from the snapshot. Refresh and server-service actions are available through the same payload transport. This protocol and its field names are experimental, not a stable third-party compatibility promise.

## Persistence

`EdenSavedData.get(server)` resolves the overworld network. Item components `Eden.DEVICE_ID` and `Eden.SIM_ID` identify physical objects. The JSON world format is schema 1 inside `data/eden_network.dat`; unsupported future versions fail instead of overwriting data.

Banking, carriers, energy, routes, internet and voice integration should be independent services/adapters, with explicit permission checks. Those adapters are not present in this alpha. Existing SPhone/Device Mod addons and external modpacks have not been compatibility-tested.
