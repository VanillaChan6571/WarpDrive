package cr0s.warpdrive.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.config.ClientConfig;
import cr0s.warpdrive.block.breathing.AirShieldBlock;
import cr0s.warpdrive.data.AirClassifier;
import cr0s.warpdrive.event.BreathingManager;
import cr0s.warpdrive.item.AirTankItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Direction;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.gui.ForgeIngameGui;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Air reserve gauge and breathing alarm, ported from 1.12.2 RenderOverlayAir.
 *
 * The gauge reuses vanilla's own icons sheet rather than shipping artwork, as the original did: the
 * bar is the horse jump bar (empty at v=84, full at v=89, 71 pixels wide) and the icon is the air
 * bubble (u=16 normal, u=25 bursting).
 *
 * Three details here are the original's and are easy to get wrong:
 *
 *   - The bar is drawn UNTINTED. It only takes colour while an alarm is on screen, and then it is
 *     red, pulsing in step with the alarm's own alpha - not a green-to-red reserve gradient.
 *   - The bubble is animated off the reserve *changing*, not off suit state: it shows the bursting
 *     frame for 8 ticks after each breath is drawn, blanks between 9 and 16, then returns to the
 *     normal frame. So it visibly pops once per breath.
 *   - Warnings are a splash alarm drawn over the screen, not action bar text.
 *
 * The alarm is gated on proximity to open space (getRangeToVoid) or the first 20 seconds after
 * joining, exactly as the original was. The gauge itself always shows in a vacuum dimension.
 */
