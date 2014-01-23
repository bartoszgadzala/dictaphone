package pl.bgadzala.android.dictaphone.library;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import pl.bgadzala.android.dictaphone.Logger;
import pl.bgadzala.android.dictaphone.exception.DirectoryCreateException;
import pl.bgadzala.android.dictaphone.exception.DirectoryReadException;
import pl.bgadzala.android.dictaphone.exception.ExternalStorageUnavailableException;
import android.os.Environment;
import android.os.StatFs;

public class RecordingsLibrary {

	public static class DiskUsage {

		private boolean mAvailable;
		private long mTotalSpace;
		private long mUsableSpace;

		public DiskUsage() {
			mTotalSpace = 0L;
			mUsableSpace = 0L;
			mAvailable = false;
		}

		public DiskUsage(long totalSpace, long usableSpace) {
			mTotalSpace = totalSpace;
			mUsableSpace = usableSpace;
			mAvailable = true;
		}

		public boolean isAvailable() {
			return mAvailable;
		}

		public long getTotalSpace() {
			return mTotalSpace;
		}

		public long getTotalSpaceKb() {
			return mTotalSpace / 1024;
		}

		public long getUsableSpace() {
			return mUsableSpace;
		}

		public long getUsableSpaceKb() {
			return mUsableSpace / 1024;
		}

		public int getPercentageUsage() {
			if (mTotalSpace == 0 || mUsableSpace == 0) {
				return 0;
			}

			return Math.round((float) mUsableSpace * 100.0f / (float) mTotalSpace);
		}

		@Override
		public String toString() {
			return String.format("DiskUsage: %d kB of %d kB (%d%%)", getUsableSpaceKb(), getTotalSpaceKb(), getPercentageUsage());
		}
	}

	private static final String AUDIO_RECORDER_FOLDER = "dictaphone";

	private List<Recording> mRecordings = new ArrayList<Recording>();
	private FileFilter mFileFilter = new FileFilter() {
		@Override
		public boolean accept(File file) {
			return file.isDirectory() || file.getName().toLowerCase().endsWith(".wav");
		}
	};
	private ChangesObserver mChangesObserver;

	public static Recording getUniqueOutputFile(String extension) {
		Calendar now = Calendar.getInstance();
		for (int recordId = 1; true; recordId++) {
			File outputFile = getNewOutputFile(extension, recordId, now);
			if (!outputFile.exists()) {
				return new Recording(outputFile);
			}
			Logger.info("Output file [" + outputFile.getAbsolutePath() + "] exists: " + outputFile.exists());
		}
	}

	public static DiskUsage getDiskUsage() {
		try {
			File baseDir = getBaseRecorderDirectory(true);
			StatFs stat = new StatFs(baseDir.getAbsolutePath());
			long blockSize = stat.getBlockSize();
			long usableSpace = blockSize * stat.getAvailableBlocks();
			long totalSpace = blockSize * stat.getBlockCount();
			return new DiskUsage(totalSpace, usableSpace);
		} catch (Exception ex) {
			return new DiskUsage();
		}
	}

	public void destroy() {
		if (mChangesObserver != null) {
			mChangesObserver.destroy();
			mChangesObserver = null;
		}
		if (mRecordings != null) {
			synchronized (mRecordings) {
				mRecordings.clear();
				mRecordings = null;
			}
		}
	}

	public List<Recording> elements() {
		if (mRecordings == null) {
			return null;
		}

		synchronized (mRecordings) {
			return Collections.synchronizedList(Collections.unmodifiableList(mRecordings));
		}
	}

	public List<Recording> selectedElements() {
		List<Recording> selected = new ArrayList<Recording>();
		if (mRecordings != null) {
			synchronized (mRecordings) {
				for (Recording recording : mRecordings) {
					if (recording.isSelected()) {
						selected.add(recording);
					}
				}
			}
		}
		return selected;
	}
	

	public void delesectAll() {
		if (mRecordings != null) {
			synchronized (mRecordings) {
				for (Recording recording : mRecordings) {
					recording.setSelected(false);
				}
			}
		}
	}

