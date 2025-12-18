package cr0s.warpdrive.data;

import cr0s.warpdrive.api.IVideoChannel;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class CameraRegistryItem {
	
	public int dimensionId;
	public BlockPos blockPos;
	public int videoChannel;
	public EnumCameraType type;
	
	public CameraRegistryItem(final World world, final BlockPos blockPos, final int videoChannel, final EnumCameraType enumCameraType) {
		this.videoChannel = videoChannel;
		this.blockPos = blockPos;
		this.dimensionId = world.provider.getDimension();
		this.type = enumCameraType;
	}
	
	public boolean isTileEntity(final TileEntity tileEntity) {
		return tileEntity instanceof IVideoChannel
			&& dimensionId == tileEntity.getWorld().provider.getDimension()
			&& blockPos.equals(tileEntity.getPos())
			&& videoChannel == ((IVideoChannel) tileEntity).getVideoChannel();
	}
}