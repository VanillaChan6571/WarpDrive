package cr0s.warpdrive.config.structures;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.config.Filler;
import cr0s.warpdrive.config.GenericSet;
import cr0s.warpdrive.config.InvalidXmlException;
import cr0s.warpdrive.config.Loot;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.config.XmlFileManager;
import cr0s.warpdrive.data.JumpBlock;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import org.w3c.dom.Element;

public class Schematic extends AbstractStructure {
	
	protected HashMap<String, Integer> filenames;
	protected Replacement[] replacements;
	protected Insertion[] insertions;
	
	public Schematic(final String group, final String name) {
		super(group, name);
	}
	
	public String getRandomFileName(final Random random) {
		
		// In loadFromXmlElement, it's already checked that there must be at least 1 "schematic" xml node
		// therefore, this should not be possible
		assert(!filenames.isEmpty());
		
		int totalWeight = 0;
		for (final int weight : filenames.values()) {
			totalWeight += weight;
		}
		int result = random.nextInt(totalWeight);
		for (final Map.Entry<String, Integer> entry : filenames.entrySet()) {
			result -= entry.getValue();
			if (result <= 0) {
				return entry.getKey();
			}
		}
		return filenames.keySet().iterator().next();
	}
	
	@Override
	public boolean generate(@Nonnull final World world, @Nonnull final Random random, @Nonnull final BlockPos blockPos) {
		return instantiate(random).generate(world, random, blockPos);
	}
	
	@Override
	public AbstractStructureInstance instantiate(final Random random) {
		return new SchematicInstance(this, random);
	}
	
	@Override
	public boolean loadFromXmlElement(final Element element) throws InvalidXmlException {
		super.loadFromXmlElement(element);
		
		final List<Element> fileNameList = XmlFileManager.getChildrenElementByTagName(element, "schematic");
		if (fileNameList.isEmpty()) {
			throw new InvalidXmlException("Must have one schematic node with file name!");
		}
		this.filenames = new HashMap<>(fileNameList.size());
		for (final Element entry : fileNameList) {
			final String filename = entry.getAttribute("filename");
			int weight = 1;
			try {
				weight = Integer.parseInt(entry.getAttribute("weight"));
			} catch (final NumberFormatException numberFormatException) {
				throw new InvalidXmlException(String.format("Invalid weight in schematic %s of structure %s:%s",
				                                            filename, group, name));
			}
			this.filenames.put(filename, weight);
		}
		
		// load all replacement elements
		final List<Element> listReplacements = XmlFileManager.getChildrenElementByTagName(element, "replacement");
		replacements = new Replacement[listReplacements.size()];
		int replacementIndexOut = 0;
		for (final Element elementReplacement : listReplacements) {
			final String blockState = elementReplacement.getAttribute("blockState");
			
			replacements[replacementIndexOut] = new Replacement(getFullName(), blockState);
			try {
				replacements[replacementIndexOut].loadFromXmlElement(elementReplacement);
				replacementIndexOut++;
			} catch (final InvalidXmlException exception) {
				exception.printStackTrace(WarpDrive.printStreamError);
				WarpDrive.logger.error(String.format("Skipping invalid replacement %s",
				                                     blockState));
			}
		}
		
		// load all insertion elements
		final List<Element> listInsertions = XmlFileManager.getChildrenElementByTagName(element, "insertion");
		insertions = new Insertion[listInsertions.size()];
		int insertionIndexOut = 0;
		for (final Element elementInsertion : listInsertions) {
			final String blockState = elementInsertion.getAttribute("blockState");
			
			insertions[insertionIndexOut] = new Insertion(getFullName(), blockState);
			try {
				insertions[insertionIndexOut].loadFromXmlElement(elementInsertion);
				insertionIndexOut++;
			} catch (final InvalidXmlException exception) {
				exception.printStackTrace(WarpDrive.printStreamError);
				WarpDrive.logger.error(String.format("Skipping invalid insertion %s",
				                                     blockState));
			}
		}
		
		return true;
	}
	
	static class BlockMatcher {
		
		IBlockState blockState;
		
