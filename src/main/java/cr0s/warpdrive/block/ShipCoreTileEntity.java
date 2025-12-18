package cr0s.warpdrive.block;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.data.Registration;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Ship Core TileEntity - Handles ship logic and ComputerCraft integration
 *
 * Implements:
 * - ITickableTileEntity for periodic updates
 * - IEnergyStorage for Forge Energy
 * - Provides IPeripheral via capability for CC:Tweaked
 */
public class ShipCoreTileEntity extends TileEntity implements ITickableTileEntity {

	// Energy storage
	private int energyStored = 0;
	private static final int MAX_ENERGY = 10_000_000; // 10M FE
	private static final int MAX_TRANSFER = 10_000;    // 10K FE/t

	// Ship data
	private int shipBlocks = 0;
	private boolean shipScanned = false;

	// Warp destination
	private int destX = 0;
	private int destY = 0;
	private int destZ = 0;
	private String destDimension = "minecraft:overworld";

	public ShipCoreTileEntity() {
		super(Registration.SHIP_CORE_TILE.get());
	}

	@Override
	public void tick() {
		// Periodic updates can go here
		// For now, just a placeholder
	}

	// ===== ComputerCraft Lua API Methods =====
	// Using @LuaFunction annotation (CC:Tweaked 1.16.5 pattern)

	@LuaFunction
	public final Object[] scan() {
		// TODO: Implement actual ship scanning in Phase 2
		// For now, just a placeholder that scans nearby blocks
		shipBlocks = 100; // Fake scan result
		shipScanned = true;

		WarpDrive.logger.info("Ship scan initiated at {}", getBlockPos());

		return new Object[]{
			true,        // success
			shipBlocks,  // block count
			"Ship scanned successfully"
		};
	}

	@LuaFunction
	public final int getEnergyStored() {
		return energyStored;
	}

	@LuaFunction
	public final int getEnergyRequired() {
		return calculateEnergyRequired();
	}

	@LuaFunction
	public final int getShipSize() {
		return shipBlocks;
	}

	private int calculateEnergyRequired() {
		if (!shipScanned || level == null) {
			return 0;
		}

		// Calculate distance
		int dx = destX - getBlockPos().getX();
		int dy = destY - getBlockPos().getY();
		int dz = destZ - getBlockPos().getZ();
		double distance = Math.sqrt(dx*dx + dy*dy + dz*dz);

		// Energy formula: blocks * 100 + distance * 10
		int baseCost = shipBlocks * 100;
		int distanceCost = (int)(distance * 10);

		// TODO: Add dimension change penalty in Phase 5

		return baseCost + distanceCost;
	}

	@LuaFunction
	public final Object[] setDestination(int x, int y, int z, String dimension) throws LuaException {
		destX = x;
		destY = y;
		destZ = z;
		destDimension = dimension;

		WarpDrive.logger.info("Destination set to: {}, {}, {} in {}",
			destX, destY, destZ, destDimension);

		return new Object[]{ true, "Destination set" };
	}

	@LuaFunction
	public final Object[] jump() {
		if (!shipScanned) {
			return new Object[]{ false, "Ship not scanned" };
		}

		int required = calculateEnergyRequired();
		if (energyStored < required) {
			return new Object[]{
				false,
				String.format("Insufficient energy: %d/%d FE", energyStored, required)
			};
		}

		// TODO: Implement actual warp logic in Phase 3
		WarpDrive.logger.info("Warp jump initiated from {} to {}, {}, {}",
			getBlockPos(), destX, destY, destZ);

		// For now, just consume energy and report success
		energyStored -= required;
		setChanged();

		return new Object[]{
			true,
			String.format("Warp successful! Used %d FE", required)
		};
	}

	// ===== Energy Capability =====

	private final LazyOptional<IEnergyStorage> energyHandler = LazyOptional.of(() -> new IEnergyStorage() {
		@Override
		public int receiveEnergy(int maxReceive, boolean simulate) {
			int received = Math.min(MAX_ENERGY - energyStored, Math.min(maxReceive, MAX_TRANSFER));
			if (!simulate) {
				energyStored += received;
				setChanged();
			}
			return received;
		}

		@Override
		public int extractEnergy(int maxExtract, boolean simulate) {
			return 0; // Ship core doesn't provide energy
		}

		@Override
		public int getEnergyStored() {
			return energyStored;
		}

		@Override
		public int getMaxEnergyStored() {
			return MAX_ENERGY;
		}

		@Override
		public boolean canExtract() {
			return false;
		}

		@Override
		public boolean canReceive() {
			return true;
		}
	});

	@Nonnull
	@Override
	public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
		if (cap == CapabilityEnergy.ENERGY) {
			return energyHandler.cast();
		}
		return super.getCapability(cap, side);
	}

	@Override
	protected void invalidateCaps() {
		super.invalidateCaps();
		energyHandler.invalidate();
	}

	// ===== NBT Serialization =====

	@Override
	public void load(@Nonnull BlockState state, @Nonnull CompoundNBT nbt) {
		super.load(state, nbt);
		energyStored = nbt.getInt("Energy");
		shipBlocks = nbt.getInt("ShipBlocks");
		shipScanned = nbt.getBoolean("ShipScanned");
		destX = nbt.getInt("DestX");
		destY = nbt.getInt("DestY");
		destZ = nbt.getInt("DestZ");
		destDimension = nbt.getString("DestDimension");
	}

	@Nonnull
	@Override
	public CompoundNBT save(@Nonnull CompoundNBT nbt) {
		super.save(nbt);
		nbt.putInt("Energy", energyStored);
		nbt.putInt("ShipBlocks", shipBlocks);
		nbt.putBoolean("ShipScanned", shipScanned);
		nbt.putInt("DestX", destX);
		nbt.putInt("DestY", destY);
		nbt.putInt("DestZ", destZ);
		nbt.putString("DestDimension", destDimension);
		return nbt;
	}
}
