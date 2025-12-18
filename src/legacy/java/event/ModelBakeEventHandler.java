package cr0s.warpdrive.event;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IMyBakedModel;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;

import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ModelBakeEventHandler {
	
	public static final ModelBakeEventHandler INSTANCE = new ModelBakeEventHandler();
	private static final Map<ModelResourceLocation, Class<? extends IMyBakedModel>> modelResourceLocationToBakedModel = new HashMap<>(64);
	
	private ModelBakeEventHandler() {
		
	}
	
	public static void registerBakedModel(final ModelResourceLocation modelResourceLocation, final Class<? extends IMyBakedModel> classBakedModel) {
		modelResourceLocationToBakedModel.put(modelResourceLocation, classBakedModel);
	}
	
	// Called after all the other baked block models have been added to the modelRegistry, before BlockModelShapes caches the models.
	@SubscribeEvent
	public void onModelBake(final ModelBakeEvent event) {
		for (final Entry<ModelResourceLocation, Class<? extends IMyBakedModel>> entry : modelResourceLocationToBakedModel.entrySet()) {
			final IBakedModel bakedModelExisting = event.getModelRegistry().getObject(entry.getKey());
			if (bakedModelExisting == null) {
				WarpDrive.logger.warn(String.format("Unable to update baked model for missing %s",
				                                    entry.getKey()));
				continue;
			}
			
			final IMyBakedModel bakedModelNew;
			
			// add a custom baked model wrapping around automatically registered models (from JSON)
			try {
				bakedModelNew = entry.getValue().newInstance();
				bakedModelNew.setModelResourceLocation(entry.getKey());
				bakedModelNew.setOriginalBakedModel(bakedModelExisting);
			} catch (final Exception exception) {
				exception.printStackTrace(WarpDrive.printStreamError);
				WarpDrive.logger.error(String.format("Failed to update baked model through %s of %s",
				                                     entry.getKey(), entry.getValue()));
				continue;
			}
			
			event.getModelRegistry().putObject(entry.getKey(), bakedModelNew);
		}
	}
}