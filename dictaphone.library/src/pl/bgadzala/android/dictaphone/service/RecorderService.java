package pl.bgadzala.android.dictaphone.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.*;
import android.os.Process;
import android.provider.MediaStore;
import android.widget.Toast;
import pl.bgadzala.android.dictaphone.Logger;
import pl.bgadzala.android.dictaphone.exception.*;
import pl.bgadzala.android.dictaphone.lib.R;
import pl.bgadzala.android.dictaphone.library.Recording;
import pl.bgadzala.android.dictaphone.library.RecordingsLibrary;
import pl.bgadzala.android.dictaphone.library.RecordingsLibrary.DiskUsage;
import pl.bgadzala.arl.Format;
import pl.bgadzala.arl.Recorder;

import java.util.HashSet;
import java.util.Set;

public class RecorderService extends Service {

    public static final String ACTION_START = "START";
    public static final String ACTION_STOP = "STOP";
    public static final String ACTION_PAUSE = "PAUSE";
    // TODO: from settings
    private static final long MIN_SPACE_IN_KB = 10240;
    private static PowerManager.WakeLock mWakeLock = null;
    private final Set<RecorderListener> mRecorderListeners = new HashSet<RecorderService.RecorderListener>();
    private int NOTIFICATION_RECORDING = R.string.notification_recording_title;
    private State mState = State.UNINITIALIZED;
    private int mRecordId = 0;
    private IBinder mBinder = new LocalBinder();
    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;
    private long mElapsedTime = 0;
    private long mRecordingStartedTime;
    private Recorder mRecorder;
    private Recording mRecording;
    private Format mFormat = Format.WAV;
    private Class<?> mActivityClass;

