package cr0s.warpdrive.item;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.EnumTier;

import ic2.api.reactor.IReactor;
import ic2.api.reactor.IReactorComponent;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import net.minecraftforge.fml.common.Optional;

@Optional.InterfaceList({
	@Optional.Interface(iface = "ic2.api.reactor.IReactorComponent", modid = "ic2")
	})
public class ItemIC2reactorLaserFocus extends ItemAbstractBase implements IReactorComponent {
	
	private static final int[] xOffset = { -1,  0, 0, 1 };
	private static final int[] yOffset = {  0, -1, 1, 0 };
	
	public ItemIC2reactorLaserFocus(final String registryName, final EnumTier enumTier) {
		super(registryName, enumTier);
		
		setMaxDamage(WarpDriveConfig.IC2_REACTOR_MAX_HEAT_STORED);
		setTranslationKey("warpdrive.energy.IC2reactorLaserFocus");
	}
	
	public static int getCurrentHeat(final ItemStack itemStackFocus) {
		return itemStackFocus.getItemDamage();
	}
	
	public static int addHeat(final ItemStack itemStackFocus, final int heat) {
		if (WarpDriveConfig.LOGGING_ENERGY) {
			WarpDrive.logger.info(String.format("%s addHeat(heat %d)",
			                                    itemStackFocus, heat));
		}
		final int heatCurrent = getCurrentHeat(itemStackFocus);
		final int heatToTransfer = Math.min(heat, WarpDriveConfig.IC2_REACTOR_MAX_HEAT_STORED - heatCurrent);
		setHeat(itemStackFocus, heatCurrent + heatToTransfer);
		return heat - heatToTransfer;
	}
	
	private static void setHeat(final ItemStack itemStackFocus, final int heat) {
		itemStackFocus.setItemDamage(heat);
	}
	
	private static void balanceComponent(final ItemStack itemStackFocus1, final ItemStack itemStackFocus2) {
		final int heatOld1 = getCurrentHeat(itemStackFocus1);
		final int heatOld2 = getCurrentHeat(itemStackFocus2);
		// force unidirectional transfer so we only transfer once per simulation tick
		if (heatOld1 < heatOld2) {
			final int heatToTransfer = Math.min((heatOld2 - heatOld1) / 2, WarpDriveConfig.IC2_REACTOR_FOCUS_HEAT_TRANSFER_PER_TICK);
			final int heatNew1 = Math.min(WarpDriveConfig.IC2_REACTOR_MAX_HEAT_STORED, heatOld1 + heatToTransfer);
			final int heatNew2 = heatOld2 - (heatNew1 - heatOld1);
			setHeat(itemStackFocus1, heatNew1);
			setHeat(itemStackFocus2, heatNew2);
		}
	}
	
	@Optional.Method(modid = "ic2")
	private static void coolComponent(final ItemStack itemStackFocus, final IReactorComponent reactorComponent,
	                                  final IReactor reactor, final ItemStack itemStackComponent, final int x, final int y) {
		final int heatMaxRate = Math.min(WarpDriveConfig.IC2_REACTOR_COMPONENT_HEAT_TRANSFER_PER_TICK,
		                                 WarpDriveConfig.IC2_REACTOR_MAX_HEAT_STORED - itemStackFocus.getItemDamage());
		final int heatComponent = reactorComponent.getCurrentHeat(itemStackComponent, reactor, x, y);
		final int heatToTransfer = -Math.min(heatComponent, heatMaxRate);
		final int heatRetained = reactorComponent.alterHeat(itemStackComponent, reactor, x, y, heatToTransfer);
		addHeat(itemStackFocus, heatRetained - heatToTransfer);
	}
	
	@Optional.Method(modid = "ic2")
	private static void coolReactor(final IReactor reactor, final ItemStack self) {
		final int heatMaxRate = Math.min(WarpDriveConfig.IC2_REACTOR_REACTOR_HEAT_TRANSFER_PER_TICK,
		                                 WarpDriveConfig.IC2_REACTOR_MAX_HEAT_STORED - self.getItemDamage());
		final int heatReactor = reactor.getHeat();
		final int heatToTransfer = Math.min(heatMaxRate, heatReactor);
		reactor.addHeat(-heatToTransfer);
		addHeat(self, heatToTransfer);
	}
	
	// IReactorComponent overrides
	@Override
	@Optional.Method(modid = "ic2")
	public void processChamber(final ItemStack itemStackFocus, final IReactor reactor, final int x, final int y, final boolean isRunning) {
		if (!isRunning) {
			return;
		}
		
		for (int index = 0; index < xOffset.length; index++) {
			final int xComponent = x + xOffset[index];
			final int yComponent = y + yOffset[index];
			final ItemStack stack = reactor.getItemAt(xComponent, yComponent);
			if (stack != null) {
				final Item item = stack.getItem();
				if (item instanceof ItemIC2reactorLaserFocus) {
					balanceComponent(itemStackFocus, stack);
				} else if (item instanceof IReactorComponent) {
					coolComponent(itemStackFocus, (IReactorComponent) item, reactor, stack, xComponent, yComponent);
				}
			}
		}
		
		coolReactor(reactor, itemStackFocus);
	}
	
	@Override
	@Optional.Method(modid = "ic2")
	public boolean acceptUraniumPulse(final ItemStack itemStackFocus, final IReactor reactor, final ItemStack pulsingStack,
	                                  final int xFocus, final int yFocus, final int xPulsing, final int yPulsing, final boolean isRunning) {
		return false;
	}
	
	@Override
	@Optional.Method(modid = "ic2")
	public boolean canStoreHeat(final ItemStack itemStackFocus, final IReactor reactor, final int x, final int y) {
		return true;
	}
	
	@Override
	@Optional.Method(modid = "ic2")
	public int getMaxHeat(final ItemStack itemStackFocus, final IReactor reactor, final int x, final int y) {
		return WarpDriveConfig.IC2_REACTOR_MAX_HEAT_STORED;
	}
	
	@Override
	@Optional.Method(modid = "ic2")
	public int getCurrentHeat(final ItemStack itemStackFocus, final IReactor reactor, final int x, final int y) {
		return getCurrentHeat(itemStackFocus);
	}
	
	@Override
	@Optional.Method(modid = "ic2")
	public int alterHeat(final ItemStack itemStackFocus, final IReactor reactor, final int x, final int y, final int heat) {
		if (WarpDriveConfig.LOGGING_ENERGY) {
			WarpDrive.logger.info(String.format("%s alterHeat(reactor %s, x %d, y %d, heat %d)",
			                                    itemStackFocus, reactor, x, y, heat));
		}
		return addHeat(itemStackFocus, heat);
	}
	
	@Override
	@Optional.Method(modid = "ic2")
	public float influenceExplosion(final ItemStack itemStack, final IReactor reactor) {
		return 0;
	}
	
	@Override
	public boolean canBePlacedIn(final ItemStack itemStack, final IReactor reactor) {
		return true;
	}
}
