package cr0s.warpdrive.data;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.config.WarpDriveConfig;

import javax.annotation.Nonnull;

import cofh.redstoneflux.api.IEnergyContainerItem;
import gregtech.api.capability.GregtechCapabilities;
import ic2.api.item.ElectricItem;
import ic2.api.item.IElectricItem;
import ic2.api.item.IElectricItemManager;
import ic2.api.item.ISpecialElectricItem;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.common.Optional;

public class EnergyWrapper {
	
	// constants
	public static final String TAG_DISPLAY_UNITS = "displayUnits";
	public static final String TAG_ENERGY = "energy";
	public static final double EU_PER_INTERNAL = 1.0D;
	public static final double GT_PER_INTERNAL = 1.0D;
	public static final double RF_PER_INTERNAL = 1800.0D / 437.5D;
	// public static final int IC2_sinkTier_max = Integer.MAX_VALUE;
	// public static final int IC2_sourceTier_max = 20;
	//                                             0     1     2     3     4     5      6      7      8      9
	public static final String[] EU_nameTier = { "ULV", "LV", "MV", "HV", "EV", "IV", "LuV", "ZPMV", "UV", "MaxV" };
	
	public static int EU_getTierByName(@Nonnull final String nameTier) {
		for (int tier = 0; tier < EU_nameTier.length; tier++) {
			if (nameTier.equals(EU_nameTier[tier])) {
				return tier;
			}
		}
		return -1;
	}
	
	// conversion handling
	public static int convertInternalToRF_ceil(final long energy) {
		return (int) Math.ceil(energy * RF_PER_INTERNAL);
	}
	
	public static int convertInternalToRF_floor(final long energy) {
		return (int) Math.floor(energy * RF_PER_INTERNAL);
	}
	
	public static long convertRFtoInternal_ceil(final int energy) {
		return (long) Math.ceil(energy / RF_PER_INTERNAL);
	}
	
	public static long convertRFtoInternal_floor(final int energy) {
		return (long) Math.floor(energy / RF_PER_INTERNAL);
	}
	
	public static long convertInternalToGT_ceil(final long energy) {
		return (long) Math.ceil(energy * GT_PER_INTERNAL);
	}
	
	public static long convertInternalToGT_floor(final long energy) {
		return (long) Math.floor(energy * GT_PER_INTERNAL);
	}
	
	public static long convertGTtoInternal_ceil(final double amount) {
		return (long) Math.ceil(amount / GT_PER_INTERNAL);
	}
	
	public static long convertGTtoInternal_floor(final double amount) {
		return (long) Math.floor(amount / GT_PER_INTERNAL);
	}
	
	public static double convertInternalToEU_ceil(final long energy) {
		return Math.ceil(energy * EU_PER_INTERNAL);
	}
	
	public static double convertInternalToEU_floor(final long energy) {
		return Math.floor(energy * EU_PER_INTERNAL);
	}
	
	public static long convertEUtoInternal_ceil(final double amount) {
		return (long) Math.ceil(amount / EU_PER_INTERNAL);
	}
	
	public static long convertEUtoInternal_floor(final double amount) {
		return (long) Math.floor(amount / EU_PER_INTERNAL);
	}
	
	public static String format(final long energy, final String units) {
		return Commons.format(convert(energy, units));
	}
	
	public static void formatAndAppendCharge(@Nonnull final WarpDriveText warpDriveText,
	                                         final long energyStored, final long maxStorage, final String units) {
		final String unitsToUse = units == null ? WarpDriveConfig.ENERGY_DISPLAY_UNITS : units;
		final String energyStored_units = EnergyWrapper.format(energyStored, unitsToUse);
		final String energyMaxStorage_units = EnergyWrapper.format(maxStorage, unitsToUse);
		final WarpDriveText textRate = new WarpDriveText(null, "warpdrive.energy.status_line.charge")
				                               .appendInLine(null, " ")
				                               .appendInLine(Commons.getStyleValue(), energyStored_units)
				                               .appendInLine(null, " / ")
				                               .appendInLine(Commons.getStyleValue(), energyMaxStorage_units)
				                               .appendInLine(null, String.format(" %s.", unitsToUse));
		warpDriveText.append(textRate);
	}
	
