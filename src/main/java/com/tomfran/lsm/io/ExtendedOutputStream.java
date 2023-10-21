package com.tomfran.lsm.io;

import com.tomfran.lsm.types.ByteArrayPair;
import it.unimi.dsi.fastutil.io.FastBufferedOutputStream;

import java.io.FileOutputStream;

/**
 * This class use a FastBufferedOutputStream as a base and adds
 * utility methods to it, mainly for writing variable-byte encoded longs and integers.
 */
public class ExtendedOutputStream {

    private static final byte[] VBYTE_BUFFER = new byte[10];
    private final FastBufferedOutputStream fos;

    /**
     * Initialize an output stream on a file.
     *
     * @param filename the file filename.
     */
    public ExtendedOutputStream(String filename) {
        try {
            fos = new FastBufferedOutputStream(new FileOutputStream(filename));
            fos.position(0);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Write a byte array to the stream.
     *
     * @param bytes array to write.
     * @return number of written bytes.
     */
    public int write(byte[] bytes) {
        try {
            fos.write(bytes);
            return bytes.length;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Write a variable-byte int to the stream.
     *
     * @param n integer to write.
     * @return number of written bytes.
     */
    public int writeVByteInt(int n) {
        return write(intToVByte(n));
    }

    /**
     * Write a variable-byte long to the stream.
     *
     * @param n long to write.
     * @return number of written bytes.
     */
    public int writeVByteLong(long n) {
        return write(longToVByte(n));
    }

    /**
     * Write 64 bits to the stream.
     *
     * @param n long to write.
     * @return number of written bytes.
     */
    public int writeLong(long n) {
        return write(longToBytes(n));
    }

    /**
     * Write a ByteArrayPair from the stream.
     * <p>
     * Each array is encoded as length, payload.
     *
     * @param pair item to write.
     * @return number of written bytes.
     */
    public int writeByteArrayPair(ByteArrayPair pair) {
        byte[] key = pair.key(), value = pair.value();
        byte[] keyBytes = intToVByte(key.length), valueBytes = intToVByte(value.length);

        byte[] result = new byte[keyBytes.length + valueBytes.length + key.length + value.length];

        System.arraycopy(keyBytes, 0, result, 0, keyBytes.length);
        System.arraycopy(valueBytes, 0, result, keyBytes.length, valueBytes.length);

        System.arraycopy(key, 0, result, keyBytes.length + valueBytes.length, key.length);
        System.arraycopy(value, 0, result, keyBytes.length + valueBytes.length + key.length, value.length);
        return write(result);
    }

    /**
     * Convert an int in V-Byte representation.
     *
     * @param n int to convert.
     * @return byte array storing the result.
     */
    byte[] intToVByte(int n) {
        return longToVByte(n);
    }


    /**
     * Close resources.
     */
    public void close() {
        try {
            fos.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] longToVByte(long n) {
        n++;

        if (n <= 0) {
            throw new IllegalArgumentException("n must be greater than 0");
        }

        int i = 0;
        while (n > 0) {
            VBYTE_BUFFER[i++] = (byte) (n & 0x7F);
            n >>>= 7;
        }

        VBYTE_BUFFER[i - 1] |= 0x80;
        byte[] res = new byte[i];
        System.arraycopy(VBYTE_BUFFER, 0, res, 0, i);
        return res;
    }

    private byte[] longToBytes(long n) {
        byte[] result = new byte[8];
        for (int i = 7; i >= 0; i--) {
            result[i] = (byte) (n & 0xFF);
            n >>>= 8;
        }
        return result;
    }

}
