package com.ishmah.musichub.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextWatcher;
import android.text.Editable;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.ishmah.musichub.R;
import com.ishmah.musichub.activity.ArtistActivity;
import com.ishmah.musichub.adapter.ArtistChipAdapter;
import com.ishmah.musichub.adapter.ChartCardAdapter;
import com.ishmah.musichub.adapter.TrackAdapter;
import com.ishmah.musichub.api.ApiConfig;
import com.ishmah.musichub.api.DeezerApi;
import com.ishmah.musichub.api.LastFmApi;
import com.ishmah.musichub.api.NetworkChecker;
import com.ishmah.musichub.api.RetrofitClient;
import com.ishmah.musichub.model.Track;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchFragment extends Fragment {

    private EditText etSearch;
    private RecyclerView rvSearchResults, rvCharts, rvArtists, rvNewReleases;
    private LinearLayout llNoNetwork, llDiscovery, llResults;
    private Button btnRefresh;
    private TextView tvSearchSection;

    // Search results
    private TrackAdapter searchAdapter;
    private List<Track> searchList = new ArrayList<>();

    // Charts
    private ChartCardAdapter chartAdapter;
    private List<Track> chartList = new ArrayList<>();

    // Artists
    private ArtistChipAdapter artistAdapter;
    private List<Map<String, String>> artistList = new ArrayList<>();

    // New Releases
    private TrackAdapter releasesAdapter;
    private List<Track> releasesList = new ArrayList<>();

    private LastFmApi lastFmApi;
    private DeezerApi deezerApi;
    private String lastQuery = "";
    // Prevents TextWatcher from firing a title-search when a genre chip sets the search bar text
    private boolean suppressSearch = false;

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
        rvCharts = view.findViewById(R.id.rv_charts);
        rvArtists = view.findViewById(R.id.rv_artists);
        rvNewReleases = view.findViewById(R.id.rv_new_releases);
        llNoNetwork = view.findViewById(R.id.ll_no_network);
        llDiscovery = view.findViewById(R.id.ll_discovery);
        llResults = view.findViewById(R.id.ll_results);
        btnRefresh = view.findViewById(R.id.btn_refresh);
        tvSearchSection = view.findViewById(R.id.tv_search_section);

        lastFmApi = RetrofitClient.getLastFmInstance().create(LastFmApi.class);
        deezerApi = RetrofitClient.getDeezerInstance().create(DeezerApi.class);

        // Search results adapter
        searchAdapter = new TrackAdapter(requireContext(), searchList);
        rvSearchResults.setLayoutManager(new LinearLayoutManager(getContext()));
        rvSearchResults.setAdapter(searchAdapter);

        // Charts adapter (horizontal)
        chartAdapter = new ChartCardAdapter(requireContext(), chartList);
        LinearLayoutManager chartsLM = new LinearLayoutManager(
                getContext(), LinearLayoutManager.HORIZONTAL, false);
        rvCharts.setLayoutManager(chartsLM);
        rvCharts.setAdapter(chartAdapter);

        // Artists adapter (horizontal)
        artistAdapter = new ArtistChipAdapter(requireContext(), artistList);
        LinearLayoutManager artistsLM = new LinearLayoutManager(
                getContext(), LinearLayoutManager.HORIZONTAL, false);
        rvArtists.setLayoutManager(artistsLM);
        rvArtists.setAdapter(artistAdapter);

        // New Releases adapter
        releasesAdapter = new TrackAdapter(requireContext(), releasesList);
        rvNewReleases.setLayoutManager(new LinearLayoutManager(getContext()));
        rvNewReleases.setAdapter(releasesAdapter);

        // Genre chips
        setupGenreChips(view);

        // Search input
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                String q = etSearch.getText().toString().trim();
                if (!q.isEmpty()) { lastQuery = q; performSearch(q); }
                return true;
            }
            return false;
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (suppressSearch) return; // chip tap already triggered searchByGenre — skip
                String q = s.toString().trim();
                if (q.length() >= 2) {
                    lastQuery = q;
                    showMode(false);
                    performSearch(q);
                } else if (q.isEmpty()) {
                    showMode(true);
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnRefresh.setOnClickListener(v -> {
            showNoNetwork(false);
            if (!lastQuery.isEmpty()) performSearch(lastQuery);
            else loadDiscovery();
        });

        // Intercept back: return to discovery if results are showing, else navigate normally
        requireActivity().getOnBackPressedDispatcher().addCallback(
                getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        if (llResults.getVisibility() == View.VISIBLE) {
                            etSearch.setText("");
                            showMode(true);
                        } else {
                            setEnabled(false);
                            requireActivity().getOnBackPressedDispatcher().onBackPressed();
                        }
                    }
                });

        loadDiscovery();
    }

    private void showMode(boolean discovery) {
        llDiscovery.setVisibility(discovery ? View.VISIBLE : View.GONE);
        llResults.setVisibility(discovery ? View.GONE : View.VISIBLE);
    }

    private void setupGenreChips(View view) {
        int[] chipIds = {
                R.id.chip_genre_pop, R.id.chip_genre_hiphop,
                R.id.chip_genre_rnb, R.id.chip_genre_rock,
                R.id.chip_genre_electronic, R.id.chip_genre_jazz,
                R.id.chip_genre_classical, R.id.chip_genre_latin
        };
        String[] tags = {
                "pop", "hip hop", "rnb", "rock",
                "electronic", "jazz", "classical", "latin"
        };
        String[] labels = {
                "Pop", "Hip-Hop", "R&B", "Rock",
                "Electronic", "Jazz", "Classical", "Latin"
        };

        for (int i = 0; i < chipIds.length; i++) {
            final String tag = tags[i];
            final String label = labels[i];
            TextView chip = view.findViewById(chipIds[i]);
            chip.setOnClickListener(v -> {
                // Suppress TextWatcher so it does not fire performSearch(label) as a title search
                suppressSearch = true;
                etSearch.setText(label);
                suppressSearch = false;
                etSearch.clearFocus();
                showMode(false);
                tvSearchSection.setText("🎵 TOP " + label.toUpperCase() + " TRACKS");
                searchByGenre(tag);
            });
        }
    }

    private void loadDiscovery() {
        if (!NetworkChecker.isConnected(requireContext())) {
            showNoNetwork(true);
            return;
        }
        showNoNetwork(false);
        showMode(true);
        loadCharts();
        loadTopArtists();
        loadNewReleases();
    }

    private void loadCharts() {
        lastFmApi.getTopTracks(ApiConfig.LASTFM_API_KEY, 10)
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        if (!response.isSuccessful() || response.body() == null) return;
                        try {
                            JsonArray tracks = response.body()
                                    .getAsJsonObject("tracks").getAsJsonArray("track");
                            chartList.clear();
                            for (int i = 0; i < tracks.size(); i++) {
                                JsonObject t = tracks.get(i).getAsJsonObject();
                                String name = t.get("name").getAsString();
                                String artist = t.getAsJsonObject("artist").get("name").getAsString();
                                String plays = t.has("playcount") ? t.get("playcount").getAsString() : "";
                                Track track = new Track(name, artist, "0:30",
                                        "", name + artist, plays, "");
                                chartList.add(track);
                            }
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> chartAdapter.notifyDataSetChanged());
                            }
                            fetchDeezerForCharts();
                        } catch (Exception ignored) {}
                    }
                    @Override public void onFailure(Call<JsonObject> call, Throwable t) {}
                });
    }

    private void fetchDeezerForCharts() {
        for (int i = 0; i < chartList.size(); i++) {
            final int idx = i;
            final Track track = chartList.get(i);
            deezerApi.searchTrack(track.getName() + " " + track.getArtist())
                    .enqueue(new Callback<JsonObject>() {
                        @Override
                        public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                            if (!response.isSuccessful() || response.body() == null) return;
                            try {
                                JsonArray data = response.body().getAsJsonArray("data");
                                if (data == null || data.size() == 0) return;
                                JsonObject first = data.get(0).getAsJsonObject();
                                if (first.has("album")) {
                                    String art = first.getAsJsonObject("album")
                                            .get("cover_xl").getAsString();
                                    track.setAlbumArt(art);
                                }
                                if (first.has("preview")) track.setPreviewUrl(first.get("preview").getAsString());
                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(() -> chartAdapter.notifyItemChanged(idx));
                                }
                            } catch (Exception ignored) {}
                        }
                        @Override public void onFailure(Call<JsonObject> call, Throwable t) {}
                    });
        }
    }

    private void loadTopArtists() {
        lastFmApi.getTopArtists(ApiConfig.LASTFM_API_KEY, 12)
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        if (!response.isSuccessful() || response.body() == null) return;
                        try {
                            JsonArray artists = response.body()
                                    .getAsJsonObject("artists").getAsJsonArray("artist");
                            artistList.clear();
                            for (int i = 0; i < artists.size(); i++) {
                                JsonObject a = artists.get(i).getAsJsonObject();
                                String name = a.get("name").getAsString();
                                Map<String, String> map = new HashMap<>();
                                map.put("name", name);
                                map.put("photo", "");
                                artistList.add(map);
                            }
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> artistAdapter.notifyDataSetChanged());
                            }
                            fetchDeezerForArtists();
                        } catch (Exception ignored) {}
                    }
                    @Override public void onFailure(Call<JsonObject> call, Throwable t) {}
                });
    }

    private void fetchDeezerForArtists() {
        for (int i = 0; i < artistList.size(); i++) {
            final int idx = i;
            final Map<String, String> artist = artistList.get(i);
            deezerApi.searchArtist(artist.get("name"))
                    .enqueue(new Callback<JsonObject>() {
                        @Override
                        public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                            if (!response.isSuccessful() || response.body() == null) return;
                            try {
                                JsonArray data = response.body().getAsJsonArray("data");
                                if (data == null || data.size() == 0) return;
                                JsonObject first = data.get(0).getAsJsonObject();
                                if (first.has("picture_medium")) {
                                    artist.put("photo", first.get("picture_medium").getAsString());
                                }
                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(() -> artistAdapter.notifyItemChanged(idx));
                                }
                            } catch (Exception ignored) {}
                        }
                        @Override public void onFailure(Call<JsonObject> call, Throwable t) {}
                    });
        }
    }

    private void loadNewReleases() {
        lastFmApi.getTopTracks(ApiConfig.LASTFM_API_KEY, 15)
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        if (!response.isSuccessful() || response.body() == null) return;
                        try {
                            JsonArray tracks = response.body()
                                    .getAsJsonObject("tracks").getAsJsonArray("track");
                            // Use tracks 10-25 to differentiate from charts
                            releasesList.clear();
                            for (int i = 0; i < tracks.size(); i++) {
                                JsonObject t = tracks.get(i).getAsJsonObject();
                                String name = t.get("name").getAsString();
                                String artist = t.getAsJsonObject("artist").get("name").getAsString();
                                releasesList.add(new Track(name, artist, "0:30",
                                        "", name + artist, "", ""));
                            }
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> releasesAdapter.notifyDataSetChanged());
                            }
                            fetchDeezerForReleases();
                        } catch (Exception ignored) {}
                    }
                    @Override public void onFailure(Call<JsonObject> call, Throwable t) {}
                });
    }

    private void fetchDeezerForReleases() {
        for (int i = 0; i < releasesList.size(); i++) {
            final int idx = i;
            final Track track = releasesList.get(i);
            deezerApi.searchTrack(track.getName() + " " + track.getArtist())
                    .enqueue(new Callback<JsonObject>() {
                        @Override
                        public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                            if (!response.isSuccessful() || response.body() == null) return;
                            try {
                                JsonArray data = response.body().getAsJsonArray("data");
                                if (data == null || data.size() == 0) return;
                                JsonObject first = data.get(0).getAsJsonObject();
                                if (first.has("album")) {
                                    track.setAlbumArt(first.getAsJsonObject("album")
                                            .get("cover_xl").getAsString());
                                }
                                if (first.has("preview")) track.setPreviewUrl(first.get("preview").getAsString());
                                if (first.has("duration")) {
                                    int dur = first.get("duration").getAsInt();
                                    track.setDuration(String.format("%d:%02d", dur / 60, dur % 60));
                                }
                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(() -> releasesAdapter.notifyItemChanged(idx));
                                }
                            } catch (Exception ignored) {}
                        }
                        @Override public void onFailure(Call<JsonObject> call, Throwable t) {}
                    });
        }
    }

    private void performSearch(String query) {
        if (!NetworkChecker.isConnected(requireContext())) {
            showNoNetwork(true);
            return;
        }
        showNoNetwork(false);
        tvSearchSection.setText("🔍 RESULTS FOR \"" + query.toUpperCase() + "\"");

        lastFmApi.searchTrack(ApiConfig.LASTFM_API_KEY, query, 20)
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        if (!response.isSuccessful() || response.body() == null) return;
                        parseSearchResults(response.body());
                    }
                    @Override public void onFailure(Call<JsonObject> call, Throwable t) {
                        showNoNetwork(true);
                    }
                });
    }

    private void searchByGenre(String tag) {
        if (!NetworkChecker.isConnected(requireContext())) {
            showNoNetwork(true);
            return;
        }
        showNoNetwork(false);

        lastFmApi.getTagTopTracks(ApiConfig.LASTFM_API_KEY, tag, 20)
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        if (!response.isSuccessful() || response.body() == null) return;
                        try {
                            JsonArray tracks = response.body()
                                    .getAsJsonObject("tracks").getAsJsonArray("track");
                            searchList.clear();
                            for (int i = 0; i < tracks.size(); i++) {
                                JsonObject t = tracks.get(i).getAsJsonObject();
                                String name = t.get("name").getAsString();
                                String artist = t.getAsJsonObject("artist").get("name").getAsString();
                                searchList.add(new Track(name, artist, "0:30",
                                        "", name + artist, "", ""));
                            }
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> searchAdapter.updateList(searchList));
                            }
                            fetchDeezerForSearch();
                        } catch (Exception ignored) {}
                    }
                    @Override public void onFailure(Call<JsonObject> call, Throwable t) {
                        showNoNetwork(true);
                    }
                });
    }

    private void parseSearchResults(JsonObject json) {
        searchList.clear();
        try {
            JsonObject results = json.getAsJsonObject("results");
            JsonObject trackMatches = results.getAsJsonObject("trackmatches");
            JsonArray tracks = trackMatches.getAsJsonArray("track");
            for (int i = 0; i < tracks.size(); i++) {
                JsonObject t = tracks.get(i).getAsJsonObject();
                String name = t.get("name").getAsString();
                String artist = t.get("artist").getAsString();
                searchList.add(new Track(name, artist, "0:30", "", name + artist, "", ""));
            }
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> searchAdapter.updateList(searchList));
            }
            fetchDeezerForSearch();
        } catch (Exception ignored) {}
    }

    private void fetchDeezerForSearch() {
        for (int i = 0; i < searchList.size(); i++) {
            final int idx = i;
            final Track track = searchList.get(i);
            deezerApi.searchTrack(track.getName() + " " + track.getArtist())
                    .enqueue(new Callback<JsonObject>() {
                        @Override
                        public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                            if (!response.isSuccessful() || response.body() == null) return;
                            try {
                                JsonArray data = response.body().getAsJsonArray("data");
                                if (data == null || data.size() == 0) return;
                                JsonObject first = data.get(0).getAsJsonObject();
                                if (first.has("album")) {
                                    track.setAlbumArt(first.getAsJsonObject("album")
                                            .get("cover_xl").getAsString());
                                }
                                if (first.has("preview")) track.setPreviewUrl(first.get("preview").getAsString());
                                if (first.has("duration")) {
                                    int dur = first.get("duration").getAsInt();
                                    track.setDuration(String.format("%d:%02d", dur / 60, dur % 60));
                                }
                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(() -> searchAdapter.notifyItemChanged(idx));
                                }
                            } catch (Exception ignored) {}
                        }
                        @Override public void onFailure(Call<JsonObject> call, Throwable t) {}
                    });
        }
    }

    private void showNoNetwork(boolean show) {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() ->
                llNoNetwork.setVisibility(show ? View.VISIBLE : View.GONE));
    }
}
