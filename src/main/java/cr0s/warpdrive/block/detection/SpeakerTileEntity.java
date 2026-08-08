package cr0s.warpdrive.block.detection;

import cr0s.warpdrive.data.Registration;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Util;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import javax.annotation.Nullable;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Rate-limited chat broadcaster, ported from 1.12.2 {@code TileEntitySpeaker}.
 *
 * Up to twelve messages can queue. A three-message burst is allowed, with one message of budget
 * returning every twenty ticks (the original 3 / 60 decay). Messages reach non-spectating players
 * inside the tier's axis-aligned range.
 */
public class SpeakerTileEntity extends TileEntity implements ITickableTileEntity {

	private static final String TAG_ENABLED = "enabled";
	private static final int QUEUE_MAX_MESSAGES = 12;
	private static final float RATE_MAX_MESSAGES = 3.0F;
	private static final float RATE_DECAY_PER_TICK = RATE_MAX_MESSAGES / 60.0F;

	private final Queue<String> messagesToSpeak = new LinkedList<>();
	private float rateMessaging;
	private boolean enabled = true;

	public SpeakerTileEntity() {
		super(Registration.SPEAKER_TILE.get());
	}

	@Override
	public void tick() {
		rateMessaging = Math.max(0.0F, rateMessaging - RATE_DECAY_PER_TICK);
		if (level == null || level.isClientSide || !enabled || messagesToSpeak.isEmpty()
		 || rateMessaging + 1.0F >= RATE_MAX_MESSAGES) {
			return;
		}

		final String rawMessage = messagesToSpeak.remove();
		final ITextComponent message = parseMessage(rawMessage);
		final float range = getTier().getRange();
		final AxisAlignedBB bounds = new AxisAlignedBB(
			getBlockPos().getX() - range, getBlockPos().getY() - range, getBlockPos().getZ() - range,
			getBlockPos().getX() + range + 1.0D, getBlockPos().getY() + range + 1.0D,
			getBlockPos().getZ() + range + 1.0D);
		for (final ServerPlayerEntity player : level.getEntitiesOfClass(ServerPlayerEntity.class, bounds,
			player -> player.isAlive() && !player.isSpectator())) {
			player.sendMessage(message, Util.NIL_UUID);
		}
		rateMessaging++;
	}

	private SpeakerTier getTier() {
		final BlockState blockState = getBlockState();
		return blockState.getBlock() instanceof SpeakerBlock
		     ? ((SpeakerBlock) blockState.getBlock()).getTier() : SpeakerTier.BASIC;
	}

	private static ITextComponent parseMessage(final String rawMessage) {
		if (rawMessage.startsWith("[") && rawMessage.endsWith("]")) {
			try {
				final ITextComponent parsed = ITextComponent.Serializer.fromJson(rawMessage);
				if (parsed != null) {
					return parsed;
				}
			} catch (final RuntimeException ignored) {
				// Legacy WarpDriveText fell back to the untranslated input on malformed JSON.
			}
		}
		final ITextComponent translated = new TranslationTextComponent(rawMessage);
		return translated.getString().equals(rawMessage)
		     ? new StringTextComponent(rawMessage) : translated;
	}

	public Object[] speak(@Nullable final String message) {
		if (message != null) {
			if (messagesToSpeak.size() >= QUEUE_MAX_MESSAGES) {
				return new Object[]{ false, "You're speaking too fast... breath!" };
			}
			messagesToSpeak.add(message);
		}
		return new Object[]{ true, messagesToSpeak.size() };
	}

	public Object[] enable(@Nullable final Boolean value) {
		if (value != null) {
			enabled = value;
			setChanged();
		}
		return new Object[]{ enabled };
	}

	@Override
	public void load(final BlockState blockState, final CompoundNBT tagCompound) {
		super.load(blockState, tagCompound);
		enabled = !tagCompound.contains(TAG_ENABLED) || tagCompound.getBoolean(TAG_ENABLED);
	}

	@Override
	public CompoundNBT save(final CompoundNBT tagCompound) {
		super.save(tagCompound);
		tagCompound.putBoolean(TAG_ENABLED, enabled);
		return tagCompound;
	}
}
