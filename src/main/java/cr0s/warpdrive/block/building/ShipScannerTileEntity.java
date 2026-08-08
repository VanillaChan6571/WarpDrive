package cr0s.warpdrive.block.building;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.block.ShipCoreTileEntity;
import cr0s.warpdrive.data.Registration;
import cr0s.warpdrive.item.ShipTokenItem;
import cr0s.warpdrive.network.BeamEffectPacket;
import cr0s.warpdrive.network.WarpDriveNetwork;
import cr0s.warpdrive.ship.ShipScanner;
import cr0s.warpdrive.ship.ShipSchematic;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.Util;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.GameType;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.network.PacketDistributor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 1.16 ship scanner and builder.
 *
 * Scanning captures the configured working Ship Core above the machine. Creative computer
 * deployment restores a schematic into empty space, while a configured token station instantiates
 * a fresh copy after a five-second player warm-up. Every destructive or placement action is
 * preflighted through Forge's protection events before the first world mutation.
 */
public class ShipScannerTileEntity extends TileEntity implements ITickableTileEntity {

	private static final int MAX_DEPLOY_RADIUS = 100;
	private static final int SEARCH_INTERVAL_TICKS = 20;
	private static final int SCAN_BLOCKS_PER_SECOND = 10;
	private static final int DEPLOY_INTERVAL_TICKS = 4;
	private static final int DEPLOY_BLOCKS_MIN = 10;
	private static final int DEPLOY_BLOCKS_MAX = 250;
	private static final int TOKEN_WARMUP_SECONDS = 5;
	private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH'h'mm'm'ss's'SSS");

	private enum State {
		IDLE("IDLE"), SCANNING("Scanning"), DEPLOYING("Deploying");
		private final String label;
		State(final String label) { this.label = label; }
	}

	private State state = State.IDLE;
	private String schematicFileName = "";
	private String lastMessage = "Idle";
	private int targetX;
	private int targetY;
	private int targetZ;
	private int rotationSteps;
	private boolean targetConfigured;
	private int searchTicks;
	private int operationTicks;
	private int tokenPollTicks = 5;
	private UUID tokenPlayerId;
	private String tokenSchematic = "";
	private int tokenWarmup = TOKEN_WARMUP_SECONDS;
	@Nullable
	private ShipCoreTileEntity shipCore;
	@Nullable
	private Deployment deployment;

	public ShipScannerTileEntity() {
		super(Registration.SHIP_SCANNER_TILE.get());
	}

	public ShipScannerTier getTier() {
		final BlockState blockState = getBlockState();
		return blockState.getBlock() instanceof ShipScannerBlock
			? ((ShipScannerBlock) blockState.getBlock()).getTier() : ShipScannerTier.BASIC;
	}

	@Override
	public void tick() {
		if (level == null || level.isClientSide) return;
		if (!(level instanceof ServerWorld)) return;

		searchTicks++;
		if (searchTicks >= SEARCH_INTERVAL_TICKS) {
			searchTicks = 0;
			shipCore = findShipCoreAbove();
		}

		syncActiveBlockState();
		switch (state) {
			case SCANNING:
				tickScanning();
				break;
			case DEPLOYING:
				tickDeploying();
				break;
			default:
				if (targetConfigured) tickShipToken();
				break;
		}
	}

	private void tickScanning() {
		operationTicks--;
		if (operationTicks % 6 == 0 && shipCore != null) {
			final int[] bounds = shipCore.getShipBounds();
			final BlockPos target = new BlockPos(
				randomBetween(bounds[0], bounds[3]), randomBetween(bounds[1], bounds[4]),
				randomBetween(bounds[2], bounds[5]));
			sendBeam(target, level.random.nextFloat(), level.random.nextFloat(), level.random.nextFloat());
			level.playSound(null, worldPosition, Registration.SOUND_LASER_LOW.get(),
				SoundCategory.BLOCKS, 0.4F, 1.0F);
		}
		if (operationTicks <= 0) finishOperation("Scan complete: " + schematicFileName);
	}

