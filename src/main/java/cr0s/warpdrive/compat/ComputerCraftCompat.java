package cr0s.warpdrive.compat;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.block.AbstractEnergyTileEntity;
import cr0s.warpdrive.block.ShipControllerTileEntity;
import cr0s.warpdrive.block.ShipCoreTileEntity;
import cr0s.warpdrive.block.atomic.AcceleratorControlPointTileEntity;
import cr0s.warpdrive.block.atomic.AcceleratorCoreTileEntity;
import cr0s.warpdrive.block.atomic.ParticlesInjectorTileEntity;
import cr0s.warpdrive.block.breathing.AirGeneratorTileEntity;
import cr0s.warpdrive.block.building.ShipScannerTileEntity;
import cr0s.warpdrive.block.collection.MiningLaserTileEntity;
import cr0s.warpdrive.block.collection.LaserTreeFarmTileEntity;
import cr0s.warpdrive.block.detection.SirenTileEntity;
import cr0s.warpdrive.block.energy.CapacitorTileEntity;
import cr0s.warpdrive.block.energy.LaserMediumTileEntity;
import cr0s.warpdrive.block.forcefield.ForceFieldProjectorTileEntity;
import cr0s.warpdrive.block.forcefield.ForceFieldRelayTileEntity;
import cr0s.warpdrive.block.forcefield.ForceFieldUpgrade;
import cr0s.warpdrive.block.movement.LiftTileEntity;
import cr0s.warpdrive.block.movement.ChunkLoaderTileEntity;
import cr0s.warpdrive.block.movement.TransporterCoreTileEntity;
import cr0s.warpdrive.block.movement.TransporterBeaconTileEntity;
import cr0s.warpdrive.block.detection.SpeakerTileEntity;
import cr0s.warpdrive.block.detection.CameraTileEntity;
import cr0s.warpdrive.block.detection.MonitorTileEntity;
import cr0s.warpdrive.block.detection.EnvironmentalSensorTileEntity;
import cr0s.warpdrive.block.detection.BiometricScannerTileEntity;
import cr0s.warpdrive.block.detection.SecurityStationTileEntity;
import cr0s.warpdrive.block.detection.VirtualAssistantTileEntity;
import cr0s.warpdrive.block.detection.RadarTileEntity;
import cr0s.warpdrive.block.detection.CloakingCoreTileEntity;
import cr0s.warpdrive.block.energy.EnanReactorCoreTileEntity;
import cr0s.warpdrive.block.energy.EnanReactorLaserTileEntity;
import cr0s.warpdrive.block.weapon.LaserCameraTileEntity;
import cr0s.warpdrive.block.weapon.LaserTileEntity;
import cr0s.warpdrive.block.weapon.WeaponControllerTileEntity;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.filesystem.IMount;
import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IDynamicPeripheral;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.Capabilities;
import dan200.computercraft.shared.peripheral.generic.GenericPeripheralProvider;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Isolates all executable CC:Tweaked references from the Ship Core class.
 *
 * This class is registered only when CC:Tweaked is loaded, allowing the base mod and its native
 * controller to run without the optional dependency while retaining the existing Lua API.
 */
public final class ComputerCraftCompat {

	private static final ResourceLocation CAPABILITY_ID =
		new ResourceLocation(WarpDrive.MODID, "ship_core_peripheral");
	private static final String LUA_COMMON = "lua.ComputerCraft/common";
	private static final String LUA_WEAPON_CONTROLLER =
		"lua.ComputerCraft/warpdriveWeaponController";
	private static final String LUA_RADAR = "lua.ComputerCraft/warpdriveRadar";
	private static final String LUA_CLOAKING_CORE = "lua.ComputerCraft/warpdriveCloakingCore";
	private static final String LUA_ACCELERATOR = "lua.ComputerCraft/warpdriveAccelerator";
	private static final String LUA_TRANSPORTER = "lua.ComputerCraft/warpdriveTransporterCore";
	private static final String LUA_ENAN_REACTOR =
		"lua.ComputerCraft/warpdriveEnanReactorCore";

	private ComputerCraftCompat() {
	}

	/**
	 * Methods inherited by every 1.12.2 TileEntityAbstractInterfaced machine. CC:Tweaked 1.101
	 * discovers annotated public default-interface methods through Class.getMethods(), so the
	 * common contract can stay central without replacing each machine's lifecycle adapter.
	 */
	public interface CommonPeripheral extends IPeripheral {

		@LuaFunction
		default Object[] isInterfaced() throws LuaException {
			return new Object[]{ true, "CC:Tweaked peripheral available." };
		}

		@LuaFunction
		default Object[] getLocalPosition() throws LuaException {
			final TileEntity tileEntity = tileEntity(this);
			return new Object[]{ tileEntity.getBlockPos().getX(), tileEntity.getBlockPos().getY(),
				tileEntity.getBlockPos().getZ() };
		}

		@LuaFunction(mainThread = true)
		default Object[] getTier() throws LuaException {
			return describeTier(tileEntity(this));
		}

		@LuaFunction(mainThread = true)
		default Object[] getUpgrades() throws LuaException {
			return describeUpgrades(tileEntity(this));
		}

		@LuaFunction
		default Object[] getVersion() throws LuaException {
			return WarpDrive.getVersionNumbers();
		}
	}

	private static TileEntity tileEntity(final IPeripheral peripheral) throws LuaException {
		final Object target = peripheral.getTarget();
		if (!(target instanceof TileEntity)) {
			throw new LuaException("WarpDrive peripheral target is unavailable");
		}
		return (TileEntity) target;
	}

	private static Object[] describeTier(final TileEntity tileEntity) {
		Object tier = invokeNoArgument(tileEntity, "getTier");
		if (tier == null) tier = invokeNoArgument(tileEntity.getBlockState().getBlock(), "getTier");
		String name = tier instanceof Enum
			? ((Enum<?>) tier).name().toLowerCase(java.util.Locale.ROOT) : null;
		final Object reportedName = tier == null ? null : invokeNoArgument(tier, "getName");
		if (reportedName != null) name = String.valueOf(reportedName);

		if (name == null || name.isEmpty()) {
			final ResourceLocation registryName = tileEntity.getBlockState().getBlock().getRegistryName();
			final String path = registryName == null ? "" : registryName.getPath();
			name = path.endsWith(".advanced") ? "advanced"
				: path.endsWith(".superior") ? "superior"
				: path.endsWith(".creative") ? "creative" : "basic";
		}

		final int legacyIndex;
		switch (name.toLowerCase(java.util.Locale.ROOT)) {
		case "creative": legacyIndex = 0; break;
		case "advanced": legacyIndex = 2; break;
		case "superior": legacyIndex = 3; break;
		case "basic":
		default: legacyIndex = 1; break;
		}
		return new Object[]{ legacyIndex, name };
	}

	private static Object[] describeUpgrades(final TileEntity tileEntity) {
		if (tileEntity instanceof CameraTileEntity) {
			return new Object[]{ true, String.format("recognition %d / 8",
				((CameraTileEntity) tileEntity).getRecognitionUpgrades()) };
		}
		if (tileEntity instanceof MiningLaserTileEntity) {
			return new Object[]{ true, String.format("pumping %d / 20",
				((MiningLaserTileEntity) tileEntity).getPumpCount()) };
		}
		if (tileEntity instanceof CloakingCoreTileEntity) {
			return new Object[]{ true, String.format("diamond crystals %d / 6",
				((CloakingCoreTileEntity) tileEntity).getDiamondCrystalCount()) };
		}
		if (tileEntity instanceof TransporterCoreTileEntity) {
			return new Object[]{ true,
				((TransporterCoreTileEntity) tileEntity).getUpgradeStatus() };
		}
		if (tileEntity instanceof ForceFieldRelayTileEntity) {
			final ForceFieldUpgrade upgrade =
				((ForceFieldRelayTileEntity) tileEntity).getUpgrade();
			return new Object[]{ true, upgrade == ForceFieldUpgrade.NONE
				? "No upgrade installed." : upgrade.getSerializedName() + "=1" };
		}
		if (tileEntity instanceof ForceFieldProjectorTileEntity) {
			final ForceFieldProjectorTileEntity projector =
				(ForceFieldProjectorTileEntity) tileEntity;
			final StringBuilder status = new StringBuilder();
			for (final ForceFieldUpgrade upgrade : ForceFieldUpgrade.values()) {
				final int count = projector.getUpgradeCount(upgrade);
				if (count <= 0) continue;
				if (status.length() > 0) status.append(", ");
				status.append(upgrade.getSerializedName()).append('=').append(count);
			}
			return new Object[]{ true,
				status.length() == 0 ? "No upgrades installed." : status.toString() };
		}
		return new Object[]{ false, "No installable upgrades." };
	}

	@Nullable
	private static Object invokeNoArgument(final Object target, final String methodName) {
		try {
			final Method method = target.getClass().getMethod(methodName);
			return method.invoke(target);
		} catch (final ReflectiveOperationException | RuntimeException exception) {
			return null;
		}
	}

	public static void register() {
		MinecraftForge.EVENT_BUS.addGenericListener(TileEntity.class,
			ComputerCraftCompat::attachCapabilities);
		WarpDrive.logger.info("CC:Tweaked machine peripheral compatibility enabled");
	}

