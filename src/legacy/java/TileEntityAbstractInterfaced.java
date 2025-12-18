package cr0s.warpdrive.block;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.FunctionGet;
import cr0s.warpdrive.api.FunctionSetVector;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.EnumComponentType;
import cr0s.warpdrive.data.Vector3;
import cr0s.warpdrive.data.VectorI;
import cr0s.warpdrive.item.ItemComponent;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.filesystem.IMount;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import li.cil.oc.api.FileSystem;
import li.cil.oc.api.Network;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import net.minecraft.nbt.NBTTagCompound;

import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Optional;

// OpenComputers API: https://github.com/MightyPirates/OpenComputers/tree/master-MC1.7.10/src/main/java/li/cil/oc/api

@Optional.InterfaceList({
	@Optional.Interface(iface = "li.cil.oc.api.network.Environment", modid = "opencomputers"),
	@Optional.Interface(iface = "dan200.computercraft.api.peripheral.IPeripheral", modid = "computercraft")
})
public abstract class TileEntityAbstractInterfaced extends TileEntityAbstractBase implements IPeripheral, Environment, cr0s.warpdrive.api.computer.IInterfaced {
	
	// global storage
	private static final ConcurrentHashMap<String, Object> CC_mountGlobals = new ConcurrentHashMap<>(32);
	private static final UpgradeSlot upgradeSlotComputerInterface = new UpgradeSlot("base.computer_interface",
	                                                                                ItemComponent.getItemStackNoCache(EnumComponentType.COMPUTER_INTERFACE, 1),
	                                                                                1);
	
	// Common computer properties
	protected String peripheralName = null;
	private String[] methodsArray = {};
	private boolean isAlwaysInterfaced = true;
	
	// String returned to LUA script in case of error
	public static final String COMPUTER_ERROR_TAG = "!ERROR!";
	
	// Cached resource presence to reduce disk access on chunk loading
	private static final HashMap<String, Boolean> hashAssetExist = new HashMap<>(128);
	
	// pre-loaded scripts support
	private volatile ManagedEnvironment OC_fileSystem = null;
	private volatile boolean CC_hasResource = false;
	private volatile boolean OC_hasResource = false;
	protected volatile List<String> CC_scripts = null;
	
	// OpenComputer specific properties
	private Node     OC_node = null;
	private boolean  OC_addedToNetwork = false;
	
	// ComputerCraft specific properties
	private final ConcurrentHashMap<IComputerAccess, CopyOnWriteArraySet<String>> CC_connectedComputers = new ConcurrentHashMap<>();
	
	public TileEntityAbstractInterfaced() {
		super();
		
		addMethods(new String[] {
				"isInterfaced",
				"getLocalPosition",
				"getTier",
				"getUpgrades",
				"getVersion",
		});
	}
	
	// WarpDrive abstraction layer
	protected void doRequireUpgradeToInterface() {
		assert isAlwaysInterfaced;
		
		isAlwaysInterfaced = false;
		registerUpgradeSlot(upgradeSlotComputerInterface);
	}
	
	@Override
	protected void onUpgradeChanged(@Nonnull final UpgradeSlot upgradeSlot, final int countNew, final boolean isAdded) {
		if (upgradeSlot.equals(upgradeSlotComputerInterface)) {
			if (isAdded) {
				if (WarpDriveConfig.isComputerCraftLoaded) {
					CC_mount();
				}
				if (WarpDriveConfig.isOpenComputersLoaded) {
					OC_constructor();
				}
			} else {
				if (WarpDriveConfig.isComputerCraftLoaded) {
					CC_unmount();
				}
				if (WarpDriveConfig.isOpenComputersLoaded) {
					OC_destructor();
				}
			}
		}
		super.onUpgradeChanged(upgradeSlot, countNew, isAdded);
	}
	
	@Override
	public boolean isInterfaceEnabled() {
		return isAlwaysInterfaced || getUpgradeCount(upgradeSlotComputerInterface) > 0;
	}
	
