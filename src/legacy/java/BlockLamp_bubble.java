package cr0s.warpdrive.block.decoration;

import cr0s.warpdrive.data.BlockProperties;
import cr0s.warpdrive.data.EnumTier;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

import javax.annotation.Nonnull;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class BlockLamp_bubble extends BlockAbstractLamp {
	
	private static final AxisAlignedBB AABB_DOWN  = new AxisAlignedBB(0.00D, 0.30D, 0.00D, 1.00D, 1.00D, 1.00D);
	private static final AxisAlignedBB AABB_UP    = new AxisAlignedBB(0.00D, 0.00D, 0.00D, 1.00D, 0.70D, 1.00D);
	private static final AxisAlignedBB AABB_NORTH = new AxisAlignedBB(0.00D, 0.00D, 0.30D, 1.00D, 1.00D, 1.00D);
	private static final AxisAlignedBB AABB_SOUTH = new AxisAlignedBB(0.00D, 0.00D, 0.00D, 1.00D, 1.00D, 0.70D);
	private static final AxisAlignedBB AABB_WEST  = new AxisAlignedBB(0.30D, 0.00D, 0.00D, 1.00D, 1.00D, 1.00D);
	private static final AxisAlignedBB AABB_EAST  = new AxisAlignedBB(0.00D, 0.00D, 0.00D, 0.70D, 1.00D, 1.00D);
	
	public BlockLamp_bubble(final String registryName, final EnumTier enumTier) {
		super(registryName, enumTier, "warpdrive.decoration.lamp_bubble");
	}
	
	@Nonnull
	@SuppressWarnings("deprecation")
	@Override
	public AxisAlignedBB getBoundingBox(@Nonnull final IBlockState blockState, @Nonnull final IBlockAccess blockAccess, @Nonnull final BlockPos blockPos) {
		switch (blockState.getValue(BlockProperties.FACING)) {
			case DOWN : return AABB_DOWN ;
			case UP   : return AABB_UP   ;
			case NORTH: return AABB_NORTH;
			case SOUTH: return AABB_SOUTH;
			case WEST : return AABB_WEST ;
			case EAST : return AABB_EAST ;
			default   : return AABB_UP;
		}
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean isTranslucent(@Nonnull final IBlockState blockState) {
		return true;
	}
	
	@Nonnull
	@SideOnly(Side.CLIENT)
	@Override
	public BlockRenderLayer getRenderLayer() {
		return BlockRenderLayer.TRANSLUCENT;
	}
}