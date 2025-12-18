package cr0s.warpdrive.config;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.ParticleRegistry;
import cr0s.warpdrive.block.decoration.BlockDecorative;
import cr0s.warpdrive.data.EnumAirTankTier;
import cr0s.warpdrive.data.EnumComponentType;
import cr0s.warpdrive.data.EnumDecorativeType;
import cr0s.warpdrive.data.EnumForceFieldShape;
import cr0s.warpdrive.data.EnumForceFieldUpgrade;
import cr0s.warpdrive.data.EnumTier;
import cr0s.warpdrive.item.ItemComponent;
import cr0s.warpdrive.item.ItemElectromagneticCell;
import cr0s.warpdrive.item.ItemForceFieldShape;
import cr0s.warpdrive.item.ItemForceFieldUpgrade;
import cr0s.warpdrive.item.ItemTuningDriver;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map.Entry;

import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.EnumDyeColor;

import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;

import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;
import net.minecraftforge.registries.ForgeRegistry;

/**
 * Hold the different recipe sets
 */
public class Recipes {
	
	private static final ResourceLocation groupComponents   = new ResourceLocation("components");
	private static final ResourceLocation groupDecorations  = new ResourceLocation("decoration");
	private static final ResourceLocation groupMachines     = new ResourceLocation("machines");
	private static final ResourceLocation groupTools        = new ResourceLocation("tools");
	
	private static final ResourceLocation groupHulls        = new ResourceLocation("hulls");
	private static final ResourceLocation groupTaintedHulls = new ResourceLocation("tainted_hulls");
	
	public static final HashMap<EnumDyeColor, String> oreDyes = new HashMap<>(16);
	static {
		oreDyes.put(EnumDyeColor.WHITE     , "dyeWhite");
		oreDyes.put(EnumDyeColor.ORANGE    , "dyeOrange");
		oreDyes.put(EnumDyeColor.MAGENTA   , "dyeMagenta");
		oreDyes.put(EnumDyeColor.LIGHT_BLUE, "dyeLightBlue");
		oreDyes.put(EnumDyeColor.YELLOW    , "dyeYellow");
		oreDyes.put(EnumDyeColor.LIME      , "dyeLime");
		oreDyes.put(EnumDyeColor.PINK      , "dyePink");
		oreDyes.put(EnumDyeColor.GRAY      , "dyeGray");
		oreDyes.put(EnumDyeColor.SILVER    , "dyeLightGray");
		oreDyes.put(EnumDyeColor.CYAN      , "dyeCyan");
		oreDyes.put(EnumDyeColor.PURPLE    , "dyePurple");
		oreDyes.put(EnumDyeColor.BLUE      , "dyeBlue");
		oreDyes.put(EnumDyeColor.BROWN     , "dyeBrown");
		oreDyes.put(EnumDyeColor.GREEN     , "dyeGreen");
		oreDyes.put(EnumDyeColor.RED       , "dyeRed");
		oreDyes.put(EnumDyeColor.BLACK     , "dyeBlack");
	}
	
	private static ItemStack[] itemStackMachineCasings;
	private static ItemStack[] itemStackMotors;
	private static Object barsIron;
	private static Object ingotIronOrSteel;
	private static Object rubber;
	private static Object goldNuggetOrBasicCircuit;
	private static Object goldIngotOrAdvancedCircuit;
	private static Object emeraldOrSuperiorCircuit;
	
	public static void initOreDictionary() {
		// vanilla
		registerOreDictionary("blockMushroom", new ItemStack(Blocks.BROWN_MUSHROOM_BLOCK));
		registerOreDictionary("blockMushroom", new ItemStack(Blocks.RED_MUSHROOM_BLOCK));
		
		// components
		registerOreDictionary("itemRubber", ItemComponent.getItemStack(EnumComponentType.RUBBER));
		registerOreDictionary("itemBiofiber", ItemComponent.getItemStack(EnumComponentType.BIOFIBER));
		registerOreDictionary("itemCeramic", ItemComponent.getItemStack(EnumComponentType.CERAMIC));
		registerOreDictionary("plateCarbon", ItemComponent.getItemStack(EnumComponentType.CARBON_FIBER));
		
		// air shields
		for (final EnumDyeColor enumDyeColor : EnumDyeColor.values()) {
			registerOreDictionary("blockAirShield", new ItemStack(WarpDrive.blockAirShield, 1, enumDyeColor.getMetadata()));
		}
		
		// decoration
		registerOreDictionary("warpDecorative", BlockDecorative.getItemStack(EnumDecorativeType.PLAIN));
		registerOreDictionary("warpDecorative", BlockDecorative.getItemStack(EnumDecorativeType.GLASS));
		registerOreDictionary("warpDecorative", BlockDecorative.getItemStack(EnumDecorativeType.GRATED));
		registerOreDictionary("warpDecorative", BlockDecorative.getItemStack(EnumDecorativeType.STRIPES_BLACK_DOWN));
		registerOreDictionary("warpDecorative", BlockDecorative.getItemStack(EnumDecorativeType.STRIPES_BLACK_UP));
		registerOreDictionary("warpDecorative", BlockDecorative.getItemStack(EnumDecorativeType.STRIPES_YELLOW_DOWN));
		registerOreDictionary("warpDecorative", BlockDecorative.getItemStack(EnumDecorativeType.STRIPES_YELLOW_UP));
		
		// tuning fork
		for (int dyeColor = 0; dyeColor < 16; dyeColor++) {
			registerOreDictionary("itemTuningFork", new ItemStack(WarpDrive.itemTuningFork, 1, dyeColor));
		}
		
		// accelerator
		if (WarpDriveConfig.ACCELERATOR_ENABLE) {
			registerOreDictionary("blockVoidShell", new ItemStack(WarpDrive.blockVoidShellPlain, 1));
			registerOreDictionary("blockVoidShell", new ItemStack(WarpDrive.blockVoidShellGlass, 1));
			for (final EnumTier enumTier : EnumTier.nonCreative()) {
				final int index = enumTier.getIndex();
				registerOreDictionary("blockElectromagnet" + index, new ItemStack(WarpDrive.blockElectromagnets_plain[index], 1));
				registerOreDictionary("blockElectromagnet" + index, new ItemStack(WarpDrive.blockElectromagnets_glass[index], 1));
			}
		}
		
		// hull
		for (final EnumTier enumTier : EnumTier.nonCreative()) {
			final int index = enumTier.getIndex();
			for (int woolColor = 0; woolColor < 16; woolColor++) {
				registerOreDictionary("blockHull" + index + "_plain", new ItemStack(WarpDrive.blockHulls_plain[index][0], 1, woolColor));
				registerOreDictionary("blockHull" + index + "_glass", new ItemStack(WarpDrive.blockHulls_glass[index], 1, woolColor));
				registerOreDictionary("blockHull" + index + "_stairs", new ItemStack(WarpDrive.blockHulls_stairs[index][woolColor], 1));
				registerOreDictionary("blockHull" + index + "_tiled", new ItemStack(WarpDrive.blockHulls_plain[index][1], 1, woolColor));
				registerOreDictionary("blockHull" + index + "_slab", new ItemStack(WarpDrive.blockHulls_slab[index][woolColor], 1, 0));
				registerOreDictionary("blockHull" + index + "_slab", new ItemStack(WarpDrive.blockHulls_slab[index][woolColor], 1, 2));
				registerOreDictionary("blockHull" + index + "_slab", new ItemStack(WarpDrive.blockHulls_slab[index][woolColor], 1, 6));
				registerOreDictionary("blockHull" + index + "_slab", new ItemStack(WarpDrive.blockHulls_slab[index][woolColor], 1, 8));
				registerOreDictionary("blockHull" + index + "_omnipanel", new ItemStack(WarpDrive.blockHulls_omnipanel[index], 1, woolColor));
			}
		}
		
		// Add Reinforced iridium plate to ore registry as applicable (it's missing in IC2 without GregTech)
		if (!OreDictionary.doesOreNameExist("plateAlloyIridium") || OreDictionary.getOres("plateAlloyIridium").isEmpty()) {
			if (WarpDriveConfig.isIndustrialCraft2Loaded) {
				final ItemStack iridiumAlloy = (ItemStack) WarpDriveConfig.getOreOrItemStack("ic2:crafting", 4,     // IC2 Experimental Iridium alloy plate
				                                                                             "ic2:itemmisc", 258 ); // IC2 Classic Iridium plate
				OreDictionary.registerOre("plateAlloyIridium", iridiumAlloy);
			}
		}
	}
	
	private static void registerOreDictionary(final String name, @Nonnull final ItemStack itemStack) {
		if (!itemStack.isEmpty()) {
			OreDictionary.registerOre(name, itemStack);
		}
	}
	
	private static void initIngredients() {
		// Get the machine casing to use
		final ItemStack itemStackMachineCasingLV;
		final ItemStack itemStackMachineCasingMV;
		final ItemStack itemStackMachineCasingHV;
		final ItemStack itemStackMachineCasingEV;
		ItemStack itemStackMotorLV = ItemComponent.getItemStack(EnumComponentType.MOTOR);
		ItemStack itemStackMotorMV = ItemComponent.getItemStack(EnumComponentType.MOTOR);
		ItemStack itemStackMotorHV = ItemComponent.getItemStack(EnumComponentType.MOTOR);
		ItemStack itemStackMotorEV = ItemComponent.getItemStack(EnumComponentType.MOTOR);
		
		if (WarpDriveConfig.isGregtechLoaded) {
			itemStackMachineCasingLV = WarpDriveConfig.getItemStackOrFire("gregtech:machine_casing", 1); // LV machine casing (Steel)
			itemStackMachineCasingMV = WarpDriveConfig.getItemStackOrFire("gregtech:machine_casing", 2); // MV machine casing (Aluminium)
			itemStackMachineCasingHV = WarpDriveConfig.getItemStackOrFire("gregtech:machine_casing", 3); // HV machine casing (Stainless Steel)
			itemStackMachineCasingEV = WarpDriveConfig.getItemStackOrFire("gregtech:machine_casing", 4); // EV machine casing (Titanium)
			
			itemStackMotorLV = WarpDriveConfig.getItemStackOrFire("gregtech:meta_item_1", 127); // LV Motor
			itemStackMotorMV = WarpDriveConfig.getItemStackOrFire("gregtech:meta_item_1", 128); // MV Motor
			itemStackMotorHV = WarpDriveConfig.getItemStackOrFire("gregtech:meta_item_1", 129); // HV Motor
			itemStackMotorEV = WarpDriveConfig.getItemStackOrFire("gregtech:meta_item_1", 130); // EV Motor
			
		} else if (WarpDriveConfig.isIndustrialCraft2Loaded) {
			itemStackMachineCasingLV = (ItemStack) WarpDriveConfig.getOreOrItemStack("ic2:resource", 12,       // IC2 Experimental Basic machine casing
			                                                                         "ic2:blockmachinelv", 0); // IC2 Classic Machine block
			itemStackMachineCasingMV = (ItemStack) WarpDriveConfig.getOreOrItemStack("ic2:resource", 13,       // IC2 Experimental Advanced machine casing
			                                                                         "ic2:blackmachinemv", 0); // IC2 Classic Advanced machine block
			itemStackMachineCasingHV = new ItemStack(WarpDrive.blockHighlyAdvancedMachine);
			itemStackMachineCasingEV = new ItemStack(WarpDrive.blockHighlyAdvancedMachine);
			
			final ItemStack itemStackMotor = WarpDriveConfig.getItemStackOrFire("ic2:crafting", 6); // IC2 Experimental Electric motor
			if (!itemStackMotor.isEmpty()) {
				itemStackMotorHV = itemStackMotor;
				itemStackMotorEV = itemStackMotor;
			}
			
			WarpDrive.register(new ShapedOreRecipe(groupComponents,
			                                       new ItemStack(WarpDrive.blockHighlyAdvancedMachine), false, "iii", "imi", "iii",
			                                       'i', "plateAlloyIridium",
			                                       'm', itemStackMachineCasingMV));
			
		} else if (WarpDriveConfig.isThermalFoundationLoaded) {
			// These are upgrade kits, there is only 1 machine frame tier as of Thermal Foundation 1.12.2-5.5.0.29
			itemStackMachineCasingLV = WarpDriveConfig.getItemStackOrFire("thermalfoundation:upgrade", 0);
			itemStackMachineCasingMV = WarpDriveConfig.getItemStackOrFire("thermalfoundation:upgrade", 1);
			itemStackMachineCasingHV = WarpDriveConfig.getItemStackOrFire("thermalfoundation:upgrade", 2);
			itemStackMachineCasingEV = WarpDriveConfig.getItemStackOrFire("thermalfoundation:upgrade", 3);
			
		} else if (WarpDriveConfig.isEnderIOLoaded) {
			// As of EnderIO on MC 1.12.2 there are 5 machine chassis
			itemStackMachineCasingLV = WarpDriveConfig.getItemStackOrFire("enderio:item_material", 0);     // Simple Machine chassis
			itemStackMachineCasingMV = WarpDriveConfig.getItemStackOrFire("enderio:item_material", 1);     // Industrial Machine chassis
			itemStackMachineCasingHV = WarpDriveConfig.getItemStackOrFire("enderio:item_material", 54);    // Enhanced Machine chassis
			itemStackMachineCasingEV = WarpDriveConfig.getItemStackOrFire("enderio:item_material", 55);    // Soulless Machine chassis
			// itemStackMachineCasingEV = WarpDriveConfig.getItemStackOrFire("enderio:item_material", 53);    // Soul Machine chassis
			
		} else {// vanilla
			itemStackMachineCasingLV = new ItemStack(Blocks.IRON_BLOCK);
			itemStackMachineCasingMV = new ItemStack(Blocks.DIAMOND_BLOCK);
			itemStackMachineCasingHV = new ItemStack(WarpDrive.blockHighlyAdvancedMachine);
			itemStackMachineCasingEV = new ItemStack(Blocks.BEACON);
			
			WarpDrive.register(new ShapedOreRecipe(groupComponents,
			                                       new ItemStack(WarpDrive.blockHighlyAdvancedMachine, 4), "pep", "ede", "pep",
			                                       'e', Items.EMERALD,
			                                       'p', Items.ENDER_EYE,
			                                       'd', Blocks.DIAMOND_BLOCK));
		}
		
		// @TODO implement machine casing from hull + iron bars/etc., gives 2
		
		itemStackMachineCasings = new ItemStack[] { itemStackMachineCasingLV, itemStackMachineCasingMV, itemStackMachineCasingHV, itemStackMachineCasingEV };
		itemStackMotors = new ItemStack[] { itemStackMotorLV, itemStackMotorMV, itemStackMotorHV, itemStackMotorEV };
		
		// integrate with iron bars from all mods
		barsIron = WarpDriveConfig.getOreOrItemStack(
				"ore:barsIron", 0,
				"minecraft:iron_bars", 0 );
		
		// integrate with steel and aluminium ingots from all mods
		ingotIronOrSteel = WarpDriveConfig.getOreOrItemStack(
				"ore:ingotSteel", 0,
				"ore:ingotAluminium", 0,
				"ore:ingotAluminum", 0,
				"ore:ingotIron", 0 );
		
		// integrate with rubber from all mods
		rubber = WarpDriveConfig.getOreOrItemStack(
				"ore:plateRubber", 0,            // comes with GregTech
				"ore:itemRubber", 0 );           // comes with WarpDrive, IndustrialCraft2, IndustrialForegoing, TechReborn
		
		// integrate with circuits from all mods
		goldNuggetOrBasicCircuit = WarpDriveConfig.getOreOrItemStack(
				"ore:circuitBasic", 0,           // comes with IndustrialCraft2, Mekanism, VoltzEngine
				"ore:nuggetGold", 0 );
		goldIngotOrAdvancedCircuit = WarpDriveConfig.getOreOrItemStack(
				"ore:circuitAdvanced", 0,        // comes with IndustrialCraft2, Mekanism, VoltzEngine
				"ore:ingotGold", 0 );
		emeraldOrSuperiorCircuit = WarpDriveConfig.getOreOrItemStack(
				"ore:circuitElite", 0,           // comes with Mekanism, VoltzEngine
				"ore:gemEmerald", 0 );
		
		// Iridium block is just that
		if (WarpDriveConfig.isGregtechLoaded) {
			WarpDrive.register(new ShapedOreRecipe(groupComponents,
			                                       new ItemStack(WarpDrive.blockIridium), "iii", "iii", "iii",
			                                       'i', "plateIridium"));
			final ItemStack itemStackIridiumAlloy = WarpDriveConfig.getOreDictionaryEntry("plateIridium");
			WarpDrive.register(new ShapelessOreRecipe(groupComponents,
			                                          new ItemStack(itemStackIridiumAlloy.getItem(), 9), new ItemStack(WarpDrive.blockIridium)));
			
		} else if (OreDictionary.doesOreNameExist("plateAlloyIridium") && !OreDictionary.getOres("plateAlloyIridium").isEmpty()) {// IC2
			WarpDrive.register(new ShapedOreRecipe(groupComponents,
			                                       new ItemStack(WarpDrive.blockIridium), "iii", "iii", "iii",
			                                       'i', "plateAlloyIridium"));
			final ItemStack itemStackIridiumAlloy = WarpDriveConfig.getOreDictionaryEntry("plateAlloyIridium");
			WarpDrive.register(new ShapelessOreRecipe(groupComponents,
			                                          new ItemStack(itemStackIridiumAlloy.getItem(), 9),
			                                          new ItemStack(WarpDrive.blockIridium)));
			
		} else if ( WarpDriveConfig.isThermalFoundationLoaded
		         || WarpDriveConfig.isEnderIOLoaded ) {// give alternate options when IC2 & GregTech are missing
			// Warning: because those are alternatives, we're not giving an uncrafting recipe, nor registering the iridium block to the ore dictionary
			
			if (WarpDriveConfig.isThermalFoundationLoaded) {
				WarpDrive.register(new ShapedOreRecipe(groupComponents,
				                                       new ItemStack(WarpDrive.blockIridium, 2), "ses", "ele", "ses",
				                                       'l', "ingotLumium",
				                                       's', "ingotSignalum",
				                                       'e', "ingotEnderium"), "_thermal");
			}
			
			if (OreDictionary.doesOreNameExist("plateIridium") && !OreDictionary.getOres("plateIridium").isEmpty()) {// ThermalFoundation
				WarpDrive.register(new ShapedOreRecipe(groupComponents,
				                                       new ItemStack(WarpDrive.blockIridium), "iii", "iii", "iii",
				                                       'i', "plateIridium"), "_plates");
			}
			
			if (WarpDriveConfig.isEnderIOLoaded) {
				final ItemStack itemStackVibrantAlloy = WarpDriveConfig.getItemStackOrFire("enderio:item_alloy_ingot", 2);
				final ItemStack itemStackRedstoneAlloy = WarpDriveConfig.getItemStackOrFire("enderio:item_alloy_ingot", 3);
				final ItemStack itemStackFranckNZombie = WarpDriveConfig.getItemStackOrFire("enderio:item_material", 42);
				WarpDrive.register(new ShapedOreRecipe(groupComponents,
				                                       new ItemStack(WarpDrive.blockIridium, 4), "ses", "ele", "ses",
				                                       'l', itemStackFranckNZombie,
				                                       's', itemStackRedstoneAlloy,
				                                       'e', itemStackVibrantAlloy), "_enderio");
			}
			
		} else {
			WarpDrive.register(new ShapedOreRecipe(groupComponents,
			                                       new ItemStack(WarpDrive.blockIridium), "ded", "yty", "ded",
			                                       't', Items.GHAST_TEAR,
			                                       'd', Items.DIAMOND,
			                                       'e', Items.EMERALD,
			                                       'y', Items.ENDER_EYE));
		}
		
		// *** Laser medium
		// basic    is 2 dyes, 1 water bottle, 1 gold nugget, 1 LV casing
		// advanced is 2 redstone dust, 1 awkward potion, 2 lapis, 1 glass tank,  1 power interface, 1 MV casing
		// superior is 1 laser medium (empty), 4 redstone blocks, 4 lapis blocks
		final ItemStack itemStackWaterBottle = WarpDriveConfig.getItemStackOrFire("minecraft:potion", 0, "{Potion: \"minecraft:water\"}");
		final ItemStack itemStackAwkwardPotion = WarpDriveConfig.getItemStackOrFire("minecraft:potion", 0, "{Potion: \"minecraft:awkward\"}");
		WarpDrive.register(new RecipeParticleShapedOre(groupMachines,
		                                               new ItemStack(WarpDrive.blockLaserMediums[EnumTier.BASIC.getIndex()]), false, "   ", "dwd", "pm ",
		                                               'd', "dye",
		                                               'w', itemStackWaterBottle,
		                                               'p', "nuggetGold",
		                                               'm', itemStackMachineCasings[0] ));
		WarpDrive.register(new RecipeParticleShapedOre(groupMachines,
		                                               new ItemStack(WarpDrive.blockLaserMediums[EnumTier.ADVANCED.getIndex()]), false, "rAr", "lBl", "pm ",
		                                               'r', "dustRedstone",
		                                               'A', itemStackAwkwardPotion,
		                                               'l', "gemLapis",
		                                               'B', ItemComponent.getItemStack(EnumComponentType.GLASS_TANK),
		                                               'p', ItemComponent.getItemStack(EnumComponentType.POWER_INTERFACE),
		                                               'm', itemStackMachineCasings[1] ));
		WarpDrive.register(new ShapedOreRecipe(groupMachines,
		                                       new ItemStack(WarpDrive.blockLaserMediums[EnumTier.SUPERIOR.getIndex()]), false, "lrl", "rmr", "lrl",
		                                       'm', ItemComponent.getItemStack(EnumComponentType.LASER_MEDIUM_EMPTY),
		                                       'r', "blockRedstone",
		                                       'l', "blockLapis"));
		
		// *** Security Station
		WarpDrive.register(new ShapedOreRecipe(groupMachines,
		                                       new ItemStack(WarpDrive.blockSecurityStation), "ede", "eme", "eMe",
		                                       'd', ItemComponent.getItemStack(EnumComponentType.DIAMOND_CRYSTAL),
		                                       'e', ItemComponent.getItemStack(EnumComponentType.ENDER_COIL),
		                                       'M', ItemComponent.getItemStack(EnumComponentType.MEMORY_CRYSTAL),
		                                       'm', itemStackMachineCasings[0] ));
	}
	
