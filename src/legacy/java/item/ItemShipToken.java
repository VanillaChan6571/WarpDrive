package cr0s.warpdrive.item;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.data.EnumTier;

import java.util.List;
import java.util.Random;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemShipToken extends ItemAbstractBase {	
	
	private static ItemStack[] itemStackCache;
	private static final int[] VALID_METADATAS = { 0, 1, 2, 3, 4, 5, 10, 11, 12, 13, 14, 15, 20, 21, 22, 23, 24, 25, 30, 31, 32, 33, 34, 35, 40, 41, 42, 43, 44, 45 };
	
	public ItemShipToken(final String registryName, final EnumTier enumTier) {
		super(registryName, enumTier);
		
		setHasSubtypes(true);
		setTranslationKey("warpdrive.tool.ship_token");
		setCreativeTab(WarpDrive.creativeTabMain);
		
		itemStackCache = new ItemStack[VALID_METADATAS.length];
	}
	
	public static ItemStack getItemStack(final Random random) {
		return getItemStack(VALID_METADATAS[random.nextInt(VALID_METADATAS.length)]);
	}
	
	public static ItemStack getItemStack(final int metadataWanted) {
		for (int index = 0; index < VALID_METADATAS.length; index++) {
			if (metadataWanted == VALID_METADATAS[index]) {
				if (itemStackCache[index] == null) {
					itemStackCache[index] = new ItemStack(WarpDrive.itemShipToken, 1, metadataWanted);
				}
				return itemStackCache[index];
			}
		}
		return null;
	}
	
	@Nonnull
	public static ItemStack getItemStackNoCache(final int metadataWanted, final int amount) {
		for (final int metadataValid : VALID_METADATAS) {
			if (metadataWanted == metadataValid) {
				return new ItemStack(WarpDrive.itemShipToken, amount, metadataWanted);
			}
		}
		return new ItemStack(WarpDrive.itemShipToken, amount, 0);
	}
	
	@Nonnull
	@Override
	public String getTranslationKey(final ItemStack itemStack) {
		final int metadata = itemStack.getItemDamage();
		for (final int metadataValid : VALID_METADATAS) {
			if (metadata == metadataValid) {
				return "item.warpdrive.tool.ship_token" + metadata;
			}
		}
		return getTranslationKey();
	}
	
	@Override
	public void getSubItems(@Nonnull final CreativeTabs creativeTab, @Nonnull final NonNullList<ItemStack> list) {
		if (!isInCreativeTab(creativeTab)) {
			return;
		}
		for (final int metadataValid : VALID_METADATAS) {
			list.add(new ItemStack(this, 1, metadataValid));
		}
	}
	
	public static String getSchematicName(final ItemStack itemStack) {
		String schematicName = "" + itemStack.getItemDamage();
		final NBTTagCompound tagCompound = itemStack.getTagCompound();
		if (tagCompound != null && tagCompound.hasKey("shipName")) {
			schematicName = tagCompound.getString("shipName");
		}
		return schematicName;
	}
	
	public static void setSchematicName(final ItemStack itemStack, final String schematicName) {
		if (!itemStack.hasTagCompound()) {
			itemStack.setTagCompound(new NBTTagCompound());
		}
		final NBTTagCompound tagCompound = itemStack.getTagCompound();
		assert tagCompound != null;
		tagCompound.setString("shipName", schematicName);
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(@Nonnull final ItemStack itemStack, @Nullable final World world,
	                           @Nonnull final List<String> list, @Nonnull final ITooltipFlag advancedItemTooltips) {
		super.addInformation(itemStack, world, list, advancedItemTooltips);
		
		Commons.addTooltip(list, new TextComponentTranslation("item.warpdrive.tool.ship_token.tooltip.usage",
		                                                      getSchematicName(itemStack)).getFormattedText());
	}
}