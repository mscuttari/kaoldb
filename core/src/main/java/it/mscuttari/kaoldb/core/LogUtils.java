/*
 * Copyright 2018 Scuttari Michele
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.mscuttari.kaoldb.core;

import android.util.Log;

class LogUtils {

    /** Log tag */
    private static final String LOG_TAG = "KaolDB";

    /** Whether the logs should be sent to Logcat or directly to the standard output */
    private static final boolean writeToLogcat = false;


    private LogUtils() {

    }


    /**
     * Log verbose message.
     *
     * @param message   message
     */
    public static void v(String message) {
        if (KaolDB.getInstance().config.isDebugEnabled()) {
            if (writeToLogcat) {
                Log.v(LOG_TAG, message);
            } else {
                System.out.println(message);
            }
        }
    }


    /**
     * Log debug message.
     *
     * @param message   message
     */
    public static void d(String message) {
        if (KaolDB.getInstance().config.isDebugEnabled()) {
            if (writeToLogcat) {
                Log.d(LOG_TAG, message);
            } else {
                System.out.println(message);
            }
        }
    }


    /**
     * Log information message.
     *
     * @param message   message
     */
    public static void i(String message) {
        if (KaolDB.getInstance().config.isDebugEnabled()) {
            if (writeToLogcat) {
                Log.i(LOG_TAG, message);
            } else {
                System.out.println(message);
            }
        }
    }


    /**
     * Log warning message.
     *
     * @param message   message
     */
    public static void w(String message) {
        if (KaolDB.getInstance().config.isDebugEnabled()) {
            if (writeToLogcat) {
                Log.w(LOG_TAG, message);
            } else {
                System.out.println(message);
            }
        }
    }


    /**
     * Log error message.
     *
     * @param message   message
     */
    public static void e(String message) {
        if (KaolDB.getInstance().config.isDebugEnabled()) {
            if (writeToLogcat) {
                Log.e(LOG_TAG, message);
            } else {
                System.out.println(message);
            }
        }
    }


    /**
     * Log critical message.
     *
     * @param message   message
     */
    public static void wtf(String message) {
        if (KaolDB.getInstance().config.isDebugEnabled()) {
            if (writeToLogcat) {
                Log.wtf(LOG_TAG, message);
            } else {
                System.out.println(message);
            }
        }
    }

}