	private static void initComponents() {
		// *** memory storage
		// Memory crystal is 2 Papers, 2 Iron bars, 4 Comparators, 1 Redstone
		final Object memory = WarpDriveConfig.getOreOrItemStack(
				"ore:circuitPrimitive", 0,       // comes with GregTech
				"ore:oc:ram2", 0,
				"opencomputers:components", 8,   // Memory Tier 1.5 (workaround for ore dictionary oc:ram2)
				"minecraft:comparator", 0 );
		WarpDrive.register(new ShapedOreRecipe(groupComponents,
		                                       ItemComponent.getItemStack(EnumComponentType.MEMORY_CRYSTAL), false, "gmg", "gmg", "prp",
		                                       'g', "paneGlassColorless",
		                                       'm', memory,
		                                       'r', Items.REDSTONE,
		                                       'p', Items.PAPER ));
		WarpDrive.register(new ShapelessOreRecipe(groupComponents,
		                                          ItemComponent.getItemStack(EnumComponentType.MEMORY_CLUSTER),
		                                          ItemComponent.getItemStack(EnumComponentType.MEMORY_CRYSTAL),
		                                          ItemComponent.getItemStack(EnumComponentType.MEMORY_CRYSTAL),
		                                          ItemComponent.getItemStack(EnumComponentType.MEMORY_CRYSTAL),
		                                          ItemComponent.getItemStack(EnumComponentType.MEMORY_CRYSTAL) ));
		
		// *** processing
		// Diamond crystal
		WarpDrive.register(new ShapedOreRecipe(groupComponents,
		                                       ItemComponent.getItemStack(EnumComponentType.DIAMOND_CRYSTAL), false, " d ", "BBB", "prp",
		                                       'd', Items.DIAMOND,
		                                       'B', barsIron,
		                                       'r', Items.REDSTONE,
		                                       'p', Items.PAPER ));
		
		// Emerald crystal
		WarpDrive.register(new ShapedOreRecipe(groupComponents,
		                                       ItemComponent.getItemStack(EnumComponentType.EMERALD_CRYSTAL), false, " e ", "BBB", "qrq",
		                                       'e', Items.EMERALD,
		                                       'B', barsIron,
		                                       'r', Items.REDSTONE,
		                                       'q', Items.QUARTZ ));
		
		// *** energy storage
		// Capacitive crystal is 2 Redstone block, 4 Paper, 1 Regeneration potion, 2 (lithium dust or electrum dust or electrical steel ingot or gold ingot)
		final Object lithiumOrElectrum = WarpDriveConfig.getOreOrItemStack(
				"ore:dustLithium", 0,            // comes with GregTech, Industrial Craft 2 and Mekanism
				"ore:dustElectrum", 0,           // comes with ImmersiveEngineering, ThermalFoundation, Metallurgy
				"ore:ingotElectricalSteel", 0,   // comes with EnderIO
				"ore:ingotGold", 0 );
		// (Lithium is processed from nether quartz)
		// (IC2 Experimental is 1 Lithium dust from 18 nether quartz)
		// Regeneration II (ghast tear + glowstone)
		final ItemStack itemStackStrongRegeneration = WarpDriveConfig.getItemStackOrFire("minecraft:potion", 0, "{Potion: \"minecraft:strong_regeneration\"}");
		WarpDrive.register(new RecipeParticleShapedOre(groupComponents,
		                                               ItemComponent.getItemStackNoCache(EnumComponentType.CAPACITIVE_CRYSTAL, 2), false, "prp", "lRl", "prp",
		                                               'R', itemStackStrongRegeneration,
		                                               'r', "blockRedstone",
		                                               'l', lithiumOrElectrum,
		                                               'p', Items.PAPER ));
		WarpDrive.register(new ShapelessOreRecipe(groupComponents,
		                                          ItemComponent.getItemStack(EnumComponentType.CAPACITIVE_CLUSTER),
		                                          ItemComponent.getItemStack(EnumComponentType.CAPACITIVE_CRYSTAL),
		                                          ItemComponent.getItemStack(EnumComponentType.CAPACITIVE_CRYSTAL),
		                                          ItemComponent.getItemStack(EnumComponentType.CAPACITIVE_CRYSTAL),
		                                          ItemComponent.getItemStack(EnumComponentType.CAPACITIVE_CRYSTAL) ));
		
		// *** networking
		// Ender coil crystal
		final Object nuggetGoldOrSilver = WarpDriveConfig.getOreOrItemStack(
				"ore:nuggetElectrum", 0,
				"ore:nuggetSilver", 0,
				"ore:nuggetGold", 0 );
		WarpDrive.register(new ShapedOreRecipe(groupComponents,
		                                       ItemComponent.getItemStackNoCache(EnumComponentType.ENDER_COIL, 2), false, "GGg", "rer", "gGG",
		                                       'e', Items.ENDER_PEARL,
		                                       'G', "paneGlassColorless",
		                                       'r', Items.REDSTONE,
		                                       'g', nuggetGoldOrSilver ));
		
		// Diamond coil is 6 Iron bars, 2 Gold ingots, 1 Diamond crystal, gives 12
		final Object ingotGoldOrSilver = WarpDriveConfig.getOreOrItemStack(
				"ore:ingotElectrum", 0,
				"ore:ingotSilver", 0,
				"ore:ingotGold", 0 );
		WarpDrive.register(new ShapedOreRecipe(groupComponents,
		                                       ItemComponent.getItemStackNoCache(EnumComponentType.DIAMOND_COIL, 12), false, "bbg", "bdb", "gbb",
		                                       'b', barsIron,
		                                       'g', ingotGoldOrSilver,
		                                       'd', ItemComponent.getItemStack(EnumComponentType.DIAMOND_CRYSTAL) ));
		
		// Computer interface is 2 Gold ingot, 2 Wired modems (or redstone), 1 Lead/Tin ingot
		Object redstoneOrModem = Items.REDSTONE;
		if (WarpDriveConfig.isComputerCraftLoaded) {
			redstoneOrModem = WarpDriveConfig.getItemStackOrFire("computercraft:cable", 1); // Wired modem
		}
		
		final Object controlUnitOrBasicCircuit = WarpDriveConfig.getOreOrItemStack(
				"ore:oc:materialCU", 0,
				"opencomputers:material", 11, // Control unit is 5 gold ingot, 2 redstone, 1 paper, 3 iron ingot
				"ore:circuitBasic", 0,
				"minecraft:light_weighted_pressure_plate", 0 );
		
		// Computer interface: double output with Soldering alloy
		if (OreDictionary.doesOreNameExist("ingotSolderingAlloy") && !OreDictionary.getOres("ingotSolderingAlloy").isEmpty()) {
			WarpDrive.register(new ShapedOreRecipe(groupComponents,
			                                       ItemComponent.getItemStackNoCache(EnumComponentType.COMPUTER_INTERFACE, 4), false, "   ", "rar", "gGg",
			                                       'G', controlUnitOrBasicCircuit,
			                                       'g', "ingotGold",
			                                       'r', redstoneOrModem,
			                                       'a', "ingotSolderingAlloy" ));
		}
		
		// Computer interface: simple output
		final Object slimeOrTinOrLead = WarpDriveConfig.getOreOrItemStack(
				"ore:ingotTin", 0,
				"ore:ingotLead", 0,
				"ore:slimeball", 0 );
		WarpDrive.register(new ShapedOreRecipe(groupComponents,
		                                       ItemComponent.getItemStackNoCache(EnumComponentType.COMPUTER_INTERFACE, 2), false, "   ", "rar", "gGg",
		                                       'G', controlUnitOrBasicCircuit,
		                                       'g', "ingotGold",
		                                       'r', redstoneOrModem,
		                                       'a', slimeOrTinOrLead ));
		
		// *** breathing
		// Bone charcoal is smelting 1 Bone
		GameRegistry.addSmelting(Items.BONE, ItemComponent.getItemStackNoCache(EnumComponentType.BONE_CHARCOAL, 1), 1);
		
		// Activated carbon is 3 bone charcoal, 3 leaves, 2 water bottles, 1 sulfur dust or gunpowder
		final Object leaves = WarpDriveConfig.getOreOrItemStack(
				"ore:treeLeaves", 0,
				"minecraft:leaves", 0 );
		final Object gunpowderOrSulfur = WarpDriveConfig.getOreOrItemStack(
				"ore:dustSulfur", 0,
				"ore:gunpowder", 0,
				"minecraft:gunpowder", 0 );
		final ItemStack itemStackWaterBottle = WarpDriveConfig.getItemStackOrFire("minecraft:potion", 0, "{Potion: \"minecraft:water\"}");
		WarpDrive.register(new RecipeParticleShapedOre(groupComponents,
		                                               ItemComponent.getItemStack(EnumComponentType.ACTIVATED_CARBON), false, "lll", "aaa", "wgw",
		                                               'l', leaves,
		                                               'a', ItemComponent.getItemStack(EnumComponentType.BONE_CHARCOAL),
		                                               'w', itemStackWaterBottle,
		                                               'g', gunpowderOrSulfur ));
		
		// Air canister is 4 iron bars, 2 rubber, 2 yellow wool, 1 tank
		final Object woolPurple = WarpDriveConfig.getOreOrItemStack(
				"ore:blockWoolPurple", 0,
				"minecraft:wool", 10 );
		WarpDrive.register(new ShapedOreRecipe(groupComponents,
		                                       ItemComponent.getItemStackNoCache(EnumComponentType.AIR_CANISTER, 4), false, "iyi", "rgr", "iyi",
		                                       'r', rubber,
		                                       'g', ItemComponent.getItemStack(EnumComponentType.GLASS_TANK),
		                                       'y', woolPurple,
		                                       'i', barsIron ));
		
		// *** human interface
		// Flat screen is 3 Dyes, 1 Glowstone dust, 2 Paper, 3 Glass panes
		WarpDrive.register(new ShapedOreRecipe(groupComponents,
		                                       ItemComponent.getItemStack(EnumComponentType.FLAT_SCREEN), false, "gRp", "gGd", "gBp",
		                                       'R', "dyeRed",
		                                       'G', "dyeLime",
		                                       'B', "dyeBlue",
		                                       'd', "dustGlowstone",
		                                       'g', "paneGlassColorless",
		                                       'p', Items.PAPER ));
		
		// Holographic projector is 5 Flat screens, 1 Zoom, 1 Emerald crystal, 1 Memory crystal
		WarpDrive.register(new ShapedOreRecipe(groupComponents,
		                                       ItemComponent.getItemStack(EnumComponentType.HOLOGRAPHIC_PROJECTOR), false, "ssM", "szc", "ssE",
		                                       's', ItemComponent.getItemStack(EnumComponentType.FLAT_SCREEN),
		                                       'z', ItemComponent.getItemStack(EnumComponentType.ZOOM),
		                                       'M', ItemComponent.getItemStack(EnumComponentType.MEMORY_CRYSTAL),
		                                       'c', ItemComponent.getItemStack(EnumComponentType.COMPUTER_INTERFACE),
		                                       'E', ItemComponent.getItemStack(EnumComponentType.EMERALD_CRYSTAL) ));
		
		// *** mechanical
		// Glass tank is 4 Slime balls, 4 Glass
		// slimeball && blockGlass are defined by forge itself
		WarpDrive.register(new ShapedOreRecipe(groupComponents,
		                                       ItemComponent.getItemStackNoCache(EnumComponentType.GLASS_TANK, 4), false, "sgs", "g g", "sgs",
		                                       's', "slimeball",
		                                       'g', "blockGlass" ));
		
		// Motor is 2 Gold nuggets (wires), 3 Iron ingots (steel rods), 4 Iron bars (coils)
		WarpDrive.register(new ShapedOreRecipe(groupComponents,
		                                       ItemComponent.getItemStack(EnumComponentType.MOTOR), false, "bbn", "iii", "bbn",
		                                       'b', barsIron,
		                                       'i', "ingotIron",
		                                       'n', "nuggetGold" ));
		
		// Pump is 2 Motor, 1 Iron ingot, 2 Tank, 4 Rubber, gives 2
		WarpDrive.register(new ShapedOreRecipe(groupComponents,
		                                       ItemComponent.getItemStackNoCache(EnumComponentType.PUMP, 2), false, "sst", "mim", "tss",
		                                       's', rubber,
		                                       'i', ingotIronOrSteel,
		                                       'm', itemStackMotors[0],
		                                       't', ItemComponent.getItemStack(EnumComponentType.GLASS_TANK) ));
		
		// *** optical
		// Lens is 1 Diamond, 6 Gold nugget, 2 Glass panel, gives 2
		final Object diamondLensOrGem = WarpDriveConfig.getOreOrItemStack(
				"ore:lensDiamond", 0,
				"ore:demDiamond", 0 );
		final Object whiteLensOrGlassPane = WarpDriveConfig.getOreOrItemStack(
				"ore:craftingLensWhite", 0,
				"ore:paneGlassColorless", 0 );
		final Object glassPane = WarpDriveConfig.getOreOrItemStack(
				"ore:plateGlass", 0,
				"ore:paneGlassColorless", 0 );
		WarpDrive.register(new ShapedOreRecipe(groupComponents,
		                                       ItemComponent.getItemStackNoCache(EnumComponentType.LENS, 2), false, "ggg", "pdp", "ggg",
		                                       'g', "nuggetGold",
		                                       'p', whiteLensOrGlassPane,
		                                       'd', diamondLensOrGem ));
		// in case craftingLens was found, add a secondary more expensive recipe with just glass
		// note: this is done in the spirit of Gregtech progression while keeping early access to laser tree farm
		if (!glassPane.equals(whiteLensOrGlassPane)) {
			WarpDrive.register(new ShapedOreRecipe(groupComponents,
			                                       ItemComponent.getItemStackNoCache(EnumComponentType.LENS, 1), false, "ggg", "pdp", "ggg",
			                                       'g', "nuggetGold",
			                                       'p', glassPane,
			                                       'd', diamondLensOrGem ));
		}
		
		// Zoom is 3 Lens, 2 Iron ingot, 2 Dyes, 1 Redstone, 1 Basic motor
		WarpDrive.register(new ShapedOreRecipe(groupComponents,
		                                       ItemComponent.getItemStack(EnumComponentType.ZOOM), false, "dir", "lll", "dit",
		                                       'r', Items.REDSTONE,
		                                       'i', ingotIronOrSteel,
		                                       'l', ItemComponent.getItemStack(EnumComponentType.LENS),
		                                       't', itemStackMotors[0],
		                                       'd', "dye" ));
		
		// Diffraction grating is 1 Ghast tear, 3 Iron bar, 3 Glowstone block
		WarpDrive.register(new ShapedOreRecipe(groupComponents,
		                                       ItemComponent.getItemStack(EnumComponentType.DIFFRACTION_GRATING), false, " t ", "iii", "ggg",
		                                       't', Items.GHAST_TEAR,
		                                       'i', barsIron,
		                                       'g', Blocks.GLOWSTONE ));
		
		// *** energy interface
		// Power interface is 4 Redstone, 2 Rubber, 3 Gold ingot
		WarpDrive.register(new ShapedOreRecipe(groupComponents,
		                                       ItemComponent.getItemStackNoCache(EnumComponentType.POWER_INTERFACE, 3), false, "rgr", "RgR", "rgr",
		                                       'g', "ingotGold",
		                                       'R', rubber,
		                                       'r', Items.REDSTONE ));
		
		// Superconductor is 1 Ender crystal, 2 Power interface, 2 Cryotheum dust/Lapis block/10k Coolant cell
		final Object coolant = WarpDriveConfig.getOreOrItemStack(
				"ore:dustCryotheum", 0,          // comes with ThermalFoundation
				"ic2:heat_storage", 0,           // IC2 Experimental 10k Coolant Cell
				"ic2:itemheatstorage", 0,        // IC2 Classic 10k Coolant Cell
				"ore:blockLapis", 0 );
		WarpDrive.register(new ShapedOreRecipe(groupComponents,
		                                       ItemComponent.getItemStack(EnumComponentType.SUPERCONDUCTOR), false, " c ", "pep", " c ",
		                                       'p', ItemComponent.getItemStack(EnumComponentType.POWER_INTERFACE),
		                                       'e', ItemComponent.getItemStack(EnumComponentType.ENDER_COIL),
		                                       'c', coolant ), "_direct");
		WarpDrive.register(new ShapedOreRecipe(groupComponents,
		                                       ItemComponent.getItemStack(EnumComponentType.SUPERCONDUCTOR), false, " p ", "cec", " p ",
		                                       'p', ItemComponent.getItemStack(EnumComponentType.POWER_INTERFACE),
		                                       'e', ItemComponent.getItemStack(EnumComponentType.ENDER_COIL),
		                                       'c', coolant ), "_rotated");
		
		// *** crafting components
		// Laser medium (empty) is 3 Glass tanks, 1 Power interface, 1 Computer interface, 1 MV Machine casing
		final ItemStack itemStackAwkwardPotion = WarpDriveConfig.getItemStackOrFire("minecraft:potion", 0, "{Potion: \"minecraft:awkward\"}");
		WarpDrive.register(new RecipeParticleShapedOre(groupMachines,
		                                               ItemComponent.getItemStack(EnumComponentType.LASER_MEDIUM_EMPTY), false, "   ", "gBg", "pm ",
		                                               'B', itemStackAwkwardPotion,
		                                               'g', ItemComponent.getItemStack(EnumComponentType.GLASS_TANK),
		                                               'p', ItemComponent.getItemStack(EnumComponentType.POWER_INTERFACE),
		                                               'm', itemStackMachineCasings[2] ));
		
		// Electromagnetic Projector is 5 Coil crystals, 1 Power interface, 1 Computer interface, 2 Motors
		WarpDrive.register(new ShapedOreRecipe(groupMachines,
		                                       ItemComponent.getItemStack(EnumComponentType.ELECTROMAGNETIC_PROJECTOR), false, "CCm", "Cpc", "CCm",
		                                       'C', ItemComponent.getItemStack(EnumComponentType.DIAMOND_COIL),
		                                       'p', ItemComponent.getItemStack(EnumComponentType.POWER_INTERFACE),
		                                       'm', itemStackMotors[2],
		                                       'c', ItemComponent.getItemStack(EnumComponentType.COMPUTER_INTERFACE)));
		
		// Intermediary component for Reactor core
		if (!WarpDriveConfig.ACCELERATOR_ENABLE) {
			WarpDrive.register(new ShapedOreRecipe(groupMachines,
			                                       ItemComponent.getItemStack(EnumComponentType.REACTOR_CORE), false, "shs", "hmh", "shs",
			                                       's', Items.NETHER_STAR,
			                                       'h', "blockHull3_plain",
			                                       'm', itemStackMachineCasings[2]));
		} else {
			WarpDrive.register(new RecipeParticleShapedOre(groupMachines,
			                                               ItemComponent.getItemStack(EnumComponentType.REACTOR_CORE), false, "chc", "hph", "cec",
			                                               'p', ItemElectromagneticCell.getItemStackNoCache(EnumTier.ADVANCED, ParticleRegistry.ION, 1000),
			                                               'h', "blockHull3_plain",
			                                               'c', ItemComponent.getItemStack(EnumComponentType.CAPACITIVE_CRYSTAL),
			                                               'e', ItemComponent.getItemStack(EnumComponentType.EMERALD_CRYSTAL)));
		}
		
		// *** rubber material
		// Raw rubber lump is produced from Jungle wood in the laser tree farm
		// (no direct recipe)
		
		// Rubber is the product of smelting (vulcanize) Raw rubber lump
		// (in reality, vulcanization requires additives. This refining is optional, so low tiers could still use the Raw rubber lump)
		GameRegistry.addSmelting(
				ItemComponent.getItemStack(EnumComponentType.RAW_RUBBER),
				ItemComponent.getItemStack(EnumComponentType.RUBBER),
				0 );
		
		// *** composite materials
		// Biopulp is some mycelium and lots of leaves
		// silktouch recipe
		WarpDrive.register(new ShapedOreRecipe(groupMachines,
		                                       ItemComponent.getItemStackNoCache(EnumComponentType.BIOPULP, 9), false, "lll", "lml", "lll",
		                                       'l', leaves,
		                                       'm', "blockMushroom" ), "_block");
		
		// easier but more expensive from fiber (sugar cane)
		final Object oreOrBrownMushroom = WarpDriveConfig.getOreOrItemStack(
				"ore:listAllmushroom", 0,
				"minecraft:brown_mushroom", 0 );
		final Object oreOrRedMushroom = WarpDriveConfig.getOreOrItemStack(
				"ore:listAllmushroom", 0,
				"minecraft:red_mushroom", 0 );
		WarpDrive.register(new ShapedOreRecipe(groupMachines,
		                                       ItemComponent.getItemStackNoCache(EnumComponentType.BIOPULP, 2), false, "lll", "mbM", "lll",
		                                       'b', Items.REEDS,
		                                       'l', leaves,
		                                       'm', oreOrBrownMushroom,
		                                       'M', oreOrRedMushroom ), "_sugarcane");
		
		// Biofiber is the product of washing/filtering/drying Biopulp
		GameRegistry.addSmelting(
				ItemComponent.getItemStack(EnumComponentType.BIOPULP),
				ItemComponent.getItemStack(EnumComponentType.BIOFIBER),
				0 );
		
		// Raw ceramic is clay, with silicate
		WarpDrive.register(new ShapelessOreRecipe(groupMachines,
		                                          ItemComponent.getItemStackNoCache(EnumComponentType.RAW_CERAMIC, 4),
		                                          Items.CLAY_BALL,
		                                          Items.CLAY_BALL,
		                                          Items.CLAY_BALL,
		                                          "sand" ));
		
		// Biofiber is the product of washing/filtering/drying Biopulp
		GameRegistry.addSmelting(
				ItemComponent.getItemStack(EnumComponentType.RAW_CERAMIC),
				ItemComponent.getItemStack(EnumComponentType.CERAMIC),
				0 );
		
		// Carbon fiber plate is a slow/expensive process from making fiber, then making mesh than cooking it
		// Raw carbon fiber from 8 coal (dust), 1 blaze powder, gives 4
		// for reference:
		// - IC2 is from 4 coal dust, gives 1 fiber
		// - TechGuns is 1 blaze powder, 1 diamond, 1B lava, gives 2 fiber/plate
		final Object coalDustOrCoal = WarpDriveConfig.getOreOrItemStack(
				"ore:dustCoal", 0,
				"minecraft:coal", 0 );
		WarpDrive.register(new ShapelessOreRecipe(groupMachines,
		                                          ItemComponent.getItemStackNoCache(EnumComponentType.RAW_CARBON_FIBER, 4),
		                                          Items.BLAZE_POWDER,
		                                          coalDustOrCoal, coalDustOrCoal, coalDustOrCoal, coalDustOrCoal,
		                                          coalDustOrCoal, coalDustOrCoal, coalDustOrCoal, coalDustOrCoal ), "coal");
		// (alternate recipe, more expensive from charcoal)
		final Object coalDustOrCharcoal = WarpDriveConfig.getOreOrItemStack(
				"ore:dustCharcoal", 0,
				"minecraft:coal", 1 );
		WarpDrive.register(new ShapelessOreRecipe(groupMachines,
		                                          ItemComponent.getItemStack(EnumComponentType.RAW_CARBON_FIBER),
		                                          Items.BLAZE_POWDER,
		                                          coalDustOrCharcoal, coalDustOrCharcoal, coalDustOrCharcoal, coalDustOrCharcoal ), "charcoal");
		
		// Raw carbon mesh is 2 Biofiber, 3 Carbon fiber
		// for reference:
		// - IC2 is 2 fiber, gives 1 mesh
		WarpDrive.register(new ShapedOreRecipe(groupMachines,
		                                       ItemComponent.getItemStackNoCache(EnumComponentType.RAW_CARBON_MESH, 4), false, "fcf", "ccc", "fcf",
		                                       'f', ItemComponent.getItemStack(EnumComponentType.BIOFIBER),
		                                       'c', ItemComponent.getItemStack(EnumComponentType.RAW_CARBON_FIBER) ));
		
		// Carbon fiber is the product of cooking the mesh (under pressure?)
		GameRegistry.addSmelting(
				ItemComponent.getItemStack(EnumComponentType.RAW_CARBON_MESH),
				ItemComponent.getItemStack(EnumComponentType.CARBON_FIBER),
				0 );
	}
	