	public static void formatAndAppendInputRate(@Nonnull final WarpDriveText warpDriveText,
	                                            final long amperage, final long voltage, final int tier, final long fluxRate, final String units) {
		formatAndAppendRate(warpDriveText, "warpdrive.energy.status_line.input_rate",
		                    amperage, voltage, tier, fluxRate, units);
	}
	
	public static void formatAndAppendOutputRate(@Nonnull final WarpDriveText warpDriveText,
	                                             final long amperage, final long voltage, final int tier, final long fluxRate, final String units) {
		formatAndAppendRate(warpDriveText, "warpdrive.energy.status_line.output_rate",
		                    amperage, voltage, tier, fluxRate, units);
	}
	
	public static long convert(final long energy, final String units) {
		final String unitsToUse = units == null ? WarpDriveConfig.ENERGY_DISPLAY_UNITS : units;
		switch (unitsToUse) {
		case "EU":
			return (long) EnergyWrapper.convertInternalToEU_floor(energy);
		
		case "RF":
		case "FE":
		case "\u0230I":// micro in ASCII code
		case "\u00B5I":// micro in UNICODE
			return (long) EnergyWrapper.convertInternalToRF_floor(energy);
		
		default:
			return energy;
		}
	}
	
	private static void formatAndAppendRate(@Nonnull final WarpDriveText warpDriveText, @Nonnull final String translationKey,
	                                        final long amperage, final long voltage, final int tier, final long fluxRate, final String units) {
		final String unitsToUse = units == null ? WarpDriveConfig.ENERGY_DISPLAY_UNITS : units;
		switch (unitsToUse) {
		case "EU":
			formatAndAppendRate(warpDriveText, translationKey, amperage, voltage, tier, unitsToUse);
			break;
			
		case "RF":
		case "FE":
		case "\u0230I":// micro in ASCII code
		case "\u00B5I":// micro in UNICODE
			formatAndAppendRate(warpDriveText, translationKey, 6, fluxRate, tier, unitsToUse);
			break;
			
		default:
			break;
		}
	}
	
	private static void formatAndAppendRate(@Nonnull final WarpDriveText warpDriveText, @Nonnull final String translationKey,
	                                        final long amperage, final long voltage, final int tier, @Nonnull final String units) {
		final WarpDriveText textRate = new WarpDriveText(null, translationKey)
				                               .appendInLine(null, " ")
				                               .appendInLine(Commons.getStyleValue(), amperage)
				                               .appendInLine(null, " x ")
				                               .appendInLine(Commons.getStyleVoltage(), voltage)
				                               .appendInLine(null, String.format(" %s/t (", units))
				                               .appendInLine(Commons.getStyleVoltage(), EnergyWrapper.EU_nameTier[tier])
				                               .appendInLine(null, ").");
		warpDriveText.append(textRate);
	}
	
	// WarpDrive methods
	public static boolean isEnergyContainer(final ItemStack itemStack) {
		boolean bResult = false;
		
		// IndustrialCraft2
		if ( WarpDriveConfig.ENERGY_ENABLE_IC2_EU
		  && WarpDriveConfig.isIndustrialCraft2Loaded ) {
			bResult = IC2_isEnergyContainer(itemStack);
		}
		
		// Gregtech
		if ( !bResult
		  && WarpDriveConfig.ENERGY_ENABLE_GTCE_EU
		  && WarpDriveConfig.isGregtechLoaded ) {
			bResult = GT_isEnergyContainer(itemStack);
		}
		
		// RedstoneFlux
		if ( !bResult
		  && WarpDriveConfig.ENERGY_ENABLE_RF
		  && WarpDriveConfig.isRedstoneFluxLoaded ) {
			bResult = RF_isEnergyContainer(itemStack);
		}
		
		// Forge Energy
		if ( !bResult
		  && WarpDriveConfig.ENERGY_ENABLE_FE ) {
			bResult = FE_isEnergyContainer(itemStack);
		}
		
		return bResult;
	}
	
