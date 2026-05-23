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
import androidx.viewpager2.widget.ViewPager2;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.ishmah.musichub.R;
import com.ishmah.musichub.adapter.FeaturedCardAdapter;
import com.ishmah.musichub.adapter.TrackAdapter;
import com.ishmah.musichub.api.ApiConfig;
import com.ishmah.musichub.api.DeezerApi;
import com.ishmah.musichub.api.LastFmApi;
import com.ishmah.musichub.api.NetworkChecker;
import com.ishmah.musichub.api.RetrofitClient;
import com.ishmah.musichub.db.CachedImageDao;
import com.ishmah.musichub.model.FeaturedCard;
import com.ishmah.musichub.model.Track;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private RecyclerView rvTrending;
    private TrackAdapter trackAdapter;
    private List<Track> trackList = new ArrayList<>();
    private LinearLayout llNoNetwork, llDots;
    private Button btnRefresh;
    private TextView tvSectionTitle;
    private TextView chipTrending, chipPop, chipHiphop, chipRnb, chipRock;
    private ViewPager2 vpFeatured;
    private FeaturedCardAdapter featuredAdapter;
    private List<FeaturedCard> featuredCards = new ArrayList<>();
    private LastFmApi lastFmApi;
    private DeezerApi deezerApi;
    private CachedImageDao cachedImageDao;
    private ExecutorService executor = Executors.newFixedThreadPool(4);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvTrending = view.findViewById(R.id.rv_trending);
        llNoNetwork = view.findViewById(R.id.ll_no_network);
        btnRefresh = view.findViewById(R.id.btn_refresh);
        tvSectionTitle = view.findViewById(R.id.tv_section_title);
        chipTrending = view.findViewById(R.id.chip_trending);
        chipPop = view.findViewById(R.id.chip_pop);
        chipHiphop = view.findViewById(R.id.chip_hiphop);
        chipRnb = view.findViewById(R.id.chip_rnb);
        chipRock = view.findViewById(R.id.chip_rock);
        vpFeatured = view.findViewById(R.id.vp_featured);
        llDots = view.findViewById(R.id.ll_dots);

        // Setup trending RecyclerView
        trackAdapter = new TrackAdapter(requireContext(), trackList);
        rvTrending.setLayoutManager(new LinearLayoutManager(getContext()));
        rvTrending.setAdapter(trackAdapter);

        // Setup Featured ViewPager2
        featuredAdapter = new FeaturedCardAdapter(
                requireContext(), featuredCards);
        vpFeatured.setAdapter(featuredAdapter);
        vpFeatured.setOffscreenPageLimit(3);
        vpFeatured.setPageTransformer((page, position) -> {
            float scale = 1 - Math.abs(position) * 0.05f;
            page.setScaleY(scale);
            page.setAlpha(1 - Math.abs(position) * 0.2f);
        });
        vpFeatured.registerOnPageChangeCallback(
                new ViewPager2.OnPageChangeCallback() {
                    @Override
                    public void onPageSelected(int position) {
                        updateDots(position);
                    }
                });

        // Init API
        lastFmApi = RetrofitClient.getLastFmInstance()
                .create(LastFmApi.class);
        deezerApi = RetrofitClient.getDeezerInstance()
                .create(DeezerApi.class);
        cachedImageDao = new CachedImageDao(requireContext());

        // Search bar click — both the card and inner text navigate to SearchFragment
        View.OnClickListener openSearch = v -> {
            if (getActivity() instanceof com.ishmah.musichub.activity.MainActivity) {
                ((com.ishmah.musichub.activity.MainActivity) requireActivity()).openSearch();
            }
        };
        view.findViewById(R.id.cv_search_bar).setOnClickListener(openSearch);
        view.findViewById(R.id.tv_search_hint).setOnClickListener(openSearch);

        // Chip clicks
        chipTrending.setOnClickListener(v -> {
            setActiveChip(chipTrending);
            tvSectionTitle.setText("TRENDING");
            loadTrendingTracks();
        });
        chipPop.setOnClickListener(v -> {
            setActiveChip(chipPop);
            tvSectionTitle.setText("POP");
            loadTracksByTag("pop");
        });
        chipHiphop.setOnClickListener(v -> {
            setActiveChip(chipHiphop);
            tvSectionTitle.setText("HIP-HOP");
            loadTracksByTag("hip hop");
        });
        chipRnb.setOnClickListener(v -> {
            setActiveChip(chipRnb);
            tvSectionTitle.setText("R&B");
            loadTracksByTag("rnb");
        });
        chipRock.setOnClickListener(v -> {
            setActiveChip(chipRock);
            tvSectionTitle.setText("ROCK");
            loadTracksByTag("rock");
        });

        btnRefresh.setOnClickListener(v -> {
            loadTrendingTracks();
            loadFeaturedCards();
        });

        loadFeaturedCards();
        loadTrendingTracks();
    }

    private void setActiveChip(TextView activeChip) {
        TextView[] chips = {chipTrending, chipPop,
                chipHiphop, chipRnb, chipRock};
        for (TextView chip : chips) {
            chip.setBackground(requireContext()
                    .getDrawable(R.drawable.bg_chip_inactive));
            chip.setTextColor(0x66FFFFFF);
        }
        activeChip.setBackground(requireContext()
                .getDrawable(R.drawable.bg_chip_active));
        android.util.TypedValue tv = new android.util.TypedValue();
        requireContext().getTheme().resolveAttribute(android.R.attr.colorPrimary, tv, true);
        activeChip.setTextColor(tv.data);
    }

    private void loadFeaturedCards() {
        if (!NetworkChecker.isConnected(requireContext())) return;

        lastFmApi.getTopTracks(ApiConfig.LASTFM_API_KEY, 4)
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call,
                                           Response<JsonObject> response) {
                        if (response.isSuccessful() &&
                                response.body() != null) {
                            parseFeaturedCards(response.body());
                        }
                    }

                    @Override
                    public void onFailure(Call<JsonObject> call,
                                          Throwable t) {}
                });
    }

    private void parseFeaturedCards(JsonObject json) {
        featuredCards.clear();
        try {
            JsonArray tracks = json.getAsJsonObject("tracks")
                    .getAsJsonArray("track");

            String[] badges = {
                    "🔥 TOP TRACK OF THE WEEK",
                    "🎤 TRENDING ARTIST",
                    "💿 HOT RIGHT NOW",
                    "✦ MOST PLAYED"
            };

            for (int i = 0; i < Math.min(tracks.size(), 4); i++) {
                JsonObject t = tracks.get(i).getAsJsonObject();
                String name = t.get("name").getAsString();
                String artist = t.getAsJsonObject("artist")
                        .get("name").getAsString();

                String meta = "";
                if (t.has("listeners")) {
                    long l = t.get("listeners").getAsLong();
                    meta = formatNumber(l) + " listeners";
                } else if (t.has("playcount")) {
                    long p = t.get("playcount").getAsLong();
                    meta = formatNumber(p) + " plays";
                }

                FeaturedCard card = new FeaturedCard(
                        badges[i], name, artist, meta,
                        "", "", name + artist);
                featuredCards.add(card);
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    featuredAdapter.updateCards(featuredCards);
                    setupDots(featuredCards.size());
                    updateDots(0);
                    fetchDeezerForFeatured();
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void fetchDeezerForFeatured() {
        for (int i = 0; i < featuredCards.size(); i++) {
            final int index = i;
            FeaturedCard card = featuredCards.get(i);
            String query = card.getTitle() + " " + card.getSubtitle();

            deezerApi.searchTrack(query).enqueue(
                    new Callback<JsonObject>() {
                        @Override
                        public void onResponse(Call<JsonObject> call,
                                               Response<JsonObject> response) {
                            if (response.isSuccessful() &&
                                    response.body() != null) {
                                try {
                                    JsonArray data = response.body()
                                            .getAsJsonArray("data");
                                    if (data != null && data.size() > 0) {
                                        JsonObject first = data.get(0)
                                                .getAsJsonObject();

                                        String artUrl = "";
                                        if (first.has("artist")) {
                                            JsonObject artist = first
                                                    .getAsJsonObject("artist");
                                            if (artist.has("picture_xl")) {
                                                artUrl = artist
                                                        .get("picture_xl")
                                                        .getAsString();
                                            }
                                        }
                                        if (artUrl.isEmpty() &&
                                                first.has("album")) {
                                            JsonObject album = first
                                                    .getAsJsonObject("album");
                                            if (album.has("cover_xl")) {
                                                artUrl = album
                                                        .get("cover_xl")
                                                        .getAsString();
                                            }
                                        }

                                        if (first.has("preview")) {
                                            featuredCards.get(index)
                                                    .setPreviewUrl(first
                                                            .get("preview")
                                                            .getAsString());
                                        }

                                        featuredCards.get(index)
                                                .setArtUrl(artUrl);

                                        if (getActivity() != null) {
                                            getActivity().runOnUiThread(() ->
                                                    featuredAdapter
                                                            .notifyItemChanged(index));
                                        }
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }

                        @Override
                        public void onFailure(Call<JsonObject> call,
                                              Throwable t) {}
                    });
        }
    }

    private void setupDots(int count) {
        if (llDots == null) return;
        llDots.removeAllViews();
        for (int i = 0; i < count; i++) {
            View dot = new View(requireContext());
            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(
                            i == 0 ? 16 : 6, 6);
            params.setMargins(3, 0, 3, 0);
            dot.setLayoutParams(params);
            dot.setBackgroundResource(i == 0
                    ? R.drawable.bg_dot_active
                    : R.drawable.bg_dot_inactive);
            llDots.addView(dot);
        }
    }

    private void updateDots(int activePos) {
        if (llDots == null) return;
        for (int i = 0; i < llDots.getChildCount(); i++) {
            View dot = llDots.getChildAt(i);
            LinearLayout.LayoutParams params =
                    (LinearLayout.LayoutParams) dot.getLayoutParams();
            params.width = (i == activePos) ? 16 : 6;
            dot.setLayoutParams(params);
            dot.setBackgroundResource(i == activePos
                    ? R.drawable.bg_dot_active
                    : R.drawable.bg_dot_inactive);
        }
    }

    private void loadTrendingTracks() {
        if (!NetworkChecker.isConnected(requireContext())) {
            showNoNetwork(true);
            return;
        }
        showNoNetwork(false);

        lastFmApi.getTopTracks(ApiConfig.LASTFM_API_KEY,
                        ApiConfig.DEFAULT_LIMIT)
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call,
                                           Response<JsonObject> response) {
                        if (response.isSuccessful() &&
                                response.body() != null) {
                            parseTracksAndFetchArt(response.body());
                        }
                    }

                    @Override
                    public void onFailure(Call<JsonObject> call,
                                          Throwable t) {
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

        lastFmApi.getTagTopTracks(ApiConfig.LASTFM_API_KEY,
                        tag, ApiConfig.DEFAULT_LIMIT)
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call,
                                           Response<JsonObject> response) {
                        if (response.isSuccessful() &&
                                response.body() != null) {
                            parseTracksAndFetchArt(response.body());
                        }
                    }

                    @Override
                    public void onFailure(Call<JsonObject> call,
                                          Throwable t) {
                        showNoNetwork(true);
                    }
                });
    }

    private void parseTracksAndFetchArt(JsonObject json) {
        trackList.clear();
        try {
            JsonArray tracks = json.getAsJsonObject("tracks")
                    .getAsJsonArray("track");

            for (int i = 0; i < tracks.size(); i++) {
                JsonObject t = tracks.get(i).getAsJsonObject();
                String name = t.get("name").getAsString();
                String artist = t.getAsJsonObject("artist")
                        .get("name").getAsString();
                String duration = "0:30";
                if (t.has("duration")) {
                    int seconds = t.get("duration").getAsInt();
                    if (seconds > 0) {
                        duration = String.format("%d:%02d",
                                seconds / 60, seconds % 60);
                    }
                }
                Track track = new Track(name, artist, duration,
                        "", name + artist, "", "");
                trackList.add(track);
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() ->
                        trackAdapter.updateList(trackList));
            }

            for (int i = 0; i < trackList.size(); i++) {
                final int index = i;
                final Track track = trackList.get(i);
                String cacheKey = track.getName() + track.getArtist();

                executor.execute(() -> {
                    // FIX: explicit Map type, bukan var
                    Map<String, String> cached =
                            cachedImageDao.getCache(cacheKey);
                    if (cached != null &&
                            cached.get("cover_url") != null &&
                            !cached.get("cover_url").isEmpty()) {
                        track.setAlbumArt(cached.get("cover_url"));
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() ->
                                    trackAdapter.notifyItemChanged(index));
                        }
                        fetchPreviewFromDeezer(track, index,
                                cacheKey, false);
                    } else {
                        fetchPreviewFromDeezer(track, index,
                                cacheKey, true);
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void fetchPreviewFromDeezer(Track track, int index,
                                        String cacheKey,
                                        boolean fetchArt) {
        String query = track.getName() + " " + track.getArtist();
        deezerApi.searchTrack(query).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call,
                                   Response<JsonObject> response) {
                if (response.isSuccessful() &&
                        response.body() != null) {
                    try {
                        JsonArray data = response.body()
                                .getAsJsonArray("data");
                        if (data != null && data.size() > 0) {
                            JsonObject first = data.get(0)
                                    .getAsJsonObject();

                            if (fetchArt && first.has("album")) {
                                JsonObject album = first
                                        .getAsJsonObject("album");
                                if (album.has("cover_xl")) {
                                    String coverUrl = album
                                            .get("cover_xl")
                                            .getAsString();
                                    track.setAlbumArt(coverUrl);
                                    cachedImageDao.saveCache(
                                            cacheKey, coverUrl,
                                            "", null);
                                }
                            }

                            if (first.has("preview")) {
                                track.setPreviewUrl(first
                                        .get("preview")
                                        .getAsString());
                            }

                            if (first.has("duration")) {
                                int dur = first.get("duration")
                                        .getAsInt();
                                if (dur > 0) {
                                    track.setDuration(
                                            String.format("%d:%02d",
                                                    dur / 60, dur % 60));
                                }
                            }

                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() ->
                                        trackAdapter
                                                .notifyItemChanged(index));
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call,
                                  Throwable t) {}
        });
    }

    private String formatNumber(long num) {
        if (num >= 1_000_000) {
            return String.format("%.1fM", num / 1_000_000.0);
        } else if (num >= 1_000) {
            return String.format("%.1fK", num / 1_000.0);
        }
        return String.valueOf(num);
    }

    private void showNoNetwork(boolean show) {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            llNoNetwork.setVisibility(show ? View.VISIBLE : View.GONE);
            rvTrending.setVisibility(show ? View.GONE : View.VISIBLE);
        });
    }
}