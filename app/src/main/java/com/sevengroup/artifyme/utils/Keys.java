package com.sevengroup.artifyme.utils;

public class Keys {
    static {
        System.loadLibrary("native-lib");
    }

    public static native String getRemoveBgKey();
}