package cr0s.warpdrive.config;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IXmlRepresentableUnit;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Element;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Random;

/**
 * Manage 'unit' sets that will be chosen randomly during world generation.
 **/
public class GenericSetManager<E extends IXmlRepresentableUnit> extends XmlFileManager {
	
	private final String prefixFilename;
	private final String nameElementUnit;
	private final String nameElementSet;
	private final E unitDefault;
	
	// all GenericSets
	private HashMap<String, XmlRandomCollection<GenericSet<E>>> genericSetsByGroup;
	
	public GenericSetManager(final String prefixFilename, final String nameElementUnit, final String nameElementSet, final E unitDefault) {
		super();
		this.prefixFilename = prefixFilename;
		this.nameElementUnit = nameElementUnit;
		this.nameElementSet = nameElementSet;
		this.unitDefault = unitDefault;
	}
	
	public void load(final File dir) {
		genericSetsByGroup = new HashMap<>();
		load(dir, prefixFilename, nameElementSet);
		
		propagateGenericSets();
	}
	
	@Override
	protected void parseRootElement(final String location, final Element elementGenericSet) throws InvalidXmlException {
		final String group = elementGenericSet.getAttribute("group");
		if (group.isEmpty()) {
			throw new InvalidXmlException(location + " is missing a group attribute!");
		}
		
		final String name = elementGenericSet.getAttribute("name");
		if (name.isEmpty()) {
			throw new InvalidXmlException(location + " is missing a name attribute!");
		}
		
		if (WarpDriveConfig.LOGGING_WORLD_GENERATION) {
			WarpDrive.logger.info(String.format("- found %s %s:%s", nameElementSet, group, name));
		}
		
		final XmlRandomCollection<GenericSet<E>> xmlRandomCollection = genericSetsByGroup.computeIfAbsent(group, k -> new XmlRandomCollection<>());
		
		GenericSet<E> genericSet = xmlRandomCollection.getNamedEntry(name);
		if (genericSet == null) {
			genericSet = new GenericSet<>(group, name, unitDefault, nameElementUnit);
		}
		xmlRandomCollection.loadFromXML(genericSet, elementGenericSet);
	}
	
	private void propagateGenericSets() {
		HashMap<GenericSet<E>, ArrayList<String>> genericSetsDependencies = new HashMap<>();
		
		// scan for static import dependencies
		for (final XmlRandomCollection<GenericSet<E>> genericSets : genericSetsByGroup.values()) {
			for (final GenericSet<E> genericSet : genericSets.elements()) {
				final ArrayList<String> dependencies = genericSetsDependencies.computeIfAbsent(genericSet, k -> new ArrayList<>());
				dependencies.addAll(genericSet.getImportGroupNames());
			}
		}
		
		// resolve
		int iterationCount = 0;
		while (!genericSetsDependencies.isEmpty() && iterationCount++ < 10) {
			final HashMap<GenericSet<E>, ArrayList<String>> genericSetsLeftToImport = new HashMap<>();
			
			for (final Entry<GenericSet<E>, ArrayList<String>> entry : genericSetsDependencies.entrySet()) {
				final ArrayList<String> newDependencies = new ArrayList<>();
				for (final String dependency : entry.getValue()) {
					final GenericSet<E> genericSet = getGenericSet(dependency);
					if (genericSet == null) {
						WarpDrive.logger.error(String.format("Ignoring missing %s %s dependency in %s %s", nameElementSet, dependency, nameElementSet, entry.getKey()));
						
					} else if (genericSetsDependencies.containsKey(genericSet)) {
						// skip until it is loaded
						newDependencies.add(dependency);
						
					} else {
						try {
							if (WarpDriveConfig.LOGGING_WORLD_GENERATION) {
								WarpDrive.logger.info(String.format("Importing %s %s in %s ", nameElementSet, genericSet.getFullName(), entry.getKey().getFullName()));
							}
							entry.getKey().loadFrom(genericSet);
						} catch (final Exception exception) {
							exception.printStackTrace();
							WarpDrive.logger.error(String.format("While importing %s into %s %s", dependency, nameElementSet, entry.getKey().getFullName()));
						}
					}
				}
				if (!newDependencies.isEmpty()) {
					genericSetsLeftToImport.put(entry.getKey(), newDependencies);
				}
			}
			
			genericSetsDependencies = genericSetsLeftToImport;
		}
		
		// recursion has reach the limit?
		if (!genericSetsDependencies.isEmpty()) {
			WarpDrive.logger.error("Too many import recursions, ignoring the remaining ones:");
			for (final Entry<GenericSet<E>, ArrayList<String>> entry : genericSetsDependencies.entrySet()) {
				WarpDrive.logger.warn(String.format("- %s %s is pending:", nameElementSet, entry.getKey()));
				for (final String dependency : entry.getValue()) {
					WarpDrive.logger.warn(String.format(" + %s", dependency));
				}
			}
		}
	}
	
	public boolean doesGroupExist(final String groupName) {
		return genericSetsByGroup.get(groupName) != null;
	}
	
	public GenericSet<E> getRandomSetFromGroup(final Random random, final String groupName) {
		final XmlRandomCollection<GenericSet<E>> group = genericSetsByGroup.get(groupName);
		if (group == null) {
			WarpDrive.logger.warn(String.format("Invalid group %s, try one of %s",
			                                    groupName, StringUtils.join(genericSetsByGroup.keySet(), ", ") ));
			return null;
		}
		return group.getRandomEntry(random);
	}
	
	public GenericSet<E> getGenericSet(final String groupAndName) {
		final String[] parts = groupAndName.split(":");
		if (parts.length != 2) {
			WarpDrive.logger.error(String.format("Invalid %s '%s'. Expecting '{group}:{name}'", nameElementSet, groupAndName));
			return null;
		}
		final XmlRandomCollection<GenericSet<E>> group = genericSetsByGroup.get(parts[0]);
		if (group == null) {
			return null;
		}
		return group.getNamedEntry(parts[1]);
	}
}
