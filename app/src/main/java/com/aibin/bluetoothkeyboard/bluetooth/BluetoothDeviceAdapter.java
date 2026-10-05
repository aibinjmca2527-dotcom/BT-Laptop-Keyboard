package com.aibin.bluetoothkeyboard.bluetooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aibin.bluetoothkeyboard.R;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class BluetoothDeviceAdapter extends RecyclerView.Adapter<BluetoothDeviceAdapter.DeviceViewHolder> {

    public interface OnDeviceClickListener {
        void onConnectClick(BluetoothDevice device);
    }

    private final List<BluetoothDevice> deviceList = new ArrayList<>();
    private final OnDeviceClickListener listener;
    private BluetoothDevice connectedDevice;
    private String connectingAddress;

    public BluetoothDeviceAdapter(OnDeviceClickListener listener) {
        this.listener = listener;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setDevices(List<BluetoothDevice> devices, BluetoothDevice connected, String connectingAddress) {
        this.deviceList.clear();
        if (devices != null) {
            this.deviceList.addAll(devices);
        }
        this.connectedDevice = connected;
        this.connectingAddress = connectingAddress;
        notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setConnectingAddress(String address) {
        this.connectingAddress = address;
        notifyDataSetChanged();
    }

    public void addDevice(BluetoothDevice device) {
        if (device == null || device.getAddress() == null) return;
        for (BluetoothDevice d : deviceList) {
            if (device.getAddress().equals(d.getAddress())) {
                return; // Already in list
            }
        }
        deviceList.add(device);
        notifyItemInserted(deviceList.size() - 1);
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_bluetooth_device, parent, false);
        return new DeviceViewHolder(view);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        BluetoothDevice device = deviceList.get(position);

        String name = device.getName();
        if (name == null || name.trim().isEmpty()) {
            name = "Laptop / Computer";
        }
        holder.tvName.setText(name);
        holder.tvAddress.setText(device.getAddress());

        String address = device.getAddress();
        boolean isConnected = connectedDevice != null && address.equals(connectedDevice.getAddress());
        boolean isConnecting = connectingAddress != null && address.equals(connectingAddress);

        if (isConnecting) {
            holder.btnConnect.setText("Connecting...");
            holder.btnConnect.setEnabled(false);
            holder.btnConnect.setBackgroundTintList(ColorStateList.valueOf(0xFFF59E0B)); // Amber
        } else if (isConnected) {
            holder.btnConnect.setText(R.string.action_disconnect);
            holder.btnConnect.setEnabled(true);
            holder.btnConnect.setBackgroundTintList(ColorStateList.valueOf(0xFFDC2626)); // Red
        } else {
            holder.btnConnect.setText(R.string.action_connect);
            holder.btnConnect.setEnabled(true);
            holder.btnConnect.setBackgroundTintList(ColorStateList.valueOf(0xFF2563EB)); // Blue
        }

        holder.btnConnect.setOnClickListener(v -> {
            if (listener != null && !isConnecting) {
                listener.onConnectClick(device);
            }
        });
    }

    @Override
    public int getItemCount() {
        return deviceList.size();
    }

    static class DeviceViewHolder extends RecyclerView.ViewHolder {
        final TextView tvName;
        final TextView tvAddress;
        final MaterialButton btnConnect;

        DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvDeviceName);
            tvAddress = itemView.findViewById(R.id.tvDeviceAddress);
            btnConnect = itemView.findViewById(R.id.btnConnectDevice);
        }
    }
}
