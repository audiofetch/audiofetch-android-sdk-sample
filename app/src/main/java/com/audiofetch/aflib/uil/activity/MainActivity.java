package com.audiofetch.aflib.uil.activity;

import com.squareup.otto.Subscribe;
import android.support.annotation.NonNull;

import android.app.Fragment;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.app.ActivityCompat;
import android.view.KeyEvent;
import android.widget.Toast;

import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import com.audiofetch.afaudiolib.R;
import com.audiofetch.afaudiolib.bll.colleagues.AudioController;
import com.audiofetch.afaudiolib.bll.colleagues.base.ControllerBase;
import com.audiofetch.afaudiolib.bll.event.AudioPreferenceChangeEvent;
import com.audiofetch.afaudiolib.bll.event.AudioFocusEvent;
import com.audiofetch.afaudiolib.bll.event.AudioStateEvent;
import com.audiofetch.afaudiolib.bll.event.AudioTypeEvent;
import com.audiofetch.afaudiolib.bll.event.ChannelsReceivedEvent;
import com.audiofetch.afaudiolib.bll.event.DemoModeEvent;
import com.audiofetch.afaudiolib.bll.event.WifiStatusEvent;
import com.audiofetch.afaudiolib.bll.helpers.LG;
import com.audiofetch.afaudiolib.bll.helpers.PREFS;
import com.audiofetch.aflib.uil.activity.base.ActivityBase;
import com.audiofetch.aflib.uil.fragment.PlayerFragment;

/**
 * Main Activity for AudioFetch app
 */
public class MainActivity extends ActivityBase implements ActivityCompat.OnRequestPermissionsResultCallback {

    /*==============================================================================================
    // CONSTANTS
    //============================================================================================*/

    public static final String TAG = MainActivity.class.getSimpleName();

    /*==============================================================================================
    // DATA MEMBERS
    //============================================================================================*/

