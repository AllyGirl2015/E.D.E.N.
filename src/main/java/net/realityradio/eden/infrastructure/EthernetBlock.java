package net.realityradio.eden.infrastructure;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.shapes.*;
import net.realityradio.eden.device.DeviceBlock;

public final class EthernetBlock extends Block {
  private static final Map<Direction, BooleanProperty> PORTS =
      Map.of(
          Direction.NORTH,
          BlockStateProperties.NORTH,
          Direction.SOUTH,
          BlockStateProperties.SOUTH,
          Direction.EAST,
          BlockStateProperties.EAST,
          Direction.WEST,
          BlockStateProperties.WEST,
          Direction.UP,
          BlockStateProperties.UP,
          Direction.DOWN,
          BlockStateProperties.DOWN);

  public EthernetBlock(Properties p) {
    super(p);
    var s = stateDefinition.any();
    for (var key : PORTS.values()) s = s.setValue(key, false);
    registerDefaultState(s);
  }

  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
    for (var key : PORTS.values()) b.add(key);
  }

  private boolean connects(LevelReader l, BlockPos p) {
    var b = l.getBlockState(p).getBlock();
    return b instanceof EthernetBlock || b instanceof NetworkNodeBlock || b instanceof DeviceBlock;
  }

  public BlockState getStateForPlacement(BlockPlaceContext c) {
    var s = defaultBlockState();
    for (var d : Direction.values())
      s = s.setValue(PORTS.get(d), connects(c.getLevel(), c.getClickedPos().relative(d)));
    return s;
  }

  protected BlockState updateShape(
      BlockState s, Direction d, BlockState other, LevelAccessor l, BlockPos p, BlockPos op) {
    return s.setValue(PORTS.get(d), connects(l, op));
  }

  protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
    var shape = Block.box(6, 6, 6, 10, 10, 10);
    for (var d : Direction.values())
      if (s.getValue(PORTS.get(d)))
        shape =
            Shapes.or(
                shape,
                switch (d) {
                  case DOWN -> Block.box(6, 0, 6, 10, 6, 10);
                  case UP -> Block.box(6, 10, 6, 10, 16, 10);
                  case NORTH -> Block.box(6, 6, 0, 10, 10, 6);
                  case SOUTH -> Block.box(6, 6, 10, 10, 10, 16);
                  case WEST -> Block.box(0, 6, 6, 6, 10, 10);
                  case EAST -> Block.box(10, 6, 6, 16, 10, 10);
                });
    return shape;
  }
}
