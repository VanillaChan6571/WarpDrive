package cr0s.warpdrive.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.vector.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.Random;

/**
 * WarpDrive's procedural coloured starfield, ported from 1.12.2 RenderSpaceSky.
 *
 * This is what produced the multicoloured stars in the original mod - they are generated, not a
 * texture. Vanilla's starfield is plain white and much sparser.
 *
 * The geometry is built once into a vertex list and replayed, because generating 20k stars every
 * frame would be wasteful. The fixed seed (10842) is deliberate: it is the original's, so the sky
 * matches 1.12.2 star for star, and it keeps the field stable between frames and sessions.
 */
final class StarFieldRenderer {

	private static final long STAR_SEED = 10842L;
	private static final double RENDER_RANGE = 10.0D;

	/** Flattened quads: 4 vertices per star, each x/y/z/r/g/b. */
	private float[] vertices;
	private int starCount;

	/** Build the field once; identical every run thanks to the fixed seed. */
	private void generate() {
		final Random random = new Random(STAR_SEED);
		final boolean hasMoreStars = random.nextBoolean() || random.nextBoolean();
		final int stars = hasMoreStars ? 20000 : 2000;

		vertices = new float[stars * 4 * 6];
		int cursor = 0;

		for (int index = 0; index < stars; index++) {
			double randomX;
			double randomY;
			double randomZ;
			double lengthSquared;
			// Rejection-sample a thin spherical shell so stars sit on a sphere, not in a cube
			do {
				randomX = random.nextDouble() * 2.0D - 1.0D;
				randomY = random.nextDouble() * 2.0D - 1.0D;
				randomZ = random.nextDouble() * 2.0D - 1.0D;
				lengthSquared = randomX * randomX + randomY * randomY + randomZ * randomZ;
			} while (lengthSquared >= 1.0D || lengthSquared <= 0.90D);

			final double renderSize = 0.020F + 0.0025F * Math.log(1.1D - random.nextDouble());

			final double normalise = 1.0D / Math.sqrt(lengthSquared);
			randomX *= normalise;
			randomY *= normalise;
			randomZ *= normalise;

			final double x0 = randomX * RENDER_RANGE;
			final double y0 = randomY * RENDER_RANGE;
			final double z0 = randomZ * RENDER_RANGE;

			final double angleH = Math.atan2(randomX, randomZ);
			final double angleV = Math.atan2(Math.sqrt(randomX * randomX + randomZ * randomZ), randomY);
			final double angleS = random.nextDouble() * Math.PI * 2.0D;

			final int rgb = starColour(random);
			final float red = ((rgb >> 16) & 0xFF) / 255.0F;
			final float green = ((rgb >> 8) & 0xFF) / 255.0F;
			final float blue = (rgb & 0xFF) / 255.0F;

			final double sinH = Math.sin(angleH);
			final double cosH = Math.cos(angleH);
			final double sinV = Math.sin(angleV);
			final double cosV = Math.cos(angleV);
			final double sinS = Math.sin(angleS);
			final double cosS = Math.cos(angleS);

			for (int corner = 0; corner < 4; corner++) {
				final double offset1 = ((corner & 2) - 1) * renderSize;
				final double offset2 = ((corner + 1 & 2) - 1) * renderSize;
				final double valV = offset1 * cosS - offset2 * sinS;
				final double valH = offset2 * cosS + offset1 * sinS;
				final double y1 = valV * sinV;
				final double valD = -valV * cosV;
				final double x1 = valD * sinH - valH * cosH;
				final double z1 = valH * sinH + valD * cosH;

				vertices[cursor++] = (float) (x0 + x1);
				vertices[cursor++] = (float) (y0 + y1);
				vertices[cursor++] = (float) (z0 + z1);
				vertices[cursor++] = red;
				vertices[cursor++] = green;
				vertices[cursor++] = blue;
			}
		}
		starCount = stars;
	}

	void render(final MatrixStack matrixStack, final float brightness) {
		if (vertices == null) {
			generate();
		}
		if (brightness <= 0.0F) {
			return;
		}

		final Tessellator tessellator = Tessellator.getInstance();
		final BufferBuilder buffer = tessellator.getBuilder();
		final Matrix4f matrix = matrixStack.last().pose();

		buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
		int cursor = 0;
		for (int index = 0; index < starCount * 4; index++) {
			final float x = vertices[cursor++];
			final float y = vertices[cursor++];
			final float z = vertices[cursor++];
			final float red = vertices[cursor++] * brightness;
			final float green = vertices[cursor++] * brightness;
			final float blue = vertices[cursor++] * brightness;
			buffer.vertex(matrix, x, y, z).color(red, green, blue, 1.0F).endVertex();
		}
		tessellator.end();
	}

	/**
	 * Colour distribution loosely following the Hertzsprung-Russell diagram, copied from 1.12.2:
	 * 8% blue, 16% white, 21% yellow-white, 22% yellow, 25% orange, remainder red giants.
	 */
	private static int starColour(final Random random) {
		final double colourType = random.nextDouble();
		final float hue;
		final float saturation;
		float brightness = 1.0F - 0.8F * random.nextFloat();   // distance effect

		if (colourType <= 0.08D) {                 // light blue, young star
			hue = 0.48F + 0.08F * random.nextFloat();
			saturation = 0.18F + 0.22F * random.nextFloat();
		} else if (colourType <= 0.24D) {          // pure white, early age
			hue = 0.126F + 0.040F * random.nextFloat();
			saturation = 0.00F + 0.15F * random.nextFloat();
			brightness *= 0.95F;
		} else if (colourType <= 0.45D) {          // yellow white
			hue = 0.126F + 0.040F * random.nextFloat();
			saturation = 0.15F + 0.15F * random.nextFloat();
			brightness *= 0.90F;
		} else if (colourType <= 0.67D) {          // yellow
			hue = 0.126F + 0.040F * random.nextFloat();
			saturation = 0.80F + 0.15F * random.nextFloat();
			brightness *= random.nextInt(3) == 1 ? 0.90F : 0.85F;
		} else if (colourType <= 0.92D) {          // orange
			hue = 0.055F + 0.055F * random.nextFloat();
			saturation = 0.85F + 0.15F * random.nextFloat();
			brightness *= random.nextInt(3) == 1 ? 0.90F : 0.80F;
		} else {                                   // red, mostly giants
			hue = 0.0F + 0.055F * random.nextFloat();
			saturation = 0.90F + 0.10F * random.nextFloat();
			brightness *= 0.75F;
		}

		return Color.HSBtoRGB(hue, saturation, brightness);
	}
}
