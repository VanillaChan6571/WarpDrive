package cr0s.warpdrive.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import cr0s.warpdrive.block.ShipCoreTileEntity;
import cr0s.warpdrive.block.ShipControllerTileEntity;
import cr0s.warpdrive.container.ShipControllerContainer;
import cr0s.warpdrive.network.ShipControllerActionPacket;
import cr0s.warpdrive.network.WarpDriveNetwork;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import org.lwjgl.glfw.GLFW;

/** Native cockpit screen for players who do not use CC:Tweaked. */
public class ShipControllerScreen extends ContainerScreen<ShipControllerContainer> {

	private static final int COLOUR_PANEL = 0xF00A1820;
	private static final int COLOUR_BORDER = 0xFF63C7E6;
	private static final int COLOUR_INNER = 0xFF102A36;
	private static final int COLOUR_TEXT = 0xFFE8F8FF;
	private static final int COLOUR_DIM = 0xFF8FB6C4;
	private static final int COLOUR_GOOD = 0xFF65E6A8;
	private static final int COLOUR_WARN = 0xFFFFCC66;
	private static final int COLOUR_BAD = 0xFFFF7070;
	private static final int LEGACY_NORMAL_BACKGROUND = 0xFF999999;
	private static final int LEGACY_NORMAL_TEXT = 0xFF111111;
	private static final int LEGACY_HEADER_BACKGROUND = 0xFF111111;
	private static final int LEGACY_HEADER_TEXT = 0xFFF2B233;
	private static final int LEGACY_HELP_BACKGROUND = 0xFF3366CC;
	private static final int LEGACY_STATUS_SUCCESS = 0xFF7FCC19;
	private static final int LEGACY_STATUS_WARNING = 0xFFCC4C4C;
	private static final int LEGACY_LIGHT_TEXT = 0xFFF0F0F0;
	private static final int TERMINAL_LEFT = 7;
	private static final int TERMINAL_TOP = 19;
	private static final int TERMINAL_ROW_HEIGHT = 11;
	private static final int TERMINAL_LAST_LINE = 19;

	private static final String[] ORIENTATIONS = { "north", "east", "south", "west" };
	private static final String[] DIMENSION_LABELS = { "Front", "Back", "Left", "Right", "Up", "Down" };
	private static final int[] LEGACY_DIMENSION_ORDER = { 0, 3, 4, 1, 2, 5 }; // Front, Right, Up, Back, Left, Down
	private static boolean keyboardModePreference = true;

	private enum KeyboardPage {
		START,
		CONTROLS,
		MOVEMENT,
		ROTATION,
		DIMENSIONS,
		NAME
	}

	private final TextFieldWidget[] dimensionFields = new TextFieldWidget[6];
	private final TextFieldWidget[] movementFields = new TextFieldWidget[3];
	private TextFieldWidget dimensionTargetField;
	private TextFieldWidget shipNameField;
	private Button orientationButton;
	private Button rotationButton;
	private Button applyButton;
	private Button boundsButton;
	private Button jumpButton;
	private Button abortButton;
	private Button modeButton;
	private TextFieldWidget commandField;
	private int orientationIndex = 2;
	private int rotationSteps;
	private int lastRevision = Integer.MIN_VALUE;
	private boolean fieldsInitialised;
	private boolean awaitingAppliedState;
	private boolean keyboardMode = keyboardModePreference;
	private KeyboardPage keyboardPage = KeyboardPage.CONTROLS;
	private boolean onboardingEvaluated;
	private boolean onboardingActive;
	private boolean onboardingNaming;
	private boolean movementByPosition;
	private int keyboardEntryIndex;
	private int keyboardEntryOriginal;
	private String keyboardInput = "";
	private final int[] keyboardMovement = new int[3];
	private final int[] keyboardAbsoluteTarget = new int[3]; // X, Z, Y, matching the legacy P page
	private final int[] keyboardDimensions = new int[6];
	private boolean jumpConfirmation;
	private String localFeedback = "";
	private String terminalFeedback = "";
	private int terminalFeedbackColour = COLOUR_DIM;
	private String lastTileFeedback = "";
	private int displayedShipState = -1;
	private int displayedCountdownTicks;
	private int displayedCooldownTicks;
	private int lastServerCountdownTicks = Integer.MIN_VALUE;
	private int lastServerCooldownTicks = Integer.MIN_VALUE;
	private float framePartialTicks;

	public ShipControllerScreen(final ShipControllerContainer container,
	                            final PlayerInventory inventory, final ITextComponent title) {
		super(container, inventory, title);
		imageWidth = 336;
		imageHeight = 248;
	}

	@Override
	protected void init() {
		super.init();
		final int x = leftPos + 10;

		for (int index = 0; index < dimensionFields.length; index++) {
			dimensionFields[index] = numberField(x + index * 50, topPos + 95, 44, 16, false);
		}
		for (int index = 0; index < movementFields.length; index++) {
			movementFields[index] = numberField(x + index * 58, topPos + 132, 52, 16, true);
		}

		orientationButton = addButton(new Button(x + 180, topPos + 131, 56, 18,
			new StringTextComponent("South"), button -> cycleOrientation()));
		rotationButton = addButton(new Button(x + 240, topPos + 131, 50, 18,
			new StringTextComponent("0°"), button -> cycleRotation()));

		dimensionTargetField = new TextFieldWidget(font, x + 74, topPos + 158, 242, 16,
			new StringTextComponent("destination dimension"));
		dimensionTargetField.setMaxLength(128);
		addButton(dimensionTargetField);

		shipNameField = new TextFieldWidget(font, x + 42, topPos + 180, 274, 16,
			new StringTextComponent("ship name"));
		shipNameField.setMaxLength(64);
		addButton(shipNameField);

		applyButton = addButton(new Button(x, topPos + 205, 64, 18,
			new StringTextComponent("Apply"), button -> applySettings()));
		boundsButton = addButton(new Button(x + 68, topPos + 205, 68, 18,
			new StringTextComponent("Bounds"), button -> sendSimple(ShipControllerActionPacket.TOGGLE_BOUNDS)));
		jumpButton = addButton(new Button(x + 140, topPos + 205, 72, 18,
			new StringTextComponent("Engage"), button -> sendSimple(ShipControllerActionPacket.JUMP)));
		abortButton = addButton(new Button(x + 216, topPos + 205, 74, 18,
			new StringTextComponent("Abort"), button -> sendSimple(ShipControllerActionPacket.ABORT)));

		commandField = new TextFieldWidget(font, x + 52, topPos + 216, imageWidth - 72, 16,
			new StringTextComponent("terminal command"));
		commandField.setMaxLength(256);
		addButton(commandField);
		modeButton = addButton(new Button(leftPos + imageWidth - 88, topPos + 3, 80, 14,
			new StringTextComponent(""), button -> toggleInputMode()));

		syncFromTile(true);
		updateModeWidgets();
	}

	private TextFieldWidget numberField(final int x, final int y, final int width,
	                                    final int maxDigits, final boolean signed) {
		final TextFieldWidget field = new TextFieldWidget(font, x, y, width, 16,
			new StringTextComponent("number"));
		field.setMaxLength(maxDigits);
		field.setFilter(value -> value.isEmpty() || (signed && "-".equals(value))
			|| value.matches(signed ? "-?[0-9]+" : "[0-9]+"));
		addButton(field);
		return field;
	}

	@Override
	public void tick() {
		super.tick();
		advanceDisplayedTimers();
		for (final TextFieldWidget field : dimensionFields) {
			field.tick();
		}
		for (final TextFieldWidget field : movementFields) {
			field.tick();
		}
		dimensionTargetField.tick();
		shipNameField.tick();
		commandField.tick();
		syncFromTile(false);
	}

