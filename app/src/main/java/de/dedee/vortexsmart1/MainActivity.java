package de.dedee.vortexsmart1;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

// Good resources:
// ---------------
// Google I/O 2013 - Best Practices for Bluetooth Development
// https://www.youtube.com/watch?v=EC5-cEbr520
// Android Bluetooth Low Energy Tutorial
// http://toastdroid.com/2014/09/22/android-bluetooth-low-energy-tutorial/

// ANT+ FE-C
// 6.8.1 Data Page 48 (0x30) – Basic Resistance
// The basic resistance page is sent by the open display to command the fitness equipment to use
// basic resistance mode, and to set the desired resistance. Any open display or fitness equipment
// that supports the FE-C use case may support this data page. This page shall be transmitted as
// an acknowledged message from the open display device to the fitness equipment.
// 6.8.2 Data Page 49 (0x31) – Target Power
// The target power page is sent by the open display to command the fitness equipment to use target
// power mode, and to set the desired target power. All open displays and fitness equipment that
// support the FE-C use case are required to support this data page. This page shall be transmitted
// as an acknowledged message from the open display device to the fitness equipment.

public class MainActivity extends AppCompatActivity {


    private final static int REQUEST_ENABLE_BT = 1;
    private final static String ACTION_DATA = "vortex_smart_new_data";
    private final static UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");


    // Cycling Speed and Cadence Service
    private final static ParcelUuid CSC_SERVICE = ParcelUuid.fromString("00001816-0000-1000-8000-00805f9b34fb");
    // Cycling Power Service
    // https://developer.bluetooth.org/gatt/services/Pages/ServiceViewer.aspx?u=org.bluetooth.service.cycling_power.xml
    private final static ParcelUuid CP_SERVICE = ParcelUuid.fromString("00001818-0000-1000-8000-00805f9b34fb");
    private final static ParcelUuid SERVICE_HEART_RATE_SENSOR = ParcelUuid.fromString("0000180d-0000-1000-8000-00805f9b34fb");
    private final static ParcelUuid CHARACTERISTIC_HEART_RATE = ParcelUuid.fromString("00002a37-0000-1000-8000-00805f9b34fb");

    // Cycling Speed and Cadence Measurement
    private final static ParcelUuid CSC_MEASUREMENT = ParcelUuid.fromString("00002a5b-0000-1000-8000-00805f9b34fb");
    // Cycling Power Measurement Characteristic
    private final static ParcelUuid CP_MEASUREMENT = ParcelUuid.fromString("00002a63-0000-1000-8000-00805f9b34fb");
    private static final int MY_PERMISSIONS_ID = 1;
    // Cycling Power Measurement
    //private final static ParcelUuid CYCLING_POWER_MEASUREMENT = ParcelUuid.fromString("00002A63-0000-1000-8000-00805f9b34fb");


    private BroadcastReceiver receiver;
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private BluetoothGatt bluetoothGatt;
    private List<BluetoothGattCharacteristic> characteristicsToRegisterNotifications = new ArrayList<BluetoothGattCharacteristic>();


    private final MyScanCallback scanCallback = new MyScanCallback();
    private final MyGattCallback gattCallback = new MyGattCallback();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        // BLE set up

// Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_CONTACTS)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_ID);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }
        // Use this check to determine whether BLE is supported on the device. Then
        // you can selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE not supported", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes Bluetooth adapter.
        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        if (bluetoothAdapter != null) {
            bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
            if (bluetoothLeScanner != null) {
//                ScanFilter vortexSmartDeviceFilter = new ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString("00001818-0000-1000-8000-00805f9b34fb")).build();
                ScanFilter vortexSmartDeviceFilter = new ScanFilter.Builder().setServiceUuid(SERVICE_HEART_RATE_SENSOR).build();
                List<ScanFilter> filters = new ArrayList<ScanFilter>();
                filters.add(vortexSmartDeviceFilter);

                ScanSettings settings = new ScanSettings.Builder().build();
                bluetoothLeScanner.startScan(filters, settings, scanCallback);
                Log.d(C.TAG, "BLE scan started");
            } else {
                Toast.makeText(this, "BLE not on?", Toast.LENGTH_SHORT).show();
            }
        }

