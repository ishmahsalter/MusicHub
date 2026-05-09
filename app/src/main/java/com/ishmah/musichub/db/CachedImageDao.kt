package com.ishmah.musichub.db

import android.content.ContentValues
import android.content.Context
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class CachedImageDao(context: Context) {

    private val db = DatabaseHelper(context).writableDatabase
    private val executor: Executor = Executors.newSingleThreadExecutor()

    // Simpan cache gambar
    fun saveCache(
        queryKey: String,
        coverUrl: String,
        artistPhoto: String,
        onDone: (() -> Unit)? = null
    ) {
        executor.execute {
            val values = ContentValues().apply {
                put(DatabaseHelper.COL_QUERY_KEY, queryKey)
                put(DatabaseHelper.COL_COVER_URL, coverUrl)
                put(DatabaseHelper.COL_ARTIST_PHOTO_URL, artistPhoto)
                put(DatabaseHelper.COL_CACHED_AT, System.currentTimeMillis().toString())
            }
            db.insertWithOnConflict(
                DatabaseHelper.TABLE_CACHED_IMAGES,
                null,
                values,
                android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
            )
            onDone?.invoke()
        }
    }

    // Ambil cache berdasarkan query key
    fun getCache(queryKey: String): Map<String, String>? {
        val cursor = db.query(
            DatabaseHelper.TABLE_CACHED_IMAGES,
            null,
            "${DatabaseHelper.COL_QUERY_KEY} = ?",
            arrayOf(queryKey),
            null, null, null
        )
        if (!cursor.moveToFirst()) {
            cursor.close()
            return null
        }
        val cache = mapOf(
            "query_key" to cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_QUERY_KEY)),
            "cover_url" to cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_COVER_URL)),
            "artist_photo" to cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ARTIST_PHOTO_URL)),
            "cached_at" to cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_CACHED_AT))
        )
        cursor.close()
        return cache
    }

    // Hapus cache yang sudah lama (lebih dari 7 hari)
    fun clearOldCache(onDone: (() -> Unit)? = null) {
        executor.execute {
            val sevenDaysAgo = (System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000).toString()
            db.delete(
                DatabaseHelper.TABLE_CACHED_IMAGES,
                "${DatabaseHelper.COL_CACHED_AT} < ?",
                arrayOf(sevenDaysAgo)
            )
            onDone?.invoke()
        }
    }

    // Hapus semua cache
    fun clearAllCache(onDone: (() -> Unit)? = null) {
        executor.execute {
            db.delete(DatabaseHelper.TABLE_CACHED_IMAGES, null, null)
            onDone?.invoke()
        }
    }
}