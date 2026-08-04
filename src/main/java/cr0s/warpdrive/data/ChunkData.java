package cr0s.warpdrive.data;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.ExceptionChunkNotLoaded;
import cr0s.warpdrive.debug.DebugLog;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nonnull;
import java.util.Arrays;

/**
 * Per-chunk air state, ported from 1.12.2.
 *
 * One int per block, held in 16 vertically stacked segments of 16x16x16. Segments are allocated
 * lazily and dropped when empty, so a chunk with no air anywhere costs nothing beyond this object -
 * which matters, because in space that is almost every chunk.
 *
 * The index packs to {@code y << 8 | x << 4 | z}, so the top four bits of Y select the segment and
 * the low twelve address the block inside it.
 *
 * Persisted into the chunk's own NBT via ChunkHandler, so air survives save and reload along with
 * the blocks it belongs to.
 *
 * Deliberately dropped from the original: the RELOAD_DELAY_MIN_MS / LOAD_UNLOAD_DELAY_MIN_MS /
 * SAVE_SAVE_DELAY_MIN_MS thresholds. They only ever drove warning logs about suspiciously fast
 * chunk churn - debugging aids for a 1.12.2 lifecycle problem, not behaviour - and 1.16.5 loads
 * chunks off-thread, which would make those warnings noise rather than signal.
 */
public class ChunkData {

	private static final String TAG_CHUNK_MOD_DATA = WarpDrive.MODID;
	private static final String TAG_VERSION = "version";
	private static final String TAG_AIR = "air";
	private static final String TAG_AIR_SEGMENT_DATA = "data";
	private static final String TAG_AIR_SEGMENT_DELAY = "delay";
	private static final String TAG_AIR_SEGMENT_Y = "y";

	/** Format version written to NBT. 1 is the 1.12.2 format, carried over unchanged. */
	private static final int DATA_VERSION = 1;

	private static final int CHUNK_SIZE_SEGMENTS = 16;      // 16 segments of 16x16x16 blocks
	private static final int SEGMENT_SIZE_BLOCKS = 16 * 256;
	private static final int INVALID_DATA_INDEX = 0xFF7F;    // central block in chunk top

	/** Highest block index, i.e. world height - 1. Still 255 in 1.16.5. */
	private static final int MAX_Y = 255;

	/** Base simulation interval, 1.12.2 BREATHING_AIR_SIMULATION_DELAY_TICKS. */
	private static final int AIR_SIMULATION_DELAY_TICKS = 30;

	// persistent state
	private final int[][] dataAirSegments = new int[CHUNK_SIZE_SEGMENTS][];
	private final byte[][] tickAirSegments = new byte[CHUNK_SIZE_SEGMENTS][];

	// transient state
	private final ChunkPos chunkPos;
	private boolean isLoaded;
	public boolean isModified;

	/**
	 * Rolling 7-bit simulation clock. Seeded per chunk so that chunks do not all tick their air on
	 * the same game tick, which would produce a periodic stall.
	 */
	private int tickCurrent;

	public ChunkData(final int xChunk, final int zChunk) {
		this.chunkPos = new ChunkPos(xChunk, zChunk);
		this.isLoaded = false;
		// 1.12.2 seeded this from Math.random(). Deriving it from the coordinates decorrelates
		// neighbouring chunks just as well, and makes a given chunk's schedule reproducible.
		this.tickCurrent = ((xChunk * 31 + zChunk) & 0x7F);
	}

	public boolean isLoaded() {
		return isLoaded;
	}

	public ChunkPos getChunkPos() {
		return chunkPos;
	}

	/** Representative position for logging - centre of the chunk column. */
	public BlockPos getChunkPosition() {
		return new BlockPos(chunkPos.getMinBlockX() + 8, 128, chunkPos.getMinBlockZ() + 8);
	}

	// ===== persistence =====