@Mod.EventBusSubscriber(modid = WarpDrive.MODID, value = Dist.CLIENT,
                        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AirOverlayRenderer extends AbstractGui {

	/** Instance only so the protected blit helpers on AbstractGui are reachable. */
	private static final AirOverlayRenderer INSTANCE = new AirOverlayRenderer();

	private static final int BAR_WIDTH = 71;
	private static final int BAR_HEIGHT = 5;

	private static final String KEY_ALARM = "warpdrive.breathing.alarm";
	private static final String KEY_INVALID_SETUP = "warpdrive.breathing.invalid_setup";
	private static final String KEY_NO_AIR = "warpdrive.breathing.no_air";
	private static final String KEY_LOW_RESERVE = "warpdrive.breathing.low_reserve";
	private static final String KEY_SUIT_ALARM = "warpdrive.suit.alarm";
	private static final String KEY_SUIT_INCOMPLETE = "warpdrive.suit.incomplete";

	/** Reserve below which the low-air alarm sounds. */
	private static final float LOW_RESERVE_RATIO = 0.15F;

	/** Grace window after joining during which the alarm shows regardless of proximity. */
	private static final int WARNING_ON_JOIN_TICKS = 20 * 20;

	/** Horizontal facings only - the original checks sideways for a breach, not up and down. */
	private static final Direction[] HORIZONTALS = {
		Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST };

	/** Cache so the scan runs once per tick rather than once per frame. */
	private static VoidExposure cachedVoidExposure = VoidExposure.NONE;
	private static int cachedRangeTick = -1;

	/** The shield is permeable to players, but still separates breathable air from open vacuum. */
	private enum VoidExposure {
		NONE,
		SHIELDED,
		OPEN
	}

	/** Bubble animation state - the reserve value last seen, and when it changed. */
	private static float ratioPreviousAir = 1.0F;
	private static long timePreviousAir = 0L;

	private AirOverlayRenderer() {
	}

	@SubscribeEvent
	public static void onRenderOverlay(final RenderGameOverlayEvent.Post event) {
		if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) {
			return;
		}

		final Minecraft minecraft = Minecraft.getInstance();
		final ClientPlayerEntity player = minecraft.player;
		if (player == null || minecraft.level == null || minecraft.options.hideGui) {
			return;
		}
		if (!BreathingManager.isVacuum(minecraft.level)) {
			return;
		}
		if (player.isCreative() || player.isSpectator()) {
			return;
		}

		INSTANCE.render(event.getMatrixStack(),
			event.getWindow().getGuiScaledWidth(),
			event.getWindow().getGuiScaledHeight(),
			player);
	}

	private void render(final MatrixStack matrixStack, final int width, final int height,
	                    final ClientPlayerEntity player) {
		final Minecraft minecraft = Minecraft.getInstance();
		final float ratioAirReserve = BreathingManager.getAirReserveRatio(player);
		final boolean hasValidSetup = BreathingManager.hasValidSetup(player);

		RenderSystem.enableBlend();

		// Splash alarm first - its alpha drives the bar tint below.
		//
		// Only raised when open space is actually within reach, or during the first seconds after
		// joining. Deep inside a sealed ship there is nothing to warn about: an incomplete suit
		// only matters if the vacuum can get to you, and alarming there would be crying wolf every
		// time someone took their helmet off indoors.
		int alpha = 255;
		boolean criticalAlarm = false;
		final VoidExposure exposure = getVoidExposure(player);
		final boolean nearVoid = exposure != VoidExposure.NONE
		                      || player.tickCount < WARNING_ON_JOIN_TICKS;
		if (nearVoid) {
			if (!hasValidSetup) {
				if (exposure == VoidExposure.SHIELDED) {
					alpha = drawSplashAlarm(matrixStack, width, height,
						KEY_SUIT_ALARM, KEY_SUIT_INCOMPLETE, false);
				} else {
					criticalAlarm = true;
					alpha = drawSplashAlarm(matrixStack, width, height,
						KEY_ALARM, KEY_INVALID_SETUP, true);
				}
			} else if (ratioAirReserve <= 0.0F) {
				criticalAlarm = true;
				alpha = drawSplashAlarm(matrixStack, width, height,
					KEY_ALARM, KEY_NO_AIR, true);
			} else if (ratioAirReserve < LOW_RESERVE_RATIO) {
				criticalAlarm = true;
				alpha = drawSplashAlarm(matrixStack, width, height,
					KEY_ALARM, KEY_LOW_RESERVE, true);
			}
		}

		// Drawing text swapped the bound texture out - put the icons sheet back
		minecraft.getTextureManager().bind(AbstractGui.GUI_ICONS_LOCATION);

		// Right-hand HUD column, directly above the food bar
		final int left = width / 2 + 91;
		final int top = height - ForgeIngameGui.right_height;

		drawBubble(matrixStack, player, ratioAirReserve, left, top);

		// Empty gauge
		blit(matrixStack, left - 81, top + 2, 20, 84, BAR_WIDTH, BAR_HEIGHT);

		// Filled portion, right-anchored so it drains towards the bubble
		final int filled = MathHelper.ceil(ratioAirReserve * BAR_WIDTH);
		if (filled > 0) {
			if (criticalAlarm && alpha != 255) {
				// Alarm is up: pulse the bar red in step with it
				final float factor = 1.0F - alpha / 255.0F;
				final float fade = 0.2F + 0.8F * factor;
				RenderSystem.color4f(1.0F, fade, fade, 1.0F);
			}
			blit(matrixStack, left - 10 - filled, top + 2, 91 - filled, 89, filled, BAR_HEIGHT);
			RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
		}

		// Drawn last, because text rendering rebinds the texture away from the icons sheet
		final int textHeight = drawTimeRemaining(matrixStack, player, ratioAirReserve, left, top);

		// Reserve the bar row plus the readout sitting above it, so anything else stacking on the
		// right-hand column clears the whole element rather than just the bar
		ForgeIngameGui.right_height += 10 + textHeight;
		RenderSystem.disableBlend();
	}

	/**
	 * Time left before asphyxiation, as m:ss just left of the gauge.
	 *
	 * The breath currently being drawn is server state, but it does not need syncing: a breath is
	 * taken exactly when a tank's contents change, which is what timePreviousAir already records
	 * for the bubble animation. So the client can infer the remainder locally. The one soft spot is
	 * immediately after login, where the start of an in-progress breath is unknown and the readout
	 * can sit up to 15 seconds low until the next breath resets it.
	 */
	private int drawTimeRemaining(final MatrixStack matrixStack, final ClientPlayerEntity player,
	                              final float ratioAirReserve, final int left, final int top) {
		if (!ClientConfig.AIR_TIMER_VISIBLE.get()) {
			return 0;
		}
		final FontRenderer font = Minecraft.getInstance().font;

		final long sinceBreath = player.level.getGameTime() - timePreviousAir;
		final long remainder = AirTankItem.BREATH_DURATION_TICKS - sinceBreath;
		final int currentBreath = (int) Math.max(0L,
			Math.min(AirTankItem.BREATH_DURATION_TICKS, remainder));

		final int totalSeconds = (currentBreath + BreathingManager.getStoredAirTicks(player)) / 20;
		final String text = String.format("%d:%02d", totalSeconds / 60, totalSeconds % 60);

		// Centred over the bar and sitting above it, so it reads as a label for the gauge rather
		// than competing with the bubble for the same row
		final int barCentre = left - 45;
		final int textHeight = font.lineHeight + 1;

		// Match the alarm threshold so the readout turns red at the same moment the alarm fires
		final int colour = ratioAirReserve < LOW_RESERVE_RATIO ? 0xFFFF5540 : 0xFFFFFFFF;
		font.drawShadow(matrixStack, text,
			barCentre - font.width(text) / 2.0F + ClientConfig.AIR_TIMER_OFFSET_X.get(),
			top - textHeight + ClientConfig.AIR_TIMER_OFFSET_Y.get(),
			colour);

		return textHeight;
	}

	/**
	 * The bubble pops each time the reserve drops: bursting frame for 8 ticks, nothing for the
	 * next 8, then back to the normal frame.
	 */
	private void drawBubble(final MatrixStack matrixStack, final ClientPlayerEntity player,
	                        final float ratioAirReserve, final int left, final int top) {
		final long timeWorld = player.level.getGameTime();
		if (ratioAirReserve != ratioPreviousAir) {
			timePreviousAir = timeWorld;
			ratioPreviousAir = ratioAirReserve;
		}

		final long timeDelta = timeWorld - timePreviousAir;
		if (timeDelta >= 0 && timeDelta <= 8) {
			blit(matrixStack, left - 9, top, 25, 18, 9, 9);   // bursting
		} else if (timeDelta < 0 || timeDelta > 16) {
			blit(matrixStack, left - 9, top, 16, 18, 9, 9);   // normal
		}
		// 9..16 draws nothing, so the bubble blinks
	}

	/**
	 * Classify nearby open space as directly reachable, separated by an energy air shield, or absent.
	 *
	 * Ported from 1.12.2 RenderOverlayAir.getRangeToVoid. The second step only happens through a
	 * neighbour that does not seal - so it reaches out through an open doorway or a gap in the hull,
	 * but stops at a wall. That is what makes it an airlock check rather than a radius: standing
	 * one block inside a closed door is safe, standing one block inside an open one is not.
	 *
	 * Cached per tick because this is called from render, which runs far more often than the world
	 * changes. A real opening wins over a shield in another direction, so an actual breach can never
	 * be downgraded to the yellow suit advisory.
	 */
	private static VoidExposure getVoidExposure(final ClientPlayerEntity player) {
		if (player.tickCount == cachedRangeTick) {
			return cachedVoidExposure;
		}
		cachedRangeTick = player.tickCount;
		cachedVoidExposure = computeVoidExposure(player);
		return cachedVoidExposure;
	}

	private static VoidExposure computeVoidExposure(final ClientPlayerEntity player) {
		final ClientWorld world = (ClientWorld) player.level;
		final BlockPos origin = player.blockPosition();

		if (AirClassifier.isVoid(world, origin)) {
			return VoidExposure.OPEN;
		}

		boolean foundShieldedVacuum = false;
		for (final Direction direction : HORIZONTALS) {
			final BlockPos close = origin.relative(direction);
			if (AirClassifier.isVoid(world, close)) {
				return VoidExposure.OPEN;
			}
			// A sealing neighbour blocks the line of sight to vacuum; anything else lets it through.
			//
			// An air shield is the exception, and 1.12.2 spelled it out the same way: it seals air
			// but you can walk through it, so vacuum is still one step away and the warning should
			// stay up. A wall earns silence, a curtain does not.
			final boolean isAirShield = world.getBlockState(close).getBlock() instanceof AirShieldBlock;
			if (!isAirShield && AirClassifier.isSealer(world, close)) {
				continue;
			}
			if (AirClassifier.isVoid(world, origin.relative(direction, 2))) {
				if (isAirShield) {
					foundShieldedVacuum = true;
				} else {
					return VoidExposure.OPEN;
				}
			}
		}
		return foundShieldedVacuum ? VoidExposure.SHIELDED : VoidExposure.NONE;
	}

	/**
	 * Bold red title over a pulsing message, drawn at double scale near the top of the screen.
	 * Returns the message alpha so the caller can pulse the gauge in sync.
	 */
	private int drawSplashAlarm(final MatrixStack matrixStack, final int width, final int height,
	                            final String titleKey, final String messageKey,
	                            final boolean critical) {
		final Minecraft minecraft = Minecraft.getInstance();
		final FontRenderer font = minecraft.font;

		// Free-running animation clock, independent of world time so it pulses while paused
		final double cycle = ((System.nanoTime() / 1000L) % 0x200000) / (double) 0x200000;
		final int alpha = 160 + (int) (85.0D * Math.sin(cycle * 2.0D * Math.PI));

		matrixStack.pushPose();
		// Everything below is in half-scale coordinates, hence width / 4 for the centre
		matrixStack.scale(2.0F, 2.0F, 1.0F);

		int y = height / 10;

		final ITextComponent title = new TranslationTextComponent(titleKey)
			.withStyle(TextFormatting.BOLD);
		font.drawShadow(matrixStack, title,
			width / 4.0F - font.width(title) / 2.0F,
			y - font.lineHeight,
			critical ? argb(230, 255, 32, 24) : argb(230, 255, 220, 48));

		final ITextComponent message = new TranslationTextComponent(messageKey);
		final List<IReorderingProcessor> lines = font.split(message, width / 2);
		for (final IReorderingProcessor line : lines) {
			font.draw(matrixStack, line,
				width / 4.0F - font.width(line) / 2.0F,
				y,
				critical ? argb(alpha, 192, 64, 48) : argb(alpha, 240, 200, 64));
			y += font.lineHeight;
		}

		matrixStack.popPose();
		return alpha;
	}

	private static int argb(final int alpha, final int red, final int green, final int blue) {
		return (alpha & 0xFF) << 24 | (red & 0xFF) << 16 | (green & 0xFF) << 8 | (blue & 0xFF);
	}
}