	private int randomBetween(final int min, final int max) {
		return min >= max ? min : min + level.random.nextInt(max - min + 1);
	}

	private void tickDeploying() {
		if (deployment == null) {
			finishOperation("Deployment state was lost");
			return;
		}
		operationTicks++;
		if (operationTicks % DEPLOY_INTERVAL_TICKS != 0) return;

		final int limit = Math.min(deployment.entries.size(), deployment.index + deployment.blocksPerInterval);
		while (deployment.index < limit) {
			final ShipSchematic.Entry entry = deployment.entries.get(deployment.index);
			final BlockPos target = deployment.schematic.destinationOf(
				entry, deployment.targetCore, deployment.rotationSteps);
			placeEntry(deployment, entry, target);
			if (deployment.index % deployment.effectPeriod == 0) sendBeam(target, 0.0F, 1.0F, 0.2F);
			deployment.index++;
		}
		level.playSound(null, worldPosition, Registration.SOUND_LASER_LOW.get(),
			SoundCategory.BLOCKS, 0.35F, 1.0F);

		if (deployment.index >= deployment.entries.size()) {
			final ServerPlayerEntity requester = playerById(deployment.requesterId);
			if (requester != null) {
				if (deployment.instantiated && requester.level == level) {
					welcomeAboard(requester, deployment.targetCore);
				}
				message(requester, TextFormatting.GREEN,
					"Ship deployed: " + deployment.schematic.getShipName());
			}
			finishOperation("Deployment complete: " + deployment.schematic.getShipName());
		}
	}

	private void placeEntry(final Deployment active, final ShipSchematic.Entry entry,
	                        final BlockPos target) {
		final ServerWorld world = (ServerWorld) level;
		final BlockState placedState = active.schematic.rotatedState(entry, active.rotationSteps);
		world.setBlock(target, placedState, 1);

		if (placedState.hasTileEntity() && world.getBlockEntity(target) == null) {
			final TileEntity created = placedState.getBlock().createTileEntity(placedState, world);
			if (created != null) {
				created.setLevelAndPosition(world, target);
				world.setBlockEntity(target, created);
			}
		}
		final CompoundNBT saved = entry.copyBlockEntityNbt();
		final TileEntity placedTile = world.getBlockEntity(target);
		if (saved != null && placedTile != null) {
			final CompoundNBT adjusted = saved.copy();
			adjusted.putInt("x", target.getX());
			adjusted.putInt("y", target.getY());
			adjusted.putInt("z", target.getZ());
			ShipSchematic.preparePlacementNbt(
				placedState, adjusted, active.targetCore, active.rotationSteps);
			if (active.instantiated) ShipSchematic.prepareInstantiatedNbt(placedState, adjusted);
			try {
				placedTile.load(placedState, adjusted);
				placedTile.setChanged();
			} catch (final RuntimeException exception) {
				WarpDrive.logger.error("Unable to restore block entity {} at {} from schematic",
					placedTile.getClass().getName(), target, exception);
			}
		}
		// Publish only after the block entity contains its saved data. This keeps incremental building
		// visible without racing a constructor-default update packet ahead of restored NBT.
		world.sendBlockUpdated(target, placedState, placedState, 2);
	}

	private void welcomeAboard(final ServerPlayerEntity player, final BlockPos targetCore) {
		for (int offsetY = 1; offsetY <= 5; offsetY++) {
			final BlockPos feet = targetCore.above(offsetY);
			if (level.getBlockState(feet).isAir() && level.getBlockState(feet.above()).isAir()) {
				player.teleportTo(feet.getX() + 0.5D, feet.getY(), feet.getZ() + 0.5D);
				message(player, TextFormatting.GREEN, "Welcome aboard");
				return;
			}
		}
	}

