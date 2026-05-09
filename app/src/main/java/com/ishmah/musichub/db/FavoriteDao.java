package com.ishmah.musichub.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FavoriteDao {

    private final SQLiteDatabase db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public FavoriteDao(Context context) {
        db = new DatabaseHelper(context).getWritableDatabase();
    }

    // Tambah favorite — background thread
    public void addFavorite(String trackId, String name, String artist,
                            String albumArt, String duration, Runnable onDone) {
        executor.execute(() -> {
            ContentValues values = new ContentValues();
            values.put(DatabaseHelper.COL_TRACK_ID, trackId);
            values.put(DatabaseHelper.COL_NAME, name);
            values.put(DatabaseHelper.COL_ARTIST, artist);
            values.put(DatabaseHelper.COL_ALBUM_ART, albumArt);
            values.put(DatabaseHelper.COL_DURATION, duration);
            db.insertWithOnConflict(DatabaseHelper.TABLE_FAVORITES, null,
                    values, SQLiteDatabase.CONFLICT_REPLACE);
            if (onDone != null) onDone.run();
        });
    }

    // Hapus favorite — background thread
    public void removeFavorite(String trackId, Runnable onDone) {
        executor.execute(() -> {
            db.delete(DatabaseHelper.TABLE_FAVORITES,
                    DatabaseHelper.COL_TRACK_ID + " = ?",
                    new String[]{trackId});
            if (onDone != null) onDone.run();
        });
    }

    // Cek apakah sudah favorite
    public boolean isFavorite(String trackId) {
        Cursor cursor = db.query(DatabaseHelper.TABLE_FAVORITES,
                new String[]{DatabaseHelper.COL_TRACK_ID},
                DatabaseHelper.COL_TRACK_ID + " = ?",
                new String[]{trackId},
                null, null, null);
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    // Ambil semua favorites
    public List<Map<String, String>> getAllFavorites() {
        List<Map<String, String>> list = new ArrayList<>();
        Cursor cursor = db.query(DatabaseHelper.TABLE_FAVORITES,
                null, null, null, null, null, null);
        while (cursor.moveToNext()) {
            Map<String, String> map = new HashMap<>();
            map.put("trackId", cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TRACK_ID)));
            map.put("name", cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NAME)));
            map.put("artist", cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ARTIST)));
            map.put("albumArt", cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ALBUM_ART)));
            map.put("duration", cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_DURATION)));
            list.add(map);
        }
        cursor.close();
        return list;
    }
}