	private static void initToolsAndArmor() {
		// Warp helmet
		WarpDrive.register(new ShapedOreRecipe(groupTools,
		                                       WarpDrive.itemWarpArmor[EnumTier.BASIC.getIndex()][EntityEquipmentSlot.HEAD.getIndex()], false, "mmm", "mgm", "ici",
		                                       'm', "itemRubber",
		                                       'g', "blockHull1_glass",
		                                       'c', ItemComponent.getItemStack(EnumComponentType.AIR_CANISTER),
		                                       'i', "nuggetIron" ));
		WarpDrive.register(new ShapedOreRecipe(groupTools,
		                                       WarpDrive.itemWarpArmor[EnumTier.ADVANCED.getIndex()][EntityEquipmentSlot.HEAD.getIndex()], false, "fmf", "mam", "   ",
		                                       'a', WarpDrive.itemWarpArmor[EnumTier.BASIC.getIndex()][EntityEquipmentSlot.HEAD.getIndex()],
		                                       'm', "itemCeramic",
		                                       'f', "itemBiofiber" ));
		WarpDrive.register(new ShapedOreRecipe(groupTools,
		                                       WarpDrive.itemWarpArmor[EnumTier.SUPERIOR.getIndex()][EntityEquipmentSlot.HEAD.getIndex()], false, "mmm", "mam", "   ",
		                                       'a', WarpDrive.itemWarpArmor[EnumTier.ADVANCED.getIndex()][EntityEquipmentSlot.HEAD.getIndex()],
		                                       'm', "plateCarbon" ));
		
		// Warp chestplate
		WarpDrive.register(new ShapedOreRecipe(groupTools,
		                                       WarpDrive.itemWarpArmor[EnumTier.BASIC.getIndex()][EntityEquipmentSlot.CHEST.getIndex()], false, "i i", "mmm", "mim",
		                                       'm', "itemRubber",
		                                       'i', "nuggetIron" ));
		WarpDrive.register(new ShapedOreRecipe(groupTools,
		                                       WarpDrive.itemWarpArmor[EnumTier.ADVANCED.getIndex()][EntityEquipmentSlot.CHEST.getIndex()], false, "faf", "mmm", "mfm",
		                                       'a', WarpDrive.itemWarpArmor[EnumTier.BASIC.getIndex()][EntityEquipmentSlot.CHEST.getIndex()],
		                                       'm', "itemCeramic",
		                                       'f', "itemBiofiber" ));
		WarpDrive.register(new ShapedOreRecipe(groupTools,
		                                       WarpDrive.itemWarpArmor[EnumTier.SUPERIOR.getIndex()][EntityEquipmentSlot.CHEST.getIndex()], false, "mam", "mpm", "mcm",
		                                       'a', WarpDrive.itemWarpArmor[EnumTier.ADVANCED.getIndex()][EntityEquipmentSlot.CHEST.getIndex()],
		                                       'm', "plateCarbon",
		                                       'p', ItemComponent.getItemStack(EnumComponentType.PUMP),
		                                       'c', ItemComponent.getItemStack(EnumComponentType.AIR_CANISTER) ));
		
		// Warp Leggings
		WarpDrive.register(new ShapedOreRecipe(groupTools,
		                                       WarpDrive.itemWarpArmor[EnumTier.BASIC.getIndex()][EntityEquipmentSlot.LEGS.getIndex()], false, "imi", "m m", "m m",
		                                       'm', "itemRubber",
		                                       'i', "nuggetIron" ));
		WarpDrive.register(new ShapedOreRecipe(groupTools,
		                                       WarpDrive.itemWarpArmor[EnumTier.ADVANCED.getIndex()][EntityEquipmentSlot.LEGS.getIndex()], false, "faf", "mMm", "w w",
		                                       'a', WarpDrive.itemWarpArmor[EnumTier.BASIC.getIndex()][EntityEquipmentSlot.LEGS.getIndex()],
		                                       'm', "itemCeramic",
		                                       'f', "itemBiofiber",
		                                       'w', Blocks.WOOL,
		                                       'M', itemStackMotors[1] ));
		WarpDrive.register(new ShapedOreRecipe(groupTools,
		                                       WarpDrive.itemWarpArmor[EnumTier.SUPERIOR.getIndex()][EntityEquipmentSlot.LEGS.getIndex()], false, "mam", "m m", "m m",
		                                       'a', WarpDrive.itemWarpArmor[EnumTier.ADVANCED.getIndex()][EntityEquipmentSlot.LEGS.getIndex()],
		                                       'm', "plateCarbon" ));
		
		// Warp boots
		WarpDrive.register(new ShapedOreRecipe(groupTools,
		                                       WarpDrive.itemWarpArmor[EnumTier.BASIC.getIndex()][EntityEquipmentSlot.FEET.getIndex()], false, "i i", "m m", "   ",
		                                       'm', "itemRubber",
		                                       'i', "nuggetIron" ));
		WarpDrive.register(new ShapedOreRecipe(groupTools,
		                                       WarpDrive.itemWarpArmor[EnumTier.ADVANCED.getIndex()][EntityEquipmentSlot.FEET.getIndex()], false, "mam", "fMf", "w w",
		                                       'a', WarpDrive.itemWarpArmor[EnumTier.BASIC.getIndex()][EntityEquipmentSlot.FEET.getIndex()],
		                                       'm', "itemCeramic",
		                                       'f', "itemBiofiber",
		                                       'w', Blocks.WOOL,
		                                       'M', itemStackMotors[1] ));
		WarpDrive.register(new ShapedOreRecipe(groupTools,
		                                       WarpDrive.itemWarpArmor[EnumTier.SUPERIOR.getIndex()][EntityEquipmentSlot.FEET.getIndex()], false, "mam", "m m", "   ",
		                                       'a', WarpDrive.itemWarpArmor[EnumTier.ADVANCED.getIndex()][EntityEquipmentSlot.FEET.getIndex()],
		                                       'm', "plateCarbon" ));
		
		// Wrench
		WarpDrive.register(new ShapedOreRecipe(groupTools,
		                                       WarpDrive.itemWrench, false, "n n", "nin", " m ",
		                                       'm', "itemRubber",
		                                       'i', "ingotIron",
		                                       'n', "nuggetIron" ));
		
		// Tuning fork variations
		for (final EnumDyeColor enumDyeColor : EnumDyeColor.values()) {
			final int damageColor = enumDyeColor.getDyeDamage();
			
			// crafting tuning fork
			WarpDrive.register(new ShapedOreRecipe(groupTools,
			                                       new ItemStack(WarpDrive.itemTuningFork, 1, damageColor), false, "  q", "iX ", " i ",
			                                       'q', "gemQuartz",
			                                       'i', "ingotIron",
			                                       'X', oreDyes.get(enumDyeColor) ));
			
			// changing colors
			WarpDrive.register(new ShapelessOreRecipe(groupTools,
			                                          new ItemStack(WarpDrive.itemTuningFork, 1, damageColor),
			                                          oreDyes.get(enumDyeColor),
			                                          "itemTuningFork"), "_dye");
		}
		
		// Tuning driver crafting
		WarpDrive.register(new ShapedOreRecipe(groupTools,
		                                       new ItemStack(WarpDrive.itemTuningDriver, 1, ItemTuningDriver.MODE_VIDEO_CHANNEL), false, "  q", "pm ", "d  ",
		                                       'q', "gemQuartz",
		                                       'p', Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE,
		                                       'd', ItemComponent.getItemStack(EnumComponentType.DIAMOND_CRYSTAL),
		                                       'm', ItemComponent.getItemStack(EnumComponentType.MEMORY_CRYSTAL) ));
		
		// Tuning driver configuration
		WarpDrive.register(new RecipeTuningDriver(groupTools,
		                                          new ItemStack(WarpDrive.itemTuningDriver, 1, ItemTuningDriver.MODE_VIDEO_CHANNEL),
		                                          new ItemStack(Items.REDSTONE), 7, "_video2"), "_video1");
		WarpDrive.register(new RecipeTuningDriver(groupTools,
		                                          new ItemStack(WarpDrive.itemTuningDriver, 1, ItemTuningDriver.MODE_BEAM_FREQUENCY),
		                                          new ItemStack(Items.REDSTONE), 4, "_bream_frequency2"), "_bream_frequency1");
		WarpDrive.register(new RecipeTuningDriver(groupTools,
		                                          new ItemStack(WarpDrive.itemTuningDriver, 1, ItemTuningDriver.MODE_CONTROL_CHANNEL),
		                                          new ItemStack(Items.REDSTONE), 7, "_control_channel2"), "_control_channel1");
		
		// User manual
		final ItemStack itemStackManual = WarpDriveConfig.getItemStackOrFire("patchouli:guide_book", 0, "{\"patchouli:book\": \"warpdrive:warpdrive_manual\"}");
		if (!itemStackManual.isEmpty()) {
			WarpDrive.register(new ShapedOreRecipe(groupTools,
			                                       itemStackManual, false, " g ", "ibi", " i ",
			                                       'g', "nuggetGold",
			                                       'i', "nuggetIron",
			                                       'b', Items.BOOK ));
		}
	}
	
	public static void initDynamic() {
		initIngredients();
		initComponents();
		initToolsAndArmor();
		
		// Ship scanner is creative only => no recipe
		/*
		WarpDrive.register(new ShapedOreRecipe(groupMachines,
		                                       new ItemStack(WarpDrive.blockShipScanner), false, "ici", "isi", "mcm",
		                                       'm', mfsu,
		                                       'i', iridiumAlloy,
		                                       'c', goldIngotOrAdvancedCircuit,
		                                       's', WarpDriveConfig.getModItemStack("ic2", "te", 64) )); // Scanner
		/**/
		
		if (WarpDriveConfig.ACCELERATOR_ENABLE) {
			initAtomic();
		}
		initBreathing();
		initCollection();
		initDecoration();
		initDetection();
		initEnergy();
		initForceField();
		initHull();
		initMovement();
		initWeapon();
	}
	
