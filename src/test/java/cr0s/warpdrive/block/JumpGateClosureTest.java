package cr0s.warpdrive.block;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Guards the deliberate closure of the unregistered 1.12 jump-gate WIP. */
public class JumpGateClosureTest {

	private static String source(final String... path) throws IOException {
		Path source = Paths.get("src", "main", "java");
		for (final String element : path) source = source.resolve(element);
		return new String(Files.readAllBytes(source),
			StandardCharsets.UTF_8);
	}

	@Test
	public void noInventedJumpGateRegistryIdentityOrAssetsAreShipped() throws IOException {
		final String registration = source("cr0s", "warpdrive", "data", "Registration.java");
		assertFalse(registration.contains("JUMP_GATE"));
		assertFalse(registration.contains("jump_gate_core"));
		for (final String tree : new String[]{ "blockstates", "models/block", "models/item" }) {
			final Path candidate = Paths.get("src", "main", "resources", "assets", "warpdrive",
				tree, "jump_gate_core.json");
			assertFalse("Unexpected invented jump-gate asset " + candidate, Files.exists(candidate));
		}
		assertFalse(Files.exists(Paths.get("src", "main", "resources", "data", "warpdrive",
			"recipes", "jump_gate_core.json")));
	}

	@Test
	public void dormantApiFailsExplicitlyInsteadOfPretendingToEngage() throws IOException {
		final String shipCore = source("cr0s", "warpdrive", "block", "ShipCoreTileEntity.java");
		assertTrue(shipCore.contains("if (\"GATE\".equals(command))"));
		assertTrue(shipCore.contains("legacy gate-core refactor was never registered or completed"));
		final String regionTypes = source("cr0s", "warpdrive", "data", "GlobalRegionType.java");
		assertTrue("The persisted/API enum identity should remain reserved",
			regionTypes.contains("JUMP_GATE(\"jump_gate\""));
	}
}