	public static boolean canInput(final ItemStack itemStack) {
		boolean bResult = false;
		
		// IndustrialCraft2
		if ( WarpDriveConfig.ENERGY_ENABLE_IC2_EU
		  && WarpDriveConfig.isIndustrialCraft2Loaded
		  && IC2_isEnergyContainer(itemStack) ) {
			bResult = IC2_canInput(itemStack);
		}
		
		// Gregtech
		if ( WarpDriveConfig.ENERGY_ENABLE_GTCE_EU
		  && WarpDriveConfig.isGregtechLoaded
		  && GT_isEnergyContainer(itemStack) ) {
			bResult = GT_canInput(itemStack);
		}
		
		// RedstoneFlux
		if ( !bResult
		  && WarpDriveConfig.ENERGY_ENABLE_RF
		  && WarpDriveConfig.isRedstoneFluxLoaded
		  && RF_isEnergyContainer(itemStack) ) {
			bResult = RF_canInput(itemStack);
		}
		
		// Forge Energy
		if ( !bResult
		  && WarpDriveConfig.ENERGY_ENABLE_FE
		  && FE_isEnergyContainer(itemStack) ) {
			bResult = FE_canInput(itemStack);
		}
		
		return bResult;
	}
	
	public static boolean canOutput(final ItemStack itemStack) {
		boolean bResult = false;
		
		// IndustrialCraft2
		if ( WarpDriveConfig.ENERGY_ENABLE_IC2_EU
		  && WarpDriveConfig.isIndustrialCraft2Loaded
		  && IC2_isEnergyContainer(itemStack) ) {
			bResult = IC2_canOutput(itemStack);
		}
		
		// Gregtech
		if ( WarpDriveConfig.ENERGY_ENABLE_GTCE_EU
		  && WarpDriveConfig.isGregtechLoaded
		  && GT_isEnergyContainer(itemStack) ) {
			bResult = GT_canOutput(itemStack);
		}
		
		// RedstoneFlux
		if ( !bResult
		  && WarpDriveConfig.ENERGY_ENABLE_RF
		  && WarpDriveConfig.isRedstoneFluxLoaded
		  && RF_isEnergyContainer(itemStack) ) {
			bResult = RF_canOutput(itemStack);
		}
		
		// Forge Energy
		if ( !bResult
		  && WarpDriveConfig.ENERGY_ENABLE_FE
		  && FE_isEnergyContainer(itemStack) ) {
			bResult = FE_canOutput(itemStack);
		}
		
		return bResult;
	}
	
	public static long getEnergyStored(final ItemStack itemStack) {
		// IndustrialCraft2
		if ( WarpDriveConfig.ENERGY_ENABLE_IC2_EU
		  && WarpDriveConfig.isIndustrialCraft2Loaded
		  && IC2_isEnergyContainer(itemStack) ) {
			final double amount_EU = Commons.clamp(0, IC2_getMaxEnergyStorage(itemStack), IC2_getEnergyStored(itemStack));
			return convertEUtoInternal_floor(amount_EU);
		}
		
		// Gregtech
		if ( WarpDriveConfig.ENERGY_ENABLE_GTCE_EU
		  && WarpDriveConfig.isGregtechLoaded
		  && GT_isEnergyContainer(itemStack) ) {
			final long amount_EU = Commons.clamp(0L, GT_getMaxEnergyStorage(itemStack), GT_getEnergyStored(itemStack));
			return convertGTtoInternal_floor(amount_EU);
		}
		
		// RedstoneFlux
		if ( WarpDriveConfig.ENERGY_ENABLE_RF
		  && WarpDriveConfig.isRedstoneFluxLoaded
		  && RF_isEnergyContainer(itemStack) ) {
			final int amount_RF = Commons.clamp(0, RF_getMaxEnergyStorage(itemStack), RF_getEnergyStored(itemStack));
			return convertRFtoInternal_floor(amount_RF);
		}
		
		// Forge Energy
		if ( WarpDriveConfig.ENERGY_ENABLE_FE
		  && WarpDriveConfig.isRedstoneFluxLoaded
		  && FE_isEnergyContainer(itemStack) ) {
			final int amount_RF = Commons.clamp(0, FE_getMaxEnergyStorage(itemStack), FE_getEnergyStored(itemStack));
			return convertRFtoInternal_floor(amount_RF);
		}
		
		return 0;
	}
	
