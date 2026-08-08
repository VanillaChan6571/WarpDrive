package cr0s.warpdrive.compat;

import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertTrue;

/** Contracts for the port of the legacy TileEntityAbstractInterfaced computer surface. */
public class ComputerCraftCompatibilityTest {

	private static final List<String> COMMON_METHODS = Arrays.asList(
		"isInterfaced", "getLocalPosition", "getTier", "getUpgrades", "getVersion");

	@Test
	public void defaultInterfaceMethodsAreVisibleAndAnnotatedOnFixedPeripherals() throws Exception {
		final List<Class<?>> peripherals = new ArrayList<>();
		for (final String nestedName : Arrays.asList(
			"AirGeneratorPeripheral", "EnergyStoragePeripheral", "SirenPeripheral",
			"ShipControllerPeripheral", "ShipScannerPeripheral", "CameraPeripheral",
			"MonitorPeripheral", "ChunkLoaderPeripheral", "LaserPeripheral",
			"LaserCameraPeripheral", "MiningLaserPeripheral", "LaserTreeFarmPeripheral",
			"LiftPeripheral", "SpeakerPeripheral", "ForceFieldProjectorPeripheral",
			"ForceFieldRelayPeripheral", "WeaponControllerPeripheral",
			"EnvironmentalSensorPeripheral", "BiometricScannerPeripheral",
			"SecurityStationPeripheral", "TransporterCorePeripheral",
			"TransporterBeaconPeripheral", "EnanReactorCorePeripheral",
			"EnanReactorLaserPeripheral", "AcceleratorPeripheral",
			"AcceleratorControlPointPeripheral", "VirtualAssistantPeripheral",
			"CloakingCorePeripheral", "RadarPeripheral")) {
			peripherals.add(Class.forName(
				"cr0s.warpdrive.compat.ComputerCraftCompat$" + nestedName));
		}

		for (final Class<?> peripheral : peripherals) {
			for (final String name : COMMON_METHODS) {
				final Method method = peripheral.getMethod(name);
				assertTrue(peripheral.getSimpleName() + '.' + name + " must be a Lua method",
					Arrays.stream(method.getAnnotations()).anyMatch(annotation ->
						"dan200.computercraft.api.lua.LuaFunction"
							.equals(annotation.annotationType().getName())));
			}
		}
	}

	@Test
	public void ccTweakedGeneratorDiscoversTheInheritedCommonMethods() throws Exception {
		final Class<?> peripheralMethod =
			Class.forName("dan200.computercraft.core.asm.PeripheralMethod");
		final Field generatorField = peripheralMethod.getField("GENERATOR");
		final Object generator = generatorField.get(null);
		final Method getMethods = generator.getClass().getMethod("getMethods", Class.class);
		final List<?> namedMethods = (List<?>) getMethods.invoke(generator,
			Class.forName("cr0s.warpdrive.compat.ComputerCraftCompat$SirenPeripheral"));
		final Set<String> names = new HashSet<>();
		for (final Object namedMethod : namedMethods) {
			names.add(String.valueOf(namedMethod.getClass().getMethod("getName").invoke(namedMethod)));
		}
		assertTrue("CC:Tweaked did not discover all inherited common methods: " + names,
			names.containsAll(COMMON_METHODS));
	}

	@Test
	public void everyLegacyMachineGapHasAnAttachmentBranchAndLegacyType() throws IOException {
		final String source = new String(Files.readAllBytes(Paths.get("src", "main", "java", "cr0s",
			"warpdrive", "compat", "ComputerCraftCompat.java")), StandardCharsets.UTF_8);
		for (final String tile : Arrays.asList(
			"ShipControllerTileEntity", "AirGeneratorTileEntity", "CapacitorTileEntity",
			"LaserMediumTileEntity", "SirenTileEntity")) {
			assertTrue("Missing capability attachment for " + tile,
				source.contains("event.getObject() instanceof " + tile));
		}
		for (final String type : Arrays.asList(
			"warpdriveShipController", "warpdriveShipCore", "warpdriveAirGenerator",
			"warpdriveCapacitor", "warpdriveLaserMedium", "warpdriveSiren")) {
			assertTrue("Missing legacy peripheral type " + type, source.contains('"' + type + '"'));
		}
		assertTrue("The bundled controller startup must be mounted",
			source.contains("lua.ComputerCraft/warpdriveShipController/startup"));
	}
}
