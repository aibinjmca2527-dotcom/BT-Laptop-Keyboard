package com.aibin.bluetoothkeyboard.model;

/**
 * Standard USB HID Keyboard Usage IDs according to USB HID Usage Tables v1.12.
 * Compatible with Windows, macOS, and Linux out of the box.
 */
public class HidKeyCodes {

    // Modifiers bitmasks (Byte 0 in Keyboard HID Report)
    public static final byte MOD_LEFT_CTRL   = (byte) (1 << 0);
    public static final byte MOD_LEFT_SHIFT  = (byte) (1 << 1);
    public static final byte MOD_LEFT_ALT    = (byte) (1 << 2);
    public static final byte MOD_LEFT_GUI    = (byte) (1 << 3); // Win / Cmd
    public static final byte MOD_RIGHT_CTRL  = (byte) (1 << 4);
    public static final byte MOD_RIGHT_SHIFT = (byte) (1 << 5);
    public static final byte MOD_RIGHT_ALT   = (byte) (1 << 6); // AltGr / Option
    public static final byte MOD_RIGHT_GUI   = (byte) (1 << 7);

    // Letter keys A-Z (0x04 - 0x1D)
    public static final byte KEY_A = 0x04;
    public static final byte KEY_B = 0x05;
    public static final byte KEY_C = 0x06;
    public static final byte KEY_D = 0x07;
    public static final byte KEY_E = 0x08;
    public static final byte KEY_F = 0x09;
    public static final byte KEY_G = 0x0A;
    public static final byte KEY_H = 0x0B;
    public static final byte KEY_I = 0x0C;
    public static final byte KEY_J = 0x0D;
    public static final byte KEY_K = 0x0E;
    public static final byte KEY_L = 0x0F;
    public static final byte KEY_M = 0x10;
    public static final byte KEY_N = 0x11;
    public static final byte KEY_O = 0x12;
    public static final byte KEY_P = 0x13;
    public static final byte KEY_Q = 0x14;
    public static final byte KEY_R = 0x15;
    public static final byte KEY_S = 0x16;
    public static final byte KEY_T = 0x17;
    public static final byte KEY_U = 0x18;
    public static final byte KEY_V = 0x19;
    public static final byte KEY_W = 0x1A;
    public static final byte KEY_X = 0x1B;
    public static final byte KEY_Y = 0x1C;
    public static final byte KEY_Z = 0x1D;

    // Number row 1-9, 0 (0x1E - 0x27)
    public static final byte KEY_1 = 0x1E;
    public static final byte KEY_2 = 0x1F;
    public static final byte KEY_3 = 0x20;
    public static final byte KEY_4 = 0x21;
    public static final byte KEY_5 = 0x22;
    public static final byte KEY_6 = 0x23;
    public static final byte KEY_7 = 0x24;
    public static final byte KEY_8 = 0x25;
    public static final byte KEY_9 = 0x26;
    public static final byte KEY_0 = 0x27;

    // Control & Navigation keys
    public static final byte KEY_ENTER       = 0x28;
    public static final byte KEY_ESCAPE      = 0x29;
    public static final byte KEY_BACKSPACE   = 0x2A;
    public static final byte KEY_TAB         = 0x2B;
    public static final byte KEY_SPACE       = 0x2C;
    public static final byte KEY_MINUS       = 0x2D; // - and _
    public static final byte KEY_EQUAL       = 0x2E; // = and +
    public static final byte KEY_LEFTBRACE   = 0x2F; // [ and {
    public static final byte KEY_RIGHTBRACE  = 0x30; // ] and }
    public static final byte KEY_BACKSLASH   = 0x31; // \ and |
    public static final byte KEY_SEMICOLON   = 0x33; // ; and :
    public static final byte KEY_APOSTROPHE  = 0x34; // ' and "
    public static final byte KEY_GRAVE       = 0x35; // ` and ~
    public static final byte KEY_COMMA       = 0x36; // , and <
    public static final byte KEY_DOT         = 0x37; // . and >
    public static final byte KEY_SLASH       = 0x38; // / and ?
    public static final byte KEY_CAPSLOCK    = 0x39;

    // Function keys F1 - F12 (0x3A - 0x45)
    public static final byte KEY_F1  = 0x3A;
    public static final byte KEY_F2  = 0x3B;
    public static final byte KEY_F3  = 0x3C;
    public static final byte KEY_F4  = 0x3D;
    public static final byte KEY_F5  = 0x3E;
    public static final byte KEY_F6  = 0x3F;
    public static final byte KEY_F7  = 0x40;
    public static final byte KEY_F8  = 0x41;
    public static final byte KEY_F9  = 0x42;
    public static final byte KEY_F10 = 0x43;
    public static final byte KEY_F11 = 0x44;
    public static final byte KEY_F12 = 0x45;

    // Additional Keys
    public static final byte KEY_PRINTSCREEN = 0x46;
    public static final byte KEY_SCROLLLOCK  = 0x47;
    public static final byte KEY_PAUSE       = 0x48;
    public static final byte KEY_INSERT      = 0x49;
    public static final byte KEY_HOME        = 0x4A;
    public static final byte KEY_PAGEUP      = 0x4B;
    public static final byte KEY_DELETE      = 0x4C;
    public static final byte KEY_END         = 0x4D;
    public static final byte KEY_PAGEDOWN    = 0x4E;
    public static final byte KEY_RIGHT       = 0x4F;
    public static final byte KEY_LEFT        = 0x50;
    public static final byte KEY_DOWN        = 0x51;
    public static final byte KEY_UP          = 0x52;

    // Numpad Keys
    public static final byte KEY_NUMLOCK     = 0x53;
    public static final byte KEY_KPSLASH     = 0x54;
    public static final byte KEY_KPASTERISK  = 0x55;
    public static final byte KEY_KPMINUS     = 0x56;
    public static final byte KEY_KPPLUS      = 0x57;
    public static final byte KEY_KPENTER     = 0x58;
    public static final byte KEY_KP1         = 0x59;
    public static final byte KEY_KP2         = 0x5A;
    public static final byte KEY_KP3         = 0x5B;
    public static final byte KEY_KP4         = 0x5C;
    public static final byte KEY_KP5         = 0x5D;
    public static final byte KEY_KP6         = 0x5E;
    public static final byte KEY_KP7         = 0x5F;
    public static final byte KEY_KP8         = 0x60;
    public static final byte KEY_KP9         = 0x61;
    public static final byte KEY_KP0         = 0x62;
    public static final byte KEY_KPDOT       = 0x63;
}