	public static long getMaxEnergyStorage(final ItemStack itemStack) {
		// IndustrialCraft2
		if ( WarpDriveConfig.ENERGY_ENABLE_IC2_EU
		  && WarpDriveConfig.isIndustrialCraft2Loaded
		  && IC2_isEnergyContainer(itemStack) ) {
			final double amount_EU = IC2_getMaxEnergyStorage(itemStack);
			return convertEUtoInternal_floor(amount_EU);
		}
		
		// Gregtech
		if ( WarpDriveConfig.ENERGY_ENABLE_GTCE_EU
		  && WarpDriveConfig.isGregtechLoaded
		  && GT_isEnergyContainer(itemStack) ) {
			final long amount_EU = GT_getMaxEnergyStorage(itemStack);
			return convertGTtoInternal_floor(amount_EU);
		}
		
		// RedstoneFlux
		if ( WarpDriveConfig.ENERGY_ENABLE_RF
		  && WarpDriveConfig.isRedstoneFluxLoaded
		  && RF_isEnergyContainer(itemStack) ) {
			final int amount_RF = RF_getMaxEnergyStorage(itemStack);
			return convertRFtoInternal_floor(amount_RF);
		}
		
		// Forge Energy
		if ( WarpDriveConfig.ENERGY_ENABLE_FE
		  && FE_isEnergyContainer(itemStack) ) {
			final int amount_RF = FE_getMaxEnergyStorage(itemStack);
			return convertRFtoInternal_floor(amount_RF);
		}
		
		return 0;
	}
	
	public static int consume(final ItemStack itemStack, final int amount, final boolean simulate) {
		// IndustrialCraft2
		if ( WarpDriveConfig.ENERGY_ENABLE_IC2_EU
		  && WarpDriveConfig.isIndustrialCraft2Loaded
		  && IC2_isEnergyContainer(itemStack) ) {
			final double amount_EU = convertInternalToEU_ceil(amount);
			return (int) convertEUtoInternal_floor(IC2_consume(itemStack, amount_EU, simulate));
		}
		
		// Gregtech
		if ( WarpDriveConfig.ENERGY_ENABLE_GTCE_EU
		  && WarpDriveConfig.isGregtechLoaded
		  && GT_isEnergyContainer(itemStack) ) {
			final long amount_EU = convertInternalToGT_ceil(amount);
			return (int) convertGTtoInternal_floor(GT_consume(itemStack, amount_EU, simulate));
		}
		
		// RedstoneFlux
		if ( WarpDriveConfig.ENERGY_ENABLE_RF
		  && WarpDriveConfig.isRedstoneFluxLoaded
		  && RF_isEnergyContainer(itemStack) ) {
			final int amount_RF = convertInternalToRF_ceil(amount);
			return (int) convertRFtoInternal_floor(RF_consume(itemStack, amount_RF, simulate));
		}
		
		// Forge Energy
		if ( WarpDriveConfig.ENERGY_ENABLE_FE
		  && FE_isEnergyContainer(itemStack) ) {
			final int amount_RF = convertInternalToRF_ceil(amount);
			return (int) convertRFtoInternal_floor(FE_consume(itemStack, amount_RF, simulate));
		}
		
		return 0;
	}
	
