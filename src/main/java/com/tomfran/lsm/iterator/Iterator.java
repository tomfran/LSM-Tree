package com.tomfran.lsm.iterator;

public interface Iterator {

    void next();

    boolean hasNext();

    byte[] key();

    byte[] value();

}
