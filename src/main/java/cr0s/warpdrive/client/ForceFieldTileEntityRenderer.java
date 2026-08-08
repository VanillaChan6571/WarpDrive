package cr0s.warpdrive.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import cr0s.warpdrive.block.forcefield.ForceFieldTileEntity;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;

/** Renders a projected field as the solid block selected by a camouflage relay. */
public class ForceFieldTileEntityRenderer extends TileEntityRenderer<ForceFieldTileEntity> {

	public ForceFieldTileEntityRenderer(final TileEntityRendererDispatcher dispatcher) {
		super(dispatcher);
	}

	@Override
	public void render(final ForceFieldTileEntity tileEntity, final float partialTicks,
	                   final MatrixStack matrixStack, final IRenderTypeBuffer buffer,
	                   final int combinedLight, final int combinedOverlay) {
		final BlockState camouflage = tileEntity.getCamouflage();
		if (camouflage == null) return;
		Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
			camouflage, matrixStack, buffer, combinedLight, combinedOverlay);
	}
}
