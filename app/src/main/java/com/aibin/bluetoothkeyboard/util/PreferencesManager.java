package com.aibin.bluetoothkeyboard.util;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferencesManager {
    private static final String PREF_NAME = "bt_keyboard_prefs";

    private static final String KEY_HAPTIC = "pref_haptic_feedback";
    private static final String KEY_SOUND = "pref_sound_feedback";
    private static final String KEY_KEEP_AWAKE = "pref_keep_awake";
    private static final String KEY_OS_MODE = "pref_os_mode"; // "windows", "macos", "linux"
    private static final String KEY_LAST_DEVICE = "pref_last_device";

    private final SharedPreferences prefs;

    public PreferencesManager(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public boolean isHapticEnabled() {
        return prefs.getBoolean(KEY_HAPTIC, true);
    }

    public void setHapticEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_HAPTIC, enabled).apply();
    }

    public boolean isSoundEnabled() {
        return prefs.getBoolean(KEY_SOUND, true);
    }

    public void setSoundEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_SOUND, enabled).apply();
    }

    public boolean isKeepAwakeEnabled() {
        return prefs.getBoolean(KEY_KEEP_AWAKE, true);
    }

    public void setKeepAwakeEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_KEEP_AWAKE, enabled).apply();
    }

    public String getOsMode() {
        return prefs.getString(KEY_OS_MODE, "windows");
    }

    public void setOsMode(String osMode) {
        prefs.edit().putString(KEY_OS_MODE, osMode).apply();
    }

    public String getLastDeviceAddress() {
        return prefs.getString(KEY_LAST_DEVICE, null);
    }

    public void setLastDeviceAddress(String address) {
        prefs.edit().putString(KEY_LAST_DEVICE, address).apply();
    }
}
