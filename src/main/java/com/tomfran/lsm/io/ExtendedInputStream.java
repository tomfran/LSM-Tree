package com.tomfran.lsm.io;

import com.tomfran.lsm.types.ByteArrayPair;
import it.unimi.dsi.fastutil.io.FastBufferedInputStream;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * This class use a FastBufferedInputStream as a base and adds
 * utility methods to it, mainly for reading variable-byte encoded longs and integers.
 */
public class ExtendedInputStream {

    private final FastBufferedInputStream fis;

    /**
     * Initialize an input stream on a file.
     *
     * @param filename the file filename.
     */
    public ExtendedInputStream(String filename) {
        try {
            fis = new FastBufferedInputStream(new FileInputStream(filename));
            fis.position(0);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Read a variable byte int from the stream, see readVByteLong()
     *
     * @return the next V-Byte int.
     */
    public int readVByteInt() {
        return (int) readVByteLong();
    }

    /**
     * Read a variable byte long from the stream.
     * <p>
     * A variable byte long is written as:
     * <tt>|continuation bit| 7-bits payload|</tt>
     * <p>
     * For instance the number 10101110101010110 is represented using 24 bits as follows:
     * <p>
     * |1|1010110|1|0000101|0|0111010|
     *
     * @return the next V-Byte long.
     */
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
        return result - 1;
    }

    /**
     * Read 8 bytes representing a long.
     *
     * @return the next long in the stream.
     */
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

    /**
     * Read a single byte as an int.
     *
     * @return the next 8-bits integer in the stream.
     */
    public int readByteInt() {
        try {
            return fis.read();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Read N bytes.
     *
     * @param n the wanted number of bytes.
     * @return an array with the next N bytes.
     */
    public byte[] readNBytes(int n) {
        try {
            return fis.readNBytes(n);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Read a ByteArrayPair from the stream.
     * <p>
     * Each array is encoded as length, payload.
     *
     * @return the next item in the stream.
     */
    public ByteArrayPair readBytePair() {
        try {
            int keyLength = readVByteInt();
            int valueLength = readVByteInt();

            return new ByteArrayPair(
                    readNBytes(keyLength),
                    readNBytes(valueLength)
            );
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Skip N bytes from the stream.
     *
     * @param n the number of bytes to skip.
     * @return the number of bytes skipped.
     */
    public long skip(int n) {
        try {
            return fis.skip(n);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Position the stream at the wanted offset.
     *
     * @param offset the offset to place the stream to.
     */
    public void seek(long offset) {
        try {
            fis.position(offset);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Close resources.
     */
    public void close() {
        try {
            fis.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