	public void load(@Nonnull final CompoundNBT tagCompoundChunk) {
		Arrays.fill(dataAirSegments, null);
		Arrays.fill(tickAirSegments, null);
		isModified = false;

		if (tagCompoundChunk.contains(TAG_CHUNK_MOD_DATA)) {
			final CompoundNBT tagCompound = tagCompoundChunk.getCompound(TAG_CHUNK_MOD_DATA);
			final int version = tagCompound.getInt(TAG_VERSION);

			if (version == DATA_VERSION) {
				final ListNBT tagList = tagCompound.getList(TAG_AIR, Constants.NBT.TAG_COMPOUND);
				if (tagList.size() != CHUNK_SIZE_SEGMENTS) {
					if (!tagList.isEmpty()) {
						WarpDrive.logger.error(String.format(
							"Chunk %s loaded with invalid air data, restoring default", chunkPos));
					}
				} else {
					for (int indexSegment = 0; indexSegment < CHUNK_SIZE_SEGMENTS; indexSegment++) {
						loadSegment(tagList.getCompound(indexSegment), indexSegment);
					}
				}
			} else if (version != 0) {
				WarpDrive.logger.error(String.format(
					"Chunk %s has air data version %d, expected %d - discarding",
					chunkPos, version, DATA_VERSION));
			}
		}

		isLoaded = true;
	}

	private void loadSegment(final CompoundNBT tagSegment, final int indexSegment) {
		final int[] intData = tagSegment.getIntArray(TAG_AIR_SEGMENT_DATA);

		// An absent segment is the normal case - it just means no air was ever there
		if (intData.length != SEGMENT_SIZE_BLOCKS) {
			if (intData.length != 0) {
				WarpDrive.logger.error(String.format(
					"Chunk %s loaded with invalid air segment %d, restoring default",
					chunkPos, indexSegment));
			}
			return;
		}

		final int indexRead = tagSegment.getByte(TAG_AIR_SEGMENT_Y);
		if (indexRead != indexSegment) {
			WarpDrive.logger.error(String.format(
				"Chunk %s air segment index mismatch: read %d, expected %d",
				chunkPos, indexRead, indexSegment));
		}

		byte[] byteTick = tagSegment.getByteArray(TAG_AIR_SEGMENT_DELAY);
		if (byteTick.length != SEGMENT_SIZE_BLOCKS) {
			byteTick = new byte[SEGMENT_SIZE_BLOCKS];
		}

		final int[] dataAirSegment = new int[SEGMENT_SIZE_BLOCKS];
		final byte[] tickAirSegment = new byte[SEGMENT_SIZE_BLOCKS];
		for (int indexBlock = 0; indexBlock < SEGMENT_SIZE_BLOCKS; indexBlock++) {
			// Mask on read so unknown bits from a future format cannot leak into the simulation
			dataAirSegment[indexBlock] = intData[indexBlock] & AirData.USED_MASK;
			tickAirSegment[indexBlock] = (byte) (byteTick[indexBlock] & 0x7F);
		}
		dataAirSegments[indexSegment] = dataAirSegment;
		tickAirSegments[indexSegment] = tickAirSegment;
	}

	public void save(@Nonnull final CompoundNBT tagCompoundChunk) {
		isModified = false;

		final CompoundNBT tagCompound = new CompoundNBT();
		tagCompoundChunk.put(TAG_CHUNK_MOD_DATA, tagCompound);
		tagCompound.putInt(TAG_VERSION, DATA_VERSION);

		final ListNBT tagList = new ListNBT();
		for (int indexSegment = 0; indexSegment < CHUNK_SIZE_SEGMENTS; indexSegment++) {
			final CompoundNBT tagSegment = new CompoundNBT();
			tagSegment.putByte(TAG_AIR_SEGMENT_Y, (byte) indexSegment);

			// Empty segments are written as an empty compound rather than 16 KB of zeroes. The list
			// still has to hold all 16 entries so indices stay meaningful on load.
			final int[] dataAirSegment = dataAirSegments[indexSegment];
			if (dataAirSegment != null && !isSegmentEmpty(dataAirSegment)) {
				tagSegment.putIntArray(TAG_AIR_SEGMENT_DATA, dataAirSegment.clone());
				tagSegment.putByteArray(TAG_AIR_SEGMENT_DELAY, tickAirSegments[indexSegment].clone());
			}
			tagList.add(tagSegment);
		}
		tagCompound.put(TAG_AIR, tagList);
	}

