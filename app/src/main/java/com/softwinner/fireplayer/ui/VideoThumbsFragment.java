package com.softwinner.fireplayer.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.softwinner.fireplayer.R;
import com.softwinner.fireplayer.constant.Constants;
import com.softwinner.fireplayer.provider.LocalMediaInfo;
import com.softwinner.fireplayer.provider.LocalMediaInfoAdapter;
import com.softwinner.fireplayer.provider.LocalMediaProviderContract;
import com.softwinner.fireplayer.util.ThumbsCache;
import com.softwinner.fireplayer.util.ThumbsFetcher;
import com.softwinner.fireplayer.util.Utils;
import com.softwinner.fireplayer.videoplayerui.FloatVideoManager;
import java.lang.ref.WeakReference;

public class VideoThumbsFragment extends Fragment implements AdapterView.OnItemClickListener,
        AdapterView.OnItemLongClickListener, FloatVideoManager.OnFullscreenListener, View.OnTouchListener {
    public static final int CATEGORY_ALL_MEDIA = 0;
    public static final int CATEGORY_FAVORITE_MEDIA = 3;
    public static final int CATEGORY_OTHER_FOLDER = 4;
    public static final int CATEGORY_RECENT_MEDIA = 1;
    public static final int CATEGORY_RECODER_MEDIA = 2;
    public static final int CATEGORY_SEARCH_MEDIA = 5;
    public static final String IMAGE_CACHE_DIR = "thumbs";
    private static final int MSG_DELETE_MEDIA = 3;
    private static final int MSG_SAVE_APPMODE = 1;
    private static final int MSG_SORT_MEDIA = 0;
    private static final int MSG_UPDATE_RECENTPLAY_UI = 2;
    private static final String TAG = "VideoThumbsFragment";
    private boolean isRetainOK = false;
    /* access modifiers changed from: private */
    public VideoThumbsAdapter mAdapter;
    private OnActivityCallbackListener mCallback;
    /* access modifiers changed from: private */
    public int mCategory = 0;
    private FloatVideoManager mFloatVideoManager;
    /* access modifiers changed from: private */
    public boolean mFuzzy = false;
    /* access modifiers changed from: private */
    public Handler mHandler = new HandlerExtension(this);
    /* access modifiers changed from: private */
    public LayoutInflater mLayoutInflater;
    /* access modifiers changed from: private */
    public LocalMediaInfoAdapter mLocalMediaInfoAdapter;
    private int mMediaCount = 0;
    private View mMediaScanProgressView = null;
    private int mMediaScanProgressViewRefCounter = 0;
    /* access modifiers changed from: private */
    public String mSelectedFolder = Constants.MEDIA_ROOT_PATH;
    /* access modifiers changed from: private */
    public int mSortMehod = 1;
    private TextView mTextviewNoMedia;
    /* access modifiers changed from: private */
    public ThumbsFetcher mThumbsFetcher;
    /* access modifiers changed from: private */
    public VideoThumbsTextureViewManager mThumbsTextureViewManager;
    private ImageView mUsageGuide;
    private boolean showUsage;

    public interface OnActivityCallbackListener {
        boolean onVideoItemClick(String str);
    }

    private static class HandlerExtension extends Handler {
        WeakReference<VideoThumbsFragment> mExternalClass;

        HandlerExtension(VideoThumbsFragment activity) {
            this.mExternalClass = new WeakReference<>(activity);
        }

        public void handleMessage(Message msg) {
            VideoThumbsFragment theExternalClass = (VideoThumbsFragment) this.mExternalClass.get();
            if (theExternalClass != null) {
                switch (msg.what) {
                    case 0:
                        int unused = theExternalClass.loadFolderMedias(((Integer) msg.obj).intValue(),
                                theExternalClass.mSelectedFolder, theExternalClass.mFuzzy);
                        theExternalClass.mThumbsTextureViewManager.suspendAllTextureView();
                        theExternalClass.mAdapter.notifyDataSetChanged();
                        return;
                    case 1:
                        AppConfig.getInstance(theExternalClass.getActivity().getApplicationContext())
                                .setAppMode(((Integer) msg.obj).intValue(), true);
                        return;
                    case 2:
                        theExternalClass.loadFolderMedias(Constants.PREDEF_CATEGORY[1], false);
                        return;
                    case 3:
                        if (theExternalClass.mLocalMediaInfoAdapter.deleteMedia((LocalMediaInfo) msg.obj) > 0) {
                            int unused2 = theExternalClass.loadFolderMedias(theExternalClass.mSortMehod,
                                    theExternalClass.mSelectedFolder, theExternalClass.mFuzzy);
                            theExternalClass.mAdapter.notifyDataSetChanged();
                            return;
                        }
                        Toast.makeText(theExternalClass.getActivity(),
                                theExternalClass.getResources().getString(R.string.str_delete_fail), 0).show();
                        return;
                    default:
                        return;
                }
            }
        }
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        this.mFloatVideoManager = FloatVideoManager.getInstance(getActivity());
        this.mFloatVideoManager.setListener(this);
        this.mAdapter = new VideoThumbsAdapter(getActivity());
        ThumbsCache.ImageCacheParams cacheParams = Utils.getCacheParameter(getActivity(), 5);
        Log.d(TAG, "CacheSize=" + cacheParams.memCacheSize);
        this.mThumbsFetcher = new ThumbsFetcher(getActivity());
        this.mLocalMediaInfoAdapter = LocalMediaInfoAdapter.getInstance(getActivity());
        this.mThumbsFetcher.setAdapter(this.mLocalMediaInfoAdapter);
        this.mThumbsFetcher.setLoadingImage((int) R.drawable.empty_thumb);
        this.mThumbsFetcher.setImageCache(ThumbsCache.findOrCreateCache(getActivity(), cacheParams));
        this.mThumbsTextureViewManager = VideoThumbsTextureViewManager.getInstance(getActivity());
        this.mThumbsFetcher.setVideoThumbsTextureViewManager(this.mThumbsTextureViewManager);
        this.mThumbsFetcher.setAthumbEnable(
                AppConfig.getInstance(getActivity().getApplicationContext()).getBoolean(AppConfig.ATHUMB_ENABLE, true));
        loadFolderMedias(this.mSortMehod, this.mSelectedFolder, this.mFuzzy);
        this.mAdapter.notifyDataSetChanged();
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setRetainInstance(true);
        View v = inflater.inflate(R.layout.thumbs_grid_fragment, container, false);
        GridView mGridView = (GridView) v.findViewById(R.id.gridView);
        this.mTextviewNoMedia = (TextView) v.findViewById(R.id.textview_no_media);
        this.mMediaScanProgressView = v.findViewById(R.id.progressbar_mediascan);
        mGridView.setAdapter(this.mAdapter);
        mGridView.setOnItemClickListener(this);
        mGridView.setOnItemLongClickListener(this);
        this.mLayoutInflater = inflater;
        if (this.isRetainOK) {
            setBackgroundTextview(this.mMediaCount);
        }
        this.mUsageGuide = (ImageView) v.findViewById(R.id.usage_guide);
        this.showUsage = AppConfig.getInstance(getActivity().getApplicationContext())
                .getBoolean(AppConfig.VPLAYER_USAGE1, true);
        if (this.showUsage) {
            this.mUsageGuide.setVisibility(0);
            this.mUsageGuide.setOnTouchListener(this);
        }
        return v;
    }

    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            this.mCallback = (OnActivityCallbackListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(
                    String.valueOf(activity.toString()) + " must implement OnHeadlineSelectedListener");
        }
    }

    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    public void onDestroyView() {
        super.onDestroyView();
    }

    private void resume() {
        this.mThumbsFetcher.setExitTasksEarly(false);
        this.mThumbsTextureViewManager.resumeAllTextureView();
        this.mAdapter.notifyDataSetChanged();
    }

    private void pause() {
        this.mThumbsFetcher.setExitTasksEarly(true);
        this.mThumbsTextureViewManager.suspendAllTextureView();
    }

    public void onResume() {
        super.onResume();
        resume();
    }

    public void onPause() {
        super.onPause();
        pause();
    }

    public void onStart() {
        super.onStart();
        setLoaded(true);
    }

    public void onItemClick(AdapterView<?> adapterView, View v, int position, long id) {
        int appMode = AppConfig.getInstance(getActivity().getApplicationContext()).getAppMode();
        LocalMediaInfo mediaInfo = (LocalMediaInfo) this.mThumbsFetcher.getMediaInfo(position);
        updatePlayCounter(mediaInfo.mPath);
        if (!this.mCallback.onVideoItemClick(mediaInfo.mPath)) {
            Boolean defaultOpenFS = Boolean.valueOf(AppConfig.getInstance(getActivity().getApplicationContext())
                    .getBoolean(AppConfig.DEFAULT_OPEN_FULLSCREEN, true));
            if (appMode != 0 && defaultOpenFS.booleanValue()) {
                Log.v("fuqiang", "onItemClick");
                Intent i = new Intent(getActivity(), VideoPlayerActivity.class);
                i.putExtra(LocalMediaProviderContract.PATH_COLUMN, mediaInfo.mPath);
                i.putExtra("boot-mode", "internal");
                startActivityForResult(i, 5);
            } else if (!this.mFloatVideoManager.openFloatView(mediaInfo.mPath)) {
                Log.e(TAG, "can't open, maybe max number reach");
            }
        }
    }

    public boolean onItemLongClick(AdapterView<?> adapterView, View arg1, int position, long arg3) {
        showMediaDetailInfoDialog(position, true);
        return true;
    }

    private void updatePlayCounter(String path) {
        final String vpath = path;
        new Thread(new Runnable() {
            public void run() {
                ContentValues cv = new ContentValues();
                int latestPlayTime = AppConfig
                        .getInstance(VideoThumbsFragment.this.getActivity().getApplicationContext())
                        .getInt(AppConfig.LATEST_PLAYTIME, 0) + 1;
                AppConfig.getInstance(VideoThumbsFragment.this.getActivity().getApplicationContext())
                        .setInt(AppConfig.LATEST_PLAYTIME, latestPlayTime);
                cv.put(LocalMediaProviderContract.LATEST_PLAY_COLUMN, Integer.valueOf(latestPlayTime));
                VideoThumbsFragment.this.getActivity().getContentResolver().update(
                        LocalMediaProviderContract.MEDIAINFO_TABLE_CONTENTURI, cv, "path=?", new String[] { vpath });
                if (VideoThumbsFragment.this.mCategory == 1) {
                    VideoThumbsFragment.this.mHandler.sendEmptyMessage(2);
                }
            }
        }).start();
    }

    private void setBackgroundTextview(int mediaNum) {
        String text;
        if (this.mTextviewNoMedia != null) {
            if (mediaNum == 0) {
                if (this.mCategory == 5) {
                    text = getResources().getString(R.string.str_no_search_media);
                } else {
                    text = getResources().getString(R.string.str_no_media);
                }
                this.mTextviewNoMedia.setText(text);
                this.mTextviewNoMedia.setVisibility(0);
            } else {
                this.mTextviewNoMedia.setVisibility(8);
            }
            this.mMediaCount = mediaNum;
        }
    }

    /* access modifiers changed from: private */
    public int loadFolderMedias(int sortMethod, String folder, boolean fuzzy) {
        int num = this.mLocalMediaInfoAdapter.loadFolderMedias(sortMethod, folder, fuzzy);
        setBackgroundTextview(num);
        return num;
    }

    public void showMediaScannerProgressBar(boolean show) {
        int i = 0;
        if (this.mMediaScanProgressView != null) {
            if (show) {
                this.mMediaScanProgressViewRefCounter++;
            } else {
                this.mMediaScanProgressViewRefCounter--;
            }
            if (this.mMediaScanProgressViewRefCounter < 0) {
                this.mMediaScanProgressViewRefCounter = 0;
            }
            View view = this.mMediaScanProgressView;
            if (this.mMediaScanProgressViewRefCounter <= 0) {
                i = 8;
            }
            view.setVisibility(i);
        }
    }

    private void showMediaDetailInfoDialog(int position, boolean isGridView) {
        View layout = getActivity().getLayoutInflater().inflate(R.layout.media_detail_info,
                (ViewGroup) getActivity().findViewById(R.id.media_detail));
        final LocalMediaInfo mediaInfo = this.mLocalMediaInfoAdapter
                .loadDetailMediaInfo(((LocalMediaInfo) this.mThumbsFetcher.getMediaInfo(position)).mKey);
        ((TextView) layout.findViewById(R.id.mediainfo_title)).setText(mediaInfo.mName);
        String path = mediaInfo.mPath;
        ((TextView) layout.findViewById(R.id.mediainfo_pos)).setText(path.substring(0, path.lastIndexOf("/") + 1));
        ((TextView) layout.findViewById(R.id.mediainfo_duration))
                .setText(Utils.stringForTime((long) mediaInfo.mDuration));
        ((TextView) layout.findViewById(R.id.mediainfo_size))
                .setText((((float) Math.round((float) (((mediaInfo.mSize / 1024) * 100) / 1024))) / 100.0f) + "M");
        ((TextView) layout.findViewById(R.id.mediainfo_width))
                .setText(new StringBuilder().append(mediaInfo.mWidth).toString());
        ((TextView) layout.findViewById(R.id.mediainfo_height))
                .setText(new StringBuilder().append(mediaInfo.mHeight).toString());
        new AlertDialog.Builder(getActivity()).setTitle(getResources().getString(R.string.mediainfo_dialog_title))
                .setView(layout)
                .setPositiveButton(getResources().getString(R.string.close), (DialogInterface.OnClickListener) null)
                .setNegativeButton(getResources().getString(R.string.str_delete),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface arg0, int arg1) {
                                VideoThumbsFragment.this.mHandler
                                        .sendMessage(VideoThumbsFragment.this.mHandler.obtainMessage(3, mediaInfo));
                            }
                        })
                .show();
    }

    public void sortMedias(int sortMethod) {
        // Just set the method, preserve the current order (which is in the MSB)
        // Or cleaner: create a separate setSortOrder method.
        // Let's modify this to just set the sort type, but keeping the current order
        // bit.
        int currentOrder = this.mSortMehod & Integer.MIN_VALUE;
        this.mSortMehod = sortMethod | currentOrder;
        this.mHandler.sendMessage(this.mHandler.obtainMessage(0, Integer.valueOf(this.mSortMehod)));
    }

    public void setSortOrder(boolean ascending) {
        int sortType = this.mSortMehod & Integer.MAX_VALUE;
        if (ascending) {
            this.mSortMehod = sortType; // Clear MSB
        } else {
            this.mSortMehod = sortType | Integer.MIN_VALUE; // Set MSB
        }
        this.mHandler.sendMessage(this.mHandler.obtainMessage(0, Integer.valueOf(this.mSortMehod)));
    }

    public void changeAppMode(int appMode) {
        boolean athumbEnable = appMode != 2;
        if (athumbEnable != this.mThumbsFetcher.getAthumbEnable()) {
            this.mThumbsFetcher.setAthumbEnable(athumbEnable);
            if (!athumbEnable) {
                this.mThumbsTextureViewManager.suspendAllTextureView();
            } else {
                this.mAdapter.notifyDataSetChanged();
            }
        }
        this.mHandler.sendMessage(this.mHandler.obtainMessage(1, Integer.valueOf(appMode)));
    }

    public void changeAthumbConfig(boolean athumbEnable) {
        if (athumbEnable != this.mThumbsFetcher.getAthumbEnable()) {
            this.mThumbsFetcher.setAthumbEnable(athumbEnable);
            if (!athumbEnable) {
                this.mThumbsTextureViewManager.suspendAllTextureView();
            } else {
                this.mAdapter.notifyDataSetChanged();
            }
        }
    }

    public void setLoaded(boolean loaded) {
        Log.v(TAG, "mSelectedFolder:" + this.mSelectedFolder);
        loadFolderMedias(this.mSortMehod, this.mSelectedFolder, this.mFuzzy);
        this.mAdapter.notifyDataSetChanged();
    }

    public int getCategory() {
        return this.mCategory;
    }

    public void setCategory(int category) {
        this.mCategory = category;
        if (this.mCategory == 0) {
            this.mSelectedFolder = Constants.MEDIA_ROOT_PATH;
        }
    }

    public void loadFolderMedias(String folder, boolean fuzzy) {
        this.mSelectedFolder = folder;
        this.mFuzzy = fuzzy;
        Log.v(TAG, "loadFolderMedias:" + this.mSelectedFolder);
        loadFolderMedias(this.mSortMehod, folder, fuzzy);
        this.mAdapter.notifyDataSetChanged();
    }

    public void searchMedias(String query, boolean fuzzy) {
        setBackgroundTextview(this.mLocalMediaInfoAdapter.searchMedias(query, fuzzy));
        this.mAdapter.notifyDataSetChanged();
    }

    public void syncBookMark(String path, int bookmark) {
        this.mLocalMediaInfoAdapter.updateTempMediaInfoBookmark(path, bookmark);
    }

    private class VideoThumbsAdapter extends BaseAdapter {
        public VideoThumbsAdapter(Context context) {
        }

        public int getCount() {
            return VideoThumbsFragment.this.mThumbsFetcher.getAdapter().getSize();
        }

        public Object getItem(int position) {
            return VideoThumbsFragment.this.mThumbsFetcher.getAdapter().getItem(position);
        }

        public long getItemId(int position) {
            return (long) position;
        }

        public View getView(int position, View convertView, ViewGroup container) {
            if (convertView == null) {
                convertView = VideoThumbsFragment.this.mLayoutInflater.inflate(R.layout.thumbs_grid_item,
                        (ViewGroup) null);
            }
            VideoThumbsFragment.this.mThumbsFetcher.loadThumbs(position, convertView, 0);
            return convertView;
        }
    }

    public void onFullScreen(boolean isFull) {
        this.mThumbsFetcher.setAthumbEnable(!isFull);
        if (isFull) {
            this.mThumbsTextureViewManager.suspendAllTextureView();
        }
    }

    public void onVideoOpen() {
        if (isVisible() && AppConfig.getInstance(getActivity().getApplicationContext())
                .getBoolean(AppConfig.ATHUMB_ENABLE, true)) {
            this.mThumbsFetcher.setAthumbEnable(false);
            this.mThumbsTextureViewManager.suspendAllTextureView();
            this.mAdapter.notifyDataSetChanged();
        }
    }

    public void onVideoClose() {
        if (isVisible() && AppConfig.getInstance(getActivity().getApplicationContext())
                .getBoolean(AppConfig.ATHUMB_ENABLE, true)) {
            this.mThumbsFetcher.setAthumbEnable(true);
            this.mAdapter.notifyDataSetChanged();
        }
    }

    public boolean onTouch(View v, MotionEvent arg1) {
        switch (v.getId()) {
            case R.id.usage_guide:
                if (!this.showUsage) {
                    return false;
                }
                this.mUsageGuide.setVisibility(8);
                this.showUsage = false;
                AppConfig.getInstance(getActivity().getApplicationContext()).setBoolean(AppConfig.VPLAYER_USAGE1,
                        false);
                return true;
            default:
                return false;
        }
    }
}
