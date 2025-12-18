package cr0s.warpdrive.render;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.CelestialObject;
import cr0s.warpdrive.data.CelestialObjectManager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;

@SideOnly(Side.CLIENT)
public class RenderOverlayLocation {
	
	private static final Minecraft minecraft = Minecraft.getMinecraft();
	
	private void renderLocation(final int widthScreen, final int heightScreen) {
		// get player
		final EntityPlayer entityPlayer = minecraft.player;
		if (entityPlayer == null) {
			return;
		}
		final int x = MathHelper.floor(entityPlayer.posX);
		final int z = MathHelper.floor(entityPlayer.posZ);
		
		// get celestial object
		String name = Commons.format(entityPlayer.world);
		String description = "";
		final CelestialObject celestialObject = CelestialObjectManager.get(entityPlayer.world, x, z);
		if (celestialObject != null) {
			if (!celestialObject.getDisplayName().isEmpty()) {
				name = celestialObject.getDisplayName();
			}
		    description = celestialObject.getDescription();
		}
		
		// start rendering
		GlStateManager.enableBlend();
		minecraft.getTextureManager().bindTexture(Gui.ICONS);
		
		// show current location name & description
		RenderCommons.drawText(widthScreen, heightScreen, name, description,
		                                  WarpDriveConfig.CLIENT_LOCATION_SCALE,
		                                  WarpDriveConfig.CLIENT_LOCATION_NAME_PREFIX,
		                                  WarpDriveConfig.CLIENT_LOCATION_BACKGROUND_COLOR,
		                                  WarpDriveConfig.CLIENT_LOCATION_TEXT_COLOR,
		                                  WarpDriveConfig.CLIENT_LOCATION_HAS_SHADOW,
		                                  WarpDriveConfig.CLIENT_LOCATION_SCREEN_ALIGNMENT,
		                                  WarpDriveConfig.CLIENT_LOCATION_SCREEN_OFFSET_X,
		                                  WarpDriveConfig.CLIENT_LOCATION_SCREEN_OFFSET_Y,
		                                  WarpDriveConfig.CLIENT_LOCATION_TEXT_ALIGNMENT,
		                                  WarpDriveConfig.CLIENT_LOCATION_WIDTH_RATIO,
		                                  WarpDriveConfig.CLIENT_LOCATION_WIDTH_MIN);
		
		// @TODO: show orbiting planet?
		
		// close rendering
		// (done by GlStateManager & TextureManager)
	}
	
	@SubscribeEvent
	public void onRender(final RenderGameOverlayEvent.Pre event) {
		if (event.getType() == ElementType.HOTBAR) {
			renderLocation(event.getResolution().getScaledWidth(), event.getResolution().getScaledHeight());
		}
	}
}