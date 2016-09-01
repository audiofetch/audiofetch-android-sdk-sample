package com.audiofetch.aflib.uil.adapter;

import android.content.Context;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.audiofetch.afaudiolib.dal.Channel;
import com.audiofetch.aflib.R;
import com.audiofetch.aflib.uil.fragment.PlayerFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for channels GridView
 */
public class ChannelGridAdapter extends BaseAdapter {

    /*==============================================================================================
    // DATA MEMBERS
    //============================================================================================*/

    protected List<Channel> mApbChannels = new ArrayList<>();
    protected LayoutInflater mInflater;
    protected Context mContext;
    protected Typeface normalFont, boldFont;
    protected int mSelectedPosition = 0;

    /*==============================================================================================
    // CONSTRUCTOR
    //============================================================================================*/

    /**
     * ChannelGridAdapter
     *
     * @param channels
     * @param ctx
     */
    public ChannelGridAdapter(final List<Channel> channels, final Context ctx) {
        setChannels(channels);
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
            itemId = ((Channel)getItem(position)).channel;
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
                holder.channelLabel = (TextView)convertView.findViewById(R.id.label_channel);
                holder.channelNumber = (TextView)convertView.findViewById(R.id.text_channel);
                convertView.setTag(holder);
            } else {
                holder = (GridItemViewHolder)convertView.getTag();
            }
            final Channel curChannel = (Channel) getItem(position);
            String name = (null != curChannel) ? curChannel.getNameOrChannel().toUpperCase() : null;
            if (name != null) {
                int nameLen = name.length();
                if (nameLen > 5) {
                    nameLen = 5;
                    name = name.substring(0,5);
                }
                holder.channelNumber.setText(name);
                switch(nameLen) {
                    case 4:
                        holder.channelNumber.setTextSize(TypedValue.COMPLEX_UNIT_PT, 8.0f);
                        break;
                    case 5:
                        holder.channelNumber.setTextSize(TypedValue.COMPLEX_UNIT_PT, 6.0f);
                        break;
                    default:
                        holder.channelNumber.setTextSize(TypedValue.COMPLEX_UNIT_PT, 9.0f);
                        break;
                }
            }
            if (null != holder) {
                holder.channelLabel.setTypeface(boldFont);
                holder.channelNumber.setTypeface(normalFont);


            }
            if (null != convertView) {
                final int backgroundResId = (getSelectedPosition() != position) ? R.drawable.rounded_corner : R.drawable.rounded_corner_selected;
                convertView.setBackgroundResource(backgroundResId);
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        return convertView;
    }


    /*==============================================================================================
    // PUBLIC METHODS
    //============================================================================================*/

    /**
     * Returns the selected position
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
        public TextView channelLabel;
        public TextView channelNumber;
    }
}