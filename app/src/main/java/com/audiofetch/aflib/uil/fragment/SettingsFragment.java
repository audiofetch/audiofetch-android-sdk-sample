package com.audiofetch.aflib.uil.fragment;

import android.os.Bundle;
import android.preference.PreferenceFragment;

import com.audiofetch.aflib.R;

/**
 * Implements a {@link PreferenceFragment} for changing the audio buffer latency in milliseconds
 */
public class SettingsFragment extends PreferenceFragment {

    public static final String TAG = SettingsFragment.class.getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.fragment_settings);
    }
}