    protected boolean mConnectionMsgShown = false;
    protected boolean mDemoPromptShown = false;

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
            // TODO: add other deeplinks besides the default
        }

        mAudioController = getAudioController();
        mGoogClient = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (null != mGoogClient) {
            mGoogClient.connect();
        }
        showPlayerFragment();
        mUiHandler.postDelayed(new Runnable() {
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

    /**
     * Swaps the content in the main view area
     *
     * @param newContent
     * @param title
     * @param tag
     * @param toggle
     */
    @Override
    public void switchContent(final Fragment newContent, final String title, final String tag, boolean toggle) {
        if (toggle) {
            toggle(); // hide menu first
        }
        showActionProgress(true);

        if (null != title && !title.isEmpty()) {
            setTitle(title);
        } else {
            setTitle(null);
        }

        // then show fragment after menu animation
        final CountDownTimer tmr = new CountDownTimer(550, 1) {
            @Override
            public void onTick(long l) {
            }

            @Override
            public void onFinish() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        // give the fragment a few to display before hiding window progress
                        final CountDownTimer t = new CountDownTimer(150, 1) {
                            @Override
                            public void onTick(long l) {

                            }

                            @Override
                            public void onFinish() {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        showActionProgress(false);
                                    }
                                });
                            }
                        };

                        if (mIsRunning) {
                            final int backstackCount = mFragManager.getBackStackEntryCount();
                            final boolean isShowingPlayer = (1 == backstackCount);
                            if (backstackCount > 0 && !isShowingPlayer) {
                                clearBackstack();
                                pushFragment(newContent, tag, true);
                            } else {
                                pushFragment(newContent, tag, true);
                            }
                            t.start();
                        }
                    }
                });
            }
        };
        tmr.start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (ControllerBase.REPORT_PERMISSION_ACCESS_COURSE_LOCATION == requestCode && 2 == permissions.length) {
            final int resId = (PackageManager.PERMISSION_GRANTED == grantResults[0] && PackageManager.PERMISSION_GRANTED == grantResults[1])
                    ? R.string.location_permission_success : R.string.location_permission_denied;
            Toast.makeText(this, resId, Toast.LENGTH_LONG).show();
        }
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
        mUiHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                dismissProgress();
            }
        }, 3000);
    }

    /**
     * This is triggered by AudioController when the demo mode has successfully been toggled
     *
     * @param event
     */
    @SuppressWarnings("unused")
    @Subscribe
    public void onDemoModeEvent(final DemoModeEvent event) {
        LG.Info(TAG, "Demo mode toggled: %s", event);
    }

    /**
     * Triggered by AudioController when it's callback AudioManager.onAudioFocusChange is called.
     * <p/>
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
     * Triggered by AudioController to notify that Sapa started, or native audio started
     *
     * @param event
     */
    @SuppressWarnings("unused")
    @Subscribe
    public void onAudioTypeEvent(final AudioTypeEvent event) {
        if (event.type.equals(AudioTypeEvent.TYPE_SAPA)) {
//            FlurryAgent.logEvent("SAPA AUDIO STARTED");
            Toast.makeText(this, getString(R.string.sapa), Toast.LENGTH_LONG).show();
        } else {
//            FlurryAgent.logEvent("NATIVE AUDIO STARTED");
            Toast.makeText(this, getString(R.string.opensl_es), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Triggered by AudioController when a preference pertaining to audio has changed.
     *
     * Buffer size is already set by audio controller on the apb before this is called.
     *
     * This is not required to be implemented by SDK clients
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
                                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                } catch (NumberFormatException ex) {
                    ex.printStackTrace();
                }
            } else if (event.key.equals(AudioController.PREF_SAPA_ENABLED)) {
                if (event.restartRequired) {
                    Toast.makeText(MainActivity.this, getString(R.string.app_restarting), Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    /**
     * This is fired when the AudioFetch connection goes from one of these states:
     * DISCOVERING, PLAYING, DROPOUT, TIMEOUT (couldn't find any devices), ERROR (event contains error message)
     *
     * This is should be implemented by SDK clients so the ui can update accordingly.
     *
     * @param event
     *
     * @todo move this to an event in playerfragment
     */
    @SuppressWarnings("unused")
    @Subscribe
    public void onAudioStateEvent(final AudioStateEvent event) {
        switch (event.state) {
            case AudioStateEvent.STATE_DISCOVERING: {
                if (!mAudioController.isAudioSourceConnected() && !mConnectionMsgShown) {
                    showProgress(getString(R.string.fetching_audio));
                    mConnectionMsgShown = true;
                }
                break;
            }
            case AudioStateEvent.STATE_PLAYING: {
                if (mAudioController.isAudioSourceConnected() && !mAudioController.isExpressDevice()) {
                    // TODO: show success msg
                }
                break;
            }
            case AudioStateEvent.STATE_DROPOUT: {
                break;
            }
            case AudioStateEvent.STATE_TIMEOUT: {
                this.dismissProgress();
                if (!mDemoPromptShown) {
                    if (!mAudioController.isSapaSession()) {
                        this.toastFor(getString(R.string.no_connection_msg), 5);
                        // todo move this to an event in playerfragment
                        if (null != mPlayerFragment) {
                            Toast.makeText(this, R.string.no_connection_msg, Toast.LENGTH_LONG).show();
                            mPlayerFragment.setupChannels(); // just show a 16 channel UI for demo mode
                        }
                        // todo ensure discovery continues and demo mode is re-enabled
//                        this.confirmDialog(R.string.restart_app, R.string.restart_app_prompt, new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialog, int which) {
//                                ApplicationBase.restartApp();
//                            }
//                        });
                    } else {
                        makeToast(R.string.sapa_fail, Toast.LENGTH_LONG);
                        mAudioController.forceNativeAudio();
                    }
                    mDemoPromptShown = true;
                }
                break;
            }
            case AudioStateEvent.STATE_ERROR:
            default: {
                // todo move this to an event in playerfragment
                Toast.makeText(this, event.error, Toast.LENGTH_LONG).show();
                break;
            }
        }
    }

    /*==============================================================================================
    // PUBLIC METHODS
    //============================================================================================*/

    public void toggleAudio(final WifiStatusEvent event) {
        try {
            final boolean isAudioPlaying = getAudioController().isAudioPlaying();
            if (!isAudioPlaying && event.enabled && event.connected) {
                getAudioController().startAudio();
            } else if (!event.enabled || !event.connected) {
                getAudioController().pauseAudio();
            }
        } catch(Exception ex) {
            LG.Error(TAG, "FAILED TO START/STOP AUDIO", ex);
        }
    }

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
     * Sets the player fragment
     * @param playerFragment
     */
    public void setPlayerFragment(final PlayerFragment playerFragment) {
        mPlayerFragment = playerFragment;
    }

    /**
     * Returns the player fragment
     * @return
     */
    public PlayerFragment getPlayerFragment() {
        return mPlayerFragment;
    }

    /**
     * Pops off any fragments that are covering the player
     */
    public void showPlayerFragment() {
        if (null != mSlidingMenuFragment) {
            mSlidingMenuFragment.showPlayerFragment();
        }
    }
}
