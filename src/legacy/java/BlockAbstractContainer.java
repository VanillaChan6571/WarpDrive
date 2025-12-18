package cr0s.warpdrive.block;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IBlockBase;
import cr0s.warpdrive.api.IBlockUpdateDetector;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.EnumHorizontalSpinning;
import cr0s.warpdrive.data.EnumTier;
import cr0s.warpdrive.data.BlockProperties;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;

import net.minecraftforge.common.IRarity;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ConcurrentModificationException;
import java.util.Random;

public abstract class BlockAbstractContainer extends BlockContainer implements IBlockBase {
	
	private static long timeUpdated = -1L;
	private static int dimensionIdUpdated = Integer.MAX_VALUE;
	private static int xUpdated = Integer.MAX_VALUE;
	private static int yUpdated = Integer.MAX_VALUE;
	private static int zUpdated = Integer.MAX_VALUE;
	
	protected EnumTier enumTier;
	protected boolean hasSubBlocks = false;
	protected boolean ignoreFacingOnPlacement = false;
	
	protected BlockAbstractContainer(final String registryName, final EnumTier enumTier, final Material material) {
		super(material);
		
		this.enumTier = enumTier;
		setHardness(5.0F);
		setResistance(6.0F * 5 / 3);
		setSoundType(SoundType.METAL);
		setCreativeTab(WarpDrive.creativeTabMain);
		setRegistryName(registryName);
		WarpDrive.register(this);
	}
	
	@Nullable
	@Override
	public ItemBlock createItemBlock() {
		return new ItemBlockAbstractBase(this, false, true);
	}
	
	@SideOnly(Side.CLIENT)
	@Override
	public void modelInitialisation() {
		// no operation
	}
	
	@SuppressWarnings("deprecation")
	@Nonnull
	@Override
	public EnumBlockRenderType getRenderType(@Nonnull final IBlockState blockState) {
		return EnumBlockRenderType.MODEL;
	}
	
	@Override
	public void onBlockAdded(@Nonnull final World world, @Nonnull final BlockPos blockPos, @Nonnull final IBlockState blockState) {
		super.onBlockAdded(world, blockPos, blockState);
		final TileEntity tileEntity = world.getTileEntity(blockPos);
		if (tileEntity instanceof IBlockUpdateDetector) {
			((IBlockUpdateDetector) tileEntity).onBlockUpdateDetected(blockPos);
		}
	}
	
	@Nonnull
	@Override
	public IBlockState getStateForPlacement(@Nonnull final World world, @Nonnull final BlockPos blockPos, @Nonnull final EnumFacing facing,
	                                        final float hitX, final float hitY, final float hitZ, final int metadata,
	                                        @Nonnull final EntityLivingBase entityLivingBase, @Nonnull final EnumHand enumHand) {
		final IBlockState blockState = super.getStateForPlacement(world, blockPos, facing, hitX, hitY, hitZ, metadata, entityLivingBase, enumHand);
		if (!ignoreFacingOnPlacement) {
			// start with the most complex: spinning down/up & rotating otherwise
			if (blockState.getProperties().containsKey(BlockProperties.HORIZONTAL_SPINNING)) {
				final EnumFacing enumFacing;
				final EnumFacing enumSpinning;
				if (blockState.isFullBlock()) {
					enumFacing = Commons.getFacingFromEntity(entityLivingBase);
					enumSpinning = Commons.getHorizontalDirectionFromEntity(entityLivingBase).getOpposite();
				} else {
					enumFacing = facing;
					enumSpinning = Commons.getHorizontalDirectionFromEntity(entityLivingBase);
				}
				final EnumHorizontalSpinning enumHorizontalSpinning = EnumHorizontalSpinning.get(enumFacing, enumSpinning);
				return blockState.withProperty(BlockProperties.HORIZONTAL_SPINNING, enumHorizontalSpinning);
			}
			
			// then 6 sided rotating
			if (blockState.getProperties().containsKey(BlockProperties.FACING)) {
				if (blockState.isFullBlock()) {
					final EnumFacing enumFacing = Commons.getFacingFromEntity(entityLivingBase);
					return blockState.withProperty(BlockProperties.FACING, enumFacing);
				} else {
					return blockState.withProperty(BlockProperties.FACING, facing);
				}
			}
			
			// finally, only rotating horizontally
			if (blockState.getProperties().containsKey(BlockProperties.FACING_HORIZONTAL)) {
				final EnumFacing enumFacing = Commons.getHorizontalDirectionFromEntity(entityLivingBase);
				if (blockState.isFullBlock()) {
					return blockState.withProperty(BlockProperties.FACING_HORIZONTAL, enumFacing.getOpposite());
				} else {
					return blockState.withProperty(BlockProperties.FACING_HORIZONTAL, enumFacing);
				}
			}
		}
		return blockState;
	}
	
