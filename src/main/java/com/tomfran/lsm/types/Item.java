package com.tomfran.lsm.types;

import java.util.Arrays;

public record Item(byte[] key, byte[] value) {
    @Override
    public int hashCode() {
        return Arrays.hashCode(key);
    }
}
