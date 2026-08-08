package cr0s.warpdrive.item;

import cr0s.warpdrive.api.IBeamFrequency;
import cr0s.warpdrive.api.IControlChannel;
import cr0s.warpdrive.api.IVideoChannel;
import cr0s.warpdrive.data.WarpDriveItemGroup;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.DyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/** A flattened tuning-fork colour with the original three deterministic channel values. */
public class TuningForkItem extends Item {

	private final int legacyDyeDamage;

	public TuningForkItem(final DyeColor color) {
		super(new Properties().tab(WarpDriveItemGroup.MAIN).stacksTo(1));
		// 1.12 item dye damage was the reverse of the modern block-colour id: black=0, white=15.
		this.legacyDyeDamage = 15 - color.getId();
	}

	public int getVideoChannel() {
		return legacyDyeDamage + 100;
	}

	public int getBeamFrequency() {
		return (legacyDyeDamage + 1) * 10;
	}

	public int getControlChannel() {
		return legacyDyeDamage + 2;
	}

	/**
	 * Tune the machine that was clicked, from 1.12.2 ItemTuningFork.onItemUse.
	 *
	 * A machine may expose more than one of the three channel types. The original's precedence:
	 * video wins, then control, and beam frequency only when the machine has nothing else - or when
	 * the player sneaks, which is the deliberate override for machines that have both.
	 *
	 * Laser cannons, force fields and transporter cores now consume this beam-frequency branch;
	 * cameras/monitors use video channels and accelerator controls use control channels.
	 */
	@Nonnull
	@Override
	public ActionResultType useOn(final ItemUseContext context) {
		final World world = context.getLevel();
		final PlayerEntity player = context.getPlayer();
		if (player == null) {
			return ActionResultType.PASS;
		}

		final TileEntity tileEntity = world.getBlockEntity(context.getClickedPos());
		final boolean hasVideoChannel = tileEntity instanceof IVideoChannel;
		final boolean hasControlChannel = tileEntity instanceof IControlChannel;
		final boolean hasBeamFrequency = tileEntity instanceof IBeamFrequency;
		if (!hasVideoChannel && !hasControlChannel && !hasBeamFrequency) {
			return ActionResultType.PASS;
		}
		if (world.isClientSide) {
			return ActionResultType.SUCCESS;
		}

		// sneaking forces the beam frequency on a machine that also has a channel
		final boolean preferBeam = player.isShiftKeyDown() && hasBeamFrequency;
		final ITextComponent nameMachine = world.getBlockState(context.getClickedPos())
			.getBlock().getName();

		final String translationKey;
		final int value;
		if (hasVideoChannel && !preferBeam) {
			value = getVideoChannel();
			((IVideoChannel) tileEntity).setVideoChannel(value);
			translationKey = "warpdrive.video_channel.set";
		} else if (hasControlChannel && !preferBeam) {
			value = getControlChannel();
			((IControlChannel) tileEntity).setControlChannel(value);
			translationKey = "warpdrive.control_channel.set";
		} else {
			value = getBeamFrequency();
			((IBeamFrequency) tileEntity).setBeamFrequency(value);
			translationKey = "warpdrive.beam_frequency.set";
		}

		player.displayClientMessage(new TranslationTextComponent(translationKey, nameMachine, value), true);
		// 1.12.2 played its own ding.ogg, which did not survive into the 1.16.5 asset set
		world.playSound(null, context.getClickedPos(), SoundEvents.NOTE_BLOCK_CHIME,
			SoundCategory.PLAYERS, 0.1F, 1.0F);
		return ActionResultType.CONSUME;
	}

	@Override
	public void appendHoverText(final ItemStack itemStack, @Nullable final World world,
	                            final List<ITextComponent> tooltip, final ITooltipFlag flag) {
		super.appendHoverText(itemStack, world, tooltip, flag);
		tooltip.add(new TranslationTextComponent("item.warpdrive.tuning_fork.tooltip",
			getVideoChannel(), getBeamFrequency(), getControlChannel()));
	}
}