	@Override
	public void onBlockPlacedBy(@Nonnull final World world, @Nonnull final BlockPos blockPos, @Nonnull final IBlockState blockState,
	                            @Nonnull final EntityLivingBase entityLivingBase, @Nonnull final ItemStack itemStack) {
		super.onBlockPlacedBy(world, blockPos, blockState, entityLivingBase, itemStack);
		
		// set inherited properties
		final TileEntity tileEntity = world.getTileEntity(blockPos);
		assert tileEntity instanceof TileEntityAbstractBase;
		if (itemStack.getTagCompound() != null) {
			final NBTTagCompound tagCompound = itemStack.getTagCompound().copy();
			tagCompound.setInteger("x", blockPos.getX());
			tagCompound.setInteger("y", blockPos.getY());
			tagCompound.setInteger("z", blockPos.getZ());
			tileEntity.readFromNBT(tagCompound);
			tileEntity.markDirty();
			world.notifyBlockUpdate(blockPos, blockState, blockState, 3);
		}
	}
	
	@Override
	public boolean removedByPlayer(@Nonnull final IBlockState blockState, @Nonnull final World world, @Nonnull final BlockPos blockPos,
	                               @Nonnull final EntityPlayer entityPlayer, final boolean willHarvest) {
		final boolean bResult;
		if (willHarvest) {// harvestBlock will be called later on, we'll need the TileEntity at that time, so we don't call ancestor here so we don't set block to air
			this.onBlockHarvested(world, blockPos, blockState, entityPlayer);
			bResult = true;
		} else {
			bResult = super.removedByPlayer(blockState, world, blockPos, entityPlayer, false);
		}
		return bResult;
	}
	
	@Override
	public void dropBlockAsItemWithChance(@Nonnull final World world, @Nonnull final BlockPos blockPos, @Nonnull final IBlockState blockState,
	                                      final float chance, final int fortune) {
		super.dropBlockAsItemWithChance(world, blockPos, blockState, chance, fortune); // calls getDrops() here below
		if ( !world.isRemote
		  && !world.restoringBlockSnapshots) {
			world.setBlockToAir(blockPos);
		}
	}
	
	// willHarvest was true during the call to removedPlayer so TileEntity is still there when drops will be computed hereafter
	@Override
	public void getDrops(@Nonnull final NonNullList<ItemStack> drops,
	                     @Nonnull final IBlockAccess blockAccess, @Nonnull final BlockPos blockPos, @Nonnull final IBlockState blockState,
	                     final int fortune) {
		final TileEntity tileEntity = blockAccess.getTileEntity(blockPos);
		if (!(tileEntity instanceof TileEntityAbstractBase)) {
			WarpDrive.logger.error(String.format("Missing tile entity for %s %s, reverting to vanilla getDrops logic",
			                                     this, Commons.format(blockAccess, blockPos)));
			super.getDrops(drops, blockAccess, blockPos, blockState, fortune);
			return;
		}
		
		final Random rand = blockAccess instanceof World ? ((World) blockAccess).rand : RANDOM;
		final int count = quantityDropped(blockState, fortune, rand);
		for (int i = 0; i < count; i++) {
			final Item item = this.getItemDropped(blockState, rand, fortune);
			if (item != Items.AIR) {
				final ItemStack itemStack = new ItemStack(item, 1, damageDropped(blockState));
				final NBTTagCompound tagCompound = new NBTTagCompound();
				((TileEntityAbstractBase) tileEntity).writeItemDropNBT(tagCompound);
				if (!tagCompound.isEmpty()) {
					itemStack.setTagCompound(tagCompound);
				}
				drops.add(itemStack);
			}
		}
	}
	
