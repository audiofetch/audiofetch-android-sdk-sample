package com.audiofetch.aflib.uil.activity;

import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;

import com.audiofetch.afaudiolib.bll.colleagues.AudioController;
import com.audiofetch.afaudiolib.bll.event.AudioFocusEvent;
import com.audiofetch.afaudiolib.bll.event.ChannelsReceivedEvent;
import com.audiofetch.afaudiolib.bll.event.WifiStatusEvent;
import com.audiofetch.afaudiolib.bll.helpers.LG;
import com.audiofetch.aflib.uil.activity.base.ActivityBase;
import com.audiofetch.aflib.uil.fragment.PlayerFragment;
import com.squareup.otto.Subscribe;

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
    }

    @Override
    protected void onStart() {
        super.onStart();
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
     * <p>
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

    /**
     * Whitelists the app for ignoring battery optimiztions.
     */
    public void whitelistAppForBattery() {
        getAudioController().whitelistAppForBatteryOptimizations(this);
    }
}