	private static void initAtomic() {
		// Void shells is Hull, Power interface, Steel or Iron
		WarpDrive.register(new ShapedOreRecipe(groupMachines,
		                                       new ItemStack(WarpDrive.blockVoidShellPlain, 6), "psh", "s s", "hsp",
		                                       'h', "blockHull1_plain",
		                                       'p', ItemComponent.getItemStack(EnumComponentType.POWER_INTERFACE),
		                                       's', ingotIronOrSteel));
		WarpDrive.register(new ShapedOreRecipe(groupMachines,
		                                       new ItemStack(WarpDrive.blockVoidShellGlass, 6), "psh", "s s", "hsp",
		                                       'h', "blockHull1_glass",
		                                       'p', ItemComponent.getItemStack(EnumComponentType.POWER_INTERFACE),
		                                       's', ingotIronOrSteel));
		
		WarpDrive.register(new ShapedOreRecipe(groupMachines,
		                                       new ItemStack(WarpDrive.blockVoidShellGlass, 2), "g g", "sfs", "g g",
		                                       'g', "blockGlass",
		                                       'f', "dustGlowstone",
		                                       's', WarpDrive.blockVoidShellPlain));
		
		// Electromagnetic cell
		WarpDrive.register(new ShapedOreRecipe(groupMachines,
		                                       new ItemStack(WarpDrive.itemElectromagneticCell[EnumTier.BASIC.getIndex()], 2), "iri", "i i", "ici",
		                                       'i', barsIron,
		                                       'c', ItemComponent.getItemStack(EnumComponentType.CAPACITIVE_CRYSTAL),
		                                       'r', Items.REDSTONE));
		WarpDrive.register(new ShapedOreRecipe(groupMachines,
		                                       new ItemStack(WarpDrive.itemElectromagneticCell[EnumTier.ADVANCED.getIndex()], 2), "iei", "iei", "gcg",
		                                       'e', WarpDrive.itemElectromagneticCell[EnumTier.BASIC.getIndex()],
		                                       'i', barsIron,
		                                       'g', Items.GOLD_NUGGET,
		                                       'c', ItemComponent.getItemStack(EnumComponentType.CAPACITIVE_CRYSTAL)));
		WarpDrive.register(new ShapedOreRecipe(groupMachines,
		                                       new ItemStack(WarpDrive.itemElectromagneticCell[EnumTier.SUPERIOR.getIndex()], 2), "geg", "geg", "gcg",
		                                       'e', WarpDrive.itemElectromagneticCell[EnumTier.ADVANCED.getIndex()],
		                                       'g', Items.GOLD_NUGGET,
		                                       'c', ItemComponent.getItemStack(EnumComponentType.CAPACITIVE_CRYSTAL)));
		
		// Plasma torch
		/*
		WarpDrive.register(new ShapedOreRecipe(groupTools,
		                                       WarpDrive.itemPlasmaTorch[EnumTier.BASIC.getIndex()], false, "tcr", "mgb", "i  ",
		                                       't', WarpDrive.itemElectromagneticCell[EnumTier.BASIC.getIndex()],
		                                       'c', ItemComponent.getItemStack(EnumComponentType.ACTIVATED_CARBON),
		                                       'r', Items.BLAZE_ROD,
		                                       'm', ItemComponent.getItemStack(EnumComponentType.PUMP),
		                                       'g', "ingotGold",
		                                       'b', Blocks.STONE_BUTTON,
		                                       'i', ingotIronOrSteel));
		/**/
		
		// Accelerator control point
		WarpDrive.register(new ShapedOreRecipe(groupMachines,
		                                       new ItemStack(WarpDrive.blockAcceleratorControlPoint), "hd ", "vc ", "he ",
		                                       'h', Blocks.HOPPER,
		                                       'd', ItemComponent.getItemStack(EnumComponentType.DIAMOND_CRYSTAL),
		                                       'e', ItemComponent.getItemStack(EnumComponentType.EMERALD_CRYSTAL),
		                                       'c', ItemComponent.getItemStack(EnumComponentType.COMPUTER_INTERFACE),
		                                       'v', "blockVoidShell"));
		
		// Particles injector
		WarpDrive.register(new ShapedOreRecipe(groupMachines,
		                                       new ItemStack(WarpDrive.blockParticlesInjector), "mm ", "vvp", "mmc",
		                                       'p', Blocks.PISTON,
		                                       'm', "blockElectromagnet1",
		                                       'c', WarpDrive.blockAcceleratorControlPoint,
		                                       'v', "blockVoidShell"));
		
		// Accelerator controller
		WarpDrive.register(new ShapedOreRecipe(groupMachines,
		                                       new ItemStack(WarpDrive.blockAcceleratorCore), "MmM", "mcm", "MmM",
		                                       'M', ItemComponent.getItemStack(EnumComponentType.MEMORY_CRYSTAL),
		                                       'm', "blockElectromagnet1",
		                                       'c', WarpDrive.blockAcceleratorControlPoint));
		
		// Particles collider
		WarpDrive.register(new ShapedOreRecipe(groupMachines,
		                                       new ItemStack(WarpDrive.blockParticlesCollider), "hoh", "odo", "hoh",
		                                       'h', "blockHull1_plain",
		                                       'o', Blocks.OBSIDIAN,
		                                       'd', Items.DIAMOND));
		
		// Chillers
		Object snowOrIce = Blocks.SNOW;
		if (OreDictionary.doesOreNameExist("dustCryotheum") && !OreDictionary.getOres("dustCryotheum").isEmpty()) {
			snowOrIce = Blocks.ICE;
		}
		WarpDrive.register(new ShapedOreRecipe(groupMachines,
		                                       new ItemStack(WarpDrive.blockChillers[EnumTier.BASIC.getIndex()]), "wgw", "sms", "bMb",
		                                       'w', snowOrIce,
		                                       'g', Items.GHAST_TEAR,
		                                       's', ingotIronOrSteel,
		                                       'm', itemStackMotors[0],
		                                       'b', barsIron,
		                                       'M', "blockElectromagnet1"));
		
		Object nitrogen = Blocks.ICE;
		if (OreDictionary.doesOreNameExist("dustCryotheum") && !OreDictionary.getOres("dustCryotheum").isEmpty()) {
			nitrogen = Blocks.PACKED_ICE;
		}
		WarpDrive.register(new ShapedOreRecipe(groupMachines,
		                                       new ItemStack(WarpDrive.blockChillers[EnumTier.ADVANCED.getIndex()]), "ngn", "dmd", "bMb",
		                                       'n', nitrogen,
		                                       'g', Items.GHAST_TEAR,
		                                       'd', Items.DIAMOND,
		                                       'm', itemStackMotors[1],
		                                       'b', barsIron,
		                                       'M', "blockElectromagnet2"));
		
		Object helium = Blocks.PACKED_ICE;
		if (OreDictionary.doesOreNameExist("dustCryotheum") && !OreDictionary.getOres("dustCryotheum").isEmpty()) {
			helium = "dustCryotheum";
		}
		WarpDrive.register(new ShapedOreRecipe(groupMachines,
		                                       new ItemStack(WarpDrive.blockChillers[EnumTier.SUPERIOR.getIndex()]), "hgh", "eme", "bMb",
		                                       'h', helium,
		                                       'g', Items.GHAST_TEAR,
		                                       'e', Items.EMERALD,
		                                       'm', itemStackMotors[2],
		                                       'b', barsIron,
		                                       'M', "blockElectromagnet3"));
		
		// Lower tier coil is iron, copper or coil
		// note: IC2 Classic has no coil, so we fallback to other mods or Copper ingot
		final Object ironIngotOrCopperIngotOrCoil1 = WarpDriveConfig.getOreOrItemStack(
				"ic2:crafting", 5,                         // IC2 Experimental Coil
				"immersiveengineering:wirecoil", 1,        // ImmersiveEngineering MV wire coil
				"enderio:item_power_conduit", 1,           // EnderIO Enhanced energy conduit
				"ore:ingotCopper", 0,
				"ore:ingotSteel", 0,
				"minecraft:iron_ingot", 0 );
		final Object ironIngotOrCopperIngotOrCoil2 = WarpDriveConfig.getOreOrItemStack(
				"gregtech:wire_coil", 0,                   // GregTech Cupronickel Coil block
				"ic2:crafting", 5,                         // IC2 Experimental Coil
				"thermalfoundation:material", 513,         // ThermalFoundation Redstone reception coil
				"immersiveengineering:wirecoil", 1,        // ImmersiveEngineering MV wire coil
				"enderio:item_power_conduit", 1,           // EnderIO Enhanced energy conduit
				"ore:ingotCopper", 0,
				"ore:ingotSteel", 0,
				"minecraft:iron_ingot", 0 );
		
		// Normal electromagnets
		WarpDrive.register(new ShapedOreRecipe(groupMachines,
		                                       new ItemStack(WarpDrive.blockElectromagnets_plain[EnumTier.BASIC.getIndex()], 4), "   ", "cdc", "Cmt",
		                                       'c', ironIngotOrCopperIngotOrCoil1,
		                                       'd', ironIngotOrCopperIngotOrCoil2,
		                                       't', ItemComponent.getItemStack(EnumComponentType.GLASS_TANK),
		                                       'm', itemStackMotors[0],
		                                       'C', ItemComponent.getItemStack(EnumComponentType.CAPACITIVE_CRYSTAL)));
		WarpDrive.register(new ShapedOreRecipe(groupMachines,
		                                       new ItemStack(WarpDrive.blockElectromagnets_glass[EnumTier.BASIC.getIndex()], 4), "mgm", "g g", "mgm",
		                                       'g', Blocks.GLASS,
		                                       'm', WarpDrive.blockElectromagnets_plain[EnumTier.BASIC.getIndex()]));
		
		// Advanced electromagnets
		WarpDrive.register(new RecipeParticleShapedOre(groupMachines,
		                                               new ItemStack(WarpDrive.blockElectromagnets_plain[EnumTier.ADVANCED.getIndex()], 6), "mpm", "pip", "mpm",
		                                               'i', ItemElectromagneticCell.getItemStackNoCache(EnumTier.BASIC, ParticleRegistry.ION, 200),
		                                               'p', ItemComponent.getItemStack(EnumComponentType.POWER_INTERFACE),
		                                               'm', WarpDrive.blockElectromagnets_plain[EnumTier.BASIC.getIndex()]));
		WarpDrive.register(new RecipeParticleShapedOre(groupMachines,
		                                               new ItemStack(WarpDrive.blockElectromagnets_glass[EnumTier.ADVANCED.getIndex()], 6), "mpm", "pip", "mpm",
		                                               'i', ItemElectromagneticCell.getItemStackNoCache(EnumTier.BASIC, ParticleRegistry.ION, 200),
		                                               'p', ItemComponent.getItemStack(EnumComponentType.POWER_INTERFACE),
		                                               'm', WarpDrive.blockElectromagnets_glass[EnumTier.BASIC.getIndex()]));
		
		// Superior electromagnets
		WarpDrive.register(new RecipeParticleShapedOre(groupMachines,
		                                               new ItemStack(WarpDrive.blockElectromagnets_plain[EnumTier.SUPERIOR.getIndex()], 6), "mtm", "sps", "mMm",
		                                               't', ItemComponent.getItemStack(EnumComponentType.GLASS_TANK),
		                                               's', ItemComponent.getItemStack(EnumComponentType.SUPERCONDUCTOR),
		                                               'p', ItemElectromagneticCell.getItemStackNoCache(EnumTier.BASIC, ParticleRegistry.PROTON, 24),
		                                               'M', itemStackMotors[2],
		                                               'm', WarpDrive.blockElectromagnets_plain[EnumTier.ADVANCED.getIndex()]));
		WarpDrive.register(new RecipeParticleShapedOre(groupMachines,
		                                               new ItemStack(WarpDrive.blockElectromagnets_glass[EnumTier.SUPERIOR.getIndex()], 6), "mtm", "sps", "mMm",
		                                               't', ItemComponent.getItemStack(EnumComponentType.GLASS_TANK),
		                                               's', ItemComponent.getItemStack(EnumComponentType.SUPERCONDUCTOR),
		                                               'p', ItemElectromagneticCell.getItemStackNoCache(EnumTier.BASIC, ParticleRegistry.PROTON, 24),
		                                               'M', itemStackMotors[2],
		                                               'm', WarpDrive.blockElectromagnets_glass[EnumTier.ADVANCED.getIndex()]));
		
		// ICBM classic
		if (WarpDriveConfig.isICBMClassicLoaded) {
			// antimatter
			final ItemStack itemStackAntimatterExplosive = WarpDriveConfig.getItemStackOrFire("icbmclassic:explosives", 22); // Antimatter Explosive
			removeRecipe(itemStackAntimatterExplosive);
			WarpDrive.register(new RecipeParticleShapedOre(groupComponents,
			                                               itemStackAntimatterExplosive, "aaa", "ana", "aaa",
			                                               'a', ItemElectromagneticCell.getItemStackNoCache(EnumTier.ADVANCED, ParticleRegistry.ANTIMATTER, 1000),
			                                               'n', WarpDriveConfig.getItemStackOrFire("icbmclassic:explosives", 15)));
			
			// red matter
			final ItemStack itemStackRedMatterExplosive = WarpDriveConfig.getItemStackOrFire("icbmclassic:explosives", 23); // Red Matter Explosive
			removeRecipe(itemStackRedMatterExplosive);
			WarpDrive.register(new RecipeParticleShapedOre(groupComponents,
			                                               itemStackRedMatterExplosive, "sss", "sas", "sss",
			                                               's', ItemElectromagneticCell.getItemStackNoCache(EnumTier.ADVANCED, ParticleRegistry.STRANGE_MATTER, 1000),
			                                               'a', WarpDriveConfig.getItemStackOrFire("icbmclassic:explosives", 22)));
		}
	}
	
	private static void initBreathing() {
		// Basic Air Tank is 2 Air canisters, 1 Pump, 1 Gold nugget, 1 Basic circuit, 4 Rubber
		WarpDrive.register(new ShapedOreRecipe(groupTools,
		                                       WarpDrive.itemAirTanks[EnumAirTankTier.BASIC.getIndex()], false, "rnr", "tpt", "rcr",
		                                       'r', rubber,
		                                       'p', ItemComponent.getItemStack(EnumComponentType.PUMP),
		                                       't', ItemComponent.getItemStack(EnumComponentType.AIR_CANISTER),
		                                       'c', goldNuggetOrBasicCircuit,
		                                       'n', "nuggetGold" ));
		
		// Advanced Air Tank is 2 Basic air tank, 1 Pump, 1 Gold nugget, 1 Advanced circuit, 4 Rubber
		WarpDrive.register(new ShapedOreRecipe(groupTools,
		                                       WarpDrive.itemAirTanks[EnumAirTankTier.ADVANCED.getIndex()], false, "rnr", "tpt", "rcr",
		                                       'r', rubber,
		                                       'p', itemStackMotors[1],
		                                       't', WarpDrive.itemAirTanks[EnumAirTankTier.BASIC.getIndex()],
		                                       'c', goldIngotOrAdvancedCircuit,
		                                       'n', "nuggetGold" ));
		
		// Superior Air Tank is 2 Advanced air tank, 1 Pump, 1 Gold nugget, 1 Elite circuit, 4 Rubber
		WarpDrive.register(new ShapedOreRecipe(groupTools,
		                                       WarpDrive.itemAirTanks[EnumAirTankTier.SUPERIOR.getIndex()], false, "rnr", "tpt", "rcr",
		                                       'r', rubber,
		                                       'p', itemStackMotors[2],
		                                       't', WarpDrive.itemAirTanks[EnumAirTankTier.ADVANCED.getIndex()],
		                                       'c', emeraldOrSuperiorCircuit,
		                                       'n', "nuggetGold" ));
		
		// Uncrafting air tanks and canister
		WarpDrive.register(new ShapelessOreRecipe(groupComponents,
		                                          ItemComponent.getItemStackNoCache(EnumComponentType.GLASS_TANK, 1),
		                                          WarpDrive.itemAirTanks[EnumAirTankTier.CANISTER.getIndex()],
		                                          WarpDrive.itemAirTanks[EnumAirTankTier.CANISTER.getIndex()],
		                                          WarpDrive.itemAirTanks[EnumAirTankTier.CANISTER.getIndex()],
		                                          WarpDrive.itemAirTanks[EnumAirTankTier.CANISTER.getIndex()] ), "_uncrafting");
		WarpDrive.register(new ShapelessOreRecipe(groupComponents,
		                                          ItemComponent.getItemStackNoCache(EnumComponentType.AIR_CANISTER, 2),
		                                          WarpDrive.itemAirTanks[EnumAirTankTier.BASIC.getIndex()]), "_uncrafting");
		WarpDrive.register(new ShapelessOreRecipe(groupComponents,
		                                          ItemComponent.getItemStackNoCache(EnumComponentType.AIR_CANISTER, 4),
		                                          WarpDrive.itemAirTanks[EnumAirTankTier.ADVANCED.getIndex()]), "_uncrafting");
		WarpDrive.register(new ShapelessOreRecipe(groupComponents,
		                                          ItemComponent.getItemStackNoCache(EnumComponentType.AIR_CANISTER, 8),
		                                          WarpDrive.itemAirTanks[EnumAirTankTier.SUPERIOR.getIndex()]), "_uncrafting");
		
		// Air generator is 1 Power interface, 4 Activated carbon, 1 Motor, 1 MV Machine casing, 2 Glass tanks
		final Object bronzeRotorOrIronBars = WarpDriveConfig.getOreOrItemStack(
				"ore:rotorBronze", 0,            // GregTech CE Bronze rotor
				"ore:plateBronze", 8,            // IC2 or ThermalExpansion Bronze plate
				"ore:gearIronInfinity", 0,       // EnderIO Infinity Bimetal Gear
				"ore:barsIron", 0,               // Ore dictionary iron bars
				"minecraft:iron_bars", 0 );
		WarpDrive.register(new ShapedOreRecipe(groupMachines,
		                                       new ItemStack(WarpDrive.blockAirGeneratorTiered[EnumTier.BASIC.getIndex()]), false, "aba", "ata", "gmp",
		                                       'p', ItemComponent.getItemStack(EnumComponentType.POWER_INTERFACE),
		                                       'a', ItemComponent.getItemStack(EnumComponentType.ACTIVATED_CARBON),
		                                       't', ItemComponent.getItemStack(EnumComponentType.PUMP),
		                                       'g', ItemComponent.getItemStack(EnumComponentType.GLASS_TANK),
		                                       'm', itemStackMachineCasings[1],
		                                       'b', bronzeRotorOrIronBars));
		WarpDrive.register(new ShapedOreRecipe(groupMachines,
		                                       new ItemStack(WarpDrive.blockAirGeneratorTiered[EnumTier.ADVANCED.getIndex()]), false, "aaa", "ata", "ama",
		                                       'a', WarpDrive.blockAirGeneratorTiered[EnumTier.BASIC.getIndex()],
		                                       't', itemStackMotors[2],
		                                       'm', itemStackMachineCasings[2]));
		WarpDrive.register(new ShapedOreRecipe(groupMachines,
		                                       new ItemStack(WarpDrive.blockAirGeneratorTiered[EnumTier.SUPERIOR.getIndex()]), false, "aaa", "ata", "ama",
		                                       'a', WarpDrive.blockAirGeneratorTiered[EnumTier.ADVANCED.getIndex()],
		                                       't', itemStackMotors[3],
		                                       'm', itemStackMachineCasings[3]));
		
		// Air shield is 4 Glowstones, 4 Omnipanels and 1 coil crystal
		for (final EnumDyeColor enumDyeColor : EnumDyeColor.values()) {
			final int metadataColor = enumDyeColor.getMetadata();
			
			WarpDrive.register(new ShapedOreRecipe(groupMachines,
			                                       new ItemStack(WarpDrive.blockAirShield, 4, metadataColor), false, "gog", "oco", "gog",
			                                       'g', "dustGlowstone",
			                                       'o', new ItemStack(WarpDrive.blockHulls_omnipanel[EnumTier.BASIC.getIndex()], 1, metadataColor),
			                                       'c', ItemComponent.getItemStack(EnumComponentType.DIAMOND_COIL) ));
			WarpDrive.register(new ShapedOreRecipe(groupMachines,
			                                       new ItemStack(WarpDrive.blockAirShield, 6, metadataColor), false, "###", "gXg", "###",
			                                       '#', "blockAirShield",
			                                       'g', Items.GOLD_NUGGET,
			                                       'X', oreDyes.get(enumDyeColor) ));
			WarpDrive.register(new ShapelessOreRecipe(groupMachines,
			                                          new ItemStack(WarpDrive.blockAirShield, 1, metadataColor),
			                                          "blockAirShield",
			                                          oreDyes.get(enumDyeColor) ));
		}
	}
	
