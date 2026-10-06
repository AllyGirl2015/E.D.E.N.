package net.realityradio.eden.device;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.realityradio.eden.Eden;

public final class DeviceBlockEntity extends BlockEntity {
    public UUID deviceId = UUID.randomUUID();
    public DeviceBlockEntity(BlockPos pos, BlockState state) { super(Eden.TERMINAL_ENTITY.get(), pos, state); }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup);
        tag.putUUID("device", deviceId);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup);
        if (tag.hasUUID("device")) deviceId = tag.getUUID("device");
    }
}
