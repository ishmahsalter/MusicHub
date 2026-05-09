package com.ishmah.musichub.db

import android.content.ContentValues
import android.content.Context
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class UserProfileDao(context: Context) {

    private val db = DatabaseHelper(context).writableDatabase
    private val executor: Executor = Executors.newSingleThreadExecutor()

    // Simpan / update profile
    fun saveProfile(
        username: String,
        bio: String,
        photoUri: String,
        theme: String,
        notifEnabled: Boolean,
        onDone: (() -> Unit)? = null
    ) {
        executor.execute {
            // Cek apakah sudah ada data profile
            val cursor = db.query(
                DatabaseHelper.TABLE_USER_PROFILE,
                null, null, null, null, null, null
            )
            val exists = cursor.count > 0
            cursor.close()

            val values = ContentValues().apply {
                put(DatabaseHelper.COL_USERNAME, username)
                put(DatabaseHelper.COL_BIO, bio)
                put(DatabaseHelper.COL_PHOTO_URI, photoUri)
                put(DatabaseHelper.COL_THEME, theme)
                put(DatabaseHelper.COL_NOTIF_ENABLED, if (notifEnabled) 1 else 0)
            }

            if (exists) {
                // Update kalau sudah ada
                db.update(DatabaseHelper.TABLE_USER_PROFILE, values, null, null)
            } else {
                // Insert kalau belum ada
                db.insert(DatabaseHelper.TABLE_USER_PROFILE, null, values)
            }
            onDone?.invoke()
        }
    }

    // Ambil profile
    fun getProfile(): Map<String, String>? {
        val cursor = db.query(
            DatabaseHelper.TABLE_USER_PROFILE,
            null, null, null, null, null, null
        )
        if (!cursor.moveToFirst()) {
            cursor.close()
            return null
        }
        val profile = mapOf(
            "username" to (cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USERNAME)) ?: ""),
            "bio" to (cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_BIO)) ?: ""),
            "photo_uri" to (cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_PHOTO_URI)) ?: ""),
            "theme" to (cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_THEME)) ?: "dark"),
            "notif_enabled" to cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NOTIF_ENABLED))
        )
        cursor.close()
        return profile
    }

    // Update username saja
    fun updateUsername(username: String, onDone: (() -> Unit)? = null) {
        executor.execute {
            val values = ContentValues().apply {
                put(DatabaseHelper.COL_USERNAME, username)
            }
            db.update(DatabaseHelper.TABLE_USER_PROFILE, values, null, null)
            onDone?.invoke()
        }
    }

    // Update foto profil saja
    fun updatePhoto(photoUri: String, onDone: (() -> Unit)? = null) {
        executor.execute {
            val values = ContentValues().apply {
                put(DatabaseHelper.COL_PHOTO_URI, photoUri)
            }
            db.update(DatabaseHelper.TABLE_USER_PROFILE, values, null, null)
            onDone?.invoke()
        }
    }

    // Update theme saja
    fun updateTheme(theme: String, onDone: (() -> Unit)? = null) {
        executor.execute {
            val values = ContentValues().apply {
                put(DatabaseHelper.COL_THEME, theme)
            }
            db.update(DatabaseHelper.TABLE_USER_PROFILE, values, null, null)
            onDone?.invoke()
        }
    }
}