package it.mscuttari.kaoldb.core;

import android.util.Log;

class LogUtils {

    // Log tag
    private static final String LOG_TAG = "KaolDB";


    private LogUtils() {

    }


    /**
     * Log verbose message
     *
     * @param   message     message
     */
    public static void v(String message) {
        if (KaolDB.getInstance().config.debug)
            Log.v(LOG_TAG, message);
    }


    /**
     * Log debug message
     *
     * @param   message     message
     */
    public static void d(String message) {
        if (KaolDB.getInstance().config.debug)
            Log.d(LOG_TAG, message);
    }


    /**
     * Log information message
     *
     * @param   message     message
     */
    public static void i(String message) {
        if (KaolDB.getInstance().config.debug)
            Log.i(LOG_TAG, message);
    }


    /**
     * Log warning message
     *
     * @param   message     message
     */
    public static void w(String message) {
        if (KaolDB.getInstance().config.debug)
            Log.w(LOG_TAG, message);
    }


    /**
     * Log error message
     *
     * @param   message     message
     */
    public static void e(String message) {
        if (KaolDB.getInstance().config.debug)
            Log.e(LOG_TAG, message);
    }


    /**
     * Log critical message
     *
     * @param   message     message
     */
    public static void wtf(String message) {
        if (KaolDB.getInstance().config.debug)
            Log.wtf(LOG_TAG, message);
    }

}
