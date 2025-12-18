package cr0s.warpdrive.render;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IMyBakedModel;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.vecmath.Matrix4f;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import org.lwjgl.util.vector.Vector3f;

import net.minecraftforge.client.model.pipeline.UnpackedBakedQuad;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.tuple.Pair;

public abstract class BakedModelAbstractBase implements IMyBakedModel {
	
	protected ModelResourceLocation modelResourceLocation;
	protected IBakedModel bakedModelOriginal;
	
	protected TextureAtlasSprite spriteParticle;
	protected TextureAtlasSprite spriteBlock;
	protected int tintIndex = -1;
	protected VertexFormat format = DefaultVertexFormats.ITEM;
	
	protected IBlockState blockStateDefault;
	
	public BakedModelAbstractBase() {
		super();
	}
	
	@Override
	public void setModelResourceLocation(final ModelResourceLocation modelResourceLocation) {
		this.modelResourceLocation = modelResourceLocation;
	}
	
	@Override
	public void setOriginalBakedModel(final IBakedModel bakedModel) {
		this.bakedModelOriginal = bakedModel;
		spriteParticle = bakedModel.getParticleTexture();
		try {
			for (final EnumFacing enumFacing : EnumFacing.VALUES) {
				final List<BakedQuad> bakedQuads = bakedModel.getQuads(null, enumFacing, 0);
				if (!bakedQuads.isEmpty()) {
					final BakedQuad bakedQuad = bakedQuads.get(0);
					format = bakedQuad.getFormat();
					spriteBlock = bakedQuad.getSprite();
					if (bakedQuad.hasTintIndex()) {
						tintIndex = bakedQuad.getTintIndex();
					}
					break;
				}
			}
		} catch (final Exception exception) {
			exception.printStackTrace(WarpDrive.printStreamError);
			WarpDrive.logger.error(String.format("Exception trying to retrieve format for %s original baked model %s, defaulting to forge",
			                                     modelResourceLocation, bakedModelOriginal));
			format = DefaultVertexFormats.ITEM;
		}
	}
	
	protected void putVertex(final UnpackedBakedQuad.Builder builder,
	                         final float x, final float y, final float z,
	                         final float red, final float green, final float blue, final float alpha,
	                         final float u, final float v,
	                         @Nullable final Vector3f normal) {
		for (int index = 0; index < format.getElementCount(); index++) {
			switch (format.getElement(index).getUsage()) {
			case POSITION:
				builder.put(index, x, y, z, 1.0F);
				break;
				
			case NORMAL:
				if (normal != null) {
					builder.put(index, normal.x, normal.y, normal.z);
				} else {
					WarpDrive.logger.warn(String.format("Missing normal vector, it's required in format %s",
					                                    format));
					builder.put(index);
				}
				break;
				
			case COLOR:
				builder.put(index, red, green, blue, alpha);
				break;
				
			case UV:
				builder.put(index, u, v, 0.0F, 1.0F);
				break;
				
//			case MATRIX:
//			case BLEND_WEIGHT:
			
			case PADDING:
				builder.put(index);
				break;
				
//			case GENERIC:
			
			default:
				WarpDrive.logger.warn(String.format("Unsupported format element #%d %s in %s",
				                                    index, format.getElement(index), format));
				builder.put(index);
				break;
			}
		}
	}
	
