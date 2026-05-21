package com.ishmah.musichub.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.ishmah.musichub.MusicPlayerManager;
import com.ishmah.musichub.R;
import com.ishmah.musichub.ThemeHelper;
import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.Path;
import java.util.ArrayList;
import java.util.List;

public class LyricsActivity extends AppCompatActivity
        implements MusicPlayerManager.OnPlayerStateChangedListener {

    interface LyricsApi {
        @GET("v1/{artist}/{title}")
        Call<JsonObject> getLyrics(
                @Path("artist") String artist,
                @Path("title") String title
        );
    }

    private TextView tvLyricsTitle, tvLyricsArtist,
            tvCurrentTime, tvTotalTime;
    private ImageView ivMiniArt, btnBack, btnMore, btnPlay;
    private LinearLayout llLyricsContainer, btnShareLyric, btnFontSize;
    private ProgressBar seekProgress;
    private ScrollView scrollView;
    private MusicPlayerManager playerManager;
    private String trackName, artistName, albumArt;
    private List<String> lyricLines = new ArrayList<>();
    private List<TextView> lyricViews = new ArrayList<>();
    private int activeLineIndex = 0;
    private int totalLines = 0;
    private String currentLyrics = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeHelper.apply(this);
        setContentView(R.layout.activity_lyrics);

        playerManager = MusicPlayerManager.getInstance();

        initViews();
        receiveIntent();
        loadLyrics();

        // Add listener
        playerManager.addListener(this);
    }

    private void initViews() {
        tvLyricsTitle = findViewById(R.id.tv_lyrics_title);
        tvLyricsArtist = findViewById(R.id.tv_lyrics_artist);
        tvCurrentTime = findViewById(R.id.tv_current_time);
        tvTotalTime = findViewById(R.id.tv_total_time);
        ivMiniArt = findViewById(R.id.iv_mini_art);
        btnBack = findViewById(R.id.btn_back);
        btnMore = findViewById(R.id.btn_more);
        btnPlay = findViewById(R.id.btn_play);
        llLyricsContainer = findViewById(R.id.ll_lyrics_container);
        seekProgress = findViewById(R.id.seek_progress);
        btnShareLyric = findViewById(R.id.btn_share_lyric);
        btnFontSize = findViewById(R.id.btn_font_size);
        scrollView = findViewById(R.id.scroll_lyrics);
    }

    private void receiveIntent() {
        trackName = getIntent().getStringExtra("trackName");
        artistName = getIntent().getStringExtra("artistName");
        albumArt = getIntent().getStringExtra("albumArt");

        if (trackName != null) tvLyricsTitle.setText(trackName);
        if (artistName != null) tvLyricsArtist.setText(artistName);

        if (albumArt != null && !albumArt.isEmpty()) {
            Glide.with(this)
                    .load(albumArt)
                    .apply(RequestOptions.bitmapTransform(
                            new RoundedCorners(8)))
                    .placeholder(R.color.bg_card)
                    .into(ivMiniArt);
        }

        // Sync progress
        int total = playerManager.getTotalDuration();
        int progress = playerManager.getCurrentProgress();
        seekProgress.setMax(total > 0 ? total : 30);
        seekProgress.setProgress(progress);
        tvCurrentTime.setText(playerManager.formatTime(progress));
        tvTotalTime.setText(playerManager.formatTime(total));
        onPlayStateChanged(playerManager.isPlaying());

        // Update active line
        updateActiveLineFromProgress(progress, total);

        btnBack.setOnClickListener(v -> finish());

        btnPlay.setOnClickListener(v -> {
            if (playerManager.hasTrack()) playerManager.togglePlayPause();
        });

        btnShareLyric.setOnClickListener(v -> {
            String activeLine = (activeLineIndex < lyricLines.size() &&
                    !lyricLines.isEmpty())
                    ? lyricLines.get(activeLineIndex) : trackName;
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT,
                    "🎵 \"" + activeLine + "\"\n" +
                            "— " + trackName + " by " + artistName +
                            "\n\nListened on MusicHub!");
            startActivity(Intent.createChooser(shareIntent, "Share Lyric"));
        });
    }

    private void loadLyrics() {
        if (trackName == null || artistName == null) return;

        // Clean artist name untuk API
        String cleanArtist = artistName.split(" feat")[0]
                .split(" ft\\.")[0].trim();
        String cleanTrack = trackName.split("\\(")[0].trim();

        retrofit2.Retrofit lyricsRetrofit = new retrofit2.Retrofit.Builder()
                .baseUrl("https://api.lyrics.ovh/")
                .addConverterFactory(
                        retrofit2.converter.gson.GsonConverterFactory.create())
                .build();

        LyricsApi lyricsApi = lyricsRetrofit.create(LyricsApi.class);
        lyricsApi.getLyrics(cleanArtist, cleanTrack)
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call,
                                           Response<JsonObject> response) {
                        if (response.isSuccessful() &&
                                response.body() != null) {
                            JsonObject body = response.body();
                            if (body.has("lyrics") &&
                                    !body.get("lyrics").getAsString()
                                            .isEmpty()) {
                                currentLyrics = body.get("lyrics")
                                        .getAsString();
                                runOnUiThread(() ->
                                        displayLyrics(currentLyrics));
                            } else {
                                runOnUiThread(() -> showNoLyrics());
                            }
                        } else {
                            runOnUiThread(() -> showNoLyrics());
                        }
                    }

                    @Override
                    public void onFailure(Call<JsonObject> call,
                                          Throwable t) {
                        runOnUiThread(() -> showNoLyrics());
                    }
                });
    }

    private void displayLyrics(String lyrics) {
        llLyricsContainer.removeAllViews();
        lyricLines.clear();
        lyricViews.clear();

        String[] lines = lyrics.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                lyricLines.add(trimmed);
            }
        }

        totalLines = lyricLines.size();

        // Hitung active line berdasarkan progress saat ini
        updateActiveLineFromProgress(
                playerManager.getCurrentProgress(),
                playerManager.getTotalDuration());

        for (int i = 0; i < lyricLines.size(); i++) {
            TextView tv = createLyricView(lyricLines.get(i), i);
            lyricViews.add(tv);
            llLyricsContainer.addView(tv);
        }

        scrollToActiveLine();
    }

    private TextView createLyricView(String line, int index) {
        TextView tv = new TextView(this);
        tv.setText(line);
        tv.setPadding(12, 6, 12, 6);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 2, 0, 2);
        tv.setLayoutParams(params);

        applyLyricStyle(tv, index);
        return tv;
    }

    private void applyLyricStyle(TextView tv, int index) {
        int diff = index - activeLineIndex;

        if (index == activeLineIndex) {
            // ACTIVE — purple soft bold, left border bg
            tv.setTextColor(getResources().getColor(R.color.purple_soft));
            tv.setTypeface(android.graphics.Typeface.create(
                    "sans-serif", android.graphics.Typeface.BOLD));
            tv.setTextSize(15f);
            tv.setBackgroundResource(R.drawable.bg_lyric_active_line);
            tv.setAlpha(1f);

        } else if (index < activeLineIndex) {
            // PAST — makin lama makin redup
            tv.setTextColor(getResources().getColor(R.color.white));
            tv.setTypeface(android.graphics.Typeface.DEFAULT);
            tv.setTextSize(13f);
            tv.setBackgroundResource(0);
            int distPast = activeLineIndex - index;
            float alpha = distPast == 1 ? 0.35f :
                    distPast == 2 ? 0.22f : 0.15f;
            tv.setAlpha(alpha);

        } else if (diff == 2 && totalLines > 6 && index % 4 == 0) {
            // GOLD — chorus estimasi
            tv.setTextColor(getResources().getColor(R.color.gold_primary));
            tv.setTypeface(android.graphics.Typeface.create(
                    "sans-serif", android.graphics.Typeface.BOLD));
            tv.setTextSize(13f);
            tv.setBackgroundResource(0);
            tv.setAlpha(0.85f);

        } else {
            // UPCOMING — makin jauh makin redup
            tv.setTextColor(getResources().getColor(R.color.white));
            tv.setTypeface(android.graphics.Typeface.DEFAULT);
            tv.setTextSize(13f);
            tv.setBackgroundResource(0);
            float alpha = diff == 1 ? 0.55f :
                    diff == 2 ? 0.45f :
                            diff == 3 ? 0.32f :
                                    diff == 4 ? 0.22f : 0.12f;
            tv.setAlpha(alpha);
        }
    }

    private void updateActiveLineFromProgress(int progress, int total) {
        if (totalLines == 0 || total == 0) return;
        float ratio = (float) progress / total;
        activeLineIndex = (int) (ratio * totalLines);
        if (activeLineIndex >= totalLines) activeLineIndex = totalLines - 1;
        if (activeLineIndex < 0) activeLineIndex = 0;
    }

    private void updateLyricHighlight() {
        if (lyricViews.isEmpty()) return;
        for (int i = 0; i < lyricViews.size(); i++) {
            applyLyricStyle(lyricViews.get(i), i);
        }
        scrollToActiveLine();
    }

    private void scrollToActiveLine() {
        if (lyricViews.isEmpty() ||
                activeLineIndex >= lyricViews.size()) return;
        TextView activeView = lyricViews.get(activeLineIndex);
        scrollView.post(() -> {
            int scrollY = activeView.getTop() - scrollView.getHeight() / 3;
            scrollView.smoothScrollTo(0, Math.max(0, scrollY));
        });
    }

    private void showNoLyrics() {
        llLyricsContainer.removeAllViews();
        TextView tv = new TextView(this);
        tv.setText("Lyrics not available for this track 🎵\n\nTry searching on Genius or AZLyrics");
        tv.setTextColor(getResources().getColor(R.color.text_muted));
        tv.setTextSize(14f);
        tv.setGravity(android.view.Gravity.CENTER);
        tv.setPadding(16, 64, 16, 16);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        tv.setLayoutParams(params);
        llLyricsContainer.addView(tv);
    }

    @Override
    public void onTrackChanged(String t, String a, String art, String id) {
        // Update mini art jika track berubah
        runOnUiThread(() -> {
            tvLyricsTitle.setText(t);
            tvLyricsArtist.setText(a);
            if (art != null && !art.isEmpty()) {
                Glide.with(this)
                        .load(art)
                        .apply(RequestOptions.bitmapTransform(
                                new RoundedCorners(8)))
                        .into(ivMiniArt);
            }
        });
    }

    @Override
    public void onPlayStateChanged(boolean isPlaying) {
        runOnUiThread(() -> {
            if (isPlaying) {
                btnPlay.setImageResource(R.drawable.ic_pause);
            } else {
                btnPlay.setImageResource(R.drawable.ic_play);
            }
        });
    }

    @Override
    public void onProgressChanged(int progress, int total) {
        runOnUiThread(() -> {
            seekProgress.setMax(total > 0 ? total : 30);
            seekProgress.setProgress(progress);
            tvCurrentTime.setText(playerManager.formatTime(progress));
            tvTotalTime.setText(playerManager.formatTime(total));

            // Update active line
            int prevActive = activeLineIndex;
            updateActiveLineFromProgress(progress, total);
            if (prevActive != activeLineIndex && !lyricViews.isEmpty()) {
                updateLyricHighlight();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        playerManager.addListener(this);
        onPlayStateChanged(playerManager.isPlaying());
        onProgressChanged(playerManager.getCurrentProgress(),
                playerManager.getTotalDuration());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        playerManager.removeListener(this);
    }
}