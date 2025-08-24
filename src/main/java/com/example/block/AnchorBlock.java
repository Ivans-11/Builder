package com.example.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;

import com.mojang.serialization.MapCodec;

// Block for anchor
public class AnchorBlock extends HorizontalFacingBlock {
	// codec is required from 1.20.5 but not used in Minecraft yet.
	public static final MapCodec<AnchorBlock> CODEC = Block.createCodec(AnchorBlock::new);
 
	public AnchorBlock(Settings settings) {
		super(settings);
		setDefaultState(getDefaultState().with(Properties.HORIZONTAL_FACING, Direction.NORTH));
	}
 
	@Override
	protected MapCodec<? extends AnchorBlock> getCodec() {
		return CODEC;
	}
 
	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(Properties.HORIZONTAL_FACING);
	}

    // Shape of anchor
	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext ctx) {
		Direction dir = state.get(FACING);
		VoxelShape corner, xAxis, yAxis, zAxis;

    	switch (dir) {
        	case NORTH -> {
				corner = VoxelShapes.cuboid(0, 0, 0, 0.125, 0.125, 0.125);
	            xAxis = VoxelShapes.cuboid(0.125, 0, 0, 1, 0.125, 0.125);
    	        yAxis = VoxelShapes.cuboid(0, 0.125, 0, 0.125, 1, 0.125);
        	    zAxis = VoxelShapes.cuboid(0, 0, 0.125, 0.125, 0.125, 1);
        	}
	        case SOUTH -> {
				corner = VoxelShapes.cuboid(0.875, 0, 0.875, 1, 0.125, 1);
        	    xAxis = VoxelShapes.cuboid(0, 0, 0.875, 0.875, 0.125, 1);
            	yAxis = VoxelShapes.cuboid(0.875, 0.125, 0.875, 1, 1, 1);
            	zAxis = VoxelShapes.cuboid(0.875, 0, 0, 1, 0.125, 0.875);
	        }
    	    case EAST -> {
				corner = VoxelShapes.cuboid(0.875, 0, 0, 1, 0.125, 0.125);
            	xAxis = VoxelShapes.cuboid(0, 0, 0, 0.875, 0.125, 0.125);
	            yAxis = VoxelShapes.cuboid(0.875, 0.125, 0, 1, 1, 0.125);
    	        zAxis = VoxelShapes.cuboid(0.875, 0, 0.125, 1, 0.125, 1);
        	}
	        case WEST -> {
				corner = VoxelShapes.cuboid(0, 0, 0.875, 0.125, 0.125, 1);
        	    xAxis = VoxelShapes.cuboid(0.125, 0, 0.875, 1, 0.125, 1);
            	yAxis = VoxelShapes.cuboid(0, 0.125, 0.875, 0.125, 1, 1);
	            zAxis = VoxelShapes.cuboid(0, 0, 0, 0.125, 0.125, 0.875);
    	    }
        	default -> {
            	return VoxelShapes.fullCube();
        	}
    	}
    	return VoxelShapes.union(corner, xAxis, yAxis, zAxis);
	}

	// When placed, the facing direction is the opposite of the player's facing direction.
    @Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		return super.getPlacementState(ctx).with(Properties.HORIZONTAL_FACING, ctx.getHorizontalPlayerFacing().getOpposite());
	}

	// Add to cache when placed
	@Override
    public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.onPlaced(world, pos, state, placer, stack);
        if (!world.isClient && world instanceof ServerWorld sw) {
            AnchorState.getState(sw.getServer()).add(pos);
        }
    }

	// Remove from cache when broken
    @Override
    public void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) {
        super.onStateReplaced(state, world, pos, moved);
        if (!moved) {
			AnchorState.getState(world.getServer()).remove(pos);
		}
	}
}