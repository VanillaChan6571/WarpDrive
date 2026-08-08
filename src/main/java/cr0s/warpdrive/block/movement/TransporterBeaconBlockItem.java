package cr0s.warpdrive.block.movement;

import cr0s.warpdrive.data.WarpDriveItemGroup;
import net.minecraft.block.Block;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/** Portable/placed transporter beacon with one shared NBT-backed FE buffer and signature. */
public class TransporterBeaconBlockItem extends BlockItem {

	public static final int MAX_ENERGY = 60_000;
	public static final int ENERGY_PER_TICK = 10;
	public static final String TAG_ENERGY = "energy";
	public static final String TAG_SIGNATURE = "transporterUuid";
	public static final String TAG_NAME = "transporterName";

	public TransporterBeaconBlockItem(final Block block) {
		super(block, new Item.Properties().tab(WarpDriveItemGroup.MAIN).stacksTo(1));
	}

	public static int getEnergy(final ItemStack itemStack) {
		return itemStack.hasTag() ? MathHelper.clamp(itemStack.getTag().getInt(TAG_ENERGY),
			0, MAX_ENERGY) : 0;
	}

	public static void setEnergy(final ItemStack itemStack, final int energy) {
		itemStack.getOrCreateTag().putInt(TAG_ENERGY, MathHelper.clamp(energy, 0, MAX_ENERGY));
	}

	public static boolean isActive(final ItemStack itemStack) {
		return getEnergy(itemStack) > ENERGY_PER_TICK;
	}

	@Nullable
	public static UUID getSignature(final ItemStack itemStack) {
		if (!itemStack.hasTag() || !itemStack.getTag().hasUUID(TAG_SIGNATURE)) return null;
		return itemStack.getTag().getUUID(TAG_SIGNATURE);
	}

	public static String getSignatureName(final ItemStack itemStack) {
		return itemStack.hasTag() ? itemStack.getTag().getString(TAG_NAME) : "";
	}

	public static void setSignature(final ItemStack itemStack, final String name, final UUID uuid) {
		final CompoundNBT tag = itemStack.getOrCreateTag();
		tag.putUUID(TAG_SIGNATURE, uuid);
		tag.putString(TAG_NAME, name == null ? "" : name);
	}

	@Override
	public void inventoryTick(@Nonnull final ItemStack itemStack, @Nonnull final World world,
	                          @Nonnull final Entity entity, final int slot, final boolean selected) {
		if (!world.isClientSide && selected && entity instanceof PlayerEntity) {
			setEnergy(itemStack, getEnergy(itemStack) - ENERGY_PER_TICK);
		}
		super.inventoryTick(itemStack, world, entity, slot, selected);
	}

	@Nonnull
	@Override
	public ActionResultType useOn(final ItemUseContext context) {
		final net.minecraft.tileentity.TileEntity tileEntity =
			context.getLevel().getBlockEntity(context.getClickedPos());
		if (!(tileEntity instanceof TransporterCoreTileEntity)) return super.useOn(context);
		final PlayerEntity player = context.getPlayer();
		if (player == null) return ActionResultType.FAIL;
		if (context.getLevel().isClientSide) return ActionResultType.SUCCESS;

		final ItemStack held = context.getItemInHand();
		final TransporterCoreTileEntity core = (TransporterCoreTileEntity) tileEntity;
		if (player.isShiftKeyDown()) {
			setSignature(held, core.getSignatureName(), core.getSignatureUUID());
			player.displayClientMessage(new StringTextComponent("Beacon linked to "
				+ core.getSignatureName() + " (" + core.getSignatureUUID() + ")"), false);
			return ActionResultType.CONSUME;
		}

		final UUID signature = getSignature(held);
		if (signature == null) {
			player.displayClientMessage(new StringTextComponent(
				"Beacon has no transporter signature; sneak-use it on a core first"), false);
		} else if (signature.equals(core.getSignatureUUID())) {
			player.displayClientMessage(new StringTextComponent(
				"A transporter cannot target its own signature"), false);
		} else {
			core.setRemoteUuid(signature);
			player.displayClientMessage(new StringTextComponent("Remote transporter set to "
				+ getSignatureName(held) + " (" + signature + ")"), false);
		}
		return ActionResultType.CONSUME;
	}

	@Override
	public boolean showDurabilityBar(final ItemStack itemStack) {
		return getEnergy(itemStack) < MAX_ENERGY;
	}

	@Override
	public double getDurabilityForDisplay(final ItemStack itemStack) {
		return 1.0D - getEnergy(itemStack) / (double) MAX_ENERGY;
	}

	@Override
	public int getRGBDurabilityForDisplay(final ItemStack itemStack) {
		return MathHelper.hsvToRgb(Math.max(0.0F, getEnergy(itemStack) / (float) MAX_ENERGY) / 3.0F,
			1.0F, 1.0F);
	}

	@Override
	public void appendHoverText(final ItemStack itemStack, @Nullable final World world,
	                            final List<ITextComponent> tooltip, final ITooltipFlag flag) {
		super.appendHoverText(itemStack, world, tooltip, flag);
		final UUID signature = getSignature(itemStack);
		tooltip.add(new StringTextComponent(getEnergy(itemStack) + " / " + MAX_ENERGY + " FE"));
		tooltip.add(new StringTextComponent(signature == null ? "Unlinked"
			: "Linked to " + getSignatureName(itemStack) + " (" + signature + ")"));
	}

	@Nullable
	@Override
	public ICapabilityProvider initCapabilities(final ItemStack itemStack,
	                                            @Nullable final CompoundNBT capabilityNbt) {
		return new ICapabilityProvider() {
			private final LazyOptional<IEnergyStorage> energy = LazyOptional.of(() ->
				new IEnergyStorage() {
					@Override
					public int receiveEnergy(final int maxReceive, final boolean simulate) {
						final int accepted = Math.min(Math.min(1024, Math.max(0, maxReceive)),
							MAX_ENERGY - getEnergy(itemStack));
						if (!simulate && accepted > 0) setEnergy(itemStack, getEnergy(itemStack) + accepted);
						return accepted;
					}
					@Override public int extractEnergy(final int maxExtract, final boolean simulate) { return 0; }
					@Override public int getEnergyStored() { return getEnergy(itemStack); }
					@Override public int getMaxEnergyStored() { return MAX_ENERGY; }
					@Override public boolean canExtract() { return false; }
					@Override public boolean canReceive() { return true; }
				});

			@Nonnull
			@Override
			public <T> LazyOptional<T> getCapability(@Nonnull final Capability<T> capability,
			                                          @Nullable final Direction side) {
				return capability == CapabilityEnergy.ENERGY ? energy.cast() : LazyOptional.empty();
			}
		};
	}
}