	public static long charge(final ItemStack itemStack, final int amount, final boolean simulate) {
		// IndustrialCraft2
		if ( WarpDriveConfig.ENERGY_ENABLE_IC2_EU
		  && WarpDriveConfig.isIndustrialCraft2Loaded
		  && IC2_isEnergyContainer(itemStack) ) {
			final double amount_EU = convertInternalToEU_floor(amount);
			return convertEUtoInternal_ceil(IC2_charge(itemStack, amount_EU, simulate));
		}
		
		// Gregtech
		if ( WarpDriveConfig.ENERGY_ENABLE_GTCE_EU
		  && WarpDriveConfig.isGregtechLoaded
		  && GT_isEnergyContainer(itemStack) ) {
			final long amount_EU = convertInternalToGT_floor(amount);
			return convertEUtoInternal_ceil(GT_charge(itemStack, amount_EU, simulate));
		}
		
		// RedstoneFlux
		if ( WarpDriveConfig.ENERGY_ENABLE_RF
		  && WarpDriveConfig.isRedstoneFluxLoaded
		  && RF_isEnergyContainer(itemStack) ) {
			final int amount_RF = convertInternalToRF_floor(amount);
			return convertRFtoInternal_ceil(RF_charge(itemStack, amount_RF, simulate));
		}
		
		// Forge Energy
		if ( WarpDriveConfig.ENERGY_ENABLE_FE
		  && FE_isEnergyContainer(itemStack) ) {
			final int amount_RF = convertInternalToRF_floor(amount);
			return convertRFtoInternal_ceil(FE_charge(itemStack, amount_RF, simulate));
		}
		
		return 0;
	}
	
	// IndustrialCraft IElectricItem interface
	@Optional.Method(modid = "ic2")
	private static IElectricItemManager IC2_getManager(final ItemStack itemStack) {
		final Item item = itemStack.getItem();
		if (item instanceof ISpecialElectricItem) {
			return ((ISpecialElectricItem) item).getManager(itemStack);
		}
		if (item instanceof IElectricItem) {
			return ElectricItem.rawManager;
		}
		return ElectricItem.getBackupManager(itemStack);
	}
	
	@Optional.Method(modid = "ic2")
	private static boolean IC2_isEnergyContainer(final ItemStack itemStack) {
		return itemStack.getItem() instanceof IElectricItem;
	}
	
	@Optional.Method(modid = "ic2")
	private static boolean IC2_canOutput(final ItemStack itemStack) {
		return ((IElectricItem) itemStack.getItem()).canProvideEnergy(itemStack);
	}
	
	@Optional.Method(modid = "ic2")
	private static boolean IC2_canInput(final ItemStack itemStack) {
		final IElectricItemManager electricItemManager = IC2_getManager(itemStack);
		if (electricItemManager == null) {
			return false;
		}
		return electricItemManager.getCharge(itemStack) < IC2_getMaxEnergyStorage(itemStack);
	}
	
	@Optional.Method(modid = "ic2")
	private static double IC2_getEnergyStored(final ItemStack itemStack) {
		final IElectricItemManager electricItemManager = IC2_getManager(itemStack);
		if (electricItemManager == null) {
			return 0.0D;
		}
		return electricItemManager.getCharge(itemStack);
	}
	
	@Optional.Method(modid = "ic2")
	private static double IC2_getMaxEnergyStorage(final ItemStack itemStack) {
		return ((IElectricItem) itemStack.getItem()).getMaxCharge(itemStack);
	}
	
