package cr0s.warpdrive.block;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.api.computer.IAbstractLaser;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.EnergyWrapper;
import cr0s.warpdrive.data.EnumTier;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;

import javax.annotation.Nonnull;
import java.util.LinkedList;
import java.util.List;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;

import net.minecraftforge.fml.common.Optional;

// Abstract class to manage laser mediums
public abstract class TileEntityAbstractLaser extends TileEntityAbstractEnergyBase implements IAbstractLaser {
	
	// configuration overridden by derived classes
	protected EnumFacing[] laserMedium_directionsValid = EnumFacing.values();
	protected int laserMedium_maxCount = 0;
	
	// computed properties
	protected EnumFacing laserMedium_direction = null;
	protected int cache_laserMedium_count = 0;
	protected double cache_laserMedium_factor = 1.0D;
	protected long cache_laserMedium_energyStored = 0L;
	protected long cache_laserMedium_maxStorage = 0L;
	
	public TileEntityAbstractLaser() {
		super();
		
		addMethods(new String[] {
				"getEnergyRequired",
				"laserMediumDirection",
				"laserMediumCount"
		});
	}
	
	@Override
	protected boolean doScanAssembly(final boolean isDirty, final WarpDriveText textReason) {
		final boolean isValid = super.doScanAssembly(isDirty, textReason);
		
		assert laserMedium_maxCount != 0;
		
		for (final EnumFacing facing : laserMedium_directionsValid) {
			TileEntity tileEntity = world.getTileEntity(pos.offset(facing));
			
			if (tileEntity instanceof TileEntityLaserMedium) {
				// at least one found
				final EnumTier enumTier = ((TileEntityLaserMedium) tileEntity).enumTier;
				if (enumTier == null) {
					WarpDrive.logger.error(String.format("Invalid NULL tier for %s, isFirstTick %s",
					                                     tileEntity, ((TileEntityLaserMedium) tileEntity).isFirstTick() ));
					WarpDrive.logger.error(String.format("NBT is %s",
					                                     tileEntity.writeToNBT(new NBTTagCompound()) ));
				} else {
					long energyStored = 0;
					long maxStorage = 0;
					int count = 0;
					while ((tileEntity instanceof TileEntityLaserMedium)
					       && count <= laserMedium_maxCount) {
						// check tier
						if (enumTier != ((TileEntityLaserMedium) tileEntity).enumTier) {
							break;
						}
						
						// add current one
						energyStored += ((TileEntityLaserMedium) tileEntity).energy_getEnergyStored();
						maxStorage += ((TileEntityLaserMedium) tileEntity).energy_getMaxStorage();
						count++;
						
						// check next one
						tileEntity = world.getTileEntity(pos.offset(facing, count + 1));
					}
					
					// save results
					laserMedium_direction = facing;
					cache_laserMedium_count = count;
					cache_laserMedium_factor = Math.max(1.0D, count * WarpDriveConfig.LASER_MEDIUM_FACTOR_BY_TIER[enumTier.getIndex()]);
					cache_laserMedium_energyStored = energyStored;
					cache_laserMedium_maxStorage = maxStorage;
					return isValid;
				}
			}
		}
		
		// nothing found
		laserMedium_direction = null;
		cache_laserMedium_count = 0;
		cache_laserMedium_factor = 0.0D;
		cache_laserMedium_energyStored = 0;
		cache_laserMedium_maxStorage = 0;
		textReason.append(Commons.getStyleWarning(), "warpdrive.laser.status_line.missing_laser_medium",
		                  Commons.format(laserMedium_directionsValid) );
		return false;
	}
	
	public int laserMedium_getEnergyStored(final boolean isCached) {
		if (isCached) {
			return (int) cache_laserMedium_energyStored;
		}
		return laserMedium_consumeUpTo(Integer.MAX_VALUE, true);
	}
	
	public int laserMedium_getCount() {
		return cache_laserMedium_count;
	}
	
	protected boolean laserMedium_consumeExactly(final int amountRequested, final boolean simulate) {
		final int amountSimulated = laserMedium_consumeUpTo(amountRequested, true);
		if (simulate) {
			return amountRequested <= amountSimulated;
		}
		if (amountRequested > amountSimulated) {
			return false;
		}
		return amountRequested <= laserMedium_consumeUpTo(amountRequested, false);
	}
	
