package cr0s.warpdrive.block;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.EnergyWrapper;
import cr0s.warpdrive.data.Vector3;
import cr0s.warpdrive.network.PacketHandler;

import cofh.redstoneflux.api.IEnergyHandler;
import cofh.redstoneflux.api.IEnergyProvider;
import cofh.redstoneflux.api.IEnergyReceiver;

import gregtech.api.capability.GregtechCapabilities;
import gregtech.api.capability.IEnergyContainer;
import gregtech.api.util.GTUtility;
import ic2.api.energy.event.EnergyTileLoadEvent;
import ic2.api.energy.event.EnergyTileUnloadEvent;
import ic2.api.energy.tile.IEnergyAcceptor;
import ic2.api.energy.tile.IEnergyEmitter;
import ic2.api.energy.tile.IEnergySink;
import ic2.api.energy.tile.IEnergySource;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.List;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos.MutableBlockPos;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.common.Optional;


/*
    public static BigInteger LONG_MAX = BigInteger.valueOf(Long.MAX_VALUE);
    public static BigInteger LONG_MIN = BigInteger.valueOf(Long.MIN_VALUE);
    public static long castToLong(BigInteger value) {
        return value.compareTo(LONG_MAX) >= 0 ? Long.MAX_VALUE : value.compareTo(LONG_MIN) <= 0 ? Long.MIN_VALUE : value.longValue();
    }
*/

@Optional.InterfaceList({
	@Optional.Interface(iface = "cofh.redstoneflux.api.IEnergyHandler", modid = "redstoneflux"),
	@Optional.Interface(iface = "cofh.redstoneflux.api.IEnergyProvider", modid = "redstoneflux"),
	@Optional.Interface(iface = "cofh.redstoneflux.api.IEnergyReceiver", modid = "redstoneflux"),
	@Optional.Interface(iface = "ic2.api.energy.tile.IEnergySink", modid = "ic2"),
	@Optional.Interface(iface = "ic2.api.energy.tile.IEnergySource", modid = "ic2")
})
public abstract class TileEntityAbstractEnergy extends TileEntityAbstractEnergyBase implements IEnergyProvider, IEnergyReceiver, IEnergyHandler, IEnergySink, IEnergySource {
	
	// block parameters constants
	private long energyMaxStorage;
	private int IC2_sinkTier;
	private int IC2_sourceTier;
	private int FERF_fluxRateInput;
	private int FERF_fluxRateOutput;
	private int GT_voltageInput;
	private int GT_amperageInput;
	private int GT_voltageOutput;
	private int GT_amperageOutput;
	protected boolean isEnergyLostWhenBroken = true;
	
	// persistent properties
	private long energyStored_internal = 0;
	
	// computed properties
	private final IEnergyStorage[]  FE_energyStorages = new IEnergyStorage[EnumFacing.VALUES.length + 1];
	private final Object[]          GT_energyContainers = new Object[EnumFacing.VALUES.length + 1];
	private boolean                 IC2_isAddedToEnergyNet = false;
	private long                    IC2_timeAddedToEnergyNet = Long.MIN_VALUE;
	private int scanTickCount = WarpDriveConfig.ENERGY_SCAN_INTERVAL_TICKS;
	
	private final IEnergyStorage[]  FE_energyReceivers = new IEnergyStorage[EnumFacing.VALUES.length + 1];
	private final TileEntity[]      RF_energyReceivers = new TileEntity[EnumFacing.VALUES.length + 1];
	private boolean isOvervoltageLogged = false;
	
	public TileEntityAbstractEnergy() {
		super();
		
		// at base construction, we disable all input/output and allow infinite storage
		// we need to know the tier before setting things up, so we do the actual setup in onConstructed()
		energy_setParameters(Integer.MAX_VALUE,
		                     0, 0,
		                     "HV", 0, "HV", 0);
		
		// addMethods(new String[] { });
	}
	
	protected void energy_setParameters(final long energyMaxStorage,
	                                    final int fluxRateInput, final int fluxRateOutput,
	                                    final String nameTierInput, final int amperageInput,
	                                    final String nameTierOutput, final int amperageOutput) {
		this.energyMaxStorage = energyMaxStorage;
		FERF_fluxRateInput = fluxRateInput;
		FERF_fluxRateOutput = fluxRateOutput;
		IC2_sinkTier = EnergyWrapper.EU_getTierByName(nameTierInput);
		IC2_sourceTier = EnergyWrapper.EU_getTierByName(nameTierOutput);
		GT_voltageInput = 8 * (int) Math.pow(4, IC2_sinkTier);
		GT_amperageInput = amperageInput;
		GT_voltageOutput = 8 * (int) Math.pow(4, IC2_sourceTier);
		GT_amperageOutput = amperageOutput;
	}
	
