package pl.bgadzala.android.dictaphone.library;

import java.io.File;
import java.io.RandomAccessFile;

import pl.bgadzala.android.dictaphone.exception.RecordingCreateException;
import pl.bgadzala.android.dictaphone.exception.RecordingDeleteException;
import pl.bgadzala.android.dictaphone.exception.RecordingExistsException;
import pl.bgadzala.android.dictaphone.exception.RecordingRenameException;

import android.net.Uri;
import pl.bgadzala.arl.AudioStream;
import pl.bgadzala.arl.RAFAudioStream;

public class Recording implements Comparable<Recording> {

	private String mFullName;
	private String mName;
	private String mExtension;
	private String mDirectory;
	private boolean mSelected;

	public Recording(File file) {
		mFullName = file.getName();
		mName = getName(mFullName);
		mExtension = getExtension(mFullName);
		mDirectory = file.getParent();
		mSelected = false;
	}

	public String getFullName() {
		return mFullName;
	}

	public String getDirectory() {
		return mDirectory;
	}

	public String getName() {
		return mName;
	}

	public String getExtension() {
		return mExtension;
	}

	public Uri getUri() {
		return Uri.fromFile(getFile());
	}

	public File getFile() {
		return new File(mDirectory, mFullName);
	}

	public String getType() {
		// TODO
		return "audio/wav";
	}

	public boolean isSelected() {
		return mSelected;
	}

	public void setSelected(boolean selected) {
		mSelected = selected;
	}

	public void delete() {
		File file = getFile();
		if (file.exists() && !file.delete()) {
			String absolutePath = file.getAbsolutePath();
			throw new RecordingDeleteException("Cannot delete [" + absolutePath + "]", absolutePath);
		}
	}

	public void rename(String name) {
		File srcFile = getFile();
		File destFile = new File(mDirectory, name);

		if (srcFile.equals(destFile)) {
			return;
		}

		if (destFile.exists()) {
			String srcPath = srcFile.getAbsolutePath();
			String destPath = destFile.getAbsolutePath();
			throw new RecordingExistsException("Cannot rename [" + srcPath + "] to existing [" + destPath + "]", destPath);
		} else if (srcFile.exists() && !srcFile.renameTo(destFile)) {
			String srcPath = srcFile.getAbsolutePath();
			String destPath = destFile.getAbsolutePath();
			throw new RecordingRenameException("Cannot rename [" + srcPath + "] to [" + destPath + "]", srcPath);
		}
	}

	@Override
	public int compareTo(Recording o) {
		int direction = -1;
		int dirComp = direction * mDirectory.compareTo(o.mDirectory);
		if (dirComp != 0) {
			return dirComp;
		}
		return direction * mFullName.compareTo(o.mFullName);
	}

    public AudioStream openForWriting() {
        File out = getFile();
        try {
            return new RAFAudioStream(new RandomAccessFile(out, "rw"));
        } catch (Exception ex) {
            String path = out.getAbsolutePath();
            throw new RecordingCreateException("Cannot open file [" + path + "] for writing", path);
        }
    }

	@Override
	public String toString() {
		return String.format("Recording {%s/%s}", mDirectory, mFullName);
	}

	private String getExtension(String fileName) {
		if (fileName == null) {
			return null;
		}

		int idx = fileName.lastIndexOf(".");
		if (idx >= 0) {
			return fileName.substring(idx);
		}
		return fileName;
	}

	private String getName(String fileName) {
		if (fileName == null) {
			return null;
		}

		int idx = fileName.lastIndexOf(".");
		if (idx >= 0) {
			return fileName.substring(0, idx);
		}
		return fileName;
	}

}