	private static void attachCapabilities(final AttachCapabilitiesEvent<TileEntity> event) {
		final PeripheralProvider provider;
		if (event.getObject() instanceof ShipCoreTileEntity) {
			provider = new ShipCorePeripheralProvider((ShipCoreTileEntity) event.getObject());
		} else if (event.getObject() instanceof ShipControllerTileEntity) {
			final ShipControllerTileEntity controller = (ShipControllerTileEntity) event.getObject();
			final ShipControllerPeripheral peripheral = new ShipControllerPeripheral(controller);
			provider = new FixedPeripheralProvider(controller, peripheral, peripheral::invalidate);
		} else if (event.getObject() instanceof AirGeneratorTileEntity) {
			final AirGeneratorTileEntity generator = (AirGeneratorTileEntity) event.getObject();
			provider = new FixedPeripheralProvider(generator, new AirGeneratorPeripheral(generator));
		} else if (event.getObject() instanceof CapacitorTileEntity) {
			final CapacitorTileEntity capacitor = (CapacitorTileEntity) event.getObject();
			provider = new FixedPeripheralProvider(capacitor,
				new EnergyStoragePeripheral(capacitor, "warpdriveCapacitor"));
		} else if (event.getObject() instanceof LaserMediumTileEntity) {
			final LaserMediumTileEntity medium = (LaserMediumTileEntity) event.getObject();
			provider = new FixedPeripheralProvider(medium,
				new EnergyStoragePeripheral(medium, "warpdriveLaserMedium"));
		} else if (event.getObject() instanceof SirenTileEntity) {
			final SirenTileEntity siren = (SirenTileEntity) event.getObject();
			provider = new FixedPeripheralProvider(siren, new SirenPeripheral(siren));
		} else if (event.getObject() instanceof ShipScannerTileEntity) {
			final ShipScannerTileEntity scanner = (ShipScannerTileEntity) event.getObject();
			provider = new FixedPeripheralProvider(scanner, new ShipScannerPeripheral(scanner));
		} else if (event.getObject() instanceof CameraTileEntity) {
			final CameraTileEntity camera = (CameraTileEntity) event.getObject();
			final CameraPeripheral peripheral = new CameraPeripheral(camera);
			provider = new FixedPeripheralProvider(camera, peripheral, peripheral::invalidate);
		} else if (event.getObject() instanceof MonitorTileEntity) {
			final MonitorTileEntity monitor = (MonitorTileEntity) event.getObject();
			provider = new FixedPeripheralProvider(monitor, new MonitorPeripheral(monitor));
		} else if (event.getObject() instanceof LaserTileEntity) {
			provider = new LaserPeripheralProvider((LaserTileEntity) event.getObject());
		} else if (event.getObject() instanceof LiftTileEntity) {
			provider = new LiftPeripheralProvider((LiftTileEntity) event.getObject());
		} else if (event.getObject() instanceof ChunkLoaderTileEntity) {
			final ChunkLoaderTileEntity loader = (ChunkLoaderTileEntity) event.getObject();
			provider = new FixedPeripheralProvider(loader, new ChunkLoaderPeripheral(loader));
		} else if (event.getObject() instanceof SpeakerTileEntity) {
			provider = new SpeakerPeripheralProvider((SpeakerTileEntity) event.getObject());
		} else if (event.getObject() instanceof MiningLaserTileEntity) {
			final MiningLaserTileEntity miningLaser = (MiningLaserTileEntity) event.getObject();
			provider = new FixedPeripheralProvider(miningLaser, new MiningLaserPeripheral(miningLaser));
		} else if (event.getObject() instanceof LaserTreeFarmTileEntity) {
			final LaserTreeFarmTileEntity treeFarm = (LaserTreeFarmTileEntity) event.getObject();
			provider = new FixedPeripheralProvider(treeFarm, new LaserTreeFarmPeripheral(treeFarm));
		} else if (event.getObject() instanceof ForceFieldProjectorTileEntity) {
			final ForceFieldProjectorTileEntity projector =
				(ForceFieldProjectorTileEntity) event.getObject();
			provider = new FixedPeripheralProvider(projector, new ForceFieldProjectorPeripheral(projector));
		} else if (event.getObject() instanceof ForceFieldRelayTileEntity) {
			final ForceFieldRelayTileEntity relay = (ForceFieldRelayTileEntity) event.getObject();
			provider = new FixedPeripheralProvider(relay, new ForceFieldRelayPeripheral(relay));
		} else if (event.getObject() instanceof WeaponControllerTileEntity) {
			final WeaponControllerTileEntity controller =
				(WeaponControllerTileEntity) event.getObject();
			final WeaponControllerPeripheral peripheral = new WeaponControllerPeripheral(controller);
			provider = new FixedPeripheralProvider(controller, peripheral, peripheral::invalidate);
		} else if (event.getObject() instanceof EnvironmentalSensorTileEntity) {
			final EnvironmentalSensorTileEntity sensor =
				(EnvironmentalSensorTileEntity) event.getObject();
			provider = new FixedPeripheralProvider(sensor, new EnvironmentalSensorPeripheral(sensor));
		} else if (event.getObject() instanceof BiometricScannerTileEntity) {
			final BiometricScannerTileEntity scanner =
				(BiometricScannerTileEntity) event.getObject();
			final BiometricScannerPeripheral peripheral = new BiometricScannerPeripheral(scanner);
			provider = new FixedPeripheralProvider(scanner, peripheral, peripheral::invalidate);
		} else if (event.getObject() instanceof SecurityStationTileEntity) {
			final SecurityStationTileEntity station =
				(SecurityStationTileEntity) event.getObject();
			provider = new FixedPeripheralProvider(station, new SecurityStationPeripheral(station));
		} else if (event.getObject() instanceof AcceleratorCoreTileEntity) {
			final AcceleratorCoreTileEntity accelerator = (AcceleratorCoreTileEntity) event.getObject();
			final AcceleratorPeripheral peripheral = new AcceleratorPeripheral(accelerator);
			provider = new FixedPeripheralProvider(accelerator, peripheral, peripheral::invalidate);
		} else if (event.getObject() instanceof AcceleratorControlPointTileEntity) {
			final AcceleratorControlPointTileEntity controlPoint =
				(AcceleratorControlPointTileEntity) event.getObject();
			provider = new FixedPeripheralProvider(controlPoint,
				new AcceleratorControlPointPeripheral(controlPoint));
		} else if (event.getObject() instanceof TransporterCoreTileEntity) {
			final TransporterCoreTileEntity core = (TransporterCoreTileEntity) event.getObject();
			final TransporterCorePeripheral peripheral = new TransporterCorePeripheral(core);
			provider = new FixedPeripheralProvider(core, peripheral, peripheral::invalidate);
		} else if (event.getObject() instanceof TransporterBeaconTileEntity) {
			final TransporterBeaconTileEntity beacon =
				(TransporterBeaconTileEntity) event.getObject();
			provider = new FixedPeripheralProvider(beacon, new TransporterBeaconPeripheral(beacon));
		} else if (event.getObject() instanceof EnanReactorCoreTileEntity) {
			final EnanReactorCoreTileEntity core =
				(EnanReactorCoreTileEntity) event.getObject();
			final EnanReactorCorePeripheral peripheral = new EnanReactorCorePeripheral(core);
			provider = new FixedPeripheralProvider(core, peripheral, peripheral::invalidate);
		} else if (event.getObject() instanceof EnanReactorLaserTileEntity) {
			final EnanReactorLaserTileEntity laser =
				(EnanReactorLaserTileEntity) event.getObject();
			provider = new FixedPeripheralProvider(laser, new EnanReactorLaserPeripheral(laser));
		} else if (event.getObject() instanceof VirtualAssistantTileEntity) {
			final VirtualAssistantTileEntity assistant = (VirtualAssistantTileEntity) event.getObject();
			final VirtualAssistantPeripheral peripheral = new VirtualAssistantPeripheral(assistant);
			provider = new FixedPeripheralProvider(assistant, peripheral, peripheral::invalidate);
		} else if (event.getObject() instanceof RadarTileEntity) {
			final RadarTileEntity radar = (RadarTileEntity) event.getObject();
			final RadarPeripheral peripheral = new RadarPeripheral(radar);
			provider = new FixedPeripheralProvider(radar, peripheral, peripheral::invalidate);
		} else if (event.getObject() instanceof CloakingCoreTileEntity) {
			final CloakingCoreTileEntity core = (CloakingCoreTileEntity) event.getObject();
			final CloakingCorePeripheral peripheral = new CloakingCorePeripheral(core);
			provider = new FixedPeripheralProvider(core, peripheral, peripheral::invalidate);
		} else {
			return;
		}
		event.addCapability(CAPABILITY_ID, provider);
		event.addListener(provider::invalidate);
	}

	/** Legacy flight-console name and API, delegating to whichever core is currently adjacent. */
	public static final class ShipControllerPeripheral implements CommonPeripheral {

		private final ShipControllerTileEntity controller;
		private final Map<IComputerAccess, List<String>> mountedPaths = new HashMap<>();

		private ShipControllerPeripheral(final ShipControllerTileEntity controller) {
			this.controller = controller;
		}

		@Override public String getType() { return "warpdriveShipController"; }
		@Override public Object getTarget() { return controller; }
		@Override public boolean equals(@Nullable final IPeripheral other) {
			return other instanceof ShipControllerPeripheral
				&& ((ShipControllerPeripheral) other).controller == controller;
		}

		@Override
		public void attach(final IComputerAccess computer) {
			if (mountedPaths.containsKey(computer)) return;
			final List<String> paths = new ArrayList<>();
			mountedPaths.put(computer, paths);
			mount(computer, paths, "warpdrive", LUA_COMMON, "WarpDrive common programs");
			mount(computer, paths, "startup", "lua.ComputerCraft/warpdriveShipController/startup",
				"WarpDrive ship controller startup");
		}

		@Override
		public void detach(final IComputerAccess computer) {
			final List<String> paths = mountedPaths.remove(computer);
			if (paths != null) for (final String path : paths) computer.unmount(path);
		}

		private static void mount(final IComputerAccess computer, final List<String> paths,
		                          final String desiredPath, final String resourcePath,
		                          final String label) {
			try {
				final IMount mount = ComputerCraftAPI.createResourceMount(WarpDrive.MODID, resourcePath);
				if (mount == null) return;
				final String actualPath = computer.mount(desiredPath, mount, label);
				if (actualPath != null) paths.add(actualPath);
			} catch (final RuntimeException exception) {
				WarpDrive.logger.error("Unable to mount ship-controller resource {} at {}",
					resourcePath, desiredPath, exception);
			}
		}

		private ShipCoreTileEntity requireCore() throws LuaException {
			final ShipCoreTileEntity core = controller.getShipCore();
			if (core == null) throw new LuaException("No adjacent Ship Core");
			return core;
		}

		@LuaFunction(mainThread = true)
		public Object[] name(final IArguments arguments) throws LuaException {
			if (arguments.count() > 1) throw new LuaException("expected zero or one name");
			return requireCore().name(arguments.count() == 0
				? java.util.Optional.empty() : java.util.Optional.of(arguments.getString(0)));
		}

		@LuaFunction(mainThread = true)
		public Object[] enable(final IArguments arguments) throws LuaException {
			if (arguments.count() > 1) throw new LuaException("expected zero or one boolean");
			return requireCore().enable(arguments.count() == 0
				? java.util.Optional.empty() : java.util.Optional.of(arguments.getBoolean(0)));
		}

		@LuaFunction(mainThread = true) public Object[] getAssemblyStatus() throws LuaException {
			return requireCore().getAssemblyStatus();
		}
		@LuaFunction(mainThread = true) public Object[] getEnergyRequired() throws LuaException {
			return requireCore().getEnergyRequired();
		}
		@LuaFunction(mainThread = true) public Object[] getEnergyStatus() throws LuaException {
			return requireCore().getEnergyStatus();
		}
		@LuaFunction(mainThread = true) public Object[] getLocation() throws LuaException {
			return requireCore().getLocation();
		}
		@LuaFunction(mainThread = true) public Object[] getOrientation() throws LuaException {
			return requireCore().getOrientation();
		}
		@LuaFunction(mainThread = true) public Object[] isInSpace() throws LuaException {
			return requireCore().isInSpace();
		}
		@LuaFunction(mainThread = true) public Object[] isInHyperspace() throws LuaException {
			return requireCore().isInHyperspace();
		}
		@LuaFunction(mainThread = true) public Object[] getShipSize() throws LuaException {
			return requireCore().getShipSize();
		}
		@LuaFunction(mainThread = true) public Object[] getMaxJumpDistance() throws LuaException {
			return requireCore().getMaxJumpDistance();
		}
		@LuaFunction(mainThread = true) public Object[] getAttachedPlayers() throws LuaException {
			return requireCore().getAttachedPlayers();
		}
		@LuaFunction(mainThread = true) public Object[] state() throws LuaException {
			return requireCore().state();
		}

		@LuaFunction(mainThread = true)
		public Object[] dim_positive(final IArguments arguments) throws LuaException {
			if (arguments.count() != 0 && arguments.count() != 3) {
				throw new LuaException("expected zero or three positive dimensions");
			}
			return requireCore().dim_positive(
				optionalInt(arguments, 0), optionalInt(arguments, 1), optionalInt(arguments, 2));
		}

		@LuaFunction(mainThread = true)
		public Object[] dim_negative(final IArguments arguments) throws LuaException {
			if (arguments.count() != 0 && arguments.count() != 3) {
				throw new LuaException("expected zero or three negative dimensions");
			}
			return requireCore().dim_negative(
				optionalInt(arguments, 0), optionalInt(arguments, 1), optionalInt(arguments, 2));
		}

		@LuaFunction(mainThread = true)
		public Object[] movement(final IArguments arguments) throws LuaException {
			if (arguments.count() != 0 && arguments.count() != 3) {
				throw new LuaException("expected zero or three movement values");
			}
			return requireCore().movement(
				optionalInt(arguments, 0), optionalInt(arguments, 1), optionalInt(arguments, 2));
		}

		private static java.util.Optional<Integer> optionalInt(final IArguments arguments,
		                                                       final int index) throws LuaException {
			return arguments.count() > index
				? java.util.Optional.of(arguments.getInt(index)) : java.util.Optional.empty();
		}

		@LuaFunction(mainThread = true)
		public Object[] rotationSteps(final IArguments arguments) throws LuaException {
			if (arguments.count() > 1) throw new LuaException("expected zero or one rotation");
			return requireCore().rotationSteps(optionalInt(arguments, 0));
		}