	private static void initCollection() {
		// Mining laser is 2 Motors, 1 Diffraction grating, 1 Lens, 1 Computer interface, 1 MV Machine casing, 1 Diamond pick, 2 Glass pane
		{
			WarpDrive.register(new ShapedOreRecipe(groupMachines,
			                                       new ItemStack(WarpDrive.blockMiningLaser), false, " mp", "tdt", "glg",
			                                       't', itemStackMotors[1],
			                                       'd', ItemComponent.getItemStack(EnumComponentType.DIFFRACTION_GRATING),
			                                       'l', ItemComponent.getItemStack(EnumComponentType.LENS),
			                                       'm', itemStackMachineCasings[1],
			                                       'p', Items.DIAMOND_PICKAXE,
			                                       'g', "paneGlassColorless"));
		}
		
		// Laser tree farm is 2 Motors, 2 Lenses, 1 Computer interface, 1 LV Machine casing, 1 Iron axe, 2 Glass pane
		// note: diamond axe was more consistent with Buildcraft, nowadays other mods have cheaper options
		WarpDrive.register(new ShapedOreRecipe(groupMachines,
		                                       new ItemStack(WarpDrive.blockLaserTreeFarm), false, "glg", "tlt", "am ",
		                                       't', itemStackMotors[0],
		                                       'l', ItemComponent.getItemStack(EnumComponentType.LENS),
		                                       'm', itemStackMachineCasings[0],
		                                       'a', Items.IRON_AXE,
		                                       'g', "paneGlassColorless"));
	}
	
	private static void initDecoration() {
		// Decorative blocks are metallic in nature
		// base block is very cheap (iron and paper)
		WarpDrive.register(new ShapedOreRecipe(groupDecorations,
		                                       BlockDecorative.getItemStackNoCache(EnumDecorativeType.PLAIN, 12), false, "ipi", "pbp", "ipi",
		                                       'i', Items.IRON_INGOT,
		                                       'b', Blocks.IRON_BARS,
		                                       'p', Items.PAPER ));
		
		// variations are 'died' from each others
		WarpDrive.register(new ShapedOreRecipe(groupDecorations,
		                                       BlockDecorative.getItemStackNoCache(EnumDecorativeType.PLAIN, 8), false, "sss", "scs", "sss",
		                                       's', "warpDecorative",
		                                       'c', "dyeWhite"), "_dye");
		WarpDrive.register(new ShapedOreRecipe(groupDecorations,
		                                       BlockDecorative.getItemStackNoCache(EnumDecorativeType.GRATED, 8), false, "sss", "sbs", "sss",
		                                       's', "warpDecorative",
		                                       'b', barsIron ), "_dye");
		WarpDrive.register(new ShapedOreRecipe(groupDecorations,
		                                       BlockDecorative.getItemStackNoCache(EnumDecorativeType.GLASS, 8), false, "sss", "scs", "sss",
		                                       's', "warpDecorative",
		                                       'c', "glass" ), "_dye");
		WarpDrive.register(new ShapedOreRecipe(groupDecorations,
		                                       BlockDecorative.getItemStackNoCache(EnumDecorativeType.STRIPES_BLACK_DOWN, 7), false, "bss", "sss", "ssy",
		                                       's', "warpDecorative",
		                                       'b', "dyeBlack",
		                                       'y', "dyeYellow" ), "_dye");
		WarpDrive.register(new ShapedOreRecipe(groupDecorations,
		                                       BlockDecorative.getItemStackNoCache(EnumDecorativeType.STRIPES_BLACK_UP, 7), false, "ssy", "sss", "bss",
		                                       's', "warpDecorative",
		                                       'b', "dyeBlack",
		                                       'y', "dyeYellow" ), "_dye");
		WarpDrive.register(new ShapedOreRecipe(groupDecorations,
		                                       BlockDecorative.getItemStackNoCache(EnumDecorativeType.STRIPES_YELLOW_DOWN, 7), false, "yss", "sss", "ssb",
		                                       's', "warpDecorative",
		                                       'b', "dyeBlack",
		                                       'y', "dyeYellow" ), "_dye");
		WarpDrive.register(new ShapedOreRecipe(groupDecorations,
		                                       BlockDecorative.getItemStackNoCache(EnumDecorativeType.STRIPES_YELLOW_UP, 7), false, "ssb", "sss", "yss",
		                                       's', "warpDecorative",
		                                       'b', "dyeBlack",
		                                       'y', "dyeYellow" ), "_dye");
		
		// stripes can toggled to each others (reducing dye consumption)
		WarpDrive.register(new ShapelessOreRecipe(groupDecorations,
		                                          BlockDecorative.getItemStackNoCache(EnumDecorativeType.STRIPES_BLACK_DOWN, 1),
		                                          BlockDecorative.getItemStackNoCache(EnumDecorativeType.STRIPES_BLACK_UP, 1) ), "_toggle");
		WarpDrive.register(new ShapelessOreRecipe(groupDecorations,
		                                          BlockDecorative.getItemStackNoCache(EnumDecorativeType.STRIPES_BLACK_UP, 1),
		                                          BlockDecorative.getItemStackNoCache(EnumDecorativeType.STRIPES_YELLOW_DOWN, 1) ), "_toggle");
		WarpDrive.register(new ShapelessOreRecipe(groupDecorations,
		                                          BlockDecorative.getItemStackNoCache(EnumDecorativeType.STRIPES_YELLOW_DOWN, 1),
		                                          BlockDecorative.getItemStackNoCache(EnumDecorativeType.STRIPES_YELLOW_UP, 1) ), "_toggle");
		WarpDrive.register(new ShapelessOreRecipe(groupDecorations,
		                                          BlockDecorative.getItemStackNoCache(EnumDecorativeType.STRIPES_YELLOW_UP, 1),
		                                          BlockDecorative.getItemStackNoCache(EnumDecorativeType.STRIPES_BLACK_DOWN, 1) ), "_toggle");
		
		// Lamps
		WarpDrive.register(new ShapedOreRecipe(groupDecorations,
		                                       WarpDrive.blockLamp_bubble, false, " g ", "glg", "h  ",
		                                       'g', "blockGlass",
		                                       'l', Blocks.REDSTONE_LAMP,
		                                       'h', "blockHull1_plain"));
		WarpDrive.register(new ShapedOreRecipe(groupDecorations,
		                                       WarpDrive.blockLamp_flat, false, " g ", "glg", " h ",
		                                       'g', "blockGlass",
		                                       'l', Blocks.REDSTONE_LAMP,
		                                       'h', "blockHull1_plain"));
		WarpDrive.register(new ShapedOreRecipe(groupDecorations,
		                                       WarpDrive.blockLamp_long, false, " g ", "glg", "  h",
		                                       'g', "blockGlass",
		                                       'l', Blocks.REDSTONE_LAMP,
		                                       'h', "blockHull1_plain"));
	}
	
	private static void initDetection() {
		// Biometric scanner
		if (!WarpDriveConfig.ACCELERATOR_ENABLE) {
			WarpDrive.register(new ShapedOreRecipe(groupMachines,
			                                       WarpDrive.blockBiometricScanner, false, "rDr", "EmE", "rCr",
			                                       'r', rubber,
			                                       'm', itemStackMachineCasings[1],
			                                       'E', ItemComponent.getItemStack(EnumComponentType.ENDER_COIL),
			                                       'D', ItemComponent.getItemStack(EnumComponentType.DIAMOND_CRYSTAL),
			                                       'C', ItemComponent.getItemStack(EnumComponentType.COMPUTER_INTERFACE) ));
		} else {
			WarpDrive.register(new ShapedOreRecipe(groupMachines,
			                                       WarpDrive.blockBiometricScanner, false, "rDr", "EmE", "rCr",
			                                       'r', rubber,
			                                       'm', "blockElectromagnet1",
			                                       'E', ItemComponent.getItemStack(EnumComponentType.ENDER_COIL),
			                                       'D', ItemComponent.getItemStack(EnumComponentType.DIAMOND_CRYSTAL),
			                                       'C', ItemComponent.getItemStack(EnumComponentType.COMPUTER_INTERFACE) ));
		}
		
		// Camera is 1 Daylight sensor, 2 Motors, 1 Computer interface, 2 Glass panel, 1 Tuning diamond, 1 LV Machine casing
		WarpDrive.register(new ShapedOreRecipe(groupMachines,
		                                       new ItemStack(WarpDrive.blockCamera), false, "gtd", "zlm", "gt ",
		                                       't', itemStackMotors[0],
		                                       'z', ItemComponent.getItemStack(EnumComponentType.ZOOM),
		                                       'd', ItemComponent.getItemStack(EnumComponentType.DIAMOND_CRYSTAL),
		                                       'm', itemStackMachineCasings[0],
		                                       'l', Blocks.DAYLIGHT_DETECTOR,
		                                       'g', "paneGlassColorless"));
		
		// Cloaking core is 3 Cloaking coils, 4 Iridium blocks, 1 Ship controller, 1 Power interface
		WarpDrive.register(new ShapedOreRecipe(groupMachines,
		                                       new ItemStack(WarpDrive.blockCloakingCore), false, "ici", "csc", "ipi",
		                                       'i', WarpDrive.blockIridium,
		                                       'c', WarpDrive.blockCloakingCoil,
		                                       's', WarpDrive.blockShipControllers[EnumTier.SUPERIOR.getIndex()],
		                                       'p', ItemComponent.getItemStack(EnumComponentType.SUPERCONDUCTOR) ));
		
		// Cloaking coil is 1 Titanium plate, 4 Reinforced iridium plate, 1 EV Machine casing (Ti) or 1 Beacon, 4 Emerald, 4 Diamond
		final Object oreGoldIngotOrCoil = WarpDriveConfig.getOreOrItemStack(
				"gregtech:wire_coil", 3,                   // GregTech Tungstensteel Coil block
				"ic2:crafting", 5,                         // IC2 Experimental Coil
				"thermalfoundation:material", 515,         // ThermalFoundation Redstone conductance coil
				"immersiveengineering:connector", 8,       // ImmersiveEngineering HV Transformer (coils wires are too cheap)
				"enderio:item_power_conduit", 2,           // EnderIO Ender energy conduit
				"minecraft:gold_ingot", 0 );
		final Object oreGoldIngotOrTitaniumPlate = WarpDriveConfig.getOreOrItemStack(
				"ore:plateTitanium", 0,
				"advanced_solar_panels:crafting", 0,	   // ASP Sunnarium
				"ore:plateDenseSteel", 0,
				"thermalfoundation:glass", 6,              // ThermalFoundation Hardened Platinum Glass
				"immersiveengineering:metal_device1", 3,   // ImmersiveEngineering Thermoelectric Generator
				"enderio:item_alloy_ingot", 2,	           // EnderIO Vibrant alloy (ore:ingotVibrantAlloy)
				"minecraft:gold_ingot", 0 );
		final Object oreEmeraldOrIridiumPlate = WarpDriveConfig.getOreOrItemStack(
				"ore:plateIridium", 0,                     // GregTech
				"ore:plateAlloyIridium", 0,                // IndustrialCraft2
				"enderio:item_material", 42,               // EnderIO Frank'N'Zombie
				"ore:ingotLumium", 0,                      // ThermalFoundation lumium ingot
				"ore:gemEmerald", 0 );
		WarpDrive.register(new ShapedOreRecipe(groupMachines,
		                                       new ItemStack(WarpDrive.blockCloakingCoil), false, "iti", "cmc", "iti",
		                                       't', oreGoldIngotOrTitaniumPlate,
		                                       'i', oreEmeraldOrIridiumPlate,
		                                       'c', oreGoldIngotOrCoil,
		                                       'm', itemStackMachineCasings[3] ));
		
		// Environmental sensor
		WarpDrive.register(new ShapedOreRecipe(groupMachines,
		                                       WarpDrive.blockEnvironmentalSensor, "   ", "dcd", "rCr",
		                                       'r', rubber,
		                                       'c', Items.CLOCK,
		                                       'd', Blocks.DAYLIGHT_DETECTOR,
		                                       'C', ItemComponent.getItemStack(EnumComponentType.COMPUTER_INTERFACE) ));
		
		// Monitor is 3 Flat screen, 1 Computer interface, 1 Tuning diamond, 1 LV Machine casing
		WarpDrive.register(new ShapedOreRecipe(groupMachines,
		                                       new ItemStack(WarpDrive.blockMonitor), false, "fd ", "fm ", "f  ",
		                                       'f', ItemComponent.getItemStack(EnumComponentType.FLAT_SCREEN),
		                                       'd', ItemComponent.getItemStack(EnumComponentType.DIAMOND_CRYSTAL),
		                                       'm', itemStackMachineCasings[0]));
		
		// Radar is 1 motor, 4 Titanium plate (diamond), 1 Quarztite rod (nether quartz), 1 Computer interface, 1 HV Machine casing, 1 Power interface
		final Object oreRadarDish = WarpDriveConfig.getOreOrItemStack(
				"ore:plateTitanium", 0,          // GregTech
				"ore:plateEnderium", 0,          // ThermalExpansion
				"ore:ingotVibrantAlloy", 0,      // EnderIO
				"ore:plateAlloyIridium", 0,      // IndustrialCraft2
				"ore:gemQuartz", 0 );
		final Object oreRadarSensor = WarpDriveConfig.getOreOrItemStack(
				"ore:stickQuartzite", 0,         // GregTech
				"ore:ingotSignalum", 0,          // ThermalExpansion
				"ore:nuggetPulsatingIron", 0,    // EnderIO
				"minecraft:ghast_tear", 0 );
		WarpDrive.register(new ShapedOreRecipe(groupMachines,
		                                       new ItemStack(WarpDrive.blockRadar), false, "PAP", "PtP", "pmc",
		                                       't', itemStackMotors[2],
		                                       'P', oreRadarDish,
		                                       'A', oreRadarSensor,
		                                       'c', ItemComponent.getItemStack(EnumComponentType.COMPUTER_INTERFACE),
		                                       'm', itemStackMachineCasings[2],
		                                       'p', ItemComponent.getItemStack(EnumComponentType.SUPERCONDUCTOR) ));
		
		// Sirens
		WarpDrive.register(new ShapedOreRecipe(groupMachines,
		                                       WarpDrive.blockSirenIndustrial[EnumTier.BASIC.getIndex()], "pip", "pNp", "pip",
		                                       'p', "plankWood",
		                                       'i', "ingotIron",
		                                       'N', new ItemStack(Blocks.NOTEBLOCK, 1) ));
		WarpDrive.register(new ShapedOreRecipe(groupMachines,
		                                       WarpDrive.blockSirenIndustrial[EnumTier.ADVANCED.getIndex()], " I ", "ISI", " I ",
		                                       'I', "ingotGold",
		                                       'S', WarpDrive.blockSirenIndustrial[EnumTier.BASIC.getIndex()] ));
		WarpDrive.register(new ShapedOreRecipe(groupMachines,
		                                       WarpDrive.blockSirenIndustrial[EnumTier.SUPERIOR.getIndex()], " I ", "ISI", " I ",
		                                       'I', "gemDiamond",
		                                       'S', WarpDrive.blockSirenIndustrial[EnumTier.ADVANCED.getIndex()] ));
		
		WarpDrive.register(new ShapedOreRecipe(groupMachines,
		                                       WarpDrive.blockSirenMilitary[EnumTier.BASIC.getIndex()], "ppp", "iNi", "ppp",
		                                       'p', "plankWood",
		                                       'i', "ingotIron",
		                                       'N', new ItemStack(Blocks.NOTEBLOCK, 1) ));
		WarpDrive.register(new ShapedOreRecipe(groupMachines,
		                                       WarpDrive.blockSirenMilitary[EnumTier.ADVANCED.getIndex()], " I ", "ISI", " I ",
		                                       'I', "ingotGold",
		                                       'S', WarpDrive.blockSirenMilitary[EnumTier.BASIC.getIndex()] ));
		WarpDrive.register(new ShapedOreRecipe(groupMachines,
		                                       WarpDrive.blockSirenMilitary[EnumTier.SUPERIOR.getIndex()], " I ", "ISI", " I ",
		                                       'I', "gemDiamond",
		                                       'S', WarpDrive.blockSirenMilitary[EnumTier.ADVANCED.getIndex()] ));
		
		// Speakers
		WarpDrive.register(new ShapedOreRecipe(groupMachines,
		                                       WarpDrive.blockSpeaker[EnumTier.BASIC.getIndex()], "BBB", "rDr", "rCr",
		                                       'B', ItemComponent.getItemStack(EnumComponentType.BIOFIBER),
		                                       'r', rubber,
		                                       'D', ItemComponent.getItemStack(EnumComponentType.DIAMOND_CRYSTAL),
		                                       'C', ItemComponent.getItemStack(EnumComponentType.COMPUTER_INTERFACE) ));
		WarpDrive.register(new ShapedOreRecipe(groupMachines,
		                                       WarpDrive.blockSpeaker[EnumTier.ADVANCED.getIndex()], " I ", "ISI", " I ",
		                                       'I', "ingotGold",
		                                       'S', WarpDrive.blockSpeaker[EnumTier.BASIC.getIndex()] ));
		WarpDrive.register(new ShapedOreRecipe(groupMachines,
		                                       WarpDrive.blockSpeaker[EnumTier.SUPERIOR.getIndex()], " I ", "ISI", " I ",
		                                       'I', "gemDiamond",
		                                       'S', WarpDrive.blockSpeaker[EnumTier.ADVANCED.getIndex()] ));
		
		// Virtual assistants
		WarpDrive.register(new ShapedOreRecipe(groupMachines,
		                                       WarpDrive.blockVirtualAssistant[EnumTier.BASIC.getIndex()], "BEB", "rmr", "rCr",
		                                       'B', ItemComponent.getItemStack(EnumComponentType.BIOFIBER),
		                                       'm', itemStackMachineCasings[1],
		                                       'r', rubber,
		                                       'E', ItemComponent.getItemStack(EnumComponentType.EMERALD_CRYSTAL),
		                                       'C', ItemComponent.getItemStack(EnumComponentType.COMPUTER_INTERFACE) ));
		WarpDrive.register(new ShapedOreRecipe(groupMachines,
		                                       WarpDrive.blockVirtualAssistant[EnumTier.ADVANCED.getIndex()], "DCD", "CSC", "DCD",
		                                       'D', ItemComponent.getItemStack(EnumComponentType.DIAMOND_COIL),
		                                       'C', ItemComponent.getItemStack(EnumComponentType.DIAMOND_CRYSTAL),
		                                       'S', WarpDrive.blockVirtualAssistant[EnumTier.BASIC.getIndex()] ));
		WarpDrive.register(new ShapedOreRecipe(groupMachines,
		                                       WarpDrive.blockVirtualAssistant[EnumTier.SUPERIOR.getIndex()], "EYE", "YSY", "EYE",
		                                       'E', ItemComponent.getItemStack(EnumComponentType.ENDER_COIL),
		                                       'Y', Items.ENDER_EYE,
		                                       'S', WarpDrive.blockVirtualAssistant[EnumTier.ADVANCED.getIndex()] ));
		
		// Warp isolation is 1 EV Machine casing (Ti), 4 Titanium plate/Enderium ingot/Vibrant alloy/Iridium plate/quartz
		WarpDrive.register(new ShapedOreRecipe(groupMachines,
		                                       new ItemStack(WarpDrive.blockWarpIsolation), false, "i i", " m ", "i i",
		                                       'i', oreRadarDish,
		                                       'm', itemStackMachineCasings[3]));
	}
	
