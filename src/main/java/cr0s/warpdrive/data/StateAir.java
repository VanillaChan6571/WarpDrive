package cr0s.warpdrive.data;

import cr0s.warpdrive.api.ExceptionChunkNotLoaded;
import cr0s.warpdrive.debug.DebugLog;
import cr0s.warpdrive.block.breathing.AirFlowBlock;
import cr0s.warpdrive.block.breathing.AirSourceBlock;
import cr0s.warpdrive.event.ChunkHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.PaneBlock;
import net.minecraft.block.material.Material;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.World;
import net.minecraft.world.gen.Heightmap;

import javax.annotation.Nullable;

/**
 * Air state at one block, ported from 1.12.2.
 *
 * A lightweight cursor over ChunkData rather than a stored object: refresh() points it at a
 * position, unpacks the int there into fields, and the setters pack changes back. The simulation
 * reuses a handful of these instead of allocating per block.
 *
 * Two pressures propagate independently and are the heart of the model. Generator pressure spreads
 * outward from an air source, losing one per block. Void pressure spreads inward from open space
 * the same way. A block with generator pressure and no void pressure is sealed and fills with air;
 * one with both is leaking, and the balance decides whether it holds.
 */
public class StateAir {

	/** How far a removal cascades before giving up, 1.12.2 BREATHING_VOLUME_UPDATE_DEPTH_BLOCKS. */
	private static final int VOLUME_UPDATE_DEPTH_BLOCKS = 256;

	private static final int MAX_Y = 255;

	private ChunkData chunkData;
	private final BlockPos.Mutable blockPos;

	/** Raw packed state as read from the chunk. */
	protected int dataAir;
	/** Cached block at this position; null until read. */
	protected BlockState blockState;

	public byte concentration;
	public short pressureGenerator;
	public short pressureVoid;
	/** Direction pointing toward the generator, null when there is no generator pressure. */
	public Direction directionGenerator;
	/** Direction pointing toward the void, null when there is no void pressure. */
	public Direction directionVoid;

	public StateAir(@Nullable final ChunkData chunkData) {
		this.chunkData = chunkData;
		this.blockPos = new BlockPos.Mutable();
	}

	// ===== positioning =====

	public void refresh(final World world, final int x, final int y, final int z) throws ExceptionChunkNotLoaded {
		blockPos.set(x, y, z);
		refresh(world);
	}

	public void refresh(final World world, final StateAir stateAir, final Direction direction) throws ExceptionChunkNotLoaded {
		blockPos.set(stateAir.blockPos.getX() + direction.getStepX(),
		             stateAir.blockPos.getY() + direction.getStepY(),
		             stateAir.blockPos.getZ() + direction.getStepZ());
		refresh(world);
	}

	private void refresh(final World world) throws ExceptionChunkNotLoaded {
		if (chunkData == null || !chunkData.isInside(blockPos.getX(), blockPos.getZ())) {
			chunkData = ChunkHandler.getChunkData(world, blockPos.getX(), blockPos.getZ());
			if (chunkData == null) {
				// Aborting is correct: the air state resumes when the chunk comes back, and
				// touching an unloaded chunk here risks a concurrent modification
				throw new ExceptionChunkNotLoaded(String.format(
					"Air refresh aborted at %d %d %d", blockPos.getX(), blockPos.getY(), blockPos.getZ()));
			}
		}

		blockState = null;
		dataAir = chunkData.getDataAir(blockPos.getX(), blockPos.getY(), blockPos.getZ());
		if (dataAir == 0) {
			dataAir = AirData.AIR_DEFAULT;
		}

		concentration = (byte) (dataAir & AirData.CONCENTRATION_MASK);
		pressureGenerator = (short) ((dataAir & AirData.GENERATOR_PRESSURE_MASK) >> AirData.GENERATOR_PRESSURE_SHIFT);
		pressureVoid = (short) ((dataAir & AirData.VOID_PRESSURE_MASK) >> AirData.VOID_PRESSURE_SHIFT);
		directionGenerator = decodeDirection((dataAir & AirData.GENERATOR_DIRECTION_MASK) >> AirData.GENERATOR_DIRECTION_SHIFT);
		directionVoid = decodeDirection((dataAir & AirData.VOID_DIRECTION_MASK) >> AirData.VOID_DIRECTION_SHIFT);

		if ((dataAir & AirData.BLOCK_MASK) == AirData.BLOCK_UNKNOWN) {
			updateBlockCache(world);
		}
		updateVoidSource(world);
	}

	public void clearCache() {
		// Cached chunk references must not outlive the tick, or a chunk unload leaks
		chunkData = null;
	}

