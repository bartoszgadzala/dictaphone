package pl.bgadzala.android.dictaphone.exception;

public class StorageFullException extends DictaphoneException {

	/** UID. */
	private static final long serialVersionUID = 1L;

	public StorageFullException(String message) {
		super(message);
	}

}