	private static void initEnergy() {
		// IC2 needs to be loaded for the following 2 recipes
		if (WarpDriveConfig.isIndustrialCraft2Loaded) {
			final Object overclockedHeatVent = WarpDriveConfig.getOreOrItemStack(
					"ic2:overclocked_heat_vent", 0,      // IC2 Experimental Overclocked heat vent
					"ic2:itemheatvent", 2 );             // IC2 Classic Overclocked heat vent (not the electric variant)
			// (there's no coolant in GT6 version 6.06.05, nor in GregTech CE version 1.12.2-0.4.5.9, so we're falling back to IC2)
			final Object reactorCoolant = WarpDriveConfig.getOreOrItemStack(
					"ic2:hex_heat_storage", 0,           // IC2 Experimental 60k Coolant Cell
					"ic2:itemheatstorage", 2 );          // IC2 Classic 60k Coolant Cell
			
			WarpDrive.register(new ShapedOreRecipe(groupMachines,
			                                       new ItemStack(WarpDrive.itemIC2reactorLaserFocus), false, "cld", "lhl", "dlc",
			                                       'l', ItemComponent.getItemStack(EnumComponentType.LENS),
			                                       'h', overclockedHeatVent,
			                                       'c', reactorCoolant,
			                                       'd', reactorCoolant ));
			
			WarpDrive.register(new ShapedOreRecipe(groupMachines,
			                                       new ItemStack(WarpDrive.blockIC2reactorLaserCooler), false, "gCp", "lme", "gC ",
			                                       'l', ItemComponent.getItemStack(EnumComponentType.LENS),
			                                       'e', ItemComponent.getItemStack(EnumComponentType.EMERALD_CRYSTAL),
			                                       'C', ItemComponent.getItemStack(EnumComponentType.CAPACITIVE_CRYSTAL),
			                                       'p', ItemComponent.getItemStack(EnumComponentType.POWER_INTERFACE),
			                                       'g', "paneGlassColorless",
			                                       'm', itemStackMachineCasings[1] ));
		}
		
		// Enantiomorphic reactor core is 1 EV Machine casing, 4 Capacitive crystal, 1 Computer interface, 1 Power interface, 2 Lenses
		if (!WarpDriveConfig.ACCELERATOR_ENABLE) {
			WarpDrive.register(new ShapedOreRecipe(groupMachines,
			                                       WarpDrive.blockEnanReactorCores[EnumTier.BASIC.getIndex()], false, "CpC", "lml", "CcC",
			                                       'm', ItemComponent.getItemStack(EnumComponentType.REACTOR_CORE),
			                                       'l', ItemComponent.getItemStack(EnumComponentType.LENS),
			                                       'p', ItemComponent.getItemStack(EnumComponentType.POWER_INTERFACE),
			                                       'c', ItemComponent.getItemStack(EnumComponentType.COMPUTER_INTERFACE),
			                                       'C', ItemComponent.getItemStack(EnumComponentType.CAPACITIVE_CRYSTAL)));
		} else {
			WarpDrive.register(new ShapedOreRecipe(groupMachines,
			                                       WarpDrive.blockEnanReactorCores[EnumTier.BASIC.getIndex()], false, " p ", "lCl", "cpm",
			                                       'C', ItemComponent.getItemStack(EnumComponentType.REACTOR_CORE),
			                                       'l', ItemComponent.getItemStack(EnumComponentType.LENS),
			                                       'p', ItemComponent.getItemStack(EnumComponentType.POWER_INTERFACE),
			                                       'c', ItemComponent.getItemStack(EnumComponentType.COMPUTER_INTERFACE),
			                                       'm', itemStackMachineCasings[2]));
		}
		WarpDrive.register(new ShapedOreRecipe(groupMachines,
		                                       WarpDrive.blockEnanReactorCores[EnumTier.ADVANCED.getIndex()], false, "lcl", "CRC", "lcl",
		                                       'C', ItemComponent.getItemStack(EnumComponentType.REACTOR_CORE),
		                                       'R', WarpDrive.blockEnanReactorCores[EnumTier.BASIC.getIndex()],
		                                       'l', ItemComponent.getItemStack(EnumComponentType.LENS),
		                                       'c', ItemComponent.getItemStack(EnumComponentType.CAPACITIVE_CRYSTAL) ));
		WarpDrive.register(new ShapedOreRecipe(groupMachines,
		                                       WarpDrive.blockEnanReactorCores[EnumTier.SUPERIOR.getIndex()], false, "lSl", "CRC", "lSl",
		                                       'C', ItemComponent.getItemStack(EnumComponentType.REACTOR_CORE),
		                                       'R', WarpDrive.blockEnanReactorCores[EnumTier.ADVANCED.getIndex()],
		                                       'l', ItemComponent.getItemStack(EnumComponentType.LENS),
		                                       'S', ItemComponent.getItemStack(EnumComponentType.SUPERCONDUCTOR) ));
		
		// Enantiomorphic reactor stabilization laser is 1 HV Machine casing, 2 Advanced hull, 1 Computer interface, 1 Power interface, 1 Lens, 1 Redstone, 2 Glass panes
		WarpDrive.register(new ShapedOreRecipe(groupMachines,
		                                       new ItemStack(WarpDrive.blockEnanReactorLaser), false, "g h", "ldm", "g c",
		                                       'd', ItemComponent.getItemStack(EnumComponentType.DIFFRACTION_GRATING),
		                                       'l', ItemComponent.getItemStack(EnumComponentType.LENS),
		                                       'c', ItemComponent.getItemStack(EnumComponentType.COMPUTER_INTERFACE),
		                                       'm', itemStackMachineCasings[1],
		                                       'g', "paneGlassColorless",
		                                       'h', "blockHull2_plain"));
		
		// Basic subspace capacitor is 1 Capacitive crystal, 1 Gold ingot, 3 Bio fiber, 4 Iron bars
		WarpDrive.register(new ShapedOreRecipe(groupMachines,
		                                       WarpDrive.blockCapacitor[EnumTier.BASIC.getIndex()], false, "iPi", "pcp", "ipi",
		                                       'c', ItemComponent.getItemStack(EnumComponentType.CAPACITIVE_CRYSTAL),
		                                       'i', barsIron,
		                                       'p', "itemBiofiber",
		                                       'P', ItemComponent.getItemStack(EnumComponentType.POWER_INTERFACE) ));
		
		// Advanced subspace capacitor is 2 Capacitive crystal, 1 Power interface, 4 Rubber, 2 Gold ingot
		// Advanced subspace capacitor is 2 Basic subspace capacitor, 1 Power interface
		WarpDrive.register(new ShapedOreRecipe(groupMachines,
		                                       WarpDrive.blockCapacitor[EnumTier.ADVANCED.getIndex()], false, "rir", "cpc", "rir",
		                                       'c', ItemComponent.getItemStack(EnumComponentType.CAPACITIVE_CRYSTAL),
		                                       'i', "ingotIron",
		                                       'r', "itemRubber",
		                                       'p', ItemComponent.getItemStack(EnumComponentType.POWER_INTERFACE) ), "_direct");
		// or 2 Basic subspace capacitor, 1 Power interface
		WarpDrive.register(new ShapedOreRecipe(groupMachines,
		                                       WarpDrive.blockCapacitor[EnumTier.ADVANCED.getIndex()], false, "r r", "cpc", "r r",
		                                       'c', new ItemStack(WarpDrive.blockCapacitor[EnumTier.BASIC.getIndex()]),
		                                       'r', "itemRubber",
		                                       'p', ItemComponent.getItemStack(EnumComponentType.POWER_INTERFACE) ), "_upgrade");
		
		// Superior subspace capacitor is 1 Capacitive cluster, 4 Carbon fiber, 2 Power interface, 1 Gold ingot, 1 Superconductor
		WarpDrive.register(new ShapedOreRecipe(groupMachines,
		                                       WarpDrive.blockCapacitor[EnumTier.SUPERIOR.getIndex()], false, "psp", "ici", "pgp",
		                                       'c', ItemComponent.getItemStack(EnumComponentType.CAPACITIVE_CLUSTER),
		                                       'p', "plateCarbon",
		                                       'i', ItemComponent.getItemStack(EnumComponentType.POWER_INTERFACE),
		                                       'g', "ingotGold",
		                                       's', ItemComponent.getItemStack(EnumComponentType.SUPERCONDUCTOR) ), "_direct");
		// or 2 Advanced subspace capacitor, 4 Carbon fiber, 1 Superconductor
		WarpDrive.register(new ShapedOreRecipe(groupMachines,
		                                       WarpDrive.blockCapacitor[EnumTier.SUPERIOR.getIndex()], false, "p p", "csc", "p p",
		                                       'c', new ItemStack(WarpDrive.blockCapacitor[EnumTier.ADVANCED.getIndex()]),
		                                       'p', "plateCarbon",
		                                       's', ItemComponent.getItemStack(EnumComponentType.SUPERCONDUCTOR) ), "_upgrade");
	}
	
	private static void initForceField() {
		// *** Force field shapes
		// Force field shapes are 1 Memory crystal, 3 to 5 Coil crystal
		WarpDrive.register(new ShapedOreRecipe(groupComponents,
		                                       ItemForceFieldShape.getItemStack(EnumForceFieldShape.SPHERE), false, "   ", "CmC", "CCC",
		                                       'C', ItemComponent.getItemStack(EnumComponentType.DIAMOND_COIL),
		                                       'm', ItemComponent.getItemStack(EnumComponentType.MEMORY_CRYSTAL)));
		WarpDrive.register(new ShapedOreRecipe(groupComponents,
		                                       ItemForceFieldShape.getItemStack(EnumForceFieldShape.CYLINDER_H), false, "C C", " m ", "C C",
		                                       'C', ItemComponent.getItemStack(EnumComponentType.DIAMOND_COIL),
		                                       'm', ItemComponent.getItemStack(EnumComponentType.MEMORY_CRYSTAL)));
		WarpDrive.register(new ShapedOreRecipe(groupComponents,
		                                       ItemForceFieldShape.getItemStack(EnumForceFieldShape.CYLINDER_V), false, " C ", "CmC", " C ",
		                                       'C', ItemComponent.getItemStack(EnumComponentType.DIAMOND_COIL),
		                                       'm', ItemComponent.getItemStack(EnumComponentType.MEMORY_CRYSTAL)));
		WarpDrive.register(new ShapedOreRecipe(groupComponents,
		                                       ItemForceFieldShape.getItemStack(EnumForceFieldShape.CUBE), false, "CCC", "CmC", "   ",
		                                       'C', ItemComponent.getItemStack(EnumComponentType.DIAMOND_COIL),
		                                       'm', ItemComponent.getItemStack(EnumComponentType.MEMORY_CRYSTAL)));
		WarpDrive.register(new ShapedOreRecipe(groupComponents,
		                                       ItemForceFieldShape.getItemStack(EnumForceFieldShape.PLANE), false, "CCC", " m ", "   ",
		                                       'C', ItemComponent.getItemStack(EnumComponentType.DIAMOND_COIL),
		                                       'm', ItemComponent.getItemStack(EnumComponentType.MEMORY_CRYSTAL)));
		WarpDrive.register(new ShapedOreRecipe(groupComponents,
		                                       ItemForceFieldShape.getItemStack(EnumForceFieldShape.TUBE), false, "   ", "CmC", "C C",
		                                       'C', ItemComponent.getItemStack(EnumComponentType.DIAMOND_COIL),
		                                       'm', ItemComponent.getItemStack(EnumComponentType.MEMORY_CRYSTAL)));
		WarpDrive.register(new ShapedOreRecipe(groupComponents,
		                                       ItemForceFieldShape.getItemStack(EnumForceFieldShape.TUNNEL), false, "C C", "CmC", "   ",
		                                       'C', ItemComponent.getItemStack(EnumComponentType.DIAMOND_COIL),
		                                       'm', ItemComponent.getItemStack(EnumComponentType.MEMORY_CRYSTAL)));
		
		// *** Force field upgrades
		// Force field attraction upgrade is 3 Coil crystal, 1 Iron block, 2 Redstone block, 1 MV motor
		WarpDrive.register(new ShapedOreRecipe(groupComponents,
		                                       ItemForceFieldUpgrade.getItemStack(EnumForceFieldUpgrade.ATTRACTION), false, "CCC", "rir", " m ",
		                                       'C', ItemComponent.getItemStack(EnumComponentType.DIAMOND_COIL),
		                                       'r', "blockRedstone",
		                                       'i', Blocks.IRON_BLOCK,
		                                       'm', itemStackMotors[1]));
		// Force field breaking upgrade is 3 Coil crystal, 1 Diamond axe, 1 Diamond shovel, 1 Diamond pick, gives 2
		WarpDrive.register(new ShapedOreRecipe(groupComponents,
		                                       ItemForceFieldUpgrade.getItemStackNoCache(EnumForceFieldUpgrade.BREAKING, 2), false, "CCC", "sap", "   ",
		                                       'C', ItemComponent.getItemStack(EnumComponentType.DIAMOND_COIL),
		                                       's', Items.DIAMOND_AXE,
		                                       'a', Items.DIAMOND_SHOVEL,
		                                       'p', Items.DIAMOND_PICKAXE));
		// Force field camouflage upgrade is 3 Coil crystal, 2 Diffraction grating, 1 Zoom, 1 Emerald crystal
		WarpDrive.register(new ShapedOreRecipe(groupComponents,
		                                       ItemForceFieldUpgrade.getItemStack(EnumForceFieldUpgrade.CAMOUFLAGE), false, "CCC", "zre", "   ",
		                                       'C', ItemComponent.getItemStack(EnumComponentType.DIAMOND_COIL),
		                                       'z', ItemComponent.getItemStack(EnumComponentType.ZOOM),
		                                       'r', Blocks.DAYLIGHT_DETECTOR,
		                                       'e', ItemComponent.getItemStack(EnumComponentType.EMERALD_CRYSTAL)));
		// Force field cooling upgrade is 3 Coil crystal, 2 Ice, 1 MV Motor
		WarpDrive.register(new ShapedOreRecipe(groupComponents,
		                                       ItemForceFieldUpgrade.getItemStack(EnumForceFieldUpgrade.COOLING), false, "CCC", "imi", "   ",
		                                       'C', ItemComponent.getItemStack(EnumComponentType.DIAMOND_COIL),
		                                       'i', Blocks.ICE,
		                                       'm', ItemComponent.getItemStack(EnumComponentType.PUMP) ));
		// Force field fusion upgrade is 3 Coil crystal, 2 Computer interface, 1 Emerald crystal
		WarpDrive.register(new ShapedOreRecipe(groupComponents,
		                                       ItemForceFieldUpgrade.getItemStack(EnumForceFieldUpgrade.FUSION), false, "CCC", "cec", "   ",
		                                       'C', ItemComponent.getItemStack(EnumComponentType.DIAMOND_COIL),
		                                       'c', ItemComponent.getItemStack(EnumComponentType.COMPUTER_INTERFACE),
		                                       'e', ItemComponent.getItemStack(EnumComponentType.EMERALD_CRYSTAL) ));
		// Force field heating upgrade is 3 Coil crystal, 2 Blaze rod, 1 MV Motor
		WarpDrive.register(new ShapedOreRecipe(groupComponents,
		                                       ItemForceFieldUpgrade.getItemStack(EnumForceFieldUpgrade.HEATING), false, "CCC", "bmb", "   ",
		                                       'C', ItemComponent.getItemStack(EnumComponentType.DIAMOND_COIL),
		                                       'b', Items.BLAZE_ROD,
		                                       'm', ItemComponent.getItemStack(EnumComponentType.PUMP) ));
		// Force field inversion upgrade is 3 Coil crystal, 1 Gold nugget, 2 Redstone
		WarpDrive.register(new ShapedOreRecipe(groupComponents,
		                                       ItemForceFieldUpgrade.getItemStack(EnumForceFieldUpgrade.INVERSION), false, "rgr", "CCC", "CCC",
		                                       'C', ItemComponent.getItemStack(EnumComponentType.DIAMOND_COIL),
		                                       'r', Items.REDSTONE,
		                                       'g', Items.GOLD_NUGGET ));
		// Force field item port upgrade is 3 Coil crystal, 3 wooden chests, 1 MV motor
		WarpDrive.register(new ShapedOreRecipe(groupComponents,
		                                       ItemForceFieldUpgrade.getItemStack(EnumForceFieldUpgrade.ITEM_PORT), false, "CCC", "cmc", " c ",
		                                       'C', ItemComponent.getItemStack(EnumComponentType.DIAMOND_COIL),
		                                       'c', "chestWood",
		                                       'm', itemStackMotors[1] ));
		// Force field silencer upgrade is 3 Coil crystal, 3 Wool
		WarpDrive.register(new ShapedOreRecipe(groupComponents,
		                                       ItemForceFieldUpgrade.getItemStack(EnumForceFieldUpgrade.SILENCER), false, "CCC", "www", "   ",
		                                       'C', ItemComponent.getItemStack(EnumComponentType.DIAMOND_COIL),
		                                       'w', Blocks.WOOL ));
		// Force field pumping upgrade is 3 Coil crystal, 1 MV Motor, 2 Glass tanks
		WarpDrive.register(new ShapedOreRecipe(groupComponents,
		                                       ItemForceFieldUpgrade.getItemStack(EnumForceFieldUpgrade.PUMPING), false, "CCC", "tmt", "   ",
		                                       'C', ItemComponent.getItemStack(EnumComponentType.DIAMOND_COIL),
		                                       'm', ItemComponent.getItemStack(EnumComponentType.PUMP),
		                                       't', ItemComponent.getItemStack(EnumComponentType.GLASS_TANK) ));
		// Force field range upgrade is 3 Coil crystal, 2 Memory crystal, 1 Redstone block
		WarpDrive.register(new ShapedOreRecipe(groupComponents,
		                                       ItemForceFieldUpgrade.getItemStack(EnumForceFieldUpgrade.RANGE), false, "CCC", "RMR", "   ",
		                                       'C', ItemComponent.getItemStack(EnumComponentType.DIAMOND_COIL),
		                                       'M', ItemComponent.getItemStack(EnumComponentType.MEMORY_CRYSTAL),
		                                       'R', "blockRedstone" ));
		// Force field repulsion upgrade is 3 Coil crystal, 1 Iron block, 2 Redstone block, 1 MV motor
		WarpDrive.register(new ShapedOreRecipe(groupComponents,
		                                       ItemForceFieldUpgrade.getItemStack(EnumForceFieldUpgrade.REPULSION), false, " m ", "rir", "CCC",
		                                       'C', ItemComponent.getItemStack(EnumComponentType.DIAMOND_COIL),
		                                       'r', "blockRedstone",
		                                       'i', Blocks.IRON_BLOCK,
		                                       'm', itemStackMotors[1] ));
		// Force field rotation upgrade is 3 Coil crystal, 2 MV Motors, 1 Computer interface
		WarpDrive.register(new ShapedOreRecipe(groupComponents,
		                                       ItemForceFieldUpgrade.getItemStackNoCache(EnumForceFieldUpgrade.ROTATION, 2), false, "CCC", " m ", " mc",
		                                       'C', ItemComponent.getItemStack(EnumComponentType.DIAMOND_COIL),
		                                       'm', itemStackMotors[1],
		                                       'c', ItemComponent.getItemStack(EnumComponentType.COMPUTER_INTERFACE) ));
		// Force field shock upgrade is 3 Coil crystal, 1 Power interface
		WarpDrive.register(new ShapedOreRecipe(groupComponents,
		                                       ItemForceFieldUpgrade.getItemStack(EnumForceFieldUpgrade.SHOCK), false, "CCC", " p ", "   ",
		                                       'C', ItemComponent.getItemStack(EnumComponentType.DIAMOND_COIL),
		                                       'p', ItemComponent.getItemStack(EnumComponentType.POWER_INTERFACE) ));
		// Force field speed upgrade is 3 Coil crystal, 2 Ghast tear, 1 Emerald crystal
		WarpDrive.register(new ShapedOreRecipe(groupComponents,
		                                       ItemForceFieldUpgrade.getItemStack(EnumForceFieldUpgrade.SPEED), false, "CCC", "geg", "   ",
		                                       'C', ItemComponent.getItemStack(EnumComponentType.DIAMOND_COIL),
		                                       'g', Items.GHAST_TEAR,
		                                       'e', ItemComponent.getItemStack(EnumComponentType.EMERALD_CRYSTAL) ));
		// Force field stabilization upgrade is 3 Coil crystal, 1 Memory crystal, 2 Lapis block
		WarpDrive.register(new ShapedOreRecipe(groupComponents,
		                                       ItemForceFieldUpgrade.getItemStack(EnumForceFieldUpgrade.STABILIZATION), "CCC", "lMl", "   ",
		                                       'C', ItemComponent.getItemStack(EnumComponentType.DIAMOND_COIL),
		                                       'M', ItemComponent.getItemStack(EnumComponentType.MEMORY_CRYSTAL),
		                                       'l', "blockLapis" ));
		// Force field thickness upgrade is 8 Coil crystal, 1 Diamond crystal
		WarpDrive.register(new ShapedOreRecipe(groupComponents,
		                                       ItemForceFieldUpgrade.getItemStack(EnumForceFieldUpgrade.THICKNESS), false, "CCC", "CpC", "   ",
		                                       'C', ItemComponent.getItemStack(EnumComponentType.DIAMOND_COIL),
		                                       'p', ItemComponent.getItemStack(EnumComponentType.ELECTROMAGNETIC_PROJECTOR)));
		// Force field translation upgrade is 3 Coil crystal, 2 MV Motor, 1 Computer interface
		WarpDrive.register(new ShapedOreRecipe(groupComponents,
		                                       ItemForceFieldUpgrade.getItemStackNoCache(EnumForceFieldUpgrade.TRANSLATION, 2), false, "CCC", "m m", " c ",
		                                       'C', ItemComponent.getItemStack(EnumComponentType.DIAMOND_COIL),
		                                       'm', itemStackMotors[1],
		                                       'c', ItemComponent.getItemStack(EnumComponentType.COMPUTER_INTERFACE) ));
		
		// Force field projector is 1 or 2 Electromagnetic Projector, 1 LV/MV/HV Machine casing, 1 Ender crystal, 1 Redstone
		for (final EnumTier enumTier : EnumTier.nonCreative()) {
			final int index = enumTier.getIndex();
			WarpDrive.register(new ShapedOreRecipe(groupMachines,
			                                       new ItemStack(WarpDrive.blockForceFieldProjectors[index], 1, 0), false, " e ", "pm ", " r ",
			                                       'p', ItemComponent.getItemStack(EnumComponentType.ELECTROMAGNETIC_PROJECTOR),
			                                       'm', itemStackMachineCasings[index],
			                                       'e', ItemComponent.getItemStack(EnumComponentType.ENDER_COIL),
			                                       'r', Items.REDSTONE), "_left");
			WarpDrive.register(new ShapedOreRecipe(groupMachines,
			                                       new ItemStack(WarpDrive.blockForceFieldProjectors[index], 1, 0), false, " e ", " mp", " r ",
			                                       'p', ItemComponent.getItemStack(EnumComponentType.ELECTROMAGNETIC_PROJECTOR),
			                                       'm', itemStackMachineCasings[index],
			                                       'e', ItemComponent.getItemStack(EnumComponentType.ENDER_COIL),
			                                       'r', Items.REDSTONE), "_right");
			WarpDrive.register(new ShapedOreRecipe(groupMachines,
			                                       new ItemStack(WarpDrive.blockForceFieldProjectors[index], 1, 1), false, " e ", "pmp", " r ",
			                                       'p', ItemComponent.getItemStack(EnumComponentType.ELECTROMAGNETIC_PROJECTOR),
			                                       'm', itemStackMachineCasings[index],
			                                       'e', ItemComponent.getItemStack(EnumComponentType.ENDER_COIL),
			                                       'r', Items.REDSTONE));
		}
		
		// Force field relay is 2 Coil crystals, 1 LV/MV/HV Machine casing, 1 Ender crystal, 1 Redstone
		for (final EnumTier enumTier : EnumTier.nonCreative()) {
			final int index = enumTier.getIndex();
			WarpDrive.register(new ShapedOreRecipe(groupMachines,
			                                       new ItemStack(WarpDrive.blockForceFieldRelays[index]), false, " e ", "CmC", " r ",
			                                       'C', ItemComponent.getItemStack(EnumComponentType.DIAMOND_COIL),
			                                       'm', itemStackMachineCasings[index],
			                                       'e', ItemComponent.getItemStack(EnumComponentType.ENDER_COIL),
			                                       'r', Items.REDSTONE));
		}
	}
	
