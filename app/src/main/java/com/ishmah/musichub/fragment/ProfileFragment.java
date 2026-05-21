package com.ishmah.musichub.fragment;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.ishmah.musichub.R;
import com.ishmah.musichub.activity.EditProfileActivity;
import com.ishmah.musichub.activity.PlaylistDetailActivity;
import com.ishmah.musichub.adapter.PlaylistCardAdapter;
import com.ishmah.musichub.db.ArtistDao;
import com.ishmah.musichub.db.FavoriteDao;
import com.ishmah.musichub.db.PlaylistDao;
import com.ishmah.musichub.db.UserProfileDao;
import de.hdodenhof.circleimageview.CircleImageView;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.os.Handler;
import android.os.Looper;

public class ProfileFragment extends Fragment {

    private TextView tvUsername, tvBio, tvFollowing, tvLiked,
            tvPlaylistCount, tvListenHours, tvNewPlaylist;
    private CircleImageView ivAvatar;
    private RecyclerView rvPlaylists;
    private LinearLayout llNoPlaylists;

    private UserProfileDao userProfileDao;
    private ArtistDao artistDao;
    private FavoriteDao favoriteDao;
    private PlaylistDao playlistDao;
    private PlaylistCardAdapter playlistCardAdapter;
    private final List<Map<String, String>> playlists = new ArrayList<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

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
        artistDao      = new ArtistDao(requireContext());
        favoriteDao    = new FavoriteDao(requireContext());
        playlistDao    = new PlaylistDao(requireContext());

        tvUsername     = view.findViewById(R.id.tv_username);
        tvBio          = view.findViewById(R.id.tv_bio);
        tvFollowing    = view.findViewById(R.id.tv_following);
        tvLiked        = view.findViewById(R.id.tv_liked);
        tvPlaylistCount= view.findViewById(R.id.tv_playlist_count);
        tvListenHours  = view.findViewById(R.id.tv_listen_hours);
        tvNewPlaylist  = view.findViewById(R.id.tv_new_playlist);
        ivAvatar       = view.findViewById(R.id.iv_avatar);
        rvPlaylists    = view.findViewById(R.id.rv_playlists);
        llNoPlaylists  = view.findViewById(R.id.ll_no_playlists);

        playlistCardAdapter = new PlaylistCardAdapter(requireContext(), playlists, (playlistId, name) -> {
            Intent intent = new Intent(requireContext(), PlaylistDetailActivity.class);
            intent.putExtra("playlistId", playlistId);
            intent.putExtra("playlistName", name);
            startActivity(intent);
        });
        rvPlaylists.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        rvPlaylists.setAdapter(playlistCardAdapter);

        View.OnClickListener openEdit = v ->
                startActivity(new Intent(requireContext(), EditProfileActivity.class));
        view.findViewById(R.id.btn_settings).setOnClickListener(openEdit);
        view.findViewById(R.id.btn_edit_profile).setOnClickListener(openEdit);

        tvNewPlaylist.setOnClickListener(v -> showNewPlaylistDialog());
    }

    @Override
    public void onResume() {
        super.onResume();
        loadProfile();
        loadPlaylists();
    }

    private void loadProfile() {
        Map<String, String> profile = userProfileDao.getProfile();
        if (profile != null) {
            String name = profile.get("username");
            tvUsername.setText(name != null && !name.isEmpty() ? name : "MusicHub User");
            String bio = profile.get("bio");
            tvBio.setText(bio != null && !bio.isEmpty() ? bio : "Your music, your universe");
            String photoUri = profile.get("photo_uri");
            if (photoUri != null && !photoUri.isEmpty() && isAdded()) {
                Glide.with(this).load(Uri.parse(photoUri)).circleCrop().into(ivAvatar);
            }
        }

        tvFollowing.setText(String.valueOf(artistDao.getFollowingCount()));

        executor.execute(() -> {
            List<Map<String, String>> favorites = favoriteDao.getAllFavorites();
            int likedCount = favorites.size();
            if (getActivity() != null) {
                new Handler(Looper.getMainLooper()).post(() ->
                        tvLiked.setText(String.valueOf(likedCount)));
            }
        });

        userProfileDao.getTotalListeningSeconds(seconds -> {
            if (isAdded() && getActivity() != null) {
                tvListenHours.setText(formatListenTime(seconds));
            }
        });
    }

    private void loadPlaylists() {
        executor.execute(() -> {
            List<Map<String, String>> raw = playlistDao.getAllPlaylists();
            List<Map<String, String>> result = new ArrayList<>();
            for (Map<String, String> p : raw) {
                Map<String, String> item = new HashMap<>(p);
                try {
                    int count = playlistDao.getTrackCount(Integer.parseInt(p.get("playlist_id")));
                    item.put("track_count", String.valueOf(count));
                } catch (Exception e) {
                    item.put("track_count", "0");
                }
                result.add(item);
            }
            if (getActivity() != null) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    playlists.clear();
                    playlists.addAll(result);
                    playlistCardAdapter.notifyDataSetChanged();
                    tvPlaylistCount.setText(String.valueOf(playlists.size()));
                    boolean empty = playlists.isEmpty();
                    llNoPlaylists.setVisibility(empty ? View.VISIBLE : View.GONE);
                    rvPlaylists.setVisibility(empty ? View.GONE : View.VISIBLE);
                });
            }
        });
    }

    private void showNewPlaylistDialog() {
        EditText etName = new EditText(requireContext());
        etName.setHint("Playlist name");
        etName.setInputType(InputType.TYPE_CLASS_TEXT);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        etName.setPadding(pad * 2, pad, pad * 2, pad);

        new AlertDialog.Builder(requireContext())
                .setTitle("New Playlist")
                .setView(etName)
                .setPositiveButton("Create", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    if (name.isEmpty()) name = "My Playlist";
                    String finalName = name;
                    executor.execute(() -> {
                        playlistDao.createPlaylistNow(finalName, "gradient", "purple");
                        if (getActivity() != null) {
                            new Handler(Looper.getMainLooper()).post(this::loadPlaylists);
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private String formatListenTime(int totalSecs) {
        if (totalSecs < 60) return totalSecs + "s";
        long hours   = totalSecs / 3600;
        long minutes = (totalSecs % 3600) / 60;
        long seconds = totalSecs % 60;
        if (hours > 0) return hours + "h " + minutes + "m";
        return minutes + "m " + seconds + "s";
    }
}
