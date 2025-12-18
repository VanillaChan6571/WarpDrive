package cr0s.warpdrive.config.structures;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.config.Filler;
import cr0s.warpdrive.config.GenericSet;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.config.structures.Orb.OrbShell;
import cr0s.warpdrive.world.EntitySphereGen;
import cr0s.warpdrive.world.EntityStarCore;
import cr0s.warpdrive.world.WorldGenSmallShip;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import net.minecraftforge.common.util.Constants;

public class OrbInstance extends AbstractStructureInstance {
	
	protected ArrayList<GenericSet<Filler>> orbShellInstances;
	private int[] orbShellThicknesses;
	protected int totalThickness;
	protected int minThickness;
	protected String schematicName;
	
	// internal look-up table to accelerate computations
	private ArrayList<GenericSet<Filler>> sqRadiusToOrbShell;
	
	public OrbInstance(final Orb orb, final Random random) {
		super(orb, random);
		
		orbShellInstances = new ArrayList<>(orb.orbShells.length);
		orbShellThicknesses = new int[orb.orbShells.length];
		totalThickness = 0;
		minThickness = 0;
		int orbShellIndexOut = 0;
		for (int orbShellIndexIn = 0; orbShellIndexIn < orb.orbShells.length; orbShellIndexIn++) {
			final OrbShell orbShell = orb.orbShells[orbShellIndexIn].instantiate(random);
			// skip if it's an empty filler set
			if (orbShell != null) {
				orbShellInstances.add(orbShell);
				final int thickness = Commons.randomRange(random, orbShell.minThickness, orbShell.maxThickness);
				orbShellThicknesses[orbShellIndexOut] = thickness;
				totalThickness += thickness;
				minThickness += orbShell.minThickness;
				orbShellIndexOut++;
			}
		}
		// resize array in case we had one or more empty filler set
		if (orbShellThicknesses.length != orbShellIndexOut) {
			orbShellThicknesses = Arrays.copyOf(orbShellThicknesses, orbShellIndexOut);
		}
		
		schematicName = orb.schematicName;
		
		constructionFinalizer();
	}
	
	private void constructionFinalizer() {
		final int sqRadius = totalThickness * totalThickness;
		sqRadiusToOrbShell = new ArrayList<>(sqRadius);
		for (int sqRange = 0; sqRange < sqRadius; sqRange++) {// FIXME should we loop the orb shells instead of the range here?
			int range = 0;
			for (int indexShell = 0; indexShell < orbShellInstances.size(); indexShell++) {
				range += orbShellThicknesses[indexShell];
				if (sqRange <= range * range) {
					sqRadiusToOrbShell.add(orbShellInstances.get(indexShell));
					break;
				}
			}
		}
	}
	
	public OrbInstance(final NBTTagCompound tagCompound) {
		super(tagCompound);
		
		final NBTTagList listOrbShells = tagCompound.getTagList("orbShellInstances", Constants.NBT.TAG_COMPOUND);
		if (listOrbShells.isEmpty()) {
			throw new RuntimeException(String.format("Empty orbShellInstances list isn't supported in %s: %s",
			                                         this, tagCompound));
		}
		orbShellInstances = new ArrayList<>(listOrbShells.tagCount());
		for (int indexOrbShell = 0; indexOrbShell < listOrbShells.tagCount(); indexOrbShell++) {
			final NBTTagCompound tagCompoundOrbShell = listOrbShells.getCompoundTagAt(indexOrbShell);
			final GenericSet<Filler> orbShell = new GenericSet<>(tagCompoundOrbShell, Filler.DEFAULT, "filler");
			orbShellInstances.add(orbShell);
		}
		orbShellThicknesses = tagCompound.getIntArray("orbShellThicknesses");
		if (orbShellInstances.size() != orbShellThicknesses.length) {
			throw new RuntimeException(String.format("Inconsistent orbShell and thicknesses sizes: %d != %d\n%s",
			                                         orbShellInstances.size(), orbShellThicknesses.length, tagCompound));
		}
		totalThickness = tagCompound.getInteger("totalThickness");
		minThickness = tagCompound.getInteger("minThickness");
		
		if (tagCompound.hasKey("schematicName")) {
			schematicName = tagCompound.getString("schematicName");
		} else {
			schematicName = null;
		}
		
		constructionFinalizer();
	}
	
	@Override
	public NBTTagCompound writeToNBT(@Nonnull final NBTTagCompound tagCompound) {
		super.writeToNBT(tagCompound);
		
		final NBTTagList listOrbShells = new NBTTagList();
		if (orbShellInstances.isEmpty()) {
			throw new RuntimeException(String.format("Empty orbShellInstances list isn't supported in %s",
			                                         this));
		}
		for (final GenericSet<Filler> orbShellInstance : orbShellInstances) {
			final NBTTagCompound tagCompoundOrbShell = orbShellInstance.writeToNBT(new NBTTagCompound());
			listOrbShells.appendTag(tagCompoundOrbShell);
		}
		tagCompound.setTag("orbShellInstances", listOrbShells);
		tagCompound.setIntArray("orbShellThicknesses", orbShellThicknesses);
		tagCompound.setInteger("totalThickness", totalThickness);
		tagCompound.setInteger("minThickness", minThickness);
		if (schematicName != null) {
			tagCompound.setString("schematicName", schematicName);
		}
		
		return tagCompound;
	}
	
	public int getTotalThickness() {
		return totalThickness;
	}
	
	@Override
	public boolean generate(@Nonnull final World world, @Nonnull final Random random, @Nonnull final BlockPos blockPos) {
		final boolean hasShip = schematicName != null && !schematicName.isEmpty();
		final int y2 = Math.min(WarpDriveConfig.SPACE_GENERATOR_Y_MAX_BORDER - totalThickness,
			  Math.max(blockPos.getY(), WarpDriveConfig.SPACE_GENERATOR_Y_MIN_BORDER + totalThickness));
		final BlockPos blockPosUpdated = y2 == blockPos.getY() ? blockPos : new BlockPos(blockPos.getX(), y2, blockPos.getZ());
		if (hasShip) {
			new WorldGenSmallShip(random.nextFloat() < 0.2F, false).generate(world, random, blockPosUpdated);
		}
		final EntitySphereGen entitySphereGen = new EntitySphereGen(world, blockPos.getX(), y2, blockPos.getZ(), this, !hasShip);
		world.spawnEntity(entitySphereGen);
		if (((Orb) structure).hasStarCore) {
			return world.spawnEntity(new EntityStarCore(world, blockPos.getX(), y2, blockPos.getZ(), totalThickness));
		}
		return true;
	}
	
	public GenericSet<Filler> getFillerSetFromSquareRange(final int sqRadius) {
		if (sqRadius < sqRadiusToOrbShell.size()) {
			return sqRadiusToOrbShell.get(sqRadius);
		} else {
			return sqRadiusToOrbShell.get(sqRadiusToOrbShell.size() - 1);
		}
	}
}
