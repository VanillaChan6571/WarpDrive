package cr0s.warpdrive.api;

import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;

public interface IMyBakedModel extends IBakedModel {
	
	void setModelResourceLocation(final ModelResourceLocation modelResourceLocation);
	
	void setOriginalBakedModel(final IBakedModel bakedModel);
}
