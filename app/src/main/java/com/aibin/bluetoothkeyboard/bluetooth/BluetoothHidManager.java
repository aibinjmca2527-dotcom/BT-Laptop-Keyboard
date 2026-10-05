package com.aibin.bluetoothkeyboard.bluetooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHidDevice;
import android.bluetooth.BluetoothHidDeviceAppQosSettings;
import android.bluetooth.BluetoothHidDeviceAppSdpSettings;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Executors;

/**
 * Manages the Bluetooth HID Device profile (Android 9+).
 * Emulates a standard USB/Bluetooth HID Keyboard.
 * Works natively across Windows, macOS, and Linux without any companion software.
 */
public class BluetoothHidManager {
    private static final String TAG = "BluetoothHidManager";

    // Standard HID Keyboard Report Descriptor
    private static final byte[] HID_REPORT_DESCRIPTOR = new byte[] {
        (byte) 0x05, (byte) 0x01,  // USAGE_PAGE (Generic Desktop)
        (byte) 0x09, (byte) 0x06,  // USAGE (Keyboard)
        (byte) 0xa1, (byte) 0x01,  // COLLECTION (Application)
        (byte) 0x85, (byte) 0x01,  //   REPORT_ID (1)
        (byte) 0x05, (byte) 0x07,  //   USAGE_PAGE (Keyboard)
        (byte) 0x19, (byte) 0xe0,  //   USAGE_MINIMUM (Keyboard LeftControl)
        (byte) 0x29, (byte) 0xe7,  //   USAGE_MAXIMUM (Keyboard Right GUI)
        (byte) 0x15, (byte) 0x00,  //   LOGICAL_MINIMUM (0)
        (byte) 0x25, (byte) 0x01,  //   LOGICAL_MAXIMUM (1)
        (byte) 0x75, (byte) 0x01,  //   REPORT_SIZE (1)
        (byte) 0x95, (byte) 0x08,  //   REPORT_COUNT (8)
        (byte) 0x81, (byte) 0x02,  //   INPUT (Data,Var,Abs) - Modifier Byte
        (byte) 0x95, (byte) 0x01,  //   REPORT_COUNT (1)
        (byte) 0x75, (byte) 0x08,  //   REPORT_SIZE (8)
        (byte) 0x81, (byte) 0x03,  //   INPUT (Cnst,Var,Abs) - Reserved Byte
        (byte) 0x95, (byte) 0x05,  //   REPORT_COUNT (5)
        (byte) 0x75, (byte) 0x01,  //   REPORT_SIZE (1)
        (byte) 0x05, (byte) 0x08,  //   USAGE_PAGE (LEDs)
        (byte) 0x19, (byte) 0x01,  //   USAGE_MINIMUM (Num Lock)
        (byte) 0x29, (byte) 0x05,  //   USAGE_MAXIMUM (Kana)
        (byte) 0x91, (byte) 0x02,  //   OUTPUT (Data,Var,Abs) - LED report
        (byte) 0x95, (byte) 0x01,  //   REPORT_COUNT (1)
        (byte) 0x75, (byte) 0x03,  //   REPORT_SIZE (3)
        (byte) 0x91, (byte) 0x03,  //   OUTPUT (Cnst,Var,Abs) - LED report padding
        (byte) 0x95, (byte) 0x06,  //   REPORT_COUNT (6)
        (byte) 0x75, (byte) 0x08,  //   REPORT_SIZE (8)
        (byte) 0x15, (byte) 0x00,  //   LOGICAL_MINIMUM (0)
        (byte) 0x25, (byte) 0x65,  //   LOGICAL_MAXIMUM (101)
        (byte) 0x05, (byte) 0x07,  //   USAGE_PAGE (Keyboard)
        (byte) 0x19, (byte) 0x00,  //   USAGE_MINIMUM (Reserved)
        (byte) 0x29, (byte) 0x65,  //   USAGE_MAXIMUM (Keyboard Application)
        (byte) 0x81, (byte) 0x00,  //   INPUT (Data,Ary,Abs) - Key array (up to 6 keys)
        (byte) 0xc0                // END_COLLECTION
    };

    public interface HidConnectionListener {
        void onAppRegistered(boolean success);
        void onDeviceConnectionChanged(BluetoothDevice device, int state);
    }

    private static BluetoothHidManager instance;
    private final Context context;
    private final BluetoothAdapter bluetoothAdapter;
    private BluetoothHidDevice hidDevice;
    private BluetoothDevice connectedDevice;
    private BluetoothDevice pendingConnectDevice;
    private boolean isAppRegistered = false;
    private final java.util.List<HidConnectionListener> listeners = new java.util.concurrent.CopyOnWriteArrayList<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final byte[] keyboardReport = new byte[8];

