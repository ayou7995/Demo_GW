package com.example.ayou7995.demo_gw;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by ayou7995 on 2016/4/12.
 */
public class BLeGatt {
    private final static String TAG = "Jonathan";

    private BluetoothGatt mBluetoothGatt;
    protected List<BluetoothGattCharacteristic> characteristicList;

    private int mConnectionState;
    public static final int STATE_DISCONNECTED = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;
    public static final int STATE_AVAILABLE = 3;

    protected final static UUID SERVICE_UUID =
            UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb");
    protected final static UUID CHAR_NOTIFY_UUID =
            UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb");
    protected final static UUID CHAR_WRITE_UUID =
            UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb");
    protected final static UUID DESCRIPTOR_UUID =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    public BLeGatt(BluetoothGatt gatt) {
        mBluetoothGatt = gatt;
        characteristicList = new ArrayList<>();
        mConnectionState = STATE_DISCONNECTED;
    }

    public void setConnectionState(int newState){
        mConnectionState = newState;
    }

    public BluetoothGatt getGatt() { return mBluetoothGatt; }
    public boolean connect() { return mBluetoothGatt.connect(); }
    public void disconnect() { mBluetoothGatt.disconnect(); }
    public void close() { mBluetoothGatt.close(); }
    public int getConnectionState() { return mConnectionState; }
    public boolean writeCharacteristic(byte[] cmd) {
        Log.i(TAG, "BLeGatt.writeCharacteristic Called.");
        characteristicList.get(1).setValue(cmd);
        if (mBluetoothGatt.writeCharacteristic(characteristicList.get(1))) {
            return true;
        } else {
            return false;
        }
    }
}
