package cr0s.warpdrive.event;

import cr0s.warpdrive.item.ElectromagneticCellItem;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.item.ItemExpireEvent;

/** Restores the particle cell's hazardous despawn effect. */
public final class ParticleCellEventHandler {

	private ParticleCellEventHandler() { }

	public static void onItemExpire(final ItemExpireEvent event) {
		final ItemEntity entity = event.getEntityItem();
		final ItemStack itemStack = entity.getItem();
		if (!(itemStack.getItem() instanceof ElectromagneticCellItem)) return;
		final ElectromagneticCellItem cell = (ElectromagneticCellItem) itemStack.getItem();
		if (cell.getParticleType() != null && cell.getAmount(itemStack) > 0) {
			cell.getParticleType().affectWorld(entity.level, entity.position(), cell.getAmount(itemStack));
		}
	}
}
