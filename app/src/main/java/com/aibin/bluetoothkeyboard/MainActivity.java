package com.aibin.bluetoothkeyboard;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.aibin.bluetoothkeyboard.bluetooth.BluetoothHidManager;
import com.aibin.bluetoothkeyboard.model.HidKeyCodes;
import com.aibin.bluetoothkeyboard.model.KeyDef;
import com.aibin.bluetoothkeyboard.util.PreferencesManager;
import com.aibin.bluetoothkeyboard.util.SoundHapticHelper;
import com.aibin.bluetoothkeyboard.util.UpdateManager;
import com.google.android.material.button.MaterialButton;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private BluetoothHidManager hidManager;
    private PreferencesManager preferencesManager;
    private SoundHapticHelper soundHapticHelper;

    private TextView tvStatusBadge;
    private View btnOpenSettings;

    // Modifiers state
    private boolean isShiftActive = false;
    private boolean isCapsLockActive = false;
    private boolean isCtrlActive = false;
    private boolean isAltActive = false;
    private boolean isGuiActive = false; // Win or Cmd

    private final Map<Integer, KeyDef> keyMap = new HashMap<>();
    private AlertDialog disconnectedDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        preferencesManager = new PreferencesManager(this);
        soundHapticHelper = new SoundHapticHelper(this, preferencesManager);
        hidManager = BluetoothHidManager.getInstance(this);

        initViews();
        setupKeyMap();
        setupKeyListeners();
        setupBluetoothListener();
        enableFullscreenImmersive();

        // Check for updates from GitHub releases asynchronously on open
        UpdateManager.getInstance(this).checkForUpdates(this, false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        enableFullscreenImmersive();
        applyScreenSettings();
        updateOsKeyLabels();
        updateConnectionStatus();

        if (hidManager.isBluetoothEnabled()) {
            hidManager.registerHidDevice();
        }

        UpdateManager.getInstance(this).resumePendingInstall(this);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            enableFullscreenImmersive();
        }
    }

    private void enableFullscreenImmersive() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN
        );
    }

    private void applyScreenSettings() {
        if (preferencesManager.isKeepAwakeEnabled()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    private void initViews() {
        tvStatusBadge = findViewById(R.id.tvStatusBadge);
        btnOpenSettings = findViewById(R.id.btnOpenSettings);

        tvStatusBadge.setOnClickListener(v -> openSettingsActivity());
        btnOpenSettings.setOnClickListener(v -> openSettingsActivity());
    }

    private void openSettingsActivity() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    private final BluetoothHidManager.HidConnectionListener mainHidListener = new BluetoothHidManager.HidConnectionListener() {
        @Override
        public void onAppRegistered(boolean success) {
            runOnUiThread(() -> updateConnectionStatus());
        }

        @Override
        public void onDeviceConnectionChanged(BluetoothDevice device, int state) {
            runOnUiThread(() -> {
                updateConnectionStatus();
                if (state == BluetoothProfile.STATE_CONNECTED) {
                    if (disconnectedDialog != null && disconnectedDialog.isShowing()) {
                        disconnectedDialog.dismiss();
                    }
                    String devName = hidManager.getConnectedDeviceName();
                    Toast.makeText(MainActivity.this, "🟢 Connected to " + devName, Toast.LENGTH_SHORT).show();
                }
            });
        }
    };

    private void setupBluetoothListener() {
        hidManager.registerListener(mainHidListener);
    }

    @SuppressLint("SetTextI18n")
    private void updateConnectionStatus() {
        if (hidManager.isConnected()) {
            String deviceName = hidManager.getConnectedDeviceName();
            if (deviceName == null || deviceName.isEmpty()) {
                deviceName = "Laptop";
            }
            tvStatusBadge.setText("🟢 Connected: " + deviceName);
            tvStatusBadge.setTextColor(getColor(R.color.status_connected));
            tvStatusBadge.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.status_connected_bg)));
        } else {
            tvStatusBadge.setText(R.string.status_not_connected);
            tvStatusBadge.setTextColor(getColor(R.color.status_disconnected));
            tvStatusBadge.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.status_disconnected_bg)));
        }
    }

    private void updateOsKeyLabels() {
        TextView keyWin = findViewById(R.id.key_win);
        TextView keyAltLeft = findViewById(R.id.key_alt_left);
        TextView keyAltRight = findViewById(R.id.key_alt_right);

        String osMode = preferencesManager.getOsMode();
        if ("macos".equalsIgnoreCase(osMode)) {
            if (keyWin != null) keyWin.setText("Cmd ⌘");
            if (keyAltLeft != null) keyAltLeft.setText("Opt ⌥");
            if (keyAltRight != null) keyAltRight.setText("Opt ⌥");
        } else {
            if (keyWin != null) keyWin.setText("Win");
            if (keyAltLeft != null) keyAltLeft.setText("Alt");
            if (keyAltRight != null) keyAltRight.setText("Alt");
        }
    }

    private void setupKeyMap() {
        // Function row
        keyMap.put(R.id.key_esc, new KeyDef("Esc", HidKeyCodes.KEY_ESCAPE));
        keyMap.put(R.id.key_f1, new KeyDef("F1", HidKeyCodes.KEY_F1));
        keyMap.put(R.id.key_f2, new KeyDef("F2", HidKeyCodes.KEY_F2));
        keyMap.put(R.id.key_f3, new KeyDef("F3", HidKeyCodes.KEY_F3));
        keyMap.put(R.id.key_f4, new KeyDef("F4", HidKeyCodes.KEY_F4));
        keyMap.put(R.id.key_f5, new KeyDef("F5", HidKeyCodes.KEY_F5));
        keyMap.put(R.id.key_f6, new KeyDef("F6", HidKeyCodes.KEY_F6));
        keyMap.put(R.id.key_f7, new KeyDef("F7", HidKeyCodes.KEY_F7));
        keyMap.put(R.id.key_f8, new KeyDef("F8", HidKeyCodes.KEY_F8));
        keyMap.put(R.id.key_f9, new KeyDef("F9", HidKeyCodes.KEY_F9));
        keyMap.put(R.id.key_f10, new KeyDef("F10", HidKeyCodes.KEY_F10));
        keyMap.put(R.id.key_f11, new KeyDef("F11", HidKeyCodes.KEY_F11));
        keyMap.put(R.id.key_f12, new KeyDef("F12", HidKeyCodes.KEY_F12));
        keyMap.put(R.id.key_prntscrn, new KeyDef("PrtSc", HidKeyCodes.KEY_PRINTSCREEN));
        keyMap.put(R.id.key_delete, new KeyDef("Del", HidKeyCodes.KEY_DELETE));

        // Number row (including ( and ) on 9 and 0)
        keyMap.put(R.id.key_grave, new KeyDef("` ~", "~", HidKeyCodes.KEY_GRAVE));
        keyMap.put(R.id.key_1, new KeyDef("1 !", "!", HidKeyCodes.KEY_1));
        keyMap.put(R.id.key_2, new KeyDef("2 @", "@", HidKeyCodes.KEY_2));
        keyMap.put(R.id.key_3, new KeyDef("3 #", "#", HidKeyCodes.KEY_3));
        keyMap.put(R.id.key_4, new KeyDef("4 $", "$", HidKeyCodes.KEY_4));
        keyMap.put(R.id.key_5, new KeyDef("5 %", "%", HidKeyCodes.KEY_5));
        keyMap.put(R.id.key_6, new KeyDef("6 ^", "^", HidKeyCodes.KEY_6));
        keyMap.put(R.id.key_7, new KeyDef("7 &", "&", HidKeyCodes.KEY_7));
        keyMap.put(R.id.key_8, new KeyDef("8 *", "*", HidKeyCodes.KEY_8));
        keyMap.put(R.id.key_9, new KeyDef("9 (", "(", HidKeyCodes.KEY_9));
        keyMap.put(R.id.key_0, new KeyDef("0 )", ")", HidKeyCodes.KEY_0));
        keyMap.put(R.id.key_minus, new KeyDef("- _", "_", HidKeyCodes.KEY_MINUS));
        keyMap.put(R.id.key_equal, new KeyDef("= +", "+", HidKeyCodes.KEY_EQUAL));
        keyMap.put(R.id.key_backspace, new KeyDef("⌫ Back", HidKeyCodes.KEY_BACKSPACE));
        keyMap.put(R.id.key_home, new KeyDef("Home", HidKeyCodes.KEY_HOME));

        // QWERTY row
        keyMap.put(R.id.key_tab, new KeyDef("Tab", HidKeyCodes.KEY_TAB));
        keyMap.put(R.id.key_q, new KeyDef("q", "Q", HidKeyCodes.KEY_Q));
        keyMap.put(R.id.key_w, new KeyDef("w", "W", HidKeyCodes.KEY_W));
        keyMap.put(R.id.key_e, new KeyDef("e", "E", HidKeyCodes.KEY_E));
        keyMap.put(R.id.key_r, new KeyDef("r", "R", HidKeyCodes.KEY_R));
        keyMap.put(R.id.key_t, new KeyDef("t", "T", HidKeyCodes.KEY_T));
        keyMap.put(R.id.key_y, new KeyDef("y", "Y", HidKeyCodes.KEY_Y));
        keyMap.put(R.id.key_u, new KeyDef("u", "U", HidKeyCodes.KEY_U));
        keyMap.put(R.id.key_i, new KeyDef("i", "I", HidKeyCodes.KEY_I));
        keyMap.put(R.id.key_o, new KeyDef("o", "O", HidKeyCodes.KEY_O));
        keyMap.put(R.id.key_p, new KeyDef("p", "P", HidKeyCodes.KEY_P));
        keyMap.put(R.id.key_leftbrace, new KeyDef("[ {", "{", HidKeyCodes.KEY_LEFTBRACE));
        keyMap.put(R.id.key_rightbrace, new KeyDef("] }", "}", HidKeyCodes.KEY_RIGHTBRACE));
        keyMap.put(R.id.key_backslash, new KeyDef("\\ |", "|", HidKeyCodes.KEY_BACKSLASH));
        keyMap.put(R.id.key_pageup, new KeyDef("PgUp", HidKeyCodes.KEY_PAGEUP));

        // Home row
        keyMap.put(R.id.key_capslock, new KeyDef("Caps", HidKeyCodes.KEY_CAPSLOCK, true, (byte) 0));
        keyMap.put(R.id.key_a, new KeyDef("a", "A", HidKeyCodes.KEY_A));
        keyMap.put(R.id.key_s, new KeyDef("s", "S", HidKeyCodes.KEY_S));
        keyMap.put(R.id.key_d, new KeyDef("d", "D", HidKeyCodes.KEY_D));
        keyMap.put(R.id.key_f, new KeyDef("f", "F", HidKeyCodes.KEY_F));
        keyMap.put(R.id.key_g, new KeyDef("g", "G", HidKeyCodes.KEY_G));
        keyMap.put(R.id.key_h, new KeyDef("h", "H", HidKeyCodes.KEY_H));
        keyMap.put(R.id.key_j, new KeyDef("j", "J", HidKeyCodes.KEY_J));
        keyMap.put(R.id.key_k, new KeyDef("k", "K", HidKeyCodes.KEY_K));
        keyMap.put(R.id.key_l, new KeyDef("l", "L", HidKeyCodes.KEY_L));
        keyMap.put(R.id.key_semicolon, new KeyDef("; :", ":", HidKeyCodes.KEY_SEMICOLON));
        keyMap.put(R.id.key_apostrophe, new KeyDef("' \"", "\"", HidKeyCodes.KEY_APOSTROPHE));
        keyMap.put(R.id.key_enter, new KeyDef("Enter", HidKeyCodes.KEY_ENTER));
        keyMap.put(R.id.key_pagedown, new KeyDef("PgDn", HidKeyCodes.KEY_PAGEDOWN));

        // Shift row
        keyMap.put(R.id.key_shift_left, new KeyDef("Shift", (byte) 0, true, HidKeyCodes.MOD_LEFT_SHIFT));
        keyMap.put(R.id.key_z, new KeyDef("z", "Z", HidKeyCodes.KEY_Z));
        keyMap.put(R.id.key_x, new KeyDef("x", "X", HidKeyCodes.KEY_X));
        keyMap.put(R.id.key_c, new KeyDef("c", "C", HidKeyCodes.KEY_C));
        keyMap.put(R.id.key_v, new KeyDef("v", "V", HidKeyCodes.KEY_V));
        keyMap.put(R.id.key_b, new KeyDef("b", "B", HidKeyCodes.KEY_B));
        keyMap.put(R.id.key_n, new KeyDef("n", "N", HidKeyCodes.KEY_N));
        keyMap.put(R.id.key_m, new KeyDef("m", "M", HidKeyCodes.KEY_M));
        keyMap.put(R.id.key_comma, new KeyDef(", <", "<", HidKeyCodes.KEY_COMMA));
        keyMap.put(R.id.key_dot, new KeyDef(". >", ">", HidKeyCodes.KEY_DOT));
        keyMap.put(R.id.key_slash, new KeyDef("/ ?", "?", HidKeyCodes.KEY_SLASH));
        keyMap.put(R.id.key_shift_right, new KeyDef("Shift", (byte) 0, true, HidKeyCodes.MOD_RIGHT_SHIFT));
        keyMap.put(R.id.key_end, new KeyDef("End", HidKeyCodes.KEY_END));

        // Bottom row
        keyMap.put(R.id.key_ctrl_left, new KeyDef("Ctrl", (byte) 0, true, HidKeyCodes.MOD_LEFT_CTRL));
        keyMap.put(R.id.key_fn, new KeyDef("Fn", (byte) 0));
        keyMap.put(R.id.key_win, new KeyDef("Win", (byte) 0, true, HidKeyCodes.MOD_LEFT_GUI));
        keyMap.put(R.id.key_alt_left, new KeyDef("Alt", (byte) 0, true, HidKeyCodes.MOD_LEFT_ALT));
        keyMap.put(R.id.key_space, new KeyDef(" ", HidKeyCodes.KEY_SPACE));
        keyMap.put(R.id.key_alt_right, new KeyDef("Alt", (byte) 0, true, HidKeyCodes.MOD_RIGHT_ALT));
        keyMap.put(R.id.key_ctrl_right, new KeyDef("Ctrl", (byte) 0, true, HidKeyCodes.MOD_RIGHT_CTRL));
        keyMap.put(R.id.key_insert, new KeyDef("Ins", HidKeyCodes.KEY_INSERT));
        keyMap.put(R.id.key_arrow_left, new KeyDef("◀", HidKeyCodes.KEY_LEFT));
        keyMap.put(R.id.key_arrow_up, new KeyDef("▲", HidKeyCodes.KEY_UP));
        keyMap.put(R.id.key_arrow_down, new KeyDef("▼", HidKeyCodes.KEY_DOWN));
        keyMap.put(R.id.key_arrow_right, new KeyDef("▶", HidKeyCodes.KEY_RIGHT));
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupKeyListeners() {
        for (Map.Entry<Integer, KeyDef> entry : keyMap.entrySet()) {
            View view = findViewById(entry.getKey());
            if (view != null) {
                final KeyDef keyDef = entry.getValue();
                view.setOnTouchListener((v, event) -> {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            v.setPressed(true);
                            onKeyPressed(v, keyDef);
                            return true;

                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL:
                            v.setPressed(false);
                            onKeyReleased(keyDef);
                            return true;
                    }
                    return false;
                });
            }
        }
    }

    private void onKeyPressed(View view, KeyDef keyDef) {
        soundHapticHelper.performFeedback(view);

        // If disconnected, alert user to connect laptop
        if (!hidManager.isConnected()) {
            showConnectLaptopPrompt();
            return;
        }

        // Handle modifier toggles
        if (keyDef.isModifier()) {
            handleModifierPress(view, keyDef);
            return;
        }

        // Calculate combined active modifiers
        byte modifiers = 0;
        if (isCtrlActive) modifiers |= HidKeyCodes.MOD_LEFT_CTRL;
        if (isAltActive) modifiers |= HidKeyCodes.MOD_LEFT_ALT;
        if (isGuiActive) modifiers |= HidKeyCodes.MOD_LEFT_GUI;
        if (isShiftActive || isCapsLockActive) modifiers |= HidKeyCodes.MOD_LEFT_SHIFT;

        // Send Key Down
        if (keyDef.getKeyCode() != 0) {
            hidManager.sendKeyDown(modifiers, keyDef.getKeyCode());
        }

        // Reset one-shot modifiers if active
        if (isShiftActive) {
            isShiftActive = false;
            updateShiftVisuals();
        }
        if (isCtrlActive || isAltActive || isGuiActive) {
            isCtrlActive = false;
            isAltActive = false;
            isGuiActive = false;
            updateModifierVisuals();
        }
    }

    private void onKeyReleased(KeyDef keyDef) {
        if (!hidManager.isConnected()) return;
        if (!keyDef.isModifier() && keyDef.getKeyCode() != 0) {
            hidManager.sendKeyUp();
        }
    }

    private void handleModifierPress(View view, KeyDef keyDef) {
        int id = view.getId();
        if (id == R.id.key_capslock) {
            isCapsLockActive = !isCapsLockActive;
            view.setSelected(isCapsLockActive);
            hidManager.sendKeyPress((byte) 0, HidKeyCodes.KEY_CAPSLOCK);
            updateShiftVisuals();
        } else if (id == R.id.key_shift_left || id == R.id.key_shift_right) {
            isShiftActive = !isShiftActive;
            updateShiftVisuals();
        } else if (id == R.id.key_ctrl_left || id == R.id.key_ctrl_right) {
            isCtrlActive = !isCtrlActive;
            view.setSelected(isCtrlActive);
        } else if (id == R.id.key_alt_left || id == R.id.key_alt_right) {
            isAltActive = !isAltActive;
            view.setSelected(isAltActive);
        } else if (id == R.id.key_win) {
            isGuiActive = !isGuiActive;
            view.setSelected(isGuiActive);
        }
    }

    private void updateShiftVisuals() {
        View shiftL = findViewById(R.id.key_shift_left);
        View shiftR = findViewById(R.id.key_shift_right);
        if (shiftL != null) shiftL.setSelected(isShiftActive);
        if (shiftR != null) shiftR.setSelected(isShiftActive);

        boolean upper = isShiftActive ^ isCapsLockActive;

        // Update letter labels
        for (Map.Entry<Integer, KeyDef> entry : keyMap.entrySet()) {
            View view = findViewById(entry.getKey());
            if (view instanceof TextView) {
                TextView tv = (TextView) view;
                KeyDef def = entry.getValue();
                if (!def.isModifier() && def.getPrimaryLabel().length() == 1 && Character.isLetter(def.getPrimaryLabel().charAt(0))) {
                    tv.setText(upper ? def.getShiftLabel() : def.getPrimaryLabel().toUpperCase());
                }
            }
        }
    }

    private void updateModifierVisuals() {
        View ctrlL = findViewById(R.id.key_ctrl_left);
        View ctrlR = findViewById(R.id.key_ctrl_right);
        View altL = findViewById(R.id.key_alt_left);
        View altR = findViewById(R.id.key_alt_right);
        View win = findViewById(R.id.key_win);

        if (ctrlL != null) ctrlL.setSelected(isCtrlActive);
        if (ctrlR != null) ctrlR.setSelected(isCtrlActive);
        if (altL != null) altL.setSelected(isAltActive);
        if (altR != null) altR.setSelected(isAltActive);
        if (win != null) win.setSelected(isGuiActive);
    }

    private void showConnectLaptopPrompt() {
        if (disconnectedDialog != null && disconnectedDialog.isShowing()) {
            return;
        }

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_connect_prompt, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true);

        disconnectedDialog = builder.create();
        if (disconnectedDialog.getWindow() != null) {
            disconnectedDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        MaterialButton btnSettings = dialogView.findViewById(R.id.btnDialogSettings);
        MaterialButton btnDismiss = dialogView.findViewById(R.id.btnDialogDismiss);

        btnSettings.setOnClickListener(v -> {
            disconnectedDialog.dismiss();
            openSettingsActivity();
        });

        btnDismiss.setOnClickListener(v -> disconnectedDialog.dismiss());

        disconnectedDialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == UpdateManager.REQUEST_CODE_INSTALL_PERMISSION) {
            UpdateManager.getInstance(this).resumePendingInstall(this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        hidManager.unregisterListener(mainHidListener);
        if (disconnectedDialog != null && disconnectedDialog.isShowing()) {
            disconnectedDialog.dismiss();
        }
    }
}
