package com.audiofetch.aflib.bll.app;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;

import com.audiofetch.afaudiolib.bll.helpers.LG;
//mcjimport com.audiofetch.afaudiolib.uil.activity.MainActivity;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class ApplicationLifecycleHandler implements Application.ActivityLifecycleCallbacks {

    public final static String TAG = ApplicationLifecycleHandler.class.getSimpleName();

    private static int resumed;
    private static int paused;
    private static int started;
    private static int stopped;

    /**
     * Indicates if the application is visible
     *
     * @return
     */
    public static boolean isApplicationVisible() {
        final boolean isVisible = started > stopped;
        return isVisible;
    }

    /**
     * Indicates if the application is in the foreground
     *
     * @return
     */
    public static boolean isApplicationInForeground() {
        final boolean isInForeground = resumed > paused;
        return isInForeground;
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {

    }

    @Override
    public void onActivityStarted(Activity activity) {
        ++started;
    }

    @Override
    public void onActivityResumed(Activity activity) {
        ++resumed;
    }

    @Override
    public void onActivityPaused(Activity activity) {
        ++paused;
    }

    @Override
    public void onActivityStopped(Activity activity) {
        ++stopped;
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        LG.Info(TAG, "Activity destroyed: %s", activity);
        //mcj orig: if (null != activity && activity instanceof MainActivity) {
            if (null != activity ) {
            try {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) {
                    // fix for app not exiting fully when swiped off screen on 5.1 devices
                    Process.killProcess(android.os.Process.myPid());
                }
            } catch(Exception ex) {
                LG.Error(TAG, "UNKNOWN ERROR", ex);
            }
        }
    }
}