package com.ishmah.musichub.db

import android.content.ContentValues
import android.content.Context
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class PlaylistDao(context: Context) {

    private val db = DatabaseHelper(context).writableDatabase
    private val executor: Executor = Executors.newSingleThreadExecutor()

    // ══ PLAYLIST CRUD ══

    // Buat playlist baru
    fun createPlaylist(
        name: String,
        coverType: String = "gradient",
        coverValue: String = "purple",
        onDone: ((Long) -> Unit)? = null
    ) {
        executor.execute {
            val values = ContentValues().apply {
                put(DatabaseHelper.COL_PLAYLIST_NAME, name)
                put(DatabaseHelper.COL_COVER_TYPE, coverType)
                put(DatabaseHelper.COL_COVER_VALUE, coverValue)
                put(DatabaseHelper.COL_CREATED_AT, System.currentTimeMillis().toString())
            }
            val id = db.insert(DatabaseHelper.TABLE_PLAYLISTS, null, values)
            onDone?.invoke(id)
        }
    }

    // Ambil semua playlist
    fun getAllPlaylists(): List<Map<String, String>> {
        val list = mutableListOf<Map<String, String>>()
        val cursor = db.query(
            DatabaseHelper.TABLE_PLAYLISTS,
            null, null, null, null, null,
            "${DatabaseHelper.COL_CREATED_AT} DESC"
        )
        while (cursor.moveToNext()) {
            list.add(mapOf(
                "playlist_id" to cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PLAYLIST_ID)),
                "name" to cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PLAYLIST_NAME)),
                "cover_type" to cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_COVER_TYPE)),
                "cover_value" to cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_COVER_VALUE)),
                "created_at" to cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CREATED_AT))
            ))
        }
        cursor.close()
        return list
    }

    // Update cover playlist
    fun updatePlaylistCover(
        playlistId: Int,
        coverType: String,
        coverValue: String,
        onDone: (() -> Unit)? = null
    ) {
        executor.execute {
            val values = ContentValues().apply {
                put(DatabaseHelper.COL_COVER_TYPE, coverType)
                put(DatabaseHelper.COL_COVER_VALUE, coverValue)
            }
            db.update(
                DatabaseHelper.TABLE_PLAYLISTS,
                values,
                "${DatabaseHelper.COL_PLAYLIST_ID} = ?",
                arrayOf(playlistId.toString())
            )
            onDone?.invoke()
        }
    }

    // Hapus playlist
    fun deletePlaylist(playlistId: Int, onDone: (() -> Unit)? = null) {
        executor.execute {
            db.delete(
                DatabaseHelper.TABLE_PLAYLISTS,
                "${DatabaseHelper.COL_PLAYLIST_ID} = ?",
                arrayOf(playlistId.toString())
            )
            // Hapus juga semua track di playlist ini
            db.delete(
                DatabaseHelper.TABLE_PLAYLIST_TRACKS,
                "${DatabaseHelper.COL_PT_PLAYLIST_ID} = ?",
                arrayOf(playlistId.toString())
            )
            onDone?.invoke()
        }
    }

    // ══ PLAYLIST TRACKS ══

    // Tambah track ke playlist
    fun addTrackToPlaylist(
        playlistId: Int,
        trackId: String,
        onDone: (() -> Unit)? = null
    ) {
        executor.execute {
            val values = ContentValues().apply {
                put(DatabaseHelper.COL_PT_PLAYLIST_ID, playlistId)
                put(DatabaseHelper.COL_PT_TRACK_ID, trackId)
            }
            db.insertWithOnConflict(
                DatabaseHelper.TABLE_PLAYLIST_TRACKS,
                null,
                values,
                android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE
            )
            onDone?.invoke()
        }
    }

    // Hapus track dari playlist
    fun removeTrackFromPlaylist(
        playlistId: Int,
        trackId: String,
        onDone: (() -> Unit)? = null
    ) {
        executor.execute {
            db.delete(
                DatabaseHelper.TABLE_PLAYLIST_TRACKS,
                "${DatabaseHelper.COL_PT_PLAYLIST_ID} = ? AND ${DatabaseHelper.COL_PT_TRACK_ID} = ?",
                arrayOf(playlistId.toString(), trackId)
            )
            onDone?.invoke()
        }
    }

    // Ambil semua track ID di playlist tertentu
    fun getTrackIdsInPlaylist(playlistId: Int): List<String> {
        val list = mutableListOf<String>()
        val cursor = db.query(
            DatabaseHelper.TABLE_PLAYLIST_TRACKS,
            arrayOf(DatabaseHelper.COL_PT_TRACK_ID),
            "${DatabaseHelper.COL_PT_PLAYLIST_ID} = ?",
            arrayOf(playlistId.toString()),
            null, null, null
        )
        while (cursor.moveToNext()) {
            list.add(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PT_TRACK_ID)))
        }
        cursor.close()
        return list
    }

    // Hitung jumlah track di playlist
    fun getTrackCount(playlistId: Int): Int {
        val cursor = db.query(
            DatabaseHelper.TABLE_PLAYLIST_TRACKS,
            arrayOf(DatabaseHelper.COL_PT_ID),
            "${DatabaseHelper.COL_PT_PLAYLIST_ID} = ?",
            arrayOf(playlistId.toString()),
            null, null, null
        )
        val count = cursor.count
        cursor.close()
        return count
    }
}