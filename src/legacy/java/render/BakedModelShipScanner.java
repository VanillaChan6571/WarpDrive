package cr0s.warpdrive.render;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.EnumFacing;

public class BakedModelShipScanner extends BakedModelAbstractBase {
	
	private TextureAtlasSprite spriteBorder;
	
	private void initSprite() {
		if (spriteBorder == null) {
			final TextureMap textureMapBlocks = Minecraft.getMinecraft().getTextureMapBlocks();
			spriteBorder = textureMapBlocks.getAtlasSprite("warpdrive:blocks/building/ship_scanner-border");
		}
	}
	
	@Nonnull
	@Override
	public List<BakedQuad> getQuads(@Nullable final IBlockState blockState, @Nullable final EnumFacing enumFacing, final long rand) {
		
		initSprite();
		
		final boolean isHidden = false;
		
		// get color
		final int color = 0xFF80FF80;
		
		// pre-compute dimensions
		final int intRadius = 1;
		final float radius = intRadius + 0.0F;
		final int size = 1 + 2 * intRadius;
		
		// pre-compute coordinates
		final float dX_min = 0.0F - radius;
		final float dX_max = 1.0F + radius;
		final float dY_min = (isHidden ? 1.999F : 0.999F);
		final float dY_max = dY_min + 1.0F;
		final float dZ_min = 0.0F - radius;
		final float dZ_max = 1.0F + radius;
		
		final float dU_min = spriteBorder.getMinU();
		final float dU_max = spriteBorder.getMaxU();
		
		final float dV_min = spriteBorder.getMinV();
		final float dV_max = spriteBorder.getMaxV();
		
		final List<BakedQuad> quads = new ArrayList<>();
		
		// start drawing
		for (int index = 0; index < size; index++) {
			final float offsetMin = index == 0 ? 0.0F : 0.001F;
			final float offsetMax = index == size - 1 ? 0.0F : 0.001F;
			
			// draw exterior faces
			addBakedQuad(quads, spriteBorder, color,
			             dX_min + index + 1, dY_max, dZ_min - offsetMax, dU_max, dV_min,
			             dX_min + index + 1, dY_min, dZ_min - offsetMax, dU_max, dV_max,
			             dX_min + index    , dY_min, dZ_min - offsetMin, dU_min, dV_max,
			             dX_min + index    , dY_max, dZ_min - offsetMin, dU_min, dV_min );
			
			addBakedQuad(quads, spriteBorder, color,
			             dX_max - index - 1, dY_max, dZ_max + offsetMax, dU_max, dV_min,
			             dX_max - index - 1, dY_min, dZ_max + offsetMax, dU_max, dV_max,
			             dX_max - index    , dY_min, dZ_max + offsetMin, dU_min, dV_max,
			             dX_max - index    , dY_max, dZ_max + offsetMin, dU_min, dV_min);
			
			addBakedQuad(quads, spriteBorder, color,
			             dX_min - offsetMin, dY_max, dZ_min + index    , dU_max, dV_min,
			             dX_min - offsetMin, dY_min, dZ_min + index    , dU_max, dV_max,
			             dX_min - offsetMax, dY_min, dZ_min + index + 1, dU_min, dV_max,
			             dX_min - offsetMax, dY_max, dZ_min + index + 1, dU_min, dV_min);
			
			addBakedQuad(quads, spriteBorder, color,
			             dX_max + offsetMin, dY_max, dZ_max - index    , dU_max, dV_min,
			             dX_max + offsetMin, dY_min, dZ_max - index    , dU_max, dV_max,
			             dX_max + offsetMax, dY_min, dZ_max - index - 1, dU_min, dV_max,
			             dX_max + offsetMax, dY_max, dZ_max - index - 1, dU_min, dV_min);
			
			// draw interior faces
			addBakedQuad(quads, spriteBorder, color,
			             dX_min + index    , dY_max, dZ_min + offsetMin, dU_min, dV_min,
			             dX_min + index    , dY_min, dZ_min + offsetMin, dU_min, dV_max,
			             dX_min + index + 1, dY_min, dZ_min + offsetMax, dU_max, dV_max,
			             dX_min + index + 1, dY_max, dZ_min + offsetMax, dU_max, dV_min);
			
			addBakedQuad(quads, spriteBorder, color,
			             dX_max - index    , dY_max, dZ_max - offsetMin, dU_min, dV_min,
			             dX_max - index    , dY_min, dZ_max - offsetMin, dU_min, dV_max,
			             dX_max - index - 1, dY_min, dZ_max - offsetMax, dU_max, dV_max,
			             dX_max - index - 1, dY_max, dZ_max - offsetMax, dU_max, dV_min);
			
			addBakedQuad(quads, spriteBorder, color,
			             dX_min + offsetMax, dY_max, dZ_min + index + 1, dU_min, dV_min,
			             dX_min + offsetMax, dY_min, dZ_min + index + 1, dU_min, dV_max,
			             dX_min + offsetMin, dY_min, dZ_min + index    , dU_max, dV_max,
			             dX_min + offsetMin, dY_max, dZ_min + index    , dU_max, dV_min);
			
			addBakedQuad(quads, spriteBorder, color,
			             dX_max - offsetMax, dY_max, dZ_max - index - 1, dU_min, dV_min,
			             dX_max - offsetMax, dY_min, dZ_max - index - 1, dU_min, dV_max,
			             dX_max - offsetMin, dY_min, dZ_max - index    , dU_max, dV_max,
			             dX_max - offsetMin, dY_max, dZ_max - index    , dU_max, dV_min);
		}
		
		return quads;
	}
}