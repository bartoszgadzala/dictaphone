package pl.bgadzala.android.dictaphone.free;

import android.content.Intent;

import com.actionbarsherlock.view.MenuItem;

public class DictaphoneBaseActivity extends NavigationActivity {

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.settings) {
			onSettingsSelected();
			return true;
		} else if (item.getItemId() == R.id.about) {
			onAboutSelected();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void onSettingsSelected() {
		Intent intent = new Intent(this, PreferenceActivity.class);
		startActivity(intent);
	}

	private void onAboutSelected() {
		Intent intent = new Intent(this, AboutActivity.class);
		startActivity(intent);
	}

}
