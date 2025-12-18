package cr0s.warpdrive.block;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.config.WarpDriveConfig;

import javax.annotation.Nonnull;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;

public class TileEntityLaserMedium extends TileEntityAbstractEnergy {
	
	private static final int BLOCKSTATE_REFRESH_PERIOD_TICKS = 20;
	
	// persistent properties
	// (none)
	
	// computed properties
	private int ticks = BLOCKSTATE_REFRESH_PERIOD_TICKS;
	
	public TileEntityLaserMedium() {
		super();
		
		peripheralName = "warpdriveLaserMedium";
		doRequireUpgradeToInterface();
	}
	
	@Override
	protected void onConstructed() {
		super.onConstructed();
		
		energy_setParameters(WarpDriveConfig.LASER_MEDIUM_MAX_ENERGY_STORED_BY_TIER[enumTier.getIndex()],
		                     4096, 0,
		                     "HV", 2, "HV", 0);
	}
	
	@Override
	public void update() {
		super.update();
		
		if (world.isRemote) {
			return;
		}
		
		ticks--;
		if (ticks < 0) {
			ticks = BLOCKSTATE_REFRESH_PERIOD_TICKS;
			
			final int level = Commons.clamp(0, 7, (int) Math.round(8.0D * (energy_getEnergyStored() / (double) energy_getMaxStorage())));
			updateBlockState(null, BlockLaserMedium.LEVEL, level);
		}
	}
	
	@Override
	public void readFromNBT(@Nonnull final NBTTagCompound tagCompound) {
		super.readFromNBT(tagCompound);
	}
	
	@Nonnull
	@Override
	public NBTTagCompound writeToNBT(@Nonnull final NBTTagCompound tagCompound) {
		return super.writeToNBT(tagCompound);
	}
	
	// IEnergySink methods
	@Override
	public boolean energy_canInput(final EnumFacing from) {
		return true;
	}
	
	// WarpDrive overrides
	@Override
	public void onEMP(final float efficiency) {
		// no effect
	}
}
