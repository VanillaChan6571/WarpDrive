package cr0s.warpdrive.compat;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.block.ShipCoreTileEntity;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.Capabilities;
import dan200.computercraft.shared.peripheral.generic.GenericPeripheralProvider;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Isolates all executable CC:Tweaked references from the Ship Core class.
 *
 * This class is registered only when CC:Tweaked is loaded, allowing the base mod and its native
 * controller to run without the optional dependency while retaining the existing Lua API.
 */
public final class ComputerCraftCompat {

	private static final ResourceLocation CAPABILITY_ID =
		new ResourceLocation(WarpDrive.MODID, "ship_core_peripheral");

	private ComputerCraftCompat() {
	}

	public static void register() {
		MinecraftForge.EVENT_BUS.addGenericListener(TileEntity.class,
			ComputerCraftCompat::attachCapabilities);
		WarpDrive.logger.info("CC:Tweaked ship peripheral compatibility enabled");
	}

	private static void attachCapabilities(final AttachCapabilitiesEvent<TileEntity> event) {
		if (!(event.getObject() instanceof ShipCoreTileEntity)) {
			return;
		}
		final PeripheralProvider provider = new PeripheralProvider((ShipCoreTileEntity) event.getObject());
		event.addCapability(CAPABILITY_ID, provider);
		event.addListener(provider::invalidate);
	}

	private static final class PeripheralProvider implements ICapabilityProvider {

		private final ShipCoreTileEntity shipCore;
		private LazyOptional<IPeripheral> peripheral = LazyOptional.empty();

		private PeripheralProvider(final ShipCoreTileEntity shipCore) {
			this.shipCore = shipCore;
		}

		@Nonnull
		@Override
		public <T> LazyOptional<T> getCapability(@Nonnull final Capability<T> capability,
		                                          @Nullable final Direction side) {
			if (capability != Capabilities.CAPABILITY_PERIPHERAL || shipCore.getLevel() == null) {
				return LazyOptional.empty();
			}
			if (!peripheral.isPresent()) {
				final IPeripheral value = GenericPeripheralProvider.getPeripheral(
					shipCore.getLevel(), shipCore.getBlockPos(), side, ignored -> invalidate());
				if (value != null) {
					peripheral = LazyOptional.of(() -> value);
				}
			}
			return peripheral.cast();
		}

		private void invalidate() {
			peripheral.invalidate();
			peripheral = LazyOptional.empty();
		}
	}
}
