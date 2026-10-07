package net.realityradio.eden.infrastructure;
import net.minecraft.core.BlockPos;import net.minecraft.network.RegistryFriendlyByteBuf;import net.minecraft.network.codec.StreamCodec;import net.minecraft.network.protocol.common.custom.CustomPacketPayload;import net.realityradio.eden.Eden;
public record NodeAction(BlockPos pos,String carrier,String ssid,String password,int range,int power,boolean enabled)implements CustomPacketPayload {
 public static final Type<NodeAction>TYPE=new Type<>(Eden.id("node_action"));
 public static final StreamCodec<RegistryFriendlyByteBuf,NodeAction>CODEC=StreamCodec.of((b,p)->{b.writeBlockPos(p.pos);b.writeUtf(p.carrier,48);b.writeUtf(p.ssid,48);b.writeUtf(p.password,128);b.writeVarInt(p.range);b.writeVarInt(p.power);b.writeBoolean(p.enabled);},b->new NodeAction(b.readBlockPos(),b.readUtf(48),b.readUtf(48),b.readUtf(128),b.readVarInt(),b.readVarInt(),b.readBoolean()));
 public Type<NodeAction>type(){return TYPE;}
}