	protected void addMethods(final String[] methodsToAdd) {
		if (methodsArray == null) {
			methodsArray = methodsToAdd;
		} else {
			int currentLength = methodsArray.length;
			methodsArray = Arrays.copyOf(methodsArray, methodsArray.length + methodsToAdd.length);
			for (final String method : methodsToAdd) {
				methodsArray[currentLength] = method;
				currentLength++;
			}
		}
	}
	
	private boolean assetExist(final String resourcePath) {
		Boolean exist = hashAssetExist.get(resourcePath);
		if (exist == null) {
			final URL url = getClass().getResource(resourcePath);
			exist = (url != null);
			hashAssetExist.put(resourcePath, exist);
		}
		return exist;
	}
	
	// TileEntity overrides
	@Override
 	public void update() {
		super.update();
		
		if (WarpDriveConfig.isOpenComputersLoaded) {
			if (!OC_addedToNetwork && isInterfaceEnabled()) {
				OC_addedToNetwork = true;
				Network.joinOrCreateNetwork(this);
			}
		}
	}
	
	@Override
	public void validate() {
		if (WarpDriveConfig.isComputerCraftLoaded) {
			final String CC_path = "/assets/" + WarpDrive.MODID.toLowerCase() + "/lua.ComputerCraft/" + peripheralName;
			CC_hasResource = assetExist(CC_path);
		}
		
		// deferred constructor so the derived class can finish it's initialization first
		if (WarpDriveConfig.isOpenComputersLoaded && OC_node == null && isInterfaceEnabled()) {
			OC_constructor();
		}
		super.validate();
	}
	
	@Override
	public void invalidate() {
		if (WarpDriveConfig.isOpenComputersLoaded) {
			OC_destructor();
		}
		super.invalidate();
	}
	
	@Override
	public void onChunkUnload() {
		if (WarpDriveConfig.isOpenComputersLoaded) {
			OC_destructor();
		}
		super.onChunkUnload();
	}
	
	@Override
	public void readFromNBT(@Nonnull final NBTTagCompound tagCompound) {
		super.readFromNBT(tagCompound);
		if ( WarpDriveConfig.isOpenComputersLoaded
		  && FMLCommonHandler.instance().getEffectiveSide().isServer()
		  && isInterfaceEnabled() ) {
			if (OC_node == null) {
				OC_constructor();
			}
			if (OC_node != null && OC_node.host() == this) {
				OC_node.load(tagCompound.getCompoundTag("oc:node"));
			} else if (tagCompound.hasKey("oc:node")) {
				WarpDrive.logger.error(String.format("%s OC node failed to construct or wrong host, ignoring NBT node data read...",
				                                     this));
			}
			if (OC_fileSystem != null && OC_fileSystem.node() != null) {
				OC_fileSystem.node().load(tagCompound.getCompoundTag("oc:fs"));
			} else if (OC_hasResource) {
				WarpDrive.logger.error(String.format("%s OC filesystem failed to construct or wrong node, ignoring NBT filesystem data read...",
				                                     this));
			}
		}
	}
	
	@Nonnull
	@Override
	public NBTTagCompound writeToNBT(@Nonnull NBTTagCompound tagCompound) {
		tagCompound = super.writeToNBT(tagCompound);
		if (WarpDriveConfig.isOpenComputersLoaded) {
			if (OC_node != null && OC_node.host() == this) {
				final NBTTagCompound nbtNode = new NBTTagCompound();
				OC_node.save(nbtNode);
				tagCompound.setTag("oc:node", nbtNode);
			}
			if (OC_fileSystem != null && OC_fileSystem.node() != null) {
				final NBTTagCompound nbtFileSystem = new NBTTagCompound();
				OC_fileSystem.node().save(nbtFileSystem);
				tagCompound.setTag("oc:fs", nbtFileSystem);
			}
		}
		return tagCompound;
	}
	
