package com.audiofetch.aflib.uil.fragment.menu;


import android.content.Context;
import android.os.Bundle;
import android.app.Fragment;
import android.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.audiofetch.aflib.R;
import com.audiofetch.aflib.uil.activity.MainActivity;
import com.audiofetch.aflib.uil.fragment.PlayerFragment;
import com.audiofetch.aflib.uil.fragment.SettingsFragment;

/**
 * Side menu fragment
 */
public class SlidingMenuFragment extends ListFragment {

    /*=======================================================
    // DATA MEMBERS
    //=====================================================*/

    public static final String TAG = ListFragment.class.getSimpleName();
    public static final int LOGO = 0,
                            PLAYER = 1,
                            SETTINGS = 2;

    protected static int mSelectedMenuItem = -1;

    protected View mView;
    protected Fragment mNewContent;

    /*=======================================================
    // OVERRIDES
    //=====================================================*/

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_menu_list, null);
        return mView;
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Log.i(TAG, "List item clicked " + id);
        selectMenuItem(position);
    }

    @Override
    public void onResume() {
        refreshMenu();
        super.onResume();
    }

    /*=======================================================
    // PUBLIC METHODS
    //=====================================================*/

    /**
     * Gets the main activity
     *
     * @return
     */
    public MainActivity getMainActivity() {
        return (MainActivity) getActivity();
    }

    /**
     * Causes the action for the selected menu item to be taken
     *
     * @param position
     */
    public void selectMenuItem(final int position) {
        selectMenuItem(position, true);
    }

    /**
     * Returns true if the player is currently the displayed fragment
     *
     * @return
     */
    public boolean isShowingPlayer() {
        return (mSelectedMenuItem == PLAYER);
    }

    /**
     * Causes the action for the selected menu item to be taken
     *
     * @param position
     * @param toggle
     */
    public synchronized void selectMenuItem(final int position, final boolean toggle) {
        if (getMainActivity().isSlidingMenuVisible() && toggle) {
            getMainActivity().toggle();
        }
        String tag = null;
        mNewContent = null;

        switch (position) {
            case PLAYER: {
                tag = PlayerFragment.TAG;
                if (mSelectedMenuItem == PLAYER) {
                    if (null == getMainActivity().getPlayerFragment()) {
                        final PlayerFragment pf = new PlayerFragment();
                        getMainActivity().setPlayerFragment(pf);
                        mNewContent = pf;
                    } else {
                        return;
                    }
                } else {
                    final PlayerFragment pf = new PlayerFragment();
                    getMainActivity().setPlayerFragment(pf);
                    mNewContent = pf;
                }
                break;
            }
            case SETTINGS: {
                mNewContent = new SettingsFragment();
                tag = SettingsFragment.TAG;
                break;
            }
            case LOGO:
            default: // nothing to do
                break;
        }

        if (mNewContent != null) {
            refreshMenu();
            switchFragment(mNewContent, tag, toggle);
            mSelectedMenuItem = position;
        }
    }

    /**
     * Pops off any fragments that are covering the player
     */
    public void showPlayerFragment() {
        selectMenuItem(SlidingMenuFragment.PLAYER);
    }

    /**
     * Shows the settings fragment
     */
    public void showSettingsFragment() {
        selectMenuItem(SlidingMenuFragment.SETTINGS);
    }

    /**
     * Loads the side menu from resources
     */
    public void refreshMenu() {
        String[] items = getResources().getStringArray(R.array.menu_list);
        final int[] icons = new int[]{
                0, // placeholder for logo item
                R.drawable.ic_back,
                R.drawable.ic_settings,
                R.drawable.ic_close
        };
        MenuAdapter menuAdapter = new MenuAdapter(getActivity());
        for (int i = 0, l = items.length; i < l; i++) {
            final boolean isLogo = (0 == icons[i]);
            menuAdapter.add(new MenuInfo(items[i], icons[i], isLogo));
        }
        setListAdapter(menuAdapter);
    }

    /**
     * Switches a fragment through the menu
     *
     * @param newContent
     * @param title
     * @param tag
     * @param toggle
     */
    public void switchFragment(Fragment newContent, String title, String tag, boolean toggle) {
        if (null != getActivity()) {
            if (null != title && !title.isEmpty()) {
                getMainActivity().setTitle(title);
            } else {
                getMainActivity().setTitle("");
            }
            getMainActivity().switchContent(newContent, tag, toggle);
        }
    }

    /**
     * Switches a fragment through the menu
     *
     * @param newContent
     * @param tag
     * @param toggle
     */
    public void switchFragment(Fragment newContent, String tag, boolean toggle) {
        switchFragment(newContent, "", tag, toggle);
    }

    /*=======================================================
    // NESTED CLASSES
    //=====================================================*/

    /**
     * Side menu list adapter
     */
    public class MenuAdapter extends ArrayAdapter<MenuInfo> {

        public MenuAdapter(Context context) {
            super(context, 0);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final MenuInfo menuItem = getItem(position);
            if (null != menuItem) {
                if (menuItem.isLogo) {
                    convertView = LayoutInflater.from(getContext()).inflate(R.layout.listitem_menu_logo, null);
                } else {
                    ViewHolder holder = null;
                    if (null != convertView) {
                        holder = (ViewHolder) convertView.getTag();
                    }

                    if (null == holder) {
                        convertView = LayoutInflater.from(getContext()).inflate(R.layout.listitem_menu, null);
                        holder = new ViewHolder(convertView);
                        convertView.setTag(holder);
                    }

                    if (null != holder) {
                        try {
                            holder.icon.setImageResource(menuItem.iconRes);
                            holder.label.setText(menuItem.tag);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }
            return convertView;
        }
    }

    /**
     * Holder pattern class for MenuAdapter
     */
    static class ViewHolder {
        final ImageView icon;
        final TextView label;

        public ViewHolder(View view) {
            icon = (ImageView) view.findViewById(R.id.list_menu_icon);
            label = (TextView) view.findViewById(R.id.list_menu_label);
        }
    }

    /**
     * Simple class to store menu info
     */
    private final class MenuInfo {
        public final String tag;
        public final int iconRes;
        public final boolean isLogo;

        public MenuInfo(final String tag, final int iconRes, final boolean isLogo) {
            this.tag = tag;
            this.iconRes = iconRes;
            this.isLogo = isLogo;
        }
    }
}
