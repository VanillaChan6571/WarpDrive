package cr0s.warpdrive.block.detection;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IGlobalRegionProvider;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.block.TileEntityAbstractEnergyCoreOrController;
import cr0s.warpdrive.block.movement.TileEntityShipCore;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.BlockProperties;
import cr0s.warpdrive.data.EnergyWrapper;
import cr0s.warpdrive.data.EnumComponentType;
import cr0s.warpdrive.data.EnumGlobalRegionType;
import cr0s.warpdrive.data.GlobalRegion;
import cr0s.warpdrive.data.GlobalRegionManager;
import cr0s.warpdrive.item.ItemComponent;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.ArrayList;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

import net.minecraftforge.fml.common.Optional;

public class TileEntityVirtualAssistant extends TileEntityAbstractEnergyCoreOrController implements IGlobalRegionProvider {
	
	// global properties
	private static final UpgradeSlot upgradeSlotSecurity = new UpgradeSlot("virtual_assistant.security",
	                                                                       ItemComponent.getItemStackNoCache(EnumComponentType.DIAMOND_CRYSTAL, 1),
	                                                                       1);
	
	// persistent properties
	private String lastCommand = "";
	
	// computed properties
	private AxisAlignedBB aabbArea = null;
	private int tickUpdate;
	
	public TileEntityVirtualAssistant() {
		super();
		
		peripheralName = "warpdriveVirtualAssistant";
		addMethods(new String[] {
			"getLastCommand",
			"pullLastCommand"
		});
		
		registerUpgradeSlot(upgradeSlotSecurity);
	}
	
	@Override
	public boolean energy_canInput(final EnumFacing from) {
		return true;
	}
	
	@Override
	protected void onConstructed() {
		super.onConstructed();
		
		energy_setParameters(WarpDriveConfig.VIRTUAL_ASSISTANT_MAX_ENERGY_STORED_BY_TIER[enumTier.getIndex()],
		                     512, 0,
		                     "LV", 2, "LV", 0);
	}
	
	@Override
	protected void onFirstUpdateTick() {
		super.onFirstUpdateTick();
		
		final float range = WarpDriveConfig.VIRTUAL_ASSISTANT_RANGE_BLOCKS_BY_TIER[enumTier.getIndex()];
		aabbArea = new AxisAlignedBB(
				pos.getX() - range, pos.getY() - range, pos.getZ() - range,
				pos.getX() + range + 1.0D, pos.getY() + range + 1.0D, pos.getZ() + range + 1.0D );
	}
	
	@Override
	public void update() {
		super.update();
		
		if (world.isRemote) {
			return;
		}
		
		tickUpdate--;
		if (tickUpdate < 0) {
			tickUpdate = WarpDriveConfig.G_PARAMETERS_UPDATE_INTERVAL_TICKS;
			
			final IBlockState blockState = world.getBlockState(pos);
			updateBlockState(blockState, BlockProperties.ACTIVE, isEnabled);
		}
	}
	
	@Override
	protected void doUpdateParameters(final boolean isDirty) {
		// no operation
	}
	
	@Override
	public void readFromNBT(@Nonnull final NBTTagCompound tagCompound) {
		super.readFromNBT(tagCompound);
		
		lastCommand = tagCompound.getString("lastCommand");
	}
	
	@Nonnull
	@Override
	public NBTTagCompound writeToNBT(@Nonnull NBTTagCompound tagCompound) {
		tagCompound = super.writeToNBT(tagCompound);
		
		tagCompound.setString("lastCommand", lastCommand);
		return tagCompound;
	}
	
	@Override
	public Object[] getEnergyRequired() {
		final String units = energy_getDisplayUnits();
		return new Object[] {
				true,
				EnergyWrapper.convert(WarpDriveConfig.VIRTUAL_ASSISTANT_ENERGY_PER_TICK_BY_TIER[enumTier.getIndex()], units) };
	}
	