		@LuaFunction(mainThread = true)
		public Object[] command(final IArguments arguments) throws LuaException {
			if (arguments.count() < 1 || arguments.count() > 2) {
				throw new LuaException("expected command and optional confirmation");
			}
			return requireCore().command(arguments.getString(0), arguments.count() == 1
				? java.util.Optional.empty() : java.util.Optional.of(arguments.getBoolean(1)));
		}

		@LuaFunction(mainThread = true)
		public Object[] targetName(final IArguments arguments) throws LuaException {
			if (arguments.count() > 1) throw new LuaException("expected zero or one target name");
			return requireCore().targetName(arguments.count() == 0
				? java.util.Optional.empty() : java.util.Optional.of(arguments.getString(0)));
		}

		private void invalidate() {
			for (final IComputerAccess computer : new ArrayList<>(mountedPaths.keySet())) detach(computer);
		}
	}

	/** Shared fixed-FE API for passive energy stores which had legacy peripherals. */
	public static class EnergyStoragePeripheral implements CommonPeripheral {

		private final AbstractEnergyTileEntity energy;
		private final String type;

		private EnergyStoragePeripheral(final AbstractEnergyTileEntity energy, final String type) {
			this.energy = energy;
			this.type = type;
		}

		@Override public String getType() { return type; }
		@Override public Object getTarget() { return energy; }
		@Override public boolean equals(@Nullable final IPeripheral other) {
			return other instanceof EnergyStoragePeripheral
				&& ((EnergyStoragePeripheral) other).energy == energy
				&& ((EnergyStoragePeripheral) other).type.equals(type);
		}

		@LuaFunction
		public Object[] getEnergyStatus() {
			return new Object[]{ energy.getEnergyStored(), energy.getMaxEnergyStored(), "FE" };
		}

		@LuaFunction
		public Object[] energyDisplayUnits(final IArguments arguments) throws LuaException {
			if (arguments.count() > 1) throw new LuaException("expected zero or one energy unit");
			if (arguments.count() == 1) {
				final String requested = arguments.getString(0);
				if (!"FE".equalsIgnoreCase(requested) && !"-".equals(requested)) {
					throw new LuaException("Only Forge Energy (FE) is available in 1.16.5");
				}
			}
			return new Object[]{ "FE", true };
		}
	}

	/** Air generator adds the legacy establish/refresh energy estimate to the shared FE API. */
	public static final class AirGeneratorPeripheral extends EnergyStoragePeripheral {

		private final AirGeneratorTileEntity generator;

		private AirGeneratorPeripheral(final AirGeneratorTileEntity generator) {
			super(generator, "warpdriveAirGenerator");
			this.generator = generator;
		}

		@LuaFunction
		public Object[] getEnergyRequired() {
			return new Object[]{ true,
				generator.getTier().getEnergyPerNewAirBlock() / 40.0D,
				generator.getTier().getEnergyPerExistingAirBlock() / 40.0D };
		}
	}

	/** Redstone state plus the inherited common API; sound playback remains client-owned. */
	public static final class SirenPeripheral implements CommonPeripheral {

		private final SirenTileEntity siren;

		private SirenPeripheral(final SirenTileEntity siren) { this.siren = siren; }
		@Override public String getType() { return "warpdriveSiren"; }
		@Override public Object getTarget() { return siren; }
		@Override public boolean equals(@Nullable final IPeripheral other) {
			return other instanceof SirenPeripheral && ((SirenPeripheral) other).siren == siren;
		}

		@LuaFunction(mainThread = true)
		public Object[] state() {
			return new Object[]{ siren.isPowered() ? "active" : "idle", siren.isPowered() };
		}

		@LuaFunction(mainThread = true)
		public Object[] getAssemblyStatus() { return new Object[]{ true, "ok" }; }
	}

	/** Legacy ship scanner API, with one additional target configuration method for token stations. */
	public static final class ShipScannerPeripheral implements CommonPeripheral {

		private final ShipScannerTileEntity scanner;

		private ShipScannerPeripheral(final ShipScannerTileEntity scanner) {
			this.scanner = scanner;
		}

		@Override
		public String getType() { return "warpdriveShipScanner"; }

		@Override
		public Object getTarget() { return scanner; }

		@Override
		public boolean equals(@Nullable final IPeripheral other) {
			return other instanceof ShipScannerPeripheral
			    && ((ShipScannerPeripheral) other).scanner == scanner;
		}

		@LuaFunction(mainThread = true)
		public Object[] scan() { return scanner.startScan(null); }

		@LuaFunction(mainThread = true)
		public Object[] fileName() { return scanner.fileName(); }

		@LuaFunction(mainThread = true)
		public Object[] state() { return scanner.state(); }

		@LuaFunction(mainThread = true)
		public Object[] deploy(final IArguments arguments) throws LuaException {
			if (arguments.count() != 5) {
				throw new LuaException("expected filename, offsetX, offsetY, offsetZ, rotationSteps");
			}
			return scanner.deploy(arguments.getString(0), arguments.getInt(1), arguments.getInt(2),
				arguments.getInt(3), arguments.getInt(4), scanner.findCreativePlayer());
		}

		@LuaFunction(mainThread = true)
		public Object[] configureTarget(final IArguments arguments) throws LuaException {
			if (arguments.count() != 4) {
				throw new LuaException("expected offsetX, offsetY, offsetZ, rotationSteps");
			}
			return scanner.configureTarget(arguments.getInt(0), arguments.getInt(1),
				arguments.getInt(2), arguments.getInt(3), scanner.findCreativePlayer());
		}
	}

	/** Legacy optical-sensor API and result-change event. */
	public static final class CameraPeripheral implements CommonPeripheral,
		CameraTileEntity.RecognitionListener {

		private final CameraTileEntity camera;
		private final Set<IComputerAccess> computers = new HashSet<>();

		private CameraPeripheral(final CameraTileEntity camera) {
			this.camera = camera;
		}

		@Override public String getType() { return "warpdriveCamera"; }
		@Override public Object getTarget() { return camera; }
		@Override public boolean equals(@Nullable final IPeripheral other) {
			return other instanceof CameraPeripheral && ((CameraPeripheral) other).camera == camera;
		}

		@Override
		public void attach(final IComputerAccess computer) {
			if (computers.add(computer) && computers.size() == 1) {
				camera.addRecognitionListener(this);
			}
		}

		@Override
		public void detach(final IComputerAccess computer) {
			computers.remove(computer);
			if (computers.isEmpty()) camera.removeRecognitionListener(this);
		}

		@LuaFunction(mainThread = true)
		public Object[] videoChannel(final IArguments arguments) throws LuaException {
			if (arguments.count() > 1) throw new LuaException("expected zero or one video channel");
			return camera.setOrGetVideoChannel(
				arguments.count() == 0 ? null : arguments.getInt(0));
		}

		@LuaFunction(mainThread = true)
		public Object[] getResults() { return camera.getResults(); }

		@LuaFunction(mainThread = true)
		public Object[] getResultsCount() { return camera.getResultsCount(); }

		@LuaFunction(mainThread = true)
		public Object[] getResult(final IArguments arguments) throws LuaException {
			if (arguments.count() != 1) throw new LuaException("expected result index");
			return camera.getResult(arguments.getInt(0));
		}

		@LuaFunction
		public Object[] getLocalPosition() {
			return new Object[]{ camera.getBlockPos().getX(), camera.getBlockPos().getY(),
				camera.getBlockPos().getZ() };
		}

		@Override
		public void queueEvent(final String eventName, final Object... arguments) {
			for (final IComputerAccess computer : new HashSet<>(computers)) {
				computer.queueEvent(eventName, arguments);
			}
		}

		private void invalidate() {
			camera.removeRecognitionListener(this);
			computers.clear();
		}
	}

	/** Legacy monitor API. */
	public static final class MonitorPeripheral implements CommonPeripheral {

		private final MonitorTileEntity monitor;

		private MonitorPeripheral(final MonitorTileEntity monitor) {
			this.monitor = monitor;
		}

		@Override public String getType() { return "warpdriveMonitor"; }
		@Override public Object getTarget() { return monitor; }
		@Override public boolean equals(@Nullable final IPeripheral other) {
			return other instanceof MonitorPeripheral && ((MonitorPeripheral) other).monitor == monitor;
		}

		@LuaFunction(mainThread = true)
		public Object[] videoChannel(final IArguments arguments) throws LuaException {
			if (arguments.count() > 1) throw new LuaException("expected zero or one video channel");
			return monitor.setOrGetVideoChannel(
				arguments.count() == 0 ? null : arguments.getInt(0));
		}
	}

	/** Legacy chunk-loader API, including the physical computer-interface upgrade gate. */
	public static final class ChunkLoaderPeripheral implements CommonPeripheral {

		private final ChunkLoaderTileEntity loader;

		private ChunkLoaderPeripheral(final ChunkLoaderTileEntity loader) {
			this.loader = loader;
		}

		@Override public String getType() { return "warpdriveChunkLoader"; }
		@Override public Object getTarget() { return loader; }
		@Override public boolean equals(@Nullable final IPeripheral other) {
			return other instanceof ChunkLoaderPeripheral
			    && ((ChunkLoaderPeripheral) other).loader == loader;
		}

		private void requireInterface() throws LuaException {
			if (!loader.hasComputerInterface()) {
				throw new LuaException("Missing Computer interface upgrade.");
			}
		}

		@LuaFunction
		public Object[] isInterfaced() {
			return loader.isInterfacedComputer();
		}

		@LuaFunction(mainThread = true)
		public Object[] name(final IArguments arguments) throws LuaException {
			requireInterface();
			if (arguments.count() > 1) throw new LuaException("expected zero or one name");
			return loader.name(arguments.count() == 0 ? null : arguments.getString(0));
		}

		@LuaFunction(mainThread = true)
		public Object[] enable(final IArguments arguments) throws LuaException {
			requireInterface();
			if (arguments.count() > 1) throw new LuaException("expected zero or one boolean");
			return loader.enable(arguments.count() == 0 ? null : arguments.getBoolean(0));
		}

		@LuaFunction(mainThread = true)
		public Object[] bounds(final IArguments arguments) throws LuaException {
			requireInterface();
			if (arguments.count() != 0 && arguments.count() != 4) {
				throw new LuaException("expected zero or four bounds");
			}
			return loader.boundsComputer(arguments.count() == 0 ? null : new int[]{
				arguments.getInt(0), arguments.getInt(1),
				arguments.getInt(2), arguments.getInt(3) });
		}

		@LuaFunction(mainThread = true)
		public Object[] radius(final IArguments arguments) throws LuaException {
			requireInterface();
			if (arguments.count() > 1) throw new LuaException("expected zero or one radius");
			return loader.radiusComputer(arguments.count() == 0 ? null : arguments.getInt(0));
		}

		@LuaFunction
		public Object[] getEnergyRequired() throws LuaException {
			requireInterface();
			return loader.getEnergyRequiredComputer();
		}

		@LuaFunction
		public Object[] getEnergyStatus() throws LuaException {
			requireInterface();
			return loader.getEnergyStatusComputer();
		}

		@LuaFunction
		public Object[] getLocalPosition() throws LuaException {
			requireInterface();
			return loader.getLocalPositionComputer();
		}

		@LuaFunction(mainThread = true)
		public Object[] getTier() throws LuaException {
			requireInterface();
			return loader.getTierComputer();
		}

		@LuaFunction
		public Object[] getUpgrades() throws LuaException {
			requireInterface();
			return loader.getUpgradesComputer();
		}

		@LuaFunction
		public Object[] getAssemblyStatus() throws LuaException {
			requireInterface();
			return new Object[]{ true, "ok" };
		}

		@LuaFunction(mainThread = true)
		public Object[] state() throws LuaException {
			requireInterface();
			return loader.stateComputer();
		}

		@LuaFunction
		public Object[] getVersion() throws LuaException {
			requireInterface();
			return WarpDrive.getVersionNumbers();
		}
	}

