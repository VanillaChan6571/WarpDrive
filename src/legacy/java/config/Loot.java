package cr0s.warpdrive.config;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IXmlRepresentableUnit;

import javax.annotation.Nonnull;

import org.w3c.dom.Element;

import java.util.Random;

import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Represents a single loot item.
 **/
public class Loot implements IXmlRepresentableUnit {
	
	public static final Loot DEFAULT;
	static {
		DEFAULT = new Loot();
		DEFAULT.name           = "-default-";
		DEFAULT.item           = Items.STICK;
		DEFAULT.damage         = 0;
		DEFAULT.tagCompound    = null;
		DEFAULT.quantityMin    = 0;
		DEFAULT.quantityMax    = 0;
	}
	
	private String name;
	public Item item;
	public int damage;
	public NBTTagCompound tagCompound = null;
	public int quantityMin;
	public int quantityMax;
	
	@Nonnull
	@Override
	public String getName() {
		return name;
	}
	
	public Loot() {
	}
	
	@Override
	public boolean loadFromXmlElement(final Element element) throws InvalidXmlException {
		
		// Check there is a block name
		if (!element.hasAttribute("item")) {
			throw new InvalidXmlException(String.format("Loot %s is missing an item attribute!",
			                                            element));
		}
		
		final String nameItem = element.getAttribute("item");
		item = Item.getByNameOrId(nameItem);
		if (item == null) {
			WarpDrive.logger.warn(String.format("Skipping missing item %s",
			                                    nameItem));
			return false;
		}
		
		// Get metadata attribute, defaults to 0
		damage = 0;
		final String stringDamage = element.getAttribute("damage");
		if (!stringDamage.isEmpty()) {
			try {
				damage = Integer.parseInt(stringDamage);
			} catch (final NumberFormatException exception) {
				throw new InvalidXmlException(String.format("Invalid damage for item %s",
				                                            nameItem));
			}
		}
		
		// Get nbt attribute, default to null/none
		tagCompound = null;
		final String stringNBT = element.getAttribute("nbt");
		if (!stringNBT.isEmpty()) {
			try {
				tagCompound = JsonToNBT.getTagFromJson(stringNBT);
			} catch (final NBTException exception) {
				WarpDrive.logger.error(exception.getMessage());
				throw new InvalidXmlException(String.format("Invalid nbt for item %s",
				                                            nameItem));
			}
		}
		
		// Get quantityMin attribute, defaults to 1
		quantityMin = 1;
		final String stringQuantityMin = element.getAttribute("minQuantity");
		if (!stringQuantityMin.isEmpty()) {
			try {
				quantityMin = Integer.parseInt(stringQuantityMin);
			} catch (final NumberFormatException exception) {
				throw new InvalidXmlException(String.format("Invalid minQuantity for item %s",
				                                            nameItem));
			}
		}
		
		// Get quantityMin attribute, defaults to 1
		quantityMax = 1;
		final String stringQuantityMax = element.getAttribute("maxQuantity");
		if (!stringQuantityMax.isEmpty()) {
			try {
				quantityMax = Integer.parseInt(stringQuantityMax);
			} catch (final NumberFormatException exception) {
				throw new InvalidXmlException(String.format("Invalid maxQuantity for item %s",
				                                            nameItem));
			}
		}
		
		name = String.format("%s@%d{%s}",
		                     nameItem, damage, tagCompound);
		
		return true;
	}
	
	public ItemStack getItemStack(final Random rand) {
		final int quantity = quantityMin + (quantityMax > quantityMin ? rand.nextInt(quantityMax - quantityMin) : 0);
		final ItemStack itemStack = new ItemStack(item, quantity, damage);
		if (tagCompound != null) {
			final NBTTagCompound tagCompoundNew = tagCompound.copy();
			itemStack.setTagCompound(tagCompoundNew);
		}
		return itemStack;
	}
	
	@Override
	public IXmlRepresentableUnit constructor() {
		return new Loot();
	}
	
	@Override
	public boolean equals(final Object object) {
		return object instanceof Loot
			&& (item == null || item.equals(((Loot) object).item))
			&& damage == ((Loot) object).damage
			&& (tagCompound == null || tagCompound.equals(((Loot) object).tagCompound));
	}
	
	@Override
	public String toString() {
		return String.format("Loot(%s@%d)",
		                     item.getRegistryName(), damage);
	}
	
	@Override
	public int hashCode() {
		return Item.getIdFromItem(item) * 16 + damage + (tagCompound == null ? 0 : tagCompound.hashCode() * 32768 * 16);
	}
}
