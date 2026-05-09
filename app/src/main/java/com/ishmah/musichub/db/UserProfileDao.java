package com.ishmah.musichub.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UserProfileDao {

    private final android.database.sqlite.SQLiteDatabase db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public UserProfileDao(Context context) {
        db = new DatabaseHelper(context).getWritableDatabase();
    }

    public void saveProfile(String username, String bio, String photoUri,
                            String theme, boolean notifEnabled, Runnable onDone) {
        executor.execute(() -> {
            Cursor cursor = db.query(DatabaseHelper.TABLE_USER_PROFILE,
                    null, null, null, null, null, null);
            boolean exists = cursor.getCount() > 0;
            cursor.close();

            ContentValues values = new ContentValues();
            values.put(DatabaseHelper.COL_USERNAME, username);
            values.put(DatabaseHelper.COL_BIO, bio);
            values.put(DatabaseHelper.COL_PHOTO_URI, photoUri);
            values.put(DatabaseHelper.COL_THEME, theme);
            values.put(DatabaseHelper.COL_NOTIF_ENABLED, notifEnabled ? 1 : 0);

            if (exists) {
                db.update(DatabaseHelper.TABLE_USER_PROFILE, values, null, null);
            } else {
                db.insert(DatabaseHelper.TABLE_USER_PROFILE, null, values);
            }
            if (onDone != null) onDone.run();
        });
    }

    public Map<String, String> getProfile() {
        Cursor cursor = db.query(DatabaseHelper.TABLE_USER_PROFILE,
                null, null, null, null, null, null);
        if (!cursor.moveToFirst()) {
            cursor.close();
            return null;
        }
        Map<String, String> profile = new HashMap<>();
        profile.put("username", cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USERNAME)));
        profile.put("bio", cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_BIO)));
        profile.put("photo_uri", cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PHOTO_URI)));
        profile.put("theme", cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_THEME)));
        profile.put("notif_enabled", cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NOTIF_ENABLED)));
        cursor.close();
        return profile;
    }

    public void updateUsername(String username, Runnable onDone) {
        executor.execute(() -> {
            ContentValues values = new ContentValues();
            values.put(DatabaseHelper.COL_USERNAME, username);
            db.update(DatabaseHelper.TABLE_USER_PROFILE, values, null, null);
            if (onDone != null) onDone.run();
        });
    }

    public void updatePhoto(String photoUri, Runnable onDone) {
        executor.execute(() -> {
            ContentValues values = new ContentValues();
            values.put(DatabaseHelper.COL_PHOTO_URI, photoUri);
            db.update(DatabaseHelper.TABLE_USER_PROFILE, values, null, null);
            if (onDone != null) onDone.run();
        });
    }

    public void updateTheme(String theme, Runnable onDone) {
        executor.execute(() -> {
            ContentValues values = new ContentValues();
            values.put(DatabaseHelper.COL_THEME, theme);
            db.update(DatabaseHelper.TABLE_USER_PROFILE, values, null, null);
            if (onDone != null) onDone.run();
        });
    }
}