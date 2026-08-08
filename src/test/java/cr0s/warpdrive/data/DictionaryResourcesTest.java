package cr0s.warpdrive.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Static contracts for the datapack replacement of the 1.12.2 Dictionary. */
public class DictionaryResourcesTest {

	private static final Path ROOT = Paths.get("src", "main");
	private static final Path RESOURCES = ROOT.resolve("resources");
	private static final Path TAGS = RESOURCES.resolve(Paths.get("data", "warpdrive", "tags"));

	@Test
	public void playerAndVacuumDefaultsMatchThePortedBehaviour() throws IOException {
		assertEquals(setOf(
			"warpdrive:warp_armor_basic_helmet",
			"warpdrive:warp_armor_advanced_helmet",
			"warpdrive:warp_armor_superior_helmet"),
			values("items/breathing_helmets.json"));
		assertEquals(12, values("items/fly_in_space.json").size());
		assertEquals(setOf(
			"warpdrive:warp_armor_advanced_leggings",
			"warpdrive:warp_armor_advanced_boots",
			"warpdrive:warp_armor_superior_leggings",
			"warpdrive:warp_armor_superior_boots"),
			values("items/no_fall_damage.json"));

		final Set<String> airless = values("entity_types/living_without_air.json");
		assertEquals(12, airless.size());
		assertTrue(airless.contains("minecraft:armor_stand"));
		assertTrue(airless.contains("minecraft:creeper"));
		assertTrue(airless.contains("minecraft:guardian"));
		assertTrue(airless.contains("minecraft:zombified_piglin"));
		assertFalse(airless.contains("minecraft:zombie_pigman"));
	}

	@Test
	public void miningTransportAndShipDefaultsAreSeparated() throws IOException {
		assertEquals(setOf("minecraft:barrier", "minecraft:bedrock"),
			values("blocks/mining_skip.json"));
		final Set<String> stop = values("blocks/mining_stop.json");
		assertTrue(stop.contains("minecraft:command_block"));
		assertTrue(stop.contains("warpdrive:bedrock_glass"));
		assertFalse(stop.contains("minecraft:bedrock"));
		assertEquals(setOf("minecraft:bedrock", "warpdrive:bedrock_glass"),
			values("blocks/no_blink.json"));

		final Set<String> anchors = values("blocks/ship_movement/anchors.json");
		assertTrue(anchors.contains("minecraft:bedrock"));
		assertTrue(anchors.contains("warpdrive:bedrock_glass"));
		assertTrue(values("blocks/ship_movement/left_behind.json")
			.contains("warpdrive:gas"));
		assertTrue(values("blocks/ship_movement/no_mass.json")
			.contains("#minecraft:leaves"));
		assertTrue(values("blocks/ship_movement/place_earliest.json")
			.contains("#warpdrive:hulls/basic/plain"));
		assertTrue(values("blocks/ship_movement/place_later.json")
			.contains("minecraft:redstone_wire"));
	}

	@Test
	public void everyWarpDriveTagReferenceAndRegistryEntryResolves() throws IOException {
		validateTagTree("blocks", "blockstates");
		validateTagTree("items", Paths.get("models", "item").toString());
		validateTagTree("entity_types", null);
	}

	@Test
	public void everyLiveConsumerUsesTheSharedTags() throws IOException {
		assertSource("event/BreathingManager.java", "WarpDriveTags.BREATHING_HELMETS",
			"WarpDriveTags.LIVING_WITHOUT_AIR");
		assertSource("event/GravityHandler.java", "WarpDriveTags.FLY_IN_SPACE");
		assertSource("event/FallDamageHandler.java", "WarpDriveTags.NO_FALL_DAMAGE");
		assertSource("block/collection/MiningLaserTileEntity.java", "WarpDriveTags.MINING",
			"WarpDriveTags.MINING_SKIP", "WarpDriveTags.MINING_STOP");
		assertSource("block/movement/TransporterCoreTileEntity.java", "WarpDriveTags.NO_BLINK");
		final String transporter = source("block/movement/TransporterCoreTileEntity.java");
		assertFalse("NoBlink must not alias mining-stop again",
			transporter.contains("new ResourceLocation(WarpDrive.MODID, \"mining_stop\")"));
		assertSource("block/forcefield/ForceFieldProjectorTileEntity.java",
			"WarpDriveTags.NO_CAMOUFLAGE");
		assertSource("ship/ShipScanner.java", "WarpDriveTags.SHIP_LEFT_BEHIND",
			"WarpDriveTags.SHIP_NO_MASS", "WarpDriveTags.PLACE_EARLIEST");
		assertSource("ship/WarpEngine.java", "WarpDriveTags.SHIP_EXPANDABLE",
			"WarpDriveTags.SHIP_ENTITY_ANCHORS", "WarpDriveTags.SHIP_ENTITIES_LEFT_BEHIND");
		assertSource("ship/ShipSchematic.java", "WarpDriveTags.SHIP_NO_MASS",
			"WarpDriveTags.PLACE_EARLIEST", "root.putInt(\"Mass\", mass)");
		assertSource("data/CloakManager.java", "WarpDriveTags.NO_REVEAL");
	}

	private static void validateTagTree(final String registry, final String assetDirectory)
		throws IOException {
		final Path root = TAGS.resolve(registry);
		try (java.util.stream.Stream<Path> paths = Files.walk(root)) {
			for (final Path file : (Iterable<Path>) paths.filter(Files::isRegularFile)::iterator) {
				for (final String value : readValues(file)) {
					if (value.startsWith("#warpdrive:")) {
						final String path = value.substring("#warpdrive:".length());
						assertTrue("Missing tag " + value + " referenced by " + file,
							Files.isRegularFile(root.resolve(path + ".json")));
					} else if (assetDirectory != null && value.startsWith("warpdrive:")) {
						final String id = value.substring("warpdrive:".length());
						assertTrue("Missing registry asset for " + value + " in " + file,
							Files.isRegularFile(RESOURCES.resolve(Paths.get(
								"assets", "warpdrive", assetDirectory, id + ".json"))));
					}
				}
			}
		}
	}

	private static Set<String> values(final String relative) throws IOException {
		return readValues(TAGS.resolve(relative));
	}

	private static Set<String> readValues(final Path path) throws IOException {
		final JsonObject json = new JsonParser().parse(new String(
			Files.readAllBytes(path), StandardCharsets.UTF_8)).getAsJsonObject();
		assertTrue("Tag must be appendable: " + path, json.has("replace")
			&& !json.get("replace").getAsBoolean());
		final Set<String> values = new HashSet<>();
		for (final JsonElement value : json.getAsJsonArray("values")) {
			values.add(value.isJsonPrimitive()
				? value.getAsString() : value.getAsJsonObject().get("id").getAsString());
		}
		return values;
	}

	private static void assertSource(final String relative, final String... needles)
		throws IOException {
		final String source = source(relative);
		for (final String needle : needles) {
			assertTrue("Missing " + needle + " from " + relative, source.contains(needle));
		}
	}

	private static String source(final String relative) throws IOException {
		return new String(Files.readAllBytes(ROOT.resolve(Paths.get(
			"java", "cr0s", "warpdrive", relative))), StandardCharsets.UTF_8);
	}

	private static Set<String> setOf(final String... values) {
		return new HashSet<>(Arrays.asList(values));
	}
}