		public static BlockMatcher fromXmlElement(final Element element, final GenericSet<?> caller) throws InvalidXmlException{
			final String blockStateString = element.getAttribute("blockState");
			final String blockNameString = element.getAttribute("block");
			final String metaString = element.getAttribute("metadata");
			
			final BlockMatcher blockMatcher;
			
			if(blockNameString.isEmpty()){
				blockMatcher = BlockMatcher.fromBlockStateString(blockStateString);
			}else {
				blockMatcher = BlockMatcher.fromBlockAndMeta(blockNameString, metaString);
			}
			
			if (blockMatcher == null){
				WarpDrive.logger.warn(String.format("Invalid matching scheme %s found for %s",
				                                    blockStateString.isEmpty() ? blockNameString + "@" + metaString : blockStateString,
				                                    caller.getFullName()));
			}
			
			return blockMatcher;
		}
		
		public static BlockMatcher fromBlockStateString(final String blockStateString) {
			// TODO: allow different data input type for meta: range (e.g. 2-13), comma separated list (e.g. 1,2,3..), multiple property (e.g. variant=oak,half=bottom)
			
			final BlockMatcher result = new BlockMatcher();
			
			String blockNameString = "";
			String metaString = "*";
			if (blockStateString.contains("@")) {// (with metadata)
				final String[] blockStateParts = blockStateString.split("@");
				blockNameString = blockStateParts[0].trim();
				metaString = blockStateParts[1].trim();
			} else {// (without metadata)
				blockNameString = blockStateString;
			}
			final Block block = Block.getBlockFromName(blockNameString);
			if (block == null) {
				WarpDrive.logger.warn(String.format("Ignoring invalid block with name %s.", blockNameString));
				return null;
			}
			if (metaString.equals("*")) {// (no metadata or explicit wildcard)
				result.blockState = block.getDefaultState();
			} else if (metaString.contains("=")) {// (in string format (e.g. "color=red"))
				final String[] metaParts = metaString.split("=");
				final String propertyKey = metaParts[0].trim();
				final String propertyValue = metaParts[1].trim();
				final IProperty<? extends Comparable<?>> property = block.getBlockState().getProperty(propertyKey);
				if (property == null) {
					WarpDrive.logger.warn(String.format("Found invalid block property %s for block %s", propertyKey, blockNameString));
					return null;
				}
				
				/*
					Note: the below code was attempted but not succeeded.
					IBlockState#WithProperty require (T extends Comparable<?> property , V extend T value).
					It is impossible to ensure V extend T because value returned from parseValue is itself <? extends Comparable<?>>
					Therefore, T may only be determined at runtime, and V extend T may not be enforced.
					
					Optional<? extends Comparable<?>> parsedValue = property.parseValue(propertyValue);
					if (result.isPresent()){
						result.blockState = block.getDefaultState().withProperty(property, parsedValue.get());
					}else{
						WarpDrive.logger.warn(String.format("Value %s is not allowed for property %s for block %s", propertyValue, propertyKey, state));
					}
				*/
				
				boolean found = false;
				for (int i = 0; i < 16; i++) {// not efficient, but this would work (and since it's load time, it should not be a problem)
					final IBlockState tmpState = block.getStateFromMeta(i);
					if (tmpState.getProperties().get(property).equals(property.parseValue(propertyValue).orNull())) {
						result.blockState = tmpState;
						found = true;
						break;
					}
				}
				if (!found) {
					WarpDrive.logger.warn(String.format("Failed to find metadata value that represent block property %s for block %s", propertyKey, blockNameString));
					return null;
				}
				
			} else {// (metadata)
				final int metadata;
				try {
					metadata = Integer.parseInt(metaString);
				} catch (final NumberFormatException numberFormatException) {
					WarpDrive.logger.warn(String.format("%s is not a valid number for metadata of block %s", metaString, blockNameString));
					return null;
				}
				result.blockState = block.getStateFromMeta(metadata);
			}
			return result;
		}
		
		public static BlockMatcher fromBlockAndMeta(final String blockName, final String metaString) {
			final BlockMatcher result = new BlockMatcher();
			
			final Block block = Block.getBlockFromName(blockName);
			if (block == null) {
				WarpDrive.logger.warn(String.format("Found invalid block %s", blockName));
				return null;
			}
			
			if (!metaString.isEmpty()) {
				try {
					final int meta = Integer.parseInt(metaString);
					result.blockState = block.getStateFromMeta(meta);
				} catch (final NumberFormatException numberFormatException) {
					WarpDrive.logger.warn(String.format("%s is not a valid number for meta of block %s", metaString, blockName));
					return null;
				}
			}
			return result;
		}
		
