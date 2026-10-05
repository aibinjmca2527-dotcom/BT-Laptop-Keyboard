# BT Laptop Keyboard - Native Android Application

**Developer:** Aibin Joseph  
**Version:** 1.0.0  
**Stack:** Java + XML (Native Android, MinSDK 28 / TargetSDK 34)  
**Supported Laptop OS:** Windows 10/11, macOS (Intel & Apple Silicon), Linux (Ubuntu, Fedora, Arch, Debian, etc.)

---

## 🌟 Overview

**BT Laptop Keyboard** transforms your Android smartphone into a full-size, tactile, professional laptop keyboard. By utilizing Android's native **Bluetooth HID (Human Interface Device) Device Profile**, your phone presents itself to your laptop as a standard physical wireless keyboard.

### Key Highlights
- **Zero Laptop Software Required:** Works right out of the box with Windows, macOS, and Linux through standard OS Bluetooth pairing.
- **Full Laptop Keyboard Layout:** Authentic 75-key layout including Esc, Function Keys (`F1`–`F12`), Numbers & Symbols, QWERTY, Modifiers (`Shift`, `Caps`, `Ctrl`, `Alt`, `Win`/`Cmd`), Space, Enter, and Arrow navigation keys.
- **Active / Inactive Keyboard States:** Initially, keys prompt for connection when not paired. Once connected, keys illuminate and send real-time keystrokes.
- **Sleek Mechanical Aesthetic:** Brushed dark theme, 3D mechanical bevels, glowing pressed states, and responsive key elevation.
- **Haptic & Audio Feedback:** Real-time tactile vibration pulse and mechanical click sounds on every key press.
- **Dynamic OS Labels:** Instantly switch between Windows/Linux (`Win` / `Alt`) and macOS (`Cmd ⌘` / `Opt ⌥`) modifier key labels.
- **Keep Screen Awake:** Prevents phone timeout during typing sessions.

---

## 📥 Download App (APK)

Get the latest ready-to-install Android APK directly from this repository:

👉 **[Download BT_Laptop_Keyboard.apk (Direct Link)](https://github.com/aibinjmca2527-dotcom/BT-Laptop-Keyboard/raw/main/BT_Laptop_Keyboard.apk)**

*(Or download the `BT_Laptop_Keyboard.apk` file directly from the root of this GitHub repository)*

To install directly to a connected phone with USB debugging enabled:
```powershell
adb install -r BT_Laptop_Keyboard.apk
```

---

## 💻 How to Connect with Your Laptop

### Step 1: Open the App
1. Launch **BT Laptop Keyboard** on your phone.
2. Tap the **Bluetooth icon / Status badge** or the **Settings gear icon** in the top bar.

### Step 2: Enable Bluetooth & Make Discoverable
1. Ensure the **Bluetooth switch** is turned ON in Settings.
2. Tap **"Make Phone Discoverable"** (this makes your phone visible to nearby laptops for 2 minutes).

### Step 3: Pair from Your Laptop
- **Windows (10 / 11):**
  1. Open **Settings** > **Bluetooth & devices**.
  2. Click **Add device** > **Bluetooth**.
  3. Select your phone (or `"BT Laptop Keyboard"`) and confirm the pairing code on both phone and laptop.
- **macOS:**
  1. Open **System Settings** > **Bluetooth**.
  2. Look under **Nearby Devices**, find your phone, and click **Connect / Pair**.
- **Linux:**
  1. Open your desktop Bluetooth manager (GNOME Settings / Blueman / `bluetoothctl`).
  2. Pair with your phone and trust the device.

### Step 4: Start Typing!
1. In the app Settings under **"Paired Laptops & Devices"**, tap **"Connect"** next to your laptop.
2. The top bar status turns `🟢 Connected: [Your Laptop Name]`.
3. Switch back to the keyboard and start typing into any text editor, browser, terminal, or game on your laptop!

---

## ⌨ Keyboard Layout Details

| Row | Keys |
| :--- | :--- |
| **Function Row** | `Esc`, `F1`, `F2`, `F3`, `F4`, `F5`, `F6`, `F7`, `F8`, `F9`, `F10`, `F11`, `F12`, `Del` |
| **Number Row** | `` `~ ``, `1!`, `2@`, `3#`, `4$`, `5%`, `6^`, `7&`, `8*`, `9(`, `0)`, `-_`, `=+`, `Backspace ⌫` |
| **Row 1 (QWERTY)**| `Tab ⇥`, `Q`, `W`, `E`, `R`, `T`, `Y`, `U`, `I`, `O`, `P`, `[{`, `]}`, `\\|` |
| **Row 2 (Home)** | `Caps Lock ⇪`, `A`, `S`, `D`, `F`, `G`, `H`, `J`, `K`, `L`, `;:`, `'\"`, `Enter ↵` |
| **Row 3 (Shift)**| `Shift ⇧ (L)`, `Z`, `X`, `C`, `V`, `B`, `N`, `M`, `,<`, `.>`, `/?`, `Shift ⇧ (R)` |
| **Row 4 (Bottom)**| `Ctrl (L)`, `Win / Cmd`, `Alt / Opt`, `Spacebar`, `Alt / Opt`, `Ctrl (R)`, `◀`, `▲`, `▼`, `▶` |

---

## ⚙ Settings & Customization

Located in the **Settings** menu:
- **Bluetooth Discovery & Device Manager:** Pair, scan, connect, or disconnect.
- **Vibration / Haptic Feedback:** Turn on/off tactile keypress vibrations.
- **Mechanical Key Click Sounds:** Toggle audio key clicks.
- **Keep Screen Awake:** Toggle `FLAG_KEEP_SCREEN_ON` so your phone doesn't sleep while typing.
- **OS Key Labels:** Radio toggle between `Windows / Linux` and `macOS (Cmd / Opt)`.
- **Developer & Version Info:** Displays developer name (**Aibin Joseph**) and version (`1.0.0`).

---

## 🛠 Project Structure

```
c:\keyboard_app\
├── app\
│   ├── src\main\
│   │   ├── AndroidManifest.xml
│   │   ├── java\com\aibin\bluetoothkeyboard\
│   │   │   ├── MainActivity.java           (Full keyboard UI, touch events & status)
│   │   │   ├── SettingsActivity.java       (Bluetooth scanner, paired list, preferences)
│   │   │   ├── bluetooth\
│   │   │   │   ├── BluetoothHidManager.java (HID Device profile, SDP registration & reports)
│   │   │   │   └── BluetoothDeviceAdapter.java (RecyclerView device list adapter)
│   │   │   ├── model\
│   │   │   │   ├── HidKeyCodes.java        (Standard USB HID scan codes)
│   │   │   │   └── KeyDef.java             (Key mapping & modifier metadata)
│   │   │   └── util\
│   │   │       ├── PreferencesManager.java (Settings persistence)
│   │   │       └── SoundHapticHelper.java  (Audio click & haptic vibration feedback)
│   │   └── res\
│   │       ├── layout\
│   │       │   ├── activity_main.xml       (Complete 75-key laptop keyboard layout)
│   │       │   ├── activity_settings.xml   (Settings & Bluetooth management)
│   │       │   ├── dialog_connect_prompt.xml (Disconnected warning popup)
│   │       │   └── item_bluetooth_device.xml (Device list item card)
│   │       ├── drawable\                   (Mechanical 3D key states, vectors & styles)
│   │       └── values\                     (colors.xml, strings.xml, styles.xml, themes.xml)
│   └── build.gradle
├── gradle\
├── gradlew
├── gradlew.bat
├── settings.gradle
├── laptop_test_receiver.py                 (Optional cross-platform companion tester)
└── README.md
```
