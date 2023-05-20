package com.tomfran.lsm.interfaces;

public interface Iterator {

    void next();

    boolean hasNext();

    byte[] key();

    byte[] value();

}
