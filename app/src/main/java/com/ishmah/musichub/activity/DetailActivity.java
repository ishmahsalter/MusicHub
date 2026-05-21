package com.ishmah.musichub.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.ishmah.musichub.MusicPlayerManager;
import com.ishmah.musichub.R;
import com.ishmah.musichub.ThemeHelper;
import com.ishmah.musichub.db.FavoriteDao;
import com.ishmah.musichub.fragment.AddToPlaylistDialog;

public class DetailActivity extends AppCompatActivity
        implements MusicPlayerManager.OnPlayerStateChangedListener {

    private TextView tvTrackName, tvArtistName, tvCurrentTime, tvTotalTime;
    private ImageView ivAlbumArt, ivLike, btnPlay, btnPrev, btnNext,
            btnShuffle, btnRepeat, btnBack;
    private SeekBar seekBar;
    private LinearLayout btnLyrics, btnAddPlaylist, btnAlbum,
            btnArtist, btnShare;
    private String trackId, trackName, artistName, albumArt, previewUrl;
    private boolean isLiked = false;
    private FavoriteDao favoriteDao;
    private MusicPlayerManager playerManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeHelper.apply(this);
        setContentView(R.layout.activity_detail);

        favoriteDao = new FavoriteDao(this);
        playerManager = MusicPlayerManager.getInstance();

        initViews();
        receiveIntent();
        setupClickListeners();

        // Add listener
        playerManager.addListener(this);
    }

    private void initViews() {
        tvTrackName = findViewById(R.id.tv_track_name);
        tvArtistName = findViewById(R.id.tv_artist_name);
        tvCurrentTime = findViewById(R.id.tv_current_time);
        tvTotalTime = findViewById(R.id.tv_total_time);
        ivAlbumArt = findViewById(R.id.iv_album_art);
        ivLike = findViewById(R.id.iv_like);
        btnPlay = findViewById(R.id.btn_play);
        btnPrev = findViewById(R.id.btn_prev);
        btnNext = findViewById(R.id.btn_next);
        btnShuffle = findViewById(R.id.btn_shuffle);
        btnRepeat = findViewById(R.id.btn_repeat);
        btnBack = findViewById(R.id.btn_back);
        seekBar = findViewById(R.id.seek_bar);
        btnLyrics = findViewById(R.id.btn_lyrics);
        btnAddPlaylist = findViewById(R.id.btn_add_playlist);
        btnAlbum = findViewById(R.id.btn_album);
        btnArtist = findViewById(R.id.btn_artist);
        btnShare = findViewById(R.id.btn_share);

        // Bottom nav
        LinearLayout navHome = findViewById(R.id.nav_home);
        LinearLayout navFind = findViewById(R.id.nav_find);
        LinearLayout navFavs = findViewById(R.id.nav_favs);
        LinearLayout navMe = findViewById(R.id.nav_me);
        ImageView navFab = findViewById(R.id.nav_fab);

        if (navHome != null) navHome.setOnClickListener(v -> goToMain("home"));
        if (navFind != null) navFind.setOnClickListener(v -> goToMain("search"));
        if (navFavs != null) navFavs.setOnClickListener(v -> goToMain("favorite"));
        if (navMe != null) navMe.setOnClickListener(v -> goToMain("profile"));
        if (navFab != null) navFab.setOnClickListener(v -> playerManager.togglePlayPause());
    }

    private void goToMain(String tab) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("nav_tab", tab);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }

    private void receiveIntent() {
        Intent intent = getIntent();
        if (intent != null) {
            trackName = intent.getStringExtra("trackName");
            artistName = intent.getStringExtra("artistName");
            trackId = intent.getStringExtra("trackId");
            albumArt = intent.getStringExtra("albumArt");
            previewUrl = intent.getStringExtra("previewUrl");

            updateUI();

            // Play kalau berbeda dari yang sedang diputar
            if (trackName != null && !trackName.equals(
                    playerManager.getCurrentTrackName())) {
                playerManager.playTrack(
                        trackName,
                        artistName != null ? artistName : "",
                        albumArt != null ? albumArt : "",
                        trackId != null ? trackId : "",
                        previewUrl != null ? previewUrl : "",
                        30);
            }
        }
    }

    private void updateUI() {
        if (trackName != null) tvTrackName.setText(trackName);
        if (artistName != null) tvArtistName.setText(artistName);

        if (albumArt != null && !albumArt.isEmpty()) {
            Glide.with(this)
                    .load(albumArt)
                    .apply(RequestOptions.bitmapTransform(
                            new RoundedCorners(32)))
                    .placeholder(R.drawable.bg_gradient_purple)
                    .into(ivAlbumArt);
        }

        if (trackId != null) {
            isLiked = favoriteDao.isFavorite(trackId);
            updateLikeIcon();
        }

        // Sync seekbar
        int total = playerManager.getTotalDuration();
        int progress = playerManager.getCurrentProgress();
        seekBar.setMax(total > 0 ? total : 30);
        seekBar.setProgress(progress);
        tvCurrentTime.setText(playerManager.formatTime(progress));
        tvTotalTime.setText(playerManager.formatTime(total));
        onPlayStateChanged(playerManager.isPlaying());
    }

    private void updateUIFromManager() {
        trackName = playerManager.getCurrentTrackName();
        artistName = playerManager.getCurrentArtistName();
        albumArt = playerManager.getCurrentAlbumArt();
        trackId = playerManager.getCurrentTrackId();
        previewUrl = playerManager.getCurrentPreviewUrl();

        runOnUiThread(() -> {
            tvTrackName.setText(trackName);
            tvArtistName.setText(artistName);

            if (albumArt != null && !albumArt.isEmpty()) {
                Glide.with(this)
                        .load(albumArt)
                        .apply(RequestOptions.bitmapTransform(
                                new RoundedCorners(32)))
                        .placeholder(R.drawable.bg_gradient_purple)
                        .into(ivAlbumArt);
            }

            isLiked = favoriteDao.isFavorite(
                    trackId != null ? trackId : "");
            updateLikeIcon();
        });
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnPlay.setOnClickListener(v -> playerManager.togglePlayPause());

        btnPrev.setOnClickListener(v -> {
            if (playerManager.hasPlaylist()) {
                playerManager.playPrev();
                updateUIFromManager();
            } else {
                playerManager.seekTo(0);
                seekBar.setProgress(0);
                tvCurrentTime.setText("0:00");
            }
        });

        btnNext.setOnClickListener(v -> {
            if (playerManager.hasPlaylist()) {
                playerManager.playNext();
                updateUIFromManager();
            }
        });

        btnShuffle.setOnClickListener(v ->
                Toast.makeText(this, "Shuffle", Toast.LENGTH_SHORT).show());

        btnRepeat.setOnClickListener(v ->
                Toast.makeText(this, "Repeat", Toast.LENGTH_SHORT).show());

        ivLike.setOnClickListener(v -> {
            if (trackId == null) return;
            if (isLiked) {
                favoriteDao.removeFavorite(trackId, null);
                isLiked = false;
            } else {
                favoriteDao.addFavorite(
                        trackId,
                        trackName != null ? trackName : "",
                        artistName != null ? artistName : "",
                        albumArt != null ? albumArt : "",
                        playerManager.formatTime(
                                playerManager.getTotalDuration()),
                        null);
                isLiked = true;
            }
            updateLikeIcon();
        });

        btnLyrics.setOnClickListener(v -> {
            Intent intent = new Intent(this, LyricsActivity.class);
            intent.putExtra("trackName", trackName);
            intent.putExtra("artistName", artistName);
            intent.putExtra("albumArt", albumArt);
            startActivity(intent);
        });

        btnAddPlaylist.setOnClickListener(v -> {
            if (trackId != null) {
                AddToPlaylistDialog dialog = AddToPlaylistDialog.newInstance(
                        trackId, trackName, artistName);
                dialog.show(getSupportFragmentManager(), "AddToPlaylist");
            }
        });

        btnShare.setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT,
                    "🎵 " + trackName + " - " + artistName +
                            "\n\nListened on MusicHub!");
            startActivity(Intent.createChooser(shareIntent, "Share"));
        });

        btnArtist.setOnClickListener(v -> {
            Intent intent = new Intent(this, ArtistActivity.class);
            intent.putExtra("artistName", artistName);
            intent.putExtra("artistId", artistName);
            startActivity(intent);
        });

        btnAlbum.setOnClickListener(v -> {
            Intent albumIntent = new Intent(this, AlbumActivity.class);
            albumIntent.putExtra("trackName", trackName);
            albumIntent.putExtra("artistName", artistName);
            albumIntent.putExtra("albumArt", albumArt != null ? albumArt : "");
            startActivity(albumIntent);
        });

        seekBar.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress,
                                                  boolean fromUser) {
                        if (fromUser) {
                            playerManager.seekTo(progress);
                            tvCurrentTime.setText(
                                    playerManager.formatTime(progress));
                        }
                    }
                    @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                    @Override public void onStopTrackingTouch(SeekBar seekBar) {}
                });
    }

    @Override
    public void onTrackChanged(String t, String a, String art, String id) {
        updateUIFromManager();
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
            seekBar.setMax(total > 0 ? total : 30);
            seekBar.setProgress(progress);
            tvCurrentTime.setText(playerManager.formatTime(progress));
            tvTotalTime.setText(playerManager.formatTime(total));
        });
    }

    private void updateLikeIcon() {
        if (isLiked) {
            ivLike.setImageResource(R.drawable.ic_heart_filled);
            ivLike.clearColorFilter();
        } else {
            ivLike.setImageResource(R.drawable.ic_heart);
            ivLike.setColorFilter(getResources()
                    .getColor(R.color.text_muted));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        playerManager.addListener(this);
        onPlayStateChanged(playerManager.isPlaying());
        onProgressChanged(playerManager.getCurrentProgress(),
                playerManager.getTotalDuration());
        // Sync UI kalau track berubah (prev/next dari mini player)
        if (!playerManager.getCurrentTrackName().equals(trackName)) {
            updateUIFromManager();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        playerManager.removeListener(this);
    }
}