package pl.bgadzala.android.dictaphone.exception;

public class DictaphoneException extends RuntimeException {

	/** UID. */
	private static final long serialVersionUID = 1L;

	public DictaphoneException(String message) {
		this(message, null);
	}

	public DictaphoneException(String message, Throwable cause) {
		super(message, cause);
	}

}
