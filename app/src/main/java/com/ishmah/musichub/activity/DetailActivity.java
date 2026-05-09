package com.ishmah.musichub.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.ishmah.musichub.R;
import com.ishmah.musichub.db.FavoriteDao;
import com.ishmah.musichub.fragment.AddToPlaylistDialog;

public class DetailActivity extends AppCompatActivity {

    private TextView tvTrackName, tvArtistName, tvCurrentTime, tvTotalTime;
    private ImageView ivAlbumArt, ivLike, btnPlay, btnPrev, btnNext,
            btnShuffle, btnRepeat, btnBack;
    private SeekBar seekBar;
    private Handler handler = new Handler(Looper.getMainLooper());
    private boolean isPlaying = true;
    private boolean isLiked = false;
    private int currentProgress = 0;
    private int totalDuration = 180;
    private String trackId, trackName, artistName, albumArt;
    private FavoriteDao favoriteDao;

    private Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            if (isPlaying && currentProgress < totalDuration) {
                currentProgress++;
                seekBar.setProgress(currentProgress);
                tvCurrentTime.setText(formatTime(currentProgress));
                handler.postDelayed(this, 1000);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        favoriteDao = new FavoriteDao(this);
        initViews();
        receiveIntent();
        setupClickListeners();

        // Auto start progress
        handler.post(progressRunnable);
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
        seekBar.setMax(totalDuration);
    }

    private void receiveIntent() {
        Intent intent = getIntent();
        if (intent != null) {
            trackName = intent.getStringExtra("trackName");
            artistName = intent.getStringExtra("artistName");
            trackId = intent.getStringExtra("trackId");
            albumArt = intent.getStringExtra("albumArt");

            if (trackName != null) tvTrackName.setText(trackName);
            if (artistName != null) tvArtistName.setText(artistName);

            // Load album art
            if (albumArt != null && !albumArt.isEmpty()) {
                Glide.with(this)
                        .load(albumArt)
                        .apply(RequestOptions.bitmapTransform(new RoundedCorners(32)))
                        .placeholder(R.drawable.bg_gradient_purple)
                        .into(ivAlbumArt);
            }

            // Cek like status
            if (trackId != null) {
                isLiked = favoriteDao.isFavorite(trackId);
                updateLikeIcon();
            }
        }
        tvTotalTime.setText(formatTime(totalDuration));
    }

    private void setupClickListeners() {
        // Back
        btnBack.setOnClickListener(v -> finish());

        // Play/Pause
        btnPlay.setOnClickListener(v -> {
            if (isPlaying) {
                isPlaying = false;
                handler.removeCallbacks(progressRunnable);
                btnPlay.setImageResource(R.drawable.ic_play);
            } else {
                isPlaying = true;
                handler.post(progressRunnable);
                btnPlay.setImageResource(R.drawable.ic_pause);
            }
        });

        // Prev
        btnPrev.setOnClickListener(v -> {
            currentProgress = 0;
            seekBar.setProgress(0);
            tvCurrentTime.setText(formatTime(0));
        });

        // Next
        btnNext.setOnClickListener(v -> finish());

        // Like
        ivLike.setOnClickListener(v -> {
            if (isLiked) {
                favoriteDao.removeFavorite(trackId, null);
                isLiked = false;
            } else {
                favoriteDao.addFavorite(trackId, trackName,
                        artistName, albumArt != null ? albumArt : "",
                        formatTime(totalDuration), null);
                isLiked = true;
            }
            updateLikeIcon();
        });

        // Add to playlist
        findViewById(R.id.btn_add_playlist).setOnClickListener(v -> {
            AddToPlaylistDialog dialog = AddToPlaylistDialog.newInstance(
                    trackId, trackName, artistName);
            dialog.show(getSupportFragmentManager(), "AddToPlaylist");
        });

        // Share lyric
        findViewById(R.id.btn_share).setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT,
                    "🎵 " + trackName + " - " + artistName + "\n\nListened on MusicHub!");
            startActivity(Intent.createChooser(shareIntent, "Share"));
        });

        // SeekBar
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    currentProgress = progress;
                    tvCurrentTime.setText(formatTime(progress));
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {
                handler.removeCallbacks(progressRunnable);
            }
            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                if (isPlaying) handler.post(progressRunnable);
            }
        });
    }

    private void updateLikeIcon() {
        if (isLiked) {
            ivLike.setImageResource(R.drawable.ic_heart_filled);
        } else {
            ivLike.setImageResource(R.drawable.ic_heart);
            ivLike.setColorFilter(getResources().getColor(R.color.text_muted));
        }
    }

    private String formatTime(int seconds) {
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(progressRunnable);
    }
}