	private static void initHull() {
		// *** Hull blocks plain
		
		for (final EnumDyeColor enumDyeColor : EnumDyeColor.values()) {
			final int metadataColor = enumDyeColor.getMetadata();
			
			// Tier 1 = 4 obsidian, 4 reinforced stone gives 10
			//  IC2 Reinforced stone is 1 scaffolding = 7.5 * 144 / 16 = 67.5 mB of Iron
			//  => 27 mB of Iron per Basic hull
			if (WarpDriveConfig.isIndustrialCraft2Loaded) {
				final ItemStack reinforcedStone = (ItemStack) WarpDriveConfig.getOreOrItemStack("ic2:resource", 11,       // IC2 Experimental Reinforced stone
				                                                                                "ic2:blockutility", 2 );  // IC2 Classic Reinforced stone (not cracked)
				WarpDrive.register(new ShapedOreRecipe(groupHulls,
				                                       new ItemStack(WarpDrive.blockHulls_plain[EnumTier.BASIC.getIndex()][0], 10, metadataColor), false, "cbc", "bXb", "cbc",
				                                       'b', reinforcedStone,
				                                       'c', Blocks.OBSIDIAN,
				                                       'X', oreDyes.get(enumDyeColor) ), "_ic2");
			}
			
			// Tier 1 = 1 concrete, 3 iron bars, 1 ceramic gives 4
			//  1 Iron bar is 6 * 144 / 16 = 54 mB of Iron
			//  => 40.5 mB of Iron per Basic hull (close to reinforced stone production)
			WarpDrive.register(new ShapedOreRecipe(groupTaintedHulls,
			                                       new ItemStack(WarpDrive.blockHulls_plain[EnumTier.BASIC.getIndex()][0], 4, metadataColor), false, " b ", "bcb", " C ",
			                                       'c', new ItemStack(Blocks.CONCRETE, 1, metadataColor),
			                                       'b', barsIron,
			                                       'C', "itemCeramic" ), "_ceramic");
			
			// Tier 1 = 5 stone, 4 steel ingots gives 10
			// Tier 1 = 5 stone, 4 iron ingots gives 10
			//  => 57.6 mB of Iron/Steel per hull (twice more expensive using less crafting steps)
			final Object ingotSteelOrIron = WarpDriveConfig.getOreOrItemStack("ore:ingotSteel", 0,
			                                                                  "ore:ingotRefinedIron", 0,
			                                                                  "ore:ingotIron", 0 );
			WarpDrive.register(new ShapedOreRecipe(groupHulls,
			                                       new ItemStack(WarpDrive.blockHulls_plain[EnumTier.BASIC.getIndex()][0], 10, metadataColor), false, "cbc", "bXb", "cbc",
			                                       'b', ingotSteelOrIron,
			                                       'c', "stone",
			                                       'X', oreDyes.get(enumDyeColor) ), "_steel");
			
			// Tier 1 = 5 stone, 4 bronze ingots gives 6
			//  => 96 mB of Bronze (almost twice more expensive using an common alloy) 
			if (OreDictionary.doesOreNameExist("ingotBronze") && !OreDictionary.getOres("ingotBronze").isEmpty()) {
				WarpDrive.register(new ShapedOreRecipe(groupHulls,
				                                       new ItemStack(WarpDrive.blockHulls_plain[EnumTier.BASIC.getIndex()][0], 5, metadataColor), false, "cbc", "bXb", "cbc",
				                                       'b', "ingotBronze",
				                                       'c', "stone",
				                                       'X', oreDyes.get(enumDyeColor) ), "_bronze");
			}
			
			// Tier 1 = 5 stone, 4 aluminium ingots gives 3
			//  => 192 mB of Aluminium (very expensive with frequent but hardly used metal)
			if (OreDictionary.doesOreNameExist("ingotAluminium") && !OreDictionary.getOres("ingotAluminium").isEmpty()) {
				WarpDrive.register(new ShapedOreRecipe(groupHulls,
				                                       new ItemStack(WarpDrive.blockHulls_plain[EnumTier.BASIC.getIndex()][0], 3, metadataColor), false, "cbc", "bXb", "cbc",
				                                       'b', "ingotAluminium",
				                                       'c', "stone",
				                                       'X', oreDyes.get(enumDyeColor) ), "_aluminium");
			} else if (OreDictionary.doesOreNameExist("ingotAluminum") && !OreDictionary.getOres("ingotAluminum").isEmpty()) {
				WarpDrive.register(new ShapedOreRecipe(groupHulls,
				                                       new ItemStack(WarpDrive.blockHulls_plain[EnumTier.BASIC.getIndex()][0], 3, metadataColor), false, "cbc", "bXb", "cbc",
				                                       'b', "ingotAluminum",
				                                       'c', "stone",
				                                       'X', oreDyes.get(enumDyeColor) ), "_aluminum");
			}
		}
		
		// Tier 2 = 4 Tier 1, 4 GregTech 5 TungstenSteel reinforced block, IC2 Carbon plate, DarkSteel ingots or Obsidian, gives 4
		final Object oreObsidianTungstenSteelPlate = WarpDriveConfig.getOreOrItemStack(
				"ore:plateTungstenSteel", 0,     // GregTech CE TungstenSteel Plate
				"ic2:crafting", 15,              // IC2 Experimental Carbon plate
				"ic2:itemmisc", 256,             // IC2 Classic Carbon plate
				"thermalfoundation:glass", 3,    // ThermalFoundation Hardened glass
				"ore:ingotDarkSteel", 0,         // EnderIO DarkSteel ingot
				"minecraft:obsidian", 0 );
		for (final EnumDyeColor enumDyeColor : EnumDyeColor.values()) {
			final int metadataColor = enumDyeColor.getMetadata();
			WarpDrive.register(new ShapedOreRecipe(groupTaintedHulls,
			                                       new ItemStack(WarpDrive.blockHulls_plain[EnumTier.ADVANCED.getIndex()][0], 4, metadataColor), false, "cbc", "b b", "cbc",
			                                       'b', new ItemStack(WarpDrive.blockHulls_plain[EnumTier.BASIC.getIndex()][0], 1, metadataColor),
			                                       'c', oreObsidianTungstenSteelPlate ));
			WarpDrive.register(new ShapedOreRecipe(groupTaintedHulls,
			                                       new ItemStack(WarpDrive.blockHulls_plain[EnumTier.ADVANCED.getIndex()][0], 4, metadataColor), false, "cbc", "bXb", "cbc",
			                                       'b', "blockHull1_plain",
			                                       'c', oreObsidianTungstenSteelPlate,
			                                       'X', oreDyes.get(enumDyeColor) ), "_dye");
		}
		
		// Tier 3 = 4 Tier 2, 1 GregTech Naquadah plate, IC2 Iridium plate, EnderIO Pulsating crystal or Diamond, gives 4
		final Object oreDiamondOrNaquadahPlate = WarpDriveConfig.getOreOrItemStack(
				"ore:plateNaquadah", 0,          // GregTech CE Naquadah plate
				"ore:plateAlloyIridium", 0,      // IC2 Iridium alloy
				"ore:itemPulsatingCrystal", 0,   // EnderIO Pulsating crystal
				"ore:gemDiamond", 0 );
		for (final EnumDyeColor enumDyeColor : EnumDyeColor.values()) {
			final int metadataColor = enumDyeColor.getMetadata();
			WarpDrive.register(new ShapedOreRecipe(groupTaintedHulls,
			                                       new ItemStack(WarpDrive.blockHulls_plain[EnumTier.SUPERIOR.getIndex()][0], 4, metadataColor), false, " b ", "bcb", " b ",
			                                       'b', new ItemStack(WarpDrive.blockHulls_plain[EnumTier.ADVANCED.getIndex()][0], 1, metadataColor),
			                                       'c', oreDiamondOrNaquadahPlate ));
			WarpDrive.register(new ShapedOreRecipe(groupTaintedHulls,
			                                       new ItemStack(WarpDrive.blockHulls_plain[EnumTier.SUPERIOR.getIndex()][0], 4, metadataColor), false, "Xb ", "bcb", " b ",
			                                       'b', "blockHull2_plain",
			                                       'c', oreDiamondOrNaquadahPlate,
			                                       'X', oreDyes.get(enumDyeColor) ), "_dye");
		}
		
		// Hull blocks variation
		for (final EnumTier enumTier : EnumTier.nonCreative()) {
			final int index = enumTier.getIndex();
			for (final EnumDyeColor enumDyeColor : EnumDyeColor.values()) {
				final int metadataColor = enumDyeColor.getMetadata();
				
				// crafting glass
				WarpDrive.register(new ShapedOreRecipe(groupHulls,
				                                       new ItemStack(WarpDrive.blockHulls_glass[index], 4, metadataColor), false, "gpg", "pFp", "gpg",
				                                       'g', "blockGlass",
				                                       'p', new ItemStack(WarpDrive.blockHulls_plain[index][0], 1, metadataColor),
				                                       'F', "dustGlowstone" ));
				
				// crafting stairs
				WarpDrive.register(new ShapedOreRecipe(groupHulls,
				                                       new ItemStack(WarpDrive.blockHulls_stairs[index][metadataColor], 4), false, "p  ", "pp ", "ppp",
				                                       'p', new ItemStack(WarpDrive.blockHulls_plain[index][0], 1, metadataColor) ));
				
				// uncrafting stairs
				WarpDrive.register(new ShapelessOreRecipe(groupHulls,
				                                          new ItemStack(WarpDrive.blockHulls_plain[index][0], 6, metadataColor),
				                                          WarpDrive.blockHulls_stairs[index][metadataColor],
				                                          WarpDrive.blockHulls_stairs[index][metadataColor],
				                                          WarpDrive.blockHulls_stairs[index][metadataColor],
				                                          WarpDrive.blockHulls_stairs[index][metadataColor] ));
				
				// smelting tiled
				GameRegistry.addSmelting(
						new ItemStack(WarpDrive.blockHulls_plain[index][0], 1, metadataColor),
						new ItemStack(WarpDrive.blockHulls_plain[index][1], 1, metadataColor),
						0);
				
				// uncrafting tiled
				WarpDrive.register(new ShapelessOreRecipe(groupHulls,
				                                          new ItemStack(WarpDrive.blockHulls_plain[index][0], 1, metadataColor),
				        new ItemStack(WarpDrive.blockHulls_plain[index][1], 1, metadataColor)));
				
				// crafting omnipanel
				WarpDrive.register(new ShapedOreRecipe(groupHulls,
				                                       new ItemStack(WarpDrive.blockHulls_omnipanel[index], 16, metadataColor), false, "ggg", "ggg",
						'g', new ItemStack(WarpDrive.blockHulls_glass[index], 1, metadataColor)));
				
				// uncrafting omnipanel
				WarpDrive.register(new ShapelessOreRecipe(groupHulls,
				                                          new ItemStack(WarpDrive.blockHulls_glass[index], 3, metadataColor),
				                                          new ItemStack(WarpDrive.blockHulls_omnipanel[index], 1, metadataColor),
				                                          new ItemStack(WarpDrive.blockHulls_omnipanel[index], 1, metadataColor),
				                                          new ItemStack(WarpDrive.blockHulls_omnipanel[index], 1, metadataColor),
				                                          new ItemStack(WarpDrive.blockHulls_omnipanel[index], 1, metadataColor),
				                                          new ItemStack(WarpDrive.blockHulls_omnipanel[index], 1, metadataColor),
				                                          new ItemStack(WarpDrive.blockHulls_omnipanel[index], 1, metadataColor),
				                                          new ItemStack(WarpDrive.blockHulls_omnipanel[index], 1, metadataColor),
				                                          new ItemStack(WarpDrive.blockHulls_omnipanel[index], 1, metadataColor) ));
				
				// crafting slab
				WarpDrive.register(new ShapedOreRecipe(groupHulls,
				                                       new ItemStack(WarpDrive.blockHulls_slab[index][metadataColor], 6, 0), false, "bbb",
				                                       'b', new ItemStack(WarpDrive.blockHulls_plain[index][0], 1, metadataColor)));
				WarpDrive.register(new ShapedOreRecipe(groupHulls,
				                                       new ItemStack(WarpDrive.blockHulls_slab[index][metadataColor], 6, 2), false, "b", "b", "b",
				                                       'b', new ItemStack(WarpDrive.blockHulls_plain[index][0], 1, metadataColor)));
				WarpDrive.register(new ShapedOreRecipe(groupHulls,
				                                       new ItemStack(WarpDrive.blockHulls_slab[index][metadataColor], 6, 6), false, "bbb",
				                                       'b', new ItemStack(WarpDrive.blockHulls_plain[index][1], 1, metadataColor)));
				WarpDrive.register(new ShapedOreRecipe(groupHulls,
				                                       new ItemStack(WarpDrive.blockHulls_slab[index][metadataColor], 6, 8), false, "b", "b", "b",
				                                       'b', new ItemStack(WarpDrive.blockHulls_plain[index][1], 1, metadataColor)));
				
				// uncrafting slab
				WarpDrive.register(new ShapedOreRecipe(groupHulls,
				                                       new ItemStack(WarpDrive.blockHulls_plain[index][0], 1, metadataColor), false, "s", "s",
				                                       's', new ItemStack(WarpDrive.blockHulls_slab[index][metadataColor], 1, 0)), "_uncrafting");
				WarpDrive.register(new ShapedOreRecipe(groupHulls,
				                                       new ItemStack(WarpDrive.blockHulls_plain[index][0], 1, metadataColor), false, "ss",
				                                       's', new ItemStack(WarpDrive.blockHulls_slab[index][metadataColor], 1, 2)), "_uncrafting_A");
				WarpDrive.register(new ShapedOreRecipe(groupHulls,
				                                       new ItemStack(WarpDrive.blockHulls_plain[index][1], 1, metadataColor), false, "s", "s",
				                                       's', new ItemStack(WarpDrive.blockHulls_slab[index][metadataColor], 1, 6)), "_uncrafting_B");
				WarpDrive.register(new ShapedOreRecipe(groupHulls,
				                                       new ItemStack(WarpDrive.blockHulls_plain[index][1], 1, metadataColor), false, "ss",
				                                       's', new ItemStack(WarpDrive.blockHulls_slab[index][metadataColor], 1, 8)), "_uncrafting_C");
				WarpDrive.register(new ShapelessOreRecipe(groupHulls,
				                                          new ItemStack(WarpDrive.blockHulls_slab[index][metadataColor], 2, 0),
				                                          new ItemStack(WarpDrive.blockHulls_slab[index][metadataColor], 1, 12)), "_uncrafting");
				WarpDrive.register(new ShapelessOreRecipe(groupHulls,
				                                          new ItemStack(WarpDrive.blockHulls_slab[index][metadataColor], 2, 6),
				                                          new ItemStack(WarpDrive.blockHulls_slab[index][metadataColor], 1, 13)), "_uncrafting_A");
				WarpDrive.register(new ShapelessOreRecipe(groupHulls,
				                                          new ItemStack(WarpDrive.blockHulls_slab[index][metadataColor], 2, 8),
				                                          new ItemStack(WarpDrive.blockHulls_slab[index][metadataColor], 1, 14)), "_uncrafting_B");
				WarpDrive.register(new ShapelessOreRecipe(groupHulls,
				                                          new ItemStack(WarpDrive.blockHulls_slab[index][metadataColor], 2, 8),
				                                          new ItemStack(WarpDrive.blockHulls_slab[index][metadataColor], 1, 15)), "_uncrafting_C");
				
				// changing colors
				WarpDrive.register(new ShapelessOreRecipe(groupTaintedHulls,
				                                          new ItemStack(WarpDrive.blockHulls_plain[index][0], 1, metadataColor),
				                                          oreDyes.get(enumDyeColor),
				                                          "blockHull" + index + "_plain"), "_dye" );
				WarpDrive.register(new ShapelessOreRecipe(groupTaintedHulls,
				                                          new ItemStack(WarpDrive.blockHulls_glass[index], 1, metadataColor),
				                                          oreDyes.get(enumDyeColor),
				                                          "blockHull" + index + "_glass"), "_dye" );
				WarpDrive.register(new ShapelessOreRecipe(groupTaintedHulls,
				                                          new ItemStack(WarpDrive.blockHulls_stairs[index][metadataColor], 1),
				                                          oreDyes.get(enumDyeColor),
				                                          "blockHull" + index + "_stairs"), "_dye" );
				WarpDrive.register(new ShapedOreRecipe(groupTaintedHulls,
				                                       new ItemStack(WarpDrive.blockHulls_plain[index][0], 8, metadataColor), false, "###", "#X#", "###",
				                                       '#', "blockHull" + index + "_plain",
				                                       'X', oreDyes.get(enumDyeColor) ), "_dye");
				WarpDrive.register(new ShapedOreRecipe(groupTaintedHulls,
				                                       new ItemStack(WarpDrive.blockHulls_glass[index], 8, metadataColor), false, "###", "#X#", "###",
				                                       '#', "blockHull" + index + "_glass",
				                                       'X', oreDyes.get(enumDyeColor) ), "_dye");
				WarpDrive.register(new ShapedOreRecipe(groupTaintedHulls,
				                                       new ItemStack(WarpDrive.blockHulls_stairs[index][metadataColor], 8), false, "###", "#X#", "###",
				                                       '#', "blockHull" + index + "_stairs",
				                                       'X', oreDyes.get(enumDyeColor) ), "_dye");
			}
		}
	}
	
