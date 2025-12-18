package cr0s.warpdrive.api;

import cr0s.warpdrive.api.computer.ICoreSignature;
import cr0s.warpdrive.data.EnumGlobalRegionType;

import javax.annotation.Nullable;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

public interface IGlobalRegionProvider extends ICoreSignature {
	
	// providers are TileEntities, so we reuse the base class methods
	default int getDimension() {
		assert this instanceof TileEntity;
		return ((TileEntity) this).getWorld().provider.getDimension();
	}
	default BlockPos getBlockPos() {
		assert this instanceof TileEntity;
		return ((TileEntity) this).getPos();
	}
	
	// get the registry type
	EnumGlobalRegionType getGlobalRegionType();
	
	// get the area controlled by this tile entity 
	AxisAlignedBB getGlobalRegionArea();
	
	// mass of the multi-block
	int getMass();
	
	// isolation rate from radars
	double getIsolationRate();
	
	// report an update in the area, return false to cancel it
	boolean onBlockUpdatingInArea(@Nullable final Entity entity, final BlockPos blockPos, final IBlockState blockState);
}
