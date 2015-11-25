package com.example.android.sunshine.app;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import java.util.prefs.Preferences;

/**
 * Created by jitendrasachdeva on 25/11/15.
 */
public class SettingsActivity extends PreferenceActivity
        implements OnPreferenceChangeListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_general);

        Preference preference = findPreference(getString(R.string.pref_location_key));
        bindPreferenceSummaryToValue(preference);

    }

    private void bindPreferenceSummaryToValue(Preference preference) {
        preference.setOnPreferenceChangeListener(this);
        onPreferenceChange(preference, PreferenceManager
                    .getDefaultSharedPreferences(preference.getContext())
                    .getString(preference.getKey(),"")
        );
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        String stringValue = value.toString();
        if (preference instanceof ListPreference){

        }else{
            preference.setSummary(stringValue);
        }

        return true;
    }


}
