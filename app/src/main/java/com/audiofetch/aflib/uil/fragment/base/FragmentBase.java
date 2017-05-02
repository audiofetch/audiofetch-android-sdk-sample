package com.audiofetch.aflib.uil.fragment.base;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.os.Build;
import android.support.annotation.ColorRes;
import android.view.View;

import com.audiofetch.aflib.uil.activity.MainActivity;

/**
 * Base class for all fragments to make switching between support and android.app.Fragment simplified
 */
public abstract class FragmentBase extends Fragment {

    /*==============================================================================================
    // DATA MEMBERS
    //============================================================================================*/

    public static final String TAG = FragmentBase.class.getSimpleName();

    protected static View mView;

    /*==============================================================================================
    // PUBLIC METHODS
    //============================================================================================*/

    /**
     * Targets 23+ for getColor
     *
     * @param colorResId
     * @return
     */
    @TargetApi(Build.VERSION_CODES.M)
    private int getColorM(@ColorRes int colorResId) {
        return getResources().getColor(colorResId, null);
    }

    /**
     * Resolves deprecation in getColor
     *
     * @param colorResId
     * @return
     */
    @SuppressWarnings("deprecation")
    public int getColor(@ColorRes int colorResId) {
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) ? getColorM(colorResId) : getResources().getColor(colorResId);
    }

    /**
     * Toggles arrow in action bar
     *
     * @param visible
     * @return
     */
    public void toggleBackArrow(final boolean visible) {
        if (null != getMainActivity()) {
            getMainActivity().toggleBackArrow(visible);
        }
    }

    /**
     * Returns the main activity of this package
     *
     * @return
     */
    public MainActivity getMainActivity() {
        MainActivity ma = null;
        final Activity a = getActivity();
        if (a instanceof MainActivity) {
            ma = (MainActivity) a;
        }
        return ma;
    }
}