	@Override
	public boolean hasCapability(@Nonnull final Capability<?> capability, @Nullable final EnumFacing facing) {
		if (energy_getMaxStorage() != 0) {
			if ( WarpDriveConfig.ENERGY_ENABLE_FE
			  && capability == CapabilityEnergy.ENERGY ) {
				return true;
			}
			
			if ( WarpDriveConfig.ENERGY_ENABLE_GTCE_EU
			  && WarpDriveConfig.isGregtechLoaded
			  && GT_isEnergyContainer(capability) ) {
				return true;
			}
		}
		return super.hasCapability(capability, facing);
	}
	
	@Optional.Method(modid = "gregtech")
	private boolean GT_isEnergyContainer(@Nonnull final Capability<?> capability) {
		return capability == GregtechCapabilities.CAPABILITY_ENERGY_CONTAINER;
	}
	
	@Nullable
	@Override
	public <T> T getCapability(@Nonnull final Capability<T> capability, @Nullable final EnumFacing facing) {
		if (energy_getMaxStorage() != 0) {
			if ( WarpDriveConfig.ENERGY_ENABLE_FE
			  && capability == CapabilityEnergy.ENERGY ) {
				IEnergyStorage energyStorage = FE_energyStorages[Commons.getOrdinal(facing)];
				if (energyStorage == null) {
					energyStorage = new IEnergyStorage() {
						
						@Override
						public int receiveEnergy(final int maxReceive, final boolean simulate) {
							return FE_receiveEnergy(facing, maxReceive, simulate);
						}
						
						@Override
						public int extractEnergy(final int maxExtract, final boolean simulate) {
							return FE_extractEnergy(facing, maxExtract, simulate);
						}
						
						@Override
						public int getEnergyStored() {
							return canExtract() || canReceive() ? EnergyWrapper.convertInternalToRF_floor(energy_getEnergyStored()) : 0;
						}
						
						@Override
						public int getMaxEnergyStored() {
							return canExtract() || canReceive() ? EnergyWrapper.convertInternalToRF_floor(energy_getMaxStorage()) : 0;
						}
						
						@Override
						public boolean canExtract() {
							return energy_canOutput(facing);
						}
						
						@Override
						public boolean canReceive() {
							return energy_canInput(facing);
						}
					};
					if (WarpDriveConfig.LOGGING_ENERGY) {
						WarpDrive.logger.info(String.format("%s IEnergyStorage(%s) capability created!",
						                                    this, facing));
					}
					FE_energyStorages[Commons.getOrdinal(facing)] = energyStorage;
				}
				return CapabilityEnergy.ENERGY.cast(energyStorage);
			}
			
			if ( WarpDriveConfig.ENERGY_ENABLE_GTCE_EU
			  && WarpDriveConfig.isGregtechLoaded
			  && GT_isEnergyContainer(capability) ) {
				return GT_getEnergyContainer(capability, facing);
			}
		}
		return super.getCapability(capability, facing);
	}
	
