package com.example.ayou7995.demo_gw;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ayou7995 on 2016/2/26.
 */
public class BLEDatabaseAdapter {

    static String Tag = "Jonathan";

    BLEDatabaseHelper bleDatabaseHelper;

    public BLEDatabaseAdapter(Context context) {
        bleDatabaseHelper = new BLEDatabaseHelper(context);
    }

    public boolean checkAddressExist(String address) {
        SQLiteDatabase db = bleDatabaseHelper.getReadableDatabase();
        String[] columns = {BLEDatabaseHelper.ADDRESS};
        Cursor cursor = db.query(BLEDatabaseHelper.DEVICE_TABLE, columns, null, null, null, null, null);
        while (cursor.moveToNext()) {
            int index = cursor.getColumnIndex(BLEDatabaseHelper.ADDRESS);
            String add = cursor.getString(index);
            if (add.equals(address)) {
                Log.i(Tag, "Device already exists");
                return true;
            }
        }
        cursor.close();
        db.close();
        return false;
    }
    /*
    // CHECK GID EXISTS
    public boolean checkGidExist(int gid){
        boolean flag = false;
        SQLiteDatabase db = bleDatabaseHelper.getReadableDatabase();
        String[] columns = {BLEDatabaseHelper.GROUPID};
        Cursor cursor = db.query(BLEDatabaseHelper.DEVICE_TABLE, columns,null, null, null, null, null);
        while(cursor.moveToNext()){
            int index = cursor.getColumnIndex(BLEDatabaseHelper.GROUPID);
            int groupID = cursor.getInt(index);
            if(groupID == gid){
                flag = true;
                Log.i(Tag,"GroupId already exists");
            }
        }
        cursor.close();
        db.close();
        return  flag;
    }*/

    public long addDevice(BLEDevice bleDevice) {
        if (!checkAddressExist(bleDevice.getAddress())) {
            SQLiteDatabase db = bleDatabaseHelper.getWritableDatabase();
            ContentValues contentValues = new ContentValues();
            contentValues.put(BLEDatabaseHelper.DEVICENAME, bleDevice.getDeviceName());
            contentValues.put(BLEDatabaseHelper.ADDRESS, bleDevice.getAddress());
            contentValues.put(BLEDatabaseHelper.DEVICEID, bleDevice.getDeviceID());
            contentValues.put(BLEDatabaseHelper.GROUPID, bleDevice.getGroupID());
            contentValues.put(BLEDatabaseHelper.BRIGHT, bleDevice.getBright());
            contentValues.put(BLEDatabaseHelper.SPEED, bleDevice.getSpeed());
            long id = db.insert(BLEDatabaseHelper.DEVICE_TABLE, null, contentValues);
            db.close();
            //Log.i(LogTag,"BLEDevice Add Return "+String.valueOf(id));
            return id;
        }
        Log.i(Tag, "BLEDevice Add Unsuccessfully");
        return -1;
    }

    public DeviceRow getDeviceRow(String address) {
        SQLiteDatabase db = bleDatabaseHelper.getReadableDatabase();
        String[] columns = {BLEDatabaseHelper.ADDRESS, BLEDatabaseHelper.DEVICEID, BLEDatabaseHelper.GROUPID, BLEDatabaseHelper.BRIGHT};
        String[] selectArg = {address};
        Cursor cursor = db.query(BLEDatabaseHelper.DEVICE_TABLE, columns, BLEDatabaseHelper.ADDRESS + "=?", selectArg, null, null, null);
        DeviceRow dRow = new DeviceRow();
        while (cursor.moveToNext()) {
            int indexA = cursor.getColumnIndex(BLEDatabaseHelper.ADDRESS);
            int indexD = cursor.getColumnIndex(BLEDatabaseHelper.DEVICEID);
            int indexG = cursor.getColumnIndex(BLEDatabaseHelper.GROUPID);
            int indexB = cursor.getColumnIndex(BLEDatabaseHelper.BRIGHT);
            dRow.setAddress(cursor.getString(indexA));
            dRow.setDID(cursor.getString(indexD));
            dRow.setGID(cursor.getString(indexG));
            dRow.setBright(cursor.getInt(indexB));
        }
        cursor.close();
        db.close();
        return dRow;
    }

    // get address list with same groupID
    public List<String> getAddressList(String gid) {
        SQLiteDatabase db = bleDatabaseHelper.getReadableDatabase();
        String[] columns = {BLEDatabaseHelper.ADDRESS};
        String[] selectArg = {gid};
        Cursor cursor = db.query(BLEDatabaseHelper.DEVICE_TABLE, columns, BLEDatabaseHelper.GROUPID + "=?", selectArg, null, null, null);
        int indexA = cursor.getColumnIndex(BLEDatabaseHelper.ADDRESS);
        List<String> addressList = new ArrayList<>();
        while (cursor.moveToNext()) {
            addressList.add(cursor.getString(indexA));
        }
        cursor.close();
        db.close();
        return addressList;
    }

