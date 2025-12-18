package cr0s.warpdrive.client;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

// Collection of texture references supporting offline players, unlike net.minecraft.client.network.NetworkPlayerInfo
public class PlayerTextureManager {
	
	private static final PlayerTextureManager INSTANCE = new PlayerTextureManager();
	private static HashMap<UUID, PlayerTextures> mapIdPlayerTextures = new HashMap<>(0);
	public static final ResourceLocation RESOURCE_LOCATION_DEFAULT = new ResourceLocation("textures/entity/steve.png");
	public static final ResourceLocation RESOURCE_LOCATION_SLIM    = new ResourceLocation("textures/entity/alex.png");
	
	private static class PlayerTextures {
		public ResourceLocation resourceLocationSkin;
		public String skinType = "default";
		public ResourceLocation resourceLocationCape;
	}
	
	@Nonnull
	public static ResourceLocation getPlayerSkin(@Nonnull final UUID uuidPlayer, @Nonnull final String namePlayer) {
		{
			final PlayerTextures playerTextures = mapIdPlayerTextures.get(uuidPlayer);
			if (playerTextures != null) {
				if (playerTextures.resourceLocationSkin == null) {
					return RESOURCE_LOCATION_DEFAULT;
				}
				return playerTextures.resourceLocationSkin;
			}
		}
		
		synchronized (INSTANCE) {
			final PlayerTextures playerTextures = new PlayerTextures();
			
			// we clone to avoid synchronization issues without impacting CPU. Hence there's more memory load initially, but less lag during rendering.
			// note: clone is being picky, so we do our own cloning
			final HashMap<UUID, PlayerTextures> mapIdTypeTextureNew = new HashMap<>(mapIdPlayerTextures.size() + 1);
			mapIdTypeTextureNew.putAll(mapIdPlayerTextures);
			mapIdTypeTextureNew.put(uuidPlayer, playerTextures);
			mapIdPlayerTextures = mapIdTypeTextureNew;
			
			Commons.getGameProfile(uuidPlayer, namePlayer, (gameProfileFilled) ->
					Minecraft.getMinecraft().getSkinManager().loadProfileTextures(gameProfileFilled, (type, location, profileTexture) -> {
						switch (type) {
						case SKIN:
							playerTextures.resourceLocationSkin = location;
							final String skinType = profileTexture.getMetadata("model");
							if (skinType == null) {
								playerTextures.skinType = "default";
							} else {
								playerTextures.skinType = skinType;
							}
							break;
							
						case CAPE:
							playerTextures.resourceLocationCape = location;
							break;
							
						default:
							WarpDrive.logger.warn(String.format("Unsupported texture type %s with location %s profile %s for %s",
							                                    type, location, profileTexture, gameProfileFilled ));
							break;
						}
					}, true) );
			
			return RESOURCE_LOCATION_DEFAULT;
		}
	}
}
