package com.ishmah.musichub.api;

import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface DeezerApi {

    // Search album cover
    @GET("search/track")
    Call<JsonObject> searchTrack(
            @Query("q") String query
    );

    // Search artist photo
    @GET("search/artist")
    Call<JsonObject> searchArtist(
            @Query("q") String query
    );

    // Get album by ID
    @GET("album/{id}")
    Call<JsonObject> getAlbum(
            @Path("id") String albumId
    );
}