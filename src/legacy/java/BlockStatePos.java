package cr0s.warpdrive.data;

import javax.annotation.Nonnull;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;

public class BlockStatePos implements Comparable<BlockStatePos> {
	
	public final BlockPos blockPos;
	public final IBlockState blockState;
	
	public BlockStatePos(@Nonnull final BlockPos blockPos, @Nonnull final IBlockState blockState) {
		this.blockPos = blockPos.toImmutable();
		this.blockState = blockState;
	}
	
	@Override
	public boolean equals(final Object objectOther) {
		if (this == objectOther) {
			return true;
		} else if (!(objectOther instanceof BlockStatePos)) {
			return false;
		} else {
			final BlockStatePos other = (BlockStatePos) objectOther;
			
			if (this.blockPos.getX() != other.blockPos.getX()) {
				return false;
			} else if (this.blockPos.getY() != other.blockPos.getY()) {
				return false;
			} else {
				return this.blockPos.getZ() == other.blockPos.getZ();
			}
		}
	}
	
	@Override
	public int hashCode() {
		return blockPos.hashCode();
	}
	
	@Override
	public int compareTo(@Nonnull final BlockStatePos other) {
		if (this.blockPos.getY() == other.blockPos.getY()) {
			return this.blockPos.getZ() == other.blockPos.getZ() ? this.blockPos.getX() - other.blockPos.getX()
			                                                     : this.blockPos.getZ() - other.blockPos.getZ();
		} else {
			return this.blockPos.getY() - other.blockPos.getY();
		}
	}
}
