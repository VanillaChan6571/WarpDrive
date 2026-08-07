package cr0s.warpdrive.client;

import cr0s.warpdrive.block.AbstractOmnipanelBlock;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockDisplayReader;
import net.minecraftforge.client.model.data.IDynamicBakedModel;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.data.ModelDataMap;
import net.minecraftforge.client.model.data.ModelProperty;
import net.minecraftforge.client.model.pipeline.BakedQuadBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Faithful 1.16.5 mesh for the 1.12.2 omnipanel.
 *
 * The geometry is a 3x3x3 grid whose cuts are 0, 7/16, 9/16 and 1. The centre cell is always
 * present, direct connections add the six axial cells, and the twelve legacy quadrant flags add
 * the edge cells. Emitting only faces on the outside of that union reproduces the old hand-built
 * model without its 684 lines of repeated vertices. It also removes every coincident face at
 * post/arm and arm/arm junctions, which is essential for the translucent shield texture.
 *
 * With no neighbours the 1.12.2 inventory/default model extends all six arms, producing the
 * familiar three-axis cross rather than the tiny centre post used by the incomplete port.
 */
public class OmnipanelBakedModel implements IDynamicBakedModel {

	public static final ModelProperty<Integer> MASK = new ModelProperty<>();

	private static final float[] CUT = { 0.0F, 7.0F / 16.0F, 9.0F / 16.0F, 1.0F };
	private static final int[][] QUADRANT_CELLS = {
		{ 0, 0, 1 }, { 2, 0, 1 }, { 0, 2, 1 }, { 2, 2, 1 },
		{ 0, 1, 0 }, { 2, 1, 0 }, { 0, 1, 2 }, { 2, 1, 2 },
		{ 1, 0, 0 }, { 1, 0, 2 }, { 1, 2, 0 }, { 1, 2, 2 }
	};
	private static final List<BakedQuad> EMPTY = Collections.emptyList();

	private final IBakedModel base;
	private final TextureAtlasSprite sprite;
	private final Map<Integer, List<BakedQuad>> quadCache = new ConcurrentHashMap<>();

	public OmnipanelBakedModel(final IBakedModel base) {
		this.base = base;
		this.sprite = base.getParticleIcon();
	}

	@Nonnull
	@Override
	public IModelData getModelData(@Nonnull final IBlockDisplayReader world,
	                               @Nonnull final BlockPos blockPos,
	                               @Nonnull final BlockState blockState,
	                               @Nonnull final IModelData tileData) {
		return new ModelDataMap.Builder()
			.withInitial(MASK, AbstractOmnipanelBlock.computeMask(world, blockPos))
			.build();
	}

	@Nonnull
	@Override
	public List<BakedQuad> getQuads(@Nullable final BlockState blockState,
	                                @Nullable final Direction side,
	                                @Nonnull final Random random,
	                                @Nonnull final IModelData modelData) {
		// Like the 1.12.2 model, all geometry is in the general bucket. Directional buckets would
		// subject internal panel faces to vanilla full-block neighbour culling.
		if (side != null) {
			return EMPTY;
		}
		final Integer renderMask = modelData.getData(MASK);
		final int mask = renderMask == null ? 0 : renderMask;
		return quadCache.computeIfAbsent(mask, this::buildMesh);
	}

	private List<BakedQuad> buildMesh(final int mask) {
		final boolean[][][] filled = new boolean[3][3][3];
		filled[1][1][1] = true;

		final boolean isolated = (mask & 0x3F) == 0;
		if (isolated || AbstractOmnipanelBlock.isConnected(mask, Direction.WEST))  filled[0][1][1] = true;
		if (isolated || AbstractOmnipanelBlock.isConnected(mask, Direction.EAST))  filled[2][1][1] = true;
		if (isolated || AbstractOmnipanelBlock.isConnected(mask, Direction.DOWN))  filled[1][0][1] = true;
		if (isolated || AbstractOmnipanelBlock.isConnected(mask, Direction.UP))    filled[1][2][1] = true;
		if (isolated || AbstractOmnipanelBlock.isConnected(mask, Direction.NORTH)) filled[1][1][0] = true;
		if (isolated || AbstractOmnipanelBlock.isConnected(mask, Direction.SOUTH)) filled[1][1][2] = true;

		for (int index = 0; index < QUADRANT_CELLS.length; index++) {
			if (isolated || AbstractOmnipanelBlock.hasQuadrant(mask, index)) {
				final int[] cell = QUADRANT_CELLS[index];
				filled[cell[0]][cell[1]][cell[2]] = true;
			}
		}

		final List<BakedQuad> quads = new ArrayList<>();
		for (int x = 0; x < 3; x++) {
			for (int y = 0; y < 3; y++) {
				for (int z = 0; z < 3; z++) {
					if (!filled[x][y][z]) {
						continue;
					}
					for (final Direction direction : Direction.values()) {
						final int nx = x + direction.getStepX();
						final int ny = y + direction.getStepY();
						final int nz = z + direction.getStepZ();
						if (inside(nx, ny, nz) && filled[nx][ny][nz]) {
							continue;
						}
						// A connected arm continues into the next block. Omitting its boundary cap
						// prevents the translucent double-band visible in the incomplete JSON model.
						if (!inside(nx, ny, nz)
						 && AbstractOmnipanelBlock.isConnected(mask, direction)) {
							continue;
						}
						addFace(quads, direction, CUT[x], CUT[y], CUT[z],
						        CUT[x + 1], CUT[y + 1], CUT[z + 1]);
					}
				}
			}
		}
		return Collections.unmodifiableList(quads);
	}

