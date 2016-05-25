package com.example.ayou7995.demo_gw;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BLeService extends Service {
    private final static String TAG = "Jonathan";

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private HashMap<String, BLeGatt> mBluetoothGatt;

    public final static String ACTION_GATT_CONNECTED = "Gatt_Connected";
    public final static String ACTION_GATT_DISCONNECTED = "Gatt_Disconnected";
    public final static String ACTION_GATT_AVAILABLE = "Gatt_Available";
    public final static String ACTION_DATA_AVAILABLE = "Gatt_Data_Available";
    public final static String ACTION_TOAST = "Gatt_Toast";
    public final static String EXTRA_DATA1 = "Extra_Data1";
    // public final static String EXTRA_DATA2 = "Extra_Data2";
    public final static String EXTRA_ADDRESS = "Extra_Address";

    /*******************
     * Service Binding *
     *******************/
    public class LocalBinder extends Binder {
        BLeService getService() {
            return BLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "BLeService onBind");
        return new LocalBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        Log.d(TAG, "BLeService onUnBind");
        close();
        return super.onUnbind(intent);
    }

    /**
     * Initialize Service
     *
     * @return true if initialize successfully, false vice versa
     */
    public boolean initialize() {
        Log.i(TAG, "initialize=======================================");
        if (mBluetoothGatt == null) {
            Log.i(TAG, "Initialize map.");
            mBluetoothGatt = new HashMap<>();
        }
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }
        if (mBluetoothAdapter == null) {
            mBluetoothAdapter = mBluetoothManager.getAdapter();
            if (mBluetoothAdapter == null) {
                Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
                return false;
            }
            return true;
        }
        return true;
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String address = gatt.getDevice().getAddress();
            BLeGatt mGatt = mBluetoothGatt.get(address);
            Log.d(TAG, "In GattCallback: " + address);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "onConnectionStateChange Success.");
                if (newState == BluetoothProfile.STATE_CONNECTED && mGatt.getConnectionState() == BLeGatt.STATE_CONNECTING) {
                    Log.d(TAG, "Connection State: State Connected.");
                    mGatt.setConnectionState(BLeGatt.STATE_CONNECTED);
                    broadcastUpdate(ACTION_GATT_CONNECTED, address);
                    // Read Current Connection Rssi
                    // Log.i(TAG, "Read Remote Rssi.");
                    // gatt.readRemoteRssi();
                    // Attempts to discover services after successful connection.
                    if (gatt.discoverServices()) {
                        Log.i(TAG, "Discover Services Success.");
                    } else {
                        Log.e(TAG, "Discover Services Failed.");
                    }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED && mGatt.getConnectionState() != BLeGatt.STATE_DISCONNECTED) {
                    Log.d(TAG, "Connection State: State Disconnected.");
                    mGatt.setConnectionState(BLeGatt.STATE_DISCONNECTED);
                    broadcastUpdate(ACTION_GATT_DISCONNECTED, address);
                }
            } else {
                Log.e(TAG, "onConnectionStateChange Failed.");
                Log.d(TAG, "Connection State: State Disconnected.");
                broadcastUpdate(ACTION_TOAST, "Connection failed.");
                mGatt.setConnectionState(BLeGatt.STATE_DISCONNECTED);
                broadcastUpdate(ACTION_GATT_DISCONNECTED, address);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            String address = gatt.getDevice().getAddress();
            BLeGatt mGatt = mBluetoothGatt.get(address);
            List<BluetoothGattService> serviceList;
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "onServicesDiscovered Success.");
                Log.d(TAG, "===========================================================");
                serviceList = gatt.getServices();
                for (BluetoothGattService sv : serviceList) {
                    Log.d(TAG, "Service UUID: " + sv.getUuid());

                    mGatt.characteristicList = sv.getCharacteristics();
                    for (BluetoothGattCharacteristic ch : mGatt.characteristicList) {
                        Log.d(TAG, "Characteristic UUID: " + ch.getUuid());
                    }

                    if (BLeGatt.SERVICE_UUID.equals(sv.getUuid())) {
                        if (BLeGatt.CHAR_NOTIFY_UUID.equals(mGatt.characteristicList.get(0).getUuid())) {
                            Log.i(TAG, "NOTIFY: " + mGatt.characteristicList.get(0).getUuid());
                            gatt.setCharacteristicNotification(mGatt.characteristicList.get(0), true);
                            BluetoothGattDescriptor descriptor = mGatt.characteristicList.get(0).getDescriptor(
                                    BLeGatt.DESCRIPTOR_UUID);
                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            gatt.writeDescriptor(descriptor);
                        }
                        if (BLeGatt.CHAR_WRITE_UUID.equals(mGatt.characteristicList.get(1).getUuid())) {
                            Log.i(TAG, "WRITE: " + mGatt.characteristicList.get(1).getUuid());
                            gatt.setCharacteristicNotification(mGatt.characteristicList.get(1), true);
                        }
                    }
                }
                Log.d(TAG, "===========================================================");
            } else {
                Log.e(TAG, "onServicesDiscovered Failed.");
            }
            super.onServicesDiscovered(gatt, status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            Log.i(TAG, "onCharacteristicChanged");
            byte[] raw_data = characteristic.getValue();
            String address = gatt.getDevice().getAddress();
            Log.i(TAG, MainActivity.bytesToHex(raw_data));
            broadcastUpdate(ACTION_DATA_AVAILABLE, address, characteristic);
            super.onCharacteristicChanged(gatt, characteristic);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt,
                                      BluetoothGattDescriptor descriptor, int status) {
            Log.i(TAG, "onDescriptorWrite");
            Log.d(TAG, "Connection State: State Available.");
            String address = gatt.getDevice().getAddress();
            BLeGatt mGatt = mBluetoothGatt.get(address);
            mGatt.setConnectionState(BLeGatt.STATE_AVAILABLE);
            broadcastUpdate(ACTION_GATT_AVAILABLE, address);
            super.onDescriptorWrite(gatt, descriptor, status);
        }
    };

    /**
     * Broadcast to main activity
     *
     * @param action ACTION_GATT_CONNECTED
     *               ACTION_GATT_DISCONNECTED
     */

    private void broadcastUpdate(final String action, final String address) {
        Log.i(TAG, "broadcastUpdate: " + action + " address : " + address);
        final Intent intent = new Intent(action);
        intent.putExtra(EXTRA_ADDRESS, address);
        sendBroadcast(intent);
    }


    /**
     * Broadcast to main activity
     *
     * @param action         ACTION_DATA_AVAILABLE
     * @param characteristic GATT services' characteristic
     */

    private void broadcastUpdate(final String action,
                                 final String address,
                                 final BluetoothGattCharacteristic characteristic) {
        Log.i(TAG, "broadcastUpdate: " + action + " address : " + address);
        final Intent intent = new Intent(action);
        byte[] value = characteristic.getValue();
        intent.putExtra(EXTRA_DATA1, value);
        intent.putExtra(EXTRA_ADDRESS, address);
        sendBroadcast(intent);
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     * @return Return true if the connection is initiated successfully. The connection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public boolean connect(final String address) {
        Log.i(TAG, "BLeService.Connect Called.");
        // Check validity.
        if (mBluetoothAdapter == null) {
            Log.w(TAG, "BluetoothAdapter not initialized.");
            return false;
        } else if (address == null) {
            Log.w(TAG, "Unspecified address.");
            return false;
        }
        // Previously connected device.  Try to reconnect.
        if (mBluetoothGatt.containsKey(address) && mBluetoothGatt.get(address) != null) {
            Log.i(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            BLeGatt mGatt = mBluetoothGatt.get(address);
            if (mGatt.connect()) {
                Log.d(TAG, "Connection State: State Connecting.");
                mGatt.setConnectionState(BLeGatt.STATE_CONNECTING);
                return true;
            } else {
                return false;
            }
        }
        // No previous connected device.
        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found. Unable to connect.");
            return false;
        }
        BluetoothGatt gatt = device.connectGatt(this, false, mGattCallback);
        BLeGatt mGatt = new BLeGatt(gatt);
        mBluetoothGatt.put(address, mGatt);
        Log.i(TAG, "Trying to create a new connection.");
        Log.d(TAG, "Connection State: State Connecting.");
        mGatt.setConnectionState(BLeGatt.STATE_CONNECTING);
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect(String address) {
        BLeGatt mGatt = mBluetoothGatt.get(address);
        Log.i(TAG, "BLeService.Disconnect Called.");
        if (mBluetoothAdapter == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
        } else if (mBluetoothGatt == null) {
            Log.w(TAG, "No working Gatt connection.");
        } else {
            mGatt.disconnect();
        }
    }

    public void disconnectAll() {
        Log.i(TAG, "BLeService.DisconnectAll Called.");
        if (mBluetoothAdapter == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        /*for(Iterator entries = mBluetoothGatt.entrySet().iterator() ; entries.hasNext();){
            Map.Entry entry = (Map.Entry) entries.next();
            entry.getValue().disonnect();
        }*/
        for (Map.Entry<String, BLeGatt> entry : mBluetoothGatt.entrySet()) {
            entry.getValue().disconnect();
        }
    }

    public int getConnectionState(String address) {
        BLeGatt mGatt = mBluetoothGatt.get(address);
        return mGatt.getConnectionState();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        Log.i(TAG, "BLeService.Close Called.");
        if (mBluetoothAdapter == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        for (Map.Entry<String, BLeGatt> entry : mBluetoothGatt.entrySet()) {
            entry.getValue().close();
        }
        mBluetoothGatt.clear();
    }

    /**
     * Request a write on a given {@code BluetoothGattCharacteristic}. The write result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicwrite(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback and through the {@code BluetoothGattCallback#onCharacteristicwrite(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic)}.
     *
     * @param cmd The byte array to write in the given characteristic.
     */
    public void writeCharacteristic(String address, byte[] cmd) {
        Log.i(TAG, "BLeService.writeCharacteristic Called.");
        if (mBluetoothAdapter == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        BLeGatt mGatt = mBluetoothGatt.get(address);
        if (mGatt.writeCharacteristic(cmd))
            Log.i(TAG, "Write Characteristic Success");
        else
            Log.i(TAG, "Write Characteristic Fail");
    }
}
