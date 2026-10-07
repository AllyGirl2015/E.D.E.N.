package net.realityradio.eden.infrastructure;

import com.google.gson.*;
import java.util.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.realityradio.eden.EdenConfig;

public final class NodeNetworking {
  private static final Map<UUID, Long> LAST = new HashMap<>();

  public static void clear() {
    LAST.clear();
  }

  private static boolean editable(ServerPlayer p, NetworkNodeEntity n) {
    return p.hasPermissions(2) || p.getUUID().equals(n.owner);
  }

  public static void open(ServerPlayer p, NetworkNodeEntity n) {
    var d = new JsonObject();
    var pos = n.getBlockPos();
    d.addProperty("x", pos.getX());
    d.addProperty("y", pos.getY());
    d.addProperty("z", pos.getZ());
    d.addProperty("kind", n.kind());
    d.addProperty("carrier", n.carrier);
    d.addProperty("ssid", n.ssid);
    d.addProperty("range", n.requestedRange);
    d.addProperty("effectiveRange", n.range());
    d.addProperty("power", n.powerDraw);
    d.addProperty("energy", n.energy.getEnergyStored());
    d.addProperty("enabled", n.enabled);
    d.addProperty("powered", n.powered);
    d.addProperty("uplink", Connectivity.uplink(p.serverLevel(), n));
    d.addProperty("editable", editable(p, n));
    PacketDistributor.sendToPlayer(p, new NodeSnapshot(d.toString()));
  }

  public static void handle(NodeAction a, IPayloadContext ctx) {
    if (!(ctx.player() instanceof ServerPlayer p)
        || !p.serverLevel().hasChunkAt(a.pos())
        || a.pos().distToCenterSqr(p.position()) > 64) return;
    if (!(p.serverLevel().getBlockEntity(a.pos()) instanceof NetworkNodeEntity n)
        || !editable(p, n)) return;
    long now = p.server.overworld().getGameTime();
    if (now - LAST.getOrDefault(p.getUUID(), now - 10) < 10) return;
    LAST.put(p.getUUID(), now);
    int max =
        n.kind().equals("tower")
            ? EdenConfig.TOWER_MAX_RANGE.get()
            : EdenConfig.ROUTER_MAX_RANGE.get();
    if (a.carrier().isBlank()
        || a.ssid().isBlank()
        || a.range() < 1
        || a.range() > max
        || a.power() < 1
        || a.power() > EdenConfig.MAX_DRAW.get()) {
      p.displayClientMessage(
          Component.literal("Invalid settings; check server range/power limits"), false);
      return;
    }
    if (!a.password().equals("__keep__"))
      n.passwordHash = NetworkPolicy.wifiKey(a.ssid().trim(), a.password());
    else if (!n.ssid.equals(a.ssid().trim())) {
      p.displayClientMessage(
          Component.literal("Re-enter the password when changing the SSID"), false);
      return;
    }
    n.carrier = a.carrier().trim();
    n.ssid = a.ssid().trim();
    n.requestedRange = a.range();
    n.powerDraw = a.power();
    n.enabled = a.enabled();
    n.setChanged();
    open(p, n);
  }

  private NodeNetworking() {}
}
