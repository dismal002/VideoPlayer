package com.softwinner.fireplayer.videoplayerui;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewAnimator;
import com.softwinner.fireplayer.R;
import com.softwinner.fireplayer.ui.AppConfig;
import com.softwinner.fireplayer.util.Utils;
import com.softwinner.fireplayer.videoplayerui.ColorPickerView;
import com.softwinner.fireplayer.videoplayerui.VideoSurfaceTextureView;
import java.util.ArrayList;

public class SettingDialog extends Dialog implements ColorPickerView.OnColorChangedListener {
    private static final int SUBTITLE_ADJUST_BAR_MAX = 2000;
    private static final int SUBTITLE_ADJUST_BAR_MIN = -2000;
    public static final int SUBTITLE_SIZE_BAR_MAX = 40;
    public static final int SUBTITLE_SIZE_BAR_MIN = 20;
    private static final String TAG = "SettingPanelItem";
    private static boolean mShow3DMenu = true;
    /* access modifiers changed from: private */
    public ListView m3DModeListV;
    /* access modifiers changed from: private */
    public String[] m3DModes;
    private SeekBar mAdjustBar;
    /* access modifiers changed from: private */
    public String[] mCharsetDisps;
    /* access modifiers changed from: private */
    public String[] mCharsetValues;
    /* access modifiers changed from: private */
    public ListView mCodingListV;
    private View mCodingSelector;
    private LinearLayout mColorLayout;
    private ColorPickerView mColorPickerView;
    private AppConfig mConfig;
    /* access modifiers changed from: private */
    public final Context mContext;
    /* access modifiers changed from: private */
    public AlwaysMarqueeTextView mCurCoding;
    /* access modifiers changed from: private */
    public AlwaysMarqueeTextView mCurSubtitle;
    /* access modifiers changed from: private */
    public TextView mCurrentAdjust;
    /* access modifiers changed from: private */
    public TextView mCurrentSize;
    private CheckedTextView mDemoModeSwitch;
    private FrameLayout.LayoutParams mLayoutParams;
    private ListView mRepeatListV;
    private SeekBar mSizeBar;
    /* access modifiers changed from: private */
    public ListView mSubtitleListV;
    private View mSubtitleSelector;
    /* access modifiers changed from: private */
    public ViewAnimator mSubtitleSettings;
    private CheckedTextView mSubtitleSwitch;
    /* access modifiers changed from: private */
    public String[] mSubtitles;
    private TabHost mTabHost;
    private ListView mTrackListV;
    /* access modifiers changed from: private */
    public String[] mTracks;
    /* access modifiers changed from: private */
    public final VideoSurfaceTextureView mVideoView;
    private View mView;

    public SettingDialog(Context context, int theme, VideoSurfaceTextureView videoView) {
        super(context, theme);
        this.mContext = context;
        this.mConfig = AppConfig.getInstance(context);
        this.mVideoView = videoView;
    }

