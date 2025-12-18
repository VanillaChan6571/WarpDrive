package cr0s.warpdrive.block.forcefield;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.data.BlockProperties;
import cr0s.warpdrive.data.EnumForceFieldUpgrade;
import cr0s.warpdrive.data.EnumTier;
import cr0s.warpdrive.item.ItemForceFieldUpgrade;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class BlockForceFieldRelay extends BlockAbstractForceField {
	
	public static final PropertyEnum<EnumForceFieldUpgrade> UPGRADE = PropertyEnum.create("upgrade", EnumForceFieldUpgrade.class);
	
	private static final AxisAlignedBB AABB_RELAY = new AxisAlignedBB(0.000D, 0.000D, 0.000D, 1.000D, 0.625D, 1.000D);
	
	public BlockForceFieldRelay(final String registryName, final EnumTier enumTier) {
		super(registryName, enumTier, Material.IRON);
		
		setTranslationKey("warpdrive.force_field.relay." + enumTier.getName());
		
		setDefaultState(getDefaultState()
				                .withProperty(BlockProperties.ACTIVE, false)
				                .withProperty(BlockProperties.FACING_HORIZONTAL, EnumFacing.NORTH)
				                .withProperty(UPGRADE, EnumForceFieldUpgrade.NONE)
		               );
	}
	
	@Nonnull
	@Override
	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, BlockProperties.ACTIVE, BlockProperties.FACING_HORIZONTAL, UPGRADE);
	}
	
	@SuppressWarnings("deprecation")
	@Nonnull
	@Override
	public IBlockState getStateFromMeta(final int metadata) {
		return getDefaultState()
				       .withProperty(BlockProperties.ACTIVE, (metadata & 0x8) != 0)
				       .withProperty(BlockProperties.FACING_HORIZONTAL, EnumFacing.byIndex(Commons.clamp(2, 5, metadata & 0x7)));
	}
	
	@Override
	public int getMetaFromState(@Nonnull final IBlockState blockState) {
		return (blockState.getValue(BlockProperties.ACTIVE) ? 0x8 : 0x0)
		     | (blockState.getValue(BlockProperties.FACING_HORIZONTAL).getIndex() & 0x7);
	}
	
	@SuppressWarnings("deprecation")
	@Nonnull
	@Override
	public IBlockState getActualState(@Nonnull final IBlockState blockState, @Nonnull final IBlockAccess blockAccess, @Nonnull final BlockPos blockPos) {
		final TileEntity tileEntity = blockAccess.getTileEntity(blockPos);
		if (tileEntity instanceof TileEntityForceFieldRelay) {
			return blockState
					       .withProperty(BlockProperties.ACTIVE, ((TileEntityForceFieldRelay) tileEntity).isConnected)
					       .withProperty(UPGRADE, ((TileEntityForceFieldRelay) tileEntity).getUpgrade());
		} else {
			return blockState;
		}
	}
	
	@Nullable
	@Override
	public ItemBlock createItemBlock() {
		return new ItemBlockForceFieldRelay(this);
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public int getLightOpacity(@Nonnull final IBlockState blockState) {
		return 255;
	}
	
	@SuppressWarnings("deprecation")
	@Nonnull
	@Override
	public AxisAlignedBB getBoundingBox(@Nonnull final IBlockState blockState, @Nonnull final IBlockAccess blockAccess, @Nonnull final BlockPos blockPos) {
		return AABB_RELAY;
	}
	
	@SuppressWarnings("deprecation")
	@Nullable
	@Override
	public AxisAlignedBB getCollisionBoundingBox(@Nonnull final IBlockState blockState, @Nonnull final IBlockAccess blockAccess, @Nonnull final BlockPos blockPos) {
		return AABB_RELAY;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean isTopSolid(@Nonnull final IBlockState blockState) {
		return false;
	}
	
	@SuppressWarnings("deprecation")
	@Nonnull
	@Override
	public BlockFaceShape getBlockFaceShape(@Nonnull final IBlockAccess blockAccess, @Nonnull final IBlockState blockState, @Nonnull final BlockPos blockPos, @Nonnull final EnumFacing facing) {
		if (facing == EnumFacing.DOWN) {
			return BlockFaceShape.SOLID;
			
		} else if (facing == EnumFacing.UP) {
			// getActualState wasn't called, so the upgrade needs to be retrieved from the tile entity itself
			final TileEntity tileEntity = blockAccess.getTileEntity(blockPos);
			if (tileEntity instanceof TileEntityForceFieldRelay) {
				final EnumForceFieldUpgrade enumForceFieldUpgrade = ((TileEntityForceFieldRelay) tileEntity).getUpgrade();
				if (enumForceFieldUpgrade == EnumForceFieldUpgrade.CAMOUFLAGE) {
					return BlockFaceShape.SOLID;
				}
			}
		}
		
		return BlockFaceShape.UNDEFINED;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean isFullBlock(@Nonnull final IBlockState blockState) {
		return false;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean isFullCube(@Nonnull final IBlockState blockState) {
		return false;
	}
	
	@Nonnull
	@Override
	public TileEntity createNewTileEntity(@Nonnull final World world, final int metadata) {
		return new TileEntityForceFieldRelay();
	}
	
	@SuppressWarnings("deprecation")
	@SideOnly(Side.CLIENT)
	@Override
	public boolean shouldSideBeRendered(@Nonnull final IBlockState blockState, @Nonnull final IBlockAccess blockAccess, @Nonnull final BlockPos blockPos, @Nonnull final EnumFacing facing) {
		final BlockPos blockPosSide = blockPos.offset(facing);
		final boolean doesSideBlockRendering = blockAccess.getBlockState(blockPosSide).doesSideBlockRendering(blockAccess, blockPosSide, facing.getOpposite());
		if (facing != EnumFacing.DOWN) {
			return !doesSideBlockRendering;
		}
		return true;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean isOpaqueCube(@Nonnull final IBlockState blockState) {
		return true;
	}
	
	@Override
	public boolean doesSideBlockRendering(@Nonnull final IBlockState blockState, @Nonnull final IBlockAccess blockAccess, @Nonnull final BlockPos blockPos, @Nonnull final EnumFacing side) {
		return side == EnumFacing.DOWN;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean isSideSolid(@Nonnull final IBlockState blockState, @Nonnull final IBlockAccess blockAccess, @Nonnull final BlockPos blockPos, @Nonnull final EnumFacing side) {
		return side == EnumFacing.DOWN;
	}
	
	@Override
	public boolean onBlockActivated(@Nonnull final World world, @Nonnull final BlockPos blockPos, @Nonnull final IBlockState blockState,
	                                @Nonnull final EntityPlayer entityPlayer, @Nonnull final EnumHand enumHand,
	                                @Nonnull final EnumFacing enumFacing, final float hitX, final float hitY, final float hitZ) {
		if (world.isRemote) {
			return super.onBlockActivated(world, blockPos, blockState, entityPlayer, enumHand, enumFacing, hitX, hitY, hitZ);
		}
		
		if (enumHand != EnumHand.MAIN_HAND) {
			return true;
		}
		
		// get context
		final ItemStack itemStackHeld = entityPlayer.getHeldItem(enumHand);
		final TileEntity tileEntity = world.getTileEntity(blockPos);
		if (!(tileEntity instanceof TileEntityForceFieldRelay)) {
			return super.onBlockActivated(world, blockPos, blockState, entityPlayer, enumHand, enumFacing, hitX, hitY, hitZ);
		}
		final TileEntityForceFieldRelay tileEntityForceFieldRelay = (TileEntityForceFieldRelay) tileEntity;
		
		// sneaking with an empty hand or an upgrade item in hand to dismount current upgrade
		if (entityPlayer.isSneaking()) {
			final EnumForceFieldUpgrade enumForceFieldUpgrade = tileEntityForceFieldRelay.getUpgrade();
			if (enumForceFieldUpgrade != EnumForceFieldUpgrade.NONE) {
				if (!entityPlayer.capabilities.isCreativeMode) {
					// dismount the upgrade item
					final ItemStack itemStackDrop = ItemForceFieldUpgrade.getItemStackNoCache(enumForceFieldUpgrade, 1);
					final EntityItem entityItem = new EntityItem(world, entityPlayer.posX, entityPlayer.posY + 0.5D, entityPlayer.posZ, itemStackDrop);
					entityItem.setNoPickupDelay();
					final boolean isSuccess = world.spawnEntity(entityItem);
					if (!isSuccess) {
						Commons.addChatMessage(entityPlayer, new WarpDriveText(Commons.getStyleWarning(), "warpdrive.upgrade.result.spawn_denied",
						                                                       entityItem ));
						return true;
					}
				}
				
				tileEntityForceFieldRelay.setUpgrade(EnumForceFieldUpgrade.NONE);
				// upgrade dismounted
				Commons.addChatMessage(entityPlayer, new WarpDriveText(Commons.getStyleCorrect(), "warpdrive.upgrade.result.dismounted",
				                                                       new TextComponentTranslation(enumForceFieldUpgrade.name()) ));
				return true;
				
			} else {
				// no more upgrades to dismount
				Commons.addChatMessage(entityPlayer, new WarpDriveText(Commons.getStyleWarning(), "warpdrive.upgrade.result.no_upgrade_to_dismount"));
				return true;
			}
			
		} else if (itemStackHeld.isEmpty()) {// no sneaking and no item in hand to show status
			Commons.addChatMessage(entityPlayer, tileEntityForceFieldRelay.getStatus());
			return true;
			
		} else if (itemStackHeld.getItem() instanceof ItemForceFieldUpgrade) {
			// validate type
			if (EnumForceFieldUpgrade.get(itemStackHeld.getItemDamage()).maxCountOnRelay <= 0) {
				// invalid upgrade type
				Commons.addChatMessage(entityPlayer, new WarpDriveText(Commons.getStyleWarning(), "warpdrive.upgrade.result.invalid_upgrade_for_relay"));
				return true;
			}
			
			if (!entityPlayer.capabilities.isCreativeMode) {
				// validate quantity
				if (itemStackHeld.getCount() < 1) {
					// not enough upgrade items
					Commons.addChatMessage(entityPlayer, new WarpDriveText(Commons.getStyleWarning(), "warpdrive.upgrade.result.not_enough_upgrades"));
					return true;
				}
				
				// update player inventory
				itemStackHeld.shrink(1);
				
				// dismount the current upgrade item
				if (tileEntityForceFieldRelay.getUpgrade() != EnumForceFieldUpgrade.NONE) {
					final ItemStack itemStackDrop = ItemForceFieldUpgrade.getItemStackNoCache(tileEntityForceFieldRelay.getUpgrade(), 1);
					final EntityItem entityItem = new EntityItem(world, entityPlayer.posX, entityPlayer.posY + 0.5D, entityPlayer.posZ, itemStackDrop);
					entityItem.setNoPickupDelay();
					final boolean isSuccess = world.spawnEntity(entityItem);
					if (!isSuccess) {
						Commons.addChatMessage(entityPlayer, new WarpDriveText(Commons.getStyleWarning(), "warpdrive.upgrade.result.spawn_denied",
						                                                       entityItem ));
						return true;
					}
				}
			}
			
			// mount the new upgrade item
			final EnumForceFieldUpgrade enumForceFieldUpgrade = EnumForceFieldUpgrade.get(itemStackHeld.getItemDamage());
			tileEntityForceFieldRelay.setUpgrade(enumForceFieldUpgrade);
			// upgrade mounted
			Commons.addChatMessage(entityPlayer, new WarpDriveText(Commons.getStyleCorrect(), "warpdrive.upgrade.result.mounted",
			                                                       new TextComponentTranslation(enumForceFieldUpgrade.name()) ));
			return true;
		}
		
		return super.onBlockActivated(world, blockPos, blockState, entityPlayer, enumHand, enumFacing, hitX, hitY, hitZ);
	}
}