	private interface PeripheralProvider extends ICapabilityProvider {
		void invalidate();
	}

	/** Preserve legacy peripheral type names while retaining CC:Tweaked's generated method table. */
	private static final class LegacyTypePeripheral implements IDynamicPeripheral {

		private final IDynamicPeripheral delegate;
		private final String type;
		private final TileEntity target;

		private LegacyTypePeripheral(final IDynamicPeripheral delegate, final String type,
		                             final TileEntity target) {
			this.delegate = delegate;
			this.type = type;
			this.target = target;
		}

		@Override public String getType() { return type; }
		@Override public Set<String> getAdditionalTypes() { return delegate.getAdditionalTypes(); }
		@Override public String[] getMethodNames() { return delegate.getMethodNames(); }
		@Override public Object getTarget() { return target; }
		@Override public void attach(final IComputerAccess computer) { delegate.attach(computer); }
		@Override public void detach(final IComputerAccess computer) { delegate.detach(computer); }

		@Override
		public MethodResult callMethod(final IComputerAccess computer, final ILuaContext context,
		                               final int method, final IArguments arguments) throws LuaException {
			return delegate.callMethod(computer, context, method, arguments);
		}

		@Override
		public boolean equals(@Nullable final IPeripheral other) {
			return other instanceof LegacyTypePeripheral
				&& ((LegacyTypePeripheral) other).target == target
				&& ((LegacyTypePeripheral) other).type.equals(type);
		}
	}

	private static final class FixedPeripheralProvider implements PeripheralProvider {

		private final TileEntity tileEntity;
		private final Runnable cleanup;
		private LazyOptional<IPeripheral> peripheral;

		private FixedPeripheralProvider(final TileEntity tileEntity, final IPeripheral value) {
			this(tileEntity, value, () -> { });
		}

		private FixedPeripheralProvider(final TileEntity tileEntity, final IPeripheral value,
		                                final Runnable cleanup) {
			this.tileEntity = tileEntity;
			this.cleanup = cleanup;
			peripheral = LazyOptional.of(() -> value);
		}

		@Nonnull
		@Override
		public <T> LazyOptional<T> getCapability(@Nonnull final Capability<T> capability,
		                                          @Nullable final Direction side) {
			if (capability != Capabilities.CAPABILITY_PERIPHERAL || tileEntity.getLevel() == null) {
				return LazyOptional.empty();
			}
			return peripheral.cast();
		}

		@Override
		public void invalidate() {
			cleanup.run();
			peripheral.invalidate();
			peripheral = LazyOptional.empty();
		}
	}

	private static final class ShipCorePeripheralProvider implements PeripheralProvider {

		private final ShipCoreTileEntity shipCore;
		private LazyOptional<IPeripheral> peripheral = LazyOptional.empty();

		private ShipCorePeripheralProvider(final ShipCoreTileEntity shipCore) {
			this.shipCore = shipCore;
		}

		@Nonnull
		@Override
		public <T> LazyOptional<T> getCapability(@Nonnull final Capability<T> capability,
		                                          @Nullable final Direction side) {
			if (capability != Capabilities.CAPABILITY_PERIPHERAL || shipCore.getLevel() == null) {
				return LazyOptional.empty();
			}
			if (!peripheral.isPresent()) {
				final IPeripheral discovered = GenericPeripheralProvider.getPeripheral(
					shipCore.getLevel(), shipCore.getBlockPos(), side, ignored -> invalidate());
				if (discovered instanceof IDynamicPeripheral) {
					final IPeripheral value = new LegacyTypePeripheral(
						(IDynamicPeripheral) discovered, "warpdriveShipCore", shipCore);
					peripheral = LazyOptional.of(() -> value);
				} else if (discovered != null) {
					WarpDrive.logger.warn("Generated Ship Core peripheral was not dynamic; "
						+ "legacy type alias is unavailable");
					peripheral = LazyOptional.of(() -> discovered);
				}
			}
			return peripheral.cast();
		}

		@Override
		public void invalidate() {
			peripheral.invalidate();
			peripheral = LazyOptional.empty();
		}
	}

	private static final class LaserPeripheralProvider implements PeripheralProvider {

		private final LaserTileEntity laser;
		private final LaserPeripheral value;
		private LazyOptional<IPeripheral> peripheral;

		private LaserPeripheralProvider(final LaserTileEntity laser) {
			this.laser = laser;
			value = laser instanceof LaserCameraTileEntity
				? new LaserCameraPeripheral((LaserCameraTileEntity) laser)
				: new LaserPeripheral(laser);
			peripheral = LazyOptional.of(() -> value);
		}

		@Nonnull
		@Override
		public <T> LazyOptional<T> getCapability(@Nonnull final Capability<T> capability,
		                                          @Nullable final Direction side) {
			if (capability != Capabilities.CAPABILITY_PERIPHERAL || laser.getLevel() == null) {
				return LazyOptional.empty();
			}
			return peripheral.cast();
		}

		@Override
		public void invalidate() {
			value.invalidate();
			peripheral.invalidate();
			peripheral = LazyOptional.empty();
		}
	}

	private static final class LiftPeripheralProvider implements PeripheralProvider {

		private final LiftTileEntity lift;
		private LazyOptional<IPeripheral> peripheral;

		private LiftPeripheralProvider(final LiftTileEntity lift) {
			this.lift = lift;
			peripheral = LazyOptional.of(() -> new LiftPeripheral(lift));
		}

		@Nonnull
		@Override
		public <T> LazyOptional<T> getCapability(@Nonnull final Capability<T> capability,
		                                          @Nullable final Direction side) {
			if (capability != Capabilities.CAPABILITY_PERIPHERAL || lift.getLevel() == null) {
				return LazyOptional.empty();
			}
			return peripheral.cast();
		}

		@Override
		public void invalidate() {
			peripheral.invalidate();
			peripheral = LazyOptional.empty();
		}
	}

	private static final class SpeakerPeripheralProvider implements PeripheralProvider {

		private final SpeakerTileEntity speaker;
		private LazyOptional<IPeripheral> peripheral;

		private SpeakerPeripheralProvider(final SpeakerTileEntity speaker) {
			this.speaker = speaker;
			peripheral = LazyOptional.of(() -> new SpeakerPeripheral(speaker));
		}

		@Nonnull
		@Override
		public <T> LazyOptional<T> getCapability(@Nonnull final Capability<T> capability,
		                                          @Nullable final Direction side) {
			if (capability != Capabilities.CAPABILITY_PERIPHERAL || speaker.getLevel() == null) {
				return LazyOptional.empty();
			}
			return peripheral.cast();
		}

		@Override
		public void invalidate() {
			peripheral.invalidate();
			peripheral = LazyOptional.empty();
		}
	}

	/** CC:Tweaked surface matching the 1.12.2 laser peripheral and event names. */
	public static class LaserPeripheral implements CommonPeripheral, LaserTileEntity.LaserEventListener {

		protected final LaserTileEntity laser;
		private final Set<IComputerAccess> computers = new HashSet<>();

		protected LaserPeripheral(final LaserTileEntity laser) {
			this.laser = laser;
		}

		@Override
		public String getType() {
			return "warpdriveLaser";
		}

		@Override
		public Object getTarget() {
			return laser;
		}

		@Override
		public void attach(final IComputerAccess computer) {
			if (computers.add(computer) && computers.size() == 1) {
				laser.addEventListener(this);
			}
		}

		@Override
		public void detach(final IComputerAccess computer) {
			computers.remove(computer);
			if (computers.isEmpty()) {
				laser.removeEventListener(this);
			}
		}

		@Override
		public boolean equals(@Nullable final IPeripheral other) {
			return other instanceof LaserPeripheral && ((LaserPeripheral) other).laser == laser;
		}

		@LuaFunction(mainThread = true)
		public Object[] emitBeam(final IArguments arguments) throws LuaException {
			if (arguments.count() == 2) {
				return laser.initiateBeamEmission(arguments.getFiniteDouble(0),
					arguments.getFiniteDouble(1));
			}
			if (arguments.count() == 3) {
				return laser.initiateBeamEmissionVector(arguments.getFiniteDouble(0),
					arguments.getFiniteDouble(1), arguments.getFiniteDouble(2));
			}
			throw new LuaException("expected yaw, pitch or deltaX, deltaY, deltaZ");
		}

		@LuaFunction(mainThread = true)
		public Object[] beamFrequency(final IArguments arguments) throws LuaException {
			if (arguments.count() > 1) {
				throw new LuaException("expected zero or one beam frequency");
			}
			return laser.setOrGetBeamFrequency(arguments.count() == 0 ? null : arguments.getInt(0));
		}

		@LuaFunction(mainThread = true)
		public Object[] getScanResult() {
			return laser.getScanResult();
		}

		@LuaFunction(mainThread = true)
		public Object[] getEnergyRequired() {
			return laser.getEnergyRequired();
		}

		@LuaFunction(mainThread = true)
		public Object[] getEnergyStatus() {
			return laser.getEnergyStatus();
		}

		@LuaFunction(mainThread = true)
		public Object[] laserMediumDirection() {
			return laser.laserMediumDirection();
		}

		@LuaFunction(mainThread = true)
		public Object[] laserMediumCount() {
			return laser.laserMediumCount();
		}

		@LuaFunction
		public Object[] getLocalPosition() {
			return new Object[]{ laser.getBlockPos().getX(), laser.getBlockPos().getY(),
				laser.getBlockPos().getZ() };
		}

		@Override
		public void queueEvent(final String eventName, final Object... arguments) {
			for (final IComputerAccess computer : new HashSet<>(computers)) {
				computer.queueEvent(eventName, arguments);
			}
		}

		private void invalidate() {
			laser.removeEventListener(this);
			computers.clear();
		}
	}

	/** The combined camera retains the complete laser API and adds its video channel. */
	public static final class LaserCameraPeripheral extends LaserPeripheral {

		private final LaserCameraTileEntity laserCamera;

		private LaserCameraPeripheral(final LaserCameraTileEntity laserCamera) {
			super(laserCamera);
			this.laserCamera = laserCamera;
		}

		@Override
		public String getType() {
			return "warpdriveLaserCamera";
		}

		@LuaFunction(mainThread = true)
		public Object[] videoChannel(final IArguments arguments) throws LuaException {
			if (arguments.count() > 1) {
				throw new LuaException("expected zero or one video channel");
			}
			return laserCamera.setOrGetVideoChannel(
				arguments.count() == 0 ? null : arguments.getInt(0));
		}
	}

	/** Computer controls retained from the legacy mining-laser peripheral. */
	public static final class MiningLaserPeripheral implements CommonPeripheral {

		private final MiningLaserTileEntity miningLaser;

		private MiningLaserPeripheral(final MiningLaserTileEntity miningLaser) {
			this.miningLaser = miningLaser;
		}

		@Override public String getType() { return "warpdriveMiningLaser"; }
		@Override public Object getTarget() { return miningLaser; }
		@Override public boolean equals(@Nullable final IPeripheral other) {
			return other instanceof MiningLaserPeripheral
			    && ((MiningLaserPeripheral) other).miningLaser == miningLaser;
		}

		@LuaFunction(mainThread = true) public Object[] state() { return miningLaser.state(); }
		@LuaFunction(mainThread = true) public Object[] getEnergyRequired() { return miningLaser.getEnergyRequired(); }
		@LuaFunction(mainThread = true) public Object[] getEnergyStatus() { return miningLaser.getEnergyStatus(); }
		@LuaFunction(mainThread = true) public Object[] laserMediumDirection() { return miningLaser.laserMediumDirection(); }
		@LuaFunction(mainThread = true) public Object[] laserMediumCount() { return miningLaser.laserMediumCount(); }

		@LuaFunction(mainThread = true)
		public Object[] enable(final IArguments arguments) throws LuaException {
			if (arguments.count() > 1) throw new LuaException("expected zero or one boolean");
			return miningLaser.enable(arguments.count() == 0 ? null : arguments.getBoolean(0));
		}