	@Nullable
	private ShipCoreTileEntity findShipCoreAbove() {
		for (int y = worldPosition.getY() + 1; y < level.getMaxBuildHeight(); y++) {
			final BlockPos candidate = new BlockPos(worldPosition.getX(), y, worldPosition.getZ());
			if (!level.hasChunkAt(candidate)) return null;
			final TileEntity tileEntity = level.getBlockEntity(candidate);
			if (tileEntity instanceof ShipCoreTileEntity) return (ShipCoreTileEntity) tileEntity;
		}
		return null;
	}

	public Object[] startScan(@Nullable final ServerPlayerEntity requester) {
		if (level == null || level.isClientSide || !(level instanceof ServerWorld)) {
			return new Object[]{ false, 0, "World is not available" };
		}
		if (state != State.IDLE) return new Object[]{ false, 0, "Already active" };
		shipCore = findShipCoreAbove();
		if (shipCore == null) return failScan("No working ship core above the scanner");

		final int[] bounds = shipCore.getShipBounds();
		if (!areChunksLoaded(bounds)) return failScan("The complete ship must be in loaded chunks");
		final ShipScanner.ShipScanResult scan = ShipScanner.captureShip(
			level, shipCore.getBlockPos(), bounds, getTier().getMaxMass());
		if (!scan.success) return failScan(scan.message);
		final String tierFailure = validateTier(
			bounds[3] - bounds[0] + 1, bounds[4] - bounds[1] + 1, bounds[5] - bounds[2] + 1,
			scan.getBlockCount());
		if (tierFailure != null) return failScan(tierFailure);

		final String stem = ShipSchematic.sanitizeGeneratedName(shipCore.getName()) + "_"
			+ FILE_TIME.format(LocalDateTime.now());
		try {
			ShipSchematic.capture(scan, shipCore.getName()).save((ServerWorld) level, stem);
		} catch (final IOException | RuntimeException exception) {
			WarpDrive.logger.error("Unable to save ship schematic {}", stem, exception);
			return failScan("Unable to save schematic: " + exception.getMessage());
		}
		schematicFileName = stem;
		operationTicks = 20 * (1 + scan.blocks.size() / SCAN_BLOCKS_PER_SECOND);
		state = State.SCANNING;
		lastMessage = "Scanning " + shipCore.getName();
		setChanged();
		if (requester != null) message(requester, TextFormatting.GREEN,
			"Scanning " + shipCore.getName() + " as " + stem);
		return new Object[]{ true, Math.max(1, operationTicks / 20), stem };
	}

	private Object[] failScan(final String reason) {
		lastMessage = reason;
		return new Object[]{ false, 0, reason };
	}

	public void bindToken(final ServerPlayerEntity player, final ItemStack itemStack) {
		if (!(itemStack.getItem() instanceof ShipTokenItem)) return;
		if (schematicFileName.isEmpty()) {
			message(player, TextFormatting.RED, "This scanner has no completed schematic to bind");
			return;
		}
		ShipTokenItem.setSchematicName(itemStack, schematicFileName);
		player.inventory.setChanged();
		message(player, TextFormatting.GREEN, "Token bound to " + schematicFileName);
	}

	public Object[] deploy(final String fileName, final int offsetX, final int offsetY,
	                      final int offsetZ, final int requestedRotation,
	                      @Nullable final ServerPlayerEntity requester) {
		if (requester == null || !requester.isCreative()) {
			return new Object[]{ false, "A creative player must be within 8 blocks" };
		}
		return startDeployment(fileName, worldPosition.offset(offsetX, offsetY, offsetZ),
			requestedRotation, false, requester);
	}

