package cr0s.warpdrive.data;

import cr0s.warpdrive.damage.WarpDamageSources;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;

import java.util.Locale;

/** The four particle payloads registered by WarpDrive 1.12.2. */
public enum ParticleType {

	ION(200, 2.0F, 0.3F),
	PROTON(200, 4.0F, 0.5F),
	ANTIMATTER(60, 10.0F, 1.0F),
	STRANGE_MATTER(40, 14.0F, 0.8F);

	private final int entityLifespan;
	private final float radiationLevel;
	private final float explosionStrength;

	ParticleType(final int entityLifespan, final float radiationLevel,
	             final float explosionStrength) {
		this.entityLifespan = entityLifespan;
		this.radiationLevel = radiationLevel;
		this.explosionStrength = explosionStrength;
	}

	public String getName() { return name().toLowerCase(Locale.ROOT); }
	public String getTranslationKey() { return "warpdrive.particle." + getName(); }
	public int getEntityLifespan() { return entityLifespan; }

	/** Uncontained cells vent their legacy radiation and explosive effects on despawn. */
	public void affectWorld(final World world, final Vector3d position, final int amount) {
		if (world.isClientSide || amount <= 0) return;
		final float radiation = radiationLevel * amount / 1_000.0F;
		if (radiation > 0.0F) {
			final double radius = Math.max(1.0D, Math.sqrt(radiation) * 3.0D);
			final AxisAlignedBB bounds = new AxisAlignedBB(position, position).inflate(radius);
			for (final LivingEntity entity : world.getEntitiesOfClass(LivingEntity.class, bounds)) {
				final double attenuation = 1.0D - Math.min(1.0D,
					Math.sqrt(entity.distanceToSqr(position)) / radius);
				if (attenuation > 0.0D) entity.hurt(WarpDamageSources.IRRADIATION,
					(float) (radiation * attenuation));
			}
		}
		final float amountFactor = Math.max(1.25F, amount / 1_000.0F);
		world.explode(null, position.x, position.y, position.z,
			explosionStrength * amountFactor, true, Explosion.Mode.DESTROY);
	}
}
