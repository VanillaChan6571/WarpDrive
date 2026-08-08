package cr0s.warpdrive.item;

import cr0s.warpdrive.block.atomic.AcceleratorTier;
import cr0s.warpdrive.data.ParticleType;
import cr0s.warpdrive.data.Registration;
import cr0s.warpdrive.data.WarpDriveItemGroup;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Rarity;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

/** NBT-backed particle amount for one flattened tier/type electromagnetic-cell id. */
public class ElectromagneticCellItem extends Item {

	private static final String TAG_AMOUNT = "particleAmount";
	private static final float[] CREATIVE_LEVELS = { 0.1F, 0.3F, 0.5F, 0.7F, 0.9F, 1.0F };
	private static final int[] CAPACITIES = { 500, 1_000, 2_000 };

	private final AcceleratorTier tier;
	@Nullable private final ParticleType particleType;

	public ElectromagneticCellItem(final AcceleratorTier tier,
	                               @Nullable final ParticleType particleType) {
		super(new Properties().tab(WarpDriveItemGroup.MAIN).stacksTo(1)
			.rarity(particleType == ParticleType.STRANGE_MATTER ? Rarity.RARE
				: particleType == ParticleType.ANTIMATTER ? Rarity.UNCOMMON : Rarity.COMMON));
		this.tier = tier;
		this.particleType = particleType;
	}

	public AcceleratorTier getTier() { return tier; }
	@Nullable public ParticleType getParticleType() { return particleType; }
	public int getCapacity() { return CAPACITIES[tier.getIndex() - 1]; }

	@Override
	public void fillItemCategory(final ItemGroup group, final NonNullList<ItemStack> items) {
		if (!allowdedIn(group)) return;
		if (particleType == null) {
			items.add(new ItemStack(this));
			return;
		}
		for (final float level : CREATIVE_LEVELS) {
			final ItemStack itemStack = new ItemStack(this);
			setAmount(itemStack, Math.round(getCapacity() * level));
			items.add(itemStack);
		}
	}

	public int getAmount(final ItemStack itemStack) {
		if (itemStack.getItem() != this || particleType == null || !itemStack.hasTag()) return 0;
		return Math.max(0, Math.min(getCapacity(), itemStack.getTag().getInt(TAG_AMOUNT)));
	}

	public void setAmount(final ItemStack itemStack, final int requested) {
		if (itemStack.getItem() != this || particleType == null) return;
		final int amount = Math.max(0, Math.min(getCapacity(), requested));
		if (amount == 0) itemStack.removeTagKey(TAG_AMOUNT);
		else itemStack.getOrCreateTag().putInt(TAG_AMOUNT, amount);
	}

	public int fill(final ItemStack itemStack, final ParticleType resource,
	                final int requested, final boolean execute) {
		if (particleType != resource || requested <= 0) return 0;
		final int transfer = Math.min(requested, getCapacity() - getAmount(itemStack));
		if (execute && transfer > 0) setAmount(itemStack, getAmount(itemStack) + transfer);
		return transfer;
	}

	public int drain(final ItemStack itemStack, final int requested, final boolean execute) {
		if (particleType == null || requested <= 0) return 0;
		final int transfer = Math.min(requested, getAmount(itemStack));
		if (execute && transfer > 0) setAmount(itemStack, getAmount(itemStack) - transfer);
		return transfer;
	}

	public static float getFillLevel(final ItemStack itemStack) {
		if (!(itemStack.getItem() instanceof ElectromagneticCellItem)) return 0.0F;
		final ElectromagneticCellItem cell = (ElectromagneticCellItem) itemStack.getItem();
		return cell.particleType == null ? 0.0F : cell.getAmount(itemStack) / (float) cell.getCapacity();
	}

	public static ItemStack create(final AcceleratorTier tier, @Nullable final ParticleType particle,
	                              final int amount) {
		final String key = "electromagnetic_cell." + tier.getName() + "-"
			+ (particle == null ? "empty" : particle.getName());
		final ItemStack result = new ItemStack(Registration.LEGACY_CATALOG_ITEMS.get(key).get());
		if (result.getItem() instanceof ElectromagneticCellItem && particle != null) {
			((ElectromagneticCellItem) result.getItem()).setAmount(result, amount);
		}
		return result;
	}

	@Override
	public int getEntityLifespan(final ItemStack itemStack, final World world) {
		if (particleType == null || getAmount(itemStack) <= 0) {
			return super.getEntityLifespan(itemStack, world);
		}
		return (int) ((2.0F - getAmount(itemStack) / (float) getCapacity())
			* particleType.getEntityLifespan());
	}

	@Override
	public void appendHoverText(final ItemStack itemStack, @Nullable final World world,
	                           final List<ITextComponent> tooltip, final ITooltipFlag flags) {
		if (particleType == null || getAmount(itemStack) <= 0) {
			tooltip.add(new TranslationTextComponent(
				"item.warpdrive.atomic.electromagnetic_cell.tooltip.empty"));
			return;
		}
		tooltip.add(new TranslationTextComponent(
			"item.warpdrive.atomic.electromagnetic_cell.tooltip.filled",
			getAmount(itemStack), new TranslationTextComponent(particleType.getTranslationKey() + ".name")));
		tooltip.add(new TranslationTextComponent(particleType.getTranslationKey() + ".tooltip"));
	}
}
