package cr0s.warpdrive.block;

import cr0s.warpdrive.container.ShipControllerContainer;
import cr0s.warpdrive.data.Registration;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

/** Server-owned status mirror and action bridge for the native Ship Controller screen. */
public class ShipControllerTileEntity extends TileEntity implements ITickableTileEntity,
		net.minecraft.inventory.container.INamedContainerProvider {

	private boolean connected;
	private boolean assemblyValid;
	private boolean boundingBoxShown;
	private boolean feedbackSuccess = true;
	private String shipName = "No Ship Core";
	private String stateText = "Disconnected";
	private String assemblyMessage = "Place this controller adjacent to a Ship Core";
	private String facingName = "south";
	private String targetDimension = "";
	private String feedback = "";
	private int dimFront;
	private int dimBack;
	private int dimLeft;
	private int dimRight;
	private int dimUp;
	private int dimDown;
	private int moveX;
	private int moveY;
	private int moveZ;
	private int rotationSteps;
	private int shipMass;
	private int shipVolume;
	private int energyStored;
	private int energyRequired;
	private int maximumRange;
	private int shipState;
	private int countdownTicks;
	private int cooldownTicks;
	private int revision;
	private String lastSyncSignature = "";

	public ShipControllerTileEntity() {
		super(Registration.SHIP_CONTROLLER_TILE.get());
	}

	@Override
	public void tick() {
		if (level == null || level.isClientSide) {
			return;
		}
		// Spread controllers across the second instead of refreshing all of them on one server tick.
		if ((level.getGameTime() + worldPosition.asLong()) % 20L == 0L) {
			refreshAndSync();
		}
	}

	@Nullable
	public ShipCoreTileEntity getShipCore() {
		if (level == null) {
			return null;
		}
		for (final Direction direction : Direction.values()) {
			final TileEntity tileEntity = level.getBlockEntity(worldPosition.relative(direction));
			if (tileEntity instanceof ShipCoreTileEntity) {
				return (ShipCoreTileEntity) tileEntity;
			}
		}
		return null;
	}

	public void refreshAndSync() {
		if (level == null || level.isClientSide) {
			return;
		}
		final ShipCoreTileEntity core = getShipCore();
		connected = core != null;

		if (core == null) {
			shipName = "No Ship Core";
			stateText = "Disconnected";
			assemblyValid = false;
			assemblyMessage = "Place this controller adjacent to a Ship Core";
			shipMass = 0;
			shipVolume = 0;
			energyStored = 0;
			energyRequired = 0;
			maximumRange = 0;
			shipState = 0;
			countdownTicks = 0;
			cooldownTicks = 0;
			dimFront = 0;
			dimBack = 0;
			dimLeft = 0;
			dimRight = 0;
			dimUp = 0;
			dimDown = 0;
			moveX = 0;
			moveY = 0;
			moveZ = 0;
			rotationSteps = 0;
			facingName = "south";
			targetDimension = "";
		} else {
			shipName = core.getName();
			final Object[] state = core.state();
			stateText = String.valueOf(state[0]);
			final Object[] assembly = core.getAssemblyStatus();
			assemblyValid = Boolean.TRUE.equals(assembly[0]);
			assemblyMessage = String.valueOf(assembly[1]);
			final Object[] dimensions = core.getDimensions();
			dimFront = ((Number) dimensions[0]).intValue();
			dimBack = ((Number) dimensions[1]).intValue();
			dimLeft = ((Number) dimensions[2]).intValue();
			dimRight = ((Number) dimensions[3]).intValue();
			dimUp = ((Number) dimensions[4]).intValue();
			dimDown = ((Number) dimensions[5]).intValue();
			final Object[] movement = core.getMovement();
			moveX = ((Number) movement[0]).intValue();
			moveY = ((Number) movement[1]).intValue();
			moveZ = ((Number) movement[2]).intValue();
			facingName = core.getFacingDirection().getName();
			rotationSteps = core.getRotationStepsValue();
			targetDimension = core.getConfiguredTargetDimension();
			boundingBoxShown = core.isBoundingBoxShown();
			final Object[] size = core.getShipSize();
			shipMass = ((Number) size[0]).intValue();
			shipVolume = ((Number) size[1]).intValue();
			energyStored = core.getEnergyStored();
			energyRequired = core.getEnergyRequiredValue();
			// Match the 1.12.2 Lua controller: the displayed movement allowance is the
			// movement-type range. Energy is reported separately and checked at engagement.
			maximumRange = core.getMaxJumpDistanceByType();
			final Object[] timers = core.getJumpTimers();
			shipState = ((Number) timers[0]).intValue();
			countdownTicks = ((Number) timers[1]).intValue();
			cooldownTicks = ((Number) timers[2]).intValue();
		}

		final String signature = snapshotSignature();
		if (!signature.equals(lastSyncSignature)) {
			lastSyncSignature = signature;
			revision++;
			setChanged();
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
		}
	}

	public void applySettings(final int front, final int back, final int left, final int right,
	                          final int up, final int down, final int dx, final int dy, final int dz,
	                          final String orientation, final int rotation, final String dimension,
	                          final String name) {
		final ShipCoreTileEntity core = getShipCore();
		if (core == null) {
			setFeedback(false, "No adjacent Ship Core");
			return;
		}

		Object[] result = core.setDimensions(front, back, left, right, up, down);
		if (!isSuccess(result)) {
			setFeedback(false, resultMessage(result));
			return;
		}
		result = core.setOrientation(orientation);
		if (!isSuccess(result)) {
			setFeedback(false, resultMessage(result));
			return;
		}
		core.rotationSteps(Optional.of(rotation));
		result = core.setTargetDimension(dimension);
		if (!isSuccess(result)) {
			setFeedback(false, resultMessage(result));
			return;
		}
		result = core.setName(name);
		if (!isSuccess(result)) {
			setFeedback(false, resultMessage(result));
			return;
		}
		result = core.setMovement(dx, dy, dz);
		setFeedback(isSuccess(result), resultMessage(result));
	}

	public void startJump() {
		final ShipCoreTileEntity core = getShipCore();
		final Object[] result = core == null
			? new Object[]{ false, "No adjacent Ship Core" }
			: core.jump();
		setFeedback(isSuccess(result), resultMessage(result));
	}

	public void scanShip() {
		final ShipCoreTileEntity core = getShipCore();
		if (core == null) {
			setFeedback(false, "No adjacent Ship Core");
			return;
		}
		final Object[] result = core.scan();
		final boolean success = isSuccess(result);
		final String message = result != null && result.length > 2
			? String.valueOf(result[2]) : "Scan failed";
		setFeedback(success, message);
	}

	public void abortJump() {
		final ShipCoreTileEntity core = getShipCore();
		final Object[] result = core == null
			? new Object[]{ false, "No adjacent Ship Core" }
			: core.abortJump();
		setFeedback(isSuccess(result), resultMessage(result));
	}

	public void toggleBounds(final PlayerEntity player) {
		final ShipCoreTileEntity core = getShipCore();
		if (core == null) {
			setFeedback(false, "No adjacent Ship Core");
			return;
		}
		core.toggleBoundingBoxDisplay(player);
		setFeedback(true, core.isBoundingBoxShown() ? "Bounding boxes shown" : "Bounding boxes hidden");
	}

	private void setFeedback(final boolean success, final String message) {
		feedbackSuccess = success;
		feedback = message == null ? "" : message;
		lastSyncSignature = "";
		refreshAndSync();
	}

	private static boolean isSuccess(final Object[] result) {
		return result != null && result.length > 0 && Boolean.TRUE.equals(result[0]);
	}

	private static String resultMessage(final Object[] result) {
		return result != null && result.length > 1 ? String.valueOf(result[1]) : "Operation failed";
	}

	private String snapshotSignature() {
		return connected + "|" + assemblyValid + "|" + boundingBoxShown + "|" + feedbackSuccess
			+ "|" + shipName + "|" + stateText + "|" + assemblyMessage + "|" + facingName
			+ "|" + targetDimension + "|" + feedback + "|" + dimFront + ',' + dimBack + ','
			+ dimLeft + ',' + dimRight + ',' + dimUp + ',' + dimDown + "|" + moveX + ','
			+ moveY + ',' + moveZ + "|" + rotationSteps + "|" + shipMass + "|" + shipVolume
			+ "|" + energyStored + "|" + energyRequired + "|" + maximumRange + "|" + shipState
			+ "|" + countdownTicks + "|" + cooldownTicks;
	}

	@Nonnull
	@Override
	public ITextComponent getDisplayName() {
		return new TranslationTextComponent("block.warpdrive.ship_controller");
	}

	@Nullable
	@Override
	public Container createMenu(final int windowId, @Nonnull final PlayerInventory inventory,
	                            @Nonnull final PlayerEntity player) {
		return new ShipControllerContainer(windowId, inventory, worldPosition);
	}

	@Nonnull
	@Override
	public CompoundNBT save(@Nonnull final CompoundNBT nbt) {
		return super.save(nbt);
	}

	@Override
	public void load(@Nonnull final BlockState state, @Nonnull final CompoundNBT nbt) {
		super.load(state, nbt);
		if (nbt.contains("ControllerSnapshot")) {
			readSnapshot(nbt.getCompound("ControllerSnapshot"));
		}
	}

	private void writeSnapshot(final CompoundNBT nbt) {
		nbt.putBoolean("Connected", connected);
		nbt.putBoolean("AssemblyValid", assemblyValid);
		nbt.putBoolean("Bounds", boundingBoxShown);
		nbt.putBoolean("FeedbackSuccess", feedbackSuccess);
		nbt.putString("ShipName", shipName);
		nbt.putString("StateText", stateText);
		nbt.putString("AssemblyMessage", assemblyMessage);
		nbt.putString("Facing", facingName);
		nbt.putString("TargetDimension", targetDimension);
		nbt.putString("Feedback", feedback);
		nbt.putInt("DimFront", dimFront);
		nbt.putInt("DimBack", dimBack);
		nbt.putInt("DimLeft", dimLeft);
		nbt.putInt("DimRight", dimRight);
		nbt.putInt("DimUp", dimUp);
		nbt.putInt("DimDown", dimDown);
		nbt.putInt("MoveX", moveX);
		nbt.putInt("MoveY", moveY);
		nbt.putInt("MoveZ", moveZ);
		nbt.putInt("Rotation", rotationSteps);
		nbt.putInt("Mass", shipMass);
		nbt.putInt("Volume", shipVolume);
		nbt.putInt("Energy", energyStored);
		nbt.putInt("EnergyRequired", energyRequired);
		nbt.putInt("MaximumRange", maximumRange);
		nbt.putInt("ShipState", shipState);
		nbt.putInt("Countdown", countdownTicks);
		nbt.putInt("Cooldown", cooldownTicks);
		nbt.putInt("Revision", revision);
	}

	private void readSnapshot(final CompoundNBT nbt) {
		connected = nbt.getBoolean("Connected");
		assemblyValid = nbt.getBoolean("AssemblyValid");
		boundingBoxShown = nbt.getBoolean("Bounds");
		feedbackSuccess = nbt.getBoolean("FeedbackSuccess");
		shipName = nbt.getString("ShipName");
		stateText = nbt.getString("StateText");
		assemblyMessage = nbt.getString("AssemblyMessage");
		facingName = nbt.getString("Facing");
		targetDimension = nbt.getString("TargetDimension");
		feedback = nbt.getString("Feedback");
		dimFront = nbt.getInt("DimFront");
		dimBack = nbt.getInt("DimBack");
		dimLeft = nbt.getInt("DimLeft");
		dimRight = nbt.getInt("DimRight");
		dimUp = nbt.getInt("DimUp");
		dimDown = nbt.getInt("DimDown");
		moveX = nbt.getInt("MoveX");
		moveY = nbt.getInt("MoveY");
		moveZ = nbt.getInt("MoveZ");
		rotationSteps = nbt.getInt("Rotation");
		shipMass = nbt.getInt("Mass");
		shipVolume = nbt.getInt("Volume");
		energyStored = nbt.getInt("Energy");
		energyRequired = nbt.getInt("EnergyRequired");
		maximumRange = nbt.getInt("MaximumRange");
		shipState = nbt.getInt("ShipState");
		countdownTicks = nbt.getInt("Countdown");
		cooldownTicks = nbt.getInt("Cooldown");
		revision = nbt.getInt("Revision");
	}

	@Nonnull
	@Override
	public CompoundNBT getUpdateTag() {
		final CompoundNBT nbt = save(new CompoundNBT());
		final CompoundNBT snapshot = new CompoundNBT();
		writeSnapshot(snapshot);
		nbt.put("ControllerSnapshot", snapshot);
		return nbt;
	}

	@Nullable
	@Override
	public SUpdateTileEntityPacket getUpdatePacket() {
		return new SUpdateTileEntityPacket(worldPosition, 2, getUpdateTag());
	}

	@Override
	public void onDataPacket(final NetworkManager network, final SUpdateTileEntityPacket packet) {
		load(getBlockState(), packet.getTag());
	}

	@Override
	public void handleUpdateTag(final BlockState state, final CompoundNBT nbt) {
		load(state, nbt);
	}

	public boolean isConnected() { return connected; }
	public boolean isAssemblyValid() { return assemblyValid; }
	public boolean isBoundingBoxShown() { return boundingBoxShown; }
	public boolean isFeedbackSuccess() { return feedbackSuccess; }
	public String getShipName() { return shipName; }
	public String getStateText() { return stateText; }
	public String getAssemblyMessage() { return assemblyMessage; }
	public String getFacingName() { return facingName; }
	public String getTargetDimension() { return targetDimension; }
	public String getFeedback() { return feedback; }
	public int[] getDimensions() { return new int[]{ dimFront, dimBack, dimLeft, dimRight, dimUp, dimDown }; }
	public int[] getMovement() { return new int[]{ moveX, moveY, moveZ }; }
	public int getRotationSteps() { return rotationSteps; }
	public int getShipMass() { return shipMass; }
	public int getShipVolume() { return shipVolume; }
	public int getEnergyStored() { return energyStored; }
	public int getEnergyRequired() { return energyRequired; }
	public int getMaximumRange() { return maximumRange; }
	public int getShipState() { return shipState; }
	public int getCountdownTicks() { return countdownTicks; }
	public int getCooldownTicks() { return cooldownTicks; }
	public int getRevision() { return revision; }
}