    /* access modifiers changed from: protected */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mShow3DMenu = !this.mConfig.getBoolean(AppConfig.CFG_HIDE_3D_MENU, false);
        this.mView = View.inflate(this.mContext, R.layout.vplayer_setting, (ViewGroup) null);
        setContentView(this.mView);
        this.mTabHost = (TabHost) this.mView.findViewById(R.id.tabhost);
        this.mTabHost.setup();
        this.mTabHost.addTab(this.mTabHost.newTabSpec("RepeatMode").setIndicator(getTabIndicator(R.string.tab_repeat))
                .setContent(R.id.tab_repeat));
        this.mTabHost.addTab(this.mTabHost.newTabSpec("Subtitle").setIndicator(getTabIndicator(R.string.tab_subtitle))
                .setContent(R.id.tab_subtitle));
        this.mTabHost.addTab(this.mTabHost.newTabSpec("Track").setIndicator(getTabIndicator(R.string.tab_track))
                .setContent(R.id.tab_track));
        if (mShow3DMenu) {
            this.mTabHost.addTab(this.mTabHost.newTabSpec("3DMode").setIndicator(getTabIndicator(R.string.tab_3d))
                    .setContent(R.id.tab_3d));
        }
        this.mTabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            public void onTabChanged(String tag) {
                SettingDialog.this.mSubtitleSettings.setDisplayedChild(0);
            }
        });
        this.mRepeatListV = (ListView) this.mView.findViewById(R.id.play_mode_list);
        this.mRepeatListV.setAdapter(new ArrayAdapter<>(this.mContext, R.layout.vplayer_list_item_single_choice,
                this.mContext.getResources().getStringArray(R.array.repeat_mode_items)));
        this.mRepeatListV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> adapterView, View v, int position, long id) {
                SettingDialog.this.mVideoView.setPlayMode(position);
                SettingDialog.this.mConfig.setInt(AppConfig.PlayMode, position);
            }
        });
        this.mDemoModeSwitch = (CheckedTextView) this.mView.findViewById(R.id.demo_mode_switch);
        if (this.mConfig.getBoolean(AppConfig.ENABLE_DEMO_MODE, false)) {
            this.mDemoModeSwitch.setChecked(this.mConfig.getBoolean(AppConfig.DEMO_MODE, false));
            this.mDemoModeSwitch.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    CheckedTextView checkV = (CheckedTextView) v;
                    checkV.toggle();
                    SettingDialog.this.mConfig.setBoolean(AppConfig.DEMO_MODE, checkV.isChecked());
                }
            });
        } else {
            this.mDemoModeSwitch.setVisibility(4);
        }
        this.mSubtitleSettings = (ViewAnimator) this.mView.findViewById(R.id.tab_subtitle);
        this.mSubtitleListV = (ListView) this.mView.findViewById(R.id.subtitle_list);
        this.mCodingListV = (ListView) this.mView.findViewById(R.id.subtitle_coding_list);
        this.mSubtitleSwitch = (CheckedTextView) this.mView.findViewById(R.id.subtitle_switch);
        this.mSubtitleSelector = this.mView.findViewById(R.id.subtitle_selector);
        this.mCurSubtitle = (AlwaysMarqueeTextView) this.mView.findViewById(R.id.current_subtitle);
        this.mAdjustBar = (SeekBar) this.mView.findViewById(R.id.adjust_bar);
        this.mCurrentAdjust = (TextView) this.mView.findViewById(R.id.current_adjust);
        this.mCodingSelector = this.mView.findViewById(R.id.subtitle_coding);
        this.mCurCoding = (AlwaysMarqueeTextView) this.mView.findViewById(R.id.current_coding);
        this.mSizeBar = (SeekBar) this.mView.findViewById(R.id.size_bar);
        this.mCurrentSize = (TextView) this.mView.findViewById(R.id.current_size);
        this.mCharsetValues = this.mContext.getResources().getStringArray(R.array.subtitle_charset_values);
        this.mCharsetDisps = this.mContext.getResources().getStringArray(R.array.subtitle_charset_list);
        this.mCodingListV.setAdapter(
                new ArrayAdapter(this.mContext, R.layout.vplayer_list_item_single_choice, this.mCharsetDisps));
        this.mAdjustBar.setMax(4000);
        this.mAdjustBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {
                if (fromuser) {
                    int mod = progress % 100;
                    int progress2 = progress - mod;
                    if (mod >= 50) {
                        progress2 += 100;
                    }
                    int subDelay = progress2 + SettingDialog.SUBTITLE_ADJUST_BAR_MIN;
                    SettingDialog.this.mVideoView.setSubDelay(subDelay);
                    SettingDialog.this.mCurrentAdjust
                            .setText(String.valueOf(new Float(((double) subDelay) / 1000.0d).toString()) + "s");
                }
            }

            public void onStartTrackingTouch(SeekBar bar) {
            }

            public void onStopTrackingTouch(SeekBar bar) {
            }
        });
        this.mSubtitleSwitch.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                CheckedTextView checkV = (CheckedTextView) v;
                checkV.toggle();
                boolean check = checkV.isChecked();
                SettingDialog.this.mVideoView.setSubGate(check);
                SettingDialog.this.mConfig.setBoolean(AppConfig.SUBGATE, check);
                SettingDialog.this.updateSubtitleSetting(check);
            }
        });
        this.mSubtitleSelector.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (SettingDialog.this.mSubtitles != null && SettingDialog.this.mSubtitles.length > 0) {
                    SettingDialog.this.mSubtitleSettings.setDisplayedChild(1);
                    SettingDialog.this.mSubtitleListV
                            .smoothScrollToPosition(SettingDialog.this.mSubtitleListV.getCheckedItemPosition());
                }
            }
        });
        this.mSubtitleListV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> adapterView, View v, int position, long id) {
                SettingDialog.this.mVideoView.switchSub(position);
                SettingDialog.this.mCurSubtitle.setText(SettingDialog.this.mSubtitles[position]);
                SettingDialog.this.mSubtitleSettings.setDisplayedChild(0);
            }
        });
        this.mCodingSelector.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (SettingDialog.this.mSubtitles != null && SettingDialog.this.mSubtitles.length > 0) {
                    SettingDialog.this.mSubtitleSettings.setDisplayedChild(2);
                    SettingDialog.this.mCodingListV
                            .smoothScrollToPosition(SettingDialog.this.mCodingListV.getCheckedItemPosition());
                }
            }
        });
        this.mCodingListV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> adapterView, View v, int position, long id) {
                SettingDialog.this.mVideoView.setSubCharset(SettingDialog.this.mCharsetValues[position]);
                SettingDialog.this.mCurCoding.setText(SettingDialog.this.mCharsetDisps[position]);
                SettingDialog.this.mConfig.setInt(AppConfig.SUBCODING, position);
                SettingDialog.this.mSubtitleSettings.setDisplayedChild(0);
            }
        });
        this.mSizeBar.setMax(20);
        this.mSizeBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {
                if (fromuser) {
                    int size = progress + 20;
                    SettingDialog.this.mVideoView.setSubFontSize(size);
                    SettingDialog.this.mCurrentSize.setText(new Integer(size).toString());
                    SettingDialog.this.mConfig.setInt(AppConfig.SUBSIZE, size);
                }
            }

            public void onStartTrackingTouch(SeekBar bar) {
            }

            public void onStopTrackingTouch(SeekBar bar) {
            }
        });
        this.mTrackListV = (ListView) this.mView.findViewById(R.id.tab_track);
        this.mTrackListV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> adapterView, View v, int position, long id) {
                if (SettingDialog.this.mTracks != null && SettingDialog.this.mTracks.length > 0) {
                    SettingDialog.this.mVideoView.switchAudioTrack(position);
                }
            }
        });
        if (mShow3DMenu && Utils.isSdkSoftwinner()) {
            this.m3DModeListV = (ListView) this.mView.findViewById(R.id.tab_3d);
            this.m3DModes = this.mVideoView.get3DModeList();
            this.m3DModeListV.setAdapter(
                    new ArrayAdapter(this.mContext, R.layout.vplayer_list_item_single_choice, this.m3DModes));
            this.m3DModeListV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                public void onItemClick(AdapterView<?> adapterView, View v, int position, long id) {
                    if (SettingDialog.this.m3DModes != null && SettingDialog.this.m3DModes.length > 0
                            && !SettingDialog.this.mVideoView.set3DMode(position, true)) {
                        SettingDialog.this.m3DModeListV.setItemChecked(SettingDialog.this.mVideoView.getCurrent3DMode(),
                                true);
                        Toast.makeText(SettingDialog.this.mContext, R.string.set_3dmode_fail, 0).show();
                    }
                }
            });
        }
        this.mColorLayout = (LinearLayout) this.mView.findViewById(R.id.color_panel);
        this.mColorPickerView = new ColorPickerView(this.mContext, this, this.mConfig.getInt(AppConfig.SUBCOLOR, -1),
                this.mContext.getResources().getDimension(R.dimen.color_picker_height));
        this.mColorLayout.addView(this.mColorPickerView, new FrameLayout.LayoutParams(-2, -2, 16));
        this.mAdjustBar.setProgress(SUBTITLE_ADJUST_BAR_MAX);
        this.mCurrentAdjust.setText(String.valueOf(new Float(((double) 0) / 1000.0d).toString()) + "s");
        int codingItem = this.mConfig.getInt(AppConfig.SUBCODING, 1);
        this.mCurCoding.setText(this.mCharsetDisps[codingItem]);
        this.mCodingListV.setItemChecked(codingItem, true);
        this.mSizeBar.setProgress(this.mConfig.getInt(AppConfig.SUBSIZE, 30) - 20);
        this.mCurrentSize.setText(new Integer(this.mConfig.getInt(AppConfig.SUBSIZE, 30)).toString());
        onShow();
    }

    public FrameLayout.LayoutParams getLayoutParams() {
        if (this.mLayoutParams == null) {
            this.mLayoutParams = new FrameLayout.LayoutParams(-2, -2, 17);
        }
        return this.mLayoutParams;
    }

    private View getTabIndicator(int resId) {
        View view = View.inflate(this.mContext, R.layout.vplayer_tab_indicator, (ViewGroup) null);
        ((TextView) view.findViewById(16908310)).setText(resId);
        return view;
    }

    private void onShow() {
        int playMode = this.mConfig.getInt(AppConfig.PlayMode, 0);
        this.mRepeatListV.setItemChecked(playMode, true);
        this.mRepeatListV.smoothScrollToPosition(playMode);
        this.mDemoModeSwitch.setChecked(this.mConfig.getBoolean(AppConfig.DEMO_MODE, false));
        this.mSubtitleSettings.setDisplayedChild(0);
        boolean subGate = this.mVideoView.getSubGate();
        this.mSubtitleSwitch.setChecked(subGate);
        updateSubtitleSetting(subGate);
        ArrayList<VideoSurfaceTextureView.AwTrackInfo> tracks = this.mVideoView.getAudioTrackList();
        if (tracks != null && tracks.size() > 0) {
            this.mTracks = new String[tracks.size()];
            for (int i = 0; i < tracks.size(); i++) {
                this.mTracks[i] = new String(tracks.get(i).name);
                if (this.mTracks[i].equals("")) {
                    this.mTracks[i] = String.valueOf("audio_track") + "[" + i + "]";
                }
            }
            int trackIndex = this.mVideoView.getCurAudioTrack();
            this.mTrackListV.setAdapter(
                    new ArrayAdapter<>(this.mContext, R.layout.vplayer_list_item_single_choice, this.mTracks));
            this.mTrackListV.setItemChecked(trackIndex, true);
            this.mTrackListV.smoothScrollToPosition(trackIndex);
        }
        if (mShow3DMenu) {
            int mode = this.mVideoView.getCurrent3DMode();
            this.m3DModeListV.setItemChecked(mode, true);
            this.m3DModeListV.smoothScrollToPosition(mode);
        }
    }

    public void onHide() {
    }

    public void colorChanged(int color) {
        this.mVideoView.setSubColor(color);
        this.mConfig.setInt(AppConfig.SUBCOLOR, color);
    }

    /* access modifiers changed from: private */
    public void updateSubtitleSetting(boolean gate) {
        boolean enable = gate;
        if (gate) {
            ArrayList<VideoSurfaceTextureView.AwTrackInfo> infoList = this.mVideoView.getSubList();
            if (infoList == null || infoList.size() <= 0) {
                enable = false;
            } else {
                this.mSubtitles = new String[infoList.size()];
                for (int i = 0; i < infoList.size(); i++) {
                    this.mSubtitles[i] = new String(infoList.get(i).name);
                    if (this.mSubtitles[i].equals("")) {
                        this.mSubtitles[i] = String.valueOf("sub_track") + "[" + i + "]";
                    }
                }
                int subIndex = this.mVideoView.getCurSub();
                this.mCurSubtitle.setText(this.mSubtitles[subIndex]);
                this.mSubtitleListV.setAdapter(
                        new ArrayAdapter<>(this.mContext, R.layout.vplayer_list_item_single_choice, this.mSubtitles));
                this.mSubtitleListV.setItemChecked(subIndex, true);
                int subDelay = this.mVideoView.getSubDelay();
                this.mAdjustBar.setProgress(subDelay + SUBTITLE_ADJUST_BAR_MAX);
                this.mCurrentAdjust.setText(String.valueOf(new Float(((double) subDelay) / 1000.0d).toString()) + "s");
                int codingItem = this.mConfig.getInt(AppConfig.SUBCODING, 1);
                this.mCurCoding.setText(this.mCharsetDisps[codingItem]);
                this.mCodingListV.setItemChecked(codingItem, true);
            }
        }
        this.mSubtitleSelector.setEnabled(enable);
        this.mAdjustBar.setEnabled(enable);
        this.mCodingSelector.setEnabled(enable);
        this.mSizeBar.setEnabled(enable);
    }
}
