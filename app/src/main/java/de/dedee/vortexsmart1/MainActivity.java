package de.dedee.vortexsmart1;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
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

import java.util.UUID;


public class MainActivity extends AppCompatActivity {

    private final static int REQUEST_ENABLE_BT = 1;

    // Cycling Speed and Cadence Service
    //private final static ParcelUuid CSC_SERVICE = ParcelUuid.fromString("00001816-0000-1000-8000-00805f9b34fb");
    // Cycling Power Service
    // https://developer.bluetooth.org/gatt/services/Pages/ServiceViewer.aspx?u=org.bluetooth.service.cycling_power.xml
//    private final static ParcelUuid CP_SERVICE = ParcelUuid.fromString("00001818-0000-1000-8000-00805f9b34fb");
    private final static UUID SERVICE_HEART_RATE_SENSOR = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb");
    private final static UUID CHARACTERISTIC_HEART_RATE = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");

    // Cycling Speed and Cadence Measurement
//    private final static ParcelUuid CSC_MEASUREMENT = ParcelUuid.fromString("00002a5b-0000-1000-8000-00805f9b34fb");
    // Cycling Power Measurement Characteristic
    //  private final static ParcelUuid CP_MEASUREMENT = ParcelUuid.fromString("00002a63-0000-1000-8000-00805f9b34fb");
    private static final int MY_PERMISSIONS_ID = 1;
    // Cycling Power Measurement
    //private final static ParcelUuid CYCLING_POWER_MEASUREMENT = ParcelUuid.fromString("00002A63-0000-1000-8000-00805f9b34fb");


    private BroadcastReceiver receiverStatus;
    private BroadcastReceiver receiverData;

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BleScan bleScan;

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
        // Use this check to determine whether BLE is supported on the device. Then
        // you can selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE not supported", Toast.LENGTH_SHORT).show();
            finish();
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_ID);
        }

        // Initializes Bluetooth adapter.
        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {

            bleScan = new BleScan(this, bluetoothAdapter, SERVICE_HEART_RATE_SENSOR, CHARACTERISTIC_HEART_RATE);

        }

        // The receiver will get BLE sensor data and update UI in correct thread
        receiverStatus = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(BleScan.ACTION_SCAN_DATA)) {
                    Parcelable parcelableExtra = intent.getParcelableExtra(BleScan.ACTION_SCAN_DATA);
                    boolean success = intent.getBooleanExtra("success", false);
                    String address = intent.getStringExtra("address");
                    String name = intent.getStringExtra("name");
                    ((TextView) findViewById(R.id.textViewDeviceName)).setText(name);
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(BleScan.ACTION_SCAN_DATA);
        registerReceiver(receiverStatus, filter);

        receiverData = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(BleScan.ACTION_CHARACTERISTIC_DATA)) {
                    Parcelable parcelableExtra = intent.getParcelableExtra(BleScan.ACTION_CHARACTERISTIC_DATA);
                    UUID uuid = UUID.fromString(intent.getStringExtra("uuid"));
                    byte[] value = intent.getByteArrayExtra("value");
                    String s = Utils.toHex(value);
                    Log.i(C.TAG, "UPDATE " + uuid + ": " + s);
                    ((TextView) findViewById(R.id.textViewHR)).setText(s);
                }
            }
        };

        filter = new IntentFilter();
        filter.addAction(BleScan.ACTION_CHARACTERISTIC_DATA);
        registerReceiver(receiverData, filter);
    }

    @Override
    protected void onDestroy() {
        if (bleScan != null) {
            bleScan.close();
        }
        if (receiverStatus != null) {
            unregisterReceiver(receiverStatus);
        }
        if (receiverData != null) {
            unregisterReceiver(receiverData);
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


}