        // The receiver will get BLE sensor data and update UI in correct thread
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(ACTION_DATA)) {

                    Parcelable parcelableExtra = intent.getParcelableExtra(ACTION_DATA);
                    if (parcelableExtra != null && parcelableExtra instanceof CyclingSpeedAndCadenceMeasurementData) {
                        CyclingSpeedAndCadenceMeasurementData o = (CyclingSpeedAndCadenceMeasurementData) parcelableExtra;
                        ((TextView) findViewById(R.id.textViewCWR)).setText("" + o.getCumulativeWheelRevolutions());
                        ((TextView) findViewById(R.id.textViewCCR)).setText("" + o.getCumulativeCrankRevolutions());
                    } else {

                        // FIXME to be reworked
                        String sensor = intent.getStringExtra("sensor");
                        if ("wheel_revolutions".equals(sensor)) {
                            ((TextView) findViewById(R.id.textViewCWR)).setText("" + intent.getLongExtra("cumulative_revolutions", 0));
                        } else if ("crank_revolutions".equals(sensor)) {
                            ((TextView) findViewById(R.id.textViewCCR)).setText("" + intent.getIntExtra("cumulative_revolutions", 0));
                        } else if ("instantaneous_power".equals(sensor)) {
                            ((TextView) findViewById(R.id.textViewIP)).setText("" + intent.getIntExtra("instantaneous_power", 0));
                        } else if ("heart_rate".equals(sensor)) {
                            ((TextView) findViewById(R.id.textViewHR)).setText("" + intent.getIntExtra("heart_rate", 0));
                        }
                    }

                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_DATA);
        registerReceiver(receiver, filter);
    }

    @Override
    protected void onDestroy() {
        if (bluetoothLeScanner != null) {
            bluetoothLeScanner.stopScan(scanCallback);
        }
        if (bluetoothGatt != null) {
            bluetoothGatt.close();
            bluetoothGatt = null;
        }
        if (receiver != null) {
            unregisterReceiver(receiver);
        }
        super.onDestroy();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    class MyScanCallback extends ScanCallback {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.d(C.TAG, "onScanResult " + result);

            BluetoothDevice device = result.getDevice();
            String name = device.getName();
            ((TextView) MainActivity.this.findViewById(R.id.textViewDeviceName)).setText(name);

            if (bluetoothLeScanner != null) {
                bluetoothLeScanner.stopScan(scanCallback);
                bluetoothLeScanner = null;
                Log.d(C.TAG, "BLE scan stopped after we got a result. All fine.");
            }

            device.connectGatt(MainActivity.this, false, gattCallback);
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e(C.TAG, "onScanFailed " + errorCode);
            bluetoothLeScanner = null;
        }
    }

    class MyGattCallback extends BluetoothGattCallback {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.d(C.TAG, "onConnectionStateChange " + status + " - " + newState);

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(C.TAG, "GATT is connected. Listing services:");
                String name = gatt.getDevice().getName();
                Log.d(C.TAG, "Device " + name + " connected");
                gatt.discoverServices();
                bluetoothGatt = gatt;
            } else {
                Log.e(C.TAG, "GATT connection change problem: " + newState);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {


                for (BluetoothGattService service : gatt.getServices()) {
                    Log.d(C.TAG, "GATT service " + service.getUuid());

                    if (CSC_SERVICE.getUuid().equals(service.getUuid())) {

                        for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                            UUID uuid = characteristic.getUuid();
                            Log.d(C.TAG, "Characteristic discovered: " + uuid);

                            if (uuid.equals(CSC_MEASUREMENT.getUuid())) {
                                Log.i(C.TAG, "CSC Measurement characteristic found.");
                                characteristicsToRegisterNotifications.add(characteristic);
                            }
                        }

                    } else if (CP_SERVICE.getUuid().equals(service.getUuid())) {

                        for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                            UUID uuid = characteristic.getUuid();
                            Log.d(C.TAG, "Characteristic discovered: " + uuid);

                            if (uuid.equals(CP_MEASUREMENT.getUuid())) {
                                Log.i(C.TAG, "CP Measurement characteristic found.");
                                characteristicsToRegisterNotifications.add(characteristic);
                            }
                        }
                    } else if (SERVICE_HEART_RATE_SENSOR.getUuid().equals(service.getUuid())) {
                        for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                            UUID uuid = characteristic.getUuid();
                            Log.d(C.TAG, "Characteristic discovered: " + uuid);

                            if (uuid.equals(CHARACTERISTIC_HEART_RATE.getUuid())) {
                                Log.i(C.TAG, "CHARACTERISTIC_HEART_RATE characteristic found.");
                                characteristicsToRegisterNotifications.add(characteristic);
                            }
                        }
                    }

                }

                // Call once from here. The next time it will be called asynchronously from callback
                registerNextCharacteristic(gatt);

            } else {
                Log.w(C.TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            registerNextCharacteristic(gatt);
        }

        private void registerNextCharacteristic(BluetoothGatt gatt) {
            if (characteristicsToRegisterNotifications.size() > 0) {
                BluetoothGattCharacteristic characteristic = characteristicsToRegisterNotifications.remove(0);
                Log.d(C.TAG, "Registering for characteristic notifications. " + characteristic.getUuid());
                gatt.setCharacteristicNotification(characteristic, true);
                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                gatt.writeDescriptor(descriptor);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {

            if (CSC_MEASUREMENT.getUuid().equals(characteristic.getUuid())) {
                CyclingSpeedAndCadenceMeasurementData cyclingSpeedAndCadenceMeasurementData = CyclingSpeedAndCadenceMeasurementData.parse(gatt, characteristic);
                if (cyclingSpeedAndCadenceMeasurementData != null) {
                    Intent intent = new Intent(ACTION_DATA);
                    intent.putExtra(ACTION_DATA, cyclingSpeedAndCadenceMeasurementData);
                    sendBroadcast(intent);
                }

            } else if (CP_MEASUREMENT.getUuid().equals(characteristic.getUuid())) {
                Log.i(C.TAG, "Cycling power changed");
                byte[] data = characteristic.getValue();
                String s = Utils.toHex(data);
                Log.d(C.TAG, "Cycling power raw data: " + s);

                // Cycling power raw data: 30 00 00 00 90 3A 00 00 2B E0 0A 16 00 E0
                if (data != null && data.length > 0) {
                    int support = Utils.decodeUInt16(data, 0);
                    // Currently just my supported ones implemented. There are so much more

                    boolean pedalPowerBalancePresent = (support & 0x01) > 0;
                    boolean accumulatedTorquePresent = (support & 0x04) > 0;

                    // Next two are duplicates to other service above. FIXME
                    boolean wheelRevolutionDataPresent = (support & 0x10) > 0;
                    boolean crankRevolutionDataPresent = (support & 0x20) > 0;

                    Log.i(C.TAG, "wheelRevolutionDataPresent=" + wheelRevolutionDataPresent + ", crankRevolutionDataPresent=" + crankRevolutionDataPresent);

                    int offset = 1;
                    // Instantaneous Power, SINT16
                    int instantaneousPower = Utils.decodeSInt16(data, offset);
                    offset += 2;

                    Intent intent = new Intent(ACTION_DATA);
                    intent.putExtra("sensor", "instantaneous_power");
                    intent.putExtra("instantaneous_power", instantaneousPower);
                    sendBroadcast(intent);
                }

            } else if (CHARACTERISTIC_HEART_RATE.getUuid().equals(characteristic.getUuid())) {
                Log.i(C.TAG, "HEART RATE changed");
                byte[] data = characteristic.getValue();
                String s = Utils.toHex(data);
                Log.d(C.TAG, "HEART RATE raw data: " + s);

                int heartRate = 0;
                if(data.length > 3) {
                    int type = characteristic.getValue()[0] & 0xff;
                    if ((type & 0x01) > 0) {
                        heartRate = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 1);
                        Log.i(C.TAG, "HEART RATE: " + heartRate);
                    }else {
                        heartRate = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1);
                        Log.i(C.TAG, "HEART RATE: " + heartRate);
                    }
                }

                Intent intent = new Intent(ACTION_DATA);
                intent.putExtra("sensor", "heart_rate");
                intent.putExtra("heart_rate", heartRate);
                sendBroadcast(intent);


            } else {
                byte[] data = characteristic.getValue();
                String s = Utils.toHex(data);
                Log.d(C.TAG, "Characteristic notification changed: " + s);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                UUID uuid = characteristic.getUuid();
                Log.d(C.TAG, "Characteristic data available " + uuid);
                Log.d(C.TAG, "Value: " + Arrays.toString(characteristic.getValue()));
            }
        }
    }
}
