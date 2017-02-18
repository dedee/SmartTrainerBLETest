package de.dedee.vortexsmart1;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.ParcelUuid;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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


public class BleScan {

    private final static UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    public final static String ACTION_SCAN_DATA = "ble_scan_data";
    public final static String ACTION_CHARACTERISTIC_DATA = "ble_characteristic_data";

    private final MyScanCallback scanCallback = new MyScanCallback();
    private final MyGattCallback gattCallback = new MyGattCallback();
    private final UUID serviceUUID;

    private Context context;
    private BluetoothLeScanner bluetoothLeScanner;

    private Set<UUID> characteristicsUuids;
    private List<BluetoothGattCharacteristic> characteristicsToRegisterNotifications = new ArrayList<>();
    private List<BluetoothGatt> gatts = new ArrayList<>();

    public BleScan(Context context, BluetoothAdapter bluetoothAdapter, UUID serviceUUID, UUID ... characteristicsUuidsList) {
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        this.characteristicsUuids = characteristicsUuids;
        this.serviceUUID = serviceUUID;
        this.context = context;
        this.characteristicsUuids = new HashSet<>();
        for (UUID uuid : characteristicsUuidsList) {
            characteristicsUuids.add(uuid);
        }

        if (bluetoothLeScanner != null) {
            ScanFilter filter = new ScanFilter.Builder().setServiceUuid(new ParcelUuid(serviceUUID)).build();
            List<ScanFilter> filters = new ArrayList<ScanFilter>();
            filters.add(filter);

            ScanSettings settings = new ScanSettings.Builder().build();
            bluetoothLeScanner.startScan(filters, settings, scanCallback);
            Log.d(C.TAG, "BLE scan started");
        } else {
            Intent intent = new Intent(ACTION_SCAN_DATA);
            intent.putExtra("success", false);
            context.sendBroadcast(intent);
        }
    }

    public void close() {
        if (bluetoothLeScanner != null) {
            bluetoothLeScanner.stopScan(scanCallback);
        }
        for (BluetoothGatt gatt : gatts) {
            gatt.close();
        }
        gatts.clear();
    }


    class MyScanCallback extends ScanCallback {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.d(C.TAG, "onScanResult " + result);

            BluetoothDevice device = result.getDevice();
            String name = device.getName();

            Intent intent = new Intent(ACTION_SCAN_DATA);
            intent.putExtra("success", true);
            intent.putExtra("address", device.getAddress());
            intent.putExtra("name", device.getName());
            context.sendBroadcast(intent);

            if (bluetoothLeScanner != null) {
                bluetoothLeScanner.stopScan(scanCallback);
                bluetoothLeScanner = null;
                Log.d(C.TAG, "BLE scan stopped after we got a result. All fine.");
            }

            device.connectGatt(context, false, gattCallback);
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e(C.TAG, "onScanFailed " + errorCode);
            bluetoothLeScanner = null;

            Intent intent = new Intent(ACTION_SCAN_DATA);
            intent.putExtra("success", false);
            intent.putExtra("errorCode", errorCode);
            context.sendBroadcast(intent);
        }
    }

    class MyGattCallback extends BluetoothGattCallback {

        private BluetoothGatt bluetoothGatt;


        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.d(C.TAG, "onConnectionStateChange " + status + " - " + newState);

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(C.TAG, "GATT is connected. Listing services:");
                String name = gatt.getDevice().getName();
                Log.d(C.TAG, "Device " + name + " connected");
                gatt.discoverServices();
                gatts.add(gatt);
                bluetoothGatt = gatt;
            } else {
                Log.e(C.TAG, "GATT connection change problem: " + newState);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                for (BluetoothGattService service : gatt.getServices()) {
                    UUID uuid = service.getUuid();
                    Log.d(C.TAG, "GATT service " + uuid);
                    if (serviceUUID.equals(uuid)) {
                        for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                            UUID characteristicUuid = characteristic.getUuid();
                            Log.d(C.TAG, "Characteristic discovered: " + characteristicUuid);

                            if (characteristicsUuids.contains(characteristicUuid)) {
                                Log.i(C.TAG, "Characteristic " + characteristicUuid + " found.");
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
            UUID uuid = characteristic.getUuid();
            byte[] data = characteristic.getValue();
            String s = Utils.toHex(data);
            Log.d(C.TAG, "Characteristic " + uuid + " notification changed: " + s);

            Intent intent = new Intent(ACTION_CHARACTERISTIC_DATA);
            intent.putExtra("uuid", characteristic.getUuid().toString());
            intent.putExtra("value", characteristic.getValue());
            context.sendBroadcast(intent);
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