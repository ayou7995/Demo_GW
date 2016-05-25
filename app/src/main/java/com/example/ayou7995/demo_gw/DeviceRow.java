package com.example.ayou7995.demo_gw;

import android.nfc.Tag;
import android.util.Log;

/**
 * Created by ayou7995 on 2016/3/27.
 */
public class DeviceRow {
    private final static String TAG = "Jonathan";

    private boolean _isEnabled;
    private boolean _isDevice;
    private boolean _isOn;
    private int _rssi;
    private String _address;
    private String _DID;
    private String _GID;
    private int _bright;
    private String _connectState;

    public static final String CONNECT = "CONNECT";
    public static final String CONNECTING = "CONNECTING";
    public static final String DISCONNECT = "DISCONNECT";
    public static final String DISCONNECTING = "DISCONNECTING";

    public DeviceRow(){
        _isEnabled = false;
        _isDevice = true;
        _isOn = true;
        _rssi = -127;
        _address = "00:00:00:00:00";
        _DID = "0";
        _GID = "0";
        _bright = 0;
        _connectState = CONNECT;
    }
    public void setEnabled(boolean enable) { this._isEnabled = enable; }
    public void setIsDevice(boolean isDevice) { this._isDevice = isDevice; }
    public void setIsOn(boolean isOn) { this._isOn = isOn; }
    public void setRssi(int rssi) { this._rssi = rssi; }
    public void setAddress(String address) { this._address = address; }
    public void setDID(String DID) { this._DID = DID; }
    public void setGID(String GID) { this._GID = GID; }
    public void setBright(int bright) { this._bright = bright; }
    public void setConnectState(String state) { this._connectState = state; }
    public void update(DeviceRow row) {
        this._address = row._address;
        this._DID = row._DID;
        this._GID = row._GID;
        this._bright = row._bright;
    }
    public void printDebug() {
        Log.w(TAG, "==========================================");
        Log.w(TAG, "isEnabled: " + String.valueOf(_isEnabled));
        Log.w(TAG, "isDevice: " + String.valueOf(_isDevice));
        Log.w(TAG, "isOn: " + String.valueOf(_isOn));
        Log.w(TAG, "rssi: " + String.valueOf(_rssi));
        Log.w(TAG, "address: " + _address);
        Log.w(TAG, "did: " + _DID);
        Log.w(TAG, "gid: " + _GID);
        Log.w(TAG, "bright: " + String.valueOf(_bright));
        Log.w(TAG, "connectState: " + _connectState);
        Log.w(TAG, "==========================================");
    }
    public boolean getEnabled() { return _isEnabled; }
    public boolean getIsDevice() { return _isDevice; }
    public boolean getIsOn() { return _isOn; }
    public int getRssi() { return _rssi; }
    public String getAddress() { return _address; }
    public String getDID() { return _DID; }
    public String getGID() { return _GID; }
    public int getBright() { return _bright; }
    public String getConnectState() { return _connectState; }
}
