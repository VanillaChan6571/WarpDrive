package cr0s.warpdrive.item;

import cr0s.warpdrive.block.atomic.AcceleratorTier;
import cr0s.warpdrive.data.ParticleType;
import cr0s.warpdrive.data.Registration;
import cr0s.warpdrive.data.WarpDriveItemGroup;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Rarity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Multi-particle NBT container used by the legacy plasma-processing recipes.
 *
 * The 1.12 recipe that created the torch was commented out, so these remain creative/command-only
 * by design. Unlike electromagnetic cells, one torch registry id may hold any particle type.
 */
public class PlasmaTorchItem extends Item {

	private static final String TAG_PARTICLE = "particle";
	private static final String TAG_PARTICLE_NAME = "name";
	private static final String TAG_PARTICLE_AMOUNT = "amount";
	private static final String TAG_AMOUNT_TO_CONSUME = "amountToConsume";
	private static final String TAG_TIME_TO_CONSUME = "tickToConsume";
	private static final long CONSUMPTION_WINDOW_MILLISECONDS = 50L;

	private final AcceleratorTier tier;

	public PlasmaTorchItem(final AcceleratorTier tier) {
		super(new Properties().tab(WarpDriveItemGroup.MAIN).stacksTo(1).rarity(rarity(tier)));
		this.tier = tier;
	}

	private static Rarity rarity(final AcceleratorTier tier) {
		switch (tier) {
		case ADVANCED: return Rarity.UNCOMMON;
		case SUPERIOR: return Rarity.RARE;
		case BASIC:
		default: return Rarity.COMMON;
		}
	}

	public AcceleratorTier getTier() { return tier; }
	public int getCapacity() { return PlasmaTorchStorage.capacity(tier); }

	@Nullable
	public ParticleType getParticleType(final ItemStack itemStack) {
		if (itemStack.getItem() != this || itemStack.getCount() != 1 || !itemStack.hasTag()) {
			return null;
		}
		final CompoundNBT particle = itemStack.getTag().getCompound(TAG_PARTICLE);
		if (!particle.contains(TAG_PARTICLE_NAME)) return null;
		return PlasmaTorchStorage.parseParticleName(particle.getString(TAG_PARTICLE_NAME));
	}

	public int getAmount(final ItemStack itemStack) {
		if (getParticleType(itemStack) == null) return 0;
		return PlasmaTorchStorage.clampAmount(
			itemStack.getTag().getCompound(TAG_PARTICLE).getInt(TAG_PARTICLE_AMOUNT),
			getCapacity());
	}

	public boolean isEmpty(final ItemStack itemStack) {
		return getParticleType(itemStack) == null || getAmount(itemStack) <= 0;
	}

	public void setContent(final ItemStack itemStack, @Nullable final ParticleType particleType,
	                       final int requestedAmount) {
		if (itemStack.getItem() != this || itemStack.getCount() != 1) return;
		final int amount = PlasmaTorchStorage.clampAmount(requestedAmount, getCapacity());
		if (particleType == null || amount == 0) {
			itemStack.removeTagKey(TAG_PARTICLE);
			return;
		}
		final CompoundNBT particle = new CompoundNBT();
		particle.putString(TAG_PARTICLE_NAME, particleType.getName());
		particle.putInt(TAG_PARTICLE_AMOUNT, amount);
		itemStack.getOrCreateTag().put(TAG_PARTICLE, particle);
	}

	/** Returns the amount accepted, without mutating when {@code execute} is false. */
	public int fill(final ItemStack itemStack, final ParticleType particleType,
	                final int requestedAmount, final boolean execute) {
		if (itemStack.getItem() != this || itemStack.getCount() != 1) return 0;
		final ParticleType storedType = getParticleType(itemStack);
		final int transfer = PlasmaTorchStorage.fillTransfer(storedType, getAmount(itemStack),
			particleType, requestedAmount, getCapacity());
		if (execute && transfer > 0) {
			setContent(itemStack, particleType, getAmount(itemStack) + transfer);
		}
		return transfer;
	}

