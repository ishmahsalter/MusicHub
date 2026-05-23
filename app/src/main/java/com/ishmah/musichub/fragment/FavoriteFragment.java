package com.ishmah.musichub.fragment;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.ishmah.musichub.R;
import com.ishmah.musichub.activity.AlbumActivity;
import com.ishmah.musichub.activity.DetailActivity;
import com.ishmah.musichub.activity.PlaylistDetailActivity;
import com.ishmah.musichub.adapter.AlbumAdapter;
import com.ishmah.musichub.adapter.FollowingArtistAdapter;
import com.ishmah.musichub.adapter.PlaylistCardAdapter;
import com.ishmah.musichub.ThemeHelper;
import com.ishmah.musichub.adapter.TrackAdapter;
import com.ishmah.musichub.db.ArtistDao;
import com.ishmah.musichub.db.FavoriteDao;
import com.ishmah.musichub.db.PlaylistDao;
import com.ishmah.musichub.model.Track;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FavoriteFragment extends Fragment {

    private static final int TAB_PLAYLISTS = 0;
    private static final int TAB_ALBUMS    = 1;
    private static final int TAB_ARTISTS   = 2;
    private static final int TAB_LIKED     = 3;

    private int currentTab = TAB_PLAYLISTS;

    // Header
    private TextView tvStats;

    // Chips
    private TextView chipPlaylists, chipAlbums, chipArtists, chipLiked;

    // Tab root views
    private View tabPlaylists, tabAlbums, tabArtists, tabLiked;

    // Playlists tab
    private RecyclerView rvPlaylists;
    private TextView tvEmptyPlaylists;
    private PlaylistCardAdapter playlistAdapter;
    private final List<Map<String, String>> playlists = new ArrayList<>();

    // Albums tab
    private RecyclerView rvAlbums;
    private TextView tvEmptyAlbums;
    private AlbumAdapter albumAdapter;
    private final List<Map<String, String>> albums = new ArrayList<>();

    // Artists tab
    private RecyclerView rvArtists;
    private TextView tvEmptyArtists;
    private FollowingArtistAdapter artistAdapter;

    // Liked songs tab
    private RecyclerView rvLiked;
    private TextView tvEmptyLiked, tvLikedCount;
    private LinearLayout btnPlayAll;
    private View dividerLiked;
    private TrackAdapter likedAdapter;
    private List<Track> likedTracks = new ArrayList<>();

    // DAOs
    private PlaylistDao playlistDao;
    private FavoriteDao favoriteDao;
    private ArtistDao artistDao;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_favorite, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        playlistDao = new PlaylistDao(requireContext());
        favoriteDao = new FavoriteDao(requireContext());
        artistDao   = new ArtistDao(requireContext());

        // Header
        tvStats = view.findViewById(R.id.tv_stats);

        // Gold gradient on title
        TextView tvTitle = view.findViewById(R.id.tv_library_title);
        tvTitle.post(() -> {
            if (tvTitle.getWidth() > 0) {
                Shader shader = new LinearGradient(
                        0, 0, tvTitle.getWidth(), 0,
                        new int[]{0xFFD4AF37, 0xFFFFE066, 0xFFD4AF37},
                        null, Shader.TileMode.CLAMP);
                tvTitle.getPaint().setShader(shader);
                tvTitle.invalidate();
            }
        });

        // Chips
        chipPlaylists = view.findViewById(R.id.chip_playlists);
        chipAlbums    = view.findViewById(R.id.chip_albums);
        chipArtists   = view.findViewById(R.id.chip_artists);
        chipLiked     = view.findViewById(R.id.chip_liked);

        // Tab views
        tabPlaylists = view.findViewById(R.id.tab_playlists);
        tabAlbums    = view.findViewById(R.id.tab_albums);
        tabArtists   = view.findViewById(R.id.tab_artists);
        tabLiked     = view.findViewById(R.id.tab_liked);

        // Playlists tab views
        rvPlaylists       = view.findViewById(R.id.rv_playlists);
        tvEmptyPlaylists  = view.findViewById(R.id.tv_empty_playlists);

        // Albums tab views
        rvAlbums      = view.findViewById(R.id.rv_albums);
        tvEmptyAlbums = view.findViewById(R.id.tv_empty_albums);

        // Artists tab views
        rvArtists      = view.findViewById(R.id.rv_artists);
        tvEmptyArtists = view.findViewById(R.id.tv_empty_artists);

        // Liked tab views
        rvLiked      = view.findViewById(R.id.rv_liked);
        tvEmptyLiked = view.findViewById(R.id.tv_empty_liked);
        tvLikedCount = view.findViewById(R.id.tv_liked_count);
        btnPlayAll   = view.findViewById(R.id.btn_play_all);
        dividerLiked = view.findViewById(R.id.divider_liked);

        setupAdapters();
        setupListeners(view);

        switchTab(TAB_PLAYLISTS);
        loadAllData();
    }

    private void setupAdapters() {
        // Playlists — 2-column grid
        playlistAdapter = new PlaylistCardAdapter(
                requireContext(), playlists,
                (playlistId, name) -> {
                    Intent intent = new Intent(requireContext(),
                            PlaylistDetailActivity.class);
                    intent.putExtra("playlistId", playlistId);
                    intent.putExtra("playlistName", name);
                    startActivity(intent);
                });
        rvPlaylists.setLayoutManager(
                new GridLayoutManager(requireContext(), 2));
        rvPlaylists.setAdapter(playlistAdapter);

        // Albums — 2-column grid
        albumAdapter = new AlbumAdapter(requireContext(), albums,
                album -> {
                    Intent intent = new Intent(requireContext(),
                            AlbumActivity.class);
                    intent.putExtra("album_name",  album.get("name"));
                    intent.putExtra("artist_name", album.get("artist"));
                    intent.putExtra("album_art",   album.get("image"));
                    startActivity(intent);
                });
        rvAlbums.setLayoutManager(
                new GridLayoutManager(requireContext(), 2));
        rvAlbums.setAdapter(albumAdapter);

        // Artists — vertical list
        artistAdapter = new FollowingArtistAdapter(
                requireContext(), artistDao);
        rvArtists.setLayoutManager(
                new LinearLayoutManager(requireContext()));
        rvArtists.setAdapter(artistAdapter);

        // Liked songs — vertical list
        likedAdapter = new TrackAdapter(requireContext(), likedTracks);
        rvLiked.setLayoutManager(
                new LinearLayoutManager(requireContext()));
        rvLiked.setAdapter(likedAdapter);
    }

    private void setupListeners(View view) {
        chipPlaylists.setOnClickListener(v -> switchTab(TAB_PLAYLISTS));
        chipAlbums.setOnClickListener(v -> switchTab(TAB_ALBUMS));
        chipArtists.setOnClickListener(v -> switchTab(TAB_ARTISTS));
        chipLiked.setOnClickListener(v -> switchTab(TAB_LIKED));

        view.findViewById(R.id.btn_new_playlist)
                .setOnClickListener(v -> showCreatePlaylistDialog());

        btnPlayAll.setOnClickListener(v -> {
            if (!likedTracks.isEmpty()) {
                Track first = likedTracks.get(0);
                Intent intent = new Intent(requireContext(), DetailActivity.class);
                intent.putExtra("trackName",  first.getName());
                intent.putExtra("artistName", first.getArtist());
                intent.putExtra("trackId",    first.getTrackId());
                intent.putExtra("albumArt",
                        first.getAlbumArt() != null ? first.getAlbumArt() : "");
                intent.putExtra("previewUrl",
                        first.getPreviewUrl() != null ? first.getPreviewUrl() : "");
                startActivity(intent);
            }
        });
    }

    private void switchTab(int tab) {
        currentTab = tab;
        tabPlaylists.setVisibility(tab == TAB_PLAYLISTS ? View.VISIBLE : View.GONE);
        tabAlbums.setVisibility(tab == TAB_ALBUMS       ? View.VISIBLE : View.GONE);
        tabArtists.setVisibility(tab == TAB_ARTISTS     ? View.VISIBLE : View.GONE);
        tabLiked.setVisibility(tab == TAB_LIKED         ? View.VISIBLE : View.GONE);
        updateChipStyles(tab);
    }

    private void updateChipStyles(int active) {
        int accentColor = ThemeHelper.getAccentColor(requireContext());
        int mutedColor  = ContextCompat.getColor(
                requireContext(), R.color.text_secondary);

        setChipStyle(chipPlaylists, active == TAB_PLAYLISTS, accentColor, mutedColor);
        setChipStyle(chipAlbums,    active == TAB_ALBUMS,    accentColor, mutedColor);
        setChipStyle(chipArtists,   active == TAB_ARTISTS,   accentColor, mutedColor);
        setChipStyle(chipLiked,     active == TAB_LIKED,     accentColor, mutedColor);
    }

    private void setChipStyle(TextView chip, boolean active,
                               int accentColor, int mutedColor) {
        chip.setBackgroundResource(active
                ? R.drawable.bg_chip_active
                : R.drawable.bg_chip_inactive);
        chip.setTextColor(active ? accentColor : mutedColor);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadAllData();
    }

    private void loadAllData() {
        executor.execute(() -> {
            // ── Playlists with track counts ──
            List<Map<String, String>> rawPlaylists =
                    playlistDao.getAllPlaylists();
            for (Map<String, String> p : rawPlaylists) {
                try {
                    int id = Integer.parseInt(p.get("playlist_id"));
                    p.put("track_count",
                            String.valueOf(playlistDao.getTrackCount(id)));
                } catch (Exception ignored) {
                    p.put("track_count", "0");
                }
            }

            // ── Liked songs + derive albums ──
            List<Map<String, String>> favs =
                    favoriteDao.getAllFavorites();
            List<Track> tracks = new ArrayList<>();
            // LinkedHashMap preserves insertion order for album dedup
            Map<String, Map<String, String>> albumMap = new LinkedHashMap<>();
            int totalSeconds = 0;

            for (Map<String, String> fav : favs) {
                String name     = nullSafe(fav.get("name"));
                String artist   = nullSafe(fav.get("artist"));
                String albumArt = nullSafe(fav.get("albumArt"));
                String trackId  = nullSafe(fav.get("trackId"));
                String dur      = nullSafe(fav.get("duration"));

                tracks.add(new Track(name, artist, dur, albumArt, trackId, "", ""));
                totalSeconds += parseDuration(dur);

                String albumKey = artist + "|" + albumArt;
                if (!albumMap.containsKey(albumKey) && !artist.isEmpty()) {
                    Map<String, String> album = new HashMap<>();
                    album.put("name",   artist);
                    album.put("image",  albumArt);
                    album.put("artist", artist);
                    albumMap.put(albumKey, album);
                }
            }

            List<Map<String, String>> albumList =
                    new ArrayList<>(albumMap.values());

            // ── Following artists ──
            List<Map<String, String>> artists =
                    artistDao.getAllFollowing();

            // ── Stats string ──
            int totalMin = totalSeconds / 60;
            int totalHr  = totalMin / 60;
            int remMin   = totalMin % 60;
            String stats;
            if (totalHr > 0) {
                stats = tracks.size() + " songs · "
                        + totalHr + "h " + remMin + "m";
            } else {
                stats = tracks.size() + " songs · " + totalMin + "m";
            }

            // ── Post to UI ──
            String finalStats = stats;
            List<Map<String, String>> finalPlaylists = rawPlaylists;
            List<Track> finalTracks = tracks;
            List<Map<String, String>> finalAlbums = albumList;
            List<Map<String, String>> finalArtists = artists;

            mainHandler.post(() -> {
                if (!isAdded()) return;

                tvStats.setText(finalStats);

                // Playlists
                playlists.clear();
                playlists.addAll(finalPlaylists);
                playlistAdapter.notifyDataSetChanged();
                boolean noPlaylists = playlists.isEmpty();
                tvEmptyPlaylists.setVisibility(
                        noPlaylists ? View.VISIBLE : View.GONE);
                rvPlaylists.setVisibility(
                        noPlaylists ? View.GONE : View.VISIBLE);

                // Liked songs
                likedTracks = new ArrayList<>(finalTracks);
                likedAdapter.updateList(likedTracks);
                boolean noLiked = likedTracks.isEmpty();
                tvEmptyLiked.setVisibility(
                        noLiked ? View.VISIBLE : View.GONE);
                rvLiked.setVisibility(
                        noLiked ? View.GONE : View.VISIBLE);
                btnPlayAll.setVisibility(
                        noLiked ? View.GONE : View.VISIBLE);
                dividerLiked.setVisibility(
                        noLiked ? View.GONE : View.VISIBLE);
                if (!noLiked) {
                    tvLikedCount.setText(likedTracks.size() + " songs");
                }

                // Albums
                albums.clear();
                albums.addAll(finalAlbums);
                albumAdapter.notifyDataSetChanged();
                boolean noAlbums = albums.isEmpty();
                tvEmptyAlbums.setVisibility(
                        noAlbums ? View.VISIBLE : View.GONE);
                rvAlbums.setVisibility(
                        noAlbums ? View.GONE : View.VISIBLE);

                // Artists
                artistAdapter.updateList(finalArtists);
                boolean noArtists = finalArtists.isEmpty();
                tvEmptyArtists.setVisibility(
                        noArtists ? View.VISIBLE : View.GONE);
                rvArtists.setVisibility(
                        noArtists ? View.GONE : View.VISIBLE);
            });
        });
    }

    private void showCreatePlaylistDialog() {
        EditText et = new EditText(requireContext());
        et.setHint("Playlist name...");
        et.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.text_primary));
        et.setHintTextColor(
                ContextCompat.getColor(requireContext(), R.color.text_hint));
        et.setPadding(48, 32, 48, 32);

        new AlertDialog.Builder(requireContext())
                .setTitle("New Playlist")
                .setView(et)
                .setPositiveButton("Create", (d, w) -> {
                    String name = et.getText().toString().trim();
                    if (!name.isEmpty()) {
                        executor.execute(() -> {
                            playlistDao.createPlaylistNow(
                                    name, "gradient", "purple");
                            mainHandler.post(this::loadAllData);
                        });
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private static int parseDuration(String dur) {
        if (dur == null || dur.isEmpty()) return 30;
        if (dur.contains(":")) {
            try {
                String[] p = dur.split(":");
                return Integer.parseInt(p[0]) * 60
                        + Integer.parseInt(p[1]);
            } catch (Exception ignored) {}
        }
        try {
            return Integer.parseInt(dur.trim());
        } catch (Exception ignored) {}
        return 30;
    }

    private static String nullSafe(String s) {
        return s != null ? s : "";
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
