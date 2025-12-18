package cr0s.warpdrive.world;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.block.TileEntityAbstractEnergy;
import cr0s.warpdrive.data.BlockProperties;
import cr0s.warpdrive.data.EnumTier;

import javax.annotation.Nonnull;
import java.util.Random;

import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenerator;

public class WorldGenSmallShip extends WorldGenerator {
	
	private final boolean isCorrupted;
	private final boolean isCreative;
	
	public WorldGenSmallShip(final boolean isCorrupted, final boolean isCreative) {
		super();
		
		this.isCorrupted = isCorrupted;
		this.isCreative = isCreative;
	}
	
	@Override
	public boolean generate(@Nonnull final World world, @Nonnull final Random rand, @Nonnull final BlockPos blockPos) {
		final WorldGenStructure genStructure = new WorldGenStructure(isCorrupted, rand);
		final boolean hasGlassRoof = rand.nextBoolean();
		final boolean hasWings = rand.nextBoolean();
		final int x = blockPos.getX() - 5;
		final int y = blockPos.getY() - 3;
		final int z = blockPos.getZ() - 7;
		
		// Ship is facing West: X- is forward, Z+ is left
		genStructure.setHullPlain(world, x, y + 1, z + 4);
		genStructure.setHullPlain(world, x, y + 1, z + 10);
		genStructure.setHullPlain(world, x + 1, y + 1, z + 4);
		genStructure.setHullPlain(world, x + 1, y + 1, z + 5);
		genStructure.setHullPlain(world, x + 1, y + 1, z + 9);
		genStructure.setHullPlain(world, x + 1, y + 1, z + 10);
		genStructure.setHullPlain(world, x + 1, y + 2, z + 4);
		genStructure.setHullPlain(world, x + 1, y + 2, z + 10);
		genStructure.setHullPlain(world, x + 2, y + 1, z + 4);
		genStructure.setHullPlain(world, x + 2, y + 1, z + 5);
		genStructure.setHullPlain(world, x + 2, y + 1, z + 9);
		genStructure.setHullPlain(world, x + 2, y + 1, z + 10);
		genStructure.setHullPlain(world, x + 2, y + 2, z + 4);
		genStructure.setHullPlain(world, x + 2, y + 2, z + 10);
		genStructure.setHullPlain(world, x + 2, y + 3, z + 6);
		genStructure.setHullPlain(world, x + 2, y + 3, z + 7);
		genStructure.setHullPlain(world, x + 2, y + 3, z + 8);
		genStructure.setHullGlass(world, x + 2, y + 4, z + 6);
		genStructure.setHullGlass(world, x + 2, y + 4, z + 7);
		genStructure.setHullGlass(world, x + 2, y + 4, z + 8);
		genStructure.setHullGlass(world, x + 2, y + 5, z + 6);
		genStructure.setHullGlass(world, x + 2, y + 5, z + 7);
		genStructure.setHullGlass(world, x + 2, y + 5, z + 8);
		genStructure.setHullPlain(world, x + 3, y + 1, z + 4);
		genStructure.setHullPlain(world, x + 3, y + 1, z + 5);
		genStructure.setHullPlain(world, x + 3, y + 1, z + 9);
		genStructure.setHullPlain(world, x + 3, y + 1, z + 10);
		genStructure.setHullPlain(world, x + 3, y + 2, z + 4);
		genStructure.setHullPlain(world, x + 3, y + 2, z + 5);
		genStructure.setHullPlain(world, x + 3, y + 2, z + 6);
		genStructure.setHullPlain(world, x + 3, y + 2, z + 7);
		genStructure.setHullPlain(world, x + 3, y + 2, z + 8);
		genStructure.setHullPlain(world, x + 3, y + 2, z + 9);
		genStructure.setHullPlain(world, x + 3, y + 2, z + 10);
		genStructure.setHullPlain(world, x + 3, y + 3, z + 5);
		genStructure.setHullPlain(world, x + 3, y + 3, z + 9);
		genStructure.setHullPlain(world, x + 3, y + 4, z + 5);
		genStructure.setHullPlain(world, x + 3, y + 4, z + 9);
		genStructure.setHullPlain(world, x + 3, y + 5, z + 5);
		genStructure.setHullPlain(world, x + 3, y + 5, z + 9);
		genStructure.setHullPlain(world, x + 3, y + 6, z + 6);
		genStructure.setHullPlain(world, x + 3, y + 6, z + 7);
		genStructure.setHullPlain(world, x + 3, y + 6, z + 8);
		genStructure.setHullPlain(world, x + 4, y + 1, z + 4);
		genStructure.setHullPlain(world, x + 4, y + 1, z + 5);
		genStructure.setHullPlain(world, x + 4, y + 1, z + 6);
		genStructure.setHullPlain(world, x + 4, y + 1, z + 7);
		genStructure.setHullPlain(world, x + 4, y + 1, z + 8);
		genStructure.setHullPlain(world, x + 4, y + 1, z + 9);
		genStructure.setHullPlain(world, x + 4, y + 1, z + 10);
		genStructure.setHullPlain(world, x + 4, y + 2, z + 4);
		world.setBlockState(new BlockPos(x + 4, y + 2, z + 5), Blocks.GLOWSTONE.getDefaultState());
		genStructure.setHullPlain(world, x + 4, y + 2, z + 6);
		genStructure.setHullPlain(world, x + 4, y + 2, z + 7);
		genStructure.setHullPlain(world, x + 4, y + 2, z + 8);
		world.setBlockState(new BlockPos(x + 4, y + 2, z + 9), Blocks.GLOWSTONE.getDefaultState());
		genStructure.setHullPlain(world, x + 4, y + 2, z + 10);
		genStructure.setHullPlain(world, x + 4, y + 3, z + 4);
		genStructure.setHullPlain(world, x + 4, y + 3, z + 10);
		genStructure.setHullGlass(world, x + 4, y + 4, z + 4);
		genStructure.setHullGlass(world, x + 4, y + 4, z + 10);
		genStructure.setHullPlain(world, x + 4, y + 5, z + 4);
		genStructure.setHullPlain(world, x + 4, y + 5, z + 5);
		genStructure.setHullPlain(world, x + 4, y + 5, z + 9);
		genStructure.setHullPlain(world, x + 4, y + 5, z + 10);
		genStructure.setHullPlain(world, x + 4, y + 6, z + 6);
		genStructure.setHullPlain(world, x + 4, y + 6, z + 7);
		genStructure.setHullPlain(world, x + 4, y + 6, z + 8);
		genStructure.setHullPlain(world, x + 5, y + 1, z + 4);
		genStructure.setHullPlain(world, x + 5, y + 1, z + 5);
		genStructure.setHullPlain(world, x + 5, y + 1, z + 6);
		genStructure.setHullPlain(world, x + 5, y + 1, z + 7);
		genStructure.setHullPlain(world, x + 5, y + 1, z + 8);
		genStructure.setHullPlain(world, x + 5, y + 1, z + 9);
		genStructure.setHullPlain(world, x + 5, y + 1, z + 10);
		genStructure.setHullPlain(world, x + 5, y + 2, z + 3);
		world.setBlockState(new BlockPos(x + 5, y + 2, z + 4), Blocks.GLOWSTONE.getDefaultState());
		genStructure.setHullPlain(world, x + 5, y + 2, z + 5);
		genStructure.setHullPlain(world, x + 5, y + 2, z + 6);
		world.setBlockState(new BlockPos(x + 5, y + 2, z + 7), Blocks.WOOL.getStateFromMeta(14), 2);
		world.setBlockState(new BlockPos(x + 5, y + 2, z + 8), Blocks.WOOL.getStateFromMeta(8), 2);
		genStructure.setHullPlain(world, x + 5, y + 2, z + 9);
		world.setBlockState(new BlockPos(x + 5, y + 2, z + 10), Blocks.GLOWSTONE.getDefaultState());
		genStructure.setHullPlain(world, x + 5, y + 2, z + 11);
		genStructure.setHullPlain(world, x + 5, y + 3, z + 3);
		genStructure.setHullPlain(world, x + 5, y + 3, z + 11);
		genStructure.setHullGlass(world, x + 5, y + 4, z + 3);
		genStructure.setHullGlass(world, x + 5, y + 4, z + 11);
		genStructure.setHullPlain(world, x + 5, y + 5, z + 3);
		genStructure.setHullPlain(world, x + 5, y + 5, z + 4);
		genStructure.setHullPlain(world, x + 5, y + 5, z + 10);
		genStructure.setHullPlain(world, x + 5, y + 5, z + 11);
		genStructure.setHullPlain(world, x + 5, y + 6, z + 5);
		genStructure.setHullPlain(world, x + 5, y + 6, z + 9);
		genStructure.setHullPlain(world, x + 5, y + 7, z + 6);
		genStructure.setHullPlain(world, x + 5, y + 7, z + 7);
		genStructure.setHullPlain(world, x + 5, y + 7, z + 8);
		genStructure.setHullPlain(world, x + 6, y + 1, z + 4);
		genStructure.setHullPlain(world, x + 6, y + 1, z + 5);
		genStructure.setHullPlain(world, x + 6, y + 1, z + 6);
		genStructure.setHullPlain(world, x + 6, y + 1, z + 7);
		genStructure.setHullPlain(world, x + 6, y + 1, z + 8);
		genStructure.setHullPlain(world, x + 6, y + 1, z + 9);
		genStructure.setHullPlain(world, x + 6, y + 1, z + 10);
		genStructure.setHullPlain(world, x + 6, y + 2, z + 3);
		genStructure.setHullPlain(world, x + 6, y + 2, z + 4);
		genStructure.setHullPlain(world, x + 6, y + 2, z + 5);
		world.setBlockState(new BlockPos(x + 6, y + 2, z + 6), Blocks.WOOL.getStateFromMeta(14), 2);
		world.setBlockState(new BlockPos(x + 6, y + 2, z + 7), Blocks.WOOL.getStateFromMeta(8), 2);
		world.setBlockState(new BlockPos(x + 6, y + 2, z + 8), Blocks.WOOL.getStateFromMeta(14), 2);
		genStructure.setHullPlain(world, x + 6, y + 2, z + 9);
		genStructure.setHullPlain(world, x + 6, y + 2, z + 10);
		genStructure.setHullPlain(world, x + 6, y + 2, z + 11);
		genStructure.setHullPlain(world, x + 6, y + 3, z + 2);
		world.setBlockState(new BlockPos(x + 6, y + 3, z + 3), Blocks.CHEST.getStateFromMeta(3), 2);
		genStructure.fillInventoryWithLoot(world, rand, x + 6, y + 3, z + 3, "ship");
		world.setBlockState(new BlockPos(x + 6, y + 3, z + 11), Blocks.CHEST.getStateFromMeta(2), 2);
		genStructure.fillInventoryWithLoot(world, rand, x + 6, y + 3, z + 11, "ship");
		genStructure.setHullPlain(world, x + 6, y + 3, z + 12);
		genStructure.setHullPlain(world, x + 6, y + 4, z + 2);
		world.setBlockState(new BlockPos(x + 6, y + 4, z + 3), Blocks.CHEST.getStateFromMeta(3), 2);
		genStructure.fillInventoryWithLoot(world, rand, x + 6, y + 4, z + 3, "ship");
		world.setBlockState(new BlockPos(x + 6, y + 4, z + 11), Blocks.CHEST.getStateFromMeta(2), 2);
		genStructure.fillInventoryWithLoot(world, rand, x + 6, y + 4, z + 11, "ship");
		genStructure.setHullPlain(world, x + 6, y + 4, z + 12);
		genStructure.setHullPlain(world, x + 6, y + 5, z + 2);
		genStructure.setHullPlain(world, x + 6, y + 5, z + 12);
		genStructure.setHullPlain(world, x + 6, y + 6, z + 3);
		genStructure.setHullPlain(world, x + 6, y + 6, z + 4);
		genStructure.setHullPlain(world, x + 6, y + 6, z + 10);
		genStructure.setHullPlain(world, x + 6, y + 6, z + 11);
		genStructure.setHullPlain(world, x + 6, y + 7, z + 5);
		if (hasGlassRoof) {
			genStructure.setHullGlass(world, x + 6, y + 7, z + 6);
			genStructure.setHullGlass(world, x + 6, y + 7, z + 7);
			genStructure.setHullGlass(world, x + 6, y + 7, z + 8);
		} else {
			genStructure.setHullPlain(world, x + 6, y + 7, z + 6);
			genStructure.setHullPlain(world, x + 6, y + 7, z + 7);
			genStructure.setHullPlain(world, x + 6, y + 7, z + 8);
		}
		genStructure.setHullPlain(world, x + 6, y + 7, z + 9);
		genStructure.setHullPlain(world, x + 7, y + 1, z + 4);
		genStructure.setHullPlain(world, x + 7, y + 1, z + 5);
		genStructure.setHullPlain(world, x + 7, y + 1, z + 6);
		genStructure.setHullPlain(world, x + 7, y + 1, z + 7);
		genStructure.setHullPlain(world, x + 7, y + 1, z + 8);
		genStructure.setHullPlain(world, x + 7, y + 1, z + 9);
		genStructure.setHullPlain(world, x + 7, y + 1, z + 10);
		genStructure.setHullPlain(world, x + 7, y + 2, z + 3);
		genStructure.setHullPlain(world, x + 7, y + 2, z + 4);
		genStructure.setHullPlain(world, x + 7, y + 2, z + 5);
		genStructure.setHullPlain(world, x + 7, y + 2, z + 6);
		world.setBlockState(new BlockPos(x + 7, y + 2, z + 7), Blocks.WOOL.getStateFromMeta(8), 2);
		world.setBlockState(new BlockPos(x + 7, y + 2, z + 8), Blocks.WOOL.getStateFromMeta(8), 2);
		world.setBlockState(new BlockPos(x + 7, y + 2, z + 9), Blocks.WOOL.getStateFromMeta(14), 2);
		genStructure.setHullPlain(world, x + 7, y + 2, z + 10);
		genStructure.setHullPlain(world, x + 7, y + 2, z + 11);
		genStructure.setHullPlain(world, x + 7, y + 3, z + 2);
		genStructure.setHullPlain(world, x + 7, y + 3, z + 12);
		genStructure.setHullGlass(world, x + 7, y + 4, z + 2);
		genStructure.setHullGlass(world, x + 7, y + 4, z + 12);
		genStructure.setHullGlass(world, x + 7, y + 5, z + 2);
		genStructure.setHullGlass(world, x + 7, y + 5, z + 12);
		genStructure.setHullPlain(world, x + 7, y + 6, z + 3);
		genStructure.setHullPlain(world, x + 7, y + 6, z + 4);
		genStructure.setHullPlain(world, x + 7, y + 6, z + 10);
		genStructure.setHullPlain(world, x + 7, y + 6, z + 11);
		genStructure.setHullPlain(world, x + 7, y + 7, z + 5);
		if (hasGlassRoof) {
			genStructure.setHullGlass(world, x + 7, y + 7, z + 6);
			genStructure.setHullGlass(world, x + 7, y + 7, z + 7);
			genStructure.setHullGlass(world, x + 7, y + 7, z + 8);
		} else {
			genStructure.setHullPlain(world, x + 7, y + 7, z + 6);
			genStructure.setHullPlain(world, x + 7, y + 7, z + 7);
			genStructure.setHullPlain(world, x + 7, y + 7, z + 8);
		}
		genStructure.setHullPlain(world, x + 7, y + 7, z + 9);
		genStructure.setHullPlain(world, x + 8, y + 1, z + 4);
		genStructure.setHullPlain(world, x + 8, y + 1, z + 5);
		genStructure.setHullPlain(world, x + 8, y + 1, z + 6);
		genStructure.setHullPlain(world, x + 8, y + 1, z + 7);
		genStructure.setHullPlain(world, x + 8, y + 1, z + 8);
		genStructure.setHullPlain(world, x + 8, y + 1, z + 9);
		genStructure.setHullPlain(world, x + 8, y + 1, z + 10);
		genStructure.setHullPlain(world, x + 8, y + 2, z + 3);
		genStructure.setHullPlain(world, x + 8, y + 2, z + 4);
		genStructure.setHullPlain(world, x + 8, y + 2, z + 5);
		world.setBlockState(new BlockPos(x + 8, y + 2, z + 6), Blocks.WOOL.getStateFromMeta(14), 2);
		world.setBlockState(new BlockPos(x + 8, y + 2, z + 7), Blocks.WOOL.getStateFromMeta(14), 2);
		world.setBlockState(new BlockPos(x + 8, y + 2, z + 8), Blocks.WOOL.getStateFromMeta(14), 2);
		genStructure.setHullPlain(world, x + 8, y + 2, z + 9);
		genStructure.setHullPlain(world, x + 8, y + 2, z + 10);
		genStructure.setHullPlain(world, x + 8, y + 2, z + 11);
		genStructure.setHullPlain(world, x + 8, y + 3, z + 2);
		genStructure.setHullPlain(world, x + 8, y + 3, z + 12);
		genStructure.setHullGlass(world, x + 8, y + 4, z + 2);
		genStructure.setHullGlass(world, x + 8, y + 4, z + 12);
		genStructure.setHullGlass(world, x + 8, y + 5, z + 2);
		genStructure.setHullGlass(world, x + 8, y + 5, z + 12);
		genStructure.setHullPlain(world, x + 8, y + 6, z + 3);
		genStructure.setHullPlain(world, x + 8, y + 6, z + 11);
		genStructure.setHullPlain(world, x + 8, y + 7, z + 4);
		genStructure.setHullPlain(world, x + 8, y + 7, z + 5);
		if (hasGlassRoof) {
			genStructure.setHullGlass(world, x + 8, y + 7, z + 6);
			genStructure.setHullGlass(world, x + 8, y + 7, z + 7);
			genStructure.setHullGlass(world, x + 8, y + 7, z + 8);
		} else {
			genStructure.setHullPlain(world, x + 8, y + 7, z + 6);
			genStructure.setHullPlain(world, x + 8, y + 7, z + 7);
			genStructure.setHullPlain(world, x + 8, y + 7, z + 8);
		}
		genStructure.setHullPlain(world, x + 8, y + 7, z + 9);
		genStructure.setHullPlain(world, x + 8, y + 7, z + 10);
		genStructure.setHullPlain(world, x + 9, y + 1, z + 4);
		genStructure.setHullPlain(world, x + 9, y + 1, z + 5);
		genStructure.setHullPlain(world, x + 9, y + 1, z + 6);
		genStructure.setHullPlain(world, x + 9, y + 1, z + 7);
		genStructure.setHullPlain(world, x + 9, y + 1, z + 8);
		genStructure.setHullPlain(world, x + 9, y + 1, z + 9);
		genStructure.setHullPlain(world, x + 9, y + 1, z + 10);
		genStructure.setHullPlain(world, x + 9, y + 2, z + 3);
		genStructure.setHullPlain(world, x + 9, y + 2, z + 4);
		world.setBlockState(new BlockPos(x + 9, y + 2, z + 5), Blocks.WOOL.getStateFromMeta(14), 2);
		world.setBlockState(new BlockPos(x + 9, y + 2, z + 6), Blocks.WOOL.getStateFromMeta(8), 2);
		world.setBlockState(new BlockPos(x + 9, y + 2, z + 7), Blocks.WOOL.getStateFromMeta(14), 2);
		genStructure.setHullPlain(world, x + 9, y + 2, z + 8);
		genStructure.setHullPlain(world, x + 9, y + 2, z + 9);
		world.setBlockState(new BlockPos(x + 9, y + 2, z + 10), Blocks.WOOL.getStateFromMeta(14), 2);
		genStructure.setHullPlain(world, x + 9, y + 2, z + 11);
		genStructure.setHullPlain(world, x + 9, y + 3, z + 2);
		genStructure.setHullPlain(world, x + 9, y + 3, z + 12);
		genStructure.setHullGlass(world, x + 9, y + 4, z + 2);
		genStructure.setHullGlass(world, x + 9, y + 4, z + 12);
		genStructure.setHullGlass(world, x + 9, y + 5, z + 2);
		genStructure.setHullGlass(world, x + 9, y + 5, z + 12);
		genStructure.setHullPlain(world, x + 9, y + 6, z + 3);
		if (!isCorrupted || rand.nextBoolean()) {
			world.setBlockState(new BlockPos(x + 9, y + 6, z + 7), WarpDrive.blockAirGeneratorTiered[1].getStateFromMeta(4), 2);
		}
		genStructure.setHullPlain(world, x + 9, y + 6, z + 11);
		genStructure.setHullPlain(world, x + 9, y + 7, z + 4);
		genStructure.setHullPlain(world, x + 9, y + 7, z + 5);
		genStructure.setHullPlain(world, x + 9, y + 7, z + 6);
		genStructure.setHullPlain(world, x + 9, y + 7, z + 7);
		genStructure.setHullPlain(world, x + 9, y + 7, z + 8);
		genStructure.setHullPlain(world, x + 9, y + 7, z + 9);
		genStructure.setHullPlain(world, x + 9, y + 7, z + 10);
		genStructure.setHullPlain(world, x + 9, y + 8, z + 4);
		genStructure.setHullPlain(world, x + 9, y + 8, z + 10);
		genStructure.setHullPlain(world, x + 10, y + 1, z + 4);
		genStructure.setHullPlain(world, x + 10, y + 1, z + 5);
		genStructure.setHullPlain(world, x + 10, y + 1, z + 6);
		genStructure.setHullPlain(world, x + 10, y + 1, z + 7);
		genStructure.setHullPlain(world, x + 10, y + 1, z + 8);
		genStructure.setHullPlain(world, x + 10, y + 1, z + 9);
		genStructure.setHullPlain(world, x + 10, y + 1, z + 10);
		genStructure.setHullPlain(world, x + 10, y + 2, z + 3);
		genStructure.setHullPlain(world, x + 10, y + 2, z + 4);
		world.setBlockState(new BlockPos(x + 10, y + 2, z + 5), Blocks.WOOL.getStateFromMeta(8), 2);
		world.setBlockState(new BlockPos(x + 10, y + 2, z + 6), Blocks.WOOL.getStateFromMeta(8), 2);
		genStructure.setHullPlain(world, x + 10, y + 2, z + 7);
		genStructure.setHullPlain(world, x + 10, y + 2, z + 8);
		world.setBlockState(new BlockPos(x + 10, y + 2, z + 9), Blocks.WOOL.getStateFromMeta(8), 2);
		world.setBlockState(new BlockPos(x + 10, y + 2, z + 10), Blocks.WOOL.getStateFromMeta(14), 2);
		genStructure.setHullPlain(world, x + 10, y + 2, z + 11);
		genStructure.setHullPlain(world, x + 10, y + 3, z + 2);
		genStructure.setHullPlain(world, x + 10, y + 3, z + 12);
		genStructure.setHullGlass(world, x + 10, y + 4, z + 2);
		genStructure.setHullGlass(world, x + 10, y + 4, z + 12);
		genStructure.setHullGlass(world, x + 10, y + 5, z + 2);
		genStructure.setHullGlass(world, x + 10, y + 5, z + 12);
		genStructure.setHullPlain(world, x + 10, y + 6, z + 3);
		genStructure.setHullPlain(world, x + 10, y + 6, z + 6);
		genStructure.setWiring(world, x + 10, y + 6, z + 7);
		genStructure.setHullPlain(world, x + 10, y + 6, z + 8);
		genStructure.setHullPlain(world, x + 10, y + 6, z + 11);
		genStructure.setHullPlain(world, x + 10, y + 7, z + 4);
		genStructure.setHullPlain(world, x + 10, y + 7, z + 5);
		genStructure.setHullPlain(world, x + 10, y + 7, z + 6);
		genStructure.setSolarPanel(world, x + 10, y + 7, z + 7);
		genStructure.setHullPlain(world, x + 10, y + 7, z + 8);
		genStructure.setHullPlain(world, x + 10, y + 7, z + 9);
		genStructure.setHullPlain(world, x + 10, y + 7, z + 10);
		genStructure.setHullPlain(world, x + 10, y + 8, z + 4);
		genStructure.setHullPlain(world, x + 10, y + 8, z + 10);
		genStructure.setHullPlain(world, x + 11, y + 1, z + 4);
		genStructure.setHullPlain(world, x + 11, y + 1, z + 5);
		genStructure.setHullPlain(world, x + 11, y + 1, z + 6);
		genStructure.setHullPlain(world, x + 11, y + 1, z + 7);
		genStructure.setHullPlain(world, x + 11, y + 1, z + 8);
		genStructure.setHullPlain(world, x + 11, y + 1, z + 9);
		genStructure.setHullPlain(world, x + 11, y + 1, z + 10);
		genStructure.setHullPlain(world, x + 11, y + 2, z + 3);
		genStructure.setHullPlain(world, x + 11, y + 2, z + 4);
		genStructure.setHullPlain(world, x + 11, y + 2, z + 5);
		genStructure.setHullPlain(world, x + 11, y + 2, z + 6);
		genStructure.setHullPlain(world, x + 11, y + 2, z + 7);
		genStructure.setHullPlain(world, x + 11, y + 2, z + 8);
		genStructure.setHullPlain(world, x + 11, y + 2, z + 9);
		genStructure.setHullPlain(world, x + 11, y + 2, z + 10);
		genStructure.setHullPlain(world, x + 11, y + 2, z + 11);
		genStructure.setHullPlain(world, x + 11, y + 3, z + 2);
		genStructure.setHullPlain(world, x + 11, y + 3, z + 7);
		world.setBlockState(new BlockPos(x + 11, y + 3, z + 9), Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE.getDefaultState(), 2);
		genStructure.setHullPlain(world, x + 11, y + 3, z + 12);
		genStructure.setHullGlass(world, x + 11, y + 4, z + 2);
		genStructure.setComputerFloppy(world, x + 11, y + 4, z + 6);
		genStructure.setComputerScreen(world, x + 11, y + 4, z + 7);
		genStructure.setComputerKeyboard(world, x + 11, y + 4, z + 8);
		genStructure.setHullGlass(world, x + 11, y + 4, z + 12);
		genStructure.setHullGlass(world, x + 11, y + 5, z + 2);
		genStructure.setHullGlass(world, x + 11, y + 5, z + 6);
		genStructure.setComputerCore(world, x + 11, y + 5, z + 7);
		genStructure.setHullGlass(world, x + 11, y + 5, z + 8);
		genStructure.setHullGlass(world, x + 11, y + 5, z + 12);
		genStructure.setHullPlain(world, x + 11, y + 6, z + 3);
		genStructure.setHullPlain(world, x + 11, y + 6, z + 5);
		genStructure.setWiring(world, x + 11, y + 6, z + 6);
		genStructure.setWiring(world, x + 11, y + 6, z + 7);
		genStructure.setWiring(world, x + 11, y + 6, z + 8);
		genStructure.setHullPlain(world, x + 11, y + 6, z + 9);
		genStructure.setHullPlain(world, x + 11, y + 6, z + 11);
		genStructure.setHullPlain(world, x + 11, y + 7, z + 4);
		genStructure.setHullPlain(world, x + 11, y + 7, z + 5);
		genStructure.setSolarPanel(world, x + 11, y + 7, z + 6);
		genStructure.setSolarPanel(world, x + 11, y + 7, z + 7);
		genStructure.setSolarPanel(world, x + 11, y + 7, z + 8);
		genStructure.setHullPlain(world, x + 11, y + 7, z + 9);
		genStructure.setHullPlain(world, x + 11, y + 7, z + 10);
		genStructure.setHullPlain(world, x + 11, y + 8, z + 4);
		genStructure.setHullPlain(world, x + 11, y + 8, z + 10);
		if (hasWings) {
			genStructure.setHullPlain(world, x + 11, y + 8, z + 3);
			genStructure.setHullPlain(world, x + 11, y + 8, z + 11);
		} else {
			genStructure.setHullPlain(world, x + 11, y + 9, z + 4);
			genStructure.setHullPlain(world, x + 11, y + 9, z + 10);
		}
		genStructure.setHullPlain(world, x + 12, y + 1, z + 4);
		genStructure.setHullPlain(world, x + 12, y + 1, z + 5);
		genStructure.setHullPlain(world, x + 12, y + 1, z + 6);
		genStructure.setHullPlain(world, x + 12, y + 1, z + 7);
		genStructure.setHullPlain(world, x + 12, y + 1, z + 8);
		genStructure.setHullPlain(world, x + 12, y + 1, z + 10);
		genStructure.setHullPlain(world, x + 12, y + 2, z + 3);
		genStructure.setHullPlain(world, x + 12, y + 2, z + 4);
		genStructure.setHullPlain(world, x + 12, y + 2, z + 5);
		genStructure.setHullPlain(world, x + 12, y + 2, z + 6);
		genStructure.setWiring(world, x + 12, y + 2, z + 7);
		genStructure.setWiring(world, x + 12, y + 2, z + 8);
		if (!isCorrupted || rand.nextBoolean()) {
			world.setBlockState(new BlockPos(x + 12, y + 2, z + 9), WarpDrive.blockLift.getDefaultState());
			if (isCreative) {// fill with energy
				final TileEntity tileEntity = world.getTileEntity(new BlockPos(x + 12, y + 2, z + 9));
				if (tileEntity instanceof TileEntityAbstractEnergy) {
					((TileEntityAbstractEnergy) tileEntity).energy_consume(-((TileEntityAbstractEnergy) tileEntity).energy_getMaxStorage());
				}
			}
		}
		genStructure.setHullPlain(world, x + 12, y + 2, z + 10);
		genStructure.setHullPlain(world, x + 12, y + 2, z + 11);
		genStructure.setHullPlain(world, x + 12, y + 3, z + 2);
		genStructure.setHullPlain(world, x + 12, y + 3, z + 6);
		genStructure.setWiring(world, x + 12, y + 3, z + 7);
		genStructure.setHullPlain(world, x + 12, y + 3, z + 8);
		genStructure.setHullPlain(world, x + 12, y + 3, z + 12);
		genStructure.setHullGlass(world, x + 12, y + 4, z + 2);
		genStructure.setHullPlain(world, x + 12, y + 4, z + 6);
		genStructure.setHullPlain(world, x + 12, y + 4, z + 7);
		genStructure.setHullPlain(world, x + 12, y + 4, z + 8);
		genStructure.setHullGlass(world, x + 12, y + 4, z + 12);
		genStructure.setHullGlass(world, x + 12, y + 5, z + 2);
		genStructure.setHullGlass(world, x + 12, y + 5, z + 6);
		if (!isCorrupted || rand.nextBoolean()) {
			world.setBlockState(new BlockPos(x + 12, y + 5, z + 7),
			                    WarpDrive.blockShipCores[1].getDefaultState().withProperty(BlockProperties.FACING_HORIZONTAL, EnumFacing.WEST));
			if (isCreative) {// fill with energy
				final TileEntity tileEntity = world.getTileEntity(new BlockPos(x + 12, y + 5, z + 7));
				if (tileEntity instanceof TileEntityAbstractEnergy) {
					((TileEntityAbstractEnergy) tileEntity).energy_consume( - ((TileEntityAbstractEnergy) tileEntity).energy_getMaxStorage() / 2);
				}
			}
		}
		genStructure.setHullGlass(world, x + 12, y + 5, z + 8);
		genStructure.setHullGlass(world, x + 12, y + 5, z + 12);
		genStructure.setHullPlain(world, x + 12, y + 6, z + 3);
		genStructure.setHullPlain(world, x + 12, y + 6, z + 4);
		genStructure.setHullPlain(world, x + 12, y + 6, z + 5);
		genStructure.setWiring(world, x + 12, y + 6, z + 7);
		genStructure.setHullPlain(world, x + 12, y + 6, z + 9);
		genStructure.setHullPlain(world, x + 12, y + 6, z + 10);
		genStructure.setHullPlain(world, x + 12, y + 6, z + 11);
		genStructure.setHullPlain(world, x + 12, y + 7, z + 5);
		genStructure.setHullPlain(world, x + 12, y + 7, z + 6);
		genStructure.setSolarPanel(world, x + 12, y + 7, z + 7);
		genStructure.setHullPlain(world, x + 12, y + 7, z + 8);
		genStructure.setHullPlain(world, x + 12, y + 7, z + 9);
		genStructure.setHullPlain(world, x + 13, y + 1, z + 4);
		genStructure.setHullPlain(world, x + 13, y + 1, z + 5);
		genStructure.setHullPlain(world, x + 13, y + 1, z + 6);
		genStructure.setHullPlain(world, x + 13, y + 1, z + 7);
		genStructure.setHullPlain(world, x + 13, y + 1, z + 8);
		genStructure.setHullPlain(world, x + 13, y + 1, z + 9);
		genStructure.setHullPlain(world, x + 13, y + 1, z + 10);
		genStructure.setHullPlain(world, x + 13, y + 2, z + 3);
		genStructure.setHullPlain(world, x + 13, y + 2, z + 4);
		genStructure.setHullPlain(world, x + 13, y + 2, z + 5);
		genStructure.setHullPlain(world, x + 13, y + 2, z + 6);
		genStructure.setHullPlain(world, x + 13, y + 2, z + 7);
		genStructure.setHullPlain(world, x + 13, y + 2, z + 8);
		genStructure.setHullPlain(world, x + 13, y + 2, z + 9);
		genStructure.setHullPlain(world, x + 13, y + 2, z + 10);
		genStructure.setHullPlain(world, x + 13, y + 2, z + 11);
		genStructure.setHullPlain(world, x + 13, y + 3, z + 2);
		if (rand.nextBoolean()) {
			genStructure.setHullPlain(world, x + 13, y + 3, z + 3);
			genStructure.setHullGlass(world, x + 13, y + 4, z + 3);
		} else if (!isCorrupted || rand.nextBoolean()) {
			world.setBlockState(new BlockPos(x + 13, y + 3, z + 3), WarpDrive.blockAirShield.getDefaultState());
			world.setBlockState(new BlockPos(x + 13, y + 4, z + 3), WarpDrive.blockAirShield.getDefaultState());
		}
		genStructure.setHullPlain(world, x + 13, y + 3, z + 4);
		genStructure.setHullPlain(world, x + 13, y + 3, z + 5);
		genStructure.setHullPlain(world, x + 13, y + 3, z + 6);
		genStructure.setWiring(world, x + 13, y + 3, z + 7);
		genStructure.setHullPlain(world, x + 13, y + 3, z + 8);
		genStructure.setHullPlain(world, x + 13, y + 3, z + 9);
		genStructure.setHullPlain(world, x + 13, y + 3, z + 10);
		if (rand.nextBoolean()) {
			genStructure.setHullPlain(world, x + 13, y + 3, z + 11);
			genStructure.setHullGlass(world, x + 13, y + 4, z + 11);
		} else if (!isCorrupted || rand.nextBoolean()) {
			world.setBlockState(new BlockPos(x + 13, y + 3, z + 11), WarpDrive.blockAirShield.getDefaultState());
			world.setBlockState(new BlockPos(x + 13, y + 4, z + 11), WarpDrive.blockAirShield.getDefaultState());
		}
		genStructure.setHullPlain(world, x + 13, y + 3, z + 12);
		genStructure.setHullPlain(world, x + 13, y + 4, z + 2);
		genStructure.setHullPlain(world, x + 13, y + 4, z + 4);
		genStructure.setHullPlain(world, x + 13, y + 4, z + 5);
		genStructure.setHullPlain(world, x + 13, y + 4, z + 6);
		genStructure.setWiring(world, x + 13, y + 4, z + 7);
		genStructure.setHullPlain(world, x + 13, y + 4, z + 8);
		genStructure.setHullPlain(world, x + 13, y + 4, z + 9);
		genStructure.setHullPlain(world, x + 13, y + 4, z + 10);
		genStructure.setHullPlain(world, x + 13, y + 4, z + 12);
		genStructure.setHullPlain(world, x + 13, y + 5, z + 2);
		genStructure.setHullPlain(world, x + 13, y + 5, z + 3);
		world.setBlockState(new BlockPos(x + 13, y + 5, z + 4), Blocks.GLOWSTONE.getDefaultState());
		genStructure.setHullPlain(world, x + 13, y + 5, z + 5);
		genStructure.setHullPlain(world, x + 13, y + 5, z + 6);
		genStructure.setWiring(world, x + 13, y + 5, z + 7);
		genStructure.setHullPlain(world, x + 13, y + 5, z + 8);
		genStructure.setHullPlain(world, x + 13, y + 5, z + 9);
		world.setBlockState(new BlockPos(x + 13, y + 5, z + 10), Blocks.GLOWSTONE.getDefaultState());
		genStructure.setHullPlain(world, x + 13, y + 5, z + 11);
		genStructure.setHullPlain(world, x + 13, y + 5, z + 12);
		genStructure.setHullPlain(world, x + 13, y + 6, z + 3);
		genStructure.setHullPlain(world, x + 13, y + 6, z + 4);
		genStructure.setHullPlain(world, x + 13, y + 6, z + 5);
		genStructure.setHullPlain(world, x + 13, y + 6, z + 6);
		genStructure.setWiring(world, x + 13, y + 6, z + 7);
		genStructure.setHullPlain(world, x + 13, y + 6, z + 8);
		genStructure.setHullPlain(world, x + 13, y + 6, z + 9);
		genStructure.setHullPlain(world, x + 13, y + 6, z + 10);
		genStructure.setHullPlain(world, x + 13, y + 6, z + 11);
		genStructure.setHullPlain(world, x + 13, y + 7, z + 6);
		genStructure.setHullPlain(world, x + 13, y + 7, z + 7);
		genStructure.setHullPlain(world, x + 13, y + 7, z + 8);
		genStructure.setHullPlain(world, x + 14, y + 2, z + 3);
		genStructure.setHullPlain(world, x + 14, y + 2, z + 4);
		genStructure.setHullPlain(world, x + 14, y + 2, z + 10);
		genStructure.setHullPlain(world, x + 14, y + 2, z + 11);
		genStructure.setHullPlain(world, x + 14, y + 3, z + 4);
		genStructure.setHullPlain(world, x + 14, y + 3, z + 5);
		genStructure.setHullPlain(world, x + 14, y + 3, z + 6);
		genStructure.setHullPlain(world, x + 14, y + 3, z + 7);
		genStructure.setHullPlain(world, x + 14, y + 3, z + 8);
		genStructure.setHullPlain(world, x + 14, y + 3, z + 9);
		genStructure.setHullPlain(world, x + 14, y + 3, z + 10);
		genStructure.setHullPlain(world, x + 14, y + 4, z + 4);
		genStructure.setPropulsion(world, x + 14, y + 4, z + 5);
		genStructure.setPropulsion(world, x + 14, y + 4, z + 6);
		genStructure.setHullPlain(world, x + 14, y + 4, z + 7);
		genStructure.setPropulsion(world, x + 14, y + 4, z + 8);
		genStructure.setPropulsion(world, x + 14, y + 4, z + 9);
		genStructure.setHullPlain(world, x + 14, y + 4, z + 10);
		genStructure.setHullPlain(world, x + 14, y + 5, z + 4);
		genStructure.setPropulsion(world, x + 14, y + 5, z + 5);
		genStructure.setPropulsion(world, x + 14, y + 5, z + 6);
		genStructure.setHullPlain(world, x + 14, y + 5, z + 7);
		genStructure.setPropulsion(world, x + 14, y + 5, z + 8);
		genStructure.setPropulsion(world, x + 14, y + 5, z + 9);
		genStructure.setHullPlain(world, x + 14, y + 5, z + 10);
		genStructure.setHullPlain(world, x + 14, y + 6, z + 4);
		genStructure.setHullPlain(world, x + 14, y + 6, z + 5);
		genStructure.setHullPlain(world, x + 14, y + 6, z + 6);
		genStructure.setHullPlain(world, x + 14, y + 6, z + 7);
		genStructure.setHullPlain(world, x + 14, y + 6, z + 8);
		genStructure.setHullPlain(world, x + 14, y + 6, z + 9);
		genStructure.setHullPlain(world, x + 14, y + 6, z + 10);
		genStructure.setHullPlain(world, x + 15, y + 2, z + 3);
		genStructure.setHullPlain(world, x + 15, y + 2, z + 4);
		genStructure.setHullPlain(world, x + 15, y + 2, z + 10);
		genStructure.setHullPlain(world, x + 15, y + 2, z + 11);
		genStructure.setHullPlain(world, x + 15, y + 3, z + 4);
		genStructure.setHullPlain(world, x + 15, y + 3, z + 10);
		genStructure.setHullPlain(world, x + 15, y + 4, z + 7);
		genStructure.setPropulsion(world, x + 15, y + 5, z + 7);
		genStructure.setHullPlain(world, x + 15, y + 6, z + 4);
		genStructure.setHullPlain(world, x + 15, y + 6, z + 7);
		genStructure.setHullPlain(world, x + 15, y + 6, z + 10);
		genStructure.setHullPlain(world, x + 16, y + 4, z + 7);
		genStructure.setPropulsion(world, x + 16, y + 5, z + 7);
		genStructure.setHullPlain(world, x + 16, y + 6, z + 7);
		genStructure.setHullPlain(world, x + 17, y + 5, z + 7);
		spawnNPC(world, x + 9, y + 3, z + 5);
		return true;
	}
	
