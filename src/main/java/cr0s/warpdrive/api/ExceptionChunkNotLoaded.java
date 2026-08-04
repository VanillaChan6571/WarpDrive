package cr0s.warpdrive.api;

/**
 * Thrown when the air simulation reaches into a chunk that is not loaded.
 *
 * Ported from 1.12.2, where it existed to abort a simulation pass rather than force-load the chunk
 * or risk a ConcurrentModificationException. The air state simply resumes when the chunk comes
 * back, so aborting is safe - a room at the edge of loaded space just stops updating until the
 * player returns.
 */
public class ExceptionChunkNotLoaded extends Exception {

	private static final long serialVersionUID = 1L;

	public ExceptionChunkNotLoaded(final String message) {
		super(message);
	}
}
