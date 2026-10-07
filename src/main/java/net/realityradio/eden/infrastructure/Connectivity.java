package net.realityradio.eden.infrastructure;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.world.level.Level;
import net.realityradio.eden.*;
import net.realityradio.eden.core.DeviceRecord;
import net.realityradio.eden.storage.EdenSavedData;

public final class Connectivity {
  private static final Map<ResourceKey<Level>, Map<BlockPos, NetworkNodeEntity>> NODES =
      new HashMap<>();

  public record Link(boolean online, String mode, String network, BlockPos endpoint) {}

  public static void add(ServerLevel l, NetworkNodeEntity n) {
    NODES.computeIfAbsent(l.dimension(), d -> new HashMap<>()).put(n.getBlockPos(), n);
  }

  public static void remove(ServerLevel l, BlockPos p) {
    var ns = NODES.get(l.dimension());
    if (ns != null) ns.remove(p);
  }

  public static void clear() {
    NODES.clear();
  }

  public static Collection<NetworkNodeEntity> nodes(ServerLevel l) {
    return NODES.getOrDefault(l.dimension(), Map.of()).values();
  }

  public static Set<NetworkNodeEntity> wired(ServerLevel l, BlockPos start) {
    Set<BlockPos> seen = new HashSet<>();
    Set<NetworkNodeEntity> found = new LinkedHashSet<>();
    var q = new ArrayDeque<BlockPos>();
    q.add(start);
    while (!q.isEmpty() && seen.size() < EdenConfig.CABLE_LIMIT.get()) {
      var p = q.removeFirst();
      if (!seen.add(p) || !l.hasChunkAt(p)) continue;
      var s = l.getBlockState(p);
      boolean cable = s.is(Eden.CABLE.get()) || s.is(Eden.WAN_CABLE.get());
      var entity = l.getBlockEntity(p);
      if (entity instanceof NetworkNodeEntity n) {
        if (!n.operating()) continue;
        found.add(n);
      } else if (!cable && !p.equals(start)) continue;
      for (var d : Direction.values()) q.add(p.relative(d));
    }
    return found;
  }

  public static boolean uplink(ServerLevel l, NetworkNodeEntity n) {
    return !EdenConfig.REQUIRE_BACKHAUL.get()
        || wired(l, n.getBlockPos()).stream().anyMatch(x -> x.kind().equals("gateway"));
  }

  public static Link link(ServerPlayer p, DeviceRecord d, BlockPos terminal) {
    if (!EdenConfig.REQUIRE_NETWORK.get())
      return new Link(true, "Creative", "Network requirements disabled", null);
    var l = p.serverLevel();
    if (terminal != null) {
      var w = wired(l, terminal);
      var endpoint = w.stream().filter(n -> n.kind().equals("gateway")).findFirst();
      if (endpoint.isPresent()) return new Link(true, "Ethernet", endpoint.get().carrier, terminal);
      if (!w.isEmpty()) return new Link(false, "LAN", "Local Ethernet network", terminal);
    }
    for (var n : nodes(l)) {
      if (!n.operating()
          || !n.kind().equals("router")
          || n.getBlockPos().distToCenterSqr(p.position()) > (double) n.range() * n.range()
          || !n.ssid.equals(d.wifiSsid)) continue;
      boolean open = n.passwordHash.equals(NetworkPolicy.wifiKey(n.ssid, ""));
      if (!open && !n.passwordHash.equals(d.wifiKey)) continue;
      return new Link(uplink(l, n), "Wi-Fi", n.ssid, n.getBlockPos());
    }
    if (d.sim != null) {
      var a = EdenSavedData.get(p.server).network.account(d.id);
      for (var n : nodes(l)) {
        if (!n.operating() || !n.kind().equals("tower") || !n.carrier.equals(a.carrier)) continue;
        if (n.getBlockPos().distToCenterSqr(p.position()) <= (double) n.range() * n.range()
            && uplink(l, n)) return new Link(true, "Cellular", n.carrier, n.getBlockPos());
      }
    }
    return new Link(false, "Offline", "No powered network coverage", null);
  }

  public static Link mobileLink(ServerPlayer p, DeviceRecord d) {
    return link(p, d, null);
  }

  public static List<NetworkNodeEntity> racks(ServerPlayer p, DeviceRecord d, BlockPos terminal) {
    var c = link(p, d, terminal);
    return c.endpoint == null
        ? List.of()
        : wired(p.serverLevel(), c.endpoint).stream().filter(n -> n.kind().equals("rack")).toList();
  }

  private Connectivity() {}
}