	@Override
	public void breakBlock(@Nonnull final World world, @Nonnull final BlockPos blockPos, @Nonnull final IBlockState blockState) {
		// cascade to tile entity before it's removed
		final TileEntity tileEntity = world.getTileEntity(blockPos);
		if (tileEntity instanceof TileEntityAbstractBase) {
			((TileEntityAbstractBase) tileEntity).onBlockBroken(world, blockPos, blockState);
		}
		super.breakBlock(world, blockPos, blockState);
	}
	
	@Nonnull
	@Override
	public ItemStack getPickBlock(@Nonnull final IBlockState blockState, @Nullable final RayTraceResult target,
	                              @Nonnull final World world, @Nonnull final BlockPos blockPos, @Nullable final EntityPlayer entityPlayer) {
		// note: target and entityPlayer should be Nonnull but the base game & most mods never use those parameters.
		// Consequently, this method is frequently called with nulls...
		final ItemStack itemStack = super.getPickBlock(blockState, target, world, blockPos, entityPlayer);
		final TileEntity tileEntity = world.getTileEntity(blockPos);
		final NBTTagCompound tagCompound = new NBTTagCompound();
		if (tileEntity instanceof TileEntityAbstractBase) {
			((TileEntityAbstractBase) tileEntity).writeItemDropNBT(tagCompound);
			if (!tagCompound.isEmpty()) {
				itemStack.setTagCompound(tagCompound);
			}
		}
		return itemStack;
	}
	
