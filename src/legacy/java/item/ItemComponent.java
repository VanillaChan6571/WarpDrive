package cr0s.warpdrive.item;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IAirContainerItem;
import cr0s.warpdrive.block.BlockAbstractContainer;
import cr0s.warpdrive.data.EnumComponentType;
import cr0s.warpdrive.data.EnumTier;

import javax.annotation.Nonnull;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemComponent extends ItemAbstractBase implements IAirContainerItem {
	
	private static ItemStack[] itemStackCache;
	
	public ItemComponent(final String registryName, final EnumTier enumTier) {
		super(registryName, enumTier);
		
		setHasSubtypes(true);
		setTranslationKey("warpdrive.component.malformed");
		
		itemStackCache = new ItemStack[EnumComponentType.length];
	}
	
	@Nonnull
	public static ItemStack getItemStack(final EnumComponentType enumComponentType) {
		if (enumComponentType != null) {
			final int damage = enumComponentType.ordinal();
			if (itemStackCache[damage] == null) {
				itemStackCache[damage] = new ItemStack(WarpDrive.itemComponent, 1, damage);
			}
			return itemStackCache[damage];
		}
		return ItemStack.EMPTY;
	}
	
	@Nonnull
	public static ItemStack getItemStackNoCache(@Nonnull final EnumComponentType enumComponentType, final int amount) {
		return new ItemStack(WarpDrive.itemComponent, amount, enumComponentType.ordinal());
	}
	
	@Nonnull
	@Override
	public String getTranslationKey(final ItemStack itemStack) {
		final int damage = itemStack.getItemDamage();
		if (damage >= 0 && damage < EnumComponentType.length) {
			return "item.warpdrive.component." + EnumComponentType.get(damage).getName();
		}
		return getTranslationKey();
	}
	
	@Override
	public void getSubItems(@Nonnull final CreativeTabs creativeTab, @Nonnull final NonNullList<ItemStack> list) {
		if (!isInCreativeTab(creativeTab)) {
			return;
		}
		for(final EnumComponentType enumComponentType : EnumComponentType.values()) {
			list.add(new ItemStack(this, 1, enumComponentType.ordinal()));
		}
	}
	
	@Nonnull
	@SideOnly(Side.CLIENT)
	@Override
	public ModelResourceLocation getModelResourceLocation(final ItemStack itemStack) {
		final int damage = itemStack.getItemDamage();
		ResourceLocation resourceLocation = getRegistryName();
		assert resourceLocation != null;
		if (damage >= 0 && damage < EnumComponentType.length) {
			resourceLocation = new ResourceLocation(resourceLocation.getNamespace(), resourceLocation.getPath() + "-" + EnumComponentType.get(damage).getName());
		}
		return new ModelResourceLocation(resourceLocation, "inventory");
	}
	
	// IAirContainerItem overrides for empty air canister
	@Override
	public boolean canContainAir(final ItemStack itemStack) {
		return (itemStack.getItem() instanceof ItemComponent && itemStack.getItemDamage() == EnumComponentType.AIR_CANISTER.ordinal());
	}
	
	@Override
	public int getMaxAirStorage(final ItemStack itemStack) {
		if (canContainAir(itemStack)) {
			return WarpDrive.itemAirTanks[0].getMaxAirStorage(itemStack);
		} else {
			return 0;
		}
	}
	
	@Override
	public int getCurrentAirStorage(final ItemStack itemStack) {
		return 0;
	}
	
	@Override
	public ItemStack consumeAir(final ItemStack itemStack) {
		WarpDrive.logger.error(String.format("%s consumeAir() with itemStack %s",
		                                     this, itemStack));
		throw new RuntimeException("Invalid call to consumeAir() on non or empty container");
	}
	
	@Override
	public int getAirTicksPerConsumption(final ItemStack itemStack) {
		if (canContainAir(itemStack)) {
			return WarpDrive.itemAirTanks[0].getAirTicksPerConsumption(new ItemStack(WarpDrive.itemAirTanks[0]));
		} else {
			return 0;
		}
	}
	
	@Override
	public ItemStack getFullAirContainer(final ItemStack itemStack) {
		if (canContainAir(itemStack)) {
			return WarpDrive.itemAirTanks[0].getFullAirContainer(new ItemStack(WarpDrive.itemAirTanks[0]));
		}
		return null;
	}
	
	@Override
	public ItemStack getEmptyAirContainer(final ItemStack itemStack) {
		if (canContainAir(itemStack)) {
			return WarpDrive.itemAirTanks[0].getEmptyAirContainer(new ItemStack(WarpDrive.itemAirTanks[0]));
		}
		return null;
	}
	
	@Override
	public boolean doesSneakBypassUse(@Nonnull final ItemStack itemStack,
	                                  @Nonnull final IBlockAccess blockAccess, @Nonnull final BlockPos blockPos,
	                                  @Nonnull final EntityPlayer player) {
		final Block block = blockAccess.getBlockState(blockPos).getBlock();
		
		return block instanceof BlockAbstractContainer
		    || super.doesSneakBypassUse(itemStack, blockAccess, blockPos, player);
	}
}