		@LuaFunction(mainThread = true)
		public Object[] offset(final IArguments arguments) throws LuaException {
			if (arguments.count() > 1) throw new LuaException("expected zero or one layer offset");
			return miningLaser.offset(arguments.count() == 0 ? null : arguments.getInt(0));
		}

		@LuaFunction(mainThread = true)
		public Object[] onlyOres(final IArguments arguments) throws LuaException {
			if (arguments.count() > 1) throw new LuaException("expected zero or one boolean");
			return miningLaser.onlyOres(arguments.count() == 0 ? null : arguments.getBoolean(0));
		}

		@LuaFunction(mainThread = true)
		public Object[] silktouch(final IArguments arguments) throws LuaException {
			if (arguments.count() > 1) throw new LuaException("expected zero or one boolean");
			return miningLaser.silktouch(arguments.count() == 0 ? null : arguments.getBoolean(0));
		}
	}

	/** Computer controls retained from the legacy laser tree-farm peripheral. */
	public static final class LaserTreeFarmPeripheral implements CommonPeripheral {

		private final LaserTreeFarmTileEntity treeFarm;

		private LaserTreeFarmPeripheral(final LaserTreeFarmTileEntity treeFarm) {
			this.treeFarm = treeFarm;
		}

		@Override public String getType() { return "warpdriveLaserTreeFarm"; }
		@Override public Object getTarget() { return treeFarm; }
		@Override public boolean equals(@Nullable final IPeripheral other) {
			return other instanceof LaserTreeFarmPeripheral
			    && ((LaserTreeFarmPeripheral) other).treeFarm == treeFarm;
		}

		@LuaFunction(mainThread = true) public Object[] state() { return treeFarm.state(); }
		@LuaFunction(mainThread = true) public Object[] getEnergyRequired() { return treeFarm.getEnergyRequired(); }
		@LuaFunction(mainThread = true) public Object[] getEnergyStatus() { return treeFarm.getEnergyStatus(); }
		@LuaFunction(mainThread = true) public Object[] laserMediumDirection() { return treeFarm.laserMediumDirection(); }
		@LuaFunction(mainThread = true) public Object[] laserMediumCount() { return treeFarm.laserMediumCount(); }

		@LuaFunction(mainThread = true)
		public Object[] enable(final IArguments arguments) throws LuaException {
			if (arguments.count() > 1) throw new LuaException("expected zero or one boolean");
			return treeFarm.enable(arguments.count() == 0 ? null : arguments.getBoolean(0));
		}

		@LuaFunction(mainThread = true)
		public Object[] radius(final IArguments arguments) throws LuaException {
			if (arguments.count() > 2) throw new LuaException("expected zero, one, or two radii");
			return treeFarm.radius(
				arguments.count() > 0 ? arguments.getInt(0) : null,
				arguments.count() > 1 ? arguments.getInt(1) : null);
		}

		@LuaFunction(mainThread = true)
		public Object[] breakLeaves(final IArguments arguments) throws LuaException {
			if (arguments.count() > 1) throw new LuaException("expected zero or one boolean");
			return treeFarm.breakLeaves(arguments.count() == 0 ? null : arguments.getBoolean(0));
		}

		@LuaFunction(mainThread = true)
		public Object[] silktouch(final IArguments arguments) throws LuaException {
			if (arguments.count() > 1) throw new LuaException("expected zero or one boolean");
			return treeFarm.silktouch(arguments.count() == 0 ? null : arguments.getBoolean(0));
		}

		@LuaFunction(mainThread = true)
		public Object[] tapTrees(final IArguments arguments) throws LuaException {
			if (arguments.count() > 1) throw new LuaException("expected zero or one boolean");
			return treeFarm.tapTrees(arguments.count() == 0 ? null : arguments.getBoolean(0));
		}
	}

	/** CC:Tweaked surface matching 1.12.2's lift methods plus the inherited machine helpers. */
	public static final class LiftPeripheral implements CommonPeripheral {

		private final LiftTileEntity lift;

		private LiftPeripheral(final LiftTileEntity lift) {
			this.lift = lift;
		}

		@Override
		public String getType() {
			return "warpdriveLift";
		}

		@Override
		public Object getTarget() {
			return lift;
		}

		@Override
		public boolean equals(@Nullable final IPeripheral other) {
			return other instanceof LiftPeripheral && ((LiftPeripheral) other).lift == lift;
		}

		@LuaFunction(mainThread = true)
		public Object[] mode(final IArguments arguments) throws LuaException {
			if (arguments.count() > 1) {
				throw new LuaException("expected zero or one mode (up, down, or redstone)");
			}
			return lift.setOrGetMode(arguments.count() == 0 ? null : arguments.getString(0));
		}

		@LuaFunction(mainThread = true)
		public Object[] state() {
			return lift.state();
		}

		@LuaFunction(mainThread = true)
		public Object[] getEnergyRequired() {
			return lift.getEnergyRequired();
		}

		@LuaFunction(mainThread = true)
		public Object[] getEnergyStatus() {
			return lift.getEnergyStatus();
		}

		@LuaFunction(mainThread = true)
		public Object[] enable(final IArguments arguments) throws LuaException {
			if (arguments.count() > 1) {
				throw new LuaException("expected zero or one boolean");
			}
			return lift.enable(arguments.count() == 0 ? null : arguments.getBoolean(0));
		}
	}

	/** CC:Tweaked surface for the tiered local chat speaker. */
	public static final class SpeakerPeripheral implements CommonPeripheral {

		private final SpeakerTileEntity speaker;

		private SpeakerPeripheral(final SpeakerTileEntity speaker) {
			this.speaker = speaker;
		}

		@Override
		public String getType() {
			return "warpdriveSpeaker";
		}

		@Override
		public Object getTarget() {
			return speaker;
		}

		@Override
		public boolean equals(@Nullable final IPeripheral other) {
			return other instanceof SpeakerPeripheral && ((SpeakerPeripheral) other).speaker == speaker;
		}

		@LuaFunction(mainThread = true)
		public Object[] speak(final IArguments arguments) throws LuaException {
			if (arguments.count() > 1) {
				throw new LuaException("expected zero or one message");
			}
			return speaker.speak(arguments.count() == 0 ? null : arguments.getString(0));
		}

		@LuaFunction(mainThread = true)
		public Object[] enable(final IArguments arguments) throws LuaException {
			if (arguments.count() > 1) {
				throw new LuaException("expected zero or one boolean");
			}
			return speaker.enable(arguments.count() == 0 ? null : arguments.getBoolean(0));
		}
	}

	/** CC:Tweaked surface matching the legacy force-field projector methods. */
	public static final class ForceFieldProjectorPeripheral implements CommonPeripheral {

		private final ForceFieldProjectorTileEntity projector;

		private ForceFieldProjectorPeripheral(final ForceFieldProjectorTileEntity projector) {
			this.projector = projector;
		}

		@Override
		public String getType() { return "warpdriveForceFieldProjector"; }

		@Override
		public Object getTarget() { return projector; }

		@Override
		public boolean equals(@Nullable final IPeripheral other) {
			return other instanceof ForceFieldProjectorPeripheral
			    && ((ForceFieldProjectorPeripheral) other).projector == projector;
		}

		@LuaFunction(mainThread = true)
		public Object[] beamFrequency(final IArguments arguments) throws LuaException {
			if (arguments.count() > 1) throw new LuaException("expected zero or one beam frequency");
			return projector.setOrGetBeamFrequency(arguments.count() == 0 ? null : arguments.getInt(0));
		}

		@LuaFunction(mainThread = true)
		public Object[] enable(final IArguments arguments) throws LuaException {
			if (arguments.count() > 1) throw new LuaException("expected zero or one boolean");
			return projector.enable(arguments.count() == 0 ? null : arguments.getBoolean(0));
		}

		@LuaFunction(mainThread = true)
		public Object[] state() { return projector.state(); }

		@LuaFunction(mainThread = true)
		public Object[] getEnergyRequired() { return projector.getEnergyRequired(); }

		@LuaFunction(mainThread = true)
		public Object[] getEnergyStatus() { return projector.getEnergyStatus(); }

		@LuaFunction(mainThread = true)
		public Object[] min(final IArguments arguments) throws LuaException {
			return vector(arguments, projector::min, "min");
		}

		@LuaFunction(mainThread = true)
		public Object[] max(final IArguments arguments) throws LuaException {
			return vector(arguments, projector::max, "max");
		}

		@LuaFunction(mainThread = true)
		public Object[] translation(final IArguments arguments) throws LuaException {
			return vector(arguments, projector::translation, "translation");
		}

		@LuaFunction(mainThread = true)
		public Object[] rotation(final IArguments arguments) throws LuaException {
			if (arguments.count() > 3) throw new LuaException("expected zero to three angles");
			return projector.rotation(
				arguments.count() > 0 ? arguments.getFiniteDouble(0) : null,
				arguments.count() > 1 ? arguments.getFiniteDouble(1) : null,
				arguments.count() > 2 ? arguments.getFiniteDouble(2) : null);
		}

		private Object[] vector(final IArguments arguments, final VectorMethod method,
		                        final String name) throws LuaException {
			if (arguments.count() != 0 && arguments.count() != 3) {
				throw new LuaException("expected zero or three values for " + name);
			}
			return method.apply(
				arguments.count() == 3 ? arguments.getFiniteDouble(0) : null,
				arguments.count() == 3 ? arguments.getFiniteDouble(1) : null,
				arguments.count() == 3 ? arguments.getFiniteDouble(2) : null);
		}

		private interface VectorMethod {
			Object[] apply(Double x, Double y, Double z);
		}
	}

	/**
	 * The legacy weapon controller is intentionally just a script host: its bundled startup program
	 * discovers laser and laser-camera peripherals and coordinates batteries and firing stations.
	 */
	public static final class WeaponControllerPeripheral implements CommonPeripheral {

		private final WeaponControllerTileEntity controller;
		private final Map<IComputerAccess, List<String>> mountedPaths = new HashMap<>();

		private WeaponControllerPeripheral(final WeaponControllerTileEntity controller) {
			this.controller = controller;
		}

		@Override public String getType() { return "warpdriveWeaponController"; }
		@Override public Object getTarget() { return controller; }

		@Override
		public boolean equals(@Nullable final IPeripheral other) {
			return other instanceof WeaponControllerPeripheral
			    && ((WeaponControllerPeripheral) other).controller == controller;
		}

		@Override
		public void attach(final IComputerAccess computer) {
			if (mountedPaths.containsKey(computer)) return;
			final List<String> paths = new ArrayList<>();
			mountedPaths.put(computer, paths);
			mount(computer, paths, "warpdrive", LUA_COMMON,
				"WarpDrive common programs");
			mount(computer, paths, "warpdrive/WeaponController", LUA_WEAPON_CONTROLLER,
				"WarpDrive weapon-controller programs");
			mount(computer, paths, "startup", LUA_WEAPON_CONTROLLER + "/startup",
				"WarpDrive weapon-controller startup");
		}

		@Override
		public void detach(final IComputerAccess computer) {
			final List<String> paths = mountedPaths.remove(computer);
			if (paths == null) return;
			for (final String path : paths) {
				computer.unmount(path);
			}
		}

		private static void mount(final IComputerAccess computer, final List<String> paths,
		                          final String desiredPath, final String resourcePath,
		                          final String label) {
			try {
				final IMount mount = ComputerCraftAPI.createResourceMount(
					WarpDrive.MODID, resourcePath);
				if (mount == null) {
					WarpDrive.logger.error("Missing CC:Tweaked resource mount {}", resourcePath);
					return;
				}
				final String actualPath = computer.mount(desiredPath, mount, label);
				if (actualPath != null) paths.add(actualPath);
			} catch (final RuntimeException exception) {
				WarpDrive.logger.error("Unable to mount {} at {} on computer {}",
					resourcePath, desiredPath, computer.getID(), exception);
			}
		}

