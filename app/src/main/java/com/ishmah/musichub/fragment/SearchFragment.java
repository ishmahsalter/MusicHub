package com.ishmah.musichub.fragment;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
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

public class SearchFragment extends Fragment {

    private EditText etSearch;
    private RecyclerView rvSearchResults;
    private LinearLayout llNoNetwork;
    private Button btnRefresh;
    private TrackAdapter trackAdapter;
    private List<Track> trackList = new ArrayList<>();
    private LastFmApi lastFmApi;
    private DeezerApi deezerApi;
    private CachedImageDao cachedImageDao;
    private ExecutorService executor = Executors.newFixedThreadPool(4);
    private String lastQuery = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etSearch = view.findViewById(R.id.et_search);
        rvSearchResults = view.findViewById(R.id.rv_search_results);
        llNoNetwork = view.findViewById(R.id.ll_no_network);
        btnRefresh = view.findViewById(R.id.btn_refresh);

        trackAdapter = new TrackAdapter(requireContext(), trackList);
        rvSearchResults.setLayoutManager(new LinearLayoutManager(getContext()));
        rvSearchResults.setAdapter(trackAdapter);

        lastFmApi = RetrofitClient.getLastFmInstance().create(LastFmApi.class);
        deezerApi = RetrofitClient.getDeezerInstance().create(DeezerApi.class);
        cachedImageDao = new CachedImageDao(requireContext());

        // Search on keyboard search button
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                String query = etSearch.getText().toString().trim();
                if (!query.isEmpty()) {
                    lastQuery = query;
                    searchTracks(query);
                }
                return true;
            }
            return false;
        });

        // Refresh button
        btnRefresh.setOnClickListener(v -> {
            if (!lastQuery.isEmpty()) {
                searchTracks(lastQuery);
            }
        });
    }

    private void searchTracks(String query) {
        if (!NetworkChecker.isConnected(requireContext())) {
            showNoNetwork(true);
            return;
        }
        showNoNetwork(false);

        lastFmApi.searchTrack(ApiConfig.LASTFM_API_KEY, query, ApiConfig.DEFAULT_LIMIT)
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            parseSearchResults(response.body());
                        }
                    }

                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {
                        showNoNetwork(true);
                    }
                });
    }

    private void parseSearchResults(JsonObject json) {
        trackList.clear();
        try {
            JsonObject results = json.getAsJsonObject("results");
            JsonObject trackMatches = results.getAsJsonObject("trackmatches");
            JsonArray tracks = trackMatches.getAsJsonArray("track");

            for (int i = 0; i < tracks.size(); i++) {
                JsonObject t = tracks.get(i).getAsJsonObject();
                String name = t.get("name").getAsString();
                String artist = t.get("artist").getAsString();

                Track track = new Track(name, artist, "", "", name + artist, "", "");
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
            rvSearchResults.setVisibility(show ? View.GONE : View.VISIBLE);
        });
    }
}