    private BluetoothHidManager(Context context) {
        this.context = context.getApplicationContext();
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public static synchronized BluetoothHidManager getInstance(Context context) {
        if (instance == null) {
            instance = new BluetoothHidManager(context);
        }
        return instance;
    }

    public void registerListener(HidConnectionListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void unregisterListener(HidConnectionListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    public void setConnectionListener(HidConnectionListener listener) {
        // Compatibility wrapper: clear and register
        registerListener(listener);
    }

    public boolean isBluetoothSupported() {
        return bluetoothAdapter != null;
    }

    public boolean isBluetoothEnabled() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    public boolean isConnected() {
        return connectedDevice != null && hidDevice != null;
    }

    public boolean isAppRegistered() {
        return isAppRegistered && hidDevice != null;
    }

    public BluetoothDevice getConnectedDevice() {
        return connectedDevice;
    }

    @SuppressLint("MissingPermission")
    public String getConnectedDeviceName() {
        if (connectedDevice != null) {
            String name = connectedDevice.getName();
            return name != null && !name.isEmpty() ? name : connectedDevice.getAddress();
        }
        return null;
    }

    @SuppressLint("MissingPermission")
    public Set<BluetoothDevice> getBondedDevices() {
        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
            return bluetoothAdapter.getBondedDevices();
        }
        return Collections.emptySet();
    }

    @SuppressLint("MissingPermission")
    public void registerHidDevice() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Log.e(TAG, "Bluetooth not enabled or not available");
            return;
        }

        if (hidDevice != null && isAppRegistered) {
            Log.d(TAG, "HID Device already registered");
            return; // Already registered
        }

        if (hidDevice != null && !isAppRegistered) {
            Log.i(TAG, "HID Device proxy already available, registering app directly...");
            registerApp();
            return;
        }

        Log.i(TAG, "Requesting Bluetooth HID Device profile proxy...");
        boolean proxyRequested = bluetoothAdapter.getProfileProxy(context, new BluetoothProfile.ServiceListener() {
            @Override
            public void onServiceConnected(int profile, BluetoothProfile proxy) {
                if (profile == BluetoothProfile.HID_DEVICE) {
                    Log.i(TAG, "Bluetooth HID Device profile proxy connected");
                    hidDevice = (BluetoothHidDevice) proxy;
                    registerApp();
                }
            }

            @Override
            public void onServiceDisconnected(int profile) {
                if (profile == BluetoothProfile.HID_DEVICE) {
                    Log.w(TAG, "Bluetooth HID Device profile disconnected");
                    hidDevice = null;
                    isAppRegistered = false;
                    connectedDevice = null;
                    notifyAppRegistered(false);
                }
            }
        }, BluetoothProfile.HID_DEVICE);

        if (!proxyRequested) {
            Log.e(TAG, "bluetoothAdapter.getProfileProxy returned false for HID_DEVICE");
            notifyAppRegistered(false);
        }
    }

    @SuppressLint("MissingPermission")
    private void registerApp() {
        if (hidDevice == null) return;

        BluetoothHidDeviceAppSdpSettings sdp = new BluetoothHidDeviceAppSdpSettings(
            "BT Laptop Keyboard",
            "Aibin Joseph's Android Bluetooth Keyboard",
            "Aibin Joseph",
            (byte) 0x40, // Standard HID Keyboard Subclass
            HID_REPORT_DESCRIPTOR
        );

        // Using null for inQos and outQos uses standard system L2CAP defaults.
        // Custom QoS parameters cause registration/connection drops on many OEM chips (e.g. MediaTek Helio on realme 7).
        boolean ok = hidDevice.registerApp(sdp, null, null, Executors.newSingleThreadExecutor(), hidCallback);
        Log.i(TAG, "hidDevice.registerApp() returned: " + ok);
        if (!ok) {
            notifyAppRegistered(false);
        }
    }

    private void notifyAppRegistered(boolean registered) {
        mainHandler.post(() -> {
            for (HidConnectionListener l : listeners) {
                try {
                    l.onAppRegistered(registered);
                } catch (Exception e) {
                    Log.e(TAG, "Error in onAppRegistered callback", e);
                }
            }
        });
    }

    private void notifyDeviceConnectionChanged(BluetoothDevice device, int state) {
        mainHandler.post(() -> {
            for (HidConnectionListener l : listeners) {
                try {
                    l.onDeviceConnectionChanged(device, state);
                } catch (Exception e) {
                    Log.e(TAG, "Error in onDeviceConnectionChanged callback", e);
                }
            }
        });
    }

    private final BluetoothHidDevice.Callback hidCallback = new BluetoothHidDevice.Callback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onAppStatusChanged(BluetoothDevice pluggedDevice, boolean registered) {
            Log.i(TAG, "onAppStatusChanged registered=" + registered);
            isAppRegistered = registered;
            if (registered && pendingConnectDevice != null && hidDevice != null) {
                BluetoothDevice target = pendingConnectDevice;
                pendingConnectDevice = null;
                hidDevice.connect(target);
            }
            notifyAppRegistered(registered);
        }

