package cr0s.warpdrive.block.atomic;

import cr0s.warpdrive.data.Registration;

/** Injector marker retaining its distinct legacy ComputerCraft peripheral type. */
public class ParticlesInjectorTileEntity extends AcceleratorControlPointTileEntity {

	public ParticlesInjectorTileEntity() {
		super(Registration.PARTICLES_INJECTOR_TILE.get());
	}
}
