package cr0s.warpdrive.data;

import cr0s.warpdrive.block.breathing.AbstractAirBlock;
import cr0s.warpdrive.block.breathing.AirFlowBlock;
import cr0s.warpdrive.block.breathing.AirShieldBlock;
import cr0s.warpdrive.block.breathing.AirSourceBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.PaneBlock;
import net.minecraft.block.material.Material;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;

/**
 * Decides how air moves through a block.
 *
 * Extracted from StateAir so it can be asked the same question without a chunk behind it. The
 * server stores the answer in the packed air state; the client needs it too - to work out how close
 * a player is to open space - and has no ChunkData to consult. The classification itself depends
 * only on the block and its neighbours, so it belongs here rather than on the stateful cursor.
 */
public final class AirClassifier {

	private AirClassifier() {
	}

	/**
	 * Ported from 1.12.2 StateAir.updateBlockType, minus its stairs special case - those reported a
	 * slab collision box in 1.12.2 and had to be hardcoded as sealers, whereas a 1.16.5 VoxelShape
	 * spans the full cube and the generic test below gets them right.
	 *
	 * Kept as an explicit cascade rather than switched to VoxelShape.isFaceSturdy: that answers
	 * whether a face supports blocks and redstone, not whether air passes. Glass panes report no
	 * sturdy face at all and would flip from "seals horizontally" to "leaks everywhere".
	 */
	public static int classify(final BlockState blockState, final IBlockReader world, final BlockPos blockPos) {
		final Block block = blockState.getBlock();

		if (block instanceof AirFlowBlock) {
			return AirData.BLOCK_AIR_FLOW;
		}
		if (block instanceof AirSourceBlock) {
			return AirData.BLOCK_AIR_SOURCE;
		}
		if (isVanillaAir(block)) {
			return AirData.BLOCK_AIR_PLACEABLE;
		}
		// Must be named explicitly: an air shield has no collision box, so deriving its behaviour
		// from shape would call it "leaks everywhere" when it is in fact a full seal. 1.12.2 had the
		// same special case, applied to its omnipanel base class.
		if (block instanceof AirShieldBlock) {
			return AirData.BLOCK_SEALER;
		}
		if (blockState.getMaterial() == Material.LEAVES) {
			return AirData.BLOCK_AIR_NON_PLACEABLE;
		}
		if (!blockState.getFluidState().isEmpty()) {
			// Two stacked sources look identical from metadata alone, so the block above decides:
			// more fluid above means we are inside a column and sealed, otherwise this is a surface
			// air can move across horizontally.
			return world.getBlockState(blockPos.above()).getFluidState().isEmpty()
			     ? AirData.BLOCK_AIR_NON_PLACEABLE_H
			     : AirData.BLOCK_SEALER;
		}
		if (block instanceof PaneBlock) {
			return AirData.BLOCK_AIR_NON_PLACEABLE_V;
		}
		if (blockState.getMaterial().isReplaceable()) {
			// grass, snow layers, modded replaceable decoration
			return AirData.BLOCK_AIR_NON_PLACEABLE;
		}
		return classifyByShape(blockState, world, blockPos);
	}

	/** Decide sealing from the collision shape's extent on each axis, as 1.12.2 did with its AABB. */
	private static int classifyByShape(final BlockState blockState, final IBlockReader world,
	                                   final BlockPos blockPos) {
		final VoxelShape shape = blockState.getCollisionShape(world, blockPos);
		if (shape.isEmpty()) {
			return AirData.BLOCK_AIR_NON_PLACEABLE;
		}

		final AxisAlignedBB bounds = shape.bounds();
		final boolean fullX = bounds.maxX - bounds.minX > 0.99D;
		final boolean fullY = bounds.maxY - bounds.minY > 0.99D;
		final boolean fullZ = bounds.maxZ - bounds.minZ > 0.99D;

		if (fullX && fullY && fullZ) {
			return AirData.BLOCK_SEALER;
		}
		if (fullX && fullZ) {
			// spans the whole footprint but not the full height: seals vertically, leaks sideways
			return AirData.BLOCK_AIR_NON_PLACEABLE_H;
		}
		if (fullY && (fullX || fullZ)) {
			// spans the full height across one axis: seals sideways, leaks vertically
			return AirData.BLOCK_AIR_NON_PLACEABLE_V;
		}
		return AirData.BLOCK_AIR_NON_PLACEABLE;
	}

	private static boolean isVanillaAir(final Block block) {
		return block == Blocks.AIR || block == Blocks.CAVE_AIR || block == Blocks.VOID_AIR;
	}

	/**
	 * True for genuinely empty space - vanilla air that is not one of our air blocks.
	 *
	 * The distinction is the point: a WarpDrive air block reports as air to the rest of the game, so
	 * a naive isAir() check would treat a pressurised room as vacuum.
	 */
	public static boolean isVoid(final IBlockReader world, final BlockPos blockPos) {
		final BlockState blockState = world.getBlockState(blockPos);
		return blockState.isAir() && !(blockState.getBlock() instanceof AbstractAirBlock);
	}

	/** True when this position holds breathable WarpDrive air. */
	public static boolean isAirBlock(final IBlockReader world, final BlockPos blockPos) {
		return world.getBlockState(blockPos).getBlock() instanceof AbstractAirBlock;
	}

	/** True when this block seals against air movement entirely. */
	public static boolean isSealer(final IBlockReader world, final BlockPos blockPos) {
		final BlockState blockState = world.getBlockState(blockPos);
		return classify(blockState, world, blockPos) == AirData.BLOCK_SEALER;
	}
}
