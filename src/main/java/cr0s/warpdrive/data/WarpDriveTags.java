package cr0s.warpdrive.data;

import cr0s.warpdrive.WarpDrive;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.ITag;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ResourceLocation;

/**
 * Datapack-backed behaviour classifications ported from the 1.12.2 Dictionary.
 *
 * <p>These are intentionally WarpDrive tags rather than lists of optional-mod registry names.
 * Other mods and modpacks can append their 1.16 content with normal datapacks, and a reload updates
 * every consumer without a configuration restart.</p>
 */
public final class WarpDriveTags {

	public static final ITag.INamedTag<Item> BREATHING_HELMETS = item("breathing_helmets");
	public static final ITag.INamedTag<Item> FLY_IN_SPACE = item("fly_in_space");
	public static final ITag.INamedTag<Item> NO_FALL_DAMAGE = item("no_fall_damage");

	public static final ITag.INamedTag<EntityType<?>> LIVING_WITHOUT_AIR =
		entity("living_without_air");
	public static final ITag.INamedTag<EntityType<?>> NON_LIVING_TARGETS =
		entity("non_living_targets");
	public static final ITag.INamedTag<EntityType<?>> NO_REVEAL = entity("no_reveal");
	public static final ITag.INamedTag<EntityType<?>> SHIP_ENTITY_ANCHORS =
		entity("ship_movement/anchors");
	public static final ITag.INamedTag<EntityType<?>> SHIP_ENTITIES_LEFT_BEHIND =
		entity("ship_movement/left_behind");

	public static final ITag.INamedTag<Block> MINING = block("mining");
	public static final ITag.INamedTag<Block> MINING_SKIP = block("mining_skip");
	public static final ITag.INamedTag<Block> MINING_STOP = block("mining_stop");
	public static final ITag.INamedTag<Block> NO_BLINK = block("no_blink");
	public static final ITag.INamedTag<Block> NO_CAMOUFLAGE = block("no_camouflage");
	public static final ITag.INamedTag<Block> SHIP_ANCHORS = block("ship_movement/anchors");
	public static final ITag.INamedTag<Block> SHIP_EXPANDABLE = block("ship_movement/expandable");
	public static final ITag.INamedTag<Block> SHIP_LEFT_BEHIND = block("ship_movement/left_behind");
	public static final ITag.INamedTag<Block> SHIP_NO_MASS = block("ship_movement/no_mass");
	public static final ITag.INamedTag<Block> PLACE_EARLIEST =
		block("ship_movement/place_earliest");
	public static final ITag.INamedTag<Block> PLACE_EARLIER =
		block("ship_movement/place_earlier");
	public static final ITag.INamedTag<Block> PLACE_NORMAL =
		block("ship_movement/place_normal");
	public static final ITag.INamedTag<Block> PLACE_LATER =
		block("ship_movement/place_later");
	public static final ITag.INamedTag<Block> PLACE_LATEST =
		block("ship_movement/place_latest");

	private WarpDriveTags() {
	}

	private static ITag.INamedTag<Block> block(final String path) {
		return BlockTags.createOptional(new ResourceLocation(WarpDrive.MODID, path));
	}

	private static ITag.INamedTag<Item> item(final String path) {
		return ItemTags.createOptional(new ResourceLocation(WarpDrive.MODID, path));
	}

	private static ITag.INamedTag<EntityType<?>> entity(final String path) {
		return EntityTypeTags.createOptional(new ResourceLocation(WarpDrive.MODID, path));
	}
}
