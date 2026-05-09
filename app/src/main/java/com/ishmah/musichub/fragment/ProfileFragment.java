package com.ishmah.musichub.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.ishmah.musichub.R;
import com.ishmah.musichub.db.ArtistDao;
import com.ishmah.musichub.db.UserProfileDao;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private TextView tvUsername, tvBio, tvFollowing, tvFollowers, tvLiked;
    private UserProfileDao userProfileDao;
    private ArtistDao artistDao;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        userProfileDao = new UserProfileDao(requireContext());
        artistDao = new ArtistDao(requireContext());

        tvUsername = view.findViewById(R.id.tv_username);
        tvBio = view.findViewById(R.id.tv_bio);
        tvFollowing = view.findViewById(R.id.tv_following);
        tvFollowers = view.findViewById(R.id.tv_followers);
        tvLiked = view.findViewById(R.id.tv_liked);

        loadProfile();
    }

    private void loadProfile() {
        // Load profile dari SQLite
        Map<String, String> profile = userProfileDao.getProfile();
        if (profile != null) {
            tvUsername.setText(profile.get("username"));
            tvBio.setText(profile.get("bio"));
        } else {
            tvUsername.setText("MusicHub User");
            tvBio.setText("Your music, your universe 🎵");
        }

        // Following count — REAL dari SQLite
        int followingCount = artistDao.getFollowingCount();
        tvFollowing.setText(String.valueOf(followingCount));

        // Followers — dummy
        tvFollowers.setText("1.2K");
    }
}