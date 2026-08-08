package cr0s.warpdrive.mixin;

import cr0s.warpdrive.client.ClientCloakManager;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Replaces the render-side view of blocks without corrupting the received client chunk data. */
@Mixin(Chunk.class)
public abstract class ChunkCloakMixin {

	@Inject(method = "getBlockState", at = @At("RETURN"), cancellable = true)
	private void warpdrive$cloakBlockState(final BlockPos position,
	                                      final CallbackInfoReturnable<BlockState> callback) {
		callback.setReturnValue(ClientCloakManager.maskBlockState(position,
			callback.getReturnValue()));
	}
}
