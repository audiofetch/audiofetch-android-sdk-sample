package com.audiofetch.aflib.uil.fragment;


import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.v7.widget.AppCompatTextView;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.media.AudioManager;
import android.content.Context;

import com.audiofetch.afaudiolib.bll.helpers.LG;
import com.audiofetch.afaudiolib.bll.helpers.PREFS;
import com.audiofetch.afaudiolib.dal.Channel;
import com.audiofetch.aflib.R;
import com.audiofetch.aflib.uil.activity.MainActivity;
import com.audiofetch.aflib.uil.adapter.ChannelGridAdapter;
import com.audiofetch.aflib.uil.fragment.base.FragmentBase;

// AudioFetch Service API
import com.audiofetch.afaudiolib.bll.app.AFAudioService;
import com.audiofetch.afaudiolib.api.AfApi;
import com.audiofetch.afaudiolib.api.ApiMessenger;
import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fragment that contains the player view
 */
public class PlayerFragment extends FragmentBase implements View.OnClickListener {

    /*==============================================================================================
    // DATA MEMBERS
    //============================================================================================*/

    public final static String TAG = PlayerFragment.class.getSimpleName();

    public final static String FONT_PRIMARY = "fonts/proxima_nova_regular.otf";
    public final static String FONT_SECONDARY = "fonts/proxima_nova_thin.otf";
    public final static String FONT_BOLD = "fonts/proxima_nova_bold.otf";

    public final static String PREF_LAST_VOLUME = "lastVolume";
    private static final String PREF_FIRST_LAUNCH = "prefIsFirstLaunch";
    public final static int PREF_LAST_VOLUME_DEFAULT = 65;

    public final static String PREF_BATTERY_PERMISSION_REQUESTED = "prefIgnoreBatteryOptimizeRequested";

    protected static int mCurrentChannel = 0;
    protected boolean mConnectionMsgShown = false;
    protected static List<Integer> mChannelIntegerList = new ArrayList<>();
    protected static List<Channel> mChannels = new ArrayList<>();

    /**
     * Tracks fragment load at static/app level
     */
    protected static AtomicInteger mLoadCount = new AtomicInteger(0);

    protected boolean mChannelsLoaded = false,
            isPaused = false;

    protected Handler mUiHandler = new Handler();

    protected SeekBar mVolumeControl;
    protected AppCompatTextView mChannelText;
    protected TextView mChannelLabel, mVolumeLabel, mErrorLabel;
    protected GridView mGridView;
    protected ChannelGridAdapter mGridViewAdapter;
    protected SharedPreferences sharedPrefs;
    protected ImageButton mPlayPause;

    protected float mLastVolume;
    protected float mMaxVolume;