	private Object[] startDeployment(final String fileName, final BlockPos targetCore,
	                                final int requestedRotation, final boolean instantiated,
	                                final ServerPlayerEntity requester) {
		if (level == null || !(level instanceof ServerWorld)) return new Object[]{ false, "World is not available" };
		if (state != State.IDLE) return new Object[]{ false, "Already active: " + state.label };

		final ShipSchematic schematic;
		try {
			schematic = ShipSchematic.load((ServerWorld) level, fileName);
		} catch (final IOException | RuntimeException exception) {
			lastMessage = exception.getMessage();
			return new Object[]{ false, exception.getMessage() };
		}
		final String tierFailure = validateTier(schematic.getSizeX(), schematic.getSizeY(),
			schematic.getSizeZ(), schematic.getMass());
		if (tierFailure != null) return new Object[]{ false, tierFailure };
		if (worldPosition.distSqr(targetCore) > MAX_DEPLOY_RADIUS * MAX_DEPLOY_RADIUS) {
			return new Object[]{ false, "Target is more than " + MAX_DEPLOY_RADIUS + " blocks away" };
		}

		final int rotation = Math.floorMod(requestedRotation, 4);
		final int[] bounds = schematic.destinationBounds(targetCore, rotation);
		final String preflightFailure = preflight(schematic, targetCore, rotation, bounds,
			instantiated, requester);
		if (preflightFailure != null) {
			lastMessage = preflightFailure;
			return new Object[]{ false, preflightFailure };
		}

		if (instantiated) clearDestination(bounds);
		final int blocksPerInterval = Math.max(DEPLOY_BLOCKS_MIN, Math.min(DEPLOY_BLOCKS_MAX,
			(int) Math.ceil(schematic.getBlockCount() * DEPLOY_INTERVAL_TICKS / 200.0D)));
		deployment = new Deployment(schematic, targetCore, rotation, instantiated,
			requester.getUUID(), blocksPerInterval);
		operationTicks = 0;
		state = State.DEPLOYING;
		lastMessage = "Deploying " + schematic.getShipName();
		setChanged();
		return new Object[]{ true, lastMessage };
	}

	@Nullable
	private String validateTier(final int sizeX, final int sizeY, final int sizeZ, final int mass) {
		final ShipScannerTier tier = getTier();
		if (sizeX > tier.getMaxSide() || sizeY > tier.getMaxSide() || sizeZ > tier.getMaxSide()) {
			return String.format("%s scanner supports at most %d blocks per side; schematic is %d x %d x %d",
				tier.getName(), tier.getMaxSide(), sizeX, sizeY, sizeZ);
		}
		if (mass > tier.getMaxMass()) {
			return String.format("%s scanner supports at most %,d blocks; schematic has %,d",
				tier.getName(), tier.getMaxMass(), mass);
		}
		return null;
	}

	@Nullable
	private String preflight(final ShipSchematic schematic, final BlockPos targetCore,
	                        final int rotation, final int[] bounds, final boolean instantiated,
	                        final ServerPlayerEntity requester) {
		if (bounds[1] < 0 || bounds[4] >= level.getMaxBuildHeight()) return "Destination is outside the world height";
		if (!level.getWorldBorder().isWithinBounds(new BlockPos(bounds[0], bounds[1], bounds[2]))
		 || !level.getWorldBorder().isWithinBounds(new BlockPos(bounds[3], bounds[4], bounds[5]))) {
			return "Destination crosses the world border";
		}
		if (contains(bounds, worldPosition)) return "Destination would overwrite the ship scanner";
		if (!areChunksLoaded(bounds)) return "The complete destination must already be in loaded chunks";

		for (int x = bounds[0] - 1; x <= bounds[3] + 1; x++) {
			for (int y = Math.max(0, bounds[1] - 1); y <= Math.min(level.getMaxBuildHeight() - 1, bounds[4] + 1); y++) {
				for (int z = bounds[2] - 1; z <= bounds[5] + 1; z++) {
					final BlockPos pos = new BlockPos(x, y, z);
					final BlockState existing = level.getBlockState(pos);
					if (existing.isAir()) continue;
					if (!instantiated) return String.format("Destination is occupied at %d, %d, %d", x, y, z);
					if (existing.getDestroySpeed(level, pos) < 0.0F) {
						return String.format("Unbreakable block at %d, %d, %d", x, y, z);
					}
					if (!canBreak(requester, pos)) {
						return String.format("Protected block at %d, %d, %d", x, y, z);
					}
					final TileEntity tileEntity = level.getBlockEntity(pos);
					if (tileEntity instanceof ShipCoreTileEntity) {
						final Object[] players = ((ShipCoreTileEntity) tileEntity).getAttachedPlayers();
						if (players.length > 0 && !String.valueOf(players[0]).isEmpty()) {
							return "An occupied ship core is inside the deployment area";
						}
					}
				}
			}
		}

		for (final ShipSchematic.Entry entry : schematic.getEntries()) {
			final BlockPos target = schematic.destinationOf(entry, targetCore, rotation);
			if (!canPlace(requester, target, schematic.rotatedState(entry, rotation))) {
				return String.format("Protected placement at %d, %d, %d",
					target.getX(), target.getY(), target.getZ());
			}
		}
		return null;
	}

