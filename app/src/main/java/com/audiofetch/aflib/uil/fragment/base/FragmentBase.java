package com.audiofetch.aflib.uil.fragment.base;

import android.app.Activity;
import android.app.Fragment;
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
            ma = (MainActivity)a;
        }
        return ma;
    }
}
