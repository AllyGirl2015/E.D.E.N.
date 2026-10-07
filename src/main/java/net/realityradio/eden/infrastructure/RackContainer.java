package net.realityradio.eden.infrastructure;

import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

final class RackContainer implements Container {
  private final NetworkNodeEntity n;

  RackContainer(NetworkNodeEntity n) {
    this.n = n;
  }

  public int getContainerSize() {
    return n.inventory.getSlots();
  }

  public boolean isEmpty() {
    for (int i = 0; i < getContainerSize(); i++) if (!getItem(i).isEmpty()) return false;
    return true;
  }

  public ItemStack getItem(int i) {
    return n.inventory.getStackInSlot(i);
  }

  public ItemStack removeItem(int i, int c) {
    return n.inventory.extractItem(i, c, false);
  }

  public ItemStack removeItemNoUpdate(int i) {
    var s = getItem(i);
    n.inventory.setStackInSlot(i, ItemStack.EMPTY);
    return s;
  }

  public void setItem(int i, ItemStack s) {
    n.inventory.setStackInSlot(i, s);
  }

  public void setChanged() {
    n.setChanged();
  }

  public boolean stillValid(Player p) {
    return !n.isRemoved()
        && n.getBlockPos().distToCenterSqr(p.position()) <= 64
        && (p.hasPermissions(2) || p.getUUID().equals(n.owner));
  }

  public void clearContent() {
    for (int i = 0; i < getContainerSize(); i++) setItem(i, ItemStack.EMPTY);
  }
}
