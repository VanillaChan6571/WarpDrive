package cr0s.warpdrive.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import cr0s.warpdrive.block.ShipCoreTileEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Renders bounding boxes for ships and jump destinations.
 *
 * Only the set of tracked core positions is stored here. The geometry is derived from the live
 * ShipCoreTileEntity every frame, so movement changes and jumps are reflected without the caller
 * having to push updated coordinates in. Caching absolute coordinates instead meant the boxes went
 * stale whenever the ship moved or the movement vector changed.
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BoundingBoxRenderer {

    private static final Set<BlockPos> tracked = new HashSet<>();

    public static void track(BlockPos corePos) {
        tracked.add(corePos.immutable());
    }

    public static void untrack(BlockPos corePos) {
        tracked.remove(corePos);
    }

    public static boolean isTracked(BlockPos corePos) {
        return tracked.contains(corePos);
    }

    public static void clear() {
        tracked.clear();
    }

    @SubscribeEvent
    public static void onRenderWorldLast(RenderWorldLastEvent event) {
        if (tracked.isEmpty()) {
            return;
        }

        final World world = Minecraft.getInstance().level;
        if (world == null) {
            tracked.clear();
            return;
        }

        final MatrixStack matrixStack = event.getMatrixStack();
        final IRenderTypeBuffer.Impl buffer = Minecraft.getInstance().renderBuffers().bufferSource();
        final Vector3d cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();

        matrixStack.pushPose();
        matrixStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        final Iterator<BlockPos> iterator = tracked.iterator();
        while (iterator.hasNext()) {
            final BlockPos corePos = iterator.next();

            // After a jump the old position is air, so the entry retires itself here
            final TileEntity tileEntity = world.getBlockEntity(corePos);
            if (!(tileEntity instanceof ShipCoreTileEntity)) {
                iterator.remove();
                continue;
            }

            final ShipCoreTileEntity shipCore = (ShipCoreTileEntity) tileEntity;
            if (!shipCore.isBoundingBoxShown()) {
                iterator.remove();
                continue;
            }

            final double[] ship = shipCore.getShipBoxBounds();
            WorldRenderer.renderLineBox(
                matrixStack,
                buffer.getBuffer(RenderType.lines()),
                ship[0], ship[1], ship[2],
                ship[3], ship[4], ship[5],
                0.0f, 1.0f, 0.0f, 1.0f  // Green
            );

            final double[] destination = shipCore.getDestinationBoxBounds();
            if (destination != null) {
                WorldRenderer.renderLineBox(
                    matrixStack,
                    buffer.getBuffer(RenderType.lines()),
                    destination[0], destination[1], destination[2],
                    destination[3], destination[4], destination[5],
                    0.0f, 1.0f, 1.0f, 1.0f  // Cyan
                );
            }
        }

        matrixStack.popPose();

        RenderSystem.disableDepthTest();
        buffer.endBatch(RenderType.lines());
        RenderSystem.enableDepthTest();
    }
}
