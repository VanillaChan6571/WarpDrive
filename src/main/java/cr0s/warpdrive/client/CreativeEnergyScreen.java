package cr0s.warpdrive.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import cr0s.warpdrive.block.CreativeEnergyTileEntity;
import cr0s.warpdrive.container.CreativeEnergyContainer;
import cr0s.warpdrive.network.SetCreativeEnergyPacket;
import cr0s.warpdrive.network.WarpDriveNetwork;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.widget.AbstractSlider;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

/**
 * Configuration screen for the creative energy source.
 *
 * Drawn entirely with fill()/text calls - there is no background texture, which keeps the mod free
 * of placeholder art.
 */
public class CreativeEnergyScreen extends ContainerScreen<CreativeEnergyContainer> {

	/** The slider covers the useful range; larger values go in the text box. */
	private static final int SLIDER_MAX = 2_000;

	private static final int COLOUR_PANEL = 0xF0101018;
	private static final int COLOUR_BORDER = 0xFF3A6EA5;
	private static final int COLOUR_INNER = 0xFF1B1B24;
	private static final int COLOUR_TEXT = 0xFFE0E0E0;
	private static final int COLOUR_DIM = 0xFF9A9AA6;
	private static final int COLOUR_GOOD = 0xFF66DD66;
	private static final int COLOUR_WARN = 0xFFDDBB55;
	private static final int COLOUR_BAD = 0xFFDD6666;

	private RateSlider slider;
	private TextFieldWidget rateBox;
	private Button faceButton;

	private int pendingRate = 20;
	private Direction pendingFace = Direction.DOWN;
	private boolean initialised = false;

	public CreativeEnergyScreen(final CreativeEnergyContainer container, final PlayerInventory inventory,
	                            final ITextComponent title) {
		super(container, inventory, title);
		imageWidth = 248;
		imageHeight = 170;
	}

	@Override
	protected void init() {
		super.init();

		final CreativeEnergyTileEntity tileEntity = menu.getTileEntity();
		if (!initialised && tileEntity != null) {
			pendingRate = tileEntity.getOutputRate();
			pendingFace = tileEntity.getOutputFace();
			initialised = true;
		}

		final int left = leftPos + 10;

		slider = new RateSlider(left, topPos + 36, 228, 18,
			(double) Math.min(pendingRate, SLIDER_MAX) / SLIDER_MAX);
		addButton(slider);

		rateBox = new TextFieldWidget(font, left + 50, topPos + 58, 84, 16, new StringTextComponent("rate"));
		rateBox.setMaxLength(7);
		rateBox.setValue(Integer.toString(pendingRate));
		addButton(rateBox);

		addButton(new Button(left + 172, topPos + 57, 56, 18, new StringTextComponent("Apply"),
			button -> applyTyped()));

		// Presets on two rows so the labels actually fit inside the buttons
		for (int i = 0; i < CreativeEnergyTileEntity.PRESET_RATES.length; i++) {
			final int rate = CreativeEnergyTileEntity.PRESET_RATES[i];
			final int column = i % 3;
			final int row = i / 3;
			addButton(new Button(left + column * 76, topPos + 82 + row * 22, 74, 18,
				new StringTextComponent(CreativeEnergyTileEntity.PRESET_NAMES[i]),
				button -> setRate(rate)));
		}

		faceButton = new Button(left, topPos + 128, 228, 18,
			new StringTextComponent("Output face: " + pendingFace), button -> cycleFace());
		addButton(faceButton);
	}

	@Override
	public void tick() {
		super.tick();
		if (rateBox != null) {
			rateBox.tick();   // cursor blink
		}
	}

	private void applyTyped() {
		try {
			setRate(Integer.parseInt(rateBox.getValue().trim()));
		} catch (final NumberFormatException exception) {
			rateBox.setValue(Integer.toString(pendingRate));
		}
	}

	private void setRate(final int rate) {
		pendingRate = MathHelper.clamp(rate, 0, CreativeEnergyTileEntity.MAX_RATE);
		rateBox.setValue(Integer.toString(pendingRate));
		slider.setFromRate(pendingRate);
		send();
	}

	private void cycleFace() {
		pendingFace = Direction.from3DDataValue((pendingFace.get3DDataValue() + 1) % Direction.values().length);
		faceButton.setMessage(new StringTextComponent("Output face: " + pendingFace));
		send();
	}

	private void send() {
		WarpDriveNetwork.CHANNEL.sendToServer(
			new SetCreativeEnergyPacket(menu.getPos(), pendingRate, pendingFace));
	}

