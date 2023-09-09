package com.tomfran.lsm.io;

import it.unimi.dsi.fastutil.io.FastBufferedInputStream;

import java.io.FileInputStream;
import java.io.IOException;

public class BaseInputStream {

    private final FastBufferedInputStream fis;

    public BaseInputStream(String filename) {
        try {
            fis = new FastBufferedInputStream(new FileInputStream(filename));
            fis.position(0);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public int readVByteInt() {
        return (int) readVByteLong();
    }

    public long readVByteLong() {
        long result = 0;
        int b;
        int shift = 0;
        while (true) {
            b = readByteInt();
            result |= (((long) b & 0x7F) << shift);

            if ((b & 0x80) == 0x80)
                break;

            shift += 7;
        }
        return result;
    }

    public long readLong() {
        try {
            long result = 0;
            for (byte b : fis.readNBytes(8)) {
                result <<= 8;
                result |= (b & 0xFF);
            }
            return result;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public int readByteInt() {
        try {
            return fis.read();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] readNBytes(int n) {
        try {
            return fis.readNBytes(n);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void seek(long offset) {
        try {
            fis.position(offset);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean hasNext() {
        try {
            return fis.available() > 0;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void close() {
        try {
            fis.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
