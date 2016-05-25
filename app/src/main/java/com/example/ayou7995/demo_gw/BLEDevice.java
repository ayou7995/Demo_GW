package com.example.ayou7995.demo_gw;

/**
 * Created by ayou7995 on 2016/2/1.
 */
public class BLEDevice {

    String _address;
    String _deviceName;
    int _deviceID;
    int _groupID;
    int _bright;
    int _speed;

    public BLEDevice() {
        this._address = "00:00:00:00:00";
        this._deviceName = "";
        this._deviceID = 0;
        this._groupID = 0;
        this._bright = 0;
        this._speed = 0;
    }

    void setAddress(String address) { this._address = address; }
    void setDeviceName(String name) { this._deviceName = name; }
    void setDID(int DID) { this._deviceID = DID; }
    void setGID(int GID) { this._groupID = GID; }
    void setBright(int bright) { this._bright= bright; }
    void setSpeed(int speed) { this._speed = speed; }

    String  getAddress() { return _address; }
    String  getDeviceName() { return _deviceName; }
    int getDeviceID() { return _deviceID; }
    int getGroupID() { return _groupID; }
    int getBright() { return _bright; }
    int getSpeed() { return _speed; }

}