	protected void addBakedQuad(final List<BakedQuad> quads, final TextureAtlasSprite sprite,
	                            final float red, final float green, final float blue, final float alpha,
	                            final float x1, final float y1, final float z1, final float u1, final float v1,
	                            final float x2, final float y2, final float z2, final float u2, final float v2,
	                            final float x3, final float y3, final float z3, final float u3, final float v3,
	                            final float x4, final float y4, final float z4, final float u4, final float v4) {
		final Vector3f vectorNormal;
		if (format.hasNormal()) {
			final Vector3f vectorTemp1 = new Vector3f(x3 - x2, y3 - y2, z3 - z2);
			final Vector3f vectorTemp2 = new Vector3f(x1 - x2, y1 - y2, z1 - z2);
			vectorNormal = Vector3f.cross(vectorTemp1, vectorTemp2, vectorTemp1);
			vectorNormal.normalise(vectorNormal);
		} else {
			vectorNormal = null;
		}
		
		final UnpackedBakedQuad.Builder builder = new UnpackedBakedQuad.Builder(format);
		builder.setTexture(sprite);
		putVertex(builder, x1, y1, z1, red, green, blue, alpha, u1, v1, vectorNormal);
		putVertex(builder, x2, y2, z2, red, green, blue, alpha, u2, v2, vectorNormal);
		putVertex(builder, x3, y3, z3, red, green, blue, alpha, u3, v3, vectorNormal);
		putVertex(builder, x4, y4, z4, red, green, blue, alpha, u4, v4, vectorNormal);
		quads.add(builder.build());
	}
	
	protected void addBakedQuad(final List<BakedQuad> quads, final TextureAtlasSprite sprite, final int color,
	                            final float x1, final float y1, final float z1, final float u1, final float v1,
	                            final float x2, final float y2, final float z2, final float u2, final float v2,
	                            final float x3, final float y3, final float z3, final float u3, final float v3,
	                            final float x4, final float y4, final float z4, final float u4, final float v4) {
		final float[] rgba = { (color >> 16 & 0xFF) / 255.0F,
		                       (color >>  8 & 0xFF) / 255.0F,
		                       (color       & 0xFF) / 255.0F,
		                       (color >> 24 & 0xFF) / 255.0F };
		if (rgba[3] == 0.0F) {
			rgba[3] = 1.0F;
		}
		addBakedQuad(quads, sprite,
		             rgba[0], rgba[1], rgba[2], rgba[3],
		             x1, y1, z1, u1, v1,
		             x2, y2, z2, u2, v2,
		             x3, y3, z3, u3, v3,
		             x4, y4, z4, u4, v4);
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
	
	@Nonnull
	@Override
	public TextureAtlasSprite getParticleTexture() {
		// Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite("warpdrive:someTexture")
		return spriteParticle;
	}
	
	/*
	@Nonnull
	@Override
	public ItemCameraTransforms getItemCameraTransforms() {
		// ItemCameraTransforms.DEFAULT
		return bakedModelOriginal.getItemCameraTransforms();
	}
	/**/
	
	@Nonnull
	@Override
	public ItemOverrideList getOverrides() {
		// return bakedModelOriginal.getOverrides();
		return itemOverrideList;
	}
	
	protected ItemOverrideList itemOverrideList = new ItemOverrideList(ImmutableList.of()) {
		@Nonnull
		@Override
		public IBakedModel handleItemState(@Nonnull final IBakedModel model, @Nonnull final ItemStack itemStack,
		                                   final World world, final EntityLivingBase entity) {
			final Block block = ((ItemBlock) itemStack.getItem()).getBlock();
			final IBlockState blockState = block.getStateFromMeta(itemStack.getMetadata());
			final IBakedModel bakedModelNew = Minecraft.getMinecraft().getBlockRendererDispatcher().getModelForState(blockState);
			blockStateDefault = blockState;
			return bakedModelNew;
		}
	};
	
	@Nonnull
	@Override
	public Pair<? extends IBakedModel, Matrix4f> handlePerspective(@Nonnull final ItemCameraTransforms.TransformType cameraTransformType) {
		if (bakedModelOriginal == null) {
			return net.minecraftforge.client.ForgeHooksClient.handlePerspective(this, cameraTransformType);
		}
		final Matrix4f matrix4f = ((IBakedModel) bakedModelOriginal).handlePerspective(cameraTransformType).getRight();
		return Pair.of(this, matrix4f);
	}
}