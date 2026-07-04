package com.example.walkietalkie.block;

import com.example.walkietalkie.block.entity.RadioStationBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A tabletop radio you can place down, tune, and (eventually -- see
 * RadioStationBlockEntity's class doc) relay voice through. Right-clicking opens the
 * same RadioMenu/RadioScreen the walkie-talkie uses, just with the mic slot unlocked.
 */
public class RadioStationBlock extends BaseEntityBlock {

    public static final MapCodec<RadioStationBlock> CODEC = simpleCodec(RadioStationBlock::new);

    // Roughly matches the model's footprint -- a squat tabletop device, not a full block.
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 9, 15);

    public RadioStationBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RadioStationBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof RadioStationBlockEntity be) {
            player.openMenu(be);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof RadioStationBlockEntity be) {
            if (level instanceof ServerLevel) {
                Containers.dropContents(level, pos, be);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
