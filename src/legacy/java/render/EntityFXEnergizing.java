package cr0s.warpdrive.render;

import cr0s.warpdrive.data.Vector3;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class EntityFXEnergizing extends AbstractEntityFX {
	
	private static final ResourceLocation TEXTURE = new ResourceLocation("warpdrive", "textures/particle/energy_grey.png");
	
	private double radius;
	private double length;
	private final int countSteps;
	private float rotYaw;
	private float rotPitch;
	private float prevYaw;
	private float prevPitch;
	
	public EntityFXEnergizing(final World world, final Vector3 position, final Vector3 target,
	                          final float red, final float green, final float blue,
	                          final int age, final float radius) {
		super(world, position.x, position.y, position.z, 0.0D, 0.0D, 0.0D);
		
		setRBGColorF(red, green, blue);
		setSize(0.02F, 0.02F);
		canCollide = false;
		motionX = 0.0D;
		motionY = 0.0D;
		motionZ = 0.0D;
		
		this.radius = radius;
		
		final float xd = (float) (posX - target.x);
		final float yd = (float) (posY - target.y);
		final float zd = (float) (posZ - target.z);
		length = new Vector3(this).distanceTo(target);
		final double lengthXZ = MathHelper.sqrt(xd * xd + zd * zd);
		rotYaw = (float) (Math.atan2(xd, zd) * 180.0D / Math.PI);
		rotPitch = (float) (Math.atan2(yd, lengthXZ) * 180.0D / Math.PI);
		prevYaw = rotYaw;
		prevPitch = rotPitch;
		particleMaxAge = age;
		
		// kill the particle if it's too far away
		// reduce cylinder resolution when fancy graphic are disabled
		final Entity entityRender = Minecraft.getMinecraft().getRenderViewEntity();
		int visibleDistance = 300;
		
		if (!Minecraft.getMinecraft().gameSettings.fancyGraphics) {
			visibleDistance = 100;
			countSteps = 1;
		} else {
			countSteps = 6;
		}
		
		if (entityRender.getDistance(posX, posY, posZ) > visibleDistance) {
			particleMaxAge = 0;
		}
	}
	
	@Override
	public void onUpdate() {
		prevPosX = posX;
		prevPosY = posY;
		prevPosZ = posZ;
		prevYaw = rotYaw;
		prevPitch = rotPitch;
		
		if (particleAge++ >= particleMaxAge) {
			setExpired();
		}
	}
	
	@Override
	public void renderParticle(final BufferBuilder vertexBuffer, final Entity entityIn, final float partialTick,
	                           final float rotationX, final float rotationZ, final float rotationYZ, final float rotationXY, final float rotationXZ) {
		GlStateManager.pushMatrix();
		
		final double factorFadeIn = Math.min((particleAge + partialTick) / 20.0F, 1.0F);
		
		// alpha starts at 50%, vanishing to 10% during last ticks
		float alpha = 0.5F;
		if (particleMaxAge - particleAge <= 4) {
			alpha = 0.5F - (4 - (particleMaxAge - particleAge)) * 0.1F;
		} else {
			// add random flickering
			final double timeAlpha = ((getSeed() ^ 0x47C8) & 0xFFFF) + particleAge + partialTick + 0.0167D;
			alpha += Math.pow(Math.sin(timeAlpha * 0.37D) + Math.sin(0.178D + timeAlpha * 0.17D), 2.0D) * 0.05D;
		}
		
		// get brightness factors
		final int brightnessForRender = getBrightnessForRender(partialTick);
		final int brightnessHigh = brightnessForRender >> 16 & 65535;
		final int brightnessLow  = Math.max(240, brightnessForRender & 65535);
		
		// texture clock is offset to de-synchronize particles
		final double timeTexture = (getSeed() & 0xFFFF) + particleAge + partialTick;
		
		// repeated a pixel column, changing periodically, to animate the texture
		final double uOffset = ((int) Math.floor(timeTexture * 0.5D) % 16) / 16.0D;
		
		// add vertical noise
		final double vOffset = Math.pow(Math.sin(timeTexture * 0.20D), 2.0D) * 0.005D;
		
		
		// bind our texture, repeating on both axis
		Minecraft.getMinecraft().getTextureManager().bindTexture(TEXTURE);
		GlStateManager.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
		GlStateManager.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
		
		// rendering on both sides
		GlStateManager.disableCull();
		
		// alpha transparency, don't update depth mask
		GlStateManager.enableBlend();
		GlStateManager.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE);
		GlStateManager.depthMask(false);
		
		// animated translation
		final float xx = (float)(prevPosX + (posX - prevPosX) * partialTick - interpPosX);
		final float yy = (float)(prevPosY + (posY - prevPosY) * partialTick - interpPosY);
		final float zz = (float)(prevPosZ + (posZ - prevPosZ) * partialTick - interpPosZ);
		GlStateManager.translate(xx, yy, zz);
		
		// animated rotation
		final float rotYaw = prevYaw + (this.rotYaw - prevYaw) * partialTick;
		final float rotPitch = prevPitch + (this.rotPitch - prevPitch) * partialTick;
		final float rotSpin = 0.0F;
		GlStateManager.rotate(90.0F, 1.0F, 0.0F, 0.0F);
		GlStateManager.rotate(180.0F + rotYaw, 0.0F, 0.0F, -1.0F);
		GlStateManager.rotate(rotPitch, 1.0F, 0.0F, 0.0F);
		GlStateManager.rotate(rotSpin, 0.0F, 1.0F, 0.0F);
		
		// actual parameters
		final double radius = this.radius * factorFadeIn;
		final double yMin = length * (0.5D - factorFadeIn / 2.0D);
		final double yMax = length * (0.5D + factorFadeIn / 2.0D);
		final double uMin = uOffset;
		final double uMax = uMin + 1.0D / 32.0D;
		
		final double vMin = -1.0D + vOffset;
		final double vMax = vMin + length * factorFadeIn;
		
		// start drawing
		final Tessellator tessellator = Tessellator.getInstance();
		vertexBuffer.begin(GL11.GL_QUADS, DefaultVertexFormats.PARTICLE_POSITION_TEX_COLOR_LMAP);
		
		// loop covering 45 deg, using symmetry to cover a full circle
		final double angleMax = Math.PI / 4.0D;
		final double angleStep = angleMax / countSteps;
		double angle = 0.0D;
		double cosPrev = radius * Math.cos(angle);
		double sinPrev = radius * Math.sin(angle);
		for (int indexStep = 1; indexStep <= countSteps; indexStep++) {
			angle += angleStep;
			final double cosNext = radius * Math.cos(angle);
			final double sinNext = radius * Math.sin(angle);
			
			// cos sin
			vertexBuffer.pos( cosPrev, yMax,  sinPrev).tex(uMax, vMax).color(particleRed, particleGreen, particleBlue, alpha).lightmap(brightnessHigh, brightnessLow).endVertex();
			vertexBuffer.pos( cosPrev, yMin,  sinPrev).tex(uMax, vMin).color(particleRed, particleGreen, particleBlue, alpha).lightmap(brightnessHigh, brightnessLow).endVertex();
			vertexBuffer.pos( cosNext, yMin,  sinNext).tex(uMin, vMin).color(particleRed, particleGreen, particleBlue, alpha).lightmap(brightnessHigh, brightnessLow).endVertex();
			vertexBuffer.pos( cosNext, yMax,  sinNext).tex(uMin, vMax).color(particleRed, particleGreen, particleBlue, alpha).lightmap(brightnessHigh, brightnessLow).endVertex();
			
			vertexBuffer.pos(-cosPrev, yMax,  sinPrev).tex(uMax, vMax).color(particleRed, particleGreen, particleBlue, alpha).lightmap(brightnessHigh, brightnessLow).endVertex();
			vertexBuffer.pos(-cosPrev, yMin,  sinPrev).tex(uMax, vMin).color(particleRed, particleGreen, particleBlue, alpha).lightmap(brightnessHigh, brightnessLow).endVertex();
			vertexBuffer.pos(-cosNext, yMin,  sinNext).tex(uMin, vMin).color(particleRed, particleGreen, particleBlue, alpha).lightmap(brightnessHigh, brightnessLow).endVertex();
			vertexBuffer.pos(-cosNext, yMax,  sinNext).tex(uMin, vMax).color(particleRed, particleGreen, particleBlue, alpha).lightmap(brightnessHigh, brightnessLow).endVertex();
			
			vertexBuffer.pos( cosPrev, yMax, -sinPrev).tex(uMax, vMax).color(particleRed, particleGreen, particleBlue, alpha).lightmap(brightnessHigh, brightnessLow).endVertex();
			vertexBuffer.pos( cosPrev, yMin, -sinPrev).tex(uMax, vMin).color(particleRed, particleGreen, particleBlue, alpha).lightmap(brightnessHigh, brightnessLow).endVertex();
			vertexBuffer.pos( cosNext, yMin, -sinNext).tex(uMin, vMin).color(particleRed, particleGreen, particleBlue, alpha).lightmap(brightnessHigh, brightnessLow).endVertex();
			vertexBuffer.pos( cosNext, yMax, -sinNext).tex(uMin, vMax).color(particleRed, particleGreen, particleBlue, alpha).lightmap(brightnessHigh, brightnessLow).endVertex();
			
			vertexBuffer.pos(-cosPrev, yMax, -sinPrev).tex(uMax, vMax).color(particleRed, particleGreen, particleBlue, alpha).lightmap(brightnessHigh, brightnessLow).endVertex();
			vertexBuffer.pos(-cosPrev, yMin, -sinPrev).tex(uMax, vMin).color(particleRed, particleGreen, particleBlue, alpha).lightmap(brightnessHigh, brightnessLow).endVertex();
			vertexBuffer.pos(-cosNext, yMin, -sinNext).tex(uMin, vMin).color(particleRed, particleGreen, particleBlue, alpha).lightmap(brightnessHigh, brightnessLow).endVertex();
			vertexBuffer.pos(-cosNext, yMax, -sinNext).tex(uMin, vMax).color(particleRed, particleGreen, particleBlue, alpha).lightmap(brightnessHigh, brightnessLow).endVertex();
			
			// sin cos
			vertexBuffer.pos( sinPrev, yMax,  cosPrev).tex(uMax, vMax).color(particleRed, particleGreen, particleBlue, alpha).lightmap(brightnessHigh, brightnessLow).endVertex();
			vertexBuffer.pos( sinPrev, yMin,  cosPrev).tex(uMax, vMin).color(particleRed, particleGreen, particleBlue, alpha).lightmap(brightnessHigh, brightnessLow).endVertex();
			vertexBuffer.pos( sinNext, yMin,  cosNext).tex(uMin, vMin).color(particleRed, particleGreen, particleBlue, alpha).lightmap(brightnessHigh, brightnessLow).endVertex();
			vertexBuffer.pos( sinNext, yMax,  cosNext).tex(uMin, vMax).color(particleRed, particleGreen, particleBlue, alpha).lightmap(brightnessHigh, brightnessLow).endVertex();
			
			vertexBuffer.pos(-sinPrev, yMax,  cosPrev).tex(uMax, vMax).color(particleRed, particleGreen, particleBlue, alpha).lightmap(brightnessHigh, brightnessLow).endVertex();
			vertexBuffer.pos(-sinPrev, yMin,  cosPrev).tex(uMax, vMin).color(particleRed, particleGreen, particleBlue, alpha).lightmap(brightnessHigh, brightnessLow).endVertex();
			vertexBuffer.pos(-sinNext, yMin,  cosNext).tex(uMin, vMin).color(particleRed, particleGreen, particleBlue, alpha).lightmap(brightnessHigh, brightnessLow).endVertex();
			vertexBuffer.pos(-sinNext, yMax,  cosNext).tex(uMin, vMax).color(particleRed, particleGreen, particleBlue, alpha).lightmap(brightnessHigh, brightnessLow).endVertex();
			
			vertexBuffer.pos( sinPrev, yMax, -cosPrev).tex(uMax, vMax).color(particleRed, particleGreen, particleBlue, alpha).lightmap(brightnessHigh, brightnessLow).endVertex();
			vertexBuffer.pos( sinPrev, yMin, -cosPrev).tex(uMax, vMin).color(particleRed, particleGreen, particleBlue, alpha).lightmap(brightnessHigh, brightnessLow).endVertex();
			vertexBuffer.pos( sinNext, yMin, -cosNext).tex(uMin, vMin).color(particleRed, particleGreen, particleBlue, alpha).lightmap(brightnessHigh, brightnessLow).endVertex();
			vertexBuffer.pos( sinNext, yMax, -cosNext).tex(uMin, vMax).color(particleRed, particleGreen, particleBlue, alpha).lightmap(brightnessHigh, brightnessLow).endVertex();
			
			vertexBuffer.pos(-sinPrev, yMax, -cosPrev).tex(uMax, vMax).color(particleRed, particleGreen, particleBlue, alpha).lightmap(brightnessHigh, brightnessLow).endVertex();
			vertexBuffer.pos(-sinPrev, yMin, -cosPrev).tex(uMax, vMin).color(particleRed, particleGreen, particleBlue, alpha).lightmap(brightnessHigh, brightnessLow).endVertex();
			vertexBuffer.pos(-sinNext, yMin, -cosNext).tex(uMin, vMin).color(particleRed, particleGreen, particleBlue, alpha).lightmap(brightnessHigh, brightnessLow).endVertex();
			vertexBuffer.pos(-sinNext, yMax, -cosNext).tex(uMin, vMax).color(particleRed, particleGreen, particleBlue, alpha).lightmap(brightnessHigh, brightnessLow).endVertex();
			
			cosPrev = cosNext;
			sinPrev = sinNext;
		}
		
		// draw
		tessellator.draw();
		
		// restore OpenGL state
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