	@Optional.Method(modid = "gregtech")
	private <T> T GT_getEnergyContainer(@Nonnull final Capability<T> capability, @Nullable final EnumFacing facing) {
		assert capability == GregtechCapabilities.CAPABILITY_ENERGY_CONTAINER;
		
		IEnergyContainer energyContainer = (IEnergyContainer) GT_energyContainers[Commons.getOrdinal(facing)];
		if (energyContainer == null) {
			energyContainer = new IEnergyContainer() {
				
				@Override
				public long acceptEnergyFromNetwork(final EnumFacing side, final long voltage, final long amperage) {
					if (!inputsEnergy(side)) {
						return 0L;
					}
					if (voltage > getInputVoltage()) {
						if (!isOvervoltageLogged) {
							WarpDrive.logger.info(String.format("Overvoltage detected at %s input side %s: %d > %d",
							                                    this, side, voltage, getInputVoltage()));
							isOvervoltageLogged = true;
						}
						
						final int tier = GTUtility.getTierByVoltage(voltage);
						applyOvervoltageEffects(tier);
						
						return Math.min(amperage, getInputAmperage());
					}
					
					final long energyMaxToAccept_GT = EnergyWrapper.convertInternalToGT_ceil(energy_getMaxStorage() - energy_getEnergyStored());
					final long amperageMaxToAccept_GT = Math.min(energyMaxToAccept_GT / voltage, Math.min(amperage, getInputAmperage()));
					if (amperageMaxToAccept_GT <= 0) {
						return 0L;
					}
					
					energyStored_internal += EnergyWrapper.convertGTtoInternal_floor(voltage * amperageMaxToAccept_GT);
					return amperageMaxToAccept_GT;
				}
				
				@Override
				public boolean inputsEnergy(final EnumFacing side) {
					return energy_canInput(side);
				}
				
				@Override
				public boolean outputsEnergy(final EnumFacing side) {
					return energy_canOutput(side);
				}
				
				@Override
				public long changeEnergy(final long differenceAmount) {
					
					final long energyMaxToRemove_GT = EnergyWrapper.convertInternalToGT_ceil(energy_getEnergyStored());
					final long energyMaxToAccept_GT = EnergyWrapper.convertInternalToGT_ceil(energy_getMaxStorage() - energy_getEnergyStored());
					final long energyToAccept_GT = Math.max(-energyMaxToRemove_GT, Math.min(energyMaxToAccept_GT, differenceAmount));
					
					energyStored_internal += EnergyWrapper.convertGTtoInternal_floor(energyToAccept_GT);
					return energyToAccept_GT;
				}
				
				@Override
				public long getEnergyStored() {
					return outputsEnergy(facing) || inputsEnergy(facing) ? EnergyWrapper.convertInternalToGT_floor(energy_getEnergyStored()) : 0;
				}
				
				@Override
				public long getEnergyCapacity() {
					return outputsEnergy(facing) || inputsEnergy(facing) ? EnergyWrapper.convertInternalToGT_floor(energy_getMaxStorage()) : 0;
				}
				
				@Override
				public long getOutputAmperage() {
					return GT_amperageOutput;
				}
				
				@Override
				public long getOutputVoltage() {
					return GT_voltageOutput;
				}
				
				@Override
				public long getInputAmperage() {
					return GT_amperageInput;
				}
				
				@Override
				public long getInputVoltage() {
					return GT_voltageInput;
				}
				
				@Override
				public long getInputPerSec() {
					return inputsEnergy(facing) ? (long) GT_amperageInput * GT_voltageInput / 20 : 0L;
				}
				
				@Override
				public long getOutputPerSec() {
					return outputsEnergy(facing) ? (long) GT_amperageOutput * GT_voltageOutput / 20 : 0L;
				}
			};
			if (WarpDriveConfig.LOGGING_ENERGY) {
				WarpDrive.logger.info(String.format("%s IEnergyContainer capability created!",
				                                    this));
			}
			GT_energyContainers[Commons.getOrdinal(facing)] = energyContainer;
		}
		return GregtechCapabilities.CAPABILITY_ENERGY_CONTAINER.cast(energyContainer);
	}
	
	private void applyOvervoltageEffects(final int tier) {
		final int radius = 3;
		if (WarpDriveConfig.ENERGY_OVERVOLTAGE_SHOCK_FACTOR > 0) {
			// light up area with particles
			final Vector3 v3Entity = new Vector3();
			final Vector3 v3Direction = new Vector3();
			for (int count = 0; count < 3; count++) {
				v3Direction.x = 2 * (world.rand.nextDouble() - 0.5D);
				v3Direction.y = 2 * (world.rand.nextDouble() - 0.5D);
				v3Direction.z = 2 * (world.rand.nextDouble() - 0.5D);
				final double range = radius * (0.4D + 0.6D * world.rand.nextDouble());
				v3Entity.x = pos.getX() + 0.5D + range * v3Direction.x + (world.rand.nextDouble() - 0.5D);
				v3Entity.y = pos.getY() + 0.5D + range * v3Direction.y + (world.rand.nextDouble() - 0.5D);
				v3Entity.z = pos.getZ() + 0.5D + range * v3Direction.z + (world.rand.nextDouble() - 0.5D);
				v3Direction.scale(0.15D);
				PacketHandler.sendSpawnParticlePacket(world, "fireworksSpark", (byte) 1, v3Entity, v3Direction,
				                                      0.20F + 0.30F * world.rand.nextFloat(), 0.50F + 0.15F * world.rand.nextFloat(), 0.75F + 0.25F * world.rand.nextFloat(),
				                                      0.10F + 0.20F * world.rand.nextFloat(), 0.10F + 0.30F * world.rand.nextFloat(), 0.20F + 0.10F * world.rand.nextFloat(),
				                                      32);
			}
			
			// attack all entities in range
			final List<EntityLivingBase> entityLivingBases = world.getEntitiesWithinAABB(
					EntityLivingBase.class,
					new AxisAlignedBB(pos.getX() - radius, pos.getY() - radius, pos.getZ() - radius,
					                  pos.getX() + radius + 1, pos.getY() + radius + 1, pos.getZ() + radius + 1));
			for (final EntityLivingBase entityLivingBase : entityLivingBases) {
				if ( entityLivingBase.isDead
				  || !entityLivingBase.attackable()
				  || ( entityLivingBase instanceof EntityPlayer
				    && ((EntityPlayer) entityLivingBase).capabilities.disableDamage ) ) {
					continue;
				}
				
				entityLivingBase.attackEntityFrom(WarpDrive.damageShock, tier * WarpDriveConfig.ENERGY_OVERVOLTAGE_SHOCK_FACTOR);
			}
		}
		
		if (WarpDriveConfig.ENERGY_OVERVOLTAGE_EXPLOSION_FACTOR > 0) {
			world.setBlockToAir(pos);
			
			world.newExplosion(
					null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
					tier * WarpDriveConfig.ENERGY_OVERVOLTAGE_EXPLOSION_FACTOR, true, true);
		}
	}
	
