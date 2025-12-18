package cr0s.warpdrive.client;

import javax.annotation.Nonnull;
import java.util.HashSet;

import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.ResourceLocation;

import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class SpriteManager {
	
	public static final SpriteManager INSTANCE = new SpriteManager();
	private static final HashSet<ResourceLocation> resourceLocationTextures = new HashSet<>(16);
	
	public static void add(@Nonnull final ResourceLocation resourceLocationTexture) {
		resourceLocationTextures.add(resourceLocationTexture);
	}
	
	@SubscribeEvent
	public void onPreTextureStitchEvent(@Nonnull final TextureStitchEvent.Pre eventPreTextureStitch) {
		final TextureMap textureMap = eventPreTextureStitch.getMap();
		
		for (final ResourceLocation resourceLocationTexture : resourceLocationTextures) {
			textureMap.registerSprite(resourceLocationTexture);
		}
	}
}