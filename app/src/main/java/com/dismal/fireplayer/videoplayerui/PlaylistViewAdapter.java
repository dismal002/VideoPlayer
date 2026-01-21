package com.dismal.fireplayer.videoplayerui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.dismal.fireplayer.R;
import com.dismal.fireplayer.provider.LocalMediaInfo;
import com.dismal.fireplayer.util.ThumbsCache;
import com.dismal.fireplayer.util.ThumbsFetcher;
import com.dismal.fireplayer.util.Utils;

public class PlaylistViewAdapter extends BaseAdapter {
    private static final String TAG = "PlaylistViewAdapter";
    private final Context mContext;
    private final LayoutInflater mInflater;
    private PlayListMediaInfoAdapter mPlaylistMediaInfoAdapter;
    private ThumbsFetcher mThumbsFetcher;

    public PlaylistViewAdapter(Context context, String path) {
        this.mContext = context;
        this.mInflater = LayoutInflater.from(context);
        this.mPlaylistMediaInfoAdapter = PlayListMediaInfoAdapter.getInstance(context);
        this.mThumbsFetcher = new ThumbsFetcher(context);
        if (path == null || Utils.isNetworkStream(path).booleanValue()) {
            this.mPlaylistMediaInfoAdapter.clearPlayList();
        } else {
            this.mPlaylistMediaInfoAdapter.loadPlayListMediaInfo("dir like ?", new String[] { "/%" });
        }
        this.mThumbsFetcher.setAdapter(this.mPlaylistMediaInfoAdapter);
        this.mThumbsFetcher.setLoadingImage((int) R.drawable.empty_thumb);
        this.mThumbsFetcher.setImageCache(new ThumbsCache(this.mContext, Utils.getCacheParameter(this.mContext, 10)));
        this.mThumbsFetcher.setAthumbEnable(false);
    }

    public int getCount() {
        return this.mPlaylistMediaInfoAdapter.getSize();
    }

    public Object getItem(int position) {
        return this.mPlaylistMediaInfoAdapter.getItem(position);
    }

    public long getItemId(int position) {
        return (long) position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = this.mInflater.inflate(R.layout.playlist_grid_item, (ViewGroup) null);
            convertView.setTag(convertView);
        }
        this.mThumbsFetcher.loadThumbs(position, convertView, 1);
        ((TextView) convertView.findViewById(R.id.grid_item_title))
                .setText(((LocalMediaInfo) this.mPlaylistMediaInfoAdapter.getItem(position)).mName);
        return convertView;
    }
}