		private void invalidate() {
			for (final IComputerAccess computer : new ArrayList<>(mountedPaths.keySet())) {
				detach(computer);
			}
		}
	}

	/** Legacy environmental-sensor query surface. */
	public static final class EnvironmentalSensorPeripheral implements CommonPeripheral {

		private final EnvironmentalSensorTileEntity sensor;

		private EnvironmentalSensorPeripheral(final EnvironmentalSensorTileEntity sensor) {
			this.sensor = sensor;
		}

		@Override public String getType() { return "warpdriveEnvironmentalSensor"; }
		@Override public Object getTarget() { return sensor; }
		@Override public boolean equals(@Nullable final IPeripheral other) {
			return other instanceof EnvironmentalSensorPeripheral
			    && ((EnvironmentalSensorPeripheral) other).sensor == sensor;
		}

		@LuaFunction(mainThread = true)
		public Object[] enable(final IArguments arguments) throws LuaException {
			if (arguments.count() > 1) throw new LuaException("expected zero or one boolean");
			return sensor.enable(arguments.count() == 0 ? null : arguments.getBoolean(0));
		}

		@LuaFunction(mainThread = true) public Object[] getAtmosphere() { return sensor.getAtmosphere(); }
		@LuaFunction(mainThread = true) public Object[] getBiome() { return sensor.getBiome(); }
		@LuaFunction(mainThread = true) public Object[] getHumidity() { return sensor.getHumidity(); }
		@LuaFunction(mainThread = true) public Object[] getTemperature() { return sensor.getTemperature(); }
		@LuaFunction(mainThread = true) public Object[] getWeather() { return sensor.getWeather(); }
		@LuaFunction(mainThread = true) public Object[] getWorldTime() { return sensor.getWorldTime(); }
	}

	/** Identity result query and completion/abort events from the legacy scanner. */
	public static final class BiometricScannerPeripheral implements CommonPeripheral,
		BiometricScannerTileEntity.ScanListener {

		private final BiometricScannerTileEntity scanner;
		private final Set<IComputerAccess> computers = new HashSet<>();

		private BiometricScannerPeripheral(final BiometricScannerTileEntity scanner) {
			this.scanner = scanner;
		}

		@Override public String getType() { return "warpdriveBiometricScanner"; }
		@Override public Object getTarget() { return scanner; }
		@Override public boolean equals(@Nullable final IPeripheral other) {
			return other instanceof BiometricScannerPeripheral
			    && ((BiometricScannerPeripheral) other).scanner == scanner;
		}

		@Override
		public void attach(final IComputerAccess computer) {
			if (computers.add(computer) && computers.size() == 1) scanner.addListener(this);
		}

		@Override
		public void detach(final IComputerAccess computer) {
			computers.remove(computer);
			if (computers.isEmpty()) scanner.removeListener(this);
		}

		@LuaFunction(mainThread = true)
		public Object[] enable(final IArguments arguments) throws LuaException {
			if (arguments.count() > 1) throw new LuaException("expected zero or one boolean");
			return scanner.enable(arguments.count() == 0 ? null : arguments.getBoolean(0));
		}

		@LuaFunction(mainThread = true)
		public Object[] getScanResults() { return scanner.getScanResults(); }

		@Override
		public void queueEvent(final String eventName, final Object... arguments) {
			for (final IComputerAccess computer : new HashSet<>(computers)) {
				computer.queueEvent(eventName, arguments);
			}
		}

		private void invalidate() {
			scanner.removeListener(this);
			computers.clear();
		}
	}

	/** Legacy security-station allowlist management API. */
	public static final class SecurityStationPeripheral implements CommonPeripheral {

		private final SecurityStationTileEntity station;

		private SecurityStationPeripheral(final SecurityStationTileEntity station) {
			this.station = station;
		}

		@Override public String getType() { return "warpdriveSecurityStation"; }
		@Override public Object getTarget() { return station; }
		@Override public boolean equals(@Nullable final IPeripheral other) {
			return other instanceof SecurityStationPeripheral
			    && ((SecurityStationPeripheral) other).station == station;
		}

		@LuaFunction(mainThread = true)
		public Object[] enable(final IArguments arguments) throws LuaException {
			if (arguments.count() > 1) throw new LuaException("expected zero or one boolean");
			return station.enable(arguments.count() == 0 ? null : arguments.getBoolean(0));
		}

		@LuaFunction(mainThread = true)
		public Object[] getAttachedPlayers() { return station.getAttachedPlayers(); }

		@LuaFunction(mainThread = true)
		public Object[] removeAllAttachedPlayers() { return station.removeAllAttachedPlayers(); }

		@LuaFunction(mainThread = true)
		public Object[] removeAttachedPlayer(final IArguments arguments) throws LuaException {
			if (arguments.count() != 1) throw new LuaException("expected one player name");
			return station.removeAttachedPlayer(arguments.getString(0));
		}
	}

	/** Full legacy transporter-room API, event stream and bundled terminal UI. */
	public static final class TransporterCorePeripheral implements CommonPeripheral,
		TransporterCoreTileEntity.TransporterEventListener {

		private final TransporterCoreTileEntity core;
		private final Set<IComputerAccess> computers = new HashSet<>();
		private final Map<IComputerAccess, List<String>> mountedPaths = new HashMap<>();

		private TransporterCorePeripheral(final TransporterCoreTileEntity core) { this.core = core; }
		@Override public String getType() { return "warpdriveTransporterCore"; }
		@Override public Object getTarget() { return core; }
		@Override public boolean equals(@Nullable final IPeripheral other) {
			return other instanceof TransporterCorePeripheral
				&& ((TransporterCorePeripheral) other).core == core;
		}

		@Override
		public void attach(final IComputerAccess computer) {
			if (!computers.add(computer)) return;
			if (computers.size() == 1) core.addEventListener(this);
			final List<String> paths = new ArrayList<>();
			mountedPaths.put(computer, paths);
			mount(computer, paths, "warpdrive", LUA_COMMON, "WarpDrive common programs");
			mount(computer, paths, "warpdrive/TransporterCore", LUA_TRANSPORTER,
				"WarpDrive transporter programs");
			mount(computer, paths, "startup", LUA_TRANSPORTER + "/startup",
				"WarpDrive transporter startup");
		}

		@Override
		public void detach(final IComputerAccess computer) {
			computers.remove(computer);
			final List<String> paths = mountedPaths.remove(computer);
			if (paths != null) for (final String path : paths) computer.unmount(path);
			if (computers.isEmpty()) core.removeEventListener(this);
		}

		private static void mount(final IComputerAccess computer, final List<String> paths,
		                          final String desiredPath, final String resourcePath,
		                          final String label) {
			try {
				final IMount mount = ComputerCraftAPI.createResourceMount(WarpDrive.MODID, resourcePath);
				if (mount == null) return;
				final String actualPath = computer.mount(desiredPath, mount, label);
				if (actualPath != null) paths.add(actualPath);
			} catch (final RuntimeException exception) {
				WarpDrive.logger.error("Unable to mount transporter resource {} at {}", resourcePath,
					desiredPath, exception);
			}
		}

		@LuaFunction public Object[] isInterfaced() {
			return new Object[]{ true, "CC:Tweaked peripheral available." };
		}

		@LuaFunction(mainThread = true)
		public Object[] name(final IArguments arguments) throws LuaException {
			if (arguments.count() > 1) throw new LuaException("expected zero or one name");
			return core.name(arguments.count() == 0 ? null : arguments.getString(0));
		}

		@LuaFunction(mainThread = true)
		public Object[] enable(final IArguments arguments) throws LuaException {
			if (arguments.count() > 1) throw new LuaException("expected zero or one boolean");
			return core.enable(arguments.count() == 0 ? null : arguments.getBoolean(0));
		}

		@LuaFunction(mainThread = true)
		public Object[] beamFrequency(final IArguments arguments) throws LuaException {
			if (arguments.count() > 1) throw new LuaException("expected zero or one frequency");
			return core.beamFrequency(arguments.count() == 0 ? null : arguments.getInt(0));
		}

		@LuaFunction public Object[] state() { return core.state(); }

		@LuaFunction(mainThread = true)
		public Object[] remoteLocation(final IArguments arguments) throws LuaException {
			if (arguments.count() == 0) return core.remoteLocation();
			if (arguments.count() == 3) {
				core.setRemoteCoordinates(arguments.getInt(0), arguments.getInt(1), arguments.getInt(2));
				return core.remoteLocation();
			}
			if (arguments.count() != 1) {
				throw new LuaException("expected a signature/player name or x, y, z");
			}
			final String value = arguments.getString(0);
			try {
				core.setRemoteUuid(java.util.UUID.fromString(value));
			} catch (final IllegalArgumentException exception) {
				core.setRemotePlayer(value);
			}
			return core.remoteLocation();
		}

		@LuaFunction(mainThread = true)
		public Object[] lock(final IArguments arguments) throws LuaException {
			if (arguments.count() > 1) throw new LuaException("expected zero or one boolean");
			return core.lock(arguments.count() == 0 ? null : arguments.getBoolean(0));
		}

		@LuaFunction(mainThread = true)
		public Object[] energyFactor(final IArguments arguments) throws LuaException {
			if (arguments.count() > 1) throw new LuaException("expected zero or one factor");
			return core.energyFactor(arguments.count() == 0 ? null : arguments.getFiniteDouble(0));
		}

		@LuaFunction public Object[] getLockStrength() { return core.getLockStrength(); }
		@LuaFunction public Object[] getEnergyRequired() { return core.getEnergyRequired(); }
		@LuaFunction public Object[] getEnergyStatus() {
			return new Object[]{ core.getEnergyStored(), core.getMaxEnergyStored(), "FE" };
		}
		@LuaFunction public Object[] getLocalPosition() { return core.getLocalPosition(); }

		@LuaFunction(mainThread = true)
		public Object[] energize(final IArguments arguments) throws LuaException {
			if (arguments.count() > 1) throw new LuaException("expected zero or one boolean");
			return core.energize(arguments.count() == 0 ? null : arguments.getBoolean(0));
		}

		@Override
		public void queueEvent(final String event, final Object... arguments) {
			for (final IComputerAccess computer : new HashSet<>(computers)) {
				computer.queueEvent(event, arguments);
			}
		}

		private void invalidate() {
			for (final IComputerAccess computer : new ArrayList<>(computers)) detach(computer);
		}
	}

	/** Placed beacon controls; portable beacons intentionally have no peripheral. */
	public static final class TransporterBeaconPeripheral implements CommonPeripheral {
		private final TransporterBeaconTileEntity beacon;
		private TransporterBeaconPeripheral(final TransporterBeaconTileEntity beacon) {
			this.beacon = beacon;
		}
		@Override public String getType() { return "warpdriveTransporterBeacon"; }
		@Override public Object getTarget() { return beacon; }
		@Override public boolean equals(@Nullable final IPeripheral other) {
			return other instanceof TransporterBeaconPeripheral
				&& ((TransporterBeaconPeripheral) other).beacon == beacon;
		}
		@LuaFunction public Object[] isInterfaced() {
			return new Object[]{ true, "CC:Tweaked peripheral available." };
		}
		@LuaFunction(mainThread = true)
		public Object[] enable(final IArguments arguments) throws LuaException {
			if (arguments.count() > 1) throw new LuaException("expected zero or one boolean");
			return beacon.enable(arguments.count() == 0 ? null : arguments.getBoolean(0));
		}
		@LuaFunction public Object[] isActive() { return beacon.isActiveComputer(); }
		@LuaFunction public Object[] getEnergyRequired() { return beacon.getEnergyRequired(); }
		@LuaFunction public Object[] getEnergyStatus() {
			return new Object[]{ beacon.getEnergyStored(), beacon.getMaxEnergyStored(), "FE" };
		}
	}

