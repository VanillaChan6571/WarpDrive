package cr0s.warpdrive.block;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IGlobalRegionProvider;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.api.computer.ICoreSignature;
import cr0s.warpdrive.api.computer.IMachine;
import cr0s.warpdrive.config.WarpDriveConfig;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;

import net.minecraftforge.fml.common.Optional;

public abstract class TileEntityAbstractMachine extends TileEntityAbstractInterfaced implements IMachine {
	
	protected static final WarpDriveText VALIDITY_ISSUES_UNKNOWN = new WarpDriveText(Commons.getStyleWarning(), "unknown");
	
	// persistent properties
	public String name = "";
	protected boolean isEnabled = true;
	
	// computed properties
	private boolean isDirtyAssembly = true;
	private int tickScanAssembly = 0;
	protected boolean isAssemblyValid = true;
	protected WarpDriveText textValidityIssues = VALIDITY_ISSUES_UNKNOWN;
	
	private boolean isDirtyParameters = true;
	private int tickUpdateParameters = 0;
	
	// allow only one computation at a time
	protected static final AtomicBoolean isGlobalThreadRunning = new AtomicBoolean(false);
	// computation is ongoing for this specific tile
	protected final AtomicBoolean isThreadRunning = new AtomicBoolean(false);
	// parameters have changed, new computation is required
	protected final AtomicBoolean isDirty = new AtomicBoolean(true);
	
	public TileEntityAbstractMachine() {
		super();
		
		addMethods(new String[] {
				"name",
				"enable",
				"getAssemblyStatus"
				});
	}
	
	@Override
	protected void onFirstUpdateTick() {
		super.onFirstUpdateTick();
		
		// force full assembly scan and parameters update, only server side, before any other processing
		if (world.isRemote) {
			return;
		}
		
		tickScanAssembly = WarpDriveConfig.G_ASSEMBLY_SCAN_INTERVAL_TICKS;
		isDirtyAssembly = false;
		doScanAssembly(true);
		
		tickUpdateParameters = WarpDriveConfig.G_PARAMETERS_UPDATE_INTERVAL_TICKS;
		isDirtyParameters = false;
		doUpdateParameters(true);
	}
	
	@Override
	public void update() {
		super.update();
		
		if (world.isRemote) {
			return;
		}
		
		// scan the assembly after a block update was detected or periodically to recover whatever may have desynchronized it
		if (isDirtyAssembly) {
			tickScanAssembly = Math.min(10, tickScanAssembly);
		}
		tickScanAssembly--;
		if (tickScanAssembly <= 0) {
			tickScanAssembly = WarpDriveConfig.G_ASSEMBLY_SCAN_INTERVAL_TICKS;
			final boolean isDirty = isDirtyAssembly;
			isDirtyAssembly = false;
			
			doScanAssembly(isDirty);
		}
		
		// update operational parameters when dirty or periodically to recover whatever may have desynchronized them
		if (isDirtyParameters) {
			tickUpdateParameters = 0;
		}
		tickUpdateParameters--;
		if (tickUpdateParameters <= 0) {
			tickUpdateParameters = WarpDriveConfig.G_PARAMETERS_UPDATE_INTERVAL_TICKS;
			final boolean isDirty = isDirtyParameters;
			isDirtyParameters = false;
			
			doUpdateParameters(isDirty);
		}
		
	}
	
	public boolean isDirtyAssembly() {
		return isDirtyAssembly;
	}
	
	public void markDirtyAssembly() {
		
		if (WarpDrive.isDev) {
			WarpDrive.logger.info(String.format("%s markDirtyAssembly at %d", this, world.getWorldTime()));
			if (Commons.throttleMe("markDirtyAssembly")) {
				new Exception().printStackTrace(WarpDrive.printStreamInfo);
			}
		}
		
		isDirtyAssembly = true;
	}
	
	private void doScanAssembly(final boolean isDirty) {
		if (world.isRemote) {
			return;
		}
		
		final WarpDriveText textReason = new WarpDriveText();
		final boolean isValid = doScanAssembly(isDirty, textReason);
		if (!isValid && textReason.isEmpty()) {
			textReason.append(Commons.getStyleWarning(), "unknown");
			WarpDrive.logger.warn(String.format("Unknown assembly status %s %s, please report to mod author",
			                                    this, Commons.format(world, pos) ));
		}
		isAssemblyValid = isValid;
		textValidityIssues = textReason;
	}
	
	protected boolean doScanAssembly(final boolean isDirty, final WarpDriveText textReason) {
		return true;
	}
	
	protected void markDirtyParameters() {
		isDirtyParameters = true;
	}
	
	protected void doUpdateParameters(final boolean isDirty) {
		// no operation
	}
	
	@Override
	public WarpDriveText getStatus() {
		final WarpDriveText textStatus = super.getStatus();
		if ( world != null
		  && !world.isRemote
		  && !textValidityIssues.isEmpty() ) {
			textStatus.append(textValidityIssues);
		}
		return textStatus;
	}
	
	public boolean isCalculated() {
		return !isDirty.get() && !isThreadRunning.get();
	}
	
	protected boolean calculation_start() {
		assert !world.isRemote;
		if (isAssemblyValid) {
			if (!isGlobalThreadRunning.getAndSet(true)) {
				isThreadRunning.set(true);
				isDirty.set(false);
				// override should create the thread
				// once finished, thread should call calculation_done()
				return true;
			}
		}
		return false;
	}
	
	final protected void calculation_done() {
		isThreadRunning.set(false);
		isGlobalThreadRunning.set(false);
	}
	
