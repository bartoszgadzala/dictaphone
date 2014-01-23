package pl.bgadzala.android.dictaphone.ui;

import android.support.v4.app.FragmentActivity;

import com.actionbarsherlock.app.SherlockFragment;

public class BaseFragment extends SherlockFragment {

	protected abstract class AsyncTask implements Runnable {
		@Override
		public void run() {
			runAsync();
		}

		protected abstract void runAsync();

	}

	protected void runOnUiThread(final AsyncTask task) {
		if (getActivity() != null) {
			getActivity().runOnUiThread(task);
		}
	}
	
	@Override
	public void onDestroy() {
		hideIndeterminateProgressBar();
		super.onDestroy();
	}

	public void showIndeterminateProgressBar() {
		BaseActivity baseActivity = getBaseActivity();
		if (baseActivity != null) {
			baseActivity.showIndeterminateProgressBar();
		}
	}

	public void hideIndeterminateProgressBar() {
		BaseActivity baseActivity = getBaseActivity();
		if (baseActivity != null) {
			baseActivity.hideIndeterminateProgressBar();
		}
	}

	private BaseActivity getBaseActivity() {
		FragmentActivity activity = getActivity();
		if (activity instanceof BaseActivity) {
			return (BaseActivity) activity;
		}

		return null;
	}

}
