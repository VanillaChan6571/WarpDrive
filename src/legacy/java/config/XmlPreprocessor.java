package cr0s.warpdrive.config;

import cr0s.warpdrive.WarpDrive;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraftforge.fml.common.Loader;

public class XmlPreprocessor {
	
	static AtomicInteger outputCount = new AtomicInteger(1);
	
	/**
	 * Check the given element for a mod attribute and return a string of all the ones that are not loaded, separated by commas
	 *
	 * @param element
	 *            Element to check
	 * @return A string, which is empty if all the mods are loaded.
	 */
	public static String checkModRequirements(final Element element) {
		
		final ModCheckResults modCheckResults = new ModCheckResults();
		
		for (final String mod : element.getAttribute("mods").split(",")) {
			
			// @TODO add version check
			
			if (mod.isEmpty()) {
				continue;
			}
			
			if (mod.startsWith("!")) {
				if (Loader.isModLoaded(mod.substring(1))) {
					modCheckResults.addMod(mod, "loaded");
				}
				
			} else if (!Loader.isModLoaded(mod)) {
				modCheckResults.addMod(mod, "not loaded");
			}
		}
		
		return modCheckResults.toString();
	}
	
	/**
	 * Goes through every child node of the given node, and remove elements failing to checkModRequirements()
	 */
	public static void doModReqSanitation(final Node base) {
		
		final NodeList children = base.getChildNodes();
		
		for (int i = 0; i < children.getLength(); i++) {
			final Node child = children.item(i);
			
			if (child instanceof Element) {
				final Element elementChild = (Element) child;
				final String result = checkModRequirements(elementChild);
				if (!result.isEmpty()) {
					final String block = elementChild.getAttribute("block");
					if (block == null || block.isEmpty()) {
						WarpDrive.logger.info(String.format("Skipping %s/%s %s:%s due to %s",
						                                    base.getNodeName(), elementChild.getNodeName(),
						                                    elementChild.getAttribute("group"), elementChild.getAttribute("name"),
						                                    result));
					} else {
						WarpDrive.logger.info(String.format("Skipping %s/%s %s:%s %s due to %s",
						                                    base.getNodeName(), elementChild.getNodeName(),
						                                    elementChild.getAttribute("group"), elementChild.getAttribute("name"),
						                                    block,
						                                    result));
					}
					base.removeChild(child);
				} else {
					doModReqSanitation(child);
				}
			}
		}
	}
	
	/**
	 * Develop 'for' elements
	 */
	public static void doLogicPreprocessing(final Node root) throws InvalidXmlException {
		// process child first
		final NodeList children = root.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			doLogicPreprocessing(children.item(i));
		}
		
		// only process 'for' elements
		if (root.getNodeType() != Node.ELEMENT_NODE || !((Element) root).getTagName().equalsIgnoreCase("for")) {
			return;
		}
		final Element elementFor = (Element) root;
		
		// get variable name
		final String variableName = elementFor.getAttribute("variable");
		if(variableName.isEmpty()) {
			throw new InvalidXmlException("A for tag must include a variable attribute!");
		}
		
		// 'in' takes precedence over 'from' attribute
		if (elementFor.hasAttribute("in")) {
			final String[] inOptions = elementFor.getAttribute("in").split(",");
			
			// copy children with replaced variable
			for (final String variableValue : inOptions) {
				if (WarpDriveConfig.LOGGING_WORLD_GENERATION) {
					WarpDrive.logger.info(String.format("Resolving for-loop with variable %s = %s",
					                                    variableName, variableValue));
				}
				final NodeList allChildren = root.getChildNodes();
				for (int childIndex = 0; childIndex < allChildren.getLength(); childIndex ++) {
					final Node copy = copyNodeAndReplaceVariable(allChildren.item(childIndex), variableName, variableValue);
					root.getParentNode().appendChild(copy);
				}
			}
			
		} else {
			final String stringFrom = elementFor.getAttribute("from");
			final String stringTo = elementFor.getAttribute("to");
			
			if (stringTo.isEmpty() || stringFrom.isEmpty()) {
				throw new InvalidXmlException(String.format("For element with no 'in' attribute requires both 'from' and 'to' attributes! %s",
				                                            variableName));
			}
			
			final int intFrom;
			final int intTo;
			try {
				intFrom = Integer.parseInt(stringFrom);
				intTo = Integer.parseInt(stringTo);
			} catch (final NumberFormatException exception) {
				throw new InvalidXmlException(exception);
			}
			
			
			// copy children with replaced variable
			for (int variableValue = intFrom; variableValue <= intTo; variableValue++) {
				if (WarpDriveConfig.LOGGING_WORLD_GENERATION) {
					WarpDrive.logger.info(String.format("Resolving for-loop with variable %s = %d",
					                                    variableName, variableValue));
				}
				final NodeList allChildren = root.getChildNodes();
				for (int childIndex = 0; childIndex < allChildren.getLength(); childIndex++) {
					final Node copy = copyNodeAndReplaceVariable(allChildren.item(childIndex), variableName, "" + variableValue);
					root.getParentNode().appendChild(copy);
				}
			}
		}
		
