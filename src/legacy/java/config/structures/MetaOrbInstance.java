package cr0s.warpdrive.config.structures;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.FastSetBlockState;
import cr0s.warpdrive.LocalProfiler;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.config.GenericSet;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.config.Filler;
import cr0s.warpdrive.data.JumpBlock;
import cr0s.warpdrive.data.VectorI;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class MetaOrbInstance extends OrbInstance {
	
	private static final int CORE_MAX_TRIES = 10;
	protected final MetaShellInstance metaShellInstance;
	
	protected int radiusTotal;
	private int sizePregen;
	private ArrayList<BlockPos> blockPoses;
	private int xMin, xMax, yMin, yMax, zMin, zMax;
	
	public MetaOrbInstance(final MetaOrb metaOrb, final Random random) {
		super(metaOrb, random);
		metaShellInstance = new MetaShellInstance(metaOrb, random);
		
		constructionFinalizer();
	}
	
	private void constructionFinalizer() {
		radiusTotal = totalThickness + metaShellInstance.radius + metaShellInstance.locations.size();
		sizePregen = (int) Math.ceil(Math.PI * 4.0F / 3.0F * Math.pow(radiusTotal + 1, 3));
		blockPoses = new ArrayList<>(sizePregen);
	}
	
	@Override
	public boolean generate(@Nonnull final World world, @Nonnull final Random random, @Nonnull final BlockPos blockPos) {
		if (WarpDriveConfig.LOGGING_WORLD_GENERATION) {
			WarpDrive.logger.info(String.format("Generating MetaOrb %s of %d cores with radius of %d (thickness of %d) at %s",
			                                    structure.getFullName(), metaShellInstance.count, radiusTotal, totalThickness, Commons.format(world, blockPos)));
		}
		LocalProfiler.start(String.format("[MetaOrbInstance] Generating %s of %d cores with radius of %d (thickness of %d)",
		                                  structure.getFullName(), metaShellInstance.count, radiusTotal, totalThickness));
		
		final int y2 = Math.min(WarpDriveConfig.SPACE_GENERATOR_Y_MAX_BORDER - radiusTotal,
		                        Math.max(blockPos.getY(), WarpDriveConfig.SPACE_GENERATOR_Y_MIN_BORDER + radiusTotal));
		final BlockPos blockPosUpdated = y2 == blockPos.getY() ? blockPos : new BlockPos(blockPos.getX(), y2, blockPos.getZ());
		if (((MetaOrb) structure).metaShell == null) {
			return super.generate(world, random, blockPosUpdated);
		}
		
		// prepare bounding box
		xMin = xMax = blockPosUpdated.getX();
		yMin = yMax = blockPosUpdated.getY();
		zMin = zMax = blockPosUpdated.getZ();
		
		// generate the meta orb shape
		tickScheduleBlocks(world, random, blockPosUpdated);
		
		// place the core blocks
		if (metaShellInstance.block != null) {
			for (final VectorI location: metaShellInstance.locations) {
				final BlockPos blockPosCore = new BlockPos(blockPosUpdated.getX() + location.x, blockPosUpdated.getY() + location.y, blockPosUpdated.getZ() + location.z);
				world.setBlockState(blockPosCore, metaShellInstance.block.getStateFromMeta(metaShellInstance.metadata), 2);
			}
		}
		
		updateClient(world);
		
		// verify for undersized area
		xMin = blockPosUpdated.getX() - xMin;
		xMax = xMax - blockPosUpdated.getX();
		yMin = blockPosUpdated.getY() - yMin;
		yMax = yMax - blockPosUpdated.getY();
		zMin = blockPosUpdated.getZ() - zMin;
		zMax = zMax - blockPosUpdated.getZ();
		if ( Math.max(xMin, xMax) >= radiusTotal
		  || Math.max(yMin, yMax) >= radiusTotal
		  || Math.max(zMin, zMax) >= radiusTotal ) {
			WarpDrive.logger.warn(String.format("Generated undersized MetaOrb %s of %d cores with radius of %d (thickness of %d) at %s: bounding box(x %d %d y %d %d z %d %d)",
			                                    structure.getFullName(), metaShellInstance.count, radiusTotal, totalThickness, Commons.format(world, blockPos),
			                                    xMin, xMax, yMin, yMax, zMin, zMax ));
		}
		
		LocalProfiler.stop();
		return false;
	}
	
	private void tickScheduleBlocks(@Nonnull final World world, @Nonnull final Random random, @Nonnull final BlockPos blockPos) {
		LocalProfiler.start("[MetaOrbInstance] Placing blocks");
		
		// square shell thickness from center of a core block
		final double sqRadiusHigh = (totalThickness + 0.5D) * (totalThickness + 0.5D);
		final double sqRadiusLow = (totalThickness - 0.5D) * (totalThickness - 0.5D);
		
		// actual radius to consider
		final int radiusCeil = radiusTotal;
		
		// Pass the cube and check points for sphere equation x^2 + y^2 + z^2 = r^2
		for (int x = -radiusCeil; x <= radiusCeil; x++) {
			for (int y = -radiusCeil; y <= radiusCeil; y++) {
				for (int z = -radiusCeil; z <= radiusCeil; z++) {
					// find square range to closest core
					double dSqRangeClosest = sqRadiusHigh + 1.0D;
					double dSqStrength = 0.0D;
					for (final VectorI location : metaShellInstance.locations) {
						// Square distance from current position to that core
						final double dSqRange = (x - location.x + 0.5D) * (x - location.x + 0.5D)
						                      + (y - location.y + 0.5D) * (y - location.y + 0.5D)
						                      + (z - location.z + 0.5D) * (z - location.z + 0.5D);
						if (dSqRange < dSqRangeClosest) {
							dSqRangeClosest  = dSqRange;
						}
						dSqStrength += Math.max(0.0D, 1.0D / dSqRange);
					}
					dSqStrength = metaShellInstance.locations.size() / dSqStrength;
					
					// Skip too far blocks
					if (dSqStrength > sqRadiusHigh) {
						continue;
					}
					final boolean isSurface = dSqStrength > sqRadiusLow;
					
					// Add blocks to memory
					final int intSqRadius = (int) Math.round(dSqStrength);
					final GenericSet<Filler> orbShell = getFillerSetFromSquareRange(intSqRadius);
					
					// WarpDrive.logger.info(String.format("dSqRange %.3f sqRadiusHigh %.3f %.3f",
					//                                     dSqRange, sqRadiusHigh, sqRadiusLow));
					// note: placing block is faster from bottom to top due to skylight computations
					addBlock(world, isSurface, new JumpBlock(orbShell.getRandomUnit(random), blockPos.getX() + x, blockPos.getY() + y, blockPos.getZ() + z));
				}
			}
		}
		// verify for memory reallocation lag
		if (blockPoses != null && blockPoses.size() > sizePregen) {
			WarpDrive.logger.warn(String.format("[MetaOrbInstance] Saved %s blocks (estimated to %d)",
			                                    blockPoses.size(), sizePregen));
		}
		
		LocalProfiler.stop();
	}
	
	private void addBlock(@Nonnull final World world, final boolean isSurface, final JumpBlock jumpBlock) {
		if (jumpBlock.y < 0 || jumpBlock.y > 255) {
			return;
		}
		
		// update bounding box
		xMin = Math.min(xMin, jumpBlock.x);
		xMax = Math.max(xMax, jumpBlock.x);
		yMin = Math.min(yMin, jumpBlock.y);
		yMax = Math.max(yMax, jumpBlock.y);
		zMin = Math.min(zMin, jumpBlock.z);
		zMax = Math.max(zMax, jumpBlock.z);
		
		// Replace water with random gas (ship in moon)
		if (world.getBlockState(new BlockPos(jumpBlock.x, jumpBlock.y, jumpBlock.z)).getBlock().isAssociatedBlock(Blocks.WATER)) {
			if (world.rand.nextInt(50) != 1) {
				jumpBlock.block = WarpDrive.blockGas;
				jumpBlock.blockMeta = 0; // gasColor;
			}
		}
		final BlockPos blockPos = new BlockPos(jumpBlock.x, jumpBlock.y, jumpBlock.z);
		blockPoses.add(blockPos);
		if (isSurface && jumpBlock.x % 4 == 0 && jumpBlock.z % 4 == 0) {
			world.setBlockState(blockPos, jumpBlock.block.getStateFromMeta(jumpBlock.blockMeta), 2);
		} else {
			FastSetBlockState.setBlockStateNoLight(world, blockPos, jumpBlock.block.getStateFromMeta(jumpBlock.blockMeta), 2);
		}
	}
	
	private void updateClient(final World world) {
		LocalProfiler.start("[MetaOrbInstance] Updating client for " + blockPoses.size() + " blocks");
		
		for (final BlockPos blockPos : blockPoses) {
			final IBlockState blockState = world.getBlockState(blockPos);
			world.notifyBlockUpdate(blockPos, blockState, blockState, 3);
		}
		
		LocalProfiler.stop();
	}
	
	public class MetaShellInstance {
		
		protected final int count;
		protected final int radius;
		protected ArrayList<VectorI> locations;
		protected final Block block;
		protected final int metadata;
		
		public MetaShellInstance(@Nonnull final MetaOrb metaOrb, final Random random) {
			if (metaOrb.metaShell == null) {
				WarpDrive.logger.warn(String.format("Invalid MetalShell instance with no definition for %s",
				                                    metaOrb.getFullName()));
				count = 1;
				radius = 0;
				block = null;
				metadata = 0;
				return;
			}
			count = Commons.randomRange(random, metaOrb.metaShell.minCount, metaOrb.metaShell.maxCount);
			final double radiusMax = Math.max(metaOrb.metaShell.minRadius, metaOrb.metaShell.relativeRadius * totalThickness);
			block = metaOrb.metaShell.block;
			metadata = metaOrb.metaShell.metadata;
			
			// evaluate core positions
			locations = new ArrayList<>();
			final double diameter = Math.max(1.0D, 2.0D * radiusMax);
			final double xMin = -radiusMax;
			final double yMin = -radiusMax;
			final double zMin = -radiusMax;
			int radiusActual = 0;
			
			for (int index = 0; index < count; index++) {
				boolean found = false;
				
				for (int step = 0; step < CORE_MAX_TRIES && !found; step++) {
					final VectorI location = new VectorI(
							(int) Math.round(xMin + diameter * random.nextDouble()),
							(int) Math.round(yMin + diameter * random.nextDouble()),
							(int) Math.round(zMin + diameter * random.nextDouble()));
					if (!locations.contains(location)) {
						locations.add(location);
						radiusActual = Math.max(radiusActual,
						                        Math.max(Math.abs(location.x),
						                                 Math.max(Math.abs(location.y),
						                                          Math.abs(location.z) ) ) );
						found = true;
					}
				}
			}
			radius = radiusActual;
		}
	}
}
