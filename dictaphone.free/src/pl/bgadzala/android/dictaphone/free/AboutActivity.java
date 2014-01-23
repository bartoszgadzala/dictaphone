package pl.bgadzala.android.dictaphone.free;

import com.actionbarsherlock.app.ActionBar;

import pl.bgadzala.android.dictaphone.ui.BaseActivity;
import android.os.Bundle;

public class AboutActivity extends BaseActivity {

	/**
	 * Called when the activity is first created.
	 *  
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_about);
		getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
	}

	@Override
	protected void onStart() {
		super.onStart();
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayShowTitleEnabled(true);
	}

	@Override
	protected Class<?> getHomeActivityClass() {
		return RecorderActivity.class;
	}

}
