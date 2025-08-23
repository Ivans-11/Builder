package com.example.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.item.ItemPlacementContext;
import com.mojang.serialization.MapCodec;

// 锚点方块
public class AnchorBlock extends HorizontalFacingBlock {
	// codec 从 1.20.5 开始是必需的，但是还没有在 Minecraft 中实际使用。
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

    // 形状
	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext ctx) {
		Direction dir = state.get(FACING);
		VoxelShape corner, xAxis, yAxis, zAxis;

    	switch (dir) {
        	case NORTH -> {
				corner = VoxelShapes.cuboid(0.875, 0, 0, 1, 0.125, 0.125);
            	xAxis = VoxelShapes.cuboid(0, 0, 0, 0.875, 0.125, 0.125);
	            yAxis = VoxelShapes.cuboid(0.875, 0.125, 0, 1, 1, 0.125);
    	        zAxis = VoxelShapes.cuboid(0.875, 0, 0.125, 1, 0.125, 1);
        	}
	        case SOUTH -> {
				corner = VoxelShapes.cuboid(0, 0, 0.875, 0.125, 0.125, 1);
        	    xAxis = VoxelShapes.cuboid(0.125, 0, 0.875, 1, 0.125, 1);
            	yAxis = VoxelShapes.cuboid(0, 0.125, 0.875, 0.125, 1, 1);
	            zAxis = VoxelShapes.cuboid(0, 0, 0, 0.125, 0.125, 0.875);
	        }
    	    case EAST -> {
        	    corner = VoxelShapes.cuboid(0.875, 0, 0.875, 1, 0.125, 1);
        	    xAxis = VoxelShapes.cuboid(0, 0, 0.875, 0.875, 0.125, 1);
            	yAxis = VoxelShapes.cuboid(0.875, 0.125, 0.875, 1, 1, 1);
            	zAxis = VoxelShapes.cuboid(0.875, 0, 0, 1, 0.125, 0.875);
        	}
	        case WEST -> {
				corner = VoxelShapes.cuboid(0, 0, 0, 0.125, 0.125, 0.125);
	            xAxis = VoxelShapes.cuboid(0.125, 0, 0, 1, 0.125, 0.125);
    	        yAxis = VoxelShapes.cuboid(0, 0.125, 0, 0.125, 1, 0.125);
        	    zAxis = VoxelShapes.cuboid(0, 0, 0.125, 0.125, 0.125, 1);
    	    }
        	default -> {
            	return VoxelShapes.fullCube();
        	}
    	}
    	return VoxelShapes.union(corner, xAxis, yAxis, zAxis);
	}

    // 放置时朝向(将玩家朝向的相反方向作为放置的朝向)
    @Override
	public BlockState getPlacementState(ItemPlacementContext ctx) {
		return super.getPlacementState(ctx).with(Properties.HORIZONTAL_FACING, ctx.getHorizontalPlayerFacing().getOpposite());
	}
}