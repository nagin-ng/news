package com.example.agylauncher;

/**
 * Bridge to libnhcore.so. The developer details are stored only inside the native library
 * (scrambled and checksummed), never as Java strings.
 */
final class NhCore {

    static final int NAME = 0;
    static final int TELEGRAM = 1;
    static final int EXPERIENCE = 2;
    static final int ROLE = 3;
    static final int HANDLE = 4;
    static final int CREDIT = 5;

    private static boolean loaded;

    static {
        try {
            System.loadLibrary("nhcore");
            loaded = true;
        } catch (Throwable t) {
            loaded = false;
        }
    }

    private NhCore() {}

    private static native String get(int id);

    private static native boolean img(byte[] data);

    /** Returns "" if the library is missing or the stored value fails its integrity check. */
    static String text(int id) {
        if (!loaded) return "";
        try {
            String s = get(id);
            return s == null ? "" : s;
        } catch (Throwable t) {
            return "";
        }
    }

    static boolean imageOk(byte[] data) {
        if (!loaded || data == null) return false;
        try {
            return img(data);
        } catch (Throwable t) {
            return false;
        }
    }
}
