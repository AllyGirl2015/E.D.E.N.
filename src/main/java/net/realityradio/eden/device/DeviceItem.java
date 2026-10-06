package net.realityradio.eden.device;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.realityradio.eden.network.ServerDevices;

public final class DeviceItem extends Item {
    public final String kind;
    public DeviceItem(Properties properties, String kind) { super(properties.stacksTo(1)); this.kind = kind; }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) ServerDevices.openItem(serverPlayer, stack, kind);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
