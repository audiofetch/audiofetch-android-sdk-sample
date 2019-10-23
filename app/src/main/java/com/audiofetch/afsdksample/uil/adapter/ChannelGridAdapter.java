package com.audiofetch.afsdksample.uil.adapter;

import android.content.Context;
import android.graphics.Typeface;
import android.support.v7.widget.AppCompatTextView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.audiofetch.afaudiolib.bll.helpers.LG;
import com.audiofetch.afaudiolib.dal.Channel;
import com.audiofetch.afsdksample.R;
import com.audiofetch.afsdksample.uil.fragment.PlayerFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for channels GridView
 */
public class ChannelGridAdapter extends BaseAdapter {

    /*==============================================================================================
    // DATA MEMBERS
    //============================================================================================*/
    public static final String TAG = ChannelGridAdapter.class.getSimpleName();

    protected List<Channel> mApbChannels = new ArrayList<>();
    protected LayoutInflater mInflater;
    protected Context mContext;
    protected Typeface normalFont, boldFont;
    protected int mSelectedPosition;

    /*==============================================================================================
    // CONSTRUCTOR
    //============================================================================================*/

    /**
     * ChannelGridAdapter
     *
     * @param channels
     * @param selectedChannel
     * @param ctx
     */
    public ChannelGridAdapter(final List<Channel> channels, final int selectedChannel, final Context ctx) {
        setChannels(channels);
        mSelectedPosition = selectedChannel;
        mContext = ctx;
        mInflater = LayoutInflater.from(mContext);
        normalFont = Typeface.createFromAsset(ctx.getAssets(), PlayerFragment.FONT_PRIMARY);
        boldFont = Typeface.createFromAsset(ctx.getAssets(), PlayerFragment.FONT_BOLD);
    }

    /*==============================================================================================
    // OVERRIDES
    //============================================================================================*/

    /**
     * Returns the number of items
     *
     * @return
     */
    @Override
    public int getCount() {
        return mApbChannels.size();
    }

    /**
     * Returns the Channel item at the position
     *
     * @param position
     * @return
     */
    @Override
    public Object getItem(int position) {
        Channel item = null;
        if (position < mApbChannels.size()) {
            item = mApbChannels.get(position);
        }
        return item;
    }

    /**
     * Returns the channel for the given item
     *
     * @param position
     * @return
     */
    @Override
    public long getItemId(int position) {
        long itemId = 0;
        if (null != getItem(position)) {
            itemId = ((Channel) getItem(position)).channel;
        }
        return itemId;
    }

    /**
     * Returns a view
     *
     * @param position
     * @param convertView
     * @param parent
     * @return
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        GridItemViewHolder holder;
        try {
            if (null == convertView) {
                holder = new GridItemViewHolder();
                convertView = mInflater.inflate(R.layout.channel_grid_item, null);
                holder.channelLabel = convertView.findViewById(R.id.label_channel);
                holder.channelNumber = convertView.findViewById(R.id.text_channel);
                convertView.setTag(holder);
            } else {
                holder = (GridItemViewHolder) convertView.getTag();
            }
            final Channel curChannel = (Channel) getItem(position);
            String name = (null != curChannel) ? curChannel.getNameOrChannel().toUpperCase() : null;
            if (null != name && null != mContext) {
                int nameLen = name.length();
                if (nameLen > 5) {
                    nameLen = 5;
                    name = name.substring(0, 5);
                }
                holder.channelNumber.setText(name);
                float textSize = 9.0f;

                switch (nameLen) {
                    case 4:
                        textSize = 8.0f;
                        break;
                    case 5:
                        textSize = 6.0f;
                        break;
                    default:
                        break;
                }
                LG.Info(TAG, "Channel name text size is: %f", textSize);
                holder.channelNumber.setTextSize(TypedValue.COMPLEX_UNIT_PT, textSize);
            }
            if (null != holder) {
                holder.channelLabel.setTypeface(boldFont);
                holder.channelNumber.setTypeface(normalFont);


            }
            if (null != convertView) {
                final int selectedPosition = getSelectedPosition();
                LG.Info(TAG, "POSITION IS: %d", position);
                final int backgroundResId = (selectedPosition != position) ? R.drawable.rounded_corner : R.drawable.rounded_corner_selected;
                convertView.setBackgroundResource(backgroundResId);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return convertView;
    }


    /*==============================================================================================
    // PUBLIC METHODS
    //============================================================================================*/

    /**
     * Returns the selected position
     *
     * @return int
     */
    public int getSelectedPosition() {
        return mSelectedPosition;
    }

    /**
     * Sets the selected position
     *
     * @param position
     * @return ChannelGridAdapter
     */
    public ChannelGridAdapter setSelectedPosition(final int position) {
        mSelectedPosition = position;
        return this;
    }

    /**
     * Sets the channel list for the adapter
     *
     * @param channels
     * @return ChannelGridAdapter
     */
    public ChannelGridAdapter setChannels(final List<Channel> channels) {
        mApbChannels = (null != channels) ? channels : mApbChannels;
        return this;
    }


    /*==============================================================================================
    // NESTED CLASSES
    //============================================================================================*/

    /**
     * Holder pattern class for adapter
     */
    static class GridItemViewHolder {
        public AppCompatTextView channelLabel;
        public AppCompatTextView channelNumber;
    }
}