	public long energy_getEnergyStored() {
		return Commons.clamp(0L, energy_getMaxStorage(), energyStored_internal);
	}
	
	// Methods to override
	
	/**
	 * Return the maximum amount of energy that can be stored (measured in internal energy units).
	 */
	public long energy_getMaxStorage() {
		return energyMaxStorage;
	}
	
	/**
	 * Return the maximum amount of energy that can be output (measured in internal energy units).
	 */
	public int energy_getPotentialOutput() {
		return 0;
	}
	
	/**
	 * Remove energy from storage, called after actual output happened (measured in internal energy units).
	 * Override this to use custom storage or measure output statistics.
	 */
	protected void energy_outputDone(final long energyOutput_internal) {
		energy_consume(energyOutput_internal);
	}
	
	/**
	 * Should return true if that direction can receive energy.
	 */
	@SuppressWarnings("UnusedParameters")
	public boolean energy_canInput(final EnumFacing from) {
		return false;
	}
	
	/**
	 * Should return true if that direction can output energy.
	 */
	@SuppressWarnings("UnusedParameters")
	public boolean energy_canOutput(final EnumFacing to) {
		return false;
	}
	
	/**
	 * Consume energy from storage for internal usage or after outputting (measured in internal energy units).
	 * Override this to use custom storage or measure energy consumption statistics (internal usage or output).
	 */
	public boolean energy_consume(final long amount_internal, final boolean simulate) {
		if (energy_getEnergyStored() >= amount_internal) {
			if (!simulate) {
				energy_consume(amount_internal);
			}
			return true;
		}
		return false;
	}
	public void energy_consume(final long amount_internal) {
		energyStored_internal -= amount_internal;
	}
	
	@Override
	public void setDebugValues() {
		super.setDebugValues();
		energyStored_internal = energyMaxStorage;
	}
	
	@Override
	protected WarpDriveText getEnergyStatusText() {
		final WarpDriveText text = new WarpDriveText();
		// skip when energy is non applicable
		final long energy_maxStorage = energy_getMaxStorage();
		if (energy_maxStorage == 0L) {
			return text;
		}
		
		// report energy level
		EnergyWrapper.formatAndAppendCharge(text, energy_getEnergyStored(), energy_maxStorage, null);
		
		// report energy tiers
		if (energy_canInput(null)) {
			EnergyWrapper.formatAndAppendInputRate(text, GT_amperageInput, GT_voltageInput, IC2_sinkTier, FERF_fluxRateInput, null);
		}
		if (energy_canOutput(null)) {
			EnergyWrapper.formatAndAppendOutputRate(text, GT_amperageOutput, GT_voltageOutput, IC2_sourceTier, FERF_fluxRateOutput, null);
		}
		
		return text;
	}
	
	// Minecraft overrides
	@Override
	protected void onConstructed() {
		super.onConstructed();
		
		// disable energy storage by default, children should call energy_setParameters() now
		energyMaxStorage = 0;
	}
	
	@Override
	protected void onFirstUpdateTick() {
		super.onFirstUpdateTick();
		
		if (world.isRemote) {
			return;
		}
		
		// RedstoneFlux and Forge energy
		if ( WarpDriveConfig.ENERGY_ENABLE_RF
		  || WarpDriveConfig.ENERGY_ENABLE_FE ) {
			FERF_scanForEnergyReceivers();
		}
	}
	
	@Override
	public void update() {
		super.update();
		
		if (world.isRemote) {
			return;
		}
		
		// IndustrialCraft2
		if ( WarpDriveConfig.ENERGY_ENABLE_IC2_EU
		  && WarpDriveConfig.isIndustrialCraft2Loaded ) {
			IC2_addToEnergyNet();
		}
		
		// RedstoneFlux & ForgeEnergy
		if ( WarpDriveConfig.ENERGY_ENABLE_FE
		  || WarpDriveConfig.ENERGY_ENABLE_RF ) {
			scanTickCount--;
			if (scanTickCount <= 0) {
				scanTickCount = WarpDriveConfig.ENERGY_SCAN_INTERVAL_TICKS;
				if (FERF_fluxRateOutput > 0) {
					FERF_scanForEnergyReceivers();
				}
			}
			
			if (FERF_fluxRateOutput > 0) {
				if ( WarpDriveConfig.ENERGY_ENABLE_RF
				  && WarpDriveConfig.isRedstoneFluxLoaded ) {
					RF_outputEnergy();
				}
				if (WarpDriveConfig.ENERGY_ENABLE_FE) {
					FE_outputEnergy();
				}
			}
		}
	}
	
