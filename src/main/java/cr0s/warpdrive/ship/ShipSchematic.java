package cr0s.warpdrive.ship;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.block.ShipCoreBlock;
import cr0s.warpdrive.block.energy.CapacitorBlock;
import cr0s.warpdrive.data.Registration;
import cr0s.warpdrive.data.WarpDriveTags;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.HorizontalBlock;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.FolderName;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * A portable ship snapshot used by the ship scanner.
 *
 * The native format stores complete 1.16 block states and block-entity data. A small importer also
 * accepts the 1.12 scanner's string-palette schematic format; metadata-only states cannot always be
 * reconstructed after the flattening, so those fall back to the registered block's default state.
 */
public final class ShipSchematic {

	private static final int FORMAT_VERSION = 1;
	private static final int MAX_BLOCKS = 110_592;
	private static final Pattern VALID_FILE_NAME = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,95}");
	private static final FolderName SCHEMATIC_FOLDER = new FolderName("warpdrive_schematics");
	private static final Rotation[] ROTATIONS = {
		Rotation.NONE, Rotation.CLOCKWISE_90, Rotation.CLOCKWISE_180, Rotation.COUNTERCLOCKWISE_90 };

	private final String shipName;
	private final BlockPos minRelative;
	private final BlockPos maxRelative;
	private final List<Entry> entries;
	private final int mass;

	private ShipSchematic(final String shipName, final BlockPos minRelative,
	                      final BlockPos maxRelative, final List<Entry> entries,
	                      final int mass) {
		this.shipName = shipName == null || shipName.trim().isEmpty() ? "Unnamed Ship" : shipName;
		this.minRelative = minRelative.immutable();
		this.maxRelative = maxRelative.immutable();
		final List<Entry> ordered = new ArrayList<>(entries);
		ordered.sort(Comparator.comparingInt(ShipSchematic::placementPriority));
		this.entries = Collections.unmodifiableList(ordered);
		this.mass = Math.max(0, Math.min(mass, entries.size()));
	}

	public static ShipSchematic capture(@Nonnull final ShipScanner.ShipScanResult scan,
	                                    @Nonnull final String shipName) {
		if (!scan.success) {
			throw new IllegalArgumentException("Cannot capture an invalid ship: " + scan.message);
		}
		final List<Entry> entries = new ArrayList<>(scan.blocks.size());
		for (final ShipScanner.ShipBlock shipBlock : scan.blocks) {
			entries.add(new Entry(shipBlock.pos.subtract(scan.corePos), shipBlock.state,
				normalizeLinkedTarget(shipBlock.copyTileEntityNBT(), scan.corePos)));
		}
		return new ShipSchematic(shipName,
			new BlockPos(scan.minX - scan.corePos.getX(), scan.minY - scan.corePos.getY(),
				scan.minZ - scan.corePos.getZ()),
			new BlockPos(scan.maxX - scan.corePos.getX(), scan.maxY - scan.corePos.getY(),
				scan.maxZ - scan.corePos.getZ()), entries, scan.getBlockCount());
	}

	public String getShipName() {
		return shipName;
	}

	public int getBlockCount() {
		return entries.size();
	}

	public int getMass() {
		return mass;
	}

	public int getSizeX() {
		return maxRelative.getX() - minRelative.getX() + 1;
	}

	public int getSizeY() {
		return maxRelative.getY() - minRelative.getY() + 1;
	}

	public int getSizeZ() {
		return maxRelative.getZ() - minRelative.getZ() + 1;
	}

	public List<Entry> getEntries() {
		return entries;
	}

	public BlockPos destinationOf(@Nonnull final Entry entry, @Nonnull final BlockPos targetCore,
	                              final int rotationSteps) {
		return targetCore.offset(rotateRelative(entry.relativePos, rotationSteps));
	}

	/** Inclusive destination envelope, ordered minX/minY/minZ/maxX/maxY/maxZ. */
	public int[] destinationBounds(@Nonnull final BlockPos targetCore, final int rotationSteps) {
		int minX = Integer.MAX_VALUE;
		int minY = Integer.MAX_VALUE;
		int minZ = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE;
		int maxY = Integer.MIN_VALUE;
		int maxZ = Integer.MIN_VALUE;
		for (final int x : new int[]{ minRelative.getX(), maxRelative.getX() }) {
			for (final int y : new int[]{ minRelative.getY(), maxRelative.getY() }) {
				for (final int z : new int[]{ minRelative.getZ(), maxRelative.getZ() }) {
					final BlockPos transformed = targetCore.offset(
						rotateRelative(new BlockPos(x, y, z), rotationSteps));
					minX = Math.min(minX, transformed.getX());
					minY = Math.min(minY, transformed.getY());
					minZ = Math.min(minZ, transformed.getZ());
					maxX = Math.max(maxX, transformed.getX());
					maxY = Math.max(maxY, transformed.getY());
					maxZ = Math.max(maxZ, transformed.getZ());
				}
			}
		}
		return new int[]{ minX, minY, minZ, maxX, maxY, maxZ };
	}

	public BlockState rotatedState(@Nonnull final Entry entry, final int rotationSteps) {
		return entry.blockState.rotate(rotation(rotationSteps));
	}

	public static BlockPos rotateRelative(@Nonnull final BlockPos relative, final int rotationSteps) {
		switch (Math.floorMod(rotationSteps, 4)) {
			case 1: return new BlockPos(-relative.getZ(), relative.getY(), relative.getX());
			case 2: return new BlockPos(-relative.getX(), relative.getY(), -relative.getZ());
			case 3: return new BlockPos(relative.getZ(), relative.getY(), -relative.getX());
			default: return relative.immutable();
		}
	}

	private static Rotation rotation(final int rotationSteps) {
		return ROTATIONS[Math.floorMod(rotationSteps, ROTATIONS.length)];
	}

	/** A safe generated filename stem; caller adds its timestamp before saving. */
	public static String sanitizeGeneratedName(@Nullable final String requested) {
		String safe = requested == null ? "ship" : requested.trim();
		safe = safe.replaceAll("[^A-Za-z0-9._-]+", "_");
		safe = safe.replaceAll("^[._-]+", "");
		safe = safe.replace("..", "_");
		if (safe.isEmpty()) safe = "ship";
		return safe.length() > 48 ? safe.substring(0, 48) : safe;
	}

	public void save(@Nonnull final ServerWorld world, @Nonnull final String fileName) throws IOException {
		final Path destination = resolve(world, fileName);
		Files.createDirectories(destination.getParent());
		final Path temporary = Files.createTempFile(destination.getParent(), ".ship-", ".tmp");
		try {
			CompressedStreamTools.writeCompressed(toNbt(), temporary.toFile());
			try {
				Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE,
					StandardCopyOption.REPLACE_EXISTING);
			} catch (final AtomicMoveNotSupportedException ignored) {
				Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
			}
		} finally {
			Files.deleteIfExists(temporary);
		}
	}

	public static ShipSchematic load(@Nonnull final ServerWorld world,
	                                 @Nonnull final String fileName) throws IOException {
		final File file = resolve(world, fileName).toFile();
		if (!file.isFile()) {
			throw new IOException("Schematic '" + normalizeFileName(fileName) + "' was not found");
		}
		final CompoundNBT root = CompressedStreamTools.readCompressed(file);
		return root.contains("WarpDriveSchematicVersion", Constants.NBT.TAG_INT)
			? fromNativeNbt(root) : fromLegacyNbt(root);
	}

	public static boolean exists(@Nonnull final ServerWorld world, @Nonnull final String fileName) {
		try {
			return Files.isRegularFile(resolve(world, fileName));
		} catch (final IllegalArgumentException ignored) {
			return false;
		}
	}

	private static Path resolve(final ServerWorld world, final String requested) {
		final String fileName = normalizeFileName(requested) + ".schematic";
		final Path folder = world.getServer().getWorldPath(SCHEMATIC_FOLDER).normalize();
		final Path resolved = folder.resolve(fileName).normalize();
		if (!resolved.getParent().equals(folder)) {
			throw new IllegalArgumentException("Invalid schematic filename");
		}
		return resolved;
	}

	public static String normalizeFileName(final String requested) {
		if (requested == null) throw new IllegalArgumentException("Schematic filename is required");
		String name = requested.trim();
		final String lower = name.toLowerCase(Locale.ROOT);
		if (lower.endsWith(".schematic")) name = name.substring(0, name.length() - 10);
		if (!VALID_FILE_NAME.matcher(name).matches() || name.contains("..")) {
			throw new IllegalArgumentException("Invalid schematic filename");
		}
		return name;
	}

	private CompoundNBT toNbt() {
		final CompoundNBT root = new CompoundNBT();
		root.putInt("WarpDriveSchematicVersion", FORMAT_VERSION);
		root.putString("ShipName", shipName);
		root.putInt("Mass", mass);
		root.put("Min", NBTUtil.writeBlockPos(minRelative));
		root.put("Max", NBTUtil.writeBlockPos(maxRelative));
		final ListNBT blocks = new ListNBT();
		for (final Entry entry : entries) {
			final CompoundNBT block = new CompoundNBT();
			block.put("Pos", NBTUtil.writeBlockPos(entry.relativePos));
			block.put("State", NBTUtil.writeBlockState(entry.blockState));
			if (entry.blockEntityNbt != null) {
				final CompoundNBT tile = entry.blockEntityNbt.copy();
				tile.remove("x");
				tile.remove("y");
				tile.remove("z");
				block.put("BlockEntity", tile);
			}
			blocks.add(block);
		}
		root.put("Blocks", blocks);
		return root;
	}

	private static ShipSchematic fromNativeNbt(final CompoundNBT root) throws IOException {
		if (root.getInt("WarpDriveSchematicVersion") != FORMAT_VERSION) {
			throw new IOException("Unsupported WarpDrive schematic version "
				+ root.getInt("WarpDriveSchematicVersion"));
		}
		final ListNBT blocks = root.getList("Blocks", Constants.NBT.TAG_COMPOUND);
		if (blocks.isEmpty() || blocks.size() > MAX_BLOCKS) {
			throw new IOException("Invalid schematic block count " + blocks.size());
		}
		final List<Entry> entries = new ArrayList<>(blocks.size());
		for (int index = 0; index < blocks.size(); index++) {
			final CompoundNBT block = blocks.getCompound(index);
			final BlockState state = NBTUtil.readBlockState(block.getCompound("State"));
			if (state.isAir()) continue;
			entries.add(new Entry(NBTUtil.readBlockPos(block.getCompound("Pos")), state,
				block.contains("BlockEntity", Constants.NBT.TAG_COMPOUND)
					? block.getCompound("BlockEntity") : null));
		}
		if (entries.isEmpty()) throw new IOException("Schematic contains no known blocks");
		return validated(root.getString("ShipName"), NBTUtil.readBlockPos(root.getCompound("Min")),
			NBTUtil.readBlockPos(root.getCompound("Max")), entries,
			root.contains("Mass", Constants.NBT.TAG_INT) ? root.getInt("Mass") : countMass(entries));
	}

	private static ShipSchematic fromLegacyNbt(final CompoundNBT root) throws IOException {
		final int width = root.getShort("Width") & 0xFFFF;
		final int height = root.getShort("Height") & 0xFFFF;
		final int length = root.getShort("Length") & 0xFFFF;
		final long volume = (long) width * height * length;
		final ListNBT palette = root.getList("Blocks", Constants.NBT.TAG_STRING);
		if (width <= 0 || height <= 0 || length <= 0 || volume > Integer.MAX_VALUE
		 || palette.size() != (int) volume) {
			throw new IOException("Unsupported or corrupt legacy schematic dimensions");
		}

		final CompoundNBT ship = root.getCompound("ship");
		final boolean hasLegacyCore = ship.contains("coreX", Constants.NBT.TAG_INT);
		final BlockPos legacyReferenceCore = hasLegacyCore
			? new BlockPos(ship.getInt("coreX"), ship.getInt("coreY"), ship.getInt("coreZ")) : null;
		int coreX = ship.getInt("coreX") - ship.getInt("minX");
		int coreY = ship.getInt("coreY") - ship.getInt("minY");
		int coreZ = ship.getInt("coreZ") - ship.getInt("minZ");
		final Map<Long, CompoundNBT> tileByPos = new HashMap<>();
		final ListNBT tiles = root.getList("TileEntities", Constants.NBT.TAG_COMPOUND);
		for (int index = 0; index < tiles.size(); index++) {
			final CompoundNBT tile = tiles.getCompound(index).copy();
			tileByPos.put(BlockPos.asLong(tile.getInt("x"), tile.getInt("y"), tile.getInt("z")), tile);
		}

		final List<Entry> entries = new ArrayList<>();
		BlockPos detectedCore = null;
		for (int y = 0; y < height; y++) {
			for (int z = 0; z < length; z++) {
				for (int x = 0; x < width; x++) {
					final int index = x + (y * length + z) * width;
					final String registryName = palette.getString(index);
					final Block block = legacyBlock(registryName);
					if (block == null || block == Blocks.AIR) continue;
					if (registryName.startsWith("warpdrive:ship_core")) {
						detectedCore = new BlockPos(x, y, z);
					}
					final CompoundNBT tile = legacyReferenceCore == null
						? tileByPos.get(BlockPos.asLong(x, y, z))
						: normalizeLinkedTarget(tileByPos.get(BlockPos.asLong(x, y, z)), legacyReferenceCore);
					entries.add(new Entry(new BlockPos(x - coreX, y - coreY, z - coreZ),
						block.defaultBlockState(), tile));
				}
			}
		}
		if (entries.isEmpty()) throw new IOException("Legacy schematic contains no known blocks");
		if (!hasLegacyCore && detectedCore != null) {
			coreX = detectedCore.getX();
			coreY = detectedCore.getY();
			coreZ = detectedCore.getZ();
			entries.clear();
			for (int y = 0; y < height; y++) {
				for (int z = 0; z < length; z++) {
					for (int x = 0; x < width; x++) {
						final int index = x + (y * length + z) * width;
						final Block block = legacyBlock(palette.getString(index));
						if (block != null && block != Blocks.AIR) {
							entries.add(new Entry(new BlockPos(x - coreX, y - coreY, z - coreZ),
								block.defaultBlockState(), tileByPos.get(BlockPos.asLong(x, y, z))));
						}
					}
				}
			}
		}
		return validated(root.getString("shipName"), new BlockPos(-coreX, -coreY, -coreZ),
			new BlockPos(width - coreX - 1, height - coreY - 1, length - coreZ - 1), entries,
			ship.contains("actualMass", Constants.NBT.TAG_INT)
				? ship.getInt("actualMass") : countMass(entries));
	}

	@Nullable
	private static Block legacyBlock(final String registryName) {
		if (registryName.startsWith("warpdrive:ship_core.")) return Registration.SHIP_CORE_BLOCK.get();
		if (registryName.startsWith("warpdrive:ship_controller.")) return Registration.SHIP_CONTROLLER_BLOCK.get();
		try {
			return ForgeRegistries.BLOCKS.getValue(new ResourceLocation(registryName));
		} catch (final RuntimeException exception) {
			WarpDrive.logger.warn("Skipping invalid legacy schematic block id {}", registryName);
			return null;
		}
	}

	private static ShipSchematic validated(final String shipName, final BlockPos min,
	                                       final BlockPos max, final List<Entry> entries,
	                                       final int mass)
		throws IOException {
		final long sizeX = (long) max.getX() - min.getX() + 1L;
		final long sizeY = (long) max.getY() - min.getY() + 1L;
		final long sizeZ = (long) max.getZ() - min.getZ() + 1L;
		if (sizeX <= 0 || sizeY <= 0 || sizeZ <= 0
		 || sizeX > ShipScanner.MAX_SHIP_SIDE + 1L
		 || sizeY > ShipScanner.MAX_SHIP_SIDE + 1L
		 || sizeZ > ShipScanner.MAX_SHIP_SIDE + 1L
		 || entries.isEmpty() || entries.size() > MAX_BLOCKS) {
			throw new IOException("Schematic exceeds supported ship limits");
		}
		final Set<Long> occupied = new HashSet<>(entries.size());
		for (final Entry entry : entries) {
			final BlockPos pos = entry.relativePos;
			if (pos.getX() < min.getX() || pos.getX() > max.getX()
			 || pos.getY() < min.getY() || pos.getY() > max.getY()
			 || pos.getZ() < min.getZ() || pos.getZ() > max.getZ()) {
				throw new IOException("Schematic block lies outside its declared bounds");
			}
			if (!occupied.add(pos.asLong())) {
				throw new IOException("Schematic contains duplicate block positions");
			}
		}
		return new ShipSchematic(shipName, min, max, entries,
			mass < 0 || mass > entries.size() ? countMass(entries) : mass);
	}

	private static int countMass(final List<Entry> entries) {
		int mass = 0;
		for (final Entry entry : entries) {
			if (!entry.blockState.is(WarpDriveTags.SHIP_NO_MASS)) mass++;
		}
		return mass;
	}

	private static int placementPriority(final Entry entry) {
		final BlockState state = entry.blockState;
		if (state.is(WarpDriveTags.PLACE_EARLIEST)) return 0;
		if (state.is(WarpDriveTags.PLACE_EARLIER)) return 1;
		if (state.is(WarpDriveTags.PLACE_NORMAL)) return 2;
		if (state.is(WarpDriveTags.PLACE_LATER)) return 3;
		if (state.is(WarpDriveTags.PLACE_LATEST)) return 4;
		return entry.blockEntityNbt == null ? 2 : 3;
	}

	@Nullable
	private static CompoundNBT normalizeLinkedTarget(@Nullable final CompoundNBT original,
	                                                 @Nonnull final BlockPos referenceCore) {
		if (original == null) return null;
		final CompoundNBT nbt = original.copy();
		if ("warpdrive:ship_scanner".equals(nbt.getString("id"))
		 && (nbt.getBoolean("targetConfigured")
		  || nbt.getInt("targetX") != 0 || nbt.getInt("targetY") != 0 || nbt.getInt("targetZ") != 0)) {
			nbt.putInt("targetX", nbt.getInt("targetX") - referenceCore.getX());
			nbt.putInt("targetY", nbt.getInt("targetY") - referenceCore.getY());
			nbt.putInt("targetZ", nbt.getInt("targetZ") - referenceCore.getZ());
			nbt.putBoolean("WarpDriveTargetRelative", true);
		}
		return nbt;
	}

	/** Restore schematic-relative links and tile-owned orientation before loading a block entity. */
	public static void preparePlacementNbt(@Nonnull final BlockState placedState,
	                                      @Nonnull final CompoundNBT nbt,
	                                      @Nonnull final BlockPos targetCore,
	                                      final int rotationSteps) {
		if (nbt.getBoolean("WarpDriveTargetRelative")) {
			final BlockPos relativeTarget = new BlockPos(
				nbt.getInt("targetX"), nbt.getInt("targetY"), nbt.getInt("targetZ"));
			final BlockPos target = targetCore.offset(rotateRelative(relativeTarget, rotationSteps));
			nbt.putInt("targetX", target.getX());
			nbt.putInt("targetY", target.getY());
			nbt.putInt("targetZ", target.getZ());
			nbt.remove("WarpDriveTargetRelative");
		}
		if (placedState.getBlock() instanceof ShipCoreBlock
		 && placedState.hasProperty(HorizontalBlock.FACING)) {
			nbt.putInt("Facing", placedState.getValue(HorizontalBlock.FACING).get3DDataValue());
		}
	}

	/** Remove identities which cannot be duplicated when a token instantiates a fresh ship. */
	public static void prepareInstantiatedNbt(@Nonnull final BlockState state,
	                                         @Nonnull final CompoundNBT nbt) {
		for (final String key : new String[]{
			"ComputerId", "Label", "computerID", "label", "UUIDMost", "UUIDLeast",
			"SignatureUUID", "oc:node", "jumpCount", "OwnerUUID", "ownerUUID" }) {
			nbt.remove(key);
		}
		if (nbt.contains("Owner", Constants.NBT.TAG_STRING)) nbt.putString("Owner", "None");
		if (nbt.contains("owner", Constants.NBT.TAG_STRING)) nbt.putString("owner", "None");
		if (state.getBlock() instanceof ShipCoreBlock) {
			nbt.putInt("Energy", 10_000_000);
			nbt.putInt("ShipState", 0);
			nbt.putInt("CountdownRemaining", 0);
			nbt.putInt("CooldownRemaining", 0);
		} else if (state.getBlock() instanceof CapacitorBlock) {
			nbt.putInt("energy", ((CapacitorBlock) state.getBlock()).getTier().getMaxEnergyStored());
		}
	}

	public static final class Entry {
		private final BlockPos relativePos;
		private final BlockState blockState;
		@Nullable
		private final CompoundNBT blockEntityNbt;

		private Entry(final BlockPos relativePos, final BlockState blockState,
		              @Nullable final CompoundNBT blockEntityNbt) {
			this.relativePos = relativePos.immutable();
			this.blockState = blockState;
			this.blockEntityNbt = blockEntityNbt == null ? null : blockEntityNbt.copy();
		}

		public BlockPos getRelativePos() {
			return relativePos;
		}

		public BlockState getBlockState() {
			return blockState;
		}

		@Nullable
		public CompoundNBT copyBlockEntityNbt() {
			return blockEntityNbt == null ? null : blockEntityNbt.copy();
		}
	}
}
