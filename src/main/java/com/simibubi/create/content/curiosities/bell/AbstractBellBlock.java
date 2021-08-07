package com.simibubi.create.content.curiosities.bell;

import javax.annotation.Nullable;

import com.simibubi.create.AllShapes;
import com.simibubi.create.foundation.block.ITE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BellAttachType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class AbstractBellBlock<TE extends AbstractBellTileEntity> extends BellBlock implements ITE<TE> {

	public AbstractBellBlock(Properties properties) {
		super(properties);
	}

	@Override
	@Nullable
	public BlockEntity newBlockEntity(BlockGetter block) {
		return null;
	}

	@Override
	public VoxelShape getShape(BlockState state, IBlockReader reader, BlockPos pos, ISelectionContext selection) {
		Direction facing = state.getValue(FACING);
		switch (state.getValue(ATTACHMENT)) {
		case CEILING:
			return AllShapes.BELL_CEILING.get(facing);
		case DOUBLE_WALL:
			return AllShapes.BELL_DOUBLE_WALL.get(facing);
		case FLOOR:
			return AllShapes.BELL_FLOOR.get(facing);
		case SINGLE_WALL:
			return AllShapes.BELL_WALL.get(facing);
		default:
			return VoxelShapes.block();
		}
	}

	@Override
	public boolean onHit(Level world, BlockState state, BlockHitResult hit, @Nullable Player player, boolean flag) {
		BlockPos pos = hit.getBlockPos();
		Direction direction = hit.getDirection();
		if (direction == null)
			direction = world.getBlockState(pos)
					.getValue(FACING);

		if (!this.isProperHit(state, direction, hit.getLocation().y - pos.getY()))
			return false;

		TE te = getTileEntity(world, pos);
		if (te == null || !te.ring(world, pos, direction))
			return false;

		if (!world.isClientSide) {
			playSound(world, pos);
			if (player != null)
				player.awardStat(Stats.BELL_RING);
		}

		return true;
	}

	public boolean isProperHit(BlockState state, Direction hitDir, double heightChange) {
		if (hitDir.getAxis() == Direction.Axis.Y)
			return false;
		if (heightChange > 0.8124)
			return false;

		Direction direction = state.getValue(FACING);
		BellAttachType bellAttachment = state.getValue(ATTACHMENT);
		switch (bellAttachment) {
			case FLOOR:
			case CEILING:
				return direction.getAxis() == hitDir.getAxis();
			case SINGLE_WALL:
			case DOUBLE_WALL:
				return direction.getAxis() != hitDir.getAxis();
			default:
				return false;
		}
	}

	public abstract void playSound(Level world, BlockPos pos);

}