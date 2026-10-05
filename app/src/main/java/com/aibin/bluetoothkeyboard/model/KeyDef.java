package com.aibin.bluetoothkeyboard.model;

public class KeyDef {
    private final String primaryLabel;
    private final String shiftLabel;
    private final byte keyCode;
    private final boolean isModifier;
    private final byte modifierMask;

    public KeyDef(String primaryLabel, String shiftLabel, byte keyCode) {
        this(primaryLabel, shiftLabel, keyCode, false, (byte) 0);
    }

    public KeyDef(String label, byte keyCode) {
        this(label, label, keyCode, false, (byte) 0);
    }

    public KeyDef(String label, byte keyCode, boolean isModifier, byte modifierMask) {
        this(label, label, keyCode, isModifier, modifierMask);
    }

    public KeyDef(String primaryLabel, String shiftLabel, byte keyCode, boolean isModifier, byte modifierMask) {
        this.primaryLabel = primaryLabel;
        this.shiftLabel = shiftLabel;
        this.keyCode = keyCode;
        this.isModifier = isModifier;
        this.modifierMask = modifierMask;
    }

    public String getPrimaryLabel() {
        return primaryLabel;
    }

    public String getShiftLabel() {
        return shiftLabel;
    }

    public byte getKeyCode() {
        return keyCode;
    }

    public boolean isModifier() {
        return isModifier;
    }

    public byte getModifierMask() {
        return modifierMask;
    }
}
