package com.ishmah.musichub.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import com.ishmah.musichub.R;

public class MainActivity extends AppCompatActivity {

    private NavController navController;
    private LinearLayout navHome, navSearch, navFavorite, navProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        NavHostFragment navHostFragment = (NavHostFragment)
                getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        navController = navHostFragment.getNavController();

        navHome = findViewById(R.id.nav_home);
        navSearch = findViewById(R.id.nav_search);
        navFavorite = findViewById(R.id.nav_favorite);
        navProfile = findViewById(R.id.nav_profile);

        navHome.setOnClickListener(v -> {
            navController.navigate(R.id.homeFragment);
            setActiveNav(navHome);
        });

        navSearch.setOnClickListener(v -> {
            navController.navigate(R.id.searchFragment);
            setActiveNav(navSearch);
        });

        navFavorite.setOnClickListener(v -> {
            navController.navigate(R.id.favoriteFragment);
            setActiveNav(navFavorite);
        });

        navProfile.setOnClickListener(v -> {
            navController.navigate(R.id.profileFragment);
            setActiveNav(navProfile);
        });

        // Default active = Home
        setActiveNav(navHome);
    }

    private void setActiveNav(LinearLayout active) {
        LinearLayout[] navItems = {navHome, navSearch, navFavorite, navProfile};
        for (LinearLayout item : navItems) {
            if (item == active) {
                item.setAlpha(1f);
                // Set icon + text purple
                ((android.widget.ImageView) item.getChildAt(0))
                        .setColorFilter(getResources().getColor(R.color.purple_glow));
                ((android.widget.TextView) item.getChildAt(1))
                        .setTextColor(getResources().getColor(R.color.purple_glow));
            } else {
                item.setAlpha(0.35f);
                ((android.widget.ImageView) item.getChildAt(0))
                        .setColorFilter(getResources().getColor(R.color.white));
                ((android.widget.TextView) item.getChildAt(1))
                        .setTextColor(getResources().getColor(R.color.white));
            }
        }
    }
}