package com.ishmah.musichub.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.ishmah.musichub.R;
import com.ishmah.musichub.adapter.TrackAdapter;
import com.ishmah.musichub.api.ApiConfig;
import com.ishmah.musichub.api.DeezerApi;
import com.ishmah.musichub.api.LastFmApi;
import com.ishmah.musichub.api.NetworkChecker;
import com.ishmah.musichub.api.RetrofitClient;
import com.ishmah.musichub.db.CachedImageDao;
import com.ishmah.musichub.model.Track;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private RecyclerView rvTrending;
    private TrackAdapter trackAdapter;
    private List<Track> trackList = new ArrayList<>();
    private LinearLayout llNoNetwork;
    private Button btnRefresh;
    private TextView tvSectionTitle;
    private TextView chipTrending, chipPop, chipHiphop, chipRnb, chipRock;
    private LastFmApi lastFmApi;
    private DeezerApi deezerApi;
    private CachedImageDao cachedImageDao;
    private ExecutorService executor = Executors.newFixedThreadPool(4);
    private String currentGenre = "trending";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Init views
        rvTrending = view.findViewById(R.id.rv_trending);
        llNoNetwork = view.findViewById(R.id.ll_no_network);
        btnRefresh = view.findViewById(R.id.btn_refresh);
        tvSectionTitle = view.findViewById(R.id.tv_section_title);

        // Init chips
        chipTrending = view.findViewById(R.id.chip_trending);
        chipPop = view.findViewById(R.id.chip_pop);
        chipHiphop = view.findViewById(R.id.chip_hiphop);
        chipRnb = view.findViewById(R.id.chip_rnb);
        chipRock = view.findViewById(R.id.chip_rock);

        // Setup RecyclerView
        trackAdapter = new TrackAdapter(requireContext(), trackList);
        rvTrending.setLayoutManager(new LinearLayoutManager(getContext()));
        rvTrending.setAdapter(trackAdapter);

        // Init API + DB
        lastFmApi = RetrofitClient.getLastFmInstance().create(LastFmApi.class);
        deezerApi = RetrofitClient.getDeezerInstance().create(DeezerApi.class);
        cachedImageDao = new CachedImageDao(requireContext());

        // Chip click listeners
        chipTrending.setOnClickListener(v -> {
            setActiveChip(chipTrending);
            currentGenre = "trending";
            tvSectionTitle.setText("TRENDING");
            loadTrendingTracks();
        });

        chipPop.setOnClickListener(v -> {
            setActiveChip(chipPop);
            currentGenre = "pop";
            tvSectionTitle.setText("POP");
            loadTracksByTag("pop");
        });

        chipHiphop.setOnClickListener(v -> {
            setActiveChip(chipHiphop);
            currentGenre = "hiphop";
            tvSectionTitle.setText("HIP-HOP");
            loadTracksByTag("hip-hop");
        });

        chipRnb.setOnClickListener(v -> {
            setActiveChip(chipRnb);
            currentGenre = "rnb";
            tvSectionTitle.setText("R&B");
            loadTracksByTag("rnb");
        });

        chipRock.setOnClickListener(v -> {
            setActiveChip(chipRock);
            currentGenre = "rock";
            tvSectionTitle.setText("ROCK");
            loadTracksByTag("rock");
        });

        // Refresh button
        btnRefresh.setOnClickListener(v -> loadTrendingTracks());

        // Load default
        loadTrendingTracks();
    }

    private void setActiveChip(TextView activeChip) {
        // Reset semua chip
        TextView[] chips = {chipTrending, chipPop, chipHiphop, chipRnb, chipRock};
        for (TextView chip : chips) {
            chip.setBackground(requireContext().getDrawable(R.drawable.bg_chip_inactive));
            chip.setTextColor(requireContext().getResources().getColor(R.color.text_muted));
        }
        // Set active
        activeChip.setBackground(requireContext().getDrawable(R.drawable.bg_chip_active));
        activeChip.setTextColor(requireContext().getResources().getColor(R.color.purple_soft));
    }

    private void loadTrendingTracks() {
        if (!NetworkChecker.isConnected(requireContext())) {
            showNoNetwork(true);
            return;
        }
        showNoNetwork(false);

        lastFmApi.getTopTracks(ApiConfig.LASTFM_API_KEY, ApiConfig.DEFAULT_LIMIT)
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            parseTracksAndFetchArt(response.body(), "tracks");
                        }
                    }

                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {
                        showNoNetwork(true);
                    }
                });
    }

    private void loadTracksByTag(String tag) {
        if (!NetworkChecker.isConnected(requireContext())) {
            showNoNetwork(true);
            return;
        }
        showNoNetwork(false);

        lastFmApi.getTopTracks(ApiConfig.LASTFM_API_KEY, ApiConfig.DEFAULT_LIMIT)
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            parseTracksAndFetchArt(response.body(), "tracks");
                        }
                    }

                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {
                        showNoNetwork(true);
                    }
                });
    }

    private void parseTracksAndFetchArt(JsonObject json, String rootKey) {
        trackList.clear();
        try {
            JsonArray tracks = json.getAsJsonObject("tracks")
                    .getAsJsonArray("track");

            for (int i = 0; i < tracks.size(); i++) {
                JsonObject t = tracks.get(i).getAsJsonObject();
                String name = t.get("name").getAsString();
                String artist = t.getAsJsonObject("artist").get("name").getAsString();

                String duration = "3:00";
                if (t.has("duration")) {
                    int seconds = t.get("duration").getAsInt();
                    if (seconds > 0) {
                        duration = String.format("%d:%02d", seconds / 60, seconds % 60);
                    }
                }

                Track track = new Track(name, artist, duration, "", name + artist, "", "");
                trackList.add(track);
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> trackAdapter.updateList(trackList));
            }

            // Fetch album art dari Deezer
            for (int i = 0; i < trackList.size(); i++) {
                final int index = i;
                final Track track = trackList.get(i);
                String cacheKey = track.getName() + track.getArtist();

                executor.execute(() -> {
                    var cached = cachedImageDao.getCache(cacheKey);
                    if (cached != null && cached.get("cover_url") != null
                            && !cached.get("cover_url").isEmpty()) {
                        track.setAlbumArt(cached.get("cover_url"));
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() ->
                                    trackAdapter.notifyItemChanged(index));
                        }
                    } else {
                        fetchAlbumArtFromDeezer(track, index, cacheKey);
                    }
                });
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void fetchAlbumArtFromDeezer(Track track, int index, String cacheKey) {
        String query = track.getName() + " " + track.getArtist();
        deezerApi.searchTrack(query).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JsonArray data = response.body().getAsJsonArray("data");
                        if (data != null && data.size() > 0) {
                            JsonObject first = data.get(0).getAsJsonObject();
                            String coverUrl = "";
                            if (first.has("album")) {
                                JsonObject album = first.getAsJsonObject("album");
                                if (album.has("cover_xl")) {
                                    coverUrl = album.get("cover_xl").getAsString();
                                }
                            }
                            if (!coverUrl.isEmpty()) {
                                final String finalUrl = coverUrl;
                                track.setAlbumArt(finalUrl);
                                cachedImageDao.saveCache(cacheKey, finalUrl, "", null);
                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(() ->
                                            trackAdapter.notifyItemChanged(index));
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {}
        });
    }

    private void showNoNetwork(boolean show) {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            llNoNetwork.setVisibility(show ? View.VISIBLE : View.GONE);
            rvTrending.setVisibility(show ? View.GONE : View.VISIBLE);
        });
    }
}