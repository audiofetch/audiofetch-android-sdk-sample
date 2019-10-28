package com.audiofetch.afsdksample.uil.activity.base;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;
import android.support.annotation.Nullable;
import java.lang.ref.WeakReference;

import android.content.ServiceConnection;
import android.content.ComponentName;
import android.os.IBinder;

import com.audiofetch.afsdksample.bll.app.ApplicationBase;
import com.audiofetch.afaudiolib.bll.helpers.LG;
import com.audiofetch.afsdksample.R;
import com.audiofetch.afsdksample.uil.activity.ExitActivity;


import dmax.dialog.SpotsDialog;


// Af API communication
import com.audiofetch.afaudiolib.bll.app.AFAudioService;
import com.audiofetch.afaudiolib.api.AfApi;

/**
 * Base activity for AudioFetch SDK Sample app
 * <p>
 * Hides boilerplate code from MainActivity...
 */
public class ActivityBase extends Activity {

    /*==============================================================================================
    // DATA MEMBERS
    //============================================================================================*/

    public static final String TAG = ActivityBase.class.getSimpleName();

    public static final int MAIN_CONTAINER_RESID = R.id.main_container;

    protected static WeakReference<ActivityBase> mInstance; // static works since we have 1 activity

    protected FragmentManager mFragManager;

    protected AlertDialog mProgressDialog;

    protected Handler mUiHandler = new Handler();

    protected boolean mIsRunning;
    protected FrameLayout mMainContainer;

    protected AFAudioService mAFAudioSvc;
    protected boolean mIsAFAudioSvcBound = false;
    protected ServiceConnection mAFAudioSvcConn;


    // Constructor
    public ActivityBase() {
        super();
        mInstance = new WeakReference<>(this);
    }

    // Singleton access
    @Nullable
    public static ActivityBase getInstance() {
        return (null != mInstance) ? mInstance.get() : null;
    }


    /*==============================================================================================
    // OVERRIDES
    //============================================================================================*/

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // handler, bus and frag manager
        mUiHandler = new Handler();
        mFragManager = getFragmentManager();

        // config window with an action bar and a progress spinner in top left corner (hidden)
        final Window win = getWindow();
        win.requestFeature(Window.FEATURE_ACTION_BAR);
        win.requestFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // configure action bar
        ActionBar ab = getActionBar();
        ab.setDisplayShowTitleEnabled(false);
        ab.setDisplayHomeAsUpEnabled(false); // show back arrow
        ab.setDisplayShowHomeEnabled(false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            disableHomeButton();
        }

