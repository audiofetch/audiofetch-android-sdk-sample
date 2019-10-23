package com.audiofetch.afsdksample.uil.activity;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

/**
 * Causes app to exit cleanly, without leaving itself in recent apps
 */
public class ExitActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            finishAll();
        } else {
            finish();
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void finishAll() {
        this.finishAffinity();
    }

    /**
     * Causes the app to exit, without leaving a trace in the recent apps
     *
     * @param context
     */
    public static void exitAppWithRemoveFromRecent(Context context) {
        Intent i = new Intent(context, ExitActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK |
                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        context.startActivity(i);
    }
}