	@Override
	public void onChunkUnload() {
		if (WarpDriveConfig.isIndustrialCraft2Loaded) {
			IC2_removeFromEnergyNet();
		}
		
		super.onChunkUnload();
	}
	
	@Override
	public void invalidate() {
		if (WarpDriveConfig.isIndustrialCraft2Loaded) {
			IC2_removeFromEnergyNet();
		}
		
		super.invalidate();
	}
	
	// EnergyBase override
	@Override
	public Object[] getEnergyStatus() {
		final String units = energy_getDisplayUnits();
		return new Object[] {
				EnergyWrapper.convert(energy_getEnergyStored(), units),
				EnergyWrapper.convert(energy_getMaxStorage(), units),
				units };
	}
	
	// IndustrialCraft IEnergyAcceptor interface
	@Override
	@Optional.Method(modid = "ic2")
	public boolean acceptsEnergyFrom(final IEnergyEmitter emitter, final EnumFacing from) {
		final boolean accepts = WarpDriveConfig.ENERGY_ENABLE_IC2_EU
		                     && energy_canInput(from);
		if (WarpDriveConfig.LOGGING_ENERGY) {
			WarpDrive.logger.info(String.format("%s [IC2]acceptsEnergyFrom(%s, %s) => %s",
			                                    this, emitter, from, accepts ));
		}
		return accepts;
	}
	
	// IndustrialCraft IEnergySink interface
	@Override
	@Optional.Method(modid = "ic2")
	public int getSinkTier() {
		final int tier = WarpDriveConfig.ENERGY_ENABLE_IC2_EU
		              && energy_getMaxStorage() > 0
		               ? IC2_sinkTier : 0;
		if (WarpDriveConfig.LOGGING_ENERGY) {
			WarpDrive.logger.info(String.format("%s [IC2]getSinkTier() => %d",
			                                    this, tier ));
		}
		return tier;
	}
	
	@Override
	@Optional.Method(modid = "ic2")
	public double getDemandedEnergy() {
		final double demanded_EU = Math.max(0.0D, EnergyWrapper.convertInternalToEU_floor(energy_getMaxStorage() - energy_getEnergyStored()));
		if (WarpDriveConfig.LOGGING_ENERGY) {
			WarpDrive.logger.info(String.format("%s [IC2]getDemandedEnergy() => %.2f EU",
			                                    this, demanded_EU ));
		}
		return demanded_EU;
	}
	
	@Override
	@Optional.Method(modid = "ic2")
	public double injectEnergy(final EnumFacing from, final double amount_EU, final double voltage) {
		double amountLeftOver_EU = amount_EU;
		if ( WarpDriveConfig.ENERGY_ENABLE_IC2_EU
		  && energy_canInput(from.getOpposite()) ) {
			long leftover_internal = 0;
			energyStored_internal += EnergyWrapper.convertEUtoInternal_floor(amount_EU);
			
			if (energyStored_internal > energy_getMaxStorage()) {
				leftover_internal = (energyStored_internal - energy_getMaxStorage());
				energyStored_internal = energy_getMaxStorage();
			}
			
			amountLeftOver_EU = EnergyWrapper.convertInternalToEU_floor(leftover_internal);
		}
		if (WarpDriveConfig.LOGGING_ENERGY) {
			WarpDrive.logger.info(String.format("%s [IC2]injectEnergy(%s, %.2f EU, %.1f) => %.2f EU",
			                                    this, from, amount_EU, voltage, amountLeftOver_EU ));
		}
		return amountLeftOver_EU;
	}
	
	// IndustrialCraft IEnergyEmitter interface
	@Override
	@Optional.Method(modid = "ic2")
	public boolean emitsEnergyTo(final IEnergyAcceptor receiver, final EnumFacing to) {
		final boolean emits = WarpDriveConfig.ENERGY_ENABLE_IC2_EU
		                   && energy_canOutput(to);
		if (WarpDriveConfig.LOGGING_ENERGY) {
			WarpDrive.logger.info(String.format("%s [IC2]emitsEnergyTo(%s, %s) => %s",
			                                    this, receiver, to, emits ));
		}
		return emits;
	}
	