	public BlockPos getBlockPos() {
		return blockPos.immutable();
	}

	/** 6 encodes null, matching 1.12.2 Commons.getOrdinal. */
	@Nullable
	private static Direction decodeDirection(final int index) {
		return index < 0 || index > 5 ? null : Direction.from3DDataValue(index);
	}

	private static int encodeDirection(@Nullable final Direction direction) {
		return direction == null ? 6 : direction.get3DDataValue();
	}

	// ===== block classification =====

	public BlockState getBlockState(final World world) {
		if (blockState == null) {
			updateBlockCache(world);
		}
		return blockState;
	}

	public void updateBlockCache(final World world) {
		if (blockPos.getY() >= 0 && blockPos.getY() <= MAX_Y) {
			blockState = world.getBlockState(blockPos);
		} else {
			blockState = Blocks.AIR.defaultBlockState();
		}
		updateBlockType(world);
	}

	/**
	 * Classify how air moves through this block.
	 *
	 * Ported from 1.12.2 with one case deliberately removed. The original hardcoded stairs as
	 * sealers with the comment "stairs are reporting slab collision box, so we can't detect them
	 * automatically" - a 1.12.2 limitation. In 1.16.5 a stair's VoxelShape spans the full cube, so
	 * the generic test below classifies it correctly with no special case.
	 *
	 * The rest is kept as-is. VoxelShape.isFaceSturdy looked like it should replace this whole
	 * cascade, but it answers a different question - whether a face can support blocks and redstone
	 * - not whether air passes. Glass panes report no sturdy face at all, which would have flipped
	 * them from "seals horizontally" to "leaks everywhere" and quietly vented every pane window.
	 */
	private void updateBlockType(final World world) {
		final int typeBlock = AirClassifier.classify(blockState, world, blockPos);

		if ((dataAir & AirData.BLOCK_MASK) != typeBlock) {
			dataAir = (dataAir & ~AirData.BLOCK_MASK) | typeBlock;
			chunkData.setDataAir(blockPos.getX(), blockPos.getY(), blockPos.getZ(), dataAir);
		}
	}

	// ===== void detection =====

	/**
	 * Decide whether this block is exposed to open space, which makes it a void source that drains
	 * any air reaching it.
	 */
	private void updateVoidSource(final World world) {
		if (!isAir()) {
			// sealed blocks hold no pressure at all
			setGenerator((short) 0, null);
			setVoid((short) 0, null);

		} else if (pressureGenerator == 0) {
			// nothing generating here, so nothing to drain - clear it to save work
			setVoid((short) 0, null);

		} else if (pressureGenerator == 1) {
			// the far edge of a generator's reach is where air meets vacuum
			setVoid((short) AirData.VOID_PRESSURE_MAX, directionGenerator == null ? Direction.UP : directionGenerator.getOpposite());

		} else if (blockPos.getY() == 0) {
			setVoid((short) AirData.VOID_PRESSURE_MAX, Direction.DOWN);

		} else if (blockPos.getY() == MAX_Y) {
			setVoid((short) AirData.VOID_PRESSURE_MAX, Direction.UP);

		} else if (blockState != null) {
			// Open sky means open space. MOTION_BLOCKING is the 1.16.5 equivalent of 1.12.2's
			// getPrecipitationHeight: the highest block that stops movement or holds fluid.
			final int highestBlock = world.getHeight(Heightmap.Type.MOTION_BLOCKING, blockPos.getX(), blockPos.getZ());
			if (highestBlock <= blockPos.getY()) {
				setVoid((short) AirData.VOID_PRESSURE_MAX, Direction.UP);
			} else if (pressureVoid == AirData.VOID_PRESSURE_MAX) {
				setVoid((short) 0, null);
			}
		}
		// propagation itself happens in AirSpreader
	}

	// ===== mutation =====

	private void setBlockToNoAir(final World world) {
		world.setBlock(blockPos, Blocks.AIR.defaultBlockState(), 2);
		blockState = Blocks.AIR.defaultBlockState();
		updateBlockType(world);
	}

	private void setBlockToAirFlow(final World world) {
		final BlockState stateAirFlow = Registration.AIR_FLOW_BLOCK.get().defaultBlockState();
		world.setBlock(blockPos, stateAirFlow, 2);
		blockState = stateAirFlow;
		updateBlockType(world);
		// Air blocks are invisible, so this log is the only way to observe the volume filling
		DebugLog.log("AIR", "air spread to {} {} {}", blockPos.getX(), blockPos.getY(), blockPos.getZ());
	}

