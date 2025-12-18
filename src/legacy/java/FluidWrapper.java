package cr0s.warpdrive.data;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.config.WarpDriveConfig;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.concurrent.CopyOnWriteArraySet;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidBlock;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

public class FluidWrapper {
	
	// constants
	public static final String TAG_FLUID = "fluid";
	public static final int MB_PER_TINY_PILE  = 144 / 9;
	public static final int MB_PER_SMALL_PILE = 144 / 4;
	public static final int MB_PER_NUGGET = 144 / 9;
	public static final int MB_PER_INGOT = 144;
	public static final int MB_PER_BLOCK = 9 * 144;
	public static final int MB_PER_BUCKET = 1000;
	
	// log throttle
	private static final CopyOnWriteArraySet<Block> blockInvalidFluid = new CopyOnWriteArraySet<>();
	
	// conversion handling
	// @TODO
	
	public static String format(final long energy, final String units) {
		return Commons.format(convert(energy, units));
	}
	
	public static void formatAndAppendCharge(@Nonnull final WarpDriveText warpDriveText,
	                                         final long energyStored, final long maxStorage, final String units) {
		final String unitsToUse = units == null ? "WarpDriveConfig.FLUID_DISPLAY_UNITS" : units;
		final String energyStored_units = FluidWrapper.format(energyStored, unitsToUse);
		final String energyMaxStorage_units = FluidWrapper.format(maxStorage, unitsToUse);
		final WarpDriveText textRate = new WarpDriveText(null, "warpdrive.fluid.status_line.charge")
				                               .appendInLine(null, " ")
				                               .appendInLine(Commons.getStyleValue(), energyStored_units)
				                               .appendInLine(null, " / ")
				                               .appendInLine(Commons.getStyleValue(), energyMaxStorage_units)
				                               .appendInLine(null, String.format(" %s.", unitsToUse));
		warpDriveText.append(textRate);
	}
	
	public static void formatAndAppendInputRate(@Nonnull final WarpDriveText warpDriveText,
	                                            final long rate, final String units) {
		formatAndAppendRate(warpDriveText, "warpdrive.fluid.status_line.input_rate",
		                    rate, units);
	}
	
	public static void formatAndAppendOutputRate(@Nonnull final WarpDriveText warpDriveText,
	                                             final long rate, final String units) {
		formatAndAppendRate(warpDriveText, "warpdrive.fluid.status_line.output_rate",
		                    rate, units);
	}
	
	public static long convert(final long value, final String units) {
		final String unitsToUse = units == null ? WarpDriveConfig.ENERGY_DISPLAY_UNITS : units;
		switch (unitsToUse) {
		case "bucket":
			return (long) Math.floor(value / (double) MB_PER_BUCKET);
			
		case "ingot":
			return (long) Math.floor(value / (double) MB_PER_INGOT);
			
		default:
			return value;
		}
	}
	
	private static void formatAndAppendRate(@Nonnull final WarpDriveText warpDriveText, @Nonnull final String translationKey,
	                                        final long rate, final String units) {
		final String unitsToUse = units == null ? WarpDriveConfig.ENERGY_DISPLAY_UNITS : units;
		final WarpDriveText textRate = new WarpDriveText(null, translationKey)
				                               .appendInLine(Commons.getStyleValue(), String.format(" %d", convert(rate, unitsToUse)))
				                               .appendInLine(null, String.format(" %s/t.", unitsToUse));
		warpDriveText.append(textRate);
	}
	
	// WarpDrive methods
	public static boolean isFluid(@Nonnull final IBlockState blockState) {
		return getFluid(blockState) != null;
	}
	
	@Nullable
	public static Fluid getFluid(@Nonnull final IBlockState blockState) {
		final Block block = blockState.getBlock();
		if ( block instanceof BlockLiquid
		  || block instanceof IFluidBlock ) {
			final Fluid fluid = block instanceof IFluidBlock ? ((IFluidBlock) block).getFluid() : Commons.fluid_getByBlock(block);
			if (WarpDriveConfig.LOGGING_COLLECTION) {
				WarpDrive.logger.info(String.format("Block %s %s Fluid %s with viscosity %d: %s %s",
				                                    block.getTranslationKey(),
				                                    blockState,
				                                    fluid == null ? null : fluid.getName(),
				                                    fluid == null ? 0 : fluid.getViscosity(),
				                                    block, fluid));
			}
			if (fluid == null) {
				if (!blockInvalidFluid.contains(block)) {
					WarpDrive.logger.error(String.format("Block %s %s is not a valid fluid! %s",
					                                     block.getTranslationKey(),
					                                     blockState,
					                                     block));
					blockInvalidFluid.add(block);
				}
				return null;
			} else {
				return fluid;
			}
		}
		return null;
	}
	
	public static boolean isSourceBlock(@Nonnull final World world, @Nonnull final BlockPos blockPos, @Nonnull final IBlockState blockState) {
		final Block block = blockState.getBlock();
		final int metadata = block.getMetaFromState(blockState);
		return ( block instanceof BlockLiquid && metadata == 0 )
		    || ( block instanceof IFluidBlock && ((IFluidBlock) block).canDrain(world, blockPos) );
	}
	
	public static boolean isFluidContainer(@Nonnull final ItemStack itemStack) {
		return itemStack.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null);
	}
	
	public static boolean isFluidContainer(@Nonnull final TileEntity tileEntity) {
		return tileEntity.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null);
	}
	
	public static FluidStack drain(@Nonnull final ItemStack itemStack, @Nonnull final FluidStack fluidStack, final boolean doNotSimulate) {
		final IFluidHandler fluidHandler = itemStack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null);
		if (fluidHandler == null) {
			return new FluidStack(fluidStack, 0);
		}
		return fluidHandler.drain(fluidStack, doNotSimulate);
	}
	
	public static int fill(@Nonnull final ItemStack itemStack, @Nonnull final FluidStack fluidStack, final boolean doNotSimulate) {
		final IFluidHandler fluidHandler = itemStack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null);
		if (fluidHandler == null) {
			return 0;
		}
		return fluidHandler.fill(fluidStack, doNotSimulate);
	}
	
	@Nullable
	public static FluidStack getFluidStored(@Nonnull final ItemStack itemStack) {
		final IFluidHandler fluidHandler = itemStack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, null);
		if (fluidHandler == null) {
			return null;
		}
		final IFluidTankProperties[] fluidTankPropertiesAll = fluidHandler.getTankProperties();
		for (final IFluidTankProperties fluidTankPropertiesOne : fluidTankPropertiesAll) {
			final FluidStack fluidStackContent = fluidTankPropertiesOne.getContents();
			if (fluidStackContent != null) {
				return fluidStackContent;
			}
		}
		return null;
	}
}
