package de.dedee.vortexsmart1;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;
import android.os.Parcel;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.util.Log;

import java.util.UUID;

/**
 * Cycling Speed and Cadence Measurement Helper class
 */
public class CyclingSpeedAndCadenceMeasurementData implements Parcelable {

    public final static UUID CSC_MEASUREMENT = UUID.fromString("00002a5b-0000-1000-8000-00805f9b34fb");

    private long cumulativeWheelRevolutions;

    private int lastWheelEventTime;

    private int cumulativeCrankRevolutions;

    private int lastCrankEventTime;


    public long getCumulativeWheelRevolutions() {
        return cumulativeWheelRevolutions;
    }

    public int getLastWheelEventTime() {
        return lastWheelEventTime;
    }

    public int getCumulativeCrankRevolutions() {
        return cumulativeCrankRevolutions;
    }

    public int getLastCrankEventTime() {
        return lastCrankEventTime;
    }

    public static CyclingSpeedAndCadenceMeasurementData parse(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        CyclingSpeedAndCadenceMeasurementData o = null;
        if (gatt != null && characteristic != null && CSC_MEASUREMENT.equals(characteristic.getUuid())) {
            o = new CyclingSpeedAndCadenceMeasurementData();

            Log.i(C.TAG, "Cycling speed changed");
            byte[] data = characteristic.getValue();
            Log.d(C.TAG, "Raw data: " + Utils.toHex(data));

            int offset = 0;

            if (data != null && data.length > 0) {
                int b0 = data[offset++];

                // [2, 7, 0, 0, -48]
                // [3, 64, 1, 0, 0, 61, -6, 8, 0, 32, -38]

                // The data format is specified
                // https://developer.bluetooth.org/gatt/services/Pages/ServiceViewer.aspx?u=org.bluetooth.service.cycling_speed_and_cadence.xml

                // FLAG Byte defining which information is included in the data
                // 0x01 = Wheel revolutions included - C1
                // 0x02 = Crank revolutions included - C2
                // The fields in the above table are in the order of LSO to MSO. Where LSO = Least Significant Octet and MSO = Most Significant Octet

                if ((b0 & 0x01) > 0) {
                    // Wheel revolution data present

                    // Cumulative wheel revolutions, UINT32
                    o.cumulativeWheelRevolutions = Utils.decodeUInt32(data, offset);
                    offset += 4;
                    Log.i(C.TAG, "Cumulative wheel revolutions: " + o.cumulativeWheelRevolutions);

                    // Last wheel event time, UINT16
                    o.lastWheelEventTime = Utils.decodeUInt16(data, offset);
                    offset += 2;
                    Log.i(C.TAG, "Last wheel event time: " + o.lastWheelEventTime);

                }

                if ((b0 & 0x02) > 0) {
                    // Crank revolution data present

                    // Cumulative crank revolutions, UINT16
                    o.cumulativeCrankRevolutions = Utils.decodeUInt16(data, offset);
                    offset += 2;
                    Log.i(C.TAG, "Cumulative crank revolutions: " + o.cumulativeCrankRevolutions);

                    // Last crank event time, UINT16
                    o.lastCrankEventTime = Utils.decodeUInt16(data, offset);
                    offset += 2;
                    Log.i(C.TAG, "Last crank event time: " + o.lastCrankEventTime);
                }
            }
        }
        return o;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(cumulativeWheelRevolutions);
        dest.writeInt(lastWheelEventTime);
        dest.writeInt(cumulativeCrankRevolutions);
        dest.writeInt(lastCrankEventTime);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<CyclingSpeedAndCadenceMeasurementData> CREATOR = new Parcelable.Creator<CyclingSpeedAndCadenceMeasurementData>() {

        @Override
        public CyclingSpeedAndCadenceMeasurementData createFromParcel(Parcel source) {
            CyclingSpeedAndCadenceMeasurementData o = new CyclingSpeedAndCadenceMeasurementData();
            o.cumulativeWheelRevolutions = source.readLong();
            o.lastWheelEventTime = source.readInt();
            o.cumulativeCrankRevolutions = source.readInt();
            o.lastCrankEventTime = source.readInt();
            return o;
        }

        @Override
        public CyclingSpeedAndCadenceMeasurementData[] newArray(int size) {
            return new CyclingSpeedAndCadenceMeasurementData[size];
        }
    };

}
