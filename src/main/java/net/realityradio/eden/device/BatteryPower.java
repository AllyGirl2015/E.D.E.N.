package net.realityradio.eden.device;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.realityradio.eden.*;
import net.realityradio.eden.core.*;
import net.realityradio.eden.storage.EdenSavedData;

public final class BatteryPower {
  public static boolean powered(net.minecraft.server.MinecraftServer server, DeviceRecord device) {
    if (device.battery != null && device.battery.advance(server.overworld().getGameTime()))
      EdenSavedData.get(server).setDirty();
    return !EdenConfig.REQUIRE_BATTERY.get()
        || device.kind.equals("desktop")
        || device.battery != null && device.battery.powered();
  }

  public static IEnergyStorage itemEnergy(ItemStack stack) {
    var server = ServerLifecycleHooks.getCurrentServer();
    var id = stack.get(Eden.DEVICE_ID.get());
    if (server == null || id == null) return null;
    var saved = EdenSavedData.get(server);
    var d = saved.network.devices.get(id);
    if (d == null || d.battery == null) return null;
    return new IEnergyStorage() {
      private void update() {
        if (d.battery != null && d.battery.advance(server.overworld().getGameTime()))
          saved.setDirty();
      }

      public int receiveEnergy(int max, boolean simulate) {
        update();
        if (d.battery == null || d.battery.creative()) return 0;
        int n = Math.max(0, Math.min(max, getMaxEnergyStored() - getEnergyStored()));
        if (!simulate && n > 0) {
          d.battery.charge += n;
          saved.setDirty();
        }
        return n;
      }

      public int extractEnergy(int max, boolean simulate) {
        return 0;
      }

      public int getEnergyStored() {
        update();
        return d.battery == null ? 0 : d.battery.creative() ? Integer.MAX_VALUE : d.battery.charge;
      }

      public int getMaxEnergyStored() {
        return d.battery == null ? 0 : BatteryPack.capacity(d.battery.type);
      }

      public boolean canExtract() {
        return false;
      }

      public boolean canReceive() {
        return d.battery != null && !d.battery.creative();
      }
    };
  }

  private BatteryPower() {}
}
