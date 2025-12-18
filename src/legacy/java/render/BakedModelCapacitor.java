package cr0s.warpdrive.render;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.block.energy.BlockCapacitor;
import cr0s.warpdrive.data.EnumDisabledInputOutput;

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

public class BakedModelCapacitor extends BakedModelAbstractBase {
	
	private IExtendedBlockState extendedBlockStateDefault;
	
	public BakedModelCapacitor() {
	}
	
	public IBakedModel getOriginalBakedModel() {
		return bakedModelOriginal;
	}
	
	@Nonnull
	@Override
	public List<BakedQuad> getQuads(@Nullable final IBlockState blockState, @Nullable final EnumFacing enumFacing, final long rand) {
		assert modelResourceLocation != null;
		assert bakedModelOriginal != null;
		
		final IExtendedBlockState extendedBlockState;
		if (blockState == null) {
			// dead code until we have different blocks for each tiers to support item rendering and 1.13+
			if (extendedBlockStateDefault == null) {
				extendedBlockStateDefault = ((IExtendedBlockState) WarpDrive.blockCapacitor[0].getDefaultState())
				        .withProperty(BlockCapacitor.DOWN , EnumDisabledInputOutput.INPUT)
				        .withProperty(BlockCapacitor.UP   , EnumDisabledInputOutput.INPUT)
				        .withProperty(BlockCapacitor.NORTH, EnumDisabledInputOutput.OUTPUT)
				        .withProperty(BlockCapacitor.SOUTH, EnumDisabledInputOutput.OUTPUT)
				        .withProperty(BlockCapacitor.WEST , EnumDisabledInputOutput.OUTPUT)
				        .withProperty(BlockCapacitor.EAST , EnumDisabledInputOutput.OUTPUT);
			}
			extendedBlockState = extendedBlockStateDefault;
		} else if (blockState instanceof IExtendedBlockState) {
			extendedBlockState = (IExtendedBlockState) blockState;
		} else {
			extendedBlockState = null;
		}
		if (extendedBlockState != null) {
			final EnumDisabledInputOutput enumDisabledInputOutput = getEnumDisabledInputOutput(extendedBlockState, enumFacing);
			if (enumDisabledInputOutput == null) {
				if (Commons.throttleMe("BakedModelCapacitor invalid extended")) {
					new RuntimeException(String.format("%s Invalid extended property for %s enumFacing %s\n%s",
					                                   this, extendedBlockState, enumFacing, formatDetails() ))
							.printStackTrace(WarpDrive.printStreamError);
				}
				return getDefaultQuads(enumFacing, rand);
			}
			final IBlockState blockStateToRender = extendedBlockState.getClean().withProperty(BlockCapacitor.CONFIG, enumDisabledInputOutput);
			
			// remap to the json model representing the proper state
			final BlockModelShapes blockModelShapes = Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelShapes();
			final IBakedModel bakedModelWrapped = blockModelShapes.getModelForState(blockStateToRender);
			final IBakedModel bakedModelToRender = ((BakedModelCapacitor) bakedModelWrapped).getOriginalBakedModel();
			return bakedModelToRender.getQuads(blockStateToRender, enumFacing, rand);
		}
		return getDefaultQuads(enumFacing, rand);
	}
	
	public EnumDisabledInputOutput getEnumDisabledInputOutput(final IExtendedBlockState extendedBlockState, @Nullable final EnumFacing facing) {
		if (facing == null) {
			return EnumDisabledInputOutput.DISABLED;
		}
		switch (facing) {
		case DOWN : return extendedBlockState.getValue(BlockCapacitor.DOWN);
		case UP   : return extendedBlockState.getValue(BlockCapacitor.UP);
		case NORTH: return extendedBlockState.getValue(BlockCapacitor.NORTH);
		case SOUTH: return extendedBlockState.getValue(BlockCapacitor.SOUTH);
		case WEST : return extendedBlockState.getValue(BlockCapacitor.WEST);
		case EAST : return extendedBlockState.getValue(BlockCapacitor.EAST);
		default: return EnumDisabledInputOutput.DISABLED;
		}
	}
	
	public List<BakedQuad> getDefaultQuads(final EnumFacing side, final long rand) {
		final IBlockState blockState = Blocks.FIRE.getDefaultState();
		return Minecraft.getMinecraft().getBlockRendererDispatcher()
		       .getModelForState(blockState).getQuads(blockState, side, rand);
	}
	
	private String formatDetails() {
		return String.format("modelResourceLocation %s\nbakedModelOriginal %s\nextendedBlockStateDefault %s]",
		                     modelResourceLocation,
		                     bakedModelOriginal,
		                     extendedBlockStateDefault);
	}
}