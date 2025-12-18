package cr0s.warpdrive.api;

import javax.annotation.Nonnull;

import net.minecraft.util.math.BlockPos;

public interface IBlockUpdateDetector {
	void onBlockUpdateDetected(@Nonnull final BlockPos blockPosUpdated);
}