    private synchronized static PowerManager.WakeLock getLock(Context context) {
        if (mWakeLock == null) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "RecorderWakeLock");
            mWakeLock.setReferenceCounted(false);
        }

        return mWakeLock;
    }

    @Override
    public void onCreate() {
        Logger.debug("Creating service");

        HandlerThread thread = new HandlerThread("RecorderThread", Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);

        setState(State.NEW);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            Logger.error("Starting command with null intent - ignored");
        } else if (ACTION_START.equals(intent.getAction())) {
            startRecording();
        } else if (ACTION_STOP.equals(intent.getAction())) {
            stopRecording();
        } else if (ACTION_PAUSE.equals(intent.getAction())) {
            togglePause();
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Logger.debug("Destroying service");
        stopRecording();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Logger.debug("Binding intent " + intent);
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Logger.debug("Unbinding intent " + intent);
        if (getState() == State.NEW) {
            Logger.debug("Stopping service");
            stopSelf();
        }

        return true;
    }

    public void addRecorderListener(RecorderListener recorderListener) {
        synchronized (mRecorderListeners) {
            mRecorderListeners.add(recorderListener);
            fireOnStateChangedEvent();
            fireOnClockChangedEvent();
        }
    }

    public void removeRecorderListener(RecorderListener recorderListener) {
        synchronized (mRecorderListeners) {
            mRecorderListeners.remove(recorderListener);
        }
    }

    public void setActivityClass(Class<?> activityClass) {
        mActivityClass = activityClass;
    }

    public State getState() {
        return mState;
    }

    private void setState(State state) {
        synchronized (mState) {
            Logger.debug("Setting state " + state);

            State oldState = mState;
            mState = state;

            try {
                onStateChanged(oldState);
                fireOnClockChangedEvent();
                fireOnStateChangedEvent();
            } catch (ExternalStorageUnavailableException ex) {
                mState = oldState;
                ExceptionHandler.handle("External storage is unavailable", R.string.error_external_storage_unavailable, ex, getApplicationContext());
            } catch (AudioRecorderException ex) {
                mState = oldState;
                ExceptionHandler.handle("Cannot create audio recorder", R.string.error_cannot_create_audio_recorder, ex, getApplicationContext());
            } catch (DirectoryCreateException ex) {
                mState = oldState;
                String path = ex.getPath();
                String userMsg = getResources().getString(R.string.error_cannot_create_directory, path);
                ExceptionHandler.handle("Cannot create directory [" + path + "]", userMsg, ex, getApplicationContext());
            } catch (DirectoryReadException ex) {
                mState = oldState;
                String path = ex.getPath();
                String userMsg = getResources().getString(R.string.error_cannot_read_directory, path);
                ExceptionHandler.handle("Cannot read directory [" + path + "]", userMsg, ex, getApplicationContext());
            } catch (StorageFullException ex) {
                mState = oldState;
                ExceptionHandler.handle("External storage is full", R.string.error_storage_full, ex, getApplicationContext());
            } catch (Exception ex) {
                mState = oldState;
                ExceptionHandler.handle("Unknown error while setting state [" + state + "]", R.string.error_unknown_error, ex, getApplicationContext());
            }
        }
    }

    private void startRecording() {
        if (getState() == State.NEW) {
            setState(State.STARTED);
        }
    }

    private void stopRecording() {
        if (getState() == State.STARTED || getState() == State.PAUSED) {
            setState(State.STOPPED);
            save();
            setState(State.NEW);
        }
    }

    private void togglePause() {
        if (getState() == State.PAUSED) {
            setState(State.STARTED);
        } else if (getState() == State.STARTED) {
            setState(mState = State.PAUSED);
        }
    }

    private void save() {
        if (mRecording == null) {
            return;
        }

        addRecordingToMediaLibrary(mRecording);
        String text = getApplicationContext().getResources().getString(R.string.on_finish_text, mRecording.getFile().getAbsolutePath());
        Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG).show();
        mRecording = null;
    }

    private void addRecordingToMediaLibrary(Recording recording) {
        try {
            ContentValues values = new ContentValues(4);
            long current = System.currentTimeMillis();
            values.put(MediaStore.Audio.Media.TITLE, "Recording " + recording.getName());
            values.put(MediaStore.Audio.Media.DATE_ADDED, (int) (current / 1000));
            values.put(MediaStore.Audio.Media.MIME_TYPE, recording.getType());
            values.put(MediaStore.Audio.Media.DATA, recording.getFile().getAbsolutePath());
            ContentResolver contentResolver = getContentResolver();

            Uri base = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            Uri newUri = contentResolver.insert(base, values);

            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, newUri));
        } catch (Exception ex) {
            Logger.error("Error while adding recording to media library", ex);
        }
    }

    private String getElapsedTime() {
        long tmp = 0;

        if (getState() == State.STARTED || getState() == State.PAUSED) {
            tmp = mElapsedTime;
            if (mRecordingStartedTime > 0) {
                tmp += System.currentTimeMillis() - mRecordingStartedTime;
            }
        }

        long hours = tmp / (3600 * 1000);
        tmp -= hours * 3600 * 1000;
        long minutes = tmp / (60 * 1000);
        tmp -= minutes * 60 * 1000;
        long seconds = tmp / 1000;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    private void onStateChanged(State oldState) {
        switch (mState) {
            case NEW:
                onNewRecording();
                break;

            case STARTED:
                if (oldState == State.NEW) {
                    onStartedRecording();
                }
                onResumedRecording();
                break;

            case PAUSED:
                onPausedRecording();
                break;

            case STOPPED:
                onStoppedRecording();
                break;

            default:
                break;
        }
    }

    private void onNewRecording() {
        mElapsedTime = 0;
        mRecordingStartedTime = 0;
        mRecordId++;
    }

    private void onStartedRecording() {
        acquireWakeLock();
        startForeground();

        try {
            mRecording = RecordingsLibrary.getUniqueOutputFile(mFormat.getFileExtension());
            mRecorder = mFormat.createRecorder(MediaRecorder.AudioSource.MIC, mRecording.openForWriting());

            Message msg = mServiceHandler.obtainMessage();
            msg.arg1 = mRecordId;
            mServiceHandler.sendMessage(msg);
        } catch (Exception ex) {
            stopForeground();
            releaseWakeLock();
            throw ExceptionHandler.convert("Error while starting recording", ex);
        }
    }

    private void onResumedRecording() {
        try {
            mRecordingStartedTime = System.currentTimeMillis();
            mRecorder.start();
        } catch (Exception ex) {
            throw ExceptionHandler.convert("Error while resuming recording", ex);
        }
    }

    private void onPausedRecording() {
        try {
            if (mRecordingStartedTime > 0) {
                mElapsedTime += System.currentTimeMillis() - mRecordingStartedTime;
                mRecordingStartedTime = 0;
            }
            mRecorder.pause();
        } catch (Exception ex) {
            throw ExceptionHandler.convert("Error while pausing recording", ex);
        }
    }

    private void onStoppedRecording() {
        try {
            mRecorder.stop();
            stopForeground();
        } catch (Exception ex) {
            throw ExceptionHandler.convert("Error while stopping recording", ex);
        } finally {
            releaseWakeLock();
        }
    }

    private void fireOnStateChangedEvent() {
        synchronized (mRecorderListeners) {
            for (RecorderListener listener : mRecorderListeners) {
                listener.onStateChanged(mState);
            }
        }
    }

    private void fireOnClockChangedEvent() {
        synchronized (mRecorderListeners) {
            String elapsedTime = getElapsedTime();
            for (RecorderListener listener : mRecorderListeners) {
                listener.onClockChanged(elapsedTime);
            }
        }
    }

    private void fireOnAmplitudeChangedEvent(int amplitude) {
        synchronized (mRecorderListeners) {
            for (RecorderListener listener : mRecorderListeners) {
                listener.onAmplitudeChanged(amplitude);
            }
        }
    }

    @SuppressWarnings("deprecation")
    private Notification createRecordingNotification() {
        if (mActivityClass == null) {
            return null;
        }

        Notification notification = new Notification(R.drawable.ic_stat_recording, getText(NOTIFICATION_RECORDING), mRecordingStartedTime);
        Intent notificationIntent = new Intent(this, mActivityClass);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        notification.setLatestEventInfo(this, getText(NOTIFICATION_RECORDING), getText(R.string.notification_recording_text), contentIntent);
        notification.flags |= Notification.FLAG_NO_CLEAR;
        return notification;
    }

    public void acquireWakeLock() {
        getLock(getApplicationContext()).acquire();
    }

    public void releaseWakeLock() {
        getLock(getApplicationContext()).release();
    }

    private void startForeground() {
        Notification notification = createRecordingNotification();
        if (notification != null) {
            startForeground(NOTIFICATION_RECORDING, notification);
        }
    }

    private void stopForeground() {
        stopForeground(true);
    }

    public enum State {
        /**
         * Recorder is uninitialized and can be initialized.
         */
        UNINITIALIZED,
        /**
         * Recorder is initialized and can be started.
         */
        NEW,
        /**
         * Recorder is started and can be paused or started.
         */
        STARTED,
        /**
         * Recorder is paused and can be stopped or started.
         */
        PAUSED,
        /**
         * Recorder is stopped and nothing more can be done.
         */
        STOPPED
    }

    public interface RecorderListener {
        void onStateChanged(State state);

        void onClockChanged(String elapsedTime);

        void onAmplitudeChanged(int amplitude);
    }

    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public RecorderService getService() {
            return RecorderService.this;
        }
    }

    private final class ServiceHandler extends Handler {

        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            int iterations = 0;

            while (getState() == State.STARTED || getState() == State.PAUSED) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    break;
                }

                if (getState() == State.STARTED) {
                    if (iterations % 10 == 0) {
                        DiskUsage diskUsage = RecordingsLibrary.getDiskUsage();
                        if (diskUsage.isAvailable() && diskUsage.getUsableSpaceKb() < MIN_SPACE_IN_KB) {
                            String text = getApplicationContext().getResources().getString(R.string.on_low_disk_space, diskUsage.getUsableSpaceKb());
                            Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG).show();
                            stopRecording();
                        }
                    }

                    fireOnAmplitudeChangedEvent(mRecorder.getMaxAmplitude());
                    fireOnClockChangedEvent();
                } else if (getState() == State.PAUSED) {
                    fireOnAmplitudeChangedEvent(0);
                }

                if (!mRecorder.isRecording()) {
                    stopRecording();
                }

                iterations++;
            }

            fireOnAmplitudeChangedEvent(0);
            fireOnClockChangedEvent();
        }
    }
}
