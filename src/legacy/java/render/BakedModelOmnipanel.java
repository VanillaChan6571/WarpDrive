package cr0s.warpdrive.render;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.block.BlockAbstractOmnipanel;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.util.EnumFacing;

import net.minecraftforge.common.property.IExtendedBlockState;

public class BakedModelOmnipanel extends BakedModelAbstractBase {
	
	private static final List<BakedQuad> EMPTY = new ArrayList<>(0);
	
	private IExtendedBlockState extendedBlockStateDefault;
	
	public BakedModelOmnipanel() {
		super();
	}
	
	@Nonnull
	@Override
	public List<BakedQuad> getQuads(@Nullable final IBlockState blockState, @Nullable final EnumFacing enumFacing, final long rand) {
		assert modelResourceLocation != null;
		assert bakedModelOriginal != null;
		
		final IExtendedBlockState extendedBlockState;
		if ( blockState instanceof IExtendedBlockState
		  && ((IExtendedBlockState) blockState).getValue(BlockAbstractOmnipanel.CAN_CONNECT_Y_NEG) != null ) {
			extendedBlockState = (IExtendedBlockState) blockState;
			
		} else {
			// first, ensure we've a fallback
			if (extendedBlockStateDefault == null) {
				if (!(blockStateDefault instanceof IExtendedBlockState)) {
					if (Commons.throttleMe("BakedModelOmnipanel Invalid default")) {
						new RuntimeException(String.format("Invalid default blockstate %s for model %s",
						                                   blockStateDefault, modelResourceLocation ))
								.printStackTrace(WarpDrive.printStreamError);
					}
					return bakedModelOriginal.getQuads(null, enumFacing, rand);
				}
				extendedBlockStateDefault = ((IExtendedBlockState) blockStateDefault)
						                            .withProperty(BlockAbstractOmnipanel.CAN_CONNECT_Y_NEG, false)
						                            .withProperty(BlockAbstractOmnipanel.CAN_CONNECT_Y_POS, false)
						                            .withProperty(BlockAbstractOmnipanel.CAN_CONNECT_Z_NEG, false)
						                            .withProperty(BlockAbstractOmnipanel.CAN_CONNECT_Z_POS, false)
						                            .withProperty(BlockAbstractOmnipanel.CAN_CONNECT_X_NEG, false)
						                            .withProperty(BlockAbstractOmnipanel.CAN_CONNECT_X_POS, false)
						                            .withProperty(BlockAbstractOmnipanel.HAS_XN_YN, true)
						                            .withProperty(BlockAbstractOmnipanel.HAS_XP_YN, true)
						                            .withProperty(BlockAbstractOmnipanel.HAS_XN_YP, true)
						                            .withProperty(BlockAbstractOmnipanel.HAS_XP_YP, true)
						                            .withProperty(BlockAbstractOmnipanel.HAS_XN_ZN, true)
						                            .withProperty(BlockAbstractOmnipanel.HAS_XP_ZN, true)
						                            .withProperty(BlockAbstractOmnipanel.HAS_XN_ZP, true)
						                            .withProperty(BlockAbstractOmnipanel.HAS_XP_ZP, true)
						                            .withProperty(BlockAbstractOmnipanel.HAS_ZN_YN, true)
						                            .withProperty(BlockAbstractOmnipanel.HAS_ZP_YN, true)
						                            .withProperty(BlockAbstractOmnipanel.HAS_ZN_YP, true)
						                            .withProperty(BlockAbstractOmnipanel.HAS_ZP_YP, true);
			}
			
			if (blockState == null) {// (probably an item form)
				extendedBlockState = extendedBlockStateDefault;
				
			} else if (!(blockState instanceof IExtendedBlockState)) {
				if (Commons.throttleMe("BakedModelOmnipanel Invalid non-extended")) {
					new RuntimeException(String.format("Invalid non-extended blockstate %s for model %s",
					                                   blockStateDefault, modelResourceLocation ))
							.printStackTrace(WarpDrive.printStreamError);
				}
				return bakedModelOriginal.getQuads(null, enumFacing, rand);
				
			} else {// it's an extended state without values
				if (Commons.throttleMe("BakedModelOmnipanel Invalid extended")) {
					new RuntimeException(String.format("Invalid extended blockstate %s for model %s",
					                                   blockStateDefault, modelResourceLocation ))
							.printStackTrace(WarpDrive.printStreamError);
				}
				extendedBlockState = extendedBlockStateDefault;
				
			}
		}
		
		// normal model has no facing
		if (enumFacing != null) {
			return EMPTY;
		}
		
		// get color
		final int color = Minecraft.getMinecraft().getBlockColors().colorMultiplier(blockState == null ? blockStateDefault : blockState, null, null, tintIndex);
		
		// pre-compute coordinates
		final float dX_min = 0.0F;
		final float dX_max = 1.0F;
		final float dY_min = 0.0F;
		final float dY_max = 1.0F;
		final float dZ_min = 0.0F;
		final float dZ_max = 1.0F;
		final float dX_neg = BlockAbstractOmnipanel.CENTER_MIN;
		final float dX_pos = BlockAbstractOmnipanel.CENTER_MAX;
		final float dY_neg = BlockAbstractOmnipanel.CENTER_MIN;
		final float dY_pos = BlockAbstractOmnipanel.CENTER_MAX;
		final float dZ_neg = BlockAbstractOmnipanel.CENTER_MIN;
		final float dZ_pos = BlockAbstractOmnipanel.CENTER_MAX;
		
		final float dU_min = spriteBlock.getMinU();
		final float dU_neg = spriteBlock.getInterpolatedU(7.0F);
		final float dU_pos = spriteBlock.getInterpolatedU(9.0F);
		final float dU_max = spriteBlock.getMaxU();
		
		final float dV_min = spriteBlock.getMinV();
		final float dV_neg = spriteBlock.getInterpolatedV(7.0F);
		final float dV_pos = spriteBlock.getInterpolatedV(9.0F);
		final float dV_max = spriteBlock.getMaxV();
		
		// get direct connections
		final boolean canConnectY_neg = extendedBlockState.getValue(BlockAbstractOmnipanel.CAN_CONNECT_Y_NEG);
		final boolean canConnectY_pos = extendedBlockState.getValue(BlockAbstractOmnipanel.CAN_CONNECT_Y_POS);
		final boolean canConnectZ_neg = extendedBlockState.getValue(BlockAbstractOmnipanel.CAN_CONNECT_Z_NEG);
		final boolean canConnectZ_pos = extendedBlockState.getValue(BlockAbstractOmnipanel.CAN_CONNECT_Z_POS);
		final boolean canConnectX_neg = extendedBlockState.getValue(BlockAbstractOmnipanel.CAN_CONNECT_X_NEG);
		final boolean canConnectX_pos = extendedBlockState.getValue(BlockAbstractOmnipanel.CAN_CONNECT_X_POS);
		final boolean canConnectNone = !canConnectY_neg && !canConnectY_pos && !canConnectZ_neg && !canConnectZ_pos && !canConnectX_neg && !canConnectX_pos;
		
		// get panels
		final boolean hasXnYn = extendedBlockState.getValue(BlockAbstractOmnipanel.HAS_XN_YN);
		final boolean hasXpYn = extendedBlockState.getValue(BlockAbstractOmnipanel.HAS_XP_YN);
		final boolean hasXnYp = extendedBlockState.getValue(BlockAbstractOmnipanel.HAS_XN_YP);
		final boolean hasXpYp = extendedBlockState.getValue(BlockAbstractOmnipanel.HAS_XP_YP);
		
		final boolean hasXnZn = extendedBlockState.getValue(BlockAbstractOmnipanel.HAS_XN_ZN);
		final boolean hasXpZn = extendedBlockState.getValue(BlockAbstractOmnipanel.HAS_XP_ZN);
		final boolean hasXnZp = extendedBlockState.getValue(BlockAbstractOmnipanel.HAS_XN_ZP);
		final boolean hasXpZp = extendedBlockState.getValue(BlockAbstractOmnipanel.HAS_XP_ZP);
		
		final boolean hasZnYn = extendedBlockState.getValue(BlockAbstractOmnipanel.HAS_ZN_YN);
		final boolean hasZpYn = extendedBlockState.getValue(BlockAbstractOmnipanel.HAS_ZP_YN);
		final boolean hasZnYp = extendedBlockState.getValue(BlockAbstractOmnipanel.HAS_ZN_YP);
		final boolean hasZpYp = extendedBlockState.getValue(BlockAbstractOmnipanel.HAS_ZP_YP);
		
		final List<BakedQuad> quads = new ArrayList<>();
		
		{// z plane
			if (hasXnYn) {
				addBakedQuad(quads, spriteBlock, color,
				             dX_neg, dY_neg, dZ_neg, dU_neg, dV_pos,
				             dX_neg, dY_min, dZ_neg, dU_neg, dV_max,
				             dX_min, dY_min, dZ_neg, dU_min, dV_max,
				             dX_min, dY_neg, dZ_neg, dU_min, dV_pos);
				
				addBakedQuad(quads, spriteBlock, color,
				             dX_min, dY_neg, dZ_pos, dU_min, dV_pos,
				             dX_min, dY_min, dZ_pos, dU_min, dV_max,
				             dX_neg, dY_min, dZ_pos, dU_neg, dV_max,
				             dX_neg, dY_neg, dZ_pos, dU_neg, dV_pos);
			} else {
				if (canConnectX_neg) {
					addBakedQuad(quads, spriteBlock, color,
					             dX_neg, dY_neg, dZ_neg, dU_neg, dV_pos,
					             dX_neg, dY_neg, dZ_pos, dU_neg, dV_neg,
					             dX_min, dY_neg, dZ_pos, dU_min, dV_neg,
					             dX_min, dY_neg, dZ_neg, dU_min, dV_pos);
				}
				if (canConnectY_neg) {
					addBakedQuad(quads, spriteBlock, color,
					             dX_neg, dY_neg, dZ_neg, dU_neg, dV_pos,
					             dX_neg, dY_min, dZ_neg, dU_neg, dV_max,
					             dX_neg, dY_min, dZ_pos, dU_pos, dV_max,
					             dX_neg, dY_neg, dZ_pos, dU_pos, dV_pos);
				}
			}
			
			if (hasXpYn) {
				addBakedQuad(quads, spriteBlock, color,
				             dX_max, dY_neg, dZ_neg, dU_max, dV_pos,
				             dX_max, dY_min, dZ_neg, dU_max, dV_max,
				             dX_pos, dY_min, dZ_neg, dU_pos, dV_max,
				             dX_pos, dY_neg, dZ_neg, dU_pos, dV_pos);
				
				addBakedQuad(quads, spriteBlock, color,
				             dX_pos, dY_neg, dZ_pos, dU_pos, dV_pos,
				             dX_pos, dY_min, dZ_pos, dU_pos, dV_max,
				             dX_max, dY_min, dZ_pos, dU_max, dV_max,
				             dX_max, dY_neg, dZ_pos, dU_max, dV_pos);
			} else {
				if (canConnectX_pos) {
					addBakedQuad(quads, spriteBlock, color,
					             dX_max, dY_neg, dZ_neg, dU_max, dV_pos,
					             dX_max, dY_neg, dZ_pos, dU_max, dV_neg,
					             dX_pos, dY_neg, dZ_pos, dU_pos, dV_neg,
					             dX_pos, dY_neg, dZ_neg, dU_pos, dV_pos);
				}
				if (canConnectY_neg) {
					addBakedQuad(quads, spriteBlock, color,
					             dX_pos, dY_neg, dZ_pos, dU_pos, dV_pos,
					             dX_pos, dY_min, dZ_pos, dU_pos, dV_max,
					             dX_pos, dY_min, dZ_neg, dU_neg, dV_max,
					             dX_pos, dY_neg, dZ_neg, dU_neg, dV_pos);
				}
			}
			
			if (hasXnYp) {
				addBakedQuad(quads, spriteBlock, color,
				             dX_neg, dY_max, dZ_neg, dU_neg, dV_min,
				             dX_neg, dY_pos, dZ_neg, dU_neg, dV_neg,
				             dX_min, dY_pos, dZ_neg, dU_min, dV_neg,
				             dX_min, dY_max, dZ_neg, dU_min, dV_min);
				
				addBakedQuad(quads, spriteBlock, color,
				             dX_min, dY_max, dZ_pos, dU_min, dV_min,
				             dX_min, dY_pos, dZ_pos, dU_min, dV_neg,
				             dX_neg, dY_pos, dZ_pos, dU_neg, dV_neg,
				             dX_neg, dY_max, dZ_pos, dU_neg, dV_min);
			} else {
				if (canConnectX_neg) {
					addBakedQuad(quads, spriteBlock, color,
					             dX_neg, dY_pos, dZ_pos, dU_neg, dV_neg,
					             dX_neg, dY_pos, dZ_neg, dU_neg, dV_pos,
					             dX_min, dY_pos, dZ_neg, dU_min, dV_pos,
					             dX_min, dY_pos, dZ_pos, dU_min, dV_neg);
				}
				if (canConnectY_pos) {
					addBakedQuad(quads, spriteBlock, color,
					             dX_neg, dY_pos, dZ_pos, dU_pos, dV_neg,
					             dX_neg, dY_max, dZ_pos, dU_pos, dV_min,
					             dX_neg, dY_max, dZ_neg, dU_neg, dV_min,
					             dX_neg, dY_pos, dZ_neg, dU_neg, dV_neg);
				}
			}
			
			if (hasXpYp) {
				addBakedQuad(quads, spriteBlock, color,
				             dX_max, dY_max, dZ_neg, dU_max, dV_min,
				             dX_max, dY_pos, dZ_neg, dU_max, dV_neg,
				             dX_pos, dY_pos, dZ_neg, dU_pos, dV_neg,
				             dX_pos, dY_max, dZ_neg, dU_pos, dV_min);
				
				addBakedQuad(quads, spriteBlock, color,
				             dX_pos, dY_max, dZ_pos, dU_pos, dV_min,
				             dX_pos, dY_pos, dZ_pos, dU_pos, dV_neg,
				             dX_max, dY_pos, dZ_pos, dU_max, dV_neg,
				             dX_max, dY_max, dZ_pos, dU_max, dV_min);
			} else {
				if (canConnectX_pos) {
					addBakedQuad(quads, spriteBlock, color,
					             dX_pos, dY_pos, dZ_neg, dU_pos, dV_pos,
					             dX_pos, dY_pos, dZ_pos, dU_pos, dV_neg,
					             dX_max, dY_pos, dZ_pos, dU_max, dV_neg,
					             dX_max, dY_pos, dZ_neg, dU_max, dV_pos);
				}
				if (canConnectY_pos) {
					addBakedQuad(quads, spriteBlock, color,
					             dX_pos, dY_pos, dZ_neg, dU_neg, dV_neg,
					             dX_pos, dY_max, dZ_neg, dU_neg, dV_min,
					             dX_pos, dY_max, dZ_pos, dU_pos, dV_min,
					             dX_pos, dY_pos, dZ_pos, dU_pos, dV_neg);
				}
			}
		}
		
		{// x plane
			if (hasZnYn) {
				addBakedQuad(quads, spriteBlock, color,
				             dX_neg, dY_neg, dZ_min, dU_min, dV_pos,
				             dX_neg, dY_min, dZ_min, dU_min, dV_max,
				             dX_neg, dY_min, dZ_neg, dU_neg, dV_max,
				             dX_neg, dY_neg, dZ_neg, dU_neg, dV_pos);
				
				addBakedQuad(quads, spriteBlock, color,
				             dX_pos, dY_neg, dZ_neg, dU_neg, dV_pos,
				             dX_pos, dY_min, dZ_neg, dU_neg, dV_max,
				             dX_pos, dY_min, dZ_min, dU_min, dV_max,
				             dX_pos, dY_neg, dZ_min, dU_min, dV_pos);
			} else {
				if (canConnectZ_neg) {
					addBakedQuad(quads, spriteBlock, color,
					             dX_neg, dY_neg, dZ_min, dU_neg, dV_max,
					             dX_pos, dY_neg, dZ_min, dU_pos, dV_max,
					             dX_pos, dY_neg, dZ_neg, dU_pos, dV_pos,
					             dX_neg, dY_neg, dZ_neg, dU_neg, dV_pos);
				}
				if (canConnectY_neg) {
					addBakedQuad(quads, spriteBlock, color,
					             dX_pos, dY_neg, dZ_neg, dU_pos, dV_pos,
					             dX_pos, dY_min, dZ_neg, dU_pos, dV_max,
					             dX_neg, dY_min, dZ_neg, dU_neg, dV_max,
					             dX_neg, dY_neg, dZ_neg, dU_neg, dV_pos);
				}
			}
			
			if (hasZpYn) {
				addBakedQuad(quads, spriteBlock, color,
				             dX_neg, dY_neg, dZ_pos, dU_pos, dV_pos,
				             dX_neg, dY_min, dZ_pos, dU_pos, dV_max,
				             dX_neg, dY_min, dZ_max, dU_max, dV_max,
				             dX_neg, dY_neg, dZ_max, dU_max, dV_pos);
				
				addBakedQuad(quads, spriteBlock, color,
				             dX_pos, dY_neg, dZ_max, dU_max, dV_pos,
				             dX_pos, dY_min, dZ_max, dU_max, dV_max,
				             dX_pos, dY_min, dZ_pos, dU_pos, dV_max,
				             dX_pos, dY_neg, dZ_pos, dU_pos, dV_pos);
			} else {
				if (canConnectZ_pos) {
					addBakedQuad(quads, spriteBlock, color,
					             dX_neg, dY_neg, dZ_pos, dU_neg, dV_neg,
					             dX_pos, dY_neg, dZ_pos, dU_pos, dV_neg,
					             dX_pos, dY_neg, dZ_max, dU_pos, dV_min,
					             dX_neg, dY_neg, dZ_max, dU_neg, dV_min);
				}
				if (canConnectY_neg) {
					addBakedQuad(quads, spriteBlock, color,
					             dX_neg, dY_neg, dZ_pos, dU_neg, dV_pos,
					             dX_neg, dY_min, dZ_pos, dU_neg, dV_max,
					             dX_pos, dY_min, dZ_pos, dU_pos, dV_max,
					             dX_pos, dY_neg, dZ_pos, dU_pos, dV_pos);
				}
			}
			
			if (hasZnYp) {
				addBakedQuad(quads, spriteBlock, color,
				             dX_neg, dY_max, dZ_min, dU_min, dV_min,
				             dX_neg, dY_pos, dZ_min, dU_min, dV_neg,
				             dX_neg, dY_pos, dZ_neg, dU_neg, dV_neg,
				             dX_neg, dY_max, dZ_neg, dU_neg, dV_min);
				
				addBakedQuad(quads, spriteBlock, color,
				             dX_pos, dY_max, dZ_neg, dU_neg, dV_min,
				             dX_pos, dY_pos, dZ_neg, dU_neg, dV_neg,
				             dX_pos, dY_pos, dZ_min, dU_min, dV_neg,
				             dX_pos, dY_max, dZ_min, dU_min, dV_min);
			} else {
				if (canConnectZ_neg) {
					addBakedQuad(quads, spriteBlock, color,
					             dX_pos, dY_pos, dZ_min, dU_pos, dV_max,
					             dX_neg, dY_pos, dZ_min, dU_neg, dV_max,
					             dX_neg, dY_pos, dZ_neg, dU_neg, dV_pos,
					             dX_pos, dY_pos, dZ_neg, dU_pos, dV_pos);
				}
				if (canConnectY_pos) {
					addBakedQuad(quads, spriteBlock, color,
					             dX_neg, dY_pos, dZ_neg, dU_neg, dV_neg,
					             dX_neg, dY_max, dZ_neg, dU_neg, dV_min,
					             dX_pos, dY_max, dZ_neg, dU_pos, dV_min,
					             dX_pos, dY_pos, dZ_neg, dU_pos, dV_neg);
				}
			}
			
			if (hasZpYp) {
				addBakedQuad(quads, spriteBlock, color,
				             dX_neg, dY_max, dZ_pos, dU_pos, dV_min,
				             dX_neg, dY_pos, dZ_pos, dU_pos, dV_neg,
				             dX_neg, dY_pos, dZ_max, dU_max, dV_neg,
				             dX_neg, dY_max, dZ_max, dU_max, dV_min);
				
				addBakedQuad(quads, spriteBlock, color,
				             dX_pos, dY_max, dZ_max, dU_max, dV_min,
				             dX_pos, dY_pos, dZ_max, dU_max, dV_neg,
				             dX_pos, dY_pos, dZ_pos, dU_pos, dV_neg,
				             dX_pos, dY_max, dZ_pos, dU_pos, dV_min);
			} else {
				if (canConnectZ_pos) {
					addBakedQuad(quads, spriteBlock, color,
					             dX_neg, dY_pos, dZ_max, dU_neg, dV_min,
					             dX_pos, dY_pos, dZ_max, dU_pos, dV_min,
					             dX_pos, dY_pos, dZ_pos, dU_pos, dV_neg,
					             dX_neg, dY_pos, dZ_pos, dU_neg, dV_neg);
				}
				if (canConnectY_pos) {
					addBakedQuad(quads, spriteBlock, color,
					             dX_pos, dY_pos, dZ_pos, dU_pos, dV_neg,
					             dX_pos, dY_max, dZ_pos, dU_pos, dV_min,
					             dX_neg, dY_max, dZ_pos, dU_neg, dV_min,
					             dX_neg, dY_pos, dZ_pos, dU_neg, dV_neg);
				}
			}
		}
		
		{// z plane
			if (hasXnZn) {
				addBakedQuad(quads, spriteBlock, color,
				             dX_min, dY_neg, dZ_neg, dU_min, dV_pos,
				             dX_min, dY_neg, dZ_min, dU_min, dV_max,
				             dX_neg, dY_neg, dZ_min, dU_neg, dV_max,
				             dX_neg, dY_neg, dZ_neg, dU_neg, dV_pos);
				
				addBakedQuad(quads, spriteBlock, color,
				             dX_neg, dY_pos, dZ_neg, dU_neg, dV_pos,
				             dX_neg, dY_pos, dZ_min, dU_neg, dV_max,
				             dX_min, dY_pos, dZ_min, dU_min, dV_max,
				             dX_min, dY_pos, dZ_neg, dU_min, dV_pos);
			} else {
				if (canConnectX_neg) {
					addBakedQuad(quads, spriteBlock, color,
					             dX_min, dY_neg, dZ_neg, dU_min, dV_pos,
					             dX_min, dY_pos, dZ_neg, dU_min, dV_neg,
					             dX_neg, dY_pos, dZ_neg, dU_neg, dV_neg,
					             dX_neg, dY_neg, dZ_neg, dU_neg, dV_pos);
				}
				if (canConnectZ_neg) {
					addBakedQuad(quads, spriteBlock, color,
					             dX_neg, dY_pos, dZ_neg, dU_neg, dV_neg,
					             dX_neg, dY_pos, dZ_min, dU_min, dV_neg,
					             dX_neg, dY_neg, dZ_min, dU_min, dV_pos,
					             dX_neg, dY_neg, dZ_neg, dU_neg, dV_pos);
				}
			}
			
			if (hasXpZn) {
				addBakedQuad(quads, spriteBlock, color,
				             dX_pos, dY_neg, dZ_neg, dU_pos, dV_pos,
				             dX_pos, dY_neg, dZ_min, dU_pos, dV_max,
				             dX_max, dY_neg, dZ_min, dU_max, dV_max,
				             dX_max, dY_neg, dZ_neg, dU_max, dV_pos);
				
				addBakedQuad(quads, spriteBlock, color,
				             dX_max, dY_pos, dZ_neg, dU_max, dV_pos,
				             dX_max, dY_pos, dZ_min, dU_max, dV_max,
				             dX_pos, dY_pos, dZ_min, dU_pos, dV_max,
				             dX_pos, dY_pos, dZ_neg, dU_pos, dV_pos);
			} else {
				if (canConnectX_pos) {
					addBakedQuad(quads, spriteBlock, color,
					             dX_pos, dY_neg, dZ_neg, dU_pos, dV_pos,
					             dX_pos, dY_pos, dZ_neg, dU_pos, dV_neg,
					             dX_max, dY_pos, dZ_neg, dU_max, dV_neg,
					             dX_max, dY_neg, dZ_neg, dU_max, dV_pos);
				}
				if (canConnectZ_neg) {
					addBakedQuad(quads, spriteBlock, color,
					             dX_pos, dY_neg, dZ_neg, dU_neg, dV_pos,
					             dX_pos, dY_neg, dZ_min, dU_min, dV_pos,
					             dX_pos, dY_pos, dZ_min, dU_min, dV_neg,
					             dX_pos, dY_pos, dZ_neg, dU_neg, dV_neg);
				}
			}
			
			if (hasXnZp) {
				addBakedQuad(quads, spriteBlock, color,
				             dX_min, dY_neg, dZ_max, dU_min, dV_min,
				             dX_min, dY_neg, dZ_pos, dU_min, dV_neg,
				             dX_neg, dY_neg, dZ_pos, dU_neg, dV_neg,
				             dX_neg, dY_neg, dZ_max, dU_neg, dV_min);
				
				addBakedQuad(quads, spriteBlock, color,
				             dX_neg, dY_pos, dZ_max, dU_neg, dV_min,
				             dX_neg, dY_pos, dZ_pos, dU_neg, dV_neg,
				             dX_min, dY_pos, dZ_pos, dU_min, dV_neg,
				             dX_min, dY_pos, dZ_max, dU_min, dV_min);
			} else {
				if (canConnectX_neg) {
					addBakedQuad(quads, spriteBlock, color,
					             dX_min, dY_pos, dZ_pos, dU_min, dV_neg,
					             dX_min, dY_neg, dZ_pos, dU_min, dV_pos,
					             dX_neg, dY_neg, dZ_pos, dU_neg, dV_pos,
					             dX_neg, dY_pos, dZ_pos, dU_neg, dV_neg);
				}
				if (canConnectZ_pos) {
					addBakedQuad(quads, spriteBlock, color,
					             dX_neg, dY_neg, dZ_pos, dU_pos, dV_pos,
					             dX_neg, dY_neg, dZ_max, dU_max, dV_pos,
					             dX_neg, dY_pos, dZ_max, dU_max, dV_neg,
					             dX_neg, dY_pos, dZ_pos, dU_pos, dV_neg);
				}
			}
			
			if (hasXpZp) {
				addBakedQuad(quads, spriteBlock, color,
				             dX_pos, dY_neg, dZ_max, dU_pos, dV_min,
				             dX_pos, dY_neg, dZ_pos, dU_pos, dV_neg,
				             dX_max, dY_neg, dZ_pos, dU_max, dV_neg,
				             dX_max, dY_neg, dZ_max, dU_max, dV_min);
				
				addBakedQuad(quads, spriteBlock, color,
				             dX_max, dY_pos, dZ_max, dU_max, dV_min,
				             dX_max, dY_pos, dZ_pos, dU_max, dV_neg,
				             dX_pos, dY_pos, dZ_pos, dU_pos, dV_neg,
				             dX_pos, dY_pos, dZ_max, dU_pos, dV_min);
			} else {
				if (canConnectX_pos) {
					addBakedQuad(quads, spriteBlock, color,
					             dX_max, dY_neg, dZ_pos, dU_max, dV_pos,
					             dX_max, dY_pos, dZ_pos, dU_max, dV_neg,
					             dX_pos, dY_pos, dZ_pos, dU_pos, dV_neg,
					             dX_pos, dY_neg, dZ_pos, dU_pos, dV_pos);
				}
				if (canConnectZ_pos) {
					addBakedQuad(quads, spriteBlock, color,
					             dX_pos, dY_pos, dZ_pos, dU_pos, dV_neg,
					             dX_pos, dY_pos, dZ_max, dU_max, dV_neg,
					             dX_pos, dY_neg, dZ_max, dU_max, dV_pos,
					             dX_pos, dY_neg, dZ_pos, dU_pos, dV_pos);
				}
			}
		}
		
		if (canConnectNone) {
			// x min
			addBakedQuad(quads, spriteBlock, color,
			             dX_min, dY_max, dZ_neg, dU_neg, dV_min,
			             dX_min, dY_min, dZ_neg, dU_neg, dV_max,
			             dX_min, dY_min, dZ_pos, dU_pos, dV_max,
			             dX_min, dY_max, dZ_pos, dU_pos, dV_min);
			
			addBakedQuad(quads, spriteBlock, color,
			             dX_min, dY_pos, dZ_max, dU_max, dV_neg,
			             dX_min, dY_pos, dZ_pos, dU_pos, dV_neg,
			             dX_min, dY_neg, dZ_pos, dU_pos, dV_pos,
			             dX_min, dY_neg, dZ_max, dU_max, dV_pos);
			
			addBakedQuad(quads, spriteBlock, color,
			             dX_min, dY_pos, dZ_neg, dU_neg, dV_neg,
			             dX_min, dY_pos, dZ_min, dU_min, dV_neg,
			             dX_min, dY_neg, dZ_min, dU_min, dV_pos,
			             dX_min, dY_neg, dZ_neg, dU_neg, dV_pos);
			
			// x max
			addBakedQuad(quads, spriteBlock, color,
			             dX_max, dY_max, dZ_pos, dU_pos, dV_min,
			             dX_max, dY_min, dZ_pos, dU_pos, dV_max,
			             dX_max, dY_min, dZ_neg, dU_neg, dV_max,
			             dX_max, dY_max, dZ_neg, dU_neg, dV_min);
			
			addBakedQuad(quads, spriteBlock, color,
			             dX_max, dY_neg, dZ_max, dU_max, dV_pos,
			             dX_max, dY_neg, dZ_pos, dU_pos, dV_pos,
			             dX_max, dY_pos, dZ_pos, dU_pos, dV_neg,
			             dX_max, dY_pos, dZ_max, dU_max, dV_neg);
			
			addBakedQuad(quads, spriteBlock, color,
			             dX_max, dY_neg, dZ_neg, dU_neg, dV_pos,
			             dX_max, dY_neg, dZ_min, dU_min, dV_pos,
			             dX_max, dY_pos, dZ_min, dU_min, dV_neg,
			             dX_max, dY_pos, dZ_neg, dU_neg, dV_neg);
			
			// z min
			addBakedQuad(quads, spriteBlock, color,
			             dX_pos, dY_max, dZ_min, dU_pos, dV_min,
			             dX_pos, dY_min, dZ_min, dU_pos, dV_max,
			             dX_neg, dY_min, dZ_min, dU_neg, dV_max,
			             dX_neg, dY_max, dZ_min, dU_neg, dV_min);
			
			addBakedQuad(quads, spriteBlock, color,
			             dX_neg, dY_pos, dZ_min, dU_neg, dV_neg,
			             dX_neg, dY_neg, dZ_min, dU_neg, dV_pos,
			             dX_min, dY_neg, dZ_min, dU_min, dV_pos,
			             dX_min, dY_pos, dZ_min, dU_min, dV_neg);
			
			addBakedQuad(quads, spriteBlock, color,
			             dX_max, dY_pos, dZ_min, dU_max, dV_neg,
			             dX_max, dY_neg, dZ_min, dU_max, dV_pos,
			             dX_pos, dY_neg, dZ_min, dU_pos, dV_pos,
			             dX_pos, dY_pos, dZ_min, dU_pos, dV_neg);
			
			// z max
			addBakedQuad(quads, spriteBlock, color,
			             dX_neg, dY_max, dZ_max, dU_neg, dV_min,
			             dX_neg, dY_min, dZ_max, dU_neg, dV_max,
			             dX_pos, dY_min, dZ_max, dU_pos, dV_max,
			             dX_pos, dY_max, dZ_max, dU_pos, dV_min);
			
			addBakedQuad(quads, spriteBlock, color,
			             dX_min, dY_pos, dZ_max, dU_min, dV_neg,
			             dX_min, dY_neg, dZ_max, dU_min, dV_pos,
			             dX_neg, dY_neg, dZ_max, dU_neg, dV_pos,
			             dX_neg, dY_pos, dZ_max, dU_neg, dV_neg);
			
			addBakedQuad(quads, spriteBlock, color,
			             dX_pos, dY_pos, dZ_max, dU_pos, dV_neg,
			             dX_pos, dY_neg, dZ_max, dU_pos, dV_pos,
			             dX_max, dY_neg, dZ_max, dU_max, dV_pos,
			             dX_max, dY_pos, dZ_max, dU_max, dV_neg);
			
			// y min
			addBakedQuad(quads, spriteBlock, color,
			             dX_neg, dY_min, dZ_max, dU_neg, dV_min,
			             dX_neg, dY_min, dZ_min, dU_neg, dV_max,
			             dX_pos, dY_min, dZ_min, dU_pos, dV_max,
			             dX_pos, dY_min, dZ_max, dU_pos, dV_min);
			
			addBakedQuad(quads, spriteBlock, color,
			             dX_min, dY_min, dZ_pos, dU_min, dV_neg,
			             dX_min, dY_min, dZ_neg, dU_min, dV_pos,
			             dX_neg, dY_min, dZ_neg, dU_neg, dV_pos,
			             dX_neg, dY_min, dZ_pos, dU_neg, dV_neg);
			
			addBakedQuad(quads, spriteBlock, color,
			             dX_pos, dY_min, dZ_pos, dU_pos, dV_neg,
			             dX_pos, dY_min, dZ_neg, dU_pos, dV_pos,
			             dX_max, dY_min, dZ_neg, dU_max, dV_pos,
			             dX_max, dY_min, dZ_pos, dU_max, dV_neg);
			
			// y max
			addBakedQuad(quads, spriteBlock, color,
			             dX_pos, dY_max, dZ_max, dU_pos, dV_min,
			             dX_pos, dY_max, dZ_min, dU_pos, dV_max,
			             dX_neg, dY_max, dZ_min, dU_neg, dV_max,
			             dX_neg, dY_max, dZ_max, dU_neg, dV_min);
			
			addBakedQuad(quads, spriteBlock, color,
			             dX_neg, dY_max, dZ_pos, dU_neg, dV_neg,
			             dX_neg, dY_max, dZ_neg, dU_neg, dV_pos,
			             dX_min, dY_max, dZ_neg, dU_min, dV_pos,
			             dX_min, dY_max, dZ_pos, dU_min, dV_neg);
			
			addBakedQuad(quads, spriteBlock, color,
			             dX_max, dY_max, dZ_pos, dU_max, dV_neg,
			             dX_max, dY_max, dZ_neg, dU_max, dV_pos,
			             dX_pos, dY_max, dZ_neg, dU_pos, dV_pos,
			             dX_pos, dY_max, dZ_pos, dU_pos, dV_neg);
		} else {
			
			// center cube
			if (!canConnectY_neg) {
				addBakedQuad(quads, spriteBlock, color,
				             dX_neg, dY_neg, dZ_neg, dU_neg, dV_pos,
				             dX_pos, dY_neg, dZ_neg, dU_neg, dV_neg,
				             dX_pos, dY_neg, dZ_pos, dU_pos, dV_neg,
				             dX_neg, dY_neg, dZ_pos, dU_pos, dV_pos);
			}
			if (!canConnectY_pos) {
				addBakedQuad(quads, spriteBlock, color,
				             dX_neg, dY_pos, dZ_pos, dU_pos, dV_pos,
				             dX_pos, dY_pos, dZ_pos, dU_pos, dV_neg,
				             dX_pos, dY_pos, dZ_neg, dU_neg, dV_neg,
				             dX_neg, dY_pos, dZ_neg, dU_neg, dV_pos);
			}
			if (!canConnectZ_neg) {
				addBakedQuad(quads, spriteBlock, color,
				             dX_neg, dY_neg, dZ_neg, dU_neg, dV_pos,
				             dX_neg, dY_pos, dZ_neg, dU_neg, dV_neg,
				             dX_pos, dY_pos, dZ_neg, dU_pos, dV_neg,
				             dX_pos, dY_neg, dZ_neg, dU_pos, dV_pos);
			}
			if (!canConnectZ_pos) {
				addBakedQuad(quads, spriteBlock, color,
				             dX_pos, dY_neg, dZ_pos, dU_pos, dV_pos,
				             dX_pos, dY_pos, dZ_pos, dU_pos, dV_neg,
				             dX_neg, dY_pos, dZ_pos, dU_neg, dV_neg,
				             dX_neg, dY_neg, dZ_pos, dU_neg, dV_pos);
			}
			if (!canConnectX_neg) {
				addBakedQuad(quads, spriteBlock, color,
				             dX_neg, dY_neg, dZ_neg, dU_neg, dV_pos,
				             dX_neg, dY_neg, dZ_pos, dU_neg, dV_neg,
				             dX_neg, dY_pos, dZ_pos, dU_pos, dV_neg,
				             dX_neg, dY_pos, dZ_neg, dU_pos, dV_pos);
			}
			if (!canConnectX_pos) {
				addBakedQuad(quads, spriteBlock, color,
				             dX_pos, dY_pos, dZ_neg, dU_pos, dV_pos,
				             dX_pos, dY_pos, dZ_pos, dU_pos, dV_neg,
				             dX_pos, dY_neg, dZ_pos, dU_neg, dV_neg,
				             dX_pos, dY_neg, dZ_neg, dU_neg, dV_pos);
			}
		}
		
		return quads;
	}
	
	@Override
	public boolean isAmbientOcclusion() {
		return false;
	}
	
	@Override
	public boolean isGui3d() {
		return true;
	}
	
	@Override
	public boolean isBuiltInRenderer() {
		return false;
	}
}