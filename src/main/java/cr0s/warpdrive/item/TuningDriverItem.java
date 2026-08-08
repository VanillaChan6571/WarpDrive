package cr0s.warpdrive.item;

import cr0s.warpdrive.api.IBeamFrequency;
import cr0s.warpdrive.api.IControlChannel;
import cr0s.warpdrive.api.IVideoChannel;
import cr0s.warpdrive.block.energy.CapacitorBlock;
import cr0s.warpdrive.data.Registration;
import cr0s.warpdrive.data.WarpDriveItemGroup;
import net.minecraft.block.Block;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * One flattened mode of the 1.12 tuning driver.
 *
 * Metadata once selected the mode. In 1.16 each mode has its own registry id, so right-clicking air
 * replaces the held stack with the next item while retaining the shared channel NBT.
 */
public class TuningDriverItem extends Item {

	public enum Mode {
		VIDEO_CHANNEL("video_channel", IVideoChannel.VIDEO_CHANNEL_TAG,
			"warpdrive.video_channel"),
		BEAM_FREQUENCY("beam_frequency", IBeamFrequency.BEAM_FREQUENCY_TAG,
			"warpdrive.beam_frequency"),
		CONTROL_CHANNEL("control_channel", IControlChannel.CONTROL_CHANNEL_TAG,
			"warpdrive.control_channel");

		private final String registrySuffix;
		private final String nbtKey;
		private final String translationPrefix;

		Mode(final String registrySuffix, final String nbtKey, final String translationPrefix) {
			this.registrySuffix = registrySuffix;
			this.nbtKey = nbtKey;
			this.translationPrefix = translationPrefix;
		}

		public String getRegistrySuffix() {
			return registrySuffix;
		}

		public String getNbtKey() {
			return nbtKey;
		}

		public String getTranslationPrefix() {
			return translationPrefix;
		}

		public Mode next() {
			switch (this) {
			case VIDEO_CHANNEL:
				return BEAM_FREQUENCY;
			case BEAM_FREQUENCY:
				return CONTROL_CHANNEL;
			case CONTROL_CHANNEL:
			default:
				return VIDEO_CHANNEL;
			}
		}
	}

	private final Mode mode;

	public TuningDriverItem(final Mode mode) {
		super(new Properties().tab(WarpDriveItemGroup.MAIN).stacksTo(1));
		this.mode = mode;
	}

	public Mode getMode() {
		return mode;
	}

	public static int getValue(final ItemStack itemStack) {
		if (!(itemStack.getItem() instanceof TuningDriverItem)) {
			return -1;
		}
		final TuningDriverItem item = (TuningDriverItem) itemStack.getItem();
		final CompoundNBT tag = itemStack.getTag();
		return tag != null && tag.contains(item.mode.getNbtKey())
			? tag.getInt(item.mode.getNbtKey()) : -1;
	}

	@Nonnull
	public static ItemStack setValue(@Nonnull final ItemStack itemStack, final int value) {
		if (!(itemStack.getItem() instanceof TuningDriverItem) || value == -1) {
			return itemStack;
		}
		final TuningDriverItem item = (TuningDriverItem) itemStack.getItem();
		itemStack.getOrCreateTag().putInt(item.mode.getNbtKey(), value);
		return itemStack;
	}

