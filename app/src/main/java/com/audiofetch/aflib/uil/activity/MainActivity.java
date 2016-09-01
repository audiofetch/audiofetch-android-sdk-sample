package com.audiofetch.aflib.uil.activity;

import com.squareup.otto.Subscribe;

import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.Toast;

import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import com.audiofetch.afaudiolib.R;
import com.audiofetch.afaudiolib.bll.colleagues.AudioController;
import com.audiofetch.afaudiolib.bll.event.AudioPreferenceChangeEvent;
import com.audiofetch.afaudiolib.bll.event.AudioFocusEvent;
import com.audiofetch.afaudiolib.bll.event.AudioTypeEvent;
import com.audiofetch.afaudiolib.bll.event.ChannelsReceivedEvent;
import com.audiofetch.afaudiolib.bll.event.WifiStatusEvent;
import com.audiofetch.afaudiolib.bll.helpers.LG;
import com.audiofetch.afaudiolib.bll.helpers.PREFS;

import com.audiofetch.aflib.uil.activity.base.ActivityBase;
import com.audiofetch.aflib.uil.fragment.PlayerFragment;

/**
 * Main Activity for AudioFetch SDK Sample app
 */
public class MainActivity extends ActivityBase {

    /*==============================================================================================
    // CONSTANTS
    //============================================================================================*/

    public static final String TAG = MainActivity.class.getSimpleName();

    /*==============================================================================================
    // DATA MEMBERS
    //============================================================================================*/

    protected AudioController mAudioController;
    protected PlayerFragment mPlayerFragment;
    protected GoogleApiClient mGoogClient;

    /*==============================================================================================
    // OVERRIDES AND ONCLICK
    //============================================================================================*/

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Intent intent = getIntent();
        final String action = (null != intent) ? intent.getAction() : null;
        if (null != action) {
            Uri data = intent.getData();
            LG.Verbose(TAG, "App started from intent: %s with data: %s", action, data);
            // TODO: add other deeplinks besides the default as needed
        }

        mAudioController = getAudioController();
        mGoogClient = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (null != mGoogClient) {
            mGoogClient.connect();
        }
        showPlayerFragment();
        afterDelay(new Runnable() {
            @Override
            public void run() {
                if (!getAudioController().isAudioPlaying()) {
                    getAudioController().startAudio();
                }
            }
        }, 500);
    }

    @Override
    protected void onStop() {
        if (null != mGoogClient) {
            mGoogClient.disconnect();
        }
        super.onStop();
    }

    /**
     * Handles system volume button
     *
     * @param keyCode
     * @param event
     * @return
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            onBackPressed();
            return true;
        }
        if (null != mPlayerFragment && mPlayerFragment.onKeyDown(keyCode, event)) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * Handles device back button press
     */
    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() { // not called every time
        mAudioController.onDestroy();
        super.onDestroy();
    }

    /**
     * Exits the app and launches {@link ExitActivity} to do cleanup on the recent apps screen
     * in android.
     */
    @Override
    public void exitApplicationClearHistory() {
        mAudioController.onDestroy();
        super.exitApplicationClearHistory();
    }

    /*==============================================================================================
    // BUS EVENTS
    //============================================================================================*/

    /**
     * Triggered by WifiController via AudioController
     *
     * @param event
     */
    @SuppressWarnings("unused")
    @Subscribe
    public void onWifiStatusEvent(final WifiStatusEvent event) {
        LG.Info(TAG, "Event is: %s", event);
    }

    /**
     * Dismiss progress bar once loaded
     *
     * @param event
     */
    @SuppressWarnings("unused")
    @Subscribe
    public void onChannelsReceivedEvent(final ChannelsReceivedEvent event) {
        afterDelay(new Runnable() {
            @Override
            public void run() {
                dismissProgress();
            }
        }, 3000);
    }

    /**
     * Triggered by AudioController when it's callback AudioManager.onAudioFocusChange is called.
     *
     * This is for when focus changes for this app, triggered by the AudioManager.onAudioFocusChange interface
     * which can be caused by an audio interruption, such as an incoming call.
     *
     * @param event - event.focusChange = AudioManager.AUDIOFOCUS_GAIN | AudioManager.AUDIOFOCUS_LOSS | AudioManager.AUDIOFOCUS_LOSS_TRANSIENT
     * @link http://developer.android.com/reference/android/media/AudioManager.OnAudioFocusChangeListener.html
     * @see AudioManager
     */
    @SuppressWarnings("unused")
    @Subscribe
    public void onAudioFocusEvent(final AudioFocusEvent event) {
        LG.Verbose(TAG, "AudioFocus gained: %s", (AudioManager.AUDIOFOCUS_GAIN == event.focusChange));
    }

    /**
     * Triggered by AudioController when a preference pertaining to audio has changed.
     *
     * Buffer size is already set by audio controller on the apb before this is called.
     *
     * This is not required to be implemented by SDK clients, but is useful if offering music
     *
     * @param event
     */
    @SuppressWarnings("unused")
    @Subscribe
    public void onAudioPreferenceChangeEvent(final AudioPreferenceChangeEvent event) {
        if (null != event && null != event.key) {
            if (event.key.equals(AudioController.PREF_BUFFER_SIZE_MS)) {
                final String bufferSize = PREFS.getString(event.key, String.valueOf(AudioController.PREF_BUFFER_SIZE_MS_DEFAULT));
                try {
                    final String[] opts = getResources().getStringArray(R.array.pref_buffer_choice_values);
                    final int DEFAULT_MUSIC_BUFSZ = 150,
                            MUSIC_BUFFER_SIZE = (null != opts && opts.length >= 2) ? Integer.valueOf(opts[1]) : DEFAULT_MUSIC_BUFSZ,
                            bufferSizeMs = Integer.valueOf(bufferSize);

                    if (bufferSizeMs > 0) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                final String[] values = getResources().getStringArray(R.array.pref_buffer_choice_names);
                                final String mode = (MUSIC_BUFFER_SIZE == bufferSizeMs) ? values[1] : values[0],
                                        msg = String.format(getString(R.string.listening_mode_set), mode);
                                makeToast(msg, Toast.LENGTH_LONG);
                            }
                        });
                    }
                } catch (NumberFormatException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    /*==============================================================================================
    // PUBLIC METHODS
    //============================================================================================*/

    /**
     * Returns the audio controller
     *
     * @return
     */
    public synchronized AudioController getAudioController() {
        if (null == mAudioController) {
            AudioController.init(this);
            mAudioController = AudioController.get();
        }
        return mAudioController;
    }

    /**
     * Pops off any fragments that are covering the player
     */
    public void showPlayerFragment() {
        if (null == mPlayerFragment) {
            mPlayerFragment = new PlayerFragment();
        }
        switchContent(mPlayerFragment, PlayerFragment.TAG);
    }
}