	private static boolean inside(final int x, final int y, final int z) {
		return x >= 0 && x < 3 && y >= 0 && y < 3 && z >= 0 && z < 3;
	}

	private void addFace(final List<BakedQuad> quads, final Direction direction,
	                     final float x0, final float y0, final float z0,
	                     final float x1, final float y1, final float z1) {
		final float[][] positions;
		switch (direction) {
		case DOWN:
			positions = new float[][] { { x0, y0, z1 }, { x0, y0, z0 }, { x1, y0, z0 }, { x1, y0, z1 } };
			break;
		case UP:
			positions = new float[][] { { x1, y1, z1 }, { x1, y1, z0 }, { x0, y1, z0 }, { x0, y1, z1 } };
			break;
		case NORTH:
			positions = new float[][] { { x0, y0, z0 }, { x0, y1, z0 }, { x1, y1, z0 }, { x1, y0, z0 } };
			break;
		case SOUTH:
			positions = new float[][] { { x1, y0, z1 }, { x1, y1, z1 }, { x0, y1, z1 }, { x0, y0, z1 } };
			break;
		case WEST:
			positions = new float[][] { { x0, y0, z0 }, { x0, y0, z1 }, { x0, y1, z1 }, { x0, y1, z0 } };
			break;
		case EAST:
			positions = new float[][] { { x1, y1, z0 }, { x1, y1, z1 }, { x1, y0, z1 }, { x1, y0, z0 } };
			break;
		default:
			throw new IllegalArgumentException("Unknown face " + direction);
		}

		final BakedQuadBuilder builder = new BakedQuadBuilder(sprite);
		builder.setQuadTint(0);
		builder.setQuadOrientation(direction);
		builder.setApplyDiffuseLighting(false);
		for (final float[] position : positions) {
			putVertex(builder, direction, position[0], position[1], position[2]);
		}
		quads.add(builder.build());
	}

	private void putVertex(final BakedQuadBuilder builder, final Direction direction,
	                       final float x, final float y, final float z) {
		final float u16;
		final float v16;
		switch (direction.getAxis()) {
		case X:
			u16 = z * 16.0F;
			v16 = (1.0F - y) * 16.0F;
			break;
		case Y:
			u16 = x * 16.0F;
			v16 = (1.0F - z) * 16.0F;
			break;
		case Z:
		default:
			u16 = x * 16.0F;
			v16 = (1.0F - y) * 16.0F;
			break;
		}

		final VertexFormat format = builder.getVertexFormat();
		for (int elementIndex = 0; elementIndex < format.getElements().size(); elementIndex++) {
			final VertexFormatElement element = format.getElements().get(elementIndex);
			switch (element.getUsage()) {
			case POSITION:
				builder.put(elementIndex, x, y, z, 1.0F);
				break;
			case COLOR:
				builder.put(elementIndex, 1.0F, 1.0F, 1.0F, 1.0F);
				break;
			case UV:
				if (element.getIndex() == 0) {
					builder.put(elementIndex, sprite.getU(u16), sprite.getV(v16), 0.0F, 1.0F);
				} else {
					builder.put(elementIndex, 0.0F, 0.0F, 0.0F, 1.0F);
				}
				break;
			case NORMAL:
				builder.put(elementIndex, direction.getStepX(), direction.getStepY(), direction.getStepZ(), 0.0F);
				break;
			default:
				builder.put(elementIndex);
				break;
			}
		}
	}

	@Override
	public boolean useAmbientOcclusion() {
		return false;
	}

	@Override
	public boolean isGui3d() {
		return true;
	}

	@Override
	public boolean usesBlockLight() {
		return base.usesBlockLight();
	}

	@Override
	public boolean isCustomRenderer() {
		return false;
	}

	@Nonnull
	@Override
	public TextureAtlasSprite getParticleIcon() {
		return sprite;
	}

	@Nonnull
	@Override
	public ItemCameraTransforms getTransforms() {
		return base.getTransforms();
	}

	@Nonnull
	@Override
	public ItemOverrideList getOverrides() {
		return ItemOverrideList.EMPTY;
	}
}
