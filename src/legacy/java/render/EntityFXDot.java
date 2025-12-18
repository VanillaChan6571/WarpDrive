package cr0s.warpdrive.render;

import cr0s.warpdrive.data.Vector3;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class EntityFXDot extends AbstractEntityFX {
	
	private Vector3 v3Acceleration;
	private double friction;
	private int layer = 0;  // 0 = particles, 1 = blocks, 2 = items
	
	public EntityFXDot(final World world, final Vector3 v3Position,
	                   final Vector3 v3Motion, final Vector3 v3Acceleration, final double friction,
	                   final int age) {
		super(world, v3Position.x, v3Position.y, v3Position.z, 0.0D, 0.0D, 0.0D);
		
		this.setSize(0.02F, 0.02F);
		this.canCollide = false;
		this.motionX = v3Motion.x;
		this.motionY = v3Motion.y;
		this.motionZ = v3Motion.z;
		this.v3Acceleration = v3Acceleration;
		this.friction = friction;
		this.particleMaxAge = age;
		
		// defaults to vanilla water drip
		setParticleTextureIndex(113);
		
		// refresh bounding box
		setPosition(v3Position.x, v3Position.y, v3Position.z);
	}
	
	public void setParticleFromBlockIcon(final TextureAtlasSprite texture) {
		layer = 1;
		setParticleTexture(texture);
	}
	
	public void setParticleFromItemIcon(final TextureAtlasSprite texture) {
		layer = 2;
		setParticleTexture(texture);
	}
	
	@Override
	public void onUpdate() {
		prevPosX = posX;
		prevPosY = posY;
		prevPosZ = posZ;
		
		if (particleAge++ >= particleMaxAge) {
			setExpired();
		}
		
		move(motionX, motionY, motionZ);
		motionX = (motionX + v3Acceleration.x) * friction;
		motionY = (motionY + v3Acceleration.y) * friction;
		motionZ = (motionZ + v3Acceleration.z) * friction;
	}
	
	@Override
	public int getFXLayer() {
		return layer;
	}
	
	@Override
	public int getBrightnessForRender(final float p_70070_1_) {
		return 0xF00000;
	}
	
	@Override
	public void renderParticle(final BufferBuilder vertexBuffer, final Entity entityIn, final float partialTick,
	                           final float rotationX, final float rotationZ, final float rotationYZ, final float rotationXY, final float rotationXZ) {
		double minU = particleTextureIndexX / 16.0F;
		double maxU = minU + 0.0624375F;
		double minV = particleTextureIndexY / 16.0F;
		double maxV = minV + 0.0624375F;
		final float scale = 0.1F * particleScale;
		
		if (particleTexture != null) {
			minU = particleTexture.getMinU();
			maxU = particleTexture.getMaxU();
			minV = particleTexture.getMinV();
			maxV = particleTexture.getMaxV();
		}
		
		final double x = prevPosX + (posX - prevPosX) * partialTick - interpPosX;
		final double y = prevPosY + (posY - prevPosY) * partialTick - interpPosY;
		final double z = prevPosZ + (posZ - prevPosZ) * partialTick - interpPosZ;
		
		// alpha increase during first tick and decays during last 2 ticks
		float alpha = particleAlpha;
		final int ageLeft = particleMaxAge - particleAge;
		if (particleAge < 1) {
			alpha = particleAlpha * partialTick;
		} else if (ageLeft < 2) {
			if (ageLeft < 1) {
				alpha = particleAlpha * (0.5F - partialTick / 2.0F);
			} else {
				alpha = particleAlpha * (1.0F - partialTick / 2.0F);
			}
		}
		
		// start drawing (done by caller in layers 0 to 3)
		// vertexBuffer.begin(GL11.GL_QUADS, DefaultVertexFormats.PARTICLE_POSITION_TEX_COLOR_LMAP);
		
		// get brightness factors
		final int brightnessForRender = getBrightnessForRender(partialTick);
		final int brightnessHigh = brightnessForRender >> 16 & 65535;
		final int brightnessLow  = brightnessForRender & 65535;
		
		// compute relative texture coordinates
		final Vec3d[] vec3ds = new Vec3d[] { new Vec3d(-rotationX * scale - rotationXY * scale, -rotationZ * scale, -rotationYZ * scale - rotationXZ * scale),
		                                     new Vec3d(-rotationX * scale + rotationXY * scale,  rotationZ * scale, -rotationYZ * scale + rotationXZ * scale),
		                                     new Vec3d( rotationX * scale + rotationXY * scale,  rotationZ * scale,  rotationYZ * scale + rotationXZ * scale),
		                                     new Vec3d( rotationX * scale - rotationXY * scale, -rotationZ * scale,  rotationYZ * scale - rotationXZ * scale)};
		
		/*
		// apply rotation motion, only used by ParticleFallingDust
		// field_190014_F = rotation actual tick
		// field_190015_G = rotation previous tick
		// field_190019_b = number of full rotations per tick
		// field_190016_K = look
		if (field_190014_F != 0.0F) {
			final float angleSpin = field_190014_F + (field_190014_F - field_190015_G) * partialTick;
			final float f9 = MathHelper.cos(angleSpin * 0.5F);
			final float f10 = MathHelper.sin(angleSpin * 0.5F) * (float) field_190016_K.x;
			final float f11 = MathHelper.sin(angleSpin * 0.5F) * (float) field_190016_K.y;
			final float f12 = MathHelper.sin(angleSpin * 0.5F) * (float) field_190016_K.z;
			final Vec3d vec3d = new Vec3d(f10, f11, f12);
			
			for (int l = 0; l < 4; ++l) {
				vec3ds[l] = vec3d.scale(2.0D * vec3ds[l].dotProduct(vec3d))
				                 .add(vec3ds[l].scale(f9 * f9 - vec3d.dotProduct(vec3d)))
				                 .add(vec3d.crossProduct(vec3ds[l]).scale(2.0D * f9));
			}
		}
		/**/
		
		vertexBuffer.pos(x + vec3ds[0].x, y + vec3ds[0].y, z + vec3ds[0].z).tex(maxU, maxV).color(particleRed, particleGreen, particleBlue, alpha).lightmap(brightnessHigh, brightnessLow).endVertex();
		vertexBuffer.pos(x + vec3ds[1].x, y + vec3ds[1].y, z + vec3ds[1].z).tex(maxU, minV).color(particleRed, particleGreen, particleBlue, alpha).lightmap(brightnessHigh, brightnessLow).endVertex();
		vertexBuffer.pos(x + vec3ds[2].x, y + vec3ds[2].y, z + vec3ds[2].z).tex(minU, minV).color(particleRed, particleGreen, particleBlue, alpha).lightmap(brightnessHigh, brightnessLow).endVertex();
		vertexBuffer.pos(x + vec3ds[3].x, y + vec3ds[3].y, z + vec3ds[3].z).tex(minU, maxV).color(particleRed, particleGreen, particleBlue, alpha).lightmap(brightnessHigh, brightnessLow).endVertex();
	}
}