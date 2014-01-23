package pl.bgadzala.android.dictaphone.exception;

public class DirectoryReadException extends DictaphoneException {

	/** UID. */
	private static final long serialVersionUID = 1L;

	private final String mPath;

	public DirectoryReadException(String message, String path) {
		super(message);
		mPath = path;
	}

	public String getPath() {
		return mPath;
	}

}
