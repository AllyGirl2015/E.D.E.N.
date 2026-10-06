package net.realityradio.eden.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DeviceSnapshot(String json, boolean open) implements CustomPacketPayload {
    public static final Type<DeviceSnapshot> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("eden", "snapshot"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DeviceSnapshot> CODEC = StreamCodec.of(
        (buf, p) -> { buf.writeUtf(p.json, 196608); buf.writeBoolean(p.open); },
        buf -> new DeviceSnapshot(buf.readUtf(196608), buf.readBoolean()));
    @Override public Type<DeviceSnapshot> type() { return TYPE; }
}
