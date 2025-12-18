package cr0s.warpdrive.event;

import cr0s.warpdrive.api.IGlobalRegionProvider;
import cr0s.warpdrive.data.GlobalRegion;

import javax.annotation.Nonnull;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;

import net.minecraftforge.fml.common.FMLCommonHandler;

public abstract class AbstractSequencer {
	
	private static final AtomicBoolean isUpdating = new AtomicBoolean(false);
	private static final ConcurrentHashMap<AbstractSequencer, Boolean> sequencers = new ConcurrentHashMap<>(10);
	private static final CopyOnWriteArraySet<GlobalRegion> globalRegionLocks = new CopyOnWriteArraySet<>();
	
	public static void updateTick() {
		if (sequencers.isEmpty()) {
			return;
		}
		while (!isUpdating.compareAndSet(false, true)) {
			Thread.yield();
		}
		for (final Iterator<Entry<AbstractSequencer, Boolean>> iterator = sequencers.entrySet().iterator(); iterator.hasNext(); ) {
			final Entry<AbstractSequencer, Boolean> entry = iterator.next();
			final boolean doContinue = entry.getKey().onUpdate();
			if (!doContinue) {
				iterator.remove();
			}
		}
		isUpdating.set(false);
	}
	
	@Nonnull
	protected static GlobalRegion addLock(@Nonnull final IGlobalRegionProvider globalRegionProvider) {
		final GlobalRegion globalRegion = new GlobalRegion(globalRegionProvider);
		
		globalRegionLocks.add(globalRegion);
		
		// ensure locked players are no longer inside GUIs
		final MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
		assert server != null;
		for(final EntityPlayerMP entityPlayerMP : server.getPlayerList().getPlayers()) {
			if (globalRegion.contains(entityPlayerMP.getPosition())) {
				entityPlayerMP.closeScreen();
				entityPlayerMP.closeContainer();
			}
		}
		
		return globalRegion;
	}
	
	protected static void removeLock(@Nonnull final GlobalRegion globalRegion) {
		globalRegionLocks.remove(globalRegion);
	}
	
	protected static boolean isLocked(@Nonnull final BlockPos blockPos) {
		for (final GlobalRegion globalRegion : globalRegionLocks) {
			if (globalRegion.contains(blockPos)) {
				return true;
			}
		}
		return false;
	}
	
	protected void register() {
		while (!isUpdating.compareAndSet(false, true)) {
			Thread.yield();
		}
		sequencers.put(this, true);
		isUpdating.set(false);
	}
	
	protected void unregister() {
		sequencers.put(this, false);
	}
	
	abstract public boolean onUpdate();

	abstract protected void readFromNBT(@Nonnull final NBTTagCompound tagCompound);

	abstract protected NBTTagCompound writeToNBT(@Nonnull final NBTTagCompound tagCompound);
	
}
