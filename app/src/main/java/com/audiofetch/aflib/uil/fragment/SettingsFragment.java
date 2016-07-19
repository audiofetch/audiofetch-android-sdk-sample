package com.audiofetch.aflib.uil.fragment;

import android.os.Bundle;
import android.preference.Preference;

import android.preference.PreferenceScreen;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.preference.PreferenceFragment;
import android.widget.Toast;
import com.audiofetch.aflib.R;
import com.audiofetch.afaudiolib.bll.colleagues.AudioController;
import com.audiofetch.afaudiolib.bll.helpers.LG;

public class SettingsFragment extends PreferenceFragment {

    public static final String TAG = SettingsFragment.class.getSimpleName();

    private PreferenceScreen mPreferenceScreen;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.fragment_settings);
        final boolean isMasterSapaEnabled = getResources().getBoolean(R.bool.sapa_enabled_master);
        mPreferenceScreen = this.getPreferenceScreen();

        if (null != mPreferenceScreen) {
            if (isMasterSapaEnabled && AudioController.isPotentialSapaDevice()) {
                mPreferenceScreen.findPreference(getString(R.string.pref_key_samsung_container)).setEnabled(true);
            } else {
                final Preference pref = mPreferenceScreen.findPreference(getString(R.string.pref_key_samsung_container));
                if (null != pref) {
                    pref.setEnabled(false);
                    mPreferenceScreen.removePreference(pref);
                }
            }

            // debug settings (used for wifi speed testing)
            final Preference pref = mPreferenceScreen.findPreference(getString(R.string.pref_key_dbg_container));
            if (null != pref) {
                if (LG.DEBUG && AudioController.get().isExpressDevice()) {
                    pref.setEnabled(true);
                } else {
                    // hidden if not debug and express device
                    pref.setEnabled(false);
                    mPreferenceScreen.removePreference(pref);
                }
            }
        }

    }

    @Override
    public View onCreateView(LayoutInflater paramLayoutInflater, ViewGroup paramViewGroup, Bundle paramBundle) {
        final View view = super.onCreateView(paramLayoutInflater, paramViewGroup, paramBundle);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        int colorRes = R.color.afetch_lightgrey;
        // put this in to indicate to AF staff sapa failed, and native audio is playing
        if (LG.DEBUG && AudioController.get().isSapaEnabled() && !AudioController.get().isSapaSession()) {
            colorRes = R.color.afetch_red;
            Toast.makeText(getActivity(), "Sapa failed to start", Toast.LENGTH_LONG).show();
        }
        getView().setBackgroundColor(ContextCompat.getColor(getActivity(), colorRes));
    }
}