	// IndustrialCraft IEnergySource interface
	@Override
	@Optional.Method(modid = "ic2")
	public int getSourceTier() {
		final int tier = WarpDriveConfig.ENERGY_ENABLE_IC2_EU
		              && energy_getMaxStorage() > 0
		               ? IC2_sourceTier : 0;
		if (WarpDriveConfig.LOGGING_ENERGY) {
			WarpDrive.logger.info(String.format("%s [IC2]getSourceTier() => %d",
			                                    this, tier ));
		}
		// IC2 caps at tier 4 (EV), cables burn & block explodes after that, so we might as well enforce it
		return Math.min(4, tier);
	}
	
	@Override
	@Optional.Method(modid = "ic2")
	public double getOfferedEnergy() {
		double offered_EU = 0.0D;
		if (WarpDriveConfig.ENERGY_ENABLE_IC2_EU) {
			offered_EU = EnergyWrapper.convertInternalToEU_floor(energy_getPotentialOutput());
		}
		if (WarpDriveConfig.LOGGING_ENERGY) {
			WarpDrive.logger.info(String.format("%s [IC2]getOfferedEnergy() => %.2f EU",
			                                    this, offered_EU ));
		}
		return offered_EU;
	}
	
	@Override
	@Optional.Method(modid = "ic2")
	public void drawEnergy(final double amount_EU) {
		if (WarpDriveConfig.LOGGING_ENERGY) {
			WarpDrive.logger.info(String.format("%s [IC2]drawEnergy(%.2f EU)",
			                                    this, amount_EU));
		}
		energy_outputDone(EnergyWrapper.convertEUtoInternal_ceil(amount_EU));
	}
	
	// IndustrialCraft compatibility methods
	@Optional.Method(modid = "ic2")
	private void IC2_addToEnergyNet() {
		if ( !world.isRemote
		  && !IC2_isAddedToEnergyNet ) {
			IC2_timeAddedToEnergyNet = world.getTotalWorldTime();
			MinecraftForge.EVENT_BUS.post(new EnergyTileLoadEvent(this));
			IC2_isAddedToEnergyNet = true;
		}
	}
	