	private static boolean isSegmentEmpty(final int[] dataAirSegment) {
		for (final int dataAir : dataAirSegment) {
			if (!AirData.isEmptyData(dataAir)) {
				return false;
			}
		}
		return true;
	}

	// ===== indexing =====

	/** True when this x/z column belongs to this chunk. Y is irrelevant - every chunk spans it. */
	public boolean isInside(final int x, final int z) {
		final int xInChunk = x - (chunkPos.x << 4);
		final int zInChunk = z - (chunkPos.z << 4);
		return xInChunk >= 0 && xInChunk <= 15
		    && zInChunk >= 0 && zInChunk <= 15;
	}

	private int getDataIndex(final int x, final int y, final int z) {
		final int xInChunk = x - (chunkPos.x << 4);
		final int yInChunk = MathHelper.clamp(y, 0, MAX_Y);
		final int zInChunk = z - (chunkPos.z << 4);
		if (xInChunk < 0 || xInChunk > 15 || zInChunk < 0 || zInChunk > 15) {
			WarpDrive.logger.error(String.format(
				"Block position (%d %d %d) is outside chunk %s", x, y, z, chunkPos));
			return INVALID_DATA_INDEX;
		}
		return yInChunk << 8 | xInChunk << 4 | zInChunk;
	}

	BlockPos getPositionFromDataIndex(final int indexSegment, final int indexBlock) {
		final int x = (chunkPos.x << 4) + ((indexBlock & 0x00F0) >> 4);
		final int y = (indexSegment << 4) + ((indexBlock & 0x0F00) >> 8);
		final int z = (chunkPos.z << 4) + (indexBlock & 0x000F);
		return new BlockPos(x, y, z);
	}

	// ===== air data =====

	public int getDataAir(final int x, final int y, final int z) {
		final int indexData = getDataIndex(x, y, z);
		final int[] dataAirSegment = dataAirSegments[indexData >> 12];
		if (dataAirSegment == null) {
			return AirData.AIR_DEFAULT;
		}
		return dataAirSegment[indexData & 0xFFF];
	}

	public void setDataAir(final int x, final int y, final int z, final int dataAirBlock) {
		if (y < 0 || y > MAX_Y) {
			return;
		}
		final int indexData = getDataIndex(x, y, z);
		final int indexSegment = indexData >> 12;
		final int indexBlock = indexData & 0xFFF;

		int[] dataAirSegment = dataAirSegments[indexSegment];
		if (dataAirSegment == null) {
			// Do not allocate 16 KB just to store "nothing here"
			if (AirData.isEmptyData(dataAirBlock)) {
				return;
			}
			dataAirSegment = new int[SEGMENT_SIZE_BLOCKS];
			dataAirSegments[indexSegment] = dataAirSegment;
			tickAirSegments[indexSegment] = new byte[SEGMENT_SIZE_BLOCKS];
			isModified = true;
		}

		if (dataAirSegment[indexBlock] != dataAirBlock) {
			dataAirSegment[indexBlock] = dataAirBlock;
			isModified = true;
		}

		// Every write reschedules the block. The delay scales with concentration, so well-sealed
		// air settles into an infrequent tick while thin or leaking air is revisited quickly -
		// this is the whole load-management strategy, not an optimisation bolted on top.
		final int delay = AIR_SIMULATION_DELAY_TICKS + AirData.getConcentration(dataAirBlock);
		tickAirSegments[indexSegment][indexBlock] = (byte) ((tickCurrent + delay) & 0x7F);
	}

