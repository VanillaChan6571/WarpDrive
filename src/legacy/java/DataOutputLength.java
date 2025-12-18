package cr0s.warpdrive.data;

import javax.annotation.Nonnull;
import java.io.DataOutput;

/*
 Simple DataOutput implementation to estimate data size in bytes.
 Size is capped at Integer.MAX_VALUE in case of overflow.
 */
class DataOutputLength implements DataOutput {
	
	protected int countBytes;
	
	public DataOutputLength() {
		super();
	}
	
	private void increaseCount(final int value) {
		int countBytes_new = countBytes + value;
		if (countBytes_new < 0) {
			countBytes_new = Integer.MAX_VALUE;
		}
		countBytes = countBytes_new;
	}
	
	@Override
	public void write(final int b) {
		increaseCount(1);
	}
	
	@Override
	public void write(@Nonnull final byte[] bytes) {
		increaseCount(bytes.length);
	}
	
	@Override
	public void write(@Nonnull final byte bytes[], final int offset, final int len) {
		increaseCount(len);
	}
	
	@Override
	public final void writeBoolean(final boolean value) {
		increaseCount(1);
	}
	
	@Override
	public final void writeByte(final int value) {
		increaseCount(1);
	}
	
	@Override
	public final void writeShort(final int value) {
		increaseCount(2);
	}
	
	@Override
	public final void writeChar(final int value) {
		increaseCount(2);
	}
	
	@Override
	public final void writeInt(final int value) {
		increaseCount(4);
	}
	
	@Override
	public final void writeLong(final long value) {
		increaseCount(8);
	}
	
	@Override
	public final void writeFloat(final float value) {
		writeInt(Float.floatToIntBits(value));
	}
	
	@Override
	public final void writeDouble(final double value) {
		writeLong(Double.doubleToLongBits(value));
	}
	
	@Override
	public final void writeBytes(final String value) {
		final int length = value.length();
		increaseCount(length);
	}
	
	@Override
	public final void writeChars(final String value) {
		final int length = value.length();
		increaseCount(length * 2);
	}
	
	@Override
	public final void writeUTF(final String string) {
		final int length = Math.round(string.length() * 1.5F);
		increaseCount(length);
	}
	
	public final int getLength() {
		return countBytes;
	}
}