    public int getDID(String address) {
        SQLiteDatabase db = bleDatabaseHelper.getReadableDatabase();
        String[] columns = {BLEDatabaseHelper.DEVICEID};
        String[] selectArg = {address};
        Cursor cursor = db.query(BLEDatabaseHelper.DEVICE_TABLE, columns, BLEDatabaseHelper.ADDRESS + "=?", selectArg, null, null, null);
        int did = 0;
        while (cursor.moveToNext()) {
            int indexD = cursor.getColumnIndex(BLEDatabaseHelper.DEVICEID);
            did = cursor.getInt(indexD);
        }
        cursor.close();
        db.close();
        Log.w(Tag, "getDID return value " + String.valueOf(did));
        return did;
    }

    public int getGID(String address) {
        SQLiteDatabase db = bleDatabaseHelper.getReadableDatabase();
        String[] columns = {BLEDatabaseHelper.GROUPID};
        String[] selectArg = {address};
        Cursor cursor = db.query(BLEDatabaseHelper.DEVICE_TABLE, columns, BLEDatabaseHelper.ADDRESS + "=?", selectArg, null, null, null);
        int gid = 0;
        while (cursor.moveToNext()) {
            int indexG = cursor.getColumnIndex(BLEDatabaseHelper.GROUPID);
            gid = cursor.getInt(indexG);
        }
        cursor.close();
        db.close();
        Log.w(Tag, "getGID return value " + String.valueOf(gid));
        return gid;
    }


    // GET DEVICE TABLE SIZE
    public int getDeviceTableSize() {
        SQLiteDatabase db = bleDatabaseHelper.getReadableDatabase();
        int size = (int) DatabaseUtils.queryNumEntries(db, BLEDatabaseHelper.DEVICE_TABLE);
        return size;
    }

    // UPDATE BRIGHTNESS
    public int updateBright(String address, int newBright) {
        SQLiteDatabase db = bleDatabaseHelper.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(BLEDatabaseHelper.BRIGHT, newBright);
        String[] whereArgs = {address};
        return db.update(BLEDatabaseHelper.DEVICE_TABLE, contentValues, BLEDatabaseHelper.ADDRESS + "=?", whereArgs);
    }

    // UPDATE DEVICEID
    public int updateDID(String address, int did) {
        SQLiteDatabase db = bleDatabaseHelper.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(BLEDatabaseHelper.DEVICEID, did);
        String[] whereArgs = {address};
        return db.update(BLEDatabaseHelper.DEVICE_TABLE, contentValues, BLEDatabaseHelper.ADDRESS + "=?", whereArgs);
    }

    // UPDATE GROUPID
    public int updateGID(String address, int gid) {
        SQLiteDatabase db = bleDatabaseHelper.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(BLEDatabaseHelper.GROUPID, gid);
        String[] whereArgs = {address};
        return db.update(BLEDatabaseHelper.DEVICE_TABLE, contentValues, BLEDatabaseHelper.ADDRESS + "=?", whereArgs);
    }

    static public class BLEDatabaseHelper extends SQLiteOpenHelper {
        private static final String DATABASE_NAME = "BLEDatabase";
        private static final String DEVICE_TABLE = "DeviceTable";
        private static final String GROUP_TABLE = "GroupTable";
        private static final int DATABASE_VERSION = 1;
        private static final String UID = "_id";
        private static final String ADDRESS = "address";
        private static final String DEVICEID = "deviceID";
        private static final String DEVICENAME = "deviceName";
        private static final String GROUPID = "groupID";
        private static final String GROUPNAME = "groupName";
        private static final String GROUPNUM = "GroupNum";
        private static final String BRIGHT = "Bright";
        private static final String SPEED = "Speed";
        private static final String CREATE_TABLE_DEVICE = "CREATE TABLE " + DEVICE_TABLE + " ( "
                + UID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + ADDRESS + " VARCHAR(255), "
                + DEVICENAME + " VARCHAR(255), "
                + DEVICEID + " INTEGER NOT NULL DEFAULT 0, "
                + GROUPID + " INTEGER NOT NULL DEFAULT 0, "
                + BRIGHT + " INTEGER NOT NULL DEFAULT 0, "
                + SPEED + " INTEGER NOT NULL DEFAULT 0"
                + " );";
        private static final String CREATE_TABLE_GROUP = "CREATE TABLE " + GROUP_TABLE + " ( "
                + UID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + GROUPID + " INTEGER NOT NULL DEFAULT 0, "
                + GROUPNAME + " VARCHAR(255), "
                + GROUPNUM + " INTEGER NOT NULL DEFAULT 0"
                + " );";
        private static final String DROP_TABLE_DEVICE = "DROP TABLE IF EXISTS " + DEVICE_TABLE;
        private static final String DROP_TABLE_GROUP = "DROP TABLE IF EXISTS " + GROUP_TABLE;
        private Context context;

        public BLEDatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
            this.context = context;
            Log.i(Tag, "constructor called");
        }


        @Override
        public void onCreate(SQLiteDatabase db) {
            try {
                db.execSQL(CREATE_TABLE_DEVICE);
                db.execSQL(CREATE_TABLE_GROUP);
                Log.i(Tag, "oncreate called");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            try {
                db.execSQL(DROP_TABLE_DEVICE);
                db.execSQL(DROP_TABLE_GROUP);
                onCreate(db);
                Log.i(Tag, "onUpgrade called");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
