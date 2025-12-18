package cr0s.warpdrive.config;

import cr0s.warpdrive.WarpDrive;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map.Entry;

import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;

import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.Configuration;

import com.google.common.base.Optional;

// note: this is a crude workaround until Forge data fixer gets a proper documentation
public class WarpDriveDataFixer {
	
	// Raw string entries (loaded from configuration file at PreInit, parsed at PostInit)
	private static HashMap<String, String> rawBlocks = null;
	// private static HashMap<String, String> rawEntities = null;
	// private static HashMap<String, String> rawItems = null;
	
	// Blocks data fixer
	public static HashMap<String, Block> BLOCKS = null;
	public static HashMap<String, IBlockState> BLOCKSTATES = null;
	
	// Entities data fixer
	// private static HashSet<ResourceLocation> ENTITIES = null;
	
	// Items data fixer
	// public static HashMap<String, Item> ITEMS = null;
	// public static HashMap<String, ItemStack> ITEMSTACKS = null;
	
	public static void loadConfig(final Configuration config) {
		
		// Block data fixer
		{
			config.addCustomCategoryComment("blocks", "Use this section to convert registry name for blocks.");
			
			final ConfigCategory categoryBlocks = config.getCategory("blocks");
			
			// *** enforce default values
			// removed blocks
			config.get("blocks", "minecraft:air"                                , "WarpDrive:blockAir WarpDrive:blockShipController WarpDrive:airBlock WarpDrive:protocolBlock").getString();
			// WarpDrive changes over time
			config.get("blocks", "warpdrive:accelerator_control_point"          , "WarpDrive:blockAcceleratorControlPoint").getString();
			config.get("blocks", "warpdrive:accelerator_core"                   , "WarpDrive:blockAcceleratorController").getString();
			config.get("blocks", "warpdrive:air_flow"                           , "WarpDrive:blockAirFlow").getString();
			config.get("blocks", "warpdrive:air_generator.advanced"             , "WarpDrive:blockAirGenerator1 WarpDrive:blockAirGenerator WarpDrive:airgenBlock").getString();
			config.get("blocks", "warpdrive:air_generator.basic"                , "WarpDrive:blockAirGenerator2").getString();
			config.get("blocks", "warpdrive:air_generator.superior"             , "WarpDrive:blockAirGenerator3").getString();
			config.get("blocks", "warpdrive:air_shield"                         , "WarpDrive:blockAirShield").getString();
			config.get("blocks", "warpdrive:air_source"                         , "WarpDrive:blockAirSource").getString();
			config.get("blocks", "warpdrive:bedrock_glass"                      , "WarpDrive:blockBedrockGlass").getString();
			config.get("blocks", "warpdrive:camera"                             , "WarpDrive:blockCamera WarpDrive:cameraBlock").getString();
			config.get("blocks", "warpdrive:capacitor.advanced"                 , "WarpDrive:blockEnergyBank@2").getString();
			config.get("blocks", "warpdrive:capacitor.basic"                    , "WarpDrive:blockEnergyBank@1 WarpDrive:powerStore").getString();
			config.get("blocks", "warpdrive:capacitor.creative"                 , "WarpDrive:blockEnergyBank@0").getString();
			config.get("blocks", "warpdrive:capacitor.superior"                 , "WarpDrive:blockEnergyBank@3").getString();
			config.get("blocks", "warpdrive:chiller.advanced"                   , "WarpDrive:blockChiller2").getString();
			config.get("blocks", "warpdrive:chiller.basic"                      , "WarpDrive:blockChiller1").getString();
			config.get("blocks", "warpdrive:chiller.superior"                   , "WarpDrive:blockChiller3").getString();
			config.get("blocks", "warpdrive:chunk_loader.advanced"              , "").getString();
			config.get("blocks", "warpdrive:chunk_loader.basic"                 , "WarpDrive:blockChunkLoader WarpDrive:chunkLoader").getString();
			config.get("blocks", "warpdrive:chunk_loader.superior"              , "").getString();
			config.get("blocks", "warpdrive:cloaking_coil"                      , "WarpDrive:blockCloakingCoil WarpDrive:cloakCoilBlock").getString();
			config.get("blocks", "warpdrive:cloaking_core"                      , "WarpDrive:blockCloakingCore WarpDrive:cloakBlock").getString();
			config.get("blocks", "warpdrive:decorative"                         , "WarpDrive:blockDecorative WarpDrive:decorative").getString();
			config.get("blocks", "warpdrive:electromagnet.advanced.glass"       , "WarpDrive:blockElectromagnetGlass2").getString();
			config.get("blocks", "warpdrive:electromagnet.advanced.plain"       , "WarpDrive:blockElectromagnetPlain2").getString();
			config.get("blocks", "warpdrive:electromagnet.basic.glass"          , "WarpDrive:blockElectromagnetGlass1").getString();
			config.get("blocks", "warpdrive:electromagnet.basic.plain"          , "WarpDrive:blockElectromagnetPlain1").getString();
			config.get("blocks", "warpdrive:electromagnet.superior.glass"       , "WarpDrive:blockElectromagnetGlass3").getString();
			config.get("blocks", "warpdrive:electromagnet.superior.plain"       , "WarpDrive:blockElectromagnetPlain3").getString();
			config.get("blocks", "warpdrive:enan_reactor_core.advanced"         , "").getString();
			config.get("blocks", "warpdrive:enan_reactor_core.basic"            , "WarpDrive:blockEnanReactorCore WarpDrive:powerReactor").getString();
			config.get("blocks", "warpdrive:enan_reactor_core.superior"         , "").getString();
			config.get("blocks", "warpdrive:enan_reactor_laser"                 , "WarpDrive:blockEnanReactorLaser WarpDrive:powerLaser").getString();
			config.get("blocks", "warpdrive:force_field_relay.advanced"         , "WarpDrive:blockForceFieldRelay2").getString();
			config.get("blocks", "warpdrive:force_field_relay.basic"            , "WarpDrive:blockForceFieldRelay1").getString();
			config.get("blocks", "warpdrive:force_field_relay.superior"         , "WarpDrive:blockForceFieldRelay3").getString();
			config.get("blocks", "warpdrive:force_field.advanced"               , "WarpDrive:blockForceField2").getString();
			config.get("blocks", "warpdrive:force_field.basic"                  , "WarpDrive:blockForceField1").getString();
			config.get("blocks", "warpdrive:force_field.superior"               , "WarpDrive:blockForceField3").getString();
			config.get("blocks", "warpdrive:gas"                                , "WarpDrive:blockGas WarpDrive:gasBlock").getString();
			config.get("blocks", "warpdrive:highly_advanced_machine"            , "WarpDrive:blockHighlyAdvancedMachine WarpDrive:blockHAMachine").getString();
			config.get("blocks", "warpdrive:hull.advanced.glass"                , "WarpDrive:blockHull2_glass").getString();
			config.get("blocks", "warpdrive:hull.advanced.omnipanel"            , "WarpDrive:blockHull2_omnipanel").getString();
			config.get("blocks", "warpdrive:hull.advanced.plain"                , "WarpDrive:blockHull2_plain").getString();
			config.get("blocks", "warpdrive:hull.advanced.slab_black"           , "WarpDrive:blockHull2_slab_black").getString();
			config.get("blocks", "warpdrive:hull.advanced.slab_blue"            , "WarpDrive:blockHull2_slab_blue").getString();
			config.get("blocks", "warpdrive:hull.advanced.slab_brown"           , "WarpDrive:blockHull2_slab_brown").getString();
			config.get("blocks", "warpdrive:hull.advanced.slab_cyan"            , "WarpDrive:blockHull2_slab_cyan").getString();
			config.get("blocks", "warpdrive:hull.advanced.slab_gray"            , "WarpDrive:blockHull2_slab_gray").getString();
			config.get("blocks", "warpdrive:hull.advanced.slab_green"           , "WarpDrive:blockHull2_slab_green").getString();
			config.get("blocks", "warpdrive:hull.advanced.slab_light_blue"      , "WarpDrive:blockHull2_slab_lightBlue").getString();
			config.get("blocks", "warpdrive:hull.advanced.slab_lime"            , "WarpDrive:blockHull2_slab_lime").getString();
			config.get("blocks", "warpdrive:hull.advanced.slab_magenta"         , "WarpDrive:blockHull2_slab_magenta").getString();
			config.get("blocks", "warpdrive:hull.advanced.slab_orange"          , "WarpDrive:blockHull2_slab_orange").getString();
			config.get("blocks", "warpdrive:hull.advanced.slab_pink"            , "WarpDrive:blockHull2_slab_pink").getString();
			config.get("blocks", "warpdrive:hull.advanced.slab_purple"          , "WarpDrive:blockHull2_slab_purple").getString();
			config.get("blocks", "warpdrive:hull.advanced.slab_red"             , "WarpDrive:blockHull2_slab_red").getString();
			config.get("blocks", "warpdrive:hull.advanced.slab_silver"          , "WarpDrive:blockHull2_slab_silver").getString();
			config.get("blocks", "warpdrive:hull.advanced.slab_white"           , "WarpDrive:blockHull2_slab_white").getString();
			config.get("blocks", "warpdrive:hull.advanced.slab_yellow"          , "WarpDrive:blockHull2_slab_yellow").getString();
			config.get("blocks", "warpdrive:hull.advanced.stairs_black"         , "WarpDrive:blockHull2_stairs_black").getString();
			config.get("blocks", "warpdrive:hull.advanced.stairs_blue"          , "WarpDrive:blockHull2_stairs_blue").getString();
			config.get("blocks", "warpdrive:hull.advanced.stairs_brown"         , "WarpDrive:blockHull2_stairs_brown").getString();
			config.get("blocks", "warpdrive:hull.advanced.stairs_cyan"          , "WarpDrive:blockHull2_stairs_cyan").getString();
			config.get("blocks", "warpdrive:hull.advanced.stairs_gray"          , "WarpDrive:blockHull2_stairs_gray").getString();
			config.get("blocks", "warpdrive:hull.advanced.stairs_green"         , "WarpDrive:blockHull2_stairs_green").getString();
			config.get("blocks", "warpdrive:hull.advanced.stairs_light_blue"    , "WarpDrive:blockHull2_stairs_lightBlue").getString();
			config.get("blocks", "warpdrive:hull.advanced.stairs_lime"          , "WarpDrive:blockHull2_stairs_lime").getString();
			config.get("blocks", "warpdrive:hull.advanced.stairs_magenta"       , "WarpDrive:blockHull2_stairs_magenta").getString();
			config.get("blocks", "warpdrive:hull.advanced.stairs_orange"        , "WarpDrive:blockHull2_stairs_orange").getString();
			config.get("blocks", "warpdrive:hull.advanced.stairs_pink"          , "WarpDrive:blockHull2_stairs_pink").getString();
			config.get("blocks", "warpdrive:hull.advanced.stairs_purple"        , "WarpDrive:blockHull2_stairs_purple").getString();
			config.get("blocks", "warpdrive:hull.advanced.stairs_red"           , "WarpDrive:blockHull2_stairs_red").getString();
			config.get("blocks", "warpdrive:hull.advanced.stairs_silver"        , "WarpDrive:blockHull2_stairs_silver").getString();
			config.get("blocks", "warpdrive:hull.advanced.stairs_white"         , "WarpDrive:blockHull2_stairs_white").getString();
			config.get("blocks", "warpdrive:hull.advanced.stairs_yellow"        , "WarpDrive:blockHull2_stairs_yellow").getString();
			config.get("blocks", "warpdrive:hull.advanced.tiled"                , "WarpDrive:blockHull2_tiled").getString();
			config.get("blocks", "warpdrive:hull.basic.glass"                   , "WarpDrive:blockHull1_glass").getString();
			config.get("blocks", "warpdrive:hull.basic.omnipanel"               , "WarpDrive:blockHull1_omnipanel").getString();
			config.get("blocks", "warpdrive:hull.basic.plain"                   , "WarpDrive:blockHull1_plain").getString();
			config.get("blocks", "warpdrive:hull.basic.slab_black"              , "WarpDrive:blockHull1_slab_black").getString();
			config.get("blocks", "warpdrive:hull.basic.slab_blue"               , "WarpDrive:blockHull1_slab_blue").getString();
			config.get("blocks", "warpdrive:hull.basic.slab_brown"              , "WarpDrive:blockHull1_slab_brown").getString();
			config.get("blocks", "warpdrive:hull.basic.slab_cyan"               , "WarpDrive:blockHull1_slab_cyan").getString();
			config.get("blocks", "warpdrive:hull.basic.slab_gray"               , "WarpDrive:blockHull1_slab_gray").getString();
			config.get("blocks", "warpdrive:hull.basic.slab_green"              , "WarpDrive:blockHull1_slab_green").getString();
			config.get("blocks", "warpdrive:hull.basic.slab_light_blue"         , "WarpDrive:blockHull1_slab_lightBlue").getString();
			config.get("blocks", "warpdrive:hull.basic.slab_lime"               , "WarpDrive:blockHull1_slab_lime").getString();
			config.get("blocks", "warpdrive:hull.basic.slab_magenta"            , "WarpDrive:blockHull1_slab_magenta").getString();
			config.get("blocks", "warpdrive:hull.basic.slab_orange"             , "WarpDrive:blockHull1_slab_orange").getString();
			config.get("blocks", "warpdrive:hull.basic.slab_pink"               , "WarpDrive:blockHull1_slab_pink").getString();
			config.get("blocks", "warpdrive:hull.basic.slab_purple"             , "WarpDrive:blockHull1_slab_purple").getString();
			config.get("blocks", "warpdrive:hull.basic.slab_red"                , "WarpDrive:blockHull1_slab_red").getString();
			config.get("blocks", "warpdrive:hull.basic.slab_silver"             , "WarpDrive:blockHull1_slab_silver").getString();
			config.get("blocks", "warpdrive:hull.basic.slab_white"              , "WarpDrive:blockHull1_slab_white").getString();
			config.get("blocks", "warpdrive:hull.basic.slab_yellow"             , "WarpDrive:blockHull1_slab_yellow").getString();
			config.get("blocks", "warpdrive:hull.basic.stairs_black"            , "WarpDrive:blockHull1_stairs_black").getString();
			config.get("blocks", "warpdrive:hull.basic.stairs_blue"             , "WarpDrive:blockHull1_stairs_blue").getString();
			config.get("blocks", "warpdrive:hull.basic.stairs_brown"            , "WarpDrive:blockHull1_stairs_brown").getString();
			config.get("blocks", "warpdrive:hull.basic.stairs_cyan"             , "WarpDrive:blockHull1_stairs_cyan").getString();
			config.get("blocks", "warpdrive:hull.basic.stairs_gray"             , "WarpDrive:blockHull1_stairs_gray").getString();
			config.get("blocks", "warpdrive:hull.basic.stairs_green"            , "WarpDrive:blockHull1_stairs_green").getString();
			config.get("blocks", "warpdrive:hull.basic.stairs_light_blue"       , "WarpDrive:blockHull1_stairs_lightBlue").getString();
			config.get("blocks", "warpdrive:hull.basic.stairs_lime"             , "WarpDrive:blockHull1_stairs_lime").getString();
			config.get("blocks", "warpdrive:hull.basic.stairs_magenta"          , "WarpDrive:blockHull1_stairs_magenta").getString();
			config.get("blocks", "warpdrive:hull.basic.stairs_orange"           , "WarpDrive:blockHull1_stairs_orange").getString();
			config.get("blocks", "warpdrive:hull.basic.stairs_pink"             , "WarpDrive:blockHull1_stairs_pink").getString();
			config.get("blocks", "warpdrive:hull.basic.stairs_purple"           , "WarpDrive:blockHull1_stairs_purple").getString();
			config.get("blocks", "warpdrive:hull.basic.stairs_red"              , "WarpDrive:blockHull1_stairs_red").getString();
			config.get("blocks", "warpdrive:hull.basic.stairs_silver"           , "WarpDrive:blockHull1_stairs_silver").getString();
			config.get("blocks", "warpdrive:hull.basic.stairs_white"            , "WarpDrive:blockHull1_stairs_white").getString();
			config.get("blocks", "warpdrive:hull.basic.stairs_yellow"           , "WarpDrive:blockHull1_stairs_yellow").getString();
			config.get("blocks", "warpdrive:hull.basic.tiled"                   , "WarpDrive:blockHull1_tiled").getString();
			config.get("blocks", "warpdrive:hull.superior.glass"                , "WarpDrive:blockHull3_glass").getString();
			config.get("blocks", "warpdrive:hull.superior.omnipanel"            , "WarpDrive:blockHull3_omnipanel").getString();
			config.get("blocks", "warpdrive:hull.superior.plain"                , "WarpDrive:blockHull3_plain").getString();
			config.get("blocks", "warpdrive:hull.superior.slab_black"           , "WarpDrive:blockHull3_slab_black").getString();
			config.get("blocks", "warpdrive:hull.superior.slab_blue"            , "WarpDrive:blockHull3_slab_blue").getString();
			config.get("blocks", "warpdrive:hull.superior.slab_brown"           , "WarpDrive:blockHull3_slab_brown").getString();
			config.get("blocks", "warpdrive:hull.superior.slab_cyan"            , "WarpDrive:blockHull3_slab_cyan").getString();
			config.get("blocks", "warpdrive:hull.superior.slab_gray"            , "WarpDrive:blockHull3_slab_gray").getString();
			config.get("blocks", "warpdrive:hull.superior.slab_green"           , "WarpDrive:blockHull3_slab_green").getString();
			config.get("blocks", "warpdrive:hull.superior.slab_light_blue"      , "WarpDrive:blockHull3_slab_lightBlue").getString();
			config.get("blocks", "warpdrive:hull.superior.slab_lime"            , "WarpDrive:blockHull3_slab_lime").getString();
			config.get("blocks", "warpdrive:hull.superior.slab_magenta"         , "WarpDrive:blockHull3_slab_magenta").getString();
			config.get("blocks", "warpdrive:hull.superior.slab_orange"          , "WarpDrive:blockHull3_slab_orange").getString();
			config.get("blocks", "warpdrive:hull.superior.slab_pink"            , "WarpDrive:blockHull3_slab_pink").getString();
			config.get("blocks", "warpdrive:hull.superior.slab_purple"          , "WarpDrive:blockHull3_slab_purple").getString();
			config.get("blocks", "warpdrive:hull.superior.slab_red"             , "WarpDrive:blockHull3_slab_red").getString();
			config.get("blocks", "warpdrive:hull.superior.slab_silver"          , "WarpDrive:blockHull3_slab_silver").getString();
			config.get("blocks", "warpdrive:hull.superior.slab_white"           , "WarpDrive:blockHull3_slab_white").getString();
			config.get("blocks", "warpdrive:hull.superior.slab_yellow"          , "WarpDrive:blockHull3_slab_yellow").getString();
			config.get("blocks", "warpdrive:hull.superior.stairs_black"         , "WarpDrive:blockHull3_stairs_black").getString();
			config.get("blocks", "warpdrive:hull.superior.stairs_blue"          , "WarpDrive:blockHull3_stairs_blue").getString();
			config.get("blocks", "warpdrive:hull.superior.stairs_brown"         , "WarpDrive:blockHull3_stairs_brown").getString();
			config.get("blocks", "warpdrive:hull.superior.stairs_cyan"          , "WarpDrive:blockHull3_stairs_cyan").getString();
			config.get("blocks", "warpdrive:hull.superior.stairs_gray"          , "WarpDrive:blockHull3_stairs_gray").getString();
			config.get("blocks", "warpdrive:hull.superior.stairs_green"         , "WarpDrive:blockHull3_stairs_green").getString();
			config.get("blocks", "warpdrive:hull.superior.stairs_light_blue"    , "WarpDrive:blockHull3_stairs_lightBlue").getString();
			config.get("blocks", "warpdrive:hull.superior.stairs_lime"          , "WarpDrive:blockHull3_stairs_lime").getString();
			config.get("blocks", "warpdrive:hull.superior.stairs_magenta"       , "WarpDrive:blockHull3_stairs_magenta").getString();
			config.get("blocks", "warpdrive:hull.superior.stairs_orange"        , "WarpDrive:blockHull3_stairs_orange").getString();
			config.get("blocks", "warpdrive:hull.superior.stairs_pink"          , "WarpDrive:blockHull3_stairs_pink").getString();
			config.get("blocks", "warpdrive:hull.superior.stairs_purple"        , "WarpDrive:blockHull3_stairs_purple").getString();
			config.get("blocks", "warpdrive:hull.superior.stairs_red"           , "WarpDrive:blockHull3_stairs_red").getString();
			config.get("blocks", "warpdrive:hull.superior.stairs_silver"        , "WarpDrive:blockHull3_stairs_silver").getString();
			config.get("blocks", "warpdrive:hull.superior.stairs_white"         , "WarpDrive:blockHull3_stairs_white").getString();
			config.get("blocks", "warpdrive:hull.superior.stairs_yellow"        , "WarpDrive:blockHull3_stairs_yellow").getString();
			config.get("blocks", "warpdrive:hull.superior.tiled"                , "WarpDrive:blockHull3_tiled").getString();
			config.get("blocks", "warpdrive:ic2_reactor_laser_cooler"           , "WarpDrive:blockIC2reactorLaserMonitor WarpDrive:reactorMonitor").getString();
			config.get("blocks", "warpdrive:iridium_block"                      , "WarpDrive:blockIridium WarpDrive:iridiumBlock").getString();
			config.get("blocks", "warpdrive:lamp_bubble"                        , "").getString();
			config.get("blocks", "warpdrive:lamp_flat"                          , "").getString();
			config.get("blocks", "warpdrive:lamp_long"                          , "").getString();
			config.get("blocks", "warpdrive:laser"                              , "WarpDrive:blockLaser WarpDrive:laserBlock").getString();
			config.get("blocks", "warpdrive:laser_camera"                       , "WarpDrive:blockLaserCamera WarpDrive:laserCamBlock").getString();
			config.get("blocks", "warpdrive:laser_medium.advanced"              , "").getString();
			config.get("blocks", "warpdrive:laser_medium.basic"                 , "").getString();
			config.get("blocks", "warpdrive:laser_medium.superior"              , "WarpDrive:blockLaserMedium WarpDrive:boosterBlock").getString();
			config.get("blocks", "warpdrive:laser_tree_farm"                    , "WarpDrive:blockLaserTreeFarm WarpDrive:laserTreeFarmBlock").getString();
			config.get("blocks", "warpdrive:lift"                               , "WarpDrive:blockLift WarpDrive:liftBlock").getString();
			config.get("blocks", "warpdrive:mining_laser"                       , "WarpDrive:blockMiningLaser WarpDrive:miningLaserBlock").getString();
			config.get("blocks", "warpdrive:monitor"                            , "WarpDrive:blockMonitor WarpDrive:monitorBlock").getString();
			config.get("blocks", "warpdrive:particles_collider"                 , "WarpDrive:blockParticlesCollider").getString();
			config.get("blocks", "warpdrive:particles_injector"                 , "WarpDrive:blockParticlesInjector").getString();
			config.get("blocks", "warpdrive:projector.advanced"                 , "").getString();
			config.get("blocks", "warpdrive:projector.basic"                    , "").getString();
			config.get("blocks", "warpdrive:projector.superior"                 , "").getString();
			config.get("blocks", "warpdrive:radar"                              , "WarpDrive:blockRadar WarpDrive:radarBlock").getString();
			config.get("blocks", "warpdrive:security_station"                   , "").getString();
			config.get("blocks", "warpdrive:ship_controller.advanced"           , "WarpDrive:blockProjector2").getString();
			config.get("blocks", "warpdrive:ship_controller.basic"              , "WarpDrive:blockProjector1").getString();
			config.get("blocks", "warpdrive:ship_controller.superior"           , "WarpDrive:blockProjector3").getString();
			config.get("blocks", "warpdrive:ship_core.advanced"                 , "").getString();
			config.get("blocks", "warpdrive:ship_core.basic"                    , "WarpDrive:blockShipCore WarpDrive:warpCore").getString();
			config.get("blocks", "warpdrive:ship_core.superior"                 , "").getString();
			config.get("blocks", "warpdrive:ship_scanner.advanced"              , "").getString();
			config.get("blocks", "warpdrive:ship_scanner.basic"                 , "WarpDrive:blockShipScanner WarpDrive:scannerBlock").getString();
			config.get("blocks", "warpdrive:ship_scanner.superior"              , "").getString();
			config.get("blocks", "warpdrive:siren_industrial.advanced"          , "").getString();
			config.get("blocks", "warpdrive:siren_industrial.basic"             , "WarpDrive:siren@0").getString();
			config.get("blocks", "warpdrive:siren_industrial.superior"          , "").getString();
			config.get("blocks", "warpdrive:siren_military.advanced"            , "WarpDrive:siren@5").getString();
			config.get("blocks", "warpdrive:siren_military.basic"               , "WarpDrive:siren@4").getString();
			config.get("blocks", "warpdrive:siren_military.superior"            , "WarpDrive:siren@6").getString();
			config.get("blocks", "warpdrive:transporter_beacon"                 , "WarpDrive:blockTransporterBeacon WarpDrive:blockTransportBeacon WarpDrive:transportBeacon").getString();
			config.get("blocks", "warpdrive:transporter_containment"            , "WarpDrive:blockTransporterContainment").getString();
			config.get("blocks", "warpdrive:transporter_core"                   , "WarpDrive:blockTransporterCore WarpDrive:blockTransporter WarpDrive:transporter").getString();
			config.get("blocks", "warpdrive:transporter_scanner"                , "WarpDrive:blockTransporterScanner").getString();
			config.get("blocks", "warpdrive:void_shell.glass"                   , "WarpDrive:blockVoidShellGlass").getString();
			config.get("blocks", "warpdrive:void_shell.plain"                   , "WarpDrive:blockVoidShellPlain").getString();
			config.get("blocks", "warpdrive:warp_isolation"                     , "WarpDrive:blockWarpIsolation WarpDrive:isolationBlock").getString();
			config.get("blocks", "warpdrive:weapon_controller"                  , "WarpDrive:blockWeaponController").getString();
			
			// *** read actual values
			final String[] nameBlocks = categoryBlocks.getValues().keySet().toArray(new String[0]);
			rawBlocks = new HashMap<>(nameBlocks.length);
			for (final String name : nameBlocks) {
				final String tags = config.get("blocks", name, "").getString();
				rawBlocks.put(name, tags);
			}
		}
		
		/*
		// Entity data fixer
		{
			config.addCustomCategoryComment("entities", "Use this section to convert registry name for entities.");
			
			final ConfigCategory categoryEntities = config.getCategory("entities");
			// *** enforce default values
			// weapon target
			config.get("entities", "somemod:someentity"                   , "somemod:someentity").getString();
			
			// *** read actual values
			final String[] nameEntities = categoryEntities.getValues().keySet().toArray(new String[0]);
			rawEntities = new HashMap<>(nameEntities.length);
			for (final String name : nameEntities) {
				final String tags = config.get("entities", name, "").getString();
				rawEntities.put(name, tags);
			}
		}
		
		// Item data fixer
		{
			config.addCustomCategoryComment("items", "Use this section to convert registry name for items.");
			
			final ConfigCategory categoryItems = config.getCategory("items");
			// *** enforce default values
			config.get("items", "somemod:someitem"    , "somemod:someitem").getString();
			
			// *** read actual values
			final String[] nameItems = categoryItems.getValues().keySet().toArray(new String[0]);
			rawItems = new HashMap<>(nameItems.length);
			for (final String name : nameItems) {
				final String tags = config.get("items", name, "").getString();
				rawItems.put(name, tags);
			}
		}
		
		// TileEntity data fixer
		{
			config.addCustomCategoryComment("tile_entities", "Use this section to convert registry name for Tile entities.");
			
			final ConfigCategory categoryEntities = config.getCategory("entities");
			// *** enforce default values
			// weapon target
			config.get("entities", "somemod:sometileentity"                   , "somemod:sometileentity").getString();
			
			// *** read actual values
			final String[] nameEntities = categoryEntities.getValues().keySet().toArray(new String[0]);
			rawEntities = new HashMap<>(nameEntities.length);
			for (final String name : nameEntities) {
				final String tags = config.get("entities", name, "").getString();
				rawEntities.put(name, tags);
			}
		}
		/**/
	}
	