	private boolean canBreak(final ServerPlayerEntity requester, final BlockPos target) {
		return ForgeHooks.onBlockBreakEvent((ServerWorld) level, GameType.SURVIVAL, requester, target) >= 0;
	}

	private boolean canPlace(final ServerPlayerEntity requester, final BlockPos target,
	                        final BlockState intendedState) {
		final BlockSnapshot snapshot = BlockSnapshot.create(level.dimension(), level, target);
		return !MinecraftForge.EVENT_BUS.post(
			new BlockEvent.EntityPlaceEvent(snapshot, intendedState, requester));
	}

	private void clearDestination(final int[] bounds) {
		for (int x = bounds[0] - 1; x <= bounds[3] + 1; x++) {
			for (int y = Math.max(0, bounds[1] - 1); y <= Math.min(level.getMaxBuildHeight() - 1, bounds[4] + 1); y++) {
				for (int z = bounds[2] - 1; z <= bounds[5] + 1; z++) {
					final BlockPos pos = new BlockPos(x, y, z);
					if (level.getBlockState(pos).isAir()) continue;
					level.removeBlockEntity(pos);
					level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
				}
			}
		}
	}

	private boolean areChunksLoaded(final int[] bounds) {
		for (int chunkX = bounds[0] >> 4; chunkX <= bounds[3] >> 4; chunkX++) {
			for (int chunkZ = bounds[2] >> 4; chunkZ <= bounds[5] >> 4; chunkZ++) {
				if (!level.hasChunkAt(new BlockPos(chunkX << 4, worldPosition.getY(), chunkZ << 4))) return false;
			}
		}
		return true;
	}

	private static boolean contains(final int[] bounds, final BlockPos pos) {
		return pos.getX() >= bounds[0] - 1 && pos.getX() <= bounds[3] + 1
		    && pos.getY() >= bounds[1] - 1 && pos.getY() <= bounds[4] + 1
		    && pos.getZ() >= bounds[2] - 1 && pos.getZ() <= bounds[5] + 1;
	}

