package cr0s.warpdrive.config.structures;

import cr0s.warpdrive.config.InvalidXmlException;

import javax.annotation.Nonnull;

import org.w3c.dom.Element;

import java.util.Random;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class StructureReference extends AbstractStructure {
	
	public StructureReference(final String group, final String name) {
		super(group, name);
	}
	
	@Override
	public boolean loadFromXmlElement(final Element element) throws InvalidXmlException {
		super.loadFromXmlElement(element);
		
		return true;
	}
	
	@Override
	public boolean generate(@Nonnull final World world, @Nonnull final Random random, @Nonnull final BlockPos blockPos) {
		return instantiate(random).generate(world, random, blockPos);
	}
	
	@Override
	public AbstractStructureInstance instantiate(final Random random) {
		return StructureManager.getStructure(random, group, name).instantiate(random);
	}
}
