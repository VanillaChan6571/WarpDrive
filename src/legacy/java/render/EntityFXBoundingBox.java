package cr0s.warpdrive.render;

import cr0s.warpdrive.data.Vector3;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class EntityFXBoundingBox extends Particle {
	
	private static final ResourceLocation TEXTURE = new ResourceLocation("warpdrive", "textures/particle/bounding_box.png");
	
	private final Vector3 min;
	private final Vector3 max;
	
	public EntityFXBoundingBox(final World world, final Vector3 position, final Vector3 min, final Vector3 max,
	                           final float red, final float green, final float blue, final int age) {
		super(world, position.x, position.y, position.z, 0.0D, 0.0D, 0.0D);
		
		this.setRBGColorF(red, green, blue);
		this.setSize(0.02F, 0.02F);
		this.canCollide = false;
		this.motionX = 0.0D;
		this.motionY = 0.0D;
		this.motionZ = 0.0D;
		this.min = min;
		this.max = max;
		this.particleMaxAge = age;
	}
	
	@Override
	public void onUpdate() {
		prevPosX = posX;
		prevPosY = posY;
		prevPosZ = posZ;
		
		if (particleAge++ >= particleMaxAge) {
			setExpired();
		}
	}
	
	@Override
	public void renderParticle(final BufferBuilder vertexBuffer, final Entity entityIn, final float partialTick,
	                           final float rotationX, final float rotationZ, final float rotationYZ, final float rotationXY, final float rotationXZ) {
		GlStateManager.pushMatrix();
		
		// final float rot = (world.provider.getWorldTime() % (360 / rotationSpeed) + partialTick) * rotationSpeed;
		
        // alpha starts at 50%, vanishing to 10% during last ticks
		float alpha = 0.45F;
		if (particleMaxAge - particleAge <= 2) {
			alpha = 0.35F; // 0.45F - (1 - (particleMaxAge - particleAge)) * 0.35F;
		} else if (particleAge < 1) {
			alpha = 0.10F;
		}
		
		// get brightness factors
		final int brightnessForRender = getBrightnessForRender(partialTick);
		final int brightnessHigh = brightnessForRender >> 16 & 65535;
		final int brightnessLow  = Math.max(240, brightnessForRender & 65535);
		
		// final double relativeTime = world.getTotalWorldTime() + partialTick;
		// final double uOffset = (float) (-relativeTime * 0.3D - MathHelper.floor(-relativeTime * 0.15D));
		// final double vOffset = (float) (-relativeTime * 0.2D - MathHelper.floor(-relativeTime * 0.1D));
		
		// box position
		final double relativeTime = Math.abs(world.getTotalWorldTime() % 64L + partialTick) / 64.0D;
		final double sizeOffset = 0.01F * (1.0F + (float) Math.sin(relativeTime * Math.PI * 2));
		final double xMin = min.x - posX - sizeOffset;
		final double xMax = max.x - posX + sizeOffset;
		final double yMin = min.y - posY - sizeOffset;
		final double yMax = max.y - posY + sizeOffset;
		final double zMin = min.z - posZ - sizeOffset;
		final double zMax = max.z - posZ + sizeOffset;
		
		// texture coordinates
		final double uvScale = 1.0D;
		final double uv_xMin = xMin / uvScale + 0.5D;
		final double uv_xMax = xMax / uvScale + 0.5D;
		final double uv_yMin = yMin / uvScale + 0.5D;
		final double uv_yMax = yMax / uvScale + 0.5D;
		final double uv_zMin = zMin / uvScale + 0.5D;
		final double uv_zMax = zMax / uvScale + 0.5D;
		
		Minecraft.getMinecraft().getTextureManager().bindTexture(TEXTURE);
		GlStateManager.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
		GlStateManager.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
		GlStateManager.disableCull();
		
		GlStateManager.enableBlend();
		GlStateManager.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE);
		GlStateManager.depthMask(false);
		
		final float xx = (float)(prevPosX + (posX - prevPosX) * partialTick - interpPosX);
		final float yy = (float)(prevPosY + (posY - prevPosY) * partialTick - interpPosY);
		final float zz = (float)(prevPosZ + (posZ - prevPosZ) * partialTick - interpPosZ);
		GlStateManager.translate(xx, yy, zz);
		
		final Tessellator tessellator = Tessellator.getInstance();
		
		// x planes
		vertexBuffer.begin(GL11.GL_QUADS, DefaultVertexFormats.PARTICLE_POSITION_TEX_COLOR_LMAP);
		vertexBuffer.pos(xMin, yMin, zMin).tex(uv_yMin, uv_zMin).color(particleRed, particleGreen, particleBlue, alpha).lightmap(brightnessHigh, brightnessLow).endVertex();
		vertexBuffer.pos(xMin, yMin, zMax).tex(uv_yMin, uv_zMax).color(particleRed, particleGreen, particleBlue, alpha).lightmap(brightnessHigh, brightnessLow).endVertex();
		vertexBuffer.pos(xMin, yMax, zMax).tex(uv_yMax, uv_zMax).color(particleRed, particleGreen, particleBlue, alpha).lightmap(brightnessHigh, brightnessLow).endVertex();
		vertexBuffer.pos(xMin, yMax, zMin).tex(uv_yMax, uv_zMin).color(particleRed, particleGreen, particleBlue, alpha).lightmap(brightnessHigh, brightnessLow).endVertex();
		tessellator.draw();
		
		vertexBuffer.begin(GL11.GL_QUADS, DefaultVertexFormats.PARTICLE_POSITION_TEX_COLOR_LMAP);
		vertexBuffer.pos(xMax, yMin, zMin).tex( uv_yMin, uv_zMin).color(particleRed, particleGreen, particleBlue, alpha).lightmap(brightnessHigh, brightnessLow).endVertex();
		vertexBuffer.pos(xMax, yMin, zMax).tex( uv_yMin, uv_zMax).color(particleRed, particleGreen, particleBlue, alpha).lightmap(brightnessHigh, brightnessLow).endVertex();
		vertexBuffer.pos(xMax, yMax, zMax).tex( uv_yMax, uv_zMax).color(particleRed, particleGreen, particleBlue, alpha).lightmap(brightnessHigh, brightnessLow).endVertex();
		vertexBuffer.pos(xMax, yMax, zMin).tex( uv_yMax, uv_zMin).color(particleRed, particleGreen, particleBlue, alpha).lightmap(brightnessHigh, brightnessLow).endVertex();
		tessellator.draw();
		
		// y planes
		vertexBuffer.begin(GL11.GL_QUADS, DefaultVertexFormats.PARTICLE_POSITION_TEX_COLOR_LMAP);
		vertexBuffer.pos(xMin, yMin, zMin).tex(uv_xMin, uv_zMin).color(particleRed, particleGreen, particleBlue, alpha).lightmap(brightnessHigh, brightnessLow).endVertex();
		vertexBuffer.pos(xMin, yMin, zMax).tex(uv_xMin, uv_zMax).color(particleRed, particleGreen, particleBlue, alpha).lightmap(brightnessHigh, brightnessLow).endVertex();
		vertexBuffer.pos(xMax, yMin, zMax).tex(uv_xMax, uv_zMax).color(particleRed, particleGreen, particleBlue, alpha).lightmap(brightnessHigh, brightnessLow).endVertex();
		vertexBuffer.pos(xMax, yMin, zMin).tex(uv_xMax, uv_zMin).color(particleRed, particleGreen, particleBlue, alpha).lightmap(brightnessHigh, brightnessLow).endVertex();
		tessellator.draw();
		
		vertexBuffer.begin(GL11.GL_QUADS, DefaultVertexFormats.PARTICLE_POSITION_TEX_COLOR_LMAP);
		vertexBuffer.pos(xMin, yMax, zMin).tex(uv_xMin, uv_zMin).color(particleRed, particleGreen, particleBlue, alpha).lightmap(brightnessHigh, brightnessLow).endVertex();
		vertexBuffer.pos(xMin, yMax, zMax).tex(uv_xMin, uv_zMax).color(particleRed, particleGreen, particleBlue, alpha).lightmap(brightnessHigh, brightnessLow).endVertex();
		vertexBuffer.pos(xMax, yMax, zMax).tex(uv_xMax, uv_zMax).color(particleRed, particleGreen, particleBlue, alpha).lightmap(brightnessHigh, brightnessLow).endVertex();
		vertexBuffer.pos(xMax, yMax, zMin).tex(uv_xMax, uv_zMin).color(particleRed, particleGreen, particleBlue, alpha).lightmap(brightnessHigh, brightnessLow).endVertex();
		tessellator.draw();
		
		// z planes
		vertexBuffer.begin(GL11.GL_QUADS, DefaultVertexFormats.PARTICLE_POSITION_TEX_COLOR_LMAP);
		vertexBuffer.pos(xMin, yMin, zMin).tex(uv_xMin, uv_yMin).color(particleRed, particleGreen, particleBlue, alpha).lightmap(brightnessHigh, brightnessLow).endVertex();
		vertexBuffer.pos(xMin, yMax, zMin).tex(uv_xMin, uv_yMax).color(particleRed, particleGreen, particleBlue, alpha).lightmap(brightnessHigh, brightnessLow).endVertex();
		vertexBuffer.pos(xMax, yMax, zMin).tex(uv_xMax, uv_yMax).color(particleRed, particleGreen, particleBlue, alpha).lightmap(brightnessHigh, brightnessLow).endVertex();
		vertexBuffer.pos(xMax, yMin, zMin).tex(uv_xMax, uv_yMin).color(particleRed, particleGreen, particleBlue, alpha).lightmap(brightnessHigh, brightnessLow).endVertex();
		tessellator.draw();
		
		vertexBuffer.begin(GL11.GL_QUADS, DefaultVertexFormats.PARTICLE_POSITION_TEX_COLOR_LMAP);
		vertexBuffer.pos(xMin, yMin, zMax).tex(uv_xMin, uv_yMin).color(particleRed, particleGreen, particleBlue, alpha).lightmap(brightnessHigh, brightnessLow).endVertex();
		vertexBuffer.pos(xMin, yMax, zMax).tex(uv_xMin, uv_yMax).color(particleRed, particleGreen, particleBlue, alpha).lightmap(brightnessHigh, brightnessLow).endVertex();
		vertexBuffer.pos(xMax, yMax, zMax).tex(uv_xMax, uv_yMax).color(particleRed, particleGreen, particleBlue, alpha).lightmap(brightnessHigh, brightnessLow).endVertex();
		vertexBuffer.pos(xMax, yMin, zMax).tex(uv_xMax, uv_yMin).color(particleRed, particleGreen, particleBlue, alpha).lightmap(brightnessHigh, brightnessLow).endVertex();
		tessellator.draw();
		
		GlStateManager.depthMask(true);
		GlStateManager.disableBlend();
		GlStateManager.enableCull();
		GlStateManager.popMatrix();
	}
	
	@Override
	public int getFXLayer() {
		return 3; // custom texture
	}
}