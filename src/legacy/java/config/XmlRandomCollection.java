package cr0s.warpdrive.config;

import cr0s.warpdrive.api.IXmlRepresentable;
import org.w3c.dom.Element;

/**
 * Collection of elements with ratios and weights. Helps to select element with controlled odds.
 * 
 * @author ncrashed, LemADEC
 *
 * @param <E>
 **/
public class XmlRandomCollection<E extends IXmlRepresentable> extends RandomCollection<E> {
	
	/**
	 * Loads object from given XML element and parses configurations for weighted pick.
	 * 
	 * @param object
	 *            Object to load into
	 * @param element
	 *            Element of an XML file
	 * @throws InvalidXmlException
	 *            Exception encountered
	 **/
	public void loadFromXML(final E object, final Element element) throws InvalidXmlException {
		if (!object.loadFromXmlElement(element)) {// skip invalid entries
			return;
		}
		
		final String stringRatio = element.getAttribute("ratio");
		final String stringWeight = element.getAttribute("weight");
		
		add(object, stringRatio, stringWeight);
	}
}