package com.ishmah.musichub.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "musichub.db"
        const val DATABASE_VERSION = 1

        // TABLE: favorites
        const val TABLE_FAVORITES = "favorites"
        const val COL_TRACK_ID = "trackId"
        const val COL_NAME = "name"
        const val COL_ARTIST = "artist"
        const val COL_ALBUM_ART = "albumArt"
        const val COL_DURATION = "duration"

        // TABLE: playlists
        const val TABLE_PLAYLISTS = "playlists"
        const val COL_PLAYLIST_ID = "playlist_id"
        const val COL_PLAYLIST_NAME = "name"
        const val COL_COVER_TYPE = "cover_type"
        const val COL_COVER_VALUE = "cover_value"
        const val COL_CREATED_AT = "created_at"

        // TABLE: playlist_tracks
        const val TABLE_PLAYLIST_TRACKS = "playlist_tracks"
        const val COL_PT_ID = "id"
        const val COL_PT_PLAYLIST_ID = "playlist_id"
        const val COL_PT_TRACK_ID = "track_id"

        // TABLE: following_artists
        const val TABLE_FOLLOWING = "following_artists"
        const val COL_ARTIST_ID = "artist_id"
        const val COL_ARTIST_NAME = "artist_name"
        const val COL_ARTIST_PHOTO = "artist_photo"
        const val COL_GENRE = "genre"
        const val COL_FOLLOWED_AT = "followed_at"

        // TABLE: user_profile
        const val TABLE_USER_PROFILE = "user_profile"
        const val COL_USERNAME = "username"
        const val COL_BIO = "bio"
        const val COL_PHOTO_URI = "photo_uri"
        const val COL_THEME = "theme"
        const val COL_NOTIF_ENABLED = "notif_enabled"

        // TABLE: cached_images
        const val TABLE_CACHED_IMAGES = "cached_images"
        const val COL_QUERY_KEY = "query_key"
        const val COL_COVER_URL = "cover_url"
        const val COL_ARTIST_PHOTO_URL = "artist_photo"
        const val COL_CACHED_AT = "cached_at"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // favorites
        db.execSQL("""
            CREATE TABLE $TABLE_FAVORITES (
                $COL_TRACK_ID TEXT PRIMARY KEY,
                $COL_NAME TEXT,
                $COL_ARTIST TEXT,
                $COL_ALBUM_ART TEXT,
                $COL_DURATION TEXT
            )
        """.trimIndent())

        // playlists
        db.execSQL("""
            CREATE TABLE $TABLE_PLAYLISTS (
                $COL_PLAYLIST_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_PLAYLIST_NAME TEXT,
                $COL_COVER_TYPE TEXT,
                $COL_COVER_VALUE TEXT,
                $COL_CREATED_AT TEXT
            )
        """.trimIndent())

        // playlist_tracks
        db.execSQL("""
            CREATE TABLE $TABLE_PLAYLIST_TRACKS (
                $COL_PT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_PT_PLAYLIST_ID INTEGER,
                $COL_PT_TRACK_ID TEXT
            )
        """.trimIndent())

        // following_artists
        db.execSQL("""
            CREATE TABLE $TABLE_FOLLOWING (
                $COL_ARTIST_ID TEXT PRIMARY KEY,
                $COL_ARTIST_NAME TEXT,
                $COL_ARTIST_PHOTO TEXT,
                $COL_GENRE TEXT,
                $COL_FOLLOWED_AT TEXT
            )
        """.trimIndent())

        // user_profile
        db.execSQL("""
            CREATE TABLE $TABLE_USER_PROFILE (
                $COL_USERNAME TEXT,
                $COL_BIO TEXT,
                $COL_PHOTO_URI TEXT,
                $COL_THEME TEXT,
                $COL_NOTIF_ENABLED INTEGER DEFAULT 1
            )
        """.trimIndent())

        // cached_images
        db.execSQL("""
            CREATE TABLE $TABLE_CACHED_IMAGES (
                $COL_QUERY_KEY TEXT PRIMARY KEY,
                $COL_COVER_URL TEXT,
                $COL_ARTIST_PHOTO_URL TEXT,
                $COL_CACHED_AT TEXT
            )
        """.trimIndent())
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_FAVORITES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PLAYLISTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PLAYLIST_TRACKS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_FOLLOWING")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USER_PROFILE")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CACHED_IMAGES")
        onCreate(db)
    }
}