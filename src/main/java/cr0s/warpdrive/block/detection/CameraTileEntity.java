package cr0s.warpdrive.block.detection;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IVideoChannel;
import cr0s.warpdrive.data.CameraType;
import cr0s.warpdrive.data.GlobalRegionRegistry;
import cr0s.warpdrive.data.Registration;
import cr0s.warpdrive.data.VideoChannelRegistry;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Video camera with the legacy optional entity-recognition sensor. */
public class CameraTileEntity extends TileEntity implements ITickableTileEntity, IVideoChannel {

	public static final int MAX_RECOGNITION_UPGRADES = 8;
	public static final int RANGE_PER_UPGRADE = 8;
	private static final int SENSING_INTERVAL_TICKS = 20;
	private static final int REGISTRY_INTERVAL_TICKS = 15 * 20;
	private static final String TAG_RESULTS = "results";
	private static final String TAG_UPGRADES = "recognitionUpgrades";

	private int videoChannel = -1;
	private int recognitionUpgrades;
	private int sensingTicks;
	private int registryTicks;
	private final List<RecognitionResult> results = new ArrayList<>();
	private final Set<RecognitionListener> listeners = new HashSet<>();

	public CameraTileEntity() {
		super(Registration.CAMERA_TILE.get());
	}

	@Override
	public void tick() {
		if (level == null) return;
		if (level.isClientSide) {
			if (--registryTicks <= 0) {
				registryTicks = REGISTRY_INTERVAL_TICKS;
				VideoChannelRegistry.update(level, getBlockPos(), videoChannel, CameraType.SIMPLE);
			}
			return;
		}
		if (recognitionUpgrades <= 0 || --sensingTicks > 0) return;
		sensingTicks = SENSING_INTERVAL_TICKS;
		scanEntities();
	}

	private void scanEntities() {
		if (level == null) return;
		final int countOld = results.size();
		final Direction facing = getBlockState().hasProperty(CameraBlock.FACING)
		                       ? getBlockState().getValue(CameraBlock.FACING) : Direction.NORTH;
		final double range = recognitionUpgrades * RANGE_PER_UPGRADE;
		final double radius = range / 2.0D;
		final Vector3d camera = Vector3d.atCenterOf(getBlockPos()).add(
			facing.getStepX() * 0.6D, facing.getStepY() * 0.6D, facing.getStepZ() * 0.6D);
		final Vector3d center = Vector3d.atCenterOf(getBlockPos()).add(
			facing.getStepX() * (radius + 0.5D),
			facing.getStepY() * (radius + 0.5D),
			facing.getStepZ() * (radius + 0.5D));
		final AxisAlignedBB bounds = new AxisAlignedBB(
			center.x - radius, center.y - radius, center.z - radius,
			center.x + radius, center.y + radius, center.z + radius);

		final Map<UUID, RecognitionResult> oldById = new HashMap<>();
		for (final RecognitionResult result : results) oldById.put(result.uniqueId, result);
		final List<RecognitionResult> next = new ArrayList<>();
		int added = 0;
		for (final Entity entity : level.getEntities((Entity) null, bounds, CameraTileEntity::isRecognizable)) {
			final RayTraceResult obstruction = level.clip(new RayTraceContext(camera,
				entity.getEyePosition(1.0F), RayTraceContext.BlockMode.COLLIDER,
				RayTraceContext.FluidMode.NONE, entity));
			if (obstruction.getType() != RayTraceResult.Type.MISS) continue;
			final boolean crewMember = entity instanceof PlayerEntity && level instanceof ServerWorld
				&& GlobalRegionRegistry.isCrewMember((ServerWorld) level, worldPosition,
					(PlayerEntity) entity);
			RecognitionResult result = oldById.remove(entity.getUUID());
			if (result == null) {
				result = new RecognitionResult(entity, crewMember);
				added++;
			} else {
				result.update(entity, crewMember);
			}
			next.add(result);
		}
		final int removed = oldById.size();
		results.clear();
		results.addAll(next);
		if (countOld > 0 || !next.isEmpty()) setChanged();
		if (added > 0 || removed > 0) {
			queueEvent("opticalSensorResultsChanged", added, removed);
		}
	}

