package pl.bgadzala.android.dictaphone;

import android.util.Log;

public class Logger {

	private static final String TAG = "Recorder";

	public static void info(String message) {
		Log.i(TAG, message);
	}

	public static void debug(String message) {
		Log.d(TAG, message);
	}

	public static void error(String message) {
		Log.e(TAG, message);
	}

	public static void error(String message, Throwable error) {
		Log.e(TAG, message, error);
	}

}
