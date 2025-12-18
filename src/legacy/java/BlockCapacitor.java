package cr0s.warpdrive.block.energy;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IWarpTool;
import cr0s.warpdrive.block.BlockAbstractContainer;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.EnergyWrapper;
import cr0s.warpdrive.data.EnumDisabledInputOutput;
import cr0s.warpdrive.data.EnumTier;
import cr0s.warpdrive.event.ModelBakeEventHandler;
import cr0s.warpdrive.render.BakedModelCapacitor;

import ic2.api.energy.tile.IExplosionPowerOverride;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import net.minecraftforge.common.property.ExtendedBlockState;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;
import net.minecraftforge.common.property.Properties;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Optional.InterfaceList({
	@Optional.Interface(iface = "ic2.api.energy.tile.IExplosionPowerOverride", modid = "ic2")
})
public class BlockCapacitor extends BlockAbstractContainer implements IExplosionPowerOverride {
	
	public static final IProperty<EnumDisabledInputOutput> CONFIG = PropertyEnum.create("config", EnumDisabledInputOutput.class);
	
	public static final IUnlistedProperty<EnumDisabledInputOutput> DOWN  = Properties.toUnlisted(PropertyEnum.create("down", EnumDisabledInputOutput.class));
	public static final IUnlistedProperty<EnumDisabledInputOutput> UP    = Properties.toUnlisted(PropertyEnum.create("up", EnumDisabledInputOutput.class));
	public static final IUnlistedProperty<EnumDisabledInputOutput> NORTH = Properties.toUnlisted(PropertyEnum.create("north", EnumDisabledInputOutput.class));
	public static final IUnlistedProperty<EnumDisabledInputOutput> SOUTH = Properties.toUnlisted(PropertyEnum.create("south", EnumDisabledInputOutput.class));
	public static final IUnlistedProperty<EnumDisabledInputOutput> WEST  = Properties.toUnlisted(PropertyEnum.create("west", EnumDisabledInputOutput.class));
	public static final IUnlistedProperty<EnumDisabledInputOutput> EAST  = Properties.toUnlisted(PropertyEnum.create("east", EnumDisabledInputOutput.class));
	
	public BlockCapacitor(final String registryName, final EnumTier enumTier) {
		super(registryName, enumTier, Material.IRON);
		
		setTranslationKey("warpdrive.energy.capacitor." + enumTier.getName());
		
		setDefaultState(getDefaultState()
				                .withProperty(CONFIG, EnumDisabledInputOutput.DISABLED)
		               );
	}
	
	@Nonnull
	@Override
	protected BlockStateContainer createBlockState() {
		return new ExtendedBlockState(this,
		                              new IProperty[] { CONFIG },
		                              new IUnlistedProperty[] { DOWN, UP, NORTH, SOUTH, WEST, EAST });
	}
	
	@SuppressWarnings("deprecation")
	@Nonnull
	@Override
	public IBlockState getStateFromMeta(final int metadata) {
		return getDefaultState();
	}
	
	@Override
	public int getMetaFromState(@Nonnull final IBlockState blockState) {
		return 0;
	}
	
	@Nonnull
	@Override
	public IBlockState getExtendedState(@Nonnull final IBlockState blockState, final IBlockAccess blockAccess, final BlockPos blockPos) {
		if (!(blockState instanceof IExtendedBlockState)) {
			WarpDrive.logger.error(String.format("%s Invalid call to getExtendedState() with invalid state %s %s",
			                                     this, blockState, Commons.format(blockAccess, blockPos)));
			return blockState;
		}
		final TileEntity tileEntity = blockAccess.getTileEntity(blockPos);
		if (!(tileEntity instanceof TileEntityCapacitor)) {
			WarpDrive.logger.error(String.format("%s Invalid TileEntityCapacitor instance for %s %s: %s",
			                                     this, blockState, Commons.format(blockAccess, blockPos), tileEntity));
			return blockState;
		}
		final TileEntityCapacitor tileEntityCapacitor = (TileEntityCapacitor) tileEntity;
		return ((IExtendedBlockState) blockState)
				       .withProperty(DOWN , tileEntityCapacitor.getMode(EnumFacing.DOWN ))
				       .withProperty(UP   , tileEntityCapacitor.getMode(EnumFacing.UP   ))
				       .withProperty(NORTH, tileEntityCapacitor.getMode(EnumFacing.NORTH))
				       .withProperty(SOUTH, tileEntityCapacitor.getMode(EnumFacing.SOUTH))
				       .withProperty(WEST , tileEntityCapacitor.getMode(EnumFacing.WEST ))
				       .withProperty(EAST , tileEntityCapacitor.getMode(EnumFacing.EAST ));
	}
	
