package net.realityradio.eden.infrastructure;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.realityradio.eden.Eden;

public record NodeSnapshot(String json) implements CustomPacketPayload {
  public static final Type<NodeSnapshot> TYPE = new Type<>(Eden.id("node_snapshot"));
  public static final StreamCodec<RegistryFriendlyByteBuf, NodeSnapshot> CODEC =
      StreamCodec.of((b, p) -> b.writeUtf(p.json, 4096), b -> new NodeSnapshot(b.readUtf(4096)));

  public Type<NodeSnapshot> type() {
    return TYPE;
  }
}
