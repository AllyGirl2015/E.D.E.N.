package net.realityradio.eden.api;

import net.minecraft.server.level.ServerPlayer;
import net.realityradio.eden.core.DeviceRecord;
import net.realityradio.eden.core.WorldNetwork;

/** Invoked only after the server verifies possession/proximity of the device. */
public record ServiceContext(ServerPlayer player, DeviceRecord device, WorldNetwork network) {}
