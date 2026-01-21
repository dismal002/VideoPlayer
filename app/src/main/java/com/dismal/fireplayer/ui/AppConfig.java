package com.dismal.fireplayer.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import com.dismal.fireplayer.util.XMLConfigParser;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class AppConfig {
    public static final String APP_MODE = "app_mode";
    public static final int APP_MODE_COOL = 0;
    public static final int APP_MODE_LOWPOWER = 2;
    public static final int APP_MODE_OPT = 1;
    public static final String ATHUMB_ENABLE = "athumb_enable";
    public static final String AUDIO_TRACK_INDEX = "audiotrackindex";
    public static final String BRIGHTNESS = "brightness";
    public static final String BRIGHTNESS_MIN = "brightness_min";
    public static final String BUILDIN_3DSCREEN = "buildin_3d_screen";
    public static final String CFG_HIDE_3D_MENU = "hide_3d_menu";
    public static final String CFG_PARSERED = "cfg_parsered";
    public static final String CHIP_VERSION = "chip_ver";
    public static final String CUSTOM_HDMI_COOL_DISABLE = "hdmi_cool_mode_disable";
    public static final String CUSTOM_HDMI_VIDEO_RESTART = "hdmi_video_restart";
    public static final String CUSTOM_ORIENTATION = "orientation";
    public static final String CUSTOM_ORIENTATION_UI = "orientation_ui";
    public static final String CUSTOM_S1080P = "s1080p";
    public static final String CUSTOM_S2160P = "s2160p";
    public static final String CUSTOM_S720P = "s720p";
    public static final String CUSTOM_SOTHER = "sother";
    public static final String CUSTOM_VIDEO_FILTER = "video_filter";
    public static final String DEFAULT_OPEN_FULLSCREEN = "default_open_fullscreen";
    public static final String DEFAULT_PLAY_MODE = "default_play_mode";
    public static final String DEMO_MODE = "demo_mode";
    public static final String DIMEN_PX_MODE = "dimen_px";
    public static final String DISABLE_SWITCH_PRESENTATION = "disable_switch_presentation";
    public static final String EARPHONE_PLUGOUT_PAUSE = "earphone_plugout_pause";
    public static final String ENABLE_DEMO_MODE = "enable_demo_mode";
    public static final String FAVORITE_FACTOR = "favorite_factor";
    public static final String FIRMWARE_VERSION = "firmware_ver";
    public static final String FLOAT_WINDOW_ENABLE = "floatwindow_enable";
    public static final String HELP_HINT = "help_hint";
    public static final String HIDEMENU_ATHUMB = "hide_menu_athumb";
    public static final String HIDEMENU_PRESENTATION = "hide_menu_presentation";
    public static final String HIDE_ICON_FLOATWINDOW = "hide_icon_floatwindow";
    public static final String INIT_PLAYLIST = "init_playlist";
    public static final String LAST_UPDATE_TIME = "last_update_time";
    public static final String LATEST_PLAYTIME = "latest_playtime";
    public static final String MEDIA_SCANNED = "media_scanned";
    public static final String OLD_UPDATE_VERSION = "old_update_version";
    public static final String PRESENTATION_BG_DISABLE = "presentation_bg_disable";
    public static final String PRESENTATION_ENABLE = "presentation_enable";
    public static final String PlayMode = "playmode";
    public static final String RECORDED_FAVORITE = "recorded_favorite";
    public static final String RECORDED_PLAYTIME = "recorded_playtime";
    public static final String RETINA_SCREEN = "retina_screen";
    public static final String SMART_DETECTION_ENABLE = "smart_detection_enable";
    public static final String SUBCODING = "subcoding";
    public static final String SUBCOLOR = "subcolor";
    public static final String SUBCOLOR_UDF = "subcolor_udf";
    public static final String SUBGATE = "subgate";
    public static final String SUBSIZE = "subsize";
    public static final String SUBSIZE_UDF = "subsize_udf";
    public static final String SUB_DELAY = "sub_delay";
    public static final String SUB_OLD_PATH = "sub_old_path";
    public static final String SUB_TRACK_INDEX = "subtrackindex";
    public static final String TAG = "AppConfig";
    public static final String VPLAYER_USAGE1 = "vplayer_usage1";
    public static final String VPLAYER_USAGE2 = "vplayer_usage2";
    public static final String ZOOM = "zoommode";
    public static boolean bAthumbEnable;
    public static int mAppMode;
    public static int mChipVersion;
    private static AppConfig mInstance;
    private final SharedPreferences mConfigs;
    private final Context mContext;
    private boolean mDisableCoolMode = false;
    private final SharedPreferences.Editor mEditor;

    public static AppConfig getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new AppConfig(context);
        }
        return mInstance;
    }

    private AppConfig(Context context) {
        this.mContext = context.getApplicationContext();
        this.mConfigs = PreferenceManager.getDefaultSharedPreferences(this.mContext);
        this.mEditor = this.mConfigs.edit();
        initialize();
    }

    private void initialize() {
        extCfgParse();
        mChipVersion = getInt(CHIP_VERSION, 31);
        mAppMode = getInt(APP_MODE, 1);
        bAthumbEnable = getBoolean(ATHUMB_ENABLE, false);
    }

    private void extCfgParse() {
        if (!getBoolean(CFG_PARSERED, false)) {
            XMLConfigParser xmlParser = new XMLConfigParser();
            if (xmlParser.loadCfgOverrides()) {
                if (xmlParser.containsCarrier(CHIP_VERSION)) {
                    setInt(CHIP_VERSION, Integer.parseInt(xmlParser.getCfg(CHIP_VERSION)));
                }
                if (xmlParser.containsCarrier(FIRMWARE_VERSION)) {
                    setInt(FIRMWARE_VERSION, Integer.parseInt(xmlParser.getCfg(FIRMWARE_VERSION)));
                }
                if (xmlParser.containsCarrier(ATHUMB_ENABLE)) {
                    setBoolean(ATHUMB_ENABLE, xmlParser.getCfg(ATHUMB_ENABLE).equals("1"));
                }
                if (xmlParser.containsCarrier(FLOAT_WINDOW_ENABLE)) {
                    setBoolean(FLOAT_WINDOW_ENABLE, xmlParser.getCfg(FLOAT_WINDOW_ENABLE).equals("1"));
                }
                if (xmlParser.containsCarrier(PRESENTATION_ENABLE)) {
                    setBoolean(PRESENTATION_ENABLE, xmlParser.getCfg(PRESENTATION_ENABLE).equals("1"));
                }
                if (xmlParser.containsCarrier(DEFAULT_OPEN_FULLSCREEN)) {
                    setBoolean(DEFAULT_OPEN_FULLSCREEN, xmlParser.getCfg(DEFAULT_OPEN_FULLSCREEN).equals("1"));
                }
                if (xmlParser.containsCarrier(DEFAULT_PLAY_MODE)) {
                    setInt(PlayMode, Integer.valueOf(xmlParser.getCfg(DEFAULT_PLAY_MODE)).intValue());
                }
                if (xmlParser.containsCarrier(DIMEN_PX_MODE)) {
                    setBoolean(DIMEN_PX_MODE, xmlParser.getCfg(DIMEN_PX_MODE).equals("1"));
                }
                if (xmlParser.containsCarrier(CUSTOM_HDMI_VIDEO_RESTART)) {
                    setBoolean(CUSTOM_HDMI_VIDEO_RESTART, xmlParser.getCfg(CUSTOM_HDMI_VIDEO_RESTART).equals("1"));
                }
                if (xmlParser.containsCarrier(CUSTOM_HDMI_COOL_DISABLE)) {
                    setBoolean(CUSTOM_HDMI_COOL_DISABLE, xmlParser.getCfg(CUSTOM_HDMI_COOL_DISABLE).equals("1"));
                }
                if (xmlParser.containsCarrier(CUSTOM_ORIENTATION)) {
                    setInt(CUSTOM_ORIENTATION, Integer.valueOf(xmlParser.getCfg(CUSTOM_ORIENTATION)).intValue());
                }
                if (xmlParser.containsCarrier(CUSTOM_ORIENTATION_UI)) {
                    setInt(CUSTOM_ORIENTATION_UI, Integer.valueOf(xmlParser.getCfg(CUSTOM_ORIENTATION_UI)).intValue());
                }
                if (xmlParser.containsCarrier(CUSTOM_S2160P)) {
                    setInt(CUSTOM_S2160P, Integer.valueOf(xmlParser.getCfg(CUSTOM_S2160P)).intValue());
                }
                if (xmlParser.containsCarrier(CUSTOM_S1080P)) {
                    setInt(CUSTOM_S1080P, Integer.valueOf(xmlParser.getCfg(CUSTOM_S1080P)).intValue());
                }
                if (xmlParser.containsCarrier(CUSTOM_S720P)) {
                    setInt(CUSTOM_S720P, Integer.valueOf(xmlParser.getCfg(CUSTOM_S720P)).intValue());
                }
                if (xmlParser.containsCarrier(CUSTOM_SOTHER)) {
                    setInt(CUSTOM_SOTHER, Integer.valueOf(xmlParser.getCfg(CUSTOM_SOTHER)).intValue());
                }
                if (xmlParser.containsCarrier(HIDEMENU_PRESENTATION)) {
                    setBoolean(HIDEMENU_PRESENTATION, xmlParser.getCfg(HIDEMENU_PRESENTATION).equals("1"));
                }
                if (xmlParser.containsCarrier(HIDEMENU_ATHUMB)) {
                    setBoolean(HIDEMENU_ATHUMB, xmlParser.getCfg(HIDEMENU_ATHUMB).equals("1"));
                }
                if (xmlParser.containsCarrier(HIDE_ICON_FLOATWINDOW)) {
                    setBoolean(HIDE_ICON_FLOATWINDOW, xmlParser.getCfg(HIDE_ICON_FLOATWINDOW).equals("1"));
                }
                if (xmlParser.containsCarrier(SUBCODING)) {
                    setInt(SUBCODING, Integer.valueOf(xmlParser.getCfg(SUBCODING)).intValue());
                }
                if (xmlParser.containsCarrier(BRIGHTNESS_MIN)) {
                    setInt(BRIGHTNESS_MIN, Integer.valueOf(xmlParser.getCfg(BRIGHTNESS_MIN)).intValue());
                }
                if (xmlParser.containsCarrier(CUSTOM_VIDEO_FILTER)) {
                    setString(CUSTOM_VIDEO_FILTER, xmlParser.getCfg(CUSTOM_VIDEO_FILTER));
                }
                if (xmlParser.containsCarrier(CFG_HIDE_3D_MENU)) {
                    setBoolean(CFG_HIDE_3D_MENU, xmlParser.getCfg(CFG_HIDE_3D_MENU).equals("1"));
                }
                if (xmlParser.containsCarrier(BUILDIN_3DSCREEN)) {
                    setBoolean(BUILDIN_3DSCREEN, xmlParser.getCfg(BUILDIN_3DSCREEN).equals("1"));
                }
                if (xmlParser.containsCarrier(PRESENTATION_BG_DISABLE)) {
                    setBoolean(PRESENTATION_BG_DISABLE, xmlParser.getCfg(PRESENTATION_BG_DISABLE).equals("1"));
                }
                if (xmlParser.containsCarrier(ENABLE_DEMO_MODE)) {
                    setBoolean(ENABLE_DEMO_MODE, xmlParser.getCfg(ENABLE_DEMO_MODE).equals("1"));
                }
                if (xmlParser.containsCarrier(EARPHONE_PLUGOUT_PAUSE)) {
                    String value = xmlParser.getCfg(EARPHONE_PLUGOUT_PAUSE);
                    Log.v(TAG, "value = " + value + "   " + value.equals("1"));
                    setBoolean(EARPHONE_PLUGOUT_PAUSE, value.equals("1"));
                }
                if (xmlParser.containsCarrier(SMART_DETECTION_ENABLE)) {
                    setBoolean(SMART_DETECTION_ENABLE, xmlParser.getCfg(SMART_DETECTION_ENABLE).equals("1"));
                }
            } else {
                String hardware = getCpuHardwareInfo();
                if ("sun7i".equals(hardware)) {
                    setInt(CHIP_VERSION, 20);
                } else if ("sun6i".equals(hardware)) {
                    setInt(CHIP_VERSION, 31);
                } else {
                    setInt(CHIP_VERSION, 31);
                }
                setInt(FIRMWARE_VERSION, 10);
                setBoolean(ATHUMB_ENABLE, true);
            }
            setBoolean(CFG_PARSERED, true);
        }
    }

    private String getCpuHardwareInfo() {
        String hardwareInfo = null;
        try {
            BufferedReader localBufferedReader = new BufferedReader(new FileReader("/proc/cpuinfo"), 8192);
            while (true) {
                String line = localBufferedReader.readLine();
                if (line != null) {
                    if (line.contains("Hardware")) {
                        hardwareInfo = line.split("\\s+")[2];
                        break;
                    }
                } else {
                    break;
                }
            }
            localBufferedReader.close();
        } catch (IOException e) {
        }
        Log.v(TAG, "cpuinfo Hardware:" + hardwareInfo);
        return hardwareInfo;
    }

    public void setAppMode(int appMode, boolean save) {
        mAppMode = appMode;
        if (save) {
            setInt(APP_MODE, mAppMode);
        }
    }

    public void resetAppMode() {
        mAppMode = getInt(APP_MODE, 1);
        this.mDisableCoolMode = false;
    }

    public int getAppMode() {
        return mAppMode;
    }

    public void disableCoolMode(boolean disable) {
        this.mDisableCoolMode = disable;
    }

    public boolean isCoolModeDisable() {
        return this.mDisableCoolMode;
    }

    public boolean getBoolean(String key, boolean def) {
        return this.mConfigs.getBoolean(key, def);
    }

    public void setBoolean(String key, boolean value) {
        this.mEditor.putBoolean(key, value);
        this.mEditor.commit();
    }

    public int getInt(String key, int def) {
        return this.mConfigs.getInt(key, def);
    }

    public void setInt(String key, int value) {
        this.mEditor.putInt(key, value);
        this.mEditor.commit();
    }

    public String getString(String key, String def) {
        return this.mConfigs.getString(key, def);
    }

    public void setString(String key, String value) {
        this.mEditor.putString(key, value);
        this.mEditor.commit();
    }

    public Float getFloat(String key, float def) {
        return Float.valueOf(this.mConfigs.getFloat(key, def));
    }

    public void setFloat(String key, float value) {
        this.mEditor.putFloat(key, value);
        this.mEditor.commit();
    }

    public long getLong(String key, Long def) {
        return this.mConfigs.getLong(key, def.longValue());
    }

    public void setLong(String key, long value) {
        this.mEditor.putLong(key, value);
        this.mEditor.commit();
    }
}
