package com.audiofetch.aflib.uil.activity.base;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;

import android.app.ActionBar;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;

import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.audiofetch.aflib.R;
import com.audiofetch.aflib.uil.activity.ExitActivity;

import com.audiofetch.afaudiolib.bll.app.ApplicationBase;
import com.audiofetch.afaudiolib.bll.event.ChannelChangedEvent;
import com.audiofetch.afaudiolib.bll.event.EventBus;
import com.audiofetch.afaudiolib.bll.helpers.LG;

import com.squareup.otto.Bus;


import android.annotation.TargetApi;



/**
 * Base activity for AudioFetch SDK Sample app
 *
 * Hides boilerplate code from MainActivity...
 */
public class ActivityBase extends Activity {

    /*==============================================================================================
    // DATA MEMBERS
    //============================================================================================*/

    public static final String TAG = ActivityBase.class.getSimpleName();

    public static final int MAIN_CONTAINER_RESID = R.id.main_container;

    protected static Bus mEventBus;

    protected FragmentManager mFragManager;
    protected ProgressDialog mProgressDialog;
    protected Handler mUiHandler = new Handler();

    protected boolean mIsRunning;
    protected FrameLayout mMainContainer;

    /*==============================================================================================
    // STATIC METHODS
    //============================================================================================*/

    public static Bus getBus() {
        if (null == mEventBus) {
            mEventBus = EventBus.get();
        }
        return mEventBus;
    }

    /*==============================================================================================
    // OVERRIDES
    //============================================================================================*/

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // handler, bus and frag manager
        mUiHandler = new Handler();
        mEventBus = getBus();
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
        } catch(Exception ex) {
            LG.Error(TAG, "UNKNOWN ERRROR: ", ex);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mEventBus.register(this);
        mUiHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mEventBus.post(new ChannelChangedEvent(0));
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
        super.onPause();
        mEventBus.unregister(this);
        dismissProgress();
    }

    /**
     * User pressed back on device
     */
    @Override
    public void onBackPressed() {
        this.exitApplicationClearHistory();
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
    public void showProgress(final String msg) {
        if (null != msg) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (null == mProgressDialog) {
                        mProgressDialog = ProgressDialog.show(ActivityBase.this, msg, null, true, false);
                    } else {
                        mProgressDialog.setTitle(msg);
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
        }
        mProgressDialog = null;
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
     * @param title
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
