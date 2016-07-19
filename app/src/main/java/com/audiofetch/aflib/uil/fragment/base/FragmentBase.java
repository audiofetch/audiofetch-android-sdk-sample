package com.audiofetch.aflib.uil.fragment.base;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

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
     * Shows the soft keyboard
     *
     * @param txt
     */
    public void showSoftKeybaord(EditText txt) {
        if (null != txt) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(txt, 0);
        }
    }

    /**
     * Attempts to hide the soft keyboard
     */
    public void hideSoftKeyboard(EditText txt) {
        getActivity().getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        if (null != txt) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(txt.getWindowToken(), 0);
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

    /**
     * Used in ActivityBase to determine if fragments are equal
     *
     * @param fb
     * @return
     */
    public boolean equalsFragment(FragmentBase fb) {
        return (this.TAG == fb.TAG);
    }
}
