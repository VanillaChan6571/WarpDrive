package cr0s.warpdrive.block.decoration;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.block.BlockAbstractBase;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.BlockProperties;
import cr0s.warpdrive.data.EnumTier;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class BlockAbstractLamp extends BlockAbstractBase {
	
	BlockAbstractLamp(final String registryName, final EnumTier enumTier, final String unlocalizedName) {
		super(registryName, enumTier, Material.ROCK);
		
		setHardness(WarpDriveConfig.HULL_HARDNESS[1]);
		setResistance(WarpDriveConfig.HULL_BLAST_RESISTANCE[1] * 5.0F / 3.0F);
		setSoundType(SoundType.METAL);
		setTranslationKey(unlocalizedName);
		setDefaultState(getDefaultState()
				                .withProperty(BlockProperties.ACTIVE, false)
				                .withProperty(BlockProperties.FACING, EnumFacing.DOWN)
		               );
	}
	
	@Nonnull
	@Override
	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, BlockProperties.ACTIVE, BlockProperties.FACING);
	}
	
	@SuppressWarnings("deprecation")
	@Nonnull
	@Override
	public IBlockState getStateFromMeta(final int metadata) {
		return getDefaultState()
				       .withProperty(BlockProperties.ACTIVE, (metadata & 0x8) != 0)
				       .withProperty(BlockProperties.FACING, EnumFacing.byIndex(metadata & 0x7));
	}
	
	@Override
	public int getMetaFromState(@Nonnull final IBlockState blockState) {
		return (blockState.getValue(BlockProperties.ACTIVE) ? 0x8 : 0x0)
		     | (blockState.getValue(BlockProperties.FACING).getIndex());
	}
	
	@Nullable
	@Override
	public ItemBlock createItemBlock() {
		return new ItemBlockLamp(this);
	}
	
	@SuppressWarnings("deprecation")
	@SideOnly(Side.CLIENT)
	@Override
	public boolean shouldSideBeRendered(@Nonnull final IBlockState blockState, @Nonnull final IBlockAccess blockAccess, @Nonnull final BlockPos blockPos, @Nonnull final EnumFacing facing) {
		return true;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean isBlockNormalCube(@Nonnull final IBlockState blockState) {
		return false;
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
	@Override
	public int getLightValue(@Nonnull final IBlockState blockState) {
		if (blockState.getValue(BlockProperties.ACTIVE)) {
			return 14;
		} else {
			return 0;
		}
	}
	
	@SuppressWarnings("deprecation")
	@Nullable
	@Override
	public AxisAlignedBB getCollisionBoundingBox(@Nonnull final IBlockState blockState, @Nonnull final IBlockAccess blockAccess, @Nonnull final BlockPos blockPos) {
		return NULL_AABB;
	}
	
	@Nonnull
	@SuppressWarnings("deprecation")
	@Override
	public IBlockState withRotation(@Nonnull final IBlockState blockState, final Rotation rot) {
		return blockState.withProperty(BlockProperties.FACING, rot.rotate(blockState.getValue(BlockProperties.FACING)));
	}
	
	@Nonnull
	@SuppressWarnings("deprecation")
	@Override
	public IBlockState withMirror(@Nonnull final IBlockState blockState, final Mirror mirrorIn) {
		return blockState.withRotation(mirrorIn.toRotation(blockState.getValue(BlockProperties.FACING)));
	}
	
	@Override
	public boolean canPlaceBlockOnSide(@Nonnull final World world, @Nonnull final BlockPos blockPos, final EnumFacing enumFacing) {
		// (do not call ancestor)
		
		final IBlockState blockState = world.getBlockState(blockPos.offset(enumFacing.getOpposite()));
		return blockState.isSideSolid(world, blockPos, enumFacing);
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public void neighborChanged(@Nonnull final IBlockState blockState, @Nonnull final World world, @Nonnull final BlockPos blockPos,
	                            @Nonnull final Block block, @Nonnull final BlockPos blockPosFrom) {
		// check if we are still attached
		if (canPlaceBlockOnSide(world, blockPos, blockState.getValue(BlockProperties.FACING))) {
			return;
		}
		
		// find a new attachment
		for (final EnumFacing enumFacing : EnumFacing.values()) {
			if (canPlaceBlockOnSide(world, blockPos, enumFacing)) {
				// new attachment found => apply
				world.setBlockState(blockPos, blockState.withProperty(BlockProperties.FACING, enumFacing));
				return;
			}
		}
		
		// can't find an attachment => drop
		dropBlockAsItem(world, blockPos, blockState, 0);
		world.setBlockToAir(blockPos);
	}
	
	@Override
	public boolean onBlockActivated(@Nonnull final World world, @Nonnull final BlockPos blockPos, @Nonnull final IBlockState blockState,
	                                @Nonnull final EntityPlayer entityPlayer, @Nonnull final EnumHand enumHand,
	                                @Nonnull final EnumFacing enumFacing, final float hitX, final float hitY, final float hitZ) {
		if (enumHand != EnumHand.MAIN_HAND) {
			return true;
		}
		if (world.isRemote) {
			return true;
		}
		
		// non-sneaking to toggle lamp on/state
		if (!entityPlayer.isSneaking()) {
			final boolean isActivated = !blockState.getValue(BlockProperties.ACTIVE);
			world.setBlockState(blockPos, blockState.withProperty(BlockProperties.ACTIVE, isActivated));
			// (visual feedback only, no message to player)
			return true;
		}
		
		// (visual feedback only: no status reported while sneaking)
		
		return super.onBlockActivated(world, blockPos, blockState, entityPlayer, enumHand, enumFacing, hitX, hitY, hitZ);
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(@Nonnull final ItemStack itemStack, @Nullable final World world,
	                           @Nonnull final List<String> list, @Nonnull final ITooltipFlag advancedItemTooltips) {
		super.addInformation(itemStack, world, list, advancedItemTooltips);
		
		Commons.addTooltip(list, new TextComponentTranslation("item.warpdrive.decoration.lamp.tooltip.usage").getFormattedText());
	}
}