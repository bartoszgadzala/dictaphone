package pl.bgadzala.android.dictaphone.free;

import pl.bgadzala.android.dictaphone.ui.BaseActivity;
import android.os.Bundle;

public class NavigationActivity extends BaseActivity {

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);

		addTab(R.string.navigation_tab_recorder, R.drawable.ic_action_recorder, RecorderActivity.class);
		addTab(R.string.navigation_tab_filelist, R.drawable.ic_action_list, FilelistActivity.class);
	}

}
