package com.ishmah.musichub.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.ishmah.musichub.R;
import com.ishmah.musichub.db.FavoriteDao;
import java.util.List;
import java.util.Map;

public class FavoriteFragment extends Fragment {

    private RecyclerView rvFavorites;
    private FavoriteDao favoriteDao;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_favorite, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        favoriteDao = new FavoriteDao(requireContext());
        rvFavorites = view.findViewById(R.id.rv_favorites);
        rvFavorites.setLayoutManager(new LinearLayoutManager(getContext()));
        loadFavorites();
    }

    private void loadFavorites() {
        List<Map<String, String>> favorites = favoriteDao.getAllFavorites();
        // Adapter akan ditambahkan nanti
    }
}