	@SuppressWarnings("deprecation")
	@Nonnull
	@Override
	public IBlockState getActualState(@Nonnull final IBlockState blockState, @Nonnull final IBlockAccess blockAccess, @Nonnull final BlockPos blockPos) {
		return super.getActualState(blockState, blockAccess, blockPos);
	}
	
	@Nonnull
	@Override
	public TileEntity createNewTileEntity(@Nonnull final World world, final int metadata) {
		return new TileEntityCapacitor();
	}
	
	@Nullable
	@Override
	public ItemBlock createItemBlock() {
		return new ItemBlockCapacitor(this);
	}
	
	@Override
	public int damageDropped(@Nonnull final IBlockState blockState) {
		return getMetaFromState(blockState);
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void getSubBlocks(@Nonnull final CreativeTabs creativeTab, @Nonnull final NonNullList<ItemStack> list) {
		ItemStack itemStack = new ItemStack(this, 1, 0);
		list.add(itemStack);
		if (enumTier != EnumTier.CREATIVE) {
			itemStack = new ItemStack(this, 1, 0);
			final NBTTagCompound tagCompound = new NBTTagCompound();
			tagCompound.setInteger(EnergyWrapper.TAG_ENERGY, WarpDriveConfig.CAPACITOR_MAX_ENERGY_STORED_BY_TIER[enumTier.getIndex()]);
			itemStack.setTagCompound(tagCompound);
			list.add(itemStack);
		}
	}
	
	@SideOnly(Side.CLIENT)
	@Override
	public void modelInitialisation() {
		super.modelInitialisation();
		
		// register (smart) baked model
		final ResourceLocation registryName = getRegistryName();
		assert registryName != null;
		for (final EnumDisabledInputOutput enumDisabledInputOutput : CONFIG.getAllowedValues()) {
			final String variant = String.format("%s=%s",
			                                     CONFIG.getName(), enumDisabledInputOutput);
			ModelBakeEventHandler.registerBakedModel(new ModelResourceLocation(registryName, variant), BakedModelCapacitor.class);
		}
	}
	
	// IExplosionPowerOverride overrides
	@Override
	public boolean shouldExplode() {
		return false;
	}
	
	@Override
	public float getExplosionPower(final int tier, final float defaultPower) {
		return defaultPower;
	}
	
	@Override
	public boolean onBlockActivated(@Nonnull final World world, @Nonnull final BlockPos blockPos, @Nonnull final IBlockState blockState,
	                                @Nonnull final EntityPlayer entityPlayer, @Nonnull final EnumHand enumHand,
	                                @Nonnull final EnumFacing enumFacing, final float hitX, final float hitY, final float hitZ) {
		if ( world.isRemote
		  || enumHand != EnumHand.MAIN_HAND ) {
			return super.onBlockActivated(world, blockPos, blockState, entityPlayer, enumHand, enumFacing, hitX, hitY, hitZ);
		}
		
		// get context
		final ItemStack itemStackHeld = entityPlayer.getHeldItem(enumHand);
		final TileEntity tileEntity = world.getTileEntity(blockPos);
		if (!(tileEntity instanceof TileEntityCapacitor)) {
			return super.onBlockActivated(world, blockPos, blockState, entityPlayer, enumHand, enumFacing, hitX, hitY, hitZ);
		}
		final TileEntityCapacitor tileEntityCapacitor = (TileEntityCapacitor) tileEntity;
		
		if ( !itemStackHeld.isEmpty()
		  && itemStackHeld.getItem() instanceof IWarpTool ) {
			if (entityPlayer.isSneaking()) {
				tileEntityCapacitor.setMode(enumFacing, tileEntityCapacitor.getMode(enumFacing).getPrevious());
			} else {
				tileEntityCapacitor.setMode(enumFacing, tileEntityCapacitor.getMode(enumFacing).getNext());
			}
			final ItemStack itemStack = new ItemStack(Item.getItemFromBlock(this), 1, getMetaFromState(blockState));
			switch (tileEntityCapacitor.getMode(enumFacing)) {
			case INPUT:
				Commons.addChatMessage(entityPlayer, Commons.getChatPrefix(itemStack)
				                                            .appendSibling(new TextComponentTranslation("warpdrive.energy.side.changed_to_input", enumFacing.name())));
				return true;
			case OUTPUT:
				Commons.addChatMessage(entityPlayer, Commons.getChatPrefix(itemStack)
				                                            .appendSibling(new TextComponentTranslation("warpdrive.energy.side.changed_to_output", enumFacing.name())));
				return true;
			case DISABLED:
			default:
				Commons.addChatMessage(entityPlayer, Commons.getChatPrefix(itemStack)
				                                            .appendSibling(new TextComponentTranslation("warpdrive.energy.side.changed_to_disabled", enumFacing.name())));
				return true;
			}
		}
		
		return super.onBlockActivated(world, blockPos, blockState, entityPlayer, enumHand, enumFacing, hitX, hitY, hitZ);
	}
}