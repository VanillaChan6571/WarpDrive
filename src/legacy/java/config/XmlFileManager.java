package cr0s.warpdrive.config;

import cr0s.warpdrive.WarpDrive;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class XmlFileManager {
	
	protected void load(final File dir, final String prefixFilename, final String nameElement) {
		
		// (directory is created by caller, so it can copy default files if any)
		
		if (!dir.isDirectory()) {
			throw new IllegalArgumentException(String.format("File path %s must be a directory!",
			                                                 dir.getPath()));
		}
		
		final File[] files = dir.listFiles((file_notUsed, name) -> name.startsWith(prefixFilename) && name.endsWith(".xml"));
		if (files == null || files.length == 0) {
			throw new IllegalArgumentException(String.format("File path %s contains no %s*.xml files!",
			                                                 dir.getPath(), prefixFilename));
		}
		
		for (final File file : files) {
			try {
				WarpDrive.logger.info(String.format("Loading configuration file %s", file.getName()));
				final Document document = WarpDriveConfig.getXmlDocumentBuilder().parse(file);
				
				// pre-process the file
				final String result = XmlPreprocessor.checkModRequirements(document.getDocumentElement());
				if (!result.isEmpty()) {
					WarpDrive.logger.info(String.format("Skipping configuration file %s due to %s", file.getName(), result));
					continue;
				}
				
				XmlPreprocessor.doModReqSanitation(document);
				XmlPreprocessor.doLogicPreprocessing(document);
				
				// only add selected root elements
				final List<Element> listElements = getChildrenElementByTagName(document.getDocumentElement(), nameElement);
				for (int indexElement = 0; indexElement < listElements.size(); indexElement++) {
					final Element element = listElements.get(indexElement);
					final String location = String.format("%s %d/%d", nameElement, indexElement + 1, listElements.size());
					parseRootElement(location, element);
				}
			} catch (final Exception exception) {
				exception.printStackTrace(WarpDrive.printStreamError);
				WarpDrive.logger.error(String.format("Error loading file %s: %s",
				                                     file.getName(), exception.getMessage() ));
			}
		}
	}
	
	protected abstract void parseRootElement(final String location, final Element elementCelestialObject) throws InvalidXmlException, SAXException, IOException;
	
	public static List<Element> getChildrenElementByTagName(final Element parent, final String name) {
		final List<Element> listElements = new ArrayList<>();
		
		for (Node nodeChild = parent.getFirstChild(); nodeChild != null; nodeChild = nodeChild.getNextSibling()) {
			if ( nodeChild.getNodeType() == Node.ELEMENT_NODE
			  && name.equals(nodeChild.getNodeName()) ) {
				listElements.add((Element) nodeChild);
			}
		}
		
		return listElements;
	}
}