	private static void spawnNPC(final World world, final int x, final int y, final int z) {
		final int countMobs = 2 + world.rand.nextInt(10);
		
		if (world.rand.nextBoolean()) {// Villagers
			for (int idx = 0; idx < countMobs; idx++) {
				final EntityVillager entityVillager = new EntityVillager(world, 0);
				entityVillager.setLocationAndAngles(x + 0.5D, y, z + 0.5D, 0.0F, 0.0F);
				entityVillager.setItemStackToSlot(EntityEquipmentSlot.HEAD, new ItemStack(WarpDrive.itemWarpArmor[EnumTier.BASIC.getIndex()][3], 1, 1));
				world.spawnEntity(entityVillager);
			}
		} else if (world.rand.nextBoolean()) {// Zombies
			for (int idx = 0; idx < countMobs; idx++) {
				final EntityZombie entityZombie = new EntityZombie(world);
				entityZombie.setLocationAndAngles(x + 0.5D, y, z + 0.5D, 0.0F, 0.0F);
				world.spawnEntity(entityZombie);
			}
		} else {// Zombie pigmen
			for (int idx = 0; idx < countMobs; idx++) {
				final EntityPigZombie entityZombie = new EntityPigZombie(world);
				entityZombie.setLocationAndAngles(x + 0.5D, y, z + 0.5D, 0.0F, 0.0F);
				world.spawnEntity(entityZombie);
			}
		}
	}
}
