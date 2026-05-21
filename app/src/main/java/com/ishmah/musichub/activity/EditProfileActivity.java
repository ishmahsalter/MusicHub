package com.ishmah.musichub.activity;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.bumptech.glide.Glide;
import com.ishmah.musichub.R;
import com.ishmah.musichub.db.UserProfileDao;
import de.hdodenhof.circleimageview.CircleImageView;
import android.widget.EditText;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EditProfileActivity extends AppCompatActivity {

    private CircleImageView ivAvatar;
    private EditText etUsername, etBio;
    private Switch swNotifications;
    // chip_theme_purple = Aurora, chip_theme_dark = Midnight, chip_theme_gold = Gold Rush
    private TextView chipThemePurple, chipThemeDark, chipThemeGold;
    private UserProfileDao userProfileDao;
    private String selectedPhotoUri = null;
    // Internal name: "aurora" | "midnight" | "goldrush"
    private String selectedThemeName = "aurora";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Aurora → dark, Midnight → dark, Gold Rush → light
    private String mapThemeNameToMode(String name) {
        return "goldrush".equals(name) ? "light" : "dark";
    }

    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedPhotoUri = uri.toString();
                    Glide.with(this).load(uri).into(ivAvatar);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applyThemeFromPrefs();
        setContentView(R.layout.activity_edit_profile);

        userProfileDao = new UserProfileDao(this);

        ivAvatar        = findViewById(R.id.iv_avatar);
        etUsername      = findViewById(R.id.et_username);
        etBio           = findViewById(R.id.et_bio);
        swNotifications = findViewById(R.id.sw_notifications);
        chipThemePurple = findViewById(R.id.chip_theme_purple);
        chipThemeDark   = findViewById(R.id.chip_theme_dark);
        chipThemeGold   = findViewById(R.id.chip_theme_gold);

        // Pre-select theme chip from SharedPreferences
        selectedThemeName = getSharedPreferences("musichub_prefs", MODE_PRIVATE)
                .getString("theme_name", "aurora");
        updateThemeChipUI();

        // Load rest of profile from DB on background thread
        loadProfile();

        chipThemePurple.setOnClickListener(v -> selectTheme("aurora"));
        chipThemeDark.setOnClickListener(v -> selectTheme("midnight"));
        chipThemeGold.setOnClickListener(v -> selectTheme("goldrush"));

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        findViewById(R.id.btn_gallery).setOnClickListener(v ->
                galleryLauncher.launch("image/*"));

        findViewById(R.id.btn_delete_photo).setOnClickListener(v -> {
            selectedPhotoUri = null;
            ivAvatar.setImageResource(R.drawable.ic_profile);
        });

        ((Button) findViewById(R.id.btn_save)).setOnClickListener(v -> saveProfile());
    }

    // FIX #2: SQLite read moved to background thread via ExecutorService
    private void loadProfile() {
        executor.execute(() -> {
            Map<String, String> profile = userProfileDao.getProfile();
            mainHandler.post(() -> {
                if (profile == null) return;

                String username = profile.get("username");
                String bio      = profile.get("bio");
                String photoUri = profile.get("photo_uri");
                String notif    = profile.get("notif_enabled");

                if (username != null) etUsername.setText(username);
                if (bio != null)      etBio.setText(bio);
                if (photoUri != null && !photoUri.isEmpty()) {
                    selectedPhotoUri = photoUri;
                    Glide.with(this).load(Uri.parse(photoUri))
                            .placeholder(R.drawable.ic_profile)
                            .into(ivAvatar);
                }
                if (notif != null) swNotifications.setChecked("1".equals(notif));
                // Theme chip already set from SharedPreferences in onCreate — no override needed
            });
        });
    }

    private void selectTheme(String themeName) {
        selectedThemeName = themeName;
        updateThemeChipUI();
    }

    private void updateThemeChipUI() {
        boolean isAurora   = "aurora".equals(selectedThemeName);
        boolean isMidnight = "midnight".equals(selectedThemeName);
        boolean isGoldRush = "goldrush".equals(selectedThemeName);

        chipThemePurple.setBackgroundResource(
                isAurora ? R.drawable.bg_chip_active : R.drawable.bg_chip_inactive);
        chipThemePurple.setTextColor(getResources().getColor(
                isAurora ? R.color.purple_soft : R.color.text_muted));

        chipThemeDark.setBackgroundResource(
                isMidnight ? R.drawable.bg_chip_active : R.drawable.bg_chip_inactive);
        chipThemeDark.setTextColor(getResources().getColor(
                isMidnight ? R.color.purple_soft : R.color.text_muted));

        chipThemeGold.setBackgroundResource(
                isGoldRush ? R.drawable.bg_chip_active : R.drawable.bg_chip_inactive);
        chipThemeGold.setTextColor(getResources().getColor(
                isGoldRush ? R.color.purple_soft : R.color.text_muted));
    }

    private void saveProfile() {
        String username    = etUsername.getText().toString().trim();
        String bio         = etBio.getText().toString().trim();
        boolean notifEnabled = swNotifications.isChecked();

        if (username.isEmpty()) {
            etUsername.setError("Username cannot be empty");
            return;
        }

        String themeMode = mapThemeNameToMode(selectedThemeName);

        // FIX #3: use mainHandler, save both keys to SharedPreferences
        getSharedPreferences("musichub_prefs", MODE_PRIVATE)
                .edit()
                .putString("theme", themeMode)
                .putString("theme_name", selectedThemeName)
                .apply();

        AppCompatDelegate.setDefaultNightMode(
                "light".equals(themeMode)
                        ? AppCompatDelegate.MODE_NIGHT_NO
                        : AppCompatDelegate.MODE_NIGHT_YES);

        userProfileDao.saveProfile(
                username, bio,
                selectedPhotoUri != null ? selectedPhotoUri : "",
                selectedThemeName, notifEnabled,
                // FIX #3: mainHandler.post instead of runOnUiThread
                () -> mainHandler.post(() -> {
                    Toast.makeText(this, "Profile saved!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                }));
    }

    private void applyThemeFromPrefs() {
        SharedPreferences prefs = getSharedPreferences("musichub_prefs", MODE_PRIVATE);
        String theme = prefs.getString("theme", "dark");
        AppCompatDelegate.setDefaultNightMode(
                "light".equals(theme)
                        ? AppCompatDelegate.MODE_NIGHT_NO
                        : AppCompatDelegate.MODE_NIGHT_YES);
    }

    // FIX #4: shut down executor to avoid thread leaks
    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
