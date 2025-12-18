package cr0s.warpdrive.block.breathing;

import cr0s.warpdrive.block.BlockAbstractOmnipanel;
import cr0s.warpdrive.data.EnumTier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.List;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockAirShield extends BlockAbstractOmnipanel {
	
	public BlockAirShield(final String registryName, final EnumTier enumTier) {
		super(registryName, enumTier, Material.CLOTH);
		
		setTranslationKey("warpdrive.breathing.air_shield");
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean causesSuffocation(@Nonnull final IBlockState blockState) {
		return false;
	}
	
	@Override
	public void addCollisionBoxToList(final IBlockState blockState, final @Nonnull World world, final @Nonnull BlockPos blockPos,
	                                  final @Nonnull AxisAlignedBB entityBox, final @Nonnull List<AxisAlignedBB> collidingBoxes,
	                                  final @Nullable Entity entity, final boolean isActualState) {
	}
	
	@SuppressWarnings("deprecation")
	@Nullable
	@Override
	public AxisAlignedBB getCollisionBoundingBox(@Nonnull final IBlockState blockState, @Nonnull final IBlockAccess blockAccess, @Nonnull final BlockPos blockPos) {
		return NULL_AABB;
	}
	
	@Override
	public boolean canCollideCheck(@Nonnull final IBlockState blockState, final boolean hitIfLiquid) {
		return !hitIfLiquid;
	}
	
	@Override
	public boolean isCollidable() {
		return false;
	}
}