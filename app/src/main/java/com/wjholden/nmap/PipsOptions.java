package com.wjholden.nmap;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.widget.Toast;

public class PipsOptions extends PreferenceActivity implements
		OnSharedPreferenceChangeListener {

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
	}

	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		PipsError.log(this.getClass().getName() + " " + key + " value changed.");
		if (key.equals("forceRoot")) {
			PipsError.setLogcatVisible(sharedPreferences.getBoolean(key, false));
		} else if (key.equals("saveHistory")) {
			Context context = getApplicationContext();
			Toast toast = Toast.makeText(context, R.string.clear, Toast.LENGTH_LONG);
			toast.show();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	}
}
