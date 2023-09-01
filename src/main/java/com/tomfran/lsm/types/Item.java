package com.tomfran.lsm.types;

public record Item(byte[] key, byte[] value) {

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
