package cr0s.warpdrive.block.weapon;

import cr0s.warpdrive.data.Registration;
import net.minecraft.tileentity.TileEntity;

/** Marker tile whose legacy behavior is supplied by the optional CC:Tweaked peripheral. */
public class WeaponControllerTileEntity extends TileEntity {

	public WeaponControllerTileEntity() {
		super(Registration.WEAPON_CONTROLLER_TILE.get());
	}
}