		public boolean isMatching(final IBlockState blockStateIn) {
			return blockStateIn.equals(blockState);
		}
		
		public boolean isMatching(final JumpBlock jumpBlockIn) {
			return blockState != null && jumpBlockIn != null && jumpBlockIn.blockMeta == blockState.getBlock().getMetaFromState(blockState);
		}
		
		@Override
		public String toString() {
			return "BlockMatcher{" + (blockState == null ? "null" : blockState.toString()) + "}";
		}
	}
	
	public static class Replacement extends GenericSet<Filler> {
		
		private final String parentFullName;
		protected BlockMatcher matcher;
		
		public Replacement(final String parentFullName, final String name) {
			super(null, name, Filler.DEFAULT, "filler");
			this.parentFullName = parentFullName;
		}
		
		@Override
		public boolean loadFromXmlElement(final Element element) throws InvalidXmlException {
			super.loadFromXmlElement(element);
			
			matcher = BlockMatcher.fromXmlElement(element, this);
			
			if ( WarpDriveConfig.LOGGING_WORLD_GENERATION
			  && matcher != null ) {
				WarpDrive.logger.info(String.format("  + found replacement for block %s", matcher));
			}
			
			// resolve static imports
			for (final String importGroupName : getImportGroupNames()) {
				final GenericSet<Filler> fillerSet = WarpDriveConfig.FillerManager.getGenericSet(importGroupName);
				if (fillerSet == null) {
					WarpDrive.logger.warn(String.format("Skipping missing FillerSet %s in replacement %s of structure %s",
					                                    importGroupName, name, parentFullName));
				} else {
					loadFrom(fillerSet);
				}
			}
			
			// validate dynamic imports
			for (final String importGroup : getImportGroups()) {
				if (!WarpDriveConfig.FillerManager.doesGroupExist(importGroup)) {
					WarpDrive.logger.warn(String.format("An invalid FillerSet group %s is referenced in replacement %s of structure %s",
					                                    importGroup, name, parentFullName));
				}
			}
			
			return true;
		}
		
		public Replacement instantiate(final Random random) {
			final Replacement replacement = new Replacement(parentFullName, name);
			replacement.matcher = this.matcher;
			try {
				replacement.loadFrom(this);
				for (final String importGroup : getImportGroups()) {
					final GenericSet<Filler> fillerSet = WarpDriveConfig.FillerManager.getRandomSetFromGroup(random, importGroup);
					if (fillerSet == null) {
						WarpDrive.logger.warn(String.format("Ignoring invalid group %s in replacement %s of structure %s",
						                                    importGroup, name, parentFullName));
						continue;
					}
					if (WarpDriveConfig.LOGGING_WORLD_GENERATION) {
						WarpDrive.logger.info(String.format("Filling %s:%s with %s:%s",
						                                    parentFullName, name, importGroup, fillerSet.getName()));
					}
					replacement.loadFrom(fillerSet);
				}
			} catch (final Exception exception) {
				exception.printStackTrace(WarpDrive.printStreamError);
				WarpDrive.logger.error(String.format("Failed to instantiate replacement %s from structure %s",
				                                     name, parentFullName));
			}
			if (replacement.isEmpty()) {
				if (WarpDriveConfig.LOGGING_WORLD_GENERATION) {
					WarpDrive.logger.info(String.format("Ignoring empty replacement %s in structure %s",
					                                    name, parentFullName));
				}
				return null;
			}
			return replacement;
		}
		
		public boolean isMatching(final IBlockState blockStateIn) {
			return matcher != null && matcher.isMatching(blockStateIn);
		}
		
		public boolean isMatching(final JumpBlock jumpBlockIn) {
			return matcher != null && matcher.isMatching(jumpBlockIn);
		}
	}
	
	public static class Insertion extends GenericSet<Loot> {
		
		private final String parentFullName;
		protected BlockMatcher matcher;
		private int minQuantity;
		private int maxQuantity;
		private int maxRetries;
		
		public Insertion(final String parentFullName, final String name) {
			super(null, name, Loot.DEFAULT, "loot");
			this.parentFullName = parentFullName;
		}
		
