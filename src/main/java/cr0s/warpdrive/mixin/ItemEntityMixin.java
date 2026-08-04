package cr0s.warpdrive.mixin;

import cr0s.warpdrive.event.GravityHandler;
import net.minecraft.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Item gravity, done the way 1.12.2 did it.
 *
 * The original used a CoreMod to rewrite two constants inside the vanilla item tick: the -0.04
 * gravity and the 0.98 vertical drag (GravityManager.getItemGravity / getItemGravity2). CoreMods
 * are gone in 1.16.5, and Forge exposes no per-entity tick event for non-living entities, so Mixin
 * is the direct equivalent - Forge already bundles the subsystem, nothing extra is installed.
 *
 * Why in-place substitution rather than correcting the motion from an event afterwards:
 * ItemEntity.tick modifies vertical velocity in three conditional places, and an external fix has
 * to reproduce all of them exactly or drift.
 *
 *   1. Gravity applies only outside water and lava - it is the final branch of an else-if chain.
 *   2. move() and the drag are skipped entirely unless
 *      !onGround || horizontalDistanceSqr > 1e-5 || (tickCount + id) % 4 == 0,
 *      so a resting item skips both on three ticks out of four.
 *   3. On landing, vertical velocity is inverted and halved - items bounce.
 *
 * Replacing the constants where they sit means all three keep working untouched, and it runs at the
 * correct point on both client and server, so the two cannot disagree.
 *
 * Targeting is unambiguous: within tick(), -0.04D occurs once, and the vertical drag is the only
 * 0.98D - the horizontal friction beside it is 0.98F, a float, which doubleValue does not match.
 */
@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {

	/** Vanilla adds (0, -0.04, 0), so the sign is flipped back here. */
	@ModifyConstant(method = "tick", constant = @Constant(doubleValue = -0.04D))
	private double warpdrive$itemGravity(final double original) {
		return -GravityHandler.getItemGravity((ItemEntity) (Object) this);
	}

	@ModifyConstant(method = "tick", constant = @Constant(doubleValue = 0.98D))
	private double warpdrive$itemVerticalDrag(final double original) {
		return GravityHandler.getItemDrag((ItemEntity) (Object) this);
	}
}
