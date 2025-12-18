package cr0s.warpdrive.block.collection;

import cr0s.warpdrive.CommonProxy;
import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.block.TileEntityAbstractLaser;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.FluidWrapper;
import cr0s.warpdrive.data.InventoryWrapper;
import cr0s.warpdrive.data.TankWrapper;
import cr0s.warpdrive.data.Vector3;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.IGrowable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;

public abstract class TileEntityAbstractMiner extends TileEntityAbstractLaser {
	
	// machine type
	protected EnumFacing laserOutputSide = EnumFacing.NORTH;
	
	// machine state
	protected boolean		 enableSilktouch = false;
	
	// pre-computation
	protected Vector3        laserOutput = null;
	
	public TileEntityAbstractMiner() {
		super();
	}
	
	@Override
	protected void onFirstUpdateTick() {
		super.onFirstUpdateTick();
		laserOutput = new Vector3(this).translate(0.5D).translate(laserOutputSide, 0.5D);
	}
	
	protected void harvestBlock(@Nonnull final BlockPos blockPos, @Nonnull final IBlockState blockState) {
		if (blockState.getBlock().isAir(blockState, world, blockPos)) {
			return;
		}
		
		final Fluid fluid = FluidWrapper.getFluid(blockState);
		if (fluid != null) {// (this is a fluid block)
			if ( WarpDriveConfig.MINING_LASER_PUMP_UPGRADE_HARVEST_FLUID
			  && FluidWrapper.isSourceBlock(world, blockPos, blockState) ) {// (fluid collection is enabled & it's a source block)
				final Collection<Object> connectedTanks = TankWrapper.getConnectedTanks(world, pos);
				if (!connectedTanks.isEmpty()) {// (at least 1 tank is connected)
					final FluidStack fluidStack = new FluidStack(fluid, 1000);
					
					final boolean fluidOverflowed = TankWrapper.addToTanks(world, pos, connectedTanks, fluidStack);
					if (fluidOverflowed) {
						// assume player wants to collect the fluid, hence stop the mining in case of overflow
						setIsEnabled(false);
					}
					
					// Collect Fluid
					world.playSound(null, blockPos, fluid.getFillSound(fluidStack), SoundCategory.BLOCKS, 0.5F,
								2.6F + (world.rand.nextFloat() - world.rand.nextFloat()) * 0.8F);
				}
			} else {
				// Evaporate fluid
				world.playSound(null, blockPos, net.minecraft.init.SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.5F,
							2.6F + (world.rand.nextFloat() - world.rand.nextFloat()) * 0.8F);
			}
			// remove without updating neighbours
			world.setBlockState(blockPos, Blocks.AIR.getDefaultState(), 2);
			
		} else {
			final List<ItemStack> itemStackDrops = getItemStackFromBlock(blockPos, blockState);
			
			final EntityPlayer entityPlayer = CommonProxy.getFakePlayer(null, (WorldServer) world, blockPos);
			net.minecraftforge.event.ForgeEventFactory.fireBlockHarvesting(itemStackDrops, getWorld(), blockPos, blockState,
			                                                               0, 1.0f, true, entityPlayer);
			
			// standard harvest block effect
			world.playEvent(2001, blockPos, Block.getStateId(blockState));
			
			// remove while updating neighbours
			world.setBlockToAir(blockPos); // setBlockState(blockPos, Blocks.AIR.getDefaultState(), 3);
			
			// try to replant the crop
			if ( itemStackDrops != null
			  && blockState.getBlock() instanceof IGrowable ) {
				for (final ItemStack itemStackPlant : itemStackDrops) {
					if (itemStackPlant.getItem() instanceof IPlantable) {
						final IPlantable plantable = (IPlantable) itemStackPlant.getItem();
						final IBlockState blockStatePlant = plantable.getPlant(world, blockPos);
						if (WarpDriveConfig.LOGGING_COLLECTION) {
							WarpDrive.logger.info(String.format("Drop includes %s which is plantable %s as block %s",
							                                    itemStackPlant, plantable, blockStatePlant));
						}
						final BlockPos blockPosSoil = blockPos.down();
						final IBlockState blockStateSoil = getWorld().getBlockState(blockPosSoil);
						if (!blockStateSoil.getBlock().canSustainPlant(blockStateSoil, world, blockPosSoil, EnumFacing.UP, plantable)) {
							continue;
						}
						
						if (!blockStatePlant.getBlock().canPlaceBlockAt(world, blockPos)) {
							continue;
						}
						
						// (we're sticking to harvesting effects)
						world.setBlockState(blockPos, blockStatePlant, 3);
						
						// refresh the drops
						itemStackDrops.remove(itemStackPlant);
						itemStackPlant.shrink(1);
						if (!itemStackPlant.isEmpty()) {
							itemStackDrops.add(itemStackPlant);
						}
						break;
					}
				}
			}
			
			if (InventoryWrapper.addToConnectedInventories(world, pos, itemStackDrops)) {
				setIsEnabled(false);
			}
		}
	}
	
	@Nullable
	private NonNullList<ItemStack> getItemStackFromBlock(final BlockPos blockPos, final IBlockState blockState) {
		if (blockState == null) {
			WarpDrive.logger.error(String.format("%s Invalid block %s",
			                                     this, Commons.format(world, blockPos)));
			return null;
		}
		final NonNullList<ItemStack> itemStackDrops = NonNullList.create();
		boolean isHarvested = false;
		if (enableSilktouch) {
			boolean isSilkHarvestable = false;
			try {
				isSilkHarvestable = blockState.getBlock().canSilkHarvest(world, blockPos, blockState, null);
			} catch (final Exception exception) {// protect in case the mined block is corrupted
				exception.printStackTrace(WarpDrive.printStreamError);
			}
			if (isSilkHarvestable) {
				// intended code if AccessTransformer was working with gradlew:
				// itemStackDrops.add(blockState.getBlock().getSilkTouchDrop(blockState));
				
				final ItemStack itemStackDrop;
				try {
					itemStackDrop = (ItemStack) WarpDrive.methodBlock_getSilkTouch.invoke(blockState.getBlock(), blockState);
				} catch (final IllegalAccessException | InvocationTargetException exception) {
					throw new RuntimeException(exception);
				}
				if (!itemStackDrop.isEmpty()) {
					itemStackDrops.add(itemStackDrop);
				}
				isHarvested = true;
			}
		}
		
		try {
			if (!isHarvested) {
				blockState.getBlock().getDrops(itemStackDrops, world, blockPos, blockState, 0);
			}
		} catch (final Exception exception) {// protect in case the mined block is corrupted
			exception.printStackTrace(WarpDrive.printStreamError);
			return null;
		}
		
		return itemStackDrops;
	}
	
	// NBT DATA
	@Override
	public void readFromNBT(@Nonnull final NBTTagCompound tagCompound) {
		super.readFromNBT(tagCompound);
		enableSilktouch = tagCompound.getBoolean("enableSilktouch");
	}
	
	@Nonnull
	@Override
	public NBTTagCompound writeToNBT(@Nonnull NBTTagCompound tagCompound) {
		tagCompound = super.writeToNBT(tagCompound);
		tagCompound.setBoolean("enableSilktouch", enableSilktouch);
		return tagCompound;
	}
}
