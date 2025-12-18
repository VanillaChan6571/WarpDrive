package cr0s.warpdrive.data;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.config.WarpDriveConfig;

import java.util.Iterator;
import java.util.LinkedList;

import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class CamerasRegistry {
	private LinkedList<CameraRegistryItem> registry;
	
	public CamerasRegistry() {
		registry = new LinkedList<>();
	}
	
	public CameraRegistryItem getCameraByVideoChannel(final World world, final int videoChannel) {
		if (world == null) {
			return null;
		}
		CameraRegistryItem cam;
		for (final Iterator<CameraRegistryItem> it = registry.iterator(); it.hasNext();) {
			cam = it.next();
			if (cam.videoChannel == videoChannel && cam.dimensionId == world.provider.getDimension()) {
				if (isCamAlive(world, cam)) {
					return cam;
				} else {
					if (WarpDriveConfig.LOGGING_VIDEO_CHANNEL) {
						WarpDrive.logger.info(String.format("Removing 'dead' camera %s (while searching)",
						                                    Commons.format(world, cam.blockPos)));
					}
					it.remove();
				}
			}
		}
		
		// not found => dump registry
		printRegistry(world);
		return null;
	}
	
	private CameraRegistryItem getCamByPosition(final World world, final BlockPos blockPos) {
		for (final CameraRegistryItem cam : registry) {
			if ( cam.blockPos.getX() == blockPos.getX()
			  && cam.blockPos.getY() == blockPos.getY()
			  && cam.blockPos.getZ() == blockPos.getZ()
			  && cam.dimensionId == world.provider.getDimension() ) {
				return cam;
			}
		}
		
		return null;
	}
	
	private static boolean isCamAlive(final World world, final CameraRegistryItem cam) {
		if (world.provider.getDimension() != cam.dimensionId) {
			WarpDrive.logger.error(String.format("Inconsistent world with camera %s: world %d vs cam %d",
			                                     cam, world.provider.getDimension(), cam.dimensionId));
			return false;
		}
		
		if (!world.getChunk(cam.blockPos).isLoaded()) {
			if (WarpDriveConfig.LOGGING_VIDEO_CHANNEL) {
				WarpDrive.logger.info(String.format("Reporting an 'unloaded' camera %s",
				                                    Commons.format(world, cam.blockPos)));
			}
			return false;
		}
		final Block block = world.getBlockState(cam.blockPos).getBlock();
		if ( block != WarpDrive.blockCamera
		  && block != WarpDrive.blockLaserCamera ) {
			if (WarpDriveConfig.LOGGING_VIDEO_CHANNEL) {
				WarpDrive.logger.info(String.format("Reporting a 'dead' camera %s",
				                                    Commons.format(world, cam.blockPos)));
			}
			return false;
		}
		
		return true;
	}
	
	private void removeDeadCams(final World world) {
		// LocalProfiler.start("CamRegistry Removing dead cameras");
		
		CameraRegistryItem cam;
		for (final Iterator<CameraRegistryItem> it = registry.iterator(); it.hasNext();) {
			cam = it.next();
			if (!isCamAlive(world, cam)) {
				if (WarpDriveConfig.LOGGING_VIDEO_CHANNEL) {
					WarpDrive.logger.info(String.format("Removing 'dead' camera %s",
					                                    Commons.format(world, cam.blockPos)));
				}
				it.remove();
			}
		}
		
		// LocalProfiler.stop();
	}
	
	public void removeFromRegistry(final World world, final BlockPos blockPos) {
		final CameraRegistryItem cam = getCamByPosition(world, blockPos);
		if (cam != null) {
			if (WarpDriveConfig.LOGGING_VIDEO_CHANNEL) {
				WarpDrive.logger.info(String.format("Removing camera by request %s",
				                                    Commons.format(world, cam.blockPos)));
			}
			registry.remove(cam);
		}
	}
	
	public void updateInRegistry(final World world, final BlockPos blockPos, final int videoChannel, final EnumCameraType enumCameraType) {
		final CameraRegistryItem cam = new CameraRegistryItem(world, blockPos, videoChannel, enumCameraType);
		removeDeadCams(world);
		
		if (isCamAlive(world, cam)) {
			final CameraRegistryItem existingCam = getCamByPosition(world, cam.blockPos);
			if (existingCam == null) {
				if (WarpDriveConfig.LOGGING_VIDEO_CHANNEL) {
					WarpDrive.logger.info(String.format("Adding 'live' camera %s with video channel %s",
					                                    Commons.format(world, cam.blockPos),
					                                    cam.videoChannel));
				}
				registry.add(cam);
			} else if (existingCam.videoChannel != cam.videoChannel) {
				if (WarpDriveConfig.LOGGING_VIDEO_CHANNEL) {
					WarpDrive.logger.info(String.format("Updating 'live' camera %s from video channel %d to video channel %d",
					                                    Commons.format(world, cam.blockPos),
					                                    existingCam.videoChannel, cam.videoChannel));
				}
				existingCam.videoChannel = cam.videoChannel;
			}
		} else {
			if (WarpDriveConfig.LOGGING_VIDEO_CHANNEL) {
				WarpDrive.logger.info(String.format("Unable to update 'dead' camera %s",
				                                    Commons.format(world, cam.blockPos)));
			}
		}
	}
	
	public void printRegistry(final World world) {
		if (world == null) {
			return;
		}
		WarpDrive.logger.info(String.format("Cameras registry for %s:",
		                                    Commons.format(world)));
		
		for (final CameraRegistryItem cam : registry) {
			WarpDrive.logger.info(String.format("- %d (%d %d %d)",
			                                    cam.videoChannel, cam.blockPos.getX(), cam.blockPos.getY(), cam.blockPos.getZ()));
		}
	}
}