	private void tickShipToken() {
		tokenPollTicks--;
		if (tokenPollTicks > 0) return;
		tokenPollTicks = 20;

		final AxisAlignedBB box = new AxisAlignedBB(
			worldPosition.getX() - 1.0D, worldPosition.getY() + 1.0D, worldPosition.getZ() - 1.0D,
			worldPosition.getX() + 2.0D, worldPosition.getY() + 5.0D, worldPosition.getZ() + 2.0D);
		final List<PlayerEntity> players = level.getEntitiesOfClass(PlayerEntity.class, box);
		if (players.size() != 1 || !(players.get(0) instanceof ServerPlayerEntity)) {
			if (players.size() > 1) for (final PlayerEntity player : players) {
				player.sendMessage(new StringTextComponent("Only one player may use a ship token station at a time")
					.withStyle(TextFormatting.RED), Util.NIL_UUID);
			}
			resetTokenWarmup();
			return;
		}
		final ServerPlayerEntity player = (ServerPlayerEntity) players.get(0);
		final TokenSlot tokenSlot = findToken(player);
		if (tokenSlot == null) {
			resetTokenWarmup();
			return;
		}

		final String name = ((ShipTokenItem) tokenSlot.stack.getItem()).getSchematicName(tokenSlot.stack);
		if (!player.getUUID().equals(tokenPlayerId) || !name.equals(tokenSchematic)) {
			tokenPlayerId = player.getUUID();
			tokenSchematic = name;
			tokenWarmup = TOKEN_WARMUP_SECONDS;
			message(player, TextFormatting.GREEN, "Ship token detected: " + name);
			return;
		}

		tokenWarmup--;
		if (tokenWarmup > 0) {
			message(player, TextFormatting.GRAY,
				"Materializing " + name + " in " + tokenWarmup + "...");
			return;
		}
		resetTokenWarmup();
		final Object[] result = startDeployment(name, new BlockPos(targetX, targetY, targetZ),
			rotationSteps, true, player);
		message(player, Boolean.TRUE.equals(result[0]) ? TextFormatting.GREEN : TextFormatting.RED,
			String.valueOf(result.length > 1 ? result[1] : result[0]));
		if (Boolean.TRUE.equals(result[0]) && !player.isCreative()) {
			tokenSlot.stack.shrink(1);
			player.inventory.setChanged();
		}
	}

	@Nullable
	private TokenSlot findToken(final ServerPlayerEntity player) {
		for (int index = 0; index < player.inventory.getContainerSize(); index++) {
			final ItemStack itemStack = player.inventory.getItem(index);
			if (!itemStack.isEmpty() && itemStack.getItem() instanceof ShipTokenItem) {
				return new TokenSlot(itemStack);
			}
		}
		return null;
	}

	private void resetTokenWarmup() {
		tokenPlayerId = null;
		tokenSchematic = "";
		tokenWarmup = TOKEN_WARMUP_SECONDS;
	}

	public Object[] configureTarget(final int offsetX, final int offsetY, final int offsetZ,
	                              final int requestedRotation, @Nullable final ServerPlayerEntity requester) {
		if (requester == null || !requester.isCreative()) {
			return new Object[]{ false, "A creative player must be within 8 blocks" };
		}
		final BlockPos target = worldPosition.offset(offsetX, offsetY, offsetZ);
		if (worldPosition.distSqr(target) > MAX_DEPLOY_RADIUS * MAX_DEPLOY_RADIUS) {
			return new Object[]{ false, "Target is more than " + MAX_DEPLOY_RADIUS + " blocks away" };
		}
		targetX = target.getX();
		targetY = target.getY();
		targetZ = target.getZ();
		rotationSteps = Math.floorMod(requestedRotation, 4);
		targetConfigured = true;
		setChanged();
		return new Object[]{ true, targetX, targetY, targetZ, rotationSteps };
	}

	public Object[] fileName() {
		return state == State.DEPLOYING
			? new Object[]{ false, "Deployment in progress" }
			: new Object[]{ !schematicFileName.isEmpty(), schematicFileName };
	}

	public Object[] state() {
		final int current = deployment == null ? 0 : deployment.index;
		final int total = deployment == null ? 0 : deployment.entries.size();
		return new Object[]{ state != State.IDLE, state.label, current, total, lastMessage };
	}

	public ITextComponent getStatusText() {
		final String tier = getTier().getName();
		final String target = targetConfigured
			? String.format(" target %d, %d, %d r%d", targetX, targetY, targetZ, rotationSteps)
			: "";
		return new StringTextComponent(String.format("Ship scanner %s: %s%s", tier, lastMessage, target));
	}

	@Nullable
	public ServerPlayerEntity findCreativePlayer() {
		if (!(level instanceof ServerWorld)) return null;
		ServerPlayerEntity closest = null;
		double closestDistance = 64.0D;
		for (final ServerPlayerEntity player : ((ServerWorld) level).players()) {
			final double distance = player.distanceToSqr(
				worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D);
			if (player.isCreative() && distance <= closestDistance) {
				closest = player;
				closestDistance = distance;
			}
		}
		return closest;
	}

