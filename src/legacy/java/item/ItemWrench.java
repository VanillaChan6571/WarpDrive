package cr0s.warpdrive.item;

import cr0s.warpdrive.api.IWarpTool;
import cr0s.warpdrive.data.EnumTier;
import cr0s.warpdrive.data.Vector3;
import cr0s.warpdrive.network.PacketHandler;

import javax.annotation.Nonnull;

import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ItemWrench extends ItemAbstractBase implements IWarpTool {
	
	public ItemWrench(final String registryName, final EnumTier enumTier) {
		super(registryName, enumTier);
		
		setMaxDamage(0);
		setMaxStackSize(1);
		setTranslationKey("warpdrive.tool.wrench");
		setFull3D();
	}
	
	@Nonnull
	@Override
	public EnumActionResult onItemUse(@Nonnull final EntityPlayer entityPlayer,
	                                  @Nonnull final World world, @Nonnull final BlockPos blockPos, @Nonnull final EnumHand hand,
	                                  @Nonnull final EnumFacing facing, final float hitX, final float hitY, final float hitZ) {
		if (world.isRemote) {
			return EnumActionResult.FAIL;
		}
		
		// get context
		final ItemStack itemStackHeld = entityPlayer.getHeldItem(hand);
		final IBlockState blockState = world.getBlockState(blockPos);
		if (blockState.getBlock().isAir(blockState, world, blockPos)) {
			return EnumActionResult.FAIL;
		}
		
		// note: we allow sneaking usage so we can rotate blocks with GUIs
		
		// compute effect position
		final Vector3 vFace = new Vector3(blockPos).translate(0.5D);
		
		// @TODO: confirm if both are really needed
		if ( !entityPlayer.canPlayerEdit(blockPos, facing, itemStackHeld)
		  || !world.isBlockModifiable(entityPlayer, blockPos) ) {
			PacketHandler.sendSpawnParticlePacket(world, "jammed", (byte) 5,
			                                      vFace,
			                                      new Vector3(0.0D, 0.0D, 0.0D),
			                                      1.0F, 1.0F, 1.0F,
			                                      1.0F, 1.0F, 1.0F,
			                                      6);
			return EnumActionResult.FAIL;
		}
		
		if (!blockState.getBlock().rotateBlock(world, blockPos, facing)) {
			PacketHandler.sendSpawnParticlePacket(world, "jammed", (byte) 5,
			                                      vFace,
			                                      new Vector3(0.0D, 0.0D, 0.0D),
			                                      1.0F, 1.0F, 1.0F,
			                                      1.0F, 1.0F, 1.0F,
			                                      6);
			return EnumActionResult.FAIL;
		}
		
		// no chat message
		
		// standard place sound effect
		final SoundType soundType = blockState.getBlock().getSoundType(blockState, world, blockPos, null);
		world.playSound(null, blockPos, soundType.getPlaceSound(), SoundCategory.BLOCKS,
		                (soundType.getVolume() + 1.0F) / 2.0F, soundType.getPitch() * 0.8F);
		
		world.notifyNeighborsOfStateChange(blockPos, blockState.getBlock(), false);
		
		entityPlayer.swingArm(hand);
		
		return EnumActionResult.SUCCESS;
	}
}
