package com.ishmah.musichub.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "musichub.db";
    public static final int DATABASE_VERSION = 1;

    // TABLE: favorites
    public static final String TABLE_FAVORITES = "favorites";
    public static final String COL_TRACK_ID = "trackId";
    public static final String COL_NAME = "name";
    public static final String COL_ARTIST = "artist";
    public static final String COL_ALBUM_ART = "albumArt";
    public static final String COL_DURATION = "duration";

    // TABLE: playlists
    public static final String TABLE_PLAYLISTS = "playlists";
    public static final String COL_PLAYLIST_ID = "playlist_id";
    public static final String COL_PLAYLIST_NAME = "playlist_name";
    public static final String COL_COVER_TYPE = "cover_type";
    public static final String COL_COVER_VALUE = "cover_value";
    public static final String COL_CREATED_AT = "created_at";

    // TABLE: playlist_tracks
    public static final String TABLE_PLAYLIST_TRACKS = "playlist_tracks";
    public static final String COL_PT_ID = "id";
    public static final String COL_PT_PLAYLIST_ID = "playlist_id";
    public static final String COL_PT_TRACK_ID = "track_id";

    // TABLE: following_artists
    public static final String TABLE_FOLLOWING = "following_artists";
    public static final String COL_ARTIST_ID = "artist_id";
    public static final String COL_ARTIST_NAME = "artist_name";
    public static final String COL_ARTIST_PHOTO = "artist_photo";
    public static final String COL_GENRE = "genre";
    public static final String COL_FOLLOWED_AT = "followed_at";

    // TABLE: user_profile
    public static final String TABLE_USER_PROFILE = "user_profile";
    public static final String COL_USERNAME = "username";
    public static final String COL_BIO = "bio";
    public static final String COL_PHOTO_URI = "photo_uri";
    public static final String COL_THEME = "theme";
    public static final String COL_NOTIF_ENABLED = "notif_enabled";

    // TABLE: cached_images
    public static final String TABLE_CACHED_IMAGES = "cached_images";
    public static final String COL_QUERY_KEY = "query_key";
    public static final String COL_COVER_URL = "cover_url";
    public static final String COL_ARTIST_PHOTO_URL = "artist_photo";
    public static final String COL_CACHED_AT = "cached_at";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // favorites
        db.execSQL("CREATE TABLE " + TABLE_FAVORITES + " (" +
                COL_TRACK_ID + " TEXT PRIMARY KEY, " +
                COL_NAME + " TEXT, " +
                COL_ARTIST + " TEXT, " +
                COL_ALBUM_ART + " TEXT, " +
                COL_DURATION + " TEXT)");

        // playlists
        db.execSQL("CREATE TABLE " + TABLE_PLAYLISTS + " (" +
                COL_PLAYLIST_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_PLAYLIST_NAME + " TEXT, " +
                COL_COVER_TYPE + " TEXT, " +
                COL_COVER_VALUE + " TEXT, " +
                COL_CREATED_AT + " TEXT)");

        // playlist_tracks
        db.execSQL("CREATE TABLE " + TABLE_PLAYLIST_TRACKS + " (" +
                COL_PT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_PT_PLAYLIST_ID + " INTEGER, " +
                COL_PT_TRACK_ID + " TEXT)");

        // following_artists
        db.execSQL("CREATE TABLE " + TABLE_FOLLOWING + " (" +
                COL_ARTIST_ID + " TEXT PRIMARY KEY, " +
                COL_ARTIST_NAME + " TEXT, " +
                COL_ARTIST_PHOTO + " TEXT, " +
                COL_GENRE + " TEXT, " +
                COL_FOLLOWED_AT + " TEXT)");

        // user_profile
        db.execSQL("CREATE TABLE " + TABLE_USER_PROFILE + " (" +
                COL_USERNAME + " TEXT, " +
                COL_BIO + " TEXT, " +
                COL_PHOTO_URI + " TEXT, " +
                COL_THEME + " TEXT, " +
                COL_NOTIF_ENABLED + " INTEGER DEFAULT 1)");

        // cached_images
        db.execSQL("CREATE TABLE " + TABLE_CACHED_IMAGES + " (" +
                COL_QUERY_KEY + " TEXT PRIMARY KEY, " +
                COL_COVER_URL + " TEXT, " +
                COL_ARTIST_PHOTO_URL + " TEXT, " +
                COL_CACHED_AT + " TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FAVORITES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PLAYLISTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PLAYLIST_TRACKS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FOLLOWING);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USER_PROFILE);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CACHED_IMAGES);
        onCreate(db);
    }
}