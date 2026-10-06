package net.realityradio.eden.network;

import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** String fields have explicit wire bounds; no client identity/account is accepted. */
public record DeviceAction(UUID device, String action, String a, String b, String c) implements CustomPacketPayload {
    public static final Type<DeviceAction> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("eden", "action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DeviceAction> CODEC = StreamCodec.of(
        (buf, p) -> { buf.writeUUID(p.device); buf.writeUtf(p.action, 32); buf.writeUtf(p.a, 64); buf.writeUtf(p.b, 2048); buf.writeUtf(p.c, 128); },
        buf -> new DeviceAction(buf.readUUID(), buf.readUtf(32), buf.readUtf(64), buf.readUtf(2048), buf.readUtf(128)));
    @Override public Type<DeviceAction> type() { return TYPE; }
}