	private void syncFromTile(final boolean force) {
		final ShipControllerTileEntity tileEntity = menu.getTileEntity();
		if (tileEntity == null) {
			setControlsActive(false);
			return;
		}
		synchroniseDisplayedTimers(tileEntity, force);

		final boolean changed = tileEntity.getRevision() != lastRevision;
		lastRevision = tileEntity.getRevision();
		if (changed && keyboardMode && !tileEntity.getFeedback().isEmpty()
		 && !tileEntity.getFeedback().equals(lastTileFeedback)) {
			lastTileFeedback = tileEntity.getFeedback();
			setTerminalFeedback(tileEntity.getFeedback(),
				tileEntity.isFeedbackSuccess() ? COLOUR_GOOD : COLOUR_BAD);
		}
		setControlsActive(tileEntity.isConnected());
		boundsButton.setMessage(new StringTextComponent(tileEntity.isBoundingBoxShown() ? "Hide box" : "Show box"));
		abortButton.active = tileEntity.isConnected()
			&& tileEntity.getShipState() == ShipCoreTileEntity.STATE_COUNTDOWN;
		jumpButton.active = tileEntity.isConnected()
			&& tileEntity.isAssemblyValid()
			&& tileEntity.getEnergyStored() >= tileEntity.getEnergyRequired()
			&& tileEntity.getShipState() == ShipCoreTileEntity.STATE_IDLE;

		if (tileEntity.isConnected() && (force || !fieldsInitialised || (changed && awaitingAppliedState))) {
			final int[] dimensions = tileEntity.getDimensions();
			for (int index = 0; index < dimensions.length; index++) {
				dimensionFields[index].setValue(Integer.toString(dimensions[index]));
			}
			final int[] movement = tileEntity.getMovement();
			for (int index = 0; index < movement.length; index++) {
				movementFields[index].setValue(Integer.toString(movement[index]));
			}
			orientationIndex = orientationIndex(tileEntity.getFacingName());
			rotationSteps = Math.floorMod(tileEntity.getRotationSteps(), 4);
			dimensionTargetField.setValue(tileEntity.getTargetDimension());
			shipNameField.setValue(tileEntity.getShipName());
			fieldsInitialised = true;
			awaitingAppliedState = false;
			updateCycleLabels();
			if (onboardingActive && !"Unnamed Ship".equals(tileEntity.getShipName())) {
				onboardingActive = false;
				onboardingNaming = false;
				keyboardPage = KeyboardPage.CONTROLS;
			}
			if (!onboardingEvaluated) {
				onboardingEvaluated = true;
				if (isNewShip(tileEntity)) {
					onboardingActive = true;
					keyboardMode = true;
					keyboardModePreference = true;
					keyboardPage = KeyboardPage.START;
					keyboardInput = "";
					terminalFeedback = "";
					updateModeWidgets();
				}
			}
		}
	}

	private static boolean isNewShip(final ShipControllerTileEntity tileEntity) {
		if (!"Unnamed Ship".equals(tileEntity.getShipName())) return false;
		for (final int dimension : tileEntity.getDimensions()) {
			if (dimension != 0) return false;
		}
		return true;
	}

	private void advanceDisplayedTimers() {
		if (displayedShipState == ShipCoreTileEntity.STATE_COUNTDOWN && displayedCountdownTicks > 0) {
			displayedCountdownTicks--;
		} else if (displayedShipState == ShipCoreTileEntity.STATE_COOLDOWN && displayedCooldownTicks > 0) {
			displayedCooldownTicks--;
		}
	}

	private void synchroniseDisplayedTimers(final ShipControllerTileEntity tileEntity, final boolean force) {
		final int serverState = tileEntity.getShipState();
		final int serverCountdown = tileEntity.getCountdownTicks();
		final int serverCooldown = tileEntity.getCooldownTicks();
		if (force || serverState != displayedShipState) {
			displayedShipState = serverState;
			displayedCountdownTicks = serverCountdown;
			displayedCooldownTicks = serverCooldown;
		} else {
			// A delayed packet must never make the visual countdown jump backwards.
			if (serverCountdown != lastServerCountdownTicks) {
				displayedCountdownTicks = Math.min(displayedCountdownTicks, serverCountdown);
			}
			if (serverCooldown != lastServerCooldownTicks) {
				displayedCooldownTicks = Math.min(displayedCooldownTicks, serverCooldown);
			}
		}
		lastServerCountdownTicks = serverCountdown;
		lastServerCooldownTicks = serverCooldown;
	}

	private void setControlsActive(final boolean active) {
		for (final TextFieldWidget field : dimensionFields) {
			field.setEditable(active);
		}
		for (final TextFieldWidget field : movementFields) {
			field.setEditable(active);
		}
		if (dimensionTargetField != null) {
			dimensionTargetField.setEditable(active);
			shipNameField.setEditable(active);
			orientationButton.active = active;
			rotationButton.active = active;
			applyButton.active = active;
			boundsButton.active = active;
			jumpButton.active = active;
			abortButton.active = active;
			commandField.setEditable(true);
		}
	}

	private void toggleInputMode() {
		keyboardMode = !keyboardMode;
		keyboardModePreference = keyboardMode;
		jumpConfirmation = false;
		keyboardPage = onboardingActive ? KeyboardPage.START : KeyboardPage.CONTROLS;
		onboardingNaming = false;
		keyboardInput = "";
		updateModeWidgets();
	}

	private void updateModeWidgets() {
		final boolean touchMode = !keyboardMode;
		for (final TextFieldWidget field : dimensionFields) {
			if (field != null) field.setVisible(touchMode);
		}
		for (final TextFieldWidget field : movementFields) {
			if (field != null) field.setVisible(touchMode);
		}
		if (dimensionTargetField == null) return;
		dimensionTargetField.setVisible(touchMode);
		shipNameField.setVisible(touchMode);
		orientationButton.visible = touchMode;
		rotationButton.visible = touchMode;
		applyButton.visible = touchMode;
		boundsButton.visible = touchMode;
		jumpButton.visible = touchMode;
		abortButton.visible = touchMode;
		// Keyboard mode is page-driven like the original Lua terminal, so it captures keys directly.
		// The text field remains an implementation detail for the older command parser and is hidden.
		commandField.setVisible(false);
		commandField.setFocus(false);
		if (keyboardMode) setFocused(null);
		modeButton.setMessage(new StringTextComponent(keyboardMode ? "Touch GUI" : "Keyboard"));
	}

	private void cycleOrientation() {
		orientationIndex = (orientationIndex + 1) % ORIENTATIONS.length;
		updateCycleLabels();
	}

	private void cycleRotation() {
		rotationSteps = (rotationSteps + 1) % 4;
		updateCycleLabels();
	}

	private void updateCycleLabels() {
		orientationButton.setMessage(new StringTextComponent(capitalise(ORIENTATIONS[orientationIndex])));
		rotationButton.setMessage(new StringTextComponent((rotationSteps * 90) + "°"));
	}

	private void applySettings() {
		try {
			final int[] dimensions = new int[6];
			for (int index = 0; index < dimensions.length; index++) {
				dimensions[index] = Integer.parseInt(dimensionFields[index].getValue());
			}
			final int[] movement = new int[3];
			for (int index = 0; index < movement.length; index++) {
				movement[index] = Integer.parseInt(movementFields[index].getValue());
			}
			if (shipNameField.getValue().trim().isEmpty()) {
				throw new NumberFormatException("empty ship name");
			}
			localFeedback = "Applying settings...";
			awaitingAppliedState = true;
			WarpDriveNetwork.CHANNEL.sendToServer(new ShipControllerActionPacket(menu.getPos(),
				ShipControllerActionPacket.APPLY, dimensions, movement, orientationIndex, rotationSteps,
				dimensionTargetField.getValue().trim(), shipNameField.getValue().trim()));
		} catch (final NumberFormatException exception) {
			localFeedback = "Enter valid whole numbers and a ship name";
		}
	}

	private void sendSimple(final int action) {
		localFeedback = action == ShipControllerActionPacket.JUMP ? "Requesting jump..." : "";
		WarpDriveNetwork.CHANNEL.sendToServer(ShipControllerActionPacket.simple(menu.getPos(), action));
	}

