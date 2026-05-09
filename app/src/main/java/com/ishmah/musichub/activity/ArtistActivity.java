package com.ishmah.musichub.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.ishmah.musichub.R;
import com.ishmah.musichub.db.ArtistDao;

public class ArtistActivity extends AppCompatActivity {

    private TextView tvArtistName, tvBio, tvFollowers, tvFollowing, tvStreams;
    private ImageView ivArtistPhoto;
    private ImageButton btnBack, btnFollow, btnShare;
    private RecyclerView rvDiscography;
    private ArtistDao artistDao;
    private String artistId, artistName, artistPhoto, genre;
    private boolean isFollowing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artist);

        artistDao = new ArtistDao(this);
        initViews();
        receiveIntent();
        setupClickListeners();
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
        rvDiscography.setLayoutManager(new LinearLayoutManager(this));
    }

    private void receiveIntent() {
        Intent intent = getIntent();
        if (intent != null) {
            artistId = intent.getStringExtra("artistId");
            artistName = intent.getStringExtra("artistName");
            artistPhoto = intent.getStringExtra("artistPhoto");
            genre = intent.getStringExtra("genre");

            if (artistName != null) tvArtistName.setText(artistName);

            // Cek status follow
            if (artistId != null) {
                isFollowing = artistDao.isFollowing(artistId);
                updateFollowButton();
            }
        }
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