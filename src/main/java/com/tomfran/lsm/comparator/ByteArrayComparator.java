package com.tomfran.lsm.comparator;

public class ByteArrayComparator {

    static int compare(byte[] a, byte[] b) {
        int aLen = a.length;
        int bLen = b.length;

        if (aLen != bLen)
            return aLen < bLen ? -1 : 1;

        for (int i = 0; i < aLen; i++) {
            byte aByte = a[i];
            byte bByte = b[i];
            if (aByte != bByte)
                return aByte < bByte ? -1 : 1;
        }
        return 0;
    }

}
