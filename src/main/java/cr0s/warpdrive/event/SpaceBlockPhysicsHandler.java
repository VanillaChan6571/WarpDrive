package cr0s.warpdrive.event;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.debug.DebugLog;
import net.minecraft.block.BlockState;
import net.minecraft.entity.item.FallingBlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Gravity-affected blocks (sand, gravel, anvils, concrete powder) do not fall in the WarpDrive
 * dimensions - a station built from sand should stay where it was put.
 *
 * Vanilla FallingBlock removes the block and spawns a FallingBlockEntity in the same tick, so by
 * the time anything is observable the block is already gone. Rather than try to intercept the
 * scheduled tick, this catches the entity as it joins the world, cancels it, and puts the block
 * back where it came from.
 */
@Mod.EventBusSubscriber(modid = WarpDrive.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SpaceBlockPhysicsHandler {

	private static final String SPACE = "warpdrive:space";
	private static final String HYPERSPACE = "warpdrive:hyperspace";

	private SpaceBlockPhysicsHandler() {
	}

	@SubscribeEvent
	public static void onEntityJoinWorld(final EntityJoinWorldEvent event) {
		if (!(event.getEntity() instanceof FallingBlockEntity)) {
			return;
		}
		final World world = event.getWorld();
		if (world == null || world.isClientSide || !isZeroGravity(world)) {
			return;
		}

		final FallingBlockEntity falling = (FallingBlockEntity) event.getEntity();
		final BlockState state = falling.getBlockState();
		final BlockPos origin = falling.blockPosition();

		event.setCanceled(true);

		// Only restore into air: if something already occupies the space, dropping the block back
		// would overwrite it, and silently losing the block is preferable to losing someone's build
		if (world.getBlockState(origin).isAir()) {
			world.setBlock(origin, state, 3);
		} else {
			DebugLog.log("PHYSICS", "suppressed falling block {} at {} but the space was occupied",
				state.getBlock().getRegistryName(), origin);
		}
	}

	private static boolean isZeroGravity(final World world) {
		final String dimension = world.dimension().location().toString();
		return SPACE.equals(dimension) || HYPERSPACE.equals(dimension);
	}
}
