package net.realityradio.eden.network;

import java.util.function.Consumer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class EdenNetworking {
    /** Installed only by the client mod entrypoint; dedicated servers never resolve client classes. */
    public static Consumer<net.realityradio.eden.infrastructure.NodeSnapshot> nodeReceiver = snapshot -> {};
    public static Consumer<DeviceSnapshot> clientReceiver = snapshot -> {};
    private EdenNetworking() {}
    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("2");
        registrar.playToServer(net.realityradio.eden.infrastructure.NodeAction.TYPE,net.realityradio.eden.infrastructure.NodeAction.CODEC,net.realityradio.eden.infrastructure.NodeNetworking::handle);
        registrar.playToClient(net.realityradio.eden.infrastructure.NodeSnapshot.TYPE,net.realityradio.eden.infrastructure.NodeSnapshot.CODEC,(payload,ctx)->nodeReceiver.accept(payload));
        registrar.playToServer(DeviceAction.TYPE, DeviceAction.CODEC, ServerDevices::handle);
        registrar.playToClient(DeviceSnapshot.TYPE, DeviceSnapshot.CODEC, (snapshot, context) -> clientReceiver.accept(snapshot));
    }
}
