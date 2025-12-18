package cr0s.warpdrive.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;

import net.minecraftforge.client.IRenderHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class RenderBlank extends IRenderHandler {
	
	private static RenderBlank INSTANCE = null;
	
	public static RenderBlank getInstance() {
	    if (INSTANCE == null) {
	        INSTANCE = new RenderBlank();
	    }
	    return INSTANCE;
	}
	
	@SideOnly(Side.CLIENT)
	@Override
	public void render(final float partialTicks, final WorldClient world, final Minecraft mc) {
	}
}