	@Override
	public NBTTagCompound writeItemDropNBT(NBTTagCompound tagCompound) {
		tagCompound = super.writeItemDropNBT(tagCompound);
		tagCompound.removeTag("oc:node");
		tagCompound.removeTag("oc:fs");
		return tagCompound;
	}
	
	@Override
	public int hashCode() {
		return (((((super.hashCode() + (world == null ? 0 : world.provider.getDimension()) << 4) + pos.getX()) << 4) + pos.getY()) << 4) + pos.getZ();
	}
	
	// Interface proxies are used to
	// - convert arguments,
	// - log LUA calls,
	// - block connection when missing the Computer interface upgrade
	// note: direct API calls remains possible without upgrade, as it's lore dependant
	@Optional.Method(modid = "opencomputers")
	protected Object[] OC_convertArgumentsAndLogCall(@Nonnull final Context context, @Nonnull final Arguments args) {
		final Object[] arguments = new Object[args.count()];
		int index = 0;
		for (final Object arg : args) {
			if (args.isString(index)) {
				arguments[index] = args.checkString(index);
			} else {
				arguments[index] = arg;
			}
			index++;
		}
		if (WarpDriveConfig.LOGGING_LUA) {
			final String methodName = Commons.getMethodName(1);
			WarpDrive.logger.info(String.format("[OC] LUA call %s from %s to %s.%s(%s)",
			                                    Commons.format(world, pos),
			                                    context.node().address(),
			                                    peripheralName, methodName, Commons.format(arguments)));
		}
		if (!isInterfaceEnabled()) {
			throw new RuntimeException("Missing Computer interface upgrade.");
		}
		return arguments;
	}
	
	@Optional.Method(modid = "computercraft")
	private String CC_getMethodNameAndLogCall(@Nonnull final IComputerAccess computerAccess, final int methodIndex, @Nonnull final Object[] arguments) {
		final String methodName = methodsArray[methodIndex];
		if (WarpDriveConfig.LOGGING_LUA) {
			WarpDrive.logger.info(String.format("[CC] LUA call %s from %d:%s to %s.%s(%s)",
			                                    Commons.format(world, pos),
			                                    computerAccess.getID(), computerAccess.getAttachmentName(),
			                                    peripheralName, methodName, Commons.format(arguments)));
		}
		if ( !isInterfaceEnabled()
		  && !"isInterfaced".equals(methodName) ) {
			throw new RuntimeException("Missing Computer interface upgrade.");
		}
		return methodName;
	}
	
	// Common OC/CC methods
	@Override
	public Object[] isInterfaced() {
		if (isInterfaceEnabled()) {
			return new Object[] { true, "I'm a WarpDrive computer interfaced tile entity." };
		} else {
			return new Object[] { false, "Missing Computer interface upgrade." };
		}
	}
	
	@Override
	public Object[] getLocalPosition() {
		return new Object[] { pos.getX(), pos.getY(), pos.getZ() };
	}
	
	@Override
	public Object[] getTier() {
		return new Object[] { enumTier.getIndex(), enumTier.getName() };
	}
	
	@Override
	public Object[] getUpgrades() {
		return new Object[] { isUpgradeable(), Commons.removeFormatting( getUpgradeStatus(false).getUnformattedText() ) };
	}
	
	@Override
	public Integer[] getVersion() {
		return WarpDrive.VERSION_NUMBERS;
	}
	
	// Common WarpDrive API
	public boolean computer_isConnected() {
		if ( WarpDriveConfig.isComputerCraftLoaded
		  && !CC_connectedComputers.isEmpty() ) {
			return true;
		}
		if ( WarpDriveConfig.isOpenComputersLoaded
		  && OC_node != null ) {
			final Iterable<Node> iterableNodes = OC_node.reachableNodes();
			return iterableNodes.iterator().hasNext();
		}
		return false;
	}
	
	protected VectorI computer_getVectorI(final VectorI vDefault, @Nonnull final Object[] arguments) {
		try {
			if (arguments.length == 3) {
				final int x = Commons.toInt(arguments[0]);
				final int y = Commons.toInt(arguments[1]);
				final int z = Commons.toInt(arguments[2]);
				return new VectorI(x, y, z);
			}
		} catch (final NumberFormatException exception) {
			// ignore
		}
		return vDefault;
	}
	
