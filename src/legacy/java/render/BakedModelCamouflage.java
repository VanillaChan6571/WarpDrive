package cr0s.warpdrive.render;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.config.Dictionary;
import cr0s.warpdrive.data.BlockProperties;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.List;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockModelShapes;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;

import net.minecraftforge.common.property.IExtendedBlockState;

public class BakedModelCamouflage extends BakedModelAbstractBase {
	
	public BakedModelCamouflage() {
	}
	
	@Nonnull
	@Override
	public List<BakedQuad> getQuads(@Nullable final IBlockState blockState, @Nullable final EnumFacing enumFacing, final long rand) {
		assert modelResourceLocation != null;
		assert bakedModelOriginal != null;
		
		if (blockState instanceof IExtendedBlockState) {
			final IExtendedBlockState extendedBlockState = (IExtendedBlockState) blockState;
			final IBlockState blockStateReference = extendedBlockState.getValue(BlockProperties.CAMOUFLAGE);
			if (blockStateReference != Blocks.AIR.getDefaultState()) {
				try {
					// Retrieve the IBakedModel of the copied block and return it.
					final BlockModelShapes blockModelShapes = Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelShapes();
					final IBakedModel bakedModelResult = blockModelShapes.getModelForState(blockStateReference);
					return bakedModelResult.getQuads(blockStateReference, enumFacing, rand);
				} catch(final Exception exception) {
					exception.printStackTrace(WarpDrive.printStreamError);
					WarpDrive.logger.error(String.format("Failed to render camouflage for block state %s, updating dictionary with %s = NOCAMOUFLAGE dictionary to prevent further errors",
					                                     blockStateReference,
					                                     blockStateReference.getBlock().getRegistryName()));
					Dictionary.BLOCKS_NOCAMOUFLAGE.add(blockStateReference.getBlock());
				}
			}
		}
		return bakedModelOriginal.getQuads(blockState, enumFacing, rand);
	}
	
	@Override
	public boolean isAmbientOcclusion() {
		return bakedModelOriginal.isAmbientOcclusion();
	}
	
	@Override
	public boolean isGui3d() {
		return bakedModelOriginal.isGui3d();
	}
	
	@Override
	public boolean isBuiltInRenderer() {
		return bakedModelOriginal.isBuiltInRenderer();
	}
}