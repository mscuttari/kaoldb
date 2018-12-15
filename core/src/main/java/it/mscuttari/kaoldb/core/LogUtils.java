package it.mscuttari.kaoldb.core;

import android.util.Log;

class LogUtils {

    // Log tag
    private static final String LOG_TAG = "KaolDB";
    private static final boolean writeToLogcat = false;


    private LogUtils() {

    }


    /**
     * Log verbose message
     *
     * @param message   message
     */
    public static void v(String message) {
        if (KaolDB.getInstance().getConfig().isDebugEnabled()) {
            if (writeToLogcat) {
                Log.v(LOG_TAG, message);
            } else {
                System.out.println(message);
            }
        }
    }


    /**
     * Log debug message
     *
     * @param message   message
     */
    public static void d(String message) {
        if (KaolDB.getInstance().getConfig().isDebugEnabled()) {
            if (writeToLogcat) {
                Log.d(LOG_TAG, message);
            } else {
                System.out.println(message);
            }
        }
    }


    /**
     * Log information message
     *
     * @param message   message
     */
    public static void i(String message) {
        if (KaolDB.getInstance().getConfig().isDebugEnabled()) {
            if (writeToLogcat) {
                Log.i(LOG_TAG, message);
            } else {
                System.out.println(message);
            }
        }
    }


    /**
     * Log warning message
     *
     * @param message   message
     */
    public static void w(String message) {
        if (KaolDB.getInstance().getConfig().isDebugEnabled()) {
            if (writeToLogcat) {
                Log.w(LOG_TAG, message);
            } else {
                System.out.println(message);
            }
        }
    }


    /**
     * Log error message
     *
     * @param message   message
     */
    public static void e(String message) {
        if (KaolDB.getInstance().getConfig().isDebugEnabled()) {
            if (writeToLogcat) {
                Log.e(LOG_TAG, message);
            } else {
                System.out.println(message);
            }
        }
    }


    /**
     * Log critical message
     *
     * @param message   message
     */
    public static void wtf(String message) {
        if (KaolDB.getInstance().getConfig().isDebugEnabled()) {
            if (writeToLogcat) {
                Log.wtf(LOG_TAG, message);
            } else {
                System.out.println(message);
            }
        }
    }

}
