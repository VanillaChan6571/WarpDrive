package cr0s.warpdrive.item;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.api.IAirContainerItem;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.EnumAirTankTier;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemAirTank extends ItemAbstractBase implements IAirContainerItem {
	
	protected EnumAirTankTier enumAirTankTier;
	
	public ItemAirTank(final String registryName, final EnumAirTankTier enumAirTankTier) {
		super(registryName, enumAirTankTier.getTier());
		
		this.enumAirTankTier = enumAirTankTier;
		setMaxDamage(WarpDriveConfig.BREATHING_AIR_TANK_CAPACITY_BY_TIER[enumAirTankTier.getIndex()]);
		setMaxStackSize(1);
		setTranslationKey("warpdrive.breathing.air_tank." + enumAirTankTier.getName());
	}
	
	@Override
	public void getSubItems(@Nonnull final CreativeTabs creativeTab, @Nonnull final NonNullList<ItemStack> list) {
		if (!isInCreativeTab(creativeTab)) {
			return;
		}
		list.add(new ItemStack(this, 1, 0));
		list.add(new ItemStack(this, 1, getMaxDamage(null)));
	}
	
	@Override
	public boolean canContainAir(final ItemStack itemStack) {
		if ( itemStack == null
		  || itemStack.getItem() != this ) {
			return false;
		}
		return itemStack.getItemDamage() > 0;
	}
	
	@Override
	public int getMaxAirStorage(final ItemStack itemStack) {
		if ( itemStack == null
		  || itemStack.getItem() != this ) {
			return 0;
		}
		return itemStack.getMaxDamage();
	}
	
	@Override
	public int getCurrentAirStorage(final ItemStack itemStack) {
		if ( itemStack == null
		  || itemStack.getItem() != this ) {
			return 0;
		}
		return getMaxDamage(itemStack) - itemStack.getItemDamage();
	}
	
	@Override
	public ItemStack consumeAir(final ItemStack itemStack) {
		if ( itemStack == null
		  || itemStack.getItem() != this ) {
			return itemStack;
		}
		itemStack.setItemDamage(Math.min(getMaxDamage(itemStack), itemStack.getItemDamage() + 1)); // bypass unbreaking enchantment
		return itemStack;
	}
	
	@Override
	public int getAirTicksPerConsumption(final ItemStack itemStack) {
		if ( itemStack != null
		  && itemStack.getItem() != this ) {
			return 0;
		}
		return WarpDriveConfig.BREATHING_AIR_TANK_BREATH_DURATION_TICKS;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public ItemStack getEmptyAirContainer(final ItemStack itemStack) {
		if ( itemStack != null
		  && itemStack.getItem() != this ) {
			return itemStack;
		}
		return new ItemStack(this, 1, getMaxDamage());
	}
	
	@Override
	public ItemStack getFullAirContainer(final ItemStack itemStack) {
		if ( itemStack != null
		  && itemStack.getItem() != this ) {
			return itemStack;
		}
		return new ItemStack(this, 1);
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(@Nonnull final ItemStack itemStack, @Nullable final World world,
	                           @Nonnull final List<String> list, @Nonnull final ITooltipFlag advancedItemTooltips) {
		super.addInformation(itemStack, world, list, advancedItemTooltips);
		
		Commons.addTooltip(list, new TextComponentTranslation("item.warpdrive.breathing.air_tank.tooltip.usage").getFormattedText());
	}
}
