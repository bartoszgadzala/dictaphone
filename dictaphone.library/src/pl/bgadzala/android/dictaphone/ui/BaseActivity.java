package pl.bgadzala.android.dictaphone.ui;

import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;

public abstract class BaseActivity extends SherlockFragmentActivity implements ActionBar.TabListener {

	private int mTabIndex = -1;

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
	}
	
	public void showIndeterminateProgressBar() {
		setSupportProgressBarIndeterminateVisibility(true);
	}
	
	public void hideIndeterminateProgressBar() {
		setSupportProgressBarIndeterminateVisibility(false);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (getHomeActivityClass() == null) {
			return super.onOptionsItemSelected(item);
		}

		switch (item.getItemId()) {
		case android.R.id.home:
			// app icon in action bar clicked; go home
			Intent intent = new Intent(this, getHomeActivityClass());
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onTabReselected(Tab tab, FragmentTransaction ft) {
		// NOP
	}

	@Override
	public void onTabSelected(Tab tab, FragmentTransaction ft) {
		// disable invoking selection action after clicking on
		// action for this activity
		if (tab.getPosition() == mTabIndex) {
			return;
		}
		
		if (tab.getTag() instanceof Intent) {
			startActivity((Intent) tab.getTag());
		}
	}

	@Override
	public void onTabUnselected(Tab tab, FragmentTransaction ft) {
		// NOP	
	}

	/**
	 * @return	class of home activity (<code>null</code> in case of implementation of home activity)s
	 */
	protected Class<?> getHomeActivityClass() {
		return null;
	}
	
	@Override
	protected void onResume() {
		setVolumeControlStream(AudioManager.STREAM_MUSIC);
		
		ActionBar actionBar = getSupportActionBar();
		if (mTabIndex >= 0 && mTabIndex < actionBar.getTabCount()) {
			actionBar.setSelectedNavigationItem(mTabIndex);
		}
		
		super.onResume();
	}

	@Override
	protected void onStart() {
		super.onStart();
		boolean enableHome = getHomeActivityClass() != null;
		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayHomeAsUpEnabled(enableHome);
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setHomeButtonEnabled(enableHome);
		
		hideIndeterminateProgressBar();
	}

	protected void addTab(Integer textResId, Integer iconResId, Class<?> activityClass) {
		ActionBar.Tab tab = getSupportActionBar().newTab();
		tab.setTabListener(this);
		if (textResId != null) {
			tab.setText(textResId);
		}
		if (iconResId != null) {
			tab.setIcon(iconResId);
		}
		if (activityClass != null) {
			Intent intent = new Intent(this, activityClass);
			intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			tab.setTag(intent);
		}
		getSupportActionBar().addTab(tab, false);
		if (getClass().equals(activityClass)) {
			mTabIndex = tab.getPosition();
		}
	}
}