	/** Legacy reactor controller API, pulse events and bundled automatic stabilization UI. */
	public static final class EnanReactorCorePeripheral implements CommonPeripheral,
		EnanReactorCoreTileEntity.ReactorEventListener {

		private final EnanReactorCoreTileEntity core;
		private final Set<IComputerAccess> computers = new HashSet<>();
		private final Map<IComputerAccess, List<String>> mountedPaths = new HashMap<>();

		private EnanReactorCorePeripheral(final EnanReactorCoreTileEntity core) {
			this.core = core;
		}

		@Override public String getType() { return "warpdriveEnanReactorCore"; }
		@Override public Object getTarget() { return core; }
		@Override public boolean equals(@Nullable final IPeripheral other) {
			return other instanceof EnanReactorCorePeripheral
				&& ((EnanReactorCorePeripheral) other).core == core;
		}

		@Override
		public void attach(final IComputerAccess computer) {
			if (!computers.add(computer)) return;
			if (computers.size() == 1) core.addEventListener(this);
			final List<String> paths = new ArrayList<>();
			mountedPaths.put(computer, paths);
			mount(computer, paths, "warpdrive", LUA_COMMON, "WarpDrive common programs");
			mount(computer, paths, "warpdrive/EnanReactorCore", LUA_ENAN_REACTOR,
				"WarpDrive reactor programs");
			mount(computer, paths, "startup", LUA_ENAN_REACTOR + "/startup",
				"WarpDrive reactor startup");
		}

		@Override
		public void detach(final IComputerAccess computer) {
			computers.remove(computer);
			final List<String> paths = mountedPaths.remove(computer);
			if (paths != null) for (final String path : paths) computer.unmount(path);
			if (computers.isEmpty()) core.removeEventListener(this);
		}

		private static void mount(final IComputerAccess computer, final List<String> paths,
		                          final String desiredPath, final String resourcePath,
		                          final String label) {
			try {
				final IMount mount = ComputerCraftAPI.createResourceMount(WarpDrive.MODID, resourcePath);
				if (mount == null) return;
				final String actualPath = computer.mount(desiredPath, mount, label);
				if (actualPath != null) paths.add(actualPath);
			} catch (final RuntimeException exception) {
				WarpDrive.logger.error("Unable to mount reactor resource {} at {}", resourcePath,
					desiredPath, exception);
			}
		}

		@LuaFunction public Object[] isInterfaced() {
			return new Object[]{ true, "CC:Tweaked peripheral available." };
		}

		@LuaFunction(mainThread = true)
		public Object[] name(final IArguments arguments) throws LuaException {
			if (arguments.count() > 1) throw new LuaException("expected zero or one name");
			return core.name(arguments.count() == 0 ? null : arguments.getString(0));
		}

		@LuaFunction(mainThread = true)
		public Object[] enable(final IArguments arguments) throws LuaException {
			if (arguments.count() > 1) throw new LuaException("expected zero or one boolean");
			return core.enable(arguments.count() == 0 ? null : arguments.getBoolean(0));
		}

		@LuaFunction public Object[] getAssemblyStatus() { return core.getAssemblyStatus(); }
		@LuaFunction public Object[] getLocalPosition() { return core.getLocalPosition(); }
		@LuaFunction public Object[] getEnergyRequired() { return core.getEnergyRequired(); }
		@LuaFunction public Object[] getEnergyStatus() { return core.getEnergyStatus(); }
		@LuaFunction public Object[] getInstabilities() { return core.getInstabilities(); }

		@LuaFunction(mainThread = true)
		public Object[] instabilityTarget(final IArguments arguments) throws LuaException {
			if (arguments.count() > 1) throw new LuaException("expected zero or one target");
			return core.instabilityTarget(arguments.count() == 0
				? null : arguments.getFiniteDouble(0));
		}

		@LuaFunction(mainThread = true)
		public Object[] outputMode(final IArguments arguments) throws LuaException {
			if (arguments.count() == 0) return core.outputMode(null, null);
			if (arguments.count() != 2) throw new LuaException("expected a mode and threshold");
			return core.outputMode(arguments.getString(0), arguments.getInt(1));
		}

		@LuaFunction(mainThread = true)
		public Object[] stabilizerEnergy(final IArguments arguments) throws LuaException {
			if (arguments.count() > 1) throw new LuaException("expected zero or one energy value");
			return core.stabilizerEnergy(arguments.count() == 0 ? null : arguments.getInt(0));
		}

		@LuaFunction public Object[] state() { return core.state(); }

		@Override
		public void queueEvent(final String eventName, final Object... arguments) {
			for (final IComputerAccess computer : new HashSet<>(computers)) {
				computer.queueEvent(eventName, arguments);
			}
		}

		private void invalidate() {
			for (final IComputerAccess computer : new ArrayList<>(computers)) detach(computer);
		}
	}

	/** Stabilizer face and one-medium energy API used by the bundled reactor program. */
	public static final class EnanReactorLaserPeripheral implements CommonPeripheral {
		private final EnanReactorLaserTileEntity laser;

		private EnanReactorLaserPeripheral(final EnanReactorLaserTileEntity laser) {
			this.laser = laser;
		}

		@Override public String getType() { return "warpdriveEnanReactorLaser"; }
		@Override public Object getTarget() { return laser; }
		@Override public boolean equals(@Nullable final IPeripheral other) {
			return other instanceof EnanReactorLaserPeripheral
				&& ((EnanReactorLaserPeripheral) other).laser == laser;
		}

		@LuaFunction public Object[] isInterfaced() {
			return new Object[]{ true, "CC:Tweaked peripheral available." };
		}
		@LuaFunction(mainThread = true)
		public Object[] name(final IArguments arguments) throws LuaException {
			if (arguments.count() > 1) throw new LuaException("expected zero or one name");
			return laser.name(arguments.count() == 0 ? null : arguments.getString(0));
		}
		@LuaFunction public Object[] getAssemblyStatus() { return laser.getAssemblyStatus(); }
		@LuaFunction public Object[] getLocalPosition() { return laser.getLocalPosition(); }
		@LuaFunction public Object[] getEnergyRequired() { return laser.getEnergyRequired(); }
		@LuaFunction public Object[] getEnergyStatus() { return laser.getEnergyStatus(); }
		@LuaFunction public Object[] laserMediumDirection() { return laser.laserMediumDirection(); }
		@LuaFunction public Object[] laserMediumCount() { return laser.laserMediumCount(); }
		@LuaFunction public Object[] side() { return laser.side(); }

		@LuaFunction(mainThread = true)
		public Object[] stabilize(final IArguments arguments) throws LuaException {
			if (arguments.count() > 1) throw new LuaException("expected zero or one energy value");
			return laser.stabilizeComputer(arguments.count() == 0 ? null : arguments.getInt(0));
		}
	}

	/** Legacy accelerator-control API shared by ordinary control points and injectors. */
	public static final class AcceleratorPeripheral implements CommonPeripheral {

		private final AcceleratorCoreTileEntity accelerator;
		private final Map<IComputerAccess, List<String>> mountedPaths = new HashMap<>();

		private AcceleratorPeripheral(final AcceleratorCoreTileEntity accelerator) {
			this.accelerator = accelerator;
		}

		@Override public String getType() { return "warpdriveAccelerator"; }
		@Override public Object getTarget() { return accelerator; }
		@Override public boolean equals(@Nullable final IPeripheral other) {
			return other instanceof AcceleratorPeripheral
			    && ((AcceleratorPeripheral) other).accelerator == accelerator;
		}

		@Override
		public void attach(final IComputerAccess computer) {
			if (mountedPaths.containsKey(computer)) return;
			final List<String> paths = new ArrayList<>();
			mountedPaths.put(computer, paths);
			mount(computer, paths, "warpdrive", LUA_COMMON, "WarpDrive common programs");
			mount(computer, paths, "warpdrive/Accelerator", LUA_ACCELERATOR,
				"WarpDrive accelerator programs");
			mount(computer, paths, "startup", LUA_ACCELERATOR + "/startup",
				"WarpDrive accelerator startup");
		}

		@Override
		public void detach(final IComputerAccess computer) {
			final List<String> paths = mountedPaths.remove(computer);
			if (paths != null) for (final String path : paths) computer.unmount(path);
		}

		private static void mount(final IComputerAccess computer, final List<String> paths,
		                          final String desiredPath, final String resourcePath,
		                          final String label) {
			try {
				final IMount mount = ComputerCraftAPI.createResourceMount(WarpDrive.MODID, resourcePath);
				if (mount == null) return;
				final String actualPath = computer.mount(desiredPath, mount, label);
				if (actualPath != null) paths.add(actualPath);
			} catch (final RuntimeException exception) {
				WarpDrive.logger.error("Unable to mount accelerator resource {} at {}", resourcePath,
					desiredPath, exception);
			}
		}

		@LuaFunction(mainThread = true)
		public Object[] enable(final IArguments arguments) throws LuaException {
			if (arguments.count() > 1) throw new LuaException("expected zero or one boolean");
			return accelerator.enable(arguments.count() == 0 ? null : arguments.getBoolean(0));
		}

		@LuaFunction public Object[] getControlPoints() { return accelerator.getControlPoints(); }
		@LuaFunction public Object[] getControlPointsCount() { return accelerator.getControlPointsCount(); }

		@LuaFunction
		public Object[] getControlPoint(final IArguments arguments) throws LuaException {
			if (arguments.count() != 1) throw new LuaException("expected control-point index");
			return accelerator.getControlPoint(arguments.getInt(0));
		}

		@LuaFunction public Object[] getParameters() { return accelerator.getParameters(); }
		@LuaFunction public Object[] getParametersControlChannels() {
			return accelerator.getParametersControlChannels();
		}

		@LuaFunction(mainThread = true)
		public Object[] parameter(final IArguments arguments) throws LuaException {
			if (arguments.count() < 1 || arguments.count() > 4) {
				throw new LuaException("expected channel and up to enabled, threshold, description");
			}
			return accelerator.parameter(arguments.getInt(0),
				arguments.count() > 1 ? arguments.optBoolean(1).orElse(null) : null,
				arguments.count() > 2 ? arguments.optFiniteDouble(2).orElse(null) : null,
				arguments.count() > 3 ? arguments.optString(3).orElse(null) : null);
		}

		@LuaFunction(mainThread = true)
		public Object[] injectionPeriod(final IArguments arguments) throws LuaException {
			if (arguments.count() > 1) throw new LuaException("expected zero or one period in seconds");
			return accelerator.injectionPeriod(
				arguments.count() == 0 ? null : arguments.getFiniteDouble(0));
		}

		@LuaFunction public Object[] state() { return accelerator.state(); }
		@LuaFunction public Object[] getEnergyRequired() { return accelerator.getEnergyRequired(); }
		@LuaFunction public Object[] getEnergyStatus() {
			return new Object[]{ accelerator.getEnergyStored(), accelerator.getMaxEnergyStored(), "FE" };
		}

		private void invalidate() {
			for (final IComputerAccess computer : new ArrayList<>(mountedPaths.keySet())) detach(computer);
		}
	}

	/** Legacy accelerator-control API shared by ordinary control points and injectors. */
	public static final class AcceleratorControlPointPeripheral implements CommonPeripheral {

		private final AcceleratorControlPointTileEntity controlPoint;

		private AcceleratorControlPointPeripheral(final AcceleratorControlPointTileEntity controlPoint) {
			this.controlPoint = controlPoint;
		}

		@Override
		public String getType() {
			return controlPoint instanceof ParticlesInjectorTileEntity
				? "warpdriveParticlesInjector" : "warpdriveAcceleratorControlPoint";
		}

		@Override public Object getTarget() { return controlPoint; }
		@Override public boolean equals(@Nullable final IPeripheral other) {
			return other instanceof AcceleratorControlPointPeripheral
			    && ((AcceleratorControlPointPeripheral) other).controlPoint == controlPoint;
		}

		@LuaFunction(mainThread = true)
		public Object[] state() { return controlPoint.state(); }

