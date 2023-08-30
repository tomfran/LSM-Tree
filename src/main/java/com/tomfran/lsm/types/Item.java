package com.tomfran.lsm.types;

public record Item(byte[] key, byte[] value) {

    public byte[] toBytes() {
        byte[] bytes = new byte[key.length + value.length + 8];

        System.arraycopy(intToBytes(key.length), 0, bytes, 0, 4);
        System.arraycopy(intToBytes(value.length), 0, bytes, 4, 4);
        System.arraycopy(key, 0, bytes, 8, key.length);
        System.arraycopy(value, 0, bytes, 8 + key.length, value.length);

        return bytes;
    }

    private byte[] intToBytes(int n) {
        byte[] bytes = new byte[4];
        for (int i = 0; i < 4; i++)
            bytes[i] = (byte) (n >>> (24 - i * 8));
        return bytes;
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("key: ");
        for (byte b : key) {
            sb.append(String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0'));
        }
        sb.append(" value: ");
        for (byte b : value) {
            sb.append(String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0'));
        }
        return sb.toString();
    }
}
