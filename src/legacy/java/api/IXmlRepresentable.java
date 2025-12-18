package cr0s.warpdrive.api;

import cr0s.warpdrive.config.InvalidXmlException;

import net.minecraft.util.IStringSerializable;

import org.w3c.dom.Element;

public interface IXmlRepresentable extends IStringSerializable {
	
	// Load the XML element, return true if successful
	boolean loadFromXmlElement(Element element) throws InvalidXmlException;
}
