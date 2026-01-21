package com.softwinner.fireplayer.ui;

import android.annotation.TargetApi;
import android.app.KeyguardManager;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.provider.SearchRecentSuggestions;
import androidx.fragment.app.FragmentTransaction;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import androidx.appcompat.widget.SearchView;
import com.slidingmenu.lib.SlidingMenu;
import com.slidingmenu.lib.app.SlidingFragmentActivity;
import com.softwinner.fireplayer.R;
import com.softwinner.fireplayer.constant.Constants;
import com.softwinner.fireplayer.mediamanager.LocalMediaScannerService;
import com.softwinner.fireplayer.provider.LocalMediaProviderContract;
import com.softwinner.fireplayer.provider.MySuggestionProvider;
import com.softwinner.fireplayer.ui.MediaPlayerEventInterface;
import com.softwinner.fireplayer.ui.PresentationScreenMonitor;
import com.softwinner.fireplayer.ui.RemoteControlPanel;
import com.softwinner.fireplayer.ui.VideoFolderFragment;
import com.softwinner.fireplayer.ui.VideoThumbsFragment;
import com.softwinner.fireplayer.util.SimpleNotifyDialog;
import com.softwinner.fireplayer.util.Utils;
import com.softwinner.fireplayer.videoplayerui.FloatVideoManager;
import com.softwinner.fireplayer.videoplayerui.FloatVideoService;
import com.softwinner.fireplayer.videoplayerui.PlayListMediaInfoAdapter;
import com.softwinner.fireplayer.videoplayerui.SettingDialog;
import com.softwinner.fireplayer.videoplayerui.VideoSurfaceTextureView;
import java.lang.ref.WeakReference;

