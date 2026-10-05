package com.aibin.bluetoothkeyboard;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aibin.bluetoothkeyboard.bluetooth.BluetoothDeviceAdapter;
import com.aibin.bluetoothkeyboard.bluetooth.BluetoothHidManager;
import com.aibin.bluetoothkeyboard.util.PreferencesManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SettingsActivity extends AppCompatActivity {

    private PreferencesManager preferencesManager;
    private BluetoothHidManager hidManager;
    private BluetoothAdapter bluetoothAdapter;

    private SwitchMaterial switchBluetooth;
    private MaterialButton btnMakeDiscoverable;
    private MaterialButton btnScanDevices;
    private TextView tvNoPairedDevices;
    private TextView tvHidServiceStatus;
    private LinearLayout layoutScannedSection;

    private RecyclerView rvPairedDevices;
    private RecyclerView rvScannedDevices;
    private BluetoothDeviceAdapter pairedAdapter;
    private BluetoothDeviceAdapter scannedAdapter;

    private SwitchMaterial switchHaptic;
    private SwitchMaterial switchSound;
    private SwitchMaterial switchKeepAwake;
    private RadioGroup rgOsMode;
    private RadioButton rbWindows;
    private RadioButton rbMacOs;

    private String connectingAddress = null;
    private BluetoothDevice connectingDevice = null;
    private final Handler timeoutHandler = new Handler(Looper.getMainLooper());
    private Runnable connectionTimeoutRunnable;

    private ActivityResultLauncher<String[]> permissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        preferencesManager = new PreferencesManager(this);
        hidManager = BluetoothHidManager.getInstance(this);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        setupPermissionLauncher();
        initViews();
        setupListeners();
        registerBtReceivers();
        loadSettings();

        if (hidManager.isBluetoothEnabled()) {
            hidManager.registerHidDevice();
        }

        updateHidStatus();
        refreshDeviceLists();
    }

    private void setupPermissionLauncher() {
        permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> {
                boolean allGranted = true;
                for (Boolean granted : result.values()) {
                    if (!granted) {
                        allGranted = false;
                        break;
                    }
                }
                if (allGranted) {
                    refreshDeviceLists();
                } else {
                    Toast.makeText(this, "Bluetooth permissions are required to scan & connect", Toast.LENGTH_LONG).show();
                }
            }
        );
    }

    private void checkAndRequestPermissions() {
        List<String> neededPermissions = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(Manifest.permission.BLUETOOTH_SCAN);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(Manifest.permission.BLUETOOTH_ADVERTISE);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                neededPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }
        }

        if (!neededPermissions.isEmpty()) {
            permissionLauncher.launch(neededPermissions.toArray(new String[0]));
        }
    }

    private void initViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        // Direct click listener to reliably return to keyboard
        toolbar.setNavigationOnClickListener(v -> finish());

        switchBluetooth = findViewById(R.id.switchBluetooth);
        btnMakeDiscoverable = findViewById(R.id.btnMakeDiscoverable);
        btnScanDevices = findViewById(R.id.btnScanDevices);
        tvNoPairedDevices = findViewById(R.id.tvNoPairedDevices);
        tvHidServiceStatus = findViewById(R.id.tvHidServiceStatus);
        layoutScannedSection = findViewById(R.id.layoutScannedSection);

        rvPairedDevices = findViewById(R.id.rvPairedDevices);
        rvPairedDevices.setLayoutManager(new LinearLayoutManager(this));
        pairedAdapter = new BluetoothDeviceAdapter(this::onDeviceActionClicked);
        rvPairedDevices.setAdapter(pairedAdapter);

        rvScannedDevices = findViewById(R.id.rvScannedDevices);
        rvScannedDevices.setLayoutManager(new LinearLayoutManager(this));
        scannedAdapter = new BluetoothDeviceAdapter(this::onDeviceActionClicked);
        rvScannedDevices.setAdapter(scannedAdapter);

        switchHaptic = findViewById(R.id.switchHaptic);
        switchSound = findViewById(R.id.switchSound);
        switchKeepAwake = findViewById(R.id.switchKeepAwake);
        rgOsMode = findViewById(R.id.rgOsMode);
        rbWindows = findViewById(R.id.rbWindows);
        rbMacOs = findViewById(R.id.rbMacOs);
    }

    private void updateHidStatus() {
        if (tvHidServiceStatus == null) return;
        if (hidManager.isAppRegistered()) {
            tvHidServiceStatus.setText("HID Keyboard Profile: Ready 🟢");
            tvHidServiceStatus.setTextColor(ContextCompat.getColor(this, R.color.status_connected));
        } else {
            tvHidServiceStatus.setText("HID Keyboard Profile: Registering... ⏳");
            tvHidServiceStatus.setTextColor(ContextCompat.getColor(this, R.color.key_accent));
        }
    }

    private void loadSettings() {
        if (bluetoothAdapter != null) {
            switchBluetooth.setChecked(bluetoothAdapter.isEnabled());
        }

        switchHaptic.setChecked(preferencesManager.isHapticEnabled());
        switchSound.setChecked(preferencesManager.isSoundEnabled());
        switchKeepAwake.setChecked(preferencesManager.isKeepAwakeEnabled());

        String osMode = preferencesManager.getOsMode();
        if ("macos".equalsIgnoreCase(osMode)) {
            rbMacOs.setChecked(true);
        } else {
            rbWindows.setChecked(true);
        }
    }

    @SuppressLint("MissingPermission")
    private void setupListeners() {
        switchBluetooth.setOnCheckedChangeListener((buttonView, isChecked) -> {
            checkAndRequestPermissions();
            if (bluetoothAdapter != null) {
                if (isChecked && !bluetoothAdapter.isEnabled()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivity(enableBtIntent);
                } else if (!isChecked && bluetoothAdapter.isEnabled()) {
                    bluetoothAdapter.disable();
                }
            }
        });

        btnMakeDiscoverable.setOnClickListener(v -> {
            checkAndRequestPermissions();
            hidManager.registerHidDevice();
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 120);
            startActivity(discoverableIntent);
            Toast.makeText(this, "Phone is discoverable for 2 min. Pair from your laptop via 'Add device' > 'Bluetooth'", Toast.LENGTH_LONG).show();
        });

        btnScanDevices.setOnClickListener(v -> {
            checkAndRequestPermissions();
            if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
                Toast.makeText(this, "Enable Bluetooth first", Toast.LENGTH_SHORT).show();
                return;
            }

            if (bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
                btnScanDevices.setText(R.string.btn_scan_devices);
            } else {
                layoutScannedSection.setVisibility(View.VISIBLE);
                scannedAdapter.setDevices(new ArrayList<>(), hidManager.getConnectedDevice(), connectingAddress);
                bluetoothAdapter.startDiscovery();
                btnScanDevices.setText(R.string.btn_stop_scan);
                Toast.makeText(this, "Scanning for nearby laptops...", Toast.LENGTH_SHORT).show();
            }
        });

        switchHaptic.setOnCheckedChangeListener((buttonView, isChecked) ->
            preferencesManager.setHapticEnabled(isChecked));

        switchSound.setOnCheckedChangeListener((buttonView, isChecked) ->
            preferencesManager.setSoundEnabled(isChecked));

        switchKeepAwake.setOnCheckedChangeListener((buttonView, isChecked) ->
            preferencesManager.setKeepAwakeEnabled(isChecked));

        rgOsMode.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbMacOs) {
                preferencesManager.setOsMode("macos");
            } else {
                preferencesManager.setOsMode("windows");
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void refreshDeviceLists() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            pairedAdapter.setDevices(new ArrayList<>(), null, connectingAddress);
            tvNoPairedDevices.setVisibility(View.VISIBLE);
            return;
        }

        try {
            Set<BluetoothDevice> paired = bluetoothAdapter.getBondedDevices();
            if (paired != null && !paired.isEmpty()) {
                List<BluetoothDevice> list = new ArrayList<>(paired);
                pairedAdapter.setDevices(list, hidManager.getConnectedDevice(), connectingAddress);
                tvNoPairedDevices.setVisibility(View.GONE);
            } else {
                pairedAdapter.setDevices(new ArrayList<>(), null, connectingAddress);
                tvNoPairedDevices.setVisibility(View.VISIBLE);
            }
        } catch (SecurityException se) {
            checkAndRequestPermissions();
        }
    }

    @SuppressLint("MissingPermission")
    private void onDeviceActionClicked(BluetoothDevice device) {
        String address = device.getAddress();

        // 1. If currently connected, disconnect
        if (hidManager.isConnected() && hidManager.getConnectedDevice() != null
                && address.equals(hidManager.getConnectedDevice().getAddress())) {
            cancelConnectionTimeout();
            connectingAddress = null;
            connectingDevice = null;
            hidManager.disconnectDevice();
            Toast.makeText(this, "Disconnected from " + device.getName(), Toast.LENGTH_SHORT).show();
            refreshDeviceLists();
            return;
        }

        // 2. Already connecting to this device, ignore click
        if (connectingAddress != null && connectingAddress.equals(address)) {
            return;
        }

        // 3. Initiate Connection
        connectingAddress = address;
        connectingDevice = device;
        pairedAdapter.setConnectingAddress(connectingAddress);
        scannedAdapter.setConnectingAddress(connectingAddress);

        String devName = device.getName() != null && !device.getName().isEmpty() ? device.getName() : address;
        Toast.makeText(this, "Connecting to " + devName + "...", Toast.LENGTH_SHORT).show();

        hidManager.connectDevice(device);
        preferencesManager.setLastDeviceAddress(address);

        // 4. Set 12-second timeout in case laptop does not respond
        cancelConnectionTimeout();
        connectionTimeoutRunnable = () -> {
            if (connectingAddress != null && !hidManager.isConnected()) {
                BluetoothDevice failedDevice = connectingDevice;
                connectingAddress = null;
                connectingDevice = null;
                pairedAdapter.setConnectingAddress(null);
                scannedAdapter.setConnectingAddress(null);
                refreshDeviceLists();
                showWindowsPairingTroubleshoot(failedDevice);
            }
        };
        timeoutHandler.postDelayed(connectionTimeoutRunnable, 12000);
    }

    private void showWindowsPairingTroubleshoot(BluetoothDevice device) {
        String devName = (device != null && device.getName() != null) ? device.getName() : "Laptop";
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Cannot Connect to " + devName)
            .setMessage("Windows often pairs phones under 'Other devices' (PAN / Audio) instead of an Input Keyboard, causing Windows to reject HID keyboard connections.\n\n"
                + "Easy 1-minute fix:\n"
                + "1. On Laptop: Open Bluetooth & devices settings.\n"
                + "2. Find your phone under 'Other devices' and click 'Remove this device'.\n"
                + "3. On this Phone: Tap 'Make Discoverable' below.\n"
                + "4. On Laptop: Click 'Add device' > 'Bluetooth' and select your phone.\n\n"
                + "Windows will now pair your phone directly as an Input Keyboard!")
            .setPositiveButton("Make Phone Discoverable", (dialog, which) -> {
                btnMakeDiscoverable.performClick();
            })
            .setNegativeButton("Close", null)
            .show();
    }

    private void cancelConnectionTimeout() {
        if (connectionTimeoutRunnable != null) {
            timeoutHandler.removeCallbacks(connectionTimeoutRunnable);
            connectionTimeoutRunnable = null;
        }
    }

    private void registerBtReceivers() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(btReceiver, filter);

        hidManager.registerListener(hidConnectionListener);
    }

    private final BluetoothHidManager.HidConnectionListener hidConnectionListener = new BluetoothHidManager.HidConnectionListener() {
        @Override
        public void onAppRegistered(boolean success) {
            runOnUiThread(() -> {
                updateHidStatus();
                refreshDeviceLists();
            });
        }

        @Override
        public void onDeviceConnectionChanged(BluetoothDevice device, int state) {
            runOnUiThread(() -> {
                updateHidStatus();
                if (state == BluetoothProfile.STATE_CONNECTED) {
                    cancelConnectionTimeout();
                    connectingAddress = null;
                    connectingDevice = null;
                    refreshDeviceLists();
                    String name = device.getName() != null ? device.getName() : "Laptop";
                    Toast.makeText(SettingsActivity.this, "🟢 Connected to " + name + "! Ready to type.", Toast.LENGTH_SHORT).show();
                } else if (state == BluetoothProfile.STATE_DISCONNECTED) {
                    boolean wasConnecting = (connectingAddress != null && device != null && device.getAddress().equals(connectingAddress));
                    BluetoothDevice failedDevice = connectingDevice != null ? connectingDevice : device;
                    cancelConnectionTimeout();
                    connectingAddress = null;
                    connectingDevice = null;
                    refreshDeviceLists();
                    if (wasConnecting) {
                        showWindowsPairingTroubleshoot(failedDevice);
                    }
                } else if (state == BluetoothProfile.STATE_CONNECTING) {
                    connectingAddress = device.getAddress();
                    connectingDevice = device;
                    pairedAdapter.setConnectingAddress(connectingAddress);
                    scannedAdapter.setConnectingAddress(connectingAddress);
                }
            });
        }
    };

    private final BroadcastReceiver btReceiver = new BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null) {
                    scannedAdapter.addDevice(device);
                }
            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE);
                if (device != null) {
                    hidManager.onBondStateChanged(device, bondState);
                    refreshDeviceLists();
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                btnScanDevices.setText(R.string.btn_scan_devices);
            } else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                if (state == BluetoothAdapter.STATE_ON) {
                    switchBluetooth.setChecked(true);
                    refreshDeviceLists();
                    hidManager.registerHidDevice();
                } else if (state == BluetoothAdapter.STATE_OFF) {
                    switchBluetooth.setChecked(false);
                    cancelConnectionTimeout();
                    connectingAddress = null;
                    refreshDeviceLists();
                }
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        checkAndRequestPermissions();
        if (hidManager.isBluetoothEnabled()) {
            hidManager.registerHidDevice();
        }
        updateHidStatus();
        refreshDeviceLists();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelConnectionTimeout();
        hidManager.unregisterListener(hidConnectionListener);
        try {
            unregisterReceiver(btReceiver);
        } catch (Exception ignored) {}
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
