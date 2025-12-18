package cr0s.warpdrive.block.atomic;

import cr0s.warpdrive.api.IBlockBase;
import cr0s.warpdrive.block.BlockAbstractBase;
import cr0s.warpdrive.data.EnumTier;

import javax.annotation.Nonnull;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLiving.SpawnPlacementType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

public abstract class BlockAbstractAccelerator extends BlockAbstractBase implements IBlockBase {
	
	public final EnumTier enumTier;
	
	BlockAbstractAccelerator(final String registryName, final EnumTier enumTier) {
		super(registryName, enumTier, Material.IRON);
		
		this.enumTier = enumTier;
		setHardness(4 + enumTier.getIndex());
		setResistance((2 + 2 * enumTier.getIndex()) * 5 / 3);
	}
	
	@Override
	public boolean canCreatureSpawn(@Nonnull final IBlockState blockState, @Nonnull final IBlockAccess blockAccess, @Nonnull final BlockPos blockPos, final SpawnPlacementType type) {
		return false;
	}
}
