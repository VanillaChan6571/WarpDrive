package cr0s.warpdrive.core;

import javax.annotation.Nonnull;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

public class ClassTransformer implements net.minecraft.launchwrapper.IClassTransformer {
	
	private static final HashMap<String, String> nodeMap = new HashMap<>();
	public static ConcurrentSkipListMap<String, Integer> countClass = new ConcurrentSkipListMap<>();
	public static ConcurrentHashMap<String, Long> sizeClass = new ConcurrentHashMap<>(8192);
	
	private static final String GRAVITY_MANAGER_CLASS = "cr0s/warpdrive/data/GravityManager";
	private static final String CLOAK_MANAGER_CLASS = "cr0s/warpdrive/data/CloakManager";
	private static final String CELESTIAL_OBJECT_MANAGER_CLASS = "cr0s/warpdrive/data/CelestialObjectManager";
	
	private static final boolean debugLog = false;
	private static final String ASM_DUMP_BEFORE = "asm/warpdrive.before";
	private static final String ASM_DUMP_AFTER  = "asm/warpdrive.after";
	private static final String ASM_DUMP_FAILED = "asm/warpdrive.failed";
	
	public ClassTransformer() {
		nodeMap.put("EntityLivingBase.class", "vn");
		nodeMap.put("travel.name", "func_191986_a");
		nodeMap.put("travel.desc", "(FFF)V");
		
		nodeMap.put("EntityItem.class", "acj");
		nodeMap.put("onUpdate.name", "func_70071_h_");
		nodeMap.put("onUpdate.desc", "()V");
		
		nodeMap.put("WorldClient.class", "brz");
		nodeMap.put("invalidateRegionAndSetBlock.name", "func_180503_b");
		nodeMap.put("invalidateRegionAndSetBlock.desc", "(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;)Z");
		nodeMap.put("setBlockState.name", "func_180501_a");
		
		nodeMap.put("NetHandlerPlayClient.class", "brx");
		nodeMap.put("handleUpdateTileEntity.name", "func_147273_a");
		nodeMap.put("handleUpdateTileEntity.desc", "(Lnet/minecraft/network/play/server/SPacketUpdateTileEntity;)V");
		
		nodeMap.put("Chunk.class", "axu");
		nodeMap.put("read.name", "func_186033_a");
		nodeMap.put("read.desc", "(Lnet/minecraft/network/PacketBuffer;IZ)V");
		nodeMap.put("generateHeightMap.name", "func_76590_a");
		nodeMap.put("generateHeightMap.desc", "()V");
		
		nodeMap.put("AdvancementManager.class", "nq");
		nodeMap.put("loadBuiltInAdvancements.name", "func_192777_a");
		nodeMap.put("loadBuiltInAdvancements.desc", "(Ljava/util/Map;)V");
		
		nodeMap.put("ForgeHooks.class", "ForgeHooks");
		nodeMap.put("loadAdvancements.name", "lambda$loadAdvancements$0");
		nodeMap.put("loadAdvancements.desc", "(Lnet/minecraftforge/fml/common/ModContainer;Ljava/util/Map;Lnet/minecraftforge/common/crafting/JsonContext;Ljava/nio/file/Path;Ljava/nio/file/Path;)Ljava/lang/Boolean;");
		
		nodeMap.put("RenderGlobal.class", "buw");
		nodeMap.put("renderWorldBorder.name", "func_180449_a");
		nodeMap.put("renderWorldBorder.desc", "(Lnet/minecraft/entity/Entity;F)V");
		nodeMap.put("getWorldBorder.name", "func_175723_af");
		nodeMap.put("getWorldBorder.desc", "()Lnet/minecraft/world/border/WorldBorder;");
	}
	
	@Override
	public byte[] transform(@Nonnull final String name, @Nonnull final String transformedName, final byte[] bytesOld) {
		if (bytesOld == null) {
			if (debugLog) {
				FMLLoadingPlugin.logger.trace(String.format("bytes is null, transformation cancelled for %s", name));
			}
			return null;
		}
		
		if (debugLog) {
			// FMLLoadingPlugin.logger.info(String.format("Checking %s", name));
			saveClassToFile(ASM_DUMP_BEFORE, transformedName, bytesOld);
		}
		
		final byte[] bytesNew;
		switch (transformedName) {
		case "net.minecraft.entity.EntityLivingBase":
			bytesNew = transformMinecraftEntityLivingBase(bytesOld);
			break;
			
		case "net.minecraft.entity.item.EntityItem":
			bytesNew = transformMinecraftEntityItem(bytesOld);
			break;
			
		case "com.creativemd.itemphysic.physics.ServerPhysic":
			bytesNew = transformItemPhysicEntityItem(bytesOld);
			break;
			
		case "micdoodle8.mods.galacticraft.core.TransformerHooks":
			bytesNew = transformGalacticraftTransformerHooks(bytesOld);
			break;
			
		case "net.minecraft.client.multiplayer.WorldClient":
			bytesNew = transformMinecraftWorldClient(bytesOld);
			break;
			
		case "net.minecraft.client.network.NetHandlerPlayClient":
			bytesNew = transformMinecraftNetHandlerPlayClient(bytesOld);
			break;
			
		case "net.minecraft.world.chunk.Chunk":
			bytesNew = transformMinecraftChunk(bytesOld);
			break;
			
		case "net.minecraft.advancements.AdvancementManager":
			bytesNew = transformMinecraftAdvancementManager(bytesOld);
			break;
			
		case "net.minecraftforge.common.ForgeHooks":
			bytesNew = transformMinecraftForgeHooks(bytesOld);
			break;
			
		case "net.minecraft.client.renderer.RenderGlobal":
			bytesNew = transformMinecraftRenderGlobal(bytesOld);
			break;
			
		default:
			bytesNew = null;
			break;
		}
		
		try {
			collectClientValidation(transformedName, bytesOld);
		} catch (final Exception exception) {
			// nop
		}
		
		if (bytesNew == null) {// ignored that class
			return bytesOld;
		}
		if (bytesNew == bytesOld) {// transformation failed
			saveClassToFile(ASM_DUMP_FAILED, transformedName, bytesOld);
			return bytesOld;
		}
		
		// all good
		saveClassToFile(ASM_DUMP_AFTER, transformedName, bytesNew);
		return bytesNew;
	}
	
