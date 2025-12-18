package cr0s.warpdrive.config.structures;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Random;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.gen.feature.WorldGenerator;

/**
 * @author LemADEC
 *
 */
public abstract class AbstractStructureInstance extends WorldGenerator {
	
	protected AbstractStructure structure;
	protected HashMap<String,Double> variables = new HashMap<>();
	
	public AbstractStructureInstance(final AbstractStructure structure, final Random random) {
		this.structure = structure;
		
		// evaluate variables
		for (final Entry<String, String> entry : structure.variables.entrySet()) {
			final double value;
			String stringValue = entry.getValue();
			try {
				if (stringValue.contains(",")) {
					final String[] values = stringValue.split(",");
					stringValue = values[random.nextInt(values.length)];
				}
				value = Double.parseDouble(entry.getValue());
			} catch (final NumberFormatException exception) {
				throw new RuntimeException(String.format("Invalid expression '%s'%s for variable %s in deployable structure %s: a numeric value is expected. Check the related XML configuration file...",
				                                         entry.getValue(),
				                                         (stringValue.equalsIgnoreCase(entry.getValue()) ? "" : " in '" + entry.getValue() + "'"),
				                                         entry.getKey(),
				                                         structure.name));
			}
			
			variables.put(entry.getKey(), value);
		}
	}
	
	protected String evaluate(final String valueOrExpression) {
		if (!valueOrExpression.contains("%")) {
			return valueOrExpression;
		}
		String result = valueOrExpression;
		for (final Entry<String, Double> variable : variables.entrySet()) {
			result = result.replaceAll(variable.getKey(), "" + variable.getValue());
		}
		return result;
	}
	
	public AbstractStructureInstance(final NBTTagCompound tagCompound) {
		// get structure
		final String groupStructure = tagCompound.getString("group");
		final String nameStructure = tagCompound.getString("name");
		structure = StructureManager.getStructure(null, groupStructure, nameStructure);
		
		// get variables values
		final NBTTagCompound tagVariables = tagCompound.getCompoundTag("variables");
		for (final String key : tagVariables.getKeySet()) {
			final double value = tagVariables.getDouble(key);
			variables.put(key, value);
		}
	}
	
	public NBTTagCompound writeToNBT(@Nonnull final NBTTagCompound tagCompound) {
		tagCompound.setString("group", structure.group);
		tagCompound.setString("name", structure.name);
		
		if (!variables.isEmpty()) {
			final NBTTagCompound tagVariables = new NBTTagCompound();
			for (final Entry<String, Double> entry : variables.entrySet()) {
				tagVariables.setDouble(entry.getKey(), entry.getValue());
			}
			tagCompound.setTag("variables", tagVariables);
		}
		
		return tagCompound;
	}
}
