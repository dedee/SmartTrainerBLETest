package de.dedee.vortexsmart1;

/*
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import org.junit.Test;
import org.mockito.Mockito;

import static junit.framework.Assert.assertEquals;

public class TestCyclingSpeedAndCadenceMeasurementData {

    @Test
    public void testParceable() {
        BluetoothGatt gatt = Mockito.mock(BluetoothGatt.class);
        BluetoothGattCharacteristic characteristic = Mockito.mock(BluetoothGattCharacteristic.class);

        Mockito.when(characteristic.getUuid()).thenReturn(CyclingSpeedAndCadenceMeasurementData.CSC_MEASUREMENT);
        Mockito.when(characteristic.getValue()).thenReturn(new byte[] { 3, 64, 1, 0, 0, 61, -6, 8, 0, 32, -38 });

        CyclingSpeedAndCadenceMeasurementData o = CyclingSpeedAndCadenceMeasurementData.parse(gatt, characteristic);

        assertEquals(1, o.getCumulativeWheelRevolutions());

    }
}

*/