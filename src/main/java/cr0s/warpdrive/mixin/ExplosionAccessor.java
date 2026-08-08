package cr0s.warpdrive.mixin;

import net.minecraft.world.Explosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Exposes the explosion radius needed by the legacy force-field damage formula. */
@Mixin(Explosion.class)
public interface ExplosionAccessor {

	@Accessor("radius")
	float warpdrive$getRadius();
}
