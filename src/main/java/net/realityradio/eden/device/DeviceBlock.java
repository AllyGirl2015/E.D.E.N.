package net.realityradio.eden.device;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.server.level.ServerLevel;
import net.realityradio.eden.Eden;
import net.realityradio.eden.storage.EdenSavedData;
import java.util.List;
import net.minecraft.world.phys.BlockHitResult;
import net.realityradio.eden.network.ServerDevices;

public final class DeviceBlock extends BaseEntityBlock {
    public static final MapCodec<DeviceBlock> CODEC = simpleCodec(DeviceBlock::new);
    public DeviceBlock(Properties properties) { super(properties); }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new DeviceBlockEntity(pos, state); }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (level.getBlockEntity(pos) instanceof DeviceBlockEntity device) {
            var id = stack.get(Eden.DEVICE_ID.get());
            if (id != null) device.deviceId = id;
            device.setChanged();
        }
    }
    @Override protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        var drops = super.getDrops(state, params);
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof DeviceBlockEntity device)
            for (var stack : drops) if (stack.is(Eden.TERMINAL_ITEM.get())) stack.set(Eden.DEVICE_ID.get(), device.deviceId);
        return drops;
    }
    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock()) && level instanceof ServerLevel server && level.getBlockEntity(pos) instanceof DeviceBlockEntity device) {
            var data = EdenSavedData.get(server.getServer());
            var record = data.network.devices.get(device.deviceId);
            if (record != null && record.sim != null) {
                var sim = data.network.eject(record.id);
                var stack = new ItemStack(Eden.SIM.get()); stack.set(Eden.SIM_ID.get(), sim);
                Block.popResource(level, pos, stack); data.setDirty();
            }
        }
        super.onRemove(state, level, pos, next, moving);
    }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.or(Block.box(1, 0, 2, 15, 2, 14), Block.box(7, 2, 7, 9, 7, 9), Block.box(1, 5, 7, 15, 15, 10));
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof DeviceBlockEntity device)
            ServerDevices.openBlock(serverPlayer, device);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
