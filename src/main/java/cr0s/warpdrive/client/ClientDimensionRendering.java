package cr0s.warpdrive.client;

import cr0s.warpdrive.WarpDrive;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.minecraft.client.world.DimensionRenderInfo;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import java.lang.reflect.Field;

/** Client-only registration for WarpDrive's dimension effects. */
public final class ClientDimensionRendering {

	private static final ResourceLocation SPACE_EFFECTS = new ResourceLocation(WarpDrive.MODID, "space");
	private static final ResourceLocation HYPERSPACE_EFFECTS = new ResourceLocation(WarpDrive.MODID, "hyperspace");

	private ClientDimensionRendering() {
	}

	@SuppressWarnings("unchecked")
	public static void register() {
		try {
			// 1.16.5 hard-codes this registry as a private map. Forge's reflection helper
			// remaps the SRG name in production, avoiding an access transformer solely for it.
			final Field effectsField = ObfuscationReflectionHelper.findField(
				DimensionRenderInfo.class, "field_239208_a_");
			final Object2ObjectMap<ResourceLocation, DimensionRenderInfo> effects =
				(Object2ObjectMap<ResourceLocation, DimensionRenderInfo>) effectsField.get(null);

			// Star brightness values come from the 1.12.2 celestialObjects-default.xml:
			// space used starBrightnessBase 0.9, hyperspace 0.2 against a red background.
			final SpaceSkyRenderer spaceSky = new SpaceSkyRenderer("skybox-small_blue", 1.0F, 1.0F, 1.0F);
			spaceSky.setStarBrightness(0.9F);
			effects.put(SPACE_EFFECTS, new WarpDriveDimensionRenderInfo(
				new Vector3d(0.0D, 0.0D, 0.0D), spaceSky));

			final SpaceSkyRenderer hyperSky = new SpaceSkyRenderer("skybox-small_purple", 0.85F, 0.38F, 0.48F);
			hyperSky.setStarBrightness(0.2F);
			effects.put(HYPERSPACE_EFFECTS, new WarpDriveDimensionRenderInfo(
				new Vector3d(0.12D, 0.0D, 0.02D), hyperSky));
		} catch (ReflectiveOperationException exception) {
			throw new IllegalStateException("Unable to register WarpDrive dimension rendering", exception);
		}
	}

	private static final class WarpDriveDimensionRenderInfo extends DimensionRenderInfo {
		private final Vector3d fogColor;

		private WarpDriveDimensionRenderInfo(final Vector3d fogColor, final SpaceSkyRenderer skyRenderer) {
			super(Float.NaN, false, FogType.NONE, false, false);
			this.fogColor = fogColor;
			setSkyRenderHandler(skyRenderer);
		}

		@Override
		public Vector3d getBrightnessDependentFogColor(final Vector3d ignored, final float daylight) {
			return fogColor;
		}

		@Override
		public boolean isFoggyAt(final int x, final int z) {
			return false;
		}

		@Override
		public float[] getSunriseColor(final float timeOfDay, final float partialTicks) {
			return null;
		}
	}
}