	@Nonnull
	protected Object[] computer_getOrSetVector3(@Nonnull final FunctionGet<Vector3> getVector,
	                                            @Nonnull final FunctionSetVector<Float> setVector,
	                                            @Nonnull final TileEntityAbstractBase.UpgradeSlot upgradeSlot,
	                                            final Object[] arguments) {
		if ( arguments != null
		  && arguments.length > 0
		  && arguments[0] != null ) {
			try {
				if (arguments.length == 1) {
					final float value = Commons.toFloat(arguments[0]);
					setVector.apply(value, value, value);
				} else if (arguments.length == 2) {
					final float valueXZ = Commons.toFloat(arguments[0]);
					final float valueY = Commons.toFloat(arguments[1]);
					setVector.apply(valueXZ, valueY, valueXZ);
				} else if (arguments.length == 3) {
					final float valueX = Commons.toFloat(arguments[0]);
					final float valueY = Commons.toFloat(arguments[1]);
					final float valueZ = Commons.toFloat(arguments[2]);
					setVector.apply(valueX, valueY, valueZ);
				}
			} catch (final Exception exception) {
				final String message = String.format("Float expected for all arguments %s",
				                                     Arrays.toString(arguments));
				if (WarpDriveConfig.LOGGING_LUA) {
					WarpDrive.logger.error(String.format("%s LUA error on %s: %s",
					                                     this, setVector, message));
				}
				final Vector3 v3Actual = getVector.apply();
				return new Object[] { v3Actual.x, v3Actual.y, v3Actual.z, message };
			}
		}
		final Vector3 v3Actual = getVector.apply();
		if (hasUpgrade(upgradeSlot)) {
			return new Double[] { v3Actual.x, v3Actual.y, v3Actual.z };
		} else {
			return new Object[] { v3Actual.x, v3Actual.y, v3Actual.z, "Missing " + upgradeSlot.itemStack.getDisplayName() };
		}
	}
	
	protected UUID computer_getUUID(final UUID uuidDefault, @Nonnull final Object[] arguments) {
		try {
			if (arguments.length == 1 && arguments[0] != null) {
				if (arguments[0] instanceof UUID) {
					return (UUID) arguments[0];
				}
				if (arguments[0] instanceof String) {
					return UUID.fromString((String) arguments[0]);
				}
			}
		} catch (final IllegalArgumentException exception) {
			// ignore
		}
		return uuidDefault;
	}
	
	// ComputerCraft IPeripheral methods
	@Nonnull
	@Override
	@Optional.Method(modid = "computercraft")
	public String getType() {
		assert peripheralName != null;
		return peripheralName;
	}
	
	@Nonnull
	@Override
	@Optional.Method(modid = "computercraft")
	public String[] getMethodNames() {
		return methodsArray;
	}
	
	@Override
	@Optional.Method(modid = "computercraft")
	final public Object[] callMethod(@Nonnull final IComputerAccess computerAccess, @Nonnull final ILuaContext context,
	                                 final int method, @Nonnull final Object[] arguments) throws LuaException {
		final String methodName = CC_getMethodNameAndLogCall(computerAccess, method, arguments);
		
		// we separate the proxy from the logs so children can override the proxy without having to handle the logs themselves
		try {
			final Object[] result = CC_callMethod(methodName, arguments);
			if (WarpDriveConfig.LOGGING_LUA) {
				WarpDrive.logger.info(String.format("[CC] LUA call is returning %s",
				                                    Commons.format(result)));
			}
			return result;
		} catch (final Exception exception) {
			if ( WarpDriveConfig.LOGGING_LUA
			  || Commons.throttleMe("LUA exception") ) {
				exception.printStackTrace(WarpDrive.printStreamError);
			}
			
			throw new LuaException(String.format("Internal exception %s from %d:%s to %s.%s(%s)\nCheck server logs for details.",
			                                     Commons.format(world, pos),
			                                     computerAccess.getID(), computerAccess.getAttachmentName(),
			                                     peripheralName, methodName, Commons.format(arguments) ));
		}
	}
	
