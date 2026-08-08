package cr0s.warpdrive.block.forcefield;

import cr0s.warpdrive.data.Registration;
import cr0s.warpdrive.mixin.ExplosionAccessor;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.EntitySelectionContext;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraft.world.Explosion;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/** Translucent, unbreakable field segment placed and owned by a projector. */
public class ForceFieldBlock extends Block {

	public static final IntegerProperty FREQUENCY = IntegerProperty.create("frequency", 0, 15);
	public static final BooleanProperty CAMOUFLAGED = BooleanProperty.create("camouflaged");
	private static final VoxelShape COLLISION = box(0.8D, 0.8D, 0.8D, 15.2D, 15.2D, 15.2D);
	private static final Map<Explosion, Set<BlockPos>> EXPLOSION_HITS = new WeakHashMap<>();
	private final ForceFieldTier tier;

	public ForceFieldBlock(final ForceFieldTier tier) {
		super(AbstractBlock.Properties.of(Material.GLASS)
			.strength(-1.0F, tier.getBlastResistance())
			.sound(SoundType.WOOL).noOcclusion().noDrops()
			.isSuffocating((state, world, pos) -> false)
			.isViewBlocking((state, world, pos) -> false));
		this.tier = tier;
		registerDefaultState(getStateDefinition().any()
			.setValue(FREQUENCY, 0).setValue(CAMOUFLAGED, false));
	}

	public ForceFieldTier getTier() {
		return tier;
	}

	@Override
	protected void createBlockStateDefinition(final StateContainer.Builder<Block, BlockState> builder) {
		builder.add(FREQUENCY, CAMOUFLAGED);
	}

	@Override
	public BlockRenderType getRenderShape(final BlockState blockState) {
		return blockState.getValue(CAMOUFLAGED) ? BlockRenderType.INVISIBLE : BlockRenderType.MODEL;
	}

	@Override
	public int getLightValue(final BlockState blockState, final IBlockReader world,
	                         final BlockPos blockPos) {
		final TileEntity tileEntity = world.getBlockEntity(blockPos);
		if (tileEntity instanceof ForceFieldTileEntity) {
			final BlockState camouflage = ((ForceFieldTileEntity) tileEntity).getCamouflage();
			if (camouflage != null) return camouflage.getLightValue(world, blockPos);
		}
		return 0;
	}

	@Override
	public boolean hasTileEntity(final BlockState blockState) {
		return true;
	}

	@Nullable
	@Override
	public TileEntity createTileEntity(@Nonnull final BlockState blockState, @Nonnull final IBlockReader world) {
		return new ForceFieldTileEntity();
	}

	@Nonnull
	@Override
	@SuppressWarnings("deprecation")
	public VoxelShape getCollisionShape(@Nonnull final BlockState blockState, @Nonnull final IBlockReader world,
	                                    @Nonnull final BlockPos blockPos, @Nonnull final ISelectionContext context) {
		if (context instanceof EntitySelectionContext) {
			final Entity entity = ((EntitySelectionContext) context).getEntity();
			if (entity instanceof PlayerEntity
			 && (((PlayerEntity) entity).isCreative() || ((PlayerEntity) entity).isSpectator())) {
				return net.minecraft.util.math.shapes.VoxelShapes.empty();
			}
		}
		return COLLISION;
	}

	@Override
	@SuppressWarnings("deprecation")
	public void entityInside(final BlockState blockState, final World world,
	                         final BlockPos blockPos, final Entity entity) {
		if (world.isClientSide || entity instanceof PlayerEntity
		 && (((PlayerEntity) entity).isCreative() || ((PlayerEntity) entity).isSpectator())) {
			return;
		}
		final TileEntity tileEntity = world.getBlockEntity(blockPos);
		if (tileEntity instanceof ForceFieldTileEntity) {
			final ForceFieldProjectorTileEntity projector = ((ForceFieldTileEntity) tileEntity).getProjector();
			if (projector != null) projector.onEntityContact(blockPos, entity);
		}
	}

	@Override
	@SuppressWarnings("deprecation")
	public void attack(final BlockState blockState, final World world,
	                   final BlockPos blockPos, final PlayerEntity player) {
		entityInside(blockState, world, blockPos, player);
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean skipRendering(final BlockState blockState, final BlockState adjacentState,
	                             final Direction side) {
		return adjacentState.getBlock() instanceof ForceFieldBlock
		    && adjacentState.getValue(FREQUENCY).equals(blockState.getValue(FREQUENCY));
	}

	@Override
	public float getExplosionResistance(final BlockState blockState, final IBlockReader world,
	                                    final BlockPos blockPos, final Explosion explosion) {
		final TileEntity tileEntity = world.getBlockEntity(blockPos);
		if (tileEntity instanceof ForceFieldTileEntity) {
			final ForceFieldProjectorTileEntity projector =
				((ForceFieldTileEntity) tileEntity).getProjector();
			if (projector != null) {
				if (markExplosionHit(explosion, blockPos)) {
					final double distance = Math.max(1.0D, explosion.getPosition().distanceTo(
						net.minecraft.util.math.vector.Vector3d.atCenterOf(blockPos)));
					final double strength = Math.min(15.0D,
						((ExplosionAccessor) explosion).warpdrive$getRadius());
					if (!projector.absorbExplosionDamage(strength / (distance * distance))) {
						return tier.getBlastResistance();
					}
				}
				if (projector.getEnergyStored() > 0) {
					return Float.MAX_VALUE;
				}
			}
		}
		return tier.getBlastResistance();
	}

	private static boolean markExplosionHit(final Explosion explosion, final BlockPos blockPos) {
		synchronized (EXPLOSION_HITS) {
			return EXPLOSION_HITS.computeIfAbsent(explosion, ignored -> new HashSet<>())
				.add(blockPos.immutable());
		}
	}

	@Override
	public boolean canDropFromExplosion(final BlockState blockState, final IBlockReader world,
	                                    final BlockPos blockPos, final Explosion explosion) {
		return false;
	}

	@Override
	public boolean canEntityDestroy(final BlockState blockState, final IBlockReader world,
	                                final BlockPos blockPos, final Entity entity) {
		return false;
	}

	@Override
	public void onBlockExploded(final BlockState blockState, final World world,
	                            final BlockPos blockPos, final Explosion explosion) {
		if (world.isClientSide) return;
		final TileEntity oldTile = world.getBlockEntity(blockPos);
		final ForceFieldProjectorTileEntity projector = oldTile instanceof ForceFieldTileEntity
			? ((ForceFieldTileEntity) oldTile).getProjector() : null;
		final BlockState camouflage = oldTile instanceof ForceFieldTileEntity
			? ((ForceFieldTileEntity) oldTile).getCamouflage() : null;
		if (tier.ordinal() == 0) {
			world.removeBlock(blockPos, false);
			return;
		}
		final ForceFieldTier lowerTier = ForceFieldTier.values()[tier.ordinal() - 1];
		final ForceFieldBlock lowerBlock = (ForceFieldBlock)
			Registration.FORCE_FIELD_BLOCKS.get(lowerTier.getName()).get();
		world.setBlock(blockPos, lowerBlock.defaultBlockState()
			.setValue(FREQUENCY, (blockState.getValue(FREQUENCY) + 1) % 16)
			.setValue(CAMOUFLAGED, camouflage != null), 2);
		final TileEntity lowerTile = world.getBlockEntity(blockPos);
		if (projector != null && lowerTile instanceof ForceFieldTileEntity) {
			((ForceFieldTileEntity) lowerTile).setProjector(projector, camouflage);
		}
	}
}
