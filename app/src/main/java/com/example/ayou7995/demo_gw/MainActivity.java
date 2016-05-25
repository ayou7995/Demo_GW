package com.example.ayou7995.demo_gw;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.adw.library.widgets.discreteseekbar.DiscreteSeekBar;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    static final String Tag = "Jonathan";
    // static final int groupNum = 5;
    // static final int deviceNum = 16;
    // String[] groupList = new String[]{"0", "1", "2", "3", "4", "5"};
    // String[] deviceList = new String[]{"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"};
    private BLEDatabaseAdapter bleDatabaseAdapter;

    // BLE
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;

    private boolean bleEnabled;
    private AlertDialog.Builder bleDialog;
    private Handler mHandler;
    private BLeService bleService;
    private BluetoothLeScanner mLEScanner;
    private Set<String> foundDevicesAddress;
    private List<String> availableDeviceAddress;

    private static final long SCAN_PERIOD = 30000;
    private static final String DEVICE_NAME = "LINK8105";
    // private static final int REQUEST_ENABLE_BT = 1;

    private byte[] mCommand;
    private static final byte[] CMD_GET_INFO = new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x81};
    // private static final byte[] CMD_SET_ID = new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x54, (byte) 0x75, (byte) 0x75, (byte) 0x75, (byte) 0x00, (byte) 0x00};
    private static final byte[] CMD_DEV_SWITCH = new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x05, (byte) 0x47, (byte) 0x57, (byte) 0x00};
    private static final byte[] CMD_GRP_SWITCH = new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x06, (byte) 0x47, (byte) 0x57, (byte) 0x00};

    ListView listview;
    List<DeviceRow> rowList;
    private MyAdapter mAdapter;

    Button button_scan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        button_scan = (Button) findViewById(R.id.button_scan);
        listview = (ListView) findViewById(R.id.listview_devicelist);

        // Bluetooth basis related.
        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        bleDialog = new AlertDialog.Builder(MainActivity.this)
                .setTitle("Bluetooth Requirement")
                .setMessage("Please turn on bluetooth to run this app.")
                .setPositiveButton("Turn On", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (mBluetoothAdapter.enable()) {
                            Toast.makeText(getApplicationContext(), "Bluetooth turning on...", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getApplicationContext(), "Failed turning on bluetooth.", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Maybe Later", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
        if (!mBluetoothAdapter.isEnabled()) {
            bleEnabled = false;
            bleDialog.show();
        } else {
            bleEnabled = true;
            mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
        }

        // BLeService related.
        Intent intent = new Intent(this, BLeService.class);
        bindService(intent, conn, Context.BIND_AUTO_CREATE);
        IntentFilter mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(BLeService.ACTION_GATT_CONNECTED);
        mIntentFilter.addAction(BLeService.ACTION_GATT_DISCONNECTED);
        mIntentFilter.addAction(BLeService.ACTION_GATT_AVAILABLE);
        mIntentFilter.addAction(BLeService.ACTION_DATA_AVAILABLE);
        mIntentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mReceiver, mIntentFilter);
        foundDevicesAddress = new HashSet<>();
        availableDeviceAddress = new ArrayList<>();

        rowList = new ArrayList<>();
        mAdapter = new MyAdapter(this, rowList);
        listview.setAdapter(mAdapter);
        bleDatabaseAdapter = new BLEDatabaseAdapter(this);
        mHandler = new Handler();
    }

    @Override
    protected void onResume() {
        // listview.setAdapter(mAdapter);
        // mAdapter.notifyDataSetChanged();
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        unbindService(conn);
        unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    /**
     * Trigger by startActivityForResult
     */
    /*
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        android.util.Log.i(Tag, "onActivityResult: " + requestCode);
        if (resultCode == Activity.RESULT_OK) {
            Log.i(Tag, "Bluetooth turned on.");
            bleEnabled = true;
        } else {    // resultCode == Activity.RESULT_CANCELED
            Log.w(Tag, "Bluetooth NOT turned on");
            bleEnabled = false;
            // this.finishAffinity();  // Quit from current activity
        }
    }
    */

    /**************
     * UI related *
     **************/
    public void scan(View view) {
        Log.i(Tag, "Scan button pressed");
        scanLeDevice(bleEnabled);
    }

    /*******************
     * Service related *
     *******************/
    private final ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i(Tag, "ServiceConnection onServiceDisconnected Called.");
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i(Tag, "ServiceConnection onServiceConnected Called.");
            bleService = ((BLeService.LocalBinder) service).getService();
        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.i(Tag, "broadcastReceiver: " + action);
            if (BLeService.ACTION_GATT_AVAILABLE.equals(action)) {
                // GATT connected
                String address = intent.getStringExtra(BLeService.EXTRA_ADDRESS);
                int index = availableDeviceAddress.indexOf(address);
                rowList.get(index).setEnabled(true);
                rowList.get(index).setConnectState(DeviceRow.DISCONNECT);
                mAdapter.notifyDataSetChanged();
            } else if (BLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                // GATT disconnected
                String address = intent.getStringExtra(BLeService.EXTRA_ADDRESS);
                int index = availableDeviceAddress.indexOf(address);
                if (index != -1) {
                    rowList.get(index).setEnabled(false);
                    rowList.get(index).setConnectState(DeviceRow.CONNECT);
                    mAdapter.notifyDataSetChanged();
                }
            } else if (BLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                // Display available GATT service characteristic
                byte[] returnVal = intent.getByteArrayExtra(BLeService.EXTRA_DATA1);
                // int rssi = intent.getIntExtra(BLeService.EXTRA_DATA2, -127);
                String address = intent.getStringExtra(BLeService.EXTRA_ADDRESS);
                if (mCommand == CMD_GET_INFO) {
                    Log.i(Tag, "Received command get info return value: " + bytesToHex(returnVal));
                    if (returnVal != null) {
                        Log.i(Tag, "Update DID and GID");
                        int did = returnVal[4];
                        int gid = returnVal[5];
                        bleDatabaseAdapter.updateDID(address, did);
                        bleDatabaseAdapter.updateGID(address, gid);
                        int index = availableDeviceAddress.indexOf(address);
                        rowList.get(index).setDID(String.valueOf(did));
                        rowList.get(index).setGID(String.valueOf(gid));
                        // rowList.get(index).setRssi(rssi);
                        mAdapter.notifyDataSetChanged();
                    } else {
                        Log.e(Tag, "Got NO return value from cmd get info.");
                    }
                }
            } else if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        Log.i(Tag, "BluetoothAdapter State Off.");
                        bleEnabled = false;
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.i(Tag, "BluetoothAdapter State Turning Off.");
                        mHandler.removeCallbacksAndMessages(null);
                        bleService.close();
                        foundDevicesAddress.clear();
                        availableDeviceAddress.clear();
                        if (mAdapter != null) {
                            rowList.clear();
                            mAdapter.notifyDataSetChanged();
                        }
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.i(Tag, "BluetoothAdapter State On.");
                        bleEnabled = true;
                        mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.i(Tag, "BluetoothAdapter State Turning On.");
                        break;
                }
            } else if (action.equals(BLeService.ACTION_TOAST)) {
                String msg = intent.getStringExtra(BLeService.EXTRA_ADDRESS);
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
            }
        }
    };

    /***************
     * BLe related *
     ***************/
    private void scanLeDevice(boolean enable) {
        if (!bleService.initialize()) {
            Log.e(Tag, "bleService unable to initialize");
        }
        Log.i(Tag, "scanLeDevice Called.");
        if (enable) {
            Log.i(Tag, "Start Scanning...");
            Toast.makeText(getApplicationContext(), "Start scanning...", Toast.LENGTH_LONG).show();
            mHandler.removeCallbacksAndMessages(null);
            bleService.disconnectAll();
            foundDevicesAddress.clear();
            availableDeviceAddress.clear();
            if (mAdapter != null) {
                rowList.clear();
                mAdapter.notifyDataSetChanged();
            }
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mLEScanner.stopScan(mScanCallback);
                    Log.i(Tag, "Scan timed out!");
                    if (foundDevicesAddress.isEmpty()) {
                        Log.w(Tag, "Found fucking nothing!");
                        Toast.makeText(getApplicationContext(), "Found no devices.", Toast.LENGTH_SHORT).show();
                    } else {
                        // availableDeviceAddress.addAll(foundDevicesAddress);
                        Toast.makeText(getApplicationContext(), "Found " + String.valueOf(foundDevicesAddress.size()) + " device(s)", Toast.LENGTH_LONG).show();
                        /*Log.i(Tag, String.valueOf(foundDevicesAddress.size()) + " address(es) waiting for connecting.");
                        Toast.makeText(getApplicationContext(), "Found " + String.valueOf(foundDevicesAddress.size()) + " device(s). Fetching data...", Toast.LENGTH_LONG).show();
                        class ChecknConnect implements Runnable {
                            String address;

                            ChecknConnect(String addr) {
                                address = addr;
                            }

                            public void run() {
                                if (bleService.mConnectionState == BLeService.STATE_DISCONNECTED) {
                                    Log.w(Tag, address + " NOW connecting...");
                                    bleService.connect(address);
                                    mHandler.postDelayed(new SendCommand(CMD_GET_INFO), 500);
                                } else {
                                    Log.w(Tag, address + " waiting for connecting...");
                                    mHandler.postDelayed(new ChecknConnect(address), 500);
                                }
                            }
                        }
                        for (String addr : foundDevicesAddress) {
                            mHandler.post(new ChecknConnect(addr));
                        }*/
                    }
                }
            }, SCAN_PERIOD);

            List<ScanFilter> filters = new ArrayList<>();
            if (mBluetoothAdapter.isOffloadedFilteringSupported()) {
                ScanFilter filter = new ScanFilter.Builder().setDeviceName(DEVICE_NAME).build();
                filters.add(filter);
            }
            ScanSettings settings = new ScanSettings.Builder().build();
            mLEScanner.startScan(filters, settings, mScanCallback);
        } else {
            Log.w(Tag, "BLe Function NOT Enabled.");
            bleDialog.show();
        }
    }

    private final ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.i(Tag, "ScanCallback onScanResult Called.");

            if (callbackType == ScanSettings.CALLBACK_TYPE_ALL_MATCHES) {
                Log.i(Tag, "Callback type all matches");
            } else if (callbackType == ScanSettings.CALLBACK_TYPE_FIRST_MATCH) {
                Log.i(Tag, "Callback type first match");
            } else {
                Log.i(Tag, "Callback type match lost");
            }
            Log.i(Tag, "Raw result: " + result.toString());
            BluetoothDevice device = result.getDevice();
            if (mBluetoothAdapter.isOffloadedFilteringSupported() || DEVICE_NAME.equals(device.getName())) {
                if (foundDevicesAddress.contains(device.getAddress())) {
                    Log.i(Tag, "Found duplicated related device:" + device.getAddress());
                } else {
                    Log.i(Tag, "Found one related device: " + device.getAddress());
                    foundDevicesAddress.add(device.getAddress());
                    availableDeviceAddress.add(device.getAddress());
                    // Add to database once found
                    BLEDevice ble = new BLEDevice();
                    ble.setAddress(device.getAddress());
                    if (bleDatabaseAdapter.addDevice(ble) != -1) {
                        Log.i(Tag, "new device found, adding " + device.getAddress() + " to database");
                        Log.i(Tag, "current database size = " +
                                String.valueOf(bleDatabaseAdapter.getDeviceTableSize()));
                    }

                    // Add to list
                    DeviceRow row = bleDatabaseAdapter.getDeviceRow(device.getAddress());
                    int rssi = result.getRssi();
                    row.setDID(String.valueOf(bleDatabaseAdapter.getDID(device.getAddress())));
                    row.setGID(String.valueOf(bleDatabaseAdapter.getGID(device.getAddress())));
                    row.setRssi(rssi);
                    rowList.add(row);
                    mAdapter.notifyDataSetChanged();
                }
            } else {
                Log.i(Tag, "Found one NOT related device: " + device.getName());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.i(Tag, "ScanCallback onScanFailed Called.");
            if (errorCode == ScanCallback.SCAN_FAILED_INTERNAL_ERROR) {
                Log.e(Tag, "Scan failed internal error.");
            } else if (errorCode == ScanCallback.SCAN_FAILED_ALREADY_STARTED) {
                Log.e(Tag, "Already started. Scan Failed.");
            } else if (errorCode == ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED) {
                Log.e(Tag, "Scan failed. Application registration failed.");
            } else {    // errorCode == ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED
                Log.e(Tag, "Scan failed feature unsupported.");
            }
        }
    };

    // Send Command Runnable
    class SendCommand implements Runnable {
        String address;
        byte[] command;

        SendCommand(String addr, byte[] cmd) {
            address = addr;
            command = cmd;
        }

        public void run() {
            if (bleService.getConnectionState(address) == BLeGatt.STATE_AVAILABLE) {
                Log.w(Tag, "NOW sending command... " + bytesToHex(command));
                mCommand = command;
                bleService.writeCharacteristic(address, command);
            } else if (bleService.getConnectionState(address) == BLeGatt.STATE_DISCONNECTED) {
                Log.w(Tag, "Connection failed. Drop pending command. " + bytesToHex(command));
            } else {       // STATE_CONNECTING or STATE_CONNECTED (not yet available)
                Log.w(Tag, "Waiting for connection complete. Command pending... " + bytesToHex(command));
                mHandler.postDelayed(new SendCommand(address, command), 500);
            }
        }
    }

    // list adapter
    public class MyAdapter extends BaseAdapter {
        private BLEDatabaseAdapter bleDatabaseAdapter;
        private LayoutInflater mInflater;
        private List<DeviceRow> mlist;

        public MyAdapter(Context context, List<DeviceRow> list) {
            bleDatabaseAdapter = new BLEDatabaseAdapter(context);
            mInflater = LayoutInflater.from(context);
            mlist = list;
        }

        @Override
        public boolean areAllItemsEnabled() {
            for (DeviceRow row : mlist)
                if (!row.getEnabled())
                    return false;
            return true;
        }

        @Override
        public boolean isEnabled(int position) {
            return mlist.get(position).getEnabled();
        }

        @Override
        public int getCount() {
            return mlist.size();
        }

        @Override
        public Object getItem(int position) {
            return mlist.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            Log.i(Tag, "MyAdapter.getView Called. Position: " + String.valueOf(position));
            ViewHolder viewHolder;
            // ArrayAdapter<String> dAdapter, gAdapter;
            ConnectBOnClickListener connectBListener;
            OnBOnClickListener onBListener;
            OffBOnClickListener offBListener;
            //DidOnItemSelectedListener deviceIDListener;
            //GidOnItemSelectedListener groupIDListener;
            OnCheckedChangeListener radioGroupListener;
            OnProgressChangeListener brightListener;

            // update latest data to mlist
            final DeviceRow currentRow = (DeviceRow) getItem(position);
            final DeviceRow dbRow = bleDatabaseAdapter.getDeviceRow(currentRow.getAddress());
            currentRow.update(dbRow);

            // currentRow.printDebug();

            if (convertView == null) {
                Log.d(Tag, "ConvertView == null");
                convertView = mInflater.inflate(R.layout.device_list_row, null);
                viewHolder = new ViewHolder();
                viewHolder.address = (TextView) convertView.findViewById(R.id.textview_address);
                viewHolder.radioGroup = (RadioGroup) convertView.findViewById(R.id.radio_group);
                viewHolder.device_radio_btn = (RadioButton) convertView.findViewById(R.id.radioButton_device);
                viewHolder.group_radio_btn = (RadioButton) convertView.findViewById(R.id.radioButton_group);
                viewHolder.deviceID = (TextView) convertView.findViewById(R.id.textview_did);
                viewHolder.groupID = (TextView) convertView.findViewById(R.id.textview_gid);
                viewHolder.bright = (DiscreteSeekBar) convertView.findViewById(R.id.seekbar_bright);
                viewHolder.connectB = (Button) convertView.findViewById(R.id.button_connect);
                viewHolder.onB = (Button) convertView.findViewById(R.id.button_on);
                viewHolder.offB = (Button) convertView.findViewById(R.id.button_off);
                viewHolder.rssi = (TextView) convertView.findViewById(R.id.rssi);

                // connect button
                connectBListener = new ConnectBOnClickListener();
                viewHolder.connectB.setOnClickListener(connectBListener);

                // did
                /*dAdapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_spinner_item, deviceList);
                 *dAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                 *viewHolder.deviceID.setAdapter(dAdapter);
                 *deviceIDListener = new DidOnItemSelectedListener();
                 *viewHolder.deviceID.setOnItemSelectedListener(deviceIDListener);
                */
                // gid
                /*gAdapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_spinner_item, groupList);
                 *gAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                 *viewHolder.groupID.setAdapter(gAdapter);
                 *groupIDListener = new GidOnItemSelectedListener();
                 *viewHolder.groupID.setOnItemSelectedListener(groupIDListener);
                */
                // radio group
                radioGroupListener = new OnCheckedChangeListener();
                viewHolder.radioGroup.setOnCheckedChangeListener(radioGroupListener);

                // discrete seekbar
                brightListener = new OnProgressChangeListener();
                viewHolder.bright.setOnProgressChangeListener(brightListener);

                // on button
                onBListener = new OnBOnClickListener();
                viewHolder.onB.setOnClickListener(onBListener);

                // off button
                offBListener = new OffBOnClickListener();
                viewHolder.offB.setOnClickListener(offBListener);

                convertView.setTag(viewHolder);
                //convertView.setTag(viewHolder.deviceID.getId() * 7, dAdapter);
                //convertView.setTag(viewHolder.groupID.getId() * 7, gAdapter);
                convertView.setTag(viewHolder.connectB.getId(), connectBListener);
                //convertView.setTag(viewHolder.deviceID.getId(), deviceIDListener);
                //convertView.setTag(viewHolder.groupID.getId(), groupIDListener);
                convertView.setTag(viewHolder.radioGroup.getId(), radioGroupListener);
                convertView.setTag(viewHolder.bright.getId(), brightListener);
                convertView.setTag(viewHolder.onB.getId(), onBListener);
                convertView.setTag(viewHolder.offB.getId(), offBListener);
            } else {
                Log.d(Tag, "ConvertView != null");
                viewHolder = (ViewHolder) convertView.getTag();
                // dAdapter = (ArrayAdapter<String>) convertView.getTag(viewHolder.deviceID.getId() * 7);
                // gAdapter = (ArrayAdapter<String>) convertView.getTag(viewHolder.groupID.getId() * 7);
                connectBListener = (ConnectBOnClickListener) convertView.getTag(viewHolder.connectB.getId());
                // deviceIDListener = (DidOnItemSelectedListener) convertView.getTag(viewHolder.deviceID.getId());
                // groupIDListener = (GidOnItemSelectedListener) convertView.getTag(viewHolder.groupID.getId());
                radioGroupListener = (OnCheckedChangeListener) convertView.getTag(viewHolder.radioGroup.getId());
                brightListener = (OnProgressChangeListener) convertView.getTag(viewHolder.bright.getId());
                onBListener = (OnBOnClickListener) convertView.getTag(viewHolder.onB.getId());
                offBListener = (OffBOnClickListener) convertView.getTag(viewHolder.offB.getId());
            }

            // set position to listeners
            connectBListener.setPosition(position);
            //deviceIDListener.setPosition(position);
            //groupIDListener.setPosition(position);
            radioGroupListener.setPosition(position);
            brightListener.setPosition(position);
            onBListener.setPosition(position);
            offBListener.setPosition(position);

            // set UIs to enabled or disabled
            if (currentRow.getEnabled()) {
                viewHolder.radioGroup.setEnabled(true);
                viewHolder.device_radio_btn.setEnabled(true);
                viewHolder.group_radio_btn.setEnabled(true);
                viewHolder.deviceID.setEnabled(true);
                viewHolder.groupID.setEnabled(true);
                viewHolder.bright.setEnabled(true);
                viewHolder.onB.setEnabled(true);
                viewHolder.offB.setEnabled(true);
                viewHolder.rssi.setEnabled(true);
            } else {
                viewHolder.radioGroup.setEnabled(false);
                viewHolder.device_radio_btn.setEnabled(false);
                viewHolder.group_radio_btn.setEnabled(false);
                viewHolder.deviceID.setEnabled(false);
                viewHolder.groupID.setEnabled(false);
                viewHolder.bright.setEnabled(false);
                viewHolder.onB.setEnabled(false);
                viewHolder.offB.setEnabled(false);
                viewHolder.rssi.setEnabled(false);
            }
            // set connect button to enabled or disabled
            if (currentRow.getConnectState().equals(DeviceRow.CONNECT) || currentRow.getConnectState().equals(DeviceRow.DISCONNECT)) {
                viewHolder.connectB.setEnabled(true);
            } else {
                viewHolder.connectB.setEnabled(false);
            }
            /*
                update UI components
             */
            // address
            viewHolder.address.setText(currentRow.getAddress());
            // did
            viewHolder.deviceID.setText(currentRow.getDID());
            //int dAdapterPosition = dAdapter.getPosition(currentRow.getDID());
            //viewHolder.deviceID.setSelection(dAdapterPosition);
            // gid
            viewHolder.groupID.setText(currentRow.getGID());
            //int gAdapterPosition = gAdapter.getPosition(currentRow.getGID());
            //viewHolder.groupID.setSelection(gAdapterPosition);
            // radio group
            if (currentRow.getIsDevice()) {
                viewHolder.radioGroup.check(R.id.radioButton_device);
            } else {
                viewHolder.radioGroup.check(R.id.radioButton_group);
            }
            // bright
            if (currentRow.getIsOn()) {
                viewHolder.bright.setProgress(currentRow.getBright());
            } else {
                viewHolder.bright.setProgress(0);
            }
            viewHolder.bright.setNumericTransformer(multiplyTransformer);
            // connect button
            viewHolder.connectB.setText(currentRow.getConnectState());
            // rssi
            viewHolder.rssi.setText(String.valueOf(currentRow.getRssi()));

            return convertView;
        }

        private void sendBrightCmd(DeviceRow row, int bright) {
            if (row.getIsDevice()) {
                if (bright != 0) {
                    bleDatabaseAdapter.updateBright(row.getAddress(), bright);
                }
                byte byte_did = intToByte(Integer.valueOf(row.getDID()));
                byte byte_bright = intToByte(bright * 255 / 20);
                byte[] cmd = CMD_DEV_SWITCH;
                cmd[0] = byte_did;
                cmd[5] = byte_bright;
                // {(byte) 0xDID, (byte) 0x00, (byte) 0x05, (byte) 0x47, (byte) 0x57, (byte) 0xBRIGHT}
                mHandler.post(new SendCommand(row.getAddress(), cmd));
            } else {
                if (bright != 0) {
                    List<String> addressList = bleDatabaseAdapter.getAddressList(row.getGID());
                    for (String address : addressList) {
                        bleDatabaseAdapter.updateBright(address, bright);
                    }
                }
                byte byte_gid = intToByte(Integer.valueOf(row.getGID()));
                byte byte_bright = intToByte(bright * 255 / 20);
                byte[] cmd = CMD_GRP_SWITCH;
                cmd[1] = byte_gid;
                cmd[5] = byte_bright;
                // {(byte) 0x00, (byte) 0xGID, (byte) 0x05, (byte) 0x47, (byte) 0x57, (byte) 0xBRIGHT}
                mHandler.post(new SendCommand(row.getAddress(), cmd));
            }
        }

        /*
        private void sendSettingCmd(int did, int gid) {
            byte byte_did = intToByte(did);
            byte byte_gid = intToByte(gid);
            byte[] cmd = CMD_SET_ID;
            cmd[7] = byte_did;
            cmd[8] = byte_gid;
            // {(byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x54, (byte) 0x75, (byte) 0x75, (byte) 0x75, (byte) 0xDID, (byte) 0xGID}
            mHandler.post(new SendCommand(cmd));
        }
        */

        private class ViewHolder {
            TextView address;
            TextView deviceID;
            TextView groupID;
            Button connectB;
            Button onB;
            Button offB;
            RadioGroup radioGroup;
            RadioButton device_radio_btn;
            RadioButton group_radio_btn;
            DiscreteSeekBar bright;
            TextView rssi;
        }

        private class ConnectBOnClickListener implements View.OnClickListener {
            DeviceRow currentRow;

            public void setPosition(int position) {
                this.currentRow = (DeviceRow) getItem(position);
            }

            @Override
            public void onClick(View v) {
                String address = currentRow.getAddress();
                Log.i(Tag, "Connect Button Clicked. Position: " + address);
                Log.w(Tag, "View ID: " + v.getId());
                if (currentRow.getConnectState().equals(DeviceRow.CONNECT)) {
                    currentRow.setConnectState(DeviceRow.CONNECTING);
                    //bleService.disconnect(address);
                    bleService.connect(address);
                    Toast.makeText(getApplicationContext(), "Start connecting...", Toast.LENGTH_LONG).show();
                    mHandler.postDelayed(new SendCommand(address, CMD_GET_INFO), 500); /***********/
                } else {
                    currentRow.setConnectState(DeviceRow.DISCONNECTING);
                    bleService.disconnect(address);
                }
                notifyDataSetChanged();
                // currentRow.printDebug();
            }
        }

        /*
        private class DidOnItemSelectedListener implements AdapterView.OnItemSelectedListener {
            DeviceRow currentRow;

            public void setPosition(int position) {
                this.currentRow = (DeviceRow) getItem(position);
            }

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!currentRow.getConnectState().equals(DeviceRow.DISCONNECT)) {
                    return;
                }
                Log.i(Tag, "DID Spinner onItemSelected. Position: " + position);
                int did = Integer.valueOf(deviceList[position]);
                currentRow.setDID(String.valueOf(did));
                bleDatabaseAdapter.updateDID(currentRow.getAddress(), did);
                sendSettingCmd(did, Integer.valueOf(currentRow.getGID()));
                currentRow.printDebug();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        }

        private class GidOnItemSelectedListener implements AdapterView.OnItemSelectedListener {
            DeviceRow currentRow;

            public void setPosition(int position) {
                this.currentRow = (DeviceRow) getItem(position);
            }

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!currentRow.getConnectState().equals(DeviceRow.DISCONNECT)) {
                    return;
                }
                Log.i(Tag, "GID Spinner onItemSelected. Position: " + position);
                int gid = Integer.valueOf(groupList[position]);
                currentRow.setGID(String.valueOf(gid));
                bleDatabaseAdapter.updateGID(currentRow.getAddress(), gid);
                sendSettingCmd(Integer.valueOf(currentRow.getDID()), gid);
                currentRow.printDebug();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        }
        */
        private class OnCheckedChangeListener implements RadioGroup.OnCheckedChangeListener {
            DeviceRow currentRow;

            public void setPosition(int position) {
                this.currentRow = (DeviceRow) getItem(position);
            }

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                Log.i(Tag, "RadioGroup onCheckedChange. Position: " + currentRow.getAddress());
                switch (checkedId) {
                    case R.id.radioButton_device:
                        currentRow.setIsDevice(true);
                        break;
                    case R.id.radioButton_group:
                        currentRow.setIsDevice(false);
                        break;
                }
                // currentRow.printDebug();
            }
        }

        private class OnProgressChangeListener implements DiscreteSeekBar.OnProgressChangeListener {
            DeviceRow currentRow;

            public void setPosition(int position) {
                this.currentRow = (DeviceRow) getItem(position);
            }

            @Override
            public void onProgressChanged(DiscreteSeekBar seekBar, int value, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(DiscreteSeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(DiscreteSeekBar seekBar) {
                Log.i(Tag, "SeekBar onStopTrackingTouch. Position: " + currentRow.getAddress());
                int bright = seekBar.getProgress();
                if (bright != 0) {
                    currentRow.setBright(bright);
                }
                sendBrightCmd(currentRow, bright);
                // currentRow.printDebug();
            }
        }

        private class OnBOnClickListener implements View.OnClickListener {
            DeviceRow currentRow;

            public void setPosition(int position) {
                this.currentRow = (DeviceRow) getItem(position);
            }

            @Override
            public void onClick(View v) {
                Log.i(Tag, "On Button onClick. Position: " + currentRow.getAddress());
                currentRow.setIsOn(true);
                sendBrightCmd(currentRow, currentRow.getBright());
                notifyDataSetChanged();
                // currentRow.printDebug();
            }
        }

        private class OffBOnClickListener implements View.OnClickListener {
            DeviceRow currentRow;

            public void setPosition(int position) {
                this.currentRow = (DeviceRow) getItem(position);
            }

            @Override
            public void onClick(View v) {
                Log.i(Tag, "Off Button onClick. Position: " + currentRow.getAddress());
                currentRow.setIsOn(false);
                sendBrightCmd(currentRow, 0);
                notifyDataSetChanged();
                // currentRow.printDebug();
            }
        }

        private DiscreteSeekBar.NumericTransformer multiplyTransformer =
                new DiscreteSeekBar.NumericTransformer() {
                    @Override
                    public int transform(int value) {
                        return value * 5;
                    }
                };
    }

    public static byte intToByte(int a) {
        return (byte) (a & 0xFF);
    }

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 3];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 3] = hexArray[v >>> 4];
            hexChars[j * 3 + 1] = hexArray[v & 0x0F];
            hexChars[j * 3 + 2] = ' ';
        }
        return new String(hexChars);
    }
}
