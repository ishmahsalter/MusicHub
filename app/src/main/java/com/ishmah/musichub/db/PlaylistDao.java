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

public class PlaylistDao {

    private final SQLiteDatabase db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public PlaylistDao(Context context) {
        db = new DatabaseHelper(context).getWritableDatabase();
    }

    // Buat playlist baru
    public void createPlaylist(String name, String coverType,
                               String coverValue, Runnable onDone) {
        executor.execute(() -> {
            ContentValues values = new ContentValues();
            values.put(DatabaseHelper.COL_PLAYLIST_NAME, name);
            values.put(DatabaseHelper.COL_COVER_TYPE, coverType);
            values.put(DatabaseHelper.COL_COVER_VALUE, coverValue);
            values.put(DatabaseHelper.COL_CREATED_AT,
                    String.valueOf(System.currentTimeMillis()));
            db.insert(DatabaseHelper.TABLE_PLAYLISTS, null, values);
            if (onDone != null) onDone.run();
        });
    }

    // Ambil semua playlist
    public List<Map<String, String>> getAllPlaylists() {
        List<Map<String, String>> list = new ArrayList<>();
        Cursor cursor = db.query(DatabaseHelper.TABLE_PLAYLISTS,
                null, null, null, null, null,
                DatabaseHelper.COL_CREATED_AT + " DESC");
        while (cursor.moveToNext()) {
            Map<String, String> map = new HashMap<>();
            map.put("playlist_id", cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PLAYLIST_ID)));
            map.put("name", cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PLAYLIST_NAME)));
            map.put("cover_type", cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_COVER_TYPE)));
            map.put("cover_value", cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_COVER_VALUE)));
            map.put("created_at", cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CREATED_AT)));
            list.add(map);
        }
        cursor.close();
        return list;
    }

    // Update cover playlist
    public void updatePlaylistCover(int playlistId, String coverType,
                                    String coverValue, Runnable onDone) {
        executor.execute(() -> {
            ContentValues values = new ContentValues();
            values.put(DatabaseHelper.COL_COVER_TYPE, coverType);
            values.put(DatabaseHelper.COL_COVER_VALUE, coverValue);
            db.update(DatabaseHelper.TABLE_PLAYLISTS, values,
                    DatabaseHelper.COL_PLAYLIST_ID + " = ?",
                    new String[]{String.valueOf(playlistId)});
            if (onDone != null) onDone.run();
        });
    }

    // Hapus playlist
    public void deletePlaylist(int playlistId, Runnable onDone) {
        executor.execute(() -> {
            db.delete(DatabaseHelper.TABLE_PLAYLISTS,
                    DatabaseHelper.COL_PLAYLIST_ID + " = ?",
                    new String[]{String.valueOf(playlistId)});
            db.delete(DatabaseHelper.TABLE_PLAYLIST_TRACKS,
                    DatabaseHelper.COL_PT_PLAYLIST_ID + " = ?",
                    new String[]{String.valueOf(playlistId)});
            if (onDone != null) onDone.run();
        });
    }

    // Tambah track ke playlist
    public void addTrackToPlaylist(int playlistId, String trackId, Runnable onDone) {
        executor.execute(() -> {
            ContentValues values = new ContentValues();
            values.put(DatabaseHelper.COL_PT_PLAYLIST_ID, playlistId);
            values.put(DatabaseHelper.COL_PT_TRACK_ID, trackId);
            db.insertWithOnConflict(DatabaseHelper.TABLE_PLAYLIST_TRACKS,
                    null, values, SQLiteDatabase.CONFLICT_IGNORE);
            if (onDone != null) onDone.run();
        });
    }

    // Hapus track dari playlist
    public void removeTrackFromPlaylist(int playlistId, String trackId, Runnable onDone) {
        executor.execute(() -> {
            db.delete(DatabaseHelper.TABLE_PLAYLIST_TRACKS,
                    DatabaseHelper.COL_PT_PLAYLIST_ID + " = ? AND " +
                            DatabaseHelper.COL_PT_TRACK_ID + " = ?",
                    new String[]{String.valueOf(playlistId), trackId});
            if (onDone != null) onDone.run();
        });
    }

    // Ambil semua track ID di playlist
    public List<String> getTrackIdsInPlaylist(int playlistId) {
        List<String> list = new ArrayList<>();
        Cursor cursor = db.query(DatabaseHelper.TABLE_PLAYLIST_TRACKS,
                new String[]{DatabaseHelper.COL_PT_TRACK_ID},
                DatabaseHelper.COL_PT_PLAYLIST_ID + " = ?",
                new String[]{String.valueOf(playlistId)},
                null, null, null);
        while (cursor.moveToNext()) {
            list.add(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PT_TRACK_ID)));
        }
        cursor.close();
        return list;
    }

    // Hitung jumlah track di playlist
    public int getTrackCount(int playlistId) {
        Cursor cursor = db.query(DatabaseHelper.TABLE_PLAYLIST_TRACKS,
                new String[]{DatabaseHelper.COL_PT_ID},
                DatabaseHelper.COL_PT_PLAYLIST_ID + " = ?",
                new String[]{String.valueOf(playlistId)},
                null, null, null);
        int count = cursor.getCount();
        cursor.close();
        return count;
    }
}