	public static void apply() {
		WarpDrive.logger.info("Evaluating data fixer entries");
		
		// apply blocks
		BLOCKS = new HashMap<>(rawBlocks.size());
		BLOCKSTATES = new HashMap<>(rawBlocks.size());
		for (final Entry<String, String> taggedBlock : rawBlocks.entrySet()) {
			final String nameFull = taggedBlock.getKey();
			
			final Object object = getBlockOrBlockState(nameFull);
			if (object == null) {
				continue;
			}
			if (object instanceof Block) {
				for (final String nameLegacy : taggedBlock.getValue().replace("\t", " ").replace(",", " ").replace("  ", " ").split(" ")) {
					BLOCKS.put(nameLegacy, (Block) object);
				}
			} else if (object instanceof IBlockState) {
				for (final String nameLegacy : taggedBlock.getValue().replace("\t", " ").replace(",", " ").replace("  ", " ").split(" ")) {
					BLOCKSTATES.put(nameLegacy, (IBlockState) object);
				}
			}
		}
	}
	
	@Nullable
	private static Object getBlockOrBlockState(@Nonnull final String nameFull) {
		final Block blockEntry;
		IBlockState blockStateEntry;
		
		final int indexAt = nameFull.indexOf('@');
		final int indexBracket = nameFull.indexOf('[');
		if (indexAt > 0) {
			final String nameBlock = nameFull.substring(0, indexAt);
			
			// to prevent a dormant failure, we first check the metadata to report format error even if mod is missing
			final String stringMetadata = nameFull.substring(indexAt + 1);
			final int intMetadata;
			try {
				intMetadata = Integer.parseInt(stringMetadata);
			} catch (final NumberFormatException exception) {
				WarpDrive.logger.error(String.format("Ignoring block with invalid metadata format: expecting integer, got %s in %s",
				                                     stringMetadata, nameFull));
				return null;
			}
			
			// then check if the block exists in the game
			blockEntry = Block.getBlockFromName(nameBlock);
			if (blockEntry == null) {
				WarpDrive.logger.info(String.format("Ignoring missing in-game block %s in %s",
				                                    nameBlock, nameFull ));
				return null;
			}
			
			// finally build the blockstate itself
			try {
				blockStateEntry = blockEntry.getStateFromMeta(intMetadata);
			} catch (final Exception exception) {
				WarpDrive.logger.info(String.format("Using default state due to exception when fixing block %s in %s",
				                                    nameBlock, nameFull ));
				blockStateEntry = blockEntry.getDefaultState();
			}
			
		} else if (indexBracket > 0) {
			// check closing bracket
			if (nameFull.charAt(nameFull.length() - 1) != ']') {
				throw new RuntimeException(String.format("Invalid syntax, missing closing bracket ] in %s",
				                                         nameFull ));
			}
			
			final String nameBlock = nameFull.substring(0, indexBracket);
			
			// to prevent a dormant failure, we first check the variant to report format error even if mod is missing
			// (note: we don't check for redundant or unsorted property names)
			final String stringVariant = nameFull.substring(indexBracket + 1, nameFull.length() - 1);
			final String[] stringPropertyValues = stringVariant.split(",");
			final HashMap<String, String> propertyValues = new HashMap<>(stringPropertyValues.length);
			for (final String stringPropertyValue : stringPropertyValues) {
				final String[] propertyValue = stringPropertyValue.split("=");
				if (propertyValue.length != 2) {
					throw new RuntimeException(String.format("Invalid syntax, expecting property=value, found '%s' in '%s'",
					                                         stringPropertyValue, nameFull ));
				}
				propertyValues.put(propertyValue[0], propertyValue[1]);
			}
			
			// then check if the block exists in the game
			blockEntry = Block.getBlockFromName(nameBlock);
			if (blockEntry == null) {
				WarpDrive.logger.info(String.format("Ignoring missing in-game block %s in %s",
				                                    nameBlock, nameFull ));
				return null;
			}
			
			// finally build the blockstate itself
			blockStateEntry = blockEntry.getDefaultState();
			for (final IProperty<?> property : blockStateEntry.getPropertyKeys()) {
				final String stringValue = propertyValues.get(property.getName());
				if (stringValue == null) {
					continue;
				}
				propertyValues.remove(property.getName());
				final Optional<?> optionalValue = property.parseValue(stringValue);
				if (!optionalValue.isPresent()) {
					throw new RuntimeException(String.format("Invalid value %s for property %s in %s",
					                                         stringValue, property, nameFull ));
				}
				while (!blockStateEntry.getValue(property).toString().equals(stringValue)) {
					blockStateEntry = blockStateEntry.cycleProperty(property);
				}
			}
			if (!propertyValues.isEmpty()) {
				throw new RuntimeException(String.format("Extraneous properties %s in %s, expecting one of %s",
				                                         propertyValues, nameFull, blockStateEntry.getPropertyKeys() ));
			}
			
		} else {// (just a block)
			blockEntry = Block.getBlockFromName(nameFull);
			if (blockEntry == null) {
				WarpDrive.logger.info(String.format("Ignoring missing in-game block %s",
				                                    nameFull ));
				return null;
			}
			return blockEntry;
		}
		
		return blockStateEntry;
	}
	
