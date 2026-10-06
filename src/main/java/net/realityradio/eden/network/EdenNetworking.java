package net.realityradio.eden.network;

import java.util.function.Consumer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class EdenNetworking {
    /** Installed only by the client mod entrypoint; dedicated servers never resolve client classes. */
    public static Consumer<DeviceSnapshot> clientReceiver = snapshot -> {};
    private EdenNetworking() {}
    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToServer(DeviceAction.TYPE, DeviceAction.CODEC, ServerDevices::handle);
        registrar.playToClient(DeviceSnapshot.TYPE, DeviceSnapshot.CODEC, (snapshot, context) -> clientReceiver.accept(snapshot));
    }
}