	/**
	 * Invalidate the cached block classification at this position and bring its next simulation
	 * forward. Called when a block changes.
	 *
	 * The second part matters as much as the first: breaking a wall should vent the room promptly,
	 * not after the full simulation delay. The schedule is a rolling 7-bit counter, so the delay is
	 * the wrapped difference between a block's stamp and the current tick.
	 */
	public void onBlockUpdated(final int x, final int y, final int z) {
		if (y < 0 || y > MAX_Y) {
			return;
		}
		final int indexData = getDataIndex(x, y, z);
		final int indexSegment = indexData >> 12;
		final int[] dataAirSegment = dataAirSegments[indexSegment];
		if (dataAirSegment == null) {
			return;
		}
		final int indexBlock = indexData & 0xFFF;

		// force the block classification to be re-read
		dataAirSegment[indexBlock] &= ~AirData.BLOCK_MASK;
		isModified = true;

		final byte[] tickAirSegment = tickAirSegments[indexSegment];
		final byte tickAir = tickAirSegment[indexBlock];
		final int delay = (0x80 + tickAir - tickCurrent) & 0x7F;
		// pull anything further out than 16 ticks forward, unless it is already on the standard
		// concentration-derived schedule
		if ( delay > 15
		  && delay != AIR_SIMULATION_DELAY_TICKS + AirData.getConcentration(dataAirSegment[indexBlock]) ) {
			tickAirSegment[indexBlock] = (byte) ((tickCurrent + delay & 0x0F) & 0x7F);
		}
	}

	/**
	 * Advance this chunk's air by one tick, simulating only the blocks that are due.
	 *
	 * Scanning every allocated segment each tick looks expensive, but the skip is a single mask
	 * test and unallocated segments cost nothing - which is almost all of them, since a chunk only
	 * allocates where air has actually been. The per-block schedule then keeps the number of blocks
	 * genuinely simulated far below the number scanned.
	 *
	 * Segments that have emptied out are released here, so a depressurised ship stops costing
	 * anything rather than holding 16 KB per segment forever.
	 */
	public void updateTick(@Nonnull final World world) {
		tickCurrent = (tickCurrent + 1) & 0xFF;
		final int tickNow = tickCurrent & 0x7F;

		for (int indexSegment = 0; indexSegment < CHUNK_SIZE_SEGMENTS; indexSegment++) {
			final int[] dataAirSegment = dataAirSegments[indexSegment];
			if (dataAirSegment == null) {
				continue;
			}
			final byte[] tickAirSegment = tickAirSegments[indexSegment];

			int countEmpty = 0;
			for (int indexBlock = 0; indexBlock < SEGMENT_SIZE_BLOCKS; indexBlock++) {
				final int dataAirBlock = dataAirSegment[indexBlock];
				if (AirData.isEmptyData(dataAirBlock)) {
					countEmpty++;
					continue;
				}
				if (tickAirSegment[indexBlock] != tickNow) {
					continue;
				}

				final int x = (chunkPos.x << 4) + ((indexBlock & 0x00F0) >> 4);
				final int y = (indexSegment << 4) + ((indexBlock & 0x0F00) >> 8);
				final int z = (chunkPos.z << 4) + (indexBlock & 0x000F);
				try {
					AirSpreader.execute(world, x, y, z);
				} catch (final ExceptionChunkNotLoaded exception) {
					// Expected at the edge of loaded space; the block resumes when the chunk returns
					DebugLog.log("AIR", "simulation deferred at {} {} {}", x, y, z);
				}
			}

			if (countEmpty == SEGMENT_SIZE_BLOCKS) {
				dataAirSegments[indexSegment] = null;
				tickAirSegments[indexSegment] = null;
			}
		}

		if (isModified) {
			isModified = false;
			final IChunk chunk = world.getChunk(chunkPos.x, chunkPos.z, ChunkStatus.FULL, false);
			if (chunk != null) {
				chunk.setUnsaved(true);
			}
		}
	}

	/** True when any block in this chunk currently holds breathable air. */
	public boolean hasAir() {
		for (final int[] dataAirSegment : dataAirSegments) {
			if (dataAirSegment == null) {
				continue;
			}
			for (final int dataAirBlock : dataAirSegment) {
				if (AirData.getConcentration(dataAirBlock) != 0) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public String toString() {
		return String.format("ChunkData %s", chunkPos);
	}
}
