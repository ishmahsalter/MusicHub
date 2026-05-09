package com.ishmah.musichub.db

import android.content.ContentValues
import android.content.Context
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class FavoriteDao(context: Context) {

    private val db = DatabaseHelper(context).writableDatabase
    private val executor: Executor = Executors.newSingleThreadExecutor()

    // Insert favorite — jalan di background thread
    fun addFavorite(
        trackId: String,
        name: String,
        artist: String,
        albumArt: String,
        duration: String,
        onDone: (() -> Unit)? = null
    ) {
        executor.execute {
            val values = ContentValues().apply {
                put(DatabaseHelper.COL_TRACK_ID, trackId)
                put(DatabaseHelper.COL_NAME, name)
                put(DatabaseHelper.COL_ARTIST, artist)
                put(DatabaseHelper.COL_ALBUM_ART, albumArt)
                put(DatabaseHelper.COL_DURATION, duration)
            }
            db.insertWithOnConflict(
                DatabaseHelper.TABLE_FAVORITES,
                null,
                values,
                android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
            )
            onDone?.invoke()
        }
    }

    // Delete favorite — jalan di background thread
    fun removeFavorite(trackId: String, onDone: (() -> Unit)? = null) {
        executor.execute {
            db.delete(
                DatabaseHelper.TABLE_FAVORITES,
                "${DatabaseHelper.COL_TRACK_ID} = ?",
                arrayOf(trackId)
            )
            onDone?.invoke()
        }
    }

    // Cek apakah track sudah di-favorite
    fun isFavorite(trackId: String): Boolean {
        val cursor = db.query(
            DatabaseHelper.TABLE_FAVORITES,
            arrayOf(DatabaseHelper.COL_TRACK_ID),
            "${DatabaseHelper.COL_TRACK_ID} = ?",
            arrayOf(trackId),
            null, null, null
        )
        val exists = cursor.count > 0
        cursor.close()
        return exists
    }

    // Ambil semua favorites
    fun getAllFavorites(): List<Map<String, String>> {
        val list = mutableListOf<Map<String, String>>()
        val cursor = db.query(
            DatabaseHelper.TABLE_FAVORITES,
            null, null, null, null, null,
            "${DatabaseHelper.COL_TRACK_ID} DESC"
        )
        while (cursor.moveToNext()) {
            list.add(mapOf(
                "trackId" to cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_TRACK_ID)),
                "name" to cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NAME)),
                "artist" to cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ARTIST)),
                "albumArt" to cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ALBUM_ART)),
                "duration" to cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_DURATION))
            ))
        }
        cursor.close()
        return list
    }
}