	private static boolean isRecognizable(@Nullable final Entity entity) {
		return entity != null && entity.isAlive() && !entity.isInvisible()
		    && (!(entity instanceof PlayerEntity) || !entity.isSpectator());
	}

	@Override
	public int getVideoChannel() { return videoChannel; }

	@Override
	public void setVideoChannel(final int requestedChannel) {
		if (!IVideoChannel.isValid(requestedChannel) || videoChannel == requestedChannel) return;
		videoChannel = requestedChannel;
		registryTicks = 0;
		setChangedAndSync();
	}

	public Object[] setOrGetVideoChannel(@Nullable final Integer requestedChannel) {
		if (requestedChannel != null) setVideoChannel(requestedChannel);
		return new Object[]{ videoChannel };
	}

	public int getRecognitionUpgrades() { return recognitionUpgrades; }

	public boolean addRecognitionUpgrade() {
		if (recognitionUpgrades >= MAX_RECOGNITION_UPGRADES) return false;
		recognitionUpgrades++;
		sensingTicks = 0;
		setChangedAndSync();
		return true;
	}

	public boolean removeRecognitionUpgrade() {
		if (recognitionUpgrades <= 0) return false;
		recognitionUpgrades--;
		if (recognitionUpgrades == 0 && !results.isEmpty()) {
			final int removed = results.size();
			results.clear();
			queueEvent("opticalSensorResultsChanged", 0, removed);
		}
		setChangedAndSync();
		return true;
	}

	public Object[] getResults() {
		final Object[] values = new Object[results.size()];
		for (int index = 0; index < results.size(); index++) {
			values[index] = results.get(index).toArray(false);
		}
		return values;
	}

	public Object[] getResultsCount() { return new Object[]{ results.size() }; }

	public Object[] getResult(final int index) {
		return index >= 0 && index < results.size()
		     ? results.get(index).toArray(true)
		     : new Object[]{ false, "ERR", "ERR", 0, 0, 0, 0, 0, 0, false };
	}

	public void addRecognitionListener(final RecognitionListener listener) { listeners.add(listener); }
	public void removeRecognitionListener(final RecognitionListener listener) { listeners.remove(listener); }

	private void queueEvent(final String eventName, final Object... arguments) {
		for (final RecognitionListener listener : new ArrayList<>(listeners)) {
			try {
				listener.queueEvent(eventName, arguments);
			} catch (final RuntimeException exception) {
				WarpDrive.logger.warn("Unable to deliver {} from camera at {}",
					eventName, getBlockPos(), exception);
			}
		}
	}

	private void setChangedAndSync() {
		setChanged();
		if (level != null && !level.isClientSide) {
			level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
		}
	}

	@Override
	public void load(@Nonnull final BlockState blockState, @Nonnull final CompoundNBT tagCompound) {
		super.load(blockState, tagCompound);
		final int storedChannel = tagCompound.getInt("frequency")
		                        + tagCompound.getInt(IVideoChannel.VIDEO_CHANNEL_TAG);
		videoChannel = IVideoChannel.isValid(storedChannel) ? storedChannel : -1;
		int storedUpgrades = tagCompound.getInt(TAG_UPGRADES);
		if (storedUpgrades == 0 && tagCompound.contains("upgrades", Constants.NBT.TAG_COMPOUND)) {
			storedUpgrades = tagCompound.getCompound("upgrades")
				.getByte("camera.recognition_range");
		}
		recognitionUpgrades = Math.max(0, Math.min(MAX_RECOGNITION_UPGRADES, storedUpgrades));
		results.clear();
		final ListNBT resultTags = tagCompound.getList(TAG_RESULTS, Constants.NBT.TAG_COMPOUND);
		for (int index = 0; index < resultTags.size(); index++) {
			final CompoundNBT resultTag = resultTags.getCompound(index);
			if (resultTag.hasUUID("uniqueId")) results.add(new RecognitionResult(resultTag));
		}
		registryTicks = 0;
	}

