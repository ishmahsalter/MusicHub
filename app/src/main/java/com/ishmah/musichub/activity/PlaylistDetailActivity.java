package com.ishmah.musichub.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.ishmah.musichub.MusicPlayerManager;
import com.ishmah.musichub.R;
import com.ishmah.musichub.ThemeHelper;
import androidx.core.content.ContextCompat;
import com.ishmah.musichub.adapter.PlaylistTrackAdapter;
import com.ishmah.musichub.api.DeezerApi;
import com.ishmah.musichub.db.PlaylistDao;
import com.ishmah.musichub.model.Track;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PlaylistDetailActivity extends AppCompatActivity {

    private ImageView ivCoverBg;
    private TextView tvPlaylistName, tvPlaylistMeta;
    private RecyclerView rvTracks;
    private LinearLayout llEmpty;

    private int playlistId;
    private String playlistName, coverType, coverValue;

    private PlaylistDao playlistDao;
    private DeezerApi deezerApi;
    private final List<Map<String, String>> tracks = new ArrayList<>();
    private PlaylistTrackAdapter trackAdapter;
    private ActivityResultLauncher<String> galleryLauncher;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_detail);

        playlistDao = new PlaylistDao(this);

        deezerApi = new Retrofit.Builder()
                .baseUrl("https://api.deezer.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(DeezerApi.class);

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri == null) return;
                    try {
                        getContentResolver().takePersistableUriPermission(
                                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    } catch (SecurityException ignored) {}
                    coverType = "image";
                    coverValue = uri.toString();
                    updateHeroCover();
                    playlistDao.updatePlaylistCover(playlistId, "image", uri.toString(), null);
                });

        playlistId   = getIntent().getIntExtra("playlistId", -1);
        playlistName = getIntent().getStringExtra("playlistName");
        if (playlistName == null) playlistName = "Playlist";

        ivCoverBg      = findViewById(R.id.iv_cover_bg);
        tvPlaylistName = findViewById(R.id.tv_playlist_name);
        tvPlaylistMeta = findViewById(R.id.tv_playlist_meta);
        rvTracks       = findViewById(R.id.rv_playlist_tracks);
        llEmpty        = findViewById(R.id.ll_empty);

        tvPlaylistName.setText(playlistName);

        trackAdapter = new PlaylistTrackAdapter(this, tracks, new PlaylistTrackAdapter.OnTrackActionListener() {
            @Override
            public void onTrackClick(int position, Map<String, String> track) {
                playTrack(position, track);
            }
            @Override
            public void onRemoveClick(int position, Map<String, String> track) {
                confirmRemoveTrack(position, track);
            }
        });
        rvTracks.setLayoutManager(new LinearLayoutManager(this));
        rvTracks.setAdapter(trackAdapter);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_play_all).setOnClickListener(v -> playAll());
        findViewById(R.id.btn_shuffle).setOnClickListener(v -> shufflePlay());
        findViewById(R.id.btn_edit_cover).setOnClickListener(v -> showEditDialog());
        findViewById(R.id.btn_delete).setOnClickListener(v -> confirmDelete());

        loadPlaylistData();
    }

    private void loadPlaylistData() {
        executor.execute(() -> {
            List<Map<String, String>> allPlaylists = playlistDao.getAllPlaylists();
            for (Map<String, String> p : allPlaylists) {
                if (String.valueOf(playlistId).equals(p.get("playlist_id"))) {
                    coverType  = p.get("cover_type");
                    coverValue = p.get("cover_value");
                    break;
                }
            }
            List<Map<String, String>> loaded = playlistDao.getPlaylistTracks(playlistId);
            runOnUiThread(() -> {
                tracks.clear();
                tracks.addAll(loaded);
                trackAdapter.notifyDataSetChanged();
                updateMeta();
                updateHeroCover();
                boolean empty = tracks.isEmpty();
                llEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
                rvTracks.setVisibility(empty ? View.GONE : View.VISIBLE);
            });
        });
    }

    private void updateMeta() {
        int count = tracks.size();
        long totalSecs = 0;
        for (Map<String, String> t : tracks) totalSecs += parseDuration(t.get("duration"));
        long minutes = totalSecs / 60;
        String meta = count + (count == 1 ? " track" : " tracks");
        if (minutes > 0) meta += " · " + minutes + " min";
        tvPlaylistMeta.setText(meta);
    }

    private void updateHeroCover() {
        if ("image".equals(coverType) && coverValue != null && !coverValue.isEmpty()) {
            ivCoverBg.setScaleType(ImageView.ScaleType.CENTER_CROP);
            Glide.with(this).load(Uri.parse(coverValue)).centerCrop()
                    .placeholder(R.drawable.bg_playlist_purple)
                    .into(ivCoverBg);
        } else {
            ivCoverBg.setScaleType(ImageView.ScaleType.FIT_XY);
            ivCoverBg.setImageResource(getGradientRes(coverValue));
        }
    }

    private void playTrack(int position, Map<String, String> track) {
        String name     = track.get("name");
        String artist   = track.get("artist");
        String albumArt = track.get("albumArt") != null ? track.get("albumArt") : "";
        String trackId  = track.get("trackId") != null ? track.get("trackId") : "";

        List<Track> playlist = buildTrackList();

        deezerApi.searchTrack(artist + " " + name).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                String previewUrl = "";
                if (response.isSuccessful() && response.body() != null) {
                    JsonArray data = response.body().getAsJsonArray("data");
                    if (data != null && data.size() > 0) {
                        JsonObject first = data.get(0).getAsJsonObject();
                        previewUrl = first.get("preview").getAsString();
                        if (position < playlist.size()) playlist.get(position).setPreviewUrl(previewUrl);
                    }
                }
                launchPlayer(playlist, position, name, artist, albumArt, trackId, previewUrl);
            }
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                launchPlayer(playlist, position, name, artist, albumArt, trackId, "");
            }
        });
    }

    private void launchPlayer(List<Track> playlist, int position, String name, String artist,
                              String albumArt, String trackId, String previewUrl) {
        MusicPlayerManager.getInstance().setPlaylist(playlist, position);
        MusicPlayerManager.getInstance().playTrack(name, artist, albumArt, trackId, previewUrl, 30);
        Intent intent = new Intent(this, DetailActivity.class);
        intent.putExtra("trackName",  name);
        intent.putExtra("artistName", artist);
        intent.putExtra("trackId",    trackId);
        intent.putExtra("albumArt",   albumArt);
        intent.putExtra("previewUrl", previewUrl);
        startActivity(intent);
    }

    private void playAll() {
        if (tracks.isEmpty()) {
            Toast.makeText(this, "No tracks in this playlist", Toast.LENGTH_SHORT).show();
            return;
        }
        playTrack(0, tracks.get(0));
    }

    private void shufflePlay() {
        if (tracks.isEmpty()) return;
        int pos = new Random().nextInt(tracks.size());
        playTrack(pos, tracks.get(pos));
    }

    private List<Track> buildTrackList() {
        List<Track> list = new ArrayList<>();
        for (Map<String, String> t : tracks) {
            Track tr = new Track(
                    t.get("name"), t.get("artist"), t.get("duration"),
                    t.get("albumArt") != null ? t.get("albumArt") : "",
                    t.get("trackId") != null ? t.get("trackId") : "",
                    "", "");
            list.add(tr);
        }
        return list;
    }

    private void confirmRemoveTrack(int position, Map<String, String> track) {
        String name = track.get("name") != null ? track.get("name") : "this track";
        new AlertDialog.Builder(this)
                .setTitle("Remove Track")
                .setMessage("Remove \"" + name + "\" from this playlist?")
                .setPositiveButton("Remove", (dialog, which) -> {
                    String trackId = track.get("trackId");
                    playlistDao.removeTrackFromPlaylist(playlistId, trackId, () -> runOnUiThread(() -> {
                        tracks.remove(position);
                        trackAdapter.notifyItemRemoved(position);
                        trackAdapter.notifyItemRangeChanged(position, tracks.size());
                        updateMeta();
                        boolean empty = tracks.isEmpty();
                        llEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
                        rvTracks.setVisibility(empty ? View.GONE : View.VISIBLE);
                    }));
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEditDialog() {
        int dp = (int) getResources().getDisplayMetrics().density;

        ScrollView scrollView = new ScrollView(this);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(24 * dp, 16 * dp, 24 * dp, 16 * dp);

        // --- Name section ---
        TextView nameLabel = new TextView(this);
        nameLabel.setText("PLAYLIST NAME");
        nameLabel.setTextSize(11);
        nameLabel.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        nameLabel.setPadding(0, 0, 0, 6 * dp);
        container.addView(nameLabel);

        EditText etName = new EditText(this);
        etName.setText(playlistName);
        etName.setInputType(InputType.TYPE_CLASS_TEXT);
        etName.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        etName.setHintTextColor(ContextCompat.getColor(this, R.color.text_hint));
        etName.setHint("Playlist name");
        etName.setPadding(0, 4 * dp, 0, 16 * dp);
        container.addView(etName);

        // --- Cover color section ---
        TextView coverLabel = new TextView(this);
        coverLabel.setText("COVER COLOR");
        coverLabel.setTextSize(11);
        coverLabel.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        coverLabel.setPadding(0, 0, 0, 8 * dp);
        container.addView(coverLabel);

        String[] colorNames = {"purple", "gold", "teal", "pink", "blue", "green"};
        int[] gradientRes = {
                R.drawable.bg_playlist_purple, R.drawable.bg_playlist_gold,
                R.drawable.bg_playlist_teal,   R.drawable.bg_playlist_pink,
                R.drawable.bg_playlist_blue,   R.drawable.bg_playlist_green
        };

        int swatchSize = 52 * dp;
        int swatchMargin = 6 * dp;

        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.setGravity(Gravity.CENTER);
        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.setGravity(Gravity.CENTER);

        final AlertDialog[] holder = {null};

        for (int i = 0; i < 6; i++) {
            View swatch = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(swatchSize, swatchSize);
            lp.setMargins(swatchMargin, swatchMargin, swatchMargin, swatchMargin);
            swatch.setLayoutParams(lp);
            swatch.setBackgroundResource(gradientRes[i]);
            String colorName = colorNames[i];
            swatch.setOnClickListener(v -> {
                playlistDao.updatePlaylistCover(playlistId, "gradient", colorName, () -> runOnUiThread(() -> {
                    coverType  = "gradient";
                    coverValue = colorName;
                    updateHeroCover();
                }));
                if (holder[0] != null) holder[0].dismiss();
            });
            if (i < 3) row1.addView(swatch);
            else       row2.addView(swatch);
        }
        container.addView(row1);
        container.addView(row2);

        // --- Gallery button ---
        Button btnGallery = new Button(this);
        btnGallery.setText("CHOOSE FROM GALLERY");
        btnGallery.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        LinearLayout.LayoutParams galleryLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        galleryLp.topMargin = 12 * dp;
        btnGallery.setLayoutParams(galleryLp);
        btnGallery.setOnClickListener(v -> {
            if (holder[0] != null) holder[0].dismiss();
            galleryLauncher.launch("image/*");
        });
        container.addView(btnGallery);

        scrollView.addView(container);

        holder[0] = new AlertDialog.Builder(this)
                .setTitle("Edit Playlist")
                .setView(scrollView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = etName.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        playlistName = newName;
                        tvPlaylistName.setText(playlistName);
                        playlistDao.updatePlaylistName(playlistId, newName, null);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Playlist")
                .setMessage("Delete \"" + playlistName + "\"? This cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) ->
                        playlistDao.deletePlaylist(playlistId, () -> runOnUiThread(this::finish)))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private int getGradientRes(String colorName) {
        if (colorName == null) return R.drawable.bg_playlist_purple;
        switch (colorName) {
            case "gold":  return R.drawable.bg_playlist_gold;
            case "teal":  return R.drawable.bg_playlist_teal;
            case "pink":  return R.drawable.bg_playlist_pink;
            case "blue":  return R.drawable.bg_playlist_blue;
            case "green": return R.drawable.bg_playlist_green;
            default:      return R.drawable.bg_playlist_purple;
        }
    }

    private long parseDuration(String duration) {
        if (duration == null || duration.isEmpty()) return 0;
        try {
            if (duration.contains(":")) {
                String[] parts = duration.split(":");
                if (parts.length == 2)
                    return Long.parseLong(parts[0].trim()) * 60 + Long.parseLong(parts[1].trim());
            } else {
                return Long.parseLong(duration.trim());
            }
        } catch (Exception ignored) {}
        return 0;
    }
}