	@Override
	public boolean keyPressed(final int keyCode, final int scanCode, final int modifiers) {
		// Enter commits the typed value without closing the screen
		if (rateBox.isFocused() && (keyCode == 257 || keyCode == 335)) {
			applyTyped();
			return true;
		}
		// Let the text box swallow keys such as 'e' instead of closing the GUI
		if (rateBox.isFocused() && keyCode != 256) {
			return rateBox.keyPressed(keyCode, scanCode, modifiers) || super.keyPressed(keyCode, scanCode, modifiers);
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public void render(final MatrixStack matrixStack, final int mouseX, final int mouseY, final float partialTicks) {
		renderBackground(matrixStack);
		super.render(matrixStack, mouseX, mouseY, partialTicks);
		renderTooltip(matrixStack, mouseX, mouseY);
	}

	@Override
	protected void renderBg(final MatrixStack matrixStack, final float partialTicks,
	                        final int mouseX, final int mouseY) {
		fill(matrixStack, leftPos - 1, topPos - 1, leftPos + imageWidth + 1, topPos + imageHeight + 1, COLOUR_BORDER);
		fill(matrixStack, leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, COLOUR_PANEL);
		fill(matrixStack, leftPos + 6, topPos + 19, leftPos + imageWidth - 6, topPos + 32, COLOUR_INNER);
	}

	@Override
	protected void renderLabels(final MatrixStack matrixStack, final int mouseX, final int mouseY) {
		font.draw(matrixStack, title, 8.0F, 7.0F, COLOUR_TEXT);

		final CreativeEnergyTileEntity tileEntity = menu.getTileEntity();
		String status = "no tile entity";
		int colour = COLOUR_BAD;
		if (tileEntity != null) {
			switch (tileEntity.getLastStatus()) {
				case CreativeEnergyTileEntity.STATUS_DELIVERING:
					status = "delivering " + tileEntity.getLastPushed() + " FE/t to " + tileEntity.getOutputFace();
					colour = COLOUR_GOOD;
					break;
				case CreativeEnergyTileEntity.STATUS_RECEIVER_FULL:
					status = "receiver on " + tileEntity.getOutputFace() + " is full";
					colour = COLOUR_WARN;
					break;
				case CreativeEnergyTileEntity.STATUS_NO_CAPABILITY:
					status = "block on " + tileEntity.getOutputFace() + " does not accept FE";
					colour = COLOUR_BAD;
					break;
				case CreativeEnergyTileEntity.STATUS_NO_BLOCK:
					status = "no block on " + tileEntity.getOutputFace();
					colour = COLOUR_BAD;
					break;
				case CreativeEnergyTileEntity.STATUS_OFF:
				default:
					status = "output is off";
					colour = COLOUR_DIM;
					break;
			}
		}
		font.draw(matrixStack, status, 10.0F, 22.0F, colour);

		font.draw(matrixStack, "Custom:", 12.0F, 62.0F, COLOUR_TEXT);
		font.draw(matrixStack, "FE/t", 148.0F, 62.0F, COLOUR_DIM);
		font.draw(matrixStack, "max " + CreativeEnergyTileEntity.MAX_RATE + " FE/t", 12.0F, 152.0F, COLOUR_DIM);
	}

	/** Slider over 0..SLIDER_MAX FE/t. */
	private class RateSlider extends AbstractSlider {

		RateSlider(final int x, final int y, final int width, final int height, final double initial) {
			super(x, y, width, height, new StringTextComponent(""), initial);
			updateMessage();
		}

		void setFromRate(final int rate) {
			value = (double) Math.min(rate, SLIDER_MAX) / SLIDER_MAX;
			updateMessage();
		}

		@Override
		protected void updateMessage() {
			setMessage(new StringTextComponent(((int) Math.round(value * SLIDER_MAX)) + " FE/t"));
		}

		@Override
		protected void applyValue() {
			// Called on every drag step: update locally only. Sending a packet per pixel of drag
			// made the server echo a block update back constantly, which is what made the slider
			// stutter and fight the cursor.
			pendingRate = (int) Math.round(value * SLIDER_MAX);
			if (rateBox != null) {
				rateBox.setValue(Integer.toString(pendingRate));
			}
		}

		@Override
		public void onRelease(final double mouseX, final double mouseY) {
			super.onRelease(mouseX, mouseY);
			send();   // commit once, when the drag finishes
		}
	}
}
