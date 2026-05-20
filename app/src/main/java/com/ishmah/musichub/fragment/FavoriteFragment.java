package com.ishmah.musichub.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.ishmah.musichub.R;
import com.ishmah.musichub.adapter.TrackAdapter;
import com.ishmah.musichub.db.FavoriteDao;
import com.ishmah.musichub.model.Track;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FavoriteFragment extends Fragment {

    private RecyclerView rvFavorites;
    private TextView tvEmpty;
    private TrackAdapter trackAdapter;
    private List<Track> trackList = new ArrayList<>();
    private FavoriteDao favoriteDao;
    private ExecutorService executor = Executors.newSingleThreadExecutor();

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

        favoriteDao = new FavoriteDao(requireContext());
        rvFavorites = view.findViewById(R.id.rv_favorites);
        tvEmpty = view.findViewById(R.id.tv_empty);

        trackAdapter = new TrackAdapter(requireContext(), trackList);
        rvFavorites.setLayoutManager(new LinearLayoutManager(getContext()));
        rvFavorites.setAdapter(trackAdapter);

        loadFavorites();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload setiap kali fragment ditampilkan
        loadFavorites();
    }

    private void loadFavorites() {
        executor.execute(() -> {
            List<Map<String, String>> favorites =
                    favoriteDao.getAllFavorites();
            trackList.clear();

            for (Map<String, String> fav : favorites) {
                Track track = new Track(
                        fav.get("name") != null ? fav.get("name") : "",
                        fav.get("artist") != null ? fav.get("artist") : "",
                        fav.get("duration") != null ?
                                fav.get("duration") : "0:30",
                        fav.get("albumArt") != null ?
                                fav.get("albumArt") : "",
                        fav.get("trackId") != null ?
                                fav.get("trackId") : "",
                        "", "");
                trackList.add(track);
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    trackAdapter.updateList(trackList);
                    // Show/hide empty state
                    if (trackList.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                        rvFavorites.setVisibility(View.GONE);
                    } else {
                        tvEmpty.setVisibility(View.GONE);
                        rvFavorites.setVisibility(View.VISIBLE);
                    }
                });
            }
        });
    }
}