	@Nonnull
	@Override
	public CompoundNBT save(@Nonnull final CompoundNBT tagCompound) {
		super.save(tagCompound);
		if (IVideoChannel.isValid(videoChannel)) {
			tagCompound.putInt(IVideoChannel.VIDEO_CHANNEL_TAG, videoChannel);
		}
		tagCompound.putInt(TAG_UPGRADES, recognitionUpgrades);
		if (!results.isEmpty()) {
			final ListNBT resultTags = new ListNBT();
			for (final RecognitionResult result : results) resultTags.add(result.save());
			tagCompound.put(TAG_RESULTS, resultTags);
		}
		return tagCompound;
	}

	@Nonnull
	@Override
	public CompoundNBT getUpdateTag() { return save(new CompoundNBT()); }

	@Nullable
	@Override
	public SUpdateTileEntityPacket getUpdatePacket() {
		return new SUpdateTileEntityPacket(getBlockPos(), 1, getUpdateTag());
	}

	@Override
	public void onDataPacket(final NetworkManager networkManager,
	                         final SUpdateTileEntityPacket packet) {
		load(getBlockState(), packet.getTag());
	}

	@Override
	public void setRemoved() {
		if (level != null) VideoChannelRegistry.remove(level, getBlockPos());
		listeners.clear();
		super.setRemoved();
	}

	@Override
	public void onChunkUnloaded() {
		if (level != null) VideoChannelRegistry.remove(level, getBlockPos());
		super.onChunkUnloaded();
	}

	public interface RecognitionListener {
		void queueEvent(String eventName, Object... arguments);
	}

	private static final class RecognitionResult {
		private final UUID uniqueId;
		private String type;
		private String name;
		private Vector3d position;
		private Vector3d motion;
		private boolean crewMember;

		private RecognitionResult(final Entity entity, final boolean crewMember) {
			uniqueId = entity.getUUID();
			update(entity, crewMember);
		}

		private RecognitionResult(final CompoundNBT tag) {
			uniqueId = tag.getUUID("uniqueId");
			type = tag.getString("type");
			name = tag.getString("name");
			position = new Vector3d(tag.getDouble("posX"), tag.getDouble("posY"), tag.getDouble("posZ"));
			motion = new Vector3d(tag.getDouble("motionX"), tag.getDouble("motionY"), tag.getDouble("motionZ"));
			crewMember = tag.getBoolean("isCrewMember");
		}

		private void update(final Entity entity, final boolean crewMember) {
			final ResourceLocation registryName = ForgeRegistries.ENTITIES.getKey(entity.getType());
			type = registryName == null ? "minecraft:unknown" : registryName.toString();
			name = entity.getName().getString();
			position = entity.getEyePosition(1.0F);
			motion = entity.getDeltaMovement();
			this.crewMember = crewMember;
		}

		private Object[] toArray(final boolean includeSuccess) {
			final Object[] values = {
				type, name, position.x, position.y, position.z,
				motion.x, motion.y, motion.z, crewMember
			};
			if (!includeSuccess) return values;
			final Object[] withSuccess = new Object[values.length + 1];
			withSuccess[0] = true;
			System.arraycopy(values, 0, withSuccess, 1, values.length);
			return withSuccess;
		}

		private CompoundNBT save() {
			final CompoundNBT tag = new CompoundNBT();
			tag.putUUID("uniqueId", uniqueId);
			tag.putString("type", type);
			tag.putString("name", name);
			tag.putDouble("posX", position.x);
			tag.putDouble("posY", position.y);
			tag.putDouble("posZ", position.z);
			tag.putDouble("motionX", motion.x);
			tag.putDouble("motionY", motion.y);
			tag.putDouble("motionZ", motion.z);
			tag.putBoolean("isCrewMember", crewMember);
			return tag;
		}
	}
}
