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

public class ArtistDao {

    private final SQLiteDatabase db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public ArtistDao(Context context) {
        db = new DatabaseHelper(context).getWritableDatabase();
    }

    public void followArtist(String artistId, String artistName,
                             String artistPhoto, String genre, Runnable onDone) {
        executor.execute(() -> {
            ContentValues values = new ContentValues();
            values.put(DatabaseHelper.COL_ARTIST_ID, artistId);
            values.put(DatabaseHelper.COL_ARTIST_NAME, artistName);
            values.put(DatabaseHelper.COL_ARTIST_PHOTO, artistPhoto);
            values.put(DatabaseHelper.COL_GENRE, genre);
            values.put(DatabaseHelper.COL_FOLLOWED_AT,
                    String.valueOf(System.currentTimeMillis()));
            db.insertWithOnConflict(DatabaseHelper.TABLE_FOLLOWING,
                    null, values, SQLiteDatabase.CONFLICT_REPLACE);
            if (onDone != null) onDone.run();
        });
    }

    public void unfollowArtist(String artistId, Runnable onDone) {
        executor.execute(() -> {
            db.delete(DatabaseHelper.TABLE_FOLLOWING,
                    DatabaseHelper.COL_ARTIST_ID + " = ?",
                    new String[]{artistId});
            if (onDone != null) onDone.run();
        });
    }

    public boolean isFollowing(String artistId) {
        Cursor cursor = db.query(DatabaseHelper.TABLE_FOLLOWING,
                new String[]{DatabaseHelper.COL_ARTIST_ID},
                DatabaseHelper.COL_ARTIST_ID + " = ?",
                new String[]{artistId},
                null, null, null);
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    public List<Map<String, String>> getAllFollowing() {
        List<Map<String, String>> list = new ArrayList<>();
        Cursor cursor = db.query(DatabaseHelper.TABLE_FOLLOWING,
                null, null, null, null, null,
                DatabaseHelper.COL_FOLLOWED_AT + " DESC");
        while (cursor.moveToNext()) {
            Map<String, String> map = new HashMap<>();
            map.put("artist_id", cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ARTIST_ID)));
            map.put("artist_name", cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ARTIST_NAME)));
            map.put("artist_photo", cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ARTIST_PHOTO)));
            map.put("genre", cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_GENRE)));
            map.put("followed_at", cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_FOLLOWED_AT)));
            list.add(map);
        }
        cursor.close();
        return list;
    }

    public int getFollowingCount() {
        Cursor cursor = db.query(DatabaseHelper.TABLE_FOLLOWING,
                new String[]{DatabaseHelper.COL_ARTIST_ID},
                null, null, null, null, null);
        int count = cursor.getCount();
        cursor.close();
        return count;
    }
}