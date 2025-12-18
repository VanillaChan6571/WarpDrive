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
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import org.lwjgl.opengl.GL11;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class EntityFXBeam extends Particle {
	
	private static final int ROTATION_SPEED = 20;
	private static final float END_MODIFIER = 1.0F;
	private static final ResourceLocation TEXTURE = new ResourceLocation("warpdrive", "textures/particle/energy_grey.png");
	
	private float length = 0.0F;
	private float rotYaw = 0.0F;
	private float rotPitch = 0.0F;
	private float prevYaw = 0.0F;
	private float prevPitch = 0.0F;
	private float prevSize = 0.0F;
	
	public EntityFXBeam(final World world, final Vector3 position, final Vector3 target,
	                    final float red, final float green, final float blue,
	                    final int age) {
		super(world, position.x, position.y, position.z, 0.0D, 0.0D, 0.0D);
		
		this.setRBGColorF(red, green, blue);
		this.setSize(0.02F, 0.02F);
		this.canCollide = false;
		this.motionX = 0.0D;
		this.motionY = 0.0D;
		this.motionZ = 0.0D;
		
		final float xd = (float) (this.posX - target.x);
		final float yd = (float) (this.posY - target.y);
		final float zd = (float) (this.posZ - target.z);
		this.length = (float) position.distanceTo(target);
		final double lengthXZ = MathHelper.sqrt(xd * xd + zd * zd);
		this.rotYaw = (float) (Math.atan2(xd, zd) * 180.0D / Math.PI);
		this.rotPitch = (float) (Math.atan2(yd, lengthXZ) * 180.0D / Math.PI);
		this.prevYaw = this.rotYaw;
		this.prevPitch = this.rotPitch;
		this.particleMaxAge = age;
		
		// kill the particle if it's too far away
		final Entity entityRender = Minecraft.getMinecraft().getRenderViewEntity();
		int visibleDistance = 300;
		
		if (!Minecraft.getMinecraft().gameSettings.fancyGraphics) {
			visibleDistance = 100;
		}
		
		if ( entityRender != null
		  && entityRender.getDistance(posX, posY, posZ) > visibleDistance ) {
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
		
		final float rot = world.provider.getWorldTime() % (360 / ROTATION_SPEED) * ROTATION_SPEED + ROTATION_SPEED * partialTick;
		
		final float sizeTarget = Math.min(particleAge / 4.0F, 1.0F);
		final float size = prevSize + (sizeTarget - prevSize) * partialTick;
		
		// alpha starts at 50%, vanishing to 10% during last ticks
		float alpha = 0.5F;
		if (particleMaxAge - particleAge <= 4) {
			alpha = 0.5F - (4 - (particleMaxAge - particleAge)) * 0.1F;
		}
		
		// get brightness factors
		final int brightnessForRender = getBrightnessForRender(partialTick);
		final int brightnessHigh = brightnessForRender >> 16 & 65535;
		final int brightnessLow  = Math.max(240, brightnessForRender & 65535);
		
		Minecraft.getMinecraft().getTextureManager().bindTexture(TEXTURE);
		GlStateManager.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
		GlStateManager.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
		GlStateManager.disableCull();
		
		final float relativeTime = world.getTotalWorldTime() + partialTick;
		final float vOffset = -relativeTime * 0.2F - MathHelper.floor(-relativeTime * 0.1F);
		
		GlStateManager.enableBlend();
		GlStateManager.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE);
		GlStateManager.depthMask(false);
		
		final float xx = (float)(prevPosX + (posX - prevPosX) * partialTick - interpPosX);
		final float yy = (float)(prevPosY + (posY - prevPosY) * partialTick - interpPosY);
		final float zz = (float)(prevPosZ + (posZ - prevPosZ) * partialTick - interpPosZ);
		GlStateManager.translate(xx, yy, zz);
		
		final float rotYaw = prevYaw + (this.rotYaw - prevYaw) * partialTick;
		final float rotPitch = prevPitch + (this.rotPitch - prevPitch) * partialTick;
		GlStateManager.rotate(90.0F, 1.0F, 0.0F, 0.0F);
		GlStateManager.rotate(180.0F + rotYaw, 0.0F, 0.0F, -1.0F);
		GlStateManager.rotate(rotPitch, 1.0F, 0.0F, 0.0F);
		GlStateManager.rotate(rot, 0.0F, 1.0F, 0.0F);
		
		final double xMinStart = -0.15D * size;
		final double xMaxStart = 0.15D * size;
		final double xMinEnd = -0.15D * size * END_MODIFIER;
		final double xMaxEnd = 0.15D * size * END_MODIFIER;
		final double yMax = length * size;
		final double uMin = 0.0D;
		final double uMax = 1.0D;
		
		final Tessellator tessellator = Tessellator.getInstance();
		for (int t = 0; t < 3; t++) {
			final double vMin = -1.0F + vOffset + t / 3.0F;
			final double vMax = vMin + length * size;
			GlStateManager.rotate(60.0F, 0.0F, 1.0F, 0.0F);
			vertexBuffer.begin(GL11.GL_QUADS, DefaultVertexFormats.PARTICLE_POSITION_TEX_COLOR_LMAP);
			vertexBuffer.pos(xMinEnd  , yMax, 0.0D).tex(uMax, vMax).color(particleRed, particleGreen, particleBlue, alpha).lightmap(brightnessHigh, brightnessLow).endVertex();
			vertexBuffer.pos(xMinStart, 0.0D, 0.0D).tex(uMax, vMin).color(particleRed, particleGreen, particleBlue, alpha).lightmap(brightnessHigh, brightnessLow).endVertex();
			vertexBuffer.pos(xMaxStart, 0.0D, 0.0D).tex(uMin, vMin).color(particleRed, particleGreen, particleBlue, alpha).lightmap(brightnessHigh, brightnessLow).endVertex();
			vertexBuffer.pos(xMaxEnd  , yMax, 0.0D).tex(uMin, vMax).color(particleRed, particleGreen, particleBlue, alpha).lightmap(brightnessHigh, brightnessLow).endVertex();
			tessellator.draw();
		}
		
		GlStateManager.depthMask(true);
		GlStateManager.disableBlend();
		GlStateManager.enableCull();
		GlStateManager.popMatrix();
		prevSize = size;
		Minecraft.getMinecraft().getTextureManager().bindTexture(new ResourceLocation("textures/particle/particles.png"));
	}
	
	@Override
	public int getFXLayer() {
		return 3; // 3 means custom texture
	}
}