	/** Returns the amount drained, without mutating when {@code execute} is false. */
	public int drain(final ItemStack itemStack, final ParticleType particleType,
	                 final int requestedAmount, final boolean execute) {
		final int transfer = PlasmaTorchStorage.drainTransfer(getParticleType(itemStack),
			getAmount(itemStack), particleType, requestedAmount, getCapacity());
		if (execute && transfer > 0) {
			setContent(itemStack, particleType, getAmount(itemStack) - transfer);
		}
		return transfer;
	}

	/** Marks what the next recipe remainder calculation should consume. */
	public void setAmountToConsume(final ItemStack itemStack, final int requestedAmount) {
		if (getParticleType(itemStack) == null) return;
		final CompoundNBT tag = itemStack.getOrCreateTag();
		if (requestedAmount > 0) {
			tag.putInt(TAG_AMOUNT_TO_CONSUME, requestedAmount);
			tag.putLong(TAG_TIME_TO_CONSUME, System.currentTimeMillis());
		} else {
			tag.remove(TAG_AMOUNT_TO_CONSUME);
			tag.remove(TAG_TIME_TO_CONSUME);
		}
	}

	private int getAmountToConsume(final ItemStack itemStack) {
		if (!itemStack.hasTag()) return 0;
		final CompoundNBT tag = itemStack.getTag();
		final long elapsed = System.currentTimeMillis() - tag.getLong(TAG_TIME_TO_CONSUME);
		if (elapsed >= 0L && elapsed < CONSUMPTION_WINDOW_MILLISECONDS) {
			return Math.max(0, tag.getInt(TAG_AMOUNT_TO_CONSUME));
		}
		tag.remove(TAG_AMOUNT_TO_CONSUME);
		tag.remove(TAG_TIME_TO_CONSUME);
		return 0;
	}

	@Override
	public boolean hasContainerItem(final ItemStack itemStack) {
		return itemStack.getItem() == this;
	}

	@Override
	public ItemStack getContainerItem(final ItemStack filled) {
		final ParticleType particleType = getParticleType(filled);
		if (particleType == null) return ItemStack.EMPTY;
		final int amountToConsume = getAmountToConsume(filled);
		if (amountToConsume <= 0) return ItemStack.EMPTY;
		final ItemStack result = new ItemStack(this);
		setContent(result, particleType, PlasmaTorchStorage.remainingAfterConsumption(
			getAmount(filled), amountToConsume, getCapacity()));
		return result;
	}

	public static float getFillLevel(final ItemStack itemStack) {
		if (!(itemStack.getItem() instanceof PlasmaTorchItem)) return 0.0F;
		final PlasmaTorchItem torch = (PlasmaTorchItem) itemStack.getItem();
		return torch.getAmount(itemStack) / (float) torch.getCapacity();
	}

	public static ItemStack create(final AcceleratorTier tier,
	                              @Nullable final ParticleType particleType,
	                              final int amount) {
		final ItemStack result = new ItemStack(Registration.LEGACY_CATALOG_ITEMS
			.get("plasma_torch." + tier.getName()).get());
		if (result.getItem() instanceof PlasmaTorchItem) {
			((PlasmaTorchItem) result.getItem()).setContent(result, particleType, amount);
		}
		return result;
	}

	@Override
	public void appendHoverText(final ItemStack itemStack, @Nullable final World world,
	                           final List<ITextComponent> tooltip, final ITooltipFlag flags) {
		final ParticleType particleType = getParticleType(itemStack);
		if (particleType == null || getAmount(itemStack) <= 0) {
			tooltip.add(new TranslationTextComponent(
				"item.warpdrive.tool.plasma_torch.tooltip.empty", getCapacity()));
			return;
		}
		tooltip.add(new TranslationTextComponent(
			"item.warpdrive.tool.plasma_torch.tooltip.filled", getAmount(itemStack),
			new TranslationTextComponent(particleType.getTranslationKey() + ".name"), getCapacity()));
		tooltip.add(new TranslationTextComponent(particleType.getTranslationKey() + ".tooltip"));
	}
}
