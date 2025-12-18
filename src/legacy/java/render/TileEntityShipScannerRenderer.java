package cr0s.warpdrive.render;

import cr0s.warpdrive.block.building.TileEntityShipScanner;
import cr0s.warpdrive.client.SpriteManager;

import javax.annotation.Nonnull;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class TileEntityShipScannerRenderer extends TileEntitySpecialRenderer<TileEntityShipScanner> {
	
	private static List<BakedQuad> bakedQuads;
	
	public TileEntityShipScannerRenderer() {
		super();
		SpriteManager.add(new ResourceLocation("warpdrive:blocks/building/ship_scanner-border"));
	}
	
	@Override
	public void render(@Nonnull final TileEntityShipScanner tileEntityShipScanner, final double x, final double y, final double z,
	                   final float partialTicks, final int destroyStage, final float alpha) {
		if (!tileEntityShipScanner.getWorld().isBlockLoaded(tileEntityShipScanner.getPos(), false)) {
			return;
		}
		if (bakedQuads == null) {
			bakedQuads = new BakedModelShipScanner().getQuads(null, null, 0L);
		}
		final Tessellator tessellator = Tessellator.getInstance();
		GlStateManager.pushAttrib();
		GlStateManager.pushMatrix();
		
		GlStateManager.translate(x + 0.5D, y + 0.5D, z + 0.5D);
		
		GlStateManager.enableBlend();
		GlStateManager.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE);
		// GlStateManager.disableCull();
		GlStateManager.disableDepth();
		RenderHelper.disableStandardItemLighting();
		GlStateManager.disableLighting();
		
		Minecraft.getMinecraft().renderEngine.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
		final BufferBuilder worldRenderer = tessellator.getBuffer();
		worldRenderer.setTranslation(-0.5, -0.5, -0.5);
		worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
		
		RenderCommons.renderModelTESR(bakedQuads, worldRenderer, tileEntityShipScanner.getWorld().getCombinedLight(tileEntityShipScanner.getPos(), 15));
		
		tessellator.draw();
		worldRenderer.setTranslation(0.0D, 0.0D, 0.0D);
		
		RenderHelper.enableStandardItemLighting();
		GlStateManager.enableDepth();
		// GlStateManager.enableCull();
		GlStateManager.disableBlend();
		
		GlStateManager.popMatrix();
		GlStateManager.popAttrib();
	}
	
	@Override
	public boolean isGlobalRenderer(@Nonnull final TileEntityShipScanner tileEntityShipScanner) {
		return true;
	}
}