	@Optional.Method(modid = "computercraft")
	protected Object[] CC_callMethod(@Nonnull final String methodName, @Nonnull final Object[] arguments) {
		switch (methodName) {
		case "isInterfaced":
			return isInterfaced();
			
		case "getLocalPosition":
			return getLocalPosition();
			
		case "getTier":
			return getTier();
			
		case "getUpgrades":
			return getUpgrades();
			
		case "getVersion":
			return getVersion();
		}
		
		return null;
	}
	
	@Override
	@Optional.Method(modid = "computercraft")
	public void attach(@Nonnull final IComputerAccess computerAccess) {
		if (CC_connectedComputers.containsKey(computerAccess)) {
			WarpDrive.logger.error(String.format("[CC] Already attached %s %s with %d:%s, ignoring...",
			                                     peripheralName, Commons.format(world, pos),
			                                     computerAccess.getID(), computerAccess.getAttachmentName() ));
			return;
		}
		if (WarpDriveConfig.LOGGING_LUA) {
			WarpDrive.logger.info(String.format("[CC] Attaching %s %s with %d:%s",
			                                    peripheralName, Commons.format(world, pos),
			                                    computerAccess.getID(), computerAccess.getAttachmentName() ));
		}
		final CopyOnWriteArraySet<String> mountedLocations = new CopyOnWriteArraySet<>();
		CC_connectedComputers.put(computerAccess, mountedLocations);
		if (isInterfaceEnabled()) {
			CC_mount(computerAccess, mountedLocations);
		}
	}
	
	@Optional.Method(modid = "computercraft")
	private void CC_mount() {
		for (final Entry<IComputerAccess, CopyOnWriteArraySet<String>> entry : CC_connectedComputers.entrySet()) {
			CC_mount(entry.getKey(), entry.getValue());
		}
	}
	
	@Optional.Method(modid = "computercraft")
	private void CC_mount(@Nonnull final IComputerAccess computerAccess, @Nonnull final CopyOnWriteArraySet<String> mountedLocations) {
		if (CC_hasResource && WarpDriveConfig.G_LUA_SCRIPTS != WarpDriveConfig.LUA_SCRIPTS_NONE) {
			try {
				CC_mount(computerAccess, mountedLocations, "lua.ComputerCraft/common", "/" + WarpDrive.MODID);
				
				final String folderPeripheral = peripheralName.replace(WarpDrive.MODID, WarpDrive.MODID + "/");
				CC_mount(computerAccess, mountedLocations, "lua.ComputerCraft/" + peripheralName, "/" + folderPeripheral);
				
				if (WarpDriveConfig.G_LUA_SCRIPTS == WarpDriveConfig.LUA_SCRIPTS_ALL) {
					for (final String script : CC_scripts) {
						CC_mount(computerAccess, mountedLocations, "lua.ComputerCraft/" + peripheralName + "/" + script, "/" + script);
					}
				}
			} catch (final Exception exception) {
				exception.printStackTrace(WarpDrive.printStreamError);
				WarpDrive.logger.error(String.format("Failed to mount ComputerCraft scripts for %s %s, isFirstTick %s",
				                                     peripheralName,
				                                     Commons.format(world, pos),
				                                     isFirstTick()));
			}
		}
	}
	