	@Optional.Method(modid = "ic2")
	private static double IC2_consume(final ItemStack itemStack, final double amount_EU, final boolean simulate) {
		final IElectricItemManager electricItemManager = IC2_getManager(itemStack);
		if (electricItemManager == null) {
			return 0.0D;
		}
		if (amount_EU <= electricItemManager.getCharge(itemStack)) {
			if (!simulate) {
				return electricItemManager.discharge(itemStack, amount_EU, ((IElectricItem) itemStack.getItem()).getTier(itemStack), true, true, simulate);
			} else {
				return amount_EU;
			}
		}
		return 0.0D;
	}
	
	@Optional.Method(modid = "ic2")
	private static double IC2_charge(final ItemStack itemStack, final double amount_EU, final boolean simulate) {
		final IElectricItemManager electricItemManager = IC2_getManager(itemStack);
		if (electricItemManager == null) {
			return 0.0D;
		}
		if (electricItemManager.getCharge(itemStack) < IC2_getMaxEnergyStorage(itemStack)) {
			if (!simulate) {
				return electricItemManager.charge(itemStack, amount_EU, ((IElectricItem) itemStack.getItem()).getTier(itemStack), true, simulate);
			} else {
				return amount_EU;
			}
		}
		return 0.0D;
	}
	
	
	// Gregtech energy capability
	@Optional.Method(modid = "gregtech")
	private static boolean GT_isEnergyContainer(final ItemStack itemStack) {
		return itemStack.hasCapability(GregtechCapabilities.CAPABILITY_ELECTRIC_ITEM, null);
	}
	
	@Optional.Method(modid = "gregtech")
	private static boolean GT_canOutput(final ItemStack itemStack) {
		final gregtech.api.capability.IElectricItem electricItem = itemStack.getCapability(GregtechCapabilities.CAPABILITY_ELECTRIC_ITEM, null);
		assert electricItem != null;
		return electricItem.canProvideChargeExternally();
	}
	
	@Optional.Method(modid = "gregtech")
	private static boolean GT_canInput(final ItemStack itemStack) {
		final gregtech.api.capability.IElectricItem electricItem = itemStack.getCapability(GregtechCapabilities.CAPABILITY_ELECTRIC_ITEM, null);
		assert electricItem != null;
		return electricItem.canUse(1L);
	}
	
	@Optional.Method(modid = "gregtech")
	private static long GT_getEnergyStored(final ItemStack itemStack) {
		final gregtech.api.capability.IElectricItem electricItem = itemStack.getCapability(GregtechCapabilities.CAPABILITY_ELECTRIC_ITEM, null);
		assert electricItem != null;
		return electricItem.discharge(Integer.MAX_VALUE, Integer.MAX_VALUE, true, true, true);
	}
	
	@Optional.Method(modid = "gregtech")
	private static long GT_getMaxEnergyStorage(final ItemStack itemStack) {
		final gregtech.api.capability.IElectricItem electricItem = itemStack.getCapability(GregtechCapabilities.CAPABILITY_ELECTRIC_ITEM, null);
		assert electricItem != null;
		return electricItem.getMaxCharge();
	}
	
	@Optional.Method(modid = "gregtech")
	private static long GT_consume(final ItemStack itemStack, final long amount_EU, final boolean simulate) {
		final gregtech.api.capability.IElectricItem electricItem = itemStack.getCapability(GregtechCapabilities.CAPABILITY_ELECTRIC_ITEM, null);
		assert electricItem != null;
		return electricItem.discharge(amount_EU, Integer.MAX_VALUE, true, true, simulate);
	}
	
	@Optional.Method(modid = "gregtech")
	private static long GT_charge(final ItemStack itemStack, final long amount_EU, final boolean simulate) {
		final gregtech.api.capability.IElectricItem electricItem = itemStack.getCapability(GregtechCapabilities.CAPABILITY_ELECTRIC_ITEM, null);
		assert electricItem != null;
		return electricItem.charge(amount_EU, Integer.MAX_VALUE, true, simulate);
	}
	
	
	// RedstoneFlux IEnergyContainerItem interface
	@Optional.Method(modid = "redstoneflux")
	private static boolean RF_isEnergyContainer(final ItemStack itemStack) {
		return itemStack.getItem() instanceof IEnergyContainerItem;
	}
	
