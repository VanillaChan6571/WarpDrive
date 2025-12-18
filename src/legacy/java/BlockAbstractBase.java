package cr0s.warpdrive.block;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IBlockBase;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;

import net.minecraftforge.common.IRarity;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import cr0s.warpdrive.api.IVideoChannel;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.block.TileEntityAbstractBase.UpgradeSlot;
import cr0s.warpdrive.data.BlockProperties;
import cr0s.warpdrive.data.EnumTier;
import cr0s.warpdrive.render.ClientCameraHandler;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

public abstract class BlockAbstractBase extends Block implements IBlockBase {
	
	protected final EnumTier enumTier;
	protected boolean ignoreFacingOnPlacement = false;
	
	protected BlockAbstractBase(final String registryName, final EnumTier enumTier, final Material material) {
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
		
		/*
		// Force a single model through a custom state mapper
		final StateMapperBase stateMapperBase = new StateMapperBase() {
			@Nonnull
			@SideOnly(Side.CLIENT)
			@Override
			protected ModelResourceLocation getModelResourceLocation(@Nonnull final IBlockState blockState) {
				return modelResourceLocation;
			}
		};
		ModelLoader.setCustomStateMapper(this, stateMapperBase);
		
		// Bind our TESR to our tile entity
		ClientRegistry.bindTileEntitySpecialRenderer(TileEntityXXXX.class, new TileEntityXXXRenderer());
		/**/
	}
	
	@Nonnull
	@Override
	public IBlockState getStateForPlacement(@Nonnull final World world, @Nonnull final BlockPos blockPos, @Nonnull final EnumFacing facing,
	                                        final float hitX, final float hitY, final float hitZ, final int metadata,
	                                        @Nonnull final EntityLivingBase entityLivingBase, @Nonnull final EnumHand enumHand) {
		final IBlockState blockState = super.getStateForPlacement(world, blockPos, facing, hitX, hitY, hitZ, metadata, entityLivingBase, enumHand);
		final boolean isRotating = !ignoreFacingOnPlacement
		                        && blockState.getProperties().containsKey(BlockProperties.FACING);
		if (isRotating) {
			if (blockState.isFullBlock()) {
				final EnumFacing enumFacing = Commons.getFacingFromEntity(entityLivingBase);
				return blockState.withProperty(BlockProperties.FACING, enumFacing);
			} else {
				return blockState.withProperty(BlockProperties.FACING, facing);
			}
		}
		return blockState;
	}
	
