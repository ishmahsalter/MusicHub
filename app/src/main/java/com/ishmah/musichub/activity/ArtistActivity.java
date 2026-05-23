package com.ishmah.musichub.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.ishmah.musichub.R;
import com.ishmah.musichub.ThemeHelper;
import com.ishmah.musichub.adapter.AlbumAdapter;
import com.ishmah.musichub.api.ApiConfig;
import com.ishmah.musichub.api.DeezerApi;
import com.ishmah.musichub.api.LastFmApi;
import com.ishmah.musichub.api.RetrofitClient;
import com.ishmah.musichub.db.ArtistDao;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ArtistActivity extends AppCompatActivity {

    private TextView tvArtistName, tvBio, tvFollowers, tvFollowing, tvStreams;
    private ImageView ivArtistPhoto;
    private ImageButton btnBack, btnFollow, btnShare;
    private RecyclerView rvDiscography;
    private ArtistDao artistDao;
    private LastFmApi lastFmApi;
    private DeezerApi deezerApi;
    private AlbumAdapter albumAdapter;
    private List<Map<String, String>> albums = new ArrayList<>();
    private String artistId, artistName, artistPhoto, genre;
    private boolean isFollowing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeHelper.apply(this);
        setContentView(R.layout.activity_artist);

        artistDao = new ArtistDao(this);
        lastFmApi = RetrofitClient.getLastFmInstance().create(LastFmApi.class);
        deezerApi = RetrofitClient.getDeezerInstance().create(DeezerApi.class);

        initViews();
        receiveIntent();
        setupClickListeners();

        if (artistName != null) loadArtistData();
    }

    private void initViews() {
        tvArtistName = findViewById(R.id.tv_artist_name);
        tvBio = findViewById(R.id.tv_bio);
        tvFollowers = findViewById(R.id.tv_followers);
        tvFollowing = findViewById(R.id.tv_following);
        tvStreams = findViewById(R.id.tv_streams);
        ivArtistPhoto = findViewById(R.id.iv_artist_photo);
        btnBack = findViewById(R.id.btn_back);
        btnFollow = findViewById(R.id.btn_follow);
        btnShare = findViewById(R.id.btn_share);
        rvDiscography = findViewById(R.id.rv_discography);

        albumAdapter = new AlbumAdapter(this, albums, album -> {
            Intent intent = new Intent(this, AlbumActivity.class);
            intent.putExtra("album_name", album.get("name"));
            intent.putExtra("artist_name", artistName);
            intent.putExtra("album_art", album.get("image") != null ? album.get("image") : "");
            startActivity(intent);
        });
        rvDiscography.setLayoutManager(new LinearLayoutManager(this));
        rvDiscography.setAdapter(albumAdapter);
    }

    private void receiveIntent() {
        Intent intent = getIntent();
        if (intent != null) {
            artistId = intent.getStringExtra("artistId");
            artistName = intent.getStringExtra("artistName");
            artistPhoto = intent.getStringExtra("artistPhoto");
            genre = intent.getStringExtra("genre");

            if (artistName != null) tvArtistName.setText(artistName);

            if (artistPhoto != null && !artistPhoto.isEmpty()) {
                Glide.with(this).load(artistPhoto)
                        .placeholder(R.color.bg_card)
                        .into(ivArtistPhoto);
            }

            if (artistId != null) {
                isFollowing = artistDao.isFollowing(artistId);
                updateFollowButton();
            }
        }
    }

    private void loadArtistData() {
        fetchArtistPhotoFromDeezer();
        fetchArtistInfoFromLastFm();
        fetchDiscographyFromLastFm();
    }

    private void fetchArtistPhotoFromDeezer() {
        if (artistPhoto != null && !artistPhoto.isEmpty()) return;

        deezerApi.searchArtist(artistName).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!response.isSuccessful() || response.body() == null) return;
                try {
                    JsonArray data = response.body().getAsJsonArray("data");
                    if (data != null && data.size() > 0) {
                        JsonObject first = data.get(0).getAsJsonObject();
                        String picUrl = first.get("picture_medium").getAsString();
                        if (picUrl != null && !picUrl.isEmpty()) {
                            runOnUiThread(() ->
                                    Glide.with(ArtistActivity.this)
                                            .load(picUrl)
                                            .placeholder(R.color.bg_card)
                                            .into(ivArtistPhoto));
                        }
                    }
                } catch (Exception ignored) {}
            }
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {}
        });
    }

    private void fetchArtistInfoFromLastFm() {
        lastFmApi.getArtistInfo(ApiConfig.LASTFM_API_KEY, artistName)
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call,
                                           Response<JsonObject> response) {
                        if (!response.isSuccessful() || response.body() == null) return;
                        try {
                            JsonObject artist = response.body().getAsJsonObject("artist");
                            if (artist == null) return;

                            JsonObject stats = artist.getAsJsonObject("stats");
                            String listeners = stats != null ?
                                    stats.get("listeners").getAsString() : "0";
                            String playcount = stats != null ?
                                    stats.get("playcount").getAsString() : "0";

                            JsonObject bio = artist.getAsJsonObject("bio");
                            String summary = bio != null ?
                                    bio.get("summary").getAsString() : "";
                            // Strip Last.fm HTML link at end
                            int linkIdx = summary.indexOf("<a href");
                            if (linkIdx > 0) summary = summary.substring(0, linkIdx).trim();

                            final String finalSummary = summary;
                            final String listenersFormatted = formatCount(Long.parseLong(listeners));
                            final String playcountFormatted = formatCount(Long.parseLong(playcount));

                            runOnUiThread(() -> {
                                tvFollowers.setText(listenersFormatted);
                                tvStreams.setText(playcountFormatted);
                                if (!finalSummary.isEmpty()) tvBio.setText(finalSummary);
                            });
                        } catch (Exception ignored) {}
                    }
                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {}
                });
    }

    private void fetchDiscographyFromLastFm() {
        lastFmApi.getArtistTopAlbums(ApiConfig.LASTFM_API_KEY, artistName, 10)
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call,
                                           Response<JsonObject> response) {
                        if (!response.isSuccessful() || response.body() == null) return;
                        try {
                            JsonObject topAlbums = response.body()
                                    .getAsJsonObject("topalbums");
                            if (topAlbums == null) return;
                            JsonArray albumArray = topAlbums.getAsJsonArray("album");
                            if (albumArray == null) return;

                            List<Map<String, String>> result = new ArrayList<>();
                            for (int i = 0; i < albumArray.size(); i++) {
                                JsonObject a = albumArray.get(i).getAsJsonObject();
                                Map<String, String> map = new HashMap<>();
                                map.put("name", a.get("name").getAsString());
                                map.put("playcount", a.get("playcount").getAsString());

                                // Get largest image (last in array)
                                JsonArray images = a.getAsJsonArray("image");
                                String imgUrl = "";
                                if (images != null && images.size() > 0) {
                                    for (int j = images.size() - 1; j >= 0; j--) {
                                        String url = images.get(j).getAsJsonObject()
                                                .get("#text").getAsString();
                                        if (!url.isEmpty()) { imgUrl = url; break; }
                                    }
                                }
                                map.put("image", imgUrl);
                                result.add(map);
                            }

                            runOnUiThread(() -> {
                                albums.clear();
                                albums.addAll(result);
                                albumAdapter.notifyDataSetChanged();
                            });
                        } catch (Exception ignored) {}
                    }
                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {}
                });
    }

    private String formatCount(long n) {
        if (n >= 1_000_000_000L) return String.format("%.1fB", n / 1_000_000_000.0);
        if (n >= 1_000_000L) return String.format("%.1fM", n / 1_000_000.0);
        if (n >= 1_000L) return String.format("%.1fK", n / 1_000.0);
        return String.valueOf(n);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnFollow.setOnClickListener(v -> {
            if (isFollowing) {
                artistDao.unfollowArtist(artistId, null);
                isFollowing = false;
            } else {
                artistDao.followArtist(artistId, artistName,
                        artistPhoto != null ? artistPhoto : "",
                        genre != null ? genre : "", null);
                isFollowing = true;
            }
            updateFollowButton();
        });

        btnShare.setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT,
                    "Check out " + artistName + " on MusicHub!");
            startActivity(Intent.createChooser(shareIntent, "Share Artist"));
        });
    }

    private void updateFollowButton() {
        if (isFollowing) {
            btnFollow.setImageResource(android.R.drawable.star_on);
            Toast.makeText(this, "Following " + artistName, Toast.LENGTH_SHORT).show();
        } else {
            btnFollow.setImageResource(android.R.drawable.star_off);
        }
    }
}