	@Optional.Method(modid = "computercraft")
	private void CC_mount(@Nonnull final IComputerAccess computerAccess, @Nonnull final CopyOnWriteArraySet<String> mountedLocations,
	                      @Nonnull final String pathAsset, @Nonnull final String pathLUA) {
		IMount mountCommon = (IMount) CC_mountGlobals.get(pathAsset);
		if (mountCommon == null) {
			mountCommon = ComputerCraftAPI.createResourceMount(WarpDrive.class, WarpDrive.MODID, pathAsset);
			assert mountCommon != null;
			CC_mountGlobals.put(pathAsset, mountCommon);
		}
		final String pathLUA_actual = computerAccess.mount(pathLUA, mountCommon);
		if (WarpDriveConfig.LOGGING_LUA) {
			WarpDrive.logger.info(String.format("[CC] %s Mounted %s to %s as %s",
			                                    Commons.format(world, pos),
			                                    pathAsset, pathLUA, pathLUA_actual));
		}
		if (pathLUA_actual != null) {
			mountedLocations.add(pathLUA_actual);
		}
	}
	
	@Optional.Method(modid = "computercraft")
	private void CC_unmount() {
		for (final Entry<IComputerAccess, CopyOnWriteArraySet<String>> entry : CC_connectedComputers.entrySet()) {
			CC_unmount(entry.getKey(), entry.getValue());
		}
	}
	
	@Optional.Method(modid = "computercraft")
	private void CC_unmount(@Nonnull final IComputerAccess computerAccess) {
		final CopyOnWriteArraySet<String> mountedLocations = CC_connectedComputers.get(computerAccess);
		if (mountedLocations != null) {
			CC_unmount(computerAccess, mountedLocations);
		}
	}
	
	@Optional.Method(modid = "computercraft")
	private void CC_unmount(@Nonnull final IComputerAccess computerAccess, @Nonnull final CopyOnWriteArraySet<String> mountedLocation) {
		if (CC_hasResource && WarpDriveConfig.G_LUA_SCRIPTS != WarpDriveConfig.LUA_SCRIPTS_NONE) {
			try {
				for (final String pathLUA : mountedLocation) {
					if (WarpDriveConfig.LOGGING_LUA) {
						WarpDrive.logger.info(String.format("[CC] %s Unmounting %s",
						                                    Commons.format(world, pos),
						                                    pathLUA ));
					}
					computerAccess.unmount(pathLUA);
				}
			} catch (final Exception exception) {
				exception.printStackTrace(WarpDrive.printStreamError);
				WarpDrive.logger.error(String.format("Failed to unmount ComputerCraft scripts for %s %s, isFirstTick %s",
				                                     peripheralName,
				                                     Commons.format(world, pos),
				                                     isFirstTick()));
			} finally {
				mountedLocation.clear();
			}
		}
	}
	
	@Override
	@Optional.Method(modid = "computercraft")
	public void detach(@Nonnull final IComputerAccess computerAccess) {
		if (!CC_connectedComputers.containsKey(computerAccess)) {
			WarpDrive.logger.error(String.format("[CC] Already detached %s %s from %d:%s, ignoring...",
			                                     peripheralName, Commons.format(world, pos),
			                                     computerAccess.getID(), computerAccess.getAttachmentName() ));
			return;
		}
		if (WarpDriveConfig.LOGGING_LUA) {
			WarpDrive.logger.info(String.format("[CC] Detaching %s %s from %d:%s",
			                                    peripheralName, Commons.format(world, pos),
			                                    computerAccess.getID(), computerAccess.getAttachmentName() ));
		}
		if (isInterfaceEnabled()) {
			CC_unmount(computerAccess);
		}
		CC_connectedComputers.remove(computerAccess);
	}
	
	@Override
	@Optional.Method(modid = "computercraft")
	public boolean equals(@Nullable final IPeripheral other) {
		return other != null
		    && other.hashCode() == hashCode();
	}
	