	@Override
	public boolean rotateBlock(@Nonnull final World world, @Nonnull final BlockPos blockPos, @Nonnull final EnumFacing axis) {
		// already handled by vanilla
		return super.rotateBlock(world, blockPos, axis);
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
	
	public static boolean onCommonBlockActivated(@Nonnull final World world, @Nonnull final BlockPos blockPos, @Nonnull final IBlockState blockState,
	                                             @Nonnull final EntityPlayer entityPlayer, @Nonnull final EnumHand enumHand,
	                                             @Nonnull final EnumFacing enumFacing, final float hitX, final float hitY, final float hitZ) {
		if (enumHand != EnumHand.MAIN_HAND) {
			return true;
		}
		if ( world.isRemote
		  && ClientCameraHandler.isOverlayEnabled ) {
			return true;
		}
		
		// get context
		final ItemStack itemStackHeld = entityPlayer.getHeldItem(enumHand);
		final TileEntity tileEntity = world.getTileEntity(blockPos);
		if (!(tileEntity instanceof TileEntityAbstractBase)) {
			return false;
		}
		final TileEntityAbstractBase tileEntityAbstractBase = (TileEntityAbstractBase) tileEntity;
		final boolean hasVideoChannel = tileEntity instanceof IVideoChannel;
		
		// video channel is reported client side, everything else is reported server side
		// we still need to process the event server side for machines that are not full blocks, so in that case we return true
		if ( world.isRemote
		  && !hasVideoChannel ) {
			return tileEntityAbstractBase instanceof TileEntityAbstractMachine
			    && itemStackHeld.getItem() == Item.getItemFromBlock(Blocks.REDSTONE_TORCH);
		}
		
		UpgradeSlot upgradeSlot = tileEntityAbstractBase.getUpgradeSlot(itemStackHeld);
		
		// sneaking with an empty hand or an upgrade item in hand to dismount current upgrade
		if ( !world.isRemote
		  && entityPlayer.isSneaking() ) {
			// using an upgrade item or an empty hand means dismount upgrade
			if ( tileEntityAbstractBase.isUpgradeable()
			  && ( itemStackHeld.isEmpty()
			    || upgradeSlot != null ) ) {
				// find a valid upgrade to dismount
				if ( upgradeSlot == null
				  || !tileEntityAbstractBase.hasUpgrade(upgradeSlot) ) {
					upgradeSlot = tileEntityAbstractBase.getFirstUpgradeOfType(itemStackHeld.isEmpty() ? Item.class : itemStackHeld.getItem().getClass(), null);
				}
				
				if (upgradeSlot == null) {
					// no more upgrades to dismount
					Commons.addChatMessage(entityPlayer, new WarpDriveText(Commons.getStyleWarning(), "warpdrive.upgrade.result.no_upgrade_to_dismount"));
					return true;
				}
				
				if (!entityPlayer.capabilities.isCreativeMode) {
					// dismount the current upgrade item
					final ItemStack itemStackDrop = new ItemStack(upgradeSlot.itemStack.getItem(), upgradeSlot.itemStack.getCount(), upgradeSlot.itemStack.getMetadata());
					final EntityItem entityItem = new EntityItem(world, entityPlayer.posX, entityPlayer.posY + 0.5D, entityPlayer.posZ, itemStackDrop);
					entityItem.setNoPickupDelay();
					final boolean isSuccess = world.spawnEntity(entityItem);
					if (!isSuccess) {
						Commons.addChatMessage(entityPlayer, new WarpDriveText(Commons.getStyleWarning(), "warpdrive.upgrade.result.spawn_denied",
						                                                       entityItem ));
						return true;
					}
				}
				
				tileEntityAbstractBase.dismountUpgrade(upgradeSlot);
				// upgrade dismounted
				Commons.addChatMessage(entityPlayer, new WarpDriveText(Commons.getStyleCorrect(), "warpdrive.upgrade.result.dismounted",
				                                                       new TextComponentTranslation(upgradeSlot.itemStack.getTranslationKey() + ".name") ));
				return true;
			}
			
		} else if ( !entityPlayer.isSneaking()
		         && itemStackHeld.isEmpty() ) {// no sneaking and no item in hand => show status
			Commons.addChatMessage(entityPlayer, tileEntityAbstractBase.getStatus());
			return true;
			
		} else if ( !world.isRemote
		         && tileEntityAbstractBase.isUpgradeable()
		         && upgradeSlot != null ) {// no sneaking and an upgrade in hand => mounting an upgrade
			// validate quantity already installed
			if (tileEntityAbstractBase.getUpgradeMaxCount(upgradeSlot) < tileEntityAbstractBase.getUpgradeCount(upgradeSlot) + 1) {
				Commons.addChatMessage(entityPlayer, new WarpDriveText(Commons.getStyleWarning(),"warpdrive.upgrade.result.too_many_upgrades",
				                                                       tileEntityAbstractBase.getUpgradeMaxCount(upgradeSlot) ));
				return true;
			}
			// validate dependency
			if (!tileEntityAbstractBase.canMountUpgrade(upgradeSlot)) {
				Commons.addChatMessage(entityPlayer, new WarpDriveText(Commons.getStyleWarning(),"warpdrive.upgrade.result.invalid_upgrade"));
				return true;
			}
			
			if (!entityPlayer.capabilities.isCreativeMode) {
				// validate quantity
				final int countRequired = upgradeSlot.itemStack.getCount();
				if (itemStackHeld.getCount() < countRequired) {
					// not enough upgrade items
					Commons.addChatMessage(entityPlayer, new WarpDriveText(Commons.getStyleWarning(), "warpdrive.upgrade.result.not_enough_upgrades"));
					return true;
				}
				
				// update player inventory
				itemStackHeld.shrink(countRequired);
			}
			
			// mount the new upgrade item
			tileEntityAbstractBase.mountUpgrade(upgradeSlot);
			// upgrade mounted
			Commons.addChatMessage(entityPlayer, new WarpDriveText(Commons.getStyleCorrect(), "warpdrive.upgrade.result.mounted",
			                                                       new TextComponentTranslation(upgradeSlot.itemStack.getTranslationKey() + ".name") ));
			return true;
			
		} else if ( !world.isRemote
		         && tileEntityAbstractBase instanceof TileEntityAbstractMachine
		         && itemStackHeld.getItem() == Item.getItemFromBlock(Blocks.REDSTONE_TORCH) ) {// redstone torch on a machine to toggle it on/off
			final TileEntityAbstractMachine tileEntityAbstractMachine = (TileEntityAbstractMachine) tileEntityAbstractBase;
			final boolean isEnabledOld = tileEntityAbstractMachine.getIsEnabled();
			tileEntityAbstractMachine.setIsEnabled(!isEnabledOld);
			final boolean isEnabledNew = tileEntityAbstractMachine.getIsEnabled();
			if (isEnabledOld != isEnabledNew) {
				if (isEnabledNew) {
					Commons.addChatMessage(entityPlayer, Commons.getChatPrefix(blockState.getBlock())
					                                            .appendSibling(new TextComponentTranslation("warpdrive.machine.is_enabled.set.enabled")) );
				} else {
					Commons.addChatMessage(entityPlayer, Commons.getChatPrefix(blockState.getBlock())
					                                            .appendSibling(new TextComponentTranslation("warpdrive.machine.is_enabled.set.disabled")) );
				}
			} else {
				if (isEnabledNew) {
					Commons.addChatMessage(entityPlayer, Commons.getChatPrefix(blockState.getBlock())
					                                            .appendSibling(new TextComponentTranslation("warpdrive.machine.is_enabled.get.enabled")) );
				} else {
					Commons.addChatMessage(entityPlayer, Commons.getChatPrefix(blockState.getBlock())
					                                            .appendSibling(new TextComponentTranslation("warpdrive.machine.is_enabled.get.disabled")) );
				}
			}
			return true;
			
		} else if ( !world.isRemote
		         && itemStackHeld.getItem() == Items.DIAMOND
		         && entityPlayer.isCreative() ) {// diamond on an block to set debug values
			tileEntityAbstractBase.setDebugValues();
			Commons.addChatMessage(entityPlayer, tileEntityAbstractBase.getStatus());
			return true;
		}
		
		return false;
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