		@Override
		public boolean loadFromXmlElement(final Element element) throws InvalidXmlException {
			super.loadFromXmlElement(element);
			
			matcher = BlockMatcher.fromXmlElement(element, this);
			
			if ( WarpDriveConfig.LOGGING_WORLD_GENERATION
			  && matcher != null ) {
				WarpDrive.logger.info(String.format("  + found insertion for block %s", matcher));
			}
			
			// get optional minQuantity attribute, defaulting to 0
			minQuantity = 0;
			final String stringMinQuantity = element.getAttribute("minQuantity");
			if (!stringMinQuantity.isEmpty()) {
				minQuantity = Integer.parseInt(stringMinQuantity);
			}
			
			// get optional maxQuantity attribute, defaulting to 7
			maxQuantity = 7;
			final String stringMaxQuantity = element.getAttribute("minQuantity");
			if (!stringMaxQuantity.isEmpty()) {
				maxQuantity = Integer.parseInt(stringMaxQuantity);
			}
			
			// get optional maxTries attribute, defaulting to 3 according to WorldGenStructure#fillInventoryWithLoot
			maxRetries = 3;
			final String stringMaxTries = element.getAttribute("maxRetries");
			if (!stringMaxTries.isEmpty()) {
				maxRetries = Integer.parseInt(stringMaxTries);
			}
			
			// resolve static imports
			for (final String importGroupName : getImportGroupNames()) {
				final GenericSet<Loot> lootSet = WarpDriveConfig.LootManager.getGenericSet(importGroupName);
				if (lootSet == null) {
					WarpDrive.logger.warn(String.format("Skipping missing LootSet %s in insertion %s of structure %s",
					                                    importGroupName, name, parentFullName));
				} else {
					loadFrom(lootSet);
				}
			}
			
			// validate dynamic imports
			for (final String importGroup : getImportGroups()) {
				if (!WarpDriveConfig.LootManager.doesGroupExist(importGroup)) {
					WarpDrive.logger.warn(String.format("An invalid LootSet group %s is referenced in insertion %s of structure %s",
					                                    importGroup, name, parentFullName));
				}
			}
			
			return true;
		}
		
		public Insertion instantiate(final Random random) {
			final Insertion insertion = new Insertion(parentFullName, name);
			insertion.minQuantity = minQuantity;
			insertion.maxQuantity = maxQuantity;
			insertion.maxRetries  = maxRetries;
			insertion.matcher     = matcher;
			try {
				insertion.loadFrom(this);
				for (final String importGroup : getImportGroups()) {
					final GenericSet<Loot> lootSet = WarpDriveConfig.LootManager.getRandomSetFromGroup(random, importGroup);
					if (lootSet == null) {
						WarpDrive.logger.warn(String.format("Ignoring invalid group %s in insertion %s of structure %s",
						                                    importGroup, name, parentFullName));
						continue;
					}
					if (WarpDriveConfig.LOGGING_WORLD_GENERATION) {
						WarpDrive.logger.info(String.format("Inserting %s:%s with %s:%s",
						                                    parentFullName, name, importGroup, lootSet.getName()));
					}
					insertion.loadFrom(lootSet);
				}
			} catch (final Exception exception) {
				exception.printStackTrace(WarpDrive.printStreamError);
				WarpDrive.logger.error(String.format("Failed to instantiate insertion %s from structure %s",
				                                     name, parentFullName));
			}
			if (insertion.isEmpty()) {
				if (WarpDriveConfig.LOGGING_WORLD_GENERATION) {
					WarpDrive.logger.info(String.format("Ignoring empty insertion %s in structure %s",
					                                    name, parentFullName));
				}
				return null;
			}
			return insertion;
		}
		
		public int getMinQuantity() {
			return minQuantity;
		}
		
		public int getMaxQuantity() {
			return maxQuantity;
		}
		
		public int getMaxRetries() {
			return maxRetries;
		}
		
		public boolean isMatching(final IBlockState blockStateIn) {
			return matcher != null && matcher.isMatching(blockStateIn);
		}
		
		public boolean isMatching(final JumpBlock jumpBlockIn) {
			return matcher != null && matcher.isMatching(jumpBlockIn);
		}
	}
}
