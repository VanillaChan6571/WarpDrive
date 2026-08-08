package cr0s.warpdrive;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

public class WarpDriveVersionTest {

	@Test
	public void computerVersionIgnoresTheMinecraftArtifactPrefixAndPrereleaseSuffix() {
		assertArrayEquals(new Integer[]{ 2, 0, 0 },
			WarpDrive.parseVersionNumbers("1.16.5-2.0.0-alpha.2.SNAPSHOT"));
		assertArrayEquals(new Integer[]{ 2, 3, 4 },
			WarpDrive.parseVersionNumbers("2.3.4"));
		assertArrayEquals(new Integer[]{ 0, 0, 0 },
			WarpDrive.parseVersionNumbers("@version@"));
	}
}
