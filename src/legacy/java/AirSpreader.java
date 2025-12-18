package cr0s.warpdrive.data;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.ExceptionChunkNotLoaded;
import cr0s.warpdrive.block.breathing.BlockAirGeneratorTiered;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.network.PacketHandler;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.world.World;

public class AirSpreader {
	
	private static final StateAir stateCenter = new StateAir(null);
	private static final StateAir[] stateAround = {
		new StateAir(null), new StateAir(null), new StateAir(null),
		new StateAir(null), new StateAir(null), new StateAir(null) };
	
	protected static void execute(final World world, final int x, final int y, final int z) throws ExceptionChunkNotLoaded {
		// note: compared to the pure block implementation, 0 really means no air, so we no longer offset by 1 on read/write
		
		// get central block state
		stateCenter.refresh(world, x, y, z);
		// force block refresh
		stateCenter.updateBlockCache(world);
		
		// skip non-air blocks
		if (!stateCenter.isAir()) {
			stateCenter.setConcentration(world, (byte) 0);
			stateCenter.removeGeneratorAndCascade(world);
			stateCenter.removeVoidAndCascade(world);
			return;
		}
		
		// identify leaking directions
		final EnumFacing[] directions;
		if (stateCenter.isLeakingHorizontally()) {
			directions = EnumFacing.HORIZONTALS;
		} else if (stateCenter.isLeakingVertically()) {
			directions = Commons.FACINGS_VERTICAL;
		} else {
			directions = EnumFacing.VALUES;
		}
		
		// collect air state in adjacent blocks
		// - biggest generator/void pressure around (excluding center block)
		int max_pressureGenerator = 0;
		EnumFacing max_directionGenerator = null;
		int max_pressureVoid = 0;
		EnumFacing max_directionVoid = null;
		// - accumulated air concentration including center block
		final int concentration = stateCenter.concentration;
		int sum_concentration = concentration;
		int max_concentration = concentration;
		int min_concentration = concentration;
		// - number of blocks to consider
		int air_count = 1;
		int empty_count = 0;
		
		for (final EnumFacing forgeDirection : directions) {
			final StateAir stateAir = stateAround[forgeDirection.ordinal()];
			stateAir.refresh(world,
			                 x + forgeDirection.getXOffset(),
			                 y + forgeDirection.getYOffset(),
			                 z + forgeDirection.getZOffset());
			if (stateAir.isAir(forgeDirection)) {
				air_count++;
				if (stateAir.concentration > 0) {// (note1)
					sum_concentration += stateAir.concentration;
					max_concentration = Math.max(max_concentration, stateAir.concentration);
					min_concentration = Math.min(min_concentration, stateAir.concentration);
				} else {
					empty_count++;
				}
				// keep highest generator pressure that's going towards us
				if (max_pressureGenerator < stateAir.pressureGenerator) {
					if ( stateAir.isAirSource()
					  || stateAir.directionGenerator != null ) {
						max_pressureGenerator = stateAir.pressureGenerator;
						max_directionGenerator = forgeDirection;
					}
				}
				// keep highest void pressure that's going towards us
				if (max_pressureVoid < stateAir.pressureVoid) {
					if ( stateAir.isVoidSource()
					  || stateAir.directionVoid != null ) {
						max_pressureVoid = stateAir.pressureVoid;
						max_directionVoid = forgeDirection;
					}
				}
			}
		}
		
		// update volume detection, skipping the sources
		if (!stateCenter.isAirSource()) {
			// propagate generator pressure if bigger pressure existing around, erase pressure otherwise
			if ( stateCenter.pressureGenerator < max_pressureGenerator
			  && max_pressureGenerator > 1 ) {
				stateCenter.setGeneratorAndUpdateVoid(world, (short) (max_pressureGenerator - 1), max_directionGenerator);
			} else if (stateCenter.pressureGenerator != 0) {
				stateCenter.removeGeneratorAndCascade(world);
				
				// invalidate cache
				for (final EnumFacing direction : directions) {
					final StateAir stateAir = stateAround[direction.ordinal()];
					stateAir.refresh(world,
					                 x + direction.getXOffset(),
					                 y + direction.getYOffset(),
					                 z + direction.getZOffset());
				}
			}
		}
		
		if (!stateCenter.isVoidSource()) {
			// propagate void pressure if bigger pressure exists around, erase pressure otherwise
			if ( stateCenter.pressureVoid < max_pressureVoid
			  && max_pressureVoid > 1 ) {
				stateCenter.setVoid((short) (max_pressureVoid - 1), max_directionVoid);
			} else if (stateCenter.pressureVoid != 0) {
				stateCenter.removeVoidAndCascade(world);
				
				// invalidate cache
				for (final EnumFacing direction : directions) {
					final StateAir stateAir = stateAround[direction.ordinal()];
					stateAir.refresh(world,
					                 x + direction.getXOffset(),
					                 y + direction.getYOffset(),
					                 z + direction.getZOffset());
				}
			}
		}
		
		if (sum_concentration == 0) {
			if ( stateCenter.pressureVoid > 0
			  && stateCenter.pressureGenerator > 0
			  && stateCenter.directionGenerator == stateCenter.directionVoid.getOpposite() ) {
				if (world.rand.nextInt(4) == 2) {
					final Vector3 v3Origin = new Vector3(x + 0.5D, y + 0.5D, z + 0.5D);
					final Vector3 v3Direction = new Vector3(stateCenter.directionVoid).scale(0.5D);
					PacketHandler.sendSpawnParticlePacket(world, "cloud", (byte) 2, v3Origin, v3Direction,
					                                      0.20F + 0.10F * world.rand.nextFloat(),
					                                      0.25F + 0.25F * world.rand.nextFloat(),
					                                      0.60F + 0.30F * world.rand.nextFloat(),
					                                      0.0F,
					                                      0.0F,
					                                      0.0F, 32);
				}
			}
			return;
		}
		
		// air leaks means penalty plus some randomization for visual effects
		if (empty_count > 0) {
			if (concentration < 4) {
				sum_concentration -= empty_count + (world.rand.nextBoolean() ? 0 : empty_count);
			} else if (concentration < 8) {
				sum_concentration -= empty_count;
			} else if (concentration < 12) {
				sum_concentration -= air_count;
			}
		}
		if (sum_concentration < 0) sum_concentration = 0;
		
		// compute new concentration, buffing closed space
		int mid_concentration;
		int new_concentration;
		final boolean isGrowth = stateCenter.pressureGenerator > 0
		                      && (stateCenter.pressureVoid == 0 || stateCenter.isAirSource())
		                      && max_concentration - min_concentration > 2;
		if (isGrowth) {
			mid_concentration = (int) Math.ceil(sum_concentration / (float) air_count);
			new_concentration = sum_concentration - mid_concentration * (air_count - 1);
			new_concentration = Math.max(Math.max(concentration + 1, max_concentration - 1), new_concentration - 20);
		} else {
			mid_concentration = (int) Math.floor(sum_concentration / (float) air_count);
			new_concentration = sum_concentration - mid_concentration * (air_count - 1);
			if (empty_count > 0) {
				new_concentration = Math.max(0, new_concentration - 5);
			}
		}
		// apply (de)pressurisation effects
		if (stateCenter.pressureVoid > 0) {
			mid_concentration = Math.min(mid_concentration, 160);
			new_concentration = Math.min(new_concentration, 160);
		} else if (stateCenter.pressureGenerator > 20 && new_concentration > 16) {
			mid_concentration += 2;
			new_concentration += 2;
		}
		
		// apply scale and clamp
		if (mid_concentration < 0) {
			mid_concentration = 0;
		} else if (mid_concentration > StateAir.CONCENTRATION_MAX) {
			mid_concentration = StateAir.CONCENTRATION_MAX;
		}
		if (new_concentration < 0) {
			new_concentration = 0;
		} else if (isGrowth && new_concentration > max_concentration) {
			new_concentration = Math.max(0, max_concentration);
		} else if (!isGrowth && new_concentration > max_concentration - 1) {
			new_concentration = Math.max(0, max_concentration - 1);
		}
		if (WarpDrive.isDev) {
			assert new_concentration >= 0;
			assert mid_concentration >= 0;
			assert new_concentration <= StateAir.CONCENTRATION_MAX;
			assert mid_concentration <= StateAir.CONCENTRATION_MAX;
			if (WarpDriveConfig.LOGGING_BREATHING) {
				final StringBuilder debugConcentrations = new StringBuilder();
				for (final EnumFacing forgeDirection : directions) {
					debugConcentrations.append(String.format(" %3d", stateAround[forgeDirection.ordinal()].concentration));
				}
				WarpDrive.logger.info(String.format("Updating air 0x%8x @ %6d %3d %6d %s from %3d near %s total %3d, empty %d/%d -> %3d + %d * %3d",
				                                    stateCenter.dataAir, x, y, z,
				                                    isGrowth ? "growing" : "stable ",
				                                    concentration,
				                                    debugConcentrations.toString(),
				                                    sum_concentration, empty_count, air_count,
				                                    new_concentration, air_count - 1, mid_concentration) );
			}
			
			// new_concentration = mid_concentration = 0;
		}
		
		// protect air generator
		final MutableBlockPos mutableBlockPos = new MutableBlockPos();
		if (concentration != new_concentration) {
			if (!stateCenter.isAirSource()) {
				if ( stateCenter.directionGenerator != null
				  || concentration > new_concentration ) {
					stateCenter.setConcentration(world, (byte) new_concentration);
				} else if (WarpDriveConfig.LOGGING_BREATHING) {
					WarpDrive.logger.warn(String.format("AirSpreader trying to increase central concentration without a generator in range at %s",
					                                    stateCenter));
				}
			} else {
				boolean hasGenerator = false;
				final IBlockState blockStateSource = stateCenter.getBlockState(world);
				if (stateCenter.isAirSource()) {
					final EnumFacing facingSource = blockStateSource.getValue(BlockProperties.FACING);
					final IBlockState blockStateGenerator = world.getBlockState(mutableBlockPos.setPos(
							x - facingSource.getXOffset(),
							y - facingSource.getYOffset(),
							z - facingSource.getZOffset()));
					if (blockStateGenerator.getBlock() instanceof BlockAirGeneratorTiered) {
						final EnumFacing facingGenerator = blockStateGenerator.getValue(BlockProperties.FACING);
						if (facingGenerator.equals(facingSource)) {
							// all good
							hasGenerator = true;
						}
					}
					if (!hasGenerator) {
						if (WarpDriveConfig.LOGGING_BREATHING) {
							WarpDrive.logger.info(String.format("AirGenerator not found, removing AirSource block at (%d %d %d) -> expecting BlockAirGeneratorTiered, found %s",
							                                    x, y, z, blockStateGenerator.getBlock()));
						}
						stateCenter.removeAirSource(world);
					}
				}
			}
		} else if (stateCenter.isAirFlow() && new_concentration == 0) {// invalid state detected => report and clear
			WarpDrive.logger.error(String.format("AirSpreader removing invalid central airFlow of %s", stateCenter));
			stateCenter.setConcentration(world, (byte) new_concentration);
		}
		
		// Check and update air to adjacent blocks
		// (do not overwrite source block, do not decrease neighbors if we're growing)
		for (final EnumFacing forgeDirection : directions) {
			final StateAir stateAir = stateAround[forgeDirection.ordinal()];
			if ( stateAir.isAirFlow()
			  || (stateAir.isAir(forgeDirection) && !stateAir.isAirSource()) ) {
				if ( stateAir.concentration != mid_concentration
				  && (!isGrowth || stateAir.concentration < mid_concentration)) {
					stateAir.setConcentration(world, (byte) mid_concentration);
				} else if (mid_concentration == 0 && stateAir.concentration != 0) {
					if (WarpDriveConfig.LOGGING_BREATHING) {
						WarpDrive.logger.warn(String.format("AirSpreader removing connected airFlow of %s", stateAir));
					}
					stateAir.setConcentration(world, (byte) mid_concentration);
				}
			}
		}
	}
	
	public static void clearCache() {
		stateCenter.clearCache();
		for (final StateAir stateAir : stateAround) {
			stateAir.clearCache();
		}
	}
}
