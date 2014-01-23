package pl.bgadzala.android.dictaphone.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import pl.bgadzala.android.dictaphone.exception.DirectoryCreateException;
import pl.bgadzala.android.dictaphone.exception.DirectoryReadException;
import pl.bgadzala.android.dictaphone.exception.ExceptionHandler;
import pl.bgadzala.android.dictaphone.exception.ExternalStorageUnavailableException;
import pl.bgadzala.android.dictaphone.exception.RecordingDeleteException;
import pl.bgadzala.android.dictaphone.exception.RecordingExistsException;
import pl.bgadzala.android.dictaphone.exception.RecordingRenameException;
import pl.bgadzala.android.dictaphone.lib.R;
import pl.bgadzala.android.dictaphone.library.Recording;
import pl.bgadzala.android.dictaphone.library.RecordingsAdapter;
import pl.bgadzala.android.dictaphone.library.RecordingsLibrary;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;

import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.widget.ShareActionProvider;

public class FilelistFragment extends BaseFragment implements RecordingsAdapter.OnRecordingSelectionChangedListener {

	private class RefreshTask extends android.os.AsyncTask<Void, Void, Void> {

		private Exception mError;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			showIndeterminateProgressBar();
		}

		@Override
		protected Void doInBackground(Void... params) {
			if (mAdapter != null) {
				synchronized (mAdapter) {
					try {
						mAdapter.refresh();
					} catch (Exception ex) {
						mError = ex;
					}
				}
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			if (mAdapter != null) {
				synchronized (mAdapter) {
					if (mError instanceof ExternalStorageUnavailableException) {
						ExceptionHandler.handle("External storage is unavailable", R.string.error_external_storage_unavailable, mError, getActivity());
					} else if (mError instanceof DirectoryCreateException) {
						String path = ((DirectoryCreateException) mError).getPath();
						String userMsg = getResources().getString(R.string.error_cannot_create_directory, path);
						ExceptionHandler.handle("Cannot create directory [" + path + "]", userMsg, mError, getActivity());
					} else if (mError instanceof DirectoryReadException) {
						String path = ((DirectoryReadException) mError).getPath();
						String userMsg = getResources().getString(R.string.error_cannot_read_directory, path);
						ExceptionHandler.handle("Cannot read directory [" + path + "]", userMsg, mError, getActivity());
					} else if (mError instanceof Exception) {
						ExceptionHandler.handle("Unknown error while scanning for recordings", R.string.error_unknown_error, mError, getActivity());
					} else {
						mAdapter.notifyDataSetChanged();
					}
				}
			}
			hideIndeterminateProgressBar();
			super.onPostExecute(result);
		}

	}

