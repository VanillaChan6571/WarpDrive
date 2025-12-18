package cr0s.warpdrive.render;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.block.energy.TileEntityEnanReactorCore;
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
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.model.TRSRTransformation;

public class TileEntityEnanReactorCoreRenderer extends TileEntitySpecialRenderer<TileEntityEnanReactorCore> {
	
	private IBakedModel bakedModelCore;
	private IBakedModel bakedModelMatter;
	private IBakedModel bakedModelSurface;
	private IBakedModel bakedModelShield;
	private static List<BakedQuad> quadsCore;
	private static List<BakedQuad> quadsMatter;
	private static List<BakedQuad> quadsSurface;
	private static List<BakedQuad> quadsShield;
	
	public TileEntityEnanReactorCoreRenderer() {
		super();
		SpriteManager.add(new ResourceLocation("warpdrive:blocks/energy/reactor_core-crystal"));
		SpriteManager.add(new ResourceLocation("warpdrive:blocks/energy/reactor_core-grip"));
		SpriteManager.add(new ResourceLocation("warpdrive:blocks/energy/reactor_matter"));
		SpriteManager.add(new ResourceLocation("warpdrive:blocks/energy/reactor_surface"));
		SpriteManager.add(new ResourceLocation("warpdrive:blocks/energy/reactor_shield"));
	}
	
	private void updateQuads() {
		// Since we cannot bake in preInit() we do lazy baking of the models as soon as we need it for rendering
		if (bakedModelCore == null) {
			final ResourceLocation resourceLocation = new ResourceLocation(WarpDrive.MODID, "block/energy/reactor_core.obj");
			final IModel model = RenderCommons.getModel(resourceLocation);
			bakedModelCore = model.bake(TRSRTransformation.identity(), DefaultVertexFormats.ITEM, ModelLoader.defaultTextureGetter());
		}
		quadsCore = bakedModelCore.getQuads(null, null, 0L);
		
		if (bakedModelMatter == null) {
			final ResourceLocation resourceLocation = new ResourceLocation(WarpDrive.MODID, "block/energy/reactor_matter.obj");
			final IModel model = RenderCommons.getModel(resourceLocation);
			bakedModelMatter = model.bake(TRSRTransformation.identity(), DefaultVertexFormats.ITEM, ModelLoader.defaultTextureGetter());
		}
		quadsMatter = bakedModelMatter.getQuads(null, null, 0L);
		
		if (bakedModelSurface == null) {
			final ResourceLocation resourceLocation = new ResourceLocation(WarpDrive.MODID, "block/energy/reactor_surface.obj");
			final IModel model = RenderCommons.getModel(resourceLocation);
			bakedModelSurface = model.bake(TRSRTransformation.identity(), DefaultVertexFormats.ITEM, ModelLoader.defaultTextureGetter());
		}
		quadsSurface = bakedModelSurface.getQuads(null, null, 0L);
		
		if (bakedModelShield == null) {
			final ResourceLocation resourceLocation = new ResourceLocation(WarpDrive.MODID, "block/energy/reactor_shield.obj");
			final IModel model = RenderCommons.getModel(resourceLocation);
			bakedModelShield = model.bake(TRSRTransformation.identity(), DefaultVertexFormats.ITEM, ModelLoader.defaultTextureGetter());
		}
		quadsShield = bakedModelShield.getQuads(null, null, 0L);
	}
	
