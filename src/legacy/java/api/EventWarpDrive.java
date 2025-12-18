package cr0s.warpdrive.api;

import cr0s.warpdrive.api.computer.IShipController;

import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

public abstract class EventWarpDrive extends Event {
	
	EventWarpDrive() {
		super();
	}
	
	public static abstract class Ship extends EventWarpDrive {
		
		// ship core location
		public final World worldCurrent;
		public final BlockPos posCurrent;
		
		// ship access
		public final IShipController shipController;
		
		// movement description, see EnumShipMovementType
		public final String movementType;
		
		public Ship(final World world, final BlockPos blockPos,
		            final IShipController shipController, final String movementType) {
			super();
			
			this.worldCurrent = world;
			this.posCurrent = blockPos;
			
			this.shipController = shipController;
			
			this.movementType = movementType;
		}
		
		// event used to update movement costs for display or an actual jump
		public static class MovementCosts extends Ship {
			
			public final int mass;
			public final int distance;
			
			// original values for reference
			public final int warmup_seconds_initial;
			public final int energyRequired_initial;
			public final int cooldown_seconds_initial;
			public final int sickness_seconds_initial;
			public final int maximumDistance_blocks_initial;
			
			// applied values
			private int maximumDistance_blocks;
			private int energyRequired;
			private int warmup_seconds;
			private int sickness_seconds;
			private int cooldown_seconds;
			
			public MovementCosts(final World world, final BlockPos blockPos,
			                     final IShipController shipController, final String movementType,
			                     final int mass,
			                     final int distance,
			                     final int maximumDistance_blocks,
			                     final int energyRequired,
			                     final int warmup_seconds,
			                     final int sickness_seconds,
			                     final int cooldown_seconds) {
				super(world, blockPos, shipController, movementType);
				
				this.mass = mass;
				this.distance = distance;
				
				this.warmup_seconds_initial = warmup_seconds;
				this.warmup_seconds = warmup_seconds;
				this.energyRequired_initial = energyRequired;
				this.energyRequired = energyRequired;
				this.cooldown_seconds_initial = cooldown_seconds;
				this.cooldown_seconds = cooldown_seconds;
				this.sickness_seconds_initial = sickness_seconds;
				this.sickness_seconds = sickness_seconds;
				this.maximumDistance_blocks_initial = maximumDistance_blocks;
				this.maximumDistance_blocks = maximumDistance_blocks;
			}
			
			public int getMaximumDistance_blocks() {
				return maximumDistance_blocks;
			}
			
			public void setMaximumDistance_blocks(final int maximumDistance_blocks) {
				this.maximumDistance_blocks = maximumDistance_blocks;
			}
			
			public int getEnergyRequired() {
				return energyRequired;
			}
			
			public void setEnergyRequired(final int energyRequired) {
				this.energyRequired = energyRequired;
			}
			
			public int getWarmup_seconds() {
				return warmup_seconds;
			}
			
			public void setWarmup_seconds(final int warmup_seconds) {
				this.warmup_seconds = warmup_seconds;
			}
			
			public int getSickness_seconds() {
				return sickness_seconds;
			}
			
			public void setSickness_seconds(final int sickness_seconds) {
				this.sickness_seconds = sickness_seconds;
			}
			
			public int getCooldown_seconds() {
				return cooldown_seconds;
			}
			
			public void setCooldown_seconds(final int cooldown_seconds) {
				this.cooldown_seconds = cooldown_seconds;
			}
		}
		
		// event for canceling a jump by broad permissions, called prior to jump
		@Cancelable
		public static class PreJump extends Ship {
			
			// cancellation message
			private final WarpDriveText reason;
			
			public PreJump(final World world, final BlockPos blockPos,
			               final IShipController shipController, final String movementType) {
				super(world, blockPos, shipController, movementType);
				
				this.reason = new WarpDriveText();
			}
			
			public WarpDriveText getReason() {
				return reason;
			}
			
			public void appendReason(final ITextComponent reasonAdded) {
				reason.append(reasonAdded);
			}
		}
		
		// event for checking collision at target location, called during jump until a valid location is found
		@Cancelable
		public static class TargetCheck extends Ship {
			
			// movement vector
			public final int moveX;
			public final int moveY;
			public final int moveZ;
			
			// target position
			public final World worldTarget;
			public final AxisAlignedBB aabbTarget;
			
			// cancellation message
			private final WarpDriveText reason;
			
			public TargetCheck(final World worldCurrent, final BlockPos blockPos,
			                   final IShipController shipController, final String movementType,
			                   final int moveX, final int moveY, final int moveZ,
			                   final World worldTarget, final AxisAlignedBB aabbTarget) {
				super(worldCurrent, blockPos, shipController, movementType);
				
				this.moveX = moveX;
				this.moveY = moveY;
				this.moveZ = moveZ;
				
				this.worldTarget = worldTarget;
				this.aabbTarget = aabbTarget;
				
				this.reason = new WarpDriveText();
			}
			
			public WarpDriveText getReason() {
				return reason;
			}
			
			public void appendReason(final ITextComponent reasonAdded) {
				reason.append(reasonAdded);
			}
		}
		
		// event reporting when a jump is cancelled or successful
		public static class JumpResult extends Ship {
			
			public final boolean isSuccessful;
			public final String reason;
			
			public JumpResult(final World world, final BlockPos blockPos,
			                  final IShipController shipController, final String jumpType,
			                  final boolean isSuccessful, final ITextComponent reason) {
				super(world, blockPos, shipController, jumpType);
				
				this.isSuccessful = isSuccessful;
				this.reason = reason == null ? "" : reason.getFormattedText();
			}
		}
	}
	
}
