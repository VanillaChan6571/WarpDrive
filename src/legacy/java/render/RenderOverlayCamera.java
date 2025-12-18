package cr0s.warpdrive.render;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.EnumCameraType;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class RenderOverlayCamera {
	
	private static final int ANIMATION_FRAMES = 200;
	
	private final Minecraft minecraft = Minecraft.getMinecraft();
	private int frameCount = 0;
	
	private void renderOverlay(final int scaledWidth, final int scaledHeight) {
		GlStateManager.disableDepth();
		GlStateManager.depthMask(false);
		GlStateManager.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
		GlStateManager.disableAlpha();
		
		try {
			final String strHelp;
			if (ClientCameraHandler.overlayType == EnumCameraType.SIMPLE_CAMERA) {
				minecraft.getTextureManager().bindTexture(new ResourceLocation("warpdrive", "textures/blocks/detection/camera-overlay.png"));
				strHelp = "Left click to zoom / Right click to exit";
			} else {
				minecraft.getTextureManager().bindTexture(new ResourceLocation("warpdrive", "textures/blocks/weapon/laser_camera-overlay.png"));
				strHelp = "Left click to zoom / Right click to exit / Space to fire";
			}
			
			final Tessellator tessellator = Tessellator.getInstance();
			final BufferBuilder vertexBuffer = tessellator.getBuffer();
			
			vertexBuffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
			vertexBuffer.pos(       0.0D, scaledHeight, -90.0D).tex(0.0D, 1.0D).color(1.0F, 1.0F, 1.0F, 1.0F).endVertex();
			vertexBuffer.pos(scaledWidth, scaledHeight, -90.0D).tex(1.0D, 1.0D).color(1.0F, 1.0F, 1.0F, 1.0F).endVertex();
			vertexBuffer.pos(scaledWidth,         0.0D, -90.0D).tex(1.0D, 0.0D).color(1.0F, 1.0F, 1.0F, 1.0F).endVertex();
			vertexBuffer.pos(       0.0D,         0.0D, -90.0D).tex(0.0D, 0.0D).color(1.0F, 1.0F, 1.0F, 1.0F).endVertex();
			tessellator.draw();
			
			frameCount++;
			if (frameCount >= ANIMATION_FRAMES) {
				frameCount = 0;
			}
			final float time = Math.abs(frameCount * 2.0F / ANIMATION_FRAMES - 1.0F);
			final int color = (RenderCommons.colorGradient(time, 0x40, 0xA0) << 16)
			                + (RenderCommons.colorGradient(time, 0x80, 0x00) << 8)
			                +  RenderCommons.colorGradient(time, 0x80, 0xFF);
			minecraft.fontRenderer.drawString(strHelp,
			                                     (scaledWidth - minecraft.fontRenderer.getStringWidth(strHelp)) / 2,
			                                     (int)(scaledHeight * 0.19) - minecraft.fontRenderer.FONT_HEIGHT,
			                                     color, true);
			
			final String strZoom = "Zoom " + (ClientCameraHandler.originalFOV / minecraft.gameSettings.fovSetting) + "x";
			minecraft.fontRenderer.drawString(strZoom,
			                                     (int) (scaledWidth * 0.91) - minecraft.fontRenderer.getStringWidth(strZoom),
			                                     (int) (scaledHeight * 0.81),
			                                     0x40A080, true);
			
			if (WarpDriveConfig.LOGGING_CAMERA) {
				minecraft.fontRenderer.drawString(ClientCameraHandler.overlayLoggingMessage,
				                                     (scaledWidth - minecraft.fontRenderer.getStringWidth(ClientCameraHandler.overlayLoggingMessage)) / 2,
				                                     (int) (scaledHeight * 0.19),
				                                     0xFF008F, true);
			}
		} catch (final Exception exception) {
			exception.printStackTrace(WarpDrive.printStreamError);
		}
		
		GlStateManager.depthMask(true);
		GlStateManager.enableDepth();
		GlStateManager.enableAlpha();
	}
	
	@SubscribeEvent
	public void onRender(final RenderGameOverlayEvent.Pre event) {
		if (ClientCameraHandler.isOverlayEnabled) {
			switch (event.getType()) {
			case HELMET:
				renderOverlay(event.getResolution().getScaledWidth(), event.getResolution().getScaledHeight());
				break;
				
			case AIR:
			case ARMOR:
			case BOSSHEALTH:
			case BOSSINFO:
			case CROSSHAIRS:
			case EXPERIENCE:
			case FOOD:
			case HEALTH:
			case HEALTHMOUNT:
			case HOTBAR:
			case TEXT:
				// Don't render inventory/stats GUI parts
				if (event.isCancelable()) {
					event.setCanceled(true);
				}
				break;
				
			default:
				// Keep other GUI parts: PORTAL, JUMPBAR, CHAT, PLAYER_LIST, DEBUG, POTION_ICONS, SUBTITLES, FPS_GRAPH, VIGNETTE
				break;
			}
		}
	}
}