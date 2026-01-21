package com.dismal.fireplayer.floatwindow;

public class Utils {
    public static boolean isSet(int flags, int flag) {
        return (flags & flag) == flag;
    }
}