	public boolean isAnySelected() {
		if (mRecordings != null) {
			synchronized (mRecordings) {
				for (Recording recording : mRecordings) {
					if (recording.isSelected()) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public void delete(Recording recording) {
		recording.delete();
		getChangesObserver().setChanged();
	}

	public void rename(Recording recording, String name) {
		recording.rename(name);
		getChangesObserver().setChanged();
	}

	public boolean scan() {
		if (mRecordings == null) {
			return false;
		} else if (!mRecordings.isEmpty() && !getChangesObserver().getChanged()) {
			return false;
		}

		synchronized (mRecordings) {
			File baseDirectory = getBaseRecorderDirectory(true);
			mRecordings.clear();
			scan(baseDirectory);
			Collections.sort(mRecordings);
		}

		return true;
	}

	private void scan(File file) {
		if (!file.exists() || !file.canRead()) {
			return;
		}

		if (file.isFile()) {
			mRecordings.add(new Recording(file));
			return;
		} else if (file.isDirectory()) {
			File[] files = file.listFiles(mFileFilter);
			for (int i = 0; i < files.length; i++) {
				scan(files[i]);
			}
		}
	}

	private ChangesObserver getChangesObserver() {
		if (mChangesObserver == null) {
			mChangesObserver = new ChangesObserver(getBaseRecorderDirectory(true));
		}
		return mChangesObserver;
	}

	private static File getNewOutputFile(String extension, int recordId, Calendar calendar) {
		int hour = calendar.get(Calendar.HOUR_OF_DAY);
		int minute = calendar.get(Calendar.MINUTE);
		int second = calendar.get(Calendar.SECOND);
		String name = null;
		if (recordId > 1) {
			name = String.format("rec_%02d-%02d-%02d_%d.%s", hour, minute, second, recordId, extension);
		} else {
			name = String.format("rec_%02d-%02d-%02d.%s", hour, minute, second, extension);
		}
		return new File(getRecorderDirectory(calendar), name);
	}

	private static File getRecorderDirectory(Calendar calendar) {
		File yearFolder = new File(getBaseRecorderDirectory(false), String.format("%04d", calendar.get(Calendar.YEAR)));
		File monthFolder = new File(yearFolder, String.format("%02d", calendar.get(Calendar.MONTH) + 1));
		File dayFolder = new File(monthFolder, String.format("%02d", calendar.get(Calendar.DAY_OF_MONTH)));
		checkFolder(dayFolder, false);
		return dayFolder;
	}

	private static File getBaseRecorderDirectory(boolean readOnly) {
		File sdDir = readOnly ? getExternalStorageDirectoryForReading() : getExternalStorageDirectoryForWriting();
		File recorderFolder = new File(sdDir, AUDIO_RECORDER_FOLDER);
		checkFolder(recorderFolder, readOnly);
		return recorderFolder;
	}

	private static File getExternalStorageDirectoryForWriting() {
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			return Environment.getExternalStorageDirectory();
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			throw new ExternalStorageUnavailableException("External storage is mounted in read-only mode");
		} else {
			throw new ExternalStorageUnavailableException("External storage is neither mounted nor writable");
		}
	}

	private static File getExternalStorageDirectoryForReading() {
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			return Environment.getExternalStorageDirectory();
		} else {
			throw new ExternalStorageUnavailableException("External storage is neither mounted nor readable");
		}
	}

	private static void checkFolder(File folder, boolean readOnly) {
		if (!folder.exists()) {
			if (readOnly) {
				throw new DirectoryReadException("Directory [" + folder.getAbsolutePath() + "] does not exist", folder.getAbsolutePath());
			} else if (!folder.mkdirs()) {
				throw new DirectoryCreateException("Cannot create directory [" + folder.getAbsolutePath() + "]", folder.getAbsolutePath());
			}
		} else if (folder.exists() && !folder.isDirectory()) {
			throw new IllegalArgumentException("Resource [" + folder.getAbsolutePath() + "] is not a directory");
		}
	}

}
