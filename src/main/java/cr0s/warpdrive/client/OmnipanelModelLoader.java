package cr0s.warpdrive.client;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.data.Registration;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ModelResourceLocation;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Swaps the ordinary block and item models for the dynamic omnipanel mesh after model baking.
 *
 * On the mod event bus rather than Forge's: both events are mod-lifecycle, and subscribing on the
 * wrong bus silently never fires.
 */
@Mod.EventBusSubscriber(modid = WarpDrive.MODID, value = Dist.CLIENT,
                        bus = Mod.EventBusSubscriber.Bus.MOD)
public final class OmnipanelModelLoader {

	private OmnipanelModelLoader() {
	}

	@SubscribeEvent
	public static void onModelBake(final ModelBakeEvent event) {
		install(event, "air_shield");
		for (final String panel : Registration.HULL_OMNIPANELS.keySet()) {
			install(event, panel);
		}
	}

	private static void install(final ModelBakeEvent event, final String panel) {
			final ModelResourceLocation blockKey =
				new ModelResourceLocation(new ResourceLocation(WarpDrive.MODID, panel), "");
			final IBakedModel blockBase = event.getModelRegistry().get(blockKey);
			if (blockBase == null) {
				WarpDrive.logger.error("No baked model registered for {} - cannot install panel model", blockKey);
			} else {
				event.getModelRegistry().put(blockKey, new OmnipanelBakedModel(blockBase));
			}

			final ModelResourceLocation itemKey =
				new ModelResourceLocation(new ResourceLocation(WarpDrive.MODID, panel), "inventory");
			final IBakedModel itemBase = event.getModelRegistry().get(itemKey);
			if (itemBase == null) {
				WarpDrive.logger.error("No baked item model registered for {} - cannot install panel item model", itemKey);
			} else {
				event.getModelRegistry().put(itemKey, new OmnipanelBakedModel(itemBase));
			}
	}
}