	@Override
	public void readFromNBT(@Nonnull final NBTTagCompound tagCompound) {
		super.readFromNBT(tagCompound);
		
		name = tagCompound.getString(ICoreSignature.NAME_TAG);
		setIsEnabled( !tagCompound.hasKey("isEnabled") || tagCompound.getBoolean("isEnabled"));
	}
	
	@Nonnull
	@Override
	public NBTTagCompound writeToNBT(@Nonnull NBTTagCompound tagCompound) {
		tagCompound = super.writeToNBT(tagCompound);
		
		if (!name.equals("")) {
			tagCompound.setString(ICoreSignature.NAME_TAG, name);
		}
		tagCompound.setBoolean("isEnabled", isEnabled);
		return tagCompound;
	}
	
	@Override
	public NBTTagCompound writeItemDropNBT(NBTTagCompound tagCompound) {
		tagCompound = super.writeItemDropNBT(tagCompound);
		
		tagCompound.removeTag("isEnabled");
		return tagCompound;
	}
	
	private static final Predicate<EntityPlayerMP> ALIVE_NOT_SPECTATING_PLAYER = entityPlayerMP -> entityPlayerMP != null
	                                                                                               && entityPlayerMP.isEntityAlive()
	                                                                                               && !entityPlayerMP.isSpectator();
	public String getAllPlayersInArea() {
		final AxisAlignedBB axisalignedbb = this instanceof IGlobalRegionProvider
		                                  ? ((IGlobalRegionProvider) this).getGlobalRegionArea()
		                                  : new AxisAlignedBB(pos).grow(10.0D);
		
		final List<EntityPlayerMP> entityPlayers = world.getPlayers(EntityPlayerMP.class, ALIVE_NOT_SPECTATING_PLAYER::test);
		final StringBuilder stringBuilderResult = new StringBuilder();
		
		boolean isFirst = true;
		for (final EntityPlayerMP entityPlayerMP : entityPlayers) {
			if (!entityPlayerMP.getEntityBoundingBox().intersects(axisalignedbb)) {
				continue;
			}
			if (isFirst) {
				isFirst = false;
			} else {
				stringBuilderResult.append(", ");
			}
			stringBuilderResult.append(entityPlayerMP.getName());
		}
		return stringBuilderResult.toString();
	}
	
	@Override
	public boolean getIsEnabled() {
		return isEnabled;
	}
	
	public void setIsEnabled(final boolean isEnabled) {
		final boolean isEnabledOld = this.isEnabled;
		this.isEnabled = isEnabled;
		// force update through main thread since CC & OC are running outside the main thread
		if (isEnabledOld != isEnabled) {
			markDirty();
		}
	}
	
	// Common OC/CC methods
	@Override
	public String[] name(final Object[] arguments) {
		if ( arguments != null
		  && arguments.length == 1
		  && arguments[0] != null ) {
			final String namePrevious = name;
			name = Commons.sanitizeFileName((String) arguments[0]);
			if (!name.equals(namePrevious)) {
				WarpDrive.logger.info(String.format("Machine renamed from '%s' to '%s' with player(s) %s",
				                                    namePrevious == null ? "-null-" : namePrevious,
				                                    name,
				                                    getAllPlayersInArea()));
			}
		}
		return new String[] { name };
	}
	
	@Override
	public Object[] enable(final Object[] arguments) {
		if ( arguments != null
		  && arguments.length == 1
		  && arguments[0] != null ) {
			final boolean enableRequest;
			try {
				enableRequest = Commons.toBool(arguments[0]);
			} catch (final Exception exception) {
				final String message = String.format("%s LUA error on enable(): Boolean expected for 1st argument %s",
				                                     this, arguments[0]);
				if (WarpDriveConfig.LOGGING_LUA) {
					WarpDrive.logger.error(message);
				}
				return new Object[] { isEnabled, message };
			}
			if (isEnabled && !enableRequest) {
				setIsEnabled(false);
				sendEvent("disabled", name);
			} else if (!isEnabled && enableRequest) {
				setIsEnabled(true);
				sendEvent("enabled", name);
			}
		}
		return new Object[] { isEnabled };
	}
	
	@Override
	public Object[] getAssemblyStatus() {
		if (isAssemblyValid && textValidityIssues.isEmpty()) {
			return new Object[] { isAssemblyValid, "ok" };
		}
		return new Object[] { isAssemblyValid, Commons.removeFormatting( textValidityIssues.getUnformattedText() ) };
	}
	
	@Override
	public boolean isAssemblyValid() {
		return isAssemblyValid;
	}
	
	// OpenComputers callback methods
	@Callback(direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] name(final Context context, final Arguments arguments) {
		return name(OC_convertArgumentsAndLogCall(context, arguments));
	}
	
	@Callback(direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] enable(final Context context, final Arguments arguments) {
		return enable(OC_convertArgumentsAndLogCall(context, arguments));
	}
	
	@Callback(direct = true)
	@Optional.Method(modid = "opencomputers")
	public Object[] getAssemblyStatus(final Context context, final Arguments arguments) {
		OC_convertArgumentsAndLogCall(context, arguments);
		return getAssemblyStatus();
	}
	
	// ComputerCraft IPeripheral methods
	@Override
	@Optional.Method(modid = "computercraft")
	protected Object[] CC_callMethod(@Nonnull final String methodName, @Nonnull final Object[] arguments) {
		switch (methodName) {
		case "name":
			return name(arguments);
			
		case "enable":
			return enable(arguments);
			
		case "getAssemblyStatus":
			return getAssemblyStatus();
		}
		
		return super.CC_callMethod(methodName, arguments);
	}
	
	@Override
	public String toString() {
		return String.format("%s '%s' %s",
		                     getClass().getSimpleName(),
		                     name,
		                     Commons.format(world, pos));
	}
}
