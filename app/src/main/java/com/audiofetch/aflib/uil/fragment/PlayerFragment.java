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

import com.audiofetch.afaudiolib.bll.colleagues.AudioController;
import com.audiofetch.afaudiolib.bll.event.AudioStateEvent;
import com.audiofetch.afaudiolib.bll.event.ChannelChangedEvent;
import com.audiofetch.afaudiolib.bll.event.ChannelSelectedEvent;
import com.audiofetch.afaudiolib.bll.event.ChannelsReceivedEvent;
import com.audiofetch.afaudiolib.bll.event.VolumeChangeEvent;
import com.audiofetch.afaudiolib.bll.event.WifiStatusEvent;
import com.audiofetch.afaudiolib.bll.helpers.LG;
import com.audiofetch.afaudiolib.bll.helpers.PREFS;
import com.audiofetch.afaudiolib.dal.Channel;
import com.audiofetch.aflib.R;
import com.audiofetch.aflib.uil.activity.MainActivity;
import com.audiofetch.aflib.uil.adapter.ChannelGridAdapter;
import com.audiofetch.aflib.uil.fragment.base.FragmentBase;
import com.squareup.otto.Subscribe;

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
    public final static int PREF_LAST_VOLUME_DEFAULT = 65;

    public final static String PREF_BATTERY_PERMISSION_REQUESTED = "prefIgnoreBatteryOptimizeRequested";

    protected static int mCurrentChannel = 0;
    protected boolean mConnectionMsgShown = false;
    protected static List<Integer> mChannelIntegerList = new ArrayList<>();
    protected static List<Channel> mChannels = new ArrayList<>();
    protected static AudioController mAudioController;

    /**
     * Tracks fragment load at static/app level
     */
    protected static AtomicInteger mLoadCount = new AtomicInteger(0);
    protected static boolean mIsBusRegistered;

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

    /*==============================================================================================
    // OVERRIDES
    //============================================================================================*/

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAudioController = getMainActivity().getAudioController();
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
        mVolumeControl = (SeekBar) mView.findViewById(R.id.volume_slider);
        mVolumeControl.setProgress(lastVolume);

        mAudioController.setVolumeControl(mVolumeControl);
        mAudioController.setVolume(lastVolume);

        mGridView = (GridView) mView.findViewById(R.id.channel_grid);
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

        if (!mIsBusRegistered) {
            try {
                MainActivity.getBus().register(this);
                mIsBusRegistered = true;
            } catch (RuntimeException ex) {
                LG.Error(TAG, "BUS ALREADY REGISTERED: ", ex);
            } catch (Exception ex) {
                LG.Error(TAG, "UNKNOWN BUS ERROR: ", ex);
            }
        }
        if (isFirstLoad) {
            mChannelsLoaded = false;
        } else {
            toggleBackArrow(false);
            setupChannels();
        }
    }

    @Override
    public void onStop() {
        if (mIsBusRegistered) {
            // remove any possible pending callbacks
            MainActivity.getBus().unregister(this);
            mIsBusRegistered = false;
        }
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    /*==============================================================================================
    // BUS EVENTS
    //============================================================================================*/

    /**
     * This is fired when the AudioFetch connection goes from one of these states:
     * DISCOVERING, PLAYING, DROPOUT, TIMEOUT (couldn't find any devices), ERROR (event contains error message)
     *
     * @param event
     */
    @SuppressWarnings("unused")
    @Subscribe
    public void onAudioStateEvent(final AudioStateEvent event) {
        switch (event.state) {
            case AudioStateEvent.STATE_DISCOVERING: {
                // This is triggered repeatedly while the AudioFetch SDK is performing discovery
                if (!mAudioController.isAudioSourceConnected() && !mConnectionMsgShown) {
                    getMainActivity().showProgress(getString(R.string.fetching_audio));
                    mConnectionMsgShown = true;
                }
                break;
            }
            case AudioStateEvent.STATE_PLAYING: {
                if (mAudioController.isAudioSourceConnected()) {
                    // This is triggered repeatedly while audio is playing with no dropouts
                }
                break;
            }
            case AudioStateEvent.STATE_DROPOUT: {
                // This is triggered repeatedly while audio is playing, but dropouts are occurring
                break;
            }
            case AudioStateEvent.STATE_TIMEOUT: {
                // This will be triggered if device discovery has failed
                getMainActivity().dismissProgress();
                mErrorLabel.setVisibility(View.VISIBLE);
                mChannelText.setText(getString(R.string.channels_not_loaded));
                checkForIgnoringBatteryOptimizations();
                break;
            }
            case AudioStateEvent.STATE_ERROR:
            default: {
                getMainActivity().dismissProgress();
                getMainActivity().makeToast(event.error, Toast.LENGTH_LONG);
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
    @SuppressWarnings("unused")
    @Subscribe
    public void onChannelsReceivedEvent(final ChannelsReceivedEvent event) {
        if (null != event) {
            if (event.hostCount > 0) {
                if (event.hasApbChannels()) {
                    mChannelsLoaded = false;
                    if (!mChannels.isEmpty()) {
                        mChannels.clear();
                    }
                    mChannels.addAll(event.getApbChannels());
                } else if (event.getChannels().size() > 0) {
                    mChannelsLoaded = false;
                    if (!mChannelIntegerList.isEmpty()) {
                        mChannelIntegerList.clear();
                    }
                    mChannelIntegerList.addAll(event.getChannels());

                    if (!mChannels.isEmpty()) {
                        mChannels.clear();
                    }
                    for (Integer i : mChannelIntegerList) {
                        mChannels.add(new Channel(i.intValue(), i.intValue() + 1, String.format("%d", i.intValue() + 1)));
                    }
                }

                mUiHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        checkForIgnoringBatteryOptimizations();
                    }
                }, 3000);
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
     * This is triggered multiple places in the app, but typically by the channel buttons
     * <p>
     * Note:  in Android Studio -
     * perform a right-click and find usage on ChannelSelectedEvent to find its usages
     *
     * @param event
     */
    @SuppressWarnings("unused")
    @Subscribe
    public void onChannelSelectedEvent(final ChannelSelectedEvent event) {
        try {
            if (event.fromClick && !event.fromChannelControl) { // handles tapping on a side channel, or same channel in some cases
                return;
            }
            if (event != null && event.channel > -1) {
                mCurrentChannel = event.channel;
                mAudioController.setChannel(mCurrentChannel);
                MainActivity.getBus().post(new ChannelChangedEvent(mCurrentChannel));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Triggered by {@link com.audiofetch.afaudiolib.bll.colleagues.AudioController} for a Wifi event (e.g., user turns off wifi, no wifi present)
     *
     * @param event
     */
    @SuppressWarnings("unused")
    @Subscribe
    public void onWifiStatusEvent(final WifiStatusEvent event) {
        if (!event.enabled || !event.connected) {
            try {
                // FAILED TO CONNECTED TO WIFI
                final int msgResId = (event.enabled) ? R.string.status_nowifi : R.string.status_wifioff;
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

    /**
     * This is triggered when the volume has changed via the keyboard, device buttons
     *
     * @param event
     */
    @SuppressWarnings("unused")
    @Subscribe
    public void onVolumeChangeEvent(final VolumeChangeEvent event) {
        LG.Info(TAG, "Volume changed to: %d", event.volume);
        sharedPrefs.edit()
                .putInt(PREF_LAST_VOLUME, event.volume)
                .commit();
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
        if (null != getMainActivity() && null != mAudioController) {
            if (mAudioController.onKeyDown(keyCode, event, currentVolume) && currentVolume.intValue() >= 0) {
                success = true;
            }
        }
        return success;
    }

    /**
     * Sets up the channel grid to display the discovered channels
     */
    public synchronized void setupChannels() {
        if (!mChannelsLoaded) {
            mGridViewAdapter = new ChannelGridAdapter(mChannels, getActivity());
            mGridView.setAdapter(mGridViewAdapter);
            if (mChannels.size() > 0) {
                mChannelText.setText(mChannels.get(0).getNameOrChannel());
            }
            mChannelsLoaded = true;
        }
    }

    /**
     * Checks to ensure that doze mode is disabled.
     */
    protected void checkForIgnoringBatteryOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            final boolean hasRequestedPerms = PREFS.getBoolean(PREF_BATTERY_PERMISSION_REQUESTED, false);
            if (!hasRequestedPerms) {
                getMainActivity().alert(R.string.battery_optimization_title, R.string.battery_optimization_msg, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        getMainActivity().whitelistAppForBattery();
                    }
                });
            }
        }
    }

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

                    MainActivity.getBus().post(new ChannelSelectedEvent(mCurrentChannel, false, true));
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
                    mAudioController.pauseAudio();
                } else {
                    mAudioController.restartAudio();
                }
            }
        }, 250);
    }
}
