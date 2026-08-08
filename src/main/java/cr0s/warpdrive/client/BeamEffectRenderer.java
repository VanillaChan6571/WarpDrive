package cr0s.warpdrive.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particles.RedstoneParticleData;
import net.minecraft.util.math.vector.Vector3d;

/** Converts the compact beam packet into a coloured client-side particle line. */
public final class BeamEffectRenderer {

	private static final double PARTICLE_SPACING = 0.5D;
	private static final int MAX_PARTICLES = 1000;

	private BeamEffectRenderer() {
	}

	public static void render(final Vector3d source, final Vector3d target,
	                          final float red, final float green, final float blue,
	                          final int remainingEnergy) {
		final ClientWorld world = Minecraft.getInstance().level;
		if (world == null) {
			return;
		}
		final Vector3d delta = target.subtract(source);
		final double length = delta.length();
		if (length <= 0.0D) {
			return;
		}
		final int particleCount = Math.max(2,
			Math.min(MAX_PARTICLES, (int) Math.ceil(length / PARTICLE_SPACING)));
		final RedstoneParticleData particle = new RedstoneParticleData(
			clampColor(red), clampColor(green), clampColor(blue),
			remainingEnergy > 900_000 ? 1.5F : 1.0F);
		for (int index = 0; index <= particleCount; index++) {
			final double progress = index / (double) particleCount;
			final Vector3d position = source.add(delta.scale(progress));
			world.addParticle(particle, position.x, position.y, position.z, 0.0D, 0.0D, 0.0D);
		}
	}

	private static float clampColor(final float value) {
		return Math.max(0.0F, Math.min(1.0F, value));
	}
}
