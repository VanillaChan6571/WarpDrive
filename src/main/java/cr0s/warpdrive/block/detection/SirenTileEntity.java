package cr0s.warpdrive.block.detection;

import cr0s.warpdrive.client.SirenSoundController;
import cr0s.warpdrive.data.Registration;
import net.minecraft.block.BlockState;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

/** Client-side lifecycle for the siren's long-range looping sound. */
public class SirenTileEntity extends TileEntity implements ITickableTileEntity {

	private int updateTicks;

	public SirenTileEntity() {
		super(Registration.SIREN_TILE.get());
	}

	@Override
	public void tick() {
		if (level == null || !level.isClientSide || --updateTicks > 0) {
			return;
		}
		updateTicks = 10;
		DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
			() -> () -> SirenSoundController.update(this));
	}

	public boolean isPowered() {
		final BlockState blockState = getBlockState();
		return blockState.getBlock() instanceof SirenBlock && blockState.getValue(SirenBlock.POWERED);
	}

	public SirenStyle getStyle() {
		final BlockState blockState = getBlockState();
		return blockState.getBlock() instanceof SirenBlock
		     ? ((SirenBlock) blockState.getBlock()).getStyle() : SirenStyle.INDUSTRIAL;
	}

	public float getRange() {
		final BlockState blockState = getBlockState();
		return blockState.getBlock() instanceof SirenBlock
		     ? ((SirenBlock) blockState.getBlock()).getTier().getRange() : 32.0F;
	}

	@Override
	public void onChunkUnloaded() {
		if (level != null && level.isClientSide) {
			DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
				() -> () -> SirenSoundController.stop(this));
		}
		super.onChunkUnloaded();
	}

	@Override
	public void setRemoved() {
		if (level != null && level.isClientSide) {
			DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
				() -> () -> SirenSoundController.stop(this));
		}
		super.setRemoved();
	}
}