	// Computer abstraction methods
	protected void sendEvent(final String eventName, final Object... arguments) {
		if (!isInterfaceEnabled()) {
			return;
		}
		if (WarpDriveConfig.LOGGING_LUA) {
			WarpDrive.logger.info(this + " Sending event '" + eventName + "'");
		}
		if (WarpDriveConfig.isComputerCraftLoaded) {
			for (final IComputerAccess computerAccess : CC_connectedComputers.keySet()) {
				computerAccess.queueEvent(eventName, arguments);
			}
		}
		if ( WarpDriveConfig.isOpenComputersLoaded
		  && OC_node != null
		  && OC_node.network() != null ) {
			if (arguments == null || arguments.length == 0) {
				OC_node.sendToReachable("computer.signal", eventName);
			} else {
				final Object[] eventWithArguments = new Object[arguments.length + 1];
				eventWithArguments[0] = eventName;
				int index = 1;
				for (final Object object : arguments) {
					eventWithArguments[index] = object;
					index++;
				}
				OC_node.sendToReachable("computer.signal", eventWithArguments);
			}
		}
	}
	
	// OpenComputers methods
	@Callback(direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] isInterfaced(final Context context, final Arguments arguments) {
		OC_convertArgumentsAndLogCall(context, arguments);
		return isInterfaced();
	}
	
	@Callback(direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] getLocalPosition(final Context context, final Arguments arguments) {
		OC_convertArgumentsAndLogCall(context, arguments);
		return getLocalPosition();
	}
	
	@Callback(direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] getUpgrades(final Context context, final Arguments arguments) {
		OC_convertArgumentsAndLogCall(context, arguments);
		return getUpgrades();
	}
	
	@Callback(direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] getTier(final Context context, final Arguments arguments) {
		OC_convertArgumentsAndLogCall(context, arguments);
		return getTier();
	}
	
	@Callback(direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] getVersion(final Context context, final Arguments arguments) {
		OC_convertArgumentsAndLogCall(context, arguments);
		return getVersion();
	}
	
	@Optional.Method(modid = "opencomputers")
	private void OC_constructor() {
		assert OC_node == null;
		final String OC_path = "/assets/" + WarpDrive.MODID.toLowerCase() + "/lua.OpenComputers/" + peripheralName;
		OC_hasResource = assetExist(OC_path);
		OC_node = Network.newNode(this, Visibility.Network).withComponent(peripheralName).create();
		if (OC_node != null && OC_hasResource && WarpDriveConfig.G_LUA_SCRIPTS != WarpDriveConfig.LUA_SCRIPTS_NONE) {
			OC_fileSystem = FileSystem.asManagedEnvironment(FileSystem.fromClass(getClass(), WarpDrive.MODID.toLowerCase(), "lua.OpenComputers/" + peripheralName), peripheralName);
			((Component) OC_fileSystem.node()).setVisibility(Visibility.Network);
		}
		// note: we can't join the network right away, it's postponed to next tick
	}
	
	@Optional.Method(modid = "opencomputers")
	private void OC_destructor() {
		if (OC_node != null) {
			if (OC_fileSystem != null) {
				OC_fileSystem.node().remove();
				OC_fileSystem = null;
			}
			OC_node.remove();
			OC_node = null;
			OC_addedToNetwork = false;
		}
	}
	
	@Override
	@Optional.Method(modid = "opencomputers")
	public Node node() {
		return OC_node;
	}
	
	@Override
	@Optional.Method(modid = "opencomputers")
	public void onConnect(@Nonnull final Node node) {
		if (node.host() instanceof Context) {
			// Attach our file system to new computers we get connected to.
			// Note that this is also called for all already present computers
			// when we're added to an already existing network, so we don't
			// have to loop through the existing nodes manually.
			if (OC_fileSystem != null) {
				node.connect(OC_fileSystem.node());
			}
		}
	}
	
	@Override
	@Optional.Method(modid = "opencomputers")
	public void onDisconnect(@Nonnull final Node node) {
		if (OC_fileSystem != null) {
			if (node.host() instanceof Context) {
				// Disconnecting from a single computer
				node.disconnect(OC_fileSystem.node());
			} else if (node.equals(OC_node)) {
				// Disconnecting from the network
				OC_fileSystem.node().remove();
			}
		}
	}
	
	@Override
	@Optional.Method(modid = "opencomputers")
	public void onMessage(@Nonnull final Message message) {
		// nothing special
	}
}