        @Override
        public void onConnectionStateChanged(BluetoothDevice device, int state) {
            Log.i(TAG, "onConnectionStateChanged: device=" + device.getAddress() + " state=" + state);
            if (state == BluetoothProfile.STATE_CONNECTED) {
                connectedDevice = device;
                pendingConnectDevice = null;
            } else if (state == BluetoothProfile.STATE_DISCONNECTED) {
                if (connectedDevice != null && connectedDevice.equals(device)) {
                    connectedDevice = null;
                }
            }
            notifyDeviceConnectionChanged(device, state);
        }

        @Override
        public void onGetReport(BluetoothDevice device, byte type, byte id, int bufferSize) {
            if (id == 1 && hidDevice != null) {
                hidDevice.replyReport(device, type, id, keyboardReport);
            }
        }

        @Override
        public void onSetReport(BluetoothDevice device, byte type, byte id, byte[] data) {
            // Handle Keyboard LED reports if sent by host
        }
    };

    @SuppressLint("MissingPermission")
    public boolean connectDevice(BluetoothDevice device) {
        if (device == null) return false;

        // 1. If device is not bonded yet, create pairing bond first!
        if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
            Log.i(TAG, "Device not bonded. Creating bond with " + device.getAddress());
            pendingConnectDevice = device;
            return device.createBond();
        }

        // 2. If HID proxy is not initialized yet, register and queue
        if (hidDevice == null || !isAppRegistered) {
            Log.i(TAG, "HID Device not ready yet. Queueing connection to " + device.getAddress());
            pendingConnectDevice = device;
            registerHidDevice();
            return true;
        }

        // 3. Connect via HID device profile
        Log.i(TAG, "Connecting to device " + device.getAddress());
        boolean initiated = hidDevice.connect(device);
        if (!initiated) {
            Log.w(TAG, "hidDevice.connect(device) returned false");
            notifyDeviceConnectionChanged(device, BluetoothProfile.STATE_DISCONNECTED);
        }
        return initiated;
    }

    @SuppressLint("MissingPermission")
    public void onBondStateChanged(BluetoothDevice device, int bondState) {
        if (bondState == BluetoothDevice.BOND_BONDED && pendingConnectDevice != null
                && pendingConnectDevice.getAddress().equals(device.getAddress())) {
            connectDevice(device);
        } else if (bondState == BluetoothDevice.BOND_NONE && pendingConnectDevice != null
                && pendingConnectDevice.getAddress().equals(device.getAddress())) {
            pendingConnectDevice = null;
            notifyDeviceConnectionChanged(device, BluetoothProfile.STATE_DISCONNECTED);
        }
    }

    @SuppressLint("MissingPermission")
    public void disconnectDevice() {
        if (hidDevice != null && connectedDevice != null) {
            Log.i(TAG, "Disconnecting device " + connectedDevice.getAddress());
            hidDevice.disconnect(connectedDevice);
            connectedDevice = null;
        }
        pendingConnectDevice = null;
    }

    /**
     * Sends a key press followed immediately by key release, with optional modifiers.
     */
    public synchronized void sendKeyPress(byte modifiers, byte keyCode) {
        if (hidDevice == null || connectedDevice == null) {
            Log.w(TAG, "Cannot send key: Not connected");
            return;
        }

        // 1. Key Down Report
        keyboardReport[0] = modifiers;
        keyboardReport[1] = 0; // Reserved
        keyboardReport[2] = keyCode;
        keyboardReport[3] = 0;
        keyboardReport[4] = 0;
        keyboardReport[5] = 0;
        keyboardReport[6] = 0;
        keyboardReport[7] = 0;
        sendReport(keyboardReport);

        // 2. Short pause then Key Up Report
        mainHandler.postDelayed(() -> {
            keyboardReport[0] = 0;
            keyboardReport[2] = 0;
            sendReport(keyboardReport);
        }, 15);
    }

    /**
     * Sends raw Key Down event.
     */
    public synchronized void sendKeyDown(byte modifiers, byte keyCode) {
        if (hidDevice == null || connectedDevice == null) return;
        keyboardReport[0] = modifiers;
        keyboardReport[1] = 0;
        keyboardReport[2] = keyCode;
        sendReport(keyboardReport);
    }

    /**
     * Sends raw Key Up event.
     */
    public synchronized void sendKeyUp() {
        if (hidDevice == null || connectedDevice == null) return;
        keyboardReport[0] = 0;
        keyboardReport[2] = 0;
        sendReport(keyboardReport);
    }

    @SuppressLint("MissingPermission")
    private void sendReport(byte[] report) {
        if (hidDevice != null && connectedDevice != null) {
            try {
                hidDevice.sendReport(connectedDevice, 1, report);
            } catch (Exception e) {
                Log.e(TAG, "Failed to send HID report", e);
            }
        }
    }

    @SuppressLint("MissingPermission")
    public void unregister() {
        if (hidDevice != null && isAppRegistered) {
            try {
                hidDevice.unregisterApp();
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering HID app", e);
            }
            isAppRegistered = false;
        }
    }
}
