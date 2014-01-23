package pl.bgadzala.android.dictaphone.library;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import android.os.FileObserver;

public class ChangesObserver {

	private static int CHANGES_ONLY = FileObserver.CREATE | FileObserver.DELETE | FileObserver.DELETE_SELF | FileObserver.CLOSE_WRITE | FileObserver.MOVE_SELF
			| FileObserver.MOVED_FROM | FileObserver.MOVED_TO;

	private class SingleFileObserver extends FileObserver {

		private String mObservedPath;

		public SingleFileObserver(String path) {
			super(path, CHANGES_ONLY);
			mObservedPath = path;
		}

		public void onEvent(int event, String path) {
			setChanged();
			observeDirectory(new File(mObservedPath));
			if (structureChange(event)) {
				gc();
			}
		}

		private boolean structureChange(int event) {
			return (event & FileObserver.DELETE) != 0 || (event & FileObserver.DELETE_SELF) != 0 || (event & FileObserver.MOVE_SELF) != 0
					|| (event & FileObserver.MOVED_FROM) != 0 || (event & FileObserver.MOVED_TO) != 0;
		}

		public boolean isActive() {
			File observed = new File(mObservedPath);
			return observed.exists();
		}
	}

	public interface ChangesListener {
		void onChange();
	}

	private File mRoot;
	private boolean mChanged;
	private Map<String, SingleFileObserver> mObservers;
	private Set<ChangesListener> mListeners;

	public ChangesObserver(File root) {
		mRoot = root;
		mObservers = new HashMap<String, SingleFileObserver>();
		mListeners = new HashSet<ChangesListener>();
		observeDirectory(mRoot);
	}

	public boolean getChanged() {
		boolean result = mChanged;
		mChanged = false;
		return result;
	}

	public void setChanged() {
		mChanged = true;
		synchronized (mListeners) {
			for (ChangesListener listener : mListeners) {
				listener.onChange();
			}
		}
	}

	public void observeDirectory(File directory) {
		if (directory.exists() && directory.isDirectory() && directory.canRead()) {
			synchronized (mObservers) {
				String path = directory.getAbsolutePath();
				SingleFileObserver observer = mObservers.get(path);
				if (observer == null) {
					observer = new SingleFileObserver(path);
					observer.startWatching();
					mObservers.put(path, observer);
				}

				File[] children = directory.listFiles();
				for (int i = 0; i < children.length; i++) {
					observeDirectory(children[i]);
				}
			}
		}
	}

	public void destroy() {
		synchronized (mObservers) {
			for (Iterator<SingleFileObserver> iterator = mObservers.values().iterator(); iterator.hasNext();) {
				SingleFileObserver observer = iterator.next();
				observer.stopWatching();
			}
			mObservers.clear();
		}

		synchronized (mListeners) {
			mListeners.clear();
		}
	}

	public void gc() {
		synchronized (mObservers) {
			for (Iterator<SingleFileObserver> iterator = mObservers.values().iterator(); iterator.hasNext();) {
				SingleFileObserver observer = iterator.next();
				if (!observer.isActive()) {
					mObservers.remove(observer);
				}
			}
		}
	}

	public void addListener(ChangesListener listener) {
		synchronized (mListeners) {
			mListeners.add(listener);
		}
	}

	public void removeListener(ChangesListener listener) {
		synchronized (mListeners) {
			mListeners.remove(listener);
		}
	}
}
