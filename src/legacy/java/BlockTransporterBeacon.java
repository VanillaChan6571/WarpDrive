package cr0s.warpdrive.block.movement;

import cr0s.warpdrive.Commons;

import cr0s.warpdrive.block.BlockAbstractContainer;
import cr0s.warpdrive.data.BlockProperties;
import cr0s.warpdrive.data.EnumTier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class BlockTransporterBeacon extends BlockAbstractContainer {
	
	private static final double BOUNDING_RADIUS = 3.0D / 32.0D;
	private static final double BOUNDING_HEIGHT = 21.0D / 32.0D;
	private static final AxisAlignedBB AABB_BEACON = new AxisAlignedBB(0.5D - BOUNDING_RADIUS, 0.0D, 0.5D - BOUNDING_RADIUS,
	                                                                   0.5D + BOUNDING_RADIUS, BOUNDING_HEIGHT, 0.5D + BOUNDING_RADIUS);
	
	public static final PropertyBool DEPLOYED = PropertyBool.create("deployed");
	
	public BlockTransporterBeacon(final String registryName, final EnumTier enumTier) {
		super(registryName, enumTier, Material.IRON);
		
		setHardness(0.5F);
		setTranslationKey("warpdrive.movement.transporter_beacon");
		
		setDefaultState(getDefaultState()
				                .withProperty(BlockProperties.ACTIVE, false)
				                .withProperty(DEPLOYED, false)
		               );
	}
	
	@Nullable
	@Override
	public ItemBlock createItemBlock() {
		return new ItemBlockTransporterBeacon(this);
	}
	
	@Nonnull
	@Override
	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, BlockProperties.ACTIVE, DEPLOYED);
	}
	
	@SuppressWarnings("deprecation")
	@Nonnull
	@Override
	public IBlockState getStateFromMeta(final int metadata) {
		return getDefaultState()
				       .withProperty(BlockProperties.ACTIVE, (metadata & 0x2) != 0)
				       .withProperty(DEPLOYED, (metadata & 0x1) != 0);
	}
	
	@Override
	public int getMetaFromState(@Nonnull final IBlockState blockState) {
		return (blockState.getValue(BlockProperties.ACTIVE) ? 2 : 0)
		     + (blockState.getValue(DEPLOYED) ? 1 : 0);
	}
	
	@SideOnly(Side.CLIENT)
	@Override
	public void modelInitialisation() {
		super.modelInitialisation();
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean isOpaqueCube(@Nonnull final IBlockState blockState) {
		return false;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean isFullCube(@Nonnull final IBlockState blockState) {
		return false;
	}
	
	@SuppressWarnings("deprecation")
	@Nonnull
	@Override
	public BlockFaceShape getBlockFaceShape(@Nonnull final IBlockAccess blockAccess, @Nonnull final IBlockState blockState, @Nonnull final BlockPos blockPos, @Nonnull final EnumFacing enumFacing) {
		return enumFacing == EnumFacing.DOWN ? BlockFaceShape.CENTER : BlockFaceShape.UNDEFINED;
	}
	
	@Nonnull
	@SideOnly(Side.CLIENT)
	@Override
	public BlockRenderLayer getRenderLayer() {
		return BlockRenderLayer.TRANSLUCENT;
	}
	
	@SuppressWarnings("deprecation")
	@Nonnull
	@Override
	public AxisAlignedBB getBoundingBox(@Nonnull final IBlockState blockState, @Nonnull final IBlockAccess blockAccess, @Nonnull final BlockPos blockPos) {
		return AABB_BEACON;
	}
	
	@SuppressWarnings("deprecation")
	@Nullable
	@Override
	public AxisAlignedBB getCollisionBoundingBox(@Nonnull final IBlockState blockState, @Nonnull final IBlockAccess blockAccess, @Nonnull final BlockPos blockPos) {
		return NULL_AABB;
	}
	
	@Override
	public int getLightValue(@Nonnull final IBlockState blockState, @Nonnull final IBlockAccess blockAccess, @Nonnull final BlockPos blockPos) {
		final boolean isActive = blockState.getValue(BlockProperties.ACTIVE);
		return isActive ? 6 : 0;
	}
	
	@Nonnull
	@Override
	public TileEntity createNewTileEntity(@Nonnull final World world, final int metadata) {
		return new TileEntityTransporterBeacon();
	}
	
	@Override
	public boolean onBlockActivated(@Nonnull final World world, @Nonnull final BlockPos blockPos, @Nonnull final IBlockState blockState,
	                                @Nonnull final EntityPlayer entityPlayer, @Nonnull final EnumHand enumHand,
	                                @Nonnull final EnumFacing enumFacing, final float hitX, final float hitY, final float hitZ) {
		if (world.isRemote) {
			return super.onBlockActivated(world, blockPos, blockState, entityPlayer, enumHand, enumFacing, hitX, hitY, hitZ);
		}
		
		if (enumHand != EnumHand.MAIN_HAND) {
			return super.onBlockActivated(world, blockPos, blockState, entityPlayer, enumHand, enumFacing, hitX, hitY, hitZ);
		}
		
		// get context
		final ItemStack itemStackHeld = entityPlayer.getHeldItem(enumHand);
		final TileEntity tileEntity = world.getTileEntity(blockPos);
		if (!(tileEntity instanceof TileEntityTransporterBeacon)) {
			return super.onBlockActivated(world, blockPos, blockState, entityPlayer, enumHand, enumFacing, hitX, hitY, hitZ);
		}
		final TileEntityTransporterBeacon tileEntityTransporterBeacon = (TileEntityTransporterBeacon) tileEntity;
		
		if (itemStackHeld.isEmpty()) {
			if (!entityPlayer.isSneaking()) {// non-sneaking with an empty hand
				Commons.addChatMessage(entityPlayer, Commons.getChatPrefix(this)
				                                            .appendSibling(new TextComponentString(tileEntityTransporterBeacon.stateTransporter)));
				return true;
				
			} else {// sneaking with an empty hand
				final boolean isEnabledOld = tileEntityTransporterBeacon.getIsEnabled();
				tileEntityTransporterBeacon.setIsEnabled(!isEnabledOld);
				final boolean isEnabledNew = tileEntityTransporterBeacon.getIsEnabled();
				if (isEnabledOld != isEnabledNew) {
					if (isEnabledNew) {
						Commons.addChatMessage(entityPlayer, Commons.getChatPrefix(this)
						                                            .appendSibling(new TextComponentTranslation("warpdrive.machine.is_enabled.set.enabled")) );
					} else {
						Commons.addChatMessage(entityPlayer, Commons.getChatPrefix(this)
						                                            .appendSibling(new TextComponentTranslation("warpdrive.machine.is_enabled.set.disabled")) );
					}
				}
				return true;
			}
		}
		
		return super.onBlockActivated(world, blockPos, blockState, entityPlayer, enumHand, enumFacing, hitX, hitY, hitZ);
	}
}
