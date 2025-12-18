package cr0s.warpdrive.render;

import cr0s.warpdrive.entity.EntityNPC;

import javax.annotation.Nonnull;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.ResourceLocation;

public class RenderEntityNPC extends RenderLivingBase<EntityNPC> {
	
	public RenderEntityNPC(@Nonnull final RenderManager renderManager) {
		super(renderManager, new ModelBiped(), 0.5F);
		
		/*
		@TODO: a redesign needed so that we dispatch to original renderers instead of reimplementing them
		
		// textures/entity/steve.png
		// textures/entity/alex.png
		final boolean useSmallArms = true;
		final ModelBiped modelPlayer = new ModelPlayer(0.0F, useSmallArms);
		mainModel = modelPlayer;
		addLayer(new LayerBipedArmor(this));
		addLayer(new LayerHeldItem(this));
		addLayer(new LayerArrow(this));
		addLayer(new LayerCustomHead(modelPlayer.bipedHead));
		addLayer(new LayerElytra(this));
		
		// textures/entity/zombie/zombie.png
		mainModel = new ModelZombie();
		addLayer(new LayerBipedArmor(this) {
			@Override
			protected void initArmor() {
				this.modelLeggings = new ModelZombie(0.5F, true);
				this.modelArmor = new ModelZombie(1.0F, true);
			}
		});
		/**/
	}
	
	@Override
	protected void preRenderCallback(@Nonnull final EntityNPC entityNPC, final float partialTickTime) {
		super.preRenderCallback(entityNPC, partialTickTime);
		
		final float sizeScale = entityNPC.getSizeScale();
		GlStateManager.scale(sizeScale, sizeScale, sizeScale);
	}
	
	@Override
	protected ResourceLocation getEntityTexture(@Nonnull final EntityNPC entityNPC) {
		
		final String textureString = entityNPC.getTextureString();
		if ( !textureString.isEmpty()
		  && ( textureString.contains(":")
			|| textureString.contains("/") ) ) {
			return new ResourceLocation(textureString);
		}
		
		return TextureMap.LOCATION_MISSING_TEXTURE;
	}
	
	@Override
	public void doRender(@Nonnull final EntityNPC entityNPC, final double x, final double y, final double z, final float entityYaw, final float partialTicks) {
		super.doRender(entityNPC, x, y, z, entityYaw, partialTicks);
	}
}
