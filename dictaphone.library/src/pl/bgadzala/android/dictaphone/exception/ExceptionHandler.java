package pl.bgadzala.android.dictaphone.exception;

import android.content.Context;
import android.widget.Toast;
import pl.bgadzala.android.dictaphone.Logger;
import pl.bgadzala.android.dictaphone.library.RecordingsLibrary;
import pl.bgadzala.android.dictaphone.library.RecordingsLibrary.DiskUsage;

import java.io.IOException;

public class ExceptionHandler {

    private ExceptionHandler() {
        // NOP
    }

    public static DictaphoneException convert(String techMessage, Exception ex) {
        Throwable exception = ex;
        while (exception != null) {
            if (exception instanceof DictaphoneException) {
                return (DictaphoneException) exception;
            } else if (exception instanceof IOException) {
                DiskUsage diskUsage = RecordingsLibrary.getDiskUsage();
                if (diskUsage.getUsableSpace() == 0L) {
                    return new StorageFullException("There is [" + diskUsage.getUsableSpace() + "] usable bytes from [" + diskUsage.getTotalSpace()
                            + "] bytes total");
                }
            }
            exception = exception.getCause();
        }

        return new DictaphoneException(techMessage, ex);
    }

    public static void handle(String techMessage, String userMessage, Throwable error, Context context) {
        Logger.error(techMessage, error);
        if (context != null) {
            Toast.makeText(context, userMessage, Toast.LENGTH_LONG).show();
        }
    }

    public static void handle(String techMessage, int userMessageId, Throwable error, Context context) {
        Logger.error(techMessage, error);
        if (context != null) {
            Toast.makeText(context, userMessageId, Toast.LENGTH_LONG).show();
        }
    }

}
