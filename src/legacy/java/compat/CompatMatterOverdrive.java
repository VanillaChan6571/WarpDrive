package cr0s.warpdrive.compat;

import javax.annotation.Nonnull;

import matteroverdrive.entity.android_player.AndroidPlayer;
import matteroverdrive.entity.player.MOPlayerCapabilityProvider;
import net.minecraft.entity.player.EntityPlayer;

public class CompatMatterOverdrive {
	
	public static boolean isAndroid(@Nonnull final EntityPlayer player) {
		final AndroidPlayer playerCapability = MOPlayerCapabilityProvider.GetAndroidCapability(player);
		return playerCapability != null && playerCapability.isAndroid();
	}
}