	public boolean setAirSource(final World world, final Direction direction, final short pressure) {
		final int blockType = dataAir & AirData.BLOCK_MASK;
		final boolean isPlaceable = blockType == AirData.BLOCK_AIR_PLACEABLE
		                         || blockType == AirData.BLOCK_AIR_FLOW
		                         || blockType == AirData.BLOCK_AIR_SOURCE;

		final boolean updateRequired = !(blockState.getBlock() instanceof AirSourceBlock)
		                            || pressureGenerator != pressure
		                            || pressureVoid != 0
		                            || concentration != AirData.CONCENTRATION_MAX;

		if (updateRequired && isPlaceable) {
			// The block's facing points away from the generator; the stored direction points back
			// toward it, which is what propagation follows.
			blockState = Registration.AIR_SOURCE_BLOCK.get().defaultBlockState()
				.setValue(AirSourceBlock.FACING, direction);
			world.setBlock(blockPos, blockState, 2);
			updateBlockType(world);
			try {
				setGeneratorAndUpdateVoid(world, pressure, direction.getOpposite());
			} catch (final ExceptionChunkNotLoaded exception) {
				// nothing to do: the state resumes when the chunk reloads
			}
			setConcentration(world, (byte) AirData.CONCENTRATION_MAX);
		}
		return updateRequired;
	}

	public void removeAirSource(final World world) {
		setBlockToAirFlow(world);
		setConcentration(world, (byte) 1);
	}

	public void setConcentration(final World world, final byte concentrationNew) {
		if (concentrationNew == 0) {
			if (isAirFlow()) {
				if (blockState == null) {
					updateBlockCache(world);
				}
				if (isAirFlow()) {
					setBlockToNoAir(world);
				}
			}

		} else if ((dataAir & AirData.BLOCK_MASK) == AirData.BLOCK_AIR_PLACEABLE) {
			if (blockState == null) {
				updateBlockCache(world);
				if ((dataAir & AirData.BLOCK_MASK) != AirData.BLOCK_AIR_PLACEABLE) {
					// stored state disagreed with the world - skip rather than place into a solid
					return;
				}
			}
			setBlockToAirFlow(world);
		}

		if (concentration != concentrationNew) {
			dataAir = (dataAir & ~AirData.CONCENTRATION_MASK) | concentrationNew;
			concentration = concentrationNew;
			chunkData.setDataAir(blockPos.getX(), blockPos.getY(), blockPos.getZ(), dataAir);
		}
	}

	protected void setGeneratorAndUpdateVoid(final World world, final short pressureNew,
	                                         final Direction directionNew) throws ExceptionChunkNotLoaded {
		if (pressureNew == 0 && pressureVoid > 0) {
			removeGeneratorAndCascade(world);
		} else {
			setGenerator(pressureNew, directionNew);
			updateVoidSource(world);
		}
	}

	private void setGenerator(final short pressureNew, @Nullable final Direction directionNew) {
		boolean isUpdated = false;
		if (pressureNew != pressureGenerator) {
			dataAir = (dataAir & ~AirData.GENERATOR_PRESSURE_MASK)
			        | (pressureNew << AirData.GENERATOR_PRESSURE_SHIFT);
			pressureGenerator = pressureNew;
			isUpdated = true;
		}
		if (directionNew != directionGenerator) {
			dataAir = (dataAir & ~AirData.GENERATOR_DIRECTION_MASK)
			        | (encodeDirection(directionNew) << AirData.GENERATOR_DIRECTION_SHIFT);
			directionGenerator = directionNew;
			isUpdated = true;
		}
		if (isUpdated && chunkData != null) {
			chunkData.setDataAir(blockPos.getX(), blockPos.getY(), blockPos.getZ(), dataAir);
		}
	}

	protected void removeGeneratorAndCascade(final World world) throws ExceptionChunkNotLoaded {
		removeGeneratorAndCascade(world, VOLUME_UPDATE_DEPTH_BLOCKS);
	}

	/**
	 * Clear generator pressure here and follow it outward, so removing a generator depressurises
	 * the whole volume it was feeding rather than leaving a stale bubble.
	 */
	private void removeGeneratorAndCascade(final World world, final int depth) throws ExceptionChunkNotLoaded {
		if (pressureGenerator == 0) {
			return;
		}
		dataAir = (dataAir & ~(AirData.GENERATOR_PRESSURE_MASK | AirData.GENERATOR_DIRECTION_MASK))
		        | (encodeDirection(null) << AirData.GENERATOR_DIRECTION_SHIFT);
		pressureGenerator = 0;
		directionGenerator = null;
		chunkData.setDataAir(blockPos.getX(), blockPos.getY(), blockPos.getZ(), dataAir);

		if (depth <= 0) {
			return;
		}
		final StateAir stateAir = new StateAir(chunkData);
		for (final Direction direction : Direction.values()) {
			stateAir.refresh(world, this, direction);
			// only follow blocks that were being fed through us
			if ( stateAir.pressureGenerator > 0
			  && stateAir.directionGenerator == direction.getOpposite() ) {
				stateAir.removeGeneratorAndCascade(world, depth - 1);
			}
		}
	}

