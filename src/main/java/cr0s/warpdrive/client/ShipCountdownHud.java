package cr0s.warpdrive.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import cr0s.warpdrive.WarpDrive;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-rendered jump countdown.
 *
 * The server sends the absolute end tick once. Rendering from client world time avoids a stream of
 * action-bar packets while preserving the server as the sole authority over when the jump happens.
 */
@Mod.EventBusSubscriber(modid = WarpDrive.MODID, value = Dist.CLIENT,
	bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ShipCountdownHud {

	private static BlockPos sourceCore;
	private static long endGameTick = Long.MIN_VALUE;

	private ShipCountdownHud() {
	}

	public static void update(final BlockPos corePos, final long endTick, final boolean active) {
		if (active) {
			sourceCore = corePos;
			endGameTick = endTick;
		} else if (sourceCore == null || sourceCore.equals(corePos)) {
			clear();
		}
	}

	private static void clear() {
		sourceCore = null;
		endGameTick = Long.MIN_VALUE;
	}

	@SubscribeEvent
	public static void onLogout(final ClientPlayerNetworkEvent.LoggedOutEvent event) {
		clear();
	}

	@SubscribeEvent
	public static void onRenderOverlay(final RenderGameOverlayEvent.Post event) {
		if (event.getType() != RenderGameOverlayEvent.ElementType.ALL || endGameTick == Long.MIN_VALUE) {
			return;
		}
		final Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || minecraft.level == null || minecraft.options.hideGui) {
			return;
		}

		final double remainingTicks = endGameTick
			- (minecraft.level.getGameTime() + event.getPartialTicks());
		if (remainingTicks <= 0.0D) {
			clear();
			return;
		}
		// A stale packet must not survive a disconnect into a newly-created world.
		if (remainingTicks > 20.0D * 61.0D) {
			clear();
			return;
		}

		final double seconds = remainingTicks / 20.0D;
		final String text = String.format(java.util.Locale.ROOT, "\u26A0 WARP IN %.1fs", seconds);
		final int colour = seconds <= 3.0D ? 0xFFFF4040 : seconds <= 5.0D ? 0xFFFFAA00 : 0xFFFFFF55;
		final FontRenderer font = minecraft.font;
		final int width = event.getWindow().getGuiScaledWidth();
		final int height = event.getWindow().getGuiScaledHeight();
		final MatrixStack matrixStack = event.getMatrixStack();

		// Above the hotbar, in the same visual area as the old action-bar countdown.
		font.drawShadow(matrixStack, text, (width - font.width(text)) / 2.0F,
			height - 68.0F, colour);
	}
}