		@LuaFunction(mainThread = true)
		public Object[] enable(final IArguments arguments) throws LuaException {
			if (arguments.count() > 1) throw new LuaException("expected zero or one boolean");
			if (arguments.count() == 1) controlPoint.setEnabled(arguments.getBoolean(0));
			return new Object[]{ controlPoint.isEnabled() };
		}

		@LuaFunction(mainThread = true)
		public Object[] controlChannel(final IArguments arguments) throws LuaException {
			if (arguments.count() > 1) throw new LuaException("expected zero or one control channel");
			if (arguments.count() == 1) controlPoint.setControlChannel(arguments.getInt(0));
			return new Object[]{ controlPoint.getControlChannel() };
		}
	}

	/** Legacy addressed-chat receiver API and command event. */
	public static final class VirtualAssistantPeripheral implements CommonPeripheral,
		VirtualAssistantTileEntity.CommandListener {

		private final VirtualAssistantTileEntity assistant;
		private final Set<IComputerAccess> computers = new HashSet<>();

		private VirtualAssistantPeripheral(final VirtualAssistantTileEntity assistant) {
			this.assistant = assistant;
		}

		@Override public String getType() { return "warpdriveVirtualAssistant"; }
		@Override public Object getTarget() { return assistant; }
		@Override public boolean equals(@Nullable final IPeripheral other) {
			return other instanceof VirtualAssistantPeripheral
			    && ((VirtualAssistantPeripheral) other).assistant == assistant;
		}

		@Override
		public void attach(final IComputerAccess computer) {
			if (computers.add(computer) && computers.size() == 1) assistant.addCommandListener(this);
		}

		@Override
		public void detach(final IComputerAccess computer) {
			computers.remove(computer);
			if (computers.isEmpty()) assistant.removeCommandListener(this);
		}

		@LuaFunction(mainThread = true)
		public Object[] getLastCommand() { return assistant.getLastCommand(); }

		@LuaFunction(mainThread = true)
		public Object[] pullLastCommand() { return assistant.pullLastCommand(); }

		@LuaFunction(mainThread = true)
		public Object[] name(final IArguments arguments) throws LuaException {
			if (arguments.count() > 1) throw new LuaException("expected zero or one name");
			if (arguments.count() == 1) assistant.setAssistantName(arguments.getString(0));
			return new Object[]{ assistant.getAssistantName() };
		}

		@LuaFunction(mainThread = true)
		public Object[] enable(final IArguments arguments) throws LuaException {
			if (arguments.count() > 1) throw new LuaException("expected zero or one boolean");
			if (arguments.count() == 1) assistant.setEnabled(arguments.getBoolean(0));
			return new Object[]{ assistant.isEnabled() };
		}

		@LuaFunction
		public Object[] getEnergyRequired() {
			return new Object[]{ true,
				((cr0s.warpdrive.block.detection.VirtualAssistantBlock) assistant.getBlockState().getBlock())
					.getTier().getEnergyPerTick() };
		}

		@LuaFunction
		public Object[] getEnergyStatus() {
			return new Object[]{ assistant.getEnergyStored(), assistant.getMaxEnergyStored(), "FE" };
		}

		@LuaFunction
		public Object[] getLocalPosition() {
			return new Object[]{ assistant.getBlockPos().getX(), assistant.getBlockPos().getY(),
				assistant.getBlockPos().getZ() };
		}

		@LuaFunction
		public Object[] getTier() {
			final cr0s.warpdrive.block.detection.VirtualAssistantTier tier =
				((cr0s.warpdrive.block.detection.VirtualAssistantBlock) assistant.getBlockState().getBlock())
					.getTier();
			return new Object[]{ tier.getLegacyIndex(), tier.getName() };
		}

		@Override
		public void queueEvent(final String eventName, final Object... arguments) {
			for (final IComputerAccess computer : new HashSet<>(computers)) {
				computer.queueEvent(eventName, arguments);
			}
		}

		private void invalidate() {
			assistant.removeCommandListener(this);
			computers.clear();
		}
	}

	/** Legacy cloaking controls and bundled enable/disable utilities. */
	public static final class CloakingCorePeripheral implements CommonPeripheral {

		private final CloakingCoreTileEntity core;
		private final Map<IComputerAccess, List<String>> mountedPaths = new HashMap<>();

		private CloakingCorePeripheral(final CloakingCoreTileEntity core) { this.core = core; }
		@Override public String getType() { return "warpdriveCloakingCore"; }
		@Override public Object getTarget() { return core; }
		@Override public boolean equals(@Nullable final IPeripheral other) {
			return other instanceof CloakingCorePeripheral
			    && ((CloakingCorePeripheral) other).core == core;
		}

		@Override
		public void attach(final IComputerAccess computer) {
			if (mountedPaths.containsKey(computer)) return;
			final List<String> paths = new ArrayList<>();
			mountedPaths.put(computer, paths);
			mount(computer, paths, "warpdrive", LUA_COMMON, "WarpDrive common programs");
			mount(computer, paths, "warpdrive/CloakingCore", LUA_CLOAKING_CORE,
				"WarpDrive cloaking programs");
			mount(computer, paths, "cloak", LUA_CLOAKING_CORE + "/enable",
				"WarpDrive enable cloak");
			mount(computer, paths, "decloak", LUA_CLOAKING_CORE + "/disable",
				"WarpDrive disable cloak");
		}

		@Override
		public void detach(final IComputerAccess computer) {
			final List<String> paths = mountedPaths.remove(computer);
			if (paths != null) for (final String path : paths) computer.unmount(path);
		}

		private static void mount(final IComputerAccess computer, final List<String> paths,
		                          final String desiredPath, final String resourcePath,
		                          final String label) {
			try {
				final IMount mount = ComputerCraftAPI.createResourceMount(WarpDrive.MODID, resourcePath);
				if (mount == null) return;
				final String actualPath = computer.mount(desiredPath, mount, label);
				if (actualPath != null) paths.add(actualPath);
			} catch (final RuntimeException exception) {
				WarpDrive.logger.error("Unable to mount cloaking resource {} at {}", resourcePath,
					desiredPath, exception);
			}
		}

		@LuaFunction public Object[] isInterfaced() {
			return new Object[]{ true, "CC:Tweaked peripheral available." };
		}

		@LuaFunction(mainThread = true)
		public Object[] enable(final IArguments arguments) throws LuaException {
			if (arguments.count() > 1) throw new LuaException("expected zero or one boolean");
			return core.enable(arguments.count() == 0 ? null : arguments.getBoolean(0));
		}

		@LuaFunction public Object[] state() { return core.state(); }
		@LuaFunction public Object[] getAssemblyStatus() { return core.getAssemblyStatus(); }
		@LuaFunction public Object[] getEnergyRequired() { return core.getEnergyRequired(); }
		@LuaFunction public Object[] getEnergyStatus() {
			return new Object[]{ core.getEnergyStored(), core.getMaxEnergyStored(), "FE" };
		}

		private void invalidate() {
			for (final IComputerAccess computer : new ArrayList<>(mountedPaths.keySet())) detach(computer);
		}
	}

	/** Legacy delayed-scan radar API with bundled scan/ping utilities. */
	public static final class RadarPeripheral implements CommonPeripheral {

		private final RadarTileEntity radar;
		private final Map<IComputerAccess, List<String>> mountedPaths = new HashMap<>();

		private RadarPeripheral(final RadarTileEntity radar) { this.radar = radar; }
		@Override public String getType() { return "warpdriveRadar"; }
		@Override public Object getTarget() { return radar; }
		@Override public boolean equals(@Nullable final IPeripheral other) {
			return other instanceof RadarPeripheral && ((RadarPeripheral) other).radar == radar;
		}

		@Override
		public void attach(final IComputerAccess computer) {
			if (mountedPaths.containsKey(computer)) return;
			final List<String> paths = new ArrayList<>();
			mountedPaths.put(computer, paths);
			radar.setComputerConnected(true);
			mount(computer, paths, "warpdrive", LUA_COMMON, "WarpDrive common programs");
			mount(computer, paths, "warpdrive/Radar", LUA_RADAR, "WarpDrive radar programs");
			mount(computer, paths, "scan", LUA_RADAR + "/scan", "WarpDrive radar scan");
			mount(computer, paths, "ping", LUA_RADAR + "/ping", "WarpDrive radar ping");
		}

		@Override
		public void detach(final IComputerAccess computer) {
			final List<String> paths = mountedPaths.remove(computer);
			if (paths == null) return;
			radar.setComputerConnected(false);
			for (final String path : paths) computer.unmount(path);
		}

		private static void mount(final IComputerAccess computer, final List<String> paths,
		                          final String desiredPath, final String resourcePath,
		                          final String label) {
			try {
				final IMount mount = ComputerCraftAPI.createResourceMount(WarpDrive.MODID, resourcePath);
				if (mount == null) return;
				final String actualPath = computer.mount(desiredPath, mount, label);
				if (actualPath != null) paths.add(actualPath);
			} catch (final RuntimeException exception) {
				WarpDrive.logger.error("Unable to mount radar resource {} at {}", resourcePath,
					desiredPath, exception);
			}
		}

		@LuaFunction public Object[] getGlobalPosition() { return radar.getGlobalPosition(); }

		@LuaFunction(mainThread = true)
		public Object[] radius(final IArguments arguments) throws LuaException {
			if (arguments.count() > 1) throw new LuaException("expected zero or one radius");
			return radar.radius(arguments.count() == 0 ? null : arguments.getInt(0));
		}

		@LuaFunction public Object[] getEnergyRequired() { return radar.getEnergyRequired(); }
		@LuaFunction public Object[] getScanDuration() { return radar.getScanDuration(); }
		@LuaFunction public Object[] getEnergyStatus() {
			return new Object[]{ radar.getEnergyStored(), radar.getMaxEnergyStored(), "FE" };
		}
		@LuaFunction(mainThread = true) public Object[] start() { return radar.start(); }
		@Nullable @LuaFunction public Object[] getResults() { return radar.getResults(); }
		@LuaFunction public Object[] getResultsCount() { return radar.getResultsCount(); }

		@LuaFunction
		public Object[] getResult(final IArguments arguments) throws LuaException {
			if (arguments.count() != 1) throw new LuaException("expected result index");
			return radar.getResult(arguments.getInt(0));
		}

		private void invalidate() {
			for (final IComputerAccess computer : new ArrayList<>(mountedPaths.keySet())) detach(computer);
		}
	}

	/** Frequency and enable controls inherited by the legacy relay peripheral. */
	public static final class ForceFieldRelayPeripheral implements CommonPeripheral {

		private final ForceFieldRelayTileEntity relay;

		private ForceFieldRelayPeripheral(final ForceFieldRelayTileEntity relay) {
			this.relay = relay;
		}

		@Override
		public String getType() { return "warpdriveForceFieldRelay"; }

		@Override
		public Object getTarget() { return relay; }

		@Override
		public boolean equals(@Nullable final IPeripheral other) {
			return other instanceof ForceFieldRelayPeripheral
			    && ((ForceFieldRelayPeripheral) other).relay == relay;
		}

		@LuaFunction(mainThread = true)
		public Object[] beamFrequency(final IArguments arguments) throws LuaException {
			if (arguments.count() > 1) throw new LuaException("expected zero or one beam frequency");
			return relay.setOrGetBeamFrequency(arguments.count() == 0 ? null : arguments.getInt(0));
		}

		@LuaFunction(mainThread = true)
		public Object[] enable(final IArguments arguments) throws LuaException {
			if (arguments.count() > 1) throw new LuaException("expected zero or one boolean");
			return relay.enable(arguments.count() == 0 ? null : arguments.getBoolean(0));
		}

		@LuaFunction(mainThread = true)
		public Object[] state() { return relay.state(); }

		@LuaFunction(mainThread = true)
		public Object[] getEnergyRequired() { return new Object[]{ false, "No energy consumption" }; }
	}
}
