package cr0s.warpdrive.item;

import cr0s.warpdrive.block.atomic.AcceleratorTier;
import cr0s.warpdrive.data.ParticleType;

import javax.annotation.Nullable;
import java.util.Locale;

/** Pure storage rules kept outside Minecraft's registry-bound Item classes for deterministic tests. */
public final class PlasmaTorchStorage {

	private static final int[] CAPACITIES = { 200, 400, 800 };

	private PlasmaTorchStorage() { }

	public static int capacity(final AcceleratorTier tier) {
		return CAPACITIES[tier.getIndex() - 1];
	}

	public static int clampAmount(final int requestedAmount, final int capacity) {
		return Math.max(0, Math.min(capacity, requestedAmount));
	}

	public static int fillTransfer(@Nullable final ParticleType storedType, final int storedAmount,
	                               final ParticleType resourceType, final int requestedAmount,
	                               final int capacity) {
		if (resourceType == null || requestedAmount <= 0
		 || storedType != null && storedType != resourceType) return 0;
		return Math.min(requestedAmount, capacity - clampAmount(storedAmount, capacity));
	}

	public static int drainTransfer(@Nullable final ParticleType storedType, final int storedAmount,
	                                final ParticleType requestedType, final int requestedAmount,
	                                final int capacity) {
		if (requestedType == null || storedType != requestedType || requestedAmount <= 0) return 0;
		return Math.min(requestedAmount, clampAmount(storedAmount, capacity));
	}

	public static int remainingAfterConsumption(final int storedAmount,
	                                            final int requestedConsumption,
	                                            final int capacity) {
		return clampAmount(storedAmount - Math.max(0, requestedConsumption), capacity);
	}

	@Nullable
	public static ParticleType parseParticleName(@Nullable final String name) {
		if (name == null || name.isEmpty()) return null;
		try {
			return ParticleType.valueOf(name.toUpperCase(Locale.ROOT));
		} catch (final IllegalArgumentException ignored) {
			return null;
		}
	}
}