	@Nullable
	private ServerPlayerEntity playerById(@Nullable final UUID uuid) {
		return uuid == null || level == null || level.getServer() == null
			? null : level.getServer().getPlayerList().getPlayer(uuid);
	}

	private void finishOperation(final String message) {
		state = State.IDLE;
		operationTicks = 0;
		deployment = null;
		lastMessage = message;
		setChanged();
		syncActiveBlockState();
	}

	private void syncActiveBlockState() {
		if (level == null) return;
		final BlockState blockState = getBlockState();
		if (blockState.getBlock() instanceof ShipScannerBlock
		 && blockState.getValue(ShipScannerBlock.ACTIVE) != (state != State.IDLE)) {
			level.setBlock(worldPosition,
				blockState.setValue(ShipScannerBlock.ACTIVE, state != State.IDLE), 3);
		}
	}

	private void sendBeam(final BlockPos target, final float red, final float green, final float blue) {
		WarpDriveNetwork.CHANNEL.send(
			PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(
				worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), 128.0D, level.dimension())),
			new BeamEffectPacket(Vector3d.atCenterOf(worldPosition), Vector3d.atCenterOf(target),
				red, green, blue, 0));
	}

	private static void message(final PlayerEntity player, final TextFormatting color, final String text) {
		player.sendMessage(new StringTextComponent(text).withStyle(color), Util.NIL_UUID);
	}

	@Override
	public void load(@Nonnull final BlockState blockState, @Nonnull final CompoundNBT tag) {
		super.load(blockState, tag);
		schematicFileName = tag.getString("schematic");
		targetX = tag.getInt("targetX");
		targetY = tag.getInt("targetY");
		targetZ = tag.getInt("targetZ");
		rotationSteps = Math.floorMod(tag.getInt("rotationSteps"), 4);
		targetConfigured = tag.contains("targetConfigured")
			? tag.getBoolean("targetConfigured") : targetX != 0 || targetY != 0 || targetZ != 0;
		lastMessage = tag.contains("lastMessage") ? tag.getString("lastMessage") : "Idle";
		if (lastMessage.startsWith("Deploying ")) {
			lastMessage = "Deployment interrupted by world reload";
		} else if (lastMessage.startsWith("Scanning ") && !schematicFileName.isEmpty()) {
			lastMessage = "Scan snapshot saved: " + schematicFileName;
		}
		state = State.IDLE;
		deployment = null;
	}

	@Nonnull
	@Override
	public CompoundNBT save(@Nonnull final CompoundNBT tag) {
		super.save(tag);
		tag.putString("schematic", schematicFileName);
		tag.putInt("targetX", targetX);
		tag.putInt("targetY", targetY);
		tag.putInt("targetZ", targetZ);
		tag.putInt("rotationSteps", rotationSteps);
		tag.putBoolean("targetConfigured", targetConfigured);
		tag.putString("lastMessage", lastMessage);
		return tag;
	}

	private static final class TokenSlot {
		private final ItemStack stack;
		private TokenSlot(final ItemStack stack) { this.stack = stack; }
	}

	private static final class Deployment {
		private final ShipSchematic schematic;
		private final List<ShipSchematic.Entry> entries;
		private final BlockPos targetCore;
		private final int rotationSteps;
		private final boolean instantiated;
		private final UUID requesterId;
		private final int blocksPerInterval;
		private final int effectPeriod;
		private int index;

		private Deployment(final ShipSchematic schematic, final BlockPos targetCore,
		                   final int rotationSteps, final boolean instantiated,
		                   final UUID requesterId, final int blocksPerInterval) {
			this.schematic = schematic;
			this.entries = new ArrayList<>(schematic.getEntries());
			this.targetCore = targetCore.immutable();
			this.rotationSteps = rotationSteps;
			this.instantiated = instantiated;
			this.requesterId = requesterId;
			this.blocksPerInterval = blocksPerInterval;
			this.effectPeriod = Math.max(1, entries.size() / 100);
		}
	}
}
