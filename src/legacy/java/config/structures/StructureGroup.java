package cr0s.warpdrive.config.structures;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;

import java.util.Random;

import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class StructureGroup implements IStringSerializable {
	
	private static final String NONE = "-none-";
	protected String group;
	protected String name;
	
	public StructureGroup(final String group, final String name) {
		this.group = group == null || group.isEmpty() ? NONE : group;
		this.name = name;
	}
	
	@Override
	public String getName() {
		return group + ":" + (name == null || name.isEmpty() ? "*" : name);
	}
	
	public void generate(final World world, final Random random, final int x, final int y, final int z) {
		if (group.equals(NONE)) {
			return;
		}
		final AbstractStructure abstractStructure = StructureManager.getStructure(random, group, name);
		if (abstractStructure == null) {
			WarpDrive.logger.warn(String.format("Celestial object %s refers to unknown structure %s. Probably a bad configuration. Skipping for now.",
			                                    Commons.format(world, x, y, z),
			                                    getName()));
			return;
		}
		final AbstractStructureInstance abstractStructureInstance = abstractStructure.instantiate(random);
		abstractStructureInstance.generate(world, random, new BlockPos(x, y, z));
	}
	
	public String getGroup() {
		return group;
	}
	
	@Override
	public String toString() {
		return String.format("StructureGroup %s", getName());
	}
}
