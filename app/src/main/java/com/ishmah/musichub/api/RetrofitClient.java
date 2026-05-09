package com.ishmah.musichub.api;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    private static final String LASTFM_BASE_URL = "https://ws.audioscrobbler.com/2.0/";
    private static final String DEEZER_BASE_URL = "https://api.deezer.com/";

    private static Retrofit lastFmRetrofit;
    private static Retrofit deezerRetrofit;

    public static Retrofit getLastFmInstance() {
        if (lastFmRetrofit == null) {
            lastFmRetrofit = new Retrofit.Builder()
                    .baseUrl(LASTFM_BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return lastFmRetrofit;
    }

    public static Retrofit getDeezerInstance() {
        if (deezerRetrofit == null) {
            deezerRetrofit = new Retrofit.Builder()
                    .baseUrl(DEEZER_BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return deezerRetrofit;
    }
}