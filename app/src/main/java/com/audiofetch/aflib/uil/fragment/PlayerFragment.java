package com.audiofetch.aflib.uil.fragment;


import android.os.Bundle;
import android.os.Handler;

import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.audiofetch.aflib.uil.adapter.ChannelGridAdapter;
import com.audiofetch.aflib.uil.activity.MainActivity;
import com.audiofetch.aflib.uil.fragment.base.FragmentBase;
import com.audiofetch.aflib.R;

import com.audiofetch.afaudiolib.bll.colleagues.AudioController;

import com.audiofetch.afaudiolib.bll.event.ChannelSelectedEvent;
import com.audiofetch.afaudiolib.bll.event.ChannelChangedEvent;
import com.audiofetch.afaudiolib.bll.event.ChannelsReceivedEvent;
import com.audiofetch.afaudiolib.bll.event.NoInternetDetectedEvent;
import com.audiofetch.afaudiolib.bll.event.VolumeChangeEvent;
import com.audiofetch.afaudiolib.bll.event.WifiStatusEvent;

import com.audiofetch.afaudiolib.bll.helpers.LG;
import com.audiofetch.afaudiolib.dal.Channel;

import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.squareup.otto.Subscribe;

/**
 * Fragment that contains the player view
 */
public class PlayerFragment extends FragmentBase implements View.OnClickListener {

    /*==============================================================================================
    // DATA MEMBERS
    //============================================================================================*/

    public final static String TAG = PlayerFragment.class.getSimpleName();

    protected static int mAudioMode = 0,
            mCurrentChannel = 0;

    protected static List<Integer> mChannelIntegerList = new ArrayList<>();
    protected static List<Channel> mChannels = new ArrayList<>();
    protected static AudioController mAudioController;

    /**
     * Tracks fragment load at static/app level
     */
    protected static AtomicInteger mLoadCount = new AtomicInteger(0);
    protected static boolean mIsBusRegistered;

    protected boolean mChannelsLoaded = false;

    protected Handler mUiHandler = new Handler();

    protected SeekBar mVolumeControl;
    protected TextView mChannelText, mChannelName;
    protected GridView mGridView;
    protected ChannelGridAdapter mGridViewAdapter;

    /*==============================================================================================
    // OVERRIDES
    //============================================================================================*/

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAudioController = AudioController.get();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_player, container, false);

        mChannelText = (TextView) mView.findViewById(R.id.text_current);
        mChannelName = (TextView) mView.findViewById(R.id.text_name);

        mVolumeControl = (SeekBar) mView.findViewById(R.id.volume_slider);
        mAudioController.setVolumeControl(mVolumeControl);

        mGridView = (GridView)mView.findViewById(R.id.channel_grid);
        mGridView.setOnItemClickListener(mChannelTappedListener);

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
        // remove any possible pending callbacks
        MainActivity.getBus().unregister(this);
        mIsBusRegistered = false;
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onClick(View view) {
        if (mAudioController.isMuted()) {
            mAudioController.unmuteAudio();
        } else {
            mAudioController.muteAudio();
        }
    }

    /*==============================================================================================
    // BUS EVENTS
    //============================================================================================*/

    /**
     * Triggered when connected to a router with no internet connection.
     * Shuts down ads so that they no longer try to load from the internet.
     *
     * @param event
     */
    @SuppressWarnings("unused")
    @Subscribe
    public void onNoInternetDetectedEvent(final NoInternetDetectedEvent event) {
        LG.Warn(TAG, "NO INTERNET DETECTED!");
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
    }

    /*==================
    // CHANNEL EVENTS
    //================*/

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
                }
            }
        }
        if (!mChannelsLoaded) {
            setupChannels();
        }
    }

    /**
     * This is triggered multiple places in the app, but typically by the channel buttons
     *
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
     * Triggered by WifiController via AudioController
     *
     * @param event
     */
    @SuppressWarnings("unused")
    @Subscribe
    public void onWifiStatusEvent(final WifiStatusEvent event) {
        try {
            if (event.enabled && event.connected) {
                // SUCCESSFULLY CONNECTED TO WIFI
            } else {
                // FAILED TO CONNECTED TO WIFI
                final int msgResId = (event.enabled) ? R.string.status_nowifi : R.string.status_wifioff;
                Toast.makeText(getActivity(), msgResId, Toast.LENGTH_LONG).show();
                setupChannels(); // show a default ui for potential demo mode

                mUiHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            getMainActivity().dismissProgress();
                        } catch(Exception e) {
                            LG.Error(TAG, "WIFI STATUS ERROR:", e);
                        }
                    }
                }, 700);
            }
        } catch(Exception ex) {
            LG.Error(TAG, "WIFI STATUS ERROR:", ex);
        }
    }

    /*==============================================================================================
    // INSTANCE METHODS
    //============================================================================================*/


    /**
     * Call in calling activity to set volume by passing event from the activity's onKeyDown event handler
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
     * Initializes the ChannelViewPager with a list of channels instead of having it generate the list
     */
    protected synchronized void initChannels() {
        if (!mChannelsLoaded) {
            mGridViewAdapter = new ChannelGridAdapter(mChannels, getActivity());
            mGridView.setAdapter(mGridViewAdapter);
            mChannelsLoaded = true;
        }
    }

    /**
     * Sets up channel control
     */
    @SuppressWarnings("unchecked")
    public synchronized void setupChannels() {
        if (!mChannelsLoaded) {
            final int STEREO_CHANNELS = 15, // 16 - index starts at 0 so 0..15 is 16
                      MONO_CHANNELS = 31; // 32
            final Range STEREO_CHANNEL_RANGE = Range.closed(0, STEREO_CHANNELS),
                    MONO_CHANNEL_RANGE = Range.closed(0, MONO_CHANNELS);
            mAudioMode = mAudioController.getAudioMode();

            final boolean isPagerTypeSet = (!mChannelIntegerList.isEmpty() || !mChannels.isEmpty()),
                    useDefaultMode = (!isPagerTypeSet || (0 == mChannelIntegerList.size() && 0 == mChannels.size()));

            if (useDefaultMode) {
                LG.Verbose(TAG, "VIEW PAGER INITIALIZING WITH DEFAULT SETTINGS AND CHANNELS FOR MODE: %d", mAudioMode);

                try {
                    ImmutableList<Integer> chnlList = (ImmutableList<Integer>) ImmutableList.copyOf(ContiguousSet.create(STEREO_CHANNEL_RANGE, DiscreteDomain.integers()));
                    if (null != chnlList && !chnlList.isEmpty()) {
                        if (!mChannelIntegerList.isEmpty()) {
                            mChannelIntegerList.clear();
                        }
                        mChannelIntegerList.addAll(chnlList);
                        Collections.sort(mChannelIntegerList);
                    }
                } catch(ClassCastException ex) {
                    LG.Error(TAG, ex.getMessage(), ex);
                }
            } else {
                LG.Debug(TAG, "VIEW PAGER INITIALIZING WITH CONFIGURED CHANNELS: %s", (!mChannels.isEmpty()) ? mChannels : mChannelIntegerList);
            }

            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    initChannels();
                }
            });
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
                final Channel selectedChannel = (Channel)mGridViewAdapter.getItem(position);
                if (null != selectedChannel) {
                    final String name = selectedChannel.getNameOrChannel(),
                            channel = String.valueOf(selectedChannel.apbChannel);
                    mChannelName.setText(name);
                    mChannelText.setText(channel);
                    mCurrentChannel = selectedChannel.channel;
                    MainActivity.getBus().post(new ChannelSelectedEvent(mCurrentChannel, false, true));
                }
            }
        }
    };
}