        @SuppressWarnings("deprecation")
        ActionBar.LayoutParams layout = new ActionBar.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT);
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View customActionBarView = inflater.inflate(R.layout.actionbar_custom, null);
        ab.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        ab.setCustomView(customActionBarView, layout);

        showActionProgress(true);
        mUiHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                showActionProgress(false);
            }
        }, 1500);

        try {
            // assign content view and sliding menu view
            setContentView(R.layout.main_fragment_container);
            mMainContainer = (FrameLayout) findViewById(R.id.main_container);

            // potential fix for startup crash on 6.x
            mMainContainer.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        } catch (Exception ex) {
            LG.Error(TAG, "UNKNOWN ERRROR: ", ex);
        }

        // Start the AudioFetch Service
        startAFAudioService();
    }

    @Override
    protected void onResume() {
        LG.Debug(TAG, "ActivityBase.onResume");
        super.onResume();
        mUiHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                AFAudioService.api().outMsgs().send( new AfApi.ChannelChangedMsg(0));
            }
        }, 1010);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mIsRunning = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        mIsRunning = false;
    }

    @Override
    protected void onPause() {

        LG.Debug(TAG, "ActivityBase.onPause");

        super.onPause();
        dismissProgress();
    }


    /**
     * Start the {@see AFAudioService}
     * @return
     */
    protected ActivityBase startAFAudioService() {
        if (null == mAFAudioSvc) {
            final Intent serviceIntent = new Intent(this, AFAudioService.class);
            startService(serviceIntent);
            bindService(new Intent(this, AFAudioService.class), getAFAudioServiceConnection(), 0);
        }
        return this;
    }

    /**
     * Stops the {@see AFAudioService}
     */
    protected void stopAFAudioService() {
        if (null != mAFAudioSvc) {
            mAFAudioSvc.hideNotifcations();
            if (mIsAFAudioSvcBound && null != mAFAudioSvcConn) {
                unbindService(mAFAudioSvcConn);
            }
            mAFAudioSvc.quit();
        }
    }

    /**
     * Starts the Audiofetch Service and returns a reference to it.
     *
     * @see AFAudioService
     * @return
     */
    protected ServiceConnection getAFAudioServiceConnection() {
        if (null == mAFAudioSvcConn) {
            mAFAudioSvcConn = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName className, IBinder service) {
                    if (service instanceof AFAudioService.AFAudioBinder) {
                        LG.Debug(TAG, "AFAudioService connected");
                        AFAudioService.AFAudioBinder binder = (AFAudioService.AFAudioBinder) service;
                        mAFAudioSvc = binder.getService();

                        if (null != mAFAudioSvc) {
                            Context ctx = getApplicationContext();
                            // app context must be set before initing audio subsystem
                            AFAudioService.api().setAppContext( getApplicationContext() );
                            AFAudioService.api().initAudioSubsystem();

                            mIsAFAudioSvcBound = true;
                            mAFAudioSvc.hideNotifcations();

                            mUiHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    startAFAudioServiceAudio();
                                }
                            });

                            LG.Debug(TAG, "AudioFetch Service In and out API connected.");
                            doSubscriptions();
                        }
                    }
                }

                @Override
                public void onServiceDisconnected(ComponentName componentName) {
                    LG.Debug(TAG, "AFAudioService disconnected");
                    mIsAFAudioSvcBound = false;
                    mAFAudioSvcConn = null;
                    mAFAudioSvc = null;
                }
            };
        }
        return mAFAudioSvcConn;
    }

    // Subclasses override this to subscribe to api messages
    public void doSubscriptions() {
        // subsclasses override
    }



    /**
     * Starts the Audio by starting the AF AudioService
     *
     * @return
     */
    protected boolean startAFAudioServiceAudio() {
        boolean started = false;
        if (null != mAFAudioSvc) {
            started = true;
            mUiHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    AFAudioService.api().startAudio();
                }
            }, 500);
        }
        return started;
    }


    /**
     * User pressed back on device
     */
    @Override
    public void onBackPressed() {
        this.showDeviceHomeScreen();
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        final int itemId = item.getItemId();
        switch (itemId) {
            case android.R.id.home: {
                onBackPressed();
                return true;
            }
        }
        return super.onMenuItemSelected(featureId, item);
    }

    /*==============================================================================================
    // INSTANCE METHODS
    //============================================================================================*/

    /**
     * Starts the Wifi settings app
     */
    public void showWifiSettings() {
        startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
    }

    /**
     * Turns on/off the back arrow in the actionbar
     *
     * @param visible
     * @return
     */
    public ActivityBase toggleBackArrow(final boolean visible) {
        getActionBar().setDisplayHomeAsUpEnabled(visible);
        return this;
    }

    /**
     * Set the action bar title
     *
     * @param title
     */
    public void setTitle(final String title) {
        getActionBar().setTitle(title);
    }

    /**
     * Toggles the spinner in the action bar
     *
     * @param showing
     */
    @SuppressWarnings("deprecation")
    public void showActionProgress(final boolean showing) {
        setProgressBarIndeterminate(showing);
        setProgressBarIndeterminateVisibility(showing);
    }

    /**
     * Returns the handler
     *
     * @return
     */
    public Handler getHandler() {
        return mUiHandler;
    }

    /**
     * Post delayed to handler
     *
     * @param runnable
     * @param delayMs
     */
    public void afterDelay(final Runnable runnable, final int delayMs) {
        getHandler().postDelayed(runnable, delayMs);
    }

    /**
     * Stops the app cleanly
     */
    public void exitApplicationClearHistory() {
        showDeviceHomeScreen();
        ExitActivity.exitAppWithRemoveFromRecent(this); // start exit activity
        ApplicationBase.killApp();
    }

    /**
     * Shows a toast window for duration
     *
     * @param msgResId
     * @param duration
     */
    public void makeToast(final int msgResId, final int duration) {
        final int dur = (Toast.LENGTH_SHORT == duration) ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG;
        Toast.makeText(this, msgResId, dur).show();
    }

    /**
     * Shows a toast window for duration
     *
     * @param msg
     * @param duration
     */
    public void makeToast(final String msg, final int duration) {
        final int dur = (Toast.LENGTH_SHORT == duration) ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG;
        Toast.makeText(this, msg, dur).show();
    }

    /**
     * Shows toast for duration of seconds
     *
     * @param msg      The message to show in the toast
     * @param duration The duration in seconds
     */
    public void toastFor(final String msg, final int duration) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (duration > 2) {
                    final Toast toasty = Toast.makeText(ActivityBase.this, msg, Toast.LENGTH_SHORT);
                    toasty.show();
                    int durationSeconds = (duration * 1000);
                    new CountDownTimer(durationSeconds, 1000) {
                        @Override
                        public void onTick(long l) {
                            toasty.show();
                        }

                        @Override
                        public void onFinish() {
                            toasty.show();
                        }
                    }.start();
                } else {
                    Toast.makeText(ActivityBase.this, msg, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Shows the progress dialog with the given message
     *
     * @param msg
     */
    @SuppressWarnings("deprecation")
    public void showProgress(final String msg) {
        if (null != msg) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (null == mProgressDialog && !TextUtils.isEmpty(msg)) {
                        mProgressDialog = new SpotsDialog(ActivityBase.this, msg, R.style.SpotsDialog);
                        mProgressDialog.show();
                    }
                }
            });
        }
    }

    /**
     * Hides the progress dialog
     */
    public void dismissProgress() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }

    /**
     * Shows an confirm dialog, think javascript:window.alert
     *
     * @param titleResId
     * @param msgResId
     * @param okCallback
     */
    public void alert(final int titleResId, final int msgResId, final DialogInterface.OnClickListener okCallback) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(titleResId)
                .setMessage(msgResId)
                .setPositiveButton(android.R.string.ok, okCallback);
        builder.create().show();
    }

    /**
     * Shows an confirm dialog, think javascript:window.confirm
     *
     * @param titleResId
     * @param msgResId
     * @param positiveCallback
     * @param negativeListener
     */
    public void confirm(final int titleResId, final int msgResId, final DialogInterface.OnClickListener positiveCallback, final DialogInterface.OnClickListener negativeListener) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(titleResId)
                .setMessage(msgResId)
                .setPositiveButton(R.string.yes, positiveCallback)
                .setNegativeButton(R.string.no, negativeListener);
        builder.create().show();
    }

    /**
     * Shows an confirm dialog, think javascript:window.confirm
     *
     * @param titleResId
     * @param msgResId
     * @param positiveCallback
     */
    public void confirm(final int titleResId, final int msgResId, final DialogInterface.OnClickListener positiveCallback) {
        confirm(titleResId, msgResId, positiveCallback, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
    }

    /**
     * Shows an confirm dialog, think javascript:window.confirm
     *
     * @param msgResId
     * @param positiveCallback
     */
    public void confirm(final int msgResId, final DialogInterface.OnClickListener positiveCallback) {
        int titleResId = R.string.confirm;
        confirm(titleResId, msgResId, positiveCallback);
    }

    /*=======================================================
    // FRAGMENT SUPPORT
    //=====================================================*/

    /**
     * Clears the backstack
     */
    public void clearBackstack() {
        if (null != mFragManager) {
            try {
                mFragManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Swaps the content in the main view area
     *
     * @param newContent
     * @param tag
     */
    public void switchContent(final Fragment newContent, final String tag) {
        showActionProgress(true);

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

    /**
     * Pushes a fragment onto the stack
     *
     * @param fragment
     * @param tag
     * @param addToBackstack
     */
    public void pushFragment(final Fragment fragment, final String tag, final boolean addToBackstack) {
        FragmentTransaction ft = mFragManager.beginTransaction();
        ft.replace(MAIN_CONTAINER_RESID, fragment);
        if (addToBackstack) {
            ft.addToBackStack(tag);
        }
        ft.commit();
        updateActionBar();
    }

    /**
     * Updates the actionbar
     */
    public void updateActionBar() {
        final ActionBar ab = getActionBar();
        ab.setDisplayHomeAsUpEnabled(false);
        ab.setDisplayShowHomeEnabled(false);
    }

    /**
     * Hides the app, and takes the user to the device's home screen
     */
    protected void showDeviceHomeScreen() {
        ApplicationBase.showDeviceHomeScreen();
    }

    /**
     * Enables the home button
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    protected void disableHomeButton() {
        getActionBar().setHomeButtonEnabled(false);
    }
}
