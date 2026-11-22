package com.example.tramdoors;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A door that is three blocks tall. Right click toggles all three parts. */
public class TramDoorBlock extends Block {

    public enum Part implements StringRepresentable {
        LOWER("lower"), MIDDLE("middle"), UPPER("upper");

        private final String name;

        Part(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    public static final EnumProperty<DoorHingeSide> HINGE = BlockStateProperties.DOOR_HINGE;
    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);

    protected static final VoxelShape SOUTH_AABB = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 3.0D);
    protected static final VoxelShape NORTH_AABB = Block.box(0.0D, 0.0D, 13.0D, 16.0D, 16.0D, 16.0D);
    protected static final VoxelShape WEST_AABB = Block.box(13.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);
    protected static final VoxelShape EAST_AABB = Block.box(0.0D, 0.0D, 0.0D, 3.0D, 16.0D, 16.0D);

    public TramDoorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
            .setValue(FACING, Direction.NORTH)
            .setValue(OPEN, Boolean.FALSE)
            .setValue(HINGE, DoorHingeSide.LEFT)
            .setValue(PART, Part.LOWER));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OPEN, HINGE, PART);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction direction = state.getValue(FACING);
        boolean closed = !state.getValue(OPEN);
        boolean rightHinge = state.getValue(HINGE) == DoorHingeSide.RIGHT;
        switch (direction) {
            case EAST:
            default:
                return closed ? EAST_AABB : (rightHinge ? NORTH_AABB : SOUTH_AABB);
            case SOUTH:
                return closed ? SOUTH_AABB : (rightHinge ? EAST_AABB : WEST_AABB);
            case WEST:
                return closed ? WEST_AABB : (rightHinge ? SOUTH_AABB : NORTH_AABB);
            case NORTH:
                return closed ? NORTH_AABB : (rightHinge ? WEST_AABB : EAST_AABB);
        }
    }

    private static BlockPos basePos(BlockState state, BlockPos pos) {
        return pos.below(state.getValue(PART).ordinal());
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        if (pos.getY() >= level.getMaxBuildHeight() - 2) {
            return null;
        }
        if (!level.getBlockState(pos.above()).canBeReplaced(context)
            || !level.getBlockState(pos.above(2)).canBeReplaced(context)) {
            return null;
        }
        return defaultBlockState()
            .setValue(FACING, context.getHorizontalDirection())
            .setValue(HINGE, getHinge(context))
            .setValue(OPEN, Boolean.FALSE)
            .setValue(PART, Part.LOWER);
    }

    private DoorHingeSide getHinge(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Direction direction = context.getHorizontalDirection();
        int stepX = direction.getStepX();
        int stepZ = direction.getStepZ();
        Vec3 click = context.getClickLocation();
        double dx = click.x - pos.getX();
        double dz = click.z - pos.getZ();
        boolean right = (stepX < 0 && dz < 0.5D) || (stepX > 0 && dz > 0.5D)
            || (stepZ < 0 && dx > 0.5D) || (stepZ > 0 && dx < 0.5D);
        return right ? DoorHingeSide.RIGHT : DoorHingeSide.LEFT;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        level.setBlock(pos.above(), state.setValue(PART, Part.MIDDLE), 3);
        level.setBlock(pos.above(2), state.setValue(PART, Part.UPPER), 3);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (state.getValue(PART) == Part.LOWER) {
            BlockPos below = pos.below();
            return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
        }
        return true;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        Part part = state.getValue(PART);
        if (direction == Direction.UP && part != Part.UPPER) {
            Part expected = Part.values()[part.ordinal() + 1];
            if (!neighbor.is(this) || neighbor.getValue(PART) != expected) {
                return Blocks.AIR.defaultBlockState();
            }
        }
        if (direction == Direction.DOWN) {
            if (part == Part.LOWER) {
                if (!state.canSurvive(level, pos)) {
                    return Blocks.AIR.defaultBlockState();
                }
            } else {
                Part expected = Part.values()[part.ordinal() - 1];
                if (!neighbor.is(this) || neighbor.getValue(PART) != expected) {
                    return Blocks.AIR.defaultBlockState();
                }
            }
        }
        return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && !player.isCreative() && state.getValue(PART) != Part.LOWER) {
            BlockPos base = basePos(state, pos);
            BlockState baseState = level.getBlockState(base);
            if (baseState.is(this) && baseState.getValue(PART) == Part.LOWER) {
                Block.dropResources(baseState, level, base, null, player, player.getMainHandItem());
            }
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        BlockPos base = basePos(state, pos);
        BlockState baseState = level.getBlockState(base);
        if (!baseState.is(this)) {
            return InteractionResult.PASS;
        }
        boolean open = !baseState.getValue(OPEN);
        for (int i = 0; i < 3; i++) {
            BlockPos p = base.above(i);
            BlockState s = level.getBlockState(p);
            if (s.is(this)) {
                level.setBlock(p, s.setValue(OPEN, open), 10);
            }
        }
        level.playSound(player, base, open ? SoundEvents.IRON_DOOR_OPEN : SoundEvents.IRON_DOOR_CLOSE,
            SoundSource.BLOCKS, 1.0F, level.getRandom().nextFloat() * 0.1F + 0.9F);
        level.gameEvent(player, open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, base);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) {
        switch (type) {
            case LAND:
            case AIR:
                return state.getValue(OPEN);
            default:
                return false;
        }
    }
}
