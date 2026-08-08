package cr0s.warpdrive.block.detection;

public enum SirenStyle {
	INDUSTRIAL("industrial"),
	MILITARY("military");

	private final String name;

	SirenStyle(final String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
}
