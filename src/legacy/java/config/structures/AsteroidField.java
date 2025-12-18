package cr0s.warpdrive.config.structures;

import cr0s.warpdrive.config.InvalidXmlException;
import org.w3c.dom.Element;

import javax.annotation.Nonnull;
import java.util.Random;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class AsteroidField extends AbstractStructure {
	
	public AsteroidField(final String group, final String name) {
		super(group, name);
	}
	
	@Override
	public boolean loadFromXmlElement(final Element element) throws InvalidXmlException {
		return false;
	}
	
	@Override
	public boolean generate(@Nonnull final World world, @Nonnull final Random random, @Nonnull final BlockPos blockPos) {
		return instantiate(random).generate(world, random, blockPos);
	}
	
	@Override
	public AbstractStructureInstance instantiate(final Random random) {
		return new AsteroidFieldInstance(this, random);
	}
}