	private RecordingsAdapter mAdapter;
	private ListView mLvRecordings;
	private ActionMode mActionMode;

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getActivity().getMenuInflater();
		inflater.inflate(R.menu.file_menu, menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		Recording recording = getRecordingtAt(info.position);
		if (recording != null) {
			if (item.getItemId() == R.id.file_rename) {
				onRename(recording);
				return true;
			}
		}

		return super.onContextItemSelected(item);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		mAdapter = new RecordingsAdapter(getActivity());
		mAdapter.setOnRecordingSelectionChangedListener(this);

		View root = inflater.inflate(R.layout.fragment_filelist, container);

		mLvRecordings = (ListView) root.findViewById(R.id.lv_recordings);
		mLvRecordings.setAdapter(mAdapter);
		mLvRecordings.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Recording recording = getRecordingtAt(position);
				if (recording != null) {
					Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
					intent.setDataAndType(recording.getUri(), recording.getType());
					try {
						startActivity(intent);
					} catch (ActivityNotFoundException ex) {
						ExceptionHandler.handle("Player for [" + recording + "] is unavalable", R.string.error_no_player_found, ex, getActivity());
					}
				}
			}
		});
		registerForContextMenu(mLvRecordings);

		return root;
	}

	@Override
	public void onResume() {
		refresh();
		super.onResume();
	}

	@Override
	public void onDestroy() {
		mLvRecordings = null;
		destroyAdapter();
		super.onDestroy();
	}

	@Override
	public void onRecordingSelectionChanged(Recording recording) {
		boolean anySelected = mAdapter.getLibrary().isAnySelected();
		if (anySelected && mActionMode == null) {
			mActionMode = getSherlockActivity().startActionMode(getActionModeCallback());
		} else if (anySelected && mActionMode != null) {
			updateShareActionProvider(mActionMode.getMenu().findItem(R.id.file_share));
		} else if (!anySelected && mActionMode != null) {
			mActionMode.finish();
		}

	}

	public Recording getRecordingtAt(int position) {
		if (mAdapter != null) {
			synchronized (mAdapter) {
				return mAdapter.getItem(position);
			}
		}

		return null;
	}

	public void refresh() {
		new RefreshTask().execute();
	}

	public void onRename(final Recording recording) {
		LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.dialog_edit_record, null);

		final EditText etName = (EditText) layout.findViewById(R.id.et_name);
		etName.setText(recording.getName());

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setView(layout).setCancelable(true);
		builder.setPositiveButton(R.string.dialog_rename_recording_positive_button, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				rename(recording, etName.getText().toString().trim());
			}

		});
		builder.setNegativeButton(R.string.dialog_rename_recording_negative_button, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				dialog.cancel();
			}
		});

		AlertDialog alert = builder.create();
		alert.show();
	}

	private void destroyAdapter() {
		if (mAdapter != null) {
			synchronized (mAdapter) {
				mAdapter.setOnRecordingSelectionChangedListener(null);
				mAdapter.clear();
				mAdapter = null;
			}
		}
	}

	private ActionMode.Callback getActionModeCallback() {
		return new ActionMode.Callback() {

			@Override
			public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
				return false;
			}

			@Override
			public void onDestroyActionMode(ActionMode mode) {
				mAdapter.deselectAll();
				mActionMode = null;
			}

			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				com.actionbarsherlock.view.MenuInflater inflater = mode.getMenuInflater();
				inflater.inflate(R.menu.recordings_action_mode, menu);

				com.actionbarsherlock.view.MenuItem shareItem = menu.findItem(R.id.file_share);
				updateShareActionProvider(shareItem);

				return true;
			}

			@Override
			public boolean onActionItemClicked(ActionMode mode, com.actionbarsherlock.view.MenuItem item) {
				if (item.getItemId() == R.id.file_delete) {
					deleteSelected();
					mode.finish();
					return true;
				} else if (item.getItemId() == R.id.file_share) {
					mode.finish();
					return true;
				}
				return false;
			}
		};
	}

	private void deleteSelected() {
		RecordingsLibrary library = mAdapter.getLibrary();
		if (library != null) {
			try {
				delete(library.selectedElements());
			} catch (RecordingDeleteException ex) {
				String path = ex.getPath();
				String userMsg = getResources().getString(R.string.error_cannot_delete_recording, path);
				ExceptionHandler.handle("Cannot delete file [" + path + "]", userMsg, ex, getActivity());
			} finally {
				refresh();
			}
		}
	}

	private void delete(final Collection<Recording> recordings) {
		if (recordings == null || recordings.isEmpty()) {
			return;
		}

		RecordingsLibrary library = mAdapter.getLibrary();
		for (Recording recording : recordings) {
			if (library != null) {
				library.delete(recording);
			} else {
				recording.delete();
			}
			mAdapter.remove(recording);
		}
	}

	private void rename(final Recording recording, final String name) {
		if (name == null || "".equals(name)) {
			return;
		}

		final String newName = name + recording.getExtension();
		try {
			RecordingsLibrary library = mAdapter.getLibrary();
			if (library != null) {
				library.rename(recording, newName);
			} else {
				recording.rename(newName);
			}
			refresh();
		} catch (RecordingExistsException ex) {
			String path = ex.getPath();
			String userMsg = getResources().getString(R.string.error_recording_already_exists, path);
			ExceptionHandler.handle("Cannot rename to file [" + path + "]", userMsg, ex, getActivity());
		} catch (RecordingRenameException ex) {
			String path = ex.getPath();
			String userMsg = getResources().getString(R.string.error_cannot_rename_recording, path);
			ExceptionHandler.handle("Cannot rename file [" + path + "]", userMsg, ex, getActivity());
		}
	}

	private void updateShareActionProvider(com.actionbarsherlock.view.MenuItem shareItem) {
		ShareActionProvider actionProvider = (ShareActionProvider) shareItem.getActionProvider();
		actionProvider.setShareHistoryFileName(ShareActionProvider.DEFAULT_SHARE_HISTORY_FILE_NAME);
		actionProvider.setShareIntent(createShareIntent());
	}

	private Intent createShareIntent() {
		List<Recording> selected = mAdapter.getLibrary().selectedElements();
		if (selected == null || selected.isEmpty()) {
			return null;
		}
		return selected.size() == 1 ? createShareIntent(selected.get(0)) : createShareIntent(selected);
	}

	private Intent createShareIntent(Recording recording) {
		Intent shareIntent = new Intent(Intent.ACTION_SEND);
		shareIntent.putExtra(Intent.EXTRA_STREAM, recording.getUri());
		shareIntent.setType(recording.getType());
		return shareIntent;
	}

	private Intent createShareIntent(Collection<Recording> recordings) {
		Intent shareIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
		ArrayList<Uri> uris = new ArrayList<Uri>();
		String type = null;
		for (Recording recording : recordings) {
			uris.add(recording.getUri());
			String recordingType = recording.getType();
			if (type == null) {
				type = recordingType;
			} else if (!type.equals(recordingType)) {
				type = "audio/*";
			}
		}
		shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
		shareIntent.setType(type);
		return shareIntent;
	}

}