	@Optional.Method(modid = "redstoneflux")
	private static boolean RF_canOutput(final ItemStack itemStack) {
		return ((IEnergyContainerItem) itemStack.getItem()).getEnergyStored(itemStack) > 0;
	}
	
	@Optional.Method(modid = "redstoneflux")
	private static boolean RF_canInput(final ItemStack itemStack) {
		return ((IEnergyContainerItem) itemStack.getItem()).getEnergyStored(itemStack) < ((IEnergyContainerItem) itemStack.getItem()).getMaxEnergyStored(itemStack);
	}
	
	@Optional.Method(modid = "redstoneflux")
	private static int RF_getEnergyStored(final ItemStack itemStack) {
		return (int) Math.floor( ((IEnergyContainerItem) itemStack.getItem()).getEnergyStored(itemStack) );
	}
	
	@Optional.Method(modid = "redstoneflux")
	private static int RF_getMaxEnergyStorage(final ItemStack itemStack) {
		return (int) Math.floor( ((IEnergyContainerItem) itemStack.getItem()).getMaxEnergyStored(itemStack) );
	}
	
	@Optional.Method(modid = "redstoneflux")
	private static int RF_consume(final ItemStack itemStack, final int amount_RF, final boolean simulate) {
		return ((IEnergyContainerItem) itemStack.getItem()).extractEnergy(itemStack, amount_RF, simulate);
	}
	
	@Optional.Method(modid = "redstoneflux")
	private static int RF_charge(final ItemStack itemStack, final int amount_RF, final boolean simulate) {
		return ((IEnergyContainerItem) itemStack.getItem()).receiveEnergy(itemStack, amount_RF, simulate);
	}
	
	
	// Forge Energy capability
	private static boolean FE_isEnergyContainer(final ItemStack itemStack) {
		return itemStack.hasCapability(CapabilityEnergy.ENERGY, null);
	}
	
	private static boolean FE_canOutput(final ItemStack itemStack) {
		final IEnergyStorage energyStorage = itemStack.getCapability(CapabilityEnergy.ENERGY, null);
		assert energyStorage != null;
		return energyStorage.canExtract();
	}
	
	private static boolean FE_canInput(final ItemStack itemStack) {
		final IEnergyStorage energyStorage = itemStack.getCapability(CapabilityEnergy.ENERGY, null);
		assert energyStorage != null;
		return energyStorage.canReceive();
	}
	
	private static int FE_getEnergyStored(final ItemStack itemStack) {
		final IEnergyStorage energyStorage = itemStack.getCapability(CapabilityEnergy.ENERGY, null);
		assert energyStorage != null;
		return (int) Math.floor( energyStorage.getEnergyStored() );
	}
	
	private static int FE_getMaxEnergyStorage(final ItemStack itemStack) {
		final IEnergyStorage energyStorage = itemStack.getCapability(CapabilityEnergy.ENERGY, null);
		assert energyStorage != null;
		return (int) Math.floor( energyStorage.getMaxEnergyStored() );
	}
	
	private static int FE_consume(final ItemStack itemStack, final int amount_RF, final boolean simulate) {
		final IEnergyStorage energyStorage = itemStack.getCapability(CapabilityEnergy.ENERGY, null);
		assert energyStorage != null;
		return energyStorage.extractEnergy(amount_RF, simulate);
	}
	
	private static int FE_charge(final ItemStack itemStack, final int amount_RF, final boolean simulate) {
		final IEnergyStorage energyStorage = itemStack.getCapability(CapabilityEnergy.ENERGY, null);
		assert energyStorage != null;
		return energyStorage.receiveEnergy(amount_RF, simulate);
	}
}
