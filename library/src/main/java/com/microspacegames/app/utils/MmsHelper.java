package com.microspacegames.app.utils;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;

import com.klinker.android.logger.Log;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class MmsHelper {

    private final static String TAG = "MmsHelper";

    public static class MmsMessage {
        private final String id;
        private final String threadId;
        private final List<MmsPart> parts;

        public MmsMessage(String id, String threadId, List<MmsPart> parts) {
            this.id = id;
            this.threadId = threadId;
            this.parts = parts;
        }

        public String getId() {
            return id;
        }

        public String getThreadId() {
            return threadId;
        }

        public List<MmsPart> getParts() {
            return parts;
        }
    }

    public static class MmsPart {
        private final String id;
        private final String contentType;
        private final Object data;

        public MmsPart(String id, String contentType, Object data) {
            this.id = id;
            this.contentType = contentType;
            this.data = data;
        }

        public String getId() {
            return id;
        }

        public String getContentType() {
            return contentType;
        }

        public Object getData() {
            return data;
        }
    }

    public static List<MmsMessage> getAllMmsMessages(ContentResolver contentResolver) {
        List<MmsMessage> mmsMessages = new ArrayList<>();
        Uri uri = Uri.parse("content://mms");
        String[] projection = {Telephony.Mms._ID, Telephony.Mms.THREAD_ID};

        Cursor cursor = contentResolver.query(uri, projection, null, null, null);

        if (cursor != null) {
            try {
                int idIndex = cursor.getColumnIndex("_id");
                int threadIdIndex = cursor.getColumnIndex("thread_id");

                while (cursor.moveToNext()) {
                    String id = cursor.getString(idIndex);
                    String threadId = cursor.getString(threadIdIndex);

                    List<MmsPart> parts = getMmsParts(contentResolver, id);
                    mmsMessages.add(new MmsMessage(id, threadId, parts));
                }
            } finally {
                cursor.close();
            }
        }

        return mmsMessages;
    }
    @SuppressLint("NewApi")
    public static List<MmsPart> getMmsParts(ContentResolver contentResolver, String mmsId) {
        List<MmsPart> parts = new ArrayList<>();
        Uri uri = Uri.parse("content://mms/part");
        String[] projection = {"_id", "ct", "text"};
        String selection = "mid=?";
        String[] selectionArgs = {mmsId};

        Cursor cursor = contentResolver.query(uri, projection, selection, selectionArgs, null);

        if (cursor != null) {
            try {
                int idIndex = cursor.getColumnIndex("_id");
                int ctIndex = cursor.getColumnIndex("ct");
                int textIndex = cursor.getColumnIndex("text");

                while (cursor.moveToNext()) {
                    String id = cursor.getString(idIndex);
                    String contentType = cursor.getString(ctIndex);
                    String text = cursor.getString(textIndex);

                    Uri partUri = Uri.parse("content://mms/part/" + id);
                    Object data;

                    if ("text/plain".equals(contentType) || "application/smil".equals(contentType)) {
                        data = text;
                        cursor.close();
                        ContentValues contentValues = new ContentValues();
                        contentValues.put(Telephony.Mms.Part.TEXT, "qqqqqqqq");
                        int rows = contentResolver.update(partUri, contentValues, null, null);
                        Log.d(TAG, "rows updated: " + rows);
                    } else {
                        data = readPartData(contentResolver, partUri);
                    }

                    parts.add(new MmsPart(id, contentType, data));
                }
            } finally {
                cursor.close();
            }
        }

        return parts;
    }

    private static byte[] readPartData(ContentResolver contentResolver, Uri partUri) {
        InputStream inputStream = null;
        ByteArrayOutputStream outputStream = null;

        try {
            inputStream = contentResolver.openInputStream(partUri);
            if (inputStream == null) {
                return null;
            }

            outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
            }

            return outputStream.toByteArray();
        } catch (Exception e) {
            Log.e(TAG, "Error reading part data", e);
            return null;
        } finally {
            try {
                if (inputStream != null) inputStream.close();
                if (outputStream != null) outputStream.close();
            } catch (Exception ignored) {}
        }
    }
    @SuppressLint("NewApi")
    public static boolean updateMmsPart(ContentResolver contentResolver, String partId, String newText, byte[] newData, String contentType) {
//        Uri partUri = Uri.parse("content://mms/part/" + partId);
         Uri partUri = Telephony.Mms.Part.getPartUriForMessage("19");
        ContentValues values = new ContentValues();

        if (contentType.equals("text/plain")) {
            // Update text content
            values.put("text", newText);
        } else {
            // Update binary content (e.g., image/video)
            try {
                contentResolver.openOutputStream(partUri).write(newData);
                return true; // Assume success if no exception occurs
            } catch (Exception e) {
                Log.e(TAG, "Error updating part data", e);e.printStackTrace();
                return false;
            }
        }

        String whear = Telephony.Mms.Part.TEXT + "=?";
        String[] selectionArgs = {Telephony.Mms.Part.TEXT};
        // Perform the update
        int rowsUpdated = contentResolver.update(partUri, values, null, null);
        return rowsUpdated > 0;
    }

    public static boolean insertMmsPart(Context context, String mmsId, String contentType, String textData, byte[] binaryData) {
        ContentResolver contentResolver = context.getContentResolver();
        Uri uri = Uri.parse("content://mms/part");
        ContentValues values = new ContentValues();

        // Set metadata for the part
        values.put("mid", mmsId); // Associate the part with the MMS message
        values.put("ct", contentType); // Content type (e.g., "text/plain", "image/jpeg")
        values.put("chset", 106); // Charset (optional, 106 for UTF-8)

        // For text parts, include the text directly
        if ("text/plain".equals(contentType)) {
            values.put("text", textData);
        }

        // Insert the part metadata
        Uri partUri = contentResolver.insert(uri, values);

        if (partUri != null) {
            // For binary parts, write the data
            if (!"text/plain".equals(contentType) && binaryData != null) {
                try {
                    OutputStream outputStream = contentResolver.openOutputStream(partUri);
                    if (outputStream != null) {
                        outputStream.write(binaryData);
                        outputStream.close();
                    }
                    return true; // Success
                } catch (Exception e) {
                    Log.e(TAG, "Error inserting part data", e);
                    return false;
                }
            }
            return true; // Success for text parts
        }
        return false; // Failed to insert part
    }

    public static boolean deleteMmsPart(ContentResolver contentResolver, String partId) {
        // Build the URI for the specific part
        Uri partUri = Uri.parse("content://mms/part/" + partId);

        try {
            // Delete the part
            int rowsDeleted = contentResolver.delete(partUri, null, null);
            return rowsDeleted > 0; // Return true if at least one row was deleted
        } catch (Exception e) {
            Log.e(TAG, "Error deleting part", e);
            return false; // Handle failure
        }
    }

}
