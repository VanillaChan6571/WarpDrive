package cr0s.warpdrive.client;

import cr0s.warpdrive.block.detection.SirenStyle;
import cr0s.warpdrive.block.detection.SirenTileEntity;
import cr0s.warpdrive.data.Registration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;

import java.util.IdentityHashMap;
import java.util.Map;

/** Owns one looping sound per loaded client siren tile. */
public final class SirenSoundController {

	private static final Map<SirenTileEntity, SirenSound> SOUNDS = new IdentityHashMap<>();

	private SirenSoundController() {
	}

	public static void update(final SirenTileEntity siren) {
		final SoundHandler soundHandler = Minecraft.getInstance().getSoundManager();
		final SirenSound existing = SOUNDS.get(siren);
		if (!siren.isPowered()) {
			stop(siren);
			return;
		}
		if (existing != null && soundHandler.isActive(existing)) {
			return;
		}
		if (existing != null) {
			existing.stopSound();
		}
		final SirenSound sound = new SirenSound(
			siren.getStyle() == SirenStyle.INDUSTRIAL
				? Registration.SOUND_SIREN_INDUSTRIAL.get()
				: Registration.SOUND_SIREN_RAID.get(),
			siren.getRange(), siren.getBlockPos());
		SOUNDS.put(siren, sound);
		soundHandler.play(sound);
	}

	public static void stop(final SirenTileEntity siren) {
		final SirenSound sound = SOUNDS.remove(siren);
		if (sound != null) {
			sound.stopSound();
			Minecraft.getInstance().getSoundManager().stop(sound);
		}
	}
}
