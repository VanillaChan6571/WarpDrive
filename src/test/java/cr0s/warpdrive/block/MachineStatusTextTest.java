package cr0s.warpdrive.block;

import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MachineStatusTextTest {

	@Test
	public void energyStatusUsesRgbHeaderAndFormattedValues() {
		final IFormattableTextComponent status = MachineStatusText.energy(
			new StringTextComponent("Laser Medium Basic"), 10_000, 30_000);

		assertEquals(MachineStatusText.COLOR_HEADER,
			status.getSiblings().get(0).getStyle().getColor().getValue());
		final TranslationTextComponent charge =
			(TranslationTextComponent) status.getSiblings().get(3);
		assertEquals("warpdrive.status.charge", charge.getKey());
		assertValue(charge.getArgs()[0], "10,000");
		assertValue(charge.getArgs()[1], "30,000");
	}

	@Test
	public void treeFarmStatusColorsStateWarningsAndHarvestCount() {
		final IFormattableTextComponent status = MachineStatusText.laserTreeFarm(
			new StringTextComponent("Laser Tree Farm"),
			"warpdrive.laser_tree_farm.status_line.warming_up", true,
			0, 10_000, 7);

		assertEquals(MachineStatusText.COLOR_CORRECT,
			status.getSiblings().get(2).getStyle().getColor().getValue());
		assertEquals(MachineStatusText.COLOR_WARNING,
			status.getSiblings().get(3).getStyle().getColor().getValue());
		final TranslationTextComponent harvested =
			(TranslationTextComponent) status.getSiblings().get(7);
		assertEquals("warpdrive.status.harvested", harvested.getKey());
		assertValue(harvested.getArgs()[0], "7");
	}

	private static void assertValue(final Object value, final String expectedText) {
		final ITextComponent component = (ITextComponent) value;
		assertEquals(expectedText, component.getString());
		assertEquals(MachineStatusText.COLOR_VALUE,
			component.getStyle().getColor().getValue());
	}
}