	@Nullable
	public static IBlockState getBlockState(@Nonnull final String nameFull) {
		// try existing blocks first
		final Object object = getBlockOrBlockState(nameFull);
		if (object instanceof IBlockState) {
			return (IBlockState) object;
		} else if (object instanceof Block) {
			return ((Block) object).getDefaultState();
		}
		
		// then fall-back to fixing it
		return getFixedBlockState(nameFull);
	}
	
	@Nullable
	private static IBlockState getFixedBlockState(@Nonnull final String nameFull) {
		// ensure metadata is defined
		final int indexAt = nameFull.indexOf('@');
		if (indexAt <= 0) {
			return getFixedBlockState(String.format("%s@0", nameFull));
		}
		
		// try an exact match first
		IBlockState blockState = BLOCKSTATES.get(nameFull);
		if (blockState != null) {
			return blockState;
		}
		Block block = BLOCKS.get(nameFull);
		if (block != null) {
			return block.getDefaultState();
		}
		
		// then try a generic match
		final String nameBlock = nameFull.substring(0, indexAt);
		final String stringMetadata = nameFull.substring(indexAt + 1);
		final int intMetadata;
		try {
			intMetadata = Integer.parseInt(stringMetadata);
		} catch (final NumberFormatException exception) {
			WarpDrive.logger.error(String.format("Ignoring block with invalid metadata format: expecting integer, got %s in %s",
			                                     stringMetadata, nameFull));
			return null;
		}
		
		blockState = BLOCKSTATES.get(nameBlock);
		if (blockState != null) {
			return blockState;
		}
		block = BLOCKS.get(nameBlock);
		if (block != null) {
			return block.getStateFromMeta(intMetadata);
		}
		// otherwise, go blank
		return null;
	}
}
