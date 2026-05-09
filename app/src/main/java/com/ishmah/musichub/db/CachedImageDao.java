package com.ishmah.musichub.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CachedImageDao {

    private final SQLiteDatabase db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public CachedImageDao(Context context) {
        db = new DatabaseHelper(context).getWritableDatabase();
    }

    public void saveCache(String queryKey, String coverUrl,
                          String artistPhoto, Runnable onDone) {
        executor.execute(() -> {
            ContentValues values = new ContentValues();
            values.put(DatabaseHelper.COL_QUERY_KEY, queryKey);
            values.put(DatabaseHelper.COL_COVER_URL, coverUrl);
            values.put(DatabaseHelper.COL_ARTIST_PHOTO_URL, artistPhoto);
            values.put(DatabaseHelper.COL_CACHED_AT,
                    String.valueOf(System.currentTimeMillis()));
            db.insertWithOnConflict(DatabaseHelper.TABLE_CACHED_IMAGES,
                    null, values, SQLiteDatabase.CONFLICT_REPLACE);
            if (onDone != null) onDone.run();
        });
    }

    public Map<String, String> getCache(String queryKey) {
        Cursor cursor = db.query(DatabaseHelper.TABLE_CACHED_IMAGES,
                null,
                DatabaseHelper.COL_QUERY_KEY + " = ?",
                new String[]{queryKey},
                null, null, null);
        if (!cursor.moveToFirst()) {
            cursor.close();
            return null;
        }
        Map<String, String> cache = new HashMap<>();
        cache.put("query_key", cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_QUERY_KEY)));
        cache.put("cover_url", cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_COVER_URL)));
        cache.put("artist_photo", cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ARTIST_PHOTO_URL)));
        cache.put("cached_at", cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CACHED_AT)));
        cursor.close();
        return cache;
    }

    public void clearOldCache(Runnable onDone) {
        executor.execute(() -> {
            long sevenDaysAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L;
            db.delete(DatabaseHelper.TABLE_CACHED_IMAGES,
                    DatabaseHelper.COL_CACHED_AT + " < ?",
                    new String[]{String.valueOf(sevenDaysAgo)});
            if (onDone != null) onDone.run();
        });
    }

    public void clearAllCache(Runnable onDone) {
        executor.execute(() -> {
            db.delete(DatabaseHelper.TABLE_CACHED_IMAGES, null, null);
            if (onDone != null) onDone.run();
        });
    }
}