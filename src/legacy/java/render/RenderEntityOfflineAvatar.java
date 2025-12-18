package cr0s.warpdrive.render;

import cr0s.warpdrive.client.PlayerTextureManager;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.entity.EntityOfflineAvatar;

import javax.annotation.Nonnull;

import java.util.UUID;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.layers.LayerArrow;
import net.minecraft.client.renderer.entity.layers.LayerBipedArmor;
import net.minecraft.client.renderer.entity.layers.LayerCustomHead;
import net.minecraft.client.renderer.entity.layers.LayerElytra;
import net.minecraft.client.renderer.entity.layers.LayerHeldItem;
import net.minecraft.util.ResourceLocation;

public class RenderEntityOfflineAvatar extends RenderLivingBase<EntityOfflineAvatar> {
	
	public RenderEntityOfflineAvatar(@Nonnull final RenderManager renderManager) {
		super(renderManager, new ModelBiped(), WarpDriveConfig.OFFLINE_AVATAR_MODEL_SCALE);
		
		final boolean useSmallArms = true;
		final ModelBiped modelPlayer = new ModelPlayer(0.0F, useSmallArms);
		mainModel = modelPlayer;
		addLayer(new LayerBipedArmor(this));
		addLayer(new LayerHeldItem(this));
		addLayer(new LayerArrow(this));
		addLayer(new LayerCustomHead(modelPlayer.bipedHead));
		addLayer(new LayerElytra(this));
	}
	
	@Override
	protected void preRenderCallback(@Nonnull final EntityOfflineAvatar entityOfflineAvatar, final float partialTickTime) {
		super.preRenderCallback(entityOfflineAvatar, partialTickTime);
		
		GlStateManager.scale(WarpDriveConfig.OFFLINE_AVATAR_MODEL_SCALE,
		                     WarpDriveConfig.OFFLINE_AVATAR_MODEL_SCALE,
		                     WarpDriveConfig.OFFLINE_AVATAR_MODEL_SCALE );
	}
	
	@Override
	protected ResourceLocation getEntityTexture(@Nonnull final EntityOfflineAvatar entityOfflineAvatar) {
		final UUID uuidPlayer = entityOfflineAvatar.getPlayerUUID();
		final String namePlayer = entityOfflineAvatar.getPlayerName();
		if (uuidPlayer != null) {
			return PlayerTextureManager.getPlayerSkin(uuidPlayer, namePlayer);
		}
		
		return PlayerTextureManager.RESOURCE_LOCATION_DEFAULT;
	}
	
	@Override
	protected boolean canRenderName(@Nonnull final EntityOfflineAvatar entityOfflineAvatar) {
		return entityOfflineAvatar.getAlwaysRenderNameTagForRender();
	}
	
	@Override
	public void doRender(@Nonnull final EntityOfflineAvatar entityOfflineAvatar,
	                     final double x, final double y, final double z, final float entityYaw, final float partialTicks) {
		super.doRender(entityOfflineAvatar, x, y, z, entityYaw, partialTicks);
	}
}