	private static void initMovement() {
		// Ship core
		// note:
		// - we want to recycle the previous tier
		// - Ship controller should be more expensive than the core, so it can't be used as ingredient
		// basic    (shuttle ) is 1 Diamond crystal, 3 Redstone dust     , 1 Power interface, 1 Memory crystal, 1 MV Machine casing, 1 Computer interface
		// advanced (corvette) is 1 Emerald crystal, 3 Capacitive crystal, 1 Power interface, 1 Memory crystal, 1 MV Machine casing, 1 basic Ship core
		// superior (frigate ) is 1 Nether star    , 3 Capacitive cluster, 1 Superconductor , 1 Memory cluster, 1 HV Machine casing, 1 advanced Ship core
		WarpDrive.register(new ShapedOreRecipe(groupMachines,
		                                       new ItemStack(WarpDrive.blockShipCores[EnumTier.BASIC.getIndex()]),"ce ", "pmc", "cCM",
		                                       'e', ItemComponent.getItemStack(EnumComponentType.DIAMOND_CRYSTAL),
		                                       'c', Items.REDSTONE,
		                                       'p', ItemComponent.getItemStack(EnumComponentType.POWER_INTERFACE),
		                                       'M', ItemComponent.getItemStack(EnumComponentType.MEMORY_CRYSTAL),
		                                       'm', itemStackMachineCasings[0],
		                                       'C', ItemComponent.getItemStack(EnumComponentType.COMPUTER_INTERFACE) ));
		WarpDrive.register(new ShapedOreRecipe(groupMachines,
		                                       new ItemStack(WarpDrive.blockShipCores[EnumTier.ADVANCED.getIndex()]),"ce ", "pmc", "cCM",
		                                       'e', ItemComponent.getItemStack(EnumComponentType.EMERALD_CRYSTAL),
		                                       'c', ItemComponent.getItemStack(EnumComponentType.CAPACITIVE_CRYSTAL),
		                                       'p', ItemComponent.getItemStack(EnumComponentType.POWER_INTERFACE),
		                                       'M', ItemComponent.getItemStack(EnumComponentType.MEMORY_CRYSTAL),
		                                       'm', itemStackMachineCasings[1],
		                                       'C', new ItemStack(WarpDrive.blockShipCores[EnumTier.BASIC.getIndex()]) ));
		WarpDrive.register(new ShapedOreRecipe(groupMachines,
		                                       new ItemStack(WarpDrive.blockShipCores[EnumTier.SUPERIOR.getIndex()]),"ce ", "pmc", "cCM",
		                                       'e', Items.NETHER_STAR,
		                                       'c', ItemComponent.getItemStack(EnumComponentType.CAPACITIVE_CLUSTER),
		                                       'p', ItemComponent.getItemStack(EnumComponentType.SUPERCONDUCTOR),
		                                       'M', ItemComponent.getItemStack(EnumComponentType.MEMORY_CLUSTER),
		                                       'm', itemStackMachineCasings[2],
		                                       'C', new ItemStack(WarpDrive.blockShipCores[EnumTier.ADVANCED.getIndex()]) ));
		
		// Remote ship controller
		// basic    is 1 Ender pearl, 3 Ender coil, 1 Diamond crystal, 1 LV Machine casing, 1 Memory crystal, 1 Ender coil, 1 Computer interface
		// advanced is 1 Ender pearl, 3 Ender coil, 1 Emerald crystal, 1 MV Machine casing, 1 Memory cluster, 2 Ender coil, 1 basic Ship controller
		// superior is 1 Ender pearl, 3 Ender coil, 1 Nether star    , 1 HV Machine casing, 1 Memory cluster, 4 Ender coil, 1 advanced Ship controller
		WarpDrive.register(new ShapedOreRecipe(groupMachines,
		                                       new ItemStack(WarpDrive.blockShipControllers[EnumTier.BASIC.getIndex()]), false, "ce ", "pmc", "cCM",
		                                       'e', ItemComponent.getItemStack(EnumComponentType.DIAMOND_CRYSTAL),
		                                       'c', ItemComponent.getItemStack(EnumComponentType.ENDER_COIL),
		                                       'p', Items.ENDER_PEARL,
		                                       'M', ItemComponent.getItemStack(EnumComponentType.MEMORY_CRYSTAL),
		                                       'm', itemStackMachineCasings[0],
		                                       'C', ItemComponent.getItemStack(EnumComponentType.COMPUTER_INTERFACE) ));
		WarpDrive.register(new ShapedOreRecipe(groupMachines,
		                                       new ItemStack(WarpDrive.blockShipControllers[EnumTier.ADVANCED.getIndex()]), false, "ce ", "pmc", "cCM",
		                                       'e', ItemComponent.getItemStack(EnumComponentType.EMERALD_CRYSTAL),
		                                       'c', ItemComponent.getItemStack(EnumComponentType.ENDER_COIL),
		                                       'p', Items.ENDER_PEARL,
		                                       'M', ItemComponent.getItemStack(EnumComponentType.MEMORY_CLUSTER),
		                                       'm', itemStackMachineCasings[1],
		                                       'C', new ItemStack(WarpDrive.blockShipControllers[EnumTier.BASIC.getIndex()]) ));
		WarpDrive.register(new ShapedOreRecipe(groupMachines,
		                                       new ItemStack(WarpDrive.blockShipControllers[EnumTier.SUPERIOR.getIndex()]), false, "ce ", "pmc", "cCM",
		                                       'e', Items.NETHER_STAR,
		                                       'c', ItemComponent.getItemStack(EnumComponentType.ENDER_COIL),
		                                       'p', Items.ENDER_PEARL,
		                                       'M', ItemComponent.getItemStack(EnumComponentType.MEMORY_CLUSTER),
		                                       'm', itemStackMachineCasings[2],
		                                       'C', new ItemStack(WarpDrive.blockShipControllers[EnumTier.ADVANCED.getIndex()]) ));
		
		// Laser lift is ...
		final Object enderPearlOrMagnetizer = WarpDriveConfig.getOreOrItemStack(
				"gregtech:machine", 420,         // Gregtech Basic polarizer
				"ic2:te", 37,                    // IC2 Experimental Magnetizer
				"ic2:blockmachinelv", 10,        // IC2 Classic Magnetizer
				"ore:ingotPulsatingIron", 0,     // EnderIO iron ingot with ender pearl
				"minecraft:ender_pearl", 0 );
		WarpDrive.register(new ShapedOreRecipe(groupMachines,
		                                       new ItemStack(WarpDrive.blockLift), false, "wlw", "per", "glg",
		                                       'r', Items.REDSTONE,
		                                       'w', Blocks.WOOL,
		                                       'l', ItemComponent.getItemStack(EnumComponentType.LENS),
		                                       'e', enderPearlOrMagnetizer,
		                                       'p', ItemComponent.getItemStack(EnumComponentType.POWER_INTERFACE),
		                                       'g', "paneGlassColorless"));
		
		// Transporter Beacon is 1 Ender pearl, 1 Memory crystal, 1 Diamond crystal, 2 Sticks
		WarpDrive.register(new ShapedOreRecipe(groupMachines,
		                                       new ItemStack(WarpDrive.blockTransporterBeacon), false, " e ", " m ", "sds",
		                                       'e', Items.ENDER_PEARL,
		                                       'd', ItemComponent.getItemStack(EnumComponentType.DIAMOND_CRYSTAL),
		                                       'm', ItemComponent.getItemStack(EnumComponentType.MEMORY_CRYSTAL),
		                                       's', Items.STICK));
		
		// Transporter containment is 1 HV Machine casing, 2 Ender crystal, gives 2
		if (!WarpDriveConfig.ACCELERATOR_ENABLE) {
			WarpDrive.register(new ShapedOreRecipe(groupMachines,
			                                       new ItemStack(WarpDrive.blockTransporterContainment, 2), false, " e ", " m ", " e ",
			                                       'm', itemStackMachineCasings[2],
			                                       'e', ItemComponent.getItemStack(EnumComponentType.ENDER_COIL)));
		} else {
			WarpDrive.register(new ShapedOreRecipe(groupMachines,
			                                       new ItemStack(WarpDrive.blockTransporterContainment, 2), false, " e ", " m ", " e ",
			                                       'm', "blockElectromagnet2",
			                                       'e', ItemComponent.getItemStack(EnumComponentType.ENDER_COIL)));
		}
		
		// Transporter core is 1 HV Machine casing, 1 Emerald crystal, 1 Capacitive crystal, 1 Diamond crystal, 1 Power interface, 1 Computer interface
		WarpDrive.register(new ShapedOreRecipe(groupMachines,
		                                       new ItemStack(WarpDrive.blockTransporterCore), false, " E ", "pmd", " c ",
		                                       'm', itemStackMachineCasings[2],
		                                       'c', ItemComponent.getItemStack(EnumComponentType.COMPUTER_INTERFACE),
		                                       'd', ItemComponent.getItemStack(EnumComponentType.DIAMOND_CRYSTAL),
		                                       'E', ItemComponent.getItemStack(EnumComponentType.EMERALD_CRYSTAL),
		                                       'p', ItemComponent.getItemStack(EnumComponentType.POWER_INTERFACE)));
		
		// Transporter scanner is 1 HV Machine casing, 1 Emerald crystal, 3 Capacitive crystal, 2 Ender crystal
		if (!WarpDriveConfig.ACCELERATOR_ENABLE) {
			WarpDrive.register(new ShapedOreRecipe(groupMachines,
			                                       new ItemStack(WarpDrive.blockTransporterScanner), false, " E ", "eme", "CCC",
			                                       'm', itemStackMachineCasings[2],
			                                       'e', ItemComponent.getItemStack(EnumComponentType.ENDER_COIL),
			                                       'E', ItemComponent.getItemStack(EnumComponentType.EMERALD_CRYSTAL),
			                                       'C', ItemComponent.getItemStack(EnumComponentType.CAPACITIVE_CRYSTAL)));
		} else {
			WarpDrive.register(new ShapedOreRecipe(groupMachines,
			                                       new ItemStack(WarpDrive.blockTransporterScanner), false, " E ", "eme", "CCC",
			                                       'm', "blockElectromagnet2",
			                                       'e', ItemComponent.getItemStack(EnumComponentType.ENDER_COIL),
			                                       'E', ItemComponent.getItemStack(EnumComponentType.EMERALD_CRYSTAL),
			                                       'C', ItemComponent.getItemStack(EnumComponentType.CAPACITIVE_CRYSTAL)));
		}
	}
	
	private static void initWeapon() {
		// Laser cannon is 2 Motors, 1 Diffraction grating, 1 lens, 1 Computer interface, 1 HV Machine casing, 1 Redstone dust, 2 Glass pane
		WarpDrive.register(new ShapedOreRecipe(groupMachines,
		                                       new ItemStack(WarpDrive.blockLaser), false, "gtr", "ldm", "gt ",
		                                       't', itemStackMotors[2],
		                                       'd', ItemComponent.getItemStack(EnumComponentType.DIFFRACTION_GRATING),
		                                       'l', ItemComponent.getItemStack(EnumComponentType.LENS),
		                                       'm', itemStackMachineCasings[1],
		                                       'r', Items.REDSTONE,
		                                       'g', "paneGlassColorless"));
		
		// Laser camera is just Laser + Camera
		WarpDrive.register(new ShapedOreRecipe(groupMachines,
		                                       new ItemStack(WarpDrive.blockLaserCamera), false, "rlr", "rsr", "rcr",
		                                       'r', rubber,
		                                       's', goldNuggetOrBasicCircuit,
		                                       'l', WarpDrive.blockLaser,
		                                       'c', WarpDrive.blockCamera ));
		
		// Weapon controller is diamond sword with Ship controller
		WarpDrive.register(new ShapedOreRecipe(groupMachines,
		                                       new ItemStack(WarpDrive.blockWeaponController), false, "rwr", "msm", "rcr",
		                                       'r', rubber,
		                                       's', ItemComponent.getItemStack(EnumComponentType.EMERALD_CRYSTAL),
		                                       'm', ItemComponent.getItemStack(EnumComponentType.MEMORY_CRYSTAL),
		                                       'w', Items.DIAMOND_SWORD,
		                                       'c', WarpDrive.blockShipControllers[EnumTier.ADVANCED.getIndex()] ));
	}
	
	/*
	public static Ingredient getIngredient(final Object object) {
		if (object instanceof ItemStack) {
			return Ingredient.fromStacks((ItemStack) object);
		}
		if (object instanceof Item) {
			return Ingredient.fromItem((Item) object);
		}
		if (object instanceof String) {
			return new OreIngredient((String) object);
		}
		final ItemStack itemStack = ItemStack.EMPTY;
		if (object != null) {
			itemStack.setStackDisplayName(object.toString());
		}
		return Ingredient.fromStacks(itemStack);
	}
	/**/
	
	private static void removeRecipe(final ItemStack itemStackOutputOfRecipeToRemove) {
		ResourceLocation recipeToRemove = null;
		for (final Entry<ResourceLocation, IRecipe> entryRecipe : ForgeRegistries.RECIPES.getEntries()) {
			final IRecipe recipe = entryRecipe.getValue();
			final ItemStack itemStackRecipeOutput = recipe.getRecipeOutput();
			if ( !itemStackRecipeOutput.isEmpty()
			  && itemStackRecipeOutput.isItemEqual(itemStackOutputOfRecipeToRemove) ) {
				recipeToRemove = entryRecipe.getKey();
				break;
			}
		}
		if (recipeToRemove == null) {
			WarpDrive.logger.error(String.format("Unable to find any recipe to remove with output %s", itemStackOutputOfRecipeToRemove));
		} else {
			WarpDrive.logger.info(String.format("Removing recipe %s with output %s", recipeToRemove, itemStackOutputOfRecipeToRemove));
			((ForgeRegistry<IRecipe>) ForgeRegistries.RECIPES).remove(recipeToRemove);
		}
	}
}
