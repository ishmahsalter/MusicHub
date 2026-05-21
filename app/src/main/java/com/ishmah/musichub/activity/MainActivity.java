package com.ishmah.musichub.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.cardview.widget.CardView;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.ishmah.musichub.MusicPlayerManager;
import com.ishmah.musichub.R;
import com.ishmah.musichub.ThemeHelper;

public class MainActivity extends AppCompatActivity
        implements MusicPlayerManager.OnPlayerStateChangedListener {

    private NavController navController;
    private LinearLayout navHome, navSearch, navFavorite, navProfile;
    private ImageView navFab, ivNpArt, btnNpPlay, btnNpPrev, btnNpNext;
    private TextView tvNpTrack, tvNpArtist;
    private ProgressBar pbNp;
    private CardView cardNowPlaying;
    private MusicPlayerManager playerManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeHelper.apply(this);
        setContentView(R.layout.activity_main);

        playerManager = MusicPlayerManager.getInstance();
        playerManager.init(this);

        NavHostFragment navHostFragment = (NavHostFragment)
                getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host_fragment);
        navController = navHostFragment.getNavController();

        // Bottom nav
        navHome = findViewById(R.id.nav_home);
        navSearch = findViewById(R.id.nav_search);
        navFavorite = findViewById(R.id.nav_favorite);
        navProfile = findViewById(R.id.nav_profile);
        navFab = findViewById(R.id.nav_fab);

        // NOW PLAYING card
        cardNowPlaying = findViewById(R.id.card_now_playing);
        ivNpArt = findViewById(R.id.iv_np_art);
        btnNpPlay = findViewById(R.id.btn_np_play);
        btnNpPrev = findViewById(R.id.btn_np_prev);
        btnNpNext = findViewById(R.id.btn_np_next);
        tvNpTrack = findViewById(R.id.tv_np_track);
        tvNpArtist = findViewById(R.id.tv_np_artist);
        pbNp = findViewById(R.id.pb_np);

        setupNavClicks();
        setActiveNav(navHome);

        // NOW PLAYING card click → DetailActivity
        cardNowPlaying.setOnClickListener(v -> {
            if (playerManager.hasTrack()) {
                Intent intent = new Intent(this, DetailActivity.class);
                intent.putExtra("trackName",
                        playerManager.getCurrentTrackName());
                intent.putExtra("artistName",
                        playerManager.getCurrentArtistName());
                intent.putExtra("trackId",
                        playerManager.getCurrentTrackId());
                intent.putExtra("albumArt",
                        playerManager.getCurrentAlbumArt());
                intent.putExtra("previewUrl",
                        playerManager.getCurrentPreviewUrl());
                startActivity(intent);
            }
        });

        // Play button NOW PLAYING card
        btnNpPlay.setOnClickListener(v -> {
            if (playerManager.hasTrack()) playerManager.togglePlayPause();
        });

        // Prev
        btnNpPrev.setOnClickListener(v -> {
            if (playerManager.hasPlaylist()) {
                playerManager.playPrev();
            } else if (playerManager.hasTrack()) {
                playerManager.seekTo(0);
            }
        });

        // Next
        btnNpNext.setOnClickListener(v -> {
            if (playerManager.hasPlaylist()) {
                playerManager.playNext();
            }
        });

        // FAB
        navFab.setOnClickListener(v -> {
            if (playerManager.hasTrack()) {
                playerManager.togglePlayPause();
            } else {
                navController.navigate(R.id.homeFragment);
                setActiveNav(navHome);
            }
        });

        // Sync kalau sudah ada track
        if (playerManager.hasTrack()) {
            cardNowPlaying.setVisibility(View.VISIBLE);
            updateNowPlayingCard(
                    playerManager.getCurrentTrackName(),
                    playerManager.getCurrentArtistName(),
                    playerManager.getCurrentAlbumArt(),
                    playerManager.getCurrentTrackId());
        }

        // Add listener
        playerManager.addListener(this);
    }

    private void setupNavClicks() {
        navHome.setOnClickListener(v -> {
            navController.navigate(R.id.homeFragment);
            setActiveNav(navHome);
        });
        navSearch.setOnClickListener(v -> {
            navController.navigate(R.id.searchFragment);
            setActiveNav(navSearch);
        });
        navFavorite.setOnClickListener(v -> {
            navController.navigate(R.id.favoriteFragment);
            setActiveNav(navFavorite);
        });
        navProfile.setOnClickListener(v -> {
            navController.navigate(R.id.profileFragment);
            setActiveNav(navProfile);
        });
    }

    @Override
    public void onTrackChanged(String trackName, String artistName,
                               String albumArt, String trackId) {
        runOnUiThread(() -> {
            cardNowPlaying.setVisibility(View.VISIBLE);
            updateNowPlayingCard(trackName, artistName, albumArt, trackId);
        });
    }

    @Override
    public void onPlayStateChanged(boolean isPlaying) {
        runOnUiThread(() -> {
            if (isPlaying) {
                btnNpPlay.setImageResource(R.drawable.ic_pause);
                navFab.setImageResource(R.drawable.ic_pause);
            } else {
                btnNpPlay.setImageResource(R.drawable.ic_play);
                navFab.setImageResource(R.drawable.ic_play);
            }
        });
    }

    @Override
    public void onProgressChanged(int progress, int total) {
        runOnUiThread(() -> {
            pbNp.setMax(total > 0 ? total : 30);
            pbNp.setProgress(progress);
        });
    }

    private void updateNowPlayingCard(String trackName, String artistName,
                                      String albumArt, String trackId) {
        tvNpTrack.setText(trackName);
        tvNpArtist.setText(artistName);

        if (albumArt != null && !albumArt.isEmpty()) {
            Glide.with(this)
                    .load(albumArt)
                    .apply(RequestOptions.bitmapTransform(
                            new RoundedCorners(16)))
                    .placeholder(R.color.bg_card)
                    .into(ivNpArt);
        }
    }

    private void setActiveNav(LinearLayout active) {
        boolean isMidnight = ThemeHelper.isMidnight(this);
        boolean isGoldRush = ThemeHelper.isGoldRush(this);
        int activeColor = isMidnight
                ? 0xFF4F6EF7
                : getResources().getColor(R.color.purple_glow);
        int inactiveColor = isGoldRush
                ? getResources().getColor(R.color.text_muted)
                : getResources().getColor(R.color.white);
        float inactiveAlpha = isGoldRush ? 1f : 0.35f;

        LinearLayout[] navItems = {navHome, navSearch,
                navFavorite, navProfile};
        for (LinearLayout item : navItems) {
            if (item == active) {
                item.setAlpha(1f);
                ((ImageView) item.getChildAt(0)).setColorFilter(activeColor);
                ((TextView) item.getChildAt(1)).setTextColor(activeColor);
            } else {
                item.setAlpha(inactiveAlpha);
                ((ImageView) item.getChildAt(0)).setColorFilter(inactiveColor);
                ((TextView) item.getChildAt(1)).setTextColor(inactiveColor);
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        String tab = intent.getStringExtra("nav_tab");
        if (tab != null) {
            switch (tab) {
                case "search":
                    navController.navigate(R.id.searchFragment);
                    setActiveNav(navSearch);
                    break;
                case "favorite":
                    navController.navigate(R.id.favoriteFragment);
                    setActiveNav(navFavorite);
                    break;
                case "profile":
                    navController.navigate(R.id.profileFragment);
                    setActiveNav(navProfile);
                    break;
                default:
                    navController.navigate(R.id.homeFragment);
                    setActiveNav(navHome);
                    break;
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        playerManager.addListener(this);
        if (playerManager.hasTrack()) {
            cardNowPlaying.setVisibility(View.VISIBLE);
            updateNowPlayingCard(
                    playerManager.getCurrentTrackName(),
                    playerManager.getCurrentArtistName(),
                    playerManager.getCurrentAlbumArt(),
                    playerManager.getCurrentTrackId());
            onPlayStateChanged(playerManager.isPlaying());
            onProgressChanged(
                    playerManager.getCurrentProgress(),
                    playerManager.getTotalDuration());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Jangan remove listener — biar tetap update saat di background
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        playerManager.removeListener(this);
    }
}