	protected void setVoid(final short pressureNew, @Nullable final Direction directionNew) {
		boolean isUpdated = false;
		if (pressureNew != pressureVoid) {
			dataAir = (dataAir & ~AirData.VOID_PRESSURE_MASK)
			        | (pressureNew << AirData.VOID_PRESSURE_SHIFT);
			pressureVoid = pressureNew;
			isUpdated = true;
		}
		if (directionNew != directionVoid) {
			dataAir = (dataAir & ~AirData.VOID_DIRECTION_MASK)
			        | (encodeDirection(directionNew) << AirData.VOID_DIRECTION_SHIFT);
			directionVoid = directionNew;
			isUpdated = true;
		}
		if (isUpdated && chunkData != null) {
			chunkData.setDataAir(blockPos.getX(), blockPos.getY(), blockPos.getZ(), dataAir);
		}
	}

	protected void removeVoidAndCascade(final World world) throws ExceptionChunkNotLoaded {
		removeVoidAndCascade(world, VOLUME_UPDATE_DEPTH_BLOCKS);
	}

	private void removeVoidAndCascade(final World world, final int depth) throws ExceptionChunkNotLoaded {
		if (pressureVoid == 0) {
			return;
		}
		dataAir = (dataAir & ~(AirData.VOID_PRESSURE_MASK | AirData.VOID_DIRECTION_MASK))
		        | (encodeDirection(null) << AirData.VOID_DIRECTION_SHIFT);
		pressureVoid = 0;
		directionVoid = null;
		chunkData.setDataAir(blockPos.getX(), blockPos.getY(), blockPos.getZ(), dataAir);

		if (depth <= 0) {
			return;
		}
		final StateAir stateAir = new StateAir(chunkData);
		for (final Direction direction : Direction.values()) {
			stateAir.refresh(world, this, direction);
			if ( stateAir.pressureVoid > 0
			  && stateAir.directionVoid == direction.getOpposite() ) {
				stateAir.removeVoidAndCascade(world, depth - 1);
			}
		}
	}

	// ===== queries =====

	public boolean isAir() {
		return (dataAir & AirData.BLOCK_MASK) != AirData.BLOCK_SEALER;
	}

	/** Whether air can move through this block when travelling in the given direction. */
	public boolean isAir(final Direction direction) {
		switch (dataAir & AirData.BLOCK_MASK) {
			case AirData.BLOCK_AIR_PLACEABLE:
			case AirData.BLOCK_AIR_FLOW:
			case AirData.BLOCK_AIR_SOURCE:
			case AirData.BLOCK_AIR_NON_PLACEABLE:
				return true;
			case AirData.BLOCK_AIR_NON_PLACEABLE_V:
				return direction.getStepY() != 0;
			case AirData.BLOCK_AIR_NON_PLACEABLE_H:
				return direction.getStepY() == 0;
			default:
				return false;
		}
	}

	public boolean isAirSource() {
		return (dataAir & AirData.BLOCK_MASK) == AirData.BLOCK_AIR_SOURCE;
	}

	public boolean isAirFlow() {
		return (dataAir & AirData.BLOCK_MASK) == AirData.BLOCK_AIR_FLOW;
	}

	public boolean isVoidSource() {
		return pressureVoid == AirData.VOID_PRESSURE_MAX;
	}

	protected boolean isLeakingHorizontally() {
		return (dataAir & AirData.BLOCK_MASK) == AirData.BLOCK_AIR_NON_PLACEABLE_H;
	}

	protected boolean isLeakingVertically() {
		return (dataAir & AirData.BLOCK_MASK) == AirData.BLOCK_AIR_NON_PLACEABLE_V;
	}

	@Override
	public String toString() {
		return String.format("StateAir %d %d %d 0x%08x concentration %d generator %d/%s void %d/%s",
			blockPos.getX(), blockPos.getY(), blockPos.getZ(), dataAir,
			concentration, pressureGenerator, directionGenerator, pressureVoid, directionVoid);
	}
}
