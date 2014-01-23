package pl.bgadzala.android.dictaphone.ui;

import pl.bgadzala.android.dictaphone.lib.R;
import android.os.Bundle;
import android.preference.PreferenceActivity;

public abstract class BasePreferenceActivity extends PreferenceActivity {

	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.base_preferences);
		if (getAdditionalPreferencesResource() != null) {
			addPreferencesFromResource(getAdditionalPreferencesResource().intValue());
		}
	}

	protected Integer getAdditionalPreferencesResource() {
		return null;
	}

}