	@Nonnull
	@Override
	public ActionResult<ItemStack> use(final World world, final PlayerEntity player, final Hand hand) {
		final ItemStack held = player.getItemInHand(hand);
		if (getPlayerPOVHitResult(world, player, RayTraceContext.FluidMode.NONE).getType()
			!= RayTraceResult.Type.MISS) {
			return ActionResult.pass(held);
		}
		if (world.isClientSide) {
			return ActionResult.success(held);
		}

		if (player.isShiftKeyDown() && player.abilities.instabuild) {
			final int value;
			switch (mode) {
			case VIDEO_CHANNEL:
				value = 1 + world.random.nextInt(IVideoChannel.VIDEO_CHANNEL_MAX);
				break;
			case BEAM_FREQUENCY:
				value = 1 + world.random.nextInt(IBeamFrequency.BEAM_FREQUENCY_MAX);
				break;
			case CONTROL_CHANNEL:
			default:
				value = world.random.nextInt(IControlChannel.CONTROL_CHANNEL_MAX);
				break;
			}
			setValue(held, value);
			player.displayClientMessage(new TranslationTextComponent(
				mode.getTranslationPrefix() + ".get", player.getDisplayName(), value), false);
			return ActionResult.consume(held);
		}

		final ItemStack replacement = new ItemStack(Registration.TUNING_DRIVERS.get(mode.next()).get());
		if (held.hasTag()) {
			replacement.setTag(held.getTag().copy());
		}
		player.setItemInHand(hand, replacement);
		world.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_CHIME,
			SoundCategory.PLAYERS, 0.1F, 1.0F);
		return ActionResult.consume(replacement);
	}

	@Nonnull
	@Override
	public ActionResultType useOn(final ItemUseContext context) {
		final World world = context.getLevel();
		final PlayerEntity player = context.getPlayer();
		if (player == null) {
			return ActionResultType.PASS;
		}

		final TileEntity tileEntity = world.getBlockEntity(context.getClickedPos());
		if (!isCompatible(tileEntity)) {
			return ActionResultType.PASS;
		}
		if (world.isClientSide) {
			return ActionResultType.SUCCESS;
		}

		final ItemStack held = context.getItemInHand();
		final ITextComponent machineName = world.getBlockState(context.getClickedPos()).getBlock().getName();
		final boolean isReading = player.isShiftKeyDown();
		final int value;
		switch (mode) {
		case VIDEO_CHANNEL:
			if (isReading) {
				setValue(held, ((IVideoChannel) tileEntity).getVideoChannel());
			} else {
				((IVideoChannel) tileEntity).setVideoChannel(getValue(held));
			}
			value = getValue(held);
			break;
		case BEAM_FREQUENCY:
			if (isReading) {
				setValue(held, ((IBeamFrequency) tileEntity).getBeamFrequency());
			} else {
				((IBeamFrequency) tileEntity).setBeamFrequency(getValue(held));
			}
			value = getValue(held);
			break;
		case CONTROL_CHANNEL:
		default:
			if (isReading) {
				setValue(held, ((IControlChannel) tileEntity).getControlChannel());
			} else {
				((IControlChannel) tileEntity).setControlChannel(getValue(held));
			}
			value = getValue(held);
			break;
		}

		player.displayClientMessage(new TranslationTextComponent(
			mode.getTranslationPrefix() + (isReading ? ".get" : ".set"), machineName, value), false);
		return ActionResultType.CONSUME;
	}

	private boolean isCompatible(@Nullable final TileEntity tileEntity) {
		switch (mode) {
		case VIDEO_CHANNEL:
			return tileEntity instanceof IVideoChannel;
		case BEAM_FREQUENCY:
			return tileEntity instanceof IBeamFrequency;
		case CONTROL_CHANNEL:
		default:
			return tileEntity instanceof IControlChannel;
		}
	}

	@Override
	public boolean doesSneakBypassUse(final ItemStack itemStack, final IWorldReader world,
	                                 final BlockPos blockPos, final PlayerEntity player) {
		final Block block = world.getBlockState(blockPos).getBlock();
		return block instanceof CapacitorBlock
			|| super.doesSneakBypassUse(itemStack, world, blockPos, player);
	}

	@Override
	public void appendHoverText(final ItemStack itemStack, @Nullable final World world,
	                            final List<ITextComponent> tooltip, final ITooltipFlag flag) {
		super.appendHoverText(itemStack, world, tooltip, flag);
		tooltip.add(new TranslationTextComponent(mode.getTranslationPrefix() + ".tooltip", getValue(itemStack)));
		tooltip.add(new TranslationTextComponent("item.warpdrive.tuning_driver.tooltip.usage"));
	}
}
