package cr0s.warpdrive.config.structures;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IXmlRepresentable;
import cr0s.warpdrive.config.InvalidXmlException;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.config.XmlFileManager;

import javax.annotation.Nonnull;

import net.minecraft.util.math.BlockPos;
import org.w3c.dom.Element;

import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.world.World;

public class MetaOrb extends Orb {
	protected MetaShell metaShell;
	
	public MetaOrb(final String group, final String name) {
		super(group, name);
	}
	
	@Override
	public boolean loadFromXmlElement(final Element element) throws InvalidXmlException {
		super.loadFromXmlElement(element);
		
		final List<Element> listMetaShells = XmlFileManager.getChildrenElementByTagName(element, "metaShell");
		if (listMetaShells.size() > 1) {
			throw new InvalidXmlException(String.format("Too many metaShell defined in structure %s. Maximum is 1.",
			                                            getFullName()));
		}
		if (listMetaShells.size() == 1) {
			metaShell = new MetaShell(getFullName());
			metaShell.loadFromXmlElement(listMetaShells.get(0));
		}
		
		return true;
	}
	
	@Override
	public boolean generate(@Nonnull final World world, @Nonnull final Random random, @Nonnull final BlockPos blockPos) {
		return instantiate(random).generate(world, random, blockPos);
	}
	
	@Override
	public AbstractStructureInstance instantiate(final Random random) {
		return new MetaOrbInstance(this, random);
	}
	
	public class MetaShell implements IXmlRepresentable {
		private final String parentFullName;
		protected Block block;
		protected int metadata;
		protected int minCount;
		protected int maxCount;
		protected double minRadius;
		protected double relativeRadius;
		
		public MetaShell(final String parentFullName) {
			this.parentFullName = parentFullName;
		}
		
		@Nonnull
		@Override
		public String getName() {
			return "metaShell";
		}
		
		@Override
		public boolean loadFromXmlElement(final Element element) throws InvalidXmlException {
			if (WarpDriveConfig.LOGGING_WORLD_GENERATION) {
				WarpDrive.logger.info("  + found metaShell");
			}
			
			String stringValue;
			
			// block & metadata
			stringValue = element.getAttribute("block");
			if (!stringValue.isEmpty()) {
				
				block = Block.getBlockFromName(stringValue);
				if (block == null) {
					WarpDrive.logger.warn(String.format("Skipping missing metaShell core block %s in %s",
					                                    stringValue, parentFullName));
				} else {
					// metadata
					stringValue = element.getAttribute("metadata");
					if (stringValue.isEmpty()) {
						metadata = 0;
					} else {
						try {
							metadata = Integer.parseInt(stringValue);
						} catch (final NumberFormatException exception) {
							throw new InvalidXmlException(String.format("Failed to load metaShell from structure %s due to invalid metadata %s, expecting an integer",
							                                            parentFullName, stringValue));
						}
					}
					
					if (metadata < 0 || metadata > 15) {
						throw new InvalidXmlException(String.format("Failed to load metaShell from structure %s due to invalid metadata %d, expecting a value between 0 and 15 included",
						                                            parentFullName, metadata));
					}
				}
			}
			
			// count
			try {
				minCount = Integer.parseInt(element.getAttribute("minCount"));
			} catch (final NumberFormatException exception) {
				throw new InvalidXmlException(String.format("Failed to load metaShell from structure %s due to invalid minCount %s, expecting an integer",
				                                            parentFullName, element.getAttribute("minCount")));
			}
			
			if (minCount < 1) {
				throw new InvalidXmlException(String.format("Failed to load metaShell from structure %s due to invalid minCount %d, expecting greater then 0",
				                                            parentFullName, minCount));
			}
			
			try {
				maxCount = Integer.parseInt(element.getAttribute("maxCount"));
			} catch (final NumberFormatException exception) {
				throw new InvalidXmlException(String.format("Failed to load metaShell from structure %s due to invalid maxCount %s, expecting an integer",
				                                            parentFullName, element.getAttribute("maxCount")));
			}
			
			if (maxCount < minCount) {
				throw new InvalidXmlException(String.format("Failed to load metaShell from structure %s due to invalid maxCount %d, expecting greater than or equal to minCount %d",
				                                            parentFullName, maxCount, minCount));
			}
			
			// radius
			try {
				stringValue = element.getAttribute("minRadius");
				if (stringValue.isEmpty()) {
					minRadius = 2;
				} else {
					minRadius = Double.parseDouble(element.getAttribute("minRadius"));
				}
			} catch (final NumberFormatException exception) {
				throw new InvalidXmlException(String.format("Failed to load metaShell from structure %s due to invalid minRadius %s, expecting a double",
				                                            parentFullName, element.getAttribute("minRadius")));
			}
			
			if (minRadius < 0.0D || minRadius > 20.0D) {
				throw new InvalidXmlException(String.format("Failed to load metaShell from structure %s due to invalid minRadius %.3f, expecting a value between 0.0 and 20.0 included",
				                                            parentFullName, minRadius));
			}
			
			try {
				stringValue = element.getAttribute("relativeRadius");
				if (stringValue.isEmpty()) {
					relativeRadius = 0.5;
				} else {
					relativeRadius = Double.parseDouble(element.getAttribute("relativeRadius"));
				}
			} catch (final NumberFormatException exception) {
				throw new InvalidXmlException(String.format("Failed to load metaShell from structure %s due to invalid relativeRadius %s, expecting a double",
				                                            parentFullName, element.getAttribute("relativeRadius")));
			}
			
			if (relativeRadius < 0.0D || relativeRadius > 2.0D) {
				throw new InvalidXmlException(String.format("Failed to load metaShell from structure %s due to invalid relativeRadius %.3f, expecting a value between 0.0 and 2.0 included",
				                                            parentFullName, relativeRadius));
			}
			
			return true;
		}
	}
}
