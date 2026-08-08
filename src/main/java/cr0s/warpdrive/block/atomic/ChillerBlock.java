package cr0s.warpdrive.block.atomic;

import cr0s.warpdrive.damage.WarpDamageSources;
import cr0s.warpdrive.data.Registration;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.util.Random;

/** Active accelerator chiller, including the legacy contact hazard and ambient effects. */
public class ChillerBlock extends AcceleratorComponentBlock {

	private static final VoxelShape COLLISION = box(0.8D, 0.0D, 0.8D, 15.2D, 16.0D, 15.2D);
	private final AcceleratorTier tier;

	public ChillerBlock(final AcceleratorTier tier) {
		super(tier.getIndex());
		this.tier = tier;
	}

	public AcceleratorTier getTier() {
		return tier;
	}

	@Nonnull
	@Override
	@SuppressWarnings("deprecation")
	public VoxelShape getCollisionShape(@Nonnull final BlockState blockState,
	                                    @Nonnull final IBlockReader world,
	                                    @Nonnull final BlockPos blockPos,
	                                    @Nonnull final ISelectionContext context) {
		return COLLISION;
	}

	@Override
	@SuppressWarnings("deprecation")
	public void entityInside(final BlockState blockState, final World world,
	                         final BlockPos blockPos, final Entity entity) {
		applyContactEffect(blockState, world, entity);
	}

	@Override
	@SuppressWarnings("deprecation")
	public void stepOn(final World world, final BlockPos blockPos,
	                   final Entity entity) {
		applyContactEffect(world.getBlockState(blockPos), world, entity);
		super.stepOn(world, blockPos, entity);
	}

	@Override
	@SuppressWarnings("deprecation")
	public void attack(final BlockState blockState, final World world,
	                   final BlockPos blockPos, final PlayerEntity player) {
		applyContactEffect(blockState, world, player);
	}

	private void applyContactEffect(final BlockState blockState, final World world,
	                                final Entity entity) {
		if (world.isClientSide || !blockState.getValue(ACTIVE)
		 || !(entity instanceof LivingEntity) || !entity.isAlive()) return;
		final LivingEntity living = (LivingEntity) entity;
		if (!living.fireImmune()) living.setSecondsOnFire(1);
		living.hurt(WarpDamageSources.WARM, 1.0F + tier.getIndex());
	}

	@Override
	public void animateTick(final BlockState blockState, final World world,
	                       final BlockPos blockPos, final Random random) {
		if (!blockState.getValue(ACTIVE)) return;
		int exposedWeight = 17;
		final int[][] offsets = {
			{ 1, 0, 0 }, { -1, 0, 0 }, { 0, 0, 1 }, { 0, 0, -1 },
			{ 2, 0, 0 }, { -2, 0, 0 }, { 0, 0, 2 }, { 0, 0, -2 },
			{ 1, 2, 0 }, { -1, 2, 0 }, { 0, 2, 1 }, { 0, 2, -1 },
			{ 1, -2, 0 }, { -1, -2, 0 }, { 0, -2, 1 }, { 0, -2, -1 }
		};
		for (final int[] offset : offsets) {
			if (world.getBlockState(blockPos.offset(offset[0], offset[1], offset[2])).getBlock() == this) {
				exposedWeight--;
			}
		}
		if (random.nextInt(17) < exposedWeight) {
			world.playLocalSound(blockPos.getX() + 0.5D, blockPos.getY() + 0.5D,
				blockPos.getZ() + 0.5D, Registration.SOUND_CHILLER.get(),
				SoundCategory.AMBIENT, 1.0F, 1.0F, false);
		}
		if (random.nextInt(8) == 1) return;
		for (int side = 0; side < 6; side++) {
			double x = blockPos.getX() + random.nextDouble();
			double y = blockPos.getY() + random.nextDouble();
			double z = blockPos.getZ() + random.nextDouble();
			boolean visible = false;
			switch (side) {
			case 0: visible = !world.getBlockState(blockPos.above()).isSolidRender(world, blockPos.above()); y = blockPos.getY() + 1.0625D; break;
			case 1: visible = !world.getBlockState(blockPos.below()).isSolidRender(world, blockPos.below()); y = blockPos.getY() - 0.0625D; break;
			case 2: visible = !world.getBlockState(blockPos.south()).isSolidRender(world, blockPos.south()); z = blockPos.getZ() + 1.0625D; break;
			case 3: visible = !world.getBlockState(blockPos.north()).isSolidRender(world, blockPos.north()); z = blockPos.getZ() - 0.0625D; break;
			case 4: visible = !world.getBlockState(blockPos.east()).isSolidRender(world, blockPos.east()); x = blockPos.getX() + 1.0625D; break;
			case 5: visible = !world.getBlockState(blockPos.west()).isSolidRender(world, blockPos.west()); x = blockPos.getX() - 0.0625D; break;
			default: break;
			}
			if (visible) world.addParticle(ParticleTypes.ITEM_SNOWBALL, x, y, z, 0.0D, 0.0D, 0.0D);
		}
	}
}