	protected int laserMedium_consumeUpTo(final int amount, final boolean simulate) {
		if (laserMedium_direction == null) {
			return 0;
		}
		
		// Primary scan of all laser mediums
		long totalEnergy = 0L;
		int count = 1;
		final List<TileEntityLaserMedium> laserMediums = new LinkedList<>();
		for (; count <= laserMedium_maxCount; count++) {
			final TileEntity tileEntity = world.getTileEntity(pos.offset(laserMedium_direction, count));
			if (!(tileEntity instanceof TileEntityLaserMedium)) {
				break;
			}
			laserMediums.add((TileEntityLaserMedium) tileEntity);
			totalEnergy += ((TileEntityLaserMedium) tileEntity).energy_getEnergyStored();
		}
		count--;
		cache_laserMedium_energyStored = totalEnergy;
		if (count == 0) {
			laserMedium_direction = null;
			cache_laserMedium_factor = 1.0D;
			cache_laserMedium_count = 0;
			cache_laserMedium_maxStorage = 0L;
			return 0;
		}
		if (simulate) {
			return (int) Math.min(amount, totalEnergy);
		}
		
		// Compute average energy to get per laser medium, capped at its capacity
		int energyAverage = amount / count;
		int energyLeftOver = amount - energyAverage * count;
		if (energyAverage >= WarpDriveConfig.LASER_MEDIUM_MAX_ENERGY_STORED_BY_TIER[EnumTier.SUPERIOR.getIndex()]) {
			// (we're set to consume all available energy, so we cap it to prevent overflows)
			energyAverage = WarpDriveConfig.LASER_MEDIUM_MAX_ENERGY_STORED_BY_TIER[EnumTier.SUPERIOR.getIndex()];
			energyLeftOver = 0;
		}
		
		// Secondary scan for laser medium below the required average
		for (final TileEntityLaserMedium laserMedium : laserMediums) {
			final long energyStored = laserMedium.energy_getEnergyStored();
			if (energyStored < energyAverage) {
				energyLeftOver += energyAverage - energyStored;
			}
		}
		
		// Third and final pass for energy consumption
		int energyTotalConsumed = 0;
		for (final TileEntityLaserMedium laserMedium : laserMediums) {
			final long energyStored = laserMedium.energy_getEnergyStored();
			final long energyToConsume = Math.min(energyStored, energyAverage + energyLeftOver);
			energyLeftOver -= Math.max(0, energyToConsume - energyAverage);
			laserMedium.energy_consume(energyToConsume, false); // simulate is always false here
			energyTotalConsumed += energyToConsume;
		}
		cache_laserMedium_energyStored -= energyTotalConsumed;
		return energyTotalConsumed;
	}
	
	// EnergyBase override
	@Override
	public Object[] getEnergyStatus() {
		final String units = energy_getDisplayUnits();
		return new Object[] {
				EnergyWrapper.convert(cache_laserMedium_energyStored, units),
				EnergyWrapper.convert(cache_laserMedium_maxStorage, units),
				units };
	}
	
	// Common OC/CC methods / IAbstractLaser overrides
	@Override
	public abstract Object[] getEnergyRequired();
	
	@Override
	public Object[] laserMediumDirection() {
		return new Object[] {
			laserMedium_direction.name(),
			laserMedium_direction.getXOffset(),
			laserMedium_direction.getYOffset(),
			laserMedium_direction.getZOffset() };
	}
	
	@Override
	public Object[] laserMediumCount() {
		return new Object[] { cache_laserMedium_count };
	}
	
	@Override
	protected WarpDriveText getEnergyStatusText() {
		final WarpDriveText text = new WarpDriveText();
		final long energy_maxStorage = cache_laserMedium_maxStorage;
		if (energy_maxStorage != 0L) {
			EnergyWrapper.formatAndAppendCharge(text, cache_laserMedium_energyStored, energy_maxStorage, null);
		}
		return text;
	}
	
	// OpenComputers callback methods
	@Callback(direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] getEnergyRequired(final Context context, final Arguments arguments) {
		OC_convertArgumentsAndLogCall(context, arguments);
		return getEnergyRequired();
	}
	
	@Callback(direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] laserMediumDirection(final Context context, final Arguments arguments) {
		OC_convertArgumentsAndLogCall(context, arguments);
		return laserMediumDirection();
	}
	
	@Callback(direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] laserMediumCount(final Context context, final Arguments arguments) {
		OC_convertArgumentsAndLogCall(context, arguments);
		return laserMediumCount();
	}
	
	// ComputerCraft IPeripheral methods
	@Override
	@Optional.Method(modid = "computercraft")
	protected Object[] CC_callMethod(@Nonnull final String methodName, @Nonnull final Object[] arguments) {
		switch (methodName) {
		case "getEnergyRequired":
			return getEnergyRequired();
			
		case "laserMediumDirection":
			return laserMediumDirection();
			
		case "laserMediumCount":
			return laserMediumCount();
		}
		
		return super.CC_callMethod(methodName, arguments);
	}
}
