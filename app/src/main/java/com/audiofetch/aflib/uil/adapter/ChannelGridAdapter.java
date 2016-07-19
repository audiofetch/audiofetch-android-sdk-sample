package com.audiofetch.aflib.uil.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.audiofetch.afaudiolib.dal.Channel;
import com.audiofetch.aflib.R;

import java.util.ArrayList;
import java.util.List;

public class ChannelGridAdapter extends BaseAdapter {

    /*==============================================================================================
    // DATA MEMBERS
    //============================================================================================*/

    protected List<Channel> mApbChannels = new ArrayList<>();
    protected LayoutInflater mInflater;
    protected Context mContext;

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
                holder.channelNumber = (TextView)convertView.findViewById(R.id.text_channel);
                convertView.setTag(holder);
            } else {
                holder = (GridItemViewHolder)convertView.getTag();
            }
            Channel curChannel = (Channel) getItem(position);
            if (null != curChannel) {
                holder.channelNumber.setText(curChannel.getNameOrChannel());
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
     * Sets the channel list for the adapter
     *
     * @param channels
     * @return
     */
    public ChannelGridAdapter setChannels(final List<Channel> channels) {
        mApbChannels = (null != channels) ? channels : mApbChannels;
        return this;
    }


    /*==============================================================================================
    // NESTED CLASSES
    //============================================================================================*/

    /**
     * Holder pattern for adapter
     */
    static class GridItemViewHolder {
        public TextView channelNumber;
    }
}
