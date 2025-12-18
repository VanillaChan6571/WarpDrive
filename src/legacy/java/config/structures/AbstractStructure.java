package cr0s.warpdrive.config.structures;

import cr0s.warpdrive.api.IXmlRepresentable;
import cr0s.warpdrive.config.InvalidXmlException;
import cr0s.warpdrive.config.XmlFileManager;

import javax.annotation.Nonnull;

import org.w3c.dom.Element;

import java.util.HashMap;
import java.util.List;
import java.util.Random;

import net.minecraft.world.gen.feature.WorldGenerator;

/**
 * @author Francesco, LemADEC
 *
 */
public abstract class AbstractStructure extends WorldGenerator implements IXmlRepresentable {
	protected String group;
	protected String name;
	protected HashMap<String,String> variables = new HashMap<>();
	
	public AbstractStructure(final String group, final String name) {
		this.group = group;
		this.name = name;
	}
	
	@Nonnull
	@Override
	public String getName() {
		return name;
	}
	
	public String getFullName() {
		return group + ":" + name;
	}
	
	
	abstract public AbstractStructureInstance instantiate(Random random);
	
	@Override
	public boolean loadFromXmlElement(final Element element) throws InvalidXmlException {
		
		final List<Element> listVariables = XmlFileManager.getChildrenElementByTagName(element, "variable");
		for (final Element elementVariable : listVariables) {
			final String variableName = elementVariable.getAttribute("name");
			final String variableExpression = elementVariable.getTextContent();
			variables.put(variableName, variableExpression);
		}
		
		return true;
	}
}