		//Remove the old node
		root.getParentNode().removeChild(root);
		
		if (WarpDriveConfig.LOGGING_XML_PREPROCESSOR) {
			try {
				final Transformer transformer = TransformerFactory.newInstance().newTransformer();
				final Result output = new StreamResult(new File(String.format("output%s.xml", outputCount)));
				final Source input = new DOMSource(root.getOwnerDocument());
				
				transformer.transform(input, output);
				outputCount.incrementAndGet();
			} catch (final Exception exception) {
				exception.printStackTrace(WarpDrive.printStreamError);
			}
		}
	}
	
	private static Node copyNodeAndReplaceVariable(final Node nodeOriginal, final String variableName, final String variableValue) {
		final Node nodeCopy = nodeOriginal.cloneNode(true);
		replaceVariable(nodeCopy, "%" + variableName + "%", variableValue);
		
		return nodeCopy;
	}
	
	private static void replaceVariable(final Node node, final String keyword, final String value) {
		final ArrayList<String> nameToRemove = new ArrayList<>();
		final ArrayList<Attr> attrToAdd = new ArrayList<>();
		
		// process element's attributes first
		if (node.getNodeType() == Node.ELEMENT_NODE) {
			
			// compute the changes
			final NamedNodeMap attributes = node.getAttributes();
			for (int indexAttr = 0; indexAttr < attributes.getLength(); indexAttr++) {
				final Attr oldAttr = (Attr) attributes.item(indexAttr);
				final String oldName = oldAttr.getName();
				final String newName = oldName.replace(keyword, value);
				
				if (oldName.equals(newName)) {// same name, just adjust the value
					oldAttr.setValue(oldAttr.getValue().replace(keyword, value));
					
				} else {// different name, needs to defer the add/remove
					nameToRemove.add(oldName);
					
					final Attr newAttr = oldAttr.getOwnerDocument().createAttribute(newName);
					newAttr.setValue(oldAttr.getValue().replace(keyword, value));
					attrToAdd.add(newAttr);
				}
			}
			
			// then apply them
			for (final String attribute : nameToRemove) {
				attributes.removeNamedItem(attribute);
			}
			
			for (final Attr attribute : attrToAdd) {
				attributes.setNamedItem(attribute);
			}
		}
		
		// attributes are done, moving through child elements now
		final NodeList children = node.getChildNodes();
		for (int childIndex = 0; childIndex < children.getLength(); childIndex++) {
			final Node nodeChild = children.item(childIndex);
			
			switch (nodeChild.getNodeType()) {
			case Node.ELEMENT_NODE: // recurse through elements
				replaceVariable(nodeChild, keyword, value);
				break;
			case Node.TEXT_NODE: // replace text in place
				nodeChild.setTextContent(nodeChild.getTextContent().replace(keyword, value));
				break;
			default: // ignore others
				// no operation
				break;
			}
		}
	}
	
	public static class ModCheckResults {
		
		private final TreeMap<String, String> modResults;
		
		public ModCheckResults() {
			modResults = new TreeMap<>();
		}
		
		public void addMod(final String name, final String error) {
			modResults.put(name, error);
		}
		
		public boolean isEmpty() {
			return modResults.isEmpty();
		}
		
		@Override
		public String toString() {
			String string = (modResults.size() > 1 ? "{" : "");
			boolean isFirst = true;
			for (final Entry<String, String> entry : modResults.entrySet()) {
				if (isFirst) {
					isFirst = false;
				} else {
					string += ", ";
				}
				string += entry.getKey() + ": " + entry.getValue();
			}
			
			return string + (modResults.size() > 1 ? "}" : "");
		}
	}
}
