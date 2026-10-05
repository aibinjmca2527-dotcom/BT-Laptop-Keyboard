package com.aibin.bluetoothkeyboard.util;

import android.content.Context;
import android.media.AudioManager;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.view.HapticFeedbackConstants;
import android.view.View;

public class SoundHapticHelper {
    private final Vibrator vibrator;
    private final AudioManager audioManager;
    private final PreferencesManager preferencesManager;

    public SoundHapticHelper(Context context, PreferencesManager preferencesManager) {
        this.preferencesManager = preferencesManager;
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vibratorManager = (VibratorManager) context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            this.vibrator = vibratorManager != null ? vibratorManager.getDefaultVibrator() : null;
        } else {
            this.vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        }
    }

    public void performFeedback(View view) {
        if (preferencesManager.isHapticEnabled()) {
            if (view != null) {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            } else if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK));
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(20);
                }
            }
        }

        if (preferencesManager.isSoundEnabled() && audioManager != null) {
            audioManager.playSoundEffect(AudioManager.FX_KEY_CLICK, 0.7f);
        }
    }
}
