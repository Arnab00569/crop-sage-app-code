package com.example.cropsage;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Looper;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class BluetoothService {
    private static final UUID BT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // Standard Serial UUID
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket socket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private boolean isConnected = false;

    public BluetoothService() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    @SuppressLint("MissingPermission")
    public boolean connectToSensor(String deviceName) {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) return false;

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : pairedDevices) {
            if (device.getName().equals(deviceName)) {
                try {
                    socket = device.createRfcommSocketToServiceRecord(BT_UUID);
                    socket.connect();
                    inputStream = socket.getInputStream();
                    outputStream = socket.getOutputStream();
                    isConnected = true;
                    return true;
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }
            }
        }
        return false;
    }

    public void disconnect() {
        try {
            if (socket != null) socket.close();
            isConnected = false;
        } catch (IOException e) { e.printStackTrace(); }
    }

    public boolean isConnected() { return isConnected; }

    public void requestData(DataCallback callback) {
        if (!isConnected) return;

        new Thread(() -> {
            try {
                // 1. Send 'G' to ESP32
                outputStream.write('G');

                // 2. Wait for response
                byte[] buffer = new byte[1024];
                int bytes;

                // Give ESP32 a moment to reply
                Thread.sleep(500);

                if (inputStream.available() > 0) {
                    bytes = inputStream.read(buffer);
                    String jsonResponse = new String(buffer, 0, bytes);

                    // Send back to Main Thread
                    new Handler(Looper.getMainLooper()).post(() -> callback.onDataReceived(jsonResponse));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public interface DataCallback {
        void onDataReceived(String json);
    }
}