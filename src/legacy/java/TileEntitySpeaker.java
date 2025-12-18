package cr0s.warpdrive.block.detection;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.block.TileEntityAbstractMachine;
import cr0s.warpdrive.config.WarpDriveConfig;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;

import javax.annotation.Nonnull;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.ITextComponent;

import net.minecraftforge.fml.common.Optional;

public class TileEntitySpeaker extends TileEntityAbstractMachine {
	
	// persistent properties
	// (none)
	
	// computed properties
	private final float rateDecayPerTick = WarpDriveConfig.SPEAKER_RATE_MAX_MESSAGES / (float) WarpDriveConfig.SPEAKER_RATE_PERIOD_TICKS;
	private AxisAlignedBB aabbRange = null;
	private float rateMessaging = 0.0F;
	private final Queue<String> messagesToSpeak = new LinkedList<>();
	
	public TileEntitySpeaker() {
		super();
		
		peripheralName = "warpdriveSpeaker";
		addMethods(new String[] {
			"speak"
		});
		CC_scripts = Collections.singletonList("speak");
	}
	
	@Override
	protected void onFirstUpdateTick() {
		super.onFirstUpdateTick();
		
		final float range = WarpDriveConfig.SPEAKER_RANGE_BLOCKS_BY_TIER[enumTier.getIndex()];
		aabbRange = new AxisAlignedBB(
				pos.getX() - range, pos.getY() - range, pos.getZ() - range,
				pos.getX() + range + 1.0D, pos.getY() + range + 1.0D, pos.getZ() + range + 1.0D );
	}
	
	@Override
	public void update() {
		super.update();
		
		rateMessaging = Math.max(0.0F, rateMessaging - rateDecayPerTick);
		if ( isEnabled
		  && !messagesToSpeak.isEmpty()
		  && rateMessaging + 1.0F < WarpDriveConfig.SPEAKER_RATE_MAX_MESSAGES ) {
			final String messageRaw = messagesToSpeak.remove();
			final ITextComponent messageFormatted = new WarpDriveText(null, messageRaw);
			final List<EntityPlayerMP> playersInRange = world.getEntitiesWithinAABB(EntityPlayerMP.class, aabbRange,
			                                                                        entityPlayerMP -> entityPlayerMP != null
			                                                                                       && entityPlayerMP.isEntityAlive()
			                                                                                       && !entityPlayerMP.isSpectator() );
			for (final EntityPlayerMP entityPlayerMP : playersInRange) {
				Commons.addChatMessage(entityPlayerMP, messageFormatted);
			}
			rateMessaging++;
		}
	}
	
	// Common OC/CC methods
	public Object[] speak(final Object[] arguments) {
		int size = messagesToSpeak.size();
		if (arguments.length == 1) {
			if (size >= WarpDriveConfig.SPEAKER_QUEUE_MAX_MESSAGES) {
				return new Object[] { false, "You're speaking too fast... breath!" };
			}
			messagesToSpeak.add(Commons.toString(arguments[0]));
			size++;
		}
		return new Object[] { true, size };
	}
	
	// OpenComputers callback methods
	@Callback(direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] speak(final Context context, final Arguments arguments) {
		return speak(OC_convertArgumentsAndLogCall(context, arguments));
	}
	
	// ComputerCraft IPeripheral methods
	@Override
	@Optional.Method(modid = "computercraft")
	protected Object[] CC_callMethod(@Nonnull final String methodName, @Nonnull final Object[] arguments) {
		switch (methodName) {
		case "speak":
			return speak(arguments);
		}
		
		return super.CC_callMethod(methodName, arguments);
	}
	
	@Override
	public String toString() {
		return String.format("%s %s %s messages queued",
		                     getClass().getSimpleName(),
		                     Commons.format(world, pos),
		                     messagesToSpeak.size() );
	}
}