	@Override
	public boolean rotateBlock(@Nonnull final World world, @Nonnull final BlockPos blockPos, @Nonnull final EnumFacing axis) {
		// already handled by vanilla
		final boolean isRotated = super.rotateBlock(world, blockPos, axis);
		if (isRotated) {
			final TileEntity tileEntity = world.getTileEntity(blockPos);
			if (tileEntity instanceof TileEntityAbstractMachine) {
				((TileEntityAbstractMachine) tileEntity).markDirtyAssembly();
			}
		}
		return isRotated;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public void neighborChanged(@Nonnull final IBlockState blockState, @Nonnull final World world, @Nonnull final BlockPos blockPos,
	                            @Nonnull final Block block, @Nonnull final BlockPos blockPosFrom) {
		super.neighborChanged(blockState, world, blockPos, block, blockPosFrom);
		onBlockUpdateDetected(world, blockPos, blockPosFrom);
	}
	
	// Triggers on server side when placing a comparator compatible block
	// May trigger twice for the same placement action (placing a vanilla chest)
	// Triggers on server side when removing a comparator compatible block
	// Triggers on both sides when removing a TileEntity
	// (by extension, it'll trigger twice for the same placement of a TileEntity with comparator output)
	@Override
	public void onNeighborChange(@Nonnull final IBlockAccess blockAccess, @Nonnull final BlockPos blockPos, @Nonnull final BlockPos blockPosNeighbor) {
		super.onNeighborChange(blockAccess, blockPos, blockPosNeighbor);
		onBlockUpdateDetected(blockAccess, blockPos, blockPosNeighbor);
	}
	
	@Override
	public void observedNeighborChange(@Nonnull final IBlockState observerState, @Nonnull final World world, @Nonnull final BlockPos blockPosObserver,
	                                   @Nonnull final Block blockChanged, @Nonnull final BlockPos blockPosChanged) {
		super.observedNeighborChange(observerState, world, blockPosObserver, blockChanged, blockPosChanged);
		onBlockUpdateDetected(world, blockPosObserver, blockPosChanged);
	}
	
	// due to our redirection, this may trigger up to 6 times for the same event (for example, when placing a chest)
	protected void onBlockUpdateDetected(@Nonnull final IBlockAccess blockAccess, @Nonnull final BlockPos blockPos, @Nonnull final BlockPos blockPosUpdated) {
		if (!Commons.isSafeThread()) {
			if (WarpDriveConfig.LOGGING_PROFILING_THREAD_SAFETY) {
				final Block blockNeighbor = blockAccess.getBlockState(blockPosUpdated).getBlock();
				final ResourceLocation registryName = blockNeighbor.getRegistryName();
				WarpDrive.logger.error(String.format("Bad multithreading detected from mod %s %s, please report to mod author",
				                                     registryName == null ? blockNeighbor : registryName.getNamespace(),
				                                     Commons.format(blockAccess, blockPosUpdated)));
				new ConcurrentModificationException().printStackTrace(WarpDrive.printStreamError);
			}
			return;
		}
		
		// try reducing duplicated events
		// note: this is just a fast check, notably, this won't cover placing a block in between 2 of ours
		if (blockAccess instanceof World) {
			final World world = (World) blockAccess;
			if ( timeUpdated == world.getTotalWorldTime()
			  && dimensionIdUpdated == world.provider.getDimension()
			  && xUpdated == blockPos.getX()
			  && yUpdated == blockPos.getY()
			  && zUpdated == blockPos.getZ() ) {
				return;
			}
			timeUpdated = world.getTotalWorldTime();
			dimensionIdUpdated = world.provider.getDimension();
			xUpdated = blockPos.getX();
			yUpdated = blockPos.getY();
			zUpdated = blockPos.getZ();
		}
		
		final TileEntity tileEntity = blockAccess.getTileEntity(blockPos);
		if ( tileEntity == null
		  || tileEntity.getWorld().isRemote ) {
			return;
		}
		if (tileEntity instanceof IBlockUpdateDetector) {
			((IBlockUpdateDetector) tileEntity).onBlockUpdateDetected(blockPosUpdated);
		}
	}
	
	public void onEMP(@Nonnull final World world, @Nonnull final BlockPos blockPos, final float efficiency) {
		final TileEntity tileEntity = world.getTileEntity(blockPos);
		if (tileEntity instanceof TileEntityAbstractEnergy) {
			final TileEntityAbstractEnergy tileEntityAbstractEnergy = (TileEntityAbstractEnergy) tileEntity;
			if (tileEntityAbstractEnergy.energy_getMaxStorage() > 0) {
				tileEntityAbstractEnergy.energy_consume(Math.round(tileEntityAbstractEnergy.energy_getEnergyStored() * efficiency), false);
			}
		}
	}
	
	@Nonnull
	@Override
	public EnumTier getTier(final ItemStack itemStack) {
		return enumTier;
	}
	
	@Nonnull
	@Override
	public IRarity getForgeRarity(@Nonnull final ItemStack itemStack) {
		return getTier(itemStack).getForgeRarity();
	}
	
	@Override
	public boolean onBlockActivated(@Nonnull final World world, @Nonnull final BlockPos blockPos, @Nonnull final IBlockState blockState,
	                                @Nonnull final EntityPlayer entityPlayer, @Nonnull final EnumHand enumHand,
	                                @Nonnull final EnumFacing enumFacing, final float hitX, final float hitY, final float hitZ) {
		if (BlockAbstractBase.onCommonBlockActivated(world, blockPos, blockState, entityPlayer, enumHand, enumFacing, hitX, hitY, hitZ)) {
			return true;
		}
		
		return super.onBlockActivated(world, blockPos, blockState, entityPlayer, enumHand, enumFacing, hitX, hitY, hitZ);
	}
}