	@Override
	public boolean keyPressed(final int keyCode, final int scanCode, final int modifiers) {
		// ContainerScreen normally treats the inventory binding (E by default) as a second close key.
		// This controller uses single-letter terminal controls, so only Escape should dismiss it.
		if (minecraft != null && minecraft.options.keyInventory.matches(keyCode, scanCode)) {
			return true;
		}
		if (keyboardMode) {
			if (jumpConfirmation) {
				if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
					answerJumpConfirmation(true);
					return true;
				}
				return keyCode == GLFW.GLFW_KEY_ESCAPE || super.keyPressed(keyCode, scanCode, modifiers);
			}
			if (keyboardPage == KeyboardPage.START) {
				if (keyCode == GLFW.GLFW_KEY_1 || keyCode == GLFW.GLFW_KEY_KP_1) {
					beginOnboardingNamePage();
					return true;
				}
				return keyCode == GLFW.GLFW_KEY_ESCAPE || super.keyPressed(keyCode, scanCode, modifiers);
			}
			if (keyboardPage == KeyboardPage.ROTATION) {
				if (keyCode == GLFW.GLFW_KEY_UP) rotationSteps = 0;
				else if (keyCode == GLFW.GLFW_KEY_RIGHT) rotationSteps = 1;
				else if (keyCode == GLFW.GLFW_KEY_DOWN) rotationSteps = 2;
				else if (keyCode == GLFW.GLFW_KEY_LEFT) rotationSteps = 3;
				else if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
					updateCycleLabels();
					applySettings();
					jumpConfirmation = true;
					setTerminalFeedback("Engage jump drive? (Y/n)", COLOUR_WARN);
				}
				else return keyCode == GLFW.GLFW_KEY_ESCAPE || super.keyPressed(keyCode, scanCode, modifiers);
				return true;
			}
			if (keyboardPage == KeyboardPage.MOVEMENT
			 || keyboardPage == KeyboardPage.DIMENSIONS
			 || keyboardPage == KeyboardPage.NAME) {
				if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
					if (!keyboardInput.isEmpty()) keyboardInput = keyboardInput.substring(0, keyboardInput.length() - 1);
					return true;
				}
				if (keyCode == GLFW.GLFW_KEY_DELETE) {
					keyboardInput = "";
					return true;
				}
				if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
					submitKeyboardEntry();
					return true;
				}
				return keyCode == GLFW.GLFW_KEY_ESCAPE || super.keyPressed(keyCode, scanCode, modifiers);
			}
		}
		if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) && anyFieldFocused()) {
			applySettings();
			return true;
		}
		if (anyFieldFocused() && keyCode != GLFW.GLFW_KEY_ESCAPE) {
			return focusedFieldKeyPressed(keyCode, scanCode, modifiers)
				|| super.keyPressed(keyCode, scanCode, modifiers);
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean charTyped(final char character, final int modifiers) {
		if (!keyboardMode) {
			return super.charTyped(character, modifiers);
		}
		if (jumpConfirmation) {
			answerJumpConfirmation(character == 'y' || character == 'Y');
			return true;
		}
		if (keyboardPage == KeyboardPage.START) {
			if (character == '1') beginOnboardingNamePage();
			return true;
		}
		if (keyboardPage == KeyboardPage.CONTROLS) {
			handleControlCharacter(Character.toLowerCase(character));
			return true;
		}
		if (keyboardPage == KeyboardPage.NAME) {
			if (!Character.isISOControl(character) && keyboardInput.length() < 64) {
				keyboardInput += character;
			}
			return true;
		}
		if (keyboardPage == KeyboardPage.MOVEMENT || keyboardPage == KeyboardPage.DIMENSIONS) {
			if (character >= '0' && character <= '9' && keyboardInput.length() < 10) {
				keyboardInput += character;
			} else if (keyboardPage == KeyboardPage.MOVEMENT
				&& (character == '-' || character == 'n' || character == 'N')) {
				keyboardInput = keyboardInput.startsWith("-")
					? keyboardInput.substring(1) : "-" + keyboardInput;
			} else if (keyboardPage == KeyboardPage.MOVEMENT
				&& (character == '+' || character == 'p' || character == 'P')
				&& keyboardInput.startsWith("-")) {
				keyboardInput = keyboardInput.substring(1);
			}
			return true;
		}
		return true;
	}

	private void handleControlCharacter(final char character) {
		final ShipControllerTileEntity tileEntity = menu.getTileEntity();
		switch (character) {
			case 'm':
				beginMovementPage(false, tileEntity);
				break;
			case 'p':
				beginMovementPage(true, tileEntity);
				break;
			case 'i':
				beginDimensionsPage(tileEntity);
				break;
			case 'n':
				beginNamePage(tileEntity);
				break;
			case 'j':
				if (tileEntity != null && canEngage(tileEntity)) {
					jumpConfirmation = true;
					setTerminalFeedback("Engage jump drive? (Y/n)", COLOUR_WARN);
				} else if (tileEntity != null) {
					setTerminalFeedback(engageFailure(tileEntity), COLOUR_BAD);
				}
				break;
			case 'a':
				sendSimple(ShipControllerActionPacket.ABORT);
				setTerminalFeedback("Abort request sent", COLOUR_WARN);
				break;
			case 'b':
				sendSimple(ShipControllerActionPacket.TOGGLE_BOUNDS);
				setTerminalFeedback("Toggling ship bounds...", COLOUR_WARN);
				break;
			case 's':
				sendSimple(ShipControllerActionPacket.SCAN);
				setTerminalFeedback("Scanning ship...", COLOUR_WARN);
				break;
			case 't':
				toggleInputMode();
				break;
			case 'q':
				onClose();
				break;
			default:
				break;
		}
	}

	private void beginMovementPage(final boolean byPosition,
	                               final ShipControllerTileEntity tileEntity) {
		if (tileEntity == null || !tileEntity.isConnected()) {
			setTerminalFeedback("No adjacent Ship Core", COLOUR_BAD);
			return;
		}
		if (tileEntity.getShipVolume() <= 1) {
			setTerminalFeedback("dimensions not set", COLOUR_BAD);
			return;
		}

		movementByPosition = byPosition;
		System.arraycopy(tileEntity.getMovement(), 0, keyboardMovement, 0, keyboardMovement.length);
		if (byPosition) {
			final ShipCoreTileEntity core = tileEntity.getShipCore();
			final Direction facing = facing(tileEntity);
			if (core == null) {
				setTerminalFeedback("Ship Core position unavailable", COLOUR_BAD);
				return;
			}
			final BlockPos origin = core.getBlockPos();
			final int worldX = facing.getStepX() * keyboardMovement[0]
				- facing.getStepZ() * keyboardMovement[2];
			final int worldZ = facing.getStepZ() * keyboardMovement[0]
				+ facing.getStepX() * keyboardMovement[2];
			keyboardAbsoluteTarget[0] = origin.getX() + worldX;
			keyboardAbsoluteTarget[1] = origin.getZ() + worldZ;
			keyboardAbsoluteTarget[2] = origin.getY() + keyboardMovement[1];
		}
		keyboardEntryIndex = 0;
		keyboardPage = KeyboardPage.MOVEMENT;
		terminalFeedback = "";
		beginNumericEntry(currentMovementEntry());
	}

	private void beginDimensionsPage(final ShipControllerTileEntity tileEntity) {
		if (tileEntity == null || !tileEntity.isConnected()) {
			setTerminalFeedback("No adjacent Ship Core", COLOUR_BAD);
			return;
		}
		System.arraycopy(tileEntity.getDimensions(), 0, keyboardDimensions, 0, keyboardDimensions.length);
		keyboardEntryIndex = 0;
		keyboardPage = KeyboardPage.DIMENSIONS;
		terminalFeedback = "";
		beginNumericEntry(keyboardDimensions[LEGACY_DIMENSION_ORDER[0]]);
	}

	private void beginNamePage(final ShipControllerTileEntity tileEntity) {
		if (tileEntity == null || !tileEntity.isConnected()) {
			setTerminalFeedback("No adjacent Ship Core", COLOUR_BAD);
			return;
		}
		keyboardPage = KeyboardPage.NAME;
		onboardingNaming = false;
		keyboardEntryIndex = 0;
		keyboardInput = tileEntity.getShipName();
		terminalFeedback = "";
	}

	private void beginOnboardingNamePage() {
		final ShipControllerTileEntity tileEntity = menu.getTileEntity();
		if (tileEntity == null || !tileEntity.isConnected()) {
			setTerminalFeedback("No adjacent Ship Core", COLOUR_BAD);
			return;
		}
		keyboardPage = KeyboardPage.NAME;
		onboardingNaming = true;
		keyboardEntryIndex = 0;
		keyboardInput = "";
		terminalFeedback = "";
	}

	private void beginNumericEntry(final int current) {
		keyboardEntryOriginal = current;
		keyboardInput = current == 0 ? "" : Integer.toString(current);
	}

	private int currentMovementEntry() {
		return movementByPosition ? keyboardAbsoluteTarget[keyboardEntryIndex]
			: keyboardMovement[keyboardEntryIndex];
	}

	private void submitKeyboardEntry() {
		if (keyboardPage == KeyboardPage.NAME) {
			final String name = keyboardInput.trim();
			if (name.isEmpty()) {
				setTerminalFeedback("Ship name cannot be empty", COLOUR_BAD);
				return;
			}
			shipNameField.setValue(name);
			applySettings();
			if (onboardingNaming) {
				onboardingActive = false;
				onboardingNaming = false;
			}
			keyboardPage = KeyboardPage.CONTROLS;
			setTerminalFeedback("Applying ship name...", COLOUR_WARN);
			return;
		}

		final int entered;
		try {
			entered = keyboardInput.isEmpty() || "-".equals(keyboardInput)
				? keyboardEntryOriginal : Integer.parseInt(keyboardInput);
		} catch (final NumberFormatException exception) {
			setTerminalFeedback("Enter a valid whole number", COLOUR_BAD);
			return;
		}

		if (keyboardPage == KeyboardPage.DIMENSIONS) {
			if (entered < 0) {
				setTerminalFeedback("Dimensions cannot be negative", COLOUR_BAD);
				return;
			}
			keyboardDimensions[LEGACY_DIMENSION_ORDER[keyboardEntryIndex++]] = entered;
			if (keyboardEntryIndex < keyboardDimensions.length) {
				beginNumericEntry(keyboardDimensions[LEGACY_DIMENSION_ORDER[keyboardEntryIndex]]);
				return;
			}
			for (int index = 0; index < keyboardDimensions.length; index++) {
				dimensionFields[index].setValue(Integer.toString(keyboardDimensions[index]));
			}
			applySettings();
			keyboardPage = KeyboardPage.CONTROLS;
			setTerminalFeedback("Applying ship dimensions...", COLOUR_WARN);
			return;
		}

		final ShipControllerTileEntity tileEntity = menu.getTileEntity();
		if (tileEntity == null || !tileEntity.isConnected()) {
			keyboardPage = KeyboardPage.CONTROLS;
			setTerminalFeedback("No adjacent Ship Core", COLOUR_BAD);
			return;
		}
		final int offset = movementByPosition ? movementOffset(tileEntity, keyboardEntryIndex) : 0;
		final long delta = (long) entered - offset;
		final long maximum = (long) movementShipLength(tileEntity, keyboardEntryIndex)
			+ tileEntity.getMaximumRange();
		if (Math.abs(delta) > maximum) {
			setTerminalFeedback("Wrong distance. Try again.", COLOUR_BAD);
			return;
		}

		if (movementByPosition) keyboardAbsoluteTarget[keyboardEntryIndex] = entered;
		else keyboardMovement[keyboardEntryIndex] = entered;
		keyboardEntryIndex++;
		terminalFeedback = "";
		if (keyboardEntryIndex < keyboardMovement.length) {
			beginNumericEntry(currentMovementEntry());
			return;
		}

		if (movementByPosition) {
			final ShipCoreTileEntity core = tileEntity.getShipCore();
			final Direction facing = facing(tileEntity);
			if (core == null) {
				keyboardPage = KeyboardPage.CONTROLS;
				setTerminalFeedback("Ship Core position unavailable", COLOUR_BAD);
				return;
			}
			final BlockPos origin = core.getBlockPos();
			final int worldX = keyboardAbsoluteTarget[0] - origin.getX();
			final int worldZ = keyboardAbsoluteTarget[1] - origin.getZ();
			keyboardMovement[0] = facing.getStepX() * worldX + facing.getStepZ() * worldZ;
			keyboardMovement[1] = keyboardAbsoluteTarget[2] - origin.getY();
			keyboardMovement[2] = -facing.getStepZ() * worldX + facing.getStepX() * worldZ;
		}
		for (int index = 0; index < keyboardMovement.length; index++) {
			movementFields[index].setValue(Integer.toString(keyboardMovement[index]));
		}
		rotationSteps = Math.floorMod(tileEntity.getRotationSteps(), 4);
		keyboardPage = KeyboardPage.ROTATION;
		keyboardInput = "";
	}

	private int movementOffset(final ShipControllerTileEntity tileEntity, final int axis) {
		final ShipCoreTileEntity core = tileEntity.getShipCore();
		if (core == null) return 0;
		if (axis == 0) return core.getBlockPos().getX();
		if (axis == 1) return core.getBlockPos().getZ();
		return core.getBlockPos().getY();
	}

	private int movementShipLength(final ShipControllerTileEntity tileEntity, final int axis) {
		final int[] dimensions = tileEntity.getDimensions();
		final int lengthFB = dimensions[0] + dimensions[1] + 1;
		final int lengthLR = dimensions[2] + dimensions[3] + 1;
		final int lengthUD = dimensions[4] + dimensions[5] + 1;
		if (!movementByPosition) {
			return axis == 0 ? lengthFB : axis == 1 ? lengthUD : lengthLR;
		}
		if (axis == 2) return lengthUD;
		final boolean bowAlongX = facing(tileEntity).getStepX() != 0;
		return axis == 0 ? (bowAlongX ? lengthFB : lengthLR) : (bowAlongX ? lengthLR : lengthFB);
	}

	private static Direction facing(final ShipControllerTileEntity tileEntity) {
		final Direction facing = Direction.byName(tileEntity.getFacingName());
		return facing == null ? Direction.SOUTH : facing;
	}

	private void answerJumpConfirmation(final boolean confirmed) {
		jumpConfirmation = false;
		keyboardPage = KeyboardPage.CONTROLS;
		if (confirmed) {
			sendSimple(ShipControllerActionPacket.JUMP);
			setTerminalFeedback("Jump request sent", COLOUR_WARN);
		} else {
			setTerminalFeedback("Jump cancelled", COLOUR_DIM);
		}
	}

	private void executeCommand(final String rawCommand) {
		final String commandLine = rawCommand == null ? "" : rawCommand.trim();
		if (commandLine.isEmpty()) return;
		final String[] commandAndArguments = commandLine.split("\\s+", 2);
		final String command = commandAndArguments[0].toLowerCase(java.util.Locale.ROOT);
		final String arguments = commandAndArguments.length > 1 ? commandAndArguments[1].trim() : "";

		if (jumpConfirmation) {
			jumpConfirmation = false;
			if ("yes".equals(command) || "y".equals(command)) {
				sendSimple(ShipControllerActionPacket.JUMP);
				setTerminalFeedback("Jump request sent", COLOUR_WARN);
			} else {
				setTerminalFeedback("Jump cancelled", COLOUR_DIM);
			}
			return;
		}

		if ("help".equals(command) || "h".equals(command) || "?".equals(command)) {
			setTerminalFeedback("Commands are listed above the prompt", COLOUR_DIM);
			return;
		}
		if ("touch".equals(command)) {
			toggleInputMode();
			return;
		}

		final ShipControllerTileEntity tileEntity = menu.getTileEntity();
		if (tileEntity == null || !tileEntity.isConnected()) {
			setTerminalFeedback("No adjacent Ship Core", COLOUR_BAD);
			return;
		}

		try {
			switch (command) {
				case "scan":
				case "status":
				case "refresh":
				case "s":
				case "r":
					sendSimple(ShipControllerActionPacket.SCAN);
					setTerminalFeedback("Scanning ship...", COLOUR_WARN);
					break;
				case "jump":
				case "engage":
				case "j":
					if (!canEngage(tileEntity)) {
						setTerminalFeedback(engageFailure(tileEntity), COLOUR_BAD);
						break;
					}
					jumpConfirmation = true;
					setTerminalFeedback("WARNING: type YES to confirm the warp jump", COLOUR_WARN);
					break;
				case "abort":
				case "a":
					sendSimple(ShipControllerActionPacket.ABORT);
					setTerminalFeedback("Abort request sent", COLOUR_WARN);
					break;
				case "bounds":
				case "b":
					sendSimple(ShipControllerActionPacket.TOGGLE_BOUNDS);
					setTerminalFeedback("Toggling ship bounds...", COLOUR_WARN);
					break;
				case "move":
				case "m":
					setMovementCommand(arguments);
					break;
				case "destination":
				case "position":
				case "d":
				case "p":
					setDestinationCommand(arguments, tileEntity);
					break;
				case "dimensions":
				case "size":
				case "i":
					setDimensionsCommand(arguments);
					break;
				case "face":
				case "facing":
					setFacingCommand(arguments);
					break;
				case "turn":
				case "rotation":
					setRotationCommand(arguments);
					break;
				case "dimension":
				case "dim":
					requireArguments(arguments, "dimension <id>");
					dimensionTargetField.setValue(arguments);
					applyFromTerminal();
					break;
				case "name":
				case "n":
					requireArguments(arguments, "name <ship name>");
					shipNameField.setValue(arguments);
					applyFromTerminal();
					break;
				case "apply":
					applyFromTerminal();
					break;
				default:
					setTerminalFeedback("Unknown command '" + command + "' - type help", COLOUR_BAD);
			}
		} catch (final IllegalArgumentException exception) {
			setTerminalFeedback(exception.getMessage(), COLOUR_BAD);
		}
	}

	private void setMovementCommand(final String arguments) {
		final String[] values = requireValues(arguments, 3, "move <forward> <up> <right>");
		for (int index = 0; index < movementFields.length; index++) {
			movementFields[index].setValue(Integer.toString(Integer.parseInt(values[index])));
		}
		applyFromTerminal();
	}

	private void setDestinationCommand(final String arguments,
	                                   final ShipControllerTileEntity tileEntity) {
		final String[] values = arguments.split("\\s+", 4);
		if (values.length < 3) throw new IllegalArgumentException("destination <x> <y> <z> [dimension]");
		final ShipCoreTileEntity core = tileEntity.getShipCore();
		final Direction facing = Direction.byName(tileEntity.getFacingName());
		if (core == null || facing == null) throw new IllegalArgumentException("Ship Core position unavailable");
		final BlockPos corePos = core.getBlockPos();
		final int worldX = Integer.parseInt(values[0]) - corePos.getX();
		final int worldY = Integer.parseInt(values[1]) - corePos.getY();
		final int worldZ = Integer.parseInt(values[2]) - corePos.getZ();
		final int forward = facing.getStepX() * worldX + facing.getStepZ() * worldZ;
		final int right = -facing.getStepZ() * worldX + facing.getStepX() * worldZ;
		movementFields[0].setValue(Integer.toString(forward));
		movementFields[1].setValue(Integer.toString(worldY));
		movementFields[2].setValue(Integer.toString(right));
		if (values.length == 4) dimensionTargetField.setValue(values[3]);
		applyFromTerminal();
	}

	private void setDimensionsCommand(final String arguments) {
		final String[] values = requireValues(arguments, 6, "dimensions <front> <back> <left> <right> <up> <down>");
		for (int index = 0; index < dimensionFields.length; index++) {
			dimensionFields[index].setValue(Integer.toString(Integer.parseInt(values[index])));
		}
		applyFromTerminal();
	}

	private void setFacingCommand(final String arguments) {
		final int requested = orientationIndex(arguments);
		if (!ORIENTATIONS[requested].equalsIgnoreCase(arguments)) {
			throw new IllegalArgumentException("face <north|east|south|west>");
		}
		orientationIndex = requested;
		updateCycleLabels();
		applyFromTerminal();
	}

	private void setRotationCommand(final String arguments) {
		final int degrees = Integer.parseInt(arguments);
		if (Math.floorMod(degrees, 90) != 0) {
			throw new IllegalArgumentException("turn <0|90|180|270>");
		}
		rotationSteps = Math.floorMod(degrees / 90, 4);
		updateCycleLabels();
		applyFromTerminal();
	}

	private void applyFromTerminal() {
		applySettings();
		setTerminalFeedback(localFeedback, COLOUR_WARN);
	}

	private static String[] requireValues(final String arguments, final int count, final String usage) {
		final String[] values = arguments.trim().isEmpty() ? new String[0] : arguments.trim().split("\\s+");
		if (values.length != count) throw new IllegalArgumentException(usage);
		return values;
	}

	private static void requireArguments(final String arguments, final String usage) {
		if (arguments.isEmpty()) throw new IllegalArgumentException(usage);
	}

	private void setTerminalFeedback(final String message, final int colour) {
		terminalFeedback = message == null ? "" : message;
		terminalFeedbackColour = colour;
	}

	private double displayedCountdownSeconds() {
		return Math.max(0.0D, (displayedCountdownTicks - framePartialTicks) / 20.0D);
	}

	private double displayedCooldownSeconds() {
		return Math.max(0.0D, (displayedCooldownTicks - framePartialTicks) / 20.0D);
	}

	private static boolean canEngage(final ShipControllerTileEntity tileEntity) {
		return tileEntity.isConnected() && tileEntity.isAssemblyValid()
			&& tileEntity.getEnergyStored() >= tileEntity.getEnergyRequired()
			&& tileEntity.getShipState() == ShipCoreTileEntity.STATE_IDLE;
	}

	private static String engageFailure(final ShipControllerTileEntity tileEntity) {
		if (!tileEntity.isAssemblyValid()) return tileEntity.getAssemblyMessage();
		if (tileEntity.getEnergyStored() < tileEntity.getEnergyRequired()) return "Insufficient energy";
		return "Ship is not idle";
	}

	private boolean anyFieldFocused() {
		for (final TextFieldWidget field : dimensionFields) {
			if (field.isFocused()) return true;
		}
		for (final TextFieldWidget field : movementFields) {
			if (field.isFocused()) return true;
		}
		return dimensionTargetField.isFocused() || shipNameField.isFocused();
	}

	private boolean focusedFieldKeyPressed(final int keyCode, final int scanCode, final int modifiers) {
		for (final TextFieldWidget field : dimensionFields) {
			if (field.isFocused()) return field.keyPressed(keyCode, scanCode, modifiers);
		}
		for (final TextFieldWidget field : movementFields) {
			if (field.isFocused()) return field.keyPressed(keyCode, scanCode, modifiers);
		}
		if (dimensionTargetField.isFocused()) return dimensionTargetField.keyPressed(keyCode, scanCode, modifiers);
		return shipNameField.isFocused() && shipNameField.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public void render(final MatrixStack matrixStack, final int mouseX, final int mouseY,
	                   final float partialTicks) {
		framePartialTicks = partialTicks;
		renderBackground(matrixStack);
		super.render(matrixStack, mouseX, mouseY, partialTicks);
		renderTooltip(matrixStack, mouseX, mouseY);
	}

	@Override
	protected void renderBg(final MatrixStack matrixStack, final float partialTicks,
	                        final int mouseX, final int mouseY) {
		if (keyboardMode) {
			fill(matrixStack, leftPos - 2, topPos - 2, leftPos + imageWidth + 2, topPos + imageHeight + 2,
				0xFFD8D8D8);
			fill(matrixStack, leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF303030);
			fill(matrixStack, leftPos + TERMINAL_LEFT, topPos + TERMINAL_TOP,
				leftPos + imageWidth - TERMINAL_LEFT, topPos + terminalLineTop(TERMINAL_LAST_LINE + 1),
				LEGACY_NORMAL_BACKGROUND);
			fillTerminalRow(matrixStack, 1, LEGACY_HEADER_BACKGROUND);

			if (keyboardPage == KeyboardPage.START) {
				fillTerminalRow(matrixStack, 6, LEGACY_HELP_BACKGROUND);
			} else if (keyboardPage == KeyboardPage.CONTROLS) {
				fillTerminalRows(matrixStack, 16, 17, LEGACY_HELP_BACKGROUND);
			} else if (keyboardPage == KeyboardPage.MOVEMENT) {
				final int inputLine = 4 + keyboardEntryIndex * 2;
				fillTerminalRows(matrixStack, inputLine + 2, inputLine + 4, LEGACY_HELP_BACKGROUND);
			} else if (keyboardPage == KeyboardPage.ROTATION) {
				fillTerminalRows(matrixStack, 8, 10, LEGACY_HELP_BACKGROUND);
			} else if (keyboardPage == KeyboardPage.DIMENSIONS) {
				fillTerminalRows(matrixStack, 14, 17, LEGACY_HELP_BACKGROUND);
			} else if (keyboardPage == KeyboardPage.NAME) {
				fillTerminalRow(matrixStack, 5, LEGACY_HELP_BACKGROUND);
			}

			if (jumpConfirmation || displayedShipState == ShipCoreTileEntity.STATE_COUNTDOWN
			 || displayedShipState == ShipCoreTileEntity.STATE_COOLDOWN || !terminalFeedback.isEmpty()) {
				final int statusBackground = jumpConfirmation
					|| displayedShipState == ShipCoreTileEntity.STATE_COUNTDOWN ? LEGACY_STATUS_WARNING
					: displayedShipState == ShipCoreTileEntity.STATE_COOLDOWN ? LEGACY_HELP_BACKGROUND
					: terminalFeedbackColour == COLOUR_GOOD ? LEGACY_STATUS_SUCCESS
					: terminalFeedbackColour == COLOUR_DIM ? LEGACY_NORMAL_BACKGROUND : LEGACY_STATUS_WARNING;
				fillTerminalRow(matrixStack, TERMINAL_LAST_LINE, statusBackground);
			}
			return;
		}
		fill(matrixStack, leftPos - 1, topPos - 1, leftPos + imageWidth + 1, topPos + imageHeight + 1, COLOUR_BORDER);
		fill(matrixStack, leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, COLOUR_PANEL);
		fill(matrixStack, leftPos + 7, topPos + 19, leftPos + imageWidth - 7,
			topPos + 78, COLOUR_INNER);
	}

	@Override
	protected void renderLabels(final MatrixStack matrixStack, final int mouseX, final int mouseY) {
		final ShipControllerTileEntity tileEntity = menu.getTileEntity();
		if (keyboardMode) {
			renderKeyboardLabels(matrixStack, tileEntity);
			return;
		}
		font.draw(matrixStack, trim(tileEntity == null ? title.getString() : tileEntity.getShipName(), 230),
			8.0F, 7.0F, COLOUR_TEXT);

		if (tileEntity == null || !tileEntity.isConnected()) {
			font.draw(matrixStack, "DISCONNECTED", 10.0F, 25.0F, COLOUR_BAD);
			font.draw(matrixStack, "Place beside a Ship Core", 10.0F, 39.0F, COLOUR_DIM);
		} else {
			final boolean enoughEnergy = tileEntity.getEnergyStored() >= tileEntity.getEnergyRequired();
			final String status;
			final int statusColour;
			if (!tileEntity.isAssemblyValid()) {
				status = "ASSEMBLY INVALID";
				statusColour = COLOUR_BAD;
			} else if (displayedShipState == ShipCoreTileEntity.STATE_COUNTDOWN) {
				status = String.format(java.util.Locale.ROOT, "JUMPING IN %.1fs", displayedCountdownSeconds());
				statusColour = COLOUR_WARN;
			} else if (displayedShipState == ShipCoreTileEntity.STATE_COOLDOWN) {
				status = String.format(java.util.Locale.ROOT, "COOLDOWN %.1fs", displayedCooldownSeconds());
				statusColour = COLOUR_WARN;
			} else if (!enoughEnergy) {
				status = "INSUFFICIENT ENERGY";
				statusColour = COLOUR_BAD;
			} else {
				status = "READY TO JUMP";
				statusColour = COLOUR_GOOD;
			}
			font.draw(matrixStack, trim(status, 145), 10.0F, 24.0F, statusColour);
			final String energy = String.format("Energy %,d / %,d FE", tileEntity.getEnergyStored(),
				tileEntity.getEnergyRequired());
			font.draw(matrixStack, energy, imageWidth - 10.0F - font.width(energy), 24.0F,
				enoughEnergy ? COLOUR_GOOD : COLOUR_BAD);
			font.draw(matrixStack, String.format("Blocks %,d  Envelope %,d  Range %,d",
				tileEntity.getShipMass(), tileEntity.getShipVolume(), tileEntity.getMaximumRange()),
				10.0F, 38.0F, COLOUR_DIM);
			final String integrity = (tileEntity.isAssemblyValid() ? "Integrity: " : "Integrity error: ")
				+ tileEntity.getAssemblyMessage();
			final String[] lines = wrap(integrity, imageWidth - 20);
			for (int index = 0; index < lines.length; index++) {
				font.draw(matrixStack, lines[index], 10.0F, 52.0F + index * 11.0F,
					tileEntity.isAssemblyValid() ? COLOUR_GOOD : COLOUR_BAD);
			}
		}

		for (int index = 0; index < DIMENSION_LABELS.length; index++) {
			font.draw(matrixStack, DIMENSION_LABELS[index], 10.0F + index * 50, 85.0F, COLOUR_DIM);
		}
		font.draw(matrixStack, "Forward", 10.0F, 122.0F, COLOUR_DIM);
		font.draw(matrixStack, "Up", 68.0F, 122.0F, COLOUR_DIM);
		font.draw(matrixStack, "Right", 126.0F, 122.0F, COLOUR_DIM);
		font.draw(matrixStack, "Bow", 190.0F, 122.0F, COLOUR_DIM);
		font.draw(matrixStack, "Turn", 250.0F, 122.0F, COLOUR_DIM);
		font.draw(matrixStack, "Destination:", 10.0F, 162.0F, COLOUR_DIM);
		font.draw(matrixStack, "Name:", 10.0F, 184.0F, COLOUR_DIM);

		String message = localFeedback;
		int colour = COLOUR_WARN;
		if (tileEntity != null && !tileEntity.getFeedback().isEmpty()) {
			message = tileEntity.getFeedback();
			colour = tileEntity.isFeedbackSuccess() ? COLOUR_GOOD : COLOUR_BAD;
		}
		font.draw(matrixStack, trim(message, imageWidth - 20), 10.0F, 232.0F, colour);
	}

	private void renderKeyboardLabels(final MatrixStack matrixStack,
	                                  final ShipControllerTileEntity tileEntity) {
		switch (keyboardPage) {
			case START:
				renderStartPage(matrixStack);
				break;
			case MOVEMENT:
				renderMovementPage(matrixStack, tileEntity);
				break;
			case ROTATION:
				renderRotationPage(matrixStack);
				break;
			case DIMENSIONS:
				renderDimensionsPage(matrixStack);
				break;
			case NAME:
				renderNamePage(matrixStack, tileEntity);
				break;
			case CONTROLS:
			default:
				renderControlsPage(matrixStack, tileEntity);
				break;
		}
		if (jumpConfirmation) {
			drawTerminalLine(matrixStack, TERMINAL_LAST_LINE, " " + "Engage jump drive? (Y/n)", LEGACY_LIGHT_TEXT);
		} else if (displayedShipState == ShipCoreTileEntity.STATE_COUNTDOWN) {
			drawTerminalLine(matrixStack, TERMINAL_LAST_LINE,
				String.format(java.util.Locale.ROOT, " WARP IN %.1fs - press A to abort", displayedCountdownSeconds()),
				LEGACY_LIGHT_TEXT);
		} else if (displayedShipState == ShipCoreTileEntity.STATE_COOLDOWN) {
			drawTerminalLine(matrixStack, TERMINAL_LAST_LINE,
				String.format(java.util.Locale.ROOT, " Drive cooling down: %.1fs", displayedCooldownSeconds()),
				LEGACY_LIGHT_TEXT);
		} else if (!terminalFeedback.isEmpty()) {
			final int colour = terminalFeedbackColour == COLOUR_DIM ? LEGACY_NORMAL_TEXT : LEGACY_LIGHT_TEXT;
			drawTerminalLine(matrixStack, TERMINAL_LAST_LINE, " " + terminalFeedback, colour);
		}
	}

	private void renderStartPage(final MatrixStack matrixStack) {
		drawTerminalHeader(matrixStack, "<==== Ship controller ====>");
		drawTerminalLine(matrixStack, 3, "This is a keyboard controlled user interface.", LEGACY_NORMAL_TEXT);
		drawTerminalLine(matrixStack, 4, "Key controls are written like so:", LEGACY_NORMAL_TEXT);
		drawTerminalLine(matrixStack, 6, " [ 1 ]  Press 1 to start", LEGACY_LIGHT_TEXT);
	}

	private void renderControlsPage(final MatrixStack matrixStack,
	                                final ShipControllerTileEntity tileEntity) {
		drawTerminalHeader(matrixStack, (tileEntity == null ? "No Ship Core" : tileEntity.getShipName())
			+ " - Ship controls");
		if (tileEntity == null || !tileEntity.isConnected()) {
			drawTerminalLine(matrixStack, 3, "No ship controller detected", LEGACY_STATUS_WARNING);
			drawTerminalLine(matrixStack, 4, "Place this block beside a Ship Core.", LEGACY_NORMAL_TEXT);
			drawTerminalControls(matrixStack);
			return;
		}

		final ShipCoreTileEntity core = tileEntity.getShipCore();
		final BlockPos position = core == null ? menu.getPos() : core.getBlockPos();
		drawTerminalLine(matrixStack, 3, "Ship:", LEGACY_NORMAL_TEXT);
		drawTerminalLine(matrixStack, 4, " Current position = " + formatInteger(position.getX()) + ", "
			+ formatInteger(position.getY()) + ", " + formatInteger(position.getZ()), LEGACY_NORMAL_TEXT);
		drawTerminalLine(matrixStack, 5, " Energy           = " + formatInteger(tileEntity.getEnergyStored())
			+ " FE", tileEntity.getEnergyStored() >= tileEntity.getEnergyRequired()
				? LEGACY_NORMAL_TEXT : LEGACY_STATUS_WARNING);

		drawTerminalLine(matrixStack, 7, "Dimensions:", LEGACY_NORMAL_TEXT);
		final int[] dimensions = tileEntity.getDimensions();
		drawTerminalLine(matrixStack, 8, " Front, Right, Up = " + formatInteger(dimensions[0]) + ", "
			+ formatInteger(dimensions[3]) + ", " + formatInteger(dimensions[4]) + " blocks", LEGACY_NORMAL_TEXT);
		drawTerminalLine(matrixStack, 9, " Back, Left, Down = " + formatInteger(dimensions[1]) + ", "
			+ formatInteger(dimensions[2]) + ", " + formatInteger(dimensions[5]) + " blocks", LEGACY_NORMAL_TEXT);
		drawTerminalLine(matrixStack, 10, " Mass, Volume     = " + formatInteger(tileEntity.getShipMass())
			+ " blocks, " + formatInteger(tileEntity.getShipVolume()) + " envelope", LEGACY_NORMAL_TEXT);

		if (tileEntity.isAssemblyValid()) {
			final int[] movement = tileEntity.getMovement();
			final int distance = (int) Math.ceil(Math.sqrt((double) movement[0] * movement[0]
				+ (double) movement[1] * movement[1] + (double) movement[2] * movement[2]));
			final int jumps = tileEntity.getEnergyRequired() <= 0 ? 0
				: tileEntity.getEnergyStored() / tileEntity.getEnergyRequired();
			drawTerminalLine(matrixStack, 12, "Warp data:", LEGACY_NORMAL_TEXT);
			drawTerminalLine(matrixStack, 13, movementDescription(" Movement         = ", movement,
				tileEntity.getRotationSteps()), LEGACY_NORMAL_TEXT);
			drawTerminalLine(matrixStack, 14, " Distance         = " + formatInteger(distance) + " m ("
				+ formatInteger(tileEntity.getEnergyRequired()) + " FE, " + jumps + " jumps)", LEGACY_NORMAL_TEXT);
			drawTerminalLine(matrixStack, 15, " " + targetDescription(tileEntity, movement), LEGACY_NORMAL_TEXT);
		} else {
			drawTerminalLine(matrixStack, 12, trim("Integrity error: " + tileEntity.getAssemblyMessage(),
				imageWidth - 28), LEGACY_STATUS_WARNING);
		}
		drawTerminalControls(matrixStack);
	}

	private void drawTerminalControls(final MatrixStack matrixStack) {
		drawTerminalLine(matrixStack, 16, " set ship Name (N), dImensions (I), Movement (M/P)", LEGACY_LIGHT_TEXT);
		drawTerminalLine(matrixStack, 17, " Jump (J), Scan (S), Abort (A), Touch GUI (T)", LEGACY_LIGHT_TEXT);
	}

	private void renderMovementPage(final MatrixStack matrixStack,
	                                final ShipControllerTileEntity tileEntity) {
		drawTerminalHeader(matrixStack, "<==== Set ship movement ====>");
		if (tileEntity == null) return;
		drawTerminalLine(matrixStack, 3, movementDescription("Current movement is ", keyboardMovement,
			tileEntity.getRotationSteps()), LEGACY_NORMAL_TEXT);

		final int inputLine = 4 + keyboardEntryIndex * 2;
		final String[] localAxes = { "Forward/back", "Up/down", "Right/left" };
		final String[] worldAxes = { "X", "Z", "Y" };
		final String[] positives = movementByPosition
			? new String[]{ "East", "South", "Up" } : new String[]{ "Forward", "Up", "Right" };
		final String[] negatives = movementByPosition
			? new String[]{ "West", "North", "Down" } : new String[]{ "Backward", "Down", "Left" };
		for (int step = 0; step <= keyboardEntryIndex; step++) {
			final String value = step == keyboardEntryIndex ? keyboardInput + "_"
				: Integer.toString(movementByPosition ? keyboardAbsoluteTarget[step] : keyboardMovement[step]);
			drawTerminalLine(matrixStack, 4 + step * 2,
				(movementByPosition ? worldAxes[step] : localAxes[step]) + " movement: " + value,
				LEGACY_NORMAL_TEXT);
		}

		final long offset = movementByPosition ? movementOffset(tileEntity, keyboardEntryIndex) : 0;
		final long shipLength = movementShipLength(tileEntity, keyboardEntryIndex);
		final long maximum = shipLength + tileEntity.getMaximumRange();
		drawTerminalLine(matrixStack, inputLine + 2, " Enter between " + formatInteger(offset + shipLength + 1)
			+ " and " + formatInteger(offset + maximum) + " to move " + positives[keyboardEntryIndex] + ".",
			LEGACY_LIGHT_TEXT);
		drawTerminalLine(matrixStack, inputLine + 3, " Enter " + formatInteger(offset)
			+ " to keep position on this axis.", LEGACY_LIGHT_TEXT);
		drawTerminalLine(matrixStack, inputLine + 4, " Enter between " + formatInteger(offset - maximum)
			+ " and " + formatInteger(offset - shipLength - 1) + " to move " + negatives[keyboardEntryIndex]
			+ ".", LEGACY_LIGHT_TEXT);
	}

	private void renderRotationPage(final MatrixStack matrixStack) {
		drawTerminalHeader(matrixStack, "<==== Set ship rotation ====>");
		final String rotation = rotationSteps == 0 ? "Front    " : rotationSteps == 1 ? "Right +90"
			: rotationSteps == 2 ? "Back 180 " : "Left -90 ";
		drawTerminalLine(matrixStack, 3, " Rotation         = " + rotation, LEGACY_NORMAL_TEXT);
		drawTerminalLine(matrixStack, 8, " Select ship rotation (Up, Down, Left, Right).", LEGACY_LIGHT_TEXT);
		drawTerminalLine(matrixStack, 9, " Select Front to keep current orientation.", LEGACY_LIGHT_TEXT);
		drawTerminalLine(matrixStack, 10, " Press Enter to save your selection.", LEGACY_LIGHT_TEXT);
	}

	private void renderDimensionsPage(final MatrixStack matrixStack) {
		drawTerminalHeader(matrixStack, "<==== Set ship dimensions ====>");
		for (int step = 0; step < LEGACY_DIMENSION_ORDER.length; step++) {
			final int dimensionIndex = LEGACY_DIMENSION_ORDER[step];
			final String value = step == keyboardEntryIndex ? keyboardInput + "_"
				: Integer.toString(keyboardDimensions[dimensionIndex]);
			drawTerminalLine(matrixStack, 3 + step, String.format(" %-5s (%s) : %s",
				DIMENSION_LABELS[dimensionIndex], formatInteger(keyboardDimensions[dimensionIndex]), value),
				LEGACY_NORMAL_TEXT);
		}
		drawTerminalLine(matrixStack, 14, " Enter ship size in blocks.", LEGACY_LIGHT_TEXT);
		drawTerminalLine(matrixStack, 15, " First block next to Ship counts as 1.", LEGACY_LIGHT_TEXT);
		drawTerminalLine(matrixStack, 17, " Press Enter to save each selection.", LEGACY_LIGHT_TEXT);
	}

	private void renderNamePage(final MatrixStack matrixStack,
	                            final ShipControllerTileEntity tileEntity) {
		drawTerminalHeader(matrixStack, "<==== Set ship name ====>");
		if (onboardingNaming) {
			drawTerminalLine(matrixStack, 3, "Enter ship name: " + keyboardInput + "_", LEGACY_NORMAL_TEXT);
		} else {
			drawTerminalLine(matrixStack, 3, " Name (" + (tileEntity == null ? "" : tileEntity.getShipName())
				+ ") : " + keyboardInput + "_", LEGACY_NORMAL_TEXT);
		}
		drawTerminalLine(matrixStack, 5, " Press Enter to validate.", LEGACY_LIGHT_TEXT);
	}

	private void drawTerminalHeader(final MatrixStack matrixStack, final String text) {
		final String trimmed = trim(text, imageWidth - 100);
		font.draw(matrixStack, trimmed, (imageWidth - font.width(trimmed)) / 2.0F,
			terminalTextY(1), LEGACY_HEADER_TEXT);
	}

	private void drawTerminalLine(final MatrixStack matrixStack, final int line,
	                              final String text, final int colour) {
		font.draw(matrixStack, trim(text, imageWidth - 20), TERMINAL_LEFT + 3.0F, terminalTextY(line), colour);
	}

	private void fillTerminalRows(final MatrixStack matrixStack, final int firstLine,
	                              final int lastLine, final int colour) {
		for (int line = firstLine; line <= lastLine; line++) fillTerminalRow(matrixStack, line, colour);
	}

	private void fillTerminalRow(final MatrixStack matrixStack, final int line, final int colour) {
		fill(matrixStack, leftPos + TERMINAL_LEFT, topPos + terminalLineTop(line),
			leftPos + imageWidth - TERMINAL_LEFT, topPos + terminalLineTop(line + 1), colour);
	}

	private static int terminalLineTop(final int line) {
		return TERMINAL_TOP + (line - 1) * TERMINAL_ROW_HEIGHT;
	}

	private static float terminalTextY(final int line) {
		return terminalLineTop(line) + 1.0F;
	}

	private static String formatInteger(final long value) {
		return String.format(java.util.Locale.ROOT, "%,d", value);
	}

	private static String movementDescription(final String prefix, final int[] movement,
	                                         final int rotation) {
		final StringBuilder result = new StringBuilder(prefix);
		int count = 0;
		if (movement[0] != 0) {
			result.append(formatInteger(Math.abs((long) movement[0])))
				.append(movement[0] > 0 ? " front" : " back");
			count++;
		}
		if (movement[1] != 0) {
			if (count++ > 0) result.append(", ");
			result.append(formatInteger(Math.abs((long) movement[1])))
				.append(movement[1] > 0 ? " up" : " down");
		}
		if (movement[2] != 0) {
			if (count++ > 0) result.append(", ");
			result.append(formatInteger(Math.abs((long) movement[2])))
				.append(movement[2] > 0 ? " right" : " left");
		}
		if (rotation != 0) {
			if (count++ > 0) result.append(", ");
			result.append(rotation == 1 ? "Turn right" : rotation == 2 ? "Turn back" : "Turn left");
		}
		if (count == 0) result.append("(none)");
		return result.toString();
	}

	private String targetDescription(final ShipControllerTileEntity tileEntity, final int[] movement) {
		final ShipCoreTileEntity core = tileEntity.getShipCore();
		final Direction facing = Direction.byName(tileEntity.getFacingName());
		final String dimension = tileEntity.getTargetDimension().isEmpty()
			? "current dimension" : tileEntity.getTargetDimension();
		if (core == null || facing == null) return "Target dimension: " + dimension;
		final BlockPos origin = core.getBlockPos();
		final int worldX = facing.getStepX() * movement[0] - facing.getStepZ() * movement[2];
		final int worldZ = facing.getStepZ() * movement[0] + facing.getStepX() * movement[2];
		return String.format("Target: %d, %d, %d | %s", origin.getX() + worldX,
			origin.getY() + movement[1], origin.getZ() + worldZ, dimension);
	}

	private String[] wrap(final String value, final int width) {
		final String text = value == null ? "" : value.trim();
		final String firstFit = font.plainSubstrByWidth(text, width);
		if (firstFit.length() >= text.length()) {
			return new String[]{ firstFit };
		}

		int breakAt = firstFit.lastIndexOf(' ');
		if (breakAt <= 0) {
			breakAt = firstFit.length();
		}
		final String first = text.substring(0, breakAt).trim();
		final String remainder = text.substring(breakAt).trim();
		String second = font.plainSubstrByWidth(remainder, width);
		if (second.length() < remainder.length()) {
			second = font.plainSubstrByWidth(remainder, width - font.width("...")) + "...";
		}
		return new String[]{ first, second };
	}

	private String trim(final String value, final int width) {
		return font.plainSubstrByWidth(value == null ? "" : value, width);
	}

	private static int orientationIndex(final String name) {
		for (int index = 0; index < ORIENTATIONS.length; index++) {
			if (ORIENTATIONS[index].equalsIgnoreCase(name)) return index;
		}
		return 2;
	}

	private static String capitalise(final String value) {
		return value.substring(0, 1).toUpperCase() + value.substring(1);
	}
}
