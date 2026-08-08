package cr0s.warpdrive.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.TickableSound;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;

/** Long-range loop whose volume falls linearly to zero at the siren tier's configured range. */
public final class SirenSound extends TickableSound {

	private final BlockPos origin;
	private final float range;

	public SirenSound(final SoundEvent soundEvent, final float range, final BlockPos origin) {
		super(soundEvent, SoundCategory.AMBIENT);
		this.origin = origin.immutable();
		this.range = range;
		looping = true;
		delay = 0;
		attenuation = ISound.AttenuationType.NONE;
		relative = true;
		volume = 0.0F;
		pitch = 1.0F;
	}

	@Override
	public void tick() {
		final ClientPlayerEntity player = Minecraft.getInstance().player;
		if (player == null) {
			stop();
			return;
		}
		// A relative, non-attenuated sound at zero is centred on the listener; volume below supplies
		// the legacy long-range falloff independently of vanilla's fixed sound distance.
		x = 0.0D;
		y = 0.0D;
		z = 0.0D;
		final double deltaX = player.getX() - (origin.getX() + 0.5D);
		final double deltaY = player.getY() - (origin.getY() + 0.5D);
		final double deltaZ = player.getZ() - (origin.getZ() + 0.5D);
		final double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
		volume = Math.max(0.0F, 1.0F - (float) (distance / range));
	}

	public void stopSound() {
		stop();
	}
}
