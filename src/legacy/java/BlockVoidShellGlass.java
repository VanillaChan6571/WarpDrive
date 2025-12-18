package cr0s.warpdrive.block.atomic;

import cr0s.warpdrive.data.EnumTier;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;

public class BlockVoidShellGlass extends BlockVoidShellPlain {
	
	public BlockVoidShellGlass(final String registryName, final EnumTier enumTier) {
		super(registryName, enumTier);
		
		setTranslationKey("warpdrive.atomic.void_shell_glass");
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean isOpaqueCube(@Nonnull final IBlockState blockState) {
		return false;
	}
	
	@Nonnull
	@SideOnly(Side.CLIENT)
	@Override
	public BlockRenderLayer getRenderLayer() {
		return BlockRenderLayer.TRANSLUCENT;
	}
	
	@SuppressWarnings("deprecation")
	@SideOnly(Side.CLIENT)
	@Override
	public boolean shouldSideBeRendered(@Nonnull final IBlockState blockState, @Nonnull final IBlockAccess blockAccess, @Nonnull final BlockPos blockPos, @Nonnull final EnumFacing facing) {
		final IBlockState blockStateSide = blockAccess.getBlockState(blockPos);
		final Block blockSide = blockStateSide.getBlock();
		if (blockSide.isAir(blockStateSide, blockAccess, blockPos)) {
			return true;
		}
		return !(blockSide instanceof BlockElectromagnetGlass);
	}
}
