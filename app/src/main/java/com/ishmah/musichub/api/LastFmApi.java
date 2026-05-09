package com.ishmah.musichub.api;

import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface LastFmApi {

    // Top Trending Tracks
    @GET("?method=chart.gettoptracks&format=json")
    Call<JsonObject> getTopTracks(
            @Query("api_key") String apiKey,
            @Query("limit") int limit
    );

    // Track Info
    @GET("?method=track.getinfo&format=json")
    Call<JsonObject> getTrackInfo(
            @Query("api_key") String apiKey,
            @Query("track") String track,
            @Query("artist") String artist
    );

    // Artist Info
    @GET("?method=artist.getinfo&format=json")
    Call<JsonObject> getArtistInfo(
            @Query("api_key") String apiKey,
            @Query("artist") String artist
    );

    // Search Track
    @GET("?method=track.search&format=json")
    Call<JsonObject> searchTrack(
            @Query("api_key") String apiKey,
            @Query("track") String track,
            @Query("limit") int limit
    );

    // Artist Top Albums
    @GET("?method=artist.gettopalbums&format=json")
    Call<JsonObject> getArtistTopAlbums(
            @Query("api_key") String apiKey,
            @Query("artist") String artist,
            @Query("limit") int limit
    );
}