    /*==============================================================================================
    // OVERRIDES
    //============================================================================================*/

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_player, container, false);

        mChannelText = mView.findViewById(R.id.text_current);
        mChannelLabel = mView.findViewById(R.id.label_current);
        mVolumeLabel = mView.findViewById(R.id.label_volume);
        mErrorLabel = mView.findViewById(R.id.label_error);
        mPlayPause = mView.findViewById(R.id.play_pause_button);

        final AssetManager assetMgr = getActivity().getAssets();
        final Typeface normalFont = Typeface.createFromAsset(assetMgr, PlayerFragment.FONT_PRIMARY),
                lightFont = Typeface.createFromAsset(assetMgr, PlayerFragment.FONT_SECONDARY),
                boldFont = Typeface.createFromAsset(assetMgr, PlayerFragment.FONT_BOLD);

        final int channelColor = getColor(R.color.afetch_black);
        mChannelText.setTypeface(boldFont);
        mChannelText.setTextColor(channelColor);

        mChannelLabel.setTypeface(normalFont);
        mChannelLabel.setTextColor(channelColor);

        mErrorLabel.setTypeface(normalFont);
        mVolumeLabel.setTypeface(lightFont);

        final int lastVolume = sharedPrefs.getInt(PREF_LAST_VOLUME, PREF_LAST_VOLUME_DEFAULT);
        mVolumeControl = mView.findViewById(R.id.volume_slider);
        mVolumeControl.setProgress(lastVolume);

        setupVolumeControl();

        mGridView = mView.findViewById(R.id.channel_grid);
        mGridView.setOnItemClickListener(mChannelTappedListener);

        mPlayPause.setOnClickListener(this);

        if (mChannels.size() > 0) {
            mChannelsLoaded = false; // this will only happen when app is backgrounded with channels already discovered
            setupChannels();
        }

        return mView;
    }

    @Override
    public void onStart() {
        super.onStart();
        final int loadCount = mLoadCount.incrementAndGet();
        final boolean isFirstLoad = (1 == loadCount);

        AFAudioService.api().outMsgs()
            .asFlowable()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                msg -> {
                  if (msg instanceof AfApi.ChannelsReceivedMsg) {
                    AfApi.ChannelsReceivedMsg  crMsg = (AfApi.ChannelsReceivedMsg) msg;
                    onChannelsReceivedMsg(crMsg);
                  }
                  else if (msg instanceof AfApi.WifiStatusMsg) {
                    AfApi.WifiStatusMsg  pMsg = (AfApi.WifiStatusMsg) msg;
                    onWifiStatusMsg(pMsg);
                  }
                  else if (msg instanceof AfApi.AudioStateMsg) {
                    AfApi.AudioStateMsg  pMsg = (AfApi.AudioStateMsg) msg;
                    onAudioStateMsg(pMsg);
                  }

                });

        if (isFirstLoad) {
            mChannelsLoaded = false;
        } else {
            toggleBackArrow(false);
            setupChannels();
        }
    }

    @Override
    public void onStop() {
        //mcj if (mIsBusRegistered) {
            // remove any possible pending callbacks
            // mcj MainActivity.getBus().unregister(this);
            //mcj mIsBusRegistered = false;
        //mcj }
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    /**
     * Sets the SeekBar to use as a volume control, so AudioController can become the
     * SeekBar.OnSeekBarChangeListener, for the SeekBar
     *f
     * @param seekbar
     * @return
     */
    public void setupVolumeControl() {

        final MainActivity ma = getMainActivity();
        final AudioManager am = (AudioManager) ma.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        mMaxVolume = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

        if (null != mVolumeControl) {
            mVolumeControl.setMax(0); // android Seekbar bug: (issue 12945) https://issuetracker.google.com/issues/36923384
            mVolumeControl.setMax( (int) mMaxVolume);
            final boolean isFirstLaunch = PREFS.getBoolean(PREF_FIRST_LAUNCH, true); // read it
            PREFS.putBoolean(PREF_FIRST_LAUNCH, false); // set it

            int startingHighVolume = Math.round((float) mMaxVolume * 0.90f),
                    lastVolume = (int) mLastVolume,
                    highVolume = Math.round((float) mMaxVolume * 0.75f);
            if (!isFirstLaunch) {
                lastVolume = getLastUserVolume();
            }
            if (lastVolume >= highVolume) { // dont let volume start too high
                lastVolume = startingHighVolume; // prevent speaker/earbud blowout
                // TODO: toast here??? explaining were turning down the volume a bit???
            }
            else {
                lastVolume = getSystemVolume();
            }

            setVolume(lastVolume);
            mVolumeControl.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, final int progress, boolean fromUser) {
                    if (fromUser) {
                        setVolume(progress);
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });
        }
    }



    /**
     * Sets the volume into last user volume, and throughout the apps processors, and android audio
     *
     * @param volume
     */
    public boolean setVolume(int volume) {
        boolean success = false;
        final MainActivity ma = getMainActivity();
        final AudioManager am = (AudioManager) ma.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);

        if (null != am && volume <= mMaxVolume && volume >= 0) {
            mMaxVolume = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            // needed in case AFAudioService was paused in background
            // from notification controls, or from headset being unplugged
            if (AFAudioService.api().isAudioPlaying() == false) {
                AFAudioService.api().startAudio();
            }

            LG.Verbose(TAG, "SETTING VOLUME TO: %d", volume);
            am.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);

            if (volume > 0) {
                mLastVolume = volume;
                PREFS.putInt(PREF_LAST_VOLUME, volume);
            }

            if (null != mVolumeControl) {
                mVolumeControl.setProgress(volume);
            }
            success = true;
        } else {
            LG.Warn(TAG, "FAILED TO SET VOLUME: %d", volume);
        }
        return success;
    }


    public int getLastUserVolume() {
        return PREFS.getInt(PREF_LAST_VOLUME, (int) mLastVolume);
    }

    /**
     * Returns the volume, or -1 if unable to get the current volume level
     *
     * @return
     */
    public int getSystemVolume() {

        final MainActivity ma = getMainActivity();
        final AudioManager am = (AudioManager) ma.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);

        int volume = -1;
        if (null != am ) {
            volume = am.getStreamVolume(AudioManager.STREAM_MUSIC);
        }
        return volume;
    }


    /**
     * This is fired when the AudioFetch connection goes from one of these states:
     * DISCOVERING, PLAYING, DROPOUT, TIMEOUT (couldn't find any devices), ERROR (event contains error message)
     *
     * @param event
     */
    public void onAudioStateMsg(final AfApi.AudioStateMsg msg) {
        switch (msg.state) {
            case AfApi.AudioStateMsg.STATE_DISCOVERING: {
                // This is triggered repeatedly while the AudioFetch SDK is performing discovery
                if (!AFAudioService.api().isAudioSourceConnected() && !mConnectionMsgShown) {
                    getMainActivity().showProgress(getString(R.string.fetching_audio));
                    mConnectionMsgShown = true;
                }
                break;
            }
            case AfApi.AudioStateMsg.STATE_PLAYING: {
                if (!AFAudioService.api().isAudioSourceConnected()) {
                    // This is triggered repeatedly while audio is playing with no dropouts
                }
                break;
            }
            case AfApi.AudioStateMsg.STATE_DROPOUT: {
                // This is triggered repeatedly while audio is playing, but dropouts are occurring
                break;
            }
            case AfApi.AudioStateMsg.STATE_TIMEOUT: {
                // This will be triggered if device discovery has failed
                getMainActivity().dismissProgress();
                mErrorLabel.setVisibility(View.VISIBLE);
                mChannelText.setText(getString(R.string.channels_not_loaded));
                //mcj bye? checkForIgnoringBatteryOptimizations();
                break;
            }
            case AfApi.AudioStateMsg.STATE_ERROR:
            default: {
                getMainActivity().dismissProgress();
                getMainActivity().makeToast(msg.error, Toast.LENGTH_LONG);
                mChannelText.setText(getString(R.string.channels_not_loaded));
                break;
            }
        }
    }

    /**
     * This is triggered by {@link com.audiofetch.afaudiolib.bll.colleagues.AudioController} when
     * channels have been discovered
     *
     * @param event
     */
    public void onChannelsReceivedMsg(final AfApi.ChannelsReceivedMsg msg) {
        if (null != msg) {
            if (msg.hostCount > 0) {
                if (msg.hasApbChannels()) {
                    mChannelsLoaded = false;
                    if (!mChannels.isEmpty()) {
                        mChannels.clear();
                    }
                    mChannels.addAll(msg.getApbChannels());
                } else if (msg.getChannels().size() > 0) {
                    mChannelsLoaded = false;
                    if (!mChannelIntegerList.isEmpty()) {
                        mChannelIntegerList.clear();
                    }
                    mChannelIntegerList.addAll(msg.getChannels());

                    if (!mChannels.isEmpty()) {
                        mChannels.clear();
                    }
                    for (Integer i : mChannelIntegerList) {
                        mChannels.add(new Channel(i.intValue(), i.intValue() + 1, String.format("%d", i.intValue() + 1)));
                    }
                }

                // Now that we have channels, start the audio.
                mUiHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        AFAudioService.api().startAudio();
                    }
                }, 500);

                // Show the battery optimizations info dialog.
                /*mcj bye? mUiHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        checkForIgnoringBatteryOptimizations();
                    }
                }, 3000);
                */
            }
        }
        if (mChannels.size() > 0) {
            mErrorLabel.setVisibility(View.GONE);
        }
        if (!mChannelsLoaded) {
            setupChannels();
        }
    }

    /**
     * Triggered by {@link com.audiofetch.afaudiolib.bll.colleagues.AudioController} for a Wifi event (e.g., user turns off wifi, no wifi present)
     *
     * @param event
     */
    public void onWifiStatusMsg(final AfApi.WifiStatusMsg msg) {
        if (!msg.enabled || !msg.connected) {
            try {
                // FAILED TO CONNECTED TO WIFI
                final int msgResId = (msg.enabled) ? R.string.status_nowifi : R.string.status_wifioff;
                getMainActivity().makeToast(getString(msgResId), Toast.LENGTH_LONG);

                mUiHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            getMainActivity().dismissProgress();
                        } catch (Exception e) {
                            LG.Error(TAG, "WIFI STATUS ERROR:", e);
                        }
                    }
                }, 700);

                getMainActivity().alert(R.string.wifi_alert_title, R.string.wifi_alert_msg, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        getMainActivity().showWifiSettings();
                    }
                });
            } catch (Exception ex) {
                LG.Error(TAG, ex.getMessage(), ex);
            }
        } else {
            LG.Info(TAG, "WIFI IS ENABLED AND CONNECTED!");
        }
    }


    /*==============================================================================================
    // INSTANCE METHODS
    //============================================================================================*/

    /**
     * Should be called by calling activity to set volume by passing event from the activity's onKeyDown event handler
     *
     * @param keyCode
     * @param event
     * @return
     */
    public boolean onKeyDown(final int keyCode, final KeyEvent event) {
        boolean success = false;
        AtomicInteger currentVolume = new AtomicInteger(-1);
        
        boolean handled = false;
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            mLastVolume = ((mLastVolume + 1) <= mMaxVolume) ? ++mLastVolume : mMaxVolume;
            setVolume( (int) mLastVolume);
            handled = true;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            mLastVolume = (float) ( ((mLastVolume - 1) > 0) ? --mLastVolume : 0 );
            setVolume( (int) mLastVolume);
            handled = true;
        }

        currentVolume.set( (int) mLastVolume);

        return success;
    }

    /**
     * Sets up the channel grid to display the discovered channels
     */
    public synchronized void setupChannels() {
        if (!mChannelsLoaded) {
            mCurrentChannel = AFAudioService.api().getCurrentChannel();
            mGridViewAdapter = new ChannelGridAdapter(mChannels, mCurrentChannel, getActivity());
            mGridView.setAdapter(mGridViewAdapter);
            if (mChannels.size() > mCurrentChannel) {
                mChannelText.setText(mChannels.get(mCurrentChannel).getNameOrChannel());
            }
            mChannelsLoaded = true;
        }
    }


    /**
     * Checks to ensure that doze mode is disabled.
     */
    /*mcj bye?
    protected void checkForIgnoringBatteryOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            final boolean hasRequestedPerms = PREFS.getBoolean(PREF_BATTERY_PERMISSION_REQUESTED, false);
            if (!hasRequestedPerms) {
                getMainActivity().alert(R.string.battery_optimization_title, R.string.battery_optimization_msg, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //mcj hi? getMainActivity().whitelistAppForBattery();
                    }
                });
            }
        }
    }
    */


    /*==============================================================================================
    // NESTED CLASSES
    //============================================================================================*/

    /**
     * Handles button tap for GridView (i.e., channel changes)
     */
    protected AdapterView.OnItemClickListener mChannelTappedListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (null != mGridViewAdapter) {
                final Channel selectedChannel = (Channel) mGridViewAdapter.getItem(position);
                if (null != selectedChannel) {
                    final String name = selectedChannel.getNameOrChannel().toUpperCase();
                    mChannelText.setText(name);
                    mCurrentChannel = selectedChannel.channel;
                    mGridViewAdapter.setSelectedPosition(position)
                            .notifyDataSetChanged();

                    //bye MainActivity.getBus().post(new ChannelSelectedEvent(mCurrentChannel, false, true));
                    
                    // what is the point of channelsSelected? It's just our app, not the service
                    //AFAudioService.api().outMsgs().send(new AfApi.ChannelSelectedMsg( mCurrentChannel, false, true ));

                    AFAudioService.api().inMsgs().send(new AfApi.SetChannelMsg( mCurrentChannel ));
                }
            }
        }
    };

    @Override
    public void onClick(View view) {
        isPaused = !isPaused;
        @ColorRes final int bgColor = (isPaused) ? R.color.afetch_green : R.color.afetch_orange;
        @DrawableRes final int img = (isPaused) ? R.drawable.ic_play : R.drawable.ic_pause;
        mPlayPause.setBackgroundResource(bgColor);
        mPlayPause.setImageResource(img);

        mUiHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isPaused) {
                    //bye mAudioController.pauseAudio();
                    AFAudioService.api().stopAudio();
                } else {
                    //bye mAudioController.startAudio();
                    AFAudioService.api().startAudio();
                }
            }
        }, 250);
    }
}