	private static void collectClientValidation(@Nonnull final String transformedName, @Nonnull final byte[] bytes) {
		final String[] names = transformedName.split("[.$]");
		final String shortName = names[0] + "." + (names.length > 1 ? names[1] : "");
		Integer count = countClass.get(shortName);
		Long size = sizeClass.get(shortName);
		if (count == null) {
			count = 0;
			size = 0L;
		}
		countClass.put(shortName, count + 1);
		sizeClass.put(shortName, size + bytes.length);
	}
	
	@Nonnull
	public static String getClientValidation() {
		final StringBuilder result = new StringBuilder().append(new Date().toString());
		for (final String key : countClass.keySet()) {
			result.append("\n").append(key)
			      .append("\t").append(countClass.get(key))
			      .append("\t").append(sizeClass.get(key));
		}
		return result.toString();
	}
	
	private byte[] transformMinecraftEntityLivingBase(@Nonnull final byte[] bytes) {
		final ClassNode classNode = new ClassNode();
		final ClassReader classReader = new ClassReader(bytes);
		classReader.accept(classNode, 0);
		
		final int countExpected = 2;
		int countTransformed = 0;
		for (final MethodNode methodNode : classNode.methods) {
			// if (debugLog) { FMLLoadingPlugin.logger.info(String.format("- Method %s %s", methodNode.name, methodNode.desc)); }
			
			if ( (methodNode.name.equals(nodeMap.get("travel.name")) || methodNode.name.equals("travel"))
			  && methodNode.desc.equals(nodeMap.get("travel.desc")) ) {
				FMLLoadingPlugin.logger.debug(String.format("Found method to transform: %s %s %s",
				                                            classNode.name, methodNode.name, methodNode.desc ));
				
				int indexInstruction = 0;
				
				while (indexInstruction < methodNode.instructions.size()) {
					final AbstractInsnNode abstractNode = methodNode.instructions.get(indexInstruction);
					if (debugLog) { decompile(abstractNode); }
					
					if (abstractNode instanceof LdcInsnNode) {
						final LdcInsnNode nodeAt = (LdcInsnNode) abstractNode;
						
						if (nodeAt.cst.equals(-0.080000000000000002D)) {
							// Elytra motion is cancelling gravity, so we adjust it to ours
							// change    this.motionY += -0.08D + (double)f4 * 0.06D;
							// into      this.motionY += GRAVITY_MANAGER_CLASS.getNegGravityForEntity(this) + (double)f4 * 0.06D;
							final VarInsnNode beforeNode = new VarInsnNode(Opcodes.ALOAD, 0);
							final MethodInsnNode overwriteNode = new MethodInsnNode(
									Opcodes.INVOKESTATIC,
									GRAVITY_MANAGER_CLASS,
									"getNegGravityForEntity",
									"(Lnet/minecraft/entity/Entity;)D",
									false);
							methodNode.instructions.insertBefore(nodeAt, beforeNode);
							methodNode.instructions.set(nodeAt, overwriteNode);
							if (debugLog) { FMLLoadingPlugin.logger.info(String.format("Injecting into %s.%s %s", classNode.name, methodNode.name, methodNode.desc)); }
							countTransformed++;
							
						} else if (nodeAt.cst.equals(0.080000000000000002D)) {
							// Entity Y motion is affected by earth gravity, so we adjust it to ours
							// change    this.motionY -= 0.08D;
							// into      this.motionY -= GRAVITY_MANAGER_CLASS.getGravityForEntity(this);
							// note: several mods are doing similar change; to increase compatibility, we tolerate the change and adjust in their callback
							final VarInsnNode beforeNode = new VarInsnNode(Opcodes.ALOAD, 0);
							final MethodInsnNode overwriteNode = new MethodInsnNode(
									Opcodes.INVOKESTATIC,
									GRAVITY_MANAGER_CLASS,
									"getGravityForEntity",
									"(Lnet/minecraft/entity/Entity;)D",
									false);
							methodNode.instructions.insertBefore(nodeAt, beforeNode);
							methodNode.instructions.set(nodeAt, overwriteNode);
							if (debugLog) { FMLLoadingPlugin.logger.info(String.format("Injecting into %s.%s %s", classNode.name, methodNode.name, methodNode.desc)); }
							countTransformed++;
						}
					}
					
					indexInstruction++;
				}
			}
		}
		
		if (countTransformed != countExpected) {
			FMLLoadingPlugin.logger.error(String.format("Transformation failed for %s (%d/%d), aborting...", classNode.name, countTransformed, countExpected));
			return bytes;
		}
		
		final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS); // | ClassWriter.COMPUTE_FRAMES);
		classNode.accept(writer);
		final byte[] bytesNew = writer.toByteArray();
		FMLLoadingPlugin.logger.info(String.format("Successful injection in %s", classNode.name));
		return bytesNew;
	}
	
	private byte[] transformMinecraftEntityItem(@Nonnull final byte[] bytes) {
		final ClassNode classNode = new ClassNode();
		final ClassReader classReader = new ClassReader(bytes);
		classReader.accept(classNode, 0);
		
		final int countExpected = 2;
		int countTransformed = 0;
		for (final MethodNode methodNode : classNode.methods) {
			// if (debugLog) { FMLLoadingPlugin.logger.info(String.format("- Method %s %s", methodNode.name, methodNode.desc)); }
			
			if ( (methodNode.name.equals(nodeMap.get("onUpdate.name")) || methodNode.name.equals("onUpdate"))
			  && methodNode.desc.equals(nodeMap.get("onUpdate.desc")) ) {
				FMLLoadingPlugin.logger.debug(String.format("Found method to transform: %s %s %s",
				                                            classNode.name, methodNode.name, methodNode.desc ));
				
				int indexInstruction = 0;
				
				while (indexInstruction < methodNode.instructions.size()) {
					final AbstractInsnNode abstractNode = methodNode.instructions.get(indexInstruction);
					if (debugLog) { decompile(abstractNode); }
					
					if (abstractNode instanceof LdcInsnNode) {
						final LdcInsnNode nodeAt = (LdcInsnNode) abstractNode;
						
						if (nodeAt.cst.equals(0.039999999105930328D)) {
							final VarInsnNode beforeNode = new VarInsnNode(Opcodes.ALOAD, 0);
							final MethodInsnNode overwriteNode = new MethodInsnNode(
									Opcodes.INVOKESTATIC,
									GRAVITY_MANAGER_CLASS,
									"getItemGravity",
									"(L" + "net/minecraft/entity/item/EntityItem" + ";)D",
									false);
							methodNode.instructions.insertBefore(nodeAt, beforeNode);
							methodNode.instructions.set(nodeAt, overwriteNode);
							if (debugLog) { FMLLoadingPlugin.logger.info(String.format("Injecting into %s.%s %s", classNode.name, methodNode.name, methodNode.desc)); }
							countTransformed++;
						}
						
						if (nodeAt.cst.equals(0.98000001907348633D)) {
							final VarInsnNode beforeNode = new VarInsnNode(Opcodes.ALOAD, 0);
							final MethodInsnNode overwriteNode = new MethodInsnNode(
									Opcodes.INVOKESTATIC,
									GRAVITY_MANAGER_CLASS,
									"getItemGravity2",
									"(L" + "net/minecraft/entity/item/EntityItem" + ";)D",
									false);
							methodNode.instructions.insertBefore(nodeAt, beforeNode);
							methodNode.instructions.set(nodeAt, overwriteNode);
							if (debugLog) { FMLLoadingPlugin.logger.info(String.format("Injecting into %s.%s %s", classNode.name, methodNode.name, methodNode.desc)); }
							countTransformed++;
						}
						
					} else if (abstractNode instanceof MethodInsnNode && (abstractNode.getOpcode() == Opcodes.INVOKESTATIC)) {
						final MethodInsnNode methodInsnNode = (MethodInsnNode) abstractNode;
						if (methodInsnNode.owner.equals("zmaster587/advancedRocketry/util/GravityHandler")) {
							methodInsnNode.owner = GRAVITY_MANAGER_CLASS;
							methodInsnNode.name = "applyEntityItemGravity";
							methodInsnNode.desc = "(L" + "net/minecraft/entity/item/EntityItem" + ";)V";
							if (debugLog) { FMLLoadingPlugin.logger.info(String.format("Rerouting into %s.%s %s", classNode.name, methodNode.name, methodNode.desc)); }
							countTransformed++;
						}
					}
					
					indexInstruction++;
				}
			}
		}
		
		if (countTransformed != countExpected) {
			FMLLoadingPlugin.logger.error(String.format("Transformation failed for %s (%d/%d), aborting...", classNode.name, countTransformed, countExpected));
			return bytes;
		}
		
		final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS); // | ClassWriter.COMPUTE_FRAMES);
		classNode.accept(writer);
		final byte[] bytesNew = writer.toByteArray();
		FMLLoadingPlugin.logger.info(String.format("Successful injection in %s", classNode.name));
		return bytesNew;
	}
	
	private byte[] transformItemPhysicEntityItem(@Nonnull final byte[] bytes) {
		final ClassNode classNode = new ClassNode();
		final ClassReader classReader = new ClassReader(bytes);
		classReader.accept(classNode, 0);
		
		final int countExpected = 2;
		int countTransformed = 0;
		for (final MethodNode methodNode : classNode.methods) {
			// if (debugLog) { FMLLoadingPlugin.logger.info(String.format("- Method %s %s", methodNode.name, methodNode.desc)); }
			
			if ( (methodNode.name.equals("update"))
			  && methodNode.desc.equals("(Lnet/minecraft/entity/item/EntityItem;)V") ) {
				FMLLoadingPlugin.logger.debug(String.format("Found method to transform: %s %s %s",
				                                            classNode.name, methodNode.name, methodNode.desc ));
				
				int indexInstruction = 0;
				
				while (indexInstruction < methodNode.instructions.size()) {
					final AbstractInsnNode abstractNode = methodNode.instructions.get(indexInstruction);
					if (debugLog) { decompile(abstractNode); }
					
					if (abstractNode instanceof LdcInsnNode) {
						final LdcInsnNode nodeAt = (LdcInsnNode) abstractNode;
						
						if (nodeAt.cst.equals(0.04D)) {
							final VarInsnNode beforeNode = new VarInsnNode(Opcodes.ALOAD, 0);
							final MethodInsnNode overwriteNode = new MethodInsnNode(
									Opcodes.INVOKESTATIC,
									GRAVITY_MANAGER_CLASS,
									"getItemGravity",
									"(L" + "net/minecraft/entity/item/EntityItem" + ";)D",
									false);
							methodNode.instructions.insertBefore(nodeAt, beforeNode);
							methodNode.instructions.set(nodeAt, overwriteNode);
							if (debugLog) { FMLLoadingPlugin.logger.info(String.format("Injecting into %s.%s %s", classNode.name, methodNode.name, methodNode.desc)); }
							countTransformed++;
						}
						
						if (nodeAt.cst.equals(0.98D)) {
							final VarInsnNode beforeNode = new VarInsnNode(Opcodes.ALOAD, 0);
							final MethodInsnNode overwriteNode = new MethodInsnNode(
									Opcodes.INVOKESTATIC,
									GRAVITY_MANAGER_CLASS,
									"getItemGravity2",
									"(L" + "net/minecraft/entity/item/EntityItem" + ";)D",
									false);
							methodNode.instructions.insertBefore(nodeAt, beforeNode);
							methodNode.instructions.set(nodeAt, overwriteNode);
							if (debugLog) { FMLLoadingPlugin.logger.info(String.format("Injecting into %s.%s %s", classNode.name, methodNode.name, methodNode.desc)); }
							countTransformed++;
						}
					}
					
					indexInstruction++;
				}
			}
		}
		
		if (countTransformed != countExpected) {
			FMLLoadingPlugin.logger.error(String.format("Transformation failed for %s (%d/%d), aborting...", classNode.name, countTransformed, countExpected));
			return bytes;
		}
		
		final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS); // | ClassWriter.COMPUTE_FRAMES);
		classNode.accept(writer);
		final byte[] bytesNew = writer.toByteArray();
		FMLLoadingPlugin.logger.info(String.format("Successful injection in %s", classNode.name));
		return bytesNew;
	}
	
	private byte[] transformGalacticraftTransformerHooks(@Nonnull final byte[] bytes) {
		final ClassNode classNode = new ClassNode();
		final ClassReader classReader = new ClassReader(bytes);
		classReader.accept(classNode, 0);
		
		final int countExpected = 7; // 5 + 2 + 0
		int countTransformed = 0;
		for (final MethodNode methodNode : classNode.methods) {
			// if (debugLog) { FMLLoadingPlugin.logger.info(String.format("- Method %s %s", methodNode.name, methodNode.desc)); }

			// Entities gravity
			if ( (methodNode.name.equals("getGravityForEntity"))
			  && methodNode.desc.equals("(Lnet/minecraft/entity/Entity;)D") ) {
				FMLLoadingPlugin.logger.debug(String.format("Found method to transform: %s %s %s",
				                                            classNode.name, methodNode.name, methodNode.desc ));
				
				int indexInstruction = 0;
				
				while (indexInstruction < methodNode.instructions.size()) {
					final AbstractInsnNode abstractNode = methodNode.instructions.get(indexInstruction);
					if (debugLog) { decompile(abstractNode); }
					
					if (abstractNode instanceof LdcInsnNode) {
						final LdcInsnNode nodeAt = (LdcInsnNode) abstractNode;
						
						if (nodeAt.cst.equals(0.08D)) {
							// Galacticraft applies armor factor based on its dimension over the vanilla gravity. We only change the gravity constant to match ours.
							// This maintain compatibility with their armors and entities for the most part while allowing some customization.
							// change    return 0.08D; (twice)
							// into      return GRAVITY_MANAGER_CLASS.getGravityForEntity(this);
							// change    return 0.08D - (customProvider.getGravity() * armorModLowGrav / 100.0F);
							// into      return GRAVITY_MANAGER_CLASS.getGravityForEntity(this) - (customProvider.getGravity() * armorModLowGrav / 100.0F);
							// change    return 0.08D - (customProvider.getGravity() * armorModHighGrav / 100.0F);
							// into      return GRAVITY_MANAGER_CLASS.getGravityForEntity(this) - (customProvider.getGravity() * armorModHighGrav / 100.0F);
							// change    return 0.08D - customProvider.getGravity();
							// into      return GRAVITY_MANAGER_CLASS.getGravityForEntity(this) - customProvider.getGravity();
							final VarInsnNode beforeNode = new VarInsnNode(Opcodes.ALOAD, 0);
							final MethodInsnNode overwriteNode = new MethodInsnNode(
									Opcodes.INVOKESTATIC,
									GRAVITY_MANAGER_CLASS,
									"getGravityForEntity",
									"(Lnet/minecraft/entity/Entity;)D",
									false);
							methodNode.instructions.insertBefore(nodeAt, beforeNode);
							methodNode.instructions.set(nodeAt, overwriteNode);
							if (debugLog) { FMLLoadingPlugin.logger.info(String.format("Injecting into %s.%s %s", classNode.name, methodNode.name, methodNode.desc)); }
							countTransformed++;
						}
					}
					
					indexInstruction++;
				}
			}
			
			// Items gravity
			if ( (methodNode.name.equals("getItemGravity"))
			  && methodNode.desc.equals("(Lnet/minecraft/entity/item/EntityItem;)D") ) {
				FMLLoadingPlugin.logger.debug(String.format("Found method to transform: %s %s %s",
				                                            classNode.name, methodNode.name, methodNode.desc ));
				
				int indexInstruction = 0;
				
				while (indexInstruction < methodNode.instructions.size()) {
					final AbstractInsnNode abstractNode = methodNode.instructions.get(indexInstruction);
					if (debugLog) { decompile(abstractNode); }
					
					if (abstractNode instanceof LdcInsnNode) {
						final LdcInsnNode nodeAt = (LdcInsnNode) abstractNode;
						
						if (nodeAt.cst.equals(0.03999999910593033D)) {
							final VarInsnNode beforeNode = new VarInsnNode(Opcodes.ALOAD, 0);
							final MethodInsnNode overwriteNode = new MethodInsnNode(
									Opcodes.INVOKESTATIC,
									GRAVITY_MANAGER_CLASS,
									"getItemGravity",
									"(L" + "net/minecraft/entity/item/EntityItem" + ";)D",
									false);
							methodNode.instructions.insertBefore(nodeAt, beforeNode);
							methodNode.instructions.set(nodeAt, overwriteNode);
							if (debugLog) { FMLLoadingPlugin.logger.info(String.format("Injecting into %s.%s %s", classNode.name, methodNode.name, methodNode.desc)); }
							countTransformed++;
						}
						/*
						if (nodeAt.cst.equals(Double.valueOf(0.98D))) {
							final VarInsnNode beforeNode = new VarInsnNode(Opcodes.ALOAD, 0);
							final MethodInsnNode overwriteNode = new MethodInsnNode(
									Opcodes.INVOKESTATIC,
									GRAVITY_MANAGER_CLASS,
									"getItemGravity2",
									"(L" + "net/minecraft/entity/item/EntityItem" + ";)D",
									false);
							methodNode.instructions.insertBefore(nodeAt, beforeNode);
							methodNode.instructions.set(nodeAt, overwriteNode);
							if (debugLog) { FMLLoadingPlugin.logger.info(String.format("Injecting into %s.%s %s", classNode.name, methodNode.name, methodNode.desc)); }
							countTransformed++;
						}
						/**/
					}
					
					indexInstruction++;
				}
			}
		}
		
		if (countTransformed != countExpected) {
			FMLLoadingPlugin.logger.error(String.format("Transformation failed for %s (%d/%d), aborting...", classNode.name, countTransformed, countExpected));
			return bytes;
		}
		
		final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS); // | ClassWriter.COMPUTE_FRAMES);
		classNode.accept(writer);
		final byte[] bytesNew = writer.toByteArray();
		FMLLoadingPlugin.logger.info(String.format("Successful injection in %s", classNode.name));
		return bytesNew;
	}
	
	private byte[] transformMinecraftWorldClient(@Nonnull final byte[] bytes) {
		final ClassNode classNode = new ClassNode();
		final ClassReader classReader = new ClassReader(bytes);
		classReader.accept(classNode, 0);
		
		final int countExpected = 1;
		int countTransformed = 0;
		for (final MethodNode methodNode : classNode.methods) {
			// if (debugLog) { FMLLoadingPlugin.logger.info(String.format("- Method %s %s", methodNode.name, methodNode.desc)); }
			
			if ( (methodNode.name.equals(nodeMap.get("invalidateRegionAndSetBlock.name")) || methodNode.name.equals("invalidateRegionAndSetBlock"))
			  && methodNode.desc.equals(nodeMap.get("invalidateRegionAndSetBlock.desc")) ) {
				FMLLoadingPlugin.logger.debug(String.format("Found method to transform: %s %s %s",
				                                            classNode.name, methodNode.name, methodNode.desc ));
				
				int indexInstruction = 0;
				
				while (indexInstruction < methodNode.instructions.size()) {
					final AbstractInsnNode abstractNode = methodNode.instructions.get(indexInstruction);
					if (debugLog) { decompile(abstractNode); }
					
					if (abstractNode instanceof MethodInsnNode) {
						final MethodInsnNode nodeAt = (MethodInsnNode) abstractNode;
						
						if (nodeAt.name.equals(nodeMap.get("setBlockState.name")) || nodeAt.name.equals("setBlockState")) {
							final MethodInsnNode overwriteNode = new MethodInsnNode(
									Opcodes.INVOKESTATIC,
									CLOAK_MANAGER_CLASS,
									"WorldClient_invalidateRegionAndSetBlock_setBlockState",
									"(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;I)Z",
									false);
							methodNode.instructions.set(nodeAt, overwriteNode);
							if (debugLog) { FMLLoadingPlugin.logger.info(String.format("Injecting into %s.%s %s", classNode.name, methodNode.name, methodNode.desc)); }
							countTransformed++;
						}
					}
					
					indexInstruction++;
				}
			}
		}
		
		if (countTransformed != countExpected) {
			FMLLoadingPlugin.logger.error(String.format("Transformation failed for %s (%d/%d), aborting...", classNode.name, countTransformed, countExpected));
			return bytes;
		}
		
		final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS); // | ClassWriter.COMPUTE_FRAMES);
		classNode.accept(writer);
		final byte[] bytesNew = writer.toByteArray();
		FMLLoadingPlugin.logger.info(String.format("Successful injection in %s", classNode.name));
		return bytesNew;
	}
	
	private byte[] transformMinecraftNetHandlerPlayClient(@Nonnull final byte[] bytes) {
		final ClassNode classNode = new ClassNode();
		final ClassReader classReader = new ClassReader(bytes);
		classReader.accept(classNode, 0);
		
		final int countExpected = 1;
		int countTransformed = 0;
		for (final MethodNode methodNode : classNode.methods) {
			if (debugLog) { FMLLoadingPlugin.logger.info(String.format("- Method %s %s", methodNode.name, methodNode.desc)); }
			
			if ( (methodNode.name.equals(nodeMap.get("handleUpdateTileEntity.name")) || methodNode.name.equals("handleUpdateTileEntity"))
			  && methodNode.desc.equals(nodeMap.get("handleUpdateTileEntity.desc")) ) {
				FMLLoadingPlugin.logger.debug(String.format("Found method to transform: %s %s %s",
				                                            classNode.name, methodNode.name, methodNode.desc ));
				
				int indexInstruction = 0;
				
				while (indexInstruction < methodNode.instructions.size()) {
					final AbstractInsnNode abstractNode = methodNode.instructions.get(indexInstruction);
					if (debugLog) { decompile(abstractNode); }
					
					if (abstractNode instanceof LdcInsnNode) {
						final LdcInsnNode nodeAt = (LdcInsnNode) abstractNode;
						
						if ( nodeAt.cst instanceof String
						  && ((String) nodeAt.cst).contains("Received invalid update packet for null tile entity at ") ) {
							final AbstractInsnNode abstractNodeToRemove = methodNode.instructions.get(indexInstruction - 1);
							if ( (abstractNodeToRemove instanceof FieldInsnNode)
							  && ((FieldInsnNode) abstractNodeToRemove).desc.equals("Lorg/apache/logging/log4j/Logger;") ) {
								indexInstruction -= 1;
								removeInstruction(methodNode, indexInstruction);
								removeInstruction(methodNode, indexInstruction);
								removeInstruction(methodNode, indexInstruction);
								removeInstruction(methodNode, indexInstruction);
								removeInstruction(methodNode, indexInstruction);
								removeInstruction(methodNode, indexInstruction);
								removeInstruction(methodNode, indexInstruction);
								indexInstruction--;
								
								if (debugLog) { FMLLoadingPlugin.logger.info(String.format("Injecting into %s.%s %s", classNode.name, methodNode.name, methodNode.desc)); }
								countTransformed++;
							}
						}
					}
					
					indexInstruction++;
				}
			}
		}
		
		if (countTransformed != countExpected) {
			FMLLoadingPlugin.logger.error(String.format("Transformation failed for %s (%d/%d), aborting...", classNode.name, countTransformed, countExpected));
			return bytes;
		}
		
		final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS); // | ClassWriter.COMPUTE_FRAMES);
		classNode.accept(writer);
		final byte[] bytesNew = writer.toByteArray();
		FMLLoadingPlugin.logger.info(String.format("Successful injection in %s", classNode.name));
		return bytesNew;
	}
	
	private byte[] transformMinecraftChunk(@Nonnull final byte[] bytes) {
		final ClassNode classNode = new ClassNode();
		final ClassReader classReader = new ClassReader(bytes);
		classReader.accept(classNode, 0);
		
		// FMLCommonHandler will load NBT classes too early, so we can't use it directly.
		// Furthermore, current thread is "main" when another core mod loaded the Chunk class (see SpongeForge), so we can't use SidedThreadGroup nor thread name reliably.
		// FMLServerHandler is documented as being environment specific, so we can't really use it.
		// DedicatedServer is only available server side, but can't be checked early on (see SpongeForge).
		if (Thread.currentThread().getName().equals("Server thread")) {
			FMLLoadingPlugin.logger.info(String.format("Skipping client-side only transformation for %s",
			                                           classNode.name));
			return bytes;
		}
		
		final int countExpected = 1;
		int countTransformed = 0;
		for (final MethodNode methodNode : classNode.methods) {
			if (debugLog) { FMLLoadingPlugin.logger.info(String.format("- Method %s %s", methodNode.name, methodNode.desc)); }
			
			if ( (methodNode.name.equals(nodeMap.get("read.name")) || methodNode.name.equals("read"))
			  && methodNode.desc.equals(nodeMap.get("read.desc")) ) {
				FMLLoadingPlugin.logger.debug(String.format("Found method to transform: %s %s %s",
				                                            classNode.name, methodNode.name, methodNode.desc ));
				
				int indexInstruction = 0;
				
				while (indexInstruction < methodNode.instructions.size()) {
					final AbstractInsnNode abstractNode = methodNode.instructions.get(indexInstruction);
					if (debugLog) { decompile(abstractNode); }
					
					if (abstractNode instanceof MethodInsnNode) {
						final MethodInsnNode nodeAt = (MethodInsnNode) abstractNode;
						
						if ( (nodeAt.name.equals(nodeMap.get("generateHeightMap.name")) || nodeAt.name.equals("generateHeightMap"))
						  && nodeAt.desc.equals(nodeMap.get("generateHeightMap.desc")) ) {
							final MethodInsnNode insertMethodNode = new MethodInsnNode(
									Opcodes.INVOKESTATIC,
									CLOAK_MANAGER_CLASS,
									"Chunk_read",
									"(Lnet/minecraft/world/chunk/Chunk;)V",
									false);
							methodNode.instructions.insertBefore(nodeAt, insertMethodNode);
							indexInstruction++;
							
							final VarInsnNode insertVarNode = new VarInsnNode(Opcodes.ALOAD, 0);
							methodNode.instructions.insertBefore(nodeAt, insertVarNode);
							indexInstruction++;
							
							if (debugLog) { FMLLoadingPlugin.logger.info(String.format("Injecting into %s.%s %s", classNode.name, methodNode.name, methodNode.desc)); }
							countTransformed++;
						}
					}
					
					indexInstruction++;
				}
			}
		}
		
		if (countTransformed != countExpected) {
			FMLLoadingPlugin.logger.error(String.format("Transformation failed for %s (%d/%d), aborting...", classNode.name, countTransformed, countExpected));
			return bytes;
		}
		
		final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS); // | ClassWriter.COMPUTE_FRAMES);
		classNode.accept(writer);
		final byte[] bytesNew = writer.toByteArray();
		FMLLoadingPlugin.logger.info(String.format("Successful injection in %s", classNode.name));
		return bytesNew;
	}
	
	private byte[] transformMinecraftAdvancementManager(@Nonnull final byte[] bytes) {
		final ClassNode classNode = new ClassNode();
		final ClassReader classReader = new ClassReader(bytes);
		classReader.accept(classNode, 0);
		
		final int countExpected = 1;
		int countTransformed = 0;
		for (final MethodNode methodNode : classNode.methods) {
			// if (debugLog) { FMLLoadingPlugin.logger.info(String.format("- Method %s %s", methodNode.name, methodNode.desc)); }
			
			if ( (methodNode.name.equals(nodeMap.get("loadBuiltInAdvancements.name")) || methodNode.name.equals("loadBuiltInAdvancements"))
			  && methodNode.desc.equals(nodeMap.get("loadBuiltInAdvancements.desc")) ) {
				FMLLoadingPlugin.logger.debug(String.format("Found method to transform: %s %s %s",
				                                            classNode.name, methodNode.name, methodNode.desc ));
				
				int indexInstruction = 0;
				
				while (indexInstruction < methodNode.instructions.size()) {
					final AbstractInsnNode abstractNode = methodNode.instructions.get(indexInstruction);
					if (debugLog) { decompile(abstractNode); }
					
					if (abstractNode instanceof LdcInsnNode) {
						final LdcInsnNode nodeAt = (LdcInsnNode) abstractNode;
						
						if ( nodeAt.cst instanceof String
						  && ((String) nodeAt.cst).contains("Parsing error loading built-in advancement ") ) {
							final AbstractInsnNode abstractNodeToRemove = methodNode.instructions.get(indexInstruction - 4);
							if ( (abstractNodeToRemove instanceof FieldInsnNode)
							  && ((FieldInsnNode) abstractNodeToRemove).desc.equals("Lorg/apache/logging/log4j/Logger;") ) {
								indexInstruction -= 4;
								removeInstruction(methodNode, indexInstruction);
								removeInstruction(methodNode, indexInstruction);
								removeInstruction(methodNode, indexInstruction);
								removeInstruction(methodNode, indexInstruction);
								removeInstruction(methodNode, indexInstruction);
								removeInstruction(methodNode, indexInstruction);
								removeInstruction(methodNode, indexInstruction);
								removeInstruction(methodNode, indexInstruction);
								removeInstruction(methodNode, indexInstruction);
								removeInstruction(methodNode, indexInstruction);
								removeInstruction(methodNode, indexInstruction);
								indexInstruction--;
								
								if (debugLog) { FMLLoadingPlugin.logger.info(String.format("Injecting into %s.%s %s", classNode.name, methodNode.name, methodNode.desc)); }
								countTransformed++;
							}
						}
					}
					
					indexInstruction++;
				}
			}
		}
		
		if (countTransformed != countExpected) {
			FMLLoadingPlugin.logger.error(String.format("Transformation failed for %s (%d/%d), aborting...", classNode.name, countTransformed, countExpected));
			return bytes;
		}
		
		final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS); // | ClassWriter.COMPUTE_FRAMES);
		classNode.accept(writer);
		final byte[] bytesNew = writer.toByteArray();
		FMLLoadingPlugin.logger.info(String.format("Successful injection in %s", classNode.name));
		return bytesNew;
	}
	
	private byte[] transformMinecraftForgeHooks(@Nonnull final byte[] bytes) {
		final ClassNode classNode = new ClassNode();
		final ClassReader classReader = new ClassReader(bytes);
		classReader.accept(classNode, 0);
		
		final int countExpected = 1;
		int countTransformed = 0;
		for (final MethodNode methodNode : classNode.methods) {
			// if (debugLog) { FMLLoadingPlugin.logger.info(String.format("- Method %s %s", methodNode.name, methodNode.desc)); }
			
			if ( (methodNode.name.equals(nodeMap.get("loadAdvancements.name")) || methodNode.name.equals("loadAdvancements"))
			  && methodNode.desc.equals(nodeMap.get("loadAdvancements.desc")) ) {
				FMLLoadingPlugin.logger.debug(String.format("Found method to transform: %s %s %s",
				                                            classNode.name, methodNode.name, methodNode.desc ));
				
				int indexInstruction = 0;
				
				while (indexInstruction < methodNode.instructions.size()) {
					final AbstractInsnNode abstractNode = methodNode.instructions.get(indexInstruction);
					if (debugLog) { decompile(abstractNode); }
					
					if (abstractNode instanceof LdcInsnNode) {
						final LdcInsnNode nodeAt = (LdcInsnNode) abstractNode;
						
						if ( nodeAt.cst instanceof String
						  && ((String) nodeAt.cst).contains("Parsing error loading built-in advancement ") ) {
							final AbstractInsnNode abstractNodeToRemove = methodNode.instructions.get(indexInstruction - 4);
							if ( (abstractNodeToRemove instanceof FieldInsnNode)
							  && ((FieldInsnNode) abstractNodeToRemove).desc.equals("Lorg/apache/logging/log4j/Logger;") ) {
								indexInstruction -= 4;
								removeInstruction(methodNode, indexInstruction);
								removeInstruction(methodNode, indexInstruction);
								removeInstruction(methodNode, indexInstruction);
								removeInstruction(methodNode, indexInstruction);
								removeInstruction(methodNode, indexInstruction);
								removeInstruction(methodNode, indexInstruction);
								removeInstruction(methodNode, indexInstruction);
								removeInstruction(methodNode, indexInstruction);
								removeInstruction(methodNode, indexInstruction);
								removeInstruction(methodNode, indexInstruction);
								removeInstruction(methodNode, indexInstruction);
								indexInstruction--;
								
								if (debugLog) { FMLLoadingPlugin.logger.info(String.format("Injecting into %s.%s %s", classNode.name, methodNode.name, methodNode.desc)); }
								countTransformed++;
							}
						}
					}
					
					indexInstruction++;
				}
			}
		}
		
		if (countTransformed != countExpected) {
			FMLLoadingPlugin.logger.error(String.format("Transformation failed for %s (%d/%d), aborting...", classNode.name, countTransformed, countExpected));
			return bytes;
		}
		
		final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS); // | ClassWriter.COMPUTE_FRAMES);
		classNode.accept(writer);
		final byte[] bytesNew = writer.toByteArray();
		FMLLoadingPlugin.logger.info(String.format("Successful injection in %s", classNode.name));
		return bytesNew;
	}
	
	private byte[] transformMinecraftRenderGlobal(@Nonnull final byte[] bytes) {
		final ClassNode classNode = new ClassNode();
		final ClassReader classReader = new ClassReader(bytes);
		classReader.accept(classNode, 0);
		
		final int countExpected = 1;
		int countTransformed = 0;
		for (final MethodNode methodNode : classNode.methods) {
			// if (debugLog) { FMLLoadingPlugin.logger.info(String.format("- Method %s %s", methodNode.name, methodNode.desc)); }
			
			if ( (methodNode.name.equals(nodeMap.get("renderWorldBorder.name")) || methodNode.name.equals("renderWorldBorder"))
			  && methodNode.desc.equals(nodeMap.get("renderWorldBorder.desc")) ) {
				FMLLoadingPlugin.logger.debug(String.format("Found method to transform: %s %s %s",
				                                            classNode.name, methodNode.name, methodNode.desc ));
				
				int indexInstruction = 0;
				
				while (indexInstruction < methodNode.instructions.size()) {
					final AbstractInsnNode abstractNode = methodNode.instructions.get(indexInstruction);
					if (debugLog) { decompile(abstractNode); }
					
					// change    WorldBorder worldborder = this.field_72769_h.func_175723_af();
					// into      WorldBorder worldborder = CelestiaObjectManager.getWorldBorder(world);
					if (abstractNode instanceof MethodInsnNode) {
						final MethodInsnNode nodeAt = (MethodInsnNode) abstractNode;
						
						if (nodeAt.name.equals(nodeMap.get("getWorldBorder.name")) || nodeAt.name.equals("getWorldBorder")) {
							final MethodInsnNode overwriteNode = new MethodInsnNode(
									Opcodes.INVOKESTATIC,
									CELESTIAL_OBJECT_MANAGER_CLASS,
									"World_getWorldBorder",
									"(Lnet/minecraft/world/World;)Lnet/minecraft/world/border/WorldBorder;",
									false);
							methodNode.instructions.set(nodeAt, overwriteNode);
							if (debugLog) { FMLLoadingPlugin.logger.info(String.format("Injecting into %s.%s %s", classNode.name, methodNode.name, methodNode.desc)); }
							countTransformed++;
						}
					}
					
					indexInstruction++;
				}
			}
		}
		
		if (countTransformed != countExpected) {
			FMLLoadingPlugin.logger.error(String.format("Transformation failed for %s (%d/%d), aborting...", classNode.name, countTransformed, countExpected));
			return bytes;
		}
		
		final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS); // | ClassWriter.COMPUTE_FRAMES);
		classNode.accept(writer);
		final byte[] bytesNew = writer.toByteArray();
		FMLLoadingPlugin.logger.info(String.format("Successful injection in %s", classNode.name));
		return bytesNew;
	}
	
	private void removeInstruction(@Nonnull final MethodNode methodNode, final int indexInstruction) {
		final AbstractInsnNode abstractNodeToRemove = methodNode.instructions.get(indexInstruction);
		if (debugLog) {
			FMLLoadingPlugin.logger.info("Removing instruction:");
			decompile(abstractNodeToRemove);
		}
		methodNode.instructions.remove(abstractNodeToRemove);
	}
	
	private void saveClassToFile(final String path, final String nameClass, final byte[] bytes) {
		try {
			// create folder
			final File fileDir = new File(path);
			if (!fileDir.exists() || !fileDir.isDirectory()) {
				if (!fileDir.mkdirs()) {
					FMLLoadingPlugin.logger.error("Unable to create ASM dump folder, skipping...");
					return;
				}
			}
			
			// sanitize class name
			final String nameClass_clean = nameClass.replace("/", "_").replace("\\", "_").replace(" ", "_");
			
			// save class file
			final File fileClass = new File(fileDir, nameClass_clean + ".class");
			final FileOutputStream fileOutputStream = new FileOutputStream(fileClass);
			final DataOutputStream dataOutputStream = new DataOutputStream(fileOutputStream);
			dataOutputStream.write(bytes);
			dataOutputStream.flush();
			dataOutputStream.close();
		} catch (final Exception exception) {
			exception.printStackTrace();
		}
	}
	
	private static boolean opcodeToString_firstDump = true;
	private static String opcodeToString(final int opcode) {
		final Field[] fields = Opcodes.class.getFields();
		for (final Field field : fields) {
			if (field.getType() == int.class) {
				try {
					if (field.getInt(null) == opcode){
						return field.getName();
					}
				} catch (final Throwable throwable){
					if (opcodeToString_firstDump) {
						throwable.printStackTrace();
						opcodeToString_firstDump = false;
					}
				}
			}
		}
		return String.format("0x%x", opcode);
	}
	
	private static void decompile(final AbstractInsnNode abstractNode) {
		if (abstractNode == null) {
			FMLLoadingPlugin.logger.error(String.format("%20s %-20s %s", "NULL", "NULL", "null node"));
			return;
		}
		
		final String opcode = opcodeToString(abstractNode.getOpcode());
		if (abstractNode instanceof FieldInsnNode) {
			final FieldInsnNode node = (FieldInsnNode) abstractNode;
			FMLLoadingPlugin.logger.info(String.format("%20s %-20s %s %s %s", opcode, "Field", node.owner, node.name, node.desc));
			
		} else if (abstractNode instanceof FrameNode) {
			final FrameNode node = (FrameNode) abstractNode;
			FMLLoadingPlugin.logger.info(String.format("%20s %-20s %d %s %s", opcode, "Frame", node.type, node.local, node.stack));
			
		} else if (abstractNode instanceof IincInsnNode) {
			final IincInsnNode node = (IincInsnNode) abstractNode;
			FMLLoadingPlugin.logger.info(String.format("%20s %-20s var %s %s", opcode, "Instruction", node.var, node.incr));
			
		} else if (abstractNode instanceof InsnNode) {
			// final InsnNode node = (InsnNode) abstractNode;
			FMLLoadingPlugin.logger.info(String.format("%20s %-20s %s", opcode, "Instruction", "-"));
			
		} else if (abstractNode instanceof IntInsnNode) {
			final IntInsnNode node = (IntInsnNode) abstractNode;
			FMLLoadingPlugin.logger.info(String.format("%20s %-20s %s", opcode, "Instruction", node.operand));
			
		} else if (abstractNode instanceof InvokeDynamicInsnNode) {
			final InvokeDynamicInsnNode node = (InvokeDynamicInsnNode) abstractNode;
			FMLLoadingPlugin.logger.info(String.format("%20s %-20s %s %s %s %s", opcode, "Instruction", node.name, node.desc, node.bsm, Arrays.toString(node.bsmArgs)));
			
		} else if (abstractNode instanceof JumpInsnNode) {
			final JumpInsnNode node = (JumpInsnNode) abstractNode;
			FMLLoadingPlugin.logger.info(String.format("%20s %-20s %s", opcode, "Jump", node.label.getLabel()));
			
		} else if (abstractNode instanceof LabelNode) {
			final LabelNode node = (LabelNode) abstractNode;
			FMLLoadingPlugin.logger.info(String.format("%20s %-20s %s", opcode, "Label", node.getLabel()));
			
		} else if (abstractNode instanceof LdcInsnNode) {
			final LdcInsnNode node = (LdcInsnNode) abstractNode;
			FMLLoadingPlugin.logger.info(String.format("%20s %-20s %s", opcode, "Load", node.cst));
			
		} else if (abstractNode instanceof LineNumberNode) {
			final LineNumberNode node = (LineNumberNode) abstractNode;
			FMLLoadingPlugin.logger.info(String.format("%20s %-20s %s", opcode, "Line", node.line));
			
		} else if (abstractNode instanceof LookupSwitchInsnNode) {
			final LookupSwitchInsnNode node = (LookupSwitchInsnNode) abstractNode;
			FMLLoadingPlugin.logger.info(String.format("%20s %-20s %s %s %s", opcode, "Instruction", node.dflt, node.keys, node.labels));
			
		} else if (abstractNode instanceof MethodInsnNode) {
			final MethodInsnNode node = (MethodInsnNode) abstractNode;
			FMLLoadingPlugin.logger.info(String.format("%20s %-20s %s %s %s %s", opcode, "Method", node.owner, node.name, node.desc, node.itf));
			
		} else if (abstractNode instanceof MultiANewArrayInsnNode) {
			final MultiANewArrayInsnNode node = (MultiANewArrayInsnNode) abstractNode;
			FMLLoadingPlugin.logger.info(String.format("%20s %-20s %s %s", opcode, "Instruction", node.desc, node.dims));
			
		} else if (abstractNode instanceof TableSwitchInsnNode) {
			final TableSwitchInsnNode node = (TableSwitchInsnNode) abstractNode;
			FMLLoadingPlugin.logger.info(String.format("%20s %-20s %s %s %s %s", opcode, "Instruction", node.dflt, node.min, node.max, node.labels));
			
		} else if (abstractNode instanceof TypeInsnNode) {
			final TypeInsnNode node = (TypeInsnNode) abstractNode;
			FMLLoadingPlugin.logger.info(String.format("%20s %-20s %s", opcode, "Typed instruction", node.desc));
			
		} else if (abstractNode instanceof VarInsnNode) {
			final VarInsnNode node = (VarInsnNode) abstractNode;
			FMLLoadingPlugin.logger.info(String.format("%20s %-20s %s", opcode, "Var", node.var));
			
		} else {
			FMLLoadingPlugin.logger.info(String.format("%20s %-20s %s %s %s", opcode, "Instruction", abstractNode.getOpcode(), abstractNode.getType(), abstractNode));
		}
	}
}