	@Override
	public void render(@Nonnull final TileEntityEnanReactorCore tileEntityEnanReactorCore, final double x, final double y, final double z,
	                   final float partialTicks, final int destroyStage, final float alpha) {
		if (!tileEntityEnanReactorCore.getWorld().isBlockLoaded(tileEntityEnanReactorCore.getPos(), false)) {
			return;
		}
		if (quadsCore == null) {
			updateQuads();
		}
		final Tessellator tessellator = Tessellator.getInstance();
		GlStateManager.pushAttrib();
		GlStateManager.pushMatrix();
		
		final double yCore = y + tileEntityEnanReactorCore.client_yCore + partialTicks * tileEntityEnanReactorCore.client_yCoreSpeed_mPerTick;
		GlStateManager.translate(x + 0.5D, yCore, z + 0.5D);
		
		GlStateManager.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
		GlStateManager.enableBlend();
		// GlStateManager.disableCull();
		RenderHelper.disableStandardItemLighting();
		
		// render the core
		GlStateManager.enableLighting();
		Minecraft.getMinecraft().renderEngine.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
		final BufferBuilder worldRenderer = tessellator.getBuffer();
		
		GlStateManager.pushMatrix();
		
		final float rotationCore = tileEntityEnanReactorCore.client_rotationCore_deg + partialTicks * tileEntityEnanReactorCore.client_rotationSpeedCore_degPerTick;
		GlStateManager.rotate(rotationCore, 0.0F, 1.0F, 0.0F);
		
		worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
		RenderCommons.renderModelTESR(quadsCore, worldRenderer, tileEntityEnanReactorCore.getWorld().getCombinedLight(tileEntityEnanReactorCore.getPos(), 15));
		tessellator.draw();
		
		GlStateManager.popMatrix();
		
		GlStateManager.disableLighting();
		
		// render the matter plasma
		if (tileEntityEnanReactorCore.client_radiusMatter_m > 0.0F) {
			final float radiusMatter = tileEntityEnanReactorCore.client_radiusMatter_m + partialTicks * tileEntityEnanReactorCore.client_radiusSpeedMatter_mPerTick;
			final float heightMatter = Math.max(1.0F, radiusMatter * 1.70F);
			
			// matter model, slightly smaller
			GlStateManager.pushMatrix();
			
			GlStateManager.scale(radiusMatter * 0.95F, heightMatter * 0.90F, radiusMatter * 0.95F);
			final float rotationMatter = tileEntityEnanReactorCore.client_rotationMatter_deg + (partialTicks - 0.75F) * tileEntityEnanReactorCore.client_rotationSpeedMatter_degPerTick;
			GlStateManager.rotate(rotationMatter, 0.0F, 1.0F, 0.0F);
			
			worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
			RenderCommons.renderModelTESR(quadsMatter, worldRenderer, tileEntityEnanReactorCore.getWorld().getCombinedLight(tileEntityEnanReactorCore.getPos(), 15));
			tessellator.draw();
			
			GlStateManager.popMatrix();
			
			// surface model (transparent surface)
			GlStateManager.pushMatrix();
			
			GlStateManager.scale(radiusMatter, heightMatter, radiusMatter);
			final float rotationSurface = tileEntityEnanReactorCore.client_rotationSurface_deg + partialTicks * tileEntityEnanReactorCore.client_rotationSpeedSurface_degPerTick;
			GlStateManager.rotate(rotationSurface, 0.0F, 1.0F, 0.0F);
			
			worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
			RenderCommons.renderModelTESR(quadsSurface, worldRenderer, tileEntityEnanReactorCore.getWorld().getCombinedLight(tileEntityEnanReactorCore.getPos(), 15));
			tessellator.draw();
			
			GlStateManager.popMatrix();
		}
		
		// render the shield
		if (tileEntityEnanReactorCore.client_radiusShield_m > 0.0F) {
			// shield model, slightly bigger
			final float radiusShield = tileEntityEnanReactorCore.client_radiusShield_m + partialTicks * tileEntityEnanReactorCore.client_radiusSpeedShield_mPerTick;
			final float heightShield = Math.max(0.75F, radiusShield * 0.70F);
			GlStateManager.scale(radiusShield, heightShield, radiusShield);
			GlStateManager.rotate(rotationCore, 0.0F, 1.0F, 0.0F);
			
			worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
			RenderCommons.renderModelTESR(quadsShield, worldRenderer, tileEntityEnanReactorCore.getWorld().getCombinedLight(tileEntityEnanReactorCore.getPos(), 15));
			tessellator.draw();
		}
		
		worldRenderer.setTranslation(0.0D, 0.0D, 0.0D);
		
		RenderHelper.enableStandardItemLighting();
		GlStateManager.disableBlend();
		// GlStateManager.enableCull();
		GlStateManager.popMatrix();
		GlStateManager.popAttrib();
	}
	
	@Override
	public boolean isGlobalRenderer(final TileEntityEnanReactorCore tileEntityEnanReactorCore) {
		return true;
	}
}
