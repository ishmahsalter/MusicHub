package com.ishmah.musichub.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.ishmah.musichub.MusicPlayerManager;
import com.ishmah.musichub.R;
import com.ishmah.musichub.adapter.TrackAdapter;
import com.ishmah.musichub.api.ApiConfig;
import com.ishmah.musichub.api.DeezerApi;
import com.ishmah.musichub.api.LastFmApi;
import com.ishmah.musichub.api.NetworkChecker;
import com.ishmah.musichub.api.RetrofitClient;
import com.ishmah.musichub.db.FavoriteDao;
import com.ishmah.musichub.fragment.AddToPlaylistDialog;
import com.ishmah.musichub.model.Track;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AlbumActivity extends AppCompatActivity {

    private ImageView ivAlbumBg, ivAlbumArt, btnLike, btnAdd, btnShare;
    private TextView tvAlbumName, tvAlbumArtist, tvAlbumMeta;
    private ImageButton btnBack;
    private Button btnPlayAll;
    private RecyclerView rvTracks;
    private LinearLayout llNoNetwork;
    private TrackAdapter trackAdapter;
    private List<Track> trackList = new ArrayList<>();
    private LastFmApi lastFmApi;
    private DeezerApi deezerApi;
    private FavoriteDao favoriteDao;
    private String passedArtistName, passedTrackName, passedAlbumArt;
    private String resolvedAlbumName = "";
    private boolean isLiked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences prefs = getSharedPreferences("musichub_prefs", MODE_PRIVATE);
        AppCompatDelegate.setDefaultNightMode(
                "light".equals(prefs.getString("theme", "dark"))
                        ? AppCompatDelegate.MODE_NIGHT_NO
                        : AppCompatDelegate.MODE_NIGHT_YES);
        setContentView(R.layout.activity_album);

        lastFmApi = RetrofitClient.getLastFmInstance().create(LastFmApi.class);
        deezerApi = RetrofitClient.getDeezerInstance().create(DeezerApi.class);
        favoriteDao = new FavoriteDao(this);

        ivAlbumBg = findViewById(R.id.iv_album_bg);
        ivAlbumArt = findViewById(R.id.iv_album_art);
        tvAlbumName = findViewById(R.id.tv_album_name);
        tvAlbumArtist = findViewById(R.id.tv_album_artist);
        tvAlbumMeta = findViewById(R.id.tv_album_meta);
        btnBack = findViewById(R.id.btn_back);
        btnPlayAll = findViewById(R.id.btn_play_all);
        btnLike = findViewById(R.id.btn_like);
        btnAdd = findViewById(R.id.btn_add);
        btnShare = findViewById(R.id.btn_share);
        rvTracks = findViewById(R.id.rv_tracks);
        llNoNetwork = findViewById(R.id.ll_no_network);

        trackAdapter = new TrackAdapter(this, trackList);
        rvTracks.setLayoutManager(new LinearLayoutManager(this));
        rvTracks.setAdapter(trackAdapter);

        Intent intent = getIntent();
        passedArtistName = intent.getStringExtra("artistName");
        passedTrackName = intent.getStringExtra("trackName");
        passedAlbumArt = intent.getStringExtra("albumArt");

        if (passedArtistName != null) tvAlbumArtist.setText(passedArtistName);

        if (passedAlbumArt != null && !passedAlbumArt.isEmpty()) {
            Glide.with(this).load(passedAlbumArt).centerCrop().into(ivAlbumBg);
            Glide.with(this).load(passedAlbumArt).centerCrop().into(ivAlbumArt);
        }

        btnBack.setOnClickListener(v -> finish());

        btnPlayAll.setOnClickListener(v -> {
            if (!trackList.isEmpty()) {
                MusicPlayerManager.getInstance().setPlaylist(trackList, 0);
                Track first = trackList.get(0);
                MusicPlayerManager.getInstance().playTrack(
                        first.getName(), first.getArtist(),
                        first.getAlbumArt() != null ? first.getAlbumArt() : "",
                        first.getTrackId(),
                        first.getPreviewUrl() != null ? first.getPreviewUrl() : "",
                        30);
                Intent di = new Intent(this, DetailActivity.class);
                di.putExtra("trackName", first.getName());
                di.putExtra("artistName", first.getArtist());
                di.putExtra("trackId", first.getTrackId());
                di.putExtra("albumArt", first.getAlbumArt() != null ? first.getAlbumArt() : "");
                di.putExtra("previewUrl", first.getPreviewUrl() != null ? first.getPreviewUrl() : "");
                startActivity(di);
            }
        });

        btnShare.setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT,
                    "Check out " + resolvedAlbumName + " by " + passedArtistName + " on MusicHub!");
            startActivity(Intent.createChooser(shareIntent, "Share Album"));
        });

        btnAdd.setOnClickListener(v -> {
            if (!trackList.isEmpty()) {
                AddToPlaylistDialog dialog = AddToPlaylistDialog.newInstance(
                        trackList.get(0).getTrackId(),
                        resolvedAlbumName,
                        passedArtistName);
                dialog.show(getSupportFragmentManager(), "AddToPlaylist");
            }
        });

        if (!NetworkChecker.isConnected(this)) {
            llNoNetwork.setVisibility(View.VISIBLE);
            return;
        }

        // Step 1: Look up album name via track.getInfo
        if (passedTrackName != null && passedArtistName != null) {
            fetchAlbumViaTrackInfo();
        }
    }

    private void fetchAlbumViaTrackInfo() {
        lastFmApi.getTrackInfo(ApiConfig.LASTFM_API_KEY, passedTrackName, passedArtistName)
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        if (!response.isSuccessful() || response.body() == null) {
                            // Fallback: use track name as album
                            loadAlbum(passedTrackName, passedArtistName);
                            return;
                        }
                        try {
                            JsonObject track = response.body().getAsJsonObject("track");
                            if (track != null && track.has("album")) {
                                JsonObject album = track.getAsJsonObject("album");
                                String albumTitle = album.get("title").getAsString();
                                loadAlbum(albumTitle, passedArtistName);
                            } else {
                                loadAlbum(passedTrackName, passedArtistName);
                            }
                        } catch (Exception e) {
                            loadAlbum(passedTrackName, passedArtistName);
                        }
                    }

                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {
                        loadAlbum(passedTrackName, passedArtistName);
                    }
                });
    }

    private void loadAlbum(String albumName, String artistName) {
        resolvedAlbumName = albumName;
        runOnUiThread(() -> tvAlbumName.setText(albumName));

        // Fetch album info (tracks + year)
        lastFmApi.getAlbumInfo(ApiConfig.LASTFM_API_KEY, albumName, artistName)
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        if (!response.isSuccessful() || response.body() == null) {
                            llNoNetwork.setVisibility(View.VISIBLE);
                            return;
                        }
                        try {
                            JsonObject album = response.body().getAsJsonObject("album");
                            if (album == null) { llNoNetwork.setVisibility(View.VISIBLE); return; }

                            String year = "—";
                            if (album.has("wiki")) {
                                JsonObject wiki = album.getAsJsonObject("wiki");
                                if (wiki.has("published")) {
                                    String pub = wiki.get("published").getAsString();
                                    if (pub.length() >= 4) year = pub.substring(pub.length() - 4).trim();
                                }
                            }

                            // Album art from LastFM
                            String artUrl = passedAlbumArt != null ? passedAlbumArt : "";
                            if (album.has("image")) {
                                JsonArray images = album.getAsJsonArray("image");
                                for (int i = images.size() - 1; i >= 0; i--) {
                                    String u = images.get(i).getAsJsonObject().get("#text").getAsString();
                                    if (!u.isEmpty()) { artUrl = u; break; }
                                }
                            }

                            final String finalArtUrl = artUrl;
                            if (!artUrl.isEmpty()) {
                                runOnUiThread(() -> {
                                    Glide.with(AlbumActivity.this).load(finalArtUrl).centerCrop().into(ivAlbumBg);
                                    Glide.with(AlbumActivity.this).load(finalArtUrl).centerCrop().into(ivAlbumArt);
                                });
                            }

                            // Parse tracks
                            List<Track> result = new ArrayList<>();
                            if (album.has("tracks")) {
                                JsonObject tracks = album.getAsJsonObject("tracks");
                                JsonArray trackArr = tracks.getAsJsonArray("track");
                                if (trackArr != null) {
                                    for (int i = 0; i < trackArr.size(); i++) {
                                        JsonObject t = trackArr.get(i).getAsJsonObject();
                                        String name = t.get("name").getAsString();
                                        String dur = "0:30";
                                        if (t.has("duration") && !t.get("duration").isJsonNull()) {
                                            try {
                                                int s = t.get("duration").getAsInt();
                                                if (s > 0) dur = String.format("%d:%02d", s / 60, s % 60);
                                            } catch (Exception ignored) {}
                                        }
                                        Track track = new Track(name, artistName, dur,
                                                finalArtUrl, name + artistName, "", "");
                                        result.add(track);
                                    }
                                }
                            }

                            final String finalYear = year;
                            final int trackCount = result.size();
                            runOnUiThread(() -> {
                                tvAlbumMeta.setText("Studio Album · " + finalYear +
                                        " · " + trackCount + " tracks");
                                trackList.clear();
                                trackList.addAll(result);
                                trackAdapter.notifyDataSetChanged();
                            });

                            // Fetch Deezer preview for each track
                            for (Track t : result) {
                                fetchDeezerPreview(t, result.indexOf(t));
                            }

                        } catch (Exception e) {
                            llNoNetwork.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {
                        llNoNetwork.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void fetchDeezerPreview(Track track, int index) {
        String query = track.getName() + " " + track.getArtist();
        deezerApi.searchTrack(query).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!response.isSuccessful() || response.body() == null) return;
                try {
                    JsonArray data = response.body().getAsJsonArray("data");
                    if (data == null || data.size() == 0) return;
                    JsonObject first = data.get(0).getAsJsonObject();
                    if (first.has("preview")) track.setPreviewUrl(first.get("preview").getAsString());
                    if (first.has("album")) {
                        JsonObject alb = first.getAsJsonObject("album");
                        if (alb.has("cover_xl") && track.getAlbumArt().isEmpty()) {
                            track.setAlbumArt(alb.get("cover_xl").getAsString());
                        }
                    }
                    runOnUiThread(() -> trackAdapter.notifyItemChanged(index));
                } catch (Exception ignored) {}
            }
            @Override public void onFailure(Call<JsonObject> call, Throwable t) {}
        });
    }
}
