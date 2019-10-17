package com.audiofetch.aflib.uil.activity;

import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.annotation.TargetApi;
import android.util.Log;

import com.audiofetch.aflib.uil.activity.base.ActivityBase;
import com.audiofetch.aflib.uil.fragment.PlayerFragment;
import com.audiofetch.aflib.bll.app.ApplicationBase;

// Audiofetch API
import com.audiofetch.afaudiolib.bll.app.AFAudioService;
import com.audiofetch.afaudiolib.api.AfApi;
import com.audiofetch.afaudiolib.api.ApiMessenger;
import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;



/**
 * Main Activity for AudioFetch SDK Sample app
 */
public class MainActivity extends ActivityBase {

    /*==============================================================================================
    // CONSTANTS
    //============================================================================================*/

    public static final String TAG = MainActivity.class.getSimpleName();

    /**
     * Set this to the channel, on which, you want the player to start playing audio.
     */
    public static final int STARTING_CHANNEL = 1;

    /*==============================================================================================
    // DATA MEMBERS
    //============================================================================================*/

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
            // TODO: add other deeplinks besides the default as needed
        }

        requestIgnoreBatteryOptimizations();
    }

    @Override
    protected void onStart() {
        super.onStart();
        showPlayerFragment();
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
        //mcj mAudioController.onDestroy();
        super.onDestroy();
    }

    /**
     * Exits the app and launches {@link ExitActivity} to do cleanup on the recent apps screen
     * in android.
     */
    @Override
    public void exitApplicationClearHistory() {
        //bye mAudioController.onDestroy();
        super.exitApplicationClearHistory();
    }

    // ---------------------------------------------------
    // AudioFetch API Messages
    // ---------------------------------------------------


    @Override
    public void doSubscriptions() {
        Log.i(TAG, "Listening for AudioFetch Service messages.");
        AFAudioService.api().outMsgs()
            .asFlowable()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                msg -> {
                  if (msg instanceof AfApi.ChannelsReceivedMsg) {
                    AfApi.ChannelsReceivedMsg  crMsg = (AfApi.ChannelsReceivedMsg) msg;
                    onChannelsReceivedEvent(crMsg);
                  }
                  else if (msg instanceof AfApi.WifiStatusMsg) {
                    AfApi.WifiStatusMsg  pMsg = (AfApi.WifiStatusMsg) msg;
                    onWifiStatusEvent(pMsg);
                  }
                  else if (msg instanceof AfApi.AudioFocusMsg) {
                    AfApi.AudioFocusMsg  pMsg = (AfApi.AudioFocusMsg) msg;
                    onAudioFocusEvent(pMsg);
                  }
                  else if (msg instanceof AfApi.ApplicationFinishMsg) {
                    ApplicationBase ab = ApplicationBase.getInstance();
                    if (null != ab) {
                        ab.finish();
                    }
                  }

                });
    }

    /**
     * Triggered by WifiController via AudioController
     *
     * @param event
     */
    public void onWifiStatusEvent(final AfApi.WifiStatusMsg msg) {
        Log.i(TAG, "AFApi WifiStatusMsg");
    }

    /**
     * Dismiss progress bar once loaded
     *
     * @param event
     */
    public void onChannelsReceivedEvent(final AfApi.ChannelsReceivedMsg msg) {
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
    public void onAudioFocusEvent(final AfApi.AudioFocusMsg msg) {
        Log.i(TAG,  "AudioFocusMsg");
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

    @TargetApi(Build.VERSION_CODES.M)
    public void requestIgnoreBatteryOptimizations() {
        final Intent intent = new Intent();
        final String packageName = getPackageName();
        final PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + packageName));
            startActivity(intent);
        }
//        else {
//            intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
//        }
    }


}
