package com.ishmah.musichub.db

import android.content.ContentValues
import android.content.Context
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class ArtistDao(context: Context) {

    private val db = DatabaseHelper(context).writableDatabase
    private val executor: Executor = Executors.newSingleThreadExecutor()

    // Follow artist
    fun followArtist(
        artistId: String,
        artistName: String,
        artistPhoto: String,
        genre: String,
        onDone: (() -> Unit)? = null
    ) {
        executor.execute {
            val values = ContentValues().apply {
                put(DatabaseHelper.COL_ARTIST_ID, artistId)
                put(DatabaseHelper.COL_ARTIST_NAME, artistName)
                put(DatabaseHelper.COL_ARTIST_PHOTO, artistPhoto)
                put(DatabaseHelper.COL_GENRE, genre)
                put(DatabaseHelper.COL_FOLLOWED_AT, System.currentTimeMillis().toString())
            }
            db.insertWithOnConflict(
                DatabaseHelper.TABLE_FOLLOWING,
                null,
                values,
                android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
            )
            onDone?.invoke()
        }
    }

    // Unfollow artist
    fun unfollowArtist(artistId: String, onDone: (() -> Unit)? = null) {
        executor.execute {
            db.delete(
                DatabaseHelper.TABLE_FOLLOWING,
                "${DatabaseHelper.COL_ARTIST_ID} = ?",
                arrayOf(artistId)
            )
            onDone?.invoke()
        }
    }

    // Cek apakah sudah follow
    fun isFollowing(artistId: String): Boolean {
        val cursor = db.query(
            DatabaseHelper.TABLE_FOLLOWING,
            arrayOf(DatabaseHelper.COL_ARTIST_ID),
            "${DatabaseHelper.COL_ARTIST_ID} = ?",
            arrayOf(artistId),
            null, null, null
        )
        val exists = cursor.count > 0
        cursor.close()
        return exists
    }

    // Ambil semua following artists
    fun getAllFollowing(): List<Map<String, String>> {
        val list = mutableListOf<Map<String, String>>()
        val cursor = db.query(
            DatabaseHelper.TABLE_FOLLOWING,
            null, null, null, null, null,
            "${DatabaseHelper.COL_FOLLOWED_AT} DESC"
        )
        while (cursor.moveToNext()) {
            list.add(mapOf(
                "artist_id" to cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ARTIST_ID)),
                "artist_name" to cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ARTIST_NAME)),
                "artist_photo" to cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ARTIST_PHOTO)),
                "genre" to cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_GENRE)),
                "followed_at" to cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_FOLLOWED_AT))
            ))
        }
        cursor.close()
        return list
    }

    // Hitung jumlah following — untuk Profile screen (REAL data bukan dummy!)
    fun getFollowingCount(): Int {
        val cursor = db.query(
            DatabaseHelper.TABLE_FOLLOWING,
            arrayOf(DatabaseHelper.COL_ARTIST_ID),
            null, null, null, null, null
        )
        val count = cursor.count
        cursor.close()
        return count
    }
}