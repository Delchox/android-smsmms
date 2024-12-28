/*
 * Copyright 2014 Jacob Klinker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.klinker.android.logger;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.ParcelFileDescriptor;

import com.microspacegames.app.utils.UriHelper;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class Log {

    public static final int VERBOSE = 2;
    public static final int DEBUG = 3;
    public static final int INFO = 4;
    public static final int WARN = 5;
    public static final int ERROR = 6;
    public static final int ASSERT = 7;

    private static final String TAG = "Log";

    private static boolean DEBUG_ENABLED = false;
    private static final String FILE_NAME = "ApplicationLog.txt";
    private static String PATH;
    private static Uri FILE_URI;
    private static OnLogListener logListener;
    private static ContentResolver CONTENT_RESOLVER;

    public static void setDebug(@Nullable ContentResolver contentResolver, boolean debug) {
        DEBUG_ENABLED = debug;
        CONTENT_RESOLVER = contentResolver;
    }

    public static void setPath(@NotNull String path) {
        if (Build.VERSION.SDK_INT >= 29) {
            if (UriHelper.isTreeUri(path)) {
                PATH = path;
            }
        } else {
            if (path.endsWith("/")) {
                PATH = path + FILE_NAME;
            } else if (!path.endsWith(".txt")) {
                PATH = path + ".txt";
            } else {
                PATH = path;
            }
        }
    }

    public static void setLogListener(OnLogListener listener) {
        logListener = listener;
    }

    public static void e(String tag, String message) {
        if (DEBUG_ENABLED) {
            int logResult = android.util.Log.e(tag, message);
            if (logResult > 0)
                logToFile(tag, message);
        }
    }

    public static void e(String tag, String message, Throwable error) {
        if (DEBUG_ENABLED) {
            int logResult = android.util.Log.e(tag, message, error);
            if (logResult > 0)
                logToFile(tag, message + "\r\n" + android.util.Log.getStackTraceString(error));
        }
    }

    public static void v(String tag, String message) {
        if (DEBUG_ENABLED) {
            int logResult = android.util.Log.v(tag, message);
            if (logResult > 0)
                logToFile(tag, message);
        }
    }

    public static void v(String tag, String message, Throwable error) {
        if (DEBUG_ENABLED) {
            int logResult = android.util.Log.v(tag, message, error);
            if (logResult > 0)
                logToFile(tag, message + "\r\n" + android.util.Log.getStackTraceString(error));
        }
    }

    public static void d(String tag, String message) {
        if (DEBUG_ENABLED) {
            int logResult = android.util.Log.d(tag, message);
            if (logResult > 0)
                logToFile(tag, message);
        }
    }

    public static void d(String tag, String message, Throwable error) {
        if (DEBUG_ENABLED) {
            int logResult = android.util.Log.d(tag, message, error);
            if (logResult > 0)
                logToFile(tag, message + "\r\n" + android.util.Log.getStackTraceString(error));
        }
    }

    public static void i(String tag, String message) {
        if (DEBUG_ENABLED) {
            int logResult = android.util.Log.i(tag, message);
            if (logResult > 0)
                logToFile(tag, message);
        }
    }

    public static void i(String tag, String message, Throwable error) {
        if (DEBUG_ENABLED) {
            int logResult = android.util.Log.i(tag, message, error);
            if (logResult > 0)
                logToFile(tag, message + "\r\n" + android.util.Log.getStackTraceString(error));
        }
    }

    public static void w(String tag, String message) {
        if (DEBUG_ENABLED) {
            int logResult = android.util.Log.w(tag, message);
            if (logResult > 0)
                logToFile(tag, message);
        }
    }

    public static void w(String tag, String message, Throwable error) {
        if (DEBUG_ENABLED) {
            int logResult = android.util.Log.w(tag, message, error);
            if (logResult > 0)
                logToFile(tag, message + "\r\n" + android.util.Log.getStackTraceString(error));
        }
    }

    public static boolean isLoggable(String string, int num) {
        return true;
    }

    private static String getDateTimeStamp() {
        Date dateNow = Calendar.getInstance().getTime();
        return (DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.US).format(dateNow));
    }

    private static void logToFile(String tag, String message) {
        if (Build.VERSION.SDK_INT >= 29) {
            if (CONTENT_RESOLVER != null) {
                ParcelFileDescriptor parcelFileDescriptor;
                try {
                    if (FILE_URI == null) {
                        FILE_URI = UriHelper.findFileTreeUri(CONTENT_RESOLVER, PATH, FILE_NAME);
                    }
                    if (FILE_URI != null) {
                        parcelFileDescriptor = CONTENT_RESOLVER.openFileDescriptor(FILE_URI, "wa");
                        if (parcelFileDescriptor != null) {
                            FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                            BufferedWriter writer = new BufferedWriter(new FileWriter(fileDescriptor));
                            writer.write(String.format("%1s [%2s]:%3s\r\n", getDateTimeStamp(), tag, message));
                            writer.close();

                        }
                    }
                } catch (IOException e) {
                    android.util.Log.e(TAG, "Unable to log exception to file.", e);
                }
            } else {
                android.util.Log.e(TAG, "Unable to log exception to file. ContentResolver is null.");
            }
        } else {
            try {
                File logFile = new File(Environment.getExternalStorageDirectory(), PATH);
                if (!logFile.exists()) {
                    logFile.getParentFile().mkdirs();
                    logFile.createNewFile();
                }
                if (logFile.length() > 2097152) { // 2 MB
                    logFile.delete();
                    logFile.createNewFile();
                }
                BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true));
                writer.write(String.format("%1s [%2s]:%3s\r\n", getDateTimeStamp(), tag, message));
                writer.close();

                if (logListener != null) {
                    logListener.onLogged(tag, message);
                }
            } catch (IOException e) {
                android.util.Log.e(TAG, "Unable to log exception to file.", e);
            }
        }
    }
}