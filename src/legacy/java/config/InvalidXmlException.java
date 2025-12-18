package cr0s.warpdrive.config;

public class InvalidXmlException extends Exception {

	public InvalidXmlException() {
		super("An unknown XML error occurred");
	}

	public InvalidXmlException(final String message) {
		super(message);
	}

	public InvalidXmlException(final Throwable cause) {
		super(cause);
	}

	public InvalidXmlException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public InvalidXmlException(final String message, final Throwable cause, final boolean enableSuppression, final boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