public class FourKMainActivity extends SlidingFragmentActivity implements VideoFolderFragment.onFolderSelectedListener,
        VideoThumbsFragment.OnActivityCallbackListener, SlidingMenu.OnOpenedListener,
        RemoteControlPanel.RemoteControlPanelCallback, PresentationScreenMonitor.PresentationScreenMonitorListener,
        MediaPlayerEventInterface.MeidaPlayerEventListener, AudioManager.OnAudioFocusChangeListener {
    public static final String FKM_ACTION_SWITCH_PRESENTATION = "com.softwinner.fireplayer.action.switch_presentation";
    private static final int MSG_RESUME_PESENTATION_VIDEO = 2;
    public static final int REQUEST_CODE_VPLAYER = 5;
    private static final String TAG = "FourKMainActivity";
    private Thread delayedResumeVideosThread = null;
    /* access modifiers changed from: private */
    public Boolean mActivityOnPause = false;
    private AudioManager mAudioManager;
    private boolean mDimenPxMode;
    /* access modifiers changed from: private */
    public boolean mDismissPresentation = false;
    private FloatVideoManager mFloatVideoManager;
    private FourKBroadcastReceiver mFourKBroadcastReceiver;
    /* access modifiers changed from: private */
    public Handler mHandler = new HandlerExtension(this);
    private boolean mIsRetina = false;
    /* access modifiers changed from: private */
    public boolean mMediaScanned = false;
    private SimpleNotifyDialog mMeidaScannerDialog = null;
    /* access modifiers changed from: private */
    public Menu mMenu = null;
    private DisplayMetrics mMetric;
    private PlayListMediaInfoAdapter mPlaylistMediaInfoAdapter;
    /* access modifiers changed from: private */
    public RemotePresentation mPresentation = null;
    /* access modifiers changed from: private */
    public RemotePresentation mPresentationBackUp = null;
    private boolean mPresentationPlayMode = false;
    /* access modifiers changed from: private */
    public PresentationScreenMonitor mPresentationScreenMonitor;
    /* access modifiers changed from: private */
    public String mPresentationVideoPath = null;
    private RemoteControlPanel mRemoteControlPanel = null;
    /* access modifiers changed from: private */
    public androidx.appcompat.widget.SearchView mSearchView;
    private SettingDialog mSettingDialog = null;
    private SettingsObserver mSettingsObserver;
    /* access modifiers changed from: private */
    public String mSuependPresentationVideoPath = null;
    private SuspendReceiver mSuspendReceiver = null;
    private boolean mSwitchToPresentation;
    /* access modifiers changed from: private */
    public VideoFolderFragment mVideoFolderFragment;
    /* access modifiers changed from: private */
    public VideoSurfaceTextureView mVideoSurfaceTextureViewBackUp = null;
    /* access modifiers changed from: private */
    public VideoThumbsFragment mVideoThumbsFragment;

    private static class HandlerExtension extends Handler {
        WeakReference<FourKMainActivity> mActivity;

        HandlerExtension(FourKMainActivity activity) {
            this.mActivity = new WeakReference<>(activity);
        }

        public void handleMessage(Message msg) {
            FourKMainActivity theActivity = (FourKMainActivity) this.mActivity.get();
            if (theActivity != null) {
                switch (msg.what) {
                    case 2:
                        theActivity.updatePresentation(theActivity.mSuependPresentationVideoPath, true);
                        theActivity.mSuependPresentationVideoPath = null;
                        return;
                    case SettingsObserver.MSG_ACCELEROMETER_ROTATION_SETTING:
                        Utils.customOrientationAdapter(theActivity, AppConfig.CUSTOM_ORIENTATION_UI, 0);
                        return;
                    default:
                        super.handleMessage(msg);
                        return;
                }
            }
        }
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.READ_EXTERNAL_STORAGE }, 101);
        }
        if (savedInstanceState != null) {
            this.mPresentationVideoPath = savedInstanceState.getString("presentation_path");
        }
        this.mMetric = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(this.mMetric);
        AppConfig.getInstance(getApplicationContext());
        this.mDimenPxMode = AppConfig.getInstance(getApplicationContext()).getBoolean(AppConfig.DIMEN_PX_MODE, false);
        this.mFloatVideoManager = FloatVideoManager.getInstance(this);
        screenAdapter();
        this.mFourKBroadcastReceiver = new FourKBroadcastReceiver(this, (FourKBroadcastReceiver) null);
        IntentFilter intentFilter = new IntentFilter(Constants.BROADCAST_ACTION_MEDIASCAN_START);
        intentFilter.addAction(Constants.BROADCAST_ACTION_MEDIASCAN_FINISHED);
        intentFilter.addAction(Constants.BROADCAST_ACTION_BOOKMARK);
        intentFilter.addCategory("android.intent.category.DEFAULT");
        registerReceiver(this.mFourKBroadcastReceiver, intentFilter);
        this.mVideoFolderFragment = (VideoFolderFragment) getSupportFragmentManager()
                .findFragmentByTag(Constants.VIDEO_FOLDER_FRAGMENT_TAG);
        if (this.mVideoFolderFragment != null) {
            getSupportFragmentManager().beginTransaction().remove(this.mVideoFolderFragment);
            this.mVideoFolderFragment = null;
        }
        this.mVideoFolderFragment = new VideoFolderFragment();
        this.mVideoThumbsFragment = (VideoThumbsFragment) getSupportFragmentManager()
                .findFragmentByTag(Constants.VIDEO_THUMBS_FRAGMENT_TAG);
        if (this.mVideoThumbsFragment == null) {
            this.mVideoThumbsFragment = new VideoThumbsFragment();
        }
        setContentView((int) R.layout.content_frame);

        // Set up the Toolbar as the action bar
        androidx.appcompat.widget.Toolbar toolbar = (androidx.appcompat.widget.Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_logo);
        }

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.content_frame, this.mVideoThumbsFragment, Constants.VIDEO_THUMBS_FRAGMENT_TAG).commit();
        setBehindContentView((int) R.layout.menu_frame);
        FragmentTransaction t = getSupportFragmentManager().beginTransaction();
        this.mVideoFolderFragment.setOnFolderSelectedListener(this);
        t.replace(R.id.menu_frame, this.mVideoFolderFragment, Constants.VIDEO_FOLDER_FRAGMENT_TAG);
        t.commit();
        SlidingMenu sm = getSlidingMenu();
        sm.setShadowWidthRes(this.mDimenPxMode ? R.dimen.shadow_width_px : R.dimen.shadow_width);
        sm.setShadowDrawable((int) R.drawable.shadow);
        sm.setBehindWidthRes(this.mDimenPxMode ? R.dimen.sliding_menu_width_px : R.dimen.sliding_menu_width);
        sm.setFadeDegree(0.35f);
        sm.setTouchModeAbove(1);
        sm.setOnOpenedListener(this);
        setSlidingActionBarEnabled(false);
        if (savedInstanceState == null) {
            this.mMediaScanned = AppConfig.getInstance(getApplicationContext()).getBoolean(AppConfig.MEDIA_SCANNED,
                    false);
            if (!this.mMediaScanned) {
                mediaScan(Constants.MEDIA_ROOT_PATH, Constants.ACTION_MEDIASCAN, true, true);
            }
            handleIntent(getIntent());
        }
        Intent intent = getIntent();
        this.mSwitchToPresentation = intent.getBooleanExtra("switchToPresentation", false);
        if (!this.mSwitchToPresentation) {
            AppConfig.getInstance(getApplicationContext()).setBoolean(AppConfig.DISABLE_SWITCH_PRESENTATION, false);
        } else if (AppConfig.getInstance(getApplicationContext()).getBoolean(AppConfig.DISABLE_SWITCH_PRESENTATION,
                false)) {
            this.mSwitchToPresentation = false;
        } else {
            AppConfig.getInstance(getApplicationContext()).setBoolean(AppConfig.DISABLE_SWITCH_PRESENTATION, true);
            this.mPresentationVideoPath = intent.getStringExtra(LocalMediaProviderContract.PATH_COLUMN);
        }
        Log.v(TAG, "onCreate() end mSwitchToPresentation=" + this.mSwitchToPresentation + " mPresentationVideoPath="
                + this.mPresentationVideoPath);
        this.mPresentationScreenMonitor = PresentationScreenMonitor.getInstance(getApplicationContext());
        if (this.mSuspendReceiver == null) {
            this.mSuspendReceiver = new SuspendReceiver();
            IntentFilter filter = new IntentFilter("android.intent.action.SCREEN_OFF");
            filter.addAction("android.intent.action.SCREEN_ON");
            registerReceiver(this.mSuspendReceiver, filter);
        }
        this.mSettingsObserver = new SettingsObserver(this, this.mHandler);
        this.mAudioManager = (AudioManager) getSystemService("audio");
        this.mPlaylistMediaInfoAdapter = PlayListMediaInfoAdapter.getInstance(getApplicationContext());
        mediaScan(Constants.MEDIA_ROOT_PATH, Constants.ACTION_MEDIASCAN, false, false);
    }

    /* access modifiers changed from: protected */
    public void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        Log.v(TAG, "handleIntent:" + intent.getAction());
        if (!"android.intent.action.VIEW".equals(intent.getAction())) {
            if ("android.intent.action.SEARCH".equals(intent.getAction())) {
                if (this.mSearchView != null) {
                    String query = intent.getStringExtra("query");
                    this.mSearchView.setQueryHint(query);
                    new SearchRecentSuggestions(this, MySuggestionProvider.AUTHORITY, 3).saveRecentQuery(query,
                            (String) null);
                    showSearchResults(query);
                }
                this.mActivityOnPause = false;
            } else if (FKM_ACTION_SWITCH_PRESENTATION.equals(intent.getAction())) {
                this.mSwitchToPresentation = intent.getBooleanExtra("switchToPresentation", false);
                if (this.mSwitchToPresentation) {
                    this.mPresentationVideoPath = intent.getStringExtra(LocalMediaProviderContract.PATH_COLUMN);
                }
            }
        }
    }

    private void showSearchResults(String query) {
        setCategory(5);
        this.mVideoThumbsFragment.searchMedias(query, true);
        getSlidingMenu().showContent();
    }

    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("presentation_path", this.mPresentationVideoPath);
    }

    public void onDestroy() {
        if (this.mFourKBroadcastReceiver != null) {
            unregisterReceiver(this.mFourKBroadcastReceiver);
            this.mFourKBroadcastReceiver = null;
        }
        if (this.mSuspendReceiver != null) {
            unregisterReceiver(this.mSuspendReceiver);
            this.mSuspendReceiver = null;
        }
        ((AudioManager) getSystemService("audio")).abandonAudioFocus((AudioManager.OnAudioFocusChangeListener) null);
        super.onDestroy();
    }

    public void onPause() {
        super.onPause();
        if (AppConfig.getInstance(getApplicationContext()).getBoolean(AppConfig.PRESENTATION_BG_DISABLE, false)) {
            this.mDismissPresentation = true;
            if (this.mPresentation != null) {
                this.mPresentationBackUp = this.mPresentation;
                this.mVideoSurfaceTextureViewBackUp = this.mPresentation.getVideoSurfaceTextureView();
            }
            dismissPresentation();
        }
        dissmissSettingDialog();
        this.mActivityOnPause = true;
        this.mSettingsObserver.unRegisterObserver();
        Log.v(TAG, "onPause()");
    }

    /* access modifiers changed from: protected */
    public void onResume() {
        super.onResume();
        this.mSettingsObserver.registerObserver();
        Utils.customOrientationAdapter(this, AppConfig.CUSTOM_ORIENTATION_UI, 0);
        this.mActivityOnPause = false;
        Log.v(TAG, ">>>> onResume() mPresentation=" + this.mPresentation + " switch=" + this.mSwitchToPresentation
                + " getPresentation=" + this.mPresentationScreenMonitor.getPresentation());
        if (!(this.mPresentation == null || this.mPresentation == this.mPresentationScreenMonitor.getPresentation())) {
            RemoteControlPanelShow(false, false);
            dissmissSettingDialog();
            this.mPresentation = null;
        }
        if (this.mPresentation == null && this.mPresentationScreenMonitor.getPresentation() != null) {
            this.mPresentation = this.mPresentationScreenMonitor.getPresentation();
            this.mPresentation.setMeidaPlayerEventListener(this);
            RemoteControlPanelShow(true, true);
        }
        this.mPresentationScreenMonitor.setListener(0, this);
        if (this.mSwitchToPresentation) {
            updatePresentation(this.mPresentationVideoPath, true);
            this.mSwitchToPresentation = false;
        }
    }

    public void onBackPressed() {
        this.mPresentationVideoPath = null;
        this.mFloatVideoManager.closeAllWindows();
        dismissPresentation();
        stopService(new Intent(this, FloatVideoService.class));
        finish();
        Process.killProcess(Process.myPid());
    }

    /* access modifiers changed from: protected */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        this.mSwitchToPresentation = false;
        if (data != null) {
            Bundle bundle = data.getExtras();
            if (bundle.getBoolean("clearPresentationPath", false)) {
                this.mSwitchToPresentation = false;
                this.mPresentationVideoPath = null;
                return;
            }
            this.mSwitchToPresentation = bundle.getBoolean("switchToPresentation", false);
            this.mPresentationVideoPath = bundle.getString(LocalMediaProviderContract.PATH_COLUMN);
        }
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        boolean z;
        boolean z2 = false;
        switch (item.getItemId()) {
            case 16908332:
                toggle();
                return true;
            case R.id.sort_by_alpha:
                this.mVideoThumbsFragment.sortMedias(1);
                this.mPlaylistMediaInfoAdapter.setSortFlag(1);
                getSlidingMenu().showContent();
                return true;
            case R.id.sort_by_size:
                this.mVideoThumbsFragment.sortMedias(2);
                this.mPlaylistMediaInfoAdapter.setSortFlag(2);
                getSlidingMenu().showContent();
                return true;
            case R.id.sort_by_date:
                this.mVideoThumbsFragment.sortMedias(5); // 5 is SORT_BY_DATE
                this.mPlaylistMediaInfoAdapter.setSortFlag(5);
                getSlidingMenu().showContent();
                return true;
            case R.id.sort_asc:
                boolean isAsc = !item.isChecked();
                item.setChecked(isAsc);
                this.mVideoThumbsFragment.setSortOrder(isAsc);
                // Also update PlaylistMediaInfoAdapter if it supports order, but
                // mPlaylistMediaInfoAdapter.setSortFlag takes int.
                // Assuming it might share same logic or we ignore it for now as user just asked
                // for app sort.
                getSlidingMenu().showContent();
                return true;
            case R.id.refresh:
                setCategory(0);
                mediaScan(Constants.MEDIA_ROOT_PATH, Constants.ACTION_MEDIASCAN, true, true);
                this.mSearchView.setIconified(true);
                getSlidingMenu().showContent();
                return true;
            case R.id.menu_athumb:
                boolean ischecked = item.isChecked();
                if (!ischecked) {
                    z2 = true;
                }
                item.setChecked(z2);
                AppConfig.getInstance(getApplicationContext()).setBoolean(AppConfig.ATHUMB_ENABLE, ischecked);
                this.mVideoThumbsFragment.changeAthumbConfig(ischecked);
                return true;
            case R.id.menu_dis_presentation:
                boolean ischecked2 = item.isChecked();
                if (ischecked2) {
                    z = false;
                } else {
                    z = true;
                }
                item.setChecked(z);
                AppConfig.getInstance(getApplicationContext()).setBoolean(AppConfig.PRESENTATION_ENABLE, ischecked2);
                this.mPresentationScreenMonitor.setPresentationEnable(ischecked2);
                if (ischecked2 || this.mPresentationVideoPath == null) {
                    return true;
                }
                updatePresentation(this.mPresentationVideoPath, false);
                return true;
            case R.id.menu_vfloatwin:
                boolean ischecked3 = item.isChecked();
                if (!ischecked3) {
                    z2 = true;
                }
                item.setChecked(z2);
                AppConfig.getInstance(getApplicationContext()).setBoolean(AppConfig.DEFAULT_OPEN_FULLSCREEN,
                        ischecked3);
                return true;
            case R.id.menu_tile:
                this.mFloatVideoManager.tile();
                return true;
            case R.id.menu_stack:
                this.mFloatVideoManager.stack();
                return true;
            case R.id.menu_clearplaylist:
                AppConfig.getInstance(getApplicationContext()).setInt(AppConfig.RECORDED_PLAYTIME,
                        AppConfig.getInstance(getApplicationContext()).getInt(AppConfig.LATEST_PLAYTIME, 0));
                this.mVideoFolderFragment.loadFolders(false);
                this.mVideoThumbsFragment.setLoaded(true);
                updateAppwidget();
                return true;
            case R.id.menu_clearhistory:
                this.mSearchView.setIconified(true);
                clearSearchHistory();
                return true;
            case R.id.menu_exit:
                stopService(new Intent(this, FloatVideoService.class));
                finish();
                Process.killProcess(Process.myPid());
                return true;
            case R.id.menu_about:
                showAboutDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        boolean z;
        boolean z2;
        boolean z3;
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main_menu, menu);
        this.mMenu = menu;
        menu.findItem(R.id.menu_athumb).setVisible(true);
        MenuItem findItem = menu.findItem(R.id.menu_athumb);
        if (AppConfig.getInstance(getApplicationContext()).getBoolean(AppConfig.ATHUMB_ENABLE, true)) {
            z = false;
        } else {
            z = true;
        }
        findItem.setChecked(z);
        MenuItem findItem2 = menu.findItem(R.id.menu_dis_presentation);
        if (AppConfig.getInstance(getApplicationContext()).getBoolean(AppConfig.PRESENTATION_ENABLE, true)) {
            z2 = false;
        } else {
            z2 = true;
        }
        findItem2.setChecked(z2);
        if (AppConfig.getInstance(getApplicationContext()).getInt(AppConfig.CUSTOM_SOTHER, 1) > 1) {
            menu.findItem(R.id.menu_vfloatwin).setVisible(true);
            MenuItem findItem3 = menu.findItem(R.id.menu_vfloatwin);
            if (AppConfig.getInstance(getApplicationContext()).getBoolean(AppConfig.DEFAULT_OPEN_FULLSCREEN, true)) {
                z3 = false;
            } else {
                z3 = true;
            }
            findItem3.setChecked(z3);
        } else {
            menu.findItem(R.id.menu_vfloatwin).setVisible(false);
        }
        menu.findItem(R.id.menu_tile).setVisible(false);
        menu.findItem(R.id.menu_stack).setVisible(false);
        if (AppConfig.getInstance(getApplicationContext()).getBoolean(AppConfig.HIDEMENU_ATHUMB, false)) {
            menu.findItem(R.id.menu_athumb).setVisible(false);
        }
        if (AppConfig.getInstance(getApplicationContext()).getBoolean(AppConfig.HIDEMENU_PRESENTATION, false)) {
            menu.findItem(R.id.menu_dis_presentation).setVisible(false);
        }
        this.mSearchView = (androidx.appcompat.widget.SearchView) menu.findItem(R.id.menu_search).getActionView();
        this.mSearchView
                .setSearchableInfo(((SearchManager) getSystemService("search")).getSearchableInfo(getComponentName()));
        this.mSearchView.setSubmitButtonEnabled(true);
        this.mSearchView.setIconifiedByDefault(true);
        this.mSearchView.setFocusable(false);
        this.mSearchView.setOnCloseListener(new androidx.appcompat.widget.SearchView.OnCloseListener() {
            public boolean onClose() {
                FourKMainActivity.this.mSearchView.setQueryHint((CharSequence) null);
                if (FourKMainActivity.this.mVideoThumbsFragment.getCategory() == 5) {
                    FourKMainActivity.this.setCategory(0);
                    FourKMainActivity.this.mVideoThumbsFragment.loadFolderMedias(Constants.MEDIA_ROOT_PATH, true);
                }
                return false;
            }
        });
        return true;
    }

    /* access modifiers changed from: private */
    public void setCategory(int category) {
        this.mVideoThumbsFragment.setCategory(category);
        this.mVideoFolderFragment.setCategory(category);
    }

    /* access modifiers changed from: private */
    public void updateAppwidget() {
    }

    private void clearSearchHistory() {
        new SearchRecentSuggestions(this, MySuggestionProvider.AUTHORITY, 3).clearHistory();
    }

    private void mediaScan(String rootPath, String action, boolean showDialog, boolean isFile) {
        if (this.mMenu != null) {
            this.mMenu.findItem(R.id.refresh).setEnabled(false);
        }
        if (showDialog) {
            showMediaScannerDialog(true);
        }
        startService(new Intent(this, LocalMediaScannerService.class).setAction(action)
                .putExtra(LocalMediaScannerService.EXTRA_DEVPATH, rootPath)
                .putExtra(LocalMediaScannerService.EXTRA_SCANTYPE, isFile));
    }

    /* access modifiers changed from: private */
    public void showMediaScannerDialog(boolean visible) {
        if (visible && this.mMeidaScannerDialog == null) {
            this.mMeidaScannerDialog = new SimpleNotifyDialog(this, R.style.SimpleDialog);
            this.mMeidaScannerDialog.setView(R.layout.media_sanning);
            this.mMeidaScannerDialog.setCancelable(false);
            this.mMeidaScannerDialog.show();
        } else if (this.mMeidaScannerDialog != null) {
            this.mMeidaScannerDialog.dismiss();
            this.mMeidaScannerDialog = null;
        }
    }

    private void screenAdapter() {
        Log.v(TAG, "densityDpi=" + this.mMetric.densityDpi + " widthPixels=" + this.mMetric.widthPixels
                + " heightPixels=" + this.mMetric.heightPixels);
        if (this.mMetric.densityDpi > 240 && this.mMetric.widthPixels * this.mMetric.heightPixels >= 2073600) {
            this.mIsRetina = true;
        }
        if (this.mIsRetina) {
            AppConfig.getInstance(getApplicationContext()).setBoolean(AppConfig.RETINA_SCREEN, this.mIsRetina);
        }
    }

    private class FourKBroadcastReceiver extends BroadcastReceiver {
        private FourKBroadcastReceiver() {
        }

        /* synthetic */ FourKBroadcastReceiver(FourKMainActivity fourKMainActivity,
                FourKBroadcastReceiver fourKBroadcastReceiver) {
            this();
        }

        public void onReceive(Context context, Intent intent) {
            Log.v(FourKMainActivity.TAG, "onReceive" + intent.getAction());
            if (intent.getAction() == Constants.BROADCAST_ACTION_MEDIASCAN_START) {
                if (!FourKMainActivity.this.mActivityOnPause.booleanValue()) {
                    FourKMainActivity.this.mVideoThumbsFragment.showMediaScannerProgressBar(true);
                }
            } else if (intent.getAction() == Constants.BROADCAST_ACTION_MEDIASCAN_FINISHED) {
                FourKMainActivity.this.showMediaScannerDialog(false);
                if (!FourKMainActivity.this.mMediaScanned) {
                    AppConfig.getInstance(FourKMainActivity.this).setBoolean(AppConfig.MEDIA_SCANNED, true);
                }
                Log.v(FourKMainActivity.TAG, "mActivityOnPause = " + FourKMainActivity.this.mActivityOnPause);
                if (!FourKMainActivity.this.mActivityOnPause.booleanValue()) {
                    FourKMainActivity.this.mVideoFolderFragment.loadFolders(true);
                    FourKMainActivity.this.setCategory(0);
                    FourKMainActivity.this.mVideoThumbsFragment.setLoaded(true);
                    FourKMainActivity.this.mVideoThumbsFragment.showMediaScannerProgressBar(false);
                    FourKMainActivity.this.updateAppwidget();
                }
                if (FourKMainActivity.this.mMenu != null) {
                    FourKMainActivity.this.mMenu.findItem(R.id.refresh).setEnabled(true);
                }
            } else if (intent.getAction() == Constants.BROADCAST_ACTION_BOOKMARK) {
                if (FourKMainActivity.this.mVideoThumbsFragment != null
                        && FourKMainActivity.this.mVideoThumbsFragment.isVisible()) {
                    FourKMainActivity.this.mVideoThumbsFragment.syncBookMark(
                            intent.getStringExtra(Constants.EXTENDED_BOOKMARK_PATH),
                            intent.getIntExtra(Constants.EXTENDED_BOOKMARK_DURATION, 0));
                } else {
                    return;
                }
            }
            Log.v(FourKMainActivity.TAG, "onReceive end");
        }
    }

    public void onFolderSelected(String folder, boolean fuzzy, boolean collapse, boolean groupFolder, int position) {
        int category = 4;
        if (groupFolder && position < Constants.PREDEF_CATEGORY.length) {
            category = position;
        }
        setCategory(category);
        this.mVideoThumbsFragment.loadFolderMedias(folder, fuzzy);
        if (collapse) {
            getSlidingMenu().showContent();
        }
    }

    public void onOpened() {
        this.mVideoFolderFragment.loadFolders(false);
    }

    @TargetApi(17)
    public synchronized boolean updatePresentation(String path, boolean reload) {
        boolean z = false;
        synchronized (this) {
            if (Utils.isSdkJB42OrAbove()) {
                boolean hasDismissPresentation = false;
                Display presentationDisplay = this.mPresentationScreenMonitor.getPresentationDisplay();
                this.mPresentationVideoPath = path;
                if (this.mPresentation != null) {
                    dismissPresentation();
                    hasDismissPresentation = true;
                    if (this.mActivityOnPause.booleanValue()) {
                        path = null;
                        this.mPresentationVideoPath = null;
                    }
                }
                Log.v(TAG,
                        ">>>>updatePresentation mPresentation=" + this.mPresentation + " presentationDisplay="
                                + presentationDisplay + " getPresentation="
                                + this.mPresentationScreenMonitor.getPresentation());
                if (this.mPresentationScreenMonitor.getPresentation() != null) {
                    this.mPresentationScreenMonitor.getPresentation().dismiss();
                    this.mPresentationScreenMonitor.setPresentation((RemotePresentation) null);
                }
                if (!(this.mPresentation != null || presentationDisplay == null || path == null)) {
                    Log.i(TAG, "Showing presentation on display: " + presentationDisplay);
                    this.mFloatVideoManager.closeAllWindows();
                    enableAnimationThumb(false);
                    hasDismissPresentation = false;
                    this.mPresentation = new RemotePresentation(getApplicationContext(), presentationDisplay, path);
                    this.mPresentation.setMeidaPlayerEventListener(this);
                    this.mPresentationScreenMonitor.setPresentation(this.mPresentation);
                    Window win = this.mPresentation.getWindow();
                    win.setType(2005);
                    win.setFormat(2);
                    WindowManager.LayoutParams winParams = win.getAttributes();
                    winParams.buttonBrightness = 0.0f;
                    winParams.flags |= 1024;
                    winParams.alpha = 1.0f;
                    win.setAttributes(winParams);
                    win.setBackgroundDrawable((Drawable) null);
                    this.mPresentation.setCancelable(false);
                    requestAudioFocus();
                    try {
                        this.mPresentation.show();
                    } catch (WindowManager.InvalidDisplayException ex) {
                        Log.w(TAG, "Couldn't show presentation!  Display was removed in the meantime.", ex);
                        this.mPresentation = null;
                    }
                    RemoteControlPanelShow(true, false);
                    this.mPresentationPlayMode = true;
                    this.mSuependPresentationVideoPath = null;
                }
                if (reload && this.mPresentation == null && path != null) {
                    Intent i = new Intent(this, VideoPlayerActivity.class);
                    i.putExtra(LocalMediaProviderContract.PATH_COLUMN, path);
                    i.putExtra("boot-mode", "internal");
                    startActivityForResult(i, 5);
                    this.mPresentationPlayMode = false;
                }
                if (hasDismissPresentation) {
                    enableAnimationThumb(true);
                }
                if (this.mPresentation != null) {
                    z = true;
                }
            }
        }
        return z;
    }

    public boolean onVideoItemClick(String path) {
        return updatePresentation(path, false);
    }

    /* access modifiers changed from: private */
    public void toggleSettingDialog() {
        if (this.mSettingDialog == null) {
            this.mSettingDialog = new SettingDialog(this, R.style.SettingDialog,
                    this.mPresentation.getVideoSurfaceTextureView());
            this.mSettingDialog.setCancelable(true);
        }
        if (!this.mSettingDialog.isShowing()) {
            this.mSettingDialog.show();
        } else {
            dissmissSettingDialog();
        }
    }

    private void dissmissSettingDialog() {
        if (this.mSettingDialog != null) {
            this.mSettingDialog.dismiss();
            this.mSettingDialog = null;
        }
    }

    private void RemoteControlPanelShow(boolean show, boolean auto) {
        FrameLayout fl = (FrameLayout) findViewById(R.id.content_frame);
        if (show) {
            if (this.mPresentation != null) {
                if (this.mRemoteControlPanel == null) {
                    this.mRemoteControlPanel = new RemoteControlPanel(this,
                            this.mPresentation.getVideoSurfaceTextureView(), this.mHandler) {
                        public void onSettingButtonClick(View v) {
                            FourKMainActivity.this.toggleSettingDialog();
                        }
                    };
                    this.mRemoteControlPanel.setListener(this);
                    View remoteControlView = this.mRemoteControlPanel.getView();
                    fl.addView(remoteControlView, this.mRemoteControlPanel.getLayoutParams());
                    remoteControlView.setTag("remoteControlBar");
                } else {
                    this.mRemoteControlPanel.updateVideoView(this.mPresentation.getVideoSurfaceTextureView());
                }
                this.mRemoteControlPanel.getView().setVisibility(0);
                this.mRemoteControlPanel.onShow(auto);
            }
        } else if (this.mRemoteControlPanel != null && this.mRemoteControlPanel.getView() != null) {
            this.mRemoteControlPanel.getView().setVisibility(8);
        }
    }

    @TargetApi(17)
    private void dismissPresentation() {
        if (this.mPresentation != null) {
            VideoSurfaceTextureView vv = this.mPresentation.getVideoSurfaceTextureView();
            if (vv != null) {
                vv.hideControls();
                vv.suspend();
            }
            RemoteControlPanelShow(false, false);
            dissmissSettingDialog();
            this.mPresentation.dismiss();
            this.mPresentationScreenMonitor.setPresentationToNull(this.mPresentation);
            this.mPresentation = null;
        }
    }

    private void enableAnimationThumb(boolean enable) {
        boolean z = true;
        if (AppConfig.getInstance(getApplicationContext()).getBoolean(AppConfig.ATHUMB_ENABLE, true)
                && this.mVideoThumbsFragment != null) {
            VideoThumbsFragment videoThumbsFragment = this.mVideoThumbsFragment;
            if (enable) {
                z = false;
            }
            videoThumbsFragment.onFullScreen(z);
        }
    }

    /* access modifiers changed from: private */
    @TargetApi(16)
    public void delayedResumeVideos() {
        if (this.mSuependPresentationVideoPath != null) {
            if (!((KeyguardManager) getSystemService("keyguard")).isKeyguardLocked()) {
                this.mPresentationVideoPath = this.mSuependPresentationVideoPath;
            } else if (this.delayedResumeVideosThread == null || !this.delayedResumeVideosThread.isAlive()) {
                this.delayedResumeVideosThread = new Thread(new Runnable() {
                    public void run() {
                        KeyguardManager km = (KeyguardManager) FourKMainActivity.this.getSystemService("keyguard");
                        while (km.isKeyguardLocked()) {
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        FourKMainActivity.this.mHandler.sendEmptyMessageDelayed(2, 200);
                    }
                });
                this.delayedResumeVideosThread.start();
            } else {
                Log.v(TAG, "Thread is running");
            }
        }
    }

    class SuspendReceiver extends BroadcastReceiver {
        SuspendReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            Log.d(FourKMainActivity.TAG, " suspend receiver action " + intent.getAction());
            String action = intent.getAction();
            if (action.equals("android.intent.action.SCREEN_OFF")) {
                if (FourKMainActivity.this.mPresentationScreenMonitor.getPresentationDisplay() != null
                        && FourKMainActivity.this.mSuependPresentationVideoPath == null) {
                    FourKMainActivity.this.mSuependPresentationVideoPath = FourKMainActivity.this.mPresentationVideoPath;
                    boolean presentation_bg_disable = AppConfig
                            .getInstance(FourKMainActivity.this.getApplicationContext())
                            .getBoolean(AppConfig.PRESENTATION_BG_DISABLE, false);
                    if (FourKMainActivity.this.mPresentation != null
                            && FourKMainActivity.this.mPresentation.getVideoSurfaceTextureView() != null) {
                        FourKMainActivity.this.mPresentation.getVideoSurfaceTextureView().pause();
                    } else if (presentation_bg_disable && FourKMainActivity.this.mDismissPresentation) {
                        FourKMainActivity.this.mDismissPresentation = false;
                        FourKMainActivity.this.mPresentation = FourKMainActivity.this.mPresentationBackUp;
                        FourKMainActivity.this.mVideoSurfaceTextureViewBackUp.pause();
                    }
                }
            } else if (action.equals("android.intent.action.SCREEN_ON")) {
                FourKMainActivity.this.delayedResumeVideos();
            }
        }
    }

    public void onRemoteMediaClose() {
        this.mPresentationVideoPath = null;
        dismissPresentation();
        enableAnimationThumb(true);
    }

    public void onCompletion() {
        this.mRemoteControlPanel.updatePlayBttnDrawable(true);
        dissmissSettingDialog();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mediaScan(Constants.MEDIA_ROOT_PATH, Constants.ACTION_MEDIASCAN, true, true);
            } else {
                com.softwinner.fireplayer.util.Utils.makeToast(this, "Storage permission is required to list videos.");
            }
        }
    }

    private void showAboutDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_about, null);
        builder.setView(dialogView);
        builder.setPositiveButton(R.string.common_ok, null);
        android.app.AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void onNewSource(String path) {
        this.mPresentationVideoPath = path;
        this.mRemoteControlPanel.updateTitleView();
        dissmissSettingDialog();
    }

    public void onPresentationDisplayChanged(Display presentationDisplay) {
        updatePresentation(this.mPresentationVideoPath, true);
    }

    public void onMediaPlayerError() {
        this.mPresentationVideoPath = null;
        dismissPresentation();
    }

    private void requestAudioFocus() {
        this.mAudioManager.requestAudioFocus(this, 3, 1);
    }

    public void onAudioFocusChange(int focusChange) {
        Log.v(TAG, "onAudioFocusChange " + focusChange);
        switch (focusChange) {
            case -1:
                if (this.mPresentationPlayMode) {
                    this.mPresentationVideoPath = null;
                    this.mSuependPresentationVideoPath = null;
                    dismissPresentation();
                    enableAnimationThumb(true);
                    return;
                }
                return;
            default:
                return;
        }
    }
}
