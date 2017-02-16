package de.dedee.vortexsmart1;

public class Utils {


    public static int decodeUInt16(final byte[] data, int offset) {
        int l = 0;
        l |= (data[offset + 0] & 0xff) << 0;
        l |= (data[offset + 1] & 0xff) << 8;
        return l;
    }

    public static int decodeSInt16(final byte[] data, int offset) {
        short l = 0;
        l |= (data[offset + 0] & 0xff) << 0;
        l |= (data[offset + 1] & 0xff) << 8;
        return (int) l;
    }

    public static long decodeUInt32(final byte[] data, int offset) {
        long l = 0;
        l |= (data[offset + 0] & 0xff) << 0;
        l |= (data[offset + 1] & 0xff) << 8;
        l |= (data[offset + 2] & 0xff) << 16;
        l |= (data[offset + 3] & 0xff) << 24;
        return l;
    }

    public static String toHex(final byte[] data) {
        if (data == null)
            return "null";

        StringBuilder sb = new StringBuilder();
        for (int i=0; i<data.length; i++) {
            sb.append(String.format("%02X", data[i] & 0xff));
            if ((i+1) < data.length) {
                sb.append(' ');
            }
        }
        return sb.toString();
    }
}