	public boolean onChatReceived(@Nonnull final EntityPlayer entityPlayer, @Nonnull final String message) {
		if (!isEnabled) {
			return false;
		}
		if (!aabbArea.contains(entityPlayer.getPositionVector())) {
			return false;
		}
		if ( name.length() < 3
		  || name.contains("/")
		  || name.contains("!") ) {
			return false;
		}
		if (!message.toLowerCase().startsWith(name.toLowerCase())) {
			return false;
		}
		
		if (getUpgradeCount(upgradeSlotSecurity) > 0) {// check security constrains
			final ArrayList<GlobalRegion> globalRegions = GlobalRegionManager.getContainers(EnumGlobalRegionType.SHIP, world, pos);
			for (final GlobalRegion globalRegion : globalRegions) {
				// abort on invalid ship cores
				final TileEntity tileEntity = world.getTileEntity(globalRegion.getBlockPos());
				if (!(tileEntity instanceof TileEntityShipCore)) {
					if (Commons.throttleMe("onChatReceived-InvalidInstance")) {
						WarpDrive.logger.error(String.format("Unable to assist player due to invalid tile entity for global region, expecting TileEntityShipCore, got %s",
						                                     tileEntity));
					}
					return false;
				}
				final TileEntityShipCore tileEntityShipCore = (TileEntityShipCore) tileEntity;
				if (!tileEntityShipCore.isAssemblyValid()) {
					if (Commons.throttleMe("onChatReceived-InvalidAssembly")) {
						WarpDrive.logger.error(String.format("Unable to assist player due to invalid ship assembly for %s",
						                                     tileEntity ));
					}
					return false;
				}
				if (!tileEntityShipCore.isCrewMember(entityPlayer)) {
					return false;
				}
			}
		}
		
		final String command = message.substring(name.length()).trim();
		lastCommand = command;
		sendEvent("virtualAssistantCommand", command);
		return true;
	}
	
	// TileEntityAbstractBase overrides
	private WarpDriveText getCommandStatus() {
		if (lastCommand == null || lastCommand.isEmpty()) {
			return new WarpDriveText(Commons.getStyleWarning(), "warpdrive.virtual_assistant.status_line.none");
		}
		return new WarpDriveText(Commons.getStyleCorrect(), "warpdrive.virtual_assistant.status_line.last_command",
		                         lastCommand );
	}
	
	@Override
	public WarpDriveText getStatus() {
		final WarpDriveText textCommandStatus = getCommandStatus();
		if (textCommandStatus.getUnformattedText().isEmpty()) {
			return super.getStatus();
		} else {
			return super.getStatus()
			            .append(textCommandStatus);
		}
	}
	
	// IGlobalRegionProvider overrides
	@Override
	public EnumGlobalRegionType getGlobalRegionType() {
		return EnumGlobalRegionType.VIRTUAL_ASSISTANT;
	}
	
	@Override
	public AxisAlignedBB getGlobalRegionArea() {
		return aabbArea;
	}
	
	@Override
	public int getMass() {
		return 0;
	}
	
	@Override
	public double getIsolationRate() {
		return 0.0D;
	}
	
	@Override
	public boolean onBlockUpdatingInArea(@Nullable final Entity entity, final BlockPos blockPos, final IBlockState blockState) {
		return true;
	}
	
	// Common OC/CC methods
	public Object[] getLastCommand() {
		if (lastCommand == null || lastCommand.isEmpty()) {
			return new Object[] { false, "No command received." };
		}
		return new Object[] { true, lastCommand };
	}
	
	public Object[] pullLastCommand() {
		if (lastCommand == null || lastCommand.isEmpty()) {
			return new Object[] { false, "No command received." };
		}
		final String lastCommandSaved = lastCommand;
		lastCommand = "";
		return new Object[] { true, lastCommandSaved };
	}
	
	// OpenComputers callback methods
	@Callback(direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] getLastCommand(final Context context, final Arguments arguments) {
		OC_convertArgumentsAndLogCall(context, arguments);
		return getLastCommand();
	}
	
	@Callback(direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] pullLastCommand(final Context context, final Arguments arguments) {
		OC_convertArgumentsAndLogCall(context, arguments);
		return pullLastCommand();
	}
	
	// ComputerCraft IPeripheral methods
	@Override
	@Optional.Method(modid = "computercraft")
	protected Object[] CC_callMethod(@Nonnull final String methodName, @Nonnull final Object[] arguments) {
		switch (methodName) {
		case "getLastCommand":
			return getLastCommand();
		case "pullLastCommand":
			return pullLastCommand();
		}
		
		return super.CC_callMethod(methodName, arguments);
	}
}