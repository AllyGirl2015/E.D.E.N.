package net.realityradio.eden.device;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.realityradio.eden.Eden;
import net.realityradio.eden.core.BatteryPack;

public final class BatteryItem extends Item {
  public final String type;

  public BatteryItem(Properties p, String type) {
    super(p.stacksTo(1));
    this.type = type;
  }

  public IEnergyStorage energy(ItemStack stack) {
    return new IEnergyStorage() {
      public int receiveEnergy(int amount, boolean simulate) {
        if (type.equals("creative")) return 0;
        int n = Math.max(0, Math.min(amount, getMaxEnergyStored() - getEnergyStored()));
        if (!simulate && n > 0) stack.set(Eden.BATTERY_ENERGY.get(), getEnergyStored() + n);
        return n;
      }

      public int extractEnergy(int n, boolean simulate) {
        return 0;
      }

      public int getEnergyStored() {
        return type.equals("creative")
            ? Integer.MAX_VALUE
            : Math.max(
                0,
                Math.min(
                    BatteryPack.capacity(type), stack.getOrDefault(Eden.BATTERY_ENERGY.get(), 0)));
      }

      public int getMaxEnergyStored() {
        return BatteryPack.capacity(type);
      }

      public boolean canExtract() {
        return false;
      }

      public boolean canReceive() {
        return !type.equals("creative");
      }
    };
  }

  public void appendHoverText(
      ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
    super.appendHoverText(stack, context, lines, flag);
    lines.add(
        Component.literal(
            type.equals("creative")
                ? "Infinite power · Uncraftable"
                : BatteryPack.days(type)
                    + " Minecraft days · "
                    + energy(stack).getEnergyStored()
                    + " / "
                    + BatteryPack.capacity(type)
                    + " FE"));
  }
}
