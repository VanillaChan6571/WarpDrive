package cr0s.warpdrive.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import cr0s.warpdrive.WarpDrive;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraftforge.client.ISkyRenderHandler;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

/** Renders one of the original WarpDrive six-face skyboxes around the camera. */
final class SpaceSkyRenderer implements ISkyRenderHandler {

	private static final float SKY_DISTANCE = 100.0F;
	private static final String[] FACE_NAMES = {
		"bottom", "front", "back", "top", "right", "left"
	};
	private static final float[][][] FACE_VERTICES = {
		{
			{-1.0F, -1.0F, -1.0F}, {-1.0F, -1.0F,  1.0F},
			{ 1.0F, -1.0F,  1.0F}, { 1.0F, -1.0F, -1.0F}
		},
		{
			{-1.0F,  1.0F, -1.0F}, {-1.0F, -1.0F, -1.0F},
			{ 1.0F, -1.0F, -1.0F}, { 1.0F,  1.0F, -1.0F}
		},
		{
			{ 1.0F,  1.0F,  1.0F}, { 1.0F, -1.0F,  1.0F},
			{-1.0F, -1.0F,  1.0F}, {-1.0F,  1.0F,  1.0F}
		},
		{
			{-1.0F,  1.0F, -1.0F}, { 1.0F,  1.0F, -1.0F},
			{ 1.0F,  1.0F,  1.0F}, {-1.0F,  1.0F,  1.0F}
		},
		{
			{ 1.0F,  1.0F, -1.0F}, { 1.0F, -1.0F, -1.0F},
			{ 1.0F, -1.0F,  1.0F}, { 1.0F,  1.0F,  1.0F}
		},
		{
			{-1.0F,  1.0F,  1.0F}, {-1.0F, -1.0F,  1.0F},
			{-1.0F, -1.0F, -1.0F}, {-1.0F,  1.0F, -1.0F}
		}
	};

	/**
	 * Per-face UVs, taken from 1.12.2 RenderSpaceSky.renderSkyBox. Five faces share the same
	 * winding, but `top` is rotated a quarter turn - using one UV set for all six leaves a visible
	 * discontinuity where the top meets the sides.
	 */
	private static final float[][][] FACE_UVS = {
		{ {0, 0}, {0, 1}, {1, 1}, {1, 0} },   // bottom
		{ {0, 0}, {0, 1}, {1, 1}, {1, 0} },   // front
		{ {0, 0}, {0, 1}, {1, 1}, {1, 0} },   // back
		{ {0, 1}, {1, 1}, {1, 0}, {0, 0} },   // top - rotated
		{ {0, 0}, {0, 1}, {1, 1}, {1, 0} },   // right
		{ {0, 0}, {0, 1}, {1, 1}, {1, 0} }    // left
	};

	private final ResourceLocation[] textures = new ResourceLocation[FACE_NAMES.length];
	private final int red;
	private final int green;
	private final int blue;

	/** WarpDrive's own coloured starfield, drawn over the skybox as 1.12.2 did. */
	private final StarFieldRenderer starField = new StarFieldRenderer();
	private float starBrightness = 0.9F;

	void setStarBrightness(final float value) {
		starBrightness = value;
	}

	SpaceSkyRenderer(final String skyboxName, final float red, final float green, final float blue) {
		for (int index = 0; index < FACE_NAMES.length; index++) {
			textures[index] = new ResourceLocation(WarpDrive.MODID,
				"textures/celestial/" + skyboxName + "/" + FACE_NAMES[index] + ".png");
		}
		this.red = Math.round(255.0F * red);
		this.green = Math.round(255.0F * green);
		this.blue = Math.round(255.0F * blue);
	}

	@Override
	public void render(final int ticks, final float partialTicks, final MatrixStack matrixStack,
	                   final ClientWorld world, final Minecraft minecraft) {
		RenderSystem.disableFog();
		RenderSystem.disableAlphaTest();
		RenderSystem.enableTexture();
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.depthMask(false);
		RenderSystem.disableCull();

		final Tessellator tessellator = Tessellator.getInstance();
		final BufferBuilder buffer = tessellator.getBuilder();
		final Matrix4f matrix = matrixStack.last().pose();
		for (int face = 0; face < textures.length; face++) {
			minecraft.getTextureManager().bind(textures[face]);
			// These legacy skyboxes are only 256x256, so linear filtering avoids each source
			// pixel becoming a conspicuous square. It must be paired with CLAMP_TO_EDGE: the
			// default GL_REPEAT wrap makes the filter sample across the texture border and draws
			// a hard seam along every cube edge, which is what made the sky look boxy.
			minecraft.getTextureManager().getTexture(textures[face]).setFilter(true, false);
			RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
			RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);

			buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
			for (int corner = 0; corner < 4; corner++) {
				vertex(buffer, matrix, FACE_VERTICES[face][corner],
					FACE_UVS[face][corner][0], FACE_UVS[face][corner][1]);
			}
			tessellator.end();
		}

		// Stars go on top of the skybox, untextured and additive-ish, same as 1.12.2
		RenderSystem.disableTexture();
		starField.render(matrixStack, starBrightness);
		RenderSystem.enableTexture();

		RenderSystem.enableCull();
		RenderSystem.depthMask(true);
		RenderSystem.disableBlend();
		RenderSystem.enableAlphaTest();
		RenderSystem.enableTexture();
		// disableFog() at the top would otherwise leave fog off for the rest of the frame
		RenderSystem.enableFog();
	}

	private void vertex(final BufferBuilder buffer, final Matrix4f matrix, final float[] position,
	                    final float u, final float v) {
		buffer.vertex(matrix,
			position[0] * SKY_DISTANCE,
			position[1] * SKY_DISTANCE,
			position[2] * SKY_DISTANCE)
			.uv(u, v).color(red, green, blue, 255).endVertex();
	}
}