	@Optional.Method(modid = "ic2")
	private void IC2_removeFromEnergyNet() {
		if ( !world.isRemote
		  && IC2_isAddedToEnergyNet ) {
			MinecraftForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this));
			IC2_isAddedToEnergyNet = false;
		}
	}
	
	// RedstoneFlux IEnergyReceiver interface
	@Override
	@Optional.Method(modid = "redstoneflux")
	public int receiveEnergy(final EnumFacing from, final int maxReceive_RF, final boolean simulate) {
		if (WarpDriveConfig.LOGGING_ENERGY) {
			WarpDrive.logger.info(String.format("%s [RF]receiveEnergy(%s, %d, %s)",
			                                    this, from, maxReceive_RF, simulate));
		}
		return FERF_receiveEnergy(from, maxReceive_RF, simulate);
	}
	private int FE_receiveEnergy(final EnumFacing from, final int maxReceive_RF, final boolean simulate) {
		if (WarpDriveConfig.LOGGING_ENERGY) {
			WarpDrive.logger.info(String.format("%s FE_receiveEnergy(%s, %d, %s)",
			                                    this, from, maxReceive_RF, simulate));
		}
		return FERF_receiveEnergy(from, maxReceive_RF, simulate);
	}
	private int FERF_receiveEnergy(final EnumFacing from, final int maxReceive_RF, final boolean simulate) {
		if (!energy_canInput(from)) {
			return 0;
		}
		
		final long energyMaxStored_internal = energy_getMaxStorage();
		if (energyMaxStored_internal == 0L) {
			return 0;
		}
		final long energyStored_internal = energy_getEnergyStored();
		final int energyMaxToAdd_RF = EnergyWrapper.convertInternalToRF_ceil(energyMaxStored_internal - energyStored_internal);
		
		final int energyToAdd_RF = Math.min(maxReceive_RF, energyMaxToAdd_RF);
		if (WarpDriveConfig.LOGGING_ENERGY) {
			final int energyMaxStored_RF = EnergyWrapper.convertInternalToRF_floor(energyMaxStored_internal);
			final int energyStored_RF = EnergyWrapper.convertInternalToRF_floor(energyStored_internal);
			WarpDrive.logger.info(String.format("%s FERF_receiveEnergy(%s, %d, %s) adding %s to %d / %s RF",
			                                    this, from, maxReceive_RF, simulate, energyToAdd_RF, energyStored_RF, energyMaxStored_RF));
		}
		if (!simulate) {
			this.energyStored_internal += EnergyWrapper.convertRFtoInternal_floor(energyToAdd_RF);
		}
		
		return energyToAdd_RF;
	}
	
	// RedstoneFlux IEnergyProvider interface
	@Override
	@Optional.Method(modid = "redstoneflux")
	public int extractEnergy(final EnumFacing from, final int maxExtract_RF, final boolean simulate) {
		if (WarpDriveConfig.LOGGING_ENERGY) {
			WarpDrive.logger.info(String.format("%s [RF]extractEnergy(%s, %d, %s)",
			                                    this, from, maxExtract_RF, simulate));
		}
		return FERF_extractEnergy(from, maxExtract_RF, simulate);
	}
	private int FE_extractEnergy(final EnumFacing from, final int maxExtract_RF, final boolean simulate) {
		if (WarpDriveConfig.LOGGING_ENERGY) {
			WarpDrive.logger.info(String.format("%s FE_extractEnergy(%s, %d, %s)",
			                                    this, from, maxExtract_RF, simulate));
		}
		return FERF_extractEnergy(from, maxExtract_RF, simulate);
	}
	private int FERF_extractEnergy(final EnumFacing from, final int maxExtract_RF, final boolean simulate) {
		if (!energy_canOutput(from)) {
			return 0;
		}
		
		final long potentialEnergyOutput_internal = energy_getPotentialOutput();
		final long energyExtracted_internal = Math.min(EnergyWrapper.convertRFtoInternal_ceil(maxExtract_RF), potentialEnergyOutput_internal);
		if (!simulate) {
			energy_outputDone(energyExtracted_internal);
		}
		return EnergyWrapper.convertInternalToRF_floor(energyExtracted_internal);
	}
	
	// RedstoneFlux IEnergyConnection interface
	@Override
	@Optional.Method(modid = "redstoneflux")
	public boolean canConnectEnergy(final EnumFacing from) {
		// note: getMaxEnergyStored() depends on this method so we need to rely only on our internal methods
		return WarpDriveConfig.ENERGY_ENABLE_RF
		    && energy_getMaxStorage() != 0
		    && ( energy_canInput(from)
		      || energy_canOutput(from) );
	}
	
	// RedstoneFlux IEnergyHandler interface
	@Override
	@Optional.Method(modid = "redstoneflux")
	public int getEnergyStored(final EnumFacing from) {
		return canConnectEnergy(from) ? EnergyWrapper.convertInternalToRF_floor(energy_getEnergyStored()) : 0;
	}
	
	@Override
	@Optional.Method(modid = "redstoneflux")
	public int getMaxEnergyStored(final EnumFacing from) {
		return canConnectEnergy(from) ? EnergyWrapper.convertInternalToRF_floor(energy_getMaxStorage()) : 0;
	}
	
	
	// WarpDrive overrides for CoFH RedstoneFlux and Forge energy
	@Optional.Method(modid = "redstoneflux")
	private void RF_outputEnergy(final EnumFacing to, @Nonnull final IEnergyReceiver energyReceiver) {
		if (!energy_canOutput(to)) {
			return;
		}
		final long potentialEnergyOutput_internal = energy_getPotentialOutput();
		if (potentialEnergyOutput_internal > 0) {
			final int potentialEnergyOutput_RF = EnergyWrapper.convertInternalToRF_floor(potentialEnergyOutput_internal);
			final int energyToOutput_RF = energyReceiver.receiveEnergy(to.getOpposite(), potentialEnergyOutput_RF, true);
			if (energyToOutput_RF > 0) {
				final int energyOutputted_RF = energyReceiver.receiveEnergy(to.getOpposite(), energyToOutput_RF, false);
				energy_outputDone(EnergyWrapper.convertRFtoInternal_ceil(energyOutputted_RF));
			}
		}
	}
	private void FE_outputEnergy(final EnumFacing to, @Nonnull final IEnergyStorage energyStorage) {
		if (!energy_canOutput(to)) {
			return;
		}
		final long potentialEnergyOutput_internal = energy_getPotentialOutput();
		if (potentialEnergyOutput_internal > 0) {
			final int potentialEnergyOutput_RF = EnergyWrapper.convertInternalToRF_floor(potentialEnergyOutput_internal);
			final int energyToOutput_RF = energyStorage.receiveEnergy(potentialEnergyOutput_RF, true);
			if (energyToOutput_RF > 0) {
				final int energyOutputted_RF = energyStorage.receiveEnergy(energyToOutput_RF, false);
				energy_outputDone(EnergyWrapper.convertRFtoInternal_ceil(energyOutputted_RF));
			}
		}
	}
	
	
	@Optional.Method(modid = "redstoneflux")
	private void RF_outputEnergy() {
		for (final EnumFacing to : EnumFacing.VALUES) {
			final TileEntity tileEntity = RF_energyReceivers[Commons.getOrdinal(to)];
			if ( tileEntity instanceof IEnergyReceiver
			  && !tileEntity.isInvalid() ) {
				RF_outputEnergy(to, (IEnergyReceiver) tileEntity);
			}
		}
	}
	private void FE_outputEnergy() {
		for (final EnumFacing to : EnumFacing.VALUES) {
			final IEnergyStorage energyStorage = FE_energyReceivers[Commons.getOrdinal(to)];
			if (energyStorage != null) {
				FE_outputEnergy(to, energyStorage);
			}
		}
	}
	
	
	@Optional.Method(modid = "redstoneflux")
	private boolean RF_addEnergyReceiver(@Nonnull final EnumFacing to, final TileEntity tileEntity) {
		if ( tileEntity instanceof IEnergyReceiver
		  && !tileEntity.isInvalid() ) {
			final IEnergyReceiver energyReceiver = (IEnergyReceiver) tileEntity;
			if (energyReceiver.canConnectEnergy(to.getOpposite())) {
				if (RF_energyReceivers[Commons.getOrdinal(to)] != tileEntity) {
					RF_energyReceivers[Commons.getOrdinal(to)] = tileEntity;
				}
				return true;
			}
		}
		RF_energyReceivers[Commons.getOrdinal(to)] = null;
		return false;
	}
	
	private boolean FE_addEnergyReceiver(@Nonnull final EnumFacing to, final TileEntity tileEntity) {
		if (tileEntity != null) {
			final IEnergyStorage energyStorage = tileEntity.getCapability(CapabilityEnergy.ENERGY, to.getOpposite());
			if (energyStorage != null) {
				if (energyStorage.canReceive()) {
					if (FE_energyReceivers[Commons.getOrdinal(to)] != energyStorage) {
						FE_energyReceivers[Commons.getOrdinal(to)] = energyStorage;
					}
					return true;
				}
			}
		}
		FE_energyReceivers[Commons.getOrdinal(to)] = null;
		return false;
	}
	
	private void FERF_scanForEnergyReceivers() {
		if (WarpDriveConfig.LOGGING_ENERGY) {
			WarpDrive.logger.info(String.format("%s FERF_scanForEnergyReceivers()",
			                                    this));
		}
		final MutableBlockPos mutableBlockPos = new MutableBlockPos(pos);
		for (final EnumFacing to : EnumFacing.VALUES) {
			if (energy_canOutput(to)) {
				mutableBlockPos.setPos(
						pos.getX() + to.getXOffset(),
						pos.getY() + to.getYOffset(),
						pos.getZ() + to.getZOffset() );
				final TileEntity tileEntity = world.getTileEntity(mutableBlockPos);
				
				if ( WarpDriveConfig.ENERGY_ENABLE_RF
				  && WarpDriveConfig.isRedstoneFluxLoaded ) {
					if (RF_addEnergyReceiver(to, tileEntity)) {
						continue;
					}
				}
				
				if (WarpDriveConfig.ENERGY_ENABLE_FE) {
					if (FE_addEnergyReceiver(to, tileEntity)) {
						continue;
					}
				}
				
			}
		}
	}
	
	
	// Forge overrides
	@Override
	public void readFromNBT(@Nonnull final NBTTagCompound tagCompound) {
		super.readFromNBT(tagCompound);
		energyStored_internal = tagCompound.getLong(EnergyWrapper.TAG_ENERGY);
	}
	
	@Nonnull
	@Override
	public NBTTagCompound writeToNBT(@Nonnull NBTTagCompound tagCompound) {
		tagCompound = super.writeToNBT(tagCompound);
		
		tagCompound.setLong(EnergyWrapper.TAG_ENERGY, energyStored_internal);
		return tagCompound;
	}
	
	@Override
	public NBTTagCompound writeItemDropNBT(NBTTagCompound tagCompound) {
		tagCompound = super.writeItemDropNBT(tagCompound);
		
		if (isEnergyLostWhenBroken) {
			tagCompound.removeTag(EnergyWrapper.TAG_ENERGY);
		}
		return tagCompound;
	}
	
	// WarpDrive overrides
	protected void energy_refreshConnections() {
		if (WarpDriveConfig.isIndustrialCraft2Loaded) {
			// IC2 EnergyNet throws a block update during the next world tick following its EnergyTileLoadEvent
			// Ignoring this specific block update prevents us to do a constant refresh of the energy net
			if (world.getTotalWorldTime() - IC2_timeAddedToEnergyNet > 1) {
				IC2_removeFromEnergyNet();
			}
		}
		scanTickCount = -1;
	}
	
	@Override
	public void onEMP(final float efficiency) {
		if (energy_getMaxStorage() > 0) {
			energy_consume(Math.round(energy_getEnergyStored() * efficiency), false);
		}
	}
}