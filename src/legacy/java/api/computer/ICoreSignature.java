package cr0s.warpdrive.api.computer;

import javax.annotation.Nullable;
import java.util.UUID;

public interface ICoreSignature {
	
	String NAME_TAG = "name";
	String UUID_LEAST_TAG = "uuidLeast";
	String UUID_MOST_TAG = "uuidMost";
	
	// get the unique id
	@Nullable
	UUID getSignatureUUID();
	
	// get the name for Friend-or-Foe
	String getSignatureName();
	
	// update signature, returns false if the latest is immutable
	boolean setSignature(final UUID uuidSignature, final String nameSignature);
}
