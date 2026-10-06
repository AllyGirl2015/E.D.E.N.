package net.realityradio.eden.storage;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.LevelResource;
import java.nio.file.Files;
import net.realityradio.eden.core.WorldNetwork;

/** One shared network per server, stored in the overworld across every dimension. */
public final class EdenSavedData extends SavedData {
    public final WorldNetwork network;
    private EdenSavedData() { network = new WorldNetwork(); }
    private EdenSavedData(WorldNetwork network) { this.network = network; }
    public static EdenSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(() -> {
                    // Minecraft catches reader failures and falls back to the supplier.
                    // Never replace an existing unreadable/future-format data file with an empty network.
                    var file = server.getWorldPath(LevelResource.ROOT).resolve("data/eden_network.dat");
                    if (Files.exists(file))
                        throw new IllegalStateException("Cannot read existing E.D.E.N. world data; restore a compatible version or backup");
                    return new EdenSavedData();
                }, EdenSavedData::load, null), "eden_network");
    }
    private static EdenSavedData load(CompoundTag tag, HolderLookup.Provider lookup) {
        return new EdenSavedData(NetworkJson.decode(tag.getString("network")));
    }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider lookup) {
        tag.putString("network", NetworkJson.encode